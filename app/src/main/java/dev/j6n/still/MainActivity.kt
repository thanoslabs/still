package dev.j6n.still

import android.Manifest
import android.content.ContentValues
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.core.content.ContextCompat
import androidx.compose.ui.graphics.asImageBitmap
import android.graphics.BitmapFactory
import androidx.exifinterface.media.ExifInterface
import android.graphics.Bitmap
import android.widget.Toast
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { StillApp() }
    }
}

enum class StillScreen { CAMERA, REVIEW, LOOKS }

@Composable
fun StillApp() {
    var screen by remember { mutableStateOf(StillScreen.CAMERA) }
    var lastPhoto by remember { mutableStateOf<Uri?>(null) }
    MaterialTheme(colorScheme = darkColorScheme()) {
        Scaffold(bottomBar = {
            NavigationBar {
                StillScreen.entries.forEach { destination ->
                    NavigationBarItem(
                        selected = screen == destination,
                        onClick = { screen = destination },
                        icon = { Text(when (destination) {
                            StillScreen.CAMERA -> "◉"
                            StillScreen.REVIEW -> "▣"
                            StillScreen.LOOKS -> "◈"
                        }) },
                        label = { Text(destination.name.lowercase().replaceFirstChar { it.uppercase() }) }
                    )
                }
            }
        }) { padding ->
            Box(Modifier.fillMaxSize().padding(padding)) {
                when (screen) {
                    StillScreen.CAMERA -> CameraScreen { uri ->
                        lastPhoto = uri
                        screen = StillScreen.REVIEW
                    }
                    StillScreen.REVIEW -> PhotoReview(lastPhoto)
                    StillScreen.LOOKS -> Column(
                        Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text("still.", style = MaterialTheme.typography.displayLarge)
                        Text("Film-inspired looks and custom recipes")
                        Column(Modifier.verticalScroll(rememberScrollState()).padding(20.dp)) {
                            CinematicLook.entries.filter { it != CinematicLook.ORIGINAL }.forEach { look ->
                                Text(look.title, modifier = Modifier.padding(vertical = 6.dp))
                            }
                            Text("Choose a look after capturing a photo.")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CameraScreen(onCaptured: (Uri) -> Unit) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var granted by remember {
        mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
            android.content.pm.PackageManager.PERMISSION_GRANTED)
    }
    val requestPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        granted = it
    }
    var imageCapture by remember { mutableStateOf<ImageCapture?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }

    if (!granted) {
        Column(Modifier.fillMaxSize().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center) {
            Text("Still needs camera permission to take photos.")
            Spacer(Modifier.height(16.dp))
            Button(onClick = { requestPermission.launch(Manifest.permission.CAMERA) }) {
                Text("Allow camera")
            }
        }
        return
    }

    Column(Modifier.fillMaxSize()) {
        AndroidView(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            factory = { ctx -> PreviewView(ctx).apply { scaleType = PreviewView.ScaleType.FILL_CENTER } },
            update = { view ->
                if (view.tag != lifecycleOwner) {
                    view.tag = lifecycleOwner
                    val future = ProcessCameraProvider.getInstance(context)
                    future.addListener({
                        try {
                            val provider = future.get()
                            val preview = Preview.Builder().build().also {
                                it.surfaceProvider = view.surfaceProvider
                            }
                            val capture = ImageCapture.Builder()
                                .setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY).build()
                            provider.unbindAll()
                            provider.bindToLifecycle(lifecycleOwner, CameraSelector.DEFAULT_BACK_CAMERA, preview, capture)
                            imageCapture = capture
                        } catch (exception: Exception) {
                            error = exception.message ?: "Unable to start camera"
                        }
                    }, ContextCompat.getMainExecutor(context))
                }
            }
        )
        error?.let { Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(12.dp)) }
        Button(
            modifier = Modifier.fillMaxWidth().padding(20.dp),
            enabled = imageCapture != null && !busy,
            onClick = {
                val capture = imageCapture ?: return@Button
                busy = true
                val values = ContentValues().apply {
                    put(MediaStore.Images.Media.DISPLAY_NAME, "Still_${System.currentTimeMillis()}.jpg")
                    put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/Still")
                    }
                }
                val options = ImageCapture.OutputFileOptions.Builder(
                    context.contentResolver, MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values
                ).build()
                capture.takePicture(options, ContextCompat.getMainExecutor(context),
                    object : ImageCapture.OnImageSavedCallback {
                        override fun onImageSaved(result: ImageCapture.OutputFileResults) {
                            busy = false
                            result.savedUri?.let(onCaptured) ?: run { error = "Photo saved without a URI" }
                        }
                        override fun onError(exception: ImageCaptureException) {
                            busy = false
                            error = exception.message ?: "Capture failed"
                        }
                    })
            }
        ) { Text(if (busy) "Saving…" else "Capture") }
    }
}

@Composable
private fun PhotoReview(uri: Uri?) {
    var selected by remember(uri) { mutableStateOf(CinematicLook.ORIGINAL) }
    var busyExport by remember { mutableStateOf(false) }
    var autoRecommend by remember { mutableStateOf(true) }
    var intensity by remember(uri) { mutableFloatStateOf(1f) }
    var showOriginal by remember(uri) { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val prefs = remember(context) { context.getSharedPreferences("still_recipes", android.content.Context.MODE_PRIVATE) }
    var customContrast by remember { mutableFloatStateOf(prefs.getFloat("contrast", 1.1f)) }
    var customWarmth by remember { mutableFloatStateOf(prefs.getFloat("warmth", 0.04f)) }
    var customSaturation by remember { mutableFloatStateOf(prefs.getFloat("saturation", 0.85f)) }
    val customSettings = LookSettings(customContrast, customWarmth, customSaturation)
    var bitmap by remember(uri) { mutableStateOf<android.graphics.Bitmap?>(null) }
    var loadError by remember(uri) { mutableStateOf<String?>(null) }
    var retryCount by remember(uri) { mutableIntStateOf(0) }
    LaunchedEffect(uri, retryCount) {
        bitmap = null
        loadError = null
        if (uri != null) {
            val result = withContext(Dispatchers.IO) {
                runCatching { PhotoDecoder.decode(context, uri, 1600) }
            }
            result.onSuccess { bitmap = it }
                .onFailure { loadError = it.message ?: "Unable to open photo" }
        }
    }
    LaunchedEffect(bitmap) {
        bitmap?.let { if (autoRecommend) selected = withContext(Dispatchers.Default) { LookRecommender.recommend(it) } }
    }
    Column(Modifier.fillMaxSize().padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center) {
        if (bitmap != null) {
            val preview = remember(bitmap, selected, intensity, customContrast, customWarmth, customSaturation) { CinematicProcessor.render(bitmap!!, selected, intensity = intensity, custom = customSettings) }
            Image(bitmap = (if (showOriginal) bitmap!! else preview).asImageBitmap(), contentDescription = if (showOriginal) "Original photo" else "Edited photo",
                modifier = Modifier.fillMaxWidth().weight(1f))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Compare original", modifier = Modifier.weight(1f))
                Switch(checked = showOriginal, onCheckedChange = { showOriginal = it })
            }
            Text(if (showOriginal) "Viewing original (edits preserved)" else "Viewing edited preview")
            Text("Original saved to Pictures/Still")
            Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())) {
                CinematicLook.entries.forEach { look ->
                    FilterChip(
                        selected = selected == look,
                        onClick = { selected = look },
                        label = { Text(look.title) },
                        modifier = Modifier.padding(end = 8.dp)
                    )
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Auto-select look", modifier = Modifier.weight(1f))
                Switch(checked = autoRecommend, onCheckedChange = {
                    autoRecommend = it
                    if (it) selected = LookRecommender.recommend(bitmap!!)
                })
            }
            Text("Suggested: ${LookRecommender.recommend(bitmap!!).title}")
            if (selected == CinematicLook.CUSTOM) {
                Text("My Recipe · saved on this device")
                Text("Contrast: ${"%.2f".format(customContrast)}")
                Slider(value = customContrast, onValueChange = {
                    customContrast = it; prefs.edit().putFloat("contrast", it).apply()
                }, valueRange = 0.6f..1.6f)
                Text("Warmth: ${"%.2f".format(customWarmth)}")
                Slider(value = customWarmth, onValueChange = {
                    customWarmth = it; prefs.edit().putFloat("warmth", it).apply()
                }, valueRange = -0.2f..0.2f)
                Text("Saturation: ${"%.2f".format(customSaturation)}")
                Slider(value = customSaturation, onValueChange = {
                    customSaturation = it; prefs.edit().putFloat("saturation", it).apply()
                }, valueRange = 0f..1.6f)
            }
            Text("Look intensity: ${(intensity * 100).toInt()}%")
            Slider(value = intensity, onValueChange = { intensity = it }, valueRange = 0f..1f)
            Button(
                enabled = !busyExport && selected != CinematicLook.ORIGINAL,
                onClick = {
                    busyExport = true
                    scope.launch {
                        val result = withContext(Dispatchers.IO) {
                            runCatching { PhotoExporter.export(context, uri!!, selected, intensity, customSettings) }
                        }
                        busyExport = false
                        Toast.makeText(context,
                            result.fold({ "Edited photo saved to Pictures/Still" },
                                { "Export failed: ${it.message}" }),
                            Toast.LENGTH_LONG).show()
                    }
                }
            ) { Text(if (busyExport) "Exporting…" else "Save edited copy") }
            Text("Original is always preserved")
        } else {
            when {
                uri == null -> Text("Capture a photo to review it here.")
                loadError != null -> {
                    Text("Couldn't load photo: ${loadError}", color = MaterialTheme.colorScheme.error)
                    Button(onClick = { retryCount++ }) { Text("Retry") }
                    Text("Your original photo remains saved in Pictures/Still.")
                }
                else -> CircularProgressIndicator()
            }
        }
    }
}
