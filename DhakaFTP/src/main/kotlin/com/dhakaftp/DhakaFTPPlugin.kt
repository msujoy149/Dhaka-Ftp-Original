package com.dhakaftp

import com.lagradost.cloudstream3.plugins.CloudstreamPlugin
import com.lagradost.cloudstream3.plugins.BasePlugin

@CloudstreamPlugin
class DhakaFTPPlugin : BasePlugin() {

    override fun load() {
        registerMainAPI(DhakaFTP())
    }
}
