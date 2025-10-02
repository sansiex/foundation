package com.aichat.foundation.client;

import com.aichat.foundation.exception.OllamaException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

import jakarta.annotation.PostConstruct;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@Component
public class OllamaClient {
    
    @Value("${ollama.base-url}")
    private String baseUrl;
    
    @Value("${ollama.model}")
    private String defaultModel;
    
    @Value("${ollama.timeout}")
    private int timeout;
    
    @Value("${ollama.max-retries}")
    private int maxRetries;
    
    private HttpClient httpClient;
    private ObjectMapper objectMapper;
    
    @PostConstruct
    public void init() {
        this.httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(30))
            .build();
        this.objectMapper = new ObjectMapper();
    }
    
    /**
     * Send a text-only message to OLLAMA and get streaming response
     */
    public Flux<String> sendTextMessage(String message) {
        return sendMessage(message, null);
    }
    
    /**
     * Send a multimodal message (text + image) to OLLAMA and get streaming response
     */
    public Flux<String> sendMultimodalMessage(String message, byte[] imageData) {
        String base64Image = Base64.getEncoder().encodeToString(imageData);
        return sendMessage(message, base64Image);
    }
    
    /**
     * Core method to send messages to OLLAMA with streaming response
     */
    private Flux<String> sendMessage(String message, String base64Image) {
        return Flux.create(sink -> {
            try {
                Map<String, Object> requestBody = createRequestBody(message, base64Image);
                String jsonBody = objectMapper.writeValueAsString(requestBody);
                
                HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + "/api/generate"))
                    .header("Content-Type", "application/json")
                    .header("Accept", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                    .timeout(Duration.ofSeconds(timeout))
                    .build();
                
                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                
                if (response.statusCode() != 200) {
                    sink.error(new OllamaException("OLLAMA returned error code: " + response.statusCode()));
                    return;
                }
                
                // Parse streaming response
                parseStreamingResponse(response.body(), sink);
                sink.complete();
                
            } catch (Exception e) {
                sink.error(new OllamaException("Failed to send request to OLLAMA: " + e.getMessage(), e));
            }
        });
    }
    
    /**
     * Create request body for OLLAMA API
     */
    private Map<String, Object> createRequestBody(String message, String base64Image) {
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", defaultModel);
        requestBody.put("prompt", message);
        requestBody.put("stream", true);
        
        // Add image if provided (multimodal request)
        if (base64Image != null && !base64Image.isEmpty()) {
            requestBody.put("images", List.of(base64Image));
        }
        
        // Add generation parameters
        Map<String, Object> options = new HashMap<>();
        options.put("temperature", 0.7);
        options.put("top_p", 0.9);
        options.put("max_tokens", 2048);
        requestBody.put("options", options);
        
        return requestBody;
    }
    
    /**
     * Parse streaming response from OLLAMA
     */
    private void parseStreamingResponse(String responseBody, reactor.core.publisher.FluxSink<String> sink) {
        try (BufferedReader reader = new BufferedReader(new StringReader(responseBody))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.trim().isEmpty()) {
                    continue;
                }
                
                try {
                    JsonNode jsonNode = objectMapper.readTree(line);
                    
                    // Check for errors
                    if (jsonNode.has("error")) {
                        sink.error(new OllamaException("OLLAMA error: " + jsonNode.get("error").asText()));
                        return;
                    }
                    
                    // Extract response content
                    if (jsonNode.has("response")) {
                        String content = jsonNode.get("response").asText();
                        if (!content.isEmpty()) {
                            sink.next(content);
                        }
                    }
                    
                    // Check if done
                    if (jsonNode.has("done") && jsonNode.get("done").asBoolean()) {
                        break;
                    }
                    
                } catch (Exception e) {
                    // Log malformed JSON line but continue processing
                    System.err.println("Failed to parse JSON line: " + line + " - " + e.getMessage());
                }
            }
        } catch (IOException e) {
            sink.error(new OllamaException("Failed to read response stream: " + e.getMessage(), e));
        }
    }
    
    /**
     * Test connection to OLLAMA service
     */
    public CompletableFuture<Boolean> testConnection() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + "/api/tags"))
                    .GET()
                    .timeout(Duration.ofSeconds(5))
                    .build();
                
                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                return response.statusCode() == 200;
                
            } catch (Exception e) {
                return false;
            }
        });
    }
    
    /**
     * Get available models from OLLAMA
     */
    public CompletableFuture<List<String>> getAvailableModels() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + "/api/tags"))
                    .GET()
                    .timeout(Duration.ofSeconds(5))
                    .build();
                
                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                
                if (response.statusCode() == 200) {
                    JsonNode jsonNode = objectMapper.readTree(response.body());
                    if (jsonNode.has("models")) {
                        return jsonNode.get("models").findValuesAsText("name");
                    }
                }
                return List.of();
                
            } catch (Exception e) {
                return List.of();
            }
        });
    }
    
    /**
     * Health check for OLLAMA service
     */
    public boolean isHealthy() {
        try {
            return testConnection().get(5, java.util.concurrent.TimeUnit.SECONDS);
        } catch (Exception e) {
            return false;
        }
    }
}