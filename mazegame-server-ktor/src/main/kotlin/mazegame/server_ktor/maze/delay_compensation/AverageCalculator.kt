package mazegame.server_ktor.maze.delay_compensation

/**
 * Interface for defining a class that is capable of calculating average values for number values.
 */
interface AverageCalculator<T : Number> {
    /**
     * Adds a value.
     */
    fun addValue(value: T)

    /**
     * Calculates and returns the current average value.
     */
    val average: T

    /**
     * Retrieves the current sum of values.
     */
    val sum: T

    /**
     * Retrieves the number of elements used to calculate the average value.
     */
    val numberOfRelevantElements: Int

    /**
     * Resets this average calculator. After this operation the average and the sum have to be 0 and the instance can be reused.
     */
    fun reset()
}