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
 * One {@code vulnerable_versions} string of an advisory: comma-separated clauses that must all hold, each an optional
 * operator ({@code <}, {@code <=}, {@code >}, {@code >=}, {@code =}) and a {@code MAJOR.MINOR.PATCH} version.
 *
 * <p>Anything not read exactly (a commit SHA, an empty string, an unknown operator) is <em>unparseable</em>, and so is
 * a range no version can satisfy. The caller reports an unparseable range for manual review, never as "not affected".
 */
public final class VersionRange {

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

        private final Version version;

        private Clause(Operator operator, Version version) {
            this.operator = operator;
            this.version = version;
        }

        private boolean matches(Version candidate) {
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

    private VersionRange(List<Clause> clauses, boolean parseable) {
        this.clauses = clauses;
        this.parseable = parseable;
    }

    private static VersionRange unparseable() {
        return new VersionRange(Collections.<Clause>emptyList(), false);
    }

    /** Never throws: an unreadable string gives an unparseable range. */
    public static VersionRange parse(String text) {
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
            Version version = Version.parse(m.group(2));
            if (version == null) {
                return unparseable();
            }
            parsed.add(new Clause(operatorOf(m.group(1)), version));
        }
        if (neverMatches(parsed)) {
            return unparseable();
        }
        return new VersionRange(Collections.unmodifiableList(parsed), true);
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

    /** @return whether no version can satisfy every clause */
    private static boolean neverMatches(List<Clause> clauses) {
        Version lower = null;
        boolean lowerInclusive = true;
        Version upper = null;
        boolean upperInclusive = true;
        for (Clause clause : clauses) {
            Version v = clause.version;
            boolean isLower = clause.operator == Operator.GT || clause.operator == Operator.GE;
            boolean isUpper = clause.operator == Operator.LT || clause.operator == Operator.LE;
            if (clause.operator == Operator.EQ) {
                isLower = true;
                isUpper = true;
            }
            boolean inclusive = clause.operator != Operator.GT && clause.operator != Operator.LT;
            if (isLower && (lower == null || v.compareTo(lower) > 0 || (v.compareTo(lower) == 0 && !inclusive))) {
                lower = v;
                lowerInclusive = inclusive;
            }
            if (isUpper && (upper == null || v.compareTo(upper) < 0 || (v.compareTo(upper) == 0 && !inclusive))) {
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

    /** @return whether the range was read exactly and can match some version */
    public boolean isParseable() {
        return parseable;
    }

    /** @return whether the version satisfies every clause; always {@code false} when unparseable */
    public boolean matches(Version version) {
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
