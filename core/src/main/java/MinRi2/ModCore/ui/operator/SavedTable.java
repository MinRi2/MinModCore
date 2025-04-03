package MinRi2.ModCore.ui.operator;

import MinRi2.ModCore.io.*;
import MinRi2.ModCore.utils.*;
import arc.util.*;

public class SavedTable extends OperableTable{
    public int saveAlign = Align.bottomLeft;

    protected final MinModSettings settings;
    protected boolean savePosition, saveSize;

    private float saveDelay = 0.5f;
    private DebounceTask savePositionTask, saveSizeTask;

    public SavedTable(MinModSettings settings, String name, boolean savePosition, boolean saveSize){
        super(true);

        if(name == null && (savePosition || saveSize)){
            throw new RuntimeException("To save position and size, SavedTable must have a name." + this);
        }

        this.settings = settings;

        this.name = name;

        this.savePosition = savePosition;
        this.saveSize = saveSize;

        if(savePosition){
            savePositionTask = new DebounceTask(saveDelay, () -> {
                settings.putSave(this.name + ".pos.x", getX(saveAlign));
                settings.putSave(this.name + ".pos.y", getY(saveAlign));
            });
        }

        if(saveSize){
            saveSizeTask = new DebounceTask(saveDelay, () -> {
                settings.putSave(this.name + ".size.width", width);
                settings.putSave(this.name + ".size.height", height);
            });
        }
    }

    protected void readPosition(){
        if(savePosition){
            float x = settings.get(name + ".pos.x", getX(saveAlign));
            float y = settings.get(name + ".pos.y", getY(saveAlign));
            setPosition(x, y, saveAlign);
        }
    }

    protected void readSize(){
        if(saveSize){
            float width = settings.get(name + ".size.width", this.width);
            float height = settings.get(name + ".size.height", this.height);

            // 自定义大小缩放无法保证ui有最低需求的空间
            width = Math.max(getMinWidth(), width);
            height = Math.max(getMinHeight(), height);

            setSize(width, height);
        }
    }

    public void savePosition(){
        if(savePosition){
            savePositionTask.run();
        };
    }

    public void saveSize(){
        if(saveSize){
            saveSizeTask.run();
        }
    }

    public void setSaveDelay(float saveDelay){
        if(this.saveDelay != saveDelay){
            this.saveDelay = saveDelay;

            if(savePositionTask != null){
                savePositionTask.setDelay(saveDelay);
            }

            if(saveSizeTask != null){
                saveSizeTask.setDelay(saveDelay);
            }
        }
    }

    @Override
    public void onDragged(float newX, float newY){
        savePosition();
    }

    @Override
    public void onResized(float newWidth, float newHeight){
        saveSize();
    }
}
