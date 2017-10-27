/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.script.wfs;

import com.google.common.base.Function;
import com.google.common.collect.Iterators;
import net.opengis.wfs.TransactionResponseType;
import net.opengis.wfs.TransactionType;
import org.geoserver.platform.resource.Resource;
import org.geoserver.script.ScriptManager;
import org.geoserver.wfs.TransactionEvent;
import org.geoserver.wfs.TransactionEventType;
import org.geoserver.wfs.TransactionPlugin;
import org.geoserver.wfs.WFSException;
import org.geotools.util.SoftValueHashMap;
import org.geotools.util.logging.Logging;
import org.springframework.stereotype.Component;

import javax.annotation.Nullable;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

@Component
public class ScriptTransactionPlugin implements TransactionPlugin {

    static Logger LOGGER = Logging.getLogger(ScriptTransactionPlugin.class);

    ScriptManager scriptMgr;

    SoftValueHashMap<Resource, ScriptTxDelegate> delegates = new SoftValueHashMap<Resource, ScriptTxDelegate>(); 

    public ScriptTransactionPlugin(ScriptManager scriptMgr) {
        this.scriptMgr = scriptMgr;
    }

    @Override
    public void dataStoreChange(final TransactionEvent event) throws WFSException {
        TransactionDetail details = details(event.getRequest());
        details.update(event);

        foreach(delegates(), new Function<ScriptTxDelegate, Void>() {
            @Override
            public Void apply(@Nullable ScriptTxDelegate input) {
                TransactionEventType type = event.getType();
                if (type == TransactionEventType.PRE_INSERT) {
                    input.preInsert(event);
                }
                else if (type == TransactionEventType.POST_INSERT) {
                    input.postInsert(event);
                }
                else if (type == TransactionEventType.PRE_UPDATE) {
                    input.preUpdate(event);
                }
                else if (type == TransactionEventType.POST_UPDATE) {
                    input.postUpdate(event);
                }
                else if (type == TransactionEventType.PRE_DELETE) {
                    input.preDelete(event);
                }
                else {
                    //TODO: POST_DELETE
                }

                return null;
            }
        });
    }

    void foreach(Iterator<ScriptTxDelegate> it, Function<ScriptTxDelegate, Void> f) {
        while(it.hasNext()) {
            f.apply(it.next());
        }
    }

    @Override
    public TransactionType beforeTransaction(TransactionType request) throws WFSException {
        for (Iterator<ScriptTxDelegate> it = delegates(); it.hasNext();) {
            ScriptTxDelegate delegate = it.next();
            request = delegate.beforeTransaction(request);
        }
        return request;
    }

    @Override
    public void beforeCommit(TransactionType request) throws WFSException {
        for (Iterator<ScriptTxDelegate> it = delegates(); it.hasNext();) {
            ScriptTxDelegate delegate = it.next();
            delegate.beforeCommit(request);
        }
    }

    @Override
    public void afterTransaction(TransactionType request, TransactionResponseType result, boolean committed) {
        for (Iterator<ScriptTxDelegate> it = delegates(); it.hasNext();) {
            ScriptTxDelegate delegate = it.next();
            delegate.afterTransaction(request, result, committed);
        }
    }

    @Override
    public int getPriority() {
        //return normal priority for now... we may want to up this or scan through all the scripts
        // and return whatever the highest priority is
        return 0;
    }

    TransactionDetail details(TransactionType request) {
        Map context = request.getExtendedProperties();
        TransactionDetail details = (TransactionDetail) context.get(TransactionDetail.class);
        if (details == null) {
            details = new TransactionDetail();
            context.put(TransactionDetail.class, details);
        }
        return details;
    }

    Iterator<ScriptTxDelegate> delegates() {
        List<Resource> files;
        try {
            files = scriptMgr.wfsTx().list();
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "Error listing files in wfs/tx directory", e);
            return Iterators.emptyIterator();
        }

        return Iterators.transform(files.iterator(), new Function<Resource, ScriptTxDelegate>() {
            @Override
            public ScriptTxDelegate apply(@Nullable Resource input) {
                return delegate(input);
            }
        }); 
    }

    ScriptTxDelegate delegate(Resource f) {
        ScriptTxDelegate delegate = delegates.get(f);
        if (delegate == null) {
            synchronized (this) {
                delegate = delegates.get(f);
                if (delegate == null) {
                    delegate = new ScriptTxDelegate(f, scriptMgr);
                    delegates.put(f, delegate);
                }
            }
        }
        return delegate;
    }
}
