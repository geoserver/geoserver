from geoscript.geom import Bounds
from geoscript.feature import Feature, Schema

def map_format(name, mime):
   """
   Decorator for a map format.
   """
   def wrap(func):
      def wrapper(context, output): 
        return func(Map(context), output)

      wrapper.__map_format__ = None
      wrapper.name = name
      wrapper.mime = mime

      return wrapper
   return wrap

class Map(object):

  def __init__(self, context):
    self.context = context
    
  def getheight(self):
    return self.context.getMapHeight();
  height = property(getheight)
  
  def getwidth(self):
    return self.context.getMapWidth()
  width = property(getwidth)
  
  def getbounds(self):
    return Bounds(env=self.context.getViewport().getBounds())
  bounds = property(getbounds)
  
  def getlayers(self):
    return [Layer(l) for l in self.context.layers()]
  layers = property(getlayers)
  
class Layer(object):

  def __init__(self, layer):
     self.layer = layer

  def getschema(self):
     return Schema(ft=self.layer.getFeatureSource().getSchema())
  schema = property(getschema)
  
  def features(self):
     fc = self.layer.getFeatureSource().getFeatures(self.layer.getQuery())
     it = fc.features()
     while it.hasNext():
        yield Feature(f=it.next())
        
     it.close()
