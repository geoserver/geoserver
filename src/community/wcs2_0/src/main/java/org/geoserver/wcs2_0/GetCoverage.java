package org.geoserver.wcs2_0;

import java.awt.Rectangle;
import java.awt.geom.AffineTransform;
import java.io.IOException;

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

import org.eclipse.emf.common.util.EList;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CoverageInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.wcs.CoverageCleanerCallback;
import org.geoserver.wcs.WCSInfo;
import org.geoserver.wcs2_0.exception.WCS20Exception;
import org.geoserver.wcs2_0.util.EnvelopeDimensionsMapper;
import org.geoserver.wcs2_0.util.NCNameResourceCodec;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.coverage.grid.GridEnvelope2D;
import org.geotools.coverage.processing.CoverageProcessor;
import org.geotools.coverage.processing.Operations;
import org.geotools.factory.Hints;
import org.geotools.geometry.GeneralEnvelope;
import org.jaitools.imageutils.ImageLayout2;
import org.opengis.coverage.grid.GridCoverage;
import org.opengis.coverage.grid.GridCoverageReader;
import org.opengis.coverage.processing.Operation;
import org.opengis.parameter.ParameterValueGroup;
import org.vfny.geoserver.util.WCSUtils;
import org.vfny.geoserver.wcs.WcsException;
import org.vfny.geoserver.wcs.WcsException.WcsExceptionCode;

/**
 * Implementation of the WCS 2.0 GetCoverage request
 * 
 * @author Andrea Aime - GeoSolutions
 * @author Simone Giannecchini, GeoSolutions
 */
public class GetCoverage {
    
    private enum ScalingPolicy{
        DoNothing{

            @Override
            public GridCoverage2D scale(GridCoverage2D sourceGC, ExtensionItemType extensionItem) {
                return sourceGC;
            }
            
        },
        ScaleByFactor{

            @Override
            public GridCoverage2D scale(GridCoverage2D sourceGC, ExtensionItemType extensionItem) {
                // get scale factor
                final String scaleFactorS=extensionItem.getSimpleContent();
                float scaleFactor=Float.NaN;
                try{
                    scaleFactor=Float.parseFloat(scaleFactorS);
                    if(scaleFactor<0){
                        throw new WCS20Exception("Invalid scale factor", WCS20Exception.WCSExceptionCode.InvalidScaleFactor, String.valueOf(scaleFactor));
                    }
                } catch (Exception e) {
                    throw new WCS20Exception("Invalid scale factor", WCS20Exception.WCSExceptionCode.InvalidScaleFactor, scaleFactorS);
                }
                
                // return coverage unchanged if we don't scale
                if(scaleFactor==1){
                    return sourceGC;
                }
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
            public GridCoverage2D scale(GridCoverage2D sourceGC, ExtensionItemType extensionItem) {
                // get scale factor
                final String sizeXS=extensionItem.getSimpleContent();
                final String sizeYS=extensionItem.getSimpleContent();
                int sizeX=Integer.MIN_VALUE;
                try{
                    sizeX=Integer.parseInt(sizeXS);
                    if(sizeX<=0){
                        throw new WCS20Exception("Invalid target size", WCS20Exception.WCSExceptionCode.InvalidScaleFactor, sizeXS);
                    }
                } catch (Exception e) {
                    throw new WCS20Exception("Invalid target size", WCS20Exception.WCSExceptionCode.InvalidScaleFactor, sizeXS);
                }
                int sizeY=Integer.MIN_VALUE;
                try{
                    sizeY=Integer.parseInt(sizeYS);
                    if(sizeX<=0){
                        throw new WCS20Exception("Invalid target size", WCS20Exception.WCSExceptionCode.InvalidScaleFactor, sizeYS);
                    }
                } catch (Exception e) {
                    throw new WCS20Exception("Invalid target size", WCS20Exception.WCSExceptionCode.InvalidScaleFactor, sizeYS);
                }
                
                // unscale
                final GridEnvelope2D sourceGE=sourceGC.getGridGeometry().getGridRange2D();
                if(sizeY==sourceGE.width&& sizeX==sourceGE.height){
                    return sourceGC;
                }
                
                // create final warp
                final Warp warp= new  WarpAffine(AffineTransform.getScaleInstance(sourceGE.width/sizeX, sourceGE.height/sizeY));// TODO check
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
//                setParameterValue(parameters, "backgroundValues", argumentValue3);// TODO
                return (GridCoverage2D) CoverageProcessor.getInstance().doOperation(parameters,hints);
            }
        },
        ScaleToExtent{

            @Override
            public GridCoverage2D scale(GridCoverage2D sourceGC, ExtensionItemType extensionItem) {
                // get scale factor
                final String sizeXS=extensionItem.getSimpleContent();
                final String sizeYS=extensionItem.getSimpleContent();
                int sizeX=Integer.MIN_VALUE;
                try{
                    sizeX=Integer.parseInt(sizeXS);
                    if(sizeX<=0){
                        throw new WCS20Exception("Invalid target size", WCS20Exception.WCSExceptionCode.InvalidScaleFactor, sizeXS);
                    }
                } catch (Exception e) {
                    throw new WCS20Exception("Invalid target size", WCS20Exception.WCSExceptionCode.InvalidScaleFactor, sizeXS);
                }
                int sizeY=Integer.MIN_VALUE;
                try{
                    sizeY=Integer.parseInt(sizeYS);
                    if(sizeX<=0){
                        throw new WCS20Exception("Invalid target size", WCS20Exception.WCSExceptionCode.InvalidScaleFactor, sizeYS);
                    }
                } catch (Exception e) {
                    throw new WCS20Exception("Invalid target size", WCS20Exception.WCSExceptionCode.InvalidScaleFactor, sizeYS);
                }
                

                final GridEnvelope2D sourceGE=sourceGC.getGridGeometry().getGridRange2D();
                final int minx=0;
                final int miny=0;
                final int maxx=0;
                final int maxy=0;
                final Rectangle destinationRectangle= new Rectangle(minx, miny,maxx-minx+1, maxy-miny+1);
                // UNSCALE
                if(destinationRectangle.equals(sourceGE)){
                    return sourceGC;
                }
                
                // create final warp
                final Warp warp= new  WarpAffine(AffineTransform.getScaleInstance(sourceGE.width/sizeX, sourceGE.height/sizeY)); // TODO check
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
//                setParameterValue(parameters, "backgroundValues", argumentValue3);// TODO
                return (GridCoverage2D) CoverageProcessor.getInstance().doOperation(parameters,hints);
            }
            
        },        
        ScaleAxesByFactor{

            @Override
            public GridCoverage2D scale(GridCoverage2D sourceGC, ExtensionItemType extensionItem) {
                // TODO dimension management
                
                // get scale factor
                final String scaleFactorSX=extensionItem.getSimpleContent();
                final String scaleFactorSY=extensionItem.getSimpleContent();
                float scaleFactorX=Float.NaN;
                try{
                    scaleFactorX=Float.parseFloat(scaleFactorSX);
                    if(scaleFactorX<0){
                        throw new WCS20Exception("Invalid scale factor", WCS20Exception.WCSExceptionCode.InvalidScaleFactor, scaleFactorSX);
                    }
                } catch (Exception e) {
                    throw new WCS20Exception("Invalid scale factor", WCS20Exception.WCSExceptionCode.InvalidScaleFactor, scaleFactorSX);
                }
                float scaleFactorY=Float.NaN;
                try{
                    scaleFactorY=Float.parseFloat(scaleFactorSX);
                    if(scaleFactorY<0){
                        throw new WCS20Exception("Invalid scale factor", WCS20Exception.WCSExceptionCode.InvalidScaleFactor, scaleFactorSY);
                    }
                } catch (Exception e) {
                    throw new WCS20Exception("Invalid scale factor", WCS20Exception.WCSExceptionCode.InvalidScaleFactor, scaleFactorSY);
                }         
                
                // unscale
                if(scaleFactorX==1.0&& scaleFactorY==1.0){
                    return sourceGC;
                }
                
                return (GridCoverage2D) Operations.DEFAULT.scale(sourceGC, scaleFactorX, scaleFactorY, 0, 0);
            }
            
        };
        /**
         * @param extensionItem 
         * @param sourceGG
         * @param width 
         * @param origin 
         * @param returnValue
         */
        abstract public GridCoverage2D scale(GridCoverage2D sourceGC, ExtensionItemType extensionItem) ;
    };
    

    private WCSInfo wcs;
    private Catalog catalog;
    
    /** Utility class to map envelope dimension*/
    private EnvelopeDimensionsMapper envelopeDimensionsMapper;

    public GetCoverage(WCSInfo serviceInfo, Catalog catalog, EnvelopeDimensionsMapper envelopeDimensionsMapper) {
        this.wcs = serviceInfo;
        this.catalog = catalog;
        this.envelopeDimensionsMapper=envelopeDimensionsMapper;
    }

    public GridCoverage run(GetCoverageType request) {
        // get the coverage 
        LayerInfo linfo = NCNameResourceCodec.getCoverage(catalog, request.getCoverageId());
        if(linfo == null) {
            throw new WCS20Exception("Could not locate coverage " + request.getCoverageId(), 
                    WCS20Exception.WCSExceptionCode.NoSuchCoverage, "coverageId");
        } 
        
        // TODO: handle trimming and slicing
        
        CoverageInfo cinfo = (CoverageInfo) linfo.getResource();
        GridCoverage2D coverage = null;
        try {
            
            GridCoverageReader reader = cinfo.getGridCoverageReader(null, null);
            
            // use this to check if we can use overviews or not
            boolean subsample = wcs.isSubsamplingEnabled();
            
            // TODO: setup the params to force the usage of imageread and to make it use
            // the right overview and so on
            // TODO here we should really try to subset before reading with a grid geometry
            // TODO here we should specify to work in streaming fashion
            coverage = (GridCoverage2D) reader.read(null);
            
            // TODO: handle crop, scale, reproject and so on
            
            // handle range subsetting
            coverage=handleRangeSubsettingExtension(coverage,request);
            
            // scaling subsetting
            coverage=handleSubsettingExtension(coverage,request);
//            
            // scaling subsetting
            coverage=handleScaling(coverage,request);
            
            // scaling subsetting
            coverage=handleReprojection(coverage,request);
            
            
            // reproject
        } catch(IOException e) {
            throw new WcsException("Failed to read the coverage " + request.getCoverageId(), e);
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
     * @param request
     * @return
     */
    private GridCoverage2D handleReprojection(GridCoverage2D coverage, GetCoverageType request) {
        // look for scaling extension
        final ExtensionType extension = request.getExtension();
        if(extension==null){
             // NO SCALING
            return coverage;
        }
        
        // look for a scaling extension
        final EList<ExtensionItemType> extensions = extension.getContents();
        for(ExtensionItemType extensionItem:extensions){
            try{
                final ScalingPolicy scaling=  ScalingPolicy.valueOf(extensionItem.getName());// TODO extract scaling behavior
                return scaling.scale(coverage, extensionItem);
            } catch (Exception e) {
                // eat me
            }
        }
        // no scaling
        return coverage;
    }

    /**
     * @param coverage
     * @param request
     * @return
     */
    private GridCoverage2D handleRangeSubsettingExtension(GridCoverage2D coverage,
            GetCoverageType request) {
        return coverage;
    }

    /**
     * @param coverage
     * @param request
     * @return
     */
    private GridCoverage2D handleSubsettingExtension(GridCoverage2D coverage, GetCoverageType request) {

        // extract subsetting
        final GeneralEnvelope subset=extractSubsettingEnvelope(coverage,request);
        if(subset!=null){
            return WCSUtils.crop(coverage, subset); // TODO I hate this classes that do it all
        }
        return coverage;
    }

    /**
     * @param coverage
     * @param request
     * @return
     */
    private GridCoverage2D handleScaling(GridCoverage2D coverage, GetCoverageType request) {

        // look for scaling extension
        final ExtensionType extension = request.getExtension();
        if(extension==null){
             // NO SCALING
            return coverage;
        }
        
        // look for a scaling extension
        final EList<ExtensionItemType> extensions = extension.getContents();
        for(ExtensionItemType extensionItem:extensions){
            try{
                final ScalingPolicy scaling=  ScalingPolicy.valueOf(extensionItem.getName());// TODO extract scaling behavior
                return scaling.scale(coverage, extensionItem);
            } catch (Exception e) {
                // eat me
            }
        }
        // no scaling
        return coverage;
    }

    /**
     * @param coverage
     * @param request
     * @return
     */
    private GeneralEnvelope extractSubsettingEnvelope(GridCoverage coverage, GetCoverageType request) {
        //default
        GeneralEnvelope envelope=new GeneralEnvelope(coverage.getEnvelope());
        
        // check what we need
        final EList<DimensionSubsetType> dimensions = request.getDimensionSubset();
        if(dimensions==null||dimensions.size()<=0){
            return null;
        }
        if(dimensions.size()>2){
            
        }
        // parse dimensions
        for(DimensionSubsetType dim:dimensions){
            // get basic information
            final String dimension=dim.getDimension(); // this is the dimension name which we compare to axes abbreviations from geotools
            final String CRS= dim.getCRS();// TODO HOW DO WE USE THIS???
            if(dim instanceof DimensionTrimType){
                
                // TRIMMING
                final DimensionTrimType trim = (DimensionTrimType) dim;
                final double low = Double.parseDouble(trim.getTrimLow());
                final double high = Double.parseDouble(trim.getTrimHigh());

                // notice how we choose the order of the axes
                envelope.setRange(envelopeDimensionsMapper.getAxisIndex(envelope, dimension), low, high);
            } else if(dim instanceof DimensionSliceType){
                
                // SLICING
                final DimensionSliceType slicing= (DimensionSliceType) dim;
                final String slicePointS = slicing.getSlicePoint();
                final double slicePoint=Double.parseDouble(slicePointS);
                
                // notice how we choose the order of the axes
                envelope.setRange(envelopeDimensionsMapper.getAxisIndex(envelope, dimension), slicePoint, slicePoint);
                
            } else {
                throw new WcsException(
                        "Invalid element found while attempting to parse dimension subsetting request", 
                        WcsExceptionCode.InvalidSubsetting,
                        dim.getClass().toString());
            }
        }
        
        return envelope;
    }

}
