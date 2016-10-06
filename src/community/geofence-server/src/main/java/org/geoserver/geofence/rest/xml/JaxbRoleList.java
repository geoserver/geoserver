/* (c) 2015 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.geofence.rest.xml;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import org.geoserver.security.impl.GeoServerRole;

@XmlRootElement(name="roles")
public class JaxbRoleList {
	
	protected List<String> roleNames;

	public JaxbRoleList() {
		
	}

	public JaxbRoleList( Collection<GeoServerRole> roles ) {
		roleNames = new ArrayList<String>();
		for (GeoServerRole role: roles) {
			roleNames.add(role.getAuthority());
		}
	}

	@XmlElement(name="role")
	public List<String> getRoles() {
		return roleNames;
	}

}