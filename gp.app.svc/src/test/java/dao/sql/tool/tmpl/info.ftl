package ${package_name}.info;

import com.gp.info.TraceableInfo;
<#if hasDate??>
import java.util.Date;
</#if>
/**-------------------RESERVE SEPARATOR LINE---------------------*/

public class ${bean_name} extends TraceableInfo {

	private static final long serialVersionUID = 1L;

	<#list columns as column>
	private ${column.type} ${column.property};
	</#list>
	
	<#list columns as column>
	public ${column.type} get${column.method_name}() {
		return this.${column.property};
	}
	public void set${column.method_name}(${column.type} ${column.property}) {
		this.${column.property} = ${column.property};
	}
	
	</#list>
	
	@Override
	public String toString(){
		return "${bean_name} ["
		<#list columns as column>
		+ "${column.property}=" + ${column.property} + ", "
		</#list>
		+ "modifier=" + getModifierUid()
		+ "]";
	}
/**-------------------RESERVE SEPARATOR LINE---------------------*/

}