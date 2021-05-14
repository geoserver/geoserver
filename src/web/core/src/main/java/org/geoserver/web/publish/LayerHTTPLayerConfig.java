/* (c) 2021 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.web.publish;

import org.apache.wicket.model.IModel;
import org.geoserver.catalog.LayerInfo;

/** Configure Layer http max-age */
public class LayerHTTPLayerConfig extends HTTPLayerConfig {

    /** layer resource metadata property name */
    public static final String RESOURCE_METADATA = "resource.metadata";

    public LayerHTTPLayerConfig(String id, IModel<LayerInfo> model) {
        super(id, model, RESOURCE_METADATA);
    }
}
