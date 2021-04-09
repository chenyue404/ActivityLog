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

    private val packageName = "android"
    private val tag = "ActivityHook-"

    override fun handleLoadPackage(lpparam: XC_LoadPackage.LoadPackageParam) {
        val packageName = lpparam.packageName
        val classLoader = lpparam.classLoader

        if (packageName != this.packageName) {
            return
        }

        when (Build.VERSION.SDK_INT) {
            Build.VERSION_CODES.O_MR1 -> hook27(classLoader)
            Build.VERSION_CODES.Q,
            Build.VERSION_CODES.R -> hook30(classLoader)
            else -> log("暂不支持这个版本的Android")
        }
    }

    private fun log(str: String) {
        XposedBridge.log("$tag-$str")
    }

    private fun hook27(classLoader: ClassLoader) {
        /**
         * int startActivityMayWait(IApplicationThread caller, int callingUid,
        String callingPackage, Intent intent, String resolvedType,
        IVoiceInteractionSession voiceSession, IVoiceInteractor voiceInteractor,
        IBinder resultTo, String resultWho, int requestCode, int startFlags,
        ProfilerInfo profilerInfo, WaitResult outResult,
        Configuration globalConfig, Bundle bOptions, boolean ignoreTargetSecurity, int userId,
        TaskRecord inTask, String reason)
         */

        val iApplicationThreadClass =
            XposedHelpers.findClass("android.app.IApplicationThread", classLoader)
        val iVoiceInteractionSessionClass =
            XposedHelpers.findClass("android.service.voice.IVoiceInteractionSession", classLoader)
        val iVoiceInteractorClass =
            XposedHelpers.findClass("com.android.internal.app.IVoiceInteractor", classLoader)
        val profilerInfoClass =
            XposedHelpers.findClass("android.app.ProfilerInfo", classLoader)
        val waitResultClass =
            XposedHelpers.findClass("android.app.WaitResult", classLoader)
        val configurationClass =
            XposedHelpers.findClass("android.content.res.Configuration", classLoader)
        val taskRecordClass =
            XposedHelpers.findClass("com.android.server.am.TaskRecord", classLoader)

        XposedHelpers.findAndHookMethod(
            "com.android.server.am.ActivityStarter", classLoader, "startActivityMayWait",
            iApplicationThreadClass, Int::class.java,
            String::class.java, Intent::class.java, String::class.java,
            iVoiceInteractionSessionClass, iVoiceInteractorClass,
            IBinder::class.java, String::class.java, Int::class.java, Int::class.java,
            profilerInfoClass, waitResultClass,
            configurationClass, Bundle::class.java, Boolean::class.java, Int::class.java,
            taskRecordClass, String::class.java,
            object : XC_MethodHook() {
                override fun afterHookedMethod(param: MethodHookParam) {
                    val callingPackage = param.args[2] as String
                    val intent: Intent = param.args[3] as Intent
                    val bundle: Bundle = param.args[14] as Bundle
                    val str = intent.transToStr(callingPackage, bundle)
                    log("\n$str")
                }
            }
        )
    }


    private fun hook30(classLoader: ClassLoader) {
        XposedHelpers.findAndHookMethod(
            "com.android.server.wm.ActivityStarter", classLoader, "execute",
            object : XC_MethodHook() {
                override fun beforeHookedMethod(param: MethodHookParam) {
                    val request = XposedHelpers.getObjectField(param.thisObject, "mRequest")
                    val callingPackage =
                        XposedHelpers.getObjectField(request, "callingPackage")?.let {
                            it as String
                        } ?: ""
                    val safeActivityOptions =
                        XposedHelpers.getObjectField(request, "activityOptions")
                    var bundle: Bundle? = null
                    safeActivityOptions?.run {
                        val activityOptions =
                            XposedHelpers.getObjectField(
                                safeActivityOptions,
                                "mOriginalOptions"
                            )
                        activityOptions?.let {
                            bundle = XposedHelpers.callMethod(it, "toBundle") as Bundle
                        }
                    }
                    val intent =
                        XposedHelpers.getObjectField(request, "intent") as Intent
                    log("\n${intent.transToStr(callingPackage, bundle)}")
                }
            }
        )
    }
}