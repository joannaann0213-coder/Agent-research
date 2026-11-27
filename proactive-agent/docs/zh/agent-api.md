# Agent API

[English](../en/agent-api.md)

Agent API 把主动式 agent 开放给应用。应用的后端，或者应用用户自己的客户端，把用户说的话和应用系统里发生的事交给它。agent
会回答，有理由时会先开口，做需要有人批准的事之前会先问，说过稍后再来的就会再来。它跑在 Claude Managed Agents 上，应用不需要直接和
CMA 打交道。

这是写给应用开发者的指南。每个接口、字段和状态码都以 OpenAPI 文档为准；服务内部如何工作见 [Agent 服务](agent-service.md)。
[教程](tutorial.md)一步步演示如何运行服务并接入应用。

## OpenAPI 规范

服务在 `GET /openapi.yaml` 发布自己的规范，不需要鉴权。它就是契约：测试套件用它校验集成测试里的每个请求和响应；规范里的某个操作或列出的某个状态码没有被测到，测试就会失败。

```bash
curl -s http://localhost:8090/openapi.yaml -o agent-api.yaml
```

## 概念

| 概念 | 是什么 |
| --- | --- |
| 应用 | 你的一个产品，有自己的 agent、用户和 key。由服务的运维方创建 |
| 用户 | 应用的一个终端用户，用应用自己的 id 表示（`user_id`）。应用第一次提到某个用户时，这个用户就存在了 |
| Agent | agent 是做什么的、能用什么：指令、模型、MCP 服务器、沙箱。一个应用可以有多个，按 key 区分；不指定时是 `default` |
| 主 agent | 带有 worker 的 agent。关于一个用户的一切都来到它这里，进入它所服务的会话；耗时的工作由它交给 worker |
| 会话 | 用户和 agent 之间的对话，按 `conversation_id` 区分；不指定时是 `default` |
| 事件 | 应用系统里发生的、和某个用户有关的事。同一个 `subject` 的事件由一个记得之前事件的 agent 处理 |
| 动作 | agent 等人来决定的事：对一次工具调用的审批，或者向用户提的问题 |
| 定时计划 | agent 在某个时间要做的事：由应用设置，或者是 agent 给自己设的跟进 |
| 任务 | worker 在主 agent 名下做的一件工作；worker 是应用的另一个 agent，有自己的 session。任务由主 agent 交出，或者由应用用发给那个 agent 的事件或定时计划交出；两种情况主 agent 都知道。主 agent 自己起意的任务先要等用户确认 |
| 长期授权 | 用户给出的长期授权：他的主 agent 可以自己开始某个角色的任务，不用每次都问 |
| 主动程度 | 用户在没开口的情况下想从主 agent 那里听到多少：`proactive`、`balanced` 或 `quiet`。由用户自己设；用户没设时，用主 agent 自己的默认值 |
| 更新 | 用户身上发生的、应用可能想知道的事：一条消息、一个动作、一次失败。可以轮询、订阅流，或者推送到 webhook |
| 凭据 | MCP 服务器放行 agent 所需的东西。保存在 agent 平台的 vault 里，本服务从不保存 |

## 鉴权

每个 `/v1` 请求都以 `Authorization: Bearer <credential>` 携带凭证。

| 凭证 | 形如 | 谁持有 | 能做什么 |
| --- | --- | --- | --- |
| 管理员 key | 至少 32 个字符，来自配置 | 服务的运维方 | 只能用 `/v1/admin`：应用和应用的 API key |
| API key | `afk.<key_id>.<secret>` | 应用的后端 | 应用的一切，针对所有用户 |
| 用户 token | `aft.<payload>.<signature>` | 某个用户的客户端，从应用后端拿到 | 这个用户的消息、动作、更新、定时计划、任务、长期授权和凭据；不能写事件，不能改 agent |

API key 只在创建时显示一次，存储的是哈希。用户 token 默认 15 分钟有效，最长一小时；后端用 `POST /v1/users/{user_id}/tokens` 签发，对同一路径调用 `DELETE` 吊销该用户的所有 token。网页和本服务不在同一个源时，把网页的源加进
`AGENT_CORS_ALLOWEDORIGINS` 之后就可以使用用户 token。

## 错误

错误响应是 `application/problem+json`，带一个稳定的 `code`。

| 状态码 | `code` | 何时 |
| --- | --- | --- |
| 400 | `invalid_request` | 请求不合法；`detail` 说明原因 |
| 401 | `unauthenticated` | 没有凭证，或凭证未知、已过期、已吊销 |
| 403 | `forbidden` | 这个凭证不能做这件事，或者应用已被停用 |
| 404 | `not_found` | 没有这个东西，或者调用方看不到 |
| 409 | `conflict` | 某个 id 已经用于别的东西，或者已经有人做了决定 |
| 413 | `payload_too_large` | 文本或数据太长 |
| 429 | `rate_limited` | 请求太多；按 `Retry-After` 的秒数等待 |
| 503 | `unavailable` | 连不上 agent 平台；什么都没有改，稍后重试 |

## 快速上手

服务和事件收件箱按 [README](../../README.zh-CN.md#agent-服务) 启动；演示模式下不需要 Anthropic key，由脚本扮演 agent。`examples/agent-api/quickstart.sh` 执行的是同样的步骤。

```bash
API=http://localhost:8090
ADMIN=change-me-admin-key-0123456789abcdef

# 运维方登记应用，并给应用后端一个 API key。
curl -s -X POST $API/v1/admin/apps -H "Authorization: Bearer $ADMIN" \
  -H 'Content-Type: application/json' -d '{"app_id": "acme", "name": "Acme"}'
KEY=$(curl -s -X POST $API/v1/admin/apps/acme/keys -H "Authorization: Bearer $ADMIN" \
  -H 'Content-Type: application/json' -d '{"label": "back end"}' | jq -r .api_key)

# 应用描述自己的 agent。
curl -s -X PUT $API/v1/agents/default -H "Authorization: Bearer $KEY" \
  -H 'Content-Type: application/json' \
  -d '{"name": "Assistant", "instructions": "You help Acme customers with their orders."}'

# 用户发消息；202 表示已保存，不表示已回答。
curl -s -X POST $API/v1/users/u42/messages -H "Authorization: Bearer $KEY" \
  -H 'Content-Type: application/json' -d '{"message_id": "m-1", "text": "Where is my order 1234?"}'

# 稍后回复就出现在消息列表里。
curl -s "$API/v1/users/u42/messages?after=0" -H "Authorization: Bearer $KEY"
```

## 会话

`POST /v1/users/{user_id}/messages` 保存一条消息并交给 agent；回答以 `kind: reply` 出现在用户的消息里。用
`GET /v1/users/{user_id}/messages` 读取，`after` 填上一次的 `next_after`；或者订阅[更新](#更新)。

- `message_id` 让重试无害：同一个 id、同样的文本返回之前的回执，带 `deduped: true`；同一个 id、不同的文本返回 409。
- 相隔很近（大约一秒半以内）的几条消息会合在一起，用一条回复回答；在 agent 拿到输入之前用户写的内容也一样。agent 正在回答时到达的消息由下一条回复处理。对主
  agent 还不止于此：正在生成的回复会被结束，不会展示给用户，下一条回复回答用户写过的全部内容。客户端不用为此做任何事，把收到的回复展示出来即可。规则见[主
  agent 与 worker](main-agent.md#用户又发来一条时)。
- 每个 `conversation_id` 是一个独立的会话，在平台上有自己的 session。会话由哪个 agent 服务，由它的第一条消息决定（`agent`）。
- `POST /v1/users/{user_id}/conversations/{conversation_id}/stop` 停止正在进行的回复：用户到此为止的消息不再回答。之后发的消息照常回答。

用户的客户端可以用用户 token 自己完成以上所有操作，但只能针对自己的用户。

## 来自你的系统的事件

`POST /v1/users/{user_id}/events` 告诉 agent 发生了一件和用户有关的事：订单发货了、工单有回复了、扣款失败了。只有 API key 能调用。

```json
{
  "event_id": "order-1234-delayed",
  "type": "order.delayed",
  "subject": "order-1234",
  "data": {"order_id": "1234", "new_delivery_date": "2026-11-27", "reason": "carrier backlog"}
}
```

agent 在后台、在这个 `subject` 的工作线上查看它，判断用户是否需要知道。需要的话，它会向 `conversation_id` 指定的会话（不指定时是
`default`）发一条 `kind: notification` 的消息。很多时候它会判断无需多言。

- `data` 是以“其他系统提供的信息”的身份交给 agent 的，从不作为指令：里面叫 agent 做什么的文字不会被执行。想让 agent 对某类事件做什么，写在 agent 的 `instructions` 里。
- `deliver_at` 让事件推迟到指定时间再处理。
- 同一个 `event_id`、同样的内容重复提交无害；内容不同返回 409。以 `agent.` 开头的类型保留给服务自己。
- 会话在回答用户下一条消息之前，会先了解后台工作和用户在这个会话里交流过什么。
- 发给[主 agent](#主-agent-和它的任务)的事件不在 `subject` 的工作线上处理：它进入 `conversation_id` 指定的会话，主 agent
  在那里得知一切。
- 在主 agent 服务的会话里，指明另一个 agent（`agent`）的事件同样没有自己的工作线。它成为主 agent 的一个任务：被指明的 agent 作为 worker
  处理它，主 agent 立刻得知，回执里带着这个任务的 `task_id`。

## 动作

当 agent 要调用的工具所属的服务器配置为 `approval: always_ask`（或者 `auto`，而平台自己判断不了这次调用），或者 agent 用
`ask_user` 工具向用户提问时，这一轮会停下来等人，同时出现一个动作。

| 类型 | 谁来决定 | 怎么决定 |
| --- | --- | --- |
| `approval` | agent 配置里该 MCP 服务器的 `approver`：`user` 或 `app` | `{"decision": "approve"}` 或 `{"decision": "deny", "message": "why"}` |
| `question` | 用户 | `{"answer": "..."}`，或者往会话里发一条消息，它会作为最近一个问题的回答；`{"decision": "deny"}` 表示不回答 |

`GET /v1/users/{user_id}/actions` 列出等待中的动作；`POST .../actions/{action_id}/resolve` 做决定。API key 可以决定其用户的所有动作；用户
token 只能决定该由用户决定的，其余的也看不到。

- 谁先决定就以谁为准，决定一旦做出就不会被推翻：批准不会因为同一时刻到了截止时间或来了新输入而变成拒绝。
- 24 小时内没人决定的动作会过期，agent 会被告知。
- 动作所在的工作线上来了新输入（会话里的新消息、该主题的新事件），等待中的动作就作废；agent 结束这一轮，新输入在下一轮处理。
- [主 agent](#主-agent-和它的任务) 提的问题不会因为事件或它的 worker 的汇报而作废：它等的是用户在会话里的下一条消息，或者用 `resolve`
  给出的回答，agent 像回复任何消息一样回复它。
- 需要审批的沙箱内置工具不会拿去问任何人，直接拒绝。

## 定时计划与跟进

`POST /v1/users/{user_id}/schedules` 让 agent 在指定时间按你的指令做事：一次（`at`），或者按 `cron` 规则反复执行，规则按用户的
`time_zone` 解释。

```json
{
  "schedule_id": "morning-brief",
  "name": "Morning brief",
  "instructions": "Look at the user's open orders and tell them about anything that needs them today.",
  "cron": "0 8 * * 1-5",
  "time_zone": "Asia/Shanghai",
  "subject": "orders"
}
```

agent 也会用自己的 `schedule_followup` 工具给自己设跟进：会话里的“一小时后提醒我”，或者后台的“明天再看一下这个订单”。两种都由
`GET /v1/users/{user_id}/schedules` 列出，用 `DELETE .../schedules/{schedule_id}` 取消；用户的客户端两者都能做，用户因此可以叫停 agent 为自己做的事。

在[主 agent](#主-agent-和它的任务) 服务的会话里，指明另一个 agent（`agent`）的定时计划由那个 agent 作为 worker 来做。每次到点，它都并入计划的
`subject` 对应的那个主 agent 的任务，交给 worker；主 agent 立刻得知，并在用户需要知道时把 worker 的汇报转告用户。

## 更新

用户身上发生的每件事都记录为一条更新：`message.created`、`action.created`、`action.resolved`、`run.failed`，以及主 agent
的任务对应的 `task.created` 和 `task.updated`，还有用户的设置变化时的
`preferences.updated`。每条都有一个只增不减的 `seq`，读取方从拿到的最后一条之后接着读，就什么都不会漏。有三种读法。

| 方式 | 接口 | 适合 |
| --- | --- | --- |
| 轮询 | `GET /v1/users/{user_id}/updates?after=<seq>` | 任何场景；每页最多 500 条 |
| 流 | `GET /v1/users/{user_id}/stream` | 需要实时显示回复的客户端；server-sent events |
| Webhook | `PUT /v1/webhook` | 应用后端；覆盖应用的所有用户 |

流把每条更新作为一个事件发出，事件的 `id` 就是它的 `seq`。客户端带着 `Last-Event-ID` 重连，就能拿到错过的部分；浏览器的
`EventSource` 会自动这样做。`EventSource` 不能带请求头，所以浏览器先用 `POST /v1/users/{user_id}/stream-tickets` 拿一张一次性票据，再打开
`GET /v1/users/{user_id}/stream?ticket=<ticket>`。

webhook 对每条更新收到一个 POST，用 HMAC-SHA256 签名：`webhook-signature` 是 `v1,` 加上对 `<webhook-id>.<webhook-timestamp>.<body>` 的
Base64 签名，密钥是 secret 的字节（`whsec_` 之后的部分做 Base64 解码）。Java 接收方可以用 `agent-api` 里的 `WebhookSignature.verify`；Python 写法如下：

```python
import base64, hashlib, hmac, time

def verify(secret: str, headers: dict, body: bytes, tolerance: int = 300) -> bool:
    key = base64.b64decode(secret.removeprefix("whsec_"))
    signed = f"{headers['webhook-id']}.{headers['webhook-timestamp']}.".encode() + body
    expected = "v1," + base64.b64encode(hmac.new(key, signed, hashlib.sha256).digest()).decode()
    fresh = abs(time.time() - int(headers["webhook-timestamp"])) <= tolerance
    return fresh and any(hmac.compare_digest(expected, s) for s in headers["webhook-signature"].split(" "))
```

- 返回任意 2xx 即表示成功。其他情况会在 10 秒、1 分钟、5 分钟、30 分钟、2 小时、6 小时和 12 小时后重试；最后一次也失败时，webhook 会被关闭，直到用 `enabled: true` 再 PUT 一次。
- 同一条投递可能到达两次：忽略见过的 `id`。按用户、按 `seq` 排序。
- URL 必须是公网上的 https 地址。回环、内网和链路本地地址（包括云厂商的元数据服务）一律拒绝，设置时检查，每次投递时再检查；不跟随重定向。

## MCP 服务器的凭据

需要凭据的 MCP 服务器从 agent 平台的 vault 里取得凭据，平台把它附加到 agent 对该服务器的调用上；agent 和它的沙箱都看不到，本服务也不保存。

```bash
# 应用级凭据：所有用户的 session 都带着它。
curl -s -X PUT $API/v1/credentials/tickets -H "Authorization: Bearer $KEY" \
  -H 'Content-Type: application/json' \
  -d '{"mcp_server_url": "https://mcp.acme.example/tickets", "token": "tk_live_example"}'

# 用户自己的账号，在用户于你的应用里通过 OAuth 授权之后。
curl -s -X PUT $API/v1/users/u42/credentials/notes -H "Authorization: Bearer $KEY" \
  -H 'Content-Type: application/json' \
  -d '{"mcp_server_url": "https://mcp.notes.example/mcp",
       "oauth": {"access_token": "at_example", "refresh_token": "rt_example",
                 "client_id": "acme-agent", "token_endpoint": "https://auth.notes.example/oauth/token"}}'
```

对同一个服务器，用户的凭据优先于应用的。替换后的 token 从下一次调用起生效。带 refresh token 时，平台会自己续期 access token。

## Agent

`PUT /v1/agents/{agent_key}` 描述一个 agent；每次提交完整配置。

| 字段 | 含义 |
| --- | --- |
| `name`、`instructions` | 必填。agent 是做什么的、怎么做。服务会附加几句自己的说明：agent 如何被唤醒、如何触达用户 |
| `model`、`effort` | Claude 模型（默认 `claude-opus-5-5`）和思考的力度 |
| `mcp_servers` | 第三方系统：`name`、https 的 `url`、`approval`（`always_ask`、`auto` 或 `always_allow`）和 `approver`（`app` 或 `user`） |
| `sandbox` | 带内置工具（shell、文件、网页搜索与抓取）的容器及其网络：`none`、`limited` 到 `allowed_hosts`，或 `unrestricted` |
| `session_budget_usd` | 一个 session 最多能花多少钱；花完的会话在新的 session 里继续 |
| `memory` | `per_user`：agent 跨 session 记住每个用户，笔记存在沙箱的文件里，因此必须开启沙箱。会话里写，后台只读 |
| `coordinator` | 让这个 agent 成为主 agent。`workers` 列出它可以把工作交给谁：每一项有 `role`、扮演这个角色的 `agent` 的 key、告诉主 agent 这个角色用来做什么的 `description`，需要时还有 `start`，规定这个角色的任务何时可以开始。`proactivity` 规定用户没说过时主 agent 的主动程度：不设就是 `proactive` |

服务给每个 agent 四个自带工具：`read_events`、`notify_user`、`ask_user` 和 `schedule_followup`；主 agent 另有五个用来交出工作的工具：`delegate_task`、`start_task`、`message_worker`、`list_tasks`
和 `close_task`，另外还有 `set_proactivity`。正在进行的会话在下一轮采用改过的配置，换到一个新的 session，并被告知之前说过的内容。

## 主 agent 和它的任务

带有 `coordinator` 的 agent 就是主 agent：关于一个用户的一切都由它得知，并由它决定怎么办。它回答用户；你的事件和定时计划在同一个 session
里唤醒它；耗时的工作由它交给 worker，worker 再向它汇报。[主 agent 与 worker](main-agent.md) 说明它如何工作，以及为什么这样实现。

```bash
# worker 就是一个普通 agent，有自己的指令和工具。
curl -s -X PUT $API/v1/agents/researcher -H "Authorization: Bearer $KEY" \
  -H 'Content-Type: application/json' \
  -d '{"name": "Researcher", "instructions": "You look things up in the order system and report what you found."}'

# 主 agent 列出它可以把工作交给哪些 worker，以及各自用来做什么。
curl -s -X PUT $API/v1/agents/default -H "Authorization: Bearer $KEY" \
  -H 'Content-Type: application/json' \
  -d '{"name": "Assistant", "instructions": "You look after Acme customers.",
       "coordinator": {"workers": [{"role": "research", "agent": "researcher",
                                    "description": "Looks things up in the order system and reports what it found."}]}}'

# 主 agent 替某个用户交出去的工作，以及怎样叫停其中一个任务。
curl -s $API/v1/users/u42/tasks -H "Authorization: Bearer $KEY"
curl -s -X POST $API/v1/users/u42/tasks/task_01EXAMPLE/close -H "Authorization: Bearer $KEY"

# 等用户确认的任务，以及确认其中一个。要拒绝就关闭它。
curl -s "$API/v1/users/u42/tasks?status=proposed" -H "Authorization: Bearer $KEY"
curl -s -X POST $API/v1/users/u42/tasks/task_01EXAMPLE/start -H "Authorization: Bearer $KEY"

# 用户给某个角色的长期授权，以及收回它。
curl -s -X PUT $API/v1/users/u42/grants/default/research -H "Authorization: Bearer $KEY"
curl -s -X DELETE $API/v1/users/u42/grants/default/research -H "Authorization: Bearer $KEY"

# 用户想要主 agent 多主动：读取，以及设置。
curl -s $API/v1/users/u42/preferences -H "Authorization: Bearer $KEY"
curl -s -X PUT $API/v1/users/u42/preferences -H "Authorization: Bearer $KEY" \
  -H 'Content-Type: application/json' -d '{"proactivity": "balanced"}'
```

调用 API 的方式没有别的变化：用户照旧写到 `POST /v1/users/{user_id}/messages`，你的系统照旧提交事件。变的是它们去哪里。

- 发给主 agent 的事件和定时计划，在 `conversation_id` 指定的会话里（不指定时是 `default`）处理，由在那里和用户说话的那个 session
  负责。如果这个会话由别的 agent 服务，请求返回 409。
- 任务由主 agent 建立，你指明另一个 agent（`agent`）的活也会建立任务：一个事件，或者定时计划的每一次到点。那个 agent 作为 worker 来做，任务带着
  `created_by: app` 和事件或计划的 `subject`，不需要确认就开始。主 agent 立刻得知，worker 向它汇报。同一个主题后来的活，只要任务还开着就进同一个任务。
- `GET /v1/users/{user_id}/tasks` 列出开着的任务，加上 `status=all` 连已关闭的也列出；`POST .../tasks/{task_id}/close`
  叫停一个任务，主 agent 会被告知。
- 用户向主 agent 要求的事直接开始。主 agent 自己起意的事是一个状态为 `proposed` 的任务：把它连同 `summary` 展示给用户，用户确认时调用
  `POST .../tasks/{task_id}/start`，用户拒绝时关闭它。它可以在 3 天内被确认。这段 `summary` 同时是对话里的一条消息，`kind: proposal`，带着任务的
  `task_id`，所以只展示对话的客户端也能展示它；在那里，用户让 agent“去做吧”就是确认。
- 有了长期授权，用户就允许主 agent 不问自己而开始某个角色的任务：`PUT /v1/users/{user_id}/grants/{agent_key}/{role}` 给出授权，对同一路径调用
  `DELETE` 收回。角色的 `start` 决定规则：`ask` 是默认值，也就是上面说的做法；`always_ask` 要求这个角色的每个任务都确认；`always_allow` 一个都不用确认。
- 用户自己的客户端可以列出和关闭任务。worker 汇报了什么、两个 agent 之间写了什么，只返回给 API key。
- 用户是从主 agent 那里听说任务的。worker 不能通知用户，也不能向用户提问；但它需要审批的工具调用照常出现在[动作](#动作)里。主 agent
  会被告知 worker 在等，以及最后怎么决定的，所以用户问起时它答得上来。
- 应用里任何不是主 agent 的 agent 都可以当 worker。关于用户和会话，它只知道主 agent 写进任务里的内容，或者你的事件里说的内容。
- 一个用户最多同时有 4 个 worker 在运行，各有自己的 session；数量由收件箱的 `inbox.lanes.batch.cap`
  决定。
- `PUT /v1/users/{user_id}/preferences` 用 `proactivity` 设置用户想要主 agent 多主动：`proactive`
  是起始档，随时向用户通报进展；`balanced` 只说需要用户做的事和结果；`quiet` 只说关键的，而且不提任何建议。用户的客户端可以设，用户也可以直接告诉主
  agent；每次变化是一条 `preferences.updated` 更新。每一档具体改变什么，见[主 agent 与 worker](main-agent.md#主动程度)。

演示模式下，脚本会把用户写在 `task:` 后面的内容交出去，worker 回答说做完了；事件的数据里如果有一段文字 `task`，脚本会自己提出做这件事；指明 worker
的事件或定时计划由 worker 处理，它的汇报由主 agent 转告用户。任务的文字里有 "needs approval" 时，带工具服务器的 worker
会去调用它，这样就能试一试等人审批的过程。以 `slow:` 开头的消息会让脚本花四秒钟才回答，这样就能试一试在回答到来之前再发一条。`examples/agent-api/main-agent.sh`
把这些情况都走一遍。

## 统计

agent 主动做多少事是可以设置的：用户的 `proactivity`、角色的开工规则、服务的各项上限。调这些设置时要看的数字来自两个接口，用 API key 调用：`GET /v1/users/{user_id}/stats`
看一个用户，`GET /v1/stats` 看应用的全部用户。两个接口都接受 `since` 和 `until`，不传就是最近 24 小时，跨度最长 92 天。

```bash
curl -s "$API/v1/stats" -H "Authorization: Bearer $KEY"
```

```json
{
  "since": "2026-11-19T10:00:00Z",
  "until": "2026-11-20T10:00:00Z",
  "users": 37,
  "turns": [
    {"kind": "background", "outcome": "notified", "turns": 61, "yields": 3, "seconds": 402.5, "model_requests": 190,
     "input_tokens": 41200, "output_tokens": 9100, "cache_read_tokens": 611000, "cache_creation_tokens": 28000},
    {"kind": "background", "outcome": "silent", "turns": 148, "yields": 0, "seconds": 655.1, "model_requests": 301,
     "input_tokens": 60400, "output_tokens": 7200, "cache_read_tokens": 980000, "cache_creation_tokens": 31000},
    {"kind": "chat", "outcome": "replied", "turns": 212, "yields": 0, "seconds": 1190.4, "model_requests": 344,
     "input_tokens": 90100, "output_tokens": 30800, "cache_read_tokens": 1320000, "cache_creation_tokens": 77000},
    {"kind": "chat", "outcome": "superseded", "turns": 9, "yields": 0, "seconds": 21.0, "model_requests": 9,
     "input_tokens": 2100, "output_tokens": 600, "cache_read_tokens": 31000, "cache_creation_tokens": 0},
    {"kind": "worker", "outcome": "reported", "turns": 58, "yields": 0, "seconds": 2740.9, "model_requests": 410,
     "input_tokens": 120300, "output_tokens": 51000, "cache_read_tokens": 1500000, "cache_creation_tokens": 90000}
  ],
  "replies": {"replies": 212, "median_seconds": 6.4, "p95_seconds": 11.9},
  "messages": {"from_user": 233, "replies": 212, "notifications": 74, "questions": 5, "proposals": 12},
  "tasks": {"made": 63, "by_agent": 51, "by_application": 12, "asked_for": 39, "confirmed": 7, "unasked": 2,
            "declined": 2, "expired": 1, "withdrawn": 0, "waiting": 0, "failures": 1},
  "levels": [
    {"proactivity": "balanced", "users": 4, "notifications": 6},
    {"proactivity": "default", "users": 31, "notifications": 66},
    {"proactivity": "quiet", "users": 2, "notifications": 2}
  ]
}
```

| 字段 | 说明 |
| --- | --- |
| `turns` | 在这段时间里开始并且已经结束的回合，按 `kind`（`chat`、`background`、`worker`）和 `outcome` 分组，附带耗时和对模型的请求所用的 token |
| `replies` | 用户等回复等了多久，从这条回复所回答的最早一条消息算起 |
| `messages` | 说了些什么：用户的消息，以及 agent 的回复、通知、提问和提议 |
| `tasks` | 建了哪些任务，谁建的，凭什么开工，提议出去的任务后来怎样了 |
| `users`、`levels` | 针对应用：有多少用户有过回合；以及按 `proactivity` 设置分组的用户数和每组收到的通知数 |
| `proactivity` | 针对单个用户：用户自己的设置；没设过就没有这个字段 |

- `background` 回合里以 `silent` 结束的，就是主 agent 被叫醒却什么也没做的那部分。把 `proactivity` 调低会少叫醒它；你的系统少发一种事件也一样。
- 以 `absorbed` 结束的回合发现它的活已经被另一个回合做了，没有向模型发过请求。
- `superseded` 的 chat 回合是因为用户又发了消息而被结束的回复。它在那之前用掉的 token 照样算，它的等待时间计入后来到达的那条回复。
- 一次请求的输入是三个数之和：`input_tokens`、`cache_read_tokens` 和 `cache_creation_tokens`。session 的大部分输入是从缓存读的，价格只是一小部分。
- 服务的指标统计的是同一批回合，但不区分用户和应用；见 [Agent 服务](agent-service.md#该看什么)。
- 演示模式下没有模型：脚本的每一步算作一次请求，输入 100 个 token，输出 20 个。

## 限制

| 项目 | 默认值 | 配置项 |
| --- | --- | --- |
| 应用 API key 的请求 | 先 100 个，之后每秒 50 个 | `agent.limits.app-burst`、`app-per-second` |
| 单个用户 token 的请求 | 先 20 个，之后每秒 5 个 | `agent.limits.user-burst`、`user-per-second` |
| 后台一轮的通知 | 3 条 | `agent.notify.per-run` |
| 单个用户的通知 | 24 小时内 20 条 | `agent.notify.per-user-per-day` |
| 想让主 agent 安静的用户收到的通知 | 24 小时内 5 条，用户自己要的事的结果不计在内 | `agent.notify.quiet-per-user-per-day` |
| 连续跟进的次数，和一条工作线上等待的跟进 | 5 和 5 | `agent.followups.max-chain`、`max-pending` |
| 动作的等待时长 | 24 小时 | `agent.actions.ttl` |
| 一轮最长的工作时间 | 10 分钟 | `agent.turn.deadline` |
| 一个主 agent 会话里的任务：排队或进行中的，以及开着的 | 4 个和 20 个 | agent 的 `max_active_tasks`、`agent.tasks.max-open` |
| 任务的指令，以及 worker 的汇报 | 8,000 和 16,000 个字符 | `agent.tasks.max-instructions`、`max-report` |
| 一个主 agent 会话里等确认的任务数，以及等多久 | 5 个，等 3 天 | `agent.tasks.max-proposed`、`proposal-ttl` |
| 一个用户不被询问就开始的任务数 | 24 小时 10 个 | `agent.tasks.max-unasked-per-day` |
| 消息文本、事件数据 | 32,000 个字符、16 KB | 固定 |
