/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wcs2_0.eo.response;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.geoserver.catalog.CoverageInfo;
import org.geoserver.catalog.DimensionInfo;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.config.GeoServer;
import org.geoserver.ows.URLMangler.URLType;
import org.geoserver.ows.util.ResponseUtils;
import org.geoserver.wcs.WCSInfo;
import org.geoserver.wcs2_0.GetCoverage;
import org.geoserver.wcs2_0.eo.WCSEOMetadata;
import org.geoserver.wcs2_0.response.WCS20CoverageMetadataProvider;
import org.geoserver.wcs2_0.response.WCSDimensionsHelper;
import org.geoserver.wcs2_0.util.NCNameResourceCodec;
import org.geotools.coverage.grid.io.GridCoverage2DReader;
import org.geotools.geometry.GeneralEnvelope;
import org.geotools.referencing.CRS;
import org.geotools.referencing.CRS.AxisOrder;
import org.geotools.util.logging.Logging;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.xml.sax.Attributes;
import org.xml.sax.helpers.AttributesImpl;
import org.xml.sax.helpers.NamespaceSupport;

/**
 * Encodes the minimal set of EO metadata for a given coverage:
 *
 * <pre>
 *                 <wcseo:EOMetadata>
 *                     <eop:EarthObservation gml:id="someEOCoverage1_metadata">
 *                         <om:phenomenonTime>
 *                             <gml:TimePeriod gml:id="someEOCoverage1_tp">
 *                                 <gml:beginPosition>2008-03-13T10:00:06.000</gml:beginPosition>
 *                                 <gml:endPosition>2008-03-13T10:20:26.000</gml:endPosition>
 *                             </gml:TimePeriod>
 *                         </om:phenomenonTime>
 *                         <om:resultTime>
 *                             <gml:TimeInstant gml:id="someEOCoverage1_archivingdate">
 *                                 <gml:timePosition>2001-08-22T11:02:47.999</gml:timePosition>
 *                             </gml:TimeInstant>
 *                         </om:resultTime>
 *                         <om:procedure/>
 *                         <om:observedProperty/>
 *                         <om:featureOfInterest>
 *                             <eop:Footprint gml:id="someEOCoverage1_fp">
 *                                 <eop:multiExtentOf>
 *                                     <gml:MultiSurface gml:id="someEOCoverage1_ms" srsName="EPSG:4326">
 *                                         <gml:surfaceMembers>
 *                                             <gml:Polygon gml:id="someEOCoverage1_fppoly">
 *                                                 <gml:exterior>
 *                                                     <gml:LinearRing>
 *                                                         <gml:posList>43.516667 2.1025 43.381667 2.861667 42.862778 2.65 42.996389 1.896944 43.516667 2.1025</gml:posList>
 *                                                     </gml:LinearRing>
 *                                                 </gml:exterior>
 *                                             </gml:Polygon>
 *                                         </gml:surfaceMembers>
 *                                     </gml:MultiSurface>
 *                                 </eop:multiExtentOf>
 *                                 <eop:centerOf>
 *                                     <gml:Point gml:id="someEOCoverage1_p" srsName="EPSG:4326">
 *                                         <gml:pos>43.190833 2.374167</gml:pos>
 *                                     </gml:Point>
 *                                 </eop:centerOf>
 *                             </eop:Footprint>
 *                         </om:featureOfInterest>
 *                         <om:result/>
 *                         <eop:metaDataProperty>
 *                             <eop:EarthObservationMetaData>
 *                                 <eop:identifier>someEOCoverage1</eop:identifier>
 *                                 <eop:acquisitionType>NOMINAL</eop:acquisitionType>
 *                                 <eop:status>ARCHIVED</eop:status>
 *                             </eop:EarthObservationMetaData>
 *                         </eop:metaDataProperty>
 *                     </eop:EarthObservation>
 *                 </wcseo:EOMetadata>
 * </pre>
 *
 * @author Andrea Aime - GeoSolutions
 */
public class WCSEOCoverageMetadataProvider implements WCS20CoverageMetadataProvider {

    static final Logger LOGGER = Logging.getLogger(WCSEOCoverageMetadataProvider.class);
    private GeoServer gs;

    public WCSEOCoverageMetadataProvider(GeoServer gs) {
        this.gs = gs;
    }

    private boolean isEarthObservationEnabled() {
        WCSInfo wcs = gs.getService(WCSInfo.class);
        Boolean enabled = wcs.getMetadata().get(WCSEOMetadata.ENABLED.key, Boolean.class);
        return Boolean.TRUE.equals(enabled);
    }

    @Override
    public String[] getSchemaLocations(String schemaBaseURL) {
        if (!isEarthObservationEnabled()) {
            return new String[0];
        }
        String schemaLocation =
                ResponseUtils.buildURL(
                        schemaBaseURL,
                        "schemas/wcseo/1.0/wcsEOCoverage.xsd",
                        null,
                        URLType.RESOURCE);
        return new String[] {WCSEOMetadata.NAMESPACE, schemaLocation};
    }

    @Override
    public void registerNamespaces(NamespaceSupport namespaces) {
        if (!isEarthObservationEnabled()) {
            return;
        }
        namespaces.declarePrefix("wcseo", WCSEOMetadata.NAMESPACE);
        namespaces.declarePrefix("eop", "http://www.opengis.net/eop/2.0");
        namespaces.declarePrefix("gml", "http://www.opengis.net/gml/3.2");
        namespaces.declarePrefix("om", "http://www.opengis.net/om/2.0");
    }

    @Override
    public void encode(Translator tx, Object context) throws IOException {
        if (!(context instanceof CoverageInfo) || !isEarthObservationEnabled()) {
            return;
        }

        CoverageInfo ci = (CoverageInfo) context;
        DimensionInfo time = ci.getMetadata().get(ResourceInfo.TIME, DimensionInfo.class);
        if (time == null) {
            LOGGER.log(
                    Level.FINE,
                    "We received a coverage info that has no "
                            + "associated time, cannot add EO metadata to it: "
                            + ci.prefixedName());
            return;
        }
        GridCoverage2DReader reader = (GridCoverage2DReader) ci.getGridCoverageReader(null, null);
        String coverageId = NCNameResourceCodec.encode(ci);
        WCSDimensionsHelper dimensionHelper = new WCSDimensionsHelper(time, reader, coverageId);
        tx.start("wcseo:EOMetadata");
        tx.start("eop:EarthObservation", atts("gml:id", coverageId + "_metadata"));

        // phenomenon time
        tx.start("om:phenomenonTime");
        tx.start("gml:TimePeriod", atts("gml:id", coverageId + "_tp"));
        element(tx, "gml:beginPosition", dimensionHelper.getBeginTime(), null);
        element(tx, "gml:endPosition", dimensionHelper.getEndTime(), null);
        tx.end("gml:TimePeriod");
        tx.end("om:phenomenonTime");

        // resultTime
        tx.start("om:resultTime");
        tx.start("gml:TimeInstant", atts("gml:id", coverageId + "_rt"));
        element(tx, "gml:timePosition", dimensionHelper.getEndTime(), null);
        tx.end("gml:TimeInstant");
        tx.end("om:resultTime");

        // some empty elements...
        element(tx, "om:procedure", null, null);
        element(tx, "om:observedProperty", null, null);

        // the footprint
        GeneralEnvelope ge = reader.getOriginalEnvelope();
        CoordinateReferenceSystem crs = reader.getCoordinateReferenceSystem();
        String srsName = getSRSName(crs);
        final boolean axisSwap = CRS.getAxisOrder(crs).equals(AxisOrder.EAST_NORTH);
        double minx = ge.getLowerCorner().getOrdinate(axisSwap ? 1 : 0);
        double miny = ge.getLowerCorner().getOrdinate(axisSwap ? 0 : 1);
        double maxx = ge.getUpperCorner().getOrdinate(axisSwap ? 1 : 0);
        double maxy = ge.getUpperCorner().getOrdinate(axisSwap ? 0 : 1);
        tx.start("om:featureOfInterest");
        tx.start("eop:Footprint", atts("gml:id", coverageId + "_fp"));
        tx.start("eop:multiExtentOf");
        tx.start("gml:MultiSurface", atts("gml:id", coverageId + "_ms", "srsName", srsName));
        tx.start("gml:surfaceMembers");
        tx.start("gml:Polygon", atts("gml:id", coverageId + "_msp"));
        tx.start("gml:exterior");
        tx.start("gml:LinearRing");
        String posList = posList(minx, miny, minx, maxy, maxx, maxy, maxx, miny, minx, miny);
        element(tx, "gml:posList", posList, null);
        tx.end("gml:LinearRing");
        tx.end("gml:exterior");
        tx.end("gml:Polygon");
        tx.end("gml:surfaceMembers");
        tx.end("gml:MultiSurface");
        tx.end("eop:multiExtentOf");
        double midx = (minx + maxx) / 2;
        double midy = (miny + maxy) / 2;
        tx.start("eop:centerOf");
        tx.start("gml:Point", atts("gml:id", coverageId + "_co", "srsName", srsName));
        element(tx, "gml:pos", midx + " " + midy, null);
        tx.end("gml:Point");
        tx.end("eop:centerOf");
        tx.end("eop:Footprint");
        tx.end("om:featureOfInterest");

        // fixed metadata properties (at least for the moment)
        tx.start("eop:metaDataProperty");
        tx.start("eop:EarthObservationMetaData");
        element(tx, "eop:identifier", coverageId, null);
        element(tx, "eop:acquisitionType", "NOMINAL", null);
        element(tx, "eop:status", "ARCHIVED", null);
        tx.end("eop:EarthObservationMetaData");
        tx.end("eop:metaDataProperty");

        tx.end("eop:EarthObservation");
        tx.end("wcseo:EOMetadata");
    }

    private String posList(double... ordinates) {
        StringBuilder sb = new StringBuilder();
        for (double ord : ordinates) {
            sb.append(ord).append(" ");
        }
        return sb.substring(0, sb.length() - 1);
    }

    private String getSRSName(CoordinateReferenceSystem crs) {
        Integer EPSGCode = null;
        try {
            EPSGCode = CRS.lookupEpsgCode(crs, false);
        } catch (FactoryException e) {
            throw new IllegalStateException("Unable to lookup epsg code for this CRS:" + crs, e);
        }
        if (EPSGCode == null) {
            throw new IllegalStateException("Unable to lookup epsg code for this CRS:" + crs);
        }
        return GetCoverage.SRS_STARTER + EPSGCode;
    }

    private void element(Translator tx, String element, String content, AttributesImpl attributes) {
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
