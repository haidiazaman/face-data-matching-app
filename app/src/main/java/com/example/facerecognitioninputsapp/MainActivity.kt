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
            Log.i("requestPermissionLauncher", "Permission Granted!")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        // connect the current activity to its layout xml file
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        getCurrentSharedPreferencesDict()
        // uncomment these 2 lines of code to clear phone SharedPreferences for this app
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
            Log.i("getCurrentSharedPreferencesDict start", "--- start ---")
            Log.i("getCurrentSharedPreferencesDict Key", key)
            Log.i("getCurrentSharedPreferencesDict Value", value ?: "null")
            Log.i("getCurrentSharedPreferencesDict end", "--- end ---")
            Log.i("getCurrentSharedPreferencesDict end", "")
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