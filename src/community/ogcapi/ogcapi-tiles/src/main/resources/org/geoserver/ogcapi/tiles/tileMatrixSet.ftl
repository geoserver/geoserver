<#include "common-header.ftl">
<h2>${model.identifier}</h2>
<ul>

<p>
Tile matrix set definition for ${model.identifier}, expressed in ${model.supportedCRS}
<#if model.title??>${model.title}</#if>

The tile matrix composing the set are reported in the following table.

<table style="width:auto">
  <tr><th>Tile matrix Id</th><th>Scale denominator</th><th>Top left Corner</th><th>Tile Width</th><th>Tile Height</th><th>Matrix Width</th><th>Matrix Height</th>
  <#list model.tileMatrix as matrix>
    <tr><td>${matrix.identifier}</td><td>${matrix.scaleDenominator}</td><td>${matrix.topLeftCorner[0]} ${matrix.topLeftCorner[1]}</td><td>${matrix.tileWidth}</td><td>${matrix.tileHeight}</td><td>${matrix.matrixWidth}</td><td>${matrix.matrixHeight}</td>
  </#list>
</table>

<#include "common-footer.ftl">
