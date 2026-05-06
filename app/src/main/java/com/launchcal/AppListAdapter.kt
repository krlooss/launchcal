package com.launchcal

import android.content.ComponentName
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class AppListAdapter(private var allApps: List<AppInfo>) :
    RecyclerView.Adapter<AppListAdapter.ViewHolder>() {

    private var filtered: List<AppInfo> = allApps
    private var currentQuery: String = ""

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val icon: ImageView = view.findViewById(R.id.appIcon)
        val name: TextView = view.findViewById(R.id.appName)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_app, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val app = filtered[position]
        holder.icon.setImageDrawable(app.icon)
        holder.name.text = app.name
        holder.itemView.setOnClickListener {
            val intent = Intent(Intent.ACTION_MAIN).apply {
                addCategory(Intent.CATEGORY_LAUNCHER)
                component = ComponentName(app.packageName, app.activityName)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED
            }
            it.context.startActivity(intent)
        }
    }

    override fun getItemCount() = filtered.size

    fun filter(query: String) {
        currentQuery = query
        filtered = if (query.isEmpty()) allApps
        else allApps.filter { AppSearch.matches(it.name, query) }
        notifyDataSetChanged()
    }

    fun updateApps(apps: List<AppInfo>) {
        allApps = apps
        filter(currentQuery)
    }
}
