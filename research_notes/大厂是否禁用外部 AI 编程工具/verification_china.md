# Verification of the China findings against full text, and gap questions (as of 28 November 2026)

**Read this first: Part 1 was stopped, because the news sites did not open.** Six cited articles on six different listed sites were tried with WebFetch (www.yicai.com, technode.com, www.tmtpost.com, www.eet-china.com, www.stdaily.com, techcrunch.com). All six failed with `getaddrinfo ENOTFOUND`: the host name did not resolve. Under the stop rule I did not try the other eight cited articles (tech.ifeng.com three times, finance.sina.com.cn twice, www.cnbc.com, www.ithome.com, www.163.com). **No news article was read in full. Every claim that rests on a news article is therefore NOT CHECKABLE in this round.**

What the failure looks like, so that it can be fixed:

- The environment settings (environment "agent-research") do list all 20 news hosts as allowed.
- A name-resolution check on the machine shows that none of those 20 hosts resolves. www.anthropic.com, code.claude.com and platform.claude.com do resolve, and WebFetch opened them without trouble.
- Likely cause, not confirmed: the allow-list is applied at the session's HTTPS proxy, and WebFetch looks host names up directly instead of going through that proxy. I did not use curl or wget, as instructed, so I did not test the proxy path.

What this file contains instead:

1. Full-text checks against five pages on Anthropic's own sites. They cover three items in the report (two confirmed, one partly confirmed) and add primary-source dates for the Claude Code versions named in the Chinese ministry's warning.
2. For every claim whose article did not open: the verdict NOT CHECKABLE, plus a cross-check against search results. A cross-check is the same kind of evidence the report already rests on. It is not a full-text verification.
3. New findings on the gap questions (Part 2), all from search results.
4. Recommended edits, each marked with the evidence it rests on.

Work done on 28 November 2026: 29 web searches (28 extended, 1 standard), no search refused for budget reasons; 6 failed page attempts; 5 pages opened (all Anthropic). Every gap question (a) to (f) had at least two searches. Four of the 29 searches (the ministry notice, Alibaba and Codex, Tencent's 90% figure, Alibaba's response to Anthropic) were cross-checks on Part 1 points, run because the articles would not open.

**Evidence labels used below**

- **FT** = full text read in this session through WebFetch. Quotes are the excerpts the fetch tool returned as verbatim (on some pages it limits each excerpt to about 125 characters). Only Anthropic pages have this label.
- **TITLE** = the page title as returned by the search engine. Reliable for the headline and often the date in the URL, not for the body.
- **SS** = search summary only: what the search tool reported about a result page. Chinese fragments in quotation marks are fragments the tool presented as quotes from the page. They were not checked against the page. Where the tool only paraphrased, I give the paraphrase in English.
- Grades for new findings: **A** = on-record company or government statement or published notice text; **B** = reputable outlet citing internal documents or several employees; **C** = anecdote. **B-** is the report's own category for a report repeated by several outlets whose original source was not seen.

---

## Part 1. Do the report's claims on Chinese companies and on the regulator's warning hold up against the full text of the cited articles?

### Takeaway

None of the 14 cited news articles could be opened, so none of the news-based claims was verified in full text. Three items were checked against Anthropic's own pages: two are CONFIRMED, one is PARTLY CONFIRMED, and Anthropic's release notes give exact dates for the Claude Code versions in the ministry's warning. Search results contradict none of the report's China claims outright, but they show several places where the wording is looser than the sources (marked "Correction" below and listed in the recommended edits).

### Cited Findings

#### 1A. Checked against full text (Anthropic's own pages)

**A1. Timeline row "Sep 2025 | Anthropic stops serving companies that are majority Chinese-owned, worldwide."**
- URL: https://www.anthropic.com/news/updating-restrictions-of-sales-to-unsupported-regions
- Full text: full.
- Verdict: **CONFIRMED.**
- Quotes (FT): published "Sep 4, 2025"; the rule covers "entities that are more than 50% owned, directly or indirectly, by companies headquartered in unsupported regions", "regardless of where they operate"; China is named twice ("including adversarial nations like China"; "jurisdictions where our products are not permitted, like China"). — [Anthropic, 4 September 2025](https://www.anthropic.com/news/updating-restrictions-of-sales-to-unsupported-regions)
- Correction: none needed. Precision only: the rule is about all unsupported regions, with China as the named example.
- Additional fact: this post does not name Claude Code or the API. It speaks of "our services", "our products" and "our models". Reasons given include "legal requirements that can compel them to share data" and "techniques like distillation". — [same page](https://www.anthropic.com/news/updating-restrictions-of-sales-to-unsupported-regions)

**A2. Sentence under the company table: "Anthropic's own policy excludes majority Chinese-owned companies from Claude and Claude Code, wherever they operate."**
- URL: https://www.anthropic.com/supported-countries
- Full text: full.
- Verdict: **CONFIRMED** for the policy now in force.
- Quotes (FT): title "Supported Regions Policy", effective "Nov 12, 2026". Not allowed: "Use by persons while physically located in an unsupported region"; "Use by entities incorporated or headquartered in an unsupported region"; entities "majority-owned or controlled, directly or indirectly, by persons or entities in unsupported regions." Covered: "our apps (such as Claude.ai, Claude Code, and Cowork)", "our API and developer platform", "Claude models offered through cloud platforms and authorized resellers". Mainland China, Hong Kong and Macau are not on the supported list; Taiwan and Singapore are. — [Anthropic Supported Regions Policy, effective 12 November 2026](https://www.anthropic.com/supported-countries)
- Correction (precision): the policy excludes entities headquartered or incorporated in an unsupported region as well as majority-owned or controlled ones, so "majority Chinese-owned" is narrower than the text. The policy page itself does not name China.
- Additional fact that bears on tool versus model: the policy lists the **Claude Code app itself** among the covered services, next to the models.

**A3. "In June, Anthropic accused operators tied to Alibaba's Qwen lab of copying Claude's abilities ("distillation") through nearly 25,000 fake accounts [B] (CNBC)."**
- Report's URL: https://www.cnbc.com/2026/06/24/anthropic-alibaba-distillation-campaign.html — not tried (stop rule; the host does not resolve).
- Checked instead against: https://www.anthropic.com/threat-intelligence-report-september-2026 — full (all three parts of the page read).
- Verdict: **PARTLY CONFIRMED.** The accusation and its wording are confirmed by Anthropic itself. The June date and the figure "nearly 25,000" are not in Anthropic's report and still depend on CNBC, which was not read.
- Quotes (FT): section heading "GTG 16005: Chain-of-thought distillation and AI R&D campaign by Alibaba (Qwen / Tongyi Lab)"; "Operators affiliated with Alibaba ran the largest distillation attack we have ever measured."; "Alibaba accessed Claude through two main pools of fraudulent accounts."; "The first consisted of nearly 5,000 fraudulent accounts leveraging residential proxies…"; "Scale of distillation attacks attributable to Alibaba between May and July 2026: over 151 million exchanges observed." — [Anthropic, "Detecting and countering misuse of AI: September 2026"](https://www.anthropic.com/threat-intelligence-report-september-2026)
- Not on the page (FT): any June letter to the US Senate Banking Committee, "25,000" accounts, "28.8 million" exchanges, or any response from Alibaba.
- Cross-check of the June figures (TITLE and SS, low-quality sites): headlines read "Anthropic accuses Alibaba of running massive AI model distillation campaign using 25,000 fake accounts" and "Anthropic alleges Alibaba's Qwen lab ran 28.8 million queries in massive AI distillation heist". The search summary adds a letter to the Senate Banking Committee dated 10 June 2026 and the period 22 April to 5 June 2026. — [Crypto Briefing, page not opened](https://cryptobriefing.com/anthropic-accuses-alibaba-copying-ai-model/); [Crypto Briefing, second item, page not opened](https://cryptobriefing.com/anthropic-alibaba-qwen-ai-distillation-heist/); [Enterprise DNA, page not opened](https://enterprisedna.co/resources/news/anthropic-alibaba-distillation-attack-senate-june-2026/)
- Correction needed on the next sentence, "Alibaba denies it by most accounts, and one report says it had not responded.": search summaries conflict and do not support "most accounts". Coverage of the ban from 4 to 6 July 2026 says "Alibaba has not publicly responded to Anthropic's distillation accusations" (SS; the tool did not say which of the result pages carries the sentence). A note said to be based on Reuters says Alibaba "did not immediately comment". Some small sites say it "firmly denied" the claims. No on-record Alibaba statement was seen. — [TechCrunch, 4 July 2026, page not opened](https://techcrunch.com/2026/07/04/alibaba-reportedly-bans-employees-from-using-claude-code/); [Quartz, 6 July 2026, page not opened](https://qz.com/alibaba-ban-claude-code-anthropic-backdoor-070626); [Tech Insider, denial claim, page not opened](https://tech-insider.org/au/anthropic-alibaba-claude-distillation-2026/)

**A4. Version dates behind the ministry's warning ("Claude Code versions 2.1.91 to 2.1.196"), from Anthropic's release notes**
- URL: https://code.claude.com/docs/en/changelog — partial: four parts of a very long page were read, covering versions 2.1.332 to 2.1.327, 2.1.260 to 2.1.238, 2.1.220 to 2.1.181 and 2.1.133 to 2.1.89.
- This does not verify the ministry's notice. It verifies what Anthropic's own record says about the versions the notice names.
- Facts (FT): 2.1.91 is dated April 2, 2026. 2.1.196 is dated June 29, 2026. 2.1.197 is dated June 30, 2026 and its whole entry is the launch of a new default model ("Introducing Claude Sonnet 5: now the default model in Claude Code…"). 2.1.198 is dated July 1, 2026; 2.1.199 July 2, 2026; 2.1.200 and 2.1.201 July 3, 2026; 2.1.202 July 6, 2026. — [Claude Code changelog](https://code.claude.com/docs/en/changelog)
- Fact (FT): in the parts read, which run from 2.1.181 (June 17, 2026) to 2.1.220 (July 25, 2026) and from 2.1.89 (April 1, 2026) to 2.1.133 (May 7, 2026), the fetch tool found no entry that mentions a time-zone or proxy check, an "experiment" of that kind, resellers, distillation, or the removal of such code. The first and last entry of each part were cut off at the edge. — [Claude Code changelog](https://code.claude.com/docs/en/changelog)
- Fact (FT): Anthropic's two other relevant pages are also silent on it. The 12 October 2026 post on distillation defences does not mention Claude Code, a time-zone or proxy check, or Alibaba. The September 2026 threat report does not mention a Claude Code check. — [Anthropic, 12 October 2026](https://www.anthropic.com/news/distillation-defenses); [Anthropic, September 2026](https://www.anthropic.com/threat-intelligence-report-september-2026)

**A5. A fact for the paragraph "In-house tool does not mean in-house model in China either" (Claude Code run against other models)**
- Fact (FT): the entry for 2.1.196 (June 29, 2026) says "Remote Control is now disabled when `ANTHROPIC_BASE_URL` points at a non-Anthropic host, matching the existing behavior under `CLAUDE_CODE_USE_BEDROCK`/`_VERTEX`/`_FOUNDRY`". Anthropic's own release notes therefore acknowledge that the program can be pointed at a non-Anthropic endpoint. Grade A for that technical point only. It says nothing about what any employer allows. — [Claude Code changelog](https://code.claude.com/docs/en/changelog)

#### 1B. Not checkable: the cited article did not open

For each item: the report's sentence, the URL, the load result, the verdict, and then the search-level cross-check. "Exact quote from the article" is "none" throughout, because no article was read.

**B1. ByteDance, May 2025 notice**
- Report: "In May 2025 its security department emailed staff that third-party AI coding software such as Cursor and Windsurf would be disabled in batches from 30 June 2025, with its own Trae as the replacement [B]"
- URL: https://www.yicai.com/news/102643443.html — **failed** (name not resolved).
- Verdict: **NOT CHECKABLE.**
- Cross-check (SS): consistent. Fragment: "2025 年 5 月，字节跳动安全与风控部门曾发布邮件称，出于防范数据泄露风险的考虑，自 2025 年 6 月 30 日起内部分批次禁用第三方 AI 开发软件，包括 AI 编程工具 Cursor、Windsurf 等，并将旗下编程助手 Trae 作为替代方案。" (In May 2025 ByteDance's Security and Risk Control department sent an email saying that, to guard against data-leak risk, third-party AI development software, including the AI coding tools Cursor and Windsurf, would be disabled internally in batches from 30 June 2025, with its own coding assistant Trae as the replacement.) The fragment comes from November 2026 coverage that recalls the 2025 notice; which result page carries it was not stated. — [ITHome, November 2026, page not opened](https://www.ithome.com/1/021/917.htm); [ifeng Tech, page not opened](https://tech.ifeng.com/c/8xlwwoMswxo)
- Correction: none indicated. Precision available: the sender is described as the "Security and Risk Control" department (安全与风控部门).

**B2. ByteDance, 2025 follow-up notice**
- Report: "A follow-up notice gave two reasons. Employees were buying tools with personal accounts, so company data built up there. And some tools were not formally offered in China, which is a compliance risk. The notice added that compliant tools could still be applied for"
- URL: https://tech.ifeng.com/c/8jkONUSCAFN — **not tried** (stop rule; host does not resolve).
- Verdict: **NOT CHECKABLE.**
- Cross-check (SS): consistent on the two reasons. The tool's English paraphrase: many employees bought these tools through personal accounts, which left data outside company security management, and "some tools and models such as Claude were not yet available in its ZG region". The part about applying for compliant tools did not appear in any summary. — [Tencent Cloud developer community reprint, "禁用Cursor等第三方AI开发工具后，字节内部再发通知", page not opened](https://cloud.tencent.com/developer/news/2618714); [Zhihu copy, page not opened](https://zhuanlan.zhihu.com/p/1911385958479274284)
- Additional fact (SS): the follow-up notice reportedly named Claude as an example of a tool or model not offered in ByteDance's "ZG" (China) region.

**B3. ByteDance, April 2026 letter on 50% reimbursement**
- Report: "In April 2026 an internal letter even said the company would repay 50% of what employees spent trying AI products in their own time [B]"
- URL: http://www.eeo.com.cn/2026/0613/913745.shtml — **not tried**: the host is not on the reachable list.
- Verdict: **NOT CHECKABLE.** No cross-check was run.

**B4. ByteDance, November 2026 rule**
- Report: "Then, on 3 and 4 November 2026, Chinese media reported a new rule. All "L4" code repositories and Feishu (飞书) documents are closed to outside AI, company-wide, including Claude and Codex accounts that employees pay for themselves [B, no official announcement]"
- URL: https://technode.com/2026/11/04/bytedance-reportedly-tightens-access-to-claude-and-codex-even-for-employees-paying-out-of-pocket/ — **failed** (name not resolved).
- Verdict: **NOT CHECKABLE.**
- TITLE: "ByteDance reportedly tightens access to Claude and Codex, even for employees paying out of pocket · TechNode".
- Cross-check (SS): consistent across at least six outlets. Fragment: "全公司L4级别的代码仓库和飞书文档将禁止外部AI访问，Seed部分场景除外。即便是员工个人自费购买的AI账号，也将无法继续访问这些受限内容。" (All L4-level code repositories and Feishu documents across the company will be closed to external AI, except some Seed scenarios. Even AI accounts that employees bought with their own money will no longer be able to access this restricted content.) — [ITHome, page not opened](https://www.ithome.com/1/021/917.htm); [Tencent News, 4 November 2026, page not opened](https://news.qq.com/rain/a/20261104A0EVKF00)
- Cross-check (TITLE, the headline text of a Sohu copy of a Beijing Business Today item): "11月4日，据报道，字节跳动近日出台新的内部AI工具使用管理规定，将大范围限制Claude、Codex等个人账号的内部使用。自本月起，字节跳动将对全公司所有L4级别的代码仓库和飞书文档实施外部AI访问封禁，员工自行购买的AI账号将无法继…" (4 November: ByteDance has reportedly issued a new internal rule on AI tool use that will widely restrict the internal use of personal accounts for Claude, Codex and others. From this month it will block external AI access to all L4-level code repositories and Feishu documents company-wide; AI accounts bought by employees themselves will no longer…) — [Sohu, page not opened](https://www.sohu.com/a/1097230183_130887)
- Date check: every dated item seen is from 4 November 2026, plus one follow-up on 6 November. Nothing seen is dated 3 November. — [Tencent News, 4 November 2026](https://news.qq.com/rain/a/20261104A0EVKZ00); [21st Century Business Herald, 6 November 2026, page not opened](https://www.21jingji.com/article/20261106/herald/ef54b23492f66a809ca77229deb2b39e.html)
- Additional facts (SS): the rule is worded around **personal accounts** of outside AI tools and around **access to internal material**; one outlet stresses that it is not a complete ban on Claude Code and Codex. One summary says the block was "further expanded" to L4 repositories and Feishu documents (进一步扩大至 L4代码库与飞书文档), which implies an earlier, narrower block; the source page of that phrase was not identified. — [Landian News, page not opened](https://www.landian.news/archives/128047.html)

**B5. ByteDance, exemptions**
- Report: "Some work in the Seed model team is exempt. Public work information and personal use are reportedly not restricted"
- URL: https://tech.ifeng.com/c/8xnW4kmNyTg — **not tried** (stop rule).
- Verdict: **NOT CHECKABLE.**
- TITLE: "字节跳动被指限制员工使用外部AI 知情人士回应_凤凰网" (ByteDance said to restrict staff use of external AI; a person familiar responds).
- Cross-check (SS): consistent. The statement comes from a person familiar with the matter (知情人士), carried by Beijing Business Today (北京商报, reporters 魏蔚 and 鲁芹) on 4 November 2026: "员工使用外部AI工具的个人账号，处理市场上公开的工作信息，或者用于非工作场景（个人生活），是不受限制的" (Employees using personal accounts of external AI tools to handle work information that is public on the market, or for non-work scenarios (personal life), are not restricted). — [Sina Finance copy of the Beijing Business Today item, 4 November 2026, page not opened](https://finance.sina.com.cn/chanjing/gsnews/2026-11-04/doc-inixqxhk9235629.shtml)
- Correction (attribution): the report gives no source for this statement. It is an unnamed person familiar, quoted by Beijing Business Today, and it is about personal accounts.

**B6. ByteDance, "L4" versus "L3 and above", and the vice president's reply**
- Report: "The sources do not agree on the level. The rule is reported as "L4", while the quoted reply from technology vice president Hong Dingkun speaks of "L3 or above"". Also, in another section, the quote "promoting a product does not need this method of banning other choices and forcing use, because it works badly".
- URL: https://www.tmtpost.com/8191863.html — **failed** (name not resolved).
- Verdict: **NOT CHECKABLE.**
- TITLE: "字节新规：限制使用Claude、Codex等外部AI工具-钛媒体官方网站".
- Cross-check (SS): every summary gives the rule as L4 and gives "L3 and above" as part of the vice president's reason, inside the same reports: "L3 及以上内容一旦有泄露，后果是非常严重的" (once content at L3 or above leaks, the consequences are very serious). CNMO's wording: "此举主要出于数据安全考虑，任何涉及L3及以上级别信息的泄露都可能造成严重后果" (the move is mainly for data security; any leak of information at L3 or above could have serious consequences). No summary says that L3 content is blocked. The second quote also matches in substance (tool's paraphrase: promoting the company's own product should not rely on banning other options to force use, because that works badly). — [21st Century Business Herald, 6 November 2026, page not opened](https://www.21jingji.com/article/20261106/herald/ef54b23492f66a809ca77229deb2b39e.html); [CNMO, page not opened](https://ai.cnmo.com/news/821818.html); [NetEase page carrying the TMTPost headline, page not opened](https://www.163.com/dy/article/LATH36Q805118O92.html)
- Correction: "The sources do not agree" overstates it. The same reports give L4 for the rule and L3 and above for the reason. See recommended edit 9.

**B7. Alibaba before the ban**
- Report: "Through June 2026 it repaid staff for Claude, GPT and Gemini use, and Claude Code, Codex and its own Qoder were all everyday tools [B]"
- URL: https://www.eet-china.com/news/202607033036.html — **failed** (name not resolved).
- Verdict: **NOT CHECKABLE.**
- TITLE: "阿里全面禁用Claude：7月10日全员卸载，导火索是隐蔽的检测机制-电子工程专辑" (Alibaba fully bans Claude: all staff to uninstall on 10 July; the trigger is a hidden detection mechanism).
- Cross-check (SS): partly covered. Tool's paraphrase: Alibaba had encouraged staff to use outside AI models and reimbursed them, with many programmers spending several hundred US dollars a week; staff could freely choose Claude, GPT, Gemini and other outside models. Nothing in any summary says that Codex was an everyday tool at Alibaba. The sentence on free choice of models is from the summary of the Zhidx report; the sentence on reimbursement came from a result set that also held EET-China and Guancha, and the tool did not say which page carries it. — [Zhidx, 3 July 2026, page not opened](https://zhidx.com/p/572489.html); [Guancha, 3 July 2026, page not opened](https://www.guancha.cn/economy/2026_07_03_822495.shtml)

**B8. The Claude Code detection code and the Anthropic employee's quote**
- Report: "On 30 June a developer found that Claude Code checked the user's time zone and proxy settings and put markers into what it sent. An Anthropic employee confirmed this as "an experiment we launched in March" against resellers and distillation"
- URL: https://techcrunch.com/2026/07/04/alibaba-reportedly-bans-employees-from-using-claude-code/ — **failed** (name not resolved).
- Verdict: **NOT CHECKABLE.**
- TITLE: "Alibaba reportedly bans employees from using Claude Code".
- Cross-check (SS): the quote is consistent. The employee is named as Thariq Shihipar of the Claude Code team, posting on X: "an experiment we launched in March that was meant to prevent account abuse from unauthorized resellers and protect against distillation." and "The team has landed stronger mitigations since then and we've actually been meaning to take this down for a while." The finding is described as: "Security researchers on Reddit and GitHub found that Claude Code had been built to examine users' local environments — things like timezone settings and proxy configurations — and quietly embed identifying markers in the data transmitted to Anthropic." The tool did not say which result page carries each sentence. — [TechCrunch, 4 July 2026, page not opened](https://techcrunch.com/2026/07/04/alibaba-reportedly-bans-employees-from-using-claude-code/); [Quartz, 6 July 2026, page not opened](https://qz.com/alibaba-ban-claude-code-anthropic-backdoor-070626); [The AI Insider, 6 July 2026, page not opened](https://theaiinsider.tech/2026/07/06/alibaba-bans-claude-code-over-security-concerns-as-anthropic-admits-hidden-user-detection-experiment/)
- Not corroborated: the date "30 June". Summaries say only that a fix was under way by 1 July and, in Chinese coverage, that the code was removed "in the new version released on 2 July" (已在7月2日的新版本中删除了相关代码). — [Zhihu, page not opened](https://zhuanlan.zhihu.com/p/2056869945246593373)
- Primary-source context (FT): Anthropic's release notes and the two Anthropic posts checked do not mention the experiment or its removal (see A4). The employee's post, as relayed by media, remains the only attribution.

**B9. Alibaba's ban as reported by Yicai**
- Report: "On 3 July, Yicai reported that Alibaba had put Claude Code on its high-risk software list and would ban it in the office environment (办公环境) from 10 July 2026, recommending Qoder [B, no on-record statement seen]"
- URL: https://finance.sina.com.cn/tech/roll/2026-07-03/doc-inifpqky0373510.shtml — **not tried** (stop rule).
- Verdict: **NOT CHECKABLE.**
- Cross-check (SS): consistent. Fragments: "因近期Claude Code被曝存在植入后门的安全风险，阿里经综合评估后已将其列入高风险软件名单。" (Because Claude Code was recently reported to carry the security risk of a planted backdoor, Alibaba, after an overall assessment, has put it on its high-risk software list.) and "自7月10日起，阿里将全面禁止内部员工在办公环境下使用Claude Code，并推荐使用Qoder作为替代方案。" (From 10 July, Alibaba will fully ban internal employees from using Claude Code in the office environment and recommends Qoder as the replacement.) The search tool attributes the second fragment to Yicai (第一财经). — [Yicai's own article, 列入“黑名单”，阿里为什么全面禁用Claude Code ？, page not opened](https://www.yicai.com/news/103259844.html); [NetEase, 阿里内部全面禁用Claude Code 推荐使用Qoder, page not opened](https://www.163.com/dy/article/L0U1V0QP05119MAU.html)
- Additional fact (SS): the news was confirmed to media by Alibaba insiders, not by an official statement. Guancha says it learned from Alibaba insiders that the news is true. — [Guancha, 3 July 2026, page not opened](https://www.guancha.cn/economy/2026_07_03_822495.shtml)

**B10. Alibaba, scope of the ban**
- Report: "Reports disagree on the scope. Yicai's wording covers Claude Code only. Other reports say all Anthropic products and models had to be uninstalled"
- URL: https://www.eet-china.com/news/202607033036.html — **failed**.
- Verdict: **NOT CHECKABLE.**
- Cross-check (TITLE): the wider version is in the headlines of several outlets: "独家：阿里全面禁用Claude" (Zhidx, marked "exclusive"); "消息称阿里巴巴全面禁用 Anthropic 旗下 Claude 产品：7 月 10 日生效，全员卸载" (ITHome); the EET-China headline in B7. — [Zhidx](https://zhidx.com/p/572489.html); [ITHome](https://www.ithome.com/0/972/210.htm)
- Cross-check (SS): the tool's paraphrase of the Zhidx report of 3 July 2026: Alibaba internally announced a "reverse ban on Claude" (反向禁用Claude), effective 10 July; all staff were to uninstall Anthropic products, including the Sonnet, Opus and Fable model series and agent tools including Claude Code. Another summary adds that staff were to uninstall all related clients and stop API calls. — [Zhidx, page not opened](https://zhidx.com/p/572489.html)
- Correction (attribution): the wider version starts with Zhidx's exclusive; EET-China and ITHome repeat it. See recommended edit 11.

**B11. Alibaba, reason for the ban**
- Report: "Reports also disagree on the reason. The stated reason is a security "backdoor" risk. Insiders also pointed to the distillation dispute"
- URL: https://news.qq.com/rain/a/20260703A0ADSW00?adChannelId=tech — **not tried**: the host is not on the reachable list.
- Verdict: **NOT CHECKABLE.**
- TITLE: the Tencent News item carries the same headline as a Yicai article, 列入“黑名单”，阿里为什么全面禁用Claude Code ？ (Put on the "blacklist": why has Alibaba fully banned Claude Code?). The Yicai original is at https://www.yicai.com/news/103259844.html, on an allow-listed host.
- Cross-check (SS): the reason given in the notice, as relayed, is the security risk (see the first fragment in B9). English coverage: "In an internal notice, Alibaba said that Claude Code has now been added to a list of high-risk software with security vulnerabilities." The distillation dispute appears in the summaries as background supplied by media and analysts, not as wording of the notice. — [TechCrunch, page not opened](https://techcrunch.com/2026/07/04/alibaba-reportedly-bans-employees-from-using-claude-code/)

**B12. Ant Group**
- Report: "Ant Group reportedly disabled "native" Claude Code from 20 July 2026, but this rests on one workplace column [C]". Table: "Claude Code: reportedly no from 20 July 2026. Staff were said to be moving to Codex and Qoder."
- URL: https://finance.sina.com.cn/roll/2026-07-15/doc-inihwrra4767554.shtml — **not tried** (stop rule).
- Verdict: **NOT CHECKABLE.**
- TITLE: "今日大厂员工事：蚂蚁全面禁用claude code，京东人均token1亿变10亿_新浪财经_新浪网" (Today's big-company staff news: Ant fully bans Claude Code; JD's per-head tokens go from 100 million to 1 billion). The URL is dated 15 July 2026.
- Cross-check (SS): the tool's paraphrase: Ant notified staff "this week" that native Claude Code (原生claude code) would be fully banned from 20 July; the internal reason is quoted as "存在干扰模型运行和潜在种植后门风险，经安全评估确认，现已列入高风险软件清单" (there is a risk of interference with model operation and of a backdoor being planted; confirmed by a security assessment, it is now on the high-risk software list); some R&D staff had started moving to "codex、zcode、qoder" and other tools. Two searches found no second source that names Ant and 20 July. — [Sina Finance column, 15 July 2026, page not opened](https://finance.sina.com.cn/roll/2026-07-15/doc-inihwrra4767554.shtml)
- Corrections: (1) Tense. The column is dated five days before the date it names. It reports a notice of a coming ban and cannot show that the ban took effect. (2) Tools. The summary lists three tools, "codex, zcode, qoder"; the report names two. "zcode" was not identified.

**B13. JD.com and Meituan, spring 2026**
- Report: "JD.com has intercepted visits to outside AI model sites since late March 2026. Staff see a page with JD's own model and an application form. Meituan stopped recommending Alibaba's Qwen models by default in April 2026 and asks for a manager's approval [B-]"
- URL: https://www.ithome.com/0/936/896.htm — **not tried** (stop rule).
- Verdict: **NOT CHECKABLE.**
- TITLE: "曝京东全面拦截员工使用外部 AI，美团内部开始限制阿里 Qwen 模型 - IT之家".
- Cross-check (SS): consistent, with detail the report lacks. JD.com: interception at network level since late March of Doubao, Tongyi Qianwen, Gemini, DeepSeek, ChatGPT and Grok; the block page offers the in-house model and an application channel; "京东的这一措施已在人力资源和风控等敏感部门进行了灰度测试" (JD's measure has been trialled in sensitive departments such as HR and risk control), with a planned extension to retail and logistics; stated reason: to stop business data leaking. Meituan: no longer recommends Alibaba Cloud's Qwen to business teams; "须详细说明使用理由，并经直属负责人（通常为X3层级管理者）审批同意后方可启用" (the reason must be explained in detail, and use may start only after approval by the direct supervisor, usually a manager at X3 level); Doubao and other outside models were at that time outside mandatory approval. The chain of sources is a tip account, "大厂日爆", relayed by media. — [ITHome, page not opened](https://www.ithome.com/0/936/896.htm); [ZOL, "京东美团加强AI管控：限制外部大模型访问，力推自研JoyAI与LongCat", page not opened](https://ai.zol.com.cn/1161/11613958.html)
- Corrections: (1) JD.com's block is described as a phased trial that started in sensitive departments; company-wide coverage is not established, although the headline says "全面" (across the board). (2) Meituan's April rule concerned Alibaba's Qwen for business teams, not outside models in general.
- Tool or model: both rules concern models and AI websites. Neither names a coding tool.

**B14. Beijing Youth Daily, July 2026; Tencent employee account; "declined to comment"**
- Report: "In July 2026 Beijing Youth Daily passed on reports that both had told programmers to "rein in calls to Claude and other external AI models" [B-]"; "The only evidence on outside tools is one employee's account that Claude Code was taken out of the internal model pool in November 2025 and later put back [B-/C]. Tencent, JD.com and Meituan all declined to comment."
- URL: https://www.163.com/dy/article/L1B066N20514R9KQ.html — **not tried** (stop rule).
- Verdict: **NOT CHECKABLE.**
- TITLE: "半两财经｜工信部要求排查AI编程工具Claude Code，追访：已有公司禁用" (MIIT asks for checks on the AI coding tool Claude Code; follow-up: some companies have already banned it).
- Cross-check (SS): consistent on the quote, with one addition: 「也有消息称美团、京东等公司此前已通知程序员“收敛Claude等外部AI模型调用”，如有需求必须单独申请」 (There are also reports that Meituan, JD.com and other companies had earlier told programmers to "rein in calls to Claude and other external AI models", and that any need must be applied for separately). On Tencent, the tool's paraphrase: Tencent moved Claude Code out of its "large-model selection pool" (大模型选择池) as early as November 2025; it was later reconnected, but most staff gradually moved to the in-house CodeBuddy. "Declined to comment" did not appear in any summary. — [NetEase, page not opened](https://www.163.com/dy/article/L1B066N20514R9KQ.html); [Tencent News, 12 July 2026, page not opened](https://news.qq.com/rain/a/20260712A09VPY00)
- Tool or model: the Tencent phrase is ambiguous. A "model selection pool" holds models, yet the item named is Claude Code, a tool.

**B15. Tencent, CodeBuddy usage figure**
- Report: "It says over 90% of its engineers use its own CodeBuddy [A, self-reported]". Table: "Tencent says over 90% of its engineers use it (October 2025)."
- URL: https://tech.ifeng.com/c/8niMJjREUb3 — **not tried** (stop rule).
- Verdict: **NOT CHECKABLE.**
- Cross-check (SS, standard search): consistent. The tool's paraphrase: Tencent's "2025 R&D big data report" (《2025年研发大数据报告》), released around the 24 October programmers' day, says CodeBuddy is used by more than 90% of engineers across several hundred R&D teams. ITHome: more than 90% of Tencent's engineers use it and more than 50% of code is AI-generated. In May 2025 Yicai gave 85% of programmers. Caution: in other reports "90%" is the share of AI-generated code, not of engineers. The statement about the R&D report comes from a low-quality English site. — [MoneyOval, page not opened](https://moneyoval.com/article/tencent-report-90percent-engineers-use-codebuddy-ai-assistant-bjammnwtdb7eyk3acl8gc814); [ITHome, page not opened](https://www.ithome.com/0/927/230.htm); [Yicai Global, May 2025, page not opened](https://www.yicaiglobal.com/news/chinas-tencent-mulls-workflow-changes-as-85-of-its-programmers-use-codebuddy-ai-coding-assistant)

**B16. Kuaishou; Baidu's Comate; Xiaomi's MiMo Code; Trae, CatPaw and the CSDN tutorial**
- Report: the sentences citing Huxiu, Eastmoney, AIBase and CSDN in the paragraphs "The rest either require approval or have no documented rule" and "In-house tool does not mean in-house model in China either".
- URLs: m.huxiu.com, www.huxiu.com, caifuhao.eastmoney.com, news.aibase.com, blog.csdn.net — **not tried**: none is on the reachable list.
- Verdict: **NOT CHECKABLE.**
- Cross-check for Kuaishou (TITLE and SS): consistent, and more specific. Title of a Juejin post: "全面封禁 Cursor！又一家大厂出手了 最近，有网友爆料称：快手的研发线发布通知，收紧了对第三方编程软件的使用权限。" (Cursor fully blocked! Another big company acts. Recently a netizen's tip said that Kuaishou's R&D line issued a notice tightening permissions for third-party programming software.) Tool's paraphrase: Cursor crashes as soon as it is opened on an office computer. The December 2025 article 大厂禁用Cursor，程序员回归“手搓时代”？ (Big companies ban Cursor: are programmers back to coding by hand?) also exists on two allow-listed hosts. — [Juejin, page not opened](https://juejin.cn/post/7584110439933100078); [Sina Finance, 8 December 2025, page not opened](https://finance.sina.com.cn/stock/t/2025-12-08/doc-infzzyrm4100933.shtml); [NetEase, page not opened](https://www.163.com/dy/article/KG8D1GQU05118O92.html)
- Cross-check for Xiaomi (TITLE): "小米开源MiMo Code：长周期编程任务胜率超Claude Code", dated 12 June 2026 in the URL, consistent with "June 2026". — [Tencent News, page not opened](https://news.qq.com/rain/a/20260612A02E2J00)
- Cross-check for Trae (SS): ByteDance's Singapore-based coding app Trae dropped Anthropic's Claude models after Anthropic restricted service to Chinese-owned entities. CodeBuddy was not cross-checked. — [SCMP via Yahoo, page not opened](https://tech.yahoo.com/ai/claude/articles/tech-war-bytedance-cuts-off-093000669.html)

**B17. The ministry's warning of 8 July 2026**
- Report: "In China, the industry ministry's vulnerability platform warned on 8 July 2026 that Claude Code versions 2.1.91 to 2.1.196 contain a "security backdoor" that sends region and identity data without consent. It told users to uninstall or upgrade [A, a government notice read through media reprints]" and "Sources differ on the exact version numbers. This is a risk warning, not a law, and Alibaba's ban was reported five days before it."
- URL: https://www.stdaily.com/web/gdxw/2026-07/08/content_544161.html — **failed** (name not resolved).
- Verdict: **NOT CHECKABLE.**
- Cross-check (SS): consistent on every element. Issuer: 工业和信息化部网络安全威胁和漏洞信息共享平台 (NVDB), 8 July 2026. Fragments: "受影响的Claude Code为2.1.91至2.1.196版本" (the affected Claude Code versions are 2.1.91 to 2.1.196); "可在未经用户同意的情况下向远程服务器回传用户地域、身份标识等敏感信息" (can send sensitive information such as the user's region and identity identifiers back to a remote server without the user's consent); "立即卸载，或升级到已清除相关代码的最新安全版本" (uninstall immediately, or upgrade to the latest safe version from which the code has been removed). — [Sina Tech, 8 July 2026, page not opened](https://finance.sina.com.cn/tech/digi/2026-07-08/doc-inihapna8018269.shtml); [ITHome, page not opened](https://www.ithome.com/0/974/001.htm); [Tencent News, 8 July 2026, page not opened](https://news.qq.com/rain/a/20260708A06K1E00)
- Additional fact (TITLE): a university IT centre reprints the notice under what looks like its official title, "关于防范AI编程工具Claude Code安全后门隐患的风险提示" (Risk notice on guarding against the security-backdoor hazard in the AI coding tool Claude Code). — [Guangdong University of Foreign Studies, page not opened](https://etr.gdufs.edu.cn/info/1206/7157.htm)
- Additional fact (SS, third-party blog, not the notice): "升级至 2.1.197 及以上" (upgrade to 2.1.197 or later). — [CalmVPS blog, page not opened](https://calmvps.com/zh/blog/2026-0709-claude-code-backdoor-miit-warning.html)
- Primary-source context (FT): the release dates in A4.
- Arithmetic: 3 July to 8 July is five days, as the report says.

**B18. Timeline rows about Chinese companies and the regulator**

| Row in the report | Verdict | Note |
|---|---|---|
| June 2025: "ByteDance starts to disable Cursor and Windsurf (from 30 June)." | NOT CHECKABLE | SS consistent (B1). |
| Sep 2025: "Anthropic stops serving companies that are majority Chinese-owned, worldwide." | CONFIRMED (FT) | See A1. |
| Oct to Nov 2025: "Chinese-owned tools (CodeBuddy, Trae) drop the Claude model." | NOT CHECKABLE | SS consistent for Trae only (B16). |
| Feb to Mar 2026: "JD.com starts to intercept outside AI sites." | NOT CHECKABLE | SS consistent: late March, phased (B13). |
| Apr 2026: "Meituan requires approval for a rival's models." | NOT CHECKABLE | SS consistent: Alibaba's Qwen (B13). |
| July 2026: "China's regulator warns about Claude Code (8 July). Alibaba bans it (10 July). Ant reportedly follows (20 July)." | NOT CHECKABLE | SS consistent. Ant's ban was announced, not confirmed (B12). Missing: Meituan and Doubao, 2 July (Part 2c). |
| Oct to Nov 2026: "ByteDance shuts outside AI out of its most sensitive code and documents." | NOT CHECKABLE | SS consistent: reports of 4 November, "from this month" (B4). |

**B19. Company table rows, ByteDance to "NetEase, PDD, Didi, Bilibili, Xiaohongshu"**

Each row repeats claims checked above: ByteDance (B1, B4), Alibaba (B7, B9, B10), Ant Group (B12), Tencent (B14, B15), JD.com (B13, B14), Meituan (B13, B14), Kuaishou (B16). All are NOT CHECKABLE in full text. The last two rows are covered by Part 2(a) and 2(f).

### Inferences

- The report's China section passes a consistency test, not a verification test: second-round search results agree with it almost everywhere, but they draw on the same pool of articles the first round summarised.
- The removal date of the detection code is not settled. Media put the removal in "the new version released on 2 July". Anthropic's release notes list 2.1.199 on 2 July and 2.1.197 on 30 June and mention the removal in neither. The ministry's range ends at 2.1.196, which fits a removal in 2.1.197. One of these must be imprecise, or the dates are in different time zones.
- The employee's "launched in March" and the first affected version do not line up exactly: Anthropic dates 2.1.91 to 2 April 2026. This may simply mean a staged or server-side start. No source explains it.
- Because Alibaba's ban was confirmed only through insiders and Ant's rests on one column written before its effective date, the two do not have the same strength of evidence. The report already grades them B and C, which fits.

### Gaps

- All 14 cited news articles remain unread. The verdicts above cannot be upgraded until the pages open.
- Not cross-checked at all: the Economic Observer letter (B3); "compliant tools could still be applied for" (B2); "declined to comment" for Tencent, JD.com and Meituan (B14); Codex as an everyday tool at Alibaba before the ban (B7); the date 30 June for the developer's finding (B8); CodeBuddy dropping Claude (B18); the OpenClaw warning of March 2026.
- Pages on allow-listed hosts that would settle most open points once access works: [Yicai on Alibaba](https://www.yicai.com/news/103259844.html); [ITHome on Alibaba](https://www.ithome.com/0/972/210.htm); [ITHome on ByteDance](https://www.ithome.com/1/021/917.htm); [NetEase page carrying the TMTPost headline](https://www.163.com/dy/article/LATH36Q805118O92.html); [Sina Finance copy of Beijing Business Today](https://finance.sina.com.cn/chanjing/gsnews/2026-11-04/doc-inixqxhk9235629.shtml); [Sina Tech on the ministry notice](https://finance.sina.com.cn/tech/digi/2026-07-08/doc-inihapna8018269.shtml); [ifeng on Meituan and Doubao](https://tech.ifeng.com/c/8uQgWPqBijG); [Sina Finance on Kuaishou](https://finance.sina.com.cn/stock/t/2025-12-08/doc-infzzyrm4100933.shtml).

---

## Part 1, points to settle where the report records disagreement

### Takeaway

None of the four points could be settled on full text. At search level all four have a clear and consistent answer: Alibaba's ban circulated in two versions and neither names Codex; the notice's stated reason is a security risk; ByteDance's rule is L4, with "L3 and above" only in the vice president's reasoning; and the ministry's range is 2.1.91 to 2.1.196, with "uninstall or upgrade" as the remedy.

### Cited Findings

- **Alibaba, scope.** Two versions were reported on 3 July 2026. Version one, from Zhidx's exclusive and repeated by EET-China and ITHome: a ban on Claude as a whole, covering the Sonnet, Opus and Fable models and agent tools such as Claude Code, with all staff to uninstall by 10 July. Version two, from Yicai: Claude Code is on the high-risk software list and banned in the office environment from 10 July, with Qoder recommended. SS and TITLE only. — [Zhidx](https://zhidx.com/p/572489.html); [ITHome](https://www.ithome.com/0/972/210.htm); [Yicai](https://www.yicai.com/news/103259844.html)
- **Alibaba, Codex.** Neither version, as summarised, names OpenAI Codex, GPT or Gemini as banned. The search tool's reading of the Zhidx report: it does not say whether GPT, Codex or Gemini remain usable; it says staff could previously choose Claude, GPT and Gemini, and that the ban cut off the Claude channel. SS only. — [Zhidx, page not opened](https://zhidx.com/p/572489.html)
- **Alibaba, stated reason.** The notice, as relayed, cites the security risk of a planted backdoor (植入后门的安全风险) and the high-risk software list. English coverage describes the list as "high-risk software with security vulnerabilities". The distillation dispute is given as background by media and analysts. SS only. — [Yicai](https://www.yicai.com/news/103259844.html); [TechCrunch](https://techcrunch.com/2026/07/04/alibaba-reportedly-bans-employees-from-using-claude-code/)
- **ByteDance, level and object.** The rule applies to L4. What is restricted is access by outside AI to L4 code repositories (代码仓库) and Feishu documents (飞书文档), company-wide (全公司), "from this month" (自本月起), including through AI accounts that staff bought themselves. Exempt: some Seed scenarios. Not restricted, according to a person familiar: personal accounts used for public work information or outside work. "L3 and above" appears only in the vice president's reason. SS only. — [Tencent News, 4 November 2026](https://news.qq.com/rain/a/20261104A0EVKZ00); [Sina Finance copy of Beijing Business Today](https://finance.sina.com.cn/chanjing/gsnews/2026-11-04/doc-inixqxhk9235629.shtml); [21st Century Business Herald, 6 November 2026](https://www.21jingji.com/article/20261106/herald/ef54b23492f66a809ca77229deb2b39e.html)
- **Ministry, version range.** 2.1.91 to 2.1.196 in every mainstream reprint seen in search results. SS only. — [Sina Tech](https://finance.sina.com.cn/tech/digi/2026-07-08/doc-inihapna8018269.shtml); [ITHome](https://www.ithome.com/0/974/001.htm)
- **Ministry, remedy.** Both: "立即卸载，或升级到已清除相关代码的最新安全版本" (uninstall immediately, or upgrade to the latest safe version from which the code has been removed). SS only. — [Sina Tech](https://finance.sina.com.cn/tech/digi/2026-07-08/doc-inihapna8018269.shtml)
- **Ministry, version dates.** Anthropic dates 2.1.91 to April 2, 2026 and 2.1.196 to June 29, 2026; 2.1.197 followed on June 30, 2026. FT. — [Claude Code changelog](https://code.claude.com/docs/en/changelog)

### Inferences

- A ban worded as "Claude Code on the high-risk software list" is a ban on the program. Read literally it would also cover running that program against Qwen or another domestic model. No source says so in terms.
- The earlier note that "sources differ on the exact version numbers" seems to come from third-party explainers. One gave a different upper bound; others gave the fix version, 2.1.197, which is not part of the affected range.

### Gaps

- The actual text of Alibaba's notice has not been seen in either version, so it is still open which version is the notice and which is a paraphrase.
- Whether ByteDance's L3 content is restricted in any way is not stated in anything seen. The NetEase page carrying the TMTPost headline is indexed with the keywords 密钥 (keys), 服务器 (servers), 代码库 (code repositories) and 显式标识 (explicit labelling), which suggests the article defines the levels. It needs to be read.
- The ministry notice itself was not read, neither on Science and Technology Daily nor on an official site.

---

## Part 2(a). Tencent, Baidu, Huawei and Xiaomi: any documented internal rule, 2025 to November 2026?

### Takeaway

No documented rule was found for any of the four after two targeted searches each. This is an empty search result. It is not evidence that there is no ban, and it is not evidence that the tools are allowed.

### Cited Findings

- Tencent: the only item is the one already in the report, that Claude Code was moved out of the "large-model selection pool" in November 2025, later reconnected, and that most staff moved to CodeBuddy. Grade B-/C. SS only. — [Tencent News, 12 July 2026](https://news.qq.com/rain/a/20260712A09VPY00); [Beijing Youth Daily via NetEase](https://www.163.com/dy/article/L1B066N20514R9KQ.html)
- Tencent: a second search on rules for Codex, Cursor and other outside tools returned only product coverage of CodeBuddy and community projects. No rule. SS only. The finding is the absence of results; the closest result was a tool comparison. — [CSDN comparison article, page not opened](https://blog.csdn.net/m0_37988015/article/details/163907326)
- Baidu: two searches returned only coverage of ByteDance and Alibaba. Baidu appears once, as the owner of Comate (文心快码). No rule. SS only. The finding is the absence of results; the one mention of Baidu is in this page. — [NetEase page carrying the TMTPost headline, page not opened](https://www.163.com/dy/article/LATH36Q805118O92.html)
- Huawei: two searches returned nothing about employee use of outside AI tools. The results were about US export controls on Huawei chips and about other companies. No rule. SS only. The finding is the absence of results; the top result was about ByteDance. — [21st Century Business Herald on ByteDance, page not opened](https://www.21jingji.com/article/20261106/herald/ef54b23492f66a809ca77229deb2b39e.html)
- Xiaomi: two searches returned only product coverage of MiMo and MiMo Code and tutorials on connecting MiMo models to Claude Code. No rule. SS only. The finding is the absence of results; the closest result was product news. — [Tencent News, 12 June 2026, page not opened](https://news.qq.com/rain/a/20260612A02E2J00)
- Xiaomi, context from a primary source: Anthropic accuses Xiaomi of a distillation campaign. "We also uncovered an illicit distillation campaign launched by Xiaomi."; "We observed more than 400k requests to Claude routed across more than 1,500 accounts via proxy services."; "Scale of distillation attacks attributable to Xiaomi over 20 days in March and April 2026: over 400,000 exchanges observed." Grade A for the statement only. FT. — [Anthropic, September 2026](https://www.anthropic.com/threat-intelligence-report-september-2026)

### Inferences

- Tencent's November 2025 change coincides with Chinese-owned tools dropping Claude after Anthropic's September 2025 rule. It may be the same event seen from inside, that is, a vendor-driven change and not a Tencent policy. No source says this.
- All four companies are headquartered in mainland China, so Anthropic's policy bars them from Claude and the Claude Code app whatever their own rules say (A2).

### Gaps

- No internal notice, policy text or credible report was found for Baidu, Huawei or Xiaomi. LatePost, Caixin, 36Kr, Jiemian, The Paper and SCMP did not surface in any result for these companies.
- Tencent's position on Codex and Cursor is not documented.

---

## Part 2(b). Were the Alibaba and Ant Group bans still in force from August to November 2026? Any change in scope?

### Takeaway

Nothing dated August to November 2026 was found that confirms, lifts or changes either ban. Five searches returned only July coverage. The question stays open.

### Cited Findings

- Three searches on Alibaba (Chinese with "November", Chinese with "October", and English) returned coverage from early July 2026 only. The search tool itself reported no later updates. SS only. — [TechCrunch, 4 July 2026](https://techcrunch.com/2026/07/04/alibaba-reportedly-bans-employees-from-using-claude-code/); [Yicai, 3 July 2026](https://www.yicai.com/news/103259844.html)
- Two searches on Ant Group found one source, the 15 July column (B12). SS only. — [Sina Finance, 15 July 2026](https://finance.sina.com.cn/roll/2026-07-15/doc-inihwrra4767554.shtml)
- Coverage of ByteDance's rule in early November 2026 recalls Alibaba's ban in the past tense and reports no reversal. Tool's paraphrase: Alibaba then required staff to stop using the affected Anthropic products and recommended Qoder. This is weak evidence that no reversal had been made public. SS only. — [NetEase page carrying the TMTPost headline, early November 2026, page not opened](https://www.163.com/dy/article/LATH36Q805118O92.html)
- Since 12 November 2026 Anthropic's own policy bars "entities incorporated or headquartered in an unsupported region" from "our apps (such as Claude.ai, Claude Code, and Cowork)". Grade A. FT. — [Anthropic Supported Regions Policy](https://www.anthropic.com/supported-countries)

### Inferences

- For Claude Code used with Anthropic's models, the question matters less after 12 November 2026: the vendor's policy excludes Alibaba and Ant even if an internal ban were lifted.
- A lead only: one result page in the search on the distillation dispute is summarised as saying that CNBC reported on 6 July 2026 that Alibaba had told staff to uninstall Claude entirely. If so, CNBC has its own article on the ban, on an allow-listed host. Not verified, and the search tool did not say which page carries the claim.

### Gaps

- No source says whether Alibaba's rule covers Codex, Cursor or GitHub Copilot, and none says what happened at Ant on or after 20 July 2026.

---

## Part 2(c). JD.com and Meituan after July 2026

### Takeaway

Nothing after July 2026 was found for either company. One finding is missing from the report: on 2 July 2026 several outlets relayed a tip that Meituan had restricted ByteDance's Doubao models, after Alibaba's Qwen in April.

### Cited Findings

- **New. Meituan and Doubao, 2 July 2026.** Headlines: "消息称美团内部全面限用豆包大模型 此前曾限用阿里云Qwen" (Report: Meituan fully restricts Doubao models internally; it had earlier restricted Alibaba Cloud's Qwen). Fragments: "通知显示，所有业务团队需完成现有豆包相关业务自查，并规划迁移至LongCat、DeepSeek等模型" (The notice shows that all business teams must complete a self-check of existing Doubao-related business and plan a migration to models such as LongCat and DeepSeek); "无法从火山引擎豆包大模型迁移的，需要提交原因 + 必要性说明，走单独的审批流程" (Where migration away from Volcano Engine's Doubao models is not possible, a reason and a statement of necessity must be submitted, through a separate approval process). Origin: the tip account "大厂日爆". Grade B- (several outlets relaying a tip; the notice itself not seen). SS and TITLE only. — [ifeng Tech, 2 July 2026](https://tech.ifeng.com/c/8uQgWPqBijG); [Tencent News, 2 July 2026](https://news.qq.com/rain/a/20260702A04HQA00); [JRJ, 2 July 2026](https://stock.jrj.com.cn/2026/07/02130957666690.shtml)
- July 2026: the Beijing Youth Daily passage in B14 ("rein in calls to Claude and other external AI models", apply separately if needed). Grade B-. SS only. — [NetEase](https://www.163.com/dy/article/L1B066N20514R9KQ.html)
- JD.com, 15 July 2026, headline only: "京东人均token1亿变10亿" (JD's per-head tokens go from 100 million to 1 billion). It suggests a larger internal AI quota, not a restriction. Meaning not verified. Grade C. TITLE only. — [Sina Finance column, 15 July 2026](https://finance.sina.com.cn/roll/2026-07-15/doc-inihwrra4767554.shtml)

### Inferences

- Meituan's rule is not "in-house only". The named migration targets are LongCat, which is its own, and DeepSeek, which is not. The pattern is a move away from two direct rivals' cloud models, Alibaba's and ByteDance's.
- The Meituan and JD.com rules found are about models and AI websites. No rule naming Claude Code, Codex or Cursor as tools was found for either company.

### Gaps

- No report from August to November 2026 for either company. Whether JD.com's trial was extended company-wide is not documented.

---

## Part 2(d). ByteDance: any official statement on the November 2026 rule? Does it apply to TikTok and overseas staff?

### Takeaway

No on-record ByteDance statement was found. The public record consists of the relayed internal notice, the vice president's intranet reply as relayed by media, and one unnamed "person familiar". No report mentions TikTok or staff outside China; the rule is described only as "company-wide".

### Cited Findings

- The only response is from a person familiar with the matter, carried by Beijing Business Today on 4 November 2026 (quoted in B5). Grade B. SS only. — [Sina Finance copy](https://finance.sina.com.cn/chanjing/gsnews/2026-11-04/doc-inixqxhk9235629.shtml)
- Technology vice president Hong Dingkun (洪定坤) replied on an internal platform, not publicly: the move is mainly for data security, and it is not meant to promote in-house tools by blocking rivals. Grade B. SS only. — [CNMO](https://ai.cnmo.com/news/821818.html); [21st Century Business Herald, 6 November 2026](https://www.21jingji.com/article/20261106/herald/ef54b23492f66a809ca77229deb2b39e.html)
- Scope words in every summary: "全公司" (company-wide) and "自本月起" (from this month). Four searches, one of them in English with "TikTok", returned no mention of TikTok, overseas offices or non-China staff. SS only. — [TechNode, 4 November 2026, page not opened](https://technode.com/2026/11/04/bytedance-reportedly-tightens-access-to-claude-and-codex-even-for-employees-paying-out-of-pocket/); [Tencent News, 4 November 2026](https://news.qq.com/rain/a/20261104A0EVKF00)
- Context in the same coverage: ByteDance is said to forbid distilling other models internally, and founder Zhang Yiming is said to have stated that the company will not rely on distillation. Grade C (a Weibo-hosted page). SS only. — [Sina, page not opened](https://www.sina.cn/weibo/detail/5361811232655797.html)

### Inferences

- "Company-wide" would normally include TikTok engineering, but no source says so. It should not be stated as a fact.
- The rule's wording targets personal accounts. Whether company-provided accounts for outside AI exist, or are treated differently, is not documented.

### Gaps

- No statement from ByteDance's press office. The TechNode article may contain a company comment; it could not be opened.

---

## Part 2(e). Does any large company permit running the Claude Code program against an internal or domestic model?

### Takeaway

No documented company rule was found that permits it, and none that forbids it in those words. What exists is evidence that it is technically supported and widely practised by individual developers.

### Cited Findings

- Anthropic's release notes acknowledge the configuration: version 2.1.196 (June 29, 2026) refers to `ANTHROPIC_BASE_URL` pointing "at a non-Anthropic host". Grade A for the technical point. FT. — [Claude Code changelog](https://code.claude.com/docs/en/changelog)
- Tutorials describe pointing Claude Code at domestic models through that setting; Alibaba Cloud's Bailian platform, DeepSeek and Zhipu are named as offering Anthropic-compatible endpoints. Grade C as evidence of practice. SS only. — [CSDN tutorial](https://blog.csdn.net/m0_63171455/article/details/156042359)
- Xiaomi's MiMo models can be connected to Claude Code, by several tutorials, and Xiaomi's own MiMo Code can import MCP servers, skills and API settings from Claude Code. This is about Xiaomi's customers, not its staff. Grade C. SS only. — [Tencent Cloud developer community](https://cloud.tencent.com/developer/article/2664940); [Tencent News, 12 June 2026](https://news.qq.com/rain/a/20260612A02E2J00)
- Ant's reported ban names "native" Claude Code (原生claude code). Grade C. SS only. — [Sina Finance column, 15 July 2026](https://finance.sina.com.cn/roll/2026-07-15/doc-inihwrra4767554.shtml)
- Unverified leads: one blog says newer Claude Code versions no longer work with domestic models and suggests downgrading to 2.1.153. A headline says Claude Desktop stopped direct integration of third-party models such as DeepSeek V4; its date was not seen, and a targeted search found only tutorials from April and May 2026 that describe an official third-party option in Claude Desktop. Grade C. — [AIBase headline, page not opened](https://news.aibase.com/news/27764); [Sina Finance tutorial, 29 April 2026, page not opened](https://finance.sina.com.cn/wm/2026-04-29/doc-inhwceik8113760.shtml)

### Inferences

- The word "native" in Ant's notice may leave room for a modified or internally wrapped build, or for the program pointed at an internal model. That is a reading of one word in one column, not a documented permission.
- Alibaba's ban, in Yicai's version, is on the program, so it would also cover the program run against Qwen (see the points-to-settle section).
- Since 12 November 2026 Anthropic's policy lists the Claude Code app itself as a covered service for entities headquartered in unsupported regions (A2). The barrier for a Chinese company is therefore on the tool as well as on the model, at least on paper.

### Gaps

- No company statement, internal notice or reputable report addresses this question directly for any large Chinese company.

---

## Part 2(f). Any documented rule at NetEase, PDD, Didi, Bilibili, Xiaohongshu or Kuaishou?

### Takeaway

No documented rule was found for NetEase, PDD, Didi, Bilibili or Xiaohongshu in two searches. This is an empty result, not evidence that there is no ban. For Kuaishou the only item is the December 2025 tip already in the report, which is about Cursor specifically.

### Cited Findings

- NetEase, PDD, Didi, Bilibili, Xiaohongshu: two searches returned nothing on employee use of outside AI coding tools. The only hits for Xiaohongshu, Bilibili and PDD concern a Shanghai regulator's clean-up of AI product marketing, which is unrelated. SS only. — [The Paper, page not opened](https://www.thepaper.cn/newsDetail_forward_30970778)
- Kuaishou, December 2025: a netizen's tip that the R&D line issued a notice tightening permissions for third-party programming software, with Cursor crashing on launch on office computers. Grade C. SS and TITLE only. — [Juejin](https://juejin.cn/post/7584110439933100078); [Sina Finance, 8 December 2025](https://finance.sina.com.cn/stock/t/2025-12-08/doc-infzzyrm4100933.shtml)
- One media claim touches several of these companies from the vendor side: the Claude Code detection code is said to have targeted 147 Chinese domains, including Meituan, NetEase, Baidu, Alibaba and ByteDance. The technical description is unverified. Grade C. SS only. — [Tencent News, 8 July 2026](https://news.qq.com/rain/a/20260708A06K1E00)

### Inferences

- None. No policy should be inferred for the five companies.

### Gaps

- The first-round notes record a doubt that one blog attributed ByteDance's 2025 reasons to Kuaishou. This round did not resolve it.

---

## Additional finding outside the listed gap questions: a second Chinese government document

### Takeaway

Coverage of ByteDance's rule pointed to a city-level guideline that the report does not mention. It is guidance on trade secrets when using AI tools, not a rule about any one tool.

### Cited Findings

- At the end of October 2026 the Shenzhen market regulator (深圳市市场监督管理局, also the city's intellectual property office) published 《人工智能工具应用中的商业秘密保护指引》 (Guideline on protecting trade secrets in the use of AI tools), described as the first of its kind in China. By the summaries it covers the life cycle of an AI tool (purchase and admission, deployment, usage permissions, exit), staff conduct and whitelists of tools, and it contains 12 leak cases; one case is a company that let staff in a semiconductor unit use an outside general-purpose AI assistant. Media describe it as guidance without binding force. Grade A for the existence of the document; its text was not read. SS and TITLE only. — [Sina Finance, 31 October 2026, page not opened](https://finance.sina.cn/2026-10-31/detail-inixakva1801733.d.html); [21st Century Business Herald, 4 November 2026, page not opened](https://m.21jingji.com/article/20261104/herald/548cebb988d200b2ab0a3a88dfadbada.html); [Wen Wei Po, page not opened](https://www.tkww.hk/epaper/view/newsDetail/2118759228494712832.html)

### Inferences

- The guideline and ByteDance's rule appeared within a week of each other, and at least one article links them. No source says the guideline caused the rule.

### Gaps

- The guideline's text and exact publication date were not seen.

---

## Recommended edits to the report

Each item gives the exact text to change, the replacement, and the source. Items 1 to 5 rest on full text (Anthropic's pages). Items 6 to 24 rest on search summaries and titles, the same level of evidence as the surrounding report text; apply them as refinements of wording, not as verified facts. No edit rests on background knowledge.

**Resting on full text**

1. Section "Vendors and governments also restrict access from outside", ministry paragraph. Change: "Sources differ on the exact version numbers." To: "The reprints seen give the range as 2.1.91 to 2.1.196; the other numbers in circulation come from third-party explainers. Anthropic's own release notes date 2.1.91 to 2 April 2026 and 2.1.196 to 29 June 2026, with 2.1.197 following on 30 June. The release notes do not mention the detection code or its removal, so the fix version "2.1.197 or later" given by explainers is not confirmed by Anthropic." Source: [Claude Code changelog](https://code.claude.com/docs/en/changelog) (FT); [Sina Tech, 8 July 2026](https://finance.sina.com.cn/tech/digi/2026-07-08/doc-inihapna8018269.shtml) (SS).

2. Section "In China, subsidies turned into bans within months", Alibaba paragraph. Change: "In June, Anthropic accused operators tied to Alibaba's Qwen lab of copying Claude's abilities ("distillation") through nearly 25,000 fake accounts [B] ([CNBC](https://www.cnbc.com/2026/06/24/anthropic-alibaba-distillation-campaign.html)). Alibaba denies it by most accounts, and one report says it had not responded." To: "In June, Anthropic accused operators tied to Alibaba's Qwen lab of copying Claude's abilities ("distillation") through nearly 25,000 fake accounts [B] ([CNBC](https://www.cnbc.com/2026/06/24/anthropic-alibaba-distillation-campaign.html)). Anthropic's own September 2026 report puts the accusation on the record, with different figures: "Operators affiliated with Alibaba ran the largest distillation attack we have ever measured" [A] ([Anthropic](https://www.anthropic.com/threat-intelligence-report-september-2026)). Reports conflict on Alibaba's response: some say it denied the allegations, others that it had not publicly responded or did not immediately comment. No on-record Alibaba statement was seen." Source: [Anthropic, September 2026](https://www.anthropic.com/threat-intelligence-report-september-2026) (FT); for the response, search summaries of July 2026 coverage, for example [TechCrunch](https://techcrunch.com/2026/07/04/alibaba-reportedly-bans-employees-from-using-claude-code/) (SS).

3. Same section, last paragraph. Change: "No big company has said whether it allows this, and the July 2026 actions target the Claude Code program itself." To: "Anthropic's own release notes acknowledge the setup: version 2.1.196 (29 June 2026) refers to the program being pointed "at a non-Anthropic host". No big company has said whether it allows this. The July 2026 actions target the Claude Code program itself, and so does Anthropic's regional policy, which since 12 November 2026 lists the Claude Code app among the services closed to companies headquartered in unsupported regions." Source: [Claude Code changelog](https://code.claude.com/docs/en/changelog) (FT); [Anthropic Supported Regions Policy](https://www.anthropic.com/supported-countries) (FT).

4. Sentence under the company table. Change: "Anthropic's own policy excludes majority Chinese-owned companies from Claude and Claude Code, wherever they operate." To: "Anthropic's own policy excludes companies that are headquartered in China, or majority-owned or controlled from there, from Claude and Claude Code, wherever they operate." Source: [Anthropic Supported Regions Policy, effective 12 November 2026](https://www.anthropic.com/supported-countries) (FT).

5. Section "In China, subsidies turned into bans within months", Alibaba paragraph. Change: "No independent technical check of that code was found." To: "No independent technical check of that code was found, and Anthropic's release notes from mid-June to late July 2026 do not mention it or its removal." Source: [Claude Code changelog](https://code.claude.com/docs/en/changelog) (FT, entries 2.1.181 to 2.1.220).

**Resting on search summaries and titles**

6. Section "What this research could not check", first paragraph. Add after "Only Anthropic's own pages (policies, announcements, documentation) were opened and read.": "A second attempt on 28 November 2026 to open the cited Chinese and English news articles failed for technical reasons. The China findings still rest on search summaries, except where Anthropic's own pages are cited." Source: this file, status note.

7. Same section, third paragraph. Change: "NetEase, PDD, Didi, Bilibili and Xiaohongshu (not searched)" To: "NetEase, PDD, Didi, Bilibili and Xiaohongshu (two searches on 28 November 2026, nothing found)". Also change "Huawei (two searches found nothing), Baidu and Xiaomi (no rule found)" To: "Huawei, Baidu and Xiaomi (two further searches each on 28 November 2026, no rule found)". Also change "and any confirmation after July 2026 for Alibaba, Ant, JD.com, Meituan and Tencent" To: "and any confirmation after July 2026 for Alibaba, Ant, JD.com, Meituan and Tencent (searched again on 28 November 2026, nothing found)". Source: Part 2(a), 2(b), 2(c) and 2(f) of this file.

8. Company table, last China row. Change the cells "**Not researched.** | Not researched | None | None" To: "**No written rule found.** This is an empty search result, not a permission. | Not verified | None | No evidence either way". In the China section, change: "NetEase, PDD, Didi, Bilibili and Xiaohongshu were not searched." To: "For NetEase, PDD, Didi, Bilibili and Xiaohongshu two searches found no rule either, which again is an empty result." Source: Part 2(f).

9. China section, ByteDance paragraph. Change: "The sources do not agree on the level. The rule is reported as "L4", while the quoted reply from technology vice president Hong Dingkun speaks of "L3 or above" ([TMTPost](https://www.tmtpost.com/8191863.html))." To: "The rule is reported as covering L4 content. In the same reports, technology vice president Hong Dingkun explains it more broadly: a leak of content at "L3 or above" would be very serious ([TMTPost](https://www.tmtpost.com/8191863.html)). No report seen says that L3 content is blocked. No report seen mentions TikTok or staff outside China, and no public statement by ByteDance was found." Source: [21st Century Business Herald, 6 November 2026](https://www.21jingji.com/article/20261106/herald/ef54b23492f66a809ca77229deb2b39e.html); [CNMO](https://ai.cnmo.com/news/821818.html); Part 2(d).

10. Same paragraph. Change: "Public work information and personal use are reportedly not restricted ([ifeng](https://tech.ifeng.com/c/8xnW4kmNyTg))." To: "A person familiar with the matter told Beijing Business Today on 4 November that personal accounts used for publicly available work information, or outside work, are not restricted ([ifeng](https://tech.ifeng.com/c/8xnW4kmNyTg))." Source: [Sina Finance copy of the Beijing Business Today item, 4 November 2026](https://finance.sina.com.cn/chanjing/gsnews/2026-11-04/doc-inixqxhk9235629.shtml).

11. China section, Alibaba paragraph. Change: "Reports disagree on the scope. Yicai's wording covers Claude Code only. Other reports say all Anthropic products and models had to be uninstalled ([EET-China](https://www.eet-china.com/news/202607033036.html))." To: "Two versions were reported on 3 July. Yicai's wording covers Claude Code only. Zhidx, in a report marked exclusive that EET-China and ITHome repeated, described a ban on Claude as a whole: all staff were to uninstall Anthropic's products, the Sonnet, Opus and Fable models as well as agent tools such as Claude Code ([EET-China](https://www.eet-china.com/news/202607033036.html)). Neither version, as summarised, names Codex, GPT or Gemini as banned." Source: [Zhidx, 3 July 2026](https://zhidx.com/p/572489.html); [ITHome](https://www.ithome.com/0/972/210.htm).

12. Same paragraph. Change the link "([Tencent News](https://news.qq.com/rain/a/20260703A0ADSW00?adChannelId=tech))" To: "([Yicai](https://www.yicai.com/news/103259844.html), also carried by Tencent News)". The two pages carry the same headline, and the Yicai page is on a host that the environment allows. Source: search result titles.

13. Same paragraph. Change: "An Anthropic employee confirmed this as "an experiment we launched in March" against resellers and distillation" To: "Thariq Shihipar, an engineer on Anthropic's Claude Code team, confirmed this on X as "an experiment we launched in March" against resellers and distillation". Source: search summaries of [TechCrunch, 4 July 2026](https://techcrunch.com/2026/07/04/alibaba-reportedly-bans-employees-from-using-claude-code/) and [Quartz, 6 July 2026](https://qz.com/alibaba-ban-claude-code-anthropic-backdoor-070626). Check the name against the article before publishing.

14. Same paragraph. Change: "Ant Group reportedly disabled "native" Claude Code from 20 July 2026, but this rests on one workplace column [C]" To: "Ant Group reportedly told staff in mid-July that "native" Claude Code would be banned from 20 July 2026 and had been put on a high-risk software list. This rests on one workplace column dated 15 July, before the date it names, so nothing seen shows that the ban took effect [C]". Source: [Sina Finance column, 15 July 2026](https://finance.sina.com.cn/roll/2026-07-15/doc-inihwrra4767554.shtml).

15. Company table, Ant Group row. Change: "**Claude Code: reportedly no** from 20 July 2026. Staff were said to be moving to Codex and Qoder." To: "**Claude Code: a ban from 20 July 2026 was reportedly announced** in mid-July. Not confirmed after that date. Some staff were said to be moving to Codex, "zcode" and Qoder." Change the next cell "Qoder, Codex (reported)" To: "Qoder, Codex, "zcode" (reported)". Source: as item 14.

16. China section, paragraph "The rest either require approval or have no documented rule". Change: "JD.com has intercepted visits to outside AI model sites since late March 2026." To: "JD.com has reportedly intercepted visits to outside AI model sites since late March 2026, starting with a trial in sensitive departments such as HR and risk control." Source: search summary of [ITHome](https://www.ithome.com/0/936/896.htm).

17. Same paragraph. Add after "Meituan stopped recommending Alibaba's Qwen models by default in April 2026 and asks for a manager's approval [B-] ([ITHome](https://www.ithome.com/0/936/896.htm)).": "On 2 July 2026 several outlets relayed a tip that Meituan had told all business teams to review their use of ByteDance's Doubao models and to plan a move to its own LongCat or to DeepSeek, with separate approval for exceptions [B-] ([ifeng](https://tech.ifeng.com/c/8uQgWPqBijG))." Source: [ifeng Tech, 2 July 2026](https://tech.ifeng.com/c/8uQgWPqBijG); [Tencent News, 2 July 2026](https://news.qq.com/rain/a/20260702A04HQA00).

18. Company table, Meituan row. Change: "**Only with approval** for outside models." To: "**Only with approval** for the outside models named in reports: Alibaba's Qwen (April 2026), ByteDance's Doubao (2 July 2026) and, by one July account, Claude. No rule naming Claude Code or Codex as tools was found." Source: as item 17, and [Beijing Youth Daily via NetEase](https://www.163.com/dy/article/L1B066N20514R9KQ.html).

19. Timeline, July 2026 row. Change: "China's regulator warns about Claude Code (8 July). Alibaba bans it (10 July). Ant reportedly follows (20 July)." To: "Meituan reportedly restricts ByteDance's Doubao models (2 July). Alibaba's ban on Claude Code is reported (3 July) and takes effect (10 July). China's regulator warns about Claude Code (8 July). Ant reportedly announces a ban from 20 July." Source: items 14 and 17; B9 and B17.

20. Company table, Kuaishou row. Change: "**Reported tightening** on outside coding tools in December 2025. Scope unclear." To: "**Reported tightening** on third-party coding software in its R&D line in December 2025. The tip says Cursor crashes on launch on office computers. Scope otherwise unclear." In the China section, change "Kuaishou reportedly tightened the use of outside coding software in December 2025, based on an anonymous online tip [C]" To: "Kuaishou reportedly tightened the use of third-party coding software in its R&D line in December 2025, with Cursor said to crash on launch on office computers, based on an anonymous online tip [C]". Source: [Juejin](https://juejin.cn/post/7584110439933100078); [Sina Finance, 8 December 2025](https://finance.sina.com.cn/stock/t/2025-12-08/doc-infzzyrm4100933.shtml).

21. Optional addition to the ministry paragraph, after "Chinese authorities, including the same platform, had warned in March 2026 about another agent tool, OpenClaw": "At city level, Shenzhen's market regulator published a guideline at the end of October 2026 on protecting trade secrets when using AI tools. It is guidance without binding force and names no single tool [A for the existence of the document, read through media summaries] ([21st Century Business Herald](https://m.21jingji.com/article/20261104/herald/548cebb988d200b2ab0a3a88dfadbada.html))." Source: [Sina Finance, 31 October 2026](https://finance.sina.cn/2026-10-31/detail-inixakva1801733.d.html); [21st Century Business Herald, 4 November 2026](https://m.21jingji.com/article/20261104/herald/548cebb988d200b2ab0a3a88dfadbada.html).

22. Company table, JD.com row, to match item 16. Change: "Visits to outside AI sites have been intercepted since late March 2026." To: "Visits to outside AI sites have reportedly been intercepted since late March 2026, starting with a trial in sensitive departments." Source: search summary of [ITHome](https://www.ithome.com/0/936/896.htm).

23. Optional. Company table, Tencent row. Change: "One employee said Claude Code access was removed in November 2025 and later restored." To: "One employee said Claude Code was taken out of the internal model pool in November 2025 and later restored, and that most staff moved to CodeBuddy. The wording does not make clear whether the tool or the model is meant." Source: search summaries of [Beijing Youth Daily via NetEase](https://www.163.com/dy/article/L1B066N20514R9KQ.html) and [Tencent News, 12 July 2026](https://news.qq.com/rain/a/20260712A09VPY00).

24. Optional, minor. China section, ByteDance paragraph. Change: "In May 2025 its security department emailed staff" To: "In May 2025 its security and risk control department emailed staff". Source: search summary of November 2026 coverage, for example [ITHome](https://www.ithome.com/1/021/917.htm).

**Not recommended as edits, because the evidence is too thin**

- Changing "3 and 4 November 2026" to "4 November 2026" for the ByteDance reports. No item dated 3 November was seen, but ITHome's item, which may be the earliest, carries no date in the search result.
- Changing the report's date of 30 June for the developer's finding, or adding a removal date for the detection code. The sources conflict (see the first two inferences in Part 1).
- Any statement that the Alibaba or Ant bans were still in force, or had been lifted, in November 2026.
