/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms.kvp;

import java.awt.image.ColorModel;
import java.awt.image.DataBuffer;
import java.awt.image.IndexColorModel;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.ImageTypeSpecifier;
import javax.imageio.stream.ImageInputStream;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.platform.GeoServerResourceLoader;
import org.geoserver.platform.resource.Resource;
import org.geoserver.platform.resource.Resources;
import org.geotools.image.palette.InverseColorMapOp;
import org.geotools.util.SoftValueHashMap;

/**
 * Allows access to palettes (implemented as {@link IndexColorModel} classes)
 *
 * @author Andrea Aime - TOPP
 * @author Simone Giannecchini - GeoSolutions
 */
public class PaletteManager {
    private static final Logger LOG = org.geotools.util.logging.Logging.getLogger("PaletteManager");

    /** The key used in the format options to specify what quantizer to use */
    public static final String QUANTIZER = "quantizer";

    /**
     * Safe palette, a 6x6x6 color cube, followed by a 39 elements gray scale, and a final
     * transparent element. See the internet safe color palette for a reference <a
     * href="http://www.intuitive.com/coolweb/colors.html">
     */
    public static final String SAFE = "SAFE";

    public static final IndexColorModel safePalette = buildDefaultPalette();
    static SoftValueHashMap<String, PaletteCacheEntry> paletteCache =
            new SoftValueHashMap<String, PaletteCacheEntry>();
    static SoftValueHashMap<IndexColorModelKey, InverseColorMapOp> opCache =
            new SoftValueHashMap<IndexColorModelKey, InverseColorMapOp>();

    /** TODO: we should probably provide the data directory as a constructor parameter here */
    private PaletteManager() {}

    /** Loads a PaletteManager */
    public static IndexColorModel getPalette(String name) throws Exception {
        // check for safe paletteInverter
        if ("SAFE".equals(name.toUpperCase())) {
            return safePalette;
        }

        // check for cached one, making sure it's not stale
        final PaletteCacheEntry entry = (PaletteCacheEntry) paletteCache.get(name);
        if (entry != null) {
            if (entry.isStale()) {
                paletteCache.remove(name);
            } else {
                return entry.icm;
            }
        }

        // ok, load it. for the moment we load palettes from .png and .gif
        // files, but we may want to extend this ability to other file formats
        // (Gimp palettes for example), in this case we'll adopt the classic
        // plugin approach using either the Spring context of the SPI

        // hum... loading the paletteDir could be done once, but then if the
        // users
        // adds the paletteInverter dir with a running Geoserver, we won't find it
        // anymore...
        GeoServerResourceLoader loader = GeoServerExtensions.bean(GeoServerResourceLoader.class);

        Resource palettes = loader.get("palettes");

        Set<String> names = new HashSet<String>();
        names.addAll(
                Arrays.asList(
                        new String[] {name + ".gif", name + ".png", name + ".pal", name + ".tif"}));

        List<Resource> paletteFiles = new ArrayList<Resource>();
        for (Resource item : palettes.list()) {
            if (names.contains(item.name().toLowerCase())) {
                paletteFiles.add(item);
            }
        }

        // scan the files found (we may have multiple files with different
        // extensions and return the first paletteInverter you find
        for (Resource resource : paletteFiles) {
            final String fileName = resource.name();
            if (fileName.endsWith("pal")) {
                final IndexColorModel icm = new PALFileLoader(resource).getIndexColorModel();

                if (icm != null) {
                    paletteCache.put(name, new PaletteCacheEntry(resource, icm));
                    return icm;
                }
            } else {
                ImageInputStream iis = ImageIO.createImageInputStream(resource.in());
                final Iterator it = ImageIO.getImageReaders(iis);
                if (it.hasNext()) {
                    final ImageReader reader = (ImageReader) it.next();
                    reader.setInput(iis);
                    final ColorModel cm =
                            ((ImageTypeSpecifier) reader.getImageTypes(0).next()).getColorModel();
                    if (cm instanceof IndexColorModel) {
                        final IndexColorModel icm = (IndexColorModel) cm;
                        paletteCache.put(name, new PaletteCacheEntry(resource, icm));
                        return icm;
                    }
                }
            }
            LOG.warning(
                    "Skipping paletteInverter file "
                            + fileName
                            + " since color model is not indexed (no 256 colors paletteInverter)");
        }

        return null;
    }

    public static InverseColorMapOp getInverseColorMapOp(IndexColorModel icm) {
        // check for cached one, making sure it's not stale
        IndexColorModelKey key = new IndexColorModelKey(icm);
        InverseColorMapOp op = (InverseColorMapOp) opCache.get(key);
        if (op != null) {
            return op;
        } else {
            op = new InverseColorMapOp(icm);
            opCache.put(key, op);
            return op;
        }
    }

    /** Builds the internet safe paletteInverter */
    static IndexColorModel buildDefaultPalette() {
        int[] cmap = new int[256];

        // Create the standard 6x6x6 color cube (all elements do cycle
        // between 00, 33, 66, 99, CC and FF, the decimal difference is 51)
        // The color is made of alpha, red, green and blue, in this order, from
        // the most significant bit onwards.
        int i = 0;
        int opaqueAlpha = 255 << 24;

        for (int r = 0; r < 256; r += 51) {
            for (int g = 0; g < 256; g += 51) {
                for (int b = 0; b < 256; b += 51) {
                    cmap[i] = opaqueAlpha | (r << 16) | (g << 8) | b;
                    i++;
                }
            }
        }

        // The gray scale. Make sure we end up with gray == 255
        int grayIncr = 256 / (255 - i);
        int gray = 255 - ((255 - i - 1) * grayIncr);

        for (; i < 255; i++) {
            cmap[i] = opaqueAlpha | (gray << 16) | (gray << 8) | gray;
            gray += grayIncr;
        }

        // setup the transparent color (alpha == 0)
        cmap[255] = (255 << 16) | (255 << 8) | 255;

        // create the color model
        return new IndexColorModel(8, 256, cmap, 0, true, 255, DataBuffer.TYPE_BYTE);
    }

    /** An entry in the paletteInverter cache. Can determine wheter it's stale or not, too */
    private static class PaletteCacheEntry {
        Resource file;

        long lastModified;

        IndexColorModel icm;

        public PaletteCacheEntry(Resource file, IndexColorModel icm) {
            this.file = file;
            this.icm = icm;
            this.lastModified = file.lastmodified();
        }

        /** Returns true if the backing file does not exist any more, or has been modified */
        public boolean isStale() {
            return !Resources.exists(file) || (file.lastmodified() != lastModified);
        }
    }

    /**
     * IndexColorModel has a broken hashcode implementation (inherited from ColorModel and not
     * overridden), use a custom key that leverages identity hash code instead (a full equals would
     * be expensive, palettes can have 65k entries)
     */
    private static class IndexColorModelKey {
        IndexColorModel icm;

        public IndexColorModelKey(IndexColorModel icm) {
            this.icm = icm;
        }

        @Override
        public int hashCode() {
            return System.identityHashCode(icm);
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (obj == null) return false;
            if (getClass() != obj.getClass()) return false;
            IndexColorModelKey other = (IndexColorModelKey) obj;
            return icm == other.icm;
        }
    }
}
