package tech.qiji.android.mupdf.customeditor.model;

import android.graphics.PointF;
import android.os.Parcel;
import android.os.Parcelable;

import java.util.List;

import tech.qiji.android.mupdf.Annotation;

/**
 * Created by Administrator on 25/1/2561.
 */

public class EditorModel implements Parcelable {

    private int pageNumber;
    private List<PointF> pointF;
    private Annotation annotationList;

    public EditorModel(int pageNumber, List<PointF> pointF, Annotation annotationList) {
        this.pageNumber = pageNumber;
        this.pointF = pointF;
        this.annotationList = annotationList;
    }

    protected EditorModel(Parcel in) {
        pageNumber = in.readInt();
        pointF = in.createTypedArrayList(PointF.CREATOR);
        annotationList = in.readParcelable(Annotation.class.getClassLoader());
    }

    public static final Creator<EditorModel> CREATOR = new Creator<EditorModel>() {
        @Override
        public EditorModel createFromParcel(Parcel in) {
            return new EditorModel(in);
        }

        @Override
        public EditorModel[] newArray(int size) {
            return new EditorModel[size];
        }
    };

    public int getPageNumber() {
        return pageNumber;
    }

    public void setPageNumber(int pageNumber) {
        this.pageNumber = pageNumber;
    }

    public List<PointF> getPointF() {
        return pointF;
    }

    public void setPointF(List<PointF> pointF) {
        this.pointF = pointF;
    }

    public Annotation getAnnotationList() {
        return annotationList;
    }

    public void setAnnotationList(Annotation annotationList) {
        this.annotationList = annotationList;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(pageNumber);
        dest.writeTypedList(pointF);
        dest.writeParcelable(annotationList, flags);
    }
}
