package com.example.camerasample.data

data class FeatureSupport(
    val isSupport: Boolean,
    val featureName: String,
    val description: String
) {
    override fun toString(): String {
        val sb = StringBuilder()
        sb.appendLine(" - featureName: $featureName")
        sb.appendLine(" - isSupport: $isSupport")
        if (description.isNotBlank()) {
            sb.appendLine(" - description: $description")
        }
        return sb.toString()
    }
}
