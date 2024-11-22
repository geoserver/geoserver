package org.geoserver.ogcapi.web;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.markup.html.form.CheckBox;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.geoserver.ogcapi.APIConformance;
import org.geoserver.ogcapi.v1.features.FeatureConformance;
import org.geoserver.ogcapi.v1.features.FeatureService;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.web.services.AdminPagePanel;
import org.geoserver.web.util.MapModel;
import org.geoserver.web.util.MetadataMapModel;
import org.geoserver.wfs.WFSInfo;
import org.geoserver.wfs.response.ShapeZipOutputFormat;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ConformanceAdminPanel extends AdminPagePanel {

        public ConformanceAdminPanel(String id, final IModel<?> info) {
            super(id, info);

            WFSInfo wfsInfo = (WFSInfo) info.getObject();

            // Enable/Disable service
            CheckBox core = new CheckBox("core", new IModel<Boolean>() {
                @Override
                public Boolean getObject() {
                    FeatureConformance features = new FeatureConformance((WFSInfo) info.getObject());
                    return features.isEnabled();
                }

                @Override
                public void setObject(Boolean object) {
                    FeatureConformance features = new FeatureConformance((WFSInfo) info.getObject());
                    features.setEnabled(object);
                }
            });
            core.add(new AttributeModifier("title", FeatureConformance.CORE.getId()));
            add(core);

            // Required
            CheckBox oas30 = new CheckBox("oas30", (IModel<Boolean>) () -> true);
            oas30.setEnabled(false);
            oas30.add(new AttributeModifier("title", FeatureConformance.OAS30.getId()));
            add(oas30);

            CheckBox html = new CheckBox("html", (IModel<Boolean>) () -> true);
            html.setEnabled(false);
            html.add(new AttributeModifier("title", FeatureConformance.HTML.getId()));
            add(html);

            CheckBox geojson = new CheckBox("geojson", (IModel<Boolean>) () -> true);
            geojson.setEnabled(false);
            geojson.add(new AttributeModifier("title", FeatureConformance.GEOJSON.getId()));
            add(geojson);

            // Optional Functionality
            CheckBox crsByReference = new CheckBox("crsByReference", new IModel<Boolean>() {
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
            crsByReference.add(new AttributeModifier("title", FeatureConformance.CRS_BY_REFERENCE.getId()));
            add(crsByReference);

            CheckBox featureFilter = new CheckBox("featuresFilter", new IModel<Boolean>() {
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
            featureFilter.add(new AttributeModifier("title", FeatureConformance.FEATURES_FILTER.getId()));
            add(featureFilter);

        }
}
