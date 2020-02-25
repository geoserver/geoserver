/* (c) 2019 Open Source Geospatial Foundation - all rights reserved
 *
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.csw.function;

import static org.geotools.filter.capability.FunctionNameImpl.parameter;

import org.geoserver.catalog.CatalogInfo;
import org.geoserver.catalog.CoverageInfo;
import org.geoserver.catalog.CoverageStoreInfo;
import org.geoserver.catalog.DataStoreInfo;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.catalog.HTTPStoreInfo;
import org.geoserver.catalog.LayerGroupInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.MapInfo;
import org.geoserver.catalog.NamespaceInfo;
import org.geoserver.catalog.StyleInfo;
import org.geoserver.catalog.WMSLayerInfo;
import org.geoserver.catalog.WMTSLayerInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.geotools.filter.FunctionExpressionImpl;
import org.geotools.filter.capability.FunctionNameImpl;
import org.opengis.filter.capability.FunctionName;

/**
 * Return the type of any Catalognfo Object
 *
 * @author Niels Charlier
 */
public class TypeOfFunction extends FunctionExpressionImpl {

    public static FunctionName NAME =
            new FunctionNameImpl("typeOf", String.class, parameter("info", CatalogInfo.class));

    private static final Class<?>[] TYPE_CLASSES =
            new Class<?>[] {
                MapInfo.class,
                NamespaceInfo.class,
                LayerInfo.class,
                LayerGroupInfo.class,
                CoverageInfo.class,
                FeatureTypeInfo.class,
                WMSLayerInfo.class,
                WMTSLayerInfo.class,
                CoverageStoreInfo.class,
                DataStoreInfo.class,
                HTTPStoreInfo.class,
                StyleInfo.class,
                WorkspaceInfo.class
            };

    @SuppressWarnings("unchecked")
    public static <T extends CatalogInfo> Class<? extends CatalogInfo> typeClass(Class<T> clazz) {
        for (Class<?> rootClass : TYPE_CLASSES) {
            if (rootClass.isAssignableFrom(clazz)) {
                return (Class<? extends CatalogInfo>) rootClass;
            }
        }
        return null;
    }

    public TypeOfFunction() {
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

        return typeClass(info.getClass()).getSimpleName();
    }
}
