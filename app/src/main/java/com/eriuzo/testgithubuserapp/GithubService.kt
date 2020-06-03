package com.eriuzo.testgithubuserapp

import com.squareup.moshi.JsonClass
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Query

interface GithubService {
    @GET("/search/users")
    fun searchUsers(
        @Query("q") queryString: String,
        @Query("page") page: Int,
        @Query("per_page") perPage: Int
    ):Call<GithubUserResponse>
}

@JsonClass(generateAdapter = true)
data class GithubUserResponse(
    val total_count: Int,
    val incomplete_results: Boolean,
    val items: List<GithubUser>
)

@JsonClass(generateAdapter = true)
data class GithubUser(
    val login: String,
    val id: Long,
    val node_id: String,
    val avatar_url: String,
    val gravatar_id: String,
    val url: String,
    val html_url: String,
    val followers_url: String,
    val following_url: String,
    val gists_url: String,
    val starred_url: String,
    val subscriptions_url: String,
    val organizations_url: String,
    val repos_url: String,
    val events_url: String,
    val received_events_url: String,
    val type: String,
    val site_admin: Boolean,
    val score: Double
)