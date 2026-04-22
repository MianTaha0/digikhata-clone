package com.digikhata.domain.model

enum class AttendanceStatus(val key: String, val label: String, val fraction: Double) {
    PRESENT("PRESENT", "Present", 1.0),
    ABSENT("ABSENT", "Absent", 0.0),
    HALF_DAY("HALF_DAY", "Half-day", 0.5),
    LEAVE("LEAVE", "Paid leave", 1.0),
    WEEK_OFF("WEEK_OFF", "Week off", 1.0);

    companion object {
        fun fromKey(key: String): AttendanceStatus =
            values().firstOrNull { it.key == key } ?: ABSENT
    }
}
