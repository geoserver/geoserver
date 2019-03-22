/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms.web.data;

import java.io.IOException;
import java.util.logging.Logger;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.panel.EmptyPanel;
import org.apache.wicket.markup.html.panel.Fragment;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.web.GeoServerApplication;
import org.geoserver.web.wicket.GeoServerDataProvider.Property;
import org.geoserver.web.wicket.GeoServerTablePanel;
import org.geotools.data.FeatureSource;
import org.geotools.data.Query;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.FeatureIterator;
import org.geotools.feature.visitor.MaxVisitor;
import org.geotools.feature.visitor.MinVisitor;
import org.geotools.util.Converters;
import org.geotools.util.logging.Logging;
import org.locationtech.jts.geom.Geometry;
import org.opengis.feature.Feature;
import org.opengis.feature.type.PropertyDescriptor;

/** Panel for listing sample attributes of a FeatureTypeInfo resource. */
public class DataPanel extends Panel {
    private static final long serialVersionUID = -2635691554700860434L;

    static final Logger LOGGER = Logging.getLogger(DataPanel.class);

    String featureTypeId;

    public DataPanel(String id, FeatureTypeInfo ft) {
        super(id, new Model<FeatureTypeInfo>(ft));
        this.featureTypeId = ft.getId();

        add(
                new Label(
                        "summary-message",
                        "For reference, here is a listing of the attributes in this data set.")); // TODO: I18N
        final WebMarkupContainer attsContainer = new WebMarkupContainer("attributes-container");
        attsContainer.setOutputMarkupId(true);
        add(attsContainer);

        Feature sample;
        try {
            sample = getSampleFeature(ft);
        } catch (Exception e) {
            attsContainer.error(
                    "Failed to load attribute list, internal error is: " + e.getMessage());
            attsContainer.add(new EmptyPanel("attributes"));
            return;
        }
        DataAttributesProvider summaries = new DataAttributesProvider(sample);

        final GeoServerTablePanel<DataAttribute> attributes =
                new GeoServerTablePanel<DataAttribute>("attributes", summaries) {

                    private static final long serialVersionUID = 7753093373969576568L;

                    @Override
                    protected Component getComponentForProperty(
                            String id,
                            final IModel<DataAttribute> itemModel,
                            Property<DataAttribute> property) {
                        if (DataAttributesProvider.COMPUTE_STATS.equals(property.getName())) {
                            Fragment f = new Fragment(id, "computeStatsFragment", DataPanel.this);
                            f.add(
                                    new AjaxLink<Void>("computeStats") {

                                        private static final long serialVersionUID = 1L;

                                        @Override
                                        public void onClick(AjaxRequestTarget target) {
                                            DataAttribute attribute =
                                                    (DataAttribute) itemModel.getObject();
                                            try {
                                                updateAttributeStats(attribute);
                                            } catch (IOException e) {
                                                error(
                                                        "Failed to compute stats for the attribute: "
                                                                + e.getMessage());
                                            }
                                            target.add(attsContainer);
                                        }
                                    });

                            return f;
                        }

                        return null;
                    }
                };
        attributes.setPageable(false);
        attributes.setFilterable(false);
        attributes.setSortable(false);
        attsContainer.add(attributes);
    }

    protected void updateAttributeStats(DataAttribute attribute) throws IOException {
        FeatureTypeInfo featureType =
                GeoServerApplication.get().getCatalog().getFeatureType(featureTypeId);
        FeatureSource<?, ?> fs = featureType.getFeatureSource(null, null);

        // check we can compute min and max
        PropertyDescriptor pd = fs.getSchema().getDescriptor(attribute.getName());
        Class<?> binding = pd.getType().getBinding();
        if (pd == null
                || !Comparable.class.isAssignableFrom(binding)
                || Geometry.class.isAssignableFrom(binding)) {
            return;
        }

        // grab the feature collection and run the min/max visitors (this will move the
        // query to the dbms in case of such data source)
        Query q = new Query();
        q.setPropertyNames(new String[] {attribute.getName()});
        FeatureCollection<?, ?> fc = fs.getFeatures(q);
        MinVisitor minVisitor = new MinVisitor(attribute.getName());
        MaxVisitor maxVisitor = new MaxVisitor(attribute.getName());
        fc.accepts(minVisitor, null);
        fc.accepts(maxVisitor, null);
        Object min = minVisitor.getResult().getValue();
        attribute.setMin(Converters.convert(min, String.class));
        Object max = maxVisitor.getResult().getValue();
        attribute.setMax(Converters.convert(max, String.class));
    }

    private Feature getSampleFeature(FeatureTypeInfo layerInfo) throws IOException {
        FeatureSource<?, ?> fs = layerInfo.getFeatureSource(null, null);
        Query q = new Query();
        q.setMaxFeatures(1);
        FeatureCollection<?, ?> features = fs.getFeatures(q);
        try (FeatureIterator<?> fi = features.features()) {
            return fi.next();
        }
    }
}
