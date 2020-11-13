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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;
import org.geotools.feature.visitor.AbstractCalcResult;
import org.geotools.feature.visitor.Aggregate;
import org.geotools.feature.visitor.CalcResult;
import org.geotools.feature.visitor.FeatureAttributeVisitor;
import org.geotools.feature.visitor.FeatureCalc;
import org.opengis.feature.Feature;
import org.opengis.filter.expression.Expression;

public class MatrixAggregate implements FeatureCalc, FeatureAttributeVisitor {

    private final List<Aggregate> aggregates;
    private final List<Expression> variables;
    private final List<FeatureCalc> calculators = new ArrayList<>();
    private MatrixAggregateResult result;

    public MatrixAggregate(List<Expression> variables, List<Aggregate> aggregates) {
        this.variables = variables;
        this.aggregates = aggregates;
        for (Expression variable : variables) {
            for (Aggregate aggregate : aggregates) {
                calculators.add(aggregate.create(variable));
            }
        }
    }

    public void setResults(List<Object> results) {
        if (results.size() != calculators.size())
            throw new IllegalArgumentException(
                    "Invalid results, size does not match expected: " + calculators.size());

        List<CalcResult> resultList = new ArrayList<>();
        Iterator<Object> iterator = results.iterator();
        for (Expression variable : variables) {
            for (Aggregate aggregate : aggregates) {
                resultList.add(aggregate.wrap(variable, iterator.next()));
            }
        }

        this.result = new MatrixAggregateResult(resultList);
    }

    public List<FeatureCalc> getCalculators() {
        return Collections.unmodifiableList(calculators);
    }

    @Override
    public CalcResult getResult() {
        MatrixAggregateResult inMemoryResults =
                new MatrixAggregateResult(
                        calculators.stream().map(c -> c.getResult()).collect(Collectors.toList()));
        if (result == null) return inMemoryResults;
        else return inMemoryResults.merge(result);
    }

    @Override
    public void visit(Feature feature) {
        for (FeatureCalc calc : calculators) {
            calc.visit(feature);
        }
    }

    @Override
    public List<Expression> getExpressions() {
        return Collections.unmodifiableList(variables);
    }

    static class MatrixAggregateResult extends AbstractCalcResult {

        List<CalcResult> results;

        public MatrixAggregateResult(List<CalcResult> results) {
            this.results = results;
        }

        @Override
        public Object getValue() {
            return results.stream().map(r -> r.getValue()).collect(Collectors.toList());
        }

        @Override
        public CalcResult merge(CalcResult resultsToAdd) {
            if (!(resultsToAdd instanceof MatrixAggregateResult))
                throw new IllegalArgumentException("Cannot merge this result: " + resultsToAdd);
            MatrixAggregateResult other = (MatrixAggregateResult) resultsToAdd;
            if (other.results.size() != this.results.size()) {
                throw new IllegalArgumentException(
                        "Size of the two calc results do not match, should be "
                                + this.results.size());
            }
            List<CalcResult> merged = new ArrayList<>();
            for (int i = 0; i < this.results.size(); i++) {
                merged.add(this.results.get(i).merge(other.results.get(i)));
            }
            return new MatrixAggregateResult(merged);
        }
    }
}
