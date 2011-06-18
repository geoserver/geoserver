package org.geoserver.sldservice.utils.classifier;

import java.awt.Color;
import java.util.List;


/**
 * Colar Ramp is useful for bulding symbolizer in classified style
 * @author kappu
 *
 */
public interface ColorRamp{
	/**
	 * Set the new classes number and update the color ramp
	 * @param numClass numero di classi della color ramp
	 */
	public void setNumClasses(int numClass);
	/**
	 * 
	 * @return int classes number
	 */
	public int getNumClasses();
	/**
	 * Return the colo ramp 
	 * @return Color[]
	 */
	public List<Color> getRamp() throws Exception;	
	/**
	 * revert color ramp order
	 */
	public void revert();
}
