from geoserver.wps import process

@process(
  inputs={},
  outputs={'result': (dict, 'The result')}, 
  title='map',
  description='Map'
)
def run():
  return {'result': {'name':'widget', 'price':12.99}}