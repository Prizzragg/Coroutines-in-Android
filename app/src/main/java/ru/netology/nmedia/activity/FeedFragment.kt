package ru.netology.nmedia.activity

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import ru.netology.nmedia.R
import ru.netology.nmedia.activity.NewPostFragment.Companion.textArg
import ru.netology.nmedia.adapter.OnInteractionListener
import ru.netology.nmedia.adapter.PostsAdapter
import ru.netology.nmedia.auth.AppAuth
import ru.netology.nmedia.databinding.FragmentFeedBinding
import ru.netology.nmedia.dto.Post
import ru.netology.nmedia.viewmodel.PostViewModel
import javax.inject.Inject

@AndroidEntryPoint
class FeedFragment : Fragment() {

    @Inject
    lateinit var appAuth: AppAuth
    private val viewModel: PostViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val binding = FragmentFeedBinding.inflate(inflater, container, false)

        val adapter = PostsAdapter(object : OnInteractionListener {
            override fun onEdit(post: Post) {
                viewModel.edit(post)
            }

            override fun onLike(post: Post) {
                if (appAuth.data.value == null) {
                    binding.pleaseAuth.visibility = View.VISIBLE
                    binding.pleaseAuth.setOnClickListener {
                        findNavController().navigate(R.id.action_feedFragment_to_signInFragment)
                    }
                } else {
                    binding.pleaseAuth.visibility = View.GONE
                    viewModel.likeById(post.id)
                }
            }

            override fun onRemove(post: Post) {
                viewModel.removeById(post.id)
            }

            override fun onOpenPhoto(photoUrl: String) {

                findNavController().navigate(
                    R.id.action_feedFragment_to_photoFragment,
                    Bundle().apply {
                        textArg = photoUrl
                    }
                )
            }

            override fun onShare(post: Post) {
                val intent = Intent().apply {
                    action = Intent.ACTION_SEND
                    putExtra(Intent.EXTRA_TEXT, post.content)
                    type = "text/plain"
                }

                val shareIntent =
                    Intent.createChooser(intent, getString(R.string.chooser_share_post))
                startActivity(shareIntent)
            }

        })
        binding.list.adapter = adapter
        viewModel.dataState.observe(viewLifecycleOwner) { state ->
            binding.progress.isVisible = state.loading
            binding.swiperefresh.isRefreshing = state.refreshing
            if (state.error) {
                Snackbar.make(binding.root, R.string.error_loading, Snackbar.LENGTH_LONG)
                    .setAction(R.string.retry_loading) { viewModel.loadPosts() }
                    .show()
            }
            if (state.errorRemove) {
                Snackbar.make(binding.root, R.string.error_remove, Snackbar.LENGTH_LONG)
                    .setAction(R.string.retry_loading) { viewModel.removeById(id = state.id) }
                    .show()
            }
            if (state.errorLike) {
                Snackbar.make(binding.root, R.string.error_like, Snackbar.LENGTH_LONG)
                    .setAction(R.string.retry_loading) { viewModel.likeById(id = state.id) }
                    .show()
            }
        }
        viewModel.data.observe(viewLifecycleOwner) { state ->
            binding.list.post {
                adapter.submitList(state.posts) {
                    binding.list.scrollToPosition(0)
                }
            }
            binding.emptyText.isVisible = state.empty
        }

        viewModel.newerCount.observe(viewLifecycleOwner) { state ->
            if (state > 0) {
                binding.update.visibility = View.VISIBLE
            }
        }

        binding.swiperefresh.setOnRefreshListener {
            viewModel.refreshPosts()
        }

        binding.update.setOnClickListener {
            viewModel.updatePosts()
            binding.update.visibility = View.GONE
        }

        binding.fab.setOnClickListener {
            if (appAuth.data.value == null) {
                binding.pleaseAuth.visibility = View.VISIBLE
                binding.pleaseAuth.setOnClickListener {
                    findNavController().navigate(R.id.action_feedFragment_to_signInFragment)
                }
            } else {
                findNavController().navigate(R.id.action_feedFragment_to_newPostFragment)
                binding.pleaseAuth.visibility = View.GONE
            }
        }

        return binding.root
    }
}
