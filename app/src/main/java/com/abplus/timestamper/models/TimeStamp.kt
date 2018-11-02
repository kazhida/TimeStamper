package com.abplus.timestamper.models

import com.google.firebase.Timestamp

data class TimeStamp(
    val uid: String,
    val now: Timestamp = Timestamp.now()
) {
    val data: Map<String, Timestamp> get() = mapOf(uid to now)
}