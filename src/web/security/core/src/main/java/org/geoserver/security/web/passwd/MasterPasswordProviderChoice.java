/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.web.passwd;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.apache.wicket.markup.html.form.ChoiceRenderer;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.model.IModel;
import org.geoserver.security.MasterPasswordProvider;
import org.geoserver.web.GeoServerApplication;

/**
 * Drop down choice widget for {@link MasterPasswordProvider} configurations.
 *
 * @author Justin Deoliveira, OpenGeo
 */
public class MasterPasswordProviderChoice extends DropDownChoice<String> {

    public MasterPasswordProviderChoice(String id) {
        super(
                id,
                new MasterPasswordProviderNamesModel(),
                new MasterPasswordProviderChoiceRenderer());
    }

    public MasterPasswordProviderChoice(String id, IModel<String> model) {
        super(
                id,
                model,
                new MasterPasswordProviderNamesModel(),
                new MasterPasswordProviderChoiceRenderer());
    }

    static class MasterPasswordProviderNamesModel implements IModel<List<String>> {

        List<String> providerNames;

        MasterPasswordProviderNamesModel() {
            try {
                providerNames =
                        new ArrayList(
                                GeoServerApplication.get()
                                        .getSecurityManager()
                                        .listMasterPasswordProviders());
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        public List<String> getObject() {
            return providerNames;
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

    static class MasterPasswordProviderChoiceRenderer extends ChoiceRenderer<String> {
        @Override
        public Object getDisplayValue(String object) {
            return object;
        }

        @Override
        public String getIdValue(String object, int index) {
            return object;
        }
    }
}
