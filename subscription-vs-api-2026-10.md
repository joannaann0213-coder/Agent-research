# 订阅套餐 vs API 按量计费：同样的 token 值多少钱

> 调研日期：2026-10-30。金额均为美元（USD）。
> 各家都不公布订阅的 token 额度，所以"API 等值"全部来自第三方实测（SemiAnalysis、Wokey）。API 单价来自官方价目表。
> 1 亿 token = 100M token。

## 结论

- **Claude Pro（$20/月）用满一个月 Opus 5.5，约 25–30 亿 token，折合 API 价 ≈ $1,178。** Bedrock 的 global 端点和 Anthropic 官方 API 价格完全一样，所以在 Bedrock 上也是 ≈ $1,178。走 Bedrock 区域端点（+10%）≈ $1,296。**约为月费的 59 倍（区域端点 65 倍）。** 按真实用户的混合流量实测，约 $680/月（34 倍）。
- **Max 5x / Max 20x 每美元的价值和 Pro 基本一样（57–59 倍）。** Max 20x 用满 ≈ $11,726/月，走 Bedrock 区域端点 ≈ $12,900/月。"5x / 20x"只指 5 小时窗口，周额度实测约为 Pro 的 5 倍和 10 倍。
- **ChatGPT Pro $200（Codex）用满 ≈ $2,084/月 的 GPT-6.1 Sol（10.4 倍），或 ≈ $2,897/月 的 GPT-6 Astra（14.5 倍）。** GPT-6 Astra 在 Bedrock 上也有，global 与 OpenAI 同价。10 月 30 日起，所有 Pro $200 都按 9 月 29 日减半后的新额度。
- **同样的月费、中档模型（Opus 5.5 对 GPT-6.1 Sol）：Claude 订阅的 API 等值约为 ChatGPT 的 5.5 倍。** 旗舰模型（Fable 5.1 对 GPT-6 Astra）两家差不多，都在 12–15 倍。
- 在 Cursor 里用 Claude 或 GPT，按 API 标价收费（1 倍，没有补贴）。中国厂商的 coding plan 平均略低于 12 倍。

## 1. API 单价（$ / 百万 token）

| 模型 / 渠道 | 输入 | 缓存写（短） | 缓存写（长） | 缓存读 | 输出 |
|---|---|---|---|---|---|
| Opus 5.5：Anthropic 官方 API | 4.00 | 5.00（5 分钟） | 8.00（1 小时） | 0.20 | 20.00 |
| **Opus 5.5：Bedrock global** | **4.00** | **5.00** | **8.00** | **0.20** | **20.00** |
| **Opus 5.5：Bedrock 区域/geo 端点** | **4.40** | **5.50** | **8.80** | **0.22** | **22.00** |
| Sonnet 5.5：Bedrock global（参考） | 2.00 | 2.50 | 4.00 | 0.10 | 10.00 |
| GPT-6.1 Sol：OpenAI API（≤272K 上下文） | 2.00 | 2.50（30 分钟） | 5.00（12 小时） | 0.10 | 10.00 |
| GPT-6 Astra：OpenAI API = Bedrock global（≤272K） | 10.00 | 12.50（30 分钟） | 25.00（12 小时，仅 OpenAI） | 1.00 | 50.00 |
| GPT-6 Astra：Bedrock 区域端点 | 11.00 | 13.75（30 分钟） | — | 1.10 | 55.00 |

- Bedrock 价格取自 AWS 公开的 Price List API（`AmazonBedrockFoundationModels`，us-east-1，2026-10-12 发布版）。Opus 5.5 global 端点的五项价格与 Anthropic 官方一致；非 global 端点全部 +10%。Anthropic 官方定价页也写明：Claude 4.5 及以后的模型，Bedrock 区域端点比 global 贵 10%。
- 在 Anthropic 官方 API 上指定 `inference_geo: "us"` 同样 ×1.1。OpenAI 的区域处理（数据驻留）端点同样 +10%。
- Opus 5.5 的缓存读只收输入价的 0.05 倍（$0.20），比一般模型的 0.1 倍更便宜。和 Opus 5（$5 / $25）相比，输入和输出降价 20%，缓存读降价 60%。
- GPT-6.1 Sol 目前不在 Bedrock 价目表里，GPT-6 Astra 在。

## 2. 核心表：订阅用满，折合多少 API 美元

"API 等值"取自 SemiAnalysis 2026-10-05 的报告。方法是把每周额度用满，再按他们 9 月统计的 agentic（编码 Agent）token 结构，用 API 价格折算。下面"约合 token"两列是我用该结构和第 1 节单价反推的：区间下限按长 TTL 缓存写计，上限按短 TTL 计。

### 2.1 Claude（Opus 5.5，agentic 负载）

| 套餐 | 月费 | API 等值/月（官方 API = Bedrock global） | 倍数 | Bedrock 区域端点（+10%） | 约合 token/月 | 其中非缓存读 |
|---|---|---|---|---|---|---|
| Pro | $20 | **$1,178** | **58.9x** | $1,296（64.8x） | 25–30 亿 | 0.8–1.0 亿 |
| Max 5x | $100 | **$5,725** | **57.3x** | $6,298（63.0x） | 120–143 亿 | 4.1–4.9 亿 |
| Max 20x | $200 | **$11,726** | **58.6x** | $12,899（64.5x） | 246–294 亿 | 8.4–10 亿 |

Max 20x 换用不同模型或负载时（SemiAnalysis）：

| 模型 | agentic 负载 | chat 负载 |
|---|---|---|
| Opus 5.5 | $11,726（58.6x） | $9,200（46.0x） |
| Sonnet 5.5 | $12,529（62.6x）¹ | $7,009（35.0x） |
| Fable 5.1 | $2,485（12.4x）² | $2,480（12.4x） |

¹ 这个数字用的是 10 月 7 日之前的缓存读价 $0.20。Sonnet 5.5 现在的缓存读价是 $0.10，按新价重算约 $8,400–8,900（42–45x），这是我的估算。
² Fable 5.1 最多只能用掉周额度的 50%，用到这个上限时还剩一半额度可以给其他模型。Pro 不含 Fable 5.1。Max 5x 用 Fable 5.1 ≈ $1,273（12.7x）。

### 2.2 ChatGPT / Codex

| 套餐 | 月费 | GPT-6.1 Sol API 等值/月 | 倍数 | GPT-6 Astra API 等值/月 | 倍数 | Astra 走 Bedrock 区域端点 |
|---|---|---|---|---|---|---|
| Plus | $20 | $211 | 10.6x | $162 | 8.1x | $178 |
| Pro $100 | $100 | $1,055 | 10.6x | $1,322 | 13.2x | $1,454 |
| **Pro $200** | $200 | **$2,084** | **10.4x** | **$2,897** | **14.5x** | $3,187（15.9x） |
| Pro $500 | $500 | ≈ $5,200（按"各档每美元 token 相同"推算） | ≈ 10.4x | $6,955 | 13.9x | $7,651 |

- 约合 token/月：Pro $200 用 GPT-6.1 Sol ≈ 79–104 亿（其中非缓存读 2.7–3.6 亿）；用 GPT-6 Astra ≈ 16–20 亿（非缓存读 0.55–0.67 亿）。
- Sol 走 OpenAI 数据驻留端点（+10%）：Pro $200 ≈ $2,292。
- 背景：9 月 29 日 OpenAI 下调了 Pro $200 的额度（被普遍报道为从 Plus 的 20 倍降到 10 倍，OpenAI 官方没有公布倍数），并新增 $500 档。老订阅者的旧额度保留到 10 月 29 日。调整后 Pro $100、$200、$500 三档每美元的 token 数相同。上表是新额度下的数据。
- 有第三方页面称"10 月 29 日起 Pro $100 和 $500 额度翻倍"，OpenAI 官方定价页没有这项内容，我没能证实。

## 3. 交叉验证：真实账号的实测（Wokey，2026-10-19 至 10-26）

Wokey 用真实订阅账号的流量测算：每周额度涨 10 个百分点以上的那段时间里有多少请求，以此推算一整周的量，再按官方 API 价计价。

| 套餐 | 每周 API 等值 | 折合每月 | 倍数 | 每月总 token | 每月非缓存 token | 每月输出 | 样本 |
|---|---|---|---|---|---|---|---|
| Claude Pro | $157（$142–170） | ≈ $680 | 34x | 6.1 亿（83% 是缓存读） | 1.0 亿 | 740 万 | 103 个账号 |
| Claude Max 20x | $1,602（$1,147–1,745） | ≈ $6,940 | 34.7x | 90 亿（92% 是缓存读） | 7.9 亿 | 5,550 万 | 21 个账号 |
| ChatGPT Plus（Codex） | $55（$41–58） | ≈ $238 | 11.9x | 9.3 亿（96% 是缓存读） | 2,900 万 | 170 万 | 12 个账号 |

Max 5x、Pro $100 和 Pro $200 的样本数还不够，没有发布数据。

**两个独立来源在"非缓存 token"上几乎一致：**

| 套餐 | SemiAnalysis 反推（非缓存 token/月） | Wokey 实测（非缓存 token/月） |
|---|---|---|
| Claude Pro | 0.8–1.0 亿 | 1.0 亿 |
| Claude Max 20x | 8.4–10 亿 | 7.9 亿 |
| ChatGPT Plus | 2,700–3,600 万 | 2,900 万 |

所以，**订阅真正限制的是非缓存 token：Claude Pro 每月约 1 亿，Max 20x 约 8–10 亿，ChatGPT Plus 约 3,000 万。** 两个来源在美元上差得多（59 倍对 34 倍），主要有两个原因：

1. 缓存读的占比不同。SemiAnalysis 假设 96.6%，真实流量是 83–92%。
2. 真实用户会混用多个模型。Wokey 测得 Opus 5.5 每个 token 占用的额度只有其他 Opus / Fable 模型的约 0.6 倍，只用 Opus 5.5 能拿到更多 token。

## 4. 倍数为什么这么大，又为什么差这么多

1. **订阅里缓存读几乎不计额度。** SemiAnalysis 的方法是：灌进 5 亿缓存读 token 后，如果额度计量条还不动，就把缓存读按免费处理。Wokey 对 Claude 的实测结果一致：缓存写占用的额度和普通输入差不多，缓存读几乎不占，输出占用约为输入的 4–7 倍。
2. **但 API 对缓存读照样收费，而 agentic 编码有 96.6% 的 token 是缓存读。** SemiAnalysis 的两种负载结构如下：

   | 负载 | 新输入 | 缓存读 | 缓存写 | 输出 |
   |---|---|---|---|---|
   | agentic | 0.4% | 96.6% | 2.6% | 0.3% |
   | chat | 2% | 75% | 13% | 10% |

   拆开 Claude Pro 的 $1,178（按 5 分钟缓存写计）：缓存读约占 48%（约 $570），缓存写约 33%，输出约 15%，新输入约 4%。按 1 小时缓存写计，缓存读约占 40%，缓存写约 44%。
3. **结论：你的负载越"agentic"，订阅越划算。** 长上下文、反复读缓存的编码 Agent 倍数最高。纯聊天或大量输出的场景倍数会下降，例如 Max 20x 用 Opus 5.5 做 chat 是 46 倍。
4. **补贴力度因模型而异。** SemiAnalysis 估算：Anthropic 对 Opus 5.5 补贴极高，满额使用时毛利约 −369%；对 Fable 5.1 基本不补贴，满额使用时毛利约 1%。按平均 20% 利用率计算，分别约为 6% 和 80%。OpenAI 9 月 29 日调整之后，Pro 各档用 Sol 约 10.5 倍，用 Astra 约 13–15 倍；Plus 用 Astra 只有 8 倍。

## 5. 其他订阅

| 产品 | 价格 | 用 Claude / GPT 的倍数 | 说明 |
|---|---|---|---|
| Cursor（Plus $8 / Premium $30 / Super $100 / Ultra $200） | — | **1x** | 订阅池只覆盖 Cursor / Grok 自家模型。Opus 5.5、GPT-6.x 等第三方模型不含额度，按 API 标价按量计费。Teams / Enterprise 每百万 token 另加 $0.25 |
| 中国厂商 coding plan（GLM / Kimi / MiniMax 等） | 见下 | 平均略低于 12x（SemiAnalysis） | 档位越高越划算。各家、各模型的细分数据在付费墙后 |
| MiniMax M Plan（原 "Token Plan"，10 月 15 日更名） | Go $22 / Explore $55 / Build $132 | — | 唯一直接按 token 给额度的：更名前 Plus / Max / Ultra 约 17 亿 / 51 亿 / 125 亿 token 每月；新档位为 Go 的 1 / 3 / 7.5 倍。折合多少倍取决于缓存占比和 M3 是否仍按首发折扣价，没有可靠实测 |
| 智谱 GLM Coding Plan | Lite $18 / Pro $80 / Max $168 | — | 按 credits 计：每 5 小时 2,000 / 12,000 / 28,000，每周 1 万 / 6 万 / 14 万 |
| Kimi 会员（含 Kimi Code） | ¥99 / ¥199 / ¥699 | — | 5 小时滚动窗口；Pro 为 4 倍 Agent 额度，Max 为 14 倍 |
| 阿里 Qwen（百炼） | ¥39 / ¥139 / ¥499 | — | 每月 11,500 / 45,000 / 180,000 credits |
| Google AI Pro / Ultra | $19.99 / $99.99 / $199.99 | 未知 | Google 不公布额度，也没有找到可靠实测 |

中国厂商的价格和额度来自第三方汇总站 codingplan.org（2026-09-24 更新），以官网为准。

## 6. 局限与注意事项

1. **都是第三方实测，而且会变。** 厂商会悄悄调整额度，也会做 A/B 测试：SemiAnalysis 发现三个配置相同的账号里，有一个的额度低约 20%。时间线：Claude Code 夏季周额度 +50% 的活动 9 月 13 日结束；Opus 5.5 于 9 月 22 日发布，同时提高了 5 小时额度（Opus 在 Pro 上约 +50%，Max 上约 +20%）；OpenAI 的变化见 2.2 节。
2. **"API 等值"是上限，不是你实际能省下的钱。** 它假设每周都把额度用满，而且改用 API 后还会用同样多的 token。真按量付费时，人会省着用、换更便宜的模型。
3. **Max 的 5x / 20x 只针对 5 小时窗口。** 周额度约为 Pro 的 5 倍和 10 倍，所以每美元价值和 Pro 一样。想要"用得猛"选 Max；单论每美元价值，Pro 不吃亏。
4. **Batch API（五折）不适合交互式编码，没有纳入比较。** 企业私有折扣（Bedrock private offer / EDP）会进一步降低 API 侧成本。

## 7. 用自己的数据算

Opus 5.5 在 Bedrock global（= Anthropic 官方 API）上的月成本，单位为百万 token 和美元：

```
月成本 = 新输入 × 4 + 5 分钟缓存写 × 5 + 1 小时缓存写 × 8 + 缓存读 × 0.20 + 输出 × 20
Bedrock 区域端点：再 × 1.1
倍数 = 月成本 ÷ 月费
```

四类 token 的用量可以从 Claude Code 的本地会话日志里取。日志里每条请求都有 `input_tokens`、`cache_creation_input_tokens`、`cache_read_input_tokens`、`output_tokens` 四个字段，ccusage 这类工具会直接按 API 价汇总。

## 来源

- Anthropic 官方定价：<https://platform.claude.com/docs/en/about-claude/pricing>
- AWS Bedrock 公开价目（Price List API，us-east-1，2026-10-12 版）：<https://pricing.us-east-1.amazonaws.com/offers/v1.0/aws/AmazonBedrockFoundationModels/current/us-east-1/index.json>；定价页：<https://aws.amazon.com/bedrock/pricing/>
- OpenAI API 定价：<https://developers.openai.com/api/docs/pricing>；ChatGPT / Codex 套餐：<https://learn.chatgpt.com/docs/pricing>
- SemiAnalysis 原文（付费墙）：<https://newsletter.semianalysis.com/p/anthropic-subscriptions-offer-5x>
- SemiAnalysis 数据的公开转述：<https://explainx.ai/blog/semianalysis-claude-vs-chatgpt-subscription-api-value-limit-testing-2026>、<https://1023jack.com/market/the-5x-is-a-subsidy-not-a-price-what-semianalysis-found-inside-ai-subscriptions/>、<https://www.implicator.ai/claude-opus-plans-5x-openai-sol-api-value/>
- Wokey 实测：Claude <https://wokey.ai/claude-usage-limits>、Codex <https://wokey.ai/codex-usage-limits>
- Pro $200 额度下调：<https://apidog.com/blog/codex-pro-limit-cut-october-30/>；Codex 额度汇总：<https://simplemetrics.xyz/chatgpt-codex-limits-2026/>
- Cursor：<https://cursor.com/pricing>、<https://cursor.com/docs/account/pricing>
- 中国厂商 coding plan：<https://codingplan.org/en>；MiniMax M Plan 更名：<https://www.usagepricing.com/blueprint/activity/minimax-2026-10-15-m-plan-replaces-token-plan>
