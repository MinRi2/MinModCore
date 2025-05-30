package MinRi2.ModCore.ui.operator;

import MinRi2.ModCore.ui.*;
import MinRi2.ModCore.ui.element.*;
import arc.*;
import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.input.*;
import arc.math.*;
import arc.math.geom.*;
import arc.scene.*;
import arc.scene.event.*;
import arc.scene.style.*;
import arc.scene.ui.layout.*;
import arc.struct.*;
import arc.util.*;
import arc.util.pooling.*;
import mindustry.*;
import mindustry.game.EventType.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.ui.*;

/**
 * 元素操作
 * 可以对元素进行的大小缩放和位置拖拽的缩放
 * @author minri2
 */
public class ElementOperator{
    // 可操作的表
    static final Seq<OperableTable> operableTables = new Seq<>();
    // 原版可对齐的元素
    static final Seq<Element> vanillaElements = new Seq<>();

    // 当前对齐的线，用于绘制 (Scene坐标系)
    private static final ObjectSet<Float> verticalLines = new ObjectSet<>();
    private static final ObjectSet<Float> horizontalLines = new ObjectSet<>(); // FloatSeq?

    // 大小边框所占比例(中间是拖拽)
    private static final float resizeBorderRatio = 2f / 10f;
    // 距离小于此值时对齐
    private static final float alignBorder = 4f;

    public static boolean operating = false;
    public static boolean dragMode, resizeMode;
    // 操作目标元素
    private static Element target;
    private static PuppetElement puppet;
    private static Element hitter;
    private static @Nullable OperateConsumer consumer;
    private static OperatorBackground background;
    private static boolean initialized;
    private static int touchEdge;

    static{
        Events.on(ResetEvent.class, e -> Core.app.post(ElementOperator::getVanillaElements));
        Events.on(ResizeEvent.class, e -> Core.app.post(ElementOperator::getVanillaElements));
    }

    private ElementOperator(){
    }

    /**
     * 获取原版可对齐的元素
     */
    private static void getVanillaElements(){
        vanillaElements.clear();

        Group hudGroup = Vars.ui.hudGroup;

        Element mainStack = Reflect.get(Vars.ui.hudfrag.blockfrag, "mainStack"),
        wavesTable = hudGroup.find("waves"),
        minimap = hudGroup.find("minimap"),
        position = hudGroup.find("position"),
        coreInfo = hudGroup.find("coreinfo");

        vanillaElements.addAll(mainStack, wavesTable, minimap, position, coreInfo);
    }

    private static void clearAlignLines(){
        verticalLines.clear();
        horizontalLines.clear();
    }

    private static void init(){
        background = new OperatorBackground();
        puppet = new OperatorPuppet();
        hitter = new Element();

        background.addChild(hitter);
        background.addChild(puppet);

        puppet.addListener(new InputListener(){
            float startX, startY; // 开始触碰元素的坐标（元素坐标系）
            float lastWidth, lastHeight;

            @Override
            public boolean touchDown(InputEvent event, float x, float y, int pointer, KeyCode button){
                startX = x;
                startY = y;

                lastWidth = puppet.getWidth();
                lastHeight = puppet.getHeight();

                updateEdge(x, y);

                clearAlignLines();

                return true;
            }

            @Override
            public void touchDragged(InputEvent event, float dragX, float dragY, int pointer){
                float deltaX = dragX - startX;
                float deltaY = dragY - startY;

                clearAlignLines();

                if(dragMode){
                    updateDragMode(deltaX, deltaY);
                }else if(resizeMode){
                    updateResizeMode(deltaX, deltaY, lastWidth, lastHeight);
                }
            }

            @Override
            public void touchUp(InputEvent event, float x, float y, int pointer, KeyCode button){
                clearAlignLines();
            }
        });

        hitter.setFillParent(true);
        hitter.addListener(new InputListener(){
            @Override
            public boolean touchDown(InputEvent event, float x, float y, int pointer, KeyCode button){
                Vec2 v = Pools.obtain(Vec2.class, Vec2::new);
                Vec2 scenePos = hitter.localToStageCoordinates(v.set(x, y));

                OperableTable operableTable = operableTables.find(o -> {
                    if(!o.visible || !o.hasParent()){
                        return false;
                    }

                    Vec2 pos = o.stageToLocalCoordinates(Tmp.v2.set(scenePos));
                    return ElementUtils.isOverlays(o, pos.x, pos.y);
                });

                if(operableTable != null && operableTable != target){
                    operableTable.operate();
                }else if(Core.scene.hit(scenePos.x, scenePos.y, true) == hitter){
                    // Scene会将触摸焦点转移到背景上, 下一帧再隐藏
                    Core.app.post(ElementOperator::hide);
                }

                v.setZero();
                Pools.free(v);

                return true;
            }
        });

        initialized = true;
    }

    /**
     * 操作元素
     * @param element 操作的元素
     */
    public static void operate(Element element){
        operate(element, null);
    }

    /**
     * 操作元素
     * @param element 操作的元素
     * @param operateConsumer 操作传感
     */
    public static void operate(Element element, @Nullable OperateConsumer operateConsumer){

        if(!initialized){
            init();
        }

        if(!operable(element)){
            return; // throw an error?
        }

        if(target != null){
            consumer.onReleased();
        }

        target = element;
        consumer = operateConsumer;

        puppet.setTarget(target);

        clearAlignLines();

        show();
    }

    public static boolean operating(Element element){
        return target == element;
    }

    public static boolean operable(Element element){
        return element != null && element.visible && element.hasParent();
    }

    private static boolean alizable(Element element){
        return element != null && element.visible
        && element.hasParent() && element != target
        && (consumer == null || consumer.alizable(element));
    }

    private static void show(){
        if(operating){
            return;
        }

        operating = true;
        background.show();
    }

    private static void hide(){
        if(!operating){
            return;
        }

        target = null;
        operating = false;
        background.hide();
    }

    // x, y: 元素坐标
    private static void updateEdge(float x, float y){
        float width = puppet.getWidth(), height = puppet.getHeight();
        float borderX = width * resizeBorderRatio, borderY = height * resizeBorderRatio;

        dragMode = resizeMode = false;
        touchEdge = 0;

        if(x >= borderX && x <= width - borderX
        && y >= borderY && y <= height - borderY){
            dragMode = true;
            touchEdge = Align.center;
            return;
        }

        resizeMode = true;

        if(x < borderX){
            touchEdge |= Align.left;
        }else if(x > width - borderX){
            touchEdge |= Align.right;
        }

        if(y < borderY){
            touchEdge |= Align.bottom;
        }else if(y > height - borderY){
            touchEdge |= Align.top;
        }
    }

    private static void updateDragMode(float deltaX, float deltaY){
        puppet.moveBy(deltaX, deltaY);

        if(consumer != null){
            if(consumer.keepWithinStage()){
                puppet.keepInStage();
            }

            consumer.onDragged(puppet.x, puppet.y);
        }

        updateDragAlign();
    }

    private static void updateResizeMode(float deltaX, float deltaY, float lastWidth, float lastHeight){
        boolean keepInStage = consumer != null && consumer.keepWithinStage();

        float width = puppet.getWidth();
        float height = puppet.getHeight();

        float minWidth = puppet.getMinWidth();
        float minHeight = puppet.getMinHeight();

        float maxWidth = puppet.getScene().getWidth();
        float maxHeight = puppet.getScene().getHeight();

        float deltaWidth = width - lastWidth;
        float deltaHeight = height - lastHeight;

        // 木偶元素坐标与Scene坐标系下的相同
        float x = puppet.x;
        float y = puppet.y;

        if(Align.isLeft(touchEdge)){
            if(width - deltaX < minWidth) deltaX = -(minWidth - width);
            if(keepInStage && x + deltaX < 0) deltaX = -x;
            width -= deltaX;
            x += deltaX;
        }
        if(Align.isBottom(touchEdge)){
            if(height - deltaY < minHeight) deltaY = -(minHeight - height);
            if(keepInStage && y + deltaY < 0) deltaY = -y;
            height -= deltaY;
            y += deltaY;
        }
        if(Align.isRight(touchEdge)){
            deltaX -= deltaWidth; // 消除宽增的坐标影响
            if(width + deltaX < minWidth) deltaX = minWidth - width;
            if(keepInStage && x + width + deltaX > maxWidth)
                deltaX = maxWidth - x - width;
            width += deltaX;
        }
        if(Align.isTop(touchEdge)){
            deltaY -= deltaHeight; // 消除高增的坐标影响
            if(height + deltaY < minHeight) deltaY = minHeight - height;
            if(keepInStage && y + height + deltaY > maxHeight)
                deltaY = maxHeight - y - height;
            height += deltaY;
        }

        puppet.setBounds(x, y, width, height);

        target.invalidateHierarchy();

        if(consumer != null){
            consumer.onResized(width, height);
        }

        updateResizeAlign();
    }

    private static void updateDragAlign(){
        for(Element element : vanillaElements){
            if(alizable(element)){
                updateDragAlign(element);
            }
        }

        for(OperableTable table : operableTables){
            if(alizable(table)){
                updateDragAlign(table);
            }
        }
    }

    private static void updateDragAlign(Element element){
        // 木偶元素坐标与Scene坐标系下的相同
        float x = puppet.x, y = puppet.y;
        float w = puppet.getWidth(), h = puppet.getHeight();

        // 对齐元素坐标不一定等于Scene坐标系下的，先做转换
        Vec2 v = Pools.obtain(Vec2.class, Vec2::new);
        ElementUtils.getOriginOnScene(element, v);
        float ex = v.x, ey = v.y;
        float ew = element.getWidth(), eh = element.getHeight();

        // 木偶元素边界
        float left = x, right = x + w;
        float bottom = y, top = y + h;

        // 对齐元素边界
        float eleft = ex, eright = ex + ew;
        float ebottom = ey, etop = ey + eh;

        float alignX = x, alignY = y;
        int alignFrom = 0, alignTo = 0;

        boolean aligned = false;

        if(Math.abs(left - eleft) <= alignBorder){ // 左边往左边贴
            alignX = eleft;

            verticalLines.add(eleft);

            alignFrom |= Align.left;
            alignTo |= Align.left;

            aligned = true;
        }else if(Math.abs(right - eleft) <= alignBorder){ // 右边往左边贴
            alignX = eleft - w;

            verticalLines.add(eleft);

            alignFrom |= Align.right;
            alignTo |= Align.left;

            aligned = true;
        }

        if(Math.abs(left - eright) <= alignBorder){ // 左边往右边贴
            alignX = eright;

            verticalLines.add(eright);

            alignFrom |= Align.left;
            alignTo |= Align.right;

            aligned = true;
        }else if(Math.abs(right - eright) <= alignBorder){ // 右边往右边贴
            alignX = eright - w;

            verticalLines.add(eright);

            alignFrom |= Align.right;
            alignTo |= Align.right;

            aligned = true;
        }

        if(Math.abs(bottom - ebottom) <= alignBorder){ // 下边往下边贴
            alignY = ebottom;

            horizontalLines.add(ebottom);

            alignFrom |= Align.bottom;
            alignTo |= Align.bottom;

            aligned = true;
        }else if(Math.abs(top - ebottom) <= alignBorder){ // 上边往下边贴
            alignY = ebottom - h;

            horizontalLines.add(ebottom);

            alignFrom |= Align.top;
            alignTo |= Align.bottom;

            aligned = true;
        }

        if(Math.abs(bottom - etop) <= alignBorder){ // 下边往上边贴
            alignY = etop;

            horizontalLines.add(etop);

            alignFrom |= Align.bottom;
            alignTo |= Align.top;

            aligned = true;
        }else if(Math.abs(top - etop) <= alignBorder){ // 上边往上边贴
            alignY = etop - h;

            horizontalLines.add(etop);

            alignFrom |= Align.top;
            alignTo |= Align.top;

            aligned = true;
        }

        puppet.setPosition(alignX, alignY);

        if(consumer != null && aligned){
            consumer.onAligned(element, alignFrom, alignTo);
        }

        v.setZero();
        Pools.free(v);
    }

    private static void updateResizeAlign(){
        for(Element element : vanillaElements){
            if(alizable(element)){
                updateResizeAlign(element);
            }
        }

        for(OperableTable table : operableTables){
            if(alizable(table)){
                updateResizeAlign(table);
            }
        }
    }

    private static void updateResizeAlign(Element element){
        // 木偶元素坐标与Scene坐标系下的相同
        float x = puppet.x, y = puppet.y;
        float w = puppet.getWidth(), h = puppet.getHeight();

        // 对齐元素坐标不一定等于Scene坐标系下的，先做转换
        Vec2 v = Pools.obtain(Vec2.class, Vec2::new);
        ElementUtils.getOriginOnScene(element, v);
        float ex = v.x, ey = v.y;
        float ew = element.getWidth(), eh = element.getHeight();

        // 木偶元素边界
        float left = x, right = x + w;
        float bottom = y, top = y + h;

        // 对齐元素边界
        float eleft = ex, eright = ex + ew;
        float ebottom = ey, etop = ey + eh;

        float alignX = x, alignY = y;
        float alignWidth = w, alignHeight = h;
        int alignFrom = 0, alignTo = 0;

        boolean aligned = false;

        if(Align.isRight(touchEdge)){
            alignFrom |= Align.right;

            if(Math.abs(right - eleft) <= alignBorder){ // 右边往左边贴
                alignWidth = eleft - left;

                verticalLines.add(eleft);

                alignTo |= Align.left;
                aligned = true;
            }else if(Math.abs(right - eright) <= alignBorder){ // 右边往右边贴
                alignWidth = eright - left;

                verticalLines.add(eright);

                alignTo |= Align.right;
                aligned = true;
            }
        }else if(Align.isLeft(touchEdge)){
            alignFrom |= Align.left;

            if(Math.abs(left - eleft) <= alignBorder){ // 左边往左边贴
                alignX = eleft;
                alignWidth = right - eleft;

                verticalLines.add(eleft);

                alignTo |= Align.left;
                aligned = true;
            }else if(Math.abs(left - eright) <= alignBorder){ // 左边往右边贴
                alignX = eright;
                alignWidth = right - eright;

                verticalLines.add(eright);

                alignTo |= Align.right;
                aligned = true;
            }
        }

        if(Align.isTop(touchEdge)){
            alignFrom |= Align.top;

            if(Math.abs(top - ebottom) <= alignBorder){ // 上边往下边贴
                alignHeight = ebottom - bottom;

                horizontalLines.add(ebottom);

                alignTo |= Align.bottom;
                aligned = true;
            }else if(Math.abs(top - etop) <= alignBorder){ // 上边贴上边
                alignHeight = etop - bottom;

                horizontalLines.add(etop);

                alignTo |= Align.top;
                aligned = true;
            }
        }else if(Align.isBottom(touchEdge)){
            alignFrom |= Align.bottom;

            if(Math.abs(bottom - ebottom) <= alignBorder){ // 下边贴下边
                alignY = ebottom;
                alignHeight = top - ebottom;

                horizontalLines.add(ebottom);

                alignFrom |= Align.bottom;
                aligned = true;
            }else if(Math.abs(bottom - etop) <= alignBorder){ // 下边贴上边
                alignY = etop;
                alignHeight = top - etop;

                horizontalLines.add(etop);

                alignTo |= Align.top;
                aligned = true;
            }
        }

        puppet.setBounds(alignX, alignY, alignWidth, alignHeight);

        if(consumer != null && aligned){
            consumer.onAligned(element, alignFrom, alignTo);
        }

        v.setZero();
        Pools.free(v);
    }

    private static class OperatorPuppet extends PuppetElement{
        private static void drawPuppet(float x, float y, float width, float height){
            float halfWidth = width / 2, halfHeight = height / 2;
            float halfX = x + halfWidth, halfY = y + halfHeight;

            // 单边宽高
            float borderWidth = width * resizeBorderRatio, borderHeight = height * resizeBorderRatio;

            float halfXAxes = (width - borderWidth) / 2;
            float halfYAxes = (height - borderHeight) / 2;

            // 边框
            Drawf.dashRect(Pal.accent, x, y, width, borderHeight);
            Drawf.dashRect(Pal.accent, x, y, borderWidth, height);
            Drawf.dashRect(Pal.accent, x + halfXAxes * 2, y, borderWidth, height);
            Drawf.dashRect(Pal.accent, x, y + halfYAxes * 2, width, borderHeight);

            // 填充
            Draw.color(Pal.accent, 0.3f);
            Fill.rect(halfX, halfY, width, height);

            // 四方的小图标提示
            float size = 16;
            Draw.color(Color.green, 1f);

            TextureRegion dragRegion = Icon.add.getRegion();
            Draw.rect(dragRegion, halfX, halfY, size, size);

            TextureRegion upRegion = Icon.up.getRegion();
            TextureRegion downRegion = Icon.down.getRegion();
            TextureRegion leftRegion = Icon.left.getRegion();
            TextureRegion rightRegion = Icon.right.getRegion();

            Draw.rect(upRegion, halfX, halfY + halfYAxes, size, size);
            Draw.rect(downRegion, halfX, halfY - halfYAxes, size, size);
            Draw.rect(rightRegion, halfX + halfXAxes, halfY, size, size);
            Draw.rect(leftRegion, halfX - halfXAxes, halfY, size, size);

            // 角落的小图标提示
            float degree = Mathf.atan2(width, height) * Mathf.radiansToDegrees;
            Draw.rect(rightRegion, halfX + halfXAxes, halfY + halfYAxes, size, size, degree); // ↗
            Draw.rect(rightRegion, halfX + halfXAxes, halfY - halfYAxes, size, size, -degree); // ↘
            Draw.rect(rightRegion, halfX - halfXAxes, halfY + halfYAxes, size, size, 180 - degree); // ↖
            Draw.rect(rightRegion, halfX - halfXAxes, halfY - halfYAxes, size, size, degree - 180); // ↙

            Draw.reset();
        }

        @Override
        public void draw(){
            super.draw();

            drawPuppet(x, y, width, height);
        }

    }

    private static class OperatorBackground extends WidgetGroup{
        public Drawable background;

        public OperatorBackground(){
            background = Styles.black6;
            setFillParent(true);
        }

        private void drawOperableTable(float x, float y, float width, float height){
            float halfWidth = width / 2, halfHeight = height / 2;
            float halfX = x + halfWidth, halfY = y + halfHeight;

            // 边框
            Drawf.dashRect(Pal.lightishGray, x, y, width, height);

            // 填充
            Draw.color(Pal.lightishGray, 0.3f);
            Fill.rect(halfX, halfY, width, height);

            Draw.reset();
        }

        private void drawVanillaElement(float x, float y, float width, float height){
            // 边框
            Drawf.dashRect(Color.sky, x, y, width, height);

            Draw.reset();
        }

        private void drawAlignLines(){
            Draw.color(Pal.accent, 0.8f);

            for(float x : verticalLines){
                Lines.line(x, 0, x, height);
            }

            for(float y : horizontalLines){
                Lines.line(0, y, width, y);
            }

            Draw.reset();
        }

        @Override
        public void draw(){
            // Draw background
            Scene stage = getScene();
            Draw.color(color.r, color.g, color.b, color.a * parentAlpha);
            background.draw(x, y, stage.getWidth(), stage.getHeight());
            Draw.reset();

            super.draw();

            Vec2 pos = Pools.obtain(Vec2.class, Vec2::new);

            for(OperableTable table : operableTables){
                if(table.operable()){
                    ElementUtils.getOriginOnScene(table, pos);
                    stageToLocalCoordinates(pos);

                    drawOperableTable(pos.x, pos.y, table.getWidth(), table.getHeight());
                }
            }

            for(Element element : vanillaElements){
                if(operable(element)){
                    ElementUtils.getOriginOnScene(element, pos);
                    stageToLocalCoordinates(pos);

                    drawVanillaElement(pos.x, pos.y, element.getWidth(), element.getHeight());
                }
            }

            drawAlignLines();

            pos.setZero();
            Pools.free(pos);
        }

        public void show(){
            // Showing
            if(hasParent()){
                return;
            }

            Core.scene.add(this);
        }

        public void hide(){
            if(!hasParent()){
                return;
            }

            remove();
        }
    }
}
