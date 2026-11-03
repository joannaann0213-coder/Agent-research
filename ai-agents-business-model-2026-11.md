# AI 智能体怎么赚钱：国内外办公智能体的商业模式

> 调研日期：2026-11-03。人民币写"元"，美元写"$"。
> 本报告整合了此前复核过的国内篇和海外篇，按"框架 → 约束 → 案例 → 共性 → 差异 → 预判"重新组织。
> 口径：ARR 多为公司自报或媒体报道，未经审计；第三方数据单独标注；各家"积分"对应的算力不同，不能跨产品比较；预判是判断，不是事实。
> 前序调研：[订阅套餐 vs API 按量计费](subscription-vs-api-2026-10.md)

## 摘要

1. **成本结构决定收费方式。** 智能体要读长上下文、多轮推理、反复调用工具，token 消耗大，而且难预测。WorkBuddy 上线三个月，人均 token 消耗涨了 10 倍以上；推理单价每年在降，但追不上用量。于是国内外都在从"包月"转向"订阅含额度 + 超额按量 + 消费上限"。
2. **利润落在谁手里，取决于谁握着稀缺资源。** 据此可以把玩家分成五类：生态和套件巨头握入口和云，模型厂握模型，独立应用层握调度能力和产品体验，垂直智能体握"结果"的定义，开源产品什么都不握，钱流向模型和云。
3. **国内是大厂的天下，智能体本身不是主要利润来源。** 腾讯 WorkBuddy 9 月月活 3,235 万，是第二名百度搭子的 3.3 倍。腾讯称付费用户毛利"十分可观"，与腾讯云整体持平；整体仍在亏，亏在免费层、拉新和模型投入，新 AI 产品合计在 Q2 拖累经营盈利 105 亿元。更大的钱在企业席位和腾讯云。
4. **海外的独立应用层能直接向用户收钱，命门在上游。** Genspark ARR 约 2.5 亿美元，Perplexity 8 月年化收入超过 7.5 亿美元，第三方估算 Manus 6 月年化收入约 4.5 亿美元。三家都靠外部模型，都没公布毛利；它们的模型供应商和渠道伙伴（Anthropic、微软）也在做同类产品。
5. **护城河正从模型迁到上下文和入口；结果能验收的场景开始按结果收费。** Genspark 做记忆、邮件和团队频道，WorkBuddy 靠微信和企业微信。客服智能体已经按结果收费：Intercom Fin 每个结果 $0.99，Sierra 自报 ARR 超过 4 亿美元。
6. **未来 12–18 个月**：收费结构继续收敛；国内办公智能体前三仍是大厂，豆包工作是最大变量；海外独立玩家转向企业；智能体安全强制国标会把安全从卖点变成门槛。最早能验证的一条是腾讯 Q3 财报：新 AI 产品的拖累大概率超过 Q2 的 105 亿元（概率约 70%）。

## 1. 分析框架

### 1.1 商业模式三要素

| 要素 | 回答的问题 | 本报告重点看什么 |
|---|---|---|
| 价值主张 | 为谁解决什么问题 | 目标客户、痛点、交付的成品 |
| 价值创造与传递 | 靠什么做出来，怎么送到客户手里 | 模型、调度能力（harness）、算力、上下文数据、渠道和入口 |
| 价值获取 | 怎么赚钱 | 收费方式、价格、成本，以及利润最终落在哪 |

### 1.2 五类玩家：按"利润落在谁手里"划分

| 类型 | 握着的稀缺资源 | 国内代表 | 海外代表 | 钱从哪来 |
|---|---|---|---|---|
| 生态和套件巨头 | 用户入口、办公套件、云 | 腾讯 WorkBuddy、百度搭子、阿里千问办公、字节豆包工作 | 微软 Copilot Cowork、谷歌 Gemini、Meta Muse | 企业席位、套件升级、云；C 端开始试交易 |
| 模型厂 | 模型 | Kimi Claw、MaxClaw、AutoClaw | Claude Cowork、OpenAI workspace agents | 模型订阅和 token |
| 独立应用层 | 调度能力、产品体验、上下文 | 办公智能体榜单前列没有 | Manus、Genspark、Perplexity | 积分订阅（赚 token 差价），靠融资扩张 |
| 垂直智能体 | 对"结果"的定义和验收 | 本报告未覆盖 | Intercom Fin、Sierra、Salesforce Agentforce | 按结果、按对话或按动作收费 |
| 开源 | 无 | OpenClaw 衍生产品（网易 LobsterAI 等） | OpenClaw | 钱流向模型 API 和云托管 |

## 2. 共同约束：为什么大家都在按量收费

- **智能体比聊天贵得多。** [前序调研](subscription-vs-api-2026-10.md)发现，订阅用满能拿到月费约 8–60 倍的 API 等值，负载越"agentic"，补贴越大。办公智能体正是最重的 agentic 负载：长上下文、多轮推理、自动任务、读写文件。
- **用量涨得比单价降得快。** WorkBuddy 上线三个月，人均 token 消耗涨了 10 倍以上。Epoch AI 较新的估算是，达到同等能力的推理价格每年下降约 5–13 倍；它 2025 年 3 月用另一种算法得出的中位数是 50 倍，方法不同，结果差异很大。
- **各家都在收紧：**
  - WorkBuddy：7 月 1 日个人版从一档 58 元改成 99/199/999 元三档，加量包从 38 元涨到 50 元/1,000 积分；9 月又有用户反映模型消耗倍率上调。
  - OpenAI：9 月下调 Pro $200 的额度；workspace agents 7 月 6 日起按 credits 收费。
  - 谷歌：5 月起 Gemini 从按次数限额改为按算力计额度。
  - 微软：Copilot Cowork 在许可证之外，按 $0.01/积分另收用量费。
  - Perplexity：100 积分 = $1，Max 每月含 10,000 积分，超出按量付费，默认每月上限 $200。
- **推论：价值获取首先是一道成本传导题。** 各家的区别在于谁来补贴、补贴多久、从哪里赚回来，这正是五类玩家的分野。

## 3. 案例一：腾讯 WorkBuddy（生态巨头）

WorkBuddy 是腾讯云推出的桌面端 AI 办公智能体，架构和 CodeBuddy 同源，2026 年 3 月 9 日发布，5 月 29 日上线国际版。

### 3.1 价值主张：为谁解决什么问题

| 客户 | 痛点 | WorkBuddy 的承诺 |
|---|---|---|
| 个人（白领、学生、小老板） | 想用 OpenClaw 这类能直接干活的智能体，但不会部署，也怕不安全 | 下载即用；一句话拆解任务，直接交付文档、表格、PPT；只操作授权的本地文件夹；兼容 OpenClaw 技能 |
| 个人（不在电脑前时） | 想随时派活 | 用微信、企业微信、QQ、飞书、钉钉远程指挥电脑上的 WorkBuddy |
| 企业 | 员工各用各的 AI，数据安全和用量管不住 | 席位制，集中管理用量，数据隔离，可私有化部署；原生内嵌企业微信、腾讯会议、腾讯文档 |
| 海外用户 | 同个人版 | 通过 Slack、Telegram、Discord、WeChat 远程执行；用 MCP 连接 GitHub、Jira、Google Drive、Gmail、Notion |

**安全是被官方点过名的痛点。** 2 月起，工信部网络安全威胁和漏洞信息共享平台（NVDB）多次提示，OpenClaw 部分实例在默认或不当配置下存在较高安全风险。3 月 10 日，国家互联网应急中心列出四类风险：提示词注入、误删邮件和生产数据、恶意插件投毒、公开的中高危漏洞。"下载即用、只碰授权文件夹、企业版可私有化"正好对着这些痛点。

### 3.2 价值创造与传递：靠什么做、怎么送达

**关键资源**

- **调度能力**：腾讯 Q2 财报写道，WorkBuddy 的领先"得益于其强大的 harness（智能体调度）工程能力，丰富的模型选择，和行业领先的技能库"。
- **多模型**：发布时可选混元、DeepSeek、GLM、Kimi、MiniMax。不押注单一模型，既能挑效果好的，也能用便宜的压成本。刘炽平在 Q2 业绩会上说，混元若能高效解决大量需求，会成为核心底层模型，但不会是唯一可用的模型。
- **算力**：Q2 资本开支 528 亿元，同比增加 176%。财报写明，大额 AI 预付款用于支持 Hy 模型升级、WorkBuddy 和 CodeBuddy 的推理需求等。腾讯首席战略官称，在各类算力变现渠道里，token 计费是最长期、最稳定的一种。
- **生态**：腾讯网盘承接文件存储（7 月起所有用户 50G），腾讯文档、乐享、IMA 将陆续接入。

**渠道**

- 自有客户端（Windows、macOS）和网页版，加上 IM 远程入口。
- **腾讯云渠道**：5 月 15 日开放伙伴售卖资格，约半年接入约 2,000 家伙伴。腾讯云副总裁杨晨说，这类伙伴和卖服务器、卖 token 的传统云伙伴不同，"更重视客户前端场景，并以结果付费"。
- **开放平台**：9 月 2 日上线，首批 100 多家生态伙伴、9 款联名智能硬件、30 多个行业应用。另外，SAP、金蝶等传统软件厂商已加入腾讯云的全球生态合作。
- **投放**：北京、深圳的户外广告和主流 App 推广位。
- **合并同门产品**：7 月 20 日，QClaw 业务和部分团队并入 WorkBuddy 所在的云产品六部。9 月 24 日宣布 QClaw 停运，当天停止新用户注册、订阅和续费，12 月 24 日正式停止运营，数据导出工具开放到 2027 年 3 月 24 日。老用户迁移到 WorkBuddy 送 1,000 积分，从未在 WorkBuddy 付过费的用户首购会员最高再送 5,000 积分。

### 3.3 价值获取：怎么赚钱

**国内个人版（2026-07-01 起）**

| 档位 | 单月购买 | 连续包月（7 折） | 连续包年 | 每月积分（基础 + 限时加赠） |
|---|---|---|---|---|
| 体验版 | 免费 | — | — | 500 |
| 标准版 | 99 元 | 70 元 | 672 元 | 2,000 + 2,000 |
| 高级版 | 199 元 | 140 元 | 1,344 元 | 4,000 + 5,000 |
| 旗舰版 | 999 元 | 700 元 | 6,720 元 | 20,000 + 30,000 |

- 积分的标价约 0.05 元/个：加量包 50 元/1,000 积分；单月购买标准版、只算基础积分（99 元买 2,000 个），也是这个价。
- 连续包月后，三档的基础积分单价都是 0.035 元。算上限时加赠，标准版、高级版、旗舰版分别降到 0.0175、0.0156、0.014 元。
- 限时加赠占实得积分的 50%–60%。一旦取消，标准版的实际积分单价涨到 2 倍，高级版 2.25 倍，旗舰版 2.5 倍。
- 加量包单价是套餐内积分（连续包月、含加赠）的 2.9–3.6 倍，重度用户会被推着升档。
- 国际版：Pro $10/月（年付 $96），每月 1,000 + 1,000 积分；Team $40/席/月，每席每月 1,000 积分进团队池。Pro 加量包 $15/500 积分，单价是套餐内积分（含加赠）的 6 倍。

**企业版（腾讯云）**

| 版本 | 起售 | 价格 | 积分 | 部署 |
|---|---|---|---|---|
| 旗舰版 | 1 席 | 198 元/人/月 | 2,000/席/月 | 多租户共享，逻辑隔离 |
| 专享版 | 100 席 | 316 元/人/月 | 2,000/席/月 | 专属资源池和 VPC |
| 私有化版 | 300 席 | 按需报价 | — | 企业内网，自备算力和模型 |

**付费与留存。** 腾讯没有公布付费用户数和收入金额。已知的是：用户支出以订阅费为主，现金收入高速增长，年内逐步计入腾讯云收入；付费用户毛利率与腾讯云整体持平，整体毛利率偏低是因为补贴了免费用户；付费用户留存率超过 80%，活跃用户留存率超过 60%（腾讯 Q1 财报材料）。

**收入分层。** 腾讯云副总裁张果以联名硬件为例拆了三层：先收硬件装机费（一次性），然后是持续的 token 消耗（"类似车辆消耗能源"），再往后是应用和专业 Skill。另一位副总裁吴奇胜说，卖 WorkBuddy"可能只收几万块钱"，客户之后在算力、服务器、带宽上的支出才是主要价值。制造业是早期增长最快的行业：很多客户原本只用 OA、MES、ERP，不上公有云，因为 AI 需求接触腾讯后，逐步用上了腾讯云。WorkBuddy 在腾讯云内部被当作"钩子产品"。

**交易层（10 月起步）。** 10 月 30 日，开放平台上线"生态资产商业化"：个人和企业开发者可以发布付费技能和专家，一次性付费或按月订阅，价格自定（后台限定 1–9,999 元），在微信签约、提现。规划中还有连接器和 Buddy 应用的商业化，以及按次、按量、席位计费。平台抽成没有官方口径。生态侧的数据：200 多家硬件开发者入驻，硬件用户超过 100 万；通达信接入 65 项专业能力，累计调用超过 3,000 万次。

**成本与投入**

- 新 AI 产品（Hy、元宝、CodeBuddy、WorkBuddy、小微）合计拖累非 IFRS 经营盈利：Q1 88 亿元，Q2 105 亿元。这包括模型研发，不能全算在 WorkBuddy 头上。WorkBuddy 爆发后，腾讯把预算向它倾斜，同时削减了其他 AI 产品的预算。
- Q2 自由现金流为 −138 亿元，剔除算力采购预付款后为 376 亿元。

**管理层表态**

- 刘炽平（腾讯总裁）：AI 产品面向个人用户的商业化难度较高；行业不能简单沿用"规模与 DAU 最大化"的逻辑，识别高价值场景至少和用户规模一样重要。
- 汤道生（云与智慧产业事业群 CEO）：WorkBuddy 在企业场景有清晰的商业模式，可以类比几年前的腾讯会议，兼具 ToC 和 ToB 属性，要发挥它的 C2B 能力；WorkBuddy 仍处于战略投入期，公司没给团队定商业化目标。
- Q2 财报：用户"通过订阅和充值购买 token 的付费意愿强烈"。

### 3.4 三要素咬合得怎么样

- **价值主张和传递咬合得最好。** "易用、安全、能远程"的承诺，靠腾讯的 IM 入口、企业微信和腾讯文档集成、云渠道兑现。我判断，这是它甩开 QClaw 和其他"龙虾"产品的主要原因。
- **价值获取和价值主张有冲突。** 官方文档没有公布各模型的积分消耗标准，倍率还在涨（7 月调价，9 月用户反映倍率上调）。重度用户算不清一个任务要花多少钱，这和"简单好用"的承诺相矛盾。
- **价值获取的闭环还没验证。** 打法是 C2B：用低价个人版铺开，再卖企业席位，最后把企业带上腾讯云，和当年的腾讯会议一样。付费用户已经能赚钱，难点在于免费层和拉新要烧多久、C 端规模能否转成 B 端席位和云收入。接下来看三个数：企业版席位数、腾讯云 AI 相关收入、新 AI 产品对利润的拖累能否收窄。

## 4. 案例二：Manus、Genspark 和 Perplexity（独立应用层）

### 4.1 Manus

2025 年 3 月发布，总部新加坡。2025 年 12 月 Meta 宣布以约 20 亿美元收购，2026 年 4 月 27 日被中国发改委叫停，Meta 6 月完成拆分。10 月 Manus 恢复独立运营，融资 5 亿美元以上，由博裕资本和 IDG 领投，腾讯、红杉中国、真格基金等老股东跟投。

| 要素 | 内容 |
|---|---|
| 价值主张 | 面向个人和小团队的通用智能体：一句话交代任务，它在云端虚拟机里用浏览器、代码解释器和办公工具跑完，交付网页、报告、应用等成品 |
| 价值创造与传递 | **模型**：据 Sacra，以 Anthropic 的 Claude 为底座，另与通义千问团队合作，获得多模态模型和云资源。**自研**：上下文工程和执行系统，10 月发布 Manus 2.0 和新执行系统 Cascade。**渠道**：自有网站和 App；微软 Agent 365 的首发合作伙伴。**新产品**：独立 App Cue，给每个个人智能体配邮箱、电话号码和移动钱包 |
| 价值获取 | **积分订阅**（第三方整理，9 月 28 日）：免费版每天 300 积分，只能用 Lite 模型，每月上限 1,500；Pro $20/月 4,000 积分，逐档到 $200/月 40,000 积分，最高 $20,000/月；Team $20/席/月起（2 席起），每席 4,000 积分进团队池。**官方参考**：15 分钟的数据分析约 200 积分（约 $1），80 分钟做一个 Web 应用约 900 积分（约 $4.5） |
| 规模 | 2025 年 12 月自报 ARR 1 亿美元，总收入年化 1.25 亿美元；Sacra 估算 2026 年 6 月年化收入约 4.5 亿美元 |

**张力**：价值主张和价值获取都建立在"模型够强"上，而模型不是自己的。一旦上游涨价，或者上游自己做同类产品（Claude Cowork 已经在做），毛利和用户都会被挤压。股权和监管的不确定性也让企业客户顾虑数据和服务连续性（Sacra）。

### 4.2 Genspark

2025 年 4 月发布 Super Agent，总部在美国 Palo Alto，CEO 景鲲（Eric Jing）是前百度高管。

| 要素 | 内容 |
|---|---|
| 价值主张 | "AI 工作台"：在一个产品里做 PPT、表格、文档、网站、财务模型，还能打电话、回邮件。7 月的 AI Workspace 6.0 主打"记住你的工作上下文"。CEO 的说法是，今天用 AI 像在和"只有金鱼记忆的天才"合作 |
| 价值创造与传递 | **模型**：编排 70 多个模型，与 OpenAI、Anthropic、AWS、微软合作。**自研的是上下文层**：SecondBrain 记忆打通邮件、会议记录、通话、文档和 CRM；GenMail 邮件客户端；GenTeam 团队聊天，人和智能体在同一频道协作；AgentBase 无代码自建 CRM 和看板；还卖 $199 的 SecondBrain Note 录音卡片。**渠道**：4 月与微软结成全球战略合作，把智能体嵌进 Microsoft 365 和 Agent 365，Genspark Claw 可以直接在 Office 里干活；6 月聘请首席营收官，加强企业销售 |
| 价值获取 | **积分订阅**（第三方整理，7 月 20 日）：免费版每天约 100 积分；Plus $24.99/月（年付 $19.99/月），10,000 积分起；Pro $249.99/月（年付 $199.99/月），125,000 积分起；Team 和 Enterprise 未公开定价。2026 年底前，付费用户的 AI 对话和生图不扣积分。**参考消耗**：一页研究报告约 5–80 积分，一整套 PPT 约 100 积分以上，代打电话约每秒 1 积分。另有硬件收入 |
| 规模 | 2025 年底 ARR 1 亿美元，2026 年一季度再增 1.5 亿美元，约 2.5 亿美元；7 月企业客户超过 7,000 家；累计融资 6.45 亿美元，6 月估值 26 亿美元（3 月为 16 亿美元） |

**张力**：Genspark 的判断是，下一步的提升来自上下文和易用性，而不是更强的模型，所以它把价值创造的重心放在记忆、邮件和团队频道上。数据沉淀得越多，用户越难搬走，订阅就越稳。风险在于，它最大的渠道伙伴微软，也在 Microsoft 365 里做同样的事。

### 4.3 Perplexity：从搜索走到智能体

- **价值主张**：从"回答问题"走到"把事办完"。2 月推出 Perplexity Computer，在云端运行，能拆出子智能体完成长任务，先给 $200/月的 Max 用户，3 月开放给 Pro。6 月又推出 Mac 端的 Personal Computer。
- **价值创造与传递**：编排 19 个模型，也用自己改过的开源中国模型降低成本。CEO 把增长归功于 Anthropic 的模型进步，并说决定胜负的指标是"每瓦每用户的 token 价值"。
- **价值获取**：100 积分 = $1。Max 每月含 10,000 积分，超出部分按量付费，默认每月上限 $200，最高可设到 $5,000。官方参考：一份多步研究报告 350–950 积分（$3.5–9.5），一个数据看板 2,400–9,800 积分（$24–98）。2025 年底已停掉广告，转向企业订阅。
- **规模**：年化收入年初不到 2.5 亿美元，3 月超过 4.5 亿美元（FT），8 月超过 7.5 亿美元（The Information）。最近一轮有定价的估值是 200 亿美元（2025 年 9 月）；The Information 8 月报道，英伟达在谈 300 亿美元以上估值的投资，截至 11 月 1 日未见完成。

### 4.4 三家的共同点

- **价值主张几乎一样**：交给它一件事，它交付成品。
- **价值创造各押一处**：Genspark 押上下文，Manus 押云端执行系统，Perplexity 押搜索和多模型路由。
- **价值获取都是积分订阅**，都没有公布毛利率。
- **共同风险在上游**：模型主要来自 Anthropic 等外部厂商，而 Anthropic 自己做了 Claude Cowork；微软既是 Genspark 和 Manus 的渠道，也做了 Copilot Cowork，还称内部测试比 Claude Cowork 便宜 30–40%。
- **出路是"向上做工作台，向外借渠道"**：向上，用记忆、邮件、团队协作把用户的工作上下文留住；向外，借微软 Agent 365 这类平台卖进企业。

## 5. 其他玩家速览

### 5.1 国内生态大厂

| 产品（公司） | 9 月月活 | 价格 | 背后赚钱的地方 |
|---|---|---|---|
| WorkBuddy（腾讯） | 3,235 万 | 个人免费 500 积分，99/199/999 元/月；企业 198–316 元/人/月 | 腾讯云、企业席位 |
| 百度搭子 DuMate（百度） | 977 万 | 个人免费版签到领积分；Pro 59 元/月（25,000 积分），Max 129 元/月（100,000 积分）。企业 189 元/席/月，首购 7 折 132 元 | 百度智能云；企业资源点还能用于秒哒、伐谋、胜算 |
| 千问办公 QwenWork（阿里） | 790.5 万 | 8 月 3 日公测。个人标准版 98 元/月、高级版 198 元/月（公测期 8 折为 78、158 元）；企业标准版 198 元/席/月；新用户送 2,000 积分 | 阿里云、钉钉 |
| QClaw（腾讯） | 317.3 万 | 已宣布停运，用户迁往 WorkBuddy | — |
| 豆包工作（字节） | 204.9 万 | 8 月 25 日发布，下载送 30 天订阅，9 月 24 日又给所有人送 30 天。豆包付费版 68/200/500 元/月（6 月推出，含办公和视频），后两档额度为标准档的 4 倍和 10 倍 | 火山引擎、飞书 |

- 月活来自 QuestMobile 2026 年 9 月办公类 Agent 数据，APP、网页、PC 客户端合并计算。字节的 TRAE Work 另有 204.2 万。阿里 Qoderwork、阿里 QwenPaw、智谱 AutoClaw 环比下降，未公布数字。百度搭子季度环比增长 1567.5%，是榜单里增速最快的。
- **千问办公和千问 App 是两个产品。** 千问办公由 QoderWork、MuleRun、悟空整合而来，主打企业，用钉钉登录，账号和千问 App 不互通。千问 App 另有面向个人的"办公助理"，8 月 10 日推出专业会员，19/49/128 元/月。
- **C 端盘子差距很大。** 2026 上半年，豆包 App 月活 3.82 亿，千问 App 1.67 亿（QuestMobile）；腾讯元宝 6 月为 4,984 万（腾讯财报引用 QuestMobile）。字节和阿里能把 C 端用户导向办公智能体，这个潜力远大于腾讯自己的 AI 助手，是 WorkBuddy 最大的外部变量。

### 5.2 海外平台巨头

| 公司 | 产品 | 收费方式 |
|---|---|---|
| 微软 | **Copilot Cowork**：6 月 16 日全面上线，用 Claude Cowork 的技术，上线时跑 Anthropic 模型；微软当时称自研的低价模型 Cowork 1 将在数周内推出 | 先要有 Microsoft 365 Copilot 许可证，用量另按 $0.01/积分收费，也可预付打折；管理员可按租户、组、用户设上限。E7 套件 $99/人/月（含 E5、Copilot、Agent 365 等） |
| 谷歌 | **Gemini Spark**（全天候运行的智能体，美国 Ultra 用户）；**Gemini Enterprise** | 5 月起 Ultra 分 $100、$200 两档（$200 档由 $250 降价）；Gemini Enterprise 发布时按席位 $21–30/人/月 |
| Meta | **Muse**：9 月 8 日在美国上线的个人智能体，能读邮件、订行程、填表、谈价，用用户的卡付款 | 免费版加 $20、$100 两档订阅（Axios）；支付走 Stripe Link；Meta 称产品内不放广告 |

对应关系：微软像腾讯（套件加渠道、多模型、自研模型压成本）；谷歌像阿里和字节（自有模型，也有套件和云）；Meta Muse 对应微信内正在灰度的智能体"小微"。

### 5.3 模型厂

| 产品 | 收费方式 |
|---|---|
| **Claude Cowork**（Anthropic） | 4 月起所有付费套餐可用，随订阅提供（Pro $20/月起），不单独收费；企业版 $20/席/月，用量按 API 价另计 |
| **ChatGPT workspace agents**（OpenAI） | Business 标准席位 $25/月（年付 $20）；7 月 6 日起按 token 折算 credits 收费；企业智能体平台 Frontier（2 月发布）未公开定价 |
| **Kimi Claw、MaxClaw、AutoClaw**（月之暗面、MiniMax、智谱） | Kimi Claw 199 元/月，含 20 倍 Kimi Code 额度；MaxClaw 39 元/月 5,000 积分；AutoClaw 29 元/月 5,000 积分（均为 3 月口径） |

模型厂的智能体是卖自家 token 的新包装。好处是成本自己说了算，风险是只能用自家模型，模型一落后就留不住人。

### 5.4 垂直智能体：结果能验收，就按结果收费

| 产品 | 收费方式 | 说明 |
|---|---|---|
| Intercom Fin（客服） | 每个结果 $0.99；接其他客服系统时每月最少 50 个结果 | "解决"的定义是 Fin 最后一次回答后用户不再求助；接 Intercom 自家客服系统另收 $19/席/月 |
| Salesforce Agentforce | 每次对话 $2；或 Flex Credits，$500/10 万积分，每个动作 20 积分（约 $0.10）；或按席位，附加包 $125/人/月起 | 价目表还列了"Help Agent Resolutions" $2；Flex Credits 和按对话计费不能在同一组织混用 |
| Sierra（客服） | 按结果收费（官方说法："为结果付费，不为 token 付费"），未公开价目 | 自报 ARR 4 亿美元以上，三季度新增 1.5 亿美元；5 月融资 9.5 亿美元，估值 158 亿美元 |

### 5.5 开源

- **OpenClaw** 免费，用户自付模型 API 和服务器。网易 LobsterAI、阿里 CoPaw 这类"工具免费、自配 API"的产品也属这一类（3 月口径）。
- **钱被模型厂和云厂商拿走。** 腾讯云 Lighthouse 1 月 28 日上线 OpenClaw 一键部署模板，3 月初"云上养虾人"超过 10 万。也就是说，OpenClaw 火起来时，腾讯云先卖了服务器，之后再用 WorkBuddy 把不想自己部署的用户收进来。
- **最大的短板是安全**（见 3.1）。这恰好是商业产品的切入点。

## 6. 共性：六条规律

1. **积分是成本传导工具。** 第 2 节的成本压力最后都落到积分设计上：Perplexity 和微软都把 1 积分定为 1 美分，也都允许设消费上限；WorkBuddy 的加量包比套餐内积分贵 2.9–3.6 倍，推着重度用户升档；限时加赠给了厂商随时收紧的空间。
2. **多模型路由是降本的主要手段。** Genspark 编排 70 多个模型，Perplexity 19 个，Manus 用 Claude 加通义千问，WorkBuddy 用混元加第三方模型，微软要推自研的 Cowork 1。推理单价的下降不会自动变成毛利，要靠把便宜模型用在简单步骤上。
3. **上游既是供应商又是对手。** Anthropic 把模型卖给 Manus、Perplexity 和微软，自己也做 Claude Cowork；微软既是 Genspark 和 Manus 的渠道，又做 Copilot Cowork。独立玩家的价值创造越依赖上游，价值获取就越被动。
4. **护城河从模型迁到上下文和入口。** Genspark 靠 SecondBrain、GenMail、GenTeam 把工作数据留在自己这里；Manus 用 Cue 给智能体配邮箱、电话和钱包；WorkBuddy 靠微信和企业微信。模型能力趋同之后，谁掌握用户的工作上下文和入口，谁就能持续收费。
5. **企业端都走"席位 + 用量"。** 微软是 Copilot 许可证加 Cowork 积分，Anthropic 是每席 $20 加 API 价用量，OpenAI 是席位加 credits，Salesforce 是席位加 Flex Credits，WorkBuddy 是席位含积分、超出再买加量包。
6. **结果能验收的场景按结果收费。** 客服的"结果"可以定义、可以验收，所以 Intercom、Sierra、Salesforce 能按结果收费；通用办公任务很难界定"做完"和"做好"，所以 Manus、Genspark 们还在按积分收费。谁先把某类办公任务的结果定义清楚，谁就能从卖 token 升级到卖结果。

## 7. 差异：为什么国内没有独立应用层

- **现象**：海外三家独立玩家做到了数亿美元的 ARR 或年化收入；国内办公智能体榜单前列全是大厂产品。华人团队创办的独立智能体（Manus、Genspark）都把主战场放在海外。
- **解释**（我的判断）：
  1. **付费意愿**：海外用户能接受 $20–250/月的订阅；国内个人付费难，腾讯总裁刘炽平的说法是"AI 产品面向个人用户的商业化难度较高"。
  2. **补贴**：国内大厂能用云和生态把个人版补贴到很低，甚至免费送订阅（豆包工作连送两轮 30 天），独立公司拼不过补贴。
  3. **入口**：国内的超级 App（微信、豆包、千问）把"派活"的入口握在大厂手里；海外的入口更分散，独立玩家还能借微软这类平台的渠道。
- **角色正好相反**：国内大厂自己就是渠道，用开放平台拉伙伴（WorkBuddy 的 2,000 家渠道伙伴、技能市场）；海外独立玩家要去借渠道（Genspark、Manus 接入微软 Agent 365）。
- **收费方式在趋同，利润分配不会趋同**：两边都在走积分、用量和"席位 + 用量"，但国内的利润主要落在大厂的云和生态里，海外还有一部分能留在独立应用层。

## 8. 未来 12–18 个月的预判

概率是我的主观判断。每条都写明依据和验证方法，方便到时对照。

### 8.1 近期可验证

1. **腾讯 Q3 财报里，新 AI 产品的拖累不会收窄，大概率超过 Q2 的 105 亿元。** 概率约 70%。
   - 依据：Q2 资本开支 528 亿元，折旧会在之后几个季度体现；WorkBuddy 三季度月活大涨，推理成本跟着涨；8 月起加大投放。抵消因素是 7 月提价、9 月倍率上调，以及削减其他 AI 产品的预算。
   - 验证：Q3 业绩公告里"剔除新 AI 产品"的经营盈利口径。预计 11 月中旬发布，2025 年同期是 11 月 13 日。

### 8.2 收费方式

2. **2027 年 6 月前，WorkBuddy 个人版至少再调整一次价格或积分规则，限时加赠大概率缩水。** 概率约 80%。
   - 依据：上线 8 个月已经调整两次；加赠占实得积分的 50%–60%，取消后实际单价涨到 2–2.5 倍；管理层强调识别高价值场景，不追求 DAU 最大化。
   - 验证：定价页的"限时加赠"是否消失或下调，是否出现更高档位或按任务计价。
3. **到 2027 年底，主流海外办公和通用智能体都会采用"订阅含额度 + 超额按量 + 消费上限"，不限量的个人套餐基本消失。** 概率约 85%。
   - 依据：Perplexity、微软、OpenAI、谷歌、Manus、Genspark、Meta Muse 都已经转向。
   - 验证：各家定价页。
4. **按结果收费会从客服扩散到销售线索、理赔、催收、招聘等结果能验收的垂直场景（概率约 75%）；通用办公智能体 2027 年内仍以积分和用量为主（概率约 80%）。**
   - 依据：Intercom、Salesforce、Sierra 已经在客服上跑通；通用办公任务的"完成"很难界定和验收。
   - 验证：新发布的垂直智能体价目表。

### 8.3 收入结构

5. **WorkBuddy 的收入重心移向企业，并被打包进腾讯的企业套餐。** 2027 年底前发生打包的概率约 50%。
   - 依据：企业版已原生内嵌企业微信、腾讯会议、腾讯文档；汤道生拿腾讯会议类比；微软已经推出 Microsoft 365 E7，把办公套件和智能体打包销售。
   - 验证：企业微信、腾讯文档、腾讯会议的企业版价格表里是否出现 WorkBuddy 权益或积分。
6. **WorkBuddy 技能市场会按规划加上按次、按量、席位计费，但 2027 年对收入的贡献仍远小于订阅和企业席位。** 概率约 80%。
   - 依据：这些计费方式腾讯已经写进规划；开发者定价从 1 元起步，交易层眼下的任务是给技能库供货、留住开发者。
   - 验证：开放平台的计费选项；业绩会是否单独提到交易收入。
7. **Manus、Genspark、Perplexity 的增长主力转向团队和企业。** 概率约 75%，要等公司披露才能验证。
   - 依据：Genspark 企业客户超过 7,000 家，6 月聘请首席营收官；Perplexity 停掉广告后优先做企业订阅；Manus 推 Team 版，并通过微软 Agent 365 卖进企业。上游把智能体塞进 $20 的订阅，C 端的定价空间被压缩。
   - 验证：企业客户数、团队版收入占比的披露。
8. **C 端智能体会试交易变现，但 2027 年内交易相关收入不会超过订阅收入。** 概率约 85%。
   - 依据：Meta Muse 能替用户付款（Stripe Link），Perplexity 在做"答案里直接下单"。交易抽成需要商家网络、支付和用户信任，短期内规模有限。
   - 验证：公司对交易收入的披露。

### 8.4 竞争格局

9. **到 2027 年中，国内办公智能体月活前三仍全是大厂产品（概率约 90%）；豆包工作进入前三的概率约 55%。**
   - 依据：豆包 App 月活 3.82 亿，是元宝的 7 倍多；豆包工作 8 月 25 日才发布，9 月已有 204.9 万月活。字节已连续两轮给所有人送 30 天订阅，10 月底又改成签到送额度。百度搭子季度环比增长 1567.5%，也在抢位置。
   - 验证：QuestMobile、易观的季度办公 Agent 数据。
10. **2027 年底前，国内不会出现订阅 ARR 超过 1 亿美元的独立办公智能体（非大厂、非模型厂）。** 概率约 80%。
    - 依据：见第 7 节的三条原因。
    - 验证：国内独立 Agent 公司的融资和收入披露。
11. **2027 年底前，Manus、Genspark 至少一家推出自研或深度微调的模型，用来承接简单步骤、降低成本。** 概率约 60%。
    - 依据：Perplexity 已在用改过的开源模型降成本，微软要推自研的 Cowork 1；推理价格在降，但用量涨得更快，路由和自有模型是改善毛利的主要手段。
    - 验证：产品发布和技术博客。

### 8.5 监管与资本

12. **智能体安全会从卖点变成门槛。** 强制性国家标准《智能体应用安全基本要求》（计划号 20263116-Q-252）已经立项，由中央网信办归口，面向公众的智能体产品和服务都要满足。2027 年底前发布征求意见稿的概率约 70%；正式实施在 2028 年或更晚的概率约 85%。
    - 依据：标准的定位是为监管、市场准入和技术检测提供依据，重点管信息泄露、权限失控、工具滥用和目标偏离，这些正是能操作本地电脑和第三方账号的智能体的风险。
    - 影响：有安全测试和合规能力的大厂受益，小团队和开源衍生产品的门槛提高。
    - 验证：中央网信办和全国标准信息公共服务平台的公告。
13. **2027 年底前，Manus、Genspark、Perplexity 中至少一家递交上市申请或被收购。** 概率约 35%。
    - 依据：三家都还处在高增长、高估值的私募融资期，资金充足；Manus 的股权和监管问题刚理顺，分析师认为它近期的优先事项是证明规模、盈利和监管合规。
    - 验证：公开申报文件和并购公告。

## 9. 还要盯的变量

这些不给概率，但会改变上面的判断：

- **微信内的智能体"小微"**：8 月起小范围灰度。如果全面上线，C 端"派活"的入口可能回到微信，WorkBuddy 的定位会更偏工作和企业。
- **混元 Hy4 的进度**：自研模型够强、够便宜，WorkBuddy 的毛利会改善；否则要持续向第三方模型付费。
- **WorkBuddy 的收入确认节奏**：腾讯首席战略官说现金收入年内逐步计入腾讯云收入，Q3、Q4 的企业服务收入增速会有所体现。
- **独立玩家的毛利**：Manus、Genspark、Perplexity 任何一家披露毛利，都能直接检验"独立应用层能否长期赚钱"。
- **英伟达对 Perplexity 的投资**：如果按 300 亿美元以上估值完成，会抬高整个独立应用层的估值锚。

## 10. 局限与注意事项

1. **收入和用户数口径不一。** Manus、Genspark、Sierra 的 ARR 是公司自报；Sacra 是估算；Perplexity 的 7.5 亿美元来自 The Information 的报道，本文经第三方转引。国内月活来自 QuestMobile（APP、网页、PC 客户端合并）；腾讯财报引用的是易观的"桌面端 AI 原生办公智能体月访问量"（6 月 WorkBuddy 2,097 万次，超过第二、三名之和）。媒体称千问办公"上线满月用户破 3,000 万"，没说明口径，不能和月活比。
2. **毛利是空白。** 三家独立玩家都没有公布毛利率。腾讯说 WorkBuddy 付费用户毛利与腾讯云整体持平，可以作参照，但口径不同。
3. **利润拖累是合并数**，含 Hy、元宝、CodeBuddy、WorkBuddy、小微，无法单拆 WorkBuddy。
4. **业绩会表态来自媒体转述**（见实科技、钛媒体），不是逐字稿。
5. **部分价格来自第三方。** Manus、Genspark 的官方定价页抓取不到，价格来自第三方整理；千问办公价格来自媒体报道；Kimi Claw、MaxClaw、AutoClaw 是 3 月口径；Meta Muse 的价格来自 Axios 报道；OpenAI 的价格来自其帮助中心页面；垂直智能体的企业合同价通常另行谈判。
6. **积分单价不可比。** WorkBuddy 加量包 0.05 元/积分，百度搭子个人加量包 0.002 元/积分；Manus Pro 约 $0.005/积分，Genspark Plus 约 $0.0025/积分，Perplexity 和微软都是 $0.01/积分。跨产品比较要看单个任务花多少钱，而不是看积分数。
7. **未经确认的说法**："WorkBuddy 9 月倍率上调"来自用户反馈；技能市场抽成 1% 来自知乎作者，腾讯未确认；微软"比 Claude Cowork 便宜 30–40%"是微软自己的内部测试（125 次运行、12 个提示词、两边都用同一个 Opus 模型）。
8. **预判是判断，不是事实。** 概率反映的是现有证据下的把握程度。

## 来源

**腾讯与 WorkBuddy**

- 腾讯 2026 年第二季度业绩公告：<https://www.tencent.com/wp-content/uploads/2026/08/%E8%85%BE%E8%AE%AF%E5%85%AC%E5%B8%83%E4%BA%8C%E9%9B%B6%E4%BA%8C%E5%85%AD%E5%B9%B4%E7%AC%AC%E4%BA%8C%E5%AD%A3%E4%B8%9A%E7%BB%A9.pdf>
- 腾讯 2026 年第一季度业绩（DoNews 转述）：<https://www.donews.com/news/detail/1/6554161.html>
- 腾讯 Q2 业绩会转述（见实科技，网易号，8 月 13 日）：<https://www.163.com/dy/article/L47B7H0U053144S4.html>
- 留存数据与 Q2 投入分析（钛媒体，8 月 24 日）：<https://www.tmtpost.com/8114162.html>
- WorkBuddy 定价：国内 <https://www.workbuddy.cn/docs/workbuddy/Pricing>、国际 <https://www.workbuddy.ai/docs/zh/workbuddy/pricing>；企业版 <https://cloud.tencent.com/product/workbuddy-enterprise>
- 华尔街见闻《调用量上涨 腾讯 Buddy 家族要重新定价了》（6 月 17 日）：<https://wallstreetcn.com/articles/3774896>
- 星岛环球网《月活 3200 万的 WorkBuddy，给腾讯云"钩来了"传统制造业》（10 月 29 日）：<https://m.stnn.cc/detail/6ae2eb2c8f9d2b19dd008df0.html>
- 开放平台"生态资产商业化"（新浪财经，11 月 1 日）：<https://cj.sina.com.cn/articles/view/5099546123/12ff4e60b00102dmbe>；技能收费内测（蓝鲸新闻，腾讯新闻转载，10 月 28 日）：<https://news.qq.com/rain/a/20261028A0C0NT00>；1% 抽成的说法（什么值得买转引知乎）：<https://post.smzdm.com/p/axko4vv9/>
- WorkBuddy 发布（IT之家，3 月 9 日）：<https://www.ithome.com/0/927/230.htm>；IM 接入教程（腾讯云开发者社区，4 月 9 日）：<https://cloud.tencent.com/developer/article/2651884>
- TechNode：国际版上线（5 月 29 日）<https://technode.com/2026/05/29/tencent-launches-workbuddy-productivity-ai-agent-for-global-users/>、最高优先级 AI 应用（8 月 10 日）<https://technode.com/2026/08/10/tencent-reportedly-makes-workbuddy-a-top-strategic-ai-priority>
- QClaw 停运与并入：虎嗅 <https://www.huxiu.com/article/4893724.html>、钛媒体 <https://www.tmtpost.com/8151538.html>
- 9 月三家办公 Agent 动向（什么值得买）：<https://post.smzdm.com/p/a6z08zlz/>

**国内同类产品**

- QuestMobile 9 月办公类 Agent 数据（快科技报道，新浪财经转载，10 月 20 日）：<https://finance.sina.com.cn/stock/t/2026-10-20/doc-inivwitp6542061.shtml>
- QuestMobile 2026 上半年 AI 原生 App 月活（凤凰网科技，8 月 4 日）：<https://tech.ifeng.com/c/8vJKV0B47Gr>
- 百度搭子：个人版 <https://www.dumate.cn/>、企业版 <https://cloud.baidu.com/product/dumate.html>
- 千问办公公测与定价（IT之家，8 月 3 日）：<https://www.ithome.com/0/984/942.htm>；千问办公与千问 App 的区别（36氪，8 月 10 日）：<https://www.36kr.com/p/3933525177653128>
- 千问 App 办公会员、豆包付费版（新浪财经，8 月 12 日）：<https://finance.sina.com.cn/jjxw/2026-08-12/doc-ininaezi3595434.shtml>；豆包专业版发布（科技日报，6 月 24 日）：<https://www.stdaily.com/web/gdxw/2026-06/24/content_536585.html>；豆包工作发布（DoNews，8 月 25 日）：<https://www.donews.com/news/detail/1/6683761.html>；再送 30 天订阅（科技日报，9 月 24 日）：<https://www.stdaily.com/web/gdxw/2026-09/24/content_587357.html>
- 8 款大厂"龙虾"测评（澎湃新闻对齐 Lab，腾讯新闻转载，3 月 12 日）：<https://news.qq.com/rain/a/20260312A03BMY00>

**安全、监管与开源**

- OpenClaw 安全风险提示（国家互联网应急中心，新华网转载，3 月 10 日）：<https://www.news.cn/tech/20260310/d5e1d772bed046239ea3774903c08970/c.html>；NVDB 首次预警（新浪财经，2 月 5 日）：<https://finance.sina.com.cn/jjxw/2026-02-05/doc-inhkuimi2883451.shtml>
- 《智能体应用安全基本要求》强制性国家标准立项（新华网，7 月 28 日）：<https://www.news.cn/tech/20260728/8ebf5083cf0e487287f894fb31e123f5/c.html>
- 腾讯云 OpenClaw 一键部署（扬子晚报，3 月 6 日）：<https://www.yzwb.net/news/ch/202603/t20260306_328547.html>

**海外独立应用层**

- **Manus**：ARR 公告 <https://manus.im/blog/manus-100m-arr>；中国叫停 Meta 收购（CNBC，4 月 27 日）<https://www.cnbc.com/2026/04/27/meta-manus-china-blocks-acquisition-ai-startup.html>；恢复独立与新一轮融资（CNBC，10 月 8 日）<https://www.cnbc.com/2026/10/08/manus-fund-raise-meta-muse-tencent.html>；收入估算与业务模式（Sacra）<https://sacra.com/c/manus/>；价格（第三方，9 月 28 日）<https://www.usecarly.com/blog/manus-pricing/>
- **Genspark**：Series B 扩展与 ARR（路透社，Yahoo Finance 转载，6 月 17 日）<https://finance.yahoo.com/technology/ai/articles/genspark-ai-valued-2-6-132914723.html>；AI Workspace 6.0（Business Wire，Yahoo Finance 转载，7 月 21 日）<https://finance.yahoo.com/technology/ai/articles/genspark-unveils-ai-workspace-6-150700549.html>；与微软的战略合作（Business Wire，4 月 29 日）<https://www.businesswire.com/news/home/20260429907387/en/Genspark-Announces-Global-Strategic-Partnership-with-Microsoft-to-Embed-AI-Agents-Across-Microsoft-365-and-Agent-365>；价格（第三方，7 月 20 日）<https://www.eesel.ai/blog/genspark-ai-pricing>
- **Perplexity**：Computer 发布（TechCrunch，2 月 27 日）<https://techcrunch.com/2026/02/27/perplexitys-new-computer-is-another-bet-that-users-need-many-ai-models/>；积分规则 <https://www.perplexity.ai/help-center/en/articles/13838041-how-credits-work-on-perplexity>；CEO 访谈（CNBC，6 月 3 日）<https://www.cnbc.com/2026/06/03/perplexity-ceo-ai-valuations-computer-agentic.html>；年化收入与估值汇总（引 The Information、FT，11 月 1 日更新）<https://valueaddvc.com/blog/perplexity-ai-valuation-revenue-2026>；ARR 5 亿美元（The Information）<https://www.theinformation.com/briefings/perplexitys-arr-rises-500-million>

**海外平台巨头与模型厂**

- **微软**：E7 发布 <https://blogs.microsoft.com/blog/2026/03/09/introducing-the-first-frontier-suite-built-on-intelligence-trust/>；E7 构成（CNBC，3 月 9 日）<https://www.cnbc.com/2026/03/09/microsoft-office-365-e7-copilot-ai.html>；Copilot Cowork 进入 Frontier（3 月 30 日）<https://www.microsoft.com/en-us/microsoft-365/blog/2026/03/30/copilot-cowork-now-available-in-frontier/>；全面上线与计费（6 月 16 日）<https://www.microsoft.com/en-us/copilot/blog/2026/06/16/copilot-cowork-is-now-generally-available/>
- **谷歌**：I/O 2026 订阅调整（5 月 19 日）<https://blog.google/products-and-platforms/products/google-one/google-ai-subscriptions/>；Gemini Enterprise 发布与席位价格（TechCrunch，2025 年 10 月 9 日）<https://techcrunch.com/2025/10/09/google-ramps-up-its-ai-in-the-workplace-ambitions-with-gemini-enterprise/>
- **Meta**：Muse 发布（The Next Web，9 月 9 日，价格引 Axios）<https://thenextweb.com/news/meta-muse-personal-ai-agent-launch>
- **Anthropic**：Claude 定价 <https://claude.com/pricing>；Cowork 向所有付费套餐开放（eWeek，4 月 10 日）<https://www.eweek.com/news/claude-cowork-general-availability-enterprise-controls/>
- **OpenAI**：workspace agents 介绍 <https://openai.com/index/introducing-workspace-agents-in-chatgpt/>；Business 版本说明 <https://help.openai.com/en/articles/11391654-chatgpt-business-release-notes>；Business 席位价格 <https://help.openai.com/en/articles/8792828-what-is-chatgpt-business>；credits 费率 <https://help.openai.com/en/articles/11481834-chatgpt-rate-card-business-enterpriseedu>

**垂直智能体与趋势数据**

- Intercom Fin 定价 <https://fin.ai/pricing>；Salesforce Agentforce 定价 <https://www.salesforce.com/agentforce/pricing/>；Sierra 增长与定价说法（官方博客，10 月 30 日）<https://sierra.ai/blog/our-customers-ambition-is-accelerating-our-growth>；Sierra 融资（CNBC，5 月 4 日）<https://www.cnbc.com/2026/05/04/bret-taylor-sierra-fundraise-openai.html>
- 推理价格趋势（Epoch AI）：<https://epoch.ai/data-insights/llm-inference-price-trends>、<https://epoch.ai/publications/the-plunging-price-of-thought>
