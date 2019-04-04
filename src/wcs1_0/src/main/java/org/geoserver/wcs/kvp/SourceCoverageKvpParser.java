/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wcs.kvp;

import static org.vfny.geoserver.wcs.WcsException.WcsExceptionCode.InvalidParameterValue;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.PublishedType;
import org.geoserver.ows.KvpParser;
import org.geoserver.ows.util.KvpUtils;
import org.vfny.geoserver.wcs.WcsException;
import org.vfny.geoserver.wcs.WcsException.WcsExceptionCode;

/**
 * The parser validates the coverage requested on a WCS 1.0.0 GetCoverage request.
 *
 * @author Alessio Fabiani, GeoSolutions
 */
public class SourceCoverageKvpParser extends KvpParser {

    private Catalog catalog;

    public SourceCoverageKvpParser(Catalog catalog) {
        super("sourcecoverage", String.class);
        setService("wcs");
        this.catalog = catalog;
    }

    @Override
    public Object parse(String value) throws Exception {
        Collection coverages = new ArrayList();
        final List<String> identifiers = KvpUtils.readFlat(value);
        if (identifiers == null || identifiers.size() == 0) {
            throw new WcsException(
                    "Required paramer, sourcecoverage, missing",
                    WcsExceptionCode.MissingParameterValue,
                    "sourcecoverage");
        }

        for (String coverage : identifiers) {
            LayerInfo layer = catalog.getLayerByName(value);
            if (layer == null || layer.getType() != PublishedType.RASTER)
                throw new WcsException(
                        "Could not find sourcecoverage '" + coverage + "'",
                        InvalidParameterValue,
                        "sourcecoverage");
            coverages.add(coverage);
        }

        if (coverages.size() > 1) {
            throw new WcsException(
                    "Wrong parameter, sourcecoverage, more than one identifier was specified",
                    WcsExceptionCode.InvalidParameterValue,
                    "sourcecoverage");
        }

        return ((ArrayList) coverages).get(0);
    }
}
