/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.wicket;

import org.apache.wicket.Component;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.FormComponentPanel;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.ResourceModel;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Point;

/**
 * A form component for a {@link Point} object.
 * 
 * @author Justin Deoliveira, OpenGeo
 * @author Andrea Aime, GeoSolutions
 */
public class PointPanel extends FormComponentPanel<Point> {
    private static final long serialVersionUID = -1046819530873258172L;

    GeometryFactory gf = new GeometryFactory();

    protected Label xLabel, yLabel;

    protected Double x, y;

    protected DecimalTextField xInput, yInput;
    
    public PointPanel(String id ) {
        super(id, new Model<Point>(null));
        
        initComponents();
    }
    
    public PointPanel(String id, Point p) {
        this(id, new Model(p));
    }
    
    public PointPanel(String id, IModel<Point> model) {
        super(id, model);
        
        initComponents();
    }
    
    public void setLabelsVisibility(boolean visible) {
        xLabel.setVisible(visible);
        yLabel.setVisible(visible);
    }

    void initComponents() {
        updateFields();
        
        add(xLabel = new Label("xL", new ResourceModel("x")));
        add(yLabel = new Label("yL", new ResourceModel("y")));

        add( xInput = new DecimalTextField( "x", new PropertyModel(this, "x")) );
        add( yInput = new DecimalTextField( "y", new PropertyModel(this, "y")) );
    }
    
    @Override
    protected void onBeforeRender() {
        updateFields();
        super.onBeforeRender();
    }
    
    private void updateFields() {
        Point p = (Point) getModelObject();
        if(p != null) {
            this.x = p.getX();
            this.y = p.getY();
        }
    }
   
    public PointPanel setReadOnly( final boolean readOnly ) {
        visitChildren( TextField.class, new org.apache.wicket.Component.IVisitor() {
            public Object component(Component component) {
                component.setEnabled( !readOnly );
                return null;
            }
        });

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
        
        // update the point model
        if(x != null && y != null) {
            setConvertedInput(gf.createPoint(new Coordinate(x, y)));
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
     * Sets the max number of digits for the 
     * @param maximumFractionDigits
     */
    public void setMaximumFractionDigits(int maximumFractionDigits) {
        xInput.setMaximumFractionDigits(maximumFractionDigits);
        yInput.setMaximumFractionDigits(maximumFractionDigits);
    }
    
    
}
