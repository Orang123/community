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
            model.addAttribute("msg","????????????,??????????????????????????????????????????????????????,???????????????!");
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
            model.addAttribute("msg","????????????,???????????????????????????????????????!");
            model.addAttribute("target","/login");
        }
        else if(result == ACTIVATION_REPEAT){
            model.addAttribute("msg","????????????,???????????????????????????!");
            model.addAttribute("target","/login");
        }
        else{
            model.addAttribute("msg","????????????,??????????????????????????????!");
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
            logger.error("?????????????????????:"+e.getMessage());
        }
    }

    @RequestMapping(path = "/login",method = RequestMethod.POST)
    public String login(String username, String password, String code, boolean rememberme,
                        Model model,@CookieValue("kaptchaOwner") String kaptchaOwner
            /*, HttpSession session*/,HttpServletResponse response){
        //?????? ???????????????60s??? cookie???redis key????????????,@CookieValue("kaptchaOwner")???????????? ?????????????????????,????????????????????????500??????,?????????????????? Cookie???????????????
//        String kaptcha = (String)session.getAttribute("kaptcha");
        if(StringUtils.isBlank(kaptchaOwner)){
            model.addAttribute("codeMsg","??????????????????!");
            return "/site/login";
        }
        String kaptchaKey = RedisKeyUtil.getKaptchaKey(kaptchaOwner);
        String kaptcha = redisTemplate.opsForValue().get(kaptchaKey).toString();
        if(StringUtils.isBlank(code) ||  StringUtils.isBlank(kaptcha) || !kaptcha.equalsIgnoreCase(code)){
            model.addAttribute("codeMsg","??????????????????!");
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

    //??????????????????????????????
    @RequestMapping(path = "/forget/code",method = RequestMethod.GET)
    @ResponseBody
    public String getForgetCode(/*HttpSession session,*/ String email, HttpServletResponse response) {
        if(StringUtils.isBlank(email)){
            return CommunityUtil.getJSONString(1,"??????????????????!");
        }
        User user = userService.isEmailRegister(email);
        if(user == null){
            return CommunityUtil.getJSONString(1,"??????????????????!");
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
        mailClient.sendMail(email,"????????????",content);
        Map<String,Object> map= new HashMap<>();
        map.put("userId",user.getId());//?????????id,???????????????????????????,?????????????????????????????????id????????????
        map.put("salt",user.getSalt());
        return CommunityUtil.getJSONString(0,"",map);
    }

    //??????????????????????????? ??????????????????
    @RequestMapping(path = "/forget/password",method = RequestMethod.POST)
    public String resetPassword(Model model,int userId, String salt,String verifyCode, String password , /*HttpSession session*/
                                @CookieValue("kaptchaOwner")String kaptchaOwner) {
//        String code = (String)session.getAttribute("verifyCode");
        String kaptchaKey = RedisKeyUtil.getKaptchaKey(kaptchaOwner);
        String code = redisTemplate.opsForValue().get(kaptchaKey).toString();
        if(StringUtils.isBlank(verifyCode) || StringUtils.isBlank(code) || !code.equalsIgnoreCase(verifyCode)){
            model.addAttribute("verifyCodeMsg","???????????????!");
            return "/site/forget";
        }
        Map<String, Object> map = userService.resetPassword(userId, salt, verifyCode, password);
        if(map.isEmpty()){
            //??????response.getWriter().write ????????????????????????,??????void,???????????? getWriter() has already been called for this response
            //???Controller?????????????????????????????????PrintWriter?????????????????????????????????????????????????????????
            //??????servlet???????????????????????????PrintWriter????????????????????????????????????
//            response.setContentType("text/html;charset=utf-8");
//            response.getWriter().print("<script type='text/javascript'>alert('??????????????????!');</script>");
//            return "redirect:/login";
            model.addAttribute("msg","??????????????????????????????!");
            model.addAttribute("target","/login");
            return "/site/operate-result";
        }
        else{
            model.addAttribute("passwordMsg",map.get("passwordMsg"));
            return "/site/forget";
        }
    }

}
