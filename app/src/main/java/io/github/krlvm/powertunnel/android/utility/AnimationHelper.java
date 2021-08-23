/*
 * This file is part of PowerTunnel-Android.
 *
 * PowerTunnel-Android is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * PowerTunnel-Android is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with PowerTunnel-Android.  If not, see <https://www.gnu.org/licenses/>.
 */

package io.github.krlvm.powertunnel.android.utility;

import android.view.animation.AccelerateInterpolator;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.AnimationSet;
import android.view.animation.DecelerateInterpolator;
import android.widget.ImageView;

public class AnimationHelper {

    public static void animate(ImageView imgView, int[] images, Runnable onAnimationEnd) {
        animate(imgView, images, 0, onAnimationEnd);
    }

    public static void animate(ImageView imgView, int[] images, int index, Runnable onAnimationEnd) {
        imgView.clearAnimation();

        final int fadeInDuration = 200;
        final int timeBetween = 300;
        final int fadeOutDuration = 250;

        imgView.setImageResource(images[index]);

        final AnimationSet animation = new AnimationSet(false);

        if(index != images.length - 1) {
            final Animation fadeOut = new AlphaAnimation(1, 0);
            fadeOut.setInterpolator(new DecelerateInterpolator());
            fadeOut.setStartOffset(fadeInDuration + timeBetween);
            fadeOut.setDuration(fadeOutDuration);
            animation.addAnimation(fadeOut);
        }

        if(index != 0) {
            final Animation fadeIn = new AlphaAnimation(0, 1);
            fadeIn.setInterpolator(new AccelerateInterpolator());
            fadeIn.setDuration(fadeInDuration);
            animation.addAnimation(fadeIn);
        }

        animation.setRepeatCount(1);
        animation.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationEnd(Animation animation) {
                if(index == images.length - 1) {
                    if(onAnimationEnd != null) onAnimationEnd.run();
                    return;
                }
                animate(imgView, images, index + 1, onAnimationEnd);
            }
            @Override public void onAnimationRepeat(Animation animation) {}
            @Override public void onAnimationStart(Animation animation) {}
        });

        imgView.startAnimation(animation);
    }
}
