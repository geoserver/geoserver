/* (c) 2014-2015 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
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
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import javax.imageio.spi.ImageInputStreamSpi;
import javax.imageio.stream.ImageInputStream;
import javax.media.jai.JAI;
import javax.media.jai.PlanarImage;
import org.geotools.coverage.Category;
import org.geotools.coverage.GridSampleDimension;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.coverage.grid.GridCoverageFactory;
import org.geotools.coverage.grid.io.AbstractGridCoverage2DReader;
import org.geotools.coverage.util.CoverageUtilities;
import org.geotools.gce.geotiff.GeoTiffReader;
import org.geotools.image.ImageWorker;
import org.geotools.image.io.ImageIOExt;
import org.geotools.metadata.i18n.Vocabulary;
import org.geotools.metadata.i18n.VocabularyKeys;
import org.geotools.util.Converters;
import org.geotools.util.NumberRange;
import org.geotools.util.factory.Hints;
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
    private static final String MY_DIMENSION_DATATYPE = MY_DIMENSION_DOMAIN + "_DATATYPE";

    private static final TIFFImageReaderSpi READER_SPI = new TIFFImageReaderSpi();

    private static final double DEFAULT_NODATA = 9999.0;

    private final File dataDirectory;

    private String clazz;

    public CustomFormatReader(Object source, Hints hints) throws IOException {
        super(source, hints);
        if (source instanceof File) {
            this.dataDirectory = (File) source;
            initReaderFromFile(hints);
        } else {
            throw new IllegalArgumentException("Invalid source object");
        }
    }

    @Override
    public Format getFormat() {
        return new CustomFormat();
    }

    @Override
    public GridCoverage2D read(GeneralParameterValue[] params) throws IOException {
        boolean haveDimension = false;
        final List<GridCoverage2D> returnValues = new ArrayList<GridCoverage2D>();
        for (GeneralParameterValue p : params) {
            if (p.getDescriptor()
                    .getName()
                    .toString()
                    .equalsIgnoreCase(CustomFormat.CUSTOM_DIMENSION_NAME)) {
                haveDimension = true;
                final List<?> value = extractValue(p);
                for (Object o : value) {
                    final String s = Converters.convert(o, String.class);
                    for (String filename : this.dataDirectory.list()) {
                        if (isDataFile(filename)) {
                            final String dimValue = getDimensionValue(filename);
                            if (dimValue.equalsIgnoreCase(s)) {
                                returnValues.add(createCoverage(filename));
                                break;
                            }
                        }
                    }
                }
            }
        }

        final int size = returnValues.size();
        if (!haveDimension) {
            // No dimension value specified; just return first image
            for (String filename : this.dataDirectory.list()) {
                if (isDataFile(filename)) {
                    return createCoverage(filename);
                }
            }
        } else if (size > 0) {
            // single value
            if (size == 1) {
                return returnValues.get(0);
            } else {
                // we return a multiband coverage that uses the original ones as sources
                final ImageWorker worker = new ImageWorker(returnValues.get(0).getRenderedImage());
                for (int i = 1; i < size; i++) {
                    worker.addBand(returnValues.get(i).getRenderedImage(), false);
                }
                final GridSampleDimension sds[] = new GridSampleDimension[size];
                Arrays.fill(sds, returnValues.get(0).getSampleDimensions()[0]);
                return new GridCoverageFactory()
                        .create(
                                "result",
                                worker.getRenderedImage(),
                                returnValues.get(0).getEnvelope(),
                                sds,
                                null,
                                null);
            }
        }
        return null;
    }

    @Override
    public String[] getMetadataNames() {
        return new String[] {HAS_MY_DIMENSION_DOMAIN, MY_DIMENSION_DOMAIN};
    }

    @Override
    public String getMetadataValue(final String name) {
        if (HAS_MY_DIMENSION_DOMAIN.equalsIgnoreCase(name)) {
            return String.valueOf(true);
        }
        if (MY_DIMENSION_DOMAIN.equalsIgnoreCase(name)) {
            return dimensionValueList();
        }
        if (MY_DIMENSION_DATATYPE.equalsIgnoreCase(name) && clazz != null) {
            return clazz;
        }
        return null;
    }

    private String dimensionValueList() {
        final TreeSet<String> elements = new TreeSet<String>();
        for (String filename : this.dataDirectory.list()) {
            if (isDataFile(filename)) {
                elements.add(getDimensionValue(filename));
            }
        }
        if (elements.size() <= 0) {
            return null;
        }

        final StringBuilder sb = new StringBuilder();
        for (String item : elements) {
            sb.append(item).append(',');
        }
        final int len = sb.length();
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

        File clazzFile = new File(dataDirectory, "clazz");
        if (clazzFile.exists()) {
            clazz = new String(Files.readAllBytes(clazzFile.toPath())).trim();
        }

        final GeoTiffReader geotiffReader = new GeoTiffReader(dataFile, hints);
        this.crs = geotiffReader.getCoordinateReferenceSystem();
        this.originalGridRange = geotiffReader.getOriginalGridRange();
        this.originalEnvelope = geotiffReader.getOriginalEnvelope();
    }

    /**
     * Reads an image from a GeoTIFF file. For more information, see <a
     * href="http://download.java.net/media/jai-imageio/javadoc/1.1/com/sun/media/jai/operator/ImageReadDescriptor.html#RenderedMode">ImageReadDescriptor</a>
     */
    private static synchronized RenderedImage readImage(File inFile) throws IOException {
        final ParameterBlock readParams = new ParameterBlock();
        ImageInputStreamSpi lSpi = ImageIOExt.getImageInputStreamSPI(inFile);
        PlanarImage lImage = null;
        ImageInputStream lImgIn = lSpi.createInputStreamInstance(inFile, false, null);
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
        final String lFileNameNoExt = lExtIndex < 0 ? lFileName : lFileName.substring(0, lExtIndex);
        lImage.setProperty("name", lFileNameNoExt);
        return lImage;
    }

    /** Creates a {@link GridCoverage2D} for the specified file. */
    private GridCoverage2D createCoverage(String filename) throws IOException {
        final File dataFile = new File(this.dataDirectory, filename);
        final RenderedImage image = readImage(dataFile);
        return createCoverage(String.valueOf(image.getProperty("name")), image);
    }

    /** Creates a {@link GridCoverage2D} for the provided {@link RenderedImage}. */
    private GridCoverage2D createCoverage(String name, RenderedImage image) {
        Category noDataCategory =
                new Category(
                        Vocabulary.formatInternational(VocabularyKeys.NODATA),
                        new Color[] {new Color(0, 0, 0, 0)},
                        NumberRange.create(DEFAULT_NODATA, DEFAULT_NODATA));
        Category[] categories = new Category[] {noDataCategory};
        GridSampleDimension[] bands;
        bands = new GridSampleDimension[1];
        bands[0] = new GridSampleDimension(null, categories, null);
        final Map<String, Object> properties = new HashMap<String, Object>();
        CoverageUtilities.setNoDataProperty(properties, DEFAULT_NODATA);
        return this.coverageFactory.create(
                name, image, this.originalEnvelope, bands, null, properties);
    }

    private static boolean isDataFile(String filename) {
        return filename.endsWith(".tif") || filename.endsWith(".tiff");
    }

    /** Helper for read method. */
    private static List<?> extractValue(GeneralParameterValue param) {
        if (param instanceof ParameterValue<?>) {
            final Object paramVal = ((ParameterValue<?>) param).getValue();
            if (paramVal != null) {
                if (paramVal instanceof List) {
                    final List<?> list = (List<?>) paramVal;
                    return list;
                } else {
                    throw new UnsupportedOperationException(
                            "Custom dimension value must be a list");
                }
            }
        }
        throw new UnsupportedOperationException("Custom dimension value must be a list");
    }
}
