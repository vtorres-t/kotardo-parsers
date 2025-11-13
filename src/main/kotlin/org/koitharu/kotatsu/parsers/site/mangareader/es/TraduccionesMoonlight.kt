package org.koitharu.kotatsu.parsers.site.mangareader.es

import org.koitharu.kotatsu.parsers.MangaLoaderContext
import org.koitharu.kotatsu.parsers.MangaSourceParser
import org.koitharu.kotatsu.parsers.model.ContentType
import org.koitharu.kotatsu.parsers.model.MangaParserSource
import org.koitharu.kotatsu.parsers.site.mangareader.MangaReaderParser
import org.koitharu.kotatsu.parsers.Broken

@Broken("Not dead, changed template")
@MangaSourceParser("TRADUCCIONESMOONLIGHT", "TraduccionesMoonlight", "es", ContentType.HENTAI)
internal class TraduccionesMoonlight(context: MangaLoaderContext) :
	MangaReaderParser(context, MangaParserSource.TRADUCCIONESMOONLIGHT, "traduccionesmoonlight.com", 20, 10)
