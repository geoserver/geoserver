import types
from geoscript import core
from org.geoscript.util import PyFeatureCollection
from org.geotools.feature import FeatureCollection

def process(inputs, outputs, title=None, description=None):
  def wrap(f):
    def wrapped(*args, **kwargs):
      # map arguments on way in
      args = (core.map(a) for a in args)
      for k in kwargs:
        kwargs[k] = core.map(kwargs[k])

      # unmap on way out
      # JD: this is a hack but we specify FeatureCollection as a hint to 
      # the mapping to deal with the FeatureCollection/FeatureSource to 
      # Layer mapping issue. In cases where we are not dealing with a layer 
      # like with raster data it should simply be ignored
      result = f(*args, **kwargs)
      if isinstance(result, types.GeneratorType):
        return PyFeatureCollection(result)
      return core.unmap(result, FeatureCollection)

    wrapped.title = title
    wrapped.description = description

    # unmap the specified inputs and outputs 
    wrapped.inputs = dict((k,_unmap(v, FeatureCollection)) 
      for k,v in inputs.iteritems())
    wrapped.outputs = dict((k,_unmap(v, FeatureCollection)) 
      for k,v in outputs.iteritems())
    return wrapped
  return wrap

def _unmap(v, t):
  return tuple([core.unmap(x, t) for x in v])
