package me.semoro.flameworks

abstract class NodeVisitor() {
    abstract fun enter(idx: Int)
    abstract fun exit(idx: Int)
}