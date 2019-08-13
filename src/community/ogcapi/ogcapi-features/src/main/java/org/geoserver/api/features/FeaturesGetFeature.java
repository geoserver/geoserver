/* (c) 2018 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.api.features;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import javax.xml.namespace.QName;
import org.geoserver.api.APIRequestInfo;
import org.geoserver.api.NCNameResourceCodec;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.ows.URLMangler.URLType;
import org.geoserver.ows.util.CaseInsensitiveMap;
import org.geoserver.ows.util.ResponseUtils;
import org.geoserver.wfs.WFSInfo;
import org.geoserver.wfs.request.FeatureCollectionResponse;
import org.geoserver.wfs.request.GetFeatureRequest;
import org.geoserver.wfs.request.Query;
import org.geotools.util.logging.Logging;

/** A subclass of GetFeature that builds proper API Feature nex/prev links */
class FeaturesGetFeature extends org.geoserver.wfs.GetFeature {

    static final Logger LOGGER = Logging.getLogger(FeaturesGetFeature.class);

    public FeaturesGetFeature(WFSInfo wfs, Catalog catalog) {
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
        List<Query> queries = request.getQueries();
        if (queries == null
                || queries.size() != 1
                || queries.get(0).getTypeNames() == null
                || queries.get(0).getTypeNames().size() != 1) {
            LOGGER.log(
                    Level.INFO,
                    "Cannot build prev/next links, the the target typename is not known (or multiple type names available)");
            return;
        }
        QName typeName = queries.get(0).getTypeNames().get(0);
        FeatureTypeInfo typeInfo =
                getCatalog()
                        .getFeatureTypeByName(typeName.getNamespaceURI(), typeName.getLocalPart());
        if (typeInfo == null) {
            LOGGER.log(
                    Level.INFO,
                    "Cannot build prev/next links, the the target typename was not found: "
                            + typeName);
            return;
        }
        String collectionName = NCNameResourceCodec.encode(typeInfo);
        String itemsPath =
                "ogc/features/collections/" + ResponseUtils.urlEncode(collectionName) + "/items";

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
            result.setPrevious(buildURL(itemsPath, kvp));
        }

        // build next link if needed
        if (count > 0 && offset > -1 && maxFeatures <= count) {
            kvp.put("startIndex", String.valueOf(offset > 0 ? offset + count : count));
            kvp.put("limit", String.valueOf(maxFeatures));
            result.setNext(buildURL(itemsPath, kvp));
        }
    }

    private String buildURL(String itemsPath, Map<String, String> kvp) {
        return ResponseUtils.buildURL(
                APIRequestInfo.get().getBaseURL(), itemsPath, kvp, URLType.SERVICE);
    }
}
