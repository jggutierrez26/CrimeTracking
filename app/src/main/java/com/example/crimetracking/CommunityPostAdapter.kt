package com.example.crimetracking

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.Timestamp
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

class CommunityPostAdapter(
    private val currentUserId: String,
    private val onLikeClick: (CommunityPost) -> Unit,
    private val onCommentClick: (CommunityPost) -> Unit
) : ListAdapter<CommunityPost, CommunityPostAdapter.PostViewHolder>(PostDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PostViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_community_post, parent, false)
        return PostViewHolder(view)
    }

    override fun onBindViewHolder(holder: PostViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class PostViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvUserName: TextView = itemView.findViewById(R.id.tvUserName)
        private val tvTimestamp: TextView = itemView.findViewById(R.id.tvTimestamp)
        private val tvQuestion: TextView = itemView.findViewById(R.id.tvQuestion)
        private val tvLikeCount: TextView = itemView.findViewById(R.id.tvLikeCount)
        private val tvCommentCount: TextView = itemView.findViewById(R.id.tvCommentCount)
        private val ivLike: ImageView = itemView.findViewById(R.id.ivLike)
        private val btnLike: LinearLayout = itemView.findViewById(R.id.btnLike)
        private val btnComment: LinearLayout = itemView.findViewById(R.id.btnComment)

        fun bind(post: CommunityPost) {
            tvUserName.text = post.userName.ifEmpty {
                post.userEmail.substringBefore("@")
            }
            tvTimestamp.text = getTimeAgo(post.timestamp)
            tvQuestion.text = post.question
            tvLikeCount.text = post.likes.size.toString()
            tvCommentCount.text = post.commentCount.toString()

            // Update like button appearance based on whether current user liked
            val isLiked = post.likes.contains(currentUserId)
            if (isLiked) {
                ivLike.setColorFilter(
                    ContextCompat.getColor(itemView.context, R.color.redPrimary)
                )
                tvLikeCount.setTextColor(
                    ContextCompat.getColor(itemView.context, R.color.redPrimary)
                )
            } else {
                ivLike.setColorFilter(
                    ContextCompat.getColor(itemView.context, R.color.textSecondary)
                )
                tvLikeCount.setTextColor(
                    ContextCompat.getColor(itemView.context, R.color.textSecondary)
                )
            }

            btnLike.setOnClickListener { onLikeClick(post) }
            btnComment.setOnClickListener { onCommentClick(post) }
        }

        private fun getTimeAgo(timestamp: Timestamp): String {
            val now = System.currentTimeMillis()
            val time = timestamp.toDate().time
            val diff = now - time

            return when {
                diff < TimeUnit.MINUTES.toMillis(1) -> "Just now"
                diff < TimeUnit.HOURS.toMillis(1) -> {
                    val minutes = TimeUnit.MILLISECONDS.toMinutes(diff)
                    "${minutes}m ago"
                }
                diff < TimeUnit.DAYS.toMillis(1) -> {
                    val hours = TimeUnit.MILLISECONDS.toHours(diff)
                    "${hours}h ago"
                }
                diff < TimeUnit.DAYS.toMillis(7) -> {
                    val days = TimeUnit.MILLISECONDS.toDays(diff)
                    "${days}d ago"
                }
                else -> {
                    SimpleDateFormat("MMM d", Locale.getDefault()).format(timestamp.toDate())
                }
            }
        }
    }

    class PostDiffCallback : DiffUtil.ItemCallback<CommunityPost>() {
        override fun areItemsTheSame(oldItem: CommunityPost, newItem: CommunityPost): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: CommunityPost, newItem: CommunityPost): Boolean {
            return oldItem == newItem
        }
    }
}

