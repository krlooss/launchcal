package com.launchcal

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProviderInfo
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class WidgetPickerActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_widget_picker)

        val widgetManager = AppWidgetManager.getInstance(this)
        val widgets = widgetManager.getInstalledProvidersForProfile(android.os.Process.myUserHandle())
            .sortedBy { it.loadLabel(packageManager) }

        val list = findViewById<RecyclerView>(R.id.widgetList)
        list.layoutManager = LinearLayoutManager(this)
        list.adapter = WidgetAdapter(widgets) { info ->
            val resultIntent = Intent().apply {
                putExtra("provider", info.provider.flattenToString())
            }
            setResult(RESULT_OK, resultIntent)
            finish()
        }
    }

    private class WidgetAdapter(
        private val widgets: List<AppWidgetProviderInfo>,
        private val onClick: (AppWidgetProviderInfo) -> Unit
    ) : RecyclerView.Adapter<WidgetAdapter.ViewHolder>() {

        class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val preview: ImageView = view.findViewById(R.id.widgetPreview)
            val name: TextView = view.findViewById(R.id.widgetName)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_widget, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val info = widgets[position]
            val pm = holder.itemView.context.packageManager
            holder.name.text = info.loadLabel(pm)
            val icon = info.loadPreviewImage(holder.itemView.context, 0)
                ?: info.loadIcon(holder.itemView.context, 0)
            holder.preview.setImageDrawable(icon)
            holder.itemView.setOnClickListener { onClick(info) }
        }

        override fun getItemCount() = widgets.size
    }
}
