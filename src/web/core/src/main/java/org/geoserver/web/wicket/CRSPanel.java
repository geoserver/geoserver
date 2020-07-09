/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.wicket;

import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.behavior.Behavior;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalWindow;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.basic.MultiLineLabel;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.FormComponentPanel;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.geoserver.web.data.resource.BasicResourceConfig;
import org.geotools.referencing.CRS;
import org.geotools.util.logging.Logging;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

/**
 * A form component for a {@link CoordinateReferenceSystem} object.
 *
 * <p>This panel provides the following functionality/information:
 *
 * <ul>
 *   <li>The SRS (epsg code) of the CRS
 *   <li>View the full WKT of the CRS.
 *   <li>A mechanism to guess the SRS (epsg code) from the CRS
 *   <li>A lookup for browsing for a particular CRS
 * </ul>
 *
 * The panel
 *
 * @author Justin Deoliveira, OpenGeo
 */
@SuppressWarnings("serial")
public class CRSPanel extends FormComponentPanel<CoordinateReferenceSystem> {
    private static Logger LOGGER = Logging.getLogger(CRSPanel.class);
    private static final long serialVersionUID = -6677103383336166008L;

    private static Behavior READ_ONLY =
            new AttributeModifier("readonly", new Model<String>("readonly"));

    /** pop-up window for WKT and SRS list */
    protected ModalWindow popupWindow;

    /** srs/epsg code text field */
    protected TextField<String> srsTextField;

    /** find link */
    protected AjaxLink<Void> findLink;

    /** wkt label */
    protected Label wktLabel;

    /** the wkt link that contains the wkt label * */
    protected GeoServerAjaxFormLink wktLink;

    protected SRSProvider srsProvider = new SRSProvider();

    /**
     * Constructs the CRS panel.
     *
     * <p>This constructor should be used if the panel is to inherit from a parent model (ie a
     * CompoundPropertyModel). If no such model is available the CRS will be left uninitialized. To
     * avoid inheriting from a parent model the constructor {@link #CRSPanel(String, IModel)} should
     * be used, specifying explicitly an uninitialized model.
     *
     * @param id The component id.
     */
    public CRSPanel(String id) {
        super(id);
        initComponents();
    }

    /**
     * Constructs the CRS panel with an explicit model.
     *
     * @param id The component id.
     * @param model The model, usually a {@link PropertyModel}.
     */
    public CRSPanel(String id, IModel<CoordinateReferenceSystem> model) {
        super(id, model);
        initComponents();
    }

    /**
     * Constructs the CRS panel specifying the underlying CRS explicitly.
     *
     * <p>When this constructor is used the {@link #getCRS()} method should be used after the form
     * is submitted to retrieve the final value of the CRS.
     *
     * @param id The component id.
     * @param crs The underlying CRS object.
     */
    public CRSPanel(String id, CoordinateReferenceSystem crs) {
        // JD: while the CoordinateReferenceSystem interface does not implement Serializable
        // all the CRS objects we use do, hence the cast
        super(id, new CRSModel(crs));
        initComponents();
        setConvertedInput(crs);
    }

    /**
     * Constructs the CRS panel with an explicit model.
     *
     * @param id The component id.
     * @param model The model, usually a {@link PropertyModel}.
     * @param otherSRS list of srs to show in popup
     */
    public CRSPanel(String id, IModel<CoordinateReferenceSystem> model, List<String> otherSRS) {
        super(id, model);
        this.srsProvider = new SRSProvider(otherSRS);
        initComponents();
    }

    /*
     * helper for internally creating the panel.
     */
    void initComponents() {

        popupWindow = new ModalWindow("popup");
        add(popupWindow);

        srsTextField = new TextField<String>("srs", new Model<String>());
        add(srsTextField);
        srsTextField.setOutputMarkupId(true);

        srsTextField.add(
                new AjaxFormComponentUpdatingBehavior("blur") {

                    @Override
                    protected void onUpdate(AjaxRequestTarget target) {
                        convertInput();

                        CoordinateReferenceSystem crs =
                                (CoordinateReferenceSystem) getConvertedInput();
                        if (crs != null) {
                            setModelObject(crs);
                            wktLabel.setDefaultModelObject(crs.getName().toString());
                            wktLink.setEnabled(true);
                        } else {
                            wktLabel.setDefaultModelObject(null);
                            wktLink.setEnabled(false);
                        }
                        target.add(wktLink);

                        onSRSUpdated(toSRS(crs), target);
                    }
                });

        findLink =
                new AjaxLink<Void>("find") {
                    @Override
                    public void onClick(AjaxRequestTarget target) {
                        popupWindow.setContent(srsListPanel());
                        popupWindow.setTitle(new ParamResourceModel("selectSRS", CRSPanel.this));
                        popupWindow.show(target);
                    }
                };
        add(findLink);

        wktLink =
                new GeoServerAjaxFormLink("wkt") {
                    @Override
                    public void onClick(AjaxRequestTarget target, Form<?> form) {
                        popupWindow.setInitialHeight(375);
                        popupWindow.setInitialWidth(525);
                        popupWindow.setContent(new WKTPanel(popupWindow.getContentId(), getCRS()));
                        CoordinateReferenceSystem crs =
                                (CoordinateReferenceSystem) CRSPanel.this.getModelObject();
                        if (crs != null) popupWindow.setTitle(crs.getName().toString());
                        popupWindow.show(target);
                    }
                };
        wktLink.setEnabled(getModelObject() != null);
        add(wktLink);

        wktLabel = new Label("wktLabel", new Model<String>());
        wktLink.add(wktLabel);
        wktLabel.setOutputMarkupId(true);
    }

    @Override
    protected void onBeforeRender() {
        CoordinateReferenceSystem crs = (CoordinateReferenceSystem) getModelObject();
        if (crs != null) {
            srsTextField.setModelObject(toSRS(crs));
            wktLabel.setDefaultModelObject(crs.getName().toString());
        } else {
            wktLabel.setDefaultModelObject(null);
            wktLink.setEnabled(false);
        }

        super.onBeforeRender();
    }

    @Override
    public void convertInput() {
        String srs = srsTextField.getInput();
        CoordinateReferenceSystem crs = null;
        if (srs != null && !"".equals(srs)) {
            if ("UNKNOWN".equals(srs)) {
                // leave underlying crs unchanged
                if (getModelObject() instanceof CoordinateReferenceSystem) {
                    setConvertedInput(getModelObject());
                }
                return;
            }
            crs = fromSRS(srs);
        }
        setConvertedInput(crs);
    }

    /**
     * Subclasses can override to perform custom behaviors when the SRS is updated, which happens
     * either when the text field is left or when the find dialog returns
     */
    protected void onSRSUpdated(String srs, AjaxRequestTarget target) {
        // do nothing by default
    }

    /** Sets the panel to be read only. */
    public CRSPanel setReadOnly(boolean readOnly) {
        if (readOnly) srsTextField.add(READ_ONLY);
        else srsTextField.remove(READ_ONLY);
        findLink.setVisible(!readOnly);
        return this;
    }

    /** Show Find EPGS button but keep text field read only */
    public CRSPanel setFindLinkVisible(boolean show) {
        srsTextField.add(READ_ONLY);
        findLink.setVisible(show);

        return this;
    }

    /**
     * Returns the underlying CRS for the panel.
     *
     * <p>This method is convenience for:
     *
     * <pre>
     * (CoordinateReferenceSystem) this.getModelObject();
     * </pre>
     */
    public CoordinateReferenceSystem getCRS() {
        // convertInput();
        return getModelObject();
    }

    /*
     * Goes from CRS to SRS.
     */
    String toSRS(CoordinateReferenceSystem crs) {
        try {
            if (crs != null) {
                Integer epsgCode = CRS.lookupEpsgCode(crs, false);
                String srs = srsTextField.getModelObject();
                // do not append
                if (srs != null
                        && srs.contains(
                                epsgCode.toString()) // assert that text field is in sync with
                        // passed crs
                        && (srs.startsWith(BasicResourceConfig.URN_OGC_PREFIX)
                                || srs.startsWith(BasicResourceConfig.EPSG_PREFIX))) {
                    return srs;
                }
                // prefix if text field only had the EPSG code.
                return epsgCode != null ? BasicResourceConfig.EPSG_PREFIX + epsgCode : "UNKNOWN";
            } else {
                return "UNKNOWN";
            }
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Could not succesffully lookup an EPSG code", e);
            return null;
        }
    }

    /*
     * Goes from SRS to CRS.
     */
    protected CoordinateReferenceSystem fromSRS(String srs) {
        try {
            return CRS.decode(srs);
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Unknown EPSG code " + srs, e);
            return null;
        }
    }

    /*
     * Builds the srs list panel component.
     */
    protected SRSListPanel srsListPanel() {
        SRSListPanel srsList =
                new SRSListPanel(popupWindow.getContentId(), srsProvider) {

                    @Override
                    protected void onCodeClicked(AjaxRequestTarget target, String epsgCode) {
                        popupWindow.close(target);

                        String srs = epsgCode;

                        // do not append EPSG for OGC URN
                        if (!epsgCode.startsWith(BasicResourceConfig.URN_OGC_PREFIX)) {
                            srs = "EPSG:" + srs;
                        }

                        srsTextField.setModelObject(srs);
                        target.add(srsTextField);

                        CoordinateReferenceSystem crs = fromSRS(srs);
                        CRSPanel.this.setModelObject(crs);
                        if (crs != null) {
                            wktLabel.setDefaultModelObject(crs.getName().toString());
                            wktLink.setEnabled(true);
                        } else {
                            wktLabel.setDefaultModelObject(null);
                            wktLink.setEnabled(false);
                        }
                        target.add(wktLink);

                        onSRSUpdated(srs, target);
                    }
                };
        srsList.setCompactMode(true);
        return srsList;
    }

    /*
     * Panel for displaying the well known text for a CRS.
     */
    public static class WKTPanel extends Panel {

        public WKTPanel(String id, CoordinateReferenceSystem crs) {
            super(id);

            MultiLineLabel wktLabel = new MultiLineLabel("wkt");

            add(wktLabel);

            if (crs != null) {
                wktLabel.setDefaultModel(new Model<String>(crs.toString()));
            }
        }
    }
}
