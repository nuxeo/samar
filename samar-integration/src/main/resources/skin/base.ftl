<html>
<head>
  <title>
     <@block name="title">
     Samar - سمر
     </@block>
  </title>
  <meta http-equiv="Content-Type" content="text/html;charset=UTF-8"/>
  <link rel="icon" type="image/png" href="${skinPath}/img/favicon.png" />
  <link rel="stylesheet" href="${skinPath}/css/site.css" type="text/css" media="screen" charset="utf-8">
  <link rel="shortcut icon" href="${skinPath}/image/favicon.gif" />
  <script src="http://ajax.googleapis.com/ajax/libs/jquery/1/jquery.min.js"></script>
  <script src="http://cdn.jquerytools.org/1.2.7/full/jquery.tools.min.js"></script>
  <script src="${skinPath}/js/jquery.dotdotdot-1.5.1.js" type="text/javascript"></script>
  <link rel="stylesheet" href="http://code.jquery.com/ui/1.9.1/themes/base/jquery-ui.css" />
  <script src="http://code.jquery.com/ui/1.9.1/jquery-ui.js"></script>
  <link rel="stylesheet" href="${skinPath}/css/fontello.css"><!--[if IE 7]>
  <link rel="stylesheet" href="${skinPath}/css/fontello-ie7.css"><![endif]-->
  <@block name="stylesheets" />
  <@block name="header_scripts" />
</head>

<body>

<div class="header">
  <a href="${This.baseUrl}"><img src="${skinPath}/img/encart-site-samar-852px.png" /></a>
</div>

<div class="content">
  <@block name="content" />
</div>
</body>
</html>
