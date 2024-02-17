package MinRi2.ModCore.ui;

import arc.func.*;
import arc.math.*;
import arc.scene.actions.*;
import arc.scene.event.*;
import arc.scene.ui.layout.*;
import arc.util.*;
import mindustry.ui.*;

import static arc.Core.*;
import static arc.util.Align.center;
import static mindustry.Vars.state;
import static mindustry.ui.Styles.black6;

/**
 * @author minri2
 * Create by 2024/2/13
 */
public class UIUtils{
    public static void showTableAtMouse(Cons<Table> cons){
        showTableAt(input.mouseX(), input.mouseY(), center, cons);
    }

    public static void showTableAt(float x, float y, int align, Cons<Table> cons){
        showTableAt(x, y, align, cons, table -> !table.hasMouse());
    }

    public static void showTableAt(float x, float y, int align, Cons<Table> cons, Boolf<Table> hideBoolp){
        Table table = new Table(black6);
        scene.add(table);
        table.actions(Actions.fadeIn(0.5f, Interp.smooth), Actions.remove());

        cons.get(table);
        table.pack();

        table.setPosition(x, y, align);
        table.keepInStage();

        table.update(() -> {
            if(hideBoolp.get(table)){
                table.actions(Actions.fadeOut(0.5f, Interp.smooth), Actions.remove());
            }
        });
    }

    public static void showInfoToast(String info, float duration, int align){
        showInfoToastAt(scene.root.getX(align), scene.root.getY(align), info, duration, center);
    }

    public static void showInfoToastAt(float x, float y, String info, float duration, int align){
        Table table = new Table(Styles.black3);
        table.touchable = Touchable.disabled;

        table.update(() -> {
            table.toFront();
            if(state.isMenu()) table.remove();
        });

        table.actions(Actions.fadeIn(0.5f, Interp.smooth), Actions.delay(duration), Actions.fadeOut(0.5f, Interp.smooth), Actions.remove());
        table.add(info).style(Styles.outlineLabel);

        table.pack();

        table.setPosition(x, y, align);
        table.keepInStage();
        scene.add(table);
    }

    public static void setClipboardText(String text){
        /* Do not copy the empty text */
        if(!text.isEmpty()){
            app.setClipboardText(text);
            showInfoToast("Copy: " + text, 3f, Align.bottom);
        }
    }
}
