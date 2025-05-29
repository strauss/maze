package mazegame.server_ktor.maze.generator

interface Buffer<T> {

    val size: Int
    val empty: Boolean

    /**
     * Retrieves and removes the next element. If the Buffer is empty an Exception is thrown. The order depends on the
     * underlying data structure in the concrete implementation.
     *
     * @exception NoSuchElementException if the Buffer is empty on call
     */
    fun next(): T

    /**
     * Adds an element to this Buffer.
     */
    fun add(element: T)

    /**
     * Clears this Buffer such that it can be reused as if it was just created.
     */
    fun clear()

    /**
     * Convenience method for adding all Elements of this buffer. The default implementation adds the elements in order
     * of the collection's iterator.
     */
    fun addAll(elements: Collection<T>) {
        elements.forEach(::add)
    }

    /**
     * Convenience method for adding all Elements of this buffer. The default implementation adds the elements in order
     * of the collection's iterator.
     */
    fun addAll(vararg elements: T) {
        elements.forEach(::add)
    }

}
