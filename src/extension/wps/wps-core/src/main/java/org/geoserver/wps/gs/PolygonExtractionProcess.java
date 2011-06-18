/* Copyright (c) 2001 - 2007 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wps.gs;

import jaitools.media.jai.contour.ContourDescriptor;
import jaitools.media.jai.contour.ContourRIF;
import jaitools.media.jai.vectorize.VectorizeDescriptor;
import jaitools.media.jai.vectorize.VectorizeRIF;
import jaitools.numeric.Range;

import java.awt.geom.AffineTransform;
import java.awt.image.RenderedImage;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import javax.media.jai.JAI;
import javax.media.jai.ParameterBlockJAI;
import javax.media.jai.RenderedOp;

import org.geoserver.wps.jts.DescribeParameter;
import org.geoserver.wps.jts.DescribeProcess;
import org.geoserver.wps.jts.DescribeResult;
import org.geoserver.wps.raster.CoverageUtilities;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.data.collection.ListFeatureCollection;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.image.jai.Registry;
import org.geotools.process.ProcessException;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.metadata.spatial.PixelOrientation;
import org.opengis.util.ProgressListener;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.Polygon;
import com.vividsolutions.jts.geom.util.AffineTransformation;

/**
 * A process that wraps a {@link GridCoverage2D} as a collection of point feature.
 * 
 * @author Simone Giannecchini, GeoSolutions
 * 
 */
@DescribeProcess(title = "PolygonExtraction", description = "Perform the polygon extraction on a provided raster")
public class PolygonExtractionProcess implements GeoServerProcess {
	
    
    static {
        Registry.registerRIF(JAI.getDefaultInstance(), new VectorizeDescriptor(), new VectorizeRIF(), Registry.JAI_TOOLS_PRODUCT);
    }

	
    @DescribeResult(name = "result", description = "The polygon feature collection")
    public SimpleFeatureCollection execute(
            @DescribeParameter(name = "data", description = "The raster to be used as the source") GridCoverage2D coverage,
            @DescribeParameter(name = "band", description = "(Integer, default=0) the source image band to process",min=0) Integer band,
            @DescribeParameter(name = "insideEdges", description = " (Boolean, default=true) whether to vectorize boundaries between adjacent regions with non-outside values",min=0) Boolean insideEdges,
            @DescribeParameter(name = "roi", description = "The geometry used to delineate the area of interest in model space",min=0) Geometry roi,
            @DescribeParameter(name = "ranges", description = "The list of ranges to be applied. \n"
                + "Each range is expressed as 'OPEN START ; END CLOSE'\n"
                + "where 'OPEN:=(|[, CLOSE=)|]',\n "
                + "START is the low value, or nothing to imply -INF,\n"
                + "CLOSE is the biggest value, or nothing to imply +INF", collectionType=Range.class, min=0) List<Range> classificationRanges,
            ProgressListener progressListener)
            throws ProcessException {
    	
    	
    	//
    	// initial checks
    	//
        if (coverage ==null) {
            throw new ProcessException("Invalid input, source grid coverage should be not null");
        }
        if(band == null) {
        	band = 0;
        } else if (band < 0 || band >= coverage.getNumSampleDimensions()) {
            throw new ProcessException("Invalid input, invalid band number:"+band);
        }
        // do we have classification ranges?
        boolean hasClassificationRanges= classificationRanges!=null&& classificationRanges.size()>0;
        
        // apply the classification by setting 0 as the default value and using 1, ..., numClasses for the other classes.
        // we use 0 also as the noData for the resulting coverage.
        if(hasClassificationRanges){

        	final RangeLookupProcess lookup = new RangeLookupProcess();
        	coverage=lookup.execute(
        			coverage, 
        			band, 
        			classificationRanges, 
        			progressListener);
        }
        
        //
        // GRID TO WORLD preparation
        //
        final AffineTransform mt2D = (AffineTransform) coverage.getGridGeometry().getGridToCRS2D(PixelOrientation.UPPER_LEFT);
        
        // get the rendered image
        final RenderedImage raster=coverage.getRenderedImage();
        
        // perform jai operation
        ParameterBlockJAI pb = new ParameterBlockJAI("Vectorize");
        pb.setSource("source0", raster);
        
        if(roi!=null){
        	pb.setParameter("roi", CoverageUtilities.prepareROI(roi,mt2D));
        }
        pb.setParameter("band", band);
        // TODO use no Data from the final coverage
        pb.setParameter("outsideValues", Arrays.asList(0));  
        // pb.setParameter("removeCollinear", false);  
          final RenderedOp dest = JAI.create("Vectorize", pb);
		@SuppressWarnings("unchecked")
		final Collection<Polygon> prop = (Collection<Polygon>) dest.getProperty(VectorizeDescriptor.VECTOR_PROPERTY_NAME
		);
        
        // wrap as a feature collection and return
		final SimpleFeatureType featureType=CoverageUtilities.createFeatureType(coverage,Polygon.class);
        final SimpleFeatureBuilder builder = new SimpleFeatureBuilder(featureType);
        int i=0;
        final ListFeatureCollection featureCollection= new ListFeatureCollection(featureType);
        final AffineTransformation jtsTransformation=new AffineTransformation(
    			mt2D.getScaleX(),
    			mt2D.getShearX(),
    			mt2D.getTranslateX(),
    			mt2D.getShearY(),
    			mt2D.getScaleY(),
    			mt2D.getTranslateY());
        for(Polygon polygon:prop){
        	// get value
        	Double  value= (Double) polygon.getUserData();
        	polygon.setUserData(null);
        	// filter coordinates in place
        	polygon.apply(jtsTransformation);
        	
        	// create feature and add to list
        	builder.set("the_geom", polygon);
        	builder.set("value", value);
        	
        	featureCollection.add(builder.buildFeature(String.valueOf(i++)));
        	
        }
        
        //return value
        return featureCollection;

    }
}
