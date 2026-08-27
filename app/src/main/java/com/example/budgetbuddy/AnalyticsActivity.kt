package com.example.budgetbuddy

import android.annotation.SuppressLint
import android.app.DatePickerDialog
import android.icu.util.Calendar
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import com.example.budgetbuddy.databinding.ActivityAnalyticsBinding
import com.google.android.material.color.MaterialColors
import com.github.anastr.speedviewlib.components.Section
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import java.text.SimpleDateFormat
import java.util.Locale


class AnalyticsActivity : BaseActivity() {
    private lateinit var binding: ActivityAnalyticsBinding
    private var startDate = ""
    private var endDate = ""
    private var isExpanded = false
    private var currentMonth: Calendar = Calendar.getInstance()
    private val formattedMonth: String
        get() = SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(currentMonth.time)
    private lateinit var repo: TransactionRepo
    private lateinit var localData: LocalDataStore
    private var showMaxInfo = false
    private var firstResume = true


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAnalyticsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        repo = TransactionRepo(this)
        localData = LocalDataStore(this)
        displayCurrency()
        binding.imgBeetleJuice.contentDescription = localData.buddyName
        setupGaugeChart(formattedMonth)
        loadBarGraphData(formattedMonth, showMaxInfo)

        updateMonthDisplay()
        setupExpandableAnalytics()

        binding.btnShowMaxInfo.setOnClickListener{
            showMaxInfo = !showMaxInfo
            if (showMaxInfo) binding.btnShowMaxInfo.text = getString(R.string.reset) else binding.btnShowMaxInfo.text = getString(R.string.show_on_graph)
            loadBarGraphData(binding.tvCurrentMonth.text.toString(), showMaxInfo)
        }
        binding.btnBack.setOnClickListener{
            clearText()
            binding.categoryBarGraph.clear()
            currentMonth.add(Calendar.MONTH, -1) //prev month
            loadAnalyticsView()
        }
        binding.btnForward.setOnClickListener{
            clearText()
            binding.categoryBarGraph.clear()
            currentMonth.add(Calendar.MONTH, 1) //next month
            loadAnalyticsView()
        }

        setupDatePickers()
        //navigation
        appNavigationSetup()
    }//end oncreate

    override fun onResume() {
        super.onResume()
        if (firstResume) {
            firstResume = false
        } else if (::localData.isInitialized) {
            loadAnalyticsView()
        }
    }

    private fun appNavigationSetup() {
        AppNavigation.bind(this, binding.bottomNavView, R.id.nav_analytics)
    }

    private fun updateMonthDisplay() {
        val monthFormat = SimpleDateFormat("MMMM yyyy", Locale.getDefault())
        val formattedMonth = monthFormat.format(currentMonth.time)
        binding.tvCurrentMonth.text = formattedMonth
    }

    private fun setupDatePickers() {
        val fmt = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val today = java.util.Calendar.getInstance()

        binding.tvStartDate.setOnClickListener {
            DatePickerDialog(
                this,
                { _, year, month, day ->
                    startDate = fmt.format(java.util.Calendar.getInstance().apply {
                        set(year, month, day)
                    }.time)
                    binding.tvStartDate.text = startDate
                    checkAndReloadGraph()
                },
                today.get(java.util.Calendar.YEAR),
                today.get(java.util.Calendar.MONTH),
                today.get(java.util.Calendar.DAY_OF_MONTH)
            ).show()
        }

        binding.tvEndDate.setOnClickListener {
            DatePickerDialog(
                this,
                { _, year, month, day ->
                    endDate = fmt.format(java.util.Calendar.getInstance().apply {
                        set(year, month, day)
                    }.time)
                    binding.tvEndDate.text = endDate
                    checkAndReloadGraph()
                },
                today.get(java.util.Calendar.YEAR),
                today.get(java.util.Calendar.MONTH),
                today.get(java.util.Calendar.DAY_OF_MONTH)
            ).show()
        }
    }

    private fun clearDatePickers(){
        binding.tvStartDate.setText(getString(R.string.start_date))
        binding.tvEndDate.setText(getString(R.string.end_date))
        startDate = ""
        endDate = ""
    }

    private fun setupExpandableAnalytics(){
        //expand
        binding.btnMinMaxHeader.setOnClickListener{
            isExpanded = !isExpanded
            binding.hiddenAnalytics.visibility = if (isExpanded) View.VISIBLE else View.GONE
            binding.btnMinMaxHeader.setCompoundDrawablesWithIntrinsicBounds(
                0, 0,
                if (isExpanded) R.drawable.ic_upward_arrow else R.drawable.ic_downward_arrow,
                0
            )
        }
    }

    private fun clearText(){
        binding.tvFeedBack.text = ""
        binding.tvPercentage.text= ""
        binding.tvMinGoal.text = ""
        binding.tvMaxGoal.text = ""
    }

    private fun loadAnalyticsView(){
        displayCurrency()
        updateMonthDisplay()//show month
        clearDatePickers()
        checkAndReloadGraph()
        setupGaugeChart(binding.tvCurrentMonth.text.toString())
        loadBarGraphData(binding.tvCurrentMonth.text.toString(), showMaxInfo)
    }

    private fun loadBarGraphData(selectedMonth:String, showMaxInfo: Boolean){
        //convert display month to start and end date of month
        val displayedFormat = SimpleDateFormat("MMMM yyyy", Locale.getDefault())
        val outputFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

        val parsedMonth = runCatching { displayedFormat.parse(selectedMonth) }.getOrNull()
        if (parsedMonth == null) {
            setupBarGraph(emptyList(), emptyList())
            return
        }
        val calendar = Calendar.getInstance()
        calendar.time = parsedMonth
        //first day of month
        calendar.set(Calendar.DAY_OF_MONTH, 1)
        val firstDay = outputFormat.format(calendar.time)
        //last day of month
        calendar.set(Calendar.DAY_OF_MONTH, calendar.getActualMaximum(Calendar.DAY_OF_MONTH))
        val lastDay = outputFormat.format(calendar.time)

        val budgetCategories = localData.getBudget(selectedMonth)?.categories.orEmpty()
            .mapValues { (_, category) -> Pair(category.allocation.toFloat(), (category.amountSpent ?: 0.0).toFloat()) }
        val onTransactionFetched: (List<Transaction>) -> Unit = { transactions ->
            val totals = transactions.filterNot(Transaction::isIncome)
                .groupBy(Transaction::categoryId)
                .mapValues { (_, values) -> values.sumOf(Transaction::amount) }
            val allCategories = (totals.keys + budgetCategories.keys).distinct()
            val stackedValues = allCategories.map { category ->
                val spent = totals[category]?.toFloat() ?: 0f
                val allocation = budgetCategories[category]?.first ?: 0f
                floatArrayOf(spent, maxOf(0f, allocation - spent))
            }
            if (showMaxInfo) {
                setupStackedBarGraph(allCategories, stackedValues)
            } else {
                setupBarGraph(totals.keys.toList(), totals.values.map(Double::toFloat))
            }
        }
        val onError: (Exception) -> Unit = { Log.e("BarGraph", "Error loading local transactions", it) }
        if (startDate.isNotBlank() && endDate.isNotBlank()) {
            repo.fetchInRange(startDate, endDate, onTransactionFetched, onError)
        } else {
            repo.fetchInRange(firstDay, lastDay, onTransactionFetched, onError)
        }
    }//end load bar graph data

    /*This code uses MPAndroidChart (https://github.com/PhilJay/MPAndroidChart)
   * Licensed under the Apache License, Version 2.0
   * Copyright 2015 Philipp Jahoda*/
    //shows just plain category expenditure
    private fun setupBarGraph(categories: List<String>, amounts: List<Float>){
        val barGraph = binding.categoryBarGraph
        barGraph.clear()
        val entries = ArrayList<BarEntry>()
        for(i in amounts.indices){
            entries.add(BarEntry(i.toFloat(), amounts[i]))
        }
        val dataSet = BarDataSet(entries, "Amount Spent")

        val colours = getBarColours()
        val barColours = ArrayList<Int>()
        for(i in entries.indices){
            barColours.add(colours[i % colours.size])
        }
        dataSet.colors = barColours
        dataSet.setDrawValues(true)

        val barData = BarData(dataSet)
        barData.barWidth = 0.5f
        barGraph.data = barData
        barGraph.setFitBars(false)
        barGraph.fitsSystemWindows
        barGraph.animateY(1000)

        //x-axis -- values
        val xAxis = binding.categoryBarGraph.xAxis
        xAxis.position = XAxis.XAxisPosition.BOTTOM
        xAxis.setDrawGridLines(false)
        xAxis.textSize = 15f
        xAxis.textColor = MaterialColors.getColor(binding.root, R.attr.budgetTextColor)
        xAxis.typeface = ResourcesCompat.getFont(applicationContext, R.font.outfit_regular)
        xAxis.granularity = 1f
        xAxis.valueFormatter = IndexAxisValueFormatter(categories)

        //y axis -- category labels
        val yAxis = binding.categoryBarGraph.axisLeft
        yAxis.setDrawGridLines(false)
        yAxis.textSize = 15f
        yAxis.textColor = MaterialColors.getColor(binding.root, R.attr.budgetTextColor)
        yAxis.typeface = ResourcesCompat.getFont(applicationContext, R.font.outfit_regular)
        yAxis.axisMinimum = 0f
        yAxis.granularity = 1f

        //disable right y-axis
        barGraph.axisRight.isEnabled = false
        barGraph.legend.isEnabled = false
        barGraph.description.isEnabled = false

        barGraph.isDragEnabled = true
        barGraph.setScaleEnabled(true)
        barGraph.setTouchEnabled(true)
        barGraph.isFullyZoomedOut




        //refresh
        barGraph.invalidate()
    }

    /*This code uses MPAndroidChart (https://github.com/PhilJay/MPAndroidChart)
  * Licensed under the Apache License, Version 2.0
  * Copyright 2015 Philipp Jahoda*/
    //shows both budget maximum, and plain transactions
    private fun setupStackedBarGraph(categories: List<String>, stackedValues: List<FloatArray>){
        val barGraph = binding.categoryBarGraph
        barGraph.clear()

        val entries = ArrayList<BarEntry>()
        for (i in stackedValues.indices) {
            entries.add(BarEntry(i.toFloat(), stackedValues[i]))
        }

        val dataSet = BarDataSet(entries, "[Spending & Budget Usage]")
        dataSet.setColors(
            MaterialColors.getColor(binding.root, R.attr.budgetPrimaryColor),
            ContextCompat.getColor(this, R.color.lightPink)
        ) // spent, remaining
        dataSet.setDrawValues(true)
        dataSet.stackLabels = arrayOf("Spent", "Remaining to Max")

        val barData = BarData(dataSet)
        barData.barWidth = 0.5f

        barGraph.data = barData
        barGraph.setFitBars(true)
        barGraph.animateY(1000)

        val xAxis = barGraph.xAxis
        xAxis.position = XAxis.XAxisPosition.BOTTOM
        xAxis.setDrawGridLines(false)
        xAxis.valueFormatter = IndexAxisValueFormatter(categories)
        xAxis.granularity = 1f
        xAxis.labelRotationAngle = -30f
        xAxis.textSize = 12f

        val yAxis = barGraph.axisLeft
        yAxis.setDrawGridLines(false)
        yAxis.axisMinimum = 0f

        barGraph.axisRight.isEnabled = false
        barGraph.legend.isEnabled = true
        barGraph.description.isEnabled = false

        barGraph.invalidate()
    }

    private fun getBarColours(): List<Int>{
        //set up colours
        val typedArray = resources.obtainTypedArray(R.array.bar_colours)
        val colourList = mutableListOf<Int>()
        for(i in 0 until typedArray.length()){
            colourList.add(
                typedArray.getColor(
                    i,
                    MaterialColors.getColor(binding.root, R.attr.budgetTextColor)
                )
            )
        }
        typedArray.recycle()
        return colourList
    }

    //helper function to check if date range was selected
    private fun checkAndReloadGraph(){
        if(startDate.isNotBlank() && endDate.isNotBlank()){
            val displayedMonth = binding.tvCurrentMonth.text.toString()
            loadBarGraphData(displayedMonth, showMaxInfo)
        }
    }

    /*Gauge chart visualisation by SpeedView (https://github.com/anastr/SpeedView)
    * © 2017 Anas Altair – Licensed under Apache 2.0*/
    private fun setupGaugeChart(month:String){
        val gauge = binding.minMaxGauge
        gauge.clearSections()
        /*percentage spent = amount spent / max Goal * 100
        minGoal = min goal from budget
        max goal = amount spent from budget */
        val budget = localData.getBudget(month)
        @SuppressLint("SetTextI18n")
        if (budget == null) {
                    gauge.clearSections()
                    gauge.addSections(
                        Section(
                            0f,
                            1f,
                            MaterialColors.getColor(binding.root, R.attr.budgetBackgroundColor),
                            gauge.speedometerWidth
                        )
                    )
                    gauge.speedTo(0f, 1000)
                    binding.btnMinMaxHeader.setText(getString(R.string.youSpent, 0.0))
                    binding.tvMinGoal.setText(R.string.no_money)
                    binding.tvMaxGoal.setText(R.string.no_money)
                    binding.imgBeetleJuice.setImageResource(R.drawable.neutral_buddy_1)
                    binding.tvFeedBack.setText(R.string.analytics_no_budget_mood)
        } else {
                    val maximumGoal = budget.maximumSpendingBudget
                    Log.d("GetMaxGoal", "Max goal is $maximumGoal")

                    val categoryTotal = budget.budgetAmount
                    val totalSpent = displayMonthToKey(month)
                        ?.let(localData::getBalance)
                        ?.totalExpenses
                        ?: 0.0
                    val percentageSpent = AnalyticsCalculator.spentPercentage(totalSpent, maximumGoal)

                    val allocationPercentage = BudgetLimitCalculator.allocationPercentage(categoryTotal, maximumGoal)

                    // Spending 0–50% leaves at least half the budget, 50–85% leaves
                    // 15–49%, and anything beyond 85% leaves less than 15%.
                    gauge.addSections(
                        Section(0f, 0.5f, ContextCompat.getColor(applicationContext, R.color.lightTeal), gauge.speedometerWidth),
                        Section(0.5f, 0.85f, ContextCompat.getColor(applicationContext, R.color.lightPink), gauge.speedometerWidth),
                        Section(0.85f, 1f, ContextCompat.getColor(applicationContext, R.color.cherry), gauge.speedometerWidth)
                    )

                    // Budster reacts to the percentage of the monthly budget still remaining.
                    val buddyPic = binding.imgBeetleJuice
                    when (AnalyticsCalculator.buddyMood(totalSpent, maximumGoal)) {
                        AnalyticsCalculator.BuddyMood.HAPPY -> {
                            buddyPic.setImageResource(R.drawable.happy_buddy)
                            binding.tvFeedBack.setText(R.string.budget_mood_happy)
                        }
                        AnalyticsCalculator.BuddyMood.NEUTRAL -> {
                            buddyPic.setImageResource(R.drawable.neutral_buddy_1)
                            binding.tvFeedBack.setText(R.string.budget_mood_neutral)
                        }
                        AnalyticsCalculator.BuddyMood.ANGRY -> {
                            buddyPic.setImageResource(R.drawable.angry_buddy)
                            binding.tvFeedBack.setText(R.string.budget_mood_angry)
                        }
                    }
                    //set gauge value
                    gauge.speedTo(AnalyticsCalculator.gaugeSpeed(percentageSpent), 1000)
                    //to display information to user in view
                    binding.btnMinMaxHeader.setText(getString(R.string.youSpent, percentageSpent.toFloat()))
                    binding.tvMinGoal.text = getString(R.string.plain_decimal_amount, categoryTotal)
                    binding.tvMaxGoal.text = getString(R.string.plain_decimal_amount, maximumGoal)
                    binding.tvPercentage.text = getString(R.string.category_budget_percentage, allocationPercentage)
        }

    }//end setupGaugeChart

    //fetching and displaying currency
    private fun displayCurrency() {
        binding.tvCurrency1.text = localData.currencySymbol
        binding.tvCurrency2.text = localData.currencySymbol
    }

    private fun displayMonthToKey(displayMonth: String): String? = runCatching {
        val input = SimpleDateFormat("MMMM yyyy", Locale.getDefault()).apply { isLenient = false }
        val output = SimpleDateFormat("yyyy-MM", Locale.US)
        output.format(requireNotNull(input.parse(displayMonth)))
    }.getOrNull()
}

/* Gauge sections mirror Budster's mood thresholds based on monthly budget remaining. */
