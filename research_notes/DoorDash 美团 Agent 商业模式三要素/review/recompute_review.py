#!/usr/bin/env python3
"""Recompute every derived figure in reports/DoorDash 美团 Agent 商业模式三要素.md
and run the sensitivity tests used in review/analysis_and_calculations.md.

Inputs come from the report itself and from the research notes
(doordash.md, meituan.md, analysis_toolkit_unit_economics.md,
merchant_agent_market.md, working_files/inference_cost_estimate.py).
Model prices were checked on 2026-11-04 (see the findings, section 2):
Google, OpenAI and DeepSeek on their official pages; Claude from the Anthropic
reference supplied by the coordinator (cached 2026-10-06), not re-fetched.
Standard library only. Run: python3 recompute_review.py
"""

FX = 7.1  # report's assumption: $1 = RMB 7.1

# ---------------------------------------------------------------------------
# 1. Inputs
# ---------------------------------------------------------------------------
# DoorDash quarterly KPIs (doordash.md Q0 table; official Q3 2026 release)
DD_Q = {  # orders, Marketplace GOV, revenue, Contribution Profit, Adj. EBITDA
    "Q3'25": (776e6, 25.0e9, 3.45e9, 1.27e9, 754e6),
    "Q4'25": (903e6, 29.7e9, 3.96e9, 1.41e9, 780e6),
    "Q1'26": (933e6, 31.6e9, 4.04e9, 1.38e9, 754e6),
    "Q2'26": (970e6, 33.1e9, 4.45e9, 1.64e9, 914e6),
    "Q3'26": (993e6, 33.9e9, 4.62e9, 1.78e9, 1060e6),
}
DD_NI_Q3 = 466e6
DD_Q3_DAYS = 92
# The report's own rounded per-order inputs (used to check its arithmetic)
AOV, CP, EBITDA = 34.2, 1.79, 1.07
TAU = 0.136  # net revenue margin, used as a conservative take on extra basket

# Meituan CLC (meituan.md Q1; RMB bn)
MT = dict(
    rev24=250.25, op24=52.42, ads24=48.84,
    rev25=260.83, op25=-6.90, comm25=99.23, deliv25=96.07, ads25=51.46, other25=14.06,
    rev_q3_25=67.4, op_q3_25=-14.1, rev_q1_26=64.06, op_q1_26=-2.03,
    rev_q2_26=71.53, op_q2_26=5.67,
    ms_q2_fd=0.313, ms_q2_flash=-0.75, ms_q2_iht=4.3,  # Morgan Stanley Q2'26 forecasts
)
Q2_DAYS = 91

# Model list prices, USD per 1M tokens: (input, cached input, output)
P = {
    "Claude Haiku 5.5":               (0.10, 0.01, 0.50),  # cache read not in Anthropic ref; assumed
    "DeepSeek Flash peak":            (0.30, 0.006, 1.20),
    "DeepSeek Flash off-peak":        (0.15, 0.003, 0.60),
    "Gemini 3.5 Flash-Lite":          (0.30, 0.03, 2.50),
    "Gemini 3.8 Flash 2026":          (0.75, 0.075, 3.75),
    "Gemini 3.8 Flash 2027":          (1.50, 0.15, 7.50),
    "GPT-5.4 mini":                   (0.75, 0.075, 4.50),
    "Claude Sonnet 5.5 (cache $0.10)": (2.00, 0.10, 10.00),  # notes/script; earlier repo report after 2026-10-07
    "Claude Sonnet 5.5 (cache $0.20)": (2.00, 0.20, 10.00),  # coordinator-verified Anthropic reference: use this
    "Claude Opus 5.5":                (4.00, 0.20, 20.00),
    "GPT-5.5":                        (5.00, 0.50, 30.00),
}
LONGCAT_RMB = (2.0, 0.04, 8.0)  # promo price, RMB per 1M tokens

# Session archetypes (notes 3.B / inference_cost_estimate.py)
SCEN = {  # calls, input tokens per call, cached share, output tokens per call, session->order conversion
    "reorder":   (3, 6000, 0.8, 300, 0.8),
    "discovery": (8, 12000, 0.6, 700, 0.4),
    "complex":   (20, 20000, 0.5, 1500, 0.4),
}


def session_cost(price, scen, write_mult=1.0):
    """Cost of one session. write_mult>1 bills uncached input as cache writes."""
    calls, inp, cached, out, _ = SCEN[scen]
    tin = calls * inp
    c = tin * cached
    u = tin - c
    o = calls * out
    p_in, p_cache, p_out = price
    return (u * p_in * write_mult + c * p_cache + o * p_out) / 1e6


def per_order(price, scen, conv=None, write_mult=1.0):
    conv = SCEN[scen][4] if conv is None else conv
    return session_cost(price, scen, write_mult) / conv


def threshold(inf, err_rate=0.001, k=0.0, ads=0.02, aov=AOV, cp=CP, basket=0.0, tau=TAU):
    """Share of agent orders that must be truly incremental to break even (report §5.3),
    optionally net of basket lift on agent orders (term 2 of the §1 formula)."""
    err = err_rate * (aov + 3.0)
    return (inf + err + k * ads * aov - basket * aov * tau) / cp


def pct(x, d=1):
    return f"{100 * x:.{d}f}%"


def h(title):
    print("\n" + "=" * 78 + f"\n{title}\n" + "=" * 78)


# ---------------------------------------------------------------------------
# 2. DoorDash per-order economics (§2.1)
# ---------------------------------------------------------------------------
h("1. DoorDash Q3 2026 per-order economics (report §2.1)")
o, g, r, cp, eb = DD_Q["Q3'26"]
print(f"GOV/order        {g / o:.3f}  (notes give $34.18 from unrounded GOV)")
print(f"Revenue/order    {r / o:.3f}")
print(f"CP/order         {cp / o:.4f}   CP % GOV {pct(cp / g, 2)}")
print(f"Adj.EBITDA/order {eb / o:.4f}   % GOV {pct(eb / g, 2)}")
print(f"GAAP NI/order    {DD_NI_Q3 / o:.4f}")
print(f"Net revenue margin {pct(r / g, 2)}")
print(f"Orders per day   {o / DD_Q3_DAYS / 1e6:.2f}M")
o9 = sum(DD_Q[q][0] for q in ("Q1'26", "Q2'26", "Q3'26"))
g9 = sum(DD_Q[q][1] for q in ("Q1'26", "Q2'26", "Q3'26"))
cp9 = sum(DD_Q[q][3] for q in ("Q1'26", "Q2'26", "Q3'26"))
eb9 = sum(DD_Q[q][4] for q in ("Q1'26", "Q2'26", "Q3'26"))
print(f"9M 2026 orders   {o9 / 1e9:.3f}B; GOV/order {g9 / o9:.2f}; CP/order {cp9 / o9:.3f}; EBITDA/order {eb9 / o9:.3f}")
# 2024 GOV implied by notes (2025 GOV +27%), to bound DoorDash's ad rate
q1_25 = 31.6 / 1.37
q2_25 = 33.1 / 1.36
gov25 = q1_25 + q2_25 + 25.0 + 29.7
gov24 = gov25 / 1.27
print(f"Implied 2025 GOV ${gov25:.1f}B; 2024 GOV ${gov24:.1f}B; $1B ad run-rate = {pct(1 / gov24, 2)} of 2024 GOV")

# ---------------------------------------------------------------------------
# 3. Inference cost (§5.3, §7.3) and price-change effects
# ---------------------------------------------------------------------------
h("2. Inference cost per session / per order (USD)")
print(f"{'model':34s}{'reorder/sess':>13s}{'disc/sess':>11s}{'cplx/sess':>11s}{'reorder/ord':>12s}{'disc/ord':>10s}{'cplx/ord':>10s}{'disc %EBITDA':>13s}{'disc %CP':>9s}")
for m, p in P.items():
    s = [session_cost(p, x) for x in SCEN]
    po = [per_order(p, x) for x in SCEN]
    print(f"{m:34s}{s[0]:13.4f}{s[1]:11.4f}{s[2]:11.4f}{po[0]:12.4f}{po[1]:10.4f}{po[2]:10.3f}{pct(po[1] / EBITDA):>13s}{pct(po[1] / CP):>9s}")

print("\nSonnet 5.5 rows that change with the cache-read price ($0.10 notes vs $0.20 coordinator ref):")
for m in ("Claude Sonnet 5.5 (cache $0.10)", "Claude Sonnet 5.5 (cache $0.20)"):
    p = P[m]
    print(f"  {m}: discovery/order {per_order(p, 'discovery'):.4f} ({pct(per_order(p, 'discovery') / EBITDA)} of EBITDA); "
          f"complex/order {per_order(p, 'complex'):.3f} vs CP {CP}")
print("Cache-write premium (uncached input billed at 1.25x, Claude only):")
for m in ("Claude Haiku 5.5", "Claude Sonnet 5.5 (cache $0.10)", "Claude Sonnet 5.5 (cache $0.20)"):
    a = per_order(P[m], "discovery")
    b = per_order(P[m], "discovery", write_mult=1.25)
    print(f"  {m}: discovery/order {a:.4f} -> {b:.4f} (+{pct(b / a - 1)})")
print("Haiku 5.5 cache-read price unverified; discovery/order at cache read $0.01 / $0.05 / $0.10:")
for cr in (0.01, 0.05, 0.10):
    po = per_order((0.10, cr, 0.50), "discovery")
    print(f"  cache {cr:.2f}: {po:.4f}/order; threshold k=0 {pct(threshold(po))}")
print("Session->order conversion 20% instead of 40% (discovery):")
for m in ("Claude Haiku 5.5", "Gemini 3.8 Flash 2027", "Claude Sonnet 5.5 (cache $0.20)", "GPT-5.5"):
    print(f"  {m}: {per_order(P[m], 'discovery', conv=0.2):.3f}/order")

# Merchant-side monthly inference (§7.3): 30 runs x 20k input (50% cached) + 2k output
h("3. Merchant agent inference per merchant-month (§7.3)")
for m in ("Claude Haiku 5.5", "DeepSeek Flash peak", "Claude Sonnet 5.5 (cache $0.10)", "Claude Sonnet 5.5 (cache $0.20)"):
    pi, pc, pout = P[m]
    cost = (300e3 * pi + 300e3 * pc + 60e3 * pout) / 1e6
    print(f"  {m}: ${cost:.3f}/month")
fee = 450 * 12
print(f"$450/mo = ${fee}/yr = {pct(fee / 1e6, 2)} of $1M sales; {pct(fee / 40e3)}–{pct(fee / 28e3)} of 4.0%–2.8% pre-tax profit; "
      f"orders to pay back at 65% contribution, $30 AOV: {fee / 12 / (30 * 0.65):.1f}/month")

# ---------------------------------------------------------------------------
# 4. Ad loss, error cost, thresholds (§5.3)
# ---------------------------------------------------------------------------
h("4. DoorDash agent-order P&L items and break-even incremental share (§5.3)")
haiku = per_order(P["Claude Haiku 5.5"], "discovery")
gem26 = per_order(P["Gemini 3.8 Flash 2026"], "discovery")
for k in (0.25, 0.5):
    loss = k * 0.02 * AOV
    print(f"ad loss k={k:.0%}: ${loss:.3f} = {pct(loss / EBITDA)} of EBITDA")
for e in (0.001, 0.01):
    c = e * (AOV + 3)
    print(f"error {e:.1%}: ${c:.4f} = {pct(c / EBITDA)} of EBITDA")
print(f"k at which ad loss = Haiku inference: {pct(haiku / (0.02 * AOV))}")
print("\nReport table (ads 2%, error 0.1%, CP $1.79):")
for k in (0, 0.25, 0.5):
    print(f"  k={k:.0%}: small {pct(threshold(haiku, k=k))}  mid(2026) {pct(threshold(gem26, k=k))}")

print("\nGrid A - model x k (ads 2% of GOV, error 0.1%, conv 40%):")
models_a = ["Claude Haiku 5.5", "DeepSeek Flash peak", "Gemini 3.8 Flash 2026", "Gemini 3.8 Flash 2027",
            "GPT-5.4 mini", "Claude Sonnet 5.5 (cache $0.20)", "Claude Opus 5.5", "GPT-5.5"]
print(f"  {'model':34s}" + "".join(f"{'k=' + format(k, '.0%'):>9s}" for k in (0, .25, .5, .75)))
for m in models_a:
    inf = per_order(P[m], "discovery")
    print(f"  {m:34s}" + "".join(f"{pct(threshold(inf, k=k)):>9s}" for k in (0, .25, .5, .75)))

print("\nGrid B - ad rate x k (Haiku, error 0.1%):")
for ads in (0.0125, 0.02, 0.025, 0.031):
    print(f"  ads {pct(ads, 2):>6s}: " + "  ".join(f"k={k:.0%} {pct(threshold(haiku, k=k, ads=ads))}" for k in (0, .25, .5, .75))
          + f"   | k* (ad loss = inference) {pct(haiku / (ads * AOV))}")

print("\nGrid C - error rate x k (Haiku, ads 2%):")
for e in (0.001, 0.003, 0.005, 0.01):
    print(f"  error {e:.1%}: " + "  ".join(f"k={k:.0%} {pct(threshold(haiku, err_rate=e, k=k))}" for k in (0, .25, .5, .75)))

print("\nGrid D - basket lift on agent orders x k (Haiku, ads 2%, take on extra basket 13.6%):")
for b in (0.0, 0.05, 0.10, 0.17):
    print(f"  basket +{b:.0%} (=${b * AOV * TAU:.2f}/order): " + "  ".join(f"k={k:.0%} {pct(threshold(haiku, k=k, basket=b))}" for k in (0, .25, .5, .75)))

print("\nGrid E - base period (Haiku, ads 2%, error 0.1%):")
for label, cpv, ebv, aovv in (("Q3 2026", CP, EBITDA, AOV), ("9M 2026 avg", cp9 / o9, eb9 / o9, g9 / o9)):
    print(f"  {label}: CP {cpv:.3f}, EBITDA {ebv:.3f}, AOV {aovv:.2f}: k=0 {pct(threshold(haiku, aov=aovv, cp=cpv))}, "
          f"k=50% {pct(threshold(haiku, k=.5, aov=aovv, cp=cpv))}; shifted-order EBITDA break-even fee {pct(ebv / aovv, 2)}")

print("\nCorners (ads, k, error, model): break-even incremental share")
corners = [
    ("best: ads 1.25%, k 0, err 0.1%, Haiku, basket +10%", threshold(haiku, k=0, ads=.0125, basket=.10)),
    ("report base: ads 2%, k 50%, err 0.1%, Haiku", threshold(haiku, k=.5)),
    ("ads 3.1%, k 75%, err 0.1%, Haiku", threshold(haiku, k=.75, ads=.031)),
    ("ads 3.1%, k 75%, err 1%, Haiku", threshold(haiku, k=.75, ads=.031, err_rate=.01)),
    ("ads 3.1%, k 75%, err 1%, Sonnet ($0.20)", threshold(per_order(P["Claude Sonnet 5.5 (cache $0.20)"], "discovery"), k=.75, ads=.031, err_rate=.01)),
    ("ads 2%, k 50%, err 0.1%, GPT-5.5", threshold(per_order(P["GPT-5.5"], "discovery"), k=.5)),
    ("ads 3.1%, k 75%, err 1%, GPT-5.5", threshold(per_order(P["GPT-5.5"], "discovery"), k=.75, ads=.031, err_rate=.01)),
]
for label, v in corners:
    print(f"  {label}: {pct(v)}")

# ---------------------------------------------------------------------------
# 5. Channel fees (§6.3)
# ---------------------------------------------------------------------------
h("5. Third-party agent channel fees (§6.3)")
ads_per_order = 0.02 * AOV
print(f"{'fee':>5s}{'fee $':>8s}{'shifted EBITDA':>16s}{'incr CP':>9s}{'incr CP, ads lost':>19s}{'shifted Δ vs app':>18s}")
for f in (0.04, 0.07, 0.12):
    fee_ = f * AOV
    print(f"{f:5.0%}{fee_:8.3f}{EBITDA - fee_:16.3f}{CP - fee_:9.3f}{CP - ads_per_order - fee_:19.3f}{-(fee_ + ads_per_order):18.3f}")
print(f"Break-even fee (GOV base): shifted {pct(EBITDA / AOV, 2)}; shifted with ads lost {pct((EBITDA - ads_per_order) / AOV, 2)}; "
      f"incremental {pct(CP / AOV, 2)}; incremental with ads lost {pct((CP - ads_per_order) / AOV, 2)}")
print("Fee base = food subtotal (assumed 70% / 80% of GOV, which includes tips, taxes and consumer fees):")
for sh in (0.7, 0.8):
    fee_ = 0.04 * sh * AOV
    print(f"  subtotal {sh:.0%}: 4% fee ${fee_:.3f}; shifted EBITDA {EBITDA - fee_:+.3f}; shifted with ads lost {EBITDA - ads_per_order - fee_:+.3f}; "
          f"incremental {CP - fee_:+.3f}; incremental ads lost {CP - ads_per_order - fee_:+.3f}; shifted break-even {pct(EBITDA / (sh * AOV), 2)} of subtotal")

# ---------------------------------------------------------------------------
# 6. Meituan (§2.2, §5.3, §6.3, §8.2)
# ---------------------------------------------------------------------------
h("6. Meituan segment and per-order figures")
print(f"CLC margins: 2024 {pct(MT['op24'] / MT['rev24'], 2)}; Q3'25 {pct(MT['op_q3_25'] / MT['rev_q3_25'], 2)}; 2025 {pct(MT['op25'] / MT['rev25'], 2)}; "
      f"Q1'26 {pct(MT['op_q1_26'] / MT['rev_q1_26'], 2)}; Q2'26 {pct(MT['op_q2_26'] / MT['rev_q2_26'], 2)}")
print(f"2025 mix: commission {pct(MT['comm25'] / MT['rev25'], 2)}, delivery {pct(MT['deliv25'] / MT['rev25'], 2)}, ads {pct(MT['ads25'] / MT['rev25'], 2)}, "
      f"other {MT['other25']:.2f}bn; commission+ads {pct((MT['comm25'] + MT['ads25']) / MT['rev25'], 2)}")
print(f"2024 ads / CLC OP = {pct(MT['ads24'] / MT['op24'], 2)}; 2025 ratio undefined (OP {MT['op25']}bn)")
ann = MT["op_q2_26"] * 4
print(f"Q2'26 annualized CLC OP RMB{ann:.2f}bn; 10% of 2025 ads = RMB{0.1 * MT['ads25']:.3f}bn = {pct(0.1 * MT['ads25'] / ann)}")

print("\nImplied food-delivery UE (RMB/order), Q2 2026:")
for d in (65e6, 85e6):
    n = d * Q2_DAYS
    ms = MT["ms_q2_fd"] * 1e9 / n
    tri = (MT["op_q2_26"] - MT["ms_q2_iht"] - MT["ms_q2_flash"]) * 1e9 / n
    print(f"  {d / 1e6:.0f}M/day: MS forecast {ms:.3f}; actual CLC OP less MS IHT and 闪购 forecasts {tri:.3f}")
for d in (65e6, 85e6):
    print(f"  Sanity: RMB1/order x {d / 1e6:.0f}M x 365 = RMB{d * 365 / 1e9:.1f}bn/yr vs Q2 annualized CLC OP RMB{ann:.1f}bn "
          f"(which includes ~RMB{MT['ms_q2_iht'] * 4:.0f}bn/yr of 到店酒旅 at MS's estimate)")

print("\nInference as % of food-delivery profit per order (RMB), at different UE assumptions:")
rows = []
for m in ("Claude Haiku 5.5", "DeepSeek Flash peak", "DeepSeek Flash off-peak"):
    rows.append((m, per_order(P[m], "reorder") * FX, per_order(P[m], "discovery") * FX))
lc = [(SCEN[x][0] * SCEN[x][1] * (1 - SCEN[x][2]) * LONGCAT_RMB[0] + SCEN[x][0] * SCEN[x][1] * SCEN[x][2] * LONGCAT_RMB[1]
       + SCEN[x][0] * SCEN[x][3] * LONGCAT_RMB[2]) / 1e6 / SCEN[x][4] for x in ("reorder", "discovery")]
rows.append(("LongCat promo", lc[0], lc[1]))
for m, a, b in rows:
    print(f"  {m:24s} RMB/order reorder {a:.4f}, discovery {b:.4f} | " +
          "  ".join(f"UE {ue:g}: {pct(a / ue)}/{pct(b / ue)}" for ue in (1.0, 0.45, 0.4, 0.3)))

print("\nChannel-fee break-even (UE / AOV):")
for ue in (1.5, 1.0, 0.45, 0.4, 0.3, 0.05, -0.3):
    print(f"  UE {ue:+.2f}: AOV 30 -> {pct(ue / 30, 2)}, AOV 50 -> {pct(ue / 50, 2)}")
for ue in (1.0, 0.4):
    print(f"12% fee on RMB30-50: RMB{0.12 * 30:.1f}-{0.12 * 50:.1f} = {0.12 * 30 / ue:.1f}-{0.12 * 50 / ue:.1f}x of RMB{ue} UE")
print(f"Mid-2025 '$1B a year' ad unit vs Q2'25 annualized GOV ${4 * q2_25:.1f}B = {pct(1 / (4 * q2_25), 2)}")

print("\nS4 Meituan RMB0.05/order saving and drones:")
for d in (65e6, 70e6, 85e6):
    s = 0.05 * d * 365 / 1e9
    print(f"  {d / 1e6:.0f}M/day: RMB{s:.2f}bn/yr = {pct(s / ann)} of Q2 annualized CLC OP; 1M drone orders = {pct(1e6 / d, 2)} of a day")

# ---------------------------------------------------------------------------
# 7. Other derived numbers
# ---------------------------------------------------------------------------
h("7. Other derived numbers")
print(f"S4 DoorDash $0.10/order: ${0.10 * o / 1e6:.1f}M/qtr = {pct(0.10 * o / g, 3)} of GOV = {pct(0.10 * o / eb)} of Q3 Adj. EBITDA")
for sv in (3, 7, 9):
    v = 0.01 * o * sv
    print(f"  robots 1% of orders ({0.01 * o / 1e6:.2f}M) x ${sv}: ${v / 1e6:.1f}M = {pct(v / eb)} of Q3 EBITDA")
print(f"  robots needed at 1,500 orders/robot/qtr: {0.01 * o / 1500:,.0f}")
print("S5 support savings per order and per year:")
for human in (3.13, 1.25):
    lo = 0.02 * 0.5 * (human - 0.05)
    hi = 0.05 * 0.7 * (human - 0.05)
    print(f"  human ${human}/contact: ${lo:.4f}-{hi:.4f}/order; x {4 * o / 1e9:.3f}B orders = ${lo * 4 * o / 1e6:.0f}-{hi * 4 * o / 1e6:.0f}M/yr "
          f"= {pct(lo * o / eb)}-{pct(hi * o / eb)} of annualized Adj. EBITDA")
rd = 0.015 * g
print(f"DoorDash R&D ≈ 1.5% x GOV = ${rd / 1e6:.0f}M = {pct(rd / r)} of revenue (Meituan R&D 7.3% of revenue); G&A {pct(0.013 * g / r)} of revenue")
for n in (200, 500, 900):
    print(f"  {n} third-party orders / {o9 / 1e9:.3f}B = {n / o9:.2e}")
print(f"Qwen subsidy upper bound RMB{3e9 / 1.2e8:.1f}/order")
print(f"Intercom Fin margin {(0.99 - 0.05) / 0.99:.1%}; LongCat RMB9.9/50M = RMB{9.9 / 50:.3f}/M; RMB399/1B = RMB{399 / 1000:.3f}/M")
print(f"DeepSeek Flash peak in RMB: input {0.30 * FX:.2f}, cache {0.006 * FX:.4f}, output {1.20 * FX:.2f}")
print(f"Peers: Uber {pct(1055 / 27463, 2)}, Instacart EBITDA {pct(342 / 10528, 2)}, Instacart ads+other {pct(331 / 10528, 2)}, "
      f"Grab {pct(123 / 4500, 2)}, Swiggy {pct(341 / 10054, 2)}")
