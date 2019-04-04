/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2014 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms.dimension.impl;

import org.geoserver.catalog.DimensionInfo;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.wms.dimension.AbstractFeatureAttributeVisitorSelectionStrategy;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.feature.visitor.CalcResult;
import org.geotools.feature.visitor.FeatureCalc;
import org.geotools.feature.visitor.NearestVisitor;
import org.geotools.util.Converters;
import org.opengis.filter.FilterFactory2;

/**
 * Default implementation for selecting the default values for dimensions of feature (vector)
 * resources using the nearest-domain-value-to-the-reference-value strategy.
 *
 * @author Ilkka Rinne / Spatineo Inc for the Finnish Meteorological Institute
 */
public class FeatureNearestValueSelectionStrategyImpl
        extends AbstractFeatureAttributeVisitorSelectionStrategy {

    private Object toMatch;
    private String fixedCapabilitiesValue;
    private FilterFactory2 ff = CommonFactoryFinder.getFilterFactory2();

    /** Default constructor. */
    public FeatureNearestValueSelectionStrategyImpl(Object toMatch) {
        this(toMatch, null);
    }

    public FeatureNearestValueSelectionStrategyImpl(Object toMatch, String capabilitiesValue) {
        this.toMatch = toMatch;
        this.fixedCapabilitiesValue = capabilitiesValue;
    }

    @Override
    public Object getDefaultValue(
            ResourceInfo resource, String dimensionName, DimensionInfo dimension, Class clz) {
        final FeatureCalc nearest =
                new NearestVisitor(ff.property(dimension.getAttribute()), this.toMatch);

        CalcResult res = getCalculatedResult((FeatureTypeInfo) resource, dimension, nearest);
        if (res.equals(CalcResult.NULL_RESULT)) {
            return null;
        } else {
            return Converters.convert(res.getValue(), clz);
        }
    }

    @Override
    public String getCapabilitiesRepresentation(
            ResourceInfo resource, String dimensionName, DimensionInfo dimensionInfo) {
        if (fixedCapabilitiesValue != null) {
            return this.fixedCapabilitiesValue;
        } else {
            return super.getCapabilitiesRepresentation(resource, dimensionName, dimensionInfo);
        }
    }
}
