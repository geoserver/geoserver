Additional "vendor options" property for `label_obstacles`:

| Property | Required? | Description | Default value |
|----|----|----|----|
| `x-labelObstacle` | No | Marks the symbolizer as an obstacle such that labels drawn via a `text symbolizer <ysld_reference_symbolizers_text>` will not be drawn over top of these features. Options are `true` or `false`. Note that the bounding boxes of features are used when calculating obstacles, so unintended effects may occur when marking a line or polygon symbolizer as an obstacle. | `false` |
