/* (c) 2019 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.metadata.data.service;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

public interface MetaDataBulkService {

    default void clearAll(boolean templatesToo) throws IOException {
        clearAll(templatesToo, null);
    }

    default void fixAll() {
        fixAll(null);
    }

    default boolean importAndLink(String geonetwork, String csvFile) {
        return importAndLink(geonetwork, csvFile, null);
    }

    default boolean nativeToCustom(List<Integer> indexes, String csvFile) {
        return nativeToCustom(indexes, csvFile, null);
    }

    default void nativeToCustom(List<Integer> indexes) {
        nativeToCustom(indexes, (UUID) null);
    }

    void clearAll(boolean templatesToo, UUID progressKey) throws IOException;

    void fixAll(UUID progressKey);

    boolean importAndLink(String geonetwork, String csvData, UUID key);

    boolean nativeToCustom(List<Integer> indexes, String csvData, UUID key);

    void nativeToCustom(List<Integer> indexes, UUID key);
}
