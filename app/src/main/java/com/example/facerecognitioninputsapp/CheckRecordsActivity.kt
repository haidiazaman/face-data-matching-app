package com.example.facerecognitioninputsapp

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
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
import com.example.facerecognitioninputsapp.databinding.ActivityCheckRecordsBinding
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
import kotlin.math.pow
import kotlin.math.sqrt


class CheckRecordsActivity : AppCompatActivity() {

    // testing github repo push pull see whether it works

    private lateinit var binding: ActivityCheckRecordsBinding
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

    //    private val faceRecognitionResult = intent.getFloatArrayExtra("faceRecognitionResult")
    private lateinit var sharedPreferences: SharedPreferences


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCheckRecordsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // initialise sharedPreferences to get the json data
        sharedPreferences =
            getSharedPreferences("face_recognition_app_user_data", Context.MODE_PRIVATE)

//        getAllKeys()
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
        // Button click listener
        binding.captureFaceButton.setOnClickListener {
            if (isFaceDetected) {
                isFaceRecognitionEnabled = true
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

    // IMPLEMENT FUNCTIONS TO CHECK IF CURRENT FACE RECOGNITION OUTPUT HAS A MATCH WITH EXISTING KEYS IN THE JSON
    // function to get all keys in the current json
//    private fun getAllKeys () {
//
//    }
    // function to do similarity check - l2Norm / can also try Cosine similarity
    private fun l2Norm(x1: FloatArray, x2: FloatArray): Float {
//        var sum = 0.0f
//        val mag1 = sqrt(x1.map { xi -> xi.pow(2) }.sum())
//        val mag2 = sqrt(x2.map { xi -> xi.pow(2) }.sum())
//        Log.i("l2Norm mag1", "l2Norm mag1: $mag1")
//        Log.i("l2Norm mag2", "l2Norm mag2: $mag2")
//        for (i in x1.indices) {
//            sum += ((x1[i] / mag1) - (x2[i] / mag2)).pow(2)
//        }
//        Log.i("l2Norm final score", "l2Norm final score: ${sqrt(sum)}")
//        Log.i("l2Norm ----", "")
//        return sqrt(sum)
        var dotProduct = 0.0f
        var normA = 0.0f
        var normB = 0.0f
        for (i in x1.indices) {
            dotProduct += x1[i] * x2[i]
            normA += x1[i].pow(2)
            normB += x2[i].pow(2)
        }
        normA = sqrt(normA)
        normB = sqrt(normB)
        val similarityScore = dotProduct / (normA * normB)
        Log.i("dot product", "--- NEW ENTRY --")
        Log.i("dot product", "dotProduct: $dotProduct")
        Log.i("dot product", "normA: $normA")
        Log.i("dot product", "normB: $normB")
        Log.i("dot product", "similarityScore: $similarityScore")
        Log.i("dot product", "--- END OF ENTRY --")
        Log.i("dot product", "")

        return similarityScore
    }
//    // function to loop thru all keys in current json, store similarity scores, and return the highest one if at least above threshold
//    private fun getMatchingKey () {
//        // create similarityScores = []
//        // for key in getAllKeys(), convert key back to FloatArray, calc l2Norm and append to similarityScores
//        // get the maxScore in similarityScores, check if its above threshold
//        // if above, return key as String again, else call function to say no Match found
//    }

        // Function to get all keys in the current json
    private fun getAllKeys() {
        val allKeyValuePairs: Map<String, *> = sharedPreferences.all
        val allKeys: Set<String> = allKeyValuePairs.keys

        for (key in allKeys) {
            Log.i("SharedPreferencesKey", key)
            // Convert each key back to a FloatArray if needed
//            val floatArrayKey = key.split(",").map { it.toFloat() }.toFloatArray()
            // Do  somethingwith the floatArrayKey
        }
    }
    // Function to loop through all keys, store similarity scores, and return the highest one if above threshold
//    private fun getMatchingKey(currentFaceRecognitionOutput: FloatArray): String? {
//        val allKeyValuePairs: Map<String, *> = sharedPreferences.all
//        val allKeys: Set<String> = allKeyValuePairs.keys
//
//        Log.i("firstPass", "first")
//        var maxSimilarityScore = Float.MIN_VALUE
//        var matchingKey: String? = null
//
//        for (key in allKeys) {
//            Log.i("secondPass", key)
//            val floatArrayKey = key.split(",").map { it.toFloat() }.toFloatArray()
//            val similarityScore = l2Norm(currentFaceRecognitionOutput, floatArrayKey)
//            Log.i("thirdPass", key)
//
//            if (similarityScore > maxSimilarityScore) {
//                maxSimilarityScore = similarityScore
//                matchingKey = key
//            }
//        }
//        Log.i("fourthPass", "key")
//        // Adjust threshold as needed
//        val threshold = 0.5
//        return if (maxSimilarityScore > threshold) matchingKey else null
//    }

    // Function to loop through all keys, store similarity scores, and return the highest one if above threshold
    private fun getMatchingKey(currentFaceRecognitionOutput: FloatArray): String? {
        val allKeyValuePairs: Map<String, *> = sharedPreferences.all
        val allKeys: Set<String> = allKeyValuePairs.keys

        Log.i("inputVector", "$currentFaceRecognitionOutput")
        Log.i("inputVector", currentFaceRecognitionOutput.contentToString())
        var maxSimilarityScore = Float.MIN_VALUE
        var matchingKey: String? = null

        for (key in allKeys) {
            try {
                Log.i("currentKey", "$key")
//                val key2 = key.replace("[","").replace("]","")
//                val floatArrayKey = key.split(",").map { it.toFloat() }.toFloatArray()
                val floatArrayKey = key
                    .trimStart('[') // Remove leading '['
                    .trimEnd(']')   // Remove trailing ']'
                    .split(", ")    // Split the string by ", "
                    .map { it.toFloat() }
                    .toFloatArray()
//                val floatArrayKey = key.split(",")
//                val floatArrayKey = key.split('/').map{it.toFloat()}
                Log.i("floatArrayKey", "$floatArrayKey")

                val similarityScore = l2Norm(currentFaceRecognitionOutput, floatArrayKey)

                Log.i("similarityScore", "$similarityScore")

                if (similarityScore > maxSimilarityScore) {
                    maxSimilarityScore = similarityScore
                    matchingKey = key
                }
            } catch (e: NumberFormatException) {
                // Handle the case where key cannot be converted to FloatArray
                Log.e("NumberFormatException", "Error converting key to FloatArray: $key")
            }
        }
        Log.i("getMatchingKey current face:", currentFaceRecognitionOutput.contentToString())
        if (matchingKey != null) {
            Log.i("getMatchingKey matched face key:", matchingKey)
        }
        Log.i("getMatchingKey sim score", "$maxSimilarityScore")

        // Adjust threshold as needed
        val threshold = 0.9
        return if (maxSimilarityScore > threshold) matchingKey else null
    }

    // Function to show a prompt for no face detected
    private fun showNoFaceMatchedPrompt() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("No Face match in database")
        builder.setMessage("Try again / Please ensure this person has been registered before / Register again")
        builder.setPositiveButton("OK") { _, _ ->
            // Handle OK button click if needed
        }
        val dialog: AlertDialog = builder.create()
        dialog.show()
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
                val croppedFaceBitmap = cropFaceFromImage(imageProxy, face) // Crop and resize the face from the original image
                val inputBuffer = convertBitmapToBuffer(croppedFaceBitmap) // Convert the cropped face Bitmap to ByteBuffer
                faceRecognitionOutput = runFaceNet(inputBuffer) // Run inference
                Log.i("checkRecords - faceRecogOutput", "faceRecogOutput: ${faceRecognitionOutput.contentToString()}")

                val matchedKey: String? = getMatchingKey(faceRecognitionOutput)
                Log.i("check2", "$matchedKey")

                if (matchedKey != null) {
                    val intent = Intent(this, RetrieveDataActivity::class.java).also {
                        it.putExtra("matchedKey", matchedKey)
                        startActivity(it)
//                        finish()
                    }
                    Log.i("last part", "reached!")
                    imageProxy.close() // Continue processing the video stream
                    isFaceRecognitionEnabled = false
                    // Start the new activity
                    startActivity(intent)
                    finish()
                    Log.i("last part", "reached again!")

                } else {
                    isFaceRecognitionEnabled = false
                    showNoFaceMatchedPrompt()
                }

//                imageProxy.close() // Continue processing the video stream
//                isFaceRecognitionEnabled = false
//                // Start the new activity
//                startActivity(intent)
//                finish()
                imageProxy.close() // Continue processing the video stream

                isFaceRecognitionEnabled =
                    false // Set isFaceRecognitionEnabled to false after face recognition is performed
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
            Intent(context, CheckRecordsActivity::class.java).also {
                context.startActivity(it)
            }
        }
    }
}



//package com.example.facerecognitioninputsapp
//
//import android.annotation.SuppressLint
//import android.content.Context
//import android.content.Intent
//import android.content.SharedPreferences
//import android.graphics.Bitmap
//import android.graphics.Rect
//import android.os.Bundle
//import android.util.Log
//import androidx.appcompat.app.AlertDialog
//import androidx.appcompat.app.AppCompatActivity
//import androidx.camera.core.CameraSelector
//import androidx.camera.core.ImageAnalysis
//import androidx.camera.core.ImageProxy
//import androidx.camera.core.Preview
//import androidx.camera.lifecycle.ProcessCameraProvider
//import androidx.core.content.ContextCompat
//import com.example.facerecognitioninputsapp.databinding.ActivityCheckRecordsBinding
//import com.google.common.util.concurrent.ListenableFuture
//import com.google.mlkit.vision.common.InputImage
//import com.google.mlkit.vision.face.Face
//import com.google.mlkit.vision.face.FaceDetection
//import com.google.mlkit.vision.face.FaceDetector
//import com.google.mlkit.vision.face.FaceDetectorOptions
//import org.tensorflow.lite.DataType
//import org.tensorflow.lite.Interpreter
//import org.tensorflow.lite.support.common.FileUtil
//import org.tensorflow.lite.support.common.ops.CastOp
//import org.tensorflow.lite.support.image.ImageProcessor
//import org.tensorflow.lite.support.image.TensorImage
//import org.tensorflow.lite.support.image.ops.ResizeOp
//import java.nio.ByteBuffer
//import java.util.concurrent.Executors
//import kotlin.math.max
//import kotlin.math.min
//import kotlin.math.pow
//import kotlin.math.sqrt
//
//class CheckRecordsActivity : AppCompatActivity() {
//
//    private lateinit var binding: ActivityCheckRecordsBinding
//    private lateinit var cameraSelector: CameraSelector
//    private lateinit var cameraProviderFuture: ListenableFuture<ProcessCameraProvider>
//    private lateinit var processCameraProvider: ProcessCameraProvider
//    private lateinit var cameraPreview: Preview
//    private lateinit var imageAnalysis: ImageAnalysis
//    private lateinit var context: Context
//
//    private var isFaceDetected = false
//    private var isFaceProcessing = false
//    private var isFaceRecognitionEnabled = false
//    private lateinit var interpreter: Interpreter
//    private val interpreterOptions = Interpreter.Options().apply { numThreads = 4 }
//    private val inputSize = 160
//    private val embeddingDim = 128
//    private var faceRecognitionOutput = FloatArray(embeddingDim)
//    private lateinit var sharedPreferences: SharedPreferences
//
//
//    override fun onCreate(savedInstanceState: Bundle?) {
//        super.onCreate(savedInstanceState)
//        binding = ActivityCheckRecordsBinding.inflate(layoutInflater)
//        setContentView(binding.root)
//        cameraSelector =
//            CameraSelector.Builder().requireLensFacing(CameraSelector.LENS_FACING_FRONT).build()
//        cameraProviderFuture = ProcessCameraProvider.getInstance(this)
//        cameraProviderFuture.addListener(
//            {
//                processCameraProvider = cameraProviderFuture.get()
//                bindCameraPreview()
//                bindInputAnalyser()
//            }, ContextCompat.getMainExecutor(this)
//        )
//
//
//        // initialise sharedPreferences to get the json data
//        sharedPreferences =
//            getSharedPreferences("face_recognition_app_user_data", Context.MODE_PRIVATE)
//
//////        getAllKeys()
////        // Button click listener
////        binding.captureFaceButton.setOnClickListener {
////            if (isFaceDetected) {
////                isFaceRecognitionEnabled = true
////////                Log.i("checkfaceRecogOutput", "faceRecogOutcheckfaceRecogOutputput: ${faceRecognitionOutput.contentToString()}")
//////                val intent = Intent(this, RetrieveDataActivity::class.java).also {
//////                    it.putExtra("faceRecognitionResult", faceRecognitionOutput)
//////                    startActivity(it)
//////                }
//////                finish()
////            } else {
////                isFaceRecognitionEnabled = false
////                showNoFaceDetectedPrompt() // Show a prompt indicating no face detected
////            }
////        }
//        // Button click listener
//        binding.captureFaceButton.setOnClickListener {
//            if (isFaceDetected) {
//                isFaceRecognitionEnabled = true
//                Log.i("firstFaceRecog", faceRecognitionOutput.toString())
//                Log.i("firstFaceRecog", "$faceRecognitionOutput.contentToString()")
//
//                val matchedKey: String? = getMatchingKey(faceRecognitionOutput)
//                Log.i("check2", "$matchedKey")
//
//                if (matchedKey != null) {
//                    val intent = Intent(this, RetrieveDataActivity::class.java).also {
//                        it.putExtra("matchedKey", matchedKey)
//                        startActivity(it)
////                        finish()
//                    }
//                } else {
//                    isFaceRecognitionEnabled = false
//                    showNoFaceMatchedPrompt()
//                }
//            } else {
//                isFaceRecognitionEnabled = false
//                showNoFaceDetectedPrompt() // Show a prompt indicating no face detected
//            }
//        }
//
//        // initialise face recognition interpreter
//        this.also { context = it }
//        try {
//            interpreter =
//                Interpreter(FileUtil.loadMappedFile(context, "facenet.tflite"), interpreterOptions)
////            val outputTensorShape = interpreter.getOutputTensor(0).shape()
////            val checkEmbeddingDim = outputTensorShape[outputTensorShape.size - 1]
////            Log.i("interpreter", "embedding dim: $checkEmbeddingDim")
//        } catch (e: Exception) {
//            e.printStackTrace()
//        }
//    }
//
//    // Function to show a prompt for no face detected
//    private fun showNoFaceMatchedPrompt() {
//        val builder = AlertDialog.Builder(this)
//        builder.setTitle("No Face match in database")
//        builder.setMessage("Try again / Please ensure this person has been registered before / Register again")
//        builder.setPositiveButton("OK") { _, _ ->
//            // Handle OK button click if needed
//        }
//        val dialog: AlertDialog = builder.create()
//        dialog.show()
//    }
//
//    // Function to loop through all keys, store similarity scores, and return the highest one if above threshold
//    private fun getMatchingKey(currentFaceRecognitionOutput: FloatArray): String? {
//        val allKeyValuePairs: Map<String, *> = sharedPreferences.all
//        val allKeys: Set<String> = allKeyValuePairs.keys
//
//        Log.i("firstPass", "first")
//        var maxSimilarityScore = Float.MIN_VALUE
//        var matchingKey: String? = null
//
//        for (key in allKeys) {
//            try {
//                val floatArrayKey = key.split(",").map { it.toFloat() }.toFloatArray()
//                val similarityScore = l2Norm(currentFaceRecognitionOutput, floatArrayKey)
//                Log.i("currentKeyArray", "1 $floatArrayKey")
//                Log.i("currentKeyArray", "2 $currentFaceRecognitionOutput")
//
//                if (similarityScore > maxSimilarityScore) {
//                    maxSimilarityScore = similarityScore
//                    matchingKey = key
//                }
//            } catch (e: NumberFormatException) {
//                // Handle the case where key cannot be converted to FloatArray
//                Log.e("NumberFormatException", "Error converting key to FloatArray: $key")
//            }
//        }
//        Log.i("fourthPass", "key")
//        Log.i("matchingKey", "$matchingKey")
//        Log.i("maxSimilarityScore", "$maxSimilarityScore")
//
//        // Adjust threshold as needed
//        val threshold = 0.5
//        return if (maxSimilarityScore < threshold) matchingKey else null
//    }
//
////    // Function to get all keys in the current json
////    private fun getAllKeys() {
////        val allKeyValuePairs: Map<String, *> = sharedPreferences.all
////        val allKeys: Set<String> = allKeyValuePairs.keys
////
////        for (key in allKeys) {
////            Log.i("SharedPreferencesKeys", key)
////            // Convert each key back to a FloatArray if needed
//////            val floatArrayKey = key.split(",").map { it.toFloat() }.toFloatArray()
////            // Do  somethingwith the floatArrayKey
////        }
////    }
//
//    // function to do similarity check - l2Norm / can also try Cosine similarity
//    private fun l2Norm(x1: FloatArray, x2: FloatArray): Float {
//        var sum = 0.0f
//        val mag1 = sqrt(x1.map { xi -> xi.pow(2) }.sum())
//        val mag2 = sqrt(x2.map { xi -> xi.pow(2) }.sum())
//        for (i in x1.indices) {
//            sum += ((x1[i] / mag1) - (x2[i] / mag2)).pow(2)
//        }
//        return sqrt(sum)
//    }
//
//    // Function to show a prompt for no face detected
//    private fun showNoFaceDetectedPrompt() {
//        val builder = AlertDialog.Builder(this)
//        builder.setTitle("No Face Detected")
//        builder.setMessage("To input a person's medical data, please capture the person's face.")
//        builder.setPositiveButton("OK") { _, _ ->
//            // Handle OK button click if needed
//        }
//        val dialog: AlertDialog = builder.create()
//        dialog.show()
//    }
//
//    private fun bindCameraPreview() {
//        cameraPreview = Preview.Builder()
//            .setTargetRotation(binding.previewView.display.rotation)
//            .build()
//        cameraPreview.setSurfaceProvider(binding.previewView.surfaceProvider)
//        processCameraProvider.bindToLifecycle(this, cameraSelector, cameraPreview)
//    }
//
//    private fun bindInputAnalyser() {
//        val detector = FaceDetection.getClient(
//            FaceDetectorOptions.Builder()
//                .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
//                .setContourMode(FaceDetectorOptions.LANDMARK_MODE_NONE) // skips landmark mapping
//                .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_NONE) // skips facial expressions and other classification such as wink
//                .build()
//        )
//        imageAnalysis = ImageAnalysis.Builder()
//            .setTargetRotation(binding.previewView.display.rotation)
//            .build()
//
//        val cameraExecutor = Executors.newSingleThreadExecutor()
//
//        imageAnalysis.setAnalyzer(cameraExecutor) { imageProxy ->
////            mainImageProxy = imageProxy // set as global var if you want to call it in the onCreate function or anywhere outside this fn
//            if (isFaceRecognitionEnabled) {
//                // if button clicked and face is detected, performFaceRecognition is called and then output saved and go to next activity
//                performFaceRecognition(detector, imageProxy)
//                // need to close camera and imageanalyser here - maybe dont need
////                startActivity(intent)
////                finish()
//            } else {
//                processImageProxy(detector, imageProxy)
//            }
//        }
//        processCameraProvider.bindToLifecycle(this, cameraSelector, imageAnalysis)
//    }
//
//    @SuppressLint("UnsafeOptInUsageError")
//    private fun performFaceRecognition(detector: FaceDetector, imageProxy: ImageProxy) {
//        // Process the current frame
//        val inputImage =
//            InputImage.fromMediaImage(imageProxy.image!!, imageProxy.imageInfo.rotationDegrees)
//        detector.process(inputImage).addOnSuccessListener { faces ->
//            if (faces.isNotEmpty()) {
//                isFaceDetected = true
//                binding.faceBoxOverlay.clear() // clear current bbox
//
//                val face = faces[0] // Get the first detected face
//                val croppedFaceBitmap = cropFaceFromImage(
//                    imageProxy,
//                    face
//                ) // Crop and resize the face from the original image
//                val inputBuffer =
//                    convertBitmapToBuffer(croppedFaceBitmap) // Convert the cropped face Bitmap to ByteBuffer
//                faceRecognitionOutput = runFaceNet(inputBuffer) // Run inference
//                Log.i(
//                    "checkrecordsfaceoutput",
//                    "checkrecordsfaceoutput: ${faceRecognitionOutput.contentToString()}"
//                )
//
//
////                Log.i("scannerfaceRecognitionOutput",faceRecognitionOutput.contentToString())
//                val intent = Intent(this, RetrieveDataActivity::class.java).also {
//                    it.putExtra("faceRecognitionResult", faceRecognitionOutput)
//                    startActivity(it)
//                }
//
//
//                imageProxy.close() // Continue processing the video stream
//                isFaceRecognitionEnabled = false
//                // Start the new activity
//                startActivity(intent)
//                finish()
//
//                isFaceRecognitionEnabled =
//                    false // Set isFaceRecognitionEnabled to false after face recognition is performed
//            } else {
//                isFaceDetected = false
//                imageProxy.close() // Continue processing the video stream if no face is detected
//            }
//        }.addOnFailureListener {
//            it.printStackTrace()
//            imageProxy.close() // Continue processing the video stream in case of failure
//        }
//    }
//
//    private fun cropFaceFromImage(imageProxy: ImageProxy, face: Face): Bitmap {
//        val cropRect = Rect(
//            max(0, face.boundingBox.left),
//            max(0, face.boundingBox.top),
//            min(imageProxy.width, face.boundingBox.right),
//            min(imageProxy.height, face.boundingBox.bottom)
//        )
////        val bitmap = imageProxy.toBitmap()
//
//        // perform crop on bitmap
//        val originalBitmap = imageProxy.toBitmap()
//        Log.i("bitmaps", "original height: ${originalBitmap.height}")
//        Log.i("bitmaps", "original width: ${originalBitmap.width}")
//
//        // Perform crop on bitmap
//        val croppedBitmap = Bitmap.createBitmap(
//            originalBitmap,
//            cropRect.left,
//            cropRect.top,
//            cropRect.width(),
//            cropRect.height()
//        )
//        Log.i("bitmaps", "cropped height: ${croppedBitmap.height}")
//        Log.i("bitmaps", "cropped width: ${croppedBitmap.width}")
//
//        val resizedBitmap = resizeBitmap(
//            croppedBitmap,
//            inputSize,
//            inputSize
//        ) // Return output of Resize cropped bitmap which is the final bitmap
//        Log.i("bitmaps", "resized height: ${resizedBitmap.height}")
//        Log.i("bitmaps", "resized width: ${resizedBitmap.width}")
//
////        return Bitmap.createBitmap(160,160,Bitmap.Config.ARGB_8888) // return dummy variable
//        return resizedBitmap
//    }
//
//    private fun resizeBitmap(bitmap: Bitmap, newWidth: Int, newHeight: Int): Bitmap {
//        return Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
//    }
//
//    private val imageTensorProcessor = ImageProcessor.Builder()
//        .add(ResizeOp(inputSize, inputSize, ResizeOp.ResizeMethod.BILINEAR))
//        .add(CastOp(DataType.FLOAT32))
//        .build()
//
//    private fun convertBitmapToBuffer(image: Bitmap): ByteBuffer {
//        return imageTensorProcessor.process(TensorImage.fromBitmap(image)).buffer
//    }
//
//    private fun runFaceNet(inputs: Any): FloatArray {
//        val output = Array(1) { FloatArray(embeddingDim) }
//        interpreter.run(inputs, output) // replaces the values in outputArray inplace
//        Log.i("recogOutput", output[0].contentToString())
//        return output[0]
//    }
//
//    // function run for normal video stream
//    @SuppressLint("UnsafeOptInUsageError")
//    private fun processImageProxy(detector: FaceDetector, imageProxy: ImageProxy) {
//        if (isFaceProcessing) {
//            imageProxy.close()
//            return
//        }
//        isFaceProcessing = true
//        val inputImage =
//            InputImage.fromMediaImage(imageProxy.image!!, imageProxy.imageInfo.rotationDegrees)
//        detector.process(inputImage).addOnSuccessListener { faces ->
//            if (faces.isNotEmpty()) {
//                isFaceDetected = true
//                binding.faceBoxOverlay.clear()
//                faces.forEach { face ->
//                    val faceBox = FaceBox(binding.faceBoxOverlay, face, imageProxy.image!!.cropRect)
//                    binding.faceBoxOverlay.add(faceBox)
//                }
//            } else {
//                isFaceDetected = false
//            }
//            isFaceProcessing = false
//        }.addOnFailureListener {
//            it.printStackTrace()
//        }.addOnCompleteListener {
//            imageProxy.close()
//        }
//    }
//
//    companion object {
//        fun startScanner(context: Context, onScan: () -> Unit) {
//            Intent(context, CheckRecordsActivity::class.java).also {
//                context.startActivity(it)
//            }
//        }
//    }
//}