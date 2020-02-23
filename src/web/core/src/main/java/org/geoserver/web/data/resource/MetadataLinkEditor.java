/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.data.resource;

import java.util.Arrays;
import java.util.List;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.ajax.markup.html.form.AjaxButton;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.form.validation.FormComponentFeedbackBorder;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.ResourceModel;
import org.apache.wicket.validation.IValidatable;
import org.apache.wicket.validation.IValidationError;
import org.apache.wicket.validation.IValidator;
import org.apache.wicket.validation.ValidationError;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.LayerGroupInfo;
import org.geoserver.catalog.MetadataLinkInfo;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.catalog.impl.MetadataLinkInfoImpl;
import org.geoserver.web.GeoServerApplication;

/**
 * Shows and allows editing of the {@link MetadataLinkInfo} attached to a {@link ResourceInfo}
 *
 * @author Andrea Aime - OpenGeo
 */
public class MetadataLinkEditor extends Panel {
    private static final long serialVersionUID = -5721941745847988670L;
    /**
     * Can't depend on the wms module here, but beware links of type ISO19115:2003 won't show up in
     * WMS 1.1.1 GetCaps
     */
    private static final List<String> LINK_TYPES =
            Arrays.asList("ISO19115:2003", "FGDC", "TC211", "19139", "other");

    private final ListView<MetadataLinkInfo> links;
    private final Label noMetadata;
    private final WebMarkupContainer table;
    private PropertyModel<List<MetadataLinkInfo>> metadataLinksModel;

    /** Convenience method for pages to get access to the catalog. */
    protected Catalog getCatalog() {
        return ((GeoServerApplication) getApplication()).getCatalog();
    }

    /**
     * @param resourceModel Must return object that has a "metadataLinks" property (such as a {@link
     *     ResourceInfo} or {@link LayerGroupInfo})
     */
    public MetadataLinkEditor(String id, final IModel<?> resourceModel) {
        super(id, resourceModel);

        // container for ajax updates
        final WebMarkupContainer container = new WebMarkupContainer("container");
        container.setOutputMarkupId(true);
        add(container);

        metadataLinksModel = new PropertyModel<>(resourceModel, "metadataLinks");

        // the link list
        table = new WebMarkupContainer("table");
        table.setOutputMarkupId(true);
        container.add(table);
        links =
                new ListView<MetadataLinkInfo>("links", metadataLinksModel) {

                    private static final long serialVersionUID = -3241009112151911288L;

                    @Override
                    protected void populateItem(ListItem<MetadataLinkInfo> item) {

                        // odd/even style
                        item.add(
                                AttributeModifier.replace(
                                        "class", item.getIndex() % 2 == 0 ? "even" : "odd"));

                        // link info
                        DropDownChoice<String> dropDownChoice =
                                new DropDownChoice<>(
                                        "type",
                                        new PropertyModel<String>(item.getModel(), "metadataType"),
                                        LINK_TYPES);
                        dropDownChoice.setRequired(true);
                        item.add(dropDownChoice);
                        FormComponentFeedbackBorder urlBorder =
                                new FormComponentFeedbackBorder("urlBorder");
                        item.add(urlBorder);
                        TextField<String> format =
                                new TextField<>(
                                        "format",
                                        new PropertyModel<String>(item.getModel(), "type"));
                        format.setRequired(true);
                        item.add(format);
                        TextField<String> url =
                                new TextField<>(
                                        "metadataLinkURL",
                                        new PropertyModel<String>(item.getModel(), "content"));
                        url.add(new UrlValidator());
                        url.setRequired(true);
                        urlBorder.add(url);

                        // remove link
                        AjaxLink<MetadataLinkInfo> link =
                                new AjaxLink<MetadataLinkInfo>("removeLink", item.getModel()) {

                                    private static final long serialVersionUID =
                                            -6204300287066695521L;

                                    @Override
                                    public void onClick(AjaxRequestTarget target) {
                                        metadataLinksModel.getObject().remove(getModelObject());
                                        updateLinksVisibility();
                                        target.add(container);
                                    }
                                };
                        item.add(link);
                    }
                };
        // this is necessary to avoid loosing item contents on edit/validation checks
        links.setReuseItems(true);
        table.add(links);

        // the no metadata links label
        noMetadata = new Label("noLinks", new ResourceModel("noMetadataLinksSoFar"));
        container.add(noMetadata);
        updateLinksVisibility();

        // add new link button
        AjaxButton button =
                new AjaxButton("addlink") {
                    private static final long serialVersionUID = -695617463194724617L;

                    @Override
                    protected void onSubmit(AjaxRequestTarget target, Form<?> form) {
                        MetadataLinkInfo link = getCatalog().getFactory().createMetadataLink();
                        link.setMetadataType(LINK_TYPES.get(0));
                        link.setType("text/plain");
                        metadataLinksModel.getObject().add(link);
                        updateLinksVisibility();

                        target.add(container);
                    }
                };
        add(button);
    }

    private void updateLinksVisibility() {
        boolean anyLink = metadataLinksModel.getObject().size() > 0;
        table.setVisible(anyLink);
        noMetadata.setVisible(!anyLink);
    }

    public class UrlValidator implements IValidator<String> {
        private static final long serialVersionUID = 8435726308689930141L;

        @Override
        public void validate(IValidatable validatable) {
            String url = (String) validatable.getValue();
            if (url != null) {
                try {
                    MetadataLinkInfoImpl.validate(url);
                } catch (IllegalArgumentException ex) {
                    IValidationError err =
                            new ValidationError("invalidURL")
                                    .addKey("invalidURL")
                                    .setVariable("url", url);
                    validatable.error(err);
                }
            }
        }
    }
}
