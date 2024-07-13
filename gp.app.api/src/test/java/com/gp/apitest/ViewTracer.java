package com.gp.apitest;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public abstract class ViewTracer {

	public static Map<String, String> paramap = new HashMap<String, String>();
	
	static {
		paramap.put("/ping", "{}");
		
		paramap.put("/demo-calc", "{}");
		
	}
	
	/**
	 * Get the api items 
	 **/
	public static List<String> getApiItems(){
		
		return Arrays.asList(paramap.keySet().toArray(new String[0]));
	}
	
	/**
	 * Format the demo data 
	 **/
	public static String getDemoData(String api) {
		
		String data = paramap.get(api);
		if(null == data) return "";
		
		return formatJSONStr(data, 2);
	}
	
	String host;
	String port;
	String user;
	String pass;
	String client;
	String secret;
	String scope;
	String api;
	String token;
	String audience;
	String request;
	String data;
	String meta;
	String ver;
	String state;
	String rootPath;
	
	abstract void doCallback();
	
	public String getPingUrl() {
		return "http://" + host + ((port!=null&&port.trim().length() > 0)?(":" + port):"") + ((rootPath!=null&&rootPath.trim().length()>0)?("/" + rootPath):"") + "/ping" ;
	}
	
	public String getApiUrl() {
		return "http://" + host + ((port!=null&&port.trim().length() > 0)?(":" + port):"") + ((rootPath!=null&&rootPath.trim().length()>0)?("/" + rootPath):"") + api ;
	}
	
	public String getToken() {
		return "Bearer " + token;
	}
	
    public static String formatJSONStr(final String json_str, final int indent_width) {
		final char[] chars = json_str.toCharArray();
		final String newline = System.lineSeparator();

		String ret = "";
		boolean begin_quotes = false;

		for (int i = 0, indent = 0; i < chars.length; i++) {
			char c = chars[i];

			if (c == '\"') {
				ret += c;
				begin_quotes = !begin_quotes;
				continue;
			}

			if (!begin_quotes) {
				switch (c) {
				case '{':
				case '[':
					ret += c + newline + String.format("%" + (indent += indent_width) + "s", "");
					continue;
				case '}':
				case ']':
					ret += newline + ((indent -= indent_width) > 0 ? String.format("%" + indent + "s", "") : "") + c;
					continue;
				case ':':
					ret += c + " ";
					continue;
				case ',':
					ret += c + newline + (indent > 0 ? String.format("%" + indent + "s", "") : "");
					continue;
				default:
					if (Character.isWhitespace(c))
						continue;
				}
			}

			ret += c + (c == '\\' ? "" + chars[++i] : "");
		}

		return ret;
	}


}
