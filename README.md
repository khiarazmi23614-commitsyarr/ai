# Minecraft AI Chat

A client-side Fabric mod for Minecraft Java **1.21.1**. Type a normal chat message beginning with `@ai ` and the mod will request a reply from an OpenAI-compatible Chat Completions endpoint, then display that reply only in your local Minecraft chat.

Example:

```
@ai How do I find diamonds in Minecraft?
```

## Features

- Fabric client mod for Minecraft 1.21.1 and Java 21
- OpenAI-compatible endpoint and model configured outside source code
- API key read only from an environment variable; no secrets in the repository or config file
- Configurable chat prefix, request cooldown, timeout, and reply length
- Non-blocking HTTP request with readable in-game error feedback

## Setup

1. Install Java 21, Fabric Loader, and Fabric API for Minecraft 1.21.1.
2. Build the mod:

   ```bash
   ./gradlew build
   ```

   On Windows, use `gradlew.bat build`. The built JAR is placed in `build/libs/`.

3. Copy the generated JAR (not the `-sources` JAR) into your Minecraft instance's `mods` folder, alongside the matching Fabric API JAR.
4. Define your API key in the environment that launches the Minecraft launcher. For PowerShell in the current session:

   ```powershell
   $env:OPENAI_API_KEY = "your_api_key"
   ```

   Start the launcher from that same PowerShell session. For persistent setup, add an environment variable through your operating system settings, then restart the launcher.

5. Start Minecraft once. The mod creates `config/minecraft-ai-chat.json`. Adjust it as needed, then restart Minecraft.

## Configuration

`config/minecraft-ai-chat.json` is created with:

```json
{
  "endpoint": "https://api.openai.com/v1/chat/completions",
  "model": "gpt-4.1-mini",
  "apiKeyEnvironmentVariable": "OPENAI_API_KEY",
  "triggerPrefix": "@ai ",
  "cooldownMs": 3000,
  "requestTimeoutSeconds": 30,
  "maxReplyCharacters": 1000
}
```

- Set `endpoint` and `model` for another provider that implements the OpenAI Chat Completions response format.
- Change `triggerPrefix` to use another marker. Set it to `""` to send every outgoing chat message; this is usually not recommended.
- Keep API keys out of the JSON config, screenshots, commits, and public chat.

## Notes

- This is a client-side mod: its AI replies are shown only to the player running the mod.
- Your prompted message is still sent as normal Minecraft chat to the server. Do not include sensitive information.
- The project has not been built by this repository workflow. Run the build command above locally before installing.
