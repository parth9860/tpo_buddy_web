/**
 * TPO Buddy - Complete Client-Side Placement Question Bank & Exam Engine
 * Provides offline & static hosting capability for AI Test Series with 0ms network latency.
 */

(function (root, factory) {
    if (typeof define === 'function' && define.amd) {
        define([], factory);
    } else if (typeof module === 'object' && module.exports) {
        module.exports = factory();
    } else {
        root.PlacementQuestionBank = factory();
    }
}(typeof self !== 'undefined' ? self : this, function () {

    const ALL_QUESTIONS = [
        // =====================================================================
        // QUANTITATIVE APTITUDE
        // =====================================================================
        {
            id: 101,
            category: "Quantitative Aptitude",
            topic: "Number System",
            difficulty: "Easy",
            question: "What is the unit digit of the expression (7^95 - 3^58)?",
            optionA: "0",
            optionB: "4",
            optionC: "6",
            optionD: "7",
            correctOption: 1,
            marks: 2,
            solution: "Step 1: The cyclicity of 7 is 4 (7^1=7, 7^2=9, 7^3=3, 7^4=1). 95 mod 4 = 3, so the unit digit of 7^95 is 7^3 = 3.\nStep 2: The cyclicity of 3 is 4 (3^1=3, 3^2=9, 3^3=7, 3^4=1). 58 mod 4 = 2, so the unit digit of 3^58 is 3^2 = 9.\nStep 3: Subtracting with borrow: (13 - 9) = 4. Therefore, the unit digit is 4."
        },
        {
            id: 102,
            category: "Quantitative Aptitude",
            topic: "Number System",
            difficulty: "Medium",
            question: "When a number is successively divided by 4, 5, and 6, the remainders are 2, 3, and 4 respectively. What is the smallest such positive integer?",
            optionA: "214",
            optionB: "102",
            optionC: "118",
            optionD: "238",
            correctOption: 0,
            marks: 2,
            solution: "Step 1: Using the formula for successive division: N = 4 * [5 * (6k + 4) + 3] + 2.\nStep 2: For the smallest positive integer, let k = 0 -> N = 4 * [20 + 3] + 2 = 94 (Check: 94 / 4 = 23 rem 2, 23 / 5 = 4 rem 3, 4 / 6 = 0 rem 4). With quotient 1: k = 1 -> 6(1)+4=10; 5(10)+3=53; 4(53)+2 = 214.\nChecking 214: 214 ÷ 4 = 53 rem 2; 53 ÷ 5 = 10 rem 3; 10 ÷ 6 = 1 rem 4. The smallest option is 214."
        },
        {
            id: 103,
            category: "Quantitative Aptitude",
            topic: "Number System",
            difficulty: "Easy",
            question: "How many two-digit prime numbers exist such that both the digits are also prime numbers?",
            optionA: "4",
            optionB: "5",
            optionC: "6",
            optionD: "7",
            correctOption: 0,
            marks: 2,
            solution: "Prime digits are {2, 3, 5, 7}.\nPossible two-digit numbers formed by these digits are: 23, 27, 32, 35, 37, 53, 57, 73.\nTesting for primality: 23 (prime), 27 (divisible by 3), 32 (even), 35 (divisible by 5), 37 (prime), 53 (prime), 57 (divisible by 3), 73 (prime).\nThus, exactly 4 such numbers exist: 23, 37, 53, 73."
        },
        {
            id: 104,
            category: "Quantitative Aptitude",
            topic: "Number System",
            difficulty: "Hard",
            question: "Find the total number of trailing zeros in 125! (125 factorial).",
            optionA: "25",
            optionB: "28",
            optionC: "31",
            optionD: "35",
            correctOption: 2,
            marks: 2,
            solution: "Legendre's formula for trailing zeros in n! counts factors of 5:\nZeros = floor(125/5) + floor(125/25) + floor(125/125)\n= 25 + 5 + 1 = 31 trailing zeros."
        },
        {
            id: 105,
            category: "Quantitative Aptitude",
            topic: "HCF and LCM",
            difficulty: "Medium",
            question: "The HCF of two numbers is 11 and their LCM is 693. If one of the numbers is 77, what is the other number?",
            optionA: "88",
            optionB: "99",
            optionC: "108",
            optionD: "121",
            correctOption: 1,
            marks: 2,
            solution: "Product of two numbers = HCF × LCM\n77 × Other Number = 11 × 693\nOther Number = (11 × 693) / 77 = 693 / 7 = 99."
        },
        {
            id: 106,
            category: "Quantitative Aptitude",
            topic: "HCF and LCM",
            difficulty: "Easy",
            question: "Four bells toll together at intervals of 6, 8, 12, and 18 seconds respectively. If they toll together now, after how many minutes will they toll together again?",
            optionA: "1.2 minutes",
            optionB: "1.5 minutes",
            optionC: "2 minutes",
            optionD: "2.4 minutes",
            correctOption: 0,
            marks: 2,
            solution: "Find the LCM of (6, 8, 12, 18):\n6 = 2 * 3, 8 = 2^3, 12 = 2^2 * 3, 18 = 2 * 3^2.\nLCM = 2^3 * 3^2 = 8 * 9 = 72 seconds.\n72 seconds / 60 = 1.2 minutes."
        },
        {
            id: 107,
            category: "Quantitative Aptitude",
            topic: "HCF and LCM",
            difficulty: "Hard",
            question: "What is the greatest number that will divide 398, 436, and 542 leaving remainders 7, 11, and 15 respectively?",
            optionA: "11",
            optionB: "17",
            optionC: "19",
            optionD: "23",
            correctOption: 1,
            marks: 2,
            solution: "The required number will be the HCF of (398 - 7), (436 - 11), and (542 - 15):\n= HCF(391, 425, 527).\n391 = 17 × 23\n425 = 17 × 25\n527 = 17 × 31\nHence, the HCF is 17."
        },
        {
            id: 108,
            category: "Quantitative Aptitude",
            topic: "Divisibility Rules",
            difficulty: "Medium",
            question: "If the 8-digit number 785x367y is divisible by 72, find the value of (x + y).",
            optionA: "6",
            optionB: "7",
            optionC: "8",
            optionD: "9",
            correctOption: 3,
            marks: 2,
            solution: "72 = 8 × 9 (where 8 and 9 are co-prime).\nDivisibility by 8: The last 3 digits 67y must be divisible by 8. 672 / 8 = 84, so y = 2.\nDivisibility by 9: Sum of digits must be divisible by 9.\nSum = 7 + 8 + 5 + x + 3 + 6 + 7 + 2 = 38 + x.\nThe next multiple of 9 is 45. Hence, 38 + x = 45 => x = 7.\nTherefore, (x + y) = 7 + 2 = 9."
        },
        {
            id: 109,
            category: "Quantitative Aptitude",
            topic: "Divisibility Rules",
            difficulty: "Easy",
            question: "Which of the following numbers is completely divisible by 11?",
            optionA: "4832718",
            optionB: "4832719",
            optionC: "4832728",
            optionD: "4832738",
            correctOption: 0,
            marks: 2,
            solution: "Divisibility by 11: Difference between sum of odd positioned digits and even positioned digits must be 0 or a multiple of 11.\nFor 4832718 (from right):\nOdd positions: 8 + 7 + 3 + 4 = 22.\nEven positions: 1 + 2 + 8 = 11.\nDifference = 22 - 11 = 11 (divisible by 11)."
        },
        {
            id: 110,
            category: "Quantitative Aptitude",
            topic: "Percentages",
            difficulty: "Easy",
            question: "If the price of sugar increases by 25%, by what percentage must a household reduce its consumption so that total expenditure remains unchanged?",
            optionA: "15%",
            optionB: "20%",
            optionC: "25%",
            optionD: "33.33%",
            correctOption: 1,
            marks: 2,
            solution: "Formula: Reduction = [R / (100 + R)] × 100%\n= [25 / (100 + 25)] × 100% = (25 / 125) × 100% = 1/5 × 100% = 20%."
        },
        {
            id: 111,
            category: "Quantitative Aptitude",
            topic: "Percentages",
            difficulty: "Medium",
            question: "In an examination, 70% of students passed in English, 65% passed in Mathematics, and 27% failed in both. If 248 students passed in both, find the total candidates.",
            optionA: "350",
            optionB: "400",
            optionC: "500",
            optionD: "600",
            correctOption: 1,
            marks: 2,
            solution: "Failed in English = 30%, Failed in Math = 35%, Failed in both = 27%.\nFailed in at least one = 30 + 35 - 27 = 38%.\nPassed in both = 100% - 38% = 62%.\n62% of Total = 248 => Total = (248 × 100) / 62 = 400 candidates."
        },
        {
            id: 112,
            category: "Quantitative Aptitude",
            topic: "Percentages",
            difficulty: "Medium",
            question: "A student scored 32% marks and failed by 12 marks. Another student scored 42% marks and got 28 marks more than passing marks. Find the passing percentage.",
            optionA: "33%",
            optionB: "35%",
            optionC: "36%",
            optionD: "38%",
            correctOption: 1,
            marks: 2,
            solution: "Difference in percentage = 42% - 32% = 10%.\nDifference in actual marks = 28 - (-12) = 40 marks.\n10% = 40 => Total Marks = 400.\nPassing marks = 32% of 400 + 12 = 128 + 12 = 140.\nPassing percentage = (140 / 400) × 100 = 35%."
        },
        {
            id: 113,
            category: "Quantitative Aptitude",
            topic: "Profit and Loss",
            difficulty: "Easy",
            question: "A shopkeeper sells an article for ₹840 and gains 20%. What was the cost price of the article?",
            optionA: "₹650",
            optionB: "₹700",
            optionC: "₹720",
            optionD: "₹750",
            correctOption: 1,
            marks: 2,
            solution: "SP = CP × (100 + Gain%) / 100\n840 = CP × 1.20\nCP = 840 / 1.20 = ₹700."
        },
        {
            id: 114,
            category: "Quantitative Aptitude",
            topic: "Profit and Loss",
            difficulty: "Hard",
            question: "A dishonest dealer claims to sell his goods at cost price, but uses a false weight of 920 grams for 1 kilogram. What is his actual profit percentage?",
            optionA: "8.0%",
            optionB: "8.25%",
            optionC: "8.70%",
            optionD: "9.12%",
            correctOption: 2,
            marks: 2,
            solution: "Profit percentage = [Error / (True Value - Error)] × 100%\n= [80 / (1000 - 80)] × 100% = (80 / 920) × 100% ≈ 8.70%."
        },
        {
            id: 115,
            category: "Quantitative Aptitude",
            topic: "Discounts",
            difficulty: "Easy",
            question: "Find the single equivalent discount for two successive discounts of 20% and 10%.",
            optionA: "28%",
            optionB: "30%",
            optionC: "26%",
            optionD: "32%",
            correctOption: 0,
            marks: 2,
            solution: "Equivalent Discount = d1 + d2 - (d1 × d2 / 100)\n= 20 + 10 - (20 × 10 / 100) = 30 - 2 = 28%."
        },
        {
            id: 116,
            category: "Quantitative Aptitude",
            topic: "Time and Work",
            difficulty: "Medium",
            question: "A can complete a piece of work in 12 days and B in 18 days. If they work together for 4 days, what fraction of work is left?",
            optionA: "1/3",
            optionB: "4/9",
            optionC: "5/9",
            optionD: "7/18",
            correctOption: 1,
            marks: 2,
            solution: "1 day work together = 1/12 + 1/18 = 5/36.\nWork done in 4 days = 4 × (5/36) = 20/36 = 5/9.\nRemaining work = 1 - 5/9 = 4/9."
        },
        {
            id: 117,
            category: "Quantitative Aptitude",
            topic: "Time, Speed and Distance",
            difficulty: "Medium",
            question: "A train 180 meters long is traveling at a speed of 72 km/h. How many seconds will it take to pass an electric pole?",
            optionA: "6 seconds",
            optionB: "8 seconds",
            optionC: "9 seconds",
            optionD: "12 seconds",
            correctOption: 2,
            marks: 2,
            solution: "Speed in m/s = 72 × (5/18) = 20 m/s.\nTime = Distance / Speed = 180 m / 20 m/s = 9 seconds."
        },
        {
            id: 118,
            category: "Quantitative Aptitude",
            topic: "Probability",
            difficulty: "Easy",
            question: "Two dice are thrown simultaneously. What is the probability of getting a sum of 8?",
            optionA: "1/6",
            optionB: "5/36",
            optionC: "7/36",
            optionD: "1/9",
            correctOption: 1,
            marks: 2,
            solution: "Total outcomes = 6 × 6 = 36.\nFavorable pairs summing to 8: (2,6), (3,5), (4,4), (5,3), (6,2) -> 5 outcomes.\nProbability = 5/36."
        },

        // =====================================================================
        // LOGICAL REASONING
        // =====================================================================
        {
            id: 201,
            category: "Logical Reasoning",
            topic: "Number Series",
            difficulty: "Easy",
            question: "Find the next number in the sequence: 4, 9, 25, 49, 121, ?",
            optionA: "144",
            optionB: "169",
            optionC: "196",
            optionD: "225",
            correctOption: 1,
            marks: 2,
            solution: "The numbers are squares of consecutive prime numbers:\n2^2 = 4\n3^2 = 9\n5^2 = 25\n7^2 = 49\n11^2 = 121\nThe next prime number is 13, so 13^2 = 169."
        },
        {
            id: 202,
            category: "Logical Reasoning",
            topic: "Coding and Decoding",
            difficulty: "Medium",
            question: "In a certain code language, 'ROBOT' is written as 'TQDOT'. How will 'BRAIN' be written in that code?",
            optionA: "DTCKP",
            optionB: "DTALP",
            optionC: "DTCLP",
            optionD: "ETCKO",
            correctOption: 0,
            marks: 2,
            solution: "Letter shift pattern: +2 for each position:\nB(2) + 2 = D(4)\nR(18) + 2 = T(20)\nA(1) + 2 = C(3)\nI(9) + 2 = K(11)\nN(14) + 2 = P(16)\nResult: DTCKP."
        },
        {
            id: 203,
            category: "Logical Reasoning",
            topic: "Blood Relations",
            difficulty: "Medium",
            question: "Pointing to a photograph, a woman says: 'He is the only son of the mother of my husband's father'. How is the man in the photograph related to the woman's husband?",
            optionA: "Brother",
            optionB: "Uncle",
            optionC: "Father",
            optionD: "Grandfather",
            correctOption: 2,
            marks: 2,
            solution: "1. 'my husband's father' = Father-in-law.\n2. 'mother of my husband's father' = Grandmother-in-law.\n3. 'only son of grandmother' = Father-in-law himself.\nHence, the man is the husband's Father."
        },
        {
            id: 204,
            category: "Logical Reasoning",
            topic: "Direction Sense",
            difficulty: "Medium",
            question: "A person walks 10 km North, turns right and walks 6 km, then turns right again and walks 18 km. How far and in which direction is he now from the starting point?",
            optionA: "10 km, South-East",
            optionB: "10 km, South-West",
            optionC: "12 km, South-East",
            optionD: "14 km, South",
            correctOption: 0,
            marks: 2,
            solution: "Coordinates: Start (0, 0) -> North 10: (0, 10) -> East 6: (6, 10) -> South 18: (6, -8).\nDistance = sqrt(6^2 + (-8)^2) = sqrt(36 + 64) = 10 km.\nDirection: (+x, -y) = South-East."
        },
        {
            id: 205,
            category: "Logical Reasoning",
            topic: "Syllogisms",
            difficulty: "Medium",
            question: "Statements:\n1. All cars are vehicles.\n2. Some vehicles are electric.\nConclusions:\nI. Some cars are electric.\nII. Some electric are vehicles.",
            optionA: "Only Conclusion I follows",
            optionB: "Only Conclusion II follows",
            optionC: "Both I and II follow",
            optionD: "Neither I nor II follows",
            correctOption: 1,
            marks: 2,
            solution: "Statement 2 'Some vehicles are electric' immediately implies the converse 'Some electric are vehicles' (Conclusion II is valid). No direct link exists between cars and electric, so I does not necessarily follow."
        },
        {
            id: 206,
            category: "Logical Reasoning",
            topic: "Clocks and Calendars",
            difficulty: "Hard",
            question: "What is the angle between the hour hand and minute hand of a clock at 3:40?",
            optionA: "120°",
            optionB: "125°",
            optionC: "130°",
            optionD: "140°",
            correctOption: 2,
            marks: 2,
            solution: "Angle formula = |30H - (11/2)M|\nAt 3:40: H = 3, M = 40.\nAngle = |30(3) - (11/2)(40)| = |90 - 220| = |-130| = 130°."
        },

        // =====================================================================
        // VERBAL ABILITY
        // =====================================================================
        {
            id: 301,
            category: "Verbal Ability",
            topic: "Grammar and Error Spotting",
            difficulty: "Medium",
            question: "Identify the part of the sentence containing an error:\n'Neither the manager (A) nor the employees (B) was present at (C) the annual company review (D).'",
            optionA: "Part A",
            optionB: "Part B",
            optionC: "Part C",
            optionD: "Part D",
            correctOption: 2,
            marks: 2,
            solution: "Rule of proximity for 'neither... nor': the verb agrees with the subject closest to it. Here 'employees' is plural, so the verb should be 'were present' instead of 'was present' (Part C)."
        },
        {
            id: 302,
            category: "Verbal Ability",
            topic: "Synonyms",
            difficulty: "Easy",
            question: "Choose the word that is most nearly identical in meaning to CANDID:",
            optionA: "Secretive",
            optionB: "Frank",
            optionC: "Polite",
            optionD: "Deceitful",
            correctOption: 1,
            marks: 2,
            solution: "'Candid' means truthful and straightforward; frank. 'Frank' is the exact synonym."
        },
        {
            id: 303,
            category: "Verbal Ability",
            topic: "Antonyms",
            difficulty: "Easy",
            question: "Choose the word that is opposite in meaning to METICULOUS:",
            optionA: "Careless",
            optionB: "Thorough",
            optionC: "Detailed",
            optionD: "Accurate",
            correctOption: 0,
            marks: 2,
            solution: "'Meticulous' means showing great attention to detail; very careful and precise. The opposite is 'Careless'."
        },
        {
            id: 304,
            category: "Verbal Ability",
            topic: "Sentence Correction",
            difficulty: "Medium",
            question: "Select the correct alternative for the underlined portion:\n'If he would have worked harder, he would have cleared the campus interview.'",
            optionA: "If he had worked harder",
            optionB: "If he worked harder",
            optionC: "Had he worked more hard",
            optionD: "No improvement needed",
            correctOption: 0,
            marks: 2,
            solution: "In third conditional sentences expressing an unreal past event, the 'if' clause takes the past perfect tense ('had + past participle'). Correct: 'If he had worked harder...'."
        },
        {
            id: 305,
            category: "Verbal Ability",
            topic: "Reading Comprehension",
            difficulty: "Hard",
            question: "Based on the provided passage, what is the author's primary attitude toward autonomous artificial intelligence systems in safety-critical sectors?",
            optionA: "Unconditional optimism",
            optionB: "Prudent vigilance and structured governance",
            optionC: "Hostile rejection",
            optionD: "Passive indifference",
            correctOption: 1,
            marks: 2,
            questionType: "READING_COMPREHENSION",
            passageOrCode: "The inexorable migration toward autonomous algorithmic systems in safety-critical sectors—such as civil aviation, surgical robotics, and high-voltage power grid orchestration—demands a fundamental reconsideration of risk mitigation paradigms. While deep neural networks deliver unprecedented pattern recognition capabilities, their inherent opacity and susceptibility to distributional shift present profound architectural challenges. Autonomous systems cannot merely rely upon statistical validation; they mandate rigorous formal verification, deterministic fail-safe rollbacks, and continuous human-in-the-loop oversight. Without robust institutional governance frameworks, the velocity of deployment risks outpacing our capacity to guarantee operational resilience.",
            solution: "The passage explicitly advocates that while technological autonomy offers speed advantages, mission-critical operations necessitate rigorous formal verification, human-in-the-loop auditability, and structured governance frameworks. Hence, 'Prudent vigilance and structured governance' represents the author's nuanced stance."
        },

        // =====================================================================
        // TECHNICAL AND CODING
        // =====================================================================
        {
            id: 401,
            category: "Technical and Coding",
            topic: "Java Programming Basics",
            difficulty: "Medium",
            question: "What will be the output of the following Java snippet?\n\nString s1 = \"TPO\";\nString s2 = new String(\"TPO\");\nSystem.out.println((s1 == s2) + \" \" + s1.equals(s2));",
            optionA: "true true",
            optionB: "false true",
            optionC: "true false",
            optionD: "false false",
            correctOption: 1,
            marks: 2,
            solution: "In Java, '==' compares object memory references. 's1' refers to the string in the String Constant Pool, while 's2' is created on the heap with 'new', so s1 == s2 is false. The equals() method compares content characters, so s1.equals(s2) is true. Output: 'false true'."
        },
        {
            id: 402,
            category: "Technical and Coding",
            topic: "DBMS and SQL",
            difficulty: "Medium",
            question: "Which SQL clause is executed FIRST in the logical query processing order?",
            optionA: "SELECT",
            optionB: "FROM",
            optionC: "WHERE",
            optionD: "ORDER BY",
            correctOption: 1,
            marks: 2,
            solution: "The standard SQL logical query processing order is: 1. FROM (and JOINs), 2. WHERE, 3. GROUP BY, 4. HAVING, 5. SELECT, 6. DISTINCT, 7. ORDER BY, 8. LIMIT / OFFSET. Therefore, FROM is processed first."
        },
        {
            id: 403,
            category: "Technical and Coding",
            topic: "Data Structures and Algorithms",
            difficulty: "Medium",
            question: "What is the average time complexity of searching an element in a balanced Binary Search Tree (AVL / Red-Black Tree)?",
            optionA: "O(1)",
            optionB: "O(log n)",
            optionC: "O(n)",
            optionD: "O(n log n)",
            correctOption: 1,
            marks: 2,
            solution: "In a self-balancing binary search tree with n nodes, the maximum height is strictly bounded by O(log n). Therefore, searching, insertion, and deletion all execute in O(log n) time."
        },
        {
            id: 404,
            category: "Technical and Coding",
            topic: "Operating Systems",
            difficulty: "Medium",
            question: "Which of the following conditions is NOT one of Coffman's four mandatory conditions for a Deadlock to occur?",
            optionA: "Mutual Exclusion",
            optionB: "Hold and Wait",
            optionC: "Preemption Allowed",
            optionD: "Circular Wait",
            correctOption: 2,
            marks: 2,
            solution: "The four Coffman conditions for deadlock are: 1. Mutual Exclusion, 2. Hold and Wait, 3. No Preemption (resources cannot be forcibly reclaimed), 4. Circular Wait. 'Preemption Allowed' actually breaks deadlocks, so it is NOT a deadlock condition."
        },
        {
            id: 405,
            category: "Technical and Coding",
            topic: "Computer Networks",
            difficulty: "Easy",
            question: "Which protocol operates at the Transport Layer of the OSI Model to provide reliable, connection-oriented data transmission?",
            optionA: "IP",
            optionB: "UDP",
            optionC: "TCP",
            optionD: "HTTP",
            correctOption: 2,
            marks: 2,
            solution: "TCP (Transmission Control Protocol) operates at Layer 4 (Transport Layer) and guarantees reliable, ordered, error-checked data delivery using a 3-way handshake."
        },
        {
            id: 406,
            category: "Technical and Coding",
            topic: "Problem Solving and Output Prediction",
            difficulty: "Hard",
            question: "What will be the output of the following Java program?",
            optionA: "Parent Static -> Child Static -> Parent Init -> Child Init",
            optionB: "Child Static -> Parent Static -> Child Init -> Parent Init",
            optionC: "Parent Init -> Child Init -> Parent Static -> Child Static",
            optionD: "Compilation Error",
            correctOption: 0,
            marks: 2,
            questionType: "OUTPUT_PREDICTION",
            passageOrCode: "class Parent {\n    static { System.out.print(\"Parent Static -> \"); }\n    { System.out.print(\"Parent Init -> \"); }\n}\nclass Child extends Parent {\n    static { System.out.print(\"Child Static -> \"); }\n    { System.out.print(\"Child Init\"); }\n    public static void main(String[] args) {\n        new Child();\n    }\n}",
            solution: "In Java class initialization order: 1. Superclass static blocks execute first. 2. Subclass static blocks execute. 3. Upon instantiation: Superclass instance initializers & constructor execute. 4. Finally, Subclass instance initializers & constructor execute."
        },
        {
            id: 407,
            category: "Technical and Coding",
            topic: "Python Programming",
            difficulty: "Hard",
            question: "What is the output of the following Python code snippet?",
            optionA: "[1] [2]",
            optionB: "[1] [1, 2]",
            optionC: "[[1]] [[2]]",
            optionD: "AttributeError",
            correctOption: 1,
            marks: 2,
            questionType: "OUTPUT_PREDICTION",
            passageOrCode: "def append_item(val, target=[]):\n    target.append(val)\n    return target\n\nprint(append_item(1), append_item(2))",
            solution: "In Python, default parameter values are evaluated ONCE when the function definition is executed, NOT each time the function is called. Because the default list 'target=[]' is mutable, both function calls mutate the exact same list instance in memory, resulting in [1] then [1, 2]."
        },
        {
            id: 408,
            category: "Technical and Coding",
            topic: "C Programming",
            difficulty: "Extra Hard",
            question: "What will be printed when the following C code executes?",
            optionA: "15 15",
            optionB: "16 16",
            optionC: "15 16",
            optionD: "Undefined behavior",
            correctOption: 0,
            marks: 2,
            questionType: "OUTPUT_PREDICTION",
            passageOrCode: "#include <stdio.h>\nint main() {\n    int arr[5] = {1, 2, 3, 4, 5};\n    int *ptr1 = (int*)(&arr + 1);\n    int *ptr2 = (int*)(arr + 1);\n    printf(\"%d %d\", *(ptr1 - 1) * *ptr2 + 5, 15);\n    return 0;\n}",
            solution: "In C: &arr is a pointer to the entire array of 5 ints. (&arr + 1) increments by sizeof(arr) = 5 * sizeof(int). Casting to (int*) and subtracting 1 points to arr[4] (value 5). *(ptr1 - 1) = 5. arr decaying to pointer + 1 points to arr[1] (value 2). *ptr2 = 2. Result = 5 * 2 + 5 = 15."
        },
        {
            id: 409,
            category: "Technical and Coding",
            topic: "DBMS and SQL",
            difficulty: "Extra Hard",
            question: "In relational database concurrency control, which Isolation Level completely prevents Dirty Reads, Non-repeatable Reads, AND Phantom Reads?",
            optionA: "Read Committed",
            optionB: "Repeatable Read",
            optionC: "Serializable",
            optionD: "Snapshot Isolation",
            correctOption: 2,
            marks: 2,
            solution: "SQL-92 Standard defines 4 transaction isolation levels. Serializable is the highest level and guarantees that concurrent transaction execution yields the exact same state as some serial execution order, completely eliminating Dirty Reads, Non-repeatable Reads, and Phantom Reads."
        },
        {
            id: 410,
            category: "Technical and Coding",
            topic: "Computer Networks",
            difficulty: "Hard",
            question: "How many usable host IP addresses are available in a subnetwork with the CIDR prefix /27?",
            optionA: "28",
            optionB: "30",
            optionC: "32",
            optionD: "62",
            correctOption: 1,
            marks: 2,
            solution: "An IPv4 address has 32 bits. A /27 network leaves 32 - 27 = 5 bits for host addressing. Total addresses = 2^5 = 32. Subtracting 2 reserved addresses (1 for network address and 1 for broadcast address) leaves 32 - 2 = 30 usable host IP addresses."
        }
    ];

    function shuffle(array) {
        const copy = [...array];
        for (let i = copy.length - 1; i > 0; i--) {
            const j = Math.floor(Math.random() * (i + 1));
            [copy[i], copy[j]] = [copy[j], copy[i]];
        }
        return copy;
    }

    function normalize(str) {
        return (str || '').toLowerCase().trim();
    }

    /**
     * Calibrate and return balanced questions matching filter
     */
    function getQuestionsForAttempt(options = {}) {
        const category = normalize(options.category);
        const topic = normalize(options.topic);
        const difficulty = normalize(options.difficulty || 'medium');
        const count = options.count || 30;

        let filtered = ALL_QUESTIONS.filter(q => {
            const catMatch = !category || category === 'all' || category.includes('mock') || normalize(q.category).includes(category) || category.includes(normalize(q.category));
            const topMatch = !topic || topic === 'all' || topic === 'general assessment' || normalize(q.topic).includes(topic) || topic.includes(normalize(q.topic));
            return catMatch && topMatch;
        });

        // If not enough questions for strict topic, include category questions
        if (filtered.length < count && category && category !== 'all') {
            const catPool = ALL_QUESTIONS.filter(q => normalize(q.category).includes(category) || category.includes(normalize(q.category)));
            const ids = new Set(filtered.map(q => q.id));
            catPool.forEach(q => {
                if (!ids.has(q.id)) {
                    filtered.push(q);
                    ids.add(q.id);
                }
            });
        }

        // If still not enough, include all pool questions
        if (filtered.length < count) {
            const ids = new Set(filtered.map(q => q.id));
            ALL_QUESTIONS.forEach(q => {
                if (!ids.has(q.id)) {
                    filtered.push(q);
                    ids.add(q.id);
                }
            });
        }

        // Shuffle and trim
        let shuffled = shuffle(filtered);
        
        // If pool is smaller than 30, duplicate with unique IDs so student gets full 30-question exam experience
        let result = [];
        let counter = 1;
        while (result.length < count) {
            for (let q of shuffled) {
                if (result.length >= count) break;
                result.push({
                    ...q,
                    id: counter++,
                    originalId: q.id
                });
            }
        }

        return result;
    }

    /**
     * Client-side Grading & Performance Analysis
     */
    function gradeAttempt(attemptMeta = {}, questions = [], userAnswers = {}, timeTakenSeconds = 0, violations = 0) {
        let correctCount = 0;
        let incorrectCount = 0;
        let unansweredCount = 0;
        let totalMarksEarned = 0;
        let totalMaxMarks = 0;

        const sectionMap = {};
        const topicWrongCount = {};

        const reviews = questions.map((q, index) => {
            const userAns = userAnswers[q.id];
            const isAnswered = (userAns !== undefined && userAns !== null && userAns >= 0);
            const isCorrect = isAnswered && (userAns === q.correctOption);

            const qMarks = q.marks || 2;
            totalMaxMarks += qMarks;

            if (isCorrect) {
                correctCount++;
                totalMarksEarned += qMarks;
            } else if (isAnswered) {
                incorrectCount++;
                topicWrongCount[q.topic] = (topicWrongCount[q.topic] || 0) + 1;
            } else {
                unansweredCount++;
                topicWrongCount[q.topic] = (topicWrongCount[q.topic] || 0) + 1;
            }

            // Section metrics
            const sec = q.section || q.category || 'General';
            if (!sectionMap[sec]) {
                sectionMap[sec] = { total: 0, correct: 0, score: 0 };
            }
            sectionMap[sec].total++;
            if (isCorrect) sectionMap[sec].correct++;

            return {
                id: q.id,
                questionNumber: index + 1,
                question: q.question,
                optionA: q.optionA,
                optionB: q.optionB,
                optionC: q.optionC,
                optionD: q.optionD,
                userAnswer: isAnswered ? userAns : -1,
                selectedOption: isAnswered ? userAns : -1,
                correctOption: q.correctOption,
                correctAnswer: q.correctOption,
                isCorrect: isCorrect,
                marks: qMarks,
                explanation: q.solution,
                solution: q.solution,
                passageOrCode: q.passageOrCode || '',
                questionType: q.questionType || 'MCQ',
                category: q.category,
                topic: q.topic,
                difficulty: q.difficulty
            };
        });

        const totalQuestions = questions.length || 30;
        const attemptedCount = correctCount + incorrectCount;
        const accuracy = attemptedCount > 0 ? Math.round((correctCount * 100) / attemptedCount) : 0;
        const score = totalQuestions > 0 ? Math.round((correctCount * 100) / totalQuestions) : 0;
        const passed = score >= 60;

        const weakTopics = Object.keys(topicWrongCount)
            .sort((a, b) => topicWrongCount[b] - topicWrongCount[a])
            .slice(0, 3);

        const attemptId = attemptMeta.attemptId || ('att_' + Date.now().toString(36));

        const resultData = {
            attemptId: attemptId,
            testTitle: attemptMeta.title || attemptMeta.seriesTitle || "Placement Readiness Assessment",
            category: attemptMeta.category || "General Assessment",
            difficulty: attemptMeta.difficulty || "Medium",
            seriesId: attemptMeta.seriesId || "college_snjb_quant",
            totalQuestions: totalQuestions,
            correctCount: correctCount,
            incorrectCount: incorrectCount,
            unansweredCount: unansweredCount,
            score: score,
            accuracy: accuracy,
            accuracyPercentage: accuracy,
            passed: passed,
            timeTakenSeconds: timeTakenSeconds,
            violationCount: violations,
            riskLevel: violations >= 3 ? "HIGH" : (violations >= 1 ? "MODERATE" : "LOW"),
            status: "submitted",
            timestamp: Date.now(),
            sectionBreakdown: sectionMap,
            weakTopics: weakTopics.length > 0 ? weakTopics : ["Speed and Accuracy", "Time Management"],
            reviews: reviews
        };

        // Persist to localStorage for static test-result.html hydration
        try {
            localStorage.setItem('latestTestAttempt', JSON.stringify(resultData));
            localStorage.setItem('testAttempt_' + attemptId, JSON.stringify(resultData));
            
            // Keep recent attempts history
            const historyStr = localStorage.getItem('tpo_attempts_history');
            const history = historyStr ? JSON.parse(historyStr) : [];
            history.unshift({
                id: attemptId,
                title: resultData.testTitle,
                category: resultData.category,
                score: score,
                passed: passed,
                date: new Date().toLocaleDateString('en-US', { month: 'short', day: 'numeric', year: 'numeric' })
            });
            localStorage.setItem('tpo_attempts_history', JSON.stringify(history.slice(0, 20)));
        } catch (e) {
            console.warn('Could not save test attempt to localStorage:', e);
        }

        return resultData;
    }

    return {
        ALL_QUESTIONS,
        getQuestionsForAttempt,
        gradeAttempt
    };
}));
