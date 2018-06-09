/* This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.catalog;

import com.google.common.io.Files;
import java.io.*;
import java.util.List;
import java.util.logging.Logger;
import org.apache.commons.io.FileUtils;
import org.geotools.styling.*;
import org.geotools.util.Version;
import org.geotools.util.logging.Logging;
import org.xml.sax.EntityResolver;

/**
 * SLD package style handler.
 *
 * @author Jose García
 */
public class SLDPackageHandler extends StyleHandler {

    static Logger LOGGER = Logging.getLogger(SLDPackageHandler.class);

    public static final String FORMAT = "zip";
    public static final String MIMETYPE = "application/zip";

    private SLDHandler sldHandler;

    protected SLDPackageHandler(SLDHandler sldHandler) {
        super("ZIP", FORMAT);
        this.sldHandler = sldHandler;
    }

    @Override
    public String mimeType(Version version) {
        return MIMETYPE;
    }

    @Override
    public StyledLayerDescriptor parse(
            Object input,
            Version version,
            ResourceLocator resourceLocator,
            EntityResolver entityResolver)
            throws IOException {
        File sldFile = null;
        try {
            sldFile = unzipSldPackage(input);
            return sldHandler.parse(sldFile, version, resourceLocator, entityResolver);
        } finally {
            if (sldFile != null) FileUtils.deleteQuietly(sldFile.getParentFile());
        }
    }

    @Override
    public void encode(
            StyledLayerDescriptor sld, Version version, boolean pretty, OutputStream output)
            throws IOException {
        sldHandler.encode(sld, version, pretty, output);
    }

    @Override
    public List<Exception> validate(Object input, Version version, EntityResolver entityResolver)
            throws IOException {
        File sldFile = null;
        try {
            sldFile = unzipSldPackage(input);
            return sldHandler.validate(input, version, entityResolver);
        } finally {
            if (sldFile != null) FileUtils.deleteQuietly(sldFile.getParentFile());
        }
    }

    /**
     * Unzips a SLD package to a temporal folder, returning the SLD file path.
     *
     * @param input
     * @throws IOException
     */
    private File unzipSldPackage(Object input) throws IOException {
        File myTempDir = Files.createTempDir();

        org.geoserver.util.IOUtils.decompress((InputStream) input, myTempDir);

        File[] files =
                myTempDir.listFiles(
                        new FilenameFilter() {
                            public boolean accept(File dir, String name) {
                                return name.toLowerCase().endsWith(".sld");
                            }
                        });

        if (files.length != 1) {
            throw new IOException("No SLD file");
        }

        return files[0];
    }
}
