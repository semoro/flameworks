package me.semoro.flameworks

import java.util.BitSet

@JvmInline
value class Node(val idx: Int)

@JvmInline
value class OwnSamples(val ownSamples: IntArray)

@JvmInline
value class FrameIdArray(val ids: IntArray)

@JvmInline
value class ColoredSamples(val coloredSamples: IntArray)

fun TraceTree.coloredSamples(predicate: FrameIdPredicate): ColoredSamples {
    val coloredSamples = IntArray(nodeCount)
    with(FrameIdArray(ids)) {
        with(OwnSamples(ownSamples)) {
            reverseTraverse {
                val matches = predicate.matches(it)
                val ownColoredSamples = if (matches) {
                    it.ownSamples()
                } else {
                    0
                }
                coloredSamples[it.idx] += ownColoredSamples
                if (it.hasParent() && matches) {
                    coloredSamples[it.parent().idx] += coloredSamples[it.idx]
                }
            }
        }
    }
    return ColoredSamples(coloredSamples)
}

context(OwnSamples)
fun Node.ownSamples(): Int = ownSamples[idx]

context(TraceTree)
fun Node.parent() = Node(parents[idx])

context(TraceTree)
fun Node.hasParent() = parents[idx] != -1

context(FrameIdArray)
fun FrameIdPredicate.matches(node: Node): Boolean {
    return matches(ids[node.idx])
}

class FrameIdPredicate(
    private val idSet: BitSet
) {
    fun matches(id: Int): Boolean {
        if (id == -1) return false
        return idSet[id]
    }
}

fun NameTable.namePredicate(match: (String) -> Boolean): FrameIdPredicate {
    val idSet = BitSet()
    for (id in 0 until size) {
        idSet[id] = match(get(id))
    }
    return FrameIdPredicate(idSet)
}