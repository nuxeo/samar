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
	  <li class="${entity.entity.summary?has_content?string('entityFacet tag', 'tag')}">${entity.title}
	   <a href="#"  class="removeEntityLink" entity="${entity.id}">x</a></li>
	  <#if entity.entity.summary?has_content>
	  <div class="entityTooltip">
        <h3 dir="auto">${entity.title}</h3>
        <p class="ellipsis">${entity.entity.summary}</p>
	  </div>
	  </#if>
	</#list>
	</ul>

</div>

<div class="results">

	<#list This.results as result>
	<div class="resultDoc ${result.doc.type} ${result.doc.id}
	  lang-${result.doc.dublincore.language}">
	  <#if result.doc.type == 'NewsML'>
      <h2 class="headline" dir="auto"><i class="icon-doc-text"></i> ${result.doc.title}</h2>
	  <div class="ellipsis newsMLContent">
	    ${result.doc.note.note}
	  </div>
	  <#elseif result.doc.type == 'Video'>
      <h2 class="headline" dir="auto"><i class="icon-video"></i> ${result.doc.title}</h2>
	  <#if result.isVideoPlayerReady()>
      <div class="video-js-box">
      <!-- HTML5 player -->
      <video class="video-js" width="320" height="180" controls="controls" preload="auto"
         poster="${result.videoPosterLink}">
          <source src="${result.videoWebmLink}" type='video/webm' />
      </video>
      </div>
      </#if>
	  <#if result.hasSpeechTranscription()>
	  <p class="videoTranscription">
	    <#list result.doc.transcription.sections as section>
	      <span class="transcriptionSection"
	       timecode=${section.timecode_start}>${section.text}</span>
	    </#list>
	  </p>
	  </#if>
	  </#if>
	  <div style="clear: both"></div>
      <ul class="entityOccurrences">
      <#list result.occurrences as occurrence>
	     <li class="${occurrence.targetEntity.entity.summary?has_content?string('entityOccurrence tag', 'tag')}">
	     <a href="${This.currentQueryUrl}&entity=${occurrence.targetEntity.id}">${occurrence.targetEntity.title}</a></li>
 	     <#if occurrence.targetEntity.entity.summary?has_content>
 	     <div class="entityTooltip">
 	       <h3 dir="auto">${occurrence.targetEntity.title}</h3>
 	       <p class="ellipsis">${occurrence.targetEntity.entity.summary}</p>
 	     </div>  
 	     </#if>
	  </#list>
	  </ul>
	</div>
	</#list>
	
	<p class="duration">${This.duration}s</p>

</div>

<script type="text/javascript">
<!--
jQuery(document).ready(function() {
  jQuery(".removeEntityLink").click(function () {
    var entityId = jQuery(this).attr('entity');
    jQuery('#queryForm input[value=' + entityId +']').remove();
    jQuery('#queryForm').submit();
  })
  jQuery(".entityFacet").tooltip({
    position: "bottom center",
    tipClass: "entityTooltip",
  });
  jQuery(".entityOccurrence").tooltip({
    position: "top center",
    tipClass: "entityTooltip",
  });
  jQuery(".Video").each(function () {
    var videoJsElement = jQuery(this).find(".video-js");
    if (videoJsElement.length > 0 && videoJsElement.get(0) != 'undefined') {
      var video = videoJsElement.get(0);
      jQuery(this).find('.transcriptionSection').css('cursor', 'pointer');
      jQuery(this).find('.transcriptionSection').click(function() {
        video.currentTime = parseFloat(jQuery(this).attr('timecode'));
        video.play();
        return false;
      });
    }
  });
  jQuery(document).ready(function() {
	jQuery(".ellipsis").dotdotdot();
  });
  document.getElementById("q").focus();
});
-->
</script>

</@block>
</@extends>
