def app(environ, start_response):
  start_response('200 OK', [('Content-Type', 'application/json')])
  return ['{"hello":"world"}']