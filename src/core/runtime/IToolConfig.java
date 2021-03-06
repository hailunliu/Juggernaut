package core.runtime;

public interface IToolConfig {

	long UNLOCKER_TIMEOUT = 60 * 1000; // 1 min

	/** external unlocker command for freeing locked ressources */
	public String getUnlocker();
}
