/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms.kvp;

import java.awt.*;
import java.awt.image.IndexColorModel;
import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.StringTokenizer;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.geoserver.platform.resource.Resource;
import org.geoserver.platform.resource.Resource.Type;

/**
 * Loads a JASC Pal files into an {@link IndexColorModel}.
 *
 * <p>I made a real minor extension to the usual form of a JASC pal file which allows us to provide
 * values in the #ffffff or 0Xffffff hex form.
 *
 * <p>Note that this kind of file does not support explicitly setting transparent pixel. However I
 * have implemented this workaround, if you use less than 256 colors in your paletteInverter I will
 * accordingly set the transparent pixel to the first available position in the paletteInverter,
 * which is palette_size. If you use 256 colors no transparency will be used for the image we
 * generate.
 *
 * <p><strong>Be aware</strong> that IrfanView does not always report correctly the size of the
 * palette it exports. Be ready to manually correct the number of colors reported.
 *
 * <p>Here is an explanation of what a JASC pal file should look like:
 *
 * <p><a href="http://www.cryer.co.uk/filetypes/p/pal.htm">JASC PAL file</a>
 *
 * <p>and here is a list of other possible formats we could parse (in the future if we have time or
 * someone pays for it :-) )
 *
 * <p><a href="http://www.pl32.com/forum/viewtopic.php?t=873">alternative PAL file formats</a>
 *
 * @author Simone Giannecchini
 */
class PALFileLoader {
    protected static final Logger LOGGER =
            org.geotools.util.logging.Logging.getLogger(
                    "it.geosolutions.inversecolormap.PALFileLoader");

    /** Size of the color map we'll use. */
    protected int mapsize;

    /** Final index color model. */
    protected IndexColorModel indexColorModel;

    /**
     * {@link PALFileLoader} constructor that accept a resource.
     *
     * <p>Note that the transparentIndex pixel should not exceed the last zero-based index available
     * for the colormap we area going to create. If this happens we might get very bad behaviour.
     * Note also that if we set this parameter to -1 we'll get an opaque {@link IndexColorModel}.
     *
     * @param file the palette file.
     */
    public PALFileLoader(Resource file) {
        if (file.getType() != Type.RESOURCE)
            throw new IllegalArgumentException("The provided file does not exist.");
        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new InputStreamReader(file.in()));
            // header
            boolean loadNext = false;
            String temp = trimNextLine(reader);
            if (temp.equalsIgnoreCase("JASC-PAL")) {
                if (LOGGER.isLoggable(Level.FINE)) LOGGER.fine("Found header in palette file");
                loadNext = true;
            }

            // version
            if (loadNext) {
                temp = trimNextLine(reader);
                if (temp.equalsIgnoreCase("0100")) {
                    if (LOGGER.isLoggable(Level.FINE)) LOGGER.fine("Found version in palette file");
                    loadNext = true;
                }
            }

            // num colors
            if (loadNext) temp = trimNextLine(reader);

            this.mapsize = Integer.parseInt(temp);
            if (mapsize > 256 || mapsize <= 0)
                throw new IllegalArgumentException("The provided number of colors is invalid");

            // load various colors
            final byte colorMap[][] = new byte[3][mapsize < 256 ? mapsize + 1 : mapsize];
            for (int i = 0; i < mapsize; i++) {
                // get the line
                temp = trimNextLine(reader);

                if (temp.startsWith("#")) temp = "0x" + temp.substring(1);

                if (temp.startsWith("0x") || temp.startsWith("0X")) {
                    final Color color = Color.decode(temp);
                    colorMap[0][i] = (byte) color.getRed();
                    colorMap[1][i] = (byte) color.getGreen();
                    colorMap[2][i] = (byte) color.getBlue();
                } else {
                    // tokenize it
                    final StringTokenizer tokenizer = new StringTokenizer(temp, " ", false);
                    int numComponents = 0;
                    while (tokenizer.hasMoreTokens()) {
                        if (numComponents >= 3)
                            throw new IllegalArgumentException(
                                    "The number of components in one the color is greater than 3!");
                        colorMap[numComponents++][i] =
                                (byte) Integer.parseInt(tokenizer.nextToken());
                    }
                    if (numComponents != 3)
                        throw new IllegalArgumentException(
                                "The number of components in one the color is invalid!");
                }
            }

            // //
            //
            // create the index color model reserving space for the transparent
            // pixel is room exists.
            //
            ////
            if (mapsize < 256)
                this.indexColorModel =
                        new IndexColorModel(
                                8, mapsize + 1, colorMap[0], colorMap[1], colorMap[2], mapsize);
            else
                this.indexColorModel =
                        new IndexColorModel(8, mapsize, colorMap[0], colorMap[1], colorMap[2]);
        } catch (FileNotFoundException e) {
            LOGGER.log(Level.SEVERE, e.getLocalizedMessage(), e);
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, e.getLocalizedMessage(), e);

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, e.getLocalizedMessage(), e);
        } finally {
            if (reader != null)
                try {
                    reader.close();
                } catch (IOException e) {
                    LOGGER.log(Level.INFO, e.getLocalizedMessage(), e);
                }
        }
    }

    public String trimNextLine(BufferedReader reader) throws IOException {
        String line = reader.readLine();
        if (line == null) {
            throw new IOException(
                    "Was expecting to get another line, but the end of file was reached, while reading a PAL file");
        }
        return line.trim();
    }

    public IndexColorModel getIndexColorModel() {
        return indexColorModel;
    }

    public int getMapsize() {
        return mapsize;
    }
}
