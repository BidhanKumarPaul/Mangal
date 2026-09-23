# Mangal — Privacy Policy (stub, fill in before publishing)

_Last updated: [DATE]_

Mangal is designed to run entirely on your device. This policy explains what
that means concretely, since Play Console will still require this document
for apps requesting SMS/CALL_PHONE permissions regardless of whether you
"seem" privacy-friendly.

## What Mangal does NOT do
- Does not send your voice recordings, transcripts, conversation history, or
  model prompts/outputs to any server. All AI inference (speech-to-text,
  language model, tool-call decisions) happens locally using on-device models.
- Does not include analytics, crash reporting, or advertising SDKs [update
  this line if you add any — it will no longer be true].
- The one exception: downloading a model file in the Model Manager screen
  requires internet access to fetch the file from [YOUR HOSTING/CDN HERE].
  No data about you is sent as part of that request beyond a standard HTTP
  GET (IP address, as with any download).

## What Mangal stores, and where
- Conversation history and app settings are stored in an encrypted local
  database (SQLCipher) on your device only, in the app's private storage.
  You can clear this at any time from Settings.
- Downloaded model files are stored in the app's private storage.

## Permissions and why Mangal asks for them
- **Microphone**: to capture your voice for on-device transcription.
- **SMS / Phone**: only used when you explicitly ask Mangal to send a text
  or place a call; never used automatically or in the background.
- **Calendar**: only used when you ask Mangal to create an event.
- **Camera**: used only to access the flashlight/torch hardware; Mangal does
  not capture images or video.
- Other permissions (Wi-Fi/Bluetooth settings, exact alarms, notifications)
  are used only to carry out the specific action you asked for.

## Restricted permissions declaration (SMS/CALL_PHONE)
Google Play requires apps requesting SMS or Call Log permissions to justify
this via the Permissions Declaration Form in Play Console, and generally
expects the app to be a candidate default handler for one of those
categories, or to remove the permission. **Flagging for your review before
Phase 6 submission — this may require either restructuring how SMS/calling
works (e.g., handing off to an intent instead of using SmsManager directly)
or accepting that this feature may not clear Play review as-is.**

## Contact
[YOUR CONTACT EMAIL]

## Changes
[How you'll notify users of policy changes, if at all — required boilerplate.]
