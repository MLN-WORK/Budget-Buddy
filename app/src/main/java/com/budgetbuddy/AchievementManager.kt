package com.budgetbuddy

import android.app.AlertDialog
import android.content.Context
import android.view.LayoutInflater
import android.widget.ImageView
import android.widget.TextView

object AchievementManager {

    val achievements = mutableListOf(
        Achievement(
            achievementId = "first_budget",
            title = "First Budget",
            description = "Congratulations! You set your very first budget. You're officially on the money path!",
            badgeResId = R.drawable.dartboard_badge
        ),
        Achievement(
            achievementId = "first_transaction",
            title = "First Transaction",
            description = "You added your first transaction. Every money move counts! you're tracking like a pro!",
            badgeResId = R.drawable.confetti_badge
        ),
        Achievement(
            achievementId = "first_category",
            title = "First Category",
            description = "Custom category created! You’re organizing your finances like a true boss.",
            badgeResId = R.drawable.stock_badge
        ),
        Achievement(
            achievementId = "monthly_budget_once",
            title = "Monthly Budgeter",
            description = "You built a budget this month. One small step for budget tracking, " +
                    " One giant leap for your financial health! (said by Neil Armstrong at some point. probably) ",
            badgeResId = R.drawable.calendar_badge,
            isRecurring = true,
            target = 1
        ),
        Achievement(
            achievementId = "monthly_budget_streak",
            title = "Budget Streak!",
            description = "3 months, 3 budgets. You're officially on a budgeting streak! ",
            badgeResId = R.drawable.safe_box_badge,
            isRecurring = true,
            target = 3
        ),
        Achievement(
            achievementId = "stay_within_budget",
            title = "On Budget",
            description = "You stayed within budget this month. That’s some top-tier self-control! " +
                    "Looks like your buddy is in high spirits!",
            badgeResId = R.drawable.trophy_badge,
            isRecurring = true,
            target = 1
        )
    )

    fun unlockAchievement(achievementId: String, context: Context) {
        restore(context)
        val achievement = achievements.find { it.achievementId == achievementId && !it.isCompleted }
        if (achievement != null) {
            achievement.isCompleted = true
            AchievementUtils.showPopup(context, achievement)
            LocalDataStore(context).saveAchievement(achievementId, true, achievement.progress)
        }
    }

    fun unlockOrProgress(
        achievementId: String,
        context: Context,
        increment: Int = 1
    ) {
        restore(context)
        val ach = achievements.find { it.achievementId == achievementId } ?: return

        if (ach.isCompleted && !ach.isRecurring) return

        if (ach.isRecurring) {
            ach.progress += increment

            if (ach.progress >= ach.target && !ach.isCompleted) {
                ach.isCompleted = true
                AchievementUtils.showPopup(context, ach)
            }

            LocalDataStore(context).saveAchievement(achievementId, ach.isCompleted, ach.progress)
        } else {
            if (!ach.isCompleted) {
                ach.isCompleted = true
                AchievementUtils.showPopup(context, ach)

                LocalDataStore(context).saveAchievement(achievementId, true, ach.progress)
            }
        }
    }

    fun recordBudgetForMonth(displayMonth: String, context: Context) {
        val localData = LocalDataStore(context)
        val months = localData.recordBudgetMonth(displayMonth)
        saveExactProgress("monthly_budget_once", months.size.coerceAtMost(1), context)
        val streak = AchievementProgressCalculator.longestConsecutiveMonthStreak(months)
        saveExactProgress("monthly_budget_streak", streak, context)
    }

    private fun saveExactProgress(achievementId: String, progress: Int, context: Context) {
        restore(context)
        val achievement = achievements.find { it.achievementId == achievementId } ?: return
        val wasCompleted = achievement.isCompleted
        achievement.progress = progress
        achievement.isCompleted = progress >= achievement.target
        LocalDataStore(context).saveAchievement(achievementId, achievement.isCompleted, progress)
        if (!wasCompleted && achievement.isCompleted) AchievementUtils.showPopup(context, achievement)
    }
    fun checkStayWithinBudgetForMonth(userId: String, monthKey: String, context: Context) {
        val localData = LocalDataStore(context)
        val month = runCatching {
            val input = java.text.SimpleDateFormat("yyyy-MM", java.util.Locale.US)
            val output = java.text.SimpleDateFormat("MMMM yyyy", java.util.Locale.getDefault())
            output.format(requireNotNull(input.parse(monthKey)))
        }.getOrNull() ?: return
        val budget = localData.getBudget(month) ?: return
        val spent = localData.getTransactions("$monthKey-01", "$monthKey-31")
            .filterNot(Transaction::isIncome)
            .sumOf(Transaction::amount)
        if (spent <= budget.maximumSpendingBudget) unlockOrProgress("stay_within_budget", context)
    }

    fun restore(context: Context) {
        val localData = LocalDataStore(context)
        achievements.forEach {
            it.isCompleted = localData.isAchievementCompleted(it.achievementId)
            it.progress = localData.achievementProgress(it.achievementId)
        }
    }
}

// Move this OUTSIDE of AchievementManager
object AchievementUtils {
    fun showPopup(context: Context, achievement: Achievement) {
        val dialogView =
            LayoutInflater.from(context).inflate(R.layout.dialog_achievement_popup, null)
        RuntimePaletteApplier.applyIfCustom(dialogView)

        val icon = dialogView.findViewById<ImageView>(R.id.popupIcon)
        val title = dialogView.findViewById<TextView>(R.id.popupTitle)
        val message = dialogView.findViewById<TextView>(R.id.popupMessage)
        val closeBtn = dialogView.findViewById<ImageView>(R.id.btnDismiss)


        // Populate the views
        icon.setImageResource(achievement.badgeResId)
        title.text = achievement.title
        message.text = achievement.description

        val dialog = AlertDialog.Builder(context)
            .setView(dialogView)
            .create()

        closeBtn.setOnClickListener {
            dialog.dismiss()
        }

        // Make background transparent (optional, for custom layout styling)
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        dialog.show()
    }
}
