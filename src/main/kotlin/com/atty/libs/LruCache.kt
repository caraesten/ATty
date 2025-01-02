package com.atty.libs

import java.util.concurrent.Executors

/**
 * LRU cache for images (in-memory), thread-safe
 */
class LruCache<K, V> (private val size: Int) {
    private val lock = Object()
    private val entries: LinkedHashMap<K, V> = LinkedHashMap(size)
    private val trimThreadPool = Executors.newFixedThreadPool(1)

    fun get(key: K, ifNotFound: (K) -> V): V {
        if (entries.contains(key)) {
            return synchronized(lock) {
                entries[key]!!.also { value ->
                    entries.remove(key)
                    entries[key] = value
                }
            }
        } else {
            val newValue = ifNotFound(key)
            return newValue.also {
                synchronized(lock) {
                    entries[key] = newValue
                    if (entries.size > size) {
                        trim()
                    }
                }
            }
        }
    }

    private fun trim() {
        // Clean up after return
        trimThreadPool.submit {
            synchronized(lock) {
                if (entries.size < size) return@submit
                val iter = entries.iterator()
                var lastEntry: Map.Entry<K, V>? = null
                while (iter.hasNext()) {
                    lastEntry = iter.next()
                }
                if (lastEntry != null) {
                    entries.remove(lastEntry.key)
                }
            }
        }
    }
}
