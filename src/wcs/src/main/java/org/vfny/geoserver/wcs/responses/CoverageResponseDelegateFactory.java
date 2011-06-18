/* Copyright (c) 2001 - 2007 TOPP - www.openplans.org.  All rights reserved.
 * This code is licensed under the GPL 2.0 license, availible at the root
 * application directory.
 */
package org.vfny.geoserver.wcs.responses;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

import org.vfny.geoserver.wcs.responses.coverage.AscCoverageResponseDelegate;
import org.vfny.geoserver.wcs.responses.coverage.DebugCoverageResponseDelegate;
import org.vfny.geoserver.wcs.responses.coverage.GTopo30CoverageResponseDelegate;
import org.vfny.geoserver.wcs.responses.coverage.GeoTIFFCoverageResponseDelegate;
import org.vfny.geoserver.wcs.responses.coverage.IMGCoverageResponseDelegate;

/**
 * DOCUMENT ME!
 * 
 * @author Alessio Fabiani (alessio.fabiani@gmail.com) $ (last modification)
 * @author $Author: Simone Giannecchini (simboss1@gmail.com) $ (last modification)
 */
public class CoverageResponseDelegateFactory {
    /** DOCUMENT ME! */
    private static final List<CoverageResponseDelegate> encoders = new ArrayList<CoverageResponseDelegate>();

    static {
        encoders.add(new AscCoverageResponseDelegate());
        encoders.add(new IMGCoverageResponseDelegate());
        encoders.add(new GTopo30CoverageResponseDelegate());
        encoders.add(new GeoTIFFCoverageResponseDelegate());
        encoders.add(new DebugCoverageResponseDelegate());
    }

    private CoverageResponseDelegateFactory() {
    }

    /**
     * Creates an encoder for a specific getfeature results output format
     * 
     * @param outputFormat
     *            DOCUMENT ME!
     * 
     * @return DOCUMENT ME!
     */
    public static CoverageResponseDelegate encoderFor(String outputFormat) {
        CoverageResponseDelegate encoder = null;

        for (Iterator<CoverageResponseDelegate> it = encoders.iterator(); it.hasNext();) {
            encoder = it.next();

            if (encoder.canProduce(outputFormat)) {
                try {
                    if (encoder != null) {
                        return encoder.getClass().newInstance();
                    }
                } catch (IllegalAccessException ex) {
                    final NoSuchElementException e = new NoSuchElementException(new StringBuffer(
                            "Can't create the encoder ").append(encoder.getClass().getName())
                            .toString());
                    e.initCause(ex);
                    throw e;
                } catch (InstantiationException ex) {
                    final NoSuchElementException e = new NoSuchElementException(new StringBuffer(
                            "Can't create the encoder ").append(encoder.getClass().getName())
                            .toString());
                    e.initCause(ex);
                    throw e;
                }
            }
        }

        return null;
    }

}
