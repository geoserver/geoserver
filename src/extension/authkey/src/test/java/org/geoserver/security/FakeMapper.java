/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import org.geoserver.security.impl.GeoServerUser;

/**
 * Mock mapper used for testing purposes.
 *
 * @author Mauro Bartolomeoli
 */
public class FakeMapper extends AbstractAuthenticationKeyMapper {

    @Override
    public GeoServerUser getUser(String key) throws IOException {
        return new GeoServerUser("fakeuser");
    }

    @Override
    public int synchronize() throws IOException {
        return 0;
    }

    @Override
    public boolean supportsReadOnlyUserGroupService() {
        return false;
    }

    public String getMapperParameter(String parameter) {
        return super.getMapperConfiguration().get(parameter);
    }

    @Override
    public Set<String> getAvailableParameters() {
        return new HashSet(Arrays.asList("param1", "param2"));
    }
}
