package io.github.fplus.core.hook

import android.view.View
import androidx.core.view.updatePadding
import com.freegang.extension.dip2px
import com.ss.android.ugc.aweme.feed.ui.PenetrateTouchRelativeLayout
import io.github.fplus.core.base.BaseHook
import io.github.fplus.core.config.ConfigV1
import io.github.xpler.core.XplerLog
import io.github.xpler.core.hookBlockRunning
import io.github.xpler.core.proxy.MethodParam
import io.github.xpler.core.thisViewGroup

/**
 * 修复说明:
 * 原实现实现了 CallMethods 接口, 会 hook PenetrateTouchRelativeLayout 的 ALL 方法,
 * 在 callOnAfterMethods 中每次方法调用后都执行 updatePadding(bottom=...).
 *
 * PenetrateTouchRelativeLayout 是视频播放页核心容器, measure/layout/draw/
 * requestLayout/invalidate 等方法每秒调用数十次. 每次 after hook 都 setPadding
 * 会触发 requestLayout -> 重新 measure/layout -> 再次触发被 hook 的方法 -> 再次
 * setPadding, 形成布局-测量循环, 是刷视频越刷越卡的核心元凶之一.
 *
 * 修复: 移除 CallMethods, 改为仅在 onAttachedToWindow 时设置一次 bottomPadding.
 */
class HPenetrateTouchRelativeLayout : BaseHook() {
    private val config get() = ConfigV1.get()

    override fun setTargetClass(): Class<*> {
        return PenetrateTouchRelativeLayout::class.java
    }

    @OnBefore("setVisibility")
    fun setVisibilityBefore(params: MethodParam, visibility: Int) {
        hookBlockRunning(params) {
            if (!config.isNeatMode) {
                return
            }
            if (!config.neatModeState) {
                return
            }
            if (visibility == View.GONE/* || visibility == View.INVISIBLE*/) {
                return
            }
            if (HPlayerController.isPlaying) {
                args[0] = View.GONE
                HMainActivity.toggleView(false)
            } else {
                args[0] = View.VISIBLE
                HMainActivity.toggleView(true)
            }
        }.onFailure {
            XplerLog.e(it)
        }
    }

    @OnBefore
    fun methodBefore(params: MethodParam, visibility: Int, string: String?) {
        hookBlockRunning(params) {
            setVisibilityBefore(params, visibility)
        }.onFailure {
            XplerLog.e(it)
        }
    }

    /**
     * 修复: 仅在 View attach 到窗口时设置一次 bottomPadding,
     * 替代原 CallMethods.callOnAfterMethods 中每方法调用都 setPadding 的实现。
     */
    @OnAfter("onAttachedToWindow")
    fun onAttachedToWindowAfter(params: MethodParam) {
        hookBlockRunning(params) {
            if (!config.isImmersive) {
                return@hookBlockRunning
            }
            thisViewGroup.apply {
                val bottomPadding = 58f.dip2px() // BottomTabBarHeight
                updatePadding(bottom = bottomPadding)
            }
        }.onFailure {
            XplerLog.e(it)
        }
    }
}
