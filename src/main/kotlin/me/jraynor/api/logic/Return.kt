package me.jraynor.api.logic

/**
 * This is the actual value returned by a function
 */
class Return<V : Any> constructor(
    val type: Type,
    var value: V
) {
    /**
     * This stores the type of return
     */
    enum class Type {
        VOID, VALUE
    }

    /**
     * This is a very simple class to denote void
     */
    class Void

    /**
     * This stores the reference to the void
     */
    companion object {
        /**
         * This will create a return value of void.
         */
        internal fun void(): Return<Void> {
            return Return(
                type = Type.VOID,
                Void()
            )
        }

        /**
         * This will create a return of the given value
         */
        inline fun <reified T : Any> of(valueIn: T): Return<T> {
            return Return(
                type = Type.VALUE,
                value = valueIn
            )
        }

    }

}