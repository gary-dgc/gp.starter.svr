package dao.sql.tool.widget;

import java.awt.Component;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JList;
import javax.swing.UIManager;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;

import dao.sql.tool.widget.CheckBoxListItem;

/**
 * A cell renderer for the CheckBoxList
 * 
 * @author Naveed Quadri
 */
class CheckboxCellRenderer extends DefaultListCellRenderer {

	private static final long serialVersionUID = 1L;
	protected static Border emptyBorder = new EmptyBorder(1, 1, 1, 1);

	@Override
	public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected,
			boolean cellHasFocus) {
		if (value instanceof CheckBoxListItem) {

			CheckBoxListItem checkbox = (CheckBoxListItem) value;
			checkbox.setBackground(isSelected ? list.getSelectionBackground() : list.getBackground());
			checkbox.setForeground(isSelected ? list.getSelectionForeground() : list.getForeground());
			checkbox.setEnabled(isEnabled());
			checkbox.setFont(getFont());
			checkbox.setFocusPainted(false);
			checkbox.setBorderPainted(true);
			checkbox.setBorder(isSelected ? UIManager.getBorder("List.focusCellHighlightBorder") : emptyBorder);

			return checkbox;
		} else {
			return super.getListCellRendererComponent(list, value.getClass().getName(), index, isSelected,
					cellHasFocus);
		}
	}

}
