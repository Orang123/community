package com.nowcoder.config;

import com.nowcoder.quartz.PostScoreRefreshJob;
import com.nowcoder.quartz.WKImageDeleteJob;
import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.quartz.JobDetailFactoryBean;
import org.springframework.scheduling.quartz.SimpleTriggerFactoryBean;

// 配置 -> 数据库 -> 调用
@Configuration
public class QuartzConifg {


    // FactoryBean可简化Bean的实例化过程:
    // 1.通过FactoryBean封装Bean的实例化过程.
    // 2.将FactoryBean装配到Spring容器里.
    // 3.将FactoryBean注入给其他的Bean.
    // 4.该Bean得到的是FactoryBean所管理的对象实例.

    // 配置JobDetail
    // 刷新帖子分数任务
    @Bean
    public JobDetailFactoryBean postScoreRefreshJobDetail() {
        JobDetailFactoryBean factoryBean = new JobDetailFactoryBean();
        factoryBean.setJobClass(PostScoreRefreshJob.class);
        factoryBean.setName("postScoreRefreshJob");
        factoryBean.setGroup("communityJobGroup");
        factoryBean.setDurability(true);//设置可持久
        factoryBean.setRequestsRecovery(true);//请求恢复
        return factoryBean;
    }

    // 配置Trigger(SimpleTriggerFactoryBean, CronTriggerFactoryBean)
    // 刷新帖子分数触发器
    //这里的postScoreRefreshJobDetail 必须要和Quartz包下的实现Job的类的命名驼峰相对应 否则编译不会通过,好像超过一个Bean 和 Trigger时就会报错
    //我是加上wkImageDeleteJobDetail就出现这个问题的
    @Bean
    public SimpleTriggerFactoryBean postScoreRefreshTrigger(JobDetail postScoreRefreshJobDetail) {
        SimpleTriggerFactoryBean factoryBean = new SimpleTriggerFactoryBean();
        factoryBean.setJobDetail(postScoreRefreshJobDetail);
        factoryBean.setName("postScoreRefreshTrigger");
        factoryBean.setGroup("communityTriggerGroup");
        //为了观察效果 每3分钟定时器 查询存放在redis中需要更新分数的帖子 同步到mysql和es中
        //实际项目 nowcoder是2个小时才会执行一次定时任务 更新redis中PostScoreKey set中记录的post的score
        //这里虽然设置了3分钟,但是实际数据库表qrtz_simple_triggers 中REPEAT_INTERVAL总是在启动springboot时初始化为300000 5分钟
        //暂时没搞懂这里如何配置 这个时间 好像这个时间只要第一次指定了,后面再改时间间隔就没用了 永远都是按照第一次配置的间隔时间计数的
        factoryBean.setRepeatInterval(1000*60*5);
        factoryBean.setJobDataMap(new JobDataMap());
        return factoryBean;
    }

    // 删除WK图片任务
    @Bean
    public JobDetailFactoryBean wkImageDeleteJobDetail() {
        JobDetailFactoryBean factoryBean = new JobDetailFactoryBean();
        factoryBean.setJobClass(WKImageDeleteJob.class);
        factoryBean.setName("wkImageDeleteJob");
        factoryBean.setGroup("communityJobGroup");
        factoryBean.setDurability(true);
        factoryBean.setRequestsRecovery(true);
        return factoryBean;
    }

    // 删除WK图片触发器
    @Bean
    public SimpleTriggerFactoryBean wkImageDeleteTrigger(JobDetail wkImageDeleteJobDetail) {
        SimpleTriggerFactoryBean factoryBean = new SimpleTriggerFactoryBean();
        factoryBean.setJobDetail(wkImageDeleteJobDetail);
        factoryBean.setName("wkImageDeleteTrigger");
        factoryBean.setGroup("communityTriggerGroup");
        factoryBean.setRepeatInterval(1000 * 60 * 4);//每4分钟触发一次 实际这个时间在这里设置是失效的
        factoryBean.setJobDataMap(new JobDataMap());
        return factoryBean;
    }

}
