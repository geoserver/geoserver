/* (c) 2014 - 2020 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.catalog;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Optional;
import javax.annotation.Nullable;
import org.geoserver.platform.resource.Files;
import org.geoserver.platform.resource.Resource;
import org.geoserver.platform.resource.Resources;
import org.geotools.util.factory.Hints;
import org.opengis.coverage.grid.GridCoverageReader;

/**
 * Attempts to convert the source input object for a {@link GridCoverageReader} to {@link File}.
 *
 * @author joshfix Created on 2/25/20
 */
public class CoverageReaderFileConverter implements CoverageReaderInputObjectConverter<File> {

    private final Catalog catalog;

    public CoverageReaderFileConverter(Catalog catalog) {
        this.catalog = catalog;
    }

    /**
     * Performs the conversion of the input object to a file object. If this converter is not able
     * to convert the input to a File, an empty {@link Optional} will be returned.
     *
     * @param input The input object.
     * @param coverageInfo The grid coverage metadata, may be <code>null</code>.
     * @param hints Hints to use when loading the coverage, may be <code>null</code>.
     * @return
     */
    @Override
    public Optional<File> convert(
            Object input, @Nullable CoverageInfo coverageInfo, @Nullable Hints hints) {
        return convert(input, coverageInfo, null, hints);
    }

    /**
     * Performs the conversion of the input object to a file object. If this converter is not able
     * to convert the input to a File, an empty {@link Optional} will be returned.
     *
     * @param input The input object.
     * @param coverageInfo The grid coverage metadata, may be <code>null</code>.
     * @param coverageStoreInfo The grid coverage store metadata, may be <code>null</code>.
     * @param hints Hints to use when loading the coverage, may be <code>null</code>.
     * @return
     */
    @Override
    public Optional<File> convert(
            Object input,
            @Nullable CoverageInfo coverageInfo,
            @Nullable CoverageStoreInfo coverageStoreInfo,
            @Nullable Hints hints) {
        if (!(input instanceof String)) {
            return Optional.empty();
        }
        String urlString = (String) input;
        return canConvert(urlString) ? convertFile(urlString) : Optional.empty();
    }

    /**
     * Checks to see if the input string is a file URI.
     *
     * @param input The input string.
     * @return Value representing whether or not this converter is able to convert the provided
     *     input to File.
     */
    protected boolean canConvert(String input) {
        boolean canConvert = false;
        // Check to see if our "url" points to a file or not.
        try {
            URI uri = new URL(input).toURI();
            canConvert = uri.getScheme() == null || "file".equalsIgnoreCase(uri.getScheme());
        } catch (MalformedURLException | URISyntaxException | IllegalArgumentException e) {
            if (input.startsWith("file:")) {
                canConvert = true;
            } else {
                // lets see if we have a normal file path
                File file = new File(input);
                canConvert = file.exists() || !file.isAbsolute();
            }
        }
        return canConvert;
    }

    /**
     * Performs the conversion to File.
     *
     * @param input The input string.
     * @return The Optional object containing the File
     */
    protected Optional<File> convertFile(String input) {
        Resource resource = Files.asResource(catalog.getResourceLoader().getBaseDirectory());
        final File readerFile = Resources.find(Resources.fromURL(resource, input), true);
        return (readerFile != null) ? Optional.of(readerFile) : Optional.empty();
    }
}
