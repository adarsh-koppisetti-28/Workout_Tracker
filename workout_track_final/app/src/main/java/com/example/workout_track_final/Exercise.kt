package com.example.workout_track_final

data class Exercise(
    val name: String,
    val reps: Int,
    val caloriesPerRep: Int
) {
    fun totalCalories(): Int {
        return reps * caloriesPerRep
    }
}
