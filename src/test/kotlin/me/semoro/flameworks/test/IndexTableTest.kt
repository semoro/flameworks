package me.semoro.flameworks.test

import me.semoro.flameworks.*
import me.semoro.flameworks.io.parseCollapsedLines
import org.jetbrains.kotlinx.dataframe.api.add
import org.jetbrains.kotlinx.dataframe.api.head
import org.jetbrains.kotlinx.dataframe.api.sortByDesc
import org.jetbrains.kotlinx.dataframe.io.toCsv
import org.junit.jupiter.api.Test
import java.io.File
import kotlin.random.Random
import kotlin.system.measureTimeMillis

class IndexTableTest {

    @Test
    fun testInserts() {

        val nameTable = NameTable()

        val names = generateSequence(0) { it + 1 }.take(16).map { it.toString() }
        val realMap = mutableMapOf<String, Int>()
        for (name in names.shuffled(Random(0))) {
            realMap[name] = nameTable.intern(name)
        }

        for (name in names.shuffled(Random(1))) {
            require(realMap[name] == nameTable.intern(name))
        }

    }

    @Test
    fun testTreeBuilder() {

        val tree = parseCollapsedLines(
            listOf("a;b;c 1", "a;b;d 1").stream()
        )

        val nameTable = tree.nameTable

        tree.traverseDfs(PrintTreeVisitor(nameTable, tree.ids, tree.ownSamples))

        run {
            val visitor = TestDfsVisitor(tree.ids)
            tree.traverseDfs(visitor)
            assert(visitor.ourStack.isEmpty()) { "Stack is not empty: ${visitor.ourStack} "}
        }

//        val mlb = TraceListBuildingVisitor(nameTable.size, tree.ids, tree.totalSamples().totalSamples, tree.ownSamples)
//            .also { tree.traverseDfs(it) }

        val df = tree.traceListDataFrame {
            val onlyTop = tree.selectTopNodePerId()
            val totalSamples = tree.totalSamples()
            val trimmedTotals = this.tree.map {
                if (onlyTop.matches(it.idx)) {
                    totalSamples.totalSamples[it.idx]
                } else {
                    0
                }
            }

            withColumn("totalSamples", trimmedTotals) { a, b -> a + b }
        }

        println(df.sortByDesc("totalSamples").head(10).toCsv())
    }
}


class TestDfsVisitor(val ids: IntArray) : NodeVisitor() {
    val ourStack = mutableListOf<Int>()
    override fun enter(idx: Int) {
        ourStack.add(ids[idx])
    }

    override fun exit(idx: Int) {
        val removed = ourStack.removeLast()
        val id = ids[idx]
        require(removed == id) {
            "Stack value is $removed, while $id given"
        }
    }
}
