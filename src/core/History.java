package core;

import html.HistoryPage;

import java.io.File;
import java.util.ArrayList;
import java.util.Date;

import launch.StatusManager.Status;

import logger.Logger;
import logger.ILogConfig.Module;

import util.FileTools;
import util.IChangedListener;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.xml.DomDriver;

import data.LaunchHistory;

/**
 * the configuration of the application,- will be serialized
 */
public class History implements ISystemComponent {

	public static History create(Configuration configuration, FileManager fileManager, Logger logger) throws Exception {
		
		File file = new File(fileManager.getDataFolderPath()+File.separator+History.OUTPUT_FILE);
		if(file.isFile()){
			return History.load(configuration, fileManager, logger, file.getAbsolutePath());
		}else{
			return new History(configuration, fileManager, logger, file.getAbsolutePath());
		}	
	}
	
	public static final String OUTPUT_FILE = "History.xml";
	
	private transient Configuration configuration;
	private transient FileManager fileManager;
	private transient Logger logger;
	
	@SuppressWarnings("unused")
	private String version;
	private ArrayList<LaunchHistory> entries;
	private transient ArrayList<IChangedListener> listeners;
	private transient String path;
	private transient boolean dirty;

	public History(Configuration configuration, FileManager fileManager, Logger logger, String path){
		
		this.configuration = configuration;
		this.fileManager = fileManager;
		this.logger = logger;
		
		version = Constants.APP_VERSION;
		entries = new ArrayList<LaunchHistory>();
		listeners = new ArrayList<IChangedListener>();
		this.path = path;
		dirty = true;
	}
	
	@Override
	public void init() throws Exception {
		save();
	}

	@Override
	public void shutdown() throws Exception {}

	public void addListener(IChangedListener listener){ listeners.add(listener); }
	
	public void notifyListeners(){
		for(IChangedListener listener : listeners){
			listener.changed(this);
		}
	}
	
	public ArrayList<LaunchHistory> getEntries(){ return entries; }
	public String getPath(){ return path; }
	
	public void setDirty(boolean dirty){ this.dirty = dirty; }
	public boolean isDirty(){ return dirty; }
	
	public static History load(Configuration configuration, FileManager fileManager, Logger logger, String path) throws Exception {
	
		logger.debug(Module.COMMON, "load: "+path);
		XStream xstream = new XStream(new DomDriver());
		String xml = FileTools.readFile(path);
		History history = (History)xstream.fromXML(xml);
		history.configuration = configuration;
		history.fileManager = fileManager;
		history.logger = logger;
		history.listeners = new ArrayList<IChangedListener>();
		history.path = path;
		history.dirty = false;
		return history;
	}
	
	public void save() throws Exception {
		
		if(isDirty()){
			logger.debug(Module.COMMON, "save: "+path);
			XStream xstream = new XStream(new DomDriver());
			String xml = xstream.toXML(this);
			FileTools.writeFile(path, xml, false);
			dirty = false;
			notifyListeners();
		}
	}
	
	public synchronized void add(LaunchHistory entry) throws Exception {
		
		entry.init();
		entries.add(0, entry);
		dirty = true;
		save();
		cleanup();
		createIndex();
	}
	
	public synchronized void delete(String id) {
		
		for(int i=0; i<entries.size(); i++){
			LaunchHistory entry = entries.get(i);
			if(entry.historyId.equals(id)){
				delete(entry);
			}
		}
		createIndex();
	}
	
	/** clear old entries */
	public synchronized void clear() {
		
		for(int i = entries.size()-1; i>=0; i--){
			delete(entries.get(i));
		}
		createIndex();
	}

	/** recreate the main index-page */
	public void createIndex(){
		
		HistoryPage page = new HistoryPage(this, getIndexPath());
		try{
			page.create();
		}catch(Exception e){
			logger.error(Module.COMMON, e);
		}
	}
	
	public String getIndexPath() {
		return 
			fileManager.getHistoryFolderPath()+
			File.separator+HistoryPage.OUTPUT_FILE;
	}

	/** remove old entries */
	private void cleanup() {
		
		int max = configuration.getMaximumHistory();
		if(max > 0){
			while(max < entries.size()){
				delete(entries.get(entries.size()-1));
			}
		}
	}
	
	private void delete(LaunchHistory entry) {
		
		logger.debug(Module.COMMON, "deleting history: "+entry.id);
		try{
			entries.remove(entry);
			FileTools.deleteFolder(entry.folder);
			dirty = true;
			save();
		}catch(Exception e){
			logger.error(Module.COMMON, e);
		}
	}
	
	public class HistoryInfo {
		
		public String historyId;
		public String name;
		public String id;
		public String logfile;
		public String trigger;
		public Date start;
		public Date end;
		public Status status;
		public String folder;
		
		public HistoryInfo(LaunchHistory entry){
			historyId = entry.historyId;
			name = entry.name;
			id = entry.id;
			logfile = entry.logfile;
			trigger = entry.trigger;
			start = entry.start;
			end = entry.end;
			status = entry.status;
			folder = entry.folder;
		}
	}
	
	public synchronized ArrayList<HistoryInfo> getHistoryInfo(){
		
		ArrayList<HistoryInfo> infos = new ArrayList<HistoryInfo>();
		for(LaunchHistory entry : entries){
			infos.add(new HistoryInfo(entry));
		}
		return infos;
	}
}
