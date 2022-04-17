package com.nowcoder.quartz;

import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;

import java.io.File;

public class WKImageDeleteJob implements Job {

    private static final Logger logger = LoggerFactory.getLogger(WKImageDeleteJob.class);

    @Value("${wk.image.storage}")
    private String wkImageStorage;

    @Override
    public void execute(JobExecutionContext jobExecutionContext) throws JobExecutionException {
        File[] files = new File(wkImageStorage).listFiles();
        if (files == null || files.length == 0) {
            logger.info("没有WK图片，任务取消！");
            return;
        }

        for (File file : files) {
            // 删除一分钟之前创建的图片
            if (System.currentTimeMillis() - file.lastModified() > 60 * 1000) {//这里时间差值是毫秒 所以乘60*1000
                logger.info("删除WK图片：" + file.getName());
                file.delete();
            }
        }
    }
}
