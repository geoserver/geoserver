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
import org.geotools.coverage.TypeMap;
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
import org.geotools.referencing.operation.transform.AffineTransform2D;
import org.geotools.resources.coverage.CoverageUtilities;
import org.geotools.util.NumberRange;
import org.geotools.util.logging.Logging;
import org.geotools.xml.transform.TransformerBase;
import org.geotools.xml.transform.Translator;
import org.opengis.coverage.SampleDimension;
import org.opengis.coverage.SampleDimensionType;
import org.opengis.coverage.grid.Format;
import org.opengis.coverage.grid.GridEnvelope;
import org.opengis.metadata.spatial.PixelOrientation;
import org.opengis.parameter.ParameterValueGroup;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.crs.GeographicCRS;
import org.opengis.referencing.cs.CoordinateSystem;
import org.opengis.referencing.datum.PixelInCell;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.MathTransform2D;
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
public class WCS20DescribeCoverageTransformer extends TransformerBase {
    private static final Logger LOGGER = Logging.getLogger(WCS20DescribeCoverageTransformer.class
            .getPackage().getName());

    private WCSInfo wcs;

    private Catalog catalog;

    private CoverageResponseDelegateFinder responseFactory;

    /** Utility class to map envelope dimension*/
    private EnvelopeAxesLabelsMapper envelopeDimensionsMapper;
    
    /**
     * Creates a new WFSCapsTransformer object.
     */
    public WCS20DescribeCoverageTransformer(WCSInfo wcs, Catalog catalog, CoverageResponseDelegateFinder responseFactory,EnvelopeAxesLabelsMapper envelopeDimensionsMapper) {
        this.wcs = wcs;
        this.catalog = catalog;
        this.responseFactory = responseFactory;
        this.envelopeDimensionsMapper=envelopeDimensionsMapper;
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

        private void handleCoverageDescription(CoverageInfo ci)  {

            // read a small portion of the underlying coverage
            GridCoverage2D gc2d=null;
            try{
                gc2d=readSampleGridCoverage(ci);
            if(gc2d==null){
                throw new WCS20Exception("Unable to read sample coverage for "+ci.getName());
            }
            // get the crs and look for an EPSG code
            final CoordinateReferenceSystem crs = gc2d.getCoordinateReferenceSystem2D();
            List<String> axesNames = envelopeDimensionsMapper.getAxesNames(gc2d.getEnvelope2D(),true);
           
            // lookup EPSG code
            Integer EPSGCode=null;
            try {
                EPSGCode = CRS.lookupEpsgCode(crs, false);
            } catch (FactoryException e) {
                throw new IllegalStateException("Unable to lookup epsg code for this CRS:"+crs,e);
            }
            if(EPSGCode==null){
                throw new IllegalStateException("Unable to lookup epsg code for this CRS:"+crs);
            }                
            final String srsName = GMLCoverageResponseDelegate.SRS_STARTER+EPSGCode;                
            // handle axes swap for geographic crs
            final boolean axisSwap = CRS.getAxisOrder(crs).equals(AxisOrder.EAST_NORTH);  
            
            
            //encoding ID of the coverage
            final AttributesImpl coverageAttributes = new AttributesImpl();
            String encodedId = NCNameResourceCodec.encode(ci);
            coverageAttributes.addAttribute("", "gml:id", "gml:id",  "", encodedId);

            // starting encoding
            start("wcs:CoverageDescription", coverageAttributes);

            
            // handle domain
            final StringBuilder builder= new StringBuilder();
            for(String axisName:axesNames){
                builder.append(axisName).append(" ");
            }           
            String axesLabel=builder.substring(0, builder.length()-1);
            handleBoundedBy(gc2d, axisSwap,srsName,axesLabel);
            
            //coverage id
            element("wcs:CoverageId", encodedId);

            // handle coverage function
            handleCoverageFunction(gc2d,axisSwap);

            // metadata 
            handleMetadata(gc2d);
            
            // handle domain
            builder.setLength(0);
            axesNames = envelopeDimensionsMapper.getAxesNames(gc2d.getEnvelope2D(),false);   
            for(String axisName:axesNames){
                builder.append(axisName).append(" ");
            }           
            axesLabel=builder.substring(0, builder.length()-1);                
            handleDomainSet(gc2d,encodedId,srsName,axisSwap);

            
            // handle rangetype
            handleRangeType(gc2d);
            
            //service parameters
            handleServiceParameters(ci);

            end("wcs:CoverageDescription");
            }catch (Exception e) {
                throw new WcsException(e);
            } finally{
                if(gc2d!=null   ){
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
         * Encodes the boundedBy element
         * 
         * e.g.:
         * 
         * <pre>
         * {@code
         * <gml:boundedBy>
         *    <gml:Envelope srsName="http://www.opengis.net/def/crs/EPSG/0/4326" axisLabels="Lat Long" uomLabels="deg deg" srsDimension="2">
         *       <gml:lowerCorner>1 1</gml:lowerCorner>
         *       <gml:upperCorner>5 3</gml:upperCorner>
         *    </gml:Envelope>
         * </gml:boundedBy>
         * }
         * </pre>
         * 
         * @param gc2d
         * @param ePSGCode 
         * @param axisSwap 
         * @param srsName
         * @param axesNames 
         * @param axisLabels 
         */
        private void handleBoundedBy(GridCoverage2D gc2d, boolean axisSwap, String srsName, String axisLabels) {
            
            final GeneralEnvelope envelope=new GeneralEnvelope(gc2d.getEnvelope());
            final CoordinateReferenceSystem crs = gc2d.getCoordinateReferenceSystem2D();
            final CoordinateSystem cs= crs.getCoordinateSystem();    
        
            // TODO time
            final String uomLabels=extractUoM(crs,cs.getAxis(axisSwap?1:0).getUnit())+ " "+extractUoM(crs,cs.getAxis(axisSwap?0:1).getUnit());  
            final int srsDimension = cs.getDimension(); 
        
            final String lower = new StringBuilder()
                    .append(envelope.getLowerCorner().getOrdinate(axisSwap?1:0))
                    .append(" ")
                    .append(envelope.getLowerCorner().getOrdinate(axisSwap?0:1))
                    .toString();
        
            final String upper = new StringBuilder()
                    .append(envelope.getUpperCorner().getOrdinate(axisSwap?1:0))
                    .append(" ")
                    .append(envelope.getUpperCorner().getOrdinate(axisSwap?0:1))
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

        /**
         * Encodes the DomainSet as per the GML spec of the provided {@link GridCoverage2D}
         * 
         * e.g.:
         * 
         * <pre>
         * {@code
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
         * 
         * 
         * @param gc2d the {@link GridCoverage2D} for which to encode the DomainSet.
         * @param srsName 
         * @param axesSwap 
         */
        private void handleDomainSet(GridCoverage2D gc2d, String gcName, String srsName, boolean axesSwap) {
            // retrieve info
            
            final GridGeometry2D gg2D=gc2d.getGridGeometry();
        
            // setup vars
            final String gridId = "grid00__" + gcName;
            
            
            // Grid Envelope 
            final GridEnvelope gridEnvelope= gg2D.getGridRange();
            final int gridDimension = gc2d.getDimension();
        
            final StringBuilder lowSb = new StringBuilder();
            for (int i : gridEnvelope.getLow().getCoordinateValues()) {
                lowSb.append(i).append(' ');
            }
            final StringBuilder highSb = new StringBuilder();
            for (int i : gridEnvelope.getHigh().getCoordinateValues()) {
                highSb.append(i).append(' ');
            }
        
            // build the fragment
            final AttributesImpl gridAttrs = new AttributesImpl();
            gridAttrs.addAttribute("", "gml:id", "gml:id", "", gridId);
            gridAttrs.addAttribute("", "dimension", "dimension", "", String.valueOf(gridDimension));
        
            start("gml:domainSet");
            start("gml:RectifiedGrid", gridAttrs);
            start("gml:limits");
            
            // GridEnvelope
            start("gml:GridEnvelope");
            element("gml:low", lowSb.toString().trim());
            element("gml:high", highSb.toString().trim());            
            end("gml:GridEnvelope");
        
            end("gml:limits");    
            
            // Axis Label              
            element("gml:axisLabels", "i j");
            
            final MathTransform2D transform = gg2D.getGridToCRS2D(PixelOrientation.UPPER_LEFT);
            if(!(transform instanceof AffineTransform2D)){
                throw new IllegalStateException("Invalid grid to worl provided:"+transform.toString());
            }
            final AffineTransform2D g2W=(AffineTransform2D)transform;
            
            // Origin
            // we use ULC as per our G2W transformation
            final AttributesImpl pointAttr = new AttributesImpl();
            pointAttr.addAttribute("", "gml:id", "gml:id", "", "p00_"+gcName);
            pointAttr.addAttribute("", "srsName", "srsName", "", srsName);                
            start("gml:origin");
            start("gml:Point",pointAttr);                
            element("gml:pos",axesSwap?g2W.getTranslateY()+" "+g2W.getTranslateX():g2W.getTranslateX()+" "+g2W.getTranslateY()); 
            end("gml:Point");                
            end("gml:origin");
            
            // Offsets
            final AttributesImpl offsetAttr = new AttributesImpl();
            offsetAttr.addAttribute("", "srsName", "srsName", "", srsName);  
        
            // notice the orientation of the transformation I create. The origin of the coordinates
            // in this grid is not at UPPER LEFT like in our grid to world but at LOWER LEFT !!!                
            element("gml:offsetVector", Double.valueOf(axesSwap?g2W.getShearX():g2W.getScaleX())+" "+Double.valueOf(axesSwap?g2W.getScaleX():g2W.getShearX()),offsetAttr); 
            element("gml:offsetVector", Double.valueOf(axesSwap?g2W.getScaleY():g2W.getShearY())+" "+Double.valueOf(axesSwap?g2W.getShearY():g2W.getScaleY()),offsetAttr);                 
            end("gml:RectifiedGrid");
            end("gml:domainSet");
        
        }

        /**
         * Returns a beautiful String representation for the provided {@link Unit}
         * @param crs
         * @param uom
         * @return
         */
        private String extractUoM(CoordinateReferenceSystem crs, Unit<?> uom) {
            // special handling for Degrees
            if(crs instanceof GeographicCRS){
                return "Deg";
            }
            return UnitFormat.getInstance().format(uom);
        }

        /**
         * Encodes the RangeType as per the GML spec of the provided {@link GridCoverage2D}
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
        private void handleRangeType(GridCoverage2D gc2d) {
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

        /**
         * @param sd
         */
        private void handleSampleDimensionNilValues(GridCoverage2D gc2d,GridSampleDimension sd) {
            start("swe:nilValues");
            start("swe:NilValues");
            
            // do we have already a a NO_DATA value at hand?
            if(gc2d.getProperties().containsKey("GC_NODATA")){
                
                String nodata = (String)gc2d.getProperties().get("GC_NODATA"); // TODO test me
                final AttributesImpl nodataAttr = new AttributesImpl();
                nodataAttr.addAttribute("", "reason", "reason", "", "http://www.opengis.net/def/nil/OGC/0/unknown");                     
                element("swe:nilValue", nodata,nodataAttr);
                // done
                return;
                
            } else {
                // check SD
                final double nodataValues[]=sd.getNoDataValues();
                if(nodataValues!=null&& nodataValues.length>0){
                    
                    for(double nodata:nodataValues){
                        final AttributesImpl nodataAttr = new AttributesImpl();
                        nodataAttr.addAttribute("", "reason", "reason", "", "http://www.opengis.net/def/nil/OGC/0/unknown");                     
                        element("swe:nilValue", String.valueOf(nodata),nodataAttr);                            
                    }
                    // done
                    return;
                } else {
                    
                    // let's suggest some meaningful value from the data type of the underlying image
                    Number nodata = CoverageUtilities.suggestNoDataValue(gc2d.getRenderedImage().getSampleModel().getDataType());
                    final AttributesImpl nodataAttr = new AttributesImpl();
                    nodataAttr.addAttribute("", "reason", "reason", "", "http://www.opengis.net/def/nil/OGC/0/unknown");                     
                    element("swe:nilValue", nodata.toString(),nodataAttr);
                }
            }
            
            
            end("swe:NilValues");
            end("swe:nilValues");
            
        }

        /**
         * Tries to encode a meaningful range for a {@link SampleDimension}.
         * 
         * @param sd the {@link SampleDimension} to encode a meaningful range for.
         */
        private void handleSampleDimensionRange(GridSampleDimension sd) {
        
            final SampleDimensionType sdType=sd.getSampleDimensionType();
            final NumberRange<? extends Number> indicativeRange = TypeMap.getRange(sdType);
            start("swe:interval");
            chars(indicativeRange.getMinValue()+" "+indicativeRange.getMaxValue());
            end("swe:interval");
            
            
            
        }

        /**
                     * Encoding eventual metadata that come along with this coverage
                     * 
                     * <pre>
                     * {@code
                     * <gmlcov:metadata>
                     *    <gmlcov:Extension>
                     *       <myNS:metadata>Some metadata ...</myNS:metadata>
                     *    </gmlcov:Extension>
                     * </gmlcov:metadata>
                     * }
                     * </pre>
                     * 
                     * @param gc2d
                     */
                    private void handleMetadata(GridCoverage2D gc2d) {
                        start("gmlcov:metadata");
                        start("gmlcov:Extension");
                        // encode properties of coverage
                        
        //                start("myNS:metadata");
        //                
        //                end("myNS:metadata");
        
                        end("gmlcov:Extension");
                        end("gmlcov:metadata");
                    }

        /**
         * Encode the coverage function or better the GridFunction as per clause 19.3.12 of GML 3.2.1
         * which helps us with indicating in which way we traverse the data.
         * 
         * <p>
         * Notice that we use the axisOrder to actually <strong>always</strong> encode data 
         * il easting,northing, hence in case of a northing,easting crs we use a reversed order 
         * to indicate that we always walk on the raster columns first.
         * 
         * <p>
         * In cases where the coordinates increases in the opposite order ho our walk
         * the offsetVectors of the RectifiedGrid will do the rest.
         * 
         * @param gc2d
         * @param axisSwap
         */
        private void handleCoverageFunction(GridCoverage2D gc2d, boolean axisSwap) {
            start("gml:coverageFunction");
            start("gml:GridFunction");
        
            // build the fragment
            final AttributesImpl gridAttrs = new AttributesImpl();
            gridAttrs.addAttribute("", "axisOrder", "axisOrder", "", axisSwap?"+2 +1":"+1 +2"); 
            element("gml:sequenceRule", "Linear",gridAttrs); // minOccurs 0, default Linear
            final GridEnvelope2D ge2D=gc2d.getGridGeometry().getGridRange2D();
            element("gml:startPoint", ge2D.x+" "+ge2D.y); // we start at minx, miny (this is optional though)
        
            
            end("gml:GridFunction");
            end("gml:coverageFunction");
        }

    }

}
