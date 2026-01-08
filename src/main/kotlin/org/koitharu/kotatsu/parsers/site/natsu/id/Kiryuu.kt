package org.dokiteam.doki.parsers.site.natsu.id

import org.jsoup.Jsoup
import org.koitharu.kotatsu.parsers.MangaLoaderContext
import org.koitharu.kotatsu.parsers.MangaSourceParser
import org.koitharu.kotatsu.parsers.config.ConfigKey
import org.koitharu.kotatsu.parsers.model.MangaChapter
import org.koitharu.kotatsu.parsers.model.MangaParserSource
import org.koitharu.kotatsu.parsers.site.natsu.NatsuParser
import org.koitharu.kotatsu.parsers.util.attrAsRelativeUrl
import org.koitharu.kotatsu.parsers.util.generateUid
import org.koitharu.kotatsu.parsers.util.parseHtml
import org.koitharu.kotatsu.parsers.webview.InterceptionConfig

@MangaSourceParser("KIRYUU", "Kiryuu", "id")
internal class Kiryuu(context: MangaLoaderContext) :
    NatsuParser(context, MangaParserSource.KIRYUU, pageSize = 24) {

    override val configKeyDomain = ConfigKey.Domain("kiryuu03.com")

    override fun onCreateConfig(keys: MutableCollection<ConfigKey<*>>) {
        super.onCreateConfig(keys)
        keys.add(ConfigKey.DisableUpdateChecking(defaultValue = true))
    }

    override suspend fun loadChapters(
        mangaId: String,
        mangaAbsoluteUrl: String,
    ): List<MangaChapter> {
        // Intercept the chapter list AJAX request from WebView
        val pageScript = """
            (async function() {
                console.log('[Kiryuu] Starting chapter loading...');

                // Helper function to wait for element
                function waitForElement(selector, timeout = 5000) {
                    return new Promise((resolve, reject) => {
                        if (document.querySelector(selector)) {
                            return resolve(document.querySelector(selector));
                        }

                        const observer = new MutationObserver(() => {
                            if (document.querySelector(selector)) {
                                observer.disconnect();
                                resolve(document.querySelector(selector));
                            }
                        });

                        observer.observe(document.body, {
                            childList: true,
                            subtree: true
                        });

                        setTimeout(() => {
                            observer.disconnect();
                            reject(new Error('Timeout waiting for ' + selector));
                        }, timeout);
                    });
                }

                // Wait for and click the chapters tab
                try {
                    const tabButton = await waitForElement('button[data-key="chapters"]');
                    console.log('[Kiryuu] Found chapters tab button, clicking...');
                    tabButton.click();
                } catch (error) {
                    console.log('[Kiryuu] Error:', error);
                    // Try direct htmx trigger as fallback
                    const chapterList = document.querySelector('#chapter-list');
                    if (chapterList && typeof htmx !== 'undefined') {
                        htmx.trigger(chapterList, 'getChapterList');
                    }
                }
            })();
        """.trimIndent()

        // Pattern to match the AJAX request - it returns all chapters
        val urlPattern = Regex("wp-admin/admin-ajax\\.php\\?.*manga_id=$mangaId.*action=chapter_list")

        val config = InterceptionConfig(
            timeoutMs = 15000L,
            urlPattern = urlPattern,
            pageScript = pageScript,
            maxRequests = 1
        )

        val interceptedRequests = context.interceptWebViewRequests(
            url = mangaAbsoluteUrl,
            config = config
        )

        if (interceptedRequests.isEmpty()) {
            throw Exception("Failed to intercept chapter list request")
        }

        // Use the intercepted response body directly - it contains all chapters
        val responseBody = interceptedRequests.first().body
            ?: throw Exception("Intercepted response body is null")

        val doc = Jsoup.parse(responseBody)
        val chapterElements = doc.select("div#chapter-list > div[data-chapter-number]")

        val chapters = chapterElements.mapNotNull { element ->
            val a = element.selectFirst("a") ?: return@mapNotNull null
            val href = a.attrAsRelativeUrl("href")
            if (href.isBlank()) return@mapNotNull null

            val chapterTitle = element.selectFirst("div.font-medium span")?.text()?.trim() ?: ""
            val dateText = element.selectFirst("time")?.text()
            val number = element.attr("data-chapter-number").toFloatOrNull() ?: -1f

            MangaChapter(
                id = generateUid(href),
                title = chapterTitle,
                url = href,
                number = number,
                volume = 0,
                scanlator = null,
                uploadDate = parseDate(dateText),
                branch = null,
                source = source,
            )
        }

        return chapters.reversed()
    }
}
