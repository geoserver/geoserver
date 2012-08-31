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
package org.geoserver.sldservice.rest.finder;

import org.geoserver.catalog.Catalog;
import org.geoserver.rest.RestletException;
import org.geoserver.sldservice.rest.resource.ClassifierResource;
import org.restlet.Finder;
import org.restlet.data.Request;
import org.restlet.data.Response;
import org.restlet.data.Status;
import org.restlet.resource.Resource;

public class ClassifierResourceFinder extends Finder {

    /**
     * reference to the catalog
     */
    protected Catalog catalog;
    
    protected ClassifierResourceFinder(Catalog catalog) {
        this.catalog = catalog;
    }
    
    @Override
    public Resource findTarget(Request request, Response response) {
        String layer = (String) request.getAttributes().get( "layer" );
        
        if ( layer != null) {
            return new ClassifierResource(getContext(),request,response,catalog);
        }
        
        throw new RestletException( "No such layer: " + layer, Status.CLIENT_ERROR_NOT_FOUND );
    }

}