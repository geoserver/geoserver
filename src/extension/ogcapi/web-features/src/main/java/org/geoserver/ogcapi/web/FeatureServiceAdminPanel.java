/* (c) 2024 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.ogcapi.web;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.CheckBox;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;
import org.geoserver.ogcapi.APIConformance;
import org.geoserver.ogcapi.v1.features.CQL2Conformance;
import org.geoserver.ogcapi.v1.features.ECQLConformance;
import org.geoserver.ogcapi.v1.features.FeatureConformance;
import org.geoserver.web.services.AdminPagePanel;
import org.geoserver.wfs.WFSInfo;

@SuppressWarnings("serial")
public class FeatureServiceAdminPanel extends AdminPagePanel {

    public FeatureServiceAdminPanel(String id, final IModel<?> info) {
        super(id, info);
        featureServiceSettings(info);
        cql2Settings(info);
        ecqlSettings(info);
    }

    /**
     * Obtain FeatureConformance for wicket model.
     *
     * <p>This is used to reduce boiler-plate code in wicket callbacks and offer some null safety.
     *
     * @param info WFSInfo model assumed
     * @return FeatureConformance for model
     */
    static FeatureConformance features(IModel<?> info) {
        if (info != null && info.getObject() != null && info.getObject() instanceof WFSInfo) {
            return FeatureConformance.configuration((WFSInfo) info.getObject());
        }
        return null;
    }
    /**
     * Obtain ECQLConformance for wicket model.
     *
     * <p>This is used to reduce boiler-plate code in wicket callbacks and offer some null safety.
     *
     * @param info WFSInfo model assumed
     * @return FeatureConformance for model
     */
    static ECQLConformance ecql(IModel<?> info) {
        if (info != null && info.getObject() != null && info.getObject() instanceof WFSInfo) {
            return ECQLConformance.configuration((WFSInfo) info.getObject());
        }
        return null;
    }

    /**
     * Obtain ECQL2Conformance for wicket model.
     *
     * <p>This is used to reduce boiler-plate code in wicket callbacks and offer some null safety.
     *
     * @param info WFSInfo model assumed
     * @return FeatureConformance for model
     */
    static CQL2Conformance cql2(IModel<?> info) {
        if (info != null && info.getObject() != null && info.getObject() instanceof WFSInfo) {
            return CQL2Conformance.configuration((WFSInfo) info.getObject());
        }
        return null;
    }

    @Override
    public void onMainFormSubmit() {
        WFSInfo wfsInfo = (WFSInfo) getDefaultModel().getObject();
        FeatureConformance features = FeatureConformance.configuration(wfsInfo);
        CQL2Conformance cql2 = CQL2Conformance.configuration(wfsInfo);
        ECQLConformance ecql = ECQLConformance.configuration(wfsInfo);

        if (!features.isEnabled(wfsInfo)) {
            wfsInfo.getMetadata().remove(FeatureConformance.METADATA_KEY);
        }
        if (!cql2.isEnabled(wfsInfo)) {
            wfsInfo.getMetadata().remove(CQL2Conformance.METADATA_KEY);
        }
        if (!ecql.isEnabled(wfsInfo)) {
            wfsInfo.getMetadata().remove(ECQLConformance.METADATA_KEY);
        }
    }
    /**
     * Insert a disabled conformance checkbox indicating of functionality is built-in, or simply not implemented.
     *
     * @param key
     * @param conformance
     * @param implemented if true, the conformance is built-in, if false, the conformance is not-implemented.
     * @return checkbox component to be used if further customization is required
     */
    protected CheckBox addConformance(String key, APIConformance conformance, boolean implemented) {

        CheckBox checkBox = addConformance(key, conformance, () -> implemented, () -> implemented);
        checkBox.setEnabled(false);

        return checkBox;
    }

    /**
     * Insert a conformance checkbox backed by the provided model.
     *
     * @param key Wicket id of checkbox, also used to obtained internationalization text
     * @param conformance Conformance class to be represented by the checkbox
     * @param booleanModel Model, often backed by WFSInfo, to store checkbox value.
     * @param enabled Lambda used to determine in conformance is enanbled (i.e. implemented and configurable)
     * @return checkbox component to be used if further customization is required
     */
    protected CheckBox addConformance(
            String key, final APIConformance conformance, IModel<Boolean> booleanModel, final IModel<Boolean> enabled) {
        WFSInfo info = (WFSInfo) getDefaultModel().getObject();
        boolean stable = conformance.getLevel().isStable();
        boolean endorsed = conformance.getLevel().isEndorsed();

        final Label label = new Label(key + "Label", conformance.getId());
        label.add(new AttributeModifier("title", conformance.getId()));
        // provide an id for ajax show/hide
        label.setOutputMarkupId(true);
        // force div wrapper something always exists to show/hide
        label.setOutputMarkupPlaceholderTag(true);
        label.setVisible(Boolean.TRUE.equals(enabled.getObject()));

        Label level = new Label(key + "Level", level(conformance.getLevel()));
        level.add(new AttributeModifier("title", recommendation(conformance.getLevel())));
        final ThreeStateAjaxCheckBox checkBox = new ThreeStateAjaxCheckBox(key, booleanModel) {
            @Override
            protected void onUpdate(AjaxRequestTarget target) {

                Label uriLabel = (Label) FeatureServiceAdminPanel.this.get(key + "Label");

                Boolean modelValue = getModelObject();

                boolean visible = modelValue == null || modelValue.booleanValue();
                uriLabel.setVisible(visible);
                uriLabel.setDefaultModelObject(conformance.getId());
                target.add(uriLabel);
            }
        };
        checkBox.add(new AttributeModifier("title", conformance.getId()));

        if (info.isCiteCompliant()) {
            level.setEnabled(stable && endorsed);
            label.setEnabled(stable && endorsed);

            checkBox.setEnabled(stable && endorsed);
        } else {
            level.setEnabled(stable);
        }
        add(level);
        add(checkBox);
        add(label);

        return checkBox;
    }

    private String level(APIConformance.Level level) {
        return level.toString().replace('_', ' ');
    }

    private String recommendation(APIConformance.Level level) {
        switch (level) {
            case COMMUNITY_STANDARD:
            case STANDARD:
                return "Stable";
            case DRAFT_STANDARD:
            case COMMUNITY_DRAFT:
                return "Unstable";
            case RETIRED_STANDARD:
                return "Deprecated";
            default:
                return "Unknown";
        }
    }

    private void featureServiceSettings(IModel<?> info) {
        FeatureConformance featuresInfo = features(info);

        // core is required at present
        addConformance("core", FeatureConformance.CORE, true);

        // Required built-in conformance
        addConformance("oas30", FeatureConformance.OAS30, true);
        addConformance("html", FeatureConformance.HTML, true);
        addConformance("geojson", FeatureConformance.GEOJSON, true);

        // formats - optional
        addConformance(
                "gml321", FeatureConformance.GML321, new PropertyModel<>(featuresInfo, "gml321"), () -> features(info)
                        .gml321((WFSInfo) info.getObject()));

        // formats - not implemented
        addConformance("gmlsf0", FeatureConformance.GMLSF0, false);
        addConformance("gmlsf2", FeatureConformance.GMLSF2, false);

        // optional
        addConformance(
                "crsByReference",
                FeatureConformance.CRS_BY_REFERENCE,
                new PropertyModel<>(featuresInfo, "crsByReference"),
                () -> features(info).crsByReference((WFSInfo) info.getObject()));

        addConformance(
                "filter", FeatureConformance.FILTER, new PropertyModel<>(featuresInfo, "filter"), () -> features(info)
                        .filter((WFSInfo) info.getObject()));

        addConformance(
                "featuresFilter",
                FeatureConformance.FEATURES_FILTER,
                new PropertyModel<>(featuresInfo, "featuresFilter"),
                () -> features(info).featuresFilter((WFSInfo) info.getObject()));

        addConformance(
                "queryables",
                FeatureConformance.QUERYABLES,
                new PropertyModel<>(featuresInfo, "queryables"),
                () -> features(info).queryables((WFSInfo) info.getObject()));

        addConformance("ids", FeatureConformance.IDS, new PropertyModel<>(featuresInfo, "ids"), () -> features(info)
                .ids((WFSInfo) info.getObject()));
        addConformance(
                "search", FeatureConformance.SEARCH, new PropertyModel<>(featuresInfo, "search"), () -> features(info)
                        .search((WFSInfo) info.getObject()));
        addConformance(
                "sortBy", FeatureConformance.SORTBY, new PropertyModel<>(featuresInfo, "sortBy"), () -> features(info)
                        .sortBy((WFSInfo) info.getObject()));
    }

    private void ecqlSettings(IModel<?> info) {
        ECQLConformance ecqlInfo = ecql(info);
        // ECQL
        addConformance("ecql", ECQLConformance.ECQL, new PropertyModel<>(ecqlInfo, "ecql"), () -> ecql(info)
                .ecql((WFSInfo) info.getObject()));
        addConformance("ecqlText", ECQLConformance.ECQL_TEXT, new PropertyModel<>(ecqlInfo, "text"), () -> ecql(info)
                .text((WFSInfo) info.getObject()));
    }

    private void cql2Settings(IModel<?> info) {
        CQL2Conformance cql2Info = cql2(info);

        // CQL2
        addConformance("cql2Text", CQL2Conformance.CQL2_TEXT, new PropertyModel<>(cql2Info, "text"), () -> cql2(info)
                .text((WFSInfo) info.getObject()));
        addConformance("cql2JSON", CQL2Conformance.CQL2_JSON, new PropertyModel<>(cql2Info, "json"), () -> cql2(info)
                .json((WFSInfo) info.getObject()));

        // built-in conformance
        addConformance("cql2Advanced", CQL2Conformance.CQL2_ADVANCED, true);
        addConformance("cql2Arithmetic", CQL2Conformance.CQL2_ARITHMETIC, true);
        addConformance("cql2Array", CQL2Conformance.CQL2_ARITHMETIC, false);
        addConformance("cql2Basic", CQL2Conformance.CQL2_BASIC, true);
        addConformance("cql2BasicSpatial", CQL2Conformance.CQL2_BASIC_SPATIAL, true);
        addConformance(
                "cql2Functions",
                CQL2Conformance.CQL2_FUNCTIONS,
                new PropertyModel<>(cql2Info, "functions"),
                () -> cql2(info).functions((WFSInfo) info.getObject()));

        addConformance("cql2Temporal", CQL2Conformance.CQL2_TEMPORAL, false); // not implemented
        addConformance("cql2PropertyProperty", CQL2Conformance.CQL2_PROPERTY_PROPERTY, true);
        addConformance("cql2Spatial", CQL2Conformance.CQL2_SPATIAL, true);
    }
}
