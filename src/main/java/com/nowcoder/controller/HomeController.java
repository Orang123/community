package com.nowcoder.controller;

import com.nowcoder.pojo.DiscussPost;
import com.nowcoder.pojo.Page;
import com.nowcoder.service.DiscussPostService;
import com.nowcoder.service.LikeService;
import com.nowcoder.service.UserService;
import com.nowcoder.util.CommunityConstant;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
public class HomeController implements CommunityConstant {

    @Autowired
    private DiscussPostService discussPostService;

    @Autowired
    private UserService userService;

    @Autowired
    private LikeService likeService;

    //Page 会自动add到model里,只有String int,double,boolean这种基本类型不可以,别的复合对象类型都是可以的 这些需要${param.name}来访问
    @RequestMapping(path = {"/","/index"},method = RequestMethod.GET)
    public String getIndexPage(Model model, Page page,
                               @RequestParam(name = "orderMode",defaultValue = "0")int orderMode){
        page.setRows(discussPostService.findDiscussPostsRows(0));
        page.setPath("/index?orderMode="+orderMode);
        List<DiscussPost> list=discussPostService.findDiscussPosts(0,page.getOffset(),page.getLimit(), orderMode);
        List<Map<String,Object>> discussPosts=new ArrayList<>();
        if(list != null){
            for(DiscussPost post : list){
                Map<String,Object> map=new HashMap<>();
                map.put("post",post);
                map.put("user",userService.findUserById(post.getUserId()));
                map.put("likeCount",likeService.findEntityLikeCount(ENTITY_TYPE_POST,post.getId()));
                discussPosts.add(map);
            }
        }
        //这种?orderMode= @RequestParam的get请求 是不能自动包含在springmvc的model里的 另一个原因也是int类型
        model.addAttribute("orderMode",orderMode);
        model.addAttribute("discussPosts",discussPosts);
        return "/index";
    }

    @RequestMapping(path = "/error", method = RequestMethod.GET)
    public String getErrorPage() {
        return "/error/500";
    }

    @RequestMapping(path = "/denied",method = RequestMethod.GET)
    public String getDeniedPage() {
        return "/error/404";
    }

}
