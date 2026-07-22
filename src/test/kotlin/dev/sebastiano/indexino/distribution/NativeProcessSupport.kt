package dev.sebastiano.indexino.distribution

import java.util.concurrent.TimeUnit

internal fun terminateProcessTree(process: Process, timeout: Long, timeoutUnit: TimeUnit): Boolean {
    val root = process.toHandle()
    val handles =
        root
            .descendants()
            .use { descendants -> descendants.toList() }
            .plus(root)
            .distinctBy { it.pid() }
    val leafFirst = handles.sortedByDescending(::processDepth)
    val timeoutNanos = timeoutUnit.toNanos(timeout)
    val startedAt = System.nanoTime()
    val gracefulDeadline = startedAt + timeoutNanos / 2L
    val finalDeadline = startedAt + timeoutNanos

    leafFirst.filter(ProcessHandle::isAlive).forEach(ProcessHandle::destroy)
    awaitTermination(leafFirst, gracefulDeadline)
    leafFirst.filter(ProcessHandle::isAlive).forEach(ProcessHandle::destroyForcibly)
    return awaitTermination(leafFirst, finalDeadline)
}

private fun processDepth(handle: ProcessHandle): Int {
    var depth = 0
    var parent = handle.parent()
    while (parent.isPresent) {
        depth += 1
        parent = parent.get().parent()
    }
    return depth
}

private fun awaitTermination(handles: List<ProcessHandle>, deadlineNanos: Long): Boolean {
    while (handles.any(ProcessHandle::isAlive)) {
        val remainingNanos = deadlineNanos - System.nanoTime()
        if (remainingNanos <= 0L) return false
        Thread.sleep(minOf(TimeUnit.NANOSECONDS.toMillis(remainingNanos) + 1L, POLL_MILLIS))
    }
    return true
}

private const val POLL_MILLIS = 10L
