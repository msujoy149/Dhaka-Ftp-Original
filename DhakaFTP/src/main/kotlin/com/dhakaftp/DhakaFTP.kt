package com.dhakaftp

import com.lagradost.cloudstream3.MainAPI
import com.lagradost.cloudstream3.TvType
import com.lagradost.cloudstream3.SearchResponse
import com.lagradost.cloudstream3.LoadResponse
import com.lagradost.cloudstream3.LoadResponse.Companion.addTrailer
import com.lagradost.cloudstream3.newMovieSearchResponse
import com.lagradost.cloudstream3.newMovieLoadResponse
import com.lagradost.cloudstream3.newMovieLoadResponse
import com.lagradost.cloudstream3.utils.loadExtractor
import com.lagradost.cloudstream3.utils.newExtractorLink
import com.lagradost.cloudstream3.app
import com.lagradost.cloudstream3.utils.AppUtils.parseJson
import com.lagradost.cloudstream3.MainPageData
import com.lagradost.cloudstream3.mainPageOf
import com.lagradost.cloudstream3.LoadResponse.Companion.isMovie
import org.jsoup.Jsoup
import java.net.URLEncoder

class DhakaFTP : MainAPI() {

    override var name = "Dhaka FTP"

    override val supportedTypes = setOf(
        TvType.Movie,
        TvType.TvSeries,
        TvType.Anime
    )

    override val hasMainPage = true
    override val hasQuickSearch = true

    private val categories = listOf(
        "English" to "http://172.16.50.7/DHAKA-FLIX-7/English%20Movies/",
        "Hindi" to "http://172.16.50.14/DHAKA-FLIX-14/Hindi%20Movies/",
        "Bangla" to "http://172.16.50.7/DHAKA-FLIX-7/Kolkata%20Bangla%20Movies/",
        "South Indian" to "http://172.16.50.14/DHAKA-FLIX-14/SOUTH%20INDIAN%20MOVIES/Hindi%20Dubbed/",
        "TV / Web Series" to "http://172.16.50.12/DHAKA-FLIX-12/TV-WEB-Series/",
        "K-Drama" to "http://172.16.50.14/DHAKA-FLIX-14/KOREAN%20TV%20%26%20WEB%20Series/",
        "Anime" to "http://172.16.50.14/DHAKA-FLIX-14/Animation%20Movies/"
    )

    override fun getMainPage(
        page: Int,
        request: MainPageRequest
    ): HomePageResponse {
        val results = categories.map { (name, url) ->
            HomePageList(
                name,
                ArrayList(),
                false
            )
        }

        return newHomePageResponse(results)
    }

    override suspend fun search(query: String): List<SearchResponse> {
        val results = ArrayList<SearchResponse>()

        categories.forEach { (_, baseUrl) ->
            try {
                val html = app.get(baseUrl).text
                val document = Jsoup.parse(html)

                document.select("a[href]").forEach { link ->
                    val title = link.text().trim()
                    val href = link.attr("abs:href")

                    if (
                        title.isNotBlank() &&
                        title.contains(query, ignoreCase = true)
                    ) {
                        results.add(
                            newMovieSearchResponse(
                                title,
                                href,
                                TvType.Movie
                            )
                        )
                    }
                }
            } catch (_: Exception) {
                // Ignore unavailable server/category
            }
        }

        return results
    }

    override suspend fun load(url: String): LoadResponse {
        val html = app.get(url).text
        val document = Jsoup.parse(html)

        val title = document
            .select("title")
            .text()
            .ifBlank {
                url.substringAfterLast("/")
                    .substringBeforeLast(".")
                    .replace("_", " ")
            }

        val videoLinks = document
            .select("a[href]")
            .mapNotNull { it.attr("abs:href") }
            .filter {
                it.endsWith(".mp4", true) ||
                it.endsWith(".mkv", true) ||
                it.endsWith(".webm", true) ||
                it.endsWith(".avi", true)
            }

        if (videoLinks.isNotEmpty()) {
            return newMovieLoadResponse(
                title,
                url,
                TvType.Movie,
                videoLinks.first()
            ) {
                plot = "Dhaka FTP"
            }
        }

        return newMovieLoadResponse(
            title,
            url,
            TvType.Movie,
            url
        ) {
            plot = "Dhaka FTP"
        }
    }
}
