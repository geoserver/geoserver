/* Copyright (c) 2001 - 2013 OpenPlans - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms.worldwind;

import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.awt.image.DataBuffer;
import java.awt.image.RenderedImage;
import java.io.IOException;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.imageio.ImageIO;
import javax.imageio.ImageWriter;
import javax.imageio.spi.ImageWriterSpi;
import javax.imageio.stream.ImageOutputStream;
import javax.media.jai.Interpolation;
import javax.media.jai.RenderedOp;
import javax.media.jai.TiledImage;
import javax.media.jai.operator.FormatDescriptor;

import org.geoserver.data.util.CoverageUtils;
import org.geoserver.platform.ServiceException;
import org.geoserver.wms.GetMapRequest;
import org.geoserver.wms.MapLayerInfo;
import org.geoserver.wms.MapProducerCapabilities;
import org.geoserver.wms.WMS;
import org.geoserver.wms.WMSMapContent;
import org.geoserver.wms.map.RenderedImageMapResponse;
import org.geoserver.wms.worldwind.util.BilWCSUtils;
import org.geotools.coverage.CoverageFactoryFinder;
import org.geotools.coverage.grid.GeneralGridEnvelope;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.coverage.grid.GridCoverageFactory;
import org.geotools.coverage.grid.GridGeometry2D;
import org.geotools.coverage.grid.io.AbstractGridFormat;
import org.geotools.coverage.grid.io.GridCoverage2DReader;
import org.geotools.geometry.GeneralEnvelope;
import org.geotools.referencing.CRS;
import org.geotools.util.logging.Logging;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.cs.AxisDirection;
import org.opengis.referencing.operation.MathTransform;

import com.sun.media.imageioimpl.plugins.raw.RawImageWriterSpi;

/**
 * Map producer for producing Raw bil images out of an elevation model.
 * Modelled after the GeoTIFFMapResponse, relying on Geotools and the
 * RawImageWriterSpi
 * @author Tishampati Dhar
 * @since 2.0.x
 * 
 */
public final class BilMapResponse extends RenderedImageMapResponse {
	/** A logger for this class. */
	private static final Logger LOGGER = Logging.getLogger(BilMapResponse.class);

	/** the only MIME type this map producer supports */
    static final String MIME_TYPE = "image/bil";

    private static final String[] OUTPUT_FORMATS = {MIME_TYPE,"application/bil",
    	"application/bil8","application/bil16", "application/bil32" };
    
	/** GridCoverageFactory. - Where do we use this again ?*/
	private final static GridCoverageFactory factory = CoverageFactoryFinder.getGridCoverageFactory(null);

	/** Raw Image Writer **/
	private final static ImageWriterSpi writerSPI = new RawImageWriterSpi();
    
    /**
     * Constructor for a {@link BilMapResponse}.
     *
     * @param wms
     *            that is asking us to encode the image.
     */
    public BilMapResponse(final WMS wms) {
        super(OUTPUT_FORMATS,wms);
    }
    
	@Override
	public void formatImageOutputStream(RenderedImage image, OutputStream outStream,
	        WMSMapContent mapContent) throws ServiceException, IOException {
		//TODO: Write reprojected terrain tile
		// TODO Get request tile size
		final GetMapRequest request = mapContent.getRequest();
		
		String bilEncoding = (String) request.getFormat();
		
		int height = request.getHeight();
		int width = request.getWidth();
		
		if ((height>512)||(width>512)){
			throw new ServiceException("Cannot get WMS bil" +
					" tiles bigger than 512x512, try WCS");
		}
		
		List<MapLayerInfo> reqlayers = request.getLayers();
		
		//Can't fetch bil for more than 1 layer
		if (reqlayers.size() > 1) 
		{
			throw new ServiceException("Cannot combine layers into BIL output");
		}
		MapLayerInfo mapLayerInfo = reqlayers.get(0);
		
		/*
		final ParameterValueGroup writerParams = format.getWriteParameters();
        writerParams.parameter(AbstractGridFormat.GEOTOOLS_WRITE_PARAMS.getName().toString())
                    .setValue(wp);
		*/
		GridCoverage2DReader coverageReader = (GridCoverage2DReader) mapLayerInfo.getCoverageReader();

		GeneralEnvelope destinationEnvelope = null;
		try {
			destinationEnvelope = getDestinationEnvelope(request, coverageReader);
		} catch (Exception e1) {
			LOGGER.severe("Could not create destination envelope");
		}

		/*
		 * Try to use a gridcoverage style render
		 */
		GridCoverage2D subCov = null;
		try {
			if (destinationEnvelope != null) {
				subCov = getFinalCoverage(request, mapLayerInfo, coverageReader, destinationEnvelope);
			}
		} catch (Exception e) {
			LOGGER.severe("Could not get a subcoverage");
		}

		if (subCov == null) {
			LOGGER.fine("Creating coverage from a blank image");
			BufferedImage emptyImage = new BufferedImage(width, height, BufferedImage.TYPE_USHORT_GRAY);
			DataBuffer data = emptyImage.getRaster().getDataBuffer();
			for (int i = 0; i < data.getSize(); ++i) {
				data.setElem(i, 32768); // 0x0080 in file (2^15)
			}
			subCov = factory.create("uselessname", emptyImage, destinationEnvelope);
		}

		if(subCov!=null)
		{
			/*
			BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(out));
			writer.write(subCov.toString());
			writer.flush();
			writer.close();
			*/
	        image = subCov.getRenderedImage();
	        if(image!=null)
	        {
	        	int dtype = image.getData().getDataBuffer().getDataType();

	        	RenderedOp formcov = null;
	        	if((bilEncoding.equals("application/bil32"))&&(dtype!=DataBuffer.TYPE_FLOAT))	        	{
	        		formcov = FormatDescriptor.create(image,DataBuffer.TYPE_FLOAT ,null);
	        	}
	        	if((bilEncoding.equals("application/bil16"))&&(dtype!=DataBuffer.TYPE_SHORT))	        	{
	        		formcov = FormatDescriptor.create(image,DataBuffer.TYPE_SHORT ,null);
	        	}
	        	if((bilEncoding.equals("application/bil8"))&&(dtype!=DataBuffer.TYPE_BYTE))	        	{
	        		formcov = FormatDescriptor.create(image,DataBuffer.TYPE_BYTE ,null);
	        	}
	        	TiledImage tiled = null;
	        	if (formcov!= null)
	        		tiled = new TiledImage(formcov,width,height);
	        	else
	        		tiled = new TiledImage(image,width,height);
	        	final ImageOutputStream imageOutStream = ImageIO.createImageOutputStream(outStream);
		        final ImageWriter writer = writerSPI.createWriterInstance();
		        writer.setOutput(imageOutStream);
		        writer.write(tiled);
		        imageOutStream.flush();
		        imageOutStream.close();
	        }
	        else
	        {
	        	throw new ServiceException("Cannot render to BIL");
	        }
		}
		else
		{			
			throw new ServiceException("You requested a bil of size:"+
					height+"x"+width+",but you can't have it!!");

		}
	}

	/**
	 * @param request request
	 * @param coverageReader reader
	 * @return destination envelope
	 * @throws Exception an error occurred
	 */
	private static GeneralEnvelope getDestinationEnvelope(GetMapRequest request,
			GridCoverage2DReader coverageReader) throws Exception {
	    final String requestCRS = request.getSRS();
	    final CoordinateReferenceSystem sourceCRS = CRS.decode(requestCRS);

	    com.vividsolutions.jts.geom.Envelope envelope = request.getBbox();

	    final boolean lonFirst = sourceCRS.getCoordinateSystem().getAxis(0).getDirection().absolute()
	                                      .equals(AxisDirection.EAST);

	    // the envelope we are provided with is lon,lat always
	    GeneralEnvelope destinationEnvelope = !lonFirst?
	        new GeneralEnvelope(new double[] {envelope.getMinY(), envelope.getMinX()}, new double[] { envelope.getMaxY(), envelope.getMaxX()}) :
	        new GeneralEnvelope(new double[] {envelope.getMinX(), envelope.getMinY()}, new double[] { envelope.getMaxX(), envelope.getMaxY()});

	    destinationEnvelope.setCoordinateReferenceSystem(sourceCRS);
	    return destinationEnvelope;
	}

	/**
	 * getFinalCoverage - message the RenderedImage into Bil
	 *
	 * @param request CoverageRequest
	 * @param meta CoverageInfo
	 * @param coverageReader reader
	 * @param destinationEnvelope
	 * @return GridCoverage2D
	 * @throws Exception an error occurred
	 */
	private static GridCoverage2D getFinalCoverage(GetMapRequest request, MapLayerInfo meta,
	    GridCoverage2DReader coverageReader, GeneralEnvelope destinationEnvelope) throws Exception {

	    final String responseCRS = request.getSRS();
	    final CoordinateReferenceSystem targetCRS = CRS.decode(responseCRS);

	    GeneralEnvelope originalEnvelope = coverageReader.getOriginalEnvelope();
	    final CoordinateReferenceSystem cvCRS = originalEnvelope.getCoordinateReferenceSystem();

	    final String requestCRS = request.getSRS();
	    final CoordinateReferenceSystem sourceCRS = CRS.decode(requestCRS);

	    // this is the destination envelope in the coverage crs
	    // This is the CRS of the Coverage Envelope
	    final MathTransform GCCRSTodeviceCRSTransformdeviceCRSToGCCRSTransform = CRS
	        .findMathTransform(cvCRS, sourceCRS, true);
	    final MathTransform deviceCRSToGCCRSTransform = GCCRSTodeviceCRSTransformdeviceCRSToGCCRSTransform
	        .inverse();
	    final GeneralEnvelope destinationEnvelopeInSourceCRS = (!deviceCRSToGCCRSTransform
	        .isIdentity()) ? CRS.transform(deviceCRSToGCCRSTransform, destinationEnvelope)
	                       : new GeneralEnvelope(destinationEnvelope);
	    destinationEnvelopeInSourceCRS.setCoordinateReferenceSystem(cvCRS);
	    /**
	     * Reading Coverage on Requested Envelope
	    */
	    Rectangle destinationSize = null;
	    /*
	    if ((request.getGridLow() != null) && (request.getGridHigh() != null)) {
	        final int[] lowers = new int[] {
	                request.getGridLow()[0].intValue(), request.getGridLow()[1].intValue()
	            };
	        final int[] highers = new int[] {
	                request.getGridHigh()[0].intValue(), request.getGridHigh()[1].intValue()
	            };
	
	        destinationSize = new Rectangle(lowers[0], lowers[1], highers[0], highers[1]);
	    } else {
	        //destinationSize = coverageReader.getOriginalGridRange().toRectangle();
	        throw new WmsException("Neither Grid Size nor Grid Resolution have been specified.");
	    }
		*/
	    destinationSize = new Rectangle(0,0,request.getHeight(),request.getWidth());
	    /**
	     * Checking for supported Interpolation Methods
	     */
	    
	    /*
	    Interpolation interpolation = Interpolation.getInstance(Interpolation.INTERP_NEAREST);
	    final String interpolationType = request.getInterpolation();
	
	    if (interpolationType != null) {
	        boolean interpolationSupported = false;
	        Iterator internal = meta.getInterpolationMethods().iterator();
	
	        while (internal.hasNext()) {
	            if (interpolationType.equalsIgnoreCase((String) internal.next())) {
	                interpolationSupported = true;
	            }
	        }
	
	        if (!interpolationSupported) {
	            throw new WcsException(
	                "The requested Interpolation method is not supported by this Coverage.");
	        } else {
	            if (interpolationType.equalsIgnoreCase("bilinear")) {
	                interpolation = Interpolation.getInstance(Interpolation.INTERP_BILINEAR);
	            } else if (interpolationType.equalsIgnoreCase("bicubic")) {
	                interpolation = Interpolation.getInstance(Interpolation.INTERP_BICUBIC);
	            }
	        }
	    }
		*/
	    Interpolation interpolation = Interpolation.getInstance(Interpolation.INTERP_BILINEAR);
	    
	    Map<Object,Object> parameters = new HashMap<Object,Object>();
	    // /////////////////////////////////////////////////////////
	    //
	    // Reading the coverage
	    //
	    // /////////////////////////////////////////////////////////
	    parameters.put(AbstractGridFormat.READ_GRIDGEOMETRY2D.getName().toString(),
	        new GridGeometry2D(new GeneralGridEnvelope(destinationSize), destinationEnvelopeInSourceCRS));
	
	    final GridCoverage2D coverage = coverageReader.read(CoverageUtils.getParameters(
	                coverageReader.getFormat().getReadParameters(), parameters, true));
	
	    if (coverage == null) {
	    	LOGGER.log(Level.FINE, "Failed to read coverage - continuing");
	    	return null;
	    }
	
	    /**
	     * Band Select
	     */
	    /*
	    Coverage bandSelectedCoverage = null;
	
	    bandSelectedCoverage = WCSUtils.bandSelect(request.getParameters(), coverage);
		*/
	    /**
	     * Crop
	     */
	    final GridCoverage2D croppedGridCoverage = BilWCSUtils.crop(coverage,
	            (GeneralEnvelope) coverage.getEnvelope(), cvCRS, destinationEnvelopeInSourceCRS,
	            Boolean.TRUE);
	    
	    /**
	     * Scale/Resampling (if necessary)
	     */
	    //GridCoverage2D subCoverage = null;
	    GridCoverage2D subCoverage = croppedGridCoverage;
	    final GeneralGridEnvelope newGridrange = new GeneralGridEnvelope(destinationSize);
	
	    /*if (!newGridrange.equals(croppedGridCoverage.getGridGeometry()
	                    .getGridRange())) {*/
	    subCoverage = BilWCSUtils.scale(croppedGridCoverage, newGridrange, croppedGridCoverage, cvCRS,
	            destinationEnvelopeInSourceCRS);
	    //}
	
	    /**
	     * Reproject
	     */
	    subCoverage = BilWCSUtils.reproject(subCoverage, sourceCRS, targetCRS, interpolation);
	    
	    return subCoverage;
	}

	/**
	 * This is not really an image map
	 */
	@Override
	public MapProducerCapabilities getCapabilities(String outputFormat) {
		// FIXME become more capable
		return new MapProducerCapabilities(false, false, false, false, null);
	}
}
