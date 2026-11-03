# 海外 AI 智能体怎么赚钱：Manus、Genspark 和同类产品

> 调研日期：2026-11-03。金额为美元。
> 框架同[国内篇](ai-office-agents-business-model-2026-11.md)：**价值主张**（为谁解决什么问题）、**价值创造与传递**（靠什么做出来、怎么送达）、**价值获取**（怎么赚钱）。
> ARR 多为公司自报，未经审计；第三方估算和第三方整理的价格单独标注。各家积分单价不同，不能直接比较积分数。

## 结论

- **海外的独立智能体公司能直接从用户手里收到钱。** Genspark 2026 年一季度新增 1.5 亿美元 ARR，总额约 2.5 亿美元，估值 26 亿美元。Manus 2025 年 12 月自报 ARR 1 亿美元，第三方估算 2026 年 6 月年化收入约 4.5 亿美元。Perplexity 到 6 月初的年化收入已是年初的 3 倍。国内是另一种结构：个人订阅只是门票，钱主要靠大厂的云和企业席位回收。
- **三家的价值主张几乎一样：交给它一件事，它交付成品**（PPT、表格、网页、研究报告）。差别在价值创造：Genspark 押"上下文"（记忆、邮件、团队协作），Manus 押云端执行系统，Perplexity 押搜索和多模型路由。
- **价值创造的核心资源握在别人手里。** 三家都靠外部模型：Manus 以 Claude 为底座，另与通义千问团队合作；Genspark 编排 70 多个模型；Perplexity 编排 19 个模型，CEO 直接把收入增长归功于 Anthropic 的模型进步。上游同时也是对手：Anthropic 自己做了 Claude Cowork；微软用 Claude Cowork 的技术做了 Copilot Cowork，并称内部测试比 Claude Cowork 便宜 30–40%。
- **价值获取在收敛到"积分 + 用量"。** Manus、Genspark、Perplexity 都按积分收费；微软 Copilot Cowork 在许可证之外按 $0.01/积分收费；OpenAI 的 workspace agents 7 月起改按 credits 收费；谷歌 Gemini 改成按算力计额度；Claude 企业版是席位费加按 API 价计的用量。原因很简单：agentic 任务的成本差异太大，按席位包干算不过账。
- **独立玩家的出路是"向上做工作台，向外借渠道"。** Genspark 和 Manus 都是微软 Agent 365 的合作伙伴，借微软卖进企业。Genspark 还做了邮件、团队聊天、自建工具，甚至录音硬件，让用户把工作上下文留在它那里。这和腾讯用 WorkBuddy 开放平台拉伙伴是一回事，只是角色反过来：国内大厂自己就是渠道，海外独立玩家要去借渠道。

## 1. Manus

2025 年 3 月发布，总部新加坡。2025 年 12 月 Meta 宣布以约 20 亿美元收购，2026 年 4 月 27 日被中国发改委叫停，Meta 6 月完成拆分。10 月 Manus 恢复独立运营，融资 5 亿美元以上，由博裕资本和 IDG 领投，腾讯、红杉中国、真格基金等老股东跟投。

| 要素 | 内容 |
|---|---|
| 价值主张 | 面向个人和小团队的通用智能体：一句话交代任务，它在云端虚拟机里用浏览器、代码解释器和办公工具跑完，交付网页、报告、应用等成品 |
| 价值创造与传递 | **模型**：以 Anthropic 的 Claude 为底座，另与通义千问团队合作，获得多模态模型和云资源。**自研**：上下文工程和执行系统，10 月发布 Manus 2.0 和新执行系统 Cascade。**渠道**：自有网站和 App；微软 Agent 365 的首发合作伙伴。**新产品**：独立 App Cue，给每个个人智能体配邮箱、电话号码和移动钱包 |
| 价值获取 | **积分订阅**（第三方整理，9 月 28 日）：免费版每天 300 积分，只能用 Lite 模型，每月上限 1,500；Pro $20/月 4,000 积分，逐档到 $200/月 40,000 积分，最高 $20,000/月；Team $20/席/月起（2 席起），每席 4,000 积分进团队池。**官方参考**：15 分钟的数据分析约 200 积分（约 $1），80 分钟做一个 Web 应用约 900 积分（约 $4.5） |
| 规模 | 2025 年 12 月自报 ARR 1 亿美元，总收入年化 1.25 亿美元；Sacra 估算 2026 年 6 月年化收入约 4.5 亿美元 |

**三要素的张力**：价值主张和价值获取都建立在"模型够强"上，而模型不是自己的。一旦上游涨价，或者上游自己做同类产品（Claude Cowork 已经在做），毛利和用户都会被挤压。股权和监管的不确定性也让企业客户顾虑数据和服务连续性（Sacra）。

## 2. Genspark

2025 年 4 月发布 Super Agent，总部在美国 Palo Alto，CEO 景鲲（Eric Jing）是前百度高管。

| 要素 | 内容 |
|---|---|
| 价值主张 | "AI 工作台"：在一个产品里做 PPT、表格、文档、网站、财务模型，还能打电话、回邮件。7 月的 AI Workspace 6.0 主打"记住你的工作上下文"。CEO 的说法是，今天用 AI 像在和"只有金鱼记忆的天才"合作 |
| 价值创造与传递 | **模型**：编排 70 多个模型，与 OpenAI、Anthropic、AWS、微软合作。**自研的是上下文层**：SecondBrain 记忆打通邮件、会议记录、通话、文档和 CRM；GenMail 邮件客户端；GenTeam 团队聊天，人和智能体在同一频道协作；AgentBase 无代码自建 CRM 和看板；还卖 $199 的 SecondBrain Note 录音卡片。**渠道**：4 月与微软结成全球战略合作，把智能体嵌进 Microsoft 365 和 Agent 365，Genspark Claw 可以直接在 Office 里干活；6 月聘请首席营收官，加强企业销售 |
| 价值获取 | **积分订阅**（第三方整理，7 月 20 日）：免费版每天约 100 积分；Plus $24.99/月（年付 $19.99/月），10,000 积分起；Pro $249.99/月（年付 $199.99/月），125,000 积分起；Team 和 Enterprise 未公开定价。2026 年底前，付费用户的 AI 对话和生图不扣积分。**参考消耗**：一页研究报告约 5–80 积分，一整套 PPT 约 100 积分以上，代打电话约每秒 1 积分。另有硬件收入 |
| 规模 | 2025 年底 ARR 1 亿美元，2026 年一季度再增 1.5 亿美元，约 2.5 亿美元；7 月企业客户超过 7,000 家；累计融资 6.45 亿美元，6 月估值 26 亿美元（3 月为 16 亿美元） |

**三要素的张力**：Genspark 的判断是，下一步的提升来自上下文和易用性，而不是更强的模型，所以它把价值创造的重心放在记忆、邮件和团队频道上。数据沉淀得越多，用户越难搬走，订阅就越稳。风险在于，它最大的渠道伙伴微软，也在 Microsoft 365 里做同样的事。

## 3. 以此类推：其他海外玩家

### 3.1 Perplexity：从搜索走到智能体

- **价值主张**：从"回答问题"走到"把事办完"。2 月推出 Perplexity Computer，在云端运行，能拆出子智能体完成长任务，先给 $200/月的 Max 用户，3 月开放给 Pro。6 月又推出 Mac 端的 Personal Computer。
- **价值创造与传递**：编排 19 个模型，也用自己改过的开源中国模型降低成本。CEO 把增长归功于 Anthropic 的模型进步，并说决定胜负的指标是"每瓦每用户的 token 价值"。
- **价值获取**：100 积分 = $1。Max 每月含 10,000 积分，超出部分按量付费，默认每月上限 $200，最高可设到 $5,000。官方参考：一份多步研究报告 350–950 积分（$3.5–9.5），一个数据看板 2,400–9,800 积分（$24–98）。2025 年底已停掉广告，转向企业订阅。
- **规模**：年化收入到 6 月初已是年初的 3 倍；The Information 报道 ARR 升至 5 亿美元；估值约 200 亿美元。

### 3.2 平台巨头：智能体是卖席位、卖 token 的新抓手

| 公司 | 产品 | 收费方式 | 国内对应 |
|---|---|---|---|
| 微软 | **Copilot Cowork**：6 月 16 日全面上线，用 Claude Cowork 的技术，上线时跑 Anthropic 模型；微软当时称自研的低价模型 Cowork 1 将在数周内推出 | 先要有 Microsoft 365 Copilot 许可证，用量另按 $0.01/积分收费，也可预付打折；管理员可按租户、组、用户设上限。E7 套件 $99/人/月（含 E5、Copilot、Agent 365 等） | 腾讯：套件加渠道，多模型，自研模型压成本 |
| OpenAI | **ChatGPT workspace agents**；企业智能体平台 **Frontier**（2 月） | Business 标准席位 $25/月（年付 $20）；workspace agents 7 月 6 日起按 token 折算 credits 收费；Frontier 未公开定价 | 模型厂（Kimi、MiniMax、智谱） |
| Anthropic | **Claude Cowork**：4 月起所有付费套餐可用 | 随订阅提供，不单独收费；企业版 $20/席/月，用量按 API 价另计 | 模型厂 |
| 谷歌 | **Gemini Spark**（全天候运行的智能体，美国 Ultra 用户）；**Gemini Enterprise** | 5 月起 Ultra 分 $100、$200 两档（$200 档由 $250 降价），Gemini 改为按算力计额度；Gemini Enterprise 发布时按席位 $21–30/人/月 | 阿里、字节：自有模型，也有套件和云 |

## 4. 国内外对照：同一类产品，不同的钱袋子

| 类型 | 国内 | 海外 | 钱从哪来 |
|---|---|---|---|
| 生态和套件巨头 | 腾讯 WorkBuddy、百度搭子、阿里千问办公、字节豆包工作 | 微软 Copilot Cowork、谷歌 Gemini | 席位、套件升级、云 |
| 模型厂 | Kimi Claw、MaxClaw、AutoClaw | Claude Cowork、OpenAI workspace agents | 模型订阅和 token |
| 独立应用层 | 办公智能体榜单前列全是大厂产品 | Manus、Genspark、Perplexity | 积分订阅（token 差价），靠融资扩张 |
| 开源 | OpenClaw 衍生产品（网易 LobsterAI 等） | OpenClaw | 模型 API 和云托管 |

最明显的差别在独立应用层。中国团队做的独立智能体（Manus、Genspark）都把主战场放在海外。一个合理的解释是：海外用户愿意为 $20–250/月的订阅付费，独立公司靠订阅就能做到数亿美元 ARR；国内个人付费难（腾讯总裁刘炽平的说法是"AI 产品面向个人用户的商业化难度较高"），大厂又能用云和生态补贴，独立公司拼不过补贴。

## 5. 五条规律

1. **积分是成本传导工具。** 用户为任务付费，厂商把模型成本的波动转给用户，同时保留调价空间。Perplexity 和微软都把 1 积分定为 1 美分，也都允许设消费上限，这是在把"按量付费"做成用户能接受的样子。
2. **多模型是独立玩家的标配，也是议价筹码。** Genspark 编排 70 多个模型，Perplexity 19 个，Manus 用 Claude 加通义千问；微软也要用自研的 Cowork 1 压成本。谁能把便宜模型用在简单步骤上，谁的毛利就高。
3. **上游既是供应商又是对手。** Anthropic 把模型卖给 Manus、Perplexity 和微软，自己也做 Claude Cowork；微软既是 Genspark 和 Manus 的渠道，又做 Copilot Cowork。独立玩家的价值创造越依赖上游，价值获取就越被动。
4. **护城河在从模型迁到上下文和入口。** Genspark 靠 SecondBrain、GenMail、GenTeam 把工作数据留在自己这里；Manus 用 Cue 给智能体配邮箱、电话和钱包；WorkBuddy 靠微信和企业微信。模型能力趋同之后，谁掌握用户的工作上下文和入口，谁就能持续收费。
5. **企业端都在走"席位 + 用量"。** 微软是 Copilot 许可证加 Cowork 积分，Anthropic 是每席 $20 加 API 价用量，OpenAI 是席位加 credits，WorkBuddy 是席位含积分、超出再买加量包。

## 6. 局限与注意事项

1. **ARR 口径不一。** Manus、Genspark 的数字是公司自报；Sacra、The Information 是估算或媒体报道，未经审计。
2. **Manus、Genspark 的价格来自第三方整理。** 官方定价页抓取不到，且两家调价频繁。
3. **积分单价不同。** Manus Pro 约 $0.005/积分，Genspark Plus 约 $0.0025/积分，Perplexity 和微软都是 $0.01/积分。跨产品比较要看单个任务花多少钱，而不是看积分数。
4. **微软"比 Claude Cowork 便宜 30–40%"是微软自己的内部测试**（125 次运行、12 个提示词、两边都用同一个 Opus 模型）。
5. **OpenAI 的价格来自其帮助中心页面**，具体费率随模型更新而变。

## 来源

- **Manus**：ARR 公告 <https://manus.im/blog/manus-100m-arr>；中国叫停 Meta 收购（CNBC，4 月 27 日）<https://www.cnbc.com/2026/04/27/meta-manus-china-blocks-acquisition-ai-startup.html>；恢复独立与新一轮融资（CNBC，10 月 8 日）<https://www.cnbc.com/2026/10/08/manus-fund-raise-meta-muse-tencent.html>；收入估算与业务模式（Sacra）<https://sacra.com/c/manus/>；价格（第三方，9 月 28 日）<https://www.usecarly.com/blog/manus-pricing/>
- **Genspark**：Series B 扩展与 ARR（路透社，Yahoo Finance 转载，6 月 17 日）<https://finance.yahoo.com/technology/ai/articles/genspark-ai-valued-2-6-132914723.html>；AI Workspace 6.0（Business Wire，Yahoo Finance 转载，7 月 21 日）<https://finance.yahoo.com/technology/ai/articles/genspark-unveils-ai-workspace-6-150700549.html>；与微软的战略合作（Business Wire，4 月 29 日）<https://www.businesswire.com/news/home/20260429907387/en/Genspark-Announces-Global-Strategic-Partnership-with-Microsoft-to-Embed-AI-Agents-Across-Microsoft-365-and-Agent-365>；价格（第三方，7 月 20 日）<https://www.eesel.ai/blog/genspark-ai-pricing>
- **Perplexity**：Computer 发布（TechCrunch，2 月 27 日）<https://techcrunch.com/2026/02/27/perplexitys-new-computer-is-another-bet-that-users-need-many-ai-models/>；积分规则 <https://www.perplexity.ai/help-center/en/articles/13838041-how-credits-work-on-perplexity>；CEO 访谈（CNBC，6 月 3 日）<https://www.cnbc.com/2026/06/03/perplexity-ceo-ai-valuations-computer-agentic.html>；ARR 5 亿美元（The Information）<https://www.theinformation.com/briefings/perplexitys-arr-rises-500-million>
- **微软**：E7 发布 <https://blogs.microsoft.com/blog/2026/03/09/introducing-the-first-frontier-suite-built-on-intelligence-trust/>；E7 构成（CNBC，3 月 9 日）<https://www.cnbc.com/2026/03/09/microsoft-office-365-e7-copilot-ai.html>；Copilot Cowork 进入 Frontier（3 月 30 日）<https://www.microsoft.com/en-us/microsoft-365/blog/2026/03/30/copilot-cowork-now-available-in-frontier/>；全面上线与计费（6 月 16 日）<https://www.microsoft.com/en-us/copilot/blog/2026/06/16/copilot-cowork-is-now-generally-available/>
- **OpenAI**：workspace agents 介绍 <https://openai.com/index/introducing-workspace-agents-in-chatgpt/>；Business 版本说明 <https://help.openai.com/en/articles/11391654-chatgpt-business-release-notes>；Business 席位价格 <https://help.openai.com/en/articles/8792828-what-is-chatgpt-business>；credits 费率 <https://help.openai.com/en/articles/11481834-chatgpt-rate-card-business-enterpriseedu>
- **Anthropic**：Claude 定价 <https://claude.com/pricing>；Cowork 向所有付费套餐开放（eWeek，4 月 10 日）<https://www.eweek.com/news/claude-cowork-general-availability-enterprise-controls/>
- **谷歌**：I/O 2026 订阅调整（5 月 19 日）<https://blog.google/products-and-platforms/products/google-one/google-ai-subscriptions/>；Gemini Enterprise 发布与席位价格（TechCrunch，2025 年 10 月 9 日）<https://techcrunch.com/2025/10/09/google-ramps-up-its-ai-in-the-workplace-ambitions-with-gemini-enterprise/>
