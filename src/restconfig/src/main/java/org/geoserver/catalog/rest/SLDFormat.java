/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.catalog.rest;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import javax.xml.transform.TransformerException;

import org.geoserver.rest.format.StreamDataFormat;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.styling.NamedLayer;
import org.geotools.styling.SLDParser;
import org.geotools.styling.SLDTransformer;
import org.geotools.styling.Style;
import org.geotools.styling.StyleFactory;
import org.geotools.styling.StyledLayer;
import org.geotools.styling.StyledLayerDescriptor;

public class SLDFormat extends StreamDataFormat {

    boolean prettyPrint;
    
    public SLDFormat() {
        this(false);
    }
    
    public SLDFormat(boolean prettyPrint) {
        super(StyleResource.MEDIATYPE_SLD);
        this.prettyPrint = prettyPrint;
    }
    
    @Override
    protected void write(Object object, OutputStream out) throws IOException {
        Style style = (Style) object;
        
        //wrap in a StyledLayerDescriptor
        StyleFactory sf = CommonFactoryFinder.getStyleFactory(null);
        NamedLayer nl = sf.createNamedLayer();
        nl.setName( style.getName() );
        nl.addStyle( style );
        
        StyledLayerDescriptor sld = sf.createStyledLayerDescriptor();
        sld.setStyledLayers( new StyledLayer[]{ nl });
        
        SLDTransformer writer = new SLDTransformer();
        if (prettyPrint) {
            writer.setIndentation(2);
        }
        try {
            writer.transform( sld, out );
        } 
        catch (TransformerException e) {
            throw (IOException) new IOException().initCause(e);
        }
    }

    @Override
    protected Object read(InputStream in) throws IOException {
        SLDParser parser
            = new SLDParser( CommonFactoryFinder.getStyleFactory(null), in );
       
        Style[] styles = parser.readXML();
        if ( styles.length > 0 ) {
            return styles[0];
        }
        return null;
    }
}
