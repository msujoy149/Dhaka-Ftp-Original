package com.dhakaftp

import com.lagradost.cloudstream3.HomePageList
import com.lagradost.cloudstream3.HomePageResponse
import com.lagradost.cloudstream3.LoadResponse
import com.lagradost.cloudstream3.MainAPI
import com.lagradost.cloudstream3.MainPageRequest
import com.lagradost.cloudstream3.SearchResponse
import com.lagradost.cloudstream3.TvType
import com.lagradost.cloudstream3.newMovieSearchResponse
import com.lagradost.cloudstream3.plugins.CloudstreamPlugin
import com.lagradost.cloudstream3.utils.loadExtractor
import com.lagradost.cloudstream3.utils.newMovieLoadResponse
import com.lagradost.cloudstream3.utils.newMovieSearchResponse
import com.lagradost.cloudstream3.utils.app
import org.jsoup.nodes.Element
import java.net.URLEncoder

@CloudstreamPlugin
class DhakaFTPPlugin : com.lagradost.cloudstream3.plugins.Plugin() {
    override fun load(context: android.content.Context) {
        registerMainAPI(DhakaFTP())
    }
}

class DhakaFTP : MainAPI() {

    override var name = "Dhaka FTP"

    override val supportedTypes = setOf(
        TvType.Movie,
        TvType.TvSeries,
        TvType.Anime
    )

    override val hasMainPage = true
    override val hasQuickSearch = true

    private data class Category(
        val name: String,
        val url: String
    )

    private val categories = listOf(
        Category(
            "English",
            "http://172.16.50.7/DHAKA-FLIX-7/English%20Movies/"
        ),
        Category(
            "Hindi",
            "http://172.16.50.14/DHAKA-FLIX-14/Hindi%20Movies/"
        ),
        Category(
            "Bangla",
            "http://172.16.50.7/DHAKA-FLIX-7/Kolkata%20Bangla%20Movies/"
        ),
        Category(
            "South Indian",
            "http://172.16.50.14/DHAKA-FLIX-14/SOUTH%20INDIAN%20MOVIES/Hindi%20Dubbed/"
        ),
        Category(
            "TV / Web Series",
            "http://172.16.50.12/DHAKA-FLIX-12/TV-WEB-Series/"
        ),
        Category(
            "K-Drama",
            "http://172.16.50.14/DHAKA-FLIX-14/KOREAN%20TV%20%26%20WEB%20Series/"
        ),
        Category(
            "Anime",
            "http://172.16.50.14/DHAKA-FLIX-14/Animation%20Movies/"
        )
    )

    override suspend fun getMainPage(
        page: Int,
        request: MainPageRequest
    ): HomePageResponse {

        val results = categories.map { category ->
            HomePageList(
                category.name,
                listOf(
                    newMovieSearchResponse(
                        category.name,
                        category.url,
                        TvType.Movie
                    )
                )
            )
        }

        return HomePageResponse(results)
    }

    override suspend fun search(query: String): List<SearchResponse>? {
        return emptyList()
    }

    override suspend fun load(url: String): LoadResponse? {
        return newMovieLoadResponse(
            "Dhaka FTP",
            url,
            TvType.Movie,
            url
        )
    }
}
