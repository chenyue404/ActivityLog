package com.chenyue404.activitylog

import android.app.ActivityOptions
import android.app.AndroidAppHelper
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Handler
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
        hookCheckBroadcastFromSystem(classLoader)

        val osVersion = Build.VERSION.SDK_INT
        log("osVersion=$osVersion")
        if (osVersion <= 27) {
            val activityRecordClass =
                XposedHelpers.findClass(
                    "com.android.server.am.ActivityRecord",
                    classLoader
                )
            val iVoiceInteractionSessionClass =
                XposedHelpers.findClass(
                    "android.service.voice.IVoiceInteractionSession",
                    classLoader
                )
            val iVoiceInteractorClass =
                XposedHelpers.findClass(
                    "com.android.internal.app.IVoiceInteractor",
                    classLoader
                )
            val taskRecordClass =
                XposedHelpers.findClass("com.android.server.am.TaskRecord", classLoader)
            if (osVersion < 26) {
                //24,25
                XposedHelpers.findAndHookMethod(
                    "com.android.server.am.ActivityStarter",
                    classLoader,
                    "startActivityUnchecked",
                    activityRecordClass,
                    activityRecordClass,
                    iVoiceInteractionSessionClass,
                    iVoiceInteractorClass,
                    Int::class.java,
                    Boolean::class.java,
                    ActivityOptions::class.java,
                    taskRecordClass,
                    hookStartActivityUnchecked()
                )
            } else {
                //26,27
                XposedHelpers.findAndHookMethod(
                    "com.android.server.am.ActivityStarter",
                    classLoader,
                    "startActivityUnchecked",
                    activityRecordClass,
                    activityRecordClass,
                    iVoiceInteractionSessionClass,
                    iVoiceInteractorClass,
                    Int::class.java,
                    Boolean::class.java,
                    ActivityOptions::class.java,
                    taskRecordClass,
                    "[Lcom.android.server.am.ActivityRecord;",
                    hookStartActivityUnchecked()
                )
            }
        } else if (osVersion <= 30) {
            val className = if (osVersion == 28) {
                "com.android.server.am.ActivityStarter"
            } else {
                "com.android.server.wm.ActivityStarter"
            }
            hookExecute(className, classLoader)
        } else {
            log("暂不支持这个版本的Android")
        }
    }

    private fun log(str: String) {
        XposedBridge.log("$tag-$str")
    }

    private fun hookExecute(className: String, classLoader: ClassLoader) {
        XposedHelpers.findAndHookMethod(
            className, classLoader, "execute",
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
                    val str = intent.transToStr(callingPackage, bundle)
                    log("\n$str")
                    sendBroadcast(str)
                }
            }
        )
    }

    private fun hookStartActivityUnchecked() = object : XC_MethodHook() {
        override fun afterHookedMethod(param: MethodHookParam) {
            log("hookStartActivityUnchecked")
            val activityRecord = param.args[0]
            val activityOptions = param.args[6]?.let { it as ActivityOptions }
            val bundle = activityOptions?.toBundle()
            val callingPackage =
                XposedHelpers.getObjectField(activityRecord, "launchedFromPackage")?.let {
                    it as String
                } ?: ""
            val intent = XposedHelpers.getObjectField(activityRecord, "intent")?.let {
                it as Intent
            } ?: return
            val str = intent.transToStr(callingPackage, bundle)
            log("\n$str")
            sendBroadcast(str)
        }
    }

    private fun sendBroadcast(str: String) {
        val context = AndroidAppHelper.currentApplication().applicationContext
        Handler(context.mainLooper).post {
            context.sendBroadcast(Intent().apply {
                action = LogReceiver.action
                putExtra(LogReceiver.extraKey, str)
            })
        }
    }

    /**
     * 解除系统不能发自定义广播的限制
     */
    private fun hookCheckBroadcastFromSystem(classLoader: ClassLoader) {
        val ProcessRecord =
            XposedHelpers.findClass("com.android.server.am.ProcessRecord", classLoader)
        XposedHelpers.findAndHookMethod(
            "com.android.server.am.ActivityManagerService", classLoader,
            "checkBroadcastFromSystem",
            Intent::class.java,
            ProcessRecord,
            String::class.java,
            Int::class.java,
            Boolean::class.java,
            List::class.java,
            object : XC_MethodHook() {
                override fun beforeHookedMethod(param: MethodHookParam) {
                    val intent: Intent = param.args[0] as Intent
                    if (intent.action == LogReceiver.action) {
                        param.args[4] = true
                    }
                }
            }
        )
    }
}