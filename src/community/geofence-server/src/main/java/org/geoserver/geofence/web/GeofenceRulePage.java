/* (c) 2015 - 2016 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.geofence.web;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.MultiPolygon;
import com.vividsolutions.jts.geom.Polygon;
import com.vividsolutions.jts.io.WKTReader;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.logging.Level;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.OnChangeAjaxBehavior;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.*;
import org.apache.wicket.markup.html.link.BookmarkablePageLink;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.ResourceModel;
import org.geoserver.catalog.Predicates;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.catalog.util.CloseableIterator;
import org.geoserver.geofence.core.model.RuleLimits;
import org.geoserver.geofence.core.model.enums.GrantType;
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
import org.geoserver.web.wicket.ParamResourceModel;
import org.geotools.factory.CommonFactoryFinder;
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

    private class RuleFormData implements Serializable {

        private static final long serialVersionUID = 3045099348340468123L;

        ShortRule rule;

        RuleLimits ruleLimits;

        String allowedArea;
    }

    protected DropDownChoice<String> userChoice,
            roleChoice,
            serviceChoice,
            requestChoice,
            workspaceChoice,
            layerChoice,
            accessChoice;

    protected DropDownChoice<GrantType> grantTypeChoice;

    protected TextArea<String> allowedArea;

    protected Label allowedAreaLabel;

    public GeofenceRulePage(final ShortRule rule, final GeofenceRulesModel rules) {

        RuleFormData ruleFormData = new RuleFormData();
        ruleFormData.rule = rule;
        final RuleLimits ruleLimits = rules.getRulesLimits(rule.getId());
        if (ruleLimits == null) {
            ruleFormData.ruleLimits = new RuleLimits();
        } else {
            ruleFormData.ruleLimits = ruleLimits;
            ruleFormData.allowedArea = getAllowedAreaAsString(ruleLimits);
        }

        CompoundPropertyModel<RuleFormData> ruleFormModel =
                new CompoundPropertyModel<RuleFormData>(ruleFormData);

        // build the form
        final Form<RuleFormData> form = new Form<RuleFormData>("form", ruleFormModel);
        add(form);

        form.add(
                new TextField<>("priority", ruleFormModel.bind("rule.priority")).setRequired(true));
        form.add(
                roleChoice =
                        new DropDownChoice<>(
                                "roleName", ruleFormModel.bind("rule.roleName"), getRoleNames()));

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

        form.add(
                userChoice =
                        new DropDownChoice<>(
                                "userName",
                                ruleFormModel.bind("rule.userName"),
                                getUserNames(rule.getRoleName())));
        userChoice.setOutputMarkupId(true);
        userChoice.setNullValid(true);

        form.add(
                serviceChoice =
                        new DropDownChoice<>(
                                "service", ruleFormModel.bind("rule.service"), getServiceNames()));
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

        form.add(
                requestChoice =
                        new DropDownChoice<>(
                                "request",
                                ruleFormModel.bind("rule.request"),
                                getOperationNames(rule.getService()),
                                new CaseConversionRenderer()));
        requestChoice.setOutputMarkupId(true);
        requestChoice.setNullValid(true);

        form.add(
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
                        layerChoice.setChoices(getLayerNames(workspaceChoice.getConvertedInput()));
                        form.getModelObject().rule.setLayer(null);
                        layerChoice.modelChanged();
                        target.add(layerChoice);
                    }
                });
        workspaceChoice.setNullValid(true);

        form.add(
                layerChoice =
                        new DropDownChoice<>(
                                "layer",
                                ruleFormModel.bind("rule.layer"),
                                getLayerNames(rule.getWorkspace())));
        layerChoice.setOutputMarkupId(true);
        layerChoice.setNullValid(true);

        form.add(
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
                        if (grantTypeChoice.getConvertedInput().equals(GrantType.LIMIT)) {
                            allowedAreaLabel.setVisible(true);
                            allowedArea.setVisible(true);
                        } else {
                            allowedAreaLabel.setVisible(false);
                            allowedArea.setVisible(false);
                        }
                        target.add(allowedAreaLabel);
                        target.add(allowedArea);
                    }
                });

        form.add(
                allowedAreaLabel =
                        new Label(
                                "allowedAreaLabel",
                                new ResourceModel("allowedArea", "Allow area")));
        allowedAreaLabel.setVisible(
                form.getModelObject().rule.getAccess() != null
                        && form.getModelObject().rule.getAccess().equals(GrantType.LIMIT));
        allowedAreaLabel.setOutputMarkupId(true);
        allowedAreaLabel.setOutputMarkupPlaceholderTag(true);

        form.add(allowedArea = new TextArea<>("allowedArea", ruleFormModel.bind("allowedArea")));
        allowedArea.setConvertedInput(form.getModelObject().allowedArea);
        allowedArea.setVisible(
                form.getModelObject().rule.getAccess() != null
                        && form.getModelObject().rule.getAccess().equals(GrantType.LIMIT));
        allowedArea.setOutputMarkupId(true);
        allowedArea.setOutputMarkupPlaceholderTag(true);

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
                                        parseAllowedArea(ruleFormData.allowedArea));
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

    private String getAllowedAreaAsString(RuleLimits ruleLimits) {
        if (ruleLimits == null || ruleLimits.getAllowedArea() == null) {
            return "";
        }
        MultiPolygon multiPolygon = ruleLimits.getAllowedArea();
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

    protected GeoServerSecurityManager securityManager() {
        return GeoServerApplication.get().getSecurityManager();
    }
}
