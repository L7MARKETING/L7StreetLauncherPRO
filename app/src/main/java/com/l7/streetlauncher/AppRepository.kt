package com.l7.streetlauncher

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.LauncherActivityInfo
import android.content.pm.LauncherApps
import android.graphics.drawable.Drawable
import android.os.Process
import android.os.UserManager
import java.text.Collator

/**
 * Lê os apps instalados pela API oficial de launchers (LauncherApps).
 * Não precisa de root nem de permissões especiais.
 */
class AppRepository(context: Context) {

    private val appContext = context.applicationContext
    private val launcherApps = appContext.getSystemService(LauncherApps::class.java)
    private val userManager = appContext.getSystemService(UserManager::class.java)

    /** Executar fora da thread principal: carregar ícones pode demorar. */
    fun loadApps(): List<AppInfo> {
        val result = ArrayList<AppInfo>()
        val me = Process.myUserHandle()
        val selfPackage = appContext.packageName

        for (user in launcherApps.profiles) {
            val serial = userManager.getSerialNumberForUser(user)
            val isMain = user == me
            val activities = try {
                launcherApps.getActivityList(null, user)
            } catch (e: Exception) {
                emptyList()
            }
            for (activity in activities) {
                val component = activity.componentName
                // O próprio launcher não aparece na gaveta (há o botão "Ajustes L7").
                if (isMain && component.packageName == selfPackage) continue
                result += AppInfo(
                    label = activity.label?.toString()?.takeIf { it.isNotBlank() }
                        ?: component.packageName,
                    component = component,
                    user = user,
                    userSerial = serial,
                    icon = loadIcon(activity),
                    isSystem = (activity.applicationInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0,
                    isMainUser = isMain
                )
            }
        }

        val collator = Collator.getInstance()
        return result.sortedWith { a, b -> collator.compare(a.label, b.label) }
    }

    private fun loadIcon(activity: LauncherActivityInfo): Drawable? =
        try {
            activity.getBadgedIcon(0)
        } catch (e: Exception) {
            try {
                activity.getIcon(0)
            } catch (e2: Exception) {
                appContext.packageManager.defaultActivityIcon
            }
        }
}
