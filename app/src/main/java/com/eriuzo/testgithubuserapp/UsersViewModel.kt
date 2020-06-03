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
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.io.IOException


class UsersViewModel : ViewModel() {
    var users: LiveData<PagedList<GithubUser>> = MutableLiveData()
    fun searchUsers(queryString: String) {
        if (queryString.isBlank()) return
        users = GithubUserDataSourceFactory(queryString, githubService).toLiveData(
            pageSize = 10
        )

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
    val queryString: String,
    val githubService: GithubService
) : DataSource.Factory<Int, GithubUser>() {
    val sourceLiveData = MutableLiveData<UserPagedDataSource>()
    private var latestSource: UserPagedDataSource? = null
    override fun create(): DataSource<Int, GithubUser> {
        latestSource = UserPagedDataSource(githubService, queryString)
        sourceLiveData.postValue(latestSource)
        return latestSource!!
    }
}

fun Response<GithubUserResponse>.getLink(): Pair<Int?, Int?> {
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

class UserPagedDataSource(
    private val githubService: GithubService,
    private val queryString: String
) : PageKeyedDataSource<Int, GithubUser>() {
    override fun loadInitial(
        params: LoadInitialParams<Int>,
        callback: LoadInitialCallback<Int, GithubUser>
    ) {
        val request = githubService.searchUsers(queryString, 1, params.requestedLoadSize)
        // triggered by a refresh, we better execute sync
        try {
            val response = request.execute()
            val (prev, next) = response.getLink()
            val data = response.body()?.items
            if (data != null) {
//                retry = null
//                networkState.postValue(NetworkState.LOADED)
//                initialLoad.postValue(NetworkState.LOADED)
                callback.onResult(data, prev, next)
            }
        } catch (ioException: IOException) {
//            retry = {
//                loadInitial(params, callback)
//            }
//            val error = NetworkState.error(ioException.message ?: "unknown error")
//            networkState.postValue(error)
//            initialLoad.postValue(error)
        }
    }

    override fun loadAfter(
        params: LoadParams<Int>,
        callback: LoadCallback<Int, GithubUser>
    ) {
//        networkState.postValue(NetworkState.LOADING)
        githubService.searchUsers(
            queryString,
            page = params.key,
            perPage = params.requestedLoadSize
        ).enqueue(
            object : retrofit2.Callback<GithubUserResponse> {
                override fun onFailure(call: Call<GithubUserResponse>, t: Throwable) {
//                    retry = {
//                        loadAfter(params, callback)
//                    }
//                    networkState.postValue(NetworkState.error(t.message ?: "unknown err"))
                }

                override fun onResponse(
                    call: Call<GithubUserResponse>,
                    response: Response<GithubUserResponse>
                ) {
                    if (response.isSuccessful) {
                        val (_, next) = response.getLink()
                        val data = response.body()?.items
                        if (data != null) {
//                retry = null
//                networkState.postValue(NetworkState.LOADED)
//                initialLoad.postValue(NetworkState.LOADED)
                            callback.onResult(data, next)
                        }

//                        retry = null
//                        networkState.postValue(NetworkState.LOADED)
                    } else {
//                        retry = {
//                            loadAfter(params, callback)
//                        }
//                        networkState.postValue(
//                            NetworkState.error("error code: ${response.code()}"))
                    }
                }
            }
        )
    }

    override fun loadBefore(
        params: LoadParams<Int>,
        callback: LoadCallback<Int, GithubUser>
    ) {

//        networkState.postValue(NetworkState.LOADING)
        githubService.searchUsers(
            queryString,
            page = params.key,
            perPage = params.requestedLoadSize
        ).enqueue(
            object : retrofit2.Callback<GithubUserResponse> {
                override fun onFailure(call: Call<GithubUserResponse>, t: Throwable) {
//                    retry = {
//                        loadAfter(params, callback)
//                    }
//                    networkState.postValue(NetworkState.error(t.message ?: "unknown err"))
                }

                override fun onResponse(
                    call: Call<GithubUserResponse>,
                    response: Response<GithubUserResponse>
                ) {
                    if (response.isSuccessful) {
                        val (prev, _) = response.getLink()
                        val data = response.body()?.items
                        if (data != null) {
//                retry = null
//                networkState.postValue(NetworkState.LOADED)
//                initialLoad.postValue(NetworkState.LOADED)
                            callback.onResult(data, prev)
                        }

//                        retry = null
//                        networkState.postValue(NetworkState.LOADED)
                    } else {
//                        retry = {
//                            loadAfter(params, callback)
//                        }
//                        networkState.postValue(
//                            NetworkState.error("error code: ${response.code()}"))
                    }
                }
            }
        )
    }
}
