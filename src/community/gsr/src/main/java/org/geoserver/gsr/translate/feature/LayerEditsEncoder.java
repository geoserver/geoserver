/* (c) 2020 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

/* Copyright (c) 2017 Boundless - http://boundlessgeo.com All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.gsr.translate.feature;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import net.sf.json.JSONArray;
import net.sf.json.JSONException;
import net.sf.json.JSONObject;
import org.geoserver.gsr.model.feature.Feature;
import org.geoserver.gsr.model.feature.FeatureArray;
import org.geoserver.gsr.model.feature.LayerEdits;
import org.geoserver.gsr.model.feature.ServiceEdits;

public class LayerEditsEncoder {
    private static final Logger LOGGER =
            org.geotools.util.logging.Logging.getLogger(LayerEditsEncoder.class);
    /**
     * Converts JSONObject into LayerEdits Object
     *
     * @param json JSONObject see http://json-lib.sourceforge.net/
     * @return LayerEdits
     */
    public static LayerEdits LayerEditsfromJSON(JSONObject json) {
        LayerEdits layerEdits = new LayerEdits();
        try {
            if (json.containsKey("id")) {
                Integer id = json.getInt("id");
                layerEdits.setId(id);
            }
        } catch (JSONException je) {
            LOGGER.info("Layer Edits JSON parsing issue when trying to retrieve the layer id");
        }

        try {
            if (json.containsKey("adds")) {
                JSONArray addsArray = json.getJSONArray("adds");
                FeatureArray adds = featureArrayFromJSON(addsArray);
                layerEdits.setAdds(adds);
            }
        } catch (JSONException je) {
            LOGGER.info(
                    "Layer Edits JSON parsing issue when trying to retrieve the feature array from the adds object");
        }
        try {
            if (json.containsKey("updates")) {
                JSONArray updatesArray = json.getJSONArray("updates");
                FeatureArray updates = featureArrayFromJSON(updatesArray);
                layerEdits.setUpdates(updates);
            }
        } catch (JSONException je) {
            LOGGER.info(
                    "Layer Edits JSON parsing issue when trying to retrieve the feature array from the updates object");
        }

        try {
            if (json.containsKey("deletes")) {
                JSONArray deletesArray = json.getJSONArray("deletes");
                List<Integer> deletes = new ArrayList<>();
                if (deletesArray != null) {
                    for (int i = 0; i < deletesArray.size(); i++) {
                        deletes.add(deletesArray.getInt(i));
                    }
                    layerEdits.setDeletes(deletes);
                }
            }
        } catch (JSONException je) {
            LOGGER.info(
                    "Layer Edits JSON parsing issue when trying to retrieve the id array from the deletes object");
        }

        return layerEdits;
    }

    /**
     * Jackson does not convert anonymous JSON arrays, so this method adapted from
     *
     * @see org.geoserver.gsr.api.GSRModelReader
     * @param json anonymous array of Features
     * @return FeatureArray object
     */
    public static FeatureArray featureArrayFromJSON(JSONArray json) {
        if (json instanceof JSONArray) {
            List<Feature> features = new ArrayList<>();
            for (Object o : json) {
                try {
                    features.add(FeatureEncoder.fromJson((JSONObject) o));
                } catch (JSONException e) {
                    features.add(null);
                }
            }
            return new FeatureArray(features);
        }
        return null;
    }

    /**
     * Jackson does not convert anonymous JSON arrays, so this method adapted from
     *
     * @see org.geoserver.gsr.api.GSRModelReader
     * @param json anonymous array of Features
     * @return FeatureArray object
     */
    public static ServiceEdits serviceEditsFromJSON(JSONArray json) {
        if (json instanceof JSONArray) {
            List<LayerEdits> layerEdits = new ArrayList<>();
            for (Object o : json) {
                try {
                    layerEdits.add(LayerEditsfromJSON((JSONObject) o));
                } catch (JSONException e) {
                    layerEdits.add(null);
                }
            }
            return new ServiceEdits(layerEdits);
        }
        return null;
    }
}
