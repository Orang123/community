package com.nowcoder;

import com.nowcoder.dao.DiscussPostMapper;
import com.nowcoder.dao.MessageMapper;
import com.nowcoder.dao.UserMapper;
import com.nowcoder.util.SensitiveFilter;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.test.context.junit4.SpringRunner;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

@RunWith(SpringRunner.class)
@SpringBootTest
public class CommunityApplicationTests {//junit 4中的类的生命和方法 访问修饰符都得是public的

    @Autowired
    private DiscussPostMapper discussPostMapper;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private SensitiveFilter sensitiveFilter;

    @Autowired
    private MessageMapper messageMapper;

    @Autowired
    private RedisTemplate redisTemplate;

    private SimpleDateFormat df = new SimpleDateFormat("yyyyMMdd");

    @Test
    public void test1() {
        System.out.println("数量:"+messageMapper.selectConversationCount(149));
    }

    @Test
    public void test2() {
        String key = "test:count";
        redisTemplate.opsForValue().set(key,"gtggre");
        System.out.println(redisTemplate.opsForValue().get(key));
    }

    @Test
    public void test3() {
        System.out.println(new Date());
        System.out.println(df.format(new Date()));
    }

    @Test
    public void testSensitiveFilter() {
        String text = "这里可以赌博,可以嫖娼,可以吸毒,可以开票,哈哈哈!";
        text = sensitiveFilter.filter(text);
        System.out.println(text);

        text = "这里可以☆赌☆博☆,可以☆嫖☆娼☆,可以☆吸☆毒☆,可以☆开☆票☆,哈哈哈!";
        text = sensitiveFilter.filter(text);
        System.out.println(text);
    }

    @Test
    public void contextLoads() throws IOException {
//        System.out.println(discussPostMapper.selectDiscussPosts(0,0,5));
//        System.out.println(discussPostMapper.selectDiscussPostsRows(0));
//        System.out.println(userMapper.selectById(0));
        Runtime.getRuntime().exec("D:/wkhtmltopdf/bin/wkhtmltoimage --quality 75 https://www.nowcoder.com/ E:/JetBrains/data/nowcoder/community/wk-images/4.png");
    }

}
