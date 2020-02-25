/*
 *  (c) 2018 Open Source Geospatial Foundation - all rights reserved
 *  * This code is licensed under the GPL 2.0 license, available at the root
 *  * application directory.
 *
 */

package org.geoserver.wfs3;

import static org.geoserver.ows.util.ResponseUtils.urlEncode;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.ows.URLMangler.URLType;
import org.geoserver.ows.util.CaseInsensitiveMap;
import org.geoserver.ows.util.ResponseUtils;
import org.geoserver.wfs.WFSInfo;
import org.geoserver.wfs.request.FeatureCollectionResponse;
import org.geoserver.wfs.request.GetFeatureRequest;
import org.geotools.util.logging.Logging;

/** A subclass of GetFeature that builds proper WFS3 nex/prev links */
class WFS3GetFeature extends org.geoserver.wfs.GetFeature {

    static final Logger LOGGER = Logging.getLogger(WFS3GetFeature.class);

    public WFS3GetFeature(WFSInfo wfs, Catalog catalog) {
        super(wfs, catalog);
    }

    @Override
    protected void buildPrevNextLinks(
            GetFeatureRequest request,
            int offset,
            int maxFeatures,
            int count,
            FeatureCollectionResponse result,
            Map<String, String> kvp) {
        // can we build the links?
        String typename = kvp.get("TYPENAME");
        if (typename == null) {
            LOGGER.log(
                    Level.INFO,
                    "Cannot build prev/next links, the the target typename is not known");
            return;
        }
        FeatureTypeInfo typeInfo = getCatalog().getFeatureTypeByName(typename);
        if (typeInfo == null) {
            if (typename == null) {
                LOGGER.log(
                        Level.INFO,
                        "Cannot build prev/next links, the the target typename was not found: "
                                + typename);
                return;
            }
        }
        String collectionName = NCNameResourceCodec.encode(typeInfo);
        String itemsPath = "wfs3/collections/" + urlEncode(collectionName) + "/items";

        // clean up the KVP params, remove the ones that are not WFS3 specific
        List<String> PARAMS_BLACKLIST =
                Arrays.asList(
                        "SERVICE",
                        "VERSION",
                        "REQUEST",
                        "TYPENAME",
                        "TYPENAMES",
                        "COUNT",
                        "OUTPUTFORMAT",
                        "STARTINDEX",
                        "LIMIT");
        kvp = new CaseInsensitiveMap(kvp);
        for (String param : PARAMS_BLACKLIST) {
            kvp.remove(param);
        }
        // remove the SRSNAME if its value is 4326
        if ("EPSG:4326".equals(kvp.get("SRSNAME"))) {
            kvp.remove("SRSNAME");
        }

        // in WFS3 params are normally lowercase (and are case sensitive)...
        // TODO: we might need a list of parameters and their "normalized case" for WFS3, we'll
        // wait for the filtering/crs extensions to show up before deciding exactly what exactly to
        // do
        kvp =
                kvp.entrySet()
                        .stream()
                        .collect(
                                Collectors.toMap(
                                        entry -> entry.getKey().toLowerCase(),
                                        entry -> entry.getValue()));

        // build prev link if needed
        if (offset > 0) {
            // previous offset calculated as the current offset - maxFeatures, or 0 if this is a
            // negative value, while  previous count should be current offset - previousOffset
            int prevOffset = Math.max(offset - maxFeatures, 0);
            kvp.put("startIndex", String.valueOf(prevOffset));
            kvp.put("limit", String.valueOf(offset - prevOffset));
            result.setPrevious(buildURL(request, itemsPath, kvp));
        }

        // build next link if needed
        if (count > 0 && offset > -1 && maxFeatures <= count) {
            kvp.put("startIndex", String.valueOf(offset > 0 ? offset + count : count));
            kvp.put("limit", String.valueOf(maxFeatures));
            result.setNext(buildURL(request, itemsPath, kvp));
        }
    }

    private String buildURL(GetFeatureRequest request, String itemsPath, Map<String, String> kvp) {
        return ResponseUtils.buildURL(request.getBaseUrl(), itemsPath, kvp, URLType.SERVICE);
    }
}
