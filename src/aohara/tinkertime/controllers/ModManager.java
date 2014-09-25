package aohara.tinkertime.controllers;

import java.io.IOException;
import java.net.URL;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import org.apache.commons.io.FilenameUtils;

import aohara.common.Listenable;
import aohara.common.workflows.ConflictResolver;
import aohara.common.workflows.ProgressPanel;
import aohara.common.workflows.Workflow;
import aohara.tinkertime.Config;
import aohara.tinkertime.crawlers.CrawlerFactory.UnsupportedHostException;
import aohara.tinkertime.models.Mod;
import aohara.tinkertime.views.DialogConflictResolver;
import aohara.tinkertime.workflows.ModWorkflowBuilder;

/**
 * Controller for initiating Asynchronous Tasks for Mod Processing.
 * 
 * All Mod-Related Actions are to be initiated through this Controller.
 * All Asynchronous tasks initiated are executed by the executors of this class,
 * and the tasks are represented by {@link aohara.common.workflows.Workflow} classes.
 * 
 * @author Andrew O'Hara
 */
public class ModManager extends Listenable<ModUpdateListener> implements WorkflowRunner {
	
	public static final int NUM_CONCURRENT_DOWNLOADS = 4;
	
	private final Executor downloadExecutor, enablerExecutor;
	private final Config config;
	private final ModStateManager sm;
	private final ProgressPanel progressPanel;
	private final ConflictResolver cr;
	
	public static ModManager createDefaultModManager(ModStateManager sm, ProgressPanel pp){
		
		ModManager mm =  new ModManager(
			sm, new Config(), pp, new DialogConflictResolver(),
			Executors.newFixedThreadPool(NUM_CONCURRENT_DOWNLOADS),
			Executors.newSingleThreadExecutor());
		
		return mm;
	}
	
	public ModManager(
			ModStateManager sm, Config config, ProgressPanel progressPanel,
			ConflictResolver cr, Executor downloadExecutor,
			Executor enablerExecutor){
		this.sm = sm;
		this.config = config;
		this.progressPanel = progressPanel;
		this.cr = cr;
		this.downloadExecutor = downloadExecutor;
		this.enablerExecutor = enablerExecutor;
		
		addListener(sm);
	}
	
	// -- Listeners -----------------------
	
	public void notifyModUpdated(Mod mod, boolean deleted){
		for (ModUpdateListener l : getListeners()){
			l.modUpdated(mod, deleted);
		}
	}
	
	// -- Accessors ------------------------
	
	public static boolean isDownloaded(Mod mod, Config config){
		return mod.getCachedImagePath(config).toFile().exists();
	}
	
	public boolean isDownloaded(Mod mod){
		return isDownloaded(mod, config);
	}
	
	// -- Modifiers ---------------------------------
	
	@Override
	public void submitDownloadWorkflow(Workflow workflow){
		workflow.addListener(progressPanel);
		downloadExecutor.execute(workflow);
	}
	
	@Override
	public void submitEnablerWorkflow(Workflow workflow){
		workflow.addListener(progressPanel);
		enablerExecutor.execute(workflow);
	}
	
	public void updateMod(Mod mod) throws ModUpdateFailedException {
		try {
			downloadMod(mod.getPageUrl());
		} catch(UnsupportedHostException e){
			throw new ModUpdateFailedException();
		}
	}
	
	public void downloadMod(URL url) throws ModUpdateFailedException, UnsupportedHostException {
		Workflow wf = new Workflow("Downloading " + FilenameUtils.getBaseName(url.toString()));
		try {
			ModWorkflowBuilder.downloadMod(wf, url, config, sm);
			submitDownloadWorkflow(wf);
		} catch (IOException e) {
			throw new ModUpdateFailedException();
		}
	}
	
	public void updateMods() throws ModUpdateFailedException{
		for (Mod mod : sm.getMods()){
			updateMod(mod);
		}
	}
	
	public void enableMod(Mod mod) throws ModAlreadyEnabledException, ModNotDownloadedException, IOException {
		if (mod.isEnabled()){
			throw new ModAlreadyEnabledException();
		} else if (!isDownloaded(mod)){
			throw new ModNotDownloadedException();
		}
		
		Workflow wf = new Workflow("Enabling " + mod);
		ModWorkflowBuilder.enableMod(wf, mod, config, sm, cr);		
		submitEnablerWorkflow(wf);
	}
	
	public void disableMod(Mod mod) throws ModAlreadyDisabledException, IOException {
		if (!mod.isEnabled()){
			throw new ModAlreadyDisabledException();
		}
		
		Workflow wf = new Workflow("Disabling " + mod);
		ModWorkflowBuilder.disableMod(wf, mod, config, sm);
		submitEnablerWorkflow(wf);
	}
	
	public void deleteMod(Mod mod) throws CannotDisableModException, IOException {
		Workflow wf = new Workflow("Deleting " + mod);
		ModWorkflowBuilder.deleteMod(wf, mod, config, sm);		
		submitEnablerWorkflow(wf);
	}
	
	public void checkForModUpdates() throws Exception{
		Exception e = null;
		
		for (Mod mod : sm.getMods()){
			try {
				Workflow wf = new Workflow("Checking for update for " + mod);
				ModWorkflowBuilder.checkForUpdates(wf, mod.getPageUrl(), mod.getUpdatedOn(), mod.getNewestFileName(), mod, sm);
				submitDownloadWorkflow(wf);
			} catch (IOException | UnsupportedHostException ex) {
				ex.printStackTrace();
				e = ex;
			}
		}
		
		if (e != null){
			throw e;
		}
	}
	
	// -- Exceptions ------------------------------------------------------
	
	@SuppressWarnings("serial")
	public static class CannotAddModException extends Exception {}
	@SuppressWarnings("serial")
	public static class ModAlreadyEnabledException extends Exception {}
	@SuppressWarnings("serial")
	public static class ModAlreadyDisabledException extends Exception {}
	@SuppressWarnings("serial")
	public static class ModNotDownloadedException extends Exception {}
	@SuppressWarnings("serial")
	public static class CannotDisableModException extends Exception {}
	@SuppressWarnings("serial")
	public static class CannotEnableModException extends Exception {}
	@SuppressWarnings("serial")
	public static class ModUpdateFailedException extends Exception {}
}
