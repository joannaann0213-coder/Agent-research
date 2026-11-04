# 研究员交回的摘要（原文保留）

> 分析框架与单位经济这一路的研究员没能直接写入笔记文件，完整笔记由协调方从它的交回内容中原样存为 ../analysis_toolkit_unit_economics.md。下面是交回内容开头的摘要和结尾的附注，原文保留，以免信息丢失。结尾附注提到的临时目录文件，已经打包私下发给仓库所有者，没有提交到本仓库。

## 开头摘要

I couldn't save the notes to `/home/user/Agent-research/research_notes/DoorDash 美团 Agent 商业模式三要素/analysis_toolkit_unit_economics.md`: the harness blocks subagents from writing report files. The full notes are below, in the required Takeaway / Cited Findings / Inferences / Gaps format, ready to save to that path. Every figure is dated, "Est." marks my own computations with assumptions shown, and DoorDash and Meituan's own initiatives are left out.

Main findings:
- **Thin profit pools.** Peer delivery profit is about 2.7–5.5% of order value: Grab 2.7%, Swiggy 3.4%, Uber Delivery 3.8%, Zomato 5.5%, Instacart 3.2% (about $3.74 per order). Delivery Hero was about 1.4% in FY2025.
- **Ads are as big as profit.** Ads run about 2–3.1% of order value, roughly the whole segment profit. That makes ad displacement the biggest profit risk.
- **Inference cost depends on the model.** My estimate for a typical ordering session is about $0.007–$0.03 on small models and $0.14–$0.39 on top-tier models. Routing requests to the right model decides whether the agent makes money in low-order-value markets. Google's Gemini 3.8 Flash list price doubles on 2027-01-01, so prices can rise as well as fall.
- **A 4% channel fee (OpenAI's precedent) wipes out the profit on orders that merely shift channel.** It leaves truly new orders profitable at about half margin.
- **Own-app agents have the strongest disclosed results:** Amazon Rufus (300M+ users, about $12B incremental sales) and Walmart Sparky (about 35% higher spend per order). Checkout inside ChatGPT failed: Walmart saw about one-third the conversion of its own site, and OpenAI scaled the feature back in March 2026.
- **Subsidies drove adoption in China.** 千问 completed 1.2亿 AI orders in 6 days during a ¥3B free-order campaign; I found no post-subsidy retention data.
- **Consumers accept narrow delegation, not autonomy.** Accenture found 74% would delegate routine tasks but only 9% would let an agent buy on its own. Only 16% of Shanghai shoppers say AI matches products precisely.
- **Gaps.** No company discloses agent incrementality, error rates or inference cost. I could not open the full McKinsey, Bain or BCG reports. I found no published 7 Powers application to delivery agents, so that mapping is my own. Several Chinese-language claims rest on search summaries and are labeled as such.

---


## 结尾附注

Supporting files: the inference-cost calculation script is at `/tmp/claude-0/-home-user-Agent-research/02dfe960-a6a4-571c-9568-6410ec9e0e50/scratchpad/calc/infer.py`. Text extracted from Uber's Q1 2026 prepared remarks and Swiggy's Q2 FY27 press release is at `/tmp/claude-0/-home-user-Agent-research/02dfe960-a6a4-571c-9568-6410ec9e0e50/scratchpad/pdfs/uber_q1_26.txt` and `/tmp/claude-0/-home-user-Agent-research/02dfe960-a6a4-571c-9568-6410ec9e0e50/scratchpad/pdfs/swiggy_q2fy27_pr.txt`.
