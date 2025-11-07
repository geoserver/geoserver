/* (c) 2023 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 *
 * Original from GeoServer 2.24-SNAPSHOT under GPL 2.0 license (factored out from org.geoserver.geofence.server.web.GeofenceRulePage)
 */
package org.geoserver.acl.plugin.web.components;

import org.apache.wicket.markup.html.form.FormComponent;
import org.apache.wicket.markup.html.form.FormComponentPanel;
import org.apache.wicket.markup.html.form.Radio;
import org.apache.wicket.markup.html.form.RadioGroup;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.geolatte.geom.MultiPolygon;
import org.geoserver.acl.domain.rules.SpatialFilterType;

/**
 * Base form component for a {@link MultiPolygon} / {@link SpatialFilterType} pair, subclasses must implement
 * {@link #convertInput()} uing #getAllo
 */
@SuppressWarnings({"serial", "rawtypes"})
public abstract class AllowedAreaEditPanel<T> extends FormComponentPanel<T> {

    protected final FormComponent<MultiPolygon> allowedArea;
    protected final FormComponent<SpatialFilterType> spatialFilterType;
    protected Radio<SpatialFilterType> intersect;
    protected Radio<SpatialFilterType> clip;

    public AllowedAreaEditPanel(
            String id, IModel<T> componentModel, String areaProperty, String spatialFilterTypeProperty) {
        super(id);
        CompoundPropertyModel<T> model = CompoundPropertyModel.of(componentModel);
        setModel(model);
        add(allowedArea = allowedArea(model.bind(areaProperty)));
        add(spatialFilterType = spatialFilterType(model.bind(spatialFilterTypeProperty)));

        super.setOutputMarkupId(true);
        super.setOutputMarkupPlaceholderTag(true);
    }

    private FormComponent<MultiPolygon> allowedArea(IModel<MultiPolygon> model) {
        return new GeometryWktTextArea<>("allowedArea", MultiPolygon.class, model);
    }

    private FormComponent<SpatialFilterType> spatialFilterType(IModel<SpatialFilterType> model) {
        RadioGroup<SpatialFilterType> group = new RadioGroup<>("spatialFilterType", model);
        intersect = new Radio<>("INTERSECT", Model.of(SpatialFilterType.INTERSECT), group);
        clip = new Radio<>("CLIP", Model.of(SpatialFilterType.CLIP), group);
        group.add(intersect, clip);
        return group;
    }

    @Override
    public abstract void convertInput();

    protected SpatialFilterType getSpatialFilterTypeConvertedInput() {
        return spatialFilterType.getConvertedInput();
    }

    protected MultiPolygon getAllowedAreaConvertedInput() {
        return allowedArea.getConvertedInput();
    }
}
