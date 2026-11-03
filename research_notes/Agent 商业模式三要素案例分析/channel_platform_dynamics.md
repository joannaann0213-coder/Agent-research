# Platform Dynamics of AI Agents Embedded in Communication Channels (2025–2026)

*Compiled 2026-11-03. Evidence tags: **[Official]** = company filing, earnings transcript, product blog, developer docs or regulator release; **[Report]** = established press (TechCrunch, CNBC, Engadget, 《财经》, 澎湃, 虎嗅, 光明网, etc.); **[3P]** = third-party blog, vendor explainer or aggregator (lower confidence; verify before quoting); **[Opinion]** = analyst or commentator judgment. **SUPERSEDED** marks a fact that a later event changed. Other researchers profile Claude in Slack (Claude Tag), the @grok bot on X and Tencent WorkBuddy; they appear here only for the platform angle. Archetypes: (A) a model vendor inside someone else's channel; (B) the channel owner also owns the model; (C) an ecosystem giant's work agent across its own office/IM suite.*

## 1. Gatekeeping moves by channel owners: what changed, when, on what terms

### Takeaway
Between May 2025 and mid-2026 the major channel owners used much the same toolkit against third-party agents:
1. They cut off bulk or archival data access and use of their data for model training (Slack, X).
2. They routed distribution through curated stores or partner-only APIs (Slack Marketplace with RTS/MCP; Microsoft Agent Store, Certification and Agent 365).
3. They charged per message or per call (WhatsApp €0.049–0.132 per message; X pay-per-use).
4. They limited how freely an agent can act inside conversations: X allows bot replies only when a bot is "summoned", the WhatsApp Business API has no group bots, WeChat blocks links, and ClawBot works only in 1:1 chats.
5. They favored their own assistants.

So far the only check that has worked is antitrust law in consumer messaging. Italy (December 2025), Brazil (January–April 2026) and the European Commission (interim measures, 9 June 2026) forced WhatsApp back open. No similar action was found against enterprise channels (Slack, Teams) or Chinese super-apps.

### Cited Findings

#### Slack (Salesforce): data lockdown plus partner-only real-time access
- **[Official]** On 29 May 2025 Slack changed its API Terms. Slack now states that "the Slack Marketplace is the only appropriate channel for commercially distributing apps built with Slack APIs." It said it was "reinforcing safeguards around how third-party applications can store, use, and share data accessed via Slack APIs," and named the risk of "bulk downloads of customers' conversational data for indexing and querying." The terms took effect immediately for apps created on or after 29 May 2025, and on 30 June 2025 for older apps — [Slack developer changelog](https://docs.slack.dev/changelog/2025/05/29/rate-limit-changes-for-non-marketplace-apps/)
- **[Official]** Rate limits changed at the same time. For commercially distributed non-Marketplace apps, `conversations.history` and `conversations.replies` dropped from Tier 3 to Tier 1: 1 request per minute, with the `limit` maximum and default cut to 15 objects. Internal customer-built apps keep 50+ requests per minute and 1,000 objects per request. Apps approved for the Marketplace are not affected — [Slack developer changelog](https://docs.slack.dev/changelog/2025/05/29/rate-limit-changes-for-non-marketplace-apps/)
- **Conflict (unresolved): when the limits apply to existing installs.**
  - The current changelog text says "The new rate limits will not be applied to existing installations of unlisted, distributed applications" — [Slack developer changelog](https://docs.slack.dev/changelog/2025/05/29/rate-limit-changes-for-non-marketplace-apps/)
  - A search-engine summary of the FAQ version of the same page says existing installs were first scheduled for 2 Sep 2025 and then moved to 3 Mar 2026 — [Slack FAQ URL (redirects to the changelog)](https://api.slack.com/changelog/2025-05-terms-rate-limit-update-and-faq). The date text itself could not be confirmed.
  - Glean dates its own forced cutover to 3 Mar 2026 (see below).
- **[Report, legal analysis]** According to Hunton Andrews Kurth, the amended terms prohibit "bulk exporting of data accessible through the Slack application programming interfaces (APIs)," "the creation of persistent copies, archives, indexes or long-term data stores," and "the usage of such data in large language models (LLMs)." Third parties may access data only "on a query-by-query basis." Glean warned customers that the change would "lock[] your data within Slack" and limit results to the scope and quality of Slack's own search — [Hunton Andrews Kurth, 28 Jul 2025](https://www.hunton.com/insights/legal/salesforce-locks-down-slack-data-time-to-review-your-slack-api-terms)
- **[Opinion]** Hunton reports that "many in the industry" see the change as a way for Salesforce to keep Slack's conversational data for its own AI products — [Hunton Andrews Kurth](https://www.hunton.com/insights/legal/salesforce-locks-down-slack-data-time-to-review-your-slack-api-terms)
- **[Official]** Slack describes the Real-Time Search (RTS) API as allowing "real-time querying of Slack data without storing it." In May 2025 it was a limited beta "with select partners" — [Slack developer changelog](https://docs.slack.dev/changelog/2025/05/29/rate-limit-changes-for-non-marketplace-apps/). **SUPERSEDED** by GA (next bullet).
- **[Official]** Slack's MCP server and the RTS API had a limited release in October 2025 and became generally available on 17 Feb 2026.
  - Slack says "more than 50 partners" are building on them, "including Anthropic, Google, OpenAI, Perplexity."
  - Named Marketplace agents include Claude, Claude Code, Perplexity, Cursor, Manus, Guru, Moveworks, WRITER, Spotter (ThoughtSpot) and Credal AI.
  - RTS queries and MCP tool calls rose 25x after the October limited release.
  - Slack says the MCP server "respects user permissions" and RTS works without "storing customer data on external servers."
  - Source: [Slack blog, 17 Feb 2026](https://slack.com/blog/news/mcp-real-time-search-api-now-available)
- **[Official]** Slack's developer docs list Claude.ai, Claude Code, Perplexity and Cursor as partner clients that can use the MCP server without coding — [Slack MCP server docs](https://docs.slack.dev/ai/slack-mcp-server/) (seen via search summary)
- **[Official, vendor docs]** Glean "discontinued use of Slack's Discovery APIs on March 3, 2026" and now supports Slack only through RTS. Each user must authorize the Glean Slack app individually to get permission-aware results, and semantic search works only on Slack plans that include Slack AI Search — [Glean docs](https://docs.glean.com/connectors/native/slack/setup/slack-rts-connector/) (via search summary)
- **[Official]** Slack's own agent: on 13 Jan 2026 Slackbot was relaunched as a personal AI agent, generally available on Business+ and Enterprise+ plans — [TechCrunch, 13 Jan 2026](https://techcrunch.com/2026/01/13/slackbot-is-an-ai-agent-now). Reports disagree on whether it carries extra consumption charges — [AI Business](https://aibusiness.com/agentic-ai/salesforce-updates-slack-for-ai-era) (3P; conflicting)

#### WhatsApp (Meta): ban, then a paid tier, then a forced reopening

| Date | Event | Source |
|---|---|---|
| Jul 2025 | Italy's competition authority (AGCM) opens an abuse-of-dominance case (Art. 102 TFEU) over Meta AI's integration into WhatsApp, "where it is given greater prominence than competing services" | [AGCM, 24 Dec 2025](https://en.agcm.it/en/media/press-releases/2025/12/A576) |
| Sep 2025 | Chatbot companies Luzia and Zapia complain to Brazil's competition authority, CADE | [Tecnoblog](https://tecnoblog.net/noticias/tribunal-do-cade-mantem-decisao-que-permite-ias-concorrentes-no-whatsapp/) |
| 15 Oct 2025 | New WhatsApp Business Solution Terms ban third-party general-purpose AI assistants from the WhatsApp for Business API, effective 15 Jan 2026 | [European Commission IP/26/1276](https://ec.europa.eu/commission/presscorner/api/files/document/print/en/ip_26_1276/IP_26_1276_EN.pdf); [TechCrunch, 18 Oct 2025](https://techcrunch.com/2025/10/18/whatssapp-changes-its-terms-to-bar-general-purpose-chatbots-from-its-platform) |
| Oct 2025 | OpenAI says ChatGPT on WhatsApp (1-800-ChatGPT) ends 15 Jan 2026; 50 million people had used it; users are told to link accounts because chats cannot be exported | [Engadget](https://www.engadget.com/ai/chatgpt-in-whatsapp-will-stop-working-in-january-161335173.html) |
| 25 Nov 2025 | AGCM widens the probe to cover the new terms and opens an interim-measures procedure | [AGCM](https://en.agcm.it/en/media/press-releases/2025/12/A576) |
| Dec 2025 (4 Dec per TechTimes) | European Commission opens a formal antitrust case, AT.41034 | [EC IP/26/1276](https://ec.europa.eu/commission/presscorner/api/files/document/print/en/ip_26_1276/IP_26_1276_EN.pdf); [TechTimes](https://www.techtimes.com/articles/320553/20260715/eu-antitrust-order-reopens-whatsapp-chatgpt-setting-precedent-ai-markets.htm) |
| 24 Dec 2025 | AGCM orders Meta to "immediately suspend the WhatsApp Business Solution Terms" in Italy | [AGCM](https://en.agcm.it/en/media/press-releases/2025/12/A576) |
| Jan 2026 | Meta opens the API to chatbots in Italy. CADE's General Superintendence imposes a preventive measure; a federal court suspends it days later | [TechCrunch, 5 Mar 2026](https://techcrunch.com/2026/03/05/meta-will-allow-rival-ai-chatbots-on-whatsapp-in-europe-but-for-a-fee/); [Mercado&Consumo](https://mercadoeconsumo.com.br/04/03/2026/noticias/cade-mantem-medida-preventiva-que-obriga-meta-a-permitir-chatbots-de-ia-no-whatsapp/) |
| 15 Jan 2026 | Ban takes effect wherever it is not suspended. ChatGPT, Copilot and Perplexity WhatsApp services shut down; the Copilot and Perplexity exits are reported only by secondary sources | [TechTimes](https://www.techtimes.com/articles/320553/20260715/eu-antitrust-order-reopens-whatsapp-chatgpt-setting-precedent-ai-markets.htm); [Galantis (3P)](https://whatsapp.galantis.com/blog/whatsapp-ai-chatbot-ban-2026) |
| Feb 2026 | The Commission issues a Statement of Objections signalling interim measures | [EC IP/26/1276](https://ec.europa.eu/commission/presscorner/api/files/document/print/en/ip_26_1276/IP_26_1276_EN.pdf) |
| Early Mar 2026 | CADE's tribunal unanimously upholds the preventive measure | [Tecnoblog](https://tecnoblog.net/noticias/tribunal-do-cade-mantem-decisao-que-permite-ias-concorrentes-no-whatsapp/) |
| 4–5 Mar 2026 | Meta readmits general-purpose chatbots in Europe "for the next 12 months" at €0.0490–€0.1323 per non-template message, depending on country | [EC](https://ec.europa.eu/commission/presscorner/api/files/document/print/en/ip_26_1276/IP_26_1276_EN.pdf); [TechCrunch](https://techcrunch.com/2026/03/05/meta-will-allow-rival-ai-chatbots-on-whatsapp-in-europe-but-for-a-fee/) |
| 11–17 Mar 2026 | Meta starts charging AI chatbots in Brazil from 11 Mar. CADE finds non-compliance from 17 Mar and imposes a R$250,000 daily fine, upheld on 30 Mar and again around 23 Apr 2026 | [Portal Tela](https://www.portaltela.com/noticias/economia/2026/03/30/cade-mantem-multa-diaria-de-r-250-mil-contra-whatsapp-por-ia/); [Mercado&Consumo, 23 Apr 2026](https://mercadoeconsumo.com.br/23/04/2026/noticias/cade-mantem-multa-diaria-contra-a-meta-no-caso-envolvendo-uso-de-chatbots-de-ia-no-whatsapp/amp/) |
| Apr 2026 | Supplementary Statement of Objections: the Commission says it intends to order reinstatement | [EC](https://ec.europa.eu/commission/presscorner/api/files/document/print/en/ip_26_1276/IP_26_1276_EN.pdf) |
| 9 Jun 2026 | Commission interim measures: Meta must restore free access on the pre-15 Oct 2025 terms within 5 working days and keep it until a final decision | [EC IP/26/1276](https://ec.europa.eu/commission/presscorner/api/files/document/print/en/ip_26_1276/IP_26_1276_EN.pdf) |
| 13 Jul 2026 | ChatGPT is restored on WhatsApp across the EEA (single outlet) | [TechTimes, 15 Jul 2026](https://www.techtimes.com/articles/320553/20260715/eu-antitrust-order-reopens-whatsapp-chatgpt-setting-precedent-ai-markets.htm) |
| Mid-2026 | Meta appeals to the EU General Court. Slaughter and May confirms the appeal; one report says it was filed in August 2026 | [Slaughter and May](https://www.slaughterandmay.com/recent-work/meta-on-its-appeal-against-the-european-commission-s-whatsapp-interim-measures-decision/); [Yahoo Finance](https://finance.yahoo.com/economy/policy/articles/eu-orders-meta-reopen-whatsapp-142603730.html) (3P summary) |

- **[Official] The Commission's preliminary findings ("at first sight"):**
  - Meta has held a dominant position "in the EEA-wide market for consumer communication applications since at least January 2023."
  - After the ban, "only Meta's own AI assistant, Meta AI, remained accessible on WhatsApp."
  - The ban was "a refusal to provide access to an infrastructure developed for and previously open to third parties."
  - The March 2026 fee "is in practice equivalent to the previous access ban."
  - This is only the second interim-measures decision under Regulation 1/2003, after Broadcom in 2009.
  - Possible penalties are fines up to 10% of turnover and daily penalty payments up to 5% of average daily turnover.
  - Teresa Ribera called WhatsApp "a key entry point to reach consumers in Europe."
  - Source: [EC IP/26/1276, 9 Jun 2026](https://ec.europa.eu/commission/presscorner/api/files/document/print/en/ip_26_1276/IP_26_1276_EN.pdf)
- **[Official]** AGCM's theory of harm is that the terms would "completely exclude Meta AI's competitors from the WhatsApp platform in the AI Chatbot services market." AGCM says it coordinates with the Commission. The Commission's decision covers the EEA except Italy, which AGCM already handles — [AGCM](https://en.agcm.it/en/media/press-releases/2025/12/A576); [Houthoff](https://www.houthoff.com/insights/news/a-rare-show-offorce-commission-deploys-interim-measures-against-meta-to-safeguard-ai-competition/)
- **[Report] Meta's stated reasons:**
  - The Business API exists so that businesses can provide customer support and send updates. Businesses that use AI to serve their own customers are not affected — [TechCrunch, 18 Oct 2025](https://techcrunch.com/2025/10/18/whatssapp-changes-its-terms-to-bar-general-purpose-chatbots-from-its-platform)
  - AI chatbots "strain systems" that the API was not built to support. The AI market is competitive, and AI companies can reach users through app stores, search engines, email, partnerships and operating systems — [TechCrunch, 5 Mar 2026](https://techcrunch.com/2026/03/05/meta-will-allow-rival-ai-chatbots-on-whatsapp-in-europe-but-for-a-fee/)
  - Meta made the same argument to CADE, saying chatbots use WhatsApp Business for an unintended purpose — [O Povo, 2 Feb 2026](https://www.opovo.com.br/noticias/economia/2026/02/02/meta-diz-ao-cade-que-chatbots-de-ia-se-aproveitam-do-whatsapp-business-para-uso-nao-previsto.html)
- **[Report]** Scale and scope: WhatsApp reported 3 billion MAU (May 2025), with roughly 500 million in the EEA (an estimate). As of mid-July 2026 only ChatGPT had announced a return. Meta has not been compelled to restore rival assistants outside the EEA (and Italy and Brazil), so WhatsApp remains closed to them in most of the world — [TechTimes](https://www.techtimes.com/articles/320553/20260715/eu-antitrust-order-reopens-whatsapp-chatgpt-setting-precedent-ai-markets.htm)
- **[3P, unverified]** One report says the interim measures last until the investigation ends or June 2029, whichever comes first. This is not in the Commission's press release — [Yahoo Finance](https://finance.yahoo.com/economy/policy/articles/eu-orders-meta-reopen-whatsapp-142603730.html)
- **[Report]** The 2025 consumer bots worked only 1:1. Perplexity said group-chat support was on hold because the WhatsApp API did not support AI bots in groups — [Yahoo/Perplexity WhatsApp launch coverage](https://www.yahoo.com/news/perplexity-ai-joins-whatsapp-chatbot-144012062.html) (3P)

#### X (owned by xAI since March 2025): training ban, paying per call, and bots that must be "summoned"
- **[Report]** 5 Jun 2025: X's Developer Agreement now bars third parties from using "the X API or X Content to fine-tune or train a foundation or frontier model," under "Reverse Engineering and other Restrictions." The ban does not limit X's or xAI's own use of the data for Grok — [TechCrunch, 5 Jun 2025](https://techcrunch.com/2025/06/05/x-changes-its-terms-to-bar-training-of-ai-models-using-its-content)
- **[Report]** 15 Jan 2026: X head of product Nikita Bier banned apps that reward users for posting ("InfoFi") and revoked their API access. Kaito's Yaps program then shut down — [Protos](https://protos.com/nikita-bier-causes-collapse-of-crypto-infofi)
- **[Report/3P]** 23 Feb 2026: programmatic replies through `POST /2/tweets` are allowed only "if the original author @ mentions you or quotes your post."
  - The rule covers the Free, Basic, Pro and Pay-Per-Use plans. Enterprise and "Public Utility" apps are exempt.
  - Automated top-level posts are unchanged.
  - Bier called the campaign "Operation Kill the Bots."
  - Sources: [PiunikaWeb, 24 Feb 2026](https://piunikaweb.com/2026/02/24/x-api-blocks-automated-spam-replies/); [Roboin](https://roboin.io/article/en/2026/02/24/x-limits-api-based-automated-replies-to-combat-engagement-farming/)
- **[Official]** X's current API pricing page (pay-per-usage, USD):

  | Action | Price |
  |---|---|
  | Post read | $0.005 per post returned |
  | User lookup | $0.010 per user returned |
  | Post create | $0.015 per request |
  | Post create with a URL | $0.200 per request |
  | Post create "(summoned)" | $0.010 per request |
  | Owned reads | $0.001 per resource |

  Plans are capped at 3 million post reads per month, and higher volumes require Enterprise. Purchases "can earn up to 20% back in xAI API credits." The page lists no Basic or Pro subscriptions — [X API pricing docs](https://docs.x.com/x-api/getting-started/pricing)
- **[3P, verify]** Timeline of the move to pay-per-use (**SUPERSEDED**: the Basic $200/month and Pro $5,000/month tiers):
  - Pay-per-use launched around 6 Feb 2026.
  - Basic subscribers were moved over after 1 Jun 2026.
  - Pro was reportedly deprecated on 14 Aug 2026, with migration after 1 Sep 2026 (single source).
  - Most of the Free tier was discontinued; users got a one-time $10 voucher.
  - Sources: [WeAreFounders](https://www.wearefounders.uk/the-x-api-price-hike-a-blow-to-indie-hackers/); [Postproxy](https://postproxy.dev/blog/x-api-pricing-2026/)
- **[Official]** Automated accounts must display a label and be connected to a human-run account — [X Help: automated account labels](https://help.x.com/en/using-x/automated-account-labels)
- **[Official, competitor claim]** On 21 Oct 2026 Meta said "Threads has now overtaken X in monthly actives" — [Meta Q3 2026 call transcript](https://s21.q4cdn.com/399680738/files/content_files/META-Q3-2026-Earnings-Call-Transcript.pdf)

#### WeChat (Tencent): link policing that hits Tencent's own agent too, and one narrow official door for agents
- **[Report] Spring Festival 2026 red-envelope campaign:**
  - 4 Feb 2026: the WeChat Security Center restricted Tencent Yuanbao's red-envelope share links. It said the campaign induced users to share links into group chats at high frequency through "做任务、领红包" mechanics, and acted under its external-link content rules after user complaints.
  - 5 Feb: Baidu's 文心助手 links were blocked.
  - 6 Feb: Alibaba's 千问 links were blocked the day its "春节30亿免单" promotion launched.
  - 6 Feb: the text codes (口令) for 千问 and 元宝 could no longer be copied inside WeChat.
  - Sources: [光明网, 6 Feb 2026](https://tech.gmw.cn/2026-02/06/content_38582125.htm); [财联社/科创板日报](https://www.cls.cn/detail/2282814); [每日经济新闻](https://www.nbd.com.cn/articles/2026-02-06/4252015.html); [36氪](https://36kr.com/p/3671541805671045)
- **[Report]** Tencent told reporters it treats all apps alike ("一视同仁") — [IT之家](https://m.ithome.com/html/919857.htm). The ¥1 billion red-envelope push briefly lifted Yuanbao to 114 million MAU. WeChat blocked its links four days later, and DAU fell back to about 9 million after the holiday — [澎湃, 14 May 2026](https://m.thepaper.cn/newsDetail_forward_33176080)
- **[Report/3P] ClawBot plug-in:** around 22–23 Mar 2026 WeChat launched an official "ClawBot" plug-in (WeChat ≥ 8.0.70, under Me → Settings → Plugins). It lets users drive a self-hosted OpenClaw agent from inside WeChat — [腾讯云开发者社区 (original 23 Mar 2026)](https://developer.cloud.tencent.com/article/2711310); [腾讯云开发者社区](https://cloud.tencent.com/developer/article/2644003); [OpenClaw docs: WeChat channel](https://docs.openclaw.ai/zh-CN/channels/wechat)
- **[3P, unverified]** Tutorials describe the plug-in as public beta, 1:1 only (it cannot join group chats), limited by a 24-hour message "keep-alive" window, and subject to WeChat content-compliance rules — [知乎](https://zhuanlan.zhihu.com/p/2019698694069471000); [CSDN](https://blog.csdn.net/weixin_70573287/article/details/160147127)
- **[3P]** In WeCom (企业微信), AI "smart bots" reportedly cannot be added to external (customer) groups and work only in internal groups — [CSDN](https://damodev.csdn.net/6ae1cb7ee5f4ca4eff8cecf5.html)
- No formal Weixin Open Platform rulebook on third-party AI agents was found (see Gaps).

#### Microsoft Teams / Microsoft 365: a curated store, certification, and a fee for governing agents
- **[Official]** The Agent Store in Microsoft 365 Copilot brings Microsoft, partner and organization-built agents together. Admins can view an agent's certification or publisher attestation and decide what employees may install — [Microsoft Learn: Agent Store](https://learn.microsoft.com/microsoft-365/copilot/copilot-agent-store); [Microsoft Agent Store guide (PDF)](https://adoption.microsoft.com/files/agents/AgentStorePracticalGuide.pdf)
- **[Official]** Microsoft 365 Certification (blog, Nov 2025) evaluates third-party apps and agents for security, compliance, data handling and privacy. Certified agents get a badge in AppSource, the Teams Store and the admin center — [Microsoft Tech Community](https://techcommunity.microsoft.com/blog/microsoft_365blog/microsoft-365-certification/4465702)
- **[3P]** Agent 365, Microsoft's control plane for agents, reportedly became generally available on 1 May 2026 as a $15 per user per month add-on and is part of E7. From 1 Jul 2026, some Defender agent-protection features require an Agent 365 licence. Third-party agents appear in the dashboard through the Agent 365 SDK — [Rencore](https://rencore.com/en/blog/agent-365-is-a-baseline-not-a-strategy-what-it-governs-what-it-costs-and-what-it-leaves-exposed/); [Medium (D. van der Woude)](https://derkvanderwoude.medium.com/microsoft-agent-365-license-enforcement-july-1-2026-security-impact-summary-47d7f02b46be); [Microsoft Learn: Agent 365 overview](https://learn.microsoft.com/en-us/microsoft-agent-365/overview)
- **[3P, single source]** Microsoft is said to be retiring its current Copilot partner certifications in favor of agent-focused ones after July 2026 — [Collab365](https://spaces.collab365.com/posts/microsoft-retires-current-copilot-partner-certific-MQJMNq)

#### Telegram: the counter-model, where the channel sells access
- **[Official, Durov]** On 28 May 2025 Telegram CEO Pavel Durov announced a one-year partnership "to distribute Grok to Telegram's billion+ users." Telegram would receive "$300M in cash and equity from xAI, plus 50% of revenue from xAI subscriptions sold via Telegram" — [Durov on X](https://x.com/durov/status/1927697402095378432); [CNBC](https://www.cnbc.com/2025/05/28/elon-musk-xai-telegram-grok.html)
- **[Report]** Elon Musk replied "No deal has been signed." Durov then described the deal as agreed in principle with formalities pending — [The Block](https://www.theblock.co/post/356171/elon-musk-contradicts-telegrams-300m-xai-partnership-announcement-no-deal-has-been-signed); [Bloomberg](https://www.bloomberg.com/news/articles/2025-05-28/telegram-to-distribute-grok-in-300-million-xai-deal-durov-says)
- **[Report]** Planned features were Grok in the search bar, chat summaries, document digests, inbox agents and group-chat moderation. xAI's data access would be limited to what users share with Grok directly — [Cointelegraph](https://cointelegraph.com/news/elon-musk-xai-300-million-telegram-deal-grok-integration); [Android Police](https://www.androidpolice.com/telegram-xai-grok-year-deal-cash/)
- **[Official, Durov]** On 30 Nov 2025 Telegram's decentralized confidential-compute network Cocoon (on TON) went live. Telegram is its first client; some Telegram requests, such as message translation, are already routed through it — [Durov on X](https://x.com/durov/status/1995208789600182391); [Crypto Briefing](https://cryptobriefing.com/telegram-cocoon-ai-ton-blockchain-confidential-compute/)
- No 2025–2026 source confirmed that Grok actually shipped inside Telegram or that the $300M was paid (see Gaps).

### Inferences
- **The common gatekeeping toolkit has five levers.**
  - Data rights: no bulk indexing, LLM use or training on channel data (Slack, X).
  - Distribution rights: Marketplace-only distribution and partner-only real-time APIs (Slack); certified stores (Microsoft).
  - Price of access: per message (WhatsApp), per call (X).
  - Limits on presence in the conversation: X's summoned-only replies, no group bots on WhatsApp, WeChat's 1:1-only ClawBot, WeCom bots barred from customer groups.
  - Favoring the house agent: Meta AI pre-installed, Slackbot bundled into paid tiers, X paying API buyers back in xAI credits.
- **Channel owners prefer sanctioned access to banning outsiders.** The trend is toward permission-scoped, non-persistent, query-time access, such as Slack RTS/MCP, which "respects user permissions" and stores nothing. A third-party agent can still deliver value in the moment, but it cannot build the data asset (an index, memory, or training corpus) that would let it compete with the channel owner's own agent later.
- **Meta's own vocabulary shows the same instinct.** Meta's CFO argued (Q3 2026) that agent access to businesses will move toward "authenticated APIs, approved agent partnerships or direct communications between the consumer's personal agent and a business' own agent," and Meta is standardizing this through its Personal Agent Protocol (see Section 2). A channel owner that blocks rivals' agents is also building the protocol its own agent will use to reach others.
- **Regulators bite only in consumer messaging, and only with dominance plus exclusion.** That was the profile of the EU, Italian and Brazilian cases. No comparable action was found against Slack's or Teams' agent terms or against WeChat. Interim measures also took about eight months (Oct 2025 to Jun 2026) and cover only the EEA, Italy and Brazil, so a model vendor's channel access is jurisdiction-dependent and temporary.
- **The fee route was ruled equivalent to a ban.** The Commission treated Meta's €0.049–0.132 per-message fee as equivalent to exclusion. That sets a ceiling on how much a dominant channel can charge a rival's agent in the EU. Telegram (no dominance finding, no own frontier model) and X (pay-per-use) price access with no such check.
- **WeChat polices viral mechanics, not AI as such.** Tencent blocked its own Yuanbao alongside Qwen and Wenxin, so the binding constraint in China is the channel's anti-spam and anti-inducement rules rather than an AI-specific rule. The official AI door it has opened (ClawBot) is 1:1, user-hosted and beta-gated.

### Gaps
- The exact effective date of Slack's rate limits for existing installs: the current page and the FAQ-version summary conflict (none versus 3 Mar 2026).
- Slack's verbatim API Terms text on LLM use: only Hunton's quotation was seen, not the primary terms page.
- Whether Microsoft Copilot and Perplexity returned to WhatsApp in the EEA after 9 Jun 2026: only ChatGPT's return is reported, by a single outlet.
- The outcome of Meta's General Court appeal.
- AGCM's final decision: the investigation deadline was reportedly 31 Dec 2026, but this is unverified.
- CADE's status after April 2026.
- Whether the Telegram–xAI deal was finalized, paid or shipped: no source after May 2025 was found.
- Whether @grok's own replies are subject to the Feb 2026 "summoned-only" API rule: no source; @grok is first-party.
- No official Weixin or WeChat rulebook for third-party AI agents or bots was found; the ClawBot limits come from third-party tutorials.
- No primary Microsoft page was found for Teams-Store-specific rules for AI agents; the Agent 365 GA date and price come from third-party sources.

## 2. Comparable agents and their business models (prices, metrics)

### Takeaway
Agents run by channel owners (archetype B) reach the most users but earn indirectly: Meta through ads that use AI-chat signals, WhatsApp messaging fees, the Meta One and Muse subscriptions, and future commerce fees. Owning the channel did not save Tencent's Yuanbao from falling to fourth place among Chinese AI apps.

Ecosystem giants (C) earn through seat add-ons, bundles and usage metering, and they increasingly sell governance of agents (Microsoft's $30 add-on, $21 SMB SKU, $99 E7 bundle, Copilot Credits and Agent 365). Over 2026 Microsoft stopped reporting seat counts and started reporting usage.

Model vendors in others' channels (A) won large free audiences (50 million ChatGPT users on WhatsApp) that were cut off by one change in the terms. Their response has been to build multiplayer features into their own apps (ChatGPT group chats, January–November 2025) or to ride sanctioned partner APIs (Slack RTS/MCP).

The open-source "agent in your chat app" (OpenClaw) puts the user in control, but it depends on unofficial or narrowly sanctioned access to each channel.

### Cited Findings

#### Meta AI / Muse / Business AI (archetype B: WhatsApp, Instagram, Messenger)

**Users and engagement**
- **[Report]** Meta AI usage: over 500 million MAU on the Q3 2024 call — [TelecomTalk](https://telecomtalk.info/meta-ai-500million-monthly-users-mark-zuckerberg/984187). "Almost a billion" monthly actives on the Q1 2025 call, when CFO Susan Li said "WhatsApp continues to see the strongest Meta AI usage across our family of apps" — [Nairametrics, 1 May 2025](https://nairametrics.com/2025/05/01/whatsapp-crosses-3-billion-monthly-users-as-meta-doubles-down-on-ai-strategy). About 1 billion later in 2025; sources disagree on whether that was the May 2025 shareholder meeting or the Q3 2025 call — [SQ Magazine (3P)](https://sqmagazine.co.uk/meta-ai-statistics/). No newer official Meta AI MAU was found through the Q3 2026 call.
- **[Official] Q1 2026 call (29 Apr 2026):**
  - Meta Superintelligence Labs' first model, Muse Spark, now powers "Meta AI in direct chat threads across our Family of Apps, as well as the standalone Meta AI app and website," with "double-digit percent increases in Meta AI sessions per user."
  - Business AIs on WhatsApp expanded to small businesses in Latin America and Indonesia (and on Messenger in Asia-Pacific). There are now "more than 10 million conversations each week ... through Business AIs, up from 1 million at the start of the year," and "business AIs today are currently free for most businesses."
  - Ads in WhatsApp Status are seen by "hundreds of millions of people ... daily."
  - Family of Apps "other revenue" was $885M, up 74%, "driven primarily by WhatsApp paid messaging and subscriptions revenue."
  - Source: [Meta Q1 2026 transcript](https://s21.q4cdn.com/399680738/files/doc_financials/2026/q1/META-Q1-2026-Earnings-Call-Transcript.pdf)
- **[Official] Q3 2026 call (21 Oct 2026):**
  - Usage: "engagement per person in Meta AI has more than doubled since April," and WhatsApp "is becoming an increasingly important platform for agents."
  - Meta One, a unified subscription for consumers, creators and businesses, expanded globally in September 2026. It has "more than 50 features across Instagram, Facebook, and WhatsApp, including Meta Business Agent and Meta AI," and "has surpassed 15 million subscriptions and trials." "The core experience across our apps and Meta AI will remain free."
  - Family of Apps other revenue was $1.1B, up 59%, "driven primarily by WhatsApp paid messaging and subscriptions revenue." Family of Apps ad revenue was $63.1B, up 26%.
  - Source: [Meta Q3 2026 transcript](https://s21.q4cdn.com/399680738/files/content_files/META-Q3-2026-Earnings-Call-Transcript.pdf)

**Muse personal agent (launched September 2026)**
- **[Official]** Muse is a standalone personal agent app, not embedded in a chat channel.
  - "More than 10 million people in the US and Canada" set it up in about six weeks, and "about 25% of people who have set up Muse use it on any given day."
  - Shopping is the top use case, used by over 25% of Muse weekly actives, with an average purchase above $200.
  - Zuckerberg described the business model:
    - The product is free for most people, and Meta gets "paid a fee based on the transactions that we help facilitate... paid for by businesses."
    - Users who want the most advanced models or extra tokens subscribe.
    - Businesses and employers subscribe for work features.
    - Users can link Muse to "the personalization and quality of their content and ads."
  - Muse is "very compute-intensive"; Meta expects "to improve its cost efficiency by more than 10x in the next year."
  - Source: [Meta Q3 2026 transcript](https://s21.q4cdn.com/399680738/files/content_files/META-Q3-2026-Earnings-Call-Transcript.pdf)
- **[Report]** Muse subscriptions cost $20 and $100 per month for power users. Muse is free, but running it on the newest model, Muse Wonder (released 12 Oct 2026), requires a subscription. Bank of America does not expect a meaningful revenue contribution in 2026–27 — [CNBC, 21 Oct 2026](https://www.cnbc.com/2026/10/21/meta-earnings-q3-2026-live-updates.html)

**Agent-to-agent protocol and commerce**
- **[Official]** Meta "previewed the Personal Agent Protocol, an open standard across Muse and Meta business agents that we are developing with industry partners like Sierra, Walmart, Stripe, Shopify." It defines how a consumer's agent interacts with a business's agent. Meta expects agentic commerce to move "away from agents attempting to navigate every digital experience like a human" toward "authenticated APIs, approved agent partnerships" — [Meta Q3 2026 transcript](https://s21.q4cdn.com/399680738/files/content_files/META-Q3-2026-Earnings-Call-Transcript.pdf)

**Ads informed by AI chats**
- **[Report]** Meta announced on 1 Oct 2025 that, from 16 Dec 2025, text and voice interactions with Meta AI would personalize content and ads across its apps.
  - There is no opt-out.
  - The EU, UK and South Korea are excluded.
  - Sensitive topics (religion, sexual orientation, politics, health, ethnicity, etc.) are not used for ads.
  - Sources: [The Register](https://www.theregister.com/2025/10/01/meta_ai_use_informs_ads/); [The Star](https://www.thestar.com.my/tech/tech-news/2025/10/01/meta-to-use-ai-chats-to-personalize-content-and-ads-from-december)
- **[Official]** Advertisers who followed the Meta AI business assistant's recommendations "drove almost 5% more conversions at the same cost per conversion and increased their advertising investment by 9%" compared with similar non-adopters — [Meta Q3 2026 transcript](https://s21.q4cdn.com/399680738/files/content_files/META-Q3-2026-Earnings-Call-Transcript.pdf)

**Cost note**
- **[Official]** Q3 2026 expense growth "was primarily driven by legal-related costs, infrastructure costs, and third-party AI token costs" — [Meta Q3 2026 transcript](https://s21.q4cdn.com/399680738/files/content_files/META-Q3-2026-Earnings-Call-Transcript.pdf)

#### Tencent Yuanbao in WeChat (archetype B)
- **[Report]** Around 16–17 Apr 2025, WeChat users could add 元宝 as a friend. It appears as a contact, not an Official Account; at launch it had no group-management agent functions — [观察者网](https://www.guancha.cn/economy/2025_04_17_772617.shtml); [财联社](https://www.cls.cn/detail/2006522)
- **[Report]** In September 2025 Yuanbao could be @-mentioned in Official Account and Channels (视频号) comment sections. On 13 May 2026 it gained WeChat group-chat summarization, but only when the user multi-selects messages and forwards them to Yuanbao. Yuanbao says this is deliberate, so that each interaction is authorized by the user — [澎湃, 14 May 2026](https://m.thepaper.cn/newsDetail_forward_33176080)
- **[Report]** On 26 Jan 2026 Tencent began testing 元宝派 inside the Yuanbao app: social groups where Yuanbao is a member that can join conversations on its own initiative, joinable through WeChat or QQ invite links. Paired with a ¥1 billion red-envelope campaign — [21世纪经济报道](https://www.21jingji.com/article/20260126/herald/bfd1c315746f5882d09b4b59a719ac73.html); [新浪财经](https://finance.sina.com.cn/stock/t/2026-01-27/doc-inhitacz7605265.shtml)
- **[Opinion]** One user review says the AI "has not yet opened the silent door of group chats" (AI还没敲开群聊那扇沉默的门) — [鱼皮AI导航](https://ai.codefather.cn/news/2017989208194473985)
- **[Report, QuestMobile data]** Q1 2026 (to March): Yuanbao MAU was 57.35 million, fourth behind Doubao (345M), Qwen (166M) and DeepSeek (127M). Its Q1 user gain was 8.2 million, against Qwen's 126 million — [澎湃](https://m.thepaper.cn/newsDetail_forward_33176080); [每日经济新闻](https://www.nbd.com.cn/articles/2026-04-21/4350446.html)
- **[Report]** On the Q1 2026 call (13 May 2026), Tencent President Martin Lau (刘炽平) said many users kept using Yuanbao, even while its experience was immature, because it is deeply tied into Tencent's ecosystem. Q1 capex payments were ¥37 billion, mainly for AI — [澎湃](https://m.thepaper.cn/newsDetail_forward_33176080)

#### ByteDance Doubao: Douyin traffic and the Feishu merger (archetype B moving into C)
- **[Report, QuestMobile data]** Doubao MAU:

  | Period | Doubao MAU | Notes |
  |---|---|---|
  | Q1 2026 | 345 million | |
  | June 2026 | 382 million | Qwen about 167 million |
  | September 2026 | 419 million, up 143.6% year on year | Qwen 158 million; DeepSeek 123 million |

  Sources: [中关村在线](https://ai.zol.com.cn/1169/11692234.html); [腾讯新闻](https://news.qq.com/rain/a/20260805A0E4JF00); [澎湃, 21 Oct 2026](https://www.thepaper.cn/newsDetail_forward_34298060)
- **[Report]** In May 2026 Doubao added three paid tiers on the App Store at ¥68, ¥200 and ¥500 per month (annual plans up to ¥5,088). The free tier remains — [澎湃](https://www.thepaper.cn/newsDetail_forward_34298060)
- **[Report]** On 30 Jul 2026 an internal ByteDance email merged Feishu's product team into Doubao under Doubao head 赵祺, with Feishu CEO 谢欣 reporting to him. Feishu's go-to-market team moved into Volcano Engine (火山引擎) as a new "创造力服务平台" under 谭待. ByteDance says Feishu's existing products and services stay unchanged — [虎嗅](https://www.huxiu.com/article/4879671.html); [腾讯新闻/中国企业家](https://news.qq.com/rain/a/20260731A0AV4H00)
  - **SUPERSEDED:** Feishu as an independent business unit.
  - One report says ByteDance then launched a unified office brand, "豆包工作" — [澎湃](https://www.thepaper.cn/newsDetail_forward_34298060)
- **[Report]** A Doubao enterprise edition launched in late June 2026, built with heavy involvement from Feishu's product team. It connects to Feishu docs, sheets, meetings and group chats, and is in internal testing with some Feishu customers — [腾讯新闻/中国企业家](https://news.qq.com/rain/a/20260731A0AV4H00)
- No source was found describing Doubao inside Douyin direct messages or group chats (see Gaps).

#### Microsoft 365 Copilot and Copilot Chat in Teams (archetype C)

**Paid seats and usage (superseded by each later disclosure)**

| Date | Disclosure | Source |
|---|---|---|
| 28 Jan 2026 (FY26 Q2) | 15 million paid seats, up 160% year on year, out of about 450 million M365 commercial seats (~3.3%) | [Directions on Microsoft](https://www.directionsonmicrosoft.com/blog/microsoft-claims-15-million-paid-m365-copilot-seats) [Report] |
| Same quarter | Daily active users of M365 Copilot up 10x year on year | [Neowin](https://www.neowin.net/news/microsoft-q2-2026-key-figures-include-copilot-windows-11-and-record-xbox-growth/) [Report] |
| 29 Apr 2026 (FY26 Q3) | 20 million paid seats | [No Jitter](https://nojitter.com/ai-automation/microsoft-365-copilot-hits-20-million-paid-seats) [Report] |
| End of June 2026 (FY26) | "Over 30 million" paid seats | [Yahoo Finance](https://finance.yahoo.com/technology/ai/articles/microsofts-copilot-just-crossed-30-183023723.html); [UC Today](https://uctoday.com/microsoft-q1-copilot-usage-jumps-seat-count-goes-missing) [Report] |
| 28 Oct 2026 (FY27 Q1) | Seat count not disclosed. Nadella said the number of M365 Copilot users rose 50% from the prior quarter; CFO Amy Hood said the E7 bundle "now has millions of seats" | [CNBC](https://www.cnbc.com/2026/10/28/microsoft-msft-q1-earnings-report-2027.html) [Report] |

- **[3P]** Other FY27 Q1 metrics: customers paying through usage-based credits grew more than 75% in the quarter, and customers with more than 50,000 Copilot seats rose more than 8x in a year — [FourWeekMBA](https://fourweekmba.com/ai-microsoft-takes-copilot-past-per-seat-usage-buyers-up-75/); [UC Today](https://uctoday.com/microsoft-q1-copilot-usage-jumps-seat-count-goes-missing)
- **[Opinion]** Coverage of the 15 million figure called it a "small number" of paying customers relative to the base — [Motley Fool](https://www.fool.com/investing/2026/02/15/microsofts-small-number-paying-copilot-customers)

**Prices and packaging**
- **[Report]** The M365 Copilot licence is $30 per user per month. Microsoft 365 E7 costs $99 per user per month and bundles E5, Copilot, Agent 365 and Entra security — [GeekWire, Mar 2026](https://www.geekwire.com/2026/microsofts-new-copilot-cowork-integrates-anthropics-claude-in-rollout-of-new-e7-licensing-tier/)
- **[3P]** M365 Copilot Business costs $21 per user per month for organizations of 300 users or fewer (since December 2025). An $18 promotion ran during 2026, with conflicting dates — [o365hq](https://o365hq.com/blog/microsoft-365-copilot-licensing-and-pricing-2026); [EPC Group](https://www.epcgroup.net/microsoft-365-copilot-licensing-cost-enterprise-2026)
- **[3P]** Copilot Chat is free with Microsoft 365. Agents used by unlicensed staff consume Copilot Credits: pay-as-you-go at $0.01 per credit, or a $200 monthly pack of 25,000 credits — [SAMexpert](https://samexpert.com/copilot-studio-licensing-guide/); [EPC Group](https://www.epcgroup.net/microsoft-365-copilot-licensing-cost-enterprise-2026)
- **[3P, single source]** Copilot Business is said to default to usage-based billing from 1 Dec 2026 — [39 Advisory](https://www.39advisory.com/en/post/copilot-usage-based-billing-sme-ai-budget)

**Microsoft as a buyer of a model vendor's technology**
- **[Official]** On 9 Mar 2026 Microsoft announced Copilot Cowork, which "integrated the technology behind Claude Cowork into Microsoft 365 Copilot." It reached the Frontier program on 30 Mar 2026 — [Microsoft 365 blog, 9 Mar 2026](https://www.microsoft.com/en-us/microsoft-365/blog/2026/03/09/copilot-cowork-a-new-way-of-getting-work-done/); [Microsoft 365 blog, 30 Mar 2026](https://www.microsoft.com/en-us/microsoft-365/blog/2026/03/30/copilot-cowork-now-available-in-frontier/)
- **[Opinion]** Forbes framed it as "Microsoft dreamed of a digital coworker. Then it licensed Anthropic's" — [Forbes](https://www.forbes.com/sites/janakirammsv/2026/03/10/microsoft-dreamed-of-a-digital-coworker-then-it-licensed-anthropics/)
- **[3P]** A July 2026 roundup says Copilot Cowork became generally available and Claude is selectable in Copilot Chat — [WindowsForum](https://windowsforum.com/windows-news.4/microsoft-365-copilot-july-2026-cowork-ga-claude-and-auto-install.436600/)

#### Google Gemini in Workspace and Google Chat (archetype C, bundling)
- **[Report]** On 15 Jan 2025 Google folded the Gemini add-on into Workspace Business and Enterprise plans and raised prices by about $2 per user per month. Business Standard went from $12 to $14 on annual plans (flexible plans from $14.40 to $16.80). Gemini became available across Gmail, Docs, Meet, Chat and other apps. The previous add-on cost $20 per user per month, so Business Standard with Gemini had been $32. Existing annual customers saw the increase from 17 Mar 2025 — [9to5Google](https://9to5google.com/2025/01/15/google-workspace-price-increase-2025/); [Android Authority](https://www.androidauthority.com/google-workspace-gemini-all-plans-3516901)

#### Slack's own agent (archetype C inside Salesforce)
- **[Official]** Slackbot became an AI agent on 13 Jan 2026 for Business+ and Enterprise+ — [TechCrunch](https://techcrunch.com/2026/01/13/slackbot-is-an-ai-agent-now). More than 30 additional features followed around 31 Mar 2026, including MCP integration — [The Next Web (3P)](https://thenextweb.com/news/slack-slackbot-30-ai-features-agentic)
- **[Official]** On 14 Oct 2025 Salesforce and OpenAI expanded their partnership. The ChatGPT app for Slack lets teams draft content and summarize conversations inside Slack, and Salesforce's Agentforce apps will come into ChatGPT — [Business Wire, 14 Oct 2025](https://www.businesswire.com/news/home/20251014617162/en/Salesforce-and-OpenAI-Partner-Across-Enterprise-Work-and-Commerce); [Salesforce](https://www.salesforce.com/news/stories/chatgpt-app-for-slack/)

#### Feishu/Lark aily, DingTalk Wukong and WeCom (archetype C, China)
- **[Report]** Feishu's 2025 revenue (or ARR) exceeded ¥3 billion, and Q2 2026 revenue grew more than 100% year on year. DingTalk's 2025 revenue reportedly exceeded ¥4 billion, with growth "limited" for two quarters in 2026. These figures are 《财经》 reporting, which Feishu did not confirm — [新浪/《财经》, 16 Jul 2026](https://finance.sina.com.cn/tech/roll/2026-07-16/doc-inihyiym7998599.shtml)
- **[Report]** Feishu CEO 谢欣 reportedly pushed the profitability target out to 2030 — [腾讯新闻/中国企业家](https://news.qq.com/rain/a/20260731A0AV4H00)
- **[Report]** Feishu's enterprise agent platform aily received a major upgrade in March 2026, alongside new enterprise agents covering personal "intelligent partners" and business systems — [量子位, Mar 2026](https://www.qbitai.com/2026/03/389311.html); [腾讯新闻](https://news.qq.com/rain/a/20260731A0AV4H00)
- **[Official page, via search summary; unverified]** An aily help-center billing page titled "豆包工作伙伴计费说明" lists ¥1,988 per seat per year or ¥198 per seat per month, plus AI point quotas. The title suggests aily's 智能伙伴 has been renamed under the Doubao brand — [aily 帮助中心](https://aily.feishu.cn/hc/1u7kleqg/1cnvbb55)
- **[Opinion/Report]** Media describe Feishu as leading a shift to "AaaS" (Agent as a Service), with buyers moving from paying per account to paying for AI output — [每日经济新闻, 15 Apr 2026](https://www.nbd.com.cn/articles/2026-04-15/4340378.html)
- **[3P/Report] DingTalk Wukong (悟空):**
  - Launched 17 Mar 2026 as an "enterprise AI-native work platform." It is DingTalk's first standalone app in its 11 years — [人人都是产品经理](https://www.woshipm.com/ai/6359136.html)
  - Third-party listings show member tiers at ¥39 and ¥99 per month. The official purchase guide covers unified procurement, seat allocation and quota management without public prices — [千问云 (3P)](https://www.qianwenai.com/agents/wukong); [钉钉悟空 purchase guide](https://wukong.dingtalk.com/docs/enterprise-membership/purchase-guide/)
  - DingTalk's CTO reportedly plans to explore pricing by token consumption and by results — [钉钉服务商 (3P)](https://www.ding.cn/help/771953.html)
  - Wukong was later grouped with QoderWork and MuleRun under a new DingTalk CEO, 陈宇森 — [腾讯新闻](https://news.qq.com/rain/a/20260731A0AV4H00) (Report)
- **[Report]** Platform context only (other researchers profile WorkBuddy):
  - Tencent's WorkBuddy had 20.97 million visits in June 2026, ranking first in one industry report — [腾讯新闻](https://news.qq.com/rain/a/20260731A0AV4H00)
  - Alibaba's 千问办公 began internal testing on 27 Jul 2026, reportedly priced ¥5–15 below WorkBuddy — [腾讯新闻](https://news.qq.com/rain/a/20260731A0AV4H00)
- **[Report/3P]** WeCom 5.0 (August 2025) added "smart summary" across group chats, docs and meetings — [扬子晚报网](https://www.yzwb.net/news/ch/202508/t20250820_252580.html). Its "smart bot" is pitched as an "AI colleague" with configurable model and knowledge base, but reportedly cannot join external customer groups — [CSDN](https://damodev.csdn.net/6ae1cb7ee5f4ca4eff8cecf5.html)

#### ChatGPT: WhatsApp, group chats and the Slack app (archetype A, plus its own channel)
- **[Report]** 50 million people had used ChatGPT on WhatsApp before Meta's ban. OpenAI urged users to link their accounts because WhatsApp offers no chat export — [Engadget](https://www.engadget.com/ai/chatgpt-in-whatsapp-will-stop-working-in-january-161335173.html); [Arlo (3P)](https://www.arlo.sh/blog/chatgpt-on-whatsapp)
- **[Report]** Restored in the EEA on 13 Jul 2026 — [TechTimes](https://www.techtimes.com/articles/320553/20260715/eu-antitrust-order-reopens-whatsapp-chatgpt-setting-precedent-ai-markets.htm)
- **[Report] ChatGPT group chats:**
  - Rolled out globally around 20 Nov 2025 to logged-in Free, Go, Plus and Pro users, after a pilot in Japan, New Zealand, South Korea and Taiwan.
  - Up to 20 participants, invited by link.
  - ChatGPT decides when to speak, or responds when "ChatGPT" is mentioned.
  - Group chats do not use or create personal memory, and sensitive content is reduced for everyone if a minor is present.
  - Source: [TechCrunch, 20 Nov 2025](https://techcrunch.com/2025/11/20/chatgpt-launches-group-chats-globally/)
- **[Official]** ChatGPT app for Slack: see the 14 Oct 2025 Salesforce–OpenAI release above — [Business Wire](https://www.businesswire.com/news/home/20251014617162/en/Salesforce-and-OpenAI-Partner-Across-Enterprise-Work-and-Commerce)

#### Perplexity: @AskPerplexity on X, WhatsApp and Telegram (archetype A)
- **[Report]** Perplexity launched on WhatsApp in late April 2025 after CEO Aravind Srinivas polled users on X. He said demand was "far beyond anything we anticipated," and the bot briefly went down at launch — [American Bazaar](https://americanbazaaronline.com/2025/04/29/perplexity-ai-chatbot-gets-integrated-into-whatsapp-462297/)
- **[3P]** The WhatsApp bot was free with no sign-up; group chats were not supported — [Yahoo](https://www.yahoo.com/news/perplexity-ai-joins-whatsapp-chatbot-144012062.html)
- **[3P]** The Telegram bot @askplexbot can be added to group chats — [Gadgets To Use](https://gadgetstouse.com/blog/2025/04/12/use-perplexity-in-telegram/)
- **[3P]** On X, @AskPerplexity answers when mentioned in replies, like @grok. That makes it a "summoned" bot under X's February 2026 rule — [Yahoo](https://www.yahoo.com/news/perplexity-ai-joins-whatsapp-chatbot-144012062.html); [PiunikaWeb](https://piunikaweb.com/2026/02/24/x-api-blocks-automated-spam-replies/)
- **[Official]** Perplexity is a GA partner of Slack's RTS API and MCP server — [Slack blog](https://slack.com/blog/news/mcp-real-time-search-api-now-available)
- **[Report/3P]** Perplexity left WhatsApp on 15 Jan 2026 — [Gulf News](https://gulfnews.com/technology/no-more-chatgpt-and-perplexity-on-whatsapp-meta-bans-major-ai-chatbots-from-2026-1.500316016); [TechTimes](https://www.techtimes.com/articles/320553/20260715/eu-antitrust-order-reopens-whatsapp-chatgpt-setting-precedent-ai-markets.htm)
- No usage numbers were found for any Perplexity channel bot.

#### OpenClaw: a bring-your-own agent in your chat app (early 2026)
- **[Report]** OpenClaw is a free, open-source autonomous agent created by Peter Steinberger. It was first called Clawdbot, then Moltbot after Anthropic objected to the name, then OpenClaw. It went viral in January–February 2026 — [TechCrunch, 15 Feb 2026](https://techcrunch.com/2026/02/15/openclaw-creator-peter-steinberger-joins-openai/)
- **[3P]** It runs on the user's own machine and connects to WhatsApp, Telegram, Signal, Discord, Slack and others — [AY Automate](https://www.ayautomate.com/blog/openclaw-open-source-ai-agent). WeChat is supported through the official ClawBot plug-in — [OpenClaw docs](https://docs.openclaw.ai/zh-CN/channels/wechat)
- **[Report]** Steinberger joined OpenAI on 15 Feb 2026. Sam Altman said OpenClaw will "live in a foundation as an open source project that OpenAI will continue to support." Meta reportedly also made an offer — [TechCrunch](https://techcrunch.com/2026/02/15/openclaw-creator-peter-steinberger-joins-openai/); [SiliconANGLE](https://siliconangle.com/2026/02/15/openai-hires-openclaw-founder-peter-steinberger-push-toward-autonomous-agents/)
- **[3P, single source]** Over 100,000 GitHub stars — [AlternativeTo](https://alternativeto.net/news/2026/2/openai-hires-openclaw-creator-peter-steinberger-will-keep-the-ai-agent-open-source); more than 200,000 internet-exposed instances, a security concern — [ThreatRoad](https://threatroad.substack.com/p/openai-acquires-the-developer-behind)
- **[Report]** Chinese cloud providers offer hosted OpenClaw deployments — [Tencent Cloud Techpedia](https://www.tencentcloud.com/techpedia/141449)

### Inferences
- **Business-model taxonomy that emerges:**
  - Channel owners (B) monetize through ads, messaging fees, subscriptions and commerce take-rates. Meta has all four live or planned; the AI agent mainly raises engagement and data value rather than charging directly.
  - Suite giants (C) monetize through seat add-ons that then turn into bundles ($99 E7) and metering (Copilot Credits), and they add a new governance fee (Agent 365 at $15).
  - Model vendors in others' channels (A) mostly do not monetize in the channel: ChatGPT and Perplexity on WhatsApp were free. The channel is acquisition, and value is captured only if users convert to the vendor's own account or subscription, which explains OpenAI's account-linking push before the cutoff.
- **Chinese suites are converging on the Western C pattern, faster and with model-house consolidation.** ByteDance folded Feishu into Doubao, and Alibaba consolidated Wukong, QoderWork and MuleRun under DingTalk. Pricing is experimenting with tiers plus quotas (aily per seat with AI points; Wukong ¥39/¥99), with token- and outcome-based pricing being discussed.
- **Meta's 2026 pivot to a standalone agent app (Muse) plus an agent-to-agent protocol** suggests that even the largest channel owner does not see "AI inside chat threads" as the full product. The chat channel becomes the network layer, and the agent lives in its own app with its own subscription.
- **Microsoft has stopped disclosing seats (October 2026)** while touting usage growth, a credits business and E7 seats. This fits a shift in value capture from per-seat uplift to bundles plus consumption. That reading is an inference consistent with commentary at [UC Today](https://uctoday.com/microsoft-q1-copilot-usage-jumps-seat-count-goes-missing), not a Microsoft statement.

### Gaps
- No usage figures found for:
  - Meta AI in WhatsApp group chats.
  - ChatGPT group chats.
  - Perplexity's X, WhatsApp or Telegram bots.
  - Yuanbao inside WeChat as a contact, separate from app MAU.
  - Yuanbao in Q2–Q3 2026.
- No source on Doubao's integration into Douyin direct messages or group chats.
- Doubao DAU: one summary cites 181 million as of 30 Sep 2026, but it is unverified and was not used.
- Revenue, customers and paid adoption for aily, Feishu 智能伙伴 and DingTalk Wukong are undisclosed; Wukong's prices come only from third parties.
- No primary Microsoft pricing page was fetched; the $21, $18 promotion and Copilot Credit prices are third-party.
- Whether Meta's AI-chat ad signals launched as planned on 16 Dec 2025: no post-launch confirmation found.
- How OpenClaw connects to WhatsApp and iMessage (official API versus unofficial client protocols) was not verified. This matters because it decides whether it falls under the Business Solution Terms.

## 3. Does embedding an agent in a shared channel change adoption and monetization?

### Takeaway
There is strong quantitative evidence that channel distribution creates reach at a low activation cost:
- 50 million ChatGPT users on WhatsApp with no app install.
- Meta AI near 1 billion MAU, with WhatsApp its strongest surface.
- Business AI conversations up tenfold in one quarter.
- Slack partner-agent calls up 25x.

The evidence that multiplayer visibility drives adoption is mostly anecdotal or forward-looking. Platforms deliberately limit an agent's presence in group conversations, even for their own agent. Embedding alone also fails without a competitive product: Yuanbao reached 57 million MAU with WeChat, against Doubao's 345 million.

Monetization gains so far go mainly to channel and suite owners: WhatsApp paid-messaging growth of 59–74%, Meta One's 15 million subscriptions and trials, Copilot seats rising from 15 million to more than 30 million, and AI-informed ads. They do not go to model vendors in others' channels.

### Cited Findings

#### Reach and lower activation cost
- **[Report]** 50 million people used ChatGPT through a WhatsApp phone number (1-800-ChatGPT) before Meta's ban — [Engadget](https://www.engadget.com/ai/chatgpt-in-whatsapp-will-stop-working-in-january-161335173.html)
- **[Report]** Perplexity's WhatsApp bot met demand "far beyond anything we anticipated" (April 2025) — [American Bazaar](https://americanbazaaronline.com/2025/04/29/perplexity-ai-chatbot-gets-integrated-into-whatsapp-462297/). It required no sign-up — [Yahoo (3P)](https://www.yahoo.com/news/perplexity-ai-joins-whatsapp-chatbot-144012062.html)
- **[Report]** Meta AI rose from more than 500 million MAU (Q3 2024) to almost 1 billion (Q1 2025), with "WhatsApp ... the strongest Meta AI usage across our family of apps" — [TelecomTalk](https://telecomtalk.info/meta-ai-500million-monthly-users-mark-zuckerberg/984187); [Nairametrics](https://nairametrics.com/2025/05/01/whatsapp-crosses-3-billion-monthly-users-as-meta-doubles-down-on-ai-strategy)
- **[Official]** Weekly conversations between people and Business AIs on Meta's messaging apps rose from 1 million to more than 10 million between the start of 2026 and late April 2026 — [Meta Q1 2026 transcript](https://s21.q4cdn.com/399680738/files/doc_financials/2026/q1/META-Q1-2026-Earnings-Call-Transcript.pdf)
- **[Official]** Slack RTS queries and MCP tool calls rose 25x from October 2025 to February 2026, across more than 50 partners — [Slack blog](https://slack.com/blog/news/mcp-real-time-search-api-now-available)
- **[Report]** M365 Copilot daily active users rose 10x year on year (FY26 Q2) — [Neowin](https://www.neowin.net/news/microsoft-q2-2026-key-figures-include-copilot-windows-11-and-record-xbox-growth/). Users rose 50% quarter on quarter (FY27 Q1) — [CNBC](https://www.cnbc.com/2026/10/28/microsoft-msft-q1-earnings-report-2027.html)
- **[Official]** Yuanbao's WeChat entry points grew step by step: search, then adding it as a contact (April 2025), @-mentions in comments (September 2025), and group-chat summaries by forwarding (May 2026) — [澎湃](https://m.thepaper.cn/newsDetail_forward_33176080). Tencent's Martin Lau credited ecosystem ties for user retention despite an immature product (same source; Report).

#### Multiplayer and group visibility
- **[Report]** ChatGPT group chats (up to 20 people, link invites, ChatGPT chooses when to speak, no personal memory) are OpenAI's own multiplayer channel, launched globally on 20 Nov 2025. No usage numbers were disclosed — [TechCrunch](https://techcrunch.com/2025/11/20/chatgpt-launches-group-chats-globally/)
- **[3P/Report] Platform limits on agents in groups:**
  - The WhatsApp Business API did not support AI bots in groups in 2025 (Perplexity) — [Yahoo](https://www.yahoo.com/news/perplexity-ai-joins-whatsapp-chatbot-144012062.html)
  - X allows API bot replies only when the bot is mentioned or quoted (23 Feb 2026) — [PiunikaWeb](https://piunikaweb.com/2026/02/24/x-api-blocks-automated-spam-replies/)
  - WeChat ClawBot reportedly works only 1:1 — [知乎](https://zhuanlan.zhihu.com/p/2019698694069471000)
  - Yuanbao's group summaries require users to forward messages — [澎湃](https://m.thepaper.cn/newsDetail_forward_33176080)
  - WeCom bots are reportedly barred from external groups — [CSDN](https://damodev.csdn.net/6ae1cb7ee5f4ca4eff8cecf5.html)
  - Telegram is the exception: Perplexity's bot works in groups — [Gadgets To Use](https://gadgetstouse.com/blog/2025/04/12/use-perplexity-in-telegram/)
- **[Official, forward-looking]** Zuckerberg described the next step as a "Muse network ... so that Muse is not just a kind of single player game ... your Muses can work with your friends and your families." He also said Meta expects to "grow Muse to serve billions of people," because "billions of Muses in a network at scale will be able to get more done" — [Meta Q3 2026 transcript](https://s21.q4cdn.com/399680738/files/content_files/META-Q3-2026-Earnings-Call-Transcript.pdf)
- **[Report/Opinion]** 元宝派 (January 2026), an AI group-chat product, drew mixed reviews, including that the AI "has not yet opened the silent door of group chats" — [鱼皮AI导航](https://ai.codefather.cn/news/2017989208194473985). No official metrics were found.

#### Counter-evidence: owning or embedding in a channel does not guarantee adoption
- **[Report]** Despite native WeChat entry points, Yuanbao's Q1 2026 MAU was 57.35 million (fourth place), against 345 million for ByteDance's Doubao. Its Spring Festival peak of 114 million MAU decayed to about 9 million DAU after the holiday — [澎湃](https://m.thepaper.cn/newsDetail_forward_33176080)
- **[Report]** Doubao reached 419 million MAU by September 2026 — [澎湃](https://www.thepaper.cn/newsDetail_forward_34298060)
- **[Report/Opinion]** Twenty-six months after general availability, Copilot was embedded in Teams, Outlook and Office, yet paid seats were about 3.3% of 450 million commercial seats (January 2026) — [Directions on Microsoft](https://www.directionsonmicrosoft.com/blog/microsoft-claims-15-million-paid-m365-copilot-seats); [Motley Fool](https://www.fool.com/investing/2026/02/15/microsofts-small-number-paying-copilot-customers)

#### Monetization outcomes
- **Seat uplift and bundling**
  - **[Report]** Copilot paid seats went from 15 million (January 2026) to 20 million (April 2026) to more than 30 million (June 2026), then were no longer disclosed — [Directions on Microsoft](https://www.directionsonmicrosoft.com/blog/microsoft-claims-15-million-paid-m365-copilot-seats); [No Jitter](https://nojitter.com/ai-automation/microsoft-365-copilot-hits-20-million-paid-seats); [UC Today](https://uctoday.com/microsoft-q1-copilot-usage-jumps-seat-count-goes-missing)
  - **[Report]** The E7 bundle at $99 has "millions of seats" (October 2026) — [CNBC](https://www.cnbc.com/2026/10/28/microsoft-msft-q1-earnings-report-2027.html); [GeekWire](https://www.geekwire.com/2026/microsofts-new-copilot-cowork-integrates-anthropics-claude-in-rollout-of-new-e7-licensing-tier/)
  - **[Report]** Google's bundling converted a $20 add-on into a roughly $2 increase in the list price — [9to5Google](https://9to5google.com/2025/01/15/google-workspace-price-increase-2025/)
  - **[Official]** Slackbot's AI agent is included in Business+ and Enterprise+ — [TechCrunch](https://techcrunch.com/2026/01/13/slackbot-is-an-ai-agent-now)
- **Messaging and subscriptions**
  - **[Official]** Meta's Family of Apps other revenue grew 74% (Q1 2026, $885M) and 59% (Q3 2026, $1.1B), "driven primarily by WhatsApp paid messaging and subscriptions revenue." Meta One has more than 15 million subscriptions and trials — [Meta Q1 2026](https://s21.q4cdn.com/399680738/files/doc_financials/2026/q1/META-Q1-2026-Earnings-Call-Transcript.pdf); [Meta Q3 2026](https://s21.q4cdn.com/399680738/files/content_files/META-Q3-2026-Earnings-Call-Transcript.pdf)
- **Ads**
  - **[Report]** AI-chat signals are used for ad targeting from 16 Dec 2025, excluding the EU, UK and South Korea — [The Register](https://www.theregister.com/2025/10/01/meta_ai_use_informs_ads/)
  - **[Official]** Advertisers who adopted the AI business assistant's recommendations got about 5% more conversions and spent 9% more on ads — [Meta Q3 2026](https://s21.q4cdn.com/399680738/files/content_files/META-Q3-2026-Earnings-Call-Transcript.pdf)
- **Fees for access to the channel**
  - **[Report]** WhatsApp charged €0.049–0.132 per message (March 2026; struck down in the EEA in June 2026) — [TechCrunch](https://techcrunch.com/2026/03/05/meta-will-allow-rival-ai-chatbots-on-whatsapp-in-europe-but-for-a-fee/)
  - **[Official]** X charges per call ($0.010 for a summoned post, $0.015 for an ordinary post, $0.20 for a post with a URL) — [X docs](https://docs.x.com/x-api/getting-started/pricing)
  - **[Official, Durov]** Telegram's announced xAI terms were $300 million plus a 50% revenue share — [Durov on X](https://x.com/durov/status/1927697402095378432)
- **Commerce**
  - **[Official]** More than 25% of Muse weekly actives shop through it, with an average purchase above $200. Meta plans fees "paid for by businesses" — [Meta Q3 2026](https://s21.q4cdn.com/399680738/files/content_files/META-Q3-2026-Earnings-Call-Transcript.pdf)

#### Inference cost (the other side of value capture)
- **[Official]** Muse is "very compute-intensive," and Meta targets more than a 10x efficiency gain in a year. Third-party AI token costs were a named driver of Q3 2026 expense growth — [Meta Q3 2026](https://s21.q4cdn.com/399680738/files/content_files/META-Q3-2026-Earnings-Call-Transcript.pdf)
- **[Report]** Meta cited AI chatbots straining systems not designed for them as the reason for the WhatsApp ban and fee — [TechCrunch](https://techcrunch.com/2026/03/05/meta-will-allow-rival-ai-chatbots-on-whatsapp-in-europe-but-for-a-fee/)
- **[3P]** Microsoft passes agent inference costs to customers through Copilot Credits ($0.01 each) on top of free Copilot Chat — [SAMexpert](https://samexpert.com/copilot-studio-licensing-guide/)

### Inferences
- **Lower activation cost is real and large.** A phone number or an @-mention removes app install, sign-up and context switching. The 50 million WhatsApp ChatGPT users and Perplexity's launch overload show this. But the channel owner holds that audience: when access was cut, ChatGPT's WhatsApp users could not export their chats and had to be steered into OpenAI accounts.
- **Context availability is the most valuable part of embedding, and the most restricted.** Slack allows only query-time, permission-scoped context. Yuanbao gets group context only when users forward it. WeChat and WhatsApp keep third-party agents out of groups. X allows bot replies only when invited. Platforms ration ambient group context because it is both a privacy risk and their own data moat.
- **"Multiplayer virality" is so far a hypothesis, not demonstrated.** No disclosed metric shows group use driving agent adoption (ChatGPT group chats, 元宝派, Meta's planned Muse network). Where viral group sharing was attempted at scale (the 2026 Spring Festival red envelopes), the channel's anti-inducement rules shut it down within days.
- **Embedding helps a strong product much more than a weak one.** Yuanbao's channel advantage did not offset product gaps against Doubao. Copilot's suite embedding produced single-digit seat penetration until price cuts and bundling (the $21 SMB tier and $99 E7) accelerated it. Distribution multiplies product quality; it does not replace it.
- **Monetization from embedding goes mostly to the channel or suite owner.** Meta collects messaging fees and subscriptions, Microsoft collects seats, bundles and credits, and Telegram and X collect access fees. Model vendors earn only by converting users to their own subscriptions (not evidenced in-channel) or by selling models and technology to the giants (Anthropic to Microsoft for Copilot Cowork).

### Gaps
- No controlled or quantitative study isolating the effect of in-channel or group placement on AI adoption was found.
- No platform disclosed group-chat AI usage separately.
- No conversion rate from WhatsApp ChatGPT users to linked or paid OpenAI accounts was found.
- Retention or seat-expansion effects specific to Copilot in Teams (versus other Copilot surfaces) are not disclosed.
- No data on revenue sharing or Marketplace fees for Slack partner agents.

## 4. Synthesis: who captures value in each archetype, and which structural conditions decide it

### Takeaway
In all three archetypes the party that controls identity, permissions and persistent conversation data, and that can bundle or set prices on the channel, captures most of the value.
- **A (model vendor in someone else's channel):** value creation and delivery are real, but capture holds only while the channel owner lacks a competing agent, lacks dominance, or is forced open by regulators. Even then, data rights limit the vendor to permissioned, query-time access.
- **B (channel owner also owns the model):** the owner captures through ads, fees, subscriptions and commerce. The checks are antitrust, privacy carve-outs, the owner's own anti-spam norms, inference cost, and the need for a competitive model.
- **C (suite giant's work agent):** the giant captures through bundles, metering and governance fees. Model vendors become suppliers. Customers keep surplus only when suites compete or bundles push the marginal AI price toward zero.

### Cited Findings
The decisive structural facts, by condition (restated from Sections 1–3):

**Data access rights**
- Slack bans bulk export, indexes and LLM use of API data; access is query-by-query only — [Hunton](https://www.hunton.com/insights/legal/salesforce-locks-down-slack-data-time-to-review-your-slack-api-terms)
- X bans third-party training on X content — [TechCrunch](https://techcrunch.com/2025/06/05/x-changes-its-terms-to-bar-training-of-ai-models-using-its-content)
- Glean was forced from the Discovery API to per-user RTS — [Glean docs](https://docs.glean.com/connectors/native/slack/setup/slack-rts-connector/)
- Meta uses its own AI-chat data for ads (outside the EU, UK and South Korea) — [The Register](https://www.theregister.com/2025/10/01/meta_ai_use_informs_ads/)

**API terms and distribution rights**
- Slack: Marketplace-only commercial distribution, and 1 request per minute for non-Marketplace apps — [Slack changelog](https://docs.slack.dev/changelog/2025/05/29/rate-limit-changes-for-non-marketplace-apps/)
- WhatsApp: the ban, then the fee, then the Commission order to restore free access — [EC](https://ec.europa.eu/commission/presscorner/api/files/document/print/en/ip_26_1276/IP_26_1276_EN.pdf)
- X: summoned-only replies and pay-per-call pricing — [PiunikaWeb](https://piunikaweb.com/2026/02/24/x-api-blocks-automated-spam-replies/); [X docs](https://docs.x.com/x-api/getting-started/pricing)
- Microsoft: Agent Store validation and certification — [Microsoft Learn](https://learn.microsoft.com/microsoft-365/copilot/copilot-agent-store)

**Bundling power**
- Google added Gemini for about +$2 — [9to5Google](https://9to5google.com/2025/01/15/google-workspace-price-increase-2025/)
- Microsoft E7 at $99 with Copilot and Agent 365 has "millions of seats" — [GeekWire](https://www.geekwire.com/2026/microsofts-new-copilot-cowork-integrates-anthropics-claude-in-rollout-of-new-e7-licensing-tier/); [CNBC](https://www.cnbc.com/2026/10/28/microsoft-msft-q1-earnings-report-2027.html)
- Slackbot is included in paid tiers — [TechCrunch](https://techcrunch.com/2026/01/13/slackbot-is-an-ai-agent-now)
- Meta AI is pre-installed and given "greater prominence" in WhatsApp — [AGCM](https://en.agcm.it/en/media/press-releases/2025/12/A576)

**Identity and permissions**
- Slack's MCP server "respects user permissions" — [Slack blog](https://slack.com/blog/news/mcp-real-time-search-api-now-available)
- Glean requires each user to authorize — [Glean docs](https://docs.glean.com/connectors/native/slack/setup/slack-rts-connector/)
- WhatsApp has no chat export; OpenAI pushed account linking — [Engadget](https://www.engadget.com/ai/chatgpt-in-whatsapp-will-stop-working-in-january-161335173.html)
- Yuanbao requires user-authorized forwarding — [澎湃](https://m.thepaper.cn/newsDetail_forward_33176080)
- X requires automated accounts to be labeled and tied to a human-run account — [X Help](https://help.x.com/en/using-x/automated-account-labels)

**Inference cost**
- Muse is compute-intensive, with a 10x efficiency target, and third-party token costs are a Meta expense driver — [Meta Q3 2026](https://s21.q4cdn.com/399680738/files/content_files/META-Q3-2026-Earnings-Call-Transcript.pdf)
- Microsoft meters agent use with Copilot Credits — [SAMexpert](https://samexpert.com/copilot-studio-licensing-guide/)
- Meta cites "strain" from chatbots — [TechCrunch](https://techcrunch.com/2026/03/05/meta-will-allow-rival-ai-chatbots-on-whatsapp-in-europe-but-for-a-fee/)

**Regulatory override**
- EC interim measures (9 Jun 2026); AGCM (24 Dec 2025); CADE's R$250,000 per day fine (March–April 2026) — [EC](https://ec.europa.eu/commission/presscorner/api/files/document/print/en/ip_26_1276/IP_26_1276_EN.pdf); [AGCM](https://en.agcm.it/en/media/press-releases/2025/12/A576); [Portal Tela](https://www.portaltela.com/noticias/economia/2026/03/30/cade-mantem-multa-diaria-de-r-250-mil-contra-whatsapp-por-ia/)

**The channel monetizes access instead of competing**
- Telegram–xAI: $300 million plus a 50% revenue share, announced but disputed — [Durov](https://x.com/durov/status/1927697402095378432); [The Block](https://www.theblock.co/post/356171/elon-musk-contradicts-telegrams-300m-xai-partnership-announcement-no-deal-has-been-signed)
- X pays API buyers up to 20% back in xAI credits — [X docs](https://docs.x.com/x-api/getting-started/pricing)

**The vendor as supplier to a C giant**
- Microsoft's Copilot Cowork is built on Claude Cowork technology — [Microsoft 365 blog](https://www.microsoft.com/en-us/microsoft-365/blog/2026/03/09/copilot-cowork-a-new-way-of-getting-work-done/)

### Inferences

**Who captures value, archetype by archetype (analytical judgment based on the evidence above):**

| | A: model vendor in someone else's channel | B: channel owner plus own model | C: suite giant's work agent |
|---|---|---|---|
| Examples | ChatGPT and Perplexity on WhatsApp; Claude, Perplexity and ChatGPT apps in Slack; @AskPerplexity on X; OpenClaw over any chat app | Meta AI and Muse on WhatsApp, Instagram and Messenger; Grok on X; Yuanbao in WeChat; Doubao with Douyin | M365 Copilot and Copilot Chat in Teams; Gemini in Workspace and Chat; Slackbot; Feishu aily / Doubao enterprise; DingTalk Wukong; WeCom bots |
| Value creation | High for users: zero-install access, context of the moment | High: native context, default placement, scale (Meta AI near 1B MAU) | High for organizations: grounded in mail, docs, chat and permissions |
| Value delivery depends on | The channel's API terms, rate limits, group and ambient permissions, and pricing | The owner's product quality and model competitiveness (Yuanbao versus Doubao), and its own anti-spam and privacy rules | Identity and permissions graph (Entra, Slack, Feishu org), tenant data gravity, admin-controlled stores |
| Who captures most value | The channel owner: fees (WhatsApp, X, Telegram), data retention (Slack), and the option to cut off. The vendor captures only by converting users to its own accounts or apps, or by supplying a channel owner. | The channel owner: ads informed by AI chats, messaging fees, subscriptions (Meta One, Muse tiers), commerce take-rates; regulators skim some back to rivals in the EEA, Italy and Brazil | The suite giant: seat add-ons, then bundles (E7, Workspace), then metering (credits) and governance fees (Agent 365). The model vendor earns supplier margins. Customers capture surplus when bundles commoditize AI or suites compete (China's Feishu, DingTalk and WeCom) |
| Main risks to the captor | Terms changes such as the 15 Jan 2026 WhatsApp cutoff; X's summoned-only rule; Slack's indexing ban | Antitrust; privacy carve-outs (EU, UK, South Korea for ad signals); inference cost (Muse); weak product despite the channel (Yuanbao) | Seat-penetration ceilings (about 3% in January 2026); disclosure shifting to usage; buyers moving from per-seat to per-outcome pricing (Feishu "AaaS", DingTalk token and outcome pricing) |

**When archetype A's value capture holds:**
1. The channel is legally prevented from excluding rivals: dominance plus refusal of access to infrastructure that was previously open. This holds in the EEA, Italy and Brazil as of 2026 and nowhere else found.
2. The channel owner has no competitive first-party agent, or would rather sell access (Telegram's model). The vendor then pays rent, so its margin depends on the fee being less than the user's lifetime value.
3. The vendor keeps identity, memory and billing outside the channel (account linking; its own group chats). The channel is then top-of-funnel, not the product.
4. Customers demand choice in enterprise channels, which pushes owners toward sanctioned partner APIs (Slack RTS/MCP has more than 50 partners, including direct competitors of Salesforce's own AI). Even then, the "no persistent index" rule caps how much the vendor's product can improve from channel data.
5. Agents are invoked when summoned rather than acting ambiently. This is the default the channels have converged on (X's rule; WhatsApp and WeChat 1:1 only).

**When archetype B's capture holds:**
1. The owner's model and product are competitive. Doubao, with ByteDance's traffic and product, beat WeChat-native Yuanbao, so channel ownership is necessary but not sufficient.
2. Monetization is indirect and lags. Meta captures through ads, WhatsApp paid messaging (other revenue up 59–74%), Meta One subscriptions and planned commerce fees. Business AIs were still "free for most businesses" in April 2026.
3. Regulators do not force open access. In the EU the owner must coexist with free rival access to the Business API, which keeps self-preferencing to default placement (Meta AI in the chat UI) rather than exclusion.
4. Inference cost is managed. Meta itself flags compute intensity and third-party token costs, so a scale advantage turns into profit only if efficiency improves; Meta targets 10x.
5. The owner respects its own trust and anti-spam norms, which limit growth hacks (WeChat blocked Yuanbao's own links).

**When archetype C's capture holds:**
1. The giant controls identity, permissions and the agent store. Microsoft adds governance (Agent 365) as a new paid layer, so third-party agents add value to the giant's platform.
2. Bundling power lets the giant set AI's marginal price near zero for rivals while raising the bundle price: Google +$2, E7 at $99, Slackbot included.
3. The giant can buy model capability from vendors (Copilot Cowork on Claude). This shifts model vendors to supplier economics, with no ownership of the customer.
4. The binding constraint is customer willingness to pay per seat. The 2026 disclosures (seat counts dropped, usage and credits emphasized) and Chinese moves toward token or outcome pricing suggest per-seat capture is weakening and metering is taking its place. That shifts more cost and risk onto customers while keeping the giant as the meter.

**The two-sided channel thesis.** In 2026 the leading channel owners (Meta's Personal Agent Protocol; Slack RTS/MCP; Microsoft Agent Store and Agent 365; WeChat ClawBot) are not simply closing their channels. They are turning agent access into a permissioned, standardized and metered layer that they define. This is consistent with Meta's CFO predicting "more permissioned, structured interactions ... authenticated APIs, approved agent partnerships." The channel captures value by owning the protocol and the meter, not by excluding agents outright.

### Gaps
- No revenue or margin data isolates what model vendors earn from in-channel distribution: ChatGPT on WhatsApp, Perplexity on X and Telegram, Slack partner agents.
- The commercial terms of Microsoft–Anthropic (Copilot Cowork) and Salesforce–OpenAI are not public, so supplier-side value capture in archetype C cannot be quantified.
- Whether the Commission's interim measures led to meaningful rival-assistant usage on WhatsApp in the EEA is unmeasured; only ChatGPT's return is reported.
- No final antitrust decisions (EC, AGCM, CADE) exist as of 2026-11-03. The legal durability of forced access is therefore unknown, and Meta's General Court appeal is pending.
- In China no regulator action on channel access for AI agents was found, and no written WeChat or Weixin agent-access policy was located. The Chinese B and C dynamics here rest on press reporting and observed enforcement (red-envelope link blocking), not on published rules.
