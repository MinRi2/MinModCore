package MinRi2.ModCore.ui;

import arc.func.*;
import arc.graphics.*;
import arc.math.geom.*;
import arc.scene.*;
import arc.scene.event.*;
import arc.scene.ui.*;
import arc.scene.ui.layout.*;
import arc.util.*;
import arc.util.pooling.*;
import mindustry.graphics.*;
import mindustry.ui.*;

import static arc.Core.bundle;

public class ElementUtils{

    public static Cell<?> getCell(Element element){
        Group parent = element.parent;

        if(parent instanceof Table table){
            return table.getCell(element);
        }

        return null;
    }

    public static void addTooltip(Element element, String text, boolean allowMobile){
        addTooltip(element, text, Align.top, allowMobile);
    }

    public static void addTooltip(Element element, String text, int align, boolean allowMobile){
        addTooltip(element, t -> {
            t.background(Styles.black8).margin(4f);
            t.add(text).style(Styles.outlineLabel);
        }, align, allowMobile);
    }

    public static void addTooltip(Element element, Prov<CharSequence> prov, boolean allowMobile){
        addTooltip(element, Align.top, prov, allowMobile);
    }

    public static void addTooltip(Element element, int align, Prov<CharSequence> prov, boolean allowMobile){
        addTooltip(element, t -> {
            t.background(Styles.black8).margin(4f);
            t.label(prov).style(Styles.outlineLabel);
        }, align, allowMobile);
    }

    public static void addTooltip(Element element, Cons<Table> cons, boolean allowMobile){
        addTooltip(element, cons, Align.top, allowMobile);
    }

    /**
     * 为ui元素添加提示
     * @param element 需要添加提示的元素
     * @param cons 自定义的信息编辑
     * @param align 对齐位置
     * @param allowMobile 是否需要手机提示
     */
    public static void addTooltip(Element element, Cons<Table> cons, int align, boolean allowMobile){
        Tooltip tip = new Tooltip(cons){
            {
                targetActor = element;

                container.update(() -> {
                    if(!targetActor.hasMouse()){
                        hide();
                    }
                });
            }

            @Override
            public void exit(InputEvent event, float x, float y, int pointer, Element toActor){
            }

            @Override
            protected void setContainerPosition(Element element, float x, float y){
                Vec2 pos = element.localToStageCoordinates(Tmp.v1.set(0, 0));

                container.pack();
                container.setPosition(pos.x, pos.y, align);
                container.setOrigin(0, element.getHeight());
            }
        };
        tip.allowMobile = allowMobile;

        element.addListener(tip);
    }

    public static void addIntroductionFor(Group group, String bundleName, boolean allowMobile){
        for(Element child : group.getChildren()){
            /* add some tooltips */
            if(child.name != null){
                addTooltip(child, bundle.get(bundleName + "." + child.name), allowMobile);
            }
        }
    }

    /**
     * hit但是无视是否可点击
     */
    public static Element hit(Group group, float x, float y){
        Vec2 point = Tmp.v1;
        Element[] childrenArray = group.getChildren().items;
        for(int i = group.getChildren().size - 1; i >= 0; i--){
            Element child = childrenArray[i];
            if(!child.visible) continue;

            child.parentToLocalCoordinates(point.set(x, y));

            Element hit;

            if(child instanceof Group g){
                hit = hit(g, point.x, point.y);
            }else{
                hit = hit(child, point.x, point.y);
            }

            if(hit != null) return hit;
        }
        return null;
    }


    /**
     * hit但是无视是否可点击
     */
    public static Element hit(Element e, float x, float y){
        return isOverlays(e, x, y) ? e : null;
    }

    /**
     * 仅判断点击是否落在元素内部
     */
    public static boolean isOverlays(Element e, float x, float y){
        return x >= e.translation.x && x < e.getWidth() + e.translation.x && y >= e.translation.y && y < e.getHeight() + e.translation.y;
    }

    /**
     * 添加标题
     * @param table 添加标题的表
     * @param title 添加的标题内容
     */
    public static void addTitle(Table table, String title){
        addTitle(table, title, Pal.accent);
    }

    /**
     * 添加标题
     * @param table 添加标题的表
     * @param title 添加的标题内容
     * @param color 背景颜色1
     */
    public static void addTitle(Table table, String title, Color color){
        table.table(MinTex.getColoredRegion(color), t -> {
            t.add(title).style(Styles.outlineLabel);
        }).margin(8f).growX();
        table.row();
    }

    public static Rect getBounds(Element element, Rect out){
        return out.set(element.x, element.y, element.getWidth(), element.getHeight());
    }

    public static Rect getBoundsOnScene(Element element, Rect out){
        Vec2 v = Pools.obtain(Vec2.class, Vec2::new);
        element.localToStageCoordinates(v.set(0, 0));

        out.set(v.x, v.y, element.getWidth(), element.getHeight());

        v.setZero();
        Pools.free(v);

        return out;
    }

    /**
     * 获取元素原点(左下角)在Scene坐标系下的坐标
     * @param element 获取的元素
     * @param out 输出坐标
     * @return 返回输出坐标
     */
    public static Vec2 getOriginOnScene(Element element, Vec2 out){
        return out.set(element.localToStageCoordinates(Tmp.v1.set(0, 0)));
    }

    /**
     * 将元素坐标系转换到目标坐标系
     * @param local 当前坐标系
     * @param target 目标坐标系
     * @param pos 转换点/输出点
     */
    public static void localToTargetCoordinate(Element local, Element target, Vec2 pos){
        local.localToStageCoordinates(pos);
        target.stageToLocalCoordinates(pos);
    }

    /**
     * 将元素的某个方位与目标元素的某个方位对齐
     * @param element 设置位置的元素
     * @param target 目标元素
     * @param align 元素对齐方位
     */
    public static void alignTo(Element element, Element target, int align, int alignTarget){
        Vec2 v = Pools.obtain(Vec2.class, Vec2::new);
        v.set(target.getX(alignTarget), target.getY(alignTarget)); // 目标元素在目标方位的坐标

        localToTargetCoordinate(target.parent, element.parent, v); // 将目标方位转换成元素的父坐标系

        element.setPosition(v.x, v.y, align); // 对齐
    }

}
