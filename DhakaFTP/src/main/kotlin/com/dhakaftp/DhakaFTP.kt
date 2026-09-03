package com.dhakaftp

import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.*
import org.jsoup.nodes.Element
import java.net.URLEncoder

class DhakaFTP : MainAPI() {

    override var name = "Dhaka FTP"

    override var mainUrl = "http://172.16.50.7/"

    override val hasMainPage = true

    override val hasQuickSearch = true

    override val supportedTypes = setOf(
        TvType.Movie,
        TvType.TvSeries,
        TvType.Anime
    )

    override suspend fun getMainPage(
        page: Int,
        request: MainPageRequest
    ): HomePageResponse {

        val pages = listOf(
            "English" to
                    "http://172.16.50.7/DHAKA-FLIX-7/English%20Movies/",

            "Hindi" to
                    "http://172.16.50.14/DHAKA-FLIX-14/Hindi%20Movies/",

            "Bangla" to
                    "http://172.16.50.7/DHAKA-FLIX-7/Kolkata%20Bangla%20Movies/",

            "South Indian" to
                    "http://172.16.50.14/DHAKA-FLIX-14/SOUTH%20INDIAN%20MOVIES/Hindi%20Dubbed/",

            "TV / Web Series" to
                    "http://172.16.50.12/DHAKA-FLIX-12/TV-WEB-Series/",

            "K-Drama" to
                    "http://172.16.50.14/DHAKA-FLIX-14/KOREAN%20TV%20%26%20WEB%20Series/",

            "Anime" to
                    "http://172.16.50.14/DHAKA-FLIX-14/Animation%20Movies/"
        )

        val home = ArrayList<HomePageList>()

        for ((title, url) in pages) {

            try {

                val items = readDirectory(url)
                    .mapNotNull { it.toSearchResponse() }

                if (items.isNotEmpty()) {
                    home.add(
                        HomePageList(
                            title,
                            items
                        )
                    )
                }

            } catch (_: Exception) {
                // Ignore unavailable server/category
            }
        }

        return HomePageResponse(home)
    }

    override suspend fun search(
        query: String
    ): List<SearchResponse> {

        val results = ArrayList<SearchResponse>()

        val pages = listOf(
            "http://172.16.50.7/DHAKA-FLIX-7/English%20Movies/",
            "http://172.16.50.14/DHAKA-FLIX-14/Hindi%20Movies/",
            "http://172.16.50.7/DHAKA-FLIX-7/Kolkata%20Bangla%20Movies/",
            "http://172.16.50.14/DHAKA-FLIX-14/SOUTH%20INDIAN%20MOVIES/Hindi%20Dubbed/",
            "http://172.16.50.12/DHAKA-FLIX-12/TV-WEB-Series/",
            "http://172.16.50.14/DHAKA-FLIX-14/KOREAN%20TV%20%26%20WEB%20Series/",
            "http://172.16.50.14/DHAKA-FLIX-14/Animation%20Movies/"
        )

        for (categoryUrl in pages) {

            try {

                val years = readDirectory(categoryUrl)

                for (year in years) {

                    val yearUrl = year.attr("href")

                    if (!yearUrl.endsWith("/")) continue

                    val yearFullUrl =
                        fixUrl(categoryUrl, yearUrl)

                    val movies = readDirectory(yearFullUrl)

                    for (movie in movies) {

                        val movieName =
                            movie.text().trim()

                        if (
                            movieName.contains(
                                query,
                                ignoreCase = true
                            )
                        ) {

                            val href =
                                movie.attr("href")

                            val movieUrl =
                                fixUrl(
                                    yearFullUrl,
                                    href
                                )

                            results.add(
                                newMovieSearchResponse(
                                    cleanName(movieName),
                                    movieUrl,
                                    TvType.Movie
                                )
                            )
                        }
                    }
                }

            } catch (_: Exception) {
                // Continue with next category
            }
        }

        return results
    }

    override suspend fun load(
        url: String
    ): LoadResponse? {

        val document =
            app.get(url).document

        val title =
            document
                .select("a")
                .map { it.text().trim() }
                .firstOrNull {
                    it.endsWith(
                        ".mkv",
                        true
                    ) ||
                    it.endsWith(
                        ".mp4",
                        true
                    )
                }
                ?.let {
                    cleanName(
                        it.substringBeforeLast(".")
                    )
                }
                ?: cleanName(
                    url
                        .trimEnd('/')
                        .substringAfterLast('/')
                )

        val videoFile =
            document
                .select("a")
                .firstOrNull {
                    val href =
                        it.attr("href")

                    href.endsWith(
                        ".mkv",
                        true
                    ) ||
                    href.endsWith(
                        ".mp4",
                        true
                    ) ||
                    href.endsWith(
                        ".webm",
                        true
                    )
                }
                ?: return null

        val videoUrl =
            fixUrl(
                url,
                videoFile.attr("href")
            )

        val poster =
            document
                .select("img")
                .firstOrNull()
                ?.attr("src")
                ?.let {
                    fixUrl(url, it)
                }

        return newMovieLoadResponse(
            title,
            url,
            TvType.Movie,
            videoUrl
        ) {
            this.posterUrl = poster
        }
    }

    override suspend fun loadLinks(
        data: String,
        isCasting: Boolean,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ): Boolean {

        callback(
            ExtractorLink(
                this.name,
                this.name,
                data,
                "",
                Qualities.Unknown.value,
                false
            )
        )

        return true
    }

    private suspend fun readDirectory(
        url: String
    ): List<Element> {

        val document =
            app.get(url).document

        return document
            .select("a")
            .filter {
                val href =
                    it.attr("href")

                href.isNotBlank() &&
                href != "../" &&
                href != "./"
            }
    }

    private fun Element.toSearchResponse():
            SearchResponse? {

        val href =
            attr("href")

        if (href.isBlank()) {
            return null
        }

        val name =
            text().trim()

        if (name.isBlank()) {
            return null
        }

        return newMovieSearchResponse(
            cleanName(name),
            href,
            TvType.Movie
        )
    }

    private fun cleanName(
        value: String
    ): String {

        return value
            .removeSuffix("/")
            .replace(
                Regex("\\.(mkv|mp4|webm)$"),
                "",
                ignoreCase = true
            )
            .trim()
    }

    private fun fixUrl(
        base: String,
        path: String
    ): String {

        if (
            path.startsWith(
                "http://"
            ) ||
            path.startsWith(
                "https://"
            )
        ) {
            return path
        }

        return when {
            path.startsWith("/") -> {
                "http://172.16.50.7$path"
            }

            else -> {
                base.trimEnd('/') +
                    "/" +
                    path.trimStart('/')
            }
        }
    }
}
