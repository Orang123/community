package com.nowcoder.controller;

import com.nowcoder.pojo.DiscussPost;
import com.nowcoder.pojo.Page;
import com.nowcoder.service.ElasticsearchService;
import com.nowcoder.service.LikeService;
import com.nowcoder.service.UserService;
import com.nowcoder.util.CommunityConstant;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
public class SearchController implements CommunityConstant {

    @Autowired
    private ElasticsearchService elasticsearchService;

    @Autowired
    private UserService userService;

    @Autowired
    private LikeService likeService;

    @RequestMapping(path = "/search",method = RequestMethod.GET)
    public String search(String keyword, Page page, Model model) {
        org.springframework.data.domain.Page<DiscussPost> discussPosts =
                elasticsearchService.searchDiscussPost(keyword, page.getCurrent() - 1, page.getLimit());
        List<Map<String,Object>> list = new ArrayList<>();
        if(discussPosts != null) {
            for(DiscussPost post : discussPosts) {
                Map<String,Object> map= new HashMap<>();
                map.put("post",post);
                map.put("user",userService.findUserById(post.getUserId()));
                map.put("likeCount",likeService.findEntityLikeCount(ENTITY_TYPE_POST, post.getId()));
                list.add(map);
            }
        }
        model.addAttribute("list",list);
        page.setRows(discussPosts == null ? 0 : (int)discussPosts.getTotalElements());
        page.setPath("/search?keyword="+keyword);
        return "/site/search";
    }

}
