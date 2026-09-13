from fastapi import FastAPI, UploadFile, File
from fastapi.middleware.cors import CORSMiddleware
from faster_whisper import WhisperModel
import tempfile
import os
import sys

app = FastAPI(title="SmartHire Whisper Service")
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

MODEL_SIZE = os.getenv("WHISPER_MODEL", "small")
DEVICE = os.getenv("WHISPER_DEVICE", "cpu")
COMPUTE_TYPE = os.getenv("WHISPER_COMPUTE_TYPE", "int8")

MODEL = None
MODEL_LOAD_ERROR = None


def get_model():
    global MODEL, MODEL_LOAD_ERROR
    if MODEL is None:
        try:
            print(f"[whisper] Initializing faster-whisper model '{MODEL_SIZE}' (device={DEVICE}, compute={COMPUTE_TYPE})...", file=sys.stderr)
            MODEL = WhisperModel(
                MODEL_SIZE,
                device=DEVICE,
                compute_type=COMPUTE_TYPE,
            )
            MODEL_LOAD_ERROR = None
            print(f"[whisper] Model '{MODEL_SIZE}' loaded successfully.", file=sys.stderr)
        except Exception as exc:
            MODEL_LOAD_ERROR = str(exc)
            print(f"[whisper] Model load error: {exc}", file=sys.stderr)
            raise exc
    return MODEL


@app.on_event("startup")
def on_startup():
    try:
        get_model()
    except Exception as exc:
        print(f"[whisper] Startup model initialization note: {exc}", file=sys.stderr)


@app.get("/health")
def health():
    return {
        "status": "ok" if MODEL_LOAD_ERROR is None else "degraded",
        "model": MODEL_SIZE,
        "word_confidence": True,
        "model_loaded": MODEL is not None,
        "error": MODEL_LOAD_ERROR,
    }


@app.post("/transcribe")
async def transcribe(audio: UploadFile = File(...)):
    model_instance = get_model()
    suffix = os.path.splitext(audio.filename or ".webm")[1] or ".webm"
    with tempfile.NamedTemporaryFile(delete=False, suffix=suffix) as f:
        f.write(await audio.read())
        path = f.name

    try:
        segments, _ = model_instance.transcribe(
            path,
            vad_filter=True,
            word_timestamps=True,
        )
        segment_list = list(segments)
        text = " ".join(s.text.strip() for s in segment_list).strip()

        segment_scores = []
        word_scores = []
        total_duration = 0.0
        for seg in segment_list:
            start = float(getattr(seg, "start", 0.0) or 0.0)
            end = float(getattr(seg, "end", start) or start)
            total_duration = max(total_duration, end)
            lp = float(getattr(seg, "avg_logprob", -2.0))
            segment_score = max(0.0, min(1.0, (lp + 2.5) / 2.5))
            segment_scores.append(segment_score)

            for word in getattr(seg, "words", []) or []:
                probability = getattr(word, "probability", None)
                if probability is not None:
                    word_scores.append(max(0.0, min(1.0, float(probability))))

        confidence_samples = word_scores or segment_scores
        confidence = (
            round((sum(confidence_samples) / len(confidence_samples)) * 100, 2)
            if confidence_samples
            else 0
        )
        word_confidence = (
            round((sum(word_scores) / len(word_scores)) * 100, 2)
            if word_scores
            else confidence
        )

        return {
            "text": text,
            "transcript": text,
            "provider": "faster-whisper",
            "confidence": confidence,
            "wordConfidence": word_confidence,
            "segments": len(segment_list),
            "durationSeconds": round(total_duration, 2),
            "pronunciationMetric": "word-confidence-and-transcription-clarity",
        }
    finally:
        try:
            os.remove(path)
        except OSError:
            pass
