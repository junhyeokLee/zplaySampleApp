package com.example.sample

import android.Manifest
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.sample.pose.HeroPoseLandmarks
import com.example.sample.pose.PoseFrame
import com.example.sample.pose.PoseLandmarkerHelper
import com.example.sample.ui.theme.SampleTheme
import java.util.concurrent.Executors
import kotlin.math.max

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SampleTheme {
                HeroChallengeApp()
            }
        }
    }
}

@Composable
private fun HeroChallengeApp() {
    val context = LocalContext.current
    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                android.content.pm.PackageManager.PERMISSION_GRANTED
        )
    }
    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasCameraPermission = granted
    }

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) launcher.launch(Manifest.permission.CAMERA)
    }

    Surface(modifier = Modifier.fillMaxSize(), color = Color.Black) {
        if (hasCameraPermission) {
            PoseCameraScreen()
        } else {
            PermissionScreen(onRequest = { launcher.launch(Manifest.permission.CAMERA) })
        }
    }
}

@Composable
private fun PoseCameraScreen() {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }
    var poseFrame by remember { mutableStateOf(PoseFrame()) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val poseHelper = remember {
        PoseLandmarkerHelper(
            context = context,
            onResult = { poseFrame = it },
            onError = {
                Log.e("HeroPose", it)
                errorMessage = it
            }
        )
    }

    DisposableEffect(Unit) {
        onDispose {
            poseHelper.close()
            cameraExecutor.shutdown()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { viewContext ->
                val previewView = PreviewView(viewContext).apply {
                    scaleType = PreviewView.ScaleType.FILL_CENTER
                    implementationMode = PreviewView.ImplementationMode.COMPATIBLE
                }

                val cameraProviderFuture = ProcessCameraProvider.getInstance(viewContext)
                cameraProviderFuture.addListener(
                    {
                        val cameraProvider = cameraProviderFuture.get()
                        val preview = Preview.Builder().build().also {
                            it.setSurfaceProvider(previewView.surfaceProvider)
                        }
                        val imageAnalysis = ImageAnalysis.Builder()
                            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                            .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_YUV_420_888)
                            .build()
                            .also { analysis ->
                                analysis.setAnalyzer(cameraExecutor) { imageProxy ->
                                    poseHelper.detectLiveStream(imageProxy)
                                }
                            }

                        cameraProvider.unbindAll()
                        cameraProvider.bindToLifecycle(
                            lifecycleOwner,
                            CameraSelector.DEFAULT_FRONT_CAMERA,
                            preview,
                            imageAnalysis,
                        )
                    },
                    ContextCompat.getMainExecutor(viewContext)
                )
                previewView
            }
        )

        PoseOverlay(
            poseFrame = poseFrame,
            mirrorHorizontally = true,
            modifier = Modifier.fillMaxSize(),
        )

        TopHud(
            detectedCount = poseFrame.poses.size,
            errorMessage = errorMessage,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .padding(top = 28.dp, start = 16.dp, end = 16.dp),
        )

        BottomHud(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(16.dp),
        )
    }
}

@Composable
private fun PoseOverlay(
    poseFrame: PoseFrame,
    mirrorHorizontally: Boolean,
    modifier: Modifier = Modifier,
) {
    val colors = listOf(
        Color(0xFF20D3E6),
        Color(0xFF31D07E),
        Color(0xFFFFA000),
        Color(0xFFB36DFF),
    )

    Canvas(modifier = modifier) {
        fun PoseFrame.project(x: Float, y: Float): Offset {
            if (imageWidth <= 0 || imageHeight <= 0) {
                val rawX = x * size.width
                return Offset(
                    x = if (mirrorHorizontally) size.width - rawX else rawX,
                    y = y * size.height,
                )
            }

            val scale = max(size.width / imageWidth, size.height / imageHeight)
            val renderedWidth = imageWidth * scale
            val renderedHeight = imageHeight * scale
            val offsetX = (size.width - renderedWidth) / 2f
            val offsetY = (size.height - renderedHeight) / 2f
            val rawX = offsetX + x * renderedWidth

            return Offset(
                x = if (mirrorHorizontally) size.width - rawX else rawX,
                y = offsetY + y * renderedHeight,
            )
        }

        poseFrame.poses.forEachIndexed { index, pose ->
            val color = colors[index % colors.size]
            HeroPoseLandmarks.skeleton.forEach { (startId, endId) ->
                val start = pose.landmarks[startId]
                val end = pose.landmarks[endId]
                if (start != null && end != null && start.confidence > 0.35f && end.confidence > 0.35f) {
                    drawLine(
                        color = color,
                        start = poseFrame.project(start.x, start.y),
                        end = poseFrame.project(end.x, end.y),
                        strokeWidth = 7f,
                        cap = StrokeCap.Round,
                    )
                }
            }

            pose.landmarks.values.forEach { point ->
                if (point.confidence > 0.35f) {
                    val center = poseFrame.project(point.x, point.y)
                    drawCircle(color = Color.White, radius = 9f, center = center)
                    drawCircle(color = color, radius = 6f, center = center)
                }
            }
        }
    }
}

@Composable
private fun TopHud(
    detectedCount: Int,
    errorMessage: String?,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .background(Color(0xAA111827), RoundedCornerShape(8.dp))
            .padding(horizontal = 14.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column {
            Text(
                text = "ZPLAY Hero Challenge",
                color = Color.White,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = errorMessage ?: "MediaPipe Pose Landmarker - 19 landmarks / up to 4 people",
                color = if (errorMessage == null) Color(0xFF9BE7F2) else Color(0xFFFFB4AB),
                style = MaterialTheme.typography.bodySmall,
            )
        }
        Text(
            text = "$detectedCount/4",
            color = Color.White,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
private fun BottomHud(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .background(Color(0xAA020617), RoundedCornerShape(8.dp))
            .padding(vertical = 12.dp),
    ) {
        HudStat(label = "정확도", value = "LIVE", modifier = Modifier.weight(1f))
        HudStat(label = "균형", value = "POSE", modifier = Modifier.weight(1f))
        HudStat(label = "유연성", value = "4P", modifier = Modifier.weight(1f))
    }
}

@Composable
private fun HudStat(label: String, value: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = label,
            color = Color(0xFF67E8F9),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = value,
            color = Color.White,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
private fun PermissionScreen(onRequest: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = "카메라 권한이 필요합니다",
            color = Color.White,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
        )
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = onRequest, modifier = Modifier.size(width = 180.dp, height = 48.dp)) {
            Text(text = "권한 허용")
        }
    }
}
