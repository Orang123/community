package com.nowcoder.controller;

import com.alibaba.fastjson.JSONObject;
import com.nowcoder.annotation.LoginRequired;
import com.nowcoder.dao.MessageMapper;
import com.nowcoder.pojo.Message;
import com.nowcoder.pojo.Page;
import com.nowcoder.pojo.User;
import com.nowcoder.service.MessageService;
import com.nowcoder.service.UserService;
import com.nowcoder.util.CommunityConstant;
import com.nowcoder.util.CommunityUtil;
import com.nowcoder.util.HostHolder;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.util.HtmlUtils;

import java.util.*;

@Controller
public class MessageController implements CommunityConstant {

    @Autowired
    HostHolder hostHolder;

    @Autowired
    private MessageService messageService;

    @Autowired
    private UserService userService;

    @LoginRequired
    @RequestMapping(path = "/letter/list",method = RequestMethod.GET)
    public String getLetterList(Model model, Page page) {
        User user = hostHolder.getUser();
        page.setLimit(5);
        page.setPath("/letter/list");
        page.setRows(messageService.findConversationCount(user.getId()));
        List<Message> conversationList = messageService.findConversations(user.getId(), page.getOffset(), page.getLimit());
        List<Map<String,Object>> conversations = new ArrayList<>();
        if(conversationList != null) {
            for(Message message : conversationList) {
                Map<String, Object> map = new HashMap<>();
                map.put("conversation",message);
                map.put("letterCount", messageService.findLetterCount(message.getConversationId()));
                map.put("unreadCount",messageService.findLetterUnreadCount(user.getId(),message.getConversationId()));
                int targetId = message.getFromId() != user.getId() ? message.getFromId() : message.getToId();
                map.put("target",userService.findUserById(targetId));
                conversations.add(map);
            }
        }
        model.addAttribute("conversations",conversations);
        model.addAttribute("letterUnreadCount",messageService.findLetterUnreadCount(user.getId(),null));
        model.addAttribute("noticeUnreadCount",messageService.findNoticeUnreadCount(user.getId(),null));
        return "/site/letter";
    }

    @LoginRequired
    @RequestMapping(path = "/letter/detail/{conversationId}",method = RequestMethod.GET)
    public String getLetterDetail(@PathVariable("conversationId")String conversationId, Model model, Page page) {
        page.setLimit(5);
        page.setPath("/letter/detail/"+conversationId);
        page.setRows(messageService.findLetterCount(conversationId));
        List<Message> letterList = messageService.findLetters(conversationId, page.getOffset(), page.getLimit());
        List<Map<String,Object>> letters = new ArrayList<>();
        if(letterList != null) {
            for(Message message : letterList) {
                Map<String,Object> map = new HashMap<>();
                map.put("letter",message);
                int targetId = message.getFromId();
                map.put("fromUser",userService.findUserById(targetId));
                letters.add(map);
            }
        }
        model.addAttribute("letters",letters);
        model.addAttribute("target",getLetterTarget(conversationId));
        List<Integer> ids = getUnreadLetters(letterList);
        if(!ids.isEmpty())
            messageService.readMessage(ids);
        return "/site/letter-detail";
    }

    private List<Integer> getUnreadLetters(List<Message> letterList) {
        List<Integer> ids = new ArrayList<>();
        if(ids != null){
            for(Message message : letterList) {
                if(message.getToId() == hostHolder.getUser().getId() && message.getStatus() == 0)
                    ids.add(message.getId());
            }
        }
        return ids;
    }

    private User getLetterTarget(String conversationId) {
        String[] ids = conversationId.split("_");
        int id0 = Integer.parseInt(ids[0]);
        int id1 = Integer.parseInt(ids[1]);
        if(hostHolder.getUser().getId() == id0) {
            return userService.findUserById(id1);
        }
        else {
            return userService.findUserById(id0);
        }
    }

    @LoginRequired
    @RequestMapping(path = "/letter/send",method = RequestMethod.POST)
    @ResponseBody
    public String sendLetter(String toName, String content) {
        User target = userService.findUserByName(toName);
        if(target == null)
            return CommunityUtil.getJSONString(1,"该目标用户不存在!");
        if(StringUtils.isBlank(content))
            return CommunityUtil.getJSONString(1,"发送内容不能为空!");
        Message message = new Message();
        message.setFromId(hostHolder.getUser().getId());
        message.setToId(target.getId());
        if(message.getFromId() < message.getToId())
            message.setConversationId(message.getFromId()+"_"+message.getToId());
        else
            message.setConversationId(message.getToId()+"_"+message.getFromId());
        message.setContent(content);
        message.setStatus(0);
        message.setCreateTime(new Date());
        messageService.addMessage(message);
        return CommunityUtil.getJSONString(0,"发送成功！");
    }

    @LoginRequired
    @RequestMapping(path = "/letter/delete", method = RequestMethod.POST)
    @ResponseBody
    public String deleteLetter(int id) {
        messageService.deleteMessage(id);
        return CommunityUtil.getJSONString(0,"已删除一条私信!");
    }

    @LoginRequired
    @RequestMapping(path = "/notice/list",method = RequestMethod.GET)
    public String getNoticeList(Model model) {
        int userId = hostHolder.getUser().getId();
        Message message = messageService.findLatestNotice(userId, TOPIC_COMMENT);
        Map<String,Object> map =new HashMap<>();
        if(message != null) {
            map.put("message",message);
            String content = HtmlUtils.htmlUnescape(message.getContent());
            Map<String,Object> data = JSONObject.parseObject(content, HashMap.class);
            map.put("user",userService.findUserById((Integer)data.get("userId")));
            map.put("entityType",data.get("entityType"));
            map.put("entityId",data.get("entityId"));
            map.put("postId",data.get("postId"));
            map.put("count",messageService.findNoticeCount(userId,TOPIC_COMMENT));
            map.put("unreadCount",messageService.findNoticeUnreadCount(userId,TOPIC_COMMENT));
            //这里要放在 if内,notice.html ,放在if外 th:if="${commentNotice.message!=null}" 如果message本身为null,就会获取不到 会报错
            // Property or field 'message' cannot be found on object of type 'java.util.HashMap' - maybe not public or not valid?
            model.addAttribute("commentNotice",map);
        }

        message = messageService.findLatestNotice(userId, TOPIC_LIKE);
        map = new HashMap<>();
        if(message != null) {
            map.put("message",message);
            String content = HtmlUtils.htmlUnescape(message.getContent());
            Map<String,Object> data = JSONObject.parseObject(content, HashMap.class);
            map.put("user",userService.findUserById((Integer)data.get("userId")));
            map.put("entityType",data.get("entityType"));
            map.put("entityId",data.get("entityId"));
            map.put("postId",data.get("postId"));
            map.put("count",messageService.findNoticeCount(userId,TOPIC_LIKE));
            map.put("unreadCount",messageService.findNoticeUnreadCount(userId,TOPIC_LIKE));
            model.addAttribute("likeNotice",map);
        }

        message  = messageService.findLatestNotice(userId, TOPIC_FOLLOW);
        map = new HashMap<>();
        if(message != null) {
            map.put("message",message);
            String content = HtmlUtils.htmlUnescape(message.getContent());
            Map<String,Object> data = JSONObject.parseObject(content, HashMap.class);
            map.put("user",userService.findUserById((Integer)data.get("userId")));
            map.put("entityType",data.get("entityType"));
            map.put("entityId",data.get("entityId"));
            map.put("count",messageService.findNoticeCount(userId,TOPIC_FOLLOW));
            map.put("unreadCount",messageService.findNoticeUnreadCount(userId,TOPIC_FOLLOW));
            model.addAttribute("followNotice",map);
        }
        model.addAttribute("letterUnreadCount",messageService.findLetterUnreadCount(userId,null));
        model.addAttribute("noticeUnreadCount",messageService.findNoticeUnreadCount(userId,null));
        return "/site/notice";
    }

    @LoginRequired
    @RequestMapping(path = "/notice/detail/{topic}",method = RequestMethod.GET)
    public String getNoticeDetail(@PathVariable("topic")String topic, Page page, Model model) {
        int userId = hostHolder.getUser().getId();
        page.setLimit(5);
        page.setPath("/notice/detail/"+topic);
        page.setRows(messageService.findNoticeCount(userId,topic));
        List<Message> list = messageService.findNotices(userId, topic, page.getOffset(), page.getLimit());
        List<Map<String,Object>> noticeLists = new ArrayList<>();
        for(Message message : list) {
            Map<String,Object> map = new HashMap<>();
            map.put("message",message);
            String content = HtmlUtils.htmlUnescape(message.getContent());
            HashMap<String,Object> data = JSONObject.parseObject(content, HashMap.class);
            map.put("user",userService.findUserById((Integer) data.get("userId")));
            map.put("entityType",data.get("entityType"));
            map.put("entityId",data.get("entityId"));//这个entityId暂时没有用到
            map.put("postId",data.get("postId"));
            //前端需要显示系统图标 和系统名称,有可能后续系统用户信息会修改 所以显示用的是 formUser本身实体的属性
            map.put("fromUser",userService.findUserById(message.getFromId()));
            noticeLists.add(map);
        }
        model.addAttribute("noticeLists",noticeLists);
        List<Integer> ids = getUnreadLetters(list);
        if(!ids.isEmpty()) {
            messageService.readMessage(ids);
        }
        return "/site/notice-detail";
    }

}
