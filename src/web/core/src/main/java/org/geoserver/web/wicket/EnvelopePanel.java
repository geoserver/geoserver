/* Copyright (c) 2001 - 2007 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.wicket;

import org.apache.wicket.Component;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.form.FormComponentPanel;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import com.vividsolutions.jts.geom.Envelope;

/**
 * A form component for a {@link Envelope} object.
 * 
 * @author Justin Deoliveira, OpenGeo
 * @author Andrea Aime, OpenGeo
 */
public class EnvelopePanel extends FormComponentPanel {

    Double minX,minY,maxX,maxY;
    CoordinateReferenceSystem crs;
    WebMarkupContainer crsContainer;
    private CRSPanel crsPanel;
    boolean crsRequired;
    
    public EnvelopePanel(String id ) {
        super(id);
        
        initComponents();
    }
    
    public EnvelopePanel(String id, ReferencedEnvelope e) {
        this(id, new Model(e));
    }
    
    public EnvelopePanel(String id, IModel model) {
        super(id, model);
        
        initComponents();
    }
    
    public void setCRSFieldVisible(boolean visible) {
        crsContainer.setVisible(visible);
    }
    
    public boolean isCRSFieldVisible() {
        return crsContainer.isVisible();
    }
    
    public boolean isCrsRequired() {
        return crsRequired;
    }

    /**
     * Makes the CRS bounds a required component of the envelope. 
     * It is warmly suggested that the crs field be made visible too
     * @param crsRequired
     */
    public void setCrsRequired(boolean crsRequired) {
        this.crsRequired = crsRequired;
    }

    void initComponents() {
        updateFields();
        
        add( new DecimalTextField( "minX", new PropertyModel(this, "minX")) );
        add( new DecimalTextField( "minY", new PropertyModel(this, "minY")) );
        add( new DecimalTextField( "maxX", new PropertyModel(this, "maxX") ));
        add( new DecimalTextField( "maxY", new PropertyModel(this, "maxY")) );
        crsContainer = new WebMarkupContainer("crsContainer");
        crsContainer.setVisible(false);
        crsPanel = new CRSPanel("crs", new PropertyModel(this, "crs"));
        crsContainer.add(crsPanel);
        add(crsContainer);
    }
    
    @Override
    protected void onBeforeRender() {
        updateFields();
        super.onBeforeRender();
    }
    
    private void updateFields() {
        ReferencedEnvelope e = (ReferencedEnvelope) getModelObject();
        if(e != null) {
            this.minX = e.getMinX();
            this.minY = e.getMinY();
            this.maxX = e.getMaxX();
            this.maxY = e.getMaxY();
            this.crs = e.getCoordinateReferenceSystem();
        }
    }
   
    public EnvelopePanel setReadOnly( final boolean readOnly ) {
        visitChildren( TextField.class, new org.apache.wicket.Component.IVisitor() {
            public Object component(Component component) {
                component.setEnabled( !readOnly );
                return null;
            }
        });
        crsPanel.setReadOnly(readOnly);

        return this;
    }
    
    @Override
    protected void convertInput() {
        visitChildren( TextField.class, new org.apache.wicket.Component.IVisitor() {

            public Object component(Component component) {
                ((TextField) component).processInput();
                return null;
            }
        });
        if(isCRSFieldVisible()) {
            crsPanel.processInput();
        }
        
        // update the envelope model
        if(minX != null && maxX != null && minY != null && maxX != null) {
            if(crsRequired && crs == null) {
                setConvertedInput(null);
            } else {
                setConvertedInput(new ReferencedEnvelope(minX, maxX, minY, maxY, crs));
            }
        } else {
            setConvertedInput(null);
        }
    }
    
    @Override
    protected void onModelChanged() {
        // when the client programmatically changed the model, update the fields
        // so that the textfields will change too
        updateFields();
        visitChildren(TextField.class, new Component.IVisitor() {
            
            public Object component(Component component) {
                ((TextField) component).clearInput();
                return CONTINUE_TRAVERSAL;
            }
        });
    }
    
    /**
     * Returns the coordinate reference system added by the user in the GUI, if any and valid
     * @return
     */
    public CoordinateReferenceSystem getCoordinateReferenceSystem() {
        return crs;
    }
    
}
