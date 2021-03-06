package ui.panel;


import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.util.ArrayList;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.ListSelectionModel;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumnModel;




import core.Constants;
import core.ISystemComponent;
import core.persistence.History;
import core.persistence.History.HistoryInfo;
import core.runtime.logger.Logger;
import core.runtime.logger.ILogConfig.Module;

import util.DateTools;
import util.FileTools;
import util.IChangeListener;
import util.SystemTools;
import util.UiTools;

public class HistoryPanel extends JPanel implements ISystemComponent, IChangeListener {

	private static final long serialVersionUID = 1L;

	private History history;
	private Logger logger;
	
	private JScrollPane historyPanel;
	private JTable historyTable;
	private DefaultTableModel tableModel;
	private JTextArea historyOutput;
	private JButton emptyHistory;
	private JButton deleteHistory;
	private JButton openHistory;
	private JButton filterHistory;
	
	private ArrayList<HistoryInfo> entries;
	private String filter;
	
	public HistoryPanel(
			History history, 
			Logger logger)
	{
		this.history = history;
		this.logger = logger;
		
		entries = new ArrayList<HistoryInfo>();
		filter = "";
		
		tableModel = new DefaultTableModel(){
			private static final long serialVersionUID = 1L;
			public Object getValueAt(int row, int column){
				try{
					return super.getValueAt(row, column);
				}catch(Exception e){
					return null;
				}
			}
		};
		tableModel.addColumn("Launch");
		tableModel.addColumn("Trigger");
		tableModel.addColumn("Start");
		tableModel.addColumn("Time");
		tableModel.addColumn("Status");
		
		historyTable = new JTable(tableModel){
			private static final long serialVersionUID = 1L;
			@Override
			public boolean isCellEditable(int row, int col){ 
					return false;
			}
		};
		historyTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		historyTable.setRowSelectionAllowed(true);
		historyTable.setColumnSelectionAllowed(false);
		
		TableColumnModel columnModel = historyTable.getColumnModel();
		columnModel.getColumn(0).setMinWidth(150);
		columnModel.getColumn(1).setMinWidth(200);
		columnModel.getColumn(2).setMinWidth(150);
			columnModel.getColumn(2).setMaxWidth(150);
		columnModel.getColumn(3).setMinWidth(100);
			columnModel.getColumn(3).setMaxWidth(100);
		columnModel.getColumn(4).setMinWidth(150);
			columnModel.getColumn(4).setMaxWidth(150);
		
		emptyHistory = new JButton(" Empty ");
		emptyHistory.addActionListener(new ActionListener(){
			public void actionPerformed(ActionEvent e){ emptyHistory(); }
		});
		deleteHistory = new JButton(" Delete ");
		deleteHistory.addActionListener(new ActionListener(){
			public void actionPerformed(ActionEvent e){ deleteHistory(); }
		});
		openHistory = new JButton(" Open ");
		openHistory.addActionListener(new ActionListener(){
			public void actionPerformed(ActionEvent e){ openHistory(); }
		});
		filterHistory = new JButton(" Filter ");
		filterHistory.addActionListener(new ActionListener(){
			public void actionPerformed(ActionEvent e){ filterHistory(); }
		});
		
		JPanel buttonPanel = new JPanel();
		buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.Y_AXIS));
		buttonPanel.add(emptyHistory);
		buttonPanel.add(deleteHistory);
		buttonPanel.add(openHistory);
		buttonPanel.add(filterHistory);
		
		historyPanel = new JScrollPane(historyTable);
		JPanel topPanel = new JPanel(new BorderLayout());
		topPanel.add(historyPanel, BorderLayout.CENTER);
		topPanel.add(buttonPanel, BorderLayout.EAST);
		
		historyOutput = new JTextArea();
		historyOutput.setEditable(false);
		
		JSplitPane centerPanel = new JSplitPane(
				JSplitPane.VERTICAL_SPLIT,
				topPanel, 
				new JScrollPane(historyOutput));
		centerPanel.setDividerLocation(200);
		
		setLayout(new BorderLayout());
		add(centerPanel, BorderLayout.CENTER);
		
		historyTable.addMouseListener(new MouseAdapter(){
			public void mouseClicked(MouseEvent e){
				adjustSelection();
			}
		});
		historyTable.addKeyListener(new KeyListener(){
			@Override
			public void keyPressed(KeyEvent e) {}
			@Override
			public void keyReleased(KeyEvent e) {
				adjustSelection();
			}
			@Override
			public void keyTyped(KeyEvent e) {}
		});
		
		history.addListener(this);
	}
	
	@Override
	public void init() throws Exception {
		
		entries = history.getHistoryInfo();
		initUI();
		adjustSelection();
	}
	
	@Override
	public void shutdown() throws Exception {}

	private void clearUI() {
	
		historyTable.clearSelection();
		for(int i=tableModel.getRowCount()-1; i>=0; i--){
			tableModel.removeRow(i);
		}
	}
	
	private void initUI() {
		
		for(HistoryInfo entry : entries){
			if(matchesFilter(entry)){
				Object[] rowData = {
					entry.name,
					entry.trigger,
					entry.start != null ? DateTools.getTextDate(entry.start) : "",
					(entry.start != null && entry.end != null) ? 
							DateTools.getDiffMinutes(entry.start, entry.end)+ " '" : "",
					entry.status.toString()
				};
				tableModel.addRow(rowData);
			}
		}
		String info = "History: "+tableModel.getRowCount()+"/"+entries.size()+" items";
		historyPanel.setToolTipText(info);
		historyTable.setToolTipText(info);
	}

	private boolean matchesFilter(HistoryInfo entry) {
		
		if(!filter.isEmpty()){
			if(!entry.name.toLowerCase().equals(filter.toLowerCase())){
				return false;
			}
		}
		return true;
	}

	private void refreshUI(HistoryInfo selected) {
		
		clearUI();
		initUI();
		if(selected != null){	
			for(int i=0; i<entries.size(); i++){
				HistoryInfo entry = entries.get(i);
				if(
					entry.historyId.equals(selected.historyId) &&
					matchesFilter(entry)
				){
					historyTable.changeSelection(i, -1, false, false);
					break;
				}
			}
		}
		adjustSelection();
	}
	
	private void adjustSelection() {
		
		HistoryInfo entry = getSelectedHistory();
		if(entry != null){
			File logfile = new File(entry.logfile);
			if(logfile.isFile()){
				try{
					historyOutput.setText(FileTools.readFile(logfile.getAbsolutePath()));
					historyOutput.setCaretPosition(0);
				}catch(Exception e){
					logger.error(Module.COMMON, e);
				}
			}else{
				historyOutput.setText("");
			}
			deleteHistory.setEnabled(true);
			File file = new File(getHistoryIndexPath(entry));
			File folder = new File(entry.folder);
			if(file.isFile() || folder.isDirectory()){
				openHistory.setEnabled(true);
			}else{
				openHistory.setEnabled(false);
			}
		}else{
			historyOutput.setText("");
			deleteHistory.setEnabled(false);
			openHistory.setEnabled(false);
		}
		if(entries.size() > 0 && filter.isEmpty()){
			emptyHistory.setEnabled(true);
		}else{
			emptyHistory.setEnabled(false);
		}
		if(entries.size() > 0){
			filterHistory.setEnabled(true);
			if(filter.isEmpty()){
				filterHistory.setForeground(Color.BLACK);
			}else{
				filterHistory.setForeground(Color.RED);
			}
		}else{
			filterHistory.setEnabled(false);
			filterHistory.setForeground(Color.GRAY);
		}
	}
	
	@Override
	public void changed(Object object) {
		
		if(object == history){
			HistoryInfo selected = getSelectedHistory();
			entries = history.getHistoryInfo();
			refreshUI(selected);
		}
	}
	
	private HistoryInfo getSelectedHistory() {
		
		HistoryInfo selected = null;
		int index = historyTable.getSelectedRow();
		if(index >=0){				
			selected = entries.get(index);
		}
		return selected;
	}
	
	public void emptyHistory(){
		
		if(UiTools.confirmDialog("Empty history ?\n\n!!! Complete history will be deleted !!!")){
			history.clear();
			refreshUI(null);
		}
	}
	
	public void deleteHistory(){
		
		HistoryInfo entry = getSelectedHistory();
		if(
				entry != null && 
				UiTools.confirmDialog("Delete history [ "+DateTools.getTextDate(entry.start)+" ] ?")
		){
			history.delete(entry.historyId);
			refreshUI(null);
		}
	}
	
	public void historyFolder(){
		
		HistoryInfo entry = getSelectedHistory();
		if(entry != null){
			File folder = new File(entry.folder);
			if(folder.isDirectory()){
				try{
					String path = folder.getAbsolutePath();
					logger.debug(Module.COMMON, "open: "+path);
					SystemTools.openBrowser(path);
				}catch(Exception e){
					UiTools.errorDialog(e);
				}
			}
		}
	}
	
	public void openHistory(){
		
		HistoryInfo entry = getSelectedHistory();
		if(entry != null){
			File file = new File(getHistoryIndexPath(entry));
			File folder = new File(entry.folder);
			String path = null;
			if(file.isFile()){
				path = file.getAbsolutePath();
			}else if(folder.isDirectory()){
				path = folder.getAbsolutePath();
			}
			if(path != null){
				try{
					logger.debug(Module.COMMON, "open: "+path);
					SystemTools.openBrowser(path);
				}catch(Exception e){
					UiTools.errorDialog(e);
				}
			}
		}
	}

	private String getHistoryIndexPath(HistoryInfo entry) {
		return entry.folder+File.separator+Constants.INDEX_NAME+".htm";
	}
	
	public void filterHistory(){
		
		String filter = UiTools.inputDialog("Filter for Launch", this.filter);
		if(filter != null){
			this.filter = filter;
		}
		HistoryInfo selected = getSelectedHistory();
		refreshUI(selected);
	}
}
