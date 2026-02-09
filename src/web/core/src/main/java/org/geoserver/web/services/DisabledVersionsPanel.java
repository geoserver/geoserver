/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.services;

import java.io.Serial;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import org.apache.wicket.Component;
import org.apache.wicket.behavior.Behavior;
import org.apache.wicket.extensions.markup.html.form.palette.Palette;
import org.apache.wicket.extensions.markup.html.form.palette.component.Recorder;
import org.apache.wicket.extensions.markup.html.form.palette.theme.DefaultTheme;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.FormComponentPanel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.ResourceModel;
import org.apache.wicket.model.util.CollectionModel;
import org.geoserver.ows.util.RequestUtils;
import org.geoserver.web.wicket.SimpleChoiceRenderer;
import org.geotools.util.Version;

/** Panel for selecting disabled service versions using a palette component. */
public class DisabledVersionsPanel extends FormComponentPanel<List<Version>> {

    @Serial
    private static final long serialVersionUID = 1L;

    protected Palette<Version> palette;
    List<Behavior> toAdd = new ArrayList<>();

    /**
     * Creates a new DisabledVersionsPanel
     *
     * @param id Component id
     * @param model Model holding the list of disabled versions
     * @param serviceType The service type
     */
    public DisabledVersionsPanel(String id, IModel<List<Version>> model, String serviceType) {
        super(id, model);

        List<String> versionStrings = RequestUtils.getSupportedVersions(serviceType);
        if (versionStrings.isEmpty()) {
            versionStrings = getVersionsFromServiceInfo(serviceType);
        }

        List<Version> availableVersions =
                versionStrings.stream().map(Version::new).collect(Collectors.toList());

        IModel<Collection<Version>> choicesModel = new CollectionModel<>(availableVersions);

        // wrap the model to ensure it never returns null (Palette requires non-null collection every time)
        IModel<Collection<Version>> safeModel = new IModel<>() {
            @Serial
            private static final long serialVersionUID = 1L;

            @Override
            public Collection<Version> getObject() {
                List<Version> list = model.getObject();
                if (list == null) {
                    list = new ArrayList<>();
                    model.setObject(list);
                }
                return list;
            }

            @Override
            public void setObject(Collection<Version> object) {
                // get the underlying list and modify it in-place
                List<Version> list = model.getObject();
                if (list == null) {
                    model.setObject(object != null ? new ArrayList<>(object) : new ArrayList<>());
                } else {
                    list.clear();
                    if (object != null) {
                        list.addAll(object);
                    }
                }
            }

            @Override
            public void detach() {
                model.detach();
            }
        };

        add(
                palette =
                        new Palette<>(
                                "palette",
                                safeModel,
                                choicesModel,
                                new SimpleChoiceRenderer<>() {
                                    @Serial
                                    private static final long serialVersionUID = 1L;

                                    @Override
                                    public Object getDisplayValue(Version object) {
                                        return object.toString();
                                    }

                                    @Override
                                    public String getIdValue(Version object, int index) {
                                        return object.toString();
                                    }
                                },
                                10,
                                false) {
                            @Serial
                            private static final long serialVersionUID = 1L;

                            @Override
                            protected Recorder<Version> newRecorderComponent() {
                                Recorder<Version> rec = super.newRecorderComponent();
                                rec.add(toAdd.toArray(new Behavior[toAdd.size()]));
                                toAdd.clear();
                                return rec;
                            }

                            @Override
                            public Component newSelectedHeader(final String componentId) {
                                return new Label(componentId, new ResourceModel(getSelectedHeaderPropertyKey()));
                            }

                            @Override
                            public Component newAvailableHeader(final String componentId) {
                                return new Label(componentId, new ResourceModel(getAvailableHeaderPropertyKey()));
                            }
                        });

        palette.add(new DefaultTheme());
        palette.setOutputMarkupId(true);
    }

    /** @return the property key for the selected header, subclasses may override */
    protected String getSelectedHeaderPropertyKey() {
        return "DisabledVersionsPanel.selectedHeader";
    }

    /** @return the property key for the available header, subclasses may override */
    protected String getAvailableHeaderPropertyKey() {
        return "DisabledVersionsPanel.availableHeader";
    }

    /**
     * Get versions from ServiceInfo when they're not available via RequestUtils. This is needed for services like WMTS
     * that don't register operation beans.
     */
    private List<String> getVersionsFromServiceInfo(String serviceType) {
        try {
            org.geoserver.platform.GeoServerExtensions extensions = new org.geoserver.platform.GeoServerExtensions();
            org.geoserver.config.GeoServer geoServer = extensions.bean(org.geoserver.config.GeoServer.class);
            if (geoServer != null) {
                for (org.geoserver.config.ServiceInfo service : geoServer.getServices()) {
                    if (serviceType.equalsIgnoreCase(service.getName())) {
                        return service.getVersions().stream()
                                .map(Version::toString)
                                .collect(Collectors.toList());
                    }
                }
            }
        } catch (Exception e) {
            // fall through to empty list
        }
        return new ArrayList<>();
    }

    @Override
    public Component add(Behavior... behaviors) {
        if (palette.getRecorderComponent() == null) {
            toAdd.addAll(Arrays.asList(behaviors));
        } else {
            palette.getRecorderComponent().add(behaviors);
        }
        return this;
    }

    public Palette<Version> getPalette() {
        return palette;
    }

    public IModel<Collection<Version>> getPaletteModel() {
        return palette.getModel();
    }

    @Override
    public void convertInput() {
        Recorder<Version> recorder = palette.getRecorderComponent();
        if (recorder != null) {
            String ids = recorder.getInput();
            List<Version> selection = new ArrayList<>();

            if (ids != null && !ids.isEmpty()) {
                for (String id : ids.split(",")) {
                    String trimmedId = id.trim();
                    if (!trimmedId.isEmpty()) {
                        selection.add(new Version(trimmedId));
                    }
                }
            }

            setConvertedInput(selection);
        } else {
            setConvertedInput(new ArrayList<>());
        }
    }

    @Override
    public void updateModel() {
        List<Version> newSelection = getConvertedInput();
        List<Version> modelList = getModelObject();
        if (modelList == null) {
            getModel().setObject(newSelection);
        } else {
            modelList.clear();
            if (newSelection != null) {
                modelList.addAll(newSelection);
            }
        }
    }
}
