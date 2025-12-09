package com.example.crimetracking

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

class CommunityFragment : Fragment() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    private lateinit var rvPosts: RecyclerView
    private lateinit var etQuestion: EditText
    private lateinit var btnPost: Button
    private lateinit var emptyState: LinearLayout
    private lateinit var progressBar: ProgressBar

    private lateinit var postAdapter: CommunityPostAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_community, container, false)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        initializeViews(view)
        setupRecyclerView()
        setupPostButton()
        loadPosts()

        return view
    }

    private fun initializeViews(view: View) {
        rvPosts = view.findViewById(R.id.rvPosts)
        etQuestion = view.findViewById(R.id.etQuestion)
        btnPost = view.findViewById(R.id.btnPost)
        emptyState = view.findViewById(R.id.emptyState)
        progressBar = view.findViewById(R.id.progressBar)
    }

    private fun setupRecyclerView() {
        val currentUserId = auth.currentUser?.uid ?: ""

        postAdapter = CommunityPostAdapter(
            currentUserId = currentUserId,
            onLikeClick = { post -> toggleLike(post) },
            onCommentClick = { post -> showCommentsDialog(post) }
        )

        rvPosts.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = postAdapter
        }
    }

    private fun setupPostButton() {
        btnPost.setOnClickListener {
            val questionText = etQuestion.text.toString().trim()

            if (questionText.isEmpty()) {
                Toast.makeText(requireContext(), "Please enter a question", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (questionText.length < 10) {
                Toast.makeText(requireContext(), "Question must be at least 10 characters", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            createPost(questionText)
        }
    }

    private fun createPost(questionText: String) {
        val user = auth.currentUser ?: return

        btnPost.isEnabled = false
        progressBar.visibility = View.VISIBLE

        val post = hashMapOf(
            "userId" to user.uid,
            "userName" to (user.displayName ?: ""),
            "userEmail" to (user.email ?: ""),
            "question" to questionText,
            "timestamp" to Timestamp.now(),
            "likes" to emptyList<String>(),
            "commentCount" to 0
        )

        db.collection("community_posts")
            .add(post)
            .addOnSuccessListener {
                etQuestion.text.clear()
                Toast.makeText(requireContext(), "Post created successfully!", Toast.LENGTH_SHORT).show()
                btnPost.isEnabled = true
                progressBar.visibility = View.GONE
            }
            .addOnFailureListener { e ->
                Toast.makeText(requireContext(), "Failed to create post: ${e.message}", Toast.LENGTH_SHORT).show()
                btnPost.isEnabled = true
                progressBar.visibility = View.GONE
            }
    }

    private fun loadPosts() {
        progressBar.visibility = View.VISIBLE

        db.collection("community_posts")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshots, error ->
                progressBar.visibility = View.GONE

                if (error != null) {
                    Toast.makeText(requireContext(), "Failed to load posts", Toast.LENGTH_SHORT).show()
                    return@addSnapshotListener
                }

                val posts = snapshots?.toObjects(CommunityPost::class.java) ?: emptyList()

                if (posts.isEmpty()) {
                    emptyState.visibility = View.VISIBLE
                    rvPosts.visibility = View.GONE
                } else {
                    emptyState.visibility = View.GONE
                    rvPosts.visibility = View.VISIBLE
                    postAdapter.submitList(posts)
                }
            }
    }

    private fun toggleLike(post: CommunityPost) {
        val userId = auth.currentUser?.uid ?: return

        val postRef = db.collection("community_posts").document(post.id)

        db.runTransaction { transaction ->
            val snapshot = transaction.get(postRef)
            val currentLikes = snapshot.get("likes") as? List<String> ?: emptyList()

            val newLikes = if (currentLikes.contains(userId)) {
                // Unlike
                currentLikes.filter { it != userId }
            } else {
                // Like
                currentLikes + userId
            }

            transaction.update(postRef, "likes", newLikes)
        }.addOnFailureListener { e ->
            Toast.makeText(requireContext(), "Failed to update like", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showCommentsDialog(post: CommunityPost) {
        val dialog = BottomSheetDialog(requireContext())
        val view = layoutInflater.inflate(R.layout.dialog_comments, null)

        val rvComments: RecyclerView = view.findViewById(R.id.rvComments)
        val etComment: EditText = view.findViewById(R.id.etComment)
        val btnSendComment: ImageButton = view.findViewById(R.id.btnSendComment)
        val btnCloseComments: ImageButton = view.findViewById(R.id.btnCloseComments)
        val tvEmptyComments: TextView = view.findViewById(R.id.tvEmptyComments)

        val commentAdapter = CommentAdapter()
        rvComments.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = commentAdapter
        }

        // Load comments
        db.collection("community_posts").document(post.id)
            .collection("comments")
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshots, error ->
                if (error != null) return@addSnapshotListener

                val comments = snapshots?.toObjects(Comment::class.java) ?: emptyList()

                if (comments.isEmpty()) {
                    tvEmptyComments.visibility = View.VISIBLE
                    rvComments.visibility = View.GONE
                } else {
                    tvEmptyComments.visibility = View.GONE
                    rvComments.visibility = View.VISIBLE
                    commentAdapter.submitList(comments)
                }
            }

        // Send comment
        btnSendComment.setOnClickListener {
            val commentText = etComment.text.toString().trim()

            if (commentText.isEmpty()) {
                Toast.makeText(requireContext(), "Please enter a comment", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val user = auth.currentUser ?: return@setOnClickListener

            val comment = hashMapOf(
                "postId" to post.id,
                "userId" to user.uid,
                "userName" to (user.displayName ?: ""),
                "userEmail" to (user.email ?: ""),
                "text" to commentText,
                "timestamp" to Timestamp.now()
            )

            db.collection("community_posts").document(post.id)
                .collection("comments")
                .add(comment)
                .addOnSuccessListener {
                    // Update comment count
                    db.collection("community_posts").document(post.id)
                        .update("commentCount", post.commentCount + 1)

                    etComment.text.clear()
                }
                .addOnFailureListener { e ->
                    Toast.makeText(requireContext(), "Failed to add comment", Toast.LENGTH_SHORT).show()
                }
        }

        btnCloseComments.setOnClickListener {
            dialog.dismiss()
        }

        dialog.setContentView(view)
        dialog.show()
    }
}

