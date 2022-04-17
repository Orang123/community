package com.nowcoder.controller;

import com.google.code.kaptcha.Producer;
import com.nowcoder.pojo.User;
import com.nowcoder.service.UserService;
import com.nowcoder.util.CommunityConstant;
import com.nowcoder.util.CommunityUtil;
import com.nowcoder.util.MailClient;
import com.nowcoder.util.RedisKeyUtil;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import javax.imageio.ImageIO;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Controller
public class LoginController implements CommunityConstant{

    private static final Logger logger = LoggerFactory.getLogger(LoginController.class);

    @Autowired
    private UserService userService;

    @Autowired
    private Producer kaptcharProducer;

    @Autowired
    private TemplateEngine templateEngine;

    @Autowired
    private MailClient mailClient;

    @Autowired
    private RedisTemplate redisTemplate;

    @Value("${server.servlet.context-path}")
    private String contextPath;

    @RequestMapping(path = "/register",method = RequestMethod.GET)
    public String getRegisterPage(){
        return "/site/register";
    }

    @RequestMapping(path = "/login",method = RequestMethod.GET)
    public String getLoginPage(){
        return "/site/login";
    }

    @RequestMapping(path = "/register",method = RequestMethod.POST)
    public String register(Model model, User user){
        Map<String, Object> map = userService.register(user);
        if(map == null || map.isEmpty()){
            model.addAttribute("msg","注册成功,我们已经向您的邮箱发送了一封激活邮件,请尽快激活!");
            model.addAttribute("target","/index");
            return "/site/operate-result";
        }
        else{
            model.addAttribute("usernameMsg",map.get("usernameMsg"));
            model.addAttribute("passwordMsg",map.get("passwordMsg"));
            model.addAttribute("emailMsg",map.get("emailMsg"));
            return "/site/register";
        }
    }

    @RequestMapping(path = "/activation/{userId}/{code}",method = RequestMethod.GET)
    public String activation(Model model,@PathVariable("userId")int userId , @PathVariable("code")String code){
        int result = userService.activation(userId, code);
        if(result == ACTIVATION_SUCCESS){
            model.addAttribute("msg","激活成功,您的账号已经可以正常使用了!");
            model.addAttribute("target","/login");
        }
        else if(result == ACTIVATION_REPEAT){
            model.addAttribute("msg","无效操作,该账号已经激活过了!");
            model.addAttribute("target","/login");
        }
        else{
            model.addAttribute("msg","激活失败,您提供的激活码不正确!");
            model.addAttribute("target","/register");
        }
        return "/site/operate-result";
    }

    @RequestMapping(path = "/kaptcha", method = RequestMethod.GET)
    public void getKaptcha(HttpServletResponse response/*, HttpSession session*/) {
        String text = kaptcharProducer.createText();
        BufferedImage image = kaptcharProducer.createImage(text);
//        session.setAttribute("kaptcha",text);
        String kaptchaOwner = CommunityUtil.generateUUID();
        Cookie cookie = new Cookie("kaptchaOwner",kaptchaOwner);
        cookie.setMaxAge(60);
        cookie.setPath(contextPath);
        response.addCookie(cookie);
        String kaptchaKey = RedisKeyUtil.getKaptchaKey(kaptchaOwner);
        redisTemplate.opsForValue().set(kaptchaKey,text,60,TimeUnit.SECONDS);
        response.setContentType("image/png");
        try{
            ServletOutputStream os = response.getOutputStream();
            ImageIO.write(image,"png",os);
        }catch (IOException e){
            logger.error("响应验证码失败:"+e.getMessage());
        }
    }

    @RequestMapping(path = "/login",method = RequestMethod.POST)
    public String login(String username, String password, String code, boolean rememberme,
                        Model model,@CookieValue("kaptchaOwner") String kaptchaOwner
            /*, HttpSession session*/,HttpServletResponse response){
        //这里 验证码超过60s后 cookie和redis key都会失效,@CookieValue("kaptchaOwner")在请求是 会报服务端异常,会直接跳转到这个500页面,暂时还未处理 Cookie的请求异常
//        String kaptcha = (String)session.getAttribute("kaptcha");
        if(StringUtils.isBlank(kaptchaOwner)){
            model.addAttribute("codeMsg","验证码已过期!");
            return "/site/login";
        }
        String kaptchaKey = RedisKeyUtil.getKaptchaKey(kaptchaOwner);
        String kaptcha = redisTemplate.opsForValue().get(kaptchaKey).toString();
        if(StringUtils.isBlank(code) ||  StringUtils.isBlank(kaptcha) || !kaptcha.equalsIgnoreCase(code)){
            model.addAttribute("codeMsg","验证码不正确!");
            return "/site/login";
        }
        long expiredSeconds = rememberme ? REMEMBER_EXPIRED_SECONDS : DEFAULT_EXPIRED_SECONDS;
        Map<String, Object> map = userService.login(username, password, expiredSeconds);
        if(map.containsKey("ticket")){
            Cookie cookie = new Cookie("ticket",map.get("ticket").toString());
            cookie.setPath(contextPath);
            cookie.setMaxAge((int)expiredSeconds);
            response.addCookie(cookie);
            return "redirect:/index";
        }
        else{
            model.addAttribute("usernameMsg",map.get("usernameMsg"));
            model.addAttribute("passwordMsg",map.get("passwordMsg"));
            return "/site/login";
        }
    }

    @RequestMapping(path = "/forget",method = RequestMethod.GET)
    public String getForgetPage(){
        return "/site/forget";
    }

    //发送验证码给用户邮箱
    @RequestMapping(path = "/forget/code",method = RequestMethod.GET)
    @ResponseBody
    public String getForgetCode(/*HttpSession session,*/ String email, HttpServletResponse response) {
        if(StringUtils.isBlank(email)){
            return CommunityUtil.getJSONString(1,"邮箱不能为空!");
        }
        User user = userService.isEmailRegister(email);
        if(user == null){
            return CommunityUtil.getJSONString(1,"该邮箱未注册!");
        }
        String code = CommunityUtil.generateUUID().substring(0,4);
//        session.setAttribute("verifyCode",code);
        String kaptchaOwner = CommunityUtil.generateUUID();
        Cookie cookie = new Cookie("kaptchaOwner",kaptchaOwner);
        cookie.setMaxAge(60);
        cookie.setPath(contextPath);
        response.addCookie(cookie);
        String kaptchaKey = RedisKeyUtil.getKaptchaKey(kaptchaOwner);
        redisTemplate.opsForValue().set(kaptchaKey,code,60,TimeUnit.SECONDS);
        Context context = new Context();
        context.setVariable("email",email);
        context.setVariable("verifyCode",code);
        String content = templateEngine.process("/mail/forget", context);
        mailClient.sendMail(email,"找回密码",content);
        Map<String,Object> map= new HashMap<>();
        map.put("userId",user.getId());//把用户id,密码盐值回传给前端,方便重置密码时根据用户id更改密码
        map.put("salt",user.getSalt());
        return CommunityUtil.getJSONString(0,"",map);
    }

    //根据验证码是否正确 重置用户密码
    @RequestMapping(path = "/forget/password",method = RequestMethod.POST)
    public String resetPassword(Model model,int userId, String salt,String verifyCode, String password , /*HttpSession session*/
                                @CookieValue("kaptchaOwner")String kaptchaOwner) {
//        String code = (String)session.getAttribute("verifyCode");
        String kaptchaKey = RedisKeyUtil.getKaptchaKey(kaptchaOwner);
        String code = redisTemplate.opsForValue().get(kaptchaKey).toString();
        if(StringUtils.isBlank(verifyCode) || StringUtils.isBlank(code) || !code.equalsIgnoreCase(verifyCode)){
            model.addAttribute("verifyCodeMsg","验证码错误!");
            return "/site/forget";
        }
        Map<String, Object> map = userService.resetPassword(userId, salt, verifyCode, password);
        if(map.isEmpty()){
            //使用response.getWriter().write 方法不能有返回值,需为void,否则会报 getWriter() has already been called for this response
            //在Controller接口方法中，既手动调用PrintWriter向客户端输出内容，又设置了方法返回值。
            //导致servlet需要两次将结果通过PrintWriter输出到客户端，结果报错。
//            response.setContentType("text/html;charset=utf-8");
//            response.getWriter().print("<script type='text/javascript'>alert('重置密码成功!');</script>");
//            return "redirect:/login";
            model.addAttribute("msg","您的密码已经重置成功!");
            model.addAttribute("target","/login");
            return "/site/operate-result";
        }
        else{
            model.addAttribute("passwordMsg",map.get("passwordMsg"));
            return "/site/forget";
        }
    }

}
