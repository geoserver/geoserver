/* Copyright (c) 2001 - 2007 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, availible at the root
 * application directory.
 */
package org.geoserver.wms.kvp;

import java.awt.image.IndexColorModel;

import org.geoserver.ows.KvpParser;
import org.geoserver.platform.ServiceException;

/**
 * 
 * @author Simone Giannecchini, GeoSolutions SAS
 * 
 */
public class PaletteKvpParser extends KvpParser {
    public PaletteKvpParser() {
        super("palette", IndexColorModel.class);
    }

    public Object parse(String value) throws Exception {
        // palette
        try {
            final IndexColorModel model = PaletteManager.getPalette(value);
            if (model == null) {
                throw new ServiceException("Palette " + value + " could not be found "
                        + "in $GEOSERVER_DATA_DIR/palettes directory");
            }

            return model;
        } catch (Exception e) {
            throw new ServiceException(e, "Palette " + value + " could not be loaded");
        }
    }
}
