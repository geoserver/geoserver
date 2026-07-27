/* (c) 2023 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.acl.plugin.web.accessrules.event;

import jakarta.annotation.Nullable;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.geoserver.catalog.PublishedInfo;

public record PublishedInfoChangeEvent(
        String workspace, String layer, @Nullable PublishedInfo info, AjaxRequestTarget target) {}
