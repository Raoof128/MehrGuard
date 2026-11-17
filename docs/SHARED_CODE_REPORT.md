# Lines of code per module

Counted on 17 November 2026 with `wc -l` over the files tracked by git. Lines include comments and blank lines, so treat the numbers as sizes and not as a measure of effort or sharing.

| Module | What it holds | Lines | Files |
|---|---|---|---|
| `common/src/commonMain` | Shared engine, model, data, shared UI text | 25,249 | 88 |
| `common/src/commonTest`, `desktopTest`, `iosTest` | Shared tests | 20,171 | 84 |
| `common/src/androidMain`, `iosMain`, `desktopMain`, `jsMain`, `wasmJsMain` | Platform implementations of shared interfaces | 2,655 | 20 |
| `androidApp` | Android app (Kotlin) | 18,097 | 43 |
| `desktopApp` | Desktop app (Kotlin) | 23,577 | 47 |
| `webApp` (Kotlin) | Web app, Kotlin/JS and Kotlin/Wasm | 18,430 | 20 |
| `webApp` (HTML, CSS, JavaScript) | Web front end, without the bundled jsQR library | 41,649 | 37 |
| `iosApp` | iOS app (Swift) | 16,404 | 39 |

**What is shared.** The detection engine, the scoring model, the data layer and their tests are written once in `common/src/commonMain` and compiled for every target. That is 25,249 lines. The platform apps and platform implementations add 120,812 lines of Kotlin, Swift, HTML, CSS and JavaScript, most of it user interface. So the shared module is about a sixth of the hand-written lines (about a quarter if the web front end's HTML, CSS and JavaScript are left out), while all of the detection logic is in it.

Earlier documents described "about 80% shared code" and a total of about 26,000 lines. Neither figure matches the repository and both have been removed.

Reproduce a count with, for example:

```bash
git ls-files common/src/commonMain | grep '\.kt$' | xargs cat | wc -l
```
