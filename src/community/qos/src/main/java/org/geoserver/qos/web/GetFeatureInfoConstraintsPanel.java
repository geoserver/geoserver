/*
 * (c) 2018 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.qos.web;

import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;
import org.apache.wicket.model.IModel;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.qos.xml.LimitedAreaRequestConstraints;
import org.geoserver.wms.featureinfo.GetFeatureInfoOutputFormat;

public class GetFeatureInfoConstraintsPanel extends LimitedConstraintsPanel {

    public GetFeatureInfoConstraintsPanel(String id, IModel<LimitedAreaRequestConstraints> model) {
        super(id, model);
    }

    @Override
    protected List<String> getOutputFormats() {
        TreeSet<String> getFeatureInfoAvailable =
                new TreeSet<String>(String.CASE_INSENSITIVE_ORDER);
        for (GetFeatureInfoOutputFormat format :
                GeoServerExtensions.extensions(GetFeatureInfoOutputFormat.class)) {
            getFeatureInfoAvailable.add(format.getContentType());
        }
        List<String> getFeatureInfoChoices = new ArrayList<String>();
        getFeatureInfoChoices.addAll(getFeatureInfoAvailable);
        return getFeatureInfoChoices;
    }
}
