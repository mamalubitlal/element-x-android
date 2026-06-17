package io.element.android.libraries.core.data

import android.content.Context
import android.content.SharedPreferences

class WatchedAdsStore(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("watched_ads_prefs", Context.MODE_PRIVATE)

    var watchedAds: Int
        get() = prefs.getInt("watched_ads", 0)
        set(value) = prefs.edit().putInt("watched_ads", value).apply()
}

object WatchedAdsStoreHolder {
    var instance: WatchedAdsStore? = null
}
