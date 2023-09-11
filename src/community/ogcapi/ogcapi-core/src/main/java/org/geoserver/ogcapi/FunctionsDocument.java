/* (c) 2019 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.ogcapi;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import org.geotools.api.filter.capability.FunctionName;
import org.geotools.api.parameter.Parameter;
import org.geotools.filter.FunctionFinder;
import org.locationtech.jts.geom.Geometry;

/** A document enumerating the available functions */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class FunctionsDocument {

    public static final String REL = "http://www.opengis.net/def/rel/ogc/1.0/functions";

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class Argument {
        String title;
        String description;
        AttributeType[] type;

        public Argument(String title, String description, AttributeType[] type) {
            this.title = title;
            this.description = description;
            this.type = type;
        }

        public String getTitle() {
            return title;
        }

        public String getDescription() {
            return description;
        }

        public AttributeType[] getType() {
            return type;
        }
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class Function {
        String name;
        String description;
        Argument returns;
        List<Argument> arguments;

        Function(FunctionName fn) {
            this.name = fn.getName();
            this.returns = toParameter(fn.getReturn());
            this.description = null; // no support for descriptions in GeoTools functions
            this.arguments =
                    fn.getArguments().stream()
                            .map(FunctionsDocument::toParameter)
                            .collect(Collectors.toList());
        }

        public String getName() {
            return name;
        }

        public Argument getReturns() {
            return returns;
        }

        public List<Argument> getArguments() {
            return arguments;
        }
    }

    private static Argument toParameter(Parameter<?> parameter) {
        return new Argument(
                parameter.getName(),
                Optional.ofNullable(parameter.getDescription()).map(d -> d.toString()).orElse(null),
                new AttributeType[] {AttributeType.fromClass(parameter.getType())});
    }

    List<Function> functions;

    public FunctionsDocument() {
        functions =
                new FunctionFinder(null)
                        .getAllFunctionDescriptions().stream()
                                .filter(FunctionsDocument::isSimpleFunction)
                                .map(Function::new)
                                .distinct()
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
                    && !Boolean.class.isAssignableFrom(type)) {
                return false;
            }
        }

        // no real way to identify functions that are meant to work against a FeatureCollection
        // rather than a Feature

        // no complex parameter found, returning
        return true;
    }

    public List<Function> getFunctions() {
        return functions;
    }
}
