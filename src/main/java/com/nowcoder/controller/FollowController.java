package com.nowcoder.controller;

import com.nowcoder.event.EventProducer;
import com.nowcoder.pojo.Event;
import com.nowcoder.pojo.Page;
import com.nowcoder.pojo.User;
import com.nowcoder.service.FollowService;
import com.nowcoder.service.UserService;
import com.nowcoder.util.CommunityConstant;
import com.nowcoder.util.CommunityUtil;
import com.nowcoder.util.HostHolder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.List;
import java.util.Map;

@Controller
public class FollowController implements CommunityConstant {

    @Autowired
    private FollowService followService;

    @Autowired
    private HostHolder hostHolder;

    @Autowired
    private UserService userService;

    @Autowired
    private EventProducer eventProducer;

    @RequestMapping(path = "/follow", method = RequestMethod.POST)
    @ResponseBody
    public String follow(int entityType, int entityId) {
        User user = hostHolder.getUser();
        if(user == null)//其实前端的th:if标签已经处理过了,未登录不显示关注按钮,这里永不为空
            return CommunityUtil.getJSONString(1,"请先登录!");
        followService.follow(user.getId(), entityType, entityId);
        // 触发关注事件
        Event event = new Event()
                .setTopic(TOPIC_FOLLOW)
                .setUserId(hostHolder.getUser().getId())
                .setEntityType(entityType)
                .setEntityId(entityId)
                .setEntityUserId(entityId);
        eventProducer.fireEvent(event);
        return CommunityUtil.getJSONString(0,"关注成功!");
    }

    @RequestMapping(path = "/unfollow", method = RequestMethod.POST)
    @ResponseBody
    public String unfollow(int entityType, int entityId) {
        User user = hostHolder.getUser();
        if(user == null)
            return CommunityUtil.getJSONString(1,"请先登录!");
        followService.unfollow(user.getId(), entityType, entityId);
        return CommunityUtil.getJSONString(0,"取消关注成功!");
    }

    @RequestMapping(path = "/followees/{userId}",method = RequestMethod.GET)
    public String getFollowees(@PathVariable("userId")int userId, Page page, Model model) {
        model.addAttribute("user",userService.findUserById(userId));
        page.setLimit(5);
        page.setPath("/followees/"+userId);
        page.setRows(followService.findFolloweeCount(userId, ENTITY_TYPE_USER).intValue());
        List<Map<String, Object>> followees = followService.findFollowees(userId, page.getOffset(), page.getLimit());
        model.addAttribute("followees",followees);
        return "/site/followee";
    }

    @RequestMapping(path = "/followers/{userId}",method = RequestMethod.GET)
    public String getFollowers(@PathVariable("userId")int userId, Page page, Model model) {
        model.addAttribute("user",userService.findUserById(userId));
        page.setLimit(5);
        page.setPath("/followers/"+userId);
        page.setRows(followService.findFollowerCount(ENTITY_TYPE_USER, userId).intValue());
        List<Map<String, Object>> followers = followService.findFollowers(userId, page.getOffset(), page.getLimit());
        model.addAttribute("followers",followers);
        return "/site/follower";
    }

}
