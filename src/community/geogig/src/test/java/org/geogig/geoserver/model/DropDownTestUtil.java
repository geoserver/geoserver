/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geogig.geoserver.model;

import java.util.ArrayList;
import java.util.List;

/** Test Utility for altering available repository backend choices in the GeoServer UI drop down. */
public class DropDownTestUtil {

    // get the runtime config so it can be restored after tests run when altering them.
    private static final ArrayList<String> DEFAULT_LIST = new ArrayList<>(2);
    private static final String DEFAULT_CONFIG = DropDownModel.DIRECTORY_CONFIG;

    static {
        DEFAULT_LIST.add(DropDownModel.DIRECTORY_CONFIG);
        DEFAULT_LIST.add(DropDownModel.PG_CONFIG);
    }

    /**
     * Allows for overriding the list of available backends for testing purposes, as well as the
     * default backend.
     *
     * @param backends List of available backends.
     * @param defaultBackend The default backend.
     */
    public static void setAvailableBackends(List<String> backends, String defaultBackend) {
        DropDownModel.setConfigList(backends, defaultBackend);
    }

    /** Resets the list of available backends, based on what is available at runtime. */
    public static void resetAvailableBackends() {
        DropDownModel.setConfigList(DEFAULT_LIST, DEFAULT_CONFIG);
    }
}
