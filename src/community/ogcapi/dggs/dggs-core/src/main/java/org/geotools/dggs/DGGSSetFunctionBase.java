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

import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;
import org.geotools.filter.FunctionImpl;
import org.opengis.filter.capability.FunctionName;
import org.opengis.filter.expression.Expression;
import org.opengis.filter.expression.Literal;
import org.opengis.parameter.Parameter;

/**
 * Base class for DGGS functions based on set evaluation, e.g., neighbors, children, parents. This
 * implementation assumes that:
 *
 * <ul>
 *   <li>The first parameter is a zoneId, which may vary call by call
 *   <li>The last parameter is a DGGSIntance
 *   <li>The function is considered stable if all but the zoneId parameter are literals
 * </ul>
 */
public abstract class DGGSSetFunctionBase extends FunctionImpl implements DGGSSetFunction {

    /** Cache of matching zone ids to be used in case the function is stable */
    protected Set<String> zoneIds;

    int dggsParameterIndex = -1;
    private int cacheLimit =
            Integer.parseInt(System.getProperty("dggs.function.cache.limit", "10000"));
    private int iterationLimit =
            Integer.parseInt(System.getProperty("dggs.function.iteration.limit", "50000"));
    private Boolean cacheTooBig;

    public DGGSSetFunctionBase(FunctionName functionName) {
        this.functionName = functionName;
        lookupDGGSParameterIndex(functionName);
    }

    public void lookupDGGSParameterIndex(FunctionName functionName) {
        List<Parameter<?>> arguments = functionName.getArguments();
        for (int i = 0; i < arguments.size(); i++) {
            if (DGGSInstance.class.isAssignableFrom(arguments.get(i).getType())) {
                dggsParameterIndex = i;
            }
        }
        if (dggsParameterIndex == -1) {
            throw new IllegalArgumentException(
                    "Extending class does not expose a DGGSInstance parameter");
        }
    }

    /**
     * Reset the id cache in case the parameters are changing
     *
     * @param params
     */
    @Override
    public void setParameters(List<Expression> params) {
        super.setParameters(params);
        this.zoneIds = null;
        this.cacheTooBig = null;
    }

    @Override
    public void setDGGSInstance(DGGSInstance dggs) {
        Literal dggsLiteral = FF.literal(dggs);
        List<Expression> parameters = getParameters();
        if (parameters.size() == dggsParameterIndex) {
            parameters.add(dggsLiteral);
        } else {
            parameters.set(dggsParameterIndex, dggsLiteral);
        }
        super.setParameters(parameters);
    }

    /**
     * Default implementation, assuming that the first parameter is a variable zoneId, and the
     * others are
     *
     * @return
     */
    @Override
    public boolean isStable() {
        return getParameters().stream().skip(1).allMatch(p -> p instanceof Literal);
    }

    public void setCacheLimit(int cacheLimit) {
        this.cacheLimit = cacheLimit;
    }

    public void setIterateLimit(int iterationLimit) {
        this.iterationLimit = iterationLimit;
    }

    protected boolean matches(String zoneId, Supplier<Iterator<Zone>> zonesSupplier) {
        if (this.zoneIds != null) return this.zoneIds.contains(zoneId);

        Iterator<Zone> zones = zonesSupplier.get();
        if (isStable() && cacheTooBig == null) {
            // we haven't tried to cache, do so
            Set<String> result = new HashSet<>();
            if (cacheLimit <= 0) {
                zones.forEachRemaining(z -> result.add(z.getId()));
                this.zoneIds = result;
            } else {
                int i = 0;
                while (zones.hasNext() && i < cacheLimit) {
                    result.add(zones.next().getId());
                    i++;
                }
                if (i >= cacheLimit) {
                    this.cacheTooBig = true;
                } else {
                    this.zoneIds = result;
                }
            }
            if (zoneIds != null) return this.zoneIds.contains(zoneId);
        }

        // fallback, iteration case
        int i = 0;
        while (zones.hasNext() && i < iterationLimit) {
            if (zones.next().getId().equals(zoneId)) return true;
            i++;
        }

        if (i >= iterationLimit)
            throw new IllegalStateException(
                    "The iteration loaded up too many zones, above the configured limit of "
                            + iterationLimit);

        return false;
    }
}
