package com.budgetbuddy

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
import com.budgetbuddy.databinding.ActivityAddImageBinding
import java.io.File
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.concurrent.Executors

/*
 * Start of class
 * Name of class and related classes (parent/child classes): AddImageActivity
 * Parent class: BaseActivity; child classes: none; related classes: ReceiptStorage, TransactionActivity, and ReceiptOcrScanner when available.
 * What the class does: Captures or imports an app-owned receipt image for a transaction.
 * What's important to other classes, if applicable: It must preserve BaseActivity appearance behavior and use LocalDataStore as the offline source of truth.
 * Code with comments begins below.
 */
class AddImageActivity : BaseActivity() {
    private lateinit var binding: ActivityAddImageBinding
    private lateinit var ocrScanner: ReceiptOcrScanner
    private val imageWorker = Executors.newSingleThreadExecutor()
    private var selectedImage: File? = null
    private var pendingCameraImage: File? = null
    private var processingCameraImage: File? = null
    private var importInProgress = false
    private var resultDelivered = false
    private var ocrResult: ReceiptOcrResult? = null
    private var ocrCategory: String? = null
    private var ocrIsIncome = false
    private var ocrScanId = 0
    private var ocrMode = false
    private var autoCameraLaunched = false

    private val takePicture = registerForActivityResult(ActivityResultContracts.TakePicture()) { captured ->
        val image = pendingCameraImage
        pendingCameraImage = null
        if ((captured || image?.length()?.let { it > 0L } == true) && image != null) {
            processCameraImage(image)
        } else {
            ReceiptStorage.deleteIfOwned(this, image?.absolutePath)
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
        ocrScanner = ReceiptOcrScanner(applicationContext)
        ocrMode = intent.getBooleanExtra(EXTRA_OCR_MODE, false)
        autoCameraLaunched = savedInstanceState?.getBoolean(STATE_AUTO_CAMERA_LAUNCHED, false) ?: false
        binding.tvPgBanner.setText(if (ocrMode) R.string.scan_receipt else R.string.add_image)
        binding.btnSaveImg.setText(if (ocrMode) R.string.use_scanned_receipt else R.string.attach_receipt)

        selectedImage = savedInstanceState?.getString(STATE_SELECTED_IMAGE)
            ?.let(::File)
            ?.takeIf { ReceiptStorage.isUsableOwnedReceipt(this, it) }
        pendingCameraImage = savedInstanceState?.getString(STATE_PENDING_CAMERA_IMAGE)?.let(::File)
        setLoading(false)
        selectedImage?.let {
            showPreview(it)
            if (ocrMode) scanReceipt(it) else updateSaveButton()
        }

        binding.btnOpenGallery.setOnClickListener {
            pickImage.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
        }
        binding.btnTakePhoto.setOnClickListener {
            startCameraWithPermission()
        }
        binding.btnSaveImg.setOnClickListener { deliverSelection() }
        binding.ivBackBtn.setOnClickListener { finish() }
        if (ocrMode && intent.getBooleanExtra(EXTRA_AUTO_CAMERA, false) && !autoCameraLaunched) {
            autoCameraLaunched = true
            binding.root.post(::startCameraWithPermission)
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString(STATE_SELECTED_IMAGE, selectedImage?.absolutePath)
        outState.putString(STATE_PENDING_CAMERA_IMAGE, pendingCameraImage?.absolutePath)
        outState.putBoolean(STATE_AUTO_CAMERA_LAUNCHED, autoCameraLaunched)
    }

    override fun onDestroy() {
        ocrScanId++
        if (::ocrScanner.isInitialized) ocrScanner.close()
        imageWorker.shutdownNow()
        if (isFinishing && !resultDelivered) {
            ReceiptStorage.deleteIfOwned(this, selectedImage?.absolutePath)
            ReceiptStorage.deleteIfOwned(this, pendingCameraImage?.absolutePath)
            ReceiptStorage.deleteIfOwned(this, processingCameraImage?.absolutePath)
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

    private fun startCameraWithPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            launchCamera()
        } else {
            requestCamera.launch(Manifest.permission.CAMERA)
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

    private fun processCameraImage(image: File) {
        if (importInProgress) return
        processingCameraImage = image
        setLoading(true)
        imageWorker.execute {
            val normalized = runCatching {
                ReceiptStorage.finalizeCameraCapture(applicationContext, image)
            }
            runOnUiThread {
                processingCameraImage = null
                if (isDestroyed || isFinishing) {
                    normalized.getOrNull()?.let { ReceiptStorage.deleteIfOwned(this, it.absolutePath) }
                    return@runOnUiThread
                }
                setLoading(false)
                normalized
                    .onSuccess(::replaceSelection)
                    .onFailure { toast(getString(R.string.camera_capture_failed)) }
            }
        }
    }

    private fun replaceSelection(image: File) {
        selectedImage
            ?.takeIf { it.absolutePath != image.absolutePath }
            ?.let { ReceiptStorage.deleteIfOwned(this, it.absolutePath) }
        selectedImage = image
        showPreview(image)
        if (ocrMode) scanReceipt(image) else {
            binding.ocrResultCard.visibility = View.GONE
            updateSaveButton()
        }
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

    private fun scanReceipt(image: File) {
        val scanId = ++ocrScanId
        ocrResult = null
        binding.ocrResultCard.visibility = View.VISIBLE
        binding.tvOcrStatus.setText(R.string.receipt_ocr_scanning)
        binding.tvOcrSummary.visibility = View.GONE
        setLoading(true)
        ocrScanner.scan(
            image,
            onSuccess = { result ->
                runOnUiThread {
                    if (scanId != ocrScanId || isFinishing || isDestroyed) return@runOnUiThread
                    ocrResult = result
                    val localData = LocalDataStore(this)
                    ocrCategory = ReceiptCategoryClassifier.suggest(
                        result.rawText,
                        result.merchant,
                        localData.getCategories(),
                        localData.ocrDefaultCategory
                    )
                    ocrIsIncome = ReceiptCategoryClassifier.isLikelyIncome(result.rawText)
                    setLoading(false)
                    showOcrResult(result)
                    deliverSelection()
                }
            },
            onFailure = {
                runOnUiThread {
                    if (scanId != ocrScanId || isFinishing || isDestroyed) return@runOnUiThread
                    ocrResult = null
                    ocrCategory = LocalDataStore(this).ocrDefaultCategory
                    ocrIsIncome = false
                    setLoading(false)
                    binding.tvOcrStatus.setText(R.string.receipt_ocr_no_details)
                    binding.tvOcrSummary.visibility = View.GONE
                }
            }
        )
    }

    private fun deliverSelection() {
        if (resultDelivered || importInProgress) return
        val image = selectedImage?.takeIf { ReceiptStorage.isUsableOwnedReceipt(this, it) }
        if (image == null) {
            toast(getString(R.string.image_load_failed))
            return
        }
        resultDelivered = true
        setResult(Activity.RESULT_OK, Intent().apply {
            putExtra(EXTRA_IMAGE_PATH, image.absolutePath)
            putExtra(EXTRA_OCR_MODE, ocrMode)
            ocrResult?.merchant?.let { putExtra(EXTRA_OCR_MERCHANT, it) }
            if (ocrMode) putExtra(EXTRA_OCR_DATE, ocrResult?.date ?: today())
            ocrResult?.total?.let { putExtra(EXTRA_OCR_TOTAL, it) }
            ocrResult?.items?.takeIf { it.isNotEmpty() }?.let {
                putStringArrayListExtra(EXTRA_OCR_ITEMS, ArrayList(it))
            }
            ocrCategory?.let { putExtra(EXTRA_OCR_CATEGORY, it) }
            putExtra(EXTRA_OCR_IS_INCOME, ocrIsIncome)
        })
        finish()
    }

    private fun showOcrResult(result: ReceiptOcrResult) {
        if (!result.hasSuggestions) {
            binding.tvOcrStatus.setText(R.string.receipt_ocr_no_details)
            binding.tvOcrSummary.visibility = View.GONE
            return
        }
        binding.tvOcrStatus.setText(R.string.receipt_ocr_ready)
        val missing = getString(R.string.receipt_ocr_not_found)
        val localData = LocalDataStore(this)
        binding.tvOcrSummary.text = getString(
            R.string.receipt_ocr_scan_summary,
            getString(if (ocrIsIncome) R.string.income else R.string.expense),
            result.merchant ?: missing,
            result.date ?: today(),
            result.total?.let { getString(R.string.money_amount, localData.currencySymbol, it) } ?: missing,
            ocrCategory?.let(localData::categoryDisplayName) ?: missing,
            result.items.takeIf { it.isNotEmpty() }?.joinToString(", ") ?: missing
        )
        binding.tvOcrSummary.visibility = View.VISIBLE
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

    private fun today(): String =
        SimpleDateFormat("yyyy-MM-dd", Locale.US).format(java.util.Date())

    companion object {
        const val EXTRA_IMAGE_PATH = "imagePath"
        const val EXTRA_OCR_MERCHANT = "ocrMerchant"
        const val EXTRA_OCR_DATE = "ocrDate"
        const val EXTRA_OCR_TOTAL = "ocrTotal"
        const val EXTRA_OCR_ITEMS = "ocrItems"
        const val EXTRA_OCR_CATEGORY = "ocrCategory"
        const val EXTRA_OCR_IS_INCOME = "ocrIsIncome"
        const val EXTRA_OCR_MODE = "ocrMode"
        const val EXTRA_AUTO_CAMERA = "autoCamera"
        private const val STATE_SELECTED_IMAGE = "selectedImage"
        private const val STATE_PENDING_CAMERA_IMAGE = "pendingCameraImage"
        private const val STATE_AUTO_CAMERA_LAUNCHED = "autoCameraLaunched"
    }
}
// End of class: AddImageActivity
