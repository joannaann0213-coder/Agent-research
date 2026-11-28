# Verification of the US-company and vendor/government claims, with gap findings (as of 28 November 2026)

## Read this first

**Part 1 could not be done.** None of the cited news articles could be opened, so no claim in the report was checked against full article text. Every Part 1 verdict below is NOT CHECKABLE. Nothing in this file confirms or contradicts a claim on the strength of a full article.

- **What failed.** WebFetch failed on all eight listed sites I tried (dnyuz.com, www.thestar.com.my, tech.slashdot.org, the-decoder.com, www.windowscentral.com, www.techrepublic.com, www.tipranks.com, www.macrumors.com), each with the error `getaddrinfo ENOTFOUND`. As instructed, I stopped Part 1 there. I did not retry and did not try the other eight pages.
- **Why it failed.** The hosts are allowed: the session's environment ("agent-research") lists all 20 news hosts. But the container's own name lookup returns nothing for any of the 20, and it does answer for Anthropic's sites. WebFetch looks names up locally, so the allowlist has not reached it in this session. I checked this with name lookups only, without fetching a page. I did not test whether the session's HTTPS proxy can reach the hosts, because the task forbids fetching from the shell.
- **What would probably fix it.** A new session started on the same environment, or permission to fetch through the session proxy. Neither was tested.
- **What was read in full.** Five pages on www.anthropic.com, listed under Part 2 (f). They are the only full-text sources in this file. Pages opened: 5, against a limit of about 18.
- **Searches.** 23 of the 25 allowed, all in extended mode. None was refused. Every gap question was searched: (a) 5, (b) 2, (c) 3, (d) 2, (e) 5, (f) 5, and one shared by (a) and (c). Questions (a), (e) and (f) went past the planned one or two searches because each extra search followed a specific new lead: the October report, the extension, and the rehearing request.
- **Background knowledge.** None is used as a finding anywhere in this file.

**Labels used below**

- Grades: **A** = on-record company statement or published memo text. **B** = reputable outlet citing internal documents or several employees. **C** = anecdote (blog, forum, tracker site, single unsourced site).
- **second-hand** = the outlet's report was seen only through other sites' coverage of it.
- **FT** = I read the page in full through WebFetch. WebFetch returns a model-written extract, not the raw page, and limits each quote to about 125 characters, so FT quotes are short fragments.
- **SS** = search summary only: the search tool's summary of the result pages. Quotation marks around SS text show the summary's wording. Where the summary itself quoted a source, I say "quoted in the summary". No SS quote was checked against the article.
- Search results do not show publication dates. Dates come from the URL or from the summary, and "date not shown" means neither gave one.
- A search summary often does not say which result a sentence came from. Where that is so, I write **outlet not named** and link the most relevant results. Treat those links as "one of these", not as a confirmed source for the sentence.

## Part 1: Do the report's claims hold up against the full text of the cited articles?

### Takeaway

Not answered. None of the 16 cited pages on the listed sites could be opened, so all 22 claims are NOT CHECKABLE and the report's caution about search-result summaries still applies to this whole part. The search summaries collected for Part 2 agree with the report where they touch it, but they are the same kind of evidence the report already rests on. Three details need extra caution: the wording of the Google spokesperson quote, the quoted word "undermined" for Microsoft, and the phrase "including for production code" for Amazon.

### Cited Findings

**Fetch log (16 pages)**

| Cited page | Claims | Result |
|---|---|---|
| [dnyuz.com, Amazon, May 2026](https://dnyuz.com/2026/05/04/amazon-employees-pushed-for-claude-code-now-theyre-getting-it-and-codex-too/) | A3, A4 | Tried. Failed (ENOTFOUND). |
| [The Star, Reuters on the Kiro memo](https://www.thestar.com.my/tech/tech-news/2025/11/25/amazon-pushes-in-house-ai-coding-tool-kiro-over-competitors039-memo-shows) | A1 | Tried. Failed. |
| [Slashdot, Amazon, Feb 2026](https://tech.slashdot.org/story/26/02/12/1530202) | A2 | Tried. Failed. |
| [The Decoder, Microsoft licences](https://the-decoder.com/microsoft-pulls-claude-code-licenses-and-pushes-developers-back-toward-its-own-ai-tool/) | M1, M3, M4 | Tried. Failed. |
| [Windows Central, Microsoft](https://www.windowscentral.com/microsoft/microsoft-cancels-claude-code-licenses-shifting-developers-to-github-copilot-cli-a-move-likely-driven-by-financial-motives) | M2 | Tried. Failed. |
| [TechRepublic, Google and Claude](https://www.techrepublic.com/article/news-google-claude-internal-coding-gemini/) | G3, G4, G5 | Tried. Failed. |
| [TipRanks, Meta](https://www.tipranks.com/news/meta-shuts-engineers-out-of-claude-code-and-codex-over-distillation-fears) | T1 | Tried. Failed. |
| [MacRumors, 4 Nov 2026](https://www.macrumors.com/2026/11/04/apple-reducing-employee-ai-spending/) | P3 | Tried. Failed. |
| [Slashdot, Bloomberg on Google](https://developers.slashdot.org/story/26/04/21/1655253/googles-internal-politics-leave-it-playing-catch-up-on-ai-coding) | G1 | Not tried (stop rule). Host has no name-lookup answer. |
| [The Decoder, Google engineer's post](https://the-decoder.com/google-engineer-says-claude-code-built-in-one-hour-what-her-team-spent-a-year-on/) | G2 | Not tried. Same host as a failed page. |
| [MacRumors, May 2023](https://www.macrumors.com/2023/05/19/apple-bans-employees-from-using-chatgpt/) | P1 | Not tried. Same host as a failed page. |
| [MacRumors, May 2025](https://www.macrumors.com/2025/05/02/apple-anthropic-ai-coding-platform/) | P2 | Not tried. Same host as a failed page. |
| [TechCrunch, Anthropic and OpenAI](https://techcrunch.com/2025/08/02/anthropic-cuts-off-openais-access-to-its-claude-models) | V1 | Not tried. Host has no name-lookup answer. |
| [CNBC, OpenAI and Cursor](https://www.cnbc.com/2026/08/29/openai-cursor-spacex-model-access.html) | V2 | Not tried. Host has no name-lookup answer. |
| [CNBC, appeals court](https://www.cnbc.com/2026/09/25/pentagon-anthropic-ai-risk-appeals-court.html) | V3 | Not tried. Host has no name-lookup answer. |
| [The Decoder, cloud providers](https://the-decoder.com/despite-pentagon-ban-google-aws-and-microsoft-stick-with-anthropics-ai-models/) | V4 | Not tried. Same host as a failed page. |

**Claim table.** For every row: full text loaded = no; supporting quote from the article = none, because no article loaded; correction from full text = none possible; additional fact from the article = none. The last column records what the 28 November search summaries say. It is not a verdict.

| ID | The report's sentence | Verdict | 28 Nov search summaries (not full text) |
|---|---|---|---|
| A1 | "On 24 November 2025 Reuters published an internal memo. It called Amazon's own Kiro "our recommended AI-native development tool for Amazon" and said the company did "not plan to support additional third party, AI development tools"" and "The memo did not say "ban", and it kept support for tools already in use." | NOT CHECKABLE | Not searched. |
| A2 | "In February 2026 Business Insider reported that Claude Code needed formal approval for production code, and that about 1,500 employees had backed an internal thread asking Amazon to adopt it. Amazon answered that there was no explicit ban [B]" | NOT CHECKABLE | Agree on approval and on 1,500. The words "no explicit ban" were not seen. Note 1. |
| A3 | "On 4 May 2026 a note from vice president Jim Haughwout made **Claude Code available company-wide at once, with Codex following on 12 May** [B]" | NOT CHECKABLE | Agree. Note 2. |
| A4 | Table cell: "**Yes, both.** Approved company-wide, including for production code, since May 2026. Both run on Amazon's own cloud. Earlier: Codex was labelled "Do Not Use", and Claude Code needed approval for production code." | NOT CHECKABLE | Agree on the cloud. "Including for production code" and "Do Not Use" were not seen. Note 2. |
| A5 | "In October 2025 Amazon gave Codex the internal label "Do Not Use" [B]." and "Claude Code briefly got the same "Do Not Use" label, and Amazon withdrew it after a reporter asked [B]." (no link in the report) | NOT CHECKABLE | Not seen in any summary. |
| M1 | "On 14 May 2026 The Verge reported that Experiences + Devices (Windows, Microsoft 365, Teams, Surface) would **cancel most Claude Code licences by 30 June 2026** and move to GitHub Copilot CLI [B]" | NOT CHECKABLE | Agree on date, group, deadline and replacement. Secondary sites differ on scope. Note 3. |
| M2 | "The Verge's sources added that the decision was "also a financial one"" and, in a later section, "The Verge wrote that Claude Code had "undermined" Microsoft's Copilot CLI" | NOT CHECKABLE | Cost motive agrees. Neither quoted wording was seen; one summary says "undercut". Note 3. |
| M3 | "No outlet called this a ban. It is a withdrawal of paid licences, and Claude models stay available inside Copilot CLI." | NOT CHECKABLE | Agree. Note 3. |
| M4 | Microsoft table row, and "In January 2026 The Verge reported that Microsoft told engineers in its CoreAI and Experiences + Devices groups to install Claude Code and compare it with GitHub Copilot [B]" | NOT CHECKABLE | January claim not searched. For the row, see Part 2 (a). |
| G1 | "Bloomberg reported on 21 April 2026 that Google **does not let employees use rival tools such as Claude Code and Codex, for security reasons**. Employees can apply for an exception if they show a work need [B]" | NOT CHECKABLE | Not searched. |
| G2 | "A Google principal engineer, Jaana Dogan, wrote in January 2026 that Claude Code is allowed only for open-source projects [C]" | NOT CHECKABLE | Not searched. |
| G3 | "Business Insider reported on 14 September that Google had made **Claude Opus 5 available to all engineers inside its internal Antigravity tool, on a per-user quota**, after engineers had grown frustrated with Gemini's limits [B]" | NOT CHECKABLE | Agree. Note 4. |
| G4 | "A spokesperson said: "Gemini remains our primary and foundational model for internal development, with third-party models available on a quota to support specialized use cases" [A, quoted in secondary coverage]." | NOT CHECKABLE | One summary gives a different wording. Note 4. |
| G5 | "The same reports say Google is not opening the Claude Code tool itself." | NOT CHECKABLE | Agree. Note 4. |
| T1 | "In late June 2026 The Information reported new limits. Engineers, mainly in the Applied AI division that builds Meta's own tool MetaCode, **may not use Claude Code or Codex output to create training or evaluation data for Meta's models** [B]" and "An internal memo warned of "serious escalations with partner companies"." | NOT CHECKABLE | General thrust agrees. Quote, division and date not seen. Note 5. |
| P1 | "The Wall Street Journal reported in May 2023 that Apple restricted ChatGPT and told employees not to use GitHub Copilot [B]" | NOT CHECKABLE | Not searched. |
| P2 | "By May 2025 Apple was rolling out an internal version of Xcode built with Anthropic's Claude [B]" | NOT CHECKABLE | Not searched. |
| P3 | "On 4 November 2026 Bloomberg reported that employees can use Claude, ChatGPT and Gemini under **daily spending caps**, and that Apple cut the cap for some engineers from $300 to $200 in October [B]" | NOT CHECKABLE | Not searched. |
| V1 | "Anthropic revoked OpenAI's API access in August 2025" | NOT CHECKABLE | Not searched. |
| V2 | "In August 2026 it gave notice that it would stop supplying models to Cursor after SpaceX bought it, with a proposed shutoff on 12 November 2026 [B]" | NOT CHECKABLE | Agree. Notice dated 28 August. Note 6. |
| V3 | "On 25 September 2026 a Washington appeals court upheld a separate Pentagon designation by 2 votes to 1 [B]" | NOT CHECKABLE | Agree. Note 7. |
| V4 | "Claude is kept out of work done for the Defense Department, and Microsoft, Google and Amazon said in March 2026 that it stays available to their other customers" | NOT CHECKABLE | Cloud providers not searched. Anthropic's own statement on scope was read. Note 8. |

The timeline rows about these companies restate the claims above and take the same verdict.

**Notes to the claim table**

- **Note 1 (A2).** [SS] Coverage of the Business Insider story says Amazon bars Claude Code for "production work or for live products without formal approval" (quoted in the summary), that about 1,500 employees supported adopting it, and that Amazon did not ban it completely but set "stricter requirements for the tools used to develop production code specifically" (quoted in the summary). — [GIGAZINE, 13 Feb 2026](https://gigazine.net/gsc_news/en/20260213-amazon-engineers-revolt-over-ai-tool-restrictions/); [Inc., date not shown](https://www.inc.com/ava-levinson/amazon-anthropic-no-claude/91301306)
- **Note 2 (A3, A4).** [SS] Business Insider reported an internal note from Jim Haughwout, "a VP in Amazon's Software Builder Experience group". Claude Code "became available company-wide right away, and Codex was scheduled for May 12". "Both run on Amazon Bedrock and are managed through AWS." Outlet not named. — [The Daily Star (Bangladesh), date not shown](https://www.thedailystar.net/news/tech-startup/news/amazon-rolls-out-claude-code-and-codex-its-employees-report-4169101); [MSN, date not shown](https://www.msn.com/en-in/news/other/amazon-opens-claude-code-to-all-staff-codex-next-week/gm-GML249BEA6)
- **Note 2, continued.** [SS] The summary says that "until recently Claude Code needed special clearance for production use". That fits the table cell but does not state that production use is now approved. Outlet not named. — [MSN, date not shown](https://www.msn.com/en-in/news/other/amazon-expands-claude-code-codex-access-after-employee-pushback/gm-GMA6F47EC4)
- **Note 3 (M1 to M3).** [SS] The story "traces to The Verge's Tom Warren, who first reported it in a May 14 newsletter". The division "covers Windows, Microsoft 365, Outlook, Teams, and Surface". The change is "expected to take effect by June 30, 2026". — [People Matters, date not shown](https://sea.peoplemattersglobal.com/news/ai-and-emerging-tech/microsoft-cancels-claude-code-licences-after-engineers-use-it-too-much-49918)
- **Note 3, continued.** [SS] "The instruction came from Rajesh Jha", the division's executive vice president. Claude models "stay reachable through Copilot CLI alongside Microsoft's internal-only models and OpenAI's range". Per The Verge as cited by several outlets, Claude Code "became so popular internally that it undercut Copilot CLI". Outlet not named. — [WinBuzzer, 15 May 2026](https://winbuzzer.com/2026/05/15/microsoft-starts-canceling-claude-code-licenses-xcxwbn/); [The Decoder, date not shown](https://the-decoder.com/microsoft-pulls-claude-code-licenses-and-pushes-developers-back-toward-its-own-ai-tool/)
- **Note 3, scope.** [SS] Secondary sites differ on how far the cut goes. Most limit it to Experiences + Devices. La Revue Tech says Microsoft is "pulling the plug on Claude Code for most of its internal engineers on June 30". Another summary, outlet not named, calls Experiences + Devices "the first group affected". The Verge's text is needed to settle this. — [La Revue Tech, date not shown](https://larevuetech.fr/microsoft-to-yank-claude-code-from-most-engineers-by-june-30-pushing-teams-to-github-copilot-cli/); [Technobezz, date not shown](https://www.technobezz.com/news/microsoft-revokes-internal-claude-code-licenses-and-pushes-engineers-to-github-copilot)
- **Note 3, the word "ban".** [SS] About 20 headlines on this story appeared in three searches. They use "cancels", "pulls", "revokes", "drops", "axes" and "restricts". None uses "ban". — for example [The Next Web, date not shown](https://thenextweb.com/news/microsoft-claude-code-retreat-ai-cost)
- **Note 4 (G3 to G5).** [SS] Engineers can pick Claude Opus 5 as a model inside Antigravity, and Claude Code "stays blocked for them". Usage "is capped per user". Before the change, access "had largely been limited to Google DeepMind teams and certain employees working on high-priority engineering projects". Business Insider is named as the origin. — [TownReader, date not shown](https://townreaders.com/article/google-opens-claude-opus-5-access-to-all-engineers-via-antigravity-ide-report-says/); [Dataconomy, 15 Sep 2026](https://dataconomy.com/2026/09/15/google-opens-anthropic-claude-access-for-engineers/)
- **Note 4, the quote.** [SS] One summary gives the spokesperson's words as "Gemini remains our primary model for corporate development," (quoted in the summary, outlet not named). That is not the report's wording. Another summary credits TechBriefly with the sense of the report's version: Gemini stays primary and third-party models are available on a quota. — [TechBriefly, 15 Sep 2026](https://techbriefly.com/2026/09/15/google-anthropic-claude-access-coding-engineers/)
- **Note 4, scope.** [SS] The search tool's two summaries describe Dataconomy's article differently: one says it "describes select teams", the other that Google gave access to "engineers across the company", which is also its headline. Not resolved. — [Dataconomy, 15 Sep 2026](https://dataconomy.com/2026/09/15/google-opens-anthropic-claude-access-for-engineers/)
- **Note 5 (T1).** [SS] Meta "has issued internal guidelines limiting employee use of Claude and OpenAI's Codex for work involving AI-model development". — [ROIC.ai, 5 Oct 2026](https://www.roic.ai/news/meta-and-microsoft-move-to-curb-internal-use-of-anthropics-claude-10-05-2026)
- **Note 6 (V2).** [SS] CNBC's headline is "OpenAI to end model access to Cursor after acquisition by Elon Musk's SpaceX". OpenAI "announced late Friday", which is 28 August 2026, and said it "cannot be confident that SpaceX will honor OpenAI's terms of service" (quoted in the summary). The 12 November date was a proposal and the longest notice the contract allows. — [CNBC, 29 Aug 2026](https://www.cnbc.com/2026/08/29/openai-cursor-spacex-model-access.html)
- **Note 7 (V3).** [SS] CNBC's headline is "U.S. appeals court upholds Pentagon designation of Anthropic as supply chain risk". "In a 2-1 ruling, the DC Circuit court of appeals denied Anthropic's petitions to overturn the designation." Anthropic's products "are not barred from the federal government as a whole under this ruling". Outlet not named for the two quoted sentences. — [CNBC, 25 Sep 2026](https://www.cnbc.com/2026/09/25/pentagon-anthropic-ai-risk-appeals-court.html); [ABC News, date not shown](https://abcnews.com/Business/anthropic-appeals-court-declines-block-pentagon-blacklisting/story?id=136755690)
- **Note 8 (V4).** [A; FT] Anthropic's statement of 5 March 2026 says the designation letter arrived on 4 March, names 10 USC 3252 as the statute, and says of the letter: "it plainly applies only to the use of Claude by customers". The rest of that sentence, in the tool's paraphrase, limits it to use as a direct part of Department of War contracts. The page does not mention Microsoft, Google or Amazon. — [Anthropic, 5 Mar 2026](https://www.anthropic.com/news/where-stand-department-war)
- **Note 8, continued.** [A; FT] On one of the three cloud providers, Anthropic's own page of 27 October 2026 says: "On Bedrock, Claude is authorized for FedRAMP High," and continues "with select models authorized for DoD IL4 and IL5 workloads in AWS GovCloud". This is Anthropic's statement that Claude remains on Amazon's government cloud seven months after the designation. The page does not say whether the Defense Department itself uses Claude. — [Anthropic, 27 Oct 2026](https://www.anthropic.com/news/expanding-partnership-gsa)

### Inferences

- No claim in this part moved from "search summary" to "full text". The report's grades and its caution about unverified quotes should stay as they are.
- The block is in how this session's WebFetch reaches the network, not in the allowlist. A new session started on the same environment, or permission to fetch through the session proxy, would probably fix it. I did not test either.
- Where the report puts words in quotation marks for Google (G4), Microsoft (M2) and Meta (T1), the wording is the least reliable part of the claim. The substance is repeated across outlets; the exact words are not.

### Gaps

- All 16 pages remain unread. If access is fixed, the order of value is: TechRepublic (the Google quote), dnyuz (production code and "Do Not Use"), The Decoder and Windows Central on Microsoft (scope of the cut, CoreAI, the two quotes), TipRanks (the memo quote and the division), the two CNBC pages, then the rest.
- Three more pages on listed hosts would help and were found today: [The Decoder's October piece on Meta and Microsoft](https://the-decoder.com/meta-and-microsoft-pull-back-from-claude-as-anthropic-transforms-from-partner-into-competitor/), [CNBC on the August court ruling](https://www.cnbc.com/2026/08/28/judge-blocks-pentagon-blacklist--anthropic-.html), and [TechCrunch on the same ruling](https://techcrunch.com/2026/08/28/anthropic-gets-its-first-court-win-over-the-pentagons-supply-chain-risk-label/).
- A1, A5, G1, G2, P1, P2, P3 and V1 had no search today either, so nothing new is known about them.

## Part 2 (a): Microsoft. Did the 30 June cancellation happen, and what is the position in November 2026?

### Takeaway

No report confirms that the Experiences + Devices group finished cancelling its Claude Code licences by 30 June 2026. The newest evidence is a report by The Information from about 5 October 2026, seen only through secondary coverage: Microsoft cut its projected internal spending on Claude by more than a third and steered engineers to GitHub Copilot, where OpenAI's model is the default and Claude can still be chosen. Nothing was found on the CoreAI group or on any Microsoft rule about Codex or Cursor.

### Cited Findings

- [B, second-hand; SS] The Information, on or about 5 October 2026: Microsoft's projected internal spending on Anthropic's models fell by more than a third. — [Seeking Alpha, date not shown](https://seekingalpha.com/news/4650314-meta-microsoft-scale-back-employee-use-of-claude-report); [Dealroom, date not shown](https://dealroom.co/news/160133-microsoft-slashes-internal-claude-spending-by-a-third-as-meta-halves-sta/); [ROIC.ai, 5 Oct 2026](https://www.roic.ai/news/meta-and-microsoft-move-to-curb-internal-use-of-anthropics-claude-10-05-2026)
- [B, second-hand; SS] Same report: leadership told staff to scale back Claude use, brought in "stricter spending limits" and steered developers to Microsoft's own infrastructure and to "OpenAI models via GitHub Copilot". Outlet not named. — [Seeking Alpha, date not shown](https://seekingalpha.com/news/4650314-meta-microsoft-scale-back-employee-use-of-claude-report); [Yahoo Finance, date not shown](https://finance.yahoo.com/technology/ai/articles/meta-microsoft-reportedly-trim-anthropic-230612967.html)
- [A as described, seen in one secondary outlet; SS] A Microsoft spokesperson "confirmed the default internal coding model is now OpenAI, while engineers can still pick Claude". This is about the model inside Copilot, not about the Claude Code tool. — [Digital Today, date not shown](https://www.digitaltoday.co.kr/en/view/111429/microsoft-meta-to-sharply-cut-in-house-claude-spending)
- [B, second-hand; SS] Microsoft's spending on Claude for customer-facing Copilot features rose, so its total payments to Anthropic are "roughly flat versus the earlier peak". — [Dealroom, date not shown](https://dealroom.co/news/160133-microsoft-slashes-internal-claude-spending-by-a-third-as-meta-halves-sta/)
- [C; SS] One site says the earlier internal projection was "at least $1 billion a year" and that some per-person AI caps were cut by 90%. The search summary itself calls the 90% figure second-hand and unverified. — [FourWeekMBA, date not shown](https://fourweekmba.com/ai-microsoft-cut-some-ai-caps-90-meta-claude-code-users-halve/)
- [B, second-hand; SS] The October coverage repeats the May plan (most Claude Code licences to end by 30 June 2026) without saying whether it was carried out. — [ROIC.ai, 5 Oct 2026](https://www.roic.ai/news/meta-and-microsoft-move-to-curb-internal-use-of-anthropics-claude-10-05-2026)

### Inferences

- The direction reported in May continued into October. But the October report is about spending and model choice. It is not evidence that the Claude Code licences were cancelled on schedule.
- Tool and model stay separate here. The tool Claude Code was being withdrawn in one group. The model Claude remains selectable inside Microsoft's own tool, though no longer as the default.
- November 2026 has no report of its own. "Early October 2026" is the honest date for Microsoft's position.

### Gaps

- Whether the 30 June cancellation was completed, delayed or reversed: three searches on 28 November found nothing dated after June about the licences themselves.
- The CoreAI group: no source mentions its licences.
- Codex and Cursor at Microsoft: one targeted search found no rule, permission or ban.
- The Information's article itself, its exact date and the spokesperson's exact words were not seen.

## Part 2 (b): Amazon. Any change since May 2026?

### Takeaway

No change was found. Two searches on 28 November returned only coverage of the February restriction and the May approval; nothing dated June to November 2026 reports a new rule on Claude Code, Codex or Kiro.

### Cited Findings

- [A as quoted, second-hand; SS] An Amazon spokesperson on the May change: "Our builders are using Kiro for agentic coding, and now with both Claude Code and Codex running on AWS, we are making additional tools available as well." (quoted in the summary; the outlet is one of the two linked) — [Yahoo News Malaysia, date not shown](https://malaysia.news.yahoo.com/amazon-admits-flagship-ai-coding-143000057.html); [subagentic.ai, date not shown](https://subagentic.ai/posts/amazon-kiro-claude-code-codex-rollout-2026/)
- [C; SS] One site says the May change covers "approximately 50,000 corporate employees". No other source repeats the figure, and the summary flags the site as less established. — [subagentic.ai, date not shown](https://subagentic.ai/posts/amazon-kiro-claude-code-codex-rollout-2026/)
- [C; SS] Reports disagree on Kiro's adoption among Amazon engineers. subagentic.ai says about 70%. Another report, outlet not named, says 83%. — [subagentic.ai, date not shown](https://subagentic.ai/posts/amazon-kiro-claude-code-codex-rollout-2026/); [The Daily Star (Bangladesh), date not shown](https://www.thedailystar.net/news/tech-startup/news/amazon-rolls-out-claude-code-and-codex-its-employees-report-4169101)

### Inferences

- The spokesperson's sentence supports the report's reading: Kiro stays the house tool, and the outside tools were added once they ran on Amazon's cloud.
- An empty result after two searches is weak evidence that nothing changed. It is the same weakness the report already states.

### Gaps

- No source dated June to November 2026 was found on Amazon's rules for Claude Code, Codex or Kiro.
- Whether production use of Claude Code and Codex is now approved without a separate step is still not stated in anything I saw.

## Part 2 (c): Meta. The 60,000 to 30,000 report, and the model behind MetaCode

### Takeaway

The report is The Information's, published on or about 5 October 2026. It says the number of Meta employees using Claude Code fell to about 30,000 from about 60,000 earlier in the year, and it speaks of users, not licences. No source seen names the model behind MetaCode; the closest statement is second-hand wording that Meta's own coding tools and Muse Spark models have largely replaced Claude.

### Cited Findings

- [B, second-hand; SS] The Information, on or about 5 October 2026: "The number of Meta employees currently using Claude Code has declined to about 30,000 users compared to 60,000 earlier this year" (wording given in the summary). — [Seeking Alpha, date not shown](https://seekingalpha.com/news/4650314-meta-microsoft-scale-back-employee-use-of-claude-report); [Channel Insider, date not shown](https://www.channelinsider.com/ai/news-microsoft-meta-cut-internal-claude-use/)
- [B, second-hand; SS] Same report: Meta still "spent more than $105M in a recent 28-day period on Claude Code". — [Seeking Alpha, date not shown](https://seekingalpha.com/news/4650314-meta-microsoft-scale-back-employee-use-of-claude-report)
- [B, second-hand; SS] Two causes are given. Meta laid off about 10% of its roughly 78,000 employees in the spring. The larger cause is Meta's push for its own tools, MetaCode and Muse Code. Outlet not named. — [The Decoder, date not shown](https://the-decoder.com/meta-and-microsoft-pull-back-from-claude-as-anthropic-transforms-from-partner-into-competitor/); [NewsBytes, date not shown](https://www.newsbytesapp.com/news/science/microsoft-and-meta-scale-back-internal-use-of-claude-ai/tldr)
- [C; SS] MetaCode "exceeded 30,000 internal users" and Muse Code "attracted more than 6,000 employees". Outlet not named; the round-one notes trace the MetaCode figure to Shattered.io, and a second summary today says it comes from a site with a sparse sourcing trail. — [BigGo Finance, date not shown](https://finance.biggo.com/news/24ec4ecb-0c98-49c0-84bd-89f88710d73b); [Shattered.io, date not shown](https://shattered.io/meta-microsoft-trim-claude-use-30k-2026/)
- [B, second-hand; SS] The date: the summary places The Information's report "around October 5-6, 2026", and one secondary article carries 5 October in its URL. — [ROIC.ai, 5 Oct 2026](https://www.roic.ai/news/meta-and-microsoft-move-to-curb-internal-use-of-anthropics-claude-10-05-2026)
- [C, second-hand wording; SS] On the model: "The rapid advancement of Meta's proprietary AI coding tools and its Muse Spark line of models has largely displaced the need for Claude" (wording given in the summary, outlet not named, one of the two linked). — [Seeking Alpha, date not shown](https://seekingalpha.com/news/4650314-meta-microsoft-scale-back-employee-use-of-claude-report); [Yahoo Finance, date not shown](https://finance.yahoo.com/technology/ai/articles/meta-microsoft-reportedly-trim-anthropic-230612967.html)
- [B, second-hand; SS] The Information, reportedly August 2026: Meta is pushing thousands of its engineers to use MetaCode. The summary says the article does not name the model. — [TechFlow, date not shown](https://www.techflowpost.com/en-US/newsletter/130852)
- [A for the product; SS] Muse Code is a different product from MetaCode. It is Meta's public terminal coding agent, launched in beta on 5 August 2026, and it runs on Muse Spark 1.2. — [Forbes, 6 Aug 2026](https://www.forbes.com/sites/jonmarkman/2026/08/06/meta-launches-muse-code-a-new-ai-coding-agent-powered-by-spark-12/); [Meta Model API documentation](https://dev.meta.ai/docs/muse-code)

### Inferences

- "Licences" in the report should become "users" or "employees using Claude Code". The round-one notes say "seats"; the most direct wording seen today says "users".
- The halving is not a ban. Coverage says Meta still spends heavily on Claude Code and has not stopped using it.
- MetaCode probably runs on Muse Spark models, because Muse Code does and the October coverage ties Meta's tools to that model line. This is an inference, not a reported fact.

### Gaps

- No source names the model inside MetaCode, or says whether Claude is still selectable there.
- The Information's original article was not seen, so the exact date, the word it uses (users, seats or licences) and the period meant by "earlier this year" are not confirmed.

## Part 2 (d): Google. Any change after 14 September 2026?

### Takeaway

No change was found. Two searches on 28 November returned only the mid-September coverage; nothing dated October or November 2026 reports a new rule.

### Cited Findings

- [B, second-hand; SS] The newest coverage found is dated 15 September 2026 and repeats the Business Insider report: Claude Opus 5 inside Antigravity, a per-user quota, Claude Code itself not opened. — [Dataconomy, 15 Sep 2026](https://dataconomy.com/2026/09/15/google-opens-anthropic-claude-access-for-engineers/); [TechBriefly, 15 Sep 2026](https://techbriefly.com/2026/09/15/google-anthropic-claude-access-coding-engineers/)

### Inferences

- Google's position is still best dated 14 September 2026.

### Gaps

- Nothing dated after mid-September 2026 was found on Claude, Claude Code or Codex inside Google.
- The [Bangkok Post](https://www.bangkokpost.com/life/tech/3326519/google-lets-engineers-use-anthropics-claude-for-coding-work) also carried the September story. Whether that is agency copy from a larger outlet than Business Insider was not checked.

## Part 2 (e): Did OpenAI stop supplying models to Cursor on or around 12 November 2026?

### Takeaway

No, not on that date. An update on OpenAI's own post, seen only as a search summary, says OpenAI temporarily extended Cursor's access beyond 12 November. A Cursor forum thread and a tracker site give 25 November as the new date, and the tracker still found OpenAI's models on Cursor's list on 26 November. Neither company has said the models were removed, so the position on 28 November is unconfirmed.

### Cited Findings

- [A; SS] OpenAI's post "Our decision on Cursor following its acquisition by SpaceX" carries an update saying it "temporarily extended Cursor's access to its models beyond November 12". — [OpenAI, first published 28 Aug 2026 per secondary sites](https://openai.com/index/our-decision-on-cursor-following-its-acquisition-by-spacex/)
- [A; SS] OpenAI's help page says it "proposed a transition period" to 12 November 2026, that Cursor "may choose to end access sooner", and that future models will not go to Cursor. — [OpenAI Help Center, date not shown](https://help.openai.com/en/articles/20001506-using-openai-models-in-cursor)
- [A; SS] OpenAI says users can keep using its models in Cursor's local chat and agent with their own OpenAI API key, billed by OpenAI, but not in Cursor's Tab, Auto, cloud agents, automations, command-line tool or API. — [OpenAI Help Center, date not shown](https://help.openai.com/en/articles/20001506-using-openai-models-in-cursor)
- [C; SS] A Cursor forum thread reports that OpenAI extended access "through November 25". — [Cursor community forum, date not shown](https://forum.cursor.com/t/what-will-happen-to-openai-models-in-cursor-after-november-12/170084)
- [C; SS] A tracker site says the 25 November date passed and "neither OpenAI nor Cursor has said OpenAI's models left Cursor". Cursor's models page "still listed GPT-5.6 Sol, GPT-5.6 Terra and GPT-5.6 Luna when we read it on November 26" (quoted in the summary). — [AgentDeals, 25 to 26 Nov 2026](https://agentdeals.dev/shutdowns/cursor-2026-11-25-openai-s-models-in-cursor)
- [C; SS] A blog post on the day carried the headline "OpenAI's Cursor Cutoff Date Arrives Today — But Neither Side Says the Models Went Dark". — [Inoryva blog, 12 Nov 2026](https://inoryva.blogspot.com/2026/11/openais-cursor-cutoff-date-arrives.html)
- [B; SS] Cursor's chief executive Michael Truell said OpenAI models serve about 5% of Cursor's traffic and that the two companies were talking. Models from Anthropic and Google and the Grok models stay available. Outlet not named. — [CNBC, 29 Aug 2026](https://www.cnbc.com/2026/08/29/openai-cursor-spacex-model-access.html); [Tom's Guide, date not shown](https://www.tomsguide.com/ai/openai-is-leaving-cursor-in-november-here-are-your-3-options)
- [C; SS] SpaceX completed its $60 billion purchase of Anysphere, Cursor's parent, on 14 August 2026, and OpenAI relied on a change-of-control clause. — [AI Builders Weekly, date not shown](https://aibuildersweekly.substack.com/p/openai-cuts-cursor-off-from-its-models)

### Inferences

- Headlines that state the 12 November cutoff as fact were written before the date and are now out of date.
- This restriction is on the model, and only on the supply contract. The Cursor tool keeps working, and a customer's own OpenAI key still works in part of it.
- One extension, continuing talks, and models still listed a day after the extended date suggest the cutoff may keep moving. That is a guess from the pattern, not a reported plan.

### Gaps

- What happened after 25 November: no statement from OpenAI or Cursor and no news report was found.
- OpenAI's post and help page were seen only as search summaries; openai.com cannot be opened from this session.
- No reputable news outlet was found reporting the extension. The on-record source is OpenAI's own page.

## Part 2 (f): Any development between Anthropic and the US Defense Department after 25 September 2026?

### Takeaway

One development was found. On 13 October 2026 Anthropic asked the full appeals court in Washington to rehear the 25 September ruling, and on 21 October a group of 17 former national security officials filed a brief in support. No decision on that request was found by 28 November, so the designation upheld in September still stands. Anthropic's own newsroom shows no statement on the dispute in the items it lists from 9 September to 20 November 2026.

### Cited Findings

- [B; SS] Reuters, 13 October 2026, headline: "Anthropic Seeks Rehearing of Case Over Pentagon's Security Risk Branding". — [Reuters via US News, 13 Oct 2026](https://www.usnews.com/news/top-news/articles/2026-10-13/anthropic-seeks-rehearing-of-case-over-pentagons-security-risk-branding)
- [B; SS] The filing is a petition "for panel rehearing or rehearing en banc", filed on 13 October. Anthropic argues that the panel majority misread 41 U.S.C. § 4713 and ignored evidence that the designation was retaliation for its speech on AI safety. — [MLex, date not shown](https://www.mlex.com/mlex/articles/2537583); [Lawfare, date not shown](https://www.lawfaremedia.org/article/why-anthropic-is-asking-for-a-rehearing-en-banc)
- [B; SS] "A bipartisan group of 17 former senior national security officials, diplomats and intelligence professionals filed a legal brief urging the court to rehear the challenge." — [DefenseScoop, 21 Oct 2026](https://defensescoop.com/2026/10/21/dc-circuit-decision-anthropic-pentagon-fight/)
- [B; SS] The California track has two rulings by Judge Rita Lin. On 26 March 2026 she barred the government from enforcing the designation; the outlet for that date is not named in today's summary, and the round-one notes record the same date from CNBC and Courthouse News. In late August 2026 she ruled that the Pentagon's actions "constituted unlawful retaliation in violation of the First Amendment" (quoted in the summary). — [Jones Walker, date not shown](https://www.joneswalker.com/en/insights/blogs/ai-law-blog/two-courts-two-postures-what-the-dc-circuits-stay-denial-means-for-the-anthrop.html); [CNN, 27 Aug 2026](https://edition.cnn.com/2026/08/27/tech/anthropic-pentagon-supply-chain-risk-unlawful-hnk); [CNBC, 28 Aug 2026](https://www.cnbc.com/2026/08/28/judge-blocks-pentagon-blacklist--anthropic-.html)
- [B; SS] The government appealed in the California track. The headline reads "Pentagon appealing order to remove Anthropic 'supply chain risk' label". — [Inside Defense, date not shown](https://insidedefense.com/insider/pentagon-appealing-order-remove-anthropic-supply-chain-risk-label)
- [B; SS] The 25 September ruling lets the Pentagon keep removing Claude from its systems and bar it from Defense Department work, and does not bar Anthropic from the federal government as a whole. — [CNBC, 25 Sep 2026](https://www.cnbc.com/2026/09/25/pentagon-anthropic-ai-risk-appeals-court.html); [The Next Web, date not shown](https://thenextweb.com/news/anthropic-pentagon-supply-chain-risk-appeals-court-ruling)
- [A; FT] Anthropic's newsroom index, read on 28 November 2026, lists 27 items dated 9 September to 20 November 2026. None is about the Defense Department dispute or a court ruling. — [Anthropic newsroom](https://www.anthropic.com/news)
- [A; FT] Anthropic's statement "Where things stand with the Department of War" is dated 5 March 2026 and carries no update note. It says: "we do not believe this action is legally sound, and we see no choice but to challenge it in court." — [Anthropic, 5 Mar 2026](https://www.anthropic.com/news/where-stand-department-war)
- [A; FT] Anthropic's page on its agreement with the General Services Administration, dated 27 October 2026, gives "a 40% discount on Claude Enterprise, Claude for Government, and the Claude API" and says "The discount also extends to state, local, tribal and territorial governments." It does not mention the Department of War, the designation or any court. — [Anthropic, 27 Oct 2026](https://www.anthropic.com/news/expanding-partnership-gsa)
- [A; FT] The same page says: "On Bedrock, Claude is authorized for FedRAMP High," and continues "with select models authorized for DoD IL4 and IL5 workloads in AWS GovCloud". It does not say whether the Defense Department itself uses Claude. — [Anthropic, 27 Oct 2026](https://www.anthropic.com/news/expanding-partnership-gsa)
- [A; FT] Anthropic's usage policy update, published 8 October 2026 and in force from 12 November, rewrote the surveillance and weapons sections. It says "We also prohibit Claude from being used to build or improve tools designed for surveillance." It does not mention the Department of War or any government exception. — [Anthropic, 8 Oct 2026](https://www.anthropic.com/news/2026-usage-policy-update)
- [A; FT] The same update confirms the regional rule the report dates to 12 November: "Our policy prohibits the use of Claude by people who are physically located in an unsupported region". — [Anthropic, 8 Oct 2026](https://www.anthropic.com/news/2026-usage-policy-update)
- [A; FT] Anthropic filed for a stock-market listing on 26 October 2026, priced it on 18 November and closed it on 20 November. The filing announcement does not mention the dispute. — [Anthropic, 26 Oct 2026](https://www.anthropic.com/news/s1-sec); [Anthropic newsroom](https://www.anthropic.com/news)

### Inferences

- With the rehearing request undecided, the legal position on 28 November is the same as on 25 September: Claude stays out of Defense Department work, and the rest of government can buy it.
- The two court tracks concern different laws. Anthropic's March statement names 10 USC 3252 for the letter it received, and the rehearing petition is about 41 U.S.C. § 4713. That fits the report's phrase "a separate Pentagon designation".
- The report's "sources disagree on whether its first win came in March or in August" can be made specific: there was a March order and an August ruling, and one headline calls the August one the "first court win". Which reading is right was not settled, because the articles could not be opened.
- Anthropic's October policy update shows no sign that it dropped the two limits behind the dispute. The page does not link the update to the dispute, so this is a reading, not a statement.

### Gaps

- Whether the appeals court has ruled on the rehearing request: nothing found by 28 November.
- The state of the government's appeal in the California track, and the date of the Inside Defense report: not found.
- Any talks or settlement between Anthropic and the Defense Department: nothing found.
- Anthropic's listing prospectus should describe the litigation as of late October 2026. The announcement page has no link to it, and the regulator's site cannot be opened from this session.

## Recommended edits to the report

Every edit below rests on search summaries or on Anthropic's own pages. None rests on the full text of a news article. Edits 1 to 9 add new, dated information. Edits 10 to 12 are cautions about wording.

1. **Microsoft table row, second cell.**
   - Change: "**Claude Code: most licences in one large group were due to end on 30 June 2026.** Whether that happened is not confirmed. Before that it was company-paid and encouraged. Not reported as a ban. **Codex: nothing found.**"
   - To: "**Claude Code: most licences in one large group were due to end on 30 June 2026.** No report confirms that this was done. By October 2026 Microsoft had cut its planned internal spending on Claude by more than a third. Before May, Claude Code was company-paid and encouraged. Not reported as a ban. **Codex: no rule found.**"
   - Source: The Information, about 5 October 2026, via [Seeking Alpha](https://seekingalpha.com/news/4650314-meta-microsoft-scale-back-employee-use-of-claude-report) and [Dealroom](https://dealroom.co/news/160133-microsoft-slashes-internal-claude-spending-by-a-third-as-meta-halves-sta/). Search summaries only.

2. **Microsoft table row, third, fourth and fifth cells.**
   - Change: "GitHub Copilot CLI. Claude models stay available inside it." To: "GitHub Copilot CLI. OpenAI's model is reported to be the default inside it, and Claude models stay available."
   - Change: "May 2026. Nothing found after 1 July 2026." To: "May 2026 for the licence plan. Early October 2026 for the spending cut."
   - Change: "B, may be outdated ([The Decoder](https://the-decoder.com/microsoft-pulls-claude-code-licenses-and-pushes-developers-back-toward-its-own-ai-tool/))" To: "B for the May plan ([The Decoder](https://the-decoder.com/microsoft-pulls-claude-code-licenses-and-pushes-developers-back-toward-its-own-ai-tool/)). B, seen only through secondary coverage, for October ([Seeking Alpha](https://seekingalpha.com/news/4650314-meta-microsoft-scale-back-employee-use-of-claude-report))"
   - Source: as edit 1, and [Digital Today](https://www.digitaltoday.co.kr/en/view/111429/microsoft-meta-to-sharply-cut-in-house-claude-spending) for the default model. Search summaries only.

3. **Microsoft paragraph, last two sentences.**
   - Change: "No source after 1 July 2026 was found, so it is not confirmed that the cut happened as planned. Nothing was found on Codex or Cursor at Microsoft."
   - To: "No report was found that confirms the cancellation was completed by 30 June. The next evidence is from October. The Information reported, on or about 5 October 2026, that Microsoft had cut its projected internal spending on Anthropic's models by more than a third, set stricter spending limits and steered engineers to OpenAI models through GitHub Copilot [B, seen only through secondary coverage] ([Seeking Alpha](https://seekingalpha.com/news/4650314-meta-microsoft-scale-back-employee-use-of-claude-report); [Dealroom](https://dealroom.co/news/160133-microsoft-slashes-internal-claude-spending-by-a-third-as-meta-halves-sta/)). One outlet adds that a Microsoft spokesperson confirmed OpenAI's model is now the default for internal coding and that engineers can still choose Claude ([Digital Today](https://www.digitaltoday.co.kr/en/view/111429/microsoft-meta-to-sharply-cut-in-house-claude-spending)). Microsoft's spending on Claude for customer-facing products rose over the same period. No rule on Codex or Cursor at Microsoft was found, and nothing was found on the CoreAI group's licences."
   - Source: as edits 1 and 2. Search summaries only.

4. **Meta table row, second and fourth cells.**
   - Change: "**Yes, with limits.** About 60,000 Claude Code licences in early 2026, later about 30,000. Since May to June 2026 the output may not be used to make training or evaluation data for Meta's own models."
   - To: "**Yes, with limits.** About 60,000 employees used Claude Code earlier in 2026, and about 30,000 by early October 2026. Since May to June 2026 the output may not be used to make training or evaluation data for Meta's own models."
   - Change: "Late June 2026 for the rule. Date of the licence cut unclear." To: "Late June 2026 for the rule. Early October 2026 for the user count."
   - Source: The Information, about 5 October 2026, via [Seeking Alpha](https://seekingalpha.com/news/4650314-meta-microsoft-scale-back-employee-use-of-claude-report) and [Channel Insider](https://www.channelinsider.com/ai/news-microsoft-meta-cut-internal-claude-use/). Search summaries only.

5. **Meta paragraph, the sentence on licences.**
   - Change: "The Information reported that Meta had roughly **60,000 Claude Code licences** earlier in 2026 and about 30,000 later. This was seen only through small secondary sites, and the date of the cut is unclear [B] ([TIKR](https://www.tikr.com/blog/shots-fired-meta-is-developers-on-outside-ai-tools-in-half-thats-a-big-problem-for-anthropic))."
   - To: "The Information reported, on or about 5 October 2026, that the number of Meta employees using Claude Code had fallen to about **30,000, from about 60,000** earlier in the year [B, seen only through secondary coverage] ([Seeking Alpha](https://seekingalpha.com/news/4650314-meta-microsoft-scale-back-employee-use-of-claude-report); [Channel Insider](https://www.channelinsider.com/ai/news-microsoft-meta-cut-internal-claude-use/)). Meta still spent more than $105 million on Claude Code in one recent 28-day period. The coverage gives two causes: Meta cut about 10% of its staff in the spring, and, more important, it pushed its own tools ([The Decoder](https://the-decoder.com/meta-and-microsoft-pull-back-from-claude-as-anthropic-transforms-from-partner-into-competitor/)). Secondary sites put MetaCode at more than 30,000 internal users and Muse Code at more than 6,000 [C] ([Shattered.io](https://shattered.io/meta-microsoft-trim-claude-use-30k-2026/))."
   - Source: as linked. Search summaries only. The summary does not name the outlet for the two causes or for the Muse Code figure, so the Decoder and Shattered.io links are "one of these" until the pages are opened.

6. **Meta paragraph, the sentence on the model behind MetaCode.**
   - Change: "The sources conflict on the model behind MetaCode: Devmate used Claude in 2025, while one 2026 summary says MetaCode runs on Meta's own Muse Spark models [C]."
   - To: "No source seen names the model behind MetaCode. Devmate used Claude in 2025. Coverage of The Information's October 2026 report says Meta's own coding tools and its Muse Spark models have largely replaced Claude inside the company [C, second-hand wording] ([Seeking Alpha](https://seekingalpha.com/news/4650314-meta-microsoft-scale-back-employee-use-of-claude-report)). Muse Code, the public coding agent Meta released in beta on 5 August 2026, runs on Muse Spark 1.2 ([Forbes](https://www.forbes.com/sites/jonmarkman/2026/08/06/meta-launches-muse-code-a-new-ai-coding-agent-powered-by-spark-12/))."
   - Source: as linked. Search summaries only.

7. **Vendor section, OpenAI and Cursor.**
   - Change: "In August 2026 it gave notice that it would stop supplying models to Cursor after SpaceX bought it, with a proposed shutoff on 12 November 2026 [B] ([CNBC](https://www.cnbc.com/2026/08/29/openai-cursor-spacex-model-access.html)). What happened on that date is unverified."
   - To: "On 28 August 2026 it gave notice that it would stop supplying models to Cursor after SpaceX bought it, with a proposed shutoff on 12 November 2026 [B] ([CNBC](https://www.cnbc.com/2026/08/29/openai-cursor-spacex-model-access.html)). By OpenAI's own account the shutoff did not happen on that date. An update on its post says it temporarily extended Cursor's access beyond 12 November [A, read only as a search summary] ([OpenAI](https://openai.com/index/our-decision-on-cursor-following-its-acquisition-by-spacex/)). A Cursor forum thread gives 25 November as the new date, and a tracker site still found OpenAI's models on Cursor's list on 26 November [C] ([Cursor forum](https://forum.cursor.com/t/what-will-happen-to-openai-models-in-cursor-after-november-12/170084); [AgentDeals](https://agentdeals.dev/shutdowns/cursor-2026-11-25-openai-s-models-in-cursor)). Neither company has said the models were removed, so the position at the end of November is unconfirmed."
   - Source: as linked. Search summaries only.

8. **Government section, the courts.**
   - Change: "The courts split. A California district court ruled for Anthropic, and sources disagree on whether its first win came in March or in August. On 25 September 2026 a Washington appeals court upheld a separate Pentagon designation by 2 votes to 1 [B] ([CNBC](https://www.cnbc.com/2026/09/25/pentagon-anthropic-ai-risk-appeals-court.html))."
   - To: "The courts split. In California, Judge Rita Lin barred the government from enforcing the designation on 26 March 2026 ([Jones Walker](https://www.joneswalker.com/en/insights/blogs/ai-law-blog/two-courts-two-postures-what-the-dc-circuits-stay-denial-means-for-the-anthrop.html)), and in late August 2026 she ruled that it was unlawful retaliation under the First Amendment [B] ([CNN](https://edition.cnn.com/2026/08/27/tech/anthropic-pentagon-supply-chain-risk-unlawful-hnk); [CNBC](https://www.cnbc.com/2026/08/28/judge-blocks-pentagon-blacklist--anthropic-.html)). One headline calls the August ruling Anthropic's "first court win", which does not fit the March order; this was not resolved ([TechCrunch](https://techcrunch.com/2026/08/28/anthropic-gets-its-first-court-win-over-the-pentagons-supply-chain-risk-label/)). On 25 September 2026 a Washington appeals court upheld a separate Pentagon designation, made under a different law, by 2 votes to 1 [B] ([CNBC](https://www.cnbc.com/2026/09/25/pentagon-anthropic-ai-risk-appeals-court.html)). On 13 October Anthropic asked the full court to rehear the case, and 17 former national security officials filed a brief in support [B] ([Reuters via US News](https://www.usnews.com/news/top-news/articles/2026-10-13/anthropic-seeks-rehearing-of-case-over-pentagons-security-risk-branding); [DefenseScoop](https://defensescoop.com/2026/10/21/dc-circuit-decision-anthropic-pentagon-fight/)). No decision on that request was found by 28 November 2026."
   - Source: as linked. Search summaries only. Today's summary does not name the outlet for the March date; the Jones Walker link is the likeliest of the results, and the round-one notes record the same date from CNBC and Courthouse News. "Made under a different law" rests on the petition's reference to 41 U.S.C. § 4713 ([MLex](https://www.mlex.com/mlex/articles/2537583)) against the 10 USC 3252 named in [Anthropic's March statement](https://www.anthropic.com/news/where-stand-department-war).

9. **Timeline, the last row.**
   - Change: "| Oct to Nov 2026 | Apple cuts daily AI budgets. ByteDance shuts outside AI out of its most sensitive code and documents. Anthropic's stricter regional policy takes effect (12 Nov). |"
   - To: "| Oct to Nov 2026 | The Information reports that Meta's Claude Code users have halved and that Microsoft has cut planned internal Claude spending by more than a third (about 5 Oct). Anthropic asks the full appeals court to rehear the Pentagon case (13 Oct). Apple cuts daily AI budgets. ByteDance shuts outside AI out of its most sensitive code and documents. Anthropic's stricter regional policy takes effect (12 Nov). OpenAI extends Cursor's access past its 12 Nov shutoff date. |"
   - Source: edits 3, 5, 7 and 8.

10. **Google paragraph, the spokesperson quote (caution, not a correction).**
    - Change: "A spokesperson said: "Gemini remains our primary and foundational model for internal development, with third-party models available on a quota to support specialized use cases" [A, quoted in secondary coverage]."
    - To: "A spokesperson said Gemini remains Google's primary model for internal development and that third-party models are available on a quota for specialised uses [A, wording differs between outlets and was not checked against Business Insider]."
    - Source: one search summary gives the quote as "Gemini remains our primary model for corporate development,"; [TechBriefly](https://techbriefly.com/2026/09/15/google-anthropic-claude-access-coding-engineers/) carries the sense of the report's version. Restore the direct quote only after reading [TechRepublic](https://www.techrepublic.com/article/news-google-claude-internal-coding-gemini/) in full.

11. **Timeline, the May 2026 row (attribution).**
    - Change: "Microsoft says it will cancel most Claude Code licences in one group by 30 June."
    - To: "Microsoft tells one group that most Claude Code licences will end by 30 June."
    - Source: the report's own body text attributes this to The Verge's report of an internal memo, not to a public statement; the search summary says the same ([People Matters](https://sea.peoplemattersglobal.com/news/ai-and-emerging-tech/microsoft-cancels-claude-code-licences-after-engineers-use-it-too-much-49918)).

12. **Sentences that record how recently each company was checked.**
    - Change, Amazon table row: "May 2026. No later change found." To: "May 2026. No later change found in searches on 28 November 2026."
    - Change, Google paragraph: "One search for later changes found none." To: "Three searches for later changes, the last two on 28 November 2026, found none."
    - Change, limits section: "Google was last confirmed in September, Alibaba in July, Meta between June and September, Amazon and Microsoft in May." To: "Google was last confirmed in September, Alibaba in July, Meta and Microsoft in early October, Amazon in May."
    - Change, limits section: "and whether OpenAI's Cursor shutoff happened" To: "and what happened to OpenAI's models in Cursor after 25 November".
    - Source: Part 2 (a) to (e) of this file.

**Not recommended.** Do not remove or soften the report's caution that findings rest on search-result summaries. For the five US companies and the four vendor and government claims in this file, that is still true.
