package dao.sql.tool.widget;

import java.awt.Component;
import java.awt.Font;

import javax.swing.DefaultCellEditor;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.SwingConstants;

public class CheckedCellEditor extends DefaultCellEditor {
	
	private static final long serialVersionUID = 1L;

	public CheckedCellEditor() {
		super(new JTextField());
	}

	public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
		
		JTextField editor = (JTextField) super.getTableCellEditorComponent(table, value, isSelected, row, column);

		if (value != null)
			editor.setText(value.toString());
		if (column == 0) {
//			editor.setHorizontalAlignment(SwingConstants.CENTER);
//			editor.setFont(new Font("Serif", Font.BOLD, 12));
			return null;
		} else {
			editor.setHorizontalAlignment(SwingConstants.LEFT);
			editor.setFont(new Font("Serif", Font.BOLD, 12));
		}
		return editor;
	}
}
