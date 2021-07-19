
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.PriorityQueue;

public class Huffman {
	private static int[] freq = new int[256];
	private static String[] strlist = new String[256];
	private static int count;
	private static String[] strBin = new String[300]; // INT TO BIN
	private static int exbits; // EXTRA BITS ADDED AT THE LAST TO MAKE THE FINAL ZIP


	// main tree class
	private static class Node implements Comparable<Node> {
		Node left;
		Node right;
		public int key;
		public int freq;

		Node(int k, int f, Node l, Node r) {
			key = k;
			freq = f;
			left = l;
			right = r;
		}

		public int compareTo(Node T) {
			if (this.freq < T.freq)
				return -1;
			else if (this.freq > T.freq)
				return 1;
			return 0;
		}
	}

	private static Node root;

	// Common helpers
	private static void initialize() {
		int i;
		for (i = 0; i < 256; i++)
			freq[i] = 0;
		for (i = 0; i < 256; i++)
			strlist[i] = "";
		exbits = 0;
		count = 0;
	}

	private static int BytetoInt(Byte b) {
		int ret = b;
		if (ret < 0) {
			ret = ~b;
			ret = ret + 1;
			ret = ret ^ 255;
			ret += 1;
		}
		return ret;
	}

	private static void MakeString(Node node, String value) {
		if ((node.left == null) && (node.right == null)) {
			strlist[node.key] = value;
			return;
		}
		if (node.left != null)
			MakeString(node.left, value + "0");
		if (node.right != null)
			MakeString(node.right, value + "1");
	}

	private static void BuildTree() {
		PriorityQueue<Node> pq = new PriorityQueue<>();
		count = 0;
		for (int i = 0; i < 256; i++) {
			if (freq[i] != 0) {
				Node node = new Node(i, freq[i], null, null);
				pq.add(node);
				count++;
			}
		}

		if (count == 0)
			return;
		else if (count == 1)
			for (int i = 0; i < 256; i++)
				if (freq[i] != 0) {
					strlist[i] = "0";
					return;
				}

		while (pq.size() != 1) {
			Node a, b;
			a = pq.poll();
			b = pq.poll();
			Node Temp = new Node(256, a.freq + b.freq , a, b);
			pq.add(Temp);
		}
		root = pq.poll();
	}

	// Compress
	private static void getFrequency(String fname) {
		try {
			DataInputStream inputStream = new DataInputStream(new FileInputStream(new File(fname)));
			while (true) {
				try {
					Byte bt = inputStream.readByte();
					freq[BytetoInt(bt)]++;
				} catch (EOFException eof) {
					break;
				}
			}
			inputStream.close();
		} catch (IOException e) {
			System.out.println("IO Exception =: " + e);
		}
	}

	private static void MakeTemp(String filename) {
		try {
			DataInputStream inputStream = new DataInputStream(new FileInputStream(new File(filename)));
			PrintStream ps = new PrintStream(new File("temp.txt"));

			while (true) {
				try {
					ps.print(strlist[BytetoInt(inputStream.readByte())]);
				} catch (EOFException eof) {
					break;
				}
			}

			inputStream.close();
			ps.close();

		} catch (IOException e) {
			System.out.println("IO Exception =: " + e);
		}
	}

	private static void Compress(String tempfile, String compressfile) {
		try {
			DataInputStream data_in = new DataInputStream(new FileInputStream(new File(tempfile)));
			DataOutputStream data_out = new DataOutputStream(new FileOutputStream(new File(compressfile)));

			data_out.writeInt(count);
			for (int i = 0; i < 256; i++) {
				if (freq[i] != 0) {
					Byte btt = (byte) i;
					data_out.write(btt);
					data_out.writeInt(freq[i]);
				}
			}
			long texbits = new File(tempfile).length() % 8;
			texbits = (8 - texbits) % 8;

			byte bt = 0;
			int exbits = (int) texbits;
			data_out.writeInt(exbits);

			while (true) {
				try {
					bt = 0;
					byte ch;
					for (exbits = 0; exbits < 8; exbits++) {
						ch = data_in.readByte();
						bt *= 2;
						if (ch == '1')
							bt++;
					}
					data_out.write(bt);

				} catch (EOFException eof) {
					if (exbits != 0) {
						for (int x = exbits; x < 8; x++) {
							bt *= 2;
						}
						data_out.write(bt);
					}
					break;
				}
			}
			data_in.close();
			data_out.close();
		} catch (IOException e) {
			System.out.println("IO exception = " + e);
		}
		(new File(tempfile)).delete();
	}

	public static void CompressHuffman(String source, String dest) {
		initialize();
		getFrequency(source);
		BuildTree();
		if (count > 1)
			MakeString(root, "");
		MakeTemp(source);
		Compress("temp.txt", dest);
	}

	// Decompress
	private static void ReadFrequency(String filename) {
		try {
			DataInputStream dataInputStream = new DataInputStream(new FileInputStream(new File(filename)));
			count = dataInputStream.readInt();

			for (int i = 0; i < count; i++) {
				Byte byteV = dataInputStream.readByte();
				freq[BytetoInt(byteV)] = dataInputStream.readInt();
			}
			dataInputStream.close();
		} catch (IOException e) {
			System.out.println("IO exception = " + e);
		}

		BuildTree(); // makeing corresponding nodes
		if (count > 1)
			MakeString(root, ""); // dfs1 to make the codes

		for (int i = 0; i < 256; i++) {
			if (strlist[i] == null)
				strlist[i] = "";
		}
	}

	private static void CreateBin() {
		for (int i = 0; i < 256; i++) {
			strBin[i] = "";
			int j = i;
			while (j != 0) {
				if (j % 2 == 1)
					strBin[i] += "1";
				else
					strBin[i] += "0";
				j /= 2;
			}
			StringBuilder t = new StringBuilder();
			for (j = strBin[i].length() - 1; j >= 0; j--)
				t.append(strBin[i].charAt(j));
			strBin[i] = t.toString();
		}

		strBin[0] = "0";
	}

	private static String MakeEight(String str) {
		StringBuilder ret = new StringBuilder();
		int len = str.length();
		for (int i = 0; i < (8 - len); i++)
			ret.append("0");
		ret.append(str);
		return ret.toString();
	}

	private static void Decompress(String source, String dest) {
		StringBuilder bigone = new StringBuilder();
		try {
			DataInputStream dataInputStream = new DataInputStream(new FileInputStream(new File(source)));
			DataOutputStream dataOutputStream = new DataOutputStream(new FileOutputStream(new File(dest)));

			try {
				count = dataInputStream.readInt();
				for (int i = 0; i < count; i++) {
					dataInputStream.readByte();
					dataInputStream.readInt();
				}
				exbits = dataInputStream.readInt();
			} catch (EOFException ignored) {}

			while (true) {
				try {
					Byte b = dataInputStream.readByte();
					int bt = BytetoInt(b);
					bigone.append(MakeEight(strBin[bt]));
					int ok;

					do {
						ok = 1;
						String temp = "";
						for (int i = 0; i < bigone.length() - exbits; i++) {
							temp += bigone.charAt(i);
							int putit = -1;
							for (int j = 0; j < 256; j++)
								if (strlist[j].compareTo(temp) == 0)
									putit = j;

							if (putit != -1) {
								dataOutputStream.write(putit);
								ok = 0;

								StringBuilder s = new StringBuilder();
								for (int j = temp.length(); j < bigone.length(); j++)
									s.append(bigone.charAt(j));

								bigone = s;
								break;
							}
						}

					} while (ok != 1);
				} catch (EOFException eof) {
					break;
				}
			}
			dataInputStream.close();
			dataOutputStream.close();
		} catch (IOException e) {
			System.out.println("IO Exception =: " + e);
		}
	}

	public static void DecompressHuffman(String source, String dest) {
		initialize();
		ReadFrequency(source);
		CreateBin();
		Decompress(source, dest);
	}

}