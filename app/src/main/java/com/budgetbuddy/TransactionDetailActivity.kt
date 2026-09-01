package com.budgetbuddy

import android.os.Bundle
import android.view.View
import com.bumptech.glide.Glide
import com.budgetbuddy.databinding.ActivityTransactionDetailBinding
import java.io.File

/*
 * Start of class
 * Name of class and related classes (parent/child classes): TransactionDetailActivity
 * Parent class: BaseActivity; child classes: none; related classes: Transaction, LocalDataStore, and TransactionActivity.
 * What the class does: Displays one transaction and offers edit or delete actions.
 * What's important to other classes, if applicable: It must preserve BaseActivity appearance behavior and use LocalDataStore as the offline source of truth.
 * Code with comments begins below.
 */
class TransactionDetailActivity : BaseActivity() {
    private lateinit var binding: ActivityTransactionDetailBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTransactionDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.btnBack.setOnClickListener { finish() }

        val localData = LocalDataStore(this)
        val transaction = intent.getStringExtra(EXTRA_TRANSACTION_ID)?.let(localData::getTransaction)
        if (transaction == null) {
            finish()
            return
        }
        binding.tvDetailType.setText(if (transaction.isIncome) R.string.income else R.string.expense)
        binding.tvDetailAmount.text = getString(
            if (transaction.isIncome) R.string.income_amount else R.string.expense_amount,
            localData.currencySymbol,
            transaction.amount
        )
        binding.tvDetailMerchant.text = getString(
            R.string.detail_description_value,
            transaction.note ?: getString(R.string.no_description)
        )
        binding.tvDetailDate.text = getString(R.string.ocr_date_value, transaction.date)
        binding.tvDetailCategory.text = getString(
            R.string.detail_category_value,
            localData.categoryDisplayName(transaction.categoryId)
        )
        binding.tvDetailOcr.visibility = if (transaction.isOcr) View.VISIBLE else View.GONE
        binding.tvDetailLimit.visibility = if (transaction.addsToSpendingLimit) View.VISIBLE else View.GONE
        val icon = localData.categoryIcon(transaction.categoryId)
        CategoryIconCatalog.bind(binding.imgDetailCategory, icon)
        val receipt = transaction.photoPath?.let(::File)?.takeIf { ReceiptStorage.isUsableOwnedReceipt(this, it) }
        binding.ivDetailReceipt.visibility = if (receipt == null) View.GONE else View.VISIBLE
        receipt?.let {
            Glide.with(this).load(it).fitCenter().into(binding.ivDetailReceipt)
        }
    }

    companion object {
        const val EXTRA_TRANSACTION_ID = "transactionId"
    }
}
// End of class: TransactionDetailActivity
