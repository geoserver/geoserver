/* (c) 2019 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.api.features;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;
import org.geoserver.api.AttributeType;
import org.geotools.filter.FunctionFinder;
import org.locationtech.jts.geom.Geometry;
import org.opengis.filter.capability.FunctionName;
import org.opengis.parameter.Parameter;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class FilterCapabilitiesDocument {

    public static final String CORE = "http://www.opengis.net/spec/cql/1.0/conf/core";
    public static final String SPATIAL = "http://www.opengis.net/spec/cql/1.0/conf/spatial";
    public static final String TEMPORAL = "http://www.opengis.net/spec/cql/1.0/conf/temporal";

    public static class Capability {
        String name;
        List<String> operators;

        public Capability(String name, List<String> operators) {
            this.name = name;
            this.operators = operators;
        }

        public Capability(String name, String... operators) {
            this.name = name;
            this.operators = Arrays.asList(operators);
        }

        public String getName() {
            return name;
        }

        public List<String> getOperators() {
            return operators;
        }
    }

    public static class FunctionParameter {
        String name;
        AttributeType type;

        public FunctionParameter(String name, AttributeType type) {
            this.name = name;
            this.type = type;
        }

        public String getName() {
            return name;
        }

        public AttributeType getType() {
            return type;
        }
    }

    public static class Function {
        String name;
        FunctionParameter returns;
        List<FunctionParameter> arguments;

        Function(FunctionName fn) {
            this.name = fn.getName();
            this.returns = toParameter(fn.getReturn());
            this.arguments =
                    fn.getArguments()
                            .stream()
                            .map(FilterCapabilitiesDocument::toParameter)
                            .collect(Collectors.toList());
        }

        public String getName() {
            return name;
        }

        public FunctionParameter getReturns() {
            return returns;
        }

        public List<FunctionParameter> getArguments() {
            return arguments;
        }
    }

    private static FunctionParameter toParameter(Parameter<?> parameter) {
        return new FunctionParameter(
                parameter.getName(), AttributeType.fromClass(parameter.getType()));
    }

    List<String> conformanceClasses;
    List<Capability> capabilities;
    List<Function> functions;

    public FilterCapabilitiesDocument() {
        conformanceClasses = Arrays.asList(CORE, SPATIAL, TEMPORAL);
        capabilities = new ArrayList<>();
        capabilities.add(new Capability("logical", "and", "or", "not"));
        capabilities.add(
                new Capability(
                        "comparison",
                        "lt",
                        "lte",
                        "gt",
                        "gte",
                        "gt",
                        "neq",
                        "like",
                        "between",
                        "in"));
        capabilities.add(
                new Capability(
                        "spatial",
                        "equals",
                        "disjoint",
                        "touches",
                        "within",
                        "overlaps",
                        "crosses",
                        "intersects",
                        "contains"));
        capabilities.add(
                new Capability(
                        "temporal",
                        "after",
                        "before",
                        "begins",
                        "begunby",
                        "tcontains",
                        "during",
                        "endedby",
                        "ends",
                        "tequals",
                        "meets",
                        "metby",
                        "toverlaps",
                        "overlappedby",
                        "anyinteracts",
                        "intersects"));
        capabilities.add(new Capability("arithmetic", "+", "-", "*", "/"));

        functions = new ArrayList<>();
        functions =
                new FunctionFinder(null)
                        .getAllFunctionDescriptions()
                        .stream()
                        .filter(FilterCapabilitiesDocument::isSimpleFunction)
                        .map(Function::new)
                        .collect(Collectors.toList());
    }

    private static boolean isSimpleFunction(FunctionName functionName) {
        for (Parameter<?> p : functionName.getArguments()) {
            Class<?> type = p.getType();
            if (type == null) {
                return false;
            }
            if (!CharSequence.class.isAssignableFrom(type)
                    && !Number.class.isAssignableFrom(type)
                    && !Date.class.isAssignableFrom(type)
                    && !Geometry.class.isAssignableFrom(type)
                    && !org.opengis.geometry.Geometry.class.isAssignableFrom(type)
                    && !Boolean.class.isAssignableFrom(type)) {
                return false;
            }
        }

        // no real way to identify functions that are meant to work against a FeatureCollection
        // rather than a Feature

        // no complex parameter found, returning
        return true;
    }

    public List<String> getConformanceClasses() {
        return conformanceClasses;
    }

    public void setConformanceClasses(List<String> conformanceClasses) {
        this.conformanceClasses = conformanceClasses;
    }

    public List<Capability> getCapabilities() {
        return capabilities;
    }

    public void setCapabilities(List<Capability> capabilities) {
        this.capabilities = capabilities;
    }

    public List<Function> getFunctions() {
        return functions;
    }
}
