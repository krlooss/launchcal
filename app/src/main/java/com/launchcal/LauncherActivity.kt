package com.launchcal

import android.Manifest
import android.app.AlertDialog
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Bundle
import android.provider.MediaStore
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import android.content.ActivityNotFoundException
import android.provider.AlarmClock
import android.widget.LinearLayout
import android.widget.TextClock

class LauncherActivity : AppCompatActivity() {

    private lateinit var viewPager: ViewPager2
    private lateinit var appAdapter: AppListAdapter
    private lateinit var favoritesManager: FavoritesManager
    private lateinit var calendarPrefs: CalendarPrefs
    private lateinit var dockManager: DockManager
    private var calendarAdapter: CalendarAdapter? = null
    private var packageReceiver: BroadcastReceiver? = null
    private var searchBar: EditText? = null
    private var calendarDays: Int = 7
    private var calendarHolder: CalendarViewHolder? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_launcher)

        viewPager = findViewById(R.id.viewPager)
        val prefs = getSharedPreferences("launcher", MODE_PRIVATE)
        favoritesManager = FavoritesManager(prefs)
        calendarPrefs = CalendarPrefs(prefs)
        dockManager = DockManager(prefs)
        appAdapter = AppListAdapter(loadApps(), favoritesManager)

        viewPager.adapter = PagerAdapter()
        viewPager.setCurrentItem(1, false)

        registerPackageReceiver()
        requestCalendarPermission()
        showOnboardingIfNeeded()
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        viewPager.setCurrentItem(1, true)
    }

    override fun onResume() {
        super.onResume()
        searchBar?.text?.clear()
        refreshCalendar()
        val prefs = getSharedPreferences("launcher", MODE_PRIVATE)
        if (prefs.getBoolean("onboarding_done", false) && !prefs.getBoolean("launcher_prompt_shown", false)) {
            promptDefaultLauncher()
            prefs.edit().putBoolean("launcher_prompt_shown", true).apply()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        packageReceiver?.let { unregisterReceiver(it) }
    }

    private fun showOnboardingIfNeeded() {
        val prefs = getSharedPreferences("launcher", MODE_PRIVATE)
        if (!prefs.getBoolean("onboarding_done", false)) {
            startActivity(Intent(this, OnboardingActivity::class.java))
        }
    }

    private fun promptDefaultLauncher() {
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
        val resolveInfo = packageManager.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY)
        if (resolveInfo?.activityInfo?.packageName != packageName) {
            AlertDialog.Builder(this)
                .setTitle("Default launcher")
                .setMessage("Set LaunchCal as your default launcher?")
                .setPositiveButton("Yes") { _, _ ->
                    val intent = Intent(android.provider.Settings.ACTION_HOME_SETTINGS)
                    startActivity(intent)
                }
                .setNegativeButton("No", null)
                .show()
        }
    }

    private fun requestCalendarPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CALENDAR) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.READ_CALENDAR), 100)
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 100 && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            refreshCalendar()
        }
    }

    private fun refreshCalendar() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CALENDAR) == PackageManager.PERMISSION_GRANTED) {
            val enabledIds = calendarPrefs.getEnabledCalendarIds()
            val events = CalendarHelper.getUpcomingEvents(contentResolver, calendarDays, enabledIds)
            calendarAdapter?.updateEvents(events)
        }
    }

    private fun registerPackageReceiver() {
        packageReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                appAdapter.updateApps(loadApps())
            }
        }
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_PACKAGE_ADDED)
            addAction(Intent.ACTION_PACKAGE_REMOVED)
            addAction(Intent.ACTION_PACKAGE_REPLACED)
            addDataScheme("package")
        }
        registerReceiver(packageReceiver, filter)
    }

    private fun loadApps(): List<AppInfo> {
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        return packageManager.queryIntentActivities(intent, PackageManager.MATCH_ALL)
            .map { resolveInfo ->
                AppInfo(
                    name = resolveInfo.loadLabel(packageManager).toString(),
                    packageName = resolveInfo.activityInfo.packageName,
                    activityName = resolveInfo.activityInfo.name,
                    icon = resolveInfo.loadIcon(packageManager)
                )
            }
            .sortedBy { it.name.lowercase() }
    }

    private inner class PagerAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

        override fun getItemViewType(position: Int) = position
        override fun getItemCount() = 2

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
            val inflater = LayoutInflater.from(parent.context)
            return when (viewType) {
                0 -> CalendarViewHolder(inflater.inflate(R.layout.page_calendar, parent, false))
                else -> AppListViewHolder(inflater.inflate(R.layout.page_app_list, parent, false))
            }
        }

        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
            when (holder) {
                is CalendarViewHolder -> bindCalendar(holder)
                is AppListViewHolder -> bindAppList(holder)
            }
        }
    }

    private class CalendarViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val clock: TextClock = view.findViewById(R.id.calendarClock)
        val list: RecyclerView = view.findViewById(R.id.calendarList)
        val empty: TextView = view.findViewById(R.id.calendarEmpty)
        val loadMore: TextView = view.findViewById(R.id.loadMoreButton)
        val settings: ImageView = view.findViewById(R.id.calendarSettings)
    }

    private class AppListViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val searchBar: EditText = view.findViewById(R.id.searchBar)
        val appList: RecyclerView = view.findViewById(R.id.appList)
        val dockBar: LinearLayout = view.findViewById(R.id.dockBar)
    }

    private fun bindCalendar(holder: CalendarViewHolder) {
        calendarHolder = holder

        holder.clock.setOnClickListener {
            try {
                startActivity(Intent(AlarmClock.ACTION_SHOW_ALARMS))
            } catch (_: ActivityNotFoundException) {
                startActivity(Intent(android.provider.Settings.ACTION_DATE_SETTINGS))
            }
        }

        calendarAdapter = CalendarAdapter(emptyList())
        holder.list.layoutManager = LinearLayoutManager(this)
        holder.list.adapter = calendarAdapter

        loadCalendarEvents(holder)

        holder.loadMore.setOnClickListener {
            calendarDays += 10
            loadCalendarEvents(holder)
        }

        holder.settings.setOnClickListener { showCalendarPicker() }
    }

    private fun loadCalendarEvents(holder: CalendarViewHolder) {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CALENDAR) == PackageManager.PERMISSION_GRANTED) {
            val enabledIds = calendarPrefs.getEnabledCalendarIds()
            val events = CalendarHelper.getUpcomingEvents(contentResolver, calendarDays, enabledIds)
            calendarAdapter?.updateEvents(events)
            holder.empty.visibility = if (events.isEmpty()) View.VISIBLE else View.GONE
            holder.list.visibility = if (events.isEmpty()) View.GONE else View.VISIBLE
        } else {
            holder.empty.visibility = View.VISIBLE
            holder.list.visibility = View.GONE
        }
    }

    private fun showCalendarPicker() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CALENDAR) != PackageManager.PERMISSION_GRANTED) return

        val calendars = CalendarHelper.getCalendars(contentResolver)
        if (calendars.isEmpty()) return

        val enabledIds = calendarPrefs.getEnabledCalendarIds()
        val names = calendars.map { "${it.name} (${it.accountName})" }.toTypedArray()
        val checked = calendars.map { enabledIds == null || enabledIds.contains(it.id) }.toBooleanArray()

        AlertDialog.Builder(this)
            .setTitle("Show calendars")
            .setMultiChoiceItems(names, checked) { _, which, isChecked ->
                checked[which] = isChecked
            }
            .setPositiveButton("OK") { _, _ ->
                val selected = calendars.filterIndexed { i, _ -> checked[i] }.map { it.id }.toSet()
                calendarPrefs.setEnabledCalendarIds(selected)
                calendarHolder?.let { loadCalendarEvents(it) }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun bindAppList(holder: AppListViewHolder) {
        searchBar = holder.searchBar
        holder.appList.layoutManager = LinearLayoutManager(this)
        holder.appList.adapter = appAdapter

        holder.searchBar.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                appAdapter.filter(s?.toString().orEmpty())
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        setupDock(holder.dockBar)
    }

    private fun setupDock(dockBar: LinearLayout) {
        for (i in 0 until DockManager.SLOT_COUNT) {
            val imageView = dockBar.getChildAt(i) as ImageView
            bindDockSlot(imageView, i)
        }
    }

    private fun bindDockSlot(imageView: ImageView, index: Int) {
        val item = dockManager.getSlot(index)

        if (dockManager.isDefault(index)) {
            when (index) {
                0 -> imageView.setImageResource(R.drawable.ic_dock_phone)
                1 -> imageView.setImageResource(R.drawable.ic_dock_browser)
                2 -> imageView.setImageResource(R.drawable.ic_dock_camera)
            }
        } else {
            val icon = try {
                item.packageName?.let { packageManager.getApplicationIcon(it) }
            } catch (e: Exception) { null }
            if (icon != null) {
                imageView.setImageDrawable(icon)
            }
        }

        imageView.setOnClickListener { launchDockItem(index) }
        imageView.setOnLongClickListener {
            showDockPicker(index, imageView)
            true
        }
    }

    private fun launchDockItem(index: Int) {
        val item = dockManager.getSlot(index)
        if (dockManager.isDefault(index)) {
            when (index) {
                0 -> startActivity(Intent(Intent.ACTION_DIAL))
                1 -> {
                    val intent = packageManager.getLaunchIntentForPackage("com.brave.browser")
                        ?: Intent(Intent.ACTION_VIEW, android.net.Uri.parse("https://www.google.com"))
                    startActivity(intent)
                }
                2 -> startActivity(Intent(MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA))
            }
        } else {
            item.packageName?.let { pkg ->
                val intent = packageManager.getLaunchIntentForPackage(pkg)
                intent?.let { startActivity(it) }
            }
        }
    }

    private fun showDockPicker(slotIndex: Int, imageView: ImageView) {
        val apps = loadApps()
        val names = apps.map { it.name }.toTypedArray()

        AlertDialog.Builder(this)
            .setTitle("Choose dock app")
            .setItems(names) { _, which ->
                val app = apps[which]
                dockManager.setSlot(slotIndex, app.packageName, app.activityName, app.name)
                bindDockSlot(imageView, slotIndex)
            }
            .setNeutralButton("Reset") { _, _ ->
                getSharedPreferences("launcher", MODE_PRIVATE).edit()
                    .remove("dock_slot_$slotIndex").apply()
                bindDockSlot(imageView, slotIndex)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
}
