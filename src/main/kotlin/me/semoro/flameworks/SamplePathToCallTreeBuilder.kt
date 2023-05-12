package me.semoro.flameworks

import java.util.*

class SamplePathToCallTreeBuilder() {

    var capacity = 16

    var ids = IntArray(capacity) { -1 }
    var firstChild = IntArray(capacity) { -1 }
    var nextSibling = IntArray(capacity) { -1 }
    var values = IntArray(capacity) { 0 }

    var treeSize = 1

    fun ensureCapacity(idx: Int) {
        if (idx >= ids.size) {
            val prevCapacity = capacity
            capacity *= 2
            ids = ids.copyOf(capacity).also { it.fill(-1, prevCapacity) }
            firstChild = firstChild.copyOf(capacity).also { it.fill(-1, prevCapacity) }
            nextSibling = nextSibling.copyOf(capacity).also { it.fill(-1, prevCapacity) }
            values = values.copyOf(capacity)
        }
    }

    private fun insertPosition(): Int {
        val next = treeSize++
        ensureCapacity(next)
        return next
    }

    var currentNodeIndex = 0


    fun enter(id: Id) {
        val parentIdx = currentNodeIndex
        var idx = firstChild[currentNodeIndex]

        // parent has no children, proceed to insertion
        if (idx == -1) {
            currentNodeIndex = insertPosition()
            firstChild[parentIdx] = currentNodeIndex
            require(ids[currentNodeIndex] == -1)
            ids[currentNodeIndex] = id // store current id
//            println("Add first child at $currentNodeIndex")
            return // we had performed successful insertion
        }

        while (true) {
            if (ids[idx] == id) {
                currentNodeIndex = idx
                return // found an existing node
            }
            if (nextSibling[idx] == -1) {

                // we don't have nextSibling, need to insert it
                currentNodeIndex = insertPosition()
                nextSibling[idx] = currentNodeIndex
                require(ids[currentNodeIndex] == -1)
                ids[currentNodeIndex] = id
//                println("Add new children at $currentNodeIndex")
                return
            }
            idx = nextSibling[idx]
        }
    }
    fun finishTrace(samples: Int) {
        values[currentNodeIndex] += samples
        currentNodeIndex = 0 // reset to root
    }

    fun traverseDfs(visitor: NodeVisitor, idx: Int) {
        visitor.enter(idx)
        var childIdx = firstChild[idx]
        while (childIdx != -1) {
            traverseDfs(visitor, childIdx)
            childIdx = nextSibling[childIdx]
        }
        visitor.exit(idx)
    }

    fun build(nameTable: NameTable): TraceTree {
        val outIds = IntArray(treeSize)
        val outNextSibling = IntArray(treeSize) { -1 }
        val outSamples = IntArray(treeSize)
        val outParents = IntArray(treeSize)

        var outPos = 0

        var maxDepth = 0
        var currentDepth = 0

        fun traverse(idx: Int, hasNext: Boolean, parent: Int) {
            val nodeOutPos = outPos
            outIds[nodeOutPos] = ids[idx]
            outSamples[nodeOutPos] = values[idx]
            outParents[nodeOutPos] = parent
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

        traverse(0, hasNext = false, -1)

        return TraceTree(nameTable, outIds, outParents, outNextSibling, outSamples, maxDepth)
    }
}