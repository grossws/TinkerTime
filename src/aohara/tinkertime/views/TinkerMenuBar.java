package aohara.tinkertime.views;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.net.URL;
import java.util.Arrays;

import javax.swing.AbstractAction;
import javax.swing.JDialog;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;

import aohara.common.Util;
import aohara.common.selectorPanel.ListListener;
import aohara.tinkertime.Config;
import aohara.tinkertime.Constants;
import aohara.tinkertime.TinkerTime;
import aohara.tinkertime.controllers.ModManager;
import aohara.tinkertime.controllers.ModManager.CannotAddModException;
import aohara.tinkertime.controllers.ModManager.CannotDisableModException;
import aohara.tinkertime.controllers.ModManager.CannotEnableModException;
import aohara.tinkertime.controllers.ModManager.ModAlreadyDisabledException;
import aohara.tinkertime.controllers.ModManager.ModAlreadyEnabledException;
import aohara.tinkertime.controllers.ModManager.ModNotDownloadedException;
import aohara.tinkertime.controllers.ModManager.ModUpdateFailedException;
import aohara.tinkertime.controllers.crawlers.Crawler;
import aohara.tinkertime.controllers.crawlers.CrawlerFactory;
import aohara.tinkertime.controllers.fileUpdater.CurrentModuleManager;
import aohara.tinkertime.controllers.fileUpdater.CurrentTinkerTime;
import aohara.tinkertime.controllers.fileUpdater.CurrentVersion;
import aohara.tinkertime.controllers.fileUpdater.ModuleManagerDownloader;
import aohara.tinkertime.controllers.fileUpdater.TinkerTimeDownloader;
import aohara.tinkertime.models.Mod;

@SuppressWarnings("serial")
public class TinkerMenuBar extends JMenuBar implements ListListener<Mod>{
	
	private final ModManager mm;
	private Mod selectedMod;
	private final JPopupMenu popupMenu;
	
	public TinkerMenuBar(ModManager mm){
		this.mm = mm;
		
		JMenu fileMenu = new JMenu("File");
		fileMenu.add(new JMenuItem(new UpdatePathsAction()));
		fileMenu.add(new JMenuItem(new ExitAction()));
		add(fileMenu);
		
		JMenu modMenu = new JMenu("Mod");
		modMenu.add(new JMenuItem(new AddModAction()));
		modMenu.add(new JMenuItem(new EnableDisableModAction()));
		modMenu.add(new JMenuItem(new DeleteModAction()));
		add(modMenu);
		
		JMenu updateMenu = new JMenu("Update");
		updateMenu.add(new JMenuItem(new UpdateModAction()));
		updateMenu.add(new JMenuItem(new UpdateAllAction()));
		updateMenu.add(new JMenuItem(new CheckforUpdatesAction()));
		updateMenu.add(new JMenuItem(new UpdateModuleManagerAction()));
		updateMenu.add(new JMenuItem(new UpdateTinkerTimeAction()));
		add(updateMenu);
		
		JMenu helpMenu = new JMenu("Help");
		helpMenu.add(new JMenuItem(new AboutAction()));
		helpMenu.add(new JMenuItem(new HelpAction()));
		helpMenu.add(new JMenuItem(new ContactAction()));
		add(helpMenu);
		
		popupMenu = new JPopupMenu();
		popupMenu.add(new EnableDisableModAction());
		popupMenu.add(new DeleteModAction());
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
		
		@Override
		public void elementRightClicked(MouseEvent evt, Mod element) throws Exception {
			popupMenu.show((Component) evt.getSource(), evt.getX(), evt.getY());
		}
		
	// -- Actions ---------------------------------------------------
	
	private class AddModAction extends AbstractAction {
		
		private AddModAction(){
			super("Add Mod");
		}

		@Override
		public void actionPerformed(ActionEvent e) {
			// Get URL from user
			String urlString = JOptionPane.showInputDialog(
				getParent(),
				"Please enter the Curse.com URl of the mod you would like to"
				+ " add.\ne.g. http://www.curse.com/ksp-mods/kerbal/220221-mechjeb",
				"Enter Curse.com Mod URL",
				JOptionPane.QUESTION_MESSAGE
			);
			
			// Try to add Mod
			try {
				mm.addNewMod(urlString);
			} catch (CannotAddModException e1) {
				errorMessage(
					"Mod data could not be deciphered.\n"
					+ "Either the URL is invalid, or the site layout has been updated.\n"
					+ " Valid hosts are: " + Arrays.asList(Constants.ACCEPTED_MOD_HOSTS)
				);
			}
		}
	}
	
	private class DeleteModAction extends AbstractAction {
		
		private DeleteModAction(){
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
		
		private UpdateModAction(){
			this("Update Mod");
		}
		
		protected UpdateModAction(String string){
			super(string);
		}

		@Override
		public void actionPerformed(ActionEvent e) {
			if (selectedMod != null){
				mm.updateMod(selectedMod);
			}
		}
	}
	
	private class UpdateAllAction extends UpdateModAction {
		
		private UpdateAllAction() {
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
		
		private CheckforUpdatesAction(){
			super("Check for Updates");
		}

		@Override
		public void actionPerformed(ActionEvent e) {
			try {
				mm.checkForModUpdates();
			} catch (ModUpdateFailedException e1) {
				errorMessage("Error checking for updates.");
			}
		}
	}
	
	private class EnableDisableModAction extends AbstractAction {
		
		private EnableDisableModAction(){
			super("Enable/Disable");
		}

		@Override
		public void actionPerformed(ActionEvent e) {
			if (selectedMod != null && selectedMod.isEnabled()){
				try {
					mm.disableMod(selectedMod);
				} catch (ModAlreadyDisabledException
						| CannotDisableModException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
			} else if (selectedMod != null){
				try {
					mm.enableMod(selectedMod);
				} catch (ModAlreadyEnabledException | ModNotDownloadedException
						| CannotEnableModException | CannotDisableModException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
			}
		}
	}
	
	private class UpdatePathsAction extends AbstractAction {
		
		private UpdatePathsAction(){
			super("Update Paths");
		}

		@Override
		public void actionPerformed(ActionEvent e) {
			Config.updateConfig(true, false);
		}
	}
	
	private class ExitAction extends AbstractAction {
		
		private ExitAction(){
			super("Exit");
		}

		@Override
		public void actionPerformed(ActionEvent e) {
			System.exit(0);
		}
	}
	
	private class HelpAction extends AbstractAction {
		
		private HelpAction(){
			super("Help");
		}

		@Override
		public void actionPerformed(ActionEvent e) {
			try {
				Util.goToHyperlink(new URL("https://github.com/oharaandrew314/TinkerTime/blob/master/README.md"));
			} catch (IOException e1) {
				errorMessage("Error opening help");
			}
		}
	}
	
	private class AboutAction extends AbstractAction {
		
		private AboutAction(){
			super("About");
		}

		@Override
		public void actionPerformed(ActionEvent e) {
			String aboutText = String.format(
				"<html>%s v%s - by %s\n",
				TinkerTime.NAME,
				TinkerTime.VERSION,
				TinkerTime.AUTHOR
			);
			
			String licenseText = (
				"This work is licensed under the Creative Commons \n"
				+ "Attribution-ShareAlike 4.0 International License. \n"
			);
			
			try {
				Object[] message = {
					aboutText,
					"\n",
					licenseText,
					new UrlPanel("View a copy of this license", new URL("http://creativecommons.org/licenses/by-sa/4.0/")).getComponent()
				};
				JOptionPane.showMessageDialog(
						getParent(),
						message,
						"About " + TinkerTime.NAME,
						JOptionPane.INFORMATION_MESSAGE
					);
			} catch (IOException e1) {
				e1.printStackTrace();
			}
		}
	}
	
	private class ContactAction extends AbstractAction {
		
		private ContactAction(){
			super("Contact Me");
		}

		@Override
		public void actionPerformed(ActionEvent e) {
			try {
				Util.goToHyperlink(new URL("http://tinkertime.uservoice.com"));
			} catch (IOException e1) {
				errorMessage(e1.getMessage());
			}
		}
	}
	
	private class UpdateModuleManagerAction extends AbstractAction {
		
		private UpdateModuleManagerAction(){
			super("Update Module Manager");
		}

		@Override
		public void actionPerformed(ActionEvent e) {
			Config config = new Config();
			CurrentVersion currentVersion = new CurrentModuleManager(config);
			URL url = Constants.getModuleManagerJenkinsUrl();
			Crawler<?, ?> crawler = new CrawlerFactory().getCrawler(url);
			JDialog dialog = new FileUpdateDialog(
				"Update Module Manager", mm, url, currentVersion,
				new ModuleManagerDownloader(mm, crawler, config.getGameDataPath(), currentVersion)
			);
			dialog.setVisible(true);
		}
	}
	
	private class UpdateTinkerTimeAction extends AbstractAction {
		
		private UpdateTinkerTimeAction(){
			super("Update Tinker Time");
		}

		@Override
		public void actionPerformed(ActionEvent e) {
			CurrentVersion currentVersion = new CurrentTinkerTime();
			URL url = Constants.getTinkerTimeGithubUrl();
			JDialog dialog = new FileUpdateDialog(
				"Update Tinker Time",mm, url, currentVersion,
				new TinkerTimeDownloader(new CrawlerFactory().getCrawler(url))
			);
			dialog.setVisible(true);
		}
	}
}
