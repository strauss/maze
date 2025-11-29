/*
 * Maze Game
 * Copyright (c) 2025 Sascha Strauß
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package de.dreamcube.mazegame.server.maze.generator

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
