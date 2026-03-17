/* (c) 2025 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.services;

import java.util.ArrayList;
import java.util.List;
import org.apache.wicket.WicketRuntimeException;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.model.IModel;
import org.geoserver.web.util.SerializableConsumer;

/** ListVIew used to include AdminPagePanelInfo in service admin page. */
public class AdminPagePanelInfoListView extends ListView<AdminPagePanelInfo> {

    private final IModel infoModel;
    protected List<SerializableConsumer<Void>> onSubmitHooks = new ArrayList<>();

    /**
     * ListView used to generate AdminPagePanels.
     *
     * @param id wicketId
     * @param panels List of panels to generate
     * @param infoModel Shared service admin page model
     * @param onSubmitHooks Shared list of form submit hooks
     */
    public AdminPagePanelInfoListView(
            String id,
            List<AdminPagePanelInfo> panels,
            IModel infoModel,
            List<SerializableConsumer<Void>> onSubmitHooks) {
        super(id, panels);
        this.infoModel = infoModel;
        this.onSubmitHooks = onSubmitHooks;
    }

    @Override
    protected void populateItem(ListItem<AdminPagePanelInfo> item) {
        AdminPagePanelInfo info = item.getModelObject();
        try {
            AdminPagePanel panel = info.getComponentClass()
                    .getConstructor(String.class, IModel.class)
                    .newInstance("content", infoModel);
            item.add(panel);
            // add onMainFormSubmit to hooks
            onSubmitHooks.add(x -> panel.onMainFormSubmit());
        } catch (Exception e) {
            throw new WicketRuntimeException(
                    "Failed to create admin extension panel of "
                            + "type "
                            + info.getComponentClass().getSimpleName(),
                    e);
        }
    }
}
