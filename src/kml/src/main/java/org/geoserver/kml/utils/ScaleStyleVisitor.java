/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.kml.utils;

import java.util.ArrayList;
import java.util.List;
import org.geotools.feature.FeatureTypes;
import org.geotools.styling.FeatureTypeStyle;
import org.geotools.styling.FeatureTypeStyleImpl;
import org.geotools.styling.Rule;
import org.geotools.styling.Style;
import org.geotools.styling.visitor.DuplicatingStyleVisitor;
import org.opengis.feature.simple.SimpleFeatureType;

/**
 * Returns a shallow copy of a style with only the active rules at the specified scale denominator
 *
 * @author Andrea Aime - GeoSolutions
 */
public class ScaleStyleVisitor extends DuplicatingStyleVisitor {

    /** Tolerance used to compare doubles for equality */
    static final double TOLERANCE = 1e-6;

    double scaleDenominator;

    SimpleFeatureType schema;

    public ScaleStyleVisitor(double scaleDenominator, SimpleFeatureType schema) {
        this.scaleDenominator = scaleDenominator;
        this.schema = schema;
    }

    @Override
    public void visit(Style style) {
        super.visit(style);
        Style copy = (Style) pages.peek();

        List<FeatureTypeStyle> filtered = new ArrayList<FeatureTypeStyle>();
        for (FeatureTypeStyle fts : copy.featureTypeStyles()) {
            // do the same filtering as streaming renderer
            if (fts.featureTypeNames().isEmpty()
                    || fts.featureTypeNames()
                            .stream()
                            .anyMatch(tn -> FeatureTypes.matches(schema, tn))) {
                filtered.add(fts);
            }
        }
        copy.featureTypeStyles().clear();
        copy.featureTypeStyles().addAll(filtered);
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
        copy.rules().clear();
        copy.rules().addAll(rulesCopy);
        pages.push(copy);
    }

    public Style getSimplifiedStyle() {
        return (Style) getCopy();
    }

    @Override
    public Style getCopy() {
        return (Style) super.getCopy();
    }
}
