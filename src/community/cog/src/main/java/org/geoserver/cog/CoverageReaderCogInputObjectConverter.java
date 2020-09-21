/* (c) 2020 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.cog;

import it.geosolutions.imageio.core.BasicAuthURI;
import it.geosolutions.imageio.core.SourceSPIProvider;
import it.geosolutions.imageioimpl.plugins.cog.*;
import it.geosolutions.imageioimpl.plugins.cog.CogSourceSPIProvider;
import java.io.Serializable;
import java.net.URI;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Logger;
import javax.annotation.Nullable;
import javax.imageio.spi.ImageInputStreamSpi;
import javax.imageio.spi.ImageReaderSpi;
import org.geoserver.catalog.*;
import org.geotools.util.factory.Hints;
import org.geotools.util.logging.Logging;
import org.opengis.coverage.grid.GridCoverageReader;

/**
 * Attempts to convert the source input object for a {@link GridCoverageReader} to a {@link
 * SourceSPIProvider}
 */
public class CoverageReaderCogInputObjectConverter
        implements CoverageReaderInputObjectConverter<SourceSPIProvider> {

    static Logger LOGGER = Logging.getLogger(CoverageReaderCogInputObjectConverter.class);

    private static final ImageInputStreamSpi COG_IMAGE_INPUT_STREAM_SPI =
            new CogImageInputStreamSpi();

    private static final ImageReaderSpi COG_IMAGE_READER_SPI = new CogImageReaderSpi();

    private final Catalog catalog;

    public CoverageReaderCogInputObjectConverter(Catalog catalog) {
        this.catalog = catalog;
    }

    @Override
    public Optional<SourceSPIProvider> convert(
            Object input, @Nullable CoverageInfo coverageInfo, @Nullable Hints hints) {
        return convert(input, coverageInfo, null, hints);
    }

    /**
     * Performs the conversion of the input object to a {@link SourceSPIProvider} object. If this
     * converter is not able to convert the input to that, an empty {@link Optional} will be
     * returned.
     *
     * @param input The input object.
     * @param coverageInfo The grid coverage metadata, may be <code>null</code>.
     * @param coverageStoreInfo The grid coverage store
     * @param hints Hints to use when loading the coverage, may be <code>null</code>.
     * @return
     */
    @Override
    public Optional<SourceSPIProvider> convert(
            Object input,
            @Nullable CoverageInfo coverageInfo,
            @Nullable CoverageStoreInfo coverageStoreInfo,
            @Nullable Hints hints) {
        if (!(input instanceof String)) {
            return Optional.empty();
        }
        String urlString = (String) input;
        return canConvert(urlString)
                ? convertReaderInputObject(urlString, coverageStoreInfo)
                : Optional.empty();
    }

    /**
     * Checks to see if the input string is a COG URI.
     *
     * @param input The input string.
     * @return Value representing whether or not this converter is able to convert the provided
     *     input.
     */
    protected boolean canConvert(String input) {
        return input.startsWith(CogSettings.COG_URL_HEADER);
    }

    /**
     * Performs the conversion to a {@link SourceSPIProvider} object
     *
     * @param input The input string.
     * @param coverageStoreInfo the input coverage store info
     * @return The Optional object containing the provider
     */
    protected Optional<SourceSPIProvider> convertReaderInputObject(
            String input, CoverageStoreInfo coverageStoreInfo) {

        String realUrl = input;
        if (realUrl.startsWith(CogSettings.COG_URL_HEADER)) {
            realUrl = input.substring(CogSettings.COG_URL_HEADER.length());
        }

        MetadataMap metadata = coverageStoreInfo.getMetadata();
        CogSettings cogSettings = new CogSettings();
        if (metadata != null && metadata.containsKey(CogSettings.COG_SETTINGS_KEY)) {
            cogSettings = (CogSettings) metadata.get(CogSettings.COG_SETTINGS_KEY);
        }

        Map<String, Serializable> connectionParameters =
                coverageStoreInfo.getConnectionParameters();
        URI baseUri = URI.create(realUrl);
        String user = null;
        String password = null;
        if (connectionParameters != null) {

            Object userObject = connectionParameters.get("user");
            Object passwordObject = connectionParameters.get("password");
            if (userObject != null && passwordObject != null) {
                user = (String) userObject;
                password = (String) passwordObject;
            }
        }
        BasicAuthURI cogUri =
                new BasicAuthURI(baseUri, cogSettings.isUseCachingStream(), user, password);
        SourceSPIProvider object =
                new CogSourceSPIProvider(
                        cogUri,
                        COG_IMAGE_READER_SPI,
                        COG_IMAGE_INPUT_STREAM_SPI,
                        cogSettings.getRangeReaderSettings().getRangeReaderClassName());
        return Optional.of(object);
    }
}
