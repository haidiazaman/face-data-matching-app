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

    private lateinit var binding: ActivityCheckRecordsBinding
    private lateinit var cameraSelector: CameraSelector
    private lateinit var cameraProviderFuture: ListenableFuture<ProcessCameraProvider>
    private lateinit var processCameraProvider: ProcessCameraProvider
    private lateinit var cameraPreview: Preview
    private lateinit var imageAnalysis: ImageAnalysis
    private lateinit var context: Context

    private var isFaceDetected = false
    private var isFaceDetectionRunning = false
    private var isFaceRecognitionEnabled =
        false // set to true to enable the faceRecognition model to do inference
    private lateinit var interpreter: Interpreter
    private val interpreterOptions = Interpreter.Options().apply { numThreads = 4 }
    private val inputSize = 160
    private val embeddingDim = 128// 192 //  // this is the output size
    private var faceRecognitionOutput =
        FloatArray(embeddingDim) // this is the actual output of the faceRecog model to be updated inPlace
    private lateinit var sharedPreferences: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        // connect the current activity to its layout xml file
        super.onCreate(savedInstanceState)
        binding = ActivityCheckRecordsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // initialise sharedPreferences to get the json data
        // this is required since we need to get existing data to get the all the keys currently saved
        sharedPreferences =
            getSharedPreferences("face_recognition_app_user_data", Context.MODE_PRIVATE)

        // setup the camera selector
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
            // when button is clicked - face should be detected to enable faceRecognition to start
            if (isFaceDetected) {
                // if face is detected, set this key to true to start passing the face to the facenet model to save the embedding logits
                isFaceRecognitionEnabled = true
            } else {
                // if face is not detected, then facerecog should be kept at false to not enable the facerecog model to do inference
                isFaceRecognitionEnabled = false
                // Show a prompt indicating no face detected
                showNoFaceDetectedPrompt() // Show a prompt indicating no face detected
            }
        }

        // initialise face recognition interpreter
        this.also { context = it }
        try {
            // place tflite model in main>assets>file_name.tflite, only need to put the file_name.tflite instead of absolute path
//            interpreter = Interpreter(FileUtil.loadMappedFile(context, "mobile_face_net.tflite"), interpreterOptions)
            interpreter = Interpreter(FileUtil.loadMappedFile(context, "facenet.tflite"), interpreterOptions)
            // uncomment this line of code to double check your model's input and embedding dim
            val inputTensorShape = interpreter.getInputTensor(0).shape()
            val checkInputDim = inputTensorShape[inputTensorShape.size - 1]
            Log.i("interpreter dim input", "input dim: $checkInputDim")
            val outputTensorShape = interpreter.getOutputTensor(0).shape()
            val checkEmbeddingDim = outputTensorShape[outputTensorShape.size - 1]
            Log.i("interpreter dim embedding", "embedding dim: $checkEmbeddingDim")
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }


    private fun bindCameraPreview() {
        cameraPreview = Preview.Builder()
            .setTargetRotation(binding.previewView.display.rotation)
            .build()
        cameraPreview.setSurfaceProvider(binding.previewView.surfaceProvider)
        processCameraProvider.bindToLifecycle(this, cameraSelector, cameraPreview)
    }

    private fun bindInputAnalyser() {
        // initialise google mlkit face detector
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
            if (isFaceRecognitionEnabled) {
                // if button clicked and face is detected, performFaceRecognition is called and then output saved and go to next activity
                performFaceRecognition(detector, imageProxy)
                isFaceRecognitionEnabled = false
            } else {
                // continue video stream
                processImageProxy(detector, imageProxy)
            }
        }
        processCameraProvider.bindToLifecycle(this, cameraSelector, imageAnalysis)
    }


    // function to run normal video stream
    @SuppressLint("UnsafeOptInUsageError")
    private fun processImageProxy(detector: FaceDetector, imageProxy: ImageProxy) {
        if (isFaceDetectionRunning) {
            // close the current image input to continue processing video stream
            imageProxy.close()
        }
        isFaceDetectionRunning = true
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
                // remove the bbox from before if there was a face detected > then the face dissapears
                binding.faceBoxOverlay.clear()
            }
            isFaceDetectionRunning = false
        }.addOnFailureListener {
            it.printStackTrace()
        }.addOnCompleteListener {
            imageProxy.close()
        }
    }

    // function to run cropped face through facerecog model if button is clicked + faceDetected=true
    @SuppressLint("UnsafeOptInUsageError")
    private fun performFaceRecognition(detector: FaceDetector, imageProxy: ImageProxy) {
        // Process the current frame
        val inputImage = InputImage.fromMediaImage(imageProxy.image!!, imageProxy.imageInfo.rotationDegrees)
        detector.process(inputImage).addOnSuccessListener { faces ->
            if (faces.isNotEmpty()) {
                isFaceDetected = true
                binding.faceBoxOverlay.clear() // clear current bbox

                val face =
                    faces[0] // Get the first detected face, since we only working with 1 user registration
                // Crop and resize the face from the original image using the face detector bbox coordinates and resize to model input size
                val croppedFaceBitmap = cropAndResizeFace(
                    imageProxy,
                    face
                )
                // Convert the cropped face Bitmap to ByteBuffer
                val inputBuffer =
                    convertBitmapToByteBuffer(croppedFaceBitmap)
                // Run inference
                faceRecognitionOutput = runFaceRecognitionModel(inputBuffer)
                Log.i(
                    "performFaceRecognition faceRecog model output",
                    "faceRecog model output .contentToString(): ${faceRecognitionOutput.contentToString()}"
                )
                Log.i("check face recog running", "FINAL MATCHEDKEY FUNCTION CALLED")
                // check for existing matched face in database, if none, return error prompt message
                val matchedKey: String? = getMatchingKey(faceRecognitionOutput) // ? since output type might be null
                Log.i("performFaceRecognition matchedKey", "$matchedKey")
                Log.i("check face recog running", "FINAL MATCHEDKEY: $matchedKey")

                if (matchedKey != null) {
                    // pass the current matchedKey to next activity - matchedKey is in type String

                    // successful face matched found prompt
                    val builder = AlertDialog.Builder(this)
                    builder.setTitle("Found a face match!")
                    builder.setMessage("Click OK to see these person's medical records.")
                    builder.setPositiveButton("OK") { _, _ ->
                        // pass the current matchedKey to next activity - matchedKey is in type String
                        val intent = Intent(this, RetrieveDataActivity::class.java).also {
                            it.putExtra("matchedKey", matchedKey)
                        }


                        // Start the new activity
                        startActivity(intent)
                        finish()

                        isFaceRecognitionEnabled = false
                        // close the current image input
                        imageProxy.close()
                    }
                    val dialog: AlertDialog = builder.create()
                    dialog.show()

                } else {
                    // Set isFaceRecognitionEnabled to false after face recognition is performed
                    isFaceRecognitionEnabled = false
                    showNoFaceMatchedPrompt()
                    // close the current image input
                    imageProxy.close()
                }
            } else {
                isFaceDetected = false
                imageProxy.close() // Continue processing the video stream if no face is detected
            }
        }.addOnFailureListener {
            it.printStackTrace()
            imageProxy.close() // Continue processing the video stream in case of failure
        }
    }

    // Function to show a prompt for no matched face in database
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


    // function to crop and resize the face from the image using the mlkit bbox info
    // resize to face recognition model's inputDim
    private fun cropAndResizeFace(imageProxy: ImageProxy, face: Face): Bitmap {
        // initialise the Rect to use in cropping code snippet
        val cropRect = Rect(
            max(0, face.boundingBox.left),
            max(0, face.boundingBox.top),
            min(imageProxy.width, face.boundingBox.right),
            min(imageProxy.height, face.boundingBox.bottom)
        )

        // convert imageProxy to bitmap for processing
        val originalBitmap = imageProxy.toBitmap()
        Log.i("cropAndResizeFace original bitmap", "original height: ${originalBitmap.height}")
        Log.i("cropAndResizeFace original bitmap", "original width: ${originalBitmap.width}")

        // Perform crop on bitmap
        val croppedBitmap = Bitmap.createBitmap(
            originalBitmap,
            cropRect.left,
            cropRect.top,
            cropRect.width(),
            cropRect.height()
        )
        Log.i("cropAndResizeFace cropped bitmap", "cropped height: ${croppedBitmap.height}")
        Log.i("cropAndResizeFace cropped bitmap", "cropped width: ${croppedBitmap.width}")

        // Perform resizing on the bitmap
        val finalBitmap = Bitmap.createScaledBitmap(croppedBitmap, inputSize, inputSize, true)
        // Return output of Resize cropped bitmap which is the final bitmap
        Log.i("cropAndResizeFace final resized", "resized height: ${finalBitmap.height}")
        Log.i("cropAndResizeFace final resized", "resized width: ${finalBitmap.width}")

//        return Bitmap.createBitmap(160,160,Bitmap.Config.ARGB_8888) // create a dummy return var to check code
        return finalBitmap
    }


    // necessary function to work with ML models in android
    // bitmap - android image representation
    // byte buffer -  ByteBuffer. This conversion is often necessary when working with machine learning models, as they typically require input data in the form of a ByteBuffer.
    private fun convertBitmapToByteBuffer(image: Bitmap): ByteBuffer {
        val imageTensorProcessor = ImageProcessor.Builder()
            .add(ResizeOp(inputSize, inputSize, ResizeOp.ResizeMethod.BILINEAR))
            .add(CastOp(DataType.FLOAT32))
            .build()
        return imageTensorProcessor.process(TensorImage.fromBitmap(image)).buffer
    }

    // run the facerecognition model
    private fun runFaceRecognitionModel(inputs: Any): FloatArray {
        Log.i(
            "runFaceRecognitionModel input",
            "input.contentToString(): ${inputs}"
        )
        val output = Array(1) { FloatArray(embeddingDim) }
        interpreter.run(inputs, output) // replaces the values in outputArray inplace
        Log.i(
            "runFaceRecognitionModel output",
            "output.contentToString(): ${output[0].contentToString()}"
        )
        return output[0]
    }

    // Function to loop through all keys, store similarity scores, and return the highest one if above threshold
    private fun getMatchingKey(currentFaceRecognitionOutput: FloatArray): String? {
        // get current key value pairs in sharedPreferences - this is the database
        val allKeyValuePairs: Map<String, *> = sharedPreferences.all
        val allKeys: Set<String> = allKeyValuePairs.keys

        Log.i("getMatchingKey current face", "current face to check: $currentFaceRecognitionOutput")
        Log.i("getMatchingKey current face", "current face to check toString(): ${currentFaceRecognitionOutput.contentToString()}")

        //  initialise the maxSim and matchingKey
        var maxSimilarityScore = Float.MIN_VALUE
        var matchingKey: String? = null

        for (key in allKeys) {
            try {
                Log.i("getMatchingKey", "--- Loop: start new key ---")
                Log.i("getMatchingKey", "key in String: ${key}")
                // convert current key which is in type String to FloatArray
                val floatArrayKey = key
                    .trimStart('[') // Remove leading '['
                    .trimEnd(']')   // Remove trailing ']'
                    .split(", ")    // Split the string by ", "
                    .map { it.toFloat() }
                    .toFloatArray()
                Log.i("getMatchingKey", "key in FloatArrayKey: $floatArrayKey")

                // calculate the similarity score btwn current face and key
                // can also change to l2Norm function
                val similarityScore = cosineSimilarity(currentFaceRecognitionOutput, floatArrayKey)
                Log.i("getMatchingKey", "similarityScore: $similarityScore")

                // update the maxSim and matchingKey if current score > max thus far
                if (similarityScore > maxSimilarityScore) {
                    maxSimilarityScore = similarityScore
                    matchingKey = key
                }
                Log.i("getMatchingKey", "--- Loop: end of new key ---")
                Log.i("getMatchingKey", "")
            } catch (e: NumberFormatException) {
                // Handle the case where key cannot be converted to FloatArray
                Log.e("NumberFormatException", "Error converting key to FloatArray: $key")
            }
        }
        if (matchingKey != null) {
            Log.i("getMatchingKey", "null $matchingKey")
        }

        Log.i("getMatchingKey final", "current max similarity score: $maxSimilarityScore")
        Log.i("getMatchingKey final", "current matchedKey $matchingKey")

        // Adjust threshold as needed
        val threshold = 0.8
        return if (maxSimilarityScore > threshold) matchingKey else null
    }

    // function to do similarity check - cosinesimilarity
    private fun cosineSimilarity(x1: FloatArray, x2: FloatArray): Float {
        var dotProduct = 0.0f
        var normA = 0.0f
        var normB = 0.0f
        Log.i("cosineSimilarity", "cosineSimilarity: ${x1.contentToString()}")
        Log.i("cosineSimilarity", "cosineSimilarity: ${x2.contentToString()}")

        for (i in x1.indices) {
            dotProduct += x1[i] * x2[i]
            normA += x1[i].pow(2)
            normB += x2[i].pow(2)
        }
        normA = sqrt(normA)
        normB = sqrt(normB)
        val similarityScore = dotProduct / (normA * normB)
        Log.i("", "--- NEW ENTRY --")
        Log.i("cosineSimilarity", "dotProduct: $dotProduct")
        Log.i("cosineSimilarity", "normA: $normA")
        Log.i("cosineSimilarity", "normB: $normB")
        Log.i("cosineSimilarity", "similarityScore: $similarityScore")
        Log.i("cosineSimilarity", "--- END OF ENTRY --")
        Log.i("cosineSimilarity", "")

        return similarityScore
    }


    // alternative function to do similarity check - l2Norm, performance not so good using this one
    private fun l2Norm(x1: FloatArray, x2: FloatArray): Float {
        var sum = 0.0f
        val mag1 = sqrt(x1.map { xi -> xi.pow(2) }.sum())
        val mag2 = sqrt(x2.map { xi -> xi.pow(2) }.sum())
        Log.i("l2Norm mag1", "mag1: $mag1")
        Log.i("l2Norm mag2", "mag2: $mag2")

        for (i in x1.indices) {
            sum += ((x1[i] / mag1) - (x2[i] / mag2)).pow(2)
        }
        val similarityScore = sqrt(sum)

        Log.i("l2Norm final score", "l2Norm final score: $similarityScore")
        Log.i("l2Norm ----", "")
        return similarityScore
    }

    companion object {
        fun startScanner(context: Context, onScan: () -> Unit) {
            Intent(context, CheckRecordsActivity::class.java).also {
                context.startActivity(it)
            }
        }
    }
}