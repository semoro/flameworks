package me.semoro.flameworks.io

import me.semoro.flameworks.NameTable
import me.semoro.flameworks.SamplePathToCallTreeBuilder
import me.semoro.flameworks.TraceTree
import java.util.stream.Stream

fun parseCollapsedLines(lines: Stream<String>): TraceTree {
    val nameTable = NameTable()
    val callTreeBuilder = SamplePathToCallTreeBuilder()
    var totalSampleCount = 0
    lines.forEach { line ->
        val lastSpaceIndex = line.lastIndexOf(' ')
        var idx = 0
        var prevIdx = 0
        do  {
            if (line[idx++] == ';' || idx > lastSpaceIndex) {
                callTreeBuilder.enter(
                    nameTable.intern(line.substring(prevIdx, idx - 1))
                )
                prevIdx = idx
            }
        } while (idx <= lastSpaceIndex)

        val count = line.substring(lastSpaceIndex + 1).toInt()
        callTreeBuilder.finishTrace(count)
        totalSampleCount += count
    }
    println(callTreeBuilder.treeSize)
    println("Total samples: $totalSampleCount")

    return callTreeBuilder.build(nameTable)
}