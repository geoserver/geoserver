/* (c) 2020 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.password;

import java.util.Set;
import org.geoserver.catalog.StoreInfo;

/** An interface returning a Set of custom fields needing Encryption */
public interface EncryptedFieldsProvider {

    /** Return the set of fields to be encrypted for the provided StoreInfo */
    Set<String> getEncryptedFields(StoreInfo info);
}
