# Tencent WorkBuddy (腾讯 WorkBuddy) — business-model case profile (state as of 2026-11-03)

Source-type tags used below: **[官方 Official]** = Tencent results releases, official docs and pricing pages, official product announcements. **[高管 Exec]** = executive remarks from earnings-call transcripts or interviews. **[媒体 Media]** = news reports and analysis. **[3P数据]** = third-party traffic or MAU trackers (QuestMobile, 易观 Analysys). **[UGC]** = user posts, blogs, tutorials and reseller sites (low reliability). Tencent's Q3 2026 results had not been published as of 2026-11-03. The latest official financial disclosure is Q2 2026, released 2026-08-12.

## 1. Entity confirmation: which "WorkBuddy"?

### Takeaway
In the AI-agent context, "WorkBuddy" is Tencent's general-purpose desktop office agent. It is built and run by the Tencent Cloud (CSIG) team behind the coding agent CodeBuddy. The domestic edition is workbuddy.cn (public launch 2026-03-09) and the international edition is workbuddy.ai (2026-05-28). My searches found no other notable AI product called WorkBuddy. The only other hits were unofficial look-alike, tutorial and reseller sites.

### Cited Findings
- [Media] TechNode reported on 2026-03-09 that Tencent had launched an "OpenClaw-like desktop AI agent for workplace tasks." The article linked to codebuddy.cn/work/, so the product first sat under the CodeBuddy web property. — [TechNode, 2026-03-09](https://technode.com/2026/03/09/tencent-launches-openclaw-like-workplace-ai-agent-workbuddy/)
- [Media] 刘毅 is titled "腾讯云副总裁，腾讯CodeBuddy & WorkBuddy负责人": one executive leads both products inside Tencent Cloud. — [财联社/科创板日报, 2026-09-02](https://www.cls.cn/detail/2472437)
- [Official] WorkBuddy and CodeBuddy share one credit (积分) balance on the same account. — [WorkBuddy 定价详情 (official)](https://www.workbuddy.cn/docs/workbuddy/Pricing); enterprise doc: [WorkBuddy Enterprise 计费概述 (Tencent Cloud docs)](https://cloud.tencent.com/document/product/1831/134333)
- [Official] The international site is workbuddy.ai and describes WorkBuddy as an "AI-native agent designed for everyday office professionals." — [WorkBuddy.ai](https://www.workbuddy.ai/); [Tencent Cloud on X](https://x.com/tencentcloud/status/2059870178264109429?lang=en)
- [Media] Domestic and international accounts are separate and their credits do not convert between editions. — [今日头条 comparison of the two sites](https://www.toutiao.com/article/7684478844716040744/)
- [Media] Tencent runs several other agent products that are easy to confuse with WorkBuddy:
  - **QClaw**, an OpenClaw-style assistant from 腾讯电脑管家 (Tencent PC Manager), launched 2026-03-09 and linked to WeChat and QQ.
  - **微信 ClawBot**, WeChat's official plugin for connecting OpenClaw instances (2026-03-22).
  - **小微 (Xiaowei)**, an AI agent inside WeChat, in small-scale gray testing as of Q2 2026.
  - Sources: [IT之家 on ClawBot, 2026-03-22](https://www.ithome.com/0/931/431.htm); [中国基金报 on ClawBot/QClaw](https://www.chnfund.com/article/AR2a4f15aa-3409-21bb-b403-3a2026cd67c7); [Tencent Q2 2026 results (official, Chinese)](https://www.tencent.com/wp-content/uploads/2026/08/%E8%85%BE%E8%AE%AF%E5%85%AC%E5%B8%83%E4%BA%8C%E9%9B%B6%E4%BA%8C%E5%85%AD%E5%B9%B4%E7%AC%AC%E4%BA%8C%E5%AD%A3%E4%B8%9A%E7%BB%A9.pdf)
- [UGC] Unofficial sites use the name, for example net-workbuddy.com.cn (marketed as "WorkBuddy微信遥控电脑"), workbuddy.fenyai.com and reseller pages. They should not be treated as official. — [net-workbuddy.com.cn](https://net-workbuddy.com.cn/); [workbuddy.fenyai.com](https://workbuddy.fenyai.com/pricing)

### Inferences
- WorkBuddy is the most prominent product by this name for three reasons: Tencent's CEO named it in both the Q1 and Q2 2026 results releases, it is a top-ranked office agent in China by MAU, and it has dedicated official domains. No competing product by this name appeared in any search.
- WorkBuddy grew out of the CodeBuddy stack and is now its own platform. The two still share a leader, a credit system and a billing backend. This matters for value capture, because WorkBuddy monetizes through Tencent Cloud rather than through the consumer (PCG/WeChat) side.

### Gaps
- I could not open Baidu Baike's WorkBuddy entry directly (the fetch returned only the site name), so timeline items sourced only from Baike snippets are flagged below.
- No search was run in Japanese or Korean markets for same-named products. The English search with Tencent excluded still returned only Tencent results.

## 2. What it is: timeline, positioning, features, models, execution, IM invocation, relationships

### Takeaway
WorkBuddy is a desktop-first "全场景" office agent, often described as an OpenClaw ("龙虾") that works without setup. It took one natural-language instruction, planned the steps and ran them on local files and apps. It supports Skills, MCP, multiple agents and "experts," and lets users switch between Tencent's Hunyuan/Hy models and third-party Chinese models (DeepSeek, GLM, Kimi, MiniMax).

Users can also send tasks from a phone. The official route is an "助理（远程任务）" assistant channel through WeChat, WeCom or QQ, with DingTalk and Feishu also cited. A full mobile app followed on 2026-07-18 and phone-based remote desktop on 2026-10-30. From October 2026 the product moved toward enterprise and suite use: deep WeCom integration (2026-10-14) and "Buddy One" (2026-10-29).

### Cited Findings

#### Dated timeline (latest state last; superseded items flagged)
- **2026-01-19** [Media]: WorkBuddy first opened to Tencent staff. More than 2,000 employees used it, in HR, administration, secretarial, operations and sales roles. — [iTiger, 2026-02-06](https://www.itiger.com/hant/news/1106600962)
- **2026-02-06** [Media]: External internal test (内测) began. The product was described as a desktop AI agent workstation for professional roles. — [iTiger, 2026-02-06](https://www.itiger.com/hant/news/1106600962). Some secondary pages give 2026-01-19 as the start of the internal test. That date is the staff-only start, so it is not a contradiction.
- **2026-03-09** [Media/Official WeChat post cited by TechNode]: Public launch in China. Local install needs no cloud deployment. It shipped with 20+ skill packages, MCP support, compatibility with OpenClaw skills, and switchable models (Hunyuan, DeepSeek, GLM, Kimi, MiniMax). — [TechNode](https://technode.com/2026/03/09/tencent-launches-openclaw-like-workplace-ai-agent-workbuddy/); [CnTechPost](https://cntechpost.com/2026/03/09/tencent-debuts-workbuddy-easy-to-use-version-openclaw)
  - Unverified: a 5,000-credit trial grant for new users at launch, which appears only in Baidu Baike and ai-indeed snippets. — [Baidu Baike (could not be opened)](https://baike.baidu.com/item/WorkBuddy/67362053)
- **2026-03 (version update)** [UGC on Tencent Cloud dev community]: "微信一键直连" was added, so users could send text from WeChat and have the office PC search, write and process files. — [腾讯云开发者社区](https://developer.cloud.tencent.com/article/2638410)
- **2026-03-22** [Media]: WeChat launched the official "ClawBot" plugin. WorkBuddy connects through *avatar → Claw settings → WeChat channel → scan QR code*. QClaw and Tencent Cloud Lighthouse connect too. It requires iOS WeChat 8.0.70+ and was rolled out gradually. — [IT之家, 2026-03-22](https://www.ithome.com/0/931/431.htm)
- **2026-03** [Exec]: Tang Daosheng says the enterprise edition launched in March 2026. — [新浪科技/快科技, 2026-10-29](https://finance.sina.com.cn/tech/roll/2026-10-29/doc-iniwwpnx8872086.shtml)
  - Conflict: a low-quality explainer site dates "WorkBuddy企业版" to 2026-06-05. — [ai-indeed](https://www.ai-indeed.com/encyclopedia/22710.html). Tang's statement is preferred.
- **2026-05-15** [Official]: New enterprise pricing took effect (see §5). — [Tencent Cloud doc](https://cloud.tencent.com/document/product/1831/134333)
- **2026-05 (approx.)** [UGC]: Custom models with OpenAI-compatible APIs can be connected, and calls to them reportedly consume no WorkBuddy credits. — [腾讯新闻 tutorial, 2026-09-15](https://news.qq.com/rain/a/20260915A03KHY00)
- **2026-05-28** [Media]: The international edition (workbuddy.ai) launched at Tencent Cloud Day in Hong Kong. — [TechNode, 2026-05-29](https://technode.com/2026/05/29/tencent-launches-workbuddy-productivity-ai-agent-for-global-users/); [今日头条](https://www.toutiao.com/article/7684478844716040744/)
- **2026-06-17** [Media]: WeChat Pay launched the "AI专属卡," and WorkBuddy was the first agent to integrate it (details in §4). — [量子位](https://www.qbitai.com/2026/06/436160.html)
- **2026-07-01** [Official]: Domestic consumer plans were restructured into four tiers (details in §5). — [Official pricing doc](https://www.workbuddy.cn/docs/workbuddy/Pricing)
- **2026-07-18** [Media]: The WorkBuddy mobile app launched on Android, iOS and HarmonyOS. Tencent calls it the first general-purpose agent app on HarmonyOS. It has a cloud-execution mode (Skills, experts and expert teams, scheduled automations, artifact download and sharing, and projects; resources come from Tencent Docs and the ima knowledge base) and a "connect to computer" mode that drives the local desktop. — [IT之家, 2026-07-18](https://www.ithome.com/0/978/468.htm)
- **2026-08-07** [Official]: International personal plan update: the Free plan gets 100 credits/month and Pro credits double (see §5). — [workbuddy.ai announcement](https://www.workbuddy.ai/announcement/)
- **2026-09-02** [Media]: First WorkBuddy ecosystem conference, held in Shenzhen. The open platform open.WorkBuddy.cn launched with three layers: hardware, "Buddy应用" (industry apps) and developers (Skill, Expert, and Connector, which supports MCP and CLI). Product count: 50+ versions in under six months. — [财联社, 2026-09-02](https://www.cls.cn/detail/2472437); [Caixin Global, 2026-09-03](https://www.caixinglobal.com/2026-09-03/tencent-opens-workbuddy-platform-to-third-parties-in-enterprise-ai-push-102481248.html)
- **2026-09-23 / 2026-10-08** (low confidence, Baike snippets only): a Leapmotor (零跑汽车) partnership and a standalone file browser. — [Baidu Baike (not opened)](https://baike.baidu.com/item/WorkBuddy/67362053)
- **2026-10-13/14** [Media]: Deep integration with WeCom. Caixin dates the announcement 10-13. Other outlets report a joint version release on 10-14. — [Caixin Global, 2026-10-14](https://www.caixinglobal.com/2026-10-14/tencent-integrates-ai-agent-workbuddy-with-enterprise-app-102493319.html); [21世纪经济报道, 2026-10-14](https://m.21jingji.com/article/20261014/herald/be87f8aee8a8597b889f1f75a957a777.html)
- **2026-10-29** [Exec/Media]: At the 2026 Tencent Global Digital Ecosystem Summit (腾讯全球数字生态大会), Tang Daosheng launched **Buddy One**, a suite with WorkBuddy as its workbench. It links accounts and points/credit pools across Tencent Docs, Tencent Meeting, ima and others. He also announced:
  - an open architecture that supports enterprise-built agents, third-party agents and third-party IM;
  - **WorkBuddy Managed Agent (WMA)**: cloud-hosted agents that run 24/7 and can be embedded via API or connected to collaboration channels;
  - Hy4 and Hy4 flash, "coming soon."
  - Sources: [新浪科技/快科技, 2026-10-29](https://finance.sina.com.cn/tech/roll/2026-10-29/doc-iniwwpnx8872086.shtml); [新浪财经 interview, 2026-10-30](https://finance.sina.com.cn/jjxw/2026-10-30/doc-iniwyfwa2422074.shtml)
- **2026-10-30** [Media]: Remote control from the phone: live view and takeover of the desktop, built with Tencent's EdgeOne team.
  - Requirements: mobile app ≥2.7.0, macOS client ≥5.9.3.
  - Safeguards: one authenticated phone per computer, an encrypted channel, no recording of input or session footage. Remote-control traffic does not count against the token quota.
  - Availability: open trial for all users. Enterprise tiered permissions and audit logs are "planned."
  - [TechNode, 2026-10-30](https://technode.com/2026/10/30/tencents-workbuddy-adds-remote-control-letting-users-manage-ai-tasks-from-their-phones/)
- **2026-10-30** [Official via media]: The 《WorkBuddy企业版安全白皮书》 (enterprise security white paper) was published (see §4). — [腾讯新闻](https://news.qq.com/rain/a/20261030A09K1300)
- [Exec] In seven months the product shipped "100 major versions" and completed "9 billion (90亿) user task requests." — [新浪科技/快科技, 2026-10-29](https://finance.sina.com.cn/tech/roll/2026-10-29/doc-iniwwpnx8872086.shtml)

#### Positioning and features
- [Official] Tencent's Q2 2026 results attribute WorkBuddy's market lead to three things: "强大的 harness（智能体调度）工程能力，丰富的模型选择，和行业领先的技能库." — [Tencent Q2 2026 results (official, Chinese)](https://www.tencent.com/wp-content/uploads/2026/08/%E8%85%BE%E8%AE%AF%E5%85%AC%E5%B8%83%E4%BA%8C%E9%9B%B6%E4%BA%8C%E5%85%AD%E5%B9%B4%E7%AC%AC%E4%BA%8C%E5%AD%A3%E4%B8%9A%E7%BB%A9.pdf)
- [Exec] On the Q2 2026 call (2026-08-12), Martin Lau described WorkBuddy as "a one-stop-shop workspace that orchestrates multiple agents to handle complex work from end to end." He called it "a new platform" and "a very flexible workspace for agentic AI." — [Investing.com Q2 2026 call transcript](https://www.investing.com/news/transcripts/earnings-call-transcript-tencent-posts-solid-q2-2026-growth-as-ai-spending-surges-93CH-4855197)
- [Media] Features at internal test (Feb 2026):
  - natural-language tasks, with multi-step, multimodal planning and execution on the local computer;
  - work restricted to authorized folders, including batch file processing;
  - generation of docs, spreadsheets and PPT, plus data analysis and research;
  - multiple agent tasks run in parallel;
  - MCP servers and Skills;
  - "blocks high-risk commands."
  - [iTiger](https://www.itiger.com/hant/news/1106600962)
- [Exec] 刘毅's ambition: "把WorkBuddy打造成面向Agent时代的操作系统——不是做一个更强的工具，而是做一个能承载所有工具的平台." — [财联社](https://www.cls.cn/detail/2472437)

#### Models
- [Media] At launch users could switch among Hunyuan, DeepSeek, GLM, Kimi and MiniMax. — [TechNode](https://technode.com/2026/03/09/tencent-launches-openclaw-like-workplace-ai-agent-workbuddy/)
- [Exec] Martin Lau said Hunyuan is one of WorkBuddy's models. It could become the core model if it serves users efficiently, but "will not be the only model." — [36Kr EN, 2026-08-13](https://eu.36kr.com/en/p/3937200774167942); [网易/Q2 call summary](https://www.163.com/dy/article/L47B7H0U053144S4.html)
- [Exec] Management ranked capex priorities: (1) training larger Hunyuan models; (2) inference compute for the Hunyuan, DeepSeek and other models behind WorkBuddy. — [36Kr EN](https://eu.36kr.com/en/p/3937200774167942)
- [Exec] Tang Daosheng (Oct 2026) said WorkBuddy supports Hunyuan, Kimi, other mainstream models and enterprise self-hosted models, and quoted "多模型战略是我们会持续坚持的" and "模型的迭代都很快，可能每两个礼拜，又刷新一个排行." — [东方财富/中国经营报, 2026-10-31](https://finance.eastmoney.com/a/202610313917592794.html); [新浪财经, 2026-10-30](https://finance.sina.com.cn/jjxw/2026-10-30/doc-iniwyfwa2422074.shtml)
- [Official] Tencent's own model line was renamed Hy:
  - Hy3 preview: April 2026.
  - Hy3 official release: July 2026, "按 OpenRouter 的 token 消耗量计…持续位列全球前三."
  - Hunyuan 4 / Hy4: expected later in 2026.
  - [Q2 results (official)](https://www.tencent.com/wp-content/uploads/2026/08/%E8%85%BE%E8%AE%AF%E5%85%AC%E5%B8%83%E4%BA%8C%E9%9B%B6%E4%BA%8C%E5%85%AD%E5%B9%B4%E7%AC%AC%E4%BA%8C%E5%AD%A3%E4%B8%9A%E7%BB%A9.pdf); [Q2 transcript](https://www.investing.com/news/transcripts/earnings-call-transcript-tencent-posts-solid-q2-2026-growth-as-ai-spending-surges-93CH-4855197)
- [Official] Free-tier users get "Auto" model scheduling. All models were open to them for a limited time. Paid tiers get every model. — [Official pricing doc](https://www.workbuddy.cn/docs/workbuddy/Pricing)

#### Local vs cloud execution
- [Media] The desktop client installs locally and needs no cloud deployment, which sets it apart from self-hosted OpenClaw. — [TechNode](https://technode.com/2026/03/09/tencent-launches-openclaw-like-workplace-ai-agent-workbuddy/)
- [Media] Cloud execution was added in the mobile app (July 2026). — [IT之家](https://www.ithome.com/0/978/468.htm)
- [Media] WMA cloud-hosted agents run 24/7 for enterprises (Oct 2026). — [新浪科技](https://finance.sina.com.cn/tech/roll/2026-10-29/doc-iniwwpnx8872086.shtml)
- [UGC] Third-party comparisons say "本机模式数据不出本地，云端模式会上传到腾讯云沙箱," and that WorkBuddy can only reach authorized folders while QClaw has wider (full-disk) permissions. — [腾讯云开发者社区: QClaw vs WorkBuddy](https://cloud.tencent.com/developer/article/2664395)

#### IM invocation (sending a task from the phone)
- [Official] The "助理（远程任务）" doc (last updated 2026-09-16) lists four integration guides:
  - **微信助理** (marked recommended)
  - **微信客服号** (QR-scan binding)
  - **企微助理** (WeCom)
  - **QQ 机器人**

  The overview also mentions DingTalk and Feishu but gives no guide for them. A user sends a task from the phone and the desktop WorkBuddy runs it. Results come back in the same IM. Remote tasks always run in a dedicated "助理" folder, and all remote commands go into one session whose history cannot be cleared. The PC must stay on with WorkBuddy running. — [Tencent Cloud doc: 助理（远程任务）](https://cloud.tencent.com/document/product/1831/134392)
- [Official/partner] Feishu publishes its own tutorial for connecting WorkBuddy to Feishu: create an agent app on the Feishu open platform and enter its App ID and Secret. — [飞书官网](https://www.feishu.cn/content/article/7651905073454304222)
- [Exec] Martin Lau said: "Users can remotely control WorkBuddy via Weixin and WeCom, as well as WPZ [sic, a transcription error in the source]." — [Q2 transcript](https://www.investing.com/news/transcripts/earnings-call-transcript-tencent-posts-solid-q2-2026-growth-as-ai-spending-surges-93CH-4855197)
- [UGC] Third-party accounts disagree on the platform count, citing 5 or 9 IM platforms. WeCom is said to be the only one with "可见范围" (visibility scoping by employee, department or tag). — [奇连 AI](https://iqilian.com/learn/workbuddy-yuancheng-vs-cli-yuancheng/)
- [Media] Since 2026-10-14, WorkBuddy appears as a chat-able bot in WeCom contacts, so staff can @-assign it work like a colleague. — [21世纪经济报道](https://m.21jingji.com/article/20261014/herald/be87f8aee8a8597b889f1f75a957a777.html)

#### Relationships
- **CodeBuddy**: same Tencent Cloud team and leader (刘毅), shared credits, shared billing pages. CodeBuddy skills are also used in Tencent Cloud migrations. — [财联社](https://www.cls.cn/detail/2472437); [pricing doc](https://www.workbuddy.cn/docs/workbuddy/Pricing); James Mitchell on the [Q2 call](https://www.investing.com/news/transcripts/earnings-call-transcript-tencent-posts-solid-q2-2026-growth-as-ai-spending-surges-93CH-4855197)
- **元宝 Yuanbao**: management now describes Yuanbao as part of a "model-product co-design flywheel." Features refined in Yuanbao become "atomic capabilities" reused in WorkBuddy, CodeBuddy, WeChat and QQ Browser. — [36Kr EN](https://eu.36kr.com/en/p/3937200774167942); [Q2 transcript](https://www.investing.com/news/transcripts/earnings-call-transcript-tencent-posts-solid-q2-2026-growth-as-ai-spending-surges-93CH-4855197)
- **QClaw / OpenClaw ("龙虾")**:
  - WorkBuddy launched as the turnkey OpenClaw-like option and is compatible with OpenClaw skills. — [TechNode](https://technode.com/2026/03/09/tencent-launches-openclaw-like-workplace-ai-agent-workbuddy/)
  - QClaw (from Tencent PC Manager) was listed among "new AI products" in Q1 2026 but not in Q2 2026, where 小微 took its place in the list. — [Q1 2026 results (PRNewswire)](https://www.prnewswire.com/apac/news-releases/tencent-announces-2026-first-quarter-results-302770779.html); [Q2 2026 results (official)](https://www.tencent.com/wp-content/uploads/2026/08/%E8%85%BE%E8%AE%AF%E5%85%AC%E5%B8%83%E4%BA%8C%E9%9B%B6%E4%BA%8C%E5%85%AD%E5%B9%B4%E7%AC%AC%E4%BA%8C%E5%AD%A3%E4%B8%9A%E7%BB%A9.pdf)
  - In March, Pony Ma's FY2025 release said 元宝, WorkBuddy and QClaw "产生实际效用." — [中国日报网, 2026-03-19](https://cn.chinadaily.com.cn/a/202603/19/WS69bba952a310942cc49a3f9f.html)
- **Tencent Cloud**: WorkBuddy subscription cash is expected to be recognized in Tencent Cloud's reported revenue. — [网易/Q2 call summary](https://www.163.com/dy/article/L47B7H0U053144S4.html); [36Kr EN](https://eu.36kr.com/en/p/3937200774167942)

### Inferences
- The product moved through three positioning stages: (1) "OpenClaw without deployment" for knowledge workers (Mar); (2) a platform and ecosystem (open platform in Sep, "OS for the agent era"); (3) enterprise suite and governance (WeCom integration, Buddy One, WMA and the security white paper in Oct). IM remote tasking was present from the start. It later expanded into a native app and true remote desktop, which suggests IM alone is not enough for agents that need human help with logins and permissions. TechNode makes this point.
- Multi-model support is a deliberate hedge. Hunyuan was not SOTA, so offering DeepSeek, GLM, Kimi and MiniMax kept task quality competitive. The platform layer (harness and skills) is where Tencent claims its edge.

### Gaps
- No official changelog was found that dates when each IM channel launched (WeChat, WeCom, QQ, DingTalk, Feishu). Third-party counts conflict (5 vs 9 platforms).
- The 5,000-credit launch grant, the 零跑 partnership and the file browser are sourced only from Baike snippets I could not open.
- I did not obtain the official WeChat-account launch post from 2026-03-09, only TechNode's summary of it.

## 3. Target users, value proposition, adoption metrics, case studies, user reviews

### Takeaway
WorkBuddy targets white-collar "效率" users: office workers, solo operators and SMB staff. Since mid-2026 it has also targeted large enterprises through WeCom. The value proposition is "one sentence in, finished deliverable out" on your own files, with model choice and Tencent-ecosystem context.

By third-party data it leads China's office-agent category: QuestMobile puts it at 32.35M MAU in September 2026 across all endpoints, and 易观 counted 20.97M PC visits in June 2026. Tencent has never disclosed users, paying users or revenue for WorkBuddy, and it has switched metrics between quarters (DAU in Q1, monthly interactions in Q2). User reviews praise its capability but complain about credit burn.

### Cited Findings

#### Target users and value proposition
- [Media] Management's target market includes office workers, individual business operators and small and micro business staff. — [网易/Q2 call summary](https://www.163.com/dy/article/L47B7H0U053144S4.html)
- [Official] The international site positions it as "an AI-native agent designed for everyday office professionals. From a single natural-language instruction, it plans, breaks down, and executes complex multi-step…" tasks. — [Tencent Cloud on X](https://x.com/tencentcloud/status/2059870178264109429?lang=en)
- [Exec] Martin Lau: "Given Tencent applications such as Weixin, WeCom, and Tencent Meeting are already widely used by enterprises… WorkBuddy provides a new way for us to monetize our enterprise relationships." — [Q2 transcript](https://www.investing.com/news/transcripts/earnings-call-transcript-tencent-posts-solid-q2-2026-growth-as-ai-spending-surges-93CH-4855197)
- [Exec] Tang Daosheng (June 2026): "WorkBuddy在企业场景里有清晰的商业模式." He likened it to Tencent Meeting a few years earlier, a product with both ToC and ToB traits that Tencent would push through its "C2B" capability. — [网易/华尔街见闻, 2026-06-17](https://www.163.com/dy/article/KVKVC3UU05198NMR.html)
- [Exec] Tang (Oct 2026) on what enterprises buy: "企业需要考虑的不只是模型调用价格，还包括任务能否完成、过程是否可控，以及投入的资源能否转化为业务收益." — [东方财富/中国经营报](https://finance.eastmoney.com/a/202610313917592794.html)

#### Adoption metrics (all dated; note metric differences)
- [Official] Q1 2026 (released 2026-05-13): "WorkBuddy was the most popular productivity AI agent service in China, measured by DAU". Pony Ma said "we believe that our WorkBuddy is currently the most widely used productivity AI agent service in China." No absolute numbers were given. — [Tencent Q1 2026 results (PRNewswire)](https://www.prnewswire.com/apac/news-releases/tencent-announces-2026-first-quarter-results-302770779.html)
- [Official] Q2 2026 (released 2026-08-12): WorkBuddy and CodeBuddy "实现了突破性的用户增长，在各自的领域里处于中国领先水平." The footnote source is "易观分析，中国桌面端AI原生办公智能体月访问量，2026年6月." Further: "WorkBuddy 实现了快速的用户增长和健康的用户留存率." — [Q2 results (official)](https://www.tencent.com/wp-content/uploads/2026/08/%E8%85%BE%E8%AE%AF%E5%85%AC%E5%B8%83%E4%BA%8C%E9%9B%B6%E4%BA%8C%E5%85%AD%E5%B9%B4%E7%AC%AC%E4%BA%8C%E5%AD%A3%E4%B8%9A%E7%BB%A9.pdf)
  - On the Q2 call, Pony Ma said WorkBuddy "recently ranked first among productivity AI service in China based on monthly interactions." — [Q2 transcript](https://www.investing.com/news/transcripts/earnings-call-transcript-tencent-posts-solid-q2-2026-growth-as-ai-spending-surges-93CH-4855197)
  - 36Kr noted the switch from DAU in Q1 to interaction volume in Q2, and that Tencent "has not officially disclosed WorkBuddy user numbers." — [36Kr EN](https://eu.36kr.com/en/p/3937200774167942)
- [3P数据] 易观 (Analysys), June 2026: 20.97M PC monthly visits (2097万). That ranks first among 17 desktop AI-native office agents and exceeds #2 and #3 combined. — [OFweek, 2026-08](https://cloud.ofweek.com/news/2026-08/ART-178801-12008-30698282.html)
- [3P数据] QuestMobile, July 2026, **PC client only**:

  | Product | MAU |
  |---|---|
  | WorkBuddy | 6.582M |
  | QClaw PC | 2.259M |
  | ByteDance TRAE Work | 1.904M |
  | Alibaba QoderWork | 0.230M |

  The "AI效率办公" track totaled 102M MAU. — [财联社, 2026-09-02](https://www.cls.cn/detail/2472437); [199IT](http://www.199it.com/archives/1849967.html)
- [3P数据] QuestMobile, **September 2026, app + web + PC combined** (report 2026-10-20):

  | Product | MAU | Quarter-on-quarter |
  |---|---|---|
  | WorkBuddy | 32.353M (#1 office agent) | +647.1% |
  | 百度搭子 | 9.77M | +1567.5% |
  | 千问办公 | 7.905M | — |

  The office category reached 142M MAU, +50.6% YoY. — [199IT](https://www.199it.com/archives/1858394.html); [新浪财经 7x24](https://finance.sina.com.cn/7x24/2026-10-20/doc-inivwiti7708543.shtml); [新浪科技 (endpoint definition)](https://finance.sina.com.cn/tech/discovery/2026-10-20/doc-inivwitm5797052.shtml)
  - **Superseded or not comparable**: the July PC-only figure.
- [Exec] By 2026-10-29: 9 billion task requests in seven months. The enterprise edition covers 50+ industries, "50% of China's Top 500 enterprises and 60% of the Top 100." Tang called it "China's largest AI office-efficiency agent by user scale." — [新浪科技/快科技](https://finance.sina.com.cn/tech/roll/2026-10-29/doc-iniwwpnx8872086.shtml)
  - The same 50%-of-Top-500 claim appears in the security white paper. — [腾讯新闻](https://news.qq.com/rain/a/20261030A09K1300)
- [Media] Per-user token consumption rose more than 10x within three months of launch. — [网易/华尔街见闻, 2026-06-17](https://www.163.com/dy/article/KVKVC3UU05198NMR.html)
- [Media] Q1 2026: token call volume across WorkBuddy, CodeBuddy and QClaw grew more than 16.5x. This is a media figure, not in the official release text I obtained. — [CIW](https://www.ciw.news/p/tencent-q1-2026?action=share)
- [Media] Internal standing: described as a strategic product that Tencent pins hopes on "继QQ、微信之后." — [财联社](https://www.cls.cn/detail/2472437)

#### Case studies and deployments
- [Media] L'Oréal, McDonald's and BYD are "exploring" the WeCom + WorkBuddy solution, per Tencent. — [21世纪经济报道](https://m.21jingji.com/article/20261014/herald/be87f8aee8a8597b889f1f75a957a777.html)
- [Media] WorkBuddy is reportedly deployed in 50+ industries, including government affairs (政务), ports and local services. — [OFweek](https://cloud.ofweek.com/news/2026-08/ART-178801-12008-30698282.html)
- [Media] The Buddy应用 partners from September 2026 cover finance, legal, HR and design (list in §4). Partners include 广发证券, 易方达, 通达信, 北大法宝, 北森, 帆软, 微盟 and 美图设计室. — [财联社](https://www.cls.cn/detail/2472437)
- [Media] Consumer and commerce case: the "美团生活助手" expert inside WorkBuddy uses WeChat Pay's AI专属卡 to buy group-buying deals. — [量子位](https://www.qbitai.com/2026/06/436160.html)

#### User reviews (UGC; not systematic)
- [UGC] Main complaint: credits burn fast and the free quota is too small for heavy use. One user reports 12,000 credits used in a little over a month (~400/day), and a commenter reports spending about ¥1,500 on credits in one month. — [今日头条 review](https://www.toutiao.com/article/7634181339318665774/)
- [UGC] One bookkeeping task reportedly used 2,000 credits. — [知乎](https://zhuanlan.zhihu.com/p/2073017093856018860)
- [UGC] "积分快用光了，干活却太墨迹，WorkBuddy 这性价比有点虚" (Weibo digital blogger). — [新浪/微博](https://www.sina.cn/weibo/detail/5346297918789042.html)
- [UGC] "WorkBuddy确实挺好用，就是消耗有点快." — [新浪/微博](https://www.sina.cn/weibo/detail/5346648018128902.html)
- [UGC] A Tencent Cloud dev-community piece lists the top pain points as "额度消耗太快、莫名其妙欠费、免费额度不够重度使用." — [腾讯云开发者社区](https://developer.cloud.tencent.com/article/2702350)
- [UGC] A gray market exists: Xianyu sellers offer "10,000 credits for ¥9.9," which a guide warns is risky. — [什么值得买](https://post.smzdm.com/p/aom0vxmm/)
- [UGC] Some users claim files were deleted, for example "6.6GB" without confirmation (Xiaohongshu posts compiled by SMZDM). These claims are unverified. — [什么值得买](https://post.smzdm.com/p/awwrqr4m/)

### Inferences
- Adoption is real and category-leading, but numbers depend on the metric (visits, PC MAU, all-endpoint MAU, DAU rank, interactions). The ~5x jump from 6.6M (July, PC only) to 32.4M (September, all endpoints) partly reflects a broader measurement scope after the July 2026 app launch, not only organic growth.
- Tencent avoids absolute user and revenue numbers and rotates its ranking metric. That pattern fits a business still in a subsidy phase, where free-user cost would look bad next to absolute counts.
- In the reviews, the value proposition is clearly felt ("好用"). The friction sits in value capture: credit opacity and burn rate. This is the classic agent-economics tension, because task-level token use is roughly 10x or more that of chat.

### Gaps
- Tencent has disclosed no DAU, MAU, paying-user count, ARPU, retention figure or enterprise-customer count.
- A search snippet attributed to iTiger/Bianews put WorkBuddy's PC visits at 8.85M at some earlier 2026 point, before the June figure of 20.97M. The month and the original page could not be pinned down, so the growth curve from March to May is unverified.
- No independent verification exists for the "50% of China Top 500" claim, which may mean any paid seat or pilot.
- No V2EX threads were found. No systematic Xiaohongshu data was obtained, only posts compiled by others.
- No quantified case studies with ROI were found, only named "exploring" companies.

## 4. Value creation and delivery: ecosystem leverage, skills and partners, security model, incidents and regulators

### Takeaway
WorkBuddy's delivery engine has five parts: (a) a harness that orchestrates multiple models and agents; (b) a skills and connector ecosystem (200,000+ Skills and 50,000+ partners/developers claimed by Oct 2026); (c) distribution and context from Tencent's installed base (WeChat at 1.439B MAU, WeCom, QQ, Tencent Docs, Tencent Meeting, ima, Tencent Drive, WeChat Pay); (d) hardware and industry partners via an open platform; (e) Tencent Cloud compute.

Security is part of the offer. The tools are sandboxing, authorized folders, deletion to the recycle bin with versioning, batch-operation confirmations, enterprise admin and identity controls through WeCom, and a security white paper. This came after China's MIIT and CNCERT repeatedly warned about OpenClaw-style agents in Feb–Mar 2026, and after Zhipu's ZCode silent-upload scandal in Sep 2026 damaged trust in desktop agents.

### Cited Findings

#### Tencent ecosystem leverage
- [Official] Combined MAU of Weixin and WeChat: 1,439M at 2026-06-30 (+2% YoY). QQ mobile MAU: 520M. — [Q2 results (official)](https://www.tencent.com/wp-content/uploads/2026/08/%E8%85%BE%E8%AE%AF%E5%85%AC%E5%B8%83%E4%BA%8C%E9%9B%B6%E4%BA%8C%E5%85%AD%E5%B9%B4%E7%AC%AC%E4%BA%8C%E5%AD%A3%E4%B8%9A%E7%BB%A9.pdf)
- [Media] WeCom integration (WeCom 5.0.12 / WorkBuddy 5.8.0, 2026-10-14):
  - WorkBuddy joins the WeCom address book as a chat-able bot.
  - The two products share one WeCom account system, and accounts are invalidated automatically when staff leave.
  - With enterprise authorization, WorkBuddy can use ten WeCom capabilities, including messages, documents, meetings and 微盘 (cloud drive).
  - Admins configure the bot centrally, manage seat allocation, usage and compute purchases, and can restrict access to highly sensitive information by org identity.
  - The integration is open to verified (认证) WeCom enterprises, with free trial slots.
  - [21世纪经济报道](https://m.21jingji.com/article/20261014/herald/be87f8aee8a8597b889f1f75a957a777.html); [新浪财经](https://finance.sina.com.cn/tech/shenji/2026-10-14/doc-iniveefm7246446.shtml); [Caixin Global](https://www.caixinglobal.com/2026-10-14/tencent-integrates-ai-agent-workbuddy-with-enterprise-app-102493319.html)
  - Caveat: the two remain separate products and separate teams, and the link is at the context and message-flow level. — [钛媒体](https://www.tmtpost.com/8170778.html)
- [Exec] Tang: WeCom agents can exchange information with WorkBuddy agents when users authorize it, and WeCom is an important context source. — [新浪财经, 2026-10-30](https://finance.sina.com.cn/jjxw/2026-10-30/doc-iniwyfwa2422074.shtml)
- [Media] Buddy One (2026-10-29) links accounts and AI credit pools across Tencent Docs, Tencent Meeting and ima, with WorkBuddy as the workbench. — [新浪财经](https://finance.sina.com.cn/jjxw/2026-10-30/doc-iniwyfwa2422074.shtml)
- [Media] Every WorkBuddy user got a 50GB cloud-drive benefit from the newly launched 腾讯网盘 (Jun 2026). — [网易/华尔街见闻](https://www.163.com/dy/article/KVKVC3UU05198NMR.html)
- [Media] WeChat Pay "AI专属卡" (2026-06-17):
  - It is a sub-wallet inside 微信零钱, isolated from the main account, that the user funds separately.
  - Every payment must be confirmed on the phone, and the AI never gets the password.
  - WorkBuddy was the first adopter, on Mac ≥5.1.1, with Meituan group-buying through the "美团生活助手" expert.
  - WeChat Pay plans to open the card to other compliant agent platforms.
  - [量子位](https://www.qbitai.com/2026/06/436160.html); [观察者网](https://www.guancha.cn/economy/2026_06_17_820805.shtml); [搜狐](https://www.sohu.com/a/1037890276_223764)
- [Exec] Martin Lau said Xiaowei (the WeChat agent) is "laying the groundwork for an agent-to-agent transaction loop." It is configured "to require user intervention and multiple step confirmations as safety measures." — [Q2 transcript](https://www.investing.com/news/transcripts/earnings-call-transcript-tencent-posts-solid-q2-2026-growth-as-ai-spending-surges-93CH-4855197)
- [Exec] Tang on WeChat vs WorkBuddy: WeChat "没有办法像WorkBuddy那样用一个T以上参数的模型，这肯定是不可持续的," because of WeChat's privacy needs and scale. — [新浪财经, 2026-10-30](https://finance.sina.com.cn/jjxw/2026-10-30/doc-iniwyfwa2422074.shtml)

#### Skills, open platform, partners
- [Media] Open platform (2026-09-02, open.WorkBuddy.cn):
  - 100+ first-batch ecosystem partners.
  - Hardware: 10+ categories, 30+ brands, 9 co-branded devices. Named makers: Plaud, Rokid, 影石 Insta360, 优篮子, 科大讯飞 iFlytek, 安克 Anker, 猛玛, 京东京造. 启元机器人's Q1 robot carries the WorkBuddy "brain."
  - Buddy应用: 20+ domains built with 30+ partners, including 通达信, 广发证券, 易方达, 腾讯SSV, 腾讯健康, 北大法宝, 微盟, 北森, 帆软 and 美图设计室.
  - Developers can plug in Skill, Expert and Connector (MCP and CLI).
  - Payment capability and distribution channels for partners were "being built."
  - [财联社](https://www.cls.cn/detail/2472437)
- [Exec] Hardware non-exclusive design: devices share one agent base, and accounts, tasks, memory and artifacts flow across devices. 林佐露 (ecosystem lead) said ecosystem is an outcome rather than a preset goal. — [财联社](https://www.cls.cn/detail/2472437)
- [Exec] As of 2026-10-29: 50,000+ partners and developers, 200,000+ Skills listed, and 2,000+ WorkBuddy partners signed this year (60%+ new). — [新浪科技/快科技](https://finance.sina.com.cn/tech/roll/2026-10-29/doc-iniwwpnx8872086.shtml)
- [Exec] A skill payment and revenue-share mechanism lets developers earn when their skills are called (mentioned on the Q2 call). — [36Kr EN](https://eu.36kr.com/en/p/3937200774167942)
- [Exec] Tang on lock-in: "Skills本身是可以带走的，其实就是文本的描述." Users can carry Skills elsewhere. — [新浪财经](https://finance.sina.com.cn/jjxw/2026-10-30/doc-iniwyfwa2422074.shtml)
- [Exec] Open architecture: "选什么模型、接什么Agent、用什么IM，选择权都在企业手里." — [新浪科技/快科技](https://finance.sina.com.cn/tech/roll/2026-10-29/doc-iniwwpnx8872086.shtml)
- [Exec] Partners include FDE (forward-deployed engineering) service providers building industry applications. — [东方财富](https://finance.eastmoney.com/a/202610313917592794.html)

#### Security and permission model
- [Media] From the internal test onward, the agent works only in authorized folders and blocks high-risk commands. — [iTiger](https://www.itiger.com/hant/news/1106600962)
- [Media] "安全中心": sandbox by default, deletions go to the recycle bin, files can be restored to earlier versions, and backups are capped at 1GB by default with FIFO cleanup. — [DoNews](https://www.donews.com/news/detail/1/6653107.html)
- [Media/UGC] Guides warn users not to switch off four default safety settings: sandbox, disk access, the security center and explicit labeling. — [网易](https://www.163.com/dy/article/L3EOG62P05568W0A.html)
- [Official via media] 《WorkBuddy企业版安全白皮书》 (2026-10-30): the agent backs up a file before each round of edits, deletions go to the system recycle bin by default, and batch operations above a threshold pause for confirmation. — [腾讯新闻](https://news.qq.com/rain/a/20261030A09K1300)
- [Official] Remote-task (IM) runs are confined to a dedicated "助理" folder with full execution logs. — [Tencent Cloud doc](https://cloud.tencent.com/document/product/1831/134392)
- [Media] Remote desktop: one phone per computer, encrypted, no recording, with enterprise tiered permissions and audit logs planned. — [TechNode](https://technode.com/2026/10/30/tencents-workbuddy-adds-remote-control-letting-users-manage-ai-tasks-from-their-phones/)
- [Exec] Tang: enterprise customers worry about agents drifting off task and causing security problems, so platforms must offer security, observability and management. Enterprise revenue comes mainly from those needs. — [东方财富](https://finance.eastmoney.com/a/202610313917592794.html); [新浪财经](https://finance.sina.com.cn/jjxw/2026-10-30/doc-iniwyfwa2422074.shtml)

#### Regulator warnings and incidents (China, 2026)
- [Official via media] MIIT's NVDB (工信部网络安全威胁和漏洞信息共享平台) issued an OpenClaw risk alert on **2026-02-05**. It cited blurred trust boundaries and the risk of instruction inducement, misconfiguration or malicious takeover leading to unauthorized actions. It advised checking public exposure, permissions, authentication and auditing. — [新浪财经, 2026-02-05](http://finance.sina.com.cn/stock/t/2026-02-05/doc-inhkuima2059527.shtml)
- [Official via media] NVDB repeated the warning on **2026-03-08**: default or improper deployments carry high risk of being used for attacks or leaking sensitive data. — [科技日报](https://www.stdaily.com/web/gdxw/2026-03/08/content_482508.html); [新浪财经](https://finance.sina.com.cn/jjxw/2026-03-08/doc-inhqhpym0679571.shtml)
- [Official via media] **2026-03-10**: CNCERT (国家互联网应急中心) issued a risk notice. The China vulnerability database had collected 82 OpenClaw vulnerabilities since January. — [观察者网](https://www.guancha.cn/industry-science/2026_03_10_809530.shtml)
- [Official via media] **2026-03-11**: MIIT NVDB, together with agent vendors, vulnerability platforms and security firms, issued the "六要六不要" guidance for OpenClaw ("龙虾") in office and finance scenarios, for example: do not expose instances to the internet, and use SSH with source-IP limits. — [央视网](https://news.cctv.com/2026/03/11/ARTIU9NPnXcPCDiU9cOfqTlD260311.shtml); [新浪财经](https://finance.sina.com.cn/stock/marketresearch/2026-03-11/doc-inhqrmmm9284202.shtml)
  - Risk areas summarized by Qi An Xin experts: runaway permissions, Skill supply chain, public exposure and data-privacy leakage. — [财联社](https://www.cls.cn/detail/2310147)
- [Media] **Zhipu ZCode incident, Sep 2026**:
  - On 2026-09-18 a developer found that ZCode, Zhipu's AI coding tool, made encrypted workspace snapshots after login and uploaded them silently, with no local opt-out.
  - Zhipu apologized and blamed a new feature that was on by default. It then open-sourced the code, commissioned audits by CAICT (信通院) and NSFOCUS (绿盟), and offered compensation on 2026-09-28.
  - An enterprise (太原承明科技) said 6+ of its workspaces were uploaded between 08-28 and 09-14 and asked whether the data crossed borders.
  - Reports say Zhipu's stock fell for six consecutive sessions.
  - [虎嗅](https://www.huxiu.com/article/4892970.html); [虎嗅 (compensation)](https://www.huxiu.com/article/4894883.html); [观察者网](https://www.guancha.cn/CaiJing/2026_09_20_901394.shtml); [新浪财经](https://finance.sina.com.cn/roll/2026-09-19/doc-inisinhf3144974.shtml)
  - The incident prompted pieces asking whether WorkBuddy is safe ("智谱事件在前，WorkBuddy 还安全吗？"). — [知乎](https://zhuanlan.zhihu.com/p/2085296092523116196)
- [UGC] WorkBuddy-specific incidents found so far are anecdotal user reports of file deletions (e.g., "6.6GB"). — [什么值得买](https://post.smzdm.com/p/awwrqr4m/)

### Inferences
- **Value creation** has three layers:
  - The harness: orchestration, model routing and skills, which Tencent's own filing names as the source of advantage.
  - Context access: WeCom messages, docs and meetings, Tencent Docs and ima. This is the hard-to-copy asset, because competitors can match models but not Tencent's enterprise communication graph.
  - Actuation: local file and app control, IM remote tasking, WeChat Pay, and Meituan as a commerce partner.
- **Delivery** runs on two tracks. One is mass-market self-serve download, with ads and the WeChat/QQ entry points. The other is enterprise go-to-market through WeCom admins, Tencent Cloud sales, FDE partners and private deployment. Security features double as enterprise sales collateral.
- The regulatory climate pushed demand toward branded, vendor-managed "safe lobsters" and away from self-hosted OpenClaw. That favors big-tech offerings like WorkBuddy. The Zhipu case shows the downside risk: one silent-upload incident can damage a vendor's trust and stock.
- Openness claims (multi-model, portable Skills, third-party IM and agents) are partly a competitive necessity against bundled rivals. Real lock-in comes from accounts, the shared credit pool across Buddy One apps, WeCom identity and org data, and history and memory.

### Gaps
- I found no official Tencent statement or regulator action naming WorkBuddy specifically, and no evidence of a confirmed WorkBuddy data incident.
- Details on government or state-employee restrictions on OpenClaw use in 2026 were not researched beyond the MIIT and CNCERT notices.
- No information was found on the skill marketplace's revenue-share percentages or payout volume.
- I could not fetch the full text of the 安全白皮书.

## 5. Value capture: pricing, ties to WeCom, Tencent Cloud and token sales, management commentary, capex, ads and cloud

### Takeaway
WorkBuddy uses a freemium, credit-metered model with four revenue streams: (1) consumer subscriptions (Free / ¥99 / ¥199 / ¥999 per month since 2026-07-01); (2) credit top-ups (≈¥0.05 per credit); (3) enterprise seats (¥198 SaaS / ¥316 dedicated per seat per month, plus private deployment) with pooled credits; (4) token and MaaS revenue booked in Tencent Cloud. Skill revenue-share and agent commerce (WeChat Pay AI card) are emerging options.

Management says most user spend is subscriptions. The paid-user gross margin is already comparable to Tencent Cloud's overall gross margin, but free users are subsidized. The investment is large: new AI products (Hy, Yuanbao, CodeBuddy, WorkBuddy, Xiaowei/QClaw) cut non-IFRS operating profit by about ¥8.8B in Q1 and ¥10.5B in Q2 2026, and Q2 capex was ¥52.8B (+176% YoY). Executives say no revenue target has been set for the WorkBuddy team.

### Cited Findings

#### Consumer pricing (domestic, workbuddy.cn) — current from 2026-07-01
- [Official] Current plans:

  | | 体验版 | 标准版 | 高级版 | 旗舰版 |
  |---|---|---|---|---|
  | Monthly price | Free | ¥99 | ¥199 | ¥999 |
  | Continuous monthly (30% off) | — | ¥70 | ¥140 | ¥700 |
  | Continuous annual | — | ¥672 | ¥1,344 | ¥6,720 |
  | Base credits per month | 500 | 2,000 | 4,000 | 20,000 |
  | Total credits per month incl. limited-time bonus | 500 | 4,000 | 9,000 | 50,000 |

  - Tiers also differ in projects, storage, hosted apps, personal assistants (3/5/8/10), automated tasks and CLI access (paid tiers only).
  - Add-on pack: 1,000 credits for ¥50, valid one month, for paid members only.
  - Credits are shared with CodeBuddy.
  - Legacy members who subscribed before 07-01 on auto-renew keep ¥58/month through 2026-12-31, rising to ¥70/month from 2027-01-01.
  - [WorkBuddy 定价详情 (official)](https://www.workbuddy.cn/docs/workbuddy/Pricing)
- [Media] **Superseded (before 2026-07-01)**:
  - Plans were "free trial + Pro."
  - Pro cost ¥58/month for 4,000 credits, or ¥696/year.
  - Add-on credits cost ¥38 per 1,000.
  - The old Pro became the ¥99 标准版, and add-ons rose to ¥50 per 1,000.
  - Tencent tied the change to call volume: per-user token use rose more than 10x in three months.
  - [网易/华尔街见闻, 2026-06-17](https://www.163.com/dy/article/KVKVC3UU05198NMR.html); [新浪财经 mirror](https://finance.sina.com.cn/stock/t/2026-06-17/doc-inictaex2869195.shtml)
- [UGC] Reports say model consumption multipliers were raised in September 2026, and users noticed faster burn. This is not confirmed officially. — [什么值得买, Sept 2026](https://post.smzdm.com/p/a6z08zlz/)

#### International pricing (workbuddy.ai)
- [Official/UGC] Pro costs US$10/month or US$96/year. From 2026-08-07, Free gets 100 credits/month and Pro credits double, which third parties put at 2,000/month (up from 1,000). There is a 7-day Pro trial that needs a credit card. Add-on figures reported by third parties (500 credits / $15) are not verified. — [workbuddy.ai announcement](https://www.workbuddy.ai/announcement/); [知乎](https://zhuanlan.zhihu.com/p/2071126097668879731)

#### Enterprise pricing (effective 2026-05-15; doc updated 2026-09-29)
- [Official] Enterprise editions:

  | Edition | Per seat per month | Per seat per year | Minimum seats |
  |---|---|---|---|
  | 企业旗舰版 (SaaS) | ¥198 | ¥2,376 | 1 |
  | 企业专享版 | ¥316 | ¥3,792 | 100 |
  | 企业私有化版 | contact sales | contact sales | — |

  - Each seat includes 2,000 credits per month, pooled across the team, with unlimited Q&A.
  - Promotion: an extra 2,000 credits per seat in the first month for new Flagship purchases between 2026-05-15 and 2026-08-14.
  - Add-on packs, valid six months: 2,000 credits ¥100; 5,000 ¥245; 10,000 ¥480; 50,000 ¥2,375; 200,000 ¥9,200.
  - [WorkBuddy Enterprise 计费概述 (Tencent Cloud docs)](https://cloud.tencent.com/document/product/1831/134333)
- [Media] With WeCom integration, admins buy compute and allocate usage inside WeCom. — [21世纪经济报道](https://m.21jingji.com/article/20261014/herald/be87f8aee8a8597b889f1f75a957a777.html)

#### Management commentary on monetization (Q2 2026 call, 2026-08-12, unless noted)
- [Official] "用户通过订阅和充值购买 token 的付费意愿强烈." — [Q2 results (official)](https://www.tencent.com/wp-content/uploads/2026/08/%E8%85%BE%E8%AE%AF%E5%85%AC%E5%B8%83%E4%BA%8C%E9%9B%B6%E4%BA%8C%E5%85%AD%E5%B9%B4%E7%AC%AC%E4%BA%8C%E5%AD%A3%E4%B8%9A%E7%BB%A9.pdf)
- [Exec] James Mitchell (CSO), from the [Q2 transcript](https://www.investing.com/news/transcripts/earnings-call-transcript-tencent-posts-solid-q2-2026-growth-as-ai-spending-surges-93CH-4855197):
  - "the majority of the WorkBuddy spending by users is on subscriptions"
  - "there is a proportion of free users whom we are subsidizing to drive market share and market growth"
  - for paid users: "the gross margins today are already comparable to the gross margins for Tencent Cloud overall"
  - "The token business, it can be positive gross margin at these low token prices because the cost is low"
  - "it is token production for WorkBuddy that carries the most enduring economic value to us"
  - "we aggressively prioritized WorkBuddy while deprioritizing some of the other products in that new AI product portfolio"
- [Exec] Martin Lau (President), from the same [Q2 transcript](https://www.investing.com/news/transcripts/earnings-call-transcript-tencent-posts-solid-q2-2026-growth-as-ai-spending-surges-93CH-4855197):
  - "WorkBuddy provides a new way for us to monetize our enterprise relationships"
  - "we can reduce token costs through agent efficiency, inference efficiency, and model optimization"
  - on downside protection: compute can be rented out via Tencent Cloud "at cost recovery or even better prices," and Tencent "can actually sell that at more than 30% profit compared to the price that we paid"; "If we just switch the model to just renting out compute, it would be actually not loss-making"
  - on capex: "one should not assume that it will be sort of new every year"; "We will only keep on investing if it generates a great return"; "you can assume there is an envelope at the back of our mind"
- [Exec/Media] WorkBuddy's cash revenue is "growing at high speed." There is a lag between cash collection and recognized revenue, and the cash will flow into Tencent Cloud's reported revenue within the year. — [网易/Q2 call summary](https://www.163.com/dy/article/L47B7H0U053144S4.html); [36Kr EN](https://eu.36kr.com/en/p/3937200774167942)
- [Exec] Cloud revenue growth accelerated from "high teens percentage year-on-year in the first quarter" (Mitchell). — [Q2 transcript](https://www.investing.com/news/transcripts/earnings-call-transcript-tencent-posts-solid-q2-2026-growth-as-ai-spending-surges-93CH-4855197)
- [Exec] Martin Lau (as reported in June 2026): consumer AI commercialization is relatively hard. AI services cannot scale at near-zero marginal cost because every model call has real cost, so identifying high-value scenarios matters at least as much as user scale. — [网易/华尔街见闻](https://www.163.com/dy/article/KVKVC3UU05198NMR.html)
- [Exec] Tang Daosheng:
  - In June he said no commercialization targets were set for the WorkBuddy team, and that it is in a strategic-investment phase. — [网易/华尔街见闻](https://www.163.com/dy/article/KVKVC3UU05198NMR.html)
  - On 2026-10-31: "今天WorkBuddy的商业化，更多是从支撑这个产品服务发展的角度来考虑，不是说给了某一个数字要逼着团队去完成"; "我们的资本也是有限的…没有什么无上限"; "Token的消耗量虽然不是一个非常严谨的衡量方式，但是也代表了用户愿意为任务花多少钱."
  - He said WorkBuddy consumes far more tokens than Q&A products like Yuanbao.
  - [东方财富/中国经营报](https://finance.eastmoney.com/a/202610313917592794.html); [新浪财经](https://finance.sina.com.cn/stock/t/2026-11-01/doc-inixfvpw3507955.shtml)

#### Tencent financials framing the investment (official unless noted)
- **FY2025** (released 2026-03-18):
  - Revenue ¥751.8B (+14%). — [中国日报网](https://cn.chinadaily.com.cn/a/202603/19/WS69bba952a310942cc49a3f9f.html)
  - [Exec/Media] Martin Lau said Tencent spent ¥18B on new AI products in 2025 and would "at least double" that in 2026. — [新浪财经, 2026-03-19](https://finance.sina.com.cn/roll/2026-03-19/doc-inhrmycy4447419.shtml)
  - [Media] FY2025 capex was ¥79.2B (+3%), as media-reported. — [新浪财经](https://finance.sina.com.cn/roll/2026-03-19/doc-inhrmycy4447419.shtml)
- **Q1 2026** (released 2026-05-13): all figures from [Tencent Q1 2026 results (PRNewswire)](https://www.prnewswire.com/apac/news-releases/tencent-announces-2026-first-quarter-results-302770779.html).
  - Revenue ¥196.5B (+9%).
  - Non-IFRS operating profit ¥75.6B (+9%); excluding new AI products, ¥84.4B (+17%), with margin 43.0% vs 39.9%.
  - The footnote defines new AI products as "Hy, Yuanbao, CodeBuddy, WorkBuddy, and QClaw."
  - Capex ¥31.9B (+16%). Some media reported about ¥37B, which conflicts with the official figure.
  - Marketing Services ¥38.2B (+20%).
  - "Business Services revenues rose by 20% YoY, with growth led by increased cloud services revenues," helped by "a more favorable pricing environment."
  - Weixin/WeChat MAU 1,432M.
- **Q2 2026** (released 2026-08-12): all figures from [Q2 results (official, Chinese)](https://www.tencent.com/wp-content/uploads/2026/08/%E8%85%BE%E8%AE%AF%E5%85%AC%E5%B8%83%E4%BA%8C%E9%9B%B6%E4%BA%8C%E5%85%AD%E5%B9%B4%E7%AC%AC%E4%BA%8C%E5%AD%A3%E4%B8%9A%E7%BB%A9.pdf).
  - Revenue ¥204.8B (+11%).
  - Non-IFRS operating profit ¥75.6B (+9%); excluding new AI products, ¥86.1B (+19%), with margin rising from 39% to 42%.
  - The footnote's new AI products are "Hy、元宝、CodeBuddy、WorkBuddy 及小微," so QClaw dropped out of the list.
  - Capex ¥52.8B (+176% YoY). On the call the CFO cited *operating* capex of ¥51.8B (+190%), a different measure. — [Q2 transcript](https://www.investing.com/news/transcripts/earnings-call-transcript-tencent-posts-solid-q2-2026-growth-as-ai-spending-surges-93CH-4855197)
  - Free cash flow was −¥13.8B, or ¥37.6B excluding compute prepayments. The prepayments support "Hy 模型升级、WorkBuddy 及 CodeBuddy 推理的需求、微信 AI 举措."
  - Marketing Services ¥43.6B (+22%), credited to "AI 驱动的广告推荐模型的优化" and AIM+.
  - FinTech & Business Services ¥60.3B (+9%). Business services growth came "主要来自于云服务，得益于 AI 相关服务需求提升…以及更为有利的定价环境."
  - Pony Ma: "在基础设施层，我们大幅增加了算力采购，将助力我们把应用及模型的使用转化为收入."
- [Media] Q2 sales and marketing expense was ¥11.9B (+26% YoY), attributed to games and AI-native products. A 36Kr author believes WorkBuddy brand ads (Focus Media elevators, CCTV) are a large part of it. That is the author's opinion. — [36Kr EN](https://eu.36kr.com/en/p/3937200774167942)

### Inferences
- **Implied unit prices** (my calculation from official prices):
  - Top-ups and enterprise packs are about ¥0.05 per credit (¥50/1,000 consumer; ¥100/2,000 to ¥9,200/200,000 enterprise, i.e. ¥0.046–0.05).
  - The ¥99 Standard plan with the limited-time bonus is about ¥0.025 per credit (¥99/4,000), or about ¥0.05 per base credit.
  - Enterprise SaaS is about ¥0.099 per included credit (¥198/2,000), so roughly half the enterprise seat price pays for seat features, governance and identity rather than tokens.
  - This fits Tang's remark that enterprise revenue rests on security, observability and management.
- **New-AI-product drag** (derived from official non-IFRS figures): about ¥8.8B in Q1 and about ¥10.5B in Q2 2026. That is a ¥37–40B annual run-rate, consistent with "at least doubling" 2025's ¥18B. WorkBuddy's share is not disclosed.
- **Four layers of value capture**, ordered from near-term to option value:
  1. Subscriptions and top-ups (cash today, booked in Cloud).
  2. Enterprise seats via WeCom/Tencent Cloud (seat-based, stickier, governance premium).
  3. Token/MaaS production. Management calls it the "most enduring" value, and it also raises Hy usage and the data flywheel.
  4. Ecosystem take-rates: skill revenue-share, partner payments, WeChat Pay agent commerce, and agent-to-agent transactions via Xiaowei. These are mostly future options.

  The downside hedge is compute rental through Tencent Cloud.
- There are two plausible readings of the July price increase, which raised the entry tier from ¥58 to ¥99 and add-ons from ¥38 to ¥50 per 1,000. One is that free and low-tier users were unprofitable at agent-scale token use. The other is that Tencent was testing willingness to pay after achieving category leadership. The September multiplier increase, if confirmed, points in the same direction.

### Gaps
- No disclosed WorkBuddy revenue, paying users, ARPU, conversion rate or gross-margin percentage.
- Search snippets from reseller and blog pages claimed the enterprise editions were renamed "SaaS企业版" and "专有云企业版" from 2026-05-15. I could not pin this to an opened source, and the official doc updated 2026-09-29 still uses 企业旗舰版 and 企业专享版, so treat the rename as unverified.
- No official per-model credit multipliers were found. The pricing pages point to in-app tables.
- No official confirmation was found of the September 2026 multiplier change, or of renamed enterprise editions.
- Q3 2026 results and call were not yet available as of 2026-11-03.
- The exact Q2 2026 cloud revenue growth rate is not confirmed. Media summaries say "20% 出头," but the transcript excerpt I obtained only shows acceleration from "high teens."
- No data on whether WeCom integration has its own fee beyond WorkBuddy seats and WeCom's existing paid tiers.

## 6. China-specific dynamics and competition: low willingness to pay, token pricing, super-app bundling, rivals, and how WorkBuddy differs

### Takeaway
China's office-agent market grew fast (142M category MAU by September 2026), and every major platform consolidated its coding agents into "office agents":
- ByteDance: TRAE Work, then 豆包工作.
- Alibaba: QoderWork, 悟空 and MuleRun merged into 千问办公, tied to DingTalk.
- Baidu: 百度搭子.

Consumer willingness to pay is weak and was historically trained by subsidies (e.g., Yuanbao's ¥1B red-envelope campaign). Token pricing flipped in 2026 from the 2024 price war to cost-driven increases, which squeezes agent products whose task-level token use is about 10x that of chat.

WorkBuddy differs through openness: multiple models including rivals', third-party IM and agents, portable Skills, and hardware partners. Rivals mostly bundle their own model and suite. WorkBuddy also benefits from Tencent's WeChat/WeCom context and payment rails.

### Cited Findings

#### Competitors (dated)
- [3P数据] QuestMobile July 2026 PC MAU: WorkBuddy 6.582M, QClaw 2.259M, TRAE Work 1.904M, QoderWork 0.230M. QuestMobile describes the market as "腾讯系领跑、抖音系紧追、阿里系多线布局." — [财联社](https://www.cls.cn/detail/2472437)
- [3P数据] QuestMobile September 2026, all endpoints: WorkBuddy 32.35M, 百度搭子 9.77M, 千问办公 7.905M. Doubao led AI-native apps overall with 419M MAU. — [199IT](https://www.199it.com/archives/1858394.html); [虎嗅](https://www.huxiu.com/article/4899386.html)
- [Media] Alibaba "千问办公": integrates QoderWork, 悟空 and MuleRun (July 2026), with public beta on 2026-08-03. Another source says 07-27. It is tied to DingTalk. — [百度百科: 千问办公](https://baike.baidu.com/item/%E5%8D%83%E9%97%AE%E5%8A%9E%E5%85%AC/68324768)
- [UGC/Media] ByteDance renamed TRAE SOLO to TRAE Work. On 2026-08-24 the TRAE and 扣子 (Coze) teams were moved into the Doubao organization, and the standalone "豆包工作" launched on 08-25, absorbing the TRAE Work desktop client. Some sources say TRAE IDE and CLI continue as separate products. — [什么值得买](https://post.smzdm.com/p/aomvd7l6/); [cniteyes comparison](http://m.cniteyes.com/archives/41235)
- [Media] ByteDance added AI agents to Feishu (Caixin, 2026-09-16, referenced on the Caixin WeCom page). — [Caixin Global](https://www.caixinglobal.com/2026-10-14/tencent-integrates-ai-agent-workbuddy-with-enterprise-app-102493319.html)
- [Media] Sept 2026 promotional moves, as described by SMZDM: Doubao work membership expiry, WorkBuddy credits "涨价," and 千问办公 giving away 2,000 credits. — [什么值得买](https://post.smzdm.com/p/a6z08zlz/)
- [UGC] Reviewer framing: TRAE Work leans on Doubao models, QoderWork/千问办公 on Qwen, while WorkBuddy is Hunyuan-led plus third-party models. One review says WorkBuddy has the "thickest ecosystem but opaque credit consumption." — [搜狐 comparison](https://www.sohu.com/a/1055867501_122913992); [cniteyes](http://m.cniteyes.com/archives/41235)
- [Exec] Tang: WorkBuddy is "relatively ahead but the market has only just started." Alibaba and ByteDance merged their enterprise and cloud businesses, and Tencent has no current plan to do the same. On phone-maker agents, starting positions are "roughly equal." — [新浪财经](https://finance.sina.com.cn/jjxw/2026-10-30/doc-iniwyfwa2422074.shtml); [东方财富](https://finance.eastmoney.com/a/202610313917592794.html)
- [Media] Rivals compete by bundling their own product suites ("全家桶"). WorkBuddy chose an "open" route, and whether it works is "尚待市场验证." — [财联社](https://www.cls.cn/detail/2472437)
- [Exec] On how WorkBuddy will compete with model vendors' first-party agent frameworks, Martin Lau did not directly answer an analyst's question. — [36Kr EN](https://eu.36kr.com/en/p/3937200774167942)

#### Willingness to pay, subsidies, super-app bundling
- [Official via media] Yuanbao's Spring Festival 2026 "分10亿现金红包" campaign: DAU over 50M and MAU 114M, announced 2026-02-18. — [人民网](http://finance.people.com.cn/n1/2026/0218/c1004-40667526.html); [新浪科技](https://finance.sina.com.cn/tech/digi/2026-02-18/doc-inhnfyek4957827.shtml)
- [Exec] Yuanbao has since been repositioned as a co-design and atomic-capability source. Martin Lau said WeChat's Xiaowei agent would cost "less than what we actually invested in Yuanbao." — [Q2 transcript](https://www.investing.com/news/transcripts/earnings-call-transcript-tencent-posts-solid-q2-2026-growth-as-ai-spending-surges-93CH-4855197)
- [Exec] Lau: consumer AI commercialization is hard because of non-zero marginal cost. Mitchell: free users are subsidized for share. — [网易/华尔街见闻](https://www.163.com/dy/article/KVKVC3UU05198NMR.html); [Q2 transcript](https://www.investing.com/news/transcripts/earnings-call-transcript-tencent-posts-solid-q2-2026-growth-as-ai-spending-surges-93CH-4855197)
- [UGC] A gray market of cheap credits (Xianyu) points to price sensitivity. — [什么值得买](https://post.smzdm.com/p/aom0vxmm/)
- [Media] Super-app hooks: the WeChat ClawBot plugin (Mar), the WeChat Pay AI card (Jun), WeCom contacts integration (Oct), and Buddy One shared accounts and credits across Tencent Docs, Meeting and ima (Oct). — [IT之家](https://www.ithome.com/0/931/431.htm); [量子位](https://www.qbitai.com/2026/06/436160.html); [21世纪经济报道](https://m.21jingji.com/article/20261014/herald/be87f8aee8a8597b889f1f75a957a777.html); [新浪财经](https://finance.sina.com.cn/jjxw/2026-10-30/doc-iniwyfwa2422074.shtml)

#### Token and compute pricing (2026 reversal of the price war)
- [Media] In 2024 tokens were priced in "厘." In 2026 model and cloud vendors raised prices together:
  - Tencent Cloud raised Tencent HY 2.0 Instruct input from ¥0.0008 to ¥0.004505 per 1K tokens (+463%) and output by about 456%.
  - GLM 5, MiniMax 2.5 and Kimi 2.5 on Tencent Cloud ended their free public beta and became paid from 03-13.
  - China's daily token consumption reportedly rose from 30T (mid-2025) to 180T (Feb 2026).
  - [科学网, 2026-04](https://news.sciencenet.cn/htmlnews/2026/4/562401.shtm)
- [Media] Tencent Cloud raised AI compute prices by about 5%. Reports disagree on the dates: an announcement on 04-09, or an effective date of 05-09. Baidu AI Cloud raised prices by 5–30% from 04-18. — [证券时报](https://www.stcn.com/article/detail/3703467.html); [证券时报](https://www.stcn.com/article/detail/3757815.html)
- [Official] Tencent cites "更为有利的定价环境" for cloud in both Q1 and Q2 2026. — [Q1 (PRNewswire)](https://www.prnewswire.com/apac/news-releases/tencent-announces-2026-first-quarter-results-302770779.html); [Q2 (official)](https://www.tencent.com/wp-content/uploads/2026/08/%E8%85%BE%E8%AE%AF%E5%85%AC%E5%B8%83%E4%BA%8C%E9%9B%B6%E4%BA%8C%E5%85%AD%E5%B9%B4%E7%AC%AC%E4%BA%8C%E5%AD%A3%E4%B8%9A%E7%BB%A9.pdf)
- [Exec] Mitchell: tokens can carry positive gross margin "at these low token prices because the cost is low." — [Q2 transcript](https://www.investing.com/news/transcripts/earnings-call-transcript-tencent-posts-solid-q2-2026-growth-as-ai-spending-surges-93CH-4855197)

### Inferences
- **How WorkBuddy differs**:
  1. **Openness as wedge.** It routes to rival models (DeepSeek, Kimi, GLM, MiniMax) and accepts third-party IM, including Feishu and DingTalk. Rivals like 千问办公 (Qwen + DingTalk) and 豆包工作 (Doubao + Feishu) bind model and suite.
  2. **Context moat.** WeChat and WeCom identity and work data, Tencent Docs and Meeting, plus WeChat Pay rails for agent commerce.
  3. **Org placement.** It sits in Tencent Cloud (CSIG), so revenue lands as cloud and token sales. Tencent explicitly rejected merging enterprise and cloud the way Alibaba and ByteDance did.
  4. **Speed of iteration and spend.** 100 major versions in seven months, priority compute allocation, and heavy brand advertising.
- China-specific value capture is constrained in three ways: low consumer willingness to pay (subsidy-trained users and gray markets), rising token and compute costs in 2026, and rivals bundling free credits into super-apps. That pushes WorkBuddy toward enterprise seats (WeCom), the governance premium and token production for Cloud. Its consumer tiers may function as a funnel more than a profit center.
- Superseded framing: early 2026 coverage called it "Tencent's OpenClaw." By Q4 2026 Tencent frames it as an enterprise "agent OS" and suite (Buddy One). Writers should not treat the March "龙虾" framing as current positioning.

### Gaps
- I did not research Kimi (Kimi Work), MiniMax Agent, Zhipu AutoGLM or Manus in depth for 2026. Their current scale and pricing relative to WorkBuddy remain open, and other researchers may cover them.
- No reliable China-wide data on paid-conversion rates for AI apps in 2026 was found.
- The exact date and effect of the ByteDance reorganization, and DingTalk/Feishu agent pricing, were not verified from official sources.

## 7. Mapping to the three business-model elements (三要素)

### Takeaway
WorkBuddy is a platform-plus-ecosystem play inside Tencent's cloud business.
- **Value proposition:** a turnkey, safe "AI coworker" that turns one instruction into finished deliverables using your files, apps and Tencent work context, reachable from WeChat or WeCom on the phone.
- **Value creation and delivery:** a multi-model harness, a skills and connector ecosystem, the Tencent super-app and enterprise distribution, and an open hardware and industry-partner layer, with security as a feature.
- **Value capture:** freemium credits, subscriptions, enterprise seats and token production booked in Tencent Cloud, with ecosystem and commerce take-rates as future options. All of it is heavily subsidized in 2026.

### Cited Findings
- Value proposition evidence: one-instruction planning and execution ([Tencent Cloud on X](https://x.com/tencentcloud/status/2059870178264109429?lang=en)); IM remote tasks ([Tencent Cloud doc](https://cloud.tencent.com/document/product/1831/134392)); WeCom context access ([21世纪经济报道](https://m.21jingji.com/article/20261014/herald/be87f8aee8a8597b889f1f75a957a777.html)); "rich model choice and industry-leading skills library" ([Q2 official](https://www.tencent.com/wp-content/uploads/2026/08/%E8%85%BE%E8%AE%AF%E5%85%AC%E5%B8%83%E4%BA%8C%E9%9B%B6%E4%BA%8C%E5%85%AD%E5%B9%B4%E7%AC%AC%E4%BA%8C%E5%AD%A3%E4%B8%9A%E7%BB%A9.pdf)).
- Creation and delivery evidence: open platform and partners ([财联社](https://www.cls.cn/detail/2472437)); 200,000+ Skills and 50,000+ partners ([新浪科技](https://finance.sina.com.cn/tech/roll/2026-10-29/doc-iniwwpnx8872086.shtml)); security white paper ([腾讯新闻](https://news.qq.com/rain/a/20261030A09K1300)); compute prioritized to WorkBuddy inference ([Q2 transcript](https://www.investing.com/news/transcripts/earnings-call-transcript-tencent-posts-solid-q2-2026-growth-as-ai-spending-surges-93CH-4855197)).
- Capture evidence: price tables ([official](https://www.workbuddy.cn/docs/workbuddy/Pricing); [enterprise](https://cloud.tencent.com/document/product/1831/134333)); subscriptions are the majority of spend and paid-user gross margin is close to Cloud's ([Q2 transcript](https://www.investing.com/news/transcripts/earnings-call-transcript-tencent-posts-solid-q2-2026-growth-as-ai-spending-surges-93CH-4855197)); revenue flows into Tencent Cloud ([36Kr EN](https://eu.36kr.com/en/p/3937200774167942)); no revenue target ([东方财富](https://finance.eastmoney.com/a/202610313917592794.html)).

### Inferences
- **价值主张 (Value Proposition).**
  - For individuals: "一句话交付" office work (docs, PPT, data, research, file operations) with model choice, low setup and phone access through familiar IM. It is the safe, packaged "龙虾."
  - For enterprises: an agent workforce governed by WeCom identity and org permissions, with audit, sandboxing, private deployment, and the option to bring your own models, agents and IM.
  - China shaping: IM-first mobile habits (WeChat, WeCom) make the "send a message, the PC does the work" pattern natural. Regulator warnings make safety and vendor accountability part of the proposition.
- **价值创造与传递 (Creation and Delivery).**
  - Key resources: the harness, Hy models plus third-party model access, Tencent Cloud compute (Q2 capex ¥52.8B), the WeChat and WeCom user and identity graph, Tencent Docs, Meeting, ima and Drive, and WeChat Pay.
  - Key partners: model vendors (DeepSeek, Moonshot/Kimi, Zhipu/GLM, MiniMax), skill developers, hardware makers, industry ISVs and FDEs (broker, fund, legal, HR and BI vendors), and Meituan for commerce.
  - Channels: self-serve desktop and mobile app, WeChat and QQ entry, WeCom admin console, Tencent Cloud enterprise sales, and brand advertising.
  - China shaping: super-app distribution and the government and SOE-heavy enterprise base favor big-tech platforms. A competitive "全家桶" bundling environment forces the open-platform differentiation.
- **价值获取 (Capture).**
  - Revenue: tiered subscriptions (¥0–999/month), credit top-ups (~¥0.05/credit), enterprise seats (¥198–316/seat/month plus private deployment), token and MaaS revenue in Tencent Cloud, skill revenue-share, agent commerce (AI专属卡), and possible WeCom upsell.
  - Cost structure: inference tokens (task tokens far exceed chat), free-user subsidies, model training, and marketing. New AI products cost about ¥10.5B of Q2 operating profit.
  - Monetization logic: Tencent captures value less as a SaaS fee and more as (a) cloud and token volume, (b) enterprise relationship monetization through WeCom, and (c) Hy model improvement via usage data ("co-design flywheel").
  - Hedge: compute rental.
  - China shaping: low consumer willingness to pay, a 2026 token and compute price upswing, and rivals' free-credit promotions. These explain the July and September price increases and the pivot toward enterprise seats.

### Gaps
- Without disclosed revenue, paying users or margins, the mapping's capture element rests on management qualitative statements and list prices. Q3 2026 results (expected mid-November 2026) may add disclosure.
