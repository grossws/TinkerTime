package aohara.tinkertime.controllers;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import aohara.tinkertime.models.Mod;
import aohara.tinkertime.models.ModStructure;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

public class ModStateManager implements ModUpdateListener {
	
	private final Gson gson;
	private final Path modsPath;
	private final Type modsType = new TypeToken<Set<Mod>>() {}.getType();
	
	private final Set<Mod> modCache = new HashSet<>();
	private final Map<Mod, ModStructure> structureCache = new HashMap<>();
	
	public ModStateManager(Path modsPath){
		gson = new Gson();
		this.modsPath = modsPath;
	}
	
	private Set<Mod> loadMods(){
		try(FileReader reader = new FileReader(modsPath.toFile())){
			Set<Mod> mods = gson.fromJson(reader, modsType);
			if (mods != null){
				return mods;
			}
		} catch (FileNotFoundException e){
			// No Action
		} catch (IOException e1) {
			e1.printStackTrace();
		}
		return new HashSet<Mod>();
	}
	
	private void saveMods(Set<Mod> mods){
		try(FileWriter writer = new FileWriter(modsPath.toFile())){
			gson.toJson(mods, modsType, writer);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public Set<Mod> getMods(){
		if (modCache.isEmpty()){
			modCache.addAll(loadMods());
		}
		return new HashSet<Mod>(modCache);
	}
	
	public Map<Mod, ModStructure> getModStructures(){
		if (structureCache.isEmpty()){
			for (Mod mod : getMods()){
				structureCache.put(mod, new ModStructure(mod));
			}
		}
		return new HashMap<Mod, ModStructure>(structureCache);
	}
	
	public Set<ModStructure> getStructures(){
		return new HashSet<ModStructure>(getModStructures().values());
	}

	@Override
	public void modUpdated(Mod mod) {
		modCache.clear();
		structureCache.clear();
		
		Set<Mod> mods = loadMods();
		if (mods.contains(mod)){
			mods.remove(mod);
		}
		mods.add(mod);
		
		saveMods(mods);
		
		modCache.addAll(mods);
	}	

}
