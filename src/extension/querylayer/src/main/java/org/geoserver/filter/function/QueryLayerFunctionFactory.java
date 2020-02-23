/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.filter.function;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.geoserver.catalog.Catalog;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.feature.NameImpl;
import org.geotools.filter.FunctionFactory;
import org.geotools.util.logging.Logging;
import org.opengis.feature.type.Name;
import org.opengis.filter.FilterFactory;
import org.opengis.filter.capability.FunctionName;
import org.opengis.filter.expression.Expression;
import org.opengis.filter.expression.Function;
import org.opengis.filter.expression.Literal;

/**
 * Factory for the functions that do query the GeoServer catalog as well as the support ones used to
 * mix them into larger filters
 *
 * @author Andrea Aime - GeoSolutions
 */
public class QueryLayerFunctionFactory implements FunctionFactory {
    static final Name COLLECT_GEOMETRIES = new NameImpl("collectGeometries");

    static final Name QUERY_COLLECTION = new NameImpl("queryCollection");

    static final Name QUERY_SINGLE = new NameImpl("querySingle");

    static final Logger LOGGER = Logging.getLogger(QueryLayerFunctionFactory.class);

    List<FunctionName> functionNames;

    Catalog catalog;

    int maxFeatures = 1000;

    long maxCoordinates = 1024 * 1024 / 28; // this results 1MB of Coordinate object max

    public QueryLayerFunctionFactory() {
        FilterFactory ff = CommonFactoryFinder.getFilterFactory(null);
        List<FunctionName> names = new ArrayList<FunctionName>();
        names.add(ff.functionName(QUERY_SINGLE, -1)); // 2 or 3 args
        names.add(ff.functionName(QUERY_COLLECTION, -1)); // 2 or 3 args
        names.add(ff.functionName(COLLECT_GEOMETRIES, 1));
        functionNames = Collections.unmodifiableList(names);
    }

    public long getMaxFeatures() {
        return maxFeatures;
    }

    /** Sets the max number of features returned by a free query */
    public void setMaxFeatures(int maxFeatures) {
        if (maxFeatures <= 0) {
            throw new IllegalArgumentException(
                    "The max features retrieved by a query layer "
                            + "function must be a positive number");
        }
        this.maxFeatures = maxFeatures;
    }

    /**
     * Sets the maximum number of coordinates to be collected, a non positive value implies no limit
     */
    public void setMaxCoordinates(long maxCoordinates) {
        this.maxCoordinates = maxCoordinates;
    }

    /** Initializes the catalog reference, without it the factory won't generate any function */
    public void setCatalog(Catalog catalog) {
        this.catalog = catalog;
    }

    public Function function(String name, List<Expression> args, Literal fallback) {
        return function(new NameImpl(name), args, fallback);
    }

    @Override
    public Function function(Name name, List<Expression> args, Literal fallback) {
        if (!isInitialized()) {
            return null;
        }

        if (QUERY_SINGLE.equals(name)) {
            return new QueryFunction(QUERY_SINGLE, catalog, args, fallback, true, 1);
        } else if (QUERY_COLLECTION.equals(name)) {
            return new QueryFunction(QUERY_COLLECTION, catalog, args, fallback, false, maxFeatures);
        } else if (COLLECT_GEOMETRIES.equals(name)) {
            return new CollectGeometriesFunction(
                    COLLECT_GEOMETRIES, args, fallback, maxCoordinates);
        } else {
            return null;
        }
    }

    public List<FunctionName> getFunctionNames() {
        if (isInitialized()) {
            return functionNames;
        } else {
            return Collections.emptyList();
        }
    }

    private boolean isInitialized() {
        if (catalog == null) {
            LOGGER.log(
                    Level.INFO,
                    "Looking for functions but the catalog still "
                            + "has not been set into QueryLayerFunctionFactory");
            return false;
        } else {
            return true;
        }
    }
}
