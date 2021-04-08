package com.chenyue404.activitylog

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import de.robv.android.xposed.IXposedHookLoadPackage
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers
import de.robv.android.xposed.callbacks.XC_LoadPackage

class ActivityHook : IXposedHookLoadPackage {

    private val PACKAGE_NAME = "android"
    private val TAG = "ActivityHook-"

    override fun handleLoadPackage(lpparam: XC_LoadPackage.LoadPackageParam) {
        val packageName = lpparam.packageName
        val classLoader = lpparam.classLoader

        if (packageName != PACKAGE_NAME) {
            return
        }

        when (Build.VERSION.SDK_INT) {
            Build.VERSION_CODES.O_MR1 -> {
                hookO_MR1(classLoader)
            }
            else -> log("暂不支持这个版本的Android")
        }
    }

    private fun log(str: String) {
        XposedBridge.log("$TAG-$str")
    }

    private fun hookO_MR1(classLoader: ClassLoader) {
        /**
         * int startActivityMayWait(IApplicationThread caller, int callingUid,
        String callingPackage, Intent intent, String resolvedType,
        IVoiceInteractionSession voiceSession, IVoiceInteractor voiceInteractor,
        IBinder resultTo, String resultWho, int requestCode, int startFlags,
        ProfilerInfo profilerInfo, WaitResult outResult,
        Configuration globalConfig, Bundle bOptions, boolean ignoreTargetSecurity, int userId,
        TaskRecord inTask, String reason)
         */

        val IApplicationThread =
            XposedHelpers.findClass("android.app.IApplicationThread", classLoader)
        val IVoiceInteractionSession =
            XposedHelpers.findClass("android.service.voice.IVoiceInteractionSession", classLoader)
        val IVoiceInteractor =
            XposedHelpers.findClass("com.android.internal.app.IVoiceInteractor", classLoader)
        val ProfilerInfo =
            XposedHelpers.findClass("android.app.ProfilerInfo", classLoader)
        val WaitResult =
            XposedHelpers.findClass("android.app.WaitResult", classLoader)
        val Configuration =
            XposedHelpers.findClass("android.content.res.Configuration", classLoader)
        val TaskRecord =
            XposedHelpers.findClass("com.android.server.am.TaskRecord", classLoader)

        XposedHelpers.findAndHookMethod(
            "com.android.server.am.ActivityStarter", classLoader, "startActivityMayWait",
            IApplicationThread, Int::class.java,
            String::class.java, Intent::class.java, String::class.java,
            IVoiceInteractionSession, IVoiceInteractor,
            IBinder::class.java, String::class.java, Int::class.java, Int::class.java,
            ProfilerInfo, WaitResult,
            Configuration, Bundle::class.java, Boolean::class.java, Int::class.java,
            TaskRecord, String::class.java,
            object : XC_MethodHook() {
                override fun afterHookedMethod(param: MethodHookParam) {
                    val callingPackage = param.args[2] as String
                    val intent: Intent = param.args[3] as Intent
                    val str = intent.transToStr(callingPackage)
                    log("\n$str")
                }
            }
        )
    }
}