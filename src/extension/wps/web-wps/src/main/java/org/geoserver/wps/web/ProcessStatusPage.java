/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wps.web;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;
import org.apache.wicket.Component;
import org.apache.wicket.Session;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.panel.Fragment;
import org.apache.wicket.model.IModel;
import org.geoserver.web.GeoServerApplication;
import org.geoserver.web.GeoServerSecuredPage;
import org.geoserver.web.wicket.GeoServerDataProvider.Property;
import org.geoserver.web.wicket.GeoServerDialog;
import org.geoserver.web.wicket.GeoServerTablePanel;
import org.geoserver.web.wicket.ParamResourceModel;
import org.geoserver.wps.executor.ExecutionStatus;
import org.geoserver.wps.executor.WPSExecutionManager;

/**
 * Shows the status of currently running, and recently completed, processes
 *
 * @author Andrea Aime - GeoSolutions
 */
@SuppressWarnings("serial")
public class ProcessStatusPage extends GeoServerSecuredPage {

    private GeoServerTablePanel<ExecutionStatus> table;

    private AjaxLink<Void> dismissSelected;

    private GeoServerDialog dialog;

    public ProcessStatusPage() {
        ProcessStatusProvider provider = new ProcessStatusProvider();

        table =
                new GeoServerTablePanel<ExecutionStatus>("table", provider, true) {

                    @Override
                    protected Component getComponentForProperty(
                            String id,
                            IModel<ExecutionStatus> itemModel,
                            Property<ExecutionStatus> property) {
                        // have the base class create a label for us
                        Object value = property.getPropertyValue(itemModel.getObject());
                        if (value instanceof Date) {
                            SimpleDateFormat gmtFrmt =
                                    new SimpleDateFormat(
                                            "E, d MMM yyyy HH:mm:ss.SSS 'GMT'",
                                            Session.get().getLocale());
                            gmtFrmt.setTimeZone(TimeZone.getTimeZone("GMT"));
                            return new Label(id, gmtFrmt.format((Date) value));
                        }
                        return null;
                    }

                    @Override
                    protected void onSelectionUpdate(AjaxRequestTarget target) {
                        dismissSelected.setEnabled(table.getSelection().size() > 0);
                        target.add(dismissSelected);
                    }
                };
        table.setOutputMarkupId(true);
        table.setSelectable(true);
        add(table);

        // the confirm dialog
        add(dialog = new GeoServerDialog("dialog"));
        setHeaderPanel(headerPanel());
    }

    protected Component headerPanel() {
        Fragment header = new Fragment(HEADER_PANEL, "header", this);

        // the removal button
        header.add(dismissSelected = new ProcessDismissLink("dismissSelected"));
        dismissSelected.setOutputMarkupId(true);
        dismissSelected.setEnabled(false);

        return header;
    }

    protected final class ProcessDismissLink extends AjaxLink<Void> {

        protected ProcessDismissLink(String id) {
            super(id);
        }

        @Override
        public void onClick(AjaxRequestTarget target) {
            // see if the user selected anything
            final List<ExecutionStatus> selection = table.getSelection();
            if (selection.size() == 0) return;

            dialog.setTitle(new ParamResourceModel("confirmDismissal", this));

            // if there is something to cancel, let's warn the user about what
            // could go wrong, and if the user accepts, let's delete what's needed
            dialog.showOkCancel(
                    target,
                    new GeoServerDialog.DialogDelegate() {

                        protected Component getContents(String id) {
                            // show a confirmation panel for all the objects we have to remove
                            return new Label(
                                    id,
                                    new ParamResourceModel(
                                            "confirmDismissProcesses", ProcessStatusPage.this));
                        }

                        protected boolean onSubmit(AjaxRequestTarget target, Component contents) {
                            // issue deletion on the specified processes
                            WPSExecutionManager executor =
                                    GeoServerApplication.get()
                                            .getBeanOfType(WPSExecutionManager.class);
                            for (ExecutionStatus status : selection) {
                                try {
                                    executor.cancel(status.getExecutionId());
                                } catch (Exception e) {
                                    LOGGER.severe(
                                            "Failed to cancel process: " + status.getExecutionId());
                                    error(
                                            "Failed to cancel process: "
                                                    + status.getExecutionId()
                                                    + " with error: "
                                                    + e.getMessage());
                                }
                            }

                            return true;
                        }

                        @Override
                        public void onClose(AjaxRequestTarget target) {
                            // if the selection has been cleared out it's sign a deletion
                            // occurred, so refresh the table
                            if (table.getSelection().size() == 0) {
                                setEnabled(false);
                            }
                            target.add(ProcessDismissLink.this);
                            target.add(table);
                        }
                    });
        }
    }
}
