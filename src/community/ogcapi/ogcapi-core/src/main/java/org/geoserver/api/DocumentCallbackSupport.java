/* (c) 2019 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.api;

import java.util.List;
import org.geoserver.platform.GeoServerExtensions;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

/** Helper that applies the {@link DocumentCallback} to the given collections */
@Component
public class DocumentCallbackSupport implements ApplicationContextAware {

    private List<DocumentCallback> callbacks;

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.callbacks = GeoServerExtensions.extensions(DocumentCallback.class, applicationContext);
    }

    /**
     * Applies all available callbacks to the document
     *
     * @param document The document the {@link DocumentCallback} will operate on
     */
    public void apply(AbstractDocument document) {
        for (DocumentCallback callback : callbacks) {
            callback.apply(document);
        }
    }
}
