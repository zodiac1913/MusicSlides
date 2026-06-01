package com.lyricslides.service

import com.lyricslides.model.LyricsResult
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.cookies.*
import io.ktor.client.request.*
import io.ktor.client.request.forms.*
import io.ktor.client.statement.*
import io.ktor.http.*
import java.net.URLEncoder

/**
 * Service for searching and fetching lyrics from CCLI SongSelect.
 * Requires valid CCLI SongSelect credentials (username + password).
 *
 * Flow:
 * 1. Login to profile.ccli.com with credentials
 * 2. Search songs on songselect.ccli.com
 * 3. Fetch lyrics from individual song pages
 */
class SongSelectService {

    private val client = HttpClient(CIO) {
        install(HttpCookies)
        followRedirects = true
        engine { requestTimeout = 30000 }
    }

    private var isAuthenticated = false

    /**
     * Authenticate with CCLI SongSelect.
     * Returns true if login was successful.
     */
    suspend fun login(username: String, password: String): Boolean {
        return try {
            // Step 1: GET the login page to get CSRF token
            val loginPageResponse = client.get("https://profile.ccli.com/account/signin") {
                header(HttpHeaders.UserAgent, "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
            }

            val loginHtml = loginPageResponse.bodyAsText()

            // Extract CSRF token
            val tokenRegex = Regex("""__RequestVerificationToken.*?value="([^"]+)"""")
            val token = tokenRegex.find(loginHtml)?.groupValues?.get(1)
                ?: return false

            // Step 2: POST login credentials
            val loginResponse = client.submitForm(
                url = "https://profile.ccli.com/Account/SignIn",
                formParameters = parameters {
                    append("EmailAddress", username)
                    append("Password", password)
                    append("RememberMe", "false")
                    append("__RequestVerificationToken", token)
                }
            ) {
                header(HttpHeaders.UserAgent, "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                header(HttpHeaders.Referrer, "https://profile.ccli.com/account/signin")
            }

            // Check if login succeeded (redirects to SongSelect or profile page)
            val responseUrl = loginResponse.request.url.toString()
            isAuthenticated = !responseUrl.contains("signin", ignoreCase = true) &&
                loginResponse.status != HttpStatusCode.Unauthorized

            isAuthenticated
        } catch (e: Exception) {
            isAuthenticated = false
            false
        }
    }

    /**
     * Search for songs on SongSelect.
     * Must be authenticated first.
     * Returns a list of (title, artist, ccliNumber, songUrl).
     */
    suspend fun searchSongs(query: String): Result<List<SongSelectResult>> {
        if (!isAuthenticated) {
            return Result.failure(Exception("Not authenticated. Please login first."))
        }

        return try {
            val encoded = URLEncoder.encode(query, "UTF-8")
            val searchUrl = "https://songselect.ccli.com/search/results?SearchText=$encoded"

            val response = client.get(searchUrl) {
                header(HttpHeaders.UserAgent, "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                header(HttpHeaders.Accept, "text/html")
            }

            val html = response.bodyAsText()
            val results = parseSongSelectSearchResults(html)

            Result.success(results)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Fetch lyrics for a specific song from SongSelect.
     * Must be authenticated first.
     */
    suspend fun fetchLyrics(songUrl: String, title: String, artist: String): Result<LyricsResult> {
        if (!isAuthenticated) {
            return Result.failure(Exception("Not authenticated"))
        }

        return try {
            // The lyrics are on the song's "viewlyrics" page
            val lyricsUrl = if (songUrl.contains("/viewlyrics")) songUrl
            else "$songUrl/viewlyrics"

            val response = client.get(lyricsUrl) {
                header(HttpHeaders.UserAgent, "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
            }

            val html = response.bodyAsText()
            val lyrics = extractSongSelectLyrics(html)

            if (lyrics.isBlank()) {
                return Result.failure(Exception("Could not extract lyrics from SongSelect"))
            }

            // Try to extract CCLI number from the page
            val ccliPattern = Regex("""CCLI\s*(?:Song\s*)?#?\s*(\d{4,8})""", RegexOption.IGNORE_CASE)
            val ccliMatch = ccliPattern.find(html)
            val ccliNumber = ccliMatch?.groupValues?.get(1) ?: ""

            Result.success(
                LyricsResult(
                    lyrics = lyrics,
                    artist = artist,
                    title = title,
                    source = "SongSelect (CCLI #$ccliNumber)"
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Parse search results from SongSelect HTML.
     */
    private fun parseSongSelectSearchResults(html: String): List<SongSelectResult> {
        val results = mutableListOf<SongSelectResult>()

        // SongSelect results have links like /songs/1234567/song-title
        val songPattern = Regex("""href="(/songs/(\d+)/([^"]+))"""")
        val matches = songPattern.findAll(html)

        val seen = mutableSetOf<String>()
        for (match in matches) {
            val path = match.groupValues[1]
            val ccliNumber = match.groupValues[2]
            val slug = match.groupValues[3]

            if (ccliNumber in seen) continue
            seen.add(ccliNumber)

            // Convert slug to readable title
            val title = slug.replace("-", " ")
                .split(" ")
                .joinToString(" ") { it.replaceFirstChar { c -> c.uppercase() } }

            results.add(
                SongSelectResult(
                    title = title,
                    artist = "", // Artist not always in the URL
                    ccliNumber = ccliNumber,
                    songUrl = "https://songselect.ccli.com$path"
                )
            )
        }

        // Try to extract artist names from the HTML near the song links
        // Pattern: song title followed by artist info
        val artistPattern = Regex("""<li[^>]*>.*?/songs/(\d+)/.*?</a>.*?<p[^>]*>(.*?)</p>""", setOf(RegexOption.DOT_MATCHES_ALL))
        val artistMatches = artistPattern.findAll(html)
        for (artistMatch in artistMatches) {
            val ccliNum = artistMatch.groupValues[1]
            var artistText = artistMatch.groupValues[2]
                .replace(Regex("""<[^>]+>"""), "")
                .trim()
            if (artistText.length > 100) artistText = artistText.take(100)

            results.find { it.ccliNumber == ccliNum }?.let { result ->
                val idx = results.indexOf(result)
                results[idx] = result.copy(artist = artistText)
            }
        }

        return results.take(15)
    }

    /**
     * Extract lyrics text from a SongSelect lyrics page.
     */
    private fun extractSongSelectLyrics(html: String): String {
        // SongSelect puts lyrics in a specific container
        // Look for the lyrics content area
        val patterns = listOf(
            // Common pattern: div with lyrics class
            Regex("""<div[^>]*class="[^"]*lyrics[^"]*"[^>]*>(.*?)</div>""", setOf(RegexOption.DOT_MATCHES_ALL, RegexOption.IGNORE_CASE)),
            // Alternative: song-viewer or content-text
            Regex("""<div[^>]*class="[^"]*song-viewer[^"]*"[^>]*>(.*?)</div>""", setOf(RegexOption.DOT_MATCHES_ALL, RegexOption.IGNORE_CASE)),
            // Pre-formatted text
            Regex("""<pre[^>]*>(.*?)</pre>""", setOf(RegexOption.DOT_MATCHES_ALL)),
        )

        for (pattern in patterns) {
            val match = pattern.find(html)
            if (match != null) {
                var content = match.groupValues[1]
                content = content.replace(Regex("""<br\s*/?>"""), "\n")
                content = content.replace(Regex("""<p[^>]*>"""), "\n\n")
                content = content.replace(Regex("""</p>"""), "")
                content = content.replace(Regex("""<[^>]+>"""), "")
                content = content.replace("&amp;", "&")
                content = content.replace("&nbsp;", " ")
                content = content.replace("&#39;", "'")
                content = content.replace("&quot;", "\"")
                content = content.replace(Regex("""\n{3,}"""), "\n\n")
                val cleaned = content.trim()
                if (cleaned.length > 50) return cleaned
            }
        }

        // Broader fallback: look for any large text block that looks like lyrics
        val textBlocks = Regex("""<p[^>]*>((?:[^<]|<br\s*/?>){100,})</p>""", setOf(RegexOption.DOT_MATCHES_ALL))
        val blockMatch = textBlocks.find(html)
        if (blockMatch != null) {
            var content = blockMatch.groupValues[1]
            content = content.replace(Regex("""<br\s*/?>"""), "\n")
            content = content.replace(Regex("""<[^>]+>"""), "")
            return content.trim()
        }

        return ""
    }

    fun isLoggedIn(): Boolean = isAuthenticated

    fun close() {
        client.close()
    }
}

/**
 * A search result from SongSelect.
 */
data class SongSelectResult(
    val title: String,
    val artist: String,
    val ccliNumber: String,
    val songUrl: String
)
