package com.eriuzo.testgithubuserapp

import android.graphics.Color
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.observe
import androidx.paging.PagedList
import androidx.paging.PagedListAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.eriuzo.testgithubuserapp.databinding.UsersFragmentBinding
import com.eriuzo.testgithubuserapp.databinding.ViewItemUserBinding
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay

class UsersFragment : Fragment() {
    companion object {
        fun newInstance() = UsersFragment()
    }

    private val viewModel: UsersViewModel by viewModels()
    private var binding: UsersFragmentBinding? = null
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val b = UsersFragmentBinding.inflate(inflater, container, false)
        binding = b
        return b.root
    }

    private var autoCompleteJob: Job? = null

    // empty observer just to trigger autocomplete api call.
    private val dummyObserver = Observer<PagedList<GithubUser>> {
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding?.apply {
            editQuery.addTextChangedListener {
                autoCompleteJob?.cancel("new letter")
                autoCompleteJob = lifecycleScope.launchWhenResumed {
                    delay(1000)
                    handleSearch(it?.toString())
                }
            }
            buttonSearch.setOnClickListener {
                handleSearch(editQuery.text.toString())
            }
        }
    }

    private fun handleSearch(query: String?) {
        binding?.apply {
            val (pagedList, networkState) = viewModel.searchUsers(query) ?: return
            pagedList.observe(viewLifecycleOwner, dummyObserver)
            networkState?.observe(viewLifecycleOwner) {
                when (it) {
                    NetworkState.LOADED -> {
                        pagedList.removeObserver(dummyObserver)
                        pagedList.observe(viewLifecycleOwner, {
                            binding?.apply {
                                if (it.isEmpty()) {
                                    Snackbar.make(root, R.string.empty_users, Snackbar.LENGTH_SHORT)
                                        .setBackgroundTint(Color.BLACK)
                                        .show()
                                }
                                usersAdapter.submitList(it)
                            }
                        })
                    }
                    is NetworkState.ERROR -> {
                        Snackbar.make(this.root, it.error, Snackbar.LENGTH_SHORT)
                            .setBackgroundTint(Color.RED)
                            .show()
                    }
                }
            }
        }
    }

    private val usersAdapter: UsersListAdapter by lazy {
        UsersListAdapter(object : DiffUtil.ItemCallback<GithubUser>() {
            override fun areItemsTheSame(oldItem: GithubUser, newItem: GithubUser): Boolean {
                return oldItem.id == newItem.id
            }

            override fun areContentsTheSame(oldItem: GithubUser, newItem: GithubUser): Boolean {
                return oldItem == newItem
            }
        })
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        binding?.recyclerResults?.apply {
            layoutManager = LinearLayoutManager(context, RecyclerView.VERTICAL, false)
            isNestedScrollingEnabled = false
            adapter = usersAdapter
        }
    }
}

class UsersVH(binding: ViewItemUserBinding) : RecyclerView.ViewHolder(binding.root) {
    val avatar = binding.imageAvatar
    val name = binding.textName
}

class UsersListAdapter(itemCallback: DiffUtil.ItemCallback<GithubUser>) :
    PagedListAdapter<GithubUser, UsersVH>(itemCallback) {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UsersVH {
        val inflater = LayoutInflater.from(parent.context)
        return UsersVH(ViewItemUserBinding.inflate(inflater, parent, false))
    }

    override fun onBindViewHolder(holder: UsersVH, position: Int) {
        val item = getItem(position)
        item?.let {
            Glide.with(holder.avatar.context)
                .load(item.avatar_url)
                .circleCrop()
                .placeholder(R.drawable.ic_baseline_sync_24)
                .into(holder.avatar)
            holder.name.text = item.login
        }
    }
}