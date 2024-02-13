package MinRi2.ModCore.ui.operator;

import MinRi2.ModCore.io.*;
import MinRi2.ModCore.utils.*;

public class SavedTable extends OperableTable{
    private final DebounceTask savePositionTask, saveSizeTask;
    protected final MinModSettings settings;
    protected boolean savePosition, saveSize;

    public SavedTable(MinModSettings settings, String name, boolean savePosition, boolean saveSize){
        super(true);

        if(name == null && (savePosition || saveSize)){
            throw new RuntimeException("To save position and size, SavedTable must have a name." + this);
        }

        this.settings = settings;

        this.name = name;

        this.savePosition = savePosition;
        this.saveSize = saveSize;

        savePositionTask = new DebounceTask(1f, () -> {
            settings.put(name + ".pos.x", x);
            settings.put(name + ".pos.y", y);
        });

        saveSizeTask = new DebounceTask(1f, () -> {
            settings.put(name + ".size.width", width);
            settings.put(name + ".size.height", height);
        });
    }

    protected void readPosition(){
        if(savePosition){
            float x = settings.get(name + ".pos.x", this.x);
            float y = settings.get(name + ".pos.y", this.y);
            setPosition(x, y);
        }
    }

    protected void readSize(){
        if(saveSize){
            float width = settings.get(name + ".size.width", this.width);
            float height = settings.get(name + ".size.height", this.height);

            // 自定义大小缩放无法保证ui有至少的空间
            width = Math.max(getMinWidth(), width);
            height = Math.max(getMinHeight(), height);

            setSize(width, height);
        }
    }

    @Override
    public void onDragged(float deltaX, float deltaY){
        if(savePosition){
            savePositionTask.run();
        }
    }

    @Override
    public void onResized(float deltaWidth, float deltaHeight){
        if(saveSize){
            saveSizeTask.run();
        }
    }
}
