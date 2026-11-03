# Grok Bot, the always-on agent product from xAI/SpaceXAI/SpaceXSI: dated fact profile and business-model mapping (as of 2026-11-03)

_Reading conventions._
- **Labels:** **[OFFICIAL]** = company page, docs, terms, SEC filing, official X account, or executive remarks on the earnings call. **[REPORTED]** = established press. **[MUSK]** = Musk's personal X posts, which state facts or intent but are not policy. **[EXEC FORECAST]** = forward-looking executive statements. **[3P]** = third-party blogs, aggregators, SEO explainers or individual users: low reliability, used only for leads or dates. **[SUPERSEDED]** = no longer current.
- **Access date:** Official docs were read on 2026-11-03 and carry no "last updated" dates unless noted.
- **Scope:** This file is ONLY about **Grok Bot, the agent product**, launched 2026-08-11. The **@grok reply account** on X is covered in `grok_bot.md` and is a different product.
- **Second X handle:** Grok Bot has its own X handle, **@bot** (x.com/bot). Tagging @bot hands a task to your agent. A post that tags both @bot and @grok is ignored by @bot ([docs](https://docs.x.ai/grok-bot/tag-on-x)).

**CORRECTIONS TO THE BRIEF AND TO `grok_bot.md`:**
- **(a) The throttling story is not from October.** The TechCrunch article "Elon Musk throttles Grok Bot limits due to 'unsustainably high' costs" is dated **2026-11-02**, not around 2026-10-13. It covers the **Halloween weekend (2026-10-31 to 11-01)** and a Musk reply posted on **2026-11-01** ([TechCrunch via Yahoo](https://finance.yahoo.com/technology/ai/articles/elon-musk-throttles-grok-bot-162321608.html)). The headline in `grok_bot.md` (around line 200) was seen in the sidebar of the 10-13 article, and sidebars show later stories.
- **(b) The launch-page quotes in the brief describe the page as revised later.** The brief's "beta for SuperGrok, SuperGrok Plus, … Cursor Pro/Pro+/Ultra and Cursor Teams" is the current wording of the x.ai post. On 2026-08-11, launch eligibility was narrower: SuperGrok Heavy, Cursor Ultra and Cursor Teams Premium only.
- **(c) "Its own usage" is out of date for bundle subscribers.** "Comes with its own usage, separate from your Grok and Cursor plans" is **[SUPERSEDED]** for subscribers to The X Subscription. Since 2026-10-12/13, Grok Bot draws from one weekly pool shared with Grok and Cursor.

---

## 1. What exactly is Grok Bot? Announcement, status, platforms, architecture, channels for assigning and supervising work, team and enterprise features

### Takeaway
- **What it is:** Grok Bot launched in beta on 2026-08-11 as "your team of always-on agents." It is a set of named Bots, each with its own memory, routines and screen, all sharing **one persistent cloud computer per account**. That computer has a browser, filesystem and terminal and is hosted in **Cursor's cloud**.
- **How Bots work:** They use apps through connectors and MCP where available, and through computer use where not. They hand control back to a human for logins and approvals.
- **Channels:**
  - The Grok Bot desktop apps (macOS, Windows, Linux) and mobile apps (iOS; Android since about 2026-09-02)
  - X Chat
  - Public **@bot** tags on X
  - **Since 2026-09-28, shared "Team Bots" that teammates @mention in Slack channels.** This surface is the most directly comparable to Claude Tag.
- **Status:** The product is still labeled beta. Enterprise access moved from a waitlist (2026-08-11) to admins enabling it from the dashboard.

### Cited Findings

#### Dated timeline (all dates 2026)
- **06-16 [OFFICIAL]:** SpaceX signed the merger agreement with Anysphere (Cursor).
  - A concurrent "Cursor Compute Agreement" had the parties collaborate "to improve existing models, including Grok, and to jointly develop SI models and related model-specific deliverables or products."
  - That agreement was terminated at closing.
  - Source: [SpaceX 10-Q, filed 2026-10-22](https://www.sec.gov/Archives/edgar/data/1181412/000162828026066880/spcx-20260930.htm)
- **08-11 [OFFICIAL]:** Launch.
  - The x.ai post is dated Aug 11, 2026: "Today we're launching Grok Bot, AI teammates you can give real work to" ([x.ai](https://x.ai/news/introducing-grok-bot)).
  - The Cursor forum announcement was posted the same day by Cursor staffer Kevin Neilson ("kevinn"). It listed eligibility as **SuperGrok Heavy, Cursor Ultra, Cursor Teams Premium**, on desktop and iOS, with an enterprise waitlist ([Cursor forum](https://forum.cursor.com/t/introducing-grok-bot/168053)).
  - VentureBeat, same day [REPORTED]: beta for SuperGrok Heavy ($300/mo), Cursor Ultra ($200/mo) and Cursor Premium Teams ($120/seat/mo); macOS, Windows, Linux and iOS; Android "coming soon" ([VentureBeat, Carl Franzen](https://venturebeat.com/orchestration/spacexais-grok-bot-turns-agents-into-persistent-digital-coworkers-that-can-operate-your-apps-for-120-per-month)).
- **08-14 [OFFICIAL]:** SpaceX "completed its acquisition of Cursor on August 14, 2026, based on an implied equity value of $60 billion" ([Q3 earnings release](https://www.sec.gov/Archives/edgar/data/1181412/000162828026066482/spaceexplorationtechnologi.htm)).
- **08-26 [3P]:** Access was reportedly expanded to all SuperGrok, Cursor Pro and Cursor Teams plans, making $20/mo Cursor Pro the cheapest way in ([KuCoin flash](https://www.kucoin.com/news/flash/grok-bot-access-expanded-to-20-month-cursor-pro-subscribers); [CellCog](https://cellcog.ai/blog/grok-bot-cursor-pro/)).
  - I could not find an official dated notice.
  - The current x.ai post [OFFICIAL, undated revision] lists the expanded set: SuperGrok, SuperGrok Plus, SuperGrok Heavy; Cursor Pro, Pro+, Ultra; Cursor Teams Standard and Premium ([x.ai](https://x.ai/news/introducing-grok-bot)).
- **09-02:** A Cursor forum topic titled "Grok Bot is Now Live on Android" appeared, with 1,890 views ([Cursor forum, related topics list](https://forum.cursor.com/t/introducing-grok-bot/168053)).
- **09-03:** An X post by Matt Palmer (@mattyp) read: "Grok Bot is now available for enterprise… 1. Connect your data sources: Slack, Gmail, Notion…"
  - I dated it to 2026-09-03 21:34 UTC by decoding the post ID.
  - A search summary attributes to a 09-03 post the line "free for all Grok and Cursor enterprise customers for the next two weeks." I could not open the post to confirm the wording or Palmer's affiliation ([X/@mattyp](https://x.com/mattyp/status/2095626442990297310)).
- **09-28 [OFFICIAL]:** The official @bot account announced **Team Bots**: "Introducing Team Bots, shared AI teammates that learn as your team works with them. Give your Team Bot the skills, plugins, and credentials it needs for its role, then work with it in Slack or Grok Bot."
  - Post ID decodes to 2026-09-28 19:56 UTC ([X/@bot](https://x.com/bot/status/2104661562715967548)).
  - Eleven minutes later, a post by user "lauren" (@poteto; affiliation unverified) read: "grok @bot is now multiplayer. make a team bot which has access to your plugins, skills, credentials, and then add it to slack with one click" ([X/@poteto](https://x.com/poteto/status/2104664283808428165)).
- **10-07 [MUSK]:** "Going forward, @SpaceX will use the best back end model for any given task." He named Claude Opus 5.5, Midjourney and Suno ([TNW, 2026-10-07](https://thenextweb.com/news/grok-bot-claude-opus-midjourney-suno-musk)). See §2.
- **10-10 [3P]:** An X trending headline read "Grok Bot Gains Dedicated Email for Independent Actions" ([X trending](https://x.com/i/trending/2108921878865727956); post ID decodes to 2026-10-10 14:05 UTC). I did not verify the details. The docs do confirm the Bot can read one-time codes from its own inbox ([docs](https://docs.x.ai/grok-bot/computer-and-apps)).
- **10-12/13 [OFFICIAL]:** **The X Subscription** launched. Grok Bot is included on Premium ($30), Super ($100) and Ultra ($200) and draws from a shared weekly pool ([docs: pricing](https://docs.x.ai/x-subscription/pricing); [TechCrunch 10-13](https://techcrunch.com/2026/10/13/spacexsi-bundles-x-premium-grok-and-cursor-into-one-subscription/)). See §4.
- **10-20 [OFFICIAL]:** The Q3 release calls it "Grok Bot, our always-on agentic SI system." On the earnings call, Shotwell said Grok Bot had 800,000 paid weekly users in early October ([release](https://www.sec.gov/Archives/edgar/data/1181412/000162828026066482/spaceexplorationtechnologi.htm); [Motley Fool transcript](https://www.fool.com/earnings/call-transcripts/2026/10/20/spacex-spcx-q3-2026-earnings-call-transcript/)). See §3.
- **10-22 [OFFICIAL]:** The 10-Q does not mention "Grok Bot" anywhere. I downloaded the text and searched it: "Grok" appears 14 times and "Bot" 0 times ([10-Q](https://www.sec.gov/Archives/edgar/data/1181412/000162828026066880/spcx-20260930.htm)).
- **10-31 [OFFICIAL]:** The Grok Bot Terms were updated ("Last updated October 31, 2026"). The previous version was dated 2026-10-12 ([Cursor: Grok Bot Terms](https://cursor.com/terms/grok-bot)).
- **10-31 to 11-01:**
  - Users' weekly caps ran out early [REPORTED].
  - On 11-01 Cursor reported a billing/dashboard problem, later marked resolved [REPORTED].
  - On 11-01 Musk posted: "The cost of serving Grok Bot was unsustainably high relative to revenue" [MUSK].
  - Coverage appeared on 11-02 ([TechCrunch via Yahoo](https://finance.yahoo.com/technology/ai/articles/elon-musk-throttles-grok-bot-162321608.html); [TNW](https://thenextweb.com/news/grok-bot-costs-unsustainably-high-musk-usage-limits); [The Decoder](https://the-decoder.com/spacexsi-apparently-cuts-grok-bot-quotas-weeks-after-musk-called-it-his-fastest-growing-product/)). See §4.

#### Announcement claims and status [OFFICIAL]
- **Positioning:**
  - "Grok Bot is your team of always-on agents."
  - Bots "keep working 24/7" and "follow up on threads you dropped."
  - "Bots share a computer of their own in the cloud, so jobs do not stall when you step away."
  - The post is labeled **Beta** ([x.ai](https://x.ai/news/introducing-grok-bot)).
- **Superseded parts of the x.ai post** as read on 2026-11-03:
  - It still says "on desktop and iOS" and "Enterprise users can join a waitlist for future access" ([x.ai](https://x.ai/news/introducing-grok-bot)). Both are **[SUPERSEDED]** by the docs: desktop on macOS, Windows and Linux, plus iOS and Android, and enterprise enabled by admins ([FAQ](https://docs.x.ai/grok-bot/faq); [teams docs](https://cursor.com/docs/grok-bot/teams)).
  - The post's macOS download link points to an api2.cursor.sh URL, and "Contact sales" points to cursor.com/contact-sales?product=grok-bot ([x.ai](https://x.ai/news/introducing-grok-bot)).
- **Docs tagline:** "AI teammates with names, jobs, and context that compounds over time" ([docs overview](https://docs.x.ai/grok-bot/overview)).

#### Architecture: one shared cloud computer per account [OFFICIAL unless marked]
- **The computer:**
  - "Persistent cloud computer with a browser, command line, and filesystem."
  - "Work continues while your laptop is closed."
  - Files, sign-ins and browser sessions carry over between tasks ([docs: computer & apps](https://docs.x.ai/grok-bot/computer-and-apps)).
  - The docs state that the Bots' computers run in Cursor's cloud ([docs overview](https://docs.x.ai/grok-bot/overview)).
- **One computer per account, not per Bot:**
  - "All of your Bots use the same cloud computer, sharing its files, browser sessions, and app logins" ([docs overview](https://docs.x.ai/grok-bot/overview)).
  - The launch post said the computer is isolated to the account, so "treat a login or file placed on it as available to all of your Bots" ([Cursor forum](https://forum.cursor.com/t/introducing-grok-bot/168053)).
  - **Implication:** the brief's "a cloud VM per agent" framing is inaccurate. The accurate description is a VM per user account, with one screen per Bot.
- **Parallelism:**
  - "Each Bot gets its own screen on the shared computer" ([docs](https://docs.x.ai/grok-bot/computer-and-apps)).
  - "One Bot can run one computer-use task on its screen at a time" ([FAQ](https://docs.x.ai/grok-bot/faq)).
- **Local desktop (optional):**
  - The cloud computer is separate from the user's own Mac or PC.
  - Local commands need the capability enabled plus approval under a local-computer policy. The default is "Ask every time," with "Always allow" and "Never allow" as alternatives ([docs](https://docs.x.ai/grok-bot/computer-and-apps)). The security docs "Recommend **Never allow** unless a Bot has a specific reason to work on local files" ([security](https://cursor.com/docs/grok-bot/security)).
  - The shared-bots doc says "A Bot that runs on your own computer can't be shared" ([shared bots](https://docs.x.ai/grok-bot/shared-bots)).
  - A Cursor forum thread titled "Local computer use for Grok Bots" (2026-11-02, 9 replies) suggests users still find local use limited ([Cursor forum list](https://forum.cursor.com/t/introducing-grok-bot/168053)).
- **[3P]** A reviewer describes "a persistent cloud Linux machine" ([eesel.ai](https://www.eesel.ai/blog/grok-bot-review)). The official docs do not name the OS.

#### Connectors, MCP, plugins, credentials [OFFICIAL]
- **Two ways to use apps:** connectors and MCP where available, computer use elsewhere ([launch post](https://forum.cursor.com/t/introducing-grok-bot/168053)). Bots can work on sites "with no clean API or MCP" ([x.ai](https://x.ai/news/introducing-grok-bot)).
- **Named apps:**
  - Connected apps give "a structured way to work with services like Gmail, Notion, and Slack."
  - The docs also name Google Calendar, Sheets, Drive, Docs and Slides; Outlook; Zoom; SharePoint; X; and 1Password.
  - Custom MCP servers can be added by URL or by a command that runs on the Bot's computer. Private-network MCP servers need approval ([docs](https://docs.x.ai/grok-bot/computer-and-apps)).
- **X plugin:** an add-on that needs **X API credits** and an X sign-in for DMs, bookmarks and similar ([docs](https://docs.x.ai/grok-bot/computer-and-apps)). In effect, this adds an internal toll on the X data platform.
- **Credentials:**
  - Sign-in goes in this order: Vault, then a password manager such as 1Password, then a Secure Form. Secrets are filled into the page without the Bot seeing them ([docs](https://docs.x.ai/grok-bot/computer-and-apps)).
  - "A Bot has no identity or credentials of its own… Bots act as the signed-in member… Connector tokens stay on Cursor's backend… Credentials stay out of the model" ([security](https://cursor.com/docs/grok-bot/security)).
- **Human handoff:**
  - For single sign-on or passkeys, CAPTCHAs, approval on another device or QR codes, bank-card verification, and sites that require a human, "The Bot hands you the computer" ([docs](https://docs.x.ai/grok-bot/computer-and-apps)).
  - Phone **calls** appear to be a capability: the terms say "Customer may not use Calls for marketing or mass calls, fraud, harassment, impersonation, emergency services" ([Grok Bot Terms](https://cursor.com/terms/grok-bot)). I found no launch note for a calling feature.

#### Memory, routines/skills, multi-Bot "team" [OFFICIAL]
- **Memory:**
  - "They remember conversations, learn how you like things done, and get sharper the more you work together" ([x.ai](https://x.ai/news/introducing-grok-bot)).
  - A named Bot keeps "its memory, files, browser sessions, and preferences across sessions." Each Bot's conversations stay separate, and only a few facts (such as name and time zone) are shared across Bots ([docs overview](https://docs.x.ai/grok-bot/overview)).
- **Routines:**
  - Show a Bot a workflow once and it "saves your workflow as a routine" ([x.ai](https://x.ai/news/introducing-grok-bot)).
  - Routines can be re-run on demand or on a schedule ([launch post](https://forum.cursor.com/t/introducing-grok-bot/168053)). They are saved as a skill and keep running with the app or laptop closed ([docs overview](https://docs.x.ai/grok-bot/overview)).
- **Team of Bots:**
  - A "chief of staff" Bot oversees specialists for inbox, expenses, recruiting, bug fixes and operations.
  - Bots "can independently message each other and share context in threads," and "You can also place Bots in a group chat where they can coordinate on their own" ([x.ai](https://x.ai/news/introducing-grok-bot)).
  - Bots can pass ownership of work to each other ([VentureBeat](https://venturebeat.com/orchestration/spacexais-grok-bot-turns-agents-into-persistent-digital-coworkers-that-can-operate-your-apps-for-120-per-month)).

#### Channels for assigning and supervising work [OFFICIAL]
- **Where to talk to Bots:** "Use the Grok Bot desktop app on macOS, Windows, or Linux, or the companion app on iOS or Android." You can also use **X Chat**, **tag @bot on X**, or talk to a **shared Bot in Slack** ([FAQ](https://docs.x.ai/grok-bot/faq)).
- **Input modes and continuity:** Input can be typed, dictated or a voice chat ([docs overview](https://docs.x.ai/grok-bot/overview)). Users "pick up the same thread later on either surface" ([x.ai](https://x.ai/news/introducing-grok-bot)).
- **Not official channels:**
  - The docs do not mention email-to-task, SMS, Telegram, WhatsApp, Microsoft Teams or Discord ([FAQ](https://docs.x.ai/grok-bot/faq); [shared bots](https://docs.x.ai/grok-bot/shared-bots)).
  - [3P] On 2026-10-16 a user shared a workaround: have the Bot open Telegram Web on its cloud computer and scan the QR code to link it ([X/@byalexai](https://x.com/byalexai/status/2111081404939288825)). This is computer use standing in for a missing native channel.
  - [3P] A search found no official WhatsApp or Slack integration beyond Team Bots. Third-party bridges exist.

#### Shared "Team Bots" in Slack: the surface comparable to Claude Tag [OFFICIAL]
- **Personal vs shared:**
  - "A Bot is either personal or shared." Sharing "doesn't make a copy, and each person's chat with a shared Bot is private."
  - "Shared Bots always run in the cloud" ([shared bots](https://docs.x.ai/grok-bot/shared-bots)).
- **Slack behaviour:**
  - The Bot works in Slack channels, threads and DMs. Teammates message it directly or **mention it in channels**, and it "acts for the teammate who mentioned it."
  - In a thread it follows, a routine Bot "answers later replies without an @mention" ([shared bots](https://docs.x.ai/grok-bot/shared-bots)).
- **Approvals in Slack:** In a Slack thread, "Only the person who asked can answer it" (the "Review an action" card). In a group chat, only the person whose message is being answered can approve ([shared bots](https://docs.x.ai/grok-bot/shared-bots)).
- **Identity and apps:**
  - In Slack channels, threads, group DMs and group chats, a shared Bot "uses one shared computer of its own" and "uses only its owner's apps."
  - A connected account "always acts as the person the Bot is answering."
  - "Vault isn't available on shared Bots" ([shared bots](https://docs.x.ai/grok-bot/shared-bots); [security](https://cursor.com/docs/grok-bot/security)).
- **Roles:**
  - Owner: the only one who can delete the Bot, which deletes it for the whole team.
  - Manager: can change "its instructions, skills, memory, and apps."
  - Collaborator: can chat with it.
  - Source: [shared bots](https://docs.x.ai/grok-bot/shared-bots)
- **Privacy:** "The owner and Managers can't read your chats, and you can't read the owner's or anyone else's." The Bot can save team-wide learnings and says when it does ([shared bots](https://docs.x.ai/grok-bot/shared-bots)).
- **Billing:**
  - A shared-Bot reply "uses the Grok Bot usage of the person it's answering."
  - Exception: the owner's usage pays for the owner's own messages and for Slack posts from apps or workflows not linked to a Cursor account ([shared bots](https://docs.x.ai/grok-bot/shared-bots)).
- **Setup:**
  1. You must be on a **Cursor team**.
  2. The owner or a Manager chooses **Connect** under "Bring to your team's Slack." If it shows "Awaiting admin approval," a Slack workspace admin must approve it.
  3. Each teammate links Slack under Settings > General > Shared Bots > Link.
  - Source: [shared bots](https://docs.x.ai/grok-bot/shared-bots)

#### @bot on X, a public tagging channel distinct from @grok [OFFICIAL]
- **How it works:** "Tag @bot in a post or reply on X to hand that post to your Grok Bot as a task." @bot replies with only a short confirmation, and the work and results stay in the Grok Bot app.
- **What the Bot receives:** the post, the post it replies to, and any quoted posts, including still photos.
- **Usage notice:** On the first tagged request, X shows a notice that tagged requests "start work automatically and use your Grok Bot usage."
- **Requirements:**
  - A Grok account with X connected
  - Grok Bot access
  - At least one Bot of your own ("Bots shared with you can't take tagged requests")
  - An X account based in a supported region
- **Excluded regions:** accounts based in Australia, the UK, the EU, Iceland, Liechtenstein or Norway, or accounts whose X location shows only a broad region.
- **When @bot ignores a post:** if the post also tags @grok, contains a video or GIF, fails moderation, or the user's weekly usage is used up.
- Source: [docs: Tag @bot on X](https://docs.x.ai/grok-bot/tag-on-x)
- **Launch date unconfirmed.** An X post dated 2026-10-23 lists "Tag @bot under a post on X" among Grok Bot uses ([X/@XFreeze](https://x.com/XFreeze/status/2113510936242016555)), so the feature existed by then.

#### Approvals and supervision model [OFFICIAL]
- **When Bots come back to you:** Bots "only come back when something needs your approval" and learn "when to ping versus keep going" ([x.ai](https://x.ai/news/introducing-grok-bot)).
- **Approval cards** offer Allow once, Always allow (which saves a rule) and Deny ([help: approvals](https://cursor.com/help/grok-bot/approvals), via search summary).
  - "An approval controls the proposed action, not work already completed."
  - "Nobody should approve an action whose target or effect they can't identify" ([security](https://cursor.com/docs/grok-bot/security)).
- **Auto-review:**
  - "An independent review model that evaluates risky Bot actions before they run." It "doesn't review every side effect."
  - Team admins can enforce it (Enterprise only) ([security](https://cursor.com/docs/grok-bot/security)).

#### Team, enterprise and admin controls [OFFICIAL]
- **Access by plan:**
  - Teams: "Included; every member has access, and usage follows the seat's allowance."
  - Enterprise: "Enable Grok Bot for your team from your dashboard" ([teams docs](https://cursor.com/docs/grok-bot/teams)).
  - FAQ: "On Cursor Enterprise, an admin enables Grok Bot for the team; contact your Cursor account team" ([FAQ](https://docs.x.ai/grok-bot/faq)).
  - Grok Enterprise: "Contact your account manager about Grok Bot access" ([plans help](https://cursor.com/help/grok-bot/plans)).
- **Admin console:** Admins manage Grok Bot from the Grok Bot page of the **Cursor dashboard**.
- **Rules and policies:**
  - "Rules applied to Grok Bot are always required, so members can't turn them off."
  - "Enforce Auto-review" is Enterprise only.
  - Team Marketplace: "Any permitted connector is available to every Bot a member runs." Connector policy is set on the Plugins & MCPs page.
  - **"No access by default"**: "A Bot can use only the accounts and connected apps the user or team grants it."
  - Source: [teams docs](https://cursor.com/docs/grok-bot/teams)
- **Identity:** "Members sign in to Grok Bot with their Cursor account, so your existing SCIM and SSO setup carries over." SCIM is Enterprise only ([teams docs](https://cursor.com/docs/grok-bot/teams)).
- **Enterprise-only logging and analytics:**
  - Audit logs: "Admin, security, and authentication events, plus Grok Bot control-plane events"
  - Action Recording: off by default
  - Conversation Insights: has a Grok Bot source
  - Source: [teams docs](https://cursor.com/docs/grok-bot/teams)
- **Spend control:** "A separate Grok Bot spend cap is not available today"; only account-level on-demand controls apply ([teams docs](https://cursor.com/docs/grok-bot/teams)).
- **Network policy:** "Teams without a policy default to allow-all." Allowlist mode permits "Only your list, plus the destinations a computer needs to function" ([security](https://cursor.com/docs/grok-bot/security)).
- **At launch [REPORTED, 08-11]:** Cursor Premium Teams offered centralized billing and settings, a team marketplace for skills and plugins, shared usage analytics, and SAML/OIDC SSO ([VentureBeat](https://venturebeat.com/orchestration/spacexais-grok-bot-turns-agents-into-persistent-digital-coworkers-that-can-operate-your-apps-for-120-per-month)).

### Inferences
- **The admin, identity and billing layer is Cursor's, not X's or Grok's.**
  - The evidence: admin console in the Cursor dashboard; SSO and SCIM via the Cursor account; computers in Cursor's cloud; terms © Anysphere; downloads and sales on cursor.com/api2.cursor.sh.
  - Operationally, Grok Bot looks like a **Cursor-built product under the Grok brand**, sold through SpaceX's consumer (X/Grok) and developer (Cursor) channels.
- **The Claude Tag comparison is real but narrower than it looks.**
  - Grok Bot can be @-mentioned in Slack channels only as a **shared Team Bot**, which needs a Cursor team and Slack-admin approval.
  - It acts as the teammate who mentioned it and bills that teammate's personal usage pool.
  - The home surface remains Grok Bot's own app. Slack is one of several entry points (app, X Chat, @bot on X, Slack), not the core of the product.
- **The shared computer per account is both a cost and a risk design.**
  - Cost: one persistent VM per user, not per Bot, keeps compute down.
  - Risk: every Bot inherits every login on that computer. The docs explicitly tell users to treat anything on it as available to all their Bots.
- **The @bot channel on X is a consumer acquisition funnel.**
  - Replies are public confirmations, but the work is private.
  - An unlinked user gets "Sign up for Grok Bot: https://x.ai/bot."
  - It is geofenced out of the EU, EEA, UK and Australia. The reason is not stated; possibly regulatory exposure (inference).

### Gaps
- The exact launch dates for @bot tagging, X Chat access, the Bot's own email inbox, and the "Calls" capability: I found no official changelog entries. The x.ai changelog page (x.ai/changelog/bot) returned HTTP 403.
- Whether the cloud computer is idle-suspended when no task is running, and its OS and specs: not documented.
- Whether Microsoft Teams, Discord, WhatsApp, Telegram or email-to-task are on the roadmap: not found.
- The official date of the plan-eligibility expansion (third parties say 2026-08-26): not found in official sources.
- Whether Bots hand coding work to Cursor Cloud Agents: claimed by third-party guides, not confirmed in the official docs I read.

---

## 2. Relationships: to Cursor, to the Grok models (and third-party models), to SpaceX's SI segment, and to The X Subscription

### Takeaway
- **Cursor:** Grok Bot is effectively a Cursor-operated product. It uses Cursor accounts, the Cursor cloud, the Cursor dashboard and Anysphere terms. It launched three days before the Cursor merger closed.
- **Models:** It is **not tied to Grok models**. There is no model picker. Since Musk's post of 2026-10-07, SpaceX says it routes each task to "the best back end model," and the docs now officially name **Opus 5.5** (Anthropic) as a routing target.
- **SpaceX:** Its filings name it only qualitatively, as a contributor to SI-segment growth.
- **The X Subscription:** Grok Bot is the headline feature of the 2026-10 bundle, included from Premium ($30) upward and drawing on one weekly pool shared with Grok and Cursor.

### Cited Findings

#### Cursor [OFFICIAL]
- **Merger terms:** The merger closed 2026-08-14 at a $60B implied equity value. GAAP purchase consideration was $42.5B, and $23.4B of equity awards is attributed to post-acquisition service. Cursor contributed **$608M** of revenue from closing through 2026-09-30 ([Q3 release](https://www.sec.gov/Archives/edgar/data/1181412/000162828026066482/spaceexplorationtechnologi.htm)).
- **Product plumbing runs through Cursor:**
  - The docs state that the Bots' computers run in Cursor's cloud ([docs overview](https://docs.x.ai/grok-bot/overview)).
  - "Members sign in to Grok Bot with their Cursor account" ([teams docs](https://cursor.com/docs/grok-bot/teams)).
  - The Terms page footer reads "© 2026 Anysphere, Inc." and defines Provider only as "the entity providing Grok Bot to Customer under the Agreement" ([Grok Bot Terms](https://cursor.com/terms/grok-bot)).
  - The launch was announced on the Cursor forum by Cursor staff ([Cursor forum](https://forum.cursor.com/t/introducing-grok-bot/168053)).
- **Pre-merger collaboration:** The pre-merger compute agreement provided for the parties to "jointly develop SI models and related model-specific deliverables or products" ([10-Q](https://www.sec.gov/Archives/edgar/data/1181412/000162828026066880/spcx-20260930.htm)).
- **Legacy Cursor plans:** Grok Bot is included with every paid individual plan and with Cursor Teams ([docs overview](https://docs.x.ai/grok-bot/overview)). Usage on legacy plans is metered on the Cursor account, not on Grok or X ([plans help](https://cursor.com/help/grok-bot/plans)).
- **[3P]** Guides say the iOS app lists Anysphere as developer and that "Grok Bot uses your Cursor account" (search summary of [learncursor.dev](https://www.learncursor.dev/learn/cursor-agents/grok-bot-setup) and others). I did not verify the iOS developer listing.

#### Models: routed, multi-vendor, no picker
- **At launch [REPORTED, 08-11]:** Bots route tasks to underlying models automatically. Matt Shumer: "You don't choose a model for your Grok Bot"; he said the router "wasn't great" in early testing but was later told it had improved ([VentureBeat](https://venturebeat.com/orchestration/spacexais-grok-bot-turns-agents-into-persistent-digital-coworkers-that-can-operate-your-apps-for-120-per-month)).
- **2026-10-07 [MUSK]:** "Going forward, @SpaceX will use the best back end model for any given task." He also wrote "Whatever is most likely to give you the best outcome," and named **Anthropic's Claude Opus 5.5, Midjourney and Suno**.
  - TNW notes he gave no details on routing and did not confirm the change was live.
  - Mobile and web access problems had been reported the day before, with no official cause given ([TNW, 2026-10-07](https://thenextweb.com/news/grok-bot-claude-opus-midjourney-suno-musk)).
  - [3P] Some outlets claim "all Bots will run on Claude Opus 5.5 by default" per "Grok Bot staff" ([tbreak](https://tbreak.com/grok-bot-claude-opus-midjourney-suno/); [AI Weekly](https://aiweekly.co/alerts/musk-grok-bot-to-route-tasks-to-claude-opus-55-midjourney-suno)). TNW does not support this; treat it as **unverified**.
- **Current docs [OFFICIAL]:**
  - "Can I pick the model?" "No." Grok Bot "picks the model for each task, and there is no model picker."
  - Models may be first-party SpaceXSI or third-party providers on the sub-processor list.
  - "Routing a task to Opus 5.5 doesn't change what it costs you per token" ([FAQ](https://docs.x.ai/grok-bot/faq)).
  - Separately, third-party models chosen *in Cursor* are "not included in the weekly usage pool" and are billed as Additional Usage ([usage limits](https://docs.x.ai/x-subscription/usage-limits)). Routing inside Grok Bot does not seem to follow that rule.

#### SpaceX "Super Intelligence (SI)" segment [OFFICIAL]
- **How the filings mention Grok Bot:**
  - Q3 release (2026-10-20): "Added $6.7 billion of incremental SI solutions and infrastructure revenue year-over-year, driven by cloud services agreements, the acquisition of Cursor, and our growing SI models and software applications, including Grok Bot."
  - Also: "Accelerated Grok model release deployment and introduced Grok Bot, our always-on agentic SI system" ([Q3 release](https://www.sec.gov/Archives/edgar/data/1181412/000162828026066482/spaceexplorationtechnologi.htm)).
- **Q3 SI figures:**
  - Revenue **$7.6B**, up 195% quarter over quarter and 830% year over year
  - Income from operations **$431M**, "inclusive of $1.2 billion in share-based compensation from the Cursor acquisition"
  - Adjusted EBITDA **$5.0B**
  - Capex **$16.4B**
  - Nameplate compute **1.7 GW** at 2026-09-30, building out COLOSSUS II
  - Source: [Q3 release](https://www.sec.gov/Archives/edgar/data/1181412/000162828026066482/spaceexplorationtechnologi.htm)
- **Segment description (10-Q):** "a vertically integrated SI platform spanning SI infrastructure solutions…, as well as a frontier LLM Grok, SI-assisted software engineering platform Cursor, and… X."
- **Revenue recognition (10-Q):** subscriptions are recognized "ratably over the period of the subscription term (ranging from month-to-month to one year) or as services are consumed for usage based arrangements, including on-demand usage in excess of included or pre-committed amounts in the arrangement." Grok Bot gets no separate line or metric ([10-Q](https://www.sec.gov/Archives/edgar/data/1181412/000162828026066880/spcx-20260930.htm)).

#### The X Subscription (launched 2026-10-12/13) [OFFICIAL]

| Tier | Monthly | Annual | Included usage | Grok Bot |
|---|---|---|---|---|
| Free | $0 | $0 | Limited | No (trial credit only) |
| Plus | $8 | $80 | Base | **No** |
| Premium | $30 | $300 | 4x | Yes |
| Super | $100 | $1,000 | 15x | Yes |
| Ultra | $200 | $2,000 | 50x | Yes |

- **Prices:** Team seats cost the same per seat on Premium, Super and Ultra; Plus is not available for teams ([pricing](https://docs.x.ai/x-subscription/pricing)).
- **Which tiers include Grok Bot:** "To use Grok Bot, choose Premium or higher" ([X Subscription: Grok Bot](https://docs.x.ai/x-subscription/grok-bot)). Plus and Free do not include it, and SuperGrok Lite is not included ([plans help](https://cursor.com/help/grok-bot/plans)).
- **Shared pool:**
  - "Grok Bot usage draws from the same weekly pool as Cursor and the Grok apps, not a separate Grok Bot meter" ([plans help](https://cursor.com/help/grok-bot/plans)).
  - The pool is per seat: "Seats do not share weekly pools." The team shares only a prepaid balance for Additional Usage ([usage limits](https://docs.x.ai/x-subscription/usage-limits)).
- **Switching ("unifying") is optional but permanent:** "It can't be undone, and Support can't undo it for you" ([X Subscription: Grok Bot](https://docs.x.ai/x-subscription/grok-bot)).
- **On the earnings call:** Shotwell said, "we further integrated the portfolio by announcing a new scheme that brings together Grok, Grok bot, [Cursor and X Premium on a single plan]" ([transcript](https://www.fool.com/earnings/call-transcripts/2026/10/20/spacex-spcx-q3-2026-earnings-call-transcript/)).
- **Linked and legacy access** ([plans help](https://cursor.com/help/grok-bot/plans)):
  - Individual SuperGrok Heavy gets the highest linked usage, then SuperGrok Plus, then SuperGrok.
  - X Premium+ gets linked usage, ranked below SuperGrok Plus.
  - A self-serve Grok Business seat matches its license.
  - A Cursor plan and a link don't stack: "Grok Bot uses whichever one grants more weekly usage."

### Inferences
- **Grok Bot is the clearest example of SpaceX's "vertical integration" thesis.**
  - The stack: SpaceX compute and models, Cursor's agent runtime and enterprise plumbing, and X as a distribution and data surface (@bot tags, X Chat, an X plugin billed in X API credits).
  - The multi-vendor router undercuts the "own model" leg. SpaceX positions Grok Bot as an outcome product, not a showcase for Grok models.
- **The router makes the cost base partly external.**
  - Routing to Claude Opus 5.5 means some inference is bought from a competitor (Anthropic).
  - The FAQ promises users an unchanged per-token cost, so SpaceX absorbs any price difference.
  - This is a plausible contributor to the late-October cost squeeze, but there is no data on routing shares.
- **The bundle turns Grok Bot from an add-on with "its own usage" (August) into the reason to upgrade within a single pool (October).**
  - Excluding it from Plus ($8) sets the effective price floor for agents at $30.

### Gaps
- No disclosed share of Grok Bot tasks routed to Grok versus third-party models.
- No commercial terms for Anthropic, Midjourney or Suno.
- No disclosure of whether Midjourney or Suno routing is live.
- No separate Grok Bot revenue, ARR or user metric in the 10-Q or release (only the earnings-call remark; see §3).
- Who legally provides Grok Bot to X Subscription customers (Anysphere vs SpaceXAI/SpaceXSI): the Grok Bot Terms define "Provider" only generically.

---

## 3. Target users, value-proposition claims, launch demos and use cases, adoption metrics, user reception

### Takeaway
- **Who it targets:** Officially, B2B knowledge-work teams: sales, ops, engineering, marketing. SpaceX says B2B is its fastest-growing category.
- **How Musk promotes it:** Personally, as a consumer errand-runner: shopping from a credit-card photo, Lego orders, gifts.
- **Only adoption metric:** Shotwell's remark on the earnings call: **800,000 paid weekly users in early October 2026, more than double the prior month.**
- **Reception:**
  - Strong early praise from influential testers in August.
  - Confusion about setup, sign-ins and local use on the Cursor forum.
  - A "rug pull" backlash after the caps ran out early on 10-31/11-01.

### Cited Findings

#### Official value-proposition claims [OFFICIAL]
- **Positioning lines** ([x.ai](https://x.ai/news/introducing-grok-bot)):
  - "Grok Bot is your team of always-on agents."
  - "AI teammates you can give real work to."
  - "Bots share a computer of their own in the cloud, so jobs do not stall when you step away."
  - Bots "sign into the tools you already use," including sites with "no clean API or MCP."
  - They "remember conversations, learn how you like things done."
  - They "only come back when something needs your approval."
- **Company quotes reported at launch:** "Bots are AI teammates that do real work for you" and "They sign in to your tools, use them just like you do, and come back with finished work" ([VentureBeat](https://venturebeat.com/orchestration/spacexais-grok-bot-turns-agents-into-persistent-digital-coworkers-that-can-operate-your-apps-for-120-per-month)).
- **Employee testimonials on the launch page** ([x.ai](https://x.ai/news/introducing-grok-bot)):
  - Vincent (Growth): "Working with Grok Bot feels like I have eight arms like an octopus."
  - Bennett (Sales): "I showed Grok Bot a workflow once and now I just fully trust it to run forever."
  - Roman (Product): "There is a huge difference between 90% done and 100% done."

#### Launch use cases [OFFICIAL]
- **Sales outbound:** overnight account research, contact scoring, and drafted email and LinkedIn messages for approval.
- **Demo readiness:** checks the environment overnight and fixes seeds and stale data.
- **Pipeline ops:** CRM hygiene, stall flags and a Monday scoreboard.
- **Account follow-up.**
- **Internal examples:**
  - A sales Bot updating the CRM from call notes
  - An ops Bot handling new-hire seating and Gmail invoices
  - An engineering Bot reproducing a bug, filing a ticket and handing off a fix
- **Other teams:** marketing campaigns and office operations.
- Source: [x.ai](https://x.ai/news/introducing-grok-bot)

#### Target segments and executive claims (Q3 call, 2026-10-20) [OFFICIAL / EXEC FORECAST]
- **Shotwell:**
  - "Grok bot, our Agentic Assistant, launched in August, and we already had paid weekly users of 800,000." A transcript summary adds that this was in early October and "more than doubling over the previous month."
  - "B2B is our fastest-growing category with companies increasingly using Grok bot as a force multiplier", "across sales, marketing, IT, and engineering" per the call summary, "treating it as an always-on teammate for complex end-to-end workflows."
  - "Grok bot, in particular, is seeing strong adoption out of the gate."
  - Source: [Motley Fool transcript](https://www.fool.com/earnings/call-transcripts/2026/10/20/spacex-spcx-q3-2026-earnings-call-transcript/)
- **Musk:**
  - "the fastest growth of a product that I've ever seen in my life for Grok bot."
  - "the usage of Grok bot grows over time per user, it does not decline."
  - [EXEC FORECAST] "We already are in early deployment with some trusted customers… we expect to be in wide deployment within a few weeks for enterprise customers."
  - [EXEC FORECAST] "Grok bot may end up being what uses most of our Super Intelligence compute."
  - Source: [transcript](https://www.fool.com/earnings/call-transcripts/2026/10/20/spacex-spcx-q3-2026-earnings-call-transcript/)
- **Context from the same call:** Shotwell said "Grok Enterprise customers with more than $1 million in run rate revenue, including Cursor," were up 50% quarter over quarter on a pro forma basis. The 50% figure comes from call summaries ([transcript](https://www.fool.com/earnings/call-transcripts/2026/10/20/spacex-spcx-q3-2026-earnings-call-transcript/); [TradingView/MarketBeat](https://www.tradingview.com/news/marketbeat:482358b5c094b:0-what-spacex-spcx-said-on-its-q3-earnings-call/)).
- **Metric definition:** "Paid weekly users" is undefined, and the figure is **not** in the 10-Q or the earnings release. I searched both texts.

#### Musk's consumer demos [MUSK; REPORTED]
- **Shopping and travel** ([TNW, 2026-10-16](https://thenextweb.com/news/musk-grok-bot-credit-card-spacex-make-you-whole)). Dates are from TNW's weekday references and approximate:
  - About 10-10: "To buy stuff, you can just take a picture of your credit card, drop it in the chat." When a user said Amazon was blocking the agent, Musk said Grok "will find another way to buy stuff."
  - About 10-11: "I just asked Grok @Bot to research the most fun Lego Star Wars kits for young kids and order them. As easy as that." Per Mashable, the order was $241.34.
  - About 10-15: a user said Grok Bot had booked a flight from a card photo.
- **Gifts:** He has continued to promote Grok Bot as a way to buy gifts for family ([TechCrunch via Yahoo, 11-02](https://finance.yahoo.com/technology/ai/articles/elon-musk-throttles-grok-bot-162321608.html)).
- **Finances [REPORTED, Aug 2026]:** After a Teslaconomics post about connecting Grok Bot to a bank account, Musk wrote "If Grok Bot messes up, we will make you whole" ([Benzinga](https://www.benzinga.com/markets/tech/26/08/61455209/elon-musk-just-made-a-wild-promise-to-user-willing-to-give-grok-their-bank-account-promises-we-will-make-you-whole-if-it-messes-up); search-snippet level, with the month taken from the URL path).

#### User reception
- **Early testers [REPORTED, 08-11]** ([VentureBeat](https://venturebeat.com/orchestration/spacexais-grok-bot-turns-agents-into-persistent-digital-coworkers-that-can-operate-your-apps-for-120-per-month)):
  - Lenny Rachitsky: "I haven't been this excited about a new AI product in a while," and "like OpenClaw, but super easy, reliable, and less scary to use."
  - Matt Shumer: "an agent for everything, not just code"; "It worked out of the box."
- **Cursor forum topics** (titles, dates and stats only; I did not read the replies) ([Cursor forum](https://forum.cursor.com/t/introducing-grok-bot/168053)):
  - "Grok Bot: per-agent browser still requires separate sign-in despite account-level login promise" (undated)
  - "Confused over Grok Bot" (2026-10-19; 22 replies, 1,545 views)
  - "Bot as the Orchestrator of local CLIs" (2026-09-01)
  - "Grok Bot - Dev Team Orchestration Feedback" (2026-09-30)
  - "Local computer use for Grok Bots" (2026-11-02; 9 replies)
- **Throttle backlash [REPORTED, 11-02]:**
  - TechRadar's headline: "…as users call the move a 'rug pull'" ([TechRadar](https://www.techradar.com/ai-platforms-assistants/elon-musk-says-grok-bot-was-throttled-because-costs-were-unsustainably-high-as-users-call-the-move-a-rug-pull)). I could read only the headline.
  - Users said their usage "ran out really fast this week" ([TNW](https://thenextweb.com/news/grok-bot-costs-unsustainably-high-musk-usage-limits)).
- **[3P] Review:** eesel.ai, from about mid-August, reviewed the docs and first-week Hacker News and Reddit reports. It calls the product real but says it lacks three controls for safe work-queue use: "rehearsal, scope, and a record" ([eesel.ai](https://www.eesel.ai/blog/grok-bot-review)). Its account of HN and Reddit sentiment is secondhand.

### Inferences
- **The value proposition has two audiences that pull in different directions.**
  - The official pitch (GTM/ops/engineering teams, Slack Team Bots, enterprise admin) sits close to Claude Tag's "teammate" framing.
  - Musk's personal promotion (credit-card-photo shopping, Lego, flights, bank accounts) markets a consumer "do-anything" agent through X.
  - The consumer channel brings volume and the B2B channel brings margin. The consumer path also carries the highest liability exposure (§6).
- **The 800k "paid weekly users" figure is a usage metric, not a revenue metric.** Many of these users pay for Cursor, SuperGrok or the bundle anyway. More than doubling in a month right after the plan expansion (≈08-26) fits a fast funnel through existing paid bases rather than new demand alone (inference).
- **Usage per user grows over time** (Musk). This is good for engagement but bad for a fixed-price pool, which links directly to §4.

### Gaps
- No official definition of "paid weekly users," and no retention, task-volume, Bot-count or enterprise-customer-count figures for Grok Bot.
- I could not read Reddit or Hacker News threads directly. Sentiment here comes from press and forum titles.
- Musk's Grok Bot "fastest-growing" remark is also attributed to Forbes about two weeks before 11-02 ([The Decoder](https://the-decoder.com/spacexsi-apparently-cuts-grok-bot-quotas-weeks-after-musk-called-it-his-fastest-growing-product/)). It is probably the 10-20 call remark; not verified independently.

---

## 4. Value capture: how Grok Bot's usage is priced and metered; the 2026-10-31 to 11-01 throttling; what it reveals about the unit economics of always-on agent compute

### Takeaway
- **Model:** Grok Bot is monetized as a **subscription with a weekly usage pool plus metered overage**, not per seat or per agent-hour.
  - In August it was gated to $120–$300 plans with "its own usage."
  - Around 08-26 it was reportedly opened to $20–$30 plans.
  - Since 2026-10-12/13 it has drawn on a shared weekly pool in The X Subscription ($30/$100/$200). Usage beyond the pool is bought as prepaid credits on the new plans, or as on-demand usage on legacy Cursor plans.
- **Meter:** Consumption is measured by "agent steps and tokens," not messages.
- **The squeeze:**
  - On 10-20 management celebrated 800k paid weekly users and growing usage per user.
  - On 10-31 to 11-01 users' weekly pools ran out early.
  - Musk said serving costs were "unsustainably high relative to revenue."
  - No official change to limits was published, and the docs list only relative multipliers.
- **The lesson:** Always-on agents break flat-price bundles. Usage scales with autonomy and time, not with human attention.

### Cited Findings

#### Launch-era pricing (Aug 2026) [SUPERSEDED for new buyers]
- **Plans at launch:** SuperGrok Heavy **$300/mo**, Cursor Ultra **$200/mo** and Cursor Premium Teams **$120/seat/mo**.
  - Per VentureBeat, Cursor Ultra included a computer for Grok Bot, tool access, scheduled routines, desktop and mobile operation, and extended AI-token limits (paraphrase) ([VentureBeat](https://venturebeat.com/orchestration/spacexais-grok-bot-turns-agents-into-persistent-digital-coworkers-that-can-operate-your-apps-for-120-per-month)).
- **Separate meter at launch:** "Grok Bot comes with its own usage, separate from your Grok and Cursor plans" ([x.ai](https://x.ai/news/introducing-grok-bot)). The x.ai page still says this, but the docs supersede it for X Subscription users.
- **[3P] Price floor:**
  - Around 08-26 the cheapest route reportedly fell to Cursor Pro at $20.
  - After The X Subscription launched, $20 Cursor Pro was reportedly closed to new Grok Bot sign-ups, putting the entry price for new customers at $30 Premium. Existing Cursor Pro users keep access ([KuCoin](https://www.kucoin.com/news/flash/grok-bot-access-expanded-to-20-month-cursor-pro-subscribers); [CellCog](https://cellcog.ai/blog/grok-bot-cursor-pro/)).
  - Unverified officially. The official plans page still lists legacy Pro as having "weekly usage, below Pro+" ([plans help](https://cursor.com/help/grok-bot/plans)).

#### Current metering [OFFICIAL, docs read 2026-11-03]
- **Unit of consumption:** "measured by agent steps and tokens rather than the number of messages you send" (stated for the trial credit) ([FAQ](https://docs.x.ai/grok-bot/faq)). "Usage rates vary by product, so different activities can draw different amounts from the pool" ([usage limits](https://docs.x.ai/x-subscription/usage-limits)).
- **Pool:** a weekly pool per paid seat for "eligible first-party usage," shared across Grok, Grok Bot and Cursor. "Usage resets weekly" ([usage limits](https://docs.x.ai/x-subscription/usage-limits); [docs overview](https://docs.x.ai/grok-bot/overview)).
- **Tier sizes:** The only official numbers are multipliers: Plus = base, Premium 4x, Super 15x, Ultra 50x ([pricing](https://docs.x.ai/x-subscription/pricing)). No absolute token, step or agent-hour allowances are published.
- **Overage on The X Subscription:**
  - "Any further first-party usage is Additional Usage, paid from additional usage credits bought with top-ups."
  - Users can turn on Auto Top-up and set a monthly limit.
  - "Spending caps can limit paid Additional Usage" ([usage limits](https://docs.x.ai/x-subscription/usage-limits); [plans help](https://cursor.com/help/grok-bot/plans)).
  - The credit price is not published in the docs I read.
- **Overage on legacy Cursor plans:**
  - Extra usage is "On-demand usage… Billed through Cursor" and counts toward the "On-demand monthly limit."
  - "A limit is not a hard stop in the middle of a run."
  - "On legacy Teams plans, on-demand usage is enabled by default" ([plans help](https://cursor.com/help/grok-bot/plans)).
  - "If on-demand is off, Grok Bot stops when weekly usage runs out and resumes when weekly usage resets" ([FAQ](https://docs.x.ai/grok-bot/faq)).
- **Team billing:**
  - "A separate Grok Bot spend cap is not available today" ([teams docs](https://cursor.com/docs/grok-bot/teams)).
  - Shared-Bot replies are charged to the asker's pool ([shared bots](https://docs.x.ai/grok-bot/shared-bots)).
  - @bot tags on X "start work automatically and use your Grok Bot usage" ([tag on X](https://docs.x.ai/grok-bot/tag-on-x)).
- **Free trial:** a usage credit with a 7-day window, available on free-plan accounts. The trial never converts into a paid plan, and cancelling is free ([plans help](https://cursor.com/help/grok-bot/plans)).

#### The throttling episode, 2026-10-31 to 11-01 [REPORTED + MUSK]
- **What users saw:**
  - TechCrunch (paraphrased): some users' weekly limits were unexpectedly maxed out over Halloween weekend, even without overnight use, with reported usage jumping from as low as 50% to 100% ([TechCrunch via Yahoo, Amanda Silberling, 2026-11-02](https://finance.yahoo.com/technology/ai/articles/elon-musk-throttles-grok-bot-162321608.html)).
  - The Telegraph, via TNW and The Decoder: one meter jumped from 33% to 66%, and several users had quotas "cut in half" ([TNW](https://thenextweb.com/news/grok-bot-costs-unsustainably-high-musk-usage-limits); [The Decoder](https://the-decoder.com/spacexsi-apparently-cuts-grok-bot-quotas-weeks-after-musk-called-it-his-fastest-growing-product/)).
  - Cursor forum users reported meters flipping between 77% and 100%. Cursor reported a billing and dashboard problem on Sunday 11-01 and later marked it resolved. It is unclear whether that caused the jumps ([TNW](https://thenextweb.com/news/grok-bot-costs-unsustainably-high-musk-usage-limits)).
- **What Musk said on 11-01, in replies, not on his main feed:**
  - To Doge Norway, who asked for a usage reset: **"The cost of serving Grok Bot was unsustainably high relative to revenue."**
  - He asked another user whether a plan with more usage would cost too much.
  - He also wrote "just being sensible" ([TNW](https://thenextweb.com/news/grok-bot-costs-unsustainably-high-musk-usage-limits)).
  - Per TechCrunch (paraphrased), he confirmed the change was not a glitch and has not said how subscribers' limits will be affected going forward ([TechCrunch via Yahoo](https://finance.yahoo.com/technology/ai/articles/elon-musk-throttles-grok-bot-162321608.html)).
- **No official notice:**
  - The pricing, usage-limit and plans docs I read on 11-03 contain no dated statement of a limit change ([usage limits](https://docs.x.ai/x-subscription/usage-limits); [plans help](https://cursor.com/help/grok-bot/plans)).
  - The Grok Bot Terms were revised on 10-31 ([Terms](https://cursor.com/terms/grok-bot)). I found no evidence linking that revision to the limits.
- **Industry context [REPORTED]:**
  - OpenAI will merge separate ChatGPT and Codex allowances starting in December 2026 (Telegraph, via [TNW](https://thenextweb.com/news/grok-bot-costs-unsustainably-high-musk-usage-limits) and [The Decoder](https://the-decoder.com/spacexsi-apparently-cuts-grok-bot-quotas-weeks-after-musk-called-it-his-fastest-growing-product/)).
  - Per The Decoder (paraphrased), Anthropic says an agent costs four to 15 times as much as a simple chat ([The Decoder](https://the-decoder.com/spacexsi-apparently-cuts-grok-bot-quotas-weeks-after-musk-called-it-his-fastest-growing-product/)).
  - Meta's Muse passed 10M users in October, and Zuckerberg called it "very compute-intensive" and not yet profitable (Business Insider, via [TNW](https://thenextweb.com/news/grok-bot-costs-unsustainably-high-musk-usage-limits)).

#### Compute economics stated by management (Q3 call, 2026-10-20) [OFFICIAL / EXEC FORECAST]
- **Musk on demand:**
  - "AI or SI agents will on a per agent basis, use far more bandwidth than a person."
  - "there will be far more agents than there are people."
  - "I think we're going to face a shortage of super intelligence compute."
- **Musk on supply:**
  - "our approach is to rent at the -- effectively the spot market rate."
  - "half of the compute is used internally and half of it is rented to others."
  - "we've actually tried to offload some of our inference compute to other cloud providers."
- **CFO Bret Johnsen:**
  - "At current pricing, new compute deployments are generating a payback of less than a year."
  - "Pricing is currently tracking slightly above the high end of the $30 to $50 per watt range."
  - "we'll continuously evaluate the highest margin usage for our compute assets."
- Source: [transcript](https://www.fool.com/earnings/call-transcripts/2026/10/20/spacex-spcx-q3-2026-earnings-call-transcript/)

### Inferences
- **The tier ladder is a volume discount, which concentrates subsidy on the heaviest agent users.** Monthly list price per unit of base usage, from official prices and multipliers:

  | Tier | Price ÷ multiplier | Price per base unit (monthly) | Annual billing |
  |---|---|---|---|
  | Premium | $30 ÷ 4 | **$7.50** | $6.25 |
  | Super | $100 ÷ 15 | **≈$6.67** | ≈$5.60 |
  | Ultra | $200 ÷ 50 | **$4.00** | ≈$3.34 |

  An always-on Bot user is exactly the user who climbs to Ultra and pays about 47% less per unit than Premium. If those users' realized consumption also grows over time (Musk: usage "grows over time per user"), the gross margin per unit falls fastest where volume is highest.
- **Opportunity cost of compute is the binding constraint, not only cash cost.**
  - The CFO describes rented compute paying back in under a year at the high end of the $30–$50/W range, and the company promises to pursue "the highest margin usage for our compute assets."
  - Against that, every GPU-hour consumed by a $30–$200 flat subscription competes with spot-rate third-party rentals.
  - Musk's "unsustainably high relative to revenue" is consistent with an internal transfer-price comparison. This is my inference; no internal pricing was disclosed.
- **Bounding the revenue at stake:**
  - 800k paid weekly users × $30–$200/month ≈ **$24M–$160M per month**, or about **$72M–$480M per quarter**.
  - That is roughly **1–6% of Q3 SI revenue ($7.6B)**, even if all of it were attributed to Grok Bot, which overstates it because many of these users pay for Cursor or SuperGrok anyway.
  - The point: Grok Bot is strategically loud but financially small next to compute rentals. That makes it cheap for SpaceX to tighten limits and expensive, in GPU terms, to let them run.
- **Levers SpaceX can pull to close the gap**, all visible in the evidence:
  1. Raise the price floor: Plus excluded; reported closure of the $20 Cursor Pro route to new sign-ups.
  2. Shrink included pools quietly. The docs publish only relative multipliers, so the base can change without the page changing.
  3. Push upgrades, as in Musk's "would a bigger plan cost too much?"
  4. Monetize overage through prepaid Additional Usage credits with Auto Top-up, and on-demand usage that is on by default for legacy Teams.
  5. Re-route to cheaper models. The router is opaque and there is no model picker.
- **The meter design itself causes trust problems.**
  - Consumption is in "agent steps and tokens," invisible and unpredictable to users, while the Bot works autonomously, including routines and public @bot tags that "start work automatically."
  - Users therefore cannot connect meter jumps to their own actions. That produces the "rug pull" framing, even if part of the episode was a billing/dashboard bug.
- **Contrast for the case comparison:** A per-seat agent such as Claude Tag (per the brief) and a subscription-pool agent such as Grok Bot face the same cost driver: autonomous tokens and compute per hour of agent life. Grok Bot shows the failure mode when that driver sits inside a consumer-priced bundle shared with chat and coding.

### Gaps
- No published absolute allowances (tokens, steps, agent-hours) for any tier, before or after 10-31.
- No price for Additional Usage credits.
- No confirmation of whether the pool was formally reduced.
- No official SpaceX or SpaceXSI statement beyond Musk's replies.
- No disclosure of Grok Bot cost to serve, compute per Bot, or how idle persistent computers are charged, internally or to users.
- I could not access The Telegraph or Forbes originals; their claims come via TNW and The Decoder.

---

## 5. Positioning versus competitors (brief)

### Takeaway
- **Press framing:** Grok Bot is a general "agent for everything" that operates real apps on a hosted computer. It is compared to Anthropic's computer use, Claude Code and Cowork; OpenAI's Codex, Workspace Agents and ChatGPT Work; OpenClaw; and Meta's Muse.
- **Differentiators claimed:** ease of use and reliability compared with OpenClaw, multi-Bot teams with routines, and distribution through X, Cursor and the bundle.
- **Weaknesses:** cost-driven limits and a shared-login risk model.

### Cited Findings
- **VentureBeat's comparison set (08-11):**
  - Anthropic: computer use for Claude (2024), the Claude Code harness (early 2025), Claude Cowork (early 2026).
  - OpenAI: Codex controlling other apps (April), Workspace Agents, ChatGPT Work.
  - Rachitsky called Grok Bot "like OpenClaw, but super easy, reliable, and less scary to use."
  - Source: [VentureBeat](https://venturebeat.com/orchestration/spacexais-grok-bot-turns-agents-into-persistent-digital-coworkers-that-can-operate-your-apps-for-120-per-month)
- **The Decoder's comparison set (11-02, paraphrased):** Grok Bot is comparable to Meta's Muse, OpenAI's Dot and Anthropic's Claude Duet ([The Decoder](https://the-decoder.com/spacexsi-apparently-cuts-grok-bot-quotas-weeks-after-musk-called-it-his-fastest-growing-product/)). I have not verified the product names "Dot" and "Claude Duet"; treat them as The Decoder's wording.
- **Meta's Muse:** passed 10M users in October; "very compute-intensive," not yet profitable (Business Insider, via [TNW](https://thenextweb.com/news/grok-bot-costs-unsustainably-high-musk-usage-limits)).
- **Co-opetition with Anthropic:** Grok Bot now routes some tasks to **Anthropic's Claude Opus 5.5** ([FAQ](https://docs.x.ai/grok-bot/faq); [TNW 10-07](https://thenextweb.com/news/grok-bot-claude-opus-midjourney-suno-musk)). Anthropic is both a supplier and the maker of the competing Claude Tag.
- **Coverage gaps:** Manus and Tencent WorkBuddy were not named in any Grok Bot coverage I found.

### Inferences
- **Comparison axes that matter for the three-case study:**
  1. **Entry channel.** Grok Bot's own app plus Slack (Team Bots), X (@bot) and X Chat, compared with Claude Tag (Slack-native) and WorkBuddy (WeChat/WeCom).
  2. **Execution locus.** A cloud computer per account in Cursor's cloud, optionally with local commands, compared with WorkBuddy's desktop agent.
  3. **Model.** A multi-vendor router including a rival's model, compared with single-vendor stacks.
  4. **Pricing.** A consumer-style bundle pool, compared with per-seat or enterprise pricing.
- **Grok Bot's distinctive asset is distribution bundling** (X, Grok and Cursor in one subscription), not model exclusivity, since it routes to Claude.

### Gaps
- No head-to-head benchmarks or win-rate data versus Claude Tag, OpenAI agents, Manus or WorkBuddy.
- No market-share data for always-on agents.

---

## 6. Safety and governance: permission model, data access, incidents, regulatory attention

### Takeaway
- **Official model:** delegated identity. The Bot acts as the signed-in member, with "no access by default." Secrets are kept out of the model, outside content is marked untrusted, and there are approvals plus an independent Auto-review model.
- **Structural risk:** all of a user's Bots share one computer's logins, and teams without a network policy default to allow-all.
- **Contract vs Musk:** The terms place nearly all risk on the customer: actions are irreversible, and the provider is not liable except for gross negligence or willful misconduct. Musk publicly promised to "make you whole" for purchase or card mistakes, which contradicts the terms.
- **No confirmed breach or regulatory action specific to Grok Bot was found.** @bot tagging is geofenced out of the EU, EEA, UK and Australia.

### Cited Findings

#### Permission and data model [OFFICIAL]
- **Credentials and identity:** "A Bot has no identity or credentials of its own"; "Bots act as the signed-in member"; "Credentials stay out of the model"; "Passwords and one-time codes never belong in ordinary chat" ([security](https://cursor.com/docs/grok-bot/security)).
- **Prompt-injection defense:** "Outside content is marked as untrusted data when presented to the model." These defenses "reduce, but don't eliminate, risk from malicious content" ([security](https://cursor.com/docs/grok-bot/security)).
- **Session persistence:** "Application sessions persist only on the member's computer." "Deleting a Bot doesn't remove computer files or browser sessions" ([security](https://cursor.com/docs/grok-bot/security)).
- **Data retention and training:**
  - Data is "deleted or returned within 30 days of written direction after the service ends."
  - "With Privacy Mode enabled, customer data is not used for training" ([security](https://cursor.com/docs/grok-bot/security)).
  - "Grok Bot requires cloud data storage, so Legacy Privacy Mode is not supported." The training opt-out follows Cursor account settings ([FAQ](https://docs.x.ai/grok-bot/faq)).
- **Shared credentials across Bots:** The launch post says to treat logins and files on the computer as available to all of a user's Bots ([Cursor forum](https://forum.cursor.com/t/introducing-grok-bot/168053)).
- **Network:** Teams without a policy default to allow-all, and "Blocking an app doesn't block that service's website" ([security](https://cursor.com/docs/grok-bot/security)).

#### Contract terms [OFFICIAL]
- **Version:** "Last updated October 31, 2026"; the previous version was dated 2026-10-12.
- **Customer responsibility:**
  - "Customer is responsible for configuring and administering available settings."
  - "Customer is solely responsible for determining the scope of access, permissions, and authority granted to each Bot."
  - "Approval and automated-review controls are provided as aids only."
  - Controls "do not reverse completed actions."
- **Human review:** required before Agentic Actions involving "financial transactions."
- **Liability:** "PROVIDER IS NOT LIABLE FOR ANY LOSS, DAMAGE, COST, CHARGE, COMMITMENT… EXCEPT TO THE EXTENT DIRECTLY CAUSED BY PROVIDER'S GROSS NEGLIGENCE, OR WILLFUL MISCONDUCT." Agentic Actions are provided "as is."
- **No dollar cap appears in this text.**
- Source: [Grok Bot Terms](https://cursor.com/terms/grok-bot)
- **SpaceXSI consumer terms:** last updated 10-12, they state: "We are not responsible for User Content or Agentic Actions" ([TNW 10-16](https://thenextweb.com/news/musk-grok-bot-credit-card-spacex-make-you-whole)).
- **[3P] Conflicting claim:** A blog asserts "The Terms Say $100" as a liability cap ([88labs](https://88labs.ai/blog/grok-bot-bank-account-liability-cap-ai-agents)). This is not found in the Cursor-hosted Grok Bot Terms. I did not check the SpaceXSI consumer terms for a $100 cap.

#### Musk's pledges versus the terms [MUSK; REPORTED]
- **About 10-15:** "This works well. If Grok Bot makes a mistake with a purchase or leaks your card (never happened yet), SpaceX will make you whole." SpaceX did not immediately respond to Business Insider's request for comment ([TNW, 2026-10-16](https://thenextweb.com/news/musk-grok-bot-credit-card-spacex-make-you-whole)).
- **Aug 2026, bank account:** "If Grok Bot messes up, we will make you whole" ([Benzinga](https://www.benzinga.com/markets/tech/26/08/61455209/elon-musk-just-made-a-wild-promise-to-user-willing-to-give-grok-their-bank-account-promises-we-will-make-you-whole-if-it-messes-up), search-snippet level).
  - Cybernews researchers warned that bank access could expose balances, transactions and personal data.
  - Researcher Rasa Jurgutytė noted that reimbursement cannot undo a data leak ([Cybernews](https://cybernews.com/ai-news/grok-bot-connect-bank-acccount/), search-snippet level; the page returned 403).
- **Musk's own Lego order (about 10-11):** a user said Musk had leaked his Grok Bot email, delivery address and order number. Musk replied that the address had "already been doxxed multiple times" ([TNW 10-16](https://thenextweb.com/news/musk-grok-bot-credit-card-spacex-make-you-whole)).

#### Incidents, research and regulation
- **No breach found:** I found no confirmed security breach or data incident specific to Grok Bot. The 10-Q's litigation and regulatory disclosures, including the Virginia AG notice of 2026-10-19, concern Grok image generation and the @grok/Grok products, not Grok Bot. The 10-Q never mentions Grok Bot ([10-Q](https://www.sec.gov/Archives/edgar/data/1181412/000162828026066880/spcx-20260930.htm)).
- **[3P] Security commentary:**
  - Prompt-injection demonstrations against web-reading agents ([DEV Community](https://dev.to/aiza-hextyx/grok-bot-prompt-injection-how-malicious-web-content-can-hijack-an-ai-agent-2elh)).
  - Warnings that shared logins and vault create a single point of failure ([hackernoob.tips](https://hackernoob.tips/grok-bot-always-on-agents-hacking-risk/)).
  - A claimed "October 5 password-snapshot bug" with no confirmed fix by October 25 (search summary of third-party guides; **unverified**).
- **Regional geofence:** @bot tagging is unavailable for X accounts based in Australia, the UK, the EU, Iceland, Liechtenstein and Norway ([tag on X](https://docs.x.ai/grok-bot/tag-on-x)).

### Inferences
- **Governance is mature on paper for teams, thin for consumers.**
  - Teams and Enterprise get "no access by default," required Team Rules, enforceable Auto-review, SCIM, audit logs, network allowlists and Action Recording, most of it Enterprise-only.
  - Individual consumers, the audience Musk courts with card-photo shopping, rely on defaults: allow-all network, a shared computer, and approvals left to the user.
- **The gap between "make you whole" and terms that disclaim liability is a governance risk in itself.**
  - CEO posts create implied promises that the contract denies.
  - This is relevant to the value-proposition-versus-risk comparison with Claude Tag and WorkBuddy, if their providers make no such public guarantees (check against those notes).
- **The geofence suggests regulatory caution.** The excluded regions match jurisdictions with strict AI, DSA/online-safety or consumer-protection regimes. No reason is given.

### Gaps
- No public incident log or transparency report for Grok Bot.
- No independent security audit or certification found; the teams docs refer to "certifications" on the security page, but I saw none listed.
- I could not verify the third-party bug claims, the $100 cap claim, or the SpaceXSI consumer-terms text beyond TNW's quote.

---

## 7. Mapping Grok Bot to the three business-model elements: 价值主张 (Value Proposition), 价值创造与传递 (Value Creation & Delivery), 价值获取 (Value Capture)

### Takeaway
- **价值主张 (value proposition):** "A team of always-on AI teammates with their own computer" that finish real work inside the tools you already use, including tools without APIs, while you are away. For individuals it is sold as a consumer errand-runner; for teams, as GTM/ops/engineering labor.
- **价值创造与传递 (creation and delivery):** created on SpaceX compute and a multi-vendor model router, through Cursor's agent runtime: a cloud computer per account, connectors/MCP plus computer use, memory and routines. Delivered through the Grok Bot apps, X Chat, @bot on X, Slack Team Bots, and the Cursor/Grok/X bundle.
- **价值获取 (value capture):** subscription bundles with a shared weekly pool and metered overage credits. The launch's premium gating ($120–$300) gave way to mass-market bundling ($30+). On 10-31/11-01 the cost of always-on usage forced visible throttling.

### Cited Findings

#### 价值主张 (Value Proposition)
- **Core promise:** "your team of always-on agents"; "AI teammates you can give real work to"; "jobs do not stall when you step away"; works on sites "with no clean API or MCP"; "only come back when something needs your approval" ([x.ai](https://x.ai/news/introducing-grok-bot)).
- **B2B framing:** "B2B is our fastest-growing category… treating it as an always-on teammate for complex end-to-end workflows" (Shotwell, 10-20) ([transcript](https://www.fool.com/earnings/call-transcripts/2026/10/20/spacex-spcx-q3-2026-earnings-call-transcript/)).
- **Consumer framing:** card-photo shopping, Lego and flight bookings, and the "SpaceX will make you whole" pledge ([TNW 10-16](https://thenextweb.com/news/musk-grok-bot-credit-card-spacex-make-you-whole)).

#### 价值创造与传递 (Value Creation & Delivery)
- **Key resources:** SI nameplate compute of 1.7 GW, and Cursor (acquired 08-14) ([Q3 release](https://www.sec.gov/Archives/edgar/data/1181412/000162828026066482/spaceexplorationtechnologi.htm)). The Bot computers run in Cursor's cloud ([docs](https://docs.x.ai/grok-bot/overview)).
- **Models:** routed per task with no picker; first-party SpaceXSI and third-party models including Opus 5.5 ([FAQ](https://docs.x.ai/grok-bot/faq)).
- **Key activities:** computer use, connectors and MCP, routines and skills, multi-Bot coordination, approvals and Auto-review ([docs](https://docs.x.ai/grok-bot/computer-and-apps); [security](https://cursor.com/docs/grok-bot/security)).
- **Channels:** desktop and mobile apps, X Chat, @bot on X, and Slack (Team Bots, from 09-28) ([FAQ](https://docs.x.ai/grok-bot/faq); [X/@bot](https://x.com/bot/status/2104661562715967548)).
- **Partners:** Anthropic, Midjourney and Suno as model suppliers (stated by Musk; Opus 5.5 confirmed in the FAQ); Slack; Google and Microsoft apps via connectors; 1Password ([TNW 10-07](https://thenextweb.com/news/grok-bot-claude-opus-midjourney-suno-musk); [docs](https://docs.x.ai/grok-bot/computer-and-apps)).

#### 价值获取 (Value Capture)
- **Pricing:** X Subscription Premium $30, Super $100 and Ultra $200 (4x/15x/50x pools), with Grok Bot excluded from Free and Plus. Additional Usage is sold as prepaid credits; legacy Cursor plans use on-demand billing ([pricing](https://docs.x.ai/x-subscription/pricing); [plans help](https://cursor.com/help/grok-bot/plans)).
- **Throttle:** 10-31/11-01; Musk: "unsustainably high relative to revenue" ([TNW](https://thenextweb.com/news/grok-bot-costs-unsustainably-high-musk-usage-limits)).
- **Revenue recognition:** subscriptions ratably; usage as consumed, "including on-demand usage in excess of included or pre-committed amounts" ([10-Q](https://www.sec.gov/Archives/edgar/data/1181412/000162828026066880/spcx-20260930.htm)).

### Inferences
**Comparison grid: Grok Bot values to set against Claude Tag and WorkBuddy** (fill the other columns from `claude_tag.md` and `workbuddy.md`):

| Dimension | Grok Bot (as of 2026-11-03) |
|---|---|
| Primary surface | Own Grok Bot app (desktop and mobile); secondary: Slack (shared Team Bots), X (@bot public tag), X Chat |
| @-mention in team channels | Yes, Slack only, via shared Team Bots (since 2026-09-28); needs a Cursor team and Slack-admin approval |
| Agent identity | None of its own; acts as the signed-in member, or as the teammate who mentioned it in Slack |
| Execution locus | Persistent cloud computer per account (Cursor cloud), one screen per Bot; optional local commands (ask by default) |
| "Team" concept | Multiple named Bots plus a chief-of-staff Bot; Bot-to-Bot messaging and group chats; shared Bots with Owner/Manager/Collaborator roles |
| Model strategy | Opaque per-task router, multi-vendor (Grok plus Claude Opus 5.5 and others); no picker |
| Pricing unit | Consumer-style subscription tier with a weekly pool shared with Grok chat and Cursor; per-seat team pricing equals the consumer tiers; overage via prepaid credits or on-demand |
| Metering unit | "Agent steps and tokens"; absolute allowances unpublished |
| Who pays in shared contexts | The asker's personal pool, not a team or Bot budget; no Grok Bot-specific spend cap |
| Admin and governance | Cursor dashboard; Team Rules; enforceable Auto-review, SCIM, audit logs and Action Recording (Enterprise); network default allow-all |
| Liability posture | Terms disclaim liability, but the CEO publicly pledges to "make you whole" |
| Cost-shock evidence | Weekly caps ran out early on 10-31/11-01; CEO: costs "unsustainably high relative to revenue" |

**Business-model reading:**
- Grok Bot's 价值主张 is the broadest of the three cases: any app, consumer or business, 24/7.
- Its 价值创造 relies on owning compute and buying distribution through X, Cursor and the bundle.
- Its 价值获取 is the weakest-coupled. A flat, pooled consumer subscription funds an always-on workload whose cost rises with autonomy and with time per user.
- The 10-31/11-01 throttle is the observable symptom of that misfit.

### Gaps
- No Grok Bot-specific revenue, gross margin or cost to serve.
- No absolute usage allowances.
- No official account of what changed in the limits on 10-31/11-01.
- These gaps prevent a quantitative unit-economics comparison with Claude Tag and WorkBuddy; the comparison has to stay structural.
