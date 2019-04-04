/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2014 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.platform.resource;

import org.geoserver.platform.resource.Resource.Type;
import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;

public class ResourceIsDirectory extends BaseMatcher<Resource> {

    @Override
    public boolean matches(Object item) {
        if (item instanceof Resource) {
            Resource res = (Resource) item;
            return res.getType() == Type.DIRECTORY;
        }
        return false;
    }

    @Override
    public void describeTo(Description description) {
        description.appendText("directory resource");
    }
}
