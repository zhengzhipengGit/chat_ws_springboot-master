package com.ws.chat.chat_app.service.impl;

import com.ws.chat.chat_app.mapper.LoginMapper;
import com.ws.chat.chat_app.po.Staff;
import com.ws.chat.chat_app.service.LoginService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;

@Transactional(propagation=Propagation.REQUIRED,isolation=Isolation.DEFAULT,timeout=5)
@Service("loginservice")
public class LoginServiceImpl implements LoginService {
	@Resource
	LoginMapper loginmapper;
	public String getpwdbyname(String name) {
		Staff s=loginmapper.getpwdbyname(name);
		if(s!=null)
		return s.getPassword();
		else
		return null;
	}
	public Long getUidbyname(String name) {
		Staff s=loginmapper.getpwdbyname(name);
		if(s!=null)
			return (long) s.getStaff_id();
			else
			return null;
	}
	public String getnamebyid(long id) {
		Staff s=loginmapper.getnamebyid(id);
		if(s!=null)
			return s.getUsername();
			else
			return null;
	}
	
	

}
