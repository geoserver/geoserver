/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web;

import java.io.Serializable;

/** Label and target for a preview link. */
public record PreviewLink(String label, String href, String title) implements Serializable {}
