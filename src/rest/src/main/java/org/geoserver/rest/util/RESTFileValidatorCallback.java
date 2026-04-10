/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.rest.util;

import java.io.IOException;
import java.io.InputStream;

/**
 * Callback for the REST file upload.
 *
 * <p>This will be called on the file you uploaded. It should throw if there's an issue processing the file.
 */
public interface RESTFileValidatorCallback {

    /**
     * Process a file to see if its "acceptable".
     *
     * @param inputStream file (from user) - might be a .zip
     * @param fname name (from the user request)
     * @throws IOException throw if there's an issue processing the file
     */
    void accept(InputStream inputStream, String fname) throws IOException;
}
