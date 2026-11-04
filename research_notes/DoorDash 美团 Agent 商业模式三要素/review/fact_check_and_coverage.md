# Fact-check and coverage review: `reports/DoorDash 美团 Agent 商业模式三要素.md`

Review date: 2026-11-04. Status: **complete**. All six tasks in the brief are covered.

## Scope and method

- **Report reviewed:** `reports/DoorDash 美团 Agent 商业模式三要素.md`, 655 lines (file time 2026-11-04 01:09), read in full. Line numbers below refer to that version.
- **Sources of truth:**
  - This round's five notes files plus `working_files/` (the handback summary and `inference_cost_estimate.py`).
  - The earlier round's report and its six notes files. The new report draws on them for the MAU table, the 腾讯研究院 0.9% figure, the 光明网 link-blocking story, the Grok Bot comparison and the 价值三角形 framework.
- **What was checked:**
  - Every number, date, price, quote and named claim in 结论, all tables, §2 and §5–§10 was traced to a note. The rest of the report was read and spot-checked.
  - All arithmetic was recomputed: per-order economics, the inference-cost tables (by re-running `working_files/inference_cost_estimate.py` and recomputing the LongCat column, which the script doesn't cover), break-even thresholds, the channel-fee table, support savings, and the robot and drone sensitivities.
  - Automated checks: all 140 URLs cited in the report appear in the two rounds' notes. A numeric sweep found no number without a source in the notes, apart from rounded or computed values. Heading numbering, tables and cross-references are intact.
- **Browsing:** none. Every claim could be resolved from the notes.
- **Item format:** each finding gives Location (section, line and the quoted sentence), Problem, Evidence (notes file plus a quote), Severity, and Replacement (the exact Chinese text to use). Coverage omissions carry a must-add or nice-to-have rating in place of a severity.

## Summary

- **The arithmetic holds.** Every computed or estimated figure I recomputed matches the report:
  - DoorDash per order: $34.2 GOV, $4.65 revenue, $1.79 CP, $1.07 Adj. EBITDA, $0.47 net income.
  - Meituan CLC revenue mix: 38.0% / 36.8% / 19.7%; merchant-paid share 58%; ads at 93% of 2024 CLC operating profit.
  - Inference cost as a share of DoorDash per-order EBITDA: 2% / 13% / 32%. Break-even thresholds: 3–22% and 10–29%.
  - Meituan inference shares: 1% / 2% / 2% (reorder) and 13% / 33% / 31% (discovery and order).
  - The channel-fee table; $0.2–1.2 per merchant per month; $99.3M per quarter ≈ 0.29 pp; ¥13 亿 ≈ 6%; about 6,600 robots; support savings of $1.2–4.3 亿 ≈ 3–10% of EBITDA.
- **Four major problems; none critical:**
  1. The 结论's Meituan range "−¥0.3 到 ¥1.5" rests on a superseded pre-war forecast (item 1).
  2. Douyin's ~12% hotel rate is attributed to 豆包 and treated as an add-on toll. It is a total take that replaces the 8% natural-traffic rate (item 2).
  3. "两家都只让外部 Agent 做发现" contradicts the notes and the report's own §6.1 (item 3).
  4. "AI 外呼…这是通用 Agent 没有的能力" is contradicted by 千问's AI phone booking via 高德 (item 4).
- **Required corrections:**
  - (a) and (d) are applied correctly everywhere they appear.
  - (b) has gaps in about a dozen table cells and restated figures (item 30).
  - (c) has the right dates, but the citation doesn't state the TechCrunch date (item 31).
- **Coverage:** five must-add omissions and 20 nice-to-have groups (items 36–60). The must-adds are:
  - Alipay's agent-payment numbers;
  - DoorDash's MCP access terms;
  - Adobe's conversion data;
  - the 2030 forecasts that §14 already refers to;
  - 千问开放平台, which reportedly takes no cut of transactions.
- **Earlier report:** structurally intact, with one unsupported claim (Claude Pro "34 倍", major) and one notes anomaly worth rechecking (items 61–62).

---

## A. Fact-check and citation findings

### Major

1. **Meituan per-order profit range relies on a superseded pre-war forecast**
   - **Location:**
     - 结论, line 12: "两家每单都只赚约 1 美元或 1 元：DoorDash Q3 2026 每单 Adj. EBITDA 约 $1.07（计算）；美团外卖每单经营利润，券商对 2026 年的估算在 −¥0.3 到 ¥1.5 之间。"
     - The same range appears in §5.3, line 289 ("假设：每单利润取 ¥1（券商估算区间为 −¥0.3 到 ¥1.5）；"), in the §5.3 table, line 297 ("每单约 −¥0.3 到 ¥1.5（券商估算）"), and in the §11.3 table, line 567 ("外卖约 −¥0.3 到 ¥1.5（券商估算），2026 Q2 刚转正").
     - §6.2, line 357 ("外卖每单只有约 ¥1（券商估算）") relies on it.
   - **Problem:** This is superseded information presented as current.
     - The ¥1.5 upper bound is Bernstein's June 2025 pre-war projection; the report's own §2.2 table labels it "战前预测".
     - The post-war 2026 estimates in the notes are −¥0.3 (UBS, full-year 2026) and ¥0.4–0.5 (J.P. Morgan, H2 2026).
     - So the ¥1 per order used in §5.3 and §6.3 sits above every post-war estimate. Meituan's inference-cost shares are therefore lower bounds, and the 2.0%–3.3% break-even channel fee is an upper bound. The report says neither.
     - Recomputed at ¥0.5 per order: a discovery-and-order session costs about 66% of profit with DeepSeek Flash and 62% with LongCat, and the break-even channel fee falls to 1.0%–1.7%.
   - **Evidence:** meituan.md, Q1 §C:
     - "Pre-war benchmark: in June 2025 Bernstein projected food-delivery profit per order of RMB1.25 (2025) and RMB1.50 (2026)."
     - "UBS (2026-06-02): Q1 2026 loss of about RMB0.9 per order, Q2 near breakeven, FY2026 −RMB0.3 per order."
     - "J.P. Morgan (Nov 2025)… RMB0.4–0.5 profit per order in H2 2026."
   - **Severity:** major
   - **Replacement:**
     - 结论: "两家每单都只赚约 1 美元或不到 1 元：DoorDash Q3 2026 每单 Adj. EBITDA 约 $1.07（计算）；美团外卖每单经营利润没有官方数字，战后的券商估算是 2026 全年约 −¥0.3（UBS）、2026 下半年 ¥0.4–0.5（J.P. Morgan），管理层称 Q2 已转正；战前 Bernstein 曾预测 2026 年 ¥1.5。"
     - §5.3, line 289: "假设：每单利润取 ¥1，高于战后的券商估算（UBS 2026 全年约 −¥0.3，J.P. Morgan 下半年 ¥0.4–0.5），所以下表比例是下限；若每单利润只有 ¥0.5，比例翻倍，发现+下单会话用 DeepSeek Flash 约占 66%、用 LongCat 约占 62%；"
     - §5.3 table, line 297: "每单约 −¥0.3 到 ¥0.5（战后券商估算；战前预测为 ¥1.5）"
     - §11.3 table, line 567: "外卖约 −¥0.3 到 ¥0.5（战后券商估算；战前预测为 ¥1.5），2026 Q2 刚转正"
     - §6.2, line 357: "外卖每单不到 ¥1（券商估算）"
     - §6.3, line 374: after "可承受的通道费上限约 2.0%–3.3%（估算）。" add "若按战后估算的 ¥0.5 计，上限只有约 1.0%–1.7%。"

2. **Douyin's ~12% hotel rate is attributed to 豆包 and used as an add-on toll**
   - **Location:**
     - 结论, line 24: "**第三方 Agent 的通道费，比每单利润还高。** … 豆包对导向抖音的酒店订单收约 12% ([界面](https://m.jiemian.com/article/14902227.html))。"
     - §3.3 table, line 179: "豆包对酒店收约 12%；中金认为微信 Agent 入口可抬高有效佣金"
     - §6.2 table, line 341: "抖音生活服务；酒店订单渠道费约 12%"
     - §6.3 table, line 369: "| 12%（豆包酒店渠道费） | $4.10 | −$3.03 | −$2.31 |"
     - §6.3, line 374: "按豆包酒店的约 12% 收，每单要付 ¥3.6–6.0，是该单利润的 3.6–6 倍（估算）。"
     - §13, line 617: "豆包把导向抖音的酒店订单单列为约 12% 的渠道费率"
   - **Problem:** The report misattributes the fee and misapplies it.
     - **Who sets it:** a Douyin Life Services (抖音来客) notice of 2026-07-27, and hotel merchants pay it. Doubao's PR lead called the claim that "豆包收费推荐酒店" inaccurate.
     - **What it is:** about 12% is the *total* take on hotel orders referred by Doubao (11.4% software service fee plus 0.6% payment fee). It replaces the 8% natural-traffic rate, so it includes Douyin's own platform commission. The premium for the AI entry point is about 4 percentage points.
     - **How the report uses it:** §6.3 stacks the full 12% on top of DoorDash's and Meituan's own take, as if an outside agent charged it. That overstates the AI-channel cost about threefold.
     - **What survives:** the 结论's headline still holds with the 4 pp premium, which exceeds DoorDash's 3.1% and Meituan's 2–3% break-even rates.
     - **Wording:** no third-party agent currently charges either platform (OpenAI's 4% was withdrawn), so the bullet heading should be conditional.
   - **Evidence:**
     - agentic_commerce_channels.md §3: "Hotel orders that go from Doubao to Douyin Laike (抖音来客) now carry a separate rate of **about 12%**: an 11.4% software service fee plus a 0.6% payment fee. Before this they counted as Douyin natural traffic at **8%**. The basis is a Douyin Life Services notice of 2026-07-27, effective 2026-08-10. Doubao's PR lead said the claim that 'Doubao charges for recommending hotels' is inaccurate. Doubao charges no advertising fee…"
     - doordash.md Q2: "No fee terms with any agent platform have been disclosed."
   - **Severity:** major
   - **Replacement:**
     - 结论 heading: "**第三方 Agent 若收通道费，费率会高过每单利润。**"
     - 结论 sentence: "抖音则把经豆包进入抖音来客的酒店订单单列为约 12% 的费率，此前按自然流量收 8%，相当于给 AI 入口加价约 4 个百分点 ([界面](https://m.jiemian.com/article/14902227.html))。"
     - §3.3 table cell: "抖音对豆包导流的酒店订单收约 12%（自然流量 8%）；中金认为微信 Agent 入口可抬高有效佣金"
     - §6.2 table cell: "抖音生活服务；豆包导流的酒店订单费率约 12%（自然流量 8%）"
     - §6.3 table, first column of the 12% row: "12%（压力测试：相当于抖音对豆包导流酒店单的总费率，其中约 8 个百分点是抖音自身佣金，AI 入口加价约 4 个百分点）"
     - §6.3, line 374: "按抖音给豆包导流的加价约 4 个百分点算，每单多付 ¥1.2–2.0，是该单利润的 1.2–2 倍；若按 12% 全额另收（压力测试），每单 ¥3.6–6.0（均为估算）。"
     - §13: "中国已经先走一步：抖音把经豆包进入的酒店订单单列为约 12% 的费率，比自然流量的 8% 高约 4 个百分点 ([界面](https://m.jiemian.com/article/14902227.html))；"

3. **"Only discovery for outside agents" contradicts the body and the notes**
   - **Location:**
     - 结论, line 24: "两家都只让外部 Agent 做"发现"，账户、结账、会员和履约留在自己手里。"
     - Related: 结论, line 25: "DoorDash 的对策是自己做场外 Agent（Text DoorDash、企业 MCP），把战场定在"atoms"。"
   - **Problem:** Outside agents already place orders and initiate payments; the platforms keep the account and payment rails, not the ordering step.
     - Gemini operates the DoorDash app through to the order, with the user confirming.
     - DoorDash's corporate MCP lets approved agents place orders.
     - The "几百单" cited in the next sentence are, by definition, orders placed by agents.
     - In WorkBuddy, the agent initiates payment from the AI card.
     - What the platforms keep is the account, checkout and payment rails, membership and fulfilment.
     - Separately, the corporate MCP is an access interface for outside agents, not a DoorDash agent.
   - **Evidence:**
     - doordash.md Q2: "Gemini (from March 2026) drives the DoorDash app itself inside a sandbox."
     - doordash.md Q2: "It runs the app in a secure virtual window, and the user confirms the final purchase."
     - doordash.md Q2: "Third parties report the MCP gives agents four capabilities: search, build cart, place order and track delivery".
     - agentic_commerce_channels.md §2: "Secondary sources describe the MCP as a beta that lets *approved* third-party agents order on a user's behalf."
     - agentic_commerce_channels.md §3: "The AI initiates payment, the user confirms on their phone, and the amount is debited from the AI card."
   - **Severity:** major
   - **Replacement:**
     - Line 24: "两家都允许外部 Agent 发现和下单，但只走平台认可的通道，账户、结账、会员和履约留在自己手里：DoorDash 的企业 MCP 要申请开通，Gemini 代操作时由用户确认最终下单；美团只向腾讯系入口开放，交易由美团自建的能力和优惠券完成。"
     - Line 25: "DoorDash 的对策是把自己的 Agent 放到场外（Text DoorDash），并用企业 MCP 控制外部 Agent 的接入，把战场定在"atoms"。"

4. **"AI outbound calling is a capability general agents lack" is contradicted by the notes**
   - **Location:** §5.2, line 252: "AI 外呼把没数字化的商户也纳入可订的供给，这是通用 Agent 没有的能力。"
   - **Problem:** Alibaba's 千问, a general agent, launched "AI 打电话订餐厅" through 高德 on 2026-01-15. The report repeated an inference in meituan.md that the channels notes contradict.
   - **Evidence:**
     - agentic_commerce_channels.md §3: "[Official, 2026-01-15] Qwen App fully connected to Taobao, Alipay, Taobao Shangou (淘宝闪购), Fliggy (飞猪) and Amap (高德)… **Amap:** 'AI calls the restaurant to book' (AI 打电话订餐厅)."
     - Compare meituan.md, Q3 Inferences: "The AI-call feature extends supply to non-digitized merchants, a capability a general agent lacks."
   - **Severity:** major
   - **Replacement:** "AI 外呼把没数字化的商户也纳入可订的供给。这不是美团独有：千问 01-15 接入高德时也上线了"AI 打电话订餐厅"。美团的差别在于外呼背后的商户核验数据和自有的订位、排队交易轨道（推断）。"

### Minor

5. **An inference is presented as a cause in 结论**
   - **Location:** 结论, line 25: "美团的第一个支付型 Agent 渠道开在到店团购券上，没开在外卖上：团购不用骑手，利润更厚。"
   - **Problem:** The reason is the report's own inference; §6.2 says so ("这是本研究的推断，美团没有解释过顺序"). In 结论 it reads as an established cause.
   - **Evidence:** agentic_commerce_channels.md §3: "The use case is in-store group-buy, **not takeout**. No commission or fee terms were disclosed." No note records a reason from Meituan.
   - **Severity:** minor
   - **Replacement:** "美团的第一个支付型 Agent 渠道开在到店团购券上，没开在外卖上。本研究推断，原因是团购不用骑手、利润更厚；美团没有解释过顺序。"

6. **The 10-Q quotation drops half of the offset**
   - **Location:** 结论, line 23: "DoorDash 的 10-Q 写明，消费者费用率的下降由广告补回"
   - **Problem:** The 10-Q credits ads *and* lower discounts and promotions. §2.1 has it right; 结论 overstates the role of ads.
   - **Evidence:** doordash.md Q0: "Consumer fees as % of GOV fell, 'offset by increased contribution from advertising revenue and reductions in discounts and promotions'".
   - **Severity:** minor
   - **Replacement:** "DoorDash 的 10-Q 写明，消费者费用率的下降由广告收入增加和折扣促销减少补回"

7. **Citation mismatch for the US price anchor; the anchor's scope is missing**
   - **Location:** 结论, line 26: "美国单店 Agent 的价格锚是每月 $399–499；中国是零，美团的 AI店长、袋鼠参谋都免费 ([Katalyst](...); [雷锋网](...))。"
   - **Problem:**
     - Katalyst supports only $399 (Slang Core) and $450 (Kea). The $499 comes from Owner.com and Toast IQ Grow.
     - 袋鼠参谋's free status is sourced to 新浪; 雷锋网 covers AI店长.
     - The $399–499 anchor applies to revenue-facing agents. SMB phone AI lists from about $85.
   - **Evidence:** merchant_agent_market.md Q1:
     - "Revenue-facing agents cluster around $400–$500 per location per month: Slang Core $399, Kea $450, Owner $499, Toast IQ Grow $499."
     - "Published list prices run from about $85 to $599 per location per month".
     - "袋鼠参谋 launched October 2025… free to all merchants — [Sina, 2025-10-16]".
   - **Severity:** minor
   - **Replacement:** "美国营收类单店 Agent 的价格锚是每月 $399–499（Slang Core $399、Kea $450、Owner 和 Toast IQ Grow $499）；中国是零，美团的 AI店长、袋鼠参谋都免费 ([Katalyst](https://www.katalystos.com/blog/ai-phone-answering-for-restaurants-what-it-costs); [Owner](https://www.owner.com/pricing); [Yahoo Finance](https://finance.yahoo.com/markets/stocks/articles/toast-tost-launches-toast-iq-132858023.html); [雷锋网](https://www.leiphone.com/category/industrynews/tZI7pYBhiFduVfr6.html); [新浪](https://client.sina.com.cn/news/2025-10-16/doc-infucfih9409659.shtml))。"

8. **An inference about the "商家服务" revenue line is stated as fact**
   - **Location:** §2.2, line 109: "2026 年起，美团把佣金和广告合并为"商家服务"披露，两者不再能分开追踪"; also §14 item 4, line 642: "2026 年起佣金和广告合并披露。"
   - **Problem:** The notes document the new classification only in the Q2 2026 filing. That "商家服务" combines commission and advertising is the notes' inference ("appears to").
   - **Evidence:** meituan.md Q1:
     - "the Q2 2026 filing reports revenue by type as **Delivery services / Merchant services / Product sales / Others**."
     - Inferences: "the 2026 'Merchant services' line… **appears to** combine commission and online marketing".
   - **Severity:** minor
   - **Replacement:**
     - §2.2: "从 Q2 2026 财报起，美团按配送服务、商家服务、商品销售和其他披露收入；"商家服务"看来合并了佣金和在线营销（推断），两者不再能分开追踪"
     - §14: "Q2 2026 起佣金和广告看来已合并为"商家服务"披露（推断）。"

9. **The peer benchmark range omits two data points and contradicts its own table**
   - **Location:**
     - §2.3 heading, line 124: "### 2.3 同行：利润和广告都只占 GOV 的 2%–4%"
     - Line 136: "利润约为 GOV 的 3%–4%，广告也有 2%–3%。"
     - Table, line 128: "| Instacart（Q3 2026） | Adj. EBITDA 3.2% | 3.1% |"
     - Table, line 129: "| Uber Delivery（Q2 2026） | 分部经营利润 3.8%（计算） | 超过 2%（管理层目标口径，二手来源） |"
   - **Problem:**
     - The notes' peer set also includes Zomato at 5.5% (of NOV) and Delivery Hero at about 1.4%; both are omitted.
     - "3%–4%" contradicts the table's own Grab row (2.7%).
     - Uber's ad figure above 2% comes from the Q4 2025 call but sits in a Q2 2026 row.
     - Instacart's 3.1% is "advertising and other" revenue, not ads alone.
   - **Evidence:** analysis_toolkit_unit_economics.md §3:
     - "Segment profit runs about 2.7% of GOV at Grab, 3.4% at Swiggy, 3.8% at Uber Delivery, 3.2% at Instacart… and 5.5% at Zomato. Delivery Hero was about 1.4% in FY2025."
     - "On the Q4 2025 call, management said delivery ads had exceeded the prior 2%-of-GB penetration target".
     - "'advertising and other' revenue was $331M in Q3 2026, or 3.1% of GTV".
   - **Severity:** minor
   - **Replacement:**
     - Heading: "### 2.3 同行：利润多在 GOV 的 3% 上下，广告与利润同量级"
     - Line 136: "利润多在 GOV 的 2.7%–3.8%（Zomato 按 NOV 计为 5.5%，Delivery Hero FY2025 约 1.4%），广告有 2%–3%。"
     - Add rows: "| Zomato 外卖（Q2 FY27） | Adj. EBITDA 5.5%（占 NOV） | 未披露 |" and "| Delivery Hero（FY2025） | Adj. EBITDA 约 1.4%（占 GMV） | 未披露 |"
     - Uber ad cell: "超过 2%（2025 Q4 电话会口径，二手来源）"
     - Instacart ad cell: "3.1%（广告及其他）"

10. **The review count is superseded by a higher official figure**
    - **Location:** §3.2 table, line 161: "13 亿+ 条评价、7 亿+ 次商户信息核验"; §5 table, line 224: "自研 LongCat；13 亿+ 条评价"; §5.2, line 252: "小团背后有 13 亿+ 条评价、7 亿+ 次商户信息核验，覆盖 2,800+ 城市"
    - **Problem:** The 13 亿 figure comes from a June media report. Meituan's official Q2 2026 filing (2026-08-28) gives 15 亿.
    - **Evidence:** meituan.md Q1 §H: "in Q2 2026 it launched the latest 必吃榜, built on '1.5 billion authentic user reviews'… [Official]"; Q3: "over 13亿 reviews… — Jiemian".
    - **Severity:** minor
    - **Replacement:** "15 亿条真实评价（美团 Q2 2026 财报）、7 亿+ 次商户信息核验，覆盖 2,800+ 城市" (shorten to "15 亿条评价" in the table cells).

11. **小团's model is stated too narrowly**
    - **Location:** §5.2, line 252: "模型是自研的 LongCat-2.0：1.6 万亿参数 MoE，每 token 激活约 480 亿，在国产算力上训练"
    - **Problem:** The FY2025 filing says the assistant integrates LongCat *and open-source models*. No note says 小团 runs on LongCat-2.0 specifically. §11.3 states it correctly.
    - **Evidence:** meituan.md Q2: "develop a versatile AI assistant for local services by integrating… LongCat… and open-source models".
    - **Severity:** minor
    - **Replacement:** "模型是自研的 LongCat 加开源模型（年报口径）；最新的 LongCat-2.0 是 1.6 万亿参数 MoE，每 token 激活约 480 亿，在国产算力上训练"

12. **The 25% 到店酒旅 margin is a Q2 forecast, not a segment figure, and is guided lower**
    - **Location:** §5.3 table, line 299: "| 到店酒旅 | 分部利润率约 25%，2024 年前超过 40%（券商估算） |"; §6.2, line 357: "到店酒旅的分部利润率约 25%"; §11.3, line 569: "到店酒旅：分部利润率约 25%（券商估算）"
    - **Problem:**
      - 25% is Morgan Stanley's and Nomura's *forecast* for Q2 2026, not an actual result.
      - 到店酒旅 is not a reported segment (CLC is), so "分部" is misleading.
      - The CFO guided this margin down in Q3–Q4.
    - **Evidence:** meituan.md Q1 §C and §H:
      - "Q2 forecast: … in-store/hotel/travel (IHT) operating profit about RMB4.3bn at about 25% margin."
      - "Nomura: Meituan's 到店酒旅 margin was over 40% before 2024, is about 35% long-term, and is forecast at about 25% for Q2 2026."
      - "IHT operating margin will fall in Q3–Q4 as investment rises."
    - **Severity:** minor
    - **Replacement:**
      - §5.3: "经营利润率：券商预测 2026 Q2 约 25%（2024 年前超过 40%），管理层预计 Q3–Q4 因加大投入而下降"
      - §6.2: "到店酒旅的经营利润率约 25%（券商对 2026 Q2 的预测）"
      - §11.3: "到店酒旅：经营利润率约 25%（券商对 2026 Q2 的预测）"

13. **The 到店 GTV comparison suggests parity the source does not**
    - **Location:** §5.3, line 301: "到店的对手在逼近：据虎嗅，抖音到店核销前 GTV 预计 2026 年约 1.2 万亿元；野村估算美团 2025 年到店 GTV 约 1.18 万亿元"
    - **Problem:** The sentence sets Douyin's 2026 estimate (pre-redemption) against Meituan's 2025 estimate and omits the same source's post-redemption comparison, which still puts Meituan well ahead.
    - **Evidence:** meituan.md Q1 §H: "Post-redemption, the author estimates Douyin at about RMB600bn (≈60% redemption) and Meituan at over RMB900bn (an assumed 80%)."
    - **Severity:** minor
    - **Replacement:** "到店的对手在逼近：据虎嗅，抖音到店核销前 GTV 预计 2026 年约 1.2 万亿元，野村估算美团 2025 年约 1.18 万亿元；但按核销后口径，作者估算抖音约 6,000 亿元、美团超过 9,000 亿元 ([虎嗅](https://www.huxiu.com/article/4888998.html))。"

14. **The pricing rules are said to restrict "paid to be chosen"**
    - **Location:** §5.4, line 307: "美团外卖的每单利润只有约 ¥1，价格规则又限制"付费被选中"，S1 的账更多要靠到店和闪购来结。"
    - **Problem:** The 2026-04-10 pricing rules target 大数据杀熟, disclosure of differential pricing and forced price cuts. They don't directly regulate paid ranking. The notes link paid placement to trust and regulatory *risk* as an inference only.
    - **Evidence:** analysis_toolkit_unit_economics.md §6:
      - "Pushes disclosure of dynamic and differential pricing rules… Prohibits below-cost dumping, '大数据杀熟'…, price gouging, and forcing merchants to cut prices."
      - Inference: "The April 2026 rules make agent-driven personalized pricing and dynamic-discount optimization legally risky."
    - **Severity:** minor
    - **Replacement:** "美团外卖的每单利润只有约 ¥1 或更低；2026-04-10 生效的价格规则让 Agent 做个性化定价和差异化优惠的法律风险变高，付费排序还要面对"AI 推贵的"信任问题（推断），S1 的账更多要靠到店和闪购来结。"

15. **The Ninth Circuit outcome lacks a date, and one citation doesn't support "技术封锁"**
    - **Location:** §6.1, line 335: "法律的护栏在变弱：第九巡回法院撤销了地区法院应 Amazon 请求对 Perplexity Comet 下达的初步禁令，理由是访问 Amazon 的是用户，不是 Perplexity ([Engadget](...))。平台转而靠条款（eBay 2026-02-20 起禁止未经批准的购物 Agent）和技术封锁 ([EcommerceBytes](...))。"
    - **Problem:**
      - The dates are missing (injunction 2026-03; vacated, reported 2026-08-04), as are the later denial of rehearing and the remand to the district court.
      - EcommerceBytes covers eBay only. The technical blocking (Amazon's robots.txt) needs its own source.
    - **Evidence:** agentic_commerce_channels.md §2:
      - "Preliminary injunction, about 2026-03-09 to 03-10"
      - "Ninth Circuit vacated the injunction (reported 2026-08-04)… Amazon had not shown irreparable harm."
      - "Rehearing denied later… The case goes back to the district court."
      - "By early 2026, Amazon's robots.txt blocked nearly 50 AI-related user agents… — Tinuiti".
    - **Severity:** minor
    - **Replacement:** "法律的护栏在变弱：据报道，第九巡回法院 2026-08 撤销了地区法院 2026-03 应 Amazon 请求对 Perplexity Comet 下达的初步禁令，理由是访问 Amazon 的是用户，不是 Perplexity，且 Amazon 没有证明不可弥补的损害；此后又驳回了重审请求，案件发回地区法院 ([Engadget](https://engadget.com/2230471/perplexity-has-successfully-overturned-amazon-injunction-on-its-ai-shopping-bot); [MediaPost](https://www.mediapost.com/publications/article/417007/appeals-court-sides-against-amazon-lifti.html))。平台转而靠条款（eBay 2026-02-20 起禁止未经批准的购物 Agent）([EcommerceBytes](https://www.ecommercebytes.com/2026/01/21/ebay-bans-ai-shopping-agents-updates-arbitration-provision/)) 和技术封锁（Amazon 的 robots.txt 屏蔽了近 50 个 AI 用户代理）([Tinuiti](https://tinuiti.com/blog/amazon/amazon-agentic-commerce/))。"

16. **A 千问 company claim is presented as plain fact**
    - **Location:** §6.2, line 349: "春节"30 亿大免单"的 6 天里，用户说了 41 亿次"千问帮我"，AI 完成 1.2 亿+ 单 ([扬子晚报](...))。"
    - **Problem:** These are Alibaba's own promotional figures from a subsidy period. The notes tag them as a company claim.
    - **Evidence:** agentic_commerce_channels.md §3: "**[Co. claim, 2026-02-12]** Spring Festival '30亿大免单'… These are subsidised promotional volumes, not steady state."
    - **Severity:** minor
    - **Replacement:** "据阿里公布的数据，春节"30 亿大免单"的 6 天里，用户说了 41 亿次"千问帮我"，AI 完成 1.2 亿+ 单，都是补贴期的数字 ([扬子晚报](https://www.yzwb.net/news/ch/202602/t20260212_321578.html))。"

17. **The 4%–13% fee may double-count the 12% hotel rate**
    - **Location:** §6.2, line 351: "抖音还对部分品类由豆包导流的订单另收 4%–13% 技术服务费 ([人人都是产品经理](...))。"; §7.3, line 434: "三是 AI 渠道附加费（抖音对豆包导流订单另收 4%–13%）。"
    - **Problem:** The 4%–13% fee starts on the same date (08-10) and covers accommodation, so it is probably the same policy as the 12% hotel rate described differently. The notes do not reconcile the two. "还…另收" presents it as an additional charge. The year is inferred.
    - **Evidence:**
      - merchant_agent_market.md Q2: "Since August 10 (year inferred as 2026 from the article's context), merchants in accommodation, fitness, home, education and maternity categories pay an extra 4%–13% technical service fee on orders referred by Doubao."
      - agentic_commerce_channels.md §3: hotels about 12% from 2026-08-10, against 8% before.
    - **Severity:** minor
    - **Replacement:**
      - §6.2: "另一篇报道称，同日起住宿、健身、家居、教育、母婴等品类经豆包导流的订单要多付 4%–13% 的技术服务费（可能与上面的酒店费率是同一政策的不同表述；年份为推断）([人人都是产品经理](https://www.woshipm.com/ai/6485093.html))。"
      - §7.3: "三是 AI 渠道附加费（抖音对豆包导流订单加收 4%–13%，据报道）。"

18. **The payment convention is paraphrased beyond its text**
    - **Location:** §6.2, line 359: "前述支付清算协会公约要求持牌机构完成结算、逐笔核验意图、自主支付应用备案。"
    - **Problem:** "逐笔" (per transaction) is not in the convention summary. The convention requires strict verification of identity and transaction intent, plus tiered transaction limits, and covers account management, transaction processing and settlement.
    - **Evidence:** agentic_commerce_channels.md §3:
      - "Account management, transaction processing and fund settlement must be done by licensed institutions."
      - "Members must… strictly verify user identity and transaction intent."
      - "Tiered, classified management with transaction limits."
    - **Severity:** minor
    - **Replacement:** "前述支付清算协会公约要求账户管理、交易处理和资金结算由持牌机构完成，严格核验用户身份和交易意图，分级设交易限额，Agent 自主发起支付的应用上线前要备案。"

19. **Citrini's 7% is mislabelled as a channel fee**
    - **Location:** §6.3 table, line 368: "| 7%（Citrini 假设情景） | $2.39 | −$1.32 | −$0.60 |"
    - **Problem:** In Citrini's scenario, 7% is the take of an AI app that bypasses DoorDash, not a fee charged *to* DoorDash.
    - **Evidence:** analysis_toolkit_unit_economics.md §1: "AI apps could transact directly with restaurants at about a 7% take rate, versus DoorDash's roughly 25% blended take, and could auction deliveries to drivers."
    - **Severity:** minor
    - **Replacement:** "| 7%（假设费率；借用 Citrini 情景的数字，原情景是 Agent 绕过 DoorDash 直连餐厅，不是向 DoorDash 收费） | $2.39 | −$1.32 | −$0.60 |"

20. **The restaurant margins lack their year and source caveat**
    - **Location:** §7.1, line 402: "税前利润率中位数只有 2.8%（正餐）和 4.0%（快餐）([Duck-Hub 汇编 NRA 数据](...))。"
    - **Problem:** These are FY2024 figures from a secondary compiler and weren't verified against the paywalled primary. The 42% unprofitable figure from the same source is left out.
    - **Evidence:** merchant_agent_market.md Q3: "Median pre-tax income for FY2024 was 2.8% of sales for full-service and 4.0% for limited-service… 42% of operators were not profitable in the past year, up from 29%… The primary source is paywalled and these figures were not verified directly."
    - **Severity:** minor
    - **Replacement:** "按 NRA 的 FY2024 数据（二手汇编，未直接核实），税前利润率中位数只有 2.8%（正餐）和 4.0%（快餐），42% 的经营者过去一年没有盈利 ([Duck-Hub 汇编 NRA 数据](https://www.duck-hub.com/blog/restaurant-profit-margin-statistics))。"

21. **The AI customer-service anecdote is misattributed**
    - **Location:** §7.3, line 436: "也有商户关掉了平台的 AI 客服，说"礼貌有余、能力不足" ([人人都是产品经理](...))。"
    - **Problem:** The notes say these merchants switched off Taobao's and JD's AI customer service. In a Meituan context, "平台的" implies Meituan.
    - **Evidence:** merchant_agent_market.md Q2: "Some merchants turned off Taobao and JD AI customer service ('礼貌有余、能力不足')."
    - **Severity:** minor
    - **Replacement:** "也有商户关掉了淘宝、京东的 AI 客服，说"礼貌有余、能力不足" ([人人都是产品经理](https://www.woshipm.com/ai/6485093.html))。"

22. **The price table omits a caveat and a conflict**
    - **Location:** §7.3 table, line 415: "| Owner.com（网站、点单、营销） | $249/月 + 每单 5%，或 $499/月 |"; line 413: "| Kea（电话点单 AI） | 每月 $450 |"
    - **Problem:** Owner also charges guests a 5% fee on both plans. Kea's $450 comes from the vendor's own blog and conflicts with a third-party $199–299.
    - **Evidence:** merchant_agent_market.md Q1: "guests pay a 5% 'order support fee' on both plans"; "$450, unlimited calls (vendor's own blog…). One third-party review lists $199–$299, which conflicts."
    - **Severity:** minor
    - **Replacement:** "| Owner.com（网站、点单、营销） | $249/月 + 每单 5%，或 $499/月；两档都另向顾客收 5% 订单支持费 |"; "| Kea（电话点单 AI） | 每月 $450（厂商博客；另有第三方列 $199–299，有冲突） |"

23. **The delivery-platform standard is presented as binding**
    - **Location:** §8.3, line 476: "中国的《外卖平台服务管理基本要求》要求调度算法考虑路况和天气，不得用算法强制超时 ([第一财经](...))"
    - **Problem:** It is a recommended national standard, not legally binding, although the major platforms pledged to comply.
    - **Evidence:** analysis_toolkit_unit_economics.md §6: "(recommended national standard, Dec 2025)… It is not legally binding, but major platforms pledged on 2025-12-04 to comply."
    - **Severity:** minor
    - **Replacement:** "中国 2025-12 发布的推荐性国家标准《外卖平台服务管理基本要求》要求调度算法考虑路况和天气，不得用算法强制超时；它不具法律强制力，但主要平台 2025-12-04 承诺执行 ([第一财经](https://www.yicai.com/news/102948456.html))"

24. **The Instacart synergy timing is attributed to the wrong source and lacks the year-one detail**
    - **Location:** §10, line 514: "公司预计第三年起每年净协同"5 亿美元或更多" ([交易公告](...))。"
    - **Problem:** The deal release gives a multi-year path to "$500 million or more". The year-3 timing comes from the CFO on the Q3 call. Also left out: over 100% of synergies are reinvested in year 1, and net synergies turn positive in year 2.
    - **Evidence:** doordash.md Q0: "Synergies: a multi-year path to '$500 million or more' of annual net synergies. Over 100% of synergies would be reinvested in year 1, with net-positive synergies from year 2."; "[CALL, 2026-10-29]… He expects about $500M net by year 3".
    - **Severity:** minor
    - **Replacement:** "公司称有一条多年路径实现每年"5 亿美元或更多"的净协同：第一年把超过 100% 的协同再投入，第二年起净协同转正，CFO 预计第三年约 5 亿美元 ([交易公告](https://www.sec.gov/Archives/edgar/data/1792789/000114036126041111/ef20084547_ex99-1.htm); [DoorDash Q3 2026 电话会](https://www.fool.com/earnings/call-transcripts/2026/10/29/doordash-dash-q3-2026-earnings-call-transcript/))。"

25. **The LongCat prices are launch promotions for the Preview**
    - **Location:** §10 table, line 522: "| LongCat-2.0 促销价 | 输入 ¥2、输出 ¥8 每百万 token；¥9.9 买 5,000 万 token（约 ¥0.2/百万）；¥399 买 10 亿 token |"
    - **Problem:** These are promotional prices at the LongCat-2.0-*Preview* launch (2026-06-30). ¥9.9 and ¥399 are monthly cards. Regular prices are not confirmed.
    - **Evidence:** meituan.md Q8:
      - "pricing at the LongCat-2.0-Preview launch (2026-06-30): Promotional pay-as-you-go… Regular prices are said to be higher but were not given."
      - "Monthly cards: ¥9.9 for 50M tokens; ¥399 for up to 1B tokens."
      - "New users get 10M free tokens after real-name verification."
    - **Severity:** minor
    - **Replacement:** "| LongCat-2.0-Preview 上线促销价（2026-06-30） | 输入 ¥2、缓存命中 ¥0.04、输出 ¥8 每百万 token；月卡 ¥9.9 含 5,000 万 token（约 ¥0.2/百万）、¥399 含 10 亿 token；实名新用户送 1,000 万 token；正式价未公布 |"

26. **The Rufus brand has reportedly been retired**
    - **Location:** §13, line 619: "Amazon 的 Rufus 品牌提示已经是一种 Agent 原生广告位，约 20% 互动过的用户会继续品牌对话 ([Modern Retail](...))。"
    - **Problem:** A secondary report says Amazon folded Rufus into "Alexa for Shopping" on 2026-05-13. The 20% figure is from the 2026-04-30 call, before that.
    - **Evidence:** agentic_commerce_channels.md §1: "[Press, secondary, 2026-05-13] Amazon retired the Rufus brand and folded it into a new agent, 'Alexa for Shopping'".
    - **Severity:** minor
    - **Replacement:** "Amazon 的 Rufus 品牌提示已经是一种 Agent 原生广告位，约 20% 互动过的用户会继续品牌对话（2026-04 电话会数据；据二手报道，Rufus 品牌 05 月已并入"Alexa for Shopping"）([Modern Retail](https://www.modernretail.co/technology/amazon-says-its-ai-shopping-assistant-is-gaining-traction-with-rufus-users-up-115/))。"

27. **智能掌柜 is not free for all 130 万+ merchants**
    - **Location:** 结论 table, S3 row, line 18: "美团 130 万+ 商户免费用"
    - **Problem:** 智能掌柜 is free for small and mid-sized merchants. The 130 万+ figure counts restaurant merchants served, which includes chains whose pricing isn't stated.
    - **Evidence:** meituan.md Q5: "智能掌柜: … **Free for small and mid-sized merchants.**"; "智能掌柜 had served over 130万 restaurant merchants".
    - **Severity:** minor
    - **Replacement:** "美团智能掌柜服务 130 万+ 餐饮商户，对中小商户免费"

28. **"The platform pays for coupons" applies to Meituan only**
    - **Location:** §11.1 table, C2 row, S2 column, line 539: "否：没人为 Agent 付钱，平台自掏优惠券"
    - **Problem:** Only Meituan is documented as supplying coupons (in WorkBuddy). Nothing shows DoorDash paying anything in S2.
    - **Evidence:** meituan.md Q4: "it built 'exclusive capabilities' for WorkBuddy… and supplies '专属优惠券'"; doordash.md Q2: "No money flows were disclosed in either direction."
    - **Severity:** minor
    - **Replacement:** "否：没人为 Agent 付钱；美团为 WorkBuddy 自掏专属优惠券，DoorDash 未披露任何费用"

29. **"No fees" is stated as fact; the notes found none disclosed**
    - **Location:** §3.3 Five Forces table, line 179: "OpenAI 4% 先例已撤回；目前没有收费"
    - **Problem:** The notes say no fee has been found or disclosed, not that none exists.
    - **Evidence:** agentic_commerce_channels.md §1 Gaps: "I found no fee for Alexa+, Gemini automation or ChatGPT apps paid either way… That most likely means no fee exists, but no document says so."
    - **Severity:** minor
    - **Replacement:** "OpenAI 4% 先例已撤回；未见披露任何费用"

---

## B. Required corrections: status

- **(a) WeChat Pay AI card: applied correctly everywhere.** The corrected facts appear in four places:
  - the header (line 7);
  - the §6.2 WorkBuddy row (line 344: "美团生活助手"卖到店团购券；首个服务伙伴);
  - the §6.2 AI 专属卡 paragraph (line 355, including "首个接入的 Agent 是腾讯 WorkBuddy", "美团是 WorkBuddy 里的首个服务伙伴", "卖的是到店团购券，不是外卖" and "双方没有披露佣金或分成");
  - 结论 bullet 3, line 25 ("第一个支付型 Agent 渠道开在到店团购券上").

  No location describes Meituan as the card's first user or as selling 外卖 through WorkBuddy. Matches agentic_commerce_channels.md §3: "the card's first user was WorkBuddy. Meituan was the first *merchant/service* partner in WorkBuddy… The use case is in-store group-buy, **not takeout**. No commission or fee terms were disclosed."
- **(b) Every estimate labelled: partly applied.** See item 30.
- **(c) Grok Bot throttling date: dates correct; report date not stated.** See item 31.
- **(d) Meituan vs 豆包: applied correctly.**
  - The December 2025 paragraph (§6.2, line 351) names WeChat, Taobao, Alipay and banks, not Meituan.
  - Every statement that Meituan refuses 豆包 cites the 10-14 list (150+ apps): 结论 line 25, the §6.2 table (line 341), the §6.2 paragraph (line 351), the §6.4 paragraph (line 386) and the §11.3 table (line 571).
  - Matches meituan.md Q4: "**No report found of Meituan specifically blocking 豆包 in December 2025.**"

30. **Some estimates and broker figures are unlabelled**
    - **Location and replacement:** add the label in each place below.
      - 结论 table S4 cell, line 19: "DoorDash 每单省 $0.10 ≈ 每季 9,900 万美元" → "DoorDash 每单省 $0.10 ≈ 每季 9,900 万美元（计算）"
      - 结论 bullet 5, line 27: "美团无人机累计 100 万单，不到一天外卖量的 2%" → "美团无人机累计 100 万单，不到一天外卖量的 2%（计算）"
      - §2.2, line 122: "据汇丰，2025 Q2 至 2026 Q1，…" → "据汇丰（券商估算），2025 Q2 至 2026 Q1，…"
      - §3.1, line 148: "汇丰预计 2026 Q4 份额稳定在约 50 / 40 / 10" → "汇丰预计（券商估算）2026 Q4 份额稳定在约 50 / 40 / 10"
      - §3.2 table, line 156: "外卖日单份额约 50%" → "外卖日单份额约 50%（汇丰估算）"
      - §3.3 table, line 183: "淘宝闪购约 40%，京东约 10%" → "淘宝闪购约 40%，京东约 10%（汇丰估算）"
      - §5.4, line 307, and §12.3, line 594: "（DoorDash 小模型下 3%–22%）" → "（DoorDash 小模型下 3%–22%，估算）"
      - §6.4, line 384: "任何超过约 3% 的费率都让转移单亏钱" → "任何超过约 3% 的费率都让转移单亏钱（估算）"
      - §11.1 C9 row, line 546: "否：4% 已超过 DoorDash 每单 EBITDA" → "否：4% 已超过 DoorDash 每单 EBITDA（估算）"
      - §11.2 单位经济 row, line 557: "小模型推理约占每单利润 2%；广告挤占 16%–32%（估算）" | "4% 通道费超过每单 EBITDA（估算）" | "每单省 $0.10 = 每季 9,930 万美元（计算）" | "客服每单省 $0.03–0.11（估算）"
      - §12.3 S2 row, line 595: "（DoorDash 转移单约 3.1%，丢广告时约 1.1%，增量单约 5.2%；美团外卖约 2%–3%）" → "（DoorDash 转移单约 3.1%，丢广告时约 1.1%，增量单约 5.2%；美团外卖约 2%–3%；均为估算）"
    - **Problem:** The header (line 6) promises that every computed figure is marked 计算, every estimate 估算 and every broker number 券商估算. These cells restate such figures without the label. HSBC's figures are attributed ("据汇丰") but not labelled 券商估算.
    - **Evidence:** meituan.md Q1 §D tags the HSBC loss and share figures "[Analyst]". analysis_toolkit_unit_economics.md marks the threshold, fee and support figures "(Est.)".
    - **Severity:** minor

31. **The TechCrunch report date is not stated in §12.4**
    - **Location:** §12.4 table, line 608: "Grok Bot 把 always-on Agent 放进平价额度池，2026-10-31 至 11-01 限流，Musk 承认成本"相对收入高得不可持续""; sources, line 611: "([TechCrunch（Yahoo 转载）](https://finance.yahoo.com/technology/ai/articles/elon-musk-throttles-grok-bot-162321608.html); …)"
    - **Problem:**
      - The event dates are correct, and nothing in the report misdates the story to October.
      - But the TechCrunch report date (2026-11-02) is never stated, so correction (c) is only implicit.
      - "限流" also reads as an announced limit change, when the notes record no official notice: users' weekly quotas ran out early, and Musk confirmed it was not a glitch.
    - **Evidence:** grok_bot_agent_product.md (earlier round):
      - "The TechCrunch article 'Elon Musk throttles Grok Bot limits due to "unsustainably high" costs' is dated **2026-11-02**… It covers the **Halloween weekend (2026-10-31 to 11-01)** and a Musk reply posted on **2026-11-01**"
      - "No official change to limits was published".
    - **Severity:** minor
    - **Replacement:**
      - Table cell: "Grok Bot 把 always-on Agent 放进平价额度池，2026-10-31 至 11-01 用户周额度提前耗尽，Musk 11-01 称服务成本"相对收入高得不可持续"（TechCrunch 2026-11-02 报道）"
      - Source label: "[TechCrunch，2026-11-02（Yahoo 转载）]"

---

## C. Internal consistency

32. **S5 is grouped with S4 as "high leverage"**
    - **Location:** 结论, line 12: "S4 和 S5 是成本账、杠杆大但未兑现"
    - **Problem:** The body supports "单位杠杆最大" only for S4. S5 is rated "采用成立，省钱未证" (§9, §11.1), and its estimated savings are 3%–10% of annualised EBITDA.
    - **Evidence:**
      - doordash.md Q0 Inferences: "So per unit, fulfillment-side automation (S4) has the most direct P&L leverage."
      - analysis_toolkit_unit_economics.md §3.D: "savings ≈ $0.03–$0.11 per order… about 2–7% of a $1.52 US per-order profit."
    - **Severity:** minor
    - **Replacement:** "S4 是成本账、单位杠杆最大但未兑现，S5 采用已成立、省钱未证"

33. **S3 is said to hold through ads for both companies**
    - **Location:** 结论, line 12: "S3 商户 Agent 靠广告预算已经成立"
    - **Problem:** This holds for DoorDash only. The 结论 table and §7.4 rate Meituan's value capture as indirect.
    - **Evidence:** meituan.md Q5: "价值获取: currently indirect. Tools are free or bundled, so returns come via merchant retention, better listings and ad ROI…"
    - **Severity:** minor
    - **Replacement:** "S3 商户 Agent 在 DoorDash 靠广告预算已经成立，在美团靠留存和广告效率间接回收"

34. **"Being invoked" is applied to agents that aren't invoked**
    - **Location:** §12.2, line 588: "宁可让自己的 Agent 被调用（Text DoorDash、小美 A2A），也不只给一个裸 API。"
    - **Problem:** No other agent invokes Text DoorDash. It is DoorDash's own agent running in Apple Messages. 小美 A2A has been announced but not launched.
    - **Evidence:**
      - doordash.md Q2: "the only Apple-surface product is DoorDash's *own* agent running over Apple Messages… It is not a Siri or Apple Intelligence integration"
      - meituan.md Q4: "Go-live not confirmed: searches up to Nov 2026 found no launch announcement."
    - **Severity:** minor
    - **Replacement:** "宁可把自己的 Agent 放进别人的入口（Text DoorDash 跑在 Apple 信息里；小美与元宝的 A2A 已宣布、未见上线），也不只给一个裸 API。"

35. **§14 caveats forecasts the body never presents**
    - **Location:** §14 item 7, line 645: "各家对 2030 年 Agent 电商规模的预测针对零售商品，McKinsey 的全球数字明确不含服务；套到外卖和本地生活上属于外推。"
    - **Problem:** The body never gives the 2030 forecasts, so the caveat has nothing to qualify.
    - **Evidence:** agentic_commerce_channels.md §4, "Forecasts for agentic commerce by 2030" (quoted in item 39).
    - **Severity:** minor
    - **Replacement:** keep §14 item 7, and add the forecasts to the body using the text in item 39.

---

## D. Coverage omissions

### Checklist of the items named in the brief

| Item | In the report? | Note |
|---|---|---|
| DoorDash–Instacart deal and synergy target | Yes (§2.1, §10) | Synergy sentence needs a fix (item 24); other terms are in item 41 |
| Ask / Text DoorDash / Zesty metrics; controlled test vs headline claims | Yes (§5.1 table) | Controlled-test contrast handled well; Zesty has only one line (item 42) |
| Xu: "several hundred orders"; "channel partners" | Yes (结论, §6.1) | Correct |
| Gemini operating the DoorDash app | Yes (§6.1) | Correct |
| DoorDash MCP for approved agents | Partly | Access terms and ordering capability missing (item 37) |
| Amazon v. Perplexity at the Ninth Circuit; eBay ban | Yes (§6.1) | No dates or rehearing outcome (item 15) |
| Alipay ACT agent-payment numbers | No | Item 36 |
| Payment & Clearing Association convention, 2026-08-24 | Yes (§4, §6.2) | Some provisions missing (item 47); paraphrase fix in item 18 |
| Douyin AI-channel fee (4–13%; ~12% hotels via 豆包) | Yes | Misattributed and misapplied (items 2, 17) |
| Wang Xing's "To A"; "information, not intelligence" | Yes (§13, §3.1) | No dates or context (item 49) |
| 小团 2.0 capabilities; engagement-only metrics | Yes (§5.1) | Correct |
| 智能掌柜 counts; drone totals; LongCat API pricing | Yes | Latest counts used; LongCat fix in item 25; extras in items 50 and 57 |
| CLC profit swing 2024→2025→Q2 2026; HSBC war-loss estimates | Yes (§2.2) | Labelling in item 30; context in item 48 |
| SAMR actions | Yes (§13, "据报道") | Extras in item 48 |
| Toast IQ Grow $499; Square Managerbot free; NRA adoption; restaurant margins | Yes (§7) | Margin caveat in item 20; extras in item 52 |
| Nugget metrics | Yes (§10) | Extras in item 53 |
| Rufus and Sparky benchmarks; Walmart in-ChatGPT ≈ ⅓; OpenAI rollback 2026-03-24 | Yes (§1, §6.1, §13) | Extras in item 45; Rufus brand in item 26 |
| Adobe, Salesforce and the 2030 forecasts | No | Items 38 and 39 |
| Inference cost by model tier | Yes (three tiers) | Frontier top tier missing (item 54) |
| 千问: 1.2 亿 AI orders in 6 days on a ¥30 亿 subsidy | Yes (§6.2, §6.3) | Label as a company claim (item 16) |

### Must-add

36. **Alipay's agent-payment scale and trust stack**
    - **Location:** §6.2, line 359, after "支付宝的 ACT 协议也规定"用户始终确认支付" ([扬子晚报](...))。"
    - **Missing:**
      - ACT 2.0 (April 2026);
      - 3 亿+ AI-agent payments by 2026-05-26;
      - support for 95% of general-purpose agents;
      - an AI wallet and Token Pay;
      - APASS/KYA (September 2026).

      This is the strongest evidence for "监管把支付关系留给了持牌钱包", and the brief named it.
    - **Evidence:** agentic_commerce_channels.md §3, Alipay:
      - "**2026-05-26:** Alipay said it had processed **more than 300 million AI-agent payments**, supports 95% of general-purpose agents, and launched an AI wallet and Token Pay."
      - "**April 2026:** ACT 2.0, built with the IIFAA trusted-authentication alliance, Zhipu and 20+ other firms"
      - "**Bund Summit, Sept 2026:** the APASS commerce-trust infrastructure, built on **KYA (Know Your Agent)**".
    - **Rating:** must-add (treated as major in the counts)
    - **Suggested text:** "支付宝的 ACT 协议 01-16 上线、04 月升级到 2.0；公司称截至 05-26 已处理 3 亿+ 笔 AI Agent 支付，支持 95% 的通用 Agent，并推出 AI 钱包和 Token Pay；09 月外滩大会又发布基于 KYA（了解你的 Agent）的 APASS 商业信任基础设施 ([腾讯新闻](https://news.qq.com/rain/a/20260526A079BK00); [IT之家](https://www.ithome.com/0/973/895.htm); [新京报](https://www.bjnews.com.cn/detail/1783910091129823.html))。这些是公司口径，主要发生在阿里自己的生态里。"

37. **DoorDash MCP access terms ("approved agents")**
    - **Location:** §6.1 table, line 324: "| 企业 MCP、CLI | CLI 2026-07 公测；企业 MCP 09-30 推出，早期伙伴有 SpaceX、Cognition、Vercel、Mercor、Tempo | DoorDash（员工各自登录） | 未披露 |"
    - **Missing:**
      - Access is by waitlist and switched on per company.
      - Each employee signs in and accepts the terms.
      - Per secondary sources, only approved third-party agents can use it, and they can search, build carts, place orders and track deliveries.
      - Unofficial community MCP servers exist that log into a user's account and place real orders.
      - Xu calls it the MCP "we've been giving to third parties to use for things like corporate lunch orders".

      This matters for C5/C8 and corrects item 3.
    - **Evidence:**
      - doordash.md Q2: "Access: waitlist at developer.doordash.com/mcp, with company-level access. Each employee signs into their own DoorDash account, accepts the terms and connects agents."
      - doordash.md Q2: "Third parties report the MCP gives agents four capabilities: search, build cart, place order and track delivery".
      - agentic_commerce_channels.md §2: "Secondary sources describe the MCP as a beta that lets *approved* third-party agents order on a user's behalf… Unofficial community DoorDash MCP servers also exist; they log into the user's account and can place real orders."
    - **Rating:** must-add (treated as major in the counts)
    - **Suggested text:**
      - Table row: "| 企业 MCP、CLI | CLI 2026-07 公测；企业 MCP 09-30 推出，需排队申请、按公司开通，二手来源称只对获批的第三方 Agent 开放，能力包括搜索、建购物车、下单和追踪配送；早期伙伴有 SpaceX、Cognition、Vercel、Mercor、Tempo | DoorDash（员工各自登录并接受条款） | 未披露 |"
      - Text after the table: "另有非官方的社区版 DoorDash MCP，直接登录用户账户下真实订单 ([Glama](https://glama.ai/mcp/servers/ashah360/doordash-mcp/tree))。"

38. **Adobe and Salesforce data on conversion**
    - **Location:** §6.3, line 378, after "S2 的真实价值是新客。…"
    - **Missing:** AI-referred traffic converts better, while checkout inside the agent converts worse. This conversion gap is the empirical basis for "Agent 导流、平台结账" and for S2's "获客渠道" verdict. The brief named Adobe and Salesforce.
    - **Evidence:** agentic_commerce_channels.md §4:
      - "Generative-AI referrals to US retail sites rose **693%** year on year… AI referrals converted **31% better** than non-AI traffic"
      - "'AI and agents' influenced **$67B, or 20%** of purchases… This is an attribution of influence, not agent-completed orders."
      - Inference: "The conversion gap… is the empirical reason the US market moved to 'agent refers, platform checks out'."
      - analysis_toolkit_unit_economics.md §4: "July 2026 was the 11th straight month of AI traffic out-converting non-AI".
    - **Rating:** must-add for Adobe, nice-to-have for Salesforce (counted once, as must-add)
    - **Suggested text:** "AI 导来的流量转化更好，在 Agent 里结账转化更差。Adobe 称 2025 年美国假日季生成式 AI 导向零售网站的流量同比 +693%，转化率比非 AI 流量高 31%，到 2026-07 已连续 11 个月高于非 AI 流量；但 Adobe 没给 AI 流量的基数和占比 ([Digital Commerce 360](https://www.digitalcommerce360.com/2026/01/13/generative-ai-online-holiday-shopping-traffic-2025/amp/); [Digital Commerce 360](https://www.digitalcommerce360.com/2026/08/19/adobe-ai-referral-traffic-data-july-2026/))。Salesforce 称 2025 年网络周 AI 和 Agent"影响"了 670 亿美元、约 20% 的线上购买，这是归因口径，不是 Agent 完成的订单 ([Salesforce](https://www.salesforce.com/news/press-releases/2025/12/05/cyber-week-ai-agents-sales/))。对照 Walmart 在 ChatGPT 内结账只有三分之一的转化，这正是美国走向"Agent 导流、平台结账"的原因。"

39. **The 2030 forecasts**
    - **Location:** §13, as a new paragraph before "第一，Agent 渠道会被定价成获客成本…" (§14 item 7, line 645, already caveats these forecasts; see item 35).
    - **Missing:** the McKinsey, Morgan Stanley and Bain 2030 figures and their definitions.
    - **Evidence:** agentic_commerce_channels.md §4:
      - "McKinsey (Oct 2025): up to **$1 trillion** of US B2C retail revenue 'orchestrated' by agents by 2030, and **$3–5 trillion globally**. The global figure **covers goods only and excludes services**"
      - "Morgan Stanley (Dec 2025): agentic shoppers at **10–20% of US e-commerce by 2030**, meaning **$190B** in the base case and **$385B** in the bull case"
      - "Bain (2025-12-17): US agentic commerce of **$300–500B by 2030**, about **15–25% of US e-commerce**… 'initiated, influenced, or completed'".
    - **Rating:** must-add (treated as major in the counts)
    - **Suggested text:** "规模预测都针对零售商品。McKinsey 认为到 2030 年 Agent 可"编排"最多 1 万亿美元的美国 B2C 零售收入、全球 3–5 万亿美元（全球数字只含商品，不含服务）；Morgan Stanley 预计占美国电商的 10%–20%，即 1,900–3,850 亿美元；Bain 预计 3,000–5,000 亿美元，占 15%–25%，口径是 Agent"发起、影响或完成"的购买 ([McKinsey](https://www.mckinsey.com/industries/retail/our-insights/the-agentic-commerce-opportunity-how-ai-agents-are-ushering-in-a-new-era-for-consumers-and-merchants); [Digital Commerce 360](https://www.digitalcommerce360.com/2025/12/09/morgan-stanley-ai-agentic-shoppers-385-billion-online-sales/amp/); [Bain](https://www.bain.com/insights/2030-forecast-how-agentic-ai-will-reshape-us-retail-snap-chart/))。没有一家单列外卖和本地生活。"

40. **千问开放平台 takes no cut of transactions**
    - **Location:** §6.2 table, line 342 ("| 千问（…） | 阿里 | 淘宝闪购 + 支付宝 AI付 | 关闭：未见与美团集成 |"), and §13, line 617 ("中国已经先走一步：…").
    - **Missing:**
      - From June 2026, 千问 reportedly opened to third-party agents and skills (not verified).
      - In August 2026 the 千问开放平台 went live with more than ten categories of service providers, and it reportedly "不碰交易环节".

      This is the counter-example to the channel-fee thesis in China. "关闭" in the table is also premature.
    - **Evidence:**
      - agentic_commerce_channels.md §3: "**August 2026:** the Qwen Open Platform went live with more than ten categories of service providers joining as agents. It reportedly does not take part in the transaction ('不碰交易环节')."
      - agentic_commerce_channels.md §3: "**About 2026-06-03:** Qwen App reportedly opened to third-party Agents and Skills, with Luckin and KFC testing — … attribution uncertain, needs verification."
      - agentic_commerce_channels.md §4: "Qwen Open Platform: reportedly takes no part in the transaction."
    - **Rating:** must-add (treated as major in the counts)
    - **Suggested text:**
      - Table cell: "未见与美团集成；08 月起千问开放平台接纳第三方服务商（据报道不碰交易）"
      - §13, appended to the first point: "但中国的 AI 入口定价并不统一：据报道，08 月上线的千问开放平台接纳十余类服务商以 Agent 身份入驻，"不碰交易环节" ([投资界](https://news.pedaily.cn/202608/567583.shtml))；抖音则对豆包导流的订单加价。"

### Nice-to-have

41. **Other Instacart deal terms**
    - **Location:** §2.1, line 95, after "10-29 当天，DoorDash 还宣布以约 170 亿美元收购 Instacart，预计 2027 年底或 2028 上半年交割"
    - **Missing:**
      - price, premium, cash share and bridge financing;
      - HSR, Canada and Israel approvals; about 19% of holders committed;
      - "Open Platform, No Grocery Exclusivity";
      - a single combined membership fee, saving about $100M a year;
      - "the advertising and AI tools each company has built" as a stated rationale;
      - the CFO's list of synergy sources.
    - **Evidence:** doordash.md Q0: "Price: $65/share… a 26% premium… At least 75% of the consideration is cash. Financing is a JPMorgan 364-day bridge loan… Needs HSR clearance plus approvals in Canada and Israel… 'Open Platform, No Grocery Exclusivity'… DashPass and Instacart+ members would pay one fee… about $100M a year".
    - **Rating:** nice-to-have
    - **Suggested text:** "交易价每股 $65，较 10-22 收盘溢价 26%，至少 75% 付现，由 J.P. Morgan 的 364 天过桥贷款融资；需通过美国 HSR 及加拿大、以色列审批，约 19% 股份的持有人已承诺投赞成票。DoorDash 承诺"开放平台、不做杂货独家"，保留 Instacart 品牌和 App；交割后 DashPass 与 Instacart+ 会员只收一份费，公司估计每年为会员省约 1 亿美元。交易理由之一是给杂货商提供"两家各自建成的广告和 AI 工具"。CFO 列出的协同来源包括：把餐饮平台上架到 Instacart、CPG 广告覆盖更多消费者、交叉销售 Instacart 的自营软件和 DashMart 履约服务、骑手和购物员密度、营销和客服节省。"

42. **S1 product details and rival assistants**
    - **Location:** §5.1, line 229, after "此外还有 2025-12 测试的独立发现 App Zesty、7 月公测的开发者 CLI、09-30 的企业 MCP。"
    - **Missing:**
      - Zesty's design, and that its 2026 status is unknown;
      - Text DoorDash billed as "the fastest way to order outside the DoorDash app"; Android "coming soon";
      - Ask's 35% latency cut after a model migration; a grocery cart in under two minutes;
      - rival assistants (Uber Eats Cart Assistant, Instacart's grocer AI).
    - **Evidence:**
      - doordash.md Q1: "DoorDash launched Zesty, an AI social app for finding restaurants, in public testing in the SF Bay Area and NYC… Orders start in Zesty but check out in the DoorDash app"
      - doordash.md Q1, Gaps: "No 2026 status or metrics found for Zesty"
      - merchant_agent_market.md Q2: "Consumer-side Cart Assistant launched 2026-02-11".
    - **Rating:** nice-to-have
    - **Suggested text:** "Zesty 是 2025-12-16 在旧金山湾区和纽约公测的 AI 社交找店 App：用 DoorDash 账户登录，推荐综合 Google Maps、TikTok、Reddit、Eater 和 DoorDash 的信息，下单跳回 DoorDash App 结账；2026 年的状态和数据都没找到，不清楚是否已并入 Ask。DoorDash 称 Text DoorDash 是"在 DoorDash App 之外下单最快的方式"，安卓版"即将推出"。Ask 换模型后延迟降了 35%，Xu 称它两分钟内就能凑好一车杂货。对手也有同类产品：Uber Eats 2026-02-11 上线购物车助手，Instacart 2025 年底推出面向杂货商的 AI 工具。"

43. **More of Xu's remarks on agents**
    - **Location:** §6.1, line 333, after "10 月他又说"Agent 里也会有广告机会"…"
    - **Missing:**
      - Xu's Q2 explanation of low agent volume ("battle for attention" vs "battle for atoms");
      - on 10-29, "never fight gravity", with no answer on revenue sharing;
      - TechCrunch's note that Amazon blocks personal agents while Walmart welcomes them.
    - **Evidence:**
      - doordash.md Q2: "Xu said agentic volume from AI partners was low because most AI platforms prioritize enterprise customers and coding tools over physical fulfillment… 'battle for attention'… 'battle for atoms'"
      - doordash.md Q2: "He gave no answer on revenue-sharing."
      - doordash.md Q2: "TechCrunch reports that Amazon has been blocking personal agents while Walmart has welcomed them".
    - **Rating:** nice-to-have
    - **Suggested text:** "Xu 在 08-05 的 Q2 电话会上解释过量小的原因：多数 AI 平台优先做企业客户和编程工具，不做实物履约；他把这称为助手之间的"注意力之争"，DoorDash 押注的是"原子之争"。10-29 被问到能否和 Agent 分成时，他说"永远不要和引力对着干"，会找双赢的合作方式，但没有回答分成问题。TechCrunch 指出，Amazon 在封锁个人 Agent，Walmart 则欢迎它们。"

44. **Peer delivery platforms' sanctioned agent channels**
    - **Location:** §6.1 table (line 319 onward), or a sentence after it.
    - **Missing:**
      - Swiggy's MCP servers (OAuth sign-in; every order requires confirmation in ChatGPT);
      - Zomato's MCP server;
      - Uber Eats in ChatGPT (redirects to the app);
      - Instacart's in-ChatGPT checkout, possibly replaced by a redirect after OpenAI's pivot.
    - **Evidence:** agentic_commerce_channels.md §1: "Swiggy MCP servers for Food, Instamart (quick commerce) and Dineout… Usable from ChatGPT, Claude and Gemini. The user logs into their Swiggy account by OAuth. In ChatGPT, every write action, such as placing an order, needs explicit confirmation."
    - **Rating:** nice-to-have
    - **Suggested text:** "同行也在开许可通道：Swiggy 2026-01 推出外卖、Instamart 和 Dineout 的 MCP 服务器，可在 ChatGPT、Claude、Gemini 里用，用户经 OAuth 登录 Swiggy 账户，ChatGPT 里每个下单动作都要明确确认 ([Swiggy](https://www.swiggy.com/corporate/?p=16731); [MediaNama](https://www.medianama.com/2026/01/223-ordering-chatgpt-swiggy-services-working/))；Zomato 2025-11 前后上线 MCP 服务器（只见第三方指南）；Uber Eats 的 ChatGPT App 浏览后跳回 Uber Eats 结账（来源较弱）。Instacart 2025-12 曾在 ChatGPT 内完成结账，OpenAI 转向后据报改为跳回自有网站。"

45. **More on Rufus, Sparky and other own-platform agents**
    - **Location:** §1, line 78 (the two measurement caveats), and §13, second point.
    - **Missing:**
      - Rufus: user count, the "60% more likely" figure (selection bias), and about $40 per user per year;
      - Sparky: reach among app users, Sparky inside ChatGPT and Gemini, and ad tests in Sparky;
      - JD 京言's user count; 千问's first-time AI shoppers;
      - AI's share of referral vs total traffic (Bain, Similarweb).
    - **Evidence:**
      - analysis_toolkit_unit_economics.md §4: "more than 300M customers used Rufus in 2025… users were '60% more likely to complete a purchase'"
      - analysis_toolkit_unit_economics.md §4: "Half of Walmart app users have engaged with Sparky… Sparky has run inside ChatGPT since the week of 2026-03-25 and in Gemini from April"
      - analysis_toolkit_unit_economics.md §6: "Walmart is testing ads in Sparky"
      - analysis_toolkit_unit_economics.md §1: "AI is up to 25% of referral traffic for some retailers but under 1% of total traffic".
    - **Rating:** nice-to-have
    - **Suggested text:** "Rufus 2025 年有 3 亿+ 用户，用户下单的可能性高 60%+（用户与非用户比较，有选择偏差）；按 120 亿美元计，每用户每年约 $40 增量销售，按外卖 3%–5% 的利润率只合 $1–2 利润。一半 Walmart App 用户用过 Sparky；它 03 月底起跑在 ChatGPT 里、04 月进 Gemini，账户和购物车同步，结账回 Walmart 自己的系统，Walmart 还在测试 Sparky 里的广告。京东京言约 8,000 万用户用于辅助购物；千问到 02 月底让约 1.4 亿用户首次体验 AI 购物（补贴驱动）。Bain 称 AI 已占部分零售商导流流量的 25%，但不到总流量的 1%；Similarweb 估计生成式 AI 只占零售访问的约 0.4%。"

46. **More on China's agent channels**
    - **Location:** §6.2, around line 351.
    - **Missing:**
      - 豆包's December 2025 adjustment notice;
      - HK01's report that Meituan doesn't support bookings via 豆包;
      - Meituan's internal restrictions on 豆包 and 千问 models (internal model choice, not agent access);
      - 豆包 travel data (direct purchase vs OTA verification; OTA commissions);
      - no official Meituan MCP;
      - the Q2 call's "progressing rapidly" on the Tencent partnership.
    - **Evidence:**
      - agentic_commerce_channels.md §3: "Doubao customer service confirmed that **Meituan does not currently support restaurant reservations through Doubao AI**… (article date not captured)"
      - agentic_commerce_channels.md §3: "only 15.2% of users buy directly after an AI travel recommendation, while 66.2% verify on an OTA"
      - agentic_commerce_channels.md §3: "Meituan internally restricted staff use of the *Doubao model*… This concerns which models Meituan uses internally".
    - **Rating:** nice-to-have
    - **Suggested text:** "2025-12 豆包随后发文称 AI 操作手机是"实验室功能"，不再支持微信，限制金融、刷分和竞技游戏场景，付款步骤交还用户。据 HK01，有食客经豆包订位被拒，豆包客服确认美团目前不支持经豆包 AI 订餐厅（报道日期不详）。美团 2026-07 在内部限制员工用豆包模型、04 月起不再推荐千问模型，这是内部用模型的选择，与外部 Agent 能否接入无关。豆包的旅游数据显示，只有 15.2% 的用户看完 AI 推荐直接下单，66.2% 会去 OTA 核对；OTA 佣金通常 10%–15%。美团没有官方 MCP 或 Agent 接入计划，只有第三方的非官方封装；Q2 电话会称与腾讯的合作"进展迅速"。"

47. **Further provisions of the payment convention, and Alipay's safeguards**
    - **Location:** §4, line 217.
    - **Missing:**
      - transaction limits; KYA; security testing and human service, monitoring and rollback;
      - PBOC guidance;
      - the three stages of agent payment;
      - Alipay AI付's safeguards.
    - **Evidence:**
      - agentic_commerce_channels.md §3: "Tiered, classified management with **transaction limits**. Members must explore **KYA**… keep human service, monitoring and rollback mechanisms… agent-assisted payment → autonomous payment under preset conditions → autonomous payment under general conditions."
      - analysis_toolkit_unit_economics.md §5: "支付宝 AI付 requires manual account binding on first use and face, fingerprint or password verification at payment, with a '被盗即赔'… promise".
    - **Rating:** nice-to-have
    - **Suggested text:** "公约在人民银行指导下制定，还要求分级分类管理、设交易限额，探索 KYA（了解你的 Agent），自主支付应用要通过安全测试并保留人工服务、监测和回滚机制。分析人士把智能体支付分为三步：Agent 辅助支付、预设条件下自主支付、通用条件下自主支付，完全自主支付尚未规模化。支付宝 AI付首次使用要手动绑定，付款时验证人脸、指纹或密码，并承诺"被盗即赔"。"

48. **Context for the Meituan P&L, war and regulation**
    - **Location:** §2.2 (around lines 99–122) and §13, fourth point.
    - **Missing:**
      - Q4 2025 CLC (−¥100.5 亿, −15.5%);
      - the company's reason for the Q2 turnaround;
      - the CFO on subsidies and the regulators' subsidy guidance;
      - the Q1 one-off fine (probably the SAMR penalty);
      - conflicting share figures; the industry's peak annual profit.
    - **Evidence:** meituan.md Q1:
      - "Q4 2025 | 64.83 (−1.1%) | −10.05 | −15.5%"
      - "primarily attributable to the reduction in user incentives… coupled with the favorable seasonality"
      - "Industry subsidies remain 'well above 2024 levels' and will take 'a few quarters to normalize'"
      - "the 'one-off fine recorded in Q1 2026' in the Q2 filing is probably this penalty"
      - "UBS says Meituan's order and GTV shares held above 55% and 60%"
      - "the industry's best annual profit was just over RMB30bn".
    - **Rating:** nice-to-have
    - **Suggested text:** "2025 Q4 CLC 经营亏损 100.5 亿元（−15.5%）。2026 Q2 转正，公司解释为"主要由于用户激励减少"，加上季节性带来的每单配送成本下降。CFO 说监管已发补贴指引，但行业补贴仍"远高于 2024 年"，需要"几个季度"才能正常化。美团 Q2 财报提到 Q1 计入一笔一次性罚款，很可能就是市场监管总局 04-17 的 7.4 亿元罚款（推断）。份额口径有冲突：汇丰按日单算美团 2026 Q1 占 50.3%，UBS 称美团订单和 GTV 份额分别高于 55% 和 60%，美团自称 GTV 份额超过 60%。据汇丰，行业最好的一年利润也只略高于 300 亿元。"

49. **Dates and detail on Meituan's AI strategy**
    - **Location:** §3.1, line 146 (王兴), and §13, third point (To A).
    - **Missing:**
      - the dates of the "爱因斯坦" remark and of "To A";
      - Wang's description of the moat;
      - 2025 R&D attributed to AI;
      - LongCat-2.0's training scale and chip adaptation;
      - commentary on "To A"; the postponed 2025 AI upgrade.
    - **Evidence:**
      - meituan.md Q2: "2026-03-13 management meeting [Exec]"
      - meituan.md Q2: "on the Q1 2026 call (2026-06-01), Wang said that… serving AI agents (To A) is becoming increasingly important… Wang sees Meituan's moat as real-time, all-category data plus fulfillment infrastructure"
      - meituan.md Q2: "R&D in 2025 was RMB26.0bn (+23.5%…), 'primarily due to the increased corporate-level investments in AI'"
      - meituan.md Q9: "'To A' is 'offensive in presentation, but defensive at its core'".
    - **Rating:** nice-to-have
    - **Suggested text:** "王兴的"爱因斯坦"说法出自 2026-03-13 的管理层会议；"To A"出自 06-01 的 Q1 电话会，他把美团的护城河归为实时全品类数据加履约基础设施。2025 年美团研发 260 亿元（+23.5%），年报称"主要由于公司层面加大了 AI 投入"。LongCat-2.0 在约 5 万卡的国产集群上训练（新华网称"万卡级"），07-06 开源，适配昇腾、摩尔线程、沐曦推理。有评论说"To A"是"形式上进攻、实质上防守"；虎嗅称 2025 年原定的 AI 升级因外卖大战被推迟。"

50. **Detail on Meituan's merchant AI**
    - **Location:** §7.1, line 400.
    - **Missing:**
      - the official FY2025 figure of 3.4 million merchants;
      - 智能掌柜's growth path;
      - adoption of 袋鼠管家, AI小管家 and 数字员工;
      - efficiency claims;
      - a merchant saving on agency costs;
      - AI店长's users and results; 袋鼠参谋 as a lead-generation funnel.
    - **Evidence:**
      - meituan.md Q5: "Meituan's AI Merchant Assistant also helped more than 3.4 million merchants effectively reduce operational costs"
      - meituan.md Q5: "智能掌柜 merchant counts rose from about 70.8万 (Mar) to over 70万 (Jun 1), over 100万 (Jun 25) and over 130万 (Sep 9)"
      - meituan.md Q5: "one avoided RMB60,000+ a year in agency and photography costs"
      - merchant_agent_market.md Q2: "'tens of thousands' of SMB merchants plus 200+ chains, including 古茗 and 兰湘子".
    - **Rating:** nice-to-have
    - **Suggested text:** "美团 2025 年报称，AI 商家助手帮 340 万+ 商户降低经营成本（官方口径）。智能掌柜的服务商户数从 03 月约 70.8 万、06-25 的 100 万+ 升到 09-09 的 130 万+；袋鼠管家有 1.3 万+ 餐饮商户，AI小管家覆盖 100 万+ 小餐馆，"数字员工"覆盖 30 万+ 零售商户；品牌驾驶舱省掉约 65% 的人工报表，商家 AI 守护让恶意差评占比降了约 60%。有商户一年省下 6 万元以上的代运营和拍摄费，说明中国商户也在为"被替代的支出"算账。AI店长已有数万家中小商户和 200+ 连锁品牌（古茗、兰湘子等）在用，益禾堂新店首月产出 +25%。美团代表"不排除"袋鼠参谋日后成为商户入驻美团的入口。"

51. **Peer merchant-AI benchmarks**
    - **Location:** §7.3, after line 436.
    - **Missing:**
      - 淘宝闪购's AI经营管家;
      - Douyin's free AI tools and LIFE+ penetration;
      - Grab's AI Merchant Assistant;
      - Uber Eats' merchant AI;
      - Chinese merchants paying for 生意参谋 and 京东商智 while rejecting new platform AI suites;
      - the Foshan repricing loss.
    - **Evidence:**
      - merchant_agent_market.md Q2: "外婆家's profit rose 14% after AI site and product selection"
      - merchant_agent_market.md Q2: "AI-feature penetration among lead-gen merchants reached 72%"
      - merchant_agent_market.md Q2: "built with OpenAI and Anthropic models"
      - merchant_agent_market.md Q4: "Chinese multi-platform merchants pay yearly for 生意参谋 and 京东商智 but reject new platform AI suites as aimed at beginners."
    - **Rating:** nice-to-have
    - **Suggested text:** "别的平台也免费送：淘宝闪购 10-30 发布"AI 经营管家"和连锁餐饮智能系统，称外婆家用 AI 选址选品后利润 +14%；抖音电商推出"豆包运营"看板和免费的开店托管等工具，抖音生活服务 LIFE+ 线索商户的 AI 功能渗透率到 72%；Grab 的 AI 商户助手用 OpenAI 和 Anthropic 的模型，印尼不到 5 个月有 100 万+ 次商户交互；Uber Eats 给商户做评价摘要、菜单文案和图片美化。中国的多平台商户每年为生意参谋、京东商智付费，却把新的平台 AI 套件当"新手工具"；佛山一家炖品店因自动改价每单亏 ¥8–10。"

52. **More on the US merchant-agent market**
    - **Location:** §7.1–§7.3 (around lines 402 and 410–432).
    - **Missing:**
      - the low end of phone-AI list prices;
      - Toast's "a multiple" of software spend going to outsourced services; IQ Grow's ARR pace, margins and pilot uplift; IQ Team's overtime result;
      - Burger King's drive-offs; Technomic's consumer survey;
      - the SEC charges against Presto;
      - NRA's figures on unprofitability and on tech improving profit.
    - **Evidence:**
      - merchant_agent_market.md Q1: "Maple Voice / Pro… Voice $85 (annual)"
      - merchant_agent_market.md Q1: "restaurants spend 'a multiple of what they spend on software' on outsourced services"
      - merchant_agent_market.md Q1: "'on track to be the fastest product to $10 million ARR'"
      - merchant_agent_market.md Q1: "'drive offs because guests don't want to talk to a bot'"
      - merchant_agent_market.md Q3: "23% of consumers find AI drive-thru ordering appealing and 46% find it unappealing"
      - merchant_agent_market.md Q3: "69% of tech-adopting operators report efficiency gains, but only 28% say tech improved profitability".
    - **Rating:** nice-to-have
    - **Suggested text:** "电话 AI 的标价从每店每月 $85（Maple 年付）到 $599。Toast CEO 说餐厅花在代运营等外包服务上的钱是软件支出的"数倍"；IQ Grow 有望成为最快达到 1,000 万美元 ARR 的产品，已经正利润率，试点餐厅销售比同类高 8% 以上（公司口径）；排班 Agent 让测试用户人均加班减少 41%。得来速语音 AI 有反面信号：Burger King 只在约 50–70 家店测试，顾客"不想和机器人说话"直接开走，正在加选人工的选项；Technomic 调查里 23% 的消费者觉得 AI 得来速有吸引力，46% 觉得没有；Presto 2025-01 因夸大 AI 能力被 SEC 指控。NRA 的数据里，69% 用了技术的经营者说效率提高了，只有 28% 说利润提高了。"

53. **Nugget extras**
    - **Location:** §10 table, line 524 (Zomato Nugget row).
    - **Missing:**
      - the 80% autonomous-resolution claim at launch;
      - free use for founders tied to legacy vendors, and the 90% sign-up claim;
      - the "$11M" headline and +40% agent productivity.
    - **Evidence:**
      - merchant_agent_market.md Q1(F): "Deepinder Goyal claimed it resolves up to 80% of queries autonomously… offered free to founders still under contract with legacy providers. The company says 90% of companies that tested it signed up."
      - merchant_agent_market.md Q1(F): "human-agent productivity up 40%, and Zomato support costs down 55% (headline: '$11M')".
    - **Rating:** nice-to-have
    - **Suggested text:** "上线时 Goyal 称最多 80% 的问题能自动解决；对仍与旧供应商有合同的创业公司免费，公司称 90% 试用过的企业签了约；MongoDB 案例称为 Zomato 省下 1,100 万美元，人工客服效率 +40%（厂商口径）。"

54. **The full range of inference cost by model tier, and related benchmarks**
    - **Location:** §5.3 DoorDash table (lines 260–266) and the "接下来值得看的信号" table in §13.
    - **Missing:**
      - the frontier top tier (GPT-5.5 at about $0.97 per order, about 91% of $1.07);
      - per-session ranges by tier; DeepSeek Flash per order;
      - the Gemini 4 Argon price doubling;
      - ads as a share of peers' profit; the error-rate break-even.
    - **Evidence:** analysis_toolkit_unit_economics.md §3:
      - "about $0.007–$0.03 on small models and $0.14–$0.39 on frontier-tier models at list price"
      - "GPT-5.5: $0.065 / $0.97 / $5.00"
      - "Gemini 4 Argon | $2.00 → $4.00 from 2026-12-01"
      - "ads ≈ 97% of Instacart's adj. EBITDA… and ≥ 53% of Uber Delivery's operating income"
      - "About 3.5% wipes out the whole per-order profit."

      Re-running `working_files/inference_cost_estimate.py` confirms $0.9720 per order for GPT-5.5.
    - **Rating:** nice-to-have
    - **Suggested text:**
      - New row in the DoorDash table: "| 推理：顶级前沿模型（GPT-5.5，第三方汇总价） | $0.97 | 91% |"
      - Note under the table: "按会话算，小模型约 $0.007–0.03，前沿档约 $0.14–0.39；DeepSeek Flash 每单约 $0.05。广告约等于 Instacart 的全部 Adj. EBITDA（3.1% 对 3.2%），至少是 Uber Delivery 经营利润的 53%；一单 $40 的订单，错误率约 3.5% 就会吃掉全部利润（均为估算）。"
      - New signals-table row: "| 2026-12-01 | Gemini 4 Argon 标价翻倍 | 前沿档模型的成本冲击 |"

55. **More DoorDash S3–S5 detail**
    - **Location:** §7.1 (line 398), §8.1 (line 454) and §9 (lines 482–504).
    - **Missing:**
      - Smart Campaigns features and the Pubbelly uplift;
      - DoorDash Ads metrics and Symbiosys growth;
      - DashBuddy's user count; ADP's merchant integrations; Dashers "the backbone";
      - Replicant's call automation (vendor claim);
      - the CFO's 2026 OpEx target; the new stack's support SLAs; the old code's AI limits.
    - **Evidence:**
      - doordash.md Q3: "Smart Campaigns now supports BOGO… adjusts targeting, discounts and limits automatically in real time… 20%+ net sales growth at select locations"
      - doordash.md Q0: "Spotlight… got 2x the CTR of banners… more than 80% of consumers reached were new to the advertiser"
      - doordash.md Q4: "used by more than 200,000 Dashers"
      - doordash.md Q0: "CFO: 2026 OpEx (R&D plus G&A) should be 'roughly about 2% of GOV'".
    - **Rating:** nice-to-have
    - **Suggested text:** "Smart Campaigns 已支持买一送一，并实时自动调整定向、折扣和上限；Pubbelly Sushi 部分门店净销售增长 20%+。DoorDash Ads 的 Spotlight 首页广告点击率是横幅的 2 倍，LiveRamp 测量里 80%+ 触达的消费者是广告主的新客；Symbiosys 收购后媒体花费近翻倍，2025 年 6–12 月广告主数量翻倍、花费增至 3 倍。DashBuddy 有 20 万+ Dasher 在用；ADP 复用 100 万+ 商户已有的集成；公司说 Dasher"在很长时间内仍是物流网络的主干"。Replicant 称为 DoorDash 每天自动拨打 35 万+ 通商户电话，解决率 87%（厂商口径，未注日期）。CFO 预计 2026 年研发加管理费用约占 GOV 2%；新技术栈让各方的客服 SLA 变好，旧代码库"不便于接入 AI"。"

56. **DoorDash baseline context**
    - **Location:** §2.1 (line 95), §8.2 (line 472) and §14.
    - **Missing:**
      - grocery growth and non-restaurant ordering; DashPass's share of US grocery orders;
      - reservations growth;
      - Q4 guidance;
      - investor pushback on investment;
      - 10-Q limits on personalisation.
    - **Evidence:**
      - doordash.md Q0: "US grocery and retail GOV is growing at more than twice the rate of US restaurants"
      - doordash.md Q0: "DashPass members placed about 75% of US grocery and retail Total Orders in Q2 2026"
      - doordash.md Q0: "Q4 2026 guidance: GOV $35.4–36.4B and Adj. EBITDA $0.9–1.1B"
      - doordash.md Q7: "its worst day on record after the Nov 2025 guide… the stock down 33% YTD at 2026-06-11"
      - doordash.md Q7: "consumer health data may restrict our ability to provide personalized content".
    - **Rating:** nice-to-have
    - **Suggested text:** "美国杂货零售 GOV 的增速是餐饮的两倍以上，约 30% 的美国用户在餐饮之外下单，Q2 美国杂货零售订单约 75% 来自 DashPass 会员；Q3 订位量在既有市场环比 +340%，10%+ 来自 DoorDash 新用户。Q4 指引：GOV 354–364 亿美元，Adj. EBITDA 9–11 亿美元。市场对投入有抵触：2025-11 公布 2026 年要多花"数亿美元"后股价创上市以来最大单日跌幅，到 2026-06-11 年内跌了 33%。10-Q 还提示，GDPR、欧盟《人工智能法》和消费者健康数据规则"可能限制我们提供个性化内容"。"

57. **More on Meituan's fulfilment**
    - **Location:** §8.1 (line 456) and §8.2 (lines 460–468).
    - **Missing:**
      - the drone order trajectory and licences;
      - the profitability headline (unverified);
      - 小黄蜂's delivery count;
      - dispatch changes;
      - the size of the pension subsidy; 新职伤 coverage in 2025.
    - **Evidence:**
      - meituan.md Q6: "Over 67万 commercial drone orders by end-September 2025… CAAC operating license in early 2023; Dubai GCAA license in late 2024"
      - meituan.md Q6: "小黄蜂… over 5.5万 cumulative deliveries"
      - meituan.md Q6: "exploring dynamic compensation for '难送' orders"
      - meituan.md Q1 §G: "about RMB800/month (RMB9,600/year) for one high-frequency rider"
      - meituan.md Q1 §G: "covered 17 provinces and cities and more than 16m couriers".
    - **Rating:** nice-to-have
    - **Suggested text:** "无人机单量从 2025-09 末的 67 万+、2026-05 的 90 万+ 增至 06 月末的 100 万+；2023 年拿到民航局经营许可，2024 年拿到迪拜民航局许可；有报道标题称美团副总裁预计 2–3 年实现前端规模盈利（正文未核实）。小黄蜂机器人累计配送 5.5 万+ 单。调度方面，06 月表态在探索给"难送"订单动态补偿，不把难单派给新骑手。养老补贴的量级：第一财经按缴费基数 8,000 元、20% 费率估算，一名高频骑手每月约 800 元；2025 年新职伤覆盖 17 个省市、1,600 万+ 骑手。"

58. **The wider agentic-commerce landscape**
    - **Location:** §6.1, around line 331.
    - **Missing:**
      - Google's sequence from AP2 to UCP to Universal Cart and Gemini Spark (Spark asks for approval before spending);
      - Copilot Checkout; Perplexity's checkout with PayPal;
      - card-network agent credentials;
      - Amazon's Buy for Me alongside its blocking of outside agents;
      - why Instant Checkout failed (CNBC).
    - **Evidence:** agentic_commerce_channels.md §1:
      - "Gemini Spark… 'won't take important actions like spending money… without explicit user approval'"
      - "In March 2026, Stripe expanded its Shared Payment Tokens to support both Visa Intelligent Commerce and Mastercard Agent Pay"
      - "OpenAI struggled with four things: onboarding merchants, showing accurate product data, supporting multi-item carts, connecting loyalty memberships".
    - **Rating:** nice-to-have
    - **Suggested text:** "美国的协议和支付层在趋同：Google 从 AP2（2025-09）到 UCP（2026-01），05-19 又发布 Universal Cart 和常驻后台的 Gemini Spark，花钱等重要操作须用户明确批准；Microsoft 01-08 推出 Copilot Checkout；Perplexity 与 PayPal 合作结账；Visa Intelligent Commerce 和 Mastercard Agent Pay 提供 Agent 凭证，Stripe 的 SPT 两者都支持。Amazon 一边让自己的 Buy for Me Agent 去别人的网站下单，一边屏蔽外部 Agent。据 CNBC，OpenAI 的 Instant Checkout 卡在四处：商户接入、商品数据准确性、多件购物车和会员体系对接。"

59. **More strategic commentary**
    - **Location:** §3.1 (line 148) and §3.3.
    - **Missing:**
      - Nilay Patel's "DoorDash problem";
      - the ARK, Adyen and Berkeley CMR views;
      - more of Citrini's scenario;
      - Winters' offline acquisition example;
      - the notes' four conditions that would push delivery platforms toward commoditisation.
    - **Evidence:**
      - agentic_commerce_channels.md §5: "Agents between service providers and consumers abstract the customer relationship and turn providers into databases"
      - agentic_commerce_channels.md §5: "drivers keeping 90–95% of the fee. Shah sees this as 'likely a 2027 event'"
      - agentic_commerce_channels.md §5: "Conditions that would flip toward commoditisation: 1. … transacting rights… 2. Agent-side payment rails mature… 3. Supply-side multi-homing… 4. The legal environment…"
      - analysis_toolkit_unit_economics.md §1: "Grubhub's in-restaurant signage drove over 15% of its new users".
    - **Rating:** nice-to-have
    - **Suggested text:** "反方观点还有：Nilay Patel 的"DoorDash 问题"（Agent 夹在中间，评价、广告、会员和加购都会消失）；ARK 认为 Agent 会把平台变成可互换的管道，按价格、库存和履约速度排序；Adyen 认为谁拥有意图谁就拥有客户；Berkeley CMR 认为去中介化最后会变成再中介化。Citrini 还设想骑手用多平台面板、拿走配送费的 90%–95%，并认为这"可能是 2027 年的事"。Winters 的打法里还有一条：用线下和包装触点获客，Grubhub 的店内标识带来了 15% 以上的新用户。笔记归纳了四个会把局面推向商品化的条件：Agent 拿到跨平台比价并下单的权利，Agent 支付通道成熟，商户和骑手低成本多归属，法律允许用户授权的 GUI Agent 无需平台同意。"

60. **More consumer delegation surveys**
    - **Location:** §4, line 215.
    - **Missing:**
      - PAR's food-specific finding (62% would order through an LLM), which runs against the S2 "① 弱" verdict;
      - Checkout.com's refusal and distrust shares;
      - Accenture's 32%; Radial/Dynata's 6%;
      - Gartner's 40% cancellation forecast.
    - **Evidence:**
      - analysis_toolkit_unit_economics.md §5: "62% would order through an LLM given the right offer… (vendor survey)"
      - analysis_toolkit_unit_economics.md §5: "24% will never delegate purchases to AI; 27% trust no organization to run a shopping agent"
      - analysis_toolkit_unit_economics.md §5: "only 6% have shopped with an AI agent".
    - **Rating:** nice-to-have
    - **Suggested text:** "唯一针对外卖的数字来自厂商调查：PAR 2026-06 调查了 1,000 名美国成年人，62% 说"条件合适"会通过大模型点餐 ([QSR Magazine](https://www.qsrmagazine.com/news/par-technology-releases-industry-report-on-ai-in-restaurants/))，与 S2 ① 偏弱的判断方向相反，值得并列。Checkout.com 发现 24% 的人永远不会把购物交给 AI，27% 不信任任何机构来运营购物 Agent；Accenture 调查里 32% 愿意让 Agent 在设定范围内替自己挑选；Radial/Dynata 发现只有 6% 的人用 AI Agent 买过东西。Gartner 预计到 2027 年底超过 40% 的 Agent 项目会被取消。"

---

## E. Integrity of the earlier report (`reports/Agent 商业模式三要素案例分析.md`)

**Checks passed:**
- **Structure:** complete. 结论, §1–§10 and 来源 are present; H2 numbering runs 1–10 without gaps; H3 numbering is continuous.
- **No defects found:** no duplicated sentences or sections, no truncated paragraphs, every table has consistent columns, and every cross-reference points to an existing section.
- **The Grok Bot date correction is already in place:** §4.3's table ("TechCrunch 于 2026-11-02 报道"), §10 item 3 and 来源.
- **结论 figures verified against the earlier notes:**
  - 80 万付费周活 within two months of launch; WorkBuddy 3,235 万 MAU; 65% of the product team's code;
  - Ultra 47% cheaper per unit than Premium; entry price ¥58 → ¥99;
  - new AI products cut Tencent's Q2 operating profit by about ¥105 亿;
  - @grok limited to paid tiers from 2026-03-11; the DSA and Ofcom proceedings;
  - SI segment back in profit in Q3; Grok Bot at most 1–6% of SI revenue.
- **§7 table cells verified:**
  - @grok's 54.5% agreement with fact-checkers; Enterprise-only Auto-review;
  - Grok Bot's 7-day trial; WorkBuddy's 500 monthly credits and unlimited enterprise Q&A;
  - Team prepay versus an uncapped Enterprise until a limit is set;
  - about 32.5 万 GPUs at $12.5 亿 a month through 2029-05;
  - Salesforce's roughly 1% stake.
- **Citations:** 105 of the 106 cited URLs appear in the earlier notes. The exception is item 61.

61. **An unsupported "34 倍" claim**
    - **Location:** §3.3, line 149: "**个人用量进订阅池，团队工作单独计量。** 同一家公司，Claude Pro 实测用满的 API 等值约为月费的 34 倍 ([Wokey](https://wokey.ai/claude-usage-limits))，Claude Tag 的 1:1 DM 也从个人席位扣。"
    - **Problem:** The claim has no support in the notes. Neither the "34 倍" figure nor the Wokey URL appears anywhere in the earlier round's notes, and it is the only one of the report's 106 URLs not recorded there. It is presented as a measured fact ("实测"). The surrounding argument doesn't need it, because the billing rules are documented.
    - **Evidence:** A search of `research_notes/Agent 商业模式三要素案例分析/` finds no "wokey", "34x", "34 倍" or "API equivalent". claude_tag.md records only the documented rules:
      - "**Team:** spend limits are 'Required, before anything runs'. 'Claude won't respond in channels until' credits are bought."
      - 1:1 DMs "run on their own Claude account and bill to their own seat".
    - **Severity:** major
    - **Replacement:** "- **个人用量进订阅池，团队工作单独计量。** 同一家公司，Claude Tag 的 1:1 DM 从发送者的个人席位扣，频道里的团队工作却没有放进订阅池，而是按 API 标价计量。Grok Bot 的做法正好相反（第 4 节）。"

62. **A notes anomaly to recheck: Tencent's Q1 and Q2 operating profit are identical**
    - **Location:**
      - 结论, line 15: "包括 WorkBuddy 在内的新 AI 产品让腾讯 Q2 经营利润少了约 ¥105 亿。"
      - §5.3, line 275: "（实际为 ¥756 亿；剔除新 AI 产品后为 ¥861 亿）"
    - **Problem:** workbuddy.md records non-IFRS operating profit as "¥75.6B (+9%)" for both Q1 2026 and Q2 2026. That is plausible, but it may be a copy error in the notes, and the ¥105 亿 figure depends on the Q2 line. The report is consistent with the notes. I did not browse to check the primary source.
    - **Evidence:** workbuddy.md:
      - "**Q1 2026**… Non-IFRS operating profit ¥75.6B (+9%); excluding new AI products, ¥84.4B (+17%)"
      - "**Q2 2026**… Non-IFRS operating profit ¥75.6B (+9%); excluding new AI products, ¥86.1B (+19%)".
    - **Severity:** minor
    - **Replacement:** none if Tencent's Q2 results PDF confirms ¥756 亿 and ¥861 亿. Otherwise, replace both numbers and the ¥105 亿 difference with the figures in the PDF.

---

## F. Counts by severity

| Group | Critical | Major | Minor | Total |
|---|---|---|---|---|
| New report: fact-check, citations and required corrections (items 1–31) | 0 | 4 | 27 | 31 |
| New report: internal consistency (items 32–35) | 0 | 0 | 4 | 4 |
| **New report: all findings (items 1–35)** | **0** | **4** | **31** | **35** |
| Earlier report (items 61–62) | 0 | 1 | 1 | 2 |

| Coverage omissions (items 36–60) | Count |
|---|---|
| Must-add (counted as major) | 5 |
| Nice-to-have (counted as minor) | 20 |
| **Total** | **25** |

With the omissions mapped as shown, the new report has 0 critical, 9 major and 51 minor items, 60 in all.
