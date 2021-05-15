/*******************************************************************************
 * This project is released under the Apache License, version 2.0.
 * This is a commercial solution, requiring a valid paid-for license for 
 * commercial use. This product is free to use for non-commercial applications, 
 * like non-profits and educational usage.
 * Copyright (c) 2016 to Present
 * All rights reserved to Gary Diao(gary.diao@yahoo.com)
 ******************************************************************************/
package com.gp.web.api;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.CaseFormat;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.common.reflect.ClassPath;
import com.google.common.reflect.ClassPath.ClassInfo;
import com.gp.bean.BeanAccessor;
import com.gp.bean.BeanMeta;
import com.gp.bind.BindScanner;
import com.gp.common.Filters;
import com.gp.common.GeneralConsts;
import com.gp.common.IdKeys;
import com.gp.common.InfoId;
import com.gp.common.KeyValuePair;
import com.gp.common.NodeIdKey;
import com.gp.common.ServiceContext;
import com.gp.dao.info.OrgHierInfo;
import com.gp.dao.info.RoleInfo;
import com.gp.dao.info.SourceInfo;
import com.gp.dao.info.StorageInfo;
import com.gp.dao.info.UserInfo;
import com.gp.dao.info.WorkgroupInfo;
import com.gp.exception.BaseException;
import com.gp.exception.ServiceException;
import com.gp.exception.WebException;
import com.gp.info.DataBuilder;
import com.gp.info.InfoCopier;
import com.gp.info.Principal;
import com.gp.svc.CommonService;
import com.gp.svc.master.OrgHierService;
import com.gp.svc.master.SourceService;
import com.gp.svc.master.StorageService;
import com.gp.svc.security.RolePermService;
import com.gp.svc.user.UserService;
import com.gp.svc.wgroup.WGroupService;
import com.gp.util.NumberUtils;
import com.gp.validate.ValidateMessage;
import com.gp.validate.ValidateUtils;
import com.gp.web.ActionResult;
import com.gp.web.BaseApiSupport;
import com.gp.web.anno.WebApi;

import io.undertow.server.HttpServerExchange;

public class CommonHandler extends BaseApiSupport {

	static Logger LOGGER = LoggerFactory.getLogger(CommonHandler.class);
	
	private SourceService sourceService;
	
	private StorageService storageService;
	
	private UserService userService;
	
	private RolePermService rolePermService;
	
	private OrgHierService orghierService;
	
	private WGroupService wgroupService;

	private CommonService commonService;
	
	private Map <String, ClassInfo> clazzMap = Maps.newHashMap();
	
	public CommonHandler() {
		
		sourceService = BindScanner.instance().getBean(SourceService.class);
		storageService = BindScanner.instance().getBean(StorageService.class);
		userService = BindScanner.instance().getBean(UserService.class);
		rolePermService = BindScanner.instance().getBean(RolePermService.class);
		orghierService = BindScanner.instance().getBean(OrgHierService.class);
		wgroupService = BindScanner.instance().getBean(WGroupService.class);
		commonService = BindScanner.instance().getBean(CommonService.class);
		
		loadInfoClasses();
	}
	
	
	/**
	 * Load all the info bean classes to Validate data with 
	 * annotation rules defined on properties.
	 * 
	 **/
	private void loadInfoClasses() {
		
		ClassPath clzpath;
		try {
			clzpath = ClassPath.from(ClassLoader.getSystemClassLoader());
			Set<ClassInfo> allClasses = clzpath.getTopLevelClassesRecursive("com.gp.dao.info");
			
			for(ClassInfo cinfo: allClasses) {
				String simplename = cinfo.getSimpleName();
				
		        clazzMap.put(simplename, cinfo);
		        
		        String snakename = CaseFormat.LOWER_CAMEL.to(CaseFormat.LOWER_UNDERSCORE, simplename);// demo_bean_info
		        clazzMap.put(snakename, cinfo);
		        
				if(simplename.endsWith("Info") || simplename.endsWith("info")) {
					
					simplename = simplename.substring(0, simplename.length() - 4); // DemoBean
					clazzMap.put(simplename, cinfo);
					
					snakename = CaseFormat.LOWER_CAMEL.to(CaseFormat.LOWER_UNDERSCORE, simplename); // demo_bean
					clazzMap.put(snakename, cinfo);
					
					clazzMap.put(simplename.toLowerCase(), cinfo); // demobean
					
				}
			}
			
		} catch (IOException e) {
			LOGGER.error("Fail to load all the classes", e);
		}
		
	}
	
	@WebApi(path="common-source-query")
	public void handleNodeList(HttpServerExchange exchange) throws BaseException {

		Map<String, Object> paramMap = this.getRequestBody(exchange) ;
		
		String namecond = Filters.filterString( paramMap, "instance_name");
				
		List<KeyValuePair<String,String>> enlist = Lists.newArrayList();
		ActionResult result = null;
	
		// query accounts information
		List<SourceInfo> gresult =  sourceService.getSources(null, namecond, null);
		
		for(SourceInfo einfo : gresult){
			Long id = einfo.getInfoId().getId();
			KeyValuePair<String, String> kv = KeyValuePair.newPair(String.valueOf(id), einfo.getSourceName());
			enlist.add(kv);
		}
		
		result = ActionResult.failure(getMessage(exchange, "mesg.find.sources"));
		result.setData(enlist);

		this.sendResult(exchange, result);
	}
	
	/**
	 * Get the storage list,  
	 **/
	@WebApi(path="common-storage-list")
	public void handleStorageList(HttpServerExchange exchange)throws BaseException {
		
		Map<String, Object> paramMap = this.getRequestBody(exchange);
		
		String namecond = Filters.filterString( paramMap, "keyword");
			
		List<KeyValuePair<String,String>> stglist = Lists.newArrayList();
		ActionResult result = null;
		String[] types = null;
		String[] states = null;
	
		List<StorageInfo> gresult = storageService.getStorages( namecond, types, states, null);	
		for(StorageInfo sinfo : gresult){
			Long id = sinfo.getInfoId().getId();
			KeyValuePair<String, String> kv = KeyValuePair.newPair(String.valueOf(id), sinfo.getStorageName());
			stglist.add(kv);
		}
		
		result = ActionResult.success(getMessage(exchange, "mesg.find.sources"));
		result.setData(stglist);

		this.sendResult(exchange, result);
	} 
	
	/**
	 * Get the storage list,  
	 **/
	@WebApi(path="common-role-list")
	public void handleRoleList(HttpServerExchange exchange)throws BaseException {
		
		Map<String, Object> paramMap = this.getRequestBody(exchange);
		
		String namecond = Filters.filterString( paramMap, "keyword");
		String defaultCase = Filters.filterString( paramMap, "default_case");
				
		List<KeyValuePair<String,String>> rolelist = Lists.newArrayList();
		ActionResult result = null;
		
		List<RoleInfo> gresult = rolePermService.getRoles(namecond, defaultCase, null);	
		for(RoleInfo rinfo : gresult){
			Long id = rinfo.getInfoId().getId();
			KeyValuePair<String, String> kv = KeyValuePair.newPair(String.valueOf(id), rinfo.getRoleName());
			rolelist.add(kv);
		}
 		
		result = ActionResult.success(getMessage(exchange, "mesg.find.roles"));
		result.setData(rolelist);

		this.sendResult(exchange, result);
	} 
	
	/**
	 * Support Select User Dialog to list all the users in system 
	 **/
	@WebApi(path="common-user-list")
	public void handleUserList(HttpServerExchange exchange)throws BaseException{

		Map<String, Object> paramMap = this.getRequestBody(exchange);
		
		String uname = Filters.filterString( paramMap, "keyword");
		Long sourceId = Filters.filterLong(paramMap, "source_id");
		Boolean boundOnly = Filters.filterBoolean(paramMap, "bound_only");
		
		ActionResult result = new ActionResult();
		List<Object> list = null;

		// query accounts information
		List<UserInfo> cresult = userService.getUsers( uname, sourceId, null, null, boundOnly, null);
		list = cresult.stream().map((info) -> {
			
			DataBuilder builder = new DataBuilder();
			builder.set("user_id", info.getId().toString());
			
			builder.set(info, "user_gid", "username", "email", "mobile", 
					"category", "full_name");
			
			builder.set("source", sbuilder -> {
				sbuilder.set("source_id", info.getProperty("source_id", Long.class).toString());
				sbuilder.set(info, "source_name");
			});
			String avatarUrl = info.getProperty("avatar_url", String.class);
			avatarUrl = ServiceApiHelper.absoluteBinaryUrl( avatarUrl);
			builder.set("avatar_url", avatarUrl);

			return builder.build();
			
		}).collect(Collectors.toList());
						
		result = ActionResult.success(getMessage(exchange, "mesg.find.users"));
		result.setData(list);
			
		this.sendResult(exchange, result);
		
	}

	@WebApi(path="common-org-nodes")
	public void handleOrghierNodes(HttpServerExchange exchange)throws BaseException {
	
		Map<String, Object> paramMap = this.getRequestBody(exchange);
		String orgIdStr = Filters.filterString( paramMap, "org_id");

		ActionResult result = ActionResult.success(getMessage(exchange, "mesg.find.orgnodes"));
		List<Map<String, Object>> olist = Lists.newArrayList();		
		Long orgId = null;
		if(!Strings.isNullOrEmpty(orgIdStr)){
		
			orgId = NumberUtils.toLong(orgIdStr, GeneralConsts.HIER_ROOT);
		}else{
			
			result.setData(olist);
			this.sendResult(exchange, result);
			return;
		}
		
		InfoId oid = IdKeys.getInfoId(NodeIdKey.ORG_HIER,orgId);
		
		List<OrgHierInfo> gresult = orghierService.getOrgHierAllNodes( oid);
		  
		for(OrgHierInfo orghier : gresult){
			DataBuilder builder = new DataBuilder();
			
			builder.set("id", orghier.getId().toString());
			if(GeneralConsts.HIER_ROOT != orghier.getOrgPid()){
				builder.set("parent", orghier.getOrgPid().toString());
			}
			
			builder.set(orghier, "org_name", "description", "email");
		
			String avatarUrl = ServiceApiHelper.absoluteBinaryUrl(orghier.getAvatarUrl());
			builder.set("avatar_url", avatarUrl);
			
			olist.add(builder.build());
		}
		
		result.setData(olist);

		this.sendResult(exchange, result);	
	}

	@WebApi(path="common-org-list")
	public void handleOrghierNodeList(HttpServerExchange exchange)throws BaseException {
		Map<String, Object> paramMap = this.getRequestBody(exchange);
		
		String orgName = Filters.filterString( paramMap, "org_name");
		String orgPid = Filters.filterString( paramMap, "org_pid");
		
		List<Map<String, Object>> list = Lists.newArrayList();

		ActionResult result = new ActionResult();
		
		InfoId pid = orgPid == null ? null: IdKeys.getInfoId(NodeIdKey.ORG_HIER, NumberUtils.toLong(orgPid));
		
		List<OrgHierInfo> olist = orghierService.getOrgHierNodes( orgName, pid);
		
		for(OrgHierInfo orghier: olist){
			DataBuilder builder = new DataBuilder();
			
			builder.set("id", orghier.getId().toString());
			if(GeneralConsts.HIER_ROOT != orghier.getOrgPid()){
				builder.set("parent", orghier.getOrgPid().toString());
			}
			
			builder.set(orghier, "org_name", "description", "email");
		
			String avatarUrl = ServiceApiHelper.absoluteBinaryUrl(orghier.getAvatarUrl());
			builder.set("avatar_url", avatarUrl);
			
			list.add(builder.build());
			
		}	
		
		result = ActionResult.success(getMessage(exchange, "mesg.find.orgnodes"));
		result.setData(list);

		this.sendResult(exchange, result);
	}
	
	/**
	 * This is used in dropdown widget to list available users could be assigned to a given workgroup
	 **/
	@WebApi(path="common-avail-users")
	public void handleAvailableUserList(HttpServerExchange exchange)throws BaseException {

		Map<String, Object> paramMap = this.getRequestBody(exchange);
		
		String wgroupid = Filters.filterString( paramMap, "wgroup_id");
		String account = Filters.filterString( paramMap, "username");
	
		List<Map<String, Object>> list = Lists.newArrayList();

		ActionResult result = new ActionResult();
		
		InfoId wkey = IdKeys.getInfoId(NodeIdKey.WORKGROUP, NumberUtils.toLong(wgroupid));
		
		// query accounts information
		List<UserInfo> ulist = wgroupService.getAvailableUsers( wkey, account, null);
		for(UserInfo info: ulist){
			
			DataBuilder builder = new DataBuilder();
			builder.set("source_id", info.getSourceId().toString());
			builder.set("user_id", info.getInfoId().getId().toString());
			builder.set(info, "username", "email", "mobile", "category", 
					"full_name", "source_name", "state");
			
			list.add(builder.build());
	
		}	
	
		result = ActionResult.success(getMessage(exchange, "mesg.find.users"));
		result.setData(list);

		this.sendResult(exchange, result);
	}
	
	@WebApi(path="common-workgroup-list")
	public void handleWorkgroupList(HttpServerExchange exchange)throws BaseException {
		
		Map<String, Object> paramMap = this.getRequestBody(exchange);
		
		String name = Filters.filterString( paramMap, "keyword");
		String category = Filters.filterAll(paramMap, "category");
		List<Map<String, Object>> list = Lists.newArrayList();
		
		ActionResult result = new ActionResult();
		
		ServiceContext svcctx = ServiceContext.getPseudoContext();
		List<WorkgroupInfo> wlist = wgroupService.getWorkgroups( svcctx, name, null, null, 
				Strings.isNullOrEmpty(category) ? null: new String[] {category}, 
				null);
		
		for(WorkgroupInfo info: wlist){
			
			DataBuilder builder = new DataBuilder();
			builder.set("workgroup_id", info.getId().toString());
			builder.set(info, "workgroup_name", "state", "description");
			builder.set("admin_uid", info.getAdminUid().toString());
			
			String avatarUrl = ServiceApiHelper.absoluteBinaryUrl( info.getAvatarUrl());
			builder.set("avatar_url", avatarUrl);
			
			list.add(builder.build());
		}	
	
		result = ActionResult.success(getMessage(exchange, "mesg.find.users"));
		result.setData(list);

		this.sendResult(exchange, result);
		
	}
	
	/**
	 * This is used in dropdown widget to list available users could be assigned to a given workgroup
	 **/
	@WebApi(path="common-generate-id")
	public void handleGenerateId(HttpServerExchange exchange) throws WebException {
		
		InfoId newId = IdKeys.newInfoId("gp_blind");
		ActionResult result = new ActionResult();

		result = ActionResult.success(getMessage(exchange, "mesg.generate.id"));
		result.setData(newId.getId().toString());
		
		this.sendResult(exchange, result);
	}
	
	/**
	 * Validate the data with the rules defined on Bean Class
	 * JSON pattern: {schema: "", data: "", mode: ""},
	 * the mode: ALL, PROP
	 **/
	@SuppressWarnings("unchecked")
	@WebApi(path="common-validate")
	public void handleValidate(HttpServerExchange exchange)throws BaseException {
		
		ActionResult result = new ActionResult();
		Map<String, Object> paraMap = this.getRequestBody(exchange);
		if(paraMap == null || paraMap.isEmpty()) {
			
			result = ActionResult.failure(getMessage(exchange, "excp.miss.param"));
			this.sendResult(exchange, result);
			return;
		}
		
		Principal principal = this.getPrincipal(exchange);
		
		try {
			
			String clzNode = (String)paraMap.get("schema");
			ClassInfo clazzInfo = clazzMap.get(clzNode);
			if(null == clazzInfo) {
				this.sendResult(exchange, result);
				return;
			}
			Map<String, Object> dataNode = (Map<String, Object>)paraMap.get("data");
			String modeNode = (String)paraMap.get("mode");
			boolean allMode = Filters.ALL.equalsIgnoreCase( modeNode) || Strings.isNullOrEmpty(modeNode);
			if(LOGGER.isDebugEnabled()) {
				LOGGER.debug("validate schema: {} / data: {}", clzNode, dataNode.toString());
			}
						
			BeanMeta clazzMeta = BeanAccessor.getBeanMeta(clazzInfo.getClass());
			Object bean = clazzMeta.getBeanClass().newInstance();
			InfoCopier.copy(dataNode, bean);
						
			Set<ValidateMessage> vmsg = Sets.newHashSet();
			// keys of props in json
			Set<String> keys = Sets.newHashSet();
			if(! allMode){
				
				// List all the props in json
				Set<String> props = dataNode.keySet();
				
				for(String key: props) {
					
					key = CaseFormat.UPPER_UNDERSCORE.to(CaseFormat.UPPER_CAMEL, key);
					key = key.substring(0, 1).toLowerCase() + key.substring(1);
					if(clazzMeta.hasProperty(key)) {
						keys.add(key);
					}
				}
				
				String[] propNames = keys.toArray(new String[0]);
				if(LOGGER.isDebugEnabled()) {
					LOGGER.debug("validate bean: {} / props: {}", clazzMeta.getBeanName(), Arrays.toString(propNames));
				}
				vmsg = ValidateUtils.validateProperty(principal.getLocale(), bean, propNames);
			} else {
				
				vmsg = ValidateUtils.validate(principal.getLocale(), bean);
			}
			
			Map<String, String> msgmap = new HashMap<String, String>();
			for(ValidateMessage msg: vmsg){
				if(allMode || (!allMode && keys.contains(msg.getProperty()))) {
					msgmap.put(JSON_CASE_BASE.translate(msg.getProperty()), msg.getMessage());
				}
			}
			
			if(msgmap.isEmpty()) {
				result = ActionResult.success("success pass the validation");
			} else {
				result = ActionResult.invalid("fail the validation", msgmap);
			}
			
			this.sendResult(exchange, result);
			
		} catch (Exception e) {
			throw new WebException(e, "excp.parse.json");
		} 
		
	}
	
	/**
	 * Query properties in different table
	 * 
	 * @param id_key the id key
	 * @param id_val the id value
	 * @param trace_code the trace code 
	 * 
	 * @param properties the properties of expected data
	 * 
	 **/
	@WebApi(path="common-prop-query")
	public void handlePropertyQuery(HttpServerExchange exchange)throws BaseException { 
		
		ActionResult result = ActionResult.success("success find properties");
		
		Map<String, Object> paramMap = this.getRequestBody(exchange);
		Principal principal = this.getPrincipal(exchange);
		
		String idKey = Filters.filterString(paramMap, "id_key");
		String traceCd = Filters.filterString(paramMap, "trace_code");
		long id = Filters.filterLong(paramMap, "id_val");
		@SuppressWarnings("unchecked")
		Collection<String> props = (Collection<String>) paramMap.get("properties");
		
		Set<ValidateMessage> vmsg = Sets.newHashSet();
		if(props == null || props.isEmpty()) {
			vmsg.add(new ValidateMessage("properties", "properties is required parameters"));
		}
		
		if(Strings.isNullOrEmpty(traceCd) && id <= 0 && !Strings.isNullOrEmpty(idKey)) {
			vmsg.add(new ValidateMessage("id_val", "id_val is required parameter"));
		}
		
		if(Strings.isNullOrEmpty(traceCd) && Strings.isNullOrEmpty(idKey)) {
			vmsg.add(new ValidateMessage("id_key", "trace_code or id_key is required"));
		}
		
		if(null != vmsg && vmsg.size() > 0){ // fail pass validation
			ServiceException svcexcp = new ServiceException(principal.getLocale(), "excp.validate");
			svcexcp.addValidateMessages(vmsg);
			throw svcexcp;
		}
		
		InfoId infoId = Strings.isNullOrEmpty(traceCd) ? IdKeys.getInfoId(idKey, id) : commonService.queryInfoId(traceCd);
		Map<String, Object> propVal = commonService.queryColumns(infoId, props.toArray(new String[0]));
		
		for(Map.Entry<String, Object> entry : propVal.entrySet()) {
			
			if(entry.getValue() != null && entry.getValue() instanceof Long) {
				
				entry.setValue(((Long)entry.getValue()).toString());
			}
		}
		result.setData(propVal);
		
		this.sendResult(exchange, result);
	}
}
