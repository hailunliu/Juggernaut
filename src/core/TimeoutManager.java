package core;

import java.util.ArrayList;

import util.Task;


public class TimeoutManager extends Task {

	private static final long CYCLE = 10 * 1000; // 10 sec
	
	private ArrayList<Task> tasks;
	
	public TimeoutManager(){
		super("TimeoutManager", Application.getInstance().getLogger());
		tasks = new ArrayList<Task>();
	}

	public void register(Task task) {
		
		synchronized(tasks){
			tasks.add(task);
		}
	}

	public void deregister(Task task) {
		
		synchronized(tasks){
			tasks.remove(task);
		}
	}
	
	public void init() {
		
		setCycle(CYCLE);
		asyncRun(0, 0);
	}
	
	public void shutdown() {

		syncKill();
		synchronized(tasks){
			for(int i=tasks.size()-1; i>=0; i--){
				Task task = tasks.get(i);
				if(task != null){
					task.syncKill();
				}
			}
		}
		tasks.clear();
	}

	@Override
	protected void runTask() {

		synchronized(tasks){
			for(int i=tasks.size()-1; i>=0; i--){
				Task task = tasks.get(i);
				if(task != null && task.isExpired()){
						tasks.remove(task);
						task.getObserver().log("Task Timeout ["+task.getName()+"]");
						task.asyncKill();
				}
			}
		}
	}
}
