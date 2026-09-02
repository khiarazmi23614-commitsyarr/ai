package com.khiar.minecraftaichat;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import net.fabricmc.loader.api.FabricLoader;

public record AiChatConfig(
        String endpoint,
        String model,
        String apiKeyEnvironmentVariable,
        String triggerPrefix,
        long cooldownMs,
        int requestTimeoutSeconds,
        int maxReplyCharacters
) {
    private static final AiChatConfig DEFAULTS = new AiChatConfig(
            "https://api.openai.com/v1/chat/completions",
            "gpt-4.1-mini",
            "OPENAI_API_KEY",
            "@ai ",
            3_000L,
            30,
            1_000
    );

    public static AiChatConfig load() {
        Path path = FabricLoader.getInstance().getConfigDir().resolve("minecraft-ai-chat.json");
        try {
            if (Files.notExists(path)) {
                Files.writeString(path, defaultJson(), StandardCharsets.UTF_8);
            }
            JsonObject json = JsonParser.parseString(Files.readString(path, StandardCharsets.UTF_8))
                    .getAsJsonObject();
            String keyVariable = string(json, "apiKeyEnvironmentVariable", DEFAULTS.apiKeyEnvironmentVariable());
            return new AiChatConfig(
                    string(json, "endpoint", DEFAULTS.endpoint()),
                    string(json, "model", DEFAULTS.model()),
                    keyVariable,
                    string(json, "triggerPrefix", DEFAULTS.triggerPrefix()),
                    positiveLong(json, "cooldownMs", DEFAULTS.cooldownMs()),
                    positiveInt(json, "requestTimeoutSeconds", DEFAULTS.requestTimeoutSeconds()),
                    positiveInt(json, "maxReplyCharacters", DEFAULTS.maxReplyCharacters())
            );
        } catch (IOException | IllegalStateException e) {
            throw new IllegalStateException("Could not load minecraft-ai-chat.json: " + e.getMessage(), e);
        }
    }

    public String apiKey() {
        String value = System.getenv(apiKeyEnvironmentVariable);
        return value == null ? "" : value.trim();
    }

    private static String defaultJson() {
        return """
                {
                  "endpoint": "https://api.openai.com/v1/chat/completions",
                  "model": "gpt-4.1-mini",
                  "apiKeyEnvironmentVariable": "OPENAI_API_KEY",
                  "triggerPrefix": "@ai ",
                  "cooldownMs": 3000,
                  "requestTimeoutSeconds": 30,
                  "maxReplyCharacters": 1000
                }
                """;
    }

    private static String string(JsonObject json, String name, String fallback) {
        return json.has(name) ? json.get(name).getAsString() : fallback;
    }

    private static int positiveInt(JsonObject json, String name, int fallback) {
        int value = json.has(name) ? json.get(name).getAsInt() : fallback;
        return value > 0 ? value : fallback;
    }

    private static long positiveLong(JsonObject json, String name, long fallback) {
        long value = json.has(name) ? json.get(name).getAsLong() : fallback;
        return value > 0 ? value : fallback;
    }
}
