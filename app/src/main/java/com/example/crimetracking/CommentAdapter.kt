package com.example.crimetracking

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.Timestamp
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

class CommentAdapter : ListAdapter<Comment, CommentAdapter.CommentViewHolder>(CommentDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CommentViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_comment, parent, false)
        return CommentViewHolder(view)
    }

    override fun onBindViewHolder(holder: CommentViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class CommentViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvCommentUserName: TextView = itemView.findViewById(R.id.tvCommentUserName)
        private val tvCommentTimestamp: TextView = itemView.findViewById(R.id.tvCommentTimestamp)
        private val tvCommentText: TextView = itemView.findViewById(R.id.tvCommentText)

        fun bind(comment: Comment) {
            tvCommentUserName.text = comment.userName.ifEmpty {
                comment.userEmail.substringBefore("@")
            }
            tvCommentTimestamp.text = getTimeAgo(comment.timestamp)
            tvCommentText.text = comment.text
        }

        private fun getTimeAgo(timestamp: Timestamp): String {
            val now = System.currentTimeMillis()
            val time = timestamp.toDate().time
            val diff = now - time

            return when {
                diff < TimeUnit.MINUTES.toMillis(1) -> "now"
                diff < TimeUnit.HOURS.toMillis(1) -> {
                    val minutes = TimeUnit.MILLISECONDS.toMinutes(diff)
                    "${minutes}m"
                }
                diff < TimeUnit.DAYS.toMillis(1) -> {
                    val hours = TimeUnit.MILLISECONDS.toHours(diff)
                    "${hours}h"
                }
                diff < TimeUnit.DAYS.toMillis(7) -> {
                    val days = TimeUnit.MILLISECONDS.toDays(diff)
                    "${days}d"
                }
                else -> {
                    SimpleDateFormat("MMM d", Locale.getDefault()).format(timestamp.toDate())
                }
            }
        }
    }

    class CommentDiffCallback : DiffUtil.ItemCallback<Comment>() {
        override fun areItemsTheSame(oldItem: Comment, newItem: Comment): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Comment, newItem: Comment): Boolean {
            return oldItem == newItem
        }
    }
}

