# ClosetAI

Free AI wardrobe and outfit assistant for Android. Build your closet from camera or gallery photos, get daily outfit suggestions with optional layering, and (optionally) plug in a free [OpenRouter](https://openrouter.ai) API key for smarter item tagging and LLM-enhanced looks.

**Package:** `com.ryebreadseeds.closetai`  
**Price:** Free forever — no Play Billing, no subscription.

## Features

- **Closet** — add items from camera or gallery; photos stay on device. Categories: Top, Bottom, Dress, Romper, Outerwear, Shoes, Accessory, Other. Edit name / color / category / season; delete items.
- **Background / vision assist** — with an OpenRouter (OpenAI-compatible) key in Settings, a vision model suggests name, category, color, and season. Without a key, a local color heuristic still fills a draft.
- **Outfit generator**
  - Occasion: Casual, Work, Date, Gym, Formal
  - Optional mood (e.g. cozy, bold)
  - Weather via free [Open-Meteo](https://open-meteo.com) (city or lat/lon; default **Little Falls, NJ**)
  - Layering supported (e.g. base top under shirt + outerwear)
  - Never pairs pants/bottoms with Dress or Romper
  - Offline rules engine always works (color harmony + category slots)
  - With API key: LLM can refine outfits from your inventory metadata
- **Feedback** — like / dislike; disliked combinations never repeat; delete any saved suggestion
- **Screens** — Today · Closet · Outfits · Settings (Material 3, dark-friendly UI)

## Offline vs API key

| Capability | Offline (no key) | With free OpenRouter key |
|---|---|---|
| Add / edit / delete closet items | Yes | Yes |
| Local color-based draft tagging | Yes | Yes (overridden by vision model) |
| Rules-engine outfits + layering | Yes | Yes (fallback if LLM fails) |
| Weather-aware suggestions | Yes (Open-Meteo) | Yes |
| Vision name/category/color | — | Yes |
| LLM outfit tips from inventory | — | Yes |
| Like / dislike / history | Yes | Yes |

API keys are stored with **EncryptedSharedPreferences** on device. Other preferences use DataStore.

## Requirements

- Android Studio Ladybug (2024.2+) or newer recommended  
- JDK 17  
- Android device or emulator, **minSdk 26** (Android 8.0+)  
- For a physical Samsung phone: USB cable + USB debugging

## Open in Android Studio

1. Clone this repo:
   ```bash
   git clone https://github.com/Ryebreadseeds/ClosetAI.git
   ```
2. Open Android Studio → **File → Open** → select the `ClosetAI` folder (the one with `settings.gradle.kts`).
3. Let Gradle sync. If prompted for an SDK, install **Android SDK 35** and build tools.
4. Android Studio usually creates `local.properties` with `sdk.dir=...`. If missing, copy `local.properties.example` and set your SDK path.

## Install on a Samsung phone (e.g. S26 Ultra) via USB

1. **On the phone**
   - Settings → About phone → tap **Build number** seven times to enable Developer options.
   - Settings → Developer options → turn on **USB debugging**.
   - (Optional) Enable **Install via USB** if shown.
2. **Connect** the phone with a USB-C cable. When prompted, allow USB debugging for this computer (check “Always allow” if you trust it).
3. **In Android Studio**
   - Wait until your device appears in the device dropdown (top toolbar).
   - Select **app** run configuration.
   - Click **Run** (green triangle) or press Shift+F10.
4. Accept any “Install” prompt on the phone. ClosetAI will launch with the hanger icon.

### Command-line alternative

```bash
./gradlew :app:installDebug
adb shell am start -n com.ryebreadseeds.closetai/.MainActivity
```

## Free OpenRouter key (optional, smarter AI)

1. Create a free account at [https://openrouter.ai](https://openrouter.ai).
2. Create an API key in the dashboard.
3. In ClosetAI → **Settings** → paste the key → **Save API key**.
4. Add a new closet photo — tagging should use a vision model. Regenerate outfits on **Today** for LLM-enhanced picks when the key is present.

Default models (changeable later in code / prefs): `google/gemini-2.0-flash-001` via OpenRouter. You can use any OpenAI-compatible base URL.

OpenRouter offers free and low-cost models; ClosetAI never requires a paid app subscription.

## Project structure

```
app/src/main/java/com/ryebreadseeds/closetai/
  ClosetAiApp.kt / MainActivity.kt
  data/          Room DB, entities, DAOs, repositories
  domain/        Categories, rules engine, color harmony
  ai/            OpenRouter client + local image fallback
  weather/       Open-Meteo client
  ui/            Today, Closet, Outfits, Settings (Compose)
```

## Privacy

- Photos and closet data stay on your device (app private storage).
- Weather uses Open-Meteo with your chosen city/coordinates.
- If you set an API key, item photos (vision) or inventory text (outfit tips) are sent to the OpenAI-compatible endpoint you configured (default OpenRouter). Clear the key anytime in Settings.

## License

Personal / MIT-style use for Ryan (Ryebreadseeds). No third-party wardrobe-app branding.
