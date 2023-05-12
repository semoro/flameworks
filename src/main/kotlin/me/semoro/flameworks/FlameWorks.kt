package me.semoro.flameworks

import me.semoro.flameworks.io.parseCollapsedLines
import java.io.File
import java.io.FileReader


object FlameWorks {
    fun loadCollapsed(path: File): TraceTree {
        return FileReader(path).useLines {
            parseCollapsedLines(it)
        }
    }
}