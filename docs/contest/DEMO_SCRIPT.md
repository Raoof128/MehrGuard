# Mehr Guard Demo Video Script

> **Contest-era document.** Written for the KotlinConf 2025–26 student contest submission and not re-verified since. An audit in November 2026 could not reproduce some figures found in documents of this period (accuracy, latency, corpus and training-set sizes, "zero network calls"). The measured figures are in the [README summary](../README.md#summary) and [EVALUATION.md](../docs/EVALUATION.md).

## Video Specifications

- **Duration**: 90-120 seconds
- **Resolution**: 1920x1080 (16:9)
- **Frame Rate**: 60fps
- **Audio**: Background music + voiceover

---

## Shot-by-Shot Storyboard

### SCENE 1: Hook (0:00 - 0:05)
**Duration**: 5 seconds

**Visual**: 
- Close-up of smartphone scanning QR code at coffee shop counter
- Slow motion capture of scan completion
- Dark overlay fades in

**On-Screen Text**:
```
"587 increase in QR phishing attacks"
— Source: 2024 Cyber Threat Report
```

**Narration**:
> "QR codes are everywhere. So are the threats."

**Transition**: Fade to black

---

### SCENE 2: Problem Statement (0:05 - 0:15)
**Duration**: 10 seconds

**Visual**:
- Quick montage (1.5s each):
  1. Restaurant menu QR code
  2. Parking meter payment QR
  3. Email with QR code
  4. Fake delivery notification

**On-Screen Text**:
```
QRishing: Phishing via QR Codes
71% of users never verify URLs before scanning
```

**Narration**:
> "Attackers embed malicious URLs in QR codes. 71% of users never check before scanning. One wrong scan could cost you everything."

**Transition**: Swoosh to app

---

### SCENE 3: App Introduction (0:15 - 0:22)
**Duration**: 7 seconds

**Visual**:
- Mehr Guard app icon on home screen
- Finger tap animation
- App launch with animated shield logo
- Logo settles into header

**On-Screen Text**:
```
Introducing Mehr Guard
Your Cross-Platform Phishing Detector
```

**Narration**:
> "Introducing Mehr Guard. Intelligent protection against QR phishing."

**Transition**: Slide to camera view

---

### SCENE 4: Safe Scan Demo (0:22 - 0:35)
**Duration**: 13 seconds

**Visual**:
- Camera viewfinder active
- QR code for google.com comes into frame
- Scanning animation (pulsing ring)
- Result screen appears

**Screen Recording Shows**:
```
┌────────────────────┐
│       [ 8 ]        │
│       SAFE         │
│        ✅          │
│                    │
│ google.com/search  │
│                    │
│ ✓ HTTPS verified   │
│ ✓ Known domain     │
│ ✓ No risk signals  │
└────────────────────┘
```

**Narration**:
> "Scan any QR code. Get instant analysis. This official Google link? Score of 8. Completely safe."

**Transition**: Quick cut

---

### SCENE 5: Suspicious Scan Demo (0:35 - 0:48)
**Duration**: 13 seconds

**Visual**:
- New QR code in frame (URL shortener)
- Scan animation
- Result with warning state

**Screen Recording Shows**:
```
┌────────────────────┐
│       [ 45 ]       │
│    SUSPICIOUS      │
│        ⚠️          │
│                    │
│ bit.ly/3xYz123     │
│                    │
│ ⚠ URL shortener    │
│ ⚠ Unknown dest.    │
│ ⚠ Cannot verify    │
└────────────────────┘
```

**Narration**:
> "A shortened URL? Mehr Guard flags it as suspicious. The destination is hidden—proceed with caution."

**Transition**: Dramatic pause

---

### SCENE 6: Malicious Scan Demo (0:48 - 1:05)
**Duration**: 17 seconds

**Visual**:
- QR code for fake PayPal comes into frame
- Scan animation intensifies
- Result appears with danger state
- Camera zooms into risk factors

**Screen Recording Shows**:
```
┌────────────────────────┐
│        [ 87 ]          │
│      MALICIOUS         │
│          ❌            │
│                        │
│ paypa1-secure.tk/login │
│                        │
│ ❌ Brand Impersonation │
│    Mimics: PayPal      │
│ ❌ High-Risk TLD (.tk) │
│ ❌ Login page keywords │
│ ❌ HTTP not HTTPS      │
│                        │
│ [ ⚔️ Proceed Anyway ]  │
│ [ ✅ Stay Safe ]       │
└────────────────────────┘
```

**Narration**:
> "Now watch this. A fake PayPal login page. Mehr Guard catches the typosquat—that's a number one, not the letter L. High-risk domain. Credential harvesting attempt. Score: 87. Blocked."

**Transition**: Slide to multi-platform

---

### SCENE 7: Cross-Platform Showcase (0:55 - 1:08)
**Duration**: 13 seconds

**Visual**:
- Split screen animation:
  - Top left: Android phone
  - Top right: iPhone
  - Bottom left: MacBook
  - Bottom right: Browser window
- All showing same Mehr Guard interface
- Synchronized scanning animation

**On-Screen Text**:
```
One Codebase. Every Platform.
Android • iOS • Desktop • Web
```

**Narration**:
> "Powered by Kotlin Multiplatform. One codebase protects you across Android, iOS, Desktop, and Web. Same security, everywhere."

**Transition**: Feature montage

---

### SCENE 8: Features Highlight (1:08 - 1:18)
**Duration**: 10 seconds

**Visual**:
Quick cuts (2s each) with animated icons:

1. **Shield with checkmarks**
   - "25+ Security Heuristics"
   
2. **Brain/AI icon with three nodes**
   - "Ensemble ML: Logistic Regression + Gradient Boosting + Rules"
   
3. **Target with brand logos + pattern icon**
   - "Brand Detection: 500+ static + dynamic pattern discovery"
   
4. **Lock with offline symbol**
   - "100% Offline & Private (<5ms analysis)"

5. **Game controller icon**
   - "Beat the Bot: Challenge the AI!"

**Narration**:
> "Twenty-five security checks. Ensemble machine learning with three models. Brand protection with both known brands and dynamic pattern discovery. All running locally in under 5 milliseconds—your data never leaves your device. Plus, challenge our AI in Beat the Bot mode!"

**Transition**: Fade to closing

---

### SCENE 9: Closing (1:18 - 1:30)
**Duration**: 12 seconds

**Visual**:
- Mehr Guard logo animation
- Shield pulsing with protective glow
- App store badges appear
- GitHub logo with stars

**On-Screen Text**:
```
Mehr Guard
Scan Smart. Stay Protected.

⭐ Open Source on GitHub
📱 Available on all platforms
```

**Narration**:
> "Mehr Guard. Because every scan matters. Open source, free forever, and available now. Scan smart. Stay protected."

**Final Frame**:
- Logo centered
- GitHub URL
- Social handles
- "Made with Kotlin Multiplatform"

---

## Audio Notes

### Background Music
- Style: Electronic, upbeat, slightly tense
- Drops during malicious detection reveal
- Swells during cross-platform showcase

### Sound Effects
- Scan confirmation: Subtle ping
- Safe result: Positive chime
- Suspicious: Warning tone
- Malicious: Alert sound (not jarring)
- Logo animation: Swoosh + settle

### Voiceover
- Tone: Professional, confident, slightly urgent
- Pace: Medium-fast, clear enunciation
- Gender: Neutral voice or alternating
