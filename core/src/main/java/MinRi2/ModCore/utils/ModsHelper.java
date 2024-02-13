package MinRi2.ModCore.utils;

import mindustry.*;
import mindustry.mod.Mods.*;

/**
 * @author minri2
 * Create by 2024/2/13
 */
public class ModsHelper{
    public static LoadedMod getMod(String modName){
        LoadedMod mod = Vars.mods.locateMod(modName);

        if(mod == null){
            throw new RuntimeException("Mod '" + modName + "' doesn't exit. Please check your modName parameter.");
        }

        return mod;
    }
}
