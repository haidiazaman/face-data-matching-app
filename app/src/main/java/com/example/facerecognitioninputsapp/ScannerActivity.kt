package com.example.facerecognitioninputsapp

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Rect
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import com.example.facerecognitioninputsapp.databinding.ActivityScannerBinding
import com.google.common.util.concurrent.ListenableFuture
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.Face
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetector
import com.google.mlkit.vision.face.FaceDetectorOptions
import org.tensorflow.lite.DataType
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.support.common.FileUtil
import org.tensorflow.lite.support.common.ops.CastOp
import org.tensorflow.lite.support.image.ImageProcessor
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.support.image.ops.ResizeOp
import java.nio.ByteBuffer
import java.util.concurrent.Executors
import kotlin.math.max
import kotlin.math.min

class ScannerActivity : AppCompatActivity() {

    private lateinit var binding: ActivityScannerBinding
    private lateinit var cameraSelector: CameraSelector
    private lateinit var cameraProviderFuture: ListenableFuture<ProcessCameraProvider>
    private lateinit var processCameraProvider: ProcessCameraProvider
    private lateinit var cameraPreview: Preview
    private lateinit var imageAnalysis: ImageAnalysis
    private lateinit var context: Context

    private var isFaceDetected = false
    private var isFaceProcessing = false
    private var isFaceRecognitionEnabled = false
    private lateinit var interpreter: Interpreter
    private val interpreterOptions = Interpreter.Options().apply { numThreads = 4 }
    private val inputSize = 160
    private val embeddingDim = 128
    private var faceRecognitionOutput = FloatArray(embeddingDim)
//    private lateinit var faceRecognitionOutputString: String


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityScannerBinding.inflate(layoutInflater)
        setContentView(binding.root)
        cameraSelector =
            CameraSelector.Builder().requireLensFacing(CameraSelector.LENS_FACING_FRONT).build()
        cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener(
            {
                processCameraProvider = cameraProviderFuture.get()
                bindCameraPreview()
                bindInputAnalyser()
            }, ContextCompat.getMainExecutor(this)
        )
        Log.i("startOutput",faceRecognitionOutput.contentToString())
//        val intent = Intent(this, UserInputActivity::class.java)

        // Button click listener
        binding.captureFaceButton.setOnClickListener {
            if (isFaceDetected) {
                isFaceRecognitionEnabled = true
//                Log.i("scanneractivityoutput", faceRecognitionOutput.contentToString())
//                val intent = Intent(this, UserInputActivity::class.java).also {
//                    it.putExtra("faceRecognitionResult", faceRecognitionOutput)
//                    startActivity(it)
//                }
//                intent.putExtra("faceRecognitionResult", faceRecognitionOutputString)
//
//                Log.i("scannerfaceRecognitionOutput",faceRecognitionOutput.contentToString())
//                val intent = Intent(this, UserInputActivity::class.java).also {
//                    it.putExtra("faceRecognitionResult", faceRecognitionOutput)
//                    startActivity(it)
//                }
//                // Start the new activity
//                startActivity(intent)
//                finish()
            } else {
                isFaceRecognitionEnabled = false
                showNoFaceDetectedPrompt() // Show a prompt indicating no face detected
            }
        }

        // initialise face recognition interpreter
        this.also { context = it }
        try {
            interpreter =
                Interpreter(FileUtil.loadMappedFile(context, "facenet.tflite"), interpreterOptions)
//            val outputTensorShape = interpreter.getOutputTensor(0).shape()
//            val checkEmbeddingDim = outputTensorShape[outputTensorShape.size - 1]
//            Log.i("interpreter", "embedding dim: $checkEmbeddingDim")
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // Function to show a prompt for no face detected
    private fun showNoFaceDetectedPrompt() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("No Face Detected")
        builder.setMessage("To input a person's medical data, please capture the person's face.")
        builder.setPositiveButton("OK") { _, _ ->
            // Handle OK button click if needed
        }
        val dialog: AlertDialog = builder.create()
        dialog.show()
    }

    private fun bindCameraPreview() {
        cameraPreview = Preview.Builder()
            .setTargetRotation(binding.previewView.display.rotation)
            .build()
        cameraPreview.setSurfaceProvider(binding.previewView.surfaceProvider)
        processCameraProvider.bindToLifecycle(this, cameraSelector, cameraPreview)
    }

    private fun bindInputAnalyser() {
        val detector = FaceDetection.getClient(
            FaceDetectorOptions.Builder()
                .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
                .setContourMode(FaceDetectorOptions.LANDMARK_MODE_NONE) // skips landmark mapping
                .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_NONE) // skips facial expressions and other classification such as wink
                .build()
        )
        imageAnalysis = ImageAnalysis.Builder()
            .setTargetRotation(binding.previewView.display.rotation)
            .build()

        val cameraExecutor = Executors.newSingleThreadExecutor()

        imageAnalysis.setAnalyzer(cameraExecutor) { imageProxy ->
//            mainImageProxy = imageProxy // set as global var if you want to call it in the onCreate function or anywhere outside this fn
            if (isFaceRecognitionEnabled) {
                // if button clicked and face is detected, performFaceRecognition is called and then output saved and go to next activity
                performFaceRecognition(detector, imageProxy)
                // need to close camera and imageanalyser here - maybe dont need
//                startActivity(intent)
//                finish()
            } else {
                processImageProxy(detector, imageProxy)
            }
        }
        processCameraProvider.bindToLifecycle(this, cameraSelector, imageAnalysis)
    }

    @SuppressLint("UnsafeOptInUsageError")
    private fun performFaceRecognition(detector: FaceDetector, imageProxy: ImageProxy) {
        // Process the current frame
        val inputImage =
            InputImage.fromMediaImage(imageProxy.image!!, imageProxy.imageInfo.rotationDegrees)
        detector.process(inputImage).addOnSuccessListener { faces ->
            if (faces.isNotEmpty()) {
                isFaceDetected = true
                binding.faceBoxOverlay.clear() // clear current bbox

                val face = faces[0] // Get the first detected face
                val croppedFaceBitmap = cropFaceFromImage(
                    imageProxy,
                    face
                ) // Crop and resize the face from the original image
                val inputBuffer =
                    convertBitmapToBuffer(croppedFaceBitmap) // Convert the cropped face Bitmap to ByteBuffer
                faceRecognitionOutput = runFaceNet(inputBuffer) // Run inference
//                Log.i(
//                    "scannerfaceoutput",
//                    "scannerfaceoutput: $faceRecognitionOutput"
//                )
//                val intent = Intent(this, UserInputActivity::class.java).also {
//                    it.putExtra("faceRecognitionResult", faceRecognitionOutput)
//                    startActivity(it)
//                }
//                faceRecognitionOutputString = faceRecognitionOutput.contentToString()
//                Log.i("faceRecognitionOutputString",faceRecognitionOutputString)
                // Set extra data before starting the activity
                Log.i("scannerinitialfaceRecognitionOutput",faceRecognitionOutput.contentToString())


//                Log.i("scannerfaceRecognitionOutput",faceRecognitionOutput.contentToString())
                val intent = Intent(this, UserInputActivity::class.java).also {
                    it.putExtra("faceRecognitionResult", faceRecognitionOutput)
                    startActivity(it)
                }


                imageProxy.close() // Continue processing the video stream
                isFaceRecognitionEnabled = false
                // Start the new activity
                startActivity(intent)
                finish()

            } else {
                isFaceDetected = false
                imageProxy.close() // Continue processing the video stream if no face is detected
            }
        }.addOnFailureListener {
            it.printStackTrace()
            imageProxy.close() // Continue processing the video stream in case of failure
        }
    }

    private fun cropFaceFromImage(imageProxy: ImageProxy, face: Face): Bitmap {
        val cropRect = Rect(
            max(0, face.boundingBox.left),
            max(0, face.boundingBox.top),
            min(imageProxy.width, face.boundingBox.right),
            min(imageProxy.height, face.boundingBox.bottom)
        )
//        val bitmap = imageProxy.toBitmap()

        // perform crop on bitmap
        val originalBitmap = imageProxy.toBitmap()
        Log.i("bitmaps", "original height: ${originalBitmap.height}")
        Log.i("bitmaps", "original width: ${originalBitmap.width}")

        // Perform crop on bitmap
        val croppedBitmap = Bitmap.createBitmap(
            originalBitmap,
            cropRect.left,
            cropRect.top,
            cropRect.width(),
            cropRect.height()
        )
        Log.i("bitmaps", "cropped height: ${croppedBitmap.height}")
        Log.i("bitmaps", "cropped width: ${croppedBitmap.width}")

        val resizedBitmap = resizeBitmap(
            croppedBitmap,
            inputSize,
            inputSize
        ) // Return output of Resize cropped bitmap which is the final bitmap
        Log.i("bitmaps", "resized height: ${resizedBitmap.height}")
        Log.i("bitmaps", "resized width: ${resizedBitmap.width}")

//        return Bitmap.createBitmap(160,160,Bitmap.Config.ARGB_8888) // return dummy variable
        return resizedBitmap
    }

    private fun resizeBitmap(bitmap: Bitmap, newWidth: Int, newHeight: Int): Bitmap {
        return Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
    }

    private val imageTensorProcessor = ImageProcessor.Builder()
        .add(ResizeOp(inputSize, inputSize, ResizeOp.ResizeMethod.BILINEAR))
        .add(CastOp(DataType.FLOAT32))
        .build()

    private fun convertBitmapToBuffer(image: Bitmap): ByteBuffer {
        return imageTensorProcessor.process(TensorImage.fromBitmap(image)).buffer
    }

    private fun runFaceNet(inputs: Any): FloatArray {
        val output = Array(1) { FloatArray(embeddingDim) }
        interpreter.run(inputs, output) // replaces the values in outputArray inplace
        Log.i("recogOutput", output[0].contentToString())
        return output[0]
    }

    // function run for normal video stream
    @SuppressLint("UnsafeOptInUsageError")
    private fun processImageProxy(detector: FaceDetector, imageProxy: ImageProxy) {
        if (isFaceProcessing) {
            imageProxy.close()
            return
        }
        isFaceProcessing = true
        val inputImage =
            InputImage.fromMediaImage(imageProxy.image!!, imageProxy.imageInfo.rotationDegrees)
        detector.process(inputImage).addOnSuccessListener { faces ->
            if (faces.isNotEmpty()) {
                isFaceDetected = true
                binding.faceBoxOverlay.clear()
                faces.forEach { face ->
                    val faceBox = FaceBox(binding.faceBoxOverlay, face, imageProxy.image!!.cropRect)
                    binding.faceBoxOverlay.add(faceBox)
                }
            } else {
                isFaceDetected = false
            }
            isFaceProcessing = false
        }.addOnFailureListener {
            it.printStackTrace()
        }.addOnCompleteListener {
            imageProxy.close()
        }
    }

    companion object {
        fun startScanner(context: Context, onScan: () -> Unit) {
            Intent(context, ScannerActivity::class.java).also {
                context.startActivity(it)
            }
        }
    }
}