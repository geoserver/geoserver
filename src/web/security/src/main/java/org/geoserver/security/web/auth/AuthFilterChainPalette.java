package org.geoserver.security.web.auth;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.wicket.WicketRuntimeException;
import org.apache.wicket.extensions.markup.html.form.palette.Palette;
import org.apache.wicket.markup.html.form.ChoiceRenderer;
import org.apache.wicket.model.IModel;
import org.geoserver.security.filter.GeoServerAuthenticationFilter;
import org.geoserver.web.GeoServerApplication;

public class AuthFilterChainPalette extends Palette<String> {

    public AuthFilterChainPalette(String id) {
        this(id, null, new AvailableAuthFilterNamesModel());
    }
    
    public AuthFilterChainPalette(String id, IModel<List<String>> model) {
        this(id, model, new AvailableAuthFilterNamesModel());
    }
    
    public AuthFilterChainPalette(String id, IModel<List<String>> model, 
        IModel<List<String>> choicesModel) {
        super(id, model, choicesModel, new ChoiceRenderer() {
            @Override
            public String getIdValue(Object object, int index) {
                return (String) getDisplayValue(object);
            }
            @Override
                public Object getDisplayValue(Object object) {
                     return object.toString();
                }
        }, 10, true);
    }

    static class AvailableAuthFilterNamesModel implements IModel<List<String>> {

        @Override
        public List<String> getObject() {
            try {
                return new ArrayList<String>(GeoServerApplication.get().getSecurityManager()
                    .listFilters(GeoServerAuthenticationFilter.class));
            } catch (IOException e) {
                throw new WicketRuntimeException(e);
            }
        }

        @Override
        public void detach() {
            //do nothing
        }

        @Override
        public void setObject(List<String> object) {
            throw new UnsupportedOperationException();
        }
    }
}
