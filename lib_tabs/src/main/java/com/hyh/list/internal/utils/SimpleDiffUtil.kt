package com.hyh.list.internal.utils

import androidx.annotation.IntRange
import androidx.recyclerview.widget.AdapterListUpdateCallback
import androidx.recyclerview.widget.BatchingListUpdateCallback
import androidx.recyclerview.widget.ListUpdateCallback
import androidx.recyclerview.widget.RecyclerView
import java.util.*

/**
 * copy from [androidx.recyclerview.widget.DiffUtil] version:1.2.0
 *
 * @author eriche
 * @data 2021/1/29
 */
object SimpleDiffUtil {

    private val DIAGONAL_COMPARATOR: Comparator<Diagonal> =
        Comparator { o1, o2 -> o1.x - o2.x }

    /**
     * Calculates the list of update operations that can covert one list into the other one.
     *
     *
     * If your old and new lists are sorted by the same constraint and items never move (swap
     * positions), you can disable move detection which takes `O(N^2)` time where
     * N is the number of added, moved, removed items.
     *
     * @param cb The callback that acts as a gateway to the backing list data
     * @param detectMoves True if DiffUtil should try to detect moved items, false otherwise.
     *
     * @return A DiffResult that contains the information about the edit sequence to convert the
     * old list into the new list.
     */
    @JvmOverloads
    fun calculateDiff(
        cb: Callback,
        detectMoves: Boolean = true
    ): DiffResult {
        val oldSize = cb.oldListSize
        val newSize = cb.newListSize
        val diagonals: MutableList<Diagonal> = ArrayList()

        // instead of a recursive implementation, we keep our own stack to avoid potential stack
        // overflow exceptions
        val stack: MutableList<Range> =
            ArrayList()
        stack.add(Range(0, oldSize, 0, newSize))
        val max = (oldSize + newSize + 1) / 2
        // allocate forward and backward k-lines. K lines are diagonal lines in the matrix. (see the
        // paper for details)
        // These arrays lines keep the max reachable position for each k-line.
        val forward = CenteredArray(max * 2 + 1)
        val backward = CenteredArray(max * 2 + 1)

        // We pool the ranges to avoid allocations for each recursive call.
        val rangePool: MutableList<Range> =
            ArrayList()
        while (stack.isNotEmpty()) {
            val range = stack.removeAt(stack.size - 1)
            val snake = midPoint(range, cb, forward, backward)
            if (snake != null) {
                // if it has a diagonal, save it
                if (snake.diagonalSize() > 0) {
                    diagonals.add(snake.toDiagonal())
                }
                // add new ranges for left and right
                val left =
                    if (rangePool.isEmpty()) Range() else rangePool.removeAt(
                        rangePool.size - 1
                    )
                left.oldListStart = range.oldListStart
                left.newListStart = range.newListStart
                left.oldListEnd = snake.startX
                left.newListEnd = snake.startY
                stack.add(left)

                // re-use range for right
                range.oldListEnd = range.oldListEnd
                range.newListEnd = range.newListEnd
                range.oldListStart = snake.endX
                range.newListStart = snake.endY
                stack.add(range)
            } else {
                rangePool.add(range)
            }
        }
        // sort snakes
        Collections.sort(diagonals, DIAGONAL_COMPARATOR)
        return DiffResult(
            cb, diagonals,
            forward.backingData(), backward.backingData(),
            detectMoves
        )
    }

    /**
     * Finds a middle snake in the given range.
     */
    private fun midPoint(
        range: Range,
        cb: Callback,
        forward: CenteredArray,
        backward: CenteredArray
    ): Snake? {
        if (range.oldSize() < 1 || range.newSize() < 1) {
            return null
        }
        val max = (range.oldSize() + range.newSize() + 1) / 2
        forward[1] = range.oldListStart
        backward[1] = range.oldListEnd
        for (d in 0 until max) {
            var snake = forward(range, cb, forward, backward, d)
            if (snake != null) {
                return snake
            }
            snake = backward(range, cb, forward, backward, d)
            if (snake != null) {
                return snake
            }
        }
        return null
    }

    private fun forward(
        range: Range,
        cb: Callback,
        forward: CenteredArray,
        backward: CenteredArray,
        d: Int
    ): Snake? {
        val checkForSnake =
            Math.abs(range.oldSize() - range.newSize()) % 2 == 1
        val delta = range.oldSize() - range.newSize()
        var k = -d
        while (k <= d) {

            // we either come from d-1, k-1 OR d-1. k+1
            // as we move in steps of 2, array always holds both current and previous d values
            // k = x - y and each array value holds the max X, y = x - k
            val startX: Int
            val startY: Int
            var x: Int
            var y: Int
            if (k == -d || k != d && forward[k + 1] > forward[k - 1]) {
                // picking k + 1, incrementing Y (by simply not incrementing X)
                startX = forward[k + 1]
                x = startX
            } else {
                // picking k - 1, incrementing X
                startX = forward[k - 1]
                x = startX + 1
            }
            y = range.newListStart + (x - range.oldListStart) - k
            startY = if (d == 0 || x != startX) y else y - 1
            // now find snake size
            while (x < range.oldListEnd && y < range.newListEnd && cb.areItemsTheSame(x, y)
            ) {
                x++
                y++
            }
            // now we have furthest reaching x, record it
            forward[k] = x
            if (checkForSnake) {
                // see if we did pass over a backwards array
                // mapping function: delta - k
                val backwardsK = delta - k
                // if backwards K is calculated and it passed me, found match
                if (backwardsK >= -d + 1 && backwardsK <= d - 1 && backward[backwardsK] <= x
                ) {
                    // match
                    val snake = Snake()
                    snake.startX = startX
                    snake.startY = startY
                    snake.endX = x
                    snake.endY = y
                    snake.reverse = false
                    return snake
                }
            }
            k += 2
        }
        return null
    }

    private fun backward(
        range: Range,
        cb: Callback,
        forward: CenteredArray,
        backward: CenteredArray,
        d: Int
    ): Snake? {
        val checkForSnake = (range.oldSize() - range.newSize()) % 2 == 0
        val delta = range.oldSize() - range.newSize()
        // same as forward but we go backwards from end of the lists to be beginning
        // this also means we'll try to optimize for minimizing x instead of maximizing it
        var k = -d
        while (k <= d) {

            // we either come from d-1, k-1 OR d-1, k+1
            // as we move in steps of 2, array always holds both current and previous d values
            // k = x - y and each array value holds the MIN X, y = x - k
            // when x's are equal, we prioritize deletion over insertion
            val startX: Int
            val startY: Int
            var x: Int
            var y: Int
            if (k == -d || k != d && backward[k + 1] < backward[k - 1]) {
                // picking k + 1, decrementing Y (by simply not decrementing X)
                startX = backward[k + 1]
                x = startX
            } else {
                // picking k - 1, decrementing X
                startX = backward[k - 1]
                x = startX - 1
            }
            y = range.newListEnd - (range.oldListEnd - x - k)
            startY = if (d == 0 || x != startX) y else y + 1
            // now find snake size
            while (x > range.oldListStart && y > range.newListStart && cb.areItemsTheSame(
                    x - 1,
                    y - 1
                )
            ) {
                x--
                y--
            }
            // now we have furthest point, record it (min X)
            backward[k] = x
            if (checkForSnake) {
                // see if we did pass over a backwards array
                // mapping function: delta - k
                val forwardsK = delta - k
                // if forwards K is calculated and it passed me, found match
                if (forwardsK >= -d && forwardsK <= d && forward[forwardsK] >= x
                ) {
                    // match
                    val snake = Snake()
                    // assignment are reverse since we are a reverse snake
                    snake.startX = x
                    snake.startY = y
                    snake.endX = startX
                    snake.endY = startY
                    snake.reverse = true
                    return snake
                }
            }
            k += 2
        }
        return null
    }

    /**
     * A Callback class used by DiffUtil while calculating the diff between two lists.
     */
    abstract class Callback {
        /**
         * Returns the size of the old list.
         *
         * @return The size of the old list.
         */
        abstract val oldListSize: Int

        /**
         * Returns the size of the new list.
         *
         * @return The size of the new list.
         */
        abstract val newListSize: Int

        /**
         * Called by the DiffUtil to decide whether two object represent the same Item.
         *
         *
         * For example, if your items have unique ids, this method should check their id equality.
         *
         * @param oldItemPosition The position of the item in the old list
         * @param newItemPosition The position of the item in the new list
         * @return True if the two items represent the same object or false if they are different.
         */
        abstract fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean

        /**
         * Called by the DiffUtil when it wants to check whether two items have the same data.
         * DiffUtil uses this information to detect if the contents of an item has changed.
         *
         *
         * DiffUtil uses this method to check equality instead of [Object.equals]
         * so that you can change its behavior depending on your UI.
         * For example, if you are using DiffUtil with a
         * [RecyclerView.Adapter], you should
         * return whether the items' visual representations are the same.
         *
         *
         * This method is called only if [.areItemsTheSame] returns
         * `true` for these items.
         *
         * @param oldItemPosition The position of the item in the old list
         * @param newItemPosition The position of the item in the new list which replaces the
         * oldItem
         * @return True if the contents of the items are the same or false if they are different.
         */
        abstract fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean

        /**
         * When [.areItemsTheSame] returns `true` for two items and
         * [.areContentsTheSame] returns false for them, DiffUtil
         * calls this method to get a payload about the change.
         *
         *
         * For example, if you are using DiffUtil with [RecyclerView], you can return the
         * particular field that changed in the item and your
         * [ItemAnimator][RecyclerView.ItemAnimator] can use that
         * information to run the correct animation.
         *
         *
         * Default implementation returns `null`.
         *
         * @param oldItemPosition The position of the item in the old list
         * @param newItemPosition The position of the item in the new list
         * @return A payload object that represents the change between the two items.
         */
        open fun getChangePayload(oldItemPosition: Int, newItemPosition: Int): Any? {
            return null
        }
    }

    /**
     * Callback for calculating the diff between two non-null items in a list.
     *
     *
     * [Callback] serves two roles - list indexing, and item diffing. ItemCallback handles
     * just the second of these, which allows separation of code that indexes into an array or List
     * from the presentation-layer and content specific diffing code.
     *
     * @param <T> Type of items to compare.
    </T> */
    abstract class ItemCallback<T> {
        /**
         * Called to check whether two objects represent the same item.
         *
         *
         * For example, if your items have unique ids, this method should check their id equality.
         *
         *
         * Note: `null` items in the list are assumed to be the same as another `null`
         * item and are assumed to not be the same as a non-`null` item. This callback will
         * not be invoked for either of those cases.
         *
         * @param oldItem The item in the old list.
         * @param newItem The item in the new list.
         * @return True if the two items represent the same object or false if they are different.
         * @see Callback.areItemsTheSame
         */
        abstract fun areItemsTheSame(oldItem: T, newItem: T): Boolean

        /**
         * Called to check whether two items have the same data.
         *
         *
         * This information is used to detect if the contents of an item have changed.
         *
         *
         * This method to check equality instead of [Object.equals] so that you can
         * change its behavior depending on your UI.
         *
         *
         * For example, if you are using DiffUtil with a
         * [RecyclerView.Adapter], you should
         * return whether the items' visual representations are the same.
         *
         *
         * This method is called only if [.areItemsTheSame] returns `true` for
         * these items.
         *
         *
         * Note: Two `null` items are assumed to represent the same contents. This callback
         * will not be invoked for this case.
         *
         * @param oldItem The item in the old list.
         * @param newItem The item in the new list.
         * @return True if the contents of the items are the same or false if they are different.
         * @see Callback.areContentsTheSame
         */
        abstract fun areContentsTheSame(oldItem: T, newItem: T): Boolean

        /**
         * When [.areItemsTheSame] returns `true` for two items and
         * [.areContentsTheSame] returns false for them, this method is called to
         * get a payload about the change.
         *
         *
         * For example, if you are using DiffUtil with [RecyclerView], you can return the
         * particular field that changed in the item and your
         * [ItemAnimator][RecyclerView.ItemAnimator] can use that
         * information to run the correct animation.
         *
         *
         * Default implementation returns `null`.
         *
         * @see Callback.getChangePayload
         */
        fun getChangePayload(oldItem: T, newItem: T): Any? {
            return null
        }
    }

    /**
     * A diagonal is a match in the graph.
     * Rather than snakes, we only record the diagonals in the path.
     */
    internal class Diagonal(val x: Int, val y: Int, val size: Int) {
        fun endX(): Int {
            return x + size
        }

        fun endY(): Int {
            return y + size
        }

    }

    /**
     * Snakes represent a match between two lists. It is optionally prefixed or postfixed with an
     * add or remove operation. See the Myers' paper for details.
     */
    internal class Snake {
        /**
         * Position in the old list
         */
        var startX = 0

        /**
         * Position in the new list
         */
        var startY = 0

        /**
         * End position in the old list, exclusive
         */
        var endX = 0

        /**
         * End position in the new list, exclusive
         */
        var endY = 0

        /**
         * True if this snake was created in the reverse search, false otherwise.
         */
        var reverse = false
        fun hasAdditionOrRemoval(): Boolean {
            return endY - startY != endX - startX
        }

        val isAddition: Boolean
            get() = endY - startY > endX - startX

        fun diagonalSize(): Int {
            return Math.min(endX - startX, endY - startY)
        }

        /**
         * Extract the diagonal of the snake to make reasoning easier for the rest of the
         * algorithm where we try to produce a path and also find moves.
         */
        fun toDiagonal(): Diagonal {
            return if (hasAdditionOrRemoval()) {
                if (reverse) {
                    // snake edge it at the end
                    Diagonal(startX, startY, diagonalSize())
                } else {
                    // snake edge it at the beginning
                    if (isAddition) {
                        Diagonal(startX, startY + 1, diagonalSize())
                    } else {
                        Diagonal(startX + 1, startY, diagonalSize())
                    }
                }
            } else {
                // we are a pure diagonal
                Diagonal(startX, startY, endX - startX)
            }
        }
    }

    /**
     * Represents a range in two lists that needs to be solved.
     *
     *
     * This internal class is used when running Myers' algorithm without recursion.
     *
     *
     * Ends are exclusive
     */
    internal class Range {
        var oldListStart = 0
        var oldListEnd = 0
        var newListStart = 0
        var newListEnd = 0

        constructor() {}
        constructor(oldListStart: Int, oldListEnd: Int, newListStart: Int, newListEnd: Int) {
            this.oldListStart = oldListStart
            this.oldListEnd = oldListEnd
            this.newListStart = newListStart
            this.newListEnd = newListEnd
        }

        fun oldSize(): Int {
            return oldListEnd - oldListStart
        }

        fun newSize(): Int {
            return newListEnd - newListStart
        }
    }

    /**
     * This class holds the information about the result of a
     * [DiffUtil.calculateDiff] call.
     *
     *
     * You can consume the updates in a DiffResult via
     * [.dispatchUpdatesTo] or directly stream the results into a
     * [RecyclerView.Adapter] via [.dispatchUpdatesTo].
     */
    class DiffResult internal constructor(
        callback: Callback,
        // The diagonals extracted from The Myers' snakes.
        private val diagonals: MutableList<Diagonal>,
        // The list to keep oldItemStatuses. As we traverse old items, we assign flags to them
        // which also includes whether they were a real removal or a move (and its new index).
        private val oldItemStatuses: IntArray,
        // The list to keep newItemStatuses. As we traverse new items, we assign flags to them
        // which also includes whether they were a real addition or a move(and its old index).
        private val newItemStatuses: IntArray,
        detectMoves: Boolean
    ) {

        // The callback that was given to calculate diff method.
        private val callback: Callback
        private val oldListSize: Int
        private val newListSize: Int
        private val detectMoves: Boolean

        /**
         * Add edge diagonals so that we can iterate as long as there are diagonals w/o lots of
         * null checks around
         */
        private fun addEdgeDiagonals() {
            val first = if (diagonals.isEmpty()) null else diagonals[0]
            // see if we should add 1 to the 0,0
            if (first == null || first.x != 0 || first.y != 0) {
                diagonals.add(0, Diagonal(0, 0, 0))
            }
            // always add one last
            diagonals.add(Diagonal(oldListSize, newListSize, 0))
        }

        /**
         * Find position mapping from old list to new list.
         * If moves are requested, we'll also try to do an n^2 search between additions and
         * removals to find moves.
         */
        private fun findMatchingItems() {
            for (diagonal in diagonals) {
                for (offset in 0 until diagonal.size) {
                    val posX = diagonal.x + offset
                    val posY = diagonal.y + offset
                    val theSame = callback.areContentsTheSame(posX, posY)
                    val changeFlag =
                        if (theSame) FLAG_NOT_CHANGED else FLAG_CHANGED
                    oldItemStatuses[posX] =
                        posY shl FLAG_OFFSET or changeFlag
                    newItemStatuses[posY] =
                        posX shl FLAG_OFFSET or changeFlag
                }
            }
            // now all matches are marked, lets look for moves
            if (detectMoves) {
                // traverse each addition / removal from the end of the list, find matching
                // addition removal from before
                findMoveMatches()
            }
        }

        private fun findMoveMatches() {
            // for each removal, find matching addition
            var posX = 0
            for (diagonal in diagonals) {
                while (posX < diagonal.x) {
                    if (oldItemStatuses[posX] == 0) {
                        // there is a removal, find matching addition from the rest
                        findMatchingAddition(posX)
                    }
                    posX++
                }
                // snap back for the next diagonal
                posX = diagonal.endX()
            }
        }

        /**
         * Search the whole list to find the addition for the given removal of position posX
         *
         * @param posX position in the old list
         */
        private fun findMatchingAddition(posX: Int) {
            var posY = 0
            val diagonalsSize = diagonals.size
            for (i in 0 until diagonalsSize) {
                val diagonal = diagonals[i]
                while (posY < diagonal.y) {
                    // found some additions, evaluate
                    if (newItemStatuses[posY] == 0) { // not evaluated yet
                        val matching = callback.areItemsTheSame(posX, posY)
                        if (matching) {
                            // yay found it, set values
                            val contentsMatching =
                                callback.areContentsTheSame(posX, posY)
                            val changeFlag =
                                if (contentsMatching) FLAG_MOVED_NOT_CHANGED else FLAG_MOVED_CHANGED
                            // once we process one of these, it will mark the other one as ignored.
                            oldItemStatuses[posX] =
                                posY shl FLAG_OFFSET or changeFlag
                            newItemStatuses[posY] =
                                posX shl FLAG_OFFSET or changeFlag
                            return
                        }
                    }
                    posY++
                }
                posY = diagonal.endY()
            }
        }

        /**
         * Given a position in the old list, returns the position in the new list, or
         * `NO_POSITION` if it was removed.
         *
         * @param oldListPosition Position of item in old list
         * @return Position of item in new list, or `NO_POSITION` if not present.
         * @see .NO_POSITION
         *
         * @see .convertNewPositionToOld
         */
        fun convertOldPositionToNew(@IntRange(from = 0) oldListPosition: Int): Int {
            if (oldListPosition < 0 || oldListPosition >= oldListSize) {
                throw IndexOutOfBoundsException(
                    "Index out of bounds - passed position = "
                            + oldListPosition + ", old list size = " + oldListSize
                )
            }
            val status = oldItemStatuses[oldListPosition]
            return if (status and FLAG_MASK == 0) {
                NO_POSITION
            } else {
                status shr FLAG_OFFSET
            }
        }

        /**
         * Given a position in the new list, returns the position in the old list, or
         * `NO_POSITION` if it was removed.
         *
         * @param newListPosition Position of item in new list
         * @return Position of item in old list, or `NO_POSITION` if not present.
         * @see .NO_POSITION
         *
         * @see .convertOldPositionToNew
         */
        fun convertNewPositionToOld(@IntRange(from = 0) newListPosition: Int): Int {
            if (newListPosition < 0 || newListPosition >= newListSize) {
                throw IndexOutOfBoundsException(
                    "Index out of bounds - passed position = "
                            + newListPosition + ", new list size = " + newListSize
                )
            }
            val status = newItemStatuses[newListPosition]
            return if (status and FLAG_MASK == 0) {
                NO_POSITION
            } else {
                status shr FLAG_OFFSET
            }
        }

        /**
         * Dispatches the update events to the given adapter.
         *
         *
         * For example, if you have an [Adapter][RecyclerView.Adapter]
         * that is backed by a [List], you can swap the list with the new one then call this
         * method to dispatch all updates to the RecyclerView.
         * <pre>
         * List oldList = mAdapter.getData();
         * DiffResult result = DiffUtil.calculateDiff(new MyCallback(oldList, newList));
         * mAdapter.setData(newList);
         * result.dispatchUpdatesTo(mAdapter);
        </pre> *
         *
         *
         * Note that the RecyclerView requires you to dispatch adapter updates immediately when you
         * change the data (you cannot defer `notify*` calls). The usage above adheres to this
         * rule because updates are sent to the adapter right after the backing data is changed,
         * before RecyclerView tries to read it.
         *
         *
         * On the other hand, if you have another
         * [AdapterDataObserver][RecyclerView.AdapterDataObserver]
         * that tries to process events synchronously, this may confuse that observer because the
         * list is instantly moved to its final state while the adapter updates are dispatched later
         * on, one by one. If you have such an
         * [AdapterDataObserver][RecyclerView.AdapterDataObserver],
         * you can use
         * [.dispatchUpdatesTo] to handle each modification
         * manually.
         *
         * @param adapter A RecyclerView adapter which was displaying the old list and will start
         * displaying the new list.
         * @see AdapterListUpdateCallback
         */
        fun dispatchUpdatesTo(adapter: RecyclerView.Adapter<*>) {
            dispatchUpdatesTo(AdapterListUpdateCallback(adapter))
        }

        /**
         * Dispatches update operations to the given Callback.
         *
         *
         * These updates are atomic such that the first update call affects every update call that
         * comes after it (the same as RecyclerView).
         *
         * @param updateCallback The callback to receive the update operations.
         * @see .dispatchUpdatesTo
         */
        fun dispatchUpdatesTo(updateCallback: ListUpdateCallback) {
            val batchingCallback: BatchingListUpdateCallback =
                if (updateCallback is BatchingListUpdateCallback) {
                    updateCallback
                } else {
                    BatchingListUpdateCallback(updateCallback)
                    // replace updateCallback with a batching callback and override references to
                    // updateCallback so that we don't call it directly by mistake
                }
            // track up to date current list size for moves
            // when a move is found, we record its position from the end of the list (which is
            // less likely to change since we iterate in reverse).
            // Later when we find the match of that move, we dispatch the update
            var currentListSize = oldListSize
            // list of postponed moves
            val postponedUpdates: MutableCollection<PostponedUpdate> =
                ArrayDeque()
            // posX and posY are exclusive
            var posX = oldListSize
            var posY = newListSize
            // iterate from end of the list to the beginning.
            // this just makes offsets easier since changes in the earlier indices has an effect
            // on the later indices.
            for (diagonalIndex in diagonals.indices.reversed()) {
                val diagonal = diagonals[diagonalIndex]
                val endX = diagonal.endX()
                val endY = diagonal.endY()
                // dispatch removals and additions until we reach to that diagonal
                // first remove then add so that it can go into its place and we don't need
                // to offset values
                while (posX > endX) {
                    posX--
                    // REMOVAL
                    val status = oldItemStatuses[posX]
                    if (status and FLAG_MOVED != 0) {
                        val newPos = status shr FLAG_OFFSET
                        // get postponed addition
                        val postponedUpdate =
                            getPostponedUpdate(
                                postponedUpdates,
                                newPos, false
                            )
                        if (postponedUpdate != null) {
                            // this is an addition that was postponed. Now dispatch it.
                            val updatedNewPos = currentListSize - postponedUpdate.currentPos
                            batchingCallback.onMoved(posX, updatedNewPos - 1)
                            if (status and FLAG_MOVED_CHANGED != 0) {
                                val changePayload =
                                    callback.getChangePayload(posX, newPos)
                                batchingCallback.onChanged(updatedNewPos - 1, 1, changePayload)
                            }
                        } else {
                            // first time we are seeing this, we'll see a matching addition
                            postponedUpdates.add(
                                PostponedUpdate(
                                    posX,
                                    currentListSize - posX - 1,
                                    true
                                )
                            )
                        }
                    } else {
                        // simple removal
                        batchingCallback.onRemoved(posX, 1)
                        currentListSize--
                    }
                }
                while (posY > endY) {
                    posY--
                    // ADDITION
                    val status = newItemStatuses[posY]
                    if (status and FLAG_MOVED != 0) {
                        // this is a move not an addition.
                        // see if this is postponed
                        val oldPos = status shr FLAG_OFFSET
                        // get postponed removal
                        val postponedUpdate =
                            getPostponedUpdate(
                                postponedUpdates,
                                oldPos, true
                            )
                        // empty size returns 0 for indexOf
                        if (postponedUpdate == null) {
                            // postpone it until we see the removal
                            postponedUpdates.add(
                                PostponedUpdate(
                                    posY,
                                    currentListSize - posX,
                                    false
                                )
                            )
                        } else {
                            // oldPosFromEnd = foundListSize - posX
                            // we can find posX if we swap the list sizes
                            // posX = listSize - oldPosFromEnd
                            val updatedOldPos =
                                currentListSize - postponedUpdate.currentPos - 1
                            batchingCallback.onMoved(updatedOldPos, posX)
                            if (status and FLAG_MOVED_CHANGED != 0) {
                                val changePayload =
                                    callback.getChangePayload(oldPos, posY)
                                batchingCallback.onChanged(posX, 1, changePayload)
                            }
                        }
                    } else {
                        // simple addition
                        batchingCallback.onInserted(posX, 1)
                        currentListSize++
                    }
                }
                // now dispatch updates for the diagonal
                posX = diagonal.x
                posY = diagonal.y
                for (i in 0 until diagonal.size) {
                    // dispatch changes
                    if (oldItemStatuses[posX] and FLAG_MASK == FLAG_CHANGED) {
                        val changePayload = callback.getChangePayload(posX, posY)
                        batchingCallback.onChanged(posX, 1, changePayload)
                    }
                    posX++
                    posY++
                }
                // snap back for the next diagonal
                posX = diagonal.x
                posY = diagonal.y
            }
            batchingCallback.dispatchLastEvent()
        }

        companion object {
            /**
             * Signifies an item not present in the list.
             */
            const val NO_POSITION = -1

            /**
             * While reading the flags below, keep in mind that when multiple items move in a list,
             * Myers's may pick any of them as the anchor item and consider that one NOT_CHANGED while
             * picking others as additions and removals. This is completely fine as we later detect
             * all moves.
             *
             *
             * Below, when an item is mentioned to stay in the same "location", it means we won't
             * dispatch a move/add/remove for it, it DOES NOT mean the item is still in the same
             * position.
             */
            // item stayed the same.
            private const val FLAG_NOT_CHANGED = 1

            // item stayed in the same location but changed.
            private const val FLAG_CHANGED = FLAG_NOT_CHANGED shl 1

            // Item has moved and also changed.
            private const val FLAG_MOVED_CHANGED = FLAG_CHANGED shl 1

            // Item has moved but did not change.
            private const val FLAG_MOVED_NOT_CHANGED =
                FLAG_MOVED_CHANGED shl 1

            // Item moved
            private const val FLAG_MOVED =
                FLAG_MOVED_CHANGED or FLAG_MOVED_NOT_CHANGED

            // since we are re-using the int arrays that were created in the Myers' step, we mask
            // change flags
            private const val FLAG_OFFSET = 4
            private const val FLAG_MASK = (1 shl FLAG_OFFSET) - 1
            private fun getPostponedUpdate(
                postponedUpdates: MutableCollection<PostponedUpdate>,
                posInList: Int,
                removal: Boolean
            ): PostponedUpdate? {
                var postponedUpdate: PostponedUpdate? = null
                val itr =
                    postponedUpdates.iterator()
                while (itr.hasNext()) {
                    val update = itr.next()
                    if (update.posInOwnerList == posInList && update.removal == removal) {
                        postponedUpdate = update
                        itr.remove()
                        break
                    }
                }
                while (itr.hasNext()) {
                    // re-offset all others
                    val update = itr.next()
                    if (removal) {
                        update.currentPos--
                    } else {
                        update.currentPos++
                    }
                }
                return postponedUpdate
            }
        }

        /**
         * @param callback        The callback that was used to calculate the diff
         * @param diagonals       Matches between the two lists
         * @param oldItemStatuses An int[] that can be re-purposed to keep metadata
         * @param newItemStatuses An int[] that can be re-purposed to keep metadata
         * @param detectMoves     True if this DiffResult will try to detect moved items
         */
        init {
            Arrays.fill(oldItemStatuses, 0)
            Arrays.fill(newItemStatuses, 0)
            this.callback = callback
            oldListSize = callback.oldListSize
            newListSize = callback.newListSize
            this.detectMoves = detectMoves
            addEdgeDiagonals()
            findMatchingItems()
        }
    }

    /**
     * Represents an update that we skipped because it was a move.
     *
     *
     * When an update is skipped, it is tracked as other updates are dispatched until the matching
     * add/remove operation is found at which point the tracked position is used to dispatch the
     * update.
     */
    private class PostponedUpdate internal constructor(
        /**
         * position in the list that owns this item
         */
        var posInOwnerList: Int,
        /**
         * position wrt to the end of the list
         */
        var currentPos: Int,
        /**
         * true if this is a removal, false otherwise
         */
        var removal: Boolean
    )

    /**
     * Array wrapper w/ negative index support.
     * We use this array instead of a regular array so that algorithm is easier to read without
     * too many offsets when accessing the "k" array in the algorithm.
     */
    internal class CenteredArray(size: Int) {
        private val data: IntArray = IntArray(size)
        private val mid: Int = data.size / 2
        operator fun get(index: Int): Int {
            return data[index + mid]
        }

        fun backingData(): IntArray {
            return data
        }

        operator fun set(index: Int, value: Int) {
            data[index + mid] = value
        }

        fun fill(value: Int) {
            Arrays.fill(data, value)
        }
    }
}
