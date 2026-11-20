package dev.agentflow.inbox.client;

import dev.agentflow.inbox.api.InboxJson;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

/**
 * An {@link Outbox} shared by several processes through a directory: one file per dedupe key, created
 * atomically. It lets a worker that crashed and the worker that takes over see the same delivery log.
 */
public final class FileOutbox implements Outbox {

    private final Path directory;

    public FileOutbox(Path directory) {
        this.directory = directory;
        try {
            Files.createDirectories(directory);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Override
    public boolean send(OutboxMessage message) {
        Path file = directory.resolve(message.dedupeKey().replaceAll("[^A-Za-z0-9._-]", "_") + ".json");
        try {
            Files.write(file, InboxJson.mapper().writeValueAsBytes(message), StandardOpenOption.CREATE_NEW);
            return true;
        } catch (FileAlreadyExistsException duplicate) {
            return false;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Override
    public List<OutboxMessage> delivered() {
        List<OutboxMessage> messages = new ArrayList<>();
        try (Stream<Path> files = Files.list(directory)) {
            for (Path file : (Iterable<Path>) files::iterator) {
                messages.add(InboxJson.mapper().readValue(Files.readAllBytes(file), OutboxMessage.class));
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return messages;
    }
}
