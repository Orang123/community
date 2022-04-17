package com.nowcoder.service;

import com.nowcoder.util.RedisKeyUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisStringCommands;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

@Service
public class DataService {

    @Autowired
    private RedisTemplate redisTemplate;

    private SimpleDateFormat df = new SimpleDateFormat("yyyyMMdd");

    // 将指定的IP计入UV 将当天日期作为key,用户ip作为value
    public void recordUV(String ip) {
        String uvKey = RedisKeyUtil.getUVKey(df.format(new Date()));
        redisTemplate.opsForHyperLogLog().add(uvKey,ip);
    }

    // 统计指定日期范围内的UV
    public long calculateUV(Date start, Date end) {
        if(start == null || end == null) {
            throw new IllegalArgumentException("参数不能为空");
        }
        // 整理该日期范围内的key
        List<String> keyList = new ArrayList<>();
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(start);
        while(!calendar.getTime().after(end)) {
            String uvKey = RedisKeyUtil.getUVKey(df.format(calendar.getTime()));
            keyList.add(uvKey);
            calendar.add(Calendar.DATE,1);
        }
        // 合并这些数据
        String uvKey = RedisKeyUtil.getUVKey(df.format(start), df.format(end));
        redisTemplate.opsForHyperLogLog().union(uvKey,keyList.toArray());
        // 返回统计的结果
        return redisTemplate.opsForHyperLogLog().size(uvKey);
    }

    // 将指定用户计入DAU  将当天日期作为key,用户userId作为value
    public void recordDAU(int userId) {
        String dauKey = RedisKeyUtil.getDAUKey(df.format(new Date()));
        //将当前daukey bitmap中 bit位为userId的位设为1
        redisTemplate.opsForValue().setBit(dauKey, userId,true);
    }

    // 统计指定日期范围内的DAU
    public long calculateDAU(Date start, Date end) {
        if(start == null || end == null) {
            throw new IllegalArgumentException("参数不能为空");
        }
        // 整理该日期范围内的key
        List<byte[]> keyList = new ArrayList<>();
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(start);
        while(!calendar.getTime().after(end)) {//不是!calendar.after(end)
            String dauKey = RedisKeyUtil.getDAUKey(df.format(calendar.getTime()));
            keyList.add(dauKey.getBytes());
            calendar.add(Calendar.DATE,1);
        }
        // 进行OR运算 因为是bit位 不同天的dauKey 可能有相同用户userId 还是只计数一次,
        //考虑不同dauKey的 bit位不同尽可能多地统计 不重复的userId
        return (long)redisTemplate.execute(new RedisCallback() {
            @Override
            public Object doInRedis(RedisConnection redisConnection) throws DataAccessException {
                String dauKey = RedisKeyUtil.getDAUKey(df.format(start), df.format(end));
                redisConnection.bitOp(RedisStringCommands.BitOperation.OR,
                        dauKey.getBytes(),keyList.toArray(new byte[0][0]));
                return redisConnection.bitCount(dauKey.getBytes());
            }
        });
    }

}
