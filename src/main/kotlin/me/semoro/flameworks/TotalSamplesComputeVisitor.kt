package me.semoro.flameworks

@JvmInline
value class TotalSamples(val totalSamples: IntArray)

fun TraceTree.totalSamples(): TotalSamples {
    val totalSamples = IntArray(nodeCount)
    with(OwnSamples(ownSamples)) {
        reverseTraverse {
            val ownSamples = it.ownSamples()
            totalSamples[it.idx] += ownSamples
            if (it.hasParent()) {
                totalSamples[it.parent().idx] += totalSamples[it.idx]
            }
        }
    }
    return TotalSamples(totalSamples)
}

