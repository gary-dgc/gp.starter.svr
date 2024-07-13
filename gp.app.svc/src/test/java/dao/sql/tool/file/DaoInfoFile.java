package dao.sql.tool.file;

import java.io.IOException;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import com.google.common.base.CaseFormat;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import dao.sql.tool.FileMeta;
import freemarker.cache.ClassTemplateLoader;
import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import dao.sql.tool.file.BaseFile;

public class DaoInfoFile extends BaseFile{

	public DaoInfoFile(Configuration cfg, FileMeta fileMeta) {
		super(cfg, fileMeta);
	}

	@Override
	public String generate( String path) throws IOException{

		Template template = cfg.getTemplate("info.ftl");
		
		Map<String, Object> dataMap = new HashMap<String, Object>();
		dataMap.put("package_name", fileMeta.packageName);
		dataMap.put("bean_name", fileMeta.packageName);
		
		StringBuilder infoName = new StringBuilder(); // info class name
		infoName.append(CaseFormat.UPPER_UNDERSCORE.to(CaseFormat.UPPER_CAMEL, fileMeta.clazzName));
		infoName.append("Info");
		dataMap.put("bean_name", infoName.toString());
		
		List<Map<String, Object>> lstColumn = fileMeta.columns;
		List<Map<String, String>> columns = Lists.newArrayList();
		for (Map<String, Object> column : lstColumn) {
			String columnName = (String)column.get("COLUMN_NAME");
			if(columnName.equalsIgnoreCase(fileMeta.idName)||
            		columnName.equalsIgnoreCase("modifier_uid")||
            		columnName.equalsIgnoreCase("modify_time") ||
					columnName.equalsIgnoreCase("del_flag")) {
            	continue; // ignore id and trace fields
            }
			String dataType = (String)column.get("DATA_TYPE");
			String javaType = convertDataTypeToJavaType(dataType);
			if (javaType.equals("Date")) {
				dataMap.put("hasDate", true);
			}

			Map<String, String> propertyMap = Maps.newHashMap();
			propertyMap.put("method_name", CaseFormat.UPPER_UNDERSCORE.to(CaseFormat.UPPER_CAMEL, columnName));
			propertyMap.put("type", convertDataTypeToJavaType(dataType));
			propertyMap.put("property", CaseFormat.UPPER_UNDERSCORE.to(CaseFormat.LOWER_CAMEL, columnName));
			columns.add(propertyMap);
		}
		dataMap.put("columns", columns);
		
		// 创建字符串写入工具
		StringWriter writer = new StringWriter();
		try {
			template.process(dataMap, writer);
		} catch (TemplateException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		writeFile(fileMeta.packageName + ".info", infoName.toString(), writer.toString(), path);
	
		return infoName.toString();
	
	}

}
