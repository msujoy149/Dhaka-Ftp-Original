package com.dhakaftp

import com.lagradost.cloudstream3.MainAPI
import com.lagradost.cloudstream3.TvType

class DhakaFTP : MainAPI() {

    override var name = "Dhaka FTP"

    override val supportedTypes = setOf(
        TvType.Movie,
        TvType.TvSeries,
        TvType.Anime
    )

    override val hasMainPage = true
    override val hasQuickSearch = true

    override val mainPage = mainPageOf(
        "english" to "English",
        "hindi" to "Hindi",
        "bangla" to "Bangla",
        "south-indian" to "South Indian",
        "tv" to "TV / Web Series",
        "k-drama" to "K-Drama",
        "anime" to "Anime"
    )
}
