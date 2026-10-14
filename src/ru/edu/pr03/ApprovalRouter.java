package ru.edu.pr03;

/**
 * Маршрутизация служебных заявок.
 *
 * Вариант 1.
 *   Часть А: R3 — FINANCE_REVIEW при amount >= 250000 (константа FINANCE_LIMIT изменена на 250000).
 *   Часть Б: addRiskScore — числовой риск-скор заявки.
 */
public class ApprovalRouter {

    /** Порог финансового контроля. Вариант 1: 250 000 (единственное место, где задано значение). */
    public static final long FINANCE_LIMIT = 250_000L;

    private static final long SCORE_AMOUNT_STEP = 10_000L;
    private static final int SCORE_URGENT = 100;
    private static final int SCORE_HIGH_RISK = 200;
    private static final int SCORE_MEDIUM_RISK = 50;

    /**
     * Определяет маршрут по правилам R1–R5 в порядке приоритета (fail-fast):
     * R1 персональные данные, R2 риск HIGH, R3 сумма >= FINANCE_LIMIT, R4 срочная и сумма < FINANCE_LIMIT, R5 стандарт.
     */
    public ApprovalRoute route(long amount, boolean urgent, RiskLevel risk, boolean containsPersonalData) {
        requireValid(amount, risk);
        if (requiresSecurityReview(containsPersonalData)) {
            return ApprovalRoute.SECURITY_REVIEW;
        }
        if (requiresDirectorReview(risk)) {
            return ApprovalRoute.DIRECTOR_REVIEW;
        }
        if (requiresFinanceReview(amount)) {
            return ApprovalRoute.FINANCE_REVIEW;
        }
        if (requiresFastTrack(amount, urgent)) {
            return ApprovalRoute.FAST_TRACK;
        }
        return ApprovalRoute.STANDARD;
    }

    /**
     * Вариант 1, часть Б: риск-скор = сумма / 10000 (целочисленно) + 100 для срочной + 200 для HIGH + 50 для MEDIUM.
     * Параметр containsPersonalData принят ради единообразия сигнатуры с route и в формуле не участвует.
     */
    public int addRiskScore(long amount, boolean urgent, RiskLevel risk, boolean containsPersonalData) {
        requireValid(amount, risk);
        int score = (int) (amount / SCORE_AMOUNT_STEP);
        if (urgent) {
            score += SCORE_URGENT;
        }
        if (risk == RiskLevel.HIGH) {
            score += SCORE_HIGH_RISK;
        } else if (risk == RiskLevel.MEDIUM) {
            score += SCORE_MEDIUM_RISK;
        }
        return score;
    }

    /** Защитные условия: контракты метода, а не проверка пользовательского ввода. */
    private void requireValid(long amount, RiskLevel risk) {
        if (amount < 0) {
            throw new IllegalArgumentException("Ошибка: amount must be >= 0, got " + amount);
        }
        if (risk == null) {
            throw new IllegalArgumentException("Ошибка: risk is null");
        }
    }

    private boolean requiresSecurityReview(boolean containsPersonalData) {
        return containsPersonalData;
    }

    private boolean requiresDirectorReview(RiskLevel risk) {
        return risk == RiskLevel.HIGH;
    }

    private boolean requiresFinanceReview(long amount) {
        return amount >= FINANCE_LIMIT;
    }

    private boolean requiresFastTrack(long amount, boolean urgent) {
        return urgent && amount < FINANCE_LIMIT;
    }
}
