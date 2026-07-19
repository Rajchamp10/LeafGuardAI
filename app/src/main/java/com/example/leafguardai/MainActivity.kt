package com.example.leafguardai

import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.cardview.widget.CardView
import androidx.constraintlayout.widget.ConstraintLayout
import org.tensorflow.lite.Interpreter
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.channels.FileChannel
import kotlin.math.exp

class MainActivity : BaseActivity() {

    private lateinit var uploadContainer: ConstraintLayout
    private lateinit var imgPreview: ImageView
    private lateinit var placeholderView: LinearLayout
    private lateinit var btnPredict: Button
    private lateinit var resultCard: CardView
    private lateinit var txtResultHeading: TextView
    private lateinit var txtConfidence: TextView

    private var selectedBitmap: Bitmap? = null
    private var tflite: Interpreter? = null

    private val REJECTION_THRESHOLD = 79.6

    private val diseaseMap = mapOf(
        0 to "Apple scab", 1 to "Apple rot", 2 to "Cedar apple rust",
        3 to "Apple healthy", 4 to "Blueberry healthy", 5 to "Cherry healthy",
        6 to "Cherry Powdery mildew", 7 to "Corn Northern Leaf Blight",
        8 to "Grape Leaf blight", 9 to "Grape healthy", 10 to "Orange Citrus greening",
        11 to "Peach Bacterial spot", 12 to "Peach healthy",
        13 to "Pepper bell Bacterial spot", 14 to "Potato Early blight",
        15 to "Potato healthy", 16 to "Rasberry healthy",
        17 to "Soybean healthy", 18 to "Squash Powdery mildew",
        19 to "Tomato Bacterial spot", 20 to "Tomato Late blight",
        21 to "Tomato Septoria leaf spot", 22 to "Tomato healthy",
        23 to "Grape Black Measles", 24 to "Pepper bell healthy"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        uploadContainer = findViewById(R.id.uploadContainer)
        imgPreview = findViewById(R.id.imgPreview)
        placeholderView = findViewById(R.id.placeholderView)
        btnPredict = findViewById(R.id.btnPredict)
        resultCard = findViewById(R.id.resultCard)
        txtResultHeading = findViewById(R.id.txtResultHeading)
        txtConfidence = findViewById(R.id.txtConfidence)

        try {
            tflite = Interpreter(loadModelFile())
            Log.d("TFLite", "Offline Model Loaded Successfully")
        } catch (e: Exception) {
            Log.e("TFLite", "Error loading model", e)
            Toast.makeText(this, "Failed to load offline model.", Toast.LENGTH_LONG).show()
        }

        val galleryLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
            uri?.let {
                selectedBitmap = uriToBitmap(it)
                imgPreview.setImageBitmap(selectedBitmap)
                placeholderView.visibility = View.GONE
                imgPreview.visibility = View.VISIBLE
                resultCard.visibility = View.GONE
            }
        }

        uploadContainer.setOnClickListener { galleryLauncher.launch("image/*") }

        btnPredict.setOnClickListener {
            if (selectedBitmap != null && tflite != null) {
                txtResultHeading.text = "Analyzing..."
                txtConfidence.text = "Running local AI engine..."
                resultCard.setCardBackgroundColor(android.graphics.Color.parseColor("#424242"))
                // Ensure visibility is set AND request layout
                resultCard.visibility = View.VISIBLE
                resultCard.requestLayout()
                resultCard.invalidate()

                Thread {
                    runInference(selectedBitmap!!)
                }.start()
            } else {
                Toast.makeText(this, "Please upload an image first!", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun loadModelFile(): ByteBuffer {
        val fileDescriptor = assets.openFd("model.tflite")
        val inputStream = FileInputStream(fileDescriptor.fileDescriptor)
        val fileChannel = inputStream.channel
        val startOffset = fileDescriptor.startOffset
        val declaredLength = fileDescriptor.declaredLength
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength)
    }

    private fun runInference(bitmap: Bitmap) {
        try {
            val resizedBitmap = Bitmap.createScaledBitmap(bitmap, 224, 224, true)
            val inputBuffer = ByteBuffer.allocateDirect(1 * 3 * 224 * 224 * 4)
            inputBuffer.order(ByteOrder.nativeOrder())

            val intValues = IntArray(224 * 224)
            resizedBitmap.getPixels(intValues, 0, 224, 0, 0, 224, 224)

            // Extract channels with normalization
            for (i in 0 until 224 * 224) {
                val value = intValues[i]
                inputBuffer.putFloat(((value shr 16 and 0xFF) / 255.0f - 0.5f) / 0.5f)
            }
            for (i in 0 until 224 * 224) {
                val value = intValues[i]
                inputBuffer.putFloat(((value shr 8 and 0xFF) / 255.0f - 0.5f) / 0.5f)
            }
            for (i in 0 until 224 * 224) {
                val value = intValues[i]
                inputBuffer.putFloat(((value and 0xFF) / 255.0f - 0.5f) / 0.5f)
            }

            val outputBuffer = Array(1) { FloatArray(25) }
            tflite?.run(inputBuffer, outputBuffer)

            val logits = outputBuffer[0]
            val maxLogit = logits.maxOrNull() ?: 0f
            var sumExp = 0f
            val probs = FloatArray(logits.size)

            for (i in logits.indices) {
                val p = exp((logits[i] - maxLogit).toDouble()).toFloat()
                probs[i] = p
                sumExp += p
            }

            var bestIndex = -1
            var maxProb = -1f
            for (i in probs.indices) {
                probs[i] = probs[i] / sumExp
                if (probs[i] > maxProb) {
                    maxProb = probs[i]
                    bestIndex = i
                }
            }

            val confidence = maxProb * 100
            val diseaseName = diseaseMap[bestIndex] ?: "Unknown"

            runOnUiThread {
                // FORCE UI REFRESH
                resultCard.visibility = View.VISIBLE

                if (confidence < REJECTION_THRESHOLD) {
                    txtResultHeading.text = "Uncertain Result"
                    txtConfidence.text = "Confidence too low (${String.format("%.1f%%", confidence)})."
                    resultCard.setCardBackgroundColor(android.graphics.Color.parseColor("#F57C00"))
                } else {
                    txtResultHeading.text = diseaseName
                    txtConfidence.text = String.format("Confidence: %.1f%%", confidence)

                    if (diseaseName.lowercase().contains("healthy")) {
                        resultCard.setCardBackgroundColor(android.graphics.Color.parseColor("#1F6E43"))
                    } else {
                        resultCard.setCardBackgroundColor(android.graphics.Color.parseColor("#B71C1C"))
                    }
                }

                // Final UI enforcement to ensure it draws
                resultCard.requestLayout()
                resultCard.invalidate()
            }

        } catch (e: Exception) {
            Log.e("TFLite", "Inference Failed", e)
            runOnUiThread {
                txtResultHeading.text = "Analysis Error"
                resultCard.visibility = View.VISIBLE
                resultCard.setCardBackgroundColor(android.graphics.Color.parseColor("#B71C1C"))
                resultCard.requestLayout()
                resultCard.invalidate()
            }
        }
    }

    private fun uriToBitmap(uri: Uri): Bitmap {
        return if (Build.VERSION.SDK_INT >= 28) {
            val source = ImageDecoder.createSource(contentResolver, uri)
            ImageDecoder.decodeBitmap(source).copy(Bitmap.Config.ARGB_8888, true)
        } else {
            MediaStore.Images.Media.getBitmap(contentResolver, uri)
        }
    }

    override fun onDestroy() {
        tflite?.close()
        super.onDestroy()
    }
}