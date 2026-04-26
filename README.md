# ZPLAY Android Sample

## 빌드 방법

프로젝트 루트에서 아래 명령어를 실행합니다.

```bash
./gradlew assembleDebug
```

빌드가 완료되면 APK는 아래 경로에 생성됩니다.

```text
app/build/outputs/apk/debug/app-debug.apk
```

## 설치 및 실행

연결된 안드로이드 기기에 설치하고 실행하려면 아래 명령어를 순서대로 실행합니다.

```bash
./gradlew installDebug
adb shell am start -n com.example.sample/.MainActivity
```

설치 후 앱이 자동으로 실행되도록 위 명령어를 함께 사용합니다.

앱 실행 후 카메라 권한을 허용하면 후면 카메라 화면에서 4인 동시 동작인식과 스켈레톤 오버레이를 확인할 수 있습니다.

## 역할 설명

- `data/model`  
  포즈 관련 데이터 클래스

- `data/processor`  
  카메라 프레임 전처리, MediaPipe 추론 처리

- `data/repository`  
  추론 결과를 UI 상태 흐름으로 연결

- `domain`  
  스켈레톤 smoothing, 4개 레인 슬롯 배정 로직

- `presentation/viewmodel`  
  ViewModel, UI 상태 관리

- `presentation/screen`  
  화면 단위 Compose 구성

- `presentation/component`  
  카메라 프리뷰, 오버레이, 공통 UI 컴포넌트

## 구현 메모

- MediaPipe Pose Landmarker를 사용하여 최대 4인 동시 포즈 인식을 수행합니다.
- `setNumPoses(4)` 설정으로 최대 4명까지 인식하도록 구성했습니다.
- 후면 카메라 기준으로 동작하며, 가로 모드에 최적화된 4개 레인 UI를 제공합니다.
- 19개 주요 랜드마크를 기준으로 실시간 스켈레톤 오버레이를 표시합니다.
- CameraX `ImageAnalysis` 기반으로 프레임을 처리합니다.
- RGBA 프레임을 사용하여 불필요한 JPEG round-trip을 줄였습니다.
- `pose_landmarker_full.task` 모델을 사용하여 정확도 중심으로 구성했습니다.
- adaptive smoothing과 lane hysteresis를 적용하여 스켈레톤 움직임이 보다 자연스럽게 보이도록 보정했습니다.
