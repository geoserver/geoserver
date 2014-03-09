/* Copyright (c) 2001 - 2014 OpenPlans - www.openplans.org. All rights reserved.
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

public class NearestVisitorFactory {


    public static FeatureCalc getNearestVisitor(String attributeName, Object valueToMatch, Class<?> clz){
        FeatureCalc retval = null;
        if (Date.class.isAssignableFrom(clz)){
            retval = new NearestDateVisitor(attributeName, (Date)valueToMatch);
        }
        else if (Number.class.isAssignableFrom(clz)){
            retval = new NearestNumberVisitor(attributeName, (Number)valueToMatch);
        }
        else {
            retval = new NearestStringVisitor(attributeName, valueToMatch.toString());
        }
        return retval;
    }
    
    private static abstract class AbstractNearestVisitor<T> implements FeatureCalc {
        protected Expression expr;

        protected T nearestValueFound;

        protected double shortestDistance = Double.MAX_VALUE;
        
        protected double matchedValue;
        
        public AbstractNearestVisitor(String attributeName){
            FilterFactory factory = CommonFactoryFinder.getFilterFactory(null);
            this.expr = factory.property(attributeName);
        }               

        @Override
        public CalcResult getResult() {
            return new AbstractCalcResult() {
                @Override
                public Object getValue() {
                    return AbstractNearestVisitor.this.nearestValueFound;
                }
            };
        }        
    }

    private static class NearestDateVisitor extends AbstractNearestVisitor<Date> {

        /**
         * Creates a NearestVisitor instance for the given attribute and a Date value to match.
         * 
         * @param attributeName
         * @param valueToMatch
         */
        public NearestDateVisitor(String attributeName, Date valueToMatch) {
            super(attributeName);
            this.matchedValue = (double)valueToMatch.getTime();
        }

        /**
         * Visitor function, which looks at each feature and finds the value of the 
         * attribute given attribute nearest to the given comparison value.
         * 
         * @param feature the feature to be visited
         */
        public void visit(org.opengis.feature.Feature feature) {
            Date attribValue = (Date) expr.evaluate(feature);
            if (attribValue == null) {
                return;
            }
            double time = (double) attribValue.getTime();
            if (Math.abs(time - this.matchedValue) < this.shortestDistance) {
                this.shortestDistance = Math.abs(time - this.matchedValue);
                this.nearestValueFound = attribValue;
            }
        }
    }
    
    private static class NearestNumberVisitor extends AbstractNearestVisitor<Number> {    

        /**
         * Creates a NearestVisitor instance for the given attribute and a Number value to match.
         * 
         * @param attributeName
         * @param valueToMatch
         */
        public NearestNumberVisitor(String attributeName, Number valueToMatch) {
            super(attributeName);
            this.matchedValue = valueToMatch.doubleValue();
        }

        /**
         * Visitor function, which looks at each feature and finds the value of the 
         * attribute given attribute nearest to the given comparison value.
         * 
         * @param feature the feature to be visited
         */
        public void visit(org.opengis.feature.Feature feature) {
            Number attribValue = (Number) expr.evaluate(feature);
            if (attribValue == null) {
                return;
            }
            double val = attribValue.doubleValue();
            if (Math.abs(val - this.matchedValue) < this.shortestDistance) {
                this.shortestDistance = Math.abs(val - this.matchedValue);
                this.nearestValueFound = attribValue;
            }
        }
    }
    
    private static class NearestStringVisitor extends AbstractNearestVisitor<String> {    

        /**
         * Creates a NearestVisitor instance for the given attribute and a String value to match.
         * 
         * @param attributeName
         * @param valueToMatch
         */
        public NearestStringVisitor(String attributeName, String valueToMatch) {
            super(attributeName);
            this.matchedValue = valueToMatch.compareTo("");
        }

        /**
         * Visitor function, which looks at each feature and finds the value of the 
         * attribute given attribute nearest to the given comparison value.
         * 
         * @param feature the feature to be visited
         */
        public void visit(org.opengis.feature.Feature feature) {
            String attribValue = (String) expr.evaluate(feature);
            if (attribValue == null) {
                return;
            }
            double val = (double)attribValue.compareTo("");
            if (Math.abs(val - this.matchedValue) < this.shortestDistance) {
                this.shortestDistance = Math.abs(val - this.matchedValue);
                this.nearestValueFound = attribValue;
            }
        }
    }
    
}