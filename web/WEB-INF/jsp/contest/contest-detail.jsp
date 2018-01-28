<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fnt" uri="http://java.sun.com/jsp/jstl/functions" %>
<%--
  Created by IntelliJ IDEA.
  User: xanarry
  Date: 18-1-18
  Time: 下午7:39
  To change this template use File | Settings | File Templates.
--%>
<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<html>
<head>
    <title>${contest.title}</title>

    <link rel="stylesheet" href="/css/bootstrap/bootstrap.min.css">

    <script src="/js/jquery-3.2.1.min.js"></script>
    <script src="/js/bootstrap/popper.min.js"></script>
    <script src="/js/bootstrap/bootstrap.min.js"></script>

    <script>
        var SecondsTohhmmss = function (totalSeconds) {
            var hours = Math.floor(totalSeconds / 3600);
            var minutes = Math.floor((totalSeconds - (hours * 3600)) / 60);
            var seconds = totalSeconds - (hours * 3600) - (minutes * 60);

            // round seconds
            seconds = Math.round(seconds * 100) / 100

            var result = (hours < 10 ? "0" + hours : hours);
            result += "小时" + (minutes < 10 ? "0" + minutes : minutes);
            result += "分钟" + (seconds < 10 ? "0" + seconds : seconds);
            result += "秒";

            return hours + "小时" + minutes + "分钟" + seconds + "秒";
        };

        $(function () {
            setInterval("GetTime()", 1000);
        });


        function GetTime() {
            var min = $("#processBar").attr('aria-valuemin');
            var max = $("#processBar").attr('aria-valuemax');
            var cur = new Date().getTime();

            var passed = cur - min;
            var total = max - min;


            if (passed <= total) {
                $("#processBar").attr('aria-valuenow', new Date().getTime());
                $("#processBar").css('width', (passed / total) * 100 + '%');

                var remainSeconds = Math.floor((total - passed) / 1000);
                $("#remainTime").html("剩余:" + SecondsTohhmmss(remainSeconds));
                if (remainSeconds < 600) {
                    $("#processBar").attr('class', 'progress-bar bg-danger');
                } else {
                    $("#processBar").attr('class', 'progress-bar bg-success');
                }

            }
        }

        function submitContestCode() {
            console.log("asdfasdfasdf");
            var contestID = $("#inputContestID").val();
            $.ajax({
                type: 'POST',
                url: '/ajax-check-contest-register',
                data: {'inputContestID': contestID},
                dataType: "json",
                success: function (data) {
                    console.log(JSON.stringify(data));
                    //"{contestID : %s, userID :%s, registered: true}"
                    if (data.registered == true) {
                        $("#submitModeal").modal('show');
                    } else {
                        $("#messageModal").modal('show');
                    }
                },
                error: function (e) {
                    alert("ajax error occured" + JSON.stringify(e));
                }
            });
        }
    </script>
</head>
<body>
<jsp:include page="/navbar.jsp"/>
<div class="container" style="margin-top: 70px">
    <h2 align="center"><a href="/contest-overview?contestID=${contest.contestID}">${contest.title}</a></h2>

    <%--在此出获取当前时间--%>
    <jsp:useBean id="current" class="java.util.Date"/>
    <div class="card">
        <div class="card-header">
            <h5><a href="/contest-rank?contestID=${contest.contestID}">排名</a></h5>
        </div>
        <c:if test="${current.time <= contest.endTime and current.time >= contest.startTime}">
            <div class="progress" style="height: 10px;">
                <div id="processBar" class="progress-bar bg-success" role="progressbar" style="width: 0%;"
                     aria-valuenow="${contest.startTime}" aria-valuemin="${contest.startTime}"
                     aria-valuemax="${contest.endTime}"></div>
            </div>
            <label class="text-sm-right" id="remainTime" style="font-size: 10px; color: blue">剩余时间:</label>
        </c:if>

        <div class="card-body">
            <ul class="pagination justify-content-center">
                <c:forEach items="${problemList}" var="contestProblem">
                    <c:choose>
                        <c:when test="${contestProblem.problemID==problem.problemID}">
                            <li class="page-item active"><span class="page-link">${contestProblem.innerID}</span></li>
                        </c:when>
                        <c:otherwise>
                            <li class="page-item"><a class="page-link" href="/contest-detail?contestID=${contest.contestID}&curProblem=${contestProblem.innerID}">${contestProblem.innerID}</a></li>
                        </c:otherwise>
                    </c:choose>
                </c:forEach>
            </ul>

            <h4 class="text-center">${problem.title}</h4>
            <table align="center">
                <tbody>
                <tr>
                    <td><b>时间限制:</b>${problem.staticLangTimeLimit}ms</td>
                    <td></td>
                    <td><b>内存限制:</b>${problem.staticLangMemLimit}KB</td>
                </tr>
                </tbody>
            </table>

            <div class="text-center">
                <div class="btn-group" role="group">
                    <c:choose>
                        <c:when test="${current.time <= contest.endTime and current.time >= contest.startTime}">
                            <a class="btn" href="#" data-toggle="modal" data-target="#submitModeal">提交</a>
                            <a class="btn" href="/discuss-list?type=1&porcID=${contest.contestID}">讨论</a>
                        </c:when>
                        <c:otherwise>
                            <span class="btn">提交</span>
                            <span class="btn">讨论</span>
                        </c:otherwise>
                    </c:choose>
                    <a class="btn"
                       href="/contest-record-list?contestID=${contest.contestID}&problemID=${problem.problemID}">状态</a></button>
                </div>
            </div>


            <h3>题目描述</h3>
            <blockquote class="card modal-body">
                ${problem.desc}
            </blockquote>

            <h3>输入</h3>
            <blockquote class="card modal-body">
                ${problem.inputDesc}
            </blockquote>

            <h3>输出</h3>
            <blockquote class="card modal-body">
                ${problem.outputDesc}
            </blockquote>


            <div class="row">
                <div class="col-sm-6">
                    <h3>输入样例</h3>
                    <blockquote class="card">
                        <pre class="pre-scrollable" style="height: 180px">${problem.inputSample}</pre>
                    </blockquote>
                </div>

                <div class="col-sm-6">
                    <h3>输出样例</h3>
                    <blockquote class="card">
                        <pre class="pre-scrollable" style="height: 180px">${problem.outputSample}</pre>
                    </blockquote>
                </div>
            </div>

            <h3>提示</h3>
            <blockquote class="card modal-body">
                ${problem.hint != null && fnt:length(problem.hint) > 0 ? problem.hint : '无'}
            </blockquote>

            <h3>来源</h3>
            <blockquote class="card modal-body">
                ${problem.source != null && fnt:length(problem.source) > 0 ? problem.source : '无'}
            </blockquote>

            <div class="text-center">
                <div class="btn-group" role="group">
                    <c:choose>
                        <c:when test="${current.time <= contest.endTime and current.time >= contest.startTime}">
                            <a class="btn" href="#" data-toggle="modal" data-target="#submitModeal">提交</a>
                            <a class="btn" href="/discuss-list?type=0&porcID=${problem.problemID}">讨论</a>
                        </c:when>
                        <c:otherwise>
                            <span class="btn">提交</span>
                            <span class="btn">讨论</span>
                        </c:otherwise>
                    </c:choose>
                    <a class="btn"
                       href="/contest-record-list?contestID=${contest.contestID}&problemID=${problem.problemID}">状态</a></button>
                </div>
            </div>

        </div>
    </div>

    <!-- Modal -->
    <div class="modal fade" id="submitModeal" tabindex="-1" role="dialog" aria-labelledby="exampleModalCenterTitle"
         aria-hidden="true">
        <div class="modal-lg modal-dialog modal-dialog-centered" role="document">
            <div class="modal-content">

                <div class="modal-header">
                    <h5 class="modal-title" id="exampleModalLongTitle">提交代码</h5>
                    <button type="button" class="close" data-dismiss="modal" aria-label="Close">
                        <span aria-hidden="true">&times;</span>
                    </button>
                </div>

                <div class="modal-body">
                    <%--<h4 class="card-title">Special title treatment</h4>--%>
                    <form class="form-horizontal" action="/submit" method="post">
                        <input hidden name="inputContestID" id="inputContestID" value="${contest.contestID}">
                        <input hidden name="inputProblemID" id="inputProblemID" value="${problem.problemID}">
                        <div class="form-group">
                            <textarea class="form-control" name="inputCode" rows="15"></textarea>
                        </div>
                        <div class="form-row align-items-center">
                            <div class="col-sm-3">
                                <div class="input-group mb-2 mb-sm-0">
                                    <div class="input-group-addon">语言:</div>
                                    <select class="form-control" name="inputLanguage">
                                        <c:forEach items="${languages}" var="lang">
                                            <option <c:if
                                                    test="${lang.language == user.preferLanguage}"> selected </c:if>
                                                    value="${lang.language}">${lang.language}</option>
                                        </c:forEach>
                                    </select>
                                </div>
                            </div>
                            <div class="col-auto">
                                <button type="submit" class="btn btn-primary">提交代码</button>
                            </div>
                        </div>
                    </form>
                </div>

            </div>
        </div>
    </div>

    <div class="modal fade" id="messageModal">
        <div class="modal-dialog" role="document">
            <div class="modal-content">
                <div class="modal-header">
                    <h5 class="modal-title">重要提示</h5>
                    <button type="button" class="close" data-dismiss="modal" aria-label="Close">
                        <span aria-hidden="true">&times;</span>
                    </button>
                </div>
                <div class="modal-body">
                    <p>登录账户, 并且注册比赛后才能提交代码</p>
                </div>
                <div class="modal-footer">
                    <button type="button" class="btn btn-danger" data-dismiss="modal">知道了</button>
                </div>
            </div>
        </div>
    </div>

    <jsp:include page="/footer.jsp"/>
</body>
</html>