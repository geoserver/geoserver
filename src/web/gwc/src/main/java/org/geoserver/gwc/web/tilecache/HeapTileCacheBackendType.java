/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.gwc.web.tilecache;

import java.io.Serial;
import org.apache.wicket.markup.html.form.NumberTextField;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;
import org.geowebcache.storage.tilecache.HeapTileCacheBackendInfo;
import org.geowebcache.storage.tilecache.TileCacheBackendInfo;

/** {@link TileCacheBackendType} for the heap backend, which keeps tiles in the memory of each node. */
public class HeapTileCacheBackendType implements TileCacheBackendType {

    @Serial
    private static final long serialVersionUID = 1L;

    @Override
    public String toString() {
        return "Heap (single node)";
    }

    @Override
    public Class<HeapTileCacheBackendInfo> getSettingsClass() {
        return HeapTileCacheBackendInfo.class;
    }

    @Override
    public TileCacheBackendInfo newSettings() {
        return new HeapTileCacheBackendInfo();
    }

    @Override
    public Panel createPanel(String id, IModel<TileCacheBackendInfo> model) {
        return new HeapPanel(id, model);
    }

    static class HeapPanel extends Panel {

        @Serial
        private static final long serialVersionUID = 1L;

        HeapPanel(String id, IModel<TileCacheBackendInfo> model) {
            super(id, model);
            NumberTextField<Integer> maxSize =
                    new NumberTextField<>("maxSizeMB", new PropertyModel<>(model, "maxSizeMB"), Integer.class);
            add(maxSize.setMinimum(1).setRequired(true));
        }
    }
}
