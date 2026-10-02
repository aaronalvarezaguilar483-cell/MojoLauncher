package net.kdt.pojavlaunch.customcontrols.buttons;

import static net.kdt.pojavlaunch.customcontrols.gamepad.GamepadJoystick.DIRECTION_EAST;
import static net.kdt.pojavlaunch.customcontrols.gamepad.GamepadJoystick.DIRECTION_NONE;
import static net.kdt.pojavlaunch.customcontrols.gamepad.GamepadJoystick.DIRECTION_NORTH;
import static net.kdt.pojavlaunch.customcontrols.gamepad.GamepadJoystick.DIRECTION_NORTH_EAST;
import static net.kdt.pojavlaunch.customcontrols.gamepad.GamepadJoystick.DIRECTION_NORTH_WEST;
import static net.kdt.pojavlaunch.customcontrols.gamepad.GamepadJoystick.DIRECTION_SOUTH;
import static net.kdt.pojavlaunch.customcontrols.gamepad.GamepadJoystick.DIRECTION_SOUTH_EAST;
import static net.kdt.pojavlaunch.customcontrols.gamepad.GamepadJoystick.DIRECTION_SOUTH_WEST;
import static net.kdt.pojavlaunch.customcontrols.gamepad.GamepadJoystick.DIRECTION_WEST;

import android.annotation.SuppressLint;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.view.MotionEvent;
import android.view.View;

import net.kdt.pojavlaunch.LwjglGlfwKeycode;
import net.kdt.pojavlaunch.Tools;
import net.kdt.pojavlaunch.customcontrols.ControlData;
import net.kdt.pojavlaunch.customcontrols.ControlJoystickData;
import net.kdt.pojavlaunch.customcontrols.ControlLayout;
import net.kdt.pojavlaunch.customcontrols.LayoutBitmaps;
import net.kdt.pojavlaunch.customcontrols.gamepad.GamepadJoystick;
import net.kdt.pojavlaunch.customcontrols.handleview.EditControlSideDialog;

import net.kdt.pojavlaunch.CallbackBridge;

import git.artdeell.dnbootstrap.glfw.GLFW;
import io.github.controlwear.virtual.joystick.android.JoystickView;

@SuppressLint("ViewConstructor")
public class ControlJoystick extends JoystickView implements ControlInterface {
    public final static int DIRECTION_FORWARD_LOCK = 8;
    // Directions keycode
    private final int[] mDirectionForwardLock = new int[]{LwjglGlfwKeycode.GLFW_KEY_LEFT_CONTROL};
    private final int[] mDirectionForward = new int[]{LwjglGlfwKeycode.GLFW_KEY_W};
    private final int[] mDirectionRight = new int[]{LwjglGlfwKeycode.GLFW_KEY_D};
    private final int[] mDirectionBackward = new int[]{LwjglGlfwKeycode.GLFW_KEY_S};
    private final int[] mDirectionLeft = new int[]{LwjglGlfwKeycode.GLFW_KEY_A};
    private ControlJoystickData mControlData;
    private int mLastDirectionInt = GamepadJoystick.DIRECTION_NONE;
    private int mCurrentDirectionInt = GamepadJoystick.DIRECTION_NONE;
    // Custom images (null = use the default look)
    private Bitmap mBaseBitmap, mStickBitmap;
    private final Paint mBitmapPaint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
    private final RectF mDrawRect = new RectF();
    // Where the finger started (relative mode) and the current stick offset, in px
    private float mOriginX, mOriginY, mStickDx, mStickDy;
    public ControlJoystick(ControlLayout parent, ControlJoystickData data) {
        super(parent.getContext());
        init(data, parent);
    }

    private static void sendInput(int[] keys, boolean isDown) {
        for (int key : keys) {
            int modifiers = CallbackBridge.getCurrentMods();
            GLFW.sendKeyEvent(key, isDown, modifiers);
        }
    }

    private void init(ControlJoystickData data, ControlLayout layout) {
        mControlData = data;
        setProperties(preProcessProperties(data, layout));
        setDeadzone(35);
        setFixedCenter(data.absolute);
        setAutoReCenterButton(true);

        injectBehaviors();

        setOnMoveListener(new OnMoveListener() {
            @Override
            public void onMove(int angle, int strength) {
                mLastDirectionInt = mCurrentDirectionInt;
                mCurrentDirectionInt = getDirectionInt(angle, strength);

                if (mLastDirectionInt != mCurrentDirectionInt) {
                    sendDirectionalKeycode(mLastDirectionInt, false);
                    sendDirectionalKeycode(mCurrentDirectionInt, true);
                }
            }

            @Override
            public void onForwardLock(boolean isLocked) {
                sendInput(mDirectionForwardLock, isLocked);
            }
        });
    }

    @Override
    public View getControlView() {
        return this;
    }

    @Override
    public ControlData getProperties() {
        return mControlData;
    }

    @Override
    public void setProperties(ControlData properties, boolean changePos) {
        mControlData = (ControlJoystickData) properties;
        mControlData.isHideable = true;
        ControlInterface.super.setProperties(properties, changePos);
        postDelayed(() -> {
            setForwardLockDistance(mControlData.forwardLock ? (int) Tools.dpToPx(60) : 0);
            setFixedCenter(mControlData.absolute);
        }, 10);
    }

    @Override
    public void removeButton() {
        getControlLayoutParent().getLayout().mJoystickDataList.remove(getProperties());
        getControlLayoutParent().removeView(this);
    }

    @Override
    public void cloneButton() {
        ControlJoystickData data = new ControlJoystickData(mControlData);
        getControlLayoutParent().addJoystickButton(data);
    }

    @Override
    public void handlePressed() {/*STUB since non swipeable*/}

    @Override
    public void handleReleased() {/*STUB since non swipeable*/}


    @Override
    public void setBackground() {
        ControlJoystickData data = (ControlJoystickData) getProperties();
        LayoutBitmaps storage = getControlLayoutParent().getBitmaps();
        mBaseBitmap = Tools.isValidString(data.bitmapTag) ? storage.getBitmap(data.bitmapTag) : null;
        mStickBitmap = Tools.isValidString(data.stickBitmapTag) ? storage.getBitmap(data.stickBitmapTag) : null;

        if (mBaseBitmap != null) {
            // The image replaces the default circle: hide the library's own background and border
            setBorderWidth(0);
            setBorderColor(Color.TRANSPARENT);
            setBackgroundColor(Color.TRANSPARENT);
        } else {
            setBorderWidth((int) Tools.dpToPx(data.strokeWidth * (getControlLayoutParent().getLayoutScale()/100f)));
            setBorderColor(data.strokeColor);
            setBackgroundColor(data.bgColor);
        }

        if (mStickBitmap != null) {
            // The image replaces the default thumb: hide the library's own one
            setButtonColor(Color.TRANSPARENT);
            setButtonSizeRatio(Math.max(0.1f, Math.min(0.9f, data.stickScale)));
        } else {
            setButtonColor(Color.BLACK); // library default
        }
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (mBaseBitmap != null) {
            float size = Math.min(getWidth(), getHeight());
            float left = (getWidth() - size) / 2f, top = (getHeight() - size) / 2f;
            mDrawRect.set(left, top, left + size, top + size);
            canvas.drawBitmap(mBaseBitmap, null, mDrawRect, mBitmapPaint);
        }
        super.onDraw(canvas);
        if (mStickBitmap != null) {
            float size = Math.min(getWidth(), getHeight());
            float ratio = Math.max(0.1f, Math.min(0.9f, ((ControlJoystickData) getProperties()).stickScale));
            float radius = size / 2f * ratio;
            float cx = getWidth() / 2f + mStickDx, cy = getHeight() / 2f + mStickDy;
            mDrawRect.set(cx - radius, cy - radius, cx + radius, cy + radius);
            canvas.drawBitmap(mStickBitmap, null, mDrawRect, mBitmapPaint);
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        boolean handled = super.onTouchEvent(event);
        if (mStickBitmap != null) updateStickOffset(event);
        return handled;
    }

    /** Follow the finger the same way the joystick logic does, to place the stick image */
    private void updateStickOffset(MotionEvent event) {
        int action = event.getActionMasked();
        if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) {
            mStickDx = mStickDy = 0;
            invalidate();
            return;
        }
        if (action == MotionEvent.ACTION_DOWN) {
            boolean fixedCenter = ((ControlJoystickData) getProperties()).absolute;
            mOriginX = fixedCenter ? getWidth() / 2f : event.getX();
            mOriginY = fixedCenter ? getHeight() / 2f : event.getY();
        }
        float size = Math.min(getWidth(), getHeight());
        float ratio = Math.max(0.1f, Math.min(0.9f, ((ControlJoystickData) getProperties()).stickScale));
        float maxDistance = size / 2f - size / 2f * ratio;
        float dx = event.getX() - mOriginX, dy = event.getY() - mOriginY;
        float dist = (float) Math.hypot(dx, dy);
        if (dist > maxDistance && dist > 0) {
            dx = dx / dist * maxDistance;
            dy = dy / dist * maxDistance;
        }
        mStickDx = dx;
        mStickDy = dy;
        invalidate();
    }

    @Override
    public void loadEditValues(EditControlSideDialog editControlPopup) {
        editControlPopup.loadJoystickValues(mControlData);
    }

    private int getDirectionInt(int angle, int intensity) {
        if (intensity == 0) return DIRECTION_NONE;
        return (int) (((angle + 22.5) / 45) % 8);
    }

    private void sendDirectionalKeycode(int direction, boolean isDown) {
        switch (direction) {
            case DIRECTION_NORTH:
                sendInput(mDirectionForward, isDown);
                break;
            case DIRECTION_NORTH_EAST:
                sendInput(mDirectionForward, isDown);
                sendInput(mDirectionRight, isDown);
                break;
            case DIRECTION_EAST:
                sendInput(mDirectionRight, isDown);
                break;
            case DIRECTION_SOUTH_EAST:
                sendInput(mDirectionRight, isDown);
                sendInput(mDirectionBackward, isDown);
                break;
            case DIRECTION_SOUTH:
                sendInput(mDirectionBackward, isDown);
                break;
            case DIRECTION_SOUTH_WEST:
                sendInput(mDirectionBackward, isDown);
                sendInput(mDirectionLeft, isDown);
                break;
            case DIRECTION_WEST:
                sendInput(mDirectionLeft, isDown);
                break;
            case DIRECTION_NORTH_WEST:
                sendInput(mDirectionForward, isDown);
                sendInput(mDirectionLeft, isDown);
                break;
            case DIRECTION_FORWARD_LOCK:
                sendInput(mDirectionForwardLock, isDown);
                break;
        }
    }

}
