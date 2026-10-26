
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
import java.util.Locale
import android.content.Intent
import android.os.Environment
import androidx.core.content.FileProvider
import java.io.File

class MainActivity : AppCompatActivity() {

    private lateinit var imagePreview: ImageView
    private lateinit var btnClassify: Button
    private lateinit var txtResult: TextView
    private lateinit var txtConfidence: TextView
    private lateinit var txtGuidance: TextView
    private lateinit var btnTakePhoto: Button

    private var selectedImageUri: Uri? = null


    // A configurable threshold, not a guarantee of correctness.
    private val confidenceThreshold = 0.50f

    private val cameraLauncher =
        registerForActivityResult(
            ActivityResultContracts.TakePicture()
        ) { success ->
            if (success) {
                val uri = cameraImageUri

                if (uri != null) {
                    selectedImageUri = uri
                    imagePreview.setImageURI(uri)

                    btnClassify.isEnabled = true
                    txtResult.text = "Photo captured"
                    txtConfidence.text = ""
                    txtGuidance.text =
                        "Tap Identify Waste to analyse this photo."
                }
            } else {
                Toast.makeText(
                    this,
                    "Photo capture cancelled",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

    private var cameraImageUri: Uri? = null

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
        btnTakePhoto = findViewById(R.id.btnTakePhoto)

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
        btnTakePhoto.setOnClickListener {
            openCamera()
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
                    WasteClassifier(this@MainActivity).use {
                        it.classify(uri)
                    }
                }

                Log.d(
                    "EcoVision",
                    "Prediction: ${result.label}, " +
                            "top score: ${result.confidence}"
                )

                showClassificationResult(
                    label = result.label,
                    confidence = result.confidence
                )

            } catch (e: Exception) {
                Log.e("EcoVision", "Classification failed", e)

                txtResult.text = "Classification unavailable"
                txtConfidence.text = ""
                txtGuidance.text =
                    e.localizedMessage
                        ?: "Check the model and try again."

            } finally {
                btnClassify.isEnabled = selectedImageUri != null
            }
        }
    }

    private fun showClassificationResult(
        label: String,
        confidence: Float
    ) {
        // WasteClassifier must return confidence in the 0.0–1.0 range.
        val safeConfidence = confidence.coerceIn(0f, 1f)
        val percentage = safeConfidence * 100f

        val displayLabel = label
            .replace("_", " ")
            .split(" ")
            .joinToString(" ") { word ->
                word.replaceFirstChar { it.uppercase() }
            }

        txtConfidence.text = String.format(
            Locale.getDefault(),
            "Model score: %.1f%%",
            percentage
        )

        if (safeConfidence < confidenceThreshold) {
            txtResult.text = "Uncertain prediction"

            txtGuidance.text =
                "⚠️ The model could not confidently identify this item.\n\n" +
                        "Try another photo with one waste item centered, " +
                        "good lighting, and a simple background.\n\n" +
                        "Verify the material before disposal."

            return
        }

        txtResult.text = "Predicted: $displayLabel"
        txtGuidance.text = getDisposalGuidance(label)
    }

    private fun getDisposalGuidance(label: String): String {
        return when (label.lowercase()) {

            "cardboard_boxes",
            "cardboard_packaging" ->
                "Flatten the cardboard. Keep it dry and free from food " +
                        "residue. Recycle it if accepted locally."

            "magazines",
            "newspaper",
            "office_paper" ->
                "Keep the paper clean and dry. Recycle it through your " +
                        "local paper collection service."

            "paper_cups" ->
                "Check whether your local facility accepts paper cups. " +
                        "Some cups have plastic linings."

            "plastic_water_bottles",
            "plastic_soda_bottles",
            "plastic_detergent_bottles",
            "plastic_food_containers" ->
                "Empty and rinse the container. Check local recycling " +
                        "rules and separate the cap if required."

            "plastic_cup_lids",
            "plastic_straws",
            "disposable_plastic_cutlery" ->
                "Check local rules before recycling. Small plastic items " +
                        "are often not accepted in standard recycling bins."

            "plastic_shopping_bags",
            "plastic_trash_bags" ->
                "Do not put plastic bags in ordinary recycling bins " +
                        "unless accepted. Look for a designated collection point."

            "aluminum_soda_cans",
            "aluminum_food_cans",
            "steel_food_cans" ->
                "Empty and rinse the can. Place it in metal recycling " +
                        "if accepted locally."

            "glass_beverage_bottles",
            "glass_food_jars",
            "glass_cosmetic_containers" ->
                "Empty the container and check local glass recycling " +
                        "rules. Handle broken glass carefully."

            "food_waste",
            "coffee_grounds",
            "eggshells",
            "tea_bags" ->
                "Use a compost collection service or compost bin if " +
                        "available. Check whether tea bags are compostable."

            "clothing",
            "shoes" ->
                "Donate usable items or use a textile collection point. " +
                        "Follow local textile-waste rules for damaged items."

            "aerosol_cans" ->
                "Do not puncture or burn the can. Follow local disposal " +
                        "instructions; partially full cans may need special handling."

            "styrofoam_cups",
            "styrofoam_food_containers" ->
                "Check for a specialist foam collection service. These " +
                        "items are not accepted in many recycling programs."

            else ->
                "Check your local waste-management guidance for the " +
                        "correct disposal method. If unsure, ask your campus " +
                        "or municipal waste collection service."
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
