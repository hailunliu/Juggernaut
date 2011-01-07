package core;

import http.HttpServer;
import launch.LaunchManager;
import launch.ScheduleManager;
import smtp.SmtpClient;
import ui.ConfigPanel;
import ui.HistoryPanel;
import ui.PreferencePanel;
import ui.ProjectMenu;
import ui.SchedulerPanel;
import ui.ToolsMenu;
import ui.Window;

public class Application extends AbstractSystem {

	private static Application application;
	
	public static void main(String[] args){
		
		try{
			application = new Application();
			application.init(); 
		}catch(Exception e){
			e.printStackTrace();
			System.exit(Constants.PROCESS_NOK);
		}
	}
	
	private CoreSystem core;
	private PersistenceSystem persistence;
	private RuntimeSystem runtime;
	private UISystem ui;
	
	private Application(){}
	
	@Override
	public void init() throws Exception {
		
		core = new CoreSystem();
		add(core);
		persistence = new PersistenceSystem();
		add(persistence);
		runtime = new RuntimeSystem();
		add(runtime);
		ui = new UISystem();
		add(ui);

		super.init();
	}

	public void revert() throws Exception {
		
		if(ui.isInitialized() && persistence.configuration.isDirty()){
			ui.shutdown();
			persistence.clear();
			persistence.init();
			ui.clear();
			ui.init();
		}
	}
	
	/** io related components */
	private class CoreSystem extends AbstractSystem {
		
		public SystemLogger logger;
		public FileManager fileManager;
		public TaskManager taskManager;
		public HeapManager heapManager;
		
		@Override
		public void init() throws Exception {
			
			logger = new SystemLogger();
			add(logger);
			fileManager = new FileManager(logger);
			add(fileManager);
			taskManager = new TaskManager(
					logger);
			add(taskManager);
			heapManager = new HeapManager(
					taskManager, 
					logger);
			add(heapManager);
			super.init();
		}
	}
	
	/** persistence related components */
	private class PersistenceSystem extends AbstractSystem {
		
		public Configuration configuration;
		public Cache cache;
		public History history;
		
		@Override
		public void init() throws Exception {
			
			configuration = Configuration.create(
					core.fileManager, 
					core.taskManager,
					core.logger);
			add(configuration);
			cache = Cache.create(
					core.fileManager, 
					core.logger);
			add(cache);
			history = History.create(
					configuration, 
					core.fileManager, 
					core.logger);
			add(history);
			super.init();
		}
	}
	
	/** runtime related components */
	private class RuntimeSystem extends AbstractSystem {
		
		public Registry registry;
		public SmtpClient smtpClient;
		public LaunchManager launchManager;
		public ScheduleManager scheduleManager;
		public HttpServer httpServer;
		
		@Override
		public void init() throws Exception {
			
			registry = new Registry(
					core.taskManager, 
					core.logger);
			add(registry);
			smtpClient = new SmtpClient(
					persistence.configuration);
			add(smtpClient);
			httpServer = new HttpServer(
					persistence.configuration, 
					core.fileManager, 
					core.taskManager, 
					core.logger);
			add(httpServer);
			launchManager = new LaunchManager(
					persistence.configuration);
			add(launchManager);
			scheduleManager = new ScheduleManager(
					persistence.configuration, 
					persistence.cache,
					persistence.history, 
					core.fileManager, 
					core.taskManager,
					smtpClient,
					httpServer,
					launchManager, 
					core.logger);
			add(scheduleManager);
			super.init();
		}
	}
	
	/** ui related componets */
	private class UISystem extends AbstractSystem {
		
		public ProjectMenu projectMenu;
		public ToolsMenu toolsMenu;
		public ConfigPanel configPanel;
		public SchedulerPanel schedulerPanel;
		public HistoryPanel historyPanel;
		public PreferencePanel preferencePanel;
		public Window window;
		
		@Override
		public void init() throws Exception {
			
			projectMenu = new ProjectMenu(
					application, 
					persistence.configuration, 
					runtime.launchManager, 
					core.logger);
			add(projectMenu);
			toolsMenu = new ToolsMenu(
					persistence.configuration, 
					core.fileManager, 
					core.heapManager);
			add(toolsMenu);
			configPanel = new ConfigPanel(
					persistence.configuration, 
					persistence.cache,
					persistence.history, 
					core.fileManager, 
					core.taskManager, 
					runtime.smtpClient,
					runtime.httpServer,
					runtime.launchManager, 
					runtime.registry);
			add(configPanel);
			schedulerPanel = new SchedulerPanel(
					runtime.launchManager, 
					runtime.scheduleManager, 
					core.logger);
			add(schedulerPanel);
			historyPanel = new HistoryPanel(
					persistence.history, 
					core.logger);
			add(historyPanel);
			preferencePanel = new PreferencePanel(
					persistence.configuration, 
					runtime.scheduleManager, 
					runtime.httpServer,
					persistence.history);
			add(preferencePanel);
			window = new Window(
					persistence.configuration, 
					runtime.launchManager, 
					core.heapManager, 
					core.logger, 
					projectMenu, 
					toolsMenu, 
					configPanel, 
					schedulerPanel, 
					historyPanel, 
					preferencePanel);
			add(window);
			super.init();
		}
	}
}
