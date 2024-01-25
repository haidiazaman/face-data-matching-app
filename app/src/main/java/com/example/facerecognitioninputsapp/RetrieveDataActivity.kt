package com.example.facerecognitioninputsapp

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.text.Html
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.example.facerecognitioninputsapp.databinding.ActivityRetrieveDataBinding
import com.google.gson.Gson

class RetrieveDataActivity : AppCompatActivity() {
    private lateinit var binding: ActivityRetrieveDataBinding
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var matchedKey: String

    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRetrieveDataBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // pull the data from SharedPreferences
        sharedPreferences =
            getSharedPreferences("face_recognition_app_user_data", Context.MODE_PRIVATE)
        // matchedKey data will be output as String in CheckRecordsActivity.kt - thus use intent.getStringExtra, else. intent.getFloatArrayExtra
        matchedKey = intent.getStringExtra("matchedKey")!!
        Log.i("retrievedMatchedKey", matchedKey)

        val retrievedJson = readFromSharedPreferences(matchedKey)
        val gson = Gson()
        val userData: UserData? = gson.fromJson(retrievedJson, UserData::class.java)

        // Now you can access each element
        val question1: String = userData?.question1 ?: "nil"
        val question2: String = userData?.question2 ?: "nil"
        val question3: String = userData?.question3 ?: "nil"
        val question4: String = userData?.question4 ?: "nil"
        val question5: String = userData?.question5 ?: "nil"
        val question6: String = userData?.question6 ?: "nil"
        val question7: String = userData?.question7 ?: "nil"

        // set the current fields with HTML formatting
        binding.answer1.text = Html.fromHtml("<b>Name:</b> <u>$question1<u>")
        binding.answer2.text = Html.fromHtml("<b>Date of Birth:</b> <u>$question2<u>")
        binding.answer3.text = Html.fromHtml("<b>Gender:</b> <u>$question3<u>")
        binding.answer4.text = Html.fromHtml("<b>Occupation:</b> <u>$question4<u>")
        binding.answer5.text = Html.fromHtml("<b>Allergies:</b> <u>$question5<u>")
        binding.answer6.text = Html.fromHtml("<b>Existing Medical Conditions:</b> <u>$question6<u>")
        binding.answer7.text = Html.fromHtml("<b>Insurance Provider:</b> <u>$question7<u>")


        // Button click listener
        binding.doneButton.setOnClickListener {
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