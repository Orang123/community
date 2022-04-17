package com.nowcoder.service;

import com.nowcoder.pojo.User;
import com.nowcoder.util.CommunityConstant;
import com.nowcoder.util.HostHolder;
import com.nowcoder.util.RedisKeyUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.RedisOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.SessionCallback;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class FollowService implements CommunityConstant {

    @Autowired
    private RedisTemplate redisTemplate;

    @Autowired
    private UserService userService;

    @Autowired
    private HostHolder hostHolder;

    //关注
    public void follow(int userId, int entityType, int entityId) {
        String followeeKey = RedisKeyUtil.getFolloweeKey(userId, entityType);
        String followerKey = RedisKeyUtil.getFollowerKey(entityType, entityId);
        redisTemplate.execute(new SessionCallback() {
            @Override
            public Object execute(RedisOperations redisOperations) throws DataAccessException {
                redisOperations.multi();
                redisOperations.opsForZSet().add(followeeKey,entityId,System.currentTimeMillis());
                redisOperations.opsForZSet().add(followerKey,userId,System.currentTimeMillis());
                return redisOperations.exec();
            }
        });
    }

    //取消关注
    public void unfollow(int userId, int entityType, int entityId) {
        String followeeKey = RedisKeyUtil.getFolloweeKey(userId, entityType);
        String followerKey = RedisKeyUtil.getFollowerKey(entityType, entityId);
        redisTemplate.execute(new SessionCallback() {
            @Override
            public Object execute(RedisOperations redisOperations) throws DataAccessException {
                redisOperations.multi();
                redisOperations.opsForZSet().remove(followeeKey,entityId);
                redisOperations.opsForZSet().remove(followerKey,userId);
                return redisOperations.exec();
            }
        });
    }

    // 查询关注的实体的数量
    public Long findFolloweeCount(int userId, int entityType) {
        String followeeKey = RedisKeyUtil.getFolloweeKey(userId,entityType);
        return redisTemplate.opsForZSet().zCard(followeeKey);
    }

    // 查询实体的粉丝的数量
    public Long findFollowerCount(int entityType, int entityId) {
        String followerKey = RedisKeyUtil.getFollowerKey(entityType,entityId);
        return redisTemplate.opsForZSet().zCard(followerKey);
    }

    // 查询当前用户是否已关注该实体
    public int hasFollowed(int userId, int entityType, int entityId) {
        String followeeKey = RedisKeyUtil.getFolloweeKey(userId, entityType);
        return redisTemplate.opsForZSet().score(followeeKey,entityId) != null ? 1 : 0;
    }

    public List<Map<String,Object>> findFollowees(int userId, int offset, int limit) {
        String followeeKey = RedisKeyUtil.getFolloweeKey(userId, ENTITY_TYPE_USER);
        Set<Integer> ids = redisTemplate.opsForZSet().reverseRange(followeeKey, offset, offset + limit - 1);
        List<Map<String,Object>> list = new ArrayList<>();
        for(Integer id : ids) {
            Map<String,Object> map = new HashMap<>();
            map.put("user",userService.findUserById(id));
            Double score = redisTemplate.opsForZSet().score(followeeKey, id);
            map.put("followeeTime",new Date(score.longValue()));
            int hasFollowed = 0;
            if(hostHolder.getUser() != null)
                hasFollowed = hasFollowed(hostHolder.getUser().getId(), ENTITY_TYPE_USER, id);
            map.put("hasFollowed",hasFollowed);
            list.add(map);
        }
        return list;
    }

    public List<Map<String,Object>> findFollowers(int userId, int offset, int limit) {
        String followerKey = RedisKeyUtil.getFollowerKey(ENTITY_TYPE_USER, userId);
        Set<Integer> ids = redisTemplate.opsForZSet().reverseRange(followerKey, offset, offset + limit - 1);
        List<Map<String,Object>> list = new ArrayList<>();
        for(Integer id : ids) {
            Map<String,Object> map = new HashMap<>();
            map.put("user",userService.findUserById(id));
            Double score = redisTemplate.opsForZSet().score(followerKey, id);
            map.put("followerTime",new Date(score.longValue()));
            int hasFollowed = 0;
            if(hostHolder.getUser() != null)
                hasFollowed = hasFollowed(hostHolder.getUser().getId(), ENTITY_TYPE_USER, id);
            map.put("hasFollowed",hasFollowed);
            list.add(map);
        }
        return list;
    }

}
