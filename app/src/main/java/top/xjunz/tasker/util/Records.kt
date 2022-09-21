package top.xjunz.tasker.util

import android.graphics.Rect
import top.xjunz.tasker.model.Record
import top.xjunz.tasker.model.Result
import java.io.FileDescriptor
import java.io.FileInputStream

/**
 * A utility class for parsing records from a [FileDescriptor] and providing some interfaces to
 * manipulate the records.
 *
 * @author xjunz 2021/8/9
 */
class Records(private val fd: FileDescriptor) : Iterable<Record> {

    private val recordSet by lazy { mutableSetOf<Record>() }

    class ParseException constructor(lineNum: Int, line: String?, reason: String) : Exception("$reason at line $lineNum: $line")

    /**
     * @see Record.toString
     */
    @Throws(ParseException::class)
    fun parse(): Records {
        if (!fd.valid()) {
            throw ParseException(0, null, "invalid file descriptor")
        }
        FileInputStream(fd).bufferedReader().useLines { lines ->
            lines.forEachIndexed { lineNum, line ->
                if (line.isNotBlank()) {
                    val components = line.split(';')
                    if (components.size != Record.COMPONENT_COUNT) {
                        throw ParseException(
                            lineNum + 1, line, "illegal component count: ${components.size}," +
                                    " require ${Record.COMPONENT_COUNT}"
                        )
                    }
                    recordSet.add(findRecord(components[0]) ?: Record(components[0]).apply {
                        count = components[1].toIntOrNull() ?: throw ParseException(
                            lineNum + 1, line,
                            "mal-formatted count: ${components[1]}"
                        )
                        text = if (components[2] == "null") null else components[2].replace('\\', '\n')
                        Rect.unflattenFromString(components[3])?.let {
                            portraitBounds = it
                        }
                        Rect.unflattenFromString(components[4])?.let {
                            landscapeBounds = it
                        }
                        firstTimestamp = components[5].toLongOrNull() ?: throw ParseException(
                            lineNum + 1,
                            line, "mal-formatted first timestamp: ${components[5]}"
                        )
                        latestTimestamp = components[6].toLongOrNull() ?: throw ParseException(
                            lineNum + 1,
                            line, "mal-formatted latest timestamp: ${components[6]}"
                        )
                    })
                }
            }
        }
        return this
    }

    private fun findRecord(pkgName: String?) = recordSet.find {
        it.pkgName == pkgName
    }

    fun putResult(result: Result) {
        check(result.passed) { "only passed results can be put into records" }
        var record = findRecord(result.pkgName)
        val found = record != null
        if (!found) {
            record = Record(result.pkgName!!)
            recordSet.add(record)
        }
        record?.apply {
            val timestamp = System.currentTimeMillis()
            if (firstTimestamp <= 0) {
                firstTimestamp = timestamp
            }
            latestTimestamp = timestamp
            count += 1
            text = result.text
            if (result.bounds != null && result.portrait != null) {
                if (result.portrait!!) {
                    portraitBounds = result.bounds
                } else {
                    landscapeBounds = result.bounds
                }
            }
        }
    }

    fun isEmpty() = recordSet.isEmpty()

    override fun iterator(): Iterator<Record> = recordSet.iterator()

    fun asList() = ArrayList(recordSet)

    fun getRecordCount(): Int {
        var count = 0
        forEach {
            count += it.count
        }
        return count
    }
}