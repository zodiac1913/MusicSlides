package com.lyricslides.viewmodel

import com.lyricslides.model.AppStep
import com.lyricslides.model.LyricsResult
import com.lyricslides.model.SlideBackground
import com.lyricslides.model.SongInfo
import com.lyricslides.service.BackgroundProvider
import com.lyricslides.service.BackgroundStore
import com.lyricslides.service.CcliDatabase
import com.lyricslides.service.CcliEntry
import com.lyricslides.service.CcliLookupService
import com.lyricslides.service.CcliSearchResult
import com.lyricslides.service.LyricsService
import com.lyricslides.service.PptxExporter
import com.lyricslides.service.SlideGenerator
import com.lyricslides.service.SongSuggestion
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO

/**
 * Main ViewModel managing the application state and workflow.
 */
class AppViewModel {

    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private val lyricsService = LyricsService()
    private val ccliLookupService = CcliLookupService()
    private val slideGenerator = SlideGenerator()
    private val pptxExporter = PptxExporter()
    private val backgroundProvider = BackgroundProvider()
    private val backgroundStore = BackgroundStore()
    private val ccliDatabase = CcliDatabase()

    // Current workflow step
    private val _currentStep = MutableStateFlow(AppStep.SEARCH)
    val currentStep: StateFlow<AppStep> = _currentStep.asStateFlow()

    // Search state
    private val _artist = MutableStateFlow("")
    val artist: StateFlow<String> = _artist.asStateFlow()

    private val _title = MutableStateFlow("")
    val title: StateFlow<String> = _title.asStateFlow()

    private val _ccliNumber = MutableStateFlow("")
    val ccliNumber: StateFlow<String> = _ccliNumber.asStateFlow()

    private val _copyright = MutableStateFlow("")
    val copyright: StateFlow<String> = _copyright.asStateFlow()

    // CCLI search state
    private val _ccliSearchResults = MutableStateFlow<List<CcliSearchResult>>(emptyList())
    val ccliSearchResults: StateFlow<List<CcliSearchResult>> = _ccliSearchResults.asStateFlow()

    private val _isSearchingCcli = MutableStateFlow(false)
    val isSearchingCcli: StateFlow<Boolean> = _isSearchingCcli.asStateFlow()

    private val _ccliSearchError = MutableStateFlow<String?>(null)
    val ccliSearchError: StateFlow<String?> = _ccliSearchError.asStateFlow()

    // Song suggestion state (autocomplete)
    private val _songQuery = MutableStateFlow("")
    val songQuery: StateFlow<String> = _songQuery.asStateFlow()

    private val _suggestions = MutableStateFlow<List<SongSuggestion>>(emptyList())
    val suggestions: StateFlow<List<SongSuggestion>> = _suggestions.asStateFlow()

    private val _isLoadingSuggestions = MutableStateFlow(false)
    val isLoadingSuggestions: StateFlow<Boolean> = _isLoadingSuggestions.asStateFlow()

    private var suggestionJob: Job? = null

    private val _isSearching = MutableStateFlow(false)
    val isSearching: StateFlow<Boolean> = _isSearching.asStateFlow()

    private val _searchError = MutableStateFlow<String?>(null)
    val searchError: StateFlow<String?> = _searchError.asStateFlow()

    // Lyrics state
    private val _lyrics = MutableStateFlow("")
    val lyrics: StateFlow<String> = _lyrics.asStateFlow()

    private val _lyricsSource = MutableStateFlow("")
    val lyricsSource: StateFlow<String> = _lyricsSource.asStateFlow()

    // Background state
    private val _selectedBackground = MutableStateFlow<SlideBackground?>(null)
    val selectedBackground: StateFlow<SlideBackground?> = _selectedBackground.asStateFlow()

    private val _customBackgroundPath = MutableStateFlow<String?>(null)
    val customBackgroundPath: StateFlow<String?> = _customBackgroundPath.asStateFlow()

    private val _availableBackgrounds = MutableStateFlow(getBuiltInBackgrounds() + backgroundStore.loadCustomBackgrounds())
    val availableBackgrounds: StateFlow<List<SlideBackground>> = _availableBackgrounds.asStateFlow()

    // Generation state
    private val _isGenerating = MutableStateFlow(false)
    val isGenerating: StateFlow<Boolean> = _isGenerating.asStateFlow()

    private val _generatedSlides = MutableStateFlow<List<BufferedImage>>(emptyList())
    val generatedSlides: StateFlow<List<BufferedImage>> = _generatedSlides.asStateFlow()

    private val _generationProgress = MutableStateFlow(0f)
    val generationProgress: StateFlow<Float> = _generationProgress.asStateFlow()

    // Export state
    private val _exportPath = MutableStateFlow<String?>(null)
    val exportPath: StateFlow<String?> = _exportPath.asStateFlow()

    fun updateArtist(value: String) {
        _artist.value = value
        tryAutoFillCcliFromDb()
    }

    fun updateTitle(value: String) {
        _title.value = value
        tryAutoFillCcliFromDb()
    }

    fun updateCcliNumber(value: String) {
        _ccliNumber.value = value
        tryAutoFillFromCcli()
    }

    fun updateCopyright(value: String) {
        _copyright.value = value
    }

    /**
     * If we have a title + artist but no CCLI #, check the local DB.
     */
    private fun tryAutoFillCcliFromDb() {
        if (_ccliNumber.value.isNotBlank()) return
        val title = _title.value
        val artist = _artist.value
        if (title.isBlank() || artist.isBlank()) return

        val entry = ccliDatabase.lookupByTitleArtist(title, artist)
        if (entry != null && entry.ccliNumber.isNotBlank()) {
            _ccliNumber.value = entry.ccliNumber
            if (_copyright.value.isBlank() && entry.copyright.isNotBlank()) {
                _copyright.value = entry.copyright
            }
        }
    }

    /**
     * If the user types a known CCLI #, auto-fill title/artist/copyright from the DB.
     */
    private fun tryAutoFillFromCcli() {
        val ccli = _ccliNumber.value
        if (ccli.isBlank() || ccli.length < 4) return

        val entry = ccliDatabase.lookupByCcliNumber(ccli) ?: return

        if (_title.value.isBlank() && entry.title.isNotBlank()) {
            _title.value = entry.title
        }
        if (_artist.value.isBlank() && entry.artist.isNotBlank()) {
            _artist.value = entry.artist
        }
        if (_copyright.value.isBlank() && entry.copyright.isNotBlank()) {
            _copyright.value = entry.copyright
        }
    }

    /**
     * Save the current song info to the local CCLI database.
     * Called automatically when slides are generated so the DB builds up over time.
     */
    private fun saveToCcliDb() {
        if (_title.value.isBlank() && _ccliNumber.value.isBlank()) return
        ccliDatabase.saveEntry(
            title = _title.value,
            artist = _artist.value,
            ccliNumber = _ccliNumber.value,
            copyright = _copyright.value
        )
    }

    /**
     * Search the local CCLI database by title/artist.
     * Falls back to online scrape if nothing found locally.
     */
    fun searchCcli() {
        scope.launch {
            _isSearchingCcli.value = true
            _ccliSearchError.value = null
            _ccliSearchResults.value = emptyList()

            // 1. Check local DB first
            val localMatches = ccliDatabase.search(_title.value.ifBlank { _artist.value })
                .filter { it.ccliNumber.isNotBlank() }

            if (localMatches.isNotEmpty()) {
                _ccliSearchResults.value = localMatches.map { entry ->
                    CcliSearchResult(
                        ccliNumber = entry.ccliNumber,
                        songTitle = entry.title,
                        artist = entry.artist,
                        songSelectUrl = "https://songselect.ccli.com/songs/${entry.ccliNumber}"
                    )
                }
                if (localMatches.size == 1) {
                    _ccliNumber.value = localMatches[0].ccliNumber
                    if (_copyright.value.isBlank() && localMatches[0].copyright.isNotBlank()) {
                        _copyright.value = localMatches[0].copyright
                    }
                }
                _isSearchingCcli.value = false
                return@launch
            }

            // 2. Fall back to online scrape
            val result = ccliLookupService.searchCcli(_artist.value, _title.value)
            result.onSuccess { results ->
                if (results.isNotEmpty()) {
                    _ccliSearchResults.value = results
                    if (results.size == 1) {
                        _ccliNumber.value = results[0].ccliNumber
                    }
                } else {
                    _ccliSearchError.value = "Not found locally or online. Look it up on SongSelect and paste the number."
                }
            }.onFailure { error ->
                _ccliSearchError.value = "Not in local database. ${error.message ?: "Online search also failed."} " +
                    "Look it up at songselect.ccli.com and paste the number here."
            }

            _isSearchingCcli.value = false
        }
    }

    /**
     * Select a CCLI result from the search results list.
     */
    fun selectCcliResult(result: CcliSearchResult) {
        _ccliNumber.value = result.ccliNumber
        _ccliSearchResults.value = emptyList()
    }

    /**
     * Update the song search query and trigger debounced suggestion fetch.
     * This is what powers the autocomplete dropdown.
     */
    fun updateSongQuery(query: String) {
        _songQuery.value = query

        // Cancel previous pending search
        suggestionJob?.cancel()

        if (query.isBlank() || query.length < 2) {
            _suggestions.value = emptyList()
            _isLoadingSuggestions.value = false
            return
        }

        // Debounce: wait 350ms after user stops typing
        suggestionJob = scope.launch {
            delay(350)
            _isLoadingSuggestions.value = true

            val result = lyricsService.searchSuggestions(query)
            result.onSuccess { suggestions ->
                _suggestions.value = suggestions
            }.onFailure {
                _suggestions.value = emptyList()
            }

            _isLoadingSuggestions.value = false
        }
    }

    /**
     * User picked a suggestion. Fill artist/title, auto-lookup CCLI, fetch lyrics.
     */
    fun selectSuggestion(suggestion: SongSuggestion) {
        _title.value = suggestion.title
        _artist.value = suggestion.artist
        _songQuery.value = "${suggestion.title} — ${suggestion.artist}"
        _suggestions.value = emptyList()

        // First, check local DB — if we've entered this song before, use that CCLI/copyright
        val localEntry = ccliDatabase.lookupByTitleArtist(suggestion.title, suggestion.artist)
        if (localEntry != null) {
            if (_ccliNumber.value.isBlank() && localEntry.ccliNumber.isNotBlank()) {
                _ccliNumber.value = localEntry.ccliNumber
            }
            if (_copyright.value.isBlank() && localEntry.copyright.isNotBlank()) {
                _copyright.value = localEntry.copyright
            }
        }

        // Fetch lyrics from Genius using the suggestion's direct URL
        scope.launch {
            _isSearching.value = true
            _searchError.value = null

            val result = lyricsService.fetchLyricsFromUrl(suggestion)
            result.onSuccess { lyricsResult ->
                _lyrics.value = lyricsResult.lyrics
                _lyricsSource.value = lyricsResult.source
                _currentStep.value = AppStep.EDIT
            }.onFailure { error ->
                _searchError.value = error.message ?: "Unknown error"
            }

            _isSearching.value = false
        }
    }

    /**
     * Clear the suggestion dropdown without changing any fields.
     */
    fun clearSuggestions() {
        _suggestions.value = emptyList()
    }

    fun updateLyrics(value: String) {
        _lyrics.value = value
    }

    fun goToStep(step: AppStep) {
        _currentStep.value = step
    }

    fun nextStep() {
        val steps = AppStep.entries
        val currentIndex = steps.indexOf(_currentStep.value)
        if (currentIndex < steps.size - 1) {
            _currentStep.value = steps[currentIndex + 1]
        }
    }

    fun previousStep() {
        val steps = AppStep.entries
        val currentIndex = steps.indexOf(_currentStep.value)
        if (currentIndex > 0) {
            _currentStep.value = steps[currentIndex - 1]
        }
    }

    /**
     * Build the current SongInfo from form fields.
     */
    fun getSongInfo(): SongInfo = SongInfo(
        title = _title.value,
        artist = _artist.value,
        ccliNumber = _ccliNumber.value,
        copyright = _copyright.value
    )

    /**
     * Search for lyrics online.
     */
    fun searchLyrics() {
        scope.launch {
            _isSearching.value = true
            _searchError.value = null

            val result = lyricsService.searchLyrics(_artist.value, _title.value)

            result.onSuccess { lyricsResult ->
                _lyrics.value = lyricsResult.lyrics
                _lyricsSource.value = lyricsResult.source
                _currentStep.value = AppStep.EDIT
            }.onFailure { error ->
                _searchError.value = error.message ?: "Unknown error occurred"
            }

            _isSearching.value = false
        }
    }

    /**
     * Set lyrics manually (for custom/pasted lyrics).
     */
    fun setManualLyrics(text: String) {
        _lyrics.value = text
        _lyricsSource.value = "Manual entry"
        _currentStep.value = AppStep.EDIT
    }

    /**
     * Select a background for slides.
     */
    fun selectBackground(background: SlideBackground?) {
        _selectedBackground.value = background
        _customBackgroundPath.value = background?.path
    }

    /**
     * Add a custom background from file path.
     */
    fun addCustomBackground(filePath: String) {
        val file = File(filePath)
        if (file.exists() && file.isFile) {
            val bg = SlideBackground(
                name = file.nameWithoutExtension,
                path = filePath,
                isBuiltIn = false,
                category = "Custom"
            )
            _availableBackgrounds.value = _availableBackgrounds.value + bg
            selectBackground(bg)
            // Persist
            backgroundStore.saveCustomBackgrounds(
                _availableBackgrounds.value.filter { !it.isBuiltIn }
            )
        }
    }

    /**
     * Remove a background from the available list.
     * Only custom (non-built-in) backgrounds can be removed.
     */
    fun removeBackground(background: SlideBackground) {
        _availableBackgrounds.value = _availableBackgrounds.value.filter { it != background }
        // If the removed one was selected, clear selection
        if (_selectedBackground.value == background) {
            _selectedBackground.value = null
            _customBackgroundPath.value = null
        }
        // Persist
        backgroundStore.saveCustomBackgrounds(
            _availableBackgrounds.value.filter { !it.isBuiltIn }
        )
    }

    /**
     * Generate the full presentation: title slide + lyrics slides + closing slide.
     * Every slide gets the CCLI reference in the bottom-right corner (if provided).
     */
    fun generateSlides() {
        scope.launch {
            _isGenerating.value = true
            _generationProgress.value = 0f

            val backgroundImage: BufferedImage? = backgroundProvider.loadBackground(_selectedBackground.value)

            val songInfo = getSongInfo()
            val slides = slideGenerator.generateFullPresentation(
                lyrics = _lyrics.value,
                songInfo = songInfo,
                backgroundImage = backgroundImage
            )

            // Remember this song's CCLI info for next time
            saveToCcliDb()

            _generatedSlides.value = slides
            _generationProgress.value = 1f
            _isGenerating.value = false
            _currentStep.value = AppStep.PREVIEW
        }
    }

    /**
     * Export slides to PPTX format.
     */
    fun exportToPptx(outputPath: String) {
        scope.launch {
            try {
                val outputFile = File(outputPath)
                pptxExporter.exportToPptx(_generatedSlides.value, outputFile)
                _exportPath.value = outputPath
            } catch (e: Exception) {
                _searchError.value = "Export failed: ${e.message}"
            }
        }
    }

    /**
     * Export slides as individual PNG images.
     */
    fun exportAsImages(outputDir: String) {
        scope.launch {
            try {
                val dir = File(outputDir)
                dir.mkdirs()
                _generatedSlides.value.forEachIndexed { index, image ->
                    val file = File(dir, "slide_${String.format("%03d", index + 1)}.png")
                    ImageIO.write(image, "PNG", file)
                }
                _exportPath.value = outputDir
            } catch (e: Exception) {
                _searchError.value = "Export failed: ${e.message}"
            }
        }
    }

    private fun getBuiltInBackgrounds(): List<SlideBackground> {
        return backgroundProvider.getBuiltInBackgrounds()
    }

    /**
     * Expose the background provider for UI thumbnail loading.
     */
    fun getBackgroundProvider(): BackgroundProvider = backgroundProvider

    fun cleanup() {
        lyricsService.close()
        ccliLookupService.close()
        scope.cancel()
    }
}
