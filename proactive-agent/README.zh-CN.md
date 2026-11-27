# 主动式 Agent：事件收件箱与 Agent 服务

[English](README.md)

事件收件箱（Event Inbox）是主动式 agent 的入站关口。每一个触发（webhook、定时器、用户消息、审批回复）都通过同一个写入
API 追加到每个用户自己的收件箱里；运行时从中取走工作的方式只有一种：把一批就绪事件领取为一个 run。

本仓库实现了收件箱路线图的 **M0 和 M1** 两个阶段，形态是一个基于 PostgreSQL 的 Spring Boot 服务；在它之上是 **Agent
服务**，通过 [Agent API](docs/zh/agent-api.md) 把主动式 agent 开放给应用，并让它跑在 Claude Managed Agents 上。一个 agent
可以是用户的[主 agent](docs/zh/main-agent.md)：关于这个用户的一切都来到它这里，耗时的工作由它交给 worker agent。多 agent
层的第三层和长期记忆在[架构文档](docs/zh/architecture.md)中有描述，但不在这里实现。单独使用收件箱时，用一个脚本化的 worker 代替 agent，使收件箱的契约可以端到端地跑通。

## 它保证什么

- **单一写入口，只追加。** 写入在一条 SQL 语句内完成鉴权、校验、去重和追加。写入从不启动 run，也从不调用模型，所以
  `202 Accepted` 不依赖任何下游。
- **幂等。** 每个 `(inbox, source_event_id)` 只有一条事件。重复写入返回先前那条事件；同一个键配上不同的 payload 则返回 `409`。
- **单飞。** 同一个收件箱里，每个合并键最多一个 active run，每条 lane 有并发上限，还可以设一个收件箱级的总上限。序号顺序等于提交顺序。
- **合批。** 同一个合并键的事件，只要在静默窗口内到达，就合成一个 run。
- **至少一次，可重放。** 每个 run 持有一份租约。租约过期后，同一个 run 连同原来的 id 和原来的事件会再次下发，所以下游的发件箱可以按
  `run_id + sequence` 去重。
- **不会卡死。** 租约会过期，重试最终落到 `dead`，挂起的 run 必须带超时。
- **可审计。** 每条事件的每一次状态变化都有记录：谁、何时、从哪个状态、到哪个状态、为什么。

## 快速开始

需要 JDK 21、Maven 3.9 和 PostgreSQL 16。

```bash
cd proactive-agent

# 1. 数据库：启动本地集群，创建角色 "inbox" 以及数据库 inbox_dev / inbox_test。
#    如果要用别的 PostgreSQL，跳过这一步：为服务设置 INBOX_JDBC_URL、INBOX_DB_USER 和 INBOX_DB_PASSWORD，
#    为测试设置 INBOX_TEST_JDBC_URL、INBOX_TEST_DB_USER 和 INBOX_TEST_DB_PASSWORD。
scripts/pg-local.sh

# 2. 构建、运行全部测试，并把各模块安装到本地 Maven 仓库，scripts/run-worker.sh 从那里找到它们（大约一分钟）。
mvn install

# 3. 启动服务，配置三个调用方：一个生产方、运行时和一个运维人员。
export INBOX_AUTH_CLIENTS_0_ID=order-bot  INBOX_AUTH_CLIENTS_0_SECRET=order-bot-secret \
       INBOX_AUTH_CLIENTS_0_TENANT=acme   INBOX_AUTH_CLIENTS_0_ROLES=PRODUCER
export INBOX_AUTH_CLIENTS_1_ID=runtime-1  INBOX_AUTH_CLIENTS_1_SECRET=runtime-secret \
       INBOX_AUTH_CLIENTS_1_ROLES=RUNTIME
export INBOX_AUTH_CLIENTS_2_ID=ops        INBOX_AUTH_CLIENTS_2_SECRET=ops-secret \
       INBOX_AUTH_CLIENTS_2_ROLES=OPERATOR
java -jar inbox-server/target/inbox-server-0.1.0-SNAPSHOT-exec.jar
```

在另一个终端里写入一条事件。`scripts/inbox-curl.sh` 会替你给请求签名。

```bash
scripts/inbox-curl.sh order-bot order-bot-secret POST /v1/inbox/u42/events \
  '{"source":"biz_webhook","source_event_id":"order-1234-v7","type":"order.paid.v1",
    "merge_key":"order:1234","payload":{"order_id":"1234","amount":299}}'
# {"event_id":"01M7WZBC3TXC7X5N7KVK7Q9WXM","deduped":false,"status":"pending"}
# [HTTP 202]
```

再发一次，得到的还是同一条事件，并带有 `"deduped":true`。接下来让一个 worker 处理它。脚本化的 worker 会领取一个
run，往 `target/outbox/` 里写一条回复，然后 ack。

```bash
scripts/run-worker.sh runtime-1 runtime-secret --worker-id worker-1 --max-runs 1
cat target/outbox/*.json
# {"run_id":"01M7WZBNDQ72G3BFC6VDVPF8RA","sequence":1,"channel":"im",...,
#  "text":"handled 1 event(s): order.paid.v1 order-1234-v7"}
```

然后把这条事件连同它的完整历史读回来：

```bash
scripts/inbox-curl.sh order-bot order-bot-secret GET /v1/inbox/u42/events/01M7WZBC3TXC7X5N7KVK7Q9WXM
# {"event":{"event_id":"01M7WZBC3TXC7X5N7KVK7Q9WXM","status":"done",...},
#  "transitions":[{"to_status":"pending","reason":"accepted","actor":"client:order-bot",...},
#    {"from_status":"pending","to_status":"claimed","reason":"claim","actor":"worker:worker-1",...},
#    {"from_status":"claimed","to_status":"done","reason":"ack","actor":"worker:worker-1",...}]}
```

健康检查在 `/actuator/health`，指标在 `/actuator/prometheus`。

## Agent 服务

最快的运行方式是 Docker。下面的命令在一台机器上运行 PostgreSQL、收件箱和 agent 服务，使用演示模式：由脚本扮演 agent，不需要 Anthropic
key；[教程](docs/zh/tutorial.md)从这里继续讲如何接入应用、如何上线。

```bash
docker compose up -d --build --wait
examples/agent-api/quickstart.sh
```

不用 Docker 时，agent 服务需要收件箱，并在收件箱里为它配置一个 client：

```bash
# 终端 1：收件箱，为 agent 服务配置一个 client（角色 RUNTIME 和 PRODUCER，不属于任何租户）。
export INBOX_AUTH_CLIENTS_0_ID=agent-server INBOX_AUTH_CLIENTS_0_SECRET=agent-inbox-secret \
       INBOX_AUTH_CLIENTS_0_ROLES=RUNTIME,PRODUCER
java -jar inbox-server/target/inbox-server-0.1.0-SNAPSHOT-exec.jar

# 终端 2：agent 服务，演示模式。
export AGENT_ADMIN_KEYS_0=change-me-admin-key-0123456789abcdef \
       AGENT_TOKENS_SECRETS_0=change-me-token-secret-0123456789abcdef \
       AGENT_INBOX_SECRET=agent-inbox-secret AGENT_CMA_MODE=demo
java -jar agent-server/target/agent-server-0.1.0-SNAPSHOT-exec.jar

# 终端 3：一个应用、一个用户、一段会话，逐步演示。
examples/agent-api/quickstart.sh
```

设置 `AGENT_CMA_MODE=live` 和 `ANTHROPIC_API_KEY` 后，agent 就跑在 Claude Managed Agents 上；先用 `scripts/cma-live-check.sh`
检查账号。[Agent 服务](docs/zh/agent-service.md)说明服务如何工作，以及如何在生产环境运行。

## 模块

| 模块 | 内容 |
| --- | --- |
| `inbox-api` | 服务端与客户端共用的传输类型、规范化 JSON、请求签名 |
| `inbox-server` | 服务本身：写入 API、领取与 run 生命周期、定时器、重放、指标 |
| `inbox-client` | 纯 Java 客户端、`ScriptedWorker`、桩发件箱、命令行 worker |
| `openapi-testkit` | 测试支持：用服务的 OpenAPI 文档核对测试中的 HTTP 流量 |
| `agent-cma` | 通往 Claude Managed Agents 的端口：agent 服务使用的接口、基于官方 Java SDK 的适配器，以及一个可脚本化的进程内替身 |
| `agent-api` | Agent API 的传输类型，agent 服务与它的客户端共用 |
| `agent-server` | Agent 服务：Agent API、在 Claude Managed Agents 上运行的回合、主 agent 和它的 worker 的任务、动作、定时计划、更新、webhook 和凭据 |

## 文档

| 文档 | 内容 |
| --- | --- |
| [架构](docs/zh/architecture.md) | 主动式 agent 的整体，以及收件箱在其中的位置 |
| [事件收件箱设计](docs/zh/event-inbox-design.md) | 收件箱是怎么实现的：不变量、数据模型、领取规则、决策 |
| [API](docs/zh/api.md) | 全部接口、请求签名、错误码、运行时契约，以及从哪里获取 OpenAPI 文档 |
| [接入指南](docs/zh/integration.md) | 一步步说明如何接入 connector（包括基于 MCP 的第三方系统）和 agent，附 Java 和 Python 示例 |
| [测试](docs/zh/testing.md) | 怎么运行测试，以及每一项验收由哪个测试证明 |
| [Agent 服务](docs/zh/agent-service.md) | Agent 服务如何工作、出了问题怎么办、怎样运行 |
| [Agent API](docs/zh/agent-api.md) | 写给应用的指南：概念、鉴权、会话、事件、动作、定时计划、更新、凭据 |
| [主 agent 与 worker](docs/zh/main-agent.md) | 得知用户一切并把工作交给 worker agent 的那个 agent：回顾、决策及其理由、它如何工作，以及谁让任务开始：用户的确认或长期授权 |
| [教程](docs/zh/tutorial.md) | 用 Docker 运行服务，一步步接入应用：后端、用户端、业务事件、审批、真实模式、上线 |

## 配置

所有配置都在 `inbox.*` 之下，都可以用环境变量设置（`inbox.lease.duration` 对应 `INBOX_LEASE_DURATION`）。默认值就是设计里的数字。

| 属性 | 默认值 | 含义 |
| --- | --- | --- |
| `INBOX_JDBC_URL`, `INBOX_DB_USER`, `INBOX_DB_PASSWORD` | 本地 `inbox_dev` | 数据库 |
| `INBOX_PORT` | `8080` | HTTP 端口 |
| `inbox.auth.clients[n].id / secret / tenant / roles` | 无 | 调用方。角色：`PRODUCER`、`RUNTIME`、`OPERATOR`。不填租户即为平台级 |
| `inbox.auth.clock-skew` | `300s` | `X-Timestamp` 与服务器时钟之间允许的最大偏差 |
| `inbox.payload.max-bytes` | `65536` | 事件 payload 的大小上限 |
| `inbox.lease.duration` / `max-duration` | `60s` / `30m` | 一次心跳续出的租约时长；单次授予租约的上限 |
| `inbox.retry.max-attempts` | `5` | 尝试多少次之后 run 进入 `dead` |
| `inbox.retry.backoff-base` / `backoff-cap` | `2s` / `5m` | 第 n 次尝试之后的退避为 base^n，有封顶 |
| `inbox.retry.max-defer` | `30m` | run 创建后多长时间内，worker 可以退回它而不计尝试次数 |
| `inbox.batch.max-events` / `max-bytes` | `20` / `262144` | 合批上限 |
| `inbox.lanes.<lane>.cap` | 1 / 2 / 4 | 每个收件箱在 `interactive` / `background` / `batch` 上的并发 run 数 |
| `inbox.lanes.<lane>.quiet-window` / `max-wait` | `1500ms` / 4 倍窗口 | 合批的静默窗口及其上界 |
| `inbox.claim.poll-interval` / `max-wait` | `2s` / `30s` | 长轮询的复查间隔与最长等待 |
| `inbox.suspend.max-timeout` | `30d` | 挂起超时的上限 |
| `inbox.timers.min-cron-period` | `60s` | cron 定时器的最小触发间隔 |
| `inbox.interrupt.keywords` | `stop, cancel, 停止, 取消` | 会置上打断标记的对话文本 |
| `inbox.scheduling.enabled` | `true` | 是否运行后台循环（清扫器、定时器调度器、仪表指标） |
| `inbox.wake.enabled` | `true` | 给长轮询领取方的进程内唤醒提示 |

Lane 上限、静默窗口和合批上限都可以通过运维 API 按收件箱单独覆盖。

## 本版本的限制

- 路线图的 M2 阶段没有实现：按 client 的 QPS 限流、单收件箱积压上限（429）、熔断、分片和消费回执。
- 唤醒提示是进程内的。部署多个实例时，另一个实例上的领取方要等到下一次轮询才发现新工作。正确性不受影响。
- 领取响应如果丢失，那个 run 会延迟一个租约周期（60 秒）。
- 跨收件箱找工作时会扫描未被领取的事件。在目前的规模下没有问题，分片会取代这种做法。
- 还没有保留期和归档任务，表只增不减。
- 调用方及其密钥来自配置。自助注册属于 M2。
- 时间戳取自应用时钟，所以各节点必须做 NTP 同步。时钟偏差只会造成提前的重复下发；run 的 epoch 保证不会出现两个持有者。
