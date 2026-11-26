# Amazon and Microsoft: internal policies on engineers' use of external AI coding tools versus in-house tools (state as of 26 November 2026)

> **Read this first: method and limits of these notes.**
> - Research date: 2026-11-26. 26 web searches were completed (10 in "extended" mode). The shared web-search budget for this turn (200 calls across all agents) then ran out, so 8 further planned searches were **not performed** (listed in the Gaps of the sections they affect).
> - **No article was read in full.** WebFetch failed for every host tried (thenewstack.io, thestar.com.my, slashdot.org, the-decoder.com, windowscentral.com) because the session's network allowlist is empty. Every finding below therefore rests on search-result summaries and snippets, cross-checked across several outlets where possible. Quotes are "as relayed in the search results from the linked page", not checked against the original page.
> - Evidence grades: **A** = on-record company statement or published memo text; **B** = reputable outlet citing internal documents or several employees; **C** = anecdote (Blind, Reddit, Hacker News, X). Because originals were not opened, many A/B items here reach me through secondary coverage; that is flagged as "(via secondary)".
> - "May be outdated" = not reconfirmed by any source dated after mid-2026.
> - Items labelled **UNVERIFIED LEAD** come from the researcher's background knowledge and were not checked in this session. They are placed only under Gaps and must not be presented as verified.
> - Tool or harness (Claude Code, Codex CLI, Cursor, GitHub Copilot, Kiro) is kept separate from model (Claude, GPT, Nova, open-weight models) throughout.

---

## 1. Amazon, the Kiro guidance: what the late-2025 internal guidance said

### Takeaway
The guidance was not worded as a ban. A memo obtained by Reuters (published 24 November 2025, confirmed by an Amazon spokesperson) named Kiro "our recommended AI-native development tool for Amazon" and said Amazon would keep supporting existing tools but did "not plan to support additional third party, AI development tools". The hard edges came from two separate mechanisms: an internal tool-status label ("Do Not Use", applied to OpenAI Codex in October 2025 and briefly to Claude Code) and a formal-approval requirement for using third-party tools on production code (reported February 2026).

### Cited Findings
- **Outlet, date, author.** Reuters, 24 November 2025, by Greg Bensinger, headline "Amazon pushes in-house AI coding tool Kiro over competitors', memo shows". [Grade A/B via syndicated copies] — [WHBL/Reuters](https://whbl.com/2025/11/24/amazon-pushes-in-house-ai-coding-tool-kiro-over-competitors-memo-shows/); [The Star/Reuters](https://www.thestar.com.my/tech/tech-news/2025/11/25/amazon-pushes-in-house-ai-coding-tool-kiro-over-competitors039-memo-shows)
- **Core wording.** The memo says: "We're making Kiro our recommended AI-native development tool for Amazon." It also says, in a sentence beginning "While we continue to support existing tools…", that "we do not plan to support additional third party, AI development tools." [Grade A, memo text published by Reuters; via secondary] — [The Star/Reuters](https://www.thestar.com.my/tech/tech-news/2025/11/25/amazon-pushes-in-house-ai-coding-tool-kiro-over-competitors039-memo-shows); [Slashdot, 30 Nov 2025](https://developers.slashdot.org/story/25/11/30/048214/amazon-tells-its-engineers-use-our-ai-coding-tool-kiro)
- **Signatories.** Peter DeSantis, senior vice president of AWS utility computing, and Dave Treadwell, senior vice president of eCommerce Foundation. [Grade A/B] — [WHBL/Reuters](https://whbl.com/2025/11/24/amazon-pushes-in-house-ai-coding-tool-kiro-over-competitors-memo-shows/); [The Independent SG](https://theindependent.sg/amazon-directs-its-engineers-to-use-kiro-over-rival-ai-coding-tools/)
- **Where it was published and confirmation.** The memo was posted on Amazon's internal news site, and an Amazon spokesperson confirmed it to Reuters. [Grade A] — [The Star/Reuters](https://www.thestar.com.my/tech/tech-news/2025/11/25/amazon-pushes-in-house-ai-coding-tool-kiro-over-competitors039-memo-shows)
- **Reuters' own reading of the effect.** Reuters wrote that the guidance "would seem to rule out" or effectively discourage tools such as OpenAI's Codex, Anthropic's Claude Code and Cursor. This is the outlet's interpretation, not memo wording. [Grade B] — [The Star/Reuters](https://www.thestar.com.my/tech/tech-news/2025/11/25/amazon-pushes-in-house-ai-coding-tool-kiro-over-competitors039-memo-shows); [Hackernoon](https://hackernoon.com/lite/amazon-urges-staff-to-use-in-house-kiro-over-rival-ai-coding-tools)
- **"Do Not Use" labels.** In October 2025 Amazon designated OpenAI's Codex "Do Not Use" after an assessment of about six months. Claude Code briefly received the same designation, and Amazon reversed that after a reporter's inquiry. [Grade B, Reuters citing internal material] — [The Star/Reuters](https://www.thestar.com.my/tech/tech-news/2025/11/25/amazon-pushes-in-house-ai-coding-tool-kiro-over-competitors039-memo-shows); [Slashdot](https://developers.slashdot.org/story/25/11/30/048214/amazon-tells-its-engineers-use-our-ai-coding-tool-kiro)
- **Commercial context noted by Reuters.** The guidance came although Amazon had invested about $8 billion in Anthropic and had signed a seven-year, $38 billion cloud deal with OpenAI. [Grade B] — [The Star/Reuters](https://www.thestar.com.my/tech/tech-news/2025/11/25/amazon-pushes-in-house-ai-coding-tool-kiro-over-competitors039-memo-shows)
- **Stronger framings in secondary coverage.** Headlines such as "Amazon blocks Codex to get devs using its AI coding tool" (NewsBytes) and "push for employees to use only Kiro" (People Matters) go further than the quoted memo wording. [Secondary framing, not evidence of a ban] — [NewsBytes](https://www.newsbytesapp.com/news/science/amazon-blocks-codex-to-get-devs-using-its-ai-coding-tool/tldr); [People Matters](https://www.peoplematters.in/news/ai-and-emerging-tech/amazon-memo-reveals-push-for-employees-to-use-only-kiro-its-in-house-ai-tool-47390)
- **Adoption target attached to the push.** Secondary coverage says Amazon wanted 80% of developers to use AI for coding at least once a week; one low-quality source attributes an "80% weekly usage by year-end" target to the November 2025 memo itself. Fortune's 28 October 2026 coverage (as summarised) says a target of more than 80% of developers using AI each week is still in place. [Grade B for the existence of the target; attribution to the memo is unconfirmed] — [Fortune, 28 Oct 2026](https://fortune.com/2026/10/28/amazon-token-maxxing-ai-work/); [AI in Plain English (low quality)](https://ai.plainenglish.io/amazons-ai-ultimatum-why-80-of-developers-must-code-with-kiro-or-else-5f5753986302)
- **Blind anecdote.** A Blind thread titled "Amazon mandates in-house AI coding tool Kiro, limits external tools, impact on dev productivity" exists; content not read. [Grade C, title only] — [Blind](https://www.teamblind.com/post/amazon-mandates-in-house-ai-coding-tool-kiro-limits-external-tools-impact-on-dev-productivity-q5seaph1)

### Inferences
- The accurate label for the November 2025 memo is "in-house tool recommended as the default, plus no support for additional third-party tools", not "ban". Tools already in use were explicitly grandfathered by the memo wording.
- The nearest thing to a ban was the tool-catalogue label "Do Not Use" for Codex (October 2025). The fact that Claude Code's identical label was withdrawn after a press inquiry suggests Amazon did not want to be seen banning the product of a company it part-owns.
- The practical restriction on Claude Code came from the production-approval rule reported in February 2026 (section 2), not from the memo text.

### Gaps
- The full memo text is not public and the Reuters article was not read in full. The exact internal posting date of the memo is not established (Reuters reported on 24 November 2025).
- Which "existing tools in use today" were grandfathered is not stated in the material seen.
- Whether the 80% weekly-use target was in the memo or in a separate goal document is unresolved.
- UNVERIFIED LEAD: at re:Invent in early December 2025, AWS CEO Matt Garman is recalled as saying Amazon had decided to standardise on Kiro as its official AI development environment. Not checked in this session.

---

## 2. Amazon, status of specific tools (Claude Code, Codex, Cursor, GitHub Copilot) and changes during 2026

### Takeaway
Amazon moved from restriction to permission in May 2026. Until then Claude Code needed formal approval for production code and Codex carried a "Do Not Use" label. On 4 May 2026 a vice president's note made Claude Code available company-wide immediately, approved for production use, with Codex following on 12 May 2026, both running on AWS and Amazon Bedrock. Kiro remains the recommended primary tool. No report of a later reversal was found, but the latest direct confirmation is May 2026, so the status "may be outdated". No evidence was found on GitHub Copilot, and nothing after November 2025 on Cursor.

### Cited Findings

**Timeline (Amazon)**

| Date | Event | Grade | Source |
|---|---|---|---|
| Jan 2023 | Amazon lawyer warns staff not to put confidential information or code into ChatGPT | B | [Yahoo News](https://news.yahoo.com/amazon-warns-employees-beware-chatgpt-174500629.html) |
| Feb 2024 | Internal documents: third-party generative AI tools "strictly prohibited" for confidential Amazon work | B | [Business Today](https://businesstoday.in/technology/story/amazon-stops-employees-from-using-generative-ai-tools-like-chatgpt-418845-2024-02-25) |
| Jun 2025 | Amazon in talks to roll out Cursor internally after employee demand; security issues to resolve first | B | [Reuters pickup of Business Insider](https://in.tradingview.com/news/reuters.com,2025:newsml_L3N3S61BX:0-amazon-is-in-talks-to-roll-out-ai-coding-assistant-cursor-internally-business-insider) |
| Oct 2025 | Codex designated "Do Not Use" after roughly six-month assessment | B | [The Star/Reuters](https://www.thestar.com.my/tech/tech-news/2025/11/25/amazon-pushes-in-house-ai-coding-tool-kiro-over-competitors039-memo-shows) |
| 24 Nov 2025 | Reuters publishes Kiro memo: no support for additional third-party AI development tools | A/B | [WHBL/Reuters](https://whbl.com/2025/11/24/amazon-pushes-in-house-ai-coding-tool-kiro-over-competitors-memo-shows/) |
| Feb 2026 (reported about 11 to 12 Feb) | Business Insider: engineers push back; Claude Code needs formal approval for production use; about 1,500 endorsements on one thread | B | [Slashdot, 12 Feb 2026](https://tech.slashdot.org/story/26/02/12/1530202) |
| 28 Apr 2026 | Amazon Bedrock offers OpenAI models, Codex and Managed Agents (limited preview) | A | [AWS What's New](https://aws.amazon.com/about-aws/whats-new/2026/04/bedrock-openai-models-codex-managed-agents/) |
| 4 May 2026 | Note from VP Jim Haughwout: Claude Code available company-wide immediately; Codex from 12 May | B (+A spokesperson) | [dnyuz repost of Business Insider](https://dnyuz.com/2026/05/04/amazon-employees-pushed-for-claude-code-now-theyre-getting-it-and-codex-too/) |
| Late May 2026 | Internal "KiroRank" token-usage leaderboard taken offline after gaming | B (+A spokesperson) | [heise](https://www.heise.de/en/news/Too-much-tokenmaxxing-Amazon-stops-internal-AI-ranking-11311902.html) |
| 27 to 28 Oct 2026 | Amazon memo warns against "tokenmaxxing"; target of more than 80% of developers using AI weekly remains | B | [Fortune](https://fortune.com/2026/10/28/amazon-token-maxxing-ai-work/) |
| Nov 2026 | No new reporting on tool-access policy found in two extended searches | n/a | n/a |

**Claude Code (tool)**
- November 2025 to early May 2026: not formally approved for production use. Business Insider (February 2026) reported that internal guidance limited third-party AI coding tools for production code or live products unless teams went through a formal approval process, and that Kiro was pushed as the preferred option for production work. [Grade B, via secondary] — [TechRepublic](https://www.techrepublic.com/article/news-amazon-engineers-revolt-over-ai-tool-restrictions/); [Gigazine, 13 Feb 2026](https://gigazine.net/gsc_news/en/20260213-amazon-engineers-revolt-over-ai-tool-restrictions)
- Amazon's stated position in February 2026: there is no explicit ban on Claude Code, but stricter requirements apply to tools used for production. [Grade A, company statement via secondary; exact wording not seen] — [Slashdot, 12 Feb 2026](https://tech.slashdot.org/story/26/02/12/1530202); [dev.ua](https://dev.ua/en/news/inzhenery-amazon-obureni-im-ne-dozvoliaiut-vykorystovuvaty-claude-code-1770987520)
- 4 May 2026: Jim Haughwout, VP of Amazon Software Builder Experience, told staff in a note obtained by Business Insider that Claude Code would be available company-wide immediately. Claude Code is described as now approved for production use, with no special clearance needed. [Grade B] — [dnyuz repost of Business Insider, 4 May 2026](https://dnyuz.com/2026/05/04/amazon-employees-pushed-for-claude-code-now-theyre-getting-it-and-codex-too/); [The New Stack, 5 May 2026](https://thenewstack.io/amazon-coding-agents-developers/)
- Amazon spokesperson, May 2026: "Our builders are using Kiro for agentic coding, and now with both Claude Code and Codex running on AWS, we are making additional tools available as well." The spokesperson described the change as "standardizing" access. [Grade A, via secondary] — [Futurism](https://futurism.com/artificial-intelligence/amazon-kiro-coding); [Tekedia](https://www.tekedia.com/amazon-opens-the-gates-to-openai-and-anthropics-ai-coding-tools/)
- Both tools run on AWS and Amazon Bedrock inside Amazon. [Grade B] — [The New Stack, 5 May 2026](https://thenewstack.io/amazon-coding-agents-developers/)

**OpenAI Codex (tool)**
- October 2025: "Do Not Use" designation after about six months of assessment. [Grade B] — [The Star/Reuters](https://www.thestar.com.my/tech/tech-news/2025/11/25/amazon-pushes-in-house-ai-coding-tool-kiro-over-competitors039-memo-shows)
- 28 April 2026: Codex (CLI, desktop app, VS Code extension) became usable through Amazon Bedrock with AWS credentials, in limited preview, alongside OpenAI models including GPT-5.5. [Grade A] — [AWS What's New](https://aws.amazon.com/about-aws/whats-new/2026/04/bedrock-openai-models-codex-managed-agents/); [OpenAI](https://openai.com/index/openai-on-aws)
- 12 May 2026: company-wide internal rollout of Codex, per the Haughwout note. [Grade B] — [dnyuz repost of Business Insider](https://dnyuz.com/2026/05/04/amazon-employees-pushed-for-claude-code-now-theyre-getting-it-and-codex-too/); [Slashdot, 10 May 2026](https://tech.slashdot.org/story/26/05/10/0618225/)

**Cursor (tool)**
- June 2025: Business Insider, citing internal Slack messages, reported Amazon was in talks to adopt Cursor "as quickly as possible" after employee requests, with several high-priority security issues to resolve first. More than 1,500 employees had joined internal channels about Cursor. Amazon generally advised caution with outside AI tools. [Grade B] — [Reuters pickup](https://in.tradingview.com/news/reuters.com,2025:newsml_L3N3S61BX:0-amazon-is-in-talks-to-roll-out-ai-coding-assistant-cursor-internally-business-insider); [Business Insider Japan](https://www.businessinsider.jp/article/2506-amazon-deploy-cursor-employee-interest-spikes-ai-coding/)
- November 2025: Reuters read the Kiro memo as seeming to rule out Cursor. [Grade B, interpretation] — [The Star/Reuters](https://www.thestar.com.my/tech/tech-news/2025/11/25/amazon-pushes-in-house-ai-coding-tool-kiro-over-competitors039-memo-shows)

**Kiro (in-house tool) as the default**
- May 2026: Amazon says teams are still "primarily using" Kiro and gives a figure of 83% of engineers. Outlets phrase this differently ("83% use it", "adopted by 83%", "83% primarily use"). [Grade A, via secondary; metric definition unclear] — [The New Stack](https://thenewstack.io/amazon-coding-agents-developers/); [Futurism](https://futurism.com/artificial-intelligence/amazon-kiro-coding)

### Inferences
- Restriction levels by period: Claude Code was "approval required for production code, discouraged otherwise" (November 2025 to 3 May 2026), then "allowed under a company-provided arrangement running on Bedrock" (from 4 May 2026). Codex was "Do Not Use", the closest to a total ban (October 2025 to 11 May 2026), then allowed on the same basis (from 12 May 2026).
- The sequence "Codex on Bedrock in limited preview (28 April 2026), internal Codex access two weeks later (12 May 2026)" and the spokesperson's phrase "now with both Claude Code and Codex running on AWS" suggest that hosting on Amazon's own infrastructure was the condition that made approval possible, in addition to employee pressure.
- Amazon did not open the door to external tools in general. It approved two named tools that run on AWS. Tools that do not run on AWS (Cursor's hosted service, GitHub Copilot) are not covered by anything found.

### Gaps
- **Status after May 2026: may be outdated.** Two extended searches aimed at September to November 2026 returned no report of any change to Claude Code or Codex access. That is weak negative evidence only.
- No information found on GitHub Copilot's internal status at Amazon.
- Outcome of the June 2025 Cursor talks and Cursor's 2026 status: not found.
- Whether the "Do Not Use" label on Codex was formally lifted before 12 May 2026, and whether approval covers all code classifications (for example restricted or highly confidential repositories): not found.
- A search summary relayed a claim that exceptions to the production rule needed vice-president approval, without identifying the page it came from. Unattributed and unconfirmed; do not use as fact.
- Number of employees covered: The New Stack speaks of "tens of thousands of developers"; one low-quality site says about 50,000 corporate employees ([subagentic.ai](https://subagentic.ai/posts/amazon-kiro-claude-code-codex-rollout-2026/)). Neither figure is confirmed.
- The Business Insider originals (February and May 2026) were not read; details come from reposts and summaries.

---

## 3. Amazon, employees: pushback, numbers and leadership response

### Takeaway
The pushback is well documented at grade B. In February 2026 Business Insider reported internal messages in which engineers objected to being steered to Kiro and away from Claude Code for production work, with one forum thread collecting about 1,500 endorsements for formally adopting Claude Code. Amazon first defended the rules ("no explicit ban", Kiro usage figures), then reversed course in May 2026.

### Cited Findings
- About 1,500 employees endorsed one internal discussion thread calling for Amazon to formally adopt Claude Code (Business Insider, February 2026, internal messages reviewed). [Grade B] — [Slashdot, 12 Feb 2026](https://tech.slashdot.org/story/26/02/12/1530202); [Daily Jagran](https://www.thedailyjagran.com/technology/1500-amazon-engineers-push-for-claude-code-say-inhouse-ai-kiro-hurts-productivity-10298771)
- Some secondary coverage instead says "more than 1,000 staff supported a petition". This looks like a looser rendering of the same event, not a second event. [Secondary, inconsistent] — [UC Strategies](https://ucstrategies.com/news/amazon-restricts-claude-ai-for-developers-while-microsoft-pushes-teams-to-use-it/)
- Arguments reported: Claude Code is clearly stronger than Kiro; forcing a weaker tool could slow development. [Grade B] — [TechRepublic](https://www.techrepublic.com/article/news-amazon-engineers-revolt-over-ai-tool-restrictions/)
- Sales and solutions engineers who sell Claude through Bedrock found it awkward that they could not freely use Claude Code themselves. [Grade B] — [TechRepublic](https://www.techrepublic.com/article/news-amazon-engineers-revolt-over-ai-tool-restrictions/); [Gigazine](https://gigazine.net/gsc_news/en/20260213-amazon-engineers-revolt-over-ai-tool-restrictions)
- Leadership response, February 2026: Amazon said there was no explicit ban, only stricter requirements for production tools, and said about 70% of its software engineers had used Kiro at least once by January 2026. [Grade A, via secondary] — [Slashdot, 12 Feb 2026](https://tech.slashdot.org/story/26/02/12/1530202)
- Leadership response, May 2026: access opened (Haughwout note, 4 May 2026). Earlier internal messages had told staff to prioritise Kiro over non-approved tools such as Claude Code. [Grade B] — [dnyuz repost of Business Insider](https://dnyuz.com/2026/05/04/amazon-employees-pushed-for-claude-code-now-theyre-getting-it-and-codex-too/)
- The New Stack framed the May 2026 change as "the internal rebellion that changed Amazon's rules". [Secondary framing] — [The New Stack, 5 May 2026](https://thenewstack.io/amazon-coding-agents-developers/)
- Earlier demand signal, June 2025: more than 1,500 employees had joined internal Slack channels about Cursor. This is a different event from the 2026 Claude Code thread and the two figures should not be merged. [Grade B] — [Reuters pickup of Business Insider](https://in.tradingview.com/news/reuters.com,2025:newsml_L3N3S61BX:0-amazon-is-in-talks-to-roll-out-ai-coding-assistant-cursor-internally-business-insider)
- Blind thread titled "Amazonians are banned from using Claude" exists; content not read. [Grade C, title only] — [Blind](https://www.teamblind.com/post/amazonians-are-banned-from-using-claude-ggthy1jw)
- Related reaction to adoption pressure: employees gamed the internal KiroRank token leaderboard by running agents (Kiro and an in-house agent tool, MeshClaw) on unnecessary tasks. Senior VP Dave Treadwell is quoted as telling staff "Please don't use AI just for the sake of using AI." [Grade B, Financial Times first; via secondary; the page carrying the quote was not identified in the search summary] — [heise](https://www.heise.de/en/news/Too-much-tokenmaxxing-Amazon-stops-internal-AI-ranking-11311902.html); [HCA Mag](https://www.hcamag.com/us/specialization/hr-technology/amazon-shuts-down-ai-leaderboard-after-tokenmaxxing/577189)

### Inferences
- The size of the documented protest (about 1,500 endorsements) is small against Amazon's engineering headcount, but it was enough, together with Codex and Claude Code becoming AWS-hosted, to change the policy within about three months.
- The employee complaint was about tool quality (the harness), not model access: Kiro already ran Claude models (section 4).

### Gaps
- One aggregator dates the 1,500-engineer push to December 2025; Business Insider's report is from February 2026. The date the thread started is not established.
- No direct quotes from the internal thread were retrieved. No Hacker News or Reddit material was retrieved.
- Employee reaction after the May 2026 opening (actual uptake of Claude Code and Codex inside Amazon, effect on Kiro usage): not found.

---

## 4. Amazon, tool versus model

### Takeaway
For November 2025 to May 2026 the accurate description is "external tool restricted, external model allowed inside Amazon's own tool": Kiro ran on Anthropic's Claude models while Claude Code needed approval and Codex was labelled "Do Not Use". Since May 2026 both the external tools (Claude Code, Codex, on Bedrock) and external models are permitted, and Kiro itself has become multi-model (Claude, OpenAI GPT, open-weight models). No evidence was found that Amazon's own Nova models are the main coding models in Kiro.

### Cited Findings
- Kiro's official documentation lists Anthropic Claude models (Opus, Sonnet and Haiku generations, for example Opus 4.8, Sonnet 4.6, Haiku 4.5) and an "Auto" router that "combines multiple frontier models". Administrators can block Auto and set an approved default model. [Grade A, official docs via search summary] — [Kiro docs: models](https://kiro.dev/docs/models/); [Kiro docs: model selection](https://kiro.dev/docs/chat/model-selection/)
- Claude Opus 5 was added to the Kiro IDE and CLI model selector as an experimental preview on 24 July 2026, the day Anthropic announced it. [Grade B, third-party developer blog] — [Classmethod](https://dev.classmethod.jp/en/articles/kiro-ide-cli-claude-opus-5/)
- AWS announced in July 2026 that "Opus 4.8, Sonnet 5, and User Activity Monitoring" were available on Kiro in AWS GovCloud (US). [Grade A] — [AWS What's New, July 2026](https://aws.amazon.com/about-aws/whats-new/2026/07/kiro-opus-sonnet-monitoring-launch-aws-govcloud-us/)
- OpenAI models in Kiro: "GPT-5.6 brings OpenAI models to Kiro for the first time", in three tiers. A third-party list names later generally available models including Claude Sonnet 5.5 and GPT-5.6 variants. Date of the GPT-5.6 addition not established. [Grade A for the Kiro docs statement, B for the third-party list] — [Kiro docs: available models](https://kiro.dev/docs/models/available-models/); [Classmethod model list](https://dev.classmethod.jp/articles/kiro-claude-sonnet-5-5-ga-model-list/)
- Open-weight models in Kiro (IDE and CLI): DeepSeek V3.2, MiniMax M2.1 and M2.5, GLM-5, Qwen3 Coder Next, at low credit multipliers. [Grade A] — [Kiro blog: open weight models](https://kiro.dev/blog/open-weight-models/)
- External tools inside Amazon run on AWS and Bedrock (May 2026). [Grade B] — [The New Stack](https://thenewstack.io/amazon-coding-agents-developers/)
- Outside analysis: routing Claude Code and Codex through Bedrock keeps workloads, inference and cloud consumption inside Amazon's infrastructure. [Analysis, not a company statement] — [Tekedia](https://www.tekedia.com/amazon-opens-the-gates-to-openai-and-anthropics-ai-coding-tools/)
- Amazon is a major Anthropic investor (about $8 billion as of November 2025). [Grade B] — [The Star/Reuters](https://www.thestar.com.my/tech/tech-news/2025/11/25/amazon-pushes-in-house-ai-coding-tool-kiro-over-competitors039-memo-shows)

### Inferences
- Amazon's line was never drawn at the model. It was drawn at the harness and at where inference runs: Claude inside Kiro on Bedrock was always acceptable; Claude Code became acceptable once positioned as "running on AWS"; Codex became acceptable once OpenAI models and Codex were on Bedrock.
- For the wider question, Amazon is a "restricted the external tool, used the external model" case until May 2026, and an "allows both, prefers in-house tool" case afterwards.

### Gaps
- None of the Kiro model results mention Amazon Nova. Whether Nova models are used inside Kiro's Auto router or in Amazon's internal builds is not established.
- Which models Amazon's internal Kiro deployment uses by default, and whether it differs from the public product, is not established.
- UNVERIFIED LEAD: Kiro launched in preview in July 2025 on Claude Sonnet 4.0 and 3.7, and Amazon Q Developer has used Claude models through Bedrock since 2024. Not checked in this session.
- UNVERIFIED LEADS on the investment side: Amazon's reported investment of up to $50 billion in OpenAI (late February 2026) and an expanded Amazon and Anthropic agreement in April 2026. Not checked in this session.
- A GitHub issue reports Claude models disappearing from the Kiro selector ([Kiro issue 8055](https://github.com/kirodotdev/Kiro/issues/8055)); not read, relevance unknown.

---

## 5. Amazon, history and incidents

### Takeaway
Amazon's caution about external AI predates Kiro: a January 2023 legal warning about ChatGPT and a February 2024 rule prohibiting third-party generative AI tools for confidential work. Its documented AI-coding incidents involve its own tools, not external ones: the July 2025 compromise of the Amazon Q extension for VS Code, a December 2025 AWS outage linked by the Financial Times to Kiro, and March 2026 retail-site outages linked to "GenAI-assisted changes". The 2026 incidents led to tighter review of AI-assisted changes, not to a tighter external-tool policy.

### Cited Findings
- **January 2023, ChatGPT.** An Amazon senior corporate counsel told employees in Slack not to give ChatGPT "any Amazon confidential information (including Amazon code you are working on)", after responses resembling internal data were seen; inputs might be used as training data. ChatGPT was not banned; staff visiting the site were warned it was a third-party service not approved by Amazon Security. [Grade B, via secondary] — [Yahoo News](https://news.yahoo.com/amazon-warns-employees-beware-chatgpt-174500629.html); [OECD AI incident record](https://oecd.ai/en/incidents/2023-01-25-258a); [Gizmodo](https://gizmodo.com/amazon-chatgpt-ai-software-job-coding-1850034383)
- **February 2024, formal rule.** Internal documents stated that use of third-party generative AI tools such as ChatGPT for confidential Amazon work is "strictly prohibited", citing the risk that providers take a licence to or ownership of inputs. Spokesperson: "We have safeguards in place for employee use of these technologies, including guidance on accessing third-party generative AI services and protecting confidential information". [Grade B plus A statement, via secondary] — [Benzinga](https://benzinga.com/z/37278112); [Business Today, 25 Feb 2024](https://businesstoday.in/technology/story/amazon-stops-employees-from-using-generative-ai-tools-like-chatgpt-418845-2024-02-25)
- **July 2025, Amazon Q extension for VS Code.** An attacker used an inappropriately scoped GitHub token to commit a prompt into the open-source repository instructing the agent to wipe the system and cloud resources. It shipped in version 1.84.0 (pull request dated 13 July, release 17 July 2025). AWS acknowledged it on 23 July and released 1.85.0. AWS said the code failed to execute because of a syntax error and no customer resources were affected. The extension had more than 964,000 installs. First reported by 404 Media. [Grade A for AWS bulletin, B for reporting] — [The Register, 24 Jul 2025](https://www.theregister.com/2025/07/24/amazon_q_ai_prompt/); [404 Media](https://404media.co/hacker-plants-computer-wiping-commands-in-amazons-ai-coding-agent); [iTnews](https://www.itnews.com.au/news/code-syntax-error-prevented-hacked-aws-ai-dev-extension-from-running-619054)
- **December 2025 outage, reported February 2026.** The Financial Times, citing four sources, reported that a 13-hour AWS outage in December 2025 affecting a customer-facing cost tool followed engineers letting Kiro make changes; the tool decided to "delete and recreate the environment". A second outage was tied to Amazon Q Developer. In both cases engineers did not need a second person's approval. Amazon's on-record reply: it was a "coincidence that AI tools were involved" and the cause was "user error, not AI error". [Grade B, with A-grade denial] — [The Register, 20 Feb 2026](https://www.theregister.com/2026/02/20/amazon_denies_kiro_agentic_ai_behind_outage/); [Engadget](https://engadget.com/ai/13-hour-aws-outage-reportedly-caused-by-amazons-own-ai-tools-170930190.html); [Tom's Hardware](https://www.tomshardware.com/tech-industry/artificial-intelligence/multiple-aws-outages-caused-by-ai-coding-bot-blunder-report-claims-amazon-says-both-incidents-were-user-error)
- **March 2026 retail outages and review rule.** After several outages, including a faulty deployment that left shoppers unable to check out or see prices for about six hours, the Financial Times reported a note from Dave Treadwell saying "junior and mid-level engineers will now require more senior engineers to sign off any AI-assisted changes". Internal briefings spoke of "GenAI-assisted changes" with "high blast radius". Amazon told Business Insider it was "not accurate" that all AI changes need sign-off, and said AWS was not involved in these incidents. [Grade B, contested by company] — [TechRadar](https://www.techradar.com/pro/amazon-is-making-even-senior-engineers-get-code-signed-off-following-multiple-recent-outages); [Tom's Hardware](https://www.tomshardware.com/tech-industry/artificial-intelligence/amazon-calls-engineers-to-address-issues-caused-by-use-of-ai-tools-report-claims-company-says-recent-incidents-had-high-blast-radius-and-were-allegedly-related-to-gen-ai-assisted-changes); [CIO](https://www.cio.com/article/4143381/amazon-is-linking-site-hiccups-to-ai-efforts-2.html); [Ars Technica/FT via Ground News](https://ground.news/article/after-outages-amazon-to-make-senior-engineers-sign-off-on-ai-assisted-changes)
- **Sales performance context, September 2025.** A Business Insider piece was headlined on Amazon Q lagging competitors in the AI coding revenue race. [Headline only, not read] — [Business Insider Japan](https://www.businessinsider.jp/article/amazon-q-lags-revenue-race-competitors-ai-coding-2025-9/)

### Inferences
- The 2026 incidents cut against the simple story "in-house tool is the safe option": the tools named in the outage reports were Kiro and Amazon Q Developer, not Claude Code or Codex.
- The tightening after incidents was about process (peer and senior review of AI-assisted changes), and it applies whatever tool produced the change. Nothing found links any incident to a tightening of external-tool access. External access was in fact loosened two months after the March 2026 outages.

### Gaps
- No evidence found of rules specific to the CodeWhisperer era (2023 to 2024) on external coding assistants such as GitHub Copilot. The search budget ran out before this could be searched.
- One report says the senior sign-off rule was set for 90 days; not confirmed. Whether it is still in force in November 2026 is unknown (may be outdated).
- UNVERIFIED LEAD: the December 2025 outage is recalled as affecting AWS Cost Explorer in a mainland China region, and Amazon is recalled as saying it added mandatory peer review for production access afterwards. Not checked in this session.
- No 2026 security incident (as opposed to outage) tied to an external AI coding tool inside Amazon was found.

---

## 6. Microsoft and Claude Code: adoption, then withdrawal in favour of GitHub Copilot CLI

### Takeaway
The lead is confirmed at grade B and the sequel is a partial withdrawal. In January 2026 The Verge reported that Microsoft had CoreAI and Experiences + Devices engineers install Claude Code and compare it with GitHub Copilot. On 14 May 2026 The Verge reported that Experiences + Devices would cancel most Claude Code licences by 30 June 2026 and converge on GitHub Copilot CLI, on a memo from executive vice president Rajesh Jha, with cost also a factor. Claude models stay available inside Copilot CLI. No source dated after 1 July 2026 was found, so the November 2026 position is not confirmed and "may be outdated".

### Cited Findings

**Timeline (Microsoft)**

| Date | Event | Grade | Source |
|---|---|---|---|
| Late 2025 | CoreAI team tests Claude Code "for several months" | B | [LeadDev](https://leaddev.com/ai/why-microsoft-engineers-are-using-claude-code); [AI CERTs](https://www.aicerts.ai/news/?p=14869) |
| Nov 2025 | Claude models become available in Microsoft Foundry | A | [Anthropic](https://www.anthropic.com/news/claude-in-microsoft-foundry) |
| 22 Jan 2026 | The Verge: "Claude Code is suddenly everywhere inside Microsoft"; CoreAI and Experiences + Devices told to install it | B | [heise](https://heise.de/-11152581); [AI CERTs](https://www.aicerts.ai/news/?p=14869); [headline as relayed by AI Intensify](https://ai-intensify.com/claude-code-everywhere-inside-microsoft/) |
| 14 May 2026 | The Verge (Tom Warren, Notepad): "Microsoft starts canceling Claude Code licenses" | B | [The Verge URL as indexed](https://www.theverge.com/tech/930447/microsoft-claude-code-discontinued-notepad); [WinBuzzer, 15 May 2026](https://winbuzzer.com/2026/05/15/microsoft-starts-canceling-claude-code-licenses-xcxwbn/) |
| 30 Jun 2026 | Planned cutoff for most Experiences + Devices Claude Code licences (fiscal year end) | B | [The Decoder, 15 May 2026](https://the-decoder.com/microsoft-pulls-claude-code-licenses-and-pushes-developers-back-toward-its-own-ai-tool/) |
| 1 Jul 2026 | Microsoft researchers publish study of the early-2026 rollout of both CLI agents | A | [arXiv 2607.01418](https://arxiv.org/abs/2607.01418) |
| Jul to Nov 2026 | No reporting found in two extended searches | n/a | n/a |

- **January 2026 rollout.** The Verge (reported 22 January 2026) said Microsoft asked engineers across Windows, Microsoft 365, Teams and Surface to install Claude Code. CoreAI and the Experiences + Devices division received instructions to install it. CoreAI had been testing it for several months. Engineers were expected to use Claude Code and GitHub Copilot side by side and give comparison feedback. [Grade B, via secondary] — [LeadDev](https://leaddev.com/ai/why-microsoft-engineers-are-using-claude-code); [Windows Report](https://windowsreport.com/microsoft-quietly-adopts-claude-code-while-marketing-github-copilot/); [PC Gamer](https://www.pcgamer.com/software/ai/not-even-microsoft-is-confident-that-copilot-is-right-for-coding-as-its-encouraging-staff-to-try-out-anthropics-ai-model-according-to-one-report/)
- **Non-developers included.** Thousands of employees, including designers and project managers, were encouraged to try Claude Code for prototyping. [Grade B, via secondary] — [Storyboard18](https://www.storyboard18.com/amp/digital/microsoft-asks-non-developer-employees-to-code-using-ai-88206.htm); [AI CERTs](https://www.aicerts.ai/news/?p=14869)
- **Microsoft's framing in January 2026.** Communications chief Frank Shaw is reported to have framed it as normal evaluation of competing products. [Grade A statement, seen only through a secondary aggregator; wording not confirmed] — [AI CERTs](https://www.aicerts.ai/news/?p=14869)
- **May 2026 reversal, scope.** The Experiences + Devices group (Windows, Microsoft 365, Outlook, Teams, Surface) "is winding down most use of Claude Code with a cutoff on June 30, 2026", the end of Microsoft's fiscal year. [Grade B] — [Gigazine, 18 May 2026](https://gigazine.net/gsc_news/en/20260518-microsoft-discontinued-claude-code); [Windows Central](https://www.windowscentral.com/microsoft/microsoft-cancels-claude-code-licenses-shifting-developers-to-github-copilot-cli-a-move-likely-driven-by-financial-motives); [TechRadar](https://www.techradar.com/pro/microsoft-may-discontinue-claude-code-internally-as-it-looks-to-push-users-towards-github-copilot)
- **Whose decision and stated reason.** An internal memo from Rajesh Jha, executive vice president of Experiences + Devices, seen by The Verge's Notepad: Microsoft offered both tools to learn and benchmark them in real engineering workflows; Claude Code was an important part of that learning; Copilot CLI is a product Microsoft can help shape directly with GitHub. [Grade B, memo paraphrased] — [The Verge article via HN reader](https://hn.jakechampion.name/reader/https://www.theverge.com/tech/930447/microsoft-claude-code-discontinued-notepad); [Developer Tech](https://www.developer-tech.com/news/microsoft-claude-code-github-copilot-cli/); [Harian Basis](https://www.harianbasis.co/en/microsoft-replaces-claude-github-copilot)
- **Reported reasons beyond the memo.** Tom Warren, as quoted in secondary coverage: "the decision is about converging on Copilot CLI as its main agentic command line interface tool across Experiences + Devices, but sources tell me the decision is also a financial one." Also: "While Claude Code has been a popular addition, it has also undermined Microsoft's new GitHub Copilot CLI coding tool." [Grade B] — [Windows Central](https://www.windowscentral.com/microsoft/microsoft-cancels-claude-code-licenses-shifting-developers-to-github-copilot-cli-a-move-likely-driven-by-financial-motives); [The Verge article via HN reader](https://hn.jakechampion.name/reader/https://www.theverge.com/tech/930447/microsoft-claude-code-discontinued-notepad)
- **Microsoft statement, May 2026.** Microsoft said it is "partnering closely with GitHub and continue[s] to improve Copilot CLI for Microsoft engineers". [Grade A, via secondary] — [Harian Basis](https://www.harianbasis.co/en/microsoft-replaces-claude-github-copilot)
- **What stays.** Anthropic's models remain available through Copilot CLI, in Microsoft 365 apps and through the Foundry arrangement; only the Claude Code harness is being removed. The wider Microsoft and Anthropic commercial relationship is unaffected. [Grade B] — [The Decoder](https://the-decoder.com/microsoft-pulls-claude-code-licenses-and-pushes-developers-back-toward-its-own-ai-tool/); [Analytics India Magazine](https://analyticsindiamag.com/ai-news/microsoft-replaces-claude-code-with-github-copilot-cli)
- **Transition detail.** Microsoft had not described an extension path for engineers mid-project at the cutoff; feature gaps between Claude Code and Copilot CLI were noted. [Grade B, secondary] — [WinBuzzer](https://winbuzzer.com/2026/05/15/microsoft-starts-canceling-claude-code-licenses-xcxwbn/); [The Decoder](https://the-decoder.com/microsoft-pulls-claude-code-licenses-and-pushes-developers-back-toward-its-own-ai-tool/)
- **Jha's status.** One report says Jha oversees the division until his retirement in July 2026. [Secondary, single source] — [Gigazine](https://gigazine.net/gsc_news/en/20260518-microsoft-discontinued-claude-code)
- **Employee sentiment.** Claude Code was popular internally and developers reportedly preferred it to Copilot CLI. [Grade B, via secondary] — [TechRadar](https://www.techradar.com/pro/microsoft-may-discontinue-claude-code-internally-as-it-looks-to-push-users-towards-github-copilot)
- **Microsoft's own study (1 July 2026).** Emerson Murphy-Hill, Jenna Butler and Alexandra Savelieva studied the early-2026 rollout of Claude Code and GitHub Copilot CLI across eligible Microsoft engineers. First use spread mainly through social networks; retention tracked coding activity; early adopters showed a sustained 24% increase in output (merged pull requests over about four months) against a synthetic counterfactual. The paper hypothesises that Copilot CLI's higher uptake may reflect Microsoft owning GitHub and aligning that harness with internal workflows. [Grade A for the paper, read only through summaries] — [arXiv 2607.01418](https://arxiv.org/abs/2607.01418); [TechRepublic](https://www.techrepublic.com/article/news-ai-coding-agents-microsoft-pull-requests/)
- **Contradictions between sources.** Most outlets say "most" licences in Experiences + Devices by 30 June. One consultancy blog asserts the group is "fully off" Claude Code ([EPC Group](https://www.epcgroup.net/blog/github-copilot-cli-replacing-claude-code-microsoft-engineers-2026)); one aggregator names the replacement as "GitHub Copilot Desktop" ([Awesome Agents](https://awesomeagents.ai/news/microsoft-drops-claude-code-copilot-desktop/)). Neither is corroborated; treat The Verge's "most licences" and "Copilot CLI" as the reliable version.

### Inferences
- Restriction levels by period for Claude Code at Microsoft: "encouraged, company-paid, alongside Copilot" (January to June 2026, CoreAI and Experiences + Devices); from 1 July 2026 "licences withdrawn for most Experiences + Devices engineers, officially steered to Copilot CLI". Nothing found describes this as a ban on the tool; it is a withdrawal of paid seats plus a designated default.
- "Most" implies some licences continue, but no source says for whom.
- The paper's remark about Copilot CLI's higher uptake sits in tension with The Verge's line that Claude Code "undermined" Copilot CLI. Both can be true (Claude Code popular enough to worry management while Copilot CLI had more total users), but the notes cannot resolve it without the paper's figures.
- Amazon and Microsoft crossed over in the first half of 2026: in February 2026 Amazon was restricting Claude Code while Microsoft was pushing teams to use it ([UC Strategies](https://ucstrategies.com/news/amazon-restricts-claude-ai-for-developers-while-microsoft-pushes-teams-to-use-it/)); by July 2026 Amazon had approved it company-wide and Microsoft had withdrawn most seats in its largest product group.

### Gaps
- **Position in November 2026 not confirmed (may be outdated).** Two extended searches (post-30 June; August to October 2026) found nothing newer than the 1 July 2026 arXiv paper. Whether the cutoff happened as planned, whether it was extended, and whether it spread to other groups is unknown.
- CoreAI's status after June 2026 (the group that first tested Claude Code) is not reported in anything found.
- The Verge originals (January and May 2026) were not read. Seat counts are not given beyond "thousands".
- UNVERIFIED LEAD: The Verge's January 2026 piece is recalled as saying Claude Code was approved across all code and repositories for the Business and Industry Copilot teams, and as quoting Frank Shaw that companies regularly test competing products and that OpenAI remains Microsoft's primary partner and model provider. A search for this did not confirm it.
- No Blind, Hacker News or Reddit material on employee reaction to the licence cancellations was retrieved.
- A Memeburn piece frames the pullback as AI costs reshaping big tech ([Memeburn](https://memeburn.com/microsoft-pulls-back-claude-code-as-ai-costs-start-reshaping-big-tech/)); not read, no cost figures obtained.

---

## 7. Microsoft, other tools and models (Codex, Cursor, Anthropic models in Microsoft products, GitHub and LinkedIn)

### Takeaway
Only the model side is documented in what was found: Anthropic models remain available to Microsoft engineers inside GitHub Copilot CLI and Microsoft products, so Microsoft after June 2026 fits "external tool withdrawn, external model allowed inside own tool". No evidence was found, in either direction, on OpenAI Codex or Cursor use inside Microsoft, or on separate rules at GitHub or LinkedIn. The search budget ran out before these could be searched.

### Cited Findings
- Claude models remain accessible to Microsoft engineers through Copilot CLI, Microsoft 365 apps and Foundry after the Claude Code wind-down. [Grade B] — [The Decoder, 15 May 2026](https://the-decoder.com/microsoft-pulls-claude-code-licenses-and-pushes-developers-back-toward-its-own-ai-tool/); [Harian Basis](https://www.harianbasis.co/en/microsoft-replaces-claude-github-copilot)
- Claude models have been offered in Microsoft Foundry since the November 2025 agreement. [Grade A] — [Anthropic](https://www.anthropic.com/news/claude-in-microsoft-foundry)
- The change of May 2026 concerns internal engineering use only; Anthropic's product and the commercial partnership are unaffected. [Grade B] — [Gigazine, 18 May 2026](https://gigazine.net/gsc_news/en/20260518-microsoft-discontinued-claude-code)
- January 2026 commentary read Microsoft's internal use of Claude Code as a possible blow to OpenAI. [Commentary] — [AOL/Motley Fool](https://www.aol.com/articles/microsoft-directed-engineers-claude-code-155429103.html)

### Inferences
- Microsoft's internal preference is for its own harness (GitHub Copilot, Copilot CLI), not for its own or its partner's model. This mirrors Amazon before May 2026.
- The November 2025 partnership with Anthropic explains why Claude models are a first-class option inside Copilot products. It did not protect the Claude Code harness from being displaced internally.

### Gaps
- **No evidence found** on whether OpenAI Codex (the tool) or Cursor is permitted, restricted or licensed for Microsoft engineers. Not searched (budget exhausted). Do not infer a ban or a permission.
- **No evidence found** on whether GitHub or LinkedIn engineers follow different rules. Not searched.
- UNVERIFIED LEAD: on 18 November 2025 Microsoft, Nvidia and Anthropic announced a partnership (Anthropic committing about $30 billion of Azure compute; Nvidia up to $10 billion and Microsoft up to $5 billion of investment in Anthropic; Claude in Foundry and across the Copilot family). Figures not checked in this session; only the Foundry availability is sourced above.
- UNVERIFIED LEADS: GitHub's Agent HQ (announced late October 2025) brought third-party agents including Claude and Codex inside GitHub Copilot subscriptions; Microsoft added Anthropic models to Microsoft 365 Copilot in September 2025; The Information reported in 2025 that VS Code's automatic model selection favoured Claude Sonnet 4. None checked in this session.
- A headline from The Information, "Microsoft memo details AI app overhaul: 'earn right to exist'", surfaced but was not read; relevance unknown ([The Information](https://www.theinformation.com/articles/microsoft-memo-details-ai-app-overhaul-earn-right-exist?rc=mf8uqd)).

---

## 8. Microsoft, history of earlier restrictions

### Takeaway
None of the historical leads (the brief ChatGPT block in 2023, the DeepSeek app ban in 2025, any rule on competing AI tools) could be verified in this session, because the search budget ran out before those searches ran. They are neither confirmed nor refuted here.

### Cited Findings
- No sourced findings. The only historically relevant item sourced in this session is the contrast noted in February 2026 coverage that Microsoft was then pushing teams to use Claude Code while Amazon restricted it. [Secondary] — [UC Strategies](https://ucstrategies.com/news/amazon-restricts-claude-ai-for-developers-while-microsoft-pushes-teams-to-use-it/)

### Inferences
- None that rest on sourced findings.

### Gaps
- **Not verified, search not performed: temporary ChatGPT block.** UNVERIFIED LEAD: CNBC reported on 9 November 2023 that Microsoft briefly blocked employee access to ChatGPT citing security and data concerns, then restored it and called the block a mistake made while testing endpoint controls for large language models. Recalled, not checked.
- **Not verified, search not performed: DeepSeek.** UNVERIFIED LEAD: Microsoft president Brad Smith told a US Senate hearing in May 2025 that Microsoft employees are not allowed to use the DeepSeek app, citing data and propaganda risks, while Microsoft offered a DeepSeek model on Azure after safety review. Recalled, not checked.
- **Not verified: early guidance.** UNVERIFIED LEAD: Business Insider reported in January 2023 that Microsoft told employees not to share sensitive data with ChatGPT. Recalled, not checked.
- No evidence found of a standing Microsoft rule that forbids competing AI coding tools as such.

---

## 9. Both companies: enforcement, adoption numbers and executive statements

### Takeaway
Enforcement in both companies works through procurement and approval, not through reported technical blocks or discipline: Amazon used tool-status labels ("Do Not Use"), a formal approval process for production use and an "approved for production" list; Microsoft cancelled paid licences at fiscal year end. No report of network or device blocking of Claude Code or Codex, or of disciplinary action, was found for either company. Adoption figures are available for Kiro (70% in January 2026, 83% in May 2026, a target above 80% weekly AI use) and for Microsoft's CLI-agent rollout (24% more merged pull requests among early adopters). Executive statements on dogfooding could not be searched.

### Cited Findings

**Amazon: enforcement**
- Tool-status designation: Codex "Do Not Use" (October 2025); Claude Code briefly the same, then reversed. [Grade B] — [The Star/Reuters](https://www.thestar.com.my/tech/tech-news/2025/11/25/amazon-pushes-in-house-ai-coding-tool-kiro-over-competitors039-memo-shows)
- Formal approval process for third-party tools on production code or live products (February 2026); "special clearance" was needed for Claude Code until 4 May 2026. [Grade B] — [TechRepublic](https://www.techrepublic.com/article/news-amazon-engineers-revolt-over-ai-tool-restrictions/); [dnyuz repost of Business Insider](https://dnyuz.com/2026/05/04/amazon-employees-pushed-for-claude-code-now-theyre-getting-it-and-codex-too/)
- Soft technical control in 2023: a warning shown to staff visiting ChatGPT that it was a third-party service not approved by Amazon Security. [Grade B] — [Yahoo News](https://news.yahoo.com/amazon-warns-employees-beware-chatgpt-174500629.html)
- Review control after outages (March 2026): senior sign-off for AI-assisted changes by junior and mid-level engineers, per the Financial Times; scope disputed by Amazon. [Grade B, contested] — [TechRadar](https://www.techradar.com/pro/amazon-is-making-even-senior-engineers-get-code-signed-off-following-multiple-recent-outages)

**Amazon: numbers**
- About 70% of Amazon software engineers had used Kiro at least once by January 2026 (company statement, February 2026). [Grade A, via secondary] — [Slashdot, 12 Feb 2026](https://tech.slashdot.org/story/26/02/12/1530202)
- 83% of engineers use Kiro (company statement, May 2026; phrased inconsistently across outlets). [Grade A, via secondary] — [The New Stack](https://thenewstack.io/amazon-coding-agents-developers/)
- Target: more than 80% of developers using AI each week, still in place in late October 2026. No cap on employee token usage as of that reporting. [Grade B] — [Fortune, 28 Oct 2026](https://fortune.com/2026/10/28/amazon-token-maxxing-ai-work/); [Reworked](https://www.reworked.co/ai-news/amazon-to-engineers-not-using-ai-can-be-a-finding/)
- KiroRank: an employee-built beta leaderboard scoring staff on Kiro AI activity, taken offline in late May 2026 (one outlet gives 29 May) after staff inflated token use. Amazon: "the beta dashboard was not a formal or approved tool, and has since been deprecated." Replacement metric reported: "normalised deployments". Financial Times first. [Grade B plus A statement] — [heise](https://www.heise.de/en/news/Too-much-tokenmaxxing-Amazon-stops-internal-AI-ranking-11311902.html); [MLQ](https://mlq.ai/news/amazon-scraps-internal-ai-leaderboard-after-employees-gamed-it-with-fake-tasks/)
- October 2026 memo (dated 27 October by two secondary sources, reported by Fortune on 28 October): token consumption, AI adoption rates and counts of code changes or reviews are "deeply flawed" measures of good work; a headline adds that not using AI "can be a finding". [Grade B; memo text not read] — [Fortune](https://fortune.com/2026/10/28/amazon-token-maxxing-ai-work/); [Reworked](https://www.reworked.co/ai-news/amazon-to-engineers-not-using-ai-can-be-a-finding/); [Startup Fortune](https://startupfortune.com/amazon-tells-engineers-coding-skill-alone-no-longer-defines-the-job/)

**Amazon: executive statements found**
- Peter DeSantis and Dave Treadwell (memo, November 2025): "We're making Kiro our recommended AI-native development tool for Amazon." [Grade A] — [The Star/Reuters](https://www.thestar.com.my/tech/tech-news/2025/11/25/amazon-pushes-in-house-ai-coding-tool-kiro-over-competitors039-memo-shows)
- Dave Treadwell (2026, as relayed in coverage of the KiroRank closure): "Please don't use AI just for the sake of using AI." [Grade B, via secondary] — [HCA Mag](https://www.hcamag.com/us/specialization/hr-technology/amazon-shuts-down-ai-leaderboard-after-tokenmaxxing/577189)

**Microsoft: enforcement and numbers**
- Enforcement mechanism: cancellation of most Claude Code licences in Experiences + Devices at fiscal year end, 30 June 2026. [Grade B] — [The Decoder](https://the-decoder.com/microsoft-pulls-claude-code-licenses-and-pushes-developers-back-toward-its-own-ai-tool/)
- Scale: "thousands" of employees had been encouraged to use Claude Code in January 2026; no seat count published. [Grade B, via secondary] — [AI CERTs](https://www.aicerts.ai/news/?p=14869)
- Measured effect: sustained 24% increase in merged pull requests for early adopters of the CLI agents (Claude Code and Copilot CLI together) over about four months. [Grade A paper, via summaries] — [arXiv 2607.01418](https://arxiv.org/abs/2607.01418)
- Rajesh Jha (memo, May 2026): Copilot CLI is a product Microsoft can help shape directly with GitHub. [Grade B, paraphrase] — [Developer Tech](https://www.developer-tech.com/news/microsoft-claude-code-github-copilot-cli/)

### Inferences
- In both companies the binding constraint is "what the company pays for and approves", not a technical block. An engineer's ability to use an external tool on a personal basis for non-proprietary work is not addressed by any source found.
- Both companies pair restriction of external harnesses with strong pressure to use AI in general (Amazon's above-80% weekly target; Microsoft's side-by-side mandate in early 2026). "Ban" in headlines usually means "not approved for production" or "no company licence".
- Cost has become an explicit driver in 2026: Microsoft's decision was "also a financial one", and Amazon shut KiroRank after wasteful token use.

### Gaps
- No evidence found of network or device blocking, licence audits or disciplinary action over Claude Code, Codex or Cursor at either company.
- Share of code written by AI: no figure sourced for either company in this session. UNVERIFIED LEADS: Satya Nadella is recalled as saying in April 2025 that perhaps 20% to 30% of code in some Microsoft repositories was written by AI; Kevin Scott is recalled as predicting 95% of code would be AI-generated within about five years; Andy Jassy is recalled as citing large savings from Amazon Q code transformation in 2024 and strong Kiro uptake on the October 2025 earnings call. None checked.
- No on-record statements from Andy Jassy, Matt Garman, Satya Nadella, Kevin Scott or Jay Parikh on using the company's own coding tools were retrieved. Searches not performed.
- A TheStreet headline, "Amazon joins Microsoft in sending shocking message to employees", suggests a comparable Microsoft message on AI usage in autumn 2026; not read, content unknown ([TheStreet](https://www.thestreet.com/technology/amazon-joins-microsoft-in-sending-shocking-message-to-employees)).
- Searches planned but not performed (budget exhausted): Fortune October 2026 details; The Verge January 2026 details; Microsoft internal AI cost and token budgets; Microsoft policy on Cursor and Codex; Microsoft ChatGPT block 2023; Microsoft DeepSeek ban 2025; exact Amazon February 2026 statement wording; Financial Times December 2025 outage details.

---

## Summary table (requested by the assignment)

"Latest confirmation" is the date of the newest source found that supports the row. Rows not reconfirmed after mid-2026 are marked "may be outdated".

| Company | External tool or model | Restriction level | In-house alternative | Date of latest confirmation | Evidence grade | Sources |
|---|---|---|---|---|---|---|
| Amazon | Claude Code (tool) | Nov 2025 to 3 May 2026: approval required for production code; otherwise officially unsupported or discouraged ("no explicit ban" per Amazon); briefly labelled "Do Not Use" at some point before 24 Nov 2025 (date not given), then reversed. From 4 May 2026: allowed company-wide including production, as a company-provided tool running on AWS and Bedrock. | Kiro (recommended default); Amazon Q Developer | May 2026 (may be outdated; no reversal found in searches through Nov 2026) | B, with A-grade spokesperson statements via secondary | [Reuters via The Star](https://www.thestar.com.my/tech/tech-news/2025/11/25/amazon-pushes-in-house-ai-coding-tool-kiro-over-competitors039-memo-shows); [Slashdot Feb 2026](https://tech.slashdot.org/story/26/02/12/1530202); [BI repost May 2026](https://dnyuz.com/2026/05/04/amazon-employees-pushed-for-claude-code-now-theyre-getting-it-and-codex-too/); [The New Stack](https://thenewstack.io/amazon-coding-agents-developers/) |
| Amazon | OpenAI Codex (tool) | Oct 2025 to 11 May 2026: "Do Not Use" designation (closest to a total ban). From 12 May 2026: allowed company-wide, running on AWS and Bedrock. | Kiro | May 2026 (may be outdated) | B | [Reuters via The Star](https://www.thestar.com.my/tech/tech-news/2025/11/25/amazon-pushes-in-house-ai-coding-tool-kiro-over-competitors039-memo-shows); [BI repost May 2026](https://dnyuz.com/2026/05/04/amazon-employees-pushed-for-claude-code-now-theyre-getting-it-and-codex-too/); [AWS](https://aws.amazon.com/about-aws/whats-new/2026/04/bedrock-openai-models-codex-managed-agents/) |
| Amazon | Cursor (tool) | Jun 2025: not approved, adoption talks under way pending security fixes. Nov 2025: covered by "no additional third-party tools" (Reuters' reading). 2026: no evidence found. | Kiro | Nov 2025 (may be outdated) | B | [Reuters pickup of BI](https://in.tradingview.com/news/reuters.com,2025:newsml_L3N3S61BX:0-amazon-is-in-talks-to-roll-out-ai-coding-assistant-cursor-internally-business-insider); [Reuters via The Star](https://www.thestar.com.my/tech/tech-news/2025/11/25/amazon-pushes-in-house-ai-coding-tool-kiro-over-competitors039-memo-shows) |
| Amazon | GitHub Copilot (tool) | No evidence found | Kiro; Amazon Q Developer | none | none | none |
| Amazon | ChatGPT and other third-party generative AI services (tool) | Banned for confidential work ("strictly prohibited" for confidential Amazon work, Feb 2024); warning only in Jan 2023 | Not researched | Feb 2024 (may be outdated) | B | [Yahoo News](https://news.yahoo.com/amazon-warns-employees-beware-chatgpt-174500629.html); [Business Today](https://businesstoday.in/technology/story/amazon-stops-employees-from-using-generative-ai-tools-like-chatgpt-418845-2024-02-25) |
| Amazon | Anthropic Claude (model) | Allowed and used inside Kiro throughout; via Bedrock | Nova (no evidence it is the main coding model) | Jul 2026 (AWS notice); Kiro docs as indexed Nov 2026 | A (official docs and AWS notice, via search summaries) | [Kiro docs](https://kiro.dev/docs/models/); [AWS, Jul 2026](https://aws.amazon.com/about-aws/whats-new/2026/07/kiro-opus-sonnet-monitoring-launch-aws-govcloud-us/) |
| Amazon | OpenAI GPT (model) | Allowed: on Bedrock from 28 Apr 2026 (limited preview); GPT-5.6 selectable in Kiro (date not established) | Nova | 2026 (Kiro docs as indexed Nov 2026) | A | [AWS](https://aws.amazon.com/about-aws/whats-new/2026/04/bedrock-openai-models-codex-managed-agents/); [Kiro docs](https://kiro.dev/docs/models/available-models/) |
| Amazon | Open-weight models (DeepSeek, MiniMax, GLM, Qwen) (model) | Allowed inside Kiro | Nova | 2026 | A | [Kiro blog](https://kiro.dev/blog/open-weight-models/) |
| Microsoft | Claude Code (tool) | Jan to Jun 2026: encouraged, company-paid, alongside Copilot (CoreAI, Experiences + Devices). From 1 Jul 2026 (planned): most Experiences + Devices licences cancelled; officially steered to Copilot CLI. Not reported as a ban. CoreAI and other groups: unknown. | GitHub Copilot CLI; GitHub Copilot | May 2026 (may be outdated; no source after 1 Jul 2026) | B | [The Verge as indexed](https://www.theverge.com/tech/930447/microsoft-claude-code-discontinued-notepad); [The Decoder](https://the-decoder.com/microsoft-pulls-claude-code-licenses-and-pushes-developers-back-toward-its-own-ai-tool/); [Windows Central](https://www.windowscentral.com/microsoft/microsoft-cancels-claude-code-licenses-shifting-developers-to-github-copilot-cli-a-move-likely-driven-by-financial-motives); [LeadDev](https://leaddev.com/ai/why-microsoft-engineers-are-using-claude-code) |
| Microsoft | OpenAI Codex (tool) | No evidence found | GitHub Copilot | none | none | none |
| Microsoft | Cursor (tool) | No evidence found | GitHub Copilot; VS Code | none | none | none |
| Microsoft | Anthropic Claude (model) | Allowed inside Copilot CLI, Microsoft 365 apps and Foundry | OpenAI models via partnership; Microsoft's own models (not researched) | May 2026 (may be outdated) | B, with A for Foundry availability | [The Decoder](https://the-decoder.com/microsoft-pulls-claude-code-licenses-and-pushes-developers-back-toward-its-own-ai-tool/); [Anthropic](https://www.anthropic.com/news/claude-in-microsoft-foundry) |
| Microsoft | ChatGPT (tool), temporary block Nov 2023 | Not verified in this session (unverified lead: brief block, reversed as a mistake) | Not researched | none this session | unverified | none |
| Microsoft | DeepSeek app (tool), 2025 | Not verified in this session (unverified lead: employees not allowed to use the app) | Not researched | none this session | unverified | none |

**One-line answer for the wider question, on the evidence found:** neither company had, as of the latest confirmed reporting, a standing total ban on Claude Code. Amazon restricted Claude Code (approval for production) and effectively banned Codex ("Do Not Use") from late 2025 until May 2026, then approved both on its own infrastructure while keeping Kiro as the recommended default. Microsoft went the other way: it encouraged Claude Code alongside Copilot in early 2026, then withdrew most licences in its largest product group by mid-2026 in favour of GitHub Copilot CLI. In both cases the external model (Claude) stayed available inside the in-house tool. The November 2026 status of both is not directly confirmed.
