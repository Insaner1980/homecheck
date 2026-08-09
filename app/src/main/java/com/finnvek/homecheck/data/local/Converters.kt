package com.finnvek.homecheck.data.local

import androidx.room.TypeConverter
import com.finnvek.homecheck.data.local.entity.AttachmentType
import com.finnvek.homecheck.domain.RecurrenceUnit
import java.time.LocalDate

class Converters {
    @TypeConverter fun localDateToString(value: LocalDate?): String? = value?.toString()

    @TypeConverter fun stringToLocalDate(value: String?): LocalDate? = value?.let(LocalDate::parse)

    @TypeConverter fun attachmentTypeToString(value: AttachmentType): String = value.name

    @TypeConverter fun stringToAttachmentType(value: String): AttachmentType = AttachmentType.valueOf(value)

    @TypeConverter fun recurrenceUnitToString(value: RecurrenceUnit?): String? = value?.name

    @TypeConverter fun stringToRecurrenceUnit(value: String?): RecurrenceUnit? = value?.let(RecurrenceUnit::valueOf)
}
