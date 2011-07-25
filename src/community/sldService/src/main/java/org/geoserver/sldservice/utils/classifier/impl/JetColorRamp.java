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

/**
 * Jet Color Ramp Implementation. 
 * A color ramp starting from BLUE and ending with RED, having
 * YELLOW as intermediate color.
 * 
 * @author Daniele Romagnoli, GeoSolutions SAS
 *
 */
public class JetColorRamp extends CustomColorRamp {

	protected void createRamp() throws Exception {
		setEndColor(new Color(255,0,0));
		setMid(new Color(255,255,0)); //Yellow color as Mid Color
		setStartColor(new Color(0,0,255));
		super.createRamp();
	}
}
