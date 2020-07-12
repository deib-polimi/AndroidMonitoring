package android.app;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import android.os.Parcel;
import android.os.Parcelable;

import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import org.json.JSONArray;
import org.json.JSONObject;
import android.util.Slog;

/** @hide */
public class RuntimeView implements Parcelable {

    public String className;
    public String tag;
    public int id;
    public int width;
    public int height;
    public int marginLeft;
    public int marginTop;
    public boolean isContainer;
    public RuntimeView[] children;
    public String text;

    public RuntimeView(View view) {
        this.className = view.getClass().getName();
        this.id = view.getId();
        this.width = view.getWidth();
        this.height = view.getHeight();
        this.marginLeft = view.getLeft();
        this.marginTop = view.getTop();
        if (view.getTag() != null) {
            this.tag = view.getTag().toString();
        }
        if (view instanceof ViewGroup) {
            this.isContainer = true;
            int childCount = ((ViewGroup) view).getChildCount();
            this.children = new RuntimeView[childCount];
            for (int i = 0; i < childCount; i++) {
                this.children[i] = new RuntimeView(((ViewGroup) view).getChildAt(i));
            }
        }
        if (view instanceof TextView) {
            this.text = ((TextView) view).getText().toString();
        }
    }

    @Override
    public String toString() {
        if (isContainer) {
            String s = "";
            for (RuntimeView v : children) {
                s += v.toString();
            }
            return s;
        } else
            return String.format("%s { id: %d, position: (%d, %d), size: (%d, %d) }\n", className, id, marginLeft,
                    marginTop, width, height);
    }

    public static final Parcelable.Creator<RuntimeView> CREATOR = new Parcelable.Creator<RuntimeView>() {
        public RuntimeView createFromParcel(Parcel in) {
            return new RuntimeView(in);
        }

        public RuntimeView[] newArray(int size) {
            return new RuntimeView[size];
        }
    };

    public RuntimeView(Parcel in) {
        this.className = in.readString();
        this.id = in.readInt();
        this.width = in.readInt();
        this.height = in.readInt();
        this.marginLeft = in.readInt();
        this.marginTop = in.readInt();
        this.isContainer = in.readBoolean();

        if (this.isContainer) {
            this.children = in.readTypedArray(RuntimeView.CREATOR);
        }

        boolean hasTag = in.readBoolean();
        if (hasTag) {
            this.tag = in.readString();
        }

        boolean hasText = in.readBoolean();
        if (hasText) {
            this.text = in.readString();
        }
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(this.className);
        dest.writeInt(this.id);
        dest.writeInt(this.width);
        dest.writeInt(this.height);
        dest.writeInt(this.marginLeft);
        dest.writeInt(this.marginTop);
        dest.writeBoolean(this.isContainer);
        if (this.isContainer) {
            dest.writeTypedArray(this.children, 0);
        }
        if (this.tag != null) {
            dest.writeBoolean(true);
            dest.writeString(this.tag);
        } else {
            dest.writeBoolean(false);
        }
        if (this.text != null) {
            dest.writeBoolean(true);
            dest.writeString(this.text);
        } else {
            dest.writeBoolean(false);
        }
    }

    public JSONObject getJSON() {
        JSONObject obj = new JSONObject();
        try {
            obj.put("class", className);
            obj.put("tag", tag);
            obj.put("id", id);
            obj.put("width", width);
            obj.put("height", height);
            obj.put("marginLeft", marginLeft);
            obj.put("marginTop", marginTop);
            if (this.text != null) {
                obj.put("text", this.text);
            }
            if (isContainer) {
                JSONArray children = new JSONArray();
                for (int i = 0; i < this.children.length; i++) {
                    children.put(this.children[i].getJSON());
                }
                obj.put("children", children);
            }
            return obj;
        } catch (Exception e) {
            return null;
        }
    }

}