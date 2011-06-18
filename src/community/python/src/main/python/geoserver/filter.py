from org.opengis.feature import Feature as _Feature
from geoscript.feature import Feature

class function(object):
  """
  Decorator for a filter function.
  """
  def __init__(self, wrapped):
     self.wrapped = wrapped
     self.__filter_function__ = None
     self.__name__ = wrapped.__name__

  def __call__(self, *args, **kwargs):
     obj = args[0]
     if isinstance(obj, _Feature):
        obj = Feature(f=obj)
        
     return self.wrapped(obj, *args[1:])
