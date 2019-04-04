/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.web.passwd;

import java.util.ArrayList;
import java.util.List;
import org.apache.wicket.markup.html.form.ChoiceRenderer;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.ResourceModel;
import org.geoserver.security.password.GeoServerPasswordEncoder;
import org.geoserver.web.GeoServerApplication;

/**
 * Drop down choice widget for {@link GeoServerPasswordEncoder} configurations.
 *
 * @author Justin Deoliveira, OpenGeo
 */
public class PasswordEncoderChoice extends DropDownChoice<String> {

    public PasswordEncoderChoice(String id) {
        super(id, new PasswordEncoderNamesModel(), new PasswordEncoderChoiceRenderer());
    }

    public PasswordEncoderChoice(String id, IModel<String> model) {
        super(id, model, new PasswordEncoderNamesModel(), new PasswordEncoderChoiceRenderer());
    }

    public PasswordEncoderChoice(String id, List<GeoServerPasswordEncoder> encoders) {
        super(id, new PasswordEncoderNamesModel(encoders), new PasswordEncoderChoiceRenderer());
    }

    public PasswordEncoderChoice(
            String id, IModel<String> model, List<GeoServerPasswordEncoder> encoders) {
        super(
                id,
                model,
                new PasswordEncoderNamesModel(encoders),
                new PasswordEncoderChoiceRenderer());
    }

    static class PasswordEncoderNamesModel implements IModel<List<String>> {

        List<String> encoderNames;

        PasswordEncoderNamesModel() {
            this(GeoServerApplication.get().getSecurityManager().loadPasswordEncoders());
        }

        PasswordEncoderNamesModel(List<GeoServerPasswordEncoder> encoders) {
            encoderNames = new ArrayList<String>();
            for (GeoServerPasswordEncoder pe : encoders) {
                encoderNames.add(pe.getName());
            }
        }

        @Override
        public List<String> getObject() {
            return encoderNames;
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

    static class PasswordEncoderChoiceRenderer extends ChoiceRenderer<String> {
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
