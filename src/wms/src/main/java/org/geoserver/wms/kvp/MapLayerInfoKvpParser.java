/* (c) 2014 - 2015 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms.kvp;

import static org.geoserver.catalog.LayerGroupHelper.isSingleOrOpaque;

import java.util.ArrayList;
import java.util.List;
import org.geoserver.catalog.LayerGroupInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.ows.FlatKvpParser;
import org.geoserver.ows.KvpParser;
import org.geoserver.ows.util.KvpUtils;
import org.geoserver.platform.ServiceException;
import org.geoserver.wms.MapLayerInfo;
import org.geoserver.wms.WMS;

/**
 * KVP parser to parse a comma separated list of layer names into a list of {@link MapLayerInfo}
 *
 * @author Gabriel Roldan
 */
public class MapLayerInfoKvpParser extends KvpParser {

    private FlatKvpParser rawNamesParser;

    private final WMS wms;
    private List<String> styles;

    public MapLayerInfoKvpParser(final String key, final WMS wms) {
        super(key, MapLayerInfo.class);
        this.wms = wms;
        rawNamesParser = new FlatKvpParser(key, String.class);
    }

    public MapLayerInfoKvpParser(final String key, final WMS wms, String styles) {
        this(key, wms);
        this.styles = KvpUtils.readFlat(styles);
    }

    /** Returns whether the specified resource must be skipped in the context of the current request. */
    protected boolean skipResource(Object theResource) {
        return false;
    }

    @SuppressWarnings("unchecked")
    @Override
    public List<MapLayerInfo> parse(final String paramValue) throws Exception {

        final List<String> layerNames = (List<String>) rawNamesParser.parse(paramValue);

        List<MapLayerInfo> layers = new ArrayList<>(layerNames.size());

        MapLayerInfo layer = null;

        for (int i = 0; i < layerNames.size(); i++) {
            String layerName = layerNames.get(i);
            LayerInfo layerInfo = wms.getLayerByName(layerName);
            if (layerInfo == null) {
                LayerGroupInfo groupInfo = wms.getLayerGroupByName(layerName);
                if (groupInfo == null || LayerGroupInfo.Mode.CONTAINER.equals(groupInfo.getMode())) {
                    throw new ServiceException(
                            layerName + ": no such layer on this server",
                            "LayerNotDefined",
                            getClass().getSimpleName());
                } else {
                    if (skipResource(groupInfo)) continue;
                    List<LayerInfo> groupLayers;
                    if (styles != null && i < styles.size() && isSingleOrOpaque(groupInfo)) {
                        String styleName = styles.get(i);
                        if (styleName != null && !"".equals(styleName)) {
                            groupLayers = groupInfo.layers(styleName);
                        } else {
                            groupLayers = groupInfo.layers();
                        }
                    } else {
                        groupLayers = groupInfo.layers();
                    }
                    for (LayerInfo li : groupLayers) {

                        if (skipResource(li)) continue;

                        layer = new MapLayerInfo(li, groupInfo.getMetadata());
                        layers.add(layer);
                    }
                }
            } else {

                if (skipResource(layerInfo)) continue;

                layer = new MapLayerInfo(layerInfo);
                layers.add(layer);
            }
        }

        return layers;
    }
}
