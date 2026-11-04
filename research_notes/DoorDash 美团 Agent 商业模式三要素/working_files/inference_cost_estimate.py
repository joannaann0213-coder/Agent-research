# Inference cost per agent session/order (estimates). Prices USD per 1M tokens (Nov 2026).
models = {
 "Claude Haiku 5.5":            dict(inp=0.10, cache=0.01,  out=0.50),
 "DeepSeek V4.1 Flash (peak)":  dict(inp=0.30, cache=0.006, out=1.20),
 "Gemini 3.5 Flash-Lite":       dict(inp=0.30, cache=0.03,  out=2.50),
 "Gemini 3.8 Flash (to 12/31/26)": dict(inp=0.75, cache=0.075, out=3.75),
 "Gemini 3.8 Flash (from 1/1/27)": dict(inp=1.50, cache=0.15,  out=7.50),
 "GPT-5.4 mini (3rd-party list)": dict(inp=0.75, cache=0.075, out=4.50),
 "Claude Sonnet 5.5":           dict(inp=2.00, cache=0.10,  out=10.0),
 "GPT-5.5 (3rd-party list; cache assumed 90% off)": dict(inp=5.00, cache=0.50, out=30.0),
}
# scenarios: calls, input tokens per call, cached share, output tokens per call
scen = {
 "S1 reorder/simple":      dict(calls=3,  inp=6000,  cached=0.8, out=300),
 "S2 discovery+order":     dict(calls=8,  inp=12000, cached=0.6, out=700),
 "S3 complex/multi-step":  dict(calls=20, inp=20000, cached=0.5, out=1500),
}
print("Token totals per session:")
for s,v in scen.items():
    tin=v['calls']*v['inp']; c=tin*v['cached']; u=tin-c; o=v['calls']*v['out']
    print(f"  {s}: input {tin:,} (cached {c:,.0f}, uncached {u:,.0f}), output {o:,}")
print()
res={}
hdr="Model".ljust(50)+"".join(s.split()[0].rjust(10) for s in scen)
print(hdr)
for m,p in models.items():
    row=m.ljust(50)
    for s,v in scen.items():
        tin=v['calls']*v['inp']; c=tin*v['cached']; u=tin-c; o=v['calls']*v['out']
        cost=(u*p['inp']+c*p['cache']+o*p['out'])/1e6
        res[(m,s)]=cost
        row+=f"{cost:10.4f}"
    print(row)
print()
# per order at session->order conversion 40% (base) and 80% for S1
conv={"S1 reorder/simple":0.8,"S2 discovery+order":0.4,"S3 complex/multi-step":0.4}
print("Per-order inference cost (S1 conv 80%, S2/S3 conv 40%):")
for m in models:
    print("  "+m.ljust(50)+"".join(f"{res[(m,s)]/conv[s]:10.4f}" for s in scen))
print()
# Profit-per-order benchmarks (illustrative AOVs where not disclosed)
bench = [
 ("Instacart Q3'26 adj.EBITDA 3.2% x AOV $115.2 (computed)", 0.032*10528/91.4),
 ("Uber Delivery Q2'26 seg. op. inc. 3.8% x illustrative AOV $40", 0.038*40),
 ("Grab Deliveries Q3'26 adj.EBITDA 2.7% x illustrative AOV $15", 0.027*15),
 ("Zomato FD Q2FY27 adj.EBITDA 5.5% x illustrative AOV INR450 @88", 0.055*450/88),
 ("Swiggy FD Q2FY27 adj.EBITDA 3.4% x illustrative AOV INR500 @88", 0.034*500/88),
 ("Delivery Hero FY25 adj.EBITDA 1.4% x illustrative AOV EUR25 @1.08", 0.014*25*1.08),
 ("China illustrative: RMB1/order @7.1", 1/7.1),
]
print("Profit per order benchmarks (USD):")
for n,v in bench: print(f"  {n}: {v:.3f}")
print()
print("S2 per-order inference as % of profit/order:")
for m in ["Claude Haiku 5.5","DeepSeek V4.1 Flash (peak)","Gemini 3.8 Flash (to 12/31/26)","Claude Sonnet 5.5","GPT-5.5 (3rd-party list; cache assumed 90% off)"]:
    po=res[(m,"S2 discovery+order")]/0.4
    print("  "+m+": "+", ".join(f"{n.split()[0]} {100*po/v:.1f}%" for n,v in bench))
print()
# Break-even incremental orders per session = cost per session / contribution per incremental order
print("Break-even incremental orders per 100 S2 sessions (contribution/incremental order $1.50 and $0.40):")
for m in models:
    c=res[(m,"S2 discovery+order")]
    print(f"  {m}: {100*c/1.5:.2f} @ $1.50 ; {100*c/0.40:.2f} @ $0.40")
print()
# Support automation savings per order (illustrative)
for cr in (0.02,0.05):
  for r in (0.5,0.7):
    human=3.13; ai=0.05
    print(f"contact rate {cr:.0%}, AI resolution {r:.0%}: savings/order ${cr*r*(human-ai):.3f} (in-house AI $0.05/contact); with $0.99 vendor price: ${cr*r*(human-0.99):.3f}")
print()
# Ads cannibalization illustration
for share in (0.1,0.2,0.4):
  for adsgov in (0.02,0.031):
    for cut in (0.5,):
      loss=share*cut*adsgov
      print(f"agent share {share:.0%}, ads {adsgov:.1%} of GOV, ad-load cut {cut:.0%}: loss {loss*100:.2f}% of total GOV")
print()
print("Ads as share of segment profit: Instacart 3.1/3.2 =", round(3.1/3.2,2), "; Uber >2/3.8 =", round(2/3.8,2))
print("Rufus $/user/yr: 12e9/300e6 =", 12e9/300e6)
print("Qwen campaign upper-bound subsidy per AI order: RMB 3e9/1.2e8 =", 3e9/1.2e8)
print("Grab Q3'26 seg adj EBITDA %:", round(123/4500*100,2), "; Uber Q2'26 delivery op inc %:", round(1055/27463*100,2))
print("Swiggy adj EBITDA %:", round(341/10054*100,2), "; Zomato:", round(627/11445*100,2))
print("Instacart: AOV", round(10528/91.4,1), "txn rev %", round(764/10528*100,2), "ads %", round(331/10528*100,2), "EBITDA/order", round(342/91.4,2))
print("Delivery Hero 2026 guide EBITDA mid 1.0bn / GMV approx? H1 GMV 25.7bn + Q3 13.9bn")
