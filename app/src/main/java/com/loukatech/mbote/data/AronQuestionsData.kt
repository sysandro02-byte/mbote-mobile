package com.loukatech.mbote.data

import com.loukatech.mbote.model.AronQuestion

/**
 * Compatibility holder kept for older callers.
 * Production questions are loaded from /v1/content/aron-questions.
 */
object AronQuestionsData {
    val allQuestions: List<AronQuestion> = emptyList()
}
