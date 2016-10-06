from geoserver.wps import process
from geoscript.geom import Geometry
from org.geoserver.wps.process import RawData
from org.geoserver.wps.process import StreamRawData

@process(
  inputs={'input': (RawData, 'The raw data input', {'mimeTypes':'application/json,text/xml'}), 
          'outputMimeType': (str, 'The user chosen output mime type' , {'min': 0})},
  outputs={'result': (RawData, 'The output', {'mimeTypes':'application/json,text/xml', 'chosenMimeType':'outputMimeType'})}, 
  title='Raw', 
  description='Raw process')
def run(input, outputMimeType):
  return StreamRawData(outputMimeType, input.stream);
