/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms;

import org.geoserver.catalog.CoverageInfo;

/**
 * The following will provide data to test the default ImageMosaic cql filter The added mosaic is
 * used WITHOUT dimensions which are instead used as attributes.
 *
 * @see {@link FilterMosaicGetMapTest}
 * @author carlo cancellieri
 */
public class WMSFilterMosaicTestSupport extends WMSDimensionsTestSupport {

    /** Here we setup the default mosaic filter to the layer */
    protected void setupMosaicFilter(String filter, String layer) {
        CoverageInfo info = getCatalog().getCoverageByName(layer);

        info.getParameters().put("Filter", filter);

        getCatalog().save(info);
    }
}
