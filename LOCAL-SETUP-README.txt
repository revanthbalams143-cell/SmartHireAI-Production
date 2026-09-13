SMART HIRE AI - LOCAL START

1. Ensure PostgreSQL 18 service is installed/running.
2. Double-click RUN-SMARTHIRE.cmd.
3. On the first run, enter the PostgreSQL 'postgres' password ONCE.
4. The launcher verifies it, creates the 'smarthire' DB if missing, and stores the credential with Windows DPAPI for the same Windows user.
5. Future runs do not ask for the password unless the PostgreSQL password was changed.

URLs after startup:
Frontend: http://127.0.0.1:5500
Backend:  http://127.0.0.1:8080
CNN:      http://127.0.0.1:8095/health

Production/deployment must use cloud-managed PostgreSQL and environment secrets; never ship the local credential file.
