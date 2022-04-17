package com.nowcoder.controller;

import com.nowcoder.annotation.LoginRequired;
import com.nowcoder.event.EventProducer;
import com.nowcoder.pojo.Event;
import com.nowcoder.service.LikeService;
import com.nowcoder.util.CommunityConstant;
import com.nowcoder.util.CommunityUtil;
import com.nowcoder.util.HostHolder;
import com.nowcoder.util.RedisKeyUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.HashMap;
import java.util.Map;

@Controller
public class LikeController implements CommunityConstant {

    @Autowired
    private LikeService likeService;

    @Autowired
    private HostHolder hostHolder;

    @Autowired
    private EventProducer eventProducer;

    @Autowired
    private RedisTemplate redisTemplate;

//    @LoginRequired//这里提示信息放在ajax alert里
    @RequestMapping(path = "/like",method = RequestMethod.POST)
    @ResponseBody
    public String like(int entityType, int entityId, int entityUserId, int postId) {
        if(hostHolder.getUser() == null)
            return CommunityUtil.getJSONString(1,"登陆后才能点赞!");
        likeService.like(hostHolder.getUser().getId(), entityType, entityId, entityUserId);
        Map<String,Object> map = new HashMap<>();
        map.put("likeCount",likeService.findEntityLikeCount(entityType, entityId));
        int likeStatus = likeService.findEntityLikeStatus(hostHolder.getUser().getId(), entityType, entityId);
        map.put("likeStatus",likeStatus);
        // 触发点赞事件
        if(likeStatus == 1) {
            Event event = new Event()
                    .setTopic(TOPIC_LIKE)
                    .setUserId(hostHolder.getUser().getId())
                    .setEntityType(entityType)
                    .setEntityId(entityId)
                    .setEntityUserId(entityUserId)
                    .setData("postId",postId);
            eventProducer.fireEvent(event);
            //对帖子点赞时 帖子点赞数改变 延时更新帖子分数
            if(entityType == ENTITY_TYPE_POST) {
                String postScoreKey = RedisKeyUtil.getPostScoreKey();
                redisTemplate.opsForSet().add(postScoreKey,postId);
            }
        }
        return CommunityUtil.getJSONString(0,"",map);
    }

}
