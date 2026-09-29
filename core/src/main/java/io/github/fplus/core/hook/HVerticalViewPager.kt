package io.github.fplus.core.hook

import com.freegang.extension.findFieldGetValue
import com.freegang.extension.findFieldSetValue
import com.freegang.ktutils.text.KTextUtils
import com.ss.android.ugc.aweme.common.widget.VerticalViewPager
import com.ss.android.ugc.aweme.feed.model.Aweme
import com.ss.android.ugc.aweme.follow.presenter.FollowFeed
import io.github.fplus.core.base.BaseHook
import io.github.fplus.core.config.ConfigV1
import io.github.fplus.core.helper.DexkitBuilder
import io.github.xpler.core.XplerLog
import io.github.xpler.core.hookClass
import io.github.xpler.core.lparam

class HVerticalViewPager : BaseHook() {
    companion object {
        var isFilterLive = false
        var isFilterImage = false
        var isFilterAd = false
        var isFilterLongVideo = false
        var isFilterRecommendedCards = false
        var isFilterRecommendedMerchants = false
        var isFilterEmptyDesc = false
    }

    private val config get() = ConfigV1.get()

    private val filterKeywordsAndTypes: Set<String>
        get() = config.videoFilterKeywords
            .replace("，", ",")
            .replace("\\s".toRegex(), "")
            .removePrefix(",").removeSuffix(",")
            .split(",")
            .filter { it.isNotEmpty() }
            .toSet()

    private val keywordsRegex: Regex
        get() = filterKeywordsAndTypes
            .filter { !config.videoFilterTypes.contains(it) }
            .joinToString("|")
            .replace("\\|+".toRegex(), "|")
            .toRegex()

    override fun setTargetClass(): Class<*> {
        return VerticalViewPager::class.java
    }

    private fun filterAwemeList(items: List<Aweme>): List<Aweme> {
        resetFilter()
        val awemes = mutableListOf<Aweme>()
        for (item in items) {
            needAweme(item) ?: continue
            awemes.add(item)
        }
        return awemes
    }

    private fun filterFollowFeedList(items: List<FollowFeed>): List<FollowFeed> {
        resetFilter()
        val followFeeds = mutableListOf<FollowFeed>()
        for (item in items) {
            needAweme(item.aweme) ?: continue
            followFeeds.add(item)
        }
        return followFeeds
    }

    private fun resetFilter() {
        isFilterLive = false
        isFilterImage = false
        isFilterAd = false
        isFilterLongVideo = false
        isFilterRecommendedCards = false
        isFilterRecommendedMerchants = false
        isFilterEmptyDesc = false
        for (s in filterKeywordsAndTypes) {
            when (s) {
                "直播" -> isFilterLive = true
                "图文" -> isFilterImage = true
                "广告" -> isFilterAd = true
                "长视频" -> isFilterLongVideo = true
                "推荐卡片" -> isFilterRecommendedCards = true
                "推荐商家" -> isFilterRecommendedMerchants = true
                "空文案" -> isFilterEmptyDesc = true
            }
        }
    }

    private fun needAweme(aweme: Aweme): Aweme? {
        return when {
            isFilterLive && aweme.isLive -> null
            isFilterImage && aweme.isMultiImage -> null
            isFilterAd && aweme.isAd -> null
            isFilterLongVideo && aweme.isCopyRightLongVideo -> null
            isFilterRecommendedCards && aweme.awemeType == 145 -> null
            isFilterRecommendedMerchants && aweme.awemeType == 140 -> null
            isFilterEmptyDesc && KTextUtils.isEmpty(aweme.desc) -> null
            keywordsRegex.pattern.trim().isNotEmpty()
                    && KTextUtils.get(aweme.desc).contains(keywordsRegex) -> null
            else -> aweme
        }
    }

    override fun onInit() {
        DexkitBuilder.recommendFeedFetchPresenterClazz?.runCatching {
            lparam.hookClass(this)
                .method("onSuccess") {
                    onBefore {
                        if (!config.isVideoFilter) return@onBefore

                        val mModel = thisObject?.findFieldGetValue<Any> { name("mModel") }
                        val mData = mModel?.findFieldGetValue<Any> { name("mData") }
                        if (mData?.javaClass?.name?.contains("FeedItemList") == true) {
                            val items = mData.findFieldGetValue<List<Aweme>> { name("items") } ?: emptyList()
                            if (items.size < 3) return@onBefore

                            mData.findFieldSetValue(filterAwemeList(items)) { name("items") }
                        }
                    }
                }
        }?.onFailure {
            XplerLog.e(it)
        }

        DexkitBuilder.fullFeedFollowFetchPresenterClazz?.runCatching {
            lparam.hookClass(this)
                .method("onSuccess") {
                    onBefore {
                        if (!config.isVideoFilter) return@onBefore

                        val mModel = thisObject?.findFieldGetValue<Any> { name("mModel") }
                        val mData = mModel?.findFieldGetValue<Any> { name("mData") }
                        if (mData?.javaClass?.name?.contains("FollowFeedList") == true) {
                            val mItems = mData.findFieldGetValue<List<FollowFeed>> { name("mItems") } ?: emptyList()
                            if (mItems.size < 3) return@onBefore

                            mData.findFieldSetValue(filterFollowFeedList(mItems)) { name("mItems") }
                        }
                    }
                }
        }?.onFailure {
            XplerLog.e(it)
        }
    }
}
