package me.semoro.flameworks

import me.semoro.flameworks.io.parseCollapsedLines
import java.io.File
import java.nio.file.Files
import java.nio.file.Path


@Suppress("unused")
object FlameWorks {
    fun loadCollapsed(path: File): TraceTree {
        return parseCollapsedLines(Files.lines(path.toPath()))
    }

    fun loadCollapsed(path: Path): TraceTree {
        return parseCollapsedLines(Files.lines(path))
    }

    fun mergeTraces(tt: List<TraceTree>): TraceTree {
        val nameTable = NameTable()
        val treeStructBuilder = TreeStructBuilder()
        var totalOwnSamples = IntArray(treeStructBuilder.capacity)
        for (trace in tt) {
            val idMap = IntArray(trace.nameTable.size)
            for (id in 0 until trace.nameTable.size) {
                idMap[id] = nameTable.intern(trace.nameTable.get(id))
            }
            val builderPosition = IntArray(trace.nodeCount) { -1 }
            trace.traverse {
                val pIdx = trace.parents[it.idx]
                if (pIdx == -1) {
                    treeStructBuilder.currentIdx = -1
                } else {
                    treeStructBuilder.currentIdx = builderPosition[pIdx]
                }
                val node = treeStructBuilder.findOrInsertChild(idMap[trace.ids[it.idx]])
                builderPosition[it.idx] = node.idx
                if (node.idx >= totalOwnSamples.size) {
                    totalOwnSamples = totalOwnSamples.copyOf(treeStructBuilder.capacity)
                }
                totalOwnSamples[node.idx] += trace.ownSamples[it.idx]
            }
        }

        val (struct, mapping) = treeStructBuilder.build()
        val outOwnSamples = IntArray(struct.nodeCount)
        for (m in mapping.indices) {
            outOwnSamples[mapping[m]] = totalOwnSamples[m]
        }
        return TraceTree(nameTable, struct, outOwnSamples)
    }
}