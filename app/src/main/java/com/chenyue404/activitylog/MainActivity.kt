package com.chenyue404.activitylog

import android.app.Activity
import android.app.AlertDialog
import android.content.IntentFilter
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ScrollView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.gson.GsonBuilder
import com.google.gson.JsonObject
import com.google.gson.JsonParser

class MainActivity : Activity() {
    private lateinit var logReceiver: LogReceiver
    private val dataList = arrayListOf<JsonObject>()
    private val listAdapter = LogListAdapter(dataList)

    private lateinit var rvList: RecyclerView
    private lateinit var btStatus: ImageButton
    private lateinit var btClear: ImageButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        rvList = findViewById(R.id.rvList)
        btStatus = findViewById(R.id.btStatus)
        btClear = findViewById(R.id.btClear)

        rvList.adapter = LogListAdapter(dataList)
        logReceiver = LogReceiver {
            dataList.add(JsonParser.parseString(it).asJsonObject)
            listAdapter.notifyItemChanged(dataList.lastIndex)
            if (!rvList.canScrollVertically(1)) {
                rvList.scrollToPosition(dataList.lastIndex)
            }
        }
        registerReceiver(logReceiver, IntentFilter().apply {
            addAction(LogReceiver.action)
        })

        btClear.setOnClickListener {
            dataList.clear()
            listAdapter.notifyDataSetChanged()
        }
    }

    override fun onDestroy() {
        unregisterReceiver(logReceiver)
        super.onDestroy()
    }

    private class LogListAdapter(val dataList: ArrayList<JsonObject>) :
        RecyclerView.Adapter<LogListAdapter.ViewHolder>() {
        inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView)

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = ViewHolder(
            LayoutInflater.from(parent.context).inflate(R.layout.item_list, parent, false)
        )

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val jo = dataList[position]
            val sb = StringBuilder()
            sb.append("time\n")
                .append(jo["time"]?.asString ?: "")
                .append("\nfrom\n")
                .append(jo["from"]?.asString ?: "")
                .append("\nto\n")
                .append(jo["to"]?.asString ?: "")
            (holder.itemView as TextView).text = sb.toString()

            holder.itemView.setOnClickListener {
                AlertDialog.Builder(it.context)
                    .setView(ScrollView(it.context).apply {
                        layoutParams = ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.WRAP_CONTENT,
                            ViewGroup.LayoutParams.WRAP_CONTENT
                        )
                        addView(
                            TextView(it.context).apply {
                                layoutParams = ViewGroup.LayoutParams(
                                    ViewGroup.LayoutParams.WRAP_CONTENT,
                                    ViewGroup.LayoutParams.WRAP_CONTENT
                                )
                                text = GsonBuilder().setPrettyPrinting().create().toJson(jo)
                                setTextIsSelectable(true)
                            }
                        )
                    })
                    .create()
                    .show()
            }
        }

        override fun getItemCount() = dataList.size
    }
}