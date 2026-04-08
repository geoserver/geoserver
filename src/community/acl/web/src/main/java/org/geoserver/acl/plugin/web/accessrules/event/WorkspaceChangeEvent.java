/* (c) 2023 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.acl.plugin.web.accessrules.event;

import lombok.Value;
import org.apache.wicket.ajax.AjaxRequestTarget;

@Value
public class WorkspaceChangeEvent {
    private String workspace;
    private AjaxRequestTarget target;
}
