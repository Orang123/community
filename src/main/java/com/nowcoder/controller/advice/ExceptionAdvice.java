package com.nowcoder.controller.advice;

import com.nowcoder.util.CommunityUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;

@ControllerAdvice(annotations = Controller.class)
public class ExceptionAdvice {

    private static final Logger logger = LoggerFactory.getLogger(ExceptionAdvice.class);

    @ExceptionHandler({Exception.class})
    public void handleException(Exception e, HttpServletRequest request, HttpServletResponse response) throws IOException {
        logger.error("服务端发生异常: "+e.getMessage());
        for(StackTraceElement element : e.getStackTrace()) {
            logger.error(element.toString());
        }
        String xRequestedWith = request.getHeader("x-requested-with");
        //这里 发送私信框 点击叉叉x 就会报服务端异常,是因为global.js里默认 会把提示框的内容替换为ajax异步返回的 "服务器异常!"
        //所以这个类暂时注解掉
        if("XMLHttpRequest".equals(xRequestedWith)) {
            response.setContentType("application/plain;charset=utf-8");
            PrintWriter writer = response.getWriter();
//            writer.write(CommunityUtil.getJSONString(1,"服务器异常!"));
            writer.write(CommunityUtil.getJSONString(1,"服务器异常!"));
        }
        else {
            response.sendRedirect(request.getContextPath()+"/error");
        }
    }
}
