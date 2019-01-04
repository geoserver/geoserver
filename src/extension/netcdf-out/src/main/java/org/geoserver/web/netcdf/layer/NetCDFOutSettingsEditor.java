/* (c) 2015-2016 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.netcdf.layer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import javax.measure.Unit;
import javax.measure.format.UnitFormat;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.extensions.ajax.markup.html.autocomplete.AutoCompleteTextField;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.FormComponent;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.repeater.RepeatingView;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.util.string.Strings;
import org.apache.wicket.util.visit.IVisitor;
import org.geoserver.catalog.CoverageDimensionInfo;
import org.geoserver.catalog.CoverageInfo;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.web.netcdf.NetCDFExtensionPanel;
import org.geoserver.web.netcdf.NetCDFPanel;
import org.geotools.coverage.io.netcdf.cf.Entry;
import org.geotools.coverage.io.netcdf.cf.NetCDFCFParser;
import si.uom.NonSI;
import si.uom.SI;
import tec.uom.se.format.SimpleUnitFormat;

/**
 * Extension of the {@link NetCDFPanel} adding support for setting the Layer name and Unit of
 * Measure
 */
public class NetCDFOutSettingsEditor extends NetCDFPanel<NetCDFLayerSettingsContainer> {

    private static final NonSI NON_SI_INSTANCE = NonSI.getInstance();

    private static final SI SI_INSTANCE = SI.getInstance();

    private static final List<Unit<?>> UNITS;

    static {
        UNITS = new ArrayList<Unit<?>>();
        UNITS.addAll(SI_INSTANCE.getUnits());
        UNITS.addAll(NON_SI_INSTANCE.getUnits());
    }

    /** serialVersionUID */
    private static final long serialVersionUID = 1L;

    private final TextField<String> standardName;

    private final TextField<String> uom;

    public NetCDFOutSettingsEditor(
            String id,
            IModel<NetCDFLayerSettingsContainer> netcdfModel,
            IModel<CoverageInfo> cinfo) {
        super(id, netcdfModel);
        // Add panel for Standard name definition
        standardName =
                new TextField<String>("standardName", new PropertyModel(netcdfModel, "layerName"));
        // Add panel for UOM definition
        uom =
                new AutoCompleteTextField<String>(
                        "uom", new PropertyModel(netcdfModel, "layerUOM")) {

                    @Override
                    protected Iterator<String> getChoices(String input) {
                        if (Strings.isEmpty(input)) {
                            List<String> emptyList = Collections.emptyList();
                            return emptyList.iterator();
                        }

                        List<String> unitNames = new ArrayList<String>();
                        UnitFormat format = SimpleUnitFormat.getInstance();
                        for (Unit<?> unit : UNITS) {
                            unitNames.add(format.format(unit));
                        }

                        List<String> choices = new ArrayList<String>();
                        for (String name : unitNames) {
                            if (name.toLowerCase().startsWith(input.toLowerCase())) {
                                choices.add(name);
                            }
                        }

                        return choices.iterator();
                    }
                };
        // Setting the default value if not defined
        String startUOM = uom.getModelObject();
        if ((startUOM == null || startUOM.isEmpty()) && cinfo != null) {
            // Add the new value from the CoverageBand Details
            List<CoverageDimensionInfo> infos = cinfo.getObject().getDimensions();
            if (infos != null && infos.size() > 0) {
                CoverageDimensionInfo info = infos.get(0);
                uom.setModelObject(info.getUnit());
            }
        }

        container.add(standardName);
        container.add(uom);

        // Getting the available standard names
        NetCDFParserBean bean = GeoServerExtensions.bean(NetCDFParserBean.class);
        Set<String> names = new TreeSet<String>();
        if (bean != null && bean.getParser() != null) {
            NetCDFCFParser parser = bean.getParser();
            names.addAll(parser.getEntryIds());
        }

        final RepeatingView availableNames = new RepeatingView("availableNames");
        for (String key : names) {
            availableNames.add(new Label(availableNames.newChildId(), key));
        }
        container.add(availableNames);

        // Add Behaviour related to standard name choice
        standardName.add(
                new AjaxFormComponentUpdatingBehavior("Change") {

                    @Override
                    protected void onUpdate(AjaxRequestTarget target) {
                        String name = standardName.getModelObject();
                        if (name != null && !name.isEmpty()) {
                            NetCDFParserBean bean =
                                    GeoServerExtensions.bean(NetCDFParserBean.class);
                            if (bean != null && bean.getParser() != null) {
                                NetCDFCFParser parser = bean.getParser();
                                Entry e = null;
                                if (parser.hasEntryId(name)) {
                                    e = parser.getEntry(name);
                                } else if (parser.hasAliasId(name)) {
                                    e = parser.getEntryFromAlias(name);
                                }
                                if (e != null) {
                                    uom.setModelObject(e.getCanonicalUnits());
                                    target.add(container);
                                }
                            }
                        }
                    }
                });
    }

    @Override
    public void convertInput() {
        IVisitor<Component, Object> formComponentVisitor =
                (component, visit) -> {
                    if (component instanceof FormComponent) {
                        FormComponent<?> formComponent = (FormComponent<?>) component;
                        formComponent.processInput();
                    }
                };
        globalAttributes.visitChildren(formComponentVisitor);
        variableAttributes.visitChildren(formComponentVisitor);
        extraVariables.visitChildren(formComponentVisitor);
        compressionLevel.processInput();
        dataPacking.processInput();
        shuffle.processInput();
        copyAttributes.processInput();
        copyGlobalAttributes.processInput();
        standardName.processInput();
        uom.processInput();
        NetCDFLayerSettingsContainer convertedInput = new NetCDFLayerSettingsContainer();
        convertedInput.setCompressionLevel(compressionLevel.getModelObject());
        convertedInput.setGlobalAttributes(globalAttributes.getModelObject());
        convertedInput.setVariableAttributes(variableAttributes.getModelObject());
        convertedInput.setExtraVariables(extraVariables.getModelObject());
        convertedInput.setDataPacking(dataPacking.getModelObject());
        convertedInput.setShuffle(shuffle.getModelObject());
        convertedInput.setCopyAttributes(copyAttributes.getModelObject());
        convertedInput.setCopyGlobalAttributes(copyGlobalAttributes.getModelObject());
        convertedInput.setLayerName(standardName.getModelObject());
        convertedInput.setLayerUOM(uom.getModelObject());

        extensionPanels.visitChildren(
                (component, visit) -> {
                    if (component instanceof NetCDFExtensionPanel) {
                        NetCDFExtensionPanel extension = (NetCDFExtensionPanel) component;
                        extension.convertInput(convertedInput);
                    }
                });

        setConvertedInput(convertedInput);
    }
}
