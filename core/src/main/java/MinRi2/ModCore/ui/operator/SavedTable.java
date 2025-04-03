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
            settings.putSave(this.name + ".pos.x", x);
            settings.putSave(this.name + ".pos.y", y);
        });

        saveSizeTask = new DebounceTask(1f, () -> {
            settings.putSave(this.name + ".size.width", width);
            settings.putSave(this.name + ".size.height", height);
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

    @Override
    public void onDragged(float newX, float newY){
        savePosition();
    }

    @Override
    public void onResized(float newWidth, float newHeight){
        saveSize();
    }
}
