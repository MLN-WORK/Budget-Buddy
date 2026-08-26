package com.example.budgetbuddy

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.bumptech.glide.Glide
import com.example.budgetbuddy.databinding.ActivityAddImageBinding
import java.io.File
import java.util.UUID

class AddImageActivity : BaseActivity() {
    private lateinit var binding: ActivityAddImageBinding
    private var selectedImage: File? = null
    private var pendingCameraImage: File? = null

    private val takePicture = registerForActivityResult(ActivityResultContracts.TakePicture()) { saved ->
        val capturedImage = pendingCameraImage
        if (saved && capturedImage?.isFile == true && capturedImage.length() > 0L) {
            selectedImage = capturedImage
            showPreview(capturedImage)
            updateSaveButton()
        } else {
            capturedImage?.delete()
            if (saved) toast(getString(R.string.camera_capture_failed))
        }
        pendingCameraImage = null
    }

    private val requestCamera = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) launchCamera() else toast(getString(R.string.camera_permission_needed))
    }

    private val pickImage = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri ?: return@registerForActivityResult
        runCatching {
            val destination = newReceiptFile()
            contentResolver.openInputStream(uri).use { input ->
                destination.outputStream().use { output -> requireNotNull(input).copyTo(output) }
            }
            selectedImage = destination
            showPreview(destination)
            updateSaveButton()
        }.onFailure { toast(getString(R.string.image_load_failed)) }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddImageBinding.inflate(layoutInflater)
        setContentView(binding.root)
        selectedImage = savedInstanceState?.getString(STATE_SELECTED_IMAGE)?.let(::File)?.takeIf(File::exists)
        pendingCameraImage = savedInstanceState?.getString(STATE_PENDING_CAMERA_IMAGE)?.let(::File)
        selectedImage?.let(::showPreview)
        updateSaveButton()
        binding.btnOpenGallery.setOnClickListener { pickImage.launch("image/*") }
        binding.btnTakePhoto.setOnClickListener {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                launchCamera()
            } else {
                requestCamera.launch(Manifest.permission.CAMERA)
            }
        }
        binding.btnSaveImg.setOnClickListener {
            val image = selectedImage ?: return@setOnClickListener
            setResult(Activity.RESULT_OK, Intent().putExtra(EXTRA_IMAGE_PATH, image.absolutePath))
            finish()
        }
        binding.ivBackBtn.setOnClickListener { finish() }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString(STATE_SELECTED_IMAGE, selectedImage?.absolutePath)
        outState.putString(STATE_PENDING_CAMERA_IMAGE, pendingCameraImage?.absolutePath)
    }

    private fun launchCamera() {
        if (Intent(MediaStore.ACTION_IMAGE_CAPTURE).resolveActivity(packageManager) == null) {
            toast(getString(R.string.camera_unavailable))
            return
        }
        runCatching {
            val image = newReceiptFile()
            pendingCameraImage = image
            takePicture.launch(FileProvider.getUriForFile(this, "$packageName.provider", image))
        }.onFailure {
            pendingCameraImage?.delete()
            pendingCameraImage = null
            toast(getString(R.string.camera_capture_failed))
        }
    }

    private fun newReceiptFile(): File {
        val directory = File(filesDir, "receipts").apply { mkdirs() }
        return File.createTempFile("receipt-${UUID.randomUUID()}-", ".jpg", directory)
    }

    private fun showPreview(image: File) {
        Glide.with(this)
            .load(image)
            .fitCenter()
            .into(binding.ivImgPreview)
    }

    private fun updateSaveButton() {
        binding.btnSaveImg.isEnabled = selectedImage != null
        binding.btnSaveImg.alpha = if (selectedImage == null) 0.5f else 1f
    }

    private fun toast(message: String) = ToastUtil.showCustomToast(this, message)

    companion object {
        const val EXTRA_IMAGE_PATH = "imagePath"
        private const val STATE_SELECTED_IMAGE = "selectedImage"
        private const val STATE_PENDING_CAMERA_IMAGE = "pendingCameraImage"
    }
}
