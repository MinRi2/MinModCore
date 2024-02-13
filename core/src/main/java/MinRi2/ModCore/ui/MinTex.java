package MinRi2.ModCore.ui;

import arc.graphics.*;
import arc.scene.style.*;
import arc.util.*;
import mindustry.graphics.*;

import static mindustry.gen.Tex.*;

/**
 * @author minri2
 * Create by 2024/2/13
 */
public class MinTex{
    public static TextureRegionDrawable whiteuiRegion, transAccent, transRed, clearFlatOver;

    static{
        whiteuiRegion = (TextureRegionDrawable)whiteui;
        transAccent = getColoredRegion(Pal.accent, 0.55f);
        transRed = getColoredRegion(Color.red, 0.55f);
        clearFlatOver = getColoredRegion(Color.lightGray, 0.45f);
    }

    public static TextureRegionDrawable getColoredRegion(Color color){
        return (TextureRegionDrawable)whiteuiRegion.tint(color);
    }

    public static TextureRegionDrawable getColoredRegion(Color color, float alpha){
        return (TextureRegionDrawable)whiteuiRegion.tint(Tmp.c1.set(color).a(alpha));
    }

}
