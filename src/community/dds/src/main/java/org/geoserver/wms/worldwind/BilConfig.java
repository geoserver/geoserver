/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms.worldwind;

/**
 * Constants used for configuration of BIL data layers.
 *
 * @author Parker Abercrombie
 */
public interface BilConfig {

    /**
     * Metadata key for the default data type to return (e.g. "application/bil16") if a request does
     * not specify a data type.
     */
    public static final String DEFAULT_DATA_TYPE = "bil.defaultDataTypeAttribute";

    /** Metadata key for the byte order of BIL response data. */
    public static final String BYTE_ORDER = "bil.byteOrderAttribute";

    /**
     * Metadata key for the "no data" value to use for BIL response data. No data values in the
     * source data will be translated to this value when BIL requests are processed.
     */
    public static final String NO_DATA_OUTPUT = "bil.noDataOutputAttribute";
}
