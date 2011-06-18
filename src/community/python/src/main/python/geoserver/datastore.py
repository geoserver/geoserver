def datastore(title, description, **params):
  """
  Dectorator for a datastore class.
  """
  def wrap(func):
     def wrapper(*args, **kwargs): return func(*args, **kwargs)
     wrapper.__datastore__ = None
     
     wrapper.title = title
     wrapper.description = description
     wrapper.params = params
     wrapper.wrapped = func
     return wrapper
     
  return wrap