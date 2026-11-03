# Meituan (美团): AI agent initiatives and the business economics they plug into (as of 2026-11-03)

Notes for scenario testing (S1–S6) of the three business-model elements: 价值主张 / 价值创造与传递 / 价值获取. Latest reported quarter as of 2026-11-03 is **Q2 2026 (results released 2026-08-28)**; Q3 2026 results had not yet been released.

Tags used: **[Official]** = Meituan HKEX filing, official release or management on an earnings call; **[Exec]** = executive aspiration or framing; **[Media]** = press report; **[Analyst]** = broker or analyst estimate; **[Commentary]** = opinion piece. Sources marked **†** were read only through search-engine result summaries, not fetched in full, so they carry lower confidence.

---

## Q1. Baseline economics: the Core Local Commerce P&L after the 2025–26 外卖大战, and which levers an agent would move

### Takeaway
Core Local Commerce (CLC: 外卖, 闪购, 到店酒旅) went from about RMB52.4bn operating profit (about 21% margin) in 2024 to a RMB6.9bn loss in 2025. Losses peaked in Q3 2025 (−20.9% margin) and the segment returned to +7.9% margin (RMB5.7bn) in Q2 2026 as subsidies were cut. About 58% of 2025 CLC revenue came from merchant-paid commission and ads (≈RMB150.7bn). Delivery-service revenue (≈37%) carries the heavy rider cost, which is now rising with social insurance. Those two pools are what an agent layer would redistribute: ad slots and commission on the capture side, and incentive and delivery costs on the cost side.

### Cited Findings

**A. Segment P&L trajectory [Official unless noted]**

| Period | CLC revenue (RMB bn) | CLC operating profit (RMB bn) | CLC margin | Source |
|---|---|---|---|---|
| FY2024 | 250.25 | +52.42 | ≈20.9% | [HKEX FY2025 results](https://www1.hkexnews.hk/listedco/listconews/sehk/2026/0326/2026032600964.pdf) |
| Q2 2025 | 65.00 | +3.72 | 5.7% | [HKEX Q2 2026 results](https://www1.hkexnews.hk/listedco/listconews/sehk/2026/0828/2026082800435.pdf) |
| Q3 2025 | 67.4 (−2.8% YoY) | −14.1 | −20.9% | [QQ News](https://news.qq.com/rain/a/20251128A05VJL00); [DoNews](https://www.donews.com/news/detail/1/6277938.html) [Media reporting official figures] |
| Q4 2025 | 64.83 (−1.1%) | −10.05 | −15.5% (vs +19.7% Q4'24) | [HKEX FY2025](https://www1.hkexnews.hk/listedco/listconews/sehk/2026/0326/2026032600964.pdf) |
| FY2025 | 260.83 (+4.2%) | −6.90 | ≈−2.6% | [HKEX FY2025](https://www1.hkexnews.hk/listedco/listconews/sehk/2026/0326/2026032600964.pdf) |
| Q1 2026 | 64.06 (+0.1%) | −2.03 | −3.2% | [HKEX Q2 2026 (QoQ section)](https://www1.hkexnews.hk/listedco/listconews/sehk/2026/0828/2026082800435.pdf) |
| Q2 2026 | 71.53 (+10.1%) | +5.67 (+52.3% YoY) | 7.9% | [HKEX Q2 2026](https://www1.hkexnews.hk/listedco/listconews/sehk/2026/0828/2026082800435.pdf) |

- **Group, FY2025:**
  - Revenue was RMB364.9bn (+8.1%).
  - IFRS loss was RMB23.4bn, against a RMB35.8bn profit in 2024.
  - Adjusted EBITDA was −RMB13.8bn and adjusted net loss −RMB18.6bn.
  - Selling and marketing (S&M) was RMB102.9bn (+60.9%; 28.2% of revenue vs 19.0%). The filing attributes the rise to "enhancing marketing and promotional efforts… in response to the intensified industry competition".
  - R&D was RMB26.0bn (+23.5%), "primarily due to the increased corporate-level investments in AI".
  - Cash was RMB106.8bn, plus RMB60.1bn of short-term treasury investments. — [HKEX FY2025](https://www1.hkexnews.hk/listedco/listconews/sehk/2026/0326/2026032600964.pdf)
- **Group, Q4 2025:**
  - Operating loss was RMB16.1bn (−17.5% margin).
  - S&M was RMB31.7bn (34.4% of revenue) vs RMB17.3bn (19.6%) a year earlier.
  - Adjusted net loss was RMB15.1bn. — [HKEX FY2025](https://www1.hkexnews.hk/listedco/listconews/sehk/2026/0326/2026032600964.pdf)
- **Group, Q3 2025:** revenue was RMB95.5bn (+2%), adjusted EBITDA −RMB14.8bn and adjusted net loss −RMB16.0bn. Transacting users (trailing 12 months) passed 800m. The company said Q4 operating losses would continue. — [QQ News](https://news.qq.com/rain/a/20251128A05VJL00)
- **Group, Q1 2026:**
  - Revenue was RMB91.0bn (+5.6%) and the operating loss RMB6.5bn.
  - Adjusted net loss was RMB4.97bn and adjusted EBITDA −RMB3.05bn.
  - S&M was RMB23.0bn (25.2%) and cost of revenues 71.5% of revenue.
  - New Initiatives lost RMB2.1bn. A "one-off fine" was recorded in Q1 2026. — [HKEX Q2 2026 QoQ section](https://www1.hkexnews.hk/listedco/listconews/sehk/2026/0828/2026082800435.pdf); [Xinhua 2026-06-01](https://www.xinhuanet.com/tech/20260601/979f59ba54314fd58d0db1ca6e828745/c.html)
- **Group, Q2 2026:**
  - Revenue was RMB104.6bn (+14.4%) and operating profit RMB2.69bn (2.6%).
  - Profit was RMB2.16bn. Adjusted EBITDA was RMB4.10bn and adjusted net profit RMB2.52bn (+69%).
  - Cost of revenues was 66.5%, S&M 23.6% (RMB24.7bn), R&D 7.3% (RMB7.67bn, +22.5%, "primarily driven by the increased corporate-level investments in AI"), and G&A 3.1%.
  - Share-based compensation was RMB1.92bn and net operating cash inflow RMB9.7bn.
  - Cash was RMB104.7bn plus RMB63.6bn of short-term treasury investments.
  - For H1 2026, the operating loss was RMB3.78bn and the adjusted net loss RMB2.44bn. — [HKEX Q2 2026](https://www1.hkexnews.hk/listedco/listconews/sehk/2026/0828/2026082800435.pdf)
- **Why CLC turned profitable QoQ (company wording):** "primarily attributable to the reduction in user incentives… coupled with the favorable seasonality which led to lower delivery related costs per order". — [HKEX Q2 2026](https://www1.hkexnews.hk/listedco/listconews/sehk/2026/0828/2026082800435.pdf)

**B. Revenue mix and take rate**
- **CLC FY2025 revenue by type** (RMB bn, YoY):
  - Delivery services: 96.07 (−2.0%)
  - Commission: 99.23 (+7.5%)
  - Online marketing services (ads): 51.46 (+5.4%)
  - Other services and sales: 14.06 (+27.2%)

  FY2024 for comparison: 98.07 / 92.29 / 48.84 / 11.06. — [HKEX FY2025](https://www1.hkexnews.hk/listedco/listconews/sehk/2026/0326/2026032600964.pdf)
- **CLC Q4 2025 by type:** delivery 23.59 (−9.9%), commission 23.78 (−1.2%), online marketing 13.14 (+2.3%). Delivery revenue fell because "elevated incentives [are] deducted from revenues". — [HKEX FY2025](https://www1.hkexnews.hk/listedco/listconews/sehk/2026/0326/2026032600964.pdf)
- **Q3 2025** delivery services were reportedly ≈RMB23.0bn and commission ≈RMB28.0bn (group). — [Tiger/media†](https://www.itiger.com/news/1161039212)
- **Superseded classification:** the Q2 2026 filing reports revenue by type as **Delivery services / Merchant services / Product sales / Others**. The FY2025 filing used Delivery / Commission / Online marketing services / Other.
  - CLC Q2 2026 under the new types: delivery services RMB26.78bn (+13.1%, "returning to positive year-over-year growth, primarily due to our disciplined spending on incentives"), merchant services RMB39.63bn (+4.0%), product sales RMB3.59bn (+78.9%; self-operated medicine and alcohol), others RMB1.53bn. — [HKEX Q2 2026](https://www1.hkexnews.hk/listedco/listconews/sehk/2026/0828/2026082800435.pdf)
- **Take rate:** Meituan no longer discloses food-delivery GTV or take rate. I found no 2025–26 broker take-rate figure (see Gaps).
  - HSBC-cited industry data: food-delivery operating margins are no higher than about 3.5%, and the industry's best annual profit was just over RMB30bn. — [Sohu/iNews on HSBC, 2026-04-10](https://m.sohu.com/a/1007728090_613239/) [Analyst/Media]
  - Douyin's restaurant 团购 commission was previously 2.5% and has been "raised to roughly Meituan's level". — [Huxiu 2026-09-11](https://www.huxiu.com/article/4888998.html) [Media]

**C. Per-order unit economics (UE) in food delivery**
- **Pre-war benchmark:** in June 2025 Bernstein projected food-delivery profit per order of RMB1.25 (2025) and RMB1.50 (2026). — [Investing.com on Bernstein downgrade†](https://ng.investing.com/news/stock-market-news/bernstein-downgrades-meituan-to-market-perform-amid-margin-expansion-risks-1970107) [Analyst]
- **During the war:**
  - J.P. Morgan (Nov 2025), citing expert estimates: per-order loss of RMB1.8 in Sept 2025, RMB1.4 in Oct, and about RMB1.2 expected in Nov. It saw breakeven by mid-2026 and RMB0.4–0.5 profit per order in H2 2026. — [Tiger†](https://www.itiger.com/news/1173649509) [Analyst]
  - An anonymous broker analyst: Meituan UE bottomed at about −RMB2 in Q3 2025 and narrowed to about −RMB1 by late Q1 2026. Rivals were about −RMB6 against Meituan's −RMB2 a year earlier, and the gap was within RMB3 by Q1 2026. — [Huxiu/定焦One 2026-06-04](https://www.huxiu.com/article/4864316.html) [Analyst via Media]
  - UBS (2026-06-02): Q1 2026 loss of about RMB0.9 per order, Q2 near breakeven, FY2026 −RMB0.3 per order.
  - Morgan Stanley (2026-06-02): Meituan's UE advantage over Alibaba widened to about RMB3 in Q1 2026 (from RMB2 in Q4 2025). Q1 CLC loss of RMB2.03bn vs MS estimate of −RMB4.27bn and consensus of −RMB4.38bn. Q2 forecast: food delivery about +RMB313m, 闪购 about −RMB750m, in-store/hotel/travel (IHT) operating profit about RMB4.3bn at about 25% margin.
  - Source for both: [Wallstreetcn 2026-06-02](https://wallstreetcn.com/articles/3773639) [Analyst]
- **Management on UE:**
  - Food-delivery losses peaked in Q3 2025 (Wang Xing). — [10jqka†](https://stock.10jqka.com.cn/20260327/c675576740.shtml) [Exec]
  - Pure food-delivery UE (excluding non-food 闪购) was profitable in April–May 2026. — [Wallstreetcn](https://wallstreetcn.com/articles/3773639); [Huxiu/定焦One](https://www.huxiu.com/article/4864316.html) [Official on call]
  - CFO, Q2 2026 call: on-demand delivery UE "turned positive", driven by seasonality and "meaningful subsidy reduction".
  - CFO, Q3 2026 guidance: UE improves YoY but weakens QoQ while staying positive. Delivery cost per order will be higher than in Q2 because of courier subsidies and insurance. IHT operating margin will fall in Q3–Q4 as investment rises. — [Investing.com transcript of Q2 2026 call](https://www.investing.com/news/transcripts/earnings-call-transcript-meituan-posts-q2-2026-profit-as-revenue-growth-accelerates-93CH-4881117)

**D. The 外卖大战: scale, losses, current state**
- **Alibaba subsidy pledge:** on 2025-07-02, 淘宝闪购 announced RMB50bn in subsidies over 12 months. — [Lanjinger†](https://www.lanjinger.com/d/1752458757169164248); [Bjnews†](https://m.bjnews.com.cn/detail/1752054135168269.html) [Media]
  - The HSBC-based article calls the original plan "RMB50bn over three years" and says actual spend passed RMB100bn. — [Sohu/iNews](https://m.sohu.com/a/1007728090_613239/) (conflict noted)
- **Peak volumes:**
  - Market: pre-war about 80–90m orders/day; combined peak over 200m/day in Q3 2025.
  - Meituan peak about 150m/day; 淘宝闪购 single-day peak about 120m (Aug 2025). — [Huxiu/定焦One](https://www.huxiu.com/article/4864316.html) [Media]
- **Subsidy spend (定焦One estimate):** for the last three quarters of 2025, Meituan about RMB40bn, Alibaba about RMB70bn and JD about RMB35bn. The whole war cost at least RMB150bn, and Q1 2026 subsidies were still over RMB10bn. — [Huxiu/定焦One](https://www.huxiu.com/article/4864316.html) [Media/Analyst]
- **HSBC (early April 2026):**
  - Losses from Q2 2025 to Q1 2026: 淘宝闪购 about RMB87bn, Meituan about RMB44bn, JD about RMB42bn (about RMB173bn in total).
  - Daily-order shares, Q4 2025: Meituan 50.9% (85m), Alibaba 37.7% (63m), JD 11.4% (19m). Q1 2026: 50.3% / 39.4% / 10.3%.
  - Expected Q4 2026 shares: about 50/40/10. Pre-war, Meituan and Ele.me were about 70/30. — [Sohu/iNews 2026-04-10](https://m.sohu.com/a/1007728090_613239/) [Analyst]
- **Conflicting order levels:** a Meituan insider puts Q1 2026 daily food-delivery orders at about 65m (Meituan), 50m (淘宝闪购) and 9m (JD), with ratios unchanged from Q4 2025. Meituan's stated aim is to hold the lead while cutting subsidies, accepting rivals at no more than 80% of its daily orders. — [Huxiu/定焦One](https://www.huxiu.com/article/4864316.html) [Media]. These order levels conflict with HSBC's 85m for Meituan in Q4 2025.
- **Share claims:** UBS says Meituan's order and GTV shares held above 55% and 60%. — [Wallstreetcn](https://wallstreetcn.com/articles/3773639) [Analyst]. Meituan says it holds more than 60% GTV share. — [NBD 2026-03-26†](https://www.nbd.com.cn/articles/2026-03-26/4310170.html) [Official via Media]
- **Regulation:**
  - 2026-01-09: SAMR opened a special investigation and evaluation of competition in food-delivery platform services.
  - 2026-04-17: SAMR fined 北京三快科技 (Meituan) RMB740m in the "幽灵外卖" (ghost-kitchen) case series. — [IT之家†](https://www.ithome.com/0/940/649.htm) [Media]
  - Late March 2026: SAMR forwarded an Economic Daily piece titled "外卖大战该结束了". — [Huxiu/定焦One](https://www.huxiu.com/article/4864316.html)
  - Inference: the "one-off fine recorded in Q1 2026" in the Q2 filing is probably this penalty.
- **Current state (Aug 2026):**
  - Official: "行业竞争趋于理性". — [Meituan release 2026-08-28](https://www.meituan.com/news/NN260828216013035)
  - CFO: regulators issued subsidy guidance. Industry subsidies remain "well above 2024 levels" and will take "a few quarters to normalize". Rivals' heavy subsidies in a dedicated app drew price-sensitive, lower-tier users with weaker repurchase.
  - Wang Xing: "We don't think irrational competition is sustainable." — [Investing.com transcript](https://www.investing.com/news/transcripts/earnings-call-transcript-meituan-posts-q2-2026-profit-as-revenue-growth-accelerates-93CH-4881117)
  - On the Q1 call, management said H2 2026 order volume might decline YoY while GMV holds up better. — [Huxiu/亿邦](https://www.huxiu.com/article/4863738.html) [Official via Media]

**E. Membership**
- On 2025-03-31, 神会员 was folded into a unified **美团会员**:
  - Six tiers: 普通 / 白银 / 黄金 / 铂金 / 黑金 / 黑钻.
  - Benefits span 吃住行游娱购医.
  - 黑钻 members can buy a 神券 annual card for RMB19.9. — [Toutiao†](https://www.toutiao.com/zixun/7488885754207225866) [Media]
- **FY2025 filing:** member privileges "significantly improved user engagement and transaction frequency, driving a notable upward shift in membership tiers". Membership is described as driving "cross-selling across various businesses". — [HKEX FY2025](https://www1.hkexnews.hk/listedco/listconews/sehk/2026/0326/2026032600964.pdf) [Official]
- **Q2 2026:** the CFO said Meituan is "enhancing membership benefits" for premium users. — [Investing.com](https://www.investing.com/news/transcripts/earnings-call-transcript-meituan-posts-q2-2026-profit-as-revenue-growth-accelerates-93CH-4881117). Morgan Stanley's Q2 CLC forecast was stated as "including membership investment". — [Wallstreetcn](https://wallstreetcn.com/articles/3773639)

**F. Keeta and New Initiatives**
- **Segment losses:** New Initiatives lost RMB10.1bn in 2025, attributed to overseas investment. In Q2 2026 the segment lost RMB1.7bn (−5.3% margin) on revenue of RMB33.1bn (+25%). — [HKEX FY2025](https://www1.hkexnews.hk/listedco/listconews/sehk/2026/0326/2026032600964.pdf); [HKEX Q2 2026](https://www1.hkexnews.hk/listedco/listconews/sehk/2026/0828/2026082800435.pdf)
- **Keeta markets:**
  - Hong Kong: positive UE in Q4 2025 (Wang: October 2025, about 29 months after the May 2023 launch).
  - Saudi Arabia: profitable in July 2026, 22 months after entry.
  - H2 2025 launches: Qatar, Kuwait, UAE and Brazil, with Brazil focused on São Paulo.
  - Guidance: the 2026 New Initiatives loss will not exceed 2025's. — [HKEX FY2025](https://www1.hkexnews.hk/listedco/listconews/sehk/2026/0326/2026032600964.pdf); [Investing.com](https://www.investing.com/news/transcripts/earnings-call-transcript-meituan-posts-q2-2026-profit-as-revenue-growth-accelerates-93CH-4881117)

**G. Rider social-insurance costs**
- **FY2025:** the pension-insurance subsidy reached nationwide coverage ("first… covering all types of couriers"). Occupational-injury insurance (新职伤) covered 17 provinces and cities and more than 16m couriers. — [HKEX FY2025](https://www1.hkexnews.hk/listedco/listconews/sehk/2026/0326/2026032600964.pdf) [Official]
- **Pension subsidy rule (nationwide from Nov 2025):** Meituan pays 50% of the contribution at the local base for riders who hit the income threshold in 3 of the last 6 months. Yicai's illustration: about RMB800/month (RMB9,600/year) for one high-frequency rider, at an RMB8,000 base and 20% rate. — [Yicai†](https://www.yicai.com/news/102884302.html) [Media calc]
- **2026-07-01:** 新职伤 went nationwide, with the platform paying the full premium ("每单必保、每人必保"). — [Meituan release](https://www.meituan.com/news/NN260828216013035). The CFO said this "adds a cost layer" and raises Q3 delivery cost per order. — [Investing.com](https://www.investing.com/news/transcripts/earnings-call-transcript-meituan-posts-q2-2026-profit-as-revenue-growth-accelerates-93CH-4881117)

**H. 到店 competition (Douyin, 高德扫街榜)**
- **高德扫街榜:** Alibaba launched it on Amap on 2025-09-10 to challenge 大众点评. — [Yicai Global](https://www2.yicaiglobal.com/news/chinas-meituan-launches-voice-command-ai-food-app-to-fend-off-jdcom-alibaba)
- **Douyin vs Meituan 到店 (Huxiu, 2026-09-11):**
  - Douyin's 到店 pre-redemption GTV: about RMB800bn in 2025, over RMB600bn in H1 2026, about RMB1.2tn expected for 2026.
  - Nomura estimates Meituan's 2025 到店 GTV at about RMB1.18tn.
  - Post-redemption, the author estimates Douyin at about RMB600bn (≈60% redemption) and Meituan at over RMB900bn (an assumed 80%). The gap is "within ~30%".
  - Nomura: Meituan's 到店酒旅 margin was over 40% before 2024, is about 35% long-term, and is forecast at about 25% for Q2 2026.
  - Douyin: breakeven expected in Q3 2026; it cut subsidies in 2026 and raised commission. — [Huxiu 2026-09-11](https://www.huxiu.com/article/4888998.html) [Media/Analyst]
- **Meituan's counter-positioning:** in Q2 2026 it launched the latest 必吃榜, built on "1.5 billion authentic user reviews". It says "authenticity, trust, and high-quality user-generated content will remain foundational". — [HKEX Q2 2026](https://www1.hkexnews.hk/listedco/listconews/sehk/2026/0828/2026082800435.pdf) [Official]

### Inferences
- **Mix shares (computed from the filings):**
  - FY2025 CLC: commission 38.0%, ads 19.7%, delivery 36.8%, other 5.4%.
  - FY2024: commission 36.9%, ads 19.5%, delivery 39.2%.
  - The merchant-paid pools (commission plus ads, about RMB150.7bn in 2025) are what S1 and S2 agents would re-price.
- **Hidden split:** the 2026 "Merchant services" line (Q2 2026 CLC RMB39.6bn) appears to combine commission and online marketing, so commission and ads can no longer be tracked separately. This classification change supersedes the old disclosure.
- **The cost of the war is visible in S&M:** RMB102.9bn in 2025 vs RMB64.0bn in 2024 (+RMB38.9bn). More incentives were netted against delivery revenue (−2.0% despite higher volume). Agent-driven personalization of incentives is a cost lever. External agents with price comparison are a counter-risk that would make subsidy competition more transparent.
- **Levers an agent would move:**
  1. Ad inventory: conversational answers show 2–3 options against a dozen-plus on a list page.
  2. Commission: an upstream agent could demand a revenue share.
  3. Incentive efficiency.
  4. Delivery cost per order (dispatch AI, drones), with insurance costs pushing it up from H2 2026.
  5. Merchant retention via AI tools.
  6. Membership as an agent-native benefit.
- **到店 economics:** IHT margins (about 25% by MS, UBS and Nomura estimates) are far above food delivery (about RMB0–1.5 per order). 到店 decision agents (小团, 必吃榜 data) therefore protect the highest-margin profit pool, the one Douyin and 高德 attack.

### Gaps
- No disclosed take rate, GTV, AOV or order count for food delivery in 2025–26. The order counts above come from insiders or HSBC and conflict (65m vs 85m/day).
- No official per-order UE figure. All RMB-per-order numbers are broker or expert estimates.
- Q3 2025 revenue-by-type figures were not fetched from the primary filing; the values above are from media.
- Total rider insurance and pension cost for 2026 is not disclosed. The ~RMB2.3bn cumulative 新职伤 premium figure seen in Dec 2025 media could not be verified against a primary source.
- Membership counts, fees and economics are not disclosed.

---

## Q2. AI strategy: 王兴's framing, the LongCat model family, compute/capex, organization

### Takeaway
Meituan frames AI in three layers (AI at work, AI in products, Building LLM), first set out in March 2025 and reaffirmed through August 2026. It explicitly refuses to be a "token factory": models exist to support the core business. In 2026 Wang Xing escalated the rhetoric: AI agents shook him more than ChatGPT, the binding constraint is information rather than intelligence, and serving AI agents ("To A") is becoming a third customer type. LongCat went from a 560B MoE model (Sept 2025) to a 1.6T-parameter LongCat-2.0 trained on domestic chips (June–July 2026). R&D is rising about 22% YoY on AI; capex is not broken out.

### Cited Findings
- **Three-layer framework [Exec]:**
  - Set out on the March 2025 call as AI at work / AI in products / Building LLM, with AI framed as "进攻而不是防守". — [The Paper†](https://www.thepaper.cn/newsDetail_forward_30457620); [IT之家†](https://www.ithome.com/0/840/222.htm)
  - Restated on the Q2 2026 call (2026-08-28), with Wang adding "we will not compete to be a token factory". — [Investing.com](https://www.investing.com/news/transcripts/earnings-call-transcript-meituan-posts-q2-2026-profit-as-revenue-growth-accelerates-93CH-4881117)
  - Chinese wording: "我们不会成为Token工厂，我们的模型和AI产品将用于支持我们的主业，提升用户、商户体验和公司经营效率". Also "AI对美团是一次组织、产品和工作流程的全面升级". — [Jiemian 8/28](https://m.jiemian.com/article/15022608.html)
- **Domestic compute [Exec]:** "全国产化训练和推理能力，也帮助我们在长期成本以及基础设施可控性上建立了更强优势" (Wang, 2026-08-28). — [Jiemian](https://m.jiemian.com/article/15022608.html)
- **2026-03-13 management meeting [Exec]:** Wang said:
  - "AI Agent对我的冲击比ChatGPT冲击更大".
  - "就算爱因斯坦当秘书，让他订一个餐厅，他依然不知道那个餐厅有没有座位。这不是智力问题，而是信息问题".
  - The digitization of the physical world is an important foundation for AI. — [凤凰科技 2026-03-13](https://tech.ifeng.com/c/8rShVTkbSYO)
  - At the same meeting, 王莆中 (CEO, Core Local Commerce) said "帮商家去了解和改造物理世界，是我们非常清晰的战略". — [新华网 2026-03-27](https://www.news.cn/tech/20260327/ff315e3e1e4d4e97a20f87db6f04e342/c.html)
- **"To A" [Exec]:** on the Q1 2026 call (2026-06-01), Wang said that in future, besides serving consumers (To C) and merchants (To B), serving AI agents (To A) is becoming increasingly important. — [KuCoin news flash†](https://www.kucoin.com/news/flash/wang-xing-proposes-to-a-era-as-tech-giants-align-around-ai-agents); [Huxiu/亿邦](https://www.huxiu.com/article/4863738.html). The latter adds that Wang sees Meituan's moat as real-time, all-category data plus fulfillment infrastructure, with AI as the lever on that advantage.
- **FY2025 strategy statement [Official]:**
  - "firmly implement our 'Retail + Technology' corporate strategy by proactively embracing AI".
  - "develop a versatile AI assistant for local services by integrating… LongCat… and open-source models, while harnessing our unique digital assets, including extensive merchant data, real-life consumption behavior and authentic user reviews". — [HKEX FY2025](https://www1.hkexnews.hk/listedco/listconews/sehk/2026/0326/2026032600964.pdf)
- **LongCat timeline:**
  - 2025-09-01 — LongCat-Flash-Chat, open-sourced: 560B-parameter MoE, 18.6–31.3B active (about 27B on average).
  - 2025-09-22 — LongCat-Flash-Thinking.
  - 2025-11-03 — LongCat-Flash-Omni (560B, 27B active, real-time audio and video).
  - 2026-01-16 — LongCat-Flash-Thinking-2601 (tool calling). — [IT之家†](https://www.ithome.com/0/879/486.htm); [IT之家†](https://www.ithome.com/0/894/423.htm); [科技日报†](https://www.stdaily.com/web/gdxw/2025-11/05/content_427085.html); [IT之家†](https://www.ithome.com/0/913/798.htm) [Media]
- **LongCat-2.0:**
  - The Preview opened for testing in April 2026. — [Xinhua 2026-06-01](https://www.xinhuanet.com/tech/20260601/979f59ba54314fd58d0db1ca6e828745/c.html)
  - The formal release came on 2026-06-30 and the open-source release on 2026-07-06: 1.6T-parameter MoE, about 48B (480亿) active per token, native 1M context. Training was on a cluster of more than 50,000 domestic accelerator cards ("五万卡"), with over 30T tokens of pretraining data. Huawei Ascend, Moore Threads and MetaX adapted inference. Weights ship in BF16, FP8 and INT8. — [10jqka 6/30](https://stock.10jqka.com.cn/20260630/c677828268.shtml); [10jqka 7/6](https://stock.10jqka.com.cn/20260706/c677966761.shtml); [科技日报](https://www.stdaily.com/web/gdxw/2026-07/06/content_542862.html) [Media reporting official claims]
  - Discrepancies: Xinhua (2026-09-09) describes the cluster as "万卡级" rather than 50,000 cards. A 36Kr English piece misstates the active parameters as "480 billion".
  - Wang (Q2 call): it is "one of the first, if not the first, trillion-parameter model trained entirely on Chinese infrastructure". It is open-weight and used internally for software development, operations, customer service and agents. — [Investing.com](https://www.investing.com/news/transcripts/earnings-call-transcript-meituan-posts-q2-2026-profit-as-revenue-growth-accelerates-93CH-4881117)
- **AI spend:**
  - R&D in 2025 was RMB26.0bn (+23.5%, 7.1% of revenue), "primarily due to the increased corporate-level investments in AI". — [HKEX FY2025](https://www1.hkexnews.hk/listedco/listconews/sehk/2026/0326/2026032600964.pdf)
  - Q1 2026: RMB7.04bn (7.7%). Q2 2026: RMB7.67bn (+22.5%, 7.3%). — [HKEX Q2 2026](https://www1.hkexnews.hk/listedco/listconews/sehk/2026/0828/2026082800435.pdf)
  - In October 2025, 王莆中 said AI investment had reached "百亿人民币级" and that "Token确实很贵". — [36Kr 2025-10](https://www.36kr.com/p/3512515121077381) [Exec]
  - Wang (Q2 2026): AI investment is assessed on an ROI basis, and Meituan is "being very deliberative". — [Investing.com](https://www.investing.com/news/transcripts/earnings-call-transcript-meituan-posts-q2-2026-profit-as-revenue-growth-accelerates-93CH-4881117)
- **Capex proxy [Official]:**
  - PP&E rose from RMB38.7bn (2025-12-31) to RMB43.3bn (2026-06-30), with H1 2026 depreciation of RMB5.7bn.
  - Q2 2026 investing outflow of RMB3.2bn was "mainly attributable to net cash outflows from capital expenditures and certain other investments". — [HKEX Q2 2026](https://www1.hkexnews.hk/listedco/listconews/sehk/2026/0828/2026082800435.pdf)
- **AI-adjacent equity stakes [Official]:** 3.86% of Z.AI (Zhipu) and 7.61% of Unitree. The fair-value gain on these, chiefly Z.AI, was RMB22.2bn in Q2 2026, booked in OCI. — [HKEX Q2 2026](https://www1.hkexnews.hk/listedco/listconews/sehk/2026/0828/2026082800435.pdf)
- **Organization [Official/Media]:** CatPaw covers 90,000 employees and more than 30,000 internally built agents (July 2026). — [Wallstreetcn](https://wallstreetcn.com/articles/3778095)
- **Commentary:** Huxiu argued that the planned 2025 AI upgrade was postponed as resources were redirected to the delivery war, while base-technology investment was kept. — [Huxiu†](https://www.huxiu.com/article/4825940.html)

### Inferences
- The "not a token factory" plus "To A" framing means Meituan's AI value capture is meant to come through the existing take rate (commission, ads, delivery fees, membership) and lower opex, not through model or API revenue. S6 is therefore strategically subordinate to S1–S5.
- The "information, not intelligence" thesis (the Einstein quote) is Meituan's core argument for S2 bargaining power. Real-time seat, stock and rider availability plus 1.3–1.5bn reviews are what a general agent cannot replicate. This is the 价值创造 asset Meituan would license or withhold.
- Running on domestic chips is partly an export-control hedge, but Wang frames it as a long-run cost advantage relevant to inference-heavy agent products (S1).

### Gaps
- No separate AI capex figure, GPU or cluster cost, or inference cost per query.
- No figure on AI headcount or the organization chart of AI teams in 2026; the LongCat team lead was not researched.
- I did not find a primary transcript of the March 2025 call that set out the three layers (search summaries only).

---

## Q3. S1: consumer ordering and decision agents inside Meituan's own apps (问小袋 → 小美 → 小团/问小团)

### Takeaway
Meituan moved from a 2024 test (问小袋 in 外卖) to a standalone agent app (小美, Sept 2025) and then to the in-app assistant **小团** inside the main Meituan app. 小团's AI search was rolled out to all users by March 2026 and upgraded to an action-taking agent (2.0) on 2026-07-27. Disclosed metrics are engagement and exposure counts (over 39亿 exposures over May Day 2026; over 7亿 cumulative responses by Sept 2026), not orders or GTV. No monetization model (ads in answers, fees) has been disclosed.

### Cited Findings
- **问小袋 (2024):** a C-end AI assistant tested in small scope inside 美团外卖 around April 2024 (dish recommendations, meal suggestions). — [Jiemian†](https://www.jiemian.com/article/11059767.html); [人人都是产品经理†](https://www.woshipm.com/it/6035131.html) [Media]. Its current status is unclear and it was likely superseded.
- **小美 standalone app:**
  - Launched 2025-09-12 as an invite-code public beta, a voice- and text-command local-life agent built on LongCat-Flash-Chat. — [Yicai Global](https://www2.yicaiglobal.com/news/chinas-meituan-launches-voice-command-ai-food-app-to-fend-off-jdcom-alibaba)
  - Features: ordering takeout, restaurant booking and queueing, and recurring errands (e.g. scheduled breakfast or coffee). It calls the main app's 外卖, travel and hotel services. — [AIHub†](https://www.aihub.cn/news/meituan-launches-ai-life-assistant-xiaomei/); [网易实测†](https://www.163.com/dy/article/KA2KQFVP0512DDVL.html) [Media]
  - Early reviews found hallucinations (a "confirmed" restaurant that could not be booked) and no rail or flight booking. — [BiaNews†](https://www.bianews.com/detail?id=220863) [Media]
  - The FY2025 filing says Meituan "first launched Xiaomei… as a standalone App". — [HKEX FY2025](https://www1.hkexnews.hk/listedco/listconews/sehk/2026/0326/2026032600964.pdf)
  - No user, DAU or order metrics have been disclosed.
- **小团 (in Meituan app):**
  - Reported as integrated into the Meituan app, focused on search and recommendations, in September 2025. — [网易†](https://m.163.com/dy/article/K9RDR5OL051180F7.html)
  - "问小团" AI search was launched over the 2026 Spring Festival. — [凤凰科技](https://tech.ifeng.com/c/8rShVTkbSYO)
  - FY2025 filing: "we have rolled out Xiaotuan… to all users… Xiaotuan will fundamentally transform consumer interaction with mobile Apps, evolving from searching to making request." — [HKEX FY2025](https://www1.hkexnews.hk/listedco/listconews/sehk/2026/0326/2026032600964.pdf) [Official/aspiration]
- **小团 adoption metrics:**
  - May Day 2026: "服务过亿人次". — [Xinhua 2026-06-01](https://www.xinhuanet.com/tech/20260601/979f59ba54314fd58d0db1ca6e828745/c.html)
  - May Day 2026: services "累计曝光超39亿次". — [Meituan release](https://www.meituan.com/news/NN260828216013035)
  - Q2 2026: "consumers increasingly turned to Xiaotuan… As adoption grew, we observed a meaningful increase in user engagement on Meituan." — [HKEX Q2 2026](https://www1.hkexnews.hk/listedco/listconews/sehk/2026/0828/2026082800435.pdf) [Official]
  - The Investing.com transcript renders 小团 as "Xiaochuan" (a transcription error).
  - By 2026-09-09: 12 categories of local-life needs and over 7亿 cumulative responses. — [新华网 2026-09-09](https://www.news.cn/tech/20260909/79161147f2414bc79664a28d32802f8e/c.html)
  - Data assets: over 7亿 merchant-information verifications, over 13亿 reviews, 2,800+ cities. — [Jiemian/商业范儿](https://m.jiemian.com/article/14842082.html); [Bjnews†](https://www.bjnews.com.cn/detail/1785481550129373.html)
- **小团 2.0 (2026-07-27):** adds "行动代理":
  - After user authorization and confirmation, it books restaurants, places takeout orders and hails rides.
  - It uses AI outbound calls (AI外呼) to book merchants that have no online reservation.
  - Meituan's internal five-level capability model puts earlier 小团 at levels 1–2 (find answers, compare). The 2.0 focus is action.
  - Positioning: "不提供情绪价值，但具备任务执行能力". — [Bjnews†](https://www.bjnews.com.cn/detail/1785481550129373.html); [10jqka](https://stock.10jqka.com.cn/20260727/c678455867.shtml) [Media]
  - Official Q2 framing: 小团 moved from "帮用户找答案" to "帮用户把事办成". — [Meituan release](https://www.meituan.com/news/NN260828216013035)
- **Surrounding transaction rails [Official]:** Pick-up Now, online reservations, advance online ordering, smart queuing and in-store smart ordering. These give 小团 bookable actions. — [HKEX Q2 2026](https://www1.hkexnews.hk/listedco/listconews/sehk/2026/0828/2026082800435.pdf)
- **Monetization commentary [Commentary]:**
  - A conversational UI shows 2–3 candidates against a dozen-plus on a list, so sellable ad exposures fall.
  - Cards could carry "sponsored" labels and be charged per click or transaction, but ad inventory and pricing must be recalculated.
  - Paid placement risks eroding trust in the AI. — [36Kr EU/商业范儿 2026-07-29](https://eu.36kr.com/en/p/3916086864090756)

### Inferences
- **价值主张:** clear for complex, low-frequency 到店 decisions (group dining, bookings). This is the "information" advantage: real-time seats, verified merchant data and reviews. Management ties 小团 to engagement, not conversion.
- **价值创造与传递:** the rails exist (booking, queuing, ride-hailing, AI calls to offline merchants). The AI-call feature extends supply to non-digitized merchants, a capability a general agent lacks.
- **价值获取:** unproven. The likely path is indirect: higher engagement and frequency feeding commission and membership, plus possible native ads in answers. Fewer ad slots per answer is a structural risk to the RMB51.5bn ads line.
- 小美's role appears to have shifted from consumer app to the agent Meituan exposes to partners (元宝, see S2). The consumer push now centers on in-app 小团. This is an inference from product chronology; no official statement on 小美's repositioning was found.

### Gaps
- No disclosed orders, GTV, conversion or retention for 小美 or 小团. "服务过亿人次" and "39亿次曝光" are not comparable units.
- No disclosure of ads or sponsored placements inside 小团 answers, or of any agent-specific fee.
- No information on inference cost per 小团 session.
- No data on AI-driven 团购 recommendation performance (CTR or GMV uplift).

---

## Q4. S2: Meituan invoked by third-party general agents (Tencent 元宝/WorkBuddy/WeChat Pay; ByteDance 豆包; Alibaba 千问; MCP/open platform)

### Takeaway
Meituan's stance is selective. It partners with Tencent, a non-competitor in local services and a long-time shareholder-partner: 小美 exposed inside 元宝 (announced 2026-06-01), first-batch WeChat AI ecosystem access (2026-06-08), and a "美团生活助手" expert in WorkBuddy paid via the WeChat Pay AI专属卡 (2026-06-17). It refuses screen-scraping GUI agents: Meituan is on 豆包手机助手's list of more than 150 apps refusing GUI automation (2026-10-14). Alibaba's 千问 routes food delivery to rival 淘宝闪购, with no Meituan integration found. No fees or revenue shares have been disclosed for any integration.

### Cited Findings
- **Tencent 元宝 × 小美:**
  - Announced by Wang on the Q1 2026 call (2026-06-01) as going live "近期". Users state a need in 元宝, and 小美 calls Meituan's 外卖 and local services. This is agent-to-agent. — [IT之家†](https://www.ithome.com/0/958/410.htm); [Huxiu/亿邦](https://www.huxiu.com/article/4863738.html) [Official on call]
  - Q2 2026 call: the Tencent partnership is "progressing rapidly". — [Investing.com](https://www.investing.com/news/transcripts/earnings-call-transcript-meituan-posts-q2-2026-profit-as-revenue-growth-accelerates-93CH-4881117)
  - Go-live not confirmed: searches up to Nov 2026 found no launch announcement. The described flow ("帮我点份附近评分高、半小时能送到的牛肉饭" → search, coupon, order, rider) is illustrative. — [Jiemian/商业范儿](https://m.jiemian.com/article/14842082.html)
- **WeChat AI ecosystem:** on 2026-06-08 WeChat opened AI-ecosystem access to developers via mini-program authorization. Meituan and JD were first-batch internal testers; Meituan said it had co-developed and tested the integration with the WeChat team. — [NBD 2026-06-08†](https://www.nbd.com.cn/articles/2026-06-08/4420386.html); [Huxiu†](https://www.huxiu.com/article/4865696.html) [Media]
- **WeChat Pay AI专属卡 + WorkBuddy + Meituan (2026-06-17), verified:**
  - **Product:** WeChat Pay launched the "AI专属卡" inside 微信零钱. It is a separate, user-funded card isolated from the main balance, and the agent can only spend its balance within user-set scope. Every payment requires on-phone confirmation.
  - **First integration:** Tencent's WorkBuddy was first. Users summon the "美团生活助手" expert from WorkBuddy's 专家 page (Mac app v5.1.1+), ask for nearby 团购 deals, pick one, and pay from the AI card. The voucher is redeemed in store.
  - **Meituan's role:** it built "exclusive capabilities" for WorkBuddy to purchase restaurant 团购 and supplies "专属优惠券".
  - **Fees:** no fee or revenue share was disclosed.
  - Sources: [量子位 2026-06-17](https://www.qbitai.com/2026/06/436160.html); [NBD 2026-06-17](https://www.nbd.com.cn/articles/2026-06-17/4429249.html); [网易†](https://www.163.com/dy/article/KVKS04RJ05198UNI.html) [Media]
  - **Verdict on the brief's claim:** "WorkBuddy and Meituan as first users" is accurate in substance: WorkBuddy was the first agent platform and Meituan the first service partner.
- **ByteDance 豆包手机助手:**
  - December 2025 (launch on the Nubia device): WeChat logins were disrupted and Alibaba apps (淘宝, 淘宝闪购, 闲鱼, 大麦) and some banks refused or flagged it. On 2025-12-05 豆包 said it would restrict some scenarios. — [NBD 2025-12-05†](https://www.nbd.com.cn/articles/2025-12-05/4169388.html); [36Kr†](https://36kr.com/p/3592275490095109) [Media]
  - **No report found of Meituan specifically blocking 豆包 in December 2025.**
  - 2026-10-14: 豆包 published its "屏幕自动化操作声明协议" (SAEP) refusal list. More than 150 apps (reported as 151) refuse GUI automation, explicitly including 美团, 微信, 支付宝, 淘宝 and 快手, plus "美团系" apps generally. Users can still install and use these apps manually.
  - Analyst 江瀚: "头部平台拒绝的不是技术本身，而是'被绕过'". — [北京日报/京报网 2026-10-15](https://news.bjd.com.cn/2026/10/15/11996628.shtml) [Media]
- **Alibaba 千问 App:**
  - On 2026-01-15 it integrated 淘宝闪购, 淘宝 and 支付宝 AI付, launching "AI点外卖" with payment completed inside the chat, among 400+ "AI办事" functions. — [IT之家†](https://www.ithome.com/0/913/546.htm); [科技日报†](https://www.stdaily.com/web/gdxw/2026-01/15/content_461939.html)
  - Spring Festival 2026 (6 days to 2026-02-12): 41亿 "千问帮我" requests and over 1.2亿 AI-completed orders, nearly half from county towns. — [扬子晚报†](https://www.yzwb.net/news/ch/202602/t20260212_321578.html) [Company data via Media]
  - No Meituan integration with 千问 was found.
- **MCP / open platform:** no official Meituan or 大众点评 MCP server or agent-access program was found. A third-party, unofficial "美团外卖 Open API MCP" wrapper exists on Vinkius (10 merchant-side tools: store info, business status, reviews, delivery tracking). — [Vinkius](https://vinkius.com/apps/meituan-waimai-open-api-mcp/with/pydantic-ai) [Not official]
- **Commentary on S2 risk:**
  - If upstream AIs pick the platform, Meituan must compete to be called and could become "an API" in business position.
  - Upstream AIs could charge diversion fees, demand revenue share, or use cross-platform price comparison to push commissions down.
  - Meituan would lose intent data (budget, taste, abandoned choices).
  - If reduced to back-end fulfillment, it "should be valued more like a logistics company". — [36Kr EU/商业范儿 2026-07-29](https://eu.36kr.com/en/p/3916086864090756) [Commentary]

### Inferences
- **价值主张 (from Meituan's side):** incremental orders from new surfaces (WeChat's audience, desk-bound WorkBuddy users) without ceding the transaction. Meituan keeps checkout (Meituan merchant, Meituan voucher, Meituan rider) and pushes its own agent (小美) or expert as the invoked unit. A bare inventory API is not what it offers.
- **价值创造与传递:** proven only in narrow form (团购 vouchers via WorkBuddy on Mac). Food delivery via 元宝 had been announced but its launch was not confirmed as of the sources found.
- **价值获取:** Meituan earns its normal merchant commission and ads on the order, minus the cost of "专属优惠券". No evidence of it paying or receiving agent fees. The Tencent alignment (WeChat Pay rails, no rival local-services business) lowers the risk of a toll.
- **Pattern:** allow structured, authenticated agent-to-agent calls with partners (Tencent); refuse unauthenticated GUI automation (豆包). Rival-owned agents (千问 routes to 淘宝闪购; 豆包 sits beside Douyin 团购) are de facto closed. The ecosystem is splitting along existing alliances: Tencent with Meituan and JD, Alibaba with 淘宝闪购, ByteDance with Douyin.

### Gaps
- Live date, order volume and any fees or revenue share for 元宝 × 小美 and for WorkBuddy × 美团生活助手.
- Whether Meituan takes any agent-referral fee or pays Tencent anything. Coupon cost is not disclosed.
- Whether Meituan exposes any MCP or official agent API beyond partner-specific builds.
- Meituan's formal response to 豆包 in December 2025, if any.

---

## Q5. S3: merchant-operations agents (袋鼠参谋 / 袋鼠管家 / 智能掌柜 / AI小管家 / 数字员工 / CatPaw for merchants)

### Takeaway
Meituan's merchant AI is broad and fast-growing: 智能掌柜 went from about 708k merchants (March 2026) to over 1.3m (September 2026), and the FY2025 filing says the AI Merchant Assistant helped over 3.4m merchants. It is free or bundled, positioned as ecosystem support during the price war and as a path to being merchants' "AI business partner". CatPaw (2026-07-27) adds a configurable agent workbench and enterprise or private deployment. No paid tier or SaaS revenue has been disclosed, so value capture is indirect: retention, ad spend efficiency, and supply quality.

### Cited Findings
- **2025-10-16 launch [Official/Media]:** at the 8th Catering Industry Conference, 王莆中 launched three tools on LongCat-Flash-Chat:
  - **袋鼠参谋:** a standalone app for site selection, district analysis, dish development and pricing. It was invite-only, with 800 codes issued at launch.
  - **袋鼠管家:** embedded in the 外卖 merchant backend for listing, promotions, member outreach and review upkeep.
  - **智能掌柜:** embedded in the 到店 backend for multi-store Q&A, daily and weekly reports, one-click bad-review replies, phone reception and marketing materials. **Free for small and mid-sized merchants.**
  - Evaluation method: compare AI against manual plans on GMV, gross margin, labor efficiency and repurchase.
  - A Meituan representative "不排除" 袋鼠参谋 later serving as an entry point for prospects to join Meituan. — [36Kr](https://www.36kr.com/p/3512515121077381)
- **FY2025 [Official]:** "Meituan's AI Merchant Assistant also helped more than 3.4 million merchants effectively reduce operational costs". In 2025, support funds assisted over 500,000 merchants. — [HKEX FY2025](https://www1.hkexnews.hk/listedco/listconews/sehk/2026/0326/2026032600964.pdf)
- **2026-03-26, 智能掌柜·品牌顾问 for chains:**
  - Over 50 chain brands used it in the pilot.
  - The single-store version had about 708k users and had resolved 660万 issues.
  - 品牌驾驶舱 has generated over 160k reports and cuts about 65% of manual reporting work.
  - 选址帮手 is used by chains such as 萨莉亚. — [新华网 2026-03-27](https://www.news.cn/tech/20260327/ff315e3e1e4d4e97a20f87db6f04e342/c.html) [Official via Media]
- **Q1 2026 (2026-06-01):** 智能掌柜 had served over 70万 merchants; "数字员工" covered over 30万 retail merchants. — [Xinhua](https://www.xinhuanet.com/tech/20260601/979f59ba54314fd58d0db1ca6e828745/c.html)
- **2026-06-25 "小店有AI" event:**
  - AI小管家: over 100万 small restaurants, over 1,000万 suggestions.
  - 袋鼠管家 (menu optimization, AI images, smart marketing, diagnostics, review and inspection hosting): trial with 3,000+ small restaurants and 100+ chains.
  - 智能掌柜: over 100万 merchants, 780万 problems solved.
  - 商家AI守护: 99% recognition accuracy, about 150万 malicious reviews or threats blocked per month, about 60% reduction in malicious-review share.
  - Merchant anecdotes: one avoided RMB60,000+ a year in agency and photography costs; another's nightly data work fell from 3–4 hours to one question.
  - Stated aim: AI that "每一个普通老板都用得上、用得起". **The article gives no pricing.** — [中新网 2026-06-26](https://www.chinanews.com.cn/cj/2026/06-26/10647834.shtml) [Official claims via Media]
- **Q2 2026 [Official]:**
  - Introduced "the first integrated AI solution tailored for quick commerce".
  - Supported 220,000 SME restaurants with "funding, operational supplies, equipment upgrades, AI tools, and store refurbishment".
  - "On the CatPaw platform, we rolled out specialized AI agents across restaurants, services retail, medical and health, and hotels and travel… our evolving role from merchants' 'online channel' to their 'AI business partner'… we aim to become a digital copilot for merchants."
  - A county-level program will reach about 400 counties and over 500,000 SMEs in H2 2026. — [HKEX Q2 2026](https://www1.hkexnews.hk/listedco/listconews/sehk/2026/0828/2026082800435.pdf)
  - The Investing.com transcript renders CatPaw as "Catapult".
- **CatPaw merchant and enterprise platform (2026-07-27):**
  - An out-of-the-box AI workbench for local merchants. After authorization and rule setup, agents aggregate sales, 团购 redemptions, reviews and inventory, flag anomalies and write reports. Built-in tools cover review analysis, product copy, posters and reports.
  - Runs on mobile, PC and cloud (24/7).
  - Price changes and promotions still need operator confirmation.
  - Enterprises can configure prompts, knowledge bases, credentials, tools and permissions. Integrates with Feishu and WeCom; supports isolation and private deployment.
  - **Pricing not stated.** — [Wallstreetcn](https://wallstreetcn.com/articles/3778095); [品玩†](https://www.pingwest.com/w/315936)
  - An "88元/月 digital employee" figure appears only on a CSDN aggregator; it is unverified.
- **2026-09-09 (服贸会):** 智能掌柜 had served over 130万 restaurant merchants and solved 860万 problems; 袋鼠管家 had over 1.3万 restaurant merchants. — [新华网 2026-09-09](https://www.news.cn/tech/20260909/79161147f2414bc79664a28d32802f8e/c.html)
- **Superseded figures:** 智能掌柜 merchant counts rose from about 70.8万 (Mar) to over 70万 (Jun 1), over 100万 (Jun 25) and over 130万 (Sep 9). Use the latest.

### Inferences
- **价值主张:** strong and quantified in time saved: reports, review replies, malicious-review blocking, images. Merchant-side tools are also a political and ecosystem lever, a way to be seen supporting SMEs after the war and SAMR scrutiny.
- **价值创造与传递:** proven at scale, embedded in existing backends with no extra install. Distribution cost is near zero because Meituan owns the merchant console.
- **价值获取:** currently indirect. Tools are free or bundled, so returns come via merchant retention, better listings and ad ROI (supporting the ads line), and supply quality. CatPaw enterprise or private deployment is the most plausible future paid SKU, but it is undisclosed. 袋鼠参谋 doubles as a lead-generation funnel for merchant acquisition.
- **Tension:** tools that optimize merchants' marketing could lower wasteful ad spend in the short term (the "marketing involution" Meituan says it is fighting), unless they also automate ad bidding.

### Gaps
- No pricing, paid conversion, ARPU or SaaS revenue for any merchant AI tool.
- No disclosed ROI study (GMV uplift vs control) despite the stated evaluation method.
- No information on automated ad-bidding agents (推广通 automation) or AI customer-service agents for merchants' end-customers.
- 美团管家 (restaurant cashier/POS SaaS) pricing and AI integration were not found in this research window.

---

## Q6. S4: fulfillment and rider-side agents, autonomous delivery (超脑, drones, robots, rider AI)

### Takeaway
Drones are the most quantified autonomy line: over 1.0m cumulative orders by end-June 2026 across six cities, with medical logistics prominent. They remain tiny next to roughly 65–85m daily food-delivery orders. Rider-facing AI is about safety and compliance (helmets, 团宝, red-light clock pauses), arriving just as social insurance raises delivery cost per order. No recent quantitative disclosure exists for the 超脑 dispatch system or ground autonomous vehicles.

### Cited Findings
- **Drone order growth:**
  - Over 67万 commercial drone orders by end-September 2025 (VP 毛一年, who said this exceeds Google Wing). CAAC operating license in early 2023; Dubai GCAA license in late 2024. — [中证网 2025-10-21](https://www.cs.com.cn/ssgs/gsxw/202510/t20251021_6518471.html) [Official via Media]
  - About 78万 orders and 70 routes by end-2025, per recruiting-page copy only. — [牛企招聘†](https://campus.niuqizp.com/schedule-7kks55Ntz.html) [Weak]
  - May 2026: the "城市低空航网" entered regular operation, open to industry partners, with over 90万 cumulative commercial orders. Regular delivery runs in Beijing, Shanghai, Shenzhen, Hong Kong and Dubai. — [Xinhua 2026-06-01](https://www.xinhuanet.com/tech/20260601/979f59ba54314fd58d0db1ca6e828745/c.html)
  - Over 100万 cumulative orders by end-June 2026 across Shenzhen, Beijing, Shanghai, Guangzhou, Hong Kong and Dubai, including over 81万 lab samples and medications. — [IT之家 2026-07-22](https://www.ithome.com/0/980/150.htm)
- **Drone delivery modes and profitability:** delivery modes are pad landing, winch descent and cabinet descent. A related headline says a Meituan VP expects "front-end scale profitability" in 2–3 years; the article body was not verified. — [IT之家](https://www.ithome.com/0/980/150.htm)
- **Ground autonomy:**
  - 小黄蜂 indoor and outdoor delivery robot: over 5.5万 cumulative deliveries (2026-09). — [新华网 2026-09-09](https://www.news.cn/tech/20260909/79161147f2414bc79664a28d32802f8e/c.html)
  - Autonomous delivery vehicles: no 2025–26 official cumulative figure found. One English trade article claims 5m orders in Shenzhen by October 2025 (single source, unverified).
- **Rider AI and safety:**
  - Smart helmet: crash detection above 95%; accident verification time cut from 382 to 2 minutes; serves over 170万 riders across Meituan and Keeta. — [新华网 2026-09-09](https://www.news.cn/tech/20260909/79161147f2414bc79664a28d32802f8e/c.html)
  - "团宝": AI safety assistant with a traffic-safety map, red-light waits and wrong-way detection. "红灯停表" (pausing the delivery clock at red lights) launched first in Beijing. — [Meituan release 2026-08-28](https://www.meituan.com/news/NN260828216013035)
- **超脑 / dispatch:** no 2025–26 quantitative disclosure found. June 2026 statements describe optimizing pricing, ETA and routing algorithms, exploring dynamic compensation for "难送" orders, and dispatch changes so new riders are not given hard orders. — [IT之家†](https://www.ithome.com/0/964/966.htm) [Media]
- **Cost context [Official]:** Q2 2026 CLC profit benefited from "lower delivery related costs per order" (seasonality). Q3 delivery cost per order is guided higher on courier subsidies and insurance. — [HKEX Q2 2026](https://www1.hkexnews.hk/listedco/listconews/sehk/2026/0828/2026082800435.pdf); [Investing.com](https://www.investing.com/news/transcripts/earnings-call-transcript-meituan-posts-q2-2026-profit-as-revenue-growth-accelerates-93CH-4881117)

### Inferences
- Rider cost is the largest variable cost line and is rising structurally (新职伤 nationwide from 2026-07-01, pension subsidy since Nov 2025). That raises the payoff from AI dispatch (orders per rider-hour) and from autonomy in dense or difficult nodes.
- Drone scale (about 1m cumulative over several years) is under 2% of one day's food-delivery volume. It is a capability and regulatory-moat play (licenses, airspace networks, medical logistics), not yet a cost lever.
- **Value capture for S4** is internal margin, not revenue. The exception is the "城市低空航网", opened to industry partners, which hints at future B2B logistics fees.

### Gaps
- No cost per drone or robot delivery vs rider delivery, and no capex for the autonomy programs.
- No 2025–26 超脑 metrics (orders per rider-hour, average delivery time, compute cost).
- No official 2026 figures for ground autonomous vehicles.

---

## Q7. S5: internal efficiency (CatPaw coding, NoCode, customer service)

### Takeaway
Internal AI adoption is high. For CatPaw, over 95% of R&D staff use it and over 50% of incremental code is AI-generated (Nov 2025, reiterated July 2026); 90,000 employees use it and over 30,000 agents have been built. Meituan discloses no cost savings, headcount effects or customer-service automation rates. Opex ratios improved in Q2 2026, mainly on lower subsidies rather than AI.

### Cited Findings
- **CatPaw history:**
  - Internal since 2023 as an editor plugin.
  - Public beta as an AI IDE on 2025-11-10, with R&D penetration above 95% and over 50% of incremental code AI-generated. — [Tiger†](https://www.itiger.com/news/2582532825); [Wallstreetcn](https://wallstreetcn.com/articles/3778095)
  - On 2026-07-27 it was upgraded to an all-scenario agent platform: 90,000 employees, over 30,000 agents. — [Wallstreetcn](https://wallstreetcn.com/articles/3778095)
  - **Superseded:** CatPaw began as a coding tool and is now Meituan's general agent platform, internal and merchant-facing.
- **Earlier code-share figure:** 36Kr cited an insider claiming about 50% of weekly new code is AI-written, with over 80% weekly active use among developers. It noted that an earlier Meituan earnings disclosure put the figure at 27% of new code (time point unclear). — [36Kr†](https://www.36kr.com/p/3300639590680584) [Media]
- **NoCode:** a "vibe coding" tool launched around May–June 2025, letting non-engineers (product and field-sales staff) build prototypes and apps. It was incubated by the CatPaw team and runs on LongCat. — [36Kr†](https://www.36kr.com/p/3300639590680584); [Yicai Global†](https://www.yicaiglobal.com/news/chinas-meituan-to-launch-ai-copilot-nocode-report-says); [量子位†](https://www.qbitai.com/2025/11/350537.html)
- **Q2 2026 call [Official]:**
  - LongCat-2.0 is used internally for software development, operations, customer service and AI agents.
  - More employees use AI tools daily, and the share of AI-generated code keeps rising.
  - G&A was 3.1% of revenue (flat YoY). — [Investing.com](https://www.investing.com/news/transcripts/earnings-call-transcript-meituan-posts-q2-2026-profit-as-revenue-growth-accelerates-93CH-4881117); [HKEX Q2 2026](https://www1.hkexnews.hk/listedco/listconews/sehk/2026/0828/2026082800435.pdf)
- **Organizational framing:** Wang said AI "一定会对组织、对工作模式带来很大的变化" and that veterans' experience may not hold. — [凤凰科技 2026-03-13](https://tech.ifeng.com/c/8rShVTkbSYO) [Exec]

### Inferences
- **S5 value capture** is opex leverage (R&D productivity, customer-service cost). With R&D at 7.3% of revenue and rising on AI, net savings are not yet visible in reported ratios. The case rests on management's qualitative claims.
- Internal adoption serves as the proving ground for S3 and S6: the same CatPaw stack is now offered to merchants and enterprises.

### Gaps
- No disclosed customer-service automation rate, cost savings or headcount impact.
- No NoCode user numbers.
- No breakdown of the ~RMB7.7bn quarterly R&D into AI compute vs people.

---

## Q8. S6: selling AI capability externally (LongCat API, CatPaw enterprise, NoCode, robotics)

### Takeaway
Meituan sells LongCat access at aggressively low prices and open-sources weights. At launch, LongCat-2.0-Preview had a promotional ¥2 input / ¥8 output per million tokens and ¥9.9/month for 50M tokens. CatPaw offers enterprise and private deployment at undisclosed prices. Management explicitly says Meituan will not be a "token factory", so external AI revenue is not a stated profit pillar and no AI revenue line is disclosed.

### Cited Findings
- **LongCat API platform (longcat.chat) pricing at the LongCat-2.0-Preview launch (2026-06-30):**
  - Promotional pay-as-you-go per million tokens: input cache hit ¥0.04, cache miss ¥2, output ¥8. Regular prices are said to be higher but were not given.
  - Monthly cards: ¥9.9 for 50M tokens; ¥399 for up to 1B tokens. Cached input does not count against packages.
  - New users get 10M free tokens after real-name verification.
  - Compatible with the OpenAI and Anthropic API formats. — [80AJ 2026-06-30 (citing Linux.do)](https://www.80aj.com/2026/06/30/meitongcat-ai-model-9-9/) [Media]
  - An aggregator lists LongCat-2.0 at $0.30 input / $1.20 output per million tokens. — [LLM Reference†](https://www.llmreference.com/provider/longcat-api-platform)
  - Earlier free quotas of 100k tokens/day (2025) were reported. — [Toolnavs†](https://toolnavs.com/article/174-longcat-api-platform-launched-developers-receive-100000-free-tokens-daily)
  - Media claim the preview ranked in OpenRouter's global top three by call volume (unverified). — [10jqka](https://stock.10jqka.com.cn/20260630/c677828268.shtml)
- **Open weights:** LongCat-2.0 is open-sourced with inference code optimized for domestic chips. — [10jqka 7/6](https://stock.10jqka.com.cn/20260706/c677966761.shtml)
- **CatPaw enterprise:** agent development, deployment and runtime management, private deployment, environment isolation, credential management and session logs. Pricing is not disclosed. — [Wallstreetcn](https://wallstreetcn.com/articles/3778095)
- **Low-altitude logistics network:** opened to "industry partners" in May 2026. — [Xinhua](https://www.xinhuanet.com/tech/20260601/979f59ba54314fd58d0db1ca6e828745/c.html)
- **Stance [Exec]:** "我们不会成为Token工厂" (2026-08-28). — [Jiemian](https://m.jiemian.com/article/15022608.html)

### Inferences
- The API pricing (promo output ¥8 per million tokens, ¥9.9 cards) looks like developer acquisition and ecosystem play. Open weights plus domestic-chip inference code also signal value to domestic chip partners. It does not look like a margin business. S6 likely fails the 价值获取 test on a standalone basis but may support S2 and S3 by standardizing how agents call Meituan.
- The most plausible paid external line is CatPaw enterprise or private deployment for merchants and chains. This overlaps with S3.

### Gaps
- No disclosed external AI revenue, API volume or paying customers.
- Regular (non-promo) LongCat-2.0 prices are not confirmed from the official pricing page.
- CatPaw and NoCode external pricing is not disclosed.

---

## Q9. Executive statements on AI monetization and analysts' views on whether agents threaten or strengthen Meituan

### Takeaway
Management never names a direct AI revenue stream. Monetization is framed as engagement (小团), merchant efficiency ("digital copilot"), internal productivity, and serving agents ("To A") on Meituan's terms, with ROI-gated investment. Analysts and commentators split. The bull case (MS, UBS buy-side targets; Huxiu-type commentary) sees fulfillment and real-time local data as an agent-proof moat. The bear and structural case (36Kr/商业范儿, 江瀚, Seeking Alpha headline) warns of disintermediation: fewer ad slots, upstream rev-share demands, and becoming "an API". I found no sell-side quantification of agent impact on Meituan's ads or commission.

### Cited Findings
- **Management statements [Exec/Official]:**
  - 2026-03-13: AI agents shook Wang more than ChatGPT; the information-not-intelligence thesis. — [凤凰科技](https://tech.ifeng.com/c/8rShVTkbSYO)
  - 2026-06-01: "To A" becoming increasingly important; Wang will "持续加大AI投入力度". — [Xinhua](https://www.xinhuanet.com/tech/20260601/979f59ba54314fd58d0db1ca6e828745/c.html); [KuCoin†](https://www.kucoin.com/news/flash/wang-xing-proposes-to-a-era-as-tech-giants-align-around-ai-agents)
  - 2026-08-28: not a token factory; ROI-based, "very deliberative" AI investment; "坚定加大生态与科技投入，推动AI融入真实业务场景"; aim to be merchants' "digital copilot". — [Investing.com](https://www.investing.com/news/transcripts/earnings-call-transcript-meituan-posts-q2-2026-profit-as-revenue-growth-accelerates-93CH-4881117); [Meituan release](https://www.meituan.com/news/NN260828216013035); [Jiemian](https://m.jiemian.com/article/15022608.html)
  - FY2025 filing: 小团 will "fundamentally transform consumer interaction with mobile Apps, evolving from searching to making request". — [HKEX FY2025](https://www1.hkexnews.hk/listedco/listconews/sehk/2026/0326/2026032600964.pdf)
- **Sell-side (mostly war and UE, not agents) [Analyst]:**
  - Morgan Stanley: Overweight, HK$120 target (18x 2027E P/E). UBS: Buy, HK$128 target (SOTP). Both dated 2026-06-02. — [Wallstreetcn](https://wallstreetcn.com/articles/3773639)
  - HSBC (April 2026): shares settle at about 50/40/10. — [Sohu](https://m.sohu.com/a/1007728090_613239/)
  - Morningstar headline: "Losses Narrow Sharply; Food Delivery Breakeven in Sight" (paywalled). — [Morningstar](https://www.morningstar.com/company-reports/1484049-meituan-earnings-losses-narrow-sharply-food-delivery-breakeven-in-sight?listing=0P0001EHWO)
  - Seeking Alpha headline: "Q2'26 Beat On Temporary Truce; Structural Headwinds Warrant Staying On Sidelines" (not read). — [Seeking Alpha](https://seekingalpha.com/article/4942365-meituan-q226-beat-on-temporary-truce-structural-headwinds-warrant-staying-on-the-sidelines)
- **Analysts on Xiaomei at launch (Sept 2025):**
  - 陈礼腾 (100EC): Xiaomei pairs AI recommendations with conversational ordering to raise stickiness, and its data feeds an algorithm loop.
  - 庄帅: AI's near-term effect on experience is "limited"; price, assortment and service will decide winners. — [Yicai Global](https://www2.yicaiglobal.com/news/chinas-meituan-launches-voice-command-ai-food-app-to-fend-off-jdcom-alibaba)
- **Disintermediation view [Commentary]:**
  - Fewer sellable exposures per transaction; upstream AIs could demand revenue share or force commission cuts via price comparison; loss of intent data; risk of becoming "an API". — [36Kr EU/商业范儿 2026-07-29](https://eu.36kr.com/en/p/3916086864090756)
  - Platforms refuse "被绕过", which would bypass their ad, recommendation and conversion paths. — [京报网 (江瀚)](https://news.bjd.com.cn/2026/10/15/11996628.shtml)
- **Moat view [Commentary]:**
  - Even if an agent holds the recommendation right, fulfillment still needs a real delivery network. 元宝 is cast as the assistant "truly willing to complement rather than compete with Meituan". — [36Kr EU†](https://eu.36kr.com/en/p/3848118357087493); [Tiger†](https://www.itiger.com/news/1182218842)
  - "To A" is "offensive in presentation, but defensive at its core". — [KuCoin news flash†](https://www.kucoin.com/news/flash/wang-xing-proposes-to-a-era-as-tech-giants-align-around-ai-agents)

### Inferences
- **For the three-element test:**
  - Management is strongest on 价值主张 (information and fulfillment moat) and 价值创造与传递 (rails, merchant console, riders).
  - It is weakest on 价值获取 for anything agent-specific. Every agent initiative so far monetizes, if at all, through the legacy take rate, membership or cost savings.
  - The open question for S1 and S2 is whether conversational surfaces keep ads plus commission at about 58% of CLC revenue, or compress the ad layer.
- The competitive context matters for how much bargaining power Meituan has with agents. Food-delivery UE only just returned to positive (Q2 2026) and Douyin is near parity in 到店 pre-redemption GTV, so Meituan cannot afford to pay agent tolls. That likely explains the partner-only, Meituan-checkout, coupon-funded approach rather than open APIs with revenue share.

### Gaps
- No sell-side report quantifying agent impact on Meituan's ad or commission revenue was found. Broker coverage focuses on UE and share.
- No management statement on pricing agent-originated orders (e.g., different commission, or ads inside 小团 or 元宝).
- The Q3 2026 results (expected late November 2026) were not available as of 2026-11-03. Q2 2026 is the latest baseline.
