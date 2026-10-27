package com.tpobuddy.service;

import com.tpobuddy.model.TestSeries;
import com.tpobuddy.model.User;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Service managing the Test Series catalogue.
 * Strictly separates College Test Series from Platform Test Series.
 * Supports dynamic configuration by faculty and administrators.
 */
@Service
public class TestSeriesService {

    private final Map<String, TestSeries> seriesRegistry = new ConcurrentHashMap<>();

    public TestSeriesService() {
        initializeCatalogue();
    }

    private void initializeCatalogue() {
        List<TestSeries> defaultSeries = PlacementSyllabusRegistry.createDefaultTestSeriesList();
        for (TestSeries ts : defaultSeries) {
            seriesRegistry.put(ts.getSeriesId(), ts);
        }
    }

    public List<TestSeries> getSeriesByCategory(String category) {
        if (category == null || category.trim().isEmpty()) {
            return getAllSeries();
        }
        String normalized = category.trim().toUpperCase();
        return seriesRegistry.values().stream()
                .filter(ts -> ts.isActive() && normalized.equalsIgnoreCase(ts.getCategory()))
                .sorted(Comparator.comparing(TestSeries::getSeriesId))
                .collect(Collectors.toList());
    }

    public List<TestSeries> getAllSeries() {
        return seriesRegistry.values().stream()
                .filter(TestSeries::isActive)
                .sorted(Comparator.comparing(TestSeries::getCategory).thenComparing(TestSeries::getSeriesId))
                .collect(Collectors.toList());
    }

    public TestSeries getSeriesById(String seriesId) {
        if (seriesId == null) return null;
        return seriesRegistry.get(seriesId);
    }

    public synchronized boolean registerSeries(TestSeries series) {
        if (series == null || series.getSeriesId() == null) return false;
        seriesRegistry.put(series.getSeriesId(), series);
        return true;
    }

    public synchronized boolean updateSeriesStatus(String seriesId, boolean active, User user) {
        if (user == null || (!user.isTeacher() && !user.isAdmin())) {
            return false;
        }
        TestSeries ts = seriesRegistry.get(seriesId);
        if (ts != null) {
            ts.setActive(active);
            ts.setUpdatedAt(System.currentTimeMillis());
            return true;
        }
        return false;
    }
}
