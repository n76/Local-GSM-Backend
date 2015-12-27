package org.fitchfamily.android.gsmlocation.ui.settings.mcc;

import android.content.Context;
import android.util.AttributeSet;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.ViewSwitcher;

import org.androidannotations.annotations.AfterViews;
import org.androidannotations.annotations.Click;
import org.androidannotations.annotations.EViewGroup;
import org.androidannotations.annotations.LongClick;
import org.androidannotations.annotations.ViewById;
import org.fitchfamily.android.gsmlocation.R;

@EViewGroup(R.layout.view_area)
public class AreaView extends FrameLayout {
    private static final int DEFAULT = 0;

    private static final int HIGHLIGHTED = 1;

    @ViewById
    protected TextView text;

    @ViewById
    protected TextView icon;

    @ViewById
    protected TextView text2;

    @ViewById
    protected TextView icon2;

    @ViewById
    protected ViewSwitcher switcher;

    private Area lastArea;

    public AreaView(Context context) {
        super(context);
    }

    public AreaView(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
    }

    @AfterViews
    protected void init() {
        setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        icon.setText("{gmd-check_box_outline_blank}");
    }

    @Click
    protected void card() {
        performClick();
    }

    @LongClick(R.id.card)
    protected void cardLongClick() {
        performLongClick();
    }

    @Click
    protected void card2() {
        performClick();
    }

    @LongClick(R.id.card2)
    protected void card2LongClick() {
        performLongClick();
    }

    public void bind(Area area) {
        setText(area.label());

        Area.Status status = area.status();
        if (status == Area.Status.mixed) {
            icon2.setText("{gmd-indeterminate_check_box}");
        } else {
            icon2.setText("{gmd-check_box}");
        }

        setDisplayChild(status == Area.Status.disabled ? DEFAULT : HIGHLIGHTED, area == lastArea);

        lastArea = area;
    }

    private void setDisplayChild(int child, boolean animated) {
        if (switcher.getDisplayedChild() != child) {
            setAnimationEnabled(animated);
            switcher.setDisplayedChild(child);
        }
    }

    private void setText(String text) {
        this.text.setText(text);
        this.text2.setText(text);
    }

    private void setAnimationEnabled(boolean enabled) {
        if (enabled) {
            switcher.setInAnimation(getContext(), R.anim.fade_in);
            switcher.setOutAnimation(getContext(), R.anim.fade_out);
        } else {
            switcher.setInAnimation(null);
            switcher.setOutAnimation(null);
        }
    }
}
