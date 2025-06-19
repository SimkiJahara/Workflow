package com.simki.workflowapp

import android.content.Context
import java.io.BufferedReader
import java.io.InputStreamReader

class BertTokenizer(context: Context) {
    private val vocab: Map<String, Int>
    private val maxSeqLength = 128

    init {
        vocab = loadVocabulary(context, "vocab.txt")
    }

    private fun loadVocabulary(context: Context, vocabFile: String): Map<String, Int> {
        val vocabMap = mutableMapOf<String, Int>()
        context.assets.open(vocabFile).use { inputStream ->
            BufferedReader(InputStreamReader(inputStream)).use { reader ->
                reader.lineSequence().forEachIndexed { index, line ->
                    vocabMap[line.trim()] = index
                }
            }
        }
        return vocabMap
    }

    fun tokenize(text: String): Triple<IntArray, IntArray, IntArray> {
        val tokens = mutableListOf("[CLS]")
        tokens.addAll(text.lowercase().split("\\s+".toRegex()).take(maxSeqLength - 2))
        tokens.add("[SEP]")

        val inputIds = IntArray(maxSeqLength)
        val attentionMask = IntArray(maxSeqLength)
        val tokenTypeIds = IntArray(maxSeqLength)

        tokens.forEachIndexed { index, token ->
            if (index < maxSeqLength) {
                inputIds[index] = vocab[token] ?: vocab["[UNK]"] ?: 0
                attentionMask[index] = 1
            }
        }

        return Triple(inputIds, attentionMask, tokenTypeIds)
    }
}
