/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.updateschecker;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * One {@code vulnerable_versions} string from a security advisory: comma-separated clauses, ANDed, each an operator
 * ({@code <}, {@code <=}, {@code >}, {@code >=}, {@code =}) and a {@code MAJOR.MINOR.PATCH} version; a bare version
 * means exact match.
 *
 * <p>This decides whether a running version is reported as affected by a CVE, so it never guesses: anything it cannot
 * read exactly - a commit SHA, an empty or malformed string, an unsupported operator - is <em>unparseable</em>, and so
 * is a range that can never match (such as {@code >2.23.5, <2.25.0, >=2.25.3}). The caller must surface an unparseable
 * range for manual review rather than treat it as "not affected".
 */
public final class UpdatesCheckerVersionRange {

    private static final int MAX_CLAUSES = 20;

    private static final Pattern CLAUSE = Pattern.compile("^(<=|>=|<|>|=)?\\s*(\\S+)$");

    private enum Operator {
        LT,
        LE,
        GT,
        GE,
        EQ
    }

    private static final class Clause {
        private final Operator operator;

        private final UpdatesCheckerVersion version;

        private Clause(Operator operator, UpdatesCheckerVersion version) {
            this.operator = operator;
            this.version = version;
        }

        private boolean matches(UpdatesCheckerVersion candidate) {
            int c = candidate.compareTo(version);
            switch (operator) {
                case LT:
                    return c < 0;
                case LE:
                    return c <= 0;
                case GT:
                    return c > 0;
                case GE:
                    return c >= 0;
                default:
                    return c == 0;
            }
        }
    }

    private final List<Clause> clauses;

    private final boolean parseable;

    private UpdatesCheckerVersionRange(List<Clause> clauses, boolean parseable) {
        this.clauses = clauses;
        this.parseable = parseable;
    }

    private static UpdatesCheckerVersionRange unparseable() {
        return new UpdatesCheckerVersionRange(Collections.<Clause>emptyList(), false);
    }

    /** Never throws: an unreadable string yields an unparseable range. */
    public static UpdatesCheckerVersionRange parse(String text) {
        if (text == null || text.trim().isEmpty()) {
            return unparseable();
        }
        String[] parts = text.split(",", -1);
        if (parts.length > MAX_CLAUSES) {
            return unparseable();
        }
        List<Clause> parsed = new ArrayList<>();
        for (String part : parts) {
            Matcher m = CLAUSE.matcher(part.trim());
            if (!m.matches()) {
                return unparseable();
            }
            UpdatesCheckerVersion version = UpdatesCheckerVersion.parse(m.group(2));
            if (version == null) {
                return unparseable();
            }
            parsed.add(new Clause(operatorOf(m.group(1)), version));
        }
        if (neverMatches(parsed)) {
            return unparseable();
        }
        return new UpdatesCheckerVersionRange(Collections.unmodifiableList(parsed), true);
    }

    private static Operator operatorOf(String symbol) {
        if (symbol == null || "=".equals(symbol)) {
            return Operator.EQ;
        }
        switch (symbol) {
            case "<":
                return Operator.LT;
            case "<=":
                return Operator.LE;
            case ">":
                return Operator.GT;
            default:
                return Operator.GE;
        }
    }

    /** Intersects the clauses into one lower and one upper bound and checks whether any version can satisfy both. */
    private static boolean neverMatches(List<Clause> clauses) {
        UpdatesCheckerVersion lower = null;
        boolean lowerInclusive = true;
        UpdatesCheckerVersion upper = null;
        boolean upperInclusive = true;
        for (Clause clause : clauses) {
            UpdatesCheckerVersion v = clause.version;
            boolean isLower = clause.operator == Operator.GT || clause.operator == Operator.GE;
            boolean isUpper = clause.operator == Operator.LT || clause.operator == Operator.LE;
            if (clause.operator == Operator.EQ) {
                isLower = true;
                isUpper = true;
            }
            boolean inclusive = clause.operator != Operator.GT && clause.operator != Operator.LT;
            if (isLower && (lower == null || v.compareTo(lower) > 0 || (v.equals(lower) && !inclusive))) {
                lower = v;
                lowerInclusive = inclusive;
            }
            if (isUpper && (upper == null || v.compareTo(upper) < 0 || (v.equals(upper) && !inclusive))) {
                upper = v;
                upperInclusive = inclusive;
            }
        }
        if (lower == null || upper == null) {
            return false;
        }
        int c = lower.compareTo(upper);
        return c > 0 || (c == 0 && !(lowerInclusive && upperInclusive));
    }

    /** @return {@code true} if the range was read exactly and can match at least one version. */
    public boolean isParseable() {
        return parseable;
    }

    /** @return whether the version satisfies every clause. Always {@code false} for an unparseable range. */
    public boolean matches(UpdatesCheckerVersion version) {
        if (!isParseable()) {
            return false;
        }
        for (Clause clause : clauses) {
            if (!clause.matches(version)) {
                return false;
            }
        }
        return true;
    }
}
