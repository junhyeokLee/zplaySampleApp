# ZPLAY Hero Challenge Android Sample

Android sample app for the ZPLAY Hero Challenge hiring task.

The app uses CameraX and MediaPipe Pose Landmarker as an Android alternative to iOS Vision body pose detection. It runs pose detection on the live front-camera stream, tracks up to 4 people at the same time, and draws a real-time skeleton overlay using 19 major body landmarks.

## Tech Stack

- Kotlin
- Jetpack Compose
- CameraX
- MediaPipe Tasks Vision Pose Landmarker
- On-device model: `pose_landmarker_lite.task`

## Features

- Real-time front-camera pose detection
- Up to 4 simultaneous people with `setNumPoses(4)`
- 19 Vision-like key landmarks selected from MediaPipe's 33 pose landmarks
- Per-person colored skeleton overlay
- Camera permission handling

## Build

```bash
./gradlew assembleDebug
```

The debug APK will be generated at:

```text
app/build/outputs/apk/debug/app-debug.apk
```

## Run

Install on a connected Android device:

```bash
./gradlew installDebug
```

Open the app and allow camera permission. The app uses the front camera and overlays skeleton lines and landmark dots over the live preview.

## Implementation Notes

MediaPipe Pose Landmarker returns 33 pose landmarks. This sample maps those results to 19 major landmarks to match the iOS Vision-style requirement:

- nose
- left/right eye
- left/right ear
- left/right shoulder
- left/right elbow
- left/right wrist
- left/right hip
- left/right knee
- left/right ankle
- left/right foot index

The model file is included under `app/src/main/assets/pose_landmarker_lite.task`, so no runtime network access is required.
