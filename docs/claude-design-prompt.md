# Claude Design — Kidloop Thesis Defence Deck

Aşağıdaki bloğun tamamını Claude Design'a yapıştırın. Blok kendi kendine
yeterlidir: içindeki her sayı 3 Eylül 2026'da `ufkumal/kinloop-be @ 46d3e5b`
üzerinde ölçülmüştür. **Bloktaki hiçbir sayıyı değiştirmeyin ve modele
"eksik veriyi tahmin et" alanı bırakmayın.**

---

```
Build a 31-slide + 9-appendix graduation thesis defence deck for a software
product called Kidloop. 16:9. English throughout.

════════════════════════════════════════════════════════════════════
PART 1 — VISUAL AND STRUCTURAL IDIOM (this is the hard requirement)
════════════════════════════════════════════════════════════════════

The deck must read like a KPMG or McKinsey client deliverable, not like a
university slide set and not like a startup pitch. Concretely:

TITLES ARE FINDINGS, NOT TOPICS.
Every content slide carries a full-sentence action title stating the
conclusion of that slide. "Elimination, not penalty, is what stops
repetition" — never "Elimination Layer". The title is the argument; the body
is the evidence for it. A reader who reads only the 31 titles in sequence
must receive the complete argument. This rule has no exceptions.

PYRAMID STRUCTURE.
Answer first, support second. Slide 2 is a one-page executive summary
carrying the governing claim, three supporting pillars, the evidence base,
and the single honest caveat. Everything after slide 2 is support for slide 2.

HORIZONTAL LOGIC.
Within each section the titles must chain: each one follows from the one
before. Reading titles 9→15 must feel like a single continuous argument
about the engine, not seven unrelated headings.

ONE MESSAGE PER SLIDE.
If a slide needs two verbs in its title, it is two slides.

LAYOUT GRAMMAR.
- 12-column grid, generous margins, disciplined whitespace. Content occupies
  roughly the central 80%; nothing touches the edges.
- Thin section rule at the top left carrying the section number and name in
  small caps letterspaced (e.g. "03 · ALGORITHM"), and slide numbers bottom
  right. Consistent on every slide.
- A "so-what" strip along the bottom of dense slides: one italic sentence in
  muted colour restating the takeaway. Consulting decks always carry this.
- Prefer structured objects over prose: staged process chevrons, comparison
  tables with a highlighted recommended column, 2x2s, waterfall/funnel bars
  for the pool reduction, before/after two-column blocks for the v5→v6 slide.
- Bullets are a last resort. Never more than four per slide, never nested,
  never a full sentence where a fragment works.
- Every number that matters gets a large numeral treatment with a small
  label beneath it, not a sentence containing a digit.
- Source/footnote line in 8pt at the bottom left wherever a figure is
  asserted, e.g. "Source: kinloop-be @ 46d3e5b, mvn test, 3 Sep 2026".

TYPOGRAPHY.
One serif for slide titles (authoritative, e.g. Source Serif 4 / Tiempos /
Georgia fallback) and one neutral grotesque for everything else (Inter /
Söhne / Helvetica Neue fallback). Monospace only for code, JSON and formulas.
Title ~30pt, body ~14–16pt, labels ~10pt small caps letterspaced, footnotes
8pt. Nothing below 8pt. No more than three sizes on any single slide.

COLOUR.
Restrained institutional palette, not a rainbow:
- One deep primary (navy or petrol blue) for structure, titles and rules.
- One warm accent (ochre or burnt orange) used sparingly — only to mark the
  single most important element on a slide. If two things are accented, one
  of them is wrong.
- A four-step cool grey ramp for everything else.
- Semantic colour only where semantics exist: green for verified, amber for
  open issue, grey for not-yet-measured. Never decorative.
Backgrounds are white or near-white. Full-bleed dark slides only for the
title, the three section dividers, and the closing slide.

WHAT TO AVOID.
No gradients, no drop shadows, no rounded "card" chrome, no stock
photography, no clip art, no emoji, no icon sets with cartoon character, no
3D charts, no chart junk, no gridlines that are not needed, no logos of
third-party companies. Charts have no legend if direct labelling will do.

════════════════════════════════════════════════════════════════════
PART 2 — THE PROJECT (all facts below are verified; do not invent any others)
════════════════════════════════════════════════════════════════════

Kidloop is a rule-based activity recommendation platform for early childhood
development (ages 0–72 months) with an LLM-assisted feedback interpretation
layer. Sabancı University, MSIT graduation project, September 2026.
Authors: Gülçin Eker Aktürk (38104) — product, rules, pedagogy, test design;
Ufkum Deniz Altunkapak (38263) — code, infrastructure, deployment.
Supervisor: Emre Kaplan, Ph.D.

THE PROBLEM
Parents ask "what should we do today?" every morning and answer it from
scratch every morning. Hundreds of activity ideas exist online; none of them
know this particular child's sensory sensitivity, skill level or interests.
The burden of choice stays with the parent and repeats daily. Existing tools
in Turkey are either preschool management software or generic content
libraries — none recommends from an individual child profile.

THE SCORING MODEL
score = (100 − D + G + P + Z) × B
- D · sensory distance (Ayres 1972, Dunn 1997): penalty when activity load
  exceeds the child's tolerance on noise, visual and movement.
  D = Σ weight × max(0, load − tolerance)
- G · interest bonus (Gardner 1983, eight intelligences): +10 if target
  intelligence ≥ 4.0; +15 if target is weak but secondary is strong;
  −15 if target < 1.5.
- P · period task (Piaget 1952): +15 when the activity domain matches the
  developmental task of the child's age band.
- Z · difficulty fit (Vygotsky 1978, ZPD): +20 one level above the child,
  0 same level, −5 below, −25 more than one above.
- B · attachment multiplier (Bowlby 1969): together-activities × 1.15 when
  separation anxiety is high.

DUNN QUADRANTS (tolerance · weight, on noise / visual / movement)
- C1 Calm observer:      3·5   3·5   3·3
- C2 Energetic explorer: 4·3   4·3   5·6
- C3 Sensitive watcher:  2·10  2·10  3·6   → heavy penalty; a two-point noise
      gap costs 20 points, enough to drop an activity out of the top three
- C4 Avoiding:           1·—   2·—   2·—   → ELIMINATION, not penalty: any
      activity with noise or visual load ≥ 3 leaves the pool before scoring.
      A safety decision, not a score adjustment.

LEVELS
Starting level by age: 0–48 mo L1 · 48–60 mo L2 · 60–72 mo L3.
A difficulty-sensitive streak counter per child: liked an easy activity +0.5,
liked a hard one +1.0. Streak reaches +3.0 → level up; drops below −1.0 →
level down.

ENGINE PIPELINE
Pool of 243 consultant-approved home activities (0–72 months)
→ 1 ELIMINATION → 20–45 remain (age, duration, sensory, anxiety, focus,
  freshness)
→ 2 SCORING → (100 − D + G + P + Z) × B on every remaining activity, per
  child, per day
→ 3 PLAN → 3 slots: Develop · Strengthen · Explore

ELIMINATION LAYER
In SQL: age range, duration ceiling (time budget), publication status/scope.
In code (ActivityEligibilityPolicy + FreshnessPolicy): sensory C4 (noise or
visual ≥ 3 removed); anxiety ≥ 4 (independent activities removed); short
focus (anything over 10 minutes removed); freshness (recently used removed,
window = max(2, ceil(pool/6)) days, shrinking with small pools).

PLAN CONSTRUCTION
Slot Develop = period-task domain (Piaget). Slot Strengthen = highest Gardner
score. Slot Explore = least-sampled intelligence.
Ties broken in five stages: score → sampling count → sensory load → duration
→ seeded draw, seed = (childId × 1000003 + YYYYMMDD × 10007 + activityId)
mod 2^31. Deterministic: refreshing the page returns the same plan.
Supervised guarantee: anxious child with no together-activity in the plan →
the Explore slot is swapped.
Five-step degradation ladder: strict → reintroduce recent → relax slot/domain
rules → partial plan → explanation screen.

FEEDBACK — THREE BUTTONS
- Liked: target intelligence +0.30, secondary +0.15, streak by difficulty
  +1.0 / +0.5 / 0
- Struggled: Gardner scores untouched, streak −0.5 / −1.0. Struggling says
  something about skill, not about interest — so interest is left alone.
- Disliked: diagnostic chain → (1) load above tolerance → SENSORY, no score
  change; (2) independent + anxious → INVOLVEMENT, no score change;
  (3) otherwise → INTEREST, −0.15.

LLM LAYER (Anthropic Claude, backend)
Parent presses a button and may add free text. Claude returns structured
output: a 9-field JSON schema (signal_type, secondary_hint, sensory_hint,
involvement_hint, difficulty_hint, duration_hint, situation_hint,
target_correction, confidence). The engine then applies a magnitude read
from the database.
SIX BRAKES: (1) confidence threshold 0.70 — below it the text is ignored;
(2) the button always wins over the text; (3) text can never produce a
penalty; (4) at most 3 intelligences touched per comment; (5) second-hand
reports capped at 0.60 of the weight; (6) delta cap per day.
The model never produces a number — only a direction. Magnitudes live in
scoring_parameters.
Prompt carries 20 many-shot examples.
Two hard cases: TRANSIENT ("she was sick today, didn't want anything") → no
score, streak or level is updated; target_correction ("honestly he never even
picked it up" under a Liked button) → the target intelligence receives no
credit and no penalty either; the button stays valid, the text only withholds.

ETHICS AND DATA PROTECTION (verified in code — give this a full slide)
- Data minimisation: the payload sent to the LLM contains ONLY the activity
  title, target intelligence, secondary intelligence, the button pressed, and
  the parent's free text. No child name, no age, no ID, no profile ever
  leaves the server.
- Consent gate: without DATA_PROCESSING consent the call is never made at
  all. Consent types held: TERMS, PRIVACY, KVKK, MARKETING, DATA_PROCESSING.
- Fail-open: if the LLM call fails, ordinary button learning continues and no
  provider or internal error detail reaches the API response.
- The model influences direction only; every magnitude is a database value.

ARCHITECTURE
- Vercel · frontend: Next.js 16, React 19. Web + Android via Capacitor.
  Onboarding, daily plan, feedback. Gemini voice input through a Next.js API
  route.
- Render · backend: Spring Boot 3.3.2, Java 21. REST API (22 endpoints),
  matching engine, feedback learning, Claude classification call.
- Neon · database: PostgreSQL. 39 Flyway migrations, 243 activities,
  65 scoring parameters, per-child profile state.
- Delivery: GitHub → merge to main → automatic deploy on Render and Vercel.
- Managed platforms chosen over AWS/Azure: zero ops for a two-person team.

DATABASE AS CONFIGURATION
65 engine parameters in one scoring_parameters table — no numeric constants
in Java. Zero deploys needed to recalibrate: a value changes with a
migration, the engine reads it on the next request. 39 migrations readable as
a design log; V20 alone records 15 decisions of the v5 → v6 alignment, most
without touching Java.

VERIFICATION — THREE LAYERS
- Automated: 159 tests executed, 0 failures, 27 skipped because they require
  Docker; 162 @Test methods across 39 test classes in source. Testcontainers,
  running against the real schema.
- Reference: an independent hand computation matched the engine exactly on
  all 18 profiles (18/18).
- Live: four child profiles on Vercel + Render (30 mo C1, 30 mo C4, 54 mo C3,
  72 mo); 16 v6 rules confirmed from database state.
State plainly on the validity slide: these establish INTERNAL correctness —
the engine does what the specification says. They do NOT establish
pedagogical effectiveness; no real parent has used the system yet.

WHAT SIMULATION FOUND (v5 → v6) — before/after, four rows
1. Freshness as a score penalty → same activity returned 7 days in a row in a
   7-day run → moved to elimination with a pool-relative window.
2. Maximise total plan score → slots mixed up in 12 of 12 scenarios, because
   a sum is order-blind → slot-by-slot construction with domain rules.
3. Streak counts "times liked" → always-succeeding, realistic and struggling
   profiles all reached the same level in a 30-day run → difficulty-sensitive
   credit +1.0 / +0.5.
4. Everyone starts at level 1 → for a 60-month-old, 38 of 41 activities
   scored −25 and the level never moved → age-based starting level L1/L2/L3.
Each fix is one of 38 entries in the decision matrix: rationale · simulation
result · pedagogical sign-off.

OPEN ISSUES (present these honestly; do not soften)
- The disliked-diagnosis sensory branch runs only for C3 / C4 profiles; a
  C1 or C2 child exceeding tolerance is not diagnosed as sensory.
- The INVOLVEMENT diagnosis is designed but currently unreachable: anxious
  children already have independent activities eliminated from the pool, so
  no such item can be voted on. A 237-vote sweep produced zero INVOLVEMENT
  rows.
- No server timezone is configured; LocalDate.now() is called in 10+ places
  and the host runs UTC, so between 00:00 and 03:00 Turkish time a parent is
  served the previous day's plan.
- The budget lower bound is not enforced.
- The LLM confidence threshold has not yet been calibrated against data.
- Content gap in the 36–48 month LANGUAGE band.
- 27 automated tests have never actually executed (Docker-gated).
- The LLM path has not been exercised end-to-end against the live provider.

DEVIATIONS FROM THE PROPOSAL
Workshop / provider side deferred; the engine matured on home activities
first. Live interaction data used instead of synthetic personas.
OpenAI replaced by Anthropic Claude.

NOT DONE
Load testing. Real end users. CI pipeline.

ROADMAP
Sep–Oct: production hardening. Closed beta with 30–50 parents.
Oct–Nov: workshop phase. RAG explanation layer grounding each engine decision
in pedagogical sources.

CLOSING NUMBERS
243 activities · 65 engine parameters · 39 migrations · 22 API endpoints ·
5 theories · 5 sensory types · 8 Gardner intelligences · 7 developmental
domains · 38 decision-matrix entries · 159 tests executed (27 skipped) ·
18/18 reference matches · 4 live profiles · 16 rules verified · 20 many-shot
examples · 6 brakes · 9 LLM schema fields

REFERENCES
Ayres 1972 · Bowlby 1969 · Dunn 1997 · Gardner 1983 · Piaget 1952 ·
Vygotsky 1978

════════════════════════════════════════════════════════════════════
PART 3 — SLIDE PLAN (build exactly these, in this order)
════════════════════════════════════════════════════════════════════

 1  Title — dark full bleed. Project name, subtitle, the formula as a quiet
    typographic motif, two authors with student numbers, supervisor,
    institution, date.
 2  EXECUTIVE SUMMARY — one page: governing claim, three supporting pillars,
    evidence base, one honest caveat. This is the most important slide.
 3  The problem — four stacked statements, no bullets, generous leading.
 4  Research question and scope boundary. Include the explicit non-scope:
    Kidloop is not a diagnostic or therapeutic instrument and makes no
    clinical claim.
 5  Related work and the gap — comparison table: Kidloop vs. generic content
    libraries vs. preschool management software vs. general-purpose
    recommenders. Columns: individual profile · sensory adaptation ·
    explainability · feedback loop · theoretical grounding. Highlight the
    Kidloop column.
 6  Why rule-based and not machine learning — four reasons as a 2x2 or four
    columns: cold start with zero users; a 243-item pool is too small for
    collaborative filtering; explainability is mandatory for parents and for
    pedagogical accountability; data minimisation under KVKK. Frame as a
    defensible design decision, not a limitation.
 7  The solution — the loop: Onboarding → Daily plan → Activity → Feedback →
    Next plan, as a closed cycle, with the four differentiators around it.
 8  Method — iterative design with a decision matrix as the method artifact;
    how the reference calculation was constructed; how simulation was used
    as an evidence source.
 9  Five theories, five terms — the formula decomposed, one column per term,
    each with symbol, name, citation and effect.
10  Sensory profiles (Dunn) — the quadrant table, with C4's elimination rule
    and C3's heavy weights called out as two annotations.
11  Difficulty fit and levels — starting level by age, the Z ladder, and how
    the streak moves.
12  Onboarding — four-question summary only, each mapped to the engine input
    it produces. Full matrix goes to appendix A1.
13  Engine pipeline — three staged chevrons with the pool count falling
    243 → 20–45 → 3.
14  Elimination layer — SQL side vs. code side, two columns.
15  Plan construction and tie-breaking — three slots, the five tie-break
    stages as a horizontal chain, the supervised guarantee and the
    degradation ladder as two annotations.
16  Worked example 1 — a 30-month avoiding child (C4, anxiety 5, 15–25 min
    budget). Pool funnel 44 → 26 → 22 → 19 as a waterfall. Resulting plan:
    Develop #71 Language supervised 10 min · Strengthen #58 Motor supervised
    5 min · Explore #146 Music together 5 min. Total 20 min within budget,
    3/3 supervised guarantee met, matches the reference calculation.
17  Worked example 2 — Ada, 54 mo, C3, 35–45 min, days 1–3. Day 1 all eight
    intelligences at 3.00, Strengthen falls back to the period-task domain.
    Votes: liked social, struggled motor, disliked music. Day 2 Interpersonal
    3.30, Musical 2.85. Day 3 Interpersonal 4.35 crosses the 4.0 bonus
    threshold, so G = +10 on social activities. Measured directly in the
    database after each day.
18  Worked example 3 — the degradation ladder firing: a child whose pool runs
    out, shown degrading strict → reintroduce recent → relax slot/domain
    rules → partial plan → explanation screen. Title it around the idea that
    the system is designed to say "not today" rather than to recommend
    something unsafe.
19  Three vote types and the diagnostic chain — and mark on the slide itself
    that the sensory branch currently runs only for C3/C4.
20  Free-text interpretation with an LLM — the parent's sentence, the JSON
    that comes back, the delta the engine applies, and the six brakes as a
    numbered column beside it.
21  Transient states and contradictions — the two hard cases side by side.
22  Ethics, KVKK and data minimisation — the exact LLM payload shown as a
    five-line block, the consent gate, fail-open behaviour, and the
    "direction not magnitude" architectural guarantee.
23  Architecture and database-as-configuration — the three managed platforms,
    the external services, and the 65 / 0 / 39 figures.
24  The product — a placeholder slide laid out for four screenshots
    (onboarding question, daily plan, feedback with free text, plan
    explanation) with caption slots beneath each. Leave clearly marked image
    frames; do not generate fake UI.
25  Three-layer verification — automated / reference / live, with the skip
    count visible.
26  Validity — what these numbers establish and what they do not. Two
    columns: internal correctness (established) vs. pedagogical
    effectiveness (not established). Name the study that would establish the
    second: closed beta, 30–50 parents, what is measured, over what period.
27  From v5 to v6 — the four-row before/after table. This is the argumentative
    centre of the deck; give it the strongest treatment.
28  Limitations, open issues and roadmap — three columns, all eight open
    issues listed.
29  Individual contributions — two columns mapping each author to concrete
    deliverables.
30  Kidloop in numbers + thank you — dark full bleed, the numeral grid.
31  References.

APPENDIX (nine slides, marked as appendix, same grid, plainer treatment)
A1 Full onboarding question matrix by age band
A2 The 65-row scoring_parameters table
A3 The 38-entry decision matrix
A4 The five-step degradation ladder in full
A5 A complete LLM prompt and response
A6 Coverage map of the 20 many-shot examples, including the two known gaps:
   no example for duration_hint = SHORT, and BODILY_KINAESTHETIC never
   appears as an output label
A7 Database ERD
A8 Derivation of the tie-break seed
A9 Test inventory: 159 executed / 27 skipped, class by class

════════════════════════════════════════════════════════════════════
PART 4 — RULES FOR YOU
════════════════════════════════════════════════════════════════════

- Use only the facts given above. If a slide needs a figure that is not
  listed, put a clearly marked placeholder — never invent a number, a
  benchmark, a competitor claim, or a citation.
- Slide 5 (related work) and slide 29 (individual contributions) contain
  material I have not supplied in full. Build the layout and mark the cells
  that need filling; do not fabricate competitor capabilities.
- Slide 24 must contain empty, clearly labelled image frames. Do not draw
  imaginary app screens.
- Every content slide gets a full-sentence action title. Check at the end
  that the 31 titles read as one continuous argument on their own.
- Keep every slide readable from the back of a lecture room: nothing below
  8pt, no table wider than seven columns in the main body.
- Deliver the deck plus a separate one-page list of the 31 action titles in
  order, so I can check the horizontal logic.
```
