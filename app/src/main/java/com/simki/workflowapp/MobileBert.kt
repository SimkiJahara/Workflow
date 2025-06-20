package com.simki.workflowapp

import android.content.Context
import android.util.Log
import org.tensorflow.lite.Interpreter
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.IntBuffer
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel
import kotlin.math.exp

class MobileBert(private val context: Context) {
    private val tag = "MobileBert"
    private var interpreter: Interpreter
    private var inputBuffer: IntBuffer
    private var outputBuffer: ByteBuffer

    init {
        @Suppress("MagicNumber")
        val modelBuffer = loadModelFile(context, "1.tflite")
        Log.d(tag, "Model loaded, size: ${modelBuffer.capacity()} bytes")
        interpreter = Interpreter(modelBuffer)
        val inputTensor = interpreter.getInputTensor(0)
        Log.d(tag, "Input quantization - scale: ${inputTensor.quantizationParams().scale}, zeroPoint: ${inputTensor.quantizationParams().zeroPoint}")
        inputBuffer = ByteBuffer.allocateDirect(384 * 4).order(ByteOrder.nativeOrder()).asIntBuffer()
        outputBuffer = ByteBuffer.allocateDirect(384 * 2).order(ByteOrder.nativeOrder())
        Log.d(tag, "Input buffer capacity: ${inputBuffer.capacity()} ints")
        Log.d(tag, "Output buffer capacity: ${outputBuffer.capacity()} bytes")
    }

    private fun loadModelFile(context: Context, modelPath: String): MappedByteBuffer {
        val fileDescriptor = context.assets.openFd(modelPath)
        val inputStream = FileInputStream(fileDescriptor.fileDescriptor)
        val fileChannel = inputStream.channel
        val startOffset = fileDescriptor.startOffset
        val declaredLength = fileDescriptor.declaredLength
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength).also {
            inputStream.close()
        }
    }

    fun predict(input: String): String? {
        try {
            Log.d(tag, "Input: $input")
            val tokenizer = BertTokenizer(context)
            val tokenIds = tokenizer.tokenize(input)
            Log.d(tag, "Token IDs size: ${tokenIds.size}")

            // Clear and resize input buffer if necessary
            inputBuffer.clear()
            if (tokenIds.size > inputBuffer.capacity()) {
                val newInputBuffer = ByteBuffer.allocateDirect(tokenIds.size * 4).order(ByteOrder.nativeOrder()).asIntBuffer()
                newInputBuffer.put(tokenIds)
                inputBuffer = newInputBuffer // Reassign after filling
                Log.d(tag, "Resized inputBuffer to ${inputBuffer.capacity()} ints")
            } else {
                inputBuffer.put(tokenIds)
            }
            inputBuffer.position(0)
            Log.d(tag, "Input buffer prepared")

            // Clear and resize output buffer if necessary
            outputBuffer.clear()
            if (tokenIds.size * 2 > outputBuffer.capacity()) {
                val newOutputBuffer = ByteBuffer.allocateDirect(tokenIds.size * 2).order(ByteOrder.nativeOrder())
                outputBuffer = newOutputBuffer // Reassign
                Log.d(tag, "Resized outputBuffer to ${outputBuffer.capacity()} bytes")
            }
            interpreter.run(inputBuffer, outputBuffer)
            outputBuffer.position(0)
            Log.d(tag, "Inference completed")

            val outputByteArray = ByteArray(tokenIds.size * 2)
            outputBuffer.get(outputByteArray)
            val floatOutput = outputByteArray.map { it.toFloat() } // Initial INT8 to float conversion
            Log.d(tag, "Raw Output: ${floatOutput.take(5).joinToString()}")
            Log.d(tag, "Output size: ${floatOutput.size}")
            return processOutput(floatOutput)
        } catch (e: Exception) {
            Log.e(tag, "Prediction failed: ${e.message}", e)
            return null
        }
    }

    private fun processOutput(output: List<Float>): String {
        val outputTensor = interpreter.getOutputTensor(0)
        val scale = outputTensor.quantizationParams().scale
        val zeroPoint = outputTensor.quantizationParams().zeroPoint.toFloat()
        Log.d(tag, "Scale: $scale, ZeroPoint: $zeroPoint")
        val dequantized = output.map { it * scale + zeroPoint }
        Log.d(tag, "Dequantized: ${dequantized.take(10).joinToString()}")
        val probabilities = dequantized.chunked(2).map { pair ->
            val sumExp = exp(pair[0]) + exp(pair[1])
            listOf(exp(pair[0]) / sumExp, exp(pair[1]) / sumExp)
        }.flatten()
        Log.d(tag, "Output size: ${output.size}")
        return "Probabilities: ${probabilities.take(5).joinToString()}"
    }


    fun close() {
        interpreter.close()
        Log.d(tag, "Interpreter closed")
    }
}