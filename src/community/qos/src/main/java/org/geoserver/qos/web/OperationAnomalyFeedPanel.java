/*
 * (c) 2018 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.qos.web;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import org.apache.commons.lang.StringUtils;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.form.AjaxSubmitLink;
import org.apache.wicket.extensions.ajax.markup.html.autocomplete.AutoCompleteTextField;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;
import org.geoserver.qos.MimeTypes;
import org.geoserver.qos.xml.OwsAbstract;
import org.geoserver.qos.xml.ReferenceType;

public class OperationAnomalyFeedPanel extends Panel {

    protected IModel<ReferenceType> anomalyFeedModel;
    protected SerializableConsumer<AjaxTargetAndModel<ReferenceType>> onDelete;

    public OperationAnomalyFeedPanel(String id, IModel<ReferenceType> model) {
        super(id, model);
        anomalyFeedModel = model;

        ReferenceType anomalyFeed = model.getObject();
        if (anomalyFeed.getAbstracts() == null) {
            anomalyFeed.setAbstracts(
                    new ArrayList<OwsAbstract>(
                            Arrays.asList(new OwsAbstract[] {new OwsAbstract("")})));
        }

        final WebMarkupContainer div = new WebMarkupContainer("anomalyDiv");
        div.setOutputMarkupId(true);
        add(div);

        TextField<String> hrefField =
                new TextField<String>("href", new PropertyModel<>(anomalyFeedModel, "href")) {};
        div.add(hrefField);

        TextField<String> abstractField =
                new TextField<String>(
                        "abstract", new PropertyModel<>(anomalyFeedModel, "abstractOne")) {};
        div.add(abstractField);

        final AutoCompleteTextField<String> formatField =
                new AutoCompleteTextField<String>(
                        "format", new PropertyModel<>(anomalyFeedModel, "format")) {

                    @Override
                    protected Iterator<String> getChoices(String input) {
                        if (StringUtils.isEmpty(input) || StringUtils.isEmpty(input.trim())) {
                            return Collections.emptyIterator();
                        }
                        return MimeTypes.getMimeTypeValuesStream()
                                .filter(m -> m.startsWith(input))
                                .limit(10)
                                .iterator();
                    }
                };
        //        TextField<String> formatField =
        //                new TextField<String>("format", new PropertyModel<>(anomalyFeed,
        // "format")) {};
        div.add(formatField);

        final AjaxSubmitLink deleteLink =
                new AjaxSubmitLink("deleteLink") {
                    @Override
                    public void onAfterSubmit(AjaxRequestTarget target, Form<?> form) {
                        delete(target);
                    }
                };
        div.add(deleteLink);
    }

    public void delete(AjaxRequestTarget target) {
        if (onDelete == null) return;
        AjaxTargetAndModel<ReferenceType> chain =
                new AjaxTargetAndModel<ReferenceType>(
                        (ReferenceType) this.getDefaultModelObject(), target);
        onDelete.accept(chain);
    }

    public void setOnDelete(SerializableConsumer<AjaxTargetAndModel<ReferenceType>> onDelete) {
        this.onDelete = onDelete;
    }
}
