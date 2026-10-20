package com.gao.xizhi.markdown

import android.annotation.SuppressLint
import android.webkit.WebView
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.viewinterop.AndroidView
import com.mikepenz.markdown.m3.Markdown

@Composable
fun MarkdownText(
    text: String,
    modifier: Modifier = Modifier,
) {
    val segments = remember(text) { MathSplitter.split(text) }
    if (segments.isEmpty()) return
    Column(modifier = modifier) {
        segments.forEach { segment ->
            when (segment) {
                is Segment.Markdown -> Markdown(
                    segment.text,
                    // phone-appropriate heading sizes: h1 no larger than a section title
                    typography = com.mikepenz.markdown.m3.markdownTypography(
                        h1 = MaterialTheme.typography.titleLarge,
                        h2 = MaterialTheme.typography.titleMedium,
                        h3 = MaterialTheme.typography.titleSmall,
                        h4 = MaterialTheme.typography.titleSmall,
                        h5 = MaterialTheme.typography.titleSmall,
                        h6 = MaterialTheme.typography.titleSmall,
                    ),
                )
                is Segment.Math -> KaTeXView(segment)
            }
        }
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
private fun KaTeXView(math: Segment.Math) {
    val textColor = String.format("#%06X", 0xFFFFFF and MaterialTheme.colorScheme.onSurfaceVariant.toArgb())
    val html = remember(math.latex, math.display, textColor) {
        buildString {
            append("<!DOCTYPE html><html><head><meta charset=\"utf-8\">")
            // baseUrl is file:///android_asset/, so asset paths are katex/...
            append("<link rel=\"stylesheet\" href=\"katex/katex.min.css\">")
            append("<style>body{margin:0;padding:2px 0;font-size:15px;color:$textColor;overflow-x:auto;}</style>")
            append("</head><body><span id=\"m\"></span>")
            append("<script src=\"katex/katex.min.js\"></script>")
            append("<script>var el=document.getElementById('m');")
            append("try{katex.render(${jsEscape(math.latex)},el,{displayMode:${math.display},throwOnError:true});}")
            append("catch(e){el.textContent=${jsEscape(math.latex)};}")
            append("</script></body></html>")
        }
    }

    AndroidView(
        factory = { context ->
            WebView(context).apply {
                settings.javaScriptEnabled = true
                setBackgroundColor(android.graphics.Color.TRANSPARENT)
                isVerticalScrollBarEnabled = false
                isHorizontalScrollBarEnabled = false
                tag = html
                loadDataWithBaseURL("file:///android_asset/", html, "text/html", "utf-8", null)
            }
        },
        update = { webView ->
            if (webView.tag != html) {
                webView.tag = html
                webView.loadDataWithBaseURL("file:///android_asset/", html, "text/html", "utf-8", null)
            }
        },
        onRelease = { webView ->
            webView.stopLoading()
            webView.destroy()
        },
        modifier = if (math.display) Modifier.fillMaxWidth() else Modifier.wrapContentWidth(),
    )
}

private fun jsEscape(s: String): String {
    val sb = StringBuilder("\"")
    for (c in s) {
        when (c) {
            '\\' -> sb.append("\\\\")
            '"' -> sb.append("\\\"")
            '\n' -> sb.append("\\n")
            '\r' -> {}
            '<' -> sb.append("\\u003c")
            '>' -> sb.append("\\u003e")
            else -> sb.append(c)
        }
    }
    return sb.append("\"").toString()
}
