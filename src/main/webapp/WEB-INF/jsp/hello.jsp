<%--
  Created by IntelliJ IDEA.
  User: H-QIU
  Date: 2016/11/19
  Time: 10:59
  To change this template use File | Settings | File Templates.
--%>
<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<script type="text/javascript" src="http://apps.bdimg.com/libs/jquery/1.6.4/jquery.js"></script>
<script type="text/javascript">
    var  ws;
    if ('WebSocket' in window) {
        ws = new WebSocket("ws://localhost:8084/webSocketServer");
    } else if ('MozWebSocket' in window) {
        ws = new MozWebSocket("ws://localhost:8084/webSocketServer");
    } else {

    }
    ws.onopen = function (evnt) {
        alert('连接成功');
    };

    ws.onmessage = function (evnt) {
        var rs = evnt.data;


    };
    ws.onerror = function (evnt) {
    };
    ws.onclose = function (evnt) {
    }
    function  send(){
        var value= '{"name":"tom","sex":"男","age":"24"}';
        ws.send(value);
    }
</script>
<html>
<head>
    <title>测试</title>
</head>
<body>
    <h1>你好！${name}</h1>
    <input type="button"  value="send" onclick="javascript:send();"  />
</body>
</html>
