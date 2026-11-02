/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.gwc.hazelcast.web;

import java.io.Serial;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;
import org.geoserver.gwc.web.memorycache.KeyValueStoreType;
import org.geowebcache.hazelcast.HazelcastEmbeddedKeyValueStoreInfo;

/** {@link KeyValueStoreType} for the embedded Hazelcast backend, making each node a member of the cluster. */
public class HazelcastEmbeddedKeyValueStoreType implements KeyValueStoreType<HazelcastEmbeddedKeyValueStoreInfo> {

    @Serial
    private static final long serialVersionUID = 1L;

    @Override
    public String toString() {
        return "Hazelcast embedded (shared by all nodes)";
    }

    @Override
    public Class<HazelcastEmbeddedKeyValueStoreInfo> getConfigClass() {
        return HazelcastEmbeddedKeyValueStoreInfo.class;
    }

    @Override
    public HazelcastEmbeddedKeyValueStoreInfo newConfigObject() {
        return new HazelcastEmbeddedKeyValueStoreInfo();
    }

    @Override
    public Panel createPanel(String id, IModel<HazelcastEmbeddedKeyValueStoreInfo> model) {
        return new EmbeddedPanel(id, model);
    }

    static class EmbeddedPanel extends Panel {

        @Serial
        private static final long serialVersionUID = 1L;

        EmbeddedPanel(String id, IModel<HazelcastEmbeddedKeyValueStoreInfo> model) {
            super(id, model);
            add(new TextField<String>("mapName", new PropertyModel<>(model, "mapName")).setRequired(true));
        }
    }
}
