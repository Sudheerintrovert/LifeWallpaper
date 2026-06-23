# Life Progress Wallpaper — Android

A minimal, battery-optimized Android Live Wallpaper that shows your life progress and goal countdown on your home screen.

## Setup

1. Open this project in **Android Studio**
2. Let Gradle sync
3. Connect your phone (USB debugging ON) or use emulator
4. Click **Run** ▶️

## Install on Phone

### Option A: Android Studio (Easiest)
1. Open project in Android Studio
2. Connect phone via USB
3. Click Run → it installs automatically

### Option B: Build APK manually
1. In Android Studio: **Build → Build Bundle(s) / APK(s) → Build APK(s)**
2. Find `app-debug.apk` in `app/build/outputs/apk/debug/`
3. Transfer to phone → Install

### Option C: GitHub + GitHub Actions (Auto-build)
1. Push this repo to GitHub
2. It auto-builds with Gradle
3. Download APK from Actions tab

## Set as Wallpaper

1. After install, long-press home screen
2. Tap **Wallpaper**
3. Select **Life Wallpaper**
4. Tap **Set Wallpaper**

## Customize

1. Long-press home screen → Wallpaper → Life Wallpaper → **Settings** (⚙️ icon)
2. Or open the **Life Wallpaper** app from app drawer
3. Configure:
   - Date of Birth
   - Goal Deadline
   - Goal Text
   - Theme (AMOLED Dark / Light)
   - Text Color
   - Font Size & Family
   - Divider Style
   - Breathing Effect

## Features

- ✅ Real-time countdown (updates every minute via TIME_TICK)
- ✅ AMOLED black background (battery friendly)
- ✅ Light theme support
- ✅ Fade-in animation on load
- ✅ Breathing animation for goal text
- ✅ Minimal CPU usage
- ✅ Works on low-end devices
- ✅ Home screen & Lock screen support

## Project Structure

```
LifeWallpaper/
├── app/src/main/
│   ├── AndroidManifest.xml
│   ├── java/com/lifewallpaper/
│   │   ├── service/LifeWallpaperService.kt    # Core wallpaper rendering
│   │   └── ui/SettingsActivity.kt              # Settings screen
│   └── res/
│       ├── layout/activity_settings.xml
│       ├── values/strings.xml, styles.xml
│       └── xml/wallpaper.xml
├── build.gradle.kts
└── settings.gradle.kts
```

## Requirements

- Android 7.0+ (API 24+)
- Android Studio
