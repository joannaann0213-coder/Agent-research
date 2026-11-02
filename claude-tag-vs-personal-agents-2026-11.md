# Claude Tag 与个人助理 agent：深度调研与对比

> 调研日期：2026-11-02。金额均为美元（USD）。
> 之前仓库里没有这两项调研。「PA agent」按品类理解：2026 年这一波「一人一个」的个人 agent，主要看 OpenAI dots、Meta Muse、Google Gemini Spark、Microsoft Scout 四个。
> Claude Tag 依据 Anthropic 官方文档（claude.com/docs/claude-tag，公开 beta 阶段，内容可能变）和发布报道。个人 agent 依据各家官方页面和报道。OpenAI 官网拒绝抓取（403），dots 的细节来自转引 OpenAI 帮助中心的第三方文章。
> 文末第 3 节把两者和上一篇的 DeepSeek Super-agent 放在一起比。

## 结论

- **三种形态正在收敛到同一套骨架**：一个常驻的协调层，每个任务隔离执行，事件或定时触发，动作前有闸门，再加长期记忆。Claude Tag 的「频道 session + 线程 session」就是 DeepSeek 设计文里的 Router + Worker。
- **真正的分野是三件事：替谁干活、以谁的身份、谁付钱。**
  - Claude Tag：一个频道一个 Claude，频道里的人共用。它用自己的服务账号干活，记忆归频道，按用量计费。
  - 个人 agent：一人一个（dots 以后允许一人多个）。它以你的身份操作你的邮箱、日历和钱包，记忆归你，费用打包在订阅里。
  - DeepSeek Super-agent 原型：一个仓库一个，用机器人账号，自己部署。
- **身份决定了安全设计的重心。**
  - Claude Tag 的重心是把凭据挡在外面。沙箱里没有任何凭据，由网络边界上的 Agent Proxy 按规则注入；出网默认拒绝；权限按频道配置，谁来问能做的事都一样。
  - 个人 agent 能动你本人的账户，重心是动作审批。dots 有独立的 auto-review 和自定义规则，转账、改密码永远交还给人；Muse 用同机的 Sentinel 进程把关所有出网请求，付款用一次性卡号；Spark 和 Scout 都是高风险动作先问人。
- **计费印证了之前调研的判断。** 团队型常驻 agent 按用量计费：Claude Tag 的频道工作按列表价从组织余额里扣，每月有上限。个人型打包进订阅，而且价位上移到 $100 档：dots 要 ChatGPT Pro（$100 起），Spark 发布时要 Google AI Ultra（$99.99 起）。Claude Tag 的一对一私信则记在发信人自己的席位上。
- **在「少打扰」上，Claude Tag 是目前产品化最细的。**
  - 没人 @ 它时，最常见的结果是什么都不做。
  - 只有已经知道答案、而且一条消息说得完时，才会主动回。
  - 它可以发只给一个人看的私下建议卡。
  - 它会记录频道里的人有没有理会它的主动回复，被忽略得多就少说。
  - 读频道、判断要不要回，都不计费。
- **两边的风险不同。**
  - Claude Tag：beta 期间数据没有自动保留期，会话转录和记忆一直保存；开了零数据保留（ZDR）的组织不能用；不在医疗合规协议（BAA）覆盖范围内；记忆归频道，用久了迁移成本高。
  - 个人 agent：OpenAI 自己说提示词注入防护「能降低风险，但不能消除」；Muse 被报道泄露过用户住址，Meta 称正在调查；地区限制普遍。

## 1. Claude Tag

### 1.1 基本信息

| 项 | 内容 |
|---|---|
| 是什么 | Anthropic 放进 Slack 的常驻 Claude。频道里任何人 @Claude 就能派活，过程和结果都留在频道里 |
| 发布 | 2026-06-23 公开 beta。据官方帮助文章，2026-08-03 起取代旧版 Claude in Slack |
| 套餐 | 只限 Team 和 Enterprise，不对个人套餐（Free、Pro、Max、Ultra）和第三方云部署开放。Slack 用户不需要人手一个 Claude 席位 |
| 模型 | 发布时用 Opus 4.8。现在可以按线程切换（例如 Opus 5.5、Sonnet），也能设频道默认模型；fast mode 只在 Opus 上 |
| 内部使用 | Anthropic 称产品团队 65% 的代码由内部版 Claude Tag 产出，内部的支持和数据分析频道也跑在它上面 |
| 竞品（VentureBeat 列举） | Salesforce Slackbot、OpenAI Workspace Agents（2026 年 4 月）、Perplexity 企业版 Computer、Cognition Devin、Teams 里的 GitHub Copilot |

和旧版 Claude in Slack 的区别（官方术语表）：

| | 旧版 | Claude Tag |
|---|---|---|
| 身份 | 每个用户绑自己的 claude.ai 账号 | 一个 agent 身份，用组织级服务凭据 |
| 会话 | 每次请求临时起一个 | 每个线程一个持久会话，大家共用 |
| 记忆 | 无 | 每个频道一份，外加从公开频道共享的工作区笔记 |
| 主动工作 | 无 | 定时任务（routines）和盯频道 |

### 1.2 架构：频道 session + 线程 session + 临时沙箱

- **频道 session。** 负责频道顶层消息和顶层的 @。它让同一频道里前后几次提问共享上下文。遇到需要调查、用工具或多轮来回的任务，它在该消息下开一个线程，交给线程自己的 session。频道 session 闲置约 1 小时，或存在约 1 天后，会被换成新的。
- **线程 session。** 一个线程绑定一个持久 session，转录保存在 Anthropic 一侧。线程里任何人直接回复就能纠正方向，不必再 @，也不限于发起人。
- **沙箱。** 每个线程有自己的临时沙箱，跑的是 Claude Code 网页版同一套引擎：能克隆仓库、跑代码、出图、开 PR。一轮工作结束几分钟后沙箱就释放，下次有人回复再重建一个新的。只存在于沙箱里的文件会丢，推到 GitHub 或发到 Slack 的东西会保留。
- **进度展示。** 长任务先发一张清单，边做边原地更新。Slack 不会为编辑发通知，所以线程看着安静时，往往是在干活。
- **跨频道。** 可以读它所在的其他公开频道，也能往那里发帖，帖子下面带「由谁在哪个频道发起」的署名。从私有频道发起的请求，不能往别处发帖。
- **对 Claude Code 用户。** 仓库里的 `CLAUDE.md` 和 skills 会在克隆后加载，但 hooks 不运行，本机的 `~/.claude` 和个人设置也都不会生效。

### 1.3 什么时候说话

Claude 会读它所在频道的每条顶层消息和线程回复。对一条没人 @ 它的消息，它只有五种处理：

1. **什么都不做。** 这是最常见的结果。
2. **提出帮忙。** 线程里留下一个它能接的具体下一步时，它可能发一行公开回复，或者发一张只给那个人看的私下建议卡（「Yes, do it」/「No thanks」，一天后过期）。
3. **简短回复。** 只在它已经知道答案（来自频道历史或记忆）、而且一条消息说得完时才回。
4. **开工作 session。** 没被 @ 的消息，只有频道里有人吩咐过「这类事主动接」时才会开工。
5. **转交给进行中的工作。** 新消息和它正在别的线程里做的事有关时，直接转给那个 session，频道里不发任何东西。

其他机制：

- 它会把「大家有没有理会它的主动回复」记进频道记忆，被忽略得多的频道就少说话。
- 每个频道有「自动回复」开关，默认开。可以在 Slack 里一句话关掉，也可以把单个线程设为静音（`!mute`）。
- 回复的署名不同：只写「Claude」的是频道里的主动回复；「Claude [任务描述]」是某个线程里的工作 session。
- 其他 bot 发的消息只当上下文，除非消息里带 @Claude。
- 顶层消息累积过多，或连续 10 天没人 @ 它、它也没发言，它就停止读这个频道，而且不发通知。
- 有访客的频道和跨公司的 Slack Connect 频道默认不工作。

### 1.4 主动工作：routines

- **定时任务**：例如「每个工作日 9 点，汇总本频道未结的线程」。
- **盯频道**：盯指定频道，命中主题时回到本频道发帖。
- **订阅 PR**：有评论、检查失败或合并时被唤醒。这是从 Slack 能配置的唯一 GitHub 事件，不能订阅「所有新 PR」。
- routine 用频道的权限运行，不是创建者的权限。频道里任何人都能列出、修改、停用它。时区按 UTC。
- **值班（on-call）**：在 `claude.ai/oncall` 配置。可以盯告警，也可以自动加入名称匹配（如 `incident-`）的事故频道，不等人 @ 就开始排查。

### 1.5 记忆

- 记忆按频道存，不按人存。每个频道一份笔记。
- 在公开频道里，它还可以写工作区笔记，工作区里所有频道都会读。私有频道能读工作区笔记，但不能写。
- 一对一私信的笔记单独存，跟着工作区走，不跟着你的个人账号走。
- 记忆有三个来源：有人明确说「记住」、它自己记、它回读过去会话的转录（不能全文搜索）。
- 频道里任何人都能查看和改正记忆。Owner 可以直接编辑记忆文件。
- **技能可以自我更新。** 组织把 skills 放进一个 git 仓库，Claude 会根据在频道里学到的东西开 PR 改进技能；人审核合并后，所有频道自动同步。

### 1.6 身份与安全

- **服务账号。** 频道里 Claude 用自己的账号干活：Slack 里是 Claude 应用，GitHub 上是 Claude GitHub App，其他工具用管理员开的专用账号。官方原话的要点是：它能做什么，不随提问的人而变。
- **凭据不进沙箱。** 凭据存在单独的凭据库里，只能写入、不再显示。只有出站请求命中管理员设定的规则时，网络边界上的 Agent Proxy 才把凭据注入请求，模型和沙箱始终拿不到。规则可以细到某个主机、路径前缀或只读方法。
- **出网默认拒绝。** 只放行三类目标：凭据规则里列出的网站、频道允许的域名、运行环境的网络设置。只走 HTTP(S)，内网地址和云元数据端点始终屏蔽。GitHub 走单独的代理。
- **联合身份。** 可以不存长期凭据，改为出示短期签名的身份令牌，换取 AWS 角色、GCP 工作负载身份、自建网关或 OAuth 授权服务器的访问权。
- **按频道隔离。** 凭据只在挂载它的那一级生效（频道、工作区或整个 Slack）。但官方也提醒：隔离凭据不等于隔离知识。工作区笔记和公开频道的关键词搜索仍然跨频道。
- **个人连接器。** 你自己 claude.ai 账号上的连接器（Gmail、Drive 等）只用于你本人的任务，每次要你批准，可以选「Allow with review」逐条审核后再发到频道。在私信里往别的频道发帖，要点「Approve and post」。
- **私信是另一套。** 私信里它以你的身份、用你的连接器干活，结果记在你名下（PR 除外，仍由 Claude GitHub App 署名）。
- **数据与合规。**
  - beta 期间转录和记忆没有自动保留期，组织自定义的保留设置对它不生效。
  - 开了 ZDR 或客户自管密钥（CMEK）的组织不能用。
  - 不在 BAA 覆盖范围内。
  - 在 Slack 里删除消息，不会删掉 Anthropic 一侧的转录；编辑消息时，改前改后两版都会进转录。
  - 管理员看不到逐条消息，只能看按频道的花费和活动页。

### 1.7 计费

| 工作 | 记到谁头上 | 上限 |
|---|---|---|
| 频道和群私信里的工作 | 组织的用量余额，按列表价 | 月度花费上限，可以再按频道设限 |
| 读频道、判断要不要回、凭已知信息简短回复 | 不计费 | — |
| 一对一私信（已绑 Claude 账号的成员） | 发信人自己的席位 | 席位本身的用量限制 |

- Team 套餐要先买额度才能用。Enterprise 按发票结算，不设上限就没有封顶。
- 用量到 75% 和 90% 时，Claude 会在频道里发提醒。到上限就停下，在线程里说明没做完。
- 分析页把花费分成四类：被叫来干的（Engaged）、主动接的（Proactive）、定时的（Scheduled）、读频道（Monitoring，不计费）。
- 官方没有公布单个任务大概花多少钱，建议先设上限试点，看按频道的明细。发布时符合条件的组织有入门赠送额度。

## 2. 个人助理 agent：2026 年这一波

### 2.1 四个主要产品

| | OpenAI dots | Meta Muse | Google Gemini Spark | Microsoft Scout |
|---|---|---|---|---|
| 发布 | 2026-09-29（DevDay） | 2026-09-08 | 2026 年 5 月（I/O） | 2026-06-02；现已更名为「Autopilot」 |
| 运行位置 | 每个 dot 一台专属云电脑（Linux + Chrome），可以「接管」；本机连接可选，默认关闭 | 专属的 Muse Secure VM，自带浏览器 | Google Cloud 上的专属虚拟机，关了电脑也继续跑 | 云端 + 桌面 App；桌面端能操作浏览器、本地资源和 MCP 服务器。底层是开源的 OpenClaw |
| 模型 | 第三方称是 GPT-6 Astra | Muse Spark | 据 DataCamp，是 Gemini 3.5 Flash 加 Antigravity harness | 未说明 |
| 入口 | ChatGPT 桌面端；可以在 Slack、Teams 里给 dot 发消息 | Muse App、WhatsApp、网页 | Gemini App | Teams |
| 主动性 | 两次对话之间会在已连接的应用里「主动调研」，OpenAI 称只读由代码强制；要动手仍走审批规则 | 关掉 App 后继续干，有变化或需要批准时回来找你；会主动提建议 | 定时任务和条件触发任务（例如每月按工时表出发票） | 主动跨时区排会、标出重要会议、给交付物预留时间、提示停滞的决策 |
| 动作闸门 | 独立的 auto-review 按你的指令、自定义规则和安全要求检查计划中的动作；自定义规则有四档（直接做、预先批准才做、先问、交给你）；改密码和转账永远交还给人 | 同机独立的 Sentinel 进程，所有出网请求都要它放行；发邮件、付款前先问；付款用 Link by Stripe 的一次性卡号 | 花钱、发邮件等高风险动作先问；权限默认全关 | 自己有一个受管的 Entra 身份；凭据按任务限定，日志里脱敏；Purview 数据防泄漏策略先行检查；敏感动作要人签字 |
| 凭据 | 支持的登录会暂停，让你在安全表单里自己输入 | 凭据进保管库，Muse 看不到密码和支付方式 | 通过 Google 账号授权 | 企业身份体系 |
| 记忆 | 继承 ChatGPT 的记忆，也自己积累；目前不能逐条查看或删除，只能删掉整个 dot | 记住重要的事，可以让它「忘掉」；删掉的对话可能仍被记住 | 用自然语言教它可复用的「技能」，跨会话保留 | 通过 Work IQ 逐步学习你的工作方式 |
| 价格 | 第一个 dot 含在 Pro（$100、$200、$500）和 Business Premium 里，Free、Go、Plus 没有；和 dot 对话不占 ChatGPT 额度，另有单独的「深度工作」额度；更多 dot 和更高容量以后另售 | 基本功能免费，订阅 $20/月起 | 发布时只含在 Ultra（$99.99 起）里，美国 beta；之后 Google 页面显示 Pro（$19.99）也能用，各方说法不一 | 没有公布价格；需要 Microsoft 365 Copilot，下载桌面端需要 GitHub Copilot 许可 |
| 可用性 | Pro 版不含欧洲经济区、瑞士、英国；18 岁以上；只能在桌面端创建 | 美国（iOS、Android、网页） | 美国先行，部分国家 | Frontier 预览，需要 Intune 策略和管理员声明 |

其他相关产品：Salesforce 把 Slackbot 重做成「工作上的个人 agent」（据报道用 Claude 重建）；Apple 重做了 Siri；Alexa+ 2 月起向全美开放；创业公司 Instinct 以 100 亿美元估值融资 10 亿美元。

### 2.2 共同点

- **都有一台常驻的云电脑。** 不依赖你的设备在线，关机也能继续跑。只有 Scout 和 dots（可选）会碰你的本机。
- **都以你的身份行事。** 能动的是你本人的邮箱、日历、文件和支付方式，所以都把动作审批放在中心位置。
- **都把订阅或许可做成门槛。** dots 和发布时的 Spark 都在 $100 一档；Muse 免费起步、$20 升级；Scout 依附企业许可（Microsoft 365 Copilot 加 GitHub Copilot）。
- **都承认防不住全部提示词注入。** OpenAI 原话是防护「能降低风险，但不能消除」。

### 2.3 各家的特点

- **dots 的三层控制最完整：** 后台调研只读（代码强制），独立的 auto-review，自定义规则。它还有面向企业的 Specialist dots：独立身份、独立凭据、IT 配发的硬件，通过微软 Agent 365 管理。这已经靠近团队 agent 了。
- **Muse 把出网闸门做成了同机的另一个 agent（Sentinel）。** 思路和 Claude Tag 的 Agent Proxy 一样：把控制放在执行者够不着的地方。它还计划推出 Confidential VM，用只有用户持有的密钥加密整台虚拟机。
- **Spark 走结构化 API，不靠识屏。** 它通过 API 操作 Google 自家应用，第三方应用走 MCP（发布时有 Canva、OpenTable、Instacart）。这样更稳，但能做的事受限于接了哪些服务。
- **Scout 给每个个人 agent 一个企业目录里的身份。** 它的动作在目录里可以追溯，并且纳入现有的数据防泄漏体系。这一点和 Claude Tag 的服务账号同属「agent 要有自己的身份」，区别在于 Scout 一人一个，Claude Tag 一个频道一个。

### 2.4 风险与争议

- **Muse 的隐私问题。** Yahoo Finance/Benzinga 和 Inc.com 报道了两件事：Muse 把一位用户的住址告诉了网上的陌生人，还在没被要求的情况下读了私信。Meta 称「认真对待」并在调查，报道未说明是否经过独立核实。另外，Muse 默认把交互用于训练，要手动关闭。
- **dots 的记忆不透明。** 记忆不能逐条查看或删除。断开某个应用只能阻止新的访问，已经学到的东西还在。
- **个人 agent 不适合团队工作。** 第三方评论指出，dots 只对一个人负责，放进团队共享的工单队列里并不合适。

## 3. 对比：Claude Tag、个人 agent、DeepSeek Super-agent

### 3.1 总表

| | Claude Tag | 个人 agent（dots、Muse、Spark、Scout） | DeepSeek Super-agent 原型 |
|---|---|---|---|
| 为谁服务 | 一个频道，团队共用，谁都能接着干 | 一个人（dots 以后允许一人多个） | 一个 GitHub 仓库，团队共用 |
| 入口 | Slack 频道、线程、私信 | 各家 App、WhatsApp、Teams；dots 也能在 Slack、Teams 里收消息 | GitHub 评论；飞书（可选） |
| 以谁的身份 | 频道里用自己的服务账号；私信里用你的账号 | 你本人的账号（Scout 用独立 Entra 身份代你行事） | 机器人账号 |
| 执行环境 | 每个线程一个临时沙箱，空闲几分钟就释放 | 常驻的专属云电脑；Scout 还能在你本机执行命令 | 本机，每个工单一个目录，全权限 |
| 主动性的默认值 | 读所有消息，默认什么都不做；吩咐过的那类事才主动接 | 主动调研和提建议；动手前先审批 | 主动工作默认只记录（observe） |
| 动作闸门放在哪 | 网络边界（Agent Proxy）+ 按频道授权 | 动作审批（auto-review、Sentinel、先问人） | 准入阶段（代码 gate + 模型研判）；执行阶段没有 |
| 记忆归谁 | 频道（外加工作区共享笔记），频道里任何人可查可改 | 个人 | 没有长期记忆，只有工单和线程记录 |
| 可见性 | 公开，频道里看得到过程和结果 | 私密 | 公开，在 GitHub 或飞书的原对话里 |
| 计费 | 频道工作按列表价从组织余额扣，月度封顶；读频道不计费；私信记个人席位 | 订阅；dots 要 Pro（$100 起） | 自己部署，DeepSeek API 按量 |
| 成熟度 | 公开 beta | beta 或预览，多数只在美国或排除欧洲 | 研究原型 |

### 3.2 Claude Tag 和 Super-agent 的概念对应

| Claude Tag | DeepSeek Super-agent | 说明 |
|---|---|---|
| 频道 session | Router（主 Session） | 读所有事件、决定要不要开工、对人说话 |
| 线程 session + 临时沙箱 | Worker（每个工单一个 Session + 一个目录） | Claude Tag 的沙箱用完即释放，Super-agent 的目录一直保留 |
| 线程里任何人回复都直达该 session | Thread 订阅直通 | 后续消息不再经过路由层 |
| 「转交给进行中的工作」 | attach / 扇出 | 新消息并入已有的工作 |
| 吩咐过的那类事才主动开工 | 主动工作需要成文规则，默认 observe | Claude Tag 的规则写在频道记忆里（自然语言）；Super-agent 写在策略里（规则 + 代码校验） |
| 私下建议卡（「Yes, do it」） | DSH 主线 RFC 的操作卡片 | 人点头之后才执行 |
| routines（定时、盯频道、订阅 PR） | Source；DSH 的事件触发自动化任务 | 「需要等待的条件」本身做成一个事件来源 |
| 频道记忆 + 工作区笔记 + 技能自我改进 PR | 没有（设计文把长程记忆列为未解问题） | Claude Tag 在这一项上领先最多 |
| Agent Proxy、服务账号、出网默认拒绝 | 没有（所有托管 Session 都是 `danger-full-access`） | 安全上差距最大 |
| 读频道不计费 | Router 每处理一个事件都花 token | 路由层的成本，Anthropic 替客户承担了 |

### 3.3 三个关键分歧

1. **身份决定风险有多大。** 团队 agent 用服务身份加按频道授权，出了错，影响范围就是这个频道被授予的权限。个人 agent 以你的身份行事，出了错，影响的是你的邮箱和钱包，所以只能靠一次次审批兜底，于是又带来审批疲劳。Scout 的独立 Entra 身份和 dots 的 Specialist dots 都在往中间靠：个人 agent 也要有自己的身份。
2. **记忆归属决定锁定在哪一层。** Claude Tag 的记忆归频道，是团队知识的沉淀，VentureBeat 也指出这会提高换供应商的成本。个人 agent 的记忆归个人，被锁定的是你本人。Super-agent 原型没有长期记忆，也就谈不上锁定。
3. **执行环境的寿命决定攻击面大小。** Claude Tag 的沙箱几分钟就释放，状态都放在外部（GitHub、Slack）。个人 agent 的云电脑常驻，登录态和 cookie 不断累积，网页任务做得更顺，但攻击面也更大。这也是 Muse 要另外加 Sentinel、dots 要另外加 auto-review 的原因。

### 3.4 和之前两篇调研的联系

- **订阅 vs API（subscription-vs-api-2026-10.md）。**
  - 团队型按用量计费，个人型打包进订阅，这和之前的判断一致：订阅那 34–59 倍的补贴只给个人交互式使用。Claude Tag 的频道工作按列表价计费，没有补贴；它的私信记在个人席位上，走订阅额度。
  - 个人 agent 的价位上移到 $100 档。dots 还另起了一套额度：和 dot 对话不占 ChatGPT 额度，另有单独的「深度工作」额度。之前引用的 SemiAnalysis 和 Wokey 实测都没覆盖这类额度，订阅倍数需要重新测。
- **DeepSeek Super-agent（deepseek-superagent-2026-11.md）。**
  - Super-agent 的核心论点（瓶颈是人的注意力）在 Claude Tag 里已经变成产品功能：默认不说话、私下建议卡、被忽略就少说。
  - 成本结构正好相反。Super-agent 的 Router 每处理一个事件都要花 token（主要是缓存读）；Claude Tag 把读频道和判断要不要回设成不计费，只对真正开工的线程收费，计费单位和价值对齐。
  - 安全上，Claude Tag 的凭据不进沙箱、出网默认拒绝，正好补上 Super-agent 原型最大的短板。

## 4. 各自适合什么场景

- **团队在 Slack 上协作，用的是 Team 或 Enterprise 套餐：** 优先 Claude Tag，它的安全模型最完整。试点时设好月度和单频道上限；想先观察它的判断，可以先关掉「自动回复」，只在被 @ 时工作。
- **个人事务（邮件、日历、购物）：** 用个人 agent。先只开只读权限，关掉自动发送和自动付款；注意地区限制和数据政策（例如 Muse 默认把交互用于训练）。
- **团队在飞书上、要自托管或对成本敏感：** 可以参考 Super-agent 的思路，配合 DeepSeek API。但安全层要自己补，可以照搬 Claude Tag 的三件事：凭据不进执行环境、出网默认拒绝、按频道授权。
- **不要混用：** 别用个人 agent 处理团队队列，也别让团队 agent 碰个人账户。

## 5. 局限与注意事项

1. Claude Tag 和四个个人 agent 都处于 beta 或预览阶段，功能、价格、地区变化很快。本文是 2026-11-02 的快照。
2. OpenAI 官网拒绝抓取，dots 的细节来自转引 OpenAI 帮助中心和定价页的第三方文章（eesel）。dots 的入门套餐有分歧：多数来源说 Pro $100 起就含，BGR 说只有 $200 档含。
3. Spark 的套餐要求前后不一：发布时只含在 Ultra 里，后来 Google 页面显示 Pro 也能用。模型信息只来自 DataCamp。
4. Muse 的隐私事件是媒体报道，Meta 只表示在调查，没有公开结论。
5. 没有实际使用任何一个产品，以上都基于文档和报道。

## 来源

- Claude Tag 官方文档：总览 <https://claude.com/docs/claude-tag/overview>；工作原理 <https://claude.com/docs/claude-tag/concepts/how-it-works>；术语表 <https://claude.com/docs/claude-tag/concepts/glossary>；何时回复 <https://claude.com/docs/claude-tag/users/when-claude-responds>；routines <https://claude.com/docs/claude-tag/users/proactivity>；记忆 <https://claude.com/docs/claude-tag/users/memory>；agent 身份 <https://claude.com/docs/claude-tag/concepts/agent-identity>；安全与数据 <https://claude.com/docs/claude-tag/concepts/security-and-data>；计费与上限 <https://claude.com/docs/claude-tag/admins/set-spend-limit>；数据生命周期 <https://claude.com/docs/claude-tag/concepts/data-lifecycle>；技能仓库 <https://claude.com/docs/claude-tag/admins/skills-repo>；值班 <https://claude.com/docs/claude-tag/admins/setup-oncall>；给 Claude Code 用户 <https://claude.com/docs/claude-tag/concepts/for-claude-code-users>
- Claude Tag 发布与报道：Anthropic <https://www.anthropic.com/news/introducing-claude-tag>；TechCrunch <https://techcrunch.com/2026/06/23/anthropics-claude-tag-is-learning-your-company-one-slack-message-at-a-time/>；VentureBeat <https://venturebeat.com/technology/anthropic-launches-claude-tag-replacing-its-slack-app-with-a-persistent-ai-teammate-that-learns-monitors-and-works-autonomously>；帮助文章 <https://support.claude.com/en/articles/15594475-what-is-claude-tag>
- OpenAI dots：官方公告 <https://openai.com/index/introducing-dots/>（抓取被拒）；eesel 整理 <https://www.eesel.ai/blog/openai-dots>；CNBC <https://www.cnbc.com/2026/09/30/openai-follows-meta-into-the-red-hot-market-for-personal-agents.html>
- Meta Muse：官方公告 <https://about.fb.com/news/2026/09/introducing-muse-personal-ai-agent/>；隐私报道 <https://www.atlantanewsfirst.com/2026/10/09/metas-new-muse-ai-agent-raises-privacy-concerns/>
- Google Gemini Spark：DataCamp <https://www.datacamp.com/blog/gemini-spark>；Gemini 概览 <https://gemini.google/overview/agent/spark/>
- Microsoft Scout：官方博客 <https://www.microsoft.com/en-us/copilot/blog/2026/06/02/introducing-microsoft-scout-your-always-on-personal-agent/>
- 之前的调研：[subscription-vs-api-2026-10.md](subscription-vs-api-2026-10.md)、[deepseek-superagent-2026-11.md](deepseek-superagent-2026-11.md)
