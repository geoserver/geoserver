def before(req, context):
  context['before'] = True

def preInsert(inserted, req, context):
  context['preInsert'] = True

def postInsert(inserted, req, context):
  context['postInsert'] = True

def preUpdate(updated, req, context):
  context['preUpdate'] = True

def postUpdate(updated, req, context):
  context['postUpdate'] = True

def preDelete(deleted, req, context):
  context['preDelete'] = True

def postDelete(deleted, req, context):
  context['postDelete'] = True

def preCommit(req, context):
  context['preCommit'] = True
  
def postCommit(req, res, context):
  context['postCommit'] = True

def abort(req, res, context):
  context['abort'] = True
 
  