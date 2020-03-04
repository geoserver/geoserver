/* (c) 2016 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.resources;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;
import org.apache.wicket.model.IModel;
import org.geoserver.platform.resource.Resource;
import org.geoserver.platform.resource.ResourceListener;
import org.geoserver.platform.resource.ResourceNotification;
import org.geoserver.platform.resource.ResourceNotification.Event;

/**
 * Model with information about which nodes are expanded and which aren't. By keeping a single
 * object for this information, there can be several instances of the same node that are consistent
 * in their expanded state.
 *
 * @author Niels Charlier
 */
public class ResourceExpandedStates implements Serializable {

    private static final long serialVersionUID = 8635581624445593893L;

    protected Set<String> expanded = new HashSet<String>();

    public IModel<Boolean> getResourceExpandedState(Resource res) {
        return new ResourceExpandedState(res);
    }

    /** The model for a single resource node */
    protected class ResourceExpandedState implements IModel<Boolean>, ResourceListener {

        private static final long serialVersionUID = 4995246395674902150L;

        protected Resource resource;

        public ResourceExpandedState(Resource resource) {
            this.resource = resource;
        }

        @Override
        public void detach() {}

        @Override
        public Boolean getObject() {
            return expanded.contains(resource.path());
        }

        @Override
        public void setObject(Boolean object) {
            if (object != null && object.booleanValue()) {
                expanded.add(resource.path());
                if (resource.parent() != null) {
                    resource.parent().addListener(this);
                }
            } else {
                expanded.remove(resource.path());
                if (resource.parent() != null) {
                    resource.parent().removeListener(this);
                }
            }
        }

        @Override
        public void changed(ResourceNotification notify) {
            for (Event event : notify.events()) {
                if (event.getKind() == ResourceNotification.Kind.ENTRY_DELETE
                        && event.getPath().equals(resource.name())) {
                    // clean up deleted resources
                    expanded.remove(resource.path());
                }
            }
        }
    }
}
