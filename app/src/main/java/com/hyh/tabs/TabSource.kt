package com.hyh.tabs


/**
 * Tab源
 *
 * @author eriche
 * @data 2021/5/20
 */
abstract class TabSource<Key : Any, Value : ITab> {

    companion object {
        private const val TAG = "TabSource"
    }

    abstract suspend fun load(params: Key): LoadResult<Value>

    public sealed class LoadResult<Value : ITab> {

        public data class Error<Value : ITab>(
            val throwable: Throwable
        ) : TabSource.LoadResult<Value>()

        public data class TabResult<Value : ITab> constructor(
            val data: List<Value>
        ) : LoadResult<Value>() {

            public companion object {
                public const val COUNT_UNDEFINED: Int = Int.MIN_VALUE

                @Suppress("MemberVisibilityCanBePrivate") // Prevent synthetic accessor generation.
                internal val EMPTY = TabResult(emptyList())

                @Suppress("UNCHECKED_CAST") // Can safely ignore, since the list is empty.
                internal fun <Value : ITab> empty() = EMPTY as TabResult<Value>
            }

        }
    }
}