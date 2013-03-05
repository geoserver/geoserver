package org.geoserver.wcs2_0;

import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.geom.NoninvertibleTransformException;
import java.awt.image.RenderedImage;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.media.jai.BorderExtender;
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
import org.geoserver.platform.ServiceException;
import org.geoserver.wcs.CoverageCleanerCallback;
import org.geoserver.wcs.WCSInfo;
import org.geoserver.wcs2_0.exception.WCS20Exception;
import org.geoserver.wcs2_0.exception.WCS20Exception.WCS20ExceptionCode;
import org.geoserver.wcs2_0.util.EnvelopeAxesLabelsMapper;
import org.geoserver.wcs2_0.util.NCNameResourceCodec;
import org.geoserver.wcs2_0.util.RequestUtils;
import org.geotools.coverage.CoverageFactoryFinder;
import org.geotools.coverage.GridSampleDimension;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.coverage.grid.GridEnvelope2D;
import org.geotools.coverage.grid.GridGeometry2D;
import org.geotools.coverage.grid.io.AbstractGridCoverage2DReader;
import org.geotools.coverage.grid.io.AbstractGridFormat;
import org.geotools.coverage.processing.CoverageProcessor;
import org.geotools.coverage.processing.operation.Scale;
import org.geotools.factory.GeoTools;
import org.geotools.factory.Hints;
import org.geotools.geometry.GeneralEnvelope;
import org.geotools.referencing.CRS;
import org.geotools.referencing.ReferencingFactoryFinder;
import org.geotools.referencing.operation.transform.AffineTransform2D;
import org.geotools.resources.coverage.CoverageUtilities;
import org.geotools.util.DefaultProgressListener;
import org.geotools.util.Utilities;
import org.geotools.util.logging.Logging;
import org.jaitools.imageutils.ImageLayout2;
import org.opengis.coverage.SampleDimension;
import org.opengis.coverage.grid.GridCoverage;
import org.opengis.coverage.grid.GridEnvelope;
import org.opengis.coverage.processing.Operation;
import org.opengis.geometry.Envelope;
import org.opengis.parameter.GeneralParameterValue;
import org.opengis.parameter.ParameterValueGroup;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.crs.CRSAuthorityFactory;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.datum.PixelInCell;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.TransformException;
import org.vfny.geoserver.util.WCSUtils;

/**
 * Implementation of the WCS 2.0.1 GetCoverage request
 * 
 * @author Andrea Aime - GeoSolutions
 * @author Simone Giannecchini, GeoSolutions
 */
public class GetCoverage {
    
    /** Logger.*/
    private Logger LOGGER= Logging.getLogger(GetCoverage.class);
    
    /** enum that representation the possible interpolation values.**/
    private enum InterpolationPolicy{
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
        /**
         * Default interpolation policy for this implementation.
         * @return an instance of {@link InterpolationPolicy} which is actually the default one.
         */
        static InterpolationPolicy getDefaultPolicy(){
            return nearestneighbor;
        }
    }
    
    /**
     * {@link Enum} for implementing the management of the various scaling options available 
     * for the scaling extension.
     * 
     * <p>
     * This enum works as a factory to separate the code that handles the scaling operations.
     * 
     * @author Simone Giannecchini, GeoSolutions
     *
     */
    private enum ScalingPolicy{
        DoNothing{

            @Override
            public GridCoverage2D scale(GridCoverage2D sourceGC, ScalingType scaling,Interpolation interpolation,Hints hints, WCSInfo wcsinfo) {
                Utilities.ensureNonNull("sourceGC", sourceGC);
                Utilities.ensureNonNull("ScalingType", scaling);
                Utilities.ensureNonNull("Interpolation", interpolation);                
                return sourceGC;
            }
            
        },
        /**
         * In this case we scale each axis by the same factor. 
         * 
         * <p>
         * We do rely on the {@link Scale} operation.
         */
        ScaleByFactor{

            @Override
            public GridCoverage2D scale(GridCoverage2D sourceGC, ScalingType scaling,Interpolation interpolation,Hints hints, WCSInfo wcsinfo) {
                Utilities.ensureNonNull("sourceGC", sourceGC);
                Utilities.ensureNonNull("ScalingType", scaling);
                
                // get scale factor
                final ScaleByFactorType scaleByFactorType = scaling.getScaleByFactor();
                double scaleFactor=scaleByFactorType.getScaleFactor();
                
                // checks
                if(scaleFactor<=0){
                    throw new WCS20Exception("Invalid scale factor", WCS20Exception.WCS20ExceptionCode.InvalidScaleFactor, String.valueOf(scaleFactor));
                }
                
                // return coverage unchanged if we don't scale
                if(scaleFactor==1){
                    // NO SCALING do we need interpolation?
                    if(interpolation instanceof InterpolationNearest){
                        return sourceGC;
                    } else {
                        // interpolate coverage if requested and not nearest!!!!         
                        final Operation operation = CoverageProcessor.getInstance().getOperation("Warp");
                        final ParameterValueGroup parameters = operation.getParameters();
                        parameters.parameter("Source").setValue(sourceGC);
                        parameters.parameter("warp").setValue(new WarpAffine(AffineTransform.getScaleInstance(1, 1)));//identity
                        parameters.parameter("interpolation").setValue(interpolation);
                        parameters.parameter( "backgroundValues").setValue(CoverageUtilities.getBackgroundValues(sourceGC));// TODO check and improve
                        return (GridCoverage2D) CoverageProcessor.getInstance(hints).doOperation(parameters,hints);
                    }      
                }

                // ==== check limits
                final GridGeometry2D gridGeometry = sourceGC.getGridGeometry();
                final GridEnvelope gridRange = gridGeometry.getGridRange();
                WCSUtils.checkOutputLimits(
                        wcsinfo, 
                        new GridEnvelope2D(
                                0,
                                0,
                                (int)(gridRange.getSpan(gridGeometry.gridDimensionX)*scaleFactor),
                                (int)(gridRange.getSpan(gridGeometry.gridDimensionY)*scaleFactor)), 
                        sourceGC.getRenderedImage().getSampleModel());
                

                // === scale
                final Operation operation = CoverageProcessor.getInstance().getOperation("Scale");
                final ParameterValueGroup parameters = operation.getParameters();
                parameters.parameter("Source").setValue(sourceGC);
                parameters.parameter("interpolation").setValue(interpolation!=null?interpolation:InterpolationPolicy.getDefaultPolicy().getInterpolation());
                parameters.parameter( "xScale").setValue(scaleFactor);
                parameters.parameter( "yScale").setValue(scaleFactor);
                parameters.parameter( "xTrans").setValue(0.0);
                parameters.parameter( "yTrans").setValue(0.0);
                return (GridCoverage2D) CoverageProcessor.getInstance(hints).doOperation(parameters,hints);   
            }
            
        },
        
        /**
         * In this case we scale each axis bto a predefined size. 
         * 
         * <p>
         * We do rely on the {@link org.geotools.coverage.processing.operation.Warp} operation as the final 
         * size must be respected on each axis.
         */        
        ScaleToSize{

            /**
             * In this case we must retain the lower bounds by scale the size, hence {@link ScaleDescriptor} JAI operation 
             * cannot be used. Same goes for {@link AffineDescriptor}, the only real option is {@link WarpDescriptor}.
             * @param wcsinfo 
             * 
             */
            @Override
            public GridCoverage2D scale(GridCoverage2D sourceGC, ScalingType scaling, Interpolation interpolation,Hints hints, WCSInfo wcsinfo) {
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
                    throw new WCS20Exception("Invalid target size", WCS20Exception.WCS20ExceptionCode.InvalidExtent, Integer.toString(sizeX));
                }
                final int sizeY=(int) ySize.getTargetSize();// TODO should this be int?
                if(sizeY<=0){
                    throw new WCS20Exception("Invalid target size", WCS20Exception.WCS20ExceptionCode.InvalidExtent, Integer.toString(sizeY));
                }
                
                // scale
                final GridEnvelope2D sourceGE=sourceGC.getGridGeometry().getGridRange2D();
                if(sizeY==sourceGE.width&& sizeX==sourceGE.height){
                    // NO SCALING do we need interpolation?
                    if(interpolation instanceof InterpolationNearest){
                        return sourceGC;
                    } else {
                        // interpolate coverage if requested and not nearest!!!!         
                        final Operation operation = CoverageProcessor.getInstance().getOperation("Warp");
                        final ParameterValueGroup parameters = operation.getParameters();
                        parameters.parameter("Source").setValue(sourceGC);
                        parameters.parameter("warp").setValue(new WarpAffine(AffineTransform.getScaleInstance(1, 1)));//identity
                        parameters.parameter("interpolation").setValue(interpolation);
                        parameters.parameter( "backgroundValues").setValue(CoverageUtilities.getBackgroundValues(sourceGC));// TODO check and improve
                        return (GridCoverage2D) CoverageProcessor.getInstance().doOperation(parameters,hints);
                    }                 
                }
                
                // === enforce output limits
                WCSUtils.checkOutputLimits(wcsinfo, new GridEnvelope2D(0,0,sizeX,sizeY), sourceGC.getRenderedImage().getSampleModel());
                
                // create final warp
                final double scaleX = 1.0*sizeX/sourceGE.width;
                final double scaleY = 1.0*sizeY/sourceGE.height;
                final RenderedImage sourceImage= sourceGC.getRenderedImage();
                final int sourceMinX = sourceImage.getMinX();
                final int sourceMinY = sourceImage.getMinY();
                final AffineTransform affineTransform = new AffineTransform(
                        scaleX,
                        0,
                        0, 
                        scaleY,
                        sourceMinX-scaleX*sourceMinX,   //preserve sourceImage.getMinX() 
                        sourceMinY-scaleY*sourceMinY);  //preserve sourceImage.getMinY() as per spec
                Warp warp;
                try {
                    warp = new  WarpAffine(
                            affineTransform.createInverse());
                } catch (NoninvertibleTransformException e) {
                    throw new RuntimeException(e);
                }
                // impose final 
                final ImageLayout2 layout = new ImageLayout2(
                        sourceMinX,
                        sourceMinY,
                        sizeX,
                        sizeY);
                hints.add(new Hints(JAI.KEY_IMAGE_LAYOUT, layout));                
                final Operation operation = CoverageProcessor.getInstance().getOperation("Warp");
                final ParameterValueGroup parameters = operation.getParameters();
                parameters.parameter("Source").setValue(sourceGC);
                parameters.parameter("warp").setValue(warp);
                parameters.parameter("interpolation").setValue(interpolation!=null?interpolation:InterpolationPolicy.getDefaultPolicy().getInterpolation());
                parameters.parameter( "backgroundValues").setValue(CoverageUtilities.getBackgroundValues(sourceGC));// TODO check and improve
                GridCoverage2D gc = (GridCoverage2D) CoverageProcessor.getInstance().doOperation(parameters,hints);
                return gc;
            }

        },
        /**
         * In this case we scale each axis to a predefined extent. 
         * 
         * <p>
         * We do rely on the {@link org.geotools.coverage.processing.operation.Warp} operation as the final 
         * extent must be respected on each axis.
         */  
        ScaleToExtent{

            @Override
            public GridCoverage2D scale(GridCoverage2D sourceGC, ScalingType scaling, Interpolation interpolation,Hints hints, WCSInfo wcsinfo) {
                Utilities.ensureNonNull("sourceGC", sourceGC);
                Utilities.ensureNonNull("ScalingType", scaling);
                
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
                    throw new WCS20Exception("Missing extent along i", WCS20Exception.WCS20ExceptionCode.InvalidExtent, "Null");
                }
                if(yExtent==null){
                    throw new WCS20Exception("Missing extent along j", WCS20Exception.WCS20ExceptionCode.InvalidExtent, "Null");
                }  
                
                final int minx=(int) targetAxisExtentElements.get(0).getLow();// TODO should this be int?
                final int maxx=(int) targetAxisExtentElements.get(0).getHigh();
                final int miny=(int) targetAxisExtentElements.get(1).getLow();
                final int maxy=(int) targetAxisExtentElements.get(1).getHigh();               
                
                // check on source geometry
                final GridEnvelope2D sourceGE=sourceGC.getGridGeometry().getGridRange2D();

                if(minx>=maxx){
                    throw new WCS20Exception("Invalid Extent for dimension:"+targetAxisExtentElements.get(0).getAxis() , WCS20Exception.WCS20ExceptionCode.InvalidExtent, String.valueOf(maxx));
                }
                if(miny>=maxy){
                    throw new WCS20Exception("Invalid Extent for dimension:"+targetAxisExtentElements.get(1).getAxis() , WCS20Exception.WCS20ExceptionCode.InvalidExtent, String.valueOf(maxy));
                }                
                final Rectangle destinationRectangle= new Rectangle(minx, miny,maxx-minx+1, maxy-miny+1);
                // UNSCALE
                if(destinationRectangle.equals(sourceGE)){
                    // NO SCALING do we need interpolation?
                    if(interpolation instanceof InterpolationNearest){
                        return sourceGC;
                    } else {
                        // interpolate coverage if requested and not nearest!!!!         
                        final Operation operation = CoverageProcessor.getInstance().getOperation("Warp");
                        final ParameterValueGroup parameters = operation.getParameters();
                        parameters.parameter("Source").setValue(sourceGC);
                        parameters.parameter("warp").setValue(new WarpAffine(AffineTransform.getScaleInstance(1, 1)));//identity
                        parameters.parameter("interpolation").setValue(interpolation);
                        parameters.parameter( "backgroundValues").setValue(CoverageUtilities.getBackgroundValues(sourceGC));// TODO check and improve
                        return (GridCoverage2D) CoverageProcessor.getInstance(hints).doOperation(parameters,hints);
                    }      
                }
                
                // === enforce output limits
                WCSUtils.checkOutputLimits(
                        wcsinfo, 
                        new GridEnvelope2D(destinationRectangle), sourceGC.getRenderedImage().getSampleModel());
                
                // create final warp
                final double scaleX = 1.0*destinationRectangle.width/sourceGE.width;
                final double scaleY = 1.0*destinationRectangle.height/sourceGE.height;
                final RenderedImage sourceImage= sourceGC.getRenderedImage();
                final int sourceMinX = sourceImage.getMinX();
                final int sourceMinY = sourceImage.getMinY();
                final AffineTransform affineTransform = new AffineTransform(
                        scaleX,
                        0,
                        0, 
                        scaleY,
                        destinationRectangle.x-scaleX*sourceMinX,   //preserve sourceImage.getMinX() 
                        destinationRectangle.y-scaleY*sourceMinY);  //preserve sourceImage.getMinY() as per spec
                Warp warp;
                try {
                    warp = new  WarpAffine(
                            affineTransform.createInverse());
                } catch (NoninvertibleTransformException e) {
                    throw new RuntimeException(e);
                }
                
                // impose size
                final ImageLayout2 layout = new ImageLayout2(
                        destinationRectangle.x,
                        destinationRectangle.y,
                        destinationRectangle.width,
                        destinationRectangle.height);
                hints.add(new Hints(JAI.KEY_IMAGE_LAYOUT, layout));
                
                final Operation operation = CoverageProcessor.getInstance().getOperation("Warp");
                final ParameterValueGroup parameters = operation.getParameters();
                parameters.parameter("Source").setValue(sourceGC);
                parameters.parameter("warp").setValue(warp);
                parameters.parameter("interpolation").setValue(interpolation!=null?interpolation:InterpolationPolicy.getDefaultPolicy().getInterpolation());
                parameters.parameter( "backgroundValues").setValue(CoverageUtilities.getBackgroundValues(sourceGC));// TODO check and improve
                GridCoverage2D gc = (GridCoverage2D) CoverageProcessor.getInstance().doOperation(parameters,hints);
//                RenderedImageBrowser.showChain(gc.getRenderedImage(),false);
                return gc;
            }
            
        },   
        /**
         * In this case we scale each axis by the a provided factor. 
         * 
         * <p>
         * We do rely on the {@link Scale} operation.
         */
        ScaleAxesByFactor{

            @Override
            public GridCoverage2D scale(GridCoverage2D sourceGC, ScalingType scaling,Interpolation interpolation,Hints hints, WCSInfo wcsinfo) {
                Utilities.ensureNonNull("sourceGC", sourceGC);
                Utilities.ensureNonNull("ScalingType", scaling);
                
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
                    throw new WCS20Exception("Missing scale factor along i", WCS20Exception.WCS20ExceptionCode.InvalidScaleFactor, "Null");
                }
                if(yScale==null){
                    throw new WCS20Exception("Missing scale factor along j", WCS20Exception.WCS20ExceptionCode.InvalidScaleFactor, "Null");
                }                

                final double scaleFactorX= xScale.getScaleFactor();
                if(scaleFactorX<=0){
                    throw new WCS20Exception("Invalid scale factor", WCS20Exception.WCS20ExceptionCode.InvalidScaleFactor, Double.toString(scaleFactorX));
                }   
                final double scaleFactorY= yScale.getScaleFactor();
                if(scaleFactorY<=0){
                    throw new WCS20Exception("Invalid scale factor", WCS20Exception.WCS20ExceptionCode.InvalidScaleFactor, Double.toString(scaleFactorY));
                }                  
                
                // unscale
                if(scaleFactorX==1.0&& scaleFactorY==1.0){
                    // NO SCALING do we need interpolation?
                    if(interpolation instanceof InterpolationNearest){
                        return sourceGC;
                    } else {
                        // interpolate coverage if requested and not nearest!!!!         
                        final Operation operation = CoverageProcessor.getInstance().getOperation("Warp");
                        final ParameterValueGroup parameters = operation.getParameters();
                        parameters.parameter("Source").setValue(sourceGC);
                        parameters.parameter("warp").setValue(new WarpAffine(AffineTransform.getScaleInstance(1, 1)));//identity
                        parameters.parameter("interpolation").setValue(interpolation);
                        parameters.parameter( "backgroundValues").setValue(CoverageUtilities.getBackgroundValues(sourceGC));// TODO check and improve
                        return (GridCoverage2D) CoverageProcessor.getInstance(hints).doOperation(parameters,hints);
                    }      
                }  


                // ==== check limits
                final GridGeometry2D gridGeometry = sourceGC.getGridGeometry();
                final GridEnvelope gridRange = gridGeometry.getGridRange();
                WCSUtils.checkOutputLimits(
                        wcsinfo, 
                        new GridEnvelope2D(
                                0,
                                0,
                                (int)(gridRange.getSpan(gridGeometry.gridDimensionX)*scaleFactorX),
                                (int)(gridRange.getSpan(gridGeometry.gridDimensionY)*scaleFactorY)), 
                        sourceGC.getRenderedImage().getSampleModel());
                
                // scale
                final Operation operation = CoverageProcessor.getInstance().getOperation("Scale");
                final ParameterValueGroup parameters = operation.getParameters();
                parameters.parameter("Source").setValue(sourceGC);
                parameters.parameter("interpolation").setValue(interpolation!=null?interpolation:InterpolationPolicy.getDefaultPolicy().getInterpolation());
                parameters.parameter( "xScale").setValue(scaleFactorX);
                parameters.parameter( "yScale").setValue(scaleFactorY);
                parameters.parameter( "xTrans").setValue(0.0);
                parameters.parameter( "yTrans").setValue(0.0);
                return (GridCoverage2D) CoverageProcessor.getInstance(hints).doOperation(parameters,hints);         
            }
            
        };
        /**
         * Scale the provided {@link GridCoverage2D} according to the provided {@link ScalingType} and the provided {@link Interpolation} and {@link Hints}.
         * 
         * @param sourceGC the {@link GridCoverage2D} to scale.
         * @param scaling the instance of {@link ScalingType} that contains he type of scaling to perform.
         * @param interpolation the {@link Interpolation} to use. In case it is <code>null</code> we will use the {@link InterpolationPolicy} default value.
         * @param hints {@link Hints} to use during this operation.
         * @param wcsinfo the current instance of {@link WCSInfo} that contains wcs config for GeoServer
         * @return a scaled version of the input {@link GridCoverage2D}. It cam be subsampled or oversampled, it depends on the {@link ScalingType} content.
         */
        abstract public GridCoverage2D scale(GridCoverage2D sourceGC, ScalingType scaling, Interpolation interpolation, Hints hints, WCSInfo wcsinfo) ;
        

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
    
    /** A URI authorithy with lonlat order.*/
    private CRSAuthorityFactory lonLatCRSFactory;
    
    /** A URI authorithy with latlon order.*/
    private CRSAuthorityFactory latLonCRSFactory;
    
    public final static String SRS_STARTER="http://www.opengis.net/def/crs/EPSG/0/";

    public GetCoverage(WCSInfo serviceInfo, Catalog catalog, EnvelopeAxesLabelsMapper envelopeDimensionsMapper) {
        this.wcs = serviceInfo;
        this.catalog = catalog;
        this.envelopeDimensionsMapper=envelopeDimensionsMapper;
        
        // building the needed URI CRS Factories
        Hints hints = GeoTools.getDefaultHints().clone();
        hints.add(new Hints(Hints.FORCE_LONGITUDE_FIRST_AXIS_ORDER,Boolean.TRUE));
        hints.add(new Hints(Hints.FORCE_AXIS_ORDER_HONORING, "http-uri"));
        lonLatCRSFactory = ReferencingFactoryFinder.getCRSAuthorityFactory("http://www.opengis.net/def", hints); 
        
        hints.add(new Hints(Hints.FORCE_LONGITUDE_FIRST_AXIS_ORDER,Boolean.FALSE));
        hints.add(new Hints(Hints.FORCE_AXIS_ORDER_HONORING, "http-uri"));
        latLonCRSFactory = ReferencingFactoryFinder.getCRSAuthorityFactory("http://www.opengis.net/def", hints); 
        
    }

    /**
     * Executes the provided {@link GetCoverageType}.
     * 
     * @param request the {@link GetCoverageType} to be executed.
     * @return the {@link GridCoverage} produced by the chain of operations specified by the provided {@link GetCoverageType}.
     */
    public GridCoverage run(GetCoverageType request) {

        //
        // get the coverage info from the catalog or throw an exception if we don't find it
        //
        final LayerInfo linfo = NCNameResourceCodec.getCoverage(catalog, request.getCoverageId());
        if(linfo == null) {
            throw new WCS20Exception("Could not locate coverage " + request.getCoverageId(), 
                    WCS20Exception.WCS20ExceptionCode.NoSuchCoverage, "coverageId");
        } 
        final CoverageInfo cinfo = (CoverageInfo) linfo.getResource();
        if(LOGGER.isLoggable(Level.FINE)){
            LOGGER.fine("Executing GetCoverage request on coverage :"+linfo.toString());
        }
        
        // === k, now start the execution
        GridCoverage2D coverage = null;
        try {
            
            // === extract all extensions for later usage
            Map<String, ExtensionItemType> extensions = extractExtensions(request);

            // === prepare the hints to use
            // here I find if I can use overviews and do subsampling
            final Hints hints = GeoTools.getDefaultHints();
            hints.add(WCSUtils.getReaderHints(wcs));
            hints.add(new RenderingHints(JAI.KEY_BORDER_EXTENDER,BorderExtender.createInstance(BorderExtender.BORDER_COPY)));
//            hints.add(new RenderingHints(JAI.KEY_REPLACE_INDEX_COLOR_MODEL,Boolean.FALSE));// TODO check interpolation

            //
            //
            // get a reader for this coverage
            final AbstractGridCoverage2DReader reader = (AbstractGridCoverage2DReader) cinfo.getGridCoverageReader(
                    new DefaultProgressListener(), 
                    hints);
            
            //
            // Extract CRS values for relative extension
            //
            final CoordinateReferenceSystem subsettingCRS=extractSubsettingCRS(reader,extensions);
            final CoordinateReferenceSystem outputCRS=extractOutputCRS(reader,extensions,subsettingCRS);
            final boolean enforceLatLonAxesOrder=requestingLatLonAxesOrder(outputCRS);
            // extract subsetting
            final GeneralEnvelope subset=extractSubsettingEnvelope(reader,request,subsettingCRS);
            assert subset!=null&&!subset.isEmpty();
            
            //
            // Handle interpolation extension 
            //
            // notice that for the moment we support only homogeneous interpolation on the 2D axis
            final Map<String,InterpolationPolicy> axesInterpolations=extractInterpolation(reader,extensions);
            final Interpolation spatialInterpolation=extractSpatialInterpolation(axesInterpolations,reader.getOriginalEnvelope());
            // TODO time interpolation
            assert spatialInterpolation!=null;
            
            
            //
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
            
            //
            // handle range subsetting
            //        
            coverage=handleRangeSubsettingExtension(coverage,extensions,hints);
          
            
            //
            // subsetting, is not really an extension
            //
            coverage=handleSubsettingExtension(coverage,subset,hints);
            
            //
            // scaling extension
            //
            // scaling is done in raster space with eventual interpolation
            coverage=handleScaling(coverage,extensions,spatialInterpolation,hints);
            
            
            //
            // reprojection
            //
            // reproject the output coverage to an eventual outputCrs
            coverage=handleReprojection(coverage,outputCRS,spatialInterpolation,hints);

            //
            // axes swap management
            //
            if(enforceLatLonAxesOrder){
                coverage = enforceLatLongOrder(coverage, hints, outputCRS);
            }
            
            // 
            // Output limits checks
            // We need to enforce them once again as it might be that no scaling or rangesubsetting is requested
            WCSUtils.checkOutputLimits(wcs, coverage.getGridGeometry().getGridRange2D(), coverage.getRenderedImage().getSampleModel());
        } catch(ServiceException e) {
            throw e;
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
     * @param coverage
     * @param hints
     * @param outputCRS
     * @return
     * @throws Exception
     */
    private GridCoverage2D enforceLatLongOrder(GridCoverage2D coverage, final Hints hints,
            final CoordinateReferenceSystem outputCRS) throws Exception {
        final Integer epsgCode = CRS.lookupEpsgCode(outputCRS, false);
        if(epsgCode!=null&& epsgCode>0){
            // final CRS
            CoordinateReferenceSystem finalCRS = latLonCRSFactory.createCoordinateReferenceSystem(SRS_STARTER+epsgCode);
            if(CRS.getAxisOrder(outputCRS).equals(CRS.getAxisOrder(finalCRS))){
                return coverage;
            }
            
            // get g2w and swap axes
            final AffineTransform g2w= new AffineTransform((AffineTransform2D) coverage.getGridGeometry().getGridToCRS2D());
            g2w.preConcatenate(CoverageUtilities.AXES_SWAP);
            
            // rework the transformation
            final GridGeometry2D finalGG= new GridGeometry2D(
                    coverage.getGridGeometry().getGridRange(), 
                    PixelInCell.CELL_CENTER, 
                    new AffineTransform2D(g2w), 
                    finalCRS, 
                    hints);
            
            // recreate the coverage
            coverage=CoverageFactoryFinder.getGridCoverageFactory(hints).create(
                    coverage.getName(),
                    coverage.getRenderedImage(),
                    finalGG,
                    coverage.getSampleDimensions(),
                    new GridCoverage[]{coverage},
                    coverage.getProperties()
                    );
        }
        return coverage;
    }

    /**
     * This utility method tells me whether or not we should do a final reverse on the axis of the data.
     * 
     * @param outputCRS the final {@link CoordinateReferenceSystem} for the data as per the request 
     * 
     * @return <code>true</code> in case we need to swap axes, <code>false</code> otherwise.
     */
    private boolean requestingLatLonAxesOrder(CoordinateReferenceSystem outputCRS) {
       
        try {
            final Integer epsgCode = CRS.lookupEpsgCode(outputCRS, false);
            if(epsgCode!=null&& epsgCode>0){
                CoordinateReferenceSystem originalCRS = latLonCRSFactory.createCoordinateReferenceSystem(SRS_STARTER+epsgCode);
                return !CRS.getAxisOrder(originalCRS).equals(CRS.getAxisOrder(outputCRS));
            }
        } catch (FactoryException e) {
            LOGGER.log(Level.INFO, e.getMessage(), e);
            return false;
        }
        return false;
    }

    /**
     * This method is responsible for extracting the spatial interpolation from the provided {@link GetCoverageType} 
     * request.
     * 
     * <p>
     * We don't support mixed interpolation at this time and we will never support it for grid axes.
     * 
     * @param axesInterpolations the association between axes URIs and the requested interpolations.
     * @param envelope the original envelope for the source {@link GridCoverage}.
     * @return the requested {@link Interpolation}
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
     * This method is responsible for reading 
     * @param cinfo
     * @param spatialInterpolation
     * @param subset
     * @param outputCRS
     * @param reader
     * @param hints
     * @return
     * @throws Exception
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
        

        GridCoverage2D coverage=null;
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
            rasterRange.setBounds(rasterRange.x-10, rasterRange.y-10, rasterRange.width+20, rasterRange.height+20);
            rasterRange=rasterRange.intersection(( GridEnvelope2D)reader.getOriginalGridRange());// make sure we are in it
            
            // read
            readGG = new GridGeometry2D(
                    new GridEnvelope2D(rasterRange),
                    PixelInCell.CELL_CENTER,
                    reader.getOriginalGridToWorld(PixelInCell.CELL_CENTER),
                    coverageCRS,
                    hints);            
        }
        
        

        // === read
        // check limits
        WCSUtils.checkInputLimits(wcs,cinfo,reader,readGG);
        coverage= RequestUtils.readBestCoverage(
                reader, 
                readParameters,  
                readGG, 
                spatialInterpolation,
                hints);
        // check limits again
        if(coverage!=null){
            WCSUtils.checkInputLimits(wcs, coverage);
        }
        
        // return
        return coverage;

    }

    /**
     * This method is responsible for etracting the outputCRS.
     * 
     * <p>
     * In case it is not provided the subsettingCRS falls back on the subsettingCRS.
     * @param reader the {@link AbstractGridCoverage2DReader} to be used
     * @param extensions the {@link Map} of extension for this request.
     * @param subsettingCRS  the subsettingCRS as a {@link CoordinateReferenceSystem}
     * @return the outputCRS as a {@link CoordinateReferenceSystem}
     */
    private CoordinateReferenceSystem extractOutputCRS(AbstractGridCoverage2DReader reader,
            Map<String, ExtensionItemType> extensions, CoordinateReferenceSystem subsettingCRS) {
        return extractCRSInternal(extensions, subsettingCRS,true);   
    }

    /**
     * Extract the specified crs being it subsetting or output with proper defaults.
     * 
     * @param extensions the {@link Map}of extensions for this request.
     * @param defaultCRS the defaultCRS as a {@link CoordinateReferenceSystem} for this extraction
     * @param isOutputCRS a <code>boolean</code> which tells me whether the CRS we are looking for is a subsetting or an OutputCRS
     * @return a {@link CoordinateReferenceSystem}.
     * @throws WCS20Exception
     */
    private CoordinateReferenceSystem extractCRSInternal(Map<String, ExtensionItemType> extensions,
            CoordinateReferenceSystem defaultCRS, boolean isOutputCRS) throws WCS20Exception {
        Utilities.ensureNonNull("defaultCRS", defaultCRS);
        final String identifier=isOutputCRS?"outputCrs":"subsettingCrs";
        // look for subsettingCRS Extension extension
        if(extensions==null||extensions.size()==0||!extensions.containsKey( identifier)){
             // NO extension at hand
            return defaultCRS;
        }
        
        // look for an crs extension
        final ExtensionItemType extensionItem=extensions.get(identifier);
        if (extensionItem.getName().equals(identifier)) {
            // get URI
            String crsName = extensionItem.getSimpleContent();

            // checks
            if (crsName == null) {
                throw new WCS20Exception(identifier+" was null",
                        WCS20ExceptionCode.NotACrs, "null");
            }

            // instantiate
            try {
                return lonLatCRSFactory.createCoordinateReferenceSystem(crsName);
            } catch (Exception e) {
                final WCS20Exception exception = new WCS20Exception("Invalid "+identifier,
                        isOutputCRS?WCS20Exception.WCS20ExceptionCode.OutputCrsNotSupported:WCS20Exception.WCS20ExceptionCode.SubsettingCrsNotSupported,
                                crsName);
                exception.initCause(e);
                throw exception;
            }        

        }
        
        // should not happen
        return defaultCRS;
    }

    /**
     * This method is responsible for extracting the subsettingCRS.
     * 
     * <p>
     * In case it is not provided the subsettingCRS falls back on the nativeCRS.
     * 
     * @param reader the {@link AbstractGridCoverage2DReader} to be used
     * @param extensions the {@link Map} of extension for this request.
     * @return the subsettingCRS as a {@link CoordinateReferenceSystem}
     */
    private CoordinateReferenceSystem extractSubsettingCRS(AbstractGridCoverage2DReader reader,Map<String, ExtensionItemType> extensions) {
        Utilities.ensureNonNull("reader", reader);
        return extractCRSInternal(extensions, reader.getCrs(),false);       
    }


    /**
     * This method id responsible for extracting the extensions from the incoming request to 
     * facilitate the work of successive methods.
     * 
     * @param request the {@link GetCoverageType} request to execute.
     * 
     * @return a {@link Map} that maps extension names to {@link ExtensionType}s.
     */
    private Map<String, ExtensionItemType> extractExtensions(GetCoverageType request) {
        // === checks
        Utilities.ensureNonNull("request", request);
        if(LOGGER.isLoggable(Level.FINE)){
            LOGGER.fine("Extracting extensions from provided request");
        }
        
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
                if(LOGGER.isLoggable(Level.FINE)){
                    LOGGER.fine("Parsing extension "+extensionName);
                }                
                if (extensionName.equals("subsettingCrs")) {
                    parsedExtensions.put("subsettingCrs", extensionItem);
                    if(LOGGER.isLoggable(Level.FINE)){
                        LOGGER.fine("Added extension subsettingCrs");
                    }                     
                } else if (extensionName.equals("outputCrs")) {
                    parsedExtensions.put("outputCrs", extensionItem);
                    if(LOGGER.isLoggable(Level.FINE)){
                        LOGGER.fine("Added extension outputCrs");
                    }    
                } else if (extensionName.equals("Scaling")) {
                    parsedExtensions.put("Scaling", extensionItem);
                    if(LOGGER.isLoggable(Level.FINE)){
                        LOGGER.fine("Added extension Scaling");
                    }    
                } else if (extensionName.equals("Interpolation")) {
                    parsedExtensions.put("Interpolation", extensionItem);
                    if(LOGGER.isLoggable(Level.FINE)){
                        LOGGER.fine("Added extension Interpolation");
                    }     
                } else if (extensionName.equals("rangeSubset")||extensionName.equals("RangeSubset")) {
                    parsedExtensions.put("rangeSubset", extensionItem);
                    if(LOGGER.isLoggable(Level.FINE)){
                        LOGGER.fine("Added extension rangeSubset");
                    }    
                } 
            }
        } else if(LOGGER.isLoggable(Level.FINE)){
            LOGGER.fine("No extensions found in provided request");
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
     * This method is responsible for handling reprojection of the source coverage to a certain CRS.
     * @param coverage the {@link GridCoverage} to reproject.
     * @param targetCRS the target {@link CoordinateReferenceSystem}
     * @param spatialInterpolation the {@link Interpolation} to adopt.
     * @param hints {@link Hints} to control the process.
     * @return a new instance of {@link GridCoverage} reprojeted to the targetCRS.
     */
    private GridCoverage2D handleReprojection(GridCoverage2D coverage, CoordinateReferenceSystem targetCRS, Interpolation spatialInterpolation, Hints hints) {
        // checks
        Utilities.ensureNonNull("interpolation", spatialInterpolation);
        // check the two crs tosee if we really need to do anything
        if(CRS.equalsIgnoreMetadata(coverage.getCoordinateReferenceSystem2D(), targetCRS)){
            return coverage;
        }

        // reproject
        final CoverageProcessor processor=hints==null?CoverageProcessor.getInstance():CoverageProcessor.getInstance(hints);
        final Operation operation = processor.getOperation("Resample");
        final ParameterValueGroup parameters = operation.getParameters();
        parameters.parameter("Source").setValue(coverage);
        parameters.parameter("CoordinateReferenceSystem").setValue(targetCRS);
        parameters.parameter("GridGeometry").setValue(null);
        parameters.parameter("InterpolationType").setValue(spatialInterpolation);
        return (GridCoverage2D) processor.doOperation(parameters);
    }

    /**
     * This method is responsible for performing the RangeSubsetting operation
     * which can be used to subset of actually remix or even duplicate bands
     * from the input source coverage.
     * 
     * <p>
     * The method tries to enforce the WCS Resource Limits specified at config time.
     * 
     * @param coverage the {@link GridCoverage2D} to work on
     * @param extensions the list of WCS extension to look for the the RangeSubset one
     * @param hints an instance of {@link Hints} to use for the operations.
     * @return a new instance of {@link GridCoverage2D} or the source one in case no operation was needed.
     * 
     */
    private GridCoverage2D handleRangeSubsettingExtension(
            GridCoverage2D coverage,
            Map<String, ExtensionItemType> extensions, 
            Hints hints) {
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
                if(!bandsNames.contains(startRangeComponent)){
                    throw new WCS20Exception("Invalid Band Name",WCS20Exception.WCS20ExceptionCode.NoSuchField,rangeComponent);
                }
                if(!bandsNames.contains(endRangeComponent)){
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
        
        // === enforce limits
        if(coverage.getNumSampleDimensions()<indexes.length){
            // ok we are enlarging the number of bands, let's check the final size
            WCSUtils.checkOutputLimits(wcs, coverage,indexes);
        }

        // create output
        return (GridCoverage2D) WCSUtils.bandSelect(coverage, indexes);
    }

    /**
     * This method is reponsible for cropping the providede {@link GridCoverage} using the provided subset envelope.
     * 
     * <p>
     * The subset envelope at this stage should be in the native crs.
     * 
     * @param coverage the source {@link GridCoverage}
     * @param subset  an instance of {@link GeneralEnvelope} that drives the crop operation.
     * @return a cropped version of the source {@link GridCoverage}
     */
    private GridCoverage2D handleSubsettingExtension(
            GridCoverage2D coverage, 
            GeneralEnvelope subset,
            Hints hints) {

        if(subset!=null){
            return WCSUtils.crop(coverage, subset); // TODO I hate this classes that do it all
        }
        return coverage;
    }

    /**
     * This method is responsible for handling the scaling WCS extension.
     * 
     * <p>
     * Scaling can be used to scale a {@link GridCoverage2D} in different ways. An user can 
     * decide to use either an uniform scale factor on each axes or by specifying a specific one 
     * on each of them.
     * Alternatively, an user can decide to specify the target size on each axes.
     * 
     * <p>
     * In case no scaling is in place but an higher order interpolation is required, scale is performed anyway
     * to respect such interpolation.
     * 
     * <p>
     * This method tries to enforce the WCS resource limits if they are set
     * 
     * @param coverage the input {@link GridCoverage2D}
     * @param spatialInterpolation the requested {@link Interpolation}
     * @param extensions the list of WCS extensions to draw info from
     * @param hints an instance of {@link Hints} to apply
     * @return a scaled version of the input {@link GridCoverage2D} according to what is specified in the list of extensions. It might be the source coverage itself if
     * no operations where to be applied.
     */
    private GridCoverage2D handleScaling(GridCoverage2D coverage, Map<String, ExtensionItemType> extensions, Interpolation spatialInterpolation, Hints hints) {
        // checks
        Utilities.ensureNonNull("interpolation", spatialInterpolation);
        
        // look for scaling extension
        if(extensions==null||extensions.size()==0||!extensions.containsKey("Scaling")){
             // NO SCALING do we need interpolation?
            if(spatialInterpolation instanceof InterpolationNearest){
                return coverage;
            } else {
                // interpolate coverage if requested and not nearest!!!!         
                final Operation operation = CoverageProcessor.getInstance().getOperation("Warp");
                final ParameterValueGroup parameters = operation.getParameters();
                parameters.parameter("Source").setValue(coverage);
                parameters.parameter("warp").setValue(new WarpAffine(AffineTransform.getScaleInstance(1, 1)));//identity
                parameters.parameter("interpolation").setValue(spatialInterpolation!=null?spatialInterpolation:InterpolationPolicy.getDefaultPolicy().getInterpolation());
                parameters.parameter( "backgroundValues").setValue(CoverageUtilities.getBackgroundValues(coverage));// TODO check and improve
                return (GridCoverage2D) CoverageProcessor.getInstance().doOperation(parameters,hints);
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
        return scalingPolicy.scale(coverage, scaling, spatialInterpolation,hints,wcs);

    }

    /**
     * This method is responsible for extracting the subsettingEvelope from the 
     * incoming request.
     * 
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
        GeneralEnvelope sourceEnvelopeInSubsettingCRS=new GeneralEnvelope(reader.getOriginalEnvelope());
        sourceEnvelopeInSubsettingCRS.setCoordinateReferenceSystem(sourceCRS);
        if(!(subsettingCRS==null||CRS.equalsIgnoreMetadata(subsettingCRS,sourceCRS))){
            
            // reproject source envelope to subsetting crs for initialization
            try {
                sourceEnvelopeInSubsettingCRS= CRS.transform(
                        CRS.findMathTransform(reader.getCrs(), subsettingCRS), 
                        reader.getOriginalEnvelope());
                sourceEnvelopeInSubsettingCRS.setCoordinateReferenceSystem(subsettingCRS);
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
            return sourceEnvelopeInSubsettingCRS;
        }
        
        // TODO remove when we handle time and elevation
        if(dimensions.size()>2){
            throw new WCS20Exception(
                    "Invalid number of dimensions", 
                    WCS20Exception.WCS20ExceptionCode.InvalidSubsetting,Integer.toString(dimensions.size()));
        }
        
        // put aside the dimensions that we have for double checking
        final List<String> axesNames = envelopeDimensionsMapper.getAxesNames(sourceEnvelopeInSubsettingCRS, true);
        final List<String> foundDimensions= new ArrayList<String>();
        
        // === parse dimensions 
        // the subsetting envelope is initialized with the source envelope in subsetting CRS
        GeneralEnvelope subsettingEnvelope = new GeneralEnvelope(sourceEnvelopeInSubsettingCRS);  
        subsettingEnvelope.setCoordinateReferenceSystem(subsettingCRS);
        for(DimensionSubsetType dim:dimensions){
            // get basic information
            String dimension=dim.getDimension(); // this is the dimension name which we compare to axes abbreviations from geotools
            
            // remove prefix
            if(dimension.startsWith("http://www.opengis.net/def/axis/OGC/0/")){
                dimension=dimension.substring("http://www.opengis.net/def/axis/OGC/0/".length());
            } else if (dimension.startsWith("http://opengis.net/def/axis/OGC/0/")){
                dimension=dimension.substring("http://opengis.net/def/axis/OGC/0/".length());
            }
            
            
            // checks
            if(dimension==null||dimension.length()<=0||!axesNames.contains(dimension)){//TODO synonyms on axes labels
                throw new WCS20Exception("Empty or wrong axis label provided",WCS20Exception.WCS20ExceptionCode.InvalidAxisLabel,dimension==null?"Null":dimension);
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
                
                // low > high???
                if(low>high){
                    throw new WCS20Exception(
                            "Low greater than High", 
                            WCS20Exception.WCS20ExceptionCode.InvalidSubsetting,
                            trim.getTrimLow());
                }

                final int axisIndex=envelopeDimensionsMapper.getAxisIndex(sourceEnvelopeInSubsettingCRS, dimension);
                if(axisIndex<0){
                    throw new WCS20Exception("Invalid axis provided",WCS20Exception.WCS20ExceptionCode.InvalidAxisLabel,dimension);
                }
                
                // notice how we choose the order of the axes
                subsettingEnvelope.setRange(axisIndex, low, high);
            } else if(dim instanceof DimensionSliceType){
                
                // SLICING
                final DimensionSliceType slicing= (DimensionSliceType) dim;
                final String slicePointS = slicing.getSlicePoint();
                final double slicePoint=Double.parseDouble(slicePointS);            
                
                final int axisIndex=envelopeDimensionsMapper.getAxisIndex(sourceEnvelopeInSubsettingCRS, dimension);
                if(axisIndex<0){
                    throw new WCS20Exception("Invalid axis provided",WCS20Exception.WCS20ExceptionCode.InvalidAxisLabel,dimension);
                }
                // notice how we choose the order of the axes
                AffineTransform affineTransform = RequestUtils.getAffineTransform(reader.getOriginalGridToWorld(PixelInCell.CELL_CENTER));
                final double scale=axisIndex==0?affineTransform.getScaleX():-affineTransform.getScaleY();
                subsettingEnvelope.setRange(axisIndex, slicePoint, slicePoint+scale);
                
                // slice point outside coverage
                if(sourceEnvelopeInSubsettingCRS.getMinimum(axisIndex)>slicePoint||slicePoint>sourceEnvelopeInSubsettingCRS.getMaximum(axisIndex)){
                    throw new WCS20Exception(
                            "SlicePoint outside coverage envelope", 
                            WCS20Exception.WCS20ExceptionCode.InvalidSubsetting,
                            slicePointS);
                }    
                
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
        GeneralEnvelope sourceEnvelope = reader.getOriginalEnvelope();

        // reproject envelope  to native crs for cropping
        try {
            if(!CRS.equalsIgnoreMetadata(subsettingEnvelope.getCoordinateReferenceSystem(), reader.getOriginalEnvelope())){
                // look for transform
                final MathTransform mathTransform = CRS.findMathTransform(subsettingCRS,sourceCRS);
                if(!mathTransform.isIdentity()){ // do we really need to reproject?
                final GeneralEnvelope subsettingEnvelopeInSourceCRS = CRS.transform(
                            mathTransform, 
                            subsettingEnvelope);
                    subsettingEnvelopeInSourceCRS.setCoordinateReferenceSystem(sourceCRS);
                    
                    // intersect
                    subsettingEnvelopeInSourceCRS.intersect(sourceEnvelope);
                    subsettingEnvelopeInSourceCRS.setCoordinateReferenceSystem(sourceCRS);          
    
                    // provided trim extent does not intersect coverage envelope
                    if(subsettingEnvelopeInSourceCRS.isEmpty()){
                        throw new WCS20Exception(
                                "Empty intersection after subsetting", 
                                WCS20Exception.WCS20ExceptionCode.InvalidSubsetting,"");// TODO spit our envelope trimmed
                    }      
                    return subsettingEnvelopeInSourceCRS;
                }     
            }    
            // we are reprojecting
            subsettingEnvelope.intersect(sourceEnvelope);
            subsettingEnvelope.setCoordinateReferenceSystem(reader.getCrs());      

            // provided trim extent does not intersect coverage envelope
            if(subsettingEnvelope.isEmpty()){
                throw new WCS20Exception(
                        "Empty intersection after subsetting", 
                        WCS20Exception.WCS20ExceptionCode.InvalidSubsetting,"");// TODO spit our envelope trimmed
            }
            return subsettingEnvelope;
        } catch (FactoryException e) {
            final WCS20Exception exception= new WCS20Exception(
                    "Unable to initialize subsetting envelope",
                    WCS20Exception.WCS20ExceptionCode.SubsettingCrsNotSupported,
                    subsettingCRS.toWKT()); // TODO extract code
            exception.initCause(e);
            throw exception;
        } catch (TransformException e) {
            final WCS20Exception exception= new WCS20Exception(
                    "Unable to initialize subsetting envelope",
                    WCS20Exception.WCS20ExceptionCode.SubsettingCrsNotSupported,
                    subsettingCRS.toWKT()); // TODO extract code
            exception.initCause(e);
            throw exception;
        } 
    }

}
