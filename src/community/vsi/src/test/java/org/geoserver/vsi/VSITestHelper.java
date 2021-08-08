/* (c) 2021 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.vsi;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.File;
import java.nio.file.Paths;
import org.geoserver.catalog.StoreInfo;
import org.geoserver.catalog.WorkspaceInfo;

/**
 * Helpers for test classes
 *
 * @author Matthew Northcott <matthewnorthcott@catalyst.net.nz>
 */
public class VSITestHelper {

    // General params
    public static final String HOSTNAME = "my.test.cloud.provider.com";
    public static final String PROJECT_ID = "9acd84d2e7d74a5cb8df15144c3ee2f7";
    public static final String PROJECT_NAME = "my.test.project";
    public static final String CONTAINER_NAME = "my_container";
    public static final String TIFF_FILE_NAME = "file.tif";
    public static final String TIFF_FILE_PATH = "path/to/" + TIFF_FILE_NAME;
    public static final String ZIP_FILE_NAME = "file.zip";
    public static final String ZIP_FILE_PATH = "path/to/" + ZIP_FILE_NAME;

    // Locations
    public static final String TIFF_LOCATION =
            String.format("/vsiswift/%s/%s", CONTAINER_NAME, TIFF_FILE_PATH);
    public static final String ZIP_LOCATION =
            String.format("/vsizip//vsiswift/%s/%s", CONTAINER_NAME, ZIP_FILE_PATH);
    public static final String INVALID_LOCATION = "INVALID LOCATION";

    // Store info
    public static final String WORKSPACE_NAME = "testWorkspace";
    public static final String STORE_NAME = "testStoreName";

    // Authentication credentials
    public static final String AUTHENTICATION_URL = "https://my.test.cloud.provider.com:5000/v3";
    public static final String USERNAME = "user@example.com";
    public static final String PASSWORD = "hunter2";

    public static final String PROPERTIES_EMPTY = "empty.properties";
    public static final String PROPERTIES_VALID = "valid.properties";
    public static final String PROPERTIES_NO_EXIST =
            Paths.get(".", "path", "that", "does", "not", "exist.properties").toString();

    public void setVSIPropertiesToResource(String resource) {
        final File file = new File(getClass().getClassLoader().getResource(resource).getFile());

        if (file.exists()) {
            System.setProperty(VSIProperties.LOCATION_PROPERTY, file.getAbsolutePath());
        }
    }

    /**
     * Mock the StoreInfo interface and configure it to return the test workspace name and store
     * name
     *
     * @return Mocked StoreInfo interface
     */
    public StoreInfo mockStoreInfo() {
        final StoreInfo storeInfo = mock(StoreInfo.class);
        final WorkspaceInfo workspaceInfo = mock(WorkspaceInfo.class);

        when(workspaceInfo.getName()).thenReturn(WORKSPACE_NAME);
        when(storeInfo.getWorkspace()).thenReturn(workspaceInfo);
        when(storeInfo.getName()).thenReturn(STORE_NAME);

        return storeInfo;
    }
}
