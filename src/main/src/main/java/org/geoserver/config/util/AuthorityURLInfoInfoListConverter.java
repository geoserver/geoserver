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
import org.geoserver.catalog.AuthorityURLInfo;
import org.geoserver.catalog.MetadataMap;
import org.geoserver.catalog.impl.AuthorityURL;

/**
 * Utility class to serialize and deserialize a list of {@link AuthorityURLInfo} objects to and from
 * String using a JSON array representation as serialized form so that {@link XStreamPersister}
 * stores it under a single key in a catalog info's {@link MetadataMap}.
 *
 * @author groldan
 */
public class AuthorityURLInfoInfoListConverter {

    private static final String NAME = "name";

    private static final String HREF = "href";

    /**
     * @param str a JSON array representation of a list of {@link AuthorityURLInfo} objects
     * @return the list of parsed authrority URL from the argument JSON array
     * @throws IllegalArgumentException if {@code str} can't be parsed to a JSONArray
     */
    public static List<AuthorityURLInfo> fromString(String str) throws IllegalArgumentException {

        try {
            final JSONArray array;
            array = JSONArray.fromObject(str);
            final int size = array.size();
            List<AuthorityURLInfo> list = new ArrayList<AuthorityURLInfo>(size);
            JSONObject jsonAuth;
            for (int i = 0; i < size; i++) {
                jsonAuth = array.getJSONObject(i);
                AuthorityURL auth = new AuthorityURL();
                auth.setName(jsonAuth.getString(NAME));
                auth.setHref(jsonAuth.getString(HREF));
                list.add(auth);
            }
            return list;
        } catch (JSONException e) {
            throw new IllegalArgumentException(e.getMessage(), e);
        }
    }

    /**
     * @param obj the list of layer identifiers to serialize
     * @return {@code null} if {@code list} is null, empty or contains only null objects; the JSON
     *     array representation of {@code list} otherwise, with any null element stripped off.
     */
    public static String toString(List<AuthorityURLInfo> obj) {
        if (obj == null || obj.isEmpty()) {
            return null;
        }
        JSONArray array = new JSONArray();

        for (AuthorityURLInfo auth : obj) {
            if (auth == null) {
                continue;
            }
            JSONObject jsonAuth = new JSONObject();
            jsonAuth.put(NAME, auth.getName());
            jsonAuth.put(HREF, auth.getHref());
            array.add(jsonAuth);
        }

        if (array.size() == 0) {
            // list was made of only null objects?
            return null;
        }

        String serialized = array.toString();
        return serialized;
    }
}
