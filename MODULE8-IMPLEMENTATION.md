# Module 8 — Dashboard & Analytics

## Candidate Dashboard
Implemented a real PostgreSQL-backed analytics API:

`GET /api/analytics/candidate/{userId}`

The response includes:
- completed/evaluated interview counts
- average and latest weighted overall score
- communication, confidence, technical, professionalism and problem-solving averages
- improvement delta from first evaluated session to latest evaluated session
- rating
- interview history with per-dimension scores
- chronological performance trends
- weak-area identification from evaluated dimensions and resume missing-skill signals
- resume skill signals
- improvement baseline/latest/delta per dimension
- latest AI strengths, weaknesses, recommendations, practice recommendations and learning resources

The candidate UI now consumes this API on:
- Performance Analytics
- Improvement Progress
- Reports

## Recruiter Dashboard
Implemented:

`GET /api/analytics/recruiter`

Includes:
- candidate counts
- scored candidate counts
- average interview score
- average ATS score
- shortlist rate from stored candidate state
- deterministic readiness ranking from latest interview + ATS evidence
- dimension averages
- chronological interview performance trend
- top resume skills
- missing/weak skill signals

## Admin Dashboard
Admin analytics now displays measured metrics from the backend performance API instead of synthetic workload percentages:
- evaluation coverage
- AI success rate in the last 24 hours
- average overall evaluation score
- average professionalism score

## PDF
The existing analytics PDF endpoint remains:

`GET /api/analytics/report/{userId}`

The endpoint is protected by candidate ownership / recruiter / admin authorization.
