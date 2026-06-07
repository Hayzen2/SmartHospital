package com.example.SmartHospital.service.consultation;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.example.SmartHospital.config.exceptions.BadRequestException;
import com.example.SmartHospital.dtos.ConsultationDtos.ConsultationExtractResponse;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class GeminiConsultationService {

    // ObjectMapper is thread-safe after configuration, so we can reuse a single instance for better performance
    private final ObjectMapper objectMapper = new ObjectMapper();

    private static final String GEMINI_GENERATE_PATH =
        "/v1beta/models/gemini-2.5-flash:generateContent";

    private static final String GEMINI_HOST = "generativelanguage.googleapis.com";

    private static final String SYSTEM_PROMPT = """
        You are a clinical documentation assistant for a hospital triage intake form.

        Your task: read the patient's free-text description of how they feel and extract structured fields ONLY. Output must follow the rules below.

        CRITICAL RULES:
        1) NEVER provide a disease name or diagnosis (no ICD labels, no "you have X"). Do not label conditions.
        2) You MAY use reasonable clinical inference to fill missing fields when the text strongly implies them (e.g. "bad cough for a week" → location may be inferred as throat/respiratory; duration "about one week").
        3) Arrays must be JSON arrays of strings; use [] when unknown or not mentioned.
        4) Use null for optional string fields when unknown or not inferable; never guess wildly.
        5) "raw_text" MUST contain the user's message verbatim (exactly as provided), with only whitespace normalization at most — preserve meaning and wording.

        Output JSON object with EXACTLY these keys:
        - main_symptoms: string[] — primary complaints/symptoms in short phrases.
        - duration: string | null — how long symptoms have lasted if stated or clearly inferable.
        - additional_signs: string[] — other associated signs/symptoms.
        - location: string | null — body region / system if stated or safely inferable from symptoms.
        - symptom_character: string | null — quality of symptom (e.g. sharp, dull, burning) if present.
        - aggravating_factors: string[] — what worsens symptoms.
        - relieving_factors: string[] — what improves symptoms.
        - progression: string | null — getting better/worse/stable if stated or inferable.
        - red_flags: string[] — alarming features explicitly mentioned (breathlessness, chest pain, neurological deficits, etc.) — still NO diagnosis.
        - raw_text: string — verbatim user input.

        Respond with JSON only — no markdown fences, no commentary.""";

    private static final String RESPONSE_SCHEMA_JSON = """
        {
          "type": "OBJECT",
          "properties": {
            "main_symptoms": { "type": "ARRAY", "items": { "type": "STRING" } },
            "duration": { "type": "STRING", "nullable": true },
            "additional_signs": { "type": "ARRAY", "items": { "type": "STRING" } },
            "location": { "type": "STRING", "nullable": true },
            "symptom_character": { "type": "STRING", "nullable": true },
            "aggravating_factors": { "type": "ARRAY", "items": { "type": "STRING" } },
            "relieving_factors": { "type": "ARRAY", "items": { "type": "STRING" } },
            "progression": { "type": "STRING", "nullable": true },
            "red_flags": { "type": "ARRAY", "items": { "type": "STRING" } },
            "raw_text": { "type": "STRING" }
          },
          "required": [
            "main_symptoms",
            "duration",
            "additional_signs",
            "location",
            "symptom_character",
            "aggravating_factors",
            "relieving_factors",
            "progression",
            "red_flags",
            "raw_text"
          ]
        }
        """;

    @Value("${gemini.api-key:}")
    private String apiKey;

    // Reuse the same HttpClient for better performance
    private final HttpClient httpClient = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(30))
        .build();

    // Parses the hardcoded JSON schema into a JsonNode
    // This is done once per analyze() call to ensure any changes to the schema are picked up 
    // without needing to restart the service
    private JsonNode responseSchemaNode() throws JsonProcessingException {
        return objectMapper.readTree(RESPONSE_SCHEMA_JSON);
    }

    // Main method to analyze raw symptom description and extract structured data
    public ConsultationExtractResponse analyze(String rawText) {
        String trimmed = rawText == null ? "" : rawText.trim();
        if (trimmed.isEmpty()) {
            throw new BadRequestException("Symptom description is required.");
        }
        if (apiKey == null || apiKey.isBlank()) {
            throw new BadRequestException("Gemini API key is not configured on the server.");
        }

        try {
            
            // Prepare the request payload according to Gemini's expected format
            JsonNode schema = responseSchemaNode();
            // Construct the request payload according to Gemini's expected format
            ObjectNode root = objectMapper.createObjectNode();

            // System instruction with the detailed prompt and schema
            ObjectNode systemInstruction = objectMapper.createObjectNode();
            // The "parts" array allows for more complex instructions with multiple components
            ArrayNode sysParts = objectMapper.createArrayNode();
            // We put the entire prompt in a single part
            sysParts.add(objectMapper.createObjectNode().put("text", SYSTEM_PROMPT));
            systemInstruction.set("parts", sysParts);
            root.set("systemInstruction", systemInstruction);

            // The "contents" array represents the conversation history; we start with the user's input as the first turn
            ArrayNode contents = objectMapper.createArrayNode();
            // Each turn in the conversation is an object with a "role" (user/assistant/system) and "parts" (the actual messages)
            ObjectNode userTurn = objectMapper.createObjectNode();
            // The "role" field indicates who is speaking; in this case, it's the user providing the symptom description
            userTurn.put("role", "user");
            ArrayNode userParts = objectMapper.createArrayNode();
            // Each part can contain text, images, or other media; here we just have one part with the user's raw text input
            userParts.add(objectMapper.createObjectNode().put("text", trimmed));
            // We could add more parts if we wanted to provide additional context or information from the user
            userTurn.set("parts", userParts);
            // Add the user's turn to the contents array, which represents the conversation history sent to Gemini
            contents.add(userTurn);
            root.set("contents", contents);

            // The "generationConfig" field allows us to specify how we want Gemini to generate its response
            // including the schema it should follow
            ObjectNode generationConfig = objectMapper.createObjectNode();
            generationConfig.put("responseMimeType", "application/json");
            generationConfig.set("responseSchema", schema);
            root.set("generationConfig", generationConfig);

            String payload = objectMapper.writeValueAsString(root);
            String query = URLEncoder.encode(apiKey, StandardCharsets.UTF_8);
            URI uri = URI.create("https://" + GEMINI_HOST + GEMINI_GENERATE_PATH + "?key=" + query);

            HttpRequest request = HttpRequest.newBuilder()
                .uri(uri)
                .timeout(Duration.ofMinutes(2))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(payload, StandardCharsets.UTF_8))
                .build();

            HttpResponse<String> response =
                httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

            if (response.statusCode() != 200) {
                JsonNode errBody = objectMapper.readTree(response.body());
                String msg = errBody.path("error").path("message").asText("Gemini request failed");
                throw new BadRequestException(msg);
            }

            JsonNode body = objectMapper.readTree(response.body());
            // The generated content is nested in a complex structure; we navigate through it to find the "text" part of the first candidate response
            JsonNode textNode = body.path("candidates").path(0).path("content").path("parts").path(0).path("text");
            String text = textNode.isMissingNode() || textNode.isNull() ? null : textNode.asText();
            if (text == null || text.isBlank()) {
                throw new BadRequestException("No structured response from Gemini.");
            }

            ConsultationExtractResponse parsed = objectMapper.readValue(text, ConsultationExtractResponse.class);
            return normalize(parsed, trimmed);
        } catch (BadRequestException e) {
            throw e;
        } catch (Exception e) {
            log.error("Gemini consultation analyze failed", e);
            throw new BadRequestException("Could not analyze consultation: " + e.getMessage(), e);
        }
    }

    // Normalizes null lists to empty and ensures raw_text is present for better downstream handling 
    private ConsultationExtractResponse normalize(ConsultationExtractResponse r, String fallbackRaw) {
        if (r.getMain_symptoms() == null) {
            r.setMain_symptoms(List.of());
        }
        if (r.getAdditional_signs() == null) {
            r.setAdditional_signs(List.of());
        }
        if (r.getAggravating_factors() == null) {
            r.setAggravating_factors(List.of());
        }
        if (r.getRelieving_factors() == null) {
            r.setRelieving_factors(List.of());
        }
        if (r.getRed_flags() == null) {
            r.setRed_flags(List.of());
        }
        if (r.getRaw_text() == null || r.getRaw_text().isBlank()) {
            r.setRaw_text(fallbackRaw);
        }
        return r;
    }
}
