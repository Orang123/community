package com.nowcoder.controller;

import com.nowcoder.annotation.LoginRequired;
import com.nowcoder.pojo.Comment;
import com.nowcoder.pojo.DiscussPost;
import com.nowcoder.pojo.Page;
import com.nowcoder.pojo.User;
import com.nowcoder.service.*;
import com.nowcoder.util.CommunityConstant;
import com.nowcoder.util.CommunityUtil;
import com.nowcoder.util.HostHolder;
import com.nowcoder.util.RedisKeyUtil;
import com.qiniu.util.Auth;
import com.qiniu.util.StringMap;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/user")
public class UserController implements CommunityConstant {

    private static final Logger logger = LoggerFactory.getLogger(UserController.class);

    @Autowired
    private UserService userService;

    @Autowired
    private DiscussPostService discussPostService;

    @Value("${community.path.domain}")
    private String domain;

    @Value("${server.servlet.context-path}")
    private String contextPath;

    @Value("${community.path.upload}")
    private String uploadPath;

    @Autowired
    private HostHolder hostHolder;

    @Autowired
    private LikeService likeService;

    @Autowired
    private FollowService followService;

    @Autowired
    private CommentService commentService;

    @Value("${qiniu.key.access}")
    private String accesskey;

    @Value("${qiniu.key.secret}")
    private String secretKey;

    @Value("${qiniu.bucket.header.name}")
    private String headerBucketName;

    @Value("${qiniu.bucket.header.url}")
    private String headerBucketUrl;

    @LoginRequired
    @RequestMapping(path = "/logout",method = RequestMethod.GET)
    public String logout(@CookieValue String ticket){
        userService.logout(ticket);
        //清除获取登陆凭证时 注入Security中的user信息
        SecurityContextHolder.clearContext();
        return "redirect:/login";
    }

    @LoginRequired
    @RequestMapping(path = "/setting",method = RequestMethod.GET)
    public String setting(Model model){
        // 上传文件名称
        String fileName = CommunityUtil.generateUUID();
        // 设置响应信息
        StringMap policy = new StringMap();
        //规定异步上传七牛云服务器成功时 返回一个json字符串 {"code":"0"}
        policy.put("returnBody",CommunityUtil.getJSONString(0));
        // 生成上传凭证 用于前端表单提交给七牛云服务器
        Auth auth = Auth.create(accesskey, secretKey);
        //3600 表示 token的有效时间为1个小时,没访问一次主页 都会生成一个有效期1个小时的token
        String uploadToken = auth.uploadToken(headerBucketName, fileName, 3600, policy);
        //前端ajax客户端上传 需要用到随机生成的文件名
        model.addAttribute("fileName",fileName);
        model.addAttribute("uploadToken",uploadToken);
        return "/site/setting";
    }

    @RequestMapping(path = "/header/url", method = RequestMethod.POST)
    @ResponseBody
    public String updateHeaderUrl(String fileName) {
        if(StringUtils.isBlank(fileName)) {
            return CommunityUtil.getJSONString(1,"文件名不能为空!");
        }
        //这里fileName并不包含图片格式后缀,七牛云服务器会自动根据图片名匹配 实际上传的文件 不影响显示
        String headerUrl = headerBucketUrl + "/" + fileName;
        userService.updateHeader(hostHolder.getUser().getId(),headerUrl);
        return CommunityUtil.getJSONString(0);
    }

    //图片上传至本地 废弃
    @LoginRequired
    @RequestMapping(path = "/upload",method = RequestMethod.POST)
    public String uploadHeader(MultipartFile headerImage, Model model) {
        if(headerImage == null){
            model.addAttribute("error","您还没有选择图片!");
            return "/site/setting";
        }
        String fileName = headerImage.getOriginalFilename();
        String suffix = fileName.substring(fileName.lastIndexOf("."));
        if(StringUtils.isBlank(suffix)){
            model.addAttribute("error","文件的格式不正确!");
            return "/site/setting";
        }
        fileName = CommunityUtil.generateUUID()+suffix;
        File dest = new File(uploadPath+"/"+fileName);
        try {
            headerImage.transferTo(dest);
        } catch (IOException e) {
            logger.error("上传文件失败: "+ e.getMessage());
            throw new RuntimeException("上传文件失败,服务器发生异常!",e);
        }
        User user = hostHolder.getUser();
        String headerUrl=domain+contextPath+"/user/header/"+fileName;
        userService.updateHeader(user.getId(),headerUrl);
        model.addAttribute("msg","您的新头像已经上传成功!");
        model.addAttribute("target","/user/setting");
        return "/site/operate-result";
    }

    //获取存放在本地的头像 显示 废弃
    @RequestMapping(path = "/header/{fileName}",method = RequestMethod.GET)
    public void getHeader(@PathVariable("fileName") String fileName, HttpServletResponse  response) {
        String suffix = fileName.substring(fileName.lastIndexOf(".") + 1);
        fileName = uploadPath+"/"+fileName;
        response.setContentType("image/"+suffix);
        try(FileInputStream fis = new FileInputStream(fileName);
            ServletOutputStream os = response.getOutputStream();) {
            byte[] buffer = new byte[1024];
            int b = 0;
            while((b = fis.read(buffer)) != -1){
                os.write(buffer,0,b);
            }
        } catch (IOException e) {
            logger.error("读取头像失败: "+e.getMessage());
        }
    }

    @LoginRequired
    @RequestMapping(path = "/updatePassword",method = RequestMethod.POST)
    public String updatePassword(Model model, String oldPassword, String newPassword, String confirmPassword) {
        if (StringUtils.isBlank(oldPassword)) {
            model.addAttribute("oldPasswordMsg","原密码不能为空!");
            return "/site/setting";
        }
        User user = hostHolder.getUser();
        if(!CommunityUtil.md5(oldPassword+user.getSalt()).equals(user.getPassword())){
            model.addAttribute("oldPasswordMsg","原始密码输入错误!");
            return "/site/setting";
        }
        if (StringUtils.isBlank(newPassword)) {
            model.addAttribute("newPasswordMsg", "新密码不能为空!");
            return "/site/setting";
        }
        if (StringUtils.isBlank(confirmPassword)) {
            model.addAttribute("confirmPasswordMsg", "确认密码不能为空!");
            return "/site/setting";
        }
        if(!newPassword.equals(confirmPassword)){
            model.addAttribute("confirmPasswordMsg", "两次输入的新密码不一致!");
            return "/site/setting";
        }
        userService.updatePassword(user.getId(),user.getSalt(),newPassword);
        //这个写法可是ok的 因为在同一Controller下,所以可以相对路径 请求,但是不能/logout 因为不存在跟路径为/logout的mapping,只有/user/logout的映射
//        return "redirect:logout";
        return "redirect:/user/logout";
    }

    @RequestMapping(path = "/profile/{userId}",method = RequestMethod.GET)
    public String getProfilePage(@PathVariable("userId")int userId, Model model) {
        User user = userService.findUserById(userId);
        if(user == null)
            throw new RuntimeException("该用户不存在!");
        model.addAttribute("user",user);
        model.addAttribute("likeCount",likeService.findUserLikeCount(userId));
        model.addAttribute("followeeCount",followService.findFolloweeCount(userId,ENTITY_TYPE_USER));
        model.addAttribute("followerCount",followService.findFollowerCount(ENTITY_TYPE_USER,userId));
        //这里注意,thymeleaf默认只能处理 boolean 即:如果hasFollowed为boolean才能 hasFollowed?'已关注':'关注TA' 这样判断,否则如果为int都得显示的判断 不等于0或别的数值
        int hasFollowed = 0;
        if(hostHolder.getUser() != null)
            hasFollowed = followService.hasFollowed(hostHolder.getUser().getId(),ENTITY_TYPE_USER,userId);
        model.addAttribute("hasFollowed",hasFollowed);
        return "/site/profile";
    }

    @LoginRequired
    @RequestMapping(path = "/mypost/{userId}",method = RequestMethod.GET)
    public String getMyPost(Model model, Page page, @PathVariable("userId")int userId) {
        User user = userService.findUserById(userId);
        if (user == null) {
            throw new RuntimeException("该用户不存在！");
        }
        page.setLimit(5);
        page.setPath("/user/mypost/"+userId);
        page.setRows(discussPostService.findDiscussPostsRows(userId));
        model.addAttribute("user",user);
        //我的帖子 按照最热去排序 orderMode=1
        List<DiscussPost> discussPosts = discussPostService.findDiscussPosts(userId, page.getOffset(), page.getLimit(), 1);
        List<Map<String,Object>> list = new ArrayList<>();
        for(DiscussPost post : discussPosts) {
            Map<String,Object> map = new HashMap<>();
            map.put("post",post);
            long likeCount = likeService.findEntityLikeCount(ENTITY_TYPE_POST, post.getId());
            map.put("likeCount",likeCount);
            list.add(map);
        }
        model.addAttribute("list",list);
        return "/site/my-post";
    }

    @LoginRequired
    @RequestMapping(path = "/myreply/{userId}",method = RequestMethod.GET)
    public String getMyReply(Model model, Page page, @PathVariable("userId") int userId) {
        User user = userService.findUserById(userId);
        if (user == null) {
            throw new RuntimeException("该用户不存在！");
        }
        page.setLimit(5);
        page.setPath("/user/myreply/"+userId);
        page.setRows(commentService.findUserCount(userId));
        model.addAttribute("user",user);
        List<Comment> userComments = commentService.findUserComments(userId, page.getOffset(), page.getLimit());
        List<Map<String,Object>> list = new ArrayList<>();
        for(Comment comment : userComments) {
            Map<String,Object> map = new HashMap<>();
            map.put("comment",comment);
            map.put("post",discussPostService.findDiscussPostById(comment.getEntityId()));
            list.add(map);
        }
        model.addAttribute("list",list);
        return "/site/my-reply";
    }

}
