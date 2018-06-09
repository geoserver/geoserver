/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.config.util;

import java.util.ArrayList;
import java.util.List;
import net.sf.json.JSONArray;
import net.sf.json.JSONException;
import net.sf.json.JSONObject;
import org.geoserver.catalog.LayerIdentifierInfo;
import org.geoserver.catalog.MetadataMap;
import org.geoserver.catalog.impl.LayerIdentifier;

/**
 * Utility class to serialize and deserialize a list of {@link LayerIdentifierInfo} objects to and
 * from String using a JSON array representation as serialized form so that {@link XStreamPersister}
 * stores it under a single key in a catalog info's {@link MetadataMap}.
 *
 * @author groldan
 */
public class LayerIdentifierInfoListConverter {

    private static final String AUTHORITY = "authority";

    private static final String IDENTIFIER = "identifier";

    /**
     * @param str a JSON array representation of a list of {@link LayerIdentifierInfo} objects
     * @return the list of parsed layer identifiers from the argument JSON array
     * @throws IllegalArgumentException if {@code str} can't be parsed to a JSONArray
     */
    public static List<LayerIdentifierInfo> fromString(String str) throws IllegalArgumentException {

        try {
            final JSONArray array;
            array = JSONArray.fromObject(str);
            final int size = array.size();
            List<LayerIdentifierInfo> list = new ArrayList<LayerIdentifierInfo>(size);
            JSONObject jsonAuth;
            for (int i = 0; i < size; i++) {
                jsonAuth = array.getJSONObject(i);
                LayerIdentifier id = new LayerIdentifier();
                id.setAuthority(jsonAuth.getString(AUTHORITY));
                id.setIdentifier(jsonAuth.getString(IDENTIFIER));
                list.add(id);
            }
            return list;
        } catch (JSONException e) {
            throw new IllegalArgumentException(e.getMessage(), e);
        }
    }

    /**
     * @param list the list of auth urls to serialize
     * @return {@code null} if {@code list} is null, empty, or contains only null objects; the JSON
     *     array representation of {@code list} otherwise, with any null element stripped off.
     */
    public static String toString(List<LayerIdentifierInfo> list) {
        if (list == null || list.isEmpty()) {
            return null;
        }
        JSONArray array = new JSONArray();

        for (LayerIdentifierInfo id : list) {
            if (id == null) {
                continue;
            }
            JSONObject jsonId = new JSONObject();
            jsonId.put(AUTHORITY, id.getAuthority());
            jsonId.put(IDENTIFIER, id.getIdentifier());
            array.add(jsonId);
        }

        if (array.size() == 0) {
            // list was made of only null objects?
            return null;
        }
        String serialized = array.toString();
        return serialized;
    }
}
