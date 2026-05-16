package edu.chd.campusmap.util;

import javafx.animation.FadeTransition;
import javafx.animation.ParallelTransition;
import javafx.animation.TranslateTransition;
import javafx.scene.Node;
import javafx.util.Duration;

public class AnimationUtil {

    private static final Duration DURATION_NORMAL = Duration.millis(300);

    /**
     * 淡入动画
     */
    public static FadeTransition fadeIn(Node node) {
        FadeTransition ft = new FadeTransition(DURATION_NORMAL, node);
        ft.setFromValue(0.0);
        ft.setToValue(1.0);
        return ft;
    }

    /**
     * 从右侧滑入并淡入
     */
    public static ParallelTransition slideInRight(Node node) {
        FadeTransition ft = new FadeTransition(DURATION_NORMAL, node);
        ft.setFromValue(0.0);
        ft.setToValue(1.0);

        TranslateTransition tt = new TranslateTransition(DURATION_NORMAL, node);
        tt.setFromX(40);
        tt.setToX(0);

        return new ParallelTransition(node, ft, tt);
    }
}
