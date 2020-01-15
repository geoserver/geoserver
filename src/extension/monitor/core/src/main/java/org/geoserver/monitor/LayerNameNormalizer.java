/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.monitor;

import java.util.List;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.NamespaceInfo;

/**
 * Post processor that normalizes layer names occuring in {@link RequestData#getLayers()}.
 *
 * @author Justin Deoliveira, OpenGeo
 */
public class LayerNameNormalizer implements RequestPostProcessor {

    static final String PROCESSOR_NAME = "layerNameNormalizer";

    Catalog cat;

    public LayerNameNormalizer(Catalog cat) {
        this.cat = cat;
    }

    public void run(RequestData data, HttpServletRequest request, HttpServletResponse response)
            throws Exception {

        List<String> layers = data.getResources();
        if (layers == null) {
            return;
        }
        final int size = layers.size();
        for (int i = 0; i < size; i++) {
            String layer = layers.get(i);
            int colon = layer.lastIndexOf(':');
            if (colon == -1) {
                // totally non prefixed, do a catalog lookup for the layer
                LayerInfo l = cat.getLayerByName(layer);
                if (l != null) {
                    layers.set(i, l.getResource().prefixedName());
                }
            } else {
                // prefix, may be by full namespace uri though
                String prefix = layer.substring(0, colon);
                String local = layer.substring(colon + 1);

                NamespaceInfo ns = cat.getNamespaceByPrefix(prefix);
                if (ns == null) {
                    // maybe it was prefixed by uri
                    ns = cat.getNamespaceByURI(prefix);
                    if (ns != null) {
                        prefix = ns.getPrefix();
                    }
                    layers.set(i, prefix + ":" + local);
                }
            }
        }
    }

    @Override
    public String getName() {
        return PROCESSOR_NAME;
    }
}
