package com.cafarovceyxun.anamuslim.utils.others

import platform.UIKit.UIApplication
import platform.UIKit.UIApplicationShortcutItem
import platform.UIKit.setShortcutItems
import platform.darwin.dispatch_async
import platform.darwin.dispatch_get_main_queue

/**
 * Central registry for iOS Home-screen quick actions ([UIApplicationShortcutItem]).
 *
 * iOS has a single `shortcutItems` array, so features cannot each set it independently — the last
 * writer would wipe the others. This holds one item per action type and rebuilds the whole array on
 * every change, letting the "continue reading" and "verse of the day" actions coexist the way
 * Android's separate dynamic shortcuts do. Taps are routed back from the Swift app delegate to the
 * handler registered under the item's type.
 */
object IosQuickActions {

    // Insertion-ordered so the on-icon menu order is stable across updates. Only touched on the
    // main queue (setItem dispatches there), which also serialises the array rebuild UIKit needs.
    private val items = LinkedHashMap<String, UIApplicationShortcutItem>()
    private val handlers = mutableMapOf<String, (UIApplicationShortcutItem) -> Unit>()

    /** Registers the tap handler for an action type. Call once at startup, before any tap. */
    fun registerHandler(type: String, handler: (UIApplicationShortcutItem) -> Unit) {
        handlers[type] = handler
    }

    /** Adds or replaces the item for its type and republishes the full array on the main thread. */
    fun setItem(item: UIApplicationShortcutItem) {
        dispatch_async(dispatch_get_main_queue()) {
            items[item.type] = item
            UIApplication.sharedApplication.setShortcutItems(items.values.toList())
        }
    }

    /** Removes the item for [type], if present, and republishes. */
    fun removeItem(type: String) {
        dispatch_async(dispatch_get_main_queue()) {
            if (items.remove(type) != null) {
                UIApplication.sharedApplication.setShortcutItems(items.values.toList())
            }
        }
    }

    /**
     * Handles a tapped quick action forwarded from the Swift app delegate. Returns whether a handler
     * claimed it, so the delegate can report handling back to iOS.
     */
    fun handle(shortcutItem: UIApplicationShortcutItem): Boolean {
        val handler = handlers[shortcutItem.type] ?: return false
        handler(shortcutItem)
        return true
    }
}
