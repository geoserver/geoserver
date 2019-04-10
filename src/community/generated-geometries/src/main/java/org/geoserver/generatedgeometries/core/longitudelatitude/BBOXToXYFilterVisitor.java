/* (c) 2019 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.generatedgeometries.core.longitudelatitude;

import java.util.Arrays;
import org.geoserver.generatedgeometries.core.longitudelatitude.LongLatGeometryGenerationStrategy.LongLatConfiguration;
import org.geotools.filter.visitor.DuplicatingFilterVisitor;
import org.opengis.filter.And;
import org.opengis.filter.FilterFactory;
import org.opengis.filter.PropertyIsBetween;
import org.opengis.filter.expression.Literal;
import org.opengis.filter.expression.PropertyName;
import org.opengis.filter.spatial.BBOX;
import org.opengis.geometry.BoundingBox;

/**
 * This class converts any BBOX filter to a In Between Expression using configured X and Y fields.
 */
public class BBOXToXYFilterVisitor extends DuplicatingFilterVisitor {

    private FilterFactory ff;
    private LongLatConfiguration configuration;

    /**
     * @param ff filter factory
     * @param configuration with information about X/Y Fields
     */
    public BBOXToXYFilterVisitor(FilterFactory ff, LongLatConfiguration configuration) {
        super();
        this.ff = ff;
        this.configuration = configuration;
    }

    @Override
    public Object visit(BBOX filter, Object extraData) {

        BoundingBox bounds = ((BBOX) filter).getBounds();
        return getLongLatFilters(bounds);
    }

    private PropertyIsBetween createBetweenFilter(
            FilterFactory ff, String name, double minValue, double maxValue) {
        PropertyName propertyName = ff.property(name);
        Literal min = ff.literal(minValue);
        Literal max = ff.literal(maxValue);
        return ff.between(propertyName, min, max);
    }

    private And getLongLatFilters(BoundingBox bounds) {
        PropertyIsBetween longitudeFilter =
                createBetweenFilter(
                        ff, configuration.longAttributeName, bounds.getMinX(), bounds.getMaxX());
        PropertyIsBetween latitudeFilter =
                createBetweenFilter(
                        ff, configuration.latAttributeName, bounds.getMinY(), bounds.getMaxY());
        return ff.and(Arrays.asList(longitudeFilter, latitudeFilter));
    }
}
