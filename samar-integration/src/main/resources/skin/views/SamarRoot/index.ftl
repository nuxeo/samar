<@extends src="base.ftl">

<@block name="content">

<div class="query">

<form method="GET">
<input class="userInput" type="text" id="q"
  name="q" value="${This.userInput}" dir="auto" />
  
<#list This.entities as entity>
<input type="hidden" name="entity" value="${entity.id}" />
</#list>

<input class="hidden" type="submit" />
</form>

<ul class="entityFacets">
<#list This.entities as entity>
  <li>${entity.title}</li>
</#list>
</ul>

</div>

<div class="results">
<#list This.matchingDocuments as doc>

</#list>
</div>

<script type="text/javascript">
document.getElementById("q").focus();
</script>

</@block>
</@extends>
