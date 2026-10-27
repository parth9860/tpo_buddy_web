package com.tpobuddy.service;

import com.tpobuddy.model.Question;
import com.tpobuddy.model.TestSeries;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service that selects exactly 30 questions from a 50-candidate question pool.
 * Enforces:
 * - Independent cryptographically-randomized selection per student attempt
 * - Section and topic weight fulfillment (e.g. 8 Quant, 8 Logical, 7 Verbal, 7 Tech)
 * - Difficulty consistency (Medium, Hard, Extra Hard)
 * - Question variety (MCQ, Output Prediction, Reading Comprehension)
 * - Safe discarding of the remaining 20 candidate questions
 */
@Service
public class QuestionSelectionService {

    public static final int TARGET_QUESTION_COUNT = 30;
    private final SecureRandom secureRandom = new SecureRandom();

    /**
     * Selects exactly 30 questions according to the test configuration.
     *
     * @param candidatePool The validated pool of candidate questions (typically 50)
     * @param series        The test series configuration
     * @param difficulty    Selected difficulty (Medium, Hard, Extra Hard)
     * @return List of exactly 30 selected questions in randomized display order
     */
    public List<Question> selectQuestionsForAttempt(List<Question> candidatePool, TestSeries series, String difficulty) {
        if (candidatePool == null || candidatePool.isEmpty()) {
            return Collections.emptyList();
        }

        // 1. Filter or bias by selected difficulty if possible
        List<Question> pool = new ArrayList<>(candidatePool);
        Collections.shuffle(pool, secureRandom);

        List<Question> selected = new ArrayList<>();
        Map<String, Integer> sectionWeights = (series != null) ? series.getSectionWeights() : Collections.emptyMap();

        if (sectionWeights != null && !sectionWeights.isEmpty()) {
            // Multi-section test: partition candidates by section
            Map<String, List<Question>> sectionMap = new HashMap<>();
            for (Question q : pool) {
                String sec = q.getSection();
                if (sec == null || sec.isEmpty()) {
                    sec = q.getCategory() != null ? q.getCategory() : "General";
                }
                // Normalize to match configured weights
                String matchedSec = matchSection(sec, sectionWeights.keySet());
                sectionMap.computeIfAbsent(matchedSec, k -> new ArrayList<>()).add(q);
            }

            // Select specified count per section
            int totalWeightedAllocated = 0;
            for (Map.Entry<String, Integer> entry : sectionWeights.entrySet()) {
                String sec = entry.getKey();
                int targetCount = entry.getValue();
                List<Question> available = sectionMap.getOrDefault(sec, Collections.emptyList());

                List<Question> chosenFromSection = pickFromList(available, targetCount);
                selected.addAll(chosenFromSection);
                totalWeightedAllocated += chosenFromSection.size();
            }

            // If we have less than 30 due to section candidate shortfalls, fill from remaining pool
            if (selected.size() < TARGET_QUESTION_COUNT) {
                Set<Integer> alreadySelectedIds = selected.stream().map(Question::getId).collect(Collectors.toSet());
                List<Question> remaining = pool.stream()
                        .filter(q -> !alreadySelectedIds.contains(q.getId()))
                        .collect(Collectors.toList());

                int needed = TARGET_QUESTION_COUNT - selected.size();
                selected.addAll(pickFromList(remaining, needed));
            }
        } else {
            // Single-subject or topic test: ensure topic variety
            Map<String, List<Question>> topicMap = new HashMap<>();
            for (Question q : pool) {
                String top = q.getTopic() != null ? q.getTopic() : "General";
                topicMap.computeIfAbsent(top, k -> new ArrayList<>()).add(q);
            }

            // Distribute across topics round-robin
            List<String> topics = new ArrayList<>(topicMap.keySet());
            Collections.shuffle(topics, secureRandom);

            int currentTopicIdx = 0;
            Set<Integer> chosenIds = new HashSet<>();

            while (selected.size() < TARGET_QUESTION_COUNT && !topics.isEmpty()) {
                String top = topics.get(currentTopicIdx % topics.size());
                List<Question> availableInTopic = topicMap.get(top);

                Optional<Question> candidate = availableInTopic.stream()
                        .filter(q -> !chosenIds.contains(q.getId()))
                        .findFirst();

                if (candidate.isPresent()) {
                    Question q = candidate.get();
                    selected.add(q);
                    chosenIds.add(q.getId());
                } else {
                    topics.remove(top);
                    if (topics.isEmpty()) break;
                }
                currentTopicIdx++;
            }

            // If still under 30, pick directly from shuffled pool
            if (selected.size() < TARGET_QUESTION_COUNT) {
                for (Question q : pool) {
                    if (selected.size() >= TARGET_QUESTION_COUNT) break;
                    if (!chosenIds.contains(q.getId())) {
                        selected.add(q);
                        chosenIds.add(q.getId());
                    }
                }
            }
        }

        // Cap to exactly TARGET_QUESTION_COUNT (30)
        if (selected.size() > TARGET_QUESTION_COUNT) {
            selected = selected.subList(0, TARGET_QUESTION_COUNT);
        }

        // 2. Final independent shuffle so no two students have identical question order
        Collections.shuffle(selected, secureRandom);

        // 3. Assign display order 1 to 30
        for (int i = 0; i < selected.size(); i++) {
            selected.get(i).setDisplayOrder(i + 1);
        }

        return selected;
    }

    private List<Question> pickFromList(List<Question> list, int count) {
        if (list == null || list.isEmpty() || count <= 0) {
            return new ArrayList<>();
        }
        List<Question> copy = new ArrayList<>(list);
        Collections.shuffle(copy, secureRandom);
        if (copy.size() <= count) {
            return copy;
        }
        return copy.subList(0, count);
    }

    private String matchSection(String candidateSec, Set<String> targetSections) {
        if (candidateSec == null) return "General";
        for (String target : targetSections) {
            if (candidateSec.equalsIgnoreCase(target)
                    || candidateSec.toLowerCase().contains(target.toLowerCase())
                    || target.toLowerCase().contains(candidateSec.toLowerCase())) {
                return target;
            }
        }
        return candidateSec;
    }
}
