/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.gwc.web.tilecache;

import java.io.Serializable;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.geowebcache.storage.tilecache.TileCacheBackend;
import org.geowebcache.storage.tilecache.TileCacheBackendInfo;

/**
 * Makes a {@link TileCacheBackend} selectable in the caching defaults page; registered as a Spring bean, its
 * {@link #toString()} is the name shown to the user.
 */
public interface TileCacheBackendType extends Serializable {

    /** The settings class edited by this type, the same as {@link TileCacheBackend#getSettingsClass()}. */
    Class<? extends TileCacheBackendInfo> getSettingsClass();

    /** New settings holding the backend defaults. */
    TileCacheBackendInfo newSettings();

    /** A panel editing the settings in the model, which are of {@link #getSettingsClass()}. */
    Panel createPanel(String id, IModel<TileCacheBackendInfo> model);
}
