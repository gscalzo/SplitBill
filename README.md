# Split Bill Android App

A Clean-MVVM Android application for splitting bills using AI-powered receipt parsing.

## Setup

### Environment Variables

1. Copy the example environment file:
   ```bash
   cp .env.example .env
   ```

2. Edit `.env` and add your OpenAI API key:
   ```
   OPENAI_API_KEY=sk-your-actual-openai-api-key-here
   ```

3. Get your OpenAI API key from [OpenAI Platform](https://platform.openai.com/api-keys)

### Build and Run

```bash
./gradlew assembleDebug
```

## Features

- 📷 Camera and gallery integration for receipt capture
- 🔍 AI-powered receipt parsing using OpenAI gpt-4o-mini
- 📱 Material Design 3 UI with Jetpack Compose
- 💰 UK receipt format support with quantity handling
- 🛡️ Secure API key management via environment variables
- ⚡ Loading spinner during API calls
- 🧪 Mock service for testing without API calls

## API Integration

### Real OpenAI API

The app uses the actual OpenAI API by default with:

- **gpt-4o-mini model** for cost-effective processing
- **Structured JSON output** for reliable parsing
- **Full image quality** for better OCR accuracy
- **Comprehensive error handling** for network issues

### Testing Mode

For development and testing, you can use mock data:

```kotlin
// In MainActivity.kt, change to:
val receiptService: ReceiptParsingService = remember { OpenAIService(useMock = true) }
```

### Loading States

- **Loading spinner** appears during API calls
- **Error handling** with retry functionality
- **Success states** with parsed receipt display

## Architecture

- **UI Layer**: Jetpack Compose with Material Design 3
- **Business Logic**: ViewModels and Use Cases
- **Data Layer**: Repository pattern with mockable services
- **AI Integration**: OpenAI API with structured JSON output
- **Network**: Retrofit with OkHttp for reliable API calls

## Security

- API keys are stored in `.env` files (excluded from git)
- Images are compressed (not resized) to balance quality and API token usage
- All network calls are made asynchronously with proper error handling
- No sensitive data is logged in production builds

