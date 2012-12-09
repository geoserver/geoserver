from geoserver.wfs import tx 

@tx.before
def onBefore(req, context):
  context['before'] = True

@tx.preInsert
def onPreInsert(inserted, req, context):
  context['preInsert'] = True

@tx.postInsert
def onPostInsert(inserted, req, context):
  context['postInsert'] = True

@tx.preUpdate
def onPreUpdate(updated, props, req, context):
  context['preUpdate'] = True

@tx.postUpdate
def onPostUpdate(updated, props, req, context):
  context['postUpdate'] = True

@tx.preDelete
def onPreDelete(deleted, req, context):
  context['preDelete'] = True

@tx.postDelete
def onPostDelete(deleted, req, context):
  context['postDelete'] = True

@tx.preCommit
def onPreCommit(req, context):
  context['preCommit'] = True

@tx.postCommit  
def onPostCommit(req, res, context):
  context['postCommit'] = True

@tx.abort
def onAbort(req, res, context):
  context['abort'] = True
 
  
