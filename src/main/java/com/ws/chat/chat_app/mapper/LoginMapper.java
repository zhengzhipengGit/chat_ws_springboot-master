package com.ws.chat.chat_app.mapper;

import com.ws.chat.chat_app.po.Staff;


public interface LoginMapper {
	Staff getpwdbyname(String name);
	Staff getnamebyid(long id);
}
