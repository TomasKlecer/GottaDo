package com.klecer.gottado.widget

import android.text.Html
import android.os.Build

/**
 * Strip HTML to plain text for widget display (RemoteViews do not support Spanned).
 */
fun stripHtmlForWidget(html: String): String {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
        Html.fromHtml(html, Html.FROM_HTML_MODE_LEGACY).toString()
    } else {
        @Suppress("DEPRECATION")
        Html.fromHtml(html).toString()
    }.trim()
}
