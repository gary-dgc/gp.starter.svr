package dao.sql.tool;

import java.util.Map;

/**
 * Interface to hook callbacks from GUI windows. 
 **/
public interface BindHooker {
	
	public static final String TRY_CONN = "TRY_CONN";
	public static final String LOAD_TBLS = "LOAD_TBLS";
	public static final String READ_PROP = "READ_PROP";
	public static final String WRITE_PROP = "WRITE_PROP";
	public static final String CLEAR_PROP = "CLEAR_PROP";
	public static final String CREATE_FIL = "CREATE_FIL";
	/**
	 * Callback methods 
	 **/
	public void callback(String eventType, Map<String, Object> parameters);
}
