from geoserver.filter import function

@function
def myFilter(feature):
  return feature['intProperty'] == 180;
