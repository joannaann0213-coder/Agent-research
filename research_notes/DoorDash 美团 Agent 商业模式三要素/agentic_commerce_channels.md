# Agentic-Commerce Channels: Third-Party AI Agents Ordering Food, Local Services and Goods, and How Delivery and Local-Commerce Platforms Respond (US/Global and China, as of 2026-11-03)

Tags used below: **[Official]** = company, regulator or industry-body statement, document or press release (sometimes read via a press write-up, as noted). **[Press]** = media report. **[Co. claim]** = a metric the company disclosed and nobody has independently checked. **[Forecast]**. **[Opinion]** = analysis or commentary. **[SUPERSEDED]** = later events overtook it. Every date is the date of the event or publication. "Fetch blocked" means the primary page returned HTTP 403 to my fetch, so the claim rests on the secondary coverage cited.

---

## 1. US/global protocols and channels: what launched, when, and on what commercial terms

### Takeaway
Between September 2025 and May 2026 every large US AI player shipped an agent-checkout protocol or channel: OpenAI ACP/Instant Checkout, Google AP2→UCP→Universal Cart, Microsoft Copilot Checkout, Perplexity with PayPal, Amazon Alexa+ and Buy for Me. The card networks and PSPs built agent-credential rails underneath them. The one channel that tried to own checkout and charge a toll was OpenAI's Instant Checkout, with a reported 4% merchant fee. It was rolled back in March 2026, and checkout moved back to merchants' own apps and sites. For food delivery, every observed integration runs on the platform's account, menu, pricing and logistics. These include the ChatGPT apps, Alexa+ with Grubhub and Uber Eats, Gemini's Android task automation, and the Swiggy and Zomato MCP servers. None has a disclosed agent fee.

### Cited Findings

#### OpenAI: Agentic Commerce Protocol (ACP), Instant Checkout, ChatGPT apps
- **[Official, 2025-09-29]** OpenAI launched Instant Checkout in ChatGPT. It runs on the Agentic Commerce Protocol (ACP), which OpenAI built with Stripe and open-sourced under Apache 2.0. — [Fortune, 2025-09-30](https://fortune.com/2025/09/30/openai-stripe-acp-chatgpt-instant-checkout-could-blow-up-online-shopping/); OpenAI's own page [“Buy it in ChatGPT”](https://openai.com/index/buy-it-in-chatgpt/) (fetch blocked). Etsy was among the first merchants. — [Modern Retail, 2026-03-27](https://www.modernretail.co/technology/what-went-wrong-with-chatgpts-instant-checkout/)
- **[Official terms, via secondary]** Shoppers paid nothing extra. Merchants paid a fee on completed purchases, on top of their normal payment-processing costs. — [Opascope](https://opascope.com/insights/ai-shopping-assistant-guide-2026-agentic-commerce-protocols/)
- **[Press, Jan 2026] [SUPERSEDED by the March 2026 pivot]** OpenAI's fee for Shopify merchants was reported as **4% of each completed Instant Checkout sale**, starting around 2026-01-26. — [ecommercenews.asia](https://ecommercenews.asia/story/openai-to-charge-4-fee-on-shopify-chatgpt-checkout); [EcomWatch](https://ecomwatch.com/news/openai-charges-shopify-merchants-fee-for-instant-checkout/); [Stellagent, 2026-01-23](https://stellagent.ai/insights/chatgpt-instant-checkout-4-percent-fee-2026-01-23). One analysis puts the all-in cost at about 9.2% including card processing **[Opinion]**. — [INITE](https://inite.ai/en/blog/agentic-commerce-after-instant-checkout)
- **[Protocol terms]** Under ACP the merchant stays merchant of record. It owns the order record and calls its own PSP to authorise payment. — [Eco ACP explainer](https://eco.com/support/en/articles/14845478-acp-agentic-commerce-protocol-explained). Stripe's Shared Payment Token is a scoped reference to the buyer's payment method. It carries usage limits and an expiry, and it never exposes card numbers to the agent or merchant. — [Stripe docs](https://docs.stripe.com/agentic-commerce/concepts/shared-payment-tokens)
- **[Press]** Starting in November 2025, Walmart put about 200,000 products into Instant Checkout. — [MarTech, 2026-03-20](https://martech.org/walmart-says-chatgpt-checkout-converted-3x-worse-than-its-own-website/)
- **[Official press release, 2025-12-08]** Instacart's ChatGPT app was the first to offer end-to-end grocery shopping and checkout inside ChatGPT, using ACP-based Instant Checkout. Details:
  - It covered 1,800+ retailers.
  - Stripe processed payments: cards at launch, with Apple Pay and Google Pay due "shortly".
  - Instacart's own logistics network fulfilled orders.
  - In-chat checkout launched on desktop and mobile web; native iOS and Android checkout was due "in the coming weeks".

  Sources: [Instacart release via Barchart](https://www.barchart.com/story/news/36507554/instacart-app-launches-in-openai-chatgpt-first-company-to-offer-new-instant-checkout-app-experience); [Retail Insight Network](https://www.retail-insight-network.com/news/instacart-embeds-grocery-shopping-chatgpt/); [TechRadar](https://techradar.com/ai-platforms-assistants/chatgpt/instacart-brings-instant-checkout-to-chatgpt)
- **[Press, 2025-12-18]** DoorDash brought grocery ordering into ChatGPT. The app was live for a limited group of users, with a broader mobile and desktop rollout planned over the following months. — [BetaNews](https://betanews.com/2025/12/18/doordash-is-bringing-grocery-shopping-directly-into-chatgpt/) (fetch blocked; search summary); the December 2025 launch is confirmed by [Modern Retail](https://www.modernretail.co/technology/what-went-wrong-with-chatgpts-instant-checkout/). I could not confirm whether DoorDash's ChatGPT checkout happens in ChatGPT or hands off to DoorDash.
- **[Press, Oct 2025]** OpenAI's Apps SDK partner list included DoorDash, Uber and Instacart, among others, for ordering and booking inside ChatGPT. — [Pulse.bot](https://www.pulse.bot/ecommerce/news/openai-adds-doordash-uber-and-dozens-of-services-to-chatgpt-boosting-ecommerce-inapp-70da2e9f-5390-4d06-8bb1-0bb0d7eaee06/); [Restaurant Business](https://restaurantbusinessonline.com/technology/restaurant-delivery-coming-chatgpt) (not fetched)
- **[Weakly sourced]** The Uber and Uber Eats app in ChatGPT is US-only. Users browse restaurants and menus in ChatGPT, then ChatGPT sends them to the Uber Eats app to finish the order and pay. Sources disagree on whether a ride can be booked inside ChatGPT. The most-cited availability date, 2026-03-14, is unverified. — [Unanswered.io guide](https://unanswered.io/guide/chatgpt-uber-integration); [Skila](https://news.skila.ai/article/chatgpt-app-integrations-march-2026)
- **[Press, early March 2026 → Official, 2026-03-24] OpenAI pivot.**
  - The Information reported that OpenAI would scale back Instant Checkout because users researched products in ChatGPT but did not buy there.
  - An OpenAI spokesperson said "Instant Checkout is moving to Apps".
  - An OpenAI blog post on 2026-03-24 said merchants could use their own checkout and that OpenAI would focus on discovery. It said Instant Checkout "did not offer the level of flexibility that we aspire to provide."

  Sources: [Modern Retail, 2026-03-27](https://www.modernretail.co/technology/what-went-wrong-with-chatgpts-instant-checkout/); [Digital Commerce 360, 2026-03-06](https://www.digitalcommerce360.com/2026/03/06/openai-shifts-checkout-plans-agentic-commerce-strategy/amp/); [PaymentReview](https://paymentreview.com/blog/agentic-commerce-merchants-2026/); [American Banker](https://www.americanbanker.com/payments/news/openai-moves-ai-checkout-to-third-parties)
- **[Press, Mar 2026]** CNBC, as cited by Internet Retailing, reported that OpenAI struggled with four things:
  - onboarding merchants
  - showing accurate product data
  - supporting multi-item carts
  - connecting loyalty memberships

  Source: [Internet Retailing](https://internetretailing.net/?p=74450). Secondary blogs put live Shopify merchants at only about 12 during testing, or about 30 by February 2026; the two figures conflict and confidence is low. — [Spree Commerce](https://spreecommerce.org/openai-just-blinked-nobody-seems-to-want-to-shop-inside-chatgpt/); [Stellagent](https://stellagent.ai/insights/openai-scales-back-instant-checkout)
- **[Press, Mar 2026]** After the pivot:
  - Shopify keeps product discovery in ChatGPT but sends checkout to merchants' own stores. — [Shopifreaks](https://www.shopifreaks.com/openai-pivots-from-instant-checkout-to-an-app-based-shopping-model-inside-chatgpt/)
  - OpenAI has published no fee for app-based checkout. — [Agentic Commerce Feed, 2026-03-16](https://www.agenticcommercefeed.com/blog/2026-03-16-openai-kills-instant-checkout); [PaymentReview](https://paymentreview.com/blog/agentic-commerce-merchants-2026/)
- **[Press, 2026-03-27] Shape of the post-pivot model.**
  - Retailer apps inside ChatGPT: Walmart's app links accounts, supports loyalty and payments, and runs its Sparky assistant inside ChatGPT. Sephora announced an app at Shoptalk in March 2026. CarMax launched one in February 2026. Etsy is building one.
  - Redirect models: Lowe's and **Instacart** send shoppers to their own websites to complete checkout.

  Source: [Modern Retail](https://www.modernretail.co/technology/what-went-wrong-with-chatgpts-instant-checkout/). **[Possible SUPERSEDED]** This suggests Instacart's December 2025 in-chat checkout was replaced by a redirect. I could not confirm that independently.

#### Google: AP2, then UCP, then Universal Cart and Gemini Spark; Gemini Android task automation
- **[Official, 2025-09-16] AP2 (Agent Payments Protocol).**
  - An open protocol launched with 60+ partners, including Mastercard, American Express, PayPal, Coinbase, Adyen and Salesforce.
  - It relies on cryptographically signed "mandates" (intent, cart and payment) as proof of what the user instructed.

  Sources: [Finextra](https://www.finextra.com/newsarticle/46600/google-unveils-agent-payments-protocol-to-power-ai-commerce); [Digital Commerce 360, 2025-09-19](https://www.digitalcommerce360.com/2025/09/19/google-ai-payments-protocol-ap2/); [The Decoder](https://the-decoder.com/google-introduces-open-protocol-for-ai-agent-payments/)
- **[Press, single secondary source]** On 2026-04-28 the FIDO Alliance announced that Google had donated AP2 to it. — [PPC Land](https://ppc.land/googles-universal-cart-wants-to-follow-you-everywhere-you-shop/) (needs verification)
- **[Official, 2026-01-11, NRF] Universal Commerce Protocol (UCP).**
  - Google built it with Shopify, Etsy, Wayfair, Target and Walmart, and it is open-source.
  - Its reference implementation powers native checkout in AI Mode in Search and in the Gemini app. Payment is through Google Pay, with PayPal "soon".
  - Merchants connect through APIs, A2A or MCP.
  - The merchant stays merchant of record and keeps the customer relationship, pricing, checkout logic and transaction data.

  Sources: [Technobezz, 2026-01-11](https://www.technobezz.com/news/google-launches-universal-commerce-protocol-for-ai-shopping-2026-01-11-jkpd); [American Banker](https://www.americanbanker.com/payments/news/google-shopify-unveil-new-agentic-protocol-for-retailers); [IT Brief](https://itbrief.co.nz/story/google-unveils-open-ucp-standard-for-ai-driven-shopping); [TechDivision](https://www.techdivision.com/en/newsroom/agentic-commerce-and-the-universal-commerce-protocol); [Digital Commerce 360, 2026-01-14](https://www.digitalcommerce360.com/2026/01/14/wayfair-google-universal-commerce-protocol/amp/)
- **[Official/Press, Feb–Mar 2026] Gemini "task automation" on Android.** It launched with the Galaxy S26 in February 2026 and rolled out on 2026-03-12; Pixel 10 is also supported.
  - Gemini operates food, grocery and rideshare apps in the background, inside a secure virtual window.
  - Supported apps: DoorDash, Grubhub, Uber Eats, Uber, Lyft and Starbucks, with Instacart coming.
  - Markets: the US (English only) and Korea.

  Sources: [9to5Google, 2026-03-12](https://9to5google.com/2026/03/12/gemini-android-app-automation-galaxy-s26-rollout/); [ConsumerAffairs, 2026-02-26](https://www.consumeraffairs.com/news/how-googles-gemini-can-order-dinner-and-book-your-ride-022626.html). DoorDash called its part a **pilot** of AI-enabled ordering in the Gemini app. — [Chain Store Age](https://chainstoreage.com/doordash-pilot-ai-enabled-ordering-google-gemini-app) (fetch blocked; headline and search summary); [Retail TouchPoints: "DoorDash, Uber Test Out True Agentic Ordering in Google Gemini"](https://www.retailtouchpoints.com/?p=617519)
- **[Official, 2026-05-19, I/O] Universal Cart and Gemini Spark.**
  - Universal Cart is one multi-merchant cart across Search, Gemini, YouTube and Gmail. Checkout runs through UCP and Google Pay, or hands off to the merchant's site. The US rollout was set for "this summer".
  - Gemini Spark is an always-on background agent. It "won't take important actions like spending money… without explicit user approval".
  - AP2 comes to Google products starting with Spark.

  Sources: [Android Central](https://androidcentral.com/apps-software/ai/5-important-gemini-updates-from-google-i-o-2026-that-could-genuinely-save-you-time); [Swipe Insight](https://web.swipeinsight.app/posts/google-unveils-universal-cart-and-new-shopping-tools-at-i-o-2026-24454); [Pulse 2.0](https://pulse2.com/google-universal-cart/)

#### Amazon: Alexa+ food ordering, Buy for Me, Alexa for Shopping
- **[Official, 2025-04-03]** The "Buy for Me" beta launched. Amazon's agent buys items on third-party brand websites, using the payment and shipping details already stored at Amazon. — [TechCrunch](https://techcrunch.com/2025/04/03/amazons-new-ai-agent-will-shop-third-party-stores-for-you)
- **[Press, Jan 2026]** Sellers pushed back because Amazon's agents listed and bought their products without permission. — [SiliconANGLE](https://siliconangle.com/?p=726997); [Modern Retail, 2026-01-06](https://www.modernretail.co/technology/why-the-ai-shopping-agent-wars-will-heat-up-in-2026/)
- **[Press, secondary, 2026-05-13]** Amazon retired the Rufus brand and folded it into a new agent, "Alexa for Shopping", which reportedly includes Buy for Me. — [Let's Data Science](https://letsdatascience.com/blog/amazon-retires-rufus-alexa-for-shopping-agent)
- **[Official, 2026-03-31]** Alexa+ added conversational ordering from **Grubhub and Uber Eats**.
  - The user links their Grubhub or Uber Eats account in the Alexa app (Alexa+ Store → Food & Reservations).
  - Past orders sync automatically.
  - It runs on Echo Show 8 and larger, for Alexa+ users.

  Sources: [TechCrunch, 2026-03-31](https://www.techcrunch.com/2026/03/31/alexa-plus-new-food-ordering-experiences-with-uber-eats-and-grubhub/); [PYMNTS](https://www.pymnts.com/amazon/2026/amazons-alexa-adds-conversational-uber-eats-and-grubhub-ordering/). DoorDash is not on the list.

#### Perplexity and Microsoft
- **[Official, 2025-11-25]** Perplexity launched free shopping in the US on desktop and web, with "Instant Buy" powered by PayPal. — [Marketing4eCommerce](https://marketing4ecommerce.net/en/perplexity-and-copilot-shopping-experience/). Business Standard contradicts this, saying Perplexity has no in-platform instant checkout. — [Business Standard](https://www.business-standard.com/technology/tech-news/microsoft-copilot-shopping-checkout-payment-feature-126010900435_1.html)
- **[Official, 2026-01-08, NRF] Microsoft Copilot Checkout.**
  - PayPal, Stripe and Shopify power it.
  - It is testing with US users on Copilot.com, with Bing, MSN and Edge planned next.
  - Launch merchants: Urban Outfitters, Anthropologie, Ashley Furniture and some Etsy sellers.

  Sources: [Technobezz](https://www.technobezz.com/news/microsoft-tests-copilot-checkout-feature-for-us-users-2026-01-08-8bek); [PPC Land](https://ppc.land/paypal-teams-with-microsoft-to-enable-purchases-inside-copilot/); [The Paypers](https://thepaypers.com/payments/news/stripe-supports-new-copilot-checkout-experience)

#### Card networks and PSPs (the payment layer under every agent)
- **[Official/Press, 2026] Visa Intelligent Commerce Connect.**
  - One integration for merchants, agent builders and payment enablers, covering Visa and non-Visa cards.
  - It supports Trusted Agent Protocol, Machine Payments Protocol, ACP and UCP.
  - Visa reports more than 20 agents or agent platforms integrated directly with Visa Intelligent Commerce, and "hundreds of controlled transactions" in live settings.

  Sources: [TechInformed](https://techinformed.com/visa-opens-one-integration-for-ai-agent-payments/); [Nasdaq](https://www.nasdaq.com/articles/how-can-visas-intelligent-commerce-connect-power-ai-agents)
- **[Official/Press] Mastercard Agent Pay** (launched April 2025).
  - Live authenticated agentic transactions in Hong Kong (2026-03-27) and Thailand (2026-04-07).
  - Since then, a reported 9 APAC markets.
  - In March 2026, Stripe expanded its Shared Payment Tokens to support both Visa Intelligent Commerce and Mastercard Agent Pay.

  Sources: [TechInformed](https://techinformed.com/visa-opens-one-integration-for-ai-agent-payments/); [Stellagent](https://stellagent.ai/insights/visa-mastercard-agentic-commerce-strategy)

#### India and Southeast Asia
- **[Official, January 2026, about 01-28] Swiggy MCP servers** for Food, Instamart (quick commerce) and Dineout (restaurant booking).
  - Usable from ChatGPT, Claude and Gemini.
  - The user logs into their Swiggy account by OAuth.
  - In ChatGPT, every write action, such as placing an order, needs explicit confirmation.
  - At launch the servers had to be added manually in developer mode, and MediaNama found the experience uneven.

  Sources: [Swiggy corporate](https://www.swiggy.com/corporate/?p=16731); [Swiggy MCP docs](https://mcp.swiggy.com/builders/docs/start/what-is-swiggy-mcp/); [Swiggy “use in AI client” doc](https://mcp.swiggy.com/builders/docs/start/consumer/use-in-ai-client.md); [MediaNama](https://www.medianama.com/2026/01/223-ordering-chatgpt-swiggy-services-working/); [India TV, 2026-01-28](https://www.indiatvnews.com/technology/news/swiggy-enables-ordering-via-chatgpt-claude-and-gemini-details-here-2026-01-28-1027737)
- **[Press/guides, about Nov 2025] Zomato (Eternal) MCP server** at `mcp-server.zomato.com/mcp`.
  - Agents can search restaurants, build a cart and check out.
  - The user authenticates through Zomato OAuth.
  - It can be added as a custom connector in ChatGPT or Claude.

  Sources: [Analytics Vidhya, Nov 2025](https://www.analyticsvidhya.com/blog/2025/11/zomato-mcp-server/); [F22 Labs](https://www.f22labs.com/blogs/zomato-mcp-server-guide-architecture-and-features). I found no official Zomato document.
- **[Official] Grab** has its own in-app Grab Shopping Agent for GrabMart groceries. Users build a list by text, voice or photo, then review it and order inside Grab. I found no official consumer MCP or ChatGPT integration. The only "GrabFood MCP" I found is an unofficial, third-party tool for merchants. — [Grab](https://www.grab.com/sg/inside-grab/stories/grabx-ai-shopping-agent-grocery-list); [Vinkius](https://vinkius.com/apps/grabfood-partner-mcp/openai-agents-sdk)

### Inferences
- **Protocol landscape (Nov 2026):** OpenAI/Stripe's ACP and Google's UCP, with AP2 underneath, are the two open checkout standards. Visa, Mastercard and Stripe now carry both, so the payment layer is converging on agent-neutral tokenised credentials. That lowers the cost for any agent to pay any merchant. It does not settle who owns the account.
- **The US equilibrium after March 2026:** the agent handles discovery and intent. The merchant or platform keeps checkout, account, loyalty and fulfilment, through apps or redirects. OpenAI's attempt to own checkout and collect 4% was the test of agent-as-toll-collector, and it failed for lack of merchant uptake and conversion. In US channel terms, the platform has kept 价值获取 (value capture) so far.
- **Two technical modes deserve separate treatment in the report:**
  - **(a) Sanctioned integrations:** ChatGPT apps, ACP/UCP, MCP servers and Alexa+ account linking. The platform decides what to expose, and the user's platform account carries the transaction.
  - **(b) GUI or browser agents** that drive the platform's own app or site with the user's logged-in session: Perplexity Comet, Gemini Android automation, Doubao Phone in China. Platforms accept (b) only when they are partners in it, as with DoorDash's Gemini pilot.
- Food delivery is the vertical where platforms opened most readily: DoorDash, Uber Eats, Grubhub, Instacart, Swiggy and Zomato all have some sanctioned agent channel. The likely reason is that fulfilment (dashers and logistics) cannot be bypassed, so the platform's 价值创造与传递 (value creation and delivery) remains indispensable even when discovery moves to an agent.

### Gaps
- I could not fetch OpenAI's primary Instant Checkout terms (HTTP 403). The 4% figure comes from press and Shopify-merchant reports, not an OpenAI document.
- No source confirms whether DoorDash's ChatGPT app checks out inside ChatGPT or redirects, or whether DashPass benefits apply there. The same applies to Uber One in the Uber ChatGPT app and to Instacart+ in Instacart's app.
- I could not verify the OpenTable ChatGPT app's launch date or terms.
- I found no "Copilot Merchant Program" page. Only Copilot Checkout (January 2026) turned up.
- I did not research Meta agent-commerce protocols in depth, and none surfaced in my searches.
- I could not confirm the launch date of Visa Intelligent Commerce Connect, or whether the reported Visa–OpenAI tokenised-credential tie-up in June 2026 happened.
- Several Gemini task-automation details (US/Korea scope, beta status) come from secondary coverage, and Chain Store Age's DoorDash pilot article was not readable.
- I found no fee for Alexa+, Gemini automation or ChatGPT apps paid either way between agent and platform. That most likely means no fee exists, but no document says so.

---

## 2. Platform responses: who opened, who blocked or sued, who charges or is charged, and whether the platform keeps the account, membership, ads exposure and data

### Takeaway
Platform responses split by market power. Owners of high-margin demand and ad inventory block unsanctioned agents and send their own agents out: Amazon, eBay, and in China WeChat and Taobao. Platforms whose real moat is fulfilment open sanctioned channels on their own terms: DoorDash, Uber, Instacart, Grubhub, Walmart, Swiggy and Zomato. In every sanctioned food or grocery channel found, the user's platform account, the platform's prices and its logistics carry the order. That keeps account, data and commission with the platform. The exposed pieces are on-platform ad impressions, cross-sell and membership upsell. DoorDash's own answer to that is to consider buying ads inside agents. Third-party agent volume is negligible so far: DoorDash reported "several hundred" orders in total as of October 2026.

### Cited Findings

#### Opened or cooperating (US/India)
- **[Official, earnings call, 2026-10-29] DoorDash on third-party agents.** Tony Xu said:
  - Ordering through agents "is something that is still a very dynamic area."
  - "we're seeing maybe several hundred orders in total from some of these third-party agents." He named Muse, Instinct and Fo, "and others".
  - "you never want to fight gravity," but you also "don't want to be delusional."

  DoorDash wants to learn about agentic use cases and build a healthy relationship with agent makers. It could advertise within these agents in the future. Users still order mainly through DoorDash's own app. — [TechCrunch, 2026-10-30](https://techcrunch.com/2026/10/30/doordash-said-its-only-received-several-hundred-orders-from-ai-agents/)
- **[Official, via TechCrunch] DoorDash's agent-facing surfaces.** DoorDash offers an MCP that lets other chatbots place orders, aimed at corporate ordering. It also runs its own agents in other companies' channels: a text-to-order agent in Apple Messages (September 2026), a CLI agent (July 2026) and the Ask DoorDash chatbot (June 2026). — [TechCrunch](https://techcrunch.com/2026/10/30/doordash-said-its-only-received-several-hundred-orders-from-ai-agents/). Secondary sources describe the MCP as a beta that lets *approved* third-party agents order on a user's behalf. — [MindStudio](https://www.mindstudio.ai/blog/doordash-ai-agent-ordering-mcp); [AI Weekly](https://aiweekly.co/alerts/doordash-launches-imessage-text-to-order-ai-agent-and-mcp-server-for-corporate). Unofficial community DoorDash MCP servers also exist; they log into the user's account and can place real orders. — [Glama](https://glama.ai/mcp/servers/ashah360/doordash-mcp/tree)
- **[Press/Opinion]** DoorDash describes AI agents the way it once described Facebook and Google: top-of-funnel channel partners rather than existential threats. — [Barchart](https://www.barchart.com/story/news/454746/is-doordash-stock-set-to-be-the-next-major-ai-victim) (secondary)
- **[Official, 2026-10-29; context only]** DoorDash agreed to buy Instacart.
  - Value about $17B, or $65 per share, a 26% premium.
  - Expected to close at the end of 2027 or in the first half of 2028.
  - The Instacart brand and app will be kept.

  Source: [AP via WRDW](https://www.wrdw.com/2026/10/29/doordash-buy-instacart-17-billion-big-consolidation-grocery-delivery/). A secondary source says DoorDash cited the "ChatGPT era" as part of its rationale and framed its strategy as "the battle for atoms". — [FourWeekMBA](https://fourweekmba.com/ai-doordash-cites-chatgpt-era-as-it-buys-instacart-for-17b/) (unverified)
- **[Official, Q1 2026 call, 2026-05-06] Uber.** Dara Khosrowshahi said: "Whether it's an Apple or an OpenAI or a Claude or Gemini, we will work with these agents as well." He still expects "the majority of those experiences are gonna come direct to us." He compared the situation to metasearch: "We saw the same theme play out in metasearch." — [Diginomica](https://diginomica.com/node/30465); [National CIO Review](https://nationalcioreview.com/articles-insights/cio-field-notes/uber-and-instacart-earnings-point-to-agentic-commerce-growth/); [Motley Fool transcript](https://www.fool.com/earnings/call-transcripts/2026/05/06/uber-uber-q1-2026-earnings-call-transcript/)
- **[Press, Mar 2026] Walmart** dropped Instant Checkout in favour of Sparky inside ChatGPT. Users log into their Walmart accounts, carts sync, and purchases complete in Walmart's own system. A similar Gemini integration is expected. — [MarTech, 2026-03-20](https://martech.org/walmart-says-chatgpt-checkout-converted-3x-worse-than-its-own-website/); [The Paypers](https://thepaypers.com/payments/news/walmart-drops-openai-checkout-and-deploys-sparky-on-ai-platforms). TechCrunch contrasts Walmart, which "welcomed" personal agents, with Amazon, which blocks them. — [TechCrunch](https://techcrunch.com/2026/10/30/doordash-said-its-only-received-several-hundred-orders-from-ai-agents/)
- Account-linked channels for Grubhub and Uber Eats (Alexa+), and the Swiggy and Zomato MCP servers (OAuth), are covered in §1.

#### Blocked or sued
- **[Court records/Press] Amazon v. Perplexity** (N.D. Cal. No. 3:25-cv-09514).
  - **Filed 2025-11-04.** Claims under the CFAA plus breach of contract and terms of service. Amazon alleges Comet disguised itself as ordinary Chrome and that Perplexity updated it within 24 hours to get around Amazon's block. — [Bloomberg, 2025-11-04](https://www.bloomberg.com/news/articles/2025-11-04/amazon-demands-perplexity-stop-ai-agent-from-making-purchases); [Law360](https://law360.com/articles/2407841); [PPC Land](https://ppc.land/amazon-sues-perplexity-over-covert-ai-agent-access-to-marketplace/). Engadget's write-up dates the suit to March 2026. That conflicts with the November 2025 filing and is probably a conflation with the injunction.
  - **Preliminary injunction, about 2026-03-09 to 03-10** (Judge Maxine Chesney). It barred Comet from password-protected parts of Amazon, and the judge refused Perplexity's request for a $1B bond. One outlet also reports an order to delete customer data. — [GeekWire, 2026-03-10](https://www.geekwire.com/2026/03/10/judge-blocks-perplexitys-ai-bot-from-shopping-on-amazon-in-early-test-of-agentic-commerce/); [eWeek](https://www.eweek.com/news/amazon-perplexity-comet-injunction/); [Rallies](https://rallies.ai/news/amazon-wins-injunction-blocking-ai-shopping-agent-orders-customer-data-deletion)
  - **[SUPERSEDED]** **Ninth Circuit vacated the injunction (reported 2026-08-04).** The panel reasoned that the Comet user, not Perplexity, accesses Amazon's servers. The CFAA claim was unlikely to succeed, and Amazon had not shown irreparable harm. Amazon said: "We respectfully disagree with today's decision… We remain confident in our case and are evaluating our next steps." — [Engadget](https://engadget.com/2230471/perplexity-has-successfully-overturned-amazon-injunction-on-its-ai-shopping-bot); [MediaPost](https://www.mediapost.com/publications/article/417007/appeals-court-sides-against-amazon-lifti.html); [Sovereign Magazine](https://www.sovereignmagazine.com/article/ninth-circuit-perplexity-comet-amazon-cfaa-ruling)
  - **Rehearing denied later** (exact date not found). The case goes back to the district court. — [MediaPost](https://www.mediapost.com/publications/article/417793/appeals-court-again-rejects-amazon-bid-to-block-pe.html) (fetch blocked; search summary)
- **[Press, secondary] Amazon robots.txt.** By early 2026, Amazon's robots.txt blocked nearly 50 AI-related user agents, including OpenAI's crawlers. Amazon blocks outside agents while sending its own Buy for Me agent onto other merchants' sites. — [Tinuiti](https://tinuiti.com/blog/amazon/amazon-agentic-commerce/); [Swipe Insight](https://web.swipeinsight.app/posts/amazon-and-ebay-block-third-party-ai-tools-as-they-build-their-own-ai-shopping-agents-22880)
- **[Official ToS, posted 2026-01-20, effective 2026-02-20] eBay.**
  - The User Agreement bans "unauthorized third-party AI-powered autonomous tools, including buy-for-me agents, LLM-driven bots, or any other tool that attempts to place orders without human review".
  - Tools with "prior explicit approval" from eBay are exempt.
  - The anti-scraping clause now also covers bots used for AI and LLMs.

  Sources: [EcommerceBytes, 2026-01-21](https://www.ecommercebytes.com/2026/01/21/ebay-bans-ai-shopping-agents-updates-arbitration-provision/); [Value Added Resource](https://www.valueaddedresource.net/ebay-bans-ai-agents-updates-arbitration-user-agreement-feb-2026/); [MediaPost](https://www.mediapost.com/publications/article/412276/ebay-blocks-third-party-agentic-agents-looks-for.html)

#### Who charges whom
- **Agent charging the merchant:** OpenAI's reported 4% Instant Checkout fee (January 2026) was withdrawn with the March 2026 pivot **[SUPERSEDED]**. No fee is published for app-based checkout. — [Agentic Commerce Feed](https://www.agenticcommercefeed.com/blog/2026-03-16-openai-kills-instant-checkout); [PaymentReview](https://paymentreview.com/blog/agentic-commerce-merchants-2026/). In China, Doubao charges a channel fee of about 12% on hotels (§3).
- **Platform paying the agent:** DoorDash says it "could advertise within these agents" in the future. — [TechCrunch](https://techcrunch.com/2026/10/30/doordash-said-its-only-received-several-hundred-orders-from-ai-agents/)
- **Hypothetical **[Opinion]**:** Citrini Research modelled an agent taking a 7% cut, against DoorDash's roughly 25% combined take from customer and restaurant (§5). — [Stocktwits](https://stocktwits.com/news-articles/markets/equity/doordash-moat-under-threat-citrini-research-warns-consumer-ai-agents-bypass-delivery-apps/cZRvn0mR4zK)

#### Account, membership, ads, data
- **[Protocol terms]** Under UCP and ACP the merchant stays merchant of record and keeps the customer relationship, pricing, checkout logic and transaction data. — [TechDivision](https://www.techdivision.com/en/newsroom/agentic-commerce-and-the-universal-commerce-protocol); [Eco](https://eco.com/support/en/articles/14845478-acp-agentic-commerce-protocol-explained)
- **[Official]** Alexa+ links the user's Grubhub or Uber Eats account, and past orders sync. The platform account carries the transaction. — [TechCrunch, 2026-03-31](https://www.techcrunch.com/2026/03/31/alexa-plus-new-food-ordering-experiences-with-uber-eats-and-grubhub/)
- **[Press] Loyalty was a stated weak point of agent-owned checkout.**
  - A specialty-retail executive said in October 2025 that Instant Checkout needed loyalty-program integration, and it never got it. Inventory, coupons, promotions, customer-data collection and store pickup were also missing.
  - Walmart's ChatGPT app now supports loyalty.
  - Circana's Marshal Cohen on why retailers want control: "I want to control the ability to be able to showcase you other products that I have."

  Source: [Modern Retail](https://www.modernretail.co/technology/what-went-wrong-with-chatgpts-instant-checkout/)
- **[Opinion, 2025-11-20]** Nilay Patel's "DoorDash problem" (Decoder): agents in the middle threaten user reviews, ads, loyalty programmes, upsells and subscriptions, and reduce the service to a database. — [Decoder episode listing](https://metacast.app/podcast/decoder-with-nilay-patel/HFyRnpbJ/the-doordash-problem-how-ai-browsers-are-a-huge-threat-to-amazon/x03xTQ5t)

### Inferences
- **Who owns the customer:** in every sanctioned US and India food-delivery channel found, the platform still owns the account, membership status, payment-on-file, order history and fulfilment data. The channels are ChatGPT apps, Alexa+, Gemini automation (which drives the user's logged-in app), and the Swiggy and Zomato MCP servers.
- **What the agent owns:** the *intent*, meaning the first query and the shortlist. That is the 价值主张 (value proposition) layer of "help me choose".
- **What is at risk is narrower than "the customer":** first, sponsored-listing ad impressions, which for DoorDash and Uber are a fast-growing high-margin line. Second, cross-sell and basket building. Third, membership upsell moments. The platform keeps the commission.
- **Most plausible outcome:** the response pattern points to agents becoming another paid acquisition channel, like Google search or Meta ads. DoorDash's "advertise within these agents", Uber's metasearch analogy and Walmart's "discovery in ChatGPT, checkout at home" all fit that. The less plausible outcome is a toll booth that takes a cut of every order.
- **The legal backstop has weakened.** The Ninth Circuit's August 2026 reasoning (the user, not the agent vendor, is the one accessing) weakens the CFAA as a way to stop *user-directed* agents in the US. Platforms will rely more on terms of service (eBay), technical blocking (Amazon) and commercial partnerships (DoorDash–Gemini pilot).

### Gaps
- No primary disclosure says whether DashPass, Uber One or Instacart+ benefits (free delivery, member pricing) apply in ChatGPT, Gemini or Alexa+ orders. Account linking implies they do, but this is unverified.
- No disclosure of ad load or sponsored-listing exposure inside agent channels.
- DoorDash's MCP terms (eligibility, fees, data sharing) are only in secondary sources, and I found no official developer page.
- The exact date of the Ninth Circuit rehearing denial was not found.

---

## 3. China: Qwen, Doubao Phone, WeChat Pay AI card, Alipay, Yuanbao/WorkBuddy and Meituan, regulation

### Takeaway
In China, "third-party agent ordering" is mostly a contest between ecosystems:
- **Alibaba** built the vertically integrated version. Qwen (千问) is the agent, Taobao Shangou, Taobao and Fliggy supply the goods, and Alipay AI付 pays. A heavily subsidised Spring Festival campaign produced more than 120 million AI orders in 6 days (February 2026).
- **ByteDance's Doubao Phone** tried system-level GUI automation of other companies' apps in December 2025. WeChat, Taobao, Alipay and banks blocked it, and it pulled back. Doubao now earns money by routing to its own Douyin local services, charging hotels a channel fee of about 12% from 2026-08-10.
- **Tencent** opened WeChat, Yuanbao and WorkBuddy to allied service providers, Meituan and JD first. WeChat Pay launched an isolated "AI专属卡" (AI-dedicated card) on 2026-06-17.
- **Meituan** is selectively open. It works with Tencent through Yuanbao A2A with its own 小美 agent, WorkBuddy group-buy deals and the WeChat AI ecosystem. Its restaurant bookings are not available to Doubao.
- **Regulation:** the China Payment & Clearing Association, under PBOC guidance, issued a self-regulatory convention on agent payments on 2026-08-24.

### Cited Findings

#### Alibaba: Qwen (千问) App 一句话办事 ("get it done in one sentence")
- **[Official, 2026-01-15]** Qwen App fully connected to Taobao, Alipay, Taobao Shangou (淘宝闪购), Fliggy (飞猪) and Amap (高德), and launched 400+ AI tasks.
  - **"AI 点外卖" (AI takeout ordering):** Qwen calls Taobao Shangou to locate the user, match merchants and generate the order. Payment through Alipay AI付 is completed inside the Qwen conversation with no app switch. The demo ordered 40 milk teas with one instruction.
  - **Amap:** "AI calls the restaurant to book" (AI 打电话订餐厅).
  - **Government services:** 50 Alipay public-service items.
  - **Roadmap:** Qwen consumer-group president Wu Jia (吴嘉) said Qwen would connect the whole Alibaba ecosystem within six months, then bring in third-party partners.

  Sources: [扬子晚报/紫牛, 2026-01-15](https://www.yzwb.net/news/ch/202601/t20260115_311234.html); [科技日报, 2026-01-15](https://www.stdaily.com/web/gdxw/2026-01/15/content_461939.html); [中国金融信息网](https://www.cnfin.com/gs-lb/detail/20260115/4365955_1.html); [财新, 2026-01-15](https://www.caixin.com/2026-01-15/102404090.html) (Caixin also reports Qwen MAU above 100 million at that point); [澎湃](https://m.thepaper.cn/newsDetail_forward_32393627)
- **[Official, 2026-01-16] Alipay's ACT (Agentic Commerce Trust Protocol).** Alipay launched it with Qwen, Taobao Shangou, Rokid and others, calling it China's first open protocol for agent commerce.
  - It has four infrastructure domains: delegated authorisation, commerce interaction, payment service and trust service.
  - The AI only executes the order. **The user always confirms payment.**
  - Qwen uses it for the milk-tea flow: location-based recommendation, price comparison and automatic coupon use, then one tap to confirm Alipay payment.

  Sources: [扬子晚报, 2026-01-16](https://www.yzwb.net/news/ch/202601/t20260116_311925.html); [量子位, Jan 2026](https://www.qbitai.com/2026/01/369878.html)
- **[Co. claim, 2026-02-12] Spring Festival "30亿大免单"** (a ¥3 billion free-order campaign). Over 6 days, users said "千问帮我" ("Qwen, help me") **4.1 billion** times, and AI completed **more than 120 million orders**.
  - Taobao Shangou says nearly half of the "one-sentence takeout" orders came from county towns.
  - 1.56 million users aged 60+ tried AI takeout for the first time.
  - Inside Taobao Shangou, user and transacting-user numbers were reported as "above expectations".

  Sources: [新浪财经, 2026-02-12](https://finance.sina.com.cn/stock/t/2026-02-12/doc-inhmpqkp4064763.shtml); [扬子晚报, 2026-02-12](https://www.yzwb.net/news/ch/202602/t20260212_321578.html); [三易生活](https://www.3elife.net/Art/internet/202602/12/106596.html); [界面](https://m.jiemian.com/article/13923452.html). These are subsidised promotional volumes, not steady state.
- **[Official, Alibaba FY26 Q3 results, 2026-03-19]** Qwen consumer apps passed **300 million MAU** across app, web and PC. By the end of February, about **140 million users** had tried AI shopping for the first time through Qwen's agent features. — [观察者网, 2026-03-19](https://www.guancha.cn/economy/2026_03_19_810672.shtml); [新浪财经, 2026-03-20](https://finance.sina.com.cn/roll/2026-03-20/doc-inhrrreu3985478.shtml?froms=ggmp). QuestMobile's March 2026 mobile-only MAU was about 165 million, a different basis. — [今日头条/QuestMobile](https://www.toutiao.com/article/7636410841508495935/)
- **[Press] Later steps.**
  - **May 2026:** Qwen and Taobao were fully connected, so users can select, compare and buy Taobao goods inside Qwen. — [新浪财经, 2026-08-24](https://finance.sina.com.cn/stock/t/2026-08-24/doc-inipmnpm0038750.shtml); [澎湃: "淘宝把'半条命'交给千问"](https://m.thepaper.cn/newsDetail_forward_33183912)
  - **About 2026-06-03:** Qwen App reportedly opened to third-party Agents and Skills, with Luckin and KFC testing. — search summary attributing this to [网易/AI周报](https://www.163.com/dy/article/L130MO7B05566ZHB.html); attribution uncertain, needs verification.
  - **August 2026:** the Qwen Open Platform went live with more than ten categories of service providers joining as agents. It reportedly does not take part in the transaction ("不碰交易环节"). — [投资界, Aug 2026](https://news.pedaily.cn/202608/567583.shtml)
- **[Official, Alibaba FY27 Q1 results, Aug 2026]** Instant-retail revenue grew 45% year on year, driven by Taobao Shangou and Freshippo. AI is described as a "hidden line" in Taobao Shangou's progress. Third-party data put Taobao Shangou's share at about 42% **[Opinion/secondary]**. — [新浪财经, 2026-08-24](https://finance.sina.com.cn/stock/t/2026-08-24/doc-inipmnpm0038750.shtml)

#### ByteDance: Doubao Phone Assistant (豆包手机助手), GUI agent (December 2025) and the blocks against it
- **[Press, early Dec 2025] Launch and blocks.**
  - ByteDance and ZTE released a technical preview of 豆包手机助手 on the Nubia M153 engineering phone. System-level permissions let the agent simulate taps and typing across apps.
  - **WeChat restricted it first**: users saw "login environment abnormal" and were refused access.
  - Taobao, Xianyu, Alipay and several banking apps followed.
  - Commentary pieces also list Pinduoduo, Amap and **Meituan** among the apps that blocked it.

  Sources: [HK01, 2025-12-08](https://global.hk01.com/数码生活/60301489/豆包ai手机遭微信阿里限制-跨app自动操作引争议-官方宣布调整); [人人都是产品经理](https://www.woshipm.com/ai/6302872.html); [每经, 2025-12-11](https://www.mrjjxw.com/articles/2025-12-11/4177585.html); [扬子晚报, 2025-12-07](https://www.yzwb.net/news/txs/202512/t20251207_297903.html); [爱范儿](https://www.ifanr.com/1648444)
- **[Official statements, Dec 2025]**
  - **Doubao:** operating the phone with AI is a "lab feature" and no longer supports WeChat. It published 《关于调整AI操作手机能力的说明》 (a notice on adjusting the phone-operation capability), planning "规范化调整" (standardising adjustments) in some scenarios. Finance (banking and payments), point-farming and competitive-game scenarios were restricted. Payment steps are handed back to the user.
  - **WeChat:** it took no Doubao-specific action, and the blocks were its existing risk controls firing.
  - **ByteDance:** said it was working with app developers on unified rules.

  Sources: [人人都是产品经理](https://www.woshipm.com/ai/6302872.html); [HK01](https://global.hk01.com/数码生活/60301489/豆包ai手机遭微信阿里限制-跨app自动操作引争议-官方宣布调整); [TechEDT, 2025-12-08](https://www.techedt.com/bytedance-faces-growing-resistance-as-chinese-apps-block-its-ai-driven-smartphone)
- **[Press] Meituan bookings via Doubao.** A diner who booked a restaurant through Doubao was refused a table. The store said online reservations come only through Meituan and Dianping. Doubao customer service confirmed that **Meituan does not currently support restaurant reservations through Doubao AI**. — [HK01](https://global.hk01.com/大国小事/60351902/顾客用豆包ai订位-遭店员拒入座调侃怒打差评-网民嘲-怎么敢信) (article date not captured)
- **[Press, 2026-08-10] Doubao's channel fee on hotels.** Hotel orders that go from Doubao to Douyin Laike (抖音来客) now carry a separate rate of **about 12%**: an 11.4% software service fee plus a 0.6% payment fee. Before this they counted as Douyin natural traffic at **8%**.
  - The basis is a Douyin Life Services notice of 2026-07-27, effective 2026-08-10.
  - Doubao's PR lead said the claim that "Doubao charges for recommending hotels" is inaccurate. Doubao charges no advertising fee, and merchants cannot pay to influence ranking. Merchants pay a channel service fee only on completed orders.
  - OTA commissions typically run 10–15%.
  - A 2026 industry report says only 15.2% of users buy directly after an AI travel recommendation, while 66.2% verify on an OTA.
  - QuestMobile: Doubao 382 million MAU, Douyin 1.029 billion.

  Sources: [界面, 2026-08](https://m.jiemian.com/article/14902227.html); [观察者网, 2026-08-11](https://www.guancha.cn/economy/2026_08_11_826852.shtml); [第一财经](https://www.yicai.com/news/103314921.html)
- **[Press, July 2026; clarification only]** Meituan internally restricted staff use of the *Doubao model*, moving teams to LongCat, DeepSeek and others. It had already stopped recommending Alibaba Cloud's Qwen models in April 2026. This concerns which models Meituan uses internally, not whether outside agents can reach Meituan. — [凤凰科技](https://tech.ifeng.com/c/8uQgWPqBijG); [东方财富](https://finance.eastmoney.com/a/202607023791674790.html). Separately, the July 2026 "Doubao and Qwen take agent features offline" story concerned persona-chat bots, not commerce agents. — [澎湃](https://m.thepaper.cn/newsDetail_forward_33526597); [知乎](https://zhuanlan.zhihu.com/p/2057571770346033260)

#### Tencent: WeChat AI ecosystem, Yuanbao (元宝), WorkBuddy, WeChat Pay AI card, and Meituan
- **[Official, Meituan Q1 2026 results call, reported about 2026-06-01]** Wang Xing said Meituan's 小美 agent will "deeply cooperate" with Tencent Yuanbao. When a user voices a local-services need in Yuanbao, Yuanbao triggers **Agent-to-Agent** communication with 小美, which then calls Meituan's takeout ordering and delivery. — [界面](https://m.jiemian.com/article/14525559.html); [虎嗅, 2026-06-09](https://m.huxiu.com/article/4865696.html); [澎湃](https://www.thepaper.cn/newsDetail_forward_33297423). I found no post-launch user data.
- **[Official, 2026-06-08/09]** WeChat opened its AI ecosystem to developers. JD and Meituan were among the first internal testers, and Meituan says it co-developed and tested its integration with the WeChat team. — [新浪财经, 2026-06-09](https://finance.sina.com.cn/roll/2026-06-09/doc-iniauvau3247861.shtml); [财联社](https://www.cls.cn/detail/2395189); [证券时报](https://www.stcn.com/article/detail/3951063.html)
- **[Official, 2026-06-17] WeChat Pay "AI专属卡" (AI-dedicated card).**
  - It sits inside WeChat Change (微信零钱) as a separate account, isolated from the user's main WeChat Pay account. Agents can spend only the card balance.
  - WeChat says the card does not hand over passwords or let agents draw funds directly. The agent buys within a scope the user sets and with the user's permission.
  - **The first integration was Tencent's desktop office agent WorkBuddy** (PC version 5.1.1+, Mac only at launch). More agent platforms are planned.

  Sources: [量子位, 2026-06](https://www.qbitai.com/2026/06/436160.html); [新浪财经, 2026-06-17](https://finance.sina.com.cn/roll/2026-06-17/doc-inicttai6901370.shtml); [网易](https://www.163.com/dy/article/KVKS04RJ05198UNI.html)
- **[Official, 2026-06-17] WorkBuddy and Meituan (same day).**
  - Meituan built dedicated capabilities for WorkBuddy to buy **dining group-buy deals (团购)** and provides **exclusive coupons**.
  - Users summon a "美团生活助手" (Meituan life assistant) expert in WorkBuddy and pick deals in natural language.
  - The AI initiates payment, the user confirms on their phone, and the amount is debited from the AI card. The voucher is redeemed in store.

  Sources: [每日经济新闻, 2026-06-17](https://www.nbd.com.cn/articles/2026-06-17/4429249.html); [网易](https://www.163.com/dy/article/KVKROJOR0511FQ39.html). **Verification of the brief's premise:** the card's first user was WorkBuddy. Meituan was the first *merchant/service* partner in WorkBuddy, not a second card integrator. The use case is in-store group-buy, **not takeout**. No commission or fee terms were disclosed.

#### Alipay: agent-payment products
- **[Official/Co. claim]**
  - **Jan 2026:** ACT 1.0 (above).
  - **April 2026:** ACT 2.0, built with the IIFAA trusted-authentication alliance, Zhipu and 20+ other firms, plus an "AI付智能安全系统" (AI Pay intelligent security system). — [IT之家](https://www.ithome.com/0/973/895.htm); [IT之家](https://www.ithome.com/0/973/400.htm)
  - **2026-05-26:** Alipay said it had processed **more than 300 million AI-agent payments**, supports 95% of general-purpose agents, and launched an AI wallet and Token Pay. — [腾讯新闻, 2026-05-26](https://news.qq.com/rain/a/20260526A079BK00)
  - **2026-06-16:** Alipay launched an AI version called "阿宝" (Abao). — [网易](https://www.163.com/dy/article/KVKS04RJ05198UNI.html)
  - **Bund Summit, Sept 2026:** the APASS commerce-trust infrastructure, built on **KYA (Know Your Agent)**, covering agent identity, intent, authorisation and behaviour. — [新京报](https://www.bjnews.com.cn/detail/1783910091129823.html); [品玩](https://www.pingwest.com/w/310777)

#### Regulation and industry standards (China)
- **[Official, 2026-08-24]** The China Payment & Clearing Association (中国支付清算协会) issued the 《智能体支付应用自律公约》 (Self-Regulatory Convention on Agent Payment Applications), developed under PBOC guidance and effective on issue. — [新华网, 2026-08-24](https://www.news.cn/fortune/20260824/b44c884bab5d4f89bce4262c362b0736/c.html). Key provisions per [第一财经, 2026-08-25](https://www.yicai.com/news/103331509.html):
  - It covers members providing account management, transaction processing, acquiring and switching/clearing in agent-payment applications.
  - **Account management, transaction processing and fund settlement must be done by licensed institutions.**
  - **"谁提供支付服务谁负责"**: whoever provides the payment service is responsible for account, fund and information security.
  - Members must sign clear authorisation agreements with users and **strictly verify user identity and transaction intent**.
  - Tiered, classified management with **transaction limits**.
  - Members must explore **KYA** and manage the risk of working with external agent developers.
  - Applications where **agents initiate payments autonomously** (both "preset-condition" and "general-condition" autonomous payment) **must be filed with the association before launch**, pass security testing, and keep human service, monitoring and rollback mechanisms.
  - Analysts describe three stages: agent-assisted payment → autonomous payment under preset conditions → autonomous payment under general conditions. Fully autonomous payment had not yet been deployed at scale.

  PBOC deputy governor Lu Lei stressed risk bottom lines and consumer protection at the 15th China Payment & Clearing Forum. — [中国金融信息网](https://www.cnfin.com/hb-lb/detail/20260825/4459763_1.html); [北京商报](https://www.bbtnews.com.cn/2026/0825/603375.shtml)

### Inferences
- **China's channel structure is ecosystem-bound, not open-agent.**
  - **Alibaba** owns agent, platform and payment. Qwen orders route to Taobao Shangou, a direct Meituan competitor, so for Alibaba the "agent" is a new first-party front door, not a third party. It even pays heavy subsidies to drive AI ordering.
  - **Tencent** owns the agents (Yuanbao, WorkBuddy, WeChat AI) and payment (WeChat Pay) but not local-services fulfilment. It therefore *invites* Meituan and JD in as service providers.
  - **ByteDance** owns an agent (Doubao) and its own local-services platform (Douyin Laike). Its GUI agent was blocked by everyone, so it monetises Doubao as a **paid traffic channel** into Douyin: 12% on hotels, up from 8%.
- **Meituan's terms when it is "invoked by someone else's agent":** it answers through **its own agent (小美) over A2A**, or through capabilities it builds for a partner (WorkBuddy). It keeps the transaction, coupons, fulfilment and account, and nothing in the record shows it paying a commission to the agent. Its 价值创造与传递 (value creation and delivery) and its commission-based 价值获取 (value capture) stay intact. Some 价值主张 (value proposition) moves to the Tencent agent as the "interface", in exchange for traffic from an ally.
- **The regulation favours incumbents.** The CPCA convention's licensed-institution, KYA, limit and pre-filing rules favour Alipay and WeChat Pay, the incumbent licensed wallets. They also keep the "user confirms payment" model as the default. This structurally limits independent agent makers from taking the payment relationship, and therefore the customer, away from licensed wallets and platforms.

### Gaps
- No official Alibaba figures on Qwen-originated *takeout* orders after the February 2026 campaign, or on repeat rates once subsidies ended.
- No disclosure of whether Taobao Shangou pays Qwen any internal transfer price or fee.
- I could not verify the date of Meituan's refusal of Doubao bookings, or whether Meituan formally blocks Qwen and Doubao GUI automation. Lists of blocking apps that include Meituan come from commentary pieces.
- The Yuanbao–小美 A2A integration's launch status, volume and fee terms are not public. Nor are the commercial terms of the WeChat AI ecosystem: commission, ads, or whether WeChat charges service providers.
- I did not obtain the full text of the CPCA convention. Its PBOC-guidance wording comes from Xinhua, while Yicai's summary does not mention the PBOC.
- I did not research non-China rules such as the EU or US CFPB/FTC on agent payments; none surfaced.

---

## 4. Evidence on usage and economics: share of orders and traffic via agents, conversion, disclosed volumes, fees, forecasts

### Takeaway
Agent-*referred* traffic is growing fast from a small base and converts well when it lands on the merchant's own site: Adobe holiday 2025 shows +693% traffic and conversion 31% above non-AI sources. Agent-*completed* checkout converts poorly: Walmart said in-ChatGPT checkout converted at about a third of the click-out rate, in March 2026. For US food delivery, agent-placed orders are negligible: DoorDash reported "several hundred" in total as of October 2026, against roughly a billion orders a quarter. China's large numbers (Qwen's 120 million orders in 6 days; Alipay's 300 million agent payments) are company claims, largely driven by subsidies or ecosystem promotion. The 2030 forecasts span $190B–$1T for US e-commerce. They mostly cover retail goods and measure "influenced" or "orchestrated" spend, not agent-*completed* orders.

### Cited Findings

#### Usage and conversion
- **[Co. claim, 2026-10-29]** DoorDash: "maybe several hundred orders in total" from third-party agents. — [TechCrunch](https://techcrunch.com/2026/10/30/doordash-said-its-only-received-several-hundred-orders-from-ai-agents/). Secondary coverage sets this against about **993 million** Q3 2026 orders. — [1023 Jack](https://1023jack.com/news/doordash-delivered-993-million-orders-last-quarter-ai-agents-placed-several-hund/); [BigGo Finance](https://finance.biggo.com/news/e59622e6-e7f9-4349-9e94-89b80b7bbb1c)
- **[Press, Mar 2026]**
  - Instacart said only "a very small percentage" of its orders come directly from AI agents.
  - Etsy said Instant Checkout did not generate a large volume of sales.
  - Lowe's declined to share figures.

  Source: [Modern Retail, 2026-03-27](https://www.modernretail.co/technology/what-went-wrong-with-chatgpts-instant-checkout/)
- **[Co. claim, reported 2026-03-18 by WIRED]** Walmart EVP Daniel Danker said in-ChatGPT Instant Checkout converted at **one-third** the rate of click-through purchases on Walmart.com, and called the experience "unsatisfying". Shoppers "generally don't want to split the checkout experience." — [MarTech, 2026-03-20](https://martech.org/walmart-says-chatgpt-checkout-converted-3x-worse-than-its-own-website/); [Search Engine Land](https://searchengineland.com/walmart-chatgpt-checkout-converted-worse-472071). Secondary reports add that ChatGPT brings Walmart new customers at about twice the rate of search engines, and that early Sparky-in-ChatGPT conversion is about 70% of Walmart.com. Both are single reports and low confidence. — [PPC Land](https://ppc.land/walmarts-chatgpt-checkout-flopped-heres-what-comes-next/)
- **[Co. claim, Adobe Analytics, published 2026-01-13] US holiday 2025 (Nov–Dec).**
  - Generative-AI referrals to US retail sites rose **693%** year on year: 769% in November, 673% in December.
  - AI referrals converted **31% better** than non-AI traffic, and 54% and 38% better on Thanksgiving and Black Friday.
  - Revenue per visit was 32% higher. In 2024, non-AI traffic had been worth 51% more.

  Sources: [Digital Commerce 360, 2026-01-13](https://www.digitalcommerce360.com/2026/01/13/generative-ai-online-holiday-shopping-traffic-2025/amp/); [Adobe blog](https://business.adobe.com/blog/ai-driven-traffic-surges-across-industries). **Caveat:** Adobe gives neither the baseline volume nor AI's share of total traffic or revenue. — [Practical Ecommerce](https://www.practicalecommerce.com/early-ai-signals-from-holiday-sales)
- **[Co. claim, Salesforce, 2025-12-05] Cyber Week 2025 (Nov 25 – Dec 1).**
  - Global online sales: $336.6B (+7%).
  - "AI and agents" influenced **$67B, or 20%** of purchases, through recommendations and conversational service. This is an attribution of influence, not agent-completed orders.

  Sources: [Salesforce press release](https://www.salesforce.com/news/press-releases/2025/12/05/cyber-week-ai-agents-sales/); [Salesforce IR](https://investor.salesforce.com/news/news-details/2025/Salesforce-Data-AI-and-Agents-Propel-Cyber-Week-to-Record-336-6B-in-Global-Spend/). Bain cites Salesforce data that AI and agents influenced $3B of US Black Friday sales. — [Bain](https://www.bain.com/insights/2030-forecast-how-agentic-ai-will-reshape-us-retail-snap-chart/)
- **[Co. claim, China]**
  - Qwen: 120 million+ AI orders in 6 days (February 2026), and about 140 million first-time AI shoppers by end-February (§3).
  - Alipay: 300 million+ AI-agent payments by 2026-05-26 (§3).
  - Doubao travel: 15.2% of users buy directly after an AI recommendation, while 66.2% check on OTAs. — [界面](https://m.jiemian.com/article/14902227.html)
- **[Official/Press]** Visa cites "hundreds of controlled transactions" through Visa Intelligent Commerce in live settings in 2026. — [TechInformed](https://techinformed.com/visa-opens-one-integration-for-ai-agent-payments/)

#### Fees merchants pay agent platforms
- OpenAI Instant Checkout: 4% for Shopify merchants from January 2026, withdrawn in March 2026 **[SUPERSEDED]**. — [ecommercenews.asia](https://ecommercenews.asia/story/openai-to-charge-4-fee-on-shopify-chatgpt-checkout); [Agentic Commerce Feed](https://www.agenticcommercefeed.com/blog/2026-03-16-openai-kills-instant-checkout)
- Doubao → Douyin Laike hotels: about 12% (11.4% + 0.6%) from 2026-08-10, versus 8% before. — [界面](https://m.jiemian.com/article/14902227.html)
- Qwen Open Platform: reportedly takes no part in the transaction. — [投资界](https://news.pedaily.cn/202608/567583.shtml)
- Microsoft Copilot Checkout, Google UCP checkout, Perplexity Instant Buy and Alexa+ food ordering: no merchant fee found (see Gaps).

#### Forecasts for agentic commerce by 2030 **[Forecast]**
- **McKinsey (Oct 2025):** up to **$1 trillion** of US B2C retail revenue "orchestrated" by agents by 2030, and **$3–5 trillion globally**. The global figure **covers goods only and excludes services** and B2B. — [McKinsey](https://www.mckinsey.com/industries/retail/our-insights/the-agentic-commerce-opportunity-how-ai-agents-are-ushering-in-a-new-era-for-consumers-and-merchants); date per [ChatterGo](https://www.chattergo.com/blog/mckinsey-agentic-commerce-opportunity-2025)
- **Morgan Stanley (Dec 2025):** agentic shoppers at **10–20% of US e-commerce by 2030**, meaning **$190B** in the base case and **$385B** in the bull case. — [Paz.ai](https://www.paz.ai/blog/morgan-stanleys-385b-bet-why-agentic-commerce-is-e-commerces-biggest-opportunity-since-mobile); [Stellagent](https://stellagent.ai/insights/agentic-commerce-market-size-forecast-2030). Both are secondary; I did not access the primary report.
- **Bain (2025-12-17):**
  - US agentic commerce of **$300–500B by 2030**, about **15–25% of US e-commerce**. AI will "complete up to a quarter of transactions".
  - The definition covers purchases "initiated, influenced, or completed" by third-party or retailer-hosted agents, and excludes journeys that only use AI-assisted search.
  - 30–45% of US consumers already use generative AI for product research.

  Source: [Bain](https://www.bain.com/insights/2030-forecast-how-agentic-ai-will-reshape-us-retail-snap-chart/)

### Inferences
- **Value capture today:** agent channels add almost no incremental orders for US delivery platforms. The economic question for the report is about *option value and risk*: who wins the discovery layer, and whether platforms must later *pay* for it, as they pay Google and Meta today.
- **The conversion gap** (referred traffic converts better, in-agent checkout converts worse) is the empirical reason the US market moved to "agent refers, platform checks out". It favours platforms keeping 价值获取 (value capture).
- **China's big numbers come from agent and platform being the same company or allies.** Alibaba subsidises its own Qwen to Taobao Shangou funnel, and Alipay counts agent payments across its own ecosystem. They show what *integrated* agent commerce can do. They are not evidence that independent third-party agents disintermediate Meituan.
- **Forecasts are mostly about retail goods.** McKinsey's global number explicitly excludes services, and Bain and Morgan Stanley define the market by e-commerce share. None isolates restaurant delivery or local services, so applying them to DoorDash or Meituan means extrapolating.

### Gaps
- I found no disclosed order volumes or GMV for orders placed through ChatGPT apps, Gemini automation or Alexa+ by DoorDash, Uber Eats or Instacart. DoorDash's "several hundred" covers *third-party agents* and may not include its own ChatGPT app.
- I found no figure for the share of DoorDash, Meituan or Swiggy traffic referred by AI.
- No conversion data specific to food delivery.
- I did not access the Morgan Stanley primary report. I found no China-specific forecast for agent-commerce size.
- I found no Adobe or Salesforce 2026 mid-year updates, and holiday 2026 data does not exist yet as of 2026-11-03.

---

## 5. Strategic analysis: do agents commoditize aggregators like DoorDash and Meituan, or reinforce them?

### Takeaway
The commoditisation case comes from Citrini Research, Nilay Patel's "DoorDash problem", Ben Thompson's agent-bypass remarks and a16z-linked ad-model arguments. They argue that agents remove habitual loyalty and ad and upsell surfaces, and that they compare prices across apps. The reinforcement case comes from DoorDash ("battle for atoms"), Uber (the metasearch analogy) and Bain ("retain data, fulfilment, checkout"). It is backed by what actually happened in 2025–26: Instant Checkout's failure and the negligible agent order volumes. As of November 2026 the evidence leans toward fulfilment-heavy local-commerce aggregators keeping value capture in the near term. Their discovery and ad layer is the most exposed part, and China shows that agent and platform can be vertically merged (Alibaba) or allied (Tencent–Meituan).

### Cited Findings
- **[Opinion, Stratechery concept]** Aggregators, by owning demand, gain power over suppliers, which become modularised and commoditised. — [Stratechery: Commoditizing Suppliers](https://stratechery.com/concept/aggregation-theory/commoditizing-suppliers/page/2/)
- **[Opinion, Stratechery, 2026-01-12, secondhand]** In "Apple and Gemini, Foundation vs. Aggregation, Universal Commerce Protocol", Thompson discusses Google using Gemini's distribution, including on Apple devices, to monetise through UCP. — [summary note](https://hq-raydata-co.pages.dev/vault/06-reference/2026-01-13-stratechery-apple-gemini-ucp/) (primary not accessed). In a conversation with Stripe's John Collison, Thompson said AI agents may bypass aggregators entirely and go straight to suppliers, which would change the power dynamic. — [LAVX summary](https://news.lavx.hu/article/ben-thompson-on-aggregation-theory-ai-agentic-commerce-and-the-future-of-saas) (secondary; date not confirmed)
- **[Opinion, 2025-11-20] Decoder, "The DoorDash Problem".** Agents between service providers and consumers abstract the customer relationship and turn providers into databases, threatening reviews, ads, loyalty, upsells and subscriptions. Summaries call Uber CEO Khosrowshahi's answer the most cogent among the CEOs interviewed. — [Decoder listing](https://metacast.app/podcast/decoder-with-nilay-patel/HFyRnpbJ/the-doordash-problem-how-ai-browsers-are-a-huge-threat-to-amazon/x03xTQ5t); [MBI Deep Dives, "Solving the DoorDash Problem"](https://www.mbi-deepdives.com/solving-the-doordash-problem/)
- **[Opinion/equity research, Feb–Mar 2026] Citrini Research, "The 2028 Global Intelligence Crisis".**
  - Agents would compare DoorDash, Uber Eats, restaurant sites and new entrants, picking the cheapest and fastest each time. "Habitual loyalty… simply didn't exist for a machine."
  - Co-author Alap Shah: DoorDash takes about 25% of the total order across customer and restaurant. A ChatGPT-style agent taking 7%, with restaurants paying 7% instead of about 15%, would save consumers about 11%.
  - Driver-side multi-app dashboards would fragment supply, with drivers keeping 90–95% of the fee. Shah sees this as "likely a 2027 event".
  - DoorDash shares fell about 7% on the day.

  Source: [Stocktwits, updated 2026-03-05](https://stocktwits.com/news-articles/markets/equity/doordash-moat-under-threat-citrini-research-warns-consumer-ai-agents-bypass-delivery-apps/cZRvn0mR4zK)
- **[Official/management view] DoorDash:** "you never want to fight gravity… [but don't] want to be delusional". DoorDash wants healthy relationships with agent makers and may advertise inside agents. — [TechCrunch, 2026-10-30](https://techcrunch.com/2026/10/30/doordash-said-its-only-received-several-hundred-orders-from-ai-agents/). It frames agents as top-of-funnel partners, like Meta and Google. — [Barchart](https://www.barchart.com/story/news/454746/is-doordash-stock-set-to-be-the-next-major-ai-victim) (secondary)
- **[Official/management view] Uber (2026-05-06):** it will work with Apple, OpenAI, Claude and Gemini agents, but expects most demand to stay direct, as happened with metasearch. — [Diginomica](https://diginomica.com/node/30465)
- **[Opinion] Bain (2025-12-17):** spec-driven agent shopping will "quickly disrupt multi-brand retailers". Leaders should set an agentic strategy now to retain "as much ownership of data, fulfillment, and checkout as possible." — [Bain](https://www.bain.com/insights/2030-forecast-how-agentic-ai-will-reshape-us-retail-snap-chart/)
- **[Opinion, a16z-linked, secondary]**
  - a16z's Justine Moore and Alex Rampell argue AI displaces informational search first, then moves into other commerce categories. — [a16z via Rosetta](https://rosetta.to/u/a16z/the-death-of-search-how-shopping-will-work-in-the-age-of-ai)
  - An a16z crypto–linked piece argues autonomous shopping agents could undercut the online ad model and that the future hinges on open protocols. — [Gentic News](https://www.gentic.news/article/agentic-ai-commerce-platforms-a16z-argues-autonomous-agents-could-end-the-online)
  - a16z partner Noah Levine: "Agentic commerce won't start with shopping". — [podcast listing](https://podcast.app/agentic-commerce-wont-start-with-shopping-noah-levine-partner-a16z-e455182267)
  - I found no a16z or Sequoia essay on "who owns the customer" specifically.
- **[Opinion, Mar 2026]** Forrester on OpenAI's retreat: "What It Means That The Leader In 'Agentic Commerce' Just Pulled Back". — [Forrester](https://www.forrester.com/blogs/what-it-means-that-the-leader-in-agentic-commerce-just-pulled-back). IDC (2026-03-18): "Is there a play for OpenAI in agentic commerce?" — [IDC](https://www.idc.com/resource-center/blog/is-there-a-play-for-openai-in-agentic-commerce/). Adyen: whoever owns intent owns the customer, and brands fade when an intermediary controls discovery, decision and checkout. — [Adyen](https://www.adyen.com/en_AU/knowledge-hub/agentic-commerce-control)
- **[Opinion, China]**
  - 澎湃 described Qwen as an "AI super-entry" that has unified Alibaba's businesses as never before. — [澎湃](https://m.thepaper.cn/newsDetail_forward_32393627)
  - "Taobao hands 'half its life' to Qwen". — [澎湃](https://m.thepaper.cn/newsDetail_forward_33183912)
  - Analysts read Doubao's move from "natural traffic" to "independent channel" as the formal start of commercial rules for AI entry points in local-services transactions. — [界面](https://m.jiemian.com/article/14902227.html)
  - The Yuanbao–Meituan link is described as a template for WeChat's agent ecosystem. — [虎嗅](https://m.huxiu.com/article/4865696.html)

### Inferences
- **Under the 价值主张 / 价值创造与传递 / 价值获取 framing (value proposition / value creation and delivery / value capture), as of November 2026:**
  - **价值主张 (value proposition)** is partly contestable. "Help me decide what to eat or buy" can migrate to a general agent: ChatGPT, Gemini, Qwen, Yuanbao. "Get it to me in 30 minutes, cheaply, reliably" cannot.
  - **价值创造与传递 (value creation and delivery)** stays with the platform in every observed case: the logistics network, merchant supply, pricing and subsidies, customer service and refunds. Agents are thin front-ends that call the platform's APIs or drive its app.
  - **价值获取 (value capture):** commissions and delivery fees are intact. *Ad revenue* (sponsored listings) and *membership upsell* are what agents can erode, and *customer-acquisition cost* may rise if platforms must pay agents for placement (DoorDash's "advertise within agents"; Doubao's 12% channel fee).
- **Conditions that would flip toward commoditisation:**
  1. Agents get cross-platform price and ETA comparison with *transacting* rights. Today they mostly get one platform's account per integration.
  2. Agent-side payment rails mature. Visa and Mastercard tokens, AP2 and ACP shared tokens make this technically possible, but China's convention requires licensed wallets and user confirmation.
  3. Supply-side multi-homing (merchants and couriers) becomes cheap. Citrini thinks this is about 2027; there is no evidence of it yet.
  4. The legal environment lets user-directed GUI agents operate without consent. The Ninth Circuit's August 2026 ruling moves the US in this direction, while China's platforms and payment rules move the other way.
- **Meituan-specific:** Meituan's position looks more defensible than DoorDash's against *neutral* third-party agents, because Chinese agents are tied to ecosystems. Meituan's risk is ecosystem competition instead: Alibaba's Qwen sends AI-originated demand to Taobao Shangou, so the agent *is* a rival's front door. Its hedge is the Tencent alliance, running its own 小美 agent over A2A inside Yuanbao, WeChat and WorkBuddy.
- **DoorDash-specific:** US agents are more platform-neutral (OpenAI, Google, Amazon, Perplexity), so the commoditisation risk is in principle higher. Mitigants:
  - negligible current volume
  - account-linked integrations
  - the March 2026 retreat from agent-owned checkout
  - DoorDash's scale in fulfilment, now plus Instacart, if the deal closes

### Gaps
- I did not access a primary Stratechery article that applies aggregation theory specifically to DoorDash or Meituan versus agents. The Thompson views above are secondhand.
- I found no named sell-side note (Morgan Stanley, Bernstein, Goldman and the like) quantifying agent risk to DoorDash's or Meituan's take rate or ad revenue. Citrini is independent research.
- I found no Sequoia piece on agentic commerce.
- I found no Chinese sell-side quantification of Qwen's impact on Meituan's takeout share. The 42% Taobao Shangou share figure is unattributed third-party data reported by Sina.
