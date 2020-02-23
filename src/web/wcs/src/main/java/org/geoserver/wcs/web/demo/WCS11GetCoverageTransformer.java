/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wcs.web.demo;

import java.awt.geom.AffineTransform;
import java.util.logging.Logger;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CoverageInfo;
import org.geoserver.wcs.responses.CoverageResponseDelegate;
import org.geoserver.wcs.responses.CoverageResponseDelegateFinder;
import org.geotools.filter.v1_0.OGC;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.gml3.GML;
import org.geotools.ows.v1_1.OWS;
import org.geotools.referencing.CRS;
import org.geotools.util.logging.Logging;
import org.geotools.xml.transform.TransformerBase;
import org.geotools.xml.transform.Translator;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.xml.sax.ContentHandler;
import org.xml.sax.helpers.AttributesImpl;

/**
 * Helper class to turn a {@link GetCoverageRequest} into the corresponding WCS 1.1 GetCoverage xml
 *
 * @author Andrea Aime - GeoSolutions
 */
class WCS11GetCoverageTransformer extends TransformerBase {

    static final Logger LOGGER = Logging.getLogger(WCS11GetCoverageTransformer.class);

    private Catalog catalog;

    private CoverageResponseDelegateFinder responseFactory;

    public WCS11GetCoverageTransformer(
            Catalog catalog, CoverageResponseDelegateFinder responseFactory) {
        this.catalog = catalog;
        this.responseFactory = responseFactory;
    }

    @Override
    public Translator createTranslator(ContentHandler handler) {
        return new ExecuteRequestTranslator(handler);
    }

    public class ExecuteRequestTranslator extends TranslatorSupport {
        protected static final String WCS_URI = "http://www.opengis.net/wcs/1.1.1";

        /** xml schema namespace + prefix */
        protected static final String XSI_PREFIX = "xsi";

        protected static final String XSI_URI = "http://www.w3.org/2001/XMLSchema-instance";

        public ExecuteRequestTranslator(ContentHandler ch) {
            super(ch, null, null);
        }

        public void encode(Object o) throws IllegalArgumentException {
            GetCoverageRequest request = (GetCoverageRequest) o;
            encode(request);
        }

        private void encode(GetCoverageRequest request) {
            AttributesImpl attributes =
                    attributes(
                            "version",
                            "1.1.1",
                            "service",
                            "WCS",
                            "xmlns:xsi",
                            XSI_URI,
                            "xmlns",
                            WCS_URI,
                            "xmlns:ows",
                            OWS.NAMESPACE,
                            "xmlns:gml",
                            GML.NAMESPACE,
                            "xmlns:ogc",
                            OGC.NAMESPACE,
                            "xsi:schemaLocation",
                            WCS_URI + " " + "http://schemas.opengis.net/wcs/1.1.1/wcsAll.xsd");

            start("GetCoverage", attributes);
            element("ows:Identifier", request.coverage);
            CoverageInfo coverage = catalog.getCoverageByName(request.coverage);
            start("DomainSubset");
            handleSpatialSubset(request, coverage);
            end("DomainSubset");
            handleOutput(request);
            end("GetCoverage");
        }

        private void handleOutput(GetCoverageRequest request) {
            String format = request.outputFormat;
            final CoverageResponseDelegate encoder = responseFactory.encoderFor(format);
            String mime = encoder.getMimeType(request.outputFormat);
            start("Output", attributes("store", "true", "format", mime));
            if (request.targetCRS != null) {
                start("GridCRS");
                element("GridBaseCRS", epsgUrnCode(request.targetCRS));
                AffineTransform at = request.targetGridToWorld;
                if (at.getTranslateX() == 0
                        && at.getTranslateY() == 0
                        && at.getShearX() == 0
                        && at.getShearY() == 0) {
                    // simple grid mode
                    element("GridType", "urn:ogc:def:method:WCS:1.1:2dSimpleGrid");
                    element("GridOffsets", at.getScaleX() + " " + at.getScaleY());
                } else {
                    element("GridType", "urn:ogc:def:method:WCS:1.1:2dGridIn2dCrs");
                    element("GridOrigin", at.getTranslateX() + " " + at.getTranslateY());
                    element(
                            "GridOffsets",
                            at.getScaleX()
                                    + " "
                                    + at.getShearX()
                                    + " "
                                    + at.getShearY()
                                    + " "
                                    + at.getScaleY());
                }
                element("GridCS", "urn:ogc:def:cs:OGC:0.0:Grid2dSquareCS");
                end("GridCRS");
            }
            end("Output");
        }

        void handleSpatialSubset(GetCoverageRequest request, CoverageInfo coverage) {
            try {
                ReferencedEnvelope bounds = request.bounds;
                CoordinateReferenceSystem boundsCrs = bounds.getCoordinateReferenceSystem();
                final String epsgCode = epsgUrnCode(boundsCrs);
                bounds = bounds.transform(CRS.decode(epsgCode), true);
                start("ows:BoundingBox", attributes("crs", epsgCode));
                element("ows:LowerCorner", bounds.getMinX() + " " + bounds.getMinY());
                element("ows:UpperCorner", bounds.getMaxX() + " " + bounds.getMaxY());
                end("ows:BoundingBox");
            } catch (Exception e) {
                // should never happen, but anyways
                throw new RuntimeException(e);
            }
        }

        private String epsgUrnCode(CoordinateReferenceSystem boundsCrs) {
            try {
                int epsg = CRS.lookupEpsgCode(boundsCrs, false);
                return "urn:ogc:def:crs:EPSG::" + epsg;
            } catch (Exception e) {
                // should never happen, but anyways
                throw new RuntimeException(e);
            }
        }

        /** Helper to build a set of attributes out of a list of key/value pairs */
        AttributesImpl attributes(String... nameValues) {
            AttributesImpl atts = new AttributesImpl();

            for (int i = 0; i < nameValues.length; i += 2) {
                String name = nameValues[i];
                String valu = nameValues[i + 1];

                atts.addAttribute(null, null, name, null, valu);
            }

            return atts;
        }
    }
}
