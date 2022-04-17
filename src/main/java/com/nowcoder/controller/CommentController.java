package com.nowcoder.controller;

import com.nowcoder.annotation.LoginRequired;
import com.nowcoder.event.EventProducer;
import com.nowcoder.pojo.Comment;
import com.nowcoder.pojo.DiscussPost;
import com.nowcoder.pojo.Event;
import com.nowcoder.pojo.User;
import com.nowcoder.service.CommentService;
import com.nowcoder.service.DiscussPostService;
import com.nowcoder.util.CommunityConstant;
import com.nowcoder.util.HostHolder;
import com.nowcoder.util.RedisKeyUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.Date;

@Controller
@RequestMapping("/comment")
public class CommentController implements CommunityConstant {

    @Autowired
    private CommentService commentService;

    @Autowired
    private HostHolder hostHolder;

    @Autowired
    private EventProducer eventProducer;

    @Autowired
    private DiscussPostService discussPostService;

    @Autowired
    private RedisTemplate redisTemplate;

    @LoginRequired
    @RequestMapping(path = "/add/{discussPostId}",method = RequestMethod.POST)
    public String addComment(@PathVariable("discussPostId") int discussPostId, Comment comment) {
        comment.setUserId(hostHolder.getUser().getId());
        comment.setStatus(0);
        comment.setCreateTime(new Date());
        commentService.addComment(comment);
        // 触发评论事件
        //这个消息中的内容 entityId 并没有真的用到,为后续业务做扩展 这个在es搜索时 消息封装时用到了
        Event event = new Event()
                .setTopic(TOPIC_COMMENT)
                .setUserId(hostHolder.getUser().getId())
                .setEntityType(comment.getEntityType())
                .setEntityId(comment.getEntityId())
                .setData("postId",discussPostId);
        if(comment.getEntityType() == ENTITY_TYPE_POST) {
            DiscussPost target = discussPostService.findDiscussPostById(comment.getEntityId());
            event.setEntityUserId(target.getUserId());
        } else if (comment.getEntityType() == ENTITY_TYPE_COMMENT) {
            Comment target = commentService.findCommentById(comment.getEntityId());
            event.setEntityUserId(target.getUserId());
        }
        eventProducer.fireEvent(event);
        // 触发发帖事件 帖子下面的回复数量改变,需要更新es中的数据
        if(comment.getEntityType() == ENTITY_TYPE_POST) {//发布对帖子评论时 对应帖子的commentCount属性会改变
            event =new Event()
                    .setTopic(TOPIC_PUBLISH)
                    .setUserId(comment.getUserId())
                    .setEntityType(ENTITY_TYPE_POST)
                    .setEntityId(discussPostId);
            eventProducer.fireEvent(event);
            //帖子回复数改变 延时更新帖子分数
            String postScoreKey = RedisKeyUtil.getPostScoreKey();
            redisTemplate.opsForSet().add(postScoreKey,discussPostId);
        }
        return "redirect:/discuss/detail/"+discussPostId;
    }

}
