/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.ows.util;

/**
 * Xerces' <code>getEncodingName()</code> method of <code>org.apache.xerces.impl.XMLEntityManager
 * </code>) returns an array with name of encoding scheme and endianness. I decided to create a
 * separate class incapsulating encoding metadata. The may idea behind this is the fact that we will
 * most probably need to save this info somewhere and use it later when writing the response. Beside
 * that, using class makes related code more clear.
 */
public class EncodingInfo {
    /**
     * This is a name of autodetected <em>encoding scheme</em> (not necessarily <em>charset</em>)
     * which should be used to read XML declaration in order to determine actual data
     * <em>charset</em>.
     */
    private String fEncoding = null;

    /**
     * Contains info about detected byte order (or endian-ness) of the incoming data. <code>true
     * </code> if order is big-endian, <code>false</code> for little-endian, and <code>null</code>
     * if byte order is not relevant for this encoding scheme. This is a "three-state" switch (third
     * is <code>null</code>), so it can't be just plain <code>boolean</code> type.
     */
    private Boolean fIsBigEndian = null;

    /**
     * This is technically not a part of encoding metadata, but more like characteristic of the
     * input XML document. Tells whether Byte Order Mark (BOM) was found while detecting encoding
     * scheme.
     */
    private boolean fHasBOM;

    /**
     * Non-arg constructor to use in a few cases when you need a blank instance of <code>
     * EncodingInfo</code>. It cant' be used right after creation and should be populated first via
     * either setters or specific charset detection methods.
     */
    public EncodingInfo() {}

    /**
     * Constructor that takes name of the encoding scheme and endianness - results of autodetection
     * in <code>getEncodingName</code>. BOM is considered missing if object is constructed this way.
     *
     * @param encoding Name of the autodetected encoding scheme.
     *     <p>Detected byte order of the data. <code>true</code> if order is big-endian, <code>false
     *     </code> if little-endian, and <code>null</code> if byte order is not relevant for this
     *     encoding scheme.
     */
    public EncodingInfo(String encoding, Boolean isBigEndian) {
        fEncoding = encoding;
        fIsBigEndian = isBigEndian;
        fHasBOM = false;
    }

    /**
     * Constructor that takes name of the encoding scheme and endianness - results of autodetection
     * in <code>getEncodingName()</code>. Also presence of Byte Order Mark should be specified
     * explicitly.
     *
     * @param encoding Name of the autodetected encoding scheme.
     *     <p>Detected byte order of the data. <code>true</code> if order is big-endian, <code>false
     *     </code> if little-endian, and <code>null</code> if byte order is not relevant for this
     *     encoding scheme.
     * @param hasBOM <code>true</code> if BOM is present, <code>false</code> otherwise.
     */
    public EncodingInfo(String encoding, Boolean isBigEndian, boolean hasBOM) {
        fEncoding = encoding;
        fIsBigEndian = isBigEndian;
        fHasBOM = hasBOM;
    }

    /** Returns current encoding scheme (or charset). */
    public String getEncoding() {
        return fEncoding;
    }

    /** Sets new value of stored encoding (charset?) name. */
    public void setEncoding(String encoding) {
        fEncoding = encoding;
    }

    /** Accessor for <code>fIsBigEndian</code>. Should we define a mutator too? */
    public Boolean isBigEndian() {
        return fIsBigEndian;
    }

    /** Accessor for <code>fHasBOM</code>. Imho mutator is not required. */
    public boolean hasBOM() {
        return fHasBOM;
    }

    /**
     * Copies property values from another <code>EncodingInfo</code> instance. Strange enough, but
     * sometimes such behavior is preferred to simple assignment or cloning. More specifically, this
     * method is currently used (at least it was :) in <code>
     * XmlCharsetDetector.getCharsetAwareReader</code> (other two ways simply don't work).
     *
     * @param encInfo source object which properties should be mirrored in this instance
     */
    public void copyFrom(EncodingInfo encInfo) {
        fEncoding = encInfo.getEncoding();
        fIsBigEndian = encInfo.isBigEndian();
        fHasBOM = encInfo.hasBOM();
    }

    /** Returns current state of this instance in human-readable form. */
    public String toString() {
        StringBuffer sb = new StringBuffer();
        sb.append((null == fEncoding) ? "[NULL]" : fEncoding);

        if (null != fIsBigEndian) {
            sb.append((fIsBigEndian.booleanValue()) ? " BIG ENDIAN" : " LITTLE ENDIAN");
        }

        if (fHasBOM) {
            sb.append(" with BOM");
        }

        return sb.toString();
    }
} // END class EncodingInfo
