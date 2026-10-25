package com.reboot.rebootearth

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Log
import org.tensorflow.lite.Interpreter
import java.io.Closeable
import java.nio.ByteBuffer
import java.nio.ByteOrder

data class WasteResult(
    val label: String,
    val confidence: Float
)

class WasteClassifier(context: Context) : Closeable {

    private val appContext = context.applicationContext

    private val labels = listOf(
        "cardboard",
        "glass",
        "metal",
        "paper",
        "plastic",
        "trash"
    )

    private val interpreter: Interpreter

    init {
        val modelBytes = appContext.assets
            .open("waste_classifier.tflite")
            .use { stream -> stream.readBytes() }

        val modelBuffer = ByteBuffer
            .allocateDirect(modelBytes.size)
            .order(ByteOrder.nativeOrder())

        modelBuffer.put(modelBytes)
        modelBuffer.rewind()



        interpreter = Interpreter(modelBuffer)
    }

    fun classify(uri: Uri): WasteResult {
        val originalBitmap = appContext.contentResolver
            .openInputStream(uri)
            ?.use { stream ->
                BitmapFactory.decodeStream(stream)
            } ?: error("Could not decode image")


        try {
            val bitmap = Bitmap.createScaledBitmap(
                originalBitmap, 124, 124, true
            )

            try {
                val input = ByteBuffer
                    .allocateDirect(1 * 124 * 124 * 3 * 4)
                    .order(ByteOrder.nativeOrder())

                for (y in 0 until 124) {
                    for (x in 0 until 124) {
                        val pixel = bitmap.getPixel(x, y)

                        input.putFloat(
                            android.graphics.Color.red(pixel) / 255.0f
                        )
                        input.putFloat(
                            android.graphics.Color.green(pixel) / 255.0f
                        )
                        input.putFloat(
                            android.graphics.Color.blue(pixel) / 255.0f
                        )
                    }
                }

                input.rewind()

                val output = Array(1) { FloatArray(6) }
                interpreter.run(input, output)

                Log.d(
                    "EcoVision",
                    "Scores: ${output[0].contentToString()}"
                )

                val scores = output[0]
                val bestIndex = scores.indices.maxByOrNull {
                    scores[it]
                } ?: error("No prediction returned")

                return WasteResult(
                    label = labels[bestIndex],
                    confidence = scores[bestIndex]
                )
            } finally {
                if (bitmap !== originalBitmap) bitmap.recycle()
            }
        } finally {
            originalBitmap.recycle()
        }
    }

    override fun close() {
        interpreter.close()
    }
}
