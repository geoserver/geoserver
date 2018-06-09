/* Copyright (c) 2014 OpenPlans - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.platform.resource;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;

public class ResourceHasContents extends BaseMatcher<Resource> {
    final byte[] contents;

    public ResourceHasContents(byte[] contents) {
        super();
        this.contents = contents;
    }

    @Override
    public boolean matches(Object item) {
        if (item instanceof Resource) {
            try (InputStream in = ((Resource) item).in()) {
                byte[] result = new byte[contents.length];
                int len = in.read(result);
                if (len != contents.length) {
                    return false;
                }
                if (in.read() != -1) {
                    return false;
                }
                return Arrays.equals(contents, result);
            } catch (IOException ex) {
                throw new IllegalStateException("Exception while reading resource contents", ex);
            }
        }
        return false;
    }

    @Override
    public void describeTo(Description description) {
        description.appendText("resource that contains: " + Arrays.toString(contents));
    }
}
