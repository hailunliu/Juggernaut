package ui;

import java.awt.BorderLayout;
import java.awt.Container;

import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenuBar;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;

import core.Application;
import core.ConfigStore;
import core.Constants;
import core.IChangeListener;

public class MainFrame extends JFrame implements IChangeListener {

	private static final long serialVersionUID = 1L;

	private JMenuBar menuBar;
	private JPanel configPanel;
	private JPanel activityPanel;
	private JPanel historyPanel;
	private JPanel preferencePanel;
	private JLabel statusBar;
	
	public MainFrame(){
		
		menuBar = new JMenuBar();
		menuBar.add(new ProjectMenu());
		setJMenuBar(menuBar);
		
		configPanel = new ConfigPanel();
		activityPanel = new ActivityPanel();
		historyPanel = new HistoryPanel();
		preferencePanel = new PreferencePanel();
		
		JTabbedPane centerPanel = new JTabbedPane();
		centerPanel.setTabPlacement(JTabbedPane.TOP);
		centerPanel.add(configPanel, "Configuration");
		centerPanel.add(activityPanel, "Activity");
		centerPanel.add(historyPanel, "History");
		centerPanel.add(preferencePanel, "Preferences");
		
		statusBar = new JLabel("> ");
		
		Container pane = getContentPane();
		pane.add(centerPanel, BorderLayout.CENTER);
		pane.add(statusBar, BorderLayout.SOUTH);
		pack();

		setSize(Constants.APP_WIDTH, Constants.APP_HEIGHT);
		setLocation(100, 100);
		
		setTitle(Constants.APP_FULL_NAME);
	}
	
	public void setStatus(String text){
		
		text = "> "+text;
		statusBar.setText(text);
		Application.getInstance().getLogger().log(text);
	}

	@Override
	public void changed(Object object) {
		
		if(object instanceof ConfigStore){
			ConfigStore.State state = ((ConfigStore)object).getState();
			if(state == ConfigStore.State.CLEAN){
				setTitle(Constants.APP_FULL_NAME);
			}else if(state == ConfigStore.State.DIRTY){
				setTitle(Constants.APP_FULL_NAME+" *");
			}
		}
	}
}
