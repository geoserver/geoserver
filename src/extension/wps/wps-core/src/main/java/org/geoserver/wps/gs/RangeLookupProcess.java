/* Copyright (c) 2001 - 2007 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wps.gs;

import jaitools.media.jai.contour.ContourDescriptor;
import jaitools.media.jai.contour.ContourRIF;
import jaitools.media.jai.rangelookup.RangeLookupDescriptor;
import jaitools.media.jai.rangelookup.RangeLookupRIF;
import jaitools.media.jai.rangelookup.RangeLookupTable;
import jaitools.numeric.Range;

import java.awt.image.DataBuffer;
import java.awt.image.RenderedImage;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Logger;

import javax.media.jai.JAI;
import javax.media.jai.RenderedOp;
import javax.media.jai.operator.BandSelectDescriptor;

import org.geoserver.wps.jts.DescribeParameter;
import org.geoserver.wps.jts.DescribeProcess;
import org.geoserver.wps.jts.DescribeResult;
import org.geoserver.wps.raster.CoverageUtilities;
import org.geotools.coverage.Category;
import org.geotools.coverage.CoverageFactoryFinder;
import org.geotools.coverage.GridSampleDimension;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.coverage.grid.GridCoverageFactory;
import org.geotools.image.jai.Registry;
import org.geotools.process.ProcessException;
import org.geotools.renderer.i18n.Errors;
import org.geotools.resources.i18n.ErrorKeys;
import org.geotools.resources.image.ColorUtilities;
import org.geotools.util.logging.Logging;
import org.opengis.coverage.grid.GridCoverage;
import org.opengis.util.ProgressListener;

/**
 * A raster reclassified process
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 * @author Emanuele Tajariol (GeoSolutions)
 * @author Simone Giannecchini (GeoSolutions)
 * @author Andrea Aime - GeoSolutions
 */
@DescribeProcess(title = "reclassify", description = "Reclassifies a continous coverage into a set of ranges identified by a number")
public class RangeLookupProcess implements GeoServerProcess {
	
	private final static Logger LOGGER = Logging.getLogger(RangeLookupProcess.class);
	
	static {
        Registry.registerRIF(JAI.getDefaultInstance(), new RangeLookupDescriptor(), new RangeLookupRIF(), Registry.JAI_TOOLS_PRODUCT);
    }

    @DescribeResult(name = "reclassified", description = "The resulting reclassified coverage")
    public GridCoverage2D execute(
            @DescribeParameter(name = "coverage", description = "The continuous coverage to be reclassified") GridCoverage2D coverage,
            @DescribeParameter(name = "band", description = "The band to be used for classification "
                    + "(defaults to 0)", min = 0) Integer classificationBand,
            @DescribeParameter(name = "ranges", description = "The list of ranges to be applied. \n"
                    + "Each range is expressed as 'OPEN START ; END CLOSE'\n"
                    + "where 'OPEN:=(|[, CLOSE=)|]',\n "
                    + "START is the low value, or nothing to imply -INF,\n"
                    + "CLOSE is the biggest value, or nothing to imply +INF", collectionType=Range.class) List<Range> classificationRanges,
            ProgressListener listener) throws ProcessException {
    	
    	//
    	// initial checks
    	//
    	if(coverage==null){
    		throw new ProcessException(Errors.format(ErrorKeys.NULL_ARGUMENT_$1,"coverage"));
    	}
    	if(classificationRanges==null){
    		throw new ProcessException(Errors.format(ErrorKeys.NULL_ARGUMENT_$1,"classificationRanges"));
    	}

        RenderedImage sourceImage= coverage.getRenderedImage();
    	
        // parse the band
        if (classificationBand != null) {
            final int band = classificationBand;
            final int numbands=sourceImage.getSampleModel().getNumBands();
            if(band<0 || numbands<=band){
            	throw new ProcessException(Errors.format(ErrorKeys.ILLEGAL_ARGUMENT_$2,"band",band));
            }
            
            if(band==0 && numbands>0 || band>0)
            	sourceImage=BandSelectDescriptor.create(sourceImage, new int []{band}, null);
        }


        //
        // Check the number of ranges we have in order to decide which type we can use for the output values. 
        // Our goal is to use the smallest possible data type that can hold the image values.
        //
        
        // Builds the range lookup table
        final RangeLookupTable lookupTable;
        final int size=classificationRanges.size();
        switch (ColorUtilities.getTransferType(size)) {
		case DataBuffer.TYPE_BYTE:
			lookupTable = CoverageUtilities.getRangeLookupTable(classificationRanges, (byte)0 );
			break;
		case DataBuffer.TYPE_USHORT:
			lookupTable = CoverageUtilities.getRangeLookupTable(classificationRanges, (short)0 );
			break;
		case DataBuffer.TYPE_INT:
			lookupTable = CoverageUtilities.getRangeLookupTable(classificationRanges, 0 );
			break;			
		default:
			throw new IllegalArgumentException(org.geotools.resources.i18n.Errors.format(ErrorKeys.ILLEGAL_ARGUMENT_$2,"classification ranges size",size));
		}
        // reclassify the source image
        final RenderedOp indexedClassification = RangeLookupDescriptor.create(sourceImage, lookupTable, null);
        
        
        //
        // build the output coverage
        //
        
        
        // build the output sample dimensions, use the default value ( 0 ) as the no data
        final GridSampleDimension outSampleDimension = new GridSampleDimension("classification",
                new Category[] { Category.NODATA }, null).geophysics(true);
        final GridCoverageFactory factory = CoverageFactoryFinder.getGridCoverageFactory(null);
        final GridCoverage2D output = factory.create("reclassified", indexedClassification, coverage
                .getGridGeometry(), new GridSampleDimension[] { outSampleDimension },
                new GridCoverage[] { coverage }, new HashMap<String,Double>(){{
                	put("GC_NODATA",0d);
                }});
        return output;
    }

}
