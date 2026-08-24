package com.ledger.config;

/**
 * Grade scale is defined for assessments out of 20 marks only.
 * totalMarks must be exactly 20; otherwise no grade / no percent is produced.
 */
public final class GradeUtil {
    private GradeUtil() {}

    /** True only when total is exactly 20. */
    public static boolean isGradable(int totalMarks) {
        return totalMarks == 20;
    }

    /**
     * Letter grade from marks obtained out of 20.
     * Returns "—" if total is not 20.
     */
    public static String letterFromMarks(int marksObtained, int totalMarks) {
        if (totalMarks != 20) return "—";
        if (marksObtained >= 18) return "A+ (Excellent)";
        if (marksObtained >= 16) return "A (Very Good)";
        if (marksObtained >= 14) return "B+ (Good)";
        if (marksObtained >= 12) return "B (Average)";
        if (marksObtained >= 10) return "C (Pass)";
        if (marksObtained >= 8)  return "D (Needs Improvement)";
        return "F (Fail)";
    }

    /**
     * Percent only when total is 20; otherwise -1 (caller should show "—").
     */
    public static double percentFromMarks(int marksObtained, int totalMarks) {
        if (totalMarks != 20) return -1;
        return Math.round((100.0 * marksObtained / 20.0) * 10.0) / 10.0;
    }

    /** Legacy helper: only grades when percent is from a 20-mark paper (caller must pass marks). Prefer letterFromMarks. */
    public static String letter(double percent) {
        // Kept for non-20 contexts that still call it — returns "—" always so we don't invent grades.
        return "—";
    }
}
