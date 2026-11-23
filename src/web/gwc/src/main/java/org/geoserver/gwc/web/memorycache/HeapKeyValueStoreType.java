/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.gwc.web.memorycache;

import java.io.Serial;
import org.apache.wicket.markup.html.form.NumberTextField;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;
import org.geowebcache.storage.memorycache.HeapKeyValueStoreInfo;

/** {@link KeyValueStoreType} for the heap backend, which keeps tiles in the memory of each node. */
public class HeapKeyValueStoreType implements KeyValueStoreType<HeapKeyValueStoreInfo> {

    @Serial
    private static final long serialVersionUID = 1L;

    @Override
    public String toString() {
        return "Heap (single node)";
    }

    @Override
    public Class<HeapKeyValueStoreInfo> getConfigClass() {
        return HeapKeyValueStoreInfo.class;
    }

    @Override
    public HeapKeyValueStoreInfo newConfigObject() {
        return new HeapKeyValueStoreInfo();
    }

    @Override
    public Panel createPanel(String id, IModel<HeapKeyValueStoreInfo> model) {
        return new HeapPanel(id, model);
    }

    static class HeapPanel extends Panel {

        @Serial
        private static final long serialVersionUID = 1L;

        HeapPanel(String id, IModel<HeapKeyValueStoreInfo> model) {
            super(id, model);
            NumberTextField<Integer> maxSize =
                    new NumberTextField<>("maxSizeMB", new PropertyModel<>(model, "maxSizeMB"), Integer.class);
            add(maxSize.setMinimum(1).setRequired(true));
        }
    }
}
