
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class LZW {
	private static int btsz;
	private static String strBin[] = new String[256];

	// Helper Methods
	private static int ByteToInt(Byte bt) {
		int ret = bt;

		if (ret < 0)
			ret += 256;

		return ret;
	}

	private static String IntToString(int inp) {
		StringBuilder ret = new StringBuilder();
		StringBuilder r1 = new StringBuilder();

		if (inp == 0)
			ret = new StringBuilder("0");

		while (inp != 0) {
			if ((inp % 2) == 1)
				ret.append("1");
			else
				ret.append("0");
			inp /= 2;
		}

		for (int i = ret.length() - 1; i >= 0; i--)
			r1.append(ret.charAt(i));

		while (r1.length() != btsz)
			r1.insert(0, "0");

		return r1.toString();
	}

	private static Byte StringToByte(String in) {
		int n = in.length();
		byte ret = 0;

		for (int i = 0; i < n; i++) {
			ret *= 2.;
			if (in.charAt(i) == '1')
				ret++;
		}

		for (; n < 8; n++)
			ret *= 2.;

		return ret;
	}

	private static int StringToInt(String s) {
		int ret = 0, i;
		for (i = 0; i < s.length(); i++) {
			ret *= 2;
			if (s.charAt(i) == '1')
				ret++;
		}
		return ret;
	}

	// Compress
	private static void Calculate(String fileis) {
		Map<String, Integer> dictionary = new HashMap<>();
		int dictSize = 256;
		for (int i = 0; i < 256; i++)
			dictionary.put("" + (char) i, i);
		int mpsz = 256;
		String w = "";

		try {
			DataInputStream data_in = new DataInputStream(new FileInputStream(new File(fileis)));

			while (true) {
				try {
					int ch = ByteToInt(data_in.readByte());
					String wc = w + (char) ch;
					if (dictionary.containsKey(wc))
						w = wc;
					else {
						if (mpsz < 100000) {
							dictionary.put(wc, dictSize++);
							mpsz += wc.length();
						}
						w = "" + (char) ch;
					}
				} catch (EOFException eof) {
					break;
				}
			}
			data_in.close();
		} catch (IOException e) {
			System.out.println("IO exception = " + e);
		}

		if (dictSize <= 1) {
			btsz = 1;
		} else {
			btsz = 0;
			long i = 1;
			while (i < dictSize) {
				i *= 2;
				btsz++;
			}
		}
	}

	public static void CompressLZW(String source, String dest) {
		Map<String, Integer> dictionary = new HashMap<>();
		int dictSize = 256;
		String big = "";
		int mpsz = 256;
		String w = "";

		btsz = 0;
		Calculate(source);

		for (int i = 0; i < 256; i++)
			dictionary.put("" + (char) i, i);

		try {
			DataInputStream data_in = new DataInputStream(new FileInputStream(new File(source)));
			DataOutputStream data_out = new DataOutputStream(new FileOutputStream(new File(dest)));

			data_out.writeInt(btsz);

			while (true) {
				try {
					Byte c = data_in.readByte();
					int ch = ByteToInt(c);

					String wc = w + (char) ch;
					if (dictionary.containsKey(wc))
						w = wc;
					else {
						big += IntToString(dictionary.get(w));
						while (big.length() >= 8) {
							data_out.write(StringToByte(big.substring(0, 8)));
							big = big.substring(8);
						}

						if (mpsz < 100000) {
							dictionary.put(wc, dictSize++);
							mpsz += wc.length();
						}
						w = "" + (char) ch;
					}
				} catch (EOFException eof) {
					break;
				}
			}

			if (!w.equals("")) {
				big += IntToString(dictionary.get(w));
				while (big.length() >= 8) {
					data_out.write(StringToByte(big.substring(0, 8)));
					big = big.substring(8);
				}
				if (big.length() >= 1)
					data_out.write(StringToByte(big));
			}
			data_in.close();
			data_out.close();
		} catch (IOException e) {
			System.out.println("IO exception = " + e);
		}
	}

	// Decompress
	private static void PreCalculate() {
		strBin[0] = "0";
		for (int i = 0; i < 256; i++) {
			String temp = "";
			int j = i;

			if (i != 0)
				strBin[i] = "";

			while (j != 0) {
				if ((j % 2) == 1)
					strBin[i] += "1";
				else
					strBin[i] += "0";
				j /= 2;
			}
			for (j = strBin[i].length() - 1; j >= 0; j--)
				temp += strBin[i].charAt(j);

			while (temp.length() < 8)
				temp = "0" + temp;

			strBin[i] = temp;
		}
	}

	public static void DecompressLZW(String source, String dest) {
		int k;
		int dictSize = 256;
		int mpsz = 256;
		String temp = "";
		String ts;
		Map<Integer, String> dictionary = new HashMap<>();

		btsz = 0;
		PreCalculate();

		for (int i = 0; i < 256; i++)
			dictionary.put(i, "" + (char) i);

		try {
			DataInputStream inputStream = new DataInputStream(new FileInputStream(new File(source)));
			DataOutputStream outputStream = new DataOutputStream(new FileOutputStream(new File(dest)));

			Byte c;
			btsz = inputStream.readInt();

			while (true) {
				try {
					c = inputStream.readByte();
					temp += strBin[ByteToInt(c)];
					if (temp.length() >= btsz)
						break;
				} catch (EOFException eof) {
					break;
				}
			}

			if (temp.length() >= btsz) {
				k = StringToInt(temp.substring(0, btsz));
				temp = temp.substring(btsz);
			} else {
				inputStream.close();
				outputStream.close();
				return;
			}

			String w = "" + (char) k;
			outputStream.writeBytes(w);

			while (true) {
				try {
					while (temp.length() < btsz) {
						c = inputStream.readByte();
						temp += strBin[ByteToInt(c)];
					}

					k = StringToInt(temp.substring(0, btsz));
					temp = temp.substring(btsz);

					String entry = "";
					if (dictionary.containsKey(k))
						entry = dictionary.get(k);
					else if (k == dictSize)
						entry = w + w.charAt(0);

					outputStream.writeBytes(entry);

					if (mpsz < 100000) {
						ts = w + entry.charAt(0);
						dictionary.put(dictSize++, ts);
						mpsz += ts.length();
					}
					w = entry;
				} catch (EOFException eof) {
					break;
				}
			}
			inputStream.close();
			outputStream.close();
		} catch (IOException e) {
			System.out.println("IO exception = " + e);
		}
	}

}