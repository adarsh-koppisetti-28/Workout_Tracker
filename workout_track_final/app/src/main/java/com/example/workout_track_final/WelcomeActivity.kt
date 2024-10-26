package com.example.workout_track_final

import android.app.AlertDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class WelcomeActivity : AppCompatActivity() {

    private lateinit var exerciseAdapter: ExerciseAdapter
    private lateinit var exerciseList: MutableList<Exercise>
    private lateinit var totalCaloriesTextView: TextView
    private lateinit var exerciseRecyclerView: RecyclerView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_welcome)

        val browseButton = findViewById<Button>(R.id.browseButton)
        val takePicButton = findViewById<Button>(R.id.takePicButton)
        val addExerciseButton = findViewById<Button>(R.id.addExerciseButton)
        totalCaloriesTextView = findViewById(R.id.totalCaloriesTextView)
        exerciseRecyclerView = findViewById(R.id.exerciseRecyclerView)

        exerciseList = mutableListOf()
        exerciseAdapter = ExerciseAdapter(exerciseList)
        exerciseRecyclerView.layoutManager = LinearLayoutManager(this)
        exerciseRecyclerView.adapter = exerciseAdapter

        browseButton.setOnClickListener {
            val intent = Intent(Intent.ACTION_GET_CONTENT)
            intent.type = "video/*"
            startActivityForResult(intent, REQUEST_VIDEO_PICK)
        }

        takePicButton.setOnClickListener {
            val intent = Intent(this, CameraModel::class.java)
            startActivity(intent)
        }

        addExerciseButton.setOnClickListener {
            showAddExerciseDialog()
        }
    }

    private fun showAddExerciseDialog() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_exercise, null)
        val exerciseNameInput = dialogView.findViewById<EditText>(R.id.exerciseNameInput)
        val repsInput = dialogView.findViewById<EditText>(R.id.repsInput)

        AlertDialog.Builder(this)
            .setTitle("Add Exercise")
            .setView(dialogView)
            .setPositiveButton("Add") { _, _ ->
                val name = exerciseNameInput.text.toString()
                val reps = repsInput.text.toString().toIntOrNull() ?: 0
                if (name.isNotBlank() && reps > 0) {
                    val caloriesPerRep = 5 // Adjust this value based on your exercise
                    val exercise = Exercise(name, reps, caloriesPerRep)
                    exerciseList.add(exercise)
                    exerciseAdapter.notifyItemInserted(exerciseList.size - 1)
                    updateCalories()
                } else {
                    Toast.makeText(this, "Please enter valid exercise and reps", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun updateCalories() {
        val totalCalories = exerciseList.sumOf { it.totalCalories() }
        totalCaloriesTextView.text = "Total Calories: $totalCalories"
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == RESULT_OK) {
            val videoUri = data?.data
            if (videoUri != null) {
                // Launch VideoFrameProcessor for inference
                val intent = Intent(this, VideoFrameProcessor::class.java)
                intent.putExtra("videoUri", videoUri.toString())
                startActivity(intent)
            } else {
                Toast.makeText(this, "Failed to load video", Toast.LENGTH_SHORT).show()
            }
        }
    }

    companion object {
        const val REQUEST_VIDEO_CAPTURE = 1
        const val REQUEST_VIDEO_PICK = 2
    }
}
