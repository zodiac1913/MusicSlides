package com.lyricslides.service

import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import java.net.URLEncoder

/**
 * Result from a CCLI lookup.
 */
data class CcliSearchResult(
    val ccliNumber: String,
    val songTitle: String,
    val artist: String,
    val songSelectUrl: String
)

/**
 * Service for looking up CCLI Song Numbers.
 *
 * Since CCLI doesn't provide a free public API, this service searches
 * the SongSelect website and parses the CCLI number from the publicly
 * visible song page URLs and content.
 *
 * URL pattern: https://songselect.ccli.com/songs/{CCLI_NUMBER}/{slug}
 */
class CcliLookupService {

    private val client = HttpClient(CIO) {
        followRedirects = true
    }

    /**
     * Search for a CCLI song number by artist and title.
     * Scrapes the SongSelect search results page.
     *
     * Returns a list of possible matches with their CCLI numbers.
     */
    suspend fun searchCcli(artist: String, title: String): Result<List<CcliSearchResult>> {
        return try {
            val query = URLEncoder.encode("$title $artist", "UTF-8")
            val searchUrl = "https://songselect.ccli.com/search/results?SearchText=$query"

            val response: HttpResponse = client.get(searchUrl) {
                header(HttpHeaders.UserAgent, "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36")
                header(HttpHeaders.Accept, "text/html")
            }

            val html = response.bodyAsText()
            val results = parseSongSelectResults(html)

            if (results.isNotEmpty()) {
                Result.success(results)
            } else {
                // Fallback: try to find CCLI number from the page content using regex
                val fallbackResults = extractCcliFromHtml(html, title, artist)
                if (fallbackResults.isNotEmpty()) {
                    Result.success(fallbackResults)
                } else {
                    Result.failure(Exception("No CCLI results found for \"$title\" by \"$artist\""))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Parse SongSelect search results HTML to extract CCLI numbers.
     * Looks for links matching pattern: /songs/{number}/{slug}
     */
    private fun parseSongSelectResults(html: String): List<CcliSearchResult> {
        val results = mutableListOf<CcliSearchResult>()

        // Pattern: href="/songs/1234567/song-slug" or full URL
        val linkPattern = Regex("""songselect\.ccli\.com/songs/(\d+)/([^"'\s]+)""")
        val matches = linkPattern.findAll(html)

        val seen = mutableSetOf<String>()
        for (match in matches) {
            val ccliNumber = match.groupValues[1]
            val slug = match.groupValues[2]

            if (ccliNumber in seen) continue
            seen.add(ccliNumber)

            // Convert slug to readable title
            val readableTitle = slug.replace("-", " ")
                .replaceFirstChar { it.uppercase() }

            results.add(
                CcliSearchResult(
                    ccliNumber = ccliNumber,
                    songTitle = readableTitle,
                    artist = "", // Not always available from URL alone
                    songSelectUrl = "https://songselect.ccli.com/songs/$ccliNumber/$slug"
                )
            )
        }

        return results.take(10) // Limit to top 10 results
    }

    /**
     * Fallback extraction: look for CCLI number patterns in raw HTML.
     */
    private fun extractCcliFromHtml(html: String, title: String, artist: String): List<CcliSearchResult> {
        val results = mutableListOf<CcliSearchResult>()

        // Look for patterns like "CCLI Song # 1234567" or "ccli.*?(\d{5,8})"
        val ccliPattern = Regex("""(?:CCLI\s*(?:Song\s*)?#?\s*|/songs/)(\d{5,8})""", RegexOption.IGNORE_CASE)
        val matches = ccliPattern.findAll(html)

        val seen = mutableSetOf<String>()
        for (match in matches) {
            val number = match.groupValues[1]
            if (number in seen) continue
            seen.add(number)

            results.add(
                CcliSearchResult(
                    ccliNumber = number,
                    songTitle = title,
                    artist = artist,
                    songSelectUrl = "https://songselect.ccli.com/songs/$number"
                )
            )
        }

        return results.take(10)
    }

    /**
     * Look up a specific song's CCLI page to get more details.
     */
    suspend fun lookupSongDetails(ccliNumber: String): Result<CcliSearchResult> {
        return try {
            val url = "https://songselect.ccli.com/songs/$ccliNumber"
            val response: HttpResponse = client.get(url) {
                header(HttpHeaders.UserAgent, "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36")
                header(HttpHeaders.Accept, "text/html")
            }

            val html = response.bodyAsText()

            // Try to extract title from page
            val titlePattern = Regex("""<title>([^<]+)</title>""", RegexOption.IGNORE_CASE)
            val titleMatch = titlePattern.find(html)
            val pageTitle = titleMatch?.groupValues?.get(1)?.trim() ?: ""

            // Clean up title (usually "Song Name | SongSelect")
            val songTitle = pageTitle.split("|").firstOrNull()?.trim() ?: ""

            Result.success(
                CcliSearchResult(
                    ccliNumber = ccliNumber,
                    songTitle = songTitle,
                    artist = "",
                    songSelectUrl = url
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun close() {
        client.close()
    }
}
