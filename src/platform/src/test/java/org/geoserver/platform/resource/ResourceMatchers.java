/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.platform.resource;

import org.hamcrest.Matcher;

public class ResourceMatchers {
    public static Matcher<Resource> defined() {
        return new ResourceDefined();
    }

    public static Matcher<Resource> undefined() {
        return new ResourceUndefined();
    }

    public static Matcher<Resource> resource() {
        return new ResourceIsLeaf();
    }

    public static Matcher<Resource> directory() {
        return new ResourceIsDirectory();
    }

    public static Matcher<Resource> hasContent(byte[] content) {
        return new ResourceHasContents(content);
    }
}
