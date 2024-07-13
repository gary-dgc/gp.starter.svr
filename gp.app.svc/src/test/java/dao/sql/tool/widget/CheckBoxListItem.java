package dao.sql.tool.widget;


import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import javax.swing.JCheckBox;

/**
 * Each item in the CheckBxoList will be an instance of this class
 * @author Naveed Quadri
 */
public class CheckBoxListItem extends JCheckBox {

	private static final long serialVersionUID = 1L;
	private Object value = null;

    public CheckBoxListItem(Object itemValue, boolean selected) {
        super(itemValue == null ? "" : "" + itemValue, selected);
        setValue(itemValue);
        addMouseListener(new MouseAdapter() {

            @Override
            public void mousePressed(MouseEvent e) {
                boolean b = isSelected();
                setSelected(!b);
            }
        });
    }

    @Override
    public boolean isSelected() {
        return super.isSelected();
    }

    @Override
    public void setSelected(boolean selected) {
        super.setSelected(selected);
    }

    public Object getValue() {
        return value;
    }

    /**
     * The value of the JCheckbox label
     * @param value 
     */
    public void setValue(Object value) {
        this.value = value;
    }

    @Override
    public String toString() {
        return value.toString();
    }
}
