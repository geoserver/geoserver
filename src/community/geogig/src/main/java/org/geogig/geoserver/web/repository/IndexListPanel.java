/* (c) 2016 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geogig.geoserver.web.repository;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import org.apache.wicket.Component;
import org.apache.wicket.extensions.markup.html.repeater.util.SortParam;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.panel.FeedbackPanel;
import org.apache.wicket.markup.repeater.DefaultItemReuseStrategy;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.geoserver.web.wicket.GeoServerDataProvider;
import org.geoserver.web.wicket.GeoServerDataProvider.Property;
import org.geoserver.web.wicket.GeoServerTablePanel;
import org.locationtech.geogig.repository.IndexInfo;
import org.locationtech.geogig.repository.IndexInfo.IndexType;

public class IndexListPanel extends GeoServerTablePanel<IndexInfoEntry> {

    private static final long serialVersionUID = -8379525803311741485L;

    private final IndexInfoProvider provider;

    private final FeedbackPanel pingFeedbackPanel;

    public IndexListPanel(final String id, final List<IndexInfo> indexes) {
        super(id, new IndexInfoProvider(indexes), false);
        super.setFilterable(false);
        this.provider = (IndexInfoProvider) super.getDataProvider();
        this.setOutputMarkupId(true);
        // set the reuse strategy
        this.setItemReuseStrategy(DefaultItemReuseStrategy.getInstance());

        add(pingFeedbackPanel = new FeedbackPanel("feedback"));
        pingFeedbackPanel.setOutputMarkupId(true);
    }

    public void add(IndexInfoEntry indexInfo) {
        this.provider.getItems().add(indexInfo);
    }

    public Iterable<IndexInfoEntry> getIndexInfoEntries() {
        return provider.getItems();
    }

    @Override
    protected Component getComponentForProperty(
            String id, IModel<IndexInfoEntry> itemModel, Property<IndexInfoEntry> property) {

        if (property == IndexInfoProvider.LAYER) {
            String value = (String) IndexInfoProvider.LAYER.getModel(itemModel).getObject();
            Label label = new Label(id, value);
            return label;
        } else if (property == IndexInfoProvider.INDEXED_ATTRIBUTE) {
            String value =
                    (String) IndexInfoProvider.INDEXED_ATTRIBUTE.getModel(itemModel).getObject();
            Label label = new Label(id, value);
            return label;
        } else if (property == IndexInfoProvider.INDEX_TYPE) {
            IndexType value =
                    (IndexType) IndexInfoProvider.INDEX_TYPE.getModel(itemModel).getObject();
            Label label = new Label(id, value.toString());
            return label;
        } else if (property == IndexInfoProvider.EXTRA_ATTRIBUTES) {
            @SuppressWarnings("unchecked")
            List<String> value =
                    (List<String>)
                            IndexInfoProvider.EXTRA_ATTRIBUTES.getModel(itemModel).getObject();
            Label label = new Label(id, value.toString());
            return label;
        }
        return null;
    }

    static class IndexInfoProvider extends GeoServerDataProvider<IndexInfoEntry> {

        private static final long serialVersionUID = -3628151089545613032L;

        static final Property<IndexInfoEntry> LAYER = new BeanProperty<>("layer", "layer");

        static final Property<IndexInfoEntry> INDEXED_ATTRIBUTE =
                new BeanProperty<>("indexedAttribute", "indexedAttribute");

        static final Property<IndexInfoEntry> INDEX_TYPE =
                new BeanProperty<>("indexType", "indexType");

        static final Property<IndexInfoEntry> EXTRA_ATTRIBUTES =
                new BeanProperty<>("extraAttributes", "extraAttributes");

        final List<Property<IndexInfoEntry>> PROPERTIES =
                Arrays.asList(LAYER, INDEXED_ATTRIBUTE, INDEX_TYPE, EXTRA_ATTRIBUTES);

        private final List<IndexInfoEntry> indexInfoEntries;

        public IndexInfoProvider(final List<IndexInfo> indexInfos) {
            this.indexInfoEntries = IndexInfoEntry.fromIndexInfos(indexInfos);
        }

        @Override
        protected List<IndexInfoEntry> getItems() {
            return indexInfoEntries;
        }

        @Override
        protected List<Property<IndexInfoEntry>> getProperties() {
            return PROPERTIES;
        }

        @Override
        protected Comparator<IndexInfoEntry> getComparator(SortParam sort) {
            return super.getComparator(sort);
        }

        @Override
        public IModel<IndexInfoEntry> newModel(IndexInfoEntry object) {
            return new Model<>(object);
        }
    }
}
