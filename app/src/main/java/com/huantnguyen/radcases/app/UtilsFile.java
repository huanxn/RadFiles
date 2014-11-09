package com.huantnguyen.radcases.app;

import android.content.Context;
import android.net.Uri;
import android.util.Log;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.channels.FileChannel;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

/**
 * Created by Huan on 10/5/2014.
 */
public class UtilsFile
{
/*
	// standard directories
	private static File downloadsDir;
	private static File picturesDir;
	private static File appDir;             // internal app data directory
	private static File dataDir;            // private data directory (with SQL database)

	public static File CSV_dir;            // contains created zip files with CSV files and images

	public static final File downloadsDir = getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS);
	public static final File picturesDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
	public static final File appDir = getApplication().getExternalFilesDir(null);
	public static final File dataDir = Environment.getDataDirectory();
	public static final File CSV_dir = new File(appDir, "/CSV/");
	*/

	public static final String TAG = "UtilsFile";

	/**
	 * Creates the specified <code>toFile</code> as a byte for byte copy of the
	 * <code>fromFile</code>. If <code>toFile</code> already exists, then it
	 * will be replaced with a copy of <code>fromFile</code>. The name and path
	 * of <code>toFile</code> will be that of <code>toFile</code>.<br/>
	 * <br/>
	 * <i> Note: <code>fromFile</code> and <code>toFile</code> will be closed by
	 * this function.</i>
	 *
	 * @param fromFile
	 *            - FileInputStream for the file to copy from.
	 * @param toFile
	 *            - FileInputStream for the file to copy to.
	 */
	public static void copyFile(FileOutputStream toFile, FileInputStream fromFile) throws IOException
	{
		FileChannel fromChannel = null;
		FileChannel toChannel = null;
		try {
			fromChannel = fromFile.getChannel();
			toChannel = toFile.getChannel();
			fromChannel.transferTo(0, fromChannel.size(), toChannel);
		} finally {
			try {
				if (fromChannel != null) {
					fromChannel.close();
				}
			} finally {
				if (toChannel != null) {
					toChannel.close();
				}
			}
		}
	}

	/*
	static public void copyFile(Uri dst_uri, Uri src_uri)
	{
		// make File
		//copyFile(dstFile, srcFile)
	}
*/

	/**
	 * Copy file contents
	 * @param dst
	 * @param src
	 * @throws IOException
	 */
	static public void copyFile(File dst, File src) throws IOException
	{
		InputStream in = new FileInputStream(src);
		OutputStream out = new FileOutputStream(dst);

		// Transfer bytes from in to out
		byte[] buf = new byte[1024];
		int len;
		while ((len = in.read(buf)) > 0)
		{
			out.write(buf, 0, len);
		}
		in.close();
		out.close();
	}

	static public File makeLocalFile(Context context, File downloadsDir, Uri uri) throws IOException
	{
		// create new local file
		File outFile = null;
		try
		{
			outFile = File.createTempFile("import", ".rcs", downloadsDir);
		}
		catch (IOException e)
		{
			e.printStackTrace();
			UtilClass.showMessage(context, "Unable to create temporary file.");
		}

		FileOutputStream outputStream = null;
		FileInputStream inputStream = null;
		try
		{
			// Google Drive file
			inputStream = (FileInputStream)context.getContentResolver().openInputStream(uri);

			// new local file
			outputStream = new FileOutputStream(outFile);

			// copy backup file contents to local file
			UtilsFile.copyFile(outputStream, inputStream);
		}
		catch (FileNotFoundException e)
		{
			e.printStackTrace();
			UtilClass.showMessage(context, "local file not found");
		}
		catch (IOException e)
		{
			e.printStackTrace();
			UtilClass.showMessage(context, "Copy backup to Google Drive: IO exception");
			UtilClass.showMessage(context, "cannot open input stream from selected uri");
		}

		return outFile;
	}

	/**
	 * Create a zip files
	 *
	 * @param files: String array of full path of files to be compressed into zip file
	 * @param filename: filename of zip file to be created
	 * @return File: reference to newly created zip File
	 * @throws IOException
	 */

	private static final int BUFFER_SIZE = 8192;

	public static File zip(String[] files, String filename) throws IOException {
		BufferedInputStream origin = null;
		File outFile = new File(filename);
		FileOutputStream outputStream = new FileOutputStream(outFile);
		ZipOutputStream out = new ZipOutputStream(new BufferedOutputStream(outputStream));
		try
		{
			byte data[] = new byte[BUFFER_SIZE];

			for (int i = 0; i < files.length; i++) {
				FileInputStream fi = new FileInputStream(files[i]);
				origin = new BufferedInputStream(fi, BUFFER_SIZE);
				try {
					ZipEntry entry = new ZipEntry(files[i].substring(files[i].lastIndexOf("/") + 1));
					out.putNextEntry(entry);
					int count;
					while ((count = origin.read(data, 0, BUFFER_SIZE)) != -1) {
						out.write(data, 0, count);
					}
					out.closeEntry();
				}
				finally {
					origin.close();
				}
			}
		}
		finally
		{
			out.close();
		}

		return outFile;
	}

	/**
	 * Unzip a zip file.  Will overwrite existing files.
	 *
	 * @param zipFile Full path of the zip file you'd like to unzip.
	 * @param location Full path of the directory you'd like to unzip to (will be created if it doesn't exist).
	 * @throws IOException
	 */
	public static void unzip(String zipFile, String location) throws IOException {
		int size;
		byte[] buffer = new byte[BUFFER_SIZE];

		try {
			if ( !location.endsWith("/") ) {
				location += "/";
			}
			File f = new File(location);
			if(!f.isDirectory())
			{
				f.mkdirs();
			}
			ZipInputStream zin = new ZipInputStream(new BufferedInputStream(new FileInputStream(zipFile), BUFFER_SIZE));
			try {
				ZipEntry ze = null;
				while ((ze = zin.getNextEntry()) != null) {
					String path = location + ze.getName();
					File unzipFile = new File(path);

					if (ze.isDirectory()) {
						if(!unzipFile.isDirectory()) {
							unzipFile.mkdirs();
						}
					} else {
						// check for and create parent directories if they don't exist
						File parentDir = unzipFile.getParentFile();
						if ( null != parentDir ) {
							if ( !parentDir.isDirectory() ) {
								parentDir.mkdirs();
							}
						}

						// unzip the file
						FileOutputStream out = new FileOutputStream(unzipFile, false);
						BufferedOutputStream fout = new BufferedOutputStream(out, BUFFER_SIZE);
						try {
							while ( (size = zin.read(buffer, 0, BUFFER_SIZE)) != -1 ) {
								fout.write(buffer, 0, size);
							}

							zin.closeEntry();
						}
						finally {
							fout.flush();
							fout.close();
						}
					}
				}
			}
			finally {
				zin.close();
			}
		}
		catch (Exception e) {
			Log.e(TAG, "Unzip exception", e);
		}
	}
}
