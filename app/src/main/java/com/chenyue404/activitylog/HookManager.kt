package com.chenyue404.activitylog

import de.robv.android.xposed.IXposedHookLoadPackage
import de.robv.android.xposed.callbacks.XC_LoadPackage

object HookManager {

    private val hookClassList by lazy(LazyThreadSafetyMode.NONE) {
        hashSetOf<IXposedHookLoadPackage>()
    }

    fun startHook(lpparam: XC_LoadPackage.LoadPackageParam) {
        hookClassList.forEach { it.handleLoadPackage(lpparam) }
    }

    private fun registHookClass(hookerList: List<IXposedHookLoadPackage>) {
        hookClassList.addAll(hookerList)
    }

    init {
        registHookClass(
            arrayListOf(
                ActivityHook()
            )
        )
    }
}