#!/usr/bin/env python3
"""Recalculations for the revised report (coordinator rulings applied).
- Sonnet 5.5 cache read = $0.10 (current price since 2026-10-07).
- DoorDash ad share: DoorDash-implied 1.0%-1.25% of GOV vs peers 2%-3.1%.
- Thresholds include the basket-lift term.
- Channel fee: shifted orders have no break-even; incremental orders lose ads.
- Fee shown on food-subtotal (70%-80% of GOV) and GOV bases.
- Meituan per-order profit: post-war range, not a ¥1 point.
"""
FX = 7.1
AOV, CP, EB = 34.18, 1.79, 1.07          # DoorDash Q3 2026 per order (notes; report rounds GOV to 34.2)
TAU = 0.136                              # net revenue margin, take on extra basket
ERR_COST = AOV + 3.0                     # full refund + $3 support

P = {  # USD per 1M tokens: input, cached, output
    "Haiku 5.5": (0.10, 0.01, 0.50),
    "DeepSeek Flash peak": (0.30, 0.006, 1.20),
    "DeepSeek Flash off-peak": (0.15, 0.003, 0.60),
    "Gemini 3.8 Flash 2026": (0.75, 0.075, 3.75),
    "Gemini 3.8 Flash 2027": (1.50, 0.15, 7.50),
    "Sonnet 5.5 (cache 0.10)": (2.00, 0.10, 10.00),
    "Opus 5.5": (4.00, 0.20, 20.00),
    "GPT-5.5": (5.00, 0.50, 30.00),
}
LONGCAT = (2.0, 0.04, 8.0)               # RMB per 1M tokens, promo
SCEN = {"reorder": (3, 6000, 0.8, 300, 0.8),
        "discovery": (8, 12000, 0.6, 700, 0.4),
        "complex": (20, 20000, 0.5, 1500, 0.4)}

def sess(price, s, write_mult=1.0):
    calls, inp, cf, out, _ = SCEN[s]
    tin = calls * inp; c = tin * cf; u = tin - c; o = calls * out
    return (u * price[0] * write_mult + c * price[1] + o * price[2]) / 1e6

def per_order(price, s, conv=None, write_mult=1.0):
    return sess(price, s, write_mult) / (SCEN[s][4] if conv is None else conv)

def thr(inf, k=0.0, ads=0.02, err=0.001, basket=0.0, cp=CP):
    return (inf + err * ERR_COST + k * ads * AOV - basket * AOV * TAU) / cp

pc = lambda x, d=1: f"{100*x:.{d}f}%"

print("== Inference per order (USD) and share of Adj. EBITDA $1.07 ==")
for m, p in P.items():
    d = per_order(p, "discovery"); r = per_order(p, "reorder"); c = per_order(p, "complex")
    print(f"{m:26s} reorder {r:.4f}  discovery {d:.4f} ({pc(d/EB)})  complex {c:.3f}")
haiku = per_order(P["Haiku 5.5"], "discovery")
g26 = per_order(P["Gemini 3.8 Flash 2026"], "discovery")
g27 = per_order(P["Gemini 3.8 Flash 2027"], "discovery")
print("Cache-write premium 1.25x on uncached input:")
for m in ("Haiku 5.5", "Sonnet 5.5 (cache 0.10)"):
    a = per_order(P[m], "discovery"); b = per_order(P[m], "discovery", write_mult=1.25)
    print(f"  {m}: {a:.4f} -> {b:.4f} (+{pc(b/a-1)})")
print("Haiku cache read 0.05 / 0.10 instead of 0.01:")
for cr in (0.05, 0.10):
    po = per_order((0.10, cr, 0.50), "discovery"); print(f"  {cr}: {po:.4f}/order; k=0 threshold {pc(thr(po))}")
print("Conversion 20%: haiku", f"{per_order(P['Haiku 5.5'],'discovery',conv=0.2):.4f}",
      " gem26", f"{per_order(P['Gemini 3.8 Flash 2026'],'discovery',conv=0.2):.4f}",
      " sonnet", f"{per_order(P['Sonnet 5.5 (cache 0.10)'],'discovery',conv=0.2):.4f}",
      " gpt", f"{per_order(P['GPT-5.5'],'discovery',conv=0.2):.4f}")

print("\n== Ad revenue per order and ad loss ==")
for ads in (0.010, 0.0125, 0.02, 0.031):
    a = ads * AOV
    print(f"ads {pc(ads,2):>6s}: ad rev/order ${a:.3f} = {pc(a/EB)} of EBITDA; k25 ${0.25*a:.3f} ({pc(0.25*a/EB)}); "
          f"k50 ${0.5*a:.3f} ({pc(0.5*a/EB)}); k* (ad loss=haiku inf) {pc(haiku/a)}")
print(f"error 0.1%: ${0.001*ERR_COST:.4f} ({pc(0.001*ERR_COST/EB)}); 1%: ${0.01*ERR_COST:.4f} ({pc(0.01*ERR_COST/EB)}); "
      f"error rate that wipes out EBITDA: {pc(EB/ERR_COST)}")

print("\n== Thresholds (error 0.1%, basket 0, CP 1.79) ==")
for name, inf in (("small/haiku", haiku), ("mid 2026", g26), ("mid 2027", g27)):
    for k in (0, 0.25, 0.5):
        vals = [thr(inf, k=k, ads=a) for a in (0.010, 0.0125, 0.02, 0.031)]
        print(f"{name:12s} k={k:.0%}: ads1.0 {pc(vals[0])} ads1.25 {pc(vals[1])} ads2 {pc(vals[2])} ads3.1 {pc(vals[3])}")
print("basket +5%% = $%.3f/order = %.1f pts; +10%% = $%.3f = %.1f pts; +17%% = %.1f pts" % (
    0.05*AOV*TAU, 100*0.05*AOV*TAU/CP, 0.10*AOV*TAU, 100*0.10*AOV*TAU/CP, 100*0.17*AOV*TAU/CP))
print("error 1%% adds %.1f pts" % (100*(0.01-0.001)*ERR_COST/CP))
reorder_haiku = per_order(P["Haiku 5.5"], "reorder")
print(f"reorder haiku per order {reorder_haiku:.5f}; k=0 threshold {pc(thr(reorder_haiku))}")
cp9 = (1.38 + 1.64 + 1.78) / (0.933 + 0.970 + 0.993); eb9 = (0.754 + 0.914 + 1.060) / (0.933 + 0.970 + 0.993)
print(f"9M 2026 CP/order {cp9:.3f}, EBITDA/order {eb9:.3f}; scale factor {CP/cp9:.3f}")
print("corner examples small: best (ads1.0,k0,basket10%)", pc(thr(haiku, k=0, ads=0.01, basket=0.10)),
      "; worst-ish (ads3.1,k75,err1%)", pc(thr(haiku, k=0.75, ads=0.031, err=0.01)))
print("k=50% thresholds with basket +5%: ads1.0", pc(thr(haiku,k=.5,ads=.01,basket=.05)), " ads2", pc(thr(haiku,k=.5,ads=.02,basket=.05)),
      " ads3.1", pc(thr(haiku,k=.5,ads=.031,basket=.05)))

print("\n== Channel fees (DoorDash) ==")
bases = {"sub70": 0.7*AOV, "sub80": 0.8*AOV, "GOV": AOV}
for f in (0.04, 0.07, 0.12):
    fees = {b: f*v for b, v in bases.items()}
    best_shift = -(fees["sub70"] + 0.010*AOV); worst_shift = -(fees["GOV"] + 0.02*AOV)
    best_inc = CP - fees["sub70"] - 0.010*AOV; worst_inc = CP - fees["GOV"] - 0.02*AOV
    print(f"fee {f:.0%}: sub70 ${fees['sub70']:.3f} sub80 ${fees['sub80']:.3f} GOV ${fees['GOV']:.3f} | "
          f"shift vs app {best_shift:+.2f} to {worst_shift:+.2f} | incremental {best_inc:+.2f} to {worst_inc:+.2f}")
    print(f"           accounting EBITDA (ads kept): sub70 {EB-fees['sub70']:+.3f} sub80 {EB-fees['sub80']:+.3f} GOV {EB-fees['GOV']:+.3f}; "
          f"ads lost (1%-2%): best {EB-fees['sub70']-0.01*AOV:+.3f} worst {EB-fees['GOV']-0.02*AOV:+.3f}")
for ads in (0.010, 0.02):
    print(f"incremental break-even fee, ads {pc(ads)}: GOV {pc((CP-ads*AOV)/AOV,2)}; sub70 {pc((CP-ads*AOV)/(0.7*AOV),2)}; "
          f"sub80 {pc((CP-ads*AOV)/(0.8*AOV),2)}")
print(f"incremental break-even if ads kept: GOV {pc(CP/AOV,2)}")
print(f"accounting break-even shifted (ads kept): GOV {pc(EB/AOV,2)}; sub70 {pc(EB/(0.7*AOV),2)}; sub80 {pc(EB/(0.8*AOV),2)}")

print("\n== Meituan: inference RMB per order and share of per-order profit ==")
def lc(s):
    calls, inp, cf, out, conv = SCEN[s]
    tin = calls*inp
    return (tin*(1-cf)*LONGCAT[0] + tin*cf*LONGCAT[1] + calls*out*LONGCAT[2]) / 1e6 / conv
rows = [("Haiku", per_order(P["Haiku 5.5"], "reorder")*FX, per_order(P["Haiku 5.5"], "discovery")*FX),
        ("DS peak", per_order(P["DeepSeek Flash peak"], "reorder")*FX, per_order(P["DeepSeek Flash peak"], "discovery")*FX),
        ("DS off-peak", per_order(P["DeepSeek Flash off-peak"], "reorder")*FX, per_order(P["DeepSeek Flash off-peak"], "discovery")*FX),
        ("LongCat", lc("reorder"), lc("discovery"))]
UES = (1.5, 0.5, 0.4, 0.3)
for n, r, d in rows:
    print(f"{n:12s} reorder ¥{r:.4f}: " + " ".join(f"UE{u}:{pc(r/u)}" for u in UES) +
          f" | discovery ¥{d:.4f}: " + " ".join(f"UE{u}:{pc(d/u)}" for u in UES))
print("Session costs RMB: LongCat reorder sess", f"{lc('reorder')*0.8:.4f}", "discovery sess", f"{lc('discovery')*0.4:.4f}")
print("\nFee break-even UE/AOV (AOV ¥30-50):")
for u in (1.5, 0.5, 0.4, 0.3):
    print(f"  UE ¥{u}: {pc(u/50)}-{pc(u/30)}")
for prem, label in ((0.04, "AI premium 4pp"), (0.12, "12% total")):
    lo, hi = prem*30, prem*50
    print(f"{label}: ¥{lo:.1f}-{hi:.1f}/order; multiples of UE: " +
          "; ".join(f"UE{u}: {lo/u:.1f}-{hi/u:.1f}x" for u in (0.5, 0.4, 0.3)))
print("\nMeituan triangulation (review): Q2 CLC OP 56.7亿 - MS 到店 43亿 - MS 闪购(-7.5亿) =", f"{56.7-43+7.5:.1f}亿")
for d in (65e6, 85e6):
    n = d * 91
    print(f"  {d/1e6:.0f}M/day: triangulated UE ¥{(56.7-43+7.5)*1e8/n:.3f}; MS food forecast 3.13亿 -> ¥{3.13e8/n:.3f}")
print("Ads sensitivity: 10% of 2025 ads 514.6亿 =", f"{51.46:.2f}亿", "vs Q2 annualized CLC OP", f"{5.67*4*10:.1f}亿",
      "=", pc(5.146/(5.67*4)))

print("\n== S4 / S5 ==")
o = 993e6; eb_q = 1060e6; gov_q = 33.9e9
print(f"$0.10/order: ${0.10*o/1e6:.1f}M/qtr = {pc(0.10*o/gov_q,3)} of GOV = {pc(0.10*o/eb_q)} of Q3 Adj. EBITDA")
for s in (3, 7, 9):
    print(f"  robots 1% x ${s}: ${0.01*o*s/1e6:.1f}M = {pc(0.01*o*s/eb_q)}")
for d in (65e6, 70e6, 85e6):
    sv = 0.05*d*365/1e8
    print(f"  Meituan ¥0.05 x {d/1e6:.0f}M/day: ¥{sv:.1f}亿/yr = {pc(sv/(5.67*4*10))}; drones 100万 = {pc(1e6/d,2)} of a day")
for human in (3.13, 1.25):
    lo = 0.02*0.5*(human-0.05); hi = 0.05*0.7*(human-0.05)
    print(f"  support human ${human}: ${lo:.3f}-{hi:.3f}/order -> ${lo*4*o/1e6:.0f}-{hi*4*o/1e6:.0f}M/yr = "
          f"{pc(lo*o/eb_q)}-{pc(hi*o/eb_q)} of annualized EBITDA")
rd = 0.015*gov_q; rev = 4.62e9
print(f"DoorDash R&D ~{rd/1e6:.0f}M = {pc(rd/rev)} of revenue")
print("DoorDash ad-rate anchors: 2024 GOV", f"{(31.6/1.37+33.1/1.36+25.0+29.7)/1.27:.1f}B ->",
      pc(1/((31.6/1.37+33.1/1.36+25.0+29.7)/1.27),2), "; Q2'25 annualized", f"{4*33.1/1.36:.1f}B ->", pc(1/(4*33.1/1.36),2))
for ads in (0.010, 0.0125, 0.02):
    print(f"  DoorDash ads {pc(ads,2)} of GOV = {pc(ads/0.0313)} of Q3 Adj. EBITDA (3.13% of GOV)")
print("Instacart ads+other / EBITDA:", pc(331/342), "; Uber >2%/3.84%:", pc(0.02/0.0384))
