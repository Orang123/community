package com.nowcoder.dao.elasticsearch;

import com.nowcoder.pojo.DiscussPost;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

@Repository
//ElasticsearchRepository<DiscussPost, Integer> es中存的是DiscussPost,实体类的主键是Integer类型,id 可用于删除es索引中对应的id数据行
public interface DiscussPostRepository extends ElasticsearchRepository<DiscussPost, Integer> {

}
