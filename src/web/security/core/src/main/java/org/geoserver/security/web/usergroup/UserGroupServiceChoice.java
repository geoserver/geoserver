/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.web.usergroup;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.apache.wicket.WicketRuntimeException;
import org.apache.wicket.markup.html.form.ChoiceRenderer;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.ResourceModel;
import org.geoserver.security.GeoServerUserGroupService;
import org.geoserver.web.GeoServerApplication;

/**
 * Drop down choice widget for {@link GeoServerUserGroupService} configurations.
 *
 * @author Justin Deoliveira, OpenGeo
 */
public class UserGroupServiceChoice extends DropDownChoice<String> {

    public UserGroupServiceChoice(String id) {
        super(id, new UserGroupServiceNamesModel(), new UserGroupServiceChoiceRenderer());
    }

    public UserGroupServiceChoice(String id, IModel<String> model) {
        super(id, model, new UserGroupServiceNamesModel(), new UserGroupServiceChoiceRenderer());
    }

    static class UserGroupServiceNamesModel implements IModel<List<String>> {

        List<String> serviceNames;

        UserGroupServiceNamesModel() {
            try {
                this.serviceNames =
                        new ArrayList(
                                GeoServerApplication.get()
                                        .getSecurityManager()
                                        .listUserGroupServices());
            } catch (IOException e) {
                throw new WicketRuntimeException(e);
            }
        }

        UserGroupServiceNamesModel(List<String> serviceNames) {
            this.serviceNames = serviceNames;
        }

        @Override
        public List<String> getObject() {
            return serviceNames;
        }

        @Override
        public void detach() {
            // do nothing
        }

        @Override
        public void setObject(List<String> object) {
            throw new UnsupportedOperationException();
        }
    }

    static class UserGroupServiceChoiceRenderer extends ChoiceRenderer<String> {
        @Override
        public Object getDisplayValue(String object) {
            // do a resource lookup
            return new ResourceModel(object, object).getObject();
        }

        @Override
        public String getIdValue(String object, int index) {
            return object;
        }
    }
}
