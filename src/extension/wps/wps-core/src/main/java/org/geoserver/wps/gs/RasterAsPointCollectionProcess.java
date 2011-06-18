/* Copyright (c) 2001 - 2007 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wps.gs;

import java.awt.Rectangle;
import java.awt.image.RenderedImage;
import java.io.IOException;
import java.util.Iterator;
import java.util.NoSuchElementException;

import javax.media.jai.PlanarImage;
import javax.media.jai.iterator.RectIter;
import javax.media.jai.iterator.RectIterFactory;

import org.geoserver.wps.jts.DescribeParameter;
import org.geoserver.wps.jts.DescribeProcess;
import org.geoserver.wps.jts.DescribeResult;
import org.geoserver.wps.raster.CoverageUtilities;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.factory.GeoTools;
import org.geotools.feature.DefaultFeatureCollection;
import org.geotools.feature.collection.AbstractFeatureCollection;
import org.geotools.feature.collection.AdaptorFeatureCollection;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.geometry.jts.JTS;
import org.geotools.geometry.jts.JTSFactoryFinder;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.process.ProcessException;
import org.geotools.referencing.CRS;
import org.geotools.resources.geometry.XRectangle2D;
import org.geotools.util.Utilities;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.geometry.MismatchedDimensionException;
import org.opengis.metadata.spatial.PixelOrientation;
import org.opengis.referencing.operation.MathTransform2D;
import org.opengis.referencing.operation.TransformException;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Point;

/**
 * A process that wraps a {@link GridCoverage2D} as a collection of point feature.
 * 
 * @author Simone Giannecchini, GeoSolutions
 * 
 */
@DescribeProcess(title = "RasterAsPointCollection", description = "Convert a Raster into a collections of points")
public class RasterAsPointCollectionProcess implements GeoServerProcess {

    @DescribeResult(name = "result", description = "The point feature collection")
    public SimpleFeatureCollection execute(
            @DescribeParameter(name = "data", description = "The raster to be converted into a point feature collection") GridCoverage2D gc2d)
            throws ProcessException {
        if (gc2d ==null) {
            throw new ProcessException("Invalid input, source grid coverage should be not null");
        }
        
        //return value
        try {
			return new RasterAsPointFeatureCollection(gc2d);
		} catch (IOException e) {
			 throw new ProcessException("Unable to wrap provided grid coverage",e);
		}

    }

    /**
     * TODO @see {@link AdaptorFeatureCollection}
     * TODO @ee {@link DefaultFeatureCollection}
     * @author simboss
     *
     */
    private final static class RasterAsPointFeatureCollection extends AbstractFeatureCollection implements SimpleFeatureCollection {
    	
    	/**
    	 * The {@link GeometryFactory} cached here for building points inside iterators
    	 */
        static final GeometryFactory geometryFactory = JTSFactoryFinder.getGeometryFactory( GeoTools.getDefaultHints() );
        
    	/**
    	 * The {@link GridCoverage2D} that we want to expose as a point feature collection.
    	 */
		final GridCoverage2D gc2d;
		
		/**
		 * Number of points in this collections is as many as width*height.
		 */
		final  int size;

		/**
		 * Grid to world transformation at the upper left corner of the raster space.
		 */
		final MathTransform2D mt2D;

		/**
		 * The bounding box for this feature collection
		 */
		private ReferencedEnvelope bounds;

		/**
		 * Raster bounds for this coverage
		 */
		final  Rectangle rasterBounds;

		/**
		 * Number of bands
		 */
		final int numBands;
		
		public RasterAsPointFeatureCollection(final GridCoverage2D gc2d) throws IOException {
        	super(CoverageUtilities.createFeatureType(gc2d, Point.class));
            this.gc2d=gc2d;
            
            //
            // get various elements from this coverage
            //
            // SIZE
            final RenderedImage raster=gc2d.getRenderedImage();
            size=raster.getWidth()*raster.getHeight();
            
            // GRID TO WORLD
            mt2D= gc2d.getGridGeometry().getGridToCRS2D(PixelOrientation.UPPER_LEFT);
            
            // BOUNDS take into account that we want to map center coordinates
            rasterBounds = PlanarImage.wrapRenderedImage(raster).getBounds();
            final XRectangle2D rasterBounds_=new XRectangle2D(raster.getMinX()+0.5, raster.getMinY()+0.5, raster.getWidth()-1,  raster.getHeight()-1);
            try {
				bounds = new ReferencedEnvelope(CRS.transform(mt2D, rasterBounds_, null),gc2d.getCoordinateReferenceSystem2D());
			} catch (MismatchedDimensionException e) {
				final IOException ioe= new IOException();
				ioe.initCause(e);
				throw ioe;
			} catch (TransformException e) {
				final IOException ioe= new IOException();
				ioe.initCause(e);
				throw ioe;
			}
			
			// BANDS
			numBands = gc2d.getNumSampleDimensions();
            
        }
        
	

        @Override
        public SimpleFeatureIterator features() {
            return new RasterAsPointFeatureIterator(this);
        }

		@Override
		public int size() {
			return size;
		}

		@Override
		public ReferencedEnvelope getBounds() {
			return new ReferencedEnvelope(bounds);
		}

		@Override
		protected Iterator<SimpleFeature> openIterator() {
			return new WrappingIterator(features());
		}

		@Override
		protected void closeIterator(Iterator<SimpleFeature> close) {
			 if (close instanceof WrappingIterator) {
	                ((WrappingIterator) close).close();
	            }
			
		}
    }

    private final static class RasterAsPointFeatureIterator implements SimpleFeatureIterator {
    	
    	private final double[] temp;

    	private final SimpleFeatureBuilder fb;
    	
    	private final RasterAsPointFeatureCollection fc;
        
        private int index=0;
        
        private final int size;

		private final RectIter iterator;

		private final Coordinate coord= new Coordinate();

        public RasterAsPointFeatureIterator(final RasterAsPointFeatureCollection fc) {

        	//checks
        	Utilities.ensureNonNull("fc", fc);
        	
        	//get elements
        	this.fc= fc;
            this.fb = new SimpleFeatureBuilder(fc.getSchema());
            this.size=fc.size;
            
            // create an iterator that only goes forward, it is the fastest one
            iterator= RectIterFactory.create(fc.gc2d.getRenderedImage(), null);

            //
            //start the iterator
            //
            iterator.startLines();
            if(iterator.finishedLines())
            	throw new NoSuchElementException("Index beyond size:"+index+">"+size);
            iterator.startPixels();
            if(iterator.finishedPixels())
            	throw new NoSuchElementException("Index beyond size:"+index+">"+size);   
            
            // appo
            temp= new double[fc.numBands];
        }

        /**
         * Closes this iterator
         */
        public void close() {
        	// NO OP
        }

        /**
         * Tells us whether or not we have more elements to iterate on.
         */
        public boolean hasNext() {
            return index<size;
        }

        public SimpleFeature next() throws NoSuchElementException {
            if(!hasNext())
            	throw new NoSuchElementException("Index beyond size:"+index+">"+size);
            
            // iterate
            if(iterator.finishedPixels())
            	throw new NoSuchElementException("Index beyond size:"+index+">"+size);
            if(iterator.finishedLines())
            	throw new NoSuchElementException("Index beyond size:"+index+">"+size);                    
            
            // ID
            final int id=index;
            
            // POINT
            // can we reuse the coord?
            coord.x= 0.5+fc.rasterBounds.x+index%fc.rasterBounds.width;
            coord.y=0.5+ fc.rasterBounds.y+index/fc.rasterBounds.width ;
            final Point point = RasterAsPointFeatureCollection.geometryFactory.createPoint( coord );
            try {
				fb.add(JTS.transform(point, fc.mt2D));
			} catch (MismatchedDimensionException e) {
				final NoSuchElementException nse= new NoSuchElementException();
				nse.initCause(e);
				throw nse;
			} catch (TransformException e) {
				final NoSuchElementException nse= new NoSuchElementException();
				nse.initCause(e);
				throw nse;
			}
            
			// VALUES
            // loop on bands
			iterator.getPixel(temp);
			for(double d:temp){
				// I exploit the internal converters to go from double to whatever the type is
				// TODO is this correct or we can do more.
				fb.add(d);
			}
			
            
            // do we need to wrap the line??
            if(iterator.nextPixelDone()){
            	if(!iterator.nextLineDone())
            		iterator.startPixels();
            }
            
            // return
            final SimpleFeature returnValue= fb.buildFeature(String.valueOf(id));
            
            // increase index and iterator
            index++;
            
            return returnValue;
        }

    }
}
