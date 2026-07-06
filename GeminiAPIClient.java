package com.ppba.plugin.api;

import com.ppba.plugin.PPBAPlugin;
import com.ppba.plugin.config.Config;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

/**
 * HTTP client for Google Gemini API integration
 * Sends player behavioral data for psychological analysis
 */
public class ClaudeAPIClient {
    
    private final PPBAPlugin plugin;
    private final Config config;
    // Gemini API standard generation endpoint
    private static final String GEMINI_API_BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models/%s:generateContent?key=%s";
    
    // System prompt for psychological analysis (from v2.1 spec)
    private static final String SYSTEM_PROMPT = """
[SYSTEM PROMPT — PLAYER PSYCHOLOGICAL BEHAVIOR ANALYZER v2.1]
Aap ek Expert Behavioral Psychologist, Forensic Linguist, aur Minecraft Plugin Logic Designer hain (Target Version: Minecraft Java 1.21.1). Aapka kaam player ke chat logs, interaction history, aur gameplay metrics ko deeply analyze karke unki asli real-life personality, emotional wiring, aur social behavior pattern ko map karna hai.

Golden Rule: Game mechanics (PvP, mining, killing, raiding) khud se kisi ko bura insaan nahi banate. Sirf intent + context + tone + consistency matter karte hain.

[PSYCHOLOGICAL FRAMEWORK — CORE DIMENSIONS]
Har player ko in 4 psychological axes par score karo (0-10):
- Emotional Regulation (ER) — Stress/loss ke waqt control kitna stable rehta hai
- Empathy Index (EI) — Doosre players ke prati consideration
- Ego Fragility (EF) — Criticism/defeat par kitna defensive/aggressive reaction
- Social Orientation (SO) — Introvert-solo vs Extrovert-community driven

[PERSONALITY ARCHETYPES]
TYPE-01: Dignified Strategist (ER: 8-10, EF: 0-3) → Minimal words, purpose-driven ("GG"), treats defeat as data
TYPE-02: Toxic Instigator (EF: 7-10, EI: 0-3) → CAPS spikes, mocking, seeks validation via dominance
TYPE-03: Altruistic Guardian (EI: 8-10, SO: High) → Welcomes newbies, conflict resolution
TYPE-04: Quiet Observer (SO: 0-2, ER: High/Isolated) → 1-2 word replies, privacy paramount
TYPE-05: Anxious Validator (EF: Med-High, SO: High-Insecure) → Constant approval-seeking

[OUTPUT FORMAT]
Provide EXACTLY this structure:

[PLAYER PSYCHOLOGICAL PROFILE: <USERNAME>]
Primary Type: [Code & Name]
Secondary Traits: [if any]
Confidence Score: [0-100%]
Axis Scores: ER: [X/10] | EI: [X/10] | EF: [X/10] | SO: [X/10]

Deep Behavioral Analysis:
- Communication Fingerprint: [analysis]
- Trigger Points: [situations that destabilize them]
- Mask vs Reality Gap: [public vs private behavior difference]
- Long-term Behavioral Prediction: [future reaction tracking]

Plugin Backend Recommendation:
- Invisible Reputation Delta: [value]
- Auto-Flag Tags: [tags]
- Suggested Permission Tier: [tier level]
""";
    
    public ClaudeAPIClient(PPBAPlugin plugin, Config config) {
        this.plugin = plugin;
        this.config = config;
    }
    
    /**
     * Send player data to Gemini API for behavioral analysis
     * @param playerDataJSON JSON formatted player data
     * @return Psychological analysis report as String
     */
    public String analyzeBehavior(String playerDataJSON) throws Exception {
        String apiKey = config.getApiKey();
        
        if (apiKey == null || apiKey.isEmpty() || apiKey.equals("YOUR_API_KEY_HERE")) {
            throw new IllegalStateException("Gemini API key not configured. Set it in config.yml");
        }
        
        // Use configured model (e.g., gemini-1.5-flash or gemini-1.5-pro)
        String model = config.getModel();
        if (model == null || model.isEmpty()) {
            model = "gemini-1.5-flash"; // Fallback default
        }
        
        // Build request payload according to Gemini structure
        String requestBody = buildRequestPayload(playerDataJSON);
        
        // Formulate Gemini endpoint URL with API key as parameter
        String finalUrl = String.format(GEMINI_API_BASE_URL, model, apiKey);
        
        // Make HTTP request
        HttpURLConnection connection = (HttpURLConnection) new URL(finalUrl).openConnection();
        connection.setRequestMethod("POST");
        connection.setRequestProperty("Content-Type", "application/json");
        connection.setDoOutput(true);
        connection.setConnectTimeout(config.getTimeoutSeconds() * 1000);
        connection.setReadTimeout(config.getTimeoutSeconds() * 1000);
        
        // Send request
        try (OutputStream os = connection.getOutputStream()) {
            byte[] input = requestBody.getBytes(StandardCharsets.UTF_8);
            os.write(input, 0, input.length);
        }
        
        // Handle response
        int responseCode = connection.getResponseCode();
        
        if (responseCode != 200) {
            String errorMessage = readStream(connection.getErrorStream());
            throw new IOException("Gemini API request failed with code " + responseCode + ": " + errorMessage);
        }
        
        String responseBody = readStream(connection.getInputStream());
        
        // Parse Gemini response and extract text
        return parseAPIResponse(responseBody);
    }
    
    /**
     * Build the request payload for Gemini API with systemInstruction
     */
    private String buildRequestPayload(String playerDataJSON) {
        return String.format("""
{
  "systemInstruction": {
    "parts": [
      {
        "text": %s
      }
    ]
  },
  "contents": [
    {
      "parts": [
        {
          "text": "Analyze this Minecraft player's behavioral data and provide a psychological profile:\\n\\n%s"
        }
      ]
    }
  ],
  "generationConfig": {
    "maxOutputTokens": 2000
  }
}
""", escapeJsonString(SYSTEM_PROMPT), escapeJsonString(playerDataJSON));
    }
    
    /**
     * Read entire stream to string
     */
    private String readStream(InputStream stream) throws IOException {
        if (stream == null) return "";
        
        StringBuilder result = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                result.append(line).append("\n");
            }
        }
        return result.toString();
    }
    
    /**
     * Parse Gemini API response and extract text content from candidates
     */
    private String parseAPIResponse(String responseBody) throws IOException {
        // String token-based extraction for Gemini's json structure: candidates[0].content.parts[0].text
        int textIndex = responseBody.indexOf("\"text\"");
        if (textIndex == -1) {
            throw new IOException("Invalid Gemini API response format: no text content found");
        }
        
        int startIndex = responseBody.indexOf("\"", textIndex + 6) + 1;
        int endIndex = responseBody.indexOf("\"", startIndex);
        
        if (startIndex == 0 || endIndex == -1) {
            throw new IOException("Invalid Gemini API response format: could not extract text");
        }
        
        String text = responseBody.substring(startIndex, endIndex);
        // Unescape JSON string
        return text
                .replace("\\n", "\n")
                .replace("\\\"", "\"")
                .replace("\\\\", "\\");
    }
    
    /**
     * Escape string for JSON
     */
    private String escapeJsonString(String text) {
        return "\"" + text
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t") + "\"";
    }
}
