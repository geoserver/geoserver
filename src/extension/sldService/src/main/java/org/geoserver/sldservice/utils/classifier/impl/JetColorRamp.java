/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2007-2008-2009 GeoSolutions S.A.S., http://www.geo-solutions.it
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.sldservice.utils.classifier.impl;

import java.awt.Color;

/**
 * Jet Color Ramp Implementation. A color ramp starting from BLUE and ending with RED, having YELLOW
 * as intermediate color.
 *
 * @author Daniele Romagnoli, GeoSolutions SAS
 */
public class JetColorRamp extends CustomColorRamp {

    protected void createRamp() throws Exception {
        setEndColor(new Color(255, 0, 0));
        setMid(new Color(255, 255, 0)); // Yellow color as Mid Color
        setStartColor(new Color(0, 0, 255));
        super.createRamp();
    }
}
