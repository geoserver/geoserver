/* (c) 2015 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.platform.util;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.geotools.util.logging.Logging;
import org.springframework.beans.factory.DisposableBean;

/**
 * Bean which allows other objects to be registered for disposal when the context closes.
 * <p>
 * This bean should only be used when it is not practical to instantiate the object to be disposed
 * of as a bean within the Spring context directly.  It does not guarantee destruction ordering
 * relative to other disposable beans in the context.
 * 
 * @author Kevin Smith, Boundless
 *
 */
public class Disposer implements DisposableBean {
    
    private static final Logger LOGGER = Logging.getLogger( Disposer.class );
    
    /**
     * Functor which disposes of an object.
     * @param <T>
     */
    //@FunctionalInterface // TODO For Java 8
    static public interface Destroyer<T> {
        /**
         * Dispose of the provided object
         * @param object
         * @throws Exception
         */
        public void accept(T object) throws Exception;
    }
    
    Map<Object, Destroyer<?>> toDestroy;
    
    static final private Destroyer<DisposableBean> DESTROYER_OF_BEANS = new Destroyer<DisposableBean>() {
        public void accept(DisposableBean object) throws Exception {
            object.destroy();
        }
    };
    static final private Destroyer<AutoCloseable> DESTROYER_OF_CLOSEABLES = new Destroyer<AutoCloseable>() {
        public void accept(AutoCloseable object) throws Exception {
            object.close();
        }
    };
    static final private Destroyer<ExecutorService> DESTROYER_OF_EXECUTORS = new Destroyer<ExecutorService>() {
        public void accept(ExecutorService object) throws Exception {
            object.shutdown();
        }
    };
    
    public Disposer()  {
        toDestroy = new HashMap<>();
    }
    
    /**
     * Register an object and a destroyer to dispose of it.
     * @param obj the object to destroy
     * @param destroyer
     */
    public <T> void register(T obj, Destroyer<? super T> destroyer) {
        checkNotNull(obj, "obj should not be null");
        checkNotNull(destroyer, "destroyer should not be null");
        checkArgument(obj!=this, "Should not attempt to register a Disposer to destroy itself"); // This way lies madness
        try{
            synchronized(this){
                checkNotDestroyed();
                toDestroy.put(obj, destroyer);
            }
        } catch(DestroyerDisposedException ex) {
            doDestroy(obj, destroyer);
            throw ex;
        }
    }
    
    /**
     * Dispose of registered object and deregister it.
     * @param o object to dispose of.  May be {@literal null}
     * @return {@literal true} if the object was successfully disposed of. {@literal false} if it 
     * was not registered, if disposal failed, or if it was {@literal null}.
     */
    public boolean disposeOf(Object o) {
        Destroyer<?> destroyer = null;
        synchronized(this){
            if(o!=null){
                destroyer = toDestroy.remove(o);
            }
        }
        if(destroyer!=null) {
            return doDestroy(o, destroyer);
        } else {
            return false;
        }
    }
    
    /**
     * Dispose of an object if it is registered and register another object in its place.
     * @param oldObj.  May be {@literal null}
     * @param newObj
     * @param newDestroyer
     * @return newObj
     */
    public <T> T replace(Object oldObj, T newObj, Destroyer<? super T> newDestroyer) {
        checkNotNull(newObj, "newObj should not be null");
        checkNotNull(newDestroyer, "newDestroyer should not be null");
        Destroyer<?> oldDestroyer = null;
        try{
            synchronized(this){
                checkNotDestroyed();
                if(oldObj!=null){
                    oldDestroyer = toDestroy.remove(oldObj);
                }
                register(newObj, newDestroyer);
            }
        } catch(DestroyerDisposedException ex) {
            doDestroy(newObj, newDestroyer);
            throw ex;
        } finally{
            if(oldDestroyer!=null) {
                doDestroy(oldObj, oldDestroyer);
            }
        }
        return newObj;
    }
    
    /**
     * Register a DisposableBean for disposal.
     * @param bean Bean to register
     */
    public void register(DisposableBean bean) {
        register(bean, DESTROYER_OF_BEANS);
    }
    
    /**
     * Dispose of an object if it is registered then register a DisposableBean in its place.
     * @param oldBean Bean to replace.  May be null.
     * @param newBean Bean to register
     * @return the new object
     */
    public <T extends DisposableBean> T replace(Object oldBean, T newBean) {
        return replace(oldBean, newBean, DESTROYER_OF_BEANS);
    }
    
    /**
     * Register an AutoCloseable for disposal.
     * @param obj AutoClosable to register
     */
    public void register(AutoCloseable obj) {
        register(obj, DESTROYER_OF_CLOSEABLES);
    }
    
    /**
     * Dispose of an object if it is registered then register an AutoCloseable in its place.
     * @param oldObj Object to replace.  May be null.
     * @param newObj Object to register
     * @return the new object
     */
    public <T extends AutoCloseable> T replace(Object oldObj, T newObj) {
        return replace(oldObj, newObj, DESTROYER_OF_CLOSEABLES);
    }
    
    /**
     * Register an ExecutorService for disposal.
     * @param obj ExecutorService to register
     */
    public void register(ExecutorService obj) {
        register(obj, DESTROYER_OF_EXECUTORS);
    }
    
    /**
     * Dispose of an object if it is registered then register an ExecutorService in its place.
     * @param oldObj Object to replace.  May be null.
     * @param newObj Object to register
     * @return the new object
     */
    public <T extends ExecutorService> T replace(Object oldObj, T newObj) {
        return replace(oldObj, newObj, DESTROYER_OF_EXECUTORS);
    }

    @SuppressWarnings("unchecked") // The types should have matched up when they were registered
    private boolean doDestroy(Object obj, Destroyer<?> dest) {
        try {
            ((Destroyer<Object>)dest).accept(obj);
            return true;
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error while disposing of registered disposable object", e);
            return false;
        }
    }
    
    /**
     * Dispose of all registered objects.  Once this method has been called, all other public 
     * methods will immediately dispose of any arguments given to them and then throw 
     * IllegalStateException.
     * <p>
     * This method is idempotent. Calling it again will have no additional effect.
     */
    @Override
    public void destroy() {
        if(toDestroy==null) return;
        synchronized(this) {
            if(toDestroy==null) return;
            for(Map.Entry<Object, Destroyer<?>> pair : toDestroy.entrySet()) {
                doDestroy(pair.getKey(), pair.getValue());
            }
            toDestroy = null;
        }
    }
    
    private static class DestroyerDisposedException extends IllegalArgumentException {
        /** serialVersionUID */
        private static final long serialVersionUID = 4547015737240523972L;

        DestroyerDisposedException(){
            super("DestroyerBean has been destroyed.");
        }
    }
    
    private void checkNotDestroyed() throws IllegalArgumentException {
        if(toDestroy==null) {
            throw new DestroyerDisposedException();
        }
    }
    private void checkArgument(boolean b, String msg) {
        if(!b) {
            throw new IllegalArgumentException(msg);
        }
    }
    private void checkNotNull(Object obj, String msg){
        if(obj==null) {
            throw new NullPointerException(msg);
        }
    }
}
