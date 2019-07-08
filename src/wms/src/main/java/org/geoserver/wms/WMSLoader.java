/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CatalogFactory;
import org.geoserver.catalog.LayerGroupInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.catalog.StyleInfo;
import org.geoserver.config.GeoServer;
import org.geoserver.config.util.LegacyServiceLoader;
import org.geoserver.config.util.LegacyServicesReader;
import org.geoserver.wms.WMSInfo.WMSInterpolation;
import org.geoserver.wms.WatermarkInfo.Position;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.util.decorate.Wrapper;
import org.geotools.util.logging.Logging;

public class WMSLoader extends LegacyServiceLoader<WMSInfo> {

    static Logger LOGGER = Logging.getLogger("org.geoserver.wms");

    public Class<WMSInfo> getServiceClass() {
        return WMSInfo.class;
    }

    @SuppressWarnings("unchecked")
    public WMSInfo load(LegacyServicesReader reader, GeoServer geoServer) throws Exception {
        WMSInfoImpl wms = new WMSInfoImpl();
        wms.setId("wms");

        Map<String, Object> props = reader.wms();
        readCommon(wms, props, geoServer);

        WatermarkInfo wm = new WatermarkInfoImpl();
        wm.setEnabled((Boolean) props.get("globalWatermarking"));
        wm.setURL((String) props.get("globalWatermarkingURL"));
        wm.setTransparency((Integer) props.get("globalWatermarkingTransparency"));
        wm.setPosition(Position.get((Integer) props.get("globalWatermarkingPosition")));
        wms.setWatermark(wm);
        wms.setDynamicStylingDisabled(
                props.containsKey("dynamicStylingDisabled")
                        ? (Boolean) props.get("dynamicStylingDisabled")
                        : false);

        try {
            wms.setInterpolation(
                    WMSInterpolation.valueOf((String) props.get("allowInterpolation")));
        } catch (Exception e) {
            // fallback on the default value if loading failed
            wms.setInterpolation(WMSInterpolation.Nearest);
        }
        wms.getMetadata().put("svgRenderer", (Serializable) props.get("svgRenderer"));
        wms.getMetadata().put("svgAntiAlias", (Serializable) props.get("svgAntiAlias"));

        // max GetFeatureInfo search radius
        wms.setMaxBuffer((Integer) props.get("maxBuffer"));

        // max memory usage
        wms.setMaxRequestMemory((Integer) props.get("maxRequestMemory"));

        // the max rendering time
        wms.setMaxRenderingTime((Integer) props.get("maxRenderingTime"));

        // the max number of rendering errors
        wms.setMaxRenderingErrors((Integer) props.get("maxRenderingErrors"));

        // base maps
        Catalog catalog = geoServer.getCatalog();
        // ... we need access to the actual catalog, not a filtered out view of the
        // layers accessible to the current user
        if (catalog instanceof Wrapper) catalog = ((Wrapper) catalog).unwrap(Catalog.class);
        CatalogFactory factory = catalog.getFactory();

        List<Map> baseMaps = (List<Map>) props.get("BaseMapGroups");
        if (baseMaps != null) {
            O:
            for (Map baseMap : baseMaps) {
                LayerGroupInfo bm = factory.createLayerGroup();
                bm.setName((String) baseMap.get("baseMapTitle"));

                // process base map layers
                List<String> layerNames = (List) baseMap.get("baseMapLayers");
                for (String layerName : layerNames) {
                    ResourceInfo resource = null;
                    if (layerName.contains(":")) {
                        String[] qname = layerName.split(":");
                        resource =
                                catalog.getResourceByName(qname[0], qname[1], ResourceInfo.class);
                    } else {
                        resource = catalog.getResourceByName(layerName, ResourceInfo.class);
                    }

                    if (resource == null) {
                        LOGGER.warning(
                                "Ignoring layer group '"
                                        + bm.getName()
                                        + "', resource '"
                                        + layerName
                                        + "' does not exist");
                        continue O;
                    }

                    List<LayerInfo> layers = catalog.getLayers(resource);
                    if (layers.isEmpty()) {
                        LOGGER.warning(
                                "Ignoring layer group '"
                                        + bm.getName()
                                        + "', no layer found for resource '"
                                        + layerName
                                        + "'");
                        continue O;
                    }

                    bm.getLayers().add(layers.get(0));
                }

                // process base map styles
                List<String> styleNames = (List) baseMap.get("baseMapStyles");
                if (styleNames.isEmpty()) {
                    // use defaults
                    bm.getStyles().addAll(Collections.nCopies(bm.getLayers().size(), null));
                } else {
                    for (int i = 0; i < styleNames.size(); i++) {
                        String styleName = styleNames.get(i);
                        styleName = styleName.trim();

                        StyleInfo style = null;
                        if ("".equals(styleName)) {
                            style = null;
                        } else {
                            style = catalog.getStyleByName(styleName);
                        }
                        bm.getStyles().add(style);
                    }
                }
                bm.getMetadata().put("rawStyleList", (String) baseMap.get("rawBaseMapStyles"));

                // base map enveloper
                ReferencedEnvelope e = (ReferencedEnvelope) baseMap.get("baseMapEnvelope");
                if (e == null) {
                    e = new ReferencedEnvelope();
                    e.setToNull();
                }
                bm.setBounds(e);

                LOGGER.info("Processed layer group '" + bm.getName() + "'");
                catalog.add(bm);
            }
        }

        wms.getVersions().add(WMS.VERSION_1_1_1);
        wms.getVersions().add(WMS.VERSION_1_3_0);
        return wms;
    }
}
