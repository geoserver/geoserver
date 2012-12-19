package org.geoserver.wcs2_0;

import java.awt.Rectangle;
import java.awt.geom.AffineTransform;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import javax.media.jai.Interpolation;
import javax.media.jai.InterpolationNearest;
import javax.media.jai.JAI;
import javax.media.jai.Warp;
import javax.media.jai.WarpAffine;
import javax.media.jai.operator.AffineDescriptor;
import javax.media.jai.operator.ScaleDescriptor;
import javax.media.jai.operator.WarpDescriptor;

import net.opengis.wcs20.DimensionSliceType;
import net.opengis.wcs20.DimensionSubsetType;
import net.opengis.wcs20.DimensionTrimType;
import net.opengis.wcs20.ExtensionItemType;
import net.opengis.wcs20.ExtensionType;
import net.opengis.wcs20.GetCoverageType;
import net.opengis.wcs20.InterpolationAxesType;
import net.opengis.wcs20.InterpolationAxisType;
import net.opengis.wcs20.InterpolationMethodType;
import net.opengis.wcs20.InterpolationType;
import net.opengis.wcs20.RangeIntervalType;
import net.opengis.wcs20.RangeItemType;
import net.opengis.wcs20.RangeSubsetType;
import net.opengis.wcs20.ScaleAxisByFactorType;
import net.opengis.wcs20.ScaleAxisType;
import net.opengis.wcs20.ScaleByFactorType;
import net.opengis.wcs20.ScaleToExtentType;
import net.opengis.wcs20.ScaleToSizeType;
import net.opengis.wcs20.ScalingType;
import net.opengis.wcs20.TargetAxisExtentType;
import net.opengis.wcs20.TargetAxisSizeType;

import org.eclipse.emf.common.util.EList;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CoverageInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.data.util.CoverageUtils;
import org.geoserver.wcs.CoverageCleanerCallback;
import org.geoserver.wcs.WCSInfo;
import org.geoserver.wcs2_0.exception.WCS20Exception;
import org.geoserver.wcs2_0.exception.WCS20Exception.WCS20ExceptionCode;
import org.geoserver.wcs2_0.util.EnvelopeAxesLabelsMapper;
import org.geoserver.wcs2_0.util.NCNameResourceCodec;
import org.geoserver.wcs2_0.util.RequestUtils;
import org.geotools.coverage.GridSampleDimension;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.coverage.grid.GridEnvelope2D;
import org.geotools.coverage.grid.GridGeometry2D;
import org.geotools.coverage.grid.io.AbstractGridCoverage2DReader;
import org.geotools.coverage.grid.io.AbstractGridFormat;
import org.geotools.coverage.processing.CoverageProcessor;
import org.geotools.coverage.processing.Operations;
import org.geotools.factory.GeoTools;
import org.geotools.factory.Hints;
import org.geotools.geometry.GeneralEnvelope;
import org.geotools.referencing.CRS;
import org.geotools.resources.coverage.CoverageUtilities;
import org.geotools.util.Utilities;
import org.geotools.util.logging.Logging;
import org.jaitools.imageutils.ImageLayout2;
import org.opengis.coverage.SampleDimension;
import org.opengis.coverage.grid.GridCoverage;
import org.opengis.coverage.processing.Operation;
import org.opengis.geometry.Envelope;
import org.opengis.parameter.GeneralParameterValue;
import org.opengis.parameter.ParameterValueGroup;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.datum.PixelInCell;
import org.vfny.geoserver.util.WCSUtils;

/**
 * Implementation of the WCS 2.0 GetCoverage request
 * 
 * @author Andrea Aime - GeoSolutions
 * @author Simone Giannecchini, GeoSolutions
 */
public class GetCoverage {
    
    /** Logger.*/
    private Logger LOGGER= Logging.getLogger(GetCoverage.class);
    
    public enum InterpolationPolicy{
        linear("http://www.opengis.net/def/interpolation/OGC/1/linear") {
            @Override
            public Interpolation getInterpolation() {
                return Interpolation.getInstance(Interpolation.INTERP_BILINEAR);
            }
        },nearestneighbor("http://www.opengis.net/def/interpolation/OGC/1/nearest-neighbor") {
            @Override
            public Interpolation getInterpolation() {
                return Interpolation.getInstance(Interpolation.INTERP_NEAREST);
            }
        },quadratic("http://www.opengis.net/def/interpolation/OGC/1/quadratic")  {
            @Override
            public Interpolation getInterpolation() {
                throw new WCS20Exception(
                        "Interpolation not supported",
                        WCS20Exception.WCS20ExceptionCode.InterpolationMethodNotSupported,
                        quadratic.toString());
            }
        },cubic("http://www.opengis.net/def/interpolation/OGC/1/cubic")  {
            @Override
            public Interpolation getInterpolation() {
                return Interpolation.getInstance(Interpolation.INTERP_BICUBIC_2);
            }
        },lostarea("http://www.opengis.net/def/interpolation/OGC/1/lost-area")  {
            @Override
            public Interpolation getInterpolation() {
                throw new WCS20Exception(
                        "Interpolation not supported",
                        WCS20Exception.WCS20ExceptionCode.InterpolationMethodNotSupported,
                        lostarea.toString());
            }
        },barycentric("http://www.opengis.net/def/interpolation/OGC/1/barycentric")  {
            @Override
            public Interpolation getInterpolation() {
                throw new WCS20Exception(
                        "Interpolation not supported",
                        WCS20Exception.WCS20ExceptionCode.InterpolationMethodNotSupported,
                        barycentric.toString());
            }
        };
        
        private InterpolationPolicy(String representation) {
            this.strVal = representation;
        }

        private final String strVal;
        
        abstract public Interpolation getInterpolation();
        
        static InterpolationPolicy getPolicy(InterpolationMethodType interpolationMethodType){
            Utilities.ensureNonNull("interpolationMethodType", interpolationMethodType);
            final String interpolationMethod=interpolationMethodType.getInterpolationMethod();
            return getPolicy(interpolationMethod);
        }  
        
        static InterpolationPolicy getPolicy(String interpolationMethod){
            Utilities.ensureNonNull("interpolationMethod", interpolationMethod);
            final InterpolationPolicy[] values = InterpolationPolicy.values();
            for(InterpolationPolicy policy:values){
                if(policy.strVal.equals(interpolationMethod)){
                    return policy;
                }
            }

            //method not found
            throw new WCS20Exception("Interpolation method not supported",WCS20ExceptionCode.InterpolationMethodNotSupported,interpolationMethod);
        }  
            
        static InterpolationPolicy getDefaultPolicy(){
            return nearestneighbor;
        }
    }
    
    private enum ScalingPolicy{
        DoNothing{

            @Override
            public GridCoverage2D scale(GridCoverage2D sourceGC, ScalingType scaling,Interpolation interpolation) {
                return sourceGC;
            }
            
        },
        ScaleByFactor{

            @Override
            public GridCoverage2D scale(GridCoverage2D sourceGC, ScalingType scaling,Interpolation interpolation) {
                // get scale factor
                final ScaleByFactorType scaleByFactorType = scaling.getScaleByFactor();
                double scaleFactor=scaleByFactorType.getScaleFactor();
                
                // checks
                if(scaleFactor<0){
                    throw new WCS20Exception("Invalid scale factor", WCS20Exception.WCS20ExceptionCode.InvalidScaleFactor, String.valueOf(scaleFactor));
                }
                
                // return coverage unchanged if we don't scale
                if(scaleFactor==1){
                    return sourceGC;
                }
                
                // checks
                return (GridCoverage2D)Operations.DEFAULT.scale(sourceGC, scaleFactor, scaleFactor, 0, 0, interpolation!=null?interpolation:InterpolationPolicy.getDefaultPolicy().getInterpolation());
            }
            
        },
        ScaleToSize{

            /**
             * In this case we must retain the lower bounds by scale the size, hence {@link ScaleDescriptor} JAI operation 
             * cannot be used. Same goes for {@link AffineDescriptor}, the only real option is {@link WarpDescriptor}.
             * 
             */
            @Override
            public GridCoverage2D scale(GridCoverage2D sourceGC, ScalingType scaling, Interpolation interpolation) {
                // get scale size
                final ScaleToSizeType scaleType = scaling.getScaleToSize();
                final EList<TargetAxisSizeType> targetAxisSizeElements = scaleType.getTargetAxisSize();

                TargetAxisSizeType xSize=null,ySize=null;
                for(TargetAxisSizeType axisSizeType:targetAxisSizeElements){
                    final String axisName=axisSizeType.getAxis();
                    if(axisName.equals("http://www.opengis.net/def/axis/OGC/1/i")){
                        xSize=axisSizeType;
                    } else if(axisName.equals("http://www.opengis.net/def/axis/OGC/1/j")){
                        ySize=axisSizeType;
                    } else {
                        // TODO remove when supporting TIME and ELEVATION
                        throw new WCS20Exception("Scale Axis Undefined", WCS20Exception.WCS20ExceptionCode.ScaleAxisUndefined, axisName);
                    }
                }
                final int sizeX=(int) xSize.getTargetSize();// TODO should this be int?
                if(sizeX<=0){
                    throw new WCS20Exception("Invalid target size", WCS20Exception.WCS20ExceptionCode.InvalidScaleFactor, Integer.toString(sizeX));
                }
                final int sizeY=(int) ySize.getTargetSize();// TODO should this be int?
                if(sizeY<=0){
                    throw new WCS20Exception("Invalid target size", WCS20Exception.WCS20ExceptionCode.InvalidScaleFactor, Integer.toString(sizeY));
                }
                
                // unscale
                final GridEnvelope2D sourceGE=sourceGC.getGridGeometry().getGridRange2D();
                if(sizeY==sourceGE.width&& sizeX==sourceGE.height){
                    return sourceGC;
                }
                
                // create final warp
                final Warp warp= new  WarpAffine(AffineTransform.getScaleInstance(sourceGE.width/sizeX, sourceGE.height/sizeY));// TODO check
                // impose final 
                final ImageLayout2 layout = new ImageLayout2(
                        sourceGE.x,
                        sourceGE.y,
                        sizeX,
                        sizeY);
                final Hints hints= new Hints(JAI.KEY_IMAGE_LAYOUT, layout);                
                final Operation operation = CoverageProcessor.getInstance().getOperation("Warp");
                final ParameterValueGroup parameters = operation.getParameters();
                parameters.parameter("Source").setValue(sourceGC);
                parameters.parameter("warp").setValue(warp);
                parameters.parameter("interpolation").setValue(interpolation!=null?interpolation:InterpolationPolicy.getDefaultPolicy().getInterpolation());
                parameters.parameter( "backgroundValues").setValue(CoverageUtilities.getBackgroundValues(sourceGC));// TODO check and improve
                return (GridCoverage2D) CoverageProcessor.getInstance().doOperation(parameters,hints);
            }

        },
        ScaleToExtent{

            @Override
            public GridCoverage2D scale(GridCoverage2D sourceGC, ScalingType scaling, Interpolation interpolation) {
                // parse area
                final ScaleToExtentType scaleType = scaling.getScaleToExtent();
                final EList<TargetAxisExtentType> targetAxisExtentElements = scaleType.getTargetAxisExtent();
                
                TargetAxisExtentType xExtent=null,yExtent=null;
                for(TargetAxisExtentType axisExtentType:targetAxisExtentElements){
                    final String axisName=axisExtentType.getAxis();
                    if(axisName.equals("http://www.opengis.net/def/axis/OGC/1/i")){
                        xExtent=axisExtentType;
                    } else if(axisName.equals("http://www.opengis.net/def/axis/OGC/1/j")){
                        yExtent=axisExtentType;
                    } else {
                        // TODO remove when supporting TIME and ELEVATION
                        throw new WCS20Exception("Scale Axis Undefined", WCS20Exception.WCS20ExceptionCode.ScaleAxisUndefined, axisName);
                    }
                }
                if(xExtent==null){
                    throw new WCS20Exception("Missing extent along i", WCS20Exception.WCS20ExceptionCode.InvalidScaleFactor, "");
                }
                if(yExtent==null){
                    throw new WCS20Exception("Missing extent along j", WCS20Exception.WCS20ExceptionCode.InvalidScaleFactor, "");
                }  
                
                final int minx=(int) targetAxisExtentElements.get(0).getLow();// TODO should this be int?
                final int maxx=(int) targetAxisExtentElements.get(0).getHigh();
                final int miny=(int) targetAxisExtentElements.get(1).getLow();
                final int maxy=(int) targetAxisExtentElements.get(1).getHigh();               
                
                // check on source geometry
                final GridEnvelope2D sourceGE=sourceGC.getGridGeometry().getGridRange2D();

                if(minx>maxx){
                    throw new WCS20Exception("Invalid Extent for dimension:"+targetAxisExtentElements.get(0).getAxis() , WCS20Exception.WCS20ExceptionCode.InvalidExtent, String.valueOf(maxx));
                }
                if(miny>maxy){
                    throw new WCS20Exception("Invalid Extent for dimension:"+targetAxisExtentElements.get(1).getAxis() , WCS20Exception.WCS20ExceptionCode.InvalidExtent, String.valueOf(maxy));
                }                
                final Rectangle destinationRectangle= new Rectangle(minx, miny,maxx-minx+1, maxy-miny+1);
                // UNSCALE
                if(destinationRectangle.equals(sourceGE)){
                    return sourceGC;
                }
                
                // create final warp
                final Warp warp= new  WarpAffine(AffineTransform.getScaleInstance(
                        sourceGE.width/destinationRectangle.width, 
                        sourceGE.height/destinationRectangle.height)); // TODO check
                // impose size
                final ImageLayout2 layout = new ImageLayout2(
                        destinationRectangle.x,
                        destinationRectangle.y,
                        destinationRectangle.width,
                        destinationRectangle.height);
                final Hints hints= new Hints(JAI.KEY_IMAGE_LAYOUT, layout);
                
                final Operation operation = CoverageProcessor.getInstance().getOperation("Warp");
                final ParameterValueGroup parameters = operation.getParameters();
                parameters.parameter("Source").setValue(sourceGC);
                parameters.parameter("warp").setValue(warp);
                parameters.parameter("interpolation").setValue(interpolation!=null?interpolation:InterpolationPolicy.getDefaultPolicy().getInterpolation());
                parameters.parameter( "backgroundValues").setValue(CoverageUtilities.getBackgroundValues(sourceGC));// TODO check and improve
                return (GridCoverage2D) CoverageProcessor.getInstance().doOperation(parameters,hints);
            }
            
        },        
        ScaleAxesByFactor{

            @Override
            public GridCoverage2D scale(GridCoverage2D sourceGC, ScalingType scaling,Interpolation interpolation) {
                // TODO dimension management
                
                // get scale factor
                final ScaleAxisByFactorType scaleType = scaling.getScaleAxesByFactor();
                final EList<ScaleAxisType> targetAxisScaleElements = scaleType.getScaleAxis();
                
                ScaleAxisType xScale=null,yScale=null;
                for(ScaleAxisType scaleAxisType:targetAxisScaleElements){
                    final String axisName=scaleAxisType.getAxis();
                    if(axisName.equals("http://www.opengis.net/def/axis/OGC/1/i")){
                        xScale=scaleAxisType;
                    } else if(axisName.equals("http://www.opengis.net/def/axis/OGC/1/j")){
                        yScale=scaleAxisType;
                    } else {
                        // TODO remove when supporting TIME and ELEVATION
                        throw new WCS20Exception("Scale Axis Undefined", WCS20Exception.WCS20ExceptionCode.ScaleAxisUndefined, axisName);
                    }
                }
                if(xScale==null){
                    throw new WCS20Exception("Missing scale factor along i", WCS20Exception.WCS20ExceptionCode.InvalidScaleFactor, "");
                }
                if(yScale==null){
                    throw new WCS20Exception("Missing scale factor along j", WCS20Exception.WCS20ExceptionCode.InvalidScaleFactor, "");
                }                

                final double scaleFactorX= xScale.getScaleFactor();// TODO should this be int?
                if(scaleFactorX<0){
                    throw new WCS20Exception("Invalid scale factor", WCS20Exception.WCS20ExceptionCode.InvalidScaleFactor, Double.toString(scaleFactorX));
                }   
                final double scaleFactorY= yScale.getScaleFactor();// TODO should this be int?
                if(scaleFactorY<0){
                    throw new WCS20Exception("Invalid scale factor", WCS20Exception.WCS20ExceptionCode.InvalidScaleFactor, Double.toString(scaleFactorY));
                }                  
                
                // unscale
                if(scaleFactorX==1.0&& scaleFactorY==1.0){
                    return sourceGC;
                }
                
                return (GridCoverage2D) Operations.DEFAULT.scale(sourceGC, scaleFactorX, scaleFactorY, 0, 0,interpolation!=null?interpolation:InterpolationPolicy.getDefaultPolicy().getInterpolation());
            }
            
        };
        /**
         * @param scaling 
         * @param interpolation 
         * @param sourceGG
         * @param width 
         * @param origin 
         * @param returnValue
         */
        abstract public GridCoverage2D scale(GridCoverage2D sourceGC, ScalingType scaling, Interpolation interpolation) ;
        

        public static ScalingPolicy getPolicy(ScalingType scaling) {
            if(scaling!=null){
                if (scaling.getScaleAxesByFactor() != null) {
                    return ScaleAxesByFactor;
                }
                if (scaling.getScaleByFactor() != null) {
                    return ScaleByFactor;
                }
                if (scaling.getScaleToExtent() != null) {
                    return ScaleToExtent;
                }
                if (scaling.getScaleToSize() != null) {
                    return ScalingPolicy.ScaleToSize;
                }   

            }
            return DoNothing;
        }
    };
    

    private WCSInfo wcs;
    private Catalog catalog;
    
    /** Utility class to map envelope dimension*/
    private EnvelopeAxesLabelsMapper envelopeDimensionsMapper;

    public GetCoverage(WCSInfo serviceInfo, Catalog catalog, EnvelopeAxesLabelsMapper envelopeDimensionsMapper) {
        this.wcs = serviceInfo;
        this.catalog = catalog;
        this.envelopeDimensionsMapper=envelopeDimensionsMapper;
    }

    public GridCoverage run(GetCoverageType request) {

        //
        // get the coverage info from the catalog or throw an exception if we don't find it
        //
        LayerInfo linfo = NCNameResourceCodec.getCoverage(catalog, request.getCoverageId());
        if(linfo == null) {
            throw new WCS20Exception("Could not locate coverage " + request.getCoverageId(), 
                    WCS20Exception.WCS20ExceptionCode.NoSuchCoverage, "coverageId");
        } 
        
        CoverageInfo cinfo = (CoverageInfo) linfo.getResource();
        GridCoverage2D coverage = null;
        try {

            // hints
            // here I find if I can use overviews and do subsampling
            final Hints hints = GeoTools.getDefaultHints();
            hints.add(WCSUtils.getReaderHints(wcs));

            // get a reader for this coverage
            final AbstractGridCoverage2DReader reader = (AbstractGridCoverage2DReader) cinfo.getGridCoverageReader(null, hints);
            
            // extract all extensions for later usage
            Map<String, ExtensionItemType> extensions = extractExtensions(coverage,request);
           
            // get CRS extension values
            final CoordinateReferenceSystem subsettingCRS=extractSubsettingCRS(reader,extensions);
            final CoordinateReferenceSystem outputCRS=extractOutputCRS(reader,extensions);
            // extract subsetting
            final GeneralEnvelope subset=extractSubsettingEnvelope(reader,request,subsettingCRS);
            
            //
            // handle interpolation extension 
            //
            // notice that for the moment we support only homogeneous interpolation on the 2D axis
            final Map<String,InterpolationPolicy> axesInterpolations=extractInterpolation(reader,extensions);
            final Interpolation spatialInterpolation=extractSpatialInterpolation(axesInterpolations,reader.getOriginalEnvelope());
            // TODO time interpolation
            assert spatialInterpolation!=null;
            
            // we setup the params to force the usage of imageread and to make it use
            // the right overview and so on
            // we really try to subset before reading with a grid geometry
            // we specify to work in streaming fashion
            // TODO time
            // TODO elevation
            coverage = readCoverage(cinfo,spatialInterpolation,subset,outputCRS,reader,hints);
            if(coverage==null){
                throw new IllegalStateException("Unable to read a coverage for the current request"+request.toString());
            }
            
            // handle range subsetting
            coverage=handleRangeSubsettingExtension(coverage,extensions);
            
            // subsetting, is not really an extension
            coverage=handleSubsettingExtension(coverage,subset);
            
            // scaling extension
            //
            // scaling is done in raster space with eventual interpolation
            // TODO we should handle the case where higher order interpolation
            // is requested without a proper scaling by performing a scale by 1
            // with interpolation or something similar
            coverage=handleScaling(coverage,extensions,spatialInterpolation);
            
            // reprojection
            //
            // reproject the output coverage to an eventual outputCrs
            coverage=handleReprojection(coverage,outputCRS,spatialInterpolation);

        } catch(Exception e) {
            throw new WCS20Exception("Failed to read the coverage " + request.getCoverageId(), e);
        } finally {
            // make sure the coverage will get cleaned at the end of the processing
            if(coverage != null) {
                CoverageCleanerCallback.addCoverages(coverage);
            }
        }
        
        return coverage;
    }

    /**
     * 
     * @param axesInterpolations
     * @param envelope
     * @return
     */
    private Interpolation extractSpatialInterpolation(
            Map<String, InterpolationPolicy> axesInterpolations, Envelope envelope) {
        // extract interpolation
        //
        // we assume that we are going to support the same interpolation ONLY on the i and j axes
        // therefore we extract the first one of the two we find. We implicitly assume we already
        // checked that we don't have mixed interpolation types for lat,lon
        Interpolation interpolation=InterpolationPolicy.getDefaultPolicy().getInterpolation();
        for(String axisLabel:axesInterpolations.keySet()){
            // check if this is an axis we like
            final int index=envelopeDimensionsMapper.getAxisIndex(envelope, axisLabel);
            if(index==0||index==1){
                // found it!
                interpolation=axesInterpolations.get(axisLabel).getInterpolation();
                break;
            }
        }
        return interpolation;
    }

    /**
     * @param cinfo
     * @param outputCRS 
     * @param subset 
     * @param spatialInterpolation 
     * @param reader 
     * @param hints 
     * @return
     * @throws IOException 
     */
    private GridCoverage2D readCoverage(
            CoverageInfo cinfo, 
            Interpolation spatialInterpolation, 
            GeneralEnvelope subset, 
            CoordinateReferenceSystem outputCRS, 
            AbstractGridCoverage2DReader reader, 
            Hints hints) throws Exception {
        // checks
        Utilities.ensureNonNull("interpolation", spatialInterpolation);
        
        //
        // check if we need to reproject the subset envelope back to coverageCRS
        //
        // this does not mean we need to reproject the coverage at the end
        // as the outputCrs can be different from the subsetCrs
        //
        // get source crs
        final CoordinateReferenceSystem coverageCRS= reader.getCrs();
        if(!CRS.equalsIgnoreMetadata(subset.getCoordinateReferenceSystem(), coverageCRS)){
            subset= CRS.transform(
                    CRS.findMathTransform(subset.getCoordinateReferenceSystem(), coverageCRS),
                    subset);
            subset.setCoordinateReferenceSystem(coverageCRS);
        }
        // k, now subset is in the CRS of the source coverage
         
        //
        // read best available coverage and render it
        //        
        final GridGeometry2D readGG;
        
        // do we need to reproject the coverage to a different crs?
        // this would force us to enlarge the read area
        final boolean equalsMetadata=CRS.equalsIgnoreMetadata(outputCRS, coverageCRS);
        boolean sameCRS;
        try {
            sameCRS = equalsMetadata?true:CRS.findMathTransform(outputCRS, coverageCRS,true).isIdentity();
        } catch (FactoryException e1) {
            final IOException ioe= new IOException();
            ioe.initCause(e1);
            throw ioe;
        }
        
        
        //
        // instantiate basic params for reading
        //
        // 
        final ParameterValueGroup readParametersDescriptor = reader.getFormat().getReadParameters();
        GeneralParameterValue[] readParameters = CoverageUtils.getParameters(readParametersDescriptor, cinfo.getParameters());
        readParameters = (readParameters != null ? readParameters : new GeneralParameterValue[0]);
        // work in streaming fashion when JAI is involved
        readParameters = RequestUtils.replaceParameter(
                readParameters, 
                Boolean.FALSE, 
                AbstractGridFormat.USE_JAI_IMAGEREAD);     
        
        
        //
        // kk, now build a good GG to read the smallest available area for the following operations
        //
        // hints
        if(sameCRS){
            // we should not be reprojecting
            // let's create a subsetting GG2D at the highest resolution available
            readGG = new GridGeometry2D(
                    PixelInCell.CELL_CENTER,
                    reader.getOriginalGridToWorld(PixelInCell.CELL_CENTER),
                    subset,
                    hints);
            
            return RequestUtils.readBestCoverage(
                    reader, 
                    readParameters, 
                    readGG,
                    spatialInterpolation,
                    hints);            
            
        }else{
            
            // we are reprojecting, let's add a gutter in raster space.
            //
            // We need to investigate much more and also we need 
            // to do this only when needed
            //
            // 
            // add gutter by increasing size of 10 pixels each side
            Rectangle rasterRange = CRS.transform(
                    reader.getOriginalGridToWorld(PixelInCell.CELL_CORNER).inverse(),
                    subset).toRectangle2D().getBounds();
//            rasterRange.setSize(
//                    (int)(rasterRange.width*1.1), 
//                    (int)(rasterRange.height*1.1));
            rasterRange.setBounds(rasterRange.x-10, rasterRange.y-10, rasterRange.width+20, rasterRange.height+20);
            rasterRange=rasterRange.intersection(( GridEnvelope2D)reader.getOriginalGridRange());// make sure we are in it
            
            // read
            readGG = new GridGeometry2D(
                    new GridEnvelope2D(rasterRange),
                    PixelInCell.CELL_CENTER,
                    reader.getOriginalGridToWorld(PixelInCell.CELL_CENTER),
                    coverageCRS,
                    hints);
            
            return RequestUtils.readBestCoverage(reader, 
                    readParameters,  
                    readGG, 
                    spatialInterpolation,
                    hints);            
        }

    }

    /**
     * @param reader
     * @param extensions
     * @return
     */
    private CoordinateReferenceSystem extractOutputCRS(AbstractGridCoverage2DReader reader,
            Map<String, ExtensionItemType> extensions) {
        return extractCRS(reader,extensions,false);
    }

    /**
     * @param reader
     * @param extensions
     * @return
     */
    private CoordinateReferenceSystem extractSubsettingCRS(AbstractGridCoverage2DReader reader,Map<String, ExtensionItemType> extensions) {
        return extractCRS(reader,extensions,true);
    }

    /**
     * @param reader
     * @param extensions
     * @param b
     * @return
     */
    private CoordinateReferenceSystem extractCRS(AbstractGridCoverage2DReader reader, Map<String, ExtensionItemType> extensions,
            boolean subsettingCRS) {
        
        // look for subsettingCRS Extension extension
        if(extensions==null||extensions.size()==0||!extensions.containsKey(subsettingCRS ? "subsettingCrs" : "outputCrs")){
             // NO extension at hand
            return reader.getCrs();
        }
        
        // look for an interpolation extension
        final ExtensionItemType extensionItem=extensions.get(subsettingCRS ? "subsettingCrs" : "outputCrs");
        if (extensionItem.getName().equals(subsettingCRS ? "subsettingCrs" : "outputCrs")) {
            // get URI
            String crsName = extensionItem.getSimpleContent();

            // checks
            if (crsName == null) {
                throw new WCS20Exception(subsettingCRS ? "Subsetting" : "Output" + " CRS was null",
                        WCS20ExceptionCode.NotACrs, "null");
            }

            // instantiate
            final int lastSlash = crsName.lastIndexOf("/");
            // error no valid URI
            // TODO improve checs
            if (lastSlash < 0) {
                throw new WCS20Exception("Invalid " + (subsettingCRS ? "subsetting" : "output")
                        + " CRS", WCS20Exception.WCS20ExceptionCode.NotACrs, crsName);
            }
            crsName = crsName.substring(lastSlash + 1, crsName.length());
            // instantiate
            try {
                return CRS.decode("EPSG:" + crsName, false); // notice the usage of boolean param
            } catch (Exception e) {
                final WCS20Exception exception = new WCS20Exception("Invalid "
                        + (subsettingCRS ? "subsetting" : "output") + " CRS",
                        WCS20Exception.WCS20ExceptionCode.NotACrs, crsName);
                exception.initCause(e);
                throw exception;
            }

        }
        return reader.getCrs();
    }

    /**
     * @param coverage
     * @param request
     * @return
     */
    private Map<String, ExtensionItemType> extractExtensions(GridCoverage2D coverage, GetCoverageType request) {
        // look for subsettingCRS Extension extension
        final ExtensionType extension = request.getExtension();
        
        // look for the various extensions
        final Map<String,ExtensionItemType> parsedExtensions=new HashMap<String, ExtensionItemType>();
        // no extensions?
        if(extension!=null){
            final EList<ExtensionItemType> extensions = extension.getContents();
            for (final ExtensionItemType extensionItem : extensions) {
                final String extensionName = extensionItem.getName();
                if (extensionName == null || extensionName.length() <= 0) {
                    throw new WCS20Exception("Null extension");
                }
                if (extensionName.equals("subsettingCrs")) {
                    parsedExtensions.put("subsettingCrs", extensionItem);
                } else if (extensionName.equals("outputCrs")) {
                    parsedExtensions.put("outputCrs", extensionItem);
                } else if (extensionName.equals("Scaling")) {
                    parsedExtensions.put("Scaling", extensionItem);
                } else if (extensionName.equals("Interpolation")) {
                    parsedExtensions.put("Interpolation", extensionItem);
                } else if (extensionName.equals("rangeSubset")) {
                    parsedExtensions.put("rangeSubset", extensionItem);
                } else if (extensionName.equals("rangeSubset")) {
                    parsedExtensions.put("rangeSubset", extensionItem);
                } 
            }
        }
        return parsedExtensions;
    }

    /**
     * @param reader 
     * @param extensions2
     * @return
     */
    private Map<String,InterpolationPolicy> extractInterpolation(AbstractGridCoverage2DReader reader, Map<String, ExtensionItemType> extensions) {
        // preparation
        final Map<String,InterpolationPolicy> returnValue= new HashMap<String, InterpolationPolicy>();
        final Envelope envelope= reader.getOriginalEnvelope();
        final List<String> axesNames = envelopeDimensionsMapper.getAxesNames(envelope, true);
        for(String axisName:axesNames){
            returnValue.put(axisName, InterpolationPolicy.getDefaultPolicy());// use defaults if no specified
        }    
    
        // look for scaling extension
        if(extensions==null||extensions.size()==0||!extensions.containsKey("Interpolation")){
             // NO INTERPOLATION
            return returnValue;
        }
            
        // look for an interpolation extension
        final ExtensionItemType extensionItem=extensions.get("Interpolation");
        // get interpolationType
        InterpolationType interpolationType = (InterpolationType) extensionItem
                .getObjectContent();
        // which type
        if (interpolationType.getInterpolationMethod() != null) {
            InterpolationMethodType method = interpolationType.getInterpolationMethod();
            InterpolationPolicy policy = InterpolationPolicy.getPolicy(method);
            for (String axisName : axesNames) {
                returnValue.put(axisName, policy);
            }

        } else if (interpolationType.getInterpolationAxes() != null) {
            // make sure we don't set things twice
            final List<String> foundAxes=new ArrayList<String>();
            
            final InterpolationAxesType axes = interpolationType.getInterpolationAxes();
            for (InterpolationAxisType axisInterpolation : axes.getInterpolationAxis()) {

                // parse interpolation
                final String method = axisInterpolation.getInterpolationMethod();
                final InterpolationPolicy policy = InterpolationPolicy.getPolicy(method);

                // parse axis
                final String axis = axisInterpolation.getAxis();
                // get label from axis
                // TODO synonyms reduction
                int index = axis.lastIndexOf("/");
                final String axisLabel = (index >= 0 ? axis.substring(index+1, axis.length())
                        : axis);
                
                // did we already set this interpolation?
                if(foundAxes.contains(axisLabel)){
                    throw new WCS20Exception("Duplicated axis",WCS20Exception.WCS20ExceptionCode.InvalidAxisLabel,axisLabel);
                }
                foundAxes.add(axisLabel);
                
                // do we have this axis?
                if(!returnValue.containsKey(axisLabel)){
                    throw new WCS20Exception("Invalid axes URI",WCS20Exception.WCS20ExceptionCode.NoSuchAxis,axisLabel);
                }
                returnValue.put(axisLabel, policy);
            }
        }
        
        // final checks, we dont' supported different interpolations on Long and Lat
        InterpolationPolicy lat=null,lon=null;
        if(returnValue.containsKey("Long")){
            lon=returnValue.get("Long");
        }
        if(returnValue.containsKey("Lat")){
            lat=returnValue.get("Lat");
        }
        if(lat!=lon){
            throw new WCS20Exception("We don't support different interpolations on Lat,Lon", WCS20Exception.WCS20ExceptionCode.InterpolationMethodNotSupported,"");
        }
        returnValue.get("Lat");
        return returnValue;
    }

    /**
     * @param coverage
     * @param outputCRS
     * @param spatialInterpolation 
     * @return
     */
    private GridCoverage2D handleReprojection(GridCoverage2D coverage, CoordinateReferenceSystem outputCRS, Interpolation spatialInterpolation) {
        // checks
        Utilities.ensureNonNull("interpolation", spatialInterpolation);
        // check the two crs tosee if we really need to do anything
        if(CRS.equalsIgnoreMetadata(coverage.getCoordinateReferenceSystem2D(), outputCRS)){
            return coverage;
        }

        // resample
        return (GridCoverage2D) Operations.DEFAULT.resample(coverage, outputCRS,null,spatialInterpolation);
    }

    /**
     * @param coverage
     * @param extensions
     * @return
     */
    private GridCoverage2D handleRangeSubsettingExtension(
            GridCoverage2D coverage,
            Map<String, ExtensionItemType> extensions) {
        // preparation
         final List<String> returnValue=new ArrayList<String>();  
    
        // look for rangeSubset extension
        if(extensions==null||extensions.size()==0||!extensions.containsKey("rangeSubset")){
             // NO subsetting
            return coverage;
        }
        // get original bands
        final GridSampleDimension[] bands = coverage.getSampleDimensions();
        final List<String> bandsNames= new ArrayList<String>();
        for(GridSampleDimension band:bands){
            bandsNames.add(band.getDescription().toString());
        }
            
        // look for an interpolation extension
        final ExtensionItemType extensionItem=extensions.get("rangeSubset");
        assert extensionItem!=null; // should not happen
        final RangeSubsetType range = (RangeSubsetType) extensionItem.getObjectContent();
        for(RangeItemType rangeItem: range.getRangeItems()){
            // there you go the range item
            
            // single element 
            final String rangeComponent=rangeItem.getRangeComponent();
            
            // range?
            if(rangeComponent==null){
                final RangeIntervalType rangeInterval = rangeItem.getRangeInterval();
                final String startRangeComponent=rangeInterval.getStartComponent();     
                final String endRangeComponent=rangeInterval.getEndComponent();
                if(bandsNames.contains(startRangeComponent)){
                    returnValue.add(startRangeComponent);
                } else {
                    throw new WCS20Exception("Invalid Band Name",WCS20Exception.WCS20ExceptionCode.NoSuchField,rangeComponent);
                }
                if(bandsNames.contains(endRangeComponent)){
                    returnValue.add(endRangeComponent);
                } else {
                    throw new WCS20Exception("Invalid Band Name",WCS20Exception.WCS20ExceptionCode.NoSuchField,rangeComponent);
                }                
                
                // loop
                boolean add=false;
                for(SampleDimension sd: bands){
                    if(sd instanceof GridSampleDimension){
                        final GridSampleDimension band=(GridSampleDimension) sd;
                        final String name=band.getDescription().toString();
                        if(name.equals(startRangeComponent)){
                            returnValue.add(startRangeComponent);
                            add=true;
                        } else if(name.equals(endRangeComponent)){
                            returnValue.add(endRangeComponent);
                            add=false;
                        } else if(add){
                            returnValue.add(name);
                        }
                    }
                }
                // paranoiac check add a false
                if(add){
                    throw new IllegalStateException("Unable to close range in band identifiers");
                }
            } else {
                if(bandsNames.contains(rangeComponent)){
                    returnValue.add(rangeComponent);
                } else {
                    throw new WCS20Exception("Invalid Band Name",WCS20Exception.WCS20ExceptionCode.NoSuchField,rangeComponent);
                }
            }
        }
   
        // kk now let's see what we got to do
        if(returnValue.isEmpty()){
            return coverage;
        }
        // houston we got a list of dimensions
        // create a list of indexes to select
        final int indexes[]= new int[returnValue.size()];
        int i=0;
        for(String bandName:returnValue){
            indexes[i++]=bandsNames.indexOf(bandName);// I am assuming there is no duplication in band names which is ok I believe
        }
        return (GridCoverage2D) WCSUtils.bandSelect(coverage, indexes);
    }

    /**
     * @param coverage
     * @param request
     * @param subsettingCRS 
     * @param subset 
     * @return
     */
    private GridCoverage2D handleSubsettingExtension(
            GridCoverage2D coverage, 
            GeneralEnvelope subset) {

        if(subset!=null){
            return WCSUtils.crop(coverage, subset); // TODO I hate this classes that do it all
        }
        return coverage;
    }

    /**
     * @param coverage
     * @param spatialInterpolation 
     * @param extensions2
     * @return
     */
    private GridCoverage2D handleScaling(GridCoverage2D coverage, Map<String, ExtensionItemType> extensions, Interpolation spatialInterpolation) {
        // checks
        Utilities.ensureNonNull("interpolation", spatialInterpolation);
        
        // look for scaling extension
        if(extensions==null||extensions.size()==0||!extensions.containsKey("Scaling")){
             // NO SCALING do we need interpolation?
            if(spatialInterpolation instanceof InterpolationNearest){
                return coverage;
            } else {
                // TODO handle this case properly
                // interpolate coverage if requested and not nearest!!!!
                return coverage;
            }
        }
        
        // look for a scaling extension
        final ExtensionItemType extensionItem=extensions.get("Scaling");
        assert extensionItem!=null;
            
        // get scaling
        ScalingType scaling = (ScalingType) extensionItem.getObjectContent();
        if(scaling==null){
            throw new IllegalStateException("Scaling extension contained a null ScalingType");
        }

        // instantiate enum
        final ScalingPolicy scalingPolicy = ScalingPolicy.getPolicy(scaling);
        return scalingPolicy.scale(coverage, scaling, spatialInterpolation);

    }

    /**
     * @param reader
     * @param request
     * @param subsettingCRS 
     * @return
     */
    private GeneralEnvelope extractSubsettingEnvelope(
            AbstractGridCoverage2DReader reader, 
            GetCoverageType request, 
            CoordinateReferenceSystem subsettingCRS) {
        //default envelope in subsettingCRS
        final CoordinateReferenceSystem sourceCRS=reader.getCrs();
        GeneralEnvelope envelope=new GeneralEnvelope(reader.getOriginalEnvelope());
        envelope.setCoordinateReferenceSystem(sourceCRS);
        if(!(subsettingCRS==null||CRS.equalsIgnoreMetadata(subsettingCRS,sourceCRS))){
            
            // reproject source coverage to subsetting crs for initialization
            try {
                envelope= CRS.transform(
                        CRS.findMathTransform(reader.getCrs(), subsettingCRS), 
                        reader.getOriginalEnvelope());
                envelope.setCoordinateReferenceSystem(subsettingCRS);
            } catch (Exception e) {
                final WCS20Exception exception= new WCS20Exception(
                        "Unable to initialize subsetting envelope",
                        WCS20Exception.WCS20ExceptionCode.SubsettingCrsNotSupported,
                        subsettingCRS.toWKT()); // TODO extract code
                exception.initCause(e);
                throw exception;
            } 
        }
        
        // check if we have to subset, if not let's send back the basic coverage
        final EList<DimensionSubsetType> dimensions = request.getDimensionSubset();
        if(dimensions==null||dimensions.size()<=0){
            return envelope;
        }
        
        // TODO remove when we handle time and elevation
        if(dimensions.size()>2){
            throw new WCS20Exception(
                    "Invalid number of dimensions", 
                    WCS20Exception.WCS20ExceptionCode.InvalidSubsetting,Integer.toString(dimensions.size()));
        }
        
        // put aside the dimensions that we have for double checking
        final List<String> axesNames = envelopeDimensionsMapper.getAxesNames(envelope, true);
        final List<String> foundDimensions= new ArrayList<String>();
        
        // parse dimensions
        for(DimensionSubsetType dim:dimensions){
            // get basic information
            final String dimension=dim.getDimension(); // this is the dimension name which we compare to axes abbreviations from geotools
            if(dimension==null||dimension.length()<=0||!axesNames.contains(dimension)){//TODO synonyms on axes labels
                throw new WCS20Exception("Empty axis label provided",WCS20Exception.WCS20ExceptionCode.InvalidAxisLabel,dimension==null?"Null":dimension);
            }
            
            // did we already do something with this dimension?
            if(foundDimensions.contains(dimension)){
                throw new WCS20Exception("Axis label already used during subsetting",WCS20Exception.WCS20ExceptionCode.InvalidAxisLabel,dimension);
            }
            foundDimensions.add(dimension);
            
            // now decide what to do
            final String CRS= dim.getCRS();// TODO HOW DO WE USE THIS???
            if(dim instanceof DimensionTrimType){
                
                // TRIMMING
                final DimensionTrimType trim = (DimensionTrimType) dim;
                final double low = Double.parseDouble(trim.getTrimLow());
                final double high = Double.parseDouble(trim.getTrimHigh());

                final int axisIndex=envelopeDimensionsMapper.getAxisIndex(envelope, dimension);
                if(axisIndex<0){
                    throw new WCS20Exception("Invalid axis provided",WCS20Exception.WCS20ExceptionCode.InvalidAxisLabel,dimension);
                }
                
                // notice how we choose the order of the axes
                envelope.setRange(axisIndex, low, high);
            } else if(dim instanceof DimensionSliceType){
                
                // SLICING
                final DimensionSliceType slicing= (DimensionSliceType) dim;
                final String slicePointS = slicing.getSlicePoint();
                final double slicePoint=Double.parseDouble(slicePointS);
                
                final int axisIndex=envelopeDimensionsMapper.getAxisIndex(envelope, dimension);
                if(axisIndex<0){
                    throw new WCS20Exception("Invalid axis provided",WCS20Exception.WCS20ExceptionCode.InvalidAxisLabel,dimension);
                }
                // notice how we choose the order of the axes
                AffineTransform affineTransform = RequestUtils.getAffineTransform(reader.getOriginalGridToWorld(PixelInCell.CELL_CENTER));
                final double scale=axisIndex==0?affineTransform.getScaleX():-affineTransform.getScaleY();
                envelope.setRange(axisIndex, slicePoint, slicePoint+scale);
                
            } else {
                throw new WCS20Exception(
                        "Invalid element found while attempting to parse dimension subsetting request", 
                        WCS20Exception.WCS20ExceptionCode.InvalidSubsetting,
                        dim.getClass().toString());
            }
        }
        
        //
        // intersect with original envelope to make sure the subsetting is valid
        //
        final GeneralEnvelope sourceEnvelope = new GeneralEnvelope(reader.getOriginalEnvelope());
        if(CRS.equalsIgnoreMetadata(envelope.getCoordinateReferenceSystem(), sourceEnvelope.getCoordinateReferenceSystem())){
            envelope.intersect(sourceEnvelope);
            envelope.setCoordinateReferenceSystem(reader.getCrs());
        } else {
            // reproject envelope  to native crs for cropping
            try {
                envelope= CRS.transform(
                        CRS.findMathTransform(subsettingCRS,sourceCRS), 
                        envelope);
                envelope.setCoordinateReferenceSystem(sourceCRS);
                
                // intersect
                envelope.intersect(sourceEnvelope);
                envelope.setCoordinateReferenceSystem(sourceCRS);                
            } catch (Exception e) {
                final WCS20Exception exception= new WCS20Exception(
                        "Unable to initialize subsetting envelope",
                        WCS20Exception.WCS20ExceptionCode.SubsettingCrsNotSupported,
                        subsettingCRS.toWKT()); // TODO extract code
                exception.initCause(e);
                throw exception;
            } 
        }        

        if(envelope.isEmpty()){
            throw new WCS20Exception(
                    "Empty intersection after subsetting", 
                    WCS20Exception.WCS20ExceptionCode.InvalidSubsetting,"");// TODO spit our envelope trimmed
        }
        return envelope;
    }

}
