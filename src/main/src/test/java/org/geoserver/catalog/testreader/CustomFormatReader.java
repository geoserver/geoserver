/* Copyright (c) 2001 - 2011 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.catalog.testreader;

import it.geosolutions.imageioimpl.plugins.tiff.TIFFImageReaderSpi;
import java.awt.Color;
import java.awt.image.RenderedImage;
import java.awt.image.renderable.ParameterBlock;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.imageio.spi.ImageInputStreamSpi;
import javax.imageio.stream.ImageInputStream;
import javax.media.jai.JAI;
import javax.media.jai.PlanarImage;
import org.geotools.coverage.Category;
import org.geotools.coverage.GridSampleDimension;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.coverage.grid.io.AbstractGridCoverage2DReader;
import org.geotools.factory.Hints;
import org.geotools.gce.geotiff.GeoTiffReader;
import org.geotools.image.io.ImageIOExt;
import org.geotools.resources.i18n.Vocabulary;
import org.geotools.resources.i18n.VocabularyKeys;
import org.geotools.util.NumberRange;
import org.opengis.coverage.grid.Format;
import org.opengis.parameter.GeneralParameterValue;
import org.opengis.parameter.ParameterValue;

/**
 * Coverage reader for custom dimensions tests.
 * 
 * @author Mike Benowitz
 */
public final class CustomFormatReader extends AbstractGridCoverage2DReader {
    
    private static final String MY_DIMENSION_DOMAIN =
            CustomFormat.CUSTOM_DIMENSION_NAME + "_DOMAIN";
    private static final String HAS_MY_DIMENSION_DOMAIN = "HAS_" + MY_DIMENSION_DOMAIN;
    
    private static final TIFFImageReaderSpi READER_SPI = new TIFFImageReaderSpi();
    
    private static final double DEFAULT_NODATA = 9999.0;
    
    private final File dataDirectory;
    
    
    public CustomFormatReader(Object source, Hints hints)
            throws IOException {
        super(source, hints);
        if (source instanceof File) {
            this.dataDirectory = (File)source;
            initReaderFromFile(hints);
        } else {
            throw new IllegalArgumentException("Invalid source object");
        }
    }

    @Override public Format getFormat() {
        return new CustomFormat();
    }

    @Override public GridCoverage2D read(GeneralParameterValue[] params)
            throws IOException {
        boolean haveDimension = false;
        for (GeneralParameterValue p : params) {
            if (p.getDescriptor().getName().toString().equalsIgnoreCase(
                        CustomFormat.CUSTOM_DIMENSION_NAME)) {
                System.out.println("CustomFormatReader.read: " + p);
                haveDimension = true;
                final String value = String.valueOf(extractValue(p));
                for (String filename : this.dataDirectory.list()) {
                    if (isDataFile(filename)) {
                        final String dimValue = getDimensionValue(filename);
                        if (dimValue.equalsIgnoreCase(value)) {
                            return createCoverage(filename);
                        }
                    }
                }
            }
        }
        
        if (!haveDimension) {
            // No dimension value specified; just return first image
            for (String filename : this.dataDirectory.list()) {
                if (isDataFile(filename)) {
                    return createCoverage(filename);
                }
            }
        }
        return null;
    }

    @Override public String[] getMetadataNames() {
        return new String[] { HAS_MY_DIMENSION_DOMAIN, MY_DIMENSION_DOMAIN };
    }

    @Override public String getMetadataValue(final String name) {
        if (HAS_MY_DIMENSION_DOMAIN.equalsIgnoreCase(name)) {
            return String.valueOf(true);
        }
        if (MY_DIMENSION_DOMAIN.equalsIgnoreCase(name)) {
            return dimensionValueList();
        }
        return null;
    }
    
    private String dimensionValueList() {
        final StringBuilder sb = new StringBuilder();
        for (String filename : this.dataDirectory.list()) {
            if (isDataFile(filename)) {
                sb.append(getDimensionValue(filename));
                sb.append(',');
            }
        }
        final int len = sb.length();
        if (len == 0) {
            return null;
        }
        return sb.substring(0, len - 1);
    }
    
    private static String getDimensionValue(String filename) {
        int startInd = filename.indexOf('^') + 1;
        int endInd = filename.lastIndexOf('.');
        return filename.substring(startInd, endInd);        
    }
    
    private void initReaderFromFile(Hints inHints) throws IOException {
        if (!this.dataDirectory.isDirectory()) {
            throw new IOException(this.dataDirectory + " is not a directory");
        }
        
        File dataFile = null;
        for (String filename : this.dataDirectory.list()) {
            if (isDataFile(filename)) {
                dataFile = new File(this.dataDirectory, filename);
                break;
            }
        }
        if (dataFile == null) {
            throw new IOException("No data file found");
        }
        
        final GeoTiffReader geotiffReader = new GeoTiffReader(dataFile, hints);
        this.crs = geotiffReader.getCrs();
        this.originalGridRange = geotiffReader.getOriginalGridRange();
        this.originalEnvelope = geotiffReader.getOriginalEnvelope();
    }
    
    /**
     * Reads an image from a GeoTIFF file. For more information, see
     * <a href="http://download.java.net/media/jai-imageio/javadoc/1.1/com/sun/media/jai/operator/ImageReadDescriptor.html#RenderedMode">ImageReadDescriptor</a>
     */
    private static synchronized RenderedImage readImage(File inFile) throws IOException {
        final ParameterBlock readParams = new ParameterBlock();
        ImageInputStreamSpi lSpi = ImageIOExt.getImageInputStreamSPI(inFile);
        PlanarImage lImage = null;
        ImageInputStream lImgIn =
            lSpi.createInputStreamInstance(inFile, false, null);
        readParams.add(lImgIn);
        readParams.add(0);
        readParams.add(Boolean.FALSE);
        readParams.add(Boolean.FALSE);
        readParams.add(Boolean.FALSE);
        readParams.add(null);
        readParams.add(null);
        readParams.add(null);
        readParams.add(READER_SPI.createReaderInstance());
        lImage = JAI.create("ImageRead", readParams, null);
        final String lFileName = inFile.getName();
        final int lExtIndex = lFileName.lastIndexOf('.');
        final String lFileNameNoExt = lExtIndex < 0 ? lFileName :
            lFileName.substring(0, lExtIndex);
        lImage.setProperty("name", lFileNameNoExt);
        return lImage;
    }
    
    /**
     * Creates a {@link GridCoverage2D} for the specified file.
     */
    private GridCoverage2D createCoverage(String filename) throws IOException {
        final File dataFile = new File(this.dataDirectory, filename);
        final RenderedImage image = readImage(dataFile);
        return createCoverage(String.valueOf(image.getProperty("name")), image);
    }
    
    /**
     * Creates a {@link GridCoverage2D} for the provided {@link RenderedImage}.
     */
    private GridCoverage2D createCoverage(String name, RenderedImage image) {
        Category noDataCategory = new Category(
                Vocabulary.formatInternational(VocabularyKeys.NODATA), 
                new Color[] { new Color(0, 0, 0, 0) }, 
                NumberRange.create(DEFAULT_NODATA, DEFAULT_NODATA), 
                NumberRange.create(DEFAULT_NODATA, DEFAULT_NODATA));
        Category[] categories = new Category[] { noDataCategory };
        GridSampleDimension[] bands;
        bands = new GridSampleDimension[1];
        bands[0] = 
            new GridSampleDimension(null, categories, null).geophysics(true);
        final Map<String, Double> properties = new HashMap<String, Double>();
        properties.put("GC_NODATA", DEFAULT_NODATA);
        return this.coverageFactory.create(name, image, this.originalEnvelope,
                                           bands, null, properties);
    }
    
    private static boolean isDataFile(String filename) {
        return filename.endsWith(".tif") || filename.endsWith(".tiff");
    }
    
    /** Helper for read method. */
    private static Object extractValue(GeneralParameterValue param) {
        Object retVal = null;
        if (param instanceof ParameterValue<?>) {
            final Object paramVal = ((ParameterValue<?>)param).getValue();
            if (paramVal != null) {
                if (paramVal instanceof List) {
                    final List<?> list = (List<?>)paramVal;
                    if (!list.isEmpty()) retVal = list.get(0);
                } else {
                    retVal = paramVal;
                }
            }
        }
        return retVal;
    }
}
