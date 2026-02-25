package com.klecer.gottado.data.db.converter

import androidx.room.TypeConverter
import com.klecer.gottado.data.db.entity.ReorderHandlePosition
import com.klecer.gottado.data.db.entity.RoutineFrequency
import com.klecer.gottado.data.db.entity.RoutineTaskAction
import com.klecer.gottado.data.db.entity.RoutineVisibilityMode

class AppConverters {

    @TypeConverter
    fun fromReorderHandlePosition(value: ReorderHandlePosition): String = value.name

    @TypeConverter
    fun toReorderHandlePosition(value: String): ReorderHandlePosition =
        ReorderHandlePosition.valueOf(value)

    @TypeConverter
    fun fromRoutineFrequency(value: RoutineFrequency): String = value.name

    @TypeConverter
    fun toRoutineFrequency(value: String): RoutineFrequency = RoutineFrequency.valueOf(value)

    @TypeConverter
    fun fromRoutineVisibilityMode(value: RoutineVisibilityMode): String = value.name

    @TypeConverter
    fun toRoutineVisibilityMode(value: String): RoutineVisibilityMode =
        RoutineVisibilityMode.valueOf(value)

    @TypeConverter
    fun fromRoutineTaskAction(value: RoutineTaskAction): String = value.name

    @TypeConverter
    fun toRoutineTaskAction(value: String): RoutineTaskAction = RoutineTaskAction.valueOf(value)
}
