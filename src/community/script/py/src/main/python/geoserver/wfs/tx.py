from geoscript.layer.layer import Layer
from geoscript import core


def preInsert(f):
  def wrapped(inserted, req, context):
    return f(core.map(inserted), req, context)
  wrapped.__decorator__ = preInsert
  return wrapped

def postInsert(f):
  def wrapped(inserted, req, context):
    return f(core.map(inserted), req, context)
  wrapped.__decorator__ = postInsert
  return wrapped
  
def preUpdate(f):
  def wrapped(updated, props, req, context):
    return f(core.map(updated), dict(props) if props is not None else {}, req, context)
  wrapped.__decorator__ = preUpdate
  return wrapped

def postUpdate(f):
  def wrapped(updated, props, req, context):
    return f(core.map(updated),  dict(props) if props is not None else {}, req, context)
  wrapped.__decorator__ = postUpdate
  return wrapped
  
def preDelete(f):
  def wrapped(deleted, req, context):
    return f(core.map(deleted), req, context)
  wrapped.__decorator__ = preDelete
  return wrapped
  
def postDelete(f):
  def wrapped(deleted, req, context):
    return f(core.map(deleted), req, context)
  wrapped.__decorator__ = postDelete
  return wrapped

def before(f):
  def wrapped(*args, **kwargs):
    return f(*args, **kwargs)
  wrapped.__decorator__ = before
  return wrapped
  
def preCommit(f):
  def wrapped(*args, **kwargs):
    return f(*args, **kwargs)
  wrapped.__decorator__ = preCommit
  return wrapped
  
def postCommit(f):
  def wrapped(*args, **kwargs):
    return f(*args, **kwargs)
  wrapped.__decorator__ = postCommit
  return wrapped

def abort(f):
  def wrapped(*args, **kwargs):
    return f(*args, **kwargs)
  wrapped.__decorator__ = abort
  return wrapped

