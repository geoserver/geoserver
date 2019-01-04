package org.geoserver.taskmanager.web.panel;

import java.util.List;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;

public abstract class PanelListPanel<T> extends Panel {

    private static final long serialVersionUID = -7299876582725984906L;

    public PanelListPanel(final String id, final List<T> list) {
        super(id);
        add(
                new ListView<T>("listview", list) {
                    private static final long serialVersionUID = -4770841274788269473L;

                    protected void populateItem(ListItem<T> item) {
                        item.add(PanelListPanel.this.populateItem("panel", item.getModel()));
                    }
                });
    }

    protected abstract Panel populateItem(String id, IModel<T> item);
}
