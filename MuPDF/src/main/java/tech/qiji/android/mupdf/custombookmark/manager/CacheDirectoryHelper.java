package tech.qiji.android.mupdf.custombookmark.manager;

import android.content.Context;

import java.io.File;

/**
 * Created by Changefouk on 8/20/15 AD.
 */
public class CacheDirectoryHelper {
    Context context;

    public CacheDirectoryHelper(Context context){
            this.context = context;
        }


    public File getCacheDir(String bookId){


        File cacheDir = new File(context.getCacheDir()+"/thumbnail_cache/"+bookId);

        return cacheDir;
    }

    public void deleteCacheDir(File cacehDir) throws Exception {

        if(cacehDir.exists()){
            deleteRecursive(cacehDir);
        }


    }

    private void deleteRecursive(File fileOrDirectory) throws Exception {

        if (fileOrDirectory.isDirectory())
            for (File child : fileOrDirectory.listFiles())
                deleteRecursive(child);

        fileOrDirectory.delete();
    }



}
