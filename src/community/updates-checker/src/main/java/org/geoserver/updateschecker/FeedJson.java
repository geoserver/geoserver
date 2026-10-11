/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.updateschecker;

import java.nio.charset.StandardCharsets;
import java.util.Iterator;
import java.util.regex.Pattern;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

/**
 * A parsed JSON value, read with {@code org.json} 20231013, which GeoWebCache brings to every GeoServer from 2.23 to
 * 3.x ({@code com.fasterxml} Jackson is missing from 3.x). It answers only what the feed parser asks: a missing or
 * {@code null} child is {@code null}, and a value is never converted to another type, so a number is not text.
 */
final class FeedJson {

    private final Object value;

    private FeedJson(Object value) {
        this.value = value;
    }

    /** A run of digits this long makes org.json spend seconds turning it into a number. */
    private static final Pattern HUGE_NUMBER = Pattern.compile("\\d{64}");

    /**
     * @return the single value in the body
     * @throws JSONException if the body is not one JSON value, repeats a key in an object, holds a NUL character (which
     *     org.json reads as the end of its input) or holds a huge number
     */
    static FeedJson parse(byte[] body) {
        String text = new String(body, StandardCharsets.UTF_8);
        if (text.indexOf('\0') >= 0 || HUGE_NUMBER.matcher(text).find()) {
            throw new JSONException("Not a feed");
        }
        JSONTokener tokens = new JSONTokener(text);
        Object parsed = tokens.nextValue();
        if (tokens.nextClean() != 0) {
            throw new JSONException("Text after the JSON value");
        }
        return new FeedJson(parsed);
    }

    boolean isObject() {
        return value instanceof JSONObject;
    }

    boolean isArray() {
        return value instanceof JSONArray;
    }

    /** @return whether the value is a number that is whole and fits an {@code int}. */
    boolean isInt() {
        return value instanceof Integer;
    }

    int asInt() {
        return (Integer) value;
    }

    boolean isBoolean() {
        return value instanceof Boolean;
    }

    boolean booleanValue() {
        return (Boolean) value;
    }

    boolean isTextual() {
        return value instanceof String;
    }

    String textValue() {
        return (String) value;
    }

    /** @return the number of elements of an array, 0 for anything else. */
    int size() {
        return value instanceof JSONArray ? ((JSONArray) value).length() : 0;
    }

    /** @return the member of an object, or {@code null} if this is not an object or has no such member. */
    FeedJson get(String name) {
        return value instanceof JSONObject ? wrap(((JSONObject) value).opt(name)) : null;
    }

    /** @return the element of an array, or {@code null} if this is not an array or the index is outside it. */
    FeedJson get(int index) {
        return value instanceof JSONArray ? wrap(((JSONArray) value).opt(index)) : null;
    }

    Iterator<String> fieldNames() {
        return value instanceof JSONObject
                ? ((JSONObject) value).keySet().iterator()
                : java.util.Collections.<String>emptyIterator();
    }

    private static FeedJson wrap(Object child) {
        return child == null || JSONObject.NULL.equals(child) ? null : new FeedJson(child);
    }
}
