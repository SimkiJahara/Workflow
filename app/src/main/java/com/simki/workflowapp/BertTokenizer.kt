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
        // Basic tokenization: split by whitespace and map to vocab IDs
        val tokens = text.lowercase()
            .split("\\s+".toRegex())
            .mapNotNull { word ->
                vocab[word] ?: vocab.getOrDefault("[UNK]", 0) // Fallback to [UNK] if not found
            }
            .toMutableList()

        // Pad or truncate to maxLength
        val inputIds = IntArray(maxLength) { 0 }
        tokens.forEachIndexed { index, tokenId ->
            if (index < maxLength) {
                inputIds[index] = tokenId
            }
        }
        return inputIds
    }
}
