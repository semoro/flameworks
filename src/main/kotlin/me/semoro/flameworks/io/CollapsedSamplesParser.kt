package me.semoro.flameworks.io

import me.semoro.flameworks.NameTable
import me.semoro.flameworks.Node
import me.semoro.flameworks.TreeStructBuilder
import me.semoro.flameworks.TraceTree
import java.util.stream.Stream

fun parseCollapsedLines(lines: Stream<String>): TraceTree {
    val nameTable = NameTable()
    val callTreeBuilder = TreeStructBuilder()

    var tmpOwnSamples = IntArray(callTreeBuilder.capacity)
    var totalSampleCount = 0
    lines.forEach { line ->
        val lastSpaceIndex = line.lastIndexOf(' ')
        var node = Node(-1)
        var idx = 0
        var prevIdx = 0
        do  {
            if (line[idx++] == ';' || idx > lastSpaceIndex) {
                node = callTreeBuilder.findOrInsertChild(
                    nameTable.intern(line.substring(prevIdx, idx - 1))
                )
                prevIdx = idx
            }
        } while (idx <= lastSpaceIndex)

        val count = line.substring(lastSpaceIndex + 1).toInt()
        if (node.idx >= tmpOwnSamples.size) {
            tmpOwnSamples = tmpOwnSamples.copyOf(callTreeBuilder.capacity)
        }
        tmpOwnSamples[node.idx] += count
        callTreeBuilder.rewind()
        totalSampleCount += count
    }
    println(callTreeBuilder.treeSize)
    println("Total samples: $totalSampleCount")

    val (struct, mapping) = callTreeBuilder.build()
    val outOwnSamples = IntArray(struct.nodeCount)
    for (idx in mapping.indices) {
        outOwnSamples[mapping[idx]] = tmpOwnSamples[idx]
    }
    return TraceTree(nameTable, struct, outOwnSamples)
}