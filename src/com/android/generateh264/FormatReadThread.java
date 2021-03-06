package com.android.generateh264;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import android.os.Environment;
import android.util.Log;

/**
 * @author dragon 读取mediaRecorder 录制的视频流线程
 */
public class FormatReadThread extends Thread {
	private static final String TAG = "FormatReadThread";
	/**
	 * 视频数据流
	 */
	private InputStream mInputStream = null;

	/**
	 * sps数据
	 */
	private byte[] mSPS = null;
	/**
	 * pps数据
	 */
	private byte[] mPPS = null;

	/**
	 * 编码信息解析测试文件
	 */
	private final File OUTPUT_FILE = new File(
			Environment.getExternalStorageDirectory(), "outputfile.h264");

	/**
	 * 数据的宽高
	 */
	private int mWidth = 0;
	private int mHeight = 0;
	/**
	 * read data buffer
	 */
	private byte[] mBuffer = null;

	private String mIP;

	/**
	 * @param is
	 *            录制的视频流
	 * @param sps
	 * @param pps
	 * @param rtp
	 * @param w
	 * @param h
	 */
	public FormatReadThread(InputStream is, byte[] sps, byte[] pps, String ip,
			int w, int h) {
		super("FormatReadThread");
		mInputStream = is;
		mSPS = sps;
		mPPS = pps;
		mIP = ip;
		mWidth = w;
		mHeight = h;
	}

	@Override
	public void run() {

		if (OUTPUT_FILE.exists()) {
			OUTPUT_FILE.delete();
		}

		OutputStream os = null;

		try {
			byte buffertemp[] = new byte[4];
			// Skip all atoms preceding mdat atom
			while (!Thread.interrupted()) {
				while (mInputStream.read() != 'm')
					;
				mInputStream.read(buffertemp, 0, 3);
				if (buffertemp[0] == 'd' && buffertemp[1] == 'a'
						&& buffertemp[2] == 't') {
					Log.e(TAG, "skip mp4 header");
					break;
				}
			}
		} catch (IOException e) {
			Log.e(TAG, "Couldn't skip mp4 header");
			return;
		}

		try {
			os = new FileOutputStream(OUTPUT_FILE);
		} catch (IOException e) {
			Log.e("error", "file outputstream e:" + e);
		}

		sendSPS(os);
		sendPPS(os);
		
		byte[] h264header = { 0, 0, 0, 1 };
		byte[] lenthBuffer = new byte[5];
		int h264length = 0;

		try {
			while (!Thread.interrupted()) {

				h264length = readLenth(mInputStream, lenthBuffer);

				 mBuffer = new byte[h264length + h264header.length];

				System.arraycopy(h264header, 0, mBuffer, 0, h264header.length);
				System.arraycopy(lenthBuffer, 0 + 4, mBuffer, h264header.length, 1);
				fill(mInputStream, mBuffer, h264header.length + 1, h264length - 1);
				os.write(mBuffer);
			}
		} catch (IOException e) {
			Log.e(TAG, "e:" + e);
		}

		try {
			mInputStream.close();
		} catch (IOException e) {
			Log.e(TAG, "e");
		}
	}

	/**
	 * 读取数据长度
	 * 
	 * @param is
	 *            数据流
	 * @param lenthBuffer
	 *            长度buffer
	 * @return
	 * @throws IOException
	 */
	private int readLenth(InputStream is, byte[] lenthBuffer)
			throws IOException {
		int ret = 0;
		fill(is, lenthBuffer, 0, lenthBuffer.length);

		ret = lenthBuffer[3] & 0xFF | (lenthBuffer[2] & 0xFF) << 8
				| (lenthBuffer[1] & 0xFF) << 16 | (lenthBuffer[0] & 0xFF) << 24;
		if (ret > 100000 || ret < 0) {
			ret = resync(is, lenthBuffer);
		}

		return ret;
	}

	/**
	 * 查找数据长度位置
	 * 
	 * @param is
	 *            输入数据
	 * @param header
	 *            用于保存数据的buffer
	 * @return
	 * @throws IOException
	 */
	private int resync(InputStream is, byte[] header) throws IOException {
		int type;
		int naluLength = 0;

		if (Utils.DEBUG) {
			Log.e(TAG, "resync");
		}

		while (true) {
			header[0] = header[1];
			header[1] = header[2];
			header[2] = header[3];
			header[3] = header[4];
			header[4] = (byte) is.read();

			type = header[4] & 0x1F;

			if (type == 5 || type == 1) {
				naluLength = header[3] & 0xFF | (header[2] & 0xFF) << 8
						| (header[1] & 0xFF) << 16 | (header[0] & 0xFF) << 24;
				if (naluLength > 0 && naluLength < 100000) {
					Log.e(TAG,
							"A NAL unit may have been found in the bit stream !");
					break;
				}
				if (naluLength == 0) {
					Log.e(TAG, "NAL unit with NULL size found...");
				} else if (header[3] == 0xFF && header[2] == 0xFF
						&& header[1] == 0xFF && header[0] == 0xFF) {
					Log.e(TAG, "NAL unit with 0xFFFFFFFF size found...");
				}
			}
		}
		return naluLength;
	}

	/**
	 * 填充数据
	 * 
	 * @param is
	 *            数据流
	 * @param buffer
	 *            填充buffer
	 * @param offset
	 *            buffer偏移量
	 * @param length
	 *            填充数据长度
	 * @return
	 * @throws IOException
	 */
	private int fill(InputStream is, byte[] buffer, int offset, int length)
			throws IOException {
		int sum = 0, len;
		while (sum < length) {
			len = is.read(buffer, offset + sum, length - sum);
			if (len < 0) {
				throw new IOException("End of stream");
			} else
				sum += len;
		}
		return sum;
	}

	private void sendSPS(OutputStream os) {
		byte[] h264header = { 0, 0, 0, 1 };
		byte[] sendsps = new byte[mSPS.length + h264header.length];
		System.arraycopy(h264header, 0, sendsps, 0, h264header.length);
		System.arraycopy(mSPS, 0, sendsps, 0 + h264header.length, mSPS.length);
		try {
			os.write(sendsps);
		} catch (IOException e) {
			Log.e("error", "send sps e:" + e);
		}
	}

	private void sendPPS(OutputStream os) {
		byte[] h264header = { 0, 0, 0, 1 };
		byte[] sendpps = new byte[mPPS.length + h264header.length];
		System.arraycopy(h264header, 0, sendpps, 0, h264header.length);
		System.arraycopy(mPPS, 0, sendpps, 0 + h264header.length, mPPS.length);
		try {
			os.write(sendpps);
		} catch (IOException e) {
			Log.e("error", "send pps e:" + e);
		}
	}

	private void sendFrameSize() {
		// Entity en = mDataCacheThread.getEmptyEntity(30);

		byte[] mBuffer = null;
		// en.getBuffer();
		mBuffer[29] = 1;
		mBuffer[28] = 0;
		mBuffer[27] = 1;
		mBuffer[26] = 0;
		mBuffer[25] = 1;
		mBuffer[24] = 0;

		mBuffer[0] = (byte) (mWidth & 0xff);
		mBuffer[1] = (byte) ((mWidth >> 8) & 0xff);
		mBuffer[2] = (byte) ((mWidth >> 16) & 0xff);
		mBuffer[3] = (byte) ((mWidth >> 24) & 0xff);

		mBuffer[4] = (byte) (mHeight & 0xff);
		mBuffer[5] = (byte) ((mHeight >> 8) & 0xff);
		mBuffer[6] = (byte) ((mHeight >> 16) & 0xff);
		mBuffer[7] = (byte) ((mHeight >> 24) & 0xff);
		// mDataCacheThread.add(en);
	}
}
