package me.semoro.flameworks

import java.util.BitSet


fun TraceTree.mergedSubTrees(select: FrameIdPredicate): TraceTree {
    with(FrameIdArray(this.ids)) {
        val inSelectedTree = BitSet(nodeCount)
        traverse {
            if (select.matches(it)) {
                inSelectedTree[it.idx] = true
            }
            if (it.hasParent() && inSelectedTree[it.parent().idx]) {
                inSelectedTree[it.idx] = true
            }
        }

        val builderPosition = IntArray(nodeCount) { -1 }
        val builder = TreeStructBuilder()
        traverse {
            if (inSelectedTree[it.idx]) {
                if (it.hasParent() && !select.matches(it)) {
                    builder.currentIdx = builderPosition[it.parent().idx]
                } else {
                    builder.currentIdx = -1
                }
                builderPosition[it.idx] = builder.findOrInsertChild(ids[it.idx]).idx
            }
        }
        val (result, mapping) = builder.build()

        val newOwnSamples = IntArray(result.nodeCount)
        traverse {
            val pos = builderPosition[it.idx]
            if (pos != -1) {
                newOwnSamples[mapping[pos]] += ownSamples[it.idx]
            }
        }

        return TraceTree(nameTable, result, newOwnSamples)
    }
}

/**
 * Data-container for arbitrary tree in DFS layout
 * ids - unique keys
 * nextSibling - index of next sibling for node or -1 if node is a last in parent
 * parents - index of parent for node
 */
class TreeStruct(
    val ids: IntArray,
    val nextSibling: IntArray,
    val parents: IntArray,
    val maxDepth: Int
) {
    val nodeCount = ids.size
}


/**
 * Represents the result of building a tree structure.
 *
 * @property struct The built tree structure.
 * @property idxMapping An integer array mapping builder indices to their corresponding index in the built tree structure.
 */
class TreeStructBuildingResult(val struct: TreeStruct, val idxMapping: IntArray) {
    operator fun component1() = struct
    operator fun component2() = idxMapping
}

/**
 * This class allows to build a custom tree structure
 */
class TreeStructBuilder {
    private var capacity = 16

    private var ids = IntArray(capacity) { -1 }
    private var firstChild = IntArray(capacity) { -1 }
    private var nextSibling = IntArray(capacity) { -1 }

    var currentIdx = -1
    var treeSize = 0

    /**
     * Appends element to the end of the tree,
     * currentIdx now points to newly inserted element,
     * treeSize is updated
     */
    private fun append(id: Int) {
        if (treeSize >= capacity) { // need to resize buffers now
            val prevCapacity = capacity
            capacity *= 2
            ids = ids.copyOf(capacity).also { it.fill(-1, fromIndex = prevCapacity) }
            firstChild = firstChild.copyOf(capacity).also { it.fill(-1, fromIndex = prevCapacity) }
            nextSibling = nextSibling.copyOf(capacity).also { it.fill(-1, fromIndex = prevCapacity) }
        }
        currentIdx = treeSize
        require(ids[currentIdx] == -1) { "Append have encountered already used id" }
        ids[currentIdx] = id
        treeSize++
    }

    /**
     * Reset insertion cursor back to the root
     */
    fun rewind() {
        currentIdx = -1
    }

    /**
     * @param id unique key that will be used to find child among siblings during find or insert
     */
    fun findOrInsertChild(id: Int): Node {
        var childIdx = 0
        if (currentIdx == -1) {
            if (treeSize > 0) {
                currentIdx = 0
            }
            childIdx = currentIdx
        } else {
            childIdx = firstChild[currentIdx]
        }
        // first, we need to enter the first child of the current node
        if (childIdx == -1) {
            val parentIdx = currentIdx
            // we reached leaf node, let's jump to position where we can append
            append(id)
            if (parentIdx != -1) {
                firstChild[parentIdx] = currentIdx
            }
            return Node(currentIdx)
        }

        // second, we have found node with children, we need to preform linear compare of ids
        while (true) {
            if (ids[childIdx] == id) {
                // we got match, store position and return
                currentIdx = childIdx
                return Node(currentIdx)
            }
            val next = nextSibling[childIdx]
            if (next == -1) {
                append(id)
                nextSibling[childIdx] = currentIdx
                return Node(currentIdx)
            }
            childIdx = next
        }
    }

    /**
     * Convert builder intermediate structures into dense direct dfs structure
     */
    fun build(): TreeStructBuildingResult {
        val outIds = IntArray(treeSize)
        val outNextSibling = IntArray(treeSize) { -1 }
        val outParents = IntArray(treeSize)
        val outMapping = IntArray(treeSize)

        var outPos = 0

        var maxDepth = 0
        var currentDepth = 0

        fun traverse(idx: Int, hasNext: Boolean, parent: Int) {
            val nodeOutPos = outPos
            outIds[nodeOutPos] = ids[idx]
            outParents[nodeOutPos] = parent
            outMapping[idx] = outPos
            outPos++
            // enter
            currentDepth++
            if (currentDepth > maxDepth) maxDepth = currentDepth

            val firstChildIdx = firstChild[idx]

            var childIdx = firstChildIdx
            while (childIdx != -1) {
                traverse(childIdx, hasNext = nextSibling[childIdx] != -1, nodeOutPos)
                childIdx = nextSibling[childIdx]
            }
            // exit
            currentDepth--
            if (hasNext) {
                outNextSibling[nodeOutPos] = outPos
            }
        }

        var idx = 0
        while (idx != -1) {
            traverse(idx, hasNext = nextSibling[idx] != -1, -1)
            idx = nextSibling[idx]
        }

        val struct = TreeStruct(
            outIds,
            outNextSibling,
            outParents,
            maxDepth
        )

        return TreeStructBuildingResult(struct, outMapping)
    }
}