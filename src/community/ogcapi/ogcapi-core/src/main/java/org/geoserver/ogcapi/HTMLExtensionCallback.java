/* (c) 2020 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.ogcapi;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.List;
import java.util.Map;
import org.geoserver.ows.Request;

/**
 * Allows to extend HTML templates by providing templates to be included. The main template uses the
 * ``htmlExtension`` function. A single main template can have multiple htmlExtension calls, and
 * differentiate them by the arguments provided (e.g. if the extension is called in a loop might be
 * passing the current loop variable as an argument that will be used to generate the HTML).
 *
 * <p>A simple Freemarker based implementation can be achieved implementing this interface and
 * depending on {@link FreemarkerTemplateSupport} for the processing.
 *
 * @param <T>
 */
public interface HTMLExtensionCallback {

    /**
     * Returns HTML to be embedded in place of the "htmlExtension" call (if multiple callbacks
     * return HTML, they will be concatenated with a newline to separate them).
     *
     * @param dr The current request
     * @param model The model value that will be used by the template
     * @param htmlExtensionArguments the arguments provided to the htmlExtension function
     */
    public String getExtension(
            Request dr, Map<String, Object> model, Charset charset, List htmlExtensionArguments)
            throws IOException;
}
