package org.geoserver.wcs2_0;

import java.awt.Rectangle;
import java.awt.geom.AffineTransform;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.media.jai.Interpolation;
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
import org.geoserver.platform.OWS20Exception;
import org.geoserver.wcs.CoverageCleanerCallback;
import org.geoserver.wcs.WCSInfo;
import org.geoserver.wcs2_0.exception.WCS20Exception;
import org.geoserver.wcs2_0.exception.WCS20Exception.WCS20ExceptionCode;
import org.geoserver.wcs2_0.util.EnvelopeAxesLabelsMapper;
import org.geoserver.wcs2_0.util.NCNameResourceCodec;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.coverage.grid.GridEnvelope2D;
import org.geotools.coverage.grid.GridGeometry2D;
import org.geotools.coverage.processing.CoverageProcessor;
import org.geotools.coverage.processing.Operations;
import org.geotools.factory.Hints;
import org.geotools.geometry.GeneralEnvelope;
import org.geotools.referencing.CRS;
import org.geotools.resources.coverage.CoverageUtilities;
import org.geotools.util.Utilities;
import org.jaitools.imageutils.ImageLayout2;
import org.opengis.coverage.Coverage;
import org.opengis.coverage.grid.GridCoverage;
import org.opengis.coverage.grid.GridCoverageReader;
import org.opengis.coverage.grid.GridGeometry;
import org.opengis.coverage.processing.Operation;
import org.opengis.geometry.Envelope;
import org.opengis.parameter.ParameterValueGroup;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.MathTransform;
import org.vfny.geoserver.util.WCSUtils;

/**
 * Implementation of the WCS 2.0 GetCoverage request
 * 
 * @author Andrea Aime - GeoSolutions
 * @author Simone Giannecchini, GeoSolutions
 */
public class GetCoverage {
    
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
            public GridCoverage2D scale(GridCoverage2D sourceGC, ScalingType scaling) {
                return sourceGC;
            }
            
        },
        ScaleByFactor{

            @Override
            public GridCoverage2D scale(GridCoverage2D sourceGC, ScalingType scaling) {
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
                return (GridCoverage2D)Operations.DEFAULT.scale(sourceGC, scaleFactor, scaleFactor, 0, 0, Interpolation.getInstance(Interpolation.INTERP_NEAREST));
            }
            
        },
        ScaleToSize{

            /**
             * In this case we must retain the lower bounds by scale the size, hence {@link ScaleDescriptor} JAI operation 
             * cannot be used. Same goes for {@link AffineDescriptor}, the only real option is {@link WarpDescriptor}.
             * 
             */
            @Override
            public GridCoverage2D scale(GridCoverage2D sourceGC, ScalingType scaling) {
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
                parameters.parameter("interpolation").setValue(Interpolation.getInstance(Interpolation.INTERP_NEAREST));
                parameters.parameter( "backgroundValues").setValue(CoverageUtilities.getBackgroundValues(sourceGC));// TODO check and improve
                return (GridCoverage2D) CoverageProcessor.getInstance().doOperation(parameters,hints);
            }

        },
        ScaleToExtent{

            @Override
            public GridCoverage2D scale(GridCoverage2D sourceGC, ScalingType scaling) {
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
                parameters.parameter("interpolation").setValue(Interpolation.getInstance(Interpolation.INTERP_NEAREST));
                parameters.parameter( "backgroundValues").setValue(CoverageUtilities.getBackgroundValues(sourceGC));// TODO check and improve
                return (GridCoverage2D) CoverageProcessor.getInstance().doOperation(parameters,hints);
            }
            
        },        
        ScaleAxesByFactor{

            @Override
            public GridCoverage2D scale(GridCoverage2D sourceGC, ScalingType scaling) {
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
                
                return (GridCoverage2D) Operations.DEFAULT.scale(sourceGC, scaleFactorX, scaleFactorY, 0, 0);
            }
            
        };
        /**
         * @param scaling 
         * @param sourceGG
         * @param width 
         * @param origin 
         * @param returnValue
         */
        abstract public GridCoverage2D scale(GridCoverage2D sourceGC, ScalingType scaling) ;
        

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
        // get the coverage 
        LayerInfo linfo = NCNameResourceCodec.getCoverage(catalog, request.getCoverageId());
        if(linfo == null) {
            throw new WCS20Exception("Could not locate coverage " + request.getCoverageId(), 
                    WCS20Exception.WCS20ExceptionCode.NoSuchCoverage, "coverageId");
        } 
        
        // TODO: handle trimming and slicing
        
        CoverageInfo cinfo = (CoverageInfo) linfo.getResource();
        GridCoverage2D coverage = null;
        try {
            
            GridCoverageReader reader = cinfo.getGridCoverageReader(null, null);
            
            // use this to check if we can use overviews or not
            boolean subsample = wcs.isSubsamplingEnabled();
            
            Map<String, ExtensionItemType> extensions = extractExtensions(coverage,request);
            
            // TODO: setup the params to force the usage of imageread and to make it use
            // the right overview and so on
            // TODO here we should really try to subset before reading with a grid geometry
            // TODO here we should specify to work in streaming fashion
            coverage = (GridCoverage2D) reader.read(null);
            
            // TODO: handle crop, scale, reproject and so on
            
            // get CRS extension values
            final CoordinateReferenceSystem subsettingCRS=extractSubsettingCRS(coverage,extensions);
            final CoordinateReferenceSystem outputCRS=extractOutputCRS(coverage,extensions);
            
            // handle interpolation
            final Map<String,InterpolationPolicy> axesInterpolations=extractInterpolation(coverage,extensions);
            
            // handle range subsetting
            coverage=handleRangeSubsettingExtension(coverage,extensions);
            
            // subsetting, is not really an extension
            coverage=handleSubsettingExtension(coverage,request,subsettingCRS);
            
            // scaling
            coverage=handleScaling(coverage,extensions,axesInterpolations);
            
            // reprojection
            coverage=handleReprojection(coverage,outputCRS,axesInterpolations);
            
            
            // reproject
        } catch(IOException e) {
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
     * @param coverage
     * @param extensions
     * @return
     */
    private CoordinateReferenceSystem extractOutputCRS(GridCoverage2D coverage,
            Map<String, ExtensionItemType> extensions) {
        return extractCRS(coverage,extensions,false);
    }

    /**
     * @param coverage
     * @param extensions
     * @return
     */
    private CoordinateReferenceSystem extractSubsettingCRS(GridCoverage2D coverage,Map<String, ExtensionItemType> extensions) {
        return extractCRS(coverage,extensions,true);
    }

    /**
     * @param coverage
     * @param extensions
     * @param b
     * @return
     */
    private CoordinateReferenceSystem extractCRS(GridCoverage2D coverage, Map<String, ExtensionItemType> extensions,
            boolean subsettingCRS) {
        
        // look for subsettingCRS Extension extension
        if(extensions==null||extensions.size()==0||!extensions.containsKey(subsettingCRS ? "subsettingCrs" : "outputCrs")){
             // NO INTERPOLATION
            return coverage.getCoordinateReferenceSystem2D();
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
        return coverage.getCoordinateReferenceSystem2D();
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
     * @param coverage 
     * @param extensions2
     * @return
     */
    private Map<String,InterpolationPolicy> extractInterpolation(GridCoverage2D coverage, Map<String, ExtensionItemType> extensions) {
        // preparation
        final Map<String,InterpolationPolicy> returnValue= new HashMap<String, InterpolationPolicy>();
        final Envelope envelope= coverage.getEnvelope();
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
     * @param axesInterpolations 
     * @return
     */
    private GridCoverage2D handleReprojection(GridCoverage2D coverage, CoordinateReferenceSystem outputCRS, Map<String, InterpolationPolicy> axesInterpolations) {

        // check the two crs tosee if we really need to do anything
        if(CRS.equalsIgnoreMetadata(coverage.getCoordinateReferenceSystem2D(), outputCRS)){
            return coverage;
        }
        // resample
        return (GridCoverage2D) Operations.DEFAULT.resample(coverage, outputCRS);
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
    
        // look for scaling extension
        if(extensions==null||extensions.size()==0||!extensions.containsKey("rangeSubset")){
             // NO INTERPOLATION
            return coverage;
        }
            
        // look for an interpolation extension
        final ExtensionItemType extensionItem=extensions.get("rangeSubset");
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
                
                returnValue.add(startRangeComponent);
                returnValue.add(endRangeComponent );
            } else {
                returnValue.add(rangeComponent);
            }
        }
   
        return coverage;
    }

    /**
     * @param coverage
     * @param request
     * @param subsettingCRS 
     * @return
     */
    private GridCoverage2D handleSubsettingExtension(
            GridCoverage2D coverage, 
            GetCoverageType request, 
            CoordinateReferenceSystem subsettingCRS) {

        // extract subsetting
        final GeneralEnvelope subset=extractSubsettingEnvelope(coverage,request,subsettingCRS);
        if(subset!=null){
            return WCSUtils.crop(coverage, subset); // TODO I hate this classes that do it all
        }
        return coverage;
    }

    /**
     * @param coverage
     * @param axesInterpolations 
     * @param extensions2
     * @return
     */
    private GridCoverage2D handleScaling(GridCoverage2D coverage, Map<String, ExtensionItemType> extensions, Map<String, InterpolationPolicy> axesInterpolations) {

        // look for scaling extension
        if(extensions==null||extensions.size()==0||!extensions.containsKey("Scaling")){
             // NO SCALING
            return coverage;
        }
        
        // look for a scaling extension
        final ExtensionItemType extensionItem=extensions.get("Scaling");
        if(extensionItem!=null){
            
            // get scaling
            ScalingType scaling=(ScalingType) extensionItem.getObjectContent();
            
            // instantiate enum
            
            final ScalingPolicy scalingPolicy=  ScalingPolicy.getPolicy(scaling);
            return scalingPolicy.scale(coverage, scaling);
        }

        // no scaling
        return coverage;
    }

    /**
     * @param coverage
     * @param request
     * @param subsettingCRS 
     * @return
     */
    private GeneralEnvelope extractSubsettingEnvelope(
            GridCoverage coverage, 
            GetCoverageType request, 
            CoordinateReferenceSystem subsettingCRS) {
        //default envelope
        final CoordinateReferenceSystem sourceCRS=coverage.getCoordinateReferenceSystem();
        GeneralEnvelope envelope=null;
        if(subsettingCRS==null||CRS.equalsIgnoreMetadata(subsettingCRS,sourceCRS)){
            envelope=new GeneralEnvelope(coverage.getEnvelope());
        } else {
            // reproject source coverage to subsetting crs for initialization
            try {
                envelope= CRS.transform(
                        CRS.findMathTransform(coverage.getCoordinateReferenceSystem(), subsettingCRS), 
                        coverage.getEnvelope());
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
        
        // check what we need
        final EList<DimensionSubsetType> dimensions = request.getDimensionSubset();
        if(dimensions==null||dimensions.size()<=0){
            return null;
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
                AffineTransform affineTransform = getAffineTransform(coverage);
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
        final GeneralEnvelope sourceEnvelope = new GeneralEnvelope(coverage.getEnvelope());
        if(CRS.equalsIgnoreMetadata(envelope, sourceEnvelope.getCoordinateReferenceSystem())){
            envelope.intersect(sourceEnvelope);
            envelope.setCoordinateReferenceSystem(coverage.getCoordinateReferenceSystem());
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

    /**
     * Returns the "Sample to geophysics" transform as an affine transform, or {@code null}
     * if none. Note that the returned instance may be an immutable one, not necessarly the
     * default Java2D implementation.
     *
     * @param  coverage The coverage for which to get the "grid to CRS" affine transform.
     * @return The "grid to CRS" affine transform of the given coverage, or {@code null}
     *         if none or if the transform is not affine.
     */
    static AffineTransform getAffineTransform(final Coverage coverage) {
        if (coverage instanceof GridCoverage) {
            final GridGeometry geometry = ((GridCoverage) coverage).getGridGeometry();
            if (geometry != null) {
                final MathTransform gridToCRS;
                if (geometry instanceof GridGeometry2D) {
                    gridToCRS = ((GridGeometry2D) geometry).getGridToCRS();
                } else {
                    gridToCRS = geometry.getGridToCRS();
                }
                if (gridToCRS instanceof AffineTransform) {
                    return (AffineTransform) gridToCRS;
                }
            }
        }
        return null;
    }

}
