/* (c) 2019 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.saml;

import java.io.UnsupportedEncodingException;
import java.util.Timer;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.httpclient.HttpClient;
import org.geotools.util.logging.Logging;
import org.joda.time.DateTime;
import org.opensaml.saml2.metadata.provider.HTTPMetadataProvider;
import org.opensaml.saml2.metadata.provider.MetadataProviderException;

public class LoggingHTTPMetadataProvider extends HTTPMetadataProvider {

    static final Logger LOGGER = Logging.getLogger(LoggingHTTPMetadataProvider.class);

    public LoggingHTTPMetadataProvider(String metadataURL, int requestTimeout)
            throws MetadataProviderException {
        super(metadataURL, requestTimeout);
    }

    public LoggingHTTPMetadataProvider(
            Timer backgroundTaskTimer, HttpClient client, String metadataURL)
            throws MetadataProviderException {
        super(backgroundTaskTimer, client, metadataURL);
    }

    @Override
    protected void processNewMetadata(
            String metadataIdentifier, DateTime refreshStart, byte[] metadataBytes)
            throws MetadataProviderException {
        try {
            LOGGER.log(
                    Level.FINE, "Metadata document content: " + new String(metadataBytes, "UTF-8"));
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
        super.processNewMetadata(metadataIdentifier, refreshStart, metadataBytes);
    }
}
