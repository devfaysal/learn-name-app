# নাম শিখি · Nam Shikhi

A native Android picture dictionary for discovering animals, birds, fruits, vegetables, crops, colors, flowers, fish, insects and vehicles familiar to children in Bangladesh. All content ships inside the APK. The app requests **zero permissions**, including no internet permission.

- Ten categories with **20 names each**, for **200 entries**.
- A streamlined home screen with search and illustrated category cards; no offline badge, info button or featured animal banner.
- Every entry includes a Bangla name, a familiar English name and an offline picture: lifelike illustrations or consistent circular color swatches.
- Tap **বাংলা** or **English** beneath an entry or on its detail page to hear its name. Bangla uses Pradeep's male voice; English uses Ryan's male British English voice. All 400 MP3 recordings ship inside the app; playback needs no connection or device speech engine. Tap again to stop. Switching names or languages replaces the previous clip, and leaving a screen or the app stops playback. Button tap sound effects are disabled so names play without an added UI click.
- Written and spoken names use simple words for children, such as Tiger, Elephant, Deer, Buffalo and Bee. Qualifiers remain only when they distinguish separate entries, such as Silver carp and Grass carp; detailed species descriptions stay in the illustration metadata.
- English names use the native sans-serif font at 14–16 sp on cards and up to 28 sp on detail pages. They automatically fit one line; compact screens and larger accessibility text use wider, single-column entry cards.
- Search in Bangla or English; English search ignores case and surrounding spaces.
- Browse larger pictures with previous/next controls.
- Save favorites locally using the heart beside each Bangla name, below the image; see how many entries you have visited. The button never covers the artwork, and English names retain their own full-width line.
- Play a five-question picture quiz with four choices, answer feedback, replay and a locally saved best score.
- Bundled Noto Sans Bengali fonts; light cream and green interface; adaptive phone/tablet layout.
- No accounts, ads, analytics, online images at runtime or external dependencies.

The illustrations use the approved semi-realistic Bengal tiger style: natural proportions, detailed textures, soft lighting and a clean ivory background. The complete subject stays visible. Colors use identical large circles on ivory, with a fine edge to keep white visible and metallic gradients for gold and silver. The collection includes familiar wildlife, domestic animals, cultivated plants and market produce; it does not imply that every species is native to Bangladesh.

## Run

Open this folder in Android Studio and run the `app` configuration. Supports Android 8.0 (API 26) and newer; compiles and targets API 37. Uses Java 17 and the Gradle wrapper.

With the Android SDK, Gradle distribution and Android Gradle plugin already cached, the entire build works offline:

```sh
./gradlew --offline assembleDebug lintDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

The ready-to-install development APK is copied to `artifacts/Nam-Shikhi.apk`. The application ID stays the same so this installs as an update and preserves saved favorites. Release builds are unsigned until you configure your own production signing key. Do not use a development signing key for Play Store distribution.

## Content and pictures

`app/src/main/assets/collection.json` contains the collection. All 200 distinct 480 × 480 WebP pictures are in `app/src/main/assets/pictures/`. The original PNG illustrations and their exact generation prompts, plus color swatches and their palette metadata, are saved in `artifacts/illustrations/`. `scripts/image_sources.json` records the sources and packaged-image hashes. Using Python 3 and Pillow, rebuild the packaged assets without network access:

```sh
python3 scripts/generate_colors.py
python3 scripts/generate_content.py
python3 scripts/verify_content.py
python3 scripts/verify_content.py --apk app/build/outputs/apk/debug/app-debug.apk
python3 scripts/preview_illustrations.py
```

The illustrations were created with the built-in `image_gen` tool, one image per entry. `scripts/illustration_style.json` records the common style and `scripts/illustration_subjects.json` records the subject descriptions. The packaging script only resizes and encodes these saved masters; it does not generate new images. The Crops category includes 20 grains, pulses, oilseeds and spice seeds, with কালোজিরা (Nigella seeds) and ধনে (Coriander seeds) as familiar spice seeds. Pictures show dry grains/seeds and identifying cereal heads. The Insects category includes caterpillar, silkworm and rice bug entries; Vegetables includes কাঁকরোল (Spine gourd). Retired Body Parts and spinach masters remain in the project archive but are not bundled in the app. Category contact sheets are saved in `artifacts/illustration-previews/`.

`scripts/generate_colors.py` draws the 20 color swatches reproducibly with Pillow. Solid fills use explicit RGB values, and color WebPs are encoded losslessly to preserve them. The orange color has its own ID (`color_orange`), separate from the fruit (`orange`); searches can return both, and quiz choices remain within the answer's category.

The Boal image is an AI redrawing based on the user-provided [Wallago attu photograph by Reezwan](https://commons.wikimedia.org/wiki/File:Wallago_attu_1.jpg), under [CC BY-SA 3.0](https://creativecommons.org/licenses/by-sa/3.0/). Its reference credit and adaptation license are included in the About dialog, `app/src/main/assets/IMAGE_CREDITS.txt`, and image metadata.

The old Wikimedia photo provenance is retained separately in `scripts/legacy_photo_sources.json` for reference; those photos are no longer the shipped artwork. Noto Sans Bengali is distributed under the SIL Open Font License; the font attribution and license are bundled in `app/src/main/assets/fonts/LICENSE.txt`.

Fish names and regional fruit names were cross-checked against [Banglapedia's fisheries overview](https://en.banglapedia.org/index.php?title=Fisheries) and [fruit overview](https://en.banglapedia.org/index.php/Fruit). English labels use familiar common names; detailed subject descriptions retain species distinctions for illustration generation.

## Verification

### Bangla and English pronunciation recordings

`app/src/main/assets/audio/bn/` and `audio/en/` each contain 200 recordings generated with the male `bn-BD-PradeepNeural` and `en-GB-RyanNeural` voices respectively, at `-5%` speed using Microsoft's online speech service through `edge-tts`. `scripts/audio_sources.json` and `scripts/audio_sources_en.json` record each spoken text, display name, voice, duration and SHA-256. These synthetic pronunciations can be tuned individually after listening review.

To change a pronunciation without changing its written label, add an entry ID and spoken text to `scripts/audio_pronunciations.json` (Bangla) or `scripts/audio_pronunciations_en.json` (English). Run `python3 scripts/generate_audio.py --language bn --only ENTRY_ID` or `--language en --only ENTRY_ID` in a Python environment with `edge-tts` and `mutagen`. Omitting `--only` resumes all matching recordings for that language; Bangla remains the default language. This development step needs network access; normal builds and app playback are entirely offline. The initial samples remain in `artifacts/voice-samples/`.

### Build and device checks

The asset audit accepts exactly 200 catalog pictures, 400 pronunciation clips, the catalog, two fonts and their required credits/licenses. It rejects stray files of any type, and `--apk` checks the actual packaged files against the source assets byte for byte. Original artwork, retired pictures, voice auditions and screenshots stay under `artifacts/` and are excluded from the APK. The unused duplicate home-banner tiger image has been removed; the catalog tiger picture remains.

```sh
./gradlew --offline assembleDebug assembleDebugAndroidTest lintDebug
python3 scripts/verify_content.py --apk app/build/outputs/apk/debug/app-debug.apk
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb install -r app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk
adb shell am instrument -w com.amarbangladesh.names.test/com.amarbangladesh.names.OfflineSmokeTest
```

The platform-only instrumentation runner checks all 200 bundled entries, images and 400 decodable Bangla/English recordings, the permission-free manifest, actual audio playback, language switching, replacement and stop behavior, completion, activity cleanup, missing-file handling, both search languages, all ten category lists, crop and color detail paging and favorites, separate orange fruit/color search results, single-line English labels without clipping or ellipses, favorite buttons beside names without image overlap, 48dp favorite touch targets and compact pronunciation controls, complete Bangla labels, detail paging, persistent favorites, quiz feedback, all five questions, activity recreation and replay. No test libraries are downloaded. To capture preview screens, add `-e capture true` before the instrumentation component; they are written into the app's external `files/previews/` directory on the test device.

## Repository layout

- `app/src/`: application code, device checks and the complete offline collection.
- `scripts/`: content tooling, pronunciation overrides and source metadata.
- `artifacts/illustrations/`: the 200 active PNG masters with generation/palette metadata; tracked so packaged artwork can be rebuilt.
- `artifacts/references/`: illustration reference provenance.
- `artifacts/archive/illustrations/`: retired artwork retained locally, excluded from Git and the APK.
- Other `artifacts/` files are local build reports, APKs, screenshots and voice auditions, excluded from Git.

Gradle outputs, IDE settings, Python caches, SDK paths and signing credentials are ignored. The checked-in WebP pictures and MP3 recordings are sufficient to build the Android app without regenerating any media. The larger PNG masters are development source files and never enter the APK.
