package com.nowcoder.event;

import com.alibaba.fastjson.JSONObject;
import com.nowcoder.pojo.Event;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
public class EventProducer {

    @Autowired
    private KafkaTemplate kafkaTemplate;

    // 处理事件
    public void fireEvent(Event event) {
        //只有当操作用户和被通知用户不同时,才加入消息队列, 自己操作自己的事件并不做通知
        if(event.getUserId() != event.getEntityUserId()) {
            // 将事件发布到指定的主题 数据传输采用json
            kafkaTemplate.send(event.getTopic(), JSONObject.toJSONString(event));
        }
    }

}
