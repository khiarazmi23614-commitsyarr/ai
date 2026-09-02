package com.khiar.minecraftaichat;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicLong;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.message.v1.ClientSendMessageEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;

public final class MinecraftAiChatClient implements ClientModInitializer {
    private static final HttpClient HTTP = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();
    private static final AtomicLong LAST_REQUEST_AT = new AtomicLong(0L);

    @Override
    public void onInitializeClient() {
        ClientSendMessageEvents.CHAT.register(this::onChatSent);
    }

    private void onChatSent(String message) {
        AiChatConfig config = AiChatConfig.load();
        String prompt = extractPrompt(message, config.triggerPrefix());

        if (prompt == null || prompt.isBlank()) {
            return;
        }

        long now = System.currentTimeMillis();
        long previous = LAST_REQUEST_AT.get();
        if (now - previous < config.cooldownMs()) {
            showClientMessage("Please wait before asking AI again.");
            return;
        }
        if (!LAST_REQUEST_AT.compareAndSet(previous, now)) {
            return;
        }

        if (config.apiKey().isBlank()) {
            showClientMessage("AI is not configured. Set " + config.apiKeyEnvironmentVariable()
                    + " and restart Minecraft.");
            return;
        }

        showClientMessage("AI is thinking...");
        CompletableFuture.supplyAsync(() -> requestAnswer(config, prompt))
                .whenComplete((answer, error) -> {
                    if (error != null) {
                        showClientMessage("AI request failed: " + safeMessage(error));
                    } else {
                        showClientMessage("AI: " + answer);
                    }
                });
    }

    private static String extractPrompt(String message, String prefix) {
        if (prefix.isEmpty()) {
            return message;
        }
        if (!message.regionMatches(true, 0, prefix, 0, prefix.length())) {
            return null;
        }
        return message.substring(prefix.length()).trim();
    }

    private static String requestAnswer(AiChatConfig config, String prompt) {
        JsonObject payload = new JsonObject();
        payload.addProperty("model", config.model());
        JsonArray messages = new JsonArray();
        JsonObject userMessage = new JsonObject();
        userMessage.addProperty("role", "user");
        userMessage.addProperty("content", prompt);
        messages.add(userMessage);
        payload.add("messages", messages);

        HttpRequest request = HttpRequest.newBuilder(URI.create(config.endpoint()))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + config.apiKey())
                .timeout(Duration.ofSeconds(config.requestTimeoutSeconds()))
                .POST(HttpRequest.BodyPublishers.ofString(payload.toString()))
                .build();

        try {
            HttpResponse<String> response = HTTP.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new IOException("HTTP " + response.statusCode() + ": "
                        + truncate(response.body(), 180));
            }

            JsonObject root = JsonParser.parseString(response.body()).getAsJsonObject();
            String answer = root.getAsJsonArray("choices").get(0).getAsJsonObject()
                    .getAsJsonObject("message").get("content").getAsString().trim();
            if (answer.isEmpty()) {
                throw new IOException("The AI returned an empty response.");
            }
            return truncate(answer, config.maxReplyCharacters());
        } catch (IOException | InterruptedException e) {
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            throw new IllegalStateException(safeMessage(e), e);
        } catch (RuntimeException e) {
            throw new IllegalStateException("Invalid AI response or configuration: " + safeMessage(e), e);
        }
    }

    private static void showClientMessage(String message) {
        MinecraftClient client = MinecraftClient.getInstance();
        client.execute(() -> {
            if (client.player != null) {
                client.player.sendMessage(Text.literal("[AI] " + message), false);
            }
        });
    }

    private static String truncate(String value, int maxCharacters) {
        return value.length() <= maxCharacters ? value : value.substring(0, maxCharacters) + "…";
    }

    private static String safeMessage(Throwable error) {
        String message = error.getMessage();
        return message == null || message.isBlank() ? error.getClass().getSimpleName() : truncate(message, 180);
    }
}
