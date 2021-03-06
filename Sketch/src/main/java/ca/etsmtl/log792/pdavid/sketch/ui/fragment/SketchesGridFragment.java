package ca.etsmtl.log792.pdavid.sketch.ui.fragment;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.graphics.Point;
import android.graphics.Rect;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import android.widget.AdapterView;
import android.widget.ImageView;

import java.io.File;
import java.util.ArrayList;

import ca.etsmtl.log792.pdavid.sketch.ApplicationManager;
import ca.etsmtl.log792.pdavid.sketch.R;
import ca.etsmtl.log792.pdavid.sketch.model.BaseModel;
import ca.etsmtl.log792.pdavid.sketch.model.Sketch;
import ca.etsmtl.log792.pdavid.sketch.ui.adapter.MyGridAdapter;

/**
 * A fragment representing a single MenuItem detail screen.
 * This fragment is either contained in a {@link ca.etsmtl.log792.pdavid.sketch.ui.activity.MenuItemListActivity}
 * in two-pane mode (on tablets) or a {@link ca.etsmtl.log792.pdavid.sketch.ui.activity.MenuItemDetailActivity}
 * on handsets.
 */
public class SketchesGridFragment extends BaseGridFragment {

    public static final String TAG = SketchesGridFragment.class.getName();
    private ImageView expandedImageView;
    private ArrayList<BaseModel> list;
    private boolean isZoomedIn;

    /**
     * Mandatory empty constructor for the fragment manager to instantiate the
     * fragment (e.g. upon screen orientation changes).
     */
    public SketchesGridFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Get all images
        File root = new File(Environment.getExternalStorageDirectory() + "/MySketches/");
        if (root != null) {
            if (!root.exists())
                root.mkdir();

        }
        File[] files = root.listFiles();
        // Generate models
        list = new ArrayList<BaseModel>();
        for (File f : files) {
            if (f.getName().contains(".png"))
                list.add(new Sketch(f.getName(), f));
        }

    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        gridview.setAdapter(new MyGridAdapter(getActivity(), list));
    }

    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
        Sketch item = (Sketch) adapterView.getAdapter().getItem(i);
        zoomImageFromThumb(view, item.getFile());
        if (!ApplicationManager.isTablet) {

            getActivity().getActionBar().hide();
        }
    }

    // Hold a reference to the current animator,
    // so that it can be canceled mid-way.
    private Animator mCurrentAnimator;

    // The system "short" animation time duration, in milliseconds. This
    // duration is ideal for subtle animations or animations that occur
    // very frequently.
    private int mShortAnimationDuration = 300;

    private void zoomImageFromThumb(final View thumbView, File path) {
        // If there's an animation in progress, cancel it
        // immediately and proceed with this one.
        if (mCurrentAnimator != null) {
            mCurrentAnimator.cancel();
        }

        // Load the high-resolution "zoomed-in" image.
        expandedImageView = (ImageView) root.findViewById(
                R.id.expanded_image);
//        expandedImageView.setImageResource(imageResId);
        ApplicationManager.getImage(expandedImageView, path);

        // Calculate the starting and ending bounds for the zoomed-in image.
        // This step involves lots of math. Yay, math.
        final Rect startBounds = new Rect();
        final Rect finalBounds = new Rect();
        final Point globalOffset = new Point();

        // The start bounds are the global visible rectangle of the thumbnail,
        // and the final bounds are the global visible rectangle of the container
        // view. Also set the container view's offset as the origin for the
        // bounds, since that's the origin for the positioning animation
        // properties (X, Y).
        thumbView.getGlobalVisibleRect(startBounds);

        root.getGlobalVisibleRect(finalBounds, globalOffset);
        startBounds.offset(-globalOffset.x, -globalOffset.y);
        finalBounds.offset(-globalOffset.x, -globalOffset.y);

        // Adjust the start bounds to be the same aspect ratio as the final
        // bounds using the "center crop" technique. This prevents undesirable
        // stretching during the animation. Also calculate the start scaling
        // factor (the end scaling factor is always 1.0).
        float startScale;
        if ((float) finalBounds.width() / finalBounds.height()
                > (float) startBounds.width() / startBounds.height()) {
            // Extend start bounds horizontally
            startScale = (float) startBounds.height() / finalBounds.height();
            float startWidth = startScale * finalBounds.width();
            float deltaWidth = (startWidth - startBounds.width()) / 2;
            startBounds.left -= deltaWidth;
            startBounds.right += deltaWidth;
        } else {
            // Extend start bounds vertically
            startScale = (float) startBounds.width() / finalBounds.width();
            float startHeight = startScale * finalBounds.height();
            float deltaHeight = (startHeight - startBounds.height()) / 2;
            startBounds.top -= deltaHeight;
            startBounds.bottom += deltaHeight;
        }

        // Hide the thumbnail and show the zoomed-in view. When the animation
        // begins, it will position the zoomed-in view in the place of the
        // thumbnail.
        thumbView.setAlpha(0f);
        expandedImageView.setVisibility(View.VISIBLE);
        gridview.setVisibility(View.GONE);

        // Set the pivot point for SCALE_X and SCALE_Y transformations
        // to the top-left corner of the zoomed-in view (the default
        // is the center of the view).
        expandedImageView.setPivotX(0f);
        expandedImageView.setPivotY(0f);

        // Construct and run the parallel animation of the four translation and
        // scale properties (X, Y, SCALE_X, and SCALE_Y).
        AnimatorSet set = new AnimatorSet();
        set
                .play(ObjectAnimator.ofFloat(expandedImageView, View.X,
                        startBounds.left, finalBounds.left))
                .with(ObjectAnimator.ofFloat(expandedImageView, View.Y,
                        startBounds.top, finalBounds.top))
                .with(ObjectAnimator.ofFloat(expandedImageView, View.SCALE_X,
                        startScale, 1f)).with(ObjectAnimator.ofFloat(expandedImageView,
                View.SCALE_Y, startScale, 1f));
        set.setDuration(mShortAnimationDuration);
        set.setInterpolator(new DecelerateInterpolator());
        set.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                mCurrentAnimator = null;
            }

            @Override
            public void onAnimationCancel(Animator animation) {
                mCurrentAnimator = null;
            }
        });
        set.start();
        mCurrentAnimator = set;
        isZoomedIn = true;
        // Upon clicking the zoomed-in image, it should zoom back down
        // to the original bounds and show the thumbnail instead of
        // the expanded image.
        final float startScaleFinal = startScale;
        expandedImageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mCurrentAnimator != null) {
                    mCurrentAnimator.cancel();
                }
                getActivity().getActionBar().show();

                // Animate the four positioning/sizing properties in parallel,
                // back to their original values.
                AnimatorSet set = new AnimatorSet();
                set.play(ObjectAnimator
                        .ofFloat(expandedImageView, View.X, startBounds.left))
                        .with(ObjectAnimator
                                .ofFloat(expandedImageView,
                                        View.Y, startBounds.top))
                        .with(ObjectAnimator
                                .ofFloat(expandedImageView,
                                        View.SCALE_X, startScaleFinal))
                        .with(ObjectAnimator
                                .ofFloat(expandedImageView,
                                        View.SCALE_Y, startScaleFinal));
                set.setDuration(mShortAnimationDuration);
                set.setInterpolator(new DecelerateInterpolator());
                set.addListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        thumbView.setAlpha(1f);
                        expandedImageView.setVisibility(View.GONE);
                        gridview.setVisibility(View.VISIBLE);
                        mCurrentAnimator = null;
                    }

                    @Override
                    public void onAnimationCancel(Animator animation) {
                        thumbView.setAlpha(1f);
                        expandedImageView.setVisibility(View.GONE);
                        mCurrentAnimator = null;
                        gridview.setVisibility(View.VISIBLE);
                    }
                });
                set.start();
                mCurrentAnimator = set;
                isZoomedIn = false;
            }
        });
    }

    public void zoomOut() {
        if (expandedImageView != null) {
            // Make sure we're running on Honeycomb or higher to use ActionBar APIs
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH_MR1) {
                expandedImageView.callOnClick();
            } else {
                expandedImageView.performClick();
            }
        }
    }

    public boolean isZoomedIn() {
        return isZoomedIn;
    }
}
