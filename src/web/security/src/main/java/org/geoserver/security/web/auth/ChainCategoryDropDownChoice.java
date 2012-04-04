package org.geoserver.security.web.auth;

import java.util.Arrays;

import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.EnumChoiceRenderer;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.util.ListModel;
import org.geoserver.security.GeoServerSecurityFilterChain.FilterChain;

public class ChainCategoryDropDownChoice extends DropDownChoice<FilterChain> {

    public ChainCategoryDropDownChoice(String id) {
        super(id, new ListModel<FilterChain>(Arrays.asList(FilterChain.values())), 
            new EnumChoiceRenderer<FilterChain>());
    }

    public ChainCategoryDropDownChoice(String id, IModel model) {
        super(id, model, new ListModel<FilterChain>(Arrays.asList(FilterChain.values())), 
            new EnumChoiceRenderer<FilterChain>());
    }

}
