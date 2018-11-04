package com.abplus.timestamper.models

import com.google.firebase.Timestamp
import java.util.*
import kotlin.collections.ArrayList

data class TimeStamp(
    val uid: String,
    val now: Timestamp = Timestamp.now()
) {
    val data: Map<String, Timestamp> get() = mapOf(uid to now)

    data class Grouped(
        val date: Calendar,
        val times: List<Calendar>
    ) {
        constructor(time: Calendar) : this(time, ArrayList())

        fun add(time: Calendar) {
            if (times is MutableList) {
                times.add(time)
            }
        }
    }

    companion object {

        fun grouping(stamps: List<TimeStamp>): List<Grouped> = ArrayList<Grouped>().also { list ->
            var last: Grouped? = null

            stamps.forEach {
                val time = Calendar.getInstance().apply {
                    timeInMillis = it.now.seconds * 1000 + it.now.nanoseconds / 1000000
                }
                val date = Calendar.getInstance().apply {
                    clear()
                    set(
                        time.get(Calendar.YEAR),
                        time.get(Calendar.MONTH),
                        time.get(Calendar.DAY_OF_MONTH)
                    )
                }
                if (last?.date?.timeInMillis != date.timeInMillis) {
                    last = Grouped(date).also { grouped ->
                        list.add(grouped)
                    }
                }
                last?.add(time)
            }
        }
    }
}
