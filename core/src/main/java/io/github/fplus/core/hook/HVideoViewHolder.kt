package io.github.fplus.core.hook

import android.annotation.SuppressLint
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

        @get:Synchronized
        @set:Synchronized
        var currentViewHolder: Any? = null

        /**
         * 调用抖音原生的 openCleanMode 方法, 同步清爽模式状态
         * @param clean true=开启清爽模式(隐藏控制栏), false=关闭清爽模式(显示控制栏)
         */
        @JvmStatic
        fun callOpenCleanMode(clean: Boolean) {
            val holder = currentViewHolder ?: return
            runCatching {
                val method = holder.javaClass.methods.firstOrNull {
                    it.name == "openCleanMode" && it.parameterTypes.size == 1 &&
                        (it.parameterTypes[0] == Boolean::class.java || it.parameterTypes[0] == Boolean::class.javaPrimitiveType)
                }
                method?.invoke(holder, clean)
            }.onFailure {
                XplerLog.e("callOpenCleanMode failed: ${it.message}")
            }
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

    /**
     * 监听抖音原生 openCleanMode 调用, 同步顶部/底部栏状态
     */
    @OnAfter("openCleanMode")
    fun openCleanModeAfter(params: MethodParam, bool: Boolean) {
        hookBlockRunning(params) {
            if (!config.isNeatMode || !config.neatModeState) return
            HMainActivity.toggleView(!bool)
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
            currentViewHolder = params.thisObject
            currentContainer = getWidgetContainer(params)
        }.onFailure {
            XplerLog.e(it)
        }
    }

    @OnAfter("onViewHolderUnSelected")
    fun onViewHolderUnSelectedAfter(params: MethodParam) {
        hookBlockRunning(params) {
            if (currentViewHolder === params.thisObject) {
                currentViewHolder = null
            }
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
            if (currentViewHolder === params.thisObject) {
                currentViewHolder = null
            }
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
            currentViewHolder = params.thisObject
            currentContainer = getWidgetContainer(params)
        }.onFailure {
            XplerLog.e(it)
        }
    }
}
