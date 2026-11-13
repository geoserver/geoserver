/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.gwc.hazelcast.web;

import java.io.Serial;
import java.util.List;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;
import org.geoserver.gwc.web.memorycache.KeyValueStoreType;
import org.geowebcache.hazelcast.HazelcastClientKeyValueStoreInfo;

/** {@link KeyValueStoreType} for the Hazelcast client backend, keeping tiles in a separate Hazelcast cluster. */
public class HazelcastClientKeyValueStoreType implements KeyValueStoreType<HazelcastClientKeyValueStoreInfo> {

    @Serial
    private static final long serialVersionUID = 1L;

    @Override
    public String toString() {
        return "Hazelcast client (shared by all nodes)";
    }

    @Override
    public Class<HazelcastClientKeyValueStoreInfo> getConfigClass() {
        return HazelcastClientKeyValueStoreInfo.class;
    }

    @Override
    public HazelcastClientKeyValueStoreInfo newConfigObject() {
        return new HazelcastClientKeyValueStoreInfo();
    }

    @Override
    public Panel createPanel(String id, IModel<HazelcastClientKeyValueStoreInfo> model) {
        return new ClientPanel(id, model);
    }

    static class ClientPanel extends Panel {

        @Serial
        private static final long serialVersionUID = 1L;

        ClientPanel(String id, IModel<HazelcastClientKeyValueStoreInfo> model) {
            super(id, model);
            for (String property : List.of("clusterName", "members", "mapName")) {
                add(new TextField<String>(property, new PropertyModel<>(model, property)).setRequired(true));
            }
        }
    }
}
