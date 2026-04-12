package ru.offerfactory.promodisplay.logger

import android.content.Context
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import timber.log.Timber
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStreamWriter
import java.io.RandomAccessFile
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class FileLoggingTree(
    context: Context,
    private val minPriority: Int = Log.WARN,
    private val maxTotalBytes: Long = 50L * 1024 * 1024,
    private val keepRatio: Double = 0.8
) : Timber.Tree() {

    private val appContext = context.applicationContext
    private val logDir: File =
        File(appContext.filesDir, Constants.DIRECTORY_NAME).apply { mkdirs() }
    private val file = File(logDir, "${Constants.FILE_NAME}.log")

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val mutex = Mutex()

    private val timeFormatter = SimpleDateFormat(
        Constants.TIME_FORMATTER_PATTERN,
        Locale.getDefault()
    )

    override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
        if (priority < minPriority) return

        val level = priorityToShort(priority)
        val safeTag = tag ?: "NoTag"

        scope.launch {
            mutex.withLock {
                val line = buildString {
                    append(timeFormatter.format(Date()))
                    append(" ")
                    append(level)
                    append(" ")
                    append(safeTag)
                    append(": ")
                    append(message)
                    t?.let {
                        append("\n")
                        append(t.stackTraceToString())
                    }
                    append("\n")
                }

                try {
                    appendLineToFile(file, line)
                    if (file.length() > maxTotalBytes) {
                        trimToLimit(file)
                    }
                } catch (_: Throwable) {
                    // Не через Timber а то будет рекурсия
                }
            }
        }
    }

    private fun trimToLimit(file: File) {
        val keepBytes = (maxTotalBytes * keepRatio).toLong().coerceAtLeast(1024 * 1024)
        RandomAccessFile(file, Constants.FILE_RIGHTS_MODE).use { raf ->
            val len = raf.length()
            if (len <= maxTotalBytes) return

            val start = (len - keepBytes).coerceAtLeast(0L)
            raf.seek(start)

            val tail = ByteArray((len - start).toInt())
            raf.readFully(tail)

            val cutFrom = run {
                val nl = tail.indexOf('\n'.code.toByte())
                if (nl >= 0 && nl + 1 < tail.size) nl + 1 else 0
            }

            raf.setLength(0)
            raf.seek(0)
            raf.write(tail, cutFrom, tail.size - cutFrom)
        }
    }

    private fun appendLineToFile(file: File, line: String) {
        FileOutputStream(file, true).use { fos ->
            OutputStreamWriter(fos, Charsets.UTF_8).use { w ->
                w.append(line)
            }
        }
    }

    private fun priorityToShort(priority: Int): String {
        return when (priority) {
            Log.VERBOSE -> "V"
            Log.DEBUG -> "D"
            Log.INFO -> "I"
            Log.WARN -> "W"
            Log.ERROR -> "E"
            Log.ASSERT -> "A"
            else -> "?"
        }
    }
}