/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.filter.function;

import java.util.List;
import org.geotools.api.feature.type.Name;
import org.geotools.api.filter.expression.Expression;
import org.geotools.api.filter.expression.Literal;
import org.geotools.filter.FunctionImpl;
import org.geotools.filter.capability.FunctionNameImpl;
import org.geotools.geometry.jts.GeometryCollector;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryCollection;
import org.locationtech.jts.geom.GeometryFactory;

/**
 * Collects all geometries provided in a list into a single {@link GeometryCollection} object (a
 * type specific subclass of it if possible)
 *
 * @author Andrea Aime - GeoSolutions
 */
public class CollectGeometriesFunction extends FunctionImpl {

    long maxCoordinates;

    public CollectGeometriesFunction(
            Name name, List<Expression> args, Literal fallback, long maxCoordinates) {
        if (args == null || args.size() != 1) {
            throw new IllegalArgumentException(
                    "CollectGeometries function requires a single"
                            + " argument, a collection of geometries");
        }

        functionName = new FunctionNameImpl(name, args.size());
        setName(name.getLocalPart());
        setFallbackValue(fallback);
        setParameters(args);
        this.maxCoordinates = maxCoordinates;
    }

    @Override
    public Object evaluate(Object object) {
        List geometries = getParameters().get(0).evaluate(object, List.class);
        if (geometries == null || geometries.isEmpty()) {
            return new GeometryCollection(null, new GeometryFactory());
        }

        // collect but don't clone, unfortunately we're already stuck with a list, by cloning
        // we'd just increase memory usage
        GeometryCollector collector = new GeometryCollector();
        collector.setFactory(null);
        collector.setMaxCoordinates(maxCoordinates);
        for (Object o : geometries) {
            Geometry geometry = (Geometry) o;
            collector.add(geometry);
        }

        return collector.collect();
    }
}
