package de.tukl.cs.softech.agilereview.plugincontrol;

import org.eclipse.core.commands.IExecutionListener;
import org.eclipse.ui.IStartup;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.commands.ICommandService;

import de.tukl.cs.softech.agilereview.plugincontrol.refactoring.ExecutionListener;
import de.tukl.cs.softech.agilereview.tools.PluginLogger;

/**
 * Startup class in order to start this plug-in on eclipse startup
 */
public class Startup implements IStartup {
	
	/**
	 * {@link IExecutionListener} for listening relevant Commands
	 */
	private ExecutionListener executionListener = new ExecutionListener();
	
	@Override
	public void earlyStartup() {
		// add executionlistener to listen to interesting commands
		PluginLogger.log(this.getClass().toString(), "earlyStartup", "register executionListener & ResourceChangeListener");
		((ICommandService)PlatformUI.getWorkbench().getService(ICommandService.class)).addExecutionListener(executionListener);
	}
}