/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wcs.web.demo;

import java.util.logging.Logger;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CoverageInfo;
import org.geotools.coverage.grid.GridEnvelope2D;
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
 * Helper class to turn a {@link GetCoverageRequest} into the corresponding WCS 1.0 GetCoverage xml
 *
 * @author Andrea Aime - GeoSolutions
 */
class WCS10GetCoverageTransformer extends TransformerBase {

    static final Logger LOGGER = Logging.getLogger(WCS10GetCoverageTransformer.class);
    private Catalog catalog;

    public WCS10GetCoverageTransformer(Catalog catalog) {
        this.catalog = catalog;
    }

    @Override
    public Translator createTranslator(ContentHandler handler) {
        return new ExecuteRequestTranslator(handler);
    }

    public class ExecuteRequestTranslator extends TranslatorSupport {

        /** wfs namespace */
        protected static final String WFS_URI = "http://www.opengis.net/wfs";

        protected static final String WCS_URI = "http://www.opengis.net/wcs";

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
                            "1.0.0",
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
                            WCS_URI + " " + "http://schemas.opengis.net/wcs/1.0.0/getCoverage.xsd");

            start("GetCoverage", attributes);
            element("sourceCoverage", request.coverage);
            CoverageInfo coverage = catalog.getCoverageByName(request.coverage);
            start("domainSubset");
            handleSpatialSubset(request, coverage);
            end("domainSubset");
            handleOutput(request);
            end("GetCoverage");
        }

        private void handleOutput(GetCoverageRequest request) {
            start("output");
            if (request.targetCRS != null) {
                element("crs", epsgCode(request.targetCRS));
            }
            element("format", request.outputFormat);
            end("output");
        }

        void handleSpatialSubset(GetCoverageRequest request, CoverageInfo coverage) {
            start("spatialSubset");

            // model space bounds
            final ReferencedEnvelope bounds = request.bounds;
            CoordinateReferenceSystem boundsCrs = bounds.getCoordinateReferenceSystem();
            final String epsgCode = epsgCode(boundsCrs);
            start("gml:Envelope", attributes("srsName", epsgCode));
            element("gml:pos", bounds.getMinX() + " " + bounds.getMinY());
            element("gml:pos", bounds.getMaxX() + " " + bounds.getMaxY());
            end("gml:Envelope");

            // the grid
            start("gml:Grid", attributes("dimension", "2"));

            // grid limits
            final GridEnvelope2D limits = request.sourceGridRange;
            start("gml:limits");
            start("gml:GridEnvelope");
            element("gml:low", (int) limits.getMinX() + " " + (int) limits.getMinY());
            element("gml:high", (int) limits.getMaxX() + " " + (int) limits.getMaxY());
            end("gml:GridEnvelope");
            end("gml:limits");

            // axis names
            CoordinateReferenceSystem gridCrs = coverage.getCRS();
            for (int dn = 0; dn < gridCrs.getCoordinateSystem().getDimension(); dn++) {
                String axisName = gridCrs.getCoordinateSystem().getAxis(dn).getAbbreviation();
                axisName = axisName.toLowerCase().startsWith("lon") ? "x" : axisName;
                axisName = axisName.toLowerCase().startsWith("lat") ? "y" : axisName;
                element("gml:axisName", axisName);
            }

            end("gml:Grid");

            end("spatialSubset");
        }

        private String epsgCode(CoordinateReferenceSystem boundsCrs) {
            try {
                int epsg = CRS.lookupEpsgCode(boundsCrs, false);
                final String epsgCode = "EPSG:" + epsg;
                return epsgCode;
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
