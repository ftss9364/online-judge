package org.oj.controller;

import judge.JudgeClient;
import org.apache.ibatis.session.SqlSession;
import org.oj.controller.beans.MessageBean;
import org.oj.controller.beans.PageBean;
import org.oj.database.*;
import org.oj.model.javaBean.*;
import utils.Consts;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URLEncoder;
import java.util.Date;
import java.util.List;

/**
 * Created by xanarry on 18-1-7.
 */
@WebServlet(
        name = "SubmitServlet",
        urlPatterns = {
                "/submit",
                "/rejudge",
                "/record-list",
                "/judge-detail"
        }
)
public class SubmitServlet extends HttpServlet {
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        if (request.getRequestURI().equals("/submit")) postSubmit(request, response);
    }

    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String uri = request.getRequestURI();

        if (uri.equals("/submit"))       getSubmit(request, response);
        if (uri.equals("/record-list"))  getRecordList(request, response);
        if (uri.equals("/judge-detail")) getRecordDetail(request, response);
        if (uri.equals("/rejudge"))      rejudgeGet(request, response);

    }


    private void getSubmit(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String strContestID = request.getParameter("contestID");
        Integer contestID = 0;
        if (strContestID != null && strContestID.length() > 0) {
            contestID = Integer.parseInt(strContestID);
        }

        String strProblemID = request.getParameter("problemID");
        Integer problemID = Integer.parseInt(strProblemID);


        //检查用户是否登录, 没有登录重定向到错误页面
        String strUserID = null;
        Cookie[] cookies = request.getCookies();
        for (Cookie c : cookies) {
            if (c.getName().equals("userID")) {
                strUserID = c.getValue();
            }
        }
        if (strUserID == null) {
            MessageBean messageBean = new MessageBean("提示", "提示", "请登录再提交代码", "/", "回到首页");
            Utils.sendErrorMsg(response, messageBean);
            return;
        }

        int userID = Integer.parseInt(strUserID);

        SqlSession sqlSession = DataSource.getSqlSesion();
        //获取提交题目信息
        TableProblem tableProblem = sqlSession.getMapper(TableProblem.class);
        ProblemBean problemBean = tableProblem.getProblemByID(problemID);

        //获取系统语言列表以供选择
        TableLanguage tableLanguage = sqlSession.getMapper(TableLanguage.class);
        List<LanguageBean> languages = tableLanguage.getLanguageList();

        //获取提交代码的用户信息
        TableUser user = sqlSession.getMapper(TableUser.class);
        UserBean userBean = user.getUserByID(userID);


        //获取用户在之前对本题目的提交记录
        TableSubmitRecord tableSubmitRecord = sqlSession.getMapper(TableSubmitRecord.class);
        List<SubmitRecordBean> submitRecordBeans = tableSubmitRecord.getSubmitRecordList(0, problemID, userID, null, null, 0, 3);

        sqlSession.close();

        request.setAttribute("problem", problemBean);
        request.setAttribute("languages", languages);
        request.setAttribute("user", userBean);
        request.setAttribute("recordList", submitRecordBeans);


        request.getRequestDispatcher("/WEB-INF/jsp/submittion/submit.jsp").forward(request, response);
    }


    private void postSubmit(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String strContestID = request.getParameter("inputContestID");
        int contesetID = 0;
        if (strContestID != null && strContestID.length() > 0) {
            contesetID = Integer.parseInt(strContestID);
        }

        String strProblemID = request.getParameter("inputProblemID");
        String code = request.getParameter("inputCode");
        String language = request.getParameter("inputLanguage");

        String strUserID = null;
        Cookie[] cookies = request.getCookies();
        for (Cookie c : cookies) {
            if (c.getName().equals("userID")) {
                strUserID = c.getValue();
            }
        }

        //检查用户是否登录
        if (strUserID == null) {
            MessageBean messageBean = new MessageBean("提示", "提示", "请登录再提交代码", "/", "回到首页");
            Utils.sendErrorMsg(response, messageBean);
            return;
        }


        int problemID = Integer.parseInt(strProblemID);
        int userID = Integer.parseInt(strUserID);


        SubmitRecordBean submitRecordBean = new SubmitRecordBean();//生成提交记录
        submitRecordBean.setProblemID(problemID);
        submitRecordBean.setUserID(userID);
        submitRecordBean.setContestID(contesetID);//非比赛提交的代码统一设置为0
        submitRecordBean.setResult(Consts.result[0]);
        submitRecordBean.setLanguage(language);
        submitRecordBean.setSourceCode(code);
        submitRecordBean.setCodeLength(code.length());
        submitRecordBean.setSubmitTime(new Date().getTime());


        SqlSession sqlSession = DataSource.getSqlSesion();
        TableProblem tableProblem = sqlSession.getMapper(TableProblem.class);
        ProblemBean problemBean = tableProblem.getProblemByID(problemID); //获取提交代码所属的题目

        //获取测试点路径

        //提交数据库
        TableSubmitRecord tableSubmitRecord = sqlSession.getMapper(TableSubmitRecord.class);
        tableSubmitRecord.addSubmitRecord(submitRecordBean);
        sqlSession.commit();
        sqlSession.close();


        //提交代码, 任何与提交代码到评测机的相关的代码都必须在记录写入数据库之后, 后续状态与结果的更新由,judge client完成
        //网页需要手动刷新才能看到更新
        JudgeClient client = (JudgeClient) getServletContext().getAttribute("judgeClient");
        //client.getState()
        client.submit(submitRecordBean, problemBean);
        if (contesetID != 0) {
            response.sendRedirect("/contest-record-list?contestID=" + contesetID);
        } else {
            response.sendRedirect("/record-list");
        }
    }


    private void getRecordList(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String strPage = request.getParameter("page");
        int page =  strPage != null ? Integer.parseInt(strPage) : 1;

        String userName = request.getParameter("userName");
        String strProblemID = request.getParameter("problemID");
        String result = request.getParameter("result");
        String language = request.getParameter("language");

        userName = userName != null && userName.length() > 0 ? userName : null;
        Integer problemID = strProblemID != null && strProblemID.length() > 0 ? Integer.parseInt(strProblemID) - 1000 : null;
        result = result != null && result.length() > 0 ? result : null;
        language = language != null && language.length() > 0 ? language : null;

        System.out.println(problemID);



        SqlSession sqlSession = DataSource.getSqlSesion();
        ViewSubmitRecord submitRecord = sqlSession.getMapper(ViewSubmitRecord.class);
        List<ViewSubmitRecordBean> submitRecordBeans = submitRecord.getSubmitRecordList(0, problemID, userName, result, language, (page-1)*Consts.COUNT_PER_PAGE, Consts.COUNT_PER_PAGE);

        //获取分页信息
        int recordCount = submitRecord.getCountOnCondition(0, problemID, userName, result, language);
        PageBean pageBean = Utils.getPagination(recordCount, page, request);

        sqlSession.close();

        request.setAttribute("recordList", submitRecordBeans);
        request.setAttribute("pageInfo", pageBean);
        request.getRequestDispatcher("/WEB-INF/jsp/submittion/record-list.jsp").forward(request, response);
    }


    private void getRecordDetail(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String strSubmitID = request.getParameter("submitID");
        if (strSubmitID == null) {
            //send error page
            return;
        }

        Integer submitID = Integer.parseInt(strSubmitID);

        SqlSession sqlSession = DataSource.getSqlSesion();
        TableJudgeDetail tableJudgeDetail = sqlSession.getMapper(TableJudgeDetail.class);
        List<JudgeDetailBean> judgeDetailList = tableJudgeDetail.getJudegeDetailBySubmitID(submitID);

        TableSubmitRecord tableSubmitRecord = sqlSession.getMapper(TableSubmitRecord.class);
        SubmitRecordBean submitRecordBean = tableSubmitRecord.getSubmitRecordByID(submitID);

        TableCompileInfo tableCompileInfo = sqlSession.getMapper(TableCompileInfo.class);
        CompileInfoBean compileInfoBean = tableCompileInfo.getCompileResult(submitID);
        if (compileInfoBean == null) {
            compileInfoBean = new CompileInfoBean(submitID, "");
        }

        TableSystemError tableSystemError = sqlSession.getMapper(TableSystemError.class);
        SystemErrorBean systemErrorBean = tableSystemError.getSystemErrorMessage(submitID);
        sqlSession.close();

        request.setAttribute("detailList", judgeDetailList);
        request.setAttribute("systemError", systemErrorBean);
        request.setAttribute("record", submitRecordBean);
        request.setAttribute("compileInfo", compileInfoBean);
        request.getRequestDispatcher("/WEB-INF/jsp/submittion/judge-detail.jsp").forward(request, response);
    }

    private void rejudgeGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String strSubmitID = request.getParameter("submitID");
        int submitID = strSubmitID != null && strSubmitID.length() > 0 ? Integer.parseInt(strSubmitID) : -1;
        if (submitID != -1) {
            SqlSession sqlSession = DataSource.getSqlSesion();
            /*获取提交信息*/
            TableSubmitRecord tableSubmitRecord = sqlSession.getMapper(TableSubmitRecord.class);
            SubmitRecordBean submit = tableSubmitRecord.getSubmitRecordByID(submitID);
            /*获取题目信息*/
            TableProblem tableProblem = sqlSession.getMapper(TableProblem.class);
            ProblemBean problemBean = tableProblem.getProblemByID(submit.getProblemID()); //获取提交代码所属的题目


            /*删除当前记录在数据库中的信息, 触发器会删除相关的judge-detail, compile-info*/
            tableSubmitRecord.deleteSubmitRecord(submitID);

            /*重置提交中需要更新的信息*/
            submit.setResult(Consts.result[0]);/*重置评测结果*/

            //提交数据库
            tableSubmitRecord.addSubmitRecord(submit);
            sqlSession.commit();
            sqlSession.close();

            JudgeClient client = (JudgeClient) getServletContext().getAttribute("judgeClient");
            //client.getState()
            client.submit(submit, problemBean);
            if (submit.getContestID() != 0) {
                response.sendRedirect("/contest-record-list?contestID=" + submit.getContestID());
            } else {
                response.sendRedirect("/record-list");
            }
        } else {
            Utils.sendErrorMsg(response, new MessageBean("错误", "错误", "不存在的提交ID", request.getHeader("referer"), "返回提交记录"));
        }
    }
}