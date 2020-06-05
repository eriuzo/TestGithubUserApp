package com.eriuzo.testgithubuserapp

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.paging.DataSource
import androidx.paging.PageKeyedDataSource
import androidx.paging.PagedList
import androidx.paging.toLiveData
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.io.IOException

sealed class NetworkState {
    object LOADED : NetworkState()
    object LOADING : NetworkState()
    data class ERROR(val error: String) : NetworkState()
}

class UsersViewModel : ViewModel() {
    fun searchUsers(queryString: String?): Pair<LiveData<PagedList<GithubUser>>, MutableLiveData<NetworkState>?>? {
        if (queryString.isNullOrBlank()) return null
        val githubUserDataSourceFactory = GithubUserDataSourceFactory(githubService, queryString)
        val pagedList = githubUserDataSourceFactory.toLiveData(10)
        val sourceLiveData = githubUserDataSourceFactory.sourceLiveData
        return pagedList to sourceLiveData.value?.networkState
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

class GithubUserDataSourceFactory(
    private val githubService: GithubService,
    private val queryString: String
) : DataSource.Factory<Int, GithubUser>() {
    private val ls = UserPagedDataSource(githubService, queryString)
    val sourceLiveData = MutableLiveData<UserPagedDataSource>(ls)
    override fun create(): DataSource<Int, GithubUser> {
        return ls
    }
}

class UserPagedDataSource(
    private val githubService: GithubService,
    private val queryString: String
) : PageKeyedDataSource<Int, GithubUser>() {
    val networkState: MutableLiveData<NetworkState> = MutableLiveData()
    private fun Response<GithubUserResponse>.getLink(): Pair<Int?, Int?> {
        val link = headers().get("link")
        val split = link?.split(',')
        val next = split?.firstOrNull {
            it.contains(">; rel=\"next\"")
        }?.run {
            substring(
                startIndex = indexOf("page=") + 5,
                endIndex = indexOf("&per_page=")
            ).toInt()
        }
        val prev = split?.firstOrNull {
            it.contains(">; rel=\"prev\"")
        }?.run {
            substring(
                startIndex = indexOf("page=") + 5,
                endIndex = indexOf("&per_page=")
            ).toInt()
        }
        return prev to next
    }

    override fun loadInitial(
        params: LoadInitialParams<Int>,
        callback: LoadInitialCallback<Int, GithubUser>
    ) {
        networkState.postValue(NetworkState.LOADING)
        val request = githubService.searchUsers(queryString, 1, params.requestedLoadSize)
        try {
            val response = request.execute()
            val (prev, next) = response.getLink()
            val data = response.body()?.items
            if (data != null) {
                networkState.postValue(NetworkState.LOADED)
                callback.onResult(data, prev, next)
            }
        } catch (ioException: IOException) {
            val error = NetworkState.ERROR(ioException.message ?: "unknown error")
            networkState.postValue(error)
        }
    }

    override fun loadAfter(
        params: LoadParams<Int>,
        callback: LoadCallback<Int, GithubUser>
    ) {
        networkState.postValue(NetworkState.LOADING)
        githubService.searchUsers(
            queryString,
            page = params.key,
            perPage = params.requestedLoadSize
        ).enqueue(object : Callback<GithubUserResponse> {
            override fun onFailure(call: Call<GithubUserResponse>, t: Throwable) {
                networkState.postValue(NetworkState.ERROR(t.message ?: "unknown err"))
            }

            override fun onResponse(
                call: Call<GithubUserResponse>,
                response: Response<GithubUserResponse>
            ) {
                if (response.isSuccessful) {
                    val (_, next) = response.getLink()
                    val data = response.body()?.items
                    if (data != null) {
                        networkState.postValue(NetworkState.LOADED)
                        callback.onResult(data, next)
                    }
                } else {
                    networkState.postValue(NetworkState.ERROR("error code: ${response.code()}"))
                }
            }
        })
    }

    override fun loadBefore(
        params: LoadParams<Int>,
        callback: LoadCallback<Int, GithubUser>
    ) {
        networkState.postValue(NetworkState.LOADING)
        githubService.searchUsers(
            queryString,
            page = params.key,
            perPage = params.requestedLoadSize
        ).enqueue(object : Callback<GithubUserResponse> {
            override fun onFailure(call: Call<GithubUserResponse>, t: Throwable) {
                networkState.postValue(NetworkState.ERROR(t.message ?: "unknown err"))
            }

            override fun onResponse(
                call: Call<GithubUserResponse>,
                response: Response<GithubUserResponse>
            ) {
                if (response.isSuccessful) {
                    val (prev, _) = response.getLink()
                    val data = response.body()?.items
                    if (data != null) {
                        networkState.postValue(NetworkState.LOADED)
                        callback.onResult(data, prev)
                    }
                } else {
                    networkState.postValue(NetworkState.ERROR("error code: ${response.code()}"))
                }
            }
        })
    }
}
