package org.geoserver.w3ds.styles;



	import javax.measure.quantity.Length;
import javax.measure.unit.Unit;

import org.geotools.styling.AbstractSymbolizer;
import org.geotools.styling.Description;
import org.geotools.styling.Displacement;
import org.geotools.styling.DisplacementImpl;
import org.geotools.styling.Fill;
import org.geotools.styling.PolygonSymbolizer;
import org.geotools.styling.PolygonSymbolizerImpl;
import org.geotools.styling.Stroke;
import org.geotools.styling.StrokeImpl;
import org.opengis.filter.expression.Expression;
import org.opengis.style.StyleVisitor;
import org.opengis.util.Cloneable;


	/**
	 * Provides a representation of a PolygonSymbolizer in an SLD Document.  A
	 * PolygonSymbolizer defines how a polygon geometry should be rendered.
	 *
	 * @author James Macgill, CCG
	 * @author Johann Sorel (Geomatys)
	 *
	 *
	 * @source $URL: http://svn.osgeo.org/geotools/trunk/modules/library/main/src/main/java/org/geotools/styling/PolygonSymbolizerImpl.java $
	 * @version $Id: PolygonSymbolizerImpl.java 38089 2011-09-27 02:21:51Z mbedward $
	 */
	public class PolygonSymbolizerImpl3D extends AbstractSymbolizer implements PolygonSymbolizer, Cloneable {
	    
	    private Expression offset;
	    private DisplacementImpl disp;
	    
	    private Fill fill = new FillImpl3D();
	    private StrokeImpl stroke = null;

	    /**
	     * Creates a new instance of DefaultPolygonStyler
	     */
	    public PolygonSymbolizerImpl3D() {
	        this(null,null,null,null,null,null,null,null);
	    }

	    public PolygonSymbolizerImpl3D(Stroke stroke, 
	            Fill fill, 
	            Displacement disp, 
	            Expression offset, 
	            Unit<Length> uom, 
	            String geom, 
	            String name, 
	            Description desc) {
	        super(name, desc, geom, uom);
	        // Problems
	        //this.stroke = StrokeImpl.cast( stroke );
	        this.stroke = (StrokeImpl) stroke;
	        this.fill = fill;
	     // Problems
	        //this.disp = DisplacementImpl.cast( disp );
	        this.disp = (DisplacementImpl) disp;
	        this.offset = offset;
	    }
	    
	    public Expression getPerpendicularOffset() {
	        return offset;
	    }

	    public void setPerpendicularOffset(Expression offset ) {
	        this.offset = offset;
	    }
	    
	    public Displacement getDisplacement() {
	        return disp;
	    }

	    public void setDisplacement(org.opengis.style.Displacement displacement) {
	    	//Problems 
	        //this.disp = DisplacementImpl.cast( displacement );
	    	this.disp = (DisplacementImpl)displacement ;
	    }
	    /**
	     * Provides the graphical-symbolization parameter to use to fill the area
	     * of the geometry.
	     *
	     * @return The Fill style to use when rendering the area.
	     */
	    public Fill getFill() {
	        return fill;
	    }

	    /**
	     * Sets the graphical-symbolization parameter to use to fill the area of
	     * the geometry.
	     *
	     * @param fill The Fill style to use when rendering the area.
	     */
	    public void setFill(org.opengis.style.Fill fill) {
	        if (this.fill == fill) {
	            return;
	        }
	        this.fill = FillImpl3D.cast(fill);
	    }

	    /**
	     * Provides the graphical-symbolization parameter to use for the outline of
	     * the Polygon.
	     *
	     * @return The Stroke style to use when rendering lines.
	     */
	    public StrokeImpl getStroke() {
	        return stroke;
	    }

	    /**
	     * Sets the graphical-symbolization parameter to use for the outline of the
	     * Polygon.
	     *
	     * @param stroke The Stroke style to use when rendering lines.
	     */
	    public void setStroke(org.opengis.style.Stroke stroke) {
	        if (this.stroke == stroke) {
	            return;
	        }
	        // Problem
	        //this.stroke = StrokeImpl.cast( stroke );
	        this.stroke = (StrokeImpl)stroke;
	    }

	    /**
	     * Accepts a StyleVisitor to perform some operation on this LineSymbolizer.
	     *
	     * @param visitor The visitor to accept.
	     */
	    public Object accept(StyleVisitor visitor,Object data) {
	        return visitor.visit(this,data);
	    }

	    public void accept(org.geotools.styling.StyleVisitor visitor) {
	        visitor.visit(this);
	    }
	    
	    /**
	     * Creates a deep copy clone.   TODO: Need to complete the deep copy,
	     * currently only shallow copy.
	     *
	     * @return The deep copy clone.
	     *
	     * @throws RuntimeException DOCUMENT ME!
	     */
	    public Object clone() {
	        PolygonSymbolizerImpl3D clone;

	        try {
	            clone = (PolygonSymbolizerImpl3D) super.clone();

	            if (fill != null) {
	                clone.fill = (FillImpl3D) ((Cloneable) fill).clone();
	            }

	            if (stroke != null) {
	                clone.stroke = (StrokeImpl) ((Cloneable) stroke).clone();
	            }
	        } catch (CloneNotSupportedException e) {
	            throw new RuntimeException(e); // this should never happen.
	        }

	        return clone;
	    }

	    @Override
	    public int hashCode() {
	        final int prime = 31;
	        int result = super.hashCode();
	        result = prime * result + ((disp == null) ? 0 : disp.hashCode());
	        result = prime * result + ((fill == null) ? 0 : fill.hashCode());
	        result = prime * result + ((offset == null) ? 0 : offset.hashCode());
	        result = prime * result + ((stroke == null) ? 0 : stroke.hashCode());
	        return result;
	    }

	    @Override
	    public boolean equals(Object obj) {
	        if (this == obj)
	            return true;
	        if (!super.equals(obj))
	            return false;
	        if (getClass() != obj.getClass())
	            return false;
	        PolygonSymbolizerImpl3D other = (PolygonSymbolizerImpl3D) obj;
	        if (disp == null) {
	            if (other.disp != null)
	                return false;
	        } else if (!disp.equals(other.disp))
	            return false;
	        if (fill == null) {
	            if (other.fill != null)
	                return false;
	        } else if (!fill.equals(other.fill))
	            return false;
	        if (offset == null) {
	            if (other.offset != null)
	                return false;
	        } else if (!offset.equals(other.offset))
	            return false;
	        if (stroke == null) {
	            if (other.stroke != null)
	                return false;
	        } else if (!stroke.equals(other.stroke))
	            return false;
	        return true;
	    }

	    static PolygonSymbolizerImpl3D cast(org.opengis.style.Symbolizer symbolizer) {
	        if( symbolizer == null ){
	            return null;
	        }
	        else if (symbolizer instanceof PolygonSymbolizerImpl){
	            return (PolygonSymbolizerImpl3D) symbolizer;
	        }
	        else if( symbolizer instanceof org.opengis.style.PolygonSymbolizer ){
	            org.opengis.style.PolygonSymbolizer polygonSymbolizer = (org.opengis.style.PolygonSymbolizer) symbolizer;
	            PolygonSymbolizerImpl3D copy = new PolygonSymbolizerImpl3D();
	            copy.setStroke( polygonSymbolizer.getStroke());
	            copy.setDescription( polygonSymbolizer.getDescription() );
	            copy.setDisplacement( polygonSymbolizer.getDisplacement());
	            copy.setFill(polygonSymbolizer.getFill());
	            copy.setGeometryPropertyName( polygonSymbolizer.getGeometryPropertyName());
	            copy.setName(polygonSymbolizer.getName());
	            copy.setPerpendicularOffset(polygonSymbolizer.getPerpendicularOffset());
	            copy.setUnitOfMeasure( polygonSymbolizer.getUnitOfMeasure());
	            return copy;
	        }
	        else {
	            return null; // not possible
	        }
	    }
}
