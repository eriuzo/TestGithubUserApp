package com.eriuzo.testgithubuserapp

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory


class UsersViewModel : ViewModel() {
    private val _users: MutableLiveData<List<GithubUser>> = MutableLiveData()
    val users: LiveData<List<GithubUser>>
        get() = _users

    suspend fun searchUsers(queryString: String) {
        // hit api
        val searchUsers = githubService.searchUsers(queryString).items
        _users.value = searchUsers
    }

    private val githubService: GithubService by lazy {
        val okhttp = OkHttpClient.Builder()
            .addNetworkInterceptor(HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BODY
            })
            .build()
        val retrofit = Retrofit.Builder()
            .client(okhttp)
            .baseUrl("https://api.github.com/")
            .addConverterFactory(MoshiConverterFactory.create())
            .build()
        retrofit.create(GithubService::class.java)
    }

}