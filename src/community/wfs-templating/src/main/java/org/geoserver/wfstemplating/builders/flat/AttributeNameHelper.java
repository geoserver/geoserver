/* (c) 2020 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wfstemplating.builders.flat;

/** An helper class to help creating attribute names when producing a flat geoJson output */
class AttributeNameHelper {

    private String key;
    private String parentKey;

    static final String PROPERTIES_KEY = "properties";

    AttributeNameHelper() {};

    AttributeNameHelper(String key, String parentKey) {
        this.key = key;
        this.parentKey = parentKey;
    }

    String getFinalAttributeName() {
        String parentKey = this.parentKey;
        String key = this.key;
        if (parentKey != null && !parentKey.equals(PROPERTIES_KEY)) key = parentKey + "." + key;
        return key;
    }

    String getCompleteCompositeAttributeName() {
        String parentKey = this.parentKey;
        String key = this.key;
        if (parentKey != null && !parentKey.equals(PROPERTIES_KEY)) {
            if (key != null && !key.equals(PROPERTIES_KEY)) key = parentKey + "." + key;
            else if (key == null || key.equals(PROPERTIES_KEY)) key = parentKey;
        }
        return key;
    }

    String getCompleteIteratingAttributeName(int elementsSize, int index) {
        String key = this.key;
        String parentKey = this.parentKey;
        if (parentKey != null && !parentKey.equals(PROPERTIES_KEY)) key = parentKey + "." + key;
        String itKey;
        if (elementsSize > 0) itKey = key + "_" + (index + 1);
        else itKey = key;
        return itKey;
    }
}
