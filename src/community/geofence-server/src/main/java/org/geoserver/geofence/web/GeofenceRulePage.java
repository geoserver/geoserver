/* (c) 2015 - 2016 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.geofence.web;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.logging.Level;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.OnChangeAjaxBehavior;
import org.apache.wicket.extensions.markup.html.form.palette.Palette;
import org.apache.wicket.extensions.markup.html.form.palette.theme.DefaultTheme;
import org.apache.wicket.extensions.markup.html.tabs.AbstractTab;
import org.apache.wicket.extensions.markup.html.tabs.TabbedPanel;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.*;
import org.apache.wicket.markup.html.link.BookmarkablePageLink;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.ResourceModel;
import org.geoserver.catalog.AttributeTypeInfo;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.Predicates;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.catalog.StyleInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.catalog.util.CloseableIterator;
import org.geoserver.geofence.core.model.LayerAttribute;
import org.geoserver.geofence.core.model.LayerDetails;
import org.geoserver.geofence.core.model.RuleLimits;
import org.geoserver.geofence.core.model.enums.AccessType;
import org.geoserver.geofence.core.model.enums.CatalogMode;
import org.geoserver.geofence.core.model.enums.GrantType;
import org.geoserver.geofence.core.model.enums.LayerType;
import org.geoserver.geofence.services.dto.ShortRule;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.platform.Service;
import org.geoserver.platform.exception.GeoServerRuntimException;
import org.geoserver.security.GeoServerRoleService;
import org.geoserver.security.GeoServerSecurityManager;
import org.geoserver.security.impl.GeoServerRole;
import org.geoserver.security.impl.GeoServerUser;
import org.geoserver.web.GeoServerApplication;
import org.geoserver.web.GeoServerSecuredPage;
import org.geoserver.web.wicket.GeoServerDataProvider.Property;
import org.geoserver.web.wicket.GeoServerTablePanel;
import org.geoserver.web.wicket.ParamResourceModel;
import org.geoserver.web.wicket.SimpleChoiceRenderer;
import org.geotools.factory.CommonFactoryFinder;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.MultiPolygon;
import org.locationtech.jts.geom.Polygon;
import org.locationtech.jts.io.WKTReader;
import org.opengis.filter.FilterFactory2;
import org.opengis.filter.sort.SortOrder;
import org.springframework.dao.DuplicateKeyException;

/**
 * Internal Geofence Rule Page
 *
 * @author Niels Charlier
 */
public class GeofenceRulePage extends GeoServerSecuredPage {

    private static final long serialVersionUID = -3986495664060319256L;

    private class LayerDetailsFormData implements Serializable {

        private static final long serialVersionUID = 3045099348340468123L;

        LayerType layerType;

        String defaultStyle;

        String cqlFilterRead;

        String cqlFilterWrite;

        String allowedArea;

        CatalogMode catalogMode;

        Set<String> allowedStyles = new HashSet<String>();

        List<LayerAttribute> attributes = new ArrayList<LayerAttribute>();
    }

    private class RuleFormData implements Serializable {

        private static final long serialVersionUID = 3045099348340468123L;

        ShortRule rule;

        String allowedArea;

        CatalogMode catalogMode;

        boolean layerDetailsCheck;

        LayerDetailsFormData layerDetails = new LayerDetailsFormData();
    }

    final CompoundPropertyModel<RuleFormData> ruleFormModel;

    // build the form
    final Form<RuleFormData> form;

    final TabbedPanel<AbstractTab> tabbedPanel;

    public GeofenceRulePage(final ShortRule rule, final GeofenceRulesModel rules) {

        RuleFormData ruleFormData = new RuleFormData();
        ruleFormData.rule = rule;
        final RuleLimits ruleLimits = rules.getRulesLimits(rule.getId());
        if (ruleLimits != null) {
            ruleFormData.allowedArea = getAllowedAreaAsString(ruleLimits.getAllowedArea());
            ruleFormData.catalogMode = ruleLimits.getCatalogMode();
        }
        final LayerDetails layerDetails = rules.getDetails(rule.getId());
        if (layerDetails != null) {
            ruleFormData.layerDetailsCheck = true;
            ruleFormData.layerDetails.allowedArea = getAllowedAreaAsString(layerDetails.getArea());
            ruleFormData.layerDetails.catalogMode = layerDetails.getCatalogMode();
            ruleFormData.layerDetails.cqlFilterRead = layerDetails.getCqlFilterRead();
            ruleFormData.layerDetails.cqlFilterWrite = layerDetails.getCqlFilterWrite();
            ruleFormData.layerDetails.defaultStyle = layerDetails.getDefaultStyle();
            ruleFormData.layerDetails.layerType = layerDetails.getType();
            ruleFormData.layerDetails.allowedStyles.addAll(layerDetails.getAllowedStyles());
            ruleFormData.layerDetails.attributes.addAll(layerDetails.getAttributes());
        }

        ruleFormModel = new CompoundPropertyModel<RuleFormData>(ruleFormData);

        // build the form
        form = new Form<RuleFormData>("form", ruleFormModel);
        add(form);

        List<AbstractTab> tabs = new ArrayList<>();
        tabs.add(
                new AbstractTab(new ResourceModel("general")) {
                    private static final long serialVersionUID = 446471321863431295L;

                    public Panel getPanel(String panelId) {
                        return new GeneralPanel(panelId);
                    }
                });
        tabs.add(
                new AbstractTab(new ResourceModel("details")) {
                    private static final long serialVersionUID = 446471321863431295L;

                    public Panel getPanel(String panelId) {
                        return new LayerDetailsPanel(panelId);
                    }
                });
        form.add(
                tabbedPanel =
                        new TabbedPanel<AbstractTab>("tabs", tabs) {
                            private static final long serialVersionUID = 2194590643994737914L;

                            @Override
                            protected WebMarkupContainer newLink(String linkId, final int index) {
                                return new SubmitLink(linkId) {
                                    private static final long serialVersionUID =
                                            4072507303411443283L;

                                    @Override
                                    public void onSubmit() {
                                        setSelectedTab(index);
                                    }
                                };
                            }
                        });

        // build the submit/cancel
        form.add(
                new SubmitLink("save") {
                    private static final long serialVersionUID = 3735176778941168701L;

                    @Override
                    public void onSubmit() {
                        RuleFormData ruleFormData = (RuleFormData) getForm().getModelObject();
                        try {
                            rules.save(ruleFormData.rule);
                            if (ruleFormData.rule.getAccess().equals(GrantType.LIMIT)) {
                                rules.save(
                                        ruleFormData.rule.getId(),
                                        parseAllowedArea(ruleFormData.allowedArea),
                                        ruleFormData.catalogMode);
                            }
                            if (ruleFormData.layerDetailsCheck) {
                                LayerDetails layerDetails = new LayerDetails();
                                layerDetails.setArea(
                                        parseAllowedArea(ruleFormData.layerDetails.allowedArea));
                                layerDetails.setAttributes(
                                        new HashSet<LayerAttribute>(
                                                ruleFormData.layerDetails.attributes));
                                layerDetails.setAllowedStyles(
                                        ruleFormData.layerDetails.allowedStyles);
                                layerDetails.setCatalogMode(ruleFormData.layerDetails.catalogMode);
                                layerDetails.setCqlFilterRead(
                                        ruleFormData.layerDetails.cqlFilterRead);
                                layerDetails.setCqlFilterWrite(
                                        ruleFormData.layerDetails.cqlFilterWrite);
                                layerDetails.setDefaultStyle(
                                        ruleFormData.layerDetails.defaultStyle);
                                layerDetails.setType((ruleFormData.layerDetails.layerType));
                                rules.save(ruleFormData.rule.getId(), layerDetails);
                            } else {
                                rules.save(ruleFormData.rule.getId(), (LayerDetails) null);
                            }
                            doReturn(GeofenceServerPage.class);
                        } catch (DuplicateKeyException e) {
                            error(new ResourceModel("GeofenceRulePage.duplicate").getObject());
                        } catch (Exception e) {
                            error(e);
                        }
                    }
                });
        form.add(new BookmarkablePageLink<ShortRule>("cancel", GeofenceServerPage.class));
    }

    private String getAllowedAreaAsString(MultiPolygon multiPolygon) {
        if (multiPolygon == null) {
            return "";
        }
        return "SRID=" + multiPolygon.getSRID() + ";" + multiPolygon.toText();
    }

    private MultiPolygon parseAllowedArea(String allowedArea) {
        if (allowedArea == null || allowedArea.isEmpty()) {
            return null;
        }
        String[] allowedAreaParts = allowedArea.split(";");
        if (allowedAreaParts.length != 2) {
            throw new GeoServerRuntimException(
                    String.format(
                            "Invalid allowed area '%s' expecting SRID=<CODE>;<WKT>.", allowedArea));
        }
        Integer srid;
        Geometry geometry;
        try {
            srid = Integer.valueOf(allowedAreaParts[0].split("=")[1]);
            geometry = new WKTReader().read(allowedAreaParts[1]);
        } catch (Exception exception) {
            String message =
                    String.format(
                            "Error parsing SRID '%s' or WKT geometry '%s' expecting SRID=<CODE>;<WKT>.",
                            allowedAreaParts[0], allowedAreaParts[1]);
            LOGGER.log(Level.WARNING, message, exception);
            throw new GeoServerRuntimException(message, exception);
        }
        MultiPolygon multiPolygon = castToMultiPolygon(geometry);
        multiPolygon.setSRID(srid);
        return multiPolygon;
    }

    private MultiPolygon castToMultiPolygon(Geometry geometry) {
        if (geometry instanceof MultiPolygon) {
            return (MultiPolygon) geometry;
        }
        if (geometry instanceof Polygon) {
            return new MultiPolygon(new Polygon[] {(Polygon) geometry}, new GeometryFactory());
        }
        throw new GeoServerRuntimException(
                String.format(
                        "Invalid geometry of type '%s' expect a Polygon or MultiPolygon.",
                        geometry.getClass().getSimpleName()));
    }

    /** Returns a sorted list of workspace names */
    protected List<String> getWorkspaceNames() {

        SortedSet<String> resultSet = new TreeSet<String>();
        for (WorkspaceInfo ws : getCatalog().getFacade().getWorkspaces()) {
            resultSet.add(ws.getName());
        }
        return new ArrayList<String>(resultSet);
    }

    /**
     * Returns a sorted list of layer names in the specified workspace (or * if the workspace is *)
     */
    protected List<String> getLayerNames(String workspaceName) {
        List<String> resultSet = new ArrayList<String>();
        if (workspaceName != null) {
            FilterFactory2 ff = CommonFactoryFinder.getFilterFactory2();

            try (CloseableIterator<ResourceInfo> it =
                    getCatalog()
                            .getFacade()
                            .list(
                                    ResourceInfo.class,
                                    Predicates.equal("store.workspace.name", workspaceName),
                                    null,
                                    null,
                                    ff.sort("name", SortOrder.ASCENDING))) {
                while (it.hasNext()) {
                    resultSet.add(it.next().getName());
                }
            }
        }

        return resultSet;
    }

    /** Returns a sorted list of workspace names */
    protected List<String> getServiceNames() {
        SortedSet<String> resultSet = new TreeSet<String>();
        for (Service ows : GeoServerExtensions.extensions(Service.class)) {
            resultSet.add(ows.getId().toUpperCase());
        }
        return new ArrayList<String>(resultSet);
    }

    /**
     * Returns a sorted list of operation names in the specified service (or * if the workspace is
     * *)
     */
    protected List<String> getOperationNames(String serviceName) {
        SortedSet<String> resultSet = new TreeSet<String>();
        boolean flag = true;
        if (serviceName != null) {
            for (Service ows : GeoServerExtensions.extensions(Service.class)) {
                if (serviceName.equalsIgnoreCase(ows.getId()) && flag) {
                    flag = false;
                    resultSet.addAll(ows.getOperations());
                }
            }
        }
        return new ArrayList<String>(resultSet);
    }

    protected List<String> getRoleNames() {
        SortedSet<String> resultSet = new TreeSet<String>();
        try {
            for (GeoServerRole role : securityManager().getRolesForAccessControl()) {
                resultSet.add(role.getAuthority());
            }
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, e.getLocalizedMessage(), e);
        }
        return new ArrayList<String>(resultSet);
    }

    protected List<String> getUserNames(String roleName) {
        SortedSet<String> resultSet = new TreeSet<String>();

        GeoServerSecurityManager securityManager = securityManager();
        try {
            if (roleName == null) {
                for (String serviceName : securityManager.listUserGroupServices()) {
                    for (GeoServerUser user :
                            securityManager.loadUserGroupService(serviceName).getUsers()) {
                        resultSet.add(user.getUsername());
                    }
                }
            } else {
                for (String serviceName : securityManager.listRoleServices()) {
                    GeoServerRoleService roleService = securityManager.loadRoleService(serviceName);
                    GeoServerRole role = roleService.getRoleByName(roleName);
                    if (role != null) {
                        resultSet.addAll(roleService.getUserNamesForRole(role));
                    }
                }
            }
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, e.getLocalizedMessage(), e);
        }

        return new ArrayList<String>(resultSet);
    }

    protected GeoServerSecurityManager securityManager() {
        return GeoServerApplication.get().getSecurityManager();
    }

    protected class GeneralPanel extends Panel {

        private static final long serialVersionUID = 8124810941260273620L;

        protected DropDownChoice<String> userChoice,
                roleChoice,
                serviceChoice,
                requestChoice,
                workspaceChoice,
                layerChoice,
                accessChoice;

        protected DropDownChoice<GrantType> grantTypeChoice;

        protected DropDownChoice<CatalogMode> catalogModeChoice;

        protected TextArea<String> allowedArea;

        protected Label allowedAreaLabel;

        protected Label catalogModeChoiceLabel;

        public GeneralPanel(String id) {
            super(id);

            add(new TextField<>("priority", ruleFormModel.bind("rule.priority")).setRequired(true));
            add(
                    roleChoice =
                            new DropDownChoice<>(
                                    "roleName",
                                    ruleFormModel.bind("rule.roleName"),
                                    getRoleNames()));

            roleChoice.add(
                    new OnChangeAjaxBehavior() {
                        private static final long serialVersionUID = -2880886409750911044L;

                        @Override
                        protected void onUpdate(AjaxRequestTarget target) {
                            userChoice.setChoices(getUserNames(roleChoice.getConvertedInput()));
                            form.getModelObject().rule.setUserName(null);
                            userChoice.modelChanged();
                            target.add(userChoice);
                        }
                    });
            roleChoice.setNullValid(true);

            add(
                    userChoice =
                            new DropDownChoice<>(
                                    "userName",
                                    ruleFormModel.bind("rule.userName"),
                                    getUserNames(ruleFormModel.getObject().rule.getRoleName())));
            userChoice.setOutputMarkupId(true);
            userChoice.setNullValid(true);

            add(
                    serviceChoice =
                            new DropDownChoice<>(
                                    "service",
                                    ruleFormModel.bind("rule.service"),
                                    getServiceNames()));
            serviceChoice.add(
                    new OnChangeAjaxBehavior() {
                        private static final long serialVersionUID = -5925784823433092831L;

                        @Override
                        protected void onUpdate(AjaxRequestTarget target) {
                            requestChoice.setChoices(
                                    getOperationNames(serviceChoice.getConvertedInput()));
                            form.getModelObject().rule.setRequest(null);
                            requestChoice.modelChanged();
                            target.add(requestChoice);
                        }
                    });
            serviceChoice.setNullValid(true);

            add(
                    requestChoice =
                            new DropDownChoice<>(
                                    "request",
                                    ruleFormModel.bind("rule.request"),
                                    getOperationNames(ruleFormModel.getObject().rule.getService()),
                                    new CaseConversionRenderer()));
            requestChoice.setOutputMarkupId(true);
            requestChoice.setNullValid(true);

            add(
                    workspaceChoice =
                            new DropDownChoice<>(
                                    "workspace",
                                    ruleFormModel.bind("rule.workspace"),
                                    getWorkspaceNames()));
            workspaceChoice.add(
                    new OnChangeAjaxBehavior() {
                        private static final long serialVersionUID = 732177308220189475L;

                        @Override
                        protected void onUpdate(AjaxRequestTarget target) {
                            layerChoice.setChoices(
                                    getLayerNames(workspaceChoice.getConvertedInput()));
                            form.getModelObject().rule.setLayer(null);
                            layerChoice.modelChanged();
                            target.add(layerChoice);
                        }
                    });
            workspaceChoice.setNullValid(true);

            add(
                    layerChoice =
                            new DropDownChoice<>(
                                    "layer",
                                    ruleFormModel.bind("rule.layer"),
                                    getLayerNames(ruleFormModel.getObject().rule.getWorkspace())));
            layerChoice.setOutputMarkupId(true);
            layerChoice.setNullValid(true);
            layerChoice.add(
                    new OnChangeAjaxBehavior() {
                        private static final long serialVersionUID = 8434775615039939193L;

                        @Override
                        protected void onUpdate(AjaxRequestTarget target) {
                            ruleFormModel.getObject().layerDetailsCheck =
                                    ruleFormModel.getObject().layerDetailsCheck
                                            && GrantType.ALLOW.equals(
                                                    grantTypeChoice.getConvertedInput())
                                            && layerChoice.getConvertedInput() != null;

                            ruleFormModel.getObject().layerDetails.attributes.clear();
                            if (layerChoice.getConvertedInput() != null) {
                                LayerInfo li =
                                        getCatalog()
                                                .getLayerByName(layerChoice.getConvertedInput());
                                switch (li.getType()) {
                                    case VECTOR:
                                    case REMOTE:
                                        ruleFormModel.getObject().layerDetails.layerType =
                                                LayerType.VECTOR;
                                        break;
                                    case RASTER:
                                    case WMS:
                                    case WMTS:
                                        ruleFormModel.getObject().layerDetails.layerType =
                                                LayerType.VECTOR;
                                        break;
                                    case GROUP:
                                        ruleFormModel.getObject().layerDetails.layerType =
                                                LayerType.LAYERGROUP;
                                        break;
                                }

                                if (li.getResource() instanceof FeatureTypeInfo) {
                                    FeatureTypeInfo fti = (FeatureTypeInfo) li.getResource();
                                    try {
                                        for (AttributeTypeInfo ati : fti.attributes()) {
                                            ruleFormModel
                                                    .getObject()
                                                    .layerDetails
                                                    .attributes
                                                    .add(
                                                            new LayerAttribute(
                                                                    ati.getName(),
                                                                    ati.getBinding() == null
                                                                            ? null
                                                                            : ati.getBinding()
                                                                                    .getName(),
                                                                    AccessType.NONE));
                                        }
                                    } catch (IOException e) {
                                        LOGGER.log(Level.WARNING, "Could not fetch attributes.", e);
                                    }
                                }
                            }
                        }
                    });

            add(
                    grantTypeChoice =
                            new DropDownChoice<>(
                                    "access",
                                    ruleFormModel.bind("rule.access"),
                                    Arrays.asList(GrantType.values()),
                                    new GrantTypeRenderer()));
            grantTypeChoice.setRequired(true);

            grantTypeChoice.add(
                    new OnChangeAjaxBehavior() {

                        private static final long serialVersionUID = -4302901248019983282L;

                        @Override
                        protected void onUpdate(AjaxRequestTarget target) {
                            boolean isLimit =
                                    grantTypeChoice.getConvertedInput().equals(GrantType.LIMIT);
                            allowedAreaLabel.setVisible(isLimit);
                            allowedArea.setVisible(isLimit);
                            catalogModeChoice.setVisible(isLimit);
                            catalogModeChoiceLabel.setVisible(isLimit);

                            target.add(allowedAreaLabel);
                            target.add(allowedArea);
                            target.add(catalogModeChoice);
                            target.add(catalogModeChoiceLabel);

                            ruleFormModel.getObject().layerDetailsCheck =
                                    ruleFormModel.getObject().layerDetailsCheck
                                            && (grantTypeChoice.getConvertedInput() != null
                                                    && grantTypeChoice
                                                            .getConvertedInput()
                                                            .equals(GrantType.ALLOW))
                                            && (layerChoice.getConvertedInput() != null
                                                    && !layerChoice.getConvertedInput().isEmpty());
                        }
                    });

            add(
                    allowedAreaLabel =
                            new Label(
                                    "allowedAreaLabel",
                                    new ResourceModel("allowedArea", "Allow area")));
            allowedAreaLabel.setVisible(
                    form.getModelObject().rule.getAccess() != null
                            && form.getModelObject().rule.getAccess().equals(GrantType.LIMIT));
            allowedAreaLabel.setOutputMarkupId(true);
            allowedAreaLabel.setOutputMarkupPlaceholderTag(true);

            add(allowedArea = new TextArea<>("allowedArea", ruleFormModel.bind("allowedArea")));
            allowedArea.setVisible(
                    form.getModelObject().rule.getAccess() != null
                            && form.getModelObject().rule.getAccess().equals(GrantType.LIMIT));
            allowedArea.setOutputMarkupId(true);
            allowedArea.setOutputMarkupPlaceholderTag(true);

            add(
                    catalogModeChoiceLabel =
                            new Label(
                                    "catalogModeLabel",
                                    new ResourceModel("catalogMode", "Catalog Mode")));
            catalogModeChoiceLabel.setVisible(
                    form.getModelObject().rule.getAccess() != null
                            && form.getModelObject().rule.getAccess().equals(GrantType.LIMIT));
            catalogModeChoiceLabel.setOutputMarkupId(true);
            catalogModeChoiceLabel.setOutputMarkupPlaceholderTag(true);

            add(
                    catalogModeChoice =
                            new DropDownChoice<>(
                                    "catalogMode",
                                    ruleFormModel.bind("catalogMode"),
                                    Arrays.asList(CatalogMode.values()),
                                    new CatalogModeRenderer()));
            catalogModeChoice.setVisible(
                    form.getModelObject().rule.getAccess() != null
                            && form.getModelObject().rule.getAccess().equals(GrantType.LIMIT));
            catalogModeChoice.setOutputMarkupId(true);
            catalogModeChoice.setOutputMarkupPlaceholderTag(true);
        }
    }

    protected class LayerDetailsPanel extends Panel {

        private static final long serialVersionUID = 2996490022169801394L;

        // private AjaxSubmitLink remove;

        public LayerDetailsPanel(String id) {
            super(id);

            final CheckBox layerDetailsCheck =
                    new CheckBox("layerDetailsCheck", ruleFormModel.bind("layerDetailsCheck"));
            layerDetailsCheck.setOutputMarkupId(true);
            layerDetailsCheck.setEnabled(
                    ruleFormModel.getObject().rule.getLayer() != null
                            && ruleFormModel.getObject().rule.getAccess().equals(GrantType.ALLOW));
            add(layerDetailsCheck);

            final WebMarkupContainer container = new WebMarkupContainer("layerDetailsContainer");
            add(container);
            container.setVisible(ruleFormModel.getObject().layerDetailsCheck);
            container.setOutputMarkupId(true);
            container.setOutputMarkupPlaceholderTag(true);

            layerDetailsCheck.add(
                    new OnChangeAjaxBehavior() {
                        private static final long serialVersionUID = 8280700310745922486L;

                        @Override
                        protected void onUpdate(AjaxRequestTarget target) {
                            container.setVisible(layerDetailsCheck.getConvertedInput());
                            target.add(container);
                        }
                    });

            DropDownChoice<LayerType> layerType =
                    new DropDownChoice<>(
                            "layerType",
                            ruleFormModel.bind("layerDetails.layerType"),
                            Arrays.asList(LayerType.values()),
                            new LayerTypeRenderer());
            layerType.setEnabled(false);
            container.add(layerType);

            DropDownChoice<String> defaultStyle =
                    new DropDownChoice<>(
                            "defaultStyle",
                            ruleFormModel.bind("layerDetails.defaultStyle"),
                            getStyles());
            container.add(defaultStyle);

            Palette<String> allowedStyles =
                    new Palette<String>(
                            "allowedStyles",
                            ruleFormModel.bind("layerDetails.allowedStyles"),
                            new Model<ArrayList<String>>(getStyles()),
                            new SimpleChoiceRenderer<String>(),
                            10,
                            false) {

                        private static final long serialVersionUID = 4843969600809421536L;

                        /** Override otherwise the header is not i18n'ized */
                        @Override
                        public Component newSelectedHeader(final String componentId) {
                            return new Label(
                                    componentId,
                                    new ResourceModel("ExtraStylesPalette.selectedHeader"));
                        }

                        /** Override otherwise the header is not i18n'ized */
                        @Override
                        public Component newAvailableHeader(final String componentId) {
                            return new Label(
                                    componentId,
                                    new ResourceModel("ExtraStylesPalette.availableHeader"));
                        }
                    };
            container.add(allowedStyles);
            allowedStyles.add(new DefaultTheme());

            TextArea<String> cqlFilterRead =
                    new TextArea<>(
                            "cqlFilterRead", ruleFormModel.bind("layerDetails.cqlFilterRead"));
            container.add(cqlFilterRead);

            TextArea<String> cqlFilterWrite =
                    new TextArea<>(
                            "cqlFilterWrite", ruleFormModel.bind("layerDetails.cqlFilterWrite"));
            container.add(cqlFilterWrite);

            TextArea<String> allowedArea =
                    new TextArea<>("allowedArea", ruleFormModel.bind("layerDetails.allowedArea"));
            container.add(allowedArea);

            container.add(
                    new DropDownChoice<>(
                            "catalogMode",
                            ruleFormModel.bind("layerDetails.catalogMode"),
                            Arrays.asList(CatalogMode.values()),
                            new CatalogModeRenderer()));

            Label layerAttsLabel =
                    new Label(
                            "layerAttributesLabel",
                            new ResourceModel("layerAttributes", "Layer Attributes"));
            container.add(layerAttsLabel);

            GeoServerTablePanel<LayerAttribute> layerAttsTable;
            container.add(
                    layerAttsTable =
                            new GeoServerTablePanel<LayerAttribute>(
                                    "layerAttributes",
                                    new LayerAttributeModel(
                                            ruleFormModel.getObject().layerDetails.attributes),
                                    true) {
                                private static final long serialVersionUID = -2001227609501100452L;

                                @SuppressWarnings("unchecked")
                                @Override
                                protected Component getComponentForProperty(
                                        String id,
                                        IModel<LayerAttribute> itemModel,
                                        Property<LayerAttribute> property) {
                                    if (LayerAttributeModel.ACCESS.equals(property)) {
                                        return new DropDownChoiceWrapperPanel<AccessType>(
                                                id,
                                                (IModel<AccessType>) property.getModel(itemModel),
                                                Arrays.asList(AccessType.values()),
                                                new AccessTypeRenderer());
                                    }
                                    return null;
                                }
                            });
            layerAttsTable.setFilterVisible(false);
            layerAttsTable.setPageable(false);
            layerAttsTable.setSortable(false);
            layerAttsTable.setSelectable(false);
            layerAttsTable.setOutputMarkupId(true);

            layerAttsLabel.setVisible(!ruleFormModel.getObject().layerDetails.attributes.isEmpty());
            layerAttsTable.setVisible(!ruleFormModel.getObject().layerDetails.attributes.isEmpty());
        }
    }

    private ArrayList<String> getStyles() {
        ArrayList<String> styleNames = new ArrayList<String>();
        for (StyleInfo si : getCatalog().getStyles()) {
            styleNames.add(si.getName());
        }
        return styleNames;
    }

    /** Makes sure we see translated text, by the raw name is used for the model */
    protected class LayerTypeRenderer extends ChoiceRenderer<LayerType> {
        private static final long serialVersionUID = -7478943956804313995L;

        public Object getDisplayValue(LayerType object) {
            return (String) new ParamResourceModel(object.name(), getPage()).getObject();
        }

        public String getIdValue(LayerType object, int index) {
            return object.name();
        }
    }

    /** Makes sure we see translated text, by the raw name is used for the model */
    protected class GrantTypeRenderer extends ChoiceRenderer<GrantType> {
        private static final long serialVersionUID = -7478943956804313995L;

        public Object getDisplayValue(GrantType object) {
            return (String) new ParamResourceModel(object.name(), getPage()).getObject();
        }

        public String getIdValue(GrantType object, int index) {
            return object.name();
        }
    }

    /** Makes sure we see translated text, by the raw name is used for the model */
    protected class AccessTypeRenderer extends ChoiceRenderer<AccessType> {
        private static final long serialVersionUID = -7478943956804313995L;

        public Object getDisplayValue(AccessType object) {
            return (String) new ParamResourceModel(object.name(), getPage()).getObject();
        }

        public String getIdValue(AccessType object, int index) {
            return object.name();
        }
    }

    /** Makes sure we see translated text, by the raw name is used for the model */
    protected class CatalogModeRenderer extends ChoiceRenderer<CatalogMode> {
        private static final long serialVersionUID = -7478943956804313995L;

        public Object getDisplayValue(CatalogMode object) {
            return (String) new ParamResourceModel(object.name(), getPage()).getObject();
        }

        public String getIdValue(CatalogMode object, int index) {
            return object.name();
        }
    }

    /** Makes sure that while rendered in mixed case, is stored in uppercase */
    protected class CaseConversionRenderer extends ChoiceRenderer<String> {
        private static final long serialVersionUID = 4238195087731806209L;

        public Object getDisplayValue(String object) {
            return object;
        }

        public String getIdValue(String object, int index) {
            return object.toUpperCase();
        }
    }

    protected class DropDownChoiceWrapperPanel<T> extends Panel {

        private static final long serialVersionUID = 5677425055959281304L;

        public DropDownChoiceWrapperPanel(
                String id, IModel<T> model, List<? extends T> list, ChoiceRenderer<T> renderer) {
            super(id, model);
            add(new DropDownChoice<T>("innerComponent", model, list, renderer).setRequired(true));
        }
    }
}
