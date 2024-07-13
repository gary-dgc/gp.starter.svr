package dao.sql.tool;

import java.util.List;
import java.util.Map;

/**
 * The file meta information
 * 
 * @author gdiao 
 **/
public class FileMeta {

	public String tablePrefix ;
	
	public String tableName;
	
	public String clazzName;
	
	public String idName;
	
	public String packageName;
	
	public List<Map<String, Object>> columns;
	
	public String[] toRowData() {
		
		return new String[]{tableName, clazzName, idName};
	}
}
