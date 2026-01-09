package org.koitharu.kotatsu.parsers.site.ar

import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import org.json.JSONArray
import org.json.JSONObject
import org.koitharu.kotatsu.parsers.MangaLoaderContext
import org.koitharu.kotatsu.parsers.MangaSourceParser
import org.koitharu.kotatsu.parsers.config.ConfigKey
import org.koitharu.kotatsu.parsers.core.PagedMangaParser
import org.koitharu.kotatsu.parsers.model.*
import org.koitharu.kotatsu.parsers.util.*
import java.text.SimpleDateFormat
import java.util.*

@MangaSourceParser("AZORAMOON", "Azoramoon", "ar")
internal class Azoramoon(context: MangaLoaderContext) :
	PagedMangaParser(context, MangaParserSource.AZORAMOON, 24) {

	override val availableSortOrders: Set<SortOrder> =
		EnumSet.of(SortOrder.UPDATED, SortOrder.POPULARITY, SortOrder.ALPHABETICAL)

	override val configKeyDomain = ConfigKey.Domain("azoramoon.com")

	override val filterCapabilities: MangaListFilterCapabilities
		get() = MangaListFilterCapabilities(
			isSearchSupported = true,
			isSearchWithFiltersSupported = true,
		)

	override suspend fun getFilterOptions() = MangaListFilterOptions(
		availableTags = fetchAvailableTags(),
	)

	override fun onCreateConfig(keys: MutableCollection<ConfigKey<*>>) {
		super.onCreateConfig(keys)
		keys.add(userAgentKey)
	}

	override suspend fun getListPage(page: Int, order: SortOrder, filter: MangaListFilter): List<Manga> {
		val url = buildString {
			append("https://api.")
			append(domain)
			append("/api/query")

			val params = mutableListOf<String>()

			// Add pagination
			params.add("page=$page")
			params.add("perPage=24")

			// Add search query
			if (!filter.query.isNullOrEmpty()) {
				params.add("searchTerm=${filter.query!!.urlEncoded()}")
			}

			// Add genre filters (comma-separated)
			if (filter.tags.isNotEmpty()) {
				val genreIds = filter.tags.joinToString(",") { it.key }
				params.add("genreIds=$genreIds")
			}

			// Add sort filter
			val (orderBy, orderDirection) = when (order) {
				SortOrder.UPDATED -> "lastChapterAddedAt" to "desc"
				SortOrder.POPULARITY -> "totalViews" to "desc"
				SortOrder.NEWEST -> "createdAt" to "desc"
				SortOrder.ALPHABETICAL -> "postTitle" to "asc"
				else -> "lastChapterAddedAt" to "desc"
			}
			params.add("orderBy=$orderBy")
			params.add("orderDirection=$orderDirection")

			// Append parameters
			if (params.isNotEmpty()) {
				append("?")
				append(params.joinToString("&"))
			}
		}

		val response = webClient.httpGet(url)
		val body = response.body.string()

		// Try to parse as JSONArray first (API returns direct array)
		val jsonArray = try {
			JSONArray(body)
		} catch (e: Exception) {
			// If that fails, try as JSONObject and extract array
			val jsonObject = JSONObject(body)
			when {
				jsonObject.has("data") -> jsonObject.getJSONArray("data")
				jsonObject.has("results") -> jsonObject.getJSONArray("results")
				else -> JSONArray()
			}
		}

		return parseMangaList(jsonArray)
	}

	private fun parseMangaList(json: JSONArray): List<Manga> {
		val result = mutableListOf<Manga>()

		for (i in 0 until json.length()) {
			val obj = json.getJSONObject(i)
			val slug = obj.getString("slug")
			val url = "/series/$slug"
			val title = obj.getString("postTitle")
			val coverUrl = obj.optString("featuredImage")

			// Parse status
			val seriesStatus = obj.optString("seriesStatus", "")
			val state = when (seriesStatus.uppercase()) {
				"ONGOING" -> MangaState.ONGOING
				"COMPLETED" -> MangaState.FINISHED
				"HIATUS" -> MangaState.PAUSED
				else -> null
			}

			// Parse genres
			val genresArray = obj.optJSONArray("genres")
			val tags = if (genresArray != null) {
				buildSet {
					for (idx in 0 until genresArray.length()) {
						val genre = genresArray.getJSONObject(idx)
						add(MangaTag(
							key = genre.getInt("id").toString(),
							title = genre.getString("name"),
							source = source,
						))
					}
				}
			} else {
				emptySet()
			}

			result.add(
				Manga(
					id = generateUid(url),
					title = title,
					altTitles = emptySet(),
					url = url,
					publicUrl = url.toAbsoluteUrl(domain),
					rating = RATING_UNKNOWN,
					contentRating = null,
					coverUrl = coverUrl,
					tags = tags,
					state = state,
					authors = emptySet(),
					source = source,
				)
			)
		}

		return result
	}

	private suspend fun fetchAvailableTags(): Set<MangaTag> {
		// Hardcoded genre list from the API
		return setOf(
			MangaTag("1", "أكشن", source),
			MangaTag("2", "حريم", source),
			MangaTag("3", "زمكاني", source),
			MangaTag("4", "سحر", source),
			MangaTag("5", "شونين", source),
			MangaTag("6", "مغامرات", source),
			MangaTag("7", "خيال", source),
			MangaTag("8", "رومانسي", source),
			MangaTag("9", "كوميدي", source),
			MangaTag("10", "مانهوا", source),
			MangaTag("11", "إثارة", source),
			MangaTag("12", "دراما", source),
			MangaTag("13", "تاريخي", source),
			MangaTag("14", "راشد", source),
			MangaTag("15", "سينين", source),
			MangaTag("16", "خارق للطبيعة", source),
			MangaTag("17", "شياطين", source),
			MangaTag("18", "حياة مدرسية", source),
			MangaTag("19", "جوسي", source),
			MangaTag("20", "مانها", source),
			MangaTag("21", "ويبتون", source),
			MangaTag("22", "شينين", source),
			MangaTag("23", "قوة خارقة", source),
			MangaTag("24", "خيال علمي", source),
			MangaTag("25", "غموض", source),
			MangaTag("26", "مأساة", source),
			MangaTag("27", "شريحة من الحياة", source),
			MangaTag("28", "فنون قتالية", source),
			MangaTag("29", "شوجو", source),
			MangaTag("30", "ايسكاي", source),
			MangaTag("31", "مصاصي الدماء", source),
			MangaTag("32", "اسبوعي", source),
			MangaTag("33", "لعبة", source),
			MangaTag("34", "نفسي", source),
			MangaTag("35", "وحوش", source),
			MangaTag("36", "الحياة اليومية", source),
			MangaTag("37", "الحياة المدرسية", source),
			MangaTag("38", "رعب", source),
			MangaTag("39", "عسكري", source),
			MangaTag("40", "رياضي", source),
			MangaTag("41", "اتشي", source),
			MangaTag("42", "ايشي", source),
			MangaTag("43", "دموي", source),
			MangaTag("44", "زومبي", source),
			MangaTag("45", "مميز", source),
			MangaTag("46", "ايسيكاي", source),
			MangaTag("47", "فنتازيا", source),
			MangaTag("48", "اشباح", source),
			MangaTag("49", "إعادة إحياء", source),
			MangaTag("50", "بطل غير اعتيادي", source),
			MangaTag("51", "ثأر", source),
			MangaTag("52", "اثارة", source),
			MangaTag("53", "تراجيدي", source),
			MangaTag("54", "طبخ", source),
			MangaTag("55", "تناسخ", source),
			MangaTag("56", "عودة بالزمن", source),
			MangaTag("57", "انتقام", source),
			MangaTag("58", "تجسيد", source),
			MangaTag("59", "فانتازيا", source),
			MangaTag("60", "عائلي", source),
			MangaTag("61", "تجسد", source),
			MangaTag("62", "العاب", source),
			MangaTag("63", "عالم اخر", source),
			MangaTag("64", "السفر عبر الزمن", source),
			MangaTag("65", "خيالي", source),
			MangaTag("66", "زمنكاني", source),
			MangaTag("67", "مغامرة", source),
			MangaTag("68", "طبي", source),
			MangaTag("69", "عصور وسطى", source),
			MangaTag("70", "ساموراي", source),
			MangaTag("71", "مافيا", source),
			MangaTag("72", "نظام", source),
			MangaTag("73", "هوس", source),
			MangaTag("74", "عصري", source),
			MangaTag("75", "بطل مجنون", source),
			MangaTag("76", "رعاية اطفال", source),
			MangaTag("77", "زواج مدبر", source),
			MangaTag("78", "تشويق", source),
			MangaTag("79", "مكتبي", source),
			MangaTag("80", "قوى خارقه", source),
			MangaTag("81", "تحقيق", source),
			MangaTag("82", "أيتام", source),
			MangaTag("83", "جوسين", source),
			MangaTag("84", "موسيقي", source),
			MangaTag("85", "قصة حقيقة", source),
			MangaTag("86", "موريم", source),
			MangaTag("87", "موظفين", source),
			MangaTag("88", "فيكتوري", source),
			MangaTag("89", "مأساوي", source),
			MangaTag("90", "عصر حديث", source),
			MangaTag("91", "ندم", source),
			MangaTag("92", "حياة جامعية", source),
			MangaTag("93", "حاصد", source),
			MangaTag("94", "الأرواح", source),
			MangaTag("95", "جريمة", source),
			MangaTag("96", "عاطفي", source),
			MangaTag("97", "أكاديمي", source),
		)
	}

	override suspend fun getDetails(manga: Manga): Manga = coroutineScope {
		val doc = webClient.httpGet(manga.url.toAbsoluteUrl(domain)).parseHtml()
		val chaptersDeferred = async { loadChapters(manga.url) }

		val coverUrl = doc.selectFirst("img[alt*='${manga.title}'], section img")?.src() ?: manga.coverUrl

		// Extract rating
		val ratingText = doc.selectFirst("div:contains(التقييم) + div, span:contains(Rating)")?.text()
		val rating = ratingText?.substringBefore("/")?.trim()?.toFloatOrNull()?.div(5f) ?: RATING_UNKNOWN

		// Extract status
		val statusText = doc.selectFirst("div:contains(الحالة) + div, span:contains(Status)")?.text()
		val state = when {
			statusText?.contains("مستمر", ignoreCase = true) == true ||
			statusText?.contains("ongoing", ignoreCase = true) == true -> MangaState.ONGOING
			statusText?.contains("مكتمل", ignoreCase = true) == true ||
			statusText?.contains("completed", ignoreCase = true) == true -> MangaState.FINISHED
			else -> null
		}

		// Extract description
		val description = doc.selectFirst("div.text-sm.text-gray-600, div.description, p.summary")?.html()

		// Extract tags/genres
		val tags = doc.select("a[href*='/series/?genres='], span.genre").mapNotNullToSet { element ->
			val genreName = element.text().trim()
			val genreId = element.attr("href").substringAfter("genres=").substringBefore("&")
				.ifEmpty { genreName }

			if (genreName.isNotEmpty()) {
				MangaTag(
					key = genreId,
					title = genreName,
					source = source,
				)
			} else {
				null
			}
		}

		manga.copy(
			coverUrl = coverUrl,
			rating = rating,
			state = state,
			tags = tags,
			description = description,
			chapters = chaptersDeferred.await(),
		)
	}

	private suspend fun loadChapters(mangaUrl: String): List<MangaChapter> {
		val doc = webClient.httpGet(mangaUrl.toAbsoluteUrl(domain)).parseHtml()
		val dateFormat = SimpleDateFormat("yyyy-MM-dd", sourceLocale)

		return doc.select("div.mt-4.space-y-2 a[href*='/chapter/'], div.chapter-list a").mapChapters { i, a ->
			val url = a.attrAsRelativeUrl("href")
			val titleElement = a.selectFirst("span.font-semibold, span.chapter-title")
			val title = titleElement?.text() ?: a.text().substringBefore("•").trim()

			// Extract chapter number from title or URL
			val chapterNumber = title.substringAfter("الفصل", "")
				.substringAfter("Chapter", "")
				.trim()
				.split(" ")[0]
				.toFloatOrNull() ?: (i + 1f)

			// Extract date
			val dateElement = a.selectFirst("time, span.text-gray-500")
			val dateText = dateElement?.attr("datetime") ?: dateElement?.text()

			MangaChapter(
				id = generateUid(url),
				title = title,
				number = chapterNumber,
				volume = 0,
				url = url,
				scanlator = null,
				uploadDate = dateFormat.parseSafe(dateText),
				branch = null,
				source = source,
			)
		}
	}

	override suspend fun getPages(chapter: MangaChapter): List<MangaPage> {
		val fullUrl = chapter.url.toAbsoluteUrl(domain)
		val doc = webClient.httpGet(fullUrl).parseHtml()

		return doc.select("div.comic-images-wrapper img, div.chapter-images img, img[data-index]")
			.mapNotNull { img ->
				val imageUrl = img.attr("data-src").ifEmpty { img.src() }
				if (imageUrl?.isNotBlank() == true && !imageUrl.startsWith("data:image")) {
					val finalUrl = imageUrl.toRelativeUrl(domain)
					MangaPage(
						id = generateUid(finalUrl),
						url = finalUrl,
						preview = null,
						source = source,
					)
				} else {
					null
				}
			}
			.distinct()
	}

}
