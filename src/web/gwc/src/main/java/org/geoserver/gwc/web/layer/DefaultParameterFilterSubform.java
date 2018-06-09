/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.gwc.web.layer;

import java.util.List;
import org.apache.wicket.Component;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;
import org.geowebcache.filter.parameters.ParameterFilter;

/**
 * Subform that displays basic information about a ParameterFilter
 *
 * @author Kevin Smith, OpenGeo
 */
public class DefaultParameterFilterSubform extends AbstractParameterFilterSubform<ParameterFilter> {

    private static final long serialVersionUID = 4827404723366519890L;

    public DefaultParameterFilterSubform(String id, IModel<ParameterFilter> model) {
        super(id, model);

        final Component defaultValue;

        defaultValue = new Label("defaultValue", new PropertyModel<String>(model, "defaultValue"));
        add(defaultValue);

        final Component legalValueList;

        legalValueList =
                new ListView<String>(
                        "legalValueList", new PropertyModel<List<String>>(model, "legalValues")) {

                    /** serialVersionUID */
                    private static final long serialVersionUID = 1L;

                    @Override
                    protected void populateItem(ListItem<String> item) {
                        item.add(new Label("legalValue", item.getModel()));
                    }
                };

        add(legalValueList);
    }
}
