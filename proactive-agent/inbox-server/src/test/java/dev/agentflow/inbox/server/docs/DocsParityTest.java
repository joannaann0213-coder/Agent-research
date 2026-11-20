package dev.agentflow.inbox.server.docs;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

/**
 * Keeps the English and the Chinese documentation in step. A translation cannot be checked for
 * meaning by a machine, but its skeleton can: the same documents, the same headings, tables, lists
 * and code, the same identifiers, and links that lead somewhere. A change made to one language only
 * fails here.
 */
class DocsParityTest {

    private static final Pattern HEADING = Pattern.compile("^(#{1,6})\\s+(.+?)\\s*$");
    private static final Pattern LIST_ITEM = Pattern.compile("^\\s*(-|\\d+\\.)\\s+.*");
    private static final Pattern INLINE_CODE = Pattern.compile("`([^`]+)`");
    private static final Pattern LINK = Pattern.compile("\\[[^\\]]*\\]\\(([^)\\s]+)\\)");

    private static final Path ROOT = projectRoot();

    /** A fenced code block: its language and its lines. */
    private record CodeBlock(String language, List<String> lines) {

        /** The part that must be identical in every language. Comments may be translated, code may not. */
        List<String> comparable() {
            if ("mermaid".equals(language)) {
                return List.of(lines.size() + " lines"); // labels are translated; the shape is not
            }
            if ("bash".equals(language)) {
                return lines.stream().filter(line -> !line.strip().startsWith("#")).toList();
            }
            return lines;
        }
    }

    /** What a Markdown file looks like once the prose is taken away. */
    private record Skeleton(List<Integer> headingLevels, List<String> headingSlugs, List<String> tableShapes,
            int bulletItems, int numberedItems, List<CodeBlock> code, Set<String> inlineCode, List<String> links) {
    }

    @Test
    void bothLanguagesHoldTheSameDocuments() {
        assertThat(names(ROOT.resolve("docs/zh"))).as("docs/zh mirrors docs/en").isEqualTo(names(ROOT.resolve("docs/en")));
        assertThat(ROOT.resolve("README.md")).exists();
        assertThat(ROOT.resolve("README.zh-CN.md")).exists();
    }

    @Test
    void everyTranslationHasTheSameSkeleton() {
        pairs().forEach((english, chinese) -> {
            Skeleton en = read(english);
            Skeleton zh = read(chinese);
            String pair = ROOT.relativize(english) + " and " + ROOT.relativize(chinese);
            assertThat(zh.headingLevels()).as("heading levels of %s", pair).isEqualTo(en.headingLevels());
            assertThat(zh.tableShapes()).as("tables (rows x columns) of %s", pair).isEqualTo(en.tableShapes());
            assertThat(zh.bulletItems()).as("bullet items of %s", pair).isEqualTo(en.bulletItems());
            assertThat(zh.numberedItems()).as("numbered items of %s", pair).isEqualTo(en.numberedItems());
            assertThat(zh.code().stream().map(CodeBlock::language).toList()).as("code blocks of %s", pair)
                    .isEqualTo(en.code().stream().map(CodeBlock::language).toList());
            for (int i = 0; i < en.code().size(); i++) {
                assertThat(zh.code().get(i).comparable()).as("code block %d of %s", i + 1, pair)
                        .isEqualTo(en.code().get(i).comparable());
            }
            assertThat(zh.inlineCode()).as("identifiers, values and paths named in %s", pair).isEqualTo(en.inlineCode());
        });
    }

    @Test
    void everyRelativeLinkLeadsSomewhere() {
        for (Path document : documents()) {
            Skeleton skeleton = read(document);
            for (String link : skeleton.links()) {
                if (link.startsWith("http://") || link.startsWith("https://")) {
                    continue;
                }
                int hash = link.indexOf('#');
                String file = hash < 0 ? link : link.substring(0, hash);
                Path target = file.isEmpty() ? document : document.getParent().resolve(file).normalize();
                assertThat(target).as("link '%s' in %s", link, ROOT.relativize(document)).exists();
                if (hash >= 0) {
                    assertThat(read(target).headingSlugs())
                            .as("anchor of link '%s' in %s", link, ROOT.relativize(document))
                            .contains(link.substring(hash + 1));
                }
            }
        }
    }

    @Test
    void everyDocumentOffersItsTranslation_andOtherwiseStaysInItsOwnLanguage() {
        pairs().forEach((english, chinese) -> {
            String toChinese = english.getParent().relativize(chinese).toString().replace('\\', '/');
            String toEnglish = chinese.getParent().relativize(english).toString().replace('\\', '/');
            List<String> fromEnglish = read(english).links();
            List<String> fromChinese = read(chinese).links();
            assertThat(fromEnglish).as("first link of %s", ROOT.relativize(english)).first().isEqualTo(toChinese);
            assertThat(fromChinese).as("first link of %s", ROOT.relativize(chinese)).first().isEqualTo(toEnglish);
            assertThat(fromEnglish.stream().skip(1)).as("%s links to English documents", ROOT.relativize(english))
                    .noneMatch(link -> link.startsWith("../zh/") || link.startsWith("docs/zh/")
                            || link.startsWith("README.zh-CN.md"));
            assertThat(fromChinese.stream().skip(1)).as("%s links to Chinese documents", ROOT.relativize(chinese))
                    .noneMatch(link -> link.startsWith("../en/") || link.startsWith("docs/en/")
                            || link.startsWith("README.md"));
        });
    }

    // ---- reading --------------------------------------------------------------------------------

    private static Map<Path, Path> pairs() {
        Map<Path, Path> pairs = new LinkedHashMap<>();
        pairs.put(ROOT.resolve("README.md"), ROOT.resolve("README.zh-CN.md"));
        for (String name : names(ROOT.resolve("docs/en"))) {
            pairs.put(ROOT.resolve("docs/en").resolve(name), ROOT.resolve("docs/zh").resolve(name));
        }
        return pairs;
    }

    private static List<Path> documents() {
        List<Path> documents = new ArrayList<>();
        pairs().forEach((english, chinese) -> {
            documents.add(english);
            documents.add(chinese);
        });
        return documents;
    }

    private static Set<String> names(Path directory) {
        try (Stream<Path> files = Files.list(directory)) {
            return new TreeSet<>(files.map(file -> file.getFileName().toString()).toList());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static Skeleton read(Path file) {
        List<String> lines;
        try {
            lines = Files.readAllLines(file);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        List<Integer> headingLevels = new ArrayList<>();
        List<String> headingSlugs = new ArrayList<>();
        List<String> tableShapes = new ArrayList<>();
        List<CodeBlock> code = new ArrayList<>();
        Set<String> inlineCode = new TreeSet<>();
        List<String> links = new ArrayList<>();
        int bullets = 0;
        int numbered = 0;

        String fenceLanguage = null;
        List<String> fenceLines = null;
        int tableRows = 0;
        int tableColumns = 0;
        for (String line : lines) {
            if (line.startsWith("```")) {
                if (fenceLanguage == null) {
                    fenceLanguage = line.substring(3).strip();
                    fenceLines = new ArrayList<>();
                } else {
                    code.add(new CodeBlock(fenceLanguage, List.copyOf(fenceLines)));
                    fenceLanguage = null;
                }
                continue;
            }
            if (fenceLanguage != null) {
                fenceLines.add(line);
                continue;
            }
            String withoutCode = INLINE_CODE.matcher(line).replaceAll("");
            if (line.startsWith("|")) {
                tableRows++;
                tableColumns = Math.max(tableColumns, (int) withoutCode.chars().filter(c -> c == '|').count() - 1);
            } else if (tableRows > 0) {
                tableShapes.add(tableRows + "x" + tableColumns);
                tableRows = 0;
                tableColumns = 0;
            }
            Matcher heading = HEADING.matcher(line);
            if (heading.matches()) {
                headingLevels.add(heading.group(1).length());
                headingSlugs.add(unique(slug(heading.group(2)), headingSlugs));
            } else if (LIST_ITEM.matcher(line).matches()) {
                if (line.strip().startsWith("-")) {
                    bullets++;
                } else {
                    numbered++;
                }
            }
            Matcher inline = INLINE_CODE.matcher(line);
            while (inline.find()) {
                inlineCode.add(inline.group(1));
            }
            Matcher link = LINK.matcher(withoutCode);
            while (link.find()) {
                links.add(link.group(1));
            }
        }
        if (tableRows > 0) {
            tableShapes.add(tableRows + "x" + tableColumns);
        }
        assertThat(fenceLanguage).as("unclosed code block in %s", file).isNull();
        return new Skeleton(headingLevels, headingSlugs, tableShapes, bullets, numbered, code, inlineCode, links);
    }

    /** The anchor a Markdown renderer gives a heading: lower case, punctuation dropped, spaces to hyphens. */
    private static String slug(String heading) {
        StringBuilder slug = new StringBuilder();
        heading.replace("`", "").toLowerCase(Locale.ROOT).codePoints().forEach(c -> {
            if (Character.isLetterOrDigit(c) || c == '-' || c == '_') {
                slug.appendCodePoint(c);
            } else if (c == ' ') {
                slug.append('-');
            }
        });
        return slug.toString();
    }

    /** A second heading with the same text gets "-1", a third "-2". */
    private static String unique(String slug, List<String> taken) {
        String candidate = slug;
        for (int n = 1; taken.contains(candidate); n++) {
            candidate = slug + "-" + n;
        }
        return candidate;
    }

    private static Path projectRoot() {
        Path directory = Path.of("").toAbsolutePath();
        while (directory != null && !Files.isDirectory(directory.resolve("docs/en"))) {
            directory = directory.getParent();
        }
        assertThat(directory).as("a parent directory that holds docs/en").isNotNull();
        return directory;
    }
}
