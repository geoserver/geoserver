/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.gwc.web.blob;

import java.io.Serializable;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.geowebcache.config.BlobStoreInfo;

/**
 * Configures a blobstore type, function as factory for the config object and config panel.
 *
 * @author Niels Charlier
 * @param <T> subclass of BlobStoreInfo for this type
 */
public interface BlobStoreType<T extends BlobStoreInfo> extends Serializable {

    Class<T> getConfigClass();

    T newConfigObject();

    Panel createPanel(String id, IModel<T> model);
}
