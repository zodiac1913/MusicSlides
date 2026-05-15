package com.lyricslides.service

import com.lyricslides.model.LyricsResult
import com.lyricslides.model.LyricsSource
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.net.URLEncoder

/**
 * A search suggestion returned when the user types a song name.
 */
data class SongSuggestion(
    val title: String,
    val artist: String,
    val source: String,
    val url: String? = null,
    val albumArtUrl: String? = null
)

/**
 * Service for fetching lyrics from various online sources.
 * Supports mainstream music AND gospel/worship/hymn sources.
 *
 * Sources (searched in order):
 * 1. lyrics.ovh — Free, no key, good mainstream coverage
 * 2. Genius — Huge catalog including gospel/worship (requires scraping lyrics from page)
 * 3. Hymnary.org — Traditional hymns and gospel songs
 * 4. Custom sources — User-added URLs
 */
class LyricsService {

    private val client = HttpClient(CIO) {
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                isLenient = true
            })
        }
        engine {
            requestTimeout = 15000
        }
    }

    private val defaultSources = listOf(
        LyricsSource("Lyrics.ovh", "https://api.lyrics.ovh/v1"),
        LyricsSource("Genius", "https://genius.com"),
        LyricsSource("Hymnary", "https://hymnary.org"),
    )

    private val customSources = mutableListOf<LyricsSource>()

    fun getAllSources(): List<LyricsSource> = defaultSources + customSources

    fun addCustomSource(source: LyricsSource) {
        customSources.add(source.copy(isCustom = true))
    }

    fun removeCustomSource(name: String) {
        customSources.removeAll { it.name == name }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Song suggestions: search by partial title/artist for autocomplete
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Search for song suggestions given a query string.
     * The query can be a song title, artist, or combination.
     * Returns a list of matching songs with title and artist.
     *
     * Searches Genius (great for modern/gospel) AND Hymnary (traditional hymns).
     */
    suspend fun searchSuggestions(query: String): Result<List<SongSuggestion>> {
        if (query.isBlank() || query.length < 2) {
            return Result.success(emptyList())
        }

        val allSuggestions = mutableListOf<SongSuggestion>()

        // 1. Genius search (modern, gospel, worship, mainstream)
        try {
            val encoded = URLEncoder.encode(query.trim(), "UTF-8")
            val url = "https://genius.com/api/search/multi?q=$encoded"

            val response: HttpResponse = client.get(url) {
                header(HttpHeaders.UserAgent, "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36")
                header(HttpHeaders.Accept, "application/json")
            }

            if (response.status == HttpStatusCode.OK) {
                val body = response.bodyAsText()
                val json = Json { ignoreUnknownKeys = true }
                val searchJson = json.decodeFromString<JsonObject>(body)
                allSuggestions.addAll(parseGeniusSuggestions(searchJson))
            }
        } catch (_: Exception) {
            // Genius failed — continue with Hymnary
        }

        // 2. Hymnary search (traditional hymns, spirituals, gospel standards)
        try {
            val hymnSuggestions = searchHymnarySuggestions(query)
            allSuggestions.addAll(hymnSuggestions)
        } catch (_: Exception) {
            // Hymnary failed — return what we have from Genius
        }

        return Result.success(allSuggestions.take(15))
    }

    /**
     * Search Hymnary.org and return suggestions from the search results page.
     */
    private suspend fun searchHymnarySuggestions(query: String): List<SongSuggestion> {
        val encoded = URLEncoder.encode(query.trim(), "UTF-8")
        val searchUrl = "https://hymnary.org/search?qu=$encoded"

        val response: HttpResponse = client.get(searchUrl) {
            header(HttpHeaders.UserAgent, "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36")
            header(HttpHeaders.Accept, "text/html")
        }

        if (response.status != HttpStatusCode.OK) return emptyList()

        val html = response.bodyAsText()
        val suggestions = mutableListOf<SongSuggestion>()

        // Extract text links and their display titles from the search results
        // Pattern: href="/text/some_text_id" ... displayTitle: "Amazing Grace"
        val textIdPattern = Regex("""href="/text/([a-z0-9_]+)""")
        val displayTitlePattern = Regex("""data-fieldName='displayTitle'><b[^>]*>[^<]*</b>:\s*([^<]+)</span>""")
        val authorPattern = Regex("""data-fieldName='author'><b[^>]*>[^<]*</b>:\s*([^<]+)</span>""")

        val textIds = textIdPattern.findAll(html)
            .map { it.groupValues[1] }
            .filter { !it.contains("fulltexts") && !it.contains("authority") }
            .distinct()
            .take(5)
            .toList()

        val titles = displayTitlePattern.findAll(html)
            .map { it.groupValues[1].trim() }
            .toList()

        val authors = authorPattern.findAll(html)
            .map { it.groupValues[1].trim() }
            .toList()

        for (i in textIds.indices) {
            val textId = textIds[i]
            val title = titles.getOrElse(i) {
                // Convert text_id to readable title
                textId.replace("_", " ").replaceFirstChar { it.uppercase() }
            }
            val author = authors.getOrElse(i) { "Traditional" }

            suggestions.add(
                SongSuggestion(
                    title = title,
                    artist = author,
                    source = "Hymnary",
                    url = "https://hymnary.org/text/$textId"
                )
            )
        }

        return suggestions
    }

    /**
     * Parse the Genius multi-search response into SongSuggestion objects.
     */
    private fun parseGeniusSuggestions(json: JsonObject): List<SongSuggestion> {
        val suggestions = mutableListOf<SongSuggestion>()
        try {
            val response = json["response"]?.jsonObject ?: return emptyList()
            val sections = response["sections"]?.jsonArray ?: return emptyList()

            for (section in sections) {
                val sectionObj = section.jsonObject
                val type = sectionObj["type"]?.jsonPrimitive?.content

                // Only grab song-type sections (skip "top hit" if it's not a song, artists, albums)
                if (type != "song" && type != "top_hit") continue

                val hits = sectionObj["hits"]?.jsonArray ?: continue
                for (hit in hits) {
                    val hitObj = hit.jsonObject
                    val hitType = hitObj["type"]?.jsonPrimitive?.content
                    if (hitType != "song") continue

                    val result = hitObj["result"]?.jsonObject ?: continue
                    val title = result["title"]?.jsonPrimitive?.content ?: continue
                    val artistName = result["primary_artist"]?.jsonObject
                        ?.get("name")?.jsonPrimitive?.content ?: ""
                    val url = result["url"]?.jsonPrimitive?.content
                    val thumbnail = result["song_art_image_thumbnail_url"]?.jsonPrimitive?.content

                    // Avoid duplicates
                    if (suggestions.any { it.title.equals(title, ignoreCase = true)
                            && it.artist.equals(artistName, ignoreCase = true) }) continue

                    suggestions.add(
                        SongSuggestion(
                            title = title,
                            artist = artistName,
                            source = "Genius",
                            url = url,
                            albumArtUrl = thumbnail
                        )
                    )
                }
            }
        } catch (_: Exception) {
            // Silently fail — return what we have
        }
        return suggestions.take(10)
    }

    /**
     * Fetch lyrics from a specific song URL (used when a suggestion is clicked).
     * Handles both Genius and Hymnary URLs.
     */
    suspend fun fetchLyricsFromUrl(suggestion: SongSuggestion): Result<LyricsResult> {
        val url = suggestion.url

        // Hymnary URL
        if (url != null && url.contains("hymnary.org/text/")) {
            val textId = url.substringAfter("/text/").substringBefore("?").substringBefore("#")
            return fetchHymnaryText(textId, suggestion.artist, suggestion.title)
        }

        // Genius URL
        if (url != null && url.contains("genius.com")) {
            return try {
                val response: HttpResponse = client.get(url) {
                    header(HttpHeaders.UserAgent, "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36")
                    header(HttpHeaders.Accept, "text/html")
                }

                if (response.status != HttpStatusCode.OK) {
                    return Result.failure(Exception("Could not load Genius page"))
                }

                val html = response.bodyAsText()
                val lyrics = extractLyricsFromGeniusHtml(html)

                if (lyrics.isBlank()) {
                    return Result.failure(Exception("Could not extract lyrics"))
                }

                Result.success(
                    LyricsResult(
                        lyrics = lyrics,
                        artist = suggestion.artist,
                        title = suggestion.title,
                        source = "Genius"
                    )
                )
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

        // No URL or unknown source — fall back to regular multi-source search
        return searchLyrics(suggestion.artist, suggestion.title)
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Source 1: lyrics.ovh (free, no API key)
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Fetch lyrics from lyrics.ovh (free, no API key required).
     * Endpoint: GET https://api.lyrics.ovh/v1/{artist}/{title}
     */
    suspend fun fetchFromLyricsOvh(artist: String, title: String): Result<LyricsResult> {
        return try {
            val encodedArtist = artist.trim()
            val encodedTitle = title.trim()
            val response: HttpResponse = client.get(
                "https://api.lyrics.ovh/v1/$encodedArtist/$encodedTitle"
            )

            if (response.status != HttpStatusCode.OK) {
                return Result.failure(Exception("Not found on lyrics.ovh"))
            }

            val body = response.bodyAsText()
            val json = Json { ignoreUnknownKeys = true }
            val parsed = json.decodeFromString<LyricsOvhResponse>(body)

            if (parsed.lyrics.isBlank()) {
                return Result.failure(Exception("Empty lyrics from lyrics.ovh"))
            }

            Result.success(
                LyricsResult(
                    lyrics = parsed.lyrics.trim(),
                    artist = artist,
                    title = title,
                    source = "lyrics.ovh"
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Source 2: Genius (scrape lyrics from song pages)
    // Great for gospel: Kirk Franklin, Maverick City, Elevation Worship,
    // Tasha Cobbs, William McDowell, etc.
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Search Genius for a song and scrape lyrics from the song page.
     * Genius has excellent gospel/worship coverage.
     */
    suspend fun fetchFromGenius(artist: String, title: String): Result<LyricsResult> {
        return try {
            // Step 1: Search for the song on Genius
            val query = URLEncoder.encode("$artist $title", "UTF-8")
            val searchUrl = "https://genius.com/api/search/multi?q=$query"

            val searchResponse: HttpResponse = client.get(searchUrl) {
                header(HttpHeaders.UserAgent, "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36")
                header(HttpHeaders.Accept, "application/json")
            }

            if (searchResponse.status != HttpStatusCode.OK) {
                return Result.failure(Exception("Genius search failed"))
            }

            val searchBody = searchResponse.bodyAsText()
            val json = Json { ignoreUnknownKeys = true }
            val searchJson = json.decodeFromString<JsonObject>(searchBody)

            // Navigate the response to find the first song URL
            val songUrl = extractGeniusSongUrl(searchJson)
                ?: return Result.failure(Exception("No song found on Genius"))

            // Step 2: Fetch the song page and extract lyrics
            val pageResponse: HttpResponse = client.get(songUrl) {
                header(HttpHeaders.UserAgent, "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36")
                header(HttpHeaders.Accept, "text/html")
            }

            if (pageResponse.status != HttpStatusCode.OK) {
                return Result.failure(Exception("Could not load Genius song page"))
            }

            val pageHtml = pageResponse.bodyAsText()
            val lyrics = extractLyricsFromGeniusHtml(pageHtml)

            if (lyrics.isBlank()) {
                return Result.failure(Exception("Could not extract lyrics from Genius"))
            }

            Result.success(
                LyricsResult(
                    lyrics = lyrics,
                    artist = artist,
                    title = title,
                    source = "Genius"
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Extract the first song URL from Genius multi-search JSON response.
     */
    private fun extractGeniusSongUrl(json: JsonObject): String? {
        try {
            val response = json["response"]?.jsonObject ?: return null
            val sections = response["sections"]?.jsonArray ?: return null

            for (section in sections) {
                val sectionObj = section.jsonObject
                val type = sectionObj["type"]?.jsonPrimitive?.content
                if (type == "song") {
                    val hits = sectionObj["hits"]?.jsonArray ?: continue
                    if (hits.isNotEmpty()) {
                        val firstHit = hits[0].jsonObject
                        val result = firstHit["result"]?.jsonObject ?: continue
                        return result["url"]?.jsonPrimitive?.content
                    }
                }
            }
        } catch (_: Exception) {}
        return null
    }

    /**
     * Extract lyrics text from a Genius song page HTML.
     * Genius stores lyrics in div elements with data-lyrics-container="true".
     */
    private fun extractLyricsFromGeniusHtml(html: String): String {
        val lyrics = StringBuilder()

        // Find all data-lyrics-container="true" sections.
        // We can't use a simple regex because these divs contain nested divs.
        // Instead, find each container start and manually extract to its balanced closing </div>.
        val marker = """data-lyrics-container="true""""
        var searchFrom = 0

        while (true) {
            val markerIdx = html.indexOf(marker, searchFrom)
            if (markerIdx < 0) break

            // Find the > that closes this opening tag
            val tagClose = html.indexOf('>', markerIdx)
            if (tagClose < 0) break

            val contentStart = tagClose + 1

            // Walk forward, counting nested <div and </div> to find the matching close
            var depth = 1
            var pos = contentStart
            while (pos < html.length && depth > 0) {
                val nextOpen = html.indexOf("<div", pos)
                val nextClose = html.indexOf("</div>", pos)

                if (nextClose < 0) break // malformed HTML, bail

                if (nextOpen in pos until nextClose) {
                    // Found a nested opening div before the next close
                    depth++
                    pos = nextOpen + 4
                } else {
                    // Found a closing div
                    depth--
                    if (depth == 0) {
                        // This is our container's closing tag
                        val content = html.substring(contentStart, nextClose)
                        val cleaned = cleanLyricsHtml(content)
                        if (cleaned.isNotBlank()) {
                            lyrics.append(cleaned)
                            lyrics.append("\n\n")
                        }
                    }
                    pos = nextClose + 6
                }
            }

            searchFrom = markerIdx + marker.length
        }

        // Clean up section markers and extra whitespace
        var result = lyrics.toString().trim()

        // Remove bracketed section markers like [Verse 1], [Chorus], [Bridge]
        result = result.replace(Regex("""\[([^\]]+)\]\s*"""), "\n\n")

        // Remove standalone section labels like "Refrain:", "Chorus:", "Bridge:", "Verse 2:", etc.
        result = result.replace(
            Regex("""(?m)^\s*(Verse|Chorus|Refrain|Bridge|Intro|Outro|Pre-Chorus|Pre-chorus|Interlude|Hook|Tag|Coda|Instrumental)\s*\d*\s*:?\s*$""", RegexOption.IGNORE_CASE),
            "\n"
        )

        // Collapse multiple blank lines into double newline (verse separator)
        result = result.replace(Regex("""\n{3,}"""), "\n\n")

        return result.trim()
    }

    /**
     * Clean raw HTML content from a lyrics container into plain text.
     */
    private fun cleanLyricsHtml(html: String): String {
        var content = html

        // Replace <br/> and <br> with newlines
        content = content.replace(Regex("""<br\s*/?>"""), "\n")

        // Remove all other HTML tags
        content = content.replace(Regex("""<[^>]+>"""), "")

        // Decode HTML entities
        content = content
            .replace("&amp;", "&")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace("&quot;", "\"")
            .replace("&#x27;", "'")
            .replace("&#39;", "'")
            .replace("&apos;", "'")
            .replace("&nbsp;", " ")

        return content.trim()
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Source 3: Hymnary.org (traditional hymns and gospel)
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Search Hymnary.org for hymn/gospel lyrics.
     * Hymnary has great coverage of traditional hymns, spirituals, and gospel songs.
     */
    suspend fun fetchFromHymnary(artist: String, title: String): Result<LyricsResult> {
        return try {
            val query = URLEncoder.encode(title, "UTF-8")
            // Use the regular search page (not CSV export) — it has text links in the HTML
            val searchUrl = "https://hymnary.org/search?qu=$query"

            val response: HttpResponse = client.get(searchUrl) {
                header(HttpHeaders.UserAgent, "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36")
                header(HttpHeaders.Accept, "text/html")
            }

            if (response.status != HttpStatusCode.OK) {
                return Result.failure(Exception("Hymnary search failed"))
            }

            val body = response.bodyAsText()

            // Look for text page links: href="/text/amazing_grace_how_sweet_the_sound"
            val textIdPattern = Regex("""href="/text/([a-z0-9_]+)""")
            val textMatch = textIdPattern.find(body)

            if (textMatch != null) {
                val textId = textMatch.groupValues[1]
                return fetchHymnaryText(textId, artist, title)
            }

            Result.failure(Exception("No hymn found on Hymnary.org"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Fetch the full text of a hymn from Hymnary.org.
     */
    private suspend fun fetchHymnaryText(textId: String, artist: String, title: String): Result<LyricsResult> {
        return try {
            val url = "https://hymnary.org/text/$textId"
            val response: HttpResponse = client.get(url) {
                header(HttpHeaders.UserAgent, "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36")
            }

            if (response.status != HttpStatusCode.OK) {
                return Result.failure(Exception("Could not load hymn page"))
            }

            val html = response.bodyAsText()
            val lyrics = extractLyricsFromHymnaryHtml(html)

            if (lyrics.isBlank()) {
                return Result.failure(Exception("Could not extract lyrics from Hymnary"))
            }

            Result.success(
                LyricsResult(
                    lyrics = lyrics,
                    artist = artist,
                    title = title,
                    source = "Hymnary.org"
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Extract hymn lyrics from a Hymnary.org text page.
     * The lyrics are in: <div class="authority_columns"><p>stanza1</p><p>stanza2</p>...</div>
     * Line breaks within stanzas use <br />.
     */
    private fun extractLyricsFromHymnaryHtml(html: String): String {
        // Strategy: find the "authority_columns" div that contains <p> tags with lyrics.
        // It appears after the "Representative Text" heading.
        val startMarker = """class="authority_columns">"""
        val startIdx = html.indexOf(startMarker)
        if (startIdx < 0) return ""

        val contentStart = startIdx + startMarker.length
        // Find the closing </div> for this section — look for the "authority_bottom_bar"
        val endMarker = """authority_bottom_bar"""
        var endIdx = html.indexOf(endMarker, contentStart)
        if (endIdx < 0) endIdx = html.indexOf("</div></div>", contentStart)
        if (endIdx < 0) endIdx = (contentStart + 10000).coerceAtMost(html.length)

        val rawContent = html.substring(contentStart, endIdx)

        // Extract each <p>...</p> as a stanza
        val stanzaPattern = Regex("""<p>(.*?)</p>""", setOf(RegexOption.DOT_MATCHES_ALL))
        val stanzas = stanzaPattern.findAll(rawContent).toList()

        if (stanzas.isEmpty()) {
            // Fallback: just strip tags from the whole block
            var content = rawContent
                .replace(Regex("""<br\s*/?>"""), "\n")
                .replace(Regex("""<[^>]+>"""), "")
                .replace("&amp;", "&")
                .replace("&nbsp;", " ")
                .replace("&#x27;", "'")
                .trim()
            return content
        }

        return stanzas.joinToString("\n\n") { match ->
            var stanza = match.groupValues[1]
            // Remove stanza numbers like "1 " at the start
            stanza = stanza.replace(Regex("""^\s*\d+\s+"""), "")
            // Convert <br /> to newlines
            stanza = stanza.replace(Regex("""<br\s*/?>"""), "\n")
            // Strip remaining HTML tags
            stanza = stanza.replace(Regex("""<[^>]+>"""), "")
            // Decode entities
            stanza = stanza
                .replace("&amp;", "&")
                .replace("&lt;", "<")
                .replace("&gt;", ">")
                .replace("&nbsp;", " ")
                .replace("&#x27;", "'")
                .replace("&quot;", "\"")
            // Clean up whitespace
            stanza.lines().joinToString("\n") { it.trim() }.trim()
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Main search: tries all sources in order
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Search across all available sources, returning the first successful result.
     * Order prioritizes sources with good gospel/worship coverage.
     */
    suspend fun searchLyrics(artist: String, title: String): Result<LyricsResult> {
        val errors = mutableListOf<String>()

        // 1. Try lyrics.ovh (fast, good for mainstream + some worship)
        val lyricsOvh = fetchFromLyricsOvh(artist, title)
        if (lyricsOvh.isSuccess) return lyricsOvh
        errors.add("lyrics.ovh: ${lyricsOvh.exceptionOrNull()?.message}")

        // 2. Try Genius (excellent gospel/worship coverage)
        val genius = fetchFromGenius(artist, title)
        if (genius.isSuccess) return genius
        errors.add("Genius: ${genius.exceptionOrNull()?.message}")

        // 3. Try Hymnary (traditional hymns, spirituals, gospel standards)
        val hymnary = fetchFromHymnary(artist, title)
        if (hymnary.isSuccess) return hymnary
        errors.add("Hymnary: ${hymnary.exceptionOrNull()?.message}")

        return Result.failure(
            Exception(
                "Could not find lyrics for \"$title\" by \"$artist\".\n" +
                "Tried: ${errors.joinToString("; ")}\n\n" +
                "You can paste lyrics manually using the Paste button."
            )
        )
    }

    /**
     * Search a specific source by name.
     */
    suspend fun searchFromSource(sourceName: String, artist: String, title: String): Result<LyricsResult> {
        return when (sourceName) {
            "Lyrics.ovh" -> fetchFromLyricsOvh(artist, title)
            "Genius" -> fetchFromGenius(artist, title)
            "Hymnary" -> fetchFromHymnary(artist, title)
            else -> Result.failure(Exception("Unknown source: $sourceName"))
        }
    }

    fun close() {
        client.close()
    }
}

@kotlinx.serialization.Serializable
private data class LyricsOvhResponse(
    val lyrics: String = ""
)
