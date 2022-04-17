package com.nowcoder.config;

import com.nowcoder.util.CommunityConstant;
import com.nowcoder.util.CommunityUtil;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.builders.WebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;

@Configuration
public class SecurityConfig extends WebSecurityConfigurerAdapter implements CommunityConstant {

    @Override
    public void configure(WebSecurity web) throws Exception {
        web.ignoring().antMatchers("/resources/**");
    }

    //配置认证服务,这里认证并没有交给Security 处理,而是采用了LoginController中的映射进行处理
    // AuthenticationManager: 认证的核心接口.
    // AuthenticationManagerBuilder: 用于构建AuthenticationManager对象的工具.
    // ProviderManager: AuthenticationManager接口的默认实现类.
//    @Override
//    protected void configure(AuthenticationManagerBuilder auth) throws Exception {
//        // 内置的认证规则
//        //userService 实现了UserDetailsService接口 实现了loadUserByUsername方法,"12345"是加密的盐值
//        //这里并没有采用内置的认证规则
//        // auth.userDetailsService(userService).passwordEncoder(new Pbkdf2PasswordEncoder("12345"));
//
//        // 自定义认证规则
//        // AuthenticationProvider: ProviderManager持有一组AuthenticationProvider,每个AuthenticationProvider负责一种认证.
//        // 委托模式: ProviderManager将认证委托给AuthenticationProvider.
//        super.configure(auth);
//    }

    //配置访问哪些url需要认证,并赋予其访问的对应用户权限
    @Override
    protected void configure(HttpSecurity http) throws Exception {
        // 授权
        http.authorizeRequests()
                .antMatchers(
                        "/user/setting",
                        "/user/upload",
                        "/user/header/url",
                        "/user/mypost/**",
                        "/user/myreply/**",
                        "/user/updatePassword",
                        "/user/logout",
                        "/discuss/add",
                        "/comment/add/**",
                        "/letter/**",
                        "/notice/**",
                        "/like",
                        "/follow",
                        "/unfollow"
                )
                .hasAnyAuthority(//访问上面的url 只要持有下面任意一个权限即可 普通用户、管理员、版主
                        AUTHORITY_USER,
                        AUTHORITY_ADMIN,
                        AUTHORITY_MODERATOR
                )
                .antMatchers(
                        "/discuss/top",
                        "/discuss/wonderful"
                )
                .hasAnyAuthority(AUTHORITY_MODERATOR)
                .antMatchers("/discuss/delete",
                        "/data/**",
                        "/actuator/**")
                .hasAnyAuthority(AUTHORITY_ADMIN)
                .anyRequest().permitAll()
                .and().csrf().disable();//关闭csrf功能 跨站请求伪造 CSRF（Cross-site request forgery） 登陆失败存在的原因
        // 权限不够时的处理
        http.exceptionHandling()
                // 没有登录
                .authenticationEntryPoint(new AuthenticationEntryPoint() {
                    @Override
                    public void commence(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse, AuthenticationException e) throws IOException, ServletException {
                        String xRequestWith = httpServletRequest.getHeader("x-requested-with");
                        if("XMLHttpRequest".equals(xRequestWith)) {//如果是ajax异步 就返回提示信息
                            httpServletResponse.setContentType("application/plain;charset=utf-8");
                            PrintWriter writer = httpServletResponse.getWriter();
                            writer.write(CommunityUtil.getJSONString(403,"你还没有登录哦!"));
                        } else {//如果是普通请求 就重定向到登陆页面 去登陆
                            httpServletResponse.sendRedirect(httpServletRequest.getContextPath()+"/login");
                        }
                    }
                })
                // 权限不足
                .accessDeniedHandler(new AccessDeniedHandler() {
                    @Override
                    public void handle(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse, AccessDeniedException e) throws IOException, ServletException {
                        String xRequestedWith = httpServletRequest.getHeader("x-requested-with");
                        if ("XMLHttpRequest".equals(xRequestedWith)) {
                            httpServletResponse.setContentType("application/plain;charset=utf-8");
                            PrintWriter writer = httpServletResponse.getWriter();
                            writer.write(CommunityUtil.getJSONString(403, "你没有访问此功能的权限!"));
                        } else {
                            httpServletResponse.sendRedirect(httpServletRequest.getContextPath() + "/denied");
                        }
                    }
                });
        // Security底层默认会自动拦截/logout请求,进行退出处理. 不是自动拦截login,如果controller中配置了 login Security就不会覆盖该访问请求
        // 覆盖它默认的逻辑,才能执行我们自己的退出代码.
        //LogoutConfigurer.java中 logoutUrl = "/logout"; 默认是"/logout" 只要修改该值,就能屏蔽Security的默认logout退出处理
        http.logout().logoutUrl("/securitylogout");
    }
}
