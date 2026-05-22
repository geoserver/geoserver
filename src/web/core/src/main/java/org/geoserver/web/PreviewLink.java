/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web;

import java.io.Serializable;

/** Label and target for a preview link. */
public record PreviewLink(String label, String href, String title, String catalogLinkType) implements Serializable {

    public static final String METADATA = "metadata";
    public static final String DATA = "data";

    public PreviewLink(String label, String href, String title) {
        this(label, href, title, null);
    }
}
