# SmartHire AI Isolation Fix – 2026-09-13

## What was fixed
- PowerShell `$PID` collision in AI launcher was removed by using service-safe variable names.
- AI environments are isolated under the project-root `.venvs` directory to shorten paths and avoid Windows TensorFlow/DeepFace long-path failures.
- AI service startup now validates the venv Python executable and service directory before launch.
- AI services are launched through `cmd.exe` using the service-specific venv Python. This avoids the Windows `Start-Process` "system cannot find the file specified" failure observed when launching venv `python.exe` directly.
- Each service writes stdout/stderr logs under `logs\`.
- `TEST-AI-SERVICES.cmd` was added for one-click health and process verification.

## Commands

```cmd
FIX-AI-AND-START.cmd
RUN-SMARTHIRE.cmd
TEST-AI-SERVICES.cmd
```

The rebuild command recreates the service environments. The regular launcher starts them without deleting the environments.
