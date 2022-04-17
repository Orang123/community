package com.nowcoder.dao;

import com.nowcoder.pojo.LoginTicket;
import org.apache.ibatis.annotations.Mapper;
import org.springframework.stereotype.Repository;

@Deprecated//登陆凭证 保存在redis中
@Mapper
@Repository
public interface LoginTicketMapper {

    int insertLoginTicket(LoginTicket loginTicket);

    LoginTicket selectByTicket(String ticket);

    int updateStatus(String ticket,int status);
}
