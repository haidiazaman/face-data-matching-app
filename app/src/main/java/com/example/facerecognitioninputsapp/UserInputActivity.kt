package com.example.facerecognitioninputsapp

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.example.facerecognitioninputsapp.databinding.ActivityUserInputBinding
import com.google.gson.Gson

// create a data class like a dict to store the user info
data class UserData(
    val question1: String,
    val question2: String,
    val question3: String,
    val question4: String,
    val question5: String,
    val question6: String,
    val question7: String
)

class UserInputActivity : AppCompatActivity() {
    private lateinit var binding: ActivityUserInputBinding
    private lateinit var sharedPreferences: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityUserInputBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // get the face recognition model output from ScannerActivity.kt
        // output wont be null, model will definitely output an array since its not possible to run the model if no face is detected
        val faceRecognitionResult = intent.getFloatArrayExtra("faceRecognitionResult")

        Log.i(
            "UserInputActivity onCreate ",
            "ScannerActivity output: ${faceRecognitionResult.contentToString()}"
        )
        // Initialize SharedPreferences
        sharedPreferences =
            getSharedPreferences("face_recognition_app_user_data", Context.MODE_PRIVATE)

        // Button click listener
        binding.completeRegistrationButton.setOnClickListener {
            // Read values from EditTexts
            val question1 = binding.editTextQuestion1.text.toString()
            val question2 = binding.editTextQuestion2.text.toString()
            val question3 = binding.editTextQuestion3.text.toString()
            val question4 = binding.editTextQuestion4.text.toString()
            val question5 = binding.editTextQuestion5.text.toString()
            val question6 = binding.editTextQuestion6.text.toString()
            val question7 = binding.editTextQuestion7.text.toString()

            if (question1.isEmpty() || question2.isEmpty() || question3.isEmpty() || question4.isEmpty()) {
                // call function that will raise error
                showMissingInputFieldsPrompt()
            } else {
                // Create UserData object
                val userData = UserData(
                    question1,
                    question2,
                    question3,
                    question4,
                    question5,
                    question6,
                    question7
                )

                // Convert UserData object to JSON
                val userDataJson = Gson().toJson(userData)

                // use the faceRecognitionResult as the key for this UserData - convert to String
                val faceRecognitionKey = faceRecognitionResult.contentToString()
                Log.i("faceRecognitionKey", faceRecognitionKey)
                // Save values to SharedPreferences with faceRecognitionKey as the key
                saveToSharedPreferences(faceRecognitionKey, userDataJson)

                showSuccessfulRegistrationPrompt() // which includes moving on to next activity
            }
        }
    }

    // Function to save a String value to SharedPreferences
    private fun saveToSharedPreferences(key: String, value: String) {
        val editor = sharedPreferences.edit()
        editor.putString(key, value)
        editor.apply()
    }

    // Function to show a prompt for missing input in some fields
    private fun showMissingInputFieldsPrompt() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Missing input fields")
        builder.setMessage("Please input all fields before clicking the Register button")
        builder.setPositiveButton("OK") { _, _ ->
            // Handle OK button click if needed
        }
        val dialog: AlertDialog = builder.create()
        dialog.show()
    }


    // Function to show a prompt for successful registration
    private fun showSuccessfulRegistrationPrompt() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Person Successfully Registered")
        builder.setMessage("Thank you! You can now use the 'Cross check medical records' button to scan the same person's face to retrieve their data in the future!")
        builder.setPositiveButton("OK") { _, _ ->
            // Move to the next activity
            val intent = Intent(this@UserInputActivity, MainActivity::class.java)
            startActivity(intent)
            finish()
        }
        val dialog: AlertDialog = builder.create()
        dialog.show()
    }
}