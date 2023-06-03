package me.semoro.flameworks

import me.semoro.flameworks.io.parseCollapsedLines
import java.io.File
import java.nio.file.Files


object FlameWorks {
    fun loadCollapsed(path: File): TraceTree {
        return parseCollapsedLines(Files.lines(path.toPath()))
    }
}