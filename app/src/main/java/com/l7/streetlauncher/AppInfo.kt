package com.l7.streetlauncher

import android.content.ComponentName
import android.graphics.drawable.Drawable
import android.os.UserHandle
import java.text.Normalizer
import java.util.Locale

/** Um app que pode ser aberto pelo launcher (inclui perfis de trabalho e "Clonar apps"). */
class AppInfo(
    val label: String,
    val component: ComponentName,
    val user: UserHandle,
    val userSerial: Long,
    val icon: Drawable?,
    val isSystem: Boolean,
    val isMainUser: Boolean
) {
    /** Chave estável usada para salvar favoritos. */
    val key: String = "$userSerial/${component.flattenToString()}"

    val packageName: String get() = component.packageName

    val searchKey: String = TextNormalizer.normalize("$label ${component.packageName}")

    /** Só é possível pedir a desinstalação de apps não-sistema do usuário principal. */
    val canUninstall: Boolean get() = isMainUser && !isSystem
}

object TextNormalizer {
    private val marks = "\\p{Mn}+".toRegex()

    /** Remove acentos e deixa minúsculo, para a busca achar "camera" em "Câmera". */
    fun normalize(text: String): String =
        Normalizer.normalize(text, Normalizer.Form.NFD)
            .replace(marks, "")
            .lowercase(Locale.ROOT)
            .trim()
}
