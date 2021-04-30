/* (c) 2021 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.web.publish;

import org.apache.wicket.model.IModel;
import org.geoserver.catalog.LayerInfo;

/** Configure LayerGroup http max-age. */
public class LayerGroupHTTPLayerConfig extends HTTPLayerConfig {

    public LayerGroupHTTPLayerConfig(String id, IModel<LayerInfo> model) {
        super(id, model, "metadata");
    }
}
