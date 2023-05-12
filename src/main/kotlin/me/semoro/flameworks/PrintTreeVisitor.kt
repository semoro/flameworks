package me.semoro.flameworks

import java.io.PrintStream

class PrintTreeVisitor(
    val nameTable: NameTable,
    val ids: IntArray,
    val values: IntArray,
    val out: PrintStream = System.out
) : NodeVisitor() {
    var level = 0
    override fun enter(idx: Int) {
        val id = ids[idx]
        val value = values[idx]
        if (id == -1 && level == 0) return
        repeat(level) {
            print(" ")
        }
        println("enter ${nameTable[id]} -> $value")
        level++
    }
    override fun exit(idx: Int) {
        val id = ids[idx]
        if (id == -1 && level == 0) return
        level--
        repeat(level) {
            print(" ")
        }
        println("exit ${nameTable[id]}")
    }
}