/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms.decoration;

import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.imageio.ImageIO;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.platform.GeoServerResourceLoader;
import org.geoserver.platform.resource.Files;
import org.geoserver.platform.resource.Resource;
import org.geoserver.platform.resource.Resources;
import org.geoserver.wms.WMSMapContent;
import org.geotools.util.SoftValueHashMap;
import org.geotools.util.URLs;

public class WatermarkDecoration implements MapDecoration {
    /** A logger for this class. */
    private static final Logger LOGGER = Logger.getLogger("org.geoserver.wms.responses");

    public static final Color TRANSPARENT = new Color(255, 255, 255, 0);

    private String imageURL;

    private float opacity = 1.0f;

    /** Transient cache to avoid reloading the same file over and over */
    private static final Map<URL, LogoCacheEntry> logoCache =
            new SoftValueHashMap<URL, LogoCacheEntry>();

    public void loadOptions(Map<String, String> options) {
        this.imageURL = options.get("url");

        if (options.containsKey("opacity")) {
            try {
                opacity = Float.valueOf(options.get("opacity")) / 100f;
                opacity = Math.max(Math.min(opacity, 1f), 0f);
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "Invalid opacity value: " + options.get("opacity"), e);
            }
        }
    }

    public Dimension findOptimalSize(Graphics2D g2d, WMSMapContent mapContent) {
        try {
            BufferedImage logo = getLogo();
            return new Dimension(logo.getWidth(), logo.getHeight());
        } catch (Exception e) {
            return new Dimension(20, 20);
        }
    }

    /** Print the WaterMarks into the graphic2D. */
    public void paint(Graphics2D g2D, Rectangle paintArea, WMSMapContent mapContent)
            throws MalformedURLException, ClassCastException, IOException {
        BufferedImage logo = getLogo();

        if (logo != null) {
            Composite oldComposite = g2D.getComposite();
            g2D.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, opacity));

            AffineTransform tx =
                    AffineTransform.getTranslateInstance(paintArea.getX(), paintArea.getY());

            tx.scale(
                    paintArea.getWidth() / logo.getWidth(),
                    paintArea.getHeight() / logo.getHeight());

            g2D.drawImage(logo, tx, null);

            g2D.setComposite(oldComposite);
        }
    }

    protected BufferedImage getLogo() throws IOException {
        BufferedImage logo = null;

        // fully resolve the url (consider data dir)
        GeoServerResourceLoader loader = GeoServerExtensions.bean(GeoServerResourceLoader.class);
        URL url = null;
        try {
            url = new URL(imageURL);

            if (url.getProtocol() == null || url.getProtocol().equals("file")) {
                File file =
                        Resources.find(
                                Resources.fromURL(
                                        Files.asResource(loader.getBaseDirectory()), imageURL),
                                true);
                if (file.exists()) {
                    url = URLs.fileToUrl(file);
                }
            }
        } catch (MalformedURLException e) {
            // could be a relative reference, check if we can find it in the layouts directory
            Resource layouts = loader.get("layouts");
            if (layouts.getType() == Resource.Type.DIRECTORY) {
                Resource image = layouts.get(imageURL);
                if (image.getType() == Resource.Type.RESOURCE) {
                    url = URLs.fileToUrl(image.file());
                }
            }
            if (url == null) {
                // also check from the root of the data dir (backwards compatibility)
                Resource image = loader.get(imageURL);
                if (image.getType() == Resource.Type.RESOURCE) {
                    url = URLs.fileToUrl(image.file());
                }
            }
        }
        if (url == null) {
            return null;
        }

        LogoCacheEntry entry = logoCache.get(url);
        if (entry == null || entry.isExpired()) {
            logo = ImageIO.read(url);
            if (url.getProtocol().equals("file")) {
                entry = new LogoCacheEntry(logo, new File(url.getFile()));
                logoCache.put(url, entry);
            }
        } else {
            logo = entry.getLogo();
        }

        return logo;
    }

    /**
     * Contains an already loaded logo and the tools to check it's up to date compared to the file
     * system
     *
     * @author Andrea Aime - TOPP
     */
    private static class LogoCacheEntry {
        private BufferedImage logo;

        private long lastModified;

        private File file;

        public LogoCacheEntry(BufferedImage logo, File file) {
            this.logo = logo;
            this.file = file;
            lastModified = file.lastModified();
        }

        public boolean isExpired() {
            return file.lastModified() > lastModified;
        }

        public BufferedImage getLogo() {
            return logo;
        }
    }
}
