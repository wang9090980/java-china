package com.javachina.service.impl;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.blade.ioc.annotation.Inject;
import com.blade.ioc.annotation.Service;
import com.blade.jdbc.AR;
import com.blade.jdbc.Page;
import com.blade.jdbc.QueryParam;
import com.javachina.kit.ImageKit;
import com.javachina.model.User;
import com.javachina.model.Userinfo;
import com.javachina.service.ActivecodeService;
import com.javachina.service.UserService;
import com.javachina.service.UserinfoService;

import blade.kit.DateKit;
import blade.kit.EncrypKit;
import blade.kit.StringKit;

@Service
public class UserServiceImpl implements UserService {
	
	@Inject
	private ActivecodeService activecodeService;
	
	@Inject
	private UserinfoService userinfoService;
	
	@Override
	public User getUser(Long uid) {
		return AR.findById(User.class, uid);
	}
	
	@Override
	public User getUser(QueryParam queryParam) {
		return AR.find(queryParam).first(User.class);
	}
		
	@Override
	public List<User> getUserList(QueryParam queryParam) {
		if(null != queryParam){
			return AR.find(queryParam).list(User.class);
		}
		return null;
	}
	
	@Override
	public Page<User> getPageList(QueryParam queryParam) {
		if(null != queryParam){
			return AR.find(queryParam).page(User.class);
		}
		return null;
	}
	
	@Override
	public boolean signup(String loginName, String passWord, String email) {
		if(StringKit.isBlank(loginName) || StringKit.isBlank(passWord) || StringKit.isBlank(email)){
			return false;
		}
		int time = DateKit.getCurrentUnixTime();
		String pwd = EncrypKit.md5(loginName + passWord);
		try {
			
			String avatar = "default/" + StringKit.random(5) + ".png";
			
			Long uid = (Long) AR.update("insert into t_user(login_name, pass_word, email, avatar, create_time, update_time) values(?, ?, ?, ?, ?)",
					loginName, pwd, email, avatar, time, time).key();
			
			User user = this.getUser(uid);
			
			// 发送邮件通知
			activecodeService.save(user, "signup");
			return true;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return false;
	}
	
	@Override
	public boolean delete(Long uid) {
		if(null != uid){
			AR.update("delete from t_user where uid = ?", uid).executeUpdate();
			return true;
		}
		return false;
	}

	@Override
	public boolean resetPwd(String email) {
		
		return false;
	}

	@Override
	public User signin(String loginName, String passWord) {
		if(StringKit.isBlank(loginName) || StringKit.isBlank(passWord)){
			return null;
		}
		String pwd = EncrypKit.md5(loginName + passWord);
	    User user = AR.find("select * from t_user where login_name = ? and pass_word = ? and status = ?",
				loginName, pwd, 1).first(User.class);
		return user;
	}

	@Override
	public Map<String, Object> getUserDetail(Long uid) {
		Map<String, Object> map = new HashMap<String, Object>();
		if(null != uid){
			User user = this.getUser(uid);
			map.put("username", user.getLogin_name());
			map.put("uid", uid);
			String avatar = ImageKit.getAvatar(user.getAvatar());
			map.put("avatar", avatar);
			map.put("create_time", user.getCreate_time());
			
			Userinfo userinfo = userinfoService.getUserinfo(uid);
			if(null != userinfo){
				map.put("signature", userinfo.getSignature());
				map.put("website", userinfo.getWeb_site());
				map.put("instructions", userinfo.getInstructions());
			}
		}
		return map;
	}

	@Override
	public boolean active(Long id, Long uid) {
		if (null != id && null != uid) {
			try {
				activecodeService.useCode(id, "signup");
				this.updateStatus(uid, 1);
				return true;
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		return false;
	}

	@Override
	public boolean updateStatus(Long uid, Integer status) {
		if(null != uid && null != status){
			try {
				AR.update("update t_user set status = ? where uid = ?", status, uid).executeUpdate();
				return true;
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		return false;
	}

	@Override
	public boolean updateCount(Long uid, String type, int count) {
		if(null != uid && StringKit.isNotBlank(type)){
			try {
				String sql = "update t_user set %s = (%s + ?) where uid = ?";
				AR.update(String.format(sql, type, type), count, uid).executeUpdate();
				return true;
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		return false;
	}
		
}
