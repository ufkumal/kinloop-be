"""v6 unit test dokumanini ayristirip referans motorla kosar, beklenenle karsilastirir."""
import re
import sys
from v6engine import Child, build_plan, load_activities

DOC = "/root/.claude/uploads/6c213db8-3ece-57a7-8cd7-f0d767d7cbb5/001644db-Kidloop_Unit_Test_v6.md"
SLOT = {"Gelişim": "Gelisim", "Güçlendirme": "Guclendirme", "Keşif": "Kesif"}


def split_scenarios(text):
    parts = re.split(r'^# (?=[A-Z]+\d+-\d+ )', text, flags=re.M)
    out = []
    for p in parts[1:]:
        code = p.split(" ", 1)[0]
        out.append((code, p))
    return out


def num(pattern, body, cast=int, default=None):
    m = re.search(pattern, body)
    return cast(m.group(1)) if m else default


def parse(body):
    s = {}
    s["age"] = num(r'age_months\s*=\s*(\d+)', body)
    if s["age"] is None:
        s["age"] = num(r'\|\s*Çocuğun yaşı\s*\|\s*\*{0,2}(\d+)\s*ay', body)
    m = re.search(r'dunn_quadrant\s*=\s*(C[1-4]|MIXED)', body)
    s["quad"] = m.group(1) if m else None
    s["anx"] = num(r'separation_anxiety\s*=\s*(\d+)', body)
    s["bmin"] = num(r'budget_min\s*=\s*(\d+)', body)
    s["bmax"] = num(r'budget_max\s*=\s*(\d+)', body)
    m = re.search(r'focus_span\s*=\s*(\w+)', body)
    s["focus"] = m.group(1) if m else None
    s["cid"] = num(r'child_id\s*=\s*(\d+)', body, default=201)

    # Gardner: "MUSICAL 4.50, diğerleri 3.00"  ya da "hepsi 3.00"
    g = {}
    m = re.search(r'Gardner puanları\s*=\s*([A-Z_]+)\s+([\d.]+)', body)
    if m:
        g[m.group(1)] = float(m.group(2))
    s["gardner"] = g

    # acik baslangic basamagi (yas turetiminin yerine gecer)
    s["level"] = num(r'başlangıç basamağı\s*=\s*(\d+)', body)

    # beklenenler
    s["e_pool"] = num(r'Havuz büyüklüğü[^|]*\|\s*\*{0,2}(\d+)', body)
    s["e_committed"] = num(r'committed_duration_minutes`?\s*\|\s*\*{0,2}(\d+)', body)
    s["e_total"] = num(r'total_duration_minutes`?\s*\|\s*\*{0,2}(\d+)', body)
    s["e_fallback"] = num(r'fallback_level`?\s*\|\s*\*{0,2}(\d+)', body)
    s["e_count"] = num(r'Plan etkinlik sayısı\s*\|\s*\*{0,2}(\d+)', body)

    plan = []
    for m in re.finditer(r'^\|\s*(Gelişim|Güçlendirme|Keşif)\s*\|\s*\*{0,2}(\d+)\*{0,2}\s*\|', body, flags=re.M):
        _id = int(m.group(2))
        plan.append((SLOT[m.group(1)], _id))
    s["e_plan"] = plan
    return s


def main():
    text = open(DOC, encoding="utf-8").read()
    acts = load_activities()
    scen = split_scenarios(text)

    runnable, skipped = [], []
    for code, body in scen:
        s = parse(body)
        if s["age"] is None or s["quad"] is None or s["bmax"] is None or not s["e_plan"]:
            skipped.append((code, s))
        else:
            runnable.append((code, s))

    print(f"toplam senaryo {len(scen)} | plan uretimi kosulabilir {len(runnable)} | diger {len(skipped)}\n")
    ok = bad = 0
    rows = []
    for code, s in runnable:
        c = Child(s["age"], s["quad"], s["anx"] if s["anx"] is not None else 2,
                  s["bmin"] or 0, s["bmax"], focus=s["focus"], gardner=s["gardner"])
        if s["level"] is not None:
            c.L0 = s["level"]
        r = build_plan(c, acts, cid=s["cid"])
        got = [(slot, a.id) for slot, a, w in r["plan"]]
        diffs = []
        if s["e_pool"] is not None and r["pool"] != s["e_pool"]:
            diffs.append(f"havuz {r['pool']}!={s['e_pool']}")
        if s["e_committed"] is not None and r["committed"] != s["e_committed"]:
            diffs.append(f"committed {r['committed']}!={s['e_committed']}")
        if s["e_total"] is not None and r["total"] != s["e_total"]:
            diffs.append(f"total {r['total']}!={s['e_total']}")
        if s["e_fallback"] is not None and r["fallback"] != s["e_fallback"]:
            diffs.append(f"fallback {r['fallback']}!={s['e_fallback']}")
        if got != s["e_plan"]:
            diffs.append(f"plan {got} != {s['e_plan']}")
        if diffs:
            bad += 1
            rows.append((code, "FARK", "; ".join(diffs)[:150]))
        else:
            ok += 1
            rows.append((code, "TAM", ""))

    for code, st, note in rows:
        mark = "OK " if st == "TAM" else "XX "
        print(f"{mark}{code:9} {note}")
    print(f"\nTUTAN {ok} / {len(runnable)}   FARKLI {bad}")
    print("\nplan uretmeyen / ayristirilamayan senaryolar:")
    print("  " + ", ".join(c for c, _ in skipped))


if __name__ == "__main__":
    main()
