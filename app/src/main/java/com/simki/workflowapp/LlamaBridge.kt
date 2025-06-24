package com.simki.workflowapp

object LlamaBridge {
    init {
        System.loadLibrary("llama-jni")
    }
    external fun runModel(prompt: String): String

}
