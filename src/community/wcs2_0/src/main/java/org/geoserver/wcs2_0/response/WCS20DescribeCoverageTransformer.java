/* Copyright (c) 2001 - 2013 OpenPlans - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wcs2_0.response;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.measure.unit.Unit;
import javax.measure.unit.UnitFormat;

import net.opengis.wcs20.DescribeCoverageType;

import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CoverageInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.wcs.CoverageCleanerCallback;
import org.geoserver.wcs.WCSInfo;
import org.geoserver.wcs.responses.CoverageResponseDelegateFinder;
import org.geoserver.wcs2_0.GetCoverage;
import org.geoserver.wcs2_0.WCS20Const;
import org.geoserver.wcs2_0.exception.WCS20Exception;
import org.geoserver.wcs2_0.util.EnvelopeAxesLabelsMapper;
import org.geoserver.wcs2_0.util.NCNameResourceCodec;
import org.geoserver.wcs2_0.util.RequestUtils;
import org.geoserver.wcs2_0.util.StringUtils;
import org.geotools.coverage.GridSampleDimension;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.data.DataUtilities;
import org.geotools.referencing.CRS;
import org.geotools.referencing.CRS.AxisOrder;
import org.geotools.util.logging.Logging;
import org.geotools.xml.transform.Translator;
import org.opengis.coverage.SampleDimension;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.vfny.geoserver.wcs.WcsException;
import org.xml.sax.ContentHandler;
import org.xml.sax.helpers.AttributesImpl;

/**
 * Based on the <code>org.geotools.xml.transform</code> framework, does the
 * job of encoding a WCS 2.0.1 DescribeCoverage document.
 * 
 * @author Emanuele Tajariol (etj) - GeoSolutions
 * @author Simone Giannecchini, GeoSolutions
 */
public class WCS20DescribeCoverageTransformer extends GMLTransformer {
    public static final Logger LOGGER = Logging.getLogger(WCS20DescribeCoverageTransformer.class
            .getPackage().getName());
    
    private MIMETypeMapper mimemapper;
    private WCSInfo wcs;

    private Catalog catalog;

    private CoverageResponseDelegateFinder responseFactory;
    
    /**
     * Creates a new WFSCapsTransformer object.
     * @param mimemapper 
     */
    public WCS20DescribeCoverageTransformer(WCSInfo wcs, Catalog catalog, CoverageResponseDelegateFinder responseFactory,EnvelopeAxesLabelsMapper envelopeDimensionsMapper, MIMETypeMapper mimemapper) {

        super(envelopeDimensionsMapper);
        this.wcs = wcs;
        this.catalog = catalog;
        this.responseFactory = responseFactory;
        this.mimemapper=mimemapper;
        setNamespaceDeclarationEnabled(false);
    }

    public Translator createTranslator(ContentHandler handler) {
        return new WCS20DescribeCoverageTranslator(handler);
    }

    public class WCS20DescribeCoverageTranslator extends GMLTranslator {
        private DescribeCoverageType request;

        private String proxifiedBaseUrl;

        public WCS20DescribeCoverageTranslator(ContentHandler handler) {
            super(handler);
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
            attributes.addAttribute("", "xmlns:swe", "xmlns:swe", "", "http://www.opengis.net/swe/2.0");

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
                        , WCS20Exception.WCS20ExceptionCode.NoSuchCoverage, mergedIds);
            }

            // ok: build the response
            start("wcs:CoverageDescriptions", attributes);
            for (LayerInfo layer : coverages) {
                CoverageInfo ci = catalog.getCoverageByName(layer.prefixedName());
                try {
                    handleCoverageDescription(ci);
                } catch (Exception e) {
                    throw new RuntimeException("Unexpected error occurred during describe coverage xml encoding", e);
                }
            }
            end("wcs:CoverageDescriptions");
        }

        /**
         * 
         * @param ci
         */
        private void handleCoverageDescription(CoverageInfo ci) {

            // read a small portion of the underlying coverage
            GridCoverage2D gc2d = null;
            try {
                gc2d = RequestUtils.readSampleGridCoverage(ci);
                if (gc2d == null) {
                    throw new WCS20Exception("Unable to read sample coverage for " + ci.getName());
                }
                // get the crs and look for an EPSG code
                final CoordinateReferenceSystem crs = gc2d.getCoordinateReferenceSystem2D();
                List<String> axesNames = envelopeDimensionsMapper.getAxesNames(
                        gc2d.getEnvelope2D(), true);

                // lookup EPSG code
                Integer EPSGCode = null;
                try {
                    EPSGCode = CRS.lookupEpsgCode(crs, false);
                } catch (FactoryException e) {
                    throw new IllegalStateException("Unable to lookup epsg code for this CRS:"
                            + crs, e);
                }
                if (EPSGCode == null) {
                    throw new IllegalStateException("Unable to lookup epsg code for this CRS:"
                            + crs);
                }
                final String srsName = GetCoverage.SRS_STARTER + EPSGCode;
                // handle axes swap for geographic crs
                final boolean axisSwap = CRS.getAxisOrder(crs).equals(AxisOrder.EAST_NORTH);

                // encoding ID of the coverage
                final AttributesImpl coverageAttributes = new AttributesImpl();
                String encodedId = NCNameResourceCodec.encode(ci);
                coverageAttributes.addAttribute("", "gml:id", "gml:id", "", encodedId);

                // starting encoding
                start("wcs:CoverageDescription", coverageAttributes);

                // handle domain
                final StringBuilder builder = new StringBuilder();
                for (String axisName : axesNames) {
                    builder.append(axisName).append(" ");
                }
                String axesLabel = builder.substring(0, builder.length() - 1);
                handleBoundedBy(gc2d, axisSwap, srsName, axesLabel);

                // coverage id
                element("wcs:CoverageId", encodedId);

                // handle coverage function
                handleCoverageFunction(gc2d, axisSwap);

                // metadata
                handleMetadata(gc2d);

                // handle domain
                builder.setLength(0);
                axesNames = envelopeDimensionsMapper.getAxesNames(gc2d.getEnvelope2D(), false);
                for (String axisName : axesNames) {
                    builder.append(axisName).append(" ");
                }
                axesLabel = builder.substring(0, builder.length() - 1);
                handleDomainSet(gc2d, encodedId, srsName, axisSwap);

                // handle rangetype
                handleRangeType(gc2d);

                // service parameters
                handleServiceParameters(ci);

                end("wcs:CoverageDescription");
            } catch (Exception e) {
                throw new WcsException(e);
            } finally {
                if (gc2d != null) {
                    CoverageCleanerCallback.addCoverages(gc2d);
                }
            }
        }

        private void handleServiceParameters(CoverageInfo ci) throws IOException {
            start("wcs:ServiceParameters");
            element("wcs:CoverageSubtype", "RectifiedGridCoverage");
            
            final String mapNativeFormat = mimemapper.mapNativeFormat(ci);
            if(mapNativeFormat==null){
                throw new WCS20Exception("Unable to create mime type for coverageinfo: "+ci.toString());
            }
            element("wcs:nativeFormat",mapNativeFormat);
            end("wcs:ServiceParameters");
        }

        /**
         * Encodes the RangeType as per the {@link DescribeCoverageType}WCS spec of the provided {@link GridCoverage2D}
         * 
         * e.g.:
         * 
         * <pre>
         * {@code
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
         * 
         * @param gc2d the {@link GridCoverage2D} for which to encode the RangeType.
         */
        public void handleRangeType(GridCoverage2D gc2d) {
            start("gmlcov:rangeType");
            start("swe:DataRecord");
            
            // get bands
            final SampleDimension[] bands= gc2d.getSampleDimensions();
            
            // handle bands
            for(SampleDimension sd:bands){
                final AttributesImpl fieldAttr = new AttributesImpl();
                fieldAttr.addAttribute("", "name", "name", "", sd.getDescription().toString());  // TODO NCNAME?  TODO Use Band[i] convention?                
                start("swe:field",fieldAttr);
                
                start("swe:Quantity");
                
                // Description
                start("swe:description");
                chars(sd.toString());// TODO can we make up something better??
                end("swe:description");
                
                //UoM
                final AttributesImpl uomAttr = new AttributesImpl();
                final Unit<?> uom=sd.getUnits();
                uomAttr.addAttribute("", "code", "code", "", uom==null?"W.m-2.Sr-1":UnitFormat.getInstance().format(uom)); 
                start("swe:uom",uomAttr);
                end("swe:uom");
                
                // constraint on values
                start("swe:constraint");
                start("swe:AllowedValues");
                handleSampleDimensionRange((GridSampleDimension) sd);// TODO make  this generic
                end("swe:AllowedValues");
                end("swe:constraint");
                
                // nil values
//                handleSampleDimensionNilValues(gc2d,(GridSampleDimension) sd);
                
                end("swe:Quantity");
                end("swe:field");
            }
        
            end("swe:DataRecord");
            end("gmlcov:rangeType");
            
        }

    }

}
