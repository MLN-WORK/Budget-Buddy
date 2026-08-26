package com.example.budgetbuddy

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.view.View
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.bumptech.glide.Glide
import com.example.budgetbuddy.databinding.ActivityAddImageBinding
import java.io.File
import java.util.concurrent.Executors

class AddImageActivity : BaseActivity() {
    private lateinit var binding: ActivityAddImageBinding
    private val imageWorker = Executors.newSingleThreadExecutor()
    private var selectedImage: File? = null
    private var pendingCameraImage: File? = null
    private var importInProgress = false
    private var resultDelivered = false

    private val takePicture = registerForActivityResult(ActivityResultContracts.TakePicture()) { captured ->
        val image = pendingCameraImage
        pendingCameraImage = null
        runCatching {
            if ((captured || image?.length()?.let { it > 0L } == true) &&
                ReceiptStorage.isUsableOwnedReceipt(this, image)
            ) {
                replaceSelection(requireNotNull(image))
            } else {
                image?.delete()
                toast(getString(R.string.camera_capture_failed))
            }
        }.onFailure {
            image?.delete()
            toast(getString(R.string.camera_capture_failed))
        }
    }

    private val requestCamera = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) launchCamera() else toast(getString(R.string.camera_permission_needed))
    }

    private val pickImage = registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        uri?.let(::importGalleryImage)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddImageBinding.inflate(layoutInflater)
        setContentView(binding.root)

        selectedImage = savedInstanceState?.getString(STATE_SELECTED_IMAGE)
            ?.let(::File)
            ?.takeIf { ReceiptStorage.isUsableOwnedReceipt(this, it) }
        pendingCameraImage = savedInstanceState?.getString(STATE_PENDING_CAMERA_IMAGE)?.let(::File)
        selectedImage?.let(::showPreview)
        setLoading(false)

        binding.btnOpenGallery.setOnClickListener {
            pickImage.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
        }
        binding.btnTakePhoto.setOnClickListener {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                launchCamera()
            } else {
                requestCamera.launch(Manifest.permission.CAMERA)
            }
        }
        binding.btnSaveImg.setOnClickListener {
            val image = selectedImage?.takeIf { ReceiptStorage.isUsableOwnedReceipt(this, it) }
            if (image == null) {
                toast(getString(R.string.image_load_failed))
                return@setOnClickListener
            }
            resultDelivered = true
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

    override fun onDestroy() {
        imageWorker.shutdownNow()
        if (isFinishing && !resultDelivered) {
            ReceiptStorage.deleteIfOwned(this, selectedImage?.absolutePath)
            ReceiptStorage.deleteIfOwned(this, pendingCameraImage?.absolutePath)
        }
        super.onDestroy()
    }

    private fun launchCamera() {
        runCatching {
            val image = ReceiptStorage.createCameraDestination(this)
            val uri = FileProvider.getUriForFile(this, "$packageName.provider", image)
            pendingCameraImage = image
            takePicture.launch(uri)
        }.onFailure {
            ReceiptStorage.deleteIfOwned(this, pendingCameraImage?.absolutePath)
            pendingCameraImage = null
            toast(getString(R.string.camera_capture_failed))
        }
    }

    private fun importGalleryImage(uri: Uri) {
        if (importInProgress) return
        setLoading(true)
        imageWorker.execute {
            val imported = runCatching { ReceiptStorage.importFromGallery(applicationContext, uri) }
            runOnUiThread {
                if (isDestroyed || isFinishing) {
                    imported.getOrNull()?.let { ReceiptStorage.deleteIfOwned(this, it.absolutePath) }
                    return@runOnUiThread
                }
                setLoading(false)
                imported
                    .onSuccess(::replaceSelection)
                    .onFailure { toast(getString(R.string.image_load_failed)) }
            }
        }
    }

    private fun replaceSelection(image: File) {
        selectedImage
            ?.takeIf { it.absolutePath != image.absolutePath }
            ?.let { ReceiptStorage.deleteIfOwned(this, it.absolutePath) }
        selectedImage = image
        showPreview(image)
        updateSaveButton()
    }

    private fun showPreview(image: File) {
        runCatching {
            Glide.with(this)
                .load(image)
                .placeholder(R.drawable.placeholder_image)
                .error(R.drawable.placeholder_image)
                .fitCenter()
                .into(binding.ivImgPreview)
        }.onFailure { toast(getString(R.string.image_load_failed)) }
    }

    private fun setLoading(loading: Boolean) {
        importInProgress = loading
        binding.receiptProgress.visibility = if (loading) View.VISIBLE else View.GONE
        binding.btnOpenGallery.isEnabled = !loading
        binding.btnTakePhoto.isEnabled = !loading
        updateSaveButton()
    }

    private fun updateSaveButton() {
        val enabled = !importInProgress && ReceiptStorage.isUsableOwnedReceipt(this, selectedImage)
        binding.btnSaveImg.isEnabled = enabled
        binding.btnSaveImg.alpha = if (enabled) 1f else 0.5f
    }

    private fun toast(message: String) = ToastUtil.showCustomToast(this, message)

    companion object {
        const val EXTRA_IMAGE_PATH = "imagePath"
        private const val STATE_SELECTED_IMAGE = "selectedImage"
        private const val STATE_PENDING_CAMERA_IMAGE = "pendingCameraImage"
    }
}
