package com.gp.apitest;

import java.awt.EventQueue;

import javax.swing.JFrame;
import java.awt.BorderLayout;
import javax.swing.JPanel;
import javax.swing.JSplitPane;
import javax.swing.JLabel;
import java.awt.GridBagLayout;
import java.awt.GridBagConstraints;
import javax.swing.JRadioButton;
import java.awt.FlowLayout;
import javax.swing.ButtonGroup;
import javax.swing.JTextField;
import javax.swing.border.EmptyBorder;
import javax.swing.JButton;
import java.awt.Dimension;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import java.awt.Insets;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.ActionEvent;
import javax.swing.SwingConstants;

public class GPressApiTester {

	private JFrame frmGroupressV;
	private final ButtonGroup profileGroup = new ButtonGroup();
	private JTextField txtHost;
	private JTextField txtPort;
	private JTextField txtUser;
	private JTextField txtPass;
	private JTextField txtAudience;
	private JTextField txtToken;
	private JTextArea txtPost;
	private JTextArea txtResult ;
	private JTextField txtConext;
	private FilterComboBox comboApi;
	private JLabel lblNewLabel_11;
	private JLabel lblStatus ;
	private JTextField txtClient;
	private JTextField txtSecret;
	private JTextField txtScope;
	private JTextArea txtMeta ;
	/**
	 * Launch the application.
	 */
	public static void main(String[] args) {
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				try {
					GPressApiTester window = new GPressApiTester();
					window.frmGroupressV.setVisible(true);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
	}

	/**
	 * Create the application.
	 */
	public GPressApiTester() {
		initialize();
	}

	/**
	 * Initialize the contents of the frame.
	 */
	private void initialize() {
		frmGroupressV = new JFrame();
		frmGroupressV.setTitle("Groupress V0.2");
		frmGroupressV.setBounds(100, 100, 819, 616);
		frmGroupressV.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frmGroupressV.getContentPane().setLayout(new BorderLayout(0, 0));
		
		JPanel topPanel = new JPanel();
		frmGroupressV.getContentPane().add(topPanel, BorderLayout.NORTH);
		GridBagLayout gbl_topPanel = new GridBagLayout();
		gbl_topPanel.columnWidths = new int[] {70, 100, 70, 100, 70, 100, 100};
		gbl_topPanel.rowHeights = new int[] {30, 0, 0, 0, 0, 0};
		gbl_topPanel.columnWeights = new double[]{0.0, 1.0, 0.0, 1.0, 0.0, 1.0, 0.0};
		gbl_topPanel.rowWeights = new double[]{0.0, 0.0, 0.0, 0.0, 0.0, 0.0};
		topPanel.setLayout(gbl_topPanel);
		
		JLabel lblNewLabel_7 = new JLabel("Profile:");
		GridBagConstraints gbc_lblNewLabel_7 = new GridBagConstraints();
		gbc_lblNewLabel_7.insets = new Insets(0, 0, 5, 5);
		gbc_lblNewLabel_7.anchor = GridBagConstraints.EAST;
		gbc_lblNewLabel_7.gridx = 0;
		gbc_lblNewLabel_7.gridy = 0;
		topPanel.add(lblNewLabel_7, gbc_lblNewLabel_7);
		
		JPanel panel = new JPanel();
		panel.setSize(new Dimension(0, 21));
		panel.setBorder(new EmptyBorder(0, 0, 1, 0));
		FlowLayout flowLayout = (FlowLayout) panel.getLayout();
		flowLayout.setAlignment(FlowLayout.LEFT);
		flowLayout.setVgap(0);
		flowLayout.setHgap(0);
		GridBagConstraints gbc_panel = new GridBagConstraints();
		gbc_panel.insets = new Insets(0, 0, 5, 5);
		gbc_panel.gridwidth = 5;
		gbc_panel.fill = GridBagConstraints.BOTH;
		gbc_panel.gridx = 1;
		gbc_panel.gridy = 0;
		topPanel.add(panel, gbc_panel);
		
		JRadioButton rdbtnNewRadioButton = new JRadioButton("Product");
		profileGroup.add(rdbtnNewRadioButton);
		panel.add(rdbtnNewRadioButton);
		
		JRadioButton rdbtnNewRadioButton_1 = new JRadioButton("Develop");
		rdbtnNewRadioButton_1.setSelected(true);
		profileGroup.add(rdbtnNewRadioButton_1);
		panel.add(rdbtnNewRadioButton_1);
		
		JRadioButton rdbtnNewRadioButton_2 = new JRadioButton("Test");
		profileGroup.add(rdbtnNewRadioButton_2);
		panel.add(rdbtnNewRadioButton_2);
		
		lblNewLabel_11 = new JLabel("Context:");
		GridBagConstraints gbc_lblNewLabel_11 = new GridBagConstraints();
		gbc_lblNewLabel_11.insets = new Insets(0, 0, 5, 5);
		gbc_lblNewLabel_11.anchor = GridBagConstraints.EAST;
		gbc_lblNewLabel_11.gridx = 4;
		gbc_lblNewLabel_11.gridy = 1;
		topPanel.add(lblNewLabel_11, gbc_lblNewLabel_11);
		
		txtConext = new JTextField();
		txtConext.setText("/gpapi");
		GridBagConstraints gbc_txtConext = new GridBagConstraints();
		gbc_txtConext.insets = new Insets(0, 0, 5, 5);
		gbc_txtConext.fill = GridBagConstraints.HORIZONTAL;
		gbc_txtConext.gridx = 5;
		gbc_txtConext.gridy = 1;
		topPanel.add(txtConext, gbc_txtConext);
		txtConext.setColumns(10);
		
		JButton btnLogin = new JButton("Login");
		btnLogin.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				doLogin();
			}
		});
		btnLogin.setPreferredSize(new Dimension(92, 29));
		GridBagConstraints gbc_btnLogin = new GridBagConstraints();
		gbc_btnLogin.fill = GridBagConstraints.BOTH;
		gbc_btnLogin.insets = new Insets(0, 0, 5, 0);
		gbc_btnLogin.gridx = 6;
		gbc_btnLogin.gridy = 1;
		topPanel.add(btnLogin, gbc_btnLogin);
		
		txtAudience = new JTextField();
		txtAudience.setText("gp.node.svr");
		GridBagConstraints gbc_txtAudience = new GridBagConstraints();
		gbc_txtAudience.insets = new Insets(0, 0, 5, 5);
		gbc_txtAudience.fill = GridBagConstraints.HORIZONTAL;
		gbc_txtAudience.gridx = 5;
		gbc_txtAudience.gridy = 2;
		topPanel.add(txtAudience, gbc_txtAudience);
		txtAudience.setColumns(10);
		
		txtPort = new JTextField();
		txtPort.setText("8082");
		GridBagConstraints gbc_txtPort = new GridBagConstraints();
		gbc_txtPort.insets = new Insets(0, 0, 5, 5);
		gbc_txtPort.fill = GridBagConstraints.HORIZONTAL;
		gbc_txtPort.gridx = 3;
		gbc_txtPort.gridy = 1;
		topPanel.add(txtPort, gbc_txtPort);
		txtPort.setColumns(10);
		
		JLabel lblNewLabel = new JLabel("Host:");
		GridBagConstraints gbc_lblNewLabel = new GridBagConstraints();
		gbc_lblNewLabel.insets = new Insets(0, 0, 5, 5);
		gbc_lblNewLabel.fill = GridBagConstraints.VERTICAL;
		gbc_lblNewLabel.anchor = GridBagConstraints.EAST;
		gbc_lblNewLabel.gridx = 0;
		gbc_lblNewLabel.gridy = 1;
		topPanel.add(lblNewLabel, gbc_lblNewLabel);
		
		txtHost = new JTextField();
		txtHost.setText("localhost");
		GridBagConstraints gbc_txtHost = new GridBagConstraints();
		gbc_txtHost.insets = new Insets(0, 0, 5, 5);
		gbc_txtHost.fill = GridBagConstraints.HORIZONTAL;
		gbc_txtHost.gridx = 1;
		gbc_txtHost.gridy = 1;
		topPanel.add(txtHost, gbc_txtHost);
		txtHost.setColumns(10);
		
		JLabel lblNewLabel_1 = new JLabel("Port:");
		GridBagConstraints gbc_lblNewLabel_1 = new GridBagConstraints();
		gbc_lblNewLabel_1.insets = new Insets(0, 0, 5, 5);
		gbc_lblNewLabel_1.anchor = GridBagConstraints.EAST;
		gbc_lblNewLabel_1.gridx = 2;
		gbc_lblNewLabel_1.gridy = 1;
		topPanel.add(lblNewLabel_1, gbc_lblNewLabel_1);
		
		JLabel lblNewLabel_2 = new JLabel("User:");
		GridBagConstraints gbc_lblNewLabel_2 = new GridBagConstraints();
		gbc_lblNewLabel_2.insets = new Insets(0, 0, 5, 5);
		gbc_lblNewLabel_2.anchor = GridBagConstraints.EAST;
		gbc_lblNewLabel_2.gridx = 0;
		gbc_lblNewLabel_2.gridy = 2;
		topPanel.add(lblNewLabel_2, gbc_lblNewLabel_2);
		
		txtUser = new JTextField();
		txtUser.setText("dev1");
		GridBagConstraints gbc_txtUser = new GridBagConstraints();
		gbc_txtUser.insets = new Insets(0, 0, 5, 5);
		gbc_txtUser.fill = GridBagConstraints.HORIZONTAL;
		gbc_txtUser.gridx = 1;
		gbc_txtUser.gridy = 2;
		topPanel.add(txtUser, gbc_txtUser);
		txtUser.setColumns(10);
		
		JLabel lblNewLabel_3 = new JLabel("Pass:");
		GridBagConstraints gbc_lblNewLabel_3 = new GridBagConstraints();
		gbc_lblNewLabel_3.insets = new Insets(0, 0, 5, 5);
		gbc_lblNewLabel_3.anchor = GridBagConstraints.EAST;
		gbc_lblNewLabel_3.gridx = 2;
		gbc_lblNewLabel_3.gridy = 2;
		topPanel.add(lblNewLabel_3, gbc_lblNewLabel_3);
		
		txtPass = new JTextField();
		txtPass.setText("1");
		GridBagConstraints gbc_txtPass = new GridBagConstraints();
		gbc_txtPass.insets = new Insets(0, 0, 5, 5);
		gbc_txtPass.fill = GridBagConstraints.HORIZONTAL;
		gbc_txtPass.gridx = 3;
		gbc_txtPass.gridy = 2;
		topPanel.add(txtPass, gbc_txtPass);
		txtPass.setColumns(10);
		
		JLabel lblNewLabel_4 = new JLabel("Audience:");
		GridBagConstraints gbc_lblNewLabel_4 = new GridBagConstraints();
		gbc_lblNewLabel_4.insets = new Insets(0, 0, 5, 5);
		gbc_lblNewLabel_4.anchor = GridBagConstraints.EAST;
		gbc_lblNewLabel_4.gridx = 4;
		gbc_lblNewLabel_4.gridy = 2;
		topPanel.add(lblNewLabel_4, gbc_lblNewLabel_4);
		
		JLabel lblNewLabel_6 = new JLabel("Client ID:");
		GridBagConstraints gbc_lblNewLabel_6 = new GridBagConstraints();
		gbc_lblNewLabel_6.anchor = GridBagConstraints.EAST;
		gbc_lblNewLabel_6.insets = new Insets(0, 0, 5, 5);
		gbc_lblNewLabel_6.gridx = 0;
		gbc_lblNewLabel_6.gridy = 3;
		topPanel.add(lblNewLabel_6, gbc_lblNewLabel_6);
		
		txtClient = new JTextField();
		txtClient.setText("1101");
		GridBagConstraints gbc_txtClient = new GridBagConstraints();
		gbc_txtClient.insets = new Insets(0, 0, 5, 5);
		gbc_txtClient.fill = GridBagConstraints.HORIZONTAL;
		gbc_txtClient.gridx = 1;
		gbc_txtClient.gridy = 3;
		topPanel.add(txtClient, gbc_txtClient);
		txtClient.setColumns(10);
		
		JLabel lblNewLabel_12 = new JLabel("Secret:");
		lblNewLabel_12.setHorizontalAlignment(SwingConstants.TRAILING);
		GridBagConstraints gbc_lblNewLabel_12 = new GridBagConstraints();
		gbc_lblNewLabel_12.anchor = GridBagConstraints.EAST;
		gbc_lblNewLabel_12.insets = new Insets(0, 0, 5, 5);
		gbc_lblNewLabel_12.gridx = 2;
		gbc_lblNewLabel_12.gridy = 3;
		topPanel.add(lblNewLabel_12, gbc_lblNewLabel_12);
		
		txtSecret = new JTextField();
		txtSecret.setText("sslssl");
		GridBagConstraints gbc_txtSecret = new GridBagConstraints();
		gbc_txtSecret.insets = new Insets(0, 0, 5, 5);
		gbc_txtSecret.fill = GridBagConstraints.HORIZONTAL;
		gbc_txtSecret.gridx = 3;
		gbc_txtSecret.gridy = 3;
		topPanel.add(txtSecret, gbc_txtSecret);
		txtSecret.setColumns(10);
		
		JLabel lblNewLabel_13 = new JLabel("Scope:");
		lblNewLabel_13.setHorizontalAlignment(SwingConstants.RIGHT);
		GridBagConstraints gbc_lblNewLabel_13 = new GridBagConstraints();
		gbc_lblNewLabel_13.anchor = GridBagConstraints.EAST;
		gbc_lblNewLabel_13.insets = new Insets(0, 0, 5, 5);
		gbc_lblNewLabel_13.gridx = 4;
		gbc_lblNewLabel_13.gridy = 3;
		topPanel.add(lblNewLabel_13, gbc_lblNewLabel_13);
		
		txtScope = new JTextField();
		txtScope.setText("read");
		GridBagConstraints gbc_txtScope = new GridBagConstraints();
		gbc_txtScope.insets = new Insets(0, 0, 5, 5);
		gbc_txtScope.fill = GridBagConstraints.HORIZONTAL;
		gbc_txtScope.gridx = 5;
		gbc_txtScope.gridy = 3;
		topPanel.add(txtScope, gbc_txtScope);
		txtScope.setColumns(10);
		
		JLabel lblNewLabel_5 = new JLabel("Api Url:");
		GridBagConstraints gbc_lblNewLabel_5 = new GridBagConstraints();
		gbc_lblNewLabel_5.insets = new Insets(0, 0, 5, 5);
		gbc_lblNewLabel_5.anchor = GridBagConstraints.EAST;
		gbc_lblNewLabel_5.gridx = 0;
		gbc_lblNewLabel_5.gridy = 4;
		topPanel.add(lblNewLabel_5, gbc_lblNewLabel_5);
		
		comboApi = new FilterComboBox();
		comboApi.setMasterList(ViewTracer.getApiItems());
		GridBagConstraints gbc_comboApi = new GridBagConstraints();
		gbc_comboApi.insets = new Insets(0, 0, 5, 5);
		gbc_comboApi.gridwidth = 5;
		gbc_comboApi.fill = GridBagConstraints.HORIZONTAL;
		gbc_comboApi.gridx = 1;
		gbc_comboApi.gridy = 4;
		topPanel.add(comboApi, gbc_comboApi);
		comboApi.addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent e) {
				if(ItemEvent.SELECTED == e.getStateChange()){
					String value = ViewTracer.getDemoData((String)e.getItem());
					txtPost.setText(value);
				}
			}
		});
		
		JButton btnSubmit = new JButton("Submit");
		btnSubmit.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				doSubmit();
			}
		});
		btnSubmit.setPreferredSize(new Dimension(92, 29));
		GridBagConstraints gbc_btnSubmit = new GridBagConstraints();
		gbc_btnSubmit.fill = GridBagConstraints.HORIZONTAL;
		gbc_btnSubmit.insets = new Insets(0, 0, 5, 0);
		gbc_btnSubmit.gridx = 6;
		gbc_btnSubmit.gridy = 4;
		topPanel.add(btnSubmit, gbc_btnSubmit);
		
		JLabel lblNewLabel_8 = new JLabel("JWT:");
		GridBagConstraints gbc_lblNewLabel_8 = new GridBagConstraints();
		gbc_lblNewLabel_8.insets = new Insets(0, 0, 0, 5);
		gbc_lblNewLabel_8.anchor = GridBagConstraints.EAST;
		gbc_lblNewLabel_8.gridx = 0;
		gbc_lblNewLabel_8.gridy = 5;
		topPanel.add(lblNewLabel_8, gbc_lblNewLabel_8);
		
		txtToken = new JTextField();
		GridBagConstraints gbc_txtToken = new GridBagConstraints();
		gbc_txtToken.insets = new Insets(0, 0, 0, 5);
		gbc_txtToken.gridwidth = 5;
		gbc_txtToken.fill = GridBagConstraints.HORIZONTAL;
		gbc_txtToken.gridx = 1;
		gbc_txtToken.gridy = 5;
		topPanel.add(txtToken, gbc_txtToken);
		txtToken.setColumns(10);
		
		JPanel bottomPanel = new JPanel();
		FlowLayout flowLayout_1 = (FlowLayout) bottomPanel.getLayout();
		flowLayout_1.setVgap(4);
		flowLayout_1.setAlignment(FlowLayout.LEFT);
		frmGroupressV.getContentPane().add(bottomPanel, BorderLayout.SOUTH);
		
		lblStatus = new JLabel("Welcome!");
		bottomPanel.add(lblStatus);
		
		JSplitPane splitPane = new JSplitPane();
		splitPane.setBorder(new EmptyBorder(0, 5, 0, 5));
		splitPane.setDividerSize(5);
		frmGroupressV.getContentPane().add(splitPane, BorderLayout.CENTER);
		
		JPanel panel_1 = new JPanel();
		splitPane.setLeftComponent(panel_1);
		panel_1.setLayout(new BorderLayout(0, 0));
		
		JPanel panel_3 = new JPanel();
		panel_3.setSize(new Dimension(0, 10));
		panel_1.add(panel_3, BorderLayout.NORTH);
		panel_3.setLayout(new BorderLayout(0, 0));
		
		JLabel lblNewLabel_9 = new JLabel("Post Data");
		panel_3.add(lblNewLabel_9, BorderLayout.WEST);
		
		JButton btnClear = new JButton("Clear");
		btnClear.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				txtPost.setText("");
				
			}
		});
		panel_3.add(btnClear, BorderLayout.EAST);
		
		JScrollPane scrollPane = new JScrollPane();
		panel_1.add(scrollPane, BorderLayout.CENTER);
		
		txtPost = new JTextArea();
		txtPost.setBorder(null);
		scrollPane.setViewportView(txtPost);
		
		JPanel panel_2 = new JPanel();
		splitPane.setRightComponent(panel_2);
		panel_2.setLayout(new BorderLayout(0, 0));
		
		JPanel panel_6 = new JPanel();
		panel_2.add(panel_6, BorderLayout.CENTER);
		panel_6.setLayout(new BorderLayout(0, 0));
		
		JPanel panel_top = new JPanel();
		panel_top.setBorder(new EmptyBorder(6, 0, 6, 0));
		panel_6.add(panel_top, BorderLayout.NORTH);
		panel_top.setLayout(new BorderLayout(0, 0));
		
		JLabel lblNewLabel_10 = new JLabel("Result Data:");
		panel_top.add(lblNewLabel_10, BorderLayout.WEST);
		
		JScrollPane scrollPane_1 = new JScrollPane();
		panel_6.add(scrollPane_1);
		
		txtResult = new JTextArea();
		scrollPane_1.setViewportView(txtResult);
		
		JPanel panel_5 = new JPanel();
		panel_2.add(panel_5, BorderLayout.NORTH);
		panel_5.setLayout(new BorderLayout(0, 0));
		
		JPanel panel_4 = new JPanel();
		panel_5.add(panel_4, BorderLayout.NORTH);
		panel_4.setLayout(new BorderLayout(7, 9));
		
		JLabel lblNewLabel_14 = new JLabel("Result Meta:");
		panel_4.add(lblNewLabel_14, BorderLayout.WEST);
		
		JButton btnClearResult = new JButton("Clear");
		panel_4.add(btnClearResult, BorderLayout.EAST);
		btnClearResult.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				txtResult.setText("");
				txtMeta.setText("");
			}
		});
		
		JScrollPane scrollPane_2 = new JScrollPane();
		panel_5.add(scrollPane_2, BorderLayout.CENTER);
		splitPane.setDividerLocation(250);
		
		txtMeta = new JTextArea();
		txtMeta.setRows(10);
		scrollPane_2.setViewportView(txtMeta);
		
	}
	
	public void doSubmit() {
		ViewTracer tracer = newTracer();
		ApiAccessHelper.callRemoteRpc(tracer);
	}
	
	public void doLogin() {
		ViewTracer tracer = newTracer();
		tracer.api = txtConext.getText() + "/authenticate";
		ApiAccessHelper.callAuthenRpc(tracer);
	}
	
	/**
	 * Build view tracer to track all the change on view
	 **/
	public ViewTracer newTracer() {
		ViewTracer tracer = new ViewTracer() {
						
			@Override
			void doCallback() {
				txtToken.setText(this.token);
				txtResult.setText(this.data);
				txtMeta.setText(this.meta);
				lblStatus.setText(this.state);
				txtPost.setText(this.request);
			}
			
		};
		tracer.user = this.txtUser.getText();
		tracer.client = this.txtClient.getText();
		tracer.secret = this.txtSecret.getText();
		tracer.scope = this.txtScope.getText();
		tracer.api = (String)this.comboApi.getSelectedItem();
		tracer.host = this.txtHost.getText();
		
		tracer.pass = this.txtPass.getText();
		tracer.port  = this.txtPort.getText();
		tracer.request = this.txtPost.getText();
		tracer.data = this.txtResult.getText();
		tracer.audience = this.txtAudience.getText();
		tracer.token = this.txtToken.getText();
//		tracer.ver = this.txtVer.getText();
		tracer.rootPath = this.txtConext.getText();
		return tracer;
	}
}
