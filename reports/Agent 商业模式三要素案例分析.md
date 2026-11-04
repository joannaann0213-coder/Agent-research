# 谁为 Agent 的自主性买单：商业模式三要素的成立条件

> 调研日期：2026-11-03。美元价格写作 $，大额写作"亿美元"；人民币写作 ¥；不做汇率换算。
> 主案例：Claude Tag（Anthropic 的 Claude in Slack）、Grok Bot（SpaceX/xAI 的 always-on agent 产品）、腾讯 WorkBuddy。对照：X 上被 @ 时回复的 @grok 账号。
> "Grok bot" 有两个含义。本文用 **Grok Bot** 指 2026-08-11 发布的 agent 产品，用 **@grok** 指 X 上的回复账号，见第 2 节。
> 三家都没有披露这些产品的收入、毛利或留存。"价值获取"的判断依据是价目表、计费规则、管理层表态和已发生的事件。Claude Tag 和 Grok Bot 仍是 beta，腾讯 Q3 财报尚未发布。
> xAI 已并入 SpaceX，AI 品牌现为 SpaceX Super Intelligence（SpaceXSI）。SpaceX 把 X、Grok、Cursor 和算力出租合并报告为 "SI" 分部。

## 结论

- **三要素同时成立，需要三个条件。** ① 价值主张：卖能验收的工作，卖给有工作预算的人。② 价值创造与传递：Agent 待在上下文、身份和权限所在的渠道里，渠道主人要么是自己，要么离不开自己。③ 价值获取：计价单位跟着成本走，付钱的人看得见、封得住。对 always-on agent，第三条最难：它越主动、越 24 小时干活，用户越满意，成本也越高。
- **三个主案例的价值主张都成立，分歧在价值获取。** Grok Bot 上线不到两个月就有 80 万付费周活（管理层口径）。WorkBuddy 9 月全端 MAU 3,235 万，国内办公 Agent 第一。Anthropic 产品团队 65% 的代码由内部版 Claude Tag 产出。
- **Claude Tag：三要素的机制都成立，规模还没证明。** 频道工作按 API 标价从组织余额扣费。"监听"频道免费，主动和定时任务收费。Team 版先付费后使用，成本风险基本在客户一侧。软肋在渠道：Slack 属于 Salesforce，Slackbot 免费捆绑；但 Slackbot 本身跑在 Claude 上，双方互相依赖。
- **Grok Bot：主张最宽，价值获取不成立（截至 2026-11-03）。** 它被放进 $30–$200 的 X Subscription，和 Grok 聊天、Cursor 共用每周额度池，档位越高单价越低（Ultra 每单位比 Premium 便宜 47%）。10-31 至 11-01 用户额度提前耗尽，Musk 承认服务成本"相对收入高得不可持续"（TechCrunch 2026-11-02 报道）。
- **WorkBuddy：渠道最强，账靠补贴撑。** 微信、企业微信和腾讯文档给了它别人拿不到的上下文和身份。管理层称付费用户毛利已与腾讯云整体相当，但免费用户在被补贴。7 月入门价从 ¥58 提到 ¥99。包括 WorkBuddy 在内的新 AI 产品让腾讯 Q2 经营利润少了约 ¥105 亿。
- **@grok 是对照：渠道和模型都自有，分发和算力几乎不花钱，成本落在治理。** 它没有直接收入，2026-03-11 起只是 X Premium 的一项权益。平台要为它的每个错误担责，Grok 在 X 上的功能已引来欧盟 DSA 程序和英国 Ofcom 调查。
- **账常常在 Agent 之外结，前提是那一层归你，而且不被 Agent 挤占。** SpaceX 的 SI 分部 Q3 扭亏，收入增长主要来自算力出租和 Cursor，Grok Bot 至多约占 1–6%。算力能按现货价出租，平价订阅里的 Agent 用量就成了机会成本。腾讯把 WorkBuddy 的现金记进腾讯云，称"给 WorkBuddy 生产 token"价值最持久。Anthropic 一边和 Grok Bot 竞争，一边向 SpaceX 租算力，还被 Grok Bot 路由调用 Opus 5.5。

## 1. 框架：把三要素拆成九个可检验的条件

本文的三要素取自张敬伟、王迎军（2010）的"价值三角形"：价值定义、价值创造与传递、价值获取 ([上海财经大学期刊](https://qks.shufe.edu.cn/J/ArticleQuery/A160824002159/CN))。本文把"价值定义"称为价值主张。中文里"商业模式三要素"没有统一定义，所以先对齐口径。它和英文经典框架一一对应 ([Teece 2010](https://www.kth.se/social/upload/528c4881f2765458b97a9368/Teece%20%282010%29.pdf); [HBR](https://hbr.org/2008/12/reinventing-your-business-model))：

| 本文要素 | 对应概念 | 放到 Agent 上要回答的问题 |
|---|---|---|
| ① 价值主张 | Teece 的 value proposition；Johnson 等的客户价值主张 | 替谁完成什么工作？比人工或现有软件好在哪？ |
| ② 价值创造与传递 | Teece 的 creation and delivery mechanisms；Johnson 等的关键资源 + 关键流程 | 上下文、身份和权限从哪来？在哪个渠道交付？模型和算力从哪来？ |
| ③ 价值获取 | Teece 的 capture mechanisms；Johnson 等的利润公式 | 谁付钱？按什么单位？毛利多少？利润会不会被渠道方或模型方拿走？ |

Agent 和传统 SaaS 有三处不同，三处最终都压在价值获取上：

| | 传统 SaaS | Agent |
|---|---|---|
| 卖什么 | 工具，按人头收费 | 工作。红杉的说法是 "A copilot sells the tool. An autopilot sells the work." ([Sequoia](https://sequoiacap.com/article/services-the-new-software/)) |
| 边际成本 | 接近零 | 每个任务都要推理。Gartner 称 Agent 完成同一任务要多用 5–30 倍 token，单个 agentic 工作流的推理成本到 2028 年将涨 5 倍以上 ([VentureBeat](https://venturebeat.com/ai/gartner-says-agent-costs-will-rise-fivefold-by-2028-even-as-tokens-get-cheaper-here-is-how-to-keep-them-in-check)) |
| 毛利 | 常见 70–80% ([Newcomer](https://www.newcomer.co/p/cursors-popularity-has-come-at-a)) | ICONIQ 调查：AI 产品毛利 2026 年预计 52%，推理平均约占收入 23% ([SaaStr](https://www.saastr.com/iconiqs-latest-state-of-ai-report-the-10-most-important-data-points-for-saas-founders)) |

据此，把"成立"拆成九个条件：

| 要素 | 条件 | 检验问题 |
|---|---|---|
| ① | C1 能验收 | 产出是 PR、工单、文档这类能验收的工作，还是一段答案？ |
| ① | C2 有人买单 | 付钱的人有与工作量挂钩的预算吗（人力、外包、IT）？还是只靠个人零花钱？ |
| ① | C3 可托付 | 出错谁负责？能否审计、限权、回滚？ |
| ② | C4 上下文与身份 | Agent 能读到工作上下文吗？以谁的身份行动？ |
| ② | C5 渠道靠得住 | 渠道主人有没有自家 Agent？条款允许你存数据吗？会不会限流或掐断？ |
| ② | C6 单任务成本可控 | 模型和算力是自有、租用，还是向竞争对手买？谁定价？ |
| ③ | C7 计价跟着成本走 | 收费单位会不会随 token、算力时长同向变化？ |
| ③ | C8 看得见、封得住 | 付费方能否看到花费、设上限？ |
| ③ | C9 毛利守得住 | 毛利有没有路走向约 60%？会不会被渠道方或模型方拿走？（60% 是 Bessemer 对优秀 AI 公司的观察值 ([BVP](https://www.bvp.com/atlas/the-state-of-ai-2025))） |

第 7 节用这张表给四个对象打分。

## 2. 案例速览：两个 "Grok bot"，一个卖工作，一个卖回复

| | Claude Tag | Grok Bot | 腾讯 WorkBuddy | @grok（对照） |
|---|---|---|---|---|
| 公司 | Anthropic | SpaceX（SpaceXSI）；账号、云电脑、后台都在 Cursor | 腾讯云（CSIG） | SpaceX 旗下 X |
| 发布 | 2026-06-23 公布，beta | 2026-08-11，beta | 2026-03-09 国内公开发布 | 2025 年 3 月起被报道 |
| 一句话 | Slack 频道里共享的 AI 同事 | 一组 24/7 干活、自带云电脑的 Bot | 一句话交付的桌面办公 Agent | 被 @ 时公开回复的平台账号 |
| 主要入口 | Slack；Teams 版 "coming soon" | 自家 app、X Chat、X 上的 @bot、Slack Team Bots | 桌面和手机 app；微信、企业微信、QQ 远程派活 | X 帖子回复 |
| 在哪执行 | 每个 thread 一个云沙箱 | 每个账号一台持久云电脑（Cursor 云） | 本机为主，可上云；企业版 WMA 云端 24/7 | 不执行任务 |
| 模型 | Anthropic 自家 Opus、Sonnet，可切换 | 自动路由，无选择器，含 Claude Opus 5.5 | 混元/Hy、DeepSeek、GLM、Kimi、MiniMax 可选 | Grok |
| 收费 | Team/Enterprise 方案 + 频道工作按 API 标价计量 | X Subscription $30/$100/$200 周额度池 + 超额 credits | 免费/¥99/¥199/¥999 + 积分；企业 ¥198/¥316 每席每月 | 无直接收费；召唤需 X Premium |
| 公开规模 | 无；Anthropic 内部 65% 产品代码由它产出 | 80 万付费周活（2026-10 初，管理层口径） | 3,235 万 MAU（2026-09，QuestMobile 全端） | 无官方数据 |

表中数据的出处见第 3–6 节。

"Grok bot" 指两个商业模式完全不同的东西，必须分开：

| | Grok Bot（主案例） | @grok（对照） |
|---|---|---|
| 是什么 | "your team of always-on agents"：一组有名字的 Bot，替你在各种 app 里干活 | X 的平台官方账号，被 @ 时在帖子下公开回复 |
| 发布 | 2026-08-11，beta | 2025 年 3 月起被广泛报道 |
| 在 X 上的入口 | @bot，不是 @grok。同时 @ 两者的帖子会被 @bot 忽略 | @grok |
| 输出去哪 | 活在 Grok Bot app 里私下干完；@bot 只公开回一条确认 | 公开回复，所有读者可见 |
| 账号与后台 | Cursor（Anysphere，已被 SpaceX 收购）：Cursor 账号、Cursor 云、Cursor dashboard | X / SpaceXSI |
| 模型 | 自动路由，含 Anthropic 的 Claude Opus 5.5 | Grok |
| 怎么收费 | X Subscription Premium（$30/月）起，每周额度池 | 无直接收费；2026-03-11 起只有 X Premium、Premium+ 能召唤 |
| 本文角色 | 与 Claude Tag、WorkBuddy 同类，按三要素逐一分析 | "渠道方自有模型"的对照 |

来源：([x.ai](https://x.ai/news/introducing-grok-bot); [Grok Bot 文档：Tag @bot on X](https://docs.x.ai/grok-bot/tag-on-x); [Inshorts](https://inshorts.com/en/news/x-restricts--ask-grok--reply-feature-to-premium--premium--users-1773244141032))。

## 3. Claude Tag：按标价计量，渠道是租来的

Claude Tag 是 Anthropic Slack 应用的第三代。2026-06-23 公布，08-03 起默认切换，10-05 起已配对的工作区停用旧版 ([Anthropic](https://www.anthropic.com/news/introducing-claude-tag); [Claude 文档：迁移](https://claude.com/docs/claude-tag/admins/migrate-from-earlier))。它仍是 public beta。只对 Team 和 Enterprise 开放，Free、Pro、Max、Ultra 等个人方案用不了，也不支持第三方部署 ([Claude 文档：概览](https://claude.com/docs/claude-tag/overview))。

| 要素 | 怎么做 | 关键证据 | 成立吗 |
|---|---|---|---|
| ① 价值主张 | 一个频道一个共享的 Claude。任何人 @Claude 交活，进度公开；有频道记忆、routines、主动跟进 | 内部 65% 产品代码由它产出；Carvana 告警减少 56%（厂商宣传）；没有采用数据 | 成立 |
| ② 创造与传递 | 自家模型 + 每个 thread 一个 Claude Code 云沙箱 + 连接器。经 Slack Marketplace 应用交付；凭证、记忆、账单都在 Anthropic 一侧 | Slack 归 Salesforce；Slackbot 免费捆绑，但跑在 Claude 上 | 成立，依赖 Slack |
| ③ 价值获取 | 先买 Team/Enterprise；频道工作按 API 标价从组织余额扣，不按人头 | 没有产品收入披露 | 机制成立，规模未证 |

### 3.1 价值主张：从单人工具变成团队同事

Anthropic 的定位是 multiplayer："One Claude per channel serves everyone" ([Anthropic](https://www.anthropic.com/news/introducing-claude-tag))。产品负责人 Cat Wu 说得更直接：Claude Code、Cowork 和聊天都是 "very single-player"，Claude Tag 是为多人协作做的 ([Fortune](https://fortune.com/2026/06/23/anthropic-claude-tag-virtual-employee-tool-slack/))。

交付物是能验收的工作，不是聊天回答。官方用例包括盯告警、分诊请求频道、查数仓并出图、把 thread 整理成文档和工单、从 bug 报告到 PR 草稿再跟到 CI 通过 ([Claude 文档：概览](https://claude.com/docs/claude-tag/overview))。

证据偏内部和厂商宣传。Anthropic 称产品团队 **65% 的代码** 由内部版 Claude Tag 产出 ([Anthropic](https://www.anthropic.com/news/introducing-claude-tag))。产品页称 Carvana 的告警减少了 **56%** ([claude.com/tag](https://claude.com/tag))。组织数、周活、任务量都没有披露。

真正的变化在身份和计费：

| | 旧版 Claude in Slack | Claude Tag |
|---|---|---|
| 身份 | 每个用户自己的 claude.ai 账号 | 一个组织级 Agent 身份，用服务凭证 |
| 会话 | 每次请求新开 | 每个 thread 一个持久会话，频道共享 |
| 记忆 | 无 | 频道记忆 + 工作区笔记 |
| 常驻工作 | 无 | routines、盯频道 |
| 谁来设置 | 每个用户 | Owner 设置一次 |
| 计费 | 用户自己的套餐额度 | 组织用量余额 |

来源：([Claude 文档：迁移](https://claude.com/docs/claude-tag/admins/migrate-from-earlier); [Claude Code 文档：Slack](https://code.claude.com/docs/en/slack))。

所以买方是 IT 和安全负责人，用户是整个频道。对买方的卖点是一个受治理的 Agent 身份：服务账号、按频道隔离、审计、支出上限。这正对着旧版"每人各自授权"带来的影子 AI 问题。

### 3.2 价值创造与传递：Anthropic 管运行时，Slack 管入口

Anthropic 一侧负责干活。每个 thread 跑在独立的临时云沙箱里，引擎与网页版 Claude Code 相同 ([Claude 文档：工作原理](https://claude.com/docs/claude-tag/concepts/how-it-works))。凭证不进沙箱，由 Agent Proxy 在请求时注入；出网默认拒绝 ([Claude 文档：安全](https://claude.com/docs/claude-tag/concepts/security-and-data))。记忆"属于频道，不属于你" ([Claude 文档：记忆](https://claude.com/docs/claude-tag/users/memory))。

Slack 一侧负责入口：身份、界面、分发、读消息。这一侧的规矩由 Salesforce 定。2025-05-29 起，Slack 规定 Marketplace 是商业分发的唯一渠道；未上架的应用读历史消息被限到每分钟 1 次、每次 15 条 ([Slack 开发者日志](https://docs.slack.dev/changelog/2025/05/29/rate-limit-changes-for-non-marketplace-apps))。据律所 Hunton 解读，新条款还禁止批量导出、建持久索引，以及把 API 数据用于 LLM ([Hunton](https://www.hunton.com/insights/legal/salesforce-locks-down-slack-data-time-to-review-your-slack-api-terms))。Claude Tag 是上架应用，在许可范围内。

这套分工满足了"进别人渠道的模型厂商"守住价值的关键：身份、记忆和账单都留在自己一侧。也留下一个摩擦点：由 Slack 数据积累的频道记忆存在 Anthropic 一侧，而这正是 Slack 条款收紧的方向（推断）。

渠道主人也是竞争者。Slackbot 在 Business+ 和 Enterprise+ 里"不额外收取用量费"。但 Slackbot 的 30 多项新能力全部跑在 Claude 上 ([TNW](https://thenextweb.com/news/slack-slackbot-30-ai-features-agentic))。Salesforce 投资 Anthropic 超过 3 亿美元，约占 1%。Benioff 在播客中说今年预计花 3 亿美元买 Anthropic 的 token，这一说法未经两家确认 ([TNW](https://thenextweb.com/news/salesforce-benioff-300-million-anthropic-tokens-slack-coding))。Engadget 在 07-25 更正报道，写明这次发布是 Anthropic 与 Slack 合作 ([Engadget](https://engadget.com/2199619/anthropic-announces-claude-tag-for-slack))。互相依赖降低了被掐断的概率。Microsoft Teams 版标着 "Coming soon"，是对单一渠道的对冲 ([claude.com/tag](https://claude.com/tag))。

治理有几处硬伤：

- Claude 能按关键词搜索所有公共频道，"没有任何管理设置能关掉"，只能限制范围 ([Claude 文档：医疗](https://claude.com/docs/claude-tag/admins/healthcare))。
- 不在 BAA 覆盖范围内。开了零数据保留、客户自管密钥或 HIPAA 配置的组织用不了 ([Claude 文档：医疗](https://claude.com/docs/claude-tag/admins/healthcare))。
- beta 期间没有自动保留期 ([Claude 文档：数据生命周期](https://claude.com/docs/claude-tag/concepts/data-lifecycle))。
- 频道里任何人都能让 Claude 用该频道的服务凭证干活 ([Claude 文档：安全](https://claude.com/docs/claude-tag/concepts/security-and-data))。

### 3.3 价值获取：主动性本身就是收费点

Claude Tag 是"平台方案 + 计量 Agent"的混合模式。先要有 Team 或 Enterprise 方案。之后频道里的工作不按人头收费，Slack 用户也不需要 Claude 席位 ([Claude 文档：概览](https://claude.com/docs/claude-tag/overview))。

| 工作 | 从哪扣费 | 上限 |
|---|---|---|
| 频道、thread 里的工作 | 组织用量余额 | 组织支出上限 + 每频道上限 |
| 群 DM | 组织用量余额 | 支出上限 + 群 DM 默认上限 |
| 读频道、判断要不要回、凭已有信息简短回复 | 不收费 | — |
| 有 Claude 账号的成员发 1:1 DM | 发送者自己的席位 | 席位原有限额 |

来源：([Claude 文档：支出上限](https://claude.com/docs/claude-tag/admins/set-spend-limit))。

- **按标价计量，折扣开票时再扣。** Team 版必须先设上限、买额度，Claude 才会在频道里回复。Enterprise 月结，"在设上限之前没有上限" ([Claude 文档：支出上限](https://claude.com/docs/claude-tag/admins/set-spend-limit))。除了不计费的监听和 beta 期的记录存储，成本风险基本在客户一侧。
- **主动性就是收费点。** 花费分三类计费：被叫来干活（Engaged）、自己发起（Proactive）、定时任务（Scheduled）。"监听"频道不计费 ([Claude 文档：支出上限](https://claude.com/docs/claude-tag/admins/set-spend-limit))。免费的在场让 Claude 能进更多频道，从而扩大能产生计费工作的范围。
- **扩张回路是内置的。** 默认工作区里任何人都能 @Claude。Claude 每周私信建议加入更多频道 ([Claude 文档：Slack 管理员](https://claude.com/docs/claude-tag/admins/for-slack-admins))。没有 Claude 账号的成员可试用 DM（7 天、$50 或 50 次会话，先到为准），用完提示连接自己的账号，改从个人席位扣 ([Claude 文档：限制访问](https://claude.com/docs/claude-tag/admins/restrict-access))。上线优惠给每个 Enterprise 组织 $25,000、Team 组织 $2,500 的频道额度；09-02 起须连接 GitHub，10-01 到期 ([Claude 帮助中心](https://support.claude.com/en/articles/15575654-claude-tag-launch-promo-for-claude-team-and-enterprise))。
- **个人用量进订阅池，团队工作单独计量。** 同一家公司，按真实账号的混合流量实测，Claude Pro 用满额度折合的 API 等值约为月费的 34 倍 ([Wokey](https://wokey.ai/claude-usage-limits)，2026-10-19 至 10-26 测算，见本仓库 `subscription-vs-api-2026-10.md` 第 3 节)，Claude Tag 的 1:1 DM 也从个人席位扣。但频道里的团队工作没有放进订阅池，而是按 API 标价计量。Grok Bot 的做法正好相反（第 4 节）。

弱点有两个。第一，客户难以预测花费。官方明说"没有公布的单任务成本指引" ([Claude 文档：支出上限](https://claude.com/docs/claude-tag/admins/set-spend-limit))。第二，规模和毛利未知。Claude Tag 的收入、组织数、平均花费都没有披露。公司层面，Anthropic 2 月官方披露的年化收入是 140 亿美元 ([Anthropic](https://www.anthropic.com/news/anthropic-raises-30-billion-series-g-funding-380-billion-post-money-valuation))，7 月底据报约 650 亿美元 ([Axios](https://axios.com/2026/08/17/anthropic-revenue-run-rate-ipo-openai))。按标价计量把推理成本转给了客户，但标价本身的毛利取决于推理成本：据 The Information 报道，Anthropic 曾因推理成本高于预期，把 2025 年毛利预测从 50% 下调到 40% ([老虎证券转述](https://www.itiger.com/news/1154519858))。

**判断：** ① 成立，工程和 on-call 是切入点。② 成立，前提是 Slack 继续把它当伙伴。③ 的机制在三案中最健康：计价跟着成本走，付费方能封顶，客户关系和账单都在 Anthropic 手里。缺的是规模证据。

## 4. Grok Bot：80 万付费周活，撞上平价额度池

Grok Bot 是 SpaceX 在 2026-08-11 发布的 always-on agent 产品，仍是 beta ([x.ai](https://x.ai/news/introducing-grok-bot))。一个账号可以建多个有名字的 Bot，各有记忆、routines 和屏幕，共用一台持久云电脑（浏览器、终端、文件系统）。这台电脑跑在 Cursor 的云上 ([Grok Bot 文档：概览](https://docs.x.ai/grok-bot/overview))。登录用 Cursor 账号，管理后台在 Cursor dashboard，条款署名 Anysphere ([Cursor 文档：团队](https://cursor.com/docs/grok-bot/teams); [Grok Bot 条款](https://cursor.com/terms/grok-bot))。运营上，它更像挂 Grok 品牌的 Cursor 产品。SpaceX 在 08-14 完成对 Cursor 的收购，隐含股权价值 600 亿美元 ([SpaceX Q3 财报](https://www.sec.gov/Archives/edgar/data/1181412/000162828026066482/spaceexplorationtechnologi.htm))。

| 要素 | 怎么做 | 关键证据 | 成立吗 |
|---|---|---|---|
| ① 价值主张 | 24/7 干活的 AI 同事，自带云电脑，能操作没有 API 的网站。官方卖给销售、运营、工程团队；Musk 个人推广消费场景 | 80 万付费周活（10 月初，管理层口径） | 成立，但两类受众互相拉扯 |
| ② 创造与传递 | SpaceX 算力 + Cursor 运行时 + 多家模型路由（含 Opus 5.5）。入口是自家 app、X Chat、X 上 @bot、Slack Team Bots；靠 X、Grok、Cursor 捆绑分发 | 没有模型选择器；路由到 Opus 5.5 不改变用户单价 | 分发强，成本不全由自己控制 |
| ③ 价值获取 | X Subscription $30/$100/$200，与 Grok、Cursor 共用每周额度池；超额买预付 credits | 10-31 至 11-01 额度提前耗尽；Musk 称成本"相对收入高得不可持续" | 不成立（截至 2026-11-03） |

### 4.1 价值主张：最宽，也最分裂

官方卖点是"一支 always-on 的 agent 团队"：你离开时工作不停，能登录没有 API 或 MCP 的网站，只在需要批准时回来找你 ([x.ai](https://x.ai/news/introducing-grok-bot))。官方用例偏 B2B：夜里做客户研究、写外联草稿、整理 CRM、检查演示环境。SpaceX 总裁 Shotwell 在 Q3 电话会上说，B2B 是增长最快的类别 ([SpaceX Q3 电话会实录](https://www.fool.com/earnings/call-transcripts/2026/10/20/spacex-spcx-q3-2026-earnings-call-transcript/))。

Musk 个人推的是消费场景：拍信用卡照片让 Bot 购物、订乐高、订机票 ([TNW](https://thenextweb.com/news/musk-grok-bot-credit-card-spacex-make-you-whole))。消费端带来量，B2B 带来毛利。消费端的责任风险也最大。

需求是真的。早期测试者 Lenny Rachitsky 称它像 OpenClaw，但"简单、可靠、没那么吓人" ([VentureBeat](https://venturebeat.com/orchestration/spacexais-grok-bot-turns-agents-into-persistent-digital-coworkers-that-can-operate-your-apps-for-120-per-month))。Shotwell 说 10 月初已有 **80 万付费周活**，比上月翻倍还多。Musk 称这是他见过增长最快的产品，"每个用户的用量随时间增长，不会下降" ([SpaceX Q3 电话会实录](https://www.fool.com/earnings/call-transcripts/2026/10/20/spacex-spcx-q3-2026-earnings-call-transcript/))。"付费周活"没有定义，也不在 10-Q 里。

信任环节有裂缝。条款把几乎所有风险推给客户：除重大过失和故意不当行为外，提供方不担责；涉及金融交易的操作须人工审核 ([Grok Bot 条款](https://cursor.com/terms/grok-bot))。Musk 却公开承诺：Bot 买错东西或泄露信用卡，"SpaceX 会全额赔偿" ([TNW](https://thenextweb.com/news/musk-grok-bot-credit-card-spacex-make-you-whole))。CEO 的承诺与合同不一致，本身就是治理风险。

### 4.2 价值创造与传递：分发靠捆绑，成本不全在自己手里

关键资源有三块：SpaceX 的算力（9 月底 1.7 GW）([SpaceX Q3 财报](https://www.sec.gov/Archives/edgar/data/1181412/000162828026066482/spaceexplorationtechnologi.htm))、Cursor 的 Agent 运行时、X/Grok/Cursor 的付费用户基数。它独特的资产是捆绑分发，不是独家模型。

**模型是外部化的。** 文档写明没有模型选择器，每个任务自动选模型。可能是自家模型，也可能是第三方，包括 Anthropic 的 Opus 5.5；"把任务路由到 Opus 5.5 不改变你的每 token 成本" ([Grok Bot FAQ](https://docs.x.ai/grok-bot/faq))。10-07 Musk 宣布，SpaceX 今后"为每个任务用最好的后端模型" ([TNW](https://thenextweb.com/news/grok-bot-claude-opus-midjourney-suno-musk))。也就是说，一部分推理要向竞争对手买，差价由 SpaceX 吸收。路由比例没有披露。

**身份是借用的。** "Bot 没有自己的身份或凭证"，它以登录成员的身份行动 ([Cursor 文档：安全](https://cursor.com/docs/grok-bot/security))。一个账号的所有 Bot 共用一台电脑的文件、浏览器会话和登录 ([Grok Bot 文档：概览](https://docs.x.ai/grok-bot/overview))。这省算力，也扩大了出事时的波及面。没设网络策略的团队默认全部放行 ([Cursor 文档：安全](https://cursor.com/docs/grok-bot/security))。

**渠道是多点的。** 入口有桌面和手机 app、X Chat、X 上的 @bot，以及 09-28 推出的 Slack Team Bots ([Grok Bot FAQ](https://docs.x.ai/grok-bot/faq); [X/@bot](https://x.com/bot/status/2104661562715967548))。@bot 在欧盟、欧洲经济区、英国和澳大利亚不可用 ([Grok Bot 文档：Tag @bot on X](https://docs.x.ai/grok-bot/tag-on-x))。在 Slack 里，它和 Claude Tag 一样是客人：要有 Cursor 团队和 Slack 管理员批准，并且"代表 @ 它的那个人"行动 ([Grok Bot 文档：共享 Bot](https://docs.x.ai/grok-bot/shared-bots))。

### 4.3 价值获取：平价池子装不下 always-on 成本

计价三个月里变了三次：

| 时间 | 准入与计价 | 来源 |
|---|---|---|
| 2026-08-11 | 只开放给 SuperGrok Heavy（$300/月）、Cursor Ultra（$200/月）、Cursor Teams Premium（$120/席/月）；"自带独立用量" | [VentureBeat](https://venturebeat.com/orchestration/spacexais-grok-bot-turns-agents-into-persistent-digital-coworkers-that-can-operate-your-apps-for-120-per-month); [x.ai](https://x.ai/news/introducing-grok-bot) |
| 约 08-26 | 据第三方报道，扩大到全部 SuperGrok、Cursor Pro（$20/月）和 Cursor Teams；官方页面已列出扩大后的名单，但没有注明日期 | [KuCoin](https://www.kucoin.com/news/flash/grok-bot-access-expanded-to-20-month-cursor-pro-subscribers) |
| 10-12/13 | 并入 X Subscription：Premium $30、Super $100、Ultra $200 含 Grok Bot，与 Grok、Cursor 共用每周额度池；Plus $8 不含 | [X Subscription 文档](https://docs.x.ai/x-subscription/pricing); [Cursor 帮助](https://cursor.com/help/grok-bot/plans) |
| 10-31 至 11-01 | 用户周额度提前耗尽；Musk 回复称服务成本"相对收入高得不可持续"。TechCrunch 于 2026-11-02 报道 | [TechCrunch（Yahoo 转载）](https://finance.yahoo.com/technology/ai/articles/elon-musk-throttles-grok-bot-162321608.html); [TNW](https://thenextweb.com/news/grok-bot-costs-unsustainably-high-musk-usage-limits) |

用量按 "agent steps and tokens" 计，不按消息数 ([Grok Bot FAQ](https://docs.x.ai/grok-bot/faq))。官方只公布档位倍数，不公布绝对额度。按官方价格和倍数算，每单位基础额度的月价如下（价格和倍数来自 [X Subscription 文档](https://docs.x.ai/x-subscription/pricing)，单价为计算值）：

| 档位 | 月费 | 额度倍数 | 每单位月价 |
|---|---|---|---|
| Premium | $30 | 4x | $7.50 |
| Super | $100 | 15x | $6.67 |
| Ultra | $200 | 50x | $4.00 |

用得越多，单价越低：Ultra 每单位比 Premium 便宜 47%。always-on 的重度用户正是会升到 Ultra 的人，而 Musk 自己说他们的用量还会随时间增长。毛利在用量最高的地方下降最快。

**限流经过。** 10-31 至 11-01 的万圣节周末，部分用户没在夜里用，周额度也被耗尽，有人看到用量从 50% 跳到 100% ([TechCrunch（Yahoo 转载）](https://finance.yahoo.com/technology/ai/articles/elon-musk-throttles-grok-bot-162321608.html))。据《每日电讯报》（经 TNW 转述），有用户的计量条从 33% 跳到 66%，几位用户的额度"被砍半"。Cursor 在 11-01 报告了计费和看板故障，随后标记为已修复 ([TNW](https://thenextweb.com/news/grok-bot-costs-unsustainably-high-musk-usage-limits))。Musk 11-01 在回复里写道："The cost of serving Grok Bot was unsustainably high relative to revenue"，并确认这不是故障 ([TechCrunch（Yahoo 转载）](https://finance.yahoo.com/technology/ai/articles/elon-musk-throttles-grok-bot-162321608.html))。官方文档没有任何调整说明。用户称之为 "rug pull" ([TechRadar](https://www.techradar.com/ai-platforms-assistants/elon-musk-says-grok-bot-was-throttled-because-costs-were-unsustainably-high-as-users-call-the-move-a-rug-pull))。就在 12 天前，Musk 还在电话会上说 Grok Bot "可能最终用掉我们大部分的 SI 算力" ([SpaceX Q3 电话会实录](https://www.fool.com/earnings/call-transcripts/2026/10/20/spacex-spcx-q3-2026-earnings-call-transcript/))。

价值获取不成立，原因是四个错配：

1. **平价订阅装不确定的成本。** Agent 的成本随自主运行时长上涨，订阅价不动。
2. **共享池加不透明计量。** Grok Bot 和聊天、编程共用一个周额度。routines 会自己跑；X 上的 @bot 标记会"自动开始工作并消耗你的 Grok Bot 用量" ([Grok Bot 文档：Tag @bot on X](https://docs.x.ai/grok-bot/tag-on-x))。用户对不上额度跳变和自己的行为，于是觉得被 "rug pull"。
3. **团队资产，个人买单。** Slack 里的共享 Bot 扣"被回复的那个人"的个人额度 ([Grok Bot 文档：共享 Bot](https://docs.x.ai/grok-bot/shared-bots))。目前没有 Grok Bot 专属的支出上限 ([Cursor 文档：团队](https://cursor.com/docs/grok-bot/teams))。
4. **自有算力不等于免费算力。** Musk 说 SpaceX 的算力一半自用、一半按接近现货的价格出租。CFO 说新增算力按当前价格不到一年回本，公司会"持续评估算力的最高毛利用途" ([SpaceX Q3 电话会实录](https://www.fool.com/earnings/call-transcripts/2026/10/20/spacex-spcx-q3-2026-earnings-call-transcript/))。平价订阅消耗的每个 GPU 小时，都在和出租竞争。

量级也说明问题。80 万付费周活 × $30–$200/月，每季最多 0.72–4.8 亿美元，约为 SI 分部 Q3 收入（75.6 亿美元）的 1–6%（计算；这是上限，很多用户本来就为 Cursor 或 SuperGrok 付费）([SpaceX Q3 财报](https://www.sec.gov/Archives/edgar/data/1181412/000162828026066482/spaceexplorationtechnologi.htm))。对 SpaceX 来说，收紧额度代价小，放任用量的 GPU 代价大。

修复手段都已露头：抬高入门价（Plus 不含 Grok Bot；据第三方报道，$20 的 Cursor Pro 路线不再对新用户开放）；只公布倍数，基础额度可以悄悄调；推动升档（Musk 曾问一位用户"更大的套餐会不会太贵"）；超额 credits 加自动充值；路由到更便宜的模型 ([TNW](https://thenextweb.com/news/grok-bot-costs-unsustainably-high-musk-usage-limits); [Cursor 帮助](https://cursor.com/help/grok-bot/plans))。这些手段都在把它推向按量计费。

**判断：** ① 成立。② 分发成立，但执行层在 Cursor，部分模型在 Anthropic，成本结构不稳。③ 截至 2026-11-03 不成立。

## 5. 腾讯 WorkBuddy：自有渠道最强，靠补贴换份额

WorkBuddy 是腾讯云（CSIG）的桌面办公 Agent，和编程 Agent CodeBuddy 同一负责人、共用积分 ([财联社](https://www.cls.cn/detail/2472437); [WorkBuddy 定价](https://www.workbuddy.cn/docs/workbuddy/Pricing))。2026-03-09 国内公开发布，05-28 发布国际版 ([TechNode](https://technode.com/2026/03/09/tencent-launches-openclaw-like-workplace-ai-agent-workbuddy/); [TechNode](https://technode.com/2026/05/29/tencent-launches-workbuddy-productivity-ai-agent-for-global-users/))。早期它被称作"免部署的 OpenClaw（龙虾）"。到 10 月，定位变成企业的"Agent 操作系统"和 Buddy One 套件。

| 要素 | 怎么做 | 关键证据 | 成立吗 |
|---|---|---|---|
| ① 价值主张 | 一句话交付：在本机文件和应用上规划并执行，产出文档、PPT、数据分析；手机上用微信、企业微信、QQ 派活；"安全的龙虾" | 9 月全端 MAU 3,235 万，办公 Agent 第一；7 个月 90 亿次任务请求（管理层口径） | 成立 |
| ② 创造与传递 | 多模型 harness + 20 万+ Skills；微信、企业微信的身份和腾讯文档等上下文；微信支付 AI 专属卡；腾讯云算力 | 10-14 进入企业微信通讯录；10-29 发布 Buddy One 和 WMA | 成立，三案中最强 |
| ③ 价值获取 | 免费/¥99/¥199/¥999 订阅 + 积分加购 + 企业席位 ¥198/¥316 + token 收入记入腾讯云 | 多数支出来自订阅；付费用户毛利与腾讯云整体相当；免费用户被补贴；没有收入目标 | 部分成立：付费层成立，整体在补贴期 |

### 5.1 价值主张：一句话交付，加上"安全的龙虾"

核心是把一条自然语言指令拆成多步并执行 ([腾讯云 X 账号](https://x.com/tencentcloud/status/2059870178264109429?lang=en))。手机端走"助理（远程任务）"：在微信、企业微信或 QQ 里发任务，电脑上的 WorkBuddy 执行，结果回到同一个聊天。任务限定在专用的"助理"文件夹里，电脑必须开着 ([腾讯云文档：远程任务](https://cloud.tencent.com/document/product/1831/134392))。

采用量领先，但口径在变。QuestMobile 统计 9 月全端 MAU **3,235 万**，办公 Agent 第一，季度环比增长 647%；整个办公类是 1.42 亿 ([199IT](https://www.199it.com/archives/1858394.html))。7 月只统计 PC 端时是 658 万 ([财联社](https://www.cls.cn/detail/2472437))。跳升有一部分来自 7 月手机 app 上线后统计范围扩大。汤道生称 7 个月处理了 90 亿次任务请求，企业版覆盖"中国 500 强的 50%" ([新浪科技](https://finance.sina.com.cn/tech/roll/2026-10-29/doc-iniwwpnx8872086.shtml))，后者未经独立验证。

安全成了主张的一部分。2026 年 2–3 月，工信部 NVDB 两次发布 OpenClaw 风险提示；CNCERT 发布风险通报，称国家漏洞库自 1 月起已收录 82 个 OpenClaw 漏洞；随后又发布"六要六不要" ([新浪财经](http://finance.sina.com.cn/stock/t/2026-02-05/doc-inhkuima2059527.shtml); [观察者网](https://www.guancha.cn/industry-science/2026_03_10_809530.shtml); [央视网](https://news.cctv.com/2026/03/11/ARTIU9NPnXcPCDiU9cOfqTlD260311.shtml))。9 月，智谱 ZCode 被曝静默上传工作区快照 ([虎嗅](https://www.huxiu.com/article/4892970.html))。WorkBuddy 的回应是：沙箱、授权文件夹、改文件前先备份、删除进回收站、批量操作超过阈值就暂停确认，并在 10-30 发布企业版安全白皮书 ([腾讯新闻](https://news.qq.com/rain/a/20261030A09K1300))。

### 5.2 价值创造与传递：渠道、身份、支付都是自己的

三个主案例里，只有它拥有自家的工作渠道和组织身份体系。微信月活 14.39 亿 ([腾讯 Q2 业绩](https://www.tencent.com/wp-content/uploads/2026/08/%E8%85%BE%E8%AE%AF%E5%85%AC%E5%B8%83%E4%BA%8C%E9%9B%B6%E4%BA%8C%E5%85%AD%E5%B9%B4%E7%AC%AC%E4%BA%8C%E5%AD%A3%E4%B8%9A%E7%BB%A9.pdf))。10-14 起，WorkBuddy 作为可对话的 bot 出现在企业微信通讯录里。它共用企业微信的账号体系，员工离职后账号自动失效；经企业授权，可调用消息、文档、会议、微盘等 10 项能力；管理员集中分配席位、用量和算力 ([21世纪经济报道](https://m.21jingji.com/article/20261014/herald/be87f8aee8a8597b889f1f75a957a777.html))。两者仍是独立产品、独立团队，打通发生在上下文和消息流层面 ([钛媒体](https://www.tmtpost.com/8170778.html))。

支付也在自己手里。06-17 微信支付推出"AI 专属卡"：一个独立的子钱包，每笔支付都要在手机上确认，AI 拿不到密码。WorkBuddy 是第一个接入的 Agent ([量子位](https://www.qbitai.com/2026/06/436160.html))。

模型是开放的。用户可在混元/Hy、DeepSeek、GLM、Kimi、MiniMax 之间切换 ([TechNode](https://technode.com/2026/03/09/tencent-launches-openclaw-like-workplace-ai-agent-workbuddy/))。汤道生说"多模型战略是我们会持续坚持的" ([东方财富](https://finance.eastmoney.com/a/202610313917592794.html))。腾讯自己把领先归因于"强大的 harness（智能体调度）工程能力，丰富的模型选择，和行业领先的技能库"，没有归因于微信 ([腾讯 Q2 业绩](https://www.tencent.com/wp-content/uploads/2026/08/%E8%85%BE%E8%AE%AF%E5%85%AC%E5%B8%83%E4%BA%8C%E9%9B%B6%E4%BA%8C%E5%85%AD%E5%B9%B4%E7%AC%AC%E4%BA%8C%E5%AD%A3%E4%B8%9A%E7%BB%A9.pdf))。

自有渠道是必要条件，不是充分条件。同样挂在微信上的元宝，Q1 MAU 5,735 万，排第四；豆包是 3.45 亿 ([澎湃](https://m.thepaper.cn/newsDetail_forward_33176080))。微信在 02-04 还封了元宝自己的红包分享链接，理由是诱导分享 ([光明网](https://tech.gmw.cn/2026-02/06/content_38582125.htm))。渠道规则对自家产品同样生效。

开放也有代价。汤道生承认"Skills 本身是可以带走的" ([新浪财经](https://finance.sina.com.cn/jjxw/2026-10-30/doc-iniwyfwa2422074.shtml))。真正的锁定来自账号、Buddy One 跨应用共享的积分池，以及企业微信的身份和组织数据。

### 5.3 价值获取：四条收入线，一层补贴

个人版（2026-07-01 起）([WorkBuddy 定价](https://www.workbuddy.cn/docs/workbuddy/Pricing))：

| | 体验版 | 标准版 | 高级版 | 旗舰版 |
|---|---|---|---|---|
| 月费 | 免费 | ¥99 | ¥199 | ¥999 |
| 每月基础积分 | 500 | 2,000 | 4,000 | 20,000 |
| 含限时赠送 | 500 | 4,000 | 9,000 | 50,000 |

加购 1,000 积分 ¥50，只对付费会员开放。企业版（05-15 起）：企业旗舰版（SaaS）¥198/席/月，1 席起；企业专享版 ¥316/席/月，100 席起；私有化另议。每席每月含 2,000 积分，团队共享，问答不限 ([腾讯云文档：企业版计费](https://cloud.tencent.com/document/product/1831/134333))。

折算单价（计算）：加购积分约 ¥0.05/个，企业积分包 ¥0.046–0.05/个。企业 SaaS 席位折合每个所含积分 ¥0.099，也就是说约一半席位费买的是席位功能、治理和身份，不是 token。这与汤道生的说法一致：企业收入主要来自安全、可观测和管理 ([东方财富](https://finance.eastmoney.com/a/202610313917592794.html))。

价格在涨。07-01 入门价从 ¥58 提到 ¥99，加购从每千积分 ¥38 提到 ¥50。腾讯给的背景是：上线三个月内，每用户 token 消耗涨了 10 倍以上 ([网易/华尔街见闻](https://www.163.com/dy/article/KVKVC3UU05198NMR.html))。用户最常见的抱怨是"额度消耗太快、莫名其妙欠费、免费额度不够重度使用" ([腾讯云开发者社区](https://developer.cloud.tencent.com/article/2702350))。

管理层在 Q2 电话会上的说法 ([腾讯 Q2 电话会实录](https://www.investing.com/news/transcripts/earnings-call-transcript-tencent-posts-solid-q2-2026-growth-as-ai-spending-surges-93CH-4855197))：

- 用户在 WorkBuddy 上的支出大部分是订阅。
- 一部分免费用户在被补贴，用来换份额。
- 付费用户的毛利"已经与腾讯云整体毛利相当"。
- "给 WorkBuddy 生产 token"是最持久的经济价值。
- 兜底方案：算力可以经腾讯云出租，价格比买入价高 30% 以上。

WorkBuddy 的现金收入记入腾讯云，确认有滞后 ([36Kr](https://eu.36kr.com/en/p/3937200774167942))。汤道生说没给团队定收入目标，但"我们的资本也是有限的" ([东方财富](https://finance.eastmoney.com/a/202610313917592794.html))。代价看得见：Hy、元宝、CodeBuddy、WorkBuddy、小微这些新 AI 产品，让腾讯 Q2 非 IFRS 经营利润少了约 ¥105 亿（实际为 ¥756 亿；剔除新 AI 产品后为 ¥861 亿）。Q2 资本开支 ¥528 亿，同比增长 176% ([腾讯 Q2 业绩](https://www.tencent.com/wp-content/uploads/2026/08/%E8%85%BE%E8%AE%AF%E5%85%AC%E5%B8%83%E4%BA%8C%E9%9B%B6%E4%BA%8C%E5%85%AD%E5%B9%B4%E7%AC%AC%E4%BA%8C%E5%AD%A3%E4%B8%9A%E7%BB%A9.pdf))。WorkBuddy 的分摊没有披露。

中国市场让这一步更难。腾讯研究院的数据显示，AI 用户付费率按 DAU 算是 18.5%，按 MAU 算只有 0.9%。豆包传出要推付费版时，一项微博投票里 96.7% 的人反对 ([虎嗅](https://m.huxiu.com/article/4897380.html))。2026 年 token 价格反转上涨，腾讯云 HY 2.0 的输入价涨了 463% ([科学网](https://news.sciencenet.cn/htmlnews/2026/4/562401.shtm))。对手用低价抢人：千问办公据报比 WorkBuddy 便宜 ¥5–15 ([腾讯新闻](https://news.qq.com/rain/a/20260731A0AV4H00))。

**判断：** ① 成立。② 三案中最强。③ 付费层成立；整体处在补贴期，由腾讯云的 token 和算力业务兜底。

## 6. 对照 @grok：渠道和模型都自有，成本落在治理

@grok 是 X 的平台官方账号。用户在回复里 @ 它，它在 thread 里公开作答。2026-01-09 起，通过 @grok 生成和编辑图片只对付费用户开放。03-11 起，召唤 @grok 只限 X Premium 和 Premium+，但所有人都能看到回复 ([ADN/AP](https://www.adn.com/nation-world/2026/01/09/musks-grok-limits-image-generator-after-backlash-over-sexualized-ai-pictures/); [Inshorts](https://inshorts.com/en/news/x-restricts--ask-grok--reply-feature-to-premium--premium--users-1773244141032))。

| 要素 | 情况 | 成立吗 |
|---|---|---|
| ① 价值主张 | 在对话现场即时解释、核查、娱乐；召唤者付费，读者免费。对 X 是互动内容和订阅转化，对模型业务是分发和实时数据 | 部分：准确率是天花板 |
| ② 创造与传递 | 渠道、模型、算力全自有；招股书称，与 X 深度整合、独家获取每天约 3.5 亿条帖子，是 Grok 的"关键竞争差异"；分发零成本 | 算力便宜，治理代价高 |
| ③ 价值获取 | 没有 @grok 的收入线；不在 Grok 上卖广告；靠订阅门槛和数据 | 作为漏斗成立，单独不成立 |

**① 的天花板是准确率。** 一项研究覆盖 2025 年 2–9 月发给 Grok 和 Perplexity 两个 bot 的约 167 万条英文事实核查请求，发现 Grok 与专业核查员的一致率是 54.5%，而人类核查员之间是 64% ([Human Cooperation Lab](https://groups.google.com/g/human-cooperation-lab/c/6ADjTadDT44))。另一项研究显示，76.8% 的用户只召唤过一次，回复通常只被少数人看到 ([arXiv](https://arxiv.org/abs/2605.19720))。Maldita 在 2026-10-27 的审计中发现，33–40% 的核查结论错误或部分错误 ([Maldita.es](https://maldita.es/investigaciones/20261027/grok-artificial-intelligence-chatbts-world-cup/))。

**② 的算力很便宜，代价在治理。** 按 grok-4.20 的 API 标价（每百万 token 输入 $1.25、输出 $2.50）粗估，一条回复约 $0.006–0.01，假设约 4,000 个输入 token、250–2,000 个输出 token ([xAI 文档](https://docs.x.ai/docs/models))。每天 100 万条，一年也只要 200–400 万美元（计算）。真正的成本是：每个模型错误都是平台自己的公开发言。2025 年 5 月有 "white genocide" 事件，7 月有 "MechaHitler" 事件，2025 年底至 2026 年初的 11 天里生成了约 300 万张性化图片 ([NBC News](https://www.nbcnews.com/news/amp/rcna207222); [The Register](https://www.theregister.com/2025/07/09/grok_nazi/); [The Star/Reuters](https://www.thestar.com.my/tech/tech-news/2026/01/23/musk039s-grok-created-three-million-sexualised-images-research-says))。欧盟委员会 2026-01-26 依 DSA 启动正式程序 ([欧盟委员会](https://digital-strategy.ec.europa.eu/en/news/commission-investigates-grok-and-xs-recommender-systems-under-digital-services-act))。英国 Ofcom 的调查 01-12 开始，10-14 仍未结案 ([TimesLIVE/Reuters](https://www.timeslive.co.za/news/sci-tech/2026-10-14-concerns-persist-over-xs-handling-of-illegal-content-uk-media-regulator/))。美国的诉讼和弗吉尼亚州总检察长 10-19 的违规通知见 10-Q ([SpaceX 10-Q](https://www.sec.gov/Archives/edgar/data/1181412/000162828026066880/spcx-20260930.htm))。

**③ 从来不在 bot 本身。** 招股书写明，目前不向广告主提供在 Grok 上投放广告的能力 ([SpaceX 招股书](https://www.sec.gov/Archives/edgar/data/1181412/000162828026042639/spaceexplorationtechnologi.htm))。付费订阅者从 2025 年底的约 490 万增至 2026-03-31 的约 630 万 ([SpaceX 招股书](https://www.sec.gov/Archives/edgar/data/1181412/000162828026042639/spaceexplorationtechnologi.htm))。X 广告收入持平略降：2026 年前三季度 12.48 亿美元，上年同期 13.23 亿美元。SI 分部 Q3 收入 75.6 亿美元，其中广告 5.38 亿美元；增长主要来自云服务（算力出租）、Cursor，以及 Grok 和 X 的订阅 ([SpaceX Q3 财报](https://www.sec.gov/Archives/edgar/data/1181412/000162828026066482/spaceexplorationtechnologi.htm); [SpaceX 10-Q](https://www.sec.gov/Archives/edgar/data/1181412/000162828026066880/spcx-20260930.htm))。

渠道主人还给别人定规矩。X 自 2025-06-05 起禁止第三方用 X 内容训练基础模型 ([TechCrunch](https://techcrunch.com/2025/06/05/x-changes-its-terms-to-bar-training-of-ai-models-using-its-content))。2026-02-23 起，第三方 bot 只有在被 @ 或被引用时才能用 API 回复，企业级和公益类应用除外 ([PiunikaWeb](https://piunikaweb.com/2026/02/24/x-api-blocks-automated-spam-replies/))。按量计费的 API 每条"被召唤"的回复收 $0.010 ([X API 文档](https://docs.x.com/x-api/getting-started/pricing))。@grok 是主人；@AskPerplexity 这类第三方 bot 是客人，要按 X 的规则和价目接入。

**对 Agent 的启示：** 自有渠道把成本从"租金"换成了"责任"。@grok 只是回答问题，已经付出这么大的治理代价。会花钱、会登录、会改文件的 Grok Bot，责任面更大。

## 7. 横向比较：分水岭在计价设计

### 7.1 九个条件打分

依据见第 3–6 节。

| 条件 | Claude Tag | Grok Bot | WorkBuddy | @grok（对照） |
|---|---|---|---|---|
| C1 能验收 | 是：PR、工单、图表 | 是：订单、CRM 更新、邮件草稿 | 是：文档、PPT、数据分析 | 否：给答案，与专业核查一致率 54.5% |
| C2 有人买单 | 是：组织预付或月结 | 部分：以个人订阅为主 | 部分：个人订阅 + 企业席位 | 部分：召唤者付 Premium，读者免费 |
| C3 可托付 | 部分：审计、上限齐全；公共频道搜索关不掉，不在 BAA 内 | 部分：企业版可强制 Auto-review；条款免责，CEO 却承诺赔付 | 是：沙箱、授权文件夹、回收站、白皮书 | 否：多次公开事故 |
| C4 上下文与身份 | 是：租用 Slack，组织级 Agent 身份 | 部分：连接器 + computer use，没有自己的身份 | 是：自有微信、企业微信身份 | 是：自有 X 数据流 |
| C5 渠道靠得住 | 部分：Slackbot 竞争，但双方互相依赖 | 部分：X 自有，Slack 租用 | 是：自有，但同受反诱导规则约束 | 是 |
| C6 单任务成本可控 | 是：自有模型，自己定价 | 否：部分向对手买模型，对用户单价不变 | 部分：自有云 + 多模型；2026 年 token 涨价 | 是：每条回复约 1 美分以内（估算） |
| C7 计价跟着成本走 | 是：按 API 标价计量 | 否：平价额度池，越用单价越低 | 部分：积分计量，但订阅附赠、免费层补贴 | 不适用 |
| C8 看得见、封得住 | 是：组织和频道上限；Enterprise 可按人归因 | 否：没有专属上限，共享 Bot 扣提问者额度 | 部分：企业由管理员分配；个人用户抱怨欠费 | 不适用 |
| C9 毛利守得住 | 未知：没有披露 | 否：成本"相对收入高得不可持续" | 部分：付费用户毛利约等于腾讯云整体 | 否：没有直接收入 |
| **总体** | **机制都成立，规模未证** | **① 成立，③ 不成立** | **①② 成立，③ 处在补贴期** | **作为订阅漏斗成立，单独不成立** |

### 7.2 计价单位对不对得上成本

| | Claude Tag | Grok Bot | WorkBuddy |
|---|---|---|---|
| 主要成本 | 每个 thread 的云沙箱 + 模型 token；主动、定时任务 | 每个账号一台持久云电脑 + 路由模型的 token；routines 24/7 运行 | 本机执行消耗的模型 token（多模型）；云端模式和 WMA |
| 计价单位 | 用量，按 API 标价 | 订阅档位，每周额度池 | 订阅（含积分）+ 积分加购 + 企业席位 |
| 团队场景谁付钱 | 组织余额 | 提问者的个人额度 | 企业席位积分池，管理员分配 |
| 免费部分 | 监听频道、判断要不要回 | 7 天试用额度 | 体验版每月 500 积分；企业版问答不限 |
| 超额怎么办 | Team 用完即停；Enterprise 月结 | 预付 credits，可自动充值 | 加购积分包 |
| 透明度 | 没有单任务成本指引 | 只公布倍数，不公布绝对额度 | 各模型的积分倍率只在 app 内显示 |
| 成本风险在谁 | 客户 | SpaceX，直到限流 | 腾讯（免费层）和用户（积分） |

Claude Tag 和 Grok Bot 的需求侧很像：都叫"AI 同事"，都能在 Slack 里被 @，都主打 always-on。结局不同，原因在这张表的第二、三行。Claude Tag 让组织按量付钱；Grok Bot 让个人用平价额度池去扛 always-on 成本。

### 7.3 渠道归属与竞合

| | Claude Tag | Grok Bot | WorkBuddy | @grok |
|---|---|---|---|---|
| 渠道原型 | 模型厂商进别人的渠道 | 自家 app 为主，兼用自有的 X 和租来的 Slack | 生态巨头在自家 IM 和办公套件里的 Agent | 渠道方自有模型 |
| 渠道主人 | Salesforce（Slack） | SpaceX（X）；Salesforce（Slack） | 腾讯 | SpaceX（X） |
| 渠道主人的同类产品 | Slackbot：免费捆绑，但跑在 Claude 上 | Slack 侧同左 | 自家 | 自家 |
| 身份、记忆、账单在谁那 | Anthropic | Cursor（SpaceX） | 腾讯 | X |

竞争发生在 Agent 层，供货发生在模型和算力层：

| 关系 | 内容 | 来源 |
|---|---|---|
| Anthropic → SpaceX | 租用 Colossus 算力（约 32.5 万块 NVIDIA GPU），每月付 12.5 亿美元，到 2029 年 5 月 | [SpaceX 招股书](https://www.sec.gov/Archives/edgar/data/1181412/000162828026042639/spaceexplorationtechnologi.htm) |
| SpaceX → Anthropic | Grok Bot 把部分任务路由到 Claude Opus 5.5 | [Grok Bot FAQ](https://docs.x.ai/grok-bot/faq) |
| Salesforce → Anthropic | Slackbot 的新能力全部跑在 Claude 上；Salesforce 持股约 1% | [TNW](https://thenextweb.com/news/slack-slackbot-30-ai-features-agentic); [TNW](https://thenextweb.com/news/salesforce-benioff-300-million-anthropic-tokens-slack-coding) |
| Microsoft → Anthropic | Copilot Cowork 集成了 Claude Cowork 背后的技术 | [Microsoft 365 博客](https://www.microsoft.com/en-us/microsoft-365/blog/2026/03/09/copilot-cowork-a-new-way-of-getting-work-done/) |
| 腾讯 → 国内模型厂商 | WorkBuddy 可选 DeepSeek、GLM、Kimi、MiniMax | [TechNode](https://technode.com/2026/03/09/tencent-launches-openclaw-like-workplace-ai-agent-workbuddy/) |

所以一个 Agent 的价值，可能被另一家公司获取。Anthropic 在 Slack 里和 Grok Bot 的 Team Bots 竞争，同时又是 Grok Bot 路由和 Slackbot 的模型供应商。SpaceX 和 Anthropic 竞争，同时向 Anthropic 收算力租金。

## 8. 回答：三要素在什么条件下成立

### 8.1 价值主张：卖能验收的工作，卖给有工作预算的人

三个主案例都卖"工作"，也都有需求。卖"答案"的 @grok，价值被准确率封顶。行业数据指向同一处：Gartner 预计到 2027 年底，超过 40% 的 agentic AI 项目会被取消，原因是"成本上升、商业价值不清或风险控制不足" ([Gartner](https://www.gartner.com/en/newsroom/press-releases/2025-06-25-gartner-predicts-over-40-percent-of-agentic-ai-projects-will-be-canceled-by-end-of-2027))。产出能验收，价值才说得清。

付钱的人比用的人更关键。个人付费意愿很低：X 上付费订阅 Premium 的用户不到 1% ([Yahoo Finance](https://finance.yahoo.com/markets/stocks/articles/spacex-filing-provides-insight-x-185621188.html))。MiniMax 面向消费者的 AI 应用毛利只有 4.7%，开放平台和企业服务是 69.4% ([虎嗅](https://m.huxiu.com/article/4897380.html))。所以价值主张要指向有工作预算的组织。Claude Tag 干脆不对个人开放；WorkBuddy 正在从个人转向企业微信；Grok Bot 官方说 B2B 增长最快，Musk 却在推消费场景。

会动手的 Agent，"可托付"也是主张的一部分。中国的监管预警把安全变成了 WorkBuddy 的卖点。Grok Bot 的条款免责与 CEO 的赔付承诺互相矛盾，削弱了这一点。

### 8.2 价值创造与传递：待在上下文所在的地方，让渠道主人离不开你

上下文和身份大多在渠道主人手里。渠道主人用的是同一套工具：收紧数据权利（Slack 禁建索引，X 禁训练）；只开许可接口（Slack 的实时搜索 API 和 MCP 查询即用、不留存数据）；按次收费（X 每条被召唤的回复 $0.010）；限制 Agent 在对话里出现的方式（X 只许被召唤回复）；优待自家 Agent（Slackbot 捆绑）([Slack 博客](https://slack.com/blog/news/mcp-real-time-search-api-now-available); [Hunton](https://www.hunton.com/insights/legal/salesforce-locks-down-slack-data-time-to-review-your-slack-api-terms); [X API 文档](https://docs.x.com/x-api/getting-started/pricing))。WhatsApp 是极端例子。2026-01-15 起它禁止第三方通用 AI 助手，此前已有 5,000 万人在 WhatsApp 上用过 ChatGPT ([Engadget](https://www.engadget.com/ai/chatgpt-in-whatsapp-will-stop-working-in-january-161335173.html))。直到欧盟委员会 06-09 下达临时措施，欧洲经济区才恢复免费接入 ([欧盟委员会](https://ec.europa.eu/commission/presscorner/api/files/document/print/en/ip_26_1276/IP_26_1276_EN.pdf))。

所以，进别人渠道的 Agent 要同时满足三点：走许可的分发渠道；身份、记忆和账单留在自己一侧；渠道主人离不开你。Claude Tag 三点都满足。

反过来，自有渠道也不够。元宝有微信，仍落后豆包（5.2 节：5,735 万对 3.45 亿）。M365 Copilot 嵌在 Office 和 Teams 里，2026 年 1 月付费席位只占商业用户约 3.3% ([Directions on Microsoft](https://www.directionsonmicrosoft.com/blog/microsoft-claims-15-million-paid-m365-copilot-seats))。渠道放大产品，不能替代产品。

单任务成本要掌握在自己手里。自有模型并自己定价（Anthropic）可以做到；自有云加多模型（腾讯）大体可以；向竞争对手买模型、又对用户承诺单价不变（SpaceX），做不到。

### 8.3 价值获取：计价跟着成本走，付钱的人看得见、封得住

行业正往这个方向走。ICONIQ 的调查中，按用量计价的 AI 公司占比从 19% 升到 35%，按结果计价从 2% 升到 18% ([SaaStr](https://www.saastr.com/iconiqs-latest-state-of-ai-report-the-10-most-important-data-points-for-saas-founders))。Cursor 在 2025 年因平价套餐遇上重度用户而出现负毛利 ([Newcomer](https://www.newcomer.co/p/cursors-popularity-has-come-at-a))。GitHub Copilot 从 2026-06-01 起改按 token 计的 AI Credits ([Neowin](https://www.neowin.net/amp/its-official-github-copilot-transitions-to-usage-based-billing-on-june-1/))。据报 OpenAI 将从 12 月起合并 ChatGPT 和 Codex 的额度 ([TNW](https://thenextweb.com/news/grok-bot-costs-unsustainably-high-musk-usage-limits))。

三个案例正好排成一列。Claude Tag 按标价计量、组织付钱、可以封顶。WorkBuddy 按积分计量，但订阅附赠和免费层补贴削弱了计价和成本的联动。Grok Bot 平价、共享、不透明、个人付钱，三个月内撞墙。

毛利的门槛大约是 60%，ICONIQ 对 2026 年的平均预测是 52% ([BVP](https://www.bvp.com/atlas/the-state-of-ai-2025); [SaaStr](https://www.saastr.com/iconiqs-latest-state-of-ai-report-the-10-most-important-data-points-for-saas-founders))。三家都没有披露产品毛利，只有腾讯说付费用户的毛利与腾讯云整体相当。这一条目前无法验证。

### 8.4 三者耦合：自主性既是卖点，也是成本

always-on、主动跟进、定时任务，是 ① 的核心卖点，也是 ③ 的主要成本。三家的处理不同。Claude Tag 对主动和定时工作计费，对"在场"免费。Grok Bot 把 routines 和 @bot 自动任务放进平价池。WorkBuddy 把 24/7 运行的 WMA 托管 Agent 留给企业版 ([新浪科技](https://finance.sina.com.cn/tech/roll/2026-10-29/doc-iniwwpnx8872086.shtml))。

规则是：Agent 越自主，越需要按量计价、由组织付钱、给付钱的人设上限。否则 ① 越成功，③ 越亏。Musk 在 10-20 把"每个用户的用量随时间增长"当作好消息；12 天后，他说服务成本相对收入高得不可持续。

### 8.5 账可以在 Agent 之外结，前提是那一层归你

三家都在 Agent 之外的一层获取价值。SpaceX 的 SI 分部 Q3 扭亏，收入增长主要来自算力出租和 Cursor，Grok Bot 至多占 1–6%。腾讯把 WorkBuddy 的现金记进腾讯云，管理层称 token 生产的价值最持久，算力还能出租兜底。Anthropic 的 Claude Tag 每完成一个任务，就是一笔按标价的 token 销售；它同时给 Slackbot、Copilot Cowork 和 Grok Bot 的路由供货。

但"上层兜底"有前提：Agent 的用量不能挤掉同一资源的更高收益用途。腾讯说 token 在低价下也能有正毛利，所以愿意补贴 WorkBuddy 来换 token 量。Anthropic 按标价卖 token，Claude Tag 用得越多越赚钱。SpaceX 的算力能按接近现货的价格出租，一年内回本，于是平价订阅里的 Grok Bot 用量成了机会成本，最后被限流。拥有算力，不等于 Agent 用算力不花钱。

## 9. 含义：计价向按量收敛，渠道接入开始收费

第一，always-on agent 的计价会收敛到"组织付钱 + 按量计价 + 设上限"。Grok Bot 能用的修复手段都指向这里；GitHub 改用 credits、OpenAI 合并额度是同一趋势。面向个人的 Agent 会变贵：门槛价抬高，超额另收，路由到更便宜的模型。

第二，价值创造与传递会越来越"明码标价"。渠道主人正把 Agent 接入变成一个许可、标准化、计量的层：Slack 的实时搜索 API 和 MCP、X 的按次计费、Meta 正在制定的 Personal Agent Protocol ([Meta Q3 2026 电话会实录](https://s21.q4cdn.com/399680738/files/content_files/META-Q3-2026-Earnings-Call-Transcript.pdf))。进别人渠道的 Agent 能在当下交付价值，但越来越难在渠道里沉淀自己的数据资产。Claude Tag 把频道记忆存在自己一侧，正处在这场拉锯的前沿。

接下来值得看的信号：

| 时间 | 事件 | 看什么 |
|---|---|---|
| 2026-11 中旬 | 腾讯 Q3 财报 | 是否首次披露 WorkBuddy 的收入、付费用户或毛利 |
| 待定 | Grok Bot 额度 | 是否公布绝对额度、调价或新档位 |
| 待定 | Claude Tag 正式版和 Teams 版 | 是否披露采用量；Teams 版是否沿用组织余额计费 |
| 2026-12 | OpenAI 合并 ChatGPT 与 Codex 额度（据报） | 同类产品是否跟进"统一额度池 + 按量超额" |
| 待定 | 欧盟 DSA 和英国 Ofcom 对 X/Grok 的结论 | 渠道方自有模型的治理成本有多高 |

## 10. 局限与注意事项

1. **没有产品级财务数据。** 三家都没有披露这些产品的收入、毛利或留存。价值获取的判断来自价目表、计费规则、管理层表态和事件，不是毛利数据。
2. **指标口径不统一。** Grok Bot 的"付费周活"没有定义，也不在 10-Q 和财报新闻稿里。腾讯先后用过 DAU 排名、月交互量和 PC 访问量，从未披露 WorkBuddy 的用户数。
3. **Grok Bot 限流的性质没有完全确定。** 官方文档没有任何调整说明。Cursor 承认 11-01 有计费和看板故障。Musk 确认这不是故障，但没说今后的额度怎么定。TechCrunch 的报道发表于 2026-11-02，不是 10 月。
4. **厂商证据未经独立验证。** Anthropic 的 65%、Carvana 的 56%、腾讯的"50% 中国 500 强"都是厂商说法。WorkBuddy 的用户抱怨来自社交媒体，不是系统抽样。Benioff 的 3 亿美元 token 支出是播客里的说法。
5. **@grok 的研究多基于 2025 年数据。** 2026-03-11 付费墙之后的使用量不明。每条回复的成本是按 API 标价做的粗估，不是公司数据。
6. **产品变化很快。** Claude Tag 和 Grok Bot 都是 beta，本文状态截至 2026-11-03。

## 来源

- 框架：张敬伟、王迎军（2010）<https://qks.shufe.edu.cn/J/ArticleQuery/A160824002159/CN>；Teece（2010）<https://www.kth.se/social/upload/528c4881f2765458b97a9368/Teece%20%282010%29.pdf>；Johnson、Christensen、Kagermann（2008）<https://hbr.org/2008/12/reinventing-your-business-model>
- 行业数据：Gartner（经 VentureBeat）<https://venturebeat.com/ai/gartner-says-agent-costs-will-rise-fivefold-by-2028-even-as-tokens-get-cheaper-here-is-how-to-keep-them-in-check>；ICONIQ（经 SaaStr）<https://www.saastr.com/iconiqs-latest-state-of-ai-report-the-10-most-important-data-points-for-saas-founders>；Bessemer <https://www.bvp.com/atlas/the-state-of-ai-2025>
- Claude Tag：发布公告 <https://www.anthropic.com/news/introducing-claude-tag>；产品文档 <https://claude.com/docs/claude-tag/overview>、<https://claude.com/docs/claude-tag/admins/set-spend-limit>、<https://claude.com/docs/claude-tag/admins/migrate-from-earlier>、<https://claude.com/docs/claude-tag/concepts/security-and-data>；上线优惠 <https://support.claude.com/en/articles/15575654-claude-tag-launch-promo-for-claude-team-and-enterprise>；Slack API 条款变更 <https://docs.slack.dev/changelog/2025/05/29/rate-limit-changes-for-non-marketplace-apps>；Slackbot 跑在 Claude 上 <https://thenextweb.com/news/slack-slackbot-30-ai-features-agentic>
- Grok Bot：发布公告 <https://x.ai/news/introducing-grok-bot>；产品文档 <https://docs.x.ai/grok-bot/overview>、<https://docs.x.ai/grok-bot/faq>、<https://docs.x.ai/grok-bot/shared-bots>；X Subscription 定价 <https://docs.x.ai/x-subscription/pricing>；Cursor 帮助与条款 <https://cursor.com/help/grok-bot/plans>、<https://cursor.com/terms/grok-bot>；SpaceX Q3 财报 <https://www.sec.gov/Archives/edgar/data/1181412/000162828026066482/spaceexplorationtechnologi.htm>；SpaceX Q3 电话会实录 <https://www.fool.com/earnings/call-transcripts/2026/10/20/spacex-spcx-q3-2026-earnings-call-transcript/>；限流报道（TechCrunch，2026-11-02）<https://finance.yahoo.com/technology/ai/articles/elon-musk-throttles-grok-bot-162321608.html>、TNW <https://thenextweb.com/news/grok-bot-costs-unsustainably-high-musk-usage-limits>
- 腾讯 WorkBuddy：个人定价 <https://www.workbuddy.cn/docs/workbuddy/Pricing>；企业版计费 <https://cloud.tencent.com/document/product/1831/134333>；腾讯 Q2 业绩 <https://www.tencent.com/wp-content/uploads/2026/08/%E8%85%BE%E8%AE%AF%E5%85%AC%E5%B8%83%E4%BA%8C%E9%9B%B6%E4%BA%8C%E5%85%AD%E5%B9%B4%E7%AC%AC%E4%BA%8C%E5%AD%A3%E4%B8%9A%E7%BB%A9.pdf>；腾讯 Q2 电话会实录 <https://www.investing.com/news/transcripts/earnings-call-transcript-tencent-posts-solid-q2-2026-growth-as-ai-spending-surges-93CH-4855197>；QuestMobile 9 月数据 <https://www.199it.com/archives/1858394.html>；企业微信集成 <https://m.21jingji.com/article/20261014/herald/be87f8aee8a8597b889f1f75a957a777.html>
- @grok 与渠道：SpaceX 招股书 <https://www.sec.gov/Archives/edgar/data/1181412/000162828026042639/spaceexplorationtechnologi.htm>；SpaceX Q3 10-Q <https://www.sec.gov/Archives/edgar/data/1181412/000162828026066880/spcx-20260930.htm>；@grok 付费墙 <https://inshorts.com/en/news/x-restricts--ask-grok--reply-feature-to-premium--premium--users-1773244141032>；准确率研究 <https://groups.google.com/g/human-cooperation-lab/c/6ADjTadDT44>、<https://arxiv.org/abs/2605.19720>；欧盟 WhatsApp 临时措施 <https://ec.europa.eu/commission/presscorner/api/files/document/print/en/ip_26_1276/IP_26_1276_EN.pdf>；X API 定价 <https://docs.x.com/x-api/getting-started/pricing>
