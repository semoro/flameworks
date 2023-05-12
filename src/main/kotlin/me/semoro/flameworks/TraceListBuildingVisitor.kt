package me.semoro.flameworks

import org.jetbrains.kotlinx.dataframe.DataColumn
import org.jetbrains.kotlinx.dataframe.DataFrame
import org.jetbrains.kotlinx.dataframe.api.dataFrameOf
import java.util.*


fun TraceTree.presentIdCount(): Int {
    val presentIds = BitSet()
    var count = 0
    traverse {
        val id = ids[it.idx]
        if (!presentIds.get(id)) {
            presentIds[id] = true
            count++
        }
    }
    return count
}

inline fun TraceTree.reducePerId(
    values: IntArray,
    reduce: (Int, Int) -> Int
): IntArray {
    val result = IntArray(nameTable.size)
    traverse {
        val id = ids[it.idx]
        if (id == -1) return@traverse
        result[id] = reduce(result[id], values[it.idx])
    }
    return result
}

fun TraceTree.selectTopNodePerId(): NodePredicate {
    val selector = SelectTopIdOccurrenceVisitor(ids)
    traverseDfs(selector)
    return NodePredicate(selector.selected)
}

class NodePredicate(private val selected: BitSet) {
    fun matches(idx: Int): Boolean {
        return selected[idx]
    }
}

class SelectTopIdOccurrenceVisitor(
    val ids: IntArray
): NodeVisitor() {
    val enteredIds = BitSet()
    val selected = BitSet(ids.size)
    override fun enter(idx: Int) {
        val id = ids[idx]
        if (id == -1) return

        if (enteredIds[id]) return // we already inside method
        enteredIds.set(id)
        selected.set(idx) // select it
    }

    override fun exit(idx: Int) {
        val id = ids[idx]
        if (id == -1) return
        enteredIds.clear(id)
    }
}



fun TraceTree.traceListDataFrame(
    build: TraceListDsl.() -> Unit
): DataFrame<*> {
    val ids = IntArray(this.nameTable.size) { it }

    val id = DataColumn.createValueColumn<Int>("id", ids.toList())
    val name = DataColumn.createValueColumn<String>("name", ids.map { nameTable[it] })
    val ownSamples = DataColumn.createValueColumn<Int>("ownSamples", reducePerId(ownSamples) { a, b -> a + b }.toList())

    val dsl = TraceListDsl(this)
    dsl.build()

    return dataFrameOf(id, name, ownSamples, *dsl.columns.toTypedArray())
}

class TraceListDsl(val tree: TraceTree) {
    val columns = mutableListOf<DataColumn<Int>>()
    val topSelection = tree.selectTopNodePerId()

    fun withColumn(name: String, perNodeValue: IntArray, reduce: (Int, Int) -> Int) {
        columns += DataColumn.createValueColumn<Int>(name, tree.reducePerId(perNodeValue, reduce).toList())
    }

    fun withAggregatedColumn(
        name: String,
        perNodeValue: IntArray,
        reduce: (Int, Int) -> Int
    ) {
        val trimmed = tree.map {
            if (topSelection.matches(it.idx)) {
                perNodeValue[it.idx]
            } else {
                0
            }
        }
        columns += DataColumn.createValueColumn<Int>(name, tree.reducePerId(trimmed, reduce).toList())
    }
}

