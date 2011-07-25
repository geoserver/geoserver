/*
 *  Copyright (C) 2007-2008-2009 GeoSolutions S.A.S.
 *  http://www.geo-solutions.it
 *
 *  GPLv3 + Classpath exception
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
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
	 * @param numClass color ramp number of classes
	 */
	public void setNumClasses(int numClass);
	/**
	 * 
	 * @return int classes number
	 */
	public int getNumClasses();
	/**
	 * Return the color ramp 
	 * @return Color[]
	 */
	public List<Color> getRamp() throws Exception;	
	/**
	 * revert color ramp order
	 */
	public void revert();
}
