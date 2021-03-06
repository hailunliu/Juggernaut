package ui.option;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Date;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SpinnerDateModel;
import javax.swing.SpinnerModel;
import javax.swing.SpinnerNumberModel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;



import ui.IStatusClient;
import ui.IStatusProvider;
import util.IChangeListener;
import util.IChangeable;
import util.StringTools;

public class OptionEditor extends JPanel implements IChangeable, IStatusProvider {

	private static final long serialVersionUID = 1L;

	private ArrayList<IChangeListener> listeners;
	private IStatusClient client;
	private OptionContainer container;
	private IOptionInitializer initializer;
	private JPanel centerPanel;
	private JPopupMenu contextMenu;
	
	private JLabel tooltipInfo;
	private JLabel contextInfo;

	private boolean showGroups;
	private boolean showInfo;
	
	public OptionEditor(){
		
		listeners = new ArrayList<IChangeListener>();
		client = null;
		container = null;
		
		showGroups = true;
		showInfo = true;
		
		setLayout(new BorderLayout());
	}
	
	public void setShowGroups(boolean show){ showGroups = show; }
	public void setShowInfo(boolean show){ showInfo = show; }
	
	public void setOptionContainer(OptionContainer container, IOptionInitializer initializer) {
		
		removeAll();
		this.container = container;
		this.initializer = initializer;
		centerPanel = new JPanel();
		centerPanel.setLayout(new BoxLayout(centerPanel, BoxLayout.Y_AXIS));
		if(container != null){
			// create option-panels
			String groupName = "";
			JPanel groupPanel = null;
			for(Option option : container.getOptions()){
				if(!groupName.equals(option.getGroup())){
					groupName = option.getGroup();
					if(groupPanel != null){
						centerPanel.add(groupPanel);
						groupPanel = null;
					}
					groupPanel = new JPanel();
					groupPanel.setLayout(new BoxLayout(groupPanel, BoxLayout.Y_AXIS));
					if(showGroups){
						groupPanel.setBorder(BorderFactory.createTitledBorder(groupName));
					}
				}
				JPanel panel = createPanel(option);
				option.parent = panel;
				groupPanel.add(panel);
			}
			if(groupPanel != null){
				centerPanel.add(groupPanel);
				groupPanel = null;
			}
			// set info-panel
			if(showInfo){
				initializeInfoPanel();
			}
			// set tool-tip
			if(!container.getDescription().isEmpty()){
				initializeToolTip();
			}
			// set actions
			if(initializer != null){
				initializeOptions();
			}
			// set center-panel
			add(centerPanel, BorderLayout.NORTH);
		}else{
			setToolTipText(null);
		}
	}

	private void initializeInfoPanel() {
		tooltipInfo = new JLabel("");
		tooltipInfo.setEnabled(false);
		contextInfo = new JLabel("");
		contextInfo.setEnabled(false);
		JPanel infoPanel = new JPanel(new BorderLayout());
		infoPanel.add(tooltipInfo, BorderLayout.WEST);
		infoPanel.add(contextInfo, BorderLayout.EAST);
		centerPanel.add(infoPanel);
	}

	private void initializeToolTip() {
		for(int i=0; i<getComponentCount(); i++){
			Component component = getComponent(i);
			if(component instanceof JPanel){
				((JPanel) component).setToolTipText(container.getDescription());
			}
		}
		setToolTipText(container.getDescription());
		if(showInfo){
			tooltipInfo.setText(" "+container.getDescription());
		}
	}

	private void initializeOptions() {
		contextMenu = new JPopupMenu();
		centerPanel.addMouseListener(new MouseAdapter(){
			public void mouseClicked(MouseEvent e){
				if(e.getButton() == MouseEvent.BUTTON3){
					contextMenu.show(e.getComponent(), e.getX(), e.getY());
				}
			}
		});
		initializer.initOptions(container);
		updateContext();
	}
	
	public void updateContext(){
		
		if(initializer != null && contextMenu != null){
			contextMenu.removeAll();
			initializer.initEditor(this);
			if(showInfo){
				if(contextMenu.getComponentCount() > 0){
					contextInfo.setText("Context available ("+contextMenu.getComponentCount()+") ");
				}else{
					contextInfo.setText("");
				}
			}
		}
	}

	@Override
	public void addListener(IChangeListener listener){ listeners.add(listener); }
	@Override
	public void removeListener(IChangeListener listener){ listeners.remove(listener); }
	@Override
	public void notifyListeners(){
		for(IChangeListener listener : listeners){ listener.changed(this); }
	}
	
	@Override
	public void setStatusClient(IStatusClient client) {
		this.client = client; 
	}

	@Override
	public void status(String text) {
		if(client != null){
			client.status(text);
		}
	}
	
	private JPanel createPanel(Option option) {
		
		switch(option.getType()){
			case TEXT:
				return createTextFieldPanel(option); 
			case TEXT_SMALL:
				return createSmallTextFieldPanel(option);
			case TEXT_AREA:
				return createTextAreaPanel(option); 
			case TEXT_LIST:
				return createTextListPanel(option);
			case INTEGER:
				return createIntegerSpinnerPanel(option);
			case BOOLEAN:
				return createCheckBoxPanel(option); 
			case TIME:
				return createTimeSpinnerPanel(option); 
		}
		return null;
	}
	
	private JPanel createTextFieldPanel(final Option option) {

		final JTextField component = new JTextField();
		option.component = component;
		component.setColumns(25);
		component.setToolTipText(option.getDescription());
		component.setText(option.getStringValue());
		component.addKeyListener(new KeyListener(){
			@Override
			public void keyPressed(KeyEvent e) {}
			@Override
			public void keyReleased(KeyEvent e) {
				if(StringTools.isModifyingKey(e)){
					String value = component.getText();
					container.getOption(option.getName()).setStringValue(value);
					notifyListeners();
				}
			}
			@Override
			public void keyTyped(KeyEvent e) {}
		});
		JPanel panel = new JPanel(new BorderLayout());
		panel.add(new JLabel(option.getUIName()+":"), BorderLayout.NORTH);
		panel.add(component, BorderLayout.CENTER);
		return panel;
	}

	private JPanel createSmallTextFieldPanel(final Option option) {

		final JTextField component = new JTextField();
		option.component = component;
		component.setColumns(25);
		component.setToolTipText(option.getDescription());
		component.setText(option.getStringValue());
		component.addKeyListener(new KeyListener(){
			@Override
			public void keyPressed(KeyEvent e) {}
			@Override
			public void keyReleased(KeyEvent e) {
				if(StringTools.isModifyingKey(e)){
					String value = component.getText();
					container.getOption(option.getName()).setStringValue(value);
					notifyListeners();
				}
			}
			@Override
			public void keyTyped(KeyEvent e) {}
		});
		JPanel panel = new JPanel(new BorderLayout());
		panel.add(new JLabel(option.getUIName()+":"), BorderLayout.NORTH);
		panel.add(component, BorderLayout.WEST);
		return panel;
	}
	
	private JPanel createTextAreaPanel(final Option option) {

		final JTextArea component = new JTextArea(5, 25);
		option.component = component;
		component.setToolTipText(option.getDescription());
		component.setText(option.getStringValue());
		component.setCaretPosition(0);
		component.addKeyListener(new KeyListener(){
			@Override
			public void keyPressed(KeyEvent e) {}
			@Override
			public void keyReleased(KeyEvent e) {
				if(StringTools.isModifyingKey(e)){
					String value = component.getText();
					container.getOption(option.getName()).setStringValue(value);
					notifyListeners();
				}
			}
			@Override
			public void keyTyped(KeyEvent e) {}
		});
		JPanel panel = new JPanel(new BorderLayout());
		panel.add(new JLabel(option.getUIName()+":"), BorderLayout.NORTH);
		panel.add(new JScrollPane(component), BorderLayout.CENTER);
		return panel;
	}

	private JPanel createTextListPanel(final Option option) {

		final JComboBox component = new JComboBox();
		option.component = component;
		component.setToolTipText(option.getDescription());
		for(int i=0; i<option.getListSize(); i++){
			component.addItem(option.getListItem(i));
		}
		component.setSelectedItem(option.getStringValue());
		component.addItemListener(new ItemListener(){
			@Override
			public void itemStateChanged(ItemEvent e) {
				if(e.getStateChange() == ItemEvent.SELECTED){
					int index = component.getSelectedIndex();
					String value = (String)component.getItemAt(index);
					container.getOption(option.getName()).setStringValue(value);
					notifyListeners();
				}
			}
		});
		JPanel panel = new JPanel(new BorderLayout());
		panel.add(new JLabel(option.getUIName()+":"), BorderLayout.NORTH);
		panel.add(component, BorderLayout.WEST);
		return panel;
	}
	
	private JPanel createIntegerSpinnerPanel(final Option option) {
		
		SpinnerModel model = new SpinnerNumberModel(
				option.getIntegerValue(),
				option.getIntegerMinimum(),
				option.getIntegerMaximum(),
                1);  
		final JSpinner component = new JSpinner(model);
		option.component = component;
		((JSpinner.DefaultEditor)component.getEditor()).getTextField().setColumns(8);
		component.setToolTipText(
				option.getDescription()+" : "+option.getIntegerMinimum()+" - "+option.getIntegerMaximum()
		);
		component.addChangeListener(new ChangeListener(){
			@Override
			public void stateChanged(ChangeEvent e) {
				int value = ((Integer)component.getValue()).intValue();
				container.getOption(option.getName()).setIntegerValue(value);
				notifyListeners();
			}
		});
		JPanel panel = new JPanel(new BorderLayout());
		panel.add(new JLabel(option.getUIName()+":"), BorderLayout.NORTH);
		panel.add(component, BorderLayout.WEST);
		return panel;
	}
	
	private JPanel createCheckBoxPanel(final Option option) {

		final JCheckBox component = new JCheckBox(option.getUIName());
		option.component = component;
		component.setToolTipText(option.getDescription());
		component.setSelected(option.getBooleanValue());
		component.addItemListener(new ItemListener(){
			@Override
			public void itemStateChanged(ItemEvent e) {
				boolean value = component.isSelected();
				container.getOption(option.getName()).setBooleanValue(value);
				notifyListeners();
			}
		});
		JPanel panel = new JPanel(new BorderLayout());
		panel.add(component, BorderLayout.WEST);
		return panel;
	}
	
	private JPanel createTimeSpinnerPanel(final Option option) {
		
	    final JSpinner component = new JSpinner(new SpinnerDateModel());
	    option.component = component;
	    JSpinner.DateEditor editor = new JSpinner.DateEditor(component, "HH:mm");
	    component.setEditor(editor);
		editor.getTextField().setColumns(8);
		component.setToolTipText(option.getDescription());
		Date date = new Date((new Long(option.getStringValue())).longValue());
		component.setValue(date);
		component.addChangeListener(new ChangeListener(){
			@Override
			public void stateChanged(ChangeEvent e) {
				long value = ((Date)component.getValue()).getTime();
				container.getOption(option.getName()).setStringValue(""+value);
				notifyListeners();
			}
		});
		JPanel panel = new JPanel(new BorderLayout());
		panel.add(new JLabel(option.getUIName()+":"), BorderLayout.NORTH);
		panel.add(component, BorderLayout.WEST);
		return panel;
	}
	
	public void addContextDelegate(final IEditorDelegate delegate){
		
		if(contextMenu != null){
			JMenuItem item = new JMenuItem(delegate.getDelegateName());
			item.addActionListener(new ActionListener(){
				public void actionPerformed(ActionEvent e){ 
					delegate.perform();
				}
			});
			contextMenu.add(item);
		}
	}
	
	public static void setOptionDelegate(final Option option, final IOptionDelegate delegate){
		
		JButton button = new JButton(" "+delegate.getDelegateName()+" ");
		button.addActionListener(new ActionListener(){
			public void actionPerformed(ActionEvent e){ 
				delegate.perform(option.getStringValue());
			}
		});
		option.parent.add(button, BorderLayout.EAST);
	}
}
