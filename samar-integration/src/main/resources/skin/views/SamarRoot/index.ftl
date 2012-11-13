<@extends src="base.ftl">

<@block name="content">

<div class="query">

	<form id="queryForm" method="GET">
	<input class="userInput" type="text" id="q"
	  name="q" value="${This.userInput}" dir="auto" />

	<#list This.entities as entity>
	  <#include "/entityFormSelection.ftl">
	</#list>

	<input class="hidden" type="submit" />
	</form>

	<ul class="entityFacets">
	<#list This.entities as entity>
	  <#include "/entityFacet.ftl">
	</#list>
	</ul>

</div>

<div class="results">

  <#list This.results as result>
  <div class="resultDoc ${result.doc.type} ${result.doc.id}">
    <#if result.doc.type == 'NewsML'>
      <h2 class="headline lang-${result.doc.dublincore.language}"><i class="icon-doc-text"></i> ${result.doc.title}</h2>
      <div class="newsMLContent lang-${result.doc.dublincore.language}">
        ${result.doc.note.note}
      </div>
      <#include "entityOccurrence.ftl">
      <div class="translations">
        <#list result.translation.getTranslatedFields('note:note')?values as translation>
        <#if translation['text']?has_content>
        <h3><strong>[${translation['language']?upper_case}]</strong> ${result.getTranslatedField('dc:title', translation['language'])}</h3>
        <div class="newsMLContent lang-${translation['language']}">
          ${translation['text']}
        </div>
        </#if>
        </#list>
      </div>

    <#elseif result.doc.type == 'Video'>
      <h2 class="headline lang-${result.doc.dublincore.language}"><i class="icon-video"></i> ${result.doc.title}</h2>
      <#if result.isVideoPlayerReady()>
        <div class="video-js-box">
          <!-- HTML5 player -->
          <video class="video-js" width="320" height="180" controls="controls" preload="none"
              poster="${result.videoPosterLink}">
            <#if result.videoWebmLink?has_content><source src="${result.videoWebmLink}" type='video/webm' /></#if>
            <#if result.videoMP4Link?has_content><source src="${result.videoMP4Link}" type='video/mp4' /></#if>
          </video>
        </div>
      </#if>
      <#if result.hasSpeechTranscription()>
        <p class="videoTranscription lang-${result.doc.dublincore.language}">
          <#list result.doc.transcription.sections as section>
            <span class="transcriptionSection" timecode=${section.timecode_start}>${section.text}</span>
          </#list>
        </p>
      </#if>
     <#include "entityOccurrence.ftl">
     <div style="clear: both"></div>

     <div class="translations">
       <#list result.translation.getTranslatedFields('relatedtext:relatedtextresources_transcription')?values as translation>
       <#if translation['text']?has_content>
         <h3><strong>[${translation['language']?upper_case}]</strong> ${result.getTranslatedField('dc:title', translation['language'])}</h3>
         <p class="lang-${translation['language']}">${translation['text']}</p>
       </#if>
       </#list>
     </div>

     </#if>
   </div>
   <div style="clear: both"></div>
</#list>

<p class="duration">${This.duration}s</p>

</div>

<script type="text/javascript">
<!--
jQuery(document).ready(function() {
  var icons = {
      header: "ui-icon-circle-arrow-e",
      activeHeader: "ui-icon-circle-arrow-s"
  };
  jQuery(".translations").accordion({
      icons: icons,
      collapsible: true,
      active: false,
      heightStyle: "content"
  });
  jQuery("#queryForm .userInput").autocomplete({
    source: "samar/suggest",
    minLength: 2,
    select: function(event, ui) {
       if (ui.item) {
         jQuery('#queryForm').append(ui.item.entityFormSelectionHTML);
         jQuery('#queryForm .userInput').attr('value', '');
         jQuery('#queryForm').submit();
       }
    }
  });

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
      jQuery(video).on('canplay', function(e) {
          console.log("canplay fired");
          if (video.seekToTimeWhenReady != undefined) {
              video.currentTime = video.seekToTimeWhenReady;
              video.seekToTimeWhenReady = undefined;
              video.play();
          }
      });
      jQuery(this).find('.transcriptionSection').css('cursor', 'pointer');
      jQuery(this).find('.transcriptionSection').click(function() {
        var timecode = parseFloat(jQuery(this).attr('timecode'));
        if (video.buffered.length == 0) {
             // video has not yet been downloaded by the client: store
             // the required seek timecode for later useage in the
             // 'canplay' event handler
             video.seekToTimeWhenReady = timecode;
             // trigger the download of the video
             video.play();
             video.pause();
        } else {
            video.currentTime = timecode;
            video.play();
        }
        return false;
      });
    }
  });

  document.getElementById("q").focus();
});
-->
</script>

</@block>
</@extends>
