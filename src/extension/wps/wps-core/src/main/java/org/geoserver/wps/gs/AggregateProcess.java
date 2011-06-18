/* Copyright (c) 2001 - 2007 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wps.gs;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Set;

import org.geoserver.wps.WPSException;
import org.geoserver.wps.jts.DescribeParameter;
import org.geoserver.wps.jts.DescribeProcess;
import org.geoserver.wps.jts.DescribeResult;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.feature.visitor.AbstractCalcResult;
import org.geotools.feature.visitor.AverageVisitor;
import org.geotools.feature.visitor.CalcResult;
import org.geotools.feature.visitor.CountVisitor;
import org.geotools.feature.visitor.FeatureCalc;
import org.geotools.feature.visitor.MaxVisitor;
import org.geotools.feature.visitor.MedianVisitor;
import org.geotools.feature.visitor.MinVisitor;
import org.geotools.feature.visitor.StandardDeviationVisitor;
import org.geotools.feature.visitor.SumVisitor;
import org.geotools.process.ProcessException;
import org.geotools.util.NullProgressListener;
import org.opengis.feature.Feature;
import org.opengis.feature.type.AttributeDescriptor;
import org.opengis.util.ProgressListener;

/**
 * Will reproject the features to another CRS. Can also be used to force a known CRS onto a dataset
 * that does not have ones
 * 
 * @author Andrea Aime
 */
@DescribeProcess(title = "aggregateProcess", description = "Computes various attribute statistics over vector data sets")
public class AggregateProcess implements GeoServerProcess {
    // the functions this process can handle
    public enum AggregationFunction {
        Count, Average, Max, Median, Min, StdDev, Sum;
    }

    @DescribeResult(name = "result", description = "The reprojected features")
    public Results execute(
            @DescribeParameter(name = "features", description = "The feature collection that will be aggregate") SimpleFeatureCollection features,
            @DescribeParameter(name = "aggregationAttribute", min = 0, description = "The attribute used for aggregation") String aggAttribute,
            @DescribeParameter(name = "function", description = "The aggregation functions to be used", collectionType = AggregationFunction.class) Set<AggregationFunction> functions,
            @DescribeParameter(name = "singlePass", description = "If all the results should be computed in a single pass (will break DBMS specific optimizations)", min = 0) Boolean singlePass,
            ProgressListener progressListener) throws Exception {

        int attIndex = -1;
        List<AttributeDescriptor> atts = features.getSchema().getAttributeDescriptors();
        for (int i = 0; i < atts.size(); i++) {
            if (atts.get(i).getLocalName().equals(aggAttribute)) {
                attIndex = i;
                break;
            }
        }

        if (attIndex == -1) {
            throw new ProcessException("Could not find attribute " + atts
                    + " the valid values are " + attNames(atts));
        }

        List<AggregationFunction> functionList = new ArrayList<AggregationFunction>(functions);
        List<FeatureCalc> visitors = new ArrayList<FeatureCalc>();

        for (AggregationFunction function : functionList) {
            FeatureCalc calc;
            if (function == AggregationFunction.Average) {
                calc = new AverageVisitor(attIndex, features.getSchema());
            } else if (function == AggregationFunction.Count) {
                calc = new CountVisitor();
            } else if (function == AggregationFunction.Max) {
                calc = new MaxVisitor(attIndex, features.getSchema());
            } else if (function == AggregationFunction.Median) {
                calc = new MedianVisitor(attIndex, features.getSchema());
            } else if (function == AggregationFunction.Min) {
                calc = new MinVisitor(attIndex, features.getSchema());
            } else if (function == AggregationFunction.StdDev) {
                calc = new StandardDeviationVisitor(CommonFactoryFinder.getFilterFactory(null).property(aggAttribute));
            } else if (function == AggregationFunction.Sum) {
                calc = new SumVisitor(attIndex, features.getSchema());
            } else {
                throw new WPSException("Uknown method " + function);
            }
            visitors.add(calc);
        }

        EnumMap<AggregationFunction, Number> results = new EnumMap<AggregationFunction, Number>(AggregationFunction.class);
        if (singlePass != null && singlePass) {
            AggregateFeatureCalc calc = new AggregateFeatureCalc(visitors);
            features.accepts(calc, new NullProgressListener());
            List<CalcResult> resultList = (List<CalcResult>) calc.getResult().getValue();
            for (int i = 0; i < functionList.size(); i++) {
                CalcResult result = resultList.get(i);
                if(result != null) {
                    results.put(functionList.get(i), (Number) result.getValue());
                }
            }
        } else {
            for (int i = 0; i < functionList.size(); i++) {
                final FeatureCalc calc = visitors.get(i);
                features.accepts(calc, new NullProgressListener());
                results.put(functionList.get(i), (Number) calc.getResult().getValue());
            }
        }

        return new Results(results);
    }

    private List<String> attNames(List<AttributeDescriptor> atts) {
        List<String> result = new ArrayList<String>();
        for (AttributeDescriptor ad : atts) {
            result.add(ad.getLocalName());
        }
        return result;
    }

    /**
     * Runs various {@link FeatureCalc} in a single pass
     * 
     * @author Andrea Aime - GeoSolutions
     */
    static class AggregateFeatureCalc implements FeatureCalc {
        List<FeatureCalc> delegates;

        public AggregateFeatureCalc(List<FeatureCalc> delegates) {
            super();
            this.delegates = delegates;
        }

        public CalcResult getResult() {
            final List<CalcResult> results = new ArrayList<CalcResult>();
            for (FeatureCalc delegate : delegates) {
                    results.add(delegate.getResult());
            }

            return new AbstractCalcResult() {
                @Override
                public Object getValue() {
                    return results;
                }
            };
        }

        public void visit(Feature feature) {
            for (FeatureCalc delegate : delegates) {
                delegate.visit(feature);
            }
        }
    }

    /**
     * The aggregate function results
     */
    public static final class Results {
        Double min;
        Double max;
        Double median;
        Double average;
        Double standardDeviation;
        Double sum;
        Long count;
        
        public Results(EnumMap<AggregationFunction, Number> results) {
            min = toDouble(results.get(AggregationFunction.Min));
            max = toDouble(results.get(AggregationFunction.Max));
            median = toDouble(results.get(AggregationFunction.Median));
            average = toDouble(results.get(AggregationFunction.Average));
            standardDeviation = toDouble(results.get(AggregationFunction.StdDev));
            sum = toDouble(results.get(AggregationFunction.Sum));
            Number nc = results.get(AggregationFunction.Count);
            if(nc != null) {
                count = nc.longValue();
            }
        }
        
        Double toDouble(Number number) {
            if(number == null) {
                return null;
            } else {
                return number.doubleValue();
            }
        }
        
        public Double getMin() {
            return min;
        }
        public Double getMax() {
            return max;
        }
        public Double getMedian() {
            return median;
        }
        public Double getAverage() {
            return average;
        }
        public Double getStandardDeviation() {
            return standardDeviation;
        }
        public Double getSum() {
            return sum;
        }

        public Long getCount() {
            return count;
        }

    }

}
