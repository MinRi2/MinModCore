package MinRi2.ModCore.io;

import MinRi2.ModCore.utils.*;
import arc.files.*;
import arc.struct.*;
import arc.util.*;
import arc.util.io.*;
import mindustry.mod.Mods.*;

import java.io.*;

import static mindustry.Vars.*;

public class MinModSettings{
    protected static final byte typeBool = 0, typeInt = 1, typeLong = 2, typeFloat = 3, typeString = 4;

    public static boolean debug = false;
    public static final Seq<MinModSettings> modSettings = new Seq<>();
    public static final Fi minModSettingsRoot = modDirectory.child("MinModCore").child("settings");

    public final LoadedMod mod;
    public Fi settingsFi, backupFi;

    private final Seq<MinSettingEntry> mSettings = new Seq<>();

    private boolean loaded = false;
    public boolean modified = false;

    static{
        minModSettingsRoot.mkdirs();

        Timer.schedule(() -> {
            for(MinModSettings modSetting : modSettings){
                if(modSetting.modified){
                    modSetting.save();
                    Log.info("Save @", modSetting.mod.meta.displayName);
                }
            }
            Log.info("Save done");
        }, 10, 30);
    }

    private MinModSettings(LoadedMod mod){
        this.mod = mod;
    }

    public static MinModSettings registerSettings(String modName){
        LoadedMod mod = ModsHelper.getMod(modName);
        MinModSettings settings = new MinModSettings(mod);
        settings.init();
        modSettings.add(settings);
        return settings;
    }

    public void init(){
        String modName = mod.meta.name;

        settingsFi = minModSettingsRoot.child(modName);
        backupFi = minModSettingsRoot.child(modName + ".backup");



        load();
        loaded = true;
    }

    public MinSettingEntry findSetting(String name){
        return mSettings.find(s -> s.name.equals(name));
    }

    public void put(String name, Object obj){
        put(name, obj, false, false);
    }

    public void put(String name, Object obj, boolean isDef, boolean forceSave){
        MinSettingEntry ms = findSetting(name);

        if(ms != null){
            if(isDef) return;
            ms.value = obj;
        }else{
            mSettings.add(new MinSettingEntry(name, obj));
        }
        modified = true;

        if(forceSave) save();
    }

    public <T> T get(String name, T def){
        MinSettingEntry setting = findSetting(name);
        if(setting == null){
            return def;
        }
        return (T)setting.value;
    }

    public boolean getBool(String name){
        return get(name, false);
    }

    public boolean getBool(String name, boolean def){
        return get(name, def);
    }

    public int getInt(String name){
        return get(name, 0);
    }

    public int getInt(String name, int def){
        return get(name, def);
    }

    public long getLong(String name){
        return get(name, 0L);
    }

    public long getLong(String name, long def){
        return get(name, def);
    }

    public float getFloat(String name){
        return get(name, 0f);
    }

    public float getFloat(String name, float def){
        return get(name, def);
    }

    public String getString(String name){
        return get(name, "");
    }

    public String getString(String name, String def){
        return get(name, def);
    }

    private void loadSettings(Fi fi) throws IOException{
        mSettings.clear();

        Reads reads = fi.reads();

        try{
            int size = reads.i();
            for(int i = 0; i < size; i++){
                String name = reads.str();

                byte type = reads.b();

                Object value;
                switch(type){
                    case typeBool -> value = reads.bool();
                    case typeInt -> value = reads.i();
                    case typeLong -> value = reads.l();
                    case typeFloat -> value = reads.f();
                    case typeString -> value = reads.str();
                    default -> throw new IOException("MinerToolsSettings: Field to load type: " + type);
                }

                mSettings.add(new MinSettingEntry(name, value));

                if(debug){
                    Log.infoTag("MinerToolsSettings", "Read setting " + name + ": " + value);
                }
            }
        }catch(Exception e){
            fi.delete();

            if(debug){
                Log.err(e);
            }
        }

        reads.close();
        modified = false;
    }

    public void load(){
        loaded = true;

        checkFile();
        try{
            loadSettings(settingsFi);
        }catch(IOException exception){
            try{
                loadSettings(backupFi);
            }catch(IOException ignored){
            }
        }
    }

    public void save(){
        if(!loaded) return;

        checkFile();
        settingsFi.copyTo(backupFi);

        var writes = settingsFi.writes();

        writes.i(mSettings.size);
        for(MinSettingEntry setting : mSettings){

            Object value = setting.value;

            writes.str(setting.name);

            if(value instanceof Boolean b){
                writes.b(typeBool);
                writes.bool(b);
            }else if(value instanceof Integer i){
                writes.b(typeInt);
                writes.i(i);
            }else if(value instanceof Long l){
                writes.b(typeLong);
                writes.l(l);
            }else if(value instanceof Float f){
                writes.b(typeFloat);
                writes.f(f);
            }else if(value instanceof String s){
                writes.b(typeString);
                writes.str(s);
            }

            if(debug){
                Log.infoTag("MinerToolsSettings", "Write setting " + setting.name + ": " + value);
            }
        }

        writes.close();
        modified = false;
    }

    private void checkFile(){
        try{
            if(!settingsFi.exists()){
                settingsFi.file().createNewFile();
            }

            if(!backupFi.exists()){
                backupFi.file().createNewFile();
            }
        }catch(IOException e){
            throw new RuntimeException(e);
        }
    }

    public static class MinSettingEntry{
        public String name;
        public Object value;

        public MinSettingEntry(String name, Object value){
            this.name = name;
            this.value = value;
        }
    }
}
