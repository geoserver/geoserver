/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.data.store.arcsde;

import static org.geotools.arcsde.session.ArcSDEConnectionConfig.CONNECTION_TIMEOUT_PARAM_NAME;
import static org.geotools.arcsde.session.ArcSDEConnectionConfig.INSTANCE_NAME_PARAM_NAME;
import static org.geotools.arcsde.session.ArcSDEConnectionConfig.MAX_CONNECTIONS_PARAM_NAME;
import static org.geotools.arcsde.session.ArcSDEConnectionConfig.MIN_CONNECTIONS_PARAM_NAME;
import static org.geotools.arcsde.session.ArcSDEConnectionConfig.PASSWORD_PARAM_NAME;
import static org.geotools.arcsde.session.ArcSDEConnectionConfig.PORT_NUMBER_PARAM_NAME;
import static org.geotools.arcsde.session.ArcSDEConnectionConfig.SERVER_NAME_PARAM_NAME;
import static org.geotools.arcsde.session.ArcSDEConnectionConfig.USER_NAME_PARAM_NAME;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.form.AjaxSubmitLink;
import org.apache.wicket.markup.html.form.ChoiceRenderer;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.FormComponent;
import org.apache.wicket.markup.html.form.validation.FormComponentFeedbackBorder;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.ResourceModel;
import org.geoserver.web.util.MapModel;
import org.geotools.arcsde.session.ArcSDEConnectionConfig;
import org.geotools.arcsde.session.ISession;
import org.geotools.arcsde.session.ISessionPool;
import org.geotools.arcsde.session.ISessionPoolFactory;
import org.geotools.arcsde.session.SessionPoolFactory;
import org.geotools.arcsde.session.UnavailableConnectionException;

/**
 * A panel for {@link ArcSDECoverageStoreEditPanel} that shows a drop down list where to choose the
 * raster table to create the coverage store for, and a refresh button to populate the list of
 * available raster tables based on the form's connection input fields.
 *
 * @author Gabriel Roldan
 */
public class RasterTableSelectionPanel extends Panel {

    private static final long serialVersionUID = 343924350476584166L;

    private transient ISessionPoolFactory sessionPoolFactory;

    /**
     * temporary parameter name used to hold the raster table selected by the drop down into the
     * store's connectionParameters
     */
    public static final String TABLE_NAME = "tableName";

    private static final String RESOURCE_KEY_PREFIX =
            RasterTableSelectionPanel.class.getSimpleName();

    private final DropDownChoice<String> choice;

    private FormComponent<?> serverComponent;

    private FormComponent<?> portComponent;

    private FormComponent<?> instanceComponent;

    private FormComponent<?> userComponent;

    private FormComponent<?> passwordComponent;

    public RasterTableSelectionPanel(
            final String id,
            final IModel<Map<String, Object>> paramsModel,
            final Form<?> storeEditForm,
            FormComponent<?> server,
            FormComponent<?> port,
            FormComponent<?> instance,
            FormComponent<?> user,
            FormComponent<?> password) {

        super(id);
        this.serverComponent = server;
        this.portComponent = port;
        this.instanceComponent = instance;
        this.userComponent = user;
        this.passwordComponent = password;

        final MapModel<String> tableNameModel = new MapModel<String>(paramsModel, TABLE_NAME);

        List<String> choices = new ArrayList<String>();
        if (tableNameModel.getObject() != null) {
            Object currentTableName = tableNameModel.getObject();
            choices.add(String.valueOf(currentTableName));
        }

        choice = new DropDownChoice<String>("rasterTable", tableNameModel, choices);

        /*
         * Make table name match the option id
         */
        choice.setChoiceRenderer(
                new ChoiceRenderer<String>() {
                    private static final long serialVersionUID = 1L;

                    public String getIdValue(String tableName, int index) {
                        return tableName.toString();
                    }

                    public Object getDisplayValue(String tableName) {
                        return tableName;
                    }
                });
        choice.setOutputMarkupId(true);
        choice.setNullValid(false);
        choice.setRequired(true);

        final FormComponentFeedbackBorder feedback = new FormComponentFeedbackBorder("border");
        feedback.add(choice);
        add(feedback);
        {
            final String titleKey = RESOURCE_KEY_PREFIX + ".tableNameChoice.title";
            ResourceModel titleModel = new ResourceModel(titleKey);
            String title = String.valueOf(titleModel.getObject());
            choice.add(AttributeModifier.replace("title", title));
        }

        final AjaxSubmitLink refreshTablesLink =
                new AjaxSubmitLink("refresh", storeEditForm) {
                    private static final long serialVersionUID = 1L;

                    /**
                     * We're not doing any validation here, just want to perform the same attempt to
                     * get to the list of connection parameters than at {@link #onSumbit}
                     */
                    @Override
                    protected void onError(AjaxRequestTarget target, Form<?> form) {
                        onSubmit(target, form);
                    }

                    @Override
                    protected void onSubmit(final AjaxRequestTarget target, final Form<?> form) {

                        final String server = serverComponent.getValue();
                        final String port = portComponent.getValue();
                        final String instance = instanceComponent.getValue();
                        final String user = userComponent.getValue();
                        final String password = passwordComponent.getValue();

                        final ISessionPoolFactory sessionFac = getSessionFactory();

                        List<String> rasterColumns;
                        try {
                            rasterColumns =
                                    getRasterColumns(
                                            server, port, instance, user, password, sessionFac);
                        } catch (IllegalArgumentException e) {
                            rasterColumns = Collections.emptyList();
                            String message = "Refreshing raster tables list: " + e.getMessage();
                            storeEditForm.error(message);
                            target.add(storeEditForm); // refresh
                        }

                        choice.setChoices(rasterColumns);
                        target.add(choice);
                        // do nothing else, so we return to the same page...
                    }
                };
        add(refreshTablesLink);
        {
            final String titleKey = RESOURCE_KEY_PREFIX + ".refresh.title";
            ResourceModel titleModel = new ResourceModel(titleKey);
            String title = String.valueOf(titleModel.getObject());
            refreshTablesLink.add(AttributeModifier.replace("title", title));
        }
    }

    public DropDownChoice<String> getFormComponent() {
        return choice;
    }

    private ISessionPoolFactory getSessionFactory() {
        if (this.sessionPoolFactory == null) {
            final ISessionPoolFactory sessionFac = SessionPoolFactory.getInstance();
            this.sessionPoolFactory = sessionFac;
        }
        return this.sessionPoolFactory;
    }

    void setSessionFactory(final ISessionPoolFactory factory) {
        this.sessionPoolFactory = factory;
    }

    /** */
    List<String> getRasterColumns(
            final String server,
            final String port,
            final String instance,
            final String user,
            final String password,
            final ISessionPoolFactory sessionFac)
            throws IllegalArgumentException {

        final ISessionPool pool;
        {
            final ArcSDEConnectionConfig connectionConfig;
            Map<String, Serializable> params = new HashMap<String, Serializable>();
            params.put(SERVER_NAME_PARAM_NAME, server);
            params.put(PORT_NUMBER_PARAM_NAME, port);
            params.put(INSTANCE_NAME_PARAM_NAME, instance);
            params.put(USER_NAME_PARAM_NAME, user);
            params.put(PASSWORD_PARAM_NAME, password);
            params.put(MIN_CONNECTIONS_PARAM_NAME, "1");
            params.put(MAX_CONNECTIONS_PARAM_NAME, "1");
            params.put(CONNECTION_TIMEOUT_PARAM_NAME, "1000");
            connectionConfig = ArcSDEConnectionConfig.fromMap(params);
            try {
                pool = sessionFac.createPool(connectionConfig);
            } catch (IOException e) {
                throw new IllegalArgumentException(e.getMessage());
            }
        }

        ISession session;
        try {
            session = pool.getSession();
        } catch (IOException e) {
            throw new IllegalAccessError(e.getMessage());
        } catch (UnavailableConnectionException e) {
            throw new IllegalAccessError(e.getMessage());
        }

        final List<String> rasterTables;
        try {
            rasterTables = session.getRasterColumns();
        } catch (IOException e) {
            throw new IllegalArgumentException(e.getMessage());
        } finally {
            session.dispose();
            pool.close();
        }

        return rasterTables;
    }
}
