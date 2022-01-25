package fastdoop;

import org.apache.hadoop.fs.FSDataInputStream;

import java.io.EOFException;
import java.io.IOException;

class Utils {

    /**
     * From FSUtils in HUDI https://github.com/apache/hudi
     * <p>
     * GCS has a different behavior for detecting EOF during seek().
     *
     * @param inputStream FSDataInputStream
     * @return true if the inputstream or the wrapped one is of type GoogleHadoopFSInputStream
     */
    public static boolean isGCSInputStream(FSDataInputStream inputStream) {
        return inputStream.getClass().getCanonicalName().equals("com.google.cloud.hadoop.fs.gcs.GoogleHadoopFSInputStream")
                || inputStream.getWrappedStream().getClass().getCanonicalName()
                .equals("com.google.cloud.hadoop.fs.gcs.GoogleHadoopFSInputStream");
    }


    /**
     * From FSUtils in HUDI https://github.com/apache/hudi
     *
     * Handles difference in seek behavior for GCS and non-GCS input stream
     * @param inputStream Input Stream
     * @param pos  Position to seek
     * @throws IOException
     */
    public static void safeSeek(FSDataInputStream inputStream, long pos) throws IOException {
        try {
            inputStream.seek(pos);
        } catch (EOFException e) {
            if (isGCSInputStream(inputStream)) {
                inputStream.seek(pos - 1);
            } else {
                throw e;
            }
        }
    }

    /**
     * Adjust an offset into a buffer such that the offset does not overrun the buffer end.
     * @param buffer
     * @param offset
     * @return
     */
    public static int trimToEnd(byte[] buffer, int offset) {
        return (offset <= buffer.length - 1) ? offset : (buffer.length - 1);
    }
}