package com.launchcal

import android.content.ComponentName
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.PopupMenu
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class AppListAdapter(
    private var allApps: List<AppInfo>,
    private val favorites: FavoritesManager
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private var items: List<ListItem> = buildItems(allApps, "")
    private var currentQuery: String = ""

    private sealed class ListItem {
        data class App(val app: AppInfo) : ListItem()
        object Separator : ListItem()
    }

    class AppViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val icon: ImageView = view.findViewById(R.id.appIcon)
        val name: TextView = view.findViewById(R.id.appName)
    }

    class SeparatorViewHolder(view: View) : RecyclerView.ViewHolder(view)

    override fun getItemViewType(position: Int) = when (items[position]) {
        is ListItem.App -> 0
        is ListItem.Separator -> 1
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            1 -> SeparatorViewHolder(inflater.inflate(R.layout.item_separator, parent, false))
            else -> AppViewHolder(inflater.inflate(R.layout.item_app, parent, false))
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val item = items[position]
        if (item !is ListItem.App) return
        val app = item.app
        val h = holder as AppViewHolder
        h.icon.setImageDrawable(app.icon)
        h.name.text = app.name

        h.itemView.setOnClickListener {
            val intent = Intent(Intent.ACTION_MAIN).apply {
                addCategory(Intent.CATEGORY_LAUNCHER)
                component = ComponentName(app.packageName, app.activityName)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED
            }
            it.context.startActivity(intent)
        }

        h.itemView.setOnLongClickListener { view ->
            val popup = PopupMenu(view.context, view)
            val isFav = favorites.isFavorite(app.packageName)
            if (isFav) {
                popup.menu.add("Remove from favorites")
            } else {
                popup.menu.add("Add to favorites")
            }
            popup.menu.add("App info")

            popup.setOnMenuItemClickListener { menuItem ->
                when (menuItem.title) {
                    "Add to favorites" -> {
                        favorites.add(app.packageName)
                        rebuild()
                    }
                    "Remove from favorites" -> {
                        favorites.remove(app.packageName)
                        rebuild()
                    }
                    "App info" -> {
                        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                            data = Uri.parse("package:${app.packageName}")
                        }
                        view.context.startActivity(intent)
                    }
                }
                true
            }
            popup.show()
            true
        }
    }

    override fun getItemCount() = items.size

    private fun buildItems(apps: List<AppInfo>, query: String): List<ListItem> {
        if (query.isNotEmpty()) {
            return apps.filter { AppSearch.matches(it.name, query) }
                .map { ListItem.App(it) }
        }

        val favs = apps.filter { favorites.isFavorite(it.packageName) }
        val rest = apps.filter { !favorites.isFavorite(it.packageName) }
        val result = mutableListOf<ListItem>()
        favs.forEach { result.add(ListItem.App(it)) }
        if (favs.isNotEmpty() && rest.isNotEmpty()) {
            result.add(ListItem.Separator)
        }
        rest.forEach { result.add(ListItem.App(it)) }
        return result
    }

    private fun rebuild() {
        items = buildItems(allApps, currentQuery)
        notifyDataSetChanged()
    }

    fun filter(query: String) {
        currentQuery = query
        rebuild()
    }

    fun updateApps(apps: List<AppInfo>) {
        allApps = apps
        rebuild()
    }
}
