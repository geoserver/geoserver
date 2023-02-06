<#macro property node>
<#if node?is_enumerable>
<#list node as element>
  <@property node=element />
</#list>
<#elseif node.isComplex>
<@feature node=node.rawValue type=node.type />
<#else>
<#assign stringVal = node.value?string>
<#if !stringVal?contains("FEATURE_LINK") && stringVal != "">
<li>
   <#if node.prefix == "">
   <span class="caret">${node.name}</span>
   <#else>
   <span class="caret">${node.prefix}:${node.name}</span>
   </#if>
   <ul class="nested">
      <li>${node.value?string}</li>
   </ul>
</li>
</#if>
</#if>
</#macro>
<#macro feature node type>
<li>
   <span class="caret">${type.name}</span>
   <ul class="nested">
      <li>${node.fid}</li>
      <#list node.attributes as attribute>
      <@property node=attribute />
      </#list>
   </ul>
</li>
</#macro>
<#--
Body section of the GetFeatureInfo template, it's provided with one feature collection, and
will be called multiple times if there are various feature collections
-->
<#if collection??>
<#-- Expended only in OGC Features -->
<h1><a href="${serviceLink("/collections/${collection}")}">${data.type.name}</a></h1>
<#else>
<h1>${data.type.name}</h1>
</#if>
<#list data.features as feature>
<ul id="rootUL">
   <li>
      <span class="caret">${feature.fid}</span>
      <ul class="nested">
         <#list feature.attributes as attribute>
         <@property node=attribute/>
         </#list>
      </ul>
   </li>
</ul>
</#list>
<script>
 window.onload=function(){
 var toggler = document.getElementsByClassName("caret");
 var i;
 for (let item of toggler) {
      item.addEventListener("click", function() {
      this.parentElement.querySelector(".nested").classList.toggle("active");
      this.classList.toggle("caret-down");
    });
   }
 }
</script>
