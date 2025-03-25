package com.example.creadordecodigos2

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.util.Size
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview as CameraPreview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Camera
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.core.graphics.createBitmap
import com.example.creadordecodigos2.ui.theme.CreadorDeCodigos2Theme
import com.google.zxing.BarcodeFormat
import com.google.zxing.DecodeHintType
import com.google.zxing.MultiFormatReader
import com.google.zxing.MultiFormatWriter
import com.google.zxing.common.BitMatrix
import com.google.zxing.common.HybridBinarizer
import java.util.concurrent.Executors
import androidx.camera.core.ImageProxy
import com.google.zxing.BinaryBitmap
import com.google.zxing.EncodeHintType
import com.google.zxing.PlanarYUVLuminanceSource
import java.nio.ByteBuffer

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            CreadorDeCodigos2Theme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    BarcodeApp(
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}

enum class BarcodeType(val format: BarcodeFormat, val displayName: String) {
    QR_CODE(BarcodeFormat.QR_CODE, "Código QR"),
    AZTEC(BarcodeFormat.AZTEC, "Aztec"),
    DATA_MATRIX(BarcodeFormat.DATA_MATRIX, "Data Matrix"),
    PDF_417(BarcodeFormat.PDF_417, "PDF 417"),
    CODE_128(BarcodeFormat.CODE_128, "Code 128"),
    CODE_39(BarcodeFormat.CODE_39, "Code 39"),
    EAN_13(BarcodeFormat.EAN_13, "EAN-13")
}

enum class AppMode {
    GENERATE,
    SCAN
}

@Composable
fun BarcodeApp(modifier: Modifier = Modifier) {
    var appMode by remember { mutableStateOf(AppMode.GENERATE) }

    Column(
        modifier = modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Tab selectors
        TabRow(
            selectedTabIndex = appMode.ordinal
        ) {
            Tab(
                selected = appMode == AppMode.GENERATE,
                onClick = { appMode = AppMode.GENERATE },
                icon = { Icon(Icons.Default.QrCode, contentDescription = "Generar") },
                text = { Text("Generar") }
            )
            Tab(
                selected = appMode == AppMode.SCAN,
                onClick = { appMode = AppMode.SCAN },
                icon = { Icon(Icons.Default.Camera, contentDescription = "Escanear") },
                text = { Text("Escanear") }
            )
        }

        when (appMode) {
            AppMode.GENERATE -> QRCodeGenerator()
            AppMode.SCAN -> QRCodeScanner()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QRCodeGenerator(modifier: Modifier = Modifier) {
    var context = LocalContext.current
    var text by remember { mutableStateOf(TextFieldValue("")) }
    var qrBitmap by remember { mutableStateOf<ImageBitmap?>(null) }
    var selectedBarcodeType by remember { mutableStateOf(BarcodeType.QR_CODE) }
    var expanded by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        TextField(
            value = text,
            onValueChange = { text = it },
            label = { Text("Ingresa el texto para el código") },
            modifier = Modifier.fillMaxWidth()
        )

        // Selector de tipo de código
        Box(modifier = Modifier.fillMaxWidth()) {
            OutlinedCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = true }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = selectedBarcodeType.displayName)
                    Icon(imageVector = Icons.Default.ArrowDropDown, contentDescription = "Seleccionar tipo")
                }
            }

            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                modifier = Modifier.fillMaxWidth()
            ) {
                BarcodeType.entries.forEach { type ->
                    DropdownMenuItem(
                        text = { Text(type.displayName) },
                        onClick = {
                            selectedBarcodeType = type
                            expanded = false
                            if (selectedBarcodeType == BarcodeType.EAN_13) {
                                Toast.makeText(context, "Para este formato solo se admiten números y 12 dígitos", Toast.LENGTH_LONG).show()
                            }
                        }
                    )
                }
            }
        }

        Button(
            onClick = {
                if (text.text.isNotEmpty()) {
                    qrBitmap = generateBarcode(text.text, selectedBarcodeType.format)
                }
            },
            modifier = Modifier.align(Alignment.CenterHorizontally)
        ) {
            Text("Generar Código")
        }

        qrBitmap?.let { bitmap ->
            Image(
                bitmap = bitmap,
                contentDescription = "Código generado",
                modifier = Modifier
                    .size(250.dp)
                    .padding(top = 16.dp)
            )
        }

        if (qrBitmap != null) {
            Text(
                text = "Tipo: ${selectedBarcodeType.displayName}",
                modifier = Modifier.padding(top = 8.dp)
            )
        }
    }
}

@Composable
fun QRCodeScanner() {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val cameraProviderFuture = remember { ProcessCameraProvider.getInstance(context) }

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    var scannedCode by remember { mutableStateOf<String?>(null) }
    var isScanning by remember { mutableStateOf(true) }

    val requestPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasCameraPermission = isGranted
        if (isGranted) {
            Toast.makeText(context, "Permiso de cámara concedido", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(context, "Se necesita permiso de cámara para escanear", Toast.LENGTH_LONG).show()
        }
    }

    LaunchedEffect(key1 = true) {
        if (!hasCameraPermission) {
            requestPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        if (hasCameraPermission) {
            if (isScanning) {
                Text(
                    "Apunta la cámara a un código para escanearlo",
                    modifier = Modifier.padding(bottom = 8.dp),
                    textAlign = TextAlign.Center
                )

                // Camera Preview
                AndroidView(
                    factory = { ctx ->
                        val previewView = PreviewView(ctx).apply {
                            implementationMode = PreviewView.ImplementationMode.COMPATIBLE
                            scaleType = PreviewView.ScaleType.FILL_CENTER
                        }

                        val executor = Executors.newSingleThreadExecutor()

                        cameraProviderFuture.addListener({
                            val cameraProvider = cameraProviderFuture.get()

                            val preview = CameraPreview.Builder().build().also {
                                it.setSurfaceProvider(previewView.surfaceProvider)
                            }

                            val imageAnalysis = ImageAnalysis.Builder()
                                .setTargetResolution(Size(640, 480)) // Resolución reducida para mejor rendimiento
                                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                                .build()
                                .also {
                                    it.setAnalyzer(executor, QRCodeAnalyzer { result ->
                                        if (isScanning) { // Evitar múltiples callbacks
                                            scannedCode = result
                                            isScanning = false
                                        }
                                    })
                                }

                            try {
                                cameraProvider.unbindAll()
                                cameraProvider.bindToLifecycle(
                                    lifecycleOwner,
                                    CameraSelector.DEFAULT_BACK_CAMERA,
                                    preview,
                                    imageAnalysis
                                )

                                // Mostrar mensaje de inicio de escaneo
                                Log.d("QRScanner", "Cámara inicializada y lista para escanear")

                            } catch (e: Exception) {
                                Log.e("QRScanner", "Error al inicializar la cámara: ${e.message}")
                                e.printStackTrace()
                            }
                        }, ContextCompat.getMainExecutor(ctx))

                        previewView
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(3f / 4f)
                )

                // Indicador de escaneo activo
                LinearProgressIndicator(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp)
                )

                Text(
                    "Escaneando...",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(top = 4.dp)
                )
            } else {
                // Mostrar resultado del escaneo
                Text(
                    "Código escaneado:",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(top = 32.dp)
                )

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Text(
                        text = scannedCode ?: "Error al escanear",
                        modifier = Modifier.padding(16.dp),
                        textAlign = TextAlign.Center
                    )
                }

                Button(
                    onClick = {
                        isScanning = true
                        scannedCode = null
                    },
                    modifier = Modifier.padding(top = 24.dp)
                ) {
                    Text("Escanear otro código")
                }
            }
        } else {
            Text(
                "Se necesita permiso de cámara para escanear códigos",
                modifier = Modifier.padding(32.dp),
                textAlign = TextAlign.Center
            )

            Button(
                onClick = {
                    requestPermissionLauncher.launch(Manifest.permission.CAMERA)
                }
            ) {
                Text("Solicitar permiso")
            }
        }
    }
}

class QRCodeAnalyzer(private val onQrCodeScanned: (String) -> Unit) : ImageAnalysis.Analyzer {
    private val supportedFormats = listOf(
        BarcodeFormat.QR_CODE,
        BarcodeFormat.AZTEC,
        BarcodeFormat.DATA_MATRIX,
        BarcodeFormat.PDF_417,
        BarcodeFormat.CODE_128,
        BarcodeFormat.CODE_39,
        BarcodeFormat.EAN_13
    )

    private val reader = MultiFormatReader().apply {
        val hints = mapOf(
            DecodeHintType.POSSIBLE_FORMATS to supportedFormats,
            DecodeHintType.TRY_HARDER to true
        )
        setHints(hints)
    }

    override fun analyze(image: ImageProxy) {
        val rotationDegrees = image.imageInfo.rotationDegrees

        // Obtener la imagen de los planos
        val buffer = image.planes[0].buffer
        val data = ByteArray(buffer.remaining())
        buffer.get(data)

        val width = image.width
        val height = image.height

        Log.d("QRScanner", "Analizando imagen: ${width}x${height}, rotación: $rotationDegrees")

        // Intentar diferentes configuraciones de PlanarYUVLuminanceSource
        val sources = listOf(
            // Fuente estándar
            PlanarYUVLuminanceSource(
                data, width, height, 0, 0, width, height, false
            ),
            // Área central (donde probablemente esté el código)
            PlanarYUVLuminanceSource(
                data, width, height,
                width / 4, height / 4, width / 2, height / 2,
                false
            )
        )

        for (source in sources) {
            try {
                val binaryBitmap = BinaryBitmap(HybridBinarizer(source))
                val result = reader.decode(binaryBitmap)

                Log.d("QRScanner", "Código detectado: ${result.text}")
                onQrCodeScanned(result.text)
                break  // Detener después de encontrar un código

            } catch (e: Exception) {
                // Continuar con la siguiente fuente si falla
                continue
            }
        }

        // Siempre cerrar la imagen para liberar recursos
        image.close()
    }
}

fun generateBarcode(content: String, format: BarcodeFormat): ImageBitmap? {
    return try {
        // Configuración según el formato
        val (width, height, hints) = when (format) {
            BarcodeFormat.QR_CODE -> Triple(500, 500, mapOf(EncodeHintType.ERROR_CORRECTION to "H"))
            BarcodeFormat.AZTEC -> Triple(500, 500, mapOf(EncodeHintType.ERROR_CORRECTION to 23))
            BarcodeFormat.DATA_MATRIX -> Triple(500, 500, emptyMap())
            BarcodeFormat.PDF_417 -> Triple(1000, 300, mapOf(EncodeHintType.ERROR_CORRECTION to 2))
            BarcodeFormat.CODE_128 -> Triple(600, 200, emptyMap())
            BarcodeFormat.CODE_39 -> Triple(600, 200, emptyMap())
            BarcodeFormat.EAN_13 -> Triple(500, 250, mapOf(EncodeHintType.MARGIN to 10))
            else -> Triple(500, 200, emptyMap())
        }

        val bitMatrix: BitMatrix = MultiFormatWriter().encode(content, format, width, height, hints)

        val bitmap = createBitmap(width, height).apply {
            for (x in 0 until width) {
                for (y in 0 until height) {
                    setPixel(x, y, if (bitMatrix[x, y]) android.graphics.Color.BLACK else android.graphics.Color.WHITE)
                }
            }
        }
        bitmap.asImageBitmap()
    } catch (e: Exception) {
        Log.e("BarcodeGenerator", "Error al generar código: ${e.message}")
        null
    }
}


@Preview(showBackground = true)
@Composable
fun QRCodeGeneratorPreview() {
    CreadorDeCodigos2Theme {
        BarcodeApp()
    }
}