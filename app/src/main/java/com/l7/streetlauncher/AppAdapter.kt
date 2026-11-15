package com.l7.streetlauncher

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView

/** Grade da gaveta de apps, com filtro de busca sem acentos. */
class AppAdapter(
    private val onClick: (AppInfo, View) -> Unit,
    private val onLongClick: (AppInfo, View) -> Unit
) : RecyclerView.Adapter<AppAdapter.Holder>() {

    private var all: List<AppInfo> = emptyList()
    private var shown: List<AppInfo> = emptyList()
    private var query: String = ""

    private var accent: Int = 0
    private var neonFrame: Boolean = true
    private var showLabels: Boolean = true

    val visibleItems: List<AppInfo> get() = shown

    fun submit(apps: List<AppInfo>) {
        all = apps
        applyFilter()
    }

    fun filter(text: String) {
        query = TextNormalizer.normalize(text)
        applyFilter()
    }

    @SuppressLint("NotifyDataSetChanged")
    fun updateStyle(accent: Int, neonFrame: Boolean, showLabels: Boolean) {
        if (this.accent == accent && this.neonFrame == neonFrame && this.showLabels == showLabels) return
        this.accent = accent
        this.neonFrame = neonFrame
        this.showLabels = showLabels
        notifyDataSetChanged()
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun applyFilter() {
        shown = if (query.isEmpty()) all else all.filter { it.searchKey.contains(query) }
        notifyDataSetChanged()
    }

    override fun getItemCount(): Int = shown.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_app, parent, false)
        return Holder(view)
    }

    override fun onBindViewHolder(holder: Holder, position: Int) {
        val app = shown[position]
        NeonStyler.bindApp(holder.itemView, app, accent, neonFrame, showLabels)
        holder.itemView.setOnClickListener { onClick(app, it) }
        holder.itemView.setOnLongClickListener {
            onLongClick(app, it)
            true
        }
    }

    class Holder(view: View) : RecyclerView.ViewHolder(view)
}
