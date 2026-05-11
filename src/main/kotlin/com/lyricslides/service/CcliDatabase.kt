package com.lyricslides.service

import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File

/**
 * A single saved entry in the local CCLI database.
 */
@Serializable
data class CcliEntry(
    val title: String,
    val artist: String,
    val ccliNumber: String,
    val copyright: String = "",
    val savedAt: Long = System.currentTimeMillis()
)

/**
 * Local JSON-backed CCLI database.
 *
 * Stores a list of (title, artist, ccliNumber, copyright) entries that the
 * user has entered. Looks up by either (title + artist) or by CCLI number.
 *
 * File location: ~/.lyricslides/ccli_database.json
 */
class CcliDatabase {

    private val storageDir: File = File(System.getProperty("user.home"), ".lyricslides")
    private val storageFile: File = File(storageDir, "ccli_database.json")
    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
    }

    private var entries: MutableList<CcliEntry> = mutableListOf()

    init {
        load()
    }

    /**
     * Load the database from disk, if it exists.
     */
    private fun load() {
        try {
            if (storageFile.exists()) {
                val content = storageFile.readText()
                if (content.isNotBlank()) {
                    entries = json.decodeFromString<List<CcliEntry>>(content).toMutableList()
                }
            }
        } catch (e: Exception) {
            // Corrupt or missing — start fresh
            entries = mutableListOf()
        }
    }

    /**
     * Persist the database to disk.
     */
    private fun save() {
        try {
            storageDir.mkdirs()
            storageFile.writeText(json.encodeToString(entries.toList()))
        } catch (_: Exception) {
            // Log in the future; silent failure is OK for now
        }
    }

    /**
     * Save or update an entry. Matching is by CCLI number first, then by
     * (title + artist). Updates in place if already present.
     */
    fun saveEntry(title: String, artist: String, ccliNumber: String, copyright: String = "") {
        if (title.isBlank() && ccliNumber.isBlank()) return

        val normalizedTitle = title.trim()
        val normalizedArtist = artist.trim()
        val normalizedCcli = ccliNumber.trim()

        // Look for existing entry by CCLI # or by title+artist
        val existingIndex = entries.indexOfFirst { entry ->
            (normalizedCcli.isNotBlank() && entry.ccliNumber == normalizedCcli) ||
                (entry.title.equals(normalizedTitle, ignoreCase = true) &&
                    entry.artist.equals(normalizedArtist, ignoreCase = true))
        }

        val newEntry = CcliEntry(
            title = normalizedTitle,
            artist = normalizedArtist,
            ccliNumber = normalizedCcli,
            copyright = copyright.trim(),
            savedAt = System.currentTimeMillis()
        )

        if (existingIndex >= 0) {
            entries[existingIndex] = newEntry
        } else {
            entries.add(newEntry)
        }
        save()
    }

    /**
     * Look up a CCLI entry by title + artist (case-insensitive).
     * Returns null if not found.
     */
    fun lookupByTitleArtist(title: String, artist: String): CcliEntry? {
        val t = title.trim()
        val a = artist.trim()
        if (t.isBlank()) return null

        // Exact match first
        entries.firstOrNull {
            it.title.equals(t, ignoreCase = true) &&
                it.artist.equals(a, ignoreCase = true)
        }?.let { return it }

        // Title-only match (artist may have different formatting)
        return entries.firstOrNull { it.title.equals(t, ignoreCase = true) }
    }

    /**
     * Look up a CCLI entry by CCLI number.
     * Returns null if not found.
     */
    fun lookupByCcliNumber(ccliNumber: String): CcliEntry? {
        val n = ccliNumber.trim()
        if (n.isBlank()) return null
        return entries.firstOrNull { it.ccliNumber == n }
    }

    /**
     * Partial search — matches any substring in title or artist.
     * Useful for an autocomplete-style lookup.
     */
    fun search(query: String): List<CcliEntry> {
        val q = query.trim().lowercase()
        if (q.isBlank()) return emptyList()
        return entries.filter {
            it.title.lowercase().contains(q) ||
                it.artist.lowercase().contains(q) ||
                it.ccliNumber.contains(q)
        }.sortedByDescending { it.savedAt }
    }

    /**
     * Get all stored entries, sorted by most recently saved.
     */
    fun getAllEntries(): List<CcliEntry> = entries.sortedByDescending { it.savedAt }

    /**
     * Delete an entry by CCLI number.
     */
    fun deleteByCcliNumber(ccliNumber: String) {
        entries.removeAll { it.ccliNumber == ccliNumber.trim() }
        save()
    }

    /**
     * Number of entries currently in the database.
     */
    fun size(): Int = entries.size
}
