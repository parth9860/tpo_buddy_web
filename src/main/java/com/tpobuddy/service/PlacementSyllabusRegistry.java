package com.tpobuddy.service;

import com.tpobuddy.model.TestSeries;

import java.util.*;

/**
 * Structured syllabus definition and registry for the AI Test Series System.
 * Encapsulates:
 * - College Test Series: SNJB Placement Preparation (4 Sections), Basic Computer Science (12 Subjects), College-Specific Assessments
 * - Platform Test Series: Independent Skill Practice, Mixed CS Tests, Weak Topic Tests, Revision Tests
 * - Explicit topic weights, section distributions, and difficulty complexity criteria.
 */
public class PlacementSyllabusRegistry {

    // =========================================================================
    // 1. SNJB Placement Preparation Syllabus Structure (4 Core Sections)
    // =========================================================================

    public static final String SECTION_QUANTITATIVE = "Quantitative Aptitude";
    public static final String SECTION_LOGICAL = "Logical Reasoning";
    public static final String SECTION_VERBAL = "Verbal Ability";
    public static final String SECTION_TECHNICAL = "Technical and Coding";

    public static final List<String> SNJB_SECTIONS = List.of(
            SECTION_QUANTITATIVE,
            SECTION_LOGICAL,
            SECTION_VERBAL,
            SECTION_TECHNICAL
    );

    // Section 1: Quantitative Aptitude Topics
    public static final List<String> TOPICS_QUANTITATIVE = List.of(
            "Number System, HCF and LCM, Divisibility",
            "Percentages, Profit and Loss, Discounts",
            "Ratio, Proportion, Averages, Mixtures and Alligation",
            "Simple and Compound Interest",
            "Time and Work, Pipes and Cisterns",
            "Time, Speed and Distance, Trains, Boats and Streams",
            "Permutations, Combinations, Probability",
            "Algebra and Series",
            "Mensuration",
            "Data Interpretation"
    );

    // Section 2: Logical Reasoning Topics
    public static final List<String> TOPICS_LOGICAL = List.of(
            "Number Series, Letter Series, and Pattern Recognition",
            "Coding-Decoding",
            "Blood Relations and Directions",
            "Linear and Circular Seating Arrangements",
            "Syllogisms",
            "Statements and Conclusions",
            "Logical Puzzles",
            "Ranking and Ordering",
            "Calendars and Clocks",
            "Data Sufficiency"
    );

    // Section 3: Verbal Ability Topics
    public static final List<String> TOPICS_VERBAL = List.of(
            "Grammar and Error Spotting",
            "Sentence Correction and Improvement",
            "Synonyms and Antonyms",
            "Idioms and Phrases",
            "Reading Comprehension",
            "Para Jumbles",
            "Fill in the Blanks"
    );

    // Section 4: Technical & Coding Topics
    public static final List<String> TOPICS_TECHNICAL = List.of(
            "Basic Programming in C, Java, and Python",
            "Code Output Prediction",
            "Variables, Operators, Conditions, Loops, and Functions",
            "Arrays, Strings, Searching, and Sorting",
            "Basic Data Structures and Algorithms",
            "DBMS and SQL",
            "Operating Systems",
            "Computer Networks",
            "Object-Oriented Programming"
    );

    // =========================================================================
    // 2. Basic Computer Science Subjects (Series 2 of College Series)
    // =========================================================================

    public static final List<String> BASIC_CS_SUBJECTS = List.of(
            "Programming Fundamentals",
            "Object-Oriented Programming",
            "Data Structures and Algorithms",
            "DBMS and SQL",
            "Operating Systems",
            "Computer Networks",
            "Software Engineering",
            "Computer Organization and Architecture",
            "Java Programming",
            "Python Programming",
            "C Programming",
            "Problem Solving and Output Prediction"
    );

    // =========================================================================
    // 3. Platform Test Series Types
    // =========================================================================

    public static final List<String> PLATFORM_TRACKS = List.of(
            "Aptitude Practice",
            "Logical Reasoning Practice",
            "Verbal Ability Practice",
            "Programming Practice",
            "DSA Practice",
            "DBMS and SQL Practice",
            "Operating Systems Practice",
            "Computer Networks Practice",
            "Java and OOP Practice",
            "Mixed Computer Science Tests",
            "Coding Challenges",
            "Weak Topic Improvement Tests",
            "Personalized Revision Tests"
    );

    // =========================================================================
    // 4. Default Test Series Factory
    // =========================================================================

    public static List<TestSeries> createDefaultTestSeriesList() {
        List<TestSeries> list = new ArrayList<>();

        // ---------------------------------------------------------------------
        // COLLEGE SERIES 1: SNJB Placement Preparation
        // ---------------------------------------------------------------------
        TestSeries snjb = new TestSeries(
                "college_snjb_full_mock",
                "SNJB Placement Preparation Assessment",
                TestSeries.CATEGORY_COLLEGE,
                "SNJB_PLACEMENT",
                "Flagship comprehensive campus placement assessment covering all 4 SNJB sections: Quantitative Aptitude, Logical Reasoning, Verbal Ability, and Technical & Coding.",
                "Multi-Section Assessment",
                "🎯",
                45
        );
        List<String> snjbAllTopics = new ArrayList<>();
        snjbAllTopics.addAll(TOPICS_QUANTITATIVE);
        snjbAllTopics.addAll(TOPICS_LOGICAL);
        snjbAllTopics.addAll(TOPICS_VERBAL);
        snjbAllTopics.addAll(TOPICS_TECHNICAL);
        snjb.setSyllabusTopics(snjbAllTopics);

        // Section weights for 30 total questions: 8 Quant, 8 Logical, 7 Verbal, 7 Technical
        Map<String, Integer> snjbWeights = new HashMap<>();
        snjbWeights.put(SECTION_QUANTITATIVE, 8);
        snjbWeights.put(SECTION_LOGICAL, 8);
        snjbWeights.put(SECTION_VERBAL, 7);
        snjbWeights.put(SECTION_TECHNICAL, 7);
        snjb.setSectionWeights(snjbWeights);
        list.add(snjb);

        // Individual SNJB Section Tests
        TestSeries snjbQuant = new TestSeries(
                "college_snjb_quant",
                "SNJB Quantitative Aptitude Sectional",
                TestSeries.CATEGORY_COLLEGE,
                "SNJB_PLACEMENT",
                "Focused test covering arithmetic, numerical reasoning, speed math, equations, algebra, and data interpretation.",
                SECTION_QUANTITATIVE,
                "📐",
                30
        );
        snjbQuant.setSyllabusTopics(TOPICS_QUANTITATIVE);
        list.add(snjbQuant);

        TestSeries snjbLogical = new TestSeries(
                "college_snjb_logical",
                "SNJB Logical Reasoning Sectional",
                TestSeries.CATEGORY_COLLEGE,
                "SNJB_PLACEMENT",
                "Targeted evaluation of deductive patterns, syllogisms, seating arrangements, blood relations, and puzzles.",
                SECTION_LOGICAL,
                "🧠",
                30
        );
        snjbLogical.setSyllabusTopics(TOPICS_LOGICAL);
        list.add(snjbLogical);

        TestSeries snjbVerbal = new TestSeries(
                "college_snjb_verbal",
                "SNJB Verbal Ability & Comprehension Sectional",
                TestSeries.CATEGORY_COLLEGE,
                "SNJB_PLACEMENT",
                "Grammar, vocabulary, error detection, sentence correction, and full reading comprehension passages.",
                SECTION_VERBAL,
                "📖",
                25
        );
        snjbVerbal.setSyllabusTopics(TOPICS_VERBAL);
        list.add(snjbVerbal);

        TestSeries snjbTech = new TestSeries(
                "college_snjb_tech",
                "SNJB Technical & Coding Sectional",
                TestSeries.CATEGORY_COLLEGE,
                "SNJB_PLACEMENT",
                "Core programming in C, Java, Python, data structures, SQL queries, OS fundamentals, and code output prediction.",
                SECTION_TECHNICAL,
                "💻",
                30
        );
        snjbTech.setSyllabusTopics(TOPICS_TECHNICAL);
        list.add(snjbTech);

        // ---------------------------------------------------------------------
        // COLLEGE SERIES 2: Basic Computer Science Subject Tests (12 Subjects)
        // ---------------------------------------------------------------------
        Map<String, String> csIcons = Map.ofEntries(
                Map.entry("Programming Fundamentals", "⚙️"),
                Map.entry("Object-Oriented Programming", "🧱"),
                Map.entry("Data Structures and Algorithms", "🌲"),
                Map.entry("DBMS and SQL", "🗄️"),
                Map.entry("Operating Systems", "🖥️"),
                Map.entry("Computer Networks", "🌐"),
                Map.entry("Software Engineering", "📋"),
                Map.entry("Computer Organization and Architecture", "🔌"),
                Map.entry("Java Programming", "☕"),
                Map.entry("Python Programming", "🐍"),
                Map.entry("C Programming", "🔤"),
                Map.entry("Problem Solving and Output Prediction", "🔍")
        );

        for (String subject : BASIC_CS_SUBJECTS) {
            String slug = subject.toLowerCase().replaceAll("[^a-z0-9]+", "_");
            String icon = csIcons.getOrDefault(subject, "💻");
            TestSeries csTest = new TestSeries(
                    "college_cs_" + slug,
                    subject + " Assessment",
                    TestSeries.CATEGORY_COLLEGE,
                    "BASIC_CS",
                    "In-depth subject assessment on " + subject + " according to standard engineering curriculum and technical interview rounds.",
                    subject,
                    icon,
                    30
            );
            csTest.setSyllabusTopics(getTopicsForSubject(subject));
            list.add(csTest);
        }

        // ---------------------------------------------------------------------
        // COLLEGE SERIES 3: College-Specific Tests (Extensible for faculty)
        // ---------------------------------------------------------------------
        TestSeries facultyMock = new TestSeries(
                "college_specific_department_mock",
                "Departmental Campus Pre-Screening Exam",
                TestSeries.CATEGORY_COLLEGE,
                "COLLEGE_SPECIFIC",
                "Faculty-curated pre-placement aptitude and technical screening aligned with tier-1 campus drive formats.",
                "Departmental Assessment",
                "🏛️",
                45
        );
        facultyMock.setSyllabusTopics(List.of("Aptitude Speed Test", "Core Java & OOP", "Relational Database Design", "Operating System Concepts"));
        list.add(facultyMock);

        // ---------------------------------------------------------------------
        // PLATFORM TEST SERIES: Self-Paced Practice & Skill Enhancement
        // ---------------------------------------------------------------------
        Map<String, String> platformIcons = Map.ofEntries(
                Map.entry("Aptitude Practice", "📈"),
                Map.entry("Logical Reasoning Practice", "🧩"),
                Map.entry("Verbal Ability Practice", "🗣️"),
                Map.entry("Programming Practice", "💻"),
                Map.entry("DSA Practice", "⚡"),
                Map.entry("DBMS and SQL Practice", "💾"),
                Map.entry("Operating Systems Practice", "⚙️"),
                Map.entry("Computer Networks Practice", "📡"),
                Map.entry("Java and OOP Practice", "☕"),
                Map.entry("Mixed Computer Science Tests", "🌌"),
                Map.entry("Coding Challenges", "🏆"),
                Map.entry("Weak Topic Improvement Tests", "🎯"),
                Map.entry("Personalized Revision Tests", "🔄")
        );

        for (String track : PLATFORM_TRACKS) {
            String slug = track.toLowerCase().replaceAll("[^a-z0-9]+", "_");
            String icon = platformIcons.getOrDefault(track, "✨");
            TestSeries pt = new TestSeries(
                    "platform_" + slug,
                    track,
                    TestSeries.CATEGORY_PLATFORM,
                    track.contains("Weak") ? "WEAK_TOPIC" : (track.contains("Revision") ? "REVISION" : "PRACTICE_TOPIC"),
                    "Self-paced platform test designed to maximize placement consistency and technical mastery.",
                    track,
                    icon,
                    track.contains("Challenge") ? 40 : 25
            );
            pt.setSyllabusTopics(getTopicsForSubject(track));
            list.add(pt);
        }

        return list;
    }

    public static List<String> getTopicsForSubject(String subject) {
        if (subject == null) return List.of("General Assessment");
        String s = subject.toLowerCase();
        if (s.contains("quant") || s.contains("aptitude")) return TOPICS_QUANTITATIVE;
        if (s.contains("logic") || s.contains("reason")) return TOPICS_LOGICAL;
        if (s.contains("verbal") || s.contains("english")) return TOPICS_VERBAL;
        if (s.contains("dsa") || s.contains("data structure")) {
            return List.of("Arrays and Strings", "Linked Lists", "Stacks and Queues", "Binary Trees and BST",
                    "Searching and Sorting", "Graph Algorithms", "Dynamic Programming Basics", "Time & Space Complexity");
        }
        if (s.contains("dbms") || s.contains("sql")) {
            return List.of("ER Modeling and Normalization", "SQL Joins and Aggregations", "Subqueries & Views",
                    "Indexing and Query Optimization", "ACID Properties & Transactions", "Stored Procedures and Triggers");
        }
        if (s.contains("operating system") || s.contains("os")) {
            return List.of("Process Management & Scheduling", "Threads and Concurrency", "Deadlocks & Semaphores",
                    "Memory Management & Paging", "Virtual Memory", "File Systems & Disk Scheduling");
        }
        if (s.contains("network")) {
            return List.of("OSI and TCP/IP Reference Models", "IP Addressing and Subnetting", "Routing Protocols (OSPF, BGP)",
                    "TCP vs UDP Transport Layer", "Application Layer (HTTP, DNS, SSL/TLS)", "Network Security Fundamentals");
        }
        if (s.contains("java") || s.contains("oop")) {
            return List.of("OOP Principles (Inheritance, Polymorphism)", "Exception Handling", "Java Collections Framework",
                    "Generics and Streams API", "Multithreading & Concurrency", "JVM Memory & Garbage Collection");
        }
        if (s.contains("python")) {
            return List.of("Python Data Types & Slicing", "List/Dict Comprehensions", "Functions and Generators",
                    "OOP in Python", "File I/O and Exceptions", "Standard Library Modules");
        }
        if (s.contains("c programming") || s.equals("c")) {
            return List.of("Data Types & Operators in C", "Pointers and Memory Allocation (malloc/free)",
                    "Arrays, Strings & String Functions", "Structs and Unions", "Bitwise Operations", "Recursion & Functions");
        }
        if (s.contains("software engineering")) {
            return List.of("SDLC Models (Agile, Scrum, Waterfall)", "Design Patterns (Factory, Singleton, Observer)",
                    "Software Testing & Quality Assurance", "CI/CD and Git Workflows", "System Design Fundamentals");
        }
        if (s.contains("organization") || s.contains("architecture") || s.contains("coa")) {
            return List.of("CPU Architecture & Registers", "Instruction Pipelining & Hazards", "Cache Memory & Mapping",
                    "Virtual Memory & MMU", "Interrupts & DMA Controller", "Instruction Set Architecture");
        }
        if (s.contains("output") || s.contains("problem solving")) {
            return List.of("Tricky Pointer Arithmetic in C", "Polymorphism & Static Binding in Java",
                    "Recursion Stack Traces", "Bitwise Operator Outputs", "Short-Circuit Logical Evaluations", "Scope and Closure in Python");
        }
        return TOPICS_TECHNICAL;
    }
}
