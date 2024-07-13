package dao.sql.tool;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import com.google.common.io.Closeables;

import dao.sql.tool.BindHooker;
import dao.sql.tool.FileMeta;

/**
 * Load save properties from file and store properties into file.
 * 
 * @author gdiao
 **/
public class PropFileAccessor {

	private Properties properties = new Properties();
	private String propPath = "";
	private BindHooker hooker;
	
	public PropFileAccessor(String propPath) {
		this.propPath = propPath;
	}
	
	public void setHooker(BindHooker hooker) {
		this.hooker = hooker;
	}
	
	public FileMeta getMetaInfo(String tableName) {
		FileMeta meta = new FileMeta();
		String val = properties.getProperty(tableName);
		if(null == val) return null;
		String[] parts = val.split(",");
		meta.tableName = tableName;
		meta.clazzName = parts[0];
		meta.idName = parts[1];
		return meta;
	}
	
	public void putMetaInfo(FileMeta meta) {
		if(null == properties)
			properties = new Properties();
		String key = meta.tableName;
		String value = meta.clazzName + "," + meta.idName;
		properties.put(key, value);
	}
	
	/**
	 * Overwrite the Properties file with the updated Properties object
	 *
	 * @param p_prop
	 * @throws Exception
	 */
	public void writePropertyFile() {
		Map<String, Object> hookMap = new HashMap<>();
		if(null == properties) {
			hookMap.put("message", "No data need to write");
			hooker.callback(BindHooker.WRITE_PROP, hookMap);
			return;
		}
		
		FileOutputStream fos = null;
		try {
			
			fos = new FileOutputStream(propPath);
			properties.store(fos, "Properties file updated");
			hookMap.put("message", "Success write properties to file");
			
		} catch (Exception e) {
			System.err.println("Error in writing Property file:" + propPath);
			e.printStackTrace();
			hookMap.put("message", "Fail write properties to file");
		
		}
		
		hooker.callback(BindHooker.WRITE_PROP, hookMap);
	}

	/**
	 * Read Properties file from the location C:/Src.prop
	 *
	 * @return Properties @exception
	 */
	public Properties readPropertyFile() {
		FileInputStream fis = null;
		Map<String, Object> hookMap = new HashMap<>();
		try {
			fis = new FileInputStream(propPath);
			if (fis != null) {
				properties = new Properties();
				properties.load(fis);
			}
			hookMap.put("message", "Success read properties from file");
			
		} catch (Exception e) {
			System.err.println("Error in reading Property file. Exception Message = " + e.getMessage());
			hookMap.put("message", "Fail read properties from file");
		} finally {
			Closeables.closeQuietly(fis);
		}
		hooker.callback(BindHooker.WRITE_PROP, hookMap);
		return properties;
	}
	
	/**
	 * clear cache 
	 **/
	public void clearProperies() {
		Map<String, Object> hookMap = new HashMap<>();
		if(null != properties) {
			properties.clear();
			hookMap.put("message", "Success clear properties cache");
			hooker.callback(BindHooker.CLEAR_PROP, hookMap);
		}
		File pfile = new File(propPath);
		if(pfile.exists()) {
			pfile.delete();
			hookMap.put("message", "Success delete cache file");
			hooker.callback(BindHooker.CLEAR_PROP, hookMap);
		}
	}
}
