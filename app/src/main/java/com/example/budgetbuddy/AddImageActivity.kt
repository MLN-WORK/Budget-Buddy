package com.example.budgetbuddy

import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.example.budgetbuddy.databinding.ActivityAddImageBinding
import java.io.File
import java.io.FileOutputStream
import java.util.*

class AddImageActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAddImageBinding
    private var imageUri: Uri? = null

    private val takePictureLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val extras = result.data?.extras
                val imageBitmap = extras?.get("data") as? Bitmap

                imageBitmap?.let {
                    binding.ivImgPreview.setImageBitmap(it)
                    saveImageToExternalStorage(it)?.let { savedUri ->
                        returnWithResult(savedUri)
                    }
                }
            }
        }

    private val pickImageLauncher =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            uri?.let {
                imageUri = uri
                binding.ivImgPreview.setImageURI(uri)

                try {
                    val bitmap = MediaStore.Images.Media.getBitmap(contentResolver, uri)
                    saveImageToExternalStorage(bitmap)?.let { savedUri ->
                        returnWithResult(savedUri)
                    }
                } catch (e: Exception) {
                    ToastUtil.showCustomToast(this, "Failed to load image: ${e.message}")
                }
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddImageBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Open gallery
        binding.btnOpenGallery.setOnClickListener {
            pickImageLauncher.launch("image/*")
        }

        // Open camera
        binding.btnTakePhoto.setOnClickListener {
            val cameraIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
            takePictureLauncher.launch(cameraIntent)
        }

        // Save image manually (optional)
        binding.btnSaveImg.setOnClickListener {
            imageUri?.let { uri ->
                try {
                    val bitmap = MediaStore.Images.Media.getBitmap(contentResolver, uri)
                    saveImageToExternalStorage(bitmap)?.let { savedUri ->
                        returnWithResult(savedUri)
                    }
                } catch (e: Exception) {
                    ToastUtil.showCustomToast(this, "Failed to save image: ${e.message}")
                }
            }
        }

        // Back button
        binding.ivBackBtn.setOnClickListener {
            finish()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 1) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                toast("Storage permission granted!")
            } else {
                toast("Storage permission denied!")
            }
        }
    }

    private fun toast(message: String) {
        ToastUtil.showCustomToast(this, message)
    }

    private fun saveImageToExternalStorage(bitmap: Bitmap): Uri? {
        return try {
            val directory = File(getExternalFilesDir(Environment.DIRECTORY_PICTURES), "BudgetBuddy")
            if (!directory.exists()) directory.mkdirs()

            val imageFile = File(directory, "${UUID.randomUUID()}.jpg")

            FileOutputStream(imageFile).use { outputStream ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 70, outputStream)
            }
            ToastUtil.showCustomToast(this, "Image saved successfully!")

            Uri.fromFile(imageFile)
        } catch (e: Exception) {
            ToastUtil.showCustomToast(this, "Failed to save image: ${e.message}")
            null
        }
    }

    private fun returnWithResult(uri: Uri) {
        val resultIntent = Intent()
        resultIntent.putExtra("imageUri", uri.toString())
        setResult(Activity.RESULT_OK, resultIntent)
        finish()
    }
}
