package org.koitharu.kotatsu.parsers.site.madara.es

import org.jsoup.nodes.Element
import org.koitharu.kotatsu.parsers.MangaLoaderContext
import org.koitharu.kotatsu.parsers.MangaSourceParser
import org.koitharu.kotatsu.parsers.model.MangaParserSource
import org.koitharu.kotatsu.parsers.model.MangaTag
import org.koitharu.kotatsu.parsers.site.madara.MadaraParser
import org.koitharu.kotatsu.parsers.util.toTitleCase
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull

@MangaSourceParser("TAURUSMANGA", "TaurusManga", "es")
internal class TaurusManga(context: MangaLoaderContext) :
	MadaraParser(context, MangaParserSource.TAURUSMANGA, "taurus.topmanhuas.org") {
	override val datePattern = "dd/MM/yyyy"
    override val selectDesc: String
        get() = super.selectDesc +", div.summary__content p"
    override val selectState: String
        get() = super.selectState + ", div.manga-status span:last-child"
    override val selectGenre: String
        get() = ".genres-filter .options a"

    override suspend fun createMangaTag(a: Element): MangaTag? {
        return MangaTag(
            key = a.absUrl("href").toHttpUrlOrNull()?.queryParameter("genre").toString(),
            title = a.text().toTitleCase(),
            source = source,
        )
    }
}
