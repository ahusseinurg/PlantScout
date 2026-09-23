# PlantScout 🌿

Android app that identifies plants from photos (via the free Pl@ntNet API), lets you scan many plants in one session, and generates a combined eradication plan: priority order, safety warnings, removal methods, herbicide options (last resort), disposal, a supplies checklist and a follow-up calendar.

## Build the APK with GitHub (no Android Studio needed)

1. Unzip this project on your computer.
2. On github.com, click **New repository**, give it a name (e.g. `PlantScout`) and create it.
3. On the empty repo page, click **uploading an existing file** and drag in **everything inside** the `PlantScout` folder (`app`, `build.gradle.kts`, `settings.gradle.kts`, `gradle.properties`, `README.md`, `.gitignore`, `.github`). Commit.
   - ⚠️ The `.github` folder is hidden on Mac/Linux, and browsers sometimes skip it. After uploading, check the repo shows a `.github/workflows/build.yml` file. If not: **Add file → Create new file**, type the name `.github/workflows/build.yml`, paste the contents of that file, and commit.
4. Open the **Actions** tab. A "Build APK" run starts automatically (about 3–6 minutes). If Actions asks you to enable workflows, click the green button.
5. When it shows a green tick, get the APK either way:
   - **Releases** (right side of the repo's main page) → latest `PlantScout build N` → `PlantScout.apk`. Easiest from a phone.
   - Or **Actions** → the run → **Artifacts** → `PlantScout-apk` (downloads a zip containing the APK).

## Install on your phone

1. Download `PlantScout.apk` on the phone and open it.
2. Allow "Install unknown apps" for your browser/Files app when prompted.
3. If Play Protect warns that it's an unknown app, choose **Install anyway** — it's your own build, just not from the Play Store.

## First run: free Pl@ntNet key

1. Go to https://my.plantnet.org, create a free account.
2. In your account settings, copy your **API key**.
3. In PlantScout, paste it into the prompt (or menu ⋮ → API key settings).

The free tier allows 500 identifications per day.

## Using the app

- **Scan plant** takes a photo; **From photos** lets you select many photos at once for batch identification.
- Choose what the photo mostly shows (leaf, flower, fruit, bark). Close-ups of one flower or leaf give the best results.
- Tap a result to pick a different match from the top 5, or remove it.
- **Generate eradication plan** combines every scan into one plan. Share or copy it from the menu.

## Notes

- Identification is AI-based and not perfect. Plans flag low-confidence matches and plants that aren't in the weed database, so double-check before removing anything that might be native or protected.
- The weed database (`KnowledgeBase.kt`) covers ~150 common weeds, invasives and dangerous plants by species/genus, with family-level fallbacks. Add entries there to extend it.
- To change the app name or icon, edit `res/values/strings.xml` and `res/drawable/ic_launcher_foreground.xml`, then push; GitHub rebuilds automatically.
