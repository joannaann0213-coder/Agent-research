# Review: analysis quality and calculations

**Report reviewed:** `reports/DoorDash 美团 Agent 商业模式三要素.md` (《谁为外卖 Agent 买单：DoorDash 与美团六个场景的三要素检验》, 655 lines, commit 3efddde). The report was not edited.

**Review date:** 2026-11-04.

**Inputs:**
- Research notes in this folder: `analysis_toolkit_unit_economics.md`, `doordash.md`, `meituan.md`, `agentic_commerce_channels.md`, `merchant_agent_market.md`, `working_files/inference_cost_estimate.py`.
- The earlier report `reports/Agent 商业模式三要素案例分析.md`.
- `subscription-vs-api-2026-10.md`.

**Script:** `review/recompute_review.py`, standard library only. Run it with `python3 recompute_review.py`.

**Severity scale:**
- *Critical:* a headline conclusion is wrong.
- *Major:* a headline number, verdict or comparison is materially misleading or internally inconsistent; the conclusion survives only with qualification.
- *Minor:* labelling, wording, rounding, or a missing caveat.

**Replacement text:** Chinese replacements use the report's own punctuation: ASCII double quotes, the U+2212 minus sign "−", and en-dash ranges.

---

## 0. Bottom line

1. **The arithmetic is clean.** Every derived figure (45 rows in Section 1) reproduces from the stated inputs within rounding. I found no USD/RMB conversion error. $1 ≈ ¥7.1 is applied consistently and only in the unit-economics sections, as the header says.

2. **The problems lie in what the arithmetic is applied to.**
   - **Channel fees, logic (F1).** The §6.3 channel-fee table contradicts the report's own §1 incremental P&L.
     - The "3.1% break-even fee" for shifted orders is not a break-even, because any fee on a shifted order is a pure loss.
     - The incremental-order column ignores the ad loss that §3.3 says third-party channels cause. At a 4% fee, a new customer's order goes to −$0.26, not +$0.42.
   - **Channel fees, base (F2).** The fee is computed on GOV, which includes tips, taxes and consumer fees, while every precedent fee is charged on the sale.
     - On a 70%–80% food-subtotal base, a 4% fee is $0.96–1.09 per order, roughly equal to the $1.07 Adj. EBITDA. The headline "从赚 $1.07 变成亏 $0.30" depends on the base.
     - The S2 conclusion holds only once the ad loss is added.
   - **Meituan per-order profit (F3).** The ¥1/order base sits above every post-war estimate, and the report's own §2.2 heading says "¥0 上下".
     - At ¥0.4, a discovery session on DeepSeek Flash or LongCat eats 77%–83% of per-order profit, not "约三分之一".
     - The fee break-even is 0.8%–1.3%, not 2%–3%.
     - Every Meituan conclusion keeps its direction; the magnitudes are 2–3× off.
   - **Break-even thresholds (F4).** The "约 3% / 约 22%" thresholds are single points.
     - They omit the basket-lift term of the report's own formula. A 5% lift cuts 22% to 9%.
     - They assume ads are 2% of GOV, but DoorDash's own last data point implies about 1.0%–1.25%.
     - They are as sensitive to the error rate as to ad loss. With small models, plausible inputs give thresholds from below 0% to about 66%.
   - **Evidence and coherence.**
     - "几百单" counts orders from small startup agents (Muse, Instinct, Fo), not ChatGPT or Gemini (F5).
     - S3 "已经成立" rests only on DoorDash's own claims (F6).
     - The 结论's thesis ("卡住价值获取的…是广告和通道费") and its "最难的是 C9 和 C5" do not match §8–§11 (F7, F8).

3. **Prices.**
   - Google, OpenAI and DeepSeek prices in the notes match the official pages fetched on 2026-11-04. The GPT figures, which came from an aggregator, are now first-party verified.
   - Claude input and output prices match the coordinator's Anthropic reference.
   - Sonnet 5.5 cache reads are $0.20 per that reference, not the $0.10 the notes used. Recomputed, the Sonnet discovery row moves from $0.35 to $0.36 per order (32% → 34% of EBITDA) and the complex session from $1.80 to $1.85.
   - The repo's earlier report records a 2026-10-07 cut to $0.10, which postdates the reference by one day.
   - Haiku 5.5's cache-read price is unverified.
   - **No conclusion changes.**

4. **What survives:**
   - Inference is small next to ads when routed to small models.
   - A fee on shifted orders destroys value.
   - Fulfilment is the moat.
   - S4 and S5 are unproven.
   - Token selling is not a profit path.

   **What needs qualification:**
   - The S3 "成立" verdict.
   - S2 acquisition-channel economics.
   - All Meituan magnitudes.
   - The 3%/22% thresholds.
   - The "hardest conditions" claim.

**No finding is critical. Eight are major (F1–F8) and twelve minor (F9–F20).**

---

## 1. Recomputation table

The report's own rounded inputs are used to check its arithmetic: $34.2 GOV per order, $1.79 CP, $1.07 Adj. EBITDA. Unrounded inputs change nothing.

| # | Section | Figure as stated in the report | Recomputed | Status |
|---|---|---|---|---|
| R1 | §2.1 | GOV per order $34.2 | $34.14 from 339亿 ÷ 9.93亿; $34.18 from the notes' unrounded GOV | OK (rounding) |
| R2 | §2.1 | Revenue per order $4.65; net revenue margin 13.6% | $4.653; 13.63% | OK |
| R3 | §2.1 | CP per order $1.79; CP 5.2% of GOV | $1.793; 5.25% from rounded inputs (company reports 5.2%) | OK |
| R4 | 结论, §2.1 | Adj. EBITDA per order $1.07; 3.1% of GOV | $1.0675; 3.13% | OK |
| R5 | §2.1 | GAAP net income per order $0.47 | $0.469 | OK |
| R6 | §3.2 | 每天约 1,080 万单 | 993M ÷ 92 days = 10.79M | OK |
| R7 | §6.1 | 前三季度约 29 亿单；几百单约千万分之一 | 2.896B; 200–900 orders = 0.7–3.1 × 10⁻⁷ | OK as order of magnitude; attribution wrong → F5 |
| R8 | §2.2 | CLC margins 约 21% / −20.9% / 约 −2.6% / −3.2% / 7.9% | 20.95% / −20.92% / −2.65% / −3.17% / 7.93% | OK |
| R9 | §2.2 | 2025 mix 38.0% / 36.8% / 19.7%; 其他 141 亿元 | 38.04% / 36.83% / 19.73%; 140.6亿 | OK |
| R10 | §2.2 | 佣金加广告约 58% | 57.77% | OK |
| R11 | 结论, §2.2 | 2024 年广告 = CLC 经营利润的 93% | 488.4 ÷ 524.2 = 93.17% | Arithmetic OK; framing → F10 |
| R12 | 结论, §2.2, §11.3 | 美团外卖每单经营利润 −¥0.3 到 ¥1.5（2026 券商估算） | Inputs match the notes, but ¥1.5 is Bernstein's 2025-06 pre-war forecast. Post-war 2026 estimates run −¥0.3 to ¥0.5. Q2 2026 implied: ¥0.04–0.05 (MS forecast) or ¥0.27–0.36 (from actual CLC profit) | Problem → F3 |
| R13 | §2.3 | Uber 3.8%; Instacart 3.2% / 3.1%; Grab 2.7%; Swiggy 3.4% | 3.84%; 3.25% / 3.14% ("advertising and other"); 2.73%; 3.39% | OK; label → F20 |
| R14 | 结论, §5.3 | 小模型推理 $0.02/单，占每单 EBITDA 2% | $0.0180; 1.7% | OK |
| R15 | §5.3 | Gemini 3.8 Flash $0.14, 13% | $0.135, 12.6% (2027 price: $0.271, 25.3%) | OK; 2027 price not carried → F9 |
| R16 | §5.3 | Sonnet 5.5 $0.35, 32% | $0.361, 33.7% at the verified $0.20 cache read ($0.346, 32.4% at $0.10) | Problem (minor) → F9 |
| R17 | 结论, §5.3 | 广告挤占 $0.17–0.34，占 16%–32% | $0.171–0.342; 16.0%–32.0% | Arithmetic OK; ad-rate range → F4 |
| R18 | §5.3 | 错误赔付 $0.04–0.37，占 3%–35% | $0.037–0.372; 3.5%–34.8% | OK |
| R19 | §5.3, §14.2 | 丢 3% 以上广告即超过推理费 | k* = 2.6% at ads 2%; 1.7%–4.2% at ads 3.1%–1.25%; 4.5% at ads 2% if Haiku cache reads get no discount | OK; state as a range |
| R20 | §5.3 | 复杂会话：小模型 $0.09；Sonnet $1.80，超过 CP | $0.092; Sonnet $1.850 at $0.20 ($1.800 at $0.10) vs CP $1.79 | → F9 |
| R21 | 结论, §5.3 | 门槛 3% / 10% / 13% / 19% / 22% / 29% | 3.1 / 9.6 / 12.6 / 19.2 / 22.2 / 28.7% | Arithmetic OK; omits basket term, single points → F4 |
| R22 | §5.3 | 美团复购会话 约 1% / 2% / 2% | 0.8% / 2.0% / 1.9% at ¥1; 2.1% / 5.0% / 4.7% at ¥0.4 | OK at ¥1; base → F3 |
| R23 | §5.3 | 美团发现会话 约 13% / 33% / 31% | 12.8% / 33.0% / 31.0% at ¥1; 32.0% / 82.5% / 77.4% at ¥0.4 | OK at ¥1; base → F3 |
| R24 | §5.3 | 每丢 10% 广告 ≈ 51 亿元 ≈ Q2 年化 CLC 经营利润的 23% | ¥51.5亿; 22.7% | OK; period and revenue-vs-profit mix → F11 |
| R25 | 结论, §6.3 | 4%：每单 $1.37；转移单 −$0.30；增量单 +$0.42 | $1.368; −$0.298; +$0.422. Incremental order with ads lost: −$0.262. Shifted order vs in-app: −$2.05 | Arithmetic OK; logic → F1; base → F2 |
| R26 | §6.3 | 7%：$2.39；−$1.32；−$0.60 | $2.394; −$1.324; −$0.604 | OK |
| R27 | §6.3 | 12%：$4.10；−$3.03；−$2.31 | $4.104; −$3.034; −$2.314 | OK |
| R28 | §6.3, §12.3 | 盈亏平衡费率 3.1% / 1.1% / 5.2% | 3.13% / 1.13% / 5.23%; incremental with ads lost 3.23% | Arithmetic OK; concept → F1 |
| R29 | 结论, §6.3 | 美团盈亏平衡费率约 2%–3%（2.0%–3.3%） | 2.00%–3.33% at ¥1; 0.80%–1.33% at ¥0.4; negative at −¥0.3 | OK at ¥1; base → F3 |
| R30 | §6.3 | 12% → 每单 ¥3.6–6.0，是该单利润的 3.6–6 倍 | ¥3.6–6.0; 3.6–6.0× at ¥1; 9–15× at ¥0.4 | OK at ¥1; base → F3 |
| R31 | §6.3 | 千问每单补贴上限约 ¥25 | ¥25.0 | OK |
| R32 | §7.3, §11.1 | 每商户每月推理 $0.2–1.2 | $0.164 (DeepSeek peak) to $1.260 (Sonnet at $0.20); $1.230 at $0.10 | Becomes $0.2–1.3 → F9 |
| R33 | §7.3 | $450/月 = $5,400/年 ≈ 销售额 0.5%，税前利润的 14%–19%；每月约 23 单 | 0.54%; 13.5%–19.3%; 23.1 orders | OK |
| R34 | 结论, §8.2, §11.2 | 每单省 $0.10 ≈ 每季 9,900 万 / 9,930 万美元；净收入率 +0.29 pp | $99.3M; 0.293 pp; 9.4% of Q3 Adj. EBITDA | OK; inconsistent rounding → F13 |
| R35 | §8.2 | 机器人 1%（990 万单）× $3–7 = 3,000–7,000 万美元 = EBITDA 的 3%–7%；约 6,600 台 | 9.93M; $29.8–69.5M; 2.8%–6.6%; 6,620 robots | Arithmetic OK; $3–7 undocumented → F13 |
| R36 | §8.2 | 美团每单省 ¥0.05 ≈ 每年约 13 亿元 ≈ CLC 经营利润的 6%（日均 7,000 万单） | ¥12.8亿, 5.6%. At 65M–85M orders/day: ¥11.9–15.5亿, 5.2%–6.8% | OK |
| R37 | 结论, §8.2 | 无人机 100 万单不到一天外卖量的 2% | 1.18%–1.54% | OK |
| R38 | §9 | 客服每单 $0.03–0.11；年化 39.7 亿单；1.2–4.3 亿美元；占年化 EBITDA 3%–10% | $0.031–0.108; 3.972B; $122–428M; 2.9%–10.1% | OK; labour-cost assumption → F19 |
| R39 | §9 | DoorDash 研发占 GOV 1.5%；美团研发占收入 7.3% | DoorDash R&D ≈ $508M = 11.0% of revenue | Different bases → F11 |
| R40 | §10 | LongCat ¥9.9 = 5,000 万 token，约 ¥0.2/百万 | ¥0.198/M; ¥399/10亿 = ¥0.399/M | OK |
| R41 | §10 | DeepSeek Flash 高峰价约 ¥2.1 / ¥8.5 | $0.30 × 7.1 = ¥2.13; $1.20 × 7.1 = ¥8.52 (the page lists USD only) | OK; label as converted |
| R42 | §10 | Fin $0.99 对自建 $0.05：毛利约 95% | 94.9% | OK |
| R43 | §11.2 | 小模型推理约占每单利润 2%；广告挤占 16%–32% | 1.7%; 16%–32% | OK |
| R44 | §11.1, §11.2 | 4% 通道费超过每单 EBITDA | $1.37 > $1.07 on GOV; $0.96–1.09 on a 70%–80% subtotal | Base-dependent → F2 |
| R45 | header | $1 ≈ ¥7.1 used only in unit economics | Holds: used for Haiku, DeepSeek→¥ and the merchant table only | OK |

---

## 2. Model price verification

**Method:**
- Google, OpenAI and DeepSeek pages were fetched on 2026-11-04.
- Claude prices were not re-fetched, at the coordinator's instruction. They come from the coordinator's Anthropic model reference, cached 2026-10-06, with first-party API rates.

| Model | Price in notes / script ($ per 1M: input / cached / output) | Verified price and source | Status | Effect on the report |
|---|---|---|---|---|
| Claude Haiku 5.5 | 0.10 / 0.01 / 0.50 | 0.10 / not listed / 0.50 for prompts ≤100K tokens; 0.50 / 2.50 above (coordinator's Anthropic reference) | Input and output OK. **Cache read unverified** | At a $0.05–0.10 cache read: $0.024–0.031 per order; k=0 threshold 3.4%–3.8% instead of 3.1%. No conclusion changes. Archetype calls are ≤20K tokens, so the >100K tier does not apply |
| Claude Sonnet 5.5 | 2.00 / **0.10** / 10.00 | 2.00 / **0.20** / 10.00 (coordinator's Anthropic reference) | Input and output OK. **Cache read is $0.20: recomputed.** On record: the repo's earlier report says it fell to $0.10 on 2026-10-07 (Bedrock global list of 2026-10-12, which that report says equals first-party), one day after the reference was cached | Discovery $0.346 → $0.361 per order (32% → 34%); complex $1.80 → $1.85; merchant $1.23 → $1.26 per month. No conclusion changes (F9) |
| Claude Opus 5.5 | 4.00 / 0.20 / 20.00 | 4.00 / 0.20 / 20.00 (coordinator) | OK | Not used in the report; frontier sensitivity $0.69 per order (F9) |
| Gemini 3.8 Flash | 0.75 / 0.075 / 3.75 to 2026-12-31; 1.50 / 0.15 / 7.50 from 2027-01-01 | Same, at aistudio.google.com/docs/pricing (ai.google.dev redirects there). Cache storage $0.50 → $1.00 per 1M tokens per hour | OK, doubling confirmed | 2027 price: $0.271 per order (25% of EBITDA); mid-tier thresholds 17% / 27% / 36% (F9). Storage fee immaterial at session-length TTLs |
| Gemini 3.5 Flash-Lite | 0.30 / 0.03 / 2.50 | Same (2026-11-04) | OK | Not used in the report |
| GPT-5.4 mini | 0.75 / 0.075 / 4.50 (aggregator) | Same, Standard tier, developers.openai.com/api/docs/pricing (openai.com/api/pricing returns 403) | OK, now first-party verified | Not used in the report |
| GPT-5.5 | 5.00 / 0.50 (assumed) / 30.00 (aggregator) | 5.00 / 0.50 / 30.00, Standard, prompts <272K; 10.00 / 1.00 / 45.00 above 272K | OK; the cached price is official, not assumed | Not used in the report; frontier sensitivity $0.97 per order (F9) |
| DeepSeek V4.1 Flash | Peak 0.30 / 0.006 / 1.20; off-peak half | `deepseek-flash` = DeepSeek-V4.1-Flash-1017. Peak 0.30 / 0.006 / 1.20, off-peak half. Peak hours are 01:00–04:00 and 06:00–10:00 UTC, Mon–Fri (09:00–12:00 and 14:00–18:00 Beijing). The page lists USD only | OK | The report's "约 ¥2.1 / ¥8.5" is a USD→RMB conversion; label it. Weekends and Beijing 12:00–14:00 and after 18:00 are off-peak, so peak pricing is conservative for 外卖 |
| LongCat-2.0 promo | ¥2 / ¥0.04 / ¥8 | Not checked: third-party source (80AJ), outside the four providers in scope | — | — |

**Do any conclusions change?** No. The largest price-driven change is +$0.014 per order on the Sonnet discovery row. The Gemini 2027 doubling matters more than any price error; F9 asks for it to be carried into the threshold table.

---

## 3. Findings

### F1 (major). §6.3's channel-fee table contradicts the §1 incremental P&L

**Location:**
- §1: "两条口径。第一，真增量的订单按每单贡献利润算；只是换了入口的订单按 EBITDA 级利润算。"
- §6.3 table header: "| 通道费率 | 每单通道费 | 转移单：原 Adj. EBITDA $1.07 变为 | 增量单：原 CP $1.79 变为 |"
- §6.3 table rows: "| 4%（OpenAI 先例，已撤回） | $1.37 | −$0.30 | +$0.42 |" and "| 盈亏平衡费率 | — | 3.1%；若该单广告全丢，约 1.1% | 5.2% |"
- §6.4: "③ 作收费站不成立，任何超过约 3% 的费率都让转移单亏钱；"
- §12.3, S2 row: "费率低于每单利润率（DoorDash 转移单约 3.1%，丢广告时约 1.1%，增量单约 5.2%；美团外卖约 2%–3%）"

**Problem:**
1. **Shifted orders.** In the §1 formula a shifted order adds no profit; it only carries the fee and the ad loss. The notes say so directly: "A channel-shifted order is *always* dilutive by f × AOV + displaced ads" (`analysis_toolkit_unit_economics.md` §2.3).
   - So the incremental break-even fee for a shifted order is 0%, not 3.1%. 3.1% is only the fee at which that order's fully loaded EBITDA reaches zero: an accounting line, not a break-even.
   - §12.3 turns 3.1% into a condition. That tells the reader a 2% fee on repeat orders is acceptable. It is not: at 2%, DoorDash pays $0.68 in fees and can lose another $0.68 of ads on every order that would have come through the app anyway.
   - The §1 rule "只是换了入口的订单按 EBITDA 级利润算" causes the confusion. It comes from the notes (§2.1), but the report's own §5.3 threshold formula, correctly, gives shifted orders zero benefit.
2. **Incremental orders.** The incremental-order column uses the full CP of $1.79, which includes average ad revenue (about $0.68 at 2% of GOV). §3.3's value-chain table says ad exposure in licensed third-party channels is "基本丢失".
   - An incremental order that arrives through ChatGPT carries no sponsored-listing revenue, so its contribution is about $1.11.
   - Its break-even fee falls from 5.2% to 3.2%. At 4% it goes to −$0.26, not +$0.42.

**Evidence** (script §5, GOV base, ads 2%):

| Fee | Shifted vs in-app (fee + ads lost) | Shifted, full EBITDA | Incremental, ads kept | Incremental, ads lost |
|---|---|---|---|---|
| 4% | −$2.05 | −$0.30 | +$0.42 | −$0.26 |
| 7% | −$3.08 | −$1.32 | −$0.60 | −$1.29 |
| 12% | −$4.79 | −$3.03 | −$2.31 | −$3.00 |
| Break-even | 0% (any fee loses) | 3.1% (1.1% with ads lost) | 5.2% | 3.2% |

**Does a conclusion change?** "作收费站不成立" gets stronger. "作获客渠道有条件成立" survives only on lifetime value: at 4%, a new customer's first order is itself loss-making unless ads survive, so payback has to come from repeat orders pulled back into the app. §6.4's condition already says this, but the §6.3 table does not show it.

**Suggested replacements:**
- §1, replacing "两条口径。第一，真增量的订单按每单贡献利润算；只是换了入口的订单按 EBITDA 级利润算。":
  > 两条口径。第一，真增量的订单按每单贡献利润（CP）计入收益；只是换了入口的订单不带来新利润，只承担推理、被挤掉的广告和通道费，所以对它而言任何通道费都是净损失。第 6.3 节另列的"转移单全口径 EBITDA"只说明该单还赚不赚钱，不是增量的盈亏平衡点。
- §6.3, the whole table (GOV base, as in the report; F2 adds the subtotal base in the note):
  > | 通道费率 | 每单通道费 | 转移单：相对在 App 内下单少赚（通道费 + 广告全丢） | 转移单：全口径 Adj. EBITDA（原 $1.07） | 增量单：CP（原 $1.79；保留广告 / 广告全丢） |
  > |---|---|---|---|---|
  > | 4%（OpenAI 先例，已撤回） | $1.37 | −$2.05 | −$0.30 | +$0.42 / −$0.26 |
  > | 7%（Citrini 假设情景） | $2.39 | −$3.08 | −$1.32 | −$0.60 / −$1.29 |
  > | 12%（豆包酒店渠道费） | $4.10 | −$4.79 | −$3.03 | −$2.31 / −$3.00 |
  > | 盈亏平衡费率 | — | 0%：任何费率都是净损失；不收费也因广告丢失少赚约 $0.68 | 3.1%；若该单广告全丢，约 1.1% | 5.2% / 3.2% |
- §6.4, replacing "③ 作收费站不成立，任何超过约 3% 的费率都让转移单亏钱；":
  > ③ 作收费站不成立：对转移单，任何通道费都是净损失，费率超过约 3% 时连该单的全口径 EBITDA 也转负；
- §12.3, S2 row, replacing the condition cell (this also includes F3's Meituan range):
  > 通道费只按获客成本付，只付新客首单；复购拉回自有入口，因为转移单上的任何费率都是净损失；新客首单的费率要低于该单扣掉广告损失后的贡献率（DoorDash 按 GOV 计约 3.2%，若广告保留约 5.2%；美团外卖按每单利润 ¥0.3–1.0 计约 0.6%–3.3%），否则只能靠复购回本

---

### F2 (major). The channel fee is charged on GOV, which includes tips, taxes and consumer fees

**Location:**
- 结论, bullet 2: "**第三方 Agent 的通道费，比每单利润还高。**" and "按 4% 算，DoorDash 一笔本来就会发生的订单从赚 $1.07 变成亏 $0.30；美团外卖的盈亏平衡费率只有约 2%–3%（均为估算）。"
- §6.3 note: "按 Q3 2026 每单 GOV $34.2 计。"
- §11.2, S2 cell: "4% 通道费超过每单 EBITDA"

**Problem:**
- DoorDash's Marketplace GOV "includes taxes, tips and consumer and membership fees" (Q3 2026 10-Q; `doordash.md` Q0).
- Every precedent the report cites is charged on the merchant's sale or the order value:
  - OpenAI's 4% was "of each Instant Checkout sale".
  - Citrini's 7% is a cut of the restaurant order.
  - Doubao's 12% is on the hotel order.
- An agent fee on food delivery would most likely be charged on the food subtotal, not on Dasher tips, sales tax and DoorDash's own fees.
- DoorDash does not disclose the subtotal share. At an assumed 70%–80% of GOV:
  - a 4% fee is $0.96–1.09 per order, about equal to the $1.07 Adj. EBITDA;
  - the shifted order lands at +$0.11 to −$0.02, not −$0.30.
- So "通道费比每单利润还高" holds on a GOV base but not on a subtotal base.
- The S2 conclusion stays robust only because of the report's own observation that third-party orders also lose ads. With ads lost, the shifted order lands at −$0.57 to −$0.71 on a subtotal base, or −$0.98 on GOV.

**Evidence** (script §5, 4% fee, ads 2%):

| Fee base | Fee per order | Shifted EBITDA, ads kept | Shifted EBITDA, ads lost | Incremental CP, ads kept | Incremental CP, ads lost |
|---|---|---|---|---|---|
| Subtotal at 70% of GOV | $0.958 | +$0.112 | −$0.572 | +$0.832 | +$0.148 |
| Subtotal at 80% of GOV | $1.094 | −$0.024 | −$0.708 | +$0.696 | +$0.012 |

On a subtotal base, the shifted-order EBITDA break-even fee is 3.9%–4.5% of the subtotal.

**Suggested replacements:**
- 结论, bullet 2 heading, replacing "**第三方 Agent 的通道费，比每单利润还高。**":
  > **第三方 Agent 的通道费加上丢失的广告，超过每单利润。**
- 结论, bullet 2, replacing "按 4% 算，DoorDash 一笔本来就会发生的订单从赚 $1.07 变成亏 $0.30；美团外卖的盈亏平衡费率只有约 2%–3%（均为估算）。" (combines F1 and F3):
  > 按 4% 算（按餐费小计计，假设小计占 GOV 70%–80%，每单约 $0.96–1.09；按 GOV 计 $1.37），一笔本来就会在 DoorDash 下的订单要多付一笔与每单 Adj. EBITDA（$1.07）相当的费用，再丢掉约 $0.68 的广告，全口径从赚 $1.07 变成亏 $0.6–1.0；对这类订单，任何通道费都是净损失。美团外卖按每单利润 ¥0.3–1.0、客单价 ¥30–50 计，可承受的费率只有约 0.6%–3.3%（均为估算）。
- §6.3 note, replacing "按 Q3 2026 每单 GOV $34.2 计。":
  > 按 Q3 2026 每单 GOV $34.2 计。GOV 含小费、税和消费者费用；若通道费只按餐费小计收（假设小计占 GOV 70%–80%），每单费额要乘以 0.7–0.8：4% 时为 $0.96–1.09，转移单全口径为 +$0.11 到 −$0.02，叠加广告全丢则为 −$0.57 到 −$0.71。
- §11.2, S2 cell, replacing "4% 通道费超过每单 EBITDA":
  > 4% 通道费约等于（按小计）或超过（按 GOV）每单 EBITDA；叠加广告丢失，转移单必亏

---

### F3 (major). Meituan's ¥1 per-order profit is above every current estimate

**Location:**
- 结论: "美团外卖每单经营利润，券商对 2026 年的估算在 −¥0.3 到 ¥1.5 之间。"
- §5.3: "假设：每单利润取 ¥1（券商估算区间为 −¥0.3 到 ¥1.5）；" and the Meituan inference table.
- §5.3: "长的发现会话即使用国产便宜模型，也要吃掉每单约三分之一的利润。"
- §5.3, business-lines table: "| 外卖 | 每单约 −¥0.3 到 ¥1.5（券商估算） |"
- §5.4: "美团外卖的每单利润只有约 ¥1"
- §6.2: "到店酒旅的分部利润率约 25%，外卖每单只有约 ¥1（券商估算）。"
- §6.3: "**美团外卖：** 按每单利润 ¥1、客单价 ¥30–50 计 … 可承受的通道费上限约 2.0%–3.3%（估算）。按豆包酒店的约 12% 收，每单要付 ¥3.6–6.0，是该单利润的 3.6–6 倍（估算）。"
- §14.2: "美团每单 ¥1"

**Problem:**
- The ¥1.5 endpoint is Bernstein's June-2025 forecast, made before the 外卖大战.
- Post-war 2026 estimates in `meituan.md` §Q1-C:
  - J.P. Morgan: ¥0.4–0.5 for H2 2026.
  - UBS: −¥0.3 for FY2026, Q2 "near breakeven".
  - Morgan Stanley's Q2 2026 food-delivery forecast of +¥3.13亿 implies ¥0.04–0.05 per order at 65M–85M orders a day.
- Triangulating from the actual Q2 CLC operating profit (¥56.7亿), less MS's estimates for 到店酒旅 (¥43亿) and 闪购 (−¥7.5亿), leaves about ¥21亿 for food delivery, or ¥0.27–0.36 per order.
- Management guided Q3 UE weaker than Q2.
- Sanity check: ¥1 × 65M–85M × 365 = ¥237–310亿 a year from food delivery alone. That exceeds the whole segment's Q2 annualized operating profit of ¥227亿, which also contains about ¥172亿 a year of 到店酒旅 profit at MS's estimate.
- The report's own §2.2 heading says "外卖每单利润在 ¥0 上下".
- So ¥1 overstates the current base about 2–3×.

**Evidence** (script §6), at ¥0.4:

| Session | Claude Haiku 5.5 | DeepSeek Flash (peak) | LongCat (promo) |
|---|---|---|---|
| Reorder | 2.1% | 5.0% | 4.7% |
| Discovery | 32.0% | 82.5% (off-peak 41.2%) | 77.4% |

- Fee break-even: 0.80%–1.33% (0.60%–1.00% at ¥0.3; negative at −¥0.3).
- Doubao's 12% fee equals 9–15× per-order profit.

**Does a conclusion change?** No. Every Meituan conclusion keeps its direction and gets stronger:
- Only short reorder sessions are affordable.
- Discovery sessions need off-peak or cheaper models.
- Meituan cannot pay any meaningful channel fee on 外卖.

The magnitudes ("约三分之一", "2%–3%", "3.6–6 倍") need replacing.

**Suggested replacements:**
- 结论, replacing "两家每单都只赚约 1 美元或 1 元：DoorDash Q3 2026 每单 Adj. EBITDA 约 $1.07（计算）；美团外卖每单经营利润，券商对 2026 年的估算在 −¥0.3 到 ¥1.5 之间。" (also fixes F11):
  > 两家每单都只赚订单金额的百分之几：DoorDash Q3 2026 每单 Adj. EBITDA 约 $1.07，占 GOV 3.1%（计算）；美团外卖每单经营利润，券商对 2026 年的最新估算在 −¥0.3（UBS，全年）到 ¥0.5（J.P. Morgan，下半年）之间，按 Q2 实际利润倒推约 ¥0.3（估算），约为客单价的 0.6%–1%；战前常态约 ¥1.5。
- §5.3, replacing "假设：每单利润取 ¥1（券商估算区间为 −¥0.3 到 ¥1.5）；":
  > 假设：每单利润取 ¥0.4（J.P. Morgan 对 2026 下半年的估算为 ¥0.4–0.5；按 Q2 实际 CLC 利润和摩根士丹利对到店、闪购的估算倒推约 ¥0.3；UBS 的全年估算为 −¥0.3；¥1.5 是 2025-06 的战前预测）；
- §5.3, table title: "**美团：每单推理费占每单利润 ¥0.4 的比例（估算；按 ¥1 计约为下表的 0.4 倍）**"
- §5.3, table rows:
  > | 复购（"老样子"） | 约 2% | 约 5% | 约 5% |
  > | 发现+下单 | 约 32% | 约 82%（非高峰时段约 41%） | 约 77% |
- §5.3, replacing "长的发现会话即使用国产便宜模型，也要吃掉每单约三分之一的利润。":
  > 长的发现会话即使用国产便宜模型，也要吃掉每单利润的四分之三以上（按每单 ¥0.4 计；按 ¥1 计约三分之一）；改用 DeepSeek 的非高峰价可降到约 40%。
- §5.3, business-lines table, replacing "每单约 −¥0.3 到 ¥1.5（券商估算）":
  > 每单约 −¥0.3 到 ¥0.5（2026 年券商估算；战前约 ¥1.5）
- §5.4, replacing "美团外卖的每单利润只有约 ¥1":
  > 美团外卖的每单利润只有约 ¥0.3–0.5（券商估算）
- §6.2, replacing "到店酒旅的分部利润率约 25%，外卖每单只有约 ¥1（券商估算）。":
  > 到店酒旅的分部利润率约 25%（券商估算，占收入）；外卖每单只赚约 ¥0.3–0.5（券商估算），不到客单价的 2%。
- §6.3, Meituan paragraph:
  > **美团外卖：** 按每单利润 ¥0.4（区间 −¥0.3 到 ¥1.0）、客单价 ¥30–50 计（假设，参照《财经》算例里的 ¥29 和 ¥60 两单 ([新浪/财经](https://finance.sina.com.cn/roll/2025-05-28/doc-ineycmqy7871683.shtml))），可承受的通道费上限约 0.8%–1.3%（按 ¥1 计约 2.0%–3.3%；按 −¥0.3 计，任何费率都亏）（估算）。按豆包酒店的约 12% 收，每单要付 ¥3.6–6.0，是该单利润的 9–15 倍（按 ¥1 计为 3.6–6 倍）（估算）。
- §14.2, replacing "美团每单 ¥1": "美团每单 ¥0.4（区间 −¥0.3 到 ¥1.0）"

---

### F4 (major). The 3% / 22% thresholds are single points that omit basket lift and hide the main drivers

**Location:**
- 结论, bullet 1: "小模型下，Agent 订单里约 3% 是真增量就能回本；若丢一半广告，门槛升到约 22%（估算，第 5.3 节）。"
- §5.3 assumption: "广告占 GOV 2%（参照 Uber Delivery 超过 2% 的口径）"
- §5.3 note: "门槛 =（推理 + 错误 + 广告挤占）÷ 每单 CP $1.79；错误率取 0.1%。用 CP 作增量订单的贡献，偏保守。"
- §5.4 and §12.3, S1: "（DoorDash 小模型下 3%–22%）"
- §14.2: "换假设，门槛会变；但只要挤占比例超过约 3%、推理路由到小模型，"广告损失大于推理费"的排序不变。"

**Problem:**
1. **The basket term is missing.** The threshold drops the second term of the §1 formula, "Agent 订单的客单价提升 × 有效抽佣率", without saying so.
   - That term is the same size as the ad loss. A 5% basket lift at 13.6% take is $0.23 per order; a 10% lift is $0.47. The ad loss at k=50% is $0.34.
   - The report's own evidence points both ways. In favour: the memory eval showed +17% basket, and Ask grocery baskets run about 50% higher (biased). Against: Suggested Swaps may push basket value down, and Walmart lost attach sales.
2. **The ad rate is high for DoorDash.** 2% is borrowed from Uber. DoorDash's own last data point is lower:
   - a $1B+ run-rate in 2024 is about 1.24% of 2024 GOV (about $80.4B, derived from the notes' quarterly table and the 27% growth in 2025);
   - the "$1 billion a year" ad unit in June 2025 is about 1.03% of Q2 2025 annualized GOV.
   - Ads have grown since, so 1.25%–2% is a fair DoorDash range. Instacart's 3.1% ("advertising and other") is an upper bound for grocery-heavy mixes.
3. **The error rate moves the threshold as much as k does.** At 1% errors, the k=0 threshold is 22%, not 3%.
4. **Q3 2026 is DoorDash's best quarter per order.** On 9M 2026 averages (CP $1.66, EBITDA $0.94), thresholds rise by 0.2–1.7 points and the shifted-order EBITDA line falls from 3.1% to 2.8%.

With small models, plausible inputs (ads 1.25%–3.1%, k 0–75%, errors 0.1%–1%, basket 0–10%) put the threshold anywhere from below 0% to about 66% (Section 4). "约 3% … 约 22%" is a corner and a midpoint of that range.

**Evidence** (script §4, Haiku 5.5, conversion 40%):

| Assumption | k=0 | k=25% | k=50% | k=75% |
|---|---|---|---|---|
| Ads 1.25% / 2% / 3.1% | 3.1% (all) | 9.1% / 12.6% / 17.9% | 15.0% / 22.2% / 32.7% | 21.0% / 31.7% / 47.5% |
| Errors 1% (ads 2%) | 21.8% | 31.3% | 40.9% | 50.4% |
| Basket +5% (ads 2%) | −9.9% | −0.4% | 9.2% | 18.8% |
| Basket +10% (ads 2%) | −22.9% | −13.3% | −3.8% | 5.8% |

**Suggested replacements:**
- 结论, bullet 1, replacing "小模型下，Agent 订单里约 3% 是真增量就能回本；若丢一半广告，门槛升到约 22%（估算，第 5.3 节）。":
  > 小模型下，若不丢广告、错误率 0.1%，Agent 订单里约 3% 是真增量就能回本；丢一半广告时升到约 15%–33%（广告占 GOV 1.25%–3.1%），错误率升到 1% 时再加约 19 个百分点；反过来，Agent 订单的客单价若提高 5%，门槛约降 13 个百分点（估算，第 5.3 节）。
- §5.3, replacing "广告占 GOV 2%（参照 Uber Delivery 超过 2% 的口径）":
  > 广告占 GOV 2%（参照 Uber Delivery 超过 2% 的口径；DoorDash 自己最近的数字是 2024 年 run-rate 超过 10 亿美元，约为当年 GOV 的 1.2%，此后增长较快，所以 2% 可能偏高，敏感度见门槛表下）
- §5.3, replacing "门槛 =（推理 + 错误 + 广告挤占）÷ 每单 CP $1.79；错误率取 0.1%。用 CP 作增量订单的贡献，偏保守。":
  > 门槛 =（推理 + 错误 + 广告挤占 − 客单价提升 × 抽佣率）÷ 每单 CP $1.79；表中错误率取 0.1%，客单价提升取 0。用 CP 作增量订单的贡献，偏保守。敏感度（小模型）：广告占 GOV 1.25% / 3.1% 时，k=50% 的门槛为 15% / 33%；错误率 1% 时，k=0 和 k=50% 的门槛为 22% 和 41%；Agent 订单客单价提升 5%（按 13.6% 抽佣，每单多 $0.23）时，k=50% 的门槛降到 9%，提升 10% 时降到 0 以下；按 2026 年前三季度平均的每单 CP $1.66 计，各格高 0.2–1.7 个百分点。
- §5.4 and §12.3, replacing "（DoorDash 小模型下 3%–22%）":
  > （DoorDash 小模型下基准为 3%–22%；广告率、错误率和客单价变化可使它低于 0，也可高于 40%）
- §14.2, replacing "但只要挤占比例超过约 3%、推理路由到小模型，"广告损失大于推理费"的排序不变。":
  > 但只要推理路由到小模型、挤占比例超过约 2%–5%（广告占 GOV 3.1%–1.25%），"广告损失大于推理费"的排序不变；门槛本身对广告率、错误率和客单价提升都很敏感，见第 5.3 节。

---

### F5 (major). "几百单" is attributed to the wrong agents

**Location:**
- 结论 table, S2: "DoorDash 面对中立 Agent，累计"几百单""
- 结论, bullet 2: "DoorDash 至今只从第三方 Agent 收到"几百单" ([TechCrunch](https://techcrunch.com/2026/10/30/doordash-said-its-only-received-several-hundred-orders-from-ai-agents/))。"
- §6.1 heading: "### 6.1 美国：通用 Agent 是中立的，订单只有几百单"
- §6.1: "量极小。Xu 在 10-29 说，第三方 Agent "总共大概几百单" (...)。2026 年前三季度 DoorDash 共约 29 亿单，几百单约是千万分之一（计算）。"
- §11.3: "| 外部 Agent 格局 | 中立：OpenAI、Google、Amazon；累计"几百单" |"

**Problem:**
- Per `agentic_commerce_channels.md` §2, citing TechCrunch (2026-10-30), Xu said "maybe several hundred orders in total from some of these third-party agents". He named **Muse, Instinct and Fo** "and others": small startup agents.
- The figure does not describe ChatGPT. Its DoorDash app sends checkout back to the DoorDash app, so those orders are most likely counted as ordinary app orders.
- Nor does it describe Gemini's automation pilot, which operates the DoorDash app itself.
- §14 item 3 concedes "可能不含它自己的 ChatGPT App". But the §6.1 heading, the 结论 table and §11.3 present "几百单" as the volume from neutral general agents.
- No volume has been disclosed for the ChatGPT, Gemini or Alexa+ channels.
- The "千万分之一" ratio divides a cumulative count over an unknown window by nine months of orders.

**Does a verdict change?** S2's "① 弱" and "量可以忽略" lose their quantitative support for ChatGPT and Gemini. Both remain plausible, but unquantified.

**Suggested replacements:**
- §6.1 heading: "### 6.1 美国：通用 Agent 大体中立，经第三方 Agent 直接下单的量极小"
- §6.1, replacing the two sentences quoted above:
  > 量极小，但只有一类有数。Xu 在 10-29 说，来自 Muse、Instinct、Fo 等第三方 Agent 的订单"总共大概几百单" ([DoorDash Q3 2026 电话会](https://www.fool.com/earnings/call-transcripts/2026/10/29/doordash-dash-q3-2026-earnings-call-transcript/); [TechCrunch](https://techcrunch.com/2026/10/30/doordash-said-its-only-received-several-hundred-orders-from-ai-agents/))，相对 2026 年前三季度约 29 亿单，是千万分之一量级（计算；分子是累计数，统计窗口未披露）。ChatGPT 杂货 App 结账跳回 DoorDash App，Gemini 自动化操作的也是 DoorDash App，这两类订单很可能记作普通 App 订单，DoorDash 没有单独披露。
- 结论 table, S2 cell, replacing "DoorDash 面对中立 Agent，累计"几百单"；":
  > DoorDash 面对大体中立的 Agent，小型第三方 Agent 累计"几百单"，ChatGPT、Gemini 渠道的量未披露；
- 结论, bullet 2, replacing the last sentence:
  > DoorDash 至今只从 Muse、Instinct 等第三方 Agent 收到"几百单"，ChatGPT 和 Gemini 渠道的量没有单独披露 ([TechCrunch](https://techcrunch.com/2026/10/30/doordash-said-its-only-received-several-hundred-orders-from-ai-agents/))。
- §11.3, replacing "中立：OpenAI、Google、Amazon；累计"几百单"":
  > 大体中立：OpenAI、Google；Amazon 的 Alexa+ 只接了 Grubhub 和 Uber Eats；小型第三方 Agent 累计"几百单"，ChatGPT、Gemini 渠道的量未披露

---

### F6 (major). S3 "DoorDash 已经成立" rests only on DoorDash's own claims

**Location:**
- 结论: "按这五条，S3 商户 Agent 靠广告预算已经成立"
- 结论 table, S3: "| S3 商户经营 | 成立 | 成立 | DoorDash 成立（广告）；美团间接 |"
- 结论, bullet 4: "**商户 Agent 已经在赚钱，赚的是广告。**"
- §7 table, ③: "| ③ 价值获取 | 通过广告花费直接变现；网站和营销工具免佣 | 免费；靠留存、广告效率和供给质量 | DoorDash 成立；美团间接 |"
- §7.4: "③ DoorDash 已经成立：Smart Campaigns 把 AI 直接变成广告花费，而且 ROI 对商户可见。"
- §11.1, C9 for S3: "DoorDash 是；美团未证"

**Problem:**
- Every piece of evidence is DoorDash's own:
  - Xu's call remark ("one of the biggest agents");
  - the DoorDash Ads release, with the Pubbelly Sushi case and CPG auto-bidding beating the minimum ROAS in 95%+ of test campaigns, against targets DoorDash set;
  - the merchant-tools post.
- DoorDash does not disclose Smart Campaigns' revenue, the share of ad spend it manages, or whether that spend is incremental to what merchants would have spent anyway.
- "始终 ROI 为正" is measured by DoorDash's own attribution, on DoorDash's own inventory. The platform sets the bids, sells the slots and grades the result.
- The report applies a stricter standard to third-party vendors in the same section ("但这些 ROI 都是厂商自报，没有审计"). It names the conflict for Meituan (a tool that saves merchants money cuts ad revenue), but not the mirror-image conflict for DoorDash: an agent that spends merchants' budgets on the platform's own ad slots.
- The mechanism is plausible; the monetization is management-asserted. The verdict should be "据管理层口径成立", not "已经成立".

**Suggested replacements:**
- 结论 table, S3, ③ cell: "DoorDash 据管理层口径成立（广告；收入和增量未披露）；美团间接"
- 结论, bullet 4 heading: "**商户 Agent 离赚钱最近，赚的是广告，但证据都来自平台自己。**"
- §7 table, ③ verdict: "DoorDash 据管理层口径成立；美团间接"
- §7.4, replacing "③ DoorDash 已经成立：Smart Campaigns 把 AI 直接变成广告花费，而且 ROI 对商户可见。":
  > ③ DoorDash 据管理层口径已经成立：Smart Campaigns 把 AI 直接变成广告花费，但收入、托管的广告花费占比和增量都没披露，"始终 ROI 为正"由 DoorDash 自己的归因算出，出价、卖位和评估都在平台一侧。这和美团的张力方向相反：替商户花钱的 Agent 有把预算花在平台自家广告位上的激励，需要对照组或第三方测量来证明。
- §11.1, C9 for S3: "DoorDash 据管理层口径是（收入和增量未披露）；美团未证"
- 结论, sentence (f): see F7.

---

### F7 (major). The 结论's thesis and "five conditions" do not fit S3–S6, and §11.1 calls S4 and S5 "成立"

**Location:**
- 结论: "难在价值获取，而卡住价值获取的不是 token 成本，是广告和通道费。"
- 结论: "所以在 Agent 不单独收费、账在原有抽佣里结的模式下，价值获取要同时满足五条：订单是真增量，不挤掉广告，推理路由到小模型，错误率远低于 1%，账户、结账和履约留在平台。按这五条，S3 商户 Agent 靠广告预算已经成立，S1 机制成立但账未证，S4 和 S5 是成本账、杠杆大但未兑现，S2 只能当获客渠道，S6 卖 token 不成立。"
- §11.1: "**读法：** 成立的场景里，Agent 都在给付钱的那一层加东西：S3 加广告花费，S4、S5 减成本。"

**Problem:**
- In the body, ads and channel fees block value capture only in S1 and S2. Other things block it elsewhere:
  - S4: utilization, licences and capex (§8.3).
  - S5: whether total cost falls, and non-disclosure (§9).
  - S6: no cornered resource outside the network (§10).
- The five conditions are conditions on consumer orders: incremental orders, ad loss, small-model routing, error rate, account and checkout. They say nothing about S3–S6, yet the next sentence applies them to all six scenarios ("按这五条").
- "要同时满足五条" also contradicts §5.3, where the first four trade off against each other: more incrementality offsets ad loss.
- The 读法 calls S4 and S5 "成立的场景". Both are "③ 未证" in their own sections, in the 结论 table and in §11.1's 总体 row.

**Suggested replacements:**
- 结论, replacing "难在价值获取，而卡住价值获取的不是 token 成本，是广告和通道费。":
  > 难在价值获取。在两个面向消费者的场景（S1 自有 App、S2 第三方调用）里，卡住价值获取的不是 token 成本，是被挤掉的广告和通道费；在 S4、S5、S6 里，卡住它的分别是利用率、总成本能否下降，以及出了自家网络没有独有资源。
- 结论, replacing the "所以在 Agent 不单独收费…S6 卖 token 不成立。" passage (includes F6 and F15):
  > 所以在 Agent 不单独收费、账在原有抽佣里结的 S1、S2 里，价值获取要看五件事：订单有多少是真增量，挤掉多少广告，推理是否路由到小模型，错误率是否远低于 1%，账户、结账和履约是否留在平台；前四件可以互相抵补，不必个个达标。据此，S1 机制成立但账未证，S2 只能当获客渠道。另外四个场景另有判据：S3 商户 Agent 靠广告预算变现，据管理层口径已经成立；S4 和 S5 是成本账、杠杆大但未兑现；S6 卖 token 不是盈利路径，把 AI 捆绑进与自家网络绑定的服务卖，机制成立但增量未披露。
- §11.1, replacing the first two sentences of 读法:
  > **读法：** 价值获取据管理层口径成立的只有 S3：Agent 在给付钱的那一层加东西（广告花费）。S4、S5 的方向也是减成本，但节省还没兑现或没披露。

---

### F8 (major). "最难的从 C7、C8 换成了 C9 和 C5" is not what §11.1 shows

**Location:**
- 结论, bullet 6: "**对垂直平台，九个条件里最难的从 C7、C8 换成了 C9 和 C5。** 消费者不为 Agent 付钱，单次推理很便宜，计价单位不再是主要矛盾。主要矛盾是利润会不会被 Agent 自己挤掉（广告），或被入口主人拿走（通道费）。"
- §12.4: "| 最难的条件 | C7 计价跟着成本走、C8 封得住 | C9 毛利守得住、C5 入口归谁 |"
- §1: "C2 和 C9 是本文多问的两句。…C9：利润可能被两方拿走，一是入口主人的通道费，二是 Agent 自己挤掉的广告。"

**Problem:**
- In §11.1, C5 is "是" for S1, S3, S4 and S5 and "部分" for S2 and S6. It has no "否" anywhere, which makes it one of the easiest conditions.
- C9 has the most "否 / 未证": S1 未证, S2 否, S3 未证 for Meituan, S4 未证, S5 未证, S6 否 for tokens.
- Of the rest, C2 is the one the 结论 itself makes decisive: its first condition, "订单是真增量", is C2 by §1's definition ("C2：…所以要问增量").
- §1 files the channel fee under C9, so naming C5 for "被入口主人拿走（通道费）" counts it twice.
- The earlier report's C9 also asked whether gross margin can reach about 60% (Bessemer). This report drops that clause without saying so. That is right for a marketplace earning about 3% of GOV, but it should be stated.
- The 结论 uses bare C-codes, so it is not self-contained.

The comparison with the earlier report otherwise holds:
- earlier: always-on agents fail on C7 and C8 ("计价跟着成本走，付钱的人看得见、封得住");
- earlier §8.5: "账可以在 Agent 之外结，前提是那一层归你", with its 结论 adding "而且不被 Agent 挤占";
- this report's §12.4 rule ("付钱的那一层既归你，又不被 Agent 吃掉") is consistent with that.

**Suggested replacements:**
- 结论, bullet 6:
  > **对垂直平台，九个条件里最难的从 C7（计价跟着成本走）、C8（看得见、封得住）换成了 C2（有人买单）和 C9（毛利守得住）。** 消费者不为 Agent 付钱，单次推理很便宜，计价单位不再是主要矛盾。主要矛盾是两件事：Agent 订单有多少是真增量（C2），以及利润会不会被 Agent 自己挤掉（广告）或被入口主人拿走（通道费）（C9）。入口归谁（C5）决定通道费这一项有多大：美国的入口大体中立、目前不收费，中国最大的两个入口属于对手。
- §12.4 row: "| 最难的条件 | C7 计价跟着成本走、C8 封得住 | C2 有人买单（真增量）、C9 毛利守得住（广告挤占和通道费）；C5 入口归谁决定通道费的大小 |"
- §1, append after "…二是 Agent 自己挤掉的广告。":
  > 前一份报告的 C9 还问"毛利有没有路走向约 60%"；外卖平台的利润只占 GOV 的约 3%，这个门槛不适用，本文的 C9 只问利润会不会被拿走。

---

### F9 (minor). Model prices: Sonnet's cache-read price, one unverified price, and three gaps in the cost model

**Location:**
- §5.3: "| 推理：中档（Gemini 3.8 Flash，2026 年标价） | $0.14 | 13% |" and "| 推理：大模型（Claude Sonnet 5.5 标价） | $0.35 | 32% |"
- §5.3: "复杂会话（团餐、多店、长推理）更贵：用小模型每单约 $0.09，用 Sonnet 5.5 约 $1.80，超过每单 CP（估算）。"
- §5.3, threshold table: the "中档模型" column.
- §7.3 and §11.1 (C6, S3): "$0.2–1.2"
- §14.2: "token 价格来自官方价目表"

**Problem** (see Section 2):
1. **Sonnet 5.5 cache reads are $0.20** per the coordinator's verified Anthropic reference; the notes and script used $0.10. At $0.20:
   - discovery $0.36 per order, 34% of EBITDA (was $0.35, 32%);
   - complex $1.85 (was $1.80);
   - merchant $1.26 per month (was $1.23).
   For the record, the repo's earlier report says the price fell to $0.10 on 2026-10-07. If that cut also applies to first-party pricing, the original figures stand. Either way no conclusion changes.
2. **Haiku 5.5's cache read is not listed** in the Anthropic reference, so $0.01 is an assumption. Its $0.10 / $0.50 prices apply only to prompts ≤100K tokens.
3. **Cache-write premiums are ignored.** Billing uncached input at 1.25× adds about 13% to Claude per-order costs (Sonnet $0.36 → $0.41).
4. **The Gemini 3.8 Flash doubling on 2027-01-01** (confirmed) is mentioned but not carried into the thresholds. From 2027 the mid tier costs $0.27 per order (25% of EBITDA), with thresholds of 17% / 27% / 36%.
5. **The top tier is mislabelled.** Sonnet 5.5 is labelled "大模型", but frontier models cost much more:
   - Opus 5.5: $0.69 per order, 65% of EBITDA;
   - GPT-5.5: $0.97, 91%;
   - at 20% conversion, GPT-5.5 is $1.94, above CP.
   "推理是小头" holds only with routing. The report says so, but its top row understates the downside.

**Suggested replacements:**
- §5.3 table, mid-tier row:
  > | 推理：中档（Gemini 3.8 Flash；2026 年标价 / 2027-01-01 起） | $0.14 / $0.27 | 13% / 25% |
- §5.3 table, replace the "大模型" row and add a row:
  > | 推理：中高档（Claude Sonnet 5.5 标价） | $0.36 | 34% |
  > | 推理：旗舰（Claude Opus 5.5 / GPT-5.5 标价） | $0.69 / $0.97 | 65% / 91% |
- §5.3 assumptions, append:
  > Claude 的输入、输出价适用于 10 万 token 以内的提示；Sonnet 5.5 的缓存读按官方参考的 $0.20 计（仓库前一份报告记为 10-07 起降至 $0.10，按 $0.10 计为每单 $0.35、32%）；Haiku 5.5 的缓存读价官方参考未列，按 $0.01 假设；未计缓存写入溢价（约使 Claude 各档每单成本高 13%）。
- §5.3, replacing "用 Sonnet 5.5 约 $1.80，超过每单 CP（估算）":
  > 用 Sonnet 5.5 约 $1.85，略高于每单 CP（$1.79），用 Opus 5.5 约 $3.60（估算）
- §5.3 threshold table: header "| 广告挤占比例 k | 小模型 | 中档模型（2026 价 / 2027 价） |", rows "| 0（Xu 的说法：不改变广告经济） | 3% | 10% / 17% |", "| 25% | 13% | 19% / 27% |", "| 50% | 22% | 29% / 36% |"
- §7.3 and §11.1 (C6, S3): "$0.2–1.2" → "$0.2–1.3"
- §14.2, replacing "token 价格来自官方价目表":
  > token 价格来自官方价目表（Sonnet 5.5 的缓存读价按官方参考的 $0.20 计，与仓库前一份报告记录的 10-07 降价后 $0.10 不一致；Haiku 5.5 的缓存读价未在官方参考中列出；两者均不改变结论）

---

### F10 (minor). "广告恰恰是利润本身" overstates the 93% ratio

**Location:**
- 结论: "广告恰恰是利润本身：美团 2024 年的广告收入相当于本地商业分部经营利润的 93%（计算）。"
- §2.2 heading: "### 2.2 美团：外卖每单利润在 ¥0 上下，广告约等于本地商业的全部利润"

**Problem:**
- 93% compares a revenue line with an operating profit. Ads carry their own costs (sales, ad tech, traffic), so ad profit is below ad revenue. The ratio shows the two are the same size, not that ads *are* the profit.
- The ratio is defined only for 2024. In 2025 the segment lost ¥69亿 on ¥515亿 of ads.
- The pool spans 外卖, 闪购 and 到店酒旅, where 大众点评 merchants are heavy advertisers, so it overstates what a food-ordering agent can cannibalize. Meituan does not disclose the split.
- For DoorDash, "广告约等于利润" is an inference from peers. If its ads are 1.25%–2% of GOV, they equal 40%–64% of Q3 Adj. EBITDA (3.1% of GOV), not about 100%.

**Suggested replacements:**
- 结论:
  > 广告收入与利润同量级：美团 2024 年的广告收入相当于本地商业分部经营利润的 93%（计算；广告收入不等于广告利润，且含到店酒旅的广告）。
- §2.2 heading:
  > ### 2.2 美团：外卖每单利润在 ¥0 上下，广告收入与本地商业利润同量级

---

### F11 (minor). Several DoorDash–Meituan comparisons are not like for like

| Location (quoted) | Problem | Replacement |
|---|---|---|
| 结论: "两家每单都只赚约 1 美元或 1 元" | $1.07 is about ¥7.6, so the phrase invites a false parity. It also mixes company-wide Adj. EBITDA with a broker's food-delivery UE. | See F3 (puts both on % of order value). |
| §11.3: "\| 每单利润 \| Adj. EBITDA $1.07，CP $1.79（Q3 2026，计算） \| 外卖约 −¥0.3 到 ¥1.5（券商估算），2026 Q2 刚转正 \|" | Company-wide profit after R&D and G&A vs food-delivery UE; USD vs RMB; no common base. | "\| 每单利润 \| Adj. EBITDA $1.07，CP $1.79（Q3 2026，全公司，计算），约占 GOV 3.1% 和 5.2% \| 外卖 UE 约 −¥0.3 到 ¥0.5（2026 年券商估算，仅外卖），按客单价 ¥30–50 约为 −1% 到 1.7%；2026 Q2 刚转正 \|" |
| §9: "研发占 GOV 1.5%（Q2 为 1.6%）" vs "研发占收入 7.3%" | Same row, different bases. On revenue, DoorDash's R&D is about 11.0%, higher than Meituan's 7.3%. | DoorDash cell: "Q3 研发同比 +41%，快于订单 +28%；研发占 GOV 1.5%（Q2 为 1.6%），约占收入 11%；G&A 占 GOV 1.3%（上年 1.6%）" |
| §5.3: "美团每丢 10% 广告收入，一年约 51 亿元，约等于 Q2 年化 CLC 经营利润的 23%（估算；Q2 季节性偏强）" | 2025 ads vs Q2-2026 annualized profit; treats lost ad revenue as lost profit. | "美团每丢 10% 广告收入，按 2025 年的 515 亿元计一年约 51 亿元，约等于 Q2 年化 CLC 经营利润的 23%（估算；按广告收入全部转为利润的上限计；Q2 季节性偏强，两者年份不同）" |
| §8.2, 敏感度 row | DoorDash's $0.10 (≈¥0.71) is reported as % of GOV; Meituan's ¥0.05 as % of segment profit. | See F13 (adds "≈ Q3 Adj. EBITDA 的 9%"). |
| §6.2: "到店酒旅的分部利润率约 25%，外卖每单只有约 ¥1" | A margin on revenue vs RMB per order. | See F3. |

---

### F12 (minor). The 结论 paraphrase of the 10-Q drops half of the cause

**Location:** 结论, bullet 1: "DoorDash 的 10-Q 写明，消费者费用率的下降由广告补回"

**Problem:** The 10-Q says the decline was "offset by increased contribution from advertising revenue and reductions in discounts and promotions" (`doordash.md` Q0). §2.1 quotes it correctly; the 结论 overstates the role of ads.

**Replacement:**
> DoorDash 的 10-Q 写明，消费者费用率的下降由广告收入增加和促销减少补回

---

### F13 (minor). S4: an undocumented assumption, inconsistent rounding, and a sensitivity shown as a difference

**Location:**
- 结论 table, S4: "DoorDash 每单省 $0.10 ≈ 每季 9,900 万美元；美团骑手保障成本在涨"
- 结论, bullet 5: "DoorDash 每单省 $0.10，约等于每季多 9,900 万美元收入（计算）。"
- §8.2: "| 示意 | 机器人承担 1% 订单（每季约 990 万单）、每单省 $3–7：每季 3,000–7,000 万美元，约为 Q3 Adj. EBITDA 的 3%–7%；需约 6,600 台机器人（估算） | — |"
- §8.2: "| 敏感度 | 每单省 $0.10 ≈ 每季多 9,930 万美元收入 ≈ 净收入率 +0.29 个百分点（计算） |"

**Problem:**
1. **"$3–7" per robot delivery comes from no cited input.** Serve's figures (human $8–10; robot target about $1 at ≥1,500 deliveries per robot per quarter) imply $7–9 at target, which is 6.6%–8.4% of Q3 EBITDA. $3–7 implies a pre-target robot cost of $3–5. That is reasonable, but unstated.
2. **Rounding differs:** 9,900 万 in the 结论 vs 9,930 万 in §8.2 and §11.2.
3. **A hypothetical sits in a "差别" column.** The 结论 table presents a sensitivity as if it were a fact about DoorDash.

**Replacements:**
- §8.2 示意 cell:
  > 机器人承担 1% 订单（每季约 990 万单）、每单省 $3–7（假设：人工每单 $8–10，机器人在达到 Serve 的利用率门槛前每单 $3–5；达标后按 Serve 的约 $1 计可省 $7–9）：每季 3,000–7,000 万美元，约为 Q3 Adj. EBITDA 的 3%–7%（达标后约 7%–8%）；需约 6,600 台机器人（估算）
- §8.2 敏感度, DoorDash cell:
  > 每单省 $0.10 ≈ 每季多 9,930 万美元收入 ≈ 净收入率 +0.29 个百分点 ≈ Q3 Adj. EBITDA 的 9%（计算）
- 结论 table, S4 difference cell:
  > DoorDash 的 Dasher 支出在收入里扣，每单省 $0.10 约合每季 9,930 万美元（敏感度，非实绩）；美团骑手保障成本在涨
- 结论, bullet 5:
  > DoorDash 每单省 $0.10，约等于每季多 9,930 万美元收入、Q3 Adj. EBITDA 的 9%（计算，敏感度）。

---

### F14 (minor). Several §11.1 cells score a different condition from the one §1 defines

| Cell (quoted) | Problem | Replacement |
|---|---|---|
| C7 / S2: "否：通道费按 GOV 收" | A GOV-based fee moves with GOV-based commission, so the pricing units match. The problem is the level (fee ≥ margin), which is C9. | "部分：通道费与抽佣同按 GOV 走，计价单位对得上；问题在费率高于利润率（见 C9）" |
| C9 / S2: "否：4% 已超过 DoorDash 每单 EBITDA" | No third-party agent charges either company today; 4% is a withdrawn precedent and the comparison depends on the base (F2). | "未证，风险高：目前没有通道费；按 4% 的先例收，费额约等于（按小计）或超过（按 GOV）每单 EBITDA，叠加广告丢失后转移单必亏" |
| C6 / S6: "DoorDash 不适用；美团否：促销价约等于 Flash 档" | C6 asks whether cost per task is under control (own model, own compute). Meituan has both. The price point is a C9 question. | "DoorDash 不适用；美团是：自研模型、国产算力（定价低是 C9 的问题）" |
| C4 / S6: "否：出了自家网络没有独有数据" | Contradicts the S6 verdict that network-bound services hold. | "卖 token：否；卖网络服务：是（履约、媒体、仓配数据都在自家网络）" |
| C9 / S6: "否（token）；是（履约加软件）" | No margin is disclosed for Drive, SevenRooms, DashOS or Symbiosys. | "否（token）；未证（履约加软件，未披露毛利）" |
| C3 / S5: "是" | §9 gives no evidence on audit, permissions or rollback for internal agents. | "未评估：第 9 节没有相关证据" |
| C6 / S5: "部分：研发费用快于业务增长" | Aggregate R&D growth is not a per-task cost. | "部分：DoorDash 用 token 路由控成本；两家都没披露单任务成本，研发费用快于业务增长" |
| C5 / S1: "是；Text DoorDash 借用 Apple 信息" | The app is an owned channel, but Text DoorDash runs on Apple's. | "是（自有 App）；部分（Text DoorDash 借用 Apple 信息）" |

Apart from these eight cells and C9 / S3 (F6), the cells match the evidence in §5–§10.

---

### F15 (minor). The S6 verdict answers a different question

**Location:**
- 结论 table, S6: "卖 token 不成立；卖履约和软件捆绑成立"
- §10 table, ③: "卖 token 不成立；卖网络服务成立"
- §10: "③ 卖 token 不成立，卖与自家网络绑定的服务成立。"

**Problem:**
- S6 is "对外输出 AI 能力". The evidence for "卖网络服务成立" is that Drive, digital ordering, Symbiosys and DashMart Fulfillment Services exist and grow. These are long-standing logistics, software and media lines in which AI is given away: AI websites carry a 0% commission.
- Neither company discloses AI-specific revenue or margin.
- "卖 token 不成立" is inferred from Meituan pricing LongCat at about DeepSeek Flash level and from Wang Xing's "不做 Token 工厂". That is intent and price, not a failed test.

**Replacements:**
- §10:
  > ③ 卖 token 不是两家的盈利路径（推断：美团的价格对标 DeepSeek Flash，管理层明言不做 Token 工厂；DoorDash 不卖）；把 AI 免费捆绑进与自家网络绑定的服务（Drive、媒体、仓配）卖，机制成立，但 AI 带来的增量收入和毛利都没披露。
- 结论 table, S6 ③: "卖 token 不是盈利路径；AI 捆绑进履约和软件卖，机制成立、增量未披露"
- §10 table, ③ verdict: "卖 token 不是盈利路径；卖网络服务机制成立，AI 增量未披露"

---

### F16 (minor). The S2 value-proposition verdict and the "neutral agents" claim rest on thin evidence

**Location:**
- 结论 table, S2 ①: "弱：外卖用 Agent 点不比 App 快"
- §6.4: "对消费者，外卖经第三方 Agent 下单并不比 App 快；"
- 结论, bullet 3: "美国的通用 Agent 是中立的：Gemini 能同样操作 DoorDash、Uber Eats 和 Grubhub"

**Problem:**
- "不比 App 快" comes from one Tom's Guide tester using Gemini's GUI-automation pilot. It says nothing about ChatGPT's app, DoorDash's MCP or A2A integrations.
- "中立" holds for Gemini and ChatGPT. But Alexa+ integrated only Grubhub and Uber Eats (§6.1 table); §11.1's C5 cell already says "Alexa+ 选了对手".
- The acquisition-channel case relies on one Walmart data point from retail, not food. §6.3 labels it "单一来源"; the verdicts do not.

**Replacements:**
- 结论 table, S2 ①: "弱：已有的测试里，经 Agent 点外卖不比 App 快（单一评测）"
- §6.4: "对消费者，外卖经第三方 Agent 下单并不比 App 快（目前只有对 Gemini 自动化试点的单一评测）；"
- 结论, bullet 3:
  > 美国的通用 Agent 大体中立：Gemini 能同样操作 DoorDash、Uber Eats 和 Grubhub；例外是 Amazon 的 Alexa+ 只接了 Grubhub 和 Uber Eats

---

### F17 (minor). Two frameworks are partly decorative; one is slightly misdescribed

1. **7 Powers.**
   - §1 sets the test metrics as "每单配送成本、独有供给、ETA 准确率、售后". But the §3.2 table fills scale economies with order volume and Dasher counts, and process power with DashMart accuracy and drone counts.
   - Helmer's test needs both a benefit (lower unit cost) and a barrier (rivals can't match it). The table shows size, not the benefit, because neither company discloses per-order delivery cost.
   - Append to §3.2's source line, after "…此表是本研究的映射。":
     > 两家都没披露每单配送成本和 ETA 准确率，所以规模经济和流程能力的强弱是推断，未经检验。
2. **JTBD.**
   - §4's central insight, "Agent 最擅长的活（复购、有约束的决策）和广告最赚钱的活（冲动浏览）正好相反", never reaches the numbers. §5.3 applies one session type (discovery) and one k to every agent order.
   - Read through JTBD, the two ends of the threshold table belong to different jobs:
     - reorders: 3-call sessions, about $0.001 per order, k ≈ 0, threshold about 2%, but little true increment;
     - discovery: k high, threshold about 22% at k=50%, and more likely increment ("近一半 Ask 餐厅订单流向没去过的店").
   - Add after the §5.3 threshold note:
     > 按任务拆开看：复购会话每单推理约 $0.001、几乎不挤占广告，门槛约 2%，但复购本身很少是真增量；发现会话在 k=50% 时门槛约 22%，而 Ask 近一半餐厅订单流向没去过的店，增量更可能出在这里。S1 的账取决于两类任务的比例。
3. **Aggregation theory.**
   - Thompson's Level 2 is defined by supply *transaction* costs. DoorDash nets Dasher pay against revenue, so consumers and merchants largely pay the per-order courier cost.
   - Replace "外卖平台是二级聚合者，还多了一条昂贵的物理边：骑手。每单都有真实的边际成本。" with:
     > 外卖平台是二级聚合者，还多了一条昂贵的物理边：骑手。每单的骑手报酬主要由消费者和商户付的费用承担（DoorDash 在收入里扣除），但招募、调度和保障骑手的成本留在平台，这正是通用 Agent 复制不了的部分。

**Five Forces and the value chain do drive verdicts:** the buyer-power row underpins S2 ③, and the rivalry row underpins S4's "省下的钱留得住". Unit economics drives S1 and S2. Aggregation theory drives S2 ②.

---

### F18 (minor). The §1 formula drops two terms from the notes' model

**Location:** the §1 code block, ending "  − 补贴".

**Problem:**
- The notes' formula (`analysis_toolkit_unit_economics.md` §2.2) also subtracts lost impulse and attach value (O_a × L_impulse × τ_eff) and fixed costs F: evals, safety and engineering.
- The report says it is "按研究笔记整理" but drops both.
- Fixed costs matter while agent volumes are small. DoorDash runs 2,000+ automated evals a day, and its R&D grew 41%, partly on "third-party software expenses". Per-order thresholds ignore them.
- Lost impulse value is the negative side of F4's basket term.

**Replacement:**
- Replace the last line "  − 补贴" of the block with:
  ```
    − 补贴
    − 丢失的冲动和加购（Agent 订单 × 每单丢失的加购额 × 有效抽佣率）
    − 固定投入（评测、安全、工程，按期摊销）
  ```
- Append to the "两条口径" paragraph:
  > 第 5.3 节的门槛只算每单变动项，没有摊固定投入；Agent 订单还少时，固定投入可能比推理费大。

---

### F19 (minor). The S5 support-savings estimate assumes onshore US labour cost

**Location:**
- §9: "客服节省的假设：每单 2%–5% 的接触率，AI 解决 50%–70%，人工每次 $3.13（每小时全成本 $25、处理 8 次），AI 每次 $0.05。"
- The §9 table row: "每单 $0.03–0.11 × 年化约 39.7 亿单 ≈ 每年 1.2–4.3 亿美元，约为年化 Adj. EBITDA 的 3%–10%"

**Problem:** $25 an hour fully loaded is an onshore US rate. DoorDash does not disclose how much of its support is outsourced or offshore. At $10 an hour ($1.25 per contact), savings fall to $0.012–0.042 per order, $48–167M a year, or 1.1%–3.9% of annualized Adj. EBITDA. The ③ verdict (未证) is unaffected.

**Replacements:**
- Table cell:
  > 每单 $0.01–0.11 × 年化约 39.7 亿单 ≈ 每年 0.5–4.3 亿美元，约为年化 Adj. EBITDA 的 1%–10%（下限按外包或离岸人工每小时 $10 计）
- Assumption sentence: change "人工每次 $3.13（每小时全成本 $25、处理 8 次）" to "人工每次 $1.25–3.13（每小时全成本 $10–25、处理 8 次）"

---

### F20 (minor). §2.3 peer benchmarks: one label and one range

**Location:**
- §2.3 table: "| Instacart（Q3 2026） | Adj. EBITDA 3.2% | 3.1% |"
- §2.3: "**这张表决定了后面每个场景的账。** 利润约为 GOV 的 3%–4%，广告也有 2%–3%。Agent 每单的推理费以美分计；它对广告和通道费的影响以几十美分计。"

**Problem:**
- $331M is Instacart's "advertising and other" revenue (notes §3.2), which includes non-ad lines.
- The peer profit range in the table is 2.7%–3.8%, not "3%–4%".
- "推理费以美分计" holds only for small models (F9).

**Replacements:**
- Table row: "| Instacart（Q3 2026） | Adj. EBITDA 3.2% | 广告及其他 3.1%（上限） |"
- Sentence:
  > **这张表决定了后面每个场景的账。** 利润约为 GOV 的 2.7%–3.8%，广告约 2%–3%（Instacart 的 3.1% 含其他收入）。小模型下，Agent 每单的推理费以美分计，旗舰模型要几十美分到约 1 美元；它对广告和通道费的影响以几十美分计。

---

## 4. Sensitivity

### 4.1 The five assumptions that drive the conclusions

1. **Ad cannibalization: ad rate × k.** This drives the S1 thresholds and the "ads > inference" ranking.
   - The report uses 2% and k = 25%–50%.
   - Plausible ranges: 1.0%–2% for DoorDash (its own data), 3.1% as a grocery upper bound; k = 0–75%.
2. **True incrementality and basket change on agent orders.** Neither company measures either. Together they decide whether S1 pays at all. A ±5% basket change moves the threshold by ±13 points.
3. **Model tier and session-to-order conversion.** These drive "推理是小头".
   - Small models: $0.02–0.05 per order.
   - Mid tier in 2027: $0.27.
   - Frontier: $0.69–0.97.
   - Halving conversion from 40% to 20% doubles all of these.
4. **Error rate.** At about $37 per error, going from 0.1% to 1% adds 19 points to every threshold, as much as k moving from 0 to 50%.
5. **The per-order profit base.**
   - DoorDash: Q3 vs 9M averages, and GOV vs subtotal as the fee base.
   - Meituan: ¥1 vs ¥0.3–0.5.
   - This drives every S2 fee claim and every Meituan ratio.

Meituan's 65M vs 85M daily orders moves only scale figures, by ±15%:
- the S4 ¥0.05 saving: ¥11.9–15.5亿, or 5.2%–6.8% of annualized CLC profit;
- drones: 1.2%–1.5% of one day's orders;
- implied Q2 UE: ¥0.27–0.36 from actual profit, ¥0.04–0.05 from MS's forecast.

It flips no conclusion.

### 4.2 Grids

Break-even share of truly incremental agent orders. Defaults: ads 2% of GOV, errors 0.1%, conversion 40%, CP $1.79, no basket lift.

| Model (discovery session, per order) | k=0 | k=25% | k=50% | k=75% |
|---|---|---|---|---|
| Claude Haiku 5.5 ($0.018) | 3.1% | 12.6% | 22.2% | 31.7% |
| DeepSeek Flash, peak ($0.047) | 4.7% | 14.2% | 23.8% | 33.3% |
| Gemini 3.8 Flash, 2026 ($0.135) | 9.6% | 19.2% | 28.7% | 38.3% |
| Gemini 3.8 Flash, 2027 ($0.271) | 17.2% | 26.7% | 36.3% | 45.9% |
| GPT-5.4 mini ($0.146) | 10.2% | 19.8% | 29.3% | 38.9% |
| Claude Sonnet 5.5, cache $0.20 ($0.361) | 22.2% | 31.8% | 41.3% | 50.9% |
| Claude Opus 5.5 ($0.693) | 40.8% | 50.3% | 59.9% | 69.4% |
| GPT-5.5 ($0.972) | 56.4% | 65.9% | 75.5% | 85.0% |

Corners (small model unless stated):

| Case | Threshold |
|---|---|
| Best: ads 1.25%, k 0, basket +10% | −22.9% (pays with no increment) |
| Report base: ads 2%, k 50% | 22.2% |
| Ads 3.1%, k 75% | 47.5% |
| Ads 3.1%, k 75%, errors 1% | 66.2% |
| Sonnet, ads 3.1%, k 75%, errors 1% | 85.4% |
| GPT-5.5, ads 3.1%, k 75%, errors 1% | 119.5% (never pays) |

Meituan inference as a share of per-order profit (discovery session):

| Model | UE ¥1.0 | UE ¥0.45 | UE ¥0.4 | UE ¥0.3 |
|---|---|---|---|---|
| Claude Haiku 5.5 | 12.8% | 28.5% | 32.0% | 42.7% |
| DeepSeek Flash, peak | 33.0% | 73.3% | 82.5% | 110.0% |
| DeepSeek Flash, off-peak | 16.5% | 36.7% | 41.2% | 55.0% |
| LongCat, promo | 31.0% | 68.8% | 77.4% | 103.3% |

Channel fee at 4%, by base. Ads 2%; "ads lost" means the whole 2% is lost on that order.

| Base | Fee per order | Shifted EBITDA, ads kept / lost | Incremental CP, ads kept / lost |
|---|---|---|---|
| GOV ($34.2) | $1.37 | −$0.30 / −$0.98 | +$0.42 / −$0.26 |
| Subtotal at 80% of GOV | $1.09 | −$0.02 / −$0.71 | +$0.70 / +$0.01 |
| Subtotal at 70% of GOV | $0.96 | +$0.11 / −$0.57 | +$0.83 / +$0.15 |

### 4.3 Which conclusions flip

- **"广告损失大于推理费"**
  - With small models it holds for any k above about 2%–4% (up to about 7% if Haiku cache reads got no discount and ads were 1.25%).
  - It **flips for frontier models.** GPT-5.5 ($0.97) exceeds the entire ad revenue of the order at ads 2% ($0.68). Opus 5.5 ($0.69) roughly equals it. Mid-tier at the 2027 price ($0.27) exceeds the ad loss whenever k < 40%.
  - The 结论 states the routing condition, so it survives as written. The §5.3 heading "推理是小头" should say "小模型下".
- **S1 break-even**
  - It ranges from below 0% to above 100%. The verdict "未证" is right; the point thresholds are not.
- **"4% 让转移单从 +$1.07 变成 −$0.30"**
  - It flips to +$0.11 on a 70% subtotal base when ads survive.
  - It stays negative once ads are lost (−$0.57 or worse).
  - In incremental terms the shifted order always loses (F1).
- **"增量单在 4% 时仍赚 $0.42"**
  - This becomes −$0.26 on the report's own ad-loss logic (GOV base), and +$0.01 to +$0.15 on a subtotal base.
- **Meituan "三分之一" and "2%–3%"**
  - These become about 77%–83% and 0.8%–1.3% at ¥0.4. Direction unchanged.
- **Nothing in S3–S6 depends on these parameters.** The S3 change (F6) is about evidence, not numbers.

### 4.4 Where the report should give ranges instead of point values

| Point value in the report | Suggested range |
|---|---|
| Ads 2% of GOV | 1.25%–3.1% (DoorDash's own data suggests the low end) |
| Thresholds 3% / 22% | 3%–33% for k ≤ 50% at errors 0.1%; about 48% at k 75% and ads 3.1%; about 66% with errors 1%; below 0 with a 5% basket lift at k ≤ 25%, or a 10% lift at k ≤ 50% |
| Meituan ¥1 per order | ¥0.4 base, range −¥0.3 to ¥1.0 |
| Meituan fee break-even 2%–3% | 0.6%–3.3%, negative if UE < 0 |
| "4% → −$0.30" | −$0.30 on GOV; +$0.11 to −$0.02 on subtotal; −$0.57 to −$0.98 with ads lost |
| Sonnet $0.35 | $0.36 at the verified $0.20 cache read; up to $0.41 with the cache-write premium |
| Mid-tier $0.14 | $0.14 (2026) / $0.27 (2027) |
| S4 robot saving $3–7 | $3–9 |
| S5 support saving $0.03–0.11 | $0.01–0.11 |
| Conversion 40% | 20%–40%; doubles per-order inference at the low end |

---

## 5. Framework application and the C1–C9 scoring: summary

| Framework | Applied correctly? | Drives a verdict? | Finding |
|---|---|---|---|
| Marketplace unit economics | Formula correct; applied inconsistently in §6.3; basket and fixed-cost terms dropped | Yes: S1 ③, S2 ③ | F1, F2, F4, F18 |
| Aggregation theory (Thompson, Winters) | Mostly; the marginal-cost wording is loose | Yes: S2 ② ("履约绕不过平台") | F17 |
| 7 Powers | Mapping is reasonable, but tested on size, not on the metrics §1 sets | Partly: S2 ② and S6 ③ cite it; no verdict rests on a measured power | F17 |
| Five Forces / value chain | Yes; the "agent as new buyer" framing follows the notes | Yes: S2 ③ (fees), S4 ③ (savings competed away), S1 (ads at the discovery layer) | — |
| JTBD / value proposition canvas | Yes for ① verdicts | ① verdicts yes; its main insight is not carried into the §5.3 numbers | F17 |
| C1–C9 scoring (§11.1) | 8 of 54 cells score a different condition from §1's definition | The 总体 row matches the scenario verdicts, except S3's evidence strength | F14, F6 |

**Verdicts that follow from the evidence:**
- S1 ①②③;
- S2 ② and ③ (toll booth);
- S3 ① and ②;
- S4 ①②③;
- S5 ①②③.

**Verdicts to qualify:**
- S2 ① (single source; F16);
- S2 ③ acquisition channel (F1);
- S3 ③ for DoorDash (management claims only; F6);
- S6 ③ (different question; F15).

---

## 6. Comparability and coherence: summary

- **USD vs RMB.** No conversion errors. The "1 美元或 1 元" framing invites a false parity (F3, F11).
- **GOV vs revenue vs subtotal.**
  - The fee base is GOV, while precedent fees are on sales (F2).
  - R&D is on GOV for DoorDash and on revenue for Meituan (F11).
  - Instacart's ad figure includes other revenue (F20).
- **Food delivery vs segment.**
  - Meituan's 93% and the ¥51亿 ad sensitivity are segment-wide, including 到店酒旅 ads, but are used for a food-ordering agent (F10, F11).
  - DoorDash figures are company-wide, including grocery, international and Drive orders in the denominator; the report notes the last of these.
- **Quarter vs year.**
  - Meituan 2025 ads vs Q2 2026 annualized profit (F11).
  - DoorDash Q3 2026 is its best per-order quarter (F4).
  - "几百单" is a cumulative count over an unknown window, divided by nine months of orders (F5).
- **Is the 结论 self-contained?** Mostly. Abbreviations are defined in the header. C-codes are not (F8).
- **Is it consistent with the body?** Not fully. The thesis covers only S1 and S2 (F7); "C9 和 C5 最难" conflicts with §11.1 (F8); S3 is overstated (F6); "几百单" is misattributed (F5).
- **Is §12 consistent with the earlier report?** Yes in substance. §12.3's "不碰付钱的那一层，或者让它变大" and §12.4's rule match the earlier report's §8.5 and its 结论 ("前提是那一层归你，而且不被 Agent 挤占"). Two silent redefinitions need a sentence each:
  - C9 drops the earlier ~60% gross-margin test (F8);
  - C7 is recast from "收费单位随 token 变化" to "收入按单、成本按会话" (fine, but S2's C7 cell then misapplies it; F14).

---

## 7. Script

`review/recompute_review.py` recomputes, in order:
1. DoorDash per-order economics, the 9M averages and the implied 2024 GOV.
2. Inference cost per session and per order for 11 price points, including both Sonnet cache prices ($0.20 is marked as the one to use), cache-write and Haiku cache-read variants, and 20% conversion.
3. Merchant-side inference and break-even.
4. Ad loss, error cost, the threshold table, and grids A–E with corner cases.
5. Channel fees on GOV and subtotal bases, with and without ad loss.
6. Meituan margins, mix, the 93% ratio, implied UE, inference shares at UE ¥1 / 0.45 / 0.4 / 0.3, fee break-evens, and S4 and drone scale at 65M / 70M / 85M orders a day.
7. Remaining derived figures (S4, S5, R&D bases, third-party order ratio, subsidy, LongCat, Fin, peers).