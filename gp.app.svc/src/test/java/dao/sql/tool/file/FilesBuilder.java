package dao.sql.tool.file;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import dao.sql.tool.BindHooker;
import dao.sql.tool.FileMeta;
import freemarker.cache.ClassTemplateLoader;
import freemarker.template.Configuration;

/**
 * File Builder to prepare the necessary steps before generating.
 * 
 * @author gdiao
 **/
public class FilesBuilder {

	public static FilesBuilder BUILDER = new FilesBuilder();
	
	Configuration cfg = new Configuration(Configuration.VERSION_2_3_28);
			
	private FilesBuilder(){
		
		ClassTemplateLoader ctl = new ClassTemplateLoader(getClass(), "/dao/sql/tool/tmpl");
		cfg.setTemplateLoader(ctl);
	}
	
	public String genDaoInfoFile(FileMeta fileMeta, String path, BindHooker hooker) throws IOException {
		Map<String, Object> hookMap = new HashMap<>();
		
		hookMap.put("message", "Generate for table: " + fileMeta.tableName);
		hooker.callback(BindHooker.CREATE_FIL, hookMap);
		
		DaoInfoFile file = new DaoInfoFile(BUILDER.cfg, fileMeta);
		String info = file.generate( path);
		hookMap.put("message", "-- bean: " + info);
		hooker.callback(BindHooker.CREATE_FIL, hookMap);
		
		DaoFile dao = new DaoFile(BUILDER.cfg, fileMeta);
		String dao1 = dao.generate( path);
		hookMap.put("message", "-- dao: " + dao1);
		hooker.callback(BindHooker.CREATE_FIL, hookMap);

		return "";
	}
	
}
