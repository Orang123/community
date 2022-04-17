package com.nowcoder.event;

import com.alibaba.fastjson.JSONObject;
import com.nowcoder.pojo.DiscussPost;
import com.nowcoder.pojo.Event;
import com.nowcoder.pojo.Message;
import com.nowcoder.service.DiscussPostService;
import com.nowcoder.service.ElasticsearchService;
import com.nowcoder.service.MessageService;
import com.nowcoder.util.CommunityConstant;
import com.nowcoder.util.CommunityUtil;
import com.qiniu.common.QiniuException;
import com.qiniu.common.Zone;
import com.qiniu.http.Response;
import com.qiniu.storage.Configuration;
import com.qiniu.storage.UploadManager;
import com.qiniu.util.Auth;
import com.qiniu.util.StringMap;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Future;

@Component
public class EventConsumer implements CommunityConstant {

    private static final Logger logger = LoggerFactory.getLogger(EventConsumer.class);

    @Autowired
    private MessageService messageService;

    @Autowired
    private DiscussPostService discussPostService;

    @Autowired
    private ElasticsearchService elasticsearchService;

    @Value("${wk.image.command}")
    private String wkImageCommand;

    @Value("${wk.image.storage}")
    private String wkImageStorage;

    @Value("${qiniu.key.access}")
    private String accesskey;

    @Value("${qiniu.key.secret}")
    private String secretKey;

    @Value("${qiniu.bucket.share.name}")
    private String shareBucketName;

    @Autowired//spring 线程池定时任务
    private ThreadPoolTaskScheduler taskScheduler;

    //kafka消息队列监听的消息类型是 评论、点赞、关注
    @KafkaListener(topics = {TOPIC_COMMENT,TOPIC_LIKE,TOPIC_FOLLOW})
    public void handleCommentMessage(ConsumerRecord record) {
        if(record == null || record.value() == null) {
            logger.error("消息的内容为空!");
            return;
        }
        Event event = JSONObject.parseObject(record.value().toString(), Event.class);
        if(event == null) {
            logger.error("消息格式错误!");
            return;
        }
        //将kakfa缓存的消息队列 依次转换为Message实体 存入数据库dao层
        Message message = new Message();
        message.setFromId(SYSTEM_USER_ID);
        message.setToId(event.getEntityUserId());
        message.setConversationId(event.getTopic());
        message.setStatus(0);
        message.setCreateTime(new Date());

        Map<String, Object> content = new HashMap<>();
        content.put("userId",event.getUserId());
        content.put("entityType",event.getEntityType());
        content.put("entityId",event.getEntityId());//这个消息中的内容 entityId 并没有真的用到,为后续业务做扩展
        //发布对帖子或评论的 回复 message内容需要附带帖子id,postId
        if(!event.getData().isEmpty()){
            for(Map.Entry<String,Object> entry : event.getData().entrySet()) {
                content.put(entry.getKey(), entry.getValue());
            }
        }
        message.setContent(JSONObject.toJSONString(content));
        messageService.addMessage(message);
    }

    //监听发布帖子、发布评论(帖子的评论数量会改变)时 在es中插入新的帖子,或覆盖之前的帖子
    @KafkaListener(topics = {TOPIC_PUBLISH})
    public void handlePublishMessage(ConsumerRecord record) {
        if(record == null || record.value() == null) {
            logger.error("消息的内容为空!");
            return;
        }
        Event event = JSONObject.parseObject(record.value().toString(), Event.class);
        if(event == null) {
            logger.error("消息格式错误!");
            return;
        }
        DiscussPost post = discussPostService.findDiscussPostById(event.getEntityId());
        elasticsearchService.saveDiscussPost(post);
    }

    @KafkaListener(topics = {TOPIC_DELETE})
    public void handleDeleteMessage(ConsumerRecord record) {
        if(record == null || record.value() == null) {
            logger.error("消息的内容为空!");
            return;
        }
        Event event = JSONObject.parseObject(record.value().toString(), Event.class);
        if(event == null) {
            logger.error("消息格式错误!");
            return;
        }
        elasticsearchService.deleteDiscussPost(event.getEntityId());
    }

    // 消费分享事件
    @KafkaListener(topics = {TOPIC_SHARE})
    public void handleShareMessage(ConsumerRecord record) {
        if(record == null || record.value() == null) {
            logger.error("消息的内容为空!");
            return;
        }
        Event event = JSONObject.parseObject(record.value().toString(), Event.class);
        if(event == null) {
            logger.error("消息格式错误!");
            return;
        }
        String htmlUrl = (String)event.getData().get("htmlUrl");
        String fileName = (String)event.getData().get("fileName");
        String suffix = (String)event.getData().get("suffix");
        // wkImageCommand和,上传图片至七牛云可能一次不会成功 启用定时器,监视该图片,一旦生成了,则上传至七牛云.
        UploadTask uploadTask = new UploadTask(htmlUrl ,fileName, suffix);
        Future future = taskScheduler.scheduleAtFixedRate(uploadTask, 500);//每500毫秒尝试上传一次
        uploadTask.setFuture(future);//对uploadTask设置定时任务 future
    }

    class UploadTask implements Runnable {

        //生成长图网页url
        private String htmlUrl;
        // 文件名称
        private String fileName;
        // 文件后缀
        private String suffix;
        // 启动任务的返回值
        private Future future;
        // 开始时间
        private long startTime;
        // 上传次数
        private int uploadTimes;

        public UploadTask(String htmlUrl, String fileName, String suffix) {
            this.htmlUrl = htmlUrl;
            this.fileName = fileName;
            this.suffix = suffix;
            this.startTime = System.currentTimeMillis();
        }

        public void setFuture(Future future) {
            this.future = future;
        }

        @Override
        public void run() {
            // 生成失败 尝试时间超过30s就停止 不会一直上传
            if(System.currentTimeMillis() - startTime > 30000) {
                logger.error("执行时间过长,终止任务:" + fileName);
                future.cancel(true);
                return;
            }
            // 上传失败 上传次数超过10次 就停止
            if(uploadTimes > 10) {
                logger.error("上传次数过多,终止任务:" + fileName);
                future.cancel(true);
                return;
            }
            String path = wkImageStorage + "/" + fileName + suffix;
            File file = new File(path);
            if(file.exists()) {
                logger.info(String.format("开始第%d次上传[%s]",++uploadTimes,fileName));//每次尝试 uploadTimes加1
                // 设置响应信息
                StringMap policy = new StringMap();
                policy.put("returnBody", CommunityUtil.getJSONString(0));
                // 生成上传凭证
                Auth auth = Auth.create(accesskey, secretKey);
                String uploadToken = auth.uploadToken(shareBucketName, fileName, 3600, policy);
                // 指定上传机房 这里是上传至华东机房 zoneo
                UploadManager manager = new UploadManager((new Configuration(Zone.zone0())));
                try{
                    // 开始上传图片
                    //这里这个"image/"+suffix suffix应该是不包含"."的,而suffix第一位是'.' 要处理一下 否则七牛云服务器上无法查看图片
                    Response reponse = manager.put(
                            path, fileName, uploadToken, null, "image/" + suffix.substring(suffix.lastIndexOf(".")+1), false);
                    // 处理响应结果
                    JSONObject json = JSONObject.parseObject(reponse.bodyString());
                    //返回为空 或得到的返回状态不为code=0 都是失败
                    if(json == null || json.get("code") == null || !json.get("code").toString().equals("0")) {
                        logger.info(String.format("第%d次上传失败[%s].", uploadTimes, fileName));
                    } else {
                        logger.info(String.format("第%d次上传成功[%s].", uploadTimes, fileName));
                        future.cancel(true);
                    }
                } catch (QiniuException e) {
                    logger.info(String.format("第%d次上传失败[%s].", uploadTimes, fileName));
                }
            } else {
                logger.info("等待图片生成[" + fileName + "].");
                //这里命令之间注意空格 定时器
                String command = wkImageCommand + " --quality 75 " + htmlUrl + " " + wkImageStorage + "/" + fileName + suffix;
                try {
                    //部分网页 不支持生成长图 youtube https://pastebin.ubuntu.com/  https://www.kuangstudy.com/ ...
                    Runtime.getRuntime().exec(command);
                    logger.info("生成长图成功: " + command);
                } catch (IOException e) {
                    logger.error("生成长图失败: " + e.getMessage());
                }
            }
        }
    }

}
