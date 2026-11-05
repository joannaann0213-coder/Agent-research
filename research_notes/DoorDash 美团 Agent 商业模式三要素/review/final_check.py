import glob, os, re, sys

ROOT = "/home/user/Agent-research"
REPORT = os.path.join(ROOT, "reports/DoorDash 美团 Agent 商业模式三要素.md")
text = open(REPORT, encoding="utf-8").read()
lines = text.splitlines()
print(f"report: {len(lines)} lines, {len(text)} chars")

# 1. Required fixes: each must be present in the revised report.
must_have = {
    "F1 shifted-order fee is a pure loss": r"纯损失",
    "F2 subtotal-base fee $0.96-1.09": r"0\.96\s*[–-]\s*1\.09",
    "F2 incremental break-even 3.2-4.2% of GOV": r"3\.2\s*[–-]\s*4\.2\s*%",
    "F3 Bernstein labelled pre-war": r"战前",
    "F3 Meituan fee ceiling 1.0-1.7% at ¥0.5": r"1\.0\s*[–-]\s*1\.7\s*%",
    "F4 DoorDash ad rate 1.0-1.25% of GOV": r"1\.0\s*[–-]\s*1\.25\s*%",
    "F5 几百单 attributed to Muse etc.": r"Muse",
    "F6 S3 qualified by management claims": r"管理层口径",
    "F8 hardest conditions C2 and C9": r"C2\s*和\s*C9|C2、C9|C2 与 C9",
    "FC2 Douyin rate set by 抖音生活服务": r"抖音生活服务",
    "FC2 AI-entry premium about 4 points": r"4 ?个百分点",
    "FC3 external agents via approved channels": r"认可的通道",
    "FC4 千问 AI phone booking via 高德": r"高德",
    "Must-add Alipay ACT 2.0": r"ACT ?2\.0",
    "Must-add APASS/KYA": r"APASS|KYA",
    "Must-add DoorDash MCP waitlist": r"候补|waitlist",
    "Must-add Adobe +31% conversion": r"31\s*%",
    "Must-add 2030 forecasts": r"2030",
    "Must-add 千问开放平台": r"千问开放平台",
    "TechCrunch date 2026-11-02": r"2026-11-02",
    "Review record line in metadata": r"review/",
    "WeChat Pay AI card correction (团购券)": r"团购券",
    "Tencent ¥105 亿 from official release": r"105 ?亿",
}
print("\n== required fixes present ==")
missing = 0
for name, pat in must_have.items():
    hits = [i + 1 for i, l in enumerate(lines) if re.search(pat, l)]
    ok = bool(hits)
    missing += (not ok)
    print(f"  {'OK ' if ok else 'MISSING'}  {name}  (lines {hits[:6]}{'...' if len(hits) > 6 else ''})")

# 2. Old wording that must be gone.
must_not = {
    "old: only discovery for external agents": r"只让外部 ?Agent ?做发现",
    "old: AI outbound calls unique capability": r"通用 ?Agent ?没有的能力",
    "old: Meituan range −¥0.3 to ¥1.5 as 2026 broker range": r"−?-?¥0\.3\s*到\s*¥1\.5",
    "old: S3 flatly 已经成立 (DoorDash)": r"DoorDash ?已经成立",
    "old: hardest from C7/C8 to C9 and C5": r"换成了 ?C9 ?和 ?C5",
    "old: Sonnet cache $0.20": r"Sonnet[^\n]{0,60}\$0\.20",
}
print("\n== old wording removed ==")
lingering = 0
for name, pat in must_not.items():
    hits = [i + 1 for i, l in enumerate(lines) if re.search(pat, l)]
    lingering += bool(hits)
    print(f"  {'GONE' if not hits else 'STILL THERE'}  {name}  {hits[:6] if hits else ''}")

# 3. Every URL in the report must appear in a source file.
sources = []
for pat in [
    "research_notes/Agent 商业模式三要素案例分析/*.md",
    "research_notes/DoorDash 美团 Agent 商业模式三要素/*.md",
    "research_notes/DoorDash 美团 Agent 商业模式三要素/review/*.md",
    "research_notes/DoorDash 美团 Agent 商业模式三要素/working_files/*.md",
    "reports/Agent 商业模式三要素案例分析.md",
    "subscription-vs-api-2026-10.md",
]:
    for f in glob.glob(os.path.join(ROOT, pat)):
        sources.append(open(f, encoding="utf-8").read())
corpus = "\n".join(sources)
urls = sorted(set(u.rstrip(").,;，。") for u in re.findall(r"https?://[^\s)\]>\"']+", text)))
unsourced = [u for u in urls if u not in corpus]
print(f"\n== URLs: {len(urls)} unique; {len(unsourced)} not found verbatim in notes/reviews/repo ==")
for u in unsourced[:20]:
    print("   ", u)

# 4. Markdown tables: column counts consistent within each table.
print("\n== table column check ==")
bad = 0
i = 0
while i < len(lines):
    if lines[i].lstrip().startswith("|"):
        start = i
        rows = []
        while i < len(lines) and lines[i].lstrip().startswith("|"):
            rows.append(lines[i])
            i += 1
        counts = [r.strip().strip("|").count("|") + 1 for r in rows]
        if len(set(counts)) > 1:
            bad += 1
            print(f"   table at line {start + 1}: column counts {sorted(set(counts))}")
    else:
        i += 1
print(f"   inconsistent tables: {bad}")

# 5. Independent recomputation of key DoorDash figures (Q3 2026, from notes).
gov, orders, ebitda, cp_pct, rev = 33.9e9, 993e6, 1060e6, 0.052, 4.6e9
aov = gov / orders
print("\n== DoorDash Q3 2026 per-order (independent) ==")
print(f"   GOV/order ${aov:.2f}; revenue/order ${rev/orders:.2f}; CP/order ${gov*cp_pct/orders:.2f}; Adj.EBITDA/order ${ebitda/orders:.2f}")
print(f"   4% fee on subtotal 70-80%: ${0.04*0.70*aov:.2f}-{0.04*0.80*aov:.2f}; on GOV: ${0.04*aov:.2f}")
print(f"   ads 1.0-1.25% of GOV per order: ${0.010*aov:.2f}-{0.0125*aov:.2f}; half lost: ${0.005*aov:.2f}-{0.00625*aov:.2f}")

print(f"\nSUMMARY: missing fixes={missing}, lingering old wording={lingering}, unsourced URLs={len(unsourced)}, bad tables={bad}")
