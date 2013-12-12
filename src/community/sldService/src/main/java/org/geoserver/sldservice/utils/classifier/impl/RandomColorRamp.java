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
package org.geoserver.sldservice.utils.classifier.impl;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.geoserver.sldservice.utils.classifier.ColorRamp;

/**
 * Random Color Ramp Implementation
 * 
 * @author Alessio Fabiani, GeoSolutions SAS
 *
 */
public class RandomColorRamp implements ColorRamp {

	private int classNum = 0;
	private List<Color> colors = new ArrayList<Color>();

	public int getNumClasses() {
		return classNum;
	}

	public List<Color> getRamp() throws Exception {
		if (colors == null)
			throw new Exception("Class num not setted, color ramp null");
		return colors;
	}

	public void revert() {
        Collections.reverse(colors);
	}

	public void setNumClasses(int numClass) {
		classNum = numClass;
		try {
			createRamp();
		} catch (Exception e) {

		}
	}

	private void createRamp() throws Exception {
		for (int i = 0; i < classNum; i++)
			colors.add(new Color((int) (Math.random() * 255), (int) (Math
					.random() * 255), (int) (Math.random() * 255)));
	}

}
