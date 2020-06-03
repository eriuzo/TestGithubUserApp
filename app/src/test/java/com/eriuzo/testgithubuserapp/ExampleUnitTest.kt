package com.eriuzo.testgithubuserapp

import org.junit.Test

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
class ExampleUnitTest {
    @Test
    fun test_link_rel_split() {
        val trimIndent = """
<https://api.github.com/search/users?q=er&page=2&per_page=10>; rel="prev", <https://api.github.com/search/users?q=er&page=4&per_page=10>; rel="next", <https://api.github.com/search/users?q=er&page=100&per_page=10>; rel="last", <https://api.github.com/search/users?q=er&page=1&per_page=10>; rel="first"
        """.trimIndent()
        val split = trimIndent.split(',')
        println(split)
        val next = split.firstOrNull {
            it.contains(">; rel=\"next\"")
        }?.run {
            substring(
                startIndex = indexOf("page=") + 5,
                endIndex = indexOf("&per_page=")
            ).toInt()
        } ?: 1
        val prev = split.firstOrNull {
            it.contains(">; rel=\"prev\"")
        }?.run {
            substring(
                startIndex = indexOf("page=") + 5,
                endIndex = indexOf("&per_page=")
            ).toInt()
        } ?: 1
        println(split)
    }
}