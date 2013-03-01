/* Copyright (c) 2013 OpenPlans - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.kml;

import java.util.ArrayList;
import java.util.List;

import org.geotools.styling.FeatureTypeStyle;
import org.geotools.styling.FeatureTypeStyleImpl;
import org.geotools.styling.Rule;
import org.geotools.styling.visitor.DuplicatingStyleVisitor;

/**
 * Returns a shallow copy of a style with only the active rules at the specified scale denominator
 * 
 * @author Andrea Aime - GeoSolutions
 * 
 */
public class ScaleStyleVisitor extends DuplicatingStyleVisitor {

    /**
     * Tolerance used to compare doubles for equality
     */
    static final double TOLERANCE = 1e-6;

    double scaleDenominator;

    public ScaleStyleVisitor(double scaleDenominator) {
        this.scaleDenominator = scaleDenominator;
    }

    @Override
    public void visit(FeatureTypeStyle fts) {
        FeatureTypeStyle copy = new FeatureTypeStyleImpl((FeatureTypeStyleImpl) fts);

        // preserve only the rules active at this scale range
        List<Rule> rulesCopy = new ArrayList<Rule>();
        for (Rule r : fts.rules()) {
            if (((r.getMinScaleDenominator() - TOLERANCE) <= scaleDenominator)
                    && ((r.getMaxScaleDenominator() + TOLERANCE) > scaleDenominator)) {
                rulesCopy.add(r);
            }

        }
        copy.rules().addAll(rulesCopy);
        pages.push(copy);
    }
}
