package MinRi2.ModCore.ui.operator;

import arc.scene.*;

public interface OperateConsumer{
    default boolean keepWithinStage(){
        return true;
    }

    default boolean alizable(Element element){
        return true;
    }

    default void onDragged(float deltaX, float deltaY){
    }

    default void onResized(float deltaX, float deltaY){
    }

    default void onAligned(Element snap, int alignFrom, int alignTo){
    }

    default void onReleased(){
    }
}