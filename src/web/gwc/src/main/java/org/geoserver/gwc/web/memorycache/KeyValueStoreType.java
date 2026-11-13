/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.gwc.web.memorycache;

import java.io.Serializable;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.geowebcache.storage.memorycache.KeyValueStoreInfo;

/**
 * Makes a cache backend selectable in the caching defaults page, as {@code BlobStoreType} does for blob stores;
 * registered as a Spring bean, its {@code toString()} is the name shown to the user.
 */
public interface KeyValueStoreType<T extends KeyValueStoreInfo> extends Serializable {

    /** The settings class edited by this type. */
    Class<T> getConfigClass();

    /** New settings holding the backend defaults. */
    T newConfigObject();

    /** A panel editing the settings in the model. */
    Panel createPanel(String id, IModel<T> model);
}
