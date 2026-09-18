# Compass SF

Compass SF is a fast, offline-first Android street-level resource navigator for San Francisco. It provides real-time community resource directories, hot-meal maps, intake checklists, and AI-assisted street navigation guidance.

## Configuration & GitHub Actions Secrets

### Gemini API Key Setup

The application supports natural-language queries and flyer analysis via the Gemini API. For security, API keys are never hardcoded or committed into source control.

To enable live Gemini features in automated builds and local development:

1. **GitHub Actions Workflow**:
   - In your GitHub repository, navigate to **Settings** > **Secrets and variables** > **Actions**.
   - Click **New repository secret**.
   - Set the **Name** to `GEMINI_API_KEY`.
   - Set the **Value** to your valid Google AI Studio Gemini API key.
   - The canonical `build-apk.yml` workflow automatically passes `${{ secrets.GEMINI_API_KEY }}` into the Gradle build environment.

2. **Local Environment / Build**:
   - Set the `GEMINI_API_KEY` environment variable in your shell or terminal:
     ```bash
     export GEMINI_API_KEY="your-gemini-api-key-here"
     ```
   - If `GEMINI_API_KEY` is not provided or empty, the application compiles normally and automatically activates built-in offline keyword and semantic matching fallbacks.

## Building the APK

To build the debug APK locally:

```bash
./gradlew assembleDebug
```

The output APK will be located at `app/build/outputs/apk/debug/`.
