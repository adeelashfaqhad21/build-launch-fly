package com.buildlaunchfly.game.ui

import com.buildlaunchfly.game.aircraft.PartSlot

/** The ten screens from design.md §6, plus the transient pause modal. */
enum class UiScreen {
    MENU,
    AIRCRAFT,
    SLINGSHOT,
    WORLDS,
    SETTINGS,
    ACHIEVEMENTS,
    PROFILE,
    REWARD,
    LAUNCH,
    RESULTS
}

/** Navigation + shared selection state the screens read and mutate. */
interface UiNav {

    var selectedSlot: PartSlot
    var worldCursor: Int
    var rewardSlot: PartSlot?

    fun go(screen: UiScreen)

    fun setPaused(paused: Boolean)

    fun isPaused(): Boolean

    fun back()
}
