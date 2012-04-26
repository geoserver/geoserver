package org.geoserver.gss.impl.query;

import java.util.Set;
import java.util.TreeSet;

public enum TemporalOp {
    /** */
    After(false),
    /** */
    Before(false),
    /** */
    Begins(false),
    /** */
    BegunBy(false),
    /** */
    TContains(true),
    /** */
    During(true),
    /** */
    EndedBy(false),
    /** */
    Ends(false),
    /** */
    TEquals(false),
    /** */
    Meets(false),
    /** */
    MetBy(false),
    /** */
    TOverlaps(true),
    /** */
    OverlappedBy(true);

    private boolean requiresPeriod;

    private TemporalOp(boolean requiresPeriod) {
        this.requiresPeriod = requiresPeriod;
    }

    public boolean requiresPeriod() {
        return this.requiresPeriod;
    }

    public static Set<TemporalOp> periodRelated() {
        Set<TemporalOp> periodRelated = new TreeSet<TemporalOp>();
        for (TemporalOp op : TemporalOp.values()) {
            if (op.requiresPeriod()) {
                periodRelated.add(op);
            }
        }
        return periodRelated;
    }

    public static Set<TemporalOp> instantRelated() {
        Set<TemporalOp> instantRelated = new TreeSet<TemporalOp>();
        for (TemporalOp op : TemporalOp.values()) {
            if (!op.requiresPeriod()) {
                instantRelated.add(op);
            }
        }
        return instantRelated;
    }

}
