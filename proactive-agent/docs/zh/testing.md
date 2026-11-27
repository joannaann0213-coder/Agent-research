# 测试

[English](../en/testing.md)

设计里的每一项验收（F1 到 F10）和每一个场景（A 到 E）都有自动化测试。数据库测试跑在真实的 PostgreSQL 上，端到端测试走真实的
HTTP，用的就是生产方或运行时会用的那个客户端。

## 运行测试

需要 JDK 21、Maven 3.9 和 PostgreSQL 16。

```bash
cd proactive-agent
# 启动 PostgreSQL，创建角色 "inbox" 和数据库 "inbox_test"。
scripts/pg-local.sh
# 先跑单元测试，再跑集成测试和端到端测试。
mvn verify
```

整个过程大约一分钟。如果要用别的数据库，设置下面这些变量。测试会清空那个数据库里收件箱的每一张表，所以绝不要把它们指向存有数据的库。

| 变量 | 默认值 |
| --- | --- |
| `INBOX_TEST_JDBC_URL` | `jdbc:postgresql://127.0.0.1:5432/inbox_test` |
| `INBOX_TEST_DB_USER` | `inbox` |
| `INBOX_TEST_DB_PASSWORD` | `inbox` |

整个流程里没有 Docker。测试只需要一个连得上的 PostgreSQL，别无他求。

```bash
# 只跑单元测试，不需要数据库。
mvn -pl inbox-server -am test
# 单独跑一个集成测试类。
mvn -pl inbox-server -am verify -Dit.test=F5BatchingIT \
    -Dfailsafe.failIfNoSpecifiedTests=false -Dsurefire.failIfNoSpecifiedTests=false
```

## 测试的种类

| 种类 | 命名 | 运行方式 | 是什么 |
| --- | --- | --- | --- |
| 单元 | `*Test` | Surefire，不需要数据库 | 纯逻辑：领取规划器、cron 排期、回收闸门、规范化 JSON、签名、桩发件箱 |
| 集成 | `*IT` | Failsafe，PostgreSQL | 在随机端口上启动的真实服务，经由 HTTP 调用 |
| 端到端 | `*E2E` | Failsafe，PostgreSQL | 设计里的一个场景，由 `ScriptedWorker` 代替 agent |
| 测量 | `WriteLatencyPerf` | 只在需要时运行 | 对照设计目标测量写入延迟 |
| 文档 | `DocsParityTest` | Surefire | 中英文文档结构一致，链接可用 |
| 契约 | `OpenApiContractIT`、`OpenApiSchemaTest` | Failsafe 和 Surefire | 用运行中的服务和传输类型核对 OpenAPI 文档 |

## 测试如何保持确定性

- **时钟只在测试拨动它时才走。** 服务从一个注入的时钟取时间，并把它传进 SQL，所以租约、静默窗口、退避和定时器的测试靠的是拨动
  `MutableClock`，而不是 sleep。
- **后台循环是关闭的。** 租约清扫器、挂起清扫器和定时器调度器都由测试用 `runOnce()` 手动驱动，一次一轮。
- **每个测试都从空表开始。**
- **每个数据库测试结束后都会检查不变量**，不管这个测试本身测的是什么（见下文）。
- **往返次数是被计数的。** `StatementCounter` 统计 SQL 语句数，“写入的正常路径是一条语句”就是这样断言的。

少数测试有意使用真实时间：长轮询和唤醒的测试、50 线程的单飞测试，以及场景 D，它会杀掉一个真实的进程。

### 每个测试之后检查的不变量

| 检查 | 保护什么 |
| --- | --- |
| 状态为 claimed 的事件属于一个 active 或挂起的 run | 没有事件卡在一个已死的持有者名下 |
| 绑定在某个 run 上的 pending 事件属于一个排队中的 run | 重试不会丢掉它的事件 |
| 恰好是已结束的事件带有 `finished_at` | 状态与时间戳一致 |
| 每条事件的历史都是一条从“无”开始、没有断点的链 | 每一次迁移都是合法的 |
| 最后一条迁移记录就是事件的当前状态 | 状态与历史一致 |
| 同一个收件箱里每个合并键最多一个 active run | 单飞 |
| 数据库没有报告新的死锁 | 加锁顺序成立 |

## API 规范

OpenAPI 文档会拿服务本身来核对，所以它不可能描述一套并不存在的 API。改了 API 却没改文档，构建就会失败。

| 检查 | 在哪里 | 什么情况会让构建失败 |
| --- | --- | --- |
| 流量 | 每个集成测试之后 | 响应不符合它的 schema、响应里有 schema 没列出的字段、返回了该操作没列出的状态码、被接受的请求不符合文档 |
| 覆盖 | 最后运行的 `OpenApiContractIT` | 有操作没被任何测试调用；有列出的状态码没被任何测试触发；控制器有文档里没有的路径，或者反过来 |
| 清单 | `OpenApiSchemaTest` | Schema 的字段与 `inbox-api` 里对应的传输类型不一致；枚举的取值与代码不一致 |
| 自洽 | `OpenApiSchemaTest` | 引用了不存在的组件、有组件没被用到、有操作缺少 `operationId`、有示例不符合它自己的 schema |

有两处是有意放宽的。被服务拒绝的请求不拿文档来核对，因为这类测试本来就是要发一个错误的请求；它的响应仍然要核对。覆盖检查也不要求每个操作都出现
400、401 和 403，因为它们由公共层产生。

这些检查放在 `openapi-testkit` 模块里。它对收件箱一无所知，本仓库的其他服务也会复用它。

## 验收项

| ID | 验收项 | 测试 |
| --- | --- | --- |
| F1 | 没有 worker 在运行、唤醒通道损坏时，写入照样被接收；重试三次得到一条事件；正常路径是一条 SQL 语句；数据库故障是显式的 503 | `F1WriteApiIT`, `F1WakeFailureIT`, `AuthIT`, `SeqCommitOrderIT` |
| F2 | 同一个 webhook 发 10 次，无论顺序发还是 10 个线程同时发，都是 1 条事件、1 个 run、1 次回复；payload 变了就是 409；键顺序不同的 JSON 是同一条事件；缺少键时会算指纹 | `F2DedupeIT`, `ScenarioAE2E` |
| F3 | 每一次迁移都记录谁、何时、从哪、到哪、为什么；nack 带退避重试，最终落到 `dead`；worker 被杀之后同一个 run 再次下发，没有事件丢失；带过期 epoch 的调用得到 409 | `F3StateMachineIT`, `EventImmutabilityIT`, `ScenarioDE2E` |
| F4 | 未来的事件只以定时器的形式存在；有多个调度器、中间还发生一次回滚时，每个定时器恰好触发一次；故障之后 cron 只触发一条补触发事件 | `F4TimerIT`, `CronScheduleTest`, `ScenarioEE2E` |
| F5 | 连续五条消息是一个 run、五条事件、一次回复；一批以 20 条事件和 256 KB 封顶；`max_wait` 给细水长流设了上界 | `F5BatchingIT`, `ClaimPlannerTest`, `ScenarioBE2E` |
| F6 | 50 个线程在同一个收件箱上领取并 ack：active run 从不超过一个，头部序号严格递增，每条事件恰好完成一次；在严格模式下再跑一遍 | `F6SingleFlightIT` |
| F7 | 一个用户的长任务不会拖慢另一个用户；后台 run 不会挡住同一个用户的新消息；同一个合并键绝不会同时跑两次 | `F7LaneIT` |
| F8 | 关掉唤醒提示，事件仍会在 10 秒内被领取；等待中的领取会被一次写入唤醒；租约最多续到 30 分钟，然后被回收 | `F8WakeIT`, `F8WakeDisabledIT`, `ReclaimGateTest` |
| F9 | Steer、followup、collect 和 interrupt 各自的行为符合规定；被忽略的打断退化为 followup；随机交错的执行顺序下不丢任何事件；挂起会释放收件箱，答复恢复的是同一个 run | `F9DeliveryModesIT`, `F9RandomInterleavingsIT`, `SuspendResumeIT`, `ScenarioCE2E` |
| F10 | 带 `force` 和不带 `force` 的重放；审计记录里有运维人员和原因；一次调用返回一条事件的完整历史；别的租户的对象查不到 | `F10ReplayAuditIT`, `InboxAdminIT`, `TenantIsolationIT`, `MetricsIT` |
| F11 | 配额、背压与熔断 | 未实现（路线图 M2 阶段），所以没有测试 |

## 场景

| 场景 | 测试 | 证明了什么 |
| --- | --- | --- |
| A. 一条业务告警，一次回复 | `ScenarioAE2E` | 写入、唤醒、领取、靠心跳续租使 run 运行超过 60 秒、一条发件箱消息、ack；写入、领取和发件箱三处是同一个 `trace_id` |
| B. 连续五条用户消息 | `ScenarioBE2E` | 一个 run，一次回复，并且反映了全部五条 |
| C. 挂起等待审批 | `ScenarioCE2E` | Run 发出请求、挂起，并在获批后继续；拒绝也是一种答复；没有答复时 run 静默结束；在变更已经生效之后崩溃，既不会再生效一次，也不会再通知一次 |
| D. Worker 崩溃 | `ScenarioDE2E` | 一个独立 JVM 里的 worker 在 run 进行到一半时被强制杀掉。租约过期后，同一个 `run_id` 带着更高的 epoch 再次下发，而回复只发出一次 |
| E. 定时跟进 | `ScenarioEE2E` | 两小时后 agent 发出提醒，或者因为回复已经到了而保持静默 |

## 不变量

| 不变量 | 测试 |
| --- | --- |
| I1 单一写入口，只追加 | `EventImmutabilityIT`, `F1WriteApiIT` |
| I2 每个幂等键一条事件 | `F2DedupeIT` |
| I3 单飞 | `F6SingleFlightIT`, `F7LaneIT`，以及每个测试之后的检查 |
| I4 Run 与事件不是一一对应 | `F5BatchingIT` |
| I5 至少一次，消费方幂等 | `F3StateMachineIT`, `ScenarioDE2E`, `OutboxTest` |
| I6 未来的时间绝不进入就绪队列 | `F4TimerIT` |
| I7 事件不可变，迁移可审计 | `EventImmutabilityIT`, `F3StateMachineIT`，以及每个测试之后的检查 |
| I8 租户隔离 | `TenantIsolationIT`, `AuthIT` |

## 写入延迟

设计要求写入的 p99 不超过 50 毫秒。`WriteLatencyPerf` 用来测量它。它不属于常规构建，因为结果取决于机器，尤其取决于磁盘确认一次提交有多快。

```bash
mvn -pl inbox-server -am verify -Dit.test=WriteLatencyPerf \
    -Dfailsafe.failIfNoSpecifiedTests=false -Dsurefire.failIfNoSpecifiedTests=false
```

它会打印以 `WRITE LATENCY` 开头的行。时间由客户端经回环 HTTP 测得，所以包含了签名、HTTP 往返和两端的 JSON 处理。开发虚拟机上的结果见
[事件收件箱设计](event-inbox-design.md#目标与实测)。

## 手动试用

README 里的快速开始就是手动冒烟测试：启动服务，用 `scripts/inbox-curl.sh` 写入一条带签名的事件，让 `scripts/run-worker.sh` 领取并
ack 它，再把这条事件的历史读回来。

## 对真实服务的验证

agent 服务的测试跑在 Claude Managed Agents 的替身上。另有三个脚本带着 API key 对真实服务运行。最近一次运行是 2026 年 11 月，模型是 `claude-opus-5-5`。

| 脚本 | 检查什么 | 结果 |
| --- | --- | --- |
| `scripts/cma-live-check.sh` | agent 服务所依赖、而文档没有说清的 22 项平台行为 | 当时检查的 21 项：全部与假设一致。第 22 项（session 会说明每次模型请求用了多少）是之后加的 |
| `scripts/agent-live-smoke.sh` | 一段会话、一个变成通知的事件、一个由用户回答的提问 | 全部通过 |
| `scripts/main-agent-live.sh` | 主 agent 和 worker：用户要的任务、应用交给 worker 的事件和定时计划、主 agent 提议而用户确认的工作、主动程度，以及汇报还在路上时用户发来消息 | 全部通过 |

平台探查第一次运行时，发现替身有四处和真实服务不一样。agent 服务没有依赖其中任何一处。替身和它的契约测试现在与真实服务一致：

- 流里一条消息只出现一次：在 session 接手它的时候，并且在 session 说自己开始运行之后。存储的事件列表里，消息也在这个位置。
- 对一个已经不在等待的调用作答（它已被回答，或者它那一轮被打断了），会被接受，但什么也不改变。如果是一次确认，session
  会再显示一次运行中和空闲；如果是自定义工具的结果，流里什么都不出现。
- 打断时，还在等待的调用会先得到一个错误作为结果，然后 session 才进入空闲。

接上模型之后，主 agent 的一次回复在服务内部耗时 5 到 6 秒，用户发出消息后 7 到 8 秒收到，其中包含收件箱的静默窗口。后台回合耗时 5 到 7 秒，worker 的回合大约 5
秒。两次运行里，后台回合给用户的消息让路了四次：三次是用户的回合已经带走了它的新情况，它没有经过模型就结束；一次是它重新开始并重复了自己的调用，这些调用没有一个被执行两次。

没有对真实服务跑过的：worker 等待审批的情形。它需要一个平台访问得到的 MCP 服务器。token 的计数也没有，它是后来加的：那几次运行表明，session
在每次模型请求之后都会存一条带用量的事件，计数读的就是它，官方 SDK 里也有这种事件的类型；但数字本身只在替身上核对过。

## 没有测试什么

- 路线图的 M2 阶段（F11）没有实现。
- 没有在 Docker 里运行过，也没有在多于一个服务实例上运行过。后台循环在设计上可以安全地跑在每个实例上。测试会同时运行多个定时器调度器，并让挂起清扫器与一个正在领取的
  worker 竞争；租约清扫器只与 worker 的调用逐步交错执行。这些都发生在一个进程之内。
- 没有生产规模的压测。50 线程的单飞测试和延迟测量是施加过的最大负载。
