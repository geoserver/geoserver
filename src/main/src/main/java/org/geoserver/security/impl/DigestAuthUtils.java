/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.security.impl;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.security.crypto.codec.Hex;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * This is an exact copy of org.springframework.security.web.authentication.www.DigestAuthUtils;
 *
 * <p>The Spring class has package visibility, no idea why. The functionality is used for test cases and may be used by
 * a client agent using the geoserver library
 *
 * @author mcr
 */
public class DigestAuthUtils {
    private static final String[] EMPTY_STRING_ARRAY = new String[0];

    public static String encodePasswordInA1Format(String username, String realm, String password) {
        String a1 = username + ":" + realm + ":" + password;
        String a1Md5 = md5Hex(a1);

        return a1Md5;
    }

    public static String[] splitIgnoringQuotes(String str, char separatorChar) {
        if (str == null) {
            return null;
        }

        int len = str.length();

        if (len == 0) {
            return EMPTY_STRING_ARRAY;
        }

        List<String> list = new ArrayList<>();
        int i = 0;
        int start = 0;
        boolean match = false;

        while (i < len) {
            if (str.charAt(i) == '"') {
                i++;
                while (i < len) {
                    if (str.charAt(i) == '"') {
                        i++;
                        break;
                    }
                    i++;
                }
                match = true;
                continue;
            }
            if (str.charAt(i) == separatorChar) {
                if (match) {
                    list.add(str.substring(start, i));
                    match = false;
                }
                start = ++i;
                continue;
            }
            match = true;
            i++;
        }
        if (match) {
            list.add(str.substring(start, i));
        }

        return list.toArray(new String[list.size()]);
    }

    /**
     * Computes the <code>response</code> portion of a Digest authentication header. Both the server and user agent
     * should compute the <code>response</code> independently. Provided as a static method to simplify the coding of
     * user agents.
     *
     * @param passwordAlreadyEncoded true if the password argument is already encoded in the correct format. False if it
     *     is plain text.
     * @param username the user's login name.
     * @param realm the name of the realm.
     * @param password the user's password in plaintext or ready-encoded.
     * @param httpMethod the HTTP request method (GET, POST etc.)
     * @param uri the request URI.
     * @param qop the qop directive, or null if not set.
     * @param nonce the nonce supplied by the server
     * @param nc the "nonce-count" as defined in RFC 2617.
     * @param cnonce opaque string supplied by the client when qop is set.
     * @return the MD5 of the digest authentication response, encoded in hex
     * @throws IllegalArgumentException if the supplied qop value is unsupported.
     */
    public static String generateDigest(
            boolean passwordAlreadyEncoded,
            String username,
            String realm,
            String password,
            String httpMethod,
            String uri,
            String qop,
            String nonce,
            String nc,
            String cnonce)
            throws IllegalArgumentException {
        String a1Md5 = null;
        String a2 = httpMethod + ":" + uri;
        String a2Md5 = md5Hex(a2);

        if (passwordAlreadyEncoded) {
            a1Md5 = password;
        } else {
            a1Md5 = DigestAuthUtils.encodePasswordInA1Format(username, realm, password);
        }

        String digest;

        if (qop == null) {
            // as per RFC 2069 compliant clients (also reaffirmed by RFC 2617)
            digest = a1Md5 + ":" + nonce + ":" + a2Md5;
        } else if ("auth".equals(qop)) {
            // As per RFC 2617 compliant clients
            digest = a1Md5 + ":" + nonce + ":" + nc + ":" + cnonce + ":" + qop + ":" + a2Md5;
        } else {
            throw new IllegalArgumentException("This method does not support a qop: '" + qop + "'");
        }

        return md5Hex(digest);
    }

    /**
     * Takes an array of <code>String</code>s, and for each element removes any instances of <code>
     * removeCharacter</code>, and splits the element based on the <code>delimiter</code>. A <code>
     * Map</code> is then generated, with the left of the delimiter providing the key, and the right of the delimiter
     * providing the value.
     *
     * <p>Will trim both the key and value before adding to the <code>Map</code>.
     *
     * @param array the array to process
     * @param delimiter to split each element using (typically the equals symbol)
     * @param removeCharacters one or more characters to remove from each element prior to attempting the split
     *     operation (typically the quotation mark symbol) or {@code null} if no removal should occur
     * @return a <code>Map</code> representing the array contents, or {@code null} if the array to process was null or
     *     empty
     */
    public static Map<String, String> splitEachArrayElementAndCreateMap(
            String[] array, String delimiter, String removeCharacters) {
        if ((array == null) || (array.length == 0)) {
            return null;
        }

        Map<String, String> map = new HashMap<>();

        for (String s : array) {
            String postRemove;

            if (removeCharacters == null) {
                postRemove = s;
            } else {
                postRemove = StringUtils.replace(s, removeCharacters, "");
            }

            String[] splitThisArrayElement = split(postRemove, delimiter);

            if (splitThisArrayElement == null) {
                continue;
            }

            map.put(splitThisArrayElement[0].trim(), splitThisArrayElement[1].trim());
        }

        return map;
    }

    /**
     * Splits a <code>String</code> at the first instance of the delimiter.
     *
     * <p>Does not include the delimiter in the response.
     *
     * @param toSplit the string to split
     * @param delimiter to split the string up with
     * @return a two element array with index 0 being before the delimiter, and index 1 being after the delimiter
     *     (neither element includes the delimiter)
     * @throws IllegalArgumentException if an argument was invalid
     */
    public static String[] split(String toSplit, String delimiter) {
        Assert.hasLength(toSplit, "Cannot split a null or empty string");
        Assert.hasLength(delimiter, "Cannot use a null or empty delimiter to split a string");

        if (delimiter.length() != 1) {
            throw new IllegalArgumentException("Delimiter can only be one character in length");
        }

        int offset = toSplit.indexOf(delimiter);

        if (offset < 0) {
            return null;
        }

        String beforeDelimiter = toSplit.substring(0, offset);
        String afterDelimiter = toSplit.substring(offset + 1);

        return new String[] {beforeDelimiter, afterDelimiter};
    }

    public static String md5Hex(String data) {
        MessageDigest digest;
        try {
            digest = MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("No MD5 algorithm available!");
        }

        return String.valueOf(Hex.encode(digest.digest(data.getBytes())));
    }
}
