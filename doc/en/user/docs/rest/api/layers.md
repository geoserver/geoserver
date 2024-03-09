# Layers

A `layer` is a *published* resource (feature type or coverage).

## `/layers[.<format>]`

Controls all layers.

Method

:   Action

    Status code

    Formats

    Default Format

GET

:   Return all layers

    200

    HTML, XML, JSON

    HTML

POST

> 405

PUT

> 405

DELETE

> 405

## `/layers/<l>[.<format>]`

Controls a particular layer.

| Method | Action           | Status code | Formats         | Default Format | Parameters                                                    |
|--------|------------------|-------------|-----------------|----------------|---------------------------------------------------------------|
| GET    | Return layer `l` | 200         | HTML, XML, JSON | HTML           | [quietOnNotFound](layers.md#rest_api_layers_quietOnNotFound) |
| POST   |                  | 405         |                 |                |                                                               |
| PUT    | Modify layer `l` | 200         | XML,JSON        |                |                                                               |
| DELETE | Delete layer `l` | 200         |                 |                | [recurse](layers.md#rest_api_layers_recurse)                 |

### Exceptions

| Exception                           | Status code |
|-------------------------------------|-------------|
| GET for a layer that does not exist | 404         |
| PUT that changes name of layer      | 403         |
| PUT that changes resource of layer  | 403         |

### Parameters

#### `recurse` {: #rest_api_layers_recurse }

The `recurse` parameter recursively deletes all styles referenced by the specified layer. Allowed values for this parameter are "true" or "false". The default value is "false".

#### `quietOnNotFound` {: #rest_api_layers_quietOnNotFound }

The `quietOnNotFound` parameter avoids to log an Exception when the layer is not present. Note that 404 status code will be returned anyway.

## `/layers/<l>/styles[.<format>]`

Controls all styles in a given layer.

| Method | Action                          | Status code                 | Formats              | Default Format |
|--------|---------------------------------|-----------------------------|----------------------|----------------|
| GET    | Return all styles for layer `l` | 200                         | SLD, HTML, XML, JSON | HTML           |
| POST   | Add a new style to layer `l`    | 201, with `Location` header | XML, JSON            |                |
| PUT    |                                 | 405                         |                      |                |
| DELETE |                                 | 405                         |                      |                |
