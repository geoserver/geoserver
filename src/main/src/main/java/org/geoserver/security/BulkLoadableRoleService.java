/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security;

import java.io.IOException;
import java.util.Map;
import java.util.Properties;

/**
 * Marker interface for {@link GeoServerRoleService} implementations that support bulk-loading of all role properties in
 * a single operation.
 *
 * <p>Role services implementing this interface indicate that they can efficiently load all role properties at once
 * (e.g., via a single SQL query), rather than requiring per-role lookups. This enables
 * {@link org.geoserver.security.impl.RoleCalculator} to use a bulk-load strategy for hierarchy resolution, reducing
 * database round-trips from O(N) to O(1).
 *
 * @see org.geoserver.security.impl.RoleCalculator#addInheritedRoles
 */
public interface BulkLoadableRoleService {

    /**
     * Bulk-loads all role properties from the backend store in a single operation.
     *
     * <p>Returns a map from role name to its {@link Properties} object. Roles with no properties may either be absent
     * from the map or mapped to an empty {@link Properties} instance.
     *
     * @return a map from role name to its properties; never null
     * @throws IOException if a backend error occurs
     */
    Map<String, Properties> getAllRoleProperties() throws IOException;
}
