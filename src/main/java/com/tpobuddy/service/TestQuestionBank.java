package com.tpobuddy.service;

import com.tpobuddy.model.Question;

import java.util.*;

/**
 * Question bank providing assessments sourced exclusively from Firebase Realtime Database.
 * Zero hardcoded, mock, or static questions.
 */
public class TestQuestionBank {

    /**
     * Retrieves questions filtered by section/category directly from Firebase Realtime Database,
     * with random shuffling up to the requested question limit.
     */
    public static List<Question> getQuestions(String category, int count) {
        List<Question> liveQuestions = FirebaseService.getInstance().fetchQuestions(category);

        if (liveQuestions == null || liveQuestions.isEmpty()) {
            return new ArrayList<>();
        }

        List<Question> result = new ArrayList<>(liveQuestions);
        Collections.shuffle(result, new Random());
        int limit = Math.min(count, result.size());
        return new ArrayList<>(result.subList(0, limit));
    }

    /**
     * Retrieves a single question by ID from Firebase Realtime Database.
     */
    public static Question getQuestionById(int id) {
        List<Question> all = FirebaseService.getInstance().fetchQuestions(null);
        if (all != null) {
            for (Question q : all) {
                if (q.getId() == id) {
                    return q;
                }
            }
        }
        return null;
    }
}
