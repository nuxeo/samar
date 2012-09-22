<html>
<head>
  <title>
     <@block name="title">
     SAMAR
     </@block>
  </title>
  <meta http-equiv="Content-Type" content="text/html;charset=UTF-8"/>
  <link rel="stylesheet" href="${skinPath}/css/site.css" type="text/css" media="screen" charset="utf-8">
  <link rel="shortcut icon" href="${skinPath}/image/favicon.gif" />
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
