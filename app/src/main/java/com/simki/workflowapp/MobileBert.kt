package com.simki.workflowapp

import android.content.Context
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.support.common.FileUtil
import java.nio.ByteBuffer
import java.nio.ByteOrder

class MobileBert(context: Context) {
    private val interpreter: Interpreter
    private val tokenizer: BertTokenizer = BertTokenizer(context)

    init {
        val modelBuffer = FileUtil.loadMappedFile(context, "mobilebert.tflite")
        interpreter = Interpreter(modelBuffer)
    }

    fun predict(inputText: String): String {
        val (inputIds, attentionMask, tokenTypeIds) = tokenizer.tokenize(inputText)

        val inputIdsBuffer = ByteBuffer.allocateDirect(inputIds.size * 4)
            .order(ByteOrder.nativeOrder())
            .apply { inputIds.forEach { putInt(it) } }
        val attentionMaskBuffer = ByteBuffer.allocateDirect(attentionMask.size * 4)
            .order(ByteOrder.nativeOrder())
            .apply { attentionMask.forEach { putInt(it) } }
        val tokenTypeIdsBuffer = ByteBuffer.allocateDirect(tokenTypeIds.size * 4)
            .order(ByteOrder.nativeOrder())
            .apply { tokenTypeIds.forEach { putInt(it) } }

        val outputBuffer = ByteBuffer.allocateDirect(2 * 4)
            .order(ByteOrder.nativeOrder())

        interpreter.runForMultipleInputsOutputs(
            arrayOf(inputIdsBuffer, attentionMaskBuffer, tokenTypeIdsBuffer),
            mapOf(0 to outputBuffer)
        )

        outputBuffer.rewind()
        val outputs = FloatArray(2)
        for (i in outputs.indices) {
            outputs[i] = outputBuffer.float
        }

        return if (outputs[0] > outputs[1]) "Positive" else "Negative"
    }

    fun close() {
        interpreter.close()
    }
}