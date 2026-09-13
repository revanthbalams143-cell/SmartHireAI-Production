package com.smarthire.backend.ai.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestClient;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/ai")
public class AiHealthController {
    private final RestClient restClient;
    @Value("${ai.emotion-cnn.url:http://localhost:8095}") String cnn;
    @Value("${ai.mediapipe.url:http://localhost:8093}") String mediapipe;
    @Value("${ai.deepface.url:http://localhost:8092}") String deepface;
    @Value("${ai.whisper.url:http://localhost:8091}") String whisper;
    @Value("${ai.object-detection.url:http://localhost:8094}") String objectDetection;

    public AiHealthController(RestClient.Builder builder){this.restClient=builder.build();}

    @GetMapping("/health")
    public ResponseEntity<Map<String,Object>> health(){
        Map<String,Object> out=new LinkedHashMap<>();
        out.put("cnn",check(cnn+"/health"));
        out.put("mediapipe",check(mediapipe+"/health"));
        out.put("deepface",check(deepface+"/health"));
        out.put("whisper",check(whisper+"/health"));
        out.put("objectDetection",check(objectDetection+"/health"));
        out.put("status",out.values().stream().allMatch(v -> Boolean.TRUE.equals(v)) ? "UP" : "DEGRADED");
        return ResponseEntity.ok(out);
    }
    private boolean check(String url){try{var r=restClient.get().uri(url).retrieve().body(Map.class);return r!=null && Boolean.TRUE.equals(r.get("available")) || (r!=null && "ok".equalsIgnoreCase(String.valueOf(r.get("status"))));}catch(Exception e){return false;}}
}
