package com.nowcoder.dao;

import com.nowcoder.pojo.Comment;
import org.apache.ibatis.annotations.Mapper;
import org.springframework.stereotype.Repository;

import java.util.List;

@Mapper
@Repository
public interface CommentMapper {

    List<Comment> selectCommentsByEntity(int entityType, int entityId, int offset, int limit);

    int selectCountByEntity(int entityType, int entityId);

    int insertComment(Comment comment);

    //这里评论只统计针对 帖子的评论,并且status不为1,实际就是0吧,可能后面还有别的有效状态,并且子查询帖子必须存在 status不为2,可能后面管理员会拉黑某个帖子
    List<Comment> selectCommentsByUser(int userId, int offset, int limit);

    int selectCountByUser(int userId);

    Comment selectCommentById(int id);

}
