/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.form.AjaxCheckBox;
import org.apache.wicket.extensions.ajax.markup.html.IndicatingAjaxLink;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.PropertyModel;
import org.geoserver.config.GeoServerLoader;

/**
 * Small utility panel showed only in dev mode that allows developers to control some Wicket
 * behavior
 */
@SuppressWarnings("serial")
public class DeveloperToolbar extends Panel {

    private AjaxCheckBox wicketIds;

    public DeveloperToolbar(String id) {
        super(id);

        // Clears the resource caches
        add(
                new IndicatingAjaxLink("clearCache") {
                    @Override
                    public void onClick(AjaxRequestTarget target) {
                        GeoServerApplication.get().clearWicketCaches();
                    }
                });

        // Reloads the whole catalog and config from the file system
        add(
                new IndicatingAjaxLink("reload") {
                    @Override
                    public void onClick(AjaxRequestTarget target) {
                        try {
                            GeoServerLoader loader =
                                    (GeoServerLoader)
                                            GeoServerApplication.get().getBean("geoServerLoader");
                            synchronized (org.geoserver.config.GeoServer.CONFIGURATION_LOCK) {
                                loader.reload();
                            }
                            info("Catalog and configuration reloaded");
                        } catch (Exception e) {
                            error(e);
                        }
                    }
                });

        IModel gsApp = new GeoServerApplicationModel();

        // controls whether wicket paths are being generated
        final AjaxCheckBox wicketPaths =
                new AjaxCheckBox(
                        "wicketPaths",
                        new PropertyModel(gsApp, "debugSettings.outputComponentPath")) {

                    @Override
                    protected void onUpdate(AjaxRequestTarget target) {}
                };
        wicketPaths.setOutputMarkupId(true);
        add(wicketPaths);

        // controls whether wicket ids are being generated
        wicketIds =
                new AjaxCheckBox(
                        "wicketIds", new PropertyModel(gsApp, "markupSettings.stripWicketTags")) {

                    @Override
                    protected void onUpdate(AjaxRequestTarget target) {
                        wicketPaths.setModelObject(Boolean.FALSE);
                        target.add(wicketPaths);
                    }
                };
        wicketIds.setOutputMarkupId(true);
        add(wicketIds);

        // controls whether the ajax debug is enabled or not
        add(
                new AjaxCheckBox(
                        "ajaxDebug",
                        new PropertyModel(gsApp, "debugSettings.ajaxDebugModeEnabled")) {

                    @Override
                    protected void onUpdate(AjaxRequestTarget target) {
                        // nothing to do, the property binding does the work for us
                    }
                });
    }

    static class GeoServerApplicationModel extends LoadableDetachableModel {

        GeoServerApplicationModel() {
            super(GeoServerApplication.get());
        }

        @Override
        protected Object load() {
            return GeoServerApplication.get();
        }
    }
}
