package com.gp.core;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.PropertyNamingStrategies.NamingBase;
import com.google.common.base.Joiner;
import com.google.common.base.Strings;
import com.google.common.collect.Iterables;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.gp.bind.BindScanner;
import com.gp.bind.IBeanBinder;
import com.gp.common.*;
import com.gp.common.GroupUsers.AuthenType;
import com.gp.dao.info.*;
import com.gp.exception.BaseException;
import com.gp.exception.ServiceException;
import com.gp.exec.OptionArg;
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
import com.gp.util.Lambdas;
import com.gp.util.NumberUtils;
import com.gp.web.ActionResult;
import com.gp.web.model.*;
import com.gp.web.util.WebUtils;
import org.jose4j.jwt.JwtClaims;
import org.jose4j.jwt.MalformedClaimException;
import org.jose4j.lang.JoseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * The Core Engine delegate class to access back-end database
 * 
 * @version 0.2
 * @author gdiao
 * @date 2017-10-12
 **/
public class CoreDelegate implements CoreAdapter, IBeanBinder {

	static final Logger LOGGER = LoggerFactory.getLogger(CoreDelegate.class);
	
	static final NamingBase SNAKE_CASE = (NamingBase)PropertyNamingStrategies.SNAKE_CASE;
	
	private final AuditService auditService;
	
	private final AuthorizeService authorizeService;
	
	private final DictionaryService dictService;
	
	private final SystemService systemService;
	
	private final SecurityService securityService;
		
	/**
	 * Default constructor 
	 **/
	public CoreDelegate(){
		
		if(LOGGER.isDebugEnabled()) {
			LOGGER.debug("The core delegate autowired and ready ...");
		}
		auditService = getBean(AuditService.class);
		authorizeService = getBean(AuthorizeService.class);
		dictService = getBean(DictionaryService.class);
		systemService = getBean(SystemService.class);
		securityService = getBean(SecurityService.class);
		
		initial();
		AuthenTypes.add(AuthenType.INTERIM);

		Consumer<Audit> auditor = this::persistAudit;
		CoreEngine.enableFeature(CoreConsts.FEATURE_AUDIT, OptionArg.newArg("auditor", auditor));

		Function<Operation, InfoId> oper = Lambdas.rethrow(OperSyncFacade.instance()::persistOperation);
		CoreEngine.enableFeature(CoreConsts.FEATURE_TRACE, OptionArg.newArg("persist", oper));

		Consumer<Operation> sync = Lambdas.rethrow(OperSyncFacade.instance()::persistSync);
		CoreEngine.enableFeature(CoreConsts.FEATURE_SYNC, OptionArg.newArg("sync", sync));
	}

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

		// query accounts information
		SysOptionInfo result = systemService.getOption( optionKey);
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
	public Principal getPrincipal(String subject, String userGid, String nodeGid)  {
	
		// here we only support user principal
		Principal principal = securityService.getPrincipal(subject, userGid, nodeGid, AuthenTypes.toArray(new AuthenType[0]));
		
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
	public AuthClient getAuthClient(String clientId) {
		
		ClientInfo info = authorizeService.getClient(clientId);
		AuthClient client = new AuthClient();
		InfoCopier.copy(info, client);
		
		return client;
	}

}
