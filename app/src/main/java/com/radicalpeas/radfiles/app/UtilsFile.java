package com.radicalpeas.radfiles.app;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.channels.FileChannel;
import java.security.SecureRandom;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

/**
 * Created by Huan on 10/5/2014.
 */
public class UtilsFile
{
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

	static public File makeLocalFile(Activity activity, File downloadsDir, String filename, String extension, Uri uri) throws IOException
	{
		// create new local file
		File outFile = null;
		try
		{
			outFile = File.createTempFile(filename, extension, downloadsDir);
		}
		catch (IOException e)
		{
			e.printStackTrace();
			UtilClass.showMessage(activity, "Unable to create temporary file.");
		}

		FileOutputStream outputStream = null;
		FileInputStream inputStream = null;
		try
		{
			// Google Drive file
			inputStream = (FileInputStream)activity.getContentResolver().openInputStream(uri);

			// new local file
			outputStream = new FileOutputStream(outFile);

			// copy backup file contents to local file
			UtilsFile.copyFile(outputStream, inputStream);
		}
		catch (FileNotFoundException e)
		{
			e.printStackTrace();
			UtilClass.showMessage(activity, "local file not found");
			outFile.delete();
			outFile = null;
		}
		catch (IOException e)
		{
			e.printStackTrace();
			UtilClass.showMessage(activity, "Copy backup to Google Drive: IO exception");
			UtilClass.showMessage(activity, "cannot open input stream from selected uri");
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

	public static File zip(String[] files, String filename) throws IOException
	{
		return zip(files, filename, null);
	}

	public static File zip(String[] files, String filename, Handler progressHandler) throws IOException
	{
		BufferedInputStream origin = null;
		File outFile = new File(filename);
		FileOutputStream outputStream = new FileOutputStream(outFile);
		ZipOutputStream out = new ZipOutputStream(new BufferedOutputStream(outputStream));
		try
		{
			byte data[] = new byte[BUFFER_SIZE];

			// set max for progress handler: number of image files to zip
			if(progressHandler != null)
			{
				Message msg = new Message();
				msg.arg1 = ImportExportActivity.PROGRESS_MSG_MAX;
				msg.arg2 = files.length;       // times 2
				progressHandler.sendMessage(msg);
			}

			for (int i = 0; i < files.length; i++)
			{
				FileInputStream fi = new FileInputStream(files[i]);
				origin = new BufferedInputStream(fi, BUFFER_SIZE);
				try
				{
					ZipEntry entry = new ZipEntry(files[i].substring(files[i].lastIndexOf("/") + 1));
					out.putNextEntry(entry);
					int count;
					while ((count = origin.read(data, 0, BUFFER_SIZE)) != -1)
					{
						out.write(data, 0, count);
					}
					out.closeEntry();
				}
				finally
				{
					origin.close();
				}

				if(progressHandler != null)
				{
					Message msg = new Message();
					msg.arg1 = ImportExportActivity.PROGRESS_MSG_INCREMENT;
					progressHandler.sendMessage(msg);
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

	// Here are some examples of how you might call this method.
	// The first parameter is the MIME type, and the second parameter is the name
	// of the file you are creating:
	//
	// createFile("text/plain", "foobar.txt");
	// createFile("image/png", "mypicture.png");

	// Unique request code.
	public static final int WRITE_REQUEST_CODE = 43;

	public static void createFile(Activity activity, String mimeType, String fileName)
	{
		Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);

		// Filter to only show results that can be "opened", such as
		// a file (as opposed to a list of contacts or timezones).
		intent.addCategory(Intent.CATEGORY_OPENABLE);

		// Create a file with the requested MIME type.
		intent.setType(mimeType);
		intent.putExtra(Intent.EXTRA_TITLE, fileName);
		activity.startActivityForResult(intent, WRITE_REQUEST_CODE);
	}

	public static int countLines(String filename) throws IOException
	{
		InputStream is = new BufferedInputStream(new FileInputStream(filename));
		try {
			byte[] c = new byte[1024];
			int count = 0;
			int readChars = 0;
			boolean empty = true;
			while ((readChars = is.read(c)) != -1) {
				empty = false;
				for (int i = 0; i < readChars; ++i) {
					if (c[i] == '\n') {
						++count;
					}
				}
			}
			return (count == 0 && !empty) ? 1 : count;
		} finally {
			is.close();
		}
	}

	/**
	 *
	 * @param dir
	 * @return false if fails
	 */
	public static boolean clearDir(File dir)
	{
		if (dir.isDirectory())
		{
			String[] children = dir.list();
			for (int i=0; i<children.length; i++) {
				boolean success = deleteDir(new File(dir, children[i]));
				if (!success) {
					return false;
				}
			}

			return true;
		}
		else
		{
			return false;
		}
	}

	public static boolean deleteDir(File dir)
	{
		if (dir.isDirectory()) {
			String[] children = dir.list();
			for (int i=0; i<children.length; i++) {
				boolean success = deleteDir(new File(dir, children[i]));
				if (!success) {
					return false;
				}
			}
		}

		// The directory is now empty so delete it
		return dir.delete();
	}

	/*
	public class ClearDirectoryTask extends AsyncTask<File, Integer, Boolean>
	{
		private Activity activity;
		private ProgressDialog progressWheelDialog;

		ClearDirectoryTask(Activity activity, ProgressDialog progressWheelDialog)
		{
			this.activity = activity;
			this.progressWheelDialog = progressWheelDialog;
		}
		protected void onPreExecute()
		{
			progressWheelDialog.setCancelable(false);
			progressWheelDialog.setCanceledOnTouchOutside(false);
			progressWheelDialog.show();
		}

		@Override
		protected Boolean doInBackground(File... dir)
		{
			return UtilsFile.clearDir(dir[0]);
		}

		protected void onPostExecute(Boolean isSuccessful)
		{
			progressWheelDialog.dismiss();

			if(isSuccessful)
			{
				UtilClass.showMessage(activity, "Cleared cache files.");
			}
			else
			{
				UtilClass.showMessage(activity, "Clear directory failed.");
			}
		}
	}
	*/

	/*
	// File to byte []
	public static byte[] readFile(File file) throws IOException
	{
		// Open file
		RandomAccessFile f = new RandomAccessFile(file, "r");
		try
		{
			// Get and check length
			long longlength = f.length();
			int length = (int) longlength;
			if (length != longlength)
				throw new IOException("File size >= 2 GB");
			// Read file and return data
			byte[] data = new byte[length];
			f.readFully(data);
			return data;
		} finally {
			f.close();
		}
	}
	*/
	// File to byte []
	public static byte[] convertFileToByteArray(File f)
	{
		byte[] byteArray = null;
		try
		{
			InputStream inputStream = new FileInputStream(f);
			ByteArrayOutputStream bos = new ByteArrayOutputStream();
			byte[] b = new byte[1024*8];
			int bytesRead =0;

			while ((bytesRead = inputStream.read(b)) != -1)
			{
				bos.write(b, 0, bytesRead);
			}

			byteArray = bos.toByteArray();
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
		return byteArray;
	}

	// byte [] to File
	public static void convertByteArrayToFile(File inputFile, byte [] byteArray)
	{
		try
		{
			// overwrite inputFile with byte array
			FileOutputStream outStream = new FileOutputStream(inputFile.getPath());

			try
			{
				outStream.write(byteArray);
			}
			finally
			{
				outStream.close();
			}
		}
		catch(Exception e)
		{
			e.printStackTrace();
			Log.d(TAG, "Unable to convert byte array to File.");
			return;
		}
	}


	// FILE ENCRYPTION
	public static byte[] generateKey(String password) throws Exception
	{
		byte[] keyStart = password.getBytes("UTF-8");

		KeyGenerator kgen = KeyGenerator.getInstance("AES");
		SecureRandom sr = SecureRandom.getInstance("SHA1PRNG", "Crypto");
		sr.setSeed(keyStart);
		kgen.init(128, sr);
		SecretKey skey = kgen.generateKey();
		return skey.getEncoded();
	}

	public static void encryptFile(byte[] key, File inputFile)
	{
		byte [] encryptedBytes = null;

		try
		{
			// encrypt the File into byte array
			encryptedBytes = encodeFile(key, convertFileToByteArray(inputFile));

			/*
			// overwrite inputFile with encrypted bytes
			FileOutputStream outStream = new FileOutputStream(inputFile.getPath());
			try
			{
				outStream.write(encryptedBytes);
			}
			finally
			{
				outStream.close();
			}
			*/

			// overwrite File with encrypted byte array
			convertByteArrayToFile(inputFile, encryptedBytes);
		}
		catch(Exception e)
		{
			e.printStackTrace();
			Log.d(TAG, "Unable to encrypt file.");
			return;
		}

	}

	public static void decryptFile(byte[] key, File inputFile)
	{
		byte [] decryptedBytes = null;

		try
		{
			// decrypt the File as byte array
			decryptedBytes = decodeFile(key, convertFileToByteArray(inputFile));

			// overwrite inputFile with decrypted byte array
			convertByteArrayToFile(inputFile, decryptedBytes);
		}
		catch (Exception e)
		{
			e.printStackTrace();
			Log.d(TAG, "Unable to decrypt file.");
			return;
		}

	}

	public static byte[] encodeFile(byte[] key, byte[] fileData) throws Exception
	{
		SecretKeySpec skeySpec = new SecretKeySpec(key, "AES");
		Cipher cipher = Cipher.getInstance("AES");
		cipher.init(Cipher.ENCRYPT_MODE, skeySpec);

		byte[] encrypted = cipher.doFinal(fileData);

		return encrypted;
	}


	public static byte[] decodeFile(byte[] key, byte[] fileData) throws Exception
	{
		SecretKeySpec skeySpec = new SecretKeySpec(key, "AES");
		Cipher cipher = Cipher.getInstance("AES");
		cipher.init(Cipher.DECRYPT_MODE, skeySpec);

		byte[] decrypted = cipher.doFinal(fileData);

		return decrypted;
	}
}
