<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<c:set var="ctx" value="${pageContext.request.contextPath}" scope="application"/>
<c:set var="ctxRes" value="${pageContext.request.contextPath}/resources" scope="application"/>
<!DOCTYPE html>
<html>
<head>
    <meta charset="utf-8">
    <meta http-equiv="cache-control" content="no-cache">
    <meta http-equiv="pragma" content="no-cache">
    <meta http-equiv="expires" content="0">
    <meta name="viewport" content="width=device-width, initial-scale=1.0" />
    <link rel="shortcut icon" href="${ctxRes}/img/kurento.png" type="image/png" />

    <link rel="stylesheet"
          href="${ctx}/webjars/bootstrap/dist/css/bootstrap.min.css">
    <link rel="stylesheet"
          href="${ctx}/webjars/ekko-lightbox/dist/ekko-lightbox.min.css">
    <link rel="stylesheet" href="${ctx}/webjars/demo-console/1.5.1/index.css">
    <link rel="stylesheet" href="${ctxRes}/css/kurento.css">

    <script src="${ctx}/webjars/jquery/dist/jquery.js"></script>
    <script src="${ctx}/webjars/bootstrap/dist/js/bootstrap.min.js"></script>
    <script src="${ctx}/webjars/ekko-lightbox/dist/ekko-lightbox.min.js"></script>
    <script src="${ctx}/webjars/adapter.js/adapter.js"></script>
    <script src="${ctx}/webjars/demo-console/index.js"></script>

    <script src="${ctx}/js/kurento-utils.js"></script>
    <script src="${ctxRes}/js/index.js"></script>
    <title>Kurento Tutorial 3: Video Call 1 to N with WebRTC</title>
</head>
<body>
<header>
    <div class="navbar navbar-inverse navbar-fixed-top">
        <div class="container">
            <div class="navbar-header">
                <button type="button" class="navbar-toggle" data-toggle="collapse"
                        data-target=".navbar-collapse"></button>
                <a class="navbar-brand" href="">Kurento Tutorial</a>
            </div>
            <div class="collapse navbar-collapse"
                 id="bs-example-navbar-collapse-1">
                <ul class="nav navbar-nav navbar-right">
                    <li><a
                            href="https://github.com/Kurento/kurento-tutorial-java/tree/master/kurento-one2many-call"><span
                            class="glyphicon glyphicon-file"></span> Source Code</a></li>
                </ul>
            </div>
        </div>
    </div>
</header>
<div class="container">
    <div class="page-header">
        <h1>Tutorial 3: Video Call 1 to N with WebRTC</h1>
        <p>
            This web application consists on an one to many video call using <a
                href="http://www.webrtc.org/">WebRTC</a>. In other words, this is
            an implementation of a video broadcasting application. There are two
            types of users in this application: 1 peer sending media (let's call
            it <i>Presenter</i>) and N peers receiving the media of the <i>Presenter</i>
            (let's call them <i>Viewers</i>). Thus, the <a
                href="${ctxRes}/img/pipeline.png" data-toggle="lightbox"
                data-title="Video Call 1 to N Media Pipeline"
                data-footer="1 WebRtcEnpoint (Presenter) is set in SendOnly mode, and its stream is sent to N WebRtcEnpoints (Viewers), whics are set in RecvOnly mode.">Media
            Pipeline</a> is composed by 1+N interconnected <i>WebRtcEndpoints</i>.
            To run this demo follow these steps:
        </p>
        <ol>
            <li>Open this page with a browser compliant with WebRTC
                (Chrome, Firefox).</li>
            <li>If you would like to be the <i>Presenter</i> of the
                communication, click on <i>Presenter</i> button. There can only be
                one of this kind of peer, and so, if the <i>Presenter</i> peer is
                already present, the application returns an error (see the the
                console).
            </li>
            <li>Grant the access to the camera and microphone. After the
                SDP negotiation the communication should start.</li>
            <li>In a different machine (or a different tab in the same
                browser) click on <i>Viewer</i> button to see the <i>Presenter</i>
                stream.
            </li>
            <li>Click on <i>Stop</i> to finish the communication. When <i>Presenter</i>
                clicks on <i>Stop</i>, the rest of <i>Viewers</i> (if any) ends the
                communication too.
            </li>
        </ol>
    </div>
    <div class="row">
        <div class="col-md-5">
            <div class="row">
                <div class="col-md-12">
                    <a id="presenter" href="#" class="btn btn-success"><span
                            class="glyphicon glyphicon-play"></span> Presenter </a> <a
                        id="viewer" href="#" class="btn btn-primary"><span
                        class="glyphicon glyphicon-user"></span> Viewer</a> <a id="stop"
                                                                               href="#" class="btn btn-danger"><span
                        class="glyphicon glyphicon-stop"></span> Stop</a>
                </div>
            </div>
            <br /> <label class="control-label" for="console">Console</label><br>
            <br>
            <div id="console" class="democonsole">
                <ul></ul>
            </div>
        </div>
        <div class="col-md-7">
            <div id="videoBig">
                <video id="video" autoplay width="640px" height="480px"
                       poster="${ctxRes}/img/webrtc.png"></video>
            </div>
        </div>
    </div>
</div>

<footer>
    <div class="foot-fixed-bottom">
        <div class="container text-center">
            <hr />
            <div class="row">&copy; 2014-2015 Kurento</div>
            <div class="row">
                <div class="col-md-4">
                    <a href="http://www.urjc.es"><img src="${ctxRes}/img/urjc.gif"
                                                      alt="Universidad Rey Juan Carlos" height="50px" /></a>
                </div>
                <div class="col-md-4">
                    <a href="http://www.kurento.org"><img src="${ctxRes}/img/kurento.png"
                                                          alt="Kurento" height="50px" /></a>
                </div>
                <div class="col-md-4">
                    <a href="http://www.naevatec.com"><img src="${ctxRes}/img/naevatec.png"
                                                           alt="Naevatec" height="50px" /></a>
                </div>
            </div>
        </div>
    </div>
</footer>
</body>
</html>
