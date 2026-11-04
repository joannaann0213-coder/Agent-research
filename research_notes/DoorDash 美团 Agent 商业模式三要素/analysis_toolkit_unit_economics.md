# Analysis toolkit and unit-economics benchmarks for commercializing a vertical consumer platform's AI agent (food delivery / local commerce), as of 2026-11-03

Scope: frameworks, formulas, and outside benchmarks. DoorDash and Meituan's own initiatives are deliberately excluded because other researchers cover them; they appear only where a third-party analyst uses them as the test case. "Est." marks my own computation, with assumptions shown. Every figure carries its date. Where a claim comes from a search-result synthesis or a vendor summary rather than a fetched primary document, it is labeled "(secondary)".

---

## 1. Which frameworks best test whether a marketplace agent can be commercialized, and how did consultancies and strategists apply them to agentic commerce in 2025–2026?

### Takeaway
No consultancy has published a P&L for a food-delivery agent specifically. The usable 2025–26 toolkit has three layers:
- **Consultancy sizing and taxonomies:** McKinsey's agent-to-site / agent-to-agent / brokered models, Bain's three strategic options, and BCG's "agent-ready" plus proprietary-agent prescription.
- **Strategists' disintermediation frameworks:** Thompson's aggregator levels, Casey Winters' discovery → transactional → supply-integration stages, Citrini's take-rate-compression bear case, and Nilay Patel's "DoorDash problem".
- **Classic frameworks the analyst must apply directly:** 7 Powers, JTBD, the Value Proposition Canvas, and Five Forces.

Across sources the conclusion is the same. Platforms that own fulfillment and proprietary supply data are better protected than pure listing aggregators. The most exposed layer is the profit that comes from monetizing discovery: ads, upsell, and loyalty.

### Cited Findings

#### 1a. Consultancies and banks (sizing, taxonomies, prescriptions)
- **McKinsey, "The agentic commerce opportunity" (Oct 2025):** agents could orchestrate up to $1T of US B2C retail revenue by 2030, and $3–5T globally — [Digital Commerce 360, 2025-10-20](https://www.digitalcommerce360.com/2025/10/20/mckinsey-forecast-5-trillion-agentic-commerce-sales-2030/).
- **McKinsey interaction-model taxonomy:** "Agentic commerce is taking shape through three key interaction models: agent to site, agent to agent, and brokered agent to site" (its Exhibit 2) — [McKinsey](https://www.mckinsey.com/capabilities/quantumblack/our-insights/the-agentic-commerce-opportunity-how-ai-agents-are-ushering-in-a-new-era-for-consumers-and-merchants). The page returned HTTP 503 to my fetch, so this wording comes from the search index and the full definitions were not verified.
- **Bain (Nov 2025, with Stripe), headline numbers:**
  - US agentic commerce could reach $300–500B by 2030, or 15–25% of online retail.
  - About 50% of consumers are cautious about fully autonomous purchases.
  - Consumers trust retailers' own agents about 3x more than third-party agents such as ChatGPT or Perplexity.
  - Third-party agents are the biggest disintermediation threat.
  - AI is up to 25% of referral traffic for some retailers but under 1% of total traffic.
  - Sources: [Bain press release](https://www.bain.com/about/media-center/press-releases/20252/agentic-ai-poised-to-disrupt-retail-even-with-50-of-consumers-cautious-of-fully-autonomous-purchasesbain--company/); [Crowdfund Insider summary](https://www.crowdfundinsider.com/2025/11/255589-bain-co-report-details-how-agentic-ai-will-transform-e-commerce/).
- **Bain's three strategic options:** (1) let outside agents crawl and transact, (2) build proprietary agents, or (3) partner with AI platforms. Bain advises keeping control of checkout and fulfillment where possible. It also warns that about 65% of retail-media spending is onsite and at risk if discovery shifts to AI, and expects spec-driven everyday categories to shift first — [Crowdfund Insider summary of Bain](https://www.crowdfundinsider.com/2025/11/255589-bain-co-report-details-how-agentic-ai-will-transform-e-commerce/) (secondary).
- **BCG (2026), "How Retailers Can Reinvent the Digital Flywheel for Agentic Commerce", as summarized by Mirakl (vendor blog):**
  - Survey base: 50 of the top 300 retailers and 100 brands.
  - Marketplaces account for 26% of retailers' online GMV, but only 18% of respondents consider their marketplace agent-ready.
  - Within two years, 67% plan to enrich product data and 58% plan to provide real-time availability.
  - 91% name losing the customer relationship as their top concern.
  - BCG prescribes proprietary branded, workforce, and partner agents.
  - Source: [Mirakl](https://www.mirakl.com/blogs/marketplace/is-your-marketplace-agent-ready-what-bcgs-new-research-says-about-digital-flywheel-and-agentic-commerce/) (secondary).
- **Morgan Stanley (Dec 2025, based on its AlphaWise survey):** agentic commerce of about $190B in the base case and about $385B in the bull case by 2030, or 10–20% of US e-commerce — [Digital Commerce 360, 2025-12-09](https://www.digitalcommerce360.com/2025/12/09/morgan-stanley-ai-agentic-shoppers-385-billion-online-sales/amp/); [Yahoo Finance](https://finance.yahoo.com/news/morgan-stanley-sees-multi-path-093502239.html).

#### 1b. Aggregation theory and strategists' disintermediation frameworks
- **Thompson, "Defining Aggregators" (Stratechery, 2017-09-26):**
  - Aggregators have three characteristics: (1) a direct relationship with users, (2) zero marginal cost to serve users, and (3) demand-driven multi-sided networks with customer acquisition costs that fall over time.
  - **Level 1** aggregators acquire their supply (Netflix).
  - **Level 2** aggregators do not own supply but incur supply transaction costs (Uber, and Airbnb in some jurisdictions).
  - **Level 3** aggregators have zero supply costs (Google).
  - Source: [Stratechery](https://stratechery.com/2017/defining-aggregators/).
- **Thompson, 2026-01-13:** a Stratechery piece titled "Apple and Gemini, Foundation vs. Aggregation, Universal Commerce Protocol" ties aggregation theory to agentic commerce protocols. I saw only the title, not the content — [archived listing](https://hq-raydata-co.pages.dev/vault/06-reference/2026-01-13-stratechery-apple-gemini-ucp/).
- **Thompson in a Stripe interview (secondary summary):** traditional aggregators won through superior UX. AI agents may bypass them, which would reduce their power and create openings for companies that serve agents well — [LAVX summary](https://news.lavx.hu/article/ben-thompson-on-aggregation-theory-ai-agentic-commerce-and-the-future-of-saas).
- **Casey Winters, "When Agents Attack" (2025-07-22):**
  - Agents compress the funnel in three stages:
    1. *Discovery agents:* marketplaces still bid for traffic but will pay far less, because they lose lifetime value.
    2. *Transactional agents:* "When they also execute the purchase, the marketplace disappears entirely."
    3. *Supply integration:* "Once agents intermediate demand, suppliers will plug in directly."
  - Moats that collapse: network effects, payback-period CAC math, retention and re-marketing, and direct supply relationships.
  - Moats that are rebuilt: data network effects, personalization and memory in high-frequency apps, offline and physical footprint, and tighter supply coupling.
  - Recommended playbook: go agentic on both sides; reorient CAC to first-order economics ("Stop overpaying for traffic without identity"); deepen supply integration; exploit offline and packaging surfaces; test subscriptions and usage fees before take rates erode.
  - Food-delivery example: Grubhub's in-restaurant signage drove over 15% of its new users.
  - Source: [Casey Accidental](https://www.caseyaccidental.com/p/when-agents-attack-how-ai-collapses).
- **ARK Invest:** agents could strip marketplaces of brand differentiation, reducing them to interchangeable pipes. Agents would then rank marketplaces on price, inventory, and fulfillment speed — [ARK Invest](https://www.ark-invest.com/articles/analyst-research/are-marketplaces-defensible-in-the-age-of-ai-purchasing-agents) (secondary; the page returned 403 to my fetch).
- **Berkeley California Management Review (Apr 2026):** disintermediation gives way to reintermediation, because AI agents act as filters and filters create gatekeepers — [CMR](https://cmr.berkeley.edu/2026/04/the-rise-of-ai-intermediaries-how-agentic-systems-are-rewiring-customer-relationships/) (secondary).
- **Nilay Patel, "The DoorDash Problem" (Decoder):**
  - If agents order food or book rides, services lose direct relationships and ad, promotion, and subscription revenue, and become interchangeable data sources.
  - User reviews, ads, loyalty programs, and upsells would vanish, because agents do not care about them.
  - Patel uses Amazon's lawsuit against Perplexity to illustrate the point.
  - Source: [Decoder episode page](https://metacast.app/podcast/decoder-with-nilay-patel/HFyRnpbJ/the-doordash-problem-how-ai-browsers-are-a-huge-threat-to-amazon/x03xTQ5t) (secondary).
- **Citrini Research (Feb 2026), the bear case, framed as a hypothetical 2028 "left-tail" scenario rather than a forecast:**
  - AI apps could transact directly with restaurants at about a 7% take rate, versus DoorDash's roughly 25% blended take, and could auction deliveries to drivers.
  - A "competent developer" could deploy a functional competitor in weeks.
  - "An agent doesn't have a home screen."
  - DoorDash shares fell about 7% after the note.
  - Sources: [Stocktwits](https://stocktwits.com/news-articles/markets/equity/doordash-moat-under-threat-citrini-research-warns-consumer-ai-agents-bypass-delivery-apps/cZRvn0mR4zK); [StreetInsider](https://www.streetinsider.com/Basic+Content/Dystopian+AI+report+sinks+DoorDash%2C+software+stocks/26040669.html) (secondary; the 7% and 25% figures were not checked against the original note).
- **Counter-views:**
  - DoorDash's CEO said there had been only "several hundred" orders in total from third-party agents, and framed agents as a top-of-funnel channel. Secondary coverage does not make clear which quarter's call this was — [Restaurant Business](https://www.restaurantbusinessonline.com/technology/why-doordash-isnt-afraid-ai-shopping-agents).
  - An unnamed analyst firm expects agents to *favor* the platform with the lowest delivery cost, fastest speed, and widest selection — [rallies.ai](https://rallies.ai/news/doordash-among-three-internet-stocks-down-15-ytd-least-exposed-to-ai-disruption) (secondary).

#### 1c. Classic frameworks (definitional sources)
- **Helmer, 7 Powers:** Scale Economies, Network Economies, Counter-Positioning, Switching Costs, Branding, Cornered Resource, Process Power. Each power needs both a Benefit and a Barrier — [7powers.com](https://7powers.com/). My searches found **no published application of 7 Powers to AI agents or delivery platforms**; the closest pieces were ARK, CMR, and Substack takes on marketplace defensibility ([ARK](https://www.ark-invest.com/articles/analyst-research/are-marketplaces-defensible-in-the-age-of-ai-purchasing-agents)).
- **Christensen et al., Jobs-to-be-Done** — [HBR, Sept 2016](https://hbr.org/2016/09/know-your-customers-jobs-to-be-done).
- **Osterwalder, Value Proposition Canvas** (customer jobs, pains, and gains mapped to products, pain relievers, and gain creators) — [Strategyzer](https://www.strategyzer.com/library/the-value-proposition-canvas).
- **Porter, Five Forces** — [HBR, Jan 2008](https://hbr.org/2008/01/the-five-competitive-forces-that-shape-strategy).

#### 1d. Chinese strategists and brokers
- **CICC (reported 2026-06-30), on Tencent:** if WeChat's Agent becomes a centralized entry point for distributing services, WeChat could raise its effective commission rate in the mini-program transaction ecosystem — [界面](https://m.jiemian.com/article/14842082.html) (secondary). This is the Chinese form of the "agent tax" mechanism.
- **格隆汇 commentary, "入口 ≠ 能力":** general-purpose assistants such as 元宝 own the conversational entry point but lack fulfillment capability. The agent-to-agent call into the vertical platform's agent is where the value sits — [格隆汇](https://m.gelonghui.com/p/5029245).
- **信达证券 2026 media strategy, "Agent 定义入口，AIGC 重塑供给":** AI agents are taking over user intent and may replace apps as the next super-entry point — [信达证券 PDF](https://pdf.dfcfw.com/pdf/H3_AP202601091816878674_1.pdf).
- I found no CICC or Huatai report that quantifies traffic diverted from delivery apps to agents — [search summary](https://m.jiemian.com/article/14842082.html).

#### 1e. Industry-structure facts for a Five Forces analysis (2026)
- DoorDash agreed to acquire Instacart on 2026-10-29 in a cash-and-stock deal with an equity value of about $17B — [Instacart Q3 2026 release, SEC](https://www.sec.gov/Archives/edgar/data/0001579091/000119312526437014/d24470dex991.htm).
- Uber has a takeover offer for Delivery Hero, with closing expected in H2 2027. Delivery Hero will operate independently until then — [ESM Magazine](https://www.esmmagazine.com/technology/delivery-hero-hikes-2026-forecasts-on-stronger-demand-profitability-320173).

### Inferences

#### A. Which framework answers which question

| Commercialization question | Framework | Testable metric |
|---|---|---|
| Will third-party agents commoditize the platform? | Aggregation theory (Thompson's levels) + Winters' stages + Five Forces (the agent as a new buyer-side intermediary) | Share of orders via third-party agents; channel fee as % of GOV; how many fulfillment networks an agent can route to (agent multi-homing) |
| What protects the platform? | 7 Powers (cornered resource, scale economies, process power) | Cost per delivery vs rivals; share of GOV from exclusive merchants; ETA accuracy; claims and refund handling |
| Is there a real job for the agent? | JTBD + Value Proposition Canvas | Task frequency; delegation willingness by job type; confirm/override rate |
| Does it make money? | Order-level unit economics (Section 2) | Incremental conversion, ΔAOV, inference cost per session, displaced ad revenue, error rate, channel fee |
| Where does the profit pool move? | Value chain + bundling/unbundling | Which layer (discovery / decision / transaction / fulfillment / after-sales) the agent occupies; who holds identity, payment and order history |

#### B. Aggregation-theory test for delivery platforms
- **Delivery platforms are Level-2 aggregators with a costly physical side.** Thompson's Level-2 example is Uber. Delivery platforms add a third side, couriers, so every order carries real marginal cost. General agents such as ChatGPT, Gemini, 千问 and 元宝 behave more like Level-3 demand aggregators.
- **Implication:** agents can capture the demand interface (discovery and choice) cheaply, but cannot cheaply replicate the fulfillment side. The delivery platform risks becoming a "fulfillment API". Its bargaining power then depends on two things:
  1. How many comparable fulfillment networks an agent can route to. Consolidation reduces this number — for example DoorDash–Instacart and Uber–Delivery Hero ([SEC](https://www.sec.gov/Archives/edgar/data/0001579091/000119312526437014/d24470dex991.htm); [ESM](https://www.esmmagazine.com/technology/delivery-hero-hikes-2026-forecasts-on-stronger-demand-profitability-320173)).
  2. Whether merchants and couriers multi-home.
- **The real gap is the 7% vs ~25% take-rate claim.** Citrini's scenario assumes an agent can assemble merchants plus auctioned couriers at a 7% take. The test is whether delivery density (scale economies) and reliability (process power) can be rebuilt at that take rate. Today's evidence (low third-party-agent volumes; Walmart's failed in-chat checkout) suggests the gap is wide in the near term.

#### C. 7 Powers scorecard through an agent-disintermediation lens (my mapping; no published version exists)

| Power | Strength vs third-party agents | Why / what to measure |
|---|---|---|
| Scale economies (delivery density, batching) | Strong | Agents can't replicate courier density. But agents will *surface* cost and speed differences (ARK), so the advantage turns into a price/ETA contest. Measure: cost per delivery and ETA vs the #2 network. |
| Network economies (local liquidity) | Mixed | Supply-side liquidity holds. The demand-side network effect weakens if agents aggregate demand across apps (Winters: network effects collapse). Measure: share of orders that start in the own app vs agents. |
| Counter-positioning | Works *against* incumbents | An incumbent's ad business makes it reluctant to let an agent choose "objectively". AI-native entrants can counter-position with low take rates (Citrini). |
| Switching costs | Weak for consumers, stronger for merchants and couriers | "An agent doesn't have a home screen." Merchants are locked in by POS/menu integrations and ad accounts; couriers by ratings and earnings history. |
| Branding | Weak inside agent flows | Agents ignore brand (Patel). It survives through membership programs, guarantees, and the trust advantage of own-app agents (Bain: 3x). |
| Cornered resource | Strongest | Exclusive merchants, courier fleets, proprietary data (prep times, ETAs, order history), licenses, and payment/identity. Measure: share of GOV that is exclusive or that agents cannot scrape. |
| Process power | Strong | Dispatch, ETA, fraud and refund operations are hard to copy. Agent errors (Walmart's wrong carts) *raise* the value of reliable fulfillment and claims handling. |

#### D. JTBD / VPC applied to the six scenarios
- **Consumer jobs:**
  - Functional: "feed me / us something good, fast, within budget, with minimal decision effort".
  - Emotional: "don't let me regret it".
  - Social: group orders.
  - Agent fit is highest for repeat orders and constraint-heavy tasks (budget, diet, group, multi-store) and lowest for impulse "inspiration" browsing, which is exactly where ads monetize. Evidence: Sparky conversations skew to complex queries rather than staples ([ppc.land/WIRED](https://ppc.land/walmarts-chatgpt-checkout-flopped-heres-what-comes-next/)), and groceries are the most delegable category at 41% ([Checkout.com](https://www.checkout.com/newsroom/consumer-demand-for-ai-shopping-is-forming-fast-but-trust-for-agentic-commerce-is-still-catching-up)).
- **Merchant jobs:** fill idle capacity profitably; set menus, prices and promotions; manage ads, ratings and complaints. Grab's self-serve advertisers grew 39% YoY in Q3 2026 alongside "AI-powered tools" ([Grab Q3 2026](https://www.sec.gov/Archives/edgar/data/0001855612/000185561226000148/a2026q3-earningspressrelea.htm)).
- **Courier jobs:** maximize hourly earnings with low uncertainty. Uber's Earner Assistant turns earnings trends and heatmaps into personalized guidance ([Uber Q1 2026 prepared remarks](https://s23.q4cdn.com/407969754/files/doc_earnings/2026/q1/transcript/Uber-Q1-26-Prepared-Remarks.pdf)).
- **VPC pain relievers that matter most:** accuracy (wrong items), a confirm step, guarantees and refunds, and price transparency. Gain creators: time saved, basket completeness (the missing-accessory problem), and personalization.

#### E. Five Forces and value-chain shift
- **Rivalry:** shifts from app engagement to "being the default fulfillment endpoint" for agents.
- **Buyer power:** the agent platform becomes a new, powerful channel "buyer" that can demand fees: OpenAI's 4% fee precedent, and CICC's WeChat commission thesis.
- **Supplier power:** merchants gain power if agents make multi-homing frictionless.
- **Threat of substitutes:** direct merchant–agent ordering plus auctioned couriers (Citrini).
- **Threat of entry:** lower for front-ends, still high for fulfillment.
- **Value chain:** discovery and decision move to the agent; transaction and fulfillment stay. The ad layer that monetizes discovery is the profit pool most at risk (Bain: about 65% of retail media is onsite).

#### F. Bundling and unbundling
- Agents *unbundle* discovery from fulfillment.
- Platforms *re-bundle* through memberships across categories (Uber One cross-platform incentives — [Uber Q1 2026](https://s23.q4cdn.com/407969754/files/doc_earnings/2026/q1/transcript/Uber-Q1-26-Prepared-Remarks.pdf)) and through ecosystem bundles of agent + commerce + payment. Example: 千问 + 淘宝闪购 + 支付宝 AI付 ([科技日报](https://www.stdaily.com/web/gdxw/2026-01/15/content_461939.html)).

#### G. Scenario × business-model element matrix (for the report's 价值主张 / 价值创造与传递 / 价值获取 test)

| Scenario | 价值主张 (value proposition) | 价值创造与传递 (value creation and delivery) | 价值获取 (value capture) | Go / kill metric |
|---|---|---|---|---|
| 1. Consumer ordering agent in the own app | Less decision effort for repeat and constraint-heavy orders; faster reorders | Order history + real-time menu, availability and ETA + checkout + guarantees; model routing; a confirm step | Existing take rate and fees on *incremental* orders; membership upsell; labeled agent-native sponsored suggestions | Incremental orders per session ≥ inference cost ÷ contribution per incremental order; ad revenue per agent order vs per browsing order; error rate well below 1% |
| 2. Invoked by third-party agents (ChatGPT, Gemini, 千问, 元宝, WeChat) | For the agent platform: a reliable "fulfillment API". For consumers: ordering without the app | MCP/ACP/UCP endpoints; account linking and identity; inventory and ETA APIs; refund policy | Take rate minus channel fee; possibly API or affiliate fees; linking membership accounts | Share of third-party-agent orders from *new* customers (Walmart: ChatGPT brought about 2x the new-customer rate of search); channel fee % vs contribution margin %; repeat orders migrating back to the own app |
| 3. Merchant-operations agents | Automating menus, pricing, promotions, ads, reviews, forecasting | Merchant data + ads system + POS integrations | SaaS fees; more ad spend through automated bidding; commission retention | Merchant ARPU uplift; growth in self-serve advertisers; merchant churn |
| 4. Fulfillment / courier-side automation | For couriers: earnings guidance and less support friction. For the platform: lower cost per delivery | Dispatch and ETA models; earner assistants; robots where permitted | Cost savings kept as margin or passed into lower fees | Cost per delivery (human $8–10 vs robot target of about $1 at ≥1,500 trips per robot per quarter); compliance with algorithm rules |
| 5. Internal efficiency | Faster development; cheaper support | AI coding agents; support automation | Opex savings | Support cost per order; % of contacts automated; headcount growth vs order growth (watch the Klarna pattern: unit cost fell while total cost rose) |
| 6. Selling AI capability externally | Turnkey agents for other merchants and retailers | Productized APIs, SLAs, compliance | SaaS or outcome fees (benchmark: $0.99 per resolution) | Gross margin per outcome; revenue share; distraction from the core business |

### Gaps
- I could not retrieve the full McKinsey, Bain or BCG reports (HTTP 503/403). Their taxonomies and figures rest on search snippets, press releases, and vendor summaries (Mirakl).
- I found no published application of 7 Powers, JTBD or the Value Proposition Canvas to delivery-platform agents. Sections C, D and G are my own mappings.
- I did not access the content of Stratechery's 2026 piece, nor any a16z, Sequoia, Bessemer, Oliver Wyman or Deloitte primary agentic-commerce publication. Deloitte was seen only as citing BCG's 3x trust finding.
- I found no CICC or Huatai quantification of agent-driven traffic diversion in local services.

---

## 2. What is the formula-level unit-economics model for an agent inside a delivery marketplace?

### Takeaway
Model the agent as an incremental P&L layered on the order-level contribution stack:
- **Incremental value:** extra orders × contribution margin + extra basket value × take rate + retention-driven lifetime value + merchant software fees + cost savings.
- **Incremental costs:** inference + error costs + ad cannibalization + lost impulse purchases and upsell + channel fees to third-party agents + adoption subsidies.

Peer delivery profit is only about 2–6% of GOV, roughly $0.2–$3.7 per order. That makes four variables decisive:
1. Measured incrementality, as opposed to attributed sales.
2. Ad displacement, because ads are worth about 50–100% of segment profit.
3. The channel fee on third-party-agent orders.
4. Model routing.

### Cited Findings (inputs that shape the model's structure)
- **Amazon's Rufus metric is "incremental annualized sales"**, defined as purchases customers likely would not have made without Rufus. It relies on attribution assumptions — [ppc.land](https://ppc.land/amazons-ai-shopping-assistant-drove-12-billion-in-sales-for-2025/).
  - *Conflict:* one report says a sale counts as Rufus-driven if the journey started in Rufus within a 7-day rolling window ([EcommerceBytes](https://www.ecommercebytes.com/1sqo), secondary). ppc.land says no window is specified.
- **Conversion lift:** Rufus users convert at rates "more than 60% higher" than non-users (Nov 2025 data) — [ppc.land](https://ppc.land/amazons-ai-shopping-assistant-drove-12-billion-in-sales-for-2025/); [Fortune, 2025-11-02](https://www.fortune.com/2025/11/02/amazon-rufus-ai-shopping-assistant-chatbot-10-billion-sales-monetization).
- **Walmart findings (via WIRED, 2026-03-18):**
  - Sparky users spend about 35% more per order (Walmart US CEO David Guggina).
  - Instant Checkout inside ChatGPT converted at about one-third the rate of click-through to Walmart.com.
  - Shoppers feared receiving "five boxes".
  - High-consideration items were not paired with accessories such as HDMI cables, so attach sales were lost.
  - ChatGPT brings about 2x the rate of new customers compared with search engines.
  - Source: [ppc.land/WIRED](https://ppc.land/walmarts-chatgpt-checkout-flopped-heres-what-comes-next/).
- **OpenAI's channel fee:** OpenAI charged Shopify merchants 4% of each Instant Checkout sale (reported start 2026-01-26), on top of about 2.9% + 30¢ processing — [Shopifreaks](https://www.shopifreaks.com/shopify-confirms-a-4-fee-on-sales-made-through-chatgpt-but-whats-the-real-cost/); [eCommerceNews](https://ecommercenews.com.au/story/openai-to-charge-4-fee-on-shopify-chatgpt-checkout).
  - One source says the fee lapsed when Instant Checkout was discontinued in March 2026 ([Paz.ai](https://www.paz.ai/glossary/instant-checkout-chatgpt), single source).
  - On 2026-03-17, OpenAI said "Instant Checkout is moving to apps" ([Stellagent](https://stellagent.ai/insights/openai-scales-back-instant-checkout); [Forrester](https://www.forrester.com/blogs/what-it-means-that-the-leader-in-agentic-commerce-just-pulled-back)).
- **Retail media at risk:** about 65% of retail-media spend is onsite and exposed if discovery shifts to AI (Bain) — [Crowdfund Insider](https://www.crowdfundinsider.com/2025/11/255589-bain-co-report-details-how-agentic-ai-will-transform-e-commerce/).
- **Subsidies as the adoption lever in China:**
  - On 2026-01-15, 千问 connected to 淘宝闪购 and 支付宝 for one-sentence food ordering, paid through 支付宝 AI付 — [科技日报](https://www.stdaily.com/web/gdxw/2026-01/15/content_461939.html).
  - Its "春节30亿大免单" (¥3B free-order) campaign produced 41亿 "千问帮我" requests and more than 1.2亿 AI-completed orders in 6 days — [扬子晚报, 2026-02-12](https://www.yzwb.net/news/ch/202602/t20260212_321578.html).
- **Support unit cost:** Klarna's customer-service cost per transaction fell from $0.32 (Q1 2023) to $0.19 (Q1 2025) — [CX Dive](https://www.customerexperiencedive.com/news/klarna-credits-ai-slash-customer-service-costs/748647/).

### Inferences — the model (all formulas are mine)

#### 2.1 Base contribution per order (before the agent)

`CM_order = AOV × (commission% + ads%) + consumer fees (delivery + service/platform) − courier cost − payment processing − support − refunds/credits − promotions/subsidies − other variable costs`

Peers disclose either a contribution-like figure (gross profit or contribution margin as % of GOV) or segment EBITDA / operating income. Section 3 gives both.
- **Use contribution margin** for decisions about incremental orders.
- **Use EBITDA-level margin** for orders that are merely shifted from one channel to another.

#### 2.2 Incremental contribution of an own-app consumer agent, per period

```
ΔΠ_agent =  N_s × c_inc × CM_inc                     (incremental orders: sessions × incremental orders per session × contribution per incremental order)
         +  O_a × ΔAOV × τ_eff                       (basket lift on agent orders × effective take rate incl. ads/fees)
         +  U × Δr × LTV                             (retention: users × churn reduction × lifetime contribution)
         +  Ads_agent                                (new agent-native ad formats, e.g., sponsored suggestions/brand prompts)
         +  S_support                                (support deflection savings; see 3.D)
         −  N_s × C_inf                              (inference + retrieval + guardrail cost per session)
         −  O_a × AOV × a% × k                       (ad cannibalization: k = share of ad revenue lost when the agent chooses instead of the user browsing)
         −  O_a × L_impulse × τ_eff                  (lost impulse/attach value per agent order)
         −  O_a × e × C_err                          (error rate × cost per error: refund + redelivery + support + goodwill)
         −  Sub                                      (subsidies/coupons to seed agent adoption)
         −  F                                        (fixed: evals, safety, engineering, amortized)
```

For third-party-agent orders (scenario 2), add `− O_3P × AOV × f`, where f is the channel fee rate. Count only the *incremental* share of those orders at CM_inc. Channel-shifted orders carry the fee and the ad loss but no new contribution.

**Incrementality must come from holdouts or A/B tests.** Amazon's "incremental" figure is model-based. Walmart's +35% AOV is a cross-sectional comparison between users and non-users, so self-selection by heavy users inflates it.

#### 2.3 Break-even conditions (worked numbers in Section 3)
- **Per session:** `c_inc × CM_inc ≥ C_inf + (a% × k × AOV × agent-order rate) + (e × C_err × agent-order rate)`.
- **Third-party-agent order:** accretive only if `CM_inc − f × AOV − displaced ads > 0` for incremental orders. A channel-shifted order is *always* dilutive by `f × AOV + displaced ads`.
- **Merchant agent:** `ARPU_merchant (SaaS fee + take on extra ad spend) − inference per merchant-month − support > 0`, plus the value of lower merchant churn.
- **Courier automation:** `savings per delivery = (human cost − automated cost) × automation share`. Utilization is the binding variable; see Serve Robotics in 3.5.
- **Internal efficiency:** `savings = Δheadcount × fully loaded cost + Δcontacts × (human cost − AI cost)`. Check that *total* cost falls, not just unit cost; Klarna's total service cost rose.
- **Selling AI externally:** `gross margin = price per outcome − inference − human-in-the-loop`. Benchmark price: $0.99 per resolution.

### Gaps
- No delivery platform discloses agent incrementality, error rates, inference cost per session, or agent-channel take rates. c_inc, k, e and C_err must be assumed or tested.
- I found no delivery-peer disclosure of support contact rate per order or cost per contact.

---

## 3. Benchmark values to populate the model: profit per order, ads as % of GOV, inference cost, support cost

### Takeaway
- **Profit pools are thin.** Segment profit runs about 2.7% of GOV at Grab, 3.4% at Swiggy, 3.8% at Uber Delivery, 3.2% at Instacart (about $3.74 per order) and 5.5% at Zomato. Delivery Hero was about 1.4% in FY2025.
- **Ads match profit.** Ads run about 2–3.1% of GOV (Uber Delivery above 2%, Instacart 3.1%), comparable to the entire segment profit.
- **Inference cost depends on the model tier.** My estimate for a base ordering session is about $0.007–$0.03 on small models and $0.14–$0.39 on frontier-tier models at list price. Model routing therefore decides accretion in low-AOV markets such as India, Southeast Asia and China.
- **List prices can rise.** Google's Gemini 3.8 Flash list price doubles on 2027-01-01.

### Cited Findings

#### 3.1 Profitability benchmarks (latest disclosed; DoorDash and Meituan excluded)

| Platform (period, release date) | Scale | Profit measure | % of GOV | Source |
|---|---|---|---|---|
| Uber Delivery (Q2 2026, released 2026-08-05) | GB $27,463M (+26%); revenue $5,245M | Segment operating income $1,055M (Uber stopped reporting segment adj. EBITDA) | 3.8% (computed) | [Uber Q2 2026 release, SEC](https://www.sec.gov/Archives/edgar/data/0001543151/000154315126000027/uberq226earningspressrelea.htm) |
| Uber Delivery (Q1 2026, 2026-05-06) | GB +23% | Segment operating income margin, "record 3.7%" | 3.7% | [Uber Q1 2026 prepared remarks](https://s23.q4cdn.com/407969754/files/doc_earnings/2026/q1/transcript/Uber-Q1-26-Prepared-Remarks.pdf) |
| Uber Delivery (Q3 2025) | GB $23.32B | Adj. EBITDA $921M | ~3.9% (computed) | [Finviz/Zacks](https://finviz.com/news/216965/uber-uber-reports-q3-earnings-what-key-metrics-have-to-say) (secondary) |
| Instacart (Q3 2026, released 2026-10-29) | 91.4M orders (+10%); GTV $10,528M (+15%) | GAAP gross profit $786M (7.5%); adj. EBITDA $342M (3.2%); transaction revenue 7.3% of GTV; total revenue 10.4% of GTV | 7.5% GP / 3.2% EBITDA | [Instacart Q3 2026, SEC](https://www.sec.gov/Archives/edgar/data/0001579091/000119312526437014/d24470dex991.htm) |
| Grab Deliveries (Q3 2026, released 2026-11-03) | GMV $4,500M (+21%; +24% constant currency); revenue $561M | Segment adj. EBITDA $123M (+58%) | 2.7% (+65 bp YoY) | [Grab Q3 2026, SEC](https://www.sec.gov/Archives/edgar/data/0001855612/000185561226000148/a2026q3-earningspressrelea.htm) |
| Grab Deliveries (Q2 2026) | GMV $4,249M | Segment adj. EBITDA $96M | 2.3% | [Grab Q2 2026, SEC](https://www.sec.gov/Archives/edgar/data/0001855612/000185561226000123/a2026q2-earningspressrelea.htm) |
| Swiggy Food Delivery (Q2 FY27, released 2026-10-22) | GOV ₹10,054 Cr (+17.7%); 19.8M MTUs | Adj. EBITDA ₹341 Cr | 3.4% (+58 bp) | [Swiggy press release](https://www.swiggy.com/corporate/wp-content/uploads/2026/10/Press-release_Q2FY2027.pdf) |
| Swiggy Food Delivery (Q1 FY27) | — | Contribution margin | 7.6% of GOV (broker note) | [Business Standard broker PDF](https://bsmedia.business-standard.com/_media/bs/data/market-reports/equity-brokertips/2026-07/17854747480.60298100.pdf) (secondary) |
| Eternal / Zomato Food Delivery (Q2 FY27, Oct 2026) | NOV ₹11,445 Cr (+21.5%) | Adj. EBITDA ₹627 Cr | 5.5% of NOV | [INDmoney](https://www.indmoney.com/blog/stocks/eternal-q2-results-analysis); [YourStory](https://yourstory.com/2026/10/eternal-q2-net-profit-rises-92-fy27-revenue-up-78-percent) (secondary) |
| Zomato Food Delivery (Q1 FY27) | — | Adj. EBITDA 5.6% of NOV; platform fee ₹14.9 per order in major cities | 5.6% | [Inc42, 2026-07-24](https://inc42.com/features/zomato-business-model-commissions-platform-fees-and-ad-revenue/) |
| Zomato Food Delivery (broker forecast, Sept 2025) | — | Contribution margin 8.6% (FY26E) → 9% (FY27E) | forecast | [Business Standard](https://www.business-standard.com/amp/markets/news/strong-growth-improving-profitability-bode-well-for-eternal-s-stock-125091901112_1.html) |
| Delivery Hero (FY2025) | — | Adj. EBITDA €692.5M; Q4 2025 gross profit margin 8.3% (record) | 1.4% of GMV | [finanzen.ch/EQS](https://script.finanzen.ch/nachrichten/aktien/eqs-news-delivery-hero-reports-strong-q4-while-business-transforms-into-everyday-app-1035877389) (secondary) |
| Delivery Hero (Q3 2026 update, 2026-10-28) | GMV about €13.9B (+13.6% like-for-like); revenue about €4.35B (+22% like-for-like); more than 1B orders | FY26 guidance: adj. EBITDA €980M–€1.02B; GMV +10–12% like-for-like | — | [Deutsche Börse/EQS](https://live.deutsche-boerse.com/news/EQS-News-Delivery-Hero-hits-one-billion-orders-in-Q3-as-growth-accelerates-further-raises-full-year-guidance-d2ae8aed-ba65-4443-a5ec-381ed4714cf2); [Nasdaq, H1 2026](https://nasdaq.com/articles/delivery-hero-h1-adj-ebitda-rises-increases-2026-outlook) |

Context: Uber's company-wide adj. EBITDA was 4.9% of gross bookings in Q2 2026. Its Q3 2026 guidance is GB of $58.25–60.25B and adj. EBITDA of $2.86–2.96B; Q3 actuals were not yet released as of 2026-11-03 — [Uber Q2 2026 release](https://www.sec.gov/Archives/edgar/data/0001543151/000154315126000027/uberq226earningspressrelea.htm).

#### 3.2 Ads as % of GOV
- **Instacart:** "advertising and other" revenue was $331M in Q3 2026, or 3.1% of GTV (+23% YoY), about 30% of total revenue — [Instacart Q3 2026](https://www.sec.gov/Archives/edgar/data/0001579091/000119312526437014/d24470dex991.htm); [ppc.land](https://ppc.land/instacart-ad-revenue-gains-23-to-331m-in-quarter-before-doordash-deal/).
- **Uber, primary:** Delivery advertising revenue rose $182M YoY in Q2 2026 and $362M YoY in H1 2026 — [Uber 10-Q, Q2 2026](https://www.sec.gov/Archives/edgar/data/0001543151/000154315126000032/uber-20260630.htm).
- **Uber, secondary:**
  - On the Q4 2025 call, management said delivery ads had exceeded the prior 2%-of-GB penetration target, with a run-rate above $2B ([rallies.ai](https://rallies.ai/news/uber-exits-2025-with-327-margin-must-prove-ad-and-eats-economics-in-2026)).
  - The run-rate was above $1.5B in Q1 2025 ([Nasdaq/Motley Fool](https://www.nasdaq.com/articles/uber-ads-hidden-gem-powering-ubers-next-growth-engine)).
  - *Conflict:* one outlet put 2025 ad revenue at about $500M. This does not fit a $2B run-rate and is likely unreliable.
- **Grab:** advertising contributions were credited for Q2 2026 margin expansion ([Malaysian Wireless](https://www.malaysianwireless.com/2026/08/grab-q2-2026-revenue-profit/)). In Q3 2026, quarterly active self-serve advertisers rose 39% YoY and their average spend rose 13% ([Grab Q3 2026](https://www.sec.gov/Archives/edgar/data/0001855612/000185561226000148/a2026q3-earningspressrelea.htm)). The ads share of GMV is not disclosed.
- **Delivery Hero:** AdTech is cited as a driver of revenue growing faster than GMV in Q3 2026 — [Deutsche Börse/EQS](https://live.deutsche-boerse.com/news/EQS-News-Delivery-Hero-hits-one-billion-orders-in-Q3-as-growth-accelerates-further-raises-full-year-guidance-d2ae8aed-ba65-4443-a5ec-381ed4714cf2).
- **Zomato:** no ad revenue share is disclosed. Ad pricing is ₹5–15 per click for sponsored listings and ₹30–300 per 1,000 impressions for banners — [Inc42](https://inc42.com/features/zomato-business-model-commissions-platform-fees-and-ad-revenue/).

#### 3.3 LLM token list prices (USD per 1M tokens, as of early Nov 2026)

| Model | Input | Cached input | Output | Source |
|---|---|---|---|---|
| Claude Haiku 5.5 (prompts ≤100k tokens) | $0.10 | $0.01 | $0.50 | [Anthropic pricing](https://platform.claude.com/docs/en/about-claude/pricing) |
| Claude Haiku 4.5 | $1.00 | $0.10 | $5.00 | [Anthropic](https://platform.claude.com/docs/en/about-claude/pricing) |
| Claude Sonnet 5.5 | $2.00 | $0.10 | $10.00 | [Anthropic](https://platform.claude.com/docs/en/about-claude/pricing) |
| Claude Opus 5.5 | $4.00 | $0.20 | $20.00 | [Anthropic](https://platform.claude.com/docs/en/about-claude/pricing) |
| Gemini 3.8 Flash | $0.75 through 2026-12-31 → $1.50 from 2027-01-01 | $0.075 → $0.15 | $3.75 → $7.50 | [Google AI Studio pricing](https://aistudio.google.com/docs/pricing) |
| Gemini 3.5 Flash-Lite | $0.30 | $0.03 | $2.50 | [Google](https://aistudio.google.com/docs/pricing) |
| Gemini 3.1 Flash-Lite | $0.25 | $0.025 | $1.50 | [Google](https://aistudio.google.com/docs/pricing) |
| Gemini 2.5 Flash-Lite | $0.10 | $0.01 | $0.40 | [Google](https://aistudio.google.com/docs/pricing) |
| Gemini 4 Argon | $2.00 → $4.00 from 2026-12-01 | $0.10 → $0.20 | $10.00 → $20.00 | [Google](https://aistudio.google.com/docs/pricing) |
| DeepSeek Flash (V4.1-Flash), peak / off-peak | $0.30 / $0.15 | $0.006 / $0.003 | $1.20 / $0.60 | [DeepSeek pricing](https://api-docs.deepseek.com/quick_start/pricing) |
| DeepSeek V4-Pro, peak / off-peak | $1.32 / $0.66 | $0.044 / $0.022 | $3.96 / $1.98 | [DeepSeek](https://api-docs.deepseek.com/quick_start/pricing) |
| GPT-5.5 / GPT-5.4 mini / GPT-5.4 nano | $5 / $0.75 / $0.20 | — / $0.075 / $0.02 | $30 / $4.50 / $1.25 | [MorphLLM aggregator](https://morphllm.com/openai-api-pricing) (secondary; the official OpenAI page returned 403) |

#### 3.4 Customer-support automation benchmarks
- **Klarna:**
  - Service cost per transaction fell 40%, from $0.32 in Q1 2023 to $0.19 in Q1 2025 ([CX Dive](https://www.customerexperiencedive.com/news/klarna-credits-ai-slash-customer-service-costs/748647/)).
  - The AI agent handles about two-thirds of inquiries and saved $60M (Q3 2025 call). But customer service and operations cost $50M in Q3 2025, up from $42M a year earlier, and Klarna partly reversed course by rehiring humans ([Pure AI, 2025-10-22](https://pureai.com/Articles/2025/10/22/Why-Klarnas-AI-Experiment-Matters.aspx)).
  - The AI agent does the work of 853 employees ([CX Dive](https://www.customerexperiencedive.com/news/klarna-says-ai-agent-work-853-employees/805987/)).
- **Intercom Fin vendor price:** about $0.99 per resolution, with outcome-based pricing and seat fees extra — [Macha](https://www.getmacha.com/blog/intercom-fin-pricing) (secondary, vendor-adjacent).
- **Gartner (2025-03-05):** predicts agentic AI will autonomously resolve 80% of common customer-service issues by 2029, with about 30% lower operational costs — [Gartner newsroom](https://www.gartner.com/en/newsroom/press-releases/2025-03-05-gartner-predicts-agentic-ai-will-autonomously-resolve-80-percent-of-common-customer-service-issues-without-human-intervention-by-20290?ssrid=ssr); [CX Today](https://cxtoday.com/contact-center/agentic-ai-gartner-predicts-80-of-customer-problems-solved-without-human-help-by-2029). Gartner's June 2025 caution: more than 40% of agentic AI projects will be canceled by end-2027 because of cost, unclear value, and weak risk controls — [Maven AGI recap](https://mavenagi.com/resources/one-year-since-gartners-ai-resolution-prediction).
- **Uber:**
  - Q1 2026 remarks list "driving down customer support costs through automation" among efficiency levers.
  - 95% of engineers use AI coding tools monthly, and more than 10% of code is written autonomously by AI coding agents.
  - This is "allowing us to moderate the pace of hiring".
  - Source: [Uber Q1 2026 prepared remarks](https://s23.q4cdn.com/407969754/files/doc_earnings/2026/q1/transcript/Uber-Q1-26-Prepared-Remarks.pdf).
  - In July 2026, Uber cut about 10% of its community-operations (support) team, citing AI — [CX Today](https://cxtoday.com/uber-customer-service-jobs-ai-contact-centers) (secondary).

#### 3.5 Fulfillment / courier-side automation
- **Serve Robotics** (Uber Eats partner; investor materials, 2026):
  - Human-courier cost is $8–10 per delivery.
  - The target is about $1 per delivery, but only once utilization exceeds 1,500 trips per robot per quarter.
  - Daily operating hours per robot rose 56% to more than 12 hours in Q4 2025.
  - Gen-3 hardware cost claims conflict: $20k → $12k vs $15k → under $10k.
  - Sources: [Serve investor deck via Boardroom Alpha](https://www.boardroomalpha.com/sec/serv-8-k-2026-03-11-0001832483-26-000004/finalserve_investorxdeck.htm); [Nasdaq/Zacks](https://www.nasdaq.com/articles/will-serve-robotics-gen-3-robots-drive-faster-unit-economics) (secondary summaries).
- **Uber Earner Assistant:** rolled out across the U.S. in 2026 to turn "earnings trends and heatmaps into simple, personalized guidance" — [Uber Q1 2026](https://s23.q4cdn.com/407969754/files/doc_earnings/2026/q1/transcript/Uber-Q1-26-Prepared-Remarks.pdf).
- **China's 《外卖平台服务管理基本要求》 (SAMR national standard, Dec 2025):** requires dispatch algorithms to set delivery times considering road and weather conditions, and bans algorithm-forced overtime — [第一财经](https://www.yicai.com/news/102948456.html).

#### 3.6 Channel fees ("agent tax") benchmarks
- **OpenAI Instant Checkout:** 4% of sale (from Jan 2026, for Shopify merchants), on top of processing — [Shopifreaks](https://www.shopifreaks.com/shopify-confirms-a-4-fee-on-sales-made-through-chatgpt-but-whats-the-real-cost/).
- **Citrini's scenario:** AI apps at about a 7% take vs DoorDash's roughly 25% blended — [Stocktwits](https://stocktwits.com/news-articles/markets/equity/doordash-moat-under-threat-citrini-research-warns-consumer-ai-agents-bypass-delivery-apps/cZRvn0mR4zK).
- **CICC on WeChat:** an agent entry point could raise WeChat's effective commission — [界面](https://m.jiemian.com/article/14842082.html).

### Inferences — computed benchmarks (all Est.)

#### 3.A Profit per order (Est.)
- **Instacart Q3 2026** (AOV ≈ $115.2 = $10,528M ÷ 91.4M orders):
  - Gross profit ≈ $8.60 per order ($786M ÷ 91.4M).
  - Adj. EBITDA ≈ $3.74 per order ($342M ÷ 91.4M).
- **Delivery-only peers with assumed AOVs.** These AOVs are *illustrative assumptions*, because the releases do not disclose them:

| Peer | Margin | Assumed AOV | Profit per order |
|---|---|---|---|
| Uber Delivery | 3.8% | $40 | ≈ $1.52 |
| Grab | 2.7% | $15 | ≈ $0.41 |
| Zomato | 5.5% | ₹450 at ₹88/$ | ≈ $0.28 |
| Swiggy | 3.4% | ₹500 at ₹88/$ | ≈ $0.19 |
| Delivery Hero (FY25) | 1.4% | €25 at $1.08/€ | ≈ $0.38 |
| China, illustrative | ¥1 per order | — | ≈ $0.14 |

- **Delivery Hero 2026E:** adj. EBITDA / GMV ≈ €1.0B ÷ (€49.5B FY25 GMV implied by 1.4% × 1.11 growth) ≈ 1.8%. This is rough: reported-currency vs like-for-like and hyperinflation accounting both distort it.

#### 3.B Inference cost per agent session and per order (Est.)

Assumptions for three session archetypes:
- System prompt plus tool definitions ≈ 3–4k tokens; user memory ≈ 1k tokens; search, menu and ETA tool results ≈ 1.5–10k tokens per call.
- One user turn triggers about 2 model calls (plan/tool call, then compose).
- Prompt caching applies to stable prefixes. Cache-write premiums are ignored.

| Archetype | Model calls | Input tokens | Output tokens (incl. short reasoning) |
|---|---|---|---|
| **S1 reorder / simple** ("my usual") | 3 | 18k total, 80% cached | 900 |
| **S2 discovery + order** (base) | 8 | 96k total, 60% cached | 5.6k |
| **S3 complex, multi-step** (group order, multi-merchant, reasoning-heavy) | 20 | 400k total, 50% cached | 30k |

**Cost per session (USD):**

| Model | S1 | S2 | S3 |
|---|---|---|---|
| Claude Haiku 5.5 | 0.0010 | 0.0072 | 0.037 |
| DeepSeek Flash (peak) | 0.0022 | 0.0186 | 0.097 |
| Gemini 3.5 Flash-Lite | 0.0038 | 0.0272 | 0.141 |
| Gemini 3.8 Flash (2026 price) | 0.0072 | 0.0541 | 0.278 |
| Gemini 3.8 Flash (2027 price) | 0.0143 | 0.1082 | 0.555 |
| GPT-5.4 mini (aggregator price) | 0.0078 | 0.0583 | 0.300 |
| Claude Sonnet 5.5 | 0.0176 | 0.1386 | 0.720 |
| GPT-5.5 (aggregator price; cache assumed 90% off) | 0.0522 | 0.3888 | 2.000 |

**Cost per order.** Assume 80% of S1 sessions and 40% of S2/S3 sessions end in an order (assumption):
- Haiku 5.5: $0.0012 / $0.018 / $0.09
- DeepSeek Flash: $0.003 / $0.047 / $0.24
- Gemini 3.8 Flash (2026 price): $0.009 / $0.135 / $0.69
- Sonnet 5.5: $0.022 / $0.35 / $1.80
- GPT-5.5: $0.065 / $0.97 / $5.00

**S2 inference per order as % of the per-order profit in 3.A:**

| Model | Instacart | Uber | Grab | Zomato | Swiggy | Delivery Hero | China (¥1) |
|---|---|---|---|---|---|---|---|
| Haiku 5.5 | 0.5% | 1.2% | 4.5% | 6.4% | 9.3% | 4.8% | 12.8% |
| DeepSeek Flash | 1.3% | 3.1% | 11.5% | 16.5% | 24% | 12% | 33% |
| Gemini 3.8 Flash (2026 price) | 3.7% | 8.9% | 33% | 48% | 70% | 36% | 96% |
| Sonnet 5.5 | 9.4% | 23% | 86% | 123% | 179% | 92% | 246% |
| GPT-5.5 | 26% | 64% | 240% | 346% | 503% | 257% | 690% |

Implications:
- **Model routing is the main lever.** Small and cheap models make high-frequency ordering agents viable everywhere. Frontier-tier models are viable only for high-AOV baskets (Instacart-like) or when routed to a small minority of complex sessions. Walmart already routes Sparky queries across models by answer quality ([ppc.land/WIRED](https://ppc.land/walmarts-chatgpt-checkout-flopped-heres-what-comes-next/)).
- **Prices are not monotonically falling.** Google's scheduled doubling of Gemini 3.8 Flash and Gemini 4 Argon list prices ([Google](https://aistudio.google.com/docs/pricing)) means the cost model needs a price-shock sensitivity.
- **In China, subsidies dwarf inference as the cost of building agent habits.** ¥3B ÷ 1.2亿 orders ≈ ¥25 per AI order. This is an *upper bound*, because the campaign budget was not necessarily all spent on first-wave orders. Inference, by comparison, is about ¥0.01–0.15 per base session.

#### 3.C Break-even incremental conversion (Est.)
Incremental orders needed per 100 S2 sessions to cover inference alone = C_inf ÷ CM_inc:

| Model | CM_inc = $1.50 (US-like) | CM_inc = $0.40 (low-AOV market) |
|---|---|---|
| Haiku 5.5 | 0.5 | 1.8 |
| DeepSeek Flash | 1.2 | 4.7 |
| Gemini 3.8 Flash (2026 price) | 3.6 | 13.5 |
| Gemini 3.8 Flash (2027 price) | 7.2 | 27 |
| Sonnet 5.5 | 9.2 | 35 |
| GPT-5.5 | 26 | 97 |

Rufus's reported "60% higher conversion" is a user-vs-non-user comparison, not a measured incremental conversion, so it cannot be plugged in here directly.

#### 3.D Support savings per order (Est.)
`savings per order = contact rate × AI resolution share × (human cost per contact − AI cost per contact)`

Assumptions:
- Contact rate 2–5% of orders (assumption; not disclosed by peers).
- AI resolution share 50–70% (Klarna: about two-thirds).
- Human cost $3.13 per contact ($25/hr fully loaded at 8 contacts/hr; assumption).
- In-house AI cost $0.05 per contact, or a vendor price of $0.99.

Result: savings ≈ $0.03–$0.11 per order in-house, or $0.02–$0.075 with a vendor. That is about 2–7% of a $1.52 US per-order profit. In low-AOV markets human support is also cheaper, so the relative gain is similar rather than larger.

#### 3.E Ad cannibalization (Est.)
`lost ad revenue as % of total GOV = agent order share × ads % of GOV × share of ad load lost`

| Agent order share | Ads as % of GOV | Ad load lost | Lost revenue (% of total GOV) | Share of a 3.2% EBITDA margin |
|---|---|---|---|---|
| 20% | 3.1% (Instacart) | 50% | 0.31% | ≈ 10% |
| 40% | 3.1% | 50% | 0.62% | ≈ 19% |

Because ads ≈ 97% of Instacart's adj. EBITDA (3.1% vs 3.2% of GTV) and ≥ 53% of Uber Delivery's operating income (>2% vs 3.8%), ad displacement is the single largest profit risk. That holds unless agent-native ad formats replace it, for example Rufus brand prompts or Walmart testing ads in Sparky.

#### 3.F Agent-tax illustration (Est.)
- **Channel-shifted order:** take an Uber-like order with an assumed AOV of $40 and operating income of $1.52 (3.8%). A 4% channel fee costs $1.60, so the order's profit is −$0.08 before any ad loss. If it also displaces 2% ads ($0.80), the order's profit falls to about −$0.88.
- **Truly incremental order:** compare against contribution-level margin instead (Instacart gross profit 7.5% of GTV; Swiggy contribution margin 7.6%; Zomato contribution margin about 8.6–9%; Delivery Hero gross profit 8.3%). A 4% fee consumes about 45–55% of that contribution, so the order stays accretive but at half the margin.
- **Conclusion:** third-party agent channels pay only if they bring *new* customers (Walmart: about 2x the new-customer rate) and the platform migrates repeat orders back to its own app or to a lower fee tier.

#### 3.G Error-cost illustration (Est.)
- Assume a $40 AOV order with $1.52 profit, and a platform-borne error that costs a full refund plus about $3 of support (≈ $43).
- A 1% agent error rate costs about $0.43 per agent order, roughly 28% of profit.
- About 3.5% wipes out the whole per-order profit.
- So agent ordering needs error rates well below 1%, or confirm-before-checkout flows, or merchant- or insurer-borne liability.

### Gaps
- Uber's Q3 2026 results were not yet released.
- Swiggy's and Eternal's Q2 FY27 food-delivery contribution margins were not retrieved: the shareholder letters exceeded fetch limits, and Zomato no longer headlines contribution margin.
- Peer AOVs for Uber Delivery, Grab, Swiggy and Zomato were not in the releases, so 3.A uses labeled assumptions.
- OpenAI's official pricing page was blocked; GPT prices come from an aggregator.
- No platform discloses inference cost per session, support contact rate, or cost per contact. No independent Gartner cost-per-contact benchmark was found.
- Serve's $1-per-delivery figure is a target, not an achieved cost.

---

## 4. What do comparable own-platform shopping and ordering agents disclose (with dates and metric definitions)?

### Takeaway
- **Own-platform agents** have the strongest disclosed results:
  - **Amazon Rufus:** 300M+ users in 2025, about $12B of incremental annualized sales, users converting more than 60% better.
  - **Walmart Sparky:** half of app users engaged, about 35% higher spend per order.
  - **JD 京言:** about 80M users.
  - **Alibaba 千问:** about 1.4亿 first-time AI shoppers within roughly six weeks, heavily subsidized.
- **Checkout inside third-party agents underperformed:** Walmart's ChatGPT Instant Checkout converted at about one-third of click-through, and OpenAI pulled back in March 2026.
- **All metrics are company-defined and unaudited.**

### Cited Findings
- **Amazon Rufus:**
  - **Q3 2025 call:** 250M shoppers used Rufus in 2025; it was on pace for more than $10B of incremental annualized sales; users were "60% more likely to complete a purchase" — [Fortune, 2025-11-02](https://www.fortune.com/2025/11/02/amazon-rufus-ai-shopping-assistant-chatbot-10-billion-sales-monetization); [Modern Retail](https://www.modernretail.co/technology/amazon-says-its-ai-shopping-assistant-is-gaining-traction-with-rufus-users-up-115/).
  - **Q4 2025 results (Feb 2026):** more than 300M customers used Rufus in 2025, delivering nearly $12B of incremental annualized sales, "an even stronger response than anticipated". Amazon gave no Rufus cost figures. An analysis computes about $40 per user per year — [ppc.land](https://ppc.land/amazons-ai-shopping-assistant-drove-12-billion-in-sales-for-2025/).
  - **Q1 2026 call (2026-04-30):** Rufus MAU +115% YoY and engagement +400%. Nearly 20% of shoppers who interact with a brand prompt continue that brand conversation. Customers can set recurring shopping tasks such as restocking — [Modern Retail](https://www.modernretail.co/technology/amazon-says-its-ai-shopping-assistant-is-gaining-traction-with-rufus-users-up-115/).
  - *Unverified:* that the $10B figure includes ad revenue from sponsored content in Rufus ([Modern Retail search summary](https://www.modernretail.co/technology/amazon-says-its-ai-shopping-assistant-is-gaining-traction-with-rufus-users-up-115/)). No Q3 2026 Rufus update was found.
- **Walmart Sparky** (WIRED interview with EVP Daniel Danker, 2026-03-18):
  - Half of Walmart app users have engaged with Sparky; Sparky users spend about 35% more per order.
  - Queries skew complex (exotic ingredients, home projects, multi-department needs).
  - Sparky runs on open-source gen-AI models plus retail-specific models, with routing by answer quality. Danker said it is currently "slow and sometimes gives weak answers".
  - Instant Checkout in ChatGPT, launched 2025-09-29, converted at about one-third of click-through and put wrong items in carts.
  - Sparky has run inside ChatGPT since the week of 2026-03-25 and in Gemini from April, with baskets synced across surfaces.
  - Source: [ppc.land/WIRED](https://ppc.land/walmarts-chatgpt-checkout-flopped-heres-what-comes-next/).
  - Walmart cited "good momentum with Sparky" on its Q4 FY26 call (2026-02-19) — [MarketBeat transcript](https://www.marketbeat.com/earnings/reports/2026-2-19-walmart-inc-stock/).
  - *Unverified:* the claim that Sparky in ChatGPT reached about 70% of direct users' purchase completion ([gentic.news](https://gentic.news/article/walmart-s-sparky-chatbot-replaces-openai-s-instant-checkout-after-3x-lower-conve), secondary).
- **Instacart:**
  - ChatGPT app with in-chat checkout launched 2025-12-08. Instacart claims it was the first grocery partner with end-to-end checkout in ChatGPT; it covers 1,800+ retailers, uses Stripe and the Agentic Commerce Protocol, and fulfills through its own network — [Barchart/press release](https://www.barchart.com/story/news/36507554/instacart-app-launches-in-openai-chatgpt-first-company-to-offer-new-instant-checkout-app-experience).
  - Coverage of Instacart's 2026 results says Google AI Mode began sending grocery requests to Instacart on 2026-07-16, with checkout completed in Instacart's app or site — [Supermarket News](https://www.supermarketnews.com/finance/instacart-sales-climb-14-as-grocery-platform-expands-ai-retail-technology) (secondary; not in the Q3 release text).
  - The Q3 2026 release gives no AI metric; it lists "AI Solutions" as a product line — [SEC](https://www.sec.gov/Archives/edgar/data/0001579091/000119312526437014/d24470dex991.htm).
- **Uber** (Q1 2026, 2026-05-06): Cart Assistant turns "a recipe, image, or simple prompt, into a curated cart, reducing friction and increasing conversion". The Earner Assistant rolled out across the US. Uber continues "to build and launch third-party integrations". No metrics were disclosed — [Uber Q1 2026 prepared remarks](https://s23.q4cdn.com/407969754/files/doc_earnings/2026/q1/transcript/Uber-Q1-26-Prepared-Remarks.pdf).
- **Alibaba / 千问 / 淘宝闪购:**
  - **2026-01-15:** the 千问 app connected to 淘宝闪购 and 支付宝 for one-sentence food ordering. The demo ordered 40 cups of milk tea. A "任务助理" invite test handles complex orders — [科技日报](https://www.stdaily.com/web/gdxw/2026-01/15/content_461939.html).
  - **2026-02-12, first 6 days of the campaign:** 41亿 "千问帮我" requests and more than 1.2亿 AI-completed orders. Nearly half of 千问-originated 淘宝闪购 orders came from county towns, and 1.56M elderly users ordered delivery for the first time — [扬子晚报](https://www.yzwb.net/news/ch/202602/t20260212_321578.html).
  - **By end-Feb 2026:** nearly 1.4亿 users had "first experienced AI shopping" through 千问's agent features; 千问 consumer MAU exceeded 300M in Feb 2026 — [上海证券报 via 同花顺, 2026-03-19](https://news.10jqka.com.cn/20260319/c675415565.shtml).
- **JD 京言:** launched 2023 on JD's own LLM. Nearly 80M users used 京言 to assist shopping in the JD app by Q1 2026. A standalone "京东AI购" app launched at end-Dec 2025. Features include price trends, AI review summaries and comparisons. No conversion or GMV data — [投资界/锌刻度, 2026-08-04](https://news.pedaily.cn/202608/567248.shtml).
- **Market-level context:**
  - **Salesforce, Cyber Week 2025 (Nov 25–Dec 1):** AI and agents influenced $67B, or 20% of orders, out of a record $336.6B. Retailers using Agentforce or their own agents grew sales 32% faster. Agent actions rose 70% week over week — [Salesforce IR](https://investor.salesforce.com/news/news-details/2025/Salesforce-Data-AI-and-Agents-Propel-Cyber-Week-to-Record-336-6B-in-Global-Spend/).
  - **Adobe:** AI-referred shoppers generated 53% more revenue per visit in Q3 2026 ([Adobe Q3 2026 report](https://business.adobe.com/resources/sdk/.q3-ai-traffic-trends-report/q3-2026-ai-sourced-traffic-insights.pdf), secondary). Adobe's 2026 holiday forecast is $275.1B online with AI traffic +130% ([Relevant Audience](https://www.relevantaudience.com/ecommerce-marketing/adobe-2026-holiday-forecast-275-billion-ai-traffic-130-percent/)). In the 2025 holiday season, retail AI traffic rose 693% YoY and converted 31% better than other sources ([Adobe blog](https://business.adobe.com/uk/blog/ai-driven-traffic-surges-across-industries), secondary). July 2026 was the 11th straight month of AI traffic out-converting non-AI ([Digital Commerce 360](https://www.digitalcommerce360.com/2026/08/19/adobe-ai-referral-traffic-data-july-2026/)).
  - **Similarweb:** GenAI is about 0.4% of retail visits — [Craftshift](https://craftshift.com/ai-referral-traffic-ecommerce-stats-2026/) (secondary).

### Inferences
- **Metric-definition cautions for the report:**

| Company | Metric | What it actually measures |
|---|---|---|
| Amazon | "Incremental annualized sales" | Model-attributed; attribution window disputed |
| Amazon | "% more likely to purchase" | Users vs non-users; selection bias |
| Walmart | "Spend per order" | Cross-sectional comparison |
| Alibaba | "Users who first experienced AI shopping" | Subsidy-driven trial |
| JD | "Users using 京言 to assist shopping" | Usage, not incrementality |
| Salesforce | "Influenced sales" | Very broad, vendor-defined |

  None of these is a holdout-measured incremental profit.
- **Rufus normalization:** about $40 of incremental sales per user per year ([ppc.land](https://ppc.land/amazons-ai-shopping-assistant-drove-12-billion-in-sales-for-2025/)). At delivery-like margins of 3–5%, that would be only about $1–2 of profit per user per year. Food ordering has higher frequency but lower AOV, so its value pool skews to frequency and reorders, not basket size.
- **Own surface vs third-party surface:** own-surface agents (Rufus, Sparky in app) show positive engagement and AOV. Checkout delegated to a third-party agent showed lower conversion and more errors. This is consistent with Bain's 3x trust gap and supports keeping checkout and fulfillment in-house (Bain's advice).
- **China:** agent ordering habits can be bought quickly with subsidies plus a payment super-app (千问 + 支付宝 AI付). The unanswered commercial question is retention and ordering frequency *after* subsidies.

### Gaps
- No disclosed incremental GMV, conversion lift, or retention for Sparky, 千问 ordering, 京言, Uber Cart Assistant, or the Instacart ChatGPT app.
- No post-subsidy data for 千问.
- Amazon's Q3 2026 Rufus figures were not found (not yet indexed).
- Taobao AI shopping-guide statistics (six apps; +20 percentage points search relevance) and claims that leading Chinese e-commerce AI shopping guides are not yet monetized (no ad slots, no merchant fees) came only from search syntheses and are unverified.

---

## 5. Will consumers delegate food ordering to agents, and what do trust and failure-mode data show?

### Takeaway
Consumers accept agents for narrow, instructed, reversible tasks far more than for autonomous purchasing:
- 74% would delegate routine tasks such as reorders, but only 9% would let an agent initiate and complete purchases (Accenture).
- About 50% are wary of fully autonomous purchases (Bain).
- Groceries are the most delegable category at 41% (Checkout.com).
- 62% of US adults say they would order food through an LLM "given the right offer" (PAR, June 2026).

Accuracy is the binding constraint. Only 16% of Shanghai shoppers say AI shopping tools match products precisely, and Walmart saw wrong items in agent carts.

### Cited Findings
- **Accenture** (25,590 consumers, 16 countries): 74% would let an agent handle routine tasks; 32% would let it choose within parameters; 9% would let it initiate and complete purchases within boundaries — [AI News](https://www.artificialintelligence-news.com/news/ai-shopping-agents-consumer-trust-accenture-report/).
- **Bain (Nov 2025):** about 50% are cautious about fully autonomous purchases; trust in retailers' own agents is about 3x that in third-party agents — [Bain](https://www.bain.com/about/media-center/press-releases/20252/agentic-ai-poised-to-disrupt-retail-even-with-50-of-consumers-cautious-of-fully-autonomous-purchasesbain--company/).
- **Checkout.com:** 24% will never delegate purchases to AI; 27% trust no organization to run a shopping agent; delegation willingness is highest for groceries (41%) and household supplies (31%) and lowest for financial services (15%) — [Checkout.com](https://www.checkout.com/newsroom/consumer-demand-for-ai-shopping-is-forming-fast-but-trust-for-agentic-commerce-is-still-catching-up).
- **PYMNTS (Jan 2026, 2,299 US adults):** 49% of interested consumers would delegate both routine and large purchases — [PYMNTS](https://www.pymnts.com/?p=3398349).
- **PAR Technology 2026 report** (June survey of 1,000 US adults): 62% would order through an LLM given the right offer; nearly three in four diners are open to AI in the restaurant experience — [QSR Magazine](https://www.qsrmagazine.com/news/par-technology-releases-industry-report-on-ai-in-restaurants/) (vendor survey).
- **SoundHound:** 77% of monthly restaurant-goers expect an AI voice assistant to order their food within a few years — [FastCasual](https://www.fastcasual.com/news/soundhound-ai-survey-finds-nearly-8-in-10-diners-believe-ai-assistants-will-take-over-most-food-ordering-within-years/) (vendor survey).
- **Radial/Dynata:** only 6% have shopped with an AI agent; 19% would not trust AI with payment information; 54% are likely to use AI for monitored delivery and problem-solving — [Chain Store Age](https://chainstoreage.com/consumer-use-agentic-ai-shopping-assistants-lags-interest) (secondary).
- **Global Payments (2026-09-23):** consumers expect AI to make 15% of their purchases within five years — [Business Wire](https://secure.businesswire.com/news/home/20260923900333/en/Consumers-Expect-AI-to-Make-15-of-Their-Purchases-Within-Five-Years-Global-Payments-Research-Finds).
- **Scurri (UK):** 88% say there should always be an option to speak to a human — [Let's Data Science](https://letsdatascience.com/news/scurri-survey-finds-ai-delivery-trust-gap-72456afe).
- **上海市消保委, 2026 618 online-shopping survey** (published 2026-06-26, 4,308 valid responses):
  - 84.56% had tried AI shopping-selection features, but only 16.06% think AI matches products precisely.
  - 38.79% say AI ignores low-price needs and pushes pricier items; 29.71% get mixed high/low-price results.
  - Only 24.21% find algorithmic recommendations highly relevant; 58.33% get repeated pushes of items already viewed or carted.
  - Source: [东方财富](https://finance.eastmoney.com/a/202606263784809881.html).
- **Failure modes in production:**
  - Walmart's Instant Checkout produced wrong items in carts, forced one checkout per item, and missed accessory prompts — [ppc.land/WIRED](https://ppc.land/walmarts-chatgpt-checkout-flopped-heres-what-comes-next/).
  - Klarna partly reversed AI-only support over quality: its CEO said "cost unfortunately seems to have been a too predominant evaluation factor" — [Pure AI](https://pureai.com/Articles/2025/10/22/Why-Klarnas-AI-Experiment-Matters.aspx).
- **Payment safeguards in China:** 支付宝 AI付 requires manual account binding on first use and face, fingerprint or password verification at payment, with a "被盗即赔" (compensation if stolen) promise — [科技日报](https://www.stdaily.com/web/gdxw/2026-01/15/content_461939.html) (as summarized from the launch coverage).

### Inferences
- **Delegation ladder for food ordering:**
  - L0: recommend
  - L1: build a cart for one-tap confirmation
  - L2: rule-based reorder (my usual, under ¥X, before 12:00)
  - L3: autonomous within a budget and time window
  - L4: fully autonomous

  The 2026–27 commercial sweet spot is L1–L2. The P&L should assume a confirm step: it cuts error cost (3.G) but adds friction and lowers the incremental conversion achievable.
- **Trust favors own-app agents.** Bain's 3x trust gap and Walmart's experience imply that, in the near term, value capture stays with platforms that own the agent, checkout, guarantees and refunds. Third-party agents work better as discovery and new-customer channels.
- **Price-trust risk is real in China.** The 消保委 finding that 38.79% feel AI pushes pricier items, combined with the April 2026 pricing rules (Section 6), means an ordering agent optimized for take rate or ads will face both a trust problem and a regulatory one.

### Gaps
- No survey specific to delegating *food-delivery* ordering in China.
- No disclosed wrong-order or refund rate for any AI ordering agent (千问, Uber Cart Assistant, Instacart in ChatGPT).
- The PAR and SoundHound food-ordering numbers are vendor surveys.

---

## 6. What pitfalls do analysts highlight?

### Takeaway
Five pitfalls dominate:
1. **Ad cannibalization:** ads are roughly the whole segment profit pool.
2. **Loss of impulse and attach sales:** Walmart's missing HDMI cable.
3. **Liability for agent errors:** wrong carts and payment authority.
4. **Regulation:** China's April 2026 platform pricing rules and Dec 2025 delivery-platform standard; contested US law on agent access.
5. **The "agent tax" on third-party-agent traffic:** OpenAI's 4% precedent, Citrini's 7% scenario, and CICC's WeChat commission thesis.

### Cited Findings
- **Ads and loyalty layer:**
  - Patel's "DoorDash problem": user reviews, ads, loyalty programs and upsells would vanish because agents don't care about them — [Decoder](https://metacast.app/podcast/decoder-with-nilay-patel/HFyRnpbJ/the-doordash-problem-how-ai-browsers-are-a-huge-threat-to-amazon/x03xTQ5t).
  - Bain: about 65% of retail media is onsite and at risk — [Crowdfund Insider](https://www.crowdfundinsider.com/2025/11/255589-bain-co-report-details-how-agentic-ai-will-transform-e-commerce/).
  - Winters: discovery agents force marketplaces to pay less for traffic and lose LTV — [Casey Accidental](https://www.caseyaccidental.com/p/when-agents-attack-how-ai-collapses).
  - Walmart is testing ads in Sparky — [ppc.land](https://ppc.land/amazons-ai-shopping-assistant-drove-12-billion-in-sales-for-2025/).
  - Amazon's brand prompts in Rufus keep about 20% of engaged shoppers in a brand conversation, an agent-native ad surface — [Modern Retail](https://www.modernretail.co/technology/amazon-says-its-ai-shopping-assistant-is-gaining-traction-with-rufus-users-up-115/).
- **Impulse and attach loss:** Instant Checkout did not prompt accessories for high-consideration items, and single-item checkout clashed with multi-item baskets — [ppc.land/WIRED](https://ppc.land/walmarts-chatgpt-checkout-flopped-heres-what-comes-next/).
- **Liability and agent access:**
  - In March 2026, a federal judge in San Francisco granted Amazon a preliminary injunction blocking Perplexity's Comet agent from password-protected areas of Amazon — [GeekWire](https://www.geekwire.com/2026/judge-blocks-perplexitys-ai-bot-from-shopping-on-amazon-in-early-test-of-agentic-commerce/).
  - The Ninth Circuit later vacated it. It found that the *user*, not Perplexity, accesses Amazon under the CFAA, that Amazon had not shown irreparable harm, and that equities favor Perplexity. The case returns to the district court; the appellate date is unclear (~mid-2026) — [MediaPost](https://www.mediapost.com/publications/article/417007/appeals-court-sides-against-amazon-lifts-perplexi.html?edition=); [Sovereign Magazine](https://www.sovereignmagazine.com/article/ninth-circuit-perplexity-comet-amazon-cfaa-ruling).
- **China, 《互联网平台价格行为规则》:**
  - Issued Dec 2025 by NDRC, SAMR and CAC; effective 2026-04-10; 7 chapters, 29 articles.
  - Pushes disclosure of dynamic and differential pricing rules.
  - Bars platforms from unreasonably restricting merchants' pricing.
  - Prohibits below-cost dumping, "大数据杀熟" (charging existing customers more based on their data), price gouging, and forcing merchants to cut prices.
  - At the 2026-04-08 compliance meeting regulators stressed respecting merchants' pricing autonomy and banning forced promotions at merchants' expense.
  - Sources: [新华网](https://www.news.cn/tech/20260410/c5186b8634a74aa089bb632c7a4d6ed0/c.html); [中国日报](https://cn.chinadaily.com.cn/a/202512/22/WS69490b28a310942cc4997ebd.html); [同花顺](https://news.10jqka.com.cn/20260410/c675879176.shtml).
- **China, 《外卖平台服务管理基本要求》** (recommended national standard, Dec 2025):
  - Merchant vetting, with a "堂食" label for dine-in kitchens.
  - Rules simplifying fees and governing promotions.
  - Dispatch algorithms must weigh traffic and weather; no algorithm-forced overtime.
  - It is not legally binding, but major platforms pledged on 2025-12-04 to comply.
  - Sources: [第一财经](https://www.yicai.com/news/102948456.html); [界面](https://m.jiemian.com/article/13723870.html); [扬子晚报](https://www.yzwb.net/news/txs/202512/t20251204_296720.html).
- **Agent tax:**
  - OpenAI 4% Instant Checkout fee — [Shopifreaks](https://www.shopifreaks.com/shopify-confirms-a-4-fee-on-sales-made-through-chatgpt-but-whats-the-real-cost/).
  - Citrini's scenario of a 7% take vs ~25% — [Stocktwits](https://stocktwits.com/news-articles/markets/equity/doordash-moat-under-threat-citrini-research-warns-consumer-ai-agents-bypass-delivery-apps/cZRvn0mR4zK).
  - CICC: an agent entry point can raise WeChat's effective commission — [界面](https://m.jiemian.com/article/14842082.html).
- **Execution risk:**
  - Gartner: more than 40% of agentic AI projects canceled by end-2027 — [Maven AGI](https://mavenagi.com/resources/one-year-since-gartners-ai-resolution-prediction).
  - Klarna: unit support cost fell while total cost rose — [Pure AI](https://pureai.com/Articles/2025/10/22/Why-Klarnas-AI-Experiment-Matters.aspx).
  - Danker: "This idea that it will all become automated might be a little bit far-fetched" — [ppc.land/WIRED](https://ppc.land/walmarts-chatgpt-checkout-flopped-heres-what-comes-next/).
- **Volume reality check:** third-party-agent orders at a major US delivery platform were only "several hundred" in total (reported) — [Restaurant Business](https://www.restaurantbusinessonline.com/technology/why-doordash-isnt-afraid-ai-shopping-agents). GenAI is about 0.4% of retail visits — [Craftshift/Similarweb](https://craftshift.com/ai-referral-traffic-ecommerce-stats-2026/). The agent tax is a forward risk, not yet a P&L line.

### Inferences
- **Self-preferencing and "pay-to-be-chosen".** An own-app agent that ranks by ad bids or by the platform's margin creates three problems:
  1. A trust problem: it erodes Bain's 3x trust advantage.
  2. A regulatory problem in China, where the 2026 pricing rules require transparency of differential pricing and ban big-data price discrimination.
  3. An antitrust self-preferencing exposure, especially after DoorDash–Instacart and Uber–Delivery Hero consolidation.

  Practical design: label sponsored agent suggestions clearly, keep a "best for you" ranking separate from paid placements, and log rationales for audit.
- **Value capture in China must come from fees, memberships and labeled ads, not personalized pricing.** The April 2026 rules make agent-driven personalized pricing and dynamic-discount optimization legally risky.
- **Treat the agent tax as customer-acquisition cost.** Following Winters, pay channel fees only on first orders or new customers, and use account linking and membership benefits to pull repeat orders back to the own app. A perpetual 4% fee on repeat orders turns them negative at EBITDA-level margins (3.F).
- **Liability allocation is a business-model choice.** The options include the platform absorbing agent errors as a guarantee (a brand and trust asset), confirm-before-pay (支付宝's verification model), merchant-borne errors (unlikely when the agent erred), or insurance (支付宝's "被盗即赔"). Each shifts C_err in the model.

### Gaps
- No US or EU regulatory action specifically on agent self-preferencing in delivery was found.
- The DMA and EU AI Act implications for delivery agents were not researched.
- China's revised Anti-Unfair Competition Law (2025) and the algorithm-recommendation regulations were not verified in this pass.
- No analyst quantifies impulse or attach loss for food delivery specifically.
- Original Citrini note figures (7% vs 25%) were not checked against the source.

