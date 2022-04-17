package com.nowcoder.quartz;

import com.nowcoder.event.EventProducer;
import com.nowcoder.pojo.DiscussPost;
import com.nowcoder.pojo.Event;
import com.nowcoder.service.DiscussPostService;
import com.nowcoder.service.LikeService;
import com.nowcoder.util.CommunityConstant;
import com.nowcoder.util.RedisKeyUtil;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.BoundSetOperations;
import org.springframework.data.redis.core.RedisTemplate;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class PostScoreRefreshJob implements Job, CommunityConstant {

    private static final Logger logger = LoggerFactory.getLogger(PostScoreRefreshJob.class);

    @Autowired
    private DiscussPostService discussPostService;

    @Autowired
    private LikeService likeService;

    @Autowired
    private RedisTemplate redisTemplate;

    @Autowired
    private EventProducer eventProducer;

    //牛客纪元
    private static final Date epoch;

    static {
        try{
            epoch = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").parse("2014-08-01 00:00:00");
        } catch (ParseException e) {
            throw new RuntimeException("初始化牛客纪元失败!",e);
        }
    }

    @Override
    public void execute(JobExecutionContext jobExecutionContext) throws JobExecutionException {
        String postScoreKey = RedisKeyUtil.getPostScoreKey();
        //获取opsForSet()的操作符,可以直接用operations再操作key为postScoreKey的set
        BoundSetOperations operations = redisTemplate.boundSetOps(postScoreKey);
        if(operations.size() == 0) {
            logger.info("[任务取消] 没有需要刷新的帖子!");
            return;
        }
        logger.info("[任务开始] 正在刷新帖子分数: "+ operations.size());
        while(operations.size() > 0){
            refresh((int)operations.pop());//所有的postId 都pop完后 operations.size()会为0,redis中的postScoreKey key会自动销毁
        }
        logger.info("[任务结束] 帖子分数刷新完毕!");
    }

    private void refresh(int postId) {
        DiscussPost post = discussPostService.findDiscussPostById(postId);
        if (post == null) {
            logger.error("该帖子不存在: id = " + postId);
            return;
        }
        // 是否精华
        boolean wonderful = post.getStatus() == 1;
        // 评论数量
        int communtCount = post.getCommentCount();
        // 点赞数量
        long likeCount = likeService.findEntityLikeCount(ENTITY_TYPE_POST,postId);
        // 计算权重
        double w = (wonderful ? 75 : 0) + communtCount * 10 + likeCount * 2;
        //帖子的分数为权重(w)+(发布时间-牛客纪元)(距离天数)
        // (post.getCreateTime().getTime() - epoch.getTime())为毫秒,3600*24为一天的秒数,乘1000将秒变为毫秒
        //通过log对数运算 只有当点赞帖子数遥遥领先时 score才会明显上升
        double score = Math.log10(Math.max(w, 1))//这里权重不能为负,最少为0,所以Math.max(w, 1),10^0=1
                + (post.getCreateTime().getTime() - epoch.getTime()) / (1000 * 3600 * 24);
        // 更新帖子分数
        discussPostService.updateScore(postId,score);
        //同步搜索数据
        //kafka 更新es中的帖子的权重方便查询,这里之所以用消息队列 是因为 定时器后期可能是多线程 有很多线程同时更新帖子分数,
        //同一时刻会高频访问mysql,为了降峰,缓解访问mysql数据库的压力,采用mq 延时更新
        Event event =new Event()
                .setTopic(TOPIC_PUBLISH)
                .setEntityType(ENTITY_TYPE_POST)
                .setEntityId(postId)
                //这里event之所以设置 EntityUserId,是因为生产者在发送消息时,会判断event.getUserId() != event.getEntityUserId()时才会触发生产者
                //要使得UserId() EntityUserId()不同,不设置userId默认为0,而本身没有id为0的用户
                // 否则不设置EntityUserId,UserId() EntityUserId()值是一样的,更新分数后的 post事件无法从生产者发出
                .setEntityUserId(post.getUserId());
        eventProducer.fireEvent(event);
    }
}
