package com.csu.ibe;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import uk.ac.ic.doc.jpair.api.Field;
import uk.ac.ic.doc.jpair.pairing.BigInt;
import uk.ac.ic.doc.jpair.pairing.EllipticCurve;
import uk.ac.ic.doc.jpair.pairing.Point;



public class Util {

	static byte[] hashToLength(byte[] toHash, int byteLength) {
		if (byteLength <= 0)
			throw new IllegalArgumentException("Invalid hash length");
		String hashAlgorithm = "SHA-512";

		int bitLength = byteLength * 8;
		int round;

		if (bitLength <= 512) {

			round = 1;

		} else {

			round = 1 + (bitLength - 1) / 512;

		}

		byte[] out = new byte[byteLength];

		byte[][] temp = new byte[round][];

		try {
			MessageDigest hash = MessageDigest.getInstance(hashAlgorithm);
			for (int i = 0; i < round; i++) {
				temp[i] = hash.digest(toHash);
				toHash = temp[i];
			}
		} catch (NoSuchAlgorithmException e) {
			System.exit(-1);
		}

		int startIndex = 0;
		// int length= byteLength;
		for (int i = 0; i < round; i++) {
			if (byteLength >= temp[i].length) {
				System.arraycopy(temp[i], 0, out, startIndex, temp[i].length);
				startIndex += temp[i].length;
				byteLength -= temp[i].length;
			} else
				System.arraycopy(temp[i], 0, out, startIndex, byteLength);
		}

		return out;

	}

	static BigInt hashToField(byte[] toHash, Field field) {
		int byteLength = 1 + (field.getP().bitLength() - 1) / 8;

		byte[] ba = Util.hashToLength(toHash, byteLength);

		BigInt b = new BigInt(1, ba);

		while (b.compareTo(field.getP()) >= 0)
			b = b.shiftRight(1);

		return b;

	}

	static Point hashToPoint(byte[] toHash, EllipticCurve ec) {

		BigInt b = Util.hashToField(toHash, ec.getField());

		Point P = ec.getPoint(b);

		while (P == null) {
			b = b.add(BigInt.ONE);
			P = ec.getPoint(b);
		}
		return P;
	}

	static Point hashToPoint(byte[] toHash, EllipticCurve ec, BigInt cofactor) {

		Point P = Util.hashToPoint(toHash, ec);
		P = ec.multiply(P, cofactor);
		return P;
	}

	//
	// public static void main(String[] args){
	// byte [] ba ="abc".getBytes();
	//
	// byte[] digest =HashUtil.hashToLength(ba, 87);
	//
	// }

	static byte[] xorTwoByteArrays(byte[] ba1, byte[] ba2) {
		byte[] result = new byte[ba1.length];
		for (int i = 0; i < ba1.length; i++) {
			result[i] = (byte) (ba1[i] ^ ba2[i]);
		}
		return result;
	}

}
