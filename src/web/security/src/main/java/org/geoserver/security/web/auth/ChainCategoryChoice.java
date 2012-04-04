package org.geoserver.security.web.auth;

import java.util.Arrays;
import java.util.List;

import org.apache.wicket.markup.html.form.CheckBoxMultipleChoice;
import org.apache.wicket.markup.html.form.EnumChoiceRenderer;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.util.ListModel;
import org.geoserver.security.GeoServerSecurityFilterChain.FilterChain;

public class ChainCategoryChoice extends CheckBoxMultipleChoice<FilterChain> {

    public ChainCategoryChoice(String id, IModel<List<FilterChain>> model) {
        super(id, model, new ListModel(Arrays.asList(FilterChain.values())), new EnumChoiceRenderer<FilterChain>());
        setPrefix("<li>");
        setSuffix("</li>");
    }

}
