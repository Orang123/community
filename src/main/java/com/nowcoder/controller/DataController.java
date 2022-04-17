package com.nowcoder.controller;

import com.nowcoder.service.DataService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import java.util.Date;

@Controller
public class DataController {

    @Autowired
    private DataService dataService;

    @RequestMapping(path = "/data",method = {RequestMethod.GET,RequestMethod.POST})
    public String getDataPage() {
        return "/site/admin/data";
    }

    @RequestMapping(path = "/data/uv",method = RequestMethod.POST)
    public String getUV(@DateTimeFormat(pattern = "yyyy-MM-dd") Date uvStart,
                        @DateTimeFormat(pattern = "yyyy-MM-dd") Date uvEnd, Model model) {
        long uvCount = dataService.calculateUV(uvStart, uvEnd);
        //这里Date类型 不能用param获取到
        model.addAttribute("uvStart",uvStart);
        model.addAttribute("uvEnd",uvEnd);
        model.addAttribute("uvCount",uvCount);
        //转发 能把当前携带的数据共享到下一个RequestMapping 这里forward: ":"冒号后必须要跟"/" 否则会404
        return "forward:/data";
    }

    @RequestMapping(path = "/data/dau",method = RequestMethod.POST)
    public String getDAU(@DateTimeFormat(pattern = "yyyy-MM-dd") Date dauStart,
                         @DateTimeFormat(pattern = "yyyy-MM-dd") Date dauEnd, Model model) {
        long dauCount = dataService.calculateDAU(dauStart, dauEnd);
        model.addAttribute("dauStart",dauStart);
        model.addAttribute("dauEnd",dauEnd);
        model.addAttribute("dauCount",dauCount);
        return "forward:/data";
    }

}
