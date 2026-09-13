# API Documentation

## Authentication
- POST `/api/auth/register`
- POST `/api/auth/login`

## Resume
- POST `/api/resume/upload`
- POST `/api/resume/extract`
- POST `/api/resume/analyze`
- GET `/api/resume/report/{id}`

## Interview Core
- POST `/api/interviews/start`
- POST `/api/interviews/evaluate`
- POST `/api/interviews/followup`
- POST `/api/interviews/{interviewId}/session`
- GET `/api/interviews/{interviewId}/report`
- GET `/api/interviews/{interviewId}/report/email-preview`
- GET `/api/interviews/history/{userId}`
- GET `/api/interviews/history/{userId}/{interviewId}`

## Candidate Enhancements
- POST `/api/interviews/candidate/{userId}/career-roadmap/generate`
- GET `/api/interviews/candidate/{userId}/enhancements`
- POST `/api/interviews/candidate/{userId}/assessments`
- POST `/api/interviews/candidate/{userId}/profile-completion`
- POST `/api/interviews/candidate/{userId}/notifications`

## Recruiter
- GET `/api/recruiter/candidates`
- GET `/api/recruiter/candidates/{candidateId}`
- POST `/api/recruiter/candidates/{candidateId}/{actionType}`

## Admin
- GET `/api/admin/dashboard`


## Authentication Management
- POST `/api/auth/forgot-password`
- POST `/api/auth/reset-password`
- GET `/oauth2/authorization/google` (when the `oauth` Spring profile is enabled)

## Speech AI
- POST `/api/ai/speech/transcribe`

## Recruiter Jobs
- GET `/api/recruiter/jobs`
- POST `/api/recruiter/jobs`
- PUT `/api/recruiter/jobs/{id}`
- DELETE `/api/recruiter/jobs/{id}`

## Recruiter Interview Templates
- GET `/api/recruiter/templates`
- POST `/api/recruiter/templates`
- PUT `/api/recruiter/templates/{id}`
- DELETE `/api/recruiter/templates/{id}`

## Admin User Management
- GET `/api/admin/users`
- POST `/api/admin/users`
- PUT `/api/admin/users/{id}`
- DELETE `/api/admin/users/{id}`
- POST `/api/admin/actions/{action}`

## Module 8 Analytics APIs

### Candidate Analytics
`GET /api/analytics/candidate/{userId}`

Requires authentication. Candidate users can access only their own ID; recruiters/admins can access candidate analytics.

Returns summary, history, trends, dimension averages, weak areas, improvement progress, latest AI feedback and resume skill signals.

### Recruiter Analytics
`GET /api/analytics/recruiter`

Requires recruiter/admin role. Returns hiring-pool performance, candidate ranking, skill analytics, weak-skill signals and trends.

## Module 9 Notification APIs

### Notification Feed
`GET /api/notifications`

Returns persisted notifications and real event-derived notification items for the authenticated user.

### Mark Notification Read
`POST /api/notifications/{id}/read`

Marks one owned notification as read.

### Mark All Read
`POST /api/notifications/read-all`

Marks the authenticated user's notifications as read.

### Create Interview Reminder
`POST /api/notifications/reminders`

Body:
```json
{
  "title": "Interview reminder",
  "message": "Your SmartHire AI practice reminder is due.",
  "actionUrl": "/pages/interview-setup.html",
  "dueAt": "2026-09-15T19:00:00"
}
```

### Analytics PDF
`GET /api/analytics/report/{userId}`

Protected candidate/recruiter/admin analytics report download.
