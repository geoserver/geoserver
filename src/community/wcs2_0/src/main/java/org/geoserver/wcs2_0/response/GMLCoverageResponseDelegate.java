/*
 *    GeoTools - The Open Source Java GIS Toolkit
 *    http://geotools.org
 *
 *    (C) 2002-2011, Open Source Geospatial Foundation (OSGeo)
 *
 *    This library is free software; you can redistribute it and/or
 *    modify it under the terms of the GNU Lesser General Public
 *    License as published by the Free Software Foundation;
 *    version 2.1 of the License.
 *
 *    This library is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *    Lesser General Public License for more details.
 */
package org.geoserver.wcs2_0.response;

import java.awt.image.DataBuffer;
import java.awt.image.RenderedImage;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.measure.unit.Unit;
import javax.measure.unit.UnitFormat;
import javax.media.jai.PlanarImage;
import javax.media.jai.iterator.RectIter;
import javax.media.jai.iterator.RectIterFactory;
import javax.xml.transform.TransformerException;

import org.geoserver.platform.ServiceException;
import org.geoserver.wcs.responses.CoverageResponseDelegate;
import org.geoserver.wcs2_0.util.EnvelopeAxesLabelsMapper;
import org.geotools.coverage.GridSampleDimension;
import org.geotools.coverage.TypeMap;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.coverage.grid.GridEnvelope2D;
import org.geotools.coverage.grid.GridGeometry2D;
import org.geotools.geometry.GeneralEnvelope;
import org.geotools.referencing.CRS;
import org.geotools.referencing.CRS.AxisOrder;
import org.geotools.referencing.operation.transform.AffineTransform2D;
import org.geotools.resources.coverage.CoverageUtilities;
import org.geotools.util.NumberRange;
import org.geotools.xml.transform.TransformerBase;
import org.geotools.xml.transform.Translator;
import org.opengis.coverage.SampleDimension;
import org.opengis.coverage.SampleDimensionType;
import org.opengis.coverage.grid.GridEnvelope;
import org.opengis.metadata.spatial.PixelOrientation;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.crs.GeographicCRS;
import org.opengis.referencing.cs.CoordinateSystem;
import org.opengis.referencing.operation.MathTransform2D;
import org.vfny.geoserver.wcs.WcsException;
import org.xml.sax.ContentHandler;
import org.xml.sax.helpers.AttributesImpl;

/**
 * Encoding a {@link GridCoverage2D} as per WCS 2.0 GML format.
 * 
 * @author Simone Giannecchini, GeoSolutions SAS
 *
 */
public class GMLCoverageResponseDelegate implements CoverageResponseDelegate {

    /** FILE_EXTENSION */
    private static final String FILE_EXTENSION = "gml";
    
    /** MIME_TYPE */
    private static final String MIME_TYPE = "application/gml+xml";
    
    /** FORMAT_ALIASES */
    private static final List<String> FORMAT_ALIASES = Arrays.asList(FILE_EXTENSION,MIME_TYPE);
    
    final static String SRS_STARTER="http://www.opengis.net/def/crs/EPSG/0/";
    
    /**
     * 
     * @author Simone Giannecchini, GeoSolutions SAS
     *
     */
   private static class GMLTransformer extends TransformerBase{
        
        private EnvelopeAxesLabelsMapper envelopeDimensionsMapper;
        
        public GMLTransformer(EnvelopeAxesLabelsMapper envelopeDimensionsMapper ) {
            this.envelopeDimensionsMapper=envelopeDimensionsMapper;
        }
        
        private class GMLTranslator extends TranslatorSupport{

            public GMLTranslator(ContentHandler contentHandler) {
                super(contentHandler, null, null);
            }

            @Override
            public void encode(Object o) throws IllegalArgumentException {
                // TODO does the real encoding
                
                // is this a GridCoverage?
                if(!(o instanceof GridCoverage2D)){
                    throw new IllegalArgumentException("Provided object is not a GridCoverage2D:"+(o!=null?o.getClass().toString():"null"));
                }
                final GridCoverage2D gc2d=(GridCoverage2D) o;
                // we are going to use this name as an ID
                final String gcName= gc2d.getName().toString(Locale.getDefault());
                
                
                // get the crs and look for an EPSG code
                final CoordinateReferenceSystem crs = gc2d.getCoordinateReferenceSystem2D();
                List<String> axesNames = GMLTransformer.this.envelopeDimensionsMapper.getAxesNames(gc2d.getEnvelope2D(),true);
               
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
                

                // TODO do we miss any of them?
                final AttributesImpl attributes = new AttributesImpl();
                attributes.addAttribute("", "xmlns:gml", "xmlns:gml", "", "http://www.opengis.net/gml/3.2");
                attributes.addAttribute("", "xmlns:gmlcov", "xmlns:gmlcov", "", "http://www.opengis.net/gmlcov/1.0");
                attributes.addAttribute("", "xmlns:swe", "xmlns:swe", "", "http://www.opengis.net/swe/2.0");
                attributes.addAttribute("", "xmlns:xlink", "xmlns:xlink", "", "http://www.w3.org/1999/xlink");
                attributes.addAttribute("", "xmlns:xsi", "xmlns:xsi", "", "http://www.w3.org/2001/XMLSchema-instance");

                // using Name as the ID
                attributes.addAttribute("", "gml:id", "gml:id", "", gc2d.getName().toString(Locale.getDefault()));
                start("gml:RectifiedGridCoverage",attributes);
                
                // handle domain
                final StringBuilder builder= new StringBuilder();
                for(String axisName:axesNames){
                    builder.append(axisName).append(" ");
                }           
                String axesLabel=builder.substring(0, builder.length()-1);
                handleBoundedBy(gc2d, axisSwap,srsName,axesLabel);
                
                // handle domain
                builder.setLength(0);
                axesNames = GMLTransformer.this.envelopeDimensionsMapper.getAxesNames(gc2d.getEnvelope2D(),false);   
                for(String axisName:axesNames){
                    builder.append(axisName).append(" ");
                }           
                axesLabel=builder.substring(0, builder.length()-1);                
                handleDomainSet(gc2d,gcName,srsName,axisSwap);
                
                // handle rangetype
                handleRangeType(gc2d);
                
                // handle coverage function
                handleCoverageFunction(gc2d,axisSwap);
                
                // handle range
                handleRange(gc2d);
                
                // handle metadata OPTIONAL
                handleMetadata(gc2d);
                
                end("gml:RectifiedGridCoverage");
                
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
             * Encodes the Range as per the GML spec of the provided {@link GridCoverage2D}
             * 
             * @param gc2d the {@link GridCoverage2D} for which to encode the Range.
             */
            private void handleRange(GridCoverage2D gc2d) {
                
                // preamble
                start("gml:rangeSet");
                start("gml:DataBlock");
                start("gml:rangeParameters"); 
                end("gml:rangeParameters"); 

                start("tupleList");
                // walk through the coverage and spit it out!
                final RenderedImage raster= gc2d.getRenderedImage();
                final int numBands=raster.getSampleModel().getNumBands();
                final int dataType=raster.getSampleModel().getDataType();
                final double[] valuesD= new double[numBands];
                final int[] valuesI= new int[numBands];
                RectIter iterator = RectIterFactory.create(raster, PlanarImage.wrapRenderedImage(raster).getBounds());
                
                    iterator.startLines();
                    while (!iterator.finishedLines()) {
                        iterator.startPixels();
                        while (!iterator.finishedPixels()) {
                            switch (dataType) {
                            case DataBuffer.TYPE_BYTE:
                            case DataBuffer.TYPE_INT:
                            case DataBuffer.TYPE_SHORT:
                            case DataBuffer.TYPE_USHORT:
                                iterator.getPixel(valuesI);
                                for(int i=0;i<numBands;i++){
                                    // spit out
                                    chars(String.valueOf(valuesI[i]));
                                    if(i+1<numBands){
                                        chars(",");
                                    }
                                }
                                break;
                            case DataBuffer.TYPE_DOUBLE:
                            case DataBuffer.TYPE_FLOAT:
                                iterator.getPixel(valuesD);
                                for(int i=0;i<numBands;i++){
                                    // spit out
                                    chars(String.valueOf(valuesD[i]));
                                    if(i+1<numBands){
                                        chars(",");
                                    }
                                }                           
                                break;
                            default:
                                break;
                            }
                            // space as sample separator
                            chars(" ");
                            iterator.nextPixel();
                        }
                        iterator.nextLine();
                        chars("\n");
                    }

                end("tupleList");
                end("gml:DataBlock");
                end("gml:rangeSet");
                
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
                start("gml:rangeType");
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
                    handleSampleDimensionNilValues(gc2d,(GridSampleDimension) sd);      
                    
                    end("swe:Quantity");
                    end("swe:field");
                }
               
                end("swe:DataRecord");
                end("gml:rangeType");
                
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

            
        }
       
        @Override
        public Translator createTranslator(ContentHandler handler) {
            return new GMLTranslator(handler);
        }
        
    }

    /** Can be used to map dimensions name to indexes*/
    private EnvelopeAxesLabelsMapper envelopeDimensionsMapper;
    


    public GMLCoverageResponseDelegate(EnvelopeAxesLabelsMapper envelopeDimensionsMapper) {
        this.envelopeDimensionsMapper=envelopeDimensionsMapper;
        
    }

    @Override
    public boolean canProduce(String outputFormat) {
        return FORMAT_ALIASES.contains(outputFormat);
    }

    @Override
    public String getMimeType(String outputFormat) {
        return MIME_TYPE;
    }

    @Override
    public String getFileExtension(String outputFormat) {
        return FILE_EXTENSION;
    }

    @Override
    public void encode(GridCoverage2D coverage, String outputFormat,
            Map<String, String> econdingParameters, OutputStream output) throws ServiceException,
            IOException {
        
        
        final GMLTransformer transformer= new GMLTransformer(envelopeDimensionsMapper);
        transformer.setIndentation(4);
        try {
            transformer.transform(coverage, output);
        } catch (TransformerException e) {
            new WcsException(e);
        }

    }

    @Override
    public List<String> getOutputFormats() {
        return FORMAT_ALIASES;
    }

    @Override
    public boolean isAvailable() {
        return true;
    }

}
