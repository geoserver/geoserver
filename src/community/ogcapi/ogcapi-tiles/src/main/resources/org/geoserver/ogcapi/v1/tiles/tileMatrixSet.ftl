<#global pagetitle=model.id>
<#global pagecrumbs="<li class='breadcrumb-item'><a href='"+serviceLink("")+"'>Home</a></li><li class='breadcrumb-item'><a href='"+serviceLink("tileMatrixSets")+"'>Tile Matrix Sets</a></li><li class='breadcrumb-item active'>"+pagetitle+"</li>">
<#include "common-header.ftl">

  <h2>${model.id}</h2>
  <p class="my-4">
    Tile matrix set definition for ${model.id}, expressed in ${model.supportedCRS}
    <#if model.title??>${model.title}</#if>
    The tile matrix composing the set are reported in the following table.
  </p>

  <div class="table-responsive-xs">
    <table class="table table-striped table-hover table-bordered">
      <thead>
        <tr>
          <th>Tile matrix Id</th>
          <th>Scale denominator</th>
          <th>Top left Corner</th>
          <th>Tile Width</th>
          <th>Tile Height</th>
          <th>Matrix Width</th>
          <th>Matrix Height</th>
        </tr>
      </thead>
      <tbody>
      <#list model.tileMatrices as matrix>
        <tr>
          <td>${matrix.id}</td>
          <td>${matrix.scaleDenominator}</td>
          <td>${matrix.pointOfOrigin[0]} ${matrix.pointOfOrigin[1]}</td>
          <td>${matrix.tileWidth}</td>
          <td>${matrix.tileHeight}</td>
          <td>${matrix.matrixWidth}</td>
          <td>${matrix.matrixHeight}</td>
        </tr>
      </#list>
      </tbody>
    </table>

<#include "common-footer.ftl">
