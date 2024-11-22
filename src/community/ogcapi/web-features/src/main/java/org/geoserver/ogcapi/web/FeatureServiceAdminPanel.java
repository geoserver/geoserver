package org.geoserver.ogcapi.web;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.CheckBox;
import org.apache.wicket.model.IModel;
import org.checkerframework.checker.units.qual.A;
import org.geoserver.ogcapi.APIConformance;
import org.geoserver.ogcapi.v1.features.CQL2Conformance;
import org.geoserver.ogcapi.v1.features.ECQLConformance;
import org.geoserver.ogcapi.v1.features.FeatureConformance;
import org.geoserver.web.services.AdminPagePanel;
import org.geoserver.wfs.WFSInfo;

public class FeatureServiceAdminPanel extends AdminPagePanel {

    public FeatureServiceAdminPanel(String id, final IModel<?> info) {
        super(id, info);

        WFSInfo wfsInfo = (WFSInfo) info.getObject();

        featureServiceSettings(info);
        cql2Settings(info);
        ecqlSettings(info);
    }

    public void onMainFormSubmit() {
        WFSInfo wfsInfo = (WFSInfo) getDefaultModel().getObject();
        FeatureConformance features = new FeatureConformance(wfsInfo);
        CQL2Conformance cql2 = new CQL2Conformance(wfsInfo);
        ECQLConformance ecql = new ECQLConformance(wfsInfo);

        if (!features.isEnabled()) {
            wfsInfo.getMetadata().remove(features.getMetadataKey());
        }
        if (!cql2.isEnabled()) {
            wfsInfo.getMetadata().remove(cql2.getMetadataKey());
        }
        if (!ecql.isEnabled()) {
            wfsInfo.getMetadata().remove(ecql.getMetadataKey());
        }
    }
    /**
     * Insert a disabled conformance checkbox indicating of functionality is built-in, or simply not implemented.
     * @param key
     * @param conformance
     * @param implemented if true, the conformance is built-in, if false, the conformance is not-implemented.
     * @return checkbox component to be used if further customization is required
     */
    protected CheckBox addConformance(String key, APIConformance conformance, boolean implemented) {
        CheckBox checkBox = addConformance(key, conformance, () -> implemented);
        checkBox.setEnabled(false);

        return checkBox;
    }

    /**
     * Insert a conformance checkbox backed by the provided model.
     *
     * @param key Wicket id of checkbox, also used to obtained internationalization text
     * @param conformance Conformance class to be represented by the checkbox
     * @param booleanModel Model, often backed by WFSInfo, to store checkbox value.
     * @return checkbox component to be used if further customization is required
     */
    protected CheckBox addConformance(String key, APIConformance conformance, IModel<Boolean> booleanModel) {
        WFSInfo info = (WFSInfo) getDefaultModel().getObject();
        boolean stable = conformance.getLevel().isStable();
        boolean endorsed = conformance.getLevel().isEndorsed();

        Label level = new Label(key+"Level", conformance.getLevel());
        level.add(new AttributeModifier("title", recommendation(conformance.getLevel())));

        CheckBox checkBox = new CheckBox(key, booleanModel);
        checkBox.add(new AttributeModifier("title", conformance.getId()));

        Label label = new Label(key+"Label", conformance.getId());
        label.add(new AttributeModifier("title", conformance.getId()));


        if (info.isCiteCompliant()) {
            level.setEnabled( stable && endorsed);
            label.setEnabled( stable  && endorsed );

            checkBox.setEnabled( stable  && endorsed );
        }
        else {
            level.setEnabled( stable );
        }
        add(level);
        add(checkBox);
        add(label);

        return checkBox;
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
        FeatureConformance conformance = new FeatureConformance((WFSInfo) info.getObject());


        // Enable/Disable service
        addConformance("core", FeatureConformance.CORE, new IModel<>() {
            @Override
            public Boolean getObject() {
                FeatureConformance features = new FeatureConformance((WFSInfo) info.getObject());
                return features.isCore();
            }

            @Override
            public void setObject(Boolean object) {
                FeatureConformance features = new FeatureConformance((WFSInfo) info.getObject());
                features.setCore(object);
            }
        });

        IModel<Boolean> builtInModel = () -> true;

        // Required built-in conformance
        addConformance("oas30", FeatureConformance.OAS30, true);
        addConformance("html", FeatureConformance.HTML,true);
        addConformance("geojson", FeatureConformance.GEOJSON,true);

        // optional formats
        addConformance("gmlsf0", FeatureConformance.GMLSF0, new IModel<Boolean>() {
            @Override
            public Boolean getObject() {
                FeatureConformance features = new FeatureConformance((WFSInfo) info.getObject());
                return features.isGMLSFO();
            }

            @Override
            public void setObject(Boolean object) {
                FeatureConformance features = new FeatureConformance((WFSInfo) info.getObject());
                features.setGMLSF0(object);
            }
        });
        addConformance("gmlsf2", FeatureConformance.GMLSF2, new IModel<Boolean>() {
            @Override
            public Boolean getObject() {
                FeatureConformance features = new FeatureConformance((WFSInfo) info.getObject());
                return features.isGMLSF2();
            }

            @Override
            public void setObject(Boolean object) {
                FeatureConformance features = new FeatureConformance((WFSInfo) info.getObject());
                IModel.super.setObject(object);features.setGMLSF2(object);
            }
        });

        // Optional Functionality
        addConformance("crsByReference", FeatureConformance.CRS_BY_REFERENCE,  new IModel<Boolean>() {
            @Override
            public Boolean getObject() {
                FeatureConformance features = new FeatureConformance((WFSInfo) info.getObject());
                return features.isCRSByReference();
            }

            @Override
            public void setObject(Boolean object) {
                FeatureConformance features = new FeatureConformance((WFSInfo) info.getObject());
                features.setCRSByReference(object);
            }
        });
        addConformance("filter", FeatureConformance.FILTER, new IModel<Boolean>() {
            @Override
            public Boolean getObject() {
                FeatureConformance features = new FeatureConformance((WFSInfo) info.getObject());
                return features.isFilter();
            }

            @Override
            public void setObject(Boolean object) {
                FeatureConformance features = new FeatureConformance((WFSInfo) info.getObject());
                features.setFilter(object);
            }
        });
        addConformance("featuresFilter", FeatureConformance.FEATURES_FILTER, new IModel<Boolean>() {
            @Override
            public Boolean getObject() {
                FeatureConformance features = new FeatureConformance((WFSInfo) info.getObject());
                return features.isFeaturesFilter();
            }

            @Override
            public void setObject(Boolean object) {
                FeatureConformance features = new FeatureConformance((WFSInfo) info.getObject());
                features.setFeaturesFilter(object);
            }
        });

        addConformance("queryables",FeatureConformance.QUERYABLES, new IModel<Boolean>() {
            @Override
            public Boolean getObject() {
                FeatureConformance features = new FeatureConformance((WFSInfo) info.getObject());
                return features.isQueryables();
            }

            @Override
            public void setObject(Boolean object) {
                FeatureConformance features = new FeatureConformance((WFSInfo) info.getObject());
                features.setQueryables(object);
            }
        });
        addConformance("ids", FeatureConformance.IDS, new IModel<Boolean>() {
            @Override
            public Boolean getObject() {
                FeatureConformance features = new FeatureConformance((WFSInfo) info.getObject());
                return features.isIDs();
            }

            @Override
            public void setObject(Boolean object) {
                FeatureConformance features = new FeatureConformance((WFSInfo) info.getObject());
                features.setIDs(object);
            }
        });
        addConformance("search", FeatureConformance.SEARCH, new IModel<Boolean>() {
            @Override
            public Boolean getObject() {
                FeatureConformance features = new FeatureConformance((WFSInfo) info.getObject());
                return features.isSearch();
            }

            @Override
            public void setObject(Boolean object) {
                FeatureConformance features = new FeatureConformance((WFSInfo) info.getObject());
                features.setSearch(object);
            }
        });
        addConformance("sortBy", FeatureConformance.SORTBY, new IModel<Boolean>() {
            @Override
            public Boolean getObject() {
                FeatureConformance features = new FeatureConformance((WFSInfo) info.getObject());
                return features.isSortBy();
            }

            @Override
            public void setObject(Boolean object) {
                FeatureConformance features = new FeatureConformance((WFSInfo) info.getObject());
                features.setSortBy(object);
            }
        });
    }

    private void ecqlSettings(IModel<?> info) {
        // ECQL
        addConformance("ecql", ECQLConformance.ECQL, new IModel<Boolean>() {
            @Override
            public Boolean getObject() {
                ECQLConformance ecql = new ECQLConformance((WFSInfo) info.getObject());
                return ecql.isECQL();
            }

            @Override
            public void setObject(Boolean object) {
                ECQLConformance ecql = new ECQLConformance((WFSInfo) info.getObject());
                ecql.setECQL(object);
            }
        });
        addConformance("ecqlText", ECQLConformance.ECQL_TEXT, new IModel<Boolean>() {
            @Override
            public Boolean getObject() {
                ECQLConformance ecql = new ECQLConformance((WFSInfo) info.getObject());
                return ecql.isText();
            }

            @Override
            public void setObject(Boolean object) {
                ECQLConformance ecql = new ECQLConformance((WFSInfo) info.getObject());
                ecql.setText(object);
            }
        });
    }

    private void cql2Settings(IModel<?> info) {
        // CQL2
        addConformance("cql2Text", CQL2Conformance.CQL2_TEXT, new IModel<Boolean>() {
            @Override
            public Boolean getObject() {
                CQL2Conformance cql2 = new CQL2Conformance((WFSInfo) info.getObject());
                return cql2.isText();
            }

            @Override
            public void setObject(Boolean object) {
                CQL2Conformance cql2 = new CQL2Conformance((WFSInfo) info.getObject());
                cql2.setText(object);
            }
        });
        addConformance("cql2JSON", CQL2Conformance.CQL2_JSON, new IModel<Boolean>() {
            @Override
            public Boolean getObject() {
                CQL2Conformance cql2 = new CQL2Conformance((WFSInfo) info.getObject());
                return cql2.isJSON();
            }

            @Override
            public void setObject(Boolean object) {
                CQL2Conformance cql2 = new CQL2Conformance((WFSInfo) info.getObject());
                cql2.setJSON(object);
            }
        });

        IModel<Boolean> builtInModel = () -> true;
        IModel<Boolean> notImplementedModel = () -> false;

        // built-in conformance
        addConformance("cql2Advanced", CQL2Conformance.CQL2_ADVANCED, true );
        addConformance("cql2Arithmetic", CQL2Conformance.CQL2_ARITHMETIC, true );
        addConformance("cql2Array", CQL2Conformance.CQL2_ARITHMETIC, false );
        addConformance("cql2Basic", CQL2Conformance.CQL2_BASIC, true );
        addConformance("cql2BasicSpatial", CQL2Conformance.CQL2_BASIC_SPATIAL, true );
        addConformance("cql2Functions", CQL2Conformance.CQL2_FUNCTIONS, true );
        addConformance("cql2Temporal", CQL2Conformance.CQL2_TEMPORAL, false ); // not implemented
        addConformance("cql2PropertyProperty", CQL2Conformance.CQL2_PROPERTY_PROPERTY, true );
        addConformance("cql2Spatial", CQL2Conformance.CQL2_SPATIAL, true );
    }
}
