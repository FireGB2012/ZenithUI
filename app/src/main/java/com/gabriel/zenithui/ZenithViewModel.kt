package com.gabriel.zenithui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.gabriel.zenithui.data.AppCell
import com.gabriel.zenithui.data.AppInfo
import com.gabriel.zenithui.data.AppRepository
import com.gabriel.zenithui.data.FolderCell
import com.gabriel.zenithui.data.GridDensity
import com.gabriel.zenithui.data.HomeItem
import com.gabriel.zenithui.data.IconLoader
import com.gabriel.zenithui.data.LayoutBuilder
import com.gabriel.zenithui.data.PageGrid
import com.gabriel.zenithui.data.ROW_UNITS
import com.gabriel.zenithui.data.SettingsStore
import com.gabriel.zenithui.data.WidgetCell
import com.gabriel.zenithui.data.Widgets
import com.gabriel.zenithui.data.WallpaperSource
import com.gabriel.zenithui.data.ZenithSettings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ZenithViewModel(application: Application) : AndroidViewModel(application) {

    val store = SettingsStore(application)
    val repository = AppRepository(application)
    val icons = IconLoader(application, repository)
    private val wallpaperSource = WallpaperSource(application)

    val settings: StateFlow<ZenithSettings> = store.settings
    val layout: StateFlow<List<HomeItem>> = store.layout
    val apps: StateFlow<List<AppInfo>> = repository.apps
    val backdrop = wallpaperSource.backdrop

    private val _homePressed = MutableStateFlow(0)

    /** Incremented every time the home button is pressed while Zenith is already showing. */
    val homePressed: StateFlow<Int> = _homePressed.asStateFlow()

    fun onHomePressed() {
        _homePressed.value += 1
    }

    private val _environmentTick = MutableStateFlow(0)

    /** Bumped when something outside the app changed, such as which launcher is the default. */
    val environmentTick: StateFlow<Int> = _environmentTick.asStateFlow()

    fun onEnvironmentChanged() {
        _environmentTick.value += 1
        refreshWallpaper()
    }

    fun noteHomePrompt() = store.update { it.copy(homePrompts = it.homePrompts + 1) }

    init {
        // Layouts saved before the grid gained sub-rows are rescaled once.
        if (settings.value.layoutVersion < 1) {
            if (layout.value.isNotEmpty()) store.setLayout(LayoutBuilder.upgradeToRowUnits(layout.value))
            store.update { it.copy(layoutVersion = 1) }
        }
        viewModelScope.launch { repository.refresh() }
        viewModelScope.launch { wallpaperSource.refresh() }
        viewModelScope.launch {
            val current = settings.value
            icons.configure(current.iconPack, current.iconShape)
        }
        // The app list arrives asynchronously; once it does, every icon asks again.
        viewModelScope.launch {
            repository.apps.collect { icons.invalidate() }
        }
        repository.observePackages { viewModelScope.launch { repository.refresh() } }
    }

    override fun onCleared() {
        repository.stopObserving()
        super.onCleared()
    }

    fun refreshWallpaper() = viewModelScope.launch { wallpaperSource.refresh() }

    fun refreshApps() = viewModelScope.launch { repository.refresh() }

    // ---------------------------------------------------------------- onboarding

    /** The real work behind the setup screen: read the device, then compose a home screen. */
    suspend fun buildHome(
        name: String,
        paletteId: String,
        appearance: String,
        widgets: List<String>,
        density: GridDensity,
        columns: Int,
        rows: Int,
    ) {
        val installed = repository.refresh()
        val dock = repository.suggestedDock(4)
        val items = LayoutBuilder.build(repository, installed, widgets, columns, rows, dock)
        store.update {
            it.copy(
                userName = name.trim(),
                paletteId = paletteId,
                appearance = appearance,
                gridDensity = density.id,
                columns = columns,
                rows = rows,
                dock = dock,
            )
        }
        store.setLayout(items)
        wallpaperSource.refresh()
    }

    fun markOnboarded() = store.update { it.copy(onboarded = true) }

    fun resetEverything() {
        store.reset()
        viewModelScope.launch { icons.configure("", "squircle") }
    }

    // ---------------------------------------------------------------- settings

    fun updateSettings(transform: (ZenithSettings) -> ZenithSettings) {
        val before = settings.value
        store.update(transform)
        val after = settings.value
        if (after.iconPack != before.iconPack || after.iconShape != before.iconShape) {
            viewModelScope.launch { icons.configure(after.iconPack, after.iconShape) }
        }
        if (after.columns != before.columns || after.rows != before.rows) {
            store.setLayout(LayoutBuilder.reflow(layout.value, after.columns, after.rows))
        }
    }

    /**
     * Re-derives the grid for the screen we are actually on. Called whenever the window changes
     * size — a fold opening, a rotation, a tablet — so cells keep the size the user asked for
     * instead of stretching to fill whatever glass they are on.
     */
    fun resolveGrid(density: GridDensity, widthDp: Float, heightDp: Float) {
        val (columns, rows) = density.gridFor(widthDp, heightDp)
        val current = settings.value
        if (columns == current.columns && rows == current.rows) return
        updateSettings { it.copy(columns = columns, rows = rows) }
    }

    fun setNote(text: String) = store.update { it.copy(note = text) }

    // ---------------------------------------------------------------- launching

    fun open(key: String) {
        if (!repository.launch(key)) refreshApps()
    }

    fun openAppInfo(key: String) = repository.openAppInfo(key)

    fun uninstall(key: String) = repository.uninstall(key)

    // ---------------------------------------------------------------- home editing

    fun moveItem(id: String, page: Int, col: Int, row: Int) = store.mutateLayout { items ->
        items.map { if (it.id == id) it.moved(page, col, row) else it }
    }

    fun removeItem(id: String) = store.mutateLayout { items -> items.filterNot { it.id == id } }

    fun addAppAt(key: String, page: Int, col: Int, row: Int) {
        val parts = key.split('/', limit = 2)
        if (parts.size != 2) return
        store.mutateLayout { items ->
            items + AppCell(LayoutBuilder.newId(), page, col, row, parts[0], parts[1])
        }
    }

    fun addAppToFirstFreeSlot(key: String): Boolean {
        val parts = key.split('/', limit = 2)
        if (parts.size != 2) return false
        val slot = firstFreeSlot(1, ROW_UNITS, LayoutBuilder.ICON_ROW_STEP) ?: return false
        store.mutateLayout { items ->
            items + AppCell(LayoutBuilder.newId(), slot.first, slot.second, slot.third, parts[0], parts[1])
        }
        return true
    }

    fun addWidget(type: String): Boolean {
        val spec = Widgets.byType(type) ?: return false
        val s = settings.value
        val (spanX, spanY) = Widgets.defaultSpan(spec, s.columns, s.rows)
        val slot = firstFreeSlot(spanX, spanY) ?: return false
        store.mutateLayout { items ->
            items + WidgetCell(LayoutBuilder.newId(), slot.first, slot.second, slot.third, spec.type, spanX, spanY)
        }
        return true
    }

    /** Resizes a widget in whole grid cells, refusing sizes that would overlap or overflow. */
    fun resizeWidget(id: String, spanX: Int, spanY: Int): Boolean {
        val widget = layout.value.firstOrNull { it.id == id } as? WidgetCell ?: return false
        val spec = Widgets.byType(widget.type) ?: return false
        val s = settings.value
        val (targetX, targetY) = Widgets.clampSpan(spec, spanX, spanY, s.columns, s.rows)
        if (targetX == widget.spanX && targetY == widget.spanY) return false
        if (!isSlotFree(widget.page, widget.col, widget.row, targetX, targetY, id)) return false
        store.mutateLayout { items ->
            items.map { if (it.id == id) widget.copy(spanX = targetX, spanY = targetY) else it }
        }
        return true
    }

    /** Returns page/col/row of the first opening big enough for the given span, in row units. */
    fun firstFreeSlot(spanX: Int, spanY: Int, rowStep: Int = 1): Triple<Int, Int, Int>? {
        val s = settings.value
        val items = layout.value
        val lastPage = (items.maxOfOrNull { it.page } ?: 0)
        for (page in 0..(lastPage + 1)) {
            val grid = PageGrid(s.columns, s.rows * ROW_UNITS)
            items.filter { it.page == page }.forEach { grid.occupy(it.col, it.row, it.spanX, it.spanY) }
            val slot = grid.findSlot(spanX, spanY, rowStep)
            if (slot != null) return Triple(page, slot.first, slot.second)
        }
        return null
    }

    /**
     * The closest opening to where you let go, measured in millimetres on the glass rather than
     * in grid units: [colWidthInRowUnits] says how tall one column is in row units, so a cell one
     * column across really does beat one four rows down. Get that wrong and an icon dropped beside
     * a widget flies to the bottom of the page instead of tucking in next to it.
     */
    fun nearestFreeSlot(
        page: Int,
        col: Int,
        row: Int,
        spanX: Int,
        spanY: Int,
        ignoreId: String?,
        rowStep: Int,
        colWidthInRowUnits: Float = 2f,
    ): Pair<Int, Int>? {
        if (isSlotFree(page, col, row, spanX, spanY, ignoreId)) return col to row

        val s = settings.value
        val rows = s.rows * ROW_UNITS
        val weight = colWidthInRowUnits.coerceIn(0.5f, 8f)
        var best: Pair<Int, Int>? = null
        var bestDistance = Float.MAX_VALUE

        var candidateRow = 0
        while (candidateRow <= rows - spanY) {
            for (candidateCol in 0..(s.columns - spanX)) {
                if (!isSlotFree(page, candidateCol, candidateRow, spanX, spanY, ignoreId)) continue
                val dx = (candidateCol - col) * weight
                val dy = (candidateRow - row).toFloat()
                val distance = dx * dx + dy * dy
                if (distance < bestDistance) {
                    bestDistance = distance
                    best = candidateCol to candidateRow
                }
            }
            candidateRow += rowStep
        }
        return best
    }

    fun isSlotFree(page: Int, col: Int, row: Int, spanX: Int, spanY: Int, ignoreId: String?): Boolean {
        val s = settings.value
        val grid = PageGrid(s.columns, s.rows * ROW_UNITS)
        layout.value.filter { it.page == page && it.id != ignoreId }
            .forEach { grid.occupy(it.col, it.row, it.spanX, it.spanY) }
        return grid.isFree(col, row, spanX, spanY)
    }

    /** Drop one app onto another to make a folder; drop onto a folder to add to it. */
    fun combine(draggedId: String, targetId: String) = store.mutateLayout { items ->
        val dragged = items.firstOrNull { it.id == draggedId } ?: return@mutateLayout items
        val target = items.firstOrNull { it.id == targetId } ?: return@mutateLayout items
        when {
            target is FolderCell && dragged is AppCell ->
                items.filterNot { it.id == draggedId }
                    .map { if (it.id == targetId) target.copy(keys = (target.keys + dragged.key).distinct()) else it }

            target is AppCell && dragged is AppCell ->
                items.filterNot { it.id == draggedId || it.id == targetId } + FolderCell(
                    id = LayoutBuilder.newId(),
                    page = target.page, col = target.col, row = target.row,
                    title = "Folder",
                    keys = listOf(target.key, dragged.key),
                )

            else -> items
        }
    }

    fun renameFolder(id: String, title: String) = store.mutateLayout { items ->
        items.map { if (it.id == id && it is FolderCell) it.copy(title = title) else it }
    }

    /**
     * Takes one app out of a folder and puts it back on the page, rather than throwing it away.
     * A folder left holding one app becomes that app again; an empty one disappears.
     */
    fun removeFromFolder(folderId: String, key: String) = store.mutateLayout { items ->
        val folder = items.firstOrNull { it.id == folderId } as? FolderCell ?: return@mutateLayout items
        val keys = folder.keys.filterNot { it == key }
        val s = settings.value

        // Give the app that just came out a cell of its own on the same page.
        fun rehome(base: List<HomeItem>): List<HomeItem> {
            val parts = key.split('/', limit = 2)
            if (parts.size != 2) return base
            val grid = PageGrid(s.columns, s.rows * ROW_UNITS)
            base.filter { it.page == folder.page }
                .forEach { grid.occupy(it.col, it.row, it.spanX, it.spanY) }
            val slot = grid.findSlot(1, ROW_UNITS, LayoutBuilder.ICON_ROW_STEP) ?: return base
            return base + AppCell(
                LayoutBuilder.newId(), folder.page, slot.first, slot.second, parts[0], parts[1],
            )
        }

        when {
            keys.isEmpty() -> rehome(items.filterNot { it.id == folderId })

            keys.size == 1 -> {
                val parts = keys.first().split('/', limit = 2)
                if (parts.size != 2) items else rehome(
                    items.map {
                        if (it.id == folderId) {
                            AppCell(folder.id, folder.page, folder.col, folder.row, parts[0], parts[1])
                        } else {
                            it
                        }
                    }
                )
            }

            else -> rehome(items.map { if (it.id == folderId) folder.copy(keys = keys) else it })
        }
    }

    // ---------------------------------------------------------------- dock

    fun toggleDock(key: String) = store.update { current ->
        val dock = current.dock
        current.copy(dock = if (key in dock) dock - key else (dock + key).takeLast(5))
    }

    fun setDock(keys: List<String>) = store.update { it.copy(dock = keys.take(5)) }
}
