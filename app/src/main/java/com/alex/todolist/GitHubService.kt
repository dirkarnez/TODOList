package com.alex.todolist

import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Path

object GitHubService {
    const val API_URL = "https://api.github.com"

    class Contributor(val login: String, val contributions: Int)

    interface GitHub {
        @GET("/repos/{owner}/{repo}/contributors")
        fun contributors(
            @Path("owner") owner: String?,
            @Path("repo") repo: String?
        ): Call<List<Contributor>>
    }
}