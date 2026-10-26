
package com.reboot.rebootearth

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.net.Uri
import android.util.Log
import org.tensorflow.lite.DataType
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
        "Hazardous",
        "Non-Recyclable",
        "Organic",
        "Recyclable"
    )


    private val interpreter: Interpreter

    init {
        val modelBytes = appContext.assets
            .open("waste_classifier.tflite")
            .use { it.readBytes() }

        val modelBuffer = ByteBuffer
            .allocateDirect(modelBytes.size)
            .order(ByteOrder.nativeOrder())

        modelBuffer.put(modelBytes)
        modelBuffer.rewind()

        interpreter = Interpreter(modelBuffer)

        val inputTensor = interpreter.getInputTensor(0)
        val outputTensor = interpreter.getOutputTensor(0)

        Log.d(
            "EcoVision",
            "Model input: ${inputTensor.shape().contentToString()}, " +
                    "type=${inputTensor.dataType()}, bytes=${inputTensor.numBytes()}"
        )

        Log.d(
            "EcoVision",
            "Model output: ${outputTensor.shape().contentToString()}, " +
                    "type=${outputTensor.dataType()}"
        )
    }

    fun classify(uri: Uri): WasteResult {
        val originalBitmap = appContext.contentResolver
            .openInputStream(uri)
            .use { stream ->
                requireNotNull(stream) { "Could not open selected image" }
                BitmapFactory.decodeStream(stream)
            } ?: error("Could not decode selected image")

        try {
            val inputTensor = interpreter.getInputTensor(0)
            val shape = inputTensor.shape()

            require(
                shape.size == 4 &&
                        shape[0] == 1 &&
                        shape[3] == 3
            ) {
                "Unsupported input shape: ${shape.contentToString()}"
            }

            require(inputTensor.dataType() == DataType.FLOAT32) {
                "Model input is not FLOAT32"
            }

            val height = shape[1]
            val width = shape[2]

            val bitmap = Bitmap.createScaledBitmap(
                originalBitmap,
                width,
                height,
                true
            )

            try {
                Log.d(
                    "EcoVision",
                    "Bitmap dimensions: ${bitmap.width}x${bitmap.height}"
                )

                val input = ByteBuffer
                    .allocateDirect(inputTensor.numBytes())
                    .order(ByteOrder.nativeOrder())

                for (y in 0 until height) {
                    for (x in 0 until width) {
                        val pixel = bitmap.getPixel(x, y)

                        input.putFloat(Color.red(pixel).toFloat())
                        input.putFloat(Color.green(pixel).toFloat())
                        input.putFloat(Color.blue(pixel).toFloat())
                    }
                }

                // Log a sample of the normalized input values.
                input.rewind()
                val firstValues = FloatArray(12)

                for (i in firstValues.indices) {
                    firstValues[i] = input.getFloat()
                }

                Log.d(
                    "EcoVision",
                    "First 12 RGB values: ${firstValues.contentToString()}"
                )

                input.rewind()

                val outputShape = interpreter.getOutputTensor(0).shape()
                val outputCount = outputShape.last()

                require(outputCount == labels.size) {
                    "Expected ${labels.size} classes but model outputs " +
                            "$outputCount. Output shape: " +
                            outputShape.contentToString()
                }

                val output = Array(1) { FloatArray(outputCount) }
                interpreter.run(input, output)

                val scores = output[0]

                Log.d(
                    "EcoVision",
                    "Scores: ${scores.contentToString()}"
                )

                val bestIndex = scores.indices.maxByOrNull {
                    scores[it]
                } ?: error("Model returned no predictions")

                return WasteResult(
                    label = labels[bestIndex],
                    confidence = scores[bestIndex]
                )
            } finally {
                if (bitmap !== originalBitmap) {
                    bitmap.recycle()
                }
            }
        } finally {
            originalBitmap.recycle()
        }
    }

    override fun close() {
        interpreter.close()
    }
}
