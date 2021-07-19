import java.io.File;
import java.util.Scanner;

public class Main {

	public static void main(String[] args) {
		Scanner sc = new Scanner(System.in);

		System.out.println("Please input filename: ");
		String filename = sc.nextLine();
		Huffman.CompressHuffman(filename, filename + ".hz");
		LZW.CompressLZW(filename, filename + ".lzw");
		LZW.CompressLZW(filename + ".hz", filename + ".az");

		long file_size = new File(filename).length();
		long huff_size = new File(filename + ".hz").length();
		long lzw_size = new File(filename + ".lzw").length();
		long huff_lzw_size = new File(filename + ".az").length();

		System.out.println("Original File size: " + file_size);
		System.out.println("Huffman File size: " + huff_size);
		System.out.println("LZW File size: " + lzw_size);
		System.out.println("Huffman-LZW File size: " + huff_lzw_size);

		Huffman.DecompressHuffman(filename + ".hz", filename + ".hz.unz");
		LZW.DecompressLZW(filename + ".lzw", filename + ".lzw.unz");
		LZW.DecompressLZW(filename + ".az", filename + ".az.temp");
		Huffman.DecompressHuffman(filename + ".az.temp", filename + ".az.unz");
		new File(filename + ".az.temp").delete();
	}
}
