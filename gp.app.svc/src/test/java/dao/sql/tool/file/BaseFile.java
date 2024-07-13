package dao.sql.tool.file;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Map;

import com.google.common.base.CaseFormat;
import com.google.common.base.Strings;
import com.google.common.collect.Maps;

import dao.sql.tool.FileMeta;
import freemarker.template.Configuration;

/**
 * Base class for different source files
 * 
 *  @author gdiao
 **/
public abstract class BaseFile {

    static final String TAB = "\t";
    static final String ENTER = "\n";
    
    static final String RESERVE_SEPARATOR = "/**-------------------RESERVE SEPARATOR LINE---------------------*/";
    
    FileMeta fileMeta;
    
    Configuration cfg;
    
    public BaseFile(Configuration cfg, FileMeta fileMeta) {
    	this.cfg = cfg;
    	this.fileMeta = fileMeta;
    	initial();
    }
    
    /**
     * @return the name of java class 
     **/
    abstract String generate(String path) throws IOException;

    Map<String, String> dbToJavaType = Maps.newHashMap();
    Map<String, String> dbToMethodType = Maps.newHashMap();
    
    public void initial() {
    	dbToJavaType.put("BIT", "Boolean");
    	dbToJavaType.put("BOOL","Boolean");
    	dbToJavaType.put("BOOLEAN","Boolean");
    	dbToJavaType.put("TINYINT","Boolean");
    	dbToJavaType.put("SMALLINT","Integer");
    	dbToJavaType.put("MEDIUMINT","Integer");
    	dbToJavaType.put("INT","Integer");
    	dbToJavaType.put("INTEGER","Integer");
    	dbToJavaType.put("BIGINT","Long");
    	dbToJavaType.put("FLOAT","Float");
    	dbToJavaType.put("DOUBLE","Double");
    	dbToJavaType.put("DECIMAL","Double");
    	dbToJavaType.put("NUMERIC","Double");
    	dbToJavaType.put("DATE","Date");
    	dbToJavaType.put("DATETIME","Date");
    	dbToJavaType.put("TIMESTAMP","Date");
    	dbToJavaType.put("TIME","Date");
    	dbToJavaType.put("CHAR","String");
    	dbToJavaType.put("VARCHAR","String");
    	dbToJavaType.put("TINYTEXT","String");
    	dbToJavaType.put("TEXT","String");
    	dbToJavaType.put("MEDIUMTEXT","String");
    	dbToJavaType.put("LONGTEXT","String");
    	dbToJavaType.put("ENUM","String");
    	dbToJavaType.put("SET","String");
    	dbToJavaType.put("BINARY","byte[]");
    	dbToJavaType.put("VARBINARY","byte[]");
    	dbToJavaType.put("TINYBLOB","byte[]");
    	dbToJavaType.put("BLOB","byte[]");
    	dbToJavaType.put("MEDIUMBLOB","byte[]");
    	dbToJavaType.put("LONGBLOB","byte[]");
    	
    	dbToMethodType.put("BIT","Boolean");
        dbToMethodType.put("BOOL","Boolean");
        dbToMethodType.put("BOOLEAN","Boolean");
        dbToMethodType.put("TINYINT","Boolean");
        dbToMethodType.put("SMALLINT","Int");
        dbToMethodType.put("MEDIUMINT","Int");
        dbToMethodType.put("INT","Int");
        dbToMethodType.put("INTEGER","Int");
        dbToMethodType.put("BIGINT","Long");
        dbToMethodType.put("FLOAT","Float");
        dbToMethodType.put("DOUBLE","Double");
        dbToMethodType.put("DECIMAL","Double");
        dbToMethodType.put("NUMERIC","Double");
        dbToMethodType.put("DATE", "Date");
        dbToMethodType.put("DATETIME","Timestamp");
        dbToMethodType.put("TIMESTAMP","Timestamp");
        dbToMethodType.put("TIME","Timestamp");
        dbToMethodType.put("CHAR","String");
        dbToMethodType.put("VARCHAR","String");
        dbToMethodType.put("TINYTEXT","String");
        dbToMethodType.put("TEXT","String");
        dbToMethodType.put("MEDIUMTEXT","String");
        dbToMethodType.put("LONGTEXT","String");
        dbToMethodType.put("ENUM","String");
        dbToMethodType.put("SET","String");
        dbToMethodType.put("BINARY","byte[]");
        dbToMethodType.put("VARBINARY","byte[]");
        dbToMethodType.put("TINYBLOB","byte[]");
        dbToMethodType.put("BLOB","byte[]");
        dbToMethodType.put("MEDIUMBLOB","byte[]");
        dbToMethodType.put("LONGBLOB","byte[]");
    }
    
    String convertDataTypeToJavaType(String dataType) {
        String javaType = dbToJavaType.get(dataType.toUpperCase());
        
        return javaType;
    }

    /**
     * DataTyp to ResultSet方法类型
     *
     * @param dataType
     * @return
     */
    String convertDataTypeToRsType(String dataType) {
        String javaType = dbToMethodType.get(dataType.toUpperCase());
        
        return javaType;
    }
    
    /**
     * Get the getter method name 
     **/
    String getterMethod(String columnName) {
        return "get" + CaseFormat.UPPER_UNDERSCORE.to(CaseFormat.UPPER_CAMEL, columnName);
    }

    /**
     * Get the setter method name 
     **/
    String setterMethod(String columnName) {
        return "set" + CaseFormat.UPPER_UNDERSCORE.to(CaseFormat.UPPER_CAMEL, columnName);
    }
    
    /**
     * Write the class content info a file 
     **/
    void writeFile(String packageName, String className, String fileInfo, String path) throws IOException {
        if (Strings.isNullOrEmpty(path)) {
            path = System.getProperty("user.dir");
        }
        System.out.println(path);
        path += File.separator + "src" + File.separator + "test" + File.separator + "java" + File.separator;
        path += packageName.replace(".", File.separator);
        String filePath = path + File.separator + className + ".java";

        File dir = new File(path);
        if (!dir.exists()) {
            if (!dir.mkdirs()) {
                throw new RuntimeException("创建文件路径失败!");
            }
        }

        try (FileOutputStream fos = new FileOutputStream(filePath); 
        		BufferedOutputStream bos = new BufferedOutputStream(fos)) {
            bos.write(fileInfo.getBytes());
            bos.flush();
        }
    }
    
}
