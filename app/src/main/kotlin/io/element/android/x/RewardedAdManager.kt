package io.element.android.x

import com.appodeal.ads.Appodeal
import com.appodeal.ads.RewardedVideoCallbacks
import io.element.android.libraries.core.data.AdProvider
import io.element.android.libraries.core.data.WatchedAdsStore

class RewardedAdManager(
    private val activity: android.app.Activity,
    private val watchedAdsStore: WatchedAdsStore
) : AdProvider {
    private var isEarningReward: Boolean = false

    init {
        Appodeal.setRewardedVideoCallbacks(object : RewardedVideoCallbacks {
            override fun onRewardedVideoLoaded(isPrecache: Boolean) {}
            override fun onRewardedVideoFailedToLoad() {}
            override fun onRewardedVideoShown() {}
            override fun onRewardedVideoShowFailed() {}
            override fun onRewardedVideoClicked() {}
            override fun onRewardedVideoFinished(amount: Double, name: String?) {
                if (isEarningReward) {
                    watchedAdsStore.watchedAds += 1
                }
            }
            override fun onRewardedVideoClosed(finished: Boolean) {}
            override fun onRewardedVideoExpired() {}
        })
    }

    fun showAd(isEarningReward: Boolean, onAdFinished: () -> Unit) {
        this.isEarningReward = isEarningReward
        if (Appodeal.isLoaded(Appodeal.REWARDED_VIDEO)) {
            Appodeal.show(activity, Appodeal.REWARDED_VIDEO)
        }
    }
}

object RewardedAdManagerHolder {
    var instance: RewardedAdManager? = null
}
