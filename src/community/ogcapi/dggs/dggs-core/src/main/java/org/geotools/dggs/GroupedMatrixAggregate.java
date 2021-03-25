/*
 *    GeoTools - The Open Source Java GIS Toolkit
 *    http://geotools.org
 *
 *    (C) 2020, Open Source Geospatial Foundation (OSGeo)
 *
 *    This library is free software; you can redistribute it and/or
 *    modify it under the terms of the GNU Lesser General Public
 *    License as published by the Free Software Foundation;
 *    version 2.1 of the License.
 *
 *    This library is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *    Lesser General Public License for more details.
 */
package org.geotools.dggs;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import org.geotools.data.CloseableIterator;
import org.geotools.feature.visitor.AbstractCalcResult;
import org.geotools.feature.visitor.Aggregate;
import org.geotools.feature.visitor.CalcResult;
import org.geotools.feature.visitor.FeatureAttributeVisitor;
import org.geotools.feature.visitor.FeatureCalc;
import org.opengis.feature.Feature;
import org.opengis.filter.expression.Expression;

public class GroupedMatrixAggregate implements FeatureCalc, FeatureAttributeVisitor {

    private final List<Aggregate> aggregates;
    private final List<Expression> variables;
    private final List<Expression> groupBy;
    private final Map<List<Object>, List<FeatureCalc>> calculators = new LinkedHashMap<>();
    private CalcResult result;

    public GroupedMatrixAggregate(
            List<Expression> variables, List<Aggregate> aggregates, List<Expression> groupBy) {
        this.variables = variables;
        this.aggregates = aggregates;
        this.groupBy = groupBy;
    }

    public void setResults(Map<List<Object>, List<Object>> results) {
        Map<List<Object>, List<CalcResult>> wrapped = new LinkedHashMap<>();
        results.entrySet()
                .stream()
                .forEach(e -> wrapped.put(e.getKey(), toCalcResults(e.getValue())));
        this.result = new MemoryResult(wrapped);
    }

    public void setResults(CalcResult result) {
        this.result = result;
    }

    private List<CalcResult> toCalcResults(List<Object> results) {
        int expectedSize = aggregates.size() * variables.size();
        if (results.size() != expectedSize)
            throw new IllegalArgumentException(
                    "Invalid results, size does not match expected: " + expectedSize);

        List<CalcResult> calcResults = new ArrayList<>();
        Iterator<Object> iterator = results.iterator();
        for (Expression variable : variables) {
            for (Aggregate aggregate : aggregates) {
                calcResults.add(aggregate.wrap(variable, iterator.next()));
            }
        }
        return calcResults;
    }

    public List<FeatureCalc> getCalculators() {
        List<FeatureCalc> calculators = new ArrayList<>();
        for (Expression variable : variables) {
            for (Aggregate aggregate : aggregates) {
                calculators.add(aggregate.create(variable));
            }
        }
        return calculators;
    }

    @Override
    public CalcResult getResult() {
        // If we have a iterable result est, it does not support merging,
        // use it as is without eventual merging with the in-memory computed part.
        // Having to run part optimized, part in memory, is an un-common situation,
        // those needing it can make an effort and implement it if they so desire.
        if (this.result instanceof IterableResult) return result;

        Map<List<Object>, List<CalcResult>> wrapped = new LinkedHashMap<>();
        calculators
                .entrySet()
                .stream()
                .forEach(e -> wrapped.put(e.getKey(), getResult(e.getValue())));
        MemoryResult computed = new MemoryResult(wrapped);
        if (this.result == null) return computed;
        else return computed.merge(this.result);
    }

    private List<CalcResult> getResult(List<FeatureCalc> calculators) {
        return calculators.stream().map(c -> c.getResult()).collect(Collectors.toList());
    }

    @Override
    public void visit(Feature feature) {
        List<Object> groupBy = evaluateGroupBy(feature);
        List<FeatureCalc> groupCalculators =
                calculators.computeIfAbsent(groupBy, k -> getCalculators());
        for (FeatureCalc calc : groupCalculators) {
            calc.visit(feature);
        }
    }

    private List<Object> evaluateGroupBy(Feature feature) {
        return groupBy.stream().map(e -> e.evaluate(feature)).collect(Collectors.toList());
    }

    @Override
    public List<Expression> getExpressions() {
        return Collections.unmodifiableList(variables);
    }

    /** Returns the list of expressions by which grouping should happen */
    public List<Expression> getGroupBy() {
        return Collections.unmodifiableList(groupBy);
    }

    /** Holds the grouping key and the associated values */
    public static class GroupByResult {
        List<Object> key;
        List<Object> values;

        public GroupByResult(List<Object> key, List<Object> values) {
            this.key = key;
            this.values = values;
        }

        public List<Object> getKey() {
            return key;
        }

        public void setKey(List<Object> key) {
            this.key = key;
        }

        public List<Object> getValues() {
            return values;
        }

        public void setValues(List<Object> values) {
            this.values = values;
        }
    }

    public static class IterableResult extends AbstractCalcResult
            implements IterableCalcResult<GroupByResult> {

        Supplier<CloseableIterator<GroupByResult>> supplier;

        public IterableResult(Supplier<CloseableIterator<GroupByResult>> supplier) {
            this.supplier = supplier;
        }

        @Override
        public Object getValue() {
            Map<List<Object>, List<Object>> value = new LinkedHashMap<>();
            try (CloseableIterator<GroupByResult> it = supplier.get()) {
                while (it.hasNext()) {
                    GroupByResult result = it.next();
                    value.put(result.getKey(), result.getValues());
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            return value;
        }

        @Override
        public CloseableIterator<GroupByResult> getIterator() {
            return supplier.get();
        }

        @Override
        public CalcResult merge(CalcResult resultsToAdd) {
            throw new UnsupportedOperationException("Merge was not implemented for this result");
        }
    }

    static class MemoryResult extends AbstractCalcResult {

        Map<List<Object>, List<CalcResult>> results;

        public MemoryResult(Map<List<Object>, List<CalcResult>> results) {
            this.results = results;
        }

        @Override
        public Object getValue() {
            Map<List<Object>, List<Object>> value = new LinkedHashMap<>();
            results.forEach((k, v) -> value.put(k, calcToValue(v)));
            return value;
        }

        private List<Object> calcToValue(List<CalcResult> calcResults) {
            return calcResults.stream().map(cr -> cr.getValue()).collect(Collectors.toList());
        }

        @Override
        public CalcResult merge(CalcResult resultsToAdd) {
            if (!(resultsToAdd instanceof MemoryResult))
                throw new IllegalArgumentException("Cannot merge this result: " + resultsToAdd);
            MemoryResult other = (MemoryResult) resultsToAdd;

            Map<List<Object>, List<CalcResult>> mergedMap = new LinkedHashMap<>();
            // scan first map and merge if neeeded
            results.entrySet()
                    .forEach(
                            e -> {
                                List<CalcResult> thisResults = e.getValue();
                                List<CalcResult> otherResults = other.results.get(e.getKey());
                                mergedMap.put(e.getKey(), mergeResults(thisResults, otherResults));
                            });
            // append all other results that do not match this map
            other.results
                    .entrySet()
                    .stream()
                    .filter(e -> results.get(e.getKey()) == null)
                    .forEach(e -> mergedMap.put(e.getKey(), e.getValue()));

            return new MemoryResult(mergedMap);
        }

        private List<CalcResult> mergeResults(
                List<CalcResult> thisResults, List<CalcResult> otherResults) {
            if (otherResults == null) return thisResults;
            if (otherResults.size() != thisResults.size())
                throw new IllegalArgumentException(
                        "Size of the two calc results do not match, should be "
                                + this.results.size());

            List<CalcResult> merged = new ArrayList<>();
            for (int i = 0; i < thisResults.size(); i++) {
                merged.add(thisResults.get(i).merge(otherResults.get(i)));
            }
            return merged;
        }
    }
}
