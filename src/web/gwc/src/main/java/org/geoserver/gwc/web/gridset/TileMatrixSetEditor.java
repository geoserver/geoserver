/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.gwc.web.gridset;

import static com.google.common.base.Preconditions.checkNotNull;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormChoiceComponentUpdatingBehavior;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.FormComponentPanel;
import org.apache.wicket.markup.html.form.Radio;
import org.apache.wicket.markup.html.form.RadioGroup;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.ResourceModel;
import org.apache.wicket.validation.IValidatable;
import org.apache.wicket.validation.IValidator;
import org.apache.wicket.validation.ValidationError;
import org.geoserver.gwc.web.GWCIconFactory;
import org.geoserver.web.wicket.DecimalTextField;
import org.geoserver.web.wicket.ImageAjaxLink;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geowebcache.grid.BoundingBox;
import org.geowebcache.grid.Grid;
import org.geowebcache.grid.GridSet;
import org.geowebcache.grid.GridSetFactory;
import org.geowebcache.grid.SRS;

public class TileMatrixSetEditor extends FormComponentPanel<List<Grid>> {

    private static final long serialVersionUID = 5098470663723800345L;

    private ListView<Grid> grids;

    // private Label noMetadata;

    private WebMarkupContainer table;

    private boolean readOnly;

    private IModel<GridSetInfo> info;

    private static class TileMatrixSetValidator implements IValidator<List<Grid>> {

        private static final long serialVersionUID = 1L;

        @Override
        public void validate(IValidatable<List<Grid>> validatable) {
            List<Grid> grids = validatable.getValue();
            if (grids == null || grids.size() == 0) {
                ValidationError error = new ValidationError();
                error.setMessage(
                        new ResourceModel("TileMatrixSetEditor.validation.empty").getObject());
                validatable.error(error);
                return;
            }

            for (int i = 1; i < grids.size(); i++) {
                Grid prev = grids.get(i - 1);
                Grid curr = grids.get(i);

                if (curr.getResolution() >= prev.getResolution()) {
                    ValidationError error = new ValidationError();
                    String message =
                            "Each resolution should be lower than it's prior one. Res["
                                    + i
                                    + "] == "
                                    + curr.getResolution()
                                    + ", Res["
                                    + (i - 1)
                                    + "] == "
                                    + prev.getResolution()
                                    + ".";
                    error.setMessage(message);
                    validatable.error(error);
                    return;
                }

                if (curr.getScaleDenominator() >= prev.getScaleDenominator()) {
                    ValidationError error = new ValidationError();
                    String message =
                            "Each scale denominator should be lower "
                                    + "than it's prior one. Scale["
                                    + i
                                    + "] == "
                                    + curr.getScaleDenominator()
                                    + ", Scale["
                                    + (i - 1)
                                    + "] == "
                                    + prev.getScaleDenominator()
                                    + ".";
                    error.setMessage(message);
                    validatable.error(error);
                    return;
                }
            }
        }
    }

    public TileMatrixSetEditor(final String id, final IModel<GridSetInfo> info) {
        super(id, new PropertyModel<List<Grid>>(info, "levels"));
        add(new TileMatrixSetValidator());

        final IModel<List<Grid>> list = getModel();
        checkNotNull(list.getObject());

        this.info = info;
        this.readOnly = info.getObject().isInternal();

        // container for ajax updates
        final WebMarkupContainer container = new WebMarkupContainer("container");
        container.setOutputMarkupId(true);
        add(container);

        final IModel<Boolean> preserveesolutionsModel =
                new PropertyModel<Boolean>(info, "resolutionsPreserved");

        final RadioGroup<Boolean> resolutionsOrScales =
                new RadioGroup<Boolean>("useResolutionsOrScalesGroup", preserveesolutionsModel);
        container.add(resolutionsOrScales);

        Radio<Boolean> preserveResolutions =
                new Radio<Boolean>("preserveResolutions", new Model<Boolean>(Boolean.TRUE));
        Radio<Boolean> preserveScales =
                new Radio<Boolean>("preserveScales", new Model<Boolean>(Boolean.FALSE));

        resolutionsOrScales.add(preserveResolutions);
        resolutionsOrScales.add(preserveScales);

        // update the table when this option changes so either the resolutions or scales column is
        // enabled
        resolutionsOrScales.add(
                new AjaxFormChoiceComponentUpdatingBehavior() {
                    private static final long serialVersionUID = 1L;

                    @Override
                    protected void onUpdate(AjaxRequestTarget target) {
                        resolutionsOrScales.processInput();
                        final boolean useResolutions =
                                resolutionsOrScales.getModelObject().booleanValue();

                        Iterator<Component> iterator = grids.iterator();
                        while (iterator.hasNext()) {
                            @SuppressWarnings("unchecked")
                            ListItem<Grid> next = (ListItem<Grid>) iterator.next();
                            next.get("resolution").setEnabled(useResolutions);
                            next.get("scale").setEnabled(!useResolutions);
                        }
                        target.add(table);
                    }
                });

        // the link list
        table = new WebMarkupContainer("table");
        table.setOutputMarkupId(true);

        table.add(thLabel("level"));
        table.add(thLabel("resolution"));
        table.add(thLabel("scale"));
        table.add(thLabel("name"));
        table.add(thLabel("tiles"));

        container.add(table);

        grids =
                new ListView<Grid>("gridLevels", new ArrayList<Grid>(list.getObject())) {

                    private static final long serialVersionUID = 1L;

                    @Override
                    protected void onBeforeRender() {
                        super.onBeforeRender();
                    }

                    @Override
                    protected void populateItem(final ListItem<Grid> item) {
                        // odd/even style
                        final int index = item.getIndex();
                        item.add(
                                AttributeModifier.replace(
                                        "class", index % 2 == 0 ? "even" : "odd"));

                        item.add(new Label("zoomLevel", String.valueOf(index)));

                        final TextField<Double> resolution;
                        final TextField<Double> scale;
                        final TextField<String> name;
                        final Label tiles;
                        final Component removeLink;

                        resolution =
                                new DecimalTextField(
                                        "resolution",
                                        new PropertyModel<Double>(item.getModel(), "resolution"));
                        resolution.setOutputMarkupId(true);
                        item.add(resolution);

                        scale =
                                new DecimalTextField(
                                        "scale",
                                        new PropertyModel<Double>(item.getModel(), "scaleDenom"));
                        scale.setOutputMarkupId(true);
                        item.add(scale);

                        name =
                                new TextField<String>(
                                        "name", new PropertyModel<String>(item.getModel(), "name"));
                        item.add(name);

                        IModel<String> tilesModel =
                                new IModel<String>() {
                                    private static final long serialVersionUID = 1L;

                                    @Override
                                    public String getObject() {
                                        // resolution.processInput();
                                        Double res = resolution.getModelObject();
                                        GridSetInfo gridSetInfo =
                                                TileMatrixSetEditor.this.info.getObject();
                                        final ReferencedEnvelope extent = gridSetInfo.getBounds();
                                        if (res == null || extent == null) {
                                            return "--";
                                        }
                                        final int tileWidth = gridSetInfo.getTileWidth();
                                        final int tileHeight = gridSetInfo.getTileHeight();
                                        final double mapUnitWidth = tileWidth * res.doubleValue();
                                        final double mapUnitHeight = tileHeight * res.doubleValue();

                                        final long tilesWide =
                                                (long)
                                                        Math.ceil(
                                                                (extent.getWidth()
                                                                                - mapUnitWidth
                                                                                        * 0.01)
                                                                        / mapUnitWidth);
                                        final long tilesHigh =
                                                (long)
                                                        Math.ceil(
                                                                (extent.getHeight()
                                                                                - mapUnitHeight
                                                                                        * 0.01)
                                                                        / mapUnitHeight);

                                        NumberFormat nf =
                                                NumberFormat.getIntegerInstance(); // so it shows
                                        // grouping
                                        // for large numbers
                                        String tilesStr =
                                                nf.format(tilesWide) + " x " + nf.format(tilesHigh);

                                        return tilesStr;
                                    }

                                    @Override
                                    public void detach() {
                                        //
                                    }

                                    @Override
                                    public void setObject(String object) {
                                        //
                                    }
                                };

                        tiles = new Label("tiles", tilesModel);
                        tiles.setOutputMarkupId(true);
                        item.add(tiles);

                        // remove link
                        if (TileMatrixSetEditor.this.readOnly) {
                            removeLink = new Label("removeLink", "");
                        } else {
                            removeLink =
                                    new ImageAjaxLink<Void>(
                                            "removeLink", GWCIconFactory.DELETE_ICON) {
                                        private static final long serialVersionUID = 1L;

                                        @Override
                                        protected void onClick(AjaxRequestTarget target) {
                                            List<Grid> list =
                                                    new ArrayList<Grid>(grids.getModelObject());
                                            int index =
                                                    ((Integer) getDefaultModelObject()).intValue();
                                            list.remove(index);
                                            grids.setModelObject(list);
                                            target.add(container);
                                        }
                                    };
                            removeLink.setDefaultModel(new Model<Integer>(Integer.valueOf(index)));
                            removeLink.add(
                                    new AttributeModifier(
                                            "title",
                                            new ResourceModel("TileMatrixSetEditor.removeLink")));
                        }
                        item.add(removeLink);

                        final boolean isResolutionsPreserved = preserveesolutionsModel.getObject();
                        resolution.setEnabled(isResolutionsPreserved);
                        scale.setEnabled(!isResolutionsPreserved);

                        resolution.add(
                                new AjaxFormComponentUpdatingBehavior("blur") {
                                    private static final long serialVersionUID = 1L;

                                    @Override
                                    protected void onUpdate(AjaxRequestTarget target) {
                                        resolution.processInput();
                                        Double res = resolution.getModelObject();
                                        Double scaleDenominator = null;
                                        if (null != res) {
                                            GridSetInfo gridSetInfo =
                                                    TileMatrixSetEditor.this.info.getObject();
                                            Double metersPerUnit = gridSetInfo.getMetersPerUnit();
                                            if (metersPerUnit != null) {
                                                scaleDenominator =
                                                        res.doubleValue()
                                                                * metersPerUnit.doubleValue()
                                                                / GridSetFactory
                                                                        .DEFAULT_PIXEL_SIZE_METER;
                                            }
                                        }
                                        scale.setModelObject(scaleDenominator);
                                        target.add(resolution);
                                        target.add(scale);
                                        target.add(tiles);
                                    }
                                });

                        scale.add(
                                new AjaxFormComponentUpdatingBehavior("blur") {
                                    private static final long serialVersionUID = 1L;

                                    @Override
                                    protected void onUpdate(AjaxRequestTarget target) {
                                        scale.processInput();
                                        final Double scaleDenominator = scale.getModelObject();
                                        Double res = null;
                                        if (null != scaleDenominator) {
                                            GridSetInfo gridSetInfo =
                                                    TileMatrixSetEditor.this.info.getObject();
                                            final double pixelSize = gridSetInfo.getPixelSize();
                                            Double metersPerUnit = gridSetInfo.getMetersPerUnit();
                                            if (metersPerUnit != null) {
                                                res = pixelSize * scaleDenominator / metersPerUnit;
                                            }
                                        }
                                        resolution.setModelObject(res);
                                        target.add(resolution);
                                        target.add(scale);
                                        target.add(tiles);
                                    }
                                });
                    }
                };
        grids.setOutputMarkupId(true);
        // this is necessary to avoid loosing item contents on edit/validation checks
        grids.setReuseItems(true);
        table.add(grids);
    }

    private Component thLabel(String id) {
        Label label = new Label(id, new ResourceModel(id));
        label.add(new AttributeModifier("title", new ResourceModel(id + ".title", "")));
        return label;
    }

    @Override
    public void convertInput() {
        List<Grid> info = grids.getModelObject();
        if (info == null || info.size() == 0) {
            setConvertedInput(new ArrayList<Grid>(2));
            return;
        }

        setConvertedInput(info);
    }

    /** */
    @Override
    protected void onBeforeRender() {
        super.onBeforeRender();
    }

    public void addZoomLevel(ReferencedEnvelope bbox, int tileWidth, int tileHeight) {
        List<Grid> list = grids.getModelObject();
        final Grid newGrid = new Grid();
        if (list.isEmpty()) {
            BoundingBox extent =
                    new BoundingBox(bbox.getMinX(), bbox.getMinY(), bbox.getMaxX(), bbox.getMaxY());
            final int levels = 1;
            GridSet tmpGridset =
                    GridSetFactory.createGridSet(
                            "stub",
                            SRS.getEPSG4326(),
                            extent,
                            false,
                            levels,
                            1D,
                            GridSetFactory.DEFAULT_PIXEL_SIZE_METER,
                            tileWidth,
                            tileHeight,
                            false);
            Grid grid = tmpGridset.getGrid(0);
            newGrid.setResolution(grid.getResolution());
            newGrid.setScaleDenominator(grid.getScaleDenominator());
        } else {
            Grid prev = list.get(list.size() - 1);
            newGrid.setResolution(prev.getResolution() / 2);
            newGrid.setScaleDenominator(prev.getScaleDenominator() / 2);
        }
        list.add(newGrid);
        grids.setModelObject(list);
        // TileMatrixSetEditor.this.convertInput();
    }
}
