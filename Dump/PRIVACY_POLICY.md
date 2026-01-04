# Aido Privacy Policy

_Last updated: 30 November 2025_

## 1. Introduction
Aido is built by Risewell Labs to provide AI-assisted writing support across the apps installed on your Android device. This Privacy Policy describes how Aido handles information when you install and use the application.

## 2. Data Safety Summary

**Data We Collect:** 
- **Account Data:** Email address, display name, and uploaded preprompt content (only when you choose to upload preprompts to the marketplace). If you upload without signing in, we create an anonymous account with a Firebase anonymous ID.
- **Usage Data:** We use Google Firebase Analytics to collect anonymous usage statistics (e.g., feature usage, crash reports) to help us improve the app.

**Why We Collect It:** 
- To authenticate users who upload preprompts.
- To store and display your shared preprompts publicly in the marketplace.
- To track downloads/ratings.
- To understand how users interact with the app and fix stability issues.

**How We Store It:** 
- All marketplace data is stored in Firebase Firestore (Google's cloud database). 
- Authentication is handled by Firebase Authentication.
- Usage data is processed by Google Analytics.

**How You Control It:** 
- You can edit or delete your uploaded preprompts anytime from the "My Shared" tab. 
- To delete your entire account and all associated data, contact aiqknow@gmail.com.

**What We Don't Collect:** 
- We do not collect advertising data, unique device identifiers for tracking (other than anonymous app instance IDs), or your regular typing activity. 
- Your keystrokes and text inputs are **NOT** logged, stored, or transmitted except when you explicitly use a trigger command (e.g., "@fixg").

## 3. Use of Accessibility Service

Aido uses Android's Accessibility Service to detect trigger commands in text fields. This service is **ONLY** used to read text when a trigger (e.g., "@fixg") is detected and does **NOT** log, collect, or share any input outside of that explicit user action.

**Important:** When enabled, the Aido Accessibility Service has the technical capability to read text from any input field on your device. However, we only read text when you explicitly use a trigger command, and we do not log, store, or transmit any other typing activity. The service is designed with privacy as a core principle and does not monitor your typing outside of trigger events.

## 4. Data Controller
Risewell Labs ("we", "us", or "our") is the data controller for Aido. You can reach us at aiqknow@gmail.com for any privacy-related questions.

## 5. Information We Collect
Aido is designed to work primarily on-device.

We process the following information solely to provide the app's core functionality:

1. **Trigger Method Data** – Aido offers two trigger detection methods:
   - **Accessibility Service**: When enabled, Android delivers text input events from your active text field so that Aido can detect triggers (for example `@fixg`). The service only reads text from the currently focused input field and only when text changes occur. We do not monitor, log, or transmit your typing activity outside of trigger commands.
   - **Custom Keyboard**: When you use the Aido keyboard as your input method, the keyboard processes your keystrokes locally on your device to detect trigger commands. No keystroke data is logged, stored, or transmitted to any server except when you explicitly activate a trigger command. The keyboard functions like a standard keyboard for all other typing.

2. **Keystroke Data**: Neither the accessibility service nor the custom keyboard logs, stores, or transmits your regular typing. Data is only processed when you use a trigger command (e.g., "@fixg"). At that moment, only the text in the currently input field is sent to the selected AI provider for processing.

3. **AI Prompt Content**:
   - **Built-in AI (Default)**: If you use the default provider, your prompt is sent to a stateless proxy server operated by Risewell Labs which forwards it to the AI model. This server does **not** permanently store your prompts or conversation history.
   - **Gemini API**: If you have enabled the Google Gemini API provider with your own key, the content is sent directly to Google's Gemini API over HTTPS. Risewell Labs does not access these requests.
   - **Custom API**: If you use a custom API endpoint, data is sent directly to that URL.

4. **Settings Data** – Preferences such as your chosen AI provider, offline mode, and any API keys are stored locally on your device using Android DataStore. They are never transmitted to Risewell Labs.

5. **Marketplace Account Data** – When you upload preprompts to the marketplace, we create an account using Firebase Authentication. This may include:
   - **Anonymous Authentication**: If you upload without signing in, we create an anonymous account with a unique ID. No personal information is collected.
   - **Email Authentication**: If you create an account, we store your email address and display name (optional) via Firebase Authentication.

6. **Uploaded Preprompts** – When you share preprompts to the marketplace, we store:
   - Preprompt content (trigger, instruction, example, description)
   - Category and metadata (downloads, ratings)
   - Author ID (Firebase user ID) and author name (display name or "Anonymous")
   - Creation and update timestamps
   - This data is stored in Firebase Firestore and is publicly accessible to all app users.

## 6. How We Use Information
- Detect triggers using your chosen method (accessibility service or keyboard) and generate AI-assisted responses.
- Replace selected text or insert the generated output when requested.
- Remember your local preferences, including your chosen trigger method.
- Authenticate users who upload preprompts to the marketplace.
- Store and display user-uploaded preprompts in the marketplace.
- Track downloads and ratings for shared preprompts.
- Display author names and user-generated content to other users.
- Analyze anonymous usage trends to improve app stability and features (via Firebase Analytics).

## 7. Third-Party Services

Aido uses the following third-party services:

**Google Firebase (Authentication, Firestore, and Analytics)**
- **What we use it for:** User authentication, cloud storage of marketplace data, and anonymous usage analytics.
- **Personal information collected:** Email address/display name (only if you create an account). Analytics data is anonymous.
- **How long it's kept:** Account information is stored until deleted. Analytics data is retained according to Google Analytics retention policies.
- **Privacy Policy:** https://policies.google.com/privacy

**Google Gemini API (Optional)**
- **What we use it for:** AI text generation when you enable the Gemini provider and provide your own API key.
- **Personal information collected:** None by Risewell Labs. Your prompts and responses are sent directly between your device and Google's Gemini API.
- **Privacy Policy:** https://ai.google.dev/gemini-api/terms

**Cloudflare Workers (For Built-in AI)**
- **What we use it for:** Proxying requests for the default "Built-in AI" provider.
- **Data handling:** Acts as a stateless pass-through. Does not store prompt data.

## 8. Sharing of Information
We do not sell your information. Data sharing occurs in the following scenarios:

- **AI Providers**: Data is shared with the selected AI provider (Risewell Labs proxy, Google Gemini, or your Custom API) only when you trigger a command.
- **Firebase Services**: We use Google Firebase for authentication, cloud storage, and analytics.
- **Public Marketplace**: When you upload preprompts to the marketplace, they become publicly visible to all Aido users.
- **Other Users**: Preprompts you share can be viewed, installed, and used by any Aido user.

## 9. Data Storage and Retention
- **Keystroke Data**: Not logged or stored. Keystrokes are processed in real-time only to detect triggers.
- **Trigger Requests**: Text processed for a trigger remains in device memory only for the duration of the request.
- **Local Settings**: Preferences are stored locally on your device using Android DataStore.
- **No Cloud Storage of Prompts**: Risewell Labs does not maintain any cloud backups or databases of your typing, prompts, or AI responses.
- **Marketplace Data**: Uploaded preprompts are stored in Firebase Firestore indefinitely until you delete them.

## 10. Security
We implement the following safeguards:
- **Trigger-Only Processing**: The accessibility service and keyboard only activate when you use a trigger command.
- **No Keystroke Logging**: We do not implement any keystroke logging or screen recording.
- **HTTPS Only**: All network communications use encrypted HTTPS connections.
- **Local Processing**: Trigger detection happens entirely on your device.
- **API Key Protection**: API keys are stored securely in Android's encrypted DataStore.

## 11. Your Choices and Control
- **Choose Trigger Method**: You can choose between Accessibility Service or Custom Keyboard.
- **Disable Anytime**: You can disable the Accessibility Service or switch keyboards at any time.
- **Provider Selection**: Choose between Built-in AI, Google Gemini API, or Custom API.
- **Offline Mode**: Enable offline mode to completely disable all network requests.
- **Account Deletion**: Contact aiqknow@gmail.com to request complete account deletion.

## 12. Children's Privacy
Aido is intended for users aged 13 and above. We do not knowingly collect personal information from children under 13.

## 13. International Transfers
Aido processes data in the following ways:
- Local processing occurs on your device.
- AI processing may occur on servers located worldwide (Google, Cloudflare, or your custom provider).
- Marketplace data is stored using Firebase Firestore on Google's global infrastructure.

## 14. Changes to This Policy
We may update this Privacy Policy to reflect product or legal changes. We will update the "Last updated" date.

## 15. Contact Us
If you have questions or requests regarding privacy, email aiqknow@gmail.com.