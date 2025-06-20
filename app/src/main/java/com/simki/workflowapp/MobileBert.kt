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
    private val inputBuffer: IntBuffer
    private val outputBuffer: ByteBuffer


init {
    val modelBuffer = loadModelFile(context, "1.tflite")
    Log.d(tag, "Model loaded, size: ${modelBuffer.capacity()} bytes")
    interpreter = Interpreter(modelBuffer as MappedByteBuffer)
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

            inputBuffer.clear()
            if (tokenIds.size > inputBuffer.capacity()) {
                Log.e(tag, "Token IDs size (${tokenIds.size}) exceeds buffer capacity (${inputBuffer.capacity()})")
                return null
            }
            inputBuffer.put(tokenIds)
            inputBuffer.position(0)
            Log.d(tag, "Input buffer prepared")

            interpreter.run(inputBuffer, outputBuffer)
            outputBuffer.position(0)
            Log.d(tag, "Inference completed")


            val outputByteArray = ByteArray(384 * 2)
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
