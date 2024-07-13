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

public class DaoFile extends BaseFile{

	public DaoFile(Configuration cfg, FileMeta fileMeta) {
		super(cfg, fileMeta);
	}

	@Override
	String generate(String path) throws IOException {

		Template template = cfg.getTemplate("dao.ftl");

		Map<String, Object> dataMap = new HashMap<String, Object>();
		dataMap.put("package_name", fileMeta.packageName);

		StringBuilder infoName = new StringBuilder(); // info class name
		infoName.append(CaseFormat.UPPER_UNDERSCORE.to(CaseFormat.UPPER_CAMEL, fileMeta.clazzName));
		infoName.append("Info");
		dataMap.put("bean_name", infoName.toString());

		StringBuilder interfaceName = new StringBuilder(); // inf class name
		interfaceName.append(CaseFormat.UPPER_UNDERSCORE.to(CaseFormat.UPPER_CAMEL, fileMeta.clazzName));
		interfaceName.append("DAO");
		dataMap.put("dao_name", interfaceName.toString());

		dataMap.put("id_key", fileMeta.clazzName.toUpperCase());
		dataMap.put("id_col", fileMeta.idName);

		List<Map<String, Object>> lstColumn = fileMeta.columns;
		List<Map<String, String>> columns = Lists.newArrayList();
		for (Map<String, Object> column : lstColumn) {
			String columnName = (String) column.get("COLUMN_NAME");
			if (columnName.equalsIgnoreCase(fileMeta.idName)) {
				continue; // ignore id and trace fields
			}
			String dataType = (String) column.get("DATA_TYPE");

			Map<String, String> propertyMap = Maps.newHashMap();
			propertyMap.put("property", CaseFormat.UPPER_UNDERSCORE.to(CaseFormat.UPPER_CAMEL, columnName));
			propertyMap.put("type", convertDataTypeToRsType(dataType));
			propertyMap.put("col_name", columnName);
			columns.add(propertyMap);
		}
		dataMap.put("columns", columns);

		int num = 0;
		List<Map<String, String>> lines = Lists.newArrayList();
		StringBuffer colLine = new StringBuffer();
		int cnt = lstColumn.size() - 1;

		for (Map<String, Object> column : lstColumn) {

			String columnName = (String)column.get( "COLUMN_NAME");
			if(columnName.equalsIgnoreCase(fileMeta.idName)) {
				continue; // ignore id and trace fields
			}
			colLine.append("\"").append(columnName).append("\", ");

			num++;
			if (num % 4 == 0) {
				Map<String, String> lineMap = Maps.newHashMap();
				if(cnt == num) {
					colLine.replace(colLine.lastIndexOf(","), colLine.length(), "");
				}
				lineMap.put("col_line", colLine.toString());

				lines.add(lineMap);

				colLine = new StringBuffer();
			}
		}
		if (num % 4 != 0) {
			Map<String, String> lineMap = Maps.newHashMap();

			colLine.replace(colLine.lastIndexOf(","), colLine.length(), "");

			lineMap.put("col_line", colLine.toString());
			lines.add(lineMap);

		}
		dataMap.put("lines", lines);

		// 创建字符串写入工具
		StringWriter writer = new StringWriter();
		try {
			template.process(dataMap, writer);
		} catch (TemplateException e) {
			e.printStackTrace();
		}
		writeFile(fileMeta.packageName, interfaceName.toString(), writer.toString(), path);
		return interfaceName.toString();
		
	}

}
