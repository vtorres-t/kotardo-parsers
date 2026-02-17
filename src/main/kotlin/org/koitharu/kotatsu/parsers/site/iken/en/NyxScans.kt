package org.koitharu.kotatsu.parsers.site.iken.en

import org.json.JSONArray
import org.koitharu.kotatsu.parsers.MangaLoaderContext
import org.koitharu.kotatsu.parsers.MangaSourceParser
import org.koitharu.kotatsu.parsers.model.*
import org.koitharu.kotatsu.parsers.site.iken.IkenParser
import org.koitharu.kotatsu.parsers.util.generateUid
import org.koitharu.kotatsu.parsers.util.parseHtml
import org.koitharu.kotatsu.parsers.util.src
import org.koitharu.kotatsu.parsers.util.toAbsoluteUrl
import org.koitharu.kotatsu.parsers.util.json.getStringOrNull
import org.koitharu.kotatsu.parsers.util.json.mapJSON

@MangaSourceParser("NYXSCANS", "Nyx Scans", "en")
internal class NyxScans(context: MangaLoaderContext) :
	IkenParser(context, MangaParserSource.NYXSCANS, "nyxscans.com", 18, true) {

	override suspend fun getListPage(page: Int, order: SortOrder, filter: MangaListFilter): List<Manga> {
		if (order == SortOrder.POPULARITY && filter.isEmpty()) {
			if (page != 1) {
				return emptyList()
			}
			return runCatching { parsePopularManga() }.getOrElse {
				super.getListPage(page, order, filter)
			}
		}
		return super.getListPage(page, order, filter)
	}

	override suspend fun getPages(chapter: MangaChapter): List<MangaPage> {
		return runCatching { super.getPages(chapter) }.getOrElse { originalError ->
			val doc = webClient.httpGet(chapter.url.toAbsoluteUrl(domain)).parseHtml()
			val images = doc.select(selectPages)
				.mapNotNull { img -> img.src() }
				.distinct()
			if (images.isEmpty()) throw originalError
			images.map { url ->
				MangaPage(
					id = generateUid(url),
					url = url,
					preview = null,
					source = source,
				)
			}
		}
	}

	private suspend fun parsePopularManga(): List<Manga> {
		val json = webClient.httpGet("https://$domain").parseHtml().getNextJson("popularPosts")
		return JSONArray(json).mapJSON {
			val url = "/series/${it.getString("slug")}"
			Manga(
				id = it.getLong("id"),
				url = url,
				publicUrl = url.toAbsoluteUrl(domain),
				coverUrl = it.getStringOrNull("featuredImage"),
				title = it.getString("postTitle"),
				altTitles = emptySet(),
				description = null,
				rating = RATING_UNKNOWN,
				tags = emptySet(),
				authors = emptySet(),
				state = null,
				source = source,
				contentRating = null,
			)
		}
	}
}
