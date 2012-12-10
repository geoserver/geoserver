/* Copyright (c) 2001 - 2007 TOPP - www.openplans.org.  All rights reserved.
 * This code is licensed under the GPL 2.0 license, availible at the root
 * application directory.
 */
package org.geoserver.wcs.responses;

import it.geosolutions.imageioimpl.plugins.tiff.TIFFLZWCompressor;

import java.awt.Dimension;
import java.awt.image.BandedSampleModel;
import java.awt.image.SampleModel;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.media.jai.JAI;

import org.geoserver.platform.ServiceException;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.coverage.grid.io.AbstractGridFormat;
import org.geotools.coverage.grid.io.imageio.GeoToolsWriteParams;
import org.geotools.gce.geotiff.GeoTiffFormat;
import org.geotools.gce.geotiff.GeoTiffWriteParams;
import org.geotools.gce.geotiff.GeoTiffWriter;
import org.geotools.util.Utilities;
import org.opengis.parameter.GeneralParameterValue;
import org.opengis.parameter.ParameterValueGroup;
import org.vfny.geoserver.wcs.WcsException;
import org.vfny.geoserver.wcs.WcsException.WcsExceptionCode;

import com.sun.media.imageio.plugins.tiff.BaselineTIFFTagSet;

/**
 * Coverage writer for the geotiff format.
 * 
 * @author $Author: Alessio Fabiani (alessio.fabiani@gmail.com) $ (last modification)
 * @author Simone Giannecchini, GeoSolutions SAS
 */
public class GeoTIFFCoverageResponseDelegate implements CoverageResponseDelegate {

    /** DEFAULT_JPEG_COMPRESSION_QUALITY */
    private static final float DEFAULT_JPEG_COMPRESSION_QUALITY = 0.75f;

    private static final Set<String> FORMATS = new HashSet<String>(Arrays.asList("image/tiff","image/tiff;subtype=\"geotiff\"","image/geotiff"));

	private static final GeoTiffFormat GEOTIF_FORMAT = new GeoTiffFormat();

	public static final String GEOTIFF_CONTENT_TYPE = "image/tiff;subtype=\"geotiff\"";

    public GeoTIFFCoverageResponseDelegate() {
    }

    public boolean canProduce(String outputFormat) {
        return outputFormat != null
                && (outputFormat.equalsIgnoreCase("geotiff") || FORMATS.contains(outputFormat.toLowerCase()));
    }

    public String getMimeType(String outputFormat) {
        if (canProduce(outputFormat))
            return "image/tiff;subtype=\"geotiff\"";
        else
            return null;
    }

    public String getFileExtension(String outputFormat) {
        return "tif";
    }

    public void encode(GridCoverage2D sourceCoverage, String outputFormat, Map<String,String> econdingParameters, OutputStream output) throws ServiceException, IOException {
        Utilities.ensureNonNull("sourceCoverage", sourceCoverage);
        Utilities.ensureNonNull("econdingParameters", econdingParameters);

        
        // imposing encoding parameters
        final GeoTiffWriteParams wp = new GeoTiffWriteParams();
        
        // compression
        handleCompression(econdingParameters, wp);
        
        // tiling
        handleTiling(econdingParameters, wp);
        
        // interleaving
        handleInterleaving(econdingParameters, wp, sourceCoverage);

        final ParameterValueGroup writerParams = GEOTIF_FORMAT.getWriteParameters();
        writerParams.parameter(AbstractGridFormat.GEOTOOLS_WRITE_PARAMS.getName().toString()).setValue(wp);

        // write down
        GeoTiffWriter writer = (GeoTiffWriter) GEOTIF_FORMAT.getWriter(output);
        try {
            if (writer != null)
                writer.write(sourceCoverage, (GeneralParameterValue[]) writerParams.values()
                        .toArray(new GeneralParameterValue[1]));
        } finally {
            try {
                if (writer != null)
                    writer.dispose();
            } catch (Throwable e) {
                // eating exception
            }
            sourceCoverage.dispose(false);
        }
    }

    /**
     * @param econdingParameters
     * @param wp
     * @param sourceCoverage 
     */
    private void handleInterleaving(Map<String, String> encondingParameters, GeoTiffWriteParams wp, GridCoverage2D sourceCoverage) {

        // interleaving is optional
        if(encondingParameters.containsKey("interleave")){
            
            // ok, the interleaving has been specified, let's see what we got
            final String interleavingS= encondingParameters.get("interleave");
            if(interleavingS.equals("pixel")){
                
                // ok we want pixel interleaving, let's see what we can do 
                
                // -1- is the image already in pixel interleaving ?
                final SampleModel sm= sourceCoverage.getRenderedImage().getSampleModel();
                if(sm instanceof BandedSampleModel){
                    // reformat image 
                    // TODO
                }
                
            } else if(interleavingS.equals("band")){
                // TODO
            } else {
                throw new WcsException("Invalid Interleaving type provided", WcsExceptionCode.InterleavingInvalid, interleavingS);
            }
        }
        
    }

    /**
     * @param econdingParameters
     * @param wp
     * @throws NumberFormatException
     * @throws WcsException
     */
    private void handleTiling(Map<String, String> econdingParameters, final GeoTiffWriteParams wp)
            throws NumberFormatException, WcsException {
        //
        // tiling
        //
        if(econdingParameters.containsKey("tiling")){
            
            final String tilingS= econdingParameters.get("tiling");
            if(tilingS!=null&&Boolean.valueOf(tilingS)){  
                wp.setTilingMode(GeoToolsWriteParams.MODE_EXPLICIT);
                
                // start with default dimension, since tileW and tileH are optional
                final Dimension tileDimensions= new Dimension(JAI.getDefaultTileSize());
                
                // tileW
                if(econdingParameters.containsKey("tilewidth")){
                    final String tileW_= econdingParameters.get("tilewidth");
                    if(tileW_!=null){  
                        final int tileW=Integer.valueOf(tileW_);
                        if(tileW>0&& (tileW%16==0)){
                            tileDimensions.width=tileW;
                        } else {
                            // compression not supported
                            throw new WcsException(
                                    "Provided tile width is invalid",
                                    WcsExceptionCode.TilingInvalid,
                                    tileW_);                            
                        }
                    }
                    
                }
                // tileH    
                if(econdingParameters.containsKey("tileheight")){
                    final String tileH_= econdingParameters.get("tileheight");
                    if(tileH_!=null){  
                        final int tileH=Integer.valueOf(tileH_);
                        if(tileH>=0&&(tileH%16==0)){
                            tileDimensions.height=tileH;
                        } else {
                            // compression not supported
                            throw new WcsException(
                                    "Provided tile width is invalid",
                                    WcsExceptionCode.TilingInvalid,
                                    tileH_);                            
                        }
                    }
                    
                }                
                
                // set tile dimensions
                wp.setTiling(tileDimensions.width, tileDimensions.height);                      
            }
        }
    }

    /**
     * @param econdingParameters
     * @param wp
     * @throws WcsException
     * @throws NumberFormatException
     */
    private void handleCompression(Map<String, String> econdingParameters,
            final GeoTiffWriteParams wp) throws WcsException, NumberFormatException {
        // compression
        if(econdingParameters.containsKey("compression")){ 
            String compressionS= econdingParameters.get("compression");
            if(compressionS!=null&&!compressionS.equalsIgnoreCase("none")){ 
                if(compressionS.equals("LZW")){
                    wp.setCompressionMode(GeoTiffWriteParams.MODE_EXPLICIT);
                    wp.setCompressionType("LZW"); 
                    
                    // look for a predictor
                    String predictorS= econdingParameters.get("predictor");
                    if(predictorS!=null){
                        if(predictorS.equals("None")){
                            
                        } else if(predictorS.equals("Horizontal")){
                            wp.setTIFFCompressor(new TIFFLZWCompressor(BaselineTIFFTagSet.PREDICTOR_HORIZONTAL_DIFFERENCING));
                        } else if(predictorS.equals("Floatingpoint")){
                            // NOT SUPPORTED YET
                            throw new WcsException(
                                    "Floating Point predictor is not supported",
                                    WcsExceptionCode.PredictorNotSupported,
                                    "");
                        } else {
                            // invalid predictor
                            throw new WcsException(
                                    "Invalid Predictor provided",
                                    WcsExceptionCode.PredictorInvalid,
                                    predictorS);
                        }
                    }
                } else if(compressionS.equals("JPEG")){
                    wp.setCompressionMode(GeoTiffWriteParams.MODE_EXPLICIT);
                    wp.setCompressionType("JPEG"); 
                    // start with the default one, visually lossless
                    wp.setCompressionQuality(DEFAULT_JPEG_COMPRESSION_QUALITY);
                    
                    // check quality 
                    if(econdingParameters.containsKey("jpeg_quality")){ 
                        final String quality_= econdingParameters.get("jpeg_quality");
                        if(quality_!=null){  
                            final int quality=Integer.valueOf(quality_);
                            if(quality>0&&quality<=100){
                                wp.setCompressionQuality(quality/100.f);
                            } else {
                                // invalid quality
                                throw new WcsException(
                                        "Provided quality value for the jpeg compression in invalid",
                                        WcsExceptionCode.JpegQualityInvalid,
                                        quality_);
                            }
                        }  
                    }
                } else if(compressionS.equals("PackBits")){
                    wp.setCompressionMode(GeoTiffWriteParams.MODE_EXPLICIT);
                    wp.setCompressionType("PackBits");      
                } else if(compressionS.equals("DEFLATE")){
                    wp.setCompressionMode(GeoTiffWriteParams.MODE_EXPLICIT);
                    wp.setCompressionType("Deflate");      
                } else if(compressionS.equals("Huffman")){
                    wp.setCompressionMode(GeoTiffWriteParams.MODE_EXPLICIT);
                    wp.setCompressionType("CCITT RLE");  
                } else {
                    // compression not supported
                    throw new WcsException("Provided compression does not seem supported",WcsExceptionCode.CompressionNotSupported,compressionS);
                }
            }
        }
    }
    
    @Override
    public List<String> getOutputFormats() {
        return Arrays.asList("image/tiff;subtype=\"geotiff\"");
    }
    
    @Override
    public boolean isAvailable() {
        return true;
    }

}
