package org.geoserver.gss.web;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Component;
import org.apache.wicket.ResourceReference;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.ResourceModel;
import org.geoserver.gss.impl.GSS;
import org.geoserver.gss.internal.atom.EntryImpl;
import org.geoserver.gss.internal.atom.FeedImpl;
import org.geoserver.web.wicket.GeoServerDataProvider;
import org.geoserver.web.wicket.GeoServerDataProvider.BeanProperty;
import org.geoserver.web.wicket.GeoServerDataProvider.Property;
import org.geoserver.web.wicket.GeoServerDataProvider.PropertyPlaceholder;
import org.geoserver.web.wicket.GeoServerTablePanel;
import org.geoserver.web.wicket.ImageExternalLink;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.feature.type.DateUtil;
import org.opengis.filter.Filter;
import org.opengis.filter.FilterFactory2;
import org.opengis.filter.sort.SortOrder;

import com.google.common.collect.Iterators;

public class ChangesPanel extends Panel {

    private static final long serialVersionUID = 1L;

    public ChangesPanel(String id) {
        super(id);
        ChangesProvider provider = new ChangesProvider();
        ChangesTablePanel table = new ChangesTablePanel("table", provider);
        add(table);
    }

    private static class ChangesProvider extends GeoServerDataProvider<EntryImpl> {

        private static final FilterFactory2 filterFactory = CommonFactoryFinder
                .getFilterFactory2(null);

        private static final long serialVersionUID = 1L;

        @SuppressWarnings("serial")
        @Override
        protected IModel<EntryImpl> newModel(Object object) {
            EntryImpl e = (EntryImpl) object;
            final String commitId = e.getId();
            return new LoadableDetachableModel<EntryImpl>() {
                @Override
                protected EntryImpl load() {
                    GSS gss = GSS.get();
                    List<String> searchTerms = null;
                    Filter filter = filterFactory.id(Collections.singleton(filterFactory
                            .featureId(commitId)));
                    Long startPosition = null;
                    Long maxEntries = null;
                    FeedImpl feed = gss.queryResolutionFeed(searchTerms, filter, startPosition,
                            maxEntries, SortOrder.ASCENDING);
                    Iterator<EntryImpl> iterator = feed.getEntry();
                    if (iterator.hasNext()) {
                        return iterator.next();
                    }
                    throw new IllegalStateException("can't find entry '" + commitId + "'");
                }
            };
        }

        @SuppressWarnings("unchecked")
        @Override
        protected List<Property<EntryImpl>> getProperties() {
            return Arrays.asList(ChangesTablePanel.AUTHOR, ChangesTablePanel.TITLE,
                    ChangesTablePanel.UPDATED, ChangesTablePanel.FEED);
        }

        @Override
        protected List<EntryImpl> getItems() {
            final GSS gss = GSS.get();
            FeedImpl latestChanges;

            List<String> searchTerms = null;
            Filter filter = Filter.INCLUDE;
            Long startPosition = null;
            Long maxEntries = null;
            latestChanges = gss.queryResolutionFeed(searchTerms, filter, startPosition, maxEntries,
                    SortOrder.ASCENDING);

            ArrayList<EntryImpl> list = new ArrayList<EntryImpl>();
            Iterators.addAll(list, latestChanges.getEntry());

            return list;
        }
    }

    private static class ChangesTablePanel extends GeoServerTablePanel<EntryImpl> {

        private static final long serialVersionUID = 1L;

        public static final Property<EntryImpl> TITLE = new BeanProperty<EntryImpl>("title",
                "title");

        public static final Property<EntryImpl> AUTHOR = new BeanProperty<EntryImpl>("author",
                "author");

        public static final Property<EntryImpl> UPDATED = new BeanProperty<EntryImpl>("updated",
                "updated");

        public static final Property<EntryImpl> FEED = new PropertyPlaceholder<EntryImpl>("feed");

        /**
         * @param id
         * @param dataProvider
         */
        public ChangesTablePanel(final String id,
                final GeoServerDataProvider<EntryImpl> dataProvider) {
            super(id, dataProvider);
            setFilterable(false);
            setItemsPerPage(10);
        }

        @Override
        protected Component getComponentForProperty(final String id, final IModel itemModel,
                final Property<EntryImpl> property) {

            final EntryImpl item = (EntryImpl) itemModel.getObject();
            if (TITLE.equals(property)) {
                return new Label(id, item.getTitle());
            } else if (AUTHOR.equals(property)) {
                return new Label(id, item.getAuthor().get(0).getName());
            } else if (UPDATED.equals(property)) {
                return new Label(id, DateUtil.serializeDateTime(item.getUpdated().getTime(), true));
            } else if (FEED.equals(property)) {
                return feedLink(id, item);
            }

            throw new IllegalArgumentException("Unknown property: " + property);
        }

        private Component feedLink(final String id, final EntryImpl item) {
            String href = "../ows?service=GSS&version=1.0.0&request=GetEntries&feed=REPLICATIONFEED&outputFormat=text/xml&";
            href += "temporalOp=TEquals&startTime="
                    + DateUtil.serializeDateTime(item.getUpdated().getTime(), true)
                    + "&startPosition=1&maxEntries=50";

            ResourceReference feedIcon = new ResourceReference(ChangesPanel.class,
                    "feed-icon-14x14.png");

            ImageExternalLink link = new ImageExternalLink(id, href, feedIcon,
                    new Model<String>(""));

            link.add(new AttributeModifier("title", true, new ResourceModel(
                    "ChangesPanel.feedTitle")));

            // link.getLink().setPopupSettings(new PopupSettings());

            return link;
        }
    }
}
