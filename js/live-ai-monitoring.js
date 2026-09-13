(() => {
  "use strict";

  const $ = (id) => document.getElementById(id);
  const apiBase = () => (window.smartHireApi?.baseUrl || window.SMART_HIRE_API_BASE || ((location.protocol === "http:" || location.protocol === "https:") ? location.origin : "http://localhost:8080")).replace(/\/$/, "");
  const token = () => localStorage.getItem("authToken") || "";
  const isLocal = () => ["localhost", "127.0.0.1"].includes(location.hostname);
  const localService = (port, path) => `http://127.0.0.1:${port}${path}`;
  const cnnEndpoint = () => `${apiBase()}/api/ai/emotion`;
  const eyeEndpoint = () => `${apiBase()}/api/ai/eye-tracking`;
  const cnnLocalEndpoint = () => localService(8095, "/analyze");
  const eyeLocalEndpoint = () => localService(8093, "/analyze");

  let timer = null;
  let busy = false;
  let canvas = null;
  let monitoringSamples = [];
  let videoReadyHandlerBound = false;

  const getVideo = () => $("liveInterviewVideo");
  const authHeaders = () => {
    const headers = { "Content-Type": "application/json" };
    const t = token();
    if (t) headers.Authorization = `Bearer ${t}`;
    return headers;
  };
  const setText = (id, value) => { const el = $(id); if (el) el.textContent = value; };
  const setStatus = (id, text, state) => {
    const el = $(id); if (!el) return;
    el.textContent = text;
    el.classList.remove("ok", "warning", "error", "neutral");
    el.classList.add(state || "neutral");
  };
  const clamp = (value) => Math.max(0, Math.min(100, Number(value) || 0));

  const captureFrame = () => {
    const video = getVideo();
    if (!video?.srcObject || !video.videoWidth || !video.videoHeight || video.readyState < 2) return null;
    canvas ||= document.createElement("canvas");
    const scale = Math.min(1, 640 / video.videoWidth);
    canvas.width = Math.max(1, Math.round(video.videoWidth * scale));
    canvas.height = Math.max(1, Math.round(video.videoHeight * scale));
    const context = canvas.getContext("2d", { alpha: false });
    context.drawImage(video, 0, 0, canvas.width, canvas.height);
    return canvas.toDataURL("image/jpeg", 0.72);
  };

  const postJson = async (path, body, timeoutMs = 9000, allowLocalFallback = false) => {
    const controller = new AbortController();
    const timeout = setTimeout(() => controller.abort(), timeoutMs);
    try {
      const url = /^https?:\/\//i.test(String(path)) ? String(path) : `${apiBase()}${path}`;
      const response = await fetch(url, { method: "POST", headers: authHeaders(), body: JSON.stringify(body), signal: controller.signal });
      if (!response.ok) throw new Error(`HTTP ${response.status}`);
      return await response.json();
    } catch (error) {
      if (allowLocalFallback && isLocal()) throw error;
      throw error;
    } finally { clearTimeout(timeout); }
  };

  const postLocal = async (url, body, timeoutMs = 7000) => {
    if (!isLocal()) throw new Error("Local AI fallback is available only in local development.");
    const controller = new AbortController();
    const timeout = setTimeout(() => controller.abort(), timeoutMs);
    try {
      const response = await fetch(url, { method: "POST", headers: { "Content-Type": "application/json" }, body: JSON.stringify(body), signal: controller.signal });
      if (!response.ok) throw new Error(`HTTP ${response.status}`);
      return await response.json();
    } finally { clearTimeout(timeout); }
  };

  const average = (field) => !monitoringSamples.length ? 0 : Math.round(monitoringSamples.reduce((sum, sample) => sum + Number(sample[field] || 0), 0) / monitoringSamples.length);
  const attentionScore = (level) => ({ high: 100, medium: 65, low: 30 }[String(level || "").toLowerCase()] || 0);
  const isRealEmotionProvider = (provider) => ["custom-cnn", "deepface"].includes(String(provider || "").toLowerCase());
  const isRealEyeProvider = (provider) => ["mediapipe", "opencv-eye-tracker-fallback"].includes(String(provider || "").toLowerCase());
  const engagementFromSignals = (emotion, eye) => {
    const eyeContact = clamp(eye?.eyeContactPercentage);
    const attention = attentionScore(eye?.attentionLevel);
    const facialActivity = clamp(eye?.facialActivityScore);
    return isRealEyeProvider(eye?.provider) ? Math.round(eyeContact * .40 + attention * .35 + facialActivity * .25) : null;
  };

  const updatePanel = ({ emotion, eye, samples }) => {
    const emotionAvailable = isRealEmotionProvider(emotion?.provider) && emotion?.simulated !== true && emotion?.available !== false;
    const eyeAvailable = isRealEyeProvider(eye?.provider) && eye?.simulated !== true && eye?.available !== false;
    setText("module6Emotion", emotionAvailable ? emotion.dominantEmotion : "Unavailable");
    setText("module6EmotionConfidence", emotionAvailable ? `${Math.round(clamp(emotion.confidence))}%` : "—");
    setText("module6EyeContact", eyeAvailable ? `${Math.round(clamp(eye.eyeContactPercentage))}%` : "—");
    setText("module6Attention", eyeAvailable ? (eye.attentionLevel || "Unavailable") : "—");
    setText("module6Gaze", eyeAvailable ? (eye.gazeDirection || "Unavailable") : "—");
    setText("module6HeadStability", eyeAvailable ? `${Math.round(clamp(eye.headStabilityScore))}%` : "—");
    setText("module6FaceCount", Number.isFinite(Number(eye.faceCount)) && Number(eye.faceCount) >= 0 ? String(eye.faceCount) : (Number.isFinite(Number(emotion.faceCount)) && Number(emotion.faceCount) >= 0 ? String(emotion.faceCount) : "—"));
    const engagement = eyeAvailable ? clamp(eye.engagementScore || engagementFromSignals(emotion, eye) || 0) : null;
    setText("module6Engagement", engagement === null ? "—" : `${Math.round(engagement)}%`);
    const confidence = eyeAvailable && emotionAvailable ? Math.round(.35 * clamp(eye.eyeContactPercentage) + .25 * clamp(eye.headStabilityScore) + .20 * clamp(eye.facialActivityScore) + .20 * attentionScore(eye.attentionLevel)) : null;
    setText("module6Confidence", confidence === null ? "—" : `${confidence}%`);
    setText("module6Provider", emotionAvailable && eyeAvailable ? `${emotion.provider} + ${eye.provider}` : emotionAvailable ? emotion.provider : eyeAvailable ? eye.provider : "Unavailable");
    setText("module6Samples", String(samples));
    const active = emotionAvailable || eyeAvailable;
    setStatus("module6MonitoringStatus", active ? "ACTIVE" : "DEGRADED", active ? "ok" : "warning");
    setText("liveMonitoringStatus", active ? "Active" : "Degraded");
    $("liveMonitoringStatus")?.classList.toggle("off", !active);
    $("liveMonitoringStatus")?.classList.toggle("on", active);
    const validation = $("liveAdvancedValidation");
    if (validation) {
      validation.style.display = "block";
      validation.textContent = active
        ? `Live Module 6 monitoring active: ${emotionAvailable ? emotion.provider : "AI"}${eyeAvailable ? " + " + eye.provider : ""}. ${monitoringSamples.length} valid samples.`
        : "Live Module 6 monitoring is degraded. Check the CNN/MediaPipe health indicators and camera permissions.";
      validation.classList.toggle("proctoring-degraded", !active);
    }
  };

  const saveSignals = (emotion, eye) => {
    const now = new Date().toISOString();
    const emotionAvailable = isRealEmotionProvider(emotion?.provider) && emotion?.simulated !== true && emotion?.available !== false;
    const eyeAvailable = isRealEyeProvider(eye?.provider) && eye?.simulated !== true && eye?.available !== false;
    if (emotionAvailable || eyeAvailable) {
      const faceCount = eyeAvailable && Number.isFinite(Number(eye.faceCount)) && Number(eye.faceCount) >= 0 ? Number(eye.faceCount) : (Number.isFinite(Number(emotion.faceCount)) && Number(emotion.faceCount) >= 0 ? Number(emotion.faceCount) : -1);
      monitoringSamples.push({
        capturedAt: now,
        eyeContactPercentage: eyeAvailable ? clamp(eye.eyeContactPercentage) : 0,
        emotionConfidence: emotionAvailable ? clamp(emotion.confidence) : 0,
        attentionLevel: eyeAvailable ? (eye.attentionLevel || "Unavailable") : "Unavailable",
        engagementLevel: eyeAvailable ? (eye.engagementLevel || "Unavailable") : "Unavailable",
        engagementScore: eyeAvailable ? clamp(eye.engagementScore || engagementFromSignals(emotion, eye) || 0) : 0,
        headStabilityScore: eyeAvailable ? clamp(eye.headStabilityScore) : 0,
        facialActivityScore: eyeAvailable ? clamp(eye.facialActivityScore) : 0,
        gazeDirection: eyeAvailable ? (eye.gazeDirection || "Unavailable") : "Unavailable",
        eyesClosed: Boolean(eye?.eyesClosed), faceCount,
        emotion: emotionAvailable ? emotion.dominantEmotion : "Unavailable",
        emotionProvider: emotionAvailable ? emotion.provider : "unavailable",
        eyeProvider: eyeAvailable ? eye.provider : "unavailable",
        simulated: false, valid: true
      });
      monitoringSamples = monitoringSamples.slice(-180);
    }
    const payload = { capturedAt: now, providerMode: { emotion: emotion?.provider || "unavailable", eye: eye?.provider || "unavailable" }, monitoringStatus: emotionAvailable || eyeAvailable ? "ACTIVE" : "DEGRADED", samples: monitoringSamples.length, emotion, eyeContact: eye, summary: { averageEyeContactPercentage: average("eyeContactPercentage"), averageEmotionConfidence: average("emotionConfidence"), averageHeadStabilityScore: average("headStabilityScore"), averageEngagementScore: average("engagementScore"), sampleCount: monitoringSamples.length, simulatedSamples: 0, monitoringComplete: Boolean(emotionAvailable || eyeAvailable) } };
    localStorage.setItem("smarthire.liveSignals", JSON.stringify(payload));
    updatePanel({ emotion, eye, samples: monitoringSamples.length });
  };

  const normalizeEmotion = (r) => ({ dominantEmotion: r?.dominantEmotion || r?.dominant_emotion || "Unavailable", confidence: Number(r?.confidence || 0), scores: r?.scores || {}, provider: String(r?.provider || "unavailable").toLowerCase(), available: r?.available !== false && (r?.model_ready !== false), simulated: Boolean(r?.simulated), faceCount: Number(r?.faceCount ?? r?.face_count ?? -1) });
  const normalizeEye = (r) => { const p = String(r?.provider || "unavailable").toLowerCase(); return { eyeContactPercentage: Number(r?.eyeContactPercentage ?? r?.eye_contact_percentage ?? 0), attentionLevel: r?.attentionLevel || r?.attention_level || "Unavailable", engagementLevel: r?.engagementLevel || r?.engagement_level || "Unavailable", engagementScore: Number(r?.engagementScore ?? r?.engagement_score ?? 0), faceCount: Number(r?.faceCount ?? r?.face_count ?? -1), gazeDirection: r?.gazeDirection || r?.gaze_direction || "Unavailable", eyesClosed: Boolean(r?.eyesClosed ?? r?.eyes_closed), headStabilityScore: Number(r?.headStabilityScore ?? r?.head_stability_score ?? 0), facialActivityScore: Number(r?.facialActivityScore ?? r?.facial_activity_score ?? 0), provider: p, available: r?.available !== false && isRealEyeProvider(p), simulated: Boolean(r?.simulated) }; };

  const cycle = async () => {
    if (busy) return;
    const image = captureFrame();
    if (!image) return;
    busy = true;
    try {
      const results = await Promise.allSettled([postJson(cnnEndpoint(), { image }), postJson(eyeEndpoint(), { image })]);
      let emotion = results[0].status === "fulfilled" ? normalizeEmotion(results[0].value) : null;
      let eye = results[1].status === "fulfilled" ? normalizeEye(results[1].value) : null;

      // Local-only resilience: the browser can directly validate the already-running
      // local AI services when the Spring Boot facade temporarily reports unavailable.
      if (isLocal() && (!emotion || !emotion.available)) {
        try { emotion = normalizeEmotion(await postLocal(cnnLocalEndpoint(), { image })); } catch (_) {}
      }
      if (isLocal() && (!eye || !eye.available)) {
        try { eye = normalizeEye(await postLocal(eyeLocalEndpoint(), { image })); } catch (_) {}
      }
      saveSignals(
        emotion || { dominantEmotion: "Unavailable", confidence: 0, provider: "unavailable", available: false, simulated: false, faceCount: -1 },
        eye || { eyeContactPercentage: 0, attentionLevel: "Unavailable", engagementLevel: "Unavailable", provider: "unavailable", available: false, simulated: false, faceCount: -1, gazeDirection: "Unavailable", headStabilityScore: 0, facialActivityScore: 0 }
      );
    } catch (error) {
      console.error("[SmartHire][Module6] monitoring cycle error:", error);
    } finally { busy = false; }
  };

  const start = () => {
    if (timer) return;
    monitoringSamples = [];
    localStorage.removeItem("smarthire.liveSignals");
    cycle();
    timer = setInterval(cycle, 2500);
  };
  const stop = () => { if (timer) clearInterval(timer); timer = null; };

  window.__smartHireModule6Monitoring = { start, stop, get samples() { return monitoringSamples.length; } };
  window.addEventListener("beforeunload", stop);
  window.addEventListener("DOMContentLoaded", () => {
    const video = getVideo();
    if (video && !videoReadyHandlerBound) {
      videoReadyHandlerBound = true;
      ["loadedmetadata", "canplay", "playing"].forEach(evt => video.addEventListener(evt, start, { once: false }));
    }
    $("liveJoinBtn")?.addEventListener("click", start);
    $("liveLeaveBtn")?.addEventListener("click", stop);
    start();
  }, { once: true });
})();
