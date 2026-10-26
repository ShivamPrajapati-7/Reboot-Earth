package com.reboot.rebootearth

import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.progressindicator.LinearProgressIndicator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private lateinit var imagePreview: ImageView
    private lateinit var btnChooseImage: MaterialButton
    private lateinit var btnTakePhoto: MaterialButton
    private lateinit var btnClassify: MaterialButton

    private lateinit var cardResult: MaterialCardView
    private lateinit var txtCategoryBadge: TextView
    private lateinit var txtResult: TextView
    private lateinit var txtConfidence: TextView
    private lateinit var progressConfidence: LinearProgressIndicator
    private lateinit var txtGuidance: TextView
    private lateinit var cardRecyclingSteps: MaterialCardView
    private lateinit var txtRecyclingSteps: TextView

    private var selectedImageUri: Uri? = null
    private var cameraImageUri: Uri? = null

    private val confidenceThreshold = 0.50f

    private val cameraLauncher =
        registerForActivityResult(
            ActivityResultContracts.TakePicture()
        ) { success ->
            if (success) {
                val uri = cameraImageUri
                if (uri != null) {
                    onImageSelected(uri)
                }
            } else {
                Toast.makeText(
                    this,
                    "Photo capture cancelled",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

    private val imagePicker =
        registerForActivityResult(
            ActivityResultContracts.PickVisualMedia()
        ) { uri ->
            if (uri != null) {
                onImageSelected(uri)
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
            val systemBars = insets.getInsets(
                WindowInsetsCompat.Type.systemBars()
            )
            view.setPadding(
                systemBars.left,
                systemBars.top,
                systemBars.right,
                systemBars.bottom
            )
            insets
        }

        imagePreview = findViewById(R.id.imagePreview)
        btnChooseImage = findViewById(R.id.btnChooseImage)
        btnTakePhoto = findViewById(R.id.btnTakePhoto)
        btnClassify = findViewById(R.id.btnClassify)

        cardResult = findViewById(R.id.cardResult)
        txtCategoryBadge = findViewById(R.id.txtCategoryBadge)
        txtResult = findViewById(R.id.txtResult)
        txtConfidence = findViewById(R.id.txtConfidence)
        progressConfidence = findViewById(R.id.progressConfidence)
        txtGuidance = findViewById(R.id.txtGuidance)
        cardRecyclingSteps = findViewById(R.id.cardRecyclingSteps)
        txtRecyclingSteps = findViewById(R.id.txtRecyclingSteps)

        btnClassify.isEnabled = false

        btnChooseImage.setOnClickListener {
            imagePicker.launch(
                PickVisualMediaRequest(
                    ActivityResultContracts.PickVisualMedia.ImageOnly
                )
            )
        }

        btnTakePhoto.setOnClickListener {
            openCamera()
        }

        btnClassify.setOnClickListener {
            classifyWaste()
        }
    }

    private fun onImageSelected(uri: Uri) {
        selectedImageUri = uri
        imagePreview.imageTintList = null
        imagePreview.setImageURI(uri)

        btnClassify.isEnabled = true
    }

    private fun classifyWaste() {
        val uri = selectedImageUri ?: return

        lifecycleScope.launch {
            btnClassify.isEnabled = false
            txtResult.text = "Analyzing image..."
            txtConfidence.text = "--%"
            progressConfidence.progress = 0

            try {
                val result = withContext(Dispatchers.IO) {
                    WasteClassifier(this@MainActivity).use {
                        it.classify(uri)
                    }
                }

                Log.d(
                    "EcoVision",
                    "Prediction: ${result.label}, top score: ${result.confidence}"
                )

                showClassificationResult(
                    label = result.label,
                    confidence = result.confidence
                )

            } catch (e: Exception) {
                Log.e("EcoVision", "Classification failed", e)

                txtCategoryBadge.text = "ERROR"
                txtResult.text = "Classification Unavailable"
                txtConfidence.text = "0%"
                progressConfidence.progress = 0
                txtGuidance.text = e.localizedMessage ?: "Check the model and try again."
                txtRecyclingSteps.text =
                    "1. Clean or rinse item if needed.\n" +
                            "2. Verify local waste sorting guidelines.\n" +
                            "3. Place in designated sorting container."
                cardRecyclingSteps.visibility = View.VISIBLE
                cardResult.visibility = View.VISIBLE
            } finally {
                btnClassify.isEnabled = selectedImageUri != null
            }
        }
    }

    private fun showClassificationResult(
        label: String,
        confidence: Float
    ) {
        val safeConfidence = confidence.coerceIn(0f, 1f)
        val percentage = safeConfidence * 100f
        val progressInt = percentage.toInt()

        txtConfidence.text = String.format(
            Locale.getDefault(),
            "%.1f%%",
            percentage
        )
        progressConfidence.progress = progressInt

        if (safeConfidence < confidenceThreshold) {
            txtCategoryBadge.text = "UNCERTAIN PREDICTION"
            txtResult.text = "Uncertain Prediction"
            txtGuidance.text =
                "⚠️ The model could not confidently identify this item.\n\n" +
                        "Try another photo with one waste item centered, " +
                        "good lighting, and a simple background."
            txtRecyclingSteps.text =
                "1. Verify material visually before disposal.\n" +
                        "2. Check local sorting guidelines.\n" +
                        "3. Place in general waste if uncertain to avoid contamination."
            cardRecyclingSteps.visibility = View.VISIBLE
            cardResult.visibility = View.VISIBLE
            return
        }

        val details = getWasteDetails(label)
        txtCategoryBadge.text = details.categoryBadge
        txtResult.text = details.itemTitle
        txtGuidance.text = details.guidance
        txtRecyclingSteps.text = details.recyclingSteps

        cardRecyclingSteps.visibility = View.VISIBLE
        cardResult.visibility = View.VISIBLE
    }

    private data class WasteDetails(
        val categoryBadge: String,
        val itemTitle: String,
        val guidance: String,
        val recyclingSteps: String
    )

    private fun getWasteDetails(label: String): WasteDetails {
        val clean = label.lowercase().trim()

        return when {
            clean.contains("straw") -> WasteDetails(
                categoryBadge = "RESIDUAL WASTE",
                itemTitle = "Plastic Straws",
                guidance = "Place in general residual waste. Consider eco-friendly re-usable alternatives next time!",
                recyclingSteps = "1. Clean or rinse item if needed.\n2. Verify local waste sorting guidelines.\n3. Place in designated sorting container."
            )
            clean.contains("hazardous") -> WasteDetails(
                categoryBadge = "HAZARDOUS WASTE",
                itemTitle = formatTitle(label, "Hazardous Waste"),
                guidance = "Handle with care. Do not throw in general trash. Take to an authorized hazardous waste facility.",
                recyclingSteps = "1. Keep in a safe, sealed container if appropriate.\n2. Store away from heat and children.\n3. Take to an authorized hazardous-waste collection point."
            )
            clean.contains("non-recyclable") || clean.contains("residual") -> WasteDetails(
                categoryBadge = "RESIDUAL WASTE",
                itemTitle = formatTitle(label, "Non-Recyclable Waste"),
                guidance = "Place in general residual waste. Consider eco-friendly re-usable alternatives next time!",
                recyclingSteps = "1. Clean or rinse item if needed.\n2. Verify local waste sorting guidelines.\n3. Place in designated sorting container."
            )
            clean.contains("organic") || clean.contains("food") || clean.contains("compost") -> WasteDetails(
                categoryBadge = "ORGANIC WASTE",
                itemTitle = formatTitle(label, "Organic Waste"),
                guidance = "Place in compost or organic waste collection bin if available. Keep free from non-compostable packaging.",
                recyclingSteps = "1. Separate food scraps from plastic and other waste.\n2. Place suitable scraps in a compost bin.\n3. Send non-compostable organic waste to the appropriate facility."
            )
            clean.contains("recyclable") || clean.contains("cardboard") || clean.contains("paper") || clean.contains("bottle") || clean.contains("can") -> WasteDetails(
                categoryBadge = "RECYCLABLE WASTE",
                itemTitle = formatTitle(label, "Recyclable Material"),
                guidance = "Empty and rinse the container before disposal. Check local recycling rules to verify acceptance.",
                recyclingSteps = "1. Empty the container and remove leftover contents.\n2. Rinse and dry it when appropriate.\n3. Sort it according to your local recycling rules."
            )
            else -> WasteDetails(
                categoryBadge = label.uppercase() + " WASTE",
                itemTitle = formatTitle(label, "Waste Item"),
                guidance = "Check your local waste-management guidance for the correct disposal method.",
                recyclingSteps = "1. Clean or rinse item if needed.\n2. Verify local waste sorting guidelines.\n3. Place in designated sorting container."
            )
        }
    }

    private fun getRecyclingSteps(category: String): List<String> {
        return when (
            category.trim()
                .uppercase()
                .replace("_", " ")
                .replace(Regex("\\s+"), " ")
        ) {
            "HAZARDOUS", "HAZARDOUS WASTE" -> listOf(
                "Keep in a safe, sealed container if appropriate.",
                "Store away from heat and children.",
                "Take to an authorized hazardous-waste collection point."
            )

            "NON-RECYCLABLE", "NON RECYCLABLE", "NON-RECYCLABLE WASTE" -> listOf(
                "Check local waste guidelines.",
                "Keep separate from recyclable materials.",
                "Dispose of it using the appropriate local waste service."
            )

            "ORGANIC", "ORGANIC WASTE" -> listOf(
                "Separate food scraps from plastic and other waste.",
                "Place suitable scraps in a compost bin.",
                "Send unsuitable material to the appropriate waste facility."
            )

            "RECYCLABLE", "RECYCLABLE WASTE" -> listOf(
                "Empty the container and remove leftover contents.",
                "Rinse and dry it when appropriate.",
                "Sort it according to local recycling rules."
            )

            else -> listOf(
                "Check local waste-sorting guidelines.",
                "Keep the item separate until its category is confirmed."
            )
        }
    }

    private fun updateRecyclingSteps(category: String) {
        val steps = getRecyclingSteps(category)

        txtRecyclingSteps.text = steps.mapIndexed { index, step ->
            "${index + 1}. $step"
        }.joinToString("\n")
        cardRecyclingSteps.visibility = View.VISIBLE
    }

    private fun formatTitle(label: String, fallback: String): String {
        if (label.isBlank()) return fallback
        return label.replace("_", " ")
            .split(" ")
            .joinToString(" ") { word ->
                word.replaceFirstChar { it.uppercase() }
            }
    }

    private fun openCamera() {
        try {
            val imageFile = File.createTempFile(
                "ecovision_",
                ".jpg",
                cacheDir
            )

            val uri = FileProvider.getUriForFile(
                this,
                "$packageName.fileprovider",
                imageFile
            )

            cameraImageUri = uri
            cameraLauncher.launch(uri)

        } catch (e: Exception) {
            Log.e("EcoVision", "Unable to open camera", e)

            Toast.makeText(
                this,
                "Unable to open camera: ${e.localizedMessage}",
                Toast.LENGTH_LONG
            ).show()
        }
    }
}
