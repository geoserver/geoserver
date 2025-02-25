package org.geoserver.smartdataloader.data.store.panel;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.markup.repeater.RefreshingView;
import org.apache.wicket.model.IModel;
import org.geoserver.catalog.DataStoreInfo;

public class SmartOverridesRefreshingView extends RefreshingView<SmartOverrideEntry> {

    private final SmartOverridesModel smartOverridesModel;

    public SmartOverridesRefreshingView(String id, IModel<DataStoreInfo> storeModel) {
        super(id);
        this.smartOverridesModel = new SmartOverridesModel(storeModel);
        this.setOutputMarkupId(true);
    }

    @Override
    protected Iterator<IModel<SmartOverrideEntry>> getItemModels() {
        List<IModel<SmartOverrideEntry>> models = new ArrayList<>();
        for (SmartOverrideEntry entry : smartOverridesModel.getObject()) {
            models.add(new SmartOverrideEntryModel(entry));
        }
        return models.iterator();
    }

    @Override
    protected void populateItem(Item<SmartOverrideEntry> item) {
        item.add(new Label("overridekey", item.getModel().getObject().getKey()));
        item.add(new Label("expression", item.getModel().getObject().getExpression()));
        item.add(new SmarOverrideRemoveLink("removeLink", item.getModel()));
    }

    public class SmarOverrideRemoveLink extends AjaxLink<SmartOverrideEntry> {

        public SmarOverrideRemoveLink(String id, IModel<SmartOverrideEntry> model) {
            super(id, model);
        }

        @Override
        public void onClick(AjaxRequestTarget target) {
            SmartOverrideEntry entry = getModelObject();
            Set<SmartOverrideEntry> smartOverrides = new HashSet<>(smartOverridesModel.getObject());
            smartOverrides.remove(entry);
            smartOverridesModel.setObject(smartOverrides);
            target.add(SmartOverridesRefreshingView.this.getParent());
        }
    }
}
