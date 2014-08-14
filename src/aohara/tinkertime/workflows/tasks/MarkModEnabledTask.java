package aohara.tinkertime.workflows.tasks;

import aohara.common.workflows.Workflow;
import aohara.common.workflows.tasks.WorkflowTask;
import aohara.tinkertime.controllers.ModStateManager;
import aohara.tinkertime.models.Mod;

public class MarkModEnabledTask extends WorkflowTask {
	
	private final Mod mod;
	private final ModStateManager sm;
	private final boolean markEnabled;

	public MarkModEnabledTask(Workflow workflow, Mod mod, ModStateManager sm, boolean enable) {
		super(workflow);
		this.mod = mod;
		this.sm = sm;
		markEnabled = enable;
	}

	@Override
	public Boolean call() throws Exception {
		mod.setEnabled(markEnabled);
		sm.modUpdated(mod, false);
		return true;
	}

	@Override
	public int getTargetProgress() throws InvalidContentException {
		return 1;
	}

}