# 谁为外卖 Agent 买单：DoorDash 与美团六个场景的三要素检验

> 调研日期：2026-11-04，资料截至 2026-11-03。美元写作 $，人民币写作 ¥；大额写作"亿美元""亿元"。只在单位经济测算中按 $1 ≈ ¥7.1 折算（假设）。
> 对象：DoorDash（含 Wolt、Deliveroo；收购 Instacart 待批）与美团核心本地商业（外卖、闪购、到店酒旅）。最新财报：DoorDash 为 Q3 2026（10-29 发布）；美团为 Q2 2026（08-28 发布），Q3 尚未发布。
> 场景：S1 平台自有 App 里的 C 端点单/决策 Agent；S2 被第三方通用 Agent 调用（ChatGPT、Gemini、Alexa+、千问、豆包、元宝/WorkBuddy）；S3 商户经营 Agent；S4 履约/骑手侧与无人配送；S5 平台内部提效；S6 对外输出 AI 能力。
> 数字口径：标"计算"的，是用公司披露的数字直接算出的比率。标"估算"的，是本研究按公开价目表和明示假设做的测算，不是公司或第三方披露。券商数字标"券商估算"。两家都没有披露任何 Agent 产品的订单、收入、推理成本或毛利。
> 一处更正：微信支付"AI 专属卡"首个接入的 Agent 是腾讯 WorkBuddy。美团是 WorkBuddy 里的首个服务伙伴，卖的是到店团购券，不是外卖（第 6.2 节）。
> 缩写：GOV 为订单总额；CP 为 DoorDash 披露的 Contribution Profit；CLC 为美团核心本地商业分部；UE 为每单经营利润。

## 结论

在 DoorDash 和美团，Agent 的价值主张、价值创造与传递在六个场景里大多成立；难在价值获取，而卡住价值获取的不是 token 成本，是广告和通道费。两家每单都只赚约 1 美元或 1 元：DoorDash Q3 2026 每单 Adj. EBITDA 约 $1.07（计算）；美团外卖每单经营利润，券商对 2026 年的估算在 −¥0.3 到 ¥1.5 之间。用小模型跑"发现+下单"会话，每成交一单的推理费约 $0.02（估算）；但若 Agent 订单丢掉一半广告，每单少收约 $0.34（估算，假设广告占 GOV 2%）。广告恰恰是利润本身：美团 2024 年的广告收入相当于本地商业分部经营利润的 93%（计算）。所以在 Agent 不单独收费、账在原有抽佣里结的模式下，价值获取要同时满足五条：订单是真增量，不挤掉广告，推理路由到小模型，错误率远低于 1%，账户、结账和履约留在平台。按这五条，S3 商户 Agent 靠广告预算已经成立，S1 机制成立但账未证，S4 和 S5 是成本账、杠杆大但未兑现，S2 只能当获客渠道，S6 卖 token 不成立。

| 场景 | ① 价值主张 | ② 创造与传递 | ③ 价值获取 | DoorDash 与美团的差别 |
|---|---|---|---|---|
| S1 自有 App 点单/决策 | 成立 | 成立 | 未证：取决于增量和广告挤占 | DoorDash 有篮子和转化数据；美团只披露曝光和回答次数 |
| S2 被第三方 Agent 调用 | 弱：外卖用 Agent 点不比 App 快 | 成立：履约绕不过平台 | 作收费站不成立；作获客渠道有条件成立 | DoorDash 面对中立 Agent，累计"几百单"；美团面对生态 Agent，与腾讯结盟、拒绝豆包 |
| S3 商户经营 | 成立 | 成立 | DoorDash 成立（广告）；美团间接 | DoorDash 的 Agent 直接花广告预算；美团 130 万+ 商户免费用 |
| S4 履约与无人配送 | 成立 | 部分 | 未证：单位杠杆最大 | DoorDash 每单省 $0.10 ≈ 每季 9,900 万美元；美团骑手保障成本在涨 |
| S5 内部提效 | 成立 | 成立 | 未证：没有披露节省 | 两家研发费用都比业务长得快 |
| S6 对外输出 | 弱 | 部分 | 卖 token 不成立；卖履约和软件捆绑成立 | DoorDash 卖 Drive、媒体和仓配；美团低价卖 LongCat，"不做 token 工厂" |

- **广告是最大变量，推理是小头。** 小模型下，Agent 订单里约 3% 是真增量就能回本；若丢一半广告，门槛升到约 22%（估算，第 5.3 节）。DoorDash 的 10-Q 写明，消费者费用率的下降由广告补回 ([DoorDash 10-Q](https://www.sec.gov/Archives/edgar/data/1792789/000179278926000070/dash-20260930.htm))。
- **第三方 Agent 的通道费，比每单利润还高。** 据报道，OpenAI 曾向 Shopify 商户收每单 4%，2026-03 撤回 ([Shopifreaks](https://www.shopifreaks.com/shopify-confirms-a-4-fee-on-sales-made-through-chatgpt-but-whats-the-real-cost/); [Agentic Commerce Feed](https://www.agenticcommercefeed.com/blog/2026-03-16-openai-kills-instant-checkout))。按 4% 算，DoorDash 一笔本来就会发生的订单从赚 $1.07 变成亏 $0.30；美团外卖的盈亏平衡费率只有约 2%–3%（均为估算）。豆包对导向抖音的酒店订单收约 12% ([界面](https://m.jiemian.com/article/14902227.html))。两家都只让外部 Agent 做"发现"，账户、结账、会员和履约留在自己手里。DoorDash 至今只从第三方 Agent 收到"几百单" ([TechCrunch](https://techcrunch.com/2026/10/30/doordash-said-its-only-received-several-hundred-orders-from-ai-agents/))。
- **中美的分野在入口归谁。** 美国的通用 Agent 是中立的：Gemini 能同样操作 DoorDash、Uber Eats 和 Grubhub ([TechCrunch](https://techcrunch.com/2026/02/25/gemini-can-now-automate-some-multi-step-tasks-on-android))。DoorDash 的对策是自己做场外 Agent（Text DoorDash、企业 MCP），把战场定在"atoms"。中国的入口绑定生态：最大的豆包导向抖音，第二的千问导向淘宝闪购。美团与没有履约网络的腾讯结盟，并拒绝豆包操作手机 ([京报网](https://news.bjd.com.cn/2026/10/15/11996628.shtml))。美团的第一个支付型 Agent 渠道开在到店团购券上，没开在外卖上：团购不用骑手，利润更厚。
- **商户 Agent 已经在赚钱，赚的是广告。** DoorDash 的 Smart Campaigns 替餐厅买"始终 ROI 为正"的广告，Xu 称之为去年上线的"最大的 Agent 之一" ([DoorDash Q4 2025 电话会](https://www.fool.com/earnings/call-transcripts/2026/02/18/doordash-dash-q4-2025-earnings-call-transcript/))。美国单店 Agent 的价格锚是每月 $399–499；中国是零，美团的 AI店长、袋鼠参谋都免费 ([Katalyst](https://www.katalystos.com/blog/ai-phone-answering-for-restaurants-what-it-costs); [雷锋网](https://www.leiphone.com/category/industrynews/tZI7pYBhiFduVfr6.html))。
- **履约的单位杠杆最大，兑现最慢。** DoorDash 每单省 $0.10，约等于每季多 9,900 万美元收入（计算）。但 Dot 的目标只是到年底在最大的测试市场送"高个位数百分比"的订单 ([DoorDash Q2 2026](https://www.sec.gov/Archives/edgar/data/1792789/000179278926000048/proddashex991-pressrelease.htm))；美团无人机累计 100 万单，不到一天外卖量的 2% ([IT之家](https://www.ithome.com/0/980/150.htm))。
- **对垂直平台，九个条件里最难的从 C7、C8 换成了 C9 和 C5。** 消费者不为 Agent 付钱，单次推理很便宜，计价单位不再是主要矛盾。主要矛盾是利润会不会被 Agent 自己挤掉（广告），或被入口主人拿走（通道费）。

## 1. 框架：九个条件不变，C2 和 C9 要多问一句

本文的三要素是价值主张、价值创造与传递、价值获取，取自"价值三角形"框架 ([上海财经大学期刊](https://qks.shufe.edu.cn/J/ArticleQuery/A160824002159/CN))，与 Teece 的 value proposition、creation and delivery、capture 一一对应 ([Teece 2010](https://www.kth.se/social/upload/528c4881f2765458b97a9368/Teece%20%282010%29.pdf))。"成立"拆成九个可检验的条件 C1–C9。

外卖平台和通用 Agent 有一处根本不同：平台的 Agent 一般不单独收费。它改变的是平台原有的损益，包括订单量、客单价、广告位、配送成本和客服成本。所以九个条件不变，但每一条都要换成平台的问题：

| 要素 | 条件 | 通用检验 | 放到外卖平台上要问 |
|---|---|---|---|
| ① 价值主张 | C1 能验收 | 产出是能验收的工作，还是一段答案？ | 产出是送达的订单、订到的位、算得出 ROI 的广告，还是一条推荐？ |
| ① | C2 有人买单 | 付钱的人有与工作量挂钩的预算吗？ | 消费者不为 Agent 付钱，付钱的是原有抽佣池。Agent 带来的是增量，还是给老订单换了个入口？ |
| ① | C3 可托付 | 出错谁负责？能否审计、限权、回滚？ | 下错单谁赔？付款谁确认？改价、投广告要不要人批？ |
| ② 价值创造与传递 | C4 上下文与身份 | Agent 读得到上下文吗？以谁的身份行动？ | 账户、订单历史、实时菜单、库存和运力在谁手里？ |
| ② | C5 渠道靠得住 | 渠道主人会不会限流或掐断？ | 用户在哪个入口说话？入口主人是盟友、中立方还是对手？ |
| ② | C6 单任务成本可控 | 模型和算力从哪来？谁定价？ | 每单推理费占每单利润多少？能否路由到小模型？ |
| ③ 价值获取 | C7 计价跟着成本走 | 收费单位随成本同向变化吗？ | 收入按订单，成本按会话，没成交的会话谁买单？通道费按 GOV 收，与成本无关 |
| ③ | C8 看得见、封得住 | 付费方能否看到花费、设上限？ | 消费者能否限额、逐笔确认？商户能否设 ROI 下限、审批改价？ |
| ③ | C9 毛利守得住 | 会不会被渠道方或模型方拿走？ | 会不会被入口主人收通道费，或被 Agent 自己挤掉广告？ |

C2 和 C9 是本文多问的两句。C2：付钱的不是消费者，而是原有的抽佣池（佣金、广告、配送费、会员费），所以要问增量。C9：利润可能被两方拿走，一是入口主人的通道费，二是 Agent 自己挤掉的广告。

五个框架各管一个问题：

| 框架 | 回答什么 | 本文用的指标 |
|---|---|---|
| 平台/市场经济学 + 单位经济 | Agent 让每单利润变多还是变少？谁在补贴谁？ | 每单贡献利润、广告率、增量订单占比、每会话推理费、通道费 |
| 聚合理论（Thompson 的三级聚合者；Winters 的三阶段） | 通用 Agent 会不会把平台降成"履约 API"？ | 第三方 Agent 订单占比；Agent 能在几张同类履约网之间切换 |
| 7 Powers（Helmer） | 平台靠什么守住价值？ | 每单配送成本、独有供给、ETA 准确率、售后 |
| JTBD + 价值主张画布 | 哪些活值得交给 Agent？ | 任务频率、委托意愿、确认和改单率 |
| 五力 + 价值链 | 利润池往哪一层移？ | 发现、决策、交易、履约、售后各归谁 |

来源：([Stratechery](https://stratechery.com/2017/defining-aggregators/); [Casey Accidental](https://www.caseyaccidental.com/p/when-agents-attack-how-ai-collapses); [7 Powers](https://7powers.com/); [HBR：JTBD](https://hbr.org/2016/09/know-your-customers-jobs-to-be-done); [Strategyzer](https://www.strategyzer.com/library/the-value-proposition-canvas); [HBR：五力](https://hbr.org/2008/01/the-five-competitive-forces-that-shape-strategy))。

单位经济用一张增量损益表来算。公式按研究笔记整理：

```
Agent 带来的增量利润 =
    增量订单 × 每单贡献利润
  + Agent 订单的客单价提升 × 有效抽佣率
  + 留存提升 × 用户终身价值
  + Agent 原生广告（如标注的赞助推荐）
  + 客服节省
  − 推理成本（按会话计，不按订单计）
  − 被挤掉的广告（Agent 订单 × 客单价 × 广告率 × 挤占比例 k）
  − 错误成本（错误率 × 每次的退款、重送、客服）
  − 通道费（第三方 Agent 订单 × 客单价 × 费率 f）
  − 补贴
```

两条口径。第一，真增量的订单按每单贡献利润算；只是换了入口的订单按 EBITDA 级利润算。第二，增量必须用对照组（holdout）测。Amazon 称 Rufus 带来近 120 亿美元"增量年化销售"，那是模型归因；Walmart 称 Sparky 用户每单多花约 35%，那是用户与非用户的横向比较 ([ppc.land](https://ppc.land/amazons-ai-shopping-assistant-drove-12-billion-in-sales-for-2025/); [ppc.land/WIRED](https://ppc.land/walmarts-chatgpt-checkout-flopped-heres-what-comes-next/))。两者都不能直接代入。

## 2. 底座：每单只赚约 1 美元或 1 元，广告约等于全部利润

### 2.1 DoorDash：每单 Adj. EBITDA $1.07，广告托住抽佣率

| Q3 2026 | 合计 | 每单（计算） |
|---|---|---|
| 总订单 | 9.93 亿单（同比 +28%；剔除 Deliveroo +17%） | — |
| Marketplace GOV | 339 亿美元（+36%） | $34.2 |
| 收入 | 46.2 亿美元；净收入率 13.6% | $4.65 |
| Contribution Profit | 17.8 亿美元；占 GOV 5.2% | $1.79 |
| Adj. EBITDA | 10.6 亿美元；占 GOV 3.1% | $1.07 |
| GAAP 净利润 | 4.66 亿美元 | $0.47 |

来源：([DoorDash Q3 2026](https://www.sec.gov/Archives/edgar/data/1792789/000179278926000062/proddashex991-pressrelease.htm))。总订单含 Drive 订单，GOV 不含，所以每单 GOV 略低估 marketplace 客单价。

DoorDash 的收入是商户佣金加消费者费用，减去 Dasher 支出和退款、补偿、促销 ([DoorDash 10-Q](https://www.sec.gov/Archives/edgar/data/1792789/000179278926000070/dash-20260930.htm))。Dasher 支出在收入里扣除，所以履约每省一分钱，都直接进净收入率。消费者费用率在降：DashPass 让美国餐饮每单交易费两年降了 9% ([DoorDash Q3 2026](https://www.sec.gov/Archives/edgar/data/1792789/000179278926000062/proddashex991-pressrelease.htm))，10-Q 说下降部分由广告和更少的促销补回。广告在托住抽佣率，却不单独披露。最近的数字是 2024 年 run-rate 超过 10 亿美元 ([Bloomberg](https://www.bloomberg.com/news/articles/2025-06-11/doordash-to-buy-symbiosys-to-boost-its-1-billion-a-year-ad-unit))，2026-06 广告主超过 40 万家 ([DoorDash Ads](https://ir.doordash.com/news/news-details/2026/DoorDash-Ads-Becomes-a-Global-Commerce-Media-Platform/default.aspx))。10-29 当天，DoorDash 还宣布以约 170 亿美元收购 Instacart，预计 2027 年底或 2028 上半年交割 ([交易公告](https://www.sec.gov/Archives/edgar/data/1792789/000114036126041111/ef20084547_ex99-1.htm))。

### 2.2 美团：外卖每单利润在 ¥0 上下，广告约等于本地商业的全部利润

| 期间 | CLC 收入（亿元） | CLC 经营利润（亿元） | 利润率 |
|---|---|---|---|
| 2024 | 2,502.5 | +524.2 | 约 21% |
| 2025 Q3 | 674 | −141 | −20.9% |
| 2025 | 2,608.3 | −69.0 | 约 −2.6% |
| 2026 Q1 | 640.6 | −20.3 | −3.2% |
| 2026 Q2 | 715.3 | +56.7 | 7.9% |

来源：([美团 2025 年报](https://www1.hkexnews.hk/listedco/listconews/sehk/2026/0326/2026032600964.pdf); [美团 Q2 2026](https://www1.hkexnews.hk/listedco/listconews/sehk/2026/0828/2026082800435.pdf))；2025 Q3 为媒体转述的官方数字 ([腾讯新闻](https://news.qq.com/rain/a/20251128A05VJL00))。

2025 年 CLC 收入里，佣金 992 亿元（38.0%），配送服务 961 亿元（36.8%），在线营销即广告 515 亿元（19.7%），其他 141 亿元。商户付的佣金加广告约占 58%（计算）([美团 2025 年报](https://www1.hkexnews.hk/listedco/listconews/sehk/2026/0326/2026032600964.pdf))。**广告与利润同量级：2024 年广告收入 488 亿元，CLC 经营利润 524 亿元，前者是后者的 93%（计算）。** 2026 年起，美团把佣金和广告合并为"商家服务"披露，两者不再能分开追踪 ([美团 Q2 2026](https://www1.hkexnews.hk/listedco/listconews/sehk/2026/0828/2026082800435.pdf))。

外卖每单利润没有官方数字，只有券商估算：

| 来源 | 发布 | 外卖每单经营利润 |
|---|---|---|
| Bernstein（战前预测） | 2025-06 | 2025 年 ¥1.25，2026 年 ¥1.50 |
| J.P. Morgan（引专家） | 2025-11 | 2025-09 约 −¥1.8；2026 下半年 ¥0.4–0.5 |
| UBS | 2026-06-02 | Q1 约 −¥0.9；2026 全年约 −¥0.3 |
| 管理层 | 2026-08-28 | Q2 转正；Q3 环比走弱但仍为正 |

来源：([Investing.com 转述 Bernstein](https://ng.investing.com/news/stock-market-news/bernstein-downgrades-meituan-to-market-perform-amid-margin-expansion-risks-1970107); [老虎证券转述 J.P. Morgan](https://www.itiger.com/news/1173649509); [华尔街见闻](https://wallstreetcn.com/articles/3773639); [美团 Q2 2026 电话会](https://www.investing.com/news/transcripts/earnings-call-transcript-meituan-posts-q2-2026-profit-as-revenue-growth-accelerates-93CH-4881117))。

外卖大战的账很重。据汇丰，2025 Q2 至 2026 Q1，淘宝闪购约亏 870 亿元，美团约 440 亿元，京东约 420 亿元；2026 Q1 日单份额约为 50.3% / 39.4% / 10.3%。汇丰还指出，外卖行业经营利润率最高不超过约 3.5% ([搜狐转述汇丰](https://m.sohu.com/a/1007728090_613239/))。美团 2025 年销售及营销费用 1,029 亿元，同比 +60.9% ([美团 2025 年报](https://www1.hkexnews.hk/listedco/listconews/sehk/2026/0326/2026032600964.pdf))。

### 2.3 同行：利润和广告都只占 GOV 的 2%–4%

| 平台（期间） | 利润占 GOV | 广告占 GOV |
|---|---|---|
| Instacart（Q3 2026） | Adj. EBITDA 3.2% | 3.1% |
| Uber Delivery（Q2 2026） | 分部经营利润 3.8%（计算） | 超过 2%（管理层目标口径，二手来源） |
| Grab Deliveries（Q3 2026） | Adj. EBITDA 2.7% | 未披露 |
| Swiggy 外卖（Q2 FY27） | Adj. EBITDA 3.4% | 未披露 |
| DoorDash（Q3 2026） | Adj. EBITDA 3.1% | 未披露 |

来源：([Instacart Q3 2026](https://www.sec.gov/Archives/edgar/data/0001579091/000119312526437014/d24470dex991.htm); [Uber Q2 2026](https://www.sec.gov/Archives/edgar/data/0001543151/000154315126000027/uberq226earningspressrelea.htm); [rallies.ai](https://rallies.ai/news/uber-exits-2025-with-327-margin-must-prove-ad-and-eats-economics-in-2026); [Grab Q3 2026](https://www.sec.gov/Archives/edgar/data/0001855612/000185561226000148/a2026q3-earningspressrelea.htm); [Swiggy](https://www.swiggy.com/corporate/wp-content/uploads/2026/10/Press-release_Q2FY2027.pdf))。

**这张表决定了后面每个场景的账。** 利润约为 GOV 的 3%–4%，广告也有 2%–3%。Agent 每单的推理费以美分计；它对广告和通道费的影响以几十美分计。

## 3. 框架诊断：Agent 能拿走"选"，拿不走"送"

### 3.1 聚合理论：外卖平台是有物理成本的二级聚合者

Thompson 给聚合者定了三个特征：直接拥有用户关系，服务用户的边际成本为零，需求驱动的多边网络。按供给成本分三级：一级要购买供给（Netflix），二级不拥有供给但承担供给交易成本（Uber），三级供给成本为零（Google）([Stratechery](https://stratechery.com/2017/defining-aggregators/))。

外卖平台是二级聚合者，还多了一条昂贵的物理边：骑手。每单都有真实的边际成本。ChatGPT、Gemini、千问、元宝这类通用 Agent 更接近三级的需求聚合者。它们能低成本拿走需求界面，即发现和选择，拿不走履约。Winters 把这个过程分三步：发现型 Agent 让平台仍要买流量，但出价更低；交易型 Agent "也执行购买时，平台就整个消失了"；最后供给方直接接入 Agent ([Casey Accidental](https://www.caseyaccidental.com/p/when-agents-attack-how-ai-collapses))。

外卖卡在第二步：交易可以由 Agent 发起，履约绕不过平台。Xu 的说法是，点餐"不是难的部分……难的是确保他们真的到了" ([DoorDash Q3 2026 电话会](https://www.fool.com/earnings/call-transcripts/2026/10/29/doordash-dash-q3-2026-earnings-call-transcript/))。DoorDash 在 Q3 财报里把战略写成"winning the battle for atoms" ([DoorDash Q3 2026](https://www.sec.gov/Archives/edgar/data/1792789/000179278926000062/proddashex991-pressrelease.htm))。王兴说 Agent 对他的冲击比 ChatGPT 更大；他也说，"就算爱因斯坦当秘书"，让他订餐厅也不知道有没有座位，"这不是智力问题，而是信息问题" ([凤凰科技](https://tech.ifeng.com/c/8rShVTkbSYO))。

所以最坏情况不是平台消失，而是平台降成"履约 API"。一篇评论说，真到那一步，美团"更应按物流公司估值" ([36氪](https://eu.36kr.com/en/p/3916086864090756))。议价力取决于两件事。第一，Agent 能在几张同类履约网之间切换。美国在集中：DoorDash 收购 Instacart，Uber 要约收购 Delivery Hero，后者预计 2027 下半年交割 ([ESM](https://www.esmmagazine.com/technology/delivery-hero-hikes-2026-forecasts-on-stronger-demand-profitability-320173))。中国是三家，汇丰预计 2026 Q4 份额稳定在约 50 / 40 / 10 ([搜狐转述汇丰](https://m.sohu.com/a/1007728090_613239/))。第二，商户和骑手是否多平台经营。Citrini Research 的悲观情景是：AI 应用按约 7% 抽成直连餐厅，再把配送拍卖给骑手，对比 DoorDash 约 25% 的综合抽成（含配送费）；报告发布当天 DoorDash 股价跌了约 7% ([Stocktwits](https://stocktwits.com/news-articles/markets/equity/doordash-moat-under-threat-citrini-research-warns-consumer-ai-agents-bypass-delivery-apps/cZRvn0mR4zK))。这是假设情景，不是预测；7% 和 25% 也没有与原文核对。

平台经济学给出同一个结论的另一面。三边市场里，平台向商户收佣金和广告，补贴消费者（会员减免、外卖大战里的红包），付钱给骑手。广告卖的是消费者在 App 里的注意力。Agent 把注意力压缩成 2–3 个候选，商户为注意力付的钱就会变少，平台要么提佣金，要么少补贴。这是本研究的推断。它解释了为什么平台最在乎的不是 Agent 拿走订单，而是拿走"浏览"。

### 3.2 7 Powers：守得住的都在"送"这一侧

| Power | 对第三方 Agent 的强弱 | DoorDash | 美团 |
|---|---|---|---|
| 规模经济（配送密度、拼单） | 强；但 Agent 会把价格和时效差异摆上台面 | 每天约 1,080 万单（计算）；2025 年 900 万+ Dasher | 外卖日单份额约 50% |
| 网络经济（本地流动性） | 中：供给侧在；需求被 Agent 聚合后会削弱 | 5,600 万+ 月活，3,500 万+ 会员（2025 年底） | 交易用户超 8 亿（截至 2025 Q3 的 12 个月） |
| 反定位（Counter-positioning） | 对在位者不利：广告生意让它的 Agent 很难"客观" | Ask 的 Suggested Swaps 推更便宜的替代品，可能压低赞助商品收入（推断） | 对话界面只给 2–3 个候选，广告位变少 |
| 转换成本 | 消费者弱，商户和骑手强 | 商户绑在 Drive、Storefront、SevenRooms | 商户工具嵌在外卖和到店后台 |
| 品牌 | 在 Agent 流程里弱，在会员和保障里强 | DashPass | 美团会员；必吃榜 |
| 独有资源（Cornered resource） | 最强 | 和 Dasher 一起给"网上不存在的物理信息"建目录 | 13 亿+ 条评价、7 亿+ 次商户信息核验 |
| 流程能力（Process power） | 强 | DashMart 履约准确率 99%+ | 无人机累计 100 万+ 单；170 万+ 骑手用智能头盔 |

来源：Helmer 的定义见 ([7 Powers](https://7powers.com/))；事实见 ([DoorDash Q3 2026](https://www.sec.gov/Archives/edgar/data/1792789/000179278926000062/proddashex991-pressrelease.htm); [DoorDash Q4 2025](https://www.sec.gov/Archives/edgar/data/1792789/000179278926000012/prodq4dashex991-pressrelea.htm); [Dash Forward 2026](https://about.doordash.com/en-us/news/dash-forward-2026); [DoorDash Q4 2025 电话会](https://www.fool.com/earnings/call-transcripts/2026/02/18/doordash-dash-q4-2025-earnings-call-transcript/); [搜狐转述汇丰](https://m.sohu.com/a/1007728090_613239/); [腾讯新闻](https://news.qq.com/rain/a/20251128A05VJL00); [界面](https://m.jiemian.com/article/14842082.html); [新华网](https://www.news.cn/tech/20260909/79161147f2414bc79664a28d32802f8e/c.html))。没找到公开发表的、把 7 Powers 用到外卖平台 Agent 上的分析，此表是本研究的映射。

读法：Agent 削弱的是消费者一侧的转换成本和品牌。Citrini 的说法是"An agent doesn't have a home screen"。平台守住的是独有资源、流程能力和规模经济，都在"送"这一侧。反定位是在位者的软肋：靠广告赚钱的平台，做出的 Agent 天然想推付费商家。

### 3.3 价值链与五力：最脆弱的是发现层的广告

| 环节 | S1 自有 App Agent | S2 许可接入（ChatGPT app、MCP、Alexa+、元宝 A2A） | S2 GUI Agent（Gemini 自动化、豆包手机） | 对手生态的 Agent（千问→淘宝闪购） |
|---|---|---|---|---|
| 发现、决策 | 平台 | 通用 Agent | 通用 Agent | 对手 |
| 账户、支付 | 平台 | 平台（账户绑定；结账回平台，或走平台自己的 Agent） | 平台界面，由 Agent 代操作 | 对手 |
| 履约、售后 | 平台 | 平台 | 平台 | 对手 |
| 广告曝光 | 平台，但会话变短 | 基本丢失 | 给机器看，不给人看（推断） | 丢失 |

| 五力 | Agent 带来的变化 | DoorDash | 美团 |
|---|---|---|---|
| 买方议价（新增：通用 Agent 平台） | 入口方可以按单收费或要分成 | OpenAI 4% 先例已撤回；目前没有收费 | 豆包对酒店收约 12%；中金认为微信 Agent 入口可抬高有效佣金 |
| 供应方议价（商户、骑手） | Agent 让多平台接单更容易 | 佣金 15% / 25% / 30%；Square 免佣接 ChatGPT、Claude 订单 | 名义佣金 6%–8%；《财经》两个算例里，算上配送费和商户出资的优惠，综合扣点为 19.7% 和 30.8% |
| 替代品 | 商户直连 Agent，配送竞价 | Square 的免佣下单；Citrini 的 7% 情景 | 千问直连淘宝闪购（是对手，不是替代品） |
| 新进入者 | 前端门槛降低，履约门槛仍高 | — | — |
| 同业竞争 | 从"抢 App 时长"变成"做 Agent 的默认履约方" | Uber Eats、Grubhub 同在 Gemini；Alexa+ 只接了这两家 | 淘宝闪购约 40%，京东约 10% |

来源：([Shopifreaks](https://www.shopifreaks.com/shopify-confirms-a-4-fee-on-sales-made-through-chatgpt-but-whats-the-real-cost/); [界面：豆包](https://m.jiemian.com/article/14902227.html); [界面：中金](https://m.jiemian.com/article/14842082.html); [DoorDash 商户价目](https://merchants.doordash.com/en-us/products/pricing); [VentureBeat](https://venturebeat.com/marketing-tech/restaurants-can-now-accept-orders-placed-directly-from-chatgpt-and-claude-thanks-to-squares-new-low-fee-no-setup-integration/); [美团](https://www.meituan.com/news/NN241225051008799); [新浪/财经](https://finance.sina.com.cn/roll/2025-05-28/doc-ineycmqy7871683.shtml); [TechCrunch：Alexa+](https://www.techcrunch.com/2026/03/31/alexa-plus-new-food-ordering-experiences-with-uber-eats-and-grubhub/))。

Bain 的建议与此一致：尽可能保住数据、履约和结账；约 65% 的零售媒体花费在站内，发现一旦迁到 AI 就有风险 ([Bain](https://www.bain.com/insights/2030-forecast-how-agentic-ai-will-reshape-us-retail-snap-chart/); [Crowdfund Insider 转述 Bain](https://www.crowdfundinsider.com/2025/11/255589-bain-co-report-details-how-agentic-ai-will-transform-e-commerce/))。

## 4. JTBD：Agent 最擅长的活，恰好不是广告赚钱的活

消费者的核心任务是"又快又好地吃上，在预算内，少费心"；情感任务是"别让我后悔"；社交任务是团餐。按任务看 Agent 的适配度：

| 谁 | 任务 | Agent 适配度 | 证据 |
|---|---|---|---|
| 消费者 | 复购"老样子"、补货 | 高 | Ask 杂货单的 30%+ 是例行补货；Text DoorDash 支持"order my usual" |
| 消费者 | 有约束的决策：预算、饮食、团餐、多店 | 高 | Text DoorDash 为饮食不同的一群人组单；千问一句话点 40 杯奶茶 |
| 消费者 | 到店决策：找店、订位、排队 | 中高 | 近一半 Ask 餐厅订单流向没去过的店；小团 2.0 用 AI 外呼订位 |
| 消费者 | 冲动、被种草的浏览 | 低 | 这正是广告变现的地方 |
| 商户 | 填满闲时产能，定价促销，管广告和差评 | 给建议高，自动执行中 | Smart Campaigns；智能掌柜；自动改价亏钱的案例（第 7 节） |
| 骑手 | 收入高且确定，安全 | 中 | DashBuddy 答对约 95% 新骑手问题；美团智能头盔、团宝 |

来源：([DoorDash：Text DoorDash](https://about.doordash.com/en-us/news/text-doordash); [科技日报](https://www.stdaily.com/web/gdxw/2026-01/15/content_461939.html); [同花顺](https://stock.10jqka.com.cn/20260727/c678455867.shtml); [Dash Forward 2026](https://about.doordash.com/en-us/news/dash-forward-2026); [新华网](https://www.news.cn/tech/20260909/79161147f2414bc79664a28d32802f8e/c.html))。

**Agent 最擅长的活（复购、有约束的决策）和广告最赚钱的活（冲动浏览）正好相反。这是 S1 的核心张力。**

用价值主张画布拆开：

| 画布要素 | 消费者 | 商户 |
|---|---|---|
| 痛点 | 选错、送错、被推贵的、价格不透明 | 人手不够、代运营贵、差评和恶意索赔 |
| 收益 | 省时间、篮子更全、个性化 | 更多订单、更低获客成本 |
| 止痛药 | 下单前确认、保障和退款、价格透明 | 审批流、ROI 下限、恶意差评拦截 |
| 增益 | 记忆、补货、团餐 | 自动投放、诊断、报表 |

信任决定委托能走多远。可以把委托分成五级：L0 推荐；L1 组好购物车、一键确认；L2 按规则复购（例如"老样子，¥30 以内，12 点前"）；L3 在预算和时间窗内自主下单；L4 完全自主。数据支持停在 L1–L2。Accenture 调查的 25,590 名消费者里，74% 愿意让 Agent 做例行任务，只有 9% 愿意让它自主发起并完成购买 ([AI News](https://www.artificialintelligence-news.com/news/ai-shopping-agents-consumer-trust-accenture-report/))。Bain 发现约一半消费者对全自主购买持谨慎态度，对零售商自家 Agent 的信任约为第三方 Agent 的 3 倍 ([Bain](https://www.bain.com/about/media-center/press-releases/20252/agentic-ai-poised-to-disrupt-retail-even-with-50-of-consumers-cautious-of-fully-autonomous-purchasesbain--company/))。杂货是最愿意委托的品类，占 41% ([Checkout.com](https://www.checkout.com/newsroom/consumer-demand-for-ai-shopping-is-forming-fast-but-trust-for-agentic-commerce-is-still-catching-up))。上海市消保委 2026 年 618 调查里，84.56% 的人用过 AI 导购，只有 16.06% 认为它匹配精准，38.79% 认为它忽视低价需求、推更贵的商品 ([东方财富](https://finance.eastmoney.com/a/202606263784809881.html))。

中国的规则把 L1–L2 写进了制度。支付清算协会 2026-08-24 发布《智能体支付应用自律公约》：账户和资金结算必须由持牌机构完成；"谁提供支付服务谁负责"；要严格核验用户身份和交易意图；Agent 自主发起支付的应用，上线前要向协会备案 ([新华网](https://www.news.cn/fortune/20260824/b44c884bab5d4f89bce4262c362b0736/c.html); [第一财经](https://www.yicai.com/news/103331509.html))。2026-04-10 生效的《互联网平台价格行为规则》禁止"大数据杀熟"，要求披露动态和差异化定价规则 ([新华网](https://www.news.cn/tech/20260410/c5186b8634a74aa089bb632c7a4d6ed0/c.html))。

## 5. S1 自有 App 的点单与决策 Agent：机制成立，账卡在广告

| 要素 | DoorDash | 美团 | 成立吗 |
|---|---|---|---|
| ① 价值主张 | Ask DoorDash（App 内）、Text DoorDash（Apple 信息）；杂货篮快约 5 倍；近一半餐厅订单流向没去过的店 | 小团（App 内）；2.0 能代下单、订位、打车，用 AI 外呼订没上线的店；只披露曝光和回答次数 | 成立；美团缺订单证据 |
| ② 创造与传递 | 内部 MCP 把目录、购物车、结账、记忆做成工具；多家模型 + token 路由；每天 2,000+ 次自动评测 | 自研 LongCat；13 亿+ 条评价；预订、排队、到店点单等交易轨道 | 成立 |
| ③ 价值获取 | 不收费；靠 GOV × 抽佣、DashPass、广告位 | 不收费；靠频次、会员、佣金；回答里放广告未披露 | 未证：取决于增量和广告挤占 |

### 5.1 价值主张：复购、凑单和找新店最有用

DoorDash 2026 年上线了一整套自有 Agent。Ask DoorDash 06-11 在 App 内上线，能按照片和提示词点餐、凑杂货、订位 ([CNBC](https://www.cnbc.com/2026/06/11/doordash-ai-ordering-automation.html))。Text DoorDash 09-30 在美国公测：用户发短信说想吃什么，Agent 搜店、给购物车、在对话里结账；能"order my usual"，能为饮食不同的一群人组团单，还会就习惯性订单主动发消息 ([DoorDash](https://about.doordash.com/en-us/news/text-doordash))。它跑在 Apple 信息里，是 DoorDash 自己的 Agent，不是 Siri 集成 ([MacRumors](https://www.macrumors.com/2026/09/30/doordash-order-through-messages-app/))。此外还有 2025-12 测试的独立发现 App Zesty、7 月公测的开发者 CLI、09-30 的企业 MCP。

DoorDash 公布的数据都是公司自测，各有数据窗口：

| 指标 | 数值 | 口径提醒 |
|---|---|---|
| 新发现餐厅（2026 年 6–8 月） | 4 万+ 家；近一半 Ask 餐厅订单流向用户没去过的店 | 公司口径 |
| 杂货篮构建速度 | 约 5 倍 | 公司口径 |
| 杂货篮金额 | 比同一批用户的传统订单高近 50%，商品种类多约 60% | 有选择偏差：大采购本来就更可能用 Ask |
| 例行补货 | 占 Ask 杂货单的 30%+ | 公司口径 |
| 用户记忆（7 天评测） | 杂货结账转化 +24%，篮子 +17%，对话轮次 −7% | 工程评测，比上一行更接近真实增量 |
| 开放式找店 | 转化 +15% | 工程评测 |

来源：([DoorDash：Text DoorDash](https://about.doordash.com/en-us/news/text-doordash); [InfoQ](https://www.infoq.com/news/2026/07/doordash-ai-ask-assistant/))。

美团的路径是先独立、后收回主 App。2025-09-12 发布独立 App 小美，基于 LongCat，可点外卖、订位排队、定时代办 ([Yicai Global](https://www2.yicaiglobal.com/news/chinas-meituan-launches-voice-command-ai-food-app-to-fend-off-jdcom-alibaba))。早期评测发现，它会"确认"一家其实订不上的餐厅 ([BiaNews](https://www.bianews.com/detail?id=220863))。之后重心转到主 App 里的小团：年报称已向全部用户开放 ([美团 2025 年报](https://www1.hkexnews.hk/listedco/listconews/sehk/2026/0326/2026032600964.pdf))。2026-07-27 的小团 2.0 加了"行动代理"：经用户授权和确认后，订餐厅、下外卖、打车，还能用 AI 外呼替没有线上预订的商户订位 ([同花顺](https://stock.10jqka.com.cn/20260727/c678455867.shtml))。官方说法是从"帮用户找答案"到"帮用户把事办成" ([美团](https://www.meituan.com/news/NN260828216013035))。

美团披露的是曝光和回答，不是订单。2026 年五一，小团"服务过亿人次""累计曝光超 39 亿次"；到 09-09，累计回答超 7 亿次，覆盖 12 类本地生活需求 ([新华网](https://www.xinhuanet.com/tech/20260601/979f59ba54314fd58d0db1ca6e828745/c.html); [美团](https://www.meituan.com/news/NN260828216013035); [新华网](https://www.news.cn/tech/20260909/79161147f2414bc79664a28d32802f8e/c.html))。这些单位不能互比，也换算不成增量 GTV。

### 5.2 价值创造与传递：数据和交易轨道都在自己手里

DoorDash 的架构是：一个 Assistant Runtime 编排多个领域 Agent；内部 MCP 层把目录搜索、推荐、购物车、结账、订单历史和用户记忆做成工具，背后是现有服务；业务逻辑不写进提示词；购物车走确认流程；每天跑 2,000 多次自动评测 ([InfoQ](https://www.infoq.com/news/2026/07/doordash-ai-ask-assistant/))。据 Bloomberg 报道的摘要，Ask 用 OpenAI、Anthropic、Google 和开源模型（付费墙，未直接核实）([Bloomberg](https://www.bloomberg.com/news/articles/2026-06-11/doordash-s-ask-doordash-ai-chatbot-can-help-with-orders-groceries-reservations))。公司自建了 token 路由软件，"把合适的成本用在所需的智能上" ([DoorDash Q2 2026](https://www.sec.gov/Archives/edgar/data/1792789/000179278926000048/proddashex991-pressrelease.htm))。DoorDash 自己也承认边界：LLM 需要可验证的回答，"没有正确的上下文，或没接到合适的工作流时，事情会失控" ([DoorDash 2025 致股东信](https://www.sec.gov/Archives/edgar/data/1792789/000179278926000012/proddashq4ex992-shareholde.htm))。

美团的资产是信息。小团背后有 13 亿+ 条评价、7 亿+ 次商户信息核验，覆盖 2,800+ 城市 ([界面](https://m.jiemian.com/article/14842082.html))。模型是自研的 LongCat-2.0：1.6 万亿参数 MoE，每 token 激活约 480 亿，在国产算力上训练 ([同花顺](https://stock.10jqka.com.cn/20260630/c677828268.shtml))。AI 外呼把没数字化的商户也纳入可订的供给，这是通用 Agent 没有的能力。

### 5.3 价值获取：推理是小头，广告和错误是大头

两家都不向消费者收 Agent 费用，账在原有抽佣里结。Xu 说，购物车变容易不会改变广告经济，只会带来"更多订单和更多广告机会" ([PYMNTS](https://www.pymnts.com/news/delivery/2026/doordash-says-agentic-traffic-is-low-but-ai-gains-are-not/))。美团没有披露小团回答里是否放广告。评论文章指出，对话界面只给 2–3 个候选，列表页有十几个，可售的广告曝光会变少 ([36氪](https://eu.36kr.com/en/p/3916086864090756))。中国消费者本来就很少为 AI 付费：腾讯研究院口径下，AI 用户付费率按月活只有 0.9% ([虎嗅](https://m.huxiu.com/article/4897380.html))。

**DoorDash：每笔 Agent 订单的增量账（估算）**

| 项目 | 每单 | 占每单 Adj. EBITDA（$1.07） |
|---|---|---|
| 推理：小模型（Claude Haiku 5.5 标价） | $0.02 | 2% |
| 推理：中档（Gemini 3.8 Flash，2026 年标价） | $0.14 | 13% |
| 推理：大模型（Claude Sonnet 5.5 标价） | $0.35 | 32% |
| 广告挤占（丢 25%–50% 广告） | $0.17–0.34 | 16%–32% |
| 错误赔付（错误率 0.1%–1%） | $0.04–0.37 | 3%–35% |

假设："发现+下单"会话 8 次模型调用，共 9.6 万输入 token（60% 命中缓存）、5,600 输出 token，40% 的会话成交；广告占 GOV 2%（参照 Uber Delivery 超过 2% 的口径）；每次错误按全额退款加 $3 客服计，约 $37。价格见 ([Anthropic](https://platform.claude.com/docs/en/about-claude/pricing); [Google](https://aistudio.google.com/docs/pricing))。Gemini 3.8 Flash 的标价 2027-01-01 起翻倍。

小模型下，只要 Agent 订单丢掉 3% 以上的广告，广告损失就超过推理费（估算）。复杂会话（团餐、多店、长推理）更贵：用小模型每单约 $0.09，用 Sonnet 5.5 约 $1.80，超过每单 CP（估算）。所以模型路由是硬条件，复杂任务要少，或者只留给大篮子。

**回本门槛：Agent 订单里至少要有多少是真增量（估算）**

| 广告挤占比例 k | 小模型 | 中档模型 |
|---|---|---|
| 0（Xu 的说法：不改变广告经济） | 3% | 10% |
| 25% | 13% | 19% |
| 50% | 22% | 29% |

门槛 =（推理 + 错误 + 广告挤占）÷ 每单 CP $1.79；错误率取 0.1%。用 CP 作增量订单的贡献，偏保守。

**美团：每单推理费占每单利润 ¥1 的比例（估算）**

| 会话类型 | Claude Haiku 5.5 标价 | DeepSeek Flash 标价 | LongCat 对外促销价 |
|---|---|---|---|
| 复购（"老样子"） | 约 1% | 约 2% | 约 2% |
| 发现+下单 | 约 13% | 约 33% | 约 31% |

假设：每单利润取 ¥1（券商估算区间为 −¥0.3 到 ¥1.5）；复购会话 3 次调用、1.8 万输入 token（80% 命中缓存）、900 输出 token，80% 成交；"发现+下单"会话同上表，40% 成交。LongCat 促销价为输入 ¥2、缓存命中 ¥0.04、输出 ¥8 每百万 token ([80AJ](https://www.80aj.com/2026/06/30/meitongcat-ai-model-9-9/))；美团的内部成本未披露。DeepSeek 价格见 ([DeepSeek](https://api-docs.deepseek.com/quick_start/pricing))。

外卖里，只有短的复购会话足够便宜。长的发现会话即使用国产便宜模型，也要吃掉每单约三分之一的利润。广告的量级更大：美团每丢 10% 广告收入，一年约 51 亿元，约等于 Q2 年化 CLC 经营利润的 23%（估算；Q2 季节性偏强）。会员是另一条回收路径：DashPass 提高频次和留存，但毛利率更低 ([DoorDash Q2 2026](https://www.sec.gov/Archives/edgar/data/1792789/000179278926000048/proddashex991-pressrelease.htm))；美团 2025-03 把神会员并入统一的美团会员 ([今日头条](https://www.toutiao.com/zixun/7488885754207225866))，年报称会员提升了频次和跨业务交叉销售 ([美团 2025 年报](https://www1.hkexnews.hk/listedco/listconews/sehk/2026/0326/2026032600964.pdf))。

美团三条业务线的账不一样：

| 业务线 | 每单或分部经济 | Agent 最适合的活 | 对手的 Agent 入口 | 读法 |
|---|---|---|---|---|
| 外卖 | 每单约 −¥0.3 到 ¥1.5（券商估算） | 复购、团餐、预算约束 | 千问→淘宝闪购（¥30 亿免单） | 只能用短会话、便宜模型 |
| 闪购 | 2026 Q2 预计亏约 ¥7.5 亿（摩根士丹利估算） | 补货、凑单（参照 DoorDash 杂货） | 千问→淘宝闪购；阿里即时零售收入同比 +45% | 篮子大，Agent 价值高，但还在亏 |
| 到店酒旅 | 分部利润率约 25%，2024 年前超过 40%（券商估算） | 找店、订位、排队、团餐 | 豆包→抖音来客；高德扫街榜 | 利润池最大，Agent 最值得投，也最先对盟友开放 |

来源：([华尔街见闻](https://wallstreetcn.com/articles/3773639); [新浪财经](https://finance.sina.com.cn/stock/t/2026-08-24/doc-inipmnpm0038750.shtml); [虎嗅](https://www.huxiu.com/article/4888998.html); [界面](https://m.jiemian.com/article/14902227.html); [Yicai Global](https://www2.yicaiglobal.com/news/chinas-meituan-launches-voice-command-ai-food-app-to-fend-off-jdcom-alibaba))。到店的对手在逼近：据虎嗅，抖音到店核销前 GTV 预计 2026 年约 1.2 万亿元；野村估算美团 2025 年到店 GTV 约 1.18 万亿元 ([虎嗅](https://www.huxiu.com/article/4888998.html))。

### 5.4 判断

框架读法：JTBD 说复购和有约束的决策适合交给 Agent；单位经济说推理是小头、广告是大头；7 Powers 说独有供给信息是护城河，反定位说靠广告赚钱的在位者很难让自己的 Agent"客观"。

**判断：** ① 成立，DoorDash 的证据更硬。② 成立。③ 机制可行，账未证。成立要四个条件：用对照组测出的真增量超过门槛（DoorDash 小模型下 3%–22%）；把广告搬进 Agent，做成标注清楚的赞助推荐，而且和"最适合你"的排序分开；默认路由到小模型；错误率压在 0.1% 量级，并保留下单前确认。两家的差别在第三条和第二条：DoorDash 的每单利润和客单价都更高，试错空间更大；美团外卖的每单利润只有约 ¥1，价格规则又限制"付费被选中"，S1 的账更多要靠到店和闪购来结。

## 6. S2 被第三方通用 Agent 调用：能当获客渠道，不能当收费站

| 要素 | DoorDash | 美团 | 成立吗 |
|---|---|---|---|
| ① 价值主张 | ChatGPT 杂货 App（结账跳回 DoorDash）；Gemini 安卓自动化试点（比自己点慢）；企业 MCP | 元宝×小美 A2A（已宣布，未见上线）；WorkBuddy 里的"美团生活助手"卖到店团购券；拒绝豆包 GUI 自动化 | 弱；企业订餐和桌面团购是例外 |
| ② 创造与传递 | 账户、结账、DashPass、履约都在 DoorDash | 被调用的是美团自己的 Agent 或专属能力；券、核销、履约在美团 | 成立：履约绕不过平台 |
| ③ 价值获取 | 没有披露任何费用；Xu 视其为"像 Facebook 和 Google 那样的渠道伙伴" | 没有披露费用；用自己出的专属优惠券买流量 | 作收费站不成立；作获客渠道有条件成立 |

### 6.1 美国：通用 Agent 是中立的，订单只有几百单

| 入口 | DoorDash 的形态 | 账户、结账、履约 | 费用 |
|---|---|---|---|
| ChatGPT | 2025-12-17 上线杂货 App；在 ChatGPT 设置里绑定 DoorDash 账户；到 DoorDash App 里改单和结账 | DoorDash | 未披露 |
| Gemini（安卓） | 2026-02 宣布、03 月起推送：在安全虚拟窗口里代操作 DoorDash App；用户确认最终下单；DoorDash 称为试点 | DoorDash（界面被 Agent 操作） | 未披露 |
| Alexa+ | 2026-03-31 接入的是 Grubhub 和 Uber Eats，没有 DoorDash | — | — |
| 企业 MCP、CLI | CLI 2026-07 公测；企业 MCP 09-30 推出，早期伙伴有 SpaceX、Cognition、Vercel、Mercor、Tempo | DoorDash（员工各自登录） | 未披露 |
| Apple 信息 | DoorDash 自己的 Text DoorDash | DoorDash | — |

来源：([DoorDash：ChatGPT](https://about.doordash.com/en-us/news/openai); [TechCrunch：Gemini](https://techcrunch.com/2026/02/25/gemini-can-now-automate-some-multi-step-tasks-on-android); [TechCrunch：Alexa+](https://www.techcrunch.com/2026/03/31/alexa-plus-new-food-ordering-experiences-with-uber-eats-and-grubhub/); [Digital Commerce 360](https://www.digitalcommerce360.com/2026/10/01/doordash-expands-access-to-agentic-ordering-for-offices-corporate-customers/))。

量极小。Xu 在 10-29 说，第三方 Agent "总共大概几百单" ([DoorDash Q3 2026 电话会](https://www.fool.com/earnings/call-transcripts/2026/10/29/doordash-dash-q3-2026-earnings-call-transcript/))。2026 年前三季度 DoorDash 共约 29 亿单，几百单约是千万分之一（计算）。Instacart 也说直接来自 AI Agent 的订单"占比非常小" ([Modern Retail](https://www.modernretail.co/technology/what-went-wrong-with-chatgpts-instant-checkout/))。体验也不占优：Tom's Guide 的测试者发现，经 Gemini 点 DoorDash"明显比自己点慢" ([Tom's Guide](https://www.tomsguide.com/phones/google-pixel-phones/samsung-galaxy-s26s-best-new-ai-feature-is-now-available-on-pixel-10-heres-how-it-works))。

想当收费站的 Agent 已经退了。OpenAI 2025-09 推出 Instant Checkout，据报道 2026-01 起向 Shopify 商户收每单 4% ([Shopifreaks](https://www.shopifreaks.com/shopify-confirms-a-4-fee-on-sales-made-through-chatgpt-but-whats-the-real-cost/))。Walmart 发现 ChatGPT 内结账的转化率只有跳回 Walmart.com 的三分之一 ([MarTech](https://martech.org/walmart-says-chatgpt-checkout-converted-3x-worse-than-its-own-website/))。OpenAI 03-24 宣布让商户用自己的结账，ChatGPT 专注发现 ([Modern Retail](https://www.modernretail.co/technology/what-went-wrong-with-chatgpts-instant-checkout/))。Google 的 UCP 也写明，商户仍是记录商户，保留客户关系、定价和交易数据 ([Technobezz](https://www.technobezz.com/news/google-launches-universal-commerce-protocol-for-ai-shopping-2026-01-11-jkpd))。

平台的口径一致：Agent 是渠道，不是威胁。Xu 把聊天助手称作"新形态的 Google"，"我把它们看作渠道伙伴"，就像过去 Facebook 和 Google 给 DoorDash 带流量 ([DoorDash Q4 2025 电话会](https://www.fool.com/earnings/call-transcripts/2026/02/18/doordash-dash-q4-2025-earnings-call-transcript/))。10 月他又说"Agent 里也会有广告机会"，但现在量太小，"还没有那个价值" ([DoorDash Q3 2026 电话会](https://www.fool.com/earnings/call-transcripts/2026/10/29/doordash-dash-q3-2026-earnings-call-transcript/))。Uber 的 Khosrowshahi 说会和 Apple、OpenAI、Claude、Gemini 的 Agent 合作，但多数需求仍会直接来 Uber，"元搜索就是这样演变的" ([Diginomica](https://diginomica.com/node/30465))。DoorDash 的 10-Q 则把 Agent 列为风险：它可能改变消费者发现和使用服务的方式，"提高竞争对手的可见度" ([DoorDash 10-Q](https://www.sec.gov/Archives/edgar/data/1792789/000179278926000070/dash-20260930.htm))。

替代品已经出现。Square 07-01 推出 ChatGPT App 和 Claude 插件，消费者可以直接向用 Square 在线点单的美国餐饮商户下单，不收平台佣金，只收常规支付手续费 ([VentureBeat](https://venturebeat.com/marketing-tech/restaurants-can-now-accept-orders-placed-directly-from-chatgpt-and-claude-thanks-to-squares-new-low-fee-no-setup-integration/))。这是 Citrini 情景的第一个真实版本，量未披露。法律的护栏在变弱：第九巡回法院撤销了地区法院应 Amazon 请求对 Perplexity Comet 下达的初步禁令，理由是访问 Amazon 的是用户，不是 Perplexity ([Engadget](https://engadget.com/2230471/perplexity-has-successfully-overturned-amazon-injunction-on-its-ai-shopping-bot))。平台转而靠条款（eBay 2026-02-20 起禁止未经批准的购物 Agent）和技术封锁 ([EcommerceBytes](https://www.ecommercebytes.com/2026/01/21/ebay-bans-ai-shopping-agents-updates-arbitration-provision/))。

### 6.2 中国：入口绑定生态，最大的两个入口都是对手的

| 入口（月活） | 主人 | 导向 | 对美团 |
|---|---|---|---|
| 豆包（2026-09 约 4.19 亿） | 字节 | 抖音生活服务；酒店订单渠道费约 12% | 关闭：美团在豆包"拒绝屏幕自动化"名单里（10-14） |
| 千问（2026-09 约 1.58 亿；阿里口径 2026-02 消费端超 3 亿） | 阿里 | 淘宝闪购 + 支付宝 AI付 | 关闭：未见与美团集成 |
| 元宝（2026 Q1 约 5,735 万，排第四） | 腾讯 | 小美 A2A（06-01 宣布，未见上线） | 开放：盟友 |
| WorkBuddy（2026-09 约 3,235 万） | 腾讯 | "美团生活助手"卖到店团购券 | 开放：首个服务伙伴 |
| 微信 AI 生态 | 腾讯 | 06-08 向开发者开放，美团、京东首批内测 | 开放 |

来源：([澎湃：豆包、千问](https://www.thepaper.cn/newsDetail_forward_34298060); [观察者网](https://www.guancha.cn/economy/2026_03_19_810672.shtml); [澎湃：元宝](https://m.thepaper.cn/newsDetail_forward_33176080); [199IT](https://www.199it.com/archives/1858394.html); [界面](https://m.jiemian.com/article/14525559.html); [新浪财经](https://finance.sina.com.cn/roll/2026-06-09/doc-iniauvau3247861.shtml); [京报网](https://news.bjd.com.cn/2026/10/15/11996628.shtml))。月活为 QuestMobile 口径，阿里口径另注。

阿里做的是一体化版本。千问 01-15 接入淘宝闪购、淘宝和支付宝 AI付，一句话点外卖，在对话里付款 ([科技日报](https://www.stdaily.com/web/gdxw/2026-01/15/content_461939.html))。春节"30 亿大免单"的 6 天里，用户说了 41 亿次"千问帮我"，AI 完成 1.2 亿+ 单 ([扬子晚报](https://www.yzwb.net/news/ch/202602/t20260212_321578.html))。活动上线当天，微信封了千问的分享链接，元宝自己的红包链接两天前也被封过 ([光明网](https://tech.gmw.cn/2026-02/06/content_38582125.htm))。对美团来说，千问不是中立的第三方，是对手的前门。

字节先试了 GUI 路线，再改成收渠道费。2025-12，豆包手机助手用系统权限模拟点击，跨 App 操作；微信率先限制，淘宝、支付宝和多家银行跟进，豆包随后收缩场景 ([HK01](https://global.hk01.com/数码生活/60301489/豆包ai手机遭微信阿里限制-跨app自动操作引争议-官方宣布调整))。2026-08-10 起，从豆包进入抖音来客的酒店订单单列为约 12% 的费率（11.4% 软件服务费 + 0.6% 支付费），此前按自然流量收 8%；豆包称不收广告费，商户也不能付费影响排序 ([界面](https://m.jiemian.com/article/14902227.html))。抖音还对部分品类由豆包导流的订单另收 4%–13% 技术服务费 ([人人都是产品经理](https://www.woshipm.com/ai/6485093.html))。10-14 豆包公布"屏幕自动化操作"拒绝名单，150 多个 App 在内，明确包括美团、微信、支付宝、淘宝 ([京报网](https://news.bjd.com.cn/2026/10/15/11996628.shtml))。分析师江瀚的话很准："头部平台拒绝的不是技术本身，而是'被绕过'。"

腾讯有入口、有支付，没有本地履约，所以请美团进来。06-01 王兴宣布小美将与元宝"深度合作"：用户在元宝里说出本地生活需求，元宝以 Agent-to-Agent 方式调用小美，小美再调用美团外卖 ([界面](https://m.jiemian.com/article/14525559.html))。截至 11 月未见上线公告。06-08 微信向开发者开放 AI 生态，美团、京东是首批内测方 ([新浪财经](https://finance.sina.com.cn/roll/2026-06-09/doc-iniauvau3247861.shtml))。

**AI 专属卡的准确说法。** 06-17 微信支付在零钱里推出"AI 专属卡"：一个由用户自己充值、与主账户隔离的子账户，Agent 只能在用户设定的范围内花卡里的钱，每笔都要在手机上确认 ([量子位](https://www.qbitai.com/2026/06/436160.html))。**首个接入的 Agent 是腾讯 WorkBuddy**（Mac 版 5.1.1 起）。**美团是 WorkBuddy 里的首个服务伙伴**：它为 WorkBuddy 做了购买餐饮团购的专属能力，并提供专属优惠券；用户在 WorkBuddy 里召唤"美团生活助手"专家，挑团购，手机确认后从 AI 卡扣款，到店核销 ([每日经济新闻](https://www.nbd.com.cn/articles/2026-06-17/4429249.html))。**卖的是到店团购券，不是外卖。** 双方没有披露佣金或分成。

为什么先开团购，不开外卖？团购券是数字商品，不需要骑手；到店酒旅的分部利润率约 25%，外卖每单只有约 ¥1（券商估算）。能拿出优惠券买新客的，是利润厚的业务。这是本研究的推断，美团没有解释过顺序。

监管把支付关系留给了持牌钱包。前述支付清算协会公约要求持牌机构完成结算、逐笔核验意图、自主支付应用备案。支付宝的 ACT 协议也规定"用户始终确认支付" ([扬子晚报](https://www.yzwb.net/news/ch/202601/t20260116_311925.html))。独立的 Agent 厂商很难拿到支付关系，也就很难拿走客户。

### 6.3 单位经济：通道费超过每单利润

**DoorDash：不同通道费率下的每单利润（估算）**

| 通道费率 | 每单通道费 | 转移单：原 Adj. EBITDA $1.07 变为 | 增量单：原 CP $1.79 变为 |
|---|---|---|---|
| 4%（OpenAI 先例，已撤回） | $1.37 | −$0.30 | +$0.42 |
| 7%（Citrini 假设情景） | $2.39 | −$1.32 | −$0.60 |
| 12%（豆包酒店渠道费） | $4.10 | −$3.03 | −$2.31 |
| 盈亏平衡费率 | — | 3.1%；若该单广告全丢，约 1.1% | 5.2% |

按 Q3 2026 每单 GOV $34.2 计。转移单指本来就会在 DoorDash 下的单，只是换了入口；增量单指没有 Agent 就不会发生的单。广告按 GOV 的 2% 假设。

**美团外卖：** 按每单利润 ¥1、客单价 ¥30–50 计（假设，参照《财经》算例里的 ¥29 和 ¥60 两单 ([新浪/财经](https://finance.sina.com.cn/roll/2025-05-28/doc-ineycmqy7871683.shtml))），可承受的通道费上限约 2.0%–3.3%（估算）。按豆包酒店的约 12% 收，每单要付 ¥3.6–6.0，是该单利润的 3.6–6 倍（估算）。

**补贴才是中国 Agent 的获客成本。** 千问 ¥30 亿免单换来 6 天 1.2 亿+ 单，平均每单补贴的上限约 ¥25（估算：预算 ÷ 订单数；预算未必全部花在这批订单上）。一次推理只要几分钱。在中国，买 Agent 习惯的主要成本是补贴，不是 token。补贴退坡后的复购，阿里没有披露。

S2 的真实价值是新客。据单一来源，ChatGPT 给 Walmart 带来的新客比例约为搜索引擎的 2 倍 ([ppc.land/WIRED](https://ppc.land/walmarts-chatgpt-checkout-flopped-heres-what-comes-next/))。Winters 的处方是：把渠道费当获客成本，"别再为没有身份的流量多付钱"；只为首单付，复购拉回自有 App ([Casey Accidental](https://www.caseyaccidental.com/p/when-agents-attack-how-ai-collapses))。一笔永久 4% 的复购费，会让 DoorDash 的转移单变成亏损。

### 6.4 判断

框架读法：聚合理论说通用 Agent 能拿走需求界面，拿不走履约，所以平台守得住 ②；五力说入口方是新的强势买方，所以 ③ 的风险在通道费；7 Powers 说独有资源和流程能力在平台手里，这正是 DoorDash 和美团敢拒绝"收费站"的底气。

**判断：** ① 弱。对消费者，外卖经第三方 Agent 下单并不比 App 快；企业订餐（DoorDash MCP）和桌面用户买团购（WorkBuddy）是例外，任务清楚且会重复。② 成立。③ 作收费站不成立，任何超过约 3% 的费率都让转移单亏钱；作获客渠道有条件成立，条件是只为新客首单付费、账户和会员留在平台、复购拉回自有入口。

两家的差别在入口格局。DoorDash 面对中立的通用 Agent，被比价、被替换的风险更高，但量可以忽略；它的对冲是让自己的 Agent 出现在别人的渠道里（Text DoorDash 在 Apple 信息里）。美团面对的是生态之争：最大的两个入口属于抖音和淘宝闪购的母公司。它的对冲是与腾讯结盟，让自己的小美被调用，而不是只给一个裸 API；对未经许可的 GUI 自动化则说不。

## 7. S3 商户经营 Agent：免费送出去，靠广告收回来

| 要素 | DoorDash | 美团 | 成立吗 |
|---|---|---|---|
| ① 价值主张 | Smart Campaigns 替商户买"始终 ROI 为正"的广告和促销；AI 上架快 35%+ | 智能掌柜 130 万+ 商户、解决 860 万个问题；每月拦约 150 万条恶意差评和威胁 | 成立 |
| ② 创造与传递 | 嵌在商户后台；自有广告系统加 Symbiosys 站外广告 | 嵌在外卖和到店后台；CatPaw 可配置、可私有化部署 | 成立；写权限限于平台店铺 |
| ③ 价值获取 | 通过广告花费直接变现；网站和营销工具免佣 | 免费；靠留存、广告效率和供给质量 | DoorDash 成立；美团间接 |

### 7.1 价值主张：替商户省下已经在付的钱

商户的任务是赚钱地填满产能：定菜单、定价、做促销、投广告、管差评。DoorDash 最成熟的 Agent 就在这里。Xu 说，Smart Campaigns 替餐厅买"始终 ROI 为正"的广告，是去年上线的"最大的 Agent 之一"，也是餐厅侧增长最快的产品之一 ([DoorDash Q4 2025 电话会](https://www.fool.com/earnings/call-transcripts/2026/02/18/doordash-dash-q4-2025-earnings-call-transcript/))。案例：Pubbelly Sushi 九个月做了 30 万美元+ 销售、4,500 单，每花 $1 带来 $4+；CPG 自动出价在 95%+ 的测试活动里超过了设定的最低 ROAS，目标由 DoorDash 设定 ([DoorDash Ads](https://ir.doordash.com/news/news-details/2026/DoorDash-Ads-Becomes-a-Global-Commerce-Media-Platform/default.aspx))。05-04 推出的 AI 工具让商户上架快 35%+，AI 网站平均下单转化接近 10%，不收佣金 ([DoorDash](https://about.doordash.com/en-us/news/ai-powered-merchant-tools))。

美团的商户 AI 覆盖面更广。2025-10 推出袋鼠参谋（选址、商圈、菜品、定价）、袋鼠管家（外卖后台）、智能掌柜（到店后台），智能掌柜对中小商户免费 ([36氪](https://www.36kr.com/p/3512515121077381))。到 2026-09-09，智能掌柜服务 130 万+ 餐饮商户，解决 860 万个问题 ([新华网](https://www.news.cn/tech/20260909/79161147f2414bc79664a28d32802f8e/c.html))。"商家 AI 守护"识别准确率 99%，每月拦约 150 万条恶意差评或威胁 ([中新网](https://www.chinanews.com.cn/cj/2026/06-26/10647834.shtml))。10-22 推出的 AI店长对全国餐饮商户免费，试点中一家门店周订单 +30% ([雷锋网](https://www.leiphone.com/category/industrynews/tZI7pYBhiFduVfr6.html))。

商户愿意付钱的地方，是 Agent 替代了一笔已经在付的钱。美国餐厅 2026 年只有 26% 用 AI，6% 用在顾客点单上 ([Restaurant Dive](https://www.restaurantdive.com/news/national-restaurant-assocation-operator-artificial-intelligence-adoption/812418/))。税前利润率中位数只有 2.8%（正餐）和 4.0%（快餐）([Duck-Hub 汇编 NRA 数据](https://www.duck-hub.com/blog/restaurant-profit-margin-statistics))。Toast 的案例客户用 Toast IQ Grow 把代运营支出砍了 70% ([Toast Q2 2026 电话会](https://www.fool.com/earnings/call-transcripts/2026/08/11/toast-tost-q2-2026-earnings-call-transcript/))。

### 7.2 价值创造与传递：平台只有自家店铺的写权限

平台的商户 Agent 嵌在已有后台里，分发成本接近零。但写权限只覆盖平台上的店铺：菜单、促销、广告。门店的 POS、排班、库存和电话在 POS 厂商手里。Toast 有约 18 万家门店，05 月推出的 IQ Grow 是它"上线过增长最快的产品"，工作流由人审批 ([Toast Q2 2026 电话会](https://www.fool.com/earnings/call-transcripts/2026/08/11/toast-tost-q2-2026-earnings-call-transcript/))。Square 的 Managerbot 公测免费，"Managerbot 提议，商户批准" ([Square](https://squareup.com/us/en/press/managerbot-open-beta))。大连锁的得来速语音 AI 多在自家栈上：Taco Bell 用 Byte by Yum 和 Omilia，麦当劳用 ArchIQ 和 Google ([RTN](https://restauranttechnologynews.com/2026/07/taco-bell-expands-drive-thru-voice-ai-to-nearly-900-restaurants-across-the-u-s-as-automation-gains-momentum/); [Restaurant Dive](https://www.restaurantdive.com/news/mcdonalds-archiq-drive-thru-artificial-intelligence-next-strategy/831256/))。美团的 CatPaw 商户版也规定，改价和促销仍需经营者确认 ([华尔街见闻](https://wallstreetcn.com/articles/3778095))。

### 7.3 价值获取：美国价格锚 $400–500，中国是零

| 产品 | 定价 | 来源 |
|---|---|---|
| Slang.ai Core / Premium（电话 AI） | 每店每月 $399 起 / $599 起 | [Katalyst](https://www.katalystos.com/blog/ai-phone-answering-for-restaurants-what-it-costs) |
| Kea（电话点单 AI） | 每月 $450 | [Katalyst](https://www.katalystos.com/blog/ai-phone-answering-for-restaurants-what-it-costs) |
| Bite Buddy（电话点单 AI） | 每单 $1.50 | [Katalyst](https://www.katalystos.com/blog/ai-phone-answering-for-restaurants-what-it-costs) |
| Owner.com（网站、点单、营销） | $249/月 + 每单 5%，或 $499/月 | [Owner](https://www.owner.com/pricing) |
| Toast IQ Grow（营销 Agent + 人工经理） | $499/月 | [Yahoo Finance](https://finance.yahoo.com/markets/stocks/articles/toast-tost-launches-toast-iq-132858023.html) |
| Square Managerbot | 公测期免费 | [Square](https://squareup.com/us/en/press/managerbot-open-beta) |
| 美团 AI店长、袋鼠参谋 | 免费 | [雷锋网](https://www.leiphone.com/category/industrynews/tZI7pYBhiFduVfr6.html); [新浪](https://client.sina.com.cn/news/2025-10-16/doc-infucfih9409659.shtml) |
| 客如云的五个 Agent | 2026 年起免费 | [新浪（赞助内容）](https://finance.sina.com.cn/jjxw/2026-03-26/doc-inhshnxa6264750.shtml) |

**每商户每月的账（估算）**

| 项目 | 美国（DoorDash 参照） | 中国（美团参照） |
|---|---|---|
| Agent 推理费 | 约 $0.2–1.2 | 同量级 |
| 商户愿付的价格锚 | 营收类 Agent 每店每月 $399–499 | ¥0 |
| 平台的回收路径 | 广告花费（Smart Campaigns）；SevenRooms、DashOS 订阅（未披露价格）；Drive 按单费 | 留存、广告效率、供给质量；CatPaw 企业版（未定价） |
| 主要成本 | 客服和人工复核；出错赔付 | 同左；外加"平台 GMV 优先"带来的信任成本 |

推理费假设：每月 30 次诊断或报表，每次 2 万输入 token（一半命中缓存）、2,000 输出 token，按 DeepSeek Flash 到 Claude Sonnet 5.5 的标价。

推理费可以忽略，难点在定价权。商户侧的回本门槛很低：一家年销售 100 万美元的门店，每月付 $450，一年 $5,400，约占销售额 0.5%，相当于 2.8%–4.0% 税前利润的 14%–19%；按增量订单 65% 的贡献率，每月多约 23 单 $30 的订单就回本（估算）。所以厂商都在讲"漏接电话"。但这些 ROI 都是厂商自报，没有审计。

可行的变现模式有三种：一是按被替代的成本定价的 SaaS（Toast IQ Grow 对标代运营费）；二是免费捆绑，从支付或抽佣里收回（Square、美团、客如云）；三是 AI 渠道附加费（抖音对豆包导流订单另收 4%–13%）。平台适合第二、三种。第一种在美国要和 POS 的免费捆绑竞争，在中国要面对零价锚。

中国商户对"免费"有戒心。一篇评论题为"免费的才是最贵的"：平台通过佣金和推广费收回成本，数据由平台生产、持有和解读，平台的 GMV 目标和商户利润不一致；文中举了 AI 或代运营自动改价亏钱的案例，深圳一家店 15 道菜同一秒改成 ¥28.81，一下午亏了 ¥2,000–3,000 ([OFweek](https://mp.ofweek.com/Internet/a056714478607))。也有商户关掉了平台的 AI 客服，说"礼貌有余、能力不足" ([人人都是产品经理](https://www.woshipm.com/ai/6485093.html))。

### 7.4 判断

框架读法：价值链说执行需要写权限，平台只有自家店铺的；平台经济学说商户 Agent 可以免费，因为它让商户在平台上花更多广告；JTBD 说商户只为替代已付的钱买单。

**判断：** ① 成立。② 成立，但限于平台店铺。③ DoorDash 已经成立：Smart Campaigns 把 AI 直接变成广告花费，而且 ROI 对商户可见。美团是间接的：工具免费，回报来自留存和广告效率。美团有一个张力：帮商户少花冤枉钱的营销工具，短期会压低广告收入，除非同时把出价自动化（推断）。成立的条件是：ROI 透明、由商户设下限；改价和投放要商户批准；承诺对齐商户利润，而不只是平台 GMV。两家的差别是，DoorDash 有一条软件收费梯子（SevenRooms、DashOS、Commerce Platform），美国商户也习惯为工具付钱；美团的规模大得多，但直接付费意愿接近零。

## 8. S4 履约与无人配送：单位杠杆最大，规模最慢

| 要素 | DoorDash | 美团 | 成立吗 |
|---|---|---|---|
| ① 价值主张 | 平台降低每单配送成本；DashBuddy 答对约 95% 新骑手的问题 | 抵消上升的骑手保障成本；智能头盔、团宝、红灯停表 | 成立 |
| ② 创造与传递 | Dot（凤凰城 2,000+ 商户）、DoorDash Air（FAA Part 135）、Waymo、Serve、Wing、Coco；ADP 统一调度 | 无人机累计 100 万+ 单，低空航网常态运行；小黄蜂机器人；调度系统无近期数据 | 部分：技术和监管都早 |
| ③ 价值获取 | Dasher 支出在收入里扣，省下的直接进抽佣率；自主配送投入正在压 Q4 利润 | 骑手成本结构性上升；自动化目前是能力和牌照，不是成本杠杆 | 未证：杠杆大，规模未到 |

### 8.1 价值主张和交付：机器人、无人机和调度

DoorDash 在物理层投入"可观的资金" ([DoorDash Q3 2026](https://www.sec.gov/Archives/edgar/data/1792789/000179278926000062/proddashex991-pressrelease.htm))。自研机器人 Dot 2025-09 发布，约为一辆车的十分之一大，时速最高 20 英里，能载 30 磅 ([The Robot Report](https://www.therobotreport.com/doordash-unveils-dot-its-first-commercial-autonomous-delivery-robot/))；2026-09 已覆盖凤凰城 2,000+ 家商户 ([Food On Demand](https://foodondemand.com/09302026/doordash-unveils-delivery-drone-unpacks-dot-robot-expansion-at-dash-forward/))。DoorDash Air 拿到 FAA Part 135 航空承运人认证；自研无人机能装下约 80% 的典型餐厅订单，初测平均不到 5 分钟送达 ([DoorDash Q2 2026](https://www.sec.gov/Archives/edgar/data/1792789/000179278926000048/proddashex991-pressrelease.htm); [Dash Forward 2026](https://about.doordash.com/en-us/news/dash-forward-2026))。Xu 称自主配送平台 ADP（为每单在 Dasher、无人机、机器人之间选择）"可能是我们在建的最有价值的部分" ([DoorDash Q4 2025 电话会](https://www.fool.com/earnings/call-transcripts/2026/02/18/doordash-dash-q4-2025-earnings-call-transcript/))。协同也在省时间：Popeyes 与 DoorDash 的 Smart Prep Time 把骑手等餐从 9 分钟降到 3 分钟 ([Food On Demand](https://foodondemand.com/09302026/doordash-unveils-delivery-drone-unpacks-dot-robot-expansion-at-dash-forward/))。

美团的自动化以无人机最成规模：到 2026-06 末累计 100 万+ 单，覆盖深圳、北京、上海、广州、香港和迪拜，其中 81 万+ 单是检测样本和药品 ([IT之家](https://www.ithome.com/0/980/150.htm))；5 月起"城市低空航网"常态运行，并向行业伙伴开放 ([新华网](https://www.xinhuanet.com/tech/20260601/979f59ba54314fd58d0db1ca6e828745/c.html))。骑手侧 AI 偏安全：智能头盔碰撞识别率超过 95%，事故核实时间从 382 分钟降到 2 分钟，覆盖 170 万+ 骑手 ([新华网](https://www.news.cn/tech/20260909/79161147f2414bc79664a28d32802f8e/c.html))；另有"团宝"安全助手；"红灯停表"（红灯时暂停配送计时）先在北京上线 ([美团](https://www.meituan.com/news/NN260828216013035))。

### 8.2 单位经济：每单省 10 美分，每季 9,900 万美元

| 项目 | DoorDash | 美团 |
|---|---|---|
| 人工履约成本参照 | 每单 $8–10（Serve Robotics 投资者材料） | 未披露；新职伤 2026-07-01 起全国由平台全额缴纳；养老补贴 2025-11 起覆盖全国 |
| 自动化成本目标 | 每单约 $1，前提是每台机器人每季度 1,500 单以上（Serve 的目标，不是实绩） | 未披露 |
| 敏感度 | 每单省 $0.10 ≈ 每季多 9,930 万美元收入 ≈ 净收入率 +0.29 个百分点（计算） | 每单省 ¥0.05 ≈ 每年约 13 亿元，约为 Q2 年化 CLC 经营利润的 6%（估算，假设日均 7,000 万单） |
| 示意 | 机器人承担 1% 订单（每季约 990 万单）、每单省 $3–7：每季 3,000–7,000 万美元，约为 Q3 Adj. EBITDA 的 3%–7%；需约 6,600 台机器人（估算） | — |
| 现状 | Dot 的目标：年底前在最大测试市场送"高个位数百分比"的订单 | 无人机累计单量不到一天外卖量的 2%（计算，按日均 6,500 万–8,500 万单） |

来源：([Boardroom Alpha 转载 Serve 投资者材料](https://www.boardroomalpha.com/sec/serv-8-k-2026-03-11-0001832483-26-000004/finalserve_investorxdeck.htm); [DoorDash Q3 2026](https://www.sec.gov/Archives/edgar/data/1792789/000179278926000062/proddashex991-pressrelease.htm); [DoorDash Q2 2026](https://www.sec.gov/Archives/edgar/data/1792789/000179278926000048/proddashex991-pressrelease.htm); [美团](https://www.meituan.com/news/NN260828216013035); [第一财经](https://www.yicai.com/news/102884302.html))。美团日单量有冲突：内部人士说 2026 Q1 约 6,500 万，汇丰说 2025 Q4 约 8,500 万 ([虎嗅](https://www.huxiu.com/article/4864316.html); [搜狐转述汇丰](https://m.sohu.com/a/1007728090_613239/))。

成本正往有利于自动化的方向走。纽约外卖员最低时薪 04-01 起升到 $22.13（不含小费）([NYC DCWP](https://www.nyc.gov/site/dca/news/009-26/major-victory-nyc-))；DoorDash 说最低收入标准已经迫使它提高对消费者和商户的收费 ([DoorDash 10-Q](https://www.sec.gov/Archives/edgar/data/1792789/000179278926000070/dash-20260930.htm))。美团 CFO 说，新职伤"增加了一层成本"，Q3 每单配送成本会高于 Q2 ([美团 Q2 2026 电话会](https://www.investing.com/news/transcripts/earnings-call-transcript-meituan-posts-q2-2026-profit-as-revenue-growth-accelerates-93CH-4881117))。

但钱先花出去。DoorDash 说 Q4 利润率会受自主配送等投入拖累 ([DoorDash Q2 2026](https://www.sec.gov/Archives/edgar/data/1792789/000179278926000048/proddashex991-pressrelease.htm))；10-Q 提示，自主配送可能比预期慢，"可能需要更多前期资本投入，且未必产生回报" ([DoorDash 10-Q](https://www.sec.gov/Archives/edgar/data/1792789/000179278926000070/dash-20260930.htm))。

### 8.3 判断

框架读法：7 Powers 说配送密度（规模经济）和调度（流程能力）是平台最硬的护城河，自动化会加深它；五力说竞争会逼平台把省下的钱让给消费者，在三家混战的中国更是如此；监管既是顺风（人工成本上升），也是约束。中国的《外卖平台服务管理基本要求》要求调度算法考虑路况和天气，不得用算法强制超时 ([第一财经](https://www.yicai.com/news/102948456.html))，AI 调度不能只朝"压榨骑手"的方向优化。

**判断：** ① 成立。② 部分成立。③ 机制最直接，但未证。成立的条件是：利用率过门槛（Serve 的参照是每台每季度 1,500 单）；牌照和空域到位；市场足够集中，省下的钱留得住。DoorDash 的账更清楚，因为 Dasher 支出直接在收入里扣，而且美国的人工成本更高；美团的自动化目前更像牌照、空域和医疗物流的能力储备，真正的成本杠杆在调度效率。

## 9. S5 内部提效：人人在用，省了多少没人披露

| 要素 | DoorDash | 美团 | 成立吗 |
|---|---|---|---|
| ① 价值主张 | 工程师编程 Agent 日活 90%+；各职能有 AI 专项组 | CatPaw 研发渗透 95%+，增量代码 50%+ 由 AI 生成；9 万员工、3 万+ 内部 Agent | 成立 |
| ② 创造与传递 | 自建 token 路由；新的全球技术栈"为 AI Agent 做好准备" | LongCat-2.0 用于研发、运营、客服和 Agent | 成立 |
| ③ 价值获取 | 没有披露节省、客服自动化率或单次成本 | 同左 | 未证 |

来源：([DoorDash Q4 2025 电话会](https://www.fool.com/earnings/call-transcripts/2026/02/18/doordash-dash-q4-2025-earnings-call-transcript/); [DoorDash Q2 2026](https://www.sec.gov/Archives/edgar/data/1792789/000179278926000048/proddashex991-pressrelease.htm); [DoorDash Q3 2026 电话会](https://www.fool.com/earnings/call-transcripts/2026/10/29/doordash-dash-q3-2026-earnings-call-transcript/); [华尔街见闻](https://wallstreetcn.com/articles/3778095); [美团 Q2 2026 电话会](https://www.investing.com/news/transcripts/earnings-call-transcript-meituan-posts-q2-2026-profit-as-revenue-growth-accelerates-93CH-4881117))。

DoorDash 自己说得很清楚："找到用 AI 的办法"和"找到有成效地用 AI 的办法"是两回事 ([DoorDash Q2 2026](https://www.sec.gov/Archives/edgar/data/1792789/000179278926000048/proddashex991-pressrelease.htm))。

**单位经济（估算与计算）**

| 项目 | DoorDash | 美团 |
|---|---|---|
| 客服节省（估算） | 每单 $0.03–0.11 × 年化约 39.7 亿单 ≈ 每年 1.2–4.3 亿美元，约为年化 Adj. EBITDA 的 3%–10% | 未披露接触率和单次成本；人工更便宜，绝对额更小 |
| 费用率检验（计算） | Q3 研发同比 +41%，快于订单 +28%；研发占 GOV 1.5%（Q2 为 1.6%）；G&A 占 GOV 1.3%（上年 1.6%） | Q2 研发同比 +22.5%，快于收入 +14.4%；研发占收入 7.3%；G&A 3.1%，持平 |
| 管理层表态 | 没有给出 AI 节省目标；用 token 路由控制推理成本 | AI 投入按 ROI 评估，"非常审慎"；王莆中 2025-10 称 AI 投入已达"百亿人民币级"，"Token 确实很贵" |

客服节省的假设：每单 2%–5% 的接触率，AI 解决 50%–70%，人工每次 $3.13（每小时全成本 $25、处理 8 次），AI 每次 $0.05。全部是假设，同行都没有披露这些数字。来源：([DoorDash Q3 2026](https://www.sec.gov/Archives/edgar/data/1792789/000179278926000062/proddashex991-pressrelease.htm); [DoorDash Q2 2026](https://www.sec.gov/Archives/edgar/data/1792789/000179278926000048/proddashex991-pressrelease.htm); [美团 Q2 2026](https://www1.hkexnews.hk/listedco/listconews/sehk/2026/0828/2026082800435.pdf); [36氪](https://www.36kr.com/p/3512515121077381))。

同行给了两个参照。Uber 说 95% 的工程师每月用 AI 编程工具，10% 以上的代码由 AI Agent 自主写成，"让我们放缓招聘节奏" ([Uber Q1 2026](https://s23.q4cdn.com/407969754/files/doc_earnings/2026/q1/transcript/Uber-Q1-26-Prepared-Remarks.pdf))。Klarna 是反例：每笔交易的客服成本从 $0.32 降到 $0.19 ([CX Dive](https://www.customerexperiencedive.com/news/klarna-credits-ai-slash-customer-service-costs/748647/))，但 2025 Q3 客服和运营总成本反而从 4,200 万美元升到 5,000 万美元，之后又重新招了人工 ([Pure AI](https://pureai.com/Articles/2025/10/22/Why-Klarnas-AI-Experiment-Matters.aspx))。

**判断：** ① 成立。② 成立。③ 未证。成立的条件是总成本下降，而不只是单位成本下降；员工增速低于订单增速；并且披露出来。目前两家的研发费用都比业务长得快，节省被 AI 投入本身盖住了。S5 还是 S3 和 S6 的试验场：美团把内部的 CatPaw 开给了商户和企业。

## 10. S6 对外输出 AI 能力：卖 token 不成立，卖"原子"成立

| 要素 | DoorDash | 美团 | 成立吗 |
|---|---|---|---|
| ① 价值主张 | 给本地商户"大零售商的能力"：AI 网站、零售媒体、仓配、自主配送 | 给开发者便宜的大模型；给企业可私有化的 Agent 平台 | 弱 |
| ② 创造与传递 | Commerce Platform、Symbiosys、DashMart Fulfillment Services；Instacart 待并入 | LongCat 开源权重和 API；CatPaw 企业版；低空航网对伙伴开放 | 部分：出了自家网络就没有独有资源 |
| ③ 价值获取 | 没有独立的 AI 产品；靠 Drive 按单费、软件订阅和媒体花费 | 没有披露 AI 收入；"我们不会成为 Token 工厂" | 卖 token 不成立；卖网络服务成立 |

DoorDash 不单独卖 AI，它把 AI 嵌在网络服务里卖。Drive 按单向商户收配送费，占 Commerce Platform 收入的大部分 ([DoorDash 10-Q](https://www.sec.gov/Archives/edgar/data/1792789/000179278926000070/dash-20260930.htm))；数字点单服务有 15 万+ 商户，Q2 收入同比 +40% 以上 ([DoorDash Q2 2026](https://www.sec.gov/Archives/edgar/data/1792789/000179278926000048/proddashex991-pressrelease.htm))。Symbiosys 为零售商做站内外商业媒体 ([DoorDash Ads](https://ir.doordash.com/news/news-details/2026/DoorDash-Ads-Becomes-a-Global-Commerce-Media-Platform/default.aspx))。收购 Instacart 后，"更多杂货商可以用 Instacart 的技术运营自己的网站和零售媒体网络"，并经 DashMart Fulfillment Services 扩充产能；公司预计第三年起每年净协同"5 亿美元或更多" ([交易公告](https://www.sec.gov/Archives/edgar/data/1792789/000114036126041111/ef20084547_ex99-1.htm))。Instacart 的财报已把"AI Solutions"列为产品线 ([Instacart Q3 2026](https://www.sec.gov/Archives/edgar/data/0001579091/000119312526437014/d24470dex991.htm))。

美团在卖 token，但价格说明它不想靠这个赚钱。王兴 08-28 说："我们不会成为 Token 工厂，我们的模型和 AI 产品将用于支持我们的主业" ([界面](https://m.jiemian.com/article/15022608.html))。美团目前唯一量化过的 AI 相关收益来自持股：Q2 智谱等股权的公允价值收益约 222 亿元，计入其他综合收益，不进利润表 ([美团 Q2 2026](https://www1.hkexnews.hk/listedco/listconews/sehk/2026/0828/2026082800435.pdf))。

**单位经济（估算）**

| 项目 | 数值 | 读法 |
|---|---|---|
| LongCat-2.0 促销价 | 输入 ¥2、输出 ¥8 每百万 token；¥9.9 买 5,000 万 token（约 ¥0.2/百万）；¥399 买 10 亿 token | 1.6 万亿参数的模型，标价与 DeepSeek Flash 高峰价（约 ¥2.1 / ¥8.5）几乎相同：是获客价，不是利润价 |
| 客服 Agent SaaS 参照 | Intercom Fin 每次解决 $0.99；自建推理约 $0.05/次 | 毛利约 95%（未含人工复核）：卖结果的 SaaS 毛利可以很高 |
| Zomato Nugget | 2025-02 对外发布的客服平台；自用每月 1,500 万次对话，厂商案例称解决率 85%、客服成本降 55% | 外卖平台对外卖 AI 最清楚的先例；价格未公开 |

来源：([80AJ](https://www.80aj.com/2026/06/30/meitongcat-ai-model-9-9/); [DeepSeek](https://api-docs.deepseek.com/quick_start/pricing); [Macha](https://www.getmacha.com/blog/intercom-fin-pricing); [Business Standard](https://www.business-standard.com/companies/news/zomato-launches-ai-customer-support-platform-nugget-125021700734_1.html); [MongoDB](https://www.mongodb.com/company/blog/innovation/zomato-ai-solution-nugget-reduces-support-costs))。

**判断：** ① 弱。② 部分成立。③ 卖 token 不成立，卖与自家网络绑定的服务成立。7 Powers 的读法是：出了自家网络，平台就没有独有资源，只能和云厂商、POS 厂商拼产品。DoorDash 的路线（履约、媒体、仓配、商户软件）和美团的低空航网，卖的都是"原子加软件"。美团的 LongCat API 和开源权重更像生态投入：争取开发者，也给适配推理的国产芯片伙伴送价值（推断）。

## 11. 横向比较：分水岭是 Agent 碰不碰付钱的那一层

### 11.1 九个条件打分

依据见第 5–10 节。

| 条件 | S1 自有 App | S2 第三方调用 | S3 商户 | S4 履约 | S5 内部 | S6 对外 |
|---|---|---|---|---|---|---|
| C1 能验收 | 是：订单送达、订位成功；推荐好坏难验收 | 是：同 S1，但意图由别人的 Agent 解读 | 部分：广告 ROI 可算，诊断建议难验收 | 是：每单成本、时效 | 部分：代码和工单可数，净节省难测 | 部分：按次、按 token 可计量 |
| C2 有人买单 | 部分：消费者不付，靠原有抽佣池 | 否：没人为 Agent 付钱，平台自掏优惠券 | DoorDash 是（广告预算）；美团部分（免费） | 是：平台自己的履约成本 | 是：内部预算 | 部分：美团低价卖 token；DoorDash 没有独立产品 |
| C3 可托付 | 部分：下单前确认，平台兜底退款 | 部分：AI 专属卡、ACT、SPT 有限额和逐笔确认；出错时责任难分 | 部分：改价要人确认；已有自动改价亏钱的案例 | 部分：安全和劳动监管约束 | 是 | 部分 |
| C4 上下文与身份 | 是：账户、历史、实时库存 | 是（在平台侧）：账户绑定或 A2A；Agent 只拿到意图 | 部分：有平台店铺数据，POS 和电话在别人手里 | 是 | 是 | 否：出了自家网络没有独有数据 |
| C5 渠道靠得住 | 是；Text DoorDash 借用 Apple 信息 | DoorDash 部分：OpenAI 撤回结账，Alexa+ 选了对手；美团部分：腾讯是盟友，最大的两个入口是对手 | 是：自有商家后台 | 是 | 是 | 部分 |
| C6 单任务成本可控 | 是，前提是路由到小模型 | 是：推理由对方付；成本在通道费和优惠券 | 是：每商户每月推理约 $0.2–1.2（估算） | 否（尚未）：资本密集，取决于利用率 | 部分：研发费用快于业务增长 | DoorDash 不适用；美团否：促销价约等于 Flash 档 |
| C7 计价跟着成本走 | 部分：收入按单，成本按会话 | 否：通道费按 GOV 收 | DoorDash 是：按广告花费；美团否：免费 | 不适用（成本项） | 不适用 | 是：按 token、按次 |
| C8 看得见、封得住 | 是：逐单确认，价格可见 | 部分：AI 专属卡隔离余额、逐笔确认 | 部分：ROI 下限、审批流 | 是（内部） | 是（内部） | 是 |
| C9 毛利守得住 | 未证：取决于广告挤占 | 否：4% 已超过 DoorDash 每单 EBITDA | DoorDash 是；美团未证 | 未证 | 未证 | 否（token）；是（履约加软件） |
| **总体** | **机制成立，账未证** | **作获客渠道成立，作收费站不成立** | **靠广告成立，单卖 SaaS 未证** | **杠杆最大，规模未到** | **采用成立，省钱未证** | **卖 token 不成立，卖网络服务成立** |

SPT 指 Stripe 的 Shared Payment Token，它带用量上限和有效期，不向 Agent 或商户暴露卡号 ([Stripe](https://docs.stripe.com/agentic-commerce/concepts/shared-payment-tokens))。

**读法：** 成立的场景里，Agent 都在给付钱的那一层加东西：S3 加广告花费，S4、S5 减成本。未证或不成立的场景里，Agent 都在削付钱的那一层：S1 可能挤掉广告，S2 可能被入口抽成，S6 卖的东西本身不赚钱。

### 11.2 五个框架怎么看六个场景

| 框架 | S1 | S2 | S3 | S4 | S5 | S6 |
|---|---|---|---|---|---|---|
| 单位经济 | 小模型推理约占每单利润 2%；广告挤占 16%–32% | 4% 通道费超过每单 EBITDA | 推理每月几美元以内；回收靠广告 | 每单省 $0.10 = 每季 9,930 万美元 | 客服每单省 $0.03–0.11 | token 促销价约等于 Flash 档 |
| 聚合理论 | 平台握住需求界面 | 需求界面让给三级聚合者，履约留下 | — | 履约是二级聚合者的成本，也是护城河 | — | 出了自家网络就不是聚合者 |
| 7 Powers | 独有供给信息；反定位对在位者不利 | 独有资源加流程能力守住履约 | 商户转换成本 | 规模经济加流程能力 | 流程能力 | 缺独有资源 |
| JTBD/画布 | 复购和有约束的决策 | 企业订餐、桌面用户买团购 | 替代代运营和电话人工 | 骑手：收入确定、安全 | 工程和客服 | 商户要"大零售商的能力" |
| 五力/价值链 | 发现层的广告最脆弱 | 新增强势买方 | 供应方议价，写权限在 POS | 竞争把节省让给消费者 | — | 与云厂商、POS 厂商竞争 |

### 11.3 DoorDash 与美团

| 维度 | DoorDash | 美团 |
|---|---|---|
| 每单利润 | Adj. EBITDA $1.07，CP $1.79（Q3 2026，计算） | 外卖约 −¥0.3 到 ¥1.5（券商估算），2026 Q2 刚转正 |
| 广告 | 不单独披露；正在补回消费者费用率的下降 | 2025 年 515 亿元；2024 年约等于 CLC 经营利润的 93%（计算） |
| 最厚的利润池 | 很可能是广告（推断：未单独披露，同行的广告约等于利润）；CFO 预计杂货零售 2026 下半年单位经济转正 | 到店酒旅：分部利润率约 25%（券商估算） |
| 外部 Agent 格局 | 中立：OpenAI、Google、Amazon；累计"几百单" | 生态绑定：豆包导向抖音，千问导向淘宝闪购，腾讯请美团进来 |
| 对外部 Agent 的策略 | 自建场外 Agent（Text DoorDash、CLI、企业 MCP）；账户和结账留在 DoorDash | 与腾讯结盟（元宝 A2A、WorkBuddy 团购、微信 AI 生态）；拒绝豆包 GUI 自动化 |
| 模型 | 多家外部模型加自建 token 路由 | 自研 LongCat-2.0 加开源模型；国产算力；"不做 Token 工厂" |
| 商户 Agent | Smart Campaigns 变现广告；SevenRooms、DashOS 软件 | 智能掌柜 130 万+ 商户；AI店长免费 |
| 履约自动化 | Dot、DoorDash Air、合作 AV；ADP 调度 | 无人机 100 万+ 单（其中 81 万+ 为检测样本和药品）；骑手安全 AI |
| 成本压力 | 最低收入标准（纽约 $22.13/小时）；自主配送投入压 Q4 利润 | 新职伤由平台全额缴纳；养老补贴 |
| 财务底子 | Q3 自由现金流 7.87 亿美元；收购 Instacart 用过桥贷款 | 2025 年 IFRS 亏损 234 亿元；Q2 末现金 1,047 亿元加短期理财 636 亿元 |

来源：见第 2、5–10 节；CFO 对杂货零售的预期见 ([DoorDash Q4 2025 电话会](https://www.fool.com/earnings/call-transcripts/2026/02/18/doordash-dash-q4-2025-earnings-call-transcript/))。

## 12. 回答：三要素在什么条件下成立

### 12.1 价值主张：选对活，交付可验收，委托停在确认之前

第一，活要选对。复购、补货、有约束的决策（预算、饮食、团餐、多店）和到店订位，适合交给 Agent；冲动浏览不适合。DoorDash 最硬的证据都在杂货和找新店上，美团小团 2.0 的重点是订位和外呼。第二，交付物要能验收：送达的订单、订到的位、算得出 ROI 的广告、每单配送成本。推荐好不好难验收，所以要有下单前确认。第三，委托停在 L1–L2：组好购物车一键确认，或按规则复购。消费者调查和中国的支付公约都指向这里。第四，对商户，Agent 要替代一笔已经在付的钱（代运营、电话人工、佣金），而且建议要和商户利润一致，否则免费也会被关掉。对第三方 Agent 而言，消费者价值主张只在新场景里成立：企业订餐、桌面用户买团购。

### 12.2 价值创造与传递：握住原子，账户和结账留在自己一侧

第一，握住"原子"：实时供给信息、履约网络和售后。这是 7 Powers 里的独有资源、流程能力和规模经济，通用 Agent 复制不了。第二，账户、结账、会员留在平台一侧。每个已上线的第三方集成都如此。宁可让自己的 Agent 被调用（Text DoorDash、小美 A2A），也不只给一个裸 API。第三，模型可路由、成本自控：DoorDash 用 token 路由，美团用自研 LongCat 和国产算力。第四，入口要么自有，要么属于没有履约网络的盟友（腾讯之于美团）；对未经许可的 GUI 自动化说不。

### 12.3 价值获取：不碰付钱的那一层，或者让它变大

| 场景 | 成立条件 | 现状 |
|---|---|---|
| S1 自有 App | 对照组测出的真增量超过门槛（DoorDash 小模型下 3%–22%）；Agent 原生广告补上被压缩的广告位，并标注清楚；错误率在 0.1% 量级 | 两家都没披露增量和 Agent 内广告 |
| S2 第三方调用 | 通道费只按获客成本付，只付新客首单；复购拉回自有入口；费率低于每单利润率（DoorDash 转移单约 3.1%，丢广告时约 1.1%，增量单约 5.2%；美团外卖约 2%–3%） | 双方都没披露费用；美团用自己的优惠券买流量 |
| S3 商户 | 靠广告预算或被替代的支出变现；ROI 对商户可见，由商户设下限 | DoorDash 已成立；美团间接 |
| S4 履约 | 利用率过门槛；省下的钱在竞争中留得住 | 规模未到，投入在压利润 |
| S5 内部 | 总成本下降，而不只是单位成本下降 | 研发费用增速快于业务 |
| S6 对外 | 卖与自家网络绑定的服务，不卖 token | DoorDash 在做；美团的 token 是获客价 |

### 12.4 与通用 Agent 的对照：一个怕算力被吃掉，一个怕广告被吃掉

| | 通用 Agent（Claude Tag、Grok Bot、WorkBuddy） | 外卖平台 Agent（DoorDash、美团） |
|---|---|---|
| 谁为 Agent 付钱 | 组织或个人，按订阅或用量 | 没人直接付；从原有抽佣、广告、会员里收回 |
| 主要成本 | 推理和算力，随自主运行时长上涨 | 被挤掉的广告、通道费、补贴、错误赔付；推理是小头（路由到小模型时） |
| 最难的条件 | C7 计价跟着成本走、C8 封得住 | C9 毛利守得住、C5 入口归谁 |
| 典型失败 | Grok Bot 把 always-on Agent 放进平价额度池，2026-10-31 至 11-01 限流，Musk 承认成本"相对收入高得不可持续" | OpenAI Instant Checkout 收 4% 通道费，商户和转化都没跟上，2026-03 撤回 |
| 账在哪里结 | 云和 token | 抽佣、广告、履约成本 |

来源：([TechCrunch（Yahoo 转载）](https://finance.yahoo.com/technology/ai/articles/elon-musk-throttles-grok-bot-162321608.html); [Modern Retail](https://www.modernretail.co/technology/what-went-wrong-with-chatgpts-instant-checkout/))。

两类 Agent 指向同一条规律：价值获取成立，要求付钱的那一层既归你，又不被 Agent 吃掉。通用 Agent 的风险是 Agent 吃掉算力；外卖 Agent 的风险是 Agent 吃掉广告，或者让入口主人分走抽佣。

## 13. 含义：Agent 渠道会被定价成获客成本，最好的防守是做被调用的那个 Agent

第一，Agent 渠道会被定价成获客成本，而不是收费站。Xu 把通用 Agent 比作当年的 Facebook 和 Google，Uber 用元搜索作类比。中国已经先走一步：豆包把导向抖音的酒店订单单列为约 12% 的渠道费率 ([界面](https://m.jiemian.com/article/14902227.html))；中金认为，若微信 Agent 成为服务分发的中心入口，微信可以抬高小程序交易的有效佣金 ([界面](https://m.jiemian.com/article/14842082.html))。接下来很可能出现单独的"Agent 来源订单"费率档，平台只为首单付钱。

第二，广告会被搬进 Agent，但在中国更难。Amazon 的 Rufus 品牌提示已经是一种 Agent 原生广告位，约 20% 互动过的用户会继续品牌对话 ([Modern Retail](https://www.modernretail.co/technology/amazon-says-its-ai-shopping-assistant-is-gaining-traction-with-rufus-users-up-115/))。DoorDash 有空间试；美团要面对价格规则和"AI 推贵的"这类信任问题，更依赖频次、会员和商户工具。

第三，最好的防守是做被调用的那个 Agent。Text DoorDash 在 Apple 的渠道里跑 DoorDash 自己的 Agent；小美在元宝里以 A2A 被调用。王兴把服务 Agent（To A）列为与 To C、To B 并列的第三类客户 ([虎嗅](https://www.huxiu.com/article/4863738.html))。被调用的是自己的 Agent，身份、优惠券、数据和售后就留在自己一侧。

第四，集中度就是对 Agent 的议价力，反垄断是它的对冲。DoorDash–Instacart 和 Uber–Delivery Hero 让美国的履约网变少，Agent 更难货比三家。监管也在看：据报道，市场监管总局 2026-01 启动外卖平台竞争状况调查，04-17 在"幽灵外卖"系列案中对美团罚款 7.4 亿元 ([IT之家](https://www.ithome.com/0/940/649.htm))。

接下来值得看的信号：

| 时间 | 事件 | 看什么 |
|---|---|---|
| 2026-11 下旬 | 美团 Q3 2026 财报 | 外卖 UE 是否仍为正；小团是否首次披露订单或 GTV；回答里是否有广告；元宝×小美是否上线 |
| 2027-01-01 | Gemini 3.8 Flash 标价翻倍 | 中档模型路由的成本冲击 |
| 2027-02（按往年节奏） | DoorDash Q4 2026 财报 | Ask、Text DoorDash 的使用量；Agent 内广告；企业 MCP 定价 |
| 待定 | 支付清算协会的自主支付备案 | 第一批"预设条件自主支付"应用是谁 |
| 待定 | 千问点外卖补贴退坡后 | 生态 Agent 没有免单时能否留住用户 |
| 待定 | Square 在 ChatGPT、Claude 的免佣点单 | 替代品是否有量 |
| 2027 年底至 2028 上半年 | DoorDash–Instacart 交割与反垄断审查 | 履约网络集中度和对 Agent 的议价力 |

## 14. 局限与注意事项

1. **没有 Agent 级财务数据。** 两家都没披露 Ask、Text DoorDash、小团、小美的订单、增量 GTV、推理成本或回答里的广告负载。第三方集成的费用条款也都没披露。价值获取的判断依据是每单经济、价目表和管理层表态。
2. **单位经济是估算。** token 价格来自官方价目表；会话 token 量、成单率、广告占 GOV 2%、挤占比例、错误成本、美团每单 ¥1、客单价 ¥30–50、日均 7,000 万单、$1 ≈ ¥7.1 都是假设。换假设，门槛会变；但只要挤占比例超过约 3%、推理路由到小模型，"广告损失大于推理费"的排序不变。
3. **公司指标口径不一。** Ask 的"篮子 +50%"有选择偏差；小团的数字是曝光和回答次数；千问的 1.2 亿单发生在补贴期；DoorDash 的"几百单"可能不含它自己的 ChatGPT App。
4. **美团数据有冲突。** 外卖日单量有 6,500 万和 8,500 万两种说法；每单 UE 全部来自券商或专家；2026 年起佣金和广告合并披露。到店酒旅的利润率是券商估算。
5. **时间和转录。** 美团 Q3 未发布。DoorDash 电话会的 Motley Fool 文字稿部分由机器生成，引语以网播为准。
6. **单一来源或未核实。** OpenAI 的 4% 通道费来自媒体报道，且已撤回；Citrini 的 7% 和约 25% 没有与原文核对；Walmart"新客 2 倍"是单一来源；元宝×小美未见上线；Ask 的模型供应商来自付费墙报道的转述；DashPass 等会员权益在第三方 Agent 订单里是否适用，没有一手披露。
7. **框架应用是本研究的映射。** 没找到把 7 Powers、JTBD 或价值主张画布用于外卖平台 Agent 的公开分析。McKinsey、Bain、BCG 的报告原文未能获取，引用依据新闻稿和转述。各家对 2030 年 Agent 电商规模的预测针对零售商品，McKinsey 的全球数字明确不含服务；套到外卖和本地生活上属于外推。

## 来源

- 框架：张敬伟、王迎军（2010）<https://qks.shufe.edu.cn/J/ArticleQuery/A160824002159/CN>；Teece（2010）<https://www.kth.se/social/upload/528c4881f2765458b97a9368/Teece%20%282010%29.pdf>；Thompson <https://stratechery.com/2017/defining-aggregators/>；Winters <https://www.caseyaccidental.com/p/when-agents-attack-how-ai-collapses>；Helmer <https://7powers.com/>；JTBD <https://hbr.org/2016/09/know-your-customers-jobs-to-be-done>；价值主张画布 <https://www.strategyzer.com/library/the-value-proposition-canvas>；五力 <https://hbr.org/2008/01/the-five-competitive-forces-that-shape-strategy>
- DoorDash：Q3 2026 新闻稿 <https://www.sec.gov/Archives/edgar/data/1792789/000179278926000062/proddashex991-pressrelease.htm>；Q3 2026 10-Q <https://www.sec.gov/Archives/edgar/data/1792789/000179278926000070/dash-20260930.htm>；Q2 2026 新闻稿 <https://www.sec.gov/Archives/edgar/data/1792789/000179278926000048/proddashex991-pressrelease.htm>；Q4 2025 新闻稿与致股东信 <https://www.sec.gov/Archives/edgar/data/1792789/000179278926000012/prodq4dashex991-pressrelea.htm>、<https://www.sec.gov/Archives/edgar/data/1792789/000179278926000012/proddashq4ex992-shareholde.htm>；电话会 <https://www.fool.com/earnings/call-transcripts/2026/02/18/doordash-dash-q4-2025-earnings-call-transcript/>、<https://www.fool.com/earnings/call-transcripts/2026/10/29/doordash-dash-q3-2026-earnings-call-transcript/>；Instacart 交易 <https://www.sec.gov/Archives/edgar/data/1792789/000114036126041111/ef20084547_ex99-1.htm>；Text DoorDash <https://about.doordash.com/en-us/news/text-doordash>；Dash Forward 2026 <https://about.doordash.com/en-us/news/dash-forward-2026>；Ask 工程架构 <https://www.infoq.com/news/2026/07/doordash-ai-ask-assistant/>；商户 AI 工具 <https://about.doordash.com/en-us/news/ai-powered-merchant-tools>；DoorDash Ads <https://ir.doordash.com/news/news-details/2026/DoorDash-Ads-Becomes-a-Global-Commerce-Media-Platform/default.aspx>；第三方 Agent 订单 <https://techcrunch.com/2026/10/30/doordash-said-its-only-received-several-hundred-orders-from-ai-agents/>
- 美团：2025 年报 <https://www1.hkexnews.hk/listedco/listconews/sehk/2026/0326/2026032600964.pdf>；Q2 2026 业绩 <https://www1.hkexnews.hk/listedco/listconews/sehk/2026/0828/2026082800435.pdf>；Q2 2026 电话会 <https://www.investing.com/news/transcripts/earnings-call-transcript-meituan-posts-q2-2026-profit-as-revenue-growth-accelerates-93CH-4881117>；"不做 Token 工厂" <https://m.jiemian.com/article/15022608.html>；王兴管理层会议 <https://tech.ifeng.com/c/8rShVTkbSYO>；小团 2.0 <https://stock.10jqka.com.cn/20260727/c678455867.shtml>；商户 AI <https://www.news.cn/tech/20260909/79161147f2414bc79664a28d32802f8e/c.html>、<https://www.leiphone.com/category/industrynews/tZI7pYBhiFduVfr6.html>；无人机 <https://www.ithome.com/0/980/150.htm>；LongCat 定价 <https://www.80aj.com/2026/06/30/meitongcat-ai-model-9-9/>；券商估算 <https://wallstreetcn.com/articles/3773639>、<https://m.sohu.com/a/1007728090_613239/>
- 渠道与协议：OpenAI 撤回 Instant Checkout <https://www.modernretail.co/technology/what-went-wrong-with-chatgpts-instant-checkout/>；4% 通道费 <https://www.shopifreaks.com/shopify-confirms-a-4-fee-on-sales-made-through-chatgpt-but-whats-the-real-cost/>；Walmart 转化 <https://martech.org/walmart-says-chatgpt-checkout-converted-3x-worse-than-its-own-website/>；Gemini 自动化 <https://techcrunch.com/2026/02/25/gemini-can-now-automate-some-multi-step-tasks-on-android>；Alexa+ <https://www.techcrunch.com/2026/03/31/alexa-plus-new-food-ordering-experiences-with-uber-eats-and-grubhub/>；UCP <https://www.technobezz.com/news/google-launches-universal-commerce-protocol-for-ai-shopping-2026-01-11-jkpd>；Stripe SPT <https://docs.stripe.com/agentic-commerce/concepts/shared-payment-tokens>；Square 免佣点单 <https://venturebeat.com/marketing-tech/restaurants-can-now-accept-orders-placed-directly-from-chatgpt-and-claude-thanks-to-squares-new-low-fee-no-setup-integration/>；Amazon v. Perplexity <https://engadget.com/2230471/perplexity-has-successfully-overturned-amazon-injunction-on-its-ai-shopping-bot>；Citrini 情景 <https://stocktwits.com/news-articles/markets/equity/doordash-moat-under-threat-citrini-research-warns-consumer-ai-agents-bypass-delivery-apps/cZRvn0mR4zK>
- 中国生态与监管：千问点外卖 <https://www.stdaily.com/web/gdxw/2026-01/15/content_461939.html>、<https://www.yzwb.net/news/ch/202602/t20260212_321578.html>；AI 专属卡与 WorkBuddy <https://www.qbitai.com/2026/06/436160.html>、<https://www.nbd.com.cn/articles/2026-06-17/4429249.html>；元宝×小美 <https://m.jiemian.com/article/14525559.html>；豆包拒绝名单 <https://news.bjd.com.cn/2026/10/15/11996628.shtml>；豆包酒店渠道费 <https://m.jiemian.com/article/14902227.html>；支付清算协会公约 <https://www.news.cn/fortune/20260824/b44c884bab5d4f89bce4262c362b0736/c.html>、<https://www.yicai.com/news/103331509.html>；价格规则 <https://www.news.cn/tech/20260410/c5186b8634a74aa089bb632c7a4d6ed0/c.html>；外卖平台标准 <https://www.yicai.com/news/102948456.html>；上海消保委调查 <https://finance.eastmoney.com/a/202606263784809881.html>
- 商户 Agent：价格基准 <https://www.katalystos.com/blog/ai-phone-answering-for-restaurants-what-it-costs>；Toast <https://www.fool.com/earnings/call-transcripts/2026/08/11/toast-tost-q2-2026-earnings-call-transcript/>；Square Managerbot <https://squareup.com/us/en/press/managerbot-open-beta>；Owner <https://www.owner.com/pricing>；NRA 调查 <https://www.restaurantdive.com/news/national-restaurant-assocation-operator-artificial-intelligence-adoption/812418/>；"免费的才是最贵的" <https://mp.ofweek.com/Internet/a056714478607>
- 基准与价目：Anthropic <https://platform.claude.com/docs/en/about-claude/pricing>；Google <https://aistudio.google.com/docs/pricing>；DeepSeek <https://api-docs.deepseek.com/quick_start/pricing>；Instacart <https://www.sec.gov/Archives/edgar/data/0001579091/000119312526437014/d24470dex991.htm>；Uber <https://www.sec.gov/Archives/edgar/data/0001543151/000154315126000027/uberq226earningspressrelea.htm>、<https://s23.q4cdn.com/407969754/files/doc_earnings/2026/q1/transcript/Uber-Q1-26-Prepared-Remarks.pdf>；Grab <https://www.sec.gov/Archives/edgar/data/0001855612/000185561226000148/a2026q3-earningspressrelea.htm>；Klarna <https://www.customerexperiencedive.com/news/klarna-credits-ai-slash-customer-service-costs/748647/>、<https://pureai.com/Articles/2025/10/22/Why-Klarnas-AI-Experiment-Matters.aspx>；Serve Robotics <https://www.boardroomalpha.com/sec/serv-8-k-2026-03-11-0001832483-26-000004/finalserve_investorxdeck.htm>；消费者信任 <https://www.artificialintelligence-news.com/news/ai-shopping-agents-consumer-trust-accenture-report/>、<https://www.bain.com/about/media-center/press-releases/20252/agentic-ai-poised-to-disrupt-retail-even-with-50-of-consumers-cautious-of-fully-autonomous-purchasesbain--company/>
