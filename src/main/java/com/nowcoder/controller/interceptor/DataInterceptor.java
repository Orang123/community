package com.nowcoder.controller.interceptor;

import com.nowcoder.pojo.User;
import com.nowcoder.service.DataService;
import com.nowcoder.util.HostHolder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.net.InetAddress;


@Component
public class DataInterceptor implements HandlerInterceptor {

    @Autowired
    private DataService dataService;

    @Autowired
    private HostHolder hostHolder;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        //uv 不需要登陆 只是访问就计入访问客户机ip
        //这个显示的是127.0.0.1 服务器ip 如果localhost访问 chrome edge localhost会显示为0:0:0:0:0:0:0:1 会出现localhost和127.0.0.1的显示误区,导致同一ip多 计算了一次 显示uv为2
//        String hostAddress = request.getRemoteHost();
        //这个显示的本机的ip 192.168.1.111 不会出现 localhost和127.0.0.1的显示误区,导致同一ip多 计算了一次 显示uv为2
        String hostAddress = InetAddress.getLocalHost().getHostAddress();
        dataService.recordUV(hostAddress);
        //dau只有登陆 用户才能统计userId
        User user = hostHolder.getUser();
        if(user != null)
            dataService.recordDAU(user.getId());
        return true;
    }
}
