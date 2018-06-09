/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.config;

import java.io.File;
import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.hamcrest.Factory;
import org.hamcrest.Matcher;

public class FileExistsMatcher extends BaseMatcher<File> {

    /**
     * Creates a matcher that matches files that exist
     *
     * @param target the target instance against which others should be assessed
     */
    @Factory
    public static Matcher<File> fileExists() {
        return new FileExistsMatcher();
    }

    @Override
    public boolean matches(Object item) {
        return ((File) item).exists();
    }

    @Override
    public void describeTo(Description description) {
        description.appendText("file that exists");
    }
}
