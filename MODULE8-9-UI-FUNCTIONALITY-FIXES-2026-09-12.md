# SmartHire AI — Module 8/9 UI & Functionality Fixes

Date: 2026-09-12

## Fixed from browser validation feedback

1. Candidate search bars now work through a global SmartHire search panel and Ctrl/Cmd+K shortcut.
2. Notification button/panel now appears as a real floating popup. CSS is loaded on candidate pages and the notification initializer is coordinated with the global UI injector.
3. Notification unread badge is driven by persisted notification records.
4. Candidate/secondary pages now get a consistent notification control.
5. Reports now download the selected interview's PDF, not always the latest candidate analytics PDF.
6. A dedicated `/api/analytics/interview/{interviewId}/report/pdf` backend endpoint was added with authorization.
7. Detailed Interview Report link in Report Center now resolves to the latest evaluated interview instead of opening without an interview ID.
8. Practice Assessment was rebuilt as a working page rather than links back to the dashboard.
9. Coding Practice now contains six coding questions, a timer, navigation, answer editor, scoring, and persistence through the candidate assessment API.
10. Aptitude Assessment now contains ten MCQs, a timer, navigation, scoring, and persistence.
11. Candidate Dashboard now includes a direct Coding Practice entry.
12. Performance Analytics/API calls now use `http://localhost:8080` automatically during the separate local frontend-on-5500 development setup instead of incorrectly calling port 5500.
13. The same local API-base correction was applied to auth/script fallback logic.
14. Improvement Progress was made resilient to empty/no-data states and correctly communicates when two evaluated interviews are required for trend measurement.
15. Settings now includes real candidate-useful preferences: interview defaults, difficulty, duration, email notifications, interview reminders, AI insights, camera/microphone test, local-cache control, reduced motion, and save/reset behavior.
16. Interview Setup now reads the saved default interview/difficulty/duration settings.
17. Custom CNN, MediaPipe, DeepFace, Whisper, and object-detection development URLs now have localhost development defaults while remaining environment-overridable for production.
18. Interview-style cards were visually upgraded so icons are visible in gradient icon blocks rather than washed-out white boxes.

## Validation performed in this environment

- 27 JavaScript files passed `node --check`.
- All Python AI-service files passed `py_compile`.
- All HTML JS/CSS local asset references resolve.
- `docker-compose.yml` parses as YAML.
- `render.yaml` parses as YAML.

## Backend test note

Fresh Maven execution could not be run in this sandbox because the Maven distribution/dependencies were not available. The user previously executed the Windows project tests successfully with 10 tests, 0 failures, 0 errors, 0 skipped. Because this revision adds a new backend PDF endpoint, Maven tests should be rerun in the user's Windows environment before committing.
