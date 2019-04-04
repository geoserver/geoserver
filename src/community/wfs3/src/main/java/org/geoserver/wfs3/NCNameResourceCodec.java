/* (c) 2018 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wfs3;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.NamespaceInfo;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.ows.LocalWorkspace;
import org.geotools.util.MapEntry;
import org.geotools.util.logging.Logging;

/**
 * Copy of NCNameResourceCodec found in WCS 2.0.
 *
 * <p>TODO: move the class in main and share (there is one static method there that's WCS 2.0
 * specific, needs first to be moved somewhere else)
 */
public class NCNameResourceCodec {
    protected static Logger LOGGER = Logging.getLogger(NCNameResourceCodec.class);

    private static final String DELIMITER = "__";

    public static String encode(ResourceInfo resource) {
        return encode(resource.getNamespace().getPrefix(), resource.getName());
    }

    public static String encode(String workspaceName, String resourceName) {
        final WorkspaceInfo workspace = LocalWorkspace.get();
        if (workspace != null && workspace.getName().equalsIgnoreCase(workspaceName)) {
            return resourceName;
        }
        return workspaceName + DELIMITER + resourceName;
    }

    /**
     * Search in the catalog the Layers matching the encoded id.
     *
     * <p>
     *
     * @return A possibly empty list of the matching layers, or null if the encoded id could not be
     *     decoded.
     */
    public static List<LayerInfo> getLayers(Catalog catalog, String encodedResourceId) {
        final WorkspaceInfo workspace = LocalWorkspace.get();
        if (workspace != null) {
            encodedResourceId = workspace.getName() + DELIMITER + encodedResourceId;
        }

        List<MapEntry<String, String>> decodedList = decode(encodedResourceId);
        if (decodedList.isEmpty()) {
            LOGGER.info("Could not decode id '" + encodedResourceId + "'");
            return null;
        }

        List<LayerInfo> ret = new ArrayList<LayerInfo>();

        LOGGER.info(" Examining encoded name " + encodedResourceId);

        for (MapEntry<String, String> mapEntry : decodedList) {

            String namespace = mapEntry.getKey();
            String localName = mapEntry.getValue();

            if (namespace == null || namespace.isEmpty()) {
                LOGGER.log(Level.FINE, " Checking coverage name {0}", localName);

                LayerInfo layer = catalog.getLayerByName(localName);
                if (layer != null) {
                    LOGGER.log(Level.FINE, " - Collecting layer {0}", layer.prefixedName());
                    ret.add(layer);
                } else {
                    LOGGER.log(Level.FINE, " - Ignoring layer {0}", localName);
                }
            } else {
                LOGGER.info(" Checking pair " + namespace + " : " + localName);

                String fullName = namespace + ":" + localName;
                NamespaceInfo nsInfo = catalog.getNamespaceByPrefix(namespace);
                if (nsInfo != null) {
                    LOGGER.log(Level.FINE, " - Namespace found {0}", namespace);
                    LayerInfo layer = catalog.getLayerByName(fullName);
                    if (layer != null) {
                        LOGGER.log(Level.FINE, " - Collecting layer {0} ", layer.prefixedName());
                        ret.add(layer);
                    } else {
                        LOGGER.log(Level.FINE, " - Ignoring layer {0} " + fullName);
                    }
                } else {
                    LOGGER.info(" - Namespace not found " + namespace);
                }
            }
        }

        return ret;
    }

    /**
     * @return a List of possible workspace/name pairs, possibly empty if the input could not be
     *     decoded;
     */
    public static List<MapEntry<String, String>> decode(String qualifiedName) {
        int lastPos = qualifiedName.lastIndexOf(DELIMITER);
        List<MapEntry<String, String>> ret = new ArrayList<MapEntry<String, String>>();

        if (lastPos == -1) {
            ret.add(new MapEntry<String, String>(null, qualifiedName));
            return ret;
        }

        while (lastPos > -1) {
            String ws = qualifiedName.substring(0, lastPos);
            String name = qualifiedName.substring(lastPos + DELIMITER.length());
            ret.add(new MapEntry<String, String>(ws, name));
            lastPos = qualifiedName.lastIndexOf(DELIMITER, lastPos - 1);
        }
        return ret;
    }
}
