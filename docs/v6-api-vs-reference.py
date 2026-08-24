"""Ufkum'un canli API'si ile bagimsiz v6 referans motorunu ayni profillerde karsilastirir."""
import json
import subprocess
from datetime import date

import v6engine as E

B = "http://127.0.0.1:8080"
PG = ["psql", "-h", "127.0.0.1", "-p", "5433", "-U", "kinloop", "-d", "kinloop", "-tAc"]
SLOTMAP = {"DEVELOP": "Gelisim", "STRENGTHEN": "Guclendirme", "EXPLORE": "Kesif"}


def sh(a):
    return subprocess.run(a, capture_output=True, text=True).stdout.strip()


def curl(m, p, tok=None, body=None):
    c = ["curl", "-sS", "-X", m, B + p, "-H", "Content-Type: application/json"]
    if tok:
        c += ["-H", "Authorization: Bearer " + tok]
    if body is not None:
        c += ["-d", json.dumps(body)]
    o = sh(c)
    try:
        return json.loads(o) if o else {}
    except json.JSONDecodeError:
        return {"_raw": o}


def months_ago(n):
    t = date.today()
    y, m = t.year, t.month - n
    while m <= 0:
        m += 12
        y -= 1
    return date(y, m, min(t.day, 28)).isoformat()


def login(email):
    curl("POST", "/api/auth/register", body={"name": "Anne", "email": email,
                                             "password": "Password123", "role": "PARENT"})
    tok = sh(PG + ["SELECT t.token FROM email_verification_tokens t JOIN users u ON u.id=t.user_id "
                   f"WHERE u.email='{email}' ORDER BY t.id DESC LIMIT 1"])
    sh(["curl", "-sS", f"{B}/api/auth/verify?token={tok}"])
    return curl("POST", "/api/auth/login", body={"email": email, "password": "Password123"})["token"]


PROFILES = [
    #  etiket                  yas  quad    kaygi  bmin bmax  focus
    ("30ay C1 kaygi2  B",       30, "C1",   2,     25, 35, None),
    ("30ay C4 kaygi5  B",       30, "C4",   5,     25, 35, None),
    ("30ay C3 kaygi5  B",       30, "C3",   5,     25, 35, None),
    ("30ay C2 kaygi2  B",       30, "C2",   2,     25, 35, None),
    ("30ay MIXED k2   B",       30, "MIXED", 2,    25, 35, None),
    ("8ay  C4 kaygi5  A",        8, "C4",   5,     15, 25, None),
    ("18ay C4 kaygi5  A",       18, "C4",   5,     15, 25, None),
    ("54ay C4 kaygi5  C",       54, "C4",   5,     35, 45, None),
    ("66ay C1 kaygi2  C",       66, "C1",   2,     35, 45, None),
    ("72ay C1 kaygi2  B",       72, "C1",   2,     25, 35, None),
    ("30ay C1 kaygi2  A",       30, "C1",   2,     15, 25, None),
    ("60ay C3 kaygi5  C",       60, "C3",   5,     35, 45, None),
]
QUAD_ANS = {"C1": "A", "C2": "B", "C3": "C", "C4": "D", "MIXED": "E"}

tok = login("cmp@example.com")
acts = E.load_activities()

print(f"{'profil':22} {'API plani':34} {'referans motor':34} {''}")
print("-" * 100)
agree = 0
rows = []
for lbl, age, quad, anx, bmin, bmax, focus in PROFILES:
    ch = curl("POST", "/api/children", tok, {"fullName": lbl[:20], "birthDate": months_ago(age),
                                             "gender": "OTHER", "dailyTimeBudgetOptionCode": "B"})
    cid = ch.get("childId")
    if not cid:
        rows.append((lbl, "cocuk olusmadi", "", False)); continue
    # butce hatasini bypass et: dogru araligi dogrudan yaz
    sh(PG + [f"UPDATE children SET daily_time_budget_min={bmin}, daily_time_budget_max={bmax} WHERE id={cid}"])
    ans = {"Q2": QUAD_ANS[quad], "Q2b": QUAD_ANS[quad], "Q3": "C",
           "Q4": str(anx), "Q4b": str(anx), "Q4c": str(anx), "Q5": "B", "Q6": "B"}
    for _ in range(10):
        cur = curl("GET", f"/api/children/{cid}/questionnaire/current", tok)
        nx = (cur.get("session") or cur).get("nextQuestionCode")
        if not nx:
            break
        curl("PUT", f"/api/children/{cid}/questionnaire/answers/{nx}", tok, {"optionCode": ans[nx]})
    curl("POST", f"/api/children/{cid}/questionnaire/complete", tok)
    plan = curl("GET", f"/api/children/{cid}/daily-plan/today", tok)
    api = [(SLOTMAP.get(a["slotType"], a["slotType"]), a["activityId"]) for a in plan.get("activities", [])]
    api.sort(key=lambda x: {"Gelisim": 0, "Guclendirme": 1, "Kesif": 2}.get(x[0], 9))

    c = E.Child(age, quad, anx, bmin, bmax, focus=focus)
    r = E.build_plan(c, acts, plan_date=date.today().isoformat(), cid=cid)
    ref = [(s, a.id) for s, a, w in r["plan"]]

    same = api == ref
    agree += same
    rows.append((lbl, " ".join(f"{s[:3]}:{i}" for s, i in api),
                 " ".join(f"{s[:3]}:{i}" for s, i in ref), same))

for lbl, a, b, same in rows:
    print(f"{lbl:22} {a:34} {b:34} {'AYNI' if same else 'FARKLI'}")
print(f"\nuyusan: {agree} / {len(rows)}")
