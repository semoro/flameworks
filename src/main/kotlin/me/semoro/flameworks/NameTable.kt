package me.semoro.flameworks

typealias Id = Int

// binary representation of fractional part of phi = (sqrt(5) - 1) / 2
private const val MAGIC: Int = 0x9E3779B9L.toInt() // ((sqrt(5.0) - 1) / 2 * pow(2.0, 32.0)).toLong().toString(16)
private const val MAX_SHIFT = 27
private const val THRESHOLD = ((1L shl 31) - 1).toInt() // 50% fill factor for speed
private val EMPTY_ARRAY = arrayOf<Any?>()


// For more details see for Knuth's multiplicative hash with golden ratio
// Shortly, we're trying to keep distribution of it uniform independently of input
// It's necessary because we use very simple linear probing
@Suppress("NOTHING_TO_INLINE")
private inline fun Any.computeHash(shift: Int) = (hashCode() * MAGIC) ushr shift

class NameTable {


    // capacity = 1 << (32 - shift)
    private var shift = MAX_SHIFT
    private var size_ = 0

    val size get() = size_

    private var keys = arrayOfNulls<String>(1 shl (32 - shift))
    private var inverse = arrayOfNulls<String>(1 shl (31 - shift))
    private var ids = IntArray(1 shl (32 - shift)) { -1 }

    private var nextId: Id = 0

    fun intern(name: String): Id {
        val id = nextId++
        val result = put(name, id)
        return if (result == -1) {
            inverse[id] = name
            if (++size_ >= (THRESHOLD ushr shift)) {
                rehash()
            }
            id
        } else {
            // already interned
            nextId--
            result
        }
    }

    operator fun get(id: Id): String {
        return inverse[id] ?: error("No name for id = $id")
    }

    private fun put(key: String, value: Int): Int {
        var i = key.computeHash(shift)

        while (true) {
            val k = keys[i]
            if (k == null) {
                keys[i] = key
                ids[i] = value
                return -1
            }
            if (k.equals(key)) break
            if (i == 0) {
                i = keys.size
            }
            i -= 1
        }

        return ids[i]
    }

    private fun checkConsistency() {
        val keysNotNull = keys.filterNotNull()
        val keysNotNullSet = keysNotNull.toSet()
        require(keysNotNullSet.containsAll(keysNotNull))
        require(keysNotNull.size == keysNotNullSet.size) {
            "Size differs"
        }
    }

    private fun rehash() {
        val newShift = maxOf(shift - 3, 0)
        val newArraySize = 1 shl (32 - newShift)
        val newInverseSize = 1 shl (31 - newShift)
        val oldKeys = keys
        val oldIds = ids
        keys = arrayOfNulls(newArraySize)
        inverse = arrayOfNulls(newInverseSize)
        ids = IntArray(newArraySize) { -1 }

        shift = newShift

        var i = 0
        val arraySize = oldKeys.size
        while (i < arraySize) {
            val key = oldKeys[i]
            if (key != null) {
                put(key, oldIds[i])
                inverse[oldIds[i]] = key
            }
            i += 1
        }
    }
}