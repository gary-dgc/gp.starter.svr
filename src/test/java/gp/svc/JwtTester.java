/*******************************************************************************
 * This project is released under the Apache License, version 2.0.
 * This is a commercial solution, requiring a valid paid-for license for 
 * commercial use. This product is free to use for non-commercial applications, 
 * like non-profits and educational usage.
 * Copyright (c) 2016 to Present
 * All rights reserved to Gary Diao(gary.diao@yahoo.com)
 ******************************************************************************/
package gp.svc;

import java.util.Arrays;
import java.util.List;

import org.jose4j.jwt.JwtClaims;
import org.jose4j.jwt.consumer.InvalidJwtException;
import org.jose4j.lang.JoseException;

import com.gp.util.JwtTokenUtils;

public class JwtTester {

	public static void main(String[] args) {
		JwtTester tester = new JwtTester();
		
		String token = tester.getJwt();
		System.out.println(token);
		JwtClaims claims = tester.checkJwt(token);
		System.out.println(claims);
		
		claims = tester.parseJwt(token);
		System.out.println(claims);
		
		StringBuilder myName = new StringBuilder(token);
		myName.setCharAt(4, 'x');
		token = myName.toString(); 
		
		claims = tester.checkJwt(token);
		System.out.println(claims);
	}

	public String getJwt() {
		
		// 创建claims，这将是JWT的内容 B部分
		JwtClaims claims = new JwtClaims();
		claims.setIssuer("Issuer"); // 谁创建了令牌并签署了它
		claims.setAudience("Audience"); // 令牌将被发送给谁
		claims.setExpirationTimeMinutesInTheFuture(10); // 令牌失效的时间长（从现在开始10分钟）
		claims.setGeneratedJwtId(); // 令牌的唯一标识符
		claims.setIssuedAtToNow(); // 当令牌被发布/创建时（现在）
		claims.setNotBeforeMinutesInThePast(2); // 在此之前，令牌无效（2分钟前）
		claims.setSubject("subject"); // 主题 ,是令牌的对象
		claims.setClaim("email", "mail@example.com"); // 可以添加关于主题的附加 声明/属性
		List<String> groups = Arrays.asList("group-one", "other-group", "group-three");

		try {
			return JwtTokenUtils.getJwt(claims);
		} catch (JoseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return null;
		}
	}
	
	public JwtClaims parseJwt(String jwtStr) {
		
			JwtClaims claims = JwtTokenUtils.parseJwt(jwtStr);
			
			return claims;
		
	}
	
	public JwtClaims checkJwt(String jwtStr) {
		
		try {
			JwtClaims claims = JwtTokenUtils.verifyJwt(jwtStr);
			
			return claims;
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return null;
		}
	}
}
