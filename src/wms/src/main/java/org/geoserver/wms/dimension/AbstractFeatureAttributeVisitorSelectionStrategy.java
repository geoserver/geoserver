/* Copyright (c) 2001 - 2013 OpenPlans - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms.dimension;

import java.io.IOException;
import java.util.Arrays;
import java.util.logging.Level;

import org.geoserver.catalog.DimensionInfo;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.platform.ServiceException;
import org.geotools.data.FeatureSource;
import org.geotools.data.Query;
import org.geotools.factory.GeoTools;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.visitor.CalcResult;
import org.geotools.feature.visitor.FeatureCalc;

abstract class AbstractFeatureAttributeVisitorSelectionStrategy extends AbstractCapabilitiesDefaultValueSelectionStrategy {

    /** serialVersionUID */
    private static final long serialVersionUID = 3863284347371098095L;

    protected CalcResult getSelectedValue(FeatureTypeInfo typeInfo, DimensionInfo dimension,
            FeatureCalc calculator) {
        CalcResult retval = null;
        try {
            FeatureCollection<?, ?> dimensionCollection = getDimensionCollection(typeInfo,
                    dimension);
            if (dimensionCollection == null) {
                throw new ServiceException(
                        "No dimension collection given, cannot select default value for dimension based on attribute"
                                + dimension.getAttribute());
            }
            dimensionCollection.accepts(calculator, null);
            retval = calculator.getResult();
        } catch (IOException e) {
            DimensionDefaultValueStrategyFactoryImpl.LOGGER.log(Level.FINER, e.getMessage(), e);
        }
        return retval;
    }

    private FeatureCollection<?, ?> getDimensionCollection(FeatureTypeInfo typeInfo,
            DimensionInfo dimension) throws IOException {
        // grab the feature source
        FeatureSource<?, ?> source = null;
        try {
            source = typeInfo.getFeatureSource(null, GeoTools.getDefaultHints());
        } catch (IOException e) {
            throw new ServiceException(
                    "Could not get the feauture source to list time info for layer "
                            + typeInfo.prefixedName(), e);
        }

        // build query to grab the dimension values
        final Query dimQuery = new Query(source.getSchema().getName().getLocalPart());
        dimQuery.setPropertyNames(Arrays.asList(dimension.getAttribute()));
        return source.getFeatures(dimQuery);
    }
}