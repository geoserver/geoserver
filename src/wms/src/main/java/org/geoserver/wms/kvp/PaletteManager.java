/* Copyright (c) 2001, 2007 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, availible at the root
 * application directory.
 */
package org.geoserver.wms.kvp;

import java.awt.image.ColorModel;
import java.awt.image.DataBuffer;
import java.awt.image.IndexColorModel;
import java.io.File;
import java.io.FilenameFilter;
import java.util.Iterator;
import java.util.logging.Logger;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.ImageTypeSpecifier;
import javax.imageio.stream.ImageInputStream;

import org.geotools.image.palette.InverseColorMapOp;
import org.geotools.util.SoftValueHashMap;
import org.vfny.geoserver.global.GeoserverDataDirectory;

/**
 * Allows access to palettes (implemented as {@link IndexColorModel} classes)
 * 
 * @author Andrea Aime - TOPP
 * @author Simone Giannecchini - GeoSolutions
 * 
 */
public class PaletteManager {
    private static final Logger LOG = org.geotools.util.logging.Logging.getLogger("PaletteManager");

    /**
     * Safe palette, a 6x6x6 color cube, followed by a 39 elements gray scale,
     * and a final transparent element. See the internet safe color palette for
     * a reference <a href="http://www.intuitive.com/coolweb/colors.html">
     */
    public static final String SAFE = "SAFE";
    public static final IndexColorModel safePalette = buildDefaultPalette();
    static SoftValueHashMap paletteCache = new SoftValueHashMap();

	private static InverseColorMapOp safePaletteInversion= new InverseColorMapOp(safePalette);
    /**
     * TODO: we should probably provide the data directory as a constructor
     * parameter here
     */
    private PaletteManager() {
    }

	/**
	 * Loads a PaletteManager
	 * 
	 * @param name
	 * @return
	 * @throws Exception
	 */
	public static InverseColorMapOp getPalette(String name)
			throws Exception {
		// check for safe paletteInverter
		if ("SAFE".equals(name.toUpperCase())) {
			return safePaletteInversion;
		}

		// check for cached one, making sure it's not stale
		final PaletteCacheEntry entry = (PaletteCacheEntry) paletteCache
				.get(name);
		if (entry != null) {
			if (entry.isStale()) {
				paletteCache.remove(name);
			} else {
				return entry.eicm;
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
		final File root = GeoserverDataDirectory.getGeoserverDataDirectory();
		final File paletteDir = GeoserverDataDirectory.findConfigDir(root,
				"palettes");
		final String[] names = new String[] { name + ".gif", name + ".png",
				name + ".pal", name + ".tif" };
		final File[] paletteFiles = paletteDir.listFiles(new FilenameFilter() {
			public boolean accept(File dir, String name) {
				for (int i = 0; i < names.length; i++) {
					if (name.toLowerCase().equals(names[i])) {
						return true;
					}
				}

				return false;
			}
		});

		// scan the files found (we may have multiple files with different
		// extensions and return the first paletteInverter you find
		for (int i = 0; i < paletteFiles.length; i++) {
			final File file = paletteFiles[i];
			final String fileName = file.getName();
			if (fileName.endsWith("pal")) {
				final IndexColorModel icm = new PALFileLoader(file)
						.getIndexColorModel();

				if (icm != null) {
					final InverseColorMapOp eicm = new InverseColorMapOp(
							icm);
					paletteCache.put(name, new PaletteCacheEntry(file, eicm));
					return eicm;
				}
			} else {
				ImageInputStream iis = ImageIO.createImageInputStream(file);
                final Iterator it = ImageIO.getImageReaders(iis);
				if (it.hasNext()) {
					final ImageReader reader = (ImageReader) it.next();
					reader.setInput(iis);
					final ColorModel cm = ((ImageTypeSpecifier) reader
							.getImageTypes(0).next()).getColorModel();
					if (cm instanceof IndexColorModel) {
						final IndexColorModel icm = (IndexColorModel) cm;
						final InverseColorMapOp eicm = new InverseColorMapOp(
								icm);
						paletteCache.put(name,
								new PaletteCacheEntry(file, eicm));
						return eicm;
					}
				}
			}
			LOG
					.warning("Skipping paletteInverter file "
							+ file.getName()
							+ " since color model is not indexed (no 256 colors paletteInverter)");
		}

		return null;
	}

	/**
	 * Builds the internet safe paletteInverter
	 */
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
		return new IndexColorModel(8, 256, cmap, 0, true, 255,
				DataBuffer.TYPE_BYTE);
	}

	/**
	 * An entry in the paletteInverter cache. Can determine wheter it's stale or not,
	 * too
	 */
	private static class PaletteCacheEntry {
		File file;

		long lastModified;

		InverseColorMapOp eicm;

		public PaletteCacheEntry(File file,
				InverseColorMapOp eicm) {
			this.file = file;
			this.eicm = eicm;
			this.lastModified = file.lastModified();
		}

		/**
		 * Returns true if the backing file does not exist any more, or has been
		 * modified
		 * 
		 * @return
		 */
		public boolean isStale() {
			return !file.exists() || (file.lastModified() != lastModified);
		}
	}
}
