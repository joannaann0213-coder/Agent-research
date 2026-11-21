# API

[English](../en/api.md)

事件收件箱在 `/v1` 之下以 JSON over HTTP 提供服务。生产方写入事件和定时器，运行时领取 run
并结束它们，运维人员做重放和配置。本页的示例都是从一个运行中的服务上抓到的真实响应。接入 connector 或 agent
时该调用哪些接口、按什么顺序，见[接入指南](integration.md)。

## 约定

- 字段名用 `snake_case`。没有值的字段直接省略，不发 `null`。服务以文档形式保存的 JSON 按保存时的原样返回：事件的
  `payload`，以及 `inbox_config` 审计条目的 `params`，其中被省略、因而回到默认值的上限显示为 `null`。
- 未知字段是错误（400），所以字段名里的笔误绝不会被悄悄忽略。收件箱的 `settings` 内部也是如此。
- 时间戳是 UTC 的 ISO-8601 格式，例如 `2026-11-20T17:27:39.671132Z`。
- Id 是 ULID：26 个字符，按创建时间有序。
- 错误以 `application/problem+json` 返回，带一个稳定的、机器可读的 `code`。
- `inbox_id` 要匹配 `[A-Za-z0-9][A-Za-z0-9_.:@-]{0,127}`。

## 鉴权

每个 `/v1` 请求都用 HMAC-SHA256 签名。签名由三个请求头携带。

| 请求头 | 值 |
| --- | --- |
| `X-Client-Id` | 调用方的 client id |
| `X-Timestamp` | 以秒为单位的 Unix 时间；与服务器时钟相差 300 秒以内才被接受 |
| `X-Signature` | `hmac-sha256=` 后跟十六进制的签名 |

签名用 client 的密钥对下面这个字符串计算，以 `\n` 为分隔符：

```text
v1
{timestamp}
{METHOD}
{path}?{query}
{hex sha256 of the body}
```

方法名大写；query 按发送时的原样包含在内（没有 query 时连同 `?` 一起省略）；空请求体按空字符串求哈希。把方法、路径和时间戳都绑进签名，被截获的请求就无法拿去打别的接口，也无法在之后重放。

```bash
timestamp=$(date +%s)
body_hash=$(printf '%s' "$body" | sha256sum | cut -d' ' -f1)
signature=$(printf 'v1\n%s\n%s\n%s\n%s' "$timestamp" "$method" "$path" "$body_hash" \
  | openssl dgst -sha256 -hmac "$secret" -hex | sed 's/^.*= *//')
```

`scripts/inbox-curl.sh` 做的正是这件事，`HmacSigner` 是它的 Java 实现。请求体超过 128 KB 时，在解析之前就以 413 拒绝。

### 角色与租户

| 角色 | 路径 |
| --- | --- |
| `PRODUCER` | `/v1/inbox/**`：写入事件和定时器，读回并取消自己写入的内容 |
| `RUNTIME` | `/v1/runs/**` |
| `OPERATOR` | `/v1/admin/**`；对 `/v1/inbox/**` 和 `GET /v1/runs/{run_id}` 的只读访问；取消任意定时器 |

属于某个租户的 client 始终在自己的租户内行事。平台级的 client（配置时不带租户）要指明租户：在 `/v1/inbox/**` 路径和管理类的 `GET`
请求上用 query 参数 `tenant_id`，在领取、重放、转入死信和配置请求上用请求体字段 `tenant_id`。针对某个 run 的调用不需要，因为 run id
已经确定了这个 run。在领取和审计日志上，租户对平台级 client 是可选的，不填表示所有租户。别的租户的事件、run 或定时器一律报告为 404。

## 错误

```json
{"detail":"run 01M7X001ND6S05N2BHWQV128TR is no longer held under this epoch; stop working on it",
 "instance":"/v1/runs/01M7X001ND6S05N2BHWQV128TR/heartbeat","status":409,"title":"Conflict","code":"lease_lost"}
```

| 状态码 | `code` | 含义 |
| --- | --- | --- |
| 400 | `invalid_request` | JSON 格式错误、有未知字段、缺少某个值或值不合法 |
| 401 | `unauthenticated` | 签名缺失、错误或已过期 |
| 403 | `forbidden` | Client 的角色不允许调用这个接口，或者它指了别的租户 |
| 404 | `not_found` | 这个租户里没有这样的收件箱、事件、run 或定时器 |
| 409 | `idempotency_conflict` | 这个幂等键已经被另一条不同的事件用过 |
| 409 | `lease_lost` | 这个 run 已不在该 epoch 之下被持有。停止处理它 |
| 409 | `resume_key_in_use` | 这个收件箱里另一个挂起的 run 在等同一个 resume key |
| 409 | `conflict` | 对象当前的状态不允许这个操作 |
| 413 | `payload_too_large` | Payload 超过 64 KB，或者请求体超过 128 KB |
| 500 | `internal_error` | 意料之外的失败 |
| 503 | `database_unavailable` | 事件存储不可用。请用同一个幂等键重试 |

## 生产方 API

### 写入一条事件

`POST /v1/inbox/{inbox_id}/events`

| 字段 | 必填 | 含义 |
| --- | --- | --- |
| `source` | 是 | `biz_webhook`、`connector`、`timer`、`conversation`、`rule` 或 `agent_self` |
| `type` | 是 | 带命名空间和版本的语义类型，例如 `order.paid.v1`。最多 256 个字符 |
| `payload` | 是 | 除 `null` 以外的任意 JSON 值，规范形式下最多 64 KB |
| `source_event_id` | 建议 | 幂等键，最多 256 个字符。不提供时由服务端给事件算指纹。前缀 `fp1:` 和 `timer:` 是保留的 |
| `merge_key` | 否 | 合批键。同键的事件同一时间只由一个 run 处理，并且可以共用一个 run |
| `lane` | 否 | `interactive`、`background` 或 `batch`。默认值随来源而定（见下文） |
| `occurred_at` | 否 | 它在来源处发生的时间 |
| `ready_at` | 否 | 它什么时候可以运行。不填或已过去表示现在；未来的值会把事件延后 |
| `resume_key` | 否 | 用在应当唤醒某个挂起 run 的回复上 |
| `interrupt` | 否 | 请正在处理这个键的 run 让路。默认对对话消息做停止词检查 |
| `trace_id` | 否 | 原样传给运行时。不填时自动生成 |

`biz_webhook`、`connector` 和 `conversation` 的默认 lane 是 `interactive`，`timer` 和 `agent_self` 是
`background`，`rule` 是 `batch`。

```bash
scripts/inbox-curl.sh order-bot order-bot-secret POST /v1/inbox/u42/events \
  '{"source":"biz_webhook","source_event_id":"order-1234-v7","type":"order.paid.v1",
    "merge_key":"order:1234","payload":{"order_id":"1234","amount":299}}'
```

```json
{"event_id":"01M7WZZSCQPP6KTQPVXCBECVHK","deduped":false,"status":"pending"}
```

**202 表示已接收，不表示已处理。** 事件已经持久化，并且会被至少投递一次。

| 情况 | 状态码 | 响应 |
| --- | --- | --- |
| 新事件 | 202 | `deduped: false`，`status: pending` |
| 同键，内容相同 | 202 | 先前的 `event_id`、`deduped: true` 以及它当前的状态 |
| 同键，内容不同 | 409 | `idempotency_conflict` |
| 没有 `source_event_id` | 202 | `fingerprinted: true`，以及一条要求提供显式键的 `warning` |
| `ready_at` 在未来 | 202 | `status: scheduled`；`event_id` 此时已经是最终的 |

“内容相同”比较的是类型和规范化之后的 payload，所以键的顺序不同、空白不同都算同一条事件。遇到 503 或网络错误，生产方应当用同一个键重试。

### 读取事件

`GET /v1/inbox/{inbox_id}/events/{event_id}` 返回事件及其完整历史，最早的在前。

```json
{"event":{"event_id":"01M7WZZSCQPP6KTQPVXCBECVHK","tenant_id":"acme","inbox_id":"u42","seq":1,
  "client_id":"order-bot","source":"biz_webhook","source_event_id":"order-1234-v7",
  "dedupe_kind":"provided","type":"order.paid.v1","payload":{"amount":299,"order_id":"1234"},
  "received_at":"2026-11-20T17:27:39.671132Z","ready_at":"2026-11-20T17:27:39.671132Z",
  "merge_key":"order:1234","lane":"interactive","interrupt":false,
  "trace_id":"01M7WZZSCQPP6KTQPVXCBECVHJ","status":"done","attempt":1,"max_attempts":5,
  "run_id":"01M7X001ND6S05N2BHWQV128TR","run_seq":1,"delivery_mode":"batch",
  "first_claimed_at":"2026-11-20T17:27:48.128723Z","finished_at":"2026-11-20T17:27:48.358564Z"},
 "transitions":[
  {"to_status":"pending","reason":"accepted","actor":"client:order-bot","at":"2026-11-20T17:27:39.671132Z"},
  {"from_status":"pending","to_status":"claimed","reason":"claim","actor":"worker:worker-1",
   "run_id":"01M7X001ND6S05N2BHWQV128TR","attempt":1,"at":"2026-11-20T17:27:48.128723Z"},
  {"from_status":"claimed","to_status":"done","reason":"ack","actor":"worker:worker-1",
   "run_id":"01M7X001ND6S05N2BHWQV128TR","attempt":1,"at":"2026-11-20T17:27:48.358564Z"}]}
```

`GET /v1/inbox/{inbox_id}/events` 按到达顺序列出事件。

| 参数 | 含义 |
| --- | --- |
| `status` | `pending`、`claimed`、`done`、`failed` 或 `dead` |
| `source` | 来源取值之一 |
| `from`, `to` | 接收时间的范围，`from <= t < to` |
| `limit` | 1 到 500，默认 100 |

生产方只能看到自己写入的事件。运维人员能看到租户的每一条事件。尚未触发的延迟事件可以按 id 读取，其状态为 `scheduled`。

### 定时器

`POST /v1/inbox/{inbox_id}/timers` 创建一个定时器。到期的定时器往收件箱里写入一条 `timer.due` 事件；它从不调用运行时。

| 字段 | 含义 |
| --- | --- |
| `kind` | `once` 或 `cron`（必填） |
| `fire_at` | `once` 定时器的触发时间 |
| `cron` | 五段（以分钟开头）或六段（以秒开头）。每 60 秒最多触发一次 |
| `time_zone` | 对 cron 求值所用的 IANA 时区，例如 `Asia/Shanghai`。默认 UTC |
| `timer_key` | 幂等键。创建一个键已经在排期中的定时器，返回的是那个定时器 |
| `merge_key` | 该定时器所产生事件的合批键 |
| `lane` | 这些事件的 lane。默认 `background` |
| `payload` | 带进该定时器产生的每一条事件。最多 32 KB |

```bash
scripts/inbox-curl.sh order-bot order-bot-secret POST /v1/inbox/u42/timers \
  '{"kind":"cron","cron":"0 9 * * 1-5","time_zone":"Asia/Shanghai","timer_key":"daily-report",
    "payload":{"report":"daily"}}'
```

```json
{"timer_id":"01M7X00EY2556QDKT50BS0CKKB","tenant_id":"acme","inbox_id":"u42","kind":"cron",
 "state":"scheduled","fire_at":"2026-11-23T01:00:00Z","cron":"0 9 * * 1-5","time_zone":"Asia/Shanghai",
 "timer_key":"daily-report","event_type":"timer.due","lane":"background","payload":{"report":"daily"},
 "created_at":"2026-11-20T17:28:01.726884Z"}
```

新建定时器时回答 201；`timer_key` 已经在排期中时回答 200。`timer.due` 事件的 payload 把定时器的 payload 包了一层：

```json
{"timer_id":"01M7X00EY2556QDKT50BS0CKKB","fire_at":"2026-11-23T01:00:00Z","kind":"cron",
 "catch_up":false,"missed_count":0,"payload":{"report":"daily"}}
```

- `GET /v1/inbox/{inbox_id}/timers/{timer_id}` 按最近的一次触发返回这个定时器，状态为 `scheduled`、`fired` 或
  `cancelled`。对 cron 定时器来说，那就是下一次已排期的触发；某次触发是否为补触发，体现在它所产生的 `timer.due` 事件的 payload 里。
- `DELETE /v1/inbox/{inbox_id}/timers/{timer_id}` 取消已排期的那一次触发，回答 204。没有已排期的触发时回答
  404。此刻正在触发的定时器会赢得这场竞争，照常触发。
- 生产方只能看到并取消自己创建的定时器。运维人员能看到租户的每一个定时器，也可以取消其中任何一个；这样的取消会写入审计日志。

如果只是要延后一条事件，改为带一个未来的 `ready_at` 去写入它。那样它会以自己的身份触发，带着自己的类型和键。

## 运行时 API

### 领取一个 run

`POST /v1/runs/claim`

| 字段 | 含义 |
| --- | --- |
| `worker_id` | 谁在请求（必填）。记录为租约的持有者，并写进事件历史 |
| `tenant_id` | 把查找范围限定在一个租户内。给了 `inbox_id` 时必填 |
| `inbox_id` | 只从这个收件箱领取。不填表示任意一个有工作的收件箱，最老的优先 |
| `lanes` | 把领取限定在这些 lane 上。不填表示全部，优先级最高的在前 |
| `accepts_steer` | 之后到达的同键事件是否可以被拉进这个 run |
| `wait_ms` | 在回答 204 之前等待工作多久。最多 30,000；0 或不填表示不等待 |

```json
{"run_id":"01M7X001ND6S05N2BHWQV128TR","epoch":1,"attempt":1,"kind":"start",
 "tenant_id":"acme","inbox_id":"u42","lane":"interactive","merge_key":"order:1234",
 "trace_id":"01M7WZZSCQPP6KTQPVXCBECVHJ","accepts_steer":true,
 "lease_expires_at":"2026-11-20T17:28:48.128723Z","lease_deadline_at":"2026-11-20T17:57:48.128723Z",
 "events":[{"event_id":"01M7WZZSCQPP6KTQPVXCBECVHK","seq":1,"run_seq":1,"delivery_mode":"batch",
   "source":"biz_webhook","type":"order.paid.v1","source_event_id":"order-1234-v7",
   "received_at":"2026-11-20T17:27:39.671132Z","merge_key":"order:1234","interrupt":false,
   "payload":{"amount":299,"order_id":"1234"},"trace_id":"01M7WZZSCQPP6KTQPVXCBECVHJ"}]}
```

| `kind` | 何时 | `events` 里有什么 |
| --- | --- | --- |
| `start` | 一个新 run | 该 run 的全部事件 |
| `redelivery` | Nack 或租约过期之后，同一个 run 再次下发 | 该 run 的全部事件 |
| `resume`，`resume_reason: event` | 带着 resume key 的事件到达了 | 作为答复的那些事件 |
| `resume`，`resume_reason: timeout` | 挂起期限已过，且策略为 `resume` | 该 run 的全部事件 |

`delivery_mode` 说明一条事件是怎么进入 run 的：`batch`（领取时）、`steer` 或 `resume`。

没有东西可领取时，回答是 204，带两个响应头。这是正常的调度结果，不是错误。

```text
HTTP/1.1 204
X-Inbox-Reason: window
X-Inbox-Retry-After-Ms: 1472
```

| `X-Inbox-Reason` | 含义 |
| --- | --- |
| `empty` | 没有未领取的工作 |
| `busy` | 有工作，但槽位已被占用，或者它的合并键正在被处理 |
| `window` | 下一批还在静默窗口内 |
| `backoff` | 下一个 run 正在等它的重试退避结束 |

只靠时间流逝答案就会改变时，才会带 `X-Inbox-Retry-After-Ms`：静默窗口关闭，或者重试退避结束。

### 心跳

`POST /v1/runs/{run_id}/heartbeat`，请求体 `{"epoch":1}`，用来续租。可选字段 `accepts_steer` 用来改变这个 run 是否接收 steer
进来的事件。

```json
{"lease_expires_at":"2026-11-20T17:28:48.258144Z","extended":true,"steer_pending":0,"interrupt_requested":false}
```

| 字段 | 含义 |
| --- | --- |
| `lease_expires_at` | 租约现在持续到这个时间 |
| `extended` | 一旦达到 30 分钟的上限即为 `false`：除非 run 先结束，否则它会在 `lease_expires_at` 被回收 |
| `steer_pending` | 等着用一次 steer 调用拉进来的同键事件数 |
| `interrupt_requested` | 有一条带打断标记的事件排在这个 run 后面等待；`interrupt_event_id` 指明是哪一条 |

### Steer

`POST /v1/runs/{run_id}/steer`，请求体 `{"epoch":1,"after_run_seq":1}`，把该 run 的合并键下等待中的事件绑定到这个
run，并返回该 run 在 `after_run_seq` 之后的全部事件。所以响应丢了的话，可以用同一个游标重新取回。没有声明 `accepts_steer`
或者没有合并键的 run，不会有任何事件并入；这些事件会等到 run 结束。

```json
{"events":[]}
```

### 结束一个 run

每个 run 都恰好以下面的调用之一结束。每个调用都回答该 run 的状态。

| 调用 | 请求体 | 效果 |
| --- | --- | --- |
| `POST /v1/runs/{run_id}/ack` | `{"epoch":1,"outcome":"done","last_run_seq":1}` | Run 及其消费的事件变为 `done` |
| `POST /v1/runs/{run_id}/ack` | `{"epoch":1,"outcome":"failed","last_run_seq":1,"error":"…"}` | 终态失败，不再重试 |
| `POST /v1/runs/{run_id}/nack` | `{"epoch":1,"reason":"…","retry_after_ms":5000}` | 退避之后同一个 run 再次下发；到达尝试次数上限则为 `dead` |
| `POST /v1/runs/{run_id}/suspend` | `{"epoch":1,"resume_key":"approval:7788","timeout_seconds":86400,"on_timeout":"finish","last_run_seq":1}` | Run 等待一条带着该 resume key 的事件 |

```json
{"run_id":"01M7X001ND6S05N2BHWQV128TR","status":"done","epoch":1,"attempt":1}
```

- `last_run_seq` 是 worker 消费过的最大 `run_seq`。该 run 里在它之后的事件回到队列。不填表示全部事件。
- `retry_after_ms` 覆盖指数退避，并以 5 分钟封顶。对 nack 的回答里带有 `next_attempt_at`。
- Suspend 必须带 `resume_key` 和 `timeout_seconds`。`on_timeout` 是 `finish`（默认）或 `resume`。超时以 30 天封顶。
- 用同一个 epoch 重复同一个调用，返回的是先前的结果。

`GET /v1/runs/{run_id}` 返回这个 run 以及绑定在它名下的事件。

## 运行时契约

下面是运行时必须遵守的规则。`inbox-client` 里的 `ScriptedWorker` 遵守了这些规则，可以当作参考实现来读。

1. **带上 epoch。** 领取之后的每一次调用都发送领取时拿到的 `epoch`。遇到 409 `lease_lost` 就立刻停下，什么都别动：这个 run 已经属于另一个
   worker。
2. **工作期间发心跳**，间隔要远小于 60 秒的租约。在两步之间读取应答里的信号。
3. **显式结束每一个 run**，用 ack、nack 或 suspend。一个只是停下来的 run，会在租约过期后被再次下发。
4. **预期会被重复下发。** 投递是至少一次的。每条消息都通过一个以 `run_id + sequence` 为键的发件箱发送，其中一个 run 的第 n 条消息的
   sequence 永远是 n，无论是首次尝试、重复下发还是恢复之后。其他副作用也用同样的办法做成幂等。
5. **发送游标。** Ack 和 suspend 都带上 `last_run_seq`，这样在最后一刻被 steer 进来的事件会被放回队列，而不是丢失。
6. **要等就挂起。** 绝不要为了等人而让一个 run 一直开着。保存现场，带上 resume key 和超时去挂起，让答复作为一条事件到达。
7. **把跟进安排成事件。** 写一条带未来 `ready_at` 的事件，来源为 `agent_self`，幂等键可以用 `run_id + sequence`；绝不要在 run
   里面 sleep。

不带 `wait_ms` 的领取是一次轮询。带上 `wait_ms`，服务端会把请求挂住，直到有工作到达、静默窗口关闭或退避结束，期间不占用数据库连接。

## 运维 API

### 重放

`POST /v1/admin/replay` 把已结束的事件放回队列。必须恰好使用一种选择方式。

| 选择方式 | 字段 |
| --- | --- |
| 单条事件 | `event_id` |
| 一个收件箱加一个时间范围 | `inbox_id`, `from`, `to` |
| 一个来源加一个时间范围，在整个租户内 | `source`, `from`, `to` |

| 字段 | 含义 |
| --- | --- |
| `tenant_id` | 租户（平台级 client 必填） |
| `reason` | 为什么（必填）。与每一条被重放的事件一起记录，并写入审计日志 |
| `operator` | 做这件事的人。默认是 client id |
| `force` | 重放 `done` 或 `dead` 的事件时必须带上 |
| `dry_run` | 只报告会重放什么，不做任何改动 |

```json
{"replayed":[],"skipped":[{"event_id":"01M7WZZSCQPP6KTQPVXCBECVHK","status":"done","why":"needs_force"}],"dry_run":true}
```

`why` 是 `needs_force` 或 `in_flight`。范围针对接收时间，左闭右开，即 `from <= t < to`。一个范围最多匹配 10,000 条事件。

### 把一条事件转入死信

`POST /v1/admin/events/{event_id}/dead`，请求体 `{"tenant_id":"acme","reason":"…","operator":"alice"}`，把一条等待中的事件移出队列，回答
204。只有正在等待、尚未被领取的事件才能被转入死信；其他情况一律是 409 `conflict`。之后可以带 `force` 把它重放。

### 收件箱的限制与状态

`PUT /v1/admin/inboxes/{inbox_id}/config` 替换这个收件箱的覆盖项。省略的字段会回到服务的默认值。

| 字段 | 含义 |
| --- | --- |
| `cap_interactive`, `cap_background`, `cap_batch` | 每条 lane 的并发 run 数，1 到 100 |
| `max_active_runs` | 收件箱级的并发 run 总上限。设为 1 即为严格的“一个收件箱一个 run” |
| `settings` | 每条 lane 的 `quiet_window_ms` 和 `max_wait_ms`，以及合批上限 `max_events` 和 `max_bytes` |

`settings` 里的其他任何键都会以 400 拒绝，超出范围的值也一样：`quiet_window_ms` 和 `max_wait_ms` 取 0 到
86,400,000（一天），`max_events` 和 `max_bytes` 至少为 1。合批上限作用于整个收件箱，不分 lane。

```json
{"tenant_id":"acme","max_active_runs":1,"operator":"alice",
 "settings":{"lanes":{"interactive":{"quiet_window_ms":3000}},"batch":{"max_events":20}}}
```

`GET /v1/admin/inboxes/{inbox_id}?tenant_id=acme` 返回配置以及正在等待的东西。

```json
{"tenant_id":"acme","inbox_id":"u42","max_active_runs":1,
 "settings":{"lanes":{"interactive":{"quiet_window_ms":3000}}},
 "pending":2,"retrying":0,"claimed":0,"dead":0,"active_runs":0,"queued_runs":0,"suspended_runs":0}
```

| 字段 | 统计什么 |
| --- | --- |
| `pending` | 等着被取走的未领取事件 |
| `retrying` | 绑定在某个等待重新下发的 run 上的事件 |
| `claimed` | 被 active 或挂起的 run 持有的事件 |
| `dead` | 超过重试上限或被手工转入死信的事件 |

### 审计日志

`GET /v1/admin/audit?tenant_id=acme&limit=100` 列出运维操作，最新的在前。操作有
`replay`、`manual_dead`、`inbox_config` 和 `timer_cancel`。limit 最大为 1,000。

```json
[{"audit_id":"01M7X00F23Z8EQ3KH18VZ0EM2P","tenant_id":"acme","actor_client_id":"ops","operator":"alice",
  "action":"replay","target":{"event_id":"01M7WZZSCQPP6KTQPVXCBECVHK"},
  "params":{"force":true,"reason":"customer asked for a re-run"},
  "result":{"skipped":0,"replayed":1},"at":"2026-11-20T17:28:01.853319Z"}]
```

## 健康检查与指标

| 路径 | 内容 |
| --- | --- |
| `/actuator/health` | 存活与就绪状态，包含数据库 |
| `/actuator/prometheus` | 指标；见[事件收件箱设计](event-inbox-design.md#可观测性) |

这两个路径不需要签名。

## Java 客户端

`inbox-client` 提供一个不依赖任何框架的纯 Java 客户端。

```java
InboxClient producer = new InboxClient(URI.create("http://127.0.0.1:8080"), "order-bot", "order-bot-secret");
WriteEventResponse accepted = producer.writeEvent("u42",
        WriteEventRequest.builder(EventSource.BIZ_WEBHOOK, "order.paid.v1")
                .sourceEventId("order-1234-v7")
                .mergeKey("order:1234")
                .payloadJson("{\"order_id\":\"1234\",\"amount\":299}")
                .build()).require();
```

一个 worker 就是一个 `ScriptedWorker`，外加一段决定每个 run 如何结束的脚本。

```java
InboxClient runtime = new InboxClient(URI.create("http://127.0.0.1:8080"), "runtime-1", "runtime-secret");
Outbox outbox = new FileOutbox(Path.of("target/outbox"));
ScriptedWorker worker = new ScriptedWorker(runtime, outbox, "worker-1", context -> {
    context.heartbeat();
    context.send("im", "handled " + context.events().size() + " event(s)");
    return new ScriptedWorker.Decision.Done();
});
worker.runOnce();
```
