package com.example.budgetbuddy

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import com.example.budgetbuddy.databinding.ActivityAddImageBinding
import java.io.File
import java.util.UUID

class AddImageActivity : AppCompatActivity() {
    private lateinit var binding: ActivityAddImageBinding
    private var selectedImage: File? = null
    private var pendingCameraImage: File? = null

    private val takePicture = registerForActivityResult(ActivityResultContracts.TakePicture()) { saved ->
        if (saved) {
            selectedImage = pendingCameraImage
            selectedImage?.let { binding.ivImgPreview.setImageURI(Uri.fromFile(it)) }
            updateSaveButton()
        } else pendingCameraImage?.delete()
        pendingCameraImage = null
    }

    private val requestCamera = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) launchCamera() else toast(getString(R.string.camera_permission_needed))
    }

    private val pickImage = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri ?: return@registerForActivityResult
        runCatching {
            val destination = newReceiptFile()
            contentResolver.openInputStream(uri).use { input -> requireNotNull(input).copyTo(destination.outputStream()) }
            selectedImage = destination
            binding.ivImgPreview.setImageURI(Uri.fromFile(destination))
            updateSaveButton()
        }.onFailure { toast(getString(R.string.image_load_failed)) }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddImageBinding.inflate(layoutInflater)
        setContentView(binding.root)
        updateSaveButton()
        binding.btnOpenGallery.setOnClickListener { pickImage.launch("image/*") }
        binding.btnTakePhoto.setOnClickListener { requestCamera.launch(Manifest.permission.CAMERA) }
        binding.btnSaveImg.setOnClickListener {
            val image = selectedImage ?: return@setOnClickListener
            setResult(Activity.RESULT_OK, Intent().putExtra(EXTRA_IMAGE_PATH, image.absolutePath))
            finish()
        }
        binding.ivBackBtn.setOnClickListener { finish() }
    }

    private fun launchCamera() {
        val image = newReceiptFile()
        pendingCameraImage = image
        takePicture.launch(FileProvider.getUriForFile(this, "$packageName.provider", image))
    }

    private fun newReceiptFile(): File {
        val directory = File(filesDir, "receipts").apply { mkdirs() }
        return File(directory, "${UUID.randomUUID()}.jpg")
    }

    private fun updateSaveButton() {
        binding.btnSaveImg.isEnabled = selectedImage != null
        binding.btnSaveImg.alpha = if (selectedImage == null) 0.5f else 1f
    }

    private fun toast(message: String) = ToastUtil.showCustomToast(this, message)

    companion object { const val EXTRA_IMAGE_PATH = "imagePath" }
}
