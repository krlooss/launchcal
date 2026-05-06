package com.launchcal

import android.appwidget.AppWidgetHost
import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.view.ContextThemeWrapper
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.FrameLayout
import androidx.activity.result.ActivityResultLauncher
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class LauncherPagerAdapter(
    private val apps: List<AppInfo>,
    private val widgetHost: AppWidgetHost,
    private val widgetManager: AppWidgetManager,
    private val prefs: SharedPreferences,
    private val activityContext: android.content.Context,
    private val bindWidgetLauncher: ActivityResultLauncher<Intent>,
    private val configWidgetLauncher: ActivityResultLauncher<Intent>,
    private val pickWidgetLauncher: ActivityResultLauncher<Intent>
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    var widgetContainer: FrameLayout? = null
        private set

    class WidgetViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val container: FrameLayout = view.findViewById(R.id.widgetContainer)
    }

    class AppListViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val searchBar: EditText = view.findViewById(R.id.searchBar)
        val recyclerView: RecyclerView = view.findViewById(R.id.appList)
    }

    override fun getItemViewType(position: Int) = position

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            0 -> WidgetViewHolder(inflater.inflate(R.layout.page_widget, parent, false))
            else -> AppListViewHolder(inflater.inflate(R.layout.page_app_list, parent, false))
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder) {
            is WidgetViewHolder -> {
                widgetContainer = holder.container
                loadSavedWidget(holder.container)
                holder.container.setOnLongClickListener {
                    val intent = Intent(it.context, WidgetPickerActivity::class.java)
                    pickWidgetLauncher.launch(intent)
                    true
                }
            }
            is AppListViewHolder -> {
                val adapter = AppListAdapter(apps)
                holder.recyclerView.layoutManager = LinearLayoutManager(holder.itemView.context)
                holder.recyclerView.adapter = adapter
                holder.searchBar.addTextChangedListener(object : TextWatcher {
                    override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                    override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                        adapter.filter(s?.toString().orEmpty())
                    }
                    override fun afterTextChanged(s: Editable?) {}
                })
            }
        }
    }

    override fun getItemCount() = 2

    private val widgetContext: Context
        get() = ContextThemeWrapper(activityContext, android.R.style.Theme_DeviceDefault)

    private fun loadSavedWidget(container: FrameLayout) {
        val widgetId = prefs.getInt("widget_id", -1)
        if (widgetId == -1) return

        val info = widgetManager.getAppWidgetInfo(widgetId) ?: return
        val view = widgetHost.createView(widgetContext, widgetId, info)
        view.setAppWidget(widgetId, info)
        container.removeAllViews()
        container.addView(view)
    }

    fun commitWidget(appWidgetId: Int) {
        val info = widgetManager.getAppWidgetInfo(appWidgetId) ?: return
        prefs.edit().putInt("widget_id", appWidgetId).apply()

        val container = widgetContainer ?: return
        val view = widgetHost.createView(widgetContext, appWidgetId, info)
        view.setAppWidget(appWidgetId, info)
        container.removeAllViews()
        container.addView(view)

        if (info.configure != null) {
            val configIntent = Intent().apply {
                component = info.configure
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
            }
            configWidgetLauncher.launch(configIntent)
        }
    }
}
