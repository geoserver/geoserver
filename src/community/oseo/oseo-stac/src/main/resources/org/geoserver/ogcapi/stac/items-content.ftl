    <#-- 
    Body section of the STAC items template, it's provided with one feature collection, and
    will be called multiple times if there are various feature collections
    -->

    <h1 id="title">${collection} items</h1>

  <div class="row">
      <#list data.features as item>
      <#assign a = item.attributes />
      <div class="col-xs-12 col-md-6 col-lg-4 pb-4">
        <div class="card h-100">
          <div class="card-header">
            <h2><a href="${serviceLink("collections/${collection}/items/${a.identifier.value}")}">${a.identifier.value}</a></h2>
          </div>
          <#include "item_include.ftl">
        </div>
      </div>
      </#list>
    </div>
