
package com.reboot.rebootearth

import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : AppCompatActivity() {

    private lateinit var imagePreview: ImageView
    private lateinit var btnClassify: Button
    private lateinit var txtResult: TextView
    private lateinit var txtConfidence: TextView
    private lateinit var txtGuidance: TextView

    private var selectedImageUri: Uri? = null

    private val imagePicker =
        registerForActivityResult(
            ActivityResultContracts.PickVisualMedia()
        ) { uri ->
            if (uri != null) {
                selectedImageUri = uri
                imagePreview.setImageURI(uri)
                btnClassify.isEnabled = true

                txtResult.text = "Image selected"
                txtConfidence.text = ""
                txtGuidance.text =
                    "Tap Identify Waste to analyse this image."
            } else {
                Toast.makeText(
                    this,
                    "No image selected",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        ViewCompat.setOnApplyWindowInsetsListener(
            findViewById(R.id.main)
        ) { view, insets ->
            val systemBars =
                insets.getInsets(WindowInsetsCompat.Type.systemBars())

            view.setPadding(
                systemBars.left,
                systemBars.top,
                systemBars.right,
                systemBars.bottom
            )
            insets
        }

        imagePreview = findViewById(R.id.imagePreview)
        btnClassify = findViewById(R.id.btnClassify)
        txtResult = findViewById(R.id.txtResult)
        txtConfidence = findViewById(R.id.txtConfidence)
        txtGuidance = findViewById(R.id.txtGuidance)

        val btnChooseImage =
            findViewById<Button>(R.id.btnChooseImage)

        btnClassify.isEnabled = false

        btnChooseImage.setOnClickListener {
            imagePicker.launch(
                PickVisualMediaRequest(
                    ActivityResultContracts.PickVisualMedia.ImageOnly
                )
            )
        }

        btnClassify.setOnClickListener {
            classifyWaste()
        }
    }

    private fun classifyWaste() {
        val uri = selectedImageUri ?: return

        lifecycleScope.launch {
            btnClassify.isEnabled = false
            txtResult.text = "Analyzing image..."
            txtConfidence.text = ""
            txtGuidance.text = "Please wait."

            try {
                val result = withContext(Dispatchers.IO) {
                    WasteClassifier(this@MainActivity).use { classifier ->
                        classifier.classify(uri)
                    }
                }

                Log.d(
                    "EcoVision",
                    "Prediction: ${result.label}, " +
                            "top score: ${result.confidence}"
                )

                val confidence =
                    (result.confidence * 100).coerceIn(0f, 100f)

                if (result.confidence < 0.50f) {
                    txtResult.text = "Uncertain prediction"
                    txtConfidence.text =
                        "Top score: ${"%.1f".format(confidence)}%"
                    txtGuidance.text =
                        "The model could not confidently identify this item. " +
                                "Try a clearer photo with one waste item centered. " +
                                "Verify the material before disposal."
                } else {
                    txtResult.text = "Predicted: ${result.label}"
                    txtConfidence.text =
                        "Model score: ${"%.1f".format(confidence)}%"
                    txtGuidance.text =
                        getDisposalGuidance(result.label)
                }

            } catch (e: Exception) {
                Log.e("EcoVision", "Classification failed", e)

                txtResult.text = "Classification unavailable"
                txtConfidence.text = ""
                txtGuidance.text =
                    e.localizedMessage ?: "Check the model and try again."
            } finally {
                btnClassify.isEnabled = selectedImageUri != null
            }
        }
    }

    private fun getDisposalGuidance(label: String): String {
        return when (label.lowercase()) {
            "plastic" ->
                "Empty and rinse the item. Check local recycling rules."

            "paper" ->
                "Keep paper clean and dry. Avoid recycling greasy paper."

            "cardboard" ->
                "Flatten the box and keep it dry before sorting."

            "metal" ->
                "Empty the container and check local metal recycling rules."

            "glass" ->
                "Handle carefully. Check local glass collection rules."

            "trash" ->
                "Check whether the item can be reused or separated " +
                        "before placing it in residual waste."

            else ->
                "Verify the material and local disposal instructions."
        }
    }
}
