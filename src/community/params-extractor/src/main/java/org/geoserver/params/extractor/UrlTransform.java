/* (c) 2016 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.params.extractor;

import java.net.URLEncoder;
import java.util.*;

public class UrlTransform {

    private final Map<String, String> normalizedNames = new HashMap<>();
    private final Map<String, String[]> parameters = new HashMap<>();
    private final String requestUri;

    private final List<String> replacements = new ArrayList<>();

    public UrlTransform(String requestUri, Map<String, String[]> parameters) {
        this.requestUri = requestUri;
        for (Map.Entry<String, String[]> entry : parameters.entrySet()) {
            normalizedNames.put(entry.getKey().toLowerCase(), entry.getKey());
            this.parameters.put(
                    entry.getKey(), Arrays.copyOf(entry.getValue(), entry.getValue().length));
        }
    }

    public String getOriginalRequestUri() {
        return requestUri;
    }

    public String getRequestUri() {
        String updatedRequestUri = requestUri;
        for (String replacement : replacements) {
            updatedRequestUri = updatedRequestUri.replace(replacement, "");
        }
        return updatedRequestUri;
    }

    public String getQueryString() {
        StringBuilder queryStringBuilder = new StringBuilder();
        for (Map.Entry<String, String[]> parameter : parameters.entrySet()) {
            queryStringBuilder
                    .append(parameter.getKey())
                    .append("=")
                    .append(URLEncoder.encode(parameter.getValue()[0]))
                    .append("&");
        }
        if (queryStringBuilder.length() == 0) {
            return "";
        }
        queryStringBuilder.deleteCharAt(queryStringBuilder.length() - 1);
        return queryStringBuilder.toString();
    }

    public void addParameter(String name, String value, String combine, Boolean repeat) {
        String rawName = getRawName(name);
        String layersRawName = getRawName("layers");
        String[] existingValues = parameters.get(rawName);
        if ((existingValues != null || repeat) && combine != null) {
            int num = 1;
            if (repeat
                    && parameters.containsKey(layersRawName)
                    && parameters.get(layersRawName) != null) {
                num = parameters.get(layersRawName)[0].split(",").length;
            }
            String existingValue = existingValues == null ? null : existingValues[0];
            for (int count = 0; count < num; count++) {
                String combinedValue =
                        existingValue == null ? "$2" : combine.replace("$1", existingValue);
                combinedValue = combinedValue.replace("$2", value);
                existingValue = combinedValue;
            }
            parameters.put(rawName, new String[] {existingValue});
        } else {
            parameters.put(rawName, new String[] {value});
        }
    }

    private String getRawName(String name) {
        String rawName = normalizedNames.get(name.toLowerCase());
        if (rawName != null) {
            return rawName;
        }
        normalizedNames.put(name.toLowerCase(), name);
        return name;
    }

    public void removeMatch(String matchedText) {
        replacements.add(matchedText);
    }

    public Map<String, String[]> getParameters() {
        return parameters;
    }

    public boolean haveChanged() {
        return !(replacements.isEmpty());
    }

    @Override
    public String toString() {
        String updatedQueryString = getQueryString();
        if (updatedQueryString.isEmpty()) {
            return getRequestUri();
        }
        return getRequestUri() + "?" + getQueryString();
    }
}
