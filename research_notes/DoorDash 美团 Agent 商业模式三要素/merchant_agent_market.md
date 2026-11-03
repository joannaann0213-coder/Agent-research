# Merchant-side AI agents for restaurants and local-service merchants (US, China, with SE Asia and India comparisons), as of 2026-11-03

*Scope: comparables and benchmarks for a scenario where a delivery platform (DoorDash, Meituan) sells or bundles agents to merchants. DoorDash's and Meituan's own merchant tools are covered by other researchers and appear here only briefly. Every fact is dated. "List" means a published price. "Reported" means a third-party or press figure. "Quote" means a negotiated price that is not public.*

---

## Q1. Product categories and leading vendors: prices, pricing units, adoption

### Takeaway
The US market has split into three layers:
- **SMB phone and voice agents.** Published list prices run from about $85 to $599 per location per month, or about $1.50 per completed order.
- **Enterprise drive-thru voice AI.** Sold to chains on quote only. Taco Bell has scaled it to about 900 stores (July 2026), McDonald's is restarting with Archy (September 2026), and Burger King is pulling back (September 2026).
- **POS-native agents.** Toast charges $499 a month for a marketing agent bundled with a human manager (May 2026). Square gives its Managerbot away free in open beta (April 2026).

Revenue-facing agents cluster around $400–$500 per location per month: Slang Core $399, Kea $450, Owner $499, Toast IQ Grow $499. Back-office tools are priced lower or given away. In China, restaurant SaaS agents (客如云) and platform agents are offered free.

### Cited Findings

#### A. Price benchmark table (merchant agents and adjacent restaurant software)

| Vendor / product | Category | Pricing unit | Price and date | List / reported / quote | Source |
|---|---|---|---|---|---|
| Slang.ai Core / Premium | Phone AI (reservations, call deflection) | per location / month | from $399 / from $599 (as of Sept 2026) | List | [Katalyst, 2026-09-12](https://www.katalystos.com/blog/ai-phone-answering-for-restaurants-what-it-costs) |
| Maple Voice / Pro | Phone AI | per location / month | Voice $85 (annual) or $150 (monthly); Pro $220 (annual) or $350 (monthly) (Sept 2026) | List | [Katalyst](https://www.katalystos.com/blog/ai-phone-answering-for-restaurants-what-it-costs) |
| Kea | Phone ordering AI | flat / month | $450, unlimited calls (vendor's own blog, as cited Sept 2026). One third-party review lists $199–$299, which conflicts. | List (vendor blog); conflicting reported figure | [Katalyst](https://www.katalystos.com/blog/ai-phone-answering-for-restaurants-what-it-costs); conflict noted in [2026 roundups](https://www.cloudtalk.io/blog/best-ai-phone-answering-systems-for-restaurants/) |
| Bite Buddy | Phone ordering AI | per completed order | $1.50 per order (2026) | List | [Katalyst](https://www.katalystos.com/blog/ai-phone-answering-for-restaurants-what-it-costs) |
| Loman, SoundHound, Voiceplug | Phone/voice AI | quote | not published (Sept 2026) | Quote | [Katalyst](https://www.katalystos.com/blog/ai-phone-answering-for-restaurants-what-it-costs) |
| Toast IQ Grow (AI Marketing Agent plus human Marketing Success Manager) | POS-native marketing agent | flat / month (bundle) | $499 / month (launched 2026-05-05) | List | [Yahoo Finance, May 2026](https://finance.yahoo.com/markets/stocks/articles/toast-tost-launches-toast-iq-132858023.html); [Pulse2](https://pulse2.com/toast-iq-grow) |
| Square Managerbot | POS-native operations agent | included | $0 extra during open beta (2026-04-28) | List (free beta) | [Square press release](https://squareup.com/us/en/press/managerbot-open-beta) |
| Owner.com Flexible / Flat Rate | AI website, ordering, marketing | subscription plus % of order | Flexible $249/mo + 5% restaurant fee per order; Flat $499/mo with no restaurant fee; guests pay a 5% "order support fee" on both plans; "multi-location special rates" (accessed Nov 2026) | List (multi-location rates negotiated) | [owner.com/pricing](https://www.owner.com/pricing) |
| 7shifts | Labor scheduling | per location / month | $0–$134.99 across 4 tiers. The top tier rose from $76.99 to $134.99 (+75.3%) on 2026-04-19, so the $76.99 figure is superseded. | List (tracked by third party) | [Costbench, Apr 2026](https://costbench.com/changelog/7shifts-price-increase-2026-04/); [Capterra](https://www.capterra.com/p/123038/7shifts-Restaurant-Scheduling/pricing/) |
| MarginEdge | Inventory, invoices, back office | per month | about $300 (G2) to $330 (Capterra); MarginEdge + Freepour $480 (2026) | Reported, conflicting | [Capterra](https://www.capterra.com/p/187718/MarginEdge/); [G2 comparison](https://www.g2.com/compare/7shifts-vs-marginedge) |
| Drive-thru voice AI (Presto, SoundHound, Omilia, Hi Auto, Google, IBM) | Enterprise voice | negotiated | no public pricing found | Quote | see section C |

#### B. AI phone and voice ordering and reservations (point solutions)
- **Slang.ai is a reservations and call-deflection tool, not a POS order-taker.** For order calls it texts the caller a link to the restaurant's online ordering page. Its listed integrations are reservation and CRM tools. The same article calculates break-evens: a $450 flat plan beats $1.50 per order above about 300 AI-taken orders a month, and Maple Pro (annual) breaks even at about 147 orders. — [Katalyst, 2026-09-12](https://www.katalystos.com/blog/ai-phone-answering-for-restaurants-what-it-costs). The author works for a POS company that does not sell phone AI.
- **SoundHound Q2 2026 (8-K dated 2026-08-06).**
  - Results: revenue $61.9M (+45% YoY); non-GAAP gross margin 58.4%; FY2026 revenue outlook raised to $230–$260M.
  - New restaurant wins: Ruby Tuesday (Smart Answering + Smart Ordering); a large seafood QSR adopting its drive-thru ordering; an unnamed "American-style Mexican" QSR; a music-themed sushi chain.
  - Expansions: Five Guys, IHOP, Jersey Mike's, and a pizza brand with SoundHound live in more than 75% of its locations.
  - Renewals: Habit Burger, Red Lobster, Torchy's Tacos. Lazy Dog renewed and expanded.
  - Not disclosed: pricing, monetization unit, restaurant revenue share.
  - The LivePerson acquisition is expected to close before end-2026.
  — [SoundHound 8-K Q2 2026](https://www.sec.gov/Archives/edgar/data/0001840856/000184085626000019/soun-20260806ex991earnings.htm)
- **SoundHound scale claims.** The company says it powers more than 15,000 locations with voice AI (company claim, relayed by Zacks in 2026) — [Globe and Mail/Zacks](https://www.theglobeandmail.com/investing/markets/stocks/SOUN/pressreleases/3409580/can-soundhound-disrupt-restaurant-automation-in-2026-and-beyond/). Q2 2026 coverage cites 100% renewal rates for key restaurant accounts (company-sourced) — [Yahoo/Zacks](https://finance.yahoo.com/technology/ai/articles/soundhound-adds-3-restaurant-chains-165000162.html).
- **SoundHound Q3 2026 timing.** Results are due after market close on 2026-11-05. As of 2026-11-03 there are no Q3 restaurant figures. — [TipRanks](https://www.tipranks.com/news/soundhound-ai-stock-faces-q3-earnings-test-as-analysts-forecast-45-revenue-growth); [Tech-Insider](https://tech-insider.org/ca/soundhound-ai-q3-earnings-500m-revenue-target-2026/)
- **SoundHound's restaurant roll-up:**
  - SYNQ3: about $25M, closed 2024-01-03, brought more than 10,000 restaurant locations — [PrivSource](https://www.privsource.com/acquisitions/deal/B6SbAO)
  - Allset: key assets, June 2024, about 7,000 restaurant partners — [PrivSource](https://www.privsource.com/acquisitions/deal/plSmMv)
  - Amelia: August 2024, about $80M (Wikipedia says $85M; the sources conflict) — [PrivSource](https://www.privsource.com/acquisitions/deal/9zSzg4); [Wikipedia](https://en.wikipedia.org/wiki/SoundHound_AI)
  - Interactions: 2025-09-09, about $60M cash plus milestones — [PrivSource](https://www.privsource.com/acquisitions/deal/x7S8mv)
- **Presto.**
  - Raised $10M led by Metropolitan Partners Group (2026-01-08). Says "hundreds of drive-thrus" are live with Presto Voice and calls itself the AI partner to about a dozen enterprise brands. — [Business Wire, 2026-01-08](https://www.businesswire.com/news/home/20260108472215/en)
  - Launched a phone-ordering business unit in April 2025 — [Presto](https://presto.com/presto-launches-phone-ordering-business-unit-expanding-its-enterprise-grade-suite-of-voice-ai-for-qsrs/). ElevenLabs voice alliance in September 2025 — [Speech Technology](https://www.speechtechmag.com/Articles/News/Speech-Technology-News/Presto-Partners-with-ElevenLabs-for-Voice-AI-for-Restaurant-Drive-thrus-171510.aspx)
  - Earlier history (superseded but relevant to trust): Presto's 2023 SEC filings said more than 70% of orders at advanced pilot sites needed off-site human help, and the SEC charged Presto in January 2025 over misleading AI claims. — [Katalyst](https://www.katalystos.com/blog/ai-phone-answering-for-restaurants-what-it-costs)
- **ConverseNow.** Total funding is $18.3M per Craft and $28.15M over 5 rounds per CB Insights (the sources conflict). It acquired Valyant AI on 2024-07-09. No 2026 funding or M&A news was found. — [Craft](https://craft.co/conversenow); [CB Insights](https://www.cbinsights.com/company/conversenow/financials)

#### C. Drive-thru voice AI (enterprise; all quote-based)
- **Taco Bell / Yum.** Voice AI runs at more than 890 US restaurants in 38 states (reported 2026-07-08). It uses Omilia (partner since 2023) on the Byte by Yum platform. Omilia says AI transactions are "on par with, and in some cases faster than" human order-taking. AI stores report higher employee retention (company-reported, no figures). — [Restaurant Technology News, 2026-07-08](https://restauranttechnologynews.com/2026/07/taco-bell-expands-drive-thru-voice-ai-to-nearly-900-restaurants-across-the-u-s-as-automation-gains-momentum/); [NRN](https://www.nrn.com/quick-service/taco-bell-s-drive-thru-voice-ai-expands-to-nearly-900-restaurants)
  - Earlier episode, now superseded: in late August 2025 Taco Bell told the WSJ it was "rethinking" voice AI after a viral prank order for 18,000 cups of water. AI had then taken more than 2M orders at 500+ locations. CDTO Dane Mathews said humans would stay "in the loop," especially at peak times. — [WSJ via iTiger](https://www.itiger.com/news/2562303742); [Computing](https://www.computing.co.uk/news/2025/ai/taco-bell-scales-back-ai-tests-after-customer-complaints); [AOL](https://www.aol.com/2-million-ai-orders-taco-213600853.html). The July 2026 expansion shows the 2025 pause did not end the program.
- **McDonald's.**
  - The IBM drive-thru test (started 2021) ended in 2024 over accuracy problems — [Restaurant Dive, 2024](https://www.restaurantdive.com/news/mcdonalds-ibm-drive-thru-automation-voice-ordering-ai/719085/)
  - At its 2026-09-25 Investor Day, McDonald's announced **ArchIQ**, an AI restaurant operating system, and **Archy**, a drive-thru voice AI in English and Spanish with accuracy above 90%. Targets: Archy frees at least 50 labor hours per week per restaurant as it scales; Bluetooth inventory tags save about 5 hours a week and cut food waste 15%.
  - Google Edge technology is already in thousands of restaurants across 7 markets and comes to the US in 2027. Rollout is "conditions based."
  — [Restaurant Dive, Sept 2026](https://www.restaurantdive.com/news/mcdonalds-archiq-drive-thru-artificial-intelligence-next-strategy/831256/)
  - McDonald's separately disputed reports that it uses AI to set prices — [Restaurant Dive](https://www.restaurantdive.com/news/mcdonalds-disputes-dynamic-pricing-artificial-intelligence-menu-claims/832096/)
- **Burger King.** As of 2026-09-24 it is testing voice AI in about 50–70 drive-thrus with Google and others. An earlier report of 1,500 locations was corrected. VP Chakri Somisetti reported "drive offs because guests don't want to talk to a bot," and BK is building a way for guests to choose a human. BK has spent about $400M on technology over several years. Its "Patty" headset assistant coaches back-of-house crew. — [NRN/Restaurant Business, 2026-09-24](https://www.nrn.com/quick-service/burger-king-rethinks-its-drive-thru-ai-strategy)
- **Wendy's FreshAI (Google Cloud).**
  - Launched in Columbus in 2023; in 36 company stores by October 2024; service was 22 seconds faster than the regional average in the first test — [Fortune, 2024-10-15](https://dc.fortune.com/2024/10/15/wendy-google-ai-drive-thru-expansion)
  - In 2025 Wendy's said it would expand to roughly 500–600 stores (secondary source) — [CDO Times, 2026-01-09](https://cdotimes.com/2026/01/09/wendys-vs-mcdonalds-the-ai-drive-thru-reckoning/)
  - The QSR 2026 Drive-Thru Report's mystery shops found voice AI in 13% of Wendy's visits in 2026, versus 12% in 2025 — [QSR Magazine 2026 Drive-Thru Report](https://www.qsrmagazine.com/story/the-2026-qsr-drive-thru-report/). (Search summary only; the page returned 403 to direct fetch.)
- **Other chains.** Bojangles is expanding its Hi Auto–based AI ("Bo-Linda") to all of its roughly 900 restaurants (date not verified) — [NRN](https://www.nrn.com/quick-service/bojangles-expands-its-drive-thru-ai-system-bo-linda). White Castle (SoundHound) planned 100+ AI lanes; Presto serves Dairy Queen and Taco John's — [RTN, July 2026](https://restauranttechnologynews.com/2026/07/taco-bell-expands-drive-thru-voice-ai-to-nearly-900-restaurants-across-the-u-s-as-automation-gains-momentum/)
- **Human intervention rates (low confidence).** A trade blog cites independent mystery-shopper data: employee intervention at 30% of Taco Bell AI orders, 33% at Wendy's, and 3% at Bojangles (Hi Auto) — [The Hospitality Hangout, 2026](https://www.thehospitalityhangout.com/blog/drive-thru-voice-ai-hits-900-restaurants-what-operators-need-to-know-in-2026/)

#### D. POS and back-office agents
- **Toast (Q2 2026 call, 2026-08-11).**
  - Scale: about 180,000 locations (+22% YoY), a record 9,500 net adds, total ARR $2.4B (+25%), SaaS ARR $1.2B (+27%), payments ARR $1.2B (+23%), mid-single-digit SaaS ARPU growth.
  - CEO Aman Narang on Toast IQ Grow: "the fastest-growing product we have ever launched," "on track to be the fastest product to $10 million ARR," and incremental to existing software. It draws on data from 150,000+ customers, and "we've got humans reviewing and approving those workflows." Gross margins are already improving and the business "is running positive margins."
  - Customer example: Spirits Food & Friends cut monthly agency spend 70% and generated more than $100K of marketing-attributed sales in under 2 months.
  - Roadmap: agents for voice (phone and drive-thru), scheduling, payroll and tax, inventory, and bookkeeping.
  - Narang: restaurants spend "a multiple of what they spend on software" on outsourced services such as marketing and payroll.
  - Not disclosed: pricing of future agents, attach rates, Grow's ARR.
  — [Motley Fool transcript](https://www.fool.com/earnings/call-transcripts/2026/08/11/toast-tost-q2-2026-earnings-call-transcript/) (auto-transcribed and may contain errors)
- **Toast IQ Grow (launched 2026-05-05).**
  - Package: an AI Marketing Agent (beta) plus a dedicated human Marketing Success Manager for $499 a month. It builds audiences and runs campaigns across email, SMS, social and paid ads.
  - Pilot result (company-reported, not independently verified): restaurants with a Success Manager saw more than 8% average sales growth versus similar Toast restaurants.
  - One competing agency judges it a fit for restaurants with at least $10K in monthly sales (low weight).
  — [Yahoo Finance](https://finance.yahoo.com/markets/stocks/articles/toast-tost-launches-toast-iq-132858023.html); [Pulse2](https://pulse2.com/toast-iq-grow); [Toast newsroom](https://pos.toasttab.com/news/toast-debuts-toast-iq-grow-spring-release-2026)
- **Toast IQ Team.** A labor suite headed by a Scheduling Agent. Beta users report 41% fewer overtime hours per employee. Pricing was not found. — [The Pass Brief](https://thepassbrief.com/articles/toast-rolls-out-new-restaurant-ai-agents-led-by-labor-tool-tied-to-41-overtime-c-e9744bcb); [Yahoo Finance](https://finance.yahoo.com/technology/ai/articles/toast-ai-strategy-agents-drive-164600798.html)
- **Square Managerbot (open beta 2026-04-28; free during beta).**
  - Available to most non-franchise US food and beverage, retail, and health and beauty sellers.
  - What it does: monitors sales and labor percentage by location, flags likely inventory shortages and updates item availability by voice or text, drafts schedules from projected sales, drafts marketing campaigns, and fixes catalog issues. In Square's words: "Managerbot proposes an action, and sellers approve."
  - Usage: restaurant operators are "among the most active users," and "thousands" of sellers have used it.
  - No statement on future pricing; Block declined to say whether it will become a paid add-on.
  — [Square press release](https://squareup.com/us/en/press/managerbot-open-beta); [PYMNTS](https://www.pymnts.com/?p=3689889)
- **Square agentic ordering (2026-07-01).** A ChatGPT app and a Claude plugin let consumers order from US food and beverage sellers that use Square Online Ordering. Sellers are opted in automatically, and no marketplace commission applies. Normal online processing fees still apply (reported as 2.9% or 3.3% + $0.30 depending on plan). — [VentureBeat](https://venturebeat.com/marketing-tech/restaurants-can-now-accept-orders-placed-directly-from-chatgpt-and-claude-thanks-to-squares-new-low-fee-no-setup-integration/); [Digital Applied, 2026-07-06](https://www.digitalapplied.com/blog/square-chatgpt-claude-ordering-agentic-commerce-2026)
- **Clover (Fiserv).** No merchant AI agent was found in 2026 coverage; only Clover GPV growth (+12% YoY) was reported — [search result: Digital Transactions](https://www.digitaltransactions.net/category/news/competitive-strategies/page/13/?ap=k)

#### E. Marketing, CRM, website, review, labor and inventory
- **Owner.com** (pricing page accessed Nov 2026).
  - Plans: $249/mo + 5% per order, or $499/mo flat (positioned for restaurants with $5K+ a month in online sales). Guests pay a 5% order-support fee on both plans. No contracts.
  - Includes: AI website, online ordering, branded app, automated SEO pages, loyalty, and AI email/SMS marketing. "AI Phone Ordering" is listed only as a waitlist, and a "Reviews Engine" appears in the site navigation.
  - Self-reported results: +20% SEO traffic in 30 days; app users reorder about 2x more often.
  — [owner.com/pricing](https://www.owner.com/pricing)
- **Uber Eats** offers merchants AI review summaries, auto-written menu descriptions, and food-photo enhancement — [Uber newsroom](https://www.uber.com/us/en/newsroom/merchant-product/)
- 7shifts and MarginEdge prices are in the table above. No verified 2026 prices were found for Popmenu, Bikky or SevenRooms (see Gaps).

#### F. AI customer-support platforms that food platforms sell to other businesses
- **Eternal (Zomato) "Nugget."**
  - Launched by Zomato Labs on 2025-02-17 as a no-code AI customer-support platform for other businesses. It was built in-house and handles more than 15M interactions a month for Zomato, Blinkit and Hyperpure.
  - At launch, Deepinder Goyal claimed it resolves up to 80% of queries autonomously.
  - Pricing is not public. It was offered free to founders still under contract with legacy providers. The company says 90% of companies that tested it signed up.
  — [Business Standard, 2025-02-17](https://www.business-standard.com/companies/news/zomato-launches-ai-customer-support-platform-nugget-125021700734_1.html); [ETV Bharat](https://www.etvbharat.com/en/!technology/zomato-labs-introduces-nugget-an-ai-driven-customer-support-platform-for-businesses-enn25021802687); [Digit](https://www.digit.in/news/general/zomato-introduces-nugget-an-ai-powered-customer-support-platform-for-businesses-how-it-works.html)
  - Vendor case study (as of January 2026): more than 15M conversations a month, 85% average resolution, human-agent productivity up 40%, and Zomato support costs down 55% (headline: "$11M"). Vendor-published. — [MongoDB blog](https://www.mongodb.com/company/blog/innovation/zomato-ai-solution-nugget-reduces-support-costs)
- **Swiggy** opened its "AI Commerce Stack" to external developers and enterprises on 2026-04-29 (single secondary report) — [Lapaas Voice](https://voice.lapaas.com/swiggy-opens-its-ai-commerce-stack-to-external-developers/?amp=1)

#### G. China restaurant SaaS agents
- **客如云 (Keruyun).**
  - Launched five AI agents in 2025: 老板助手 (decision support), 营销助手 (marketing), 超级店员 (staff collaboration), 超级IT (system stability), and 智能小On (unified entry point). It said it would actively promote **free use** of all five from early 2026.
  - Monetization still comes from SaaS, hardware (the POS 5N, co-developed with Alipay's 碰一下 tap-to-pay) and services.
  - Claims 1.81M merchants' operating data used to train its models and 1,200+ service providers.
  - Source is sponsored content dated 2026-03-26.
  — [Sina Finance (sponsored), 2026-03-26](https://finance.sina.com.cn/jjxw/2026-03-26/doc-inhshnxa6264750.shtml)
- **哗啦啦 (Hualala).** No AI-agent announcement was found. The only fee information is an unverified personal Zhihu claim: an account-opening fee, a monthly management fee, and about a 0.38% withdrawal fee. — [Zhihu answer](https://www.zhihu.com/question/263597074/answer/2897478762)

### Inferences
- **Pricing units observed (Nov 2026):**
  1. Per location per month for SMB phone AI and labor and inventory tools ($0–$599).
  2. Per completed order ($1.50, Bite Buddy).
  3. Subscription plus % of order (Owner: $249 + 5%).
  4. A flat bundle of agent plus human service (Toast IQ Grow, $499), which is effectively a cheap agency retainer.
  5. Free, bundled into POS or payments (Square Managerbot, 客如云).
  6. Negotiated enterprise contracts (drive-thru).

  Seat-based pricing is effectively absent at the merchant level. The "seat" unit is the location.
- **The SMB price anchor for revenue-generating agents is about $400–$500 per location per month.** Slang Core is $399, Kea $450, Owner Flat $499 and Toast IQ Grow $499, so Toast's price looks set to match Owner's. A platform-sold agent priced above this band would compete with POS-bundled and free offers.
- **Drive-thru is a separate, enterprise-only market.** Big chains build on their own platforms (Byte by Yum, McDonald's ArchIQ with Google), so delivery platforms have no natural role there.
- **Through the 价值主张 / 价值创造与传递 / 价值获取 lens:**
  - 价值主张: point solutions sell "answer every call / capture missed orders." POS vendors sell "an agent that runs your back office and marketing."
  - 价值创造与传递: POS vendors deliver through existing workflow and data, with human approval in the loop (Toast, Square).
  - 价值获取: SaaS fees (Toast, point solutions) or indirect capture through payments and retention (Square, 客如云).

### Gaps
- No public pricing was found for SoundHound, ConverseNow, Presto, Hi Auto, Omilia, Hostie, Popmenu (including AI Answering), Bikky or SevenRooms (SevenRooms is now DoorDash-owned; see the DoorDash researcher). Third-party claims that Loman costs about $199/mo plus a $149 setup fee could not be verified.
- The Yum–Nvidia partnership (announced March 2025) was not verified in this pass. July 2026 Taco Bell coverage names Omilia and Byte by Yum, not Nvidia.
- No 2026 store count was found for Wendy's FreshAI, only the 13% mystery-shop incidence. One claim that FreshAI handles "86% of orders autonomously" had no source and was excluded.
- Not disclosed or not found: Toast IQ Team pricing, Toast IQ Grow attach rate and ARR, SoundHound's restaurant revenue share, and SoundHound's per-location pricing.
- No Clover merchant AI agent was found.
- A Popmenu AI Answering case (Next Level Brands: 58,457 calls, 44% revenue-related, 1,184 staff hours saved) appeared only through aggregators and could not be traced to a primary page.
- Not researched or not found: 美团管家 (Meituan's own restaurant SaaS); 天财商龙 and 二维火 AI features; and 客如云's ownership (reportedly Alibaba-controlled since 2019, not verified here).

---

## Q2. Delivery-platform merchant AI tools (brief comparison)

### Takeaway
Every marketplace checked gives merchant AI away free, as part of its commission and ads economics:
- Uber Eats: menu, photo and review AI.
- Grab: AI Merchant Assistant.
- Swiggy and Zomato: dish analytics and partner-app bots.
- Meituan: 袋鼠参谋 (2025) and the free "AI店长" (October 2026).
- 淘宝闪购: AI经营管家.
- Douyin: free AI tools.

None of them sells a standalone paid merchant-agent subscription. The clearest monetization signal is Douyin's extra 4–13% technical fee on orders referred by its AI assistant Doubao in some categories, which is a take-rate surcharge on an AI channel rather than a tool fee.

### Cited Findings
- **Uber Eats (US).**
  - Merchant AI covers review summaries, menu descriptions and photo enhancement — [Uber newsroom](https://www.uber.com/us/en/newsroom/merchant-product/). Consumer-side Cart Assistant launched 2026-02-11 — [TechCrunch](https://techcrunch.com/2026/02/11/uber-eats-launches-ai-assistant-to-help-with-grocery-cart-creation/).
  - US pricing page (accessed Nov 2026):

    | Plan | Delivery commission | Pickup |
    |---|---|---|
    | Lite | 20% (15% in select cities such as Portland and NYC) | 7% (10% without validated in-store pricing) |
    | Plus | 25% (+5% on Uber One orders) | 7% / 10% |
    | Premium | 30% (includes ads-credit match up to $100/mo) | 7% / 10% |
    | Self-delivery | 15% | 7% / 10% |

    Webshop orders cost 2.5% + $0.29. No AI features appear on the pricing page. — [Uber Eats merchant pricing](https://merchants.ubereats.com/us/en/pricing/)
  - **Superseded-information flag:** the common "Uber Eats 15/25/30%" shorthand is out of date for Lite in most US cities. The current page shows 20%.
- **DoorDash (US; brief, covered elsewhere).** Basic 15%, Plus 25%, Premier 30% on delivery; 6% on pickup. Card processing is included in the commission. Online Ordering is commission-free on all plans. Plans apply to restaurants with 75 or fewer locations. No AI tools are mentioned on the pricing page (accessed Nov 2026). — [DoorDash merchant pricing](https://merchants.doordash.com/en-us/pricing)
- **Grab (SE Asia).**
  - AI Merchant Assistant launched April 2025 inside the GrabMerchant app, built with OpenAI and Anthropic models. It gives sales-improvement suggestions, builds ad campaigns, updates menus, and is planned to suggest GrabFin financing. — [Grab press, April 2025](https://www.grab.com/sg/press/others/grab-deploys-agentic-ai-to-empower-merchants-and-driver-partners/)
  - Grab Indonesia reports more than 1M merchant interactions in under 5 months; Menu Scanner is the most-used feature (report date not confirmed) — [Marketing-Interactive](https://www.marketing-interactive.com/grab-s-ai-assistant-surpasses-1m-merchant-interactions-in-under-five-months)
  - Claude was chosen for the conversational layer — [Anthropic customer story](https://claude.com/customers/grab)
  - The 2026 foodpanda Taiwan deal promises the AI Merchant Assistant to Taiwan merchants — [Grab 6-K, 2026](https://www.sec.gov/Archives/edgar/data/1855612/000185561226000051/foodpandtaiwan-finalannoun.htm)
- **India.**
  - Swiggy's food marketplace CEO says generative-AI tools now give restaurants dish-level performance data. An AI bot for the restaurant-owner app (onboarding, ratings, payouts) is described as in progress. — [YourStory, January 2026](https://yourstory.com/2026/01/swiggy-genai-streamline-operations-india-food-delivery-market-expands)
  - Swiggy has taken consumer orders via ChatGPT, Claude and Gemini since late January 2026 — [YourStory](https://yourstory.com/2026/01/swiggy-lets-users-order-through-chatgpt-claude-gemini)
  - Zomato offers a merchant business dashboard. Nugget is covered in Q1(F).
- **Meituan (brief).**
  - **袋鼠参谋** launched October 2025: an AI decision tool covering category analysis, site selection, dish R&D and financial analysis, free to all merchants — [Sina, 2025-10-16](https://client.sina.com.cn/news/2025-10-16/doc-infucfih9409659.shtml); [Meituan](https://www.meituan.com/news/NN251017149001531)
  - On 2026-10-22, Meituan Waimai GM 薛冰 announced **妙手外卖版·袋鼠管家 ("AI店长")**, free to all catering merchants nationwide. It covers business diagnosis, store inspection, product operations and new-store operations.
  - Users: "tens of thousands" of SMB merchants plus 200+ chains, including 古茗 and 兰湘子.
  - Trial results: a Hebei chicken-pot restaurant's weekly orders rose more than 30%; 益禾堂's new-store first-month output rose 25%.
  — [Leiphone, 2026-10-23](https://www.leiphone.com/category/industrynews/tZI7pYBhiFduVfr6.html); [CNR, 2026-10-23](https://www.cnr.cn/mspd/jrhm/20261023/t20261023_527850850.shtml)
  - **Critique (2026-10-29), "免费的才是最贵的":**
    - The platform recovers the cost through commission and promotion fees.
    - The data are produced, held and interpreted by the platform.
    - Platform targets (GMV, order density) diverge from merchant profit.
    - It cites cases of automated repricing by AI or 代运营 agencies: in Shenzhen, 15 dishes were repriced to ¥28.81 in the same second and the owner lost ¥2–3K in an afternoon; a Foshan stew shop lost ¥8–10 per order.
    — [OFweek 维科号](https://mp.ofweek.com/Internet/a056714478607)
- **淘宝闪购 / Ele.me (Alibaba).**
  - April 2026: the store assistant moved from passive Q&A to proactive diagnosis and accepts voice commands, e.g. "帮我下架这个套餐" (one-sentence delisting) and one-sentence invoicing — [Sina, 2026-04-09](https://finance.sina.cn/tech/2026-04-09/detail-inhtwhrf6008583.d.html?vt=4)
  - October 30, 2026 conference: "AI经营管家" (single-store diagnosis), "连锁餐饮智能系统" (chain site selection, product innovation, multi-store management) and a "全栈AI经营智能体." Pricing not stated.
  - Results: 外婆家's profit rose 14% after AI site and product selection; in Q1 2026, orders per store doubled YoY.
  — [21世纪经济报道, 2026-11-03](https://m.21jingji.com/article/20261103/herald/b56e37691b92f337f4a4d22c09b29386.html)
- **Douyin / 抖音来客.**
  - Around Double 11 2026, Douyin e-commerce launched a "豆包运营" dashboard and free AI tools (开店托管 store hosting, 分销管家 distribution manager) for small merchants.
  - **Since 2026-08-10, merchants in accommodation, fitness, home, education and maternity categories pay an extra 4%–13% technical service fee on orders referred by Doubao.**
  — [人人都是产品经理/电厂, 2026-10-28](https://www.woshipm.com/ai/6485093.html)
  - Douyin life services launched the LIFE+ lead-generation system on 2026-09-10. AI-feature penetration among lead-gen merchants reached 72% in the three months to August 2026. — [CNFIN, 2026-09-11](https://www.cnfin.com/cmjj-lb/detail/20260911/4469014_1.html)
- **Merchant reception in China.**
  - Some merchants turned off Taobao and JD AI customer service ("礼貌有余、能力不足"). One said that if a tool isn't useful they won't use it even free, and would use it even less if it cost money. Others build their own tools but cannot get API access, and plugin scraping risks risk-control penalties. — [woshipm, 2026-10-28](https://www.woshipm.com/ai/6485093.html)
  - Mid-2025 commentary called local-life platform AI "鸡肋" (marginal) — [Sina, 2025-07-03](https://finance.sina.cn/stock/jdts/2025-07-03/detail-infeekxk4889857.d.html?vt=4)

### Inferences
- **Marketplaces treat merchant AI as a free retention and performance layer and monetize it through take rate.** That means commission tiers (Uber and DoorDash Plus/Premium with bundled ads credits), promotions and ads, and now AI-channel surcharges (Douyin's 4–13%).
- **Meituan's free AI店长 and 袋鼠参谋 set a zero-price anchor in China.** 淘宝闪购 and 客如云 point the same way. A Meituan paid agent subscription would face this anchor plus active distrust of platform-aligned advice (the OFweek critique).
- **In the US, a DoorDash agent sold as a paid SKU would face a different anchor.** Third-party phone agents cost $85–$599 a month, and Square bundles a free operations agent. US marketplaces bundle "free" AI the same way, but US merchants already pay point-solution prices.

### Gaps
- Grubhub's merchant AI was not researched. Its 2025 acquisition by Wonder was not verified in this pass.
- An Eternal–OpenAI collaboration to put AI assistants in merchant partner portals appeared in search summaries but could not be tied to a dated primary source.
- No 2026 information was found on Ele.me's earlier "AI助手" beyond the 淘宝闪购 rebrand.
- No usage or adoption metrics were found for Uber Eats merchant AI. No 2026 Grab merchant-assistant revenue impact was found.

---

## Q3. Willingness to pay and ROI, with restaurant margin and commission context

### Takeaway
Adoption is broad but shallow, and margins are thin:
- NRA 2026: 26% of operators use AI, and only 6% use it for customer orders.
- Median pre-tax margin: 2.8% for full-service and 4.0% for limited-service (FY2024). 42% of operators were unprofitable in the past year.
- Nearly all ROI evidence is vendor-reported.

Willingness to pay shows up where an agent replaces an existing paid line: agency fees (Toast IQ Grow), phone labor and missed orders (phone AI), or marketplace commission (direct-ordering agents). It does not show up as a new "AI budget." Commissions dwarf agent fees. US marketplaces take 15–30% of delivery orders. Meituan's nominal 6–8% becomes roughly 20–38% all-in on worked examples once fulfillment fees and merchant-funded discounts are counted.

### Cited Findings

#### Surveys and adoption
- **NRA State of the Restaurant Industry 2026 (reported 2026-02-18).**
  - 26% of operators use AI tools.
  - Marketing is the top use: 19% of full-service and 15% of limited-service operators. 10% use AI for administrative tasks, and 6% for customer orders.
  - 60% plan to invest more in customer-experience technology. More than half plan front-of-house productivity investments, and just under half back-of-house.
  - 12% call themselves "leading edge," 60% in line with competitors, and 28% lagging.
  - About 6 in 10 millennial and Gen Z adults would order from an AI bot.
  — [Restaurant Dive, 2026-02-18](https://www.restaurantdive.com/news/national-restaurant-assocation-operator-artificial-intelligence-adoption/812418/)
- **Profitability context (NRA, via a secondary compiler, updated 2026-10-03).**
  - Median pre-tax income for FY2024 was 2.8% of sales for full-service and 4.0% for limited-service (Restaurant Operations Data Abstract, 900+ operators).
  - Labor is 36.5% of sales for full-service and 31.7% for limited-service; food and beverage is about 32%.
  - 42% of operators were not profitable in the past year, up from 29%.
  - NRA 2025 research: 69% of tech-adopting operators report efficiency gains, but only 28% say tech improved profitability.
  - The primary source is paywalled and these figures were not verified directly.
  — [Duck-Hub compilation](https://www.duck-hub.com/blog/restaurant-profit-margin-statistics)
  - Outlier: TouchBistro's 2026 report claims independent operators reported "double-digit" margins. The method is unclear. — [Business Wire, 2026-01-27](https://www.businesswire.com/news/home/20260127769924/en/U.S.-Restaurants-Overcome-Financial-Strain-TouchBistros-2026-American-State-of-Restaurants-Report-Reveals-Double-Digit-Profit-Margins-and-Tech-Driven-Resilience)
- **Consumer acceptance.** Technomic finds 23% of consumers find AI drive-thru ordering appealing and 46% find it unappealing. 46% find ordering from an in-restaurant employee appealing. — [NRN/Restaurant Business, 2026-09-24](https://www.nrn.com/quick-service/burger-king-rethinks-its-drive-thru-ai-strategy)
- **Phone-demand statistics are almost all vendor-sourced** — [Katalyst, 2026-09-12](https://www.katalystos.com/blog/ai-phone-answering-for-restaurants-what-it-costs):
  - "43% of calls unanswered" (Breez, 2025; method not found)
  - "60% missed at peak" (Slang, proprietary data)
  - "40.1% missed in quick service" (Revmo, August 2026, 12,091 call recordings)
  - Harris Poll for Hostie (2,065 adults): 63% prefer to phone and 69% would likely give up on a restaurant that doesn't answer. These are attitudes, not measured behavior.

#### ROI case evidence (company or vendor-reported unless noted)
- **Toast IQ Grow:** pilot sales +8% versus similar restaurants. Spirits Food & Friends cut agency spend 70% and logged $100K+ in attributed sales in under 2 months. — [Yahoo Finance](https://finance.yahoo.com/markets/stocks/articles/toast-tost-launches-toast-iq-132858023.html); [Motley Fool transcript](https://www.fool.com/earnings/call-transcripts/2026/08/11/toast-tost-q2-2026-earnings-call-transcript/)
- **Toast IQ Team Scheduling Agent:** 41% fewer overtime hours per employee among beta users — [The Pass Brief](https://thepassbrief.com/articles/toast-rolls-out-new-restaurant-ai-agents-led-by-labor-tool-tied-to-41-overtime-c-e9744bcb)
- **McDonald's (targets, not results):** Archy aims to free at least 50 labor hours per week per restaurant; inventory tags about 5 hours a week and 15% less food waste. Separately, crew catch and correct about 10% of orders before they reach customers using verification scales. — [Restaurant Dive, Sept 2026](https://www.restaurantdive.com/news/mcdonalds-archiq-drive-thru-artificial-intelligence-next-strategy/831256/)
- **Wendy's FreshAI:** 22 seconds faster service in the first test — [Fortune, 2024](https://dc.fortune.com/2024/10/15/wendy-google-ai-drive-thru-expansion)
- **SoundHound (Q1 2026):** a major QSR reports that AI-enabled drive-thru locations generate more revenue than non-AI locations. No figures given. — [Yahoo/Zacks](https://finance.yahoo.com/technology/ai/articles/soundhounds-drive-thru-ai-shows-142800856.html)
- **Bite Buddy (single location, about 2025):** missed calls fell from 450 to 14, average ticket rose 25%, and the vendor claims "10x ROI in 27 days" — [Bite Buddy case study](https://bitebuddy.ai/case-studies/ai-phone-ordering-25-percent-revenue-increase)
- **China:** Meituan AI店长 trials showed +30% weekly orders and +25% new-store output — [Leiphone](https://www.leiphone.com/category/industrynews/tZI7pYBhiFduVfr6.html). 淘宝闪购 says 外婆家's profit rose 14% — [21jingji](https://m.21jingji.com/article/20261103/herald/b56e37691b92f337f4a4d22c09b29386.html)

#### Complaints, accuracy and churn signals
- McDonald's ended its IBM test in 2024 over accuracy — [Restaurant Dive](https://www.restaurantdive.com/news/mcdonalds-ibm-drive-thru-automation-voice-ordering-ai/719085/)
- Taco Bell's 18,000-water-cup prank and "rethink" (August 2025) — [Computing](https://www.computing.co.uk/news/2025/ai/taco-bell-scales-back-ai-tests-after-customer-complaints)
- Burger King drive-offs and a move to optional human ordering (September 2026) — [NRN](https://www.nrn.com/quick-service/burger-king-rethinks-its-drive-thru-ai-strategy)
- Presto's human-in-the-loop disclosures and the January 2025 SEC charges — [Katalyst](https://www.katalystos.com/blog/ai-phone-answering-for-restaurants-what-it-costs)
- Chinese merchants switching off platform AI customer service — [woshipm](https://www.woshipm.com/ai/6485093.html)
- Losses from automated repricing — [OFweek](https://mp.ofweek.com/Internet/a056714478607)
- On the positive side, SoundHound claims 100% renewal for key restaurant accounts (company-sourced) — [Yahoo/Zacks](https://finance.yahoo.com/technology/ai/articles/soundhound-adds-3-restaurant-chains-165000162.html)

#### Commission context
- **US (pricing pages accessed Nov 2026).** DoorDash: 15% / 25% / 30% delivery and 6% pickup — [DoorDash](https://merchants.doordash.com/en-us/pricing). Uber Eats: 20% (15% in select cities) / 25% / 30% delivery and 7–10% pickup — [Uber Eats](https://merchants.ubereats.com/us/en/pricing/)
- **Meituan, official position.** The technical service fee (commission) is 6–8%, and reports of 30% commission are "不实" (untrue). The fulfillment fee applies only when the merchant uses Meituan delivery. — [Meituan](https://www.meituan.com/news/NN241225051008799); [Sina, 2025-02-09](https://finance.sina.com.cn/roll/2025-02-09/doc-ineivyah8215709.shtml)
- **Meituan, all-in cost examples (财经, 2025-05-28).**
  - Fulfillment fee: ¥3.65 base within 3 km, +¥0.11 per 0.1 km beyond that, +¥0.14 per ¥1 of order value between ¥30 and ¥80.
  - Merchant-funded coupons: on a ¥5 神券 the merchant pays ¥3; on 满38减18 the merchant pays ¥11.
  - Worked orders:

    | Order | Breakdown | Total take |
    |---|---|---|
    | Meituan ¥29 | commission 6.4% | 19.7% |
    | Meituan ¥60 | subsidy ¥11, commission ¥2.55 (7.2%), delivery ¥4.97; merchant nets ¥41.48 | 30.8% |
    | Ele.me ¥24 | — | 38.1% |

  - Ele.me base commission is 5–8%. JD charged no commission for merchants that joined before May 2025. Merchants cite comprehensive rates of 20–40%.
  — [Sina/财经, 2025-05-28](https://finance.sina.com.cn/roll/2025-05-28/doc-ineycmqy7871683.shtml)
- **Meituan in 2026 (low reliability).**
  - A self-media estimate puts the composite rate at about 20–25% (6–8% commission plus a dynamic fulfillment fee) — [Toutiao, 2026](https://www.toutiao.com/w/1867764058540108/)
  - An **unverified single-source** claim says that from Q1 2026 Meituan merged commission and online marketing into "商家服务收益" at a 7–10% rate. It conflicts with the official 6–8%. Source: search summary only, not verified.
  - Beijing regulators cited delivery-war examples: an ¥18 dumpling order netting the merchant ¥1.25, and a fried-chicken set losing ¥5–6 per order (secondary) — [OFweek](https://mp.ofweek.com/Internet/a056714478607)

### Inferences
- **The binding constraint on willingness to pay is trust and accuracy, not the sticker price.** Illustrative arithmetic under stated assumptions:
  - Assume a location with $1M annual sales and a $450/month agent.
  - Annual agent cost of $5.4K is about 0.5% of sales, equal to roughly 14–19% of a 2.8–4.0% pre-tax profit.
  - If incremental orders carry about 65% contribution (food about 32%, card fees about 3%, labor fixed), the agent pays back with about $8.3K a year of incremental sales, or about 23 incremental $30 orders a month.
  - This low hurdle explains why vendors lean on missed-call statistics. Their ROI claims are unaudited and depend on assumed conversion rates (Katalyst).
- **Agent fees are tiny next to commissions.** A $30 delivery order costs $4.50–$9.00 in US marketplace commission at 15–30%. A per-order agent fee of $1.50 is 5% of that ticket. Emerging "AI-channel" prices cluster around 5% of order value: Owner's 5% per-order fee, Douyin's 4–13% AI surcharge, and Square's roughly 3% processing on ChatGPT and Claude orders. That band is a plausible ceiling for a platform-sold ordering agent, well below marketplace commission.
- **Merchants pay for outcomes they can attribute** (sales lift, avoided agency fees, avoided commission). They resist agents that add work or errors: AI customer service turned off in China, drive-offs at Burger King. Human-in-the-loop designs (Toast, Square, McDonald's crew verification, Taco Bell at peak) are the norm, which raises vendors' delivery costs.
- **China's high all-in take rate (20–38%) against a low nominal commission (6–8%)** gives Meituan room to give agents away and recover the value through promotions and fulfillment. It also makes merchants suspicious that agent advice favors platform GMV over merchant profit.

### Gaps
- No Deloitte or McKinsey 2025–2026 restaurant-AI willingness-to-pay surveys were found in this pass.
- Toast and Square restaurant surveys appeared only through aggregators and were not verified: Toast's "86% comfortable with AI / 81% plan to increase usage," a Popmenu survey of 328 operators (69% adopting AI), and a Hospitality Technology study (79% using AI).
- No audited, independent ROI study of phone or drive-thru AI exists in the sources found.
- No churn or logo-retention data for AI phone vendors was found beyond SoundHound's company claim.
- No verified US average unit volume was gathered. The break-even example above uses an assumed $1M location.
- No primary 2026 Meituan fee schedule was found, so the 7–10% "商家服务收益" claim remains unverified.

---

## Q4. Who captures the merchant's agent budget, and why

### Takeaway
As of November 2026, in the US the POS or restaurant OS layer is best placed. Toast (about 180,000 locations) and Square own the transaction data and daily workflow. They can either charge where the agent replaces an outsourced service (Toast IQ Grow at $499 with a human included) or give agents away to protect payments and SaaS revenue (Square Managerbot). They are also making the POS a commission-free rail for AI-assistant ordering (Square in ChatGPT and Claude).

Point solutions win only where they own a hard modality (voice, drive-thru), and they face consolidation (SoundHound's roll-up) and encroachment from POS vendors (Toast's voice-agent roadmap, Owner's phone-ordering waitlist). Large chains keep drive-thru AI in-house on their own stacks.

Marketplaces, and in China the super-platforms, give agents away and capture value through commission, ads and AI-channel take-rate. In China that model dominates, and direct willingness to pay for agents is close to zero. Pricing is moving from per-location SaaS toward usage (per order), percentage-of-order and bundled agent-plus-human outcome offers. Fully outcome-based contracts are not yet visible in public pricing.

### Cited Findings
- **Workflow and data ownership.**
  - Toast IQ Grow uses data from 150,000+ Toast customers with human approval of AI workflows. Toast plans agents for voice, scheduling, payroll and tax, inventory and bookkeeping. — [Motley Fool transcript, 2026-08-11](https://www.fool.com/earnings/call-transcripts/2026/08/11/toast-tost-q2-2026-earnings-call-transcript/)
  - Square Managerbot acts on POS data (sales, labor, inventory, catalog) on a propose-then-approve basis, free in beta — [Square](https://squareup.com/us/en/press/managerbot-open-beta)
  - Point-solution integration limits: Slang integrates with reservation and CRM systems, not POS order entry — [Katalyst](https://www.katalystos.com/blog/ai-phone-answering-for-restaurants-what-it-costs)
- **The budget is the services line, not the software line.** Narang says restaurants spend "a multiple" of their software spend on outsourced services. Toast's showcase customer cut agency spend 70%. — [Motley Fool transcript](https://www.fool.com/earnings/call-transcripts/2026/08/11/toast-tost-q2-2026-earnings-call-transcript/)
- **The fight over AI-assistant ordering channels.**
  - Square routes ChatGPT and Claude orders to merchants with no marketplace commission — [VentureBeat](https://venturebeat.com/marketing-tech/restaurants-can-now-accept-orders-placed-directly-from-chatgpt-and-claude-thanks-to-squares-new-low-fee-no-setup-integration/)
  - Swiggy takes orders via ChatGPT, Claude and Gemini (January 2026) — [YourStory](https://yourstory.com/2026/01/swiggy-lets-users-order-through-chatgpt-claude-gemini)
  - Douyin charges a 4–13% surcharge on Doubao-referred orders in some categories — [woshipm](https://www.woshipm.com/ai/6485093.html)
  - DoorDash makes its own Online Ordering commission-free — [DoorDash pricing](https://merchants.doordash.com/en-us/pricing)
- **Enterprise chains keep drive-thru AI in-house.** Taco Bell runs on Byte by Yum with Omilia — [RTN](https://restauranttechnologynews.com/2026/07/taco-bell-expands-drive-thru-voice-ai-to-nearly-900-restaurants-across-the-u-s-as-automation-gains-momentum/). McDonald's uses ArchIQ with Google — [Restaurant Dive](https://www.restaurantdive.com/news/mcdonalds-archiq-drive-thru-artificial-intelligence-next-strategy/831256/). Burger King spent about $400M unifying its tech stack — [NRN](https://www.nrn.com/quick-service/burger-king-rethinks-its-drive-thru-ai-strategy)
- **Point-solution consolidation.** SoundHound bought SYNQ3, Allset, Amelia and Interactions (2024–2025), and the LivePerson deal is pending — [PrivSource](https://www.privsource.com/acquisitions/deal/x7S8mv); [SoundHound 8-K](https://www.sec.gov/Archives/edgar/data/0001840856/000184085626000019/soun-20260806ex991earnings.htm)
- **Free bundling in China.**
  - Meituan's AI店长 is free — [Leiphone](https://www.leiphone.com/category/industrynews/tZI7pYBhiFduVfr6.html). So is 袋鼠参谋 — [Sina](https://client.sina.com.cn/news/2025-10-16/doc-infucfih9409659.shtml)
  - 客如云's five agents are free, with revenue from SaaS, hardware and services — [Sina (sponsored)](https://finance.sina.com.cn/jjxw/2026-03-26/doc-inhshnxa6264750.shtml)
  - 淘宝闪购's AI tools are offered alongside its merchant-growth programs — [21jingji](https://m.21jingji.com/article/20261103/herald/b56e37691b92f337f4a4d22c09b29386.html)
  - Critics argue the "free" tools are paid back through commission and promotion fees and lock in merchant data — [OFweek](https://mp.ofweek.com/Internet/a056714478607)
- **Multi-homing and self-building.** Chinese multi-platform merchants pay yearly for 生意参谋 and 京东商智 but reject new platform AI suites as aimed at beginners. Some build their own AI workflows but lack API access. — [woshipm, 2026-10-28](https://www.woshipm.com/ai/6485093.html)
- **Pricing-unit evidence:**
  - Per location per month: Slang, Maple, Kea, 7shifts — [Katalyst](https://www.katalystos.com/blog/ai-phone-answering-for-restaurants-what-it-costs); [Costbench](https://costbench.com/changelog/7shifts-price-increase-2026-04/)
  - Per order: Bite Buddy, $1.50 — [Katalyst](https://www.katalystos.com/blog/ai-phone-answering-for-restaurants-what-it-costs)
  - Subscription plus % of order: Owner, $249 + 5% — [Owner](https://www.owner.com/pricing)
  - Agent-plus-human bundle: Toast IQ Grow, $499 — [Yahoo Finance](https://finance.yahoo.com/markets/stocks/articles/toast-tost-launches-toast-iq-132858023.html)
  - Free, bundled with POS or payments: Square — [Square](https://squareup.com/us/en/press/managerbot-open-beta)
  - Platform take-rate surcharge: Douyin, 4–13% — [woshipm](https://www.woshipm.com/ai/6485093.html)

### Inferences
- **价值主张: who can credibly promise merchant profit.**
  - POS and direct-ordering vendors can promise "more profit, fewer outsourced costs, fewer commissions" because their incentives line up with the merchant's margin.
  - Marketplaces promise "more orders on our platform." Merchants increasingly read that as platform GMV first: Meituan's goals diverge from merchant profit (OFweek), and Chinese merchants switch off platform AI.
  - A DoorDash or Meituan agent therefore needs a profit-aligned promise to win budget. Examples: guaranteed incremental orders, transparent and auditable actions, no automatic price changes without approval.
- **价值创造与传递: execution needs write access to the merchant's systems.** That means menu, pricing, schedules, inventory and the phone line. POS vendors already have it. Marketplaces have it only for their own storefront (menu, promotions on platform). That is why Meituan's AI店长 focuses on in-platform operations (diagnosis, product operations, new stores) and why US marketplace AI is limited to menu copy, photos and review summaries. Drive-thru and phone ordering run through POS integrations, so a marketplace agent covering those channels would need deep POS partnerships.
- **价值获取: three viable models are visible.**
  1. SaaS pricing tied to a replaced cost line: Toast IQ Grow, about $499/mo versus agency fees; phone AI at $399–$599 versus labor and missed orders.
  2. Free bundling, with capture through payments or take-rate: Square, Meituan, 淘宝闪购, 客如云.
  3. AI-channel take-rate: Douyin's 4–13%; and, by analogy, any platform fee on agent-originated orders.

  For DoorDash and Meituan, model (2) or (3) fits existing economics. Model (1) faces free POS bundles in the US and a zero-price anchor in China.
- **Likely winner of the merchant's own agent budget (cash out of pocket):**
  - US independents and SMBs: POS suites (Toast, Square), then direct-ordering and marketing platforms (Owner), then phone-AI specialists.
  - Large chains: their in-house stacks plus big-tech and voice vendors (Google, Omilia, SoundHound, Presto).
  - China: no meaningful standalone agent budget. Value flows through platform commission and ads, so the platforms (Meituan, Alibaba, Douyin) capture it indirectly.
- **Pricing trend.** List pricing is moving from per-location SaaS toward hybrids with usage (per order, percent of order) and bundled human-plus-AI "service-as-software" retainers. Pure outcome-based contracts (pay per incremental sale) are not yet visible in public price lists. The nearest analogs are per-completed-order fees and Toast's sales-lift positioning.

### Gaps
- Not examined here, covered by the DoorDash and Meituan researchers: how DoorDash bundles SevenRooms, voice ordering or AI into commission tiers, and Meituan's ad-load or commission effects from AI店长.
- No quantitative data was found on how many merchants use more than one AI phone or marketing vendor, or on switching between POS-native and point-solution agents.
- Toast has not disclosed how much ARPU uplift agents produce. Grow's only metric is "fastest to $10M ARR," with no attach rate.
- Square has not said whether Managerbot will stay free after the beta.
- No public outcome-based (pay-per-incremental-revenue) contracts were found for restaurant AI vendors.
