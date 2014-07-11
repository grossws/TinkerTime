package aohara.tinkertime.views;

import java.awt.event.ActionEvent;
import java.net.URI;
import java.net.URISyntaxException;

import javax.swing.AbstractAction;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;

import aohara.common.selectorPanel.ListListener;
import aohara.tinkertime.config.Config;
import aohara.tinkertime.controllers.ModManager;
import aohara.tinkertime.controllers.ModManager.CannotAddModException;
import aohara.tinkertime.controllers.ModManager.CannotDisableModException;
import aohara.tinkertime.controllers.ModManager.ModUpdateFailedException;
import aohara.tinkertime.models.Mod;

@SuppressWarnings("serial")
public class TinkerMenuBar extends JMenuBar implements ListListener<Mod>{
	
	private final ModManager mm;
	private Mod selectedMod;
	
	public TinkerMenuBar(ModManager mm){
		this.mm = mm;
		
		JMenu fileMenu = new JMenu("File");
		fileMenu.add(new JMenuItem(new UpdatePathsAction()));
		fileMenu.add(new JMenuItem(new ExitAction()));
		add(fileMenu);
		
		JMenu modMenu = new JMenu("Mod");
		modMenu.add(new JMenuItem(new AddModAction()));
		modMenu.add(new JMenuItem(new DeleteModAction()));
		add(modMenu);
		
		JMenu updateMenu = new JMenu("Update");
		updateMenu.add(new JMenuItem(new UpdateModAction()));
		updateMenu.add(new JMenuItem(new UpdateAllAction()));
		updateMenu.add(new JMenuItem(new CheckforUpdatesAction()));
		add(updateMenu);
	}
	
	private void errorMessage(String message){
		JOptionPane.showMessageDialog(
			getParent(), message, "Error", JOptionPane.ERROR_MESSAGE);
	}
	
	// -- Listeners --------------------------------------------------

		@Override
		public void elementClicked(Mod element, int numTimes) {
			// Do Nothing
		}

		@Override
		public void elementSelected(Mod element) {
			selectedMod = element;
		}
		
	// -- Actions ---------------------------------------------------
	
	private class AddModAction extends AbstractAction {
		
		public AddModAction(){
			super("Add Mod");
		}

		@Override
		public void actionPerformed(ActionEvent e) {
			// Get URL from user
			String url = JOptionPane.showInputDialog(
				getParent(),
				"Please enter the Curse.com URl of the mod you would like to"
				+ " add.\ne.g. http://www.curse.com/ksp-mods/kerbal/220221-mechjeb",
				"Enter Curse.com Mod URL",
				JOptionPane.QUESTION_MESSAGE
			);
			
			// Cancel if not input given
			if (url == null || url.trim().isEmpty()){
				return;
			}
			
			// Check if URL is valid
			try {
				URI uri = new URI(url);
				if (!uri.getHost().contains("curse.com")){
					throw new URISyntaxException(
						url,
						"Currently, only mods from curse.com are accepted.\n"
					);
				}
			} catch (URISyntaxException e2) {
				errorMessage(e2.getMessage());
				return;
			}
			
			// Try to add Mod
			try {
				mm.addNewMod(url);
			} catch (CannotAddModException e1) {
				errorMessage(
					"Error Extracting Mod Info From Page.\n"
					+ "Either Curse.com has been updated,"
					+ "or this is an invalid link."
				);
			}
		}
	}
	
	private class DeleteModAction extends AbstractAction {
		
		public DeleteModAction(){
			super("Delete");
		}

		@Override
		public void actionPerformed(ActionEvent e) {
			if (selectedMod != null){
				try {
					if (JOptionPane.showConfirmDialog(
						getParent(),
						"Are you sure you want to delete "
						+ selectedMod.getName() + "?",
						"Delete?",
						JOptionPane.YES_NO_OPTION
					) == JOptionPane.YES_OPTION){
						mm.deleteMod(selectedMod);
					}
				} catch (CannotDisableModException e1) {
					errorMessage(selectedMod.getName() + " could not be disabled.");
				}
			}
		}
	}
	
	private class UpdateModAction extends AbstractAction {
		
		public UpdateModAction(){
			this("Update Mod");
		}
		
		protected UpdateModAction(String string){
			super(string);
		}

		@Override
		public void actionPerformed(ActionEvent e) {
			if (selectedMod != null){
				updateMod(selectedMod);
			}
		}
		
		protected void updateMod(Mod mod) {
			try {
				mm.updateMod(selectedMod);
			} catch (ModUpdateFailedException e1) {
				errorMessage("There was an error updating " + mod.getName());
				e1.printStackTrace();
			}
		}
	}
	
	private class UpdateAllAction extends UpdateModAction {
		
		public UpdateAllAction() {
			super("Update All");
		}

		@Override
		public void actionPerformed(ActionEvent e) {
			try {
				mm.updateMods();
			} catch (ModUpdateFailedException e1) {
				errorMessage("One or more mods failed to update");
			}
		}
	}
	
	private class CheckforUpdatesAction extends AbstractAction {
		
		public CheckforUpdatesAction(){
			super("Check for Updates");
		}

		@Override
		public void actionPerformed(ActionEvent e) {
			try {
				mm.checkForUpdates();
			} catch (ModUpdateFailedException e1) {
				errorMessage("Error checking for updates.");
			}
		}
	}
	
	private class UpdatePathsAction extends AbstractAction {
		
		public UpdatePathsAction(){
			super("Update Paths");
		}

		@Override
		public void actionPerformed(ActionEvent e) {
			Config.updateConfig(false);
		}
	}
	
	private class ExitAction extends AbstractAction {
		
		public ExitAction(){
			super("Exit");
		}

		@Override
		public void actionPerformed(ActionEvent e) {
			System.exit(0);
		}
	}
}
