package com.example.facerecognitioninputsapp

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.example.facerecognitioninputsapp.databinding.ActivityRetrieveDataBinding
import com.google.gson.Gson

class RetrieveDataActivity : AppCompatActivity() {
    private lateinit var binding: ActivityRetrieveDataBinding
    private lateinit var sharedPreferences: SharedPreferences
//    private val matchedKey: FloatArray? = intent.getFloatArrayExtra("faceRecognitionResult")
//private lateinit var matchedKey: FloatArray
    private lateinit var matchedKey: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRetrieveDataBinding.inflate(layoutInflater)
        setContentView(binding.root)
        sharedPreferences =
            getSharedPreferences("face_recognition_app_user_data", Context.MODE_PRIVATE)
//        val matchedKey = "test1"
        matchedKey = intent.getStringExtra("matchedKey")!!
//        matchedKey = intent.getFloatArrayExtra("faceRecognitionResult")!!
        Log.i("retrieveMatchedKey", matchedKey)
//        matchedKey = matchedKey.contentToString() ?.joinToString(separator = ",") ?: ""

        val retrievedJson = readFromSharedPreferences(matchedKey)

        val gson = Gson()
        val userData: UserData? = gson.fromJson(retrievedJson, UserData::class.java)

//         Now you can access each element
        val question1: String = userData?.question1 ?: "nil"
        val question2: String = userData?.question2 ?: "nil"
        val question3: String = userData?.question3 ?: "nil"
        val question4: String = userData?.question4 ?: "nil"

        binding.answer1.text = question1
        binding.answer2.text = question2
        binding.answer3.text = question3
        binding.answer4.text = question4

        // Button click listener
        binding.doneButton.setOnClickListener {
            // add logic for all fields filled then can move on to next page else you get warning error
            // also have a prompt to say the registration is successful
            // move on to next activity
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
            finish()
        }
    }

    // Function to read a String value from SharedPreferences
    private fun readFromSharedPreferences(key: String): String? {
        return sharedPreferences.getString(key, null)
    }
}