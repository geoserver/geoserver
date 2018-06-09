/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.kml.regionate;

import java.sql.Connection;
import java.util.Map;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.config.GeoServer;
import org.geoserver.platform.ServiceException;
import org.geoserver.wms.MapLayerInfo;
import org.geoserver.wms.WMSMapContent;
import org.geotools.data.FeatureSource;
import org.geotools.data.Query;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.feature.FeatureIterator;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.map.Layer;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.AttributeType;
import org.opengis.feature.type.GeometryDescriptor;
import org.opengis.filter.FilterFactory;
import org.opengis.filter.sort.SortBy;
import org.opengis.filter.sort.SortOrder;
import org.opengis.filter.spatial.BBOX;

/**
 * An attribute based regionating strategy assuming it's possible (and fast) to sort on the user
 * specified attribute. Features with higher values of the attribute will be found in higher tiles.
 *
 * @author Andrea Aime
 */
public class NativeSortRegionatingStrategy extends CachedHierarchyRegionatingStrategy {

    public NativeSortRegionatingStrategy(GeoServer gs) {
        super(gs);
    }

    static final FilterFactory ff = CommonFactoryFinder.getFilterFactory(null);

    String attribute;

    FeatureSource fs;

    @Override
    protected String getDatabaseName(WMSMapContent con, Layer layer) throws Exception {
        fs = layer.getFeatureSource();
        SimpleFeatureType type = (SimpleFeatureType) fs.getSchema();

        // find out which attribute we're going to use
        Map options = con.getRequest().getFormatOptions();
        attribute = (String) options.get("regionateAttr");
        if (attribute == null) attribute = MapLayerInfo.getRegionateAttribute(featureType);
        if (attribute == null)
            throw new ServiceException("Regionating attribute has not been specified");

        // Make sure the attribute is actually there
        AttributeType attributeType = type.getType(attribute);
        if (attributeType == null) {
            throw new ServiceException(
                    "Could not find regionating attribute "
                            + attribute
                            + " in layer "
                            + featureType.getName());
        }

        // check we can actually sort on that attribute
        if (!fs.getQueryCapabilities()
                .supportsSorting(new SortBy[] {ff.sort(attribute, SortOrder.DESCENDING)}))
            throw new ServiceException(
                    "Native sorting on the "
                            + attribute
                            + " is not possible for layer "
                            + featureType.getName());

        // make sure a special db for this layer and attribute will be created
        return super.getDatabaseName(con, layer) + "_" + attribute;
    }

    @Override
    protected String getDatabaseName(FeatureTypeInfo cfg) throws Exception {
        return super.getDatabaseName(cfg) + "_" + MapLayerInfo.getRegionateAttribute(cfg);
    }

    public FeatureIterator getSortedFeatures(
            GeometryDescriptor geom,
            ReferencedEnvelope latLongEnv,
            ReferencedEnvelope nativeEnv,
            Connection cacheConn)
            throws Exception {
        // build the bbox filter
        FilterFactory ff = CommonFactoryFinder.getFilterFactory(null);
        BBOX filter =
                ff.bbox(
                        geom.getLocalName(),
                        nativeEnv.getMinX(),
                        nativeEnv.getMinY(),
                        nativeEnv.getMaxX(),
                        nativeEnv.getMaxY(),
                        null);

        // build an optimized query (only the necessary attributes
        Query q = new Query();
        q.setFilter(filter);
        q.setPropertyNames(new String[] {geom.getLocalName(), attribute});
        // TODO: enable this when JTS learns how to compute centroids
        // without triggering the
        // generation of Coordinate[] out of the sequences...
        // q.setHints(new Hints(Hints.JTS_COORDINATE_SEQUENCE_FACTORY,
        // PackedCoordinateSequenceFactory.class));
        q.setSortBy(new SortBy[] {ff.sort(attribute, SortOrder.DESCENDING)});

        // return the reader
        return fs.getFeatures(q).features();
    }
}
