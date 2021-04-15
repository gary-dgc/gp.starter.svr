package com.gp.core;

import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

import org.jose4j.jwt.JwtClaims;
import org.jose4j.jwt.MalformedClaimException;
import org.jose4j.lang.JoseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.PropertyNamingStrategy.PropertyNamingStrategyBase;
import com.google.common.base.Joiner;
import com.google.common.base.Strings;
import com.google.common.collect.Iterables;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.gp.bind.BindScanner;
import com.gp.common.Filters;
import com.gp.common.GroupUsers;
import com.gp.common.GroupUsers.AuthenType;
import com.gp.common.IdKeys;
import com.gp.common.InfoId;
import com.gp.common.KeyValuePair;
import com.gp.common.ServiceContext;
import com.gp.dao.info.AuditInfo;
import com.gp.dao.info.ClientInfo;
import com.gp.dao.info.DictionaryInfo;
import com.gp.dao.info.SysOptionInfo;
import com.gp.dao.info.TokenInfo;
import com.gp.dao.info.UserInfo;
import com.gp.exception.BaseException;
import com.gp.exception.ServiceException;
import com.gp.info.BaseIdKey;
import com.gp.info.InfoCopier;
import com.gp.info.Principal;
import com.gp.svc.AuditService;
import com.gp.svc.SystemService;
import com.gp.svc.master.DictionaryService;
import com.gp.svc.security.AuthorizeService;
import com.gp.svc.security.SecurityService;
import com.gp.util.JsonUtils;
import com.gp.util.JwtTokenUtils;
import com.gp.util.NumberUtils;
import com.gp.web.ActionResult;
import com.gp.web.model.Audit;
import com.gp.web.model.AuthClient;
import com.gp.web.model.AuthToken;
import com.gp.web.model.AuthenData;
import com.gp.web.model.SysOption;
import com.gp.web.util.WebUtils;

/**
 * The Core Engine delegate class to access back-end database
 * 
 * @version 0.2
 * @author gdiao
 * @date 2017-10-12
 **/
public class CoreDelegate implements CoreAdapter{

	static Logger LOGGER = LoggerFactory.getLogger(CoreDelegate.class);
	
	static PropertyNamingStrategyBase SNAKE_CASE = (PropertyNamingStrategyBase)PropertyNamingStrategy.SNAKE_CASE;
	
	private AuditService auditService;
	
	private AuthorizeService authorizeService;
	
	private DictionaryService dictService;
	
	private SystemService systemService;
	
	private SecurityService securityService;
		
	/**
	 * Default constructor 
	 **/
	public CoreDelegate(){
		
		if(LOGGER.isDebugEnabled()) {
			LOGGER.debug("The core deleage autowired and ready ...");
		}
		auditService = BindScanner.instance().getBean(AuditService.class);
		authorizeService = BindScanner.instance().getBean(AuthorizeService.class);
		dictService = BindScanner.instance().getBean(DictionaryService.class);
		systemService = BindScanner.instance().getBean(SystemService.class);
		securityService = BindScanner.instance().getBean(SecurityService.class);
		
		initial();
		AuthenTypes.add(AuthenType.INTERIM);
	}
	
	@Override
	public void persistAudit(Audit audit)  {
		
		if(Objects.isNull(audit))
			return ;
		
		AuditInfo info = new AuditInfo();
		InfoCopier.copy(audit, info);
		info.setPath(audit.getApi());
		info.setInfoId(audit.getAuditId());
		info.setModifierUid(GroupUsers.ADMIN_UID.getId());
		info.setModifyTime(new Date(System.currentTimeMillis()));
		
		auditService.addAudit( info );
		
	}

	@Override
	public String getMessagePattern(Locale locale, String dictKey) {
		DictionaryInfo dinfo = dictService.getDictEntry(dictKey.toLowerCase(), false);
		if(dinfo == null) return dictKey;
		String msgptn = dictKey;
		
		for(String lang : Filters.LANGUAGES) {
			if(lang.equalsIgnoreCase(locale.toString())){
				msgptn = dinfo.getLabel(lang);
				break;
			}
		}
		
		return msgptn;
	}

	@Override
	public String getPropertyName(Locale locale, String dictKey) {
		String newkey = SNAKE_CASE.translate(dictKey);
		if( !newkey.startsWith( DictionaryService.PROP_PREFIX ) ){
			newkey = DictionaryService.PROP_PREFIX + newkey;
		}
		
		DictionaryInfo dinfo = dictService.getDictEntry(newkey, true);
		if(dinfo == null) return dictKey;
		String msgptn = dictKey;
		
		for(String lang : Filters.LANGUAGES) {
			if(lang.equalsIgnoreCase(locale.toString())){
				msgptn = dinfo.getLabel(lang);
				break;
			}
		}
		
		return msgptn;
	}

	@Override
	public SysOption getSystemOption(String optionKey) {
		SysOptionInfo result = null;		
		
		// query accounts information
		result = systemService.getOption( optionKey);
		SysOption opt = new SysOption();
		InfoCopier.copy(result, opt);
		
		return opt;
	}

	@Override
	public AuthToken getAuthToken(InfoId tokenId, String refreshToken)  {
	
		TokenInfo info = securityService.getToken( tokenId, refreshToken );
		AuthToken token = new AuthToken();
		InfoCopier.copy(info, token);
		token.setTokenId(info.getId());
		
		return token;
	}

	@Override
	public Principal getPrincipal(String login)  {
	
		// here we only support user principal
		Principal principal = securityService.getPrincipal(login, AuthenTypes.toArray(new AuthenType[0]));
		
 		return principal;
		
	}

	@Override
	public String refreshAuthToken(ServiceContext svcctx, JwtClaims claims, String refreshToken)  {
			
		String token = null;
		try {

			Long jwtid = NumberUtils.toLong(claims.getJwtId());
			InfoId tokenId = IdKeys.getInfoId(BaseIdKey.TOKEN,jwtid);
			
			TokenInfo tokenInfo = new TokenInfo();
			
			tokenInfo.setInfoId(tokenId);
			tokenInfo.setIssueAt(new Date(claims.getIssuedAt().getValueInMillis()));
			tokenInfo.setExpireTime(new Date(claims.getExpirationTime().getValueInMillis()));
			tokenInfo.setNotBefore(new Date(claims.getNotBefore().getValueInMillis()));
			
			token = JwtTokenUtils.getJwt(claims);
			tokenInfo.setJwtToken(token);
			tokenInfo.setRefreshToken(refreshToken);
			
			securityService.refreshToken(svcctx, tokenInfo);
			
		} catch (JoseException | MalformedClaimException e) {
			
			LOGGER.error("fail issue token", e);
		}
		
		return token;
		
	}

	@Override
	public boolean removeAuthToken(ServiceContext svcctx, InfoId tokenId)  {
	
		return securityService.removeToken(svcctx, tokenId);
	
	}

	@Override
	public Boolean authenticate(ServiceContext svcctx, AuthenData authenData) throws BaseException{
		
		String nodeGid = systemService.getLocalNodeGid();
		
		svcctx.putContextData(JwtTokenUtils.NODE_GLOBAL_ID, nodeGid);
		
		// save device and  transfer it across methods 
		String device = (String) authenData.getExtraValue("device");
		svcctx.putContextData(JwtTokenUtils.DEVICE_ID, device);
		
		return securityService.authenticate(svcctx, authenData.getPrincipal(), 
				authenData.getCredential(), 
				AuthenTypes.toArray(new AuthenType[0]));
		
	}

	@Override
	public String newAuthToken(ServiceContext svcctx, JwtClaims claims, String refreshToken) {
		
		InfoId tokenId = IdKeys.newInfoId(BaseIdKey.TOKEN);
		TokenInfo info = new TokenInfo();

		String jwt = null;
		try {
					
			info.setInfoId(tokenId);
			claims.setJwtId(String.valueOf(tokenId.getId()));
			
			info.setAudience(Iterables.getFirst(claims.getAudience(), ""));
			
			info.setIssuer(claims.getIssuer());
			info.setSubject(claims.getSubject());
			info.setIssueAt(new Date(claims.getIssuedAt().getValueInMillis()));
			info.setExpireTime(new Date(claims.getExpirationTime().getValueInMillis()));
			info.setNotBefore(new Date(claims.getNotBefore().getValueInMillis()));
			
			Set<String> keys = Sets.newHashSet("aud", "iss", "sub", "aud", "exp", "iat", "jti", "nbf", "scp");
			info.setClaims(JsonUtils.toJson(claims.getClaimsMap(keys)));
			
			info.setDevice(claims.getStringClaimValue(JwtTokenUtils.DEVICE_ID));
			String scope = Joiner.on(' ').join((List<?>)claims.getClaimValue(JwtTokenUtils.SCOPE));
			info.setScope(scope);
			info.setRefreshToken(refreshToken);
			
			svcctx.setTraceInfo(info);
			
			jwt = JwtTokenUtils.getJwt(claims);
			info.setJwtToken(jwt);
			
			securityService.newToken(svcctx, info);
		} catch (JoseException | MalformedClaimException e) {
			
			LOGGER.error("fail issue token", e);
		}
		
		return jwt;
		
	}

	@Override
	public KeyValuePair<JwtClaims, String> swapAuthToken(ServiceContext svcctx, String token, String scope, String subject) throws ServiceException {
		
		JwtClaims _payload = JwtTokenUtils.parseJwt(token);
		KeyValuePair<JwtClaims, String> rtv = KeyValuePair.newPair();
		try {
				
			String userGid = _payload.getStringClaimValue(JwtTokenUtils.USER_GLOBAL_ID);
			if(Strings.isNullOrEmpty(userGid)) {
				throw new ServiceException("excp.miss.user_gid");
			}
		
			Map<String, Object> post = Maps.newHashMap();
			post.put(Filters.TOKEN, token); // send token for verification
			post.put(Filters.SUBJECT, subject); // send bizcard code for verification
			
			try {
				
				ActionResult verify = NodeApiAgent.instance().sendGlobalPost(WebUtils.getAuthApiUri("verify-token"), post);
				if(!verify.isSuccess()) {
					throw new ServiceException("excp.not.valid");
				}
				
			} catch(BaseException be) {
				throw new ServiceException(be, "excp.not.valid");
			}
			
			UserInfo info = securityService.getUserInfo(null, null, subject, userGid);
			if (null == info) {
				throw new ServiceException("excp.not.exist");
			}
			JwtClaims claims = new JwtClaims();
			claims.setIssuer(getTokenIssuer());
			claims.setSubject(info.getUsername());
			claims.setAudience(_payload.getAudience());
			claims.setNotBefore(_payload.getNotBefore());
			claims.setIssuedAtToNow();
			claims.setExpirationTime(_payload.getExpirationTime());
			
			claims.setClaim(JwtTokenUtils.USER_GLOBAL_ID, userGid);
			claims.setClaim(JwtTokenUtils.DEVICE_ID, _payload.getClaimValue(JwtTokenUtils.DEVICE_ID));
			if(!Strings.isNullOrEmpty(scope)) {
				List<String> scopes = Arrays.asList(scope.split("\\s+"));
		        claims.setStringListClaim(JwtTokenUtils.SCOPE, scopes);
			}
			String gid = systemService.getLocalNodeGid();
			claims.setClaim(JwtTokenUtils.NODE_GLOBAL_ID, gid);
			
			InfoId tokenId = IdKeys.newInfoId(BaseIdKey.TOKEN);
			TokenInfo tokenInfo = new TokenInfo();
	
			tokenInfo.setInfoId(tokenId);
			claims.setJwtId(String.valueOf(tokenId.getId()));
			tokenInfo.setAudience(Iterables.getFirst(claims.getAudience(), ""));
			
			tokenInfo.setIssuer(claims.getIssuer());
			tokenInfo.setSubject(claims.getSubject());
			tokenInfo.setIssueAt(new Date(claims.getIssuedAt().getValueInMillis()));
			tokenInfo.setExpireTime(new Date(claims.getExpirationTime().getValueInMillis()));
			tokenInfo.setNotBefore(new Date(claims.getNotBefore().getValueInMillis()));
			
			Set<String> keys = Sets.newHashSet("aud", "iss", "sub", "aud", "exp", "iat", "jti", "nbf", "scp");
			tokenInfo.setClaims(JsonUtils.toJson(claims.getClaimsMap(keys)));
			
			tokenInfo.setDevice(claims.getStringClaimValue(JwtTokenUtils.DEVICE_ID));
	
			tokenInfo.setScope(scope);
			
			String refreshToken = UUID.randomUUID().toString();
			tokenInfo.setRefreshToken(refreshToken);
			
			svcctx.setTraceInfo(tokenInfo);
			
			securityService.newToken(svcctx, tokenInfo);
			
			String _password = securityService.newInterimLogin(svcctx, 
					info.getInfoId(), // user id
					(String)_payload.getClaimValue(JwtTokenUtils.DEVICE_ID) // device 
				);
			// return the token payload and the interim password
			rtv.setKey(claims);
			rtv.setValue(_password);
			
		} catch (MalformedClaimException e) {
			
			LOGGER.error("fail issue token", e);
		}
		
		return rtv;
	}

	@Override
	public AuthClient getAuthClient(String clientId) {
		
		ClientInfo info = authorizeService.getClient(clientId);
		AuthClient client = new AuthClient();
		InfoCopier.copy(info, client);
		
		return client;
	}

}
