package com.example.budgetbuddy

import android.Manifest
import android.app.Activity
import android.content.ClipData
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Environment
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

    private val takePicture = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val capturedImage = pendingCameraImage
        revokeCameraUriPermission(capturedImage)
        if (capturedImage?.isFile == true && capturedImage.length() > 0L) {
            selectedImage = capturedImage
            showPreview(capturedImage)
            updateSaveButton()
        } else {
            capturedImage?.delete()
            if (result.resultCode == Activity.RESULT_OK) toast(getString(R.string.camera_capture_failed))
        }
        pendingCameraImage = null
    }

    private val requestCamera = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) launchCamera() else toast(getString(R.string.camera_permission_needed))
    }

    private val pickImage = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri ?: return@registerForActivityResult
        var destination: File? = null
        runCatching {
            destination = newReceiptFile()
            contentResolver.openInputStream(uri).use { input ->
                requireNotNull(destination).outputStream().use { output -> requireNotNull(input).copyTo(output) }
            }
            selectedImage = destination
            showPreview(requireNotNull(destination))
            updateSaveButton()
        }.onFailure {
            destination?.delete()
            toast(getString(R.string.image_load_failed))
        }
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
        val captureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        if (captureIntent.resolveActivity(packageManager) == null) {
            toast(getString(R.string.camera_unavailable))
            return
        }
        runCatching {
            val image = newReceiptFile()
            val imageUri = FileProvider.getUriForFile(this, "$packageName.provider", image)
            pendingCameraImage = image
            captureIntent.apply {
                putExtra(MediaStore.EXTRA_OUTPUT, imageUri)
                clipData = ClipData.newRawUri(getString(R.string.receipt_photo), imageUri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
            }
            packageManager.queryIntentActivities(captureIntent, PackageManager.MATCH_DEFAULT_ONLY).forEach {
                grantUriPermission(
                    it.activityInfo.packageName,
                    imageUri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                )
            }
            takePicture.launch(captureIntent)
        }.onFailure {
            revokeCameraUriPermission(pendingCameraImage)
            pendingCameraImage?.delete()
            pendingCameraImage = null
            toast(getString(R.string.camera_capture_failed))
        }
    }

    private fun newReceiptFile(): File {
        val externalDirectory = getExternalFilesDir(Environment.DIRECTORY_PICTURES)
            ?.let { File(it, "BudgetBuddy/Receipts") }
            ?.takeIf { it.isDirectory || it.mkdirs() }
        val directory = externalDirectory ?: File(filesDir, "receipts")
        check(directory.isDirectory || directory.mkdirs()) { "Receipt directory is unavailable" }
        return File.createTempFile("receipt-${UUID.randomUUID()}-", ".jpg", directory)
    }

    private fun revokeCameraUriPermission(image: File?) {
        val uri = image?.let {
            runCatching { FileProvider.getUriForFile(this, "$packageName.provider", it) }.getOrNull()
        } ?: return
        runCatching {
            revokeUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            )
        }
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
