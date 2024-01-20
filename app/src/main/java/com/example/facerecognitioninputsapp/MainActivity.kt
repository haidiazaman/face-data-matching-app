package com.example.facerecognitioninputsapp

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.example.facerecognitioninputsapp.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {
    private val cameraPermission = android.Manifest.permission.CAMERA
    private lateinit var binding: ActivityMainBinding
    private lateinit var sharedPreferences: SharedPreferences

    private val requestPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) {isGranted ->
        if (isGranted) {
//            startScanner()
            Log.i("temp", "put this line of code so that if statement not empty, need to see how to change this method of permission")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        getCurrentSharedPreferencesDict()
//        clearSharedPreferences()
//        getCurrentSharedPreferencesDict()

        // button 1 > ScannerActivity.kt
        binding.registerButton.setOnClickListener {
            requestCameraAndStartRegistration()
        }
        // button 2 > CheckRecordsActivity.kt
        binding.checkButton.setOnClickListener {
            requestCameraAndStartCheckRecords()
        }
    }

    private fun clearSharedPreferences () {
        // Initialize SharedPreferences
        val editor = sharedPreferences.edit()
        editor.clear()
        editor.apply()
    }

    private fun getCurrentSharedPreferencesDict() {

        sharedPreferences =
            getSharedPreferences("face_recognition_app_user_data", Context.MODE_PRIVATE)

        val allKeyValuePairs: Map<String, *> = sharedPreferences.all
        val allKeys: Set<String> = allKeyValuePairs.keys

        for (key in allKeys) {
            val value = sharedPreferences.getString(key, null) // Assuming your values are strings, adjust accordingly

            Log.i("SharedPreferences Key", key)
            Log.i("SharedPreferences Value", value ?: "null")
        }
    }


    private fun requestCameraAndStartRegistration() {
        if (isPermissionGranted(cameraPermission)) {
            ScannerActivity.startScanner(this) { }
        } else {
            requestCameraPermission()
        }
    }

    private fun requestCameraAndStartCheckRecords() {
        if (isPermissionGranted(cameraPermission)) {
            CheckRecordsActivity.startScanner(this) { }
//            val intent = Intent(this, RetrieveDataActivity::class.java)
//            startActivity(intent)
//            finish()
        } else {
            requestCameraPermission()
        }
    }

    private fun requestCameraPermission() {
        when {
            shouldShowRequestPermissionRationale(cameraPermission) -> {
                cameraPermissionRequest(
                    positive = { openPermissionSetting() }
                )
            }
            else -> {
                requestPermissionLauncher.launch(cameraPermission)
            }
        }
    }
}