/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.data.layer;

import java.io.IOException;
import java.util.Arrays;
import java.util.logging.Level;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.SubmitLink;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.link.Link;
import org.apache.wicket.markup.html.panel.Fragment;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.request.resource.PackageResourceReference;
import org.geoserver.catalog.CatalogBuilder;
import org.geoserver.catalog.DataStoreInfo;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.web.ComponentAuthorizer;
import org.geoserver.web.GeoServerSecuredPage;
import org.geoserver.web.data.resource.ResourceConfigurationPage;
import org.geoserver.web.wicket.GeoServerAjaxFormLink;
import org.geoserver.web.wicket.GeoServerDataProvider.Property;
import org.geoserver.web.wicket.GeoServerTablePanel;
import org.geoserver.web.wicket.ImageAjaxLink;
import org.geoserver.web.wicket.ParamResourceModel;
import org.geotools.data.DataStore;
import org.geotools.data.FeatureSource;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.geotools.referencing.CRS;
import org.locationtech.jts.geom.Geometry;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;

/**
 * Builds a new feature type by having the user specify the attributes
 *
 * @author aaime
 */
@SuppressWarnings("serial")
public class NewFeatureTypePage extends GeoServerSecuredPage {

    public static final String DATASTORE = "storeName";

    public static final String WORKSPACE = "wsName";

    String name;

    Form<?> form;

    AttributesProvider attributesProvider;

    GeoServerTablePanel<AttributeDescription> attributeTable;

    String storeId;

    public NewFeatureTypePage(PageParameters params) {
        this(params.get(WORKSPACE).toOptionalString(), params.get(DATASTORE).toString());
    }

    public NewFeatureTypePage(String workspaceName, String storeName) {
        DataStoreInfo di = getCatalog().getDataStoreByName(workspaceName, storeName);
        if (di == null) {
            throw new IllegalArgumentException(
                    "Could not find a "
                            + storeName
                            + " store in the "
                            + workspaceName
                            + " workspace");
        }
        this.storeId = di.getId();

        form = new Form<>("form");
        form.setOutputMarkupId(true);
        add(form);

        form.add(new TextField<>("name", new PropertyModel<>(this, "name")).setRequired(true));

        attributesProvider = new AttributesProvider();
        attributeTable =
                new GeoServerTablePanel<AttributeDescription>(
                        "attributes", attributesProvider, true) {

                    @Override
                    protected Component getComponentForProperty(
                            String id,
                            IModel<AttributeDescription> itemModel,
                            Property<AttributeDescription> property) {
                        AttributeDescription att = (AttributeDescription) itemModel.getObject();
                        if (property == AttributesProvider.NAME) {
                            Fragment f = new Fragment(id, "nameFragment", NewFeatureTypePage.this);
                            f.add(editAttributeLink(itemModel));
                            return f;
                        } else if (property == AttributesProvider.BINDING) {
                            return new Label(
                                    id, AttributeDescription.getLocalizedName(att.getBinding()));
                        } else if (property == AttributesProvider.CRS) {
                            if (att.getBinding() != null
                                    && Geometry.class.isAssignableFrom(att.getBinding())) {
                                try {
                                    Integer epsgCode = CRS.lookupEpsgCode(att.getCrs(), false);
                                    return new Label(id, "EPSG:" + epsgCode);
                                } catch (Exception e) {
                                    throw new RuntimeException(e);
                                }
                            } else {
                                return new Label(id, "");
                            }
                        } else if (property == AttributesProvider.SIZE) {
                            if (att.getBinding() != null && String.class.equals(att.getBinding())) {
                                return new Label(id, String.valueOf(att.getSize()));
                            } else {
                                return new Label(id, "");
                            }
                        } else if (property == AttributesProvider.UPDOWN) {
                            return upDownFragment(id, att);
                        }

                        return null;
                    }
                };
        attributeTable.setSortable(false);
        attributeTable.setFilterable(false);
        attributeTable.getBottomPager().setVisible(false);
        form.add(attributeTable);

        SubmitLink saveLink = saveLink();
        form.add(saveLink);
        form.setDefaultButton(saveLink);
        form.add(cancelLink());

        setHeaderPanel(headerPanel());
    }

    private SubmitLink saveLink() {
        return new SubmitLink("save") {
            @Override
            public void onSubmit() {
                DataStore ds = null;
                DataStoreInfo dsInfo = null;
                try {
                    // basic checks
                    dsInfo = getCatalog().getDataStore(storeId);
                    ds = (DataStore) dsInfo.getDataStore(null);
                    if (Arrays.asList(ds.getTypeNames()).contains(name)) {
                        error(
                                new ParamResourceModel(
                                                "duplicateTypeName", this, dsInfo.getName(), name)
                                        .getString());
                        return;
                    }
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }

                if (attributesProvider.getAttributes().size() == 0) {
                    error(new ParamResourceModel("noAttributes", this).getString());
                    return;
                }

                try {
                    SimpleFeatureType featureType = buildFeatureType();
                    ds.createSchema(featureType);

                    CatalogBuilder builder = new CatalogBuilder(getCatalog());
                    builder.setStore(dsInfo);
                    FeatureTypeInfo fti = builder.buildFeatureType(getFeatureSource(ds));
                    LayerInfo layerInfo = builder.buildLayer(fti);
                    setResponsePage(new ResourceConfigurationPage(layerInfo, true));
                } catch (Exception e) {
                    LOGGER.log(Level.SEVERE, "Failed to create feature type", e);
                    error(
                            new ParamResourceModel("creationFailure", this, e.getMessage())
                                    .getString());
                }
            }
        };
    }

    FeatureSource<SimpleFeatureType, SimpleFeature> getFeatureSource(DataStore ds)
            throws IOException {
        try {
            return ds.getFeatureSource(name);
        } catch (IOException e) {
            // maybe it's Oracle?
            try {
                return ds.getFeatureSource(name.toUpperCase());
            } catch (Exception ora) {
                // nope, the reason was another one
                throw e;
            }
        }
    }

    SimpleFeatureType buildFeatureType() {
        SimpleFeatureTypeBuilder builder = new SimpleFeatureTypeBuilder();
        for (AttributeDescription att : attributesProvider.getAttributes()) {
            if (att.getSize() > 0) {
                builder.length(att.getSize());
            }
            if (Geometry.class.isAssignableFrom(att.getBinding())) {
                builder.add(att.getName(), att.getBinding(), att.getCrs());
            } else {
                builder.add(att.getName(), att.getBinding());
            }
        }
        builder.setName(name);
        return builder.buildFeatureType();
    }

    private Link<Void> cancelLink() {
        return new Link<Void>("cancel") {

            @Override
            public void onClick() {
                doReturn(NewLayerPage.class);
            }
        };
    }

    Component editAttributeLink(final IModel<AttributeDescription> itemModel) {
        GeoServerAjaxFormLink link =
                new GeoServerAjaxFormLink("link") {

                    @Override
                    protected void onClick(AjaxRequestTarget target, Form<?> form) {
                        AttributeDescription attribute = itemModel.getObject();
                        setResponsePage(new AttributeEditPage(attribute, NewFeatureTypePage.this));
                    }
                };
        link.add(new Label("name", new PropertyModel<String>(itemModel, "name")));
        return link;
    }

    protected Component headerPanel() {
        Fragment header = new Fragment(HEADER_PANEL, "header", this);

        // the add button
        header.add(
                new GeoServerAjaxFormLink("addNew", form) {

                    @Override
                    public void onClick(AjaxRequestTarget target, Form<?> form) {
                        AttributeDescription attribute = new AttributeDescription();
                        setResponsePage(new AttributeNewPage(attribute, NewFeatureTypePage.this));
                    }
                });

        header.add(
                new GeoServerAjaxFormLink("removeSelected", form) {

                    @Override
                    public void onClick(AjaxRequestTarget target, Form<?> form) {
                        attributesProvider.removeAll(attributeTable.getSelection());
                        attributeTable.clearSelection();
                        target.add(form);
                    }
                });

        return header;
    }

    protected Component upDownFragment(String id, final AttributeDescription attribute) {
        Fragment upDown = new Fragment(id, "upDown", this);
        if (attributesProvider.isFirst(attribute)) {
            upDown.add(new PlaceholderLink("up"));
        } else {
            ImageAjaxLink<Void> upLink =
                    new ImageAjaxLink<Void>(
                            "up",
                            new PackageResourceReference(
                                    getClass(), "../../img/icons/silk/arrow_up.png")) {
                        @Override
                        protected void onClick(AjaxRequestTarget target) {
                            attributesProvider.moveUp(attribute);
                            target.add(form);
                        }
                    };
            upDown.add(upLink);
        }

        if (attributesProvider.isLast(attribute)) {
            upDown.add(new PlaceholderLink("down"));
        } else {
            ImageAjaxLink<Void> downLink =
                    new ImageAjaxLink<Void>(
                            "down",
                            new PackageResourceReference(
                                    getClass(), "../../img/icons/silk/arrow_down.png")) {
                        @Override
                        protected void onClick(AjaxRequestTarget target) {
                            attributesProvider.moveDown(attribute);
                            target.add(form);
                        }
                    };
            upDown.add(downLink);
        }

        return upDown;
    }

    @Override
    protected ComponentAuthorizer getPageAuthorizer() {
        return ComponentAuthorizer.WORKSPACE_ADMIN;
    }

    /**
     * An empty link, put there just so that it will consume the same space as an ImageAjaxLink
     *
     * @author Andrea Aime
     */
    class PlaceholderLink extends ImageAjaxLink<Void> {

        public PlaceholderLink(String id) {
            super(
                    id,
                    new PackageResourceReference(
                            NewFeatureTypePage.class, "../../img/icons/blank.png"));
            setEnabled(false);
        }

        @Override
        protected void onClick(AjaxRequestTarget target) {
            // nothing to do
        }
    }
}
