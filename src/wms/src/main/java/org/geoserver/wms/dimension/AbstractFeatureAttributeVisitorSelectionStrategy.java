/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2014 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms.dimension;

import java.io.IOException;
import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.geoserver.catalog.DimensionInfo;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.platform.ServiceException;
import org.geotools.data.FeatureSource;
import org.geotools.data.Query;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.visitor.CalcResult;
import org.geotools.feature.visitor.FeatureCalc;
import org.geotools.util.factory.GeoTools;
import org.geotools.util.logging.Logging;

/**
 * An abstract parent class for a DefaultValueSelectionStrategy implementations that use a {@link
 * FeatureCalc} instances for finding the matching default value.
 *
 * @author Ilkka Rinne / Spatineo Inc for the Finnish Meteorological Institute
 */
public abstract class AbstractFeatureAttributeVisitorSelectionStrategy
        extends AbstractDefaultValueSelectionStrategy {

    private static Logger LOGGER =
            Logging.getLogger(AbstractFeatureAttributeVisitorSelectionStrategy.class);

    /**
     * Return the result of iterating through the dimension collection of the given dimension using
     * given calculator as the attribute value calculator.
     */
    protected CalcResult getCalculatedResult(
            FeatureTypeInfo typeInfo, DimensionInfo dimension, FeatureCalc calculator) {
        CalcResult retval = null;
        try {
            FeatureCollection<?, ?> dimensionCollection =
                    getDimensionCollection(typeInfo, dimension);
            if (dimensionCollection == null) {
                throw new ServiceException(
                        "No dimension collection given, cannot select default value for dimension based on attribute"
                                + dimension.getAttribute());
            }
            dimensionCollection.accepts(calculator, null);
            retval = calculator.getResult();
        } catch (IOException e) {
            LOGGER.log(Level.FINER, e.getMessage(), e);
        }
        return retval;
    }

    private FeatureCollection<?, ?> getDimensionCollection(
            FeatureTypeInfo typeInfo, DimensionInfo dimension) throws IOException {
        // grab the feature source
        FeatureSource<?, ?> source = null;
        try {
            source = typeInfo.getFeatureSource(null, GeoTools.getDefaultHints());
        } catch (IOException e) {
            throw new ServiceException(
                    "Could not get the feauture source to list time info for layer "
                            + typeInfo.prefixedName(),
                    e);
        }

        // build query to grab the dimension values
        final Query dimQuery = new Query(source.getSchema().getName().getLocalPart());
        dimQuery.setPropertyNames(Arrays.asList(dimension.getAttribute()));
        return source.getFeatures(dimQuery);
    }
}
