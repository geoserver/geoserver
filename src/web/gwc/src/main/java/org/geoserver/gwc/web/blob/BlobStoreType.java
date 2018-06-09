package org.geoserver.gwc.web.blob;

import java.io.Serializable;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.geowebcache.config.BlobStoreConfig;

/**
 * Configures a blobstore type, function as factory for the config object and config panel.
 *
 * @author Niels Charlier
 * @param <T> subclass of BlobStoreConfig for this type
 */
public interface BlobStoreType<T extends BlobStoreConfig> extends Serializable {

    Class<T> getConfigClass();

    T newConfigObject();

    Panel createPanel(String id, IModel<T> model);
}
