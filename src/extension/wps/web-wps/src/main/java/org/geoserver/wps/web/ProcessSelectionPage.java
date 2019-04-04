/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wps.web;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.logging.Level;
import org.apache.wicket.Component;
import org.apache.wicket.extensions.ajax.markup.html.autocomplete.AutoCompleteBehavior;
import org.apache.wicket.extensions.ajax.markup.html.autocomplete.AutoCompleteSettings;
import org.apache.wicket.extensions.ajax.markup.html.autocomplete.IAutoCompleteRenderer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.CheckBox;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.SubmitLink;
import org.apache.wicket.markup.html.form.TextArea;
import org.apache.wicket.markup.html.link.Link;
import org.apache.wicket.markup.html.panel.Fragment;
import org.apache.wicket.model.AbstractReadOnlyModel;
import org.apache.wicket.model.IModel;
import org.geoserver.security.GeoServerRoleService;
import org.geoserver.security.impl.GeoServerRole;
import org.geoserver.security.web.AbstractSecurityPage;
import org.geoserver.web.wicket.GeoServerDataProvider.Property;
import org.geoserver.web.wicket.GeoServerTablePanel;
import org.geoserver.web.wicket.ParamResourceModel;
import org.geoserver.wps.ProcessGroupInfo;
import org.geoserver.wps.ProcessInfo;
import org.geoserver.wps.process.GeoServerProcessors;
import org.geoserver.wps.web.FilteredProcessesProvider.FilteredProcess;
import org.geotools.process.ProcessFactory;
import org.opengis.feature.type.Name;

/**
 * A page listing all WPS process for specific group, allowing enable/disable single process and
 * add/remove roles to grant access to it This page is opened and return to WPS security group
 * management page.
 *
 * @see WPSAccessRulePage
 */
public class ProcessSelectionPage extends AbstractSecurityPage {

    private String title;
    private GeoServerTablePanel<FilteredProcess> processSelector;
    private ProcessGroupInfo pfi;
    private List<String> availableRoles = new ArrayList<String>();

    public ProcessSelectionPage(
            final WPSAccessRulePage wpsAccessRulePage, final ProcessGroupInfo pfi) {
        this.pfi = pfi;

        // prepare the process factory title
        Class<? extends ProcessFactory> factoryClass = pfi.getFactoryClass();
        ProcessFactory pf = GeoServerProcessors.getProcessFactory(factoryClass, false);
        if (pf == null) {
            throw new IllegalArgumentException(
                    "Failed to locate the process factory " + factoryClass);
        }
        this.title = pf.getTitle().toString(getLocale());

        Form form = new Form("form");
        add(form);

        GeoServerRoleService roleService = getSecurityManager().getActiveRoleService();
        try {
            for (GeoServerRole r : roleService.getRoles()) {
                availableRoles.add(r.getAuthority());
            }
        } catch (IOException e1) {
            LOGGER.log(Level.FINER, e1.getMessage(), e1);
        }

        final FilteredProcessesProvider provider = new FilteredProcessesProvider(pfi, getLocale());
        final AutoCompleteSettings settings = new AutoCompleteSettings();
        settings.setShowCompleteListOnFocusGain(false);
        settings.setShowListOnEmptyInput(true);
        settings.setShowListOnFocusGain(true);
        settings.setMaxHeightInPx(100);
        processSelector =
                new GeoServerTablePanel<FilteredProcess>("selectionTable", provider) {

                    @Override
                    protected Component getComponentForProperty(
                            String id,
                            final IModel<FilteredProcess> itemModel,
                            Property<FilteredProcess> property) {
                        if (property.getName().equals("enabled")) {
                            Fragment fragment =
                                    new Fragment(id, "enabledFragment", ProcessSelectionPage.this);
                            CheckBox enabled =
                                    new CheckBox(
                                            "enabled",
                                            (IModel<Boolean>) property.getModel(itemModel));
                            enabled.setOutputMarkupId(true);
                            fragment.add(enabled);
                            return fragment;
                        } else if (property.getName().equals("title")) {
                            return new Label(id, property.getModel(itemModel));
                        } else if (property.getName().equals("description")) {
                            return new Label(id, property.getModel(itemModel));
                        } else if (property.getName().equals("roles")) {
                            Fragment fragment =
                                    new Fragment(id, "rolesFragment", ProcessSelectionPage.this);
                            TextArea<?> roles =
                                    new TextArea("roles", property.getModel(itemModel)) {
                                        public <C extends Object>
                                                org.apache.wicket.util.convert.IConverter<C>
                                                        getConverter(java.lang.Class<C> type) {
                                            return new RolesConverter(availableRoles);
                                        };
                                    };
                            StringBuilder selectedRoles = new StringBuilder();
                            IAutoCompleteRenderer<String> roleRenderer =
                                    new RolesRenderer(selectedRoles);
                            AutoCompleteBehavior<String> b =
                                    new RolesAutoCompleteBehavior(
                                            roleRenderer, settings, selectedRoles, availableRoles);
                            roles.setOutputMarkupId(true);
                            roles.add(b);
                            fragment.add(roles);
                            return fragment;
                        } else if (property.getName().equals("validated")) {
                            final IModel<Boolean> hasValidatorsModel =
                                    (IModel<Boolean>) property.getModel(itemModel);
                            IModel<String> availableModel =
                                    new AbstractReadOnlyModel<String>() {

                                        @Override
                                        public String getObject() {
                                            Boolean value = hasValidatorsModel.getObject();
                                            if (Boolean.TRUE.equals(value)) {
                                                return "*";
                                            } else {
                                                return "";
                                            }
                                        }
                                    };
                            return new Label(id, availableModel);
                        } else if (property.getName().equals("edit")) {
                            Fragment fragment =
                                    new Fragment(id, "linkFragment", ProcessSelectionPage.this);
                            // we use a submit link to avoid losing the other edits in the form
                            Link link =
                                    new Link("link") {
                                        @Override
                                        public void onClick() {
                                            FilteredProcess fp =
                                                    (FilteredProcess) itemModel.getObject();
                                            setResponsePage(
                                                    new ProcessLimitsPage(
                                                            ProcessSelectionPage.this, fp));
                                        }
                                    };
                            fragment.add(link);

                            return fragment;
                        }
                        return null;
                    }
                };
        processSelector.setFilterable(false);
        processSelector.setPageable(false);
        processSelector.setOutputMarkupId(true);
        form.add(processSelector);
        SubmitLink apply =
                new SubmitLink("apply") {
                    @Override
                    public void onSubmit() {
                        // super.onSubmit();
                        pfi.getFilteredProcesses().clear();
                        for (FilteredProcess process : provider.getItems()) {
                            if ((process.getRoles() != null && !process.getRoles().isEmpty())
                                    || !process.getEnabled()
                                    || (process.getValidators() != null
                                            && !process.getValidators().isEmpty())) {
                                ProcessInfo pai = process.toProcessInfo();
                                pfi.getFilteredProcesses().add(pai);
                            }
                        }
                        setResponsePage(wpsAccessRulePage);
                    }
                };
        form.add(apply);
        Link cancel =
                new Link("cancel") {
                    @Override
                    public void onClick() {
                        setResponsePage(wpsAccessRulePage);
                    }
                };
        form.add(cancel);
    }

    protected Collection<? extends Name> getFilteredProcesses() {
        ProcessFactory pf = GeoServerProcessors.getProcessFactory(pfi.getFactoryClass(), false);
        List<Name> disabled = new ArrayList<Name>(pf.getNames());
        for (FilteredProcess fp : processSelector.getSelection()) {
            disabled.remove(fp.getName());
        }

        return disabled;
    }

    @Override
    protected String getDescription() {
        return new ParamResourceModel("description", this, title).getString();
    }
}
