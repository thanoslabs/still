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
                        Text("Cinematic looks coming next")
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
    val context = LocalContext.current
    val bitmap = remember(uri) {
        uri?.let {
            try {
                context.contentResolver.openInputStream(it)?.use { stream ->
                    BitmapFactory.decodeStream(stream)?.asImageBitmap()
                }
            } catch (_: Exception) { null }
        }
    }
    Column(Modifier.fillMaxSize().padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center) {
        if (bitmap != null) {
            Image(bitmap = bitmap, contentDescription = "Most recently captured photo",
                modifier = Modifier.fillMaxWidth().weight(1f))
            Text("Original saved to Pictures/Still")
        } else {
            Text("Capture a photo to review it here.")
        }
    }
}
