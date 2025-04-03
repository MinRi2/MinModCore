package MinRi2.ModCore.ui.operator;

import arc.scene.*;

public interface OperateConsumer{
    default boolean keepWithinStage(){
        return true;
    }

    default boolean alizable(Element element){
        return true;
    }

    default void onDragged(float newX, float newY){
    }

    default void onResized(float newWidth, float newHeight){
    }

    default void onAligned(Element snap, int alignFrom, int alignTo){
    }

    default void onReleased(){
    }
}