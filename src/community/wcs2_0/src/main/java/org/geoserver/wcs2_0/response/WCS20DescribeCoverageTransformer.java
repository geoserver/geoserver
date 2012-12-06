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
import org.geoserver.wcs2_0.util.NSNameResourceCodec;
import org.geoserver.wcs2_0.util.StringUtils;
import org.geotools.util.logging.Logging;
import org.geotools.xml.transform.TransformerBase;
import org.geotools.xml.transform.Translator;
import org.xml.sax.ContentHandler;
import org.xml.sax.helpers.AttributesImpl;


import static org.apache.commons.lang.StringUtils.isNotBlank;

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
                LayerInfo layer = getCoverage(encodedCoverageId);
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
            String encodedId = NSNameResourceCodec.encode(ci);
            coverageAttributes.addAttribute("", "gml:id", "gml:id",  "", encodedId);

            start("wcs:CoverageDescription", coverageAttributes);

//            handleBound(ci);

            element("wcs:CoverageId", encodedId);

            handleCoverageFunction(ci);
            handleMetadata(ci);
            handleDomainSet(ci);
            handleRangeType(ci);
            handleServiceParameters(ci);

            end("wcs:CoverageDescription");
        }

        private void handleBound(CoverageInfo ci) {
            start("gml:boundedBy");
//       <gml:boundedBy>
//            <gml:Envelope srsName="http://www.opengis.net/def/crs/EPSG/0/4326" axisLabels="Lat Long" uomLabels="deg deg" srsDimension="2">
//                <gml:lowerCorner>1 1</gml:lowerCorner>
//                <gml:upperCorner>5 3</gml:upperCorner>
//            </gml:Envelope>
//        </gml:boundedBy>
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
        private void handleDomainSet(CoverageInfo ci) {
            start("gml:domainSet");
            end("gml:domainSet");
        }
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
            List<LayerInfo> layers = NSNameResourceCodec.getLayers(catalog, encodedCoverageId);
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
    }

}
