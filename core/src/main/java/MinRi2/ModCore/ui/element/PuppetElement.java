package MinRi2.ModCore.ui.element;

import arc.scene.*;

public class PuppetElement extends Element{
    private Element target;

    public void setTarget(Element target){
        this.target = target;

        updateState();
    }

    private void setPositionInternal(float x, float y){
        super.setPosition(x, y);
    }

    private void setSizeInternal(float width, float height){
        super.setSize(width, height);
    }

    private void updateState(){
        if(target == null){
            return;
        }

        setPositionInternal(target.x, target.y);
        setSizeInternal(target.getWidth(), target.getHeight());
        translation.set(target.translation);
    }

    @Override
    public void setPosition(float x, float y){
        target.setPosition(x, y);
        updateState();
    }

    @Override
    public void setPosition(float x, float y, int alignment){
        target.setPosition(x, y, alignment);
        updateState();
    }

    @Override
    public void moveBy(float x, float y){
        target.moveBy(x, y);
        updateState();
    }

    @Override
    public void setSize(float width, float height){
        target.setSize(width, height);
        updateState();
    }

    @Override
    public void sizeBy(float size){
        target.sizeBy(size);
        updateState();
    }

    @Override
    public void sizeBy(float width, float height){
        target.sizeBy(width, height);
        updateState();
    }

    @Override
    public void setBounds(float x, float y, float width, float height){
        target.setBounds(x, y, width, height);
        updateState();
    }

    @Override
    public void keepInStage(){
        target.keepInStage();
        updateState();
    }

    @Override
    public void setTranslation(float x, float y){
        target.setTranslation(x, y);
        updateState();
    }

    @Override
    public float getPrefWidth(){
        return target.getPrefWidth();
    }

    @Override
    public float getPrefHeight(){
        return target.getPrefHeight();
    }

    @Override
    public float getMinWidth(){
        return target.getMinWidth();
    }

    @Override
    public float getMinHeight(){
        return target.getMinHeight();
    }

    @Override
    public void draw(){
        super.draw();
        target.draw();
    }
}
