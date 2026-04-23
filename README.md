# ZPLAY Hero Challenge Android Sample

Android sample app for the ZPLAY Hero Challenge hiring task.

The app uses CameraX and MediaPipe Pose Landmarker as an Android alternative to iOS Vision body pose detection. It runs pose detection on the live front-camera stream, tracks up to 4 people at the same time, and draws a real-time skeleton overlay using 19 major body landmarks.

## Tech Stack

- Kotlin
- Jetpack Compose
- CameraX
- MediaPipe Tasks Vision Pose Landmarker
- MVVM + Repository
- ViewModel + StateFlow
- On-device model: `pose_landmarker_lite.task`

## Features

- Real-time front-camera pose detection
- Up to 4 simultaneous people with `setNumPoses(4)`
- 19 Vision-like key landmarks selected from MediaPipe's 33 pose landmarks
- Per-person colored skeleton overlay
- Camera permission handling

## Why MediaPipe?

I selected MediaPipe Pose Landmarker instead of raw TensorFlow Lite / MoveNet MultiPose because MediaPipe is conceptually closest to Apple's iOS Vision body pose API.

Both iOS Vision and MediaPipe expose a high-level pose detection pipeline:

```text
camera frame -> body landmarks -> confidence scores -> skeleton overlay
```

This keeps the Android architecture close to the existing iOS implementation. The app does not need to manually implement model output tensor decoding, multi-person landmark grouping, or pose tracking logic.

MediaPipe Pose Landmarker also supports live stream mode and `setNumPoses(4)`, which matches the requirement to detect up to 4 people simultaneously. MediaPipe returns 33 pose landmarks; this sample maps them to 19 major landmarks to match the iOS Vision-style requirement.

## Architecture

```text
MainActivity / Compose UI
        |
PoseViewModel
        |
PoseRepository
        |
PoseLandmarkerHelper
        |
CameraFramePreprocessor
        |
CameraX ImageAnalysis ImageProxy
```

- `MainActivity`: owns the Android screen and camera permission UI.
- `PoseViewModel`: exposes `StateFlow<PoseUiState>` and survives configuration changes.
- `PoseRepository`: owns the pose detection pipeline state.
- `PoseLandmarkerHelper`: configures MediaPipe Pose Landmarker in `LIVE_STREAM` mode.
- `CameraFramePreprocessor`: converts `ImageProxy -> Bitmap -> MPImage`.

For a raw TFLite / MoveNet implementation, `CameraFramePreprocessor` is the place where the `ImageProxy -> Bitmap -> resized/normalized tensor` preprocessing pipeline would feed a TFLite interpreter. In this MediaPipe implementation, the final input object is `MPImage`, because MediaPipe Tasks handles model-specific tensor preparation internally.

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
