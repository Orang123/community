package com.nowcoder.pojo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import java.util.Date;

@Data
@AllArgsConstructor
@NoArgsConstructor
//shards分片 Lucene索引，倒排索引    replicas 副本
//elasticsearch在后台把每个索引划分成多个分片，每分分片可以在集群中的不同服务器间迁移
@Document(indexName = "discusspost" ,type = "_doc", shards = 6, replicas = 3)
public class DiscussPost {

    //在es索引中对应的id编号 方便用来删除索引中的数据
    @Id
    private int id;

    @Field(type = FieldType.Integer)
    private int userId;

    //存储时采用ik_max_word为最细粒度划分,拆分出更多的分词  搜索时采用ik_smart为最少切分,拆词更少 搜索更精确
    @Field(type = FieldType.Text,analyzer = "ik_max_word" ,searchAnalyzer = "ik_smart")
    private String title;

    @Field(type = FieldType.Text,analyzer = "ik_max_word" ,searchAnalyzer = "ik_smart")
    private String content;

    @Field(type = FieldType.Integer)
    private int type;

    @Field(type = FieldType.Integer)
    private int status;

    @Field(type = FieldType.Date)
    private Date createTime;

    @Field(type = FieldType.Integer)
    private int commentCount;

    @Field(type = FieldType.Double)
    private double score;

}
