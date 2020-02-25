/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wps.gs.download;

import javax.xml.bind.annotation.XmlRootElement;

/** The format definition in a map/animation download */
@XmlRootElement(name = "Format")
public class Format extends AbstractParametricEntity {}
