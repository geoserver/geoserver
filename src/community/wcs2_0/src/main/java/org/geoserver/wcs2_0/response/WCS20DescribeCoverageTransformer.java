/* Copyright (c) 2012 TOPP - www.openplans.org.  All rights reserved.
 * This code is licensed under the GPL 2.0 license, availible at the root
 * application directory.
 */
package org.geoserver.wcs2_0.response;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import javax.measure.unit.Unit;
import javax.measure.unit.UnitFormat;

import net.opengis.wcs20.DescribeCoverageType;

import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CoverageInfo;
import org.geoserver.catalog.CoverageStoreInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.data.util.CoverageUtils;
import org.geoserver.wcs.CoverageCleanerCallback;
import org.geoserver.wcs.WCSInfo;
import org.geoserver.wcs.responses.CoverageResponseDelegateFinder;
import org.geoserver.wcs2_0.WCS20Const;
import org.geoserver.wcs2_0.exception.WCS20Exception;
import org.geoserver.wcs2_0.util.EnvelopeAxesLabelsMapper;
import org.geoserver.wcs2_0.util.NCNameResourceCodec;
import org.geoserver.wcs2_0.util.StringUtils;
import org.geotools.coverage.GridSampleDimension;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.coverage.grid.GridEnvelope2D;
import org.geotools.coverage.grid.GridGeometry2D;
import org.geotools.coverage.grid.io.AbstractGridCoverage2DReader;
import org.geotools.coverage.grid.io.AbstractGridFormat;
import org.geotools.factory.GeoTools;
import org.geotools.gce.imagemosaic.ImageMosaicFormat;
import org.geotools.geometry.GeneralEnvelope;
import org.geotools.referencing.CRS;
import org.geotools.referencing.CRS.AxisOrder;
import org.geotools.resources.coverage.CoverageUtilities;
import org.geotools.util.logging.Logging;
import org.geotools.xml.transform.Translator;
import org.opengis.coverage.SampleDimension;
import org.opengis.coverage.grid.Format;
import org.opengis.coverage.grid.GridEnvelope;
import org.opengis.parameter.ParameterValueGroup;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.cs.CoordinateSystem;
import org.opengis.referencing.datum.PixelInCell;
import org.opengis.referencing.operation.MathTransform;
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

    private WCSInfo wcs;

    private Catalog catalog;

    private CoverageResponseDelegateFinder responseFactory;
    
    /**
     * Creates a new WFSCapsTransformer object.
     */
    public WCS20DescribeCoverageTransformer(WCSInfo wcs, Catalog catalog, CoverageResponseDelegateFinder responseFactory,EnvelopeAxesLabelsMapper envelopeDimensionsMapper) {

        super(envelopeDimensionsMapper);
        this.wcs = wcs;
        this.catalog = catalog;
        this.responseFactory = responseFactory;
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
                    throw new RuntimeException(
                            "Unexpected error occurred during describe coverage xml encoding", e);
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
                gc2d = readSampleGridCoverage(ci);
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
                final String srsName = GMLCoverageResponseDelegate.SRS_STARTER + EPSGCode;
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

        /**
         * @param ci
         * @return
         */
        private GridCoverage2D readSampleGridCoverage(CoverageInfo ci)throws Exception {
            
            // get a reader for this coverage
            final CoverageStoreInfo store = (CoverageStoreInfo) ci.getStore();
            final AbstractGridCoverage2DReader reader = (AbstractGridCoverage2DReader) catalog
                    .getResourcePool().getGridCoverageReader(store, GeoTools.getDefaultHints());

            if (reader == null)
                throw new Exception("Unable to acquire a reader for this coverage with format: "
                        + store.getFormat().getName());

            // /////////////////////////////////////////////////////////////////////
            //
            // Now reading a fake small GridCoverage just to retrieve meta
            // information about bands:
            //
            // - calculating a new envelope which is just 5x5 pixels
            // - if it's a mosaic, limit the number of tiles we're going to read to one 
            //   (with time and elevation there might be hundreds of superimposed tiles)
            // - reading the GridCoverage subset
            //
            // /////////////////////////////////////////////////////////////////////

            final GeneralEnvelope originalEnvelope = reader.getOriginalEnvelope();
            final GridEnvelope originalRange = reader.getOriginalGridRange();            
            final Format coverageFormat = reader.getFormat();
            final GridCoverage2D gc;

            final ParameterValueGroup readParams = coverageFormat.getReadParameters();
            final Map parameters = CoverageUtils.getParametersKVP(readParams);
            final int minX = originalRange.getLow(0);
            final int minY = originalRange.getLow(1);
            final int width = originalRange.getSpan(0);
            final int height = originalRange.getSpan(1);
            final int maxX = minX + (width <= 5 ? width : 5);
            final int maxY = minY + (height <= 5 ? height : 5);

            // we have to be sure that we are working against a valid grid range.
            final GridEnvelope2D testRange = new GridEnvelope2D(minX, minY, maxX, maxY);

            // build the corresponding envelope
            final MathTransform gridToWorldCorner = reader.getOriginalGridToWorld(PixelInCell.CELL_CORNER);
            final GeneralEnvelope testEnvelope = CRS.transform(gridToWorldCorner, new GeneralEnvelope(testRange.getBounds()));
            testEnvelope.setCoordinateReferenceSystem(originalEnvelope.getCoordinateReferenceSystem());

            
            // make sure mosaics with many superimposed tiles won't blow up with 
            // a "too many open files" exception
            String maxAllowedTiles = ImageMosaicFormat.MAX_ALLOWED_TILES.getName().toString();
            if(parameters.keySet().contains(maxAllowedTiles)) {
                parameters.put(maxAllowedTiles, 1);
            }
            parameters.put(AbstractGridFormat.READ_GRIDGEOMETRY2D.getName().toString(), new GridGeometry2D(testRange, testEnvelope));

            // try to read this coverage
            return (GridCoverage2D) reader.read(CoverageUtils.getParameters(readParams, parameters, true));          
        }

        private void handleServiceParameters(CoverageInfo ci) {
            start("wcs:ServiceParameters");
            element("wcs:CoverageSubtype", "GridCoverage");
            element("wcs:nativeFormat", ci.getNativeFormat());
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
