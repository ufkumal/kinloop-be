"""Kidloop v6 referans motoru — Kidloop_Oneri_Prensibi_v6.md kurallarini birebir uygular.
Gercek veritabanini okur (243 etkinlik). Ufkum'un kodundan bagimsizdir."""
import hashlib
import subprocess
from math import ceil

PG = ["psql", "-h", "127.0.0.1", "-p", "5433", "-U", "kinloop", "-d", "kinloop", "-tA", "-F", "\x1f", "-c"]

# 1.2 dunn_profiles (v6 tablosu)
DUNN = {  # quadrant: (tol_ses, tol_gorsel, tol_hareket, w_ses, w_gorsel, w_hareket)
    "C1": (3, 3, 3, 5, 5, 3),
    "C2": (4, 4, 5, 3, 3, 6),
    "C3": (2, 2, 3, 10, 10, 6),
    "C4": (1, 2, 2, 5, 5, 3),
    "MIXED": (3, 3, 3, 5, 5, 3),
}

# 1.3 developmental_period_tasks (son bant 73)
PERIODS = [(0, 12, "GROSS_MOTOR"), (12, 24, "GROSS_MOTOR"),
           (24, 48, "LANGUAGE"), (48, 73, "SOCIAL_EMOTIONAL")]

# 1.7 scoring_parameters
P = dict(gardner_comfort_threshold=4.0, gardner_comfort_bonus=10.0,
         gardner_bridge_target_threshold=2.5, gardner_bridge_secondary_threshold=4.0,
         gardner_bridge_bonus=15.0, gardner_block_threshold=1.5, gardner_block_penalty=-15.0,
         developmental_period_bonus=15.0,
         zpd_sweet_spot_bonus=20.0, zpd_boredom_penalty=-5.0, zpd_frustration_penalty=-25.0,
         attachment_multiplier_together=1.15, attachment_multiplier_supervised=1.00,
         attachment_anxiety_threshold=4,
         freshness_window_divisor=6, freshness_window_min=2,
         tiebreak_seed_a=1000003, tiebreak_seed_b=10007, tiebreak_seed_mod=2147483647,
         level_max=4, level_min=1, score_base=100.0)


class Activity:
    __slots__ = ("id", "title", "domain", "intel", "sec_intel", "d", "dk", "inv",
                 "n", "v", "ph", "easier", "harder", "min_age", "max_age")

    def __repr__(self):
        return f"{self.id}"


def load_activities():
    q = """SELECT a.id, a.title, a.target_domain, a.target_intelligence,
                  coalesce(a.secondary_intelligence,''), a.difficulty, a.duration_minutes,
                  a.involvement_type, a.noise_load, a.visual_load, a.physical_intensity,
                  (ai.easier_variation IS NOT NULL AND ai.easier_variation <> '')::int,
                  (ai.harder_variation IS NOT NULL AND ai.harder_variation <> '')::int,
                  a.min_age_months, a.max_age_months
           FROM activities a LEFT JOIN activity_instructions ai ON ai.activity_id = a.id
           WHERE a.scope='HOME' AND a.status='PUBLISHED' AND a.deleted_at IS NULL"""
    out = subprocess.run(PG + [q], capture_output=True, text=True)
    if out.returncode:
        raise SystemExit(out.stderr[:400])
    acts = []
    for line in out.stdout.strip().splitlines():
        f = line.split("\x1f")
        a = Activity()
        (a.id, a.title, a.domain, a.intel, a.sec_intel, a.d, a.dk, a.inv,
         a.n, a.v, a.ph, a.easier, a.harder, a.min_age, a.max_age) = (
            int(f[0]), f[1], f[2], f[3], f[4] or None, int(f[5]), int(f[6]), f[7],
            int(f[8]), int(f[9]), int(f[10]), f[11] == "1", f[12] == "1", int(f[13]), int(f[14]))
        acts.append(a)
    return acts


def period_task(age):
    for lo, hi, dom in PERIODS:
        if lo <= age < hi:
            return dom
    return None


def initial_level(age):                      # 3.1 / EK6
    if age < 48:
        return 1
    if age < 60:
        return 2
    return 3


class Child:
    def __init__(self, age, quadrant, anxiety, budget_min, budget_max,
                 focus=None, gardner=None, levels=None, exposure=None, recent_plans=None):
        self.age = age
        self.quadrant = quadrant
        self.anxiety = anxiety
        self.bmin, self.bmax = budget_min, budget_max
        self.focus = focus
        self.gardner = gardner or {}          # varsayilan 3.00
        L0 = initial_level(age)
        self.levels = levels or {}
        self.L0 = L0
        self.exposure = exposure or {}        # zeka alani -> ornek sayisi
        self.recent_plans = recent_plans or []  # [[id,...], ...] en yeni once

    def g(self, intel):
        return self.gardner.get(intel, 3.00)

    def level(self, domain):
        return self.levels.get(domain, self.L0)


def pool(child, acts):                        # 3.4
    tol = DUNN[child.quadrant]
    out = [a for a in acts
           if a.min_age <= child.age <= a.max_age
           and a.dk <= child.bmax
           and not (child.quadrant == "C4" and max(a.n, a.v) >= 3)
           and not (child.anxiety >= P["attachment_anxiety_threshold"] and a.inv == "BAGIMSIZ")
           and not (child.focus == "SHORT" and a.dk > 10)]
    return out


def apply_freshness(candidates, child):       # 3.4 adim 7
    if not child.recent_plans:
        return candidates, 0
    N = max(P["freshness_window_min"], ceil(len(candidates) / P["freshness_window_divisor"]))
    banned = set()
    for plan in child.recent_plans[:N]:
        banned.update(plan)
    return [a for a in candidates if a.id not in banned], N


def score(a, child):                          # bolum 2
    tn, tv, tm, wn, wv, wm = DUNN[child.quadrant]
    D = wn * abs(tn - a.n) + wv * abs(tv - a.v) + wm * abs(tm - a.ph)

    tgt = child.g(a.intel)
    sec = child.g(a.sec_intel) if a.sec_intel else None
    if tgt >= P["gardner_comfort_threshold"]:
        G = P["gardner_comfort_bonus"]
    elif tgt <= P["gardner_bridge_target_threshold"] and sec is not None and sec >= P["gardner_bridge_secondary_threshold"]:
        G = P["gardner_bridge_bonus"]
    elif tgt <= P["gardner_block_threshold"]:
        G = P["gardner_block_penalty"]
    else:
        G = 0.0

    Pb = P["developmental_period_bonus"] if a.domain == period_task(child.age) else 0.0

    L = child.level(a.domain)
    if a.d == L + 1 and a.easier:
        Z = P["zpd_sweet_spot_bonus"]
    elif L == P["level_max"] and a.d == P["level_max"] and a.harder:
        Z = P["zpd_sweet_spot_bonus"]
    elif a.d == L:
        Z = 0.0
    elif a.d < L:
        Z = P["zpd_boredom_penalty"]
    elif a.d > L + 1:
        Z = P["zpd_frustration_penalty"]
    else:
        Z = 0.0                                # d = L+1 ama easier yok

    B = P["attachment_multiplier_together"] if (
        child.anxiety >= P["attachment_anxiety_threshold"] and a.inv == "BIRLIKTE") else 1.00

    raw = (P["score_base"] - D + G + Pb + Z) * B
    return round(raw, 2), dict(D=D, G=G, P=Pb, Z=Z, B=B)


def sort_key(a, child, plan_date, scores):    # 4.3 esitlik bozma zinciri (v6 revize)
    gun = int(plan_date.replace("-", ""))     # 2026-08-21 -> 20260821
    seed = (child.cid * P["tiebreak_seed_a"] + gun * P["tiebreak_seed_b"] + a.id) % P["tiebreak_seed_mod"]
    return (-scores[a.id][0],                 # 1 skor
            child.exposure.get(a.intel, 0),   # 2 ornekleme
            a.n + a.v + a.ph,                 # 3 duyusal yuk
            a.dk,                             # 4 sure (YENI)
            seed)                             # 5 tohumlu kura


def pick(cands, cap, child, plan_date, scores):
    fit = [a for a in cands if a.dk <= cap]
    if not fit:
        return None
    return sorted(fit, key=lambda a: sort_key(a, child, plan_date, scores))[0]


def build_plan(child, acts, plan_date="2026-08-21", cid=201):
    child.cid = cid
    base = pool(child, acts)
    pool_size = len(base)
    fresh, window = apply_freshness(base, child)

    for level in range(0, 5):                 # 4.5 kademeli geri cekilme
        cands = base if level >= 1 else fresh
        repeat = level >= 1
        relaxed = level >= 2
        sc = {a.id: score(a, child) for a in cands}
        if not cands:
            continue

        shortest = min(a.dk for a in cands)
        kalan = child.bmax
        chosen = []

        # ADIM 1 GELISIM
        dom = period_task(child.age)
        dev_c = cands if relaxed else [a for a in cands if a.domain == dom]
        if not dev_c:
            if level < 4:
                continue
            dev_c = cands
        dev = pick(dev_c, kalan - shortest, child, plan_date, sc) or pick(dev_c, kalan, child, plan_date, sc)
        if dev is None:
            continue
        chosen.append(("Gelisim", dev, True))
        kalan -= dev.dk

        # ADIM 2 GUCLENDIRME
        used = {dev.id}
        if relaxed:
            str_c = [a for a in cands if a.id not in used]
        else:
            best_g = max(child.g(i) for i in {a.intel for a in cands})
            order = sorted({a.intel for a in cands}, key=lambda i: -child.g(i))
            str_c = []
            if len({child.g(i) for i in order}) == 1:
                str_c = [a for a in cands if a.id not in used]
            else:
                for i in order[:3]:
                    str_c = [a for a in cands if a.intel == i and a.id not in used]
                    if str_c:
                        break
        st = pick(str_c, kalan, child, plan_date, sc)
        if st is not None:
            chosen.append(("Guclendirme", st, True))
            kalan -= st.dk
            used.add(st.id)

        # ADIM 3 KESIF
        if relaxed:
            exp_c = [a for a in cands if a.id not in used]
        else:
            least = min((child.exposure.get(i, 0) for i in {a.intel for a in cands}), default=0)
            exp_c = [a for a in cands if child.exposure.get(a.intel, 0) == least and a.id not in used]
            if not exp_c:
                exp_c = [a for a in cands if a.id not in used]
        ex = pick(exp_c, kalan, child, plan_date, sc)
        within = True
        if ex is None and exp_c:
            m = min(a.dk for a in exp_c)
            shortlist = [a for a in exp_c if a.dk == m]
            ex = sorted(shortlist, key=lambda a: sort_key(a, child, plan_date, sc))[0]
            within = False
        if ex is not None:
            chosen.append(("Kesif", ex, within))

        if level < 3 and len(chosen) < 3:
            continue

        # 4.4 gozetimli garantisi
        if child.anxiety >= P["attachment_anxiety_threshold"] and len(chosen) == 3:
            if not any(c[1].inv == "GOZETIMLI" for c in chosen):
                ids = {c[1].id for c in chosen}
                sup = [a for a in cands if a.inv == "GOZETIMLI" and a.id not in ids]
                if sup:
                    kalan_after = child.bmax - sum(c[1].dk for c in chosen[:2])
                    fit = [a for a in sup if a.dk <= kalan_after]
                    if fit:
                        rep = sorted(fit, key=lambda a: sort_key(a, child, plan_date, sc))[0]
                        chosen[2] = ("Kesif", rep, True)
                    else:
                        m = min(a.dk for a in sup)
                        short = [a for a in sup if a.dk == m]
                        rep = sorted(short, key=lambda a: sort_key(a, child, plan_date, sc))[0]
                        chosen[2] = ("Kesif", rep, False)

        committed = sum(c[1].dk for c in chosen if c[2])
        total = sum(c[1].dk for c in chosen)
        return dict(pool=pool_size, window=window, fallback=level, plan=chosen,
                    committed=committed, total=total, scores=sc, repeat=repeat)

    return dict(pool=pool_size, window=window, fallback=4, plan=[], committed=0, total=0,
                scores={}, repeat=False)


def counter_delta(vote, d, L):                # 3.2 revize
    if vote == "SEVDI":
        if d >= L + 1: return 1.0
        if d == L:     return 0.5
        return 0.0
    if vote == "ZORLANDI":
        if d > L + 1:  return 0.0
        if d == L + 1: return -0.5
        return -1.0
    return 0.0
