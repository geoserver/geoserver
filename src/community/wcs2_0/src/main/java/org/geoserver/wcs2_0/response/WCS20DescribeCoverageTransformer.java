/* Copyright (c) 2012 TOPP - www.openplans.org.  All rights reserved.
 * This code is licensed under the GPL 2.0 license, availible at the root
 * application directory.
 */
package org.geoserver.wcs2_0.response;

import java.util.ArrayList;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import net.opengis.wcs20.DescribeCoverageType;

import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CoverageInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.wcs.WCSInfo;
import org.geoserver.wcs.responses.CoverageResponseDelegateFinder;
import org.geoserver.wcs2_0.WCS20Const;
import org.geoserver.wcs2_0.exception.WCS20Exception;
import org.geoserver.wcs2_0.util.NCNameResourceCodec;
import org.geoserver.wcs2_0.util.StringUtils;
import org.geotools.util.logging.Logging;
import org.geotools.xml.transform.TransformerBase;
import org.geotools.xml.transform.Translator;
import org.xml.sax.ContentHandler;
import org.xml.sax.helpers.AttributesImpl;


import static org.apache.commons.lang.StringUtils.isNotBlank;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.referencing.CRS;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

/**
 * Based on the <code>org.geotools.xml.transform</code> framework, does the
 * job of encoding a WCS 2.0.1 DescribeCoverage document.
 * 
 * @author Emanuele Tajariol (etj) - GeoSolutions
 */
public class WCS20DescribeCoverageTransformer extends TransformerBase {
    private static final Logger LOGGER = Logging.getLogger(WCS20DescribeCoverageTransformer.class
            .getPackage().getName());

    private WCSInfo wcs;

    private Catalog catalog;

    private CoverageResponseDelegateFinder responseFactory;

    /**
     * Creates a new WFSCapsTransformer object.
     */
    public WCS20DescribeCoverageTransformer(WCSInfo wcs, Catalog catalog, CoverageResponseDelegateFinder responseFactory) {
        super();
        this.wcs = wcs;
        this.catalog = catalog;
        this.responseFactory = responseFactory;
        setNamespaceDeclarationEnabled(false);
    }

    public Translator createTranslator(ContentHandler handler) {
        return new WCS20DescribeCoverageTranslator(handler);
    }

    private class WCS20DescribeCoverageTranslator extends TranslatorSupport {
        private DescribeCoverageType request;

        private String proxifiedBaseUrl;

        public WCS20DescribeCoverageTranslator(ContentHandler handler) {
            super(handler, null, null);
        }

        /**
         * Encode the object.

         */
        @Override
        public void encode(Object o) throws IllegalArgumentException {
            
            if (!(o instanceof DescribeCoverageType)) {
                throw new IllegalArgumentException(new StringBuffer("Not a GetCapabilitiesType: ")
                        .append(o).toString());
            }

            this.request = (DescribeCoverageType) o;

            final AttributesImpl attributes = WCS20Const.getDefaultNamespaces();

            // collect coverages
            List<String> badCoverageIds = new ArrayList<String>();
            List<LayerInfo> coverages = new ArrayList<LayerInfo>();

            for (String encodedCoverageId : (List<String>)request.getCoverageId()) {
                LayerInfo layer = NCNameResourceCodec.getCoverage(catalog, encodedCoverageId);
                if(layer != null) {
                    coverages.add(layer);
                } else {
                    badCoverageIds.add(encodedCoverageId);
                }
            }

            // any error?
            if( ! badCoverageIds.isEmpty() ) {
                String mergedIds = StringUtils.merge(badCoverageIds);
                throw new WCS20Exception("Could not find the requested coverage(s): " + mergedIds
                        , WCS20Exception.WCSExceptionCode.NoSuchCoverage, mergedIds);
            }

            // ok: build the response
            start("wcs:CoverageDescriptions", attributes);
            for (LayerInfo layer : coverages) {
                CoverageInfo ci = catalog.getCoverageByName(layer.prefixedName());
                try {
                    handleCoverageDescription(ci);
                } catch (Exception e) {
                    throw new RuntimeException(
                            "Unexpected error occurred during describe coverage xml encoding", e);
                }
            }
            end("wcs:CoverageDescriptions");
        }

        void handleCoverageDescription(CoverageInfo ci) throws Exception {
            final AttributesImpl coverageAttributes = new AttributesImpl();
            String encodedId = NCNameResourceCodec.encode(ci);
            coverageAttributes.addAttribute("", "gml:id", "gml:id",  "", encodedId);

            start("wcs:CoverageDescription", coverageAttributes);

            handleBoundedBy(ci);

            element("wcs:CoverageId", encodedId);

            handleCoverageFunction(ci);
            handleMetadata(ci);
            handleDomainSet(ci);
            handleRangeType(ci);
            handleServiceParameters(ci);

            end("wcs:CoverageDescription");
        }

        /**
         * e.g.:<pre> {@code
         * <gml:boundedBy>
         *    <gml:Envelope srsName="http://www.opengis.net/def/crs/EPSG/0/4326" axisLabels="Lat Long" uomLabels="deg deg" srsDimension="2">
         *       <gml:lowerCorner>1 1</gml:lowerCorner>
         *       <gml:upperCorner>5 3</gml:upperCorner>
         *    </gml:Envelope>
         * </gml:boundedBy>
         * }
         * </pre>
        */
        private void handleBoundedBy(CoverageInfo ci) {
            // retrieve info
            final ReferencedEnvelope latLonBoundingBox = ci.getLatLonBoundingBox();
            final CoordinateReferenceSystem crs = latLonBoundingBox.getCoordinateReferenceSystem();

            // setup vars
            final String srsName = "http://www.opengis.net/def/crs/EPSG/0/4326";
            final String axisLabels="Lat Long"; // should also add elev? time?
            final String uomLabels="deg deg";  // should also add elev? time?
            final int srsDimension = crs.getCoordinateSystem().getDimension();  // should also add time?

            final String lower = new StringBuilder()
                    .append(latLonBoundingBox.getLowerCorner().getOrdinate(0))
                    .append(" ")
                    .append(latLonBoundingBox.getLowerCorner().getOrdinate(1))
                    .toString();

            final String upper = new StringBuilder()
                    .append(latLonBoundingBox.getUpperCorner().getOrdinate(0))
                    .append(" ")
                    .append(latLonBoundingBox.getUpperCorner().getOrdinate(1))
                    .toString();

            // build the fragment
            final AttributesImpl envelopeAttrs = new AttributesImpl();
            envelopeAttrs.addAttribute("", "srsName", "srsName", "", srsName);
            envelopeAttrs.addAttribute("", "axisLabels", "axisLabels", "", axisLabels);
            envelopeAttrs.addAttribute("", "uomLabels", "uomLabels", "", uomLabels);
            envelopeAttrs.addAttribute("", "srsDimension", "srsDimension", "", String.valueOf(srsDimension));

            start("gml:boundedBy");
            start("gml:Envelope", envelopeAttrs);

            element("gml:lowerCorner", lower);
            element("gml:upperCorner", upper);

            end("gml:Envelope");
            end("gml:boundedBy");
        }

        private void handleCoverageFunction(CoverageInfo ci) {
            start("gml:coverageFunction");
            start("gml:GridFunction");

            element("gml:sequenceRule", "Linear"); // minOccurs 0, default Linear

            start("gml:startPoint");   // minOccurs 0
            
            end("gml:startPoint");
            end("gml:GridFunction");
            end("gml:coverageFunction");
        }
        
        private void handleMetadata(CoverageInfo ci) {

        }

        /**
         * e.g.:<pre> {@code
         * <gml:domainSet>
         *    <gml:Grid gml:id="gr0001_C0001" dimension="2">
         *       <gml:limits>
         *          <gml:GridEnvelope>
         *             <gml:low>1 1</gml:low>
         *             <gml:high>5 3</gml:high>
         *          </gml:GridEnvelope>
         *       </gml:limits>
         *       <gml:axisLabels>Lat Long</gml:axisLabels>
         *    </gml:Grid>
         * </gml:domainSet>
         * }
         * </pre>
         */
        private void handleDomainSet(CoverageInfo ci) {
            // retrieve info
            final ReferencedEnvelope latLonBoundingBox = ci.getLatLonBoundingBox();
            final CoordinateReferenceSystem crs = latLonBoundingBox.getCoordinateReferenceSystem();

            // setup vars
            final String gridId = "grid00__" + NCNameResourceCodec.encode(ci);
            final String axisLabels = "Lat Long"; // should also add elev? time?
            final int gridDimension = ci.getGrid().getGridRange().getDimension();

            final StringBuilder lowSb = new StringBuilder();
            for (int i : ci.getGrid().getGridRange().getLow().getCoordinateValues()) {
                lowSb.append(i).append(' ');
            }
            final StringBuilder highSb = new StringBuilder();
            for (int i : ci.getGrid().getGridRange().getHigh().getCoordinateValues()) {
                highSb.append(i).append(' ');
            }

            // build the fragment
            final AttributesImpl gridAttrs = new AttributesImpl();
            gridAttrs.addAttribute("", "gml:id", "gml:id", "", gridId);
            gridAttrs.addAttribute("", "dimension", "dimension", "", String.valueOf(gridDimension));

            start("gml:domainSet");
            start("gml:Grid", gridAttrs);
            start("gml:limits");
            start("gml:GridEnvelope");
            element("gml:low", lowSb.toString().trim());
            element("gml:high", highSb.toString().trim());
            end("gml:GridEnvelope");
            end("gml:limits");
            element("gml:axisLabels", axisLabels);
            end("gml:Grid");
            end("gml:domainSet");
        }

        /**
         * e.g.:<pre> {@code
         * <gmlcov:rangeType>
         *    <swe:DataRecord>
         *        <swe:field name="singleBand">
         *           <swe:Quantity definition="http://www.opengis.net/def/property/OGC/0/Radiance">
         *               <gml:description>Panchromatic Channel</gml:description>
         *               <gml:name>single band</gml:name>
         *               <swe:uom code="W/cm2"/>
         *               <swe:constraint>
         *                   <swe:AllowedValues>
         *                       <swe:interval>0 255</swe:interval>
         *                       <swe:significantFigures>3</swe:significantFigures>
         *                   </swe:AllowedValues>
         *               </swe:constraint>
         *           </swe:Quantity>
         *        </swe:field>
         *    </swe:DataRecord>
         * </gmlcov:rangeType>
         * }
         * </pre>
         */
        private void handleRangeType(CoverageInfo ci) {
            start("gmlcov:rangeType");
            end("gmlcov:rangeType");
        }
        private void handleServiceParameters(CoverageInfo ci) {
            start("wcs:ServiceParameters");
            element("wcs:CoverageSubtype", "GridCoverage");
            element("wcs:nativeFormat", ci.getNativeFormat());
            end("wcs:ServiceParameters");
        }


        /**
         * Writes the element if and only if the content is not null and not
         * empty
         * 
         * @param elementName
         * @param content
         */
        private void elementIfNotEmpty(String elementName, String content) {
            if ( isNotBlank(content) )
                element(elementName, content);
        }
        private LayerInfo getCoverage(String encodedCoverageId) throws WCS20Exception {
            List<LayerInfo> layers = NCNameResourceCodec.getLayers(catalog, encodedCoverageId);
            if(layers == null)
                return null;

            LayerInfo ret = null;

            for (LayerInfo layer : layers) {
                if ( layer.getType() == LayerInfo.Type.RASTER) {
                    if(ret == null) {
                        ret = layer;
                    } else {
                        LOGGER.warning("Multiple coverages found for NSName '" + encodedCoverageId + "': "
                                + ret.prefixedName() + " is selected, "
                                + layer.prefixedName() + " will be ignored");
                    }
                }
            }

            return ret;
        }

        private String urnIdentifier(final CoordinateReferenceSystem crs) throws FactoryException {
            String authorityAndCode = CRS.lookupIdentifier(crs, false);
            String code = authorityAndCode.substring(authorityAndCode.lastIndexOf(":") + 1);
            // we don't specify the version, but we still need to put a space
            // for it in the urn form, that's why we have :: before the code
//            return "urn:ogc:def:crs:EPSG::" + code;
            return "http://www.opengis.net/def/crs/EPSG/0/" + code;
        }

    }

}
