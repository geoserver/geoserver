/* Copyright (c) 2001 - 2013 OpenPlans - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wcs2_0;

import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.media.jai.BorderExtender;
import javax.media.jai.Interpolation;
import javax.media.jai.InterpolationNearest;
import javax.media.jai.JAI;
import javax.media.jai.WarpAffine;

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
import net.opengis.wcs20.ScalingType;

import org.eclipse.emf.common.util.EList;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CoverageInfo;
import org.geoserver.catalog.DimensionInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.catalog.util.ReaderDimensionsAccessor;
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
import org.geotools.factory.GeoTools;
import org.geotools.factory.Hints;
import org.geotools.geometry.GeneralEnvelope;
import org.geotools.referencing.CRS;
import org.geotools.referencing.ReferencingFactoryFinder;
import org.geotools.referencing.operation.transform.AffineTransform2D;
import org.geotools.resources.coverage.CoverageUtilities;
import org.geotools.util.DateRange;
import org.geotools.util.DefaultProgressListener;
import org.geotools.util.Utilities;
import org.geotools.util.logging.Logging;
import org.geotools.xml.impl.DatatypeConverterImpl;
import org.opengis.coverage.SampleDimension;
import org.opengis.coverage.grid.GridCoverage;
import org.opengis.coverage.processing.Operation;
import org.opengis.geometry.Envelope;
import org.opengis.parameter.GeneralParameterDescriptor;
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
    
    private WCSInfo wcs;
    
    private Catalog catalog;
    
    /** Utility class to map envelope dimension*/
    private EnvelopeAxesLabelsMapper envelopeDimensionsMapper;
    
    /** A URI authorithy with lonlat order.*/
    private CRSAuthorityFactory lonLatCRSFactory;
    
    /** A URI authorithy with latlon order.*/
    private CRSAuthorityFactory latLonCRSFactory;
    
    public final static String SRS_STARTER="http://www.opengis.net/def/crs/EPSG/0/";
    
    public static final Set<String> TIME_NAMES = new HashSet<String>();
    
    static {
        TIME_NAMES.add("t");
        TIME_NAMES.add("time");
        TIME_NAMES.add("temporal");
        TIME_NAMES.add("phenomenontime");
    }

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
            
            GridCoverageRequest gcr = parseGridCoverageRequest(cinfo, reader, request, extensions);
            
            //
            // we setup the params to force the usage of imageread and to make it use
            // the right overview and so on
            // we really try to subset before reading with a grid geometry
            // we specify to work in streaming fashion
            // TODO elevation
            coverage = readCoverage(cinfo, gcr, reader, hints);
            if(coverage == null) {
                throw new IllegalStateException("Unable to read a coverage for the current request" + request.toString());
            }
            
            //
            // handle range subsetting
            //        
            coverage=handleRangeSubsettingExtension(coverage,extensions,hints);
          
            
            //
            // subsetting, is not really an extension
            //
            coverage=handleSubsettingExtension(coverage,gcr.getSpatialSubset(),hints);
            
            //
            // scaling extension
            //
            // scaling is done in raster space with eventual interpolation
            coverage=handleScaling(coverage,extensions,gcr.getSpatialInterpolation(),hints);
            
            
            //
            // reprojection
            //
            // reproject the output coverage to an eventual outputCrs
            coverage=handleReprojection(coverage,gcr.getOutputCRS(),gcr.getSpatialInterpolation(),hints);

            //
            // axes swap management
            //
            final boolean enforceLatLonAxesOrder=requestingLatLonAxesOrder(gcr.getOutputCRS());
            if(enforceLatLonAxesOrder){
                coverage = enforceLatLongOrder(coverage, hints, gcr.getOutputCRS());
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

    private GridCoverageRequest parseGridCoverageRequest(CoverageInfo ci, AbstractGridCoverage2DReader reader,
            GetCoverageType request, Map<String, ExtensionItemType> extensions) {
        //
        // Extract CRS values for relative extension
        //
        final CoordinateReferenceSystem subsettingCRS = extractSubsettingCRS(reader, extensions);
        final CoordinateReferenceSystem outputCRS = extractOutputCRS(reader, extensions,
                subsettingCRS);
        
        // is this a temporal coverage
        DimensionInfo timeDimension = ci.getMetadata().get(ResourceInfo.TIME, DimensionInfo.class);
        boolean hasTime = timeDimension != null;

        // extract spatial subsetting
        final GeneralEnvelope subset = extractSubsettingEnvelope(reader, request, subsettingCRS, hasTime);
        assert subset != null && !subset.isEmpty();
        
        // extract temporal subsetting
        DateRange timeSubset = null;
        if(hasTime) {
            timeSubset = extractTimeSubset(reader, request, timeDimension);
            if(timeSubset == null) {
                // use "current" as the default
                ReaderDimensionsAccessor accessor = new ReaderDimensionsAccessor(reader);
                Date maxTime = accessor.getMaxTime();
                if(maxTime != null) {
                    timeSubset = new DateRange(maxTime, maxTime);
                }
            }
        }

        //
        // Handle interpolation extension
        //
        // notice that for the moment we support only homogeneous interpolation on the 2D axis
        final Map<String, InterpolationPolicy> axesInterpolations = extractInterpolation(reader,
                extensions);
        final Interpolation spatialInterpolation = extractSpatialInterpolation(axesInterpolations,
                reader.getOriginalEnvelope());
        // TODO time interpolation
        assert spatialInterpolation != null;

        // build the grid coverage request
        GridCoverageRequest gcr = new GridCoverageRequest();
        gcr.setOutputCRS(outputCRS);
        gcr.setSpatialInterpolation(spatialInterpolation);
        gcr.setSpatialSubset(subset);
        gcr.setTemporalSubset(timeSubset);

        return gcr;
    }

    /**
     * Parses a date range out of the dimension subsetting directives
     * @param reader
     * @param request
     * @param timeDimension
     * @return
     */
    private DateRange extractTimeSubset(AbstractGridCoverage2DReader reader,
            GetCoverageType request, DimensionInfo timeDimension) {
        DatatypeConverterImpl xmlTimeConverter = DatatypeConverterImpl.getInstance();
        DateRange range = null;
        for (DimensionSubsetType dim : request.getDimensionSubset()) {
            String dimension = getDimensionName(dim);
            
            // only care for time dimensions
            if (!TIME_NAMES.contains(dimension.toLowerCase())) {
                continue;
            }
            
            // did we parse the range already?
            if(range != null) {
                throw new WCS20Exception("Time dimension trimming/slicing specified twice in the request",
                        WCS20Exception.WCS20ExceptionCode.InvalidSubsetting, "subset");
            }

            // now decide what to do
            if (dim instanceof DimensionTrimType) {

                // TRIMMING
                final DimensionTrimType trim = (DimensionTrimType) dim;
                final Date low = xmlTimeConverter.parseDateTime(trim.getTrimLow()).getTime();
                final Date high = xmlTimeConverter.parseDateTime(trim.getTrimHigh()).getTime();

                // low > high???
                if (low.compareTo(high) > 0) {
                    throw new WCS20Exception("Low greater than High: " + trim.getTrimLow() 
                            + ", " + trim.getTrimHigh(),
                            WCS20Exception.WCS20ExceptionCode.InvalidSubsetting, "subset");
                }

                range = new DateRange(low, high);
            } else if (dim instanceof DimensionSliceType) {

                // SLICING
                final DimensionSliceType slicing = (DimensionSliceType) dim;
                final String slicePointS = slicing.getSlicePoint();
                final Date slicePoint = xmlTimeConverter.parseDateTime(slicePointS).getTime();

                range = new DateRange(slicePoint, slicePoint);
            } else {
                throw new WCS20Exception(
                        "Invalid element found while attempting to parse dimension subsetting request: " + dim.getClass()
                        .toString(), WCS20Exception.WCS20ExceptionCode.InvalidSubsetting, "subset");
            }
        }
        
        // right now we don't support trimming
        // TODO: revisit when we have some multidimensional output support
        if(range != null && !range.getMinValue().equals(range.getMaxValue())) {
            throw new WCS20Exception("Trimming on time is not supported at the moment, only slicing is");
        }
        
        // apply nearest neighbor matching on time
        if(range != null) {
            ReaderDimensionsAccessor accessor = new ReaderDimensionsAccessor(reader);
            TreeSet<Object> domain = accessor.getTimeDomain();
            Date slicePoint = range.getMinValue();
            if(!domainContains(slicePoint, domain)) {
                    // look for the closest time
                    Date previous = null;
                    Date newSlicePoint = null;
                    // for NN matching we don't need the ranges, NN against their extrema will be fine
                    TreeSet<Date> domainDates = getDomainDates(domain);
                    for (Date curr : domainDates) {
                        if(curr.compareTo(slicePoint) > 0) {
                            if(previous == null) {
                                newSlicePoint = curr;
                                break;
                            } else {
                                long diffPrevious = slicePoint.getTime() - previous.getTime();
                                long diffCurr = curr.getTime() - slicePoint.getTime();
                                if(diffCurr > diffPrevious) {
                                    newSlicePoint = curr;
                                    break;
                                } else {
                                    newSlicePoint = previous;
                                    break;
                                }
                            }
                        } else {
                            previous = curr;
                        }
                    }
                    
                    if(newSlicePoint == null) {
                        newSlicePoint = previous;
                    }
                    range = new DateRange(newSlicePoint, newSlicePoint);
                }
        }
        
        return range;
    }

    private TreeSet<Date> getDomainDates(TreeSet<Object> domain) {
        TreeSet<Date> results = new TreeSet<Date>();
        for (Object item : domain) {
            if(item instanceof Date) {
                Date date = (Date) item;
                results.add(date);
            } else if(item instanceof DateRange) {
                DateRange range = (DateRange) item;
                results.add(range.getMinValue());
                results.add(range.getMaxValue());
            }
        }
        
        return results;
    }

    private boolean domainContains(Date slicePoint, TreeSet<Object> domain) {
        // cannot use this...
        //        if(domain.contains(slicePoint)) {
        //            return true;
        //        }
        
        // check date ranges for containment
        for (Object curr : domain) {
            if(curr instanceof Date) {
                Date date = (Date) curr;
                int result = date.compareTo(slicePoint);
                if(result > 0) {
                    return false;
                } else if(result == 0) {
                    return true;
                }
            } else if(curr instanceof DateRange) {
                DateRange range = (DateRange) curr;
                if(range.contains(slicePoint)) {
                    return true;
                } else if(range.getMaxValue().compareTo(slicePoint) < 0) {
                    return false;
                }
            }
        }
        
        return false;
    }

    /**
     * Extracts the simplified dimension name, throws exception if the dimension name is empty
     * @param dim
     * @return
     */
    private String getDimensionName(DimensionSubsetType dim) {
        // get basic information
        String dimension = dim.getDimension(); 

        // remove common prefixes
        if (dimension.startsWith("http://www.opengis.net/def/axis/OGC/0/")) {
            dimension = dimension.substring("http://www.opengis.net/def/axis/OGC/0/".length());
        } else if (dimension.startsWith("http://opengis.net/def/axis/OGC/0/")) {
            dimension = dimension.substring("http://opengis.net/def/axis/OGC/0/".length());
        } else if (dimension.startsWith("http://opengis.net/def/crs/ISO/2004/")) {
            dimension = dimension.substring("http://opengis.net/def/crs/ISO/2004/".length());
        }

        // checks 
        // TODO synonyms on axes labels
        if (dimension == null || dimension.length() <= 0) { 
            throw new WCS20Exception("Empty/invalid axis label provided: " + dim.getDimension(),
                    WCS20Exception.WCS20ExceptionCode.InvalidAxisLabel, "subset");
        }
        return dimension;
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
            GridCoverageRequest request, 
            AbstractGridCoverage2DReader reader, 
            Hints hints) throws Exception {
        // checks
        Interpolation spatialInterpolation = request.getSpatialInterpolation();
        Utilities.ensureNonNull("interpolation", spatialInterpolation);

        
        //
        // check if we need to reproject the subset envelope back to coverageCRS
        //
        // this does not mean we need to reproject the coverage at the end
        // as the outputCrs can be different from the subsetCrs
        //
        // get source crs
        final CoordinateReferenceSystem coverageCRS = reader.getCrs();
        GeneralEnvelope subset = request.getSpatialSubset();
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
        CoordinateReferenceSystem outputCRS = request.getOutputCRS();
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
        
        // handle "time"
        if(request.getTemporalSubset() != null) {
            List<GeneralParameterDescriptor> descriptors = readParametersDescriptor.getDescriptor().descriptors();
            List<Object> times = new ArrayList<Object>();
            times.add(request.getTemporalSubset());
            readParameters = CoverageUtils.mergeParameter(descriptors, readParameters, times, "TIME", "Time");
        }
        

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
            CoordinateReferenceSystem subsettingCRS,
            boolean hasTime) {
        
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
        
        // TODO update when we have elevation too
        int maxDimensions = 2 + (hasTime ? 1 : 0);
        if(dimensions.size() > maxDimensions){
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
            String dimension = getDimensionName(dim);
            // skip time support
            if(TIME_NAMES.contains(dimension.toLowerCase())) {
                if(hasTime) {
                    // fine, we'll parse it later
                    continue;
                } else {
                    throw new WCS20Exception("Invalid axis label provided: " + dimension,
                            WCS20Exception.WCS20ExceptionCode.InvalidAxisLabel, null);
                }
            }
            if(!axesNames.contains(dimension)) {
                throw new WCS20Exception("Invalid axis label provided: " + dimension,
                        WCS20Exception.WCS20ExceptionCode.InvalidAxisLabel,
                        dimension == null ? "Null" : dimension);

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
