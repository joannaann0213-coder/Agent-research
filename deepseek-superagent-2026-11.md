# DeepSeek Super-agent：设计文与开源原型拆解，以及和「订阅 vs API」调研的差异

> 调研日期：2026-11-01。金额均为美元（USD）。
> 代码基于 `deepseek-ai/dsh-superagent-prototype` 的唯一一次提交 `6337fc8`（2026-10-31）和 `deepseek-ai/deepseek-harness` 的 master `ce7073d`（2026-10-30）。只做了静态阅读，没有运行：原型需要 DSH 和一个 GitHub 机器人账号，而且以 `danger-full-access` 权限运行。
> 设计文《Exploring Super-agent Design: Towards a More Autonomous Agent System》（中文名《Super-agent 设计探索：迈向更自主的 Agent 系统》）没有找到 DeepSeek 官方原文链接，原型 README 只给了标题。本文关于设计文的内容来自 IT之家、InfoQ（新浪转载）、Offloop 英译和什么值得买评论，数字至少在两个来源间核对过。

## 结论

- **这次发布的是三样东西，不是新模型，也不是产品。**
  1. 设计文。
  2. 原型 `dsh-superagent`：只服务一个 GitHub 仓库的常驻 coding agent。MIT 许可，1.5 万行 TypeScript 加 1.5 万行测试，只有一次提交。README 明说它是研究原型，和内部版本、和设计文都不一致。
  3. 底座 DeepSeek Harness（DSH）：8 月 13 日开源，截至 11 月 1 日有 26.8 万 star。原型是 DSH 的一个插件 bundle。
- **核心判断成立，但不新。** 同时推进很多事时，瓶颈从模型能力变成人的注意力：一个人只能跟进 3–5 个活跃 session。所以「会话」应该从工作单位降为实现细节，具体是三件事：
  - Router：一个常驻的主 Session。
  - Worker：一个工单一个 Session。
  - Thread 订阅直通：已分派线程上的后续消息直接发给 Worker。

  设计文自己也承认，这些是快慢路径、缓存、投机执行、存算分离等经典系统思想在 agent 上的应用。
- **最值得学的是治理层，不是 Router-Worker。** 原型用确定性代码加可审计的领域模型，规定 agent 什么时候动手、什么时候说话：
  - 准入（gate）由代码判定。模型只做研判，研判结果还要过代码校验。
  - 主动工作默认是 `observe` 模式，只记录「本来会接」，不真的接。
  - 每个工单的授权（Mandate）记录策略版本。策略变更时不取消正在跑的工单，只通知它；工单恢复前按当前策略重新校验。
  - IM 回复最多 100 词，进度不外发，一次只问一个问题。

  在我读过的开源实现里，它把「少打扰」落实得最彻底。
- **有四处要打折扣：**
  1. 文章和代码不一致。Watchdog、投机执行、云端存算分离和 BYOM 在原型里都没有。异步 ReAct 只实现了一部分：前台命令超过 5 秒自动转入后台。
  2. 实验是作者自测的合成负载：约 300 条消息，其中约 20 个工作请求；编码换成只耗时间的假工具；每个方案跑 5 次；时间缩放到 1/4。它能说明调度效果，说明不了代码质量和成本。
  3. 安全很弱。所有托管 Session 都是 `danger-full-access`，Worker 以运维者的系统用户身份免审批执行命令，命令黑名单用绝对路径就能绕过。
  4. 主动能力很窄。默认只有「文案修复」「视觉修复」两条规则。
- **战略上，DeepSeek 在用「开源 harness + 极便宜的 API」争夺 harness 这一层。**
  - DSH 已经带有这些能力（其中不少是随安装附带、默认关闭的实验插件）：桥接 Claude Code 的 hooks 和 mods，把真实的 Claude Code 和 Codex 作为子 agent 运行，以及 agent team、ralph loop、goal。
  - 主仓库 10 月 29 日提出了一份 RFC（状态 proposed），要把 Super-agent 的思路做成正式功能「事件触发的自动化任务」。这份方案比原型保守得多：事件轮次只读，只有通知工具能触达用户，用户接受之后才执行。

## 1. 和我们之前调研（订阅 vs API）的差异

### 1.1 回答的不是同一个问题

| | 之前的调研 | DeepSeek Super-agent |
|---|---|---|
| 回答的问题 | 同样的 token，订阅和 API 哪个更划算（计费问题） | 同时有很多事要做时，agent 系统怎么组织（架构问题） |
| 分析单位 | 一个人的订阅席位；一个交互式编码 session | 一个团队的常驻 agent；一个工单（Ticket） |
| 假设的瓶颈 | token 额度，准确地说是非缓存 token（Claude Pro 每月约 1 亿） | 人的注意力（一个人跟进 3–5 个 session） |
| 负载形态 | SemiAnalysis 的 agentic 结构：96.6% 是缓存读 | 常驻 Router 反复读长上下文，多个 Worker 并发；设计文承认是「用 token 换时间」 |
| 证据 | 两个独立第三方（SemiAnalysis、Wokey）实测，非缓存 token 数量互相印证 | 作者自测，合成负载，每方案 5 次 |
| 计费归属 | 个人订阅（5 小时窗口 + 周额度） | 团队机器人账号，24 小时运行，本来就应该走 API 计费 |

### 1.2 两者的交汇点：缓存读

之前的核心结论：agentic 负载 96.6% 是缓存读，而订阅几乎不把缓存读计入额度，所以 Claude 订阅用满能拿到 34–59 倍于月费的 API 价值。

Super-agent 恰好是一个缓存读更多的架构。设计文的消融实验里：

- 关掉 Thread 订阅直通后，Router 的决策次数多了约 30%，缓存读从 2,323 万涨到 3,177 万 token（+37%）。
- 完整方案的缓存读也比单 session 方案多，换来的是更短、更稳定的完成时间。

这种架构只在缓存读便宜时才划算，而 DeepSeek 自己的定价正好让缓存读很便宜：

| 模型 | 缓存读价 ÷ 未命中输入价 | 缓存写附加费 |
|---|---|---|
| Opus 5.5（Anthropic API = Bedrock global） | $0.20 ÷ $4.00 = 5% | 有：5 分钟缓存为输入价的 1.25 倍，1 小时缓存为 2 倍 |
| DeepSeek V4-Pro | $0.044 ÷ $1.32 = 3.3% | 无（价目表只有缓存命中、未命中、输出三列） |
| DeepSeek Flash（V4.1-Flash） | $0.006 ÷ $0.30 = 2% | 无 |

DeepSeek 在低谷时段全部价格再打五折。高峰时段是 UTC 周一到周五 01:00–04:00 和 06:00–10:00，其余时间（含周末）都是低谷。

### 1.3 同样的 token，在 DeepSeek 上要花多少钱

沿用之前调研第 4 节的 SemiAnalysis agentic 结构（新输入 0.4%、缓存读 96.6%、缓存写 2.6%、输出 0.3%），先算每百万 token 的价格，再算 Claude Pro 用满一个月的量（约 25–30 亿 token）：

| 模型与时段 | 每百万 token（agentic 结构） | Claude Pro 用满一个月的量 | 折合 Pro 月费（$20）的倍数 |
|---|---|---|---|
| Opus 5.5 API | $0.40–0.48 | **$1,178** | 59x |
| DeepSeek V4-Pro 高峰 | $0.094 | $232–277 | 12–14x |
| DeepSeek V4-Pro 低谷 | $0.047 | $116–139 | 6–7x |
| DeepSeek Flash 高峰 | $0.018 | $45–54 | 2.3–2.7x |
| DeepSeek Flash 低谷 | $0.009 | $23–27 | 1.1–1.4x |

- 区间的算法：Opus 那一行下限按 1 小时缓存写计、上限按 5 分钟计，与之前的调研一致。DeepSeek 的缓存写按未命中输入价计。
- **一句话总结：DeepSeek Flash 低谷时段的按量价格，大约等于 Claude 打过补贴之后的订阅价。** 我没查到 DeepSeek 官方有 coding 订阅，第三方汇总都说只有按量 API。DSH 的 DeepSeek 账号登录也是从余额扣费：额度不足时返回 402，提示充值。
- 这里只比价格结构。不同模型的能力、分词器不同，完成同一个任务用的 token 数也不同，不能理解成「同样的活 DeepSeek 便宜 25 倍」。

### 1.4 对之前结论的四点补充

1. **「订阅 59 倍」不适用于 Super-agent 这类系统。** 那个倍数的前提是一个人交互式使用，受 5 小时窗口和周额度约束。团队级 24 小时运行的机器人应该走 API。Anthropic 的 Agent SDK 文档也写明：未经批准，第三方不得在产品中提供 claude.ai 登录或订阅额度。按 agentic 结构整体计算，DeepSeek 的按量价比 Opus 5.5 API 便宜约 4–26 倍（V4-Pro 到 Flash，均按高峰价）。
2. **并行之后，token 额度又会成为约束。** 原型默认同时跑 3 个 Worker（上限 16），再加一个常驻 Router。DSH 的 `subagent-claude-code` 运行子 agent 时继承本机 Claude Code 的登录状态，这可能就是 Pro 或 Max 订阅。也就是说，技术上可以把订阅额度接进 Worker 池。但这样 Pro 每月约 1 亿的非缓存 token 很快会用完，在条款上也有风险。
3. **缓存的有效期（TTL）会影响架构选择。** DSH 的事件触发 RFC 否决了「在常驻会话里判断每次轮询结果」的方案，理由之一是：轮询间隔一旦超过前缀缓存的有效期，每次都要在没有缓存的情况下重读整个上下文。这和之前调研里 5 分钟、1 小时两档缓存写的差价是同一个问题：常驻 agent 的成本取决于缓存命中率。
4. **之前的调研缺了一个维度：人的注意力。** 订阅 vs API 只比较每个 token 的价格。按 Super-agent 的思路，更应该比较的是每合并一个 PR 花多少 token 成本、人被打断几次。以后做实测时，建议两项都记录。

## 2. 设计文要点（来自转述）

| 节 | 内容 |
|---|---|
| 1 动机 | Super-agent 有四个特征：连续、高效、主动、持久。单 session 的 ReAct 循环做不到这四点 |
| 2 双层架构 | **Router**：常驻，处理所有来自人的入口并分派任务；自己不做具体任务，可以不给 Bash，常常用更轻的模型就够。**Worker**：隔离的 session，工具齐全，并行执行；不直接对人说话，由 Router 统一回复。Router 推理期间到达的 Trigger 合并进下一次调用 |
| 3 事件驱动 | **两级过滤**：代码走快路径（检查来源、群范围、白名单），模型走慢路径（按提示词里的自然语言策略判断）。**Thread**：同一个 PR、IM 话题或邮件会话里的事件；Worker 订阅 Thread 后，后续事件直达 Worker，不经过 Router。**上下文管理**：Inline & Spill 把窗口当缓存，先只放 id 和摘要，放不下的放到检索工具后面；链式召回沿 Trigger、Thread、Session 之间的 id 找回历史。**投机执行**：后台先尝试复现和修复，确认有简单的修法才开口 |
| 4 交互 | **异步 ReAct**：慢工具改成 `start()` / `stop()`，结果作为新事件返回。文中说这个机制已在 DSH 实现，默认没开。**输出**：IM 简短（约 100 词）；GitHub、邮件等异步渠道给信息密度高的完整内容 |
| 5 存算分离 | Agent Loop 跑在无状态的云容器里，session 存在数据库；本地只跑一个轻量 daemon 执行命令，即 BYOM（Bring Your Own Machine） |
| 6 Watchdog | 一个由定时消息驱动的独立 session。它检查基础设施故障，并用模型判断 Worker 有没有实质进展、方向有没有跑偏 |
| 7 实验 | 见下表 |
| 8 未解问题 | Router 如何扩展（分片会削弱全局记忆）；跨月的长程记忆；脱离 session 后人怎么协作；agent 之间的 A2A 通信；云端 agent 接入本地设备后的提示词注入和凭据泄露 |

实验（合成负载，作者自测）：

| 实验 | 设置 | 结果 |
|---|---|---|
| 消融 | 每次约 300 条消息，其中约 20 个工作请求；编码换成只耗时间的工具；时间缩放到 1/4，模型延迟用真实值；每个方案跑 5 次 | Super-agent 完成时间 p50 12.4 分钟，p95 15.7 分钟。单 session + Subagent 用中性提示词时，5 次都没调用 subagent，p95 29.5 分钟；改用「优先委派」提示词后，5 次里有 4 次委派，整体仍更慢，且有长尾。关掉订阅直通后，完成时间基本不变，Router 决策 +30%，缓存读从 2,323 万增至 3,177 万（+37%） |
| Router 压测 | Router 调真实模型，Worker 用 mock；Trigger 速率从约 1 条/分钟升到约 40 条/分钟 | 平均批大小 1.0 → 2.8；单次请求耗时 2.5 → 3.9 秒；每个决定分摊的推理时间 6.4 → 1.6 秒 |

全文最有说服力的其实是单 session 对照组：让模型临场决定要不要分工，结果很不稳定；把分工放到系统层更可靠。

## 3. 代码拆解：dsh-superagent-prototype

下文的路径都指原型仓库的提交 `6337fc8`。例如 `src/policy.ts:191` 对应 <https://github.com/deepseek-ai/dsh-superagent-prototype/blob/6337fc823ce20251c8de8085948165c81aaa701e/src/policy.ts#L191>。

### 3.1 概况

| 项 | 数值 |
|---|---|
| 提交 | 1 次：`6337fc8`，2026-10-31，作者署名「DeepSeek Harness Team」 |
| GitHub | 5,942 star，568 fork（2026-11-01） |
| 源码 | 15,205 行 TS/TSX，110 个文件。其中领域核心 2,088 行，GitHub 和飞书来源 2,059 行，管理面板 4,419 行 |
| 测试 | 14,960 行，59 个 spec 文件，和源码约 1:1 |
| 文档和提示词 | 9 篇编号文档共 1,973 行；15 个提示词文件约 7,500 词 |
| 运行形态 | DSH 插件 bundle：`cordis.patch.yml` 插入 3 个插件和 2 个 agent preset。单机运行，状态存在一个 SQLite 文件里 |
| 事件来源 | GitHub：通知、订阅的 Issue 和 PR、CI 变化，可选择接收全部消息。飞书：可选，默认关闭 |
| 自述 | 大部分工程由 coding agent 完成；属于研究代码，不能直接用于生产 |

### 3.2 领域模型

```
Source ─Trigger─▶ Thread ─(按批)─▶ Eval = gate(代码) + 研判(模型) ─accept─▶ Ticket + Mandate ─Assignment─▶ Worker Session + 独立目录
                    │                                                         ▲                                    │
                    └─ Subscription 扇出：人在工单线程里的后续消息直接交给 Worker，不经过 Eval ─┘                          Report
                                                                                                                   ▼
                                                                                  主 Session（Router）─Channel─▶ 回到原对话 / PR
```

`docs/001-domain.md` 里有 mermaid 原图。几条关键规则：

- 同一个 Thread 同时只跑一个 Eval，期间到达的 Trigger 并入下一批。设计文说的「Router 批处理」就是这个。
- 同一个 Trigger 被 accept 两次，只产生一个 Ticket（幂等）。
- Ticket 有自己的状态，不跟随 Worker Session，所以可以交接、恢复，也可以由多个 Executor 执行。
- `docs/001` 里的「拒绝的概念」一表很值得看。例如：不做跨来源的身份映射；Eval 不自己排延时 Trigger，需要等待的条件本身做成一个 Source；策略变更不取消正在跑的工单；不从报告的自由文本里推断「在等人」。

### 3.3 设计文里的概念在代码中落地了多少

| 设计文概念 | 原型里的实现 | 位置 |
|---|---|---|
| Router 层 | 主 Session，14 个工具，用于路由、读取、回复、研判和改设置。它的 preset 对文件系统只读，但**带 bash**（5 秒超时）。「不读代码」只是提示词里的规则，没有在工具层面限制 | `src/app/tools/main.ts`、`cordis.patch.yml` |
| Worker 层 | 每个 Ticket 一个 `worker-<id>` Session 加一个独立目录，目录通常由 prepare 命令用 `git worktree` 创建。另有 6 个专用工具：ack、report、handback、follow、unrelated、read | `src/executor/dsh.ts`、`src/app/tools/executor.ts` |
| 两级过滤 | gate 是纯代码；模型研判之后，结果还要过 schema 校验 | `src/policy.ts:191`、`src/policy.ts:40` |
| Thread 订阅直通 | Intake 把 Trigger 扇出给订阅了它的 Ticket；人发的后续消息由对应的 Worker 判断 | `src/core/intake.ts` |
| Router 批处理 | Eval 串行执行；Worker 的报告在 Router 的下一步一起注入 | `src/core/evals.ts`、`src/app/notices.ts` |
| Inline & Spill | 研判卡片里，Trigger 正文最多 4,000 字符；Thread 窗口取前 5 条加触发点前后各 5 条（单条 400 或 1,200 字符）；其他消息只给 50 字符的存根，可按 id 展开 | `src/app/assessment.ts:37`、`src/sources/admission-context.ts` |
| 链式召回 | 页面上出现的每个 id 都能用 `orchestrator_read` 读取，Session 可以沿 id 一跳一跳往回找 | `src/app/resources.ts` |
| 异步 ReAct | 部分实现：前台命令超过 5 秒转为后台 job，job 结束时自动唤醒 Session；每轮结束后，后台任务最多再跑 30 分钟 | `cordis.patch.yml`、`src/executor/background.ts` |
| 投机执行 | 没有。最接近的是 `observe` 模式下记录「本来会接」，相当于影子模式 | — |
| Watchdog | 没有（全仓库搜不到） | — |
| 存算分离 / BYOM | 没有：单机进程，本地 SQLite，本地 Worker | `src/app/store.ts` |

### 3.4 授权：最值得借鉴的部分

**准入由代码判定。** 每条 Trigger 先过确定性的 gate，结果分三种：

- `explicit`：白名单里的人 @ 了 agent，或者在已有工单的线程上跟进。
- `inferred`：没有 @ 的消息、白名单外的人发的消息、名单内机器人发的消息。这类需要按规则研判。
- 不准入：其他机器人的消息、准入表设为忽略的消息，以及 `off` 模式下所有 inferred 消息。

每个来源有一张准入表，按「在不在名单」和「有没有 @」组合配置。机器人只能产生 inferred 候选，不能下明确请求，也不能叫停工单。

**模型研判，代码校验。** `assessmentOutcome`（`src/policy.ts:40`）对模型的研判有四条硬要求：

- 每条启用的规则恰好判一次。
- 方向必须满足三点：明确要求实现；目标已经指定，或有既有标准可依；不需要设计决策。
- 至少命中一条规则。
- 给出意图证据（引用原话）。

不满足就降级为 ignore。在 `auto` 以外的模式下，accept 只记录为「本来会接」。

**默认只有两条规则**（`src/policy.ts:62`）：`copy` 处理错别字、措辞、标点和翻译；`visual` 处理有明确目标的局部间距、对齐和溢出。提示词花了大量篇幅防止模型把「这个按钮好丑」当成重新设计的授权。

**授权（Mandate）一经创建不可修改。** 它记录策略版本、命中的规则、发送者身份和接收范围。只有三种操作会影响它：accept 创建它，人叫停时撤回它，重新校验失败时吊销它。策略变更时：

- 进行中的 Eval 作废，重新排队；
- 只通知受影响的工单；
- 不取消正在跑的工作；
- 工单恢复前，按当前策略重新校验。

**接单模式**有三档：`off`、`observe`（默认）、`auto`。Eval 默认 120 秒超时，连续两次超时会重新排队，第三次记为 `unavailable`。

### 3.5 执行与可靠性

- **领域核心同步执行，每个命令一个事务。** `Engine` 是 Trigger、Thread、Decision、Ticket、Assignment 唯一的写入方；`src/core` 不做任何 I/O。
- **副作用先落盘再执行（outbox 模式）。** 转发给 Router、启动 Worker、投递输入、回复消息，都先写进持久化的 effect 队列再执行。失败后按指数退避重试，重启也不会丢。
- **并发槽位。** 默认 3 个 Worker（`src/policy.ts:93`，上限 16），另留 1 个槽专给人发来的消息，这样新工作不会挡住人继续推进已有工单（`src/executor/dsh.ts:169`）。
- **重启恢复。** 被中断的 Assignment 带着恢复说明重新启动。说明要求 Worker 在重复任何外部操作之前，先检查目录、git 历史和 GitHub。
- **Router 不许等待。** `noWait` 守卫拒绝 Router 执行 `sleep`、超过 30 秒的命令和超过 5 秒的 job 等待，因为报告和新候选会自己送到（`src/app/sessions.ts:77`）。

### 3.6 提示词怎样把「少打扰」写死

- IM 回复上限 100 词，超长消息由传输层直接拒绝。中文按分词计数，不允许用分号长句或拆成多条消息来绕过。
- 未完成的 PR 不算里程碑，不对外发。只有三种情况才发消息：结果可以用了、需要人做决定、确定做不了。
- 发到 GitHub 的每条内容，第一行都必须是「This is by an automated agent, not a human.」
- 每个 PR 上，每个自动评审机器人的意见最多处理 3 轮。
- 不用 agent 自己的账号开启 auto-merge。只有交付要求里指定了另一个账号的 CLI，并且改动少于 200 行、审批覆盖了最新 commit，才会开启。
- 给人类评审者署名的做法相当激进：首次评估某位评审者的反馈时，用 `git filter-branch` 改写 PR 分支，给每个 commit 加上 `Co-authored-by`，再用 `--force-with-lease` 推送。
- 如果有人自己的 PR 已经包含了本工单的代码，agent 立即关掉自己的 PR。
- **提示词有测试把关。** `tests/model-surface.spec.ts:40` 给每个角色的「提示词 + 工具描述」设了字符预算：Router 不超过 59,000，Worker 不超过 40,500。预算只许调低，调高要在 commit 里说明理由。`tests/cards.spec.ts` 逐个文件检查内置提示词有没有写明必须写的规则。这个做法值得借鉴。

### 3.7 安全

- [`src/app/sessions.ts:152`](https://github.com/deepseek-ai/dsh-superagent-prototype/blob/6337fc823ce20251c8de8085948165c81aaa701e/src/app/sessions.ts#L152)：所有托管 Session，包括 Router 和 Worker，都设为 `danger-full-access`。
- Worker 以运维者的系统用户身份免审批执行命令。命令黑名单只是 shim，用绝对路径就能绕过。文档原话是：这些包装和目录只是默认设置，不是安全边界。
- Router 同样有 bash。设计文说 Router 可以不给 Bash，原型没有这样做。
- 架构文档的未解问题里仍有一条：Trigger 的哪些字段可信（例如来源给出的发送者、线程归属），哪些不可信（例如正文和引用）。
- 结论：只适合私有仓库，白名单要短，并从 `observe` 模式起步。对照 DSH 主线的 RFC：那里的事件轮次只给只读工具，只有 `automation_notify` 能触达用户。

## 4. 底座 DeepSeek Harness 中相关的部分

| 能力 | 包 | 和 Super-agent 的关系 |
|---|---|---|
| 后台作业 | `jobs`、`tool-jobs` | 作业结束后，结果直接投递到会话里，不需要轮询。原型的「超过 5 秒转后台」就靠它实现 |
| 溢出存储 | `spill`、`spill-policy` | 工具结果过长时存到外部，上下文里只留预览和定位符。这是 Inline & Spill 的基础设施 |
| 子 agent | `subagent-*` | 支持进程内新建和 fork、ACP、**真实的 Codex（通过 app-server 协议）和真实的 Claude Code（通过 Agent SDK）**，以及另一个 DSH 实例 |
| 生态桥接（实验） | `hooks-claude-code`、`hooks-codex`、`claude-code-mods`、pi 扩展桥（proposed） | 可以直接加载 Claude Code 的 hooks.json 和 mods |
| 长任务（实验） | `agent-team`、`tool-ralph`、`goal`、`auto-review`、`webhook-github` | 依次为：团队收件箱加共享任务板；ralph 循环；持久目标；工具执行前的自动审查；GitHub webhook |
| 模型 | `llm-deepseek*`、`llm-pi-ai` | 原生只接 DeepSeek（内置目录为 `deepseek-flash`、`deepseek-v4-pro`）。OpenAI、Anthropic 等通过 pi-ai 适配器接入 |

主仓库的 [`.agents/notes/proposed/feature/2026-10-29-event-triggered-automation-tasks.zh.md`](https://github.com/deepseek-ai/deepseek-harness/blob/ce7073d51254c26cd63b22a60835b8f948deb447/.agents/notes/proposed/feature/2026-10-29-event-triggered-automation-tasks.zh.md)（10 月 29 日，proposed）是把 Super-agent 思路做成正式功能的方案：

- **分三层。** 来源提供方（代码）→ 事件触发器（代码：比较新旧状态、匹配条件、合并同一对象的多次变化）→ agent（模型，只处理已投递的候选事件）。
- **事件轮次只读。** 没有 shell、写文件、网页抓取和 MCP。新增的 `automation_notify` 工具会生成操作卡片、未读标记和桌面通知。用户接受卡片后，建议的操作才会在创建该任务的会话里、按那个会话的权限执行。
- **交付顺序。** 先把任务和会话解耦；再上线触发层，时间作为第一个来源；然后接 GitHub 来源（PR 检查、评审、评论）；最后是邮件、IM，以及 DSH 自身状态作为来源。

也就是说，**原型是激进的研究版：全权限，自动开 PR。主线是保守的产品版：只读判断，人点头之后才执行。**

## 5. 局限与注意事项

1. 没有找到设计文的官方原文，相关内容都来自转述。缓存读 2,323 万和 3,177 万这两个数出现在 Offloop 英译和什么值得买评论两处，但都没说明是单次运行的量还是 5 次合计。
2. 原型只做了静态阅读，没有实际运行。
3. DeepSeek 价格取自官方价目页。该页没写生效日期，并声明保留调价权。高峰时段不含中国法定假日。
4. 第 1.3 节的成本表只比价格结构，不比能力。不同模型完成同一个任务用的 token 数可能差很多。
5. star 数来自 GitHub 搜索 API，2026-11-01 查询。

## 来源

- 原型仓库：<https://github.com/deepseek-ai/dsh-superagent-prototype>（提交 `6337fc8`）
- DeepSeek Harness：<https://github.com/deepseek-ai/deepseek-harness>（master `ce7073d`）；产品页：<https://www.deepseek.com/en/harness/>
- DeepSeek API 价格：<https://api-docs.deepseek.com/quick_start/pricing>
- Anthropic Agent SDK 关于 claude.ai 登录的说明：<https://code.claude.com/docs/en/agent-sdk/overview>
- 设计文转述：IT之家 <https://www.ithome.com/1/020/615.htm>；InfoQ（新浪转载）<https://finance.sina.com.cn/roll/2026-10-31/doc-inixcwwm7186478.shtml>；Offloop 英译（第三方）<https://offloop.org/learn/deepseek-super-agent-design>
- 评论：什么值得买《怎么评 DeepSeek 的 super-agent 构想？方向是共识，交付待验证》<https://post.smzdm.com/p/ad7zx8vd/>；Developers Digest <https://www.developersdigest.tech/blog/deepseek-dsh-superagent-router-worker-always-on-agent-2026>；Startup Fortune <https://startupfortune.com/deepseek-open-sources-the-architecture-behind-its-super-agent/>
- DeepSeek 无官方 coding 订阅（第三方汇总）：<https://aitoolsatlas.ai/tools/deepseek/pricing>
- 之前的调研：[subscription-vs-api-2026-10.md](subscription-vs-api-2026-10.md)
