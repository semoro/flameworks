package me.semoro.flameworks

class TraceTree(
    val nameTable: NameTable,
    val ids: IntArray,
    val parents: IntArray,
    val nextSibling: IntArray,
    val ownSamples: IntArray,
    val maxDepth: Int
) {
    val nodeCount = ids.size

    constructor(nameTable: NameTable, struct: TreeStruct, ownSamples: IntArray): this(
        nameTable,
        struct.ids,
        struct.parents,
        struct.nextSibling,
        ownSamples,
        struct.maxDepth
    )

    inline fun map(compute: (Node) -> Int): IntArray {
        val data = IntArray(ids.size)
        for (idx in ids.indices) {
            data[idx] = compute(Node(idx))
        }
        return data
    }

    inline fun traverse(action: (Node) -> Unit) {
        for (idx in ids.indices) {
            action(Node(idx))
        }
    }

    inline fun reverseTraverse(action: (Node) -> Unit) {
        for (idx in ids.lastIndex downTo 0) {
            action(Node(idx))
        }
    }

    fun traverseDfs(visitor: NodeVisitor) {
        val stackSize = maxDepth * 2 + 2
        val stack = IntArray(stackSize) // i used for childrenEnd, i + 1 used for idx
        var stackTop = stackSize
        stackTop -= 2
        stack[stackTop] = ids.size
        stack[stackTop + 1] = -1

        var idx = 0
        while (idx < ids.size) {
            visitor.enter(idx)
            val nextSiblingIdx = nextSibling[idx]
            val childrenEnd = if (nextSiblingIdx == -1) {
                stack[stackTop]
            } else {
                nextSiblingIdx
            }

            stackTop -= 2
            stack[stackTop] = childrenEnd
            stack[stackTop + 1] = idx

            while(idx >= stack[stackTop] - 1) {
                if (stackTop == stackSize - 2) break
                visitor.exit(stack[stackTop + 1])
                stackTop += 2
            }

            idx++
        }

    }
}