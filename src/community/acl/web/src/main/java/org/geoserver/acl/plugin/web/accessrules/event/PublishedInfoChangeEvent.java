/* (c) 2023 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.acl.plugin.web.accessrules.event;

import java.util.Optional;
import lombok.Value;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.geoserver.catalog.PublishedInfo;

@Value
public class PublishedInfoChangeEvent {
    private String workspace;
    private String layer;
    Optional<PublishedInfo> info;
    private AjaxRequestTarget target;
}
