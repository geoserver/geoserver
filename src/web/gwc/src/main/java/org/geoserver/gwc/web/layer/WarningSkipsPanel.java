/* (c) 2021 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.gwc.web.layer;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Check;
import org.apache.wicket.markup.html.form.CheckGroup;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.geoserver.util.DimensionWarning.WarningType;
import org.geoserver.web.wicket.ParamResourceModel;

public class WarningSkipsPanel extends Panel {

    public WarningSkipsPanel(String id, IModel<Set<WarningType>> warningSkipsModel) {
        super(id);
        final CheckGroup warningSkipsGroup =
                new CheckGroup<>("warningSkipsGroup", warningSkipsModel);
        add(warningSkipsGroup);

        final List<WarningType> allWarningSkips = Arrays.asList(WarningType.values());
        ListView<WarningType> warningSkips =
                new ListView<WarningType>("warningSkips", allWarningSkips) {
                    private static final long serialVersionUID = 1L;

                    @Override
                    protected void populateItem(ListItem<WarningType> item) {
                        item.add(new Check<>("warningSkip", item.getModel()));
                        String key = "warning." + item.getModel().getObject().toString();
                        ParamResourceModel labelModel =
                                new ParamResourceModel(key, WarningSkipsPanel.this);
                        item.add(new Label("warningSkipLabel", labelModel));
                    }
                };
        warningSkips.setReuseItems(true); // otherwise it looses state on invalid form state
        warningSkipsGroup.add(warningSkips);
    }
}
