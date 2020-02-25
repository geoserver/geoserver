/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wcs2_0.eo.response;

import static org.geoserver.ows.util.ResponseUtils.appendQueryString;
import static org.geoserver.ows.util.ResponseUtils.buildURL;

import java.io.IOException;
import java.util.List;
import net.opengis.wcs20.GetCapabilitiesType;
import org.geoserver.catalog.CoverageInfo;
import org.geoserver.catalog.DimensionInfo;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.config.GeoServer;
import org.geoserver.ows.URLMangler;
import org.geoserver.ows.URLMangler.URLType;
import org.geoserver.ows.util.ResponseUtils;
import org.geoserver.wcs.WCSInfo;
import org.geoserver.wcs2_0.eo.EOCoverageResourceCodec;
import org.geoserver.wcs2_0.eo.WCSEOMetadata;
import org.geoserver.wcs2_0.response.WCSDimensionsHelper;
import org.geoserver.wcs2_0.response.WCSExtendedCapabilitiesProvider;
import org.geotools.coverage.grid.io.GridCoverage2DReader;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.xml.sax.Attributes;
import org.xml.sax.helpers.AttributesImpl;
import org.xml.sax.helpers.NamespaceSupport;

/**
 * Encodes the extensions to the WCS capabilities document
 *
 * @author Andrea Aime - GeoSolutions
 */
public class WCSEOExtendedCapabilitiesProvider extends WCSExtendedCapabilitiesProvider {
    EOCoverageResourceCodec codec;
    GeoServer gs;

    public WCSEOExtendedCapabilitiesProvider(GeoServer gs, EOCoverageResourceCodec codec) {
        this.codec = codec;
        this.gs = gs;
    }

    private boolean isEarthObservationEnabled() {
        WCSInfo wcs = gs.getService(WCSInfo.class);
        Boolean enabled = wcs.getMetadata().get(WCSEOMetadata.ENABLED.key, Boolean.class);
        return Boolean.TRUE.equals(enabled);
    }

    /** IGN : Do we still need to host this xsd ? */
    public String[] getSchemaLocations(String schemaBaseURL) {
        if (!isEarthObservationEnabled()) {
            return new String[0];
        }
        String schemaLocation =
                ResponseUtils.buildURL(
                        schemaBaseURL,
                        "schemas/wcseo/1.0/wcsEOGetCapabilites.xsd",
                        null,
                        URLType.RESOURCE);
        return new String[] {WCSEOMetadata.NAMESPACE, schemaLocation};
    }

    @Override
    public void registerNamespaces(NamespaceSupport namespaces) {
        if (isEarthObservationEnabled()) {
            namespaces.declarePrefix("wcseo", WCSEOMetadata.NAMESPACE);
        }
    }

    @Override
    public void encodeExtendedOperations(
            org.geoserver.ExtendedCapabilitiesProvider.Translator tx,
            WCSInfo wcs,
            GetCapabilitiesType request)
            throws IOException {
        if (!isEarthObservationEnabled()) {
            return;
        }

        AttributesImpl attributes = new AttributesImpl();
        attributes.addAttribute(null, "name", "name", null, "DescribeEOCoverageSet");
        tx.start("ows:Operation", attributes);

        final String url =
                appendQueryString(
                        buildURL(request.getBaseUrl(), "wcs", null, URLMangler.URLType.SERVICE),
                        "");

        tx.start("ows:DCP");
        tx.start("ows:HTTP");
        attributes = new AttributesImpl();
        attributes.addAttribute("", "xlink:href", "xlink:href", "", url);
        element(tx, "ows:Get", null, attributes);
        tx.end("ows:HTTP");
        tx.end("ows:DCP");

        attributes = new AttributesImpl();
        attributes.addAttribute("", "xlink:href", "xlink:href", "", url);
        tx.start("ows:DCP");
        tx.start("ows:HTTP");
        element(tx, "ows:Post", null, attributes);
        tx.end("ows:HTTP");
        tx.end("ows:DCP");

        tx.end("ows:Operation");

        Integer defaultCount =
                wcs.getMetadata().get(WCSEOMetadata.COUNT_DEFAULT.key, Integer.class);
        if (defaultCount != null) {
            tx.start("ows:Constraint", atts("name", "CountDefault"));
            element(tx, "ows:NoValues", null, null);
            element(tx, "ows:DefaultValue", String.valueOf(defaultCount), null);
            tx.end("ows:Constraint");
        }
    }

    @Override
    public void encodeExtendedContents(
            org.geoserver.ExtendedCapabilitiesProvider.Translator tx,
            WCSInfo wcs,
            List<CoverageInfo> coverages,
            GetCapabilitiesType request)
            throws IOException {
        Boolean enabled = wcs.getMetadata().get(WCSEOMetadata.ENABLED.key, Boolean.class);
        if (enabled == null || !enabled) {
            return;
        }

        for (CoverageInfo ci : coverages) {
            Boolean dataset = ci.getMetadata().get(WCSEOMetadata.DATASET.key, Boolean.class);
            DimensionInfo time = ci.getMetadata().get(ResourceInfo.TIME, DimensionInfo.class);
            if (dataset != null && dataset && time != null && time.isEnabled()) {
                tx.start("wcseo:DatasetSeriesSummary");
                ReferencedEnvelope bbox = ci.getLatLonBoundingBox();
                tx.start("ows:WGS84BoundingBox");
                element(tx, "ows:LowerCorner", bbox.getMinX() + " " + bbox.getMinY(), null);
                element(tx, "ows:UpperCorner", bbox.getMaxX() + " " + bbox.getMaxY(), null);
                tx.end("ows:WGS84BoundingBox");
                String datasetId = codec.getDatasetName(ci);
                element(tx, "wcseo:DatasetSeriesId", datasetId, null);

                GridCoverage2DReader reader =
                        (GridCoverage2DReader) ci.getGridCoverageReader(null, null);

                WCSDimensionsHelper dimensionsHelper = new WCSDimensionsHelper(time, reader, null);
                tx.start("gml:TimePeriod", atts("gml:id", datasetId + "__timeperiod"));
                element(tx, "gml:beginPosition", dimensionsHelper.getBeginTime(), null);
                element(tx, "gml:endPosition", dimensionsHelper.getEndTime(), null);
                tx.end("gml:TimePeriod");
                tx.end("wcseo:DatasetSeriesSummary");
            }
        }
    }

    private void element(
            org.geoserver.ExtendedCapabilitiesProvider.Translator tx,
            String element,
            String content,
            AttributesImpl attributes) {
        tx.start(element, attributes);
        if (content != null) {
            tx.chars(content);
        }
        tx.end(element);
    }

    Attributes atts(String... atts) {
        AttributesImpl attributes = new AttributesImpl();
        for (int i = 0; i < atts.length; i += 2) {
            attributes.addAttribute(null, atts[i], atts[i], null, atts[i + 1]);
        }
        return attributes;
    }
}
