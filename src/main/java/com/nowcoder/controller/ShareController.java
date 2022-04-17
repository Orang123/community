package com.nowcoder.controller;

import com.nowcoder.event.EventProducer;
import com.nowcoder.pojo.Event;
import com.nowcoder.util.CommunityConstant;
import com.nowcoder.util.CommunityUtil;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@Controller
public class ShareController implements CommunityConstant{

    private static final Logger logger = LoggerFactory.getLogger(UserController.class);

    @Autowired
    private EventProducer eventProducer;

    @Value("${community.path.domain}")
    private String domain;

    @Value("${server.servlet.context-path}")
    private String contextPath;

    @Value("${wk.image.storage}")
    private String wkImageStorage;

    @Value("${qiniu.bucket.share.url}")
    private String shareBucketUrl;

    //浏览器url里 请求参数为/share?htmlUrl=https://www.nowcoder.com/ 别忘了"htmlUrl="
    @RequestMapping(path = "/share",method = RequestMethod.GET)
    @ResponseBody
    public String share(String htmlUrl) {
        String fileName = CommunityUtil.generateUUID();
        String suffix = ".png";
        // 异步生成长图 将wkhtmltoimage指令 放到mq中,因为指令转化很耗时 为了降峰
        Event event = new Event()
                .setTopic(TOPIC_SHARE)
                .setEntityUserId(-1)//这里是为了保证entityUserId和userId不同,保证fireEvent 生产者能成功生产消息
                .setData("htmlUrl",htmlUrl)
                .setData("fileName",fileName)
                .setData("suffix",suffix);
        eventProducer.fireEvent(event);
        Map<String,Object> map = new HashMap<>();
        String url = shareBucketUrl + "/" + fileName;
        map.put(url,"url");
        return CommunityUtil.getJSONString(0,"分享成功", map);
    }

    // 获取长图 弃用
    @RequestMapping(path = "/share/image/{fileName}",method = RequestMethod.GET)
    public void getShareImage(@PathVariable("fileName")String fileName, HttpServletResponse response) {
        if (StringUtils.isBlank(fileName)) {
            throw new IllegalArgumentException("文件名不能为空!");
        }
        String suffix = fileName.substring(fileName.lastIndexOf(".")+1);
        fileName = wkImageStorage + "/" + fileName;
        response.setContentType("image/"+suffix);
        try(FileInputStream fis = new FileInputStream(fileName);
            ServletOutputStream os = response.getOutputStream();) {
            byte[] buffer = new byte[1024];
            int b = 0;
            while((b = fis.read(buffer)) != -1){
                os.write(buffer,0, b);
            }
        } catch (IOException e) {
            logger.error("读取长图失败: "+e.getMessage());
        }
    }

}
