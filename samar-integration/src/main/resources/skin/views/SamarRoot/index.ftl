<@extends src="base.ftl">

<@block name="content">

<div class="query">

	<form id="queryForm" method="GET">
	<input class="userInput" type="text" id="q"
	  name="q" value="${This.userInput}" dir="auto" />
	  
	<#list This.entities as entity>
	<input type="hidden" name="entity" value="${entity.id}" />
	</#list>
	
	<input class="hidden" type="submit" />
	</form>

	<ul class="entityFacets">
	<#list This.entities as entity>
	  <li class="entityFacet">${entity.title}</li>
	  <div class="entityTooltip">${entity.title}</div>
	</#list>
	</ul>

</div>

<div class="results">

	<#list This.results as result>
	  <div class="resultDoc ${result.doc.type}">
	  <h2 class="headline" dir="auto">${result.doc.title}</h2>
	  <#if result.doc.type == 'NewsML'>
	  <div class="ellipsis newsMLContent lang-${result.doc.dublincore.language}">
	  ${result.doc.note.note}
	  </div>
	  </#if>
      <ul>
      <#list result.relatedEntities as entity>
	     <li>${entity.title} - ${entity.id}</li>	  
	  </#list>
	  </ul>
	  </div>
	</#list>
	
	<p class="duration">${This.duration}s</p>

</div>

<script type="text/javascript">
<!--
jQuery(document).ready(function() {
  jQuery(".entityFacet").tooltip({
    position: "bottom center",
    tipClass: "entityTooltip",
  });
  $(document).ready(function() {
	$(".ellipsis").dotdotdot();
});
  document.getElementById("q").focus();
});
-->
</script>

</@block>
</@extends>
