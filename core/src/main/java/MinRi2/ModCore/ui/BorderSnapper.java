package MinRi2.ModCore.ui;

import MinRi2.ModCore.math.*;
import arc.math.geom.*;
import arc.scene.*;
import arc.scene.actions.*;
import arc.util.*;

public class BorderSnapper{
    // 目标元素
    private final Element targetElem;

    // 吸附的元素
    public Element snapElem;

    public boolean pause;

    // 吸附元素左下角相对于目标元素左下角的坐标
    private float relativeX, relativeY;
    // 记录吸附元素的坐标，用于判断坐标是否改变
    private float lastX, lastY;

    public BorderSnapper(Element targetElem){
        this.targetElem = targetElem;

        // 植入Action
        targetElem.addAction(Actions.forever(Actions.run(this::updateSnap)));
    }

    public void cancelSnapping(){
        snapElem = null;
    }

    public boolean setSnap(Element snapElem, int snapAlign){
        if(canSnap(snapElem, snapAlign)){
            this.snapElem = snapElem;

            Vec2 p1 = Tmp.v1.setZero(), p2 = Tmp.v2.setZero();
            targetElem.localToStageCoordinates(p1);
            snapElem.localToStageCoordinates(p2);

            relativeX = p2.x - p1.x;
            relativeY = p2.y - p1.y;
            return true;
        }

        return false;
    }

    public void resume(){
        pause = false;
    }

    public void pause(){
        pause = true;
    }

    private void updateSnap(){
        if(pause || snapElem == null) return;

        if(updateSnapChanged()){
            Vec2 p = Tmp.v1.setZero();
            snapElem.localToStageCoordinates(p);

            targetElem.setPosition(p.x + relativeX, p.y + relativeY);
            targetElem.keepInStage();
        }
    }

    private boolean updateSnapChanged(){
        float sx = snapElem.x;
        float sy = snapElem.y;

        if(sx != lastX || sy != lastY){
            lastX = sx;
            lastY = sy;
            return true;
        }
        return false;
    }

    private boolean canSnap(Element snapElem, int snapAlign){
        if(snapElem == null || snapAlign == 0){
            return false;
        }

        Rect targetBounds = ElementUtils.getBoundsOnScene(targetElem, Tmp.r1);
        Rect snapBounds = ElementUtils.getBoundsOnScene(snapElem, Tmp.r2);
        return GeometryUtils.hasCommonEdge(targetBounds, snapBounds, snapAlign);
    }

}
