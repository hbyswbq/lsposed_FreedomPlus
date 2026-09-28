package io.github.fplus.core.hook

import android.annotation.SuppressLint
import android.view.View
import androidx.core.view.isVisible
import com.freegang.extension.asOrNull
import com.freegang.extension.findFieldGetValue
import com.ss.android.ugc.aweme.feed.model.Aweme
import com.ss.android.ugc.aweme.feed.ui.PenetrateTouchRelativeLayout
import io.github.fplus.core.base.BaseHook
import io.github.fplus.core.config.ConfigV1
import io.github.fplus.core.helper.DexkitBuilder
import io.github.xpler.core.XplerLog
import io.github.xpler.core.entity.NoneHook
import io.github.xpler.core.hookBlockRunning
import io.github.xpler.core.proxy.MethodParam

class HVideoViewHolder : BaseHook() {
    companion object {
        @get:Synchronized
        @set:Synchronized
        var aweme: Aweme? = null

        @get:Synchronized
        @set:Synchronized
        var currentContainer: PenetrateTouchRelativeLayout? = null

        /**
         * 根据播放状态应用清爽模式
         * @param isPlaying true=播放中(隐藏控制栏), false=暂停(显示控制栏)
         */
        @JvmStatic
        fun applyNeatMode(isPlaying: Boolean) {
            val config = ConfigV1.get()
            if (!config.isNeatMode || !config.neatModeState) return

            val visible = !isPlaying
            currentContainer?.isVisible = visible
            HMainActivity.toggleView(visible)
        }
    }

    private val config get() = ConfigV1.get()

    override fun setTargetClass(): Class<*> {
        return DexkitBuilder.videoViewHolderClazz ?: NoneHook::class.java
    }

    private fun getWidgetContainer(params: MethodParam): PenetrateTouchRelativeLayout? {
        return params.thisObject?.findFieldGetValue<PenetrateTouchRelativeLayout> {
            type(PenetrateTouchRelativeLayout::class.java)
        }
    }

    @OnBefore("isCleanMode")
    fun isCleanModeBefore(params: MethodParam, view: View?, bool: Boolean) {
        hookBlockRunning(params) {
            if (!config.isNeatMode)
                return

            if (!config.neatModeState)
                return

            setResultVoid()
        }.onFailure {
            XplerLog.e(it)
        }
    }

    @OnBefore("openCleanMode")
    fun openCleanModeBefore(params: MethodParam, bool: Boolean) {
        hookBlockRunning(params) {
            if (!config.isNeatMode)
                return

            if (!config.neatModeState)
                return

            setResultVoid()
        }.onFailure {
            XplerLog.e(it)
        }
    }

    @OnAfter("getAweme")
    fun getAwemeAfter(params: MethodParam) {
        hookBlockRunning(params) {
            HVideoViewHolder.aweme = result?.asOrNull()
        }.onFailure {
            XplerLog.e(it)
        }
    }

    @OnAfter("onViewHolderSelected")
    fun onViewHolderSelectedAfter(params: MethodParam, index: Int) {
        hookBlockRunning(params) {
            currentContainer = getWidgetContainer(params)
            // 选中时立即应用当前播放状态
            applyNeatMode(HPlayerController.isPlaying)
        }.onFailure {
            XplerLog.e(it)
        }
    }

    @OnAfter("onViewHolderUnSelected")
    fun onViewHolderUnSelectedAfter(params: MethodParam) {
        hookBlockRunning(params) {
            val container = getWidgetContainer(params)
            if (currentContainer === container) {
                currentContainer = null
            }
        }.onFailure {
            XplerLog.e(it)
        }
    }

    @OnBefore("onPause")
    fun onPauseBefore(params: MethodParam) {
        hookBlockRunning(params) {
            val container = getWidgetContainer(params)
            if (currentContainer === container) {
                currentContainer = null
            }
        }.onFailure {
            XplerLog.e(it)
        }
    }

    @OnAfter("onResume")
    fun onResumeAfter(params: MethodParam) {
        hookBlockRunning(params) {
            currentContainer = getWidgetContainer(params)
            applyNeatMode(HPlayerController.isPlaying)
        }.onFailure {
            XplerLog.e(it)
        }
    }
}
