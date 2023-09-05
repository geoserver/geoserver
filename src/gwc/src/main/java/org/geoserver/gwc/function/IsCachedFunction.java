/* (c) 2019 Open Source Geospatial Foundation - all rights reserved
 *
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.gwc.function;

import static org.geotools.filter.capability.FunctionNameImpl.parameter;

import org.geoserver.catalog.CatalogInfo;
import org.geoserver.gwc.GWC;
import org.geotools.api.filter.capability.FunctionName;
import org.geotools.filter.FunctionExpressionImpl;
import org.geotools.filter.capability.FunctionNameImpl;

/**
 * Detect if layer is cached.
 *
 * @author Niels Charlier
 */
public class IsCachedFunction extends FunctionExpressionImpl {

    public static FunctionName NAME =
            new FunctionNameImpl("isCached", Boolean.class, parameter("info", CatalogInfo.class));

    public IsCachedFunction() {
        super(NAME);
    }

    @Override
    public Object evaluate(Object object) {
        CatalogInfo info;

        try {
            info = (CatalogInfo) getExpression(0).evaluate(object);
        } catch (Exception e) {
            throw new IllegalArgumentException(
                    "Filter Function problem for function typeOf argument #0 - expected type CatalogInfo",
                    e);
        }

        return GWC.get().hasTileLayer(info);
    }
}
