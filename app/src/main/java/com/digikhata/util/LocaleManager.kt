package com.digikhata.util

import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat

/**
 * Phase 4c: per-app language selector. Wraps AppCompatDelegate so the rest of the app
 * doesn't care whether we're on Android 13+ (system-backed) or older (AppCompat backport).
 */
object LocaleManager {

    /** Null means "use system default". */
    enum class Lang(val tag: String?) {
        SYSTEM(null),
        ENGLISH("en"),
        URDU("ur"),
        HINDI("hi");

        companion object {
            fun fromTag(tag: String?): Lang = values().firstOrNull { it.tag == tag } ?: SYSTEM
        }
    }

    /** Currently applied language, or [Lang.SYSTEM] if none set. */
    fun current(): Lang {
        val list = AppCompatDelegate.getApplicationLocales()
        if (list.isEmpty) return Lang.SYSTEM
        return Lang.fromTag(list[0]?.language)
    }

    fun apply(lang: Lang) {
        val locales = if (lang.tag == null) {
            LocaleListCompat.getEmptyLocaleList()
        } else {
            LocaleListCompat.forLanguageTags(lang.tag)
        }
        AppCompatDelegate.setApplicationLocales(locales)
    }
}
