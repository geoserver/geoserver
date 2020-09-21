/* (c) 2020 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.cog;

import java.io.Serializable;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import org.geoserver.catalog.CoverageStoreInfo;
import org.geoserver.catalog.StoreInfo;
import org.geoserver.security.password.EncryptedFieldsProvider;

/** EncryptedFieldsProvider for COG stores containing a password in the connectionParameters */
class CogEncryptedFieldsProvider implements EncryptedFieldsProvider {

    public CogEncryptedFieldsProvider() {}

    private static final String COG_FORMAT_TYPE = "GeoTIFF";

    private static final String COG_PASSWORD_FIELD = "password";

    @Override
    public Set<String> getEncryptedFields(StoreInfo info) {
        if (info instanceof CoverageStoreInfo) {
            CoverageStoreInfo storeInfo = (CoverageStoreInfo) info;
            String storeType = storeInfo.getType();
            if (COG_FORMAT_TYPE.equalsIgnoreCase(storeType)) {
                Map<String, Serializable> connectionParams = info.getConnectionParameters();
                if (connectionParams != null && connectionParams.containsKey(COG_PASSWORD_FIELD)) {
                    return Collections.singleton(COG_PASSWORD_FIELD);
                }
            }
        }
        return Collections.emptySet();
    }
}
