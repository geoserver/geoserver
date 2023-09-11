/* (c) 2020 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wps.gs.download.vertical;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.text.MessageFormat;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.io.FilenameUtils;
import org.geotools.api.referencing.FactoryException;
import org.geotools.metadata.i18n.ErrorKeys;
import org.geotools.referencing.factory.ReferencingFactory;
import org.geotools.util.SoftValueHashMap;
import org.geotools.util.URLs;
import org.geotools.util.factory.AbstractFactory;
import org.geotools.util.factory.BufferedFactory;
import org.geotools.util.logging.Logging;

/**
 * Loads and caches Vertical grid files. This incorporates a soft cache mechanism to keep grids in
 * memory when first loaded.
 */
public class VerticalGridShiftFactory extends ReferencingFactory implements BufferedFactory {

    /** The number of hard references to hold internally. */
    private static final int GRID_CACHE_HARD_REFERENCES = 10;

    /** Logger. */
    protected static final Logger LOGGER = Logging.getLogger(VerticalGridShiftFactory.class);

    /** The soft cache that holds loaded grids. */
    private SoftValueHashMap<String, VerticalGridShift> verticalGridCache;

    /** Constructs a factory with the default priority. */
    public VerticalGridShiftFactory() {
        this(NORMAL_PRIORITY);
    }

    /**
     * Constructs an instance using the specified priority level.
     *
     * @param priority The priority for this factory, as a number between {@link
     *     AbstractFactory#MINIMUM_PRIORITY MINIMUM_PRIORITY} and {@link
     *     AbstractFactory#MAXIMUM_PRIORITY MAXIMUM_PRIORITY} inclusive.
     */
    public VerticalGridShiftFactory(final int priority) {
        super(priority);
        verticalGridCache =
                new SoftValueHashMap<>(
                        GRID_CACHE_HARD_REFERENCES,
                        (key, object) -> ((VerticalGridShift) object).dispose());
    }

    /**
     * Creates a VerticalGrid.
     *
     * @param gridLocation The Vertical grid file location
     * @param gridCRSCode the EPSG code of the grid's CRS
     * @return the grid
     * @throws FactoryException if grid cannot be created
     */
    public VerticalGridShift createVerticalGrid(URL gridLocation, int gridCRSCode)
            throws FactoryException {
        if (gridLocation == null) {
            throw new FactoryException("The grid location must be not null");
        }

        synchronized (verticalGridCache) { // Prevent simultaneous threads trying to load same grid
            VerticalGridShift grid = verticalGridCache.get(gridLocation.toExternalForm());
            if (grid != null) { // Cached:
                return grid; // - Return
            } else { // Not cached:
                if (gridLocation != null) {
                    grid = loadVerticalGrid(gridLocation, gridCRSCode); // - Load
                    if (grid != null) {
                        verticalGridCache.put(gridLocation.toExternalForm(), grid); // - Cache
                        return grid; // - Return
                    }
                }
                throw new FactoryException(
                        "Vertical Offset Grid " + gridLocation + " could not be created.");
            }
        }
    }
    /**
     * Loads the grid
     *
     * @param url the vertical grid file absolute path
     * @param gridCRSCode
     * @return the grid, or {@code null} on error
     */
    private VerticalGridShift loadVerticalGrid(URL url, int gridCRSCode) throws FactoryException {
        try {
            if (url.getProtocol().equals("file")) {
                File file = URLs.urlToFile(url);

                if (!file.exists() || !file.canRead()) {
                    throw new IOException(
                            MessageFormat.format(ErrorKeys.FILE_DOES_NOT_EXIST_$1, file));
                }
                return loadGridShift(file, gridCRSCode);
            }
            throw new FactoryException(
                    "URL " + url + " doesn't refer a supported Vertical Grid file");
        } catch (IOException | IllegalArgumentException e) {
            LOGGER.log(Level.SEVERE, e.getLocalizedMessage(), e);
            throw new FactoryException(e.getLocalizedMessage(), e);
        }
    }

    /**
     * Load a {@link VerticalGridShift} from the specified File by assigning it the specified CRS
     * EPSG code. Current implementation only support GeoTIFF Vertical Grids.
     *
     * @param file the File containing the Vertical Grid
     * @param gridCRSCode the EPSG code to be assigned as CRS of the underlying Vertical Grid
     * @return the {@link VerticalGridShift} instance
     * @throws IOException
     * @throws FactoryException
     */
    public static VerticalGridShift loadGridShift(File file, int gridCRSCode)
            throws IOException, FactoryException {
        // In the future, if we start supporting multiple type of grids (csv, gtx, geotiff) we can
        // leverage on an SPI mechanism and a finder.
        String path = file.getAbsolutePath();
        String extension = FilenameUtils.getExtension(FilenameUtils.getName(path));
        if (extension.toUpperCase().endsWith("TIF")) {
            return new GeoTIFFVerticalGridShift(file, gridCRSCode);
        }
        throw new IOException("File " + file + " is not a supported Vertical Grid Offset File");
    }
}
