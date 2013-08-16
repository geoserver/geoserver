/* Copyright (c) 2001 - 2013 OpenPlans - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.kml;

import org.geotools.xml.transform.TransformerBase;
import org.xml.sax.ContentHandler;

/**
 * Base class for kml transformers.
 * 
 * @author Justin Deoliveira, The Open Planning Project, jdeolive@openplans.org
 *
 */
public abstract class KMLTransformerBase extends TransformerBase {

    /**
     * standalone flag. 
     */
    boolean standAlone = true;
    
    /**
     * Sets flag controlling wether the transformer encodes the document as 
     * a standalone document, or as part of another kml document.
     * <p>
     * If <tt>standAlone</tt> is <code>true</code>, the transformer will wrap
     * its content in &lt;kml> tags.
     * </p>
     * 
     * @param standAlone <code>true</code> to set standalone, otherwise <code>false</code>
     */
    public void setStandAlone(boolean standAlone) {
        this.standAlone = standAlone;
    }
    
    /**
     * Determines if the document is being encoded standalone.
     * 
     * @see #setStandAlone(boolean)
     */
    public boolean isStandAlone() {
        return standAlone;
    }
    
    public abstract class KMLTranslatorSupport extends TranslatorSupport {

        public KMLTranslatorSupport(ContentHandler contentHandler) {
            super(contentHandler, null, null);
        }
        
    }

}
