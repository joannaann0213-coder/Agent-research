# Claude Tag (Anthropic's Claude in Slack): business-model case profile, current as of 2026-11-03

Source-type labels used below: **[OFFICIAL]** = Anthropic, Slack/Salesforce, GitHub, OpenAI or Cursor first-party docs and announcements; **[PRESS]** = tech or business press; **[3P]** = third-party, vendor or aggregator blog (low reliability, often from competitors). Dates are publication or effective dates. "Current docs" means the claude.com/docs/claude-tag pages as fetched on 2026-11-03, which are labeled "Public Beta" throughout.

## Q1. What is Claude Tag? Naming history, launch dates, availability, and current feature set (as of Nov 2026)

### Takeaway
Claude Tag (launched 2026-06-23, still in public beta in Nov 2026) is the third generation of Anthropic's Slack app. The first was the per-user "Claude App for Slack" (2023), then "Claude in Slack" (2025-10-01) plus "Claude Code in Slack" (2025-12-08). Claude Tag keeps the same `@Claude` Slack app but replaces those per-user versions with one admin-provisioned agent identity shared per channel. Each thread runs a persistent Claude Code-style cloud sandbox session, and Claude Tag adds channel memory, routines and proactive behavior. It is available only to Team and Enterprise organizations on Anthropic's first-party service. It switched over on 2026-08-03, and the legacy bot was retired in paired workspaces on 2026-10-05.

### Cited Findings
**Naming and dated timeline**
- **2023 (March):** Anthropic released a "Claude App for Slack" in beta. It was built on Slack's platform for thread summaries and Q&A, and was free during the beta. **[3P aggregator summary; the original anthropic.com/news/claude-now-in-slack page now returns HTTP 404, so this could not be checked against the original]** — [Plushcap summary](https://www.plushcap.com/content/anthropic/blog/anthropic-claude-now-in-slack)
- **2025-10-01:** Anthropic announced "Claude and Slack", which ran in two directions:
  - (a) The Claude app added to a Slack workspace through the Slack Marketplace "for teams on paid Slack plans", usable via DMs, the AI assistant panel, and @Claude in threads. In public threads, Claude drafted replies privately first so the user could review them before sharing.
  - (b) A Slack connector inside the Claude apps that searches channels, DMs and files. It launched for Team/Enterprise and was extended to Pro and Max on 2026-01-26.
  - Rob Seaman, Slack CPO at Salesforce, was quoted: "Partnering with Anthropic to integrate Claude into Slack and Slack context into Claude." **[OFFICIAL]** — [Claude blog: Claude and Slack](https://claude.com/blog/claude-and-slack)
- **Legacy app limits (Slack Help Center, still live):** The app required a paid Claude account (Pro, Max, Team or Enterprise) and was "available on all Slack plans". In channels it read the 20 most recent messages, and in threads the 50 most recent replies. Slack conversations were not synced with the user's Claude web history. **[OFFICIAL]** — [Slack Help: Use Claude in Slack](https://slack.com/help/articles/53532192117267-Use-Claude-in-Slack). This conflicts with the Oct 2025 blog's "paid Slack plans" wording, so the policy may have changed.
- **2025-12-08:** "Claude Code in Slack" launched as a beta research preview.
  - When @Claude was tagged with a coding task, it created a Claude Code cloud session from the thread or channel context and posted progress in the thread. **[PRESS]** — [TechCrunch](https://techcrunch.com/2025/12/08/claude-code-is-coming-to-slack-and-thats-a-bigger-deal-than-it-sounds); [VentureBeat](https://venturebeat.com/ai/anthropics-claude-code-can-now-read-your-slack-messages-and-write-code-for); [ITPro](https://www.itpro.com/software/development/claude-code-is-coming-to-slack-heres-how-to-use-it-what-it-can-do-and-how-to-get-access)
  - Official doc details:
    - Two routing modes: "Code only" or "Code + Chat".
    - Channels only, no DMs.
    - "Each session runs under your own Claude account, using your connected repositories and your plan limits."
    - Buttons: View session and Create PR.
    - Docs warn that "Claude may follow directions from other messages in the context." **[OFFICIAL]** — [Claude Code docs: Claude Code in Slack](https://code.claude.com/docs/en/slack)
- **`/install-slack-app`:** a Claude Code CLI command that will "Install the Claude Slack app. Opens a browser to complete the OAuth flow." **[OFFICIAL]** — [Claude Code commands reference](https://code.claude.com/docs/en/commands)
- **Slack Marketplace listing (app ID A08SF47R6P4):** The listing is "Claude" by Anthropic: "Just start a DM with Claude, or tag Claude in a thread… ask Claude to help with coding tasks to automatically route work to Claude Code."
  - Scopes include "Perform actions as you".
  - HIPAA compliant: No.
  - "By default, your Slack conversations aren't used to train our models or stored within them."
  - The listing has not been updated to mention Claude Tag. **[OFFICIAL]** — [Slack Marketplace listing](https://slack.com/marketplace/A08SF47R6P4)
- **2026-06-23:** Anthropic announced Claude Tag.
  - "a new way for teams to work with Claude… the beginning of an evolution of Claude Code."
  - Beta, for Claude Enterprise and Team customers.
  - "Works with Opus 4.8."
  - "Replaces the existing Claude in Slack app," with admins able to opt in to migrate within 30 days.
  - Anthropic said it aims to expand to "other places they work". **[OFFICIAL]** — [Anthropic: Introducing Claude Tag](https://www.anthropic.com/news/introducing-claude-tag)
  - The press called it a "research preview". **[PRESS]** — [Engadget](https://engadget.com/2199619/anthropic-announces-claude-tag-for-slack)
- **2026-08-03:** "Claude in Slack switched over to the new Claude Tag experience on August 3, 2026." **[OFFICIAL]** — [Claude Help: What is Claude Tag](https://support.claude.com/en/articles/15594475-what-is-claude-tag); Slack's own help center gives the same date. — [Slack Help](https://slack.com/help/articles/53532192117267-Use-Claude-in-Slack)
- **2026-10-05:** "Since October 5, 2026, the earlier Claude in Slack app, shown as Legacy in admin settings, no longer answers in channels of a workspace paired with your Claude organization."
  - Workspaces and channels can no longer be switched back to Legacy.
  - In unpaired workspaces, Pro, Max and Ultra users still get the earlier app in channels.
  - The legacy and new versions are "two versions of the same `@Claude` Slack app, not two apps." **[OFFICIAL]** — [Docs: Migrate from the earlier Claude in Slack](https://claude.com/docs/claude-tag/admins/migrate-from-earlier)
  - The legacy Claude Code in Slack now "answers channel @mentions only from Pro, Max, and Ultra accounts, and only in a Slack workspace that no organization has connected to Claude Tag." **[OFFICIAL]** — [Claude Code docs: Slack](https://code.claude.com/docs/en/slack)
- **Legacy vs. Claude Tag (official comparison table):**

  | | Legacy | Claude Tag |
  | :- | :- | :- |
  | Identity | Each user's own claude.ai account | "One agent identity with org-level service credentials" |
  | Sessions | Spawned per request | "One persistent session per thread, shared with the channel" |
  | Memory | None | Per-channel memory plus workspace notes |
  | Standing work | None | Routines and channel watching |
  | Setup | Each user | "An Owner, once" |

  Code is now authored by the Claude GitHub App rather than under the requesting user's name. **[OFFICIAL]** — [Docs: Migrate](https://claude.com/docs/claude-tag/admins/migrate-from-earlier)

**Availability (plans, platform, region)**
- "Claude Tag is available on Team and Enterprise plans, on Anthropic's first-party service. It isn't available on individual plans (Free, Pro, Max, or Ultra), or for third-party deployments." **[OFFICIAL]** — [Docs: Claude Tag overview](https://claude.com/docs/claude-tag/overview); [Docs: Setup overview](https://claude.com/docs/claude-tag/admins/setup-overview)
  - Some third-party explainers seen in search results claim Pro access. The official wording above explicitly excludes Pro, so prefer the official docs.
- **Ultra plan:** "Mentions of Max in the post from before October 30, 2026 include what are now the Max and Ultra plans." So the Ultra individual tier was split out of Max around 2026-10-30. **[OFFICIAL]** — [Claude blog: Claude and Slack (editor's note)](https://claude.com/blog/claude-and-slack)
- **Platforms:** Slack only. The product page lists Microsoft Teams as "Coming soon" with a waitlist, as of 2026-11-03. **[OFFICIAL]** — [claude.com/tag](https://claude.com/tag)
- **Not available in some organizations:** Claude Tag isn't available in an organization with Zero Data Retention, customer-managed encryption keys, or the HIPAA configuration applied to Claude Code and Cowork. **[OFFICIAL]** — [Docs: Healthcare](https://claude.com/docs/claude-tag/admins/healthcare)

**Current feature set (official docs, Nov 2026)**
- **Invocation:** Anyone in a channel tags `@Claude`. A task "gets a working session in a thread… The work runs in an ephemeral cloud sandbox." Users can also DM Claude and use it in group DMs.
  - The sandbox "runs the same engine that powers Claude Code on the web."
  - Claude clones GitHub repos into the sandbox and pushes branches or PRs.
  - The sandbox is released a few minutes after a turn. The thread's session persists and is rebuilt on the next reply. **[OFFICIAL]** — [Docs: How Claude Tag works](https://claude.com/docs/claude-tag/concepts/how-it-works)
- **Repository config:** CLAUDE.md files and `.claude/skills` from a granted repository load into the session. Local `~/.claude` settings, hooks and local MCP servers do not. **[OFFICIAL]** — [Docs: Claude Tag for Claude Code users](https://claude.com/docs/claude-tag/concepts/for-claude-code-users)
- **Long-running and proactive work:**
  - Claude "can schedule its own tasks and pursue projects over hours or days." When enabled, it "flags relevant information and follows up on stalled threads". **[OFFICIAL]** — [Anthropic announcement](https://www.anthropic.com/news/introducing-claude-tag)
  - Routines cover scheduled jobs, channel watching and PR subscriptions. **[OFFICIAL]** — [Docs index](https://claude.com/docs/llms.txt)
  - A channel's "Respond automatically" setting "is on by default." **[OFFICIAL]** — [Docs: Restrict access](https://claude.com/docs/claude-tag/admins/restrict-access)
- **On-call product:** Set up at claude.ai/oncall. Claude watches monitoring tools and auto-joins incident channels, for example by matching the prefix `incident-`. It is billed as usage. **[OFFICIAL]** — [Docs: On-call](https://claude.com/docs/claude-tag/admins/setup-oncall)
- **Memory:**
  - Memory "belongs to the channel, not to you."
  - Public channels can write "workspace notes" that Claude reads in every channel.
  - Private channels read workspace notes but write only their own notes. **[OFFICIAL]** — [Docs: Memory](https://claude.com/docs/claude-tag/users/memory)
- **Models:** At launch Claude Tag worked with Opus 4.8. **[OFFICIAL]** — [Announcement](https://www.anthropic.com/news/introducing-claude-tag)
  - Current docs: "The options include Opus and Sonnet models," filtered by organization policy.
  - Users switch models in plain language ("switch to Claude Opus 5.5"). Channels can have a default model that follows a family ("latest Opus").
  - A `!fast` fast mode costs more per token. **[OFFICIAL]** — [Docs: Models](https://claude.com/docs/claude-tag/users/models)
  - Opus 5.5 was reportedly released 2026-09-22. **[3P]** — [claudelog](https://claudelog.com/claude-news/)
- **Connectors:** Amplitude, Asana, Jira/Confluence, BigQuery, Datadog, GitLab, Gong, Google Drive/Calendar/Gmail, HubSpot, Linear, Notion, PagerDuty, Salesforce, Sentry, Snowflake, Stripe and Vercel. Also available:
  - Custom MCP servers.
  - GitHub through the Claude GitHub App.
  - "Federated agent access", where short-lived signed identity tokens reach AWS roles, Google Cloud, gateways and OAuth servers.
  - Plugins and skills, plus a skills repository where "Claude can open pull requests to improve its own skills."
  - Users' own claude.ai "personal connectors", used for their own requests after approval. **[OFFICIAL]** — [Docs index (llms.txt)](https://claude.com/docs/llms.txt)
- **Admin controls:**
  - Setup is Owner-only at claude.ai/admin-settings/claude-tag and pairs the workspace with an `@Claude connect` code.
  - Per-channel bundles (sets of connections, repos and skills).
  - Spend limits for the organization and per channel.
  - Restrict who can use Claude: to organization members on Team, or to roles on Enterprise.
  - Guest channels default to "Restrict".
  - Slack Connect channels are off unless enabled.
  - Blocked and auto-join channel-name patterns, a DM toggle, and an Activity/audit page.
  - On Enterprise, Owners can delegate a "Claude Tag Admin" role. **[OFFICIAL]** — [Docs: Restrict access](https://claude.com/docs/claude-tag/admins/restrict-access); [Docs: Setup overview](https://claude.com/docs/claude-tag/admins/setup-overview)
  - Per-channel on/off switches ("limiting Claude to approved channels") require Enterprise. **[OFFICIAL]** — [Docs: Healthcare](https://claude.com/docs/claude-tag/admins/healthcare)

### Inferences
- "Claude Tag" is better read as a new operating and billing model for the existing `@Claude` Slack app than as a new app. The identity moves from the individual user to the organization's agent, sessions become persistent per thread, and billing moves from seats to organization consumption. The product line converges on Claude Code's cloud-sandbox engine, which is consistent with Anthropic calling it "an evolution of Claude Code."
- The rollout was fast: announced 2026-06-23, default switchover 2026-08-03, legacy retired 2026-10-05. This suggests Anthropic deliberately forced Team and Enterprise workspaces onto the consumption-billed version within about 3.5 months. Individual-plan users keep the legacy per-user app only in unpaired workspaces.

### Gaps
- No general-availability date. Every doc still says "Public Beta" as of 2026-11-03.
- Regional availability is not stated in any official source found. Docs say only "Anthropic's first-party service", so Bedrock and Vertex deployments are excluded.
- The 2023 Claude App for Slack's end-of-life date was not found, and the original 2023 page is gone (404).
- The Microsoft Teams launch date is unknown (waitlist only).

## Q2. 价值主张 Value Proposition: target customer, job-to-be-done, value claims, customer evidence and metrics

### Takeaway
The proposition is a shared, visible, persistent AI teammate that anyone in a Slack channel can hand real work to, with no per-user setup and no seat needed. It runs governed by admins and delivers finished artifacts (PRs, docs, queries and charts, tickets) rather than chat answers. The buyer is a Team or Enterprise IT owner. The users are cross-functional Slack teams, with engineering and on-call as the anchor use cases. Evidence is mainly Anthropic's internal statistic plus a few customer quotes. No adoption metrics have been disclosed.

### Cited Findings
- **Positioning:**
  - "Multiplayer: One Claude per channel serves everyone, and anyone can pick up a conversation where another left off."
  - It learns over time and takes initiative ("ambient"). It works asynchronously "over hours or days". **[OFFICIAL]** — [Anthropic announcement](https://www.anthropic.com/news/introducing-claude-tag)
  - Anthropic describes it as "working with a real colleague — one that can produce work in public view." **[PRESS quoting Anthropic]** — [TechCrunch, 2026-06-23](https://techcrunch.com/2026/06/23/anthropics-claude-tag-is-learning-your-company-one-slack-message-at-a-time/)
- **Cat Wu, head of product for Claude Code and Cowork:**
  - "We see Claude Tag as an evolution of Claude Code."
  - "Claude Code, Cowork, and chat are very single-player, whereas Claude Tag is built to be interactive and multiplayer."
  - Fortune frames the product as a "virtual employee". **[PRESS]** — [Fortune, 2026-06-23](https://fortune.com/2026/06/23/anthropic-claude-tag-virtual-employee-tool-slack/)
- **Official segmentation:** "team work → Claude Tag; personal work → Cowork or Claude Code."

  | | Claude Tag | Cowork | Claude Code |
  | :- | :- | :- | :- |
  | Whose access | "The team's: service-account credentials an admin sets per channel" | Personal OAuth | Local credentials |
  | Who sees the work | "Everyone in the channel" | Just you | Just you |

  **[OFFICIAL]** — [Docs: How it works](https://claude.com/docs/claude-tag/concepts/how-it-works)
- **Jobs-to-be-done listed in the docs:**
  - Watch monitors and alerts
  - Triage request channels
  - Find answers in docs
  - Answer data questions (warehouse query plus chart)
  - Track projects and chase approvals
  - Turn threads into docs and tickets
  - Fix bugs (report → draft PR, follow CI to green)
  - Work with GitHub repos
  - Pull deal state
  - Review documents against a checklist
  - Marketing-team recipes
  - "Work from your own channel"

  **[OFFICIAL]** — [Docs: Overview](https://claude.com/docs/claude-tag/overview); [Docs index](https://claude.com/docs/llms.txt)
- **Target teams and industries on the product page:** support, engineering, data, sales, marketing and events teams. Industry pages cover customer support, cybersecurity, legal, sales, financial services, healthcare and life sciences.
  - Tagline: "Tag Claude in Slack." Claude "reads threads, understands full context, and reacts in real time." **[OFFICIAL]** — [claude.com/tag](https://claude.com/tag)
  - Caveat for healthcare: "Claude Tag… is not covered by Anthropic's Business Associate Agreement." **[OFFICIAL]** — [Docs: Healthcare](https://claude.com/docs/claude-tag/admins/healthcare)
- **Internal evidence:**
  - "Today, 65% of our product team's code is created by our internal version of Claude Tag." Teams also use it for product metrics, support tickets and bug root-cause analysis. **[OFFICIAL]** — [Anthropic announcement](https://www.anthropic.com/news/introducing-claude-tag)
  - Fortune paraphrases this differently: the product team has Claude Tag "approving and incorporating 65% of the code changes it submits". **[PRESS]** — [Fortune](https://fortune.com/2026/06/23/anthropic-claude-tag-virtual-employee-tool-slack/). This is a wording discrepancy, and the official wording should be preferred.
- **Customer metric:** Carvana, "56% fewer alerts after Claude Tag began resolving root causes behind them." **[OFFICIAL vendor marketing]** — [claude.com/tag](https://claude.com/tag)
- **Named customer quotes:**
  - GitLab: Simon Mansfield, Director, Enterprise AI
  - Fractional AI: Darian Bailey
  - Gusto: Chad Kunsman ("Slack is Gusto's entry point")
  - Firecrawl: Micah Stairs
  - Clay: George Dilthey, Head of Support (interface and data access "managed in one place")
  - Hebbia: Aabhas Sharma, CTO (Claude Tag as "first responder for internal bugs" using Datadog, Linear and GitHub)
  - Descript: Matthew Scott

  **[OFFICIAL vendor marketing]** — [claude.com/tag](https://claude.com/tag)
- **Admin-governance value claims:**
  - Each channel setup "acts like a separate Claude identity, and its memories stay scoped to those channels." For example, a sales-configured Claude won't share memories with engineering.
  - Admins can set token spend limits and view a log of actions with the requester of each task. **[OFFICIAL]** — [Anthropic announcement](https://www.anthropic.com/news/introducing-claude-tag)
- **Enterprise adoption barriers the product targets, per Fortune:** lack of training, data siloed across teams, and safety concerns about sensitive-data access and leaks. **[PRESS]** — [Fortune](https://fortune.com/2026/06/23/anthropic-claude-tag-virtual-employee-tool-slack/)

### Inferences
- The proposition has three layers:
  - **Users:** delegation with visible, steerable progress in the place they already talk.
  - **Team leads:** institutional memory that accrues per channel.
  - **IT/security buyers:** a governed agent identity with service accounts, per-channel scoping, audit and spend caps. This is a direct answer to the "shadow AI" and per-user OAuth sprawl of the legacy app.
- Engineering and on-call are the wedge. Most named proof points are code or incident related (65% of internal code, Carvana alerts, Hebbia bugs). The docs then generalize to GTM, marketing and data teams to widen the addressable seat base in each organization.

### Gaps
- No disclosed adoption numbers: organizations, channels, weekly users, or tasks per day.
- No third-party ROI studies.
- No independent user-experience threads located on HN or Reddit. Community commentary found was mostly vendor blogs, for example [eesel review](https://www.eesel.ai/blog/claude-tag-review), which is a competitor and not neutral.
- The Carvana and customer quotes are undated marketing claims, not audited.

## Q3. 价值创造与传递 Value Creation & Delivery: models, Slack surfaces, data and permission model, dependence on Slack/Salesforce policy

### Takeaway
Value is created by Anthropic's own agent runtime: a Claude Code cloud sandbox per thread, Agent Proxy credential injection, connectors and MCP, memory and routines, running Opus and Sonnet models. It is delivered through a single Slack Marketplace app (bot scopes, DMs, the AI assistant panel, public-channel search) and connected tools. Distribution therefore depends on Salesforce-owned Slack. Slack's 2025 API terms make Marketplace listing the only sanctioned commercial channel and throttle unlisted apps. Salesforce is at once Anthropic's partner, investor and customer, and also a competitor through Slackbot.

### Cited Findings
**Runtime and data model (Anthropic side)**
- **Per-thread sandboxes:**
  - "Each Slack thread runs in its own sandbox". Credentials "aren't placed in that sandbox".
  - Outbound requests pass through Agent Proxy, which "injects credentials from the credential store at request time".
  - Egress is "default-deny" apart from GitHub, which goes through a dedicated GitHub proxy limited to granted repos. **[OFFICIAL]** — [Docs: Security and data handling](https://claude.com/docs/claude-tag/concepts/security-and-data)
- **Shared credentials:** "In channels, Claude acts under service credentials of its own, not under the account of the person who tagged it… Anyone in one of those channels can ask Claude to act with the credential." Admins are advised to connect dedicated service identities. **[OFFICIAL]** — [Docs: Security and data handling](https://claude.com/docs/claude-tag/concepts/security-and-data)
- **What Claude reads in Slack:**
  - It reads and posts in channels it has been added to.
  - It can "Search every public channel by keyword, including public channels it hasn't been added to. No admin setting turns this search off." An Owner can only limit search to the channels Claude is in.
  - It "never searches private channels". **[OFFICIAL]** — [Docs: Healthcare](https://claude.com/docs/claude-tag/admins/healthcare)
- **Slack app scopes:** bot scopes for reading and posting in member channels, reactions, files, canvases, user lookup and public-channel search, plus `channels:join` and `users:read.email`.
  - Claude can add itself to public channels via "Add to channel" or "Approve and post" buttons, weekly channel-suggestion DMs, or admin auto-join patterns.
  - Credentials for other tools "live on Anthropic's side rather than in Slack."
  - Uninstalling deletes the workspace's Claude-side data. **[OFFICIAL]** — [Docs: What the Claude Slack app can access](https://claude.com/docs/claude-tag/admins/for-slack-admins)
- **Slack UI surfaces:**
  - Slack's "Agents & tools" / Apps / Home tab.
  - Org-level install on Enterprise Grid and Enterprise+, with admin scope review. **[OFFICIAL]** — [Slack Help](https://slack.com/help/articles/53532192117267-Use-Claude-in-Slack)
  - Claude Tag surfaces are channel tagging, DMs and "the AI assistant panel". **[OFFICIAL]** — [Claude Help: What is Claude Tag](https://support.claude.com/en/articles/15594475-what-is-claude-tag)
- **Retention and deletion:**
  - "During the beta there is no automatic retention period for Claude Tag data." Organizations' custom data-retention settings don't apply to transcripts or memory.
  - Deleting a Slack message or thread doesn't remove it from the transcript.
  - Channel-level sessions are replaced "after about an hour of quiet or about a day of age" and archived, not deleted. **[OFFICIAL]** — [Docs: Data lifecycle](https://claude.com/docs/claude-tag/concepts/data-lifecycle); [Docs: How it works](https://claude.com/docs/claude-tag/concepts/how-it-works)
  - The Help Center says conversations are deleted from Claude within 30 days after disconnecting or uninstalling. **[OFFICIAL]** — [Claude Help](https://support.claude.com/en/articles/15594475-what-is-claude-tag)
- **Training:** "By default, your Slack conversations aren't used to train our models." **[OFFICIAL]** — [Slack Marketplace listing](https://slack.com/marketplace/A08SF47R6P4)
- **Models:** Opus 4.8 at launch. Opus and Sonnet now, with fast mode on Opus. **[OFFICIAL]** — [Announcement](https://www.anthropic.com/news/introducing-claude-tag); [Docs: Models](https://claude.com/docs/claude-tag/users/models)
- **Relationship to Claude Managed Agents:** a separate developer product (a pre-built agent harness behind an API key) configured on the Claude Platform, not shared with Claude Tag's settings. **[OFFICIAL]** — [Docs: Settings map](https://claude.com/docs/claude-tag/concepts/settings-map)

**Slack platform policy (Salesforce side)**
- **2025-05-29, Slack API Terms of Service update and rate-limit change:**
  - Commercially distributed apps not approved for the Slack Marketplace have `conversations.history` and `conversations.replies` moved "from Tier 3 to Tier 1": 1 request per minute, with the `limit` maximum and default cut to 15 objects.
  - The stated purpose is to "prevent bulk data exfiltration by unvetted applications".
  - Marketplace apps and internal customer-built apps are unaffected. Internal apps keep 50+ requests per minute and 1,000 objects.
  - The Marketplace is "the only appropriate channel for commercially distributing apps built with Slack APIs".
  - The terms reinforce "safeguards around how third-party applications can store, use, and share data accessed via Slack APIs."
  - The new limits apply to new unlisted apps and installs from 2025-05-29. The terms are effective immediately for apps created on or after 2025-05-29, and from 2025-06-30 for older apps. **[OFFICIAL]** — [Slack changelog 2025-05-29](https://docs.slack.dev/changelog/2025/05/29/rate-limit-changes-for-non-marketplace-apps)
  - **Conflict:** the changelog says existing installations are not affected. A Slack FAQ reportedly set a later date for existing installs, 2025-09-02 and later 2026-03-03. This was not verified directly. — [Slack FAQ (unverified)](https://docs.slack.dev/ja-jp/2025-05-terms-rate-limit-update-and-faq)
  - The changelog fetched does not itself mention LLM training. Reports of an explicit LLM-training ban could not be confirmed from the page retrieved.
- **2026-02-17, Slack MCP server and Real-Time Search (RTS) API reach GA:**
  - Both had a limited release in October 2025.
  - RTS gives "secure, query-based access to Slack data" "without storing customer data on external servers". The MCP server "respects user permissions".
  - "25x increase in both RTS queries and MCP tool calls"; "more than 50" partners including Anthropic, Google, OpenAI and Perplexity.
  - Claude and Claude Code are both listed as partner agents. Scott White, Head of Product for Claude, is quoted. **[OFFICIAL]** — [Slack blog](https://slack.com/blog/news/mcp-real-time-search-api-now-available)

**Anthropic–Salesforce relationship**
- **2025-10-14, expanded partnership:**
  - Claude becomes a preferred model in Agentforce for regulated industries (financial services, healthcare, cybersecurity, life sciences), via Amazon Bedrock inside Salesforce's VPC. Anthropic is "the first LLM provider fully integrated within the Salesforce trust boundary". Named customers are CrowdStrike and RBC Wealth Management.
  - Slack's MCP server gives Claude access to channels, messages and files, and Claude in Slack can pull from Salesforce CRM and Tableau.
  - Salesforce is deploying Claude Code across its global engineering organization, and Anthropic uses Claude in Slack internally for sales.
  - No commercial terms were disclosed. **[OFFICIAL]** — [Anthropic news](https://www.anthropic.com/news/salesforce-anthropic-expanded-partnership); [Salesforce press release](https://www.salesforce.com/news/press-releases/2025/10/14/anthropic-regulated-industries-partnership-expansion-announcement/)
- **Engadget correction:** On 2026-07-25 Engadget updated its Claude Tag story to clarify "that Anthropic and Slack partnered on the release." Slack says Slackbot is "a deeply integrated but more-traditional AI assistant" while Claude Tag is "a delegatable AI teammate", and that the two complement each other. **[PRESS]** — [Engadget](https://engadget.com/2199619/anthropic-announces-claude-tag-for-slack)
- **Slackbot runs on Claude:** All of Slackbot's 30+ new capabilities (2026-03-31) "run on Anthropic's Claude". Salesforce said Anthropic was the only provider meeting FedRAMP Moderate when Slack began designing the system. Slackbot has ~1M weekly active users. **[PRESS]** — [TNW, 2026-04-06](https://thenextweb.com/news/slack-slackbot-30-ai-features-agentic)
- **Salesforce as investor and customer:**
  - Salesforce invested more than $300M in Anthropic starting with the Series C in early 2023, roughly a 1% stake.
  - Benioff (All-In podcast) said Salesforce expects to spend $300M on Anthropic tokens in 2026, "almost entirely on coding". He also said: "You're going to see some cool stuff with Slack and code I'm not ready to talk about yet."
  - This is a podcast remark that neither company has confirmed. **[PRESS]** — [TNW, 2026-05-16](https://thenextweb.com/news/salesforce-benioff-300-million-anthropic-tokens-slack-coding)

### Inferences
- **The delivery stack splits cleanly.** Anthropic owns the compute, agent runtime, credentials and memory, all on Anthropic's side. Slack supplies identity, UI, distribution and message access via the Marketplace app.
  - Because Claude Tag keeps tool credentials and transcripts off Slack, its "data moat" (channel memory and workspace notes) accrues to Anthropic, not Salesforce.
  - That is strategically valuable, and it is exactly the kind of off-platform storage of Slack data the 2025 terms tighten around.
  - The terms leave room for listed Marketplace apps, which is Claude Tag's status, and for permission-respecting query access (RTS/MCP).
- **Dependence on Salesforce/Slack is high but mutual.**
  - Anthropic depends on Slack for the surface and Marketplace standing.
  - Salesforce depends on Claude for Slackbot, Agentforce in regulated industries, and its own engineers' Claude Code use, and it holds roughly 1% equity.
  - This "co-opetition" lowers but does not eliminate the risk of Slack restricting Claude Tag.
- **Expansion to Microsoft Teams ("coming soon")** looks like an explicit hedge against single-platform dependence.

### Gaps
- No official statement on whether Claude Tag's public-channel search uses Slack's RTS API, the Slack MCP server, or the legacy `search:read` scope. Docs say only "public-channel search".
- No disclosed Slack-specific terms between Anthropic and Salesforce for Claude Tag: data-access tier, rate-limit exemptions or co-marketing.
- The Information ran a headline "Salesforce Employees Worry Over Anthropic's Invasion of Slack", seen only as a paywalled teaser. The date and contents are unverified. — [The Information (paywalled)](https://www.theinformation.com/articles/anthropics-claude-drives-strong-revenue-growth-while-powering-manus-sensation)
- The cost of serving (sandbox minutes, indefinite transcript storage, unbilled monitoring) is not disclosed.

## Q4. 价值获取 Value Capture: seats vs. usage, billing mechanics, Slack/Salesforce economics, expansion loops, revenue context

### Takeaway
Claude Tag uses a hybrid "platform seat plus metered agent" model. An organization must hold a Team or Enterprise plan to switch it on. Channel, thread and group-DM work is then billed by consumption at list (API-style) prices to an organization usage balance under admin spend limits, not to seats. Slack users need no Claude seat to use it in channels. One-to-one DMs run on, and count against, the sender's own seat. Monetizable activity (engaged, proactive and scheduled work) is billed, while "listening" is free. There are launch credits and a DM trial that converts users to seats. No Claude Tag revenue, Slack revenue share, or measured seat-expansion data is public. Company-level revenue grew from a $14B run-rate (Feb 2026, official) to a reported ~$65B (end of July 2026).

### Cited Findings
**Billing mechanics (official)**
- **No per-seat charge:** "Adding Claude to Slack doesn't add a per-seat charge. Channel, thread, and group DM work is billed by usage instead: it draws from a usage balance… an Owner funds."
  - "Slack users don't each need a Claude seat to work with Claude in channels."
  - By default "anyone in the paired Slack workspace can tag `@Claude`". **[OFFICIAL]** — [Docs: Overview](https://claude.com/docs/claude-tag/overview)
- **Billing table:**

  | Work | Bills to | Capped by |
  | :- | :- | :- |
  | Channel work | Organization usage balance | Spend limit plus per-channel limits |
  | Group DMs | Organization usage balance | Spend limit plus default per-group-DM limit |
  | "Reading a channel… deciding whether to reply, and short replies from what Claude already knows" | "Nothing" | Not counted |
  | One-to-one DM from a member with a connected Claude account | "The sender's own seat" | "The seat's usual limits" |

  - "The spend limit counts usage at list price. If your organization has a negotiated discount, that discount applies at invoice time."
  - "There's no published per-task cost guidance." **[OFFICIAL]** — [Docs: Set a spend limit](https://claude.com/docs/claude-tag/admins/set-spend-limit)
- **Plan funding differences:**
  - **Team:** spend limits are "Required, before anything runs". "Claude won't respond in channels until" credits are bought.
  - **Enterprise (invoiced):** "Usage bills to your invoice with no upper bound until you set a spend limit."
  - Limits reset monthly. Heads-up warnings post publicly in the channel at 75% and 90%. **[OFFICIAL]** — [Docs: Set a spend limit](https://claude.com/docs/claude-tag/admins/set-spend-limit)
  - **Conflict:** the Help Center says alerts fire at 75% and 95%. — [Claude Help](https://support.claude.com/en/articles/15594475-what-is-claude-tag)
  - The Help Center also states: "Claude Tag is consumption-based, so spend is based on usage rather than the number of people." **[OFFICIAL]** — [Claude Help](https://support.claude.com/en/articles/15594475-what-is-claude-tag)
- **Spend categories:**
  - **Engaged:** threads where someone tagged or replied to Claude.
  - **Proactive:** work Claude "picked up or started on its own".
  - **Scheduled:** recurring routines.
  - **Monitoring:** "Claude reading the channels it belongs to, which isn't billed."
  - Per-thread fast mode costs more per token. Throughput rate limits are separate from spend limits. **[OFFICIAL]** — [Docs: Set a spend limit](https://claude.com/docs/claude-tag/admins/set-spend-limit); [Docs: Models](https://claude.com/docs/claude-tag/users/models)
- **Showback and chargeback:** Enterprise organizations can pull spend per Slack user from the Enterprise Analytics API, grouping by `claude_tag_user_id` with `products[]=claude-tag`.
  - Requested work is attributed to the asker. Proactive work goes to a person in the thread. Routines go to their creator. Unattributable spend lands in a null row. **[OFFICIAL]** — [Docs: Attribute costs](https://claude.com/docs/claude-tag/admins/attribute-costs)
- **DM seat requirement:** "The seat must include Claude Code, or, on the Enterprise plan, be a Standard or Usage-Based Chat seat held by someone who also has Cowork." **[OFFICIAL]** — [Docs: Overview](https://claude.com/docs/claude-tag/overview)
- **DM trial for members without a Claude account:**
  - These DMs bill to the organization. Each member is capped at the first of "7 days", "$50 of usage" or "50 sessions".
  - After a limit is reached, "their next DM gets a prompt to connect a Claude account, and after connecting, their DMs run on their own Claude account and bill to their own seat." **[OFFICIAL]** — [Docs: Restrict access](https://claude.com/docs/claude-tag/admins/restrict-access)
- **Launch promo credit (one-time, per organization):**
  - "$25000 per Claude Enterprise organization" and "$2500 per Claude Team organization".
  - Covers channel usage only, not DMs, Claude Code, Cowork, chat or API.
  - From 2026-09-02, "organizations must connect Claude Tag to GitHub to receive the credit."
  - "All promotional credits now expire at 11:59 PM Pacific Time on October 1, 2026" (an extension).
  - Not available to Enterprise free trials.
  - Moving from Team to Enterprise raises the cap to $25,000. **[OFFICIAL]** — [Claude Help: Claude Tag launch promo](https://support.claude.com/en/articles/15575654-claude-tag-launch-promo-for-claude-team-and-enterprise)
  - **Conflict:** third-party blogs cite a "10 paid seats" minimum for Team and a 2026-09-01 expiry. The current article shows neither, so these were probably superseded. **[3P]** — [eesel pricing blog](https://www.eesel.ai/blog/claude-tag-pricing)
- **Underlying plan prices on claude.com/pricing (fetched 2026-11-03):**
  - **Team ("teams of 2 to 150"):** Standard seat $20 annual or $25 monthly. Premium seat $100 annual or $125 monthly, "5x more usage than standard". "Up to $500 per month in pooled API credits."
  - **Enterprise:** "Seat price + usage at API rates". Self-serve is "US$20/seat/month, billed annually, 20 seat minimum."
  - **Individual:** Pro $20 monthly; Max $100; Ultra $200.
  - Claude Tag is listed as included in Team and Enterprise only, with no separate price. **[OFFICIAL]** — [Claude pricing](https://claude.com/pricing)

**Slack/Salesforce economics**
- Slack's help page and the Marketplace listing show no Slack-side charge for the Claude app. The Marketplace listing gives no pricing. **[OFFICIAL]** — [Slack Help](https://slack.com/help/articles/53532192117267-Use-Claude-in-Slack); [Slack Marketplace](https://slack.com/marketplace/A08SF47R6P4)
- **Slackbot as a priced-in competitor:** Slackbot is "included in Business+ and Enterprise+ plans at no additional consumption cost". Slack revenue is expected at ~$3B this year, and Agentforce ARR is $800M. **[PRESS]** — [TNW (Slackbot)](https://thenextweb.com/news/slack-slackbot-30-ai-features-agentic); [TNW (Benioff)](https://thenextweb.com/news/salesforce-benioff-300-million-anthropic-tokens-slack-coding)

**Revenue context (company level)**
- **Official, 2026-02-12 (Series G):** run-rate revenue "$14 billion"; Claude Code run-rate "over $2.5 billion", having more than doubled since the start of 2026; "Business subscriptions to Claude Code have quadrupled" since January; raised $30B at a $380B post-money valuation. **[OFFICIAL]** — [Anthropic Series G](https://www.anthropic.com/news/anthropic-raises-30-billion-series-g-funding-380-billion-post-money-valuation)
- **Reported trajectory:** ~$9B (end of 2025), then ~$30B (end of March 2026). **[PRESS]** — [TNW](https://thenextweb.com/news/salesforce-benioff-300-million-anthropic-tokens-slack-coding)
- **Reported, end of July 2026:** ~$65B run-rate, per Bloomberg on 2026-08-17 citing an investor update, up from more than $47B in May. **[PRESS]** — [Axios](https://axios.com/2026/08/17/anthropic-revenue-run-rate-ipo-openai)
  - Caveat: "a run rate is not revenue." — [TNW](https://thenextweb.com/news/anthropic-revenue-run-rate-65-billion)
  - A May 2026 Series H valuation of $965B is reported. **[PRESS]** — [AI Magazine](https://aimagazine.com/news/anthropic-claude-enterprise-ai-agents-and-965bn-valuation)
- **Ramp AI Index (May 2026):** 34.4% of U.S. firms paid for Anthropic vs. 32.3% for OpenAI, the first time Anthropic led in business adoption, "Claude Code was the main driver". Fortune also notes a likely Anthropic IPO this year. **[PRESS citing Ramp]** — [Fortune](https://fortune.com/2026/06/23/anthropic-claude-tag-virtual-employee-tool-slack/)

### Inferences
- **Value capture shifts from "per human" to "per unit of agent work."**
  - Seats become an entry ticket: the Team/Enterprise plan and the Owner role.
  - Consumption scales with how much work the shared agent does, including work it starts itself (Proactive/Scheduled).
  - The "initiative" features double as revenue features. Leaving monitoring unbilled lowers the perceived cost of keeping Claude in many channels, which widens the surface on which billable proactive work can arise.
- **The design has several built-in expansion loops:**
  - Public visibility in channels: colleagues see results.
  - Seatless tagging: anyone in the workspace can tag Claude by default.
  - A 7-day/$50/50-session DM trial that ends with "connect a Claude account".
  - Weekly channel-suggestion DMs, auto-join patterns and "Approve and post", which pull Claude into more channels.
  - Promo credits that required a GitHub connection from 2026-09-02, steering organizations into the code use case and Claude Code-style consumption.
  - These are official mechanisms. Their effect is unmeasured publicly.
- **Two expansion paths:** DMs bill to seats that "must include Claude Code" (or, on Enterprise, a Standard or Usage-Based Chat seat with Cowork), so Claude Tag pulls DM users toward premium or Claude Code-inclusive seats. Channel use expands consumption revenue independently of headcount.
- **Who bears cost risk:**
  - Team plans: prepaid credits, so Anthropic carries none.
  - Enterprise: uncapped unless a limit is set.
  - Anthropic absorbs the unbilled monitoring cost and indefinite beta transcript storage.

### Gaps
- No Claude Tag-specific revenue, ARR, usage share, or average spend per organization has been disclosed.
- No evidence of seat expansion or conversion rates from the DM trial.
- No public per-task or per-thread cost benchmarks. Anthropic explicitly says none are published.
- Unknown whether Team's "up to $500 per month in pooled API credits" can fund Claude Tag usage.
- No evidence of any revenue share, listing fee, or API fee paid to Slack/Salesforce for Claude Tag. Commercial terms are undisclosed.
- The latest revenue figures (Mar–Jul 2026) are press reports of investor updates, not Anthropic disclosures.
- Aggregator sites claim enterprise accounts for more than half of Claude Code revenue. This was not verified against Anthropic's primary wording, so it should not be cited as fact.

## Q5. Competition and risks (Slackbot/Agentforce, OpenAI, coding agents in Slack, Microsoft Teams; platform, governance, cost)

### Takeaway
Claude Tag competes on a crowded Slack surface:
- Salesforce's own Slackbot agent, which is bundled at no extra consumption cost and, notably, runs on Claude.
- OpenAI's ChatGPT app and Codex-in-Slack.
- Coding agents launched from Slack: Cursor, GitHub Copilot, Devin.
- Microsoft's Copilot/Work IQ in the Teams ecosystem.

Its differentiation is the shared, team-visible, persistent and governed agent identity with metered billing. The main risks are dependence on a platform owned by a competitor, governance exposure (broad public-channel search, shared credentials, indefinite beta retention, no BAA or ZDR), and cost unpredictability for customers.

### Cited Findings
**Competitors (dated)**
- **Slackbot (Salesforce):**
  - The rebuilt Slackbot AI agent was reported GA on 2026-01-13 for Business+/Enterprise+. **[PRESS]** — [Pulse/VentureBeat](https://www.pulse.bot/ai/news/salesforce-rolls-out-new-slackbot-ai-agent-as-it-battles-microsoft-and-google-in-workplace-ai-15d6da45-bbdd-46e0-b45c-fdbf6ec545ed)
  - 2026-03-31 added 30+ capabilities: an "agentic operating system", Slackbot as an MCP client to 6,000+ apps, a desktop agent and a native CRM. All run on Claude, with ~1M weekly active users.
  - Included in Business+/Enterprise+ "at no additional consumption cost". A limited version reaches Free and Pro from April 2026. **[PRESS]** — [TNW](https://thenextweb.com/news/slack-slackbot-30-ai-features-agentic)
  - Fortune names Slackbot and the startup Viktor as competitors. Fortune frames Claude Tag as public, team-visible and channel-based, versus Slackbot as a private individual assistant. **[PRESS]** — [Fortune](https://fortune.com/2026/06/23/anthropic-claude-tag-virtual-employee-tool-slack/)
- **OpenAI:**
  - Codex GA (2025-10-06) included a Slack integration for delegating tasks from channels and threads, for Plus, Pro, Business, Edu and Enterprise, with admin features on Business, Edu and Enterprise. **[OFFICIAL]** — [OpenAI: Codex GA](https://openai.com/index/codex-now-generally-available); [Codex Slack docs](https://developers.openai.com/codex/integrations/slack)
  - A ChatGPT app for Slack (beta, built by OpenAI on the Slack platform) offers summaries, research and writing. **[OFFICIAL]** — [Salesforce newsroom](https://www.salesforce.com/news/stories/chatgpt-app-for-slack/)
  - Denise Dresser, Slack's CEO, left in December 2025 to become OpenAI's first CRO. Rob Seaman became interim CEO. **[PRESS]** — [TNW](https://thenextweb.com/news/slack-slackbot-30-ai-features-agentic)
- **Cursor:** "launch Background Agents directly from Slack by mentioning @Cursor" (changelog 1.1, ~June 2025). Agents read the thread and post a PR link. **[OFFICIAL]** — [Cursor changelog 1.1](https://cursor.com/changelog/1-1)
- **GitHub Copilot coding agent:**
  - In Microsoft Teams: public preview, 2025-09-19.
  - In Slack: 2025-10-28, PRs from Slack threads; needs an admin policy on Business or Enterprise.
  - 2026-09-25 update for Slack and Teams context (files, message links, channel history). **[OFFICIAL]** — [GitHub changelog, Slack](https://github.blog/changelog/2025-10-28-work-with-copilot-coding-agent-in-slack/); [GitHub changelog, Teams](https://github.blog/changelog/2025-09-19-work-with-copilot-coding-agent-in-microsoft-teams/); [GitHub changelog 2026-09-25](https://github.blog/changelog/2026-09-25-updates-to-github-copilot-for-slack-and-microsoft-teams)
- **Devin:** described as an autonomous agent that "works through Slack". **[3P, unverified against Cognition docs]** — [morphllm comparison](https://www.morphllm.com/comparisons/devin-vs-cursor)
- **Knowledge-layer competitors:** TechCrunch frames the Claude Tag fight as one over organizational context:
  - Microsoft Graph via Copilot and Work IQ.
  - Snowflake and Databricks as back-end knowledge sources.
  - Glean as an intelligence layer.

  **[PRESS]** — [TechCrunch](https://techcrunch.com/2026/06/23/anthropics-claude-tag-is-learning-your-company-one-slack-message-at-a-time/)
- **Microsoft Teams:** Anthropic lists Claude Tag for Teams as "Coming soon" with a waitlist. **[OFFICIAL]** — [claude.com/tag](https://claude.com/tag)

**Risks documented in official sources**
- **Governance and data exposure:**
  - Claude can keyword-search every public channel, and "No admin setting turns this search off". It can only be limited.
  - Workspace notes from public channels are shared across all channels.
  - Connector credentials are usable by everyone in the scoped channels.
  - Deleted Slack messages persist in transcripts. There is no automatic retention period during the beta.
  - Not covered by the BAA. Unavailable with ZDR, CMEK or the HIPAA configuration.
  - Guest and Slack Connect channels are restricted by default. **[OFFICIAL]** — [Docs: Healthcare](https://claude.com/docs/claude-tag/admins/healthcare); [Docs: Security](https://claude.com/docs/claude-tag/concepts/security-and-data); [Docs: Data lifecycle](https://claude.com/docs/claude-tag/concepts/data-lifecycle); [Docs: Restrict access](https://claude.com/docs/claude-tag/admins/restrict-access)
- **Prompt injection:** For the earlier Slack integration Anthropic warns "Claude may follow directions from other messages in the context, so users should make sure to only use Claude in trusted Slack conversations." **[OFFICIAL]** — [Claude Code docs: Slack](https://code.claude.com/docs/en/slack)
- **Platform terms:** Slack's 2025 terms make the Marketplace "the only appropriate channel" for commercial distribution and tighten data use. Unlisted apps are throttled to 1 request per minute and 15 messages. **[OFFICIAL]** — [Slack changelog](https://docs.slack.dev/changelog/2025/05/29/rate-limit-changes-for-non-marketplace-apps)
- **Cost unpredictability for customers:**
  - "Usage bills to your invoice with no upper bound until you set a spend limit" (Enterprise).
  - "There's no published per-task cost guidance." **[OFFICIAL]** — [Docs: Set a spend limit](https://claude.com/docs/claude-tag/admins/set-spend-limit)
  - Critics say usage billing makes costs hard to forecast. Some builders counter that sessions stay short. **[3P, competitor-authored]** — [eesel review](https://www.eesel.ai/blog/claude-tag-review)

**Risks raised in press**
- TechCrunch (Dec 2025) raised code-security concerns and dependence on both Slack's and Claude's API uptime for Claude Code in Slack. **[PRESS]** — [TechCrunch](https://techcrunch.com/2025/12/08/claude-code-is-coming-to-slack-and-thats-a-bigger-deal-than-it-sounds)
- Fortune notes Anthropic could later enter Slackbot's private-assistant space, implying escalating overlap. **[PRESS]** — [Fortune](https://fortune.com/2026/06/23/anthropic-claude-tag-virtual-employee-tool-slack/)
- Benioff called for an "intermediary layer" that routes complex tasks to Claude and simpler ones to cheaper models. TNW reads this as a signal that Salesforce may build model routing itself. **[PRESS]** — [TNW](https://thenextweb.com/news/salesforce-benioff-300-million-anthropic-tokens-slack-coding)

### Inferences
- **Platform-owner risk is real but offset by mutual dependence.** Salesforce owns the surface and bundles a free competing agent (Slackbot). That agent, though, is powered by Claude, and Salesforce is an Anthropic investor and large token customer. Slack publicly frames the two as complementary (Engadget's 2026-07-25 edit).
  - The most likely friction points are data-access terms (search and RTS scopes, storage of Slack-derived memory off-platform) and default placement. Outright exclusion looks less likely.
- **The differentiation that a bundled Slackbot can't easily match:**
  - An org-provisioned agent identity with service accounts and federated credentials.
  - A full code-execution sandbox: the "Claude Code engine" with GitHub PR authorship.
  - Per-channel memory and routines.
  - Admin-grade spend and governance.

  The weakness is price perception: metered spend versus Slackbot's "no additional consumption cost".
- **Coding agents in Slack** (Cursor, Copilot, Codex, Devin) are close substitutes for the engineering wedge. Claude Tag's broader cross-functional scope (data, GTM, on-call) and shared memory are its hedge against commoditization of "PR from a Slack thread."

### Gaps
- No head-to-head adoption or market-share data for AI agents inside Slack.
- No Chinese-language coverage gathered. Japanese coverage exists but was not analyzed, e.g. [gori.me](https://gori.me/?p=167435).
- No verified HN or Reddit threads. The HN quote circulating ("API-style billing inside Slack") is second-hand and unverified.
- No disclosed incident, security or misuse reports specific to Claude Tag.
- Unknown whether Slack has granted Claude Tag any special API tier or rate-limit treatment beyond standard Marketplace status.
