package dao.sql.tool.widget;

import java.awt.BorderLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;

import javax.swing.DefaultListModel;
import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListModel;

import java.awt.Rectangle;
import java.util.ArrayList;
import javax.swing.ListSelectionModel;

import dao.sql.tool.widget.CheckBoxListItem;
import dao.sql.tool.widget.CheckboxCellRenderer;
import dao.sql.tool.widget.JCheckBoxList;

/**
 * An implementation of JCheckboxList, a JList with checkboxes
 * @author Naveed Quadri
 */
public class JCheckBoxList extends JList<CheckBoxListItem> {

	private static final long serialVersionUID = 1L;

	public JCheckBoxList() {
        super();
        setModel(new DefaultListModel<CheckBoxListItem>());
        setCellRenderer(new CheckboxCellRenderer());

        addMouseListener(new MouseAdapter() {

            @Override
            public void mousePressed(MouseEvent e) {
                int index = locationToIndex(e.getPoint());
                Rectangle bounds = getCellBounds(index, index);
                if (index != -1) {
                    Object obj = getModel().getElementAt(index);
                    if (obj instanceof JCheckBox) {
                        JCheckBox checkbox = (JCheckBox) obj;
                        //check if the click is on checkbox (including the label)
                        boolean inCheckbox = getComponentOrientation().isLeftToRight() ? e.getX() < bounds.x + checkbox.getPreferredSize().getWidth() : e.getX() > bounds.x + checkbox.getPreferredSize().getWidth();
                        //change the state of the checkbox on double click or if the click is on checkbox (including the label)
                        if (e.getClickCount() >= 2 || inCheckbox) {
                            checkbox.setSelected(!checkbox.isSelected());
                            fireSelectionValueChanged(index, index, inCheckbox);
                        }
                        repaint();
                    }
                }
            }
        });

        setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
    }

    /**
     * Gets all the indices that are checked.
     * @return Checked Indices
     */
    public int[] getCheckedIndices() {
        List<Integer> list = new ArrayList<Integer>();
        ListModel<CheckBoxListItem> dlm = getModel();
        for (int i = 0; i < dlm.getSize(); ++i) {
            Object obj = getModel().getElementAt(i);
            if (obj instanceof JCheckBox) {
                JCheckBox checkbox = (JCheckBox) obj;
                if (checkbox.isSelected()) {
                    list.add(new Integer(i));
                }
            }
        }

        int[] indexes = new int[list.size()];

        for (int i = 0; i < list.size(); ++i) {
            indexes[i] = ((Integer) list.get(i)).intValue();
        }

        return indexes;
    }

    /**
     * Gets Checked Items
     * @return Checked Items
     */
    public ArrayList<CheckBoxListItem> getCheckedItems() {
        ArrayList<CheckBoxListItem> list = new ArrayList<CheckBoxListItem>();
        ListModel<CheckBoxListItem> dlm =  getModel();
        for (int i = 0; i < dlm.getSize(); ++i) {
        	CheckBoxListItem checkboxListItem = dlm.getElementAt(i);
           
            if (checkboxListItem.isSelected()) {
                list.add(checkboxListItem);
            }
        }
        return list;
    }
   
    public boolean isCheckedIndex(int index) {
    	ListModel<CheckBoxListItem> dlm =  getModel();
    	return dlm.getElementAt(index).isSelected();
    }
    
    public static void main(String args[]) {
        
        JFrame frame = new JFrame("JList CheckBox Example");
        frame.getContentPane().setLayout(new BorderLayout());
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(400, 300);


        JPanel panel = new JPanel();
        JCheckBoxList myList = new JCheckBoxList();

        String[] items = new String[]{"item1", "item2", "item3"};
        DefaultListModel<CheckBoxListItem> model = new DefaultListModel<CheckBoxListItem>();
        for (int i = 0; i < items.length; i++) {
            model.addElement(new CheckBoxListItem(items[i], true));
        }
        myList.setModel(model);
  
        System.out.println("MODEL SIZE:" + (myList.getModel()).getSize());
        panel.add(new JScrollPane(myList));


        frame.getContentPane().add(panel,BorderLayout.CENTER);
        frame.setVisible(true);
    }
}