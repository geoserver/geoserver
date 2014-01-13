/* Copyright (c) 2001 - 2013 OpenPlans - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms.dimension;

import java.util.Date;

import org.geotools.factory.CommonFactoryFinder;
import org.geotools.feature.visitor.AbstractCalcResult;
import org.geotools.feature.visitor.CalcResult;
import org.geotools.feature.visitor.FeatureCalc;
import org.opengis.filter.FilterFactory;
import org.opengis.filter.expression.Expression;

public class NearestVisitor<T> implements FeatureCalc {
    private Expression expr;

    private T nearestValueFound;

    private T matchedValue;

    double shortestDistance = Double.MAX_VALUE;

    public NearestVisitor(String attributeTypeName, T valueToMatch) {
        FilterFactory factory = CommonFactoryFinder.getFilterFactory(null);
        expr = factory.property(attributeTypeName);
        this.matchedValue = valueToMatch;
    }

    /**
     * Visitor function, which looks at each feature and finds the value of the attribute given attribute nearest to the given comparison value.
     * 
     * @param feature the feature to be visited
     */
    @SuppressWarnings("unchecked")
    public void visit(org.opengis.feature.Feature feature) {
        Object attribValue = expr.evaluate(feature);

        if (attribValue == null) {
            return;
        }

        if (attribValue instanceof Number) {
            double doubleVal = ((Number) attribValue).doubleValue();

            if (Double.isNaN(doubleVal) || Double.isInfinite(doubleVal)) {
                return;
            }
            if (this.matchedValue instanceof Number) {
                double toMatch = ((Number) this.matchedValue).doubleValue();
                if (Math.abs(toMatch - doubleVal) < this.shortestDistance) {
                    this.shortestDistance = Math.abs(toMatch - doubleVal);
                    this.nearestValueFound = (T) attribValue;
                }
            } else {
                return;
            }
        } else if ((attribValue instanceof Date) && (this.matchedValue instanceof Date)) {
            long time = ((Date) attribValue).getTime();
            if (Math.abs(time - ((Date) this.matchedValue).getTime()) < this.shortestDistance) {
                this.shortestDistance = Math.abs(time - ((Date) this.matchedValue).getTime());
                this.nearestValueFound = (T) attribValue;
            }

        } else if ((attribValue instanceof String) && (this.matchedValue instanceof String)) {
            String s = (String) attribValue;
            if (Math.abs(s.compareTo((String) this.matchedValue)) < this.shortestDistance) {
                this.shortestDistance = Math.abs(s.compareTo((String) this.matchedValue));
                this.nearestValueFound = (T) attribValue;
            }
        }
    }

    public T getNearestMatch() throws IllegalStateException {
        return this.nearestValueFound;
    }

    @Override
    public CalcResult getResult() {
        return new AbstractCalcResult() {
            @Override
            public Object getValue() {
                return NearestVisitor.this.nearestValueFound;
            }
        };
    }

}