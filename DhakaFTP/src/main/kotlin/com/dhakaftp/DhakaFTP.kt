package com.dhakaftp

import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.*
import org.jsoup.nodes.Element

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

    /*
     * Main categories.
     *
     * The folders INSIDE these categories are completely dynamic.
     * No year or folder name is hardcoded.
     */
    private val categoryPages = listOf(
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

    override suspend fun getMainPage(
        page: Int,
        request: MainPageRequest
    ): HomePageResponse {

        val home = ArrayList<HomePageList>()

        /*
         * Every category is scanned directly from the server.
         * Any folder added later will automatically be detected.
         */
        for ((categoryName, categoryUrl) in categoryPages) {

            try {

                val folders = readDirectory(categoryUrl)

                val items = folders
                    .filter { it.attr("href").endsWith("/") }
                    .mapNotNull { element ->

                        val folderName =
                            element.text().trim()

                        val href =
                            element.attr("href")

                        if (
                            folderName.isBlank() ||
                            href.isBlank()
                        ) {
                            null
                        } else {

                            val folderUrl =
                                fixUrl(
                                    categoryUrl,
                                    href
                                )

                            newMovieSearchResponse(
                                cleanName(folderName),
                                folderUrl,
                                TvType.Movie
                            )
                        }
                    }

                if (items.isNotEmpty()) {

                    home.add(
                        HomePageList(
                            categoryName,
                            items
                        )
                    )
                }

            } catch (_: Exception) {
                // Ignore unavailable category
            }
        }

        return newHomePageResponse(home)
    }

    /*
     * Search
     *
     * Searches through:
     *
     * Category
     *   -> Folder
     *      -> Sub-folder
     *
     * Folder names are NOT assumed to be years.
     */
    override suspend fun search(
        query: String
    ): List<SearchResponse> {

        val results =
            ArrayList<SearchResponse>()

        for ((_, categoryUrl) in categoryPages) {

            try {

                val folders =
                    readDirectory(categoryUrl)

                for (folder in folders) {

                    if (
                        !folder.attr("href")
                            .endsWith("/")
                    ) {
                        continue
                    }

                    val folderName =
                        folder.text().trim()

                    val folderUrl =
                        fixUrl(
                            categoryUrl,
                            folder.attr("href")
                        )

                    /*
                     * Check the first folder itself.
                     */
                    if (
                        folderName.contains(
                            query,
                            ignoreCase = true
                        )
                    ) {

                        results.add(
                            newMovieSearchResponse(
                                cleanName(folderName),
                                folderUrl,
                                TvType.Movie
                            )
                        )
                    }

                    /*
                     * Search inside the folder.
                     * This works whether the folder is named:
                     *
                     * (2026)
                     * (2027)
                     * Nepal Travel
                     * সত্যচিত্রায় Films
                     * etc.
                     */
                    try {

                        val subFolders =
                            readDirectory(folderUrl)

                        for (subFolder in subFolders) {

                            if (
                                !subFolder.attr("href")
                                    .endsWith("/")
                            ) {
                                continue
                            }

                            val subFolderName =
                                subFolder.text().trim()

                            if (
                                !subFolderName.contains(
                                    query,
                                    ignoreCase = true
                                )
                            ) {
                                continue
                            }

                            val subFolderUrl =
                                fixUrl(
                                    folderUrl,
                                    subFolder.attr("href")
                                )

                            results.add(
                                newMovieSearchResponse(
                                    cleanName(subFolderName),
                                    subFolderUrl,
                                    TvType.Movie
                                )
                            )
                        }

                    } catch (_: Exception) {
                        // Continue searching
                    }
                }

            } catch (_: Exception) {
                // Continue with next category
            }
        }

        return results
    }

    /*
     * Opens a movie/video folder.
     *
     * Images/posters are completely ignored.
     * Only actual video files are searched.
     */
    override suspend fun load(
        url: String
    ): LoadResponse? {

        val document =
            app.get(url).document

        val title =
            cleanName(
                url
                    .trimEnd('/')
                    .substringAfterLast('/')
            )

        /*
         * Find the first supported video file.
         */
        val videoFile =
            document
                .select("a")
                .firstOrNull { element ->

                    val href =
                        element.attr("href")

                    isVideoFile(href)
                }
                ?: return null

        val videoUrl =
            fixUrl(
                url,
                videoFile.attr("href")
            )

        return newMovieLoadResponse(
            title,
            url,
            TvType.Movie,
            videoUrl
        )
    }

    /*
     * Sends the direct video URL to CloudStream player.
     */
    override suspend fun loadLinks(
        data: String,
        isCasting: Boolean,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ): Boolean {

        callback(
            newExtractorLink(
                source = this.name,
                name = this.name,
                url = data
            )
        )

        return true
    }

    /*
     * Reads a directory listing from the server.
     */
    private suspend fun readDirectory(
        url: String
    ): List<Element> {

        val document =
            app.get(url).document

        return document
            .select("a")
            .filter { element ->

                val href =
                    element.attr("href")

                href.isNotBlank() &&
                href != "../" &&
                href != "./"
            }
    }

    /*
     * Checks whether a link points to a video.
     */
    private fun isVideoFile(
        path: String
    ): Boolean {

        val lower =
            path.lowercase()

        return lower.endsWith(".mkv") ||
                lower.endsWith(".mp4") ||
                lower.endsWith(".webm") ||
                lower.endsWith(".avi") ||
                lower.endsWith(".mov") ||
                lower.endsWith(".m4v")
    }

    /*
     * Cleans folder/file names for display.
     */
    private fun cleanName(
        value: String
    ): String {

        return value
            .removeSuffix("/")
            .replace(
                Regex(
                    "\\.(mkv|mp4|webm|avi|mov|m4v)$",
                    RegexOption.IGNORE_CASE
                ),
                ""
            )
            .trim()
    }

    /*
     * Converts relative links into complete URLs.
     */
    private fun fixUrl(
        base: String,
        path: String
    ): String {

        if (
            path.startsWith(
                "http://",
                ignoreCase = true
            ) ||
            path.startsWith(
                "https://",
                ignoreCase = true
            )
        ) {
            return path
        }

        return if (
            path.startsWith("/")
        ) {

            /*
             * The server paths are private LAN paths.
             * Keep the original host from the path.
             */
            when {
                path.startsWith(
                    "/DHAKA-FLIX-7"
                ) -> {
                    "http://172.16.50.7$path"
                }

                path.startsWith(
                    "/DHAKA-FLIX-14"
                ) -> {
                    "http://172.16.50.14$path"
                }

                path.startsWith(
                    "/DHAKA-FLIX-12"
                ) -> {
                    "http://172.16.50.12$path"
                }

                else -> {
                    mainUrl.trimEnd('/') +
                            path
                }
            }

        } else {

            base.trimEnd('/') +
                    "/" +
                    path.trimStart('/')
        }
    }
}
