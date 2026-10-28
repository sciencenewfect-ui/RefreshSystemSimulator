
package com.refreshsystem.simulator

import android.content.pm.PackageManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetector
import com.google.mlkit.vision.face.FaceDetectorOptions
import com.google.mlkit.vision.label.ImageLabeler
import com.google.mlkit.vision.label.ImageLabeling
import com.google.mlkit.vision.label.defaults.ImageLabelerOptions
import java.util.concurrent.Executors
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

private val Ink = Color(0xFF10131C)
private val Panel = Color(0xFF1A2030)
private val Red = Color(0xFFFF3B45)
private val Cyan = Color(0xFF6BE7F2)
private val Muted = Color(0xFFB7C0D3)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: android.os.Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            MaterialTheme(
                colorScheme = darkColorScheme(
                    background = Ink,
                    surface = Panel,
                    primary = Red,
                    onBackground = Color.White,
                    onSurface = Color.White
                )
            ) {
                RefreshTheoryApp()
            }
        }
    }
}

@Composable
private fun RefreshTheoryApp() {
    var selectedTab by remember { mutableIntStateOf(0) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Ink)
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Text(
            text = "REFRESH THEORY",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Black,
            color = Color.White
        )

        Spacer(Modifier.height(4.dp))

        Text(
            text = "Open System • Refresh • Stability • Information",
            color = Muted
        )

        Spacer(Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            listOf("Robot", "Experiment", "Learn")
                .forEachIndexed { index, title ->
                    FilterChip(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        label = { Text(title) },
                        modifier = Modifier.weight(1f)
                    )
                }
        }

        Spacer(Modifier.height(16.dp))

        when (selectedTab) {
            0 -> RobotWorldScreen()
            1 -> ExperimentScreen()
            2 -> LearnScreen()
        }

        Spacer(Modifier.height(18.dp))

        Text(
            text = "Research hypothesis: “Consciousness is the Stability of an Open System.” " +
                "यह ऐप व्यवहार, जानकारी और बदलाव का simulation है; यह चेतना का वैज्ञानिक प्रमाण नहीं है।",
            color = Muted,
            style = MaterialTheme.typography.bodySmall
        )
    }
}

@Composable
private fun ExperimentScreen() {
    var refreshCount by remember { mutableIntStateOf(0) }
    var environment by remember { mutableFloatStateOf(0.5f) }
    var internalState by remember { mutableFloatStateOf(0.5f) }
    var stability by remember { mutableFloatStateOf(0.72f) }
    var running by remember { mutableStateOf(false) }

    LaunchedEffect(running) {
        while (running) {
            kotlinx.coroutines.delay(1200)

            refreshCount++

            internalState = (
                internalState * 0.65f +
                    environment * 0.25f +
                    kotlin.random.Random.nextFloat() * 0.10f
                ).coerceIn(0f, 1f)

            stability = (
                0.60f +
                    (1f - kotlin.math.abs(internalState - environment)) * 0.40f
                ).coerceIn(0f, 1f)
        }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Panel),
        shape = RoundedCornerShape(22.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                "OPEN SYSTEM EXPERIMENT",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Black
            )

            Text(
                "यह प्रयोग environment, internal state और Stability के बीच संबंध का एक सरल मॉडल है।",
                color = Muted
            )

            StabilityRing(stability)

            Text("Refresh Count: $refreshCount")
            Text("Stability: ${(stability * 100).toInt()}%")

            LinearProgressIndicator(
                progress = { stability },
                modifier = Modifier.fillMaxWidth(),
                color = Cyan
            )

            Text("Internal State: ${(internalState * 100).toInt()}%")
            Text("External Environment")

            Slider(
                value = environment,
                onValueChange = { environment = it },
                valueRange = 0f..1f
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = { running = !running },
                    modifier = Modifier.weight(1f)
                ) {
                    Text(if (running) "Pause" else "Start Experiment")
                }

                OutlinedButton(
                    onClick = {
                        refreshCount++

                        internalState = (
                            (internalState + environment) / 2f
                            ).coerceIn(0f, 1f)

                        stability = (
                            1f - kotlin.math.abs(internalState - environment) * 0.4f
                            ).coerceIn(0f, 1f)
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Manual Refresh")
                }
            }

            Text(
                "यहाँ Stability गणितीय नियम से निकला simulation score है, वास्तविक चेतना का माप नहीं।",
                color = Muted,
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

@Composable
private fun LearnScreen() {
    val lessons = listOf(
        "Refresh Process" to
            "सिस्टम के हिस्से समय के साथ बदलते और अपडेट होते हैं।",

        "Open System" to
            "Open System अपने वातावरण से ऊर्जा, पदार्थ या संकेत लेता और बाहर भेजता है।",

        "Stability" to
            "बदलाव के दौरान सिस्टम के जरूरी संगठन को बनाए रखने की क्षमता को इस मॉडल में Stability कहा गया है।",

        "Information Retention" to
            "जानकारी रिकॉर्ड होने पर उसे बाद में वापस पढ़ा या इस्तेमाल किया जा सकता है।",

        "Memory और Consciousness" to
            "जानकारी रिकॉर्ड करना और उसे सचेत रूप से महसूस करना एक ही बात नहीं है।",

        "Refresh Theory" to
            "यह प्रस्तावित परिकल्पना है कि Open System की Stability और Information Retention का चेतना से संबंध हो सकता है। इसे प्रयोगों से जाँचना आवश्यक है।"
    )

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Panel),
        shape = RoundedCornerShape(22.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                "LEARN REFRESH THEORY",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Black
            )

            lessons.forEachIndexed { index, lesson ->
                HorizontalDivider(color = Color.DarkGray)

                Text(
                    "${index + 1}. ${lesson.first}",
                    color = Cyan,
                    fontWeight = FontWeight.Bold
                )

                Text(lesson.second, color = Color.White)
            }
        }
    }
}

/*
 * जरूरी: यह function private नहीं होना चाहिए।
 * RobotWorld.kt भी इसी CameraPanel() को इस्तेमाल करता है।
 */
@Composable
fun CameraPanel(
    enabled: Boolean,
    onEnabledChange: (Boolean) -> Unit,
    onObservation: (String, Int) -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var hasPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    var previewView by remember { mutableStateOf<PreviewView?>(null) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasPermission = granted
        if (granted) onEnabledChange(true)
    }

    DisposableEffect(enabled, hasPermission, lifecycleOwner, previewView) {
        var cameraProvider: ProcessCameraProvider? = null
        var executor: java.util.concurrent.ExecutorService? = null
        var faceDetector: FaceDetector? = null
        var labeler: ImageLabeler? = null

        if (enabled && hasPermission && previewView != null) {
            val cameraFuture = ProcessCameraProvider.getInstance(context)

            cameraFuture.addListener({
                try {
                    val view = previewView ?: return@addListener
                    cameraProvider = cameraFuture.get()

                    val preview = Preview.Builder()
                        .build()
                        .also { it.setSurfaceProvider(view.surfaceProvider) }

                    val analysis = ImageAnalysis.Builder()
                        .setBackpressureStrategy(
                            ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST
                        )
                        .build()

                    executor = Executors.newSingleThreadExecutor()

                    val faceOptions = FaceDetectorOptions.Builder()
                        .setPerformanceMode(
                            FaceDetectorOptions.PERFORMANCE_MODE_FAST
                        )
                        .build()

                    faceDetector = FaceDetection.getClient(faceOptions)
                    labeler = ImageLabeling.getClient(
                        ImageLabelerOptions.DEFAULT_OPTIONS
                    )

                    val detector = faceDetector!!
                    val imageLabeler = labeler!!
                    val analysisExecutor = executor!!

                    analysis.setAnalyzer(analysisExecutor) { proxy ->
                        analyzeCameraFrame(
                            proxy,
                            detector,
                            imageLabeler
                        ) { text, faceCount ->
                            android.os.Handler(
                                android.os.Looper.getMainLooper()
                            ).post {
                                onObservation(text, faceCount)
                            }
                        }
                    }

                    cameraProvider!!.unbindAll()
                    cameraProvider!!.bindToLifecycle(
                        lifecycleOwner,
                        CameraSelector.DEFAULT_FRONT_CAMERA,
                        preview,
                        analysis
                    )
                } catch (error: Exception) {
                    android.os.Handler(
                        android.os.Looper.getMainLooper()
                    ).post {
                        onObservation(
                            "Camera could not start: ${error.javaClass.simpleName}",
                            0
                        )
                    }
                }
            }, ContextCompat.getMainExecutor(context))
        }

        onDispose {
            try {
                cameraProvider?.unbindAll()
            } catch (_: Exception) {
            }

            try {
                executor?.shutdown()
            } catch (_: Exception) {
            }

            try {
                faceDetector?.close()
            } catch (_: Exception) {
            }

            try {
                labeler?.close()
            } catch (_: Exception) {
            }
        }
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (!hasPermission) {
            Button(
                onClick = {
                    permissionLauncher.launch(
                        android.Manifest.permission.CAMERA
                    )
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Allow Camera Access")
            }

            Text(
                "Live camera analysis के लिए camera permission जरूरी है।",
                color = Muted,
                style = MaterialTheme.typography.bodySmall
            )
        } else {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = { onEnabledChange(!enabled) },
                    modifier = Modifier.weight(1f)
                ) {
                    Text(if (enabled) "Stop Camera" else "Start Camera")
                }

                if (enabled) {
                    Text(
                        "LIVE",
                        color = Red,
                        fontWeight = FontWeight.Black
                    )
                }
            }

            if (enabled) {
                AndroidView(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(250.dp),
                    factory = { viewContext ->
                        PreviewView(viewContext).apply {
                            scaleType = PreviewView.ScaleType.FILL_CENTER
                            previewView = this
                        }
                    },
                    update = { view ->
                        if (previewView !== view) {
                            previewView = view
                        }
                    }
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(90.dp)
                        .background(Panel, RoundedCornerShape(14.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Camera stopped", color = Muted)
                }
            }
        }
    }
}

private fun analyzeCameraFrame(
    proxy: ImageProxy,
    faceDetector: FaceDetector,
    labeler: ImageLabeler,
    onResult: (String, Int) -> Unit
) {
    val mediaImage = proxy.image

    if (mediaImage == null) {
        proxy.close()
        return
    }

    val inputImage = InputImage.fromMediaImage(
        mediaImage,
        proxy.imageInfo.rotationDegrees
    )

    var faceDone = false
    var labelsDone = false
    var faceCount = 0
    var labels = emptyList<String>()
    var completed = false

    fun finishIfReady() {
        if (faceDone && labelsDone && !completed) {
            completed = true

            val scene = if (labels.isEmpty()) {
                "No confident scene labels"
            } else {
                labels.joinToString(", ")
            }

            onResult(
                "Faces detected: $faceCount • Scene labels: $scene",
                faceCount
            )

            proxy.close()
        }
    }

    faceDetector.process(inputImage)
        .addOnSuccessListener { faces ->
            faceCount = faces.size
        }
        .addOnFailureListener {
            // Face analysis failed; scene analysis may still complete.
        }
        .addOnCompleteListener {
            faceDone = true
            finishIfReady()
        }

    labeler.process(inputImage)
        .addOnSuccessListener { results ->
            labels = results
                .filter { it.confidence >= 0.55f }
                .take(4)
                .map { it.text }
        }
        .addOnFailureListener {
            // Scene analysis failed; face analysis may still complete.
        }
        .addOnCompleteListener {
            labelsDone = true
            finishIfReady()
        }
}

@Composable
private fun StabilityRing(stability: Float) {
    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(220.dp)
    ) {
        val center = Offset(size.width / 2f, size.height / 2f)
        val radius = size.minDimension * 0.32f

        drawCircle(
            color = Red.copy(alpha = 0.08f),
            radius = radius * 1.3f,
            center = center
        )

        drawCircle(
            color = Red.copy(alpha = 0.85f),
            radius = radius,
            center = center,
            style = Stroke(width = 8.dp.toPx())
        )

        drawArc(
            color = Cyan,
            startAngle = -90f,
            sweepAngle = 360f * stability.coerceIn(0f, 1f),
            useCenter = false,
            topLeft = Offset(
                center.x - radius,
                center.y - radius
            ),
            size = androidx.compose.ui.geometry.Size(
                radius * 2f,
                radius * 2f
            ),
            style = Stroke(width = 8.dp.toPx())
        )

        repeat(18) { index ->
            val angle = index * 2.0 * PI / 18.0
            val dotRadius = radius * (0.45f + (index % 4) * 0.1f)

            val point = Offset(
                center.x + cos(angle).toFloat() * dotRadius,
                center.y + sin(angle).toFloat() * dotRadius
            )

            drawCircle(
                color = if (index % 3 == 0) Cyan else Color.White,
                radius = 3.dp.toPx(),
                center = point
            )
        }
    }
}
