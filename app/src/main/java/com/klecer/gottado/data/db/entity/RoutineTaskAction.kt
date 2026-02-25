package com.klecer.gottado.data.db.entity

/**
 * Action to apply to tasks when a routine runs (incomplete or completed tasks).
 */
enum class RoutineTaskAction {
    DELETE,
    MOVE,
    COMPLETE,
    UNCOMPLETE
}
