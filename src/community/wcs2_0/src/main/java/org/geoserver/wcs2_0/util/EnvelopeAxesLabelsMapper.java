/*
 *    GeoTools - The Open Source Java GIS Toolkit
 *    http://geotools.org
 *
 *    (C) 2002-2011, Open Source Geospatial Foundation (OSGeo)
 *
 *    This library is free software; you can redistribute it and/or
 *    modify it under the terms of the GNU Lesser General Public
 *    License as published by the Free Software Foundation;
 *    version 2.1 of the License.
 *
 *    This library is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *    Lesser General Public License for more details.
 */
package org.geoserver.wcs2_0.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;

import org.geotools.referencing.CRS;
import org.geotools.referencing.CRS.AxisOrder;
import org.geotools.referencing.cs.DefaultCoordinateSystemAxis;
import org.geotools.util.Utilities;
import org.geotools.util.logging.Logging;
import org.opengis.geometry.Envelope;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.crs.GeographicCRS;
import org.opengis.referencing.cs.CoordinateSystem;
import org.opengis.referencing.cs.CoordinateSystemAxis;

/**
 * Retain a unique mapping between {@link Envelope} axes and their names.
 * 
 * @author Simone Giannecchini, GeoSolutions
 * TODO caching depending on CRS?
 * TODO handle composite CRS
 */
public class EnvelopeAxesLabelsMapper {
       
    /** Logger for this class*/
    private final static Logger LOGGER= Logging.getLogger(EnvelopeAxesLabelsMapper.class);
    
    public List<String> getAxesNames(Envelope envelope, boolean swapAxes){
        Utilities.ensureNonNull("envelope", envelope);
        final CoordinateReferenceSystem crs= envelope.getCoordinateReferenceSystem();
        
        // handle axes switch for geographic crs
        final boolean axesSwitch= crs instanceof GeographicCRS &&swapAxes;
        
        // additional fields for CRS 
        final CoordinateSystem cs=crs.getCoordinateSystem();
        
        final int dimension=cs.getDimension();
        // loop through dimensions
        final ArrayList<String> retValue=new ArrayList<String>();
        if (!axesSwitch) {
            for (int i = 0; i < dimension; i++) {
                retValue.add(cs.getAxis(i).getAbbreviation()); // use axis abbreviation
            }
        } else {
            int northing=-1, easting=-1;
            for (int i = 0; i < dimension; i++) {
                final CoordinateSystemAxis axis = cs.getAxis(i);
                if (Math.abs(DefaultCoordinateSystemAxis.getAngle(axis.getDirection(), DefaultCoordinateSystemAxis.LONGITUDE.getDirection()))<1E-6) {
                    easting = i;
                } else if (Math.abs(DefaultCoordinateSystemAxis.getAngle(axis.getDirection(), DefaultCoordinateSystemAxis.LATITUDE.getDirection()))<1E-6){
                    northing = i;
                }
                retValue.add(axis.getAbbreviation()); // use axis abbreviation
            }       
            
            // now switch them
            retValue.add(northing, retValue.remove(easting));
            
        }

        return retValue;
    }
    
    public int getAxisIndex(final Envelope envelope, final String axisAbbreviation){
        final int[] val= getAxesIndexes(envelope, Arrays.asList(axisAbbreviation));
        return (val==null?-1:val[0]);
    }
    
    public int[] getAxesIndexes(final Envelope envelope, final List<String> axesAbbreviations){
        Utilities.ensureNonNull("envelope", envelope);
        Utilities.ensureNonNull("dimensionNames", axesAbbreviations);

        final CoordinateReferenceSystem crs= envelope.getCoordinateReferenceSystem();
        final CoordinateSystem cs=crs.getCoordinateSystem();
        final int crsDimension=cs.getDimension();
                
        final int dimension=axesAbbreviations.size();
        final int[] retValue= new int[dimension];
        for (int i = 0; i < dimension; i++) {
            
            // abbreviation we are looking for
            final String axisAbbreviation=axesAbbreviations.get(i);
            int pos=-1;
            // search for this dimension in cs axes
            for(int j=0;j<crsDimension;j++){
                // check exact abbreviation
                if(cs.getAxis(j).getAbbreviation().equals(axisAbbreviation)){
                    pos=j; // FOUND!!!
                    break;
                }
            }
            
            // found?
            if(pos<0){
                // NOT FOUND
                return null;
            }
            
            // yes!!!
            retValue[i]=pos;
        }            
        return retValue;
    }

}
