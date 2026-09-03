package com.dhakaftp

import com.lagradost.cloudstream3.MainAPI
import com.lagradost.cloudstream3.TvType
import com.lagradost.cloudstream3.MainPageRequest
import com.lagradost.cloudstream3.HomePageResponse

class DhakaFTP : MainAPI() {

    override var name = "Dhaka FTP"

    override val supportedTypes = setOf(
        TvType.Movie,
        TvType.TvSeries,
        TvType.Anime
    )

    override val hasMainPage = true

    override val hasQuickSearch = true

    override suspend fun getMainPage(
        page: Int,
        request: MainPageRequest
    ): HomePageResponse? {

        return null
    }

    override suspend fun search(query: String) =
        emptyList<com.lagradost.cloudstream3.SearchResponse>()
}
