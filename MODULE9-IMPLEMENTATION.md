# Module 9 — Notifications & Reports

## Notification Center
Implemented persistent `user_notifications` storage with:

`GET /api/notifications`
`POST /api/notifications/{id}/read`
`POST /api/notifications/read-all`
`POST /api/notifications/reminders`

Candidate notification events are derived from real stored data:
- resume analysis available
- interview session started
- interview session completed
- evaluation completed
- report ready
- recording available

No fabricated default notifications are returned when a candidate has no activity.

Reminder creation requires a future due time and stores the reminder persistently.

## Recruiter/Admin Notifications
Recruiter and admin notification feeds use actual recent interview/evaluation platform activity and do not expose private candidate content inside the notification payload.

## Reports
The candidate report center now provides:
- stored report history
- detailed report navigation
- analytics PDF download
- email report action

## Email
`POST /api/email/interview-report/{interviewId}` remains the report-email endpoint. Authorization was tightened so only the candidate who owns the interview, an authorized recruiter, or an admin can send that report.

SMTP remains environment-configured; the application returns a clear configuration error rather than pretending a message was sent.
