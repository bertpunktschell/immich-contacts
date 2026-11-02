# Immich Contacts

Android app to set contact photos from your Immich photo library.

## What it does

- Connect to your self-hosted Immich server
- Browse your Immich people (face recognition) and photo library
- Pick any image, crop it in-app (square, zoom/pan)
- Set the cropped image as an Android contact photo

## Tech

- Kotlin + Jetpack Compose
- Immich REST API (read-only)
- Android ContactsContract API
- uCrop for image cropping
- Material 3

## Permissions

- `READ_CONTACTS`, `WRITE_CONTACTS` — manage contact photos
- `INTERNET` — talk to Immich server

## Immich API Key

Create an API key in Immich Web UI → User Settings → API Keys with these permissions:

- `user.read`
- `asset.read`
- `asset.view`
- `asset.download`
- `person.read`
- `face.read`
- `person.statistics`

No upload/delete permissions needed — the app is read-only.