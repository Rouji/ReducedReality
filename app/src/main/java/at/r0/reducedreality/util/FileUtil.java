package at.r0.reducedreality.util;

import android.content.Context;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import at.r0.reducedreality.R;

public class FileUtil
{
    public static File resToFile(Context context, int id) throws IOException
    {
        InputStream is = context.getResources().openRawResource(R.raw.lbpcascade_frontalface);
        File dir = context.getDir("rawres", Context.MODE_PRIVATE);
        File file = new File(dir, String.format("%d", id));
        FileOutputStream os = new FileOutputStream(file);

        byte[] buffer = new byte[4096];
        int bytesRead;
        while ((bytesRead = is.read(buffer)) != -1)
        {
            os.write(buffer, 0, bytesRead);
        }
        is.close();
        os.close();
        return file;
    }
}
