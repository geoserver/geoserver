/*
 *    GeoTools - The Open Source Java GIS Toolkit
 *    http://geotools.org
 * 
 *    (C) 2005-2008, Open Source Geospatial Foundation (OSGeo)
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
package org.geoserver.csw.store.simple;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.geotools.feature.CollectionListener;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.FeatureIterator;
import org.geotools.feature.collection.DelegateFeatureIterator;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.util.NullProgressListener;
import org.opengis.feature.Feature;
import org.opengis.feature.type.FeatureType;

/**
 * Implement a feature collection just based on provision of iterator.
 * 
 * @author Jody Garnett (Refractions Research Inc)
 *
 *
 *
 * @source $URL$
 */
abstract class AbstractFeatureCollection<T extends FeatureType, F extends Feature> implements FeatureCollection<T, F> {
    /**
     * listeners
     */
    protected List<CollectionListener> listeners = new ArrayList<CollectionListener>();
    /** 
     * id used when serialized to gml
     */
    protected String id;
    protected T schema;

    protected AbstractFeatureCollection(T memberType) {
        this.id = id == null ? "featureCollection" : id;
        this.schema = memberType;
    }

    //
    // FeatureCollection - Feature Access
    // 
    @SuppressWarnings("unchecked")
	public FeatureIterator features() {
        FeatureIterator iter = new DelegateFeatureIterator( this, openIterator() );
        getOpenIterators().add( iter );
        return iter;
    }
    /**
     * Clean up after any resources associated with this iteartor in a manner similar to JDO collections.
     * </p>
     * Example (safe) use:<pre><code>
     * Iterator iterator = collection.iterator();
     * try {
     *     for( Iterator i=collection.iterator(); i.hasNext();){
     *          Feature feature = (Feature) i.hasNext();
     *          System.out.println( feature.getID() );
     *     }
     * }
     * finally {
     *     collection.close( iterator );
     * }
     * </code></pre>
     * </p>
     * @param close
     */
    @SuppressWarnings("unchecked")
	final public void close( Iterator close ){
        if( close == null ) return;
        try {
            closeIterator( close );
        }
        catch ( Throwable e ){
            // TODO Log e = ln
        }
        finally {
            open.remove( close );
        }       
    }
    
    public void close(FeatureIterator<F> close) {
        if( close != null ){
            close.close();
        }
    }
    
    /**
     * Open a resource based Iterator, we will call close( iterator ).
     * <p>
     * Please subclass to provide your own iterator for the the ResourceCollection,
     * note <code>iterator()</code> is implemented to call <code>open()</code>
     * and track the results in for later <code>purge()</code>.
     * 
     * @return Iterator based on resource use
     */
    abstract protected Iterator<F> openIterator();
    
    /**
     * Please override to cleanup after your own iterators, and
     * any used resources.
     * <p>
     * As an example if the iterator was working off a File then
     * the inputstream should be closed.
     * </p>
     * <p>
     * Subclass must call super.close( close ) to allow the list
     * of open iterators to be adjusted.
     * </p>
     * 
     * @param close Iterator, will not be <code>null</code>
     */
    abstract protected void closeIterator( Iterator<F> close );
    
    /**
     * Close any outstanding resources released by this resources.
     * <p>
     * This method should be used with great caution, it is however available
     * to allow the use of the ResourceCollection with algorthims that are
     * unaware of the need to close iterators after use.
     * </p>
     * <p>
     * Example of using a normal Collections utility method:<pre><code>
     * Collections.sort( collection );
     * collection.purge(); 
     * </code></pre>
     */
    @SuppressWarnings("unchecked")
	public void purge(){        
        for( Iterator i = open.iterator(); i.hasNext(); ){
            Object resource = i.next();
            if( resource instanceof Iterator ){
                Iterator resourceIterator = (Iterator) resource;
                try {
                    closeIterator( resourceIterator );
                }
                catch( Throwable e){
                    // TODO: Log e = ln
                }
                finally {
                    i.remove();
                }
            }
        }
    }
    
    /**
     * Returns the number of elements in this collection.
     * 
     * @return Number of items, or Interger.MAX_VALUE
     */
    public abstract int size();
    /**
     * Implement to support modification.
     * 
     * @param o element whose presence in this collection is to be ensured.
     * @return <tt>true</tt> if the collection changed as a result of the call.
     * 
     * @throws UnsupportedOperationException if the <tt>add</tt> method is not
     *        supported by this collection.
     * 
     * @throws NullPointerException if this collection does not permit
     *        <tt>null</tt> elements, and the specified element is
     *        <tt>null</tt>.
     * 
     * @throws ClassCastException if the class of the specified element
     *        prevents it from being added to this collection.
     * 
     * @throws IllegalArgumentException if some aspect of this element
     *         prevents it from being added to this collection.
     */
    public boolean add(F o) {
        throw new UnsupportedOperationException();
    }

    /**
     * Adds all of the elements in the specified collection to this collection
     * (optional operation).
     *
     * @param c collection whose elements are to be added to this collection.
     * @return <tt>true</tt> if this collection changed as a result of the
     *         call.
     * @throws UnsupportedOperationException if this collection does not
     *         support the <tt>addAll</tt> method.
     * @throws NullPointerException if the specified collection is null.
     * 
     * @see #add(Object)
     */
    @SuppressWarnings("unchecked")
	public boolean addAll(Collection<? extends F> c) {
        boolean modified = false;
        Iterator<? extends F> e = c.iterator();
        try {
            while (e.hasNext()) {
                if (add(e.next()))
                modified = true;
            }
        }
        finally {
            if( c instanceof FeatureCollection){
                FeatureCollection other = (FeatureCollection) c;
                other.close( e );
            }
        }
        return modified;
    } 

    public boolean addAll(FeatureCollection<? extends T,? extends F> c) {
        boolean modified = false;
        FeatureIterator<? extends F> e = c.features();
        try {
            while (e.hasNext()) {
                if (add(e.next()))
                modified = true;
            }
        }
        finally {
            e.close();
        }
        return modified;
    }
    
    /**
     * Removes all of the elements from this collection (optional operation).
     * 
     * @throws UnsupportedOperationException if the <tt>clear</tt> method is
     *        not supported by this collection.
     */
    public void clear() {
        Iterator<F> e = iterator();
        try {
            while (e.hasNext()) {
                e.next();
                e.remove();
            }
        }finally {
            close( e );            
        }
    }

    /**
     * Returns <tt>true</tt> if this collection contains the specified
     * element.
     * <tt></tt>.<p>
     *
     * This implementation iterates over the elements in the collection,
     * checking each element in turn for equality with the specified element.
     *
     * @param o object to be checked for containment in this collection.
     * @return <tt>true</tt> if this collection contains the specified element.
     */
    public boolean contains(Object o) {
        Iterator<F> e = null;
        try {
            e = iterator();
            if (o==null) {
                while (e.hasNext())
                if (e.next()==null)
                    return true;
            } else {
                while (e.hasNext())
                if (o.equals(e.next()))
                    return true;
            }
            return false;
        }
        finally {
            close( e );
        }
    }

    /**
     * Returns <tt>true</tt> if this collection contains all of the elements
     * in the specified collection. <p>
     * 
     * @param c collection to be checked for containment in this collection.
     * @return <tt>true</tt> if this collection contains all of the elements
     *         in the specified collection.
     * @throws NullPointerException if the specified collection is null.
     * 
     * @see #contains(Object)
     */
    public boolean containsAll(Collection<?> c) {
        Iterator<?> e = c.iterator();
        try {
            while (e.hasNext())
                if(!contains(e.next()))
                return false;
            return true;
        } finally {
            close( e );
        }
    }
    //
    // Contents
    //
    //
    /** Set of open resource iterators */
    @SuppressWarnings("unchecked")
	protected final Set open = new HashSet<Iterator<F>>();

    /**
     * Returns the set of open iterators.
     * <p>
     * Contents are a mix of Iterator<F> and FeatureIterator
     */
    @SuppressWarnings("unchecked")
	final public Set getOpenIterators() {
        return open;
    }
    
    /**
     * Please implement!
     * <p>
     * Note: If you return a ResourceIterator, the default implemntation of close( Iterator )
     * will know what to do.
     * 
     */
    @SuppressWarnings("unchecked")
	final public Iterator<F> iterator(){
    	Iterator<F> iterator = openIterator();
    	getOpenIterators().add( iterator );
        return iterator;
    }
    
    /**
     * @return <tt>true</tt> if this collection contains no elements.
     */
    public boolean isEmpty() {
        Iterator<F> iterator = iterator();
        try {
            return !iterator.hasNext();
        }
        finally {
            close( iterator );
        }
    }

    /**
     * Removes a single instance of the specified element from this
     * collection, if it is present (optional operation). 
     * 
     * @param o element to be removed from this collection, if present.
     * @return <tt>true</tt> if the collection contained the specified
     *         element.
     * @throws UnsupportedOperationException if the <tt>remove</tt> method is
     *        not supported by this collection.
     */
    public boolean remove(Object o) {
        Iterator<F> e = iterator();
        try {
            if (o==null) {
                while (e.hasNext()) {
                if (e.next()==null) {
                    e.remove();
                    return true;
                }
                }
            } else {
                while (e.hasNext()) {
                if (o.equals(e.next())) {
                    e.remove();
                    return true;
                }
            }
        }
        return false;
        }
        finally {
            close( e );
        }
    }

    /**
     * Removes from this collection all of its elements that are contained in
     * the specified collection (optional operation). <p>
     *
     * @param c elements to be removed from this collection.
     * @return <tt>true</tt> if this collection changed as a result of the
     *         call.
     * @throws UnsupportedOperationException if the <tt>removeAll</tt> method
     *         is not supported by this collection.
     * @throws NullPointerException if the specified collection is null.
     *
     * @see #remove(Object)
     * @see #contains(Object)
     */
    @SuppressWarnings("unchecked")
	final public boolean removeAll(Collection<?> c) {
        boolean modified = false;
        Iterator e = iterator();
        try {
            while (e.hasNext()) {
                if (c.contains(e.next())) {
                e.remove();
                modified = true;
                }
            }
            return modified;
        }
        finally {
            if( c instanceof FeatureCollection){
                FeatureCollection other = (FeatureCollection) c;
                other.close( e );
            }
        }
    }

    /**
     * Retains only the elements in this collection that are contained in the
     * specified collection (optional operation).
     *
     * @param c elements to be retained in this collection.
     * @return <tt>true</tt> if this collection changed as a result of the
     *         call.
     * @throws UnsupportedOperationException if the <tt>retainAll</tt> method
     *         is not supported by this Collection.
     * @throws NullPointerException if the specified collection is null.
     *
     * @see #remove(Object)
     * @see #contains(Object)
     */
    @SuppressWarnings("unchecked")
    final public boolean retainAll(Collection<?> c) {
        boolean modified = false;
        Iterator e = iterator();
        try {
            while (e.hasNext()) {
                if (!c.contains(e.next())) {
                e.remove();
                modified = true;
                }
            }
            return modified;
        }
        finally {
            if( c instanceof FeatureCollection){
                FeatureCollection other = (FeatureCollection) c;
                other.close( e );
            }
        }
    }

    
    /**
     * Array of all the elements.
     * 
     * @return an array containing all of the elements in this collection.
     */
    public Object[] toArray() {
        Object[] result = new Object[size()];
        Iterator<F> e = null;
        try {
            e = iterator();
            for (int i=0; e.hasNext(); i++)
                result[i] = e.next();
            return result;
        } finally {
            close( e );
        }
    }

    @SuppressWarnings("unchecked")
    public <T> T[] toArray(T[] a) {
        int size = size();
        if (a.length < size){
            a = (T[])java.lang.reflect.Array.newInstance(a.getClass().getComponentType(), size);
         }
        Iterator<F> it = iterator();
        try {
            
            Object[] result = a;
            for (int i=0; i<size; i++)
                result[i] = it.next();
            if (a.length > size)
            a[size] = null;
            return a;
        }
        finally {
            close( it );
        }
    }

	public void accepts(org.opengis.feature.FeatureVisitor visitor, org.opengis.util.ProgressListener progress) {
    	Iterator<F> iterator = null;
    	if( progress == null ) progress = new NullProgressListener();
        try{
            float size = size();
            float position = 0;            
            progress.started();
            for( iterator = iterator(); !progress.isCanceled() && iterator.hasNext();){
                if (size > 0) progress.progress( position++/size );
                try {
                    Feature feature = (Feature) iterator.next();
                    visitor.visit(feature);
                }
                catch( Exception erp ){
                    progress.exceptionOccurred( erp );
                }
            }            
        }
        finally {
            progress.complete();            
            close( iterator );
        }
    }
    
    public String getID() {
    	return id;
    }

    public final void addListener(CollectionListener listener) throws NullPointerException {
        listeners.add(listener);
    }

    public final void removeListener(CollectionListener listener) throws NullPointerException {
        listeners.remove(listener);
    }

    public T getSchema() {
    	return schema;
    }

    /**
     * Subclasses need to override this.
     */
    public abstract ReferencedEnvelope getBounds();
    
}
