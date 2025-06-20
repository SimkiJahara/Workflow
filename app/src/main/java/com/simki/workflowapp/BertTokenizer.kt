package com.simki.workflowapp

import android.content.Context
import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.*

class BertTokenizer(context: Context) {
    private val vocab: Map<String, Int>
    private val maxLength = 384 // Match the model's input shape from Netron

    init {
        vocab = loadVocab(context)
    }

    private fun loadVocab(context: Context): Map<String, Int> {
        val vocabMap = mutableMapOf<String, Int>()
        try {
            context.assets.open("vocab.txt").use { inputStream ->
                BufferedReader(InputStreamReader(inputStream)).use { reader ->
                    var line: String? = reader.readLine()
                    var index = 0
                    while (line != null) {
                        vocabMap[line.trim()] = index++
                        line = reader.readLine()
                    }
                }
            }
        } catch (e: Exception) {
            throw RuntimeException("Failed to load vocab.txt: ${e.message}")
        }
        return vocabMap
    }

fun tokenize(text: String): IntArray {
    val tokens = text.lowercase()
        .split("\\s+".toRegex())
        .mapNotNull { word ->
            vocab[word] ?: vocab.getOrDefault("[UNK]", 0)
        }
        .toMutableList()

    // Truncate or pad to maxLength
    val inputIds = IntArray(maxLength)
    tokens.take(maxLength).forEachIndexed { index, tokenId ->
        inputIds[index] = tokenId
    }
    // Pad with 0s if necessary
    return inputIds
}
}
