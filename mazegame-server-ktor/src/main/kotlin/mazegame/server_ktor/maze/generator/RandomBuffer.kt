package mazegame.server_ktor.maze.generator

import kotlin.math.max
import kotlin.random.Random

class RandomBuffer<T>(initialCapacity: Int = 16, private val random: Random = Random) : Buffer<T> {

    companion object {
        private const val MAX_ARRAY_SIZE = Integer.MAX_VALUE - 8
    }

    private var internalArray: Array<Any?> = arrayOfNulls(initialCapacity)
    private var lastElement: Int = -1

    override val size: Int
        get() = lastElement + 1
    override val empty: Boolean
        get() = lastElement < 0

    override fun add(element: T) {
        lastElement += 1
        val currentMaxSize = internalArray.size
        if (lastElement >= currentMaxSize) {
            // border case: exceed max array size
            if (lastElement >= MAX_ARRAY_SIZE) {
                throw OutOfMemoryError("Cannot create larger internal array!")
            }
            val newMaxSize: Int = calculateNewSize(currentMaxSize)
            val newArray: Array<Any?> = arrayOfNulls(newMaxSize)
            System.arraycopy(internalArray, 0, newArray, 0, currentMaxSize)
            internalArray = newArray
        }
        internalArray[lastElement] = element
    }

    /**
     * Grow function taken from hornet queen.
     */
    private fun calculateNewSize(currentMaxSize: Int): Int {
        val oldSizeAsLong = currentMaxSize.toLong()
        val newSizeAsLong = max(oldSizeAsLong + (oldSizeAsLong shr 1), 2)
        return if (newSizeAsLong <= MAX_ARRAY_SIZE.toLong()) newSizeAsLong.toInt() else MAX_ARRAY_SIZE
    }

    override fun next(): T {
        val randomPosition: Int = random.nextInt(size)
        val nextElement = internalArray[randomPosition]
        internalArray[randomPosition] = internalArray[lastElement]
        internalArray[lastElement] = null
        lastElement -= 1
        @Suppress("UNCHECKED_CAST")
        return nextElement as T
    }

    override fun clear() {
        for (i in 0..lastElement) {
            internalArray[i] = null
        }
        lastElement = -1
    }

}