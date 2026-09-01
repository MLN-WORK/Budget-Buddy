package com.budgetbuddy

import android.app.Activity
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.Typeface
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.activity.addCallback
import androidx.core.graphics.ColorUtils
import androidx.core.view.ViewCompat
import androidx.recyclerview.widget.RecyclerView
import com.github.anastr.speedviewlib.SpeedView
import com.github.anastr.speedviewlib.components.Section
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.color.MaterialColors

/** A short guided tour displayed over the app's real screens. */
/*
 * Start of class
 * Name of class and related classes (parent/child classes): TutorialFlow
 * Parent class: Any; child classes: TutorialLineAdapter and RowHolder; related classes: BaseActivity, LocalDataStore, GaugePalette, and TutorialLineAdapter.
 * What the class does: Coordinates the guided, theme-aware walkthrough across application screens.
 * What's important to other classes, if applicable: Related classes depend on this class keeping its inputs validated and its output contract deterministic.
 * Code with comments begins below.
 */
object TutorialFlow {
    const val EXTRA_STEP = "budgetBuddyTutorialStep"
    private const val FIRST_APP_STEP = 1
    private const val LAST_APP_STEP = 6
    private const val OVERLAY_TAG = "budgetBuddyTutorialOverlay"
    private const val SELECTOR_TAG = "budgetBuddyTutorialStateSelector"

    fun attachIfNeeded(activity: BaseActivity) {
        val step = activity.intent.getIntExtra(EXTRA_STEP, -1)
        if (step !in FIRST_APP_STEP..LAST_APP_STEP) return
        val content = activity.findViewById<ViewGroup>(android.R.id.content)
        content.post {
            if (activity.isFinishing || activity.isDestroyed || content.findViewWithTag<View>(OVERLAY_TAG) != null) return@post
            applySimulatedData(activity, step)
            content.addView(createOverlay(activity, step))
        }
        activity.onBackPressedDispatcher.addCallback(activity) {
            if (step == FIRST_APP_STEP) finishTutorial(activity) else openStep(activity, step - 1)
        }
    }

    fun start(activity: Activity) = openStep(activity, FIRST_APP_STEP)

    private fun openStep(activity: Activity, step: Int) {
        if (step > LAST_APP_STEP) {
            finishTutorial(activity)
            return
        }
        val destination = when (step) {
            1 -> MainActivity::class.java
            2 -> BudgetActivity::class.java
            3 -> TransactionActivity::class.java
            4 -> AddImageActivity::class.java
            5 -> AnalyticsActivity::class.java
            else -> TransactionHistoryActivity::class.java
        }
        activity.startActivity(Intent(activity, destination).apply {
            if (step > 0) putExtra(EXTRA_STEP, step)
        })
        activity.finish()
        activity.overridePendingTransition(0, 0)
    }

    private fun finishTutorial(activity: Activity) {
        LocalDataStore(activity).completeTutorial()
        activity.startActivity(Intent(activity, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        })
    }

    private fun createOverlay(activity: BaseActivity, step: Int): View {
        val density = activity.resources.displayMetrics.density
        fun dp(value: Int) = (value * density).toInt()
        val data = LocalDataStore(activity)
        val primary = MaterialColors.getColor(activity.window.decorView, R.attr.budgetPrimaryColor)
        val onPrimary = MaterialColors.getColor(activity.window.decorView, R.attr.budgetOnPrimaryColor)
        val surface = MaterialColors.getColor(activity.window.decorView, R.attr.budgetSurfaceColor)
        val textColor = MaterialColors.getColor(activity.window.decorView, R.attr.budgetTextColor)
        val copy = tutorialCopy(activity, step, data)

        return FrameLayout(activity).apply {
            tag = OVERLAY_TAG
            importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_YES
            layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
            addView(MaterialCardView(activity).apply {
                radius = dp(18).toFloat()
                strokeWidth = dp(1)
                strokeColor = primary
                cardElevation = dp(10).toFloat()
                setCardBackgroundColor(surface)
                contentDescription = copy.first + ". " + copy.second
                addView(LinearLayout(activity).apply {
                    orientation = LinearLayout.VERTICAL
                    setPadding(dp(16), dp(14), dp(16), dp(12))
                    addView(LinearLayout(activity).apply {
                        orientation = LinearLayout.HORIZONTAL
                        gravity = Gravity.CENTER_VERTICAL
                        addView(ImageView(activity).apply {
                            setImageResource(buddyForStep(step))
                            scaleType = ImageView.ScaleType.FIT_CENTER
                            contentDescription = data.buddyName
                        }, LinearLayout.LayoutParams(dp(64), dp(64)))
                        addView(LinearLayout(activity).apply {
                            orientation = LinearLayout.VERTICAL
                            setPadding(dp(12), 0, 0, 0)
                            addView(TextView(activity).apply {
                                this.text = copy.first
                                setTextColor(primary)
                                textSize = 20f
                                typeface = Typeface.DEFAULT_BOLD
                            })
                            addView(TextView(activity).apply {
                                this.text = copy.second
                                setTextColor(textColor)
                                textSize = 15f
                                setLineSpacing(0f, 1.05f)
                            })
                        }, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
                    })
                    addView(LinearLayout(activity).apply {
                        orientation = LinearLayout.HORIZONTAL
                        gravity = Gravity.END or Gravity.CENTER_VERTICAL
                        addView(tutorialButton(activity, R.string.skip_tutorial, primary, onPrimary, false) {
                            finishTutorial(activity)
                        })
                        addView(View(activity), LinearLayout.LayoutParams(0, 1, 1f))
                        if (step > FIRST_APP_STEP) {
                            addView(tutorialButton(activity, R.string.back, primary, onPrimary, false) {
                                openStep(activity, step - 1)
                            }, LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, dp(44)).apply {
                                marginEnd = dp(8)
                            })
                        }
                        addView(tutorialButton(
                            activity,
                            if (step == LAST_APP_STEP) R.string.finish_tutorial else R.string.next,
                            primary,
                            onPrimary,
                            true
                        ) { openStep(activity, step + 1) })
                    }, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(44)).apply {
                        topMargin = dp(10)
                    })
                })
            }, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.BOTTOM).apply {
                marginStart = dp(14)
                marginEnd = dp(14)
                bottomMargin = if (activity.findViewById<View>(R.id.bottomNavView) != null) dp(94) else dp(18)
            })
            ViewCompat.setAccessibilityPaneTitle(this, copy.first)
        }
    }

    private fun tutorialButton(
        activity: Activity,
        label: Int,
        primary: Int,
        onPrimary: Int,
        filled: Boolean,
        action: () -> Unit
    ) = MaterialButton(activity).apply {
        setText(label)
        textSize = 14f
        minWidth = 0
        minHeight = 0
        insetTop = 0
        insetBottom = 0
        isAllCaps = false
        backgroundTintList = ColorStateList.valueOf(if (filled) primary else Color.TRANSPARENT)
        setTextColor(if (filled) onPrimary else primary)
        if (!filled) {
            strokeColor = ColorStateList.valueOf(primary)
            strokeWidth = activity.resources.displayMetrics.density.toInt().coerceAtLeast(1)
        }
        setOnClickListener { action() }
    }

    private fun applySimulatedData(activity: BaseActivity, step: Int) {
        val data = LocalDataStore(activity)
        when (step) {
            1 -> {
                installStateSelector(activity, R.id.pageContentsLayout) { state -> applyHomeState(activity, state) }
                applyHomeState(activity, 0)
                activity.findViewById<RecyclerView>(R.id.rvRecords)?.adapter = TutorialLineAdapter(
                    activity,
                    listOf(
                        activity.getString(R.string.income) to "+ ${data.currencySymbol}1,200.00",
                        activity.getString(R.string.tutorial_demo_groceries) to "− ${data.currencySymbol}180.00",
                        activity.getString(R.string.tutorial_demo_food) to "− ${data.currencySymbol}120.00"
                    )
                )
            }
            2 -> {
                activity.findViewById<android.widget.EditText>(R.id.edtMaximumBudget)
                    ?.setText(activity.getString(R.string.plain_decimal_amount, 1000.0))
                activity.findViewById<TextView>(R.id.tvTotalBudgeted)?.text = activity.getString(R.string.plain_decimal_amount, 650.0)
                activity.findViewById<TextView>(R.id.tvUnallocatedPercentage)?.text = activity.getString(R.string.budget_unallocated_percentage, 35.0)
                activity.findViewById<View>(R.id.fabSaveBudget)?.apply { isEnabled = false; alpha = 0.55f }
                activity.findViewById<RecyclerView>(R.id.rvBudgetCategories)?.adapter = TutorialLineAdapter(
                    activity,
                    listOf(
                        activity.getString(R.string.tutorial_demo_housing) to activity.getString(R.string.tutorial_demo_amount, data.currencySymbol, 400.0),
                        activity.getString(R.string.tutorial_demo_food) to activity.getString(R.string.tutorial_demo_amount, data.currencySymbol, 250.0)
                    )
                )
            }
            3 -> {
                activity.findViewById<android.widget.EditText>(R.id.etAmount)?.setText(activity.getString(R.string.plain_decimal_amount, 120.0))
                activity.findViewById<android.widget.EditText>(R.id.etDescription)?.setText(activity.getString(R.string.tutorial_demo_groceries))
                activity.findViewById<View>(R.id.btnSave)?.apply { isEnabled = false; alpha = 0.55f }
            }
            4 -> activity.findViewById<View>(R.id.btnSaveImg)?.apply {
                isEnabled = false
                alpha = 0.55f
            }
            5 -> {
                installStateSelector(activity, R.id.scrollableContent) { state -> applyAnalyticsState(activity, state) }
                applyAnalyticsState(activity, 0)
            }
            6 -> activity.findViewById<RecyclerView>(R.id.rvTransactions)?.adapter = TutorialLineAdapter(
                activity,
                listOf(
                    activity.getString(R.string.income) to "+ ${data.currencySymbol}500.00",
                    activity.getString(R.string.tutorial_demo_groceries) to "− ${data.currencySymbol}85.00",
                    activity.getString(R.string.tutorial_demo_rent) to "− ${data.currencySymbol}300.00",
                    activity.getString(R.string.tutorial_demo_food) to "− ${data.currencySymbol}42.80"
                )
            )
        }
    }

    private fun installStateSelector(activity: BaseActivity, parentId: Int, onState: (Int) -> Unit) {
        val parent = activity.findViewById<ViewGroup>(parentId) ?: return
        if (parent.findViewWithTag<View>(SELECTOR_TAG) != null) return
        val density = activity.resources.displayMetrics.density
        fun dp(value: Int) = (value * density).toInt()
        val primary = MaterialColors.getColor(parent, R.attr.budgetPrimaryColor)
        val onPrimary = MaterialColors.getColor(parent, R.attr.budgetOnPrimaryColor)
        val surface = MaterialColors.getColor(parent, R.attr.budgetSurfaceColor)
        val selector = MaterialCardView(activity).apply {
            tag = SELECTOR_TAG
            radius = dp(16).toFloat()
            setCardBackgroundColor(surface)
            strokeWidth = dp(2)
            strokeColor = primary
            addView(LinearLayout(activity).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(dp(12), dp(9), dp(12), dp(10))
                addView(TextView(activity).apply {
                    setText(R.string.tutorial_try_three_states)
                    setTextColor(MaterialColors.getColor(parent, R.attr.budgetTextColor))
                    textSize = 16f
                    gravity = Gravity.CENTER
                    typeface = Typeface.DEFAULT_BOLD
                })
                addView(LinearLayout(activity).apply {
                    orientation = LinearLayout.HORIZONTAL
                    listOf(R.string.tutorial_state_good, R.string.tutorial_state_neutral, R.string.tutorial_state_bad).forEachIndexed { index, label ->
                        addView(MaterialButton(activity).apply {
                            setText(label)
                            textSize = 14f
                            minWidth = 0
                            insetTop = 0
                            insetBottom = 0
                            backgroundTintList = ColorStateList.valueOf(primary)
                            setTextColor(onPrimary)
                            setOnClickListener { onState(index) }
                        }, LinearLayout.LayoutParams(0, dp(44), 1f).apply { if (index > 0) marginStart = dp(6) })
                    }
                }, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply { topMargin = dp(6) })
            })
        }
        parent.addView(selector, 0, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
            bottomMargin = dp(10)
        })
    }

    private fun applyHomeState(activity: BaseActivity, state: Int) {
        val data = LocalDataStore(activity)
        val symbol = data.currencySymbol
        val palette = data.gaugePalette
        val surface = MaterialColors.getColor(activity.window.decorView, R.attr.budgetSurfaceColor)
        val (spent, percentage, buddy) = when (state) {
            1 -> Triple(680.0, 68, R.drawable.happy_buddy)
            2 -> Triple(1080.0, 108, R.drawable.angry_buddy)
            else -> Triple(300.0, 30, R.drawable.neutral_buddy_1)
        }
        val income = 1200.0
        val remaining = 1000.0 - spent
        val dynamic = readableColor(statusColor(palette, percentage), surface)
        val good = readableColor(palette.good, surface)
        val bad = readableColor(palette.bad, surface)
        activity.findViewById<TextView>(R.id.tvBalanceAmount)?.apply {
            text = activity.getString(R.string.money_amount, symbol, income - spent)
            setTextColor(if (income - spent >= 0) good else bad)
        }
        activity.findViewById<TextView>(R.id.tvIncomeAmount)?.apply { text = activity.getString(R.string.money_amount, symbol, income); setTextColor(good) }
        activity.findViewById<TextView>(R.id.tvExpensesAmount)?.apply { text = activity.getString(R.string.money_amount, symbol, spent); setTextColor(bad) }
        activity.findViewById<TextView>(R.id.tvBudgetLimit)?.text = activity.getString(R.string.spending_limit_amount, symbol, 1000.0)
        activity.findViewById<TextView>(R.id.tvBudgetSpent)?.apply { text = activity.getString(R.string.spent_amount, symbol, spent); setTextColor(bad) }
        activity.findViewById<TextView>(R.id.tvBudgetIncome)?.apply { text = activity.getString(R.string.income_amount_label, symbol, income); setTextColor(good) }
        activity.findViewById<TextView>(R.id.tvBudgetRemaining)?.apply {
            text = if (remaining >= 0) activity.getString(R.string.remaining_amount, symbol, remaining)
            else activity.getString(R.string.over_limit_amount, symbol, -remaining)
            setTextColor(dynamic)
        }
        activity.findViewById<TextView>(R.id.tvBudgetUsage)?.apply {
            text = activity.getString(R.string.budget_remaining_percentage, (100 - percentage).coerceAtLeast(0).toDouble())
            setTextColor(dynamic)
        }
        activity.findViewById<ProgressBar>(R.id.pgBudgetBar)?.apply {
            progress = percentage.coerceAtMost(100)
            progressTintList = ColorStateList.valueOf(dynamic)
        }
        activity.findViewById<ImageView>(R.id.imgBuddy)?.setImageResource(buddy)
    }

    private fun applyAnalyticsState(activity: BaseActivity, state: Int) {
        val data = LocalDataStore(activity)
        val palette = data.gaugePalette
        val percentage = when (state) { 1 -> 68; 2 -> 92; else -> 30 }
        val gauge = activity.findViewById<SpeedView>(R.id.minMaxGauge)
        gauge?.clearSections()
        gauge?.addSections(
            Section(0f, 0.5f, palette.good, gauge.speedometerWidth),
            Section(0.5f, 0.85f, palette.okay, gauge.speedometerWidth),
            Section(0.85f, 1f, palette.bad, gauge.speedometerWidth)
        )
        gauge?.speedTo(percentage.toFloat(), 500)
        activity.findViewById<TextView>(R.id.btnMinMaxHeader)?.text = activity.getString(
            R.string.tutorial_demo_analytics_header_state,
            percentage.toDouble(),
            data.currencySymbol
        )
        activity.findViewById<TextView>(R.id.tvPercentage)?.text = activity.getString(R.string.percentage_two_decimals, percentage.toDouble())
        activity.findViewById<ImageView>(R.id.imgBeetleJuice)?.setImageResource(
            when (state) { 1 -> R.drawable.happy_buddy; 2 -> R.drawable.angry_buddy; else -> R.drawable.neutral_buddy_1 }
        )
    }

    private fun statusColor(palette: GaugePalette, percentage: Int) = when {
        percentage < 50 -> palette.good
        percentage < 85 -> palette.okay
        else -> palette.bad
    }

    private fun readableColor(color: Int, surface: Int): Int {
        if (ColorUtils.calculateContrast(color, surface) >= 4.5) return color
        val target = if (ColorUtils.calculateLuminance(surface) < 0.45) Color.WHITE else Color.BLACK
        for (step in 1..10) {
            val candidate = ColorUtils.blendARGB(color, target, step / 10f)
            if (ColorUtils.calculateContrast(candidate, surface) >= 4.5) return candidate
        }
        return target
    }

    private fun buddyForStep(step: Int) = when (step) {
        1, 2 -> R.drawable.neutral_buddy_1
        5 -> R.drawable.happy_buddy
        else -> R.drawable.main_buddy
    }

    /*
     * Start of class
     * Name of class and related classes (parent/child classes): TutorialLineAdapter
     * Parent class: RecyclerView.Adapter; child classes: RowHolder; related classes: TutorialFlow and RowHolder.
     * What the class does: Binds tutorial example label/value rows to a RecyclerView.
     * What's important to other classes, if applicable: Callers supply its model data and depend on stable row binding and click-callback behavior.
     * Code with comments begins below.
     */
    private class TutorialLineAdapter(private val activity: Activity, private val rows: List<Pair<String, String>>) :
        RecyclerView.Adapter<TutorialLineAdapter.RowHolder>() {
        /*
         * Start of class
         * Name of class and related classes (parent/child classes): RowHolder
         * Parent class: RecyclerView.ViewHolder; child classes: none; related classes: TutorialLineAdapter.
         * What the class does: Caches the label and value views for one tutorial example row.
         * What's important to other classes, if applicable: Its enclosing adapter owns it; it must not retain activity state beyond the bound row.
         * Code with comments begins below.
         */
        class RowHolder(val row: LinearLayout, val title: TextView, val value: TextView) : RecyclerView.ViewHolder(row)
        // End of class: RowHolder
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RowHolder {
            val density = activity.resources.displayMetrics.density
            val textColor = MaterialColors.getColor(parent, R.attr.budgetTextColor)
            val row = LinearLayout(activity).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setPadding((20 * density).toInt(), (15 * density).toInt(), (20 * density).toInt(), (15 * density).toInt())
            }
            val title = TextView(activity).apply { setTextColor(textColor); textSize = 18f; typeface = Typeface.DEFAULT_BOLD }
            val value = TextView(activity).apply { setTextColor(textColor); textSize = 18f; typeface = Typeface.DEFAULT_BOLD; gravity = Gravity.END }
            row.addView(title, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
            row.addView(value, LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT))
            return RowHolder(row, title, value)
        }
        override fun onBindViewHolder(holder: RowHolder, position: Int) {
            holder.title.text = rows[position].first
            holder.value.text = rows[position].second
        }
        override fun getItemCount() = rows.size
    }
    // End of class: TutorialLineAdapter

    private fun tutorialCopy(activity: Activity, step: Int, data: LocalDataStore): Pair<String, String> {
        val title = when (step) {
            1 -> R.string.tutorial_home_title
            2 -> R.string.tutorial_monthly_budget_title
            3 -> R.string.tutorial_transaction_title
            4 -> R.string.tutorial_receipt_title
            5 -> R.string.tutorial_analytics_title
            else -> R.string.tutorial_records_title
        }
        val message = when (step) {
            1 -> R.string.tutorial_home_coach
            2 -> R.string.tutorial_budget_coach
            3 -> R.string.tutorial_transaction_coach
            4 -> R.string.tutorial_receipt_coach
            5 -> R.string.tutorial_analytics_coach
            else -> R.string.tutorial_records_coach
        }
        return activity.getString(title) to activity.getString(message, data.buddyName, data.displayName)
    }
}
// End of class: TutorialFlow
