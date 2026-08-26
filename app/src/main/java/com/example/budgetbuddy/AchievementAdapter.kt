import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.budgetbuddy.Achievement
import com.example.budgetbuddy.R

class AchievementAdapter(
    private val achievements: List<Achievement>
) : RecyclerView.Adapter<AchievementAdapter.AchievementViewHolder>() {

    class AchievementViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val achievementTitle: TextView = itemView.findViewById(R.id.tvAchievementTitle)
        val achievementDescription: TextView = itemView.findViewById(R.id.tvAchievementDescription)
        val achievementIcon: ImageView = itemView.findViewById(R.id.ivAchievementIcon)
        val progressBar: ProgressBar = itemView.findViewById(R.id.pbAchievementProgress)
        val statusBanner: TextView = itemView.findViewById(R.id.tvStatusBanner)
        val expandToggle: ImageView = itemView.findViewById(R.id.ivExpandToggle)
        val expandedLayout: View = itemView.findViewById(R.id.expandedLayout)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AchievementViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_achievement, parent, false)
        return AchievementViewHolder(view)
    }

    override fun onBindViewHolder(holder: AchievementViewHolder, position: Int) {
        val achievement = achievements[position]

        holder.achievementTitle.text = achievement.title

        val titleColorRes = if (achievement.isCompleted) R.color.black else R.color.grey
        holder.achievementTitle.setTextColor(holder.itemView.context.getColor(titleColorRes))

        holder.achievementIcon.setImageResource(
            if (achievement.isCompleted) achievement.badgeResId else R.drawable.locked_badge
        )

        // Set visibility of expanded section
        holder.expandedLayout.visibility = if (achievement.isExpanded) View.VISIBLE else View.GONE

        // Set description
        holder.achievementDescription.text = achievement.description

        // Show progress bar if needed
        if (achievement.isRecurring && !achievement.isCompleted) {
            holder.progressBar.visibility = View.VISIBLE
            holder.progressBar.max = achievement.target
            holder.progressBar.progress = achievement.progress
        } else {
            holder.progressBar.visibility = View.GONE
        }

        // Status banner
        if (achievement.isExpanded) {
            holder.statusBanner.visibility = View.VISIBLE
            holder.statusBanner.text = when {
                achievement.isCompleted -> "Completed"
                achievement.isRecurring -> "${achievement.progress}/${achievement.target} progress"
                else -> "In Progress"
            }
        } else {
            holder.statusBanner.visibility = View.GONE
        }

        // Rotate arrow if expanded (optional but nice)
        holder.expandToggle.rotation = if (achievement.isExpanded) 180f else 0f

        // Toggle expansion on arrow click
        holder.expandToggle.setOnClickListener {
            achievement.isExpanded = !achievement.isExpanded
            notifyItemChanged(position)
        }
    }

    override fun getItemCount(): Int = achievements.size
}