package ru.edu.pr03;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.Scanner;

/**
 * Единственная точка входа. Режим 1: unit-проверки. Режим 2: data-driven по data/sample.csv.
 * Режим 3: интерактивный (amount;urgent;risk;personal, выход: exit или пустая строка).
 * Строка sample.csv с неверным форматом считается FAIL (см. DECISIONS.md).
 */
public class SelfCheck {

    private static int passed = 0;
    private static int failed = 0;
    private static final ApprovalRouter ROUTER = new ApprovalRouter();

    // ------------------------------------------------------------------ вспомогательные методы

    private static void report(String name, boolean ok, String details) {
        if (ok) {
            passed++;
            System.out.println("PASS " + name);
        } else {
            failed++;
            System.out.println("FAIL " + name + ": " + details);
        }
    }

    private static void checkRoute(String name, long amount, boolean urgent, RiskLevel risk, boolean personal,
                                   ApprovalRoute expected) {
        try {
            ApprovalRoute actual = ROUTER.route(amount, urgent, risk, personal);
            report(name, actual == expected, "ожидалось " + expected + ", получено " + actual);
        } catch (IllegalArgumentException e) {
            report(name, false, "неожиданная ошибка: " + e.getMessage());
        }
    }

    private static void checkRouteError(String name, long amount, RiskLevel risk, String expectedPart) {
        try {
            ROUTER.route(amount, true, risk, false);
            report(name, false, "ожидалась ошибка");
        } catch (IllegalArgumentException e) {
            report(name, e.getMessage().contains(expectedPart), e.getMessage());
        }
    }

    private static void checkScore(String name, long amount, boolean urgent, RiskLevel risk, boolean personal,
                                   int expected) {
        try {
            int actual = ROUTER.addRiskScore(amount, urgent, risk, personal);
            report(name, actual == expected, "ожидалось " + expected + ", получено " + actual);
        } catch (IllegalArgumentException e) {
            report(name, false, "неожиданная ошибка: " + e.getMessage());
        }
    }

    // ------------------------------------------------------------------ режим 1: unit-проверки

    /** Базовые 13 проверок из раздела 8.4; ожидания учитывают вариант 1 (FINANCE_LIMIT = 250000). */
    private static void checkBase() {
        checkRoute("1 personal > urgent", 10_000, true, RiskLevel.LOW, true, ApprovalRoute.SECURITY_REVIEW);
        checkRoute("2 HIGH > urgent", 10_000, true, RiskLevel.HIGH, false, ApprovalRoute.DIRECTOR_REVIEW);
        checkRoute("3 personal > HIGH", 10_000, false, RiskLevel.HIGH, true, ApprovalRoute.SECURITY_REVIEW);
        checkRoute("4 amount > urgent", 600_000, true, RiskLevel.LOW, false, ApprovalRoute.FINANCE_REVIEW);
        checkRoute("5 FAST_TRACK", 10_000, true, RiskLevel.LOW, false, ApprovalRoute.FAST_TRACK);
        checkRoute("6 FINANCE_REVIEW 500000", 500_000, false, RiskLevel.LOW, false, ApprovalRoute.FINANCE_REVIEW);
        checkRoute("7 STANDARD", 10_000, false, RiskLevel.LOW, false, ApprovalRoute.STANDARD);
        checkRoute("8 MEDIUM -> STANDARD", 10_000, false, RiskLevel.MEDIUM, false, ApprovalRoute.STANDARD);
        checkRoute("9 SECURITY_REVIEW", 10_000, false, RiskLevel.LOW, true, ApprovalRoute.SECURITY_REVIEW);
        // 10. Раньше 499999;true давал FAST_TRACK; при пороге 250000 это уже FINANCE_REVIEW
        checkRoute("10 499999 urgent -> FINANCE (variant 1)", 499_999, true, RiskLevel.LOW, false, ApprovalRoute.FINANCE_REVIEW);
        checkRoute("11 500001 -> FINANCE_REVIEW", 500_001, false, RiskLevel.LOW, false, ApprovalRoute.FINANCE_REVIEW);
        checkRouteError("12 amount < 0", -1, RiskLevel.LOW, "amount must be >= 0");
        checkRouteError("13 risk == null", 10_000, null, "risk is null");
    }

    /** Часть А варианта 1: граница 250000 включена в R3, остальные срочные ниже порога идут в FAST_TRACK. */
    private static void checkVariantA() {
        checkRoute("A1 249999 urgent -> FAST_TRACK", 249_999, true, RiskLevel.LOW, false, ApprovalRoute.FAST_TRACK);
        checkRoute("A2 250000 -> FINANCE_REVIEW", 250_000, false, RiskLevel.LOW, false, ApprovalRoute.FINANCE_REVIEW);
        checkRoute("A3 250000 urgent -> FINANCE_REVIEW", 250_000, true, RiskLevel.LOW, false, ApprovalRoute.FINANCE_REVIEW);
        checkRoute("A4 249999 not urgent -> STANDARD", 249_999, false, RiskLevel.LOW, false, ApprovalRoute.STANDARD);
        report("A5 FINANCE_LIMIT constant", ApprovalRouter.FINANCE_LIMIT == 250_000L, "" + ApprovalRouter.FINANCE_LIMIT);
    }

    /** Часть Б варианта 1: addRiskScore. */
    private static void checkVariantB() {
        checkScore("B1 urgent LOW", 10_000, true, RiskLevel.LOW, false, 101);
        checkScore("B2 HIGH not urgent", 500_000, false, RiskLevel.HIGH, false, 250);
        checkScore("B3 zero", 0, false, RiskLevel.LOW, false, 0);
        checkScore("B4 MEDIUM, floor division", 99_999, false, RiskLevel.MEDIUM, true, 59);
        checkScore("B5 everything", 250_000, true, RiskLevel.HIGH, true, 325);
        try {
            ROUTER.addRiskScore(-5, false, RiskLevel.LOW, false);
            report("B6 amount < 0", false, "ожидалась ошибка");
        } catch (IllegalArgumentException e) {
            report("B6 amount < 0", e.getMessage().contains("amount must be >= 0"), e.getMessage());
        }
    }

    /** Дополнительные проверки из раздела 8.5. */
    private static void checkExtra() {
        checkRoute("X1 HIGH + big amount -> DIRECTOR", 600_000, false, RiskLevel.HIGH, false, ApprovalRoute.DIRECTOR_REVIEW);
        checkRoute("X2 amount 0 -> STANDARD", 0, false, RiskLevel.LOW, false, ApprovalRoute.STANDARD);
        checkRoute("X3 MEDIUM urgent small -> FAST_TRACK", 10_000, true, RiskLevel.MEDIUM, false, ApprovalRoute.FAST_TRACK);
        checkRoute("X4 defaults -> STANDARD", 0, false, RiskLevel.LOW, false, ApprovalRoute.STANDARD);
        checkRoute("X5 MEDIUM big -> FINANCE", 300_000, false, RiskLevel.MEDIUM, false, ApprovalRoute.FINANCE_REVIEW);
    }

    // ------------------------------------------------------------------ разбор строки (csv и интерактив)

    private static long parseAmount(String text) {
        try {
            return Long.parseLong(text);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Ошибка: amount должен быть целым числом, получено \"" + text + "\"");
        }
    }

    private static boolean parseFlag(String text, String name) {
        if (text.equals("true")) {
            return true;
        }
        if (text.equals("false")) {
            return false;
        }
        throw new IllegalArgumentException("Ошибка: " + name + " должен быть true или false, получено \"" + text + "\"");
    }

    private static RiskLevel parseRisk(String text) {
        if (text.equals("null")) {
            return null;
        }
        try {
            return RiskLevel.valueOf(text);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Ошибка: risk должен быть LOW, MEDIUM или HIGH, получено \"" + text + "\"");
        }
    }

    /** parts: amount, urgent, risk, personal (первые 4 элемента). */
    private static ApprovalRoute routeOf(String[] parts) {
        return ROUTER.route(parseAmount(parts[0]), parseFlag(parts[1], "urgent"), parseRisk(parts[2]),
                parseFlag(parts[3], "personal"));
    }

    // ------------------------------------------------------------------ режим 2: data/sample.csv

    private static void runDataLine(String line, int n) {
        String name = "data/" + n;
        String[] p = line.split(";", -1);
        if (p.length != 5) {
            report(name, false, "ожидалось 5 колонок, получено " + p.length);
            return;
        }
        try {
            ApprovalRoute actual = routeOf(p);
            report(name, actual.name().equals(p[4]), "ожидалось " + p[4] + ", получено " + actual);
        } catch (IllegalArgumentException e) {
            report(name, p[4].equals("ERROR") && e.getMessage().startsWith("Ошибка: "), "ожидалось " + p[4] + ", " + e.getMessage());
        }
    }

    private static void runDataDriven() throws IOException {
        List<String> lines = Files.readAllLines(Paths.get("data/sample.csv"));
        int before = passed;
        int n = 0;
        for (int i = 1; i < lines.size(); i++) { // первая строка — заголовок
            if (lines.get(i).isEmpty()) {
                continue;
            }
            n++;
            runDataLine(lines.get(i), n);
        }
        System.out.println("Data-driven: " + (passed - before) + "/" + n + " passed");
    }

    // ------------------------------------------------------------------ режим 3: интерактив

    private static void runInteractive() {
        Scanner in = new Scanner(System.in);
        while (true) {
            System.out.print("Заявка amount;urgent;risk;personal (exit или пустая строка — выход): ");
            if (!in.hasNextLine()) {
                break;
            }
            String line = in.nextLine();
            if (line.isEmpty() || line.equals("exit")) {
                break;
            }
            String[] p = line.split(";", -1);
            if (p.length != 4) {
                System.out.println("Ошибка: ожидалось 4 значения, получено " + p.length);
                continue;
            }
            try {
                System.out.println("Маршрут: " + routeOf(p));
                System.out.println("Риск-скор: " + ROUTER.addRiskScore(parseAmount(p[0]), parseFlag(p[1], "urgent"),
                        parseRisk(p[2]), parseFlag(p[3], "personal")));
            } catch (IllegalArgumentException e) {
                System.out.println(e.getMessage());
            }
        }
    }

    // ------------------------------------------------------------------ точка входа

    public static void main(String[] args) throws IOException {
        checkBase();
        checkVariantA();
        checkVariantB();
        checkExtra();
        System.out.println("Unit: " + passed + "/" + (passed + failed) + " passed");
        runDataDriven();
        System.out.println(failed == 0 ? "ALL PASS" : "FAILED: " + failed);
        if (failed != 0) {
            System.exit(1);
        }
        runInteractive();
    }
}
