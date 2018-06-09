/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.web.auth;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.apache.wicket.Component;
import org.apache.wicket.WicketRuntimeException;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.extensions.markup.html.form.palette.Palette;
import org.apache.wicket.extensions.markup.html.form.palette.component.Recorder;
import org.apache.wicket.extensions.markup.html.form.palette.theme.DefaultTheme;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.ChoiceRenderer;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.ResourceModel;
import org.geoserver.security.VariableFilterChain;
import org.geoserver.web.GeoServerApplication;

public class AuthFilterChainPalette extends Palette<String> {

    AvailableAuthFilterNamesModel choicesModel;

    public AuthFilterChainPalette(String id) {
        this(id, null, new AvailableAuthFilterNamesModel());
    }

    public AuthFilterChainPalette(String id, IModel<List<String>> model) {
        this(id, model, new AvailableAuthFilterNamesModel());
    }

    public AuthFilterChainPalette(
            String id, IModel<List<String>> model, IModel<List<String>> choicesModel) {
        super(
                id,
                model,
                choicesModel,
                new ChoiceRenderer() {
                    @Override
                    public String getIdValue(Object object, int index) {
                        return (String) getDisplayValue(object);
                    }

                    @Override
                    public Object getDisplayValue(Object object) {
                        return object.toString();
                    }
                },
                10,
                true);
        this.choicesModel = (AvailableAuthFilterNamesModel) choicesModel;
        add(new DefaultTheme());
    }

    public void setChain(VariableFilterChain chain) {
        choicesModel.chain = chain;
    }

    static class AvailableAuthFilterNamesModel implements IModel<List<String>> {

        VariableFilterChain chain;

        @Override
        public List<String> getObject() {
            List<String> result = new ArrayList<String>();
            try {
                result.addAll(
                        chain.listFilterCandidates(
                                GeoServerApplication.get().getSecurityManager()));
            } catch (IOException e) {
                throw new WicketRuntimeException(e);
            }
            return result;
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

    @Override
    protected Recorder newRecorderComponent() {
        Recorder recorder = super.newRecorderComponent();
        recorder.add(
                new AjaxFormComponentUpdatingBehavior("change") {
                    private static final long serialVersionUID = 1L;

                    @Override
                    protected void onUpdate(AjaxRequestTarget target) {}
                });
        return recorder;
    }

    /** Override otherwise the header is not i18n'ized */
    @Override
    public Component newSelectedHeader(final String componentId) {
        return new Label(componentId, new ResourceModel("AuthFilterChainPalette.selectedHeader"));
    }

    /** Override otherwise the header is not i18n'ized */
    @Override
    public Component newAvailableHeader(final String componentId) {
        return new Label(componentId, new ResourceModel("AuthFilterChainPalette.availableHeader"));
    }
}
