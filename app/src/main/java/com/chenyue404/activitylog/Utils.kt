package com.chenyue404.activitylog

import android.content.Intent
import android.os.Bundle
import com.google.gson.GsonBuilder
import com.google.gson.JsonArray
import com.google.gson.JsonObject

class Utils {
}

fun Intent.transToStr(from: String = "null", bundle: Bundle? = null): String {
    val jsonObject = JsonObject().apply {
        addProperty("from", from)
        addProperty("to", component?.className ?: "null")
        addProperty("action", action)
        addProperty("clipData", clipData.toString())
        addProperty("flags", flags)
        addProperty("dataString", dataString)
        addProperty("type", type)
        addProperty("componentName", component.toString())
        addProperty("scheme", scheme)
        addProperty("package", `package`)
        addProperty("categories", categories?.joinToString() ?: "null")
        extras?.let { bundle ->
            val jsonArray = JsonArray()
            bundle.keySet().forEach {
                val value = bundle.get(it)
                value?.run {
                    jsonArray.add(JsonObject().apply {
                        addProperty("key", it)
                        addProperty("value", value.toString())
                        addProperty("class", value.javaClass.name)
                    })
                }

            }
            add("intentExtras", jsonArray)
        }
        bundle?.let { bundle ->
            val jsonArray = JsonArray()
            bundle.keySet().forEach {
                val value = bundle.get(it)
                value?.run {
                    jsonArray.add(JsonObject().apply {
                        addProperty("key", it)
                        addProperty("value", value.toString())
                        addProperty("class", value.javaClass.name)
                    })
                }

            }
            add("bundle", jsonArray)
        }
    }

    return GsonBuilder().setPrettyPrinting().create().toJson(jsonObject)
}
