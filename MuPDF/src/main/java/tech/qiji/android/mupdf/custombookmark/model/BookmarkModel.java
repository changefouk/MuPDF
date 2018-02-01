package tech.qiji.android.mupdf.custombookmark.model;

/**
 * Created by Changefouk on 4/10/2560.
 */

public class BookmarkModel {

    int page;
    String Description;
    boolean bookmark;


    public int getPage() {
        return page;
    }

    public void setPage(int page) {
        this.page = page;
    }

    public String getDescription() {
        return Description;
    }

    public void setDescription(String description) {
        Description = description;
    }

    public boolean isBookmark() {
        return bookmark;
    }

    public void setBookmark(boolean bookmark) {
        this.bookmark = bookmark;
    }
}
