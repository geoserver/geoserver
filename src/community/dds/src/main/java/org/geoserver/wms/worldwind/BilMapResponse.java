/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms.worldwind;

import java.awt.Rectangle;
import java.awt.image.DataBuffer;
import java.awt.image.RenderedImage;
import java.awt.image.renderable.ParameterBlock;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteOrder;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import javax.imageio.ImageIO;
import javax.imageio.ImageWriter;
import javax.imageio.spi.ImageWriterSpi;
import javax.imageio.stream.ImageOutputStream;
import javax.media.jai.Interpolation;
import javax.media.jai.JAI;
import javax.media.jai.TiledImage;
import javax.media.jai.operator.FormatDescriptor;

import org.geoserver.catalog.MetadataMap;
import org.geoserver.data.util.CoverageUtils;
import org.geoserver.platform.ServiceException;
import org.geoserver.wms.GetMapRequest;
import org.geoserver.wms.MapLayerInfo;
import org.geoserver.wms.MapProducerCapabilities;
import org.geoserver.wms.WMS;
import org.geoserver.wms.WMSMapContent;
import org.geoserver.wms.map.RenderedImageMapResponse;
import org.geoserver.wms.worldwind.util.BilWCSUtils;
import org.geoserver.wms.worldwind.util.RecodeRaster;
import org.geotools.coverage.CoverageFactoryFinder;
import org.geotools.coverage.grid.GeneralGridEnvelope;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.coverage.grid.GridCoverageFactory;
import org.geotools.coverage.grid.GridGeometry2D;
import org.geotools.coverage.grid.io.AbstractGridFormat;
import org.geotools.coverage.grid.io.GridCoverage2DReader;
import org.geotools.geometry.GeneralEnvelope;
import org.geotools.referencing.CRS;
import org.geotools.resources.coverage.CoverageUtilities;
import org.geotools.util.logging.Logging;
import org.opengis.coverage.grid.GridCoverage;
import org.opengis.coverage.grid.GridCoverageReader;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.TransformException;
import org.vfny.geoserver.wcs.WcsException;

import com.sun.media.imageioimpl.plugins.raw.RawImageWriterSpi;

/**
 * Map producer for producing Raw bil images out of an elevation model.
 * Modeled after the GeoTIFFMapResponse, relying on Geotools and the
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
    
    private WMS wmsConfig;

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
        this.wmsConfig = wms;
    }

	@Override
	public void formatImageOutputStream(RenderedImage image, OutputStream outStream,
	        WMSMapContent mapContent) throws ServiceException, IOException {
		//TODO: Write reprojected terrain tile
		// TODO Get request tile size
		GetMapRequest request = mapContent.getRequest();

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
		
		// Get BIL layer configuration. This configuration is set by the server administrator
		// using the BIL layer config panel.
		MapLayerInfo mapLayerInfo = reqlayers.get(0);
		MetadataMap metadata = mapLayerInfo.getResource().getMetadata();

		String defaultDataType = (String) metadata.get(BilConfig.DEFAULT_DATA_TYPE);
		String byteOrder = (String) metadata.get(BilConfig.BYTE_ORDER);

		Double outNoData = null;
		Object noDataParam = metadata.get(BilConfig.NO_DATA_OUTPUT);
		if (noDataParam instanceof Number)
		{
		    outNoData = ((Number) noDataParam).doubleValue();
		} else if (noDataParam instanceof String)
		{
    		try
    		{
    			outNoData = Double.parseDouble((String) noDataParam);
    		} catch (NumberFormatException e)
    		{
    			LOGGER.warning("Can't parse output no data attribute: " + e.getMessage()); // TODO localize
    		}
		}


		/*
		final ParameterValueGroup writerParams = format.getWriteParameters();
        writerParams.parameter(AbstractGridFormat.GEOTOOLS_WRITE_PARAMS.getName().toString())
                    .setValue(wp);
		*/
		GridCoverageReader coverageReader = mapLayerInfo.getCoverageReader();

		/*
		 * Try to use a gridcoverage style render
		 */
		GridCoverage2D subCov = null;
		try {
			subCov = getFinalCoverage(request,
					mapLayerInfo, mapContent, (GridCoverage2DReader)coverageReader);
		} catch (IndexOutOfBoundsException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (FactoryException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (TransformException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
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
	        	/* Throw exception if required to perform conversion */
	        	/*
	        	if((bilEncoding.equals("application/bil32"))&&(dtype!=DataBuffer.TYPE_FLOAT))	        	{
	        		throw new ServiceException("Cannot fetch BIL float data,"+
	        				"Wrong underlying data type");
	        	}
	        	if((bilEncoding.equals("application/bil16"))&&(dtype!=DataBuffer.TYPE_SHORT))	        	{
	        		throw new ServiceException("Cannot fetch BIL int data,"+
	        				"Wrong underlying data type");
	        	}
	        	if((bilEncoding.equals("application/bil8"))&&(dtype!=DataBuffer.TYPE_BYTE))	        	{
	        		throw new ServiceException("Cannot fetch BIL byte data,"+
	        				"Wrong underlying data type");
	        	}
	        	*/

	        	RenderedImage transformedImage = image;

                // Perform NoData translation
	        	final double[] inNoDataValues = CoverageUtilities.getBackgroundValues(
	        			(GridCoverage2D) subCov);
	        	if (inNoDataValues != null && outNoData != null)
	        	{
	        		// TODO should support multiple no-data values
	        		final double inNoData = inNoDataValues[0];

	        		if (inNoData != outNoData)
	        		{
	        			ParameterBlock param = new ParameterBlock().addSource(image);
	        			param = param.add(inNoData);
	        			param = param.add(outNoData);
	        			transformedImage = JAI.create(RecodeRaster.OPERATION_NAME, param, null);
	        			}
	        		}

	        	// Perform format conversion. If the requested encoding does not specify the format
	        	// (i.e. application/bil or image/bil), then convert to the default encoding configured
	        	// by the server administrator. Operator is not created if no conversion is necessary.
	        	if (defaultDataType != null && ((bilEncoding.equals("application/bil") ||
	        			bilEncoding.equals("image/bil"))))
	        	{
	        		bilEncoding = defaultDataType;
	        	}

	        	if((bilEncoding.equals("application/bil32"))&&(dtype!=DataBuffer.TYPE_FLOAT))	        	{
	        	    transformedImage = FormatDescriptor.create(transformedImage,DataBuffer.TYPE_FLOAT ,null);
	        	}
	        	if((bilEncoding.equals("application/bil16"))&&(dtype!=DataBuffer.TYPE_SHORT))	        	{
	        	    transformedImage = FormatDescriptor.create(transformedImage,DataBuffer.TYPE_SHORT ,null);
	        	}
	        	if((bilEncoding.equals("application/bil8"))&&(dtype!=DataBuffer.TYPE_BYTE))	        	{
	        	    transformedImage = FormatDescriptor.create(transformedImage,DataBuffer.TYPE_BYTE ,null);
	        	}

	        	TiledImage tiled = new TiledImage(transformedImage,width,height);

	        	final ImageOutputStream imageOutStream = ImageIO.createImageOutputStream(outStream);
		        final ImageWriter writer = writerSPI.createWriterInstance();

		        // Set byte order out of output stream based on layer configuration.
		        if (ByteOrder.LITTLE_ENDIAN.toString().equals(byteOrder))
		        {
		        	imageOutStream.setByteOrder(ByteOrder.LITTLE_ENDIAN);
		        } else if (ByteOrder.BIG_ENDIAN.toString().equals(byteOrder))
		        {
		        	imageOutStream.setByteOrder(ByteOrder.BIG_ENDIAN);
		        }

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
	 * getFinalCoverage - message the RenderedImage into Bil
	 *
	 * @param request CoverageRequest
	 * @param meta CoverageInfo
	 * @param parameters
	 * @param mapContent Context for GetMap request.
	 * @param coverage GridCoverage
	 * @return GridCoverage2D
	 * @throws WcsException
	 * @throws IOException
	 * @throws IndexOutOfBoundsException
	 * @throws FactoryException
	 * @throws TransformException
	 */
	private static GridCoverage2D getFinalCoverage(GetMapRequest request, MapLayerInfo meta,
	        WMSMapContent mapContent, GridCoverage2DReader coverageReader)
	    throws WcsException, IOException, IndexOutOfBoundsException, FactoryException,
	        TransformException {
	    // This is the final Response CRS
	    final String responseCRS = request.getSRS();

	    // - then create the Coordinate Reference System
	    final CoordinateReferenceSystem targetCRS = CRS.decode(responseCRS);

	    // This is the CRS of the requested Envelope
	    final String requestCRS = request.getSRS();

	    // - then create the Coordinate Reference System
	    final CoordinateReferenceSystem sourceCRS = CRS.decode(requestCRS);

	    // This is the CRS of the Coverage Envelope
	    final CoordinateReferenceSystem cvCRS = ((GeneralEnvelope) coverageReader
	        .getOriginalEnvelope()).getCoordinateReferenceSystem();

	    GeneralEnvelope destinationEnvelope = new GeneralEnvelope(mapContent.getRenderingArea());

	    // this is the destination envelope in the coverage crs
        final GeneralEnvelope destinationEnvelopeInSourceCRS = CRS.transform(destinationEnvelope,
                cvCRS);

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
	
	    final GridCoverage coverage = coverageReader.read(CoverageUtils.getParameters(
	                coverageReader.getFormat().getReadParameters(), parameters, true));
	
	    if ((coverage == null) || !(coverage instanceof GridCoverage2D)) {
	        throw new IOException("The requested coverage could not be found.");
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

	static {
	    RecodeRaster.register(JAI.getDefaultInstance());
	}
}
