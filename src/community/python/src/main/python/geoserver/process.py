def process(title, description, version='1.0.0', args=[], result=()):
  """
  Decorator for a process function.
  """
  def wrap(func):
     def wrapper(*args, **kwargs): return func(*args, **kwargs)
     wrapper.__process__ = None
     wrapper.title = title
     wrapper.description = description
     wrapper.version = version
     wrapper.args = args
     wrapper.result = result
     return wrapper
     
  return wrap
