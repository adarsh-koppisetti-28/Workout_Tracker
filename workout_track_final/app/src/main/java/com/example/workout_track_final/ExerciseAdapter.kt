package com.example.workout_track_final

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class ExerciseAdapter(private val exercises: List<Exercise>) :
    RecyclerView.Adapter<ExerciseAdapter.ExerciseViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ExerciseViewHolder {
        // Ensure this references the correct layout name
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_exercise, parent, false)
        return ExerciseViewHolder(view)
    }

    override fun onBindViewHolder(holder: ExerciseViewHolder, position: Int) {
        val exercise = exercises[position]
        holder.bind(exercise)
    }

    override fun getItemCount(): Int {
        return exercises.size
    }

    inner class ExerciseViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val nameTextView: TextView = itemView.findViewById(R.id.exerciseNameTextView)
        private val repsTextView: TextView = itemView.findViewById(R.id.exerciseRepsTextView)
        private val caloriesTextView: TextView = itemView.findViewById(R.id.exerciseCaloriesTextView)

        fun bind(exercise: Exercise) {
            nameTextView.text = exercise.name
            repsTextView.text = "Reps: ${exercise.reps}"
            caloriesTextView.text = "Calories: ${exercise.totalCalories()}"
        }
    }
}
