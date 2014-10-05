package com.teamfc.echo.util;

import android.content.Context;
import android.graphics.Color;

import com.teamfc.echo.R;

import java.util.ArrayList;
import java.util.Random;

/**
 * Created by jmok on 10/5/14.
 */
public class ColorUtils {
    private ArrayList<Integer> colors;

    public ColorUtils(Context context) {
        colors = new ArrayList<Integer>();

        colors.add(context.getResources().getColor(R.color.flatui_turquoise));
        colors.add(context.getResources().getColor(R.color.flatui_green_sea));
        colors.add(context.getResources().getColor(R.color.flatui_emerald));
        colors.add(context.getResources().getColor(R.color.flatui_nephritis));
        colors.add(context.getResources().getColor(R.color.flatui_peter_river));
        colors.add(context.getResources().getColor(R.color.flatui_belize_hole));
        colors.add(context.getResources().getColor(R.color.flatui_amethyst));
        colors.add(context.getResources().getColor(R.color.flatui_wisteria));
        colors.add(context.getResources().getColor(R.color.flatui_wet_asphalt));
        colors.add(context.getResources().getColor(R.color.flatui_midnight_blue));
        colors.add(context.getResources().getColor(R.color.flatui_sun_flower));
        colors.add(context.getResources().getColor(R.color.flatui_orange));
        colors.add(context.getResources().getColor(R.color.flatui_carrot));
        colors.add(context.getResources().getColor(R.color.flatui_pumpkin));
        colors.add(context.getResources().getColor(R.color.flatui_alizarin));
        colors.add(context.getResources().getColor(R.color.flatui_pomegranate));
        colors.add(context.getResources().getColor(R.color.flatui_clouds));
        colors.add(context.getResources().getColor(R.color.flatui_silver));
        colors.add(context.getResources().getColor(R.color.flatui_concrete));
        colors.add(context.getResources().getColor(R.color.flatui_asbestos));
    }

    public int getColor(){
        Random random = new Random();
        int randInt = random.nextInt(colors.size());
        return colors.get(randInt);
    }

}
