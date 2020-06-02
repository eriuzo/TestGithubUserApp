package com.eriuzo.testgithubuserapp

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.observe
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.eriuzo.testgithubuserapp.databinding.UsersFragmentBinding
import com.eriuzo.testgithubuserapp.databinding.ViewItemUserBinding
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
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding?.apply {
            editQuery.addTextChangedListener {
                autoCompleteJob?.cancel("new letter")
                autoCompleteJob = lifecycleScope.launchWhenResumed {
                    delay(1500)
                    viewModel.searchUsers(it?.toString() ?: "")
                }
            }
            buttonSearch.setOnClickListener {
                lifecycleScope.launchWhenResumed {
                    viewModel.searchUsers(editQuery.text.toString())
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
        viewModel.users.observe(viewLifecycleOwner) {
            usersAdapter.submitList(it)
        }
    }
}

class UsersVH(binding: ViewItemUserBinding) : RecyclerView.ViewHolder(binding.root) {
    val avatar = binding.imageAvatar
    val name = binding.textName
}

class UsersListAdapter(itemCallback: DiffUtil.ItemCallback<GithubUser>) :
    ListAdapter<GithubUser, UsersVH>(itemCallback) {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = UsersVH(
        ViewItemUserBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
    )

    override fun onBindViewHolder(holder: UsersVH, position: Int) {
        val item = getItem(position)
        Glide.with(holder.avatar.context)
            .load(item.avatar_url)
            .circleCrop()
            .placeholder(R.drawable.ic_baseline_sync_24)
            .into(holder.avatar)
        holder.name.text = item.login
    }
}