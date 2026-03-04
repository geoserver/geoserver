package org.geoserver.taskmanager.web.panel;

import java.io.Serial;
import java.util.List;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;

public abstract class PanelListPanel<T> extends Panel {

    private static final boolean isCssEmpty = org.geoserver.web.util.WebUtils.IsWicketCssFileEmpty(
            java.lang.invoke.MethodHandles.lookup().lookupClass());

    @Override
    public void renderHead(org.apache.wicket.markup.head.IHeaderResponse response) {
        super.renderHead(response);
        // if the panel-specific CSS file contains actual css then have the browser load the css
        if (!isCssEmpty) {
            response.render(org.apache.wicket.markup.head.CssHeaderItem.forReference(
                    new org.apache.wicket.request.resource.PackageResourceReference(
                            getClass(), getClass().getSimpleName() + ".css")));
        }
    }

    @Serial
    private static final long serialVersionUID = -7299876582725984906L;

    public PanelListPanel(final String id, final List<T> list) {
        super(id);
        add(new ListView<T>("listview", list) {
            @Serial
            private static final long serialVersionUID = -4770841274788269473L;

            @Override
            protected void populateItem(ListItem<T> item) {
                item.add(PanelListPanel.this.populateItem("panel", item.getModel()));
            }
        });
    }

    protected abstract Panel populateItem(String id, IModel<T> item);
}
