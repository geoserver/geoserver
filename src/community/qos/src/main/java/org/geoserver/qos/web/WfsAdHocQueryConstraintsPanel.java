/*
 * (c) 2018 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.qos.web;

import java.util.ArrayList;
import java.util.List;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.qos.xml.WfsAdHocQueryConstraints;
import org.geoserver.wfs.WFSGetFeatureOutputFormat;

public class WfsAdHocQueryConstraintsPanel
        extends BaseLimitedConstraintsPanel<WfsAdHocQueryConstraints> {

    public WfsAdHocQueryConstraintsPanel(String id, IModel<WfsAdHocQueryConstraints> model) {
        super(id, model, new TypesListBuilder());

        // vars init
        if (model.getObject().getTypeNames() == null) {
            model.getObject().setTypeNames(new ArrayList<>());
        }

        final TextField<Integer> countInput =
                new TextField<>("countInput", new PropertyModel<>(model, "count"));
        innerConstraintsDiv.add(countInput);

        final TextField<String> resolveReferencesInput =
                new TextField<>(
                        "resolveReferencesInput", new PropertyModel<>(model, "resolveReferences"));
        innerConstraintsDiv.add(resolveReferencesInput);

        final TextField<String> sortBy =
                new TextField<>("sortByInput", new PropertyModel<>(model, "sortBy"));
        innerConstraintsDiv.add(sortBy);

        final TextField<String> propertyName =
                new TextField<>("propertyNameInput", new PropertyModel<>(model, "propertyName"));
        innerConstraintsDiv.add(propertyName);
    }

    @Override
    protected List<String> getSelectedLayers() {
        return getMainModel().getObject().getTypeNames();
    }

    @Override
    protected List<String> getOutputFormats() {
        List<WFSGetFeatureOutputFormat> formats =
                GeoServerExtensions.extensions(WFSGetFeatureOutputFormat.class);
        List<String> result = new ArrayList<>();
        for (WFSGetFeatureOutputFormat format : formats) {
            result.addAll(format.getOutputFormats());
        }
        return result;
    }
}
