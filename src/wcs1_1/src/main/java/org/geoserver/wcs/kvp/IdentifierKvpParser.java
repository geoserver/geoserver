/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wcs.kvp;

import static org.vfny.geoserver.wcs.WcsException.WcsExceptionCode.InvalidParameterValue;

import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.PublishedType;
import org.geoserver.ows.kvp.CodeTypeKvpParser;
import org.vfny.geoserver.wcs.WcsException;

public class IdentifierKvpParser extends CodeTypeKvpParser {

    private Catalog catalog;

    public IdentifierKvpParser(Catalog catalog) {
        super("identifier", "wcs");
        this.catalog = catalog;
    }

    @Override
    public Object parse(String value) throws Exception {
        LayerInfo layer = catalog.getLayerByName(value);
        if (layer == null || layer.getType() != PublishedType.RASTER)
            throw new WcsException(
                    "Could not find coverage '" + value + "'", InvalidParameterValue, "identifier");
        return super.parse(value);
    }
}
