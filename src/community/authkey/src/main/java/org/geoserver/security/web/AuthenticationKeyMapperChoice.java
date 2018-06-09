/* (c) 2018 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.web;

import java.util.ArrayList;
import java.util.List;
import org.apache.wicket.markup.html.form.ChoiceRenderer;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.StringResourceModel;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.security.AuthenticationKeyMapper;

/**
 * Drop down choice widget for {@link AuthenticationKeyMapper} configurations.
 *
 * @author mcr
 */
public class AuthenticationKeyMapperChoice extends DropDownChoice<String> {

    /** */
    private static final long serialVersionUID = 1L;

    public AuthenticationKeyMapperChoice(String id) {
        super(
                id,
                new AuthenticationKeyMapperNamesModel(),
                new AuthenticationKeyMapperChoiceRenderer());
    }

    public AuthenticationKeyMapperChoice(String id, IModel<String> model) {
        super(
                id,
                model,
                new AuthenticationKeyMapperNamesModel(),
                new AuthenticationKeyMapperChoiceRenderer());
    }

    static class AuthenticationKeyMapperNamesModel implements IModel<List<String>> {

        private static final long serialVersionUID = 1L;
        List<String> mapperNames;

        AuthenticationKeyMapperNamesModel() {
            List<AuthenticationKeyMapper> mappers =
                    GeoServerExtensions.extensions(AuthenticationKeyMapper.class);
            this.mapperNames = new ArrayList<String>();
            for (AuthenticationKeyMapper mapper : mappers) {
                this.mapperNames.add(mapper.getBeanName());
            }
        }

        AuthenticationKeyMapperNamesModel(List<String> serviceNames) {
            this.mapperNames = serviceNames;
        }

        @Override
        public List<String> getObject() {
            return mapperNames;
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

    static class AuthenticationKeyMapperChoiceRenderer extends ChoiceRenderer<String> {
        private static final long serialVersionUID = 1L;

        @Override
        public Object getDisplayValue(String object) {
            // do a resource lookup
            return new StringResourceModel(
                            AuthenticationKeyFilterPanel.class.getSimpleName() + "." + object)
                    .setParameters(object)
                    .getObject();
        }

        @Override
        public String getIdValue(String object, int index) {
            return object;
        }
    }
}
