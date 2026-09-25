# ClosetAI

Free AI wardrobe and outfit assistant for Android. Build your closet from camera or gallery photos, get daily outfit suggestions with optional layering, and (optionally) plug in a free [OpenRouter](https://openrouter.ai) API key for smarter item tagging and LLM-enhanced looks.

**Package:** `com.ryebreadseeds.closetai`  
**Price:** Free forever — no Play Billing, no subscription, no third-party wardrobe branding.

## Features

### Closet
- Add items from camera or gallery; photos stay on device
- Categories: Top, Bottom, Dress, Romper, Outerwear, Shoes, Accessory, Other
- Edit name / color / category / season; delete items
- **Search** + **category chip filters**
- **Magic upload** — one photo of many garments → multiple closet items (vision + optional bbox crop; needs API key)
- **What goes with this** — from an item detail, get 3 complementary outfits (like / dislike / save)

### Today
- Occasion: Casual, Work, Date, Gym, Formal
- Optional mood (e.g. cozy, bold)
- Weather via free [Open-Meteo](https://open-meteo.com) (default **Little Falls, NJ**)
- Offline rules engine + optional LLM refinement
- Layering supported; never pairs pants with Dress/Romper
- Like / dislike feedback; disliked combos never repeat
- Weights **liked** outfit colors/categories when generating
- Clear **Why this look** rationale
- **Glow up** — regenerate favoring unused + liked-style pieces

### Smart hub
- **Mix & Match** — fill slots (base top, layer, bottom *or* dress/romper, outerwear, shoes, accessory); Save / Ask AI to complete / Clear
- **Outfit Check** — score Color / Coherence / Occasion (1–10) with tips; works offline, richer with API key
- **Shopping Buddy** — 3–7 concrete gap recommendations (e.g. “navy chinos”); no affiliates, no in-app purchases

### Outfits & Settings
- History of generated / saved looks
- Encrypted OpenRouter API key storage
- Weather city / lat / lon

## Offline vs API key

| Capability | Offline (no key) | With free OpenRouter key |
|---|---|---|
| Closet CRUD, search, filters | Yes | Yes |
| Local color draft tagging | Yes | Yes (vision overrides) |
| Rules outfits + layering + Glow up | Yes | Yes (LLM preferred when key set) |
| Weather-aware suggestions | Yes (Open-Meteo) | Yes |
| Magic upload (multi-item) | Explains need for key → single-item fallback | Yes |
| What goes with this | Rules | Rules + LLM |
| Mix & Match AI complete | Offline fill | LLM complete |
| Outfit Check | Heuristic scores | AI scores + tips |
| Shopping Buddy | Rule gaps | AI gap analysis |
| Like / dislike / history | Yes | Yes |

## Requirements

- Android Studio Ladybug (2024.2+) or newer  
- JDK 17  
- Device/emulator **minSdk 26**  
- Samsung phone: USB + USB debugging

## Pull & run on your phone

```bash
git clone https://github.com/Ryebreadseeds/ClosetAI.git
cd ClosetAI
git pull origin main
```

1. Open the folder in Android Studio (the one with `settings.gradle.kts`).
2. Let Gradle sync (SDK 35).
3. Connect phone with USB debugging on.
4. Select the device → **Run** (green triangle) / Shift+F10.

CLI:

```bash
./gradlew :app:installDebug
adb shell am start -n com.ryebreadseeds.closetai/.MainActivity
```

## Free OpenRouter key (optional)

1. Create a free account at [https://openrouter.ai](https://openrouter.ai).
2. Create an API key.
3. ClosetAI → **Settings** → paste → **Save API key**.
4. Use Magic upload, Glow up, Mix AI complete, Outfit Check, Shopping Buddy for richer results.

Default models: `google/gemini-2.0-flash-001` via OpenRouter (OpenAI-compatible).

## Privacy

- Photos and closet data stay on device.
- Weather uses Open-Meteo with your city/coords.
- With an API key, photos (vision) or inventory text may be sent to your configured endpoint. Clear the key anytime.

## License

Personal / MIT-style use for Ryan (Ryebreadseeds). No third-party wardrobe-app branding.
