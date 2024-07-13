package dao.sql.tool;

import java.awt.EventQueue;

import javax.swing.JFrame;
import java.awt.BorderLayout;
import javax.swing.JPanel;
import javax.swing.JLabel;
import java.awt.Dimension;
import java.awt.GridBagLayout;
import java.awt.GridBagConstraints;
import javax.swing.JTextField;
import java.awt.Insets;
import javax.swing.border.EmptyBorder;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.awt.event.ActionEvent;
import javax.swing.JCheckBox;

import com.google.common.base.Strings;
import com.google.common.collect.Lists;

import dao.sql.tool.file.FilesBuilder;
import dao.sql.tool.widget.CheckBoxListItem;
import dao.sql.tool.widget.CheckedCellEditor;
import dao.sql.tool.widget.JCheckBoxList;
import dao.sql.tool.widget.TableCellListener;
import dao.sql.tool.BindHooker;
import dao.sql.tool.DaoGenerator;
import dao.sql.tool.DataBaseAccessor;
import dao.sql.tool.FileMeta;
import dao.sql.tool.PropFileAccessor;

import javax.swing.JTable;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.DefaultTableModel;
import javax.swing.event.ListSelectionEvent;
import java.awt.Font;
import java.awt.FlowLayout;
import javax.swing.SwingConstants;

/**
 * The Dao File Generator
 * 
 * @author gdiao
 **/
public class DaoGenerator {

	static String VER = "1.0";
	
	private JFrame frmDaoGeneratorV;
	private JTextField textHost;
	private JTextField textPort;
	private JTextField textSchema;
	private JTextField textUser;
	private JTextField textPassword;
	private JTextField textConn;
	private JTextField textPackage;
	private JTextField textTarget;
	private JTextField textStatus;
	private JTextArea textLogArea;
	private JTable tableChecked;
	
	JButton btnTryConn;
	JButton btnLoad;
	JButton btnGenerate;
	JCheckBoxList list;
	JCheckBox chckbxAll;
	
	/**
	 * Launch the application.
	 */
	public static void main(String[] args) {
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				try {
					DaoGenerator window = new DaoGenerator();
					window.frmDaoGeneratorV.setVisible(true);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
	}

	/**
	 * Create the application.
	 */
	public DaoGenerator() {
		initialize();
		initialOthers();
	}

	/**
	 * Initialize the contents of the frame.
	 */
	private void initialize() {
		frmDaoGeneratorV = new JFrame();
		frmDaoGeneratorV.setTitle("DAO GENERATOR V0.1");
		//frame.getContentPane().setPreferredSize(new Dimension(600, 400));
		//frame.setPreferredSize(new Dimension(450, 400));
		frmDaoGeneratorV.setMinimumSize(new Dimension(840, 600));
		frmDaoGeneratorV.setBounds(100, 100, 1099, 622);
		frmDaoGeneratorV.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frmDaoGeneratorV.getContentPane().setLayout(new BorderLayout(0, 0));
		
		JPanel topPanel = new JPanel();
		topPanel.setBorder(new EmptyBorder(5, 5, 0, 5));
		frmDaoGeneratorV.getContentPane().add(topPanel, BorderLayout.NORTH);
		GridBagLayout gbl_topPanel = new GridBagLayout();
		gbl_topPanel.columnWidths = new int[] {0, 141, 72, 0, 75, 189, 0, 4};
		gbl_topPanel.rowHeights = new int[] {30, 0, 0, 0};
		gbl_topPanel.columnWeights = new double[]{0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 1.0, Double.MIN_VALUE};
		gbl_topPanel.rowWeights = new double[]{1.0, 1.0, 0.0, 0.0};
		topPanel.setLayout(gbl_topPanel);
		
		JLabel lblNewLabel_1 = new JLabel("HOST:");
		GridBagConstraints gbc_lblNewLabel_1 = new GridBagConstraints();
		gbc_lblNewLabel_1.anchor = GridBagConstraints.EAST;
		gbc_lblNewLabel_1.insets = new Insets(0, 0, 5, 5);
		gbc_lblNewLabel_1.gridx = 0;
		gbc_lblNewLabel_1.gridy = 0;
		topPanel.add(lblNewLabel_1, gbc_lblNewLabel_1);
		
		textHost = new JTextField();
		textHost.setPreferredSize(new Dimension(4, 22));
		GridBagConstraints gbc_textHost = new GridBagConstraints();
		gbc_textHost.fill = GridBagConstraints.BOTH;
		gbc_textHost.insets = new Insets(0, 0, 5, 5);
		gbc_textHost.gridx = 1;
		gbc_textHost.gridy = 0;
		topPanel.add(textHost, gbc_textHost);
		textHost.setColumns(10);
		
		JLabel lblNewLabel_2 = new JLabel("PORT:");
		GridBagConstraints gbc_lblNewLabel_2 = new GridBagConstraints();
		gbc_lblNewLabel_2.anchor = GridBagConstraints.EAST;
		gbc_lblNewLabel_2.insets = new Insets(0, 0, 5, 5);
		gbc_lblNewLabel_2.gridx = 2;
		gbc_lblNewLabel_2.gridy = 0;
		topPanel.add(lblNewLabel_2, gbc_lblNewLabel_2);
		
		textPort = new JTextField();
		textPort.setPreferredSize(new Dimension(4, 22));
		GridBagConstraints gbc_textPort = new GridBagConstraints();
		gbc_textPort.fill = GridBagConstraints.VERTICAL;
		gbc_textPort.anchor = GridBagConstraints.WEST;
		gbc_textPort.insets = new Insets(0, 0, 5, 5);
		gbc_textPort.gridx = 3;
		gbc_textPort.gridy = 0;
		topPanel.add(textPort, gbc_textPort);
		textPort.setColumns(10);
		
		JLabel lblNewLabel_3 = new JLabel("SCHEMA:");
		GridBagConstraints gbc_lblNewLabel_3 = new GridBagConstraints();
		gbc_lblNewLabel_3.insets = new Insets(0, 0, 5, 5);
		gbc_lblNewLabel_3.anchor = GridBagConstraints.EAST;
		gbc_lblNewLabel_3.gridx = 4;
		gbc_lblNewLabel_3.gridy = 0;
		topPanel.add(lblNewLabel_3, gbc_lblNewLabel_3);
		
		textSchema = new JTextField();
		textSchema.setPreferredSize(new Dimension(4, 22));
		GridBagConstraints gbc_textSchema = new GridBagConstraints();
		gbc_textSchema.fill = GridBagConstraints.VERTICAL;
		gbc_textSchema.anchor = GridBagConstraints.WEST;
		gbc_textSchema.insets = new Insets(0, 0, 5, 5);
		gbc_textSchema.gridx = 5;
		gbc_textSchema.gridy = 0;
		topPanel.add(textSchema, gbc_textSchema);
		textSchema.setColumns(10);
		
		JPanel panel_3 = new JPanel();
		FlowLayout flowLayout = (FlowLayout) panel_3.getLayout();
		flowLayout.setAlignment(FlowLayout.LEFT);
		flowLayout.setVgap(0);
		panel_3.setBorder(new EmptyBorder(0, 0, 0, 0));
		GridBagConstraints gbc_panel_3 = new GridBagConstraints();
		gbc_panel_3.insets = new Insets(0, 0, 5, 0);
		gbc_panel_3.fill = GridBagConstraints.BOTH;
		gbc_panel_3.gridx = 6;
		gbc_panel_3.gridy = 0;
		topPanel.add(panel_3, gbc_panel_3);
		
		btnTryConn = new JButton("Try Connect");
		btnTryConn.setHorizontalAlignment(SwingConstants.LEFT);
		panel_3.add(btnTryConn);
		
		JButton btnVersion = new JButton("Version");
		btnVersion.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				printVersion();
			}
		});
		panel_3.add(btnVersion);
		btnTryConn.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				tryConn(e);
			}
		});
		
		JLabel lblPrefix = new JLabel("PREFIX:");
		GridBagConstraints gbc_lblPrefix = new GridBagConstraints();
		gbc_lblPrefix.anchor = GridBagConstraints.EAST;
		gbc_lblPrefix.insets = new Insets(0, 0, 5, 5);
		gbc_lblPrefix.gridx = 4;
		gbc_lblPrefix.gridy = 1;
		topPanel.add(lblPrefix, gbc_lblPrefix);
		
		textPrefix = new JTextField();
		GridBagConstraints gbc_textPrefix = new GridBagConstraints();
		gbc_textPrefix.anchor = GridBagConstraints.WEST;
		gbc_textPrefix.insets = new Insets(0, 0, 5, 5);
		gbc_textPrefix.gridx = 5;
		gbc_textPrefix.gridy = 1;
		topPanel.add(textPrefix, gbc_textPrefix);
		textPrefix.setColumns(10);
		
		JPanel panel_4 = new JPanel();
		FlowLayout flowLayout_1 = (FlowLayout) panel_4.getLayout();
		flowLayout_1.setAlignment(FlowLayout.LEFT);
		flowLayout_1.setVgap(0);
		panel_4.setBorder(new EmptyBorder(0, 0, 0, 0));
		GridBagConstraints gbc_panel_4 = new GridBagConstraints();
		gbc_panel_4.insets = new Insets(0, 0, 5, 0);
		gbc_panel_4.fill = GridBagConstraints.BOTH;
		gbc_panel_4.gridx = 6;
		gbc_panel_4.gridy = 1;
		topPanel.add(panel_4, gbc_panel_4);
		
		btnLoad = new JButton("Load");
		panel_4.add(btnLoad);
		btnLoad.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				loadTables();
			}
		});
		btnLoad.setEnabled(false);
		
		JButton btnClearCache = new JButton("Clear Cache");
		btnClearCache.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				clearCache();
			}
		});
		panel_4.add(btnClearCache);
		
		JLabel lblNewLabel_4 = new JLabel("USER:");
		GridBagConstraints gbc_lblNewLabel_4 = new GridBagConstraints();
		gbc_lblNewLabel_4.anchor = GridBagConstraints.EAST;
		gbc_lblNewLabel_4.insets = new Insets(0, 0, 5, 5);
		gbc_lblNewLabel_4.gridx = 0;
		gbc_lblNewLabel_4.gridy = 1;
		topPanel.add(lblNewLabel_4, gbc_lblNewLabel_4);
		
		textUser = new JTextField();
		textUser.setPreferredSize(new Dimension(4, 22));
		GridBagConstraints gbc_textUser = new GridBagConstraints();
		gbc_textUser.fill = GridBagConstraints.VERTICAL;
		gbc_textUser.anchor = GridBagConstraints.WEST;
		gbc_textUser.insets = new Insets(0, 0, 5, 5);
		gbc_textUser.gridx = 1;
		gbc_textUser.gridy = 1;
		topPanel.add(textUser, gbc_textUser);
		textUser.setColumns(10);
		
		JLabel lblNewLabel_5 = new JLabel("PASSWORD:");
		GridBagConstraints gbc_lblNewLabel_5 = new GridBagConstraints();
		gbc_lblNewLabel_5.anchor = GridBagConstraints.EAST;
		gbc_lblNewLabel_5.insets = new Insets(0, 0, 5, 5);
		gbc_lblNewLabel_5.gridx = 2;
		gbc_lblNewLabel_5.gridy = 1;
		topPanel.add(lblNewLabel_5, gbc_lblNewLabel_5);
		
		textPassword = new JTextField();
		textPassword.setPreferredSize(new Dimension(4, 22));
		GridBagConstraints gbc_textPassword = new GridBagConstraints();
		gbc_textPassword.fill = GridBagConstraints.VERTICAL;
		gbc_textPassword.anchor = GridBagConstraints.WEST;
		gbc_textPassword.insets = new Insets(0, 0, 5, 5);
		gbc_textPassword.gridx = 3;
		gbc_textPassword.gridy = 1;
		topPanel.add(textPassword, gbc_textPassword);
		textPassword.setColumns(10);
		
		JLabel lblNewLabel_6 = new JLabel("CONN:");
		GridBagConstraints gbc_lblNewLabel_6 = new GridBagConstraints();
		gbc_lblNewLabel_6.anchor = GridBagConstraints.EAST;
		gbc_lblNewLabel_6.insets = new Insets(0, 0, 5, 5);
		gbc_lblNewLabel_6.gridx = 0;
		gbc_lblNewLabel_6.gridy = 2;
		topPanel.add(lblNewLabel_6, gbc_lblNewLabel_6);
		
		textConn = new JTextField();
		textConn.setPreferredSize(new Dimension(4, 22));
		textConn.setEditable(false);
		GridBagConstraints gbc_textConn = new GridBagConstraints();
		gbc_textConn.gridwidth = 6;
		gbc_textConn.insets = new Insets(0, 0, 5, 0);
		gbc_textConn.fill = GridBagConstraints.BOTH;
		gbc_textConn.gridx = 1;
		gbc_textConn.gridy = 2;
		topPanel.add(textConn, gbc_textConn);
		textConn.setColumns(10);
		
		JLabel lblNewLabel_7 = new JLabel("PACKAGE:");
		GridBagConstraints gbc_lblNewLabel_7 = new GridBagConstraints();
		gbc_lblNewLabel_7.anchor = GridBagConstraints.EAST;
		gbc_lblNewLabel_7.insets = new Insets(0, 0, 0, 5);
		gbc_lblNewLabel_7.gridx = 0;
		gbc_lblNewLabel_7.gridy = 3;
		topPanel.add(lblNewLabel_7, gbc_lblNewLabel_7);
		
		textPackage = new JTextField();
		textPackage.setPreferredSize(new Dimension(4, 22));
		GridBagConstraints gbc_textPackage = new GridBagConstraints();
		gbc_textPackage.gridwidth = 2;
		gbc_textPackage.insets = new Insets(0, 0, 0, 5);
		gbc_textPackage.fill = GridBagConstraints.BOTH;
		gbc_textPackage.gridx = 1;
		gbc_textPackage.gridy = 3;
		topPanel.add(textPackage, gbc_textPackage);
		textPackage.setColumns(10);
		
		JLabel lblNewLabel_8 = new JLabel("TARGET:");
		GridBagConstraints gbc_lblNewLabel_8 = new GridBagConstraints();
		gbc_lblNewLabel_8.anchor = GridBagConstraints.EAST;
		gbc_lblNewLabel_8.insets = new Insets(0, 0, 0, 5);
		gbc_lblNewLabel_8.gridx = 3;
		gbc_lblNewLabel_8.gridy = 3;
		topPanel.add(lblNewLabel_8, gbc_lblNewLabel_8);
		
		textTarget = new JTextField();
		textTarget.setPreferredSize(new Dimension(4, 22));
		GridBagConstraints gbc_textTarget = new GridBagConstraints();
		gbc_textTarget.gridwidth = 2;
		gbc_textTarget.insets = new Insets(0, 0, 0, 5);
		gbc_textTarget.fill = GridBagConstraints.BOTH;
		gbc_textTarget.gridx = 4;
		gbc_textTarget.gridy = 3;
		topPanel.add(textTarget, gbc_textTarget);
		textTarget.setColumns(10);
		
		btnGenerate = new JButton("Generate");
		btnGenerate.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				generateFiles();
			}
		});
		btnGenerate.setEnabled(false);
		GridBagConstraints gbc_btnGenerate = new GridBagConstraints();
		gbc_btnGenerate.anchor = GridBagConstraints.WEST;
		gbc_btnGenerate.gridx = 6;
		gbc_btnGenerate.gridy = 3;
		topPanel.add(btnGenerate, gbc_btnGenerate);
		
		JPanel leftPanel = new JPanel();
		leftPanel.setPreferredSize(new Dimension(300, 10));
		leftPanel.setMinimumSize(new Dimension(300, 10));
		leftPanel.setBorder(new EmptyBorder(5, 5, 0, 5));
		frmDaoGeneratorV.getContentPane().add(leftPanel, BorderLayout.WEST);
		leftPanel.setLayout(new BorderLayout(5, 5));
		
		JScrollPane scrollPane = new JScrollPane();
		leftPanel.add(scrollPane, BorderLayout.CENTER);
		
		list = new JCheckBoxList();
		list.addListSelectionListener(new ListSelectionListener() {
			public void valueChanged(ListSelectionEvent e) {
				
				if(e.getFirstIndex() == e.getLastIndex() && e.getValueIsAdjusting()) {
					
					onCheckTable(e.getFirstIndex(), list.isCheckedIndex(e.getFirstIndex()));
				}
			}
		});
		scrollPane.setViewportView(list);
		
		JPanel panel_1 = new JPanel();
		panel_1.setPreferredSize(new Dimension(10, 29));
		panel_1.setBorder(new EmptyBorder(0, 0, 0, 0));
		leftPanel.add(panel_1, BorderLayout.NORTH);
		panel_1.setLayout(new BorderLayout(0, 0));
		
		JLabel lblNewLabel_9 = new JLabel("TABLES:");
		panel_1.add(lblNewLabel_9, BorderLayout.WEST);
		
		chckbxAll = new JCheckBox("ALL");
		chckbxAll.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				
				checkAll(((JCheckBox)e.getSource()).isSelected());
			}
		});
		panel_1.add(chckbxAll, BorderLayout.EAST);
		
		JPanel bottomPanel = new JPanel();
		bottomPanel.setPreferredSize(new Dimension(10, 35));
		bottomPanel.setBorder(new EmptyBorder(5, 5, 5, 5));
		frmDaoGeneratorV.getContentPane().add(bottomPanel, BorderLayout.SOUTH);
		bottomPanel.setLayout(new BorderLayout(0, 0));
		
		textStatus = new JTextField();
		textStatus.setEditable(false);
		bottomPanel.add(textStatus, BorderLayout.CENTER);
		textStatus.setColumns(10);
		
		JPanel rightPanel = new JPanel();
		rightPanel.setPreferredSize(new Dimension(300, 10));
		rightPanel.setBorder(new EmptyBorder(5, 0, 0, 5));
		frmDaoGeneratorV.getContentPane().add(rightPanel, BorderLayout.EAST);
		rightPanel.setLayout(new BorderLayout(5, 5));
		
		JScrollPane scrollPane_1 = new JScrollPane();
		rightPanel.add(scrollPane_1, BorderLayout.CENTER);
		
		textLogArea = new JTextArea();
		textLogArea.setBorder(new EmptyBorder(0, 0, 0, 0));
		scrollPane_1.setViewportView(textLogArea);
		
		JPanel panel = new JPanel();
		panel.setPreferredSize(new Dimension(10, 29));
		panel.setBorder(new EmptyBorder(0, 0, 0, 0));
		rightPanel.add(panel, BorderLayout.NORTH);
		panel.setLayout(new BorderLayout(0, 0));
		
		JLabel lblNewLabel = new JLabel("PROCESS LOG:");
		panel.add(lblNewLabel, BorderLayout.WEST);
		
		JButton btnClear = new JButton("Clear");
		btnClear.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				textLogArea.setText("");
			}
		});
		panel.add(btnClear, BorderLayout.EAST);
		
		JPanel centerPanel = new JPanel();
		centerPanel.setBorder(new EmptyBorder(5, 0, 0, 5));
		frmDaoGeneratorV.getContentPane().add(centerPanel, BorderLayout.CENTER);
		centerPanel.setLayout(new BorderLayout(0, 5));
		
		JPanel panel_2 = new JPanel();
		panel_2.setBorder(new EmptyBorder(0, 0, 0, 0));
		panel_2.setPreferredSize(new Dimension(10, 29));
		centerPanel.add(panel_2, BorderLayout.NORTH);
		panel_2.setLayout(new BorderLayout(0, 0));
		
		JLabel lblNewLabel_10 = new JLabel("CHECKED:");
		panel_2.add(lblNewLabel_10, BorderLayout.WEST);
		
		JPanel panel_5 = new JPanel();
		FlowLayout flowLayout_2 = (FlowLayout) panel_5.getLayout();
		flowLayout_2.setVgap(0);
		panel_5.setBorder(new EmptyBorder(0, 0, 0, 0));
		panel_2.add(panel_5, BorderLayout.EAST);
		
		JButton btnClearSelected = new JButton("Clear");
		btnClearSelected.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				checkAll(false);
			}
		});
		panel_5.add(btnClearSelected);
		
		JButton btnSave = new JButton("Save");
		panel_5.add(btnSave);
		btnSave.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				saveFileMetasToFile();
			}
		});
		
		JScrollPane scrollPane_2 = new JScrollPane();
		centerPanel.add(scrollPane_2, BorderLayout.CENTER);
		
		DefaultTableModel model = new DefaultTableModel(new Object[][] {}, new Object[] { "TBL", "CLASS", "ID" });
		tableChecked = new JTable(model);
		tableChecked.setFont(new Font("Dialog", Font.PLAIN, 12));
		scrollPane_2.setViewportView(tableChecked);
	}

	//----------------------------- Customize ---------------------------
	private void initialOthers() {
		frmDaoGeneratorV.addComponentListener(new ComponentAdapter() {
			public void componentResized(ComponentEvent evt) {
				Dimension size = frmDaoGeneratorV.getSize();
				Dimension min = frmDaoGeneratorV.getMinimumSize();
				if (size.getWidth() < min.getWidth()) {
					frmDaoGeneratorV.setSize((int) min.getWidth(), (int) size.getHeight());
				}
				if (size.getHeight() < min.getHeight()) {
					frmDaoGeneratorV.setSize((int) size.getWidth(), (int) min.getHeight());
				}
			}
		});
		//
		this.textHost.setText("localhost");
		this.textPort.setText("3306");
		this.textSchema.setText("gpress_master");
		this.textUser.setText("gpadmin");
		this.textPrefix.setText("gp_");
		this.textPassword.setText("gpress");
		this.textPackage.setText("com.gp.dao");
		this.textTarget.setText(System.getProperty("user.dir"));
		
		Action action = new AbstractAction()
		{
			private static final long serialVersionUID = 1L;

			public void actionPerformed(ActionEvent e)
		    {
		        TableCellListener tcl = (TableCellListener)e.getSource();
//		        System.out.println("Row   : " + tcl.getRow());
//		        System.out.println("Column: " + tcl.getColumn());
//		        System.out.println("Old   : " + tcl.getOldValue());
//		        System.out.println("New   : " + tcl.getNewValue());
		        onMetaCellChange(tcl.getRow(), tcl.getColumn(), (String)tcl.getNewValue());
		    }
		};
		tableChecked.setDefaultEditor(Object.class, new CheckedCellEditor());
		new TableCellListener(tableChecked, action);
	}
	
	FilesBuilder filesBuilder = FilesBuilder.BUILDER;
	DataBaseAccessor dbAccessor = null;
	List<String> tables = null; // tables in check-box list
	List<FileMeta> checkedFiles = Lists.newArrayList(); // checked tables
	PropFileAccessor propertyAccessor = null; // property pair accessor
	
	/**
	 * Hooker to receive the callback invoke. 
	 **/
	BindHooker hooker = new BindHooker() {
		@SuppressWarnings("unchecked")
		@Override
		public void callback(String eventType, Map<String, Object> parameters) {
			String message = (String)parameters.get("message");
			textLogArea.append(message);
			textLogArea.append("\n");
			if(BindHooker.TRY_CONN.equals(eventType)) {
				
				if(parameters.containsKey("conn")) {
					textStatus.setText("Success connect to the database");
					btnLoad.setEnabled(true);
				}else {
					dbAccessor = null; // reset to null
				}
				textConn.setText((String)parameters.get("url"));
			}
			if(BindHooker.LOAD_TBLS.equals(eventType)) {
				tables = (List<String>) parameters.get("tables");
				if(tables != null && tables.size() > 0) {
					DefaultListModel<CheckBoxListItem> model = new DefaultListModel<CheckBoxListItem>();
			        for (String table : tables) {
			            model.addElement(new CheckBoxListItem(table, false));
			        }
			        list.setModel(model);
			        btnGenerate.setEnabled(true);
				}else {
					textLogArea.append("OMG, you load none tables.");
				}
				
				propertyAccessor = new PropFileAccessor(System.getProperty("user.dir") + File.separatorChar + "dao_gen.properties");
				propertyAccessor.setHooker(this);
				propertyAccessor.readPropertyFile();
			}
		}
	};
	private JTextField textPrefix;
	
	private void tryConn(ActionEvent e) {
		dbAccessor = new DataBaseAccessor(
				this.textHost.getText(),
				this.textPort.getText(),
				this.textSchema.getText(),
				this.textUser.getText(),
				this.textPassword.getText()
		);
		
		dbAccessor.setHooker(this.hooker);
		dbAccessor.tryConn();
	}
	
	private void loadTables() {
		dbAccessor.loadTables();
	}

	/**
	 * check all tables or not 
	 **/
	private void checkAll(boolean shouldCheck) {
		if(tables != null && tables.size() > 0) {
			DefaultListModel<CheckBoxListItem> model = new DefaultListModel<CheckBoxListItem>();
	        for (String table : tables) {
	            model.addElement(new CheckBoxListItem(table, shouldCheck));
	        }
	        list.setModel(model);
		}else {
			return;
		}
		if(!shouldCheck && this.chckbxAll.isSelected()) {
			this.chckbxAll.setSelected(false);
		}
		// clear all the file metas
		checkedFiles.clear();
		if(shouldCheck) {
			for (String table : tables) {
				FileMeta meta = new FileMeta();
				meta.tableName = table;
				meta.clazzName = table;
				if(!Strings.isNullOrEmpty(this.textPrefix.getText())) {
					meta.clazzName = table.replace(this.textPrefix.getText(), "");
				}
				String pkey = this.dbAccessor.findPrimaryKey(table);
				meta.idName = Strings.isNullOrEmpty(pkey) ? "id" : pkey;
				
				checkedFiles.add(meta);
	        }
		}
		// repaint table
		refreshTableChecked();
	}
	
	/**
	 * When checked tables change, refresh the table 
	 **/
	private void refreshTableChecked() {
				
		Object[][] data = new Object[checkedFiles.size()][3];
		for(int i = 0; i < checkedFiles.size(); i ++) {
			data[i] = checkedFiles.get(i).toRowData();
			propertyAccessor.putMetaInfo(checkedFiles.get(i));
		}
		
		DefaultTableModel model = new DefaultTableModel(data, new Object[] { "TBL", "CLASS", "ID" });
		tableChecked.setModel(model);
	}
	
	/**
	 * On one table is checked in left list 
	 **/
	private void onCheckTable(int index, boolean selected) {
		
		String table = tables.get(index);
		if(selected) {
			
			FileMeta meta = propertyAccessor.getMetaInfo(table);
			if(null == meta) {
				meta = new FileMeta();
				meta.tableName = table;
				meta.clazzName = table;
				if(!Strings.isNullOrEmpty(this.textPrefix.getText())) {
					meta.clazzName = table.replace(this.textPrefix.getText(), "");
				}
				String pkey = this.dbAccessor.findPrimaryKey(table);
				meta.idName = Strings.isNullOrEmpty(pkey) ? "id" : pkey;
			}
			checkedFiles.add(meta);
		}else {
			// remove from last to first.
			for(int j = checkedFiles.size() - 1; j >=0 ;j--) {
				if(table.equals(checkedFiles.get(j).tableName))
					checkedFiles.remove(j);
			}
		}
		
		refreshTableChecked();
	}
	
	private void onMetaCellChange(int row, int column, String cellVal) {
		FileMeta meta = checkedFiles.get(row);
		if(column == 1) meta.clazzName = cellVal;
		if(column == 2) meta.idName = cellVal;
		propertyAccessor.putMetaInfo(meta);
	}
	
	private void generateFiles() {
		int[] checks = list.getCheckedIndices();
		List<String> tbls = Lists.newArrayList();
		for(int i = 0; i < checks.length; i++) {
			tbls.add(tables.get(checks[i]));
			
		}
		System.out.println(tbls.toString());
		try {
			for(FileMeta meta : checkedFiles) {
				List<Map<String, Object>> cols = dbAccessor.findColumnList(meta.tableName);
				meta.columns = cols;
				meta.packageName = this.textPackage.getText();
				filesBuilder.genDaoInfoFile(meta, this.textTarget.getText(), this.hooker);
			}
		} catch (IOException e) {

			e.printStackTrace();
			this.textLogArea.append("Faile to generate the file. \n");
			this.textLogArea.append(e.getMessage() +" \n");
		}
	}
	
	private void saveFileMetasToFile() {
		
		for(FileMeta meta: checkedFiles) {
			propertyAccessor.putMetaInfo(meta);
		}
		
		propertyAccessor.writePropertyFile();
	}
	
	private void printVersion() {
		
		this.textLogArea.append("Author: Gary Diao \n");
		this.textLogArea.append("Version: " + VER + "\n");
		this.textLogArea.append("Release: 2018-02-01 \n");
	}
	
	/**
	 * clear property cache 
	 **/
	private void clearCache() {
		if(propertyAccessor != null)
		propertyAccessor.clearProperies();
	}
}
