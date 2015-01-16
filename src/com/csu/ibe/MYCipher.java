package com.csu.ibe;

import java.io.UnsupportedEncodingException;

import uk.ac.ic.doc.jpair.api.Field;
import uk.ac.ic.doc.jpair.api.FieldElement;
import uk.ac.ic.doc.jpair.api.Pairing;
import uk.ac.ic.doc.jpair.ibe.key.BFMasterPublicKey;
import uk.ac.ic.doc.jpair.ibe.key.BFUserPrivateKey;
import uk.ac.ic.doc.jpair.ibe.key.BFUserPublicKey;
import uk.ac.ic.doc.jpair.pairing.BigInt;
import uk.ac.ic.doc.jpair.pairing.Point;



public class MYCipher {

	public static final int sigmaBitLength = 512;
	public static final String charSet = "UTF-8";

	

	
	/**
	 * The Encrypt algorithm which encrypts data using the public key of a user.
	 * 
	 * @param pk
	 *            a user's public key
	 * @param m
	 *            the data to be encrypted, the size of the data is arbitrary
	 * @param rnd
	 *            source of randomness
	 * @return the ciphertext generated from the data
	 */

	public static MYCtext encrypt(BFUserPublicKey upk, byte[] m, byte[] sigma) {
		Pairing e = upk.gerParam().getPairing();

		// sigma||m
		byte[] toHash = new byte[sigma.length + m.length];
		System.arraycopy(sigma, 0, toHash, 0, sigma.length);
		System.arraycopy(m, 0, toHash, sigma.length, m.length);

		// hash(sigma||m) to biginteger r;
		Field field = e.getCurve2().getField();
		BigInt r = Util.hashToField(toHash, field);

		// hash(ID) to point
		byte[] bid = null;
		String ID = upk.gerKey();
		try {
			bid = ID.getBytes(charSet);
		} catch (UnsupportedEncodingException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}

		Point Q = Util.hashToPoint(bid, e.getCurve(), e.getCofactor());

		// gID = e(Q, sP), sP is Ppub
		FieldElement gID = e.compute(Q, upk.gerParam().getPpub());
		// U=rP
		Point U = e.getCurve2().multiply(upk.gerParam().getP(), r);
		// gID^r
		FieldElement gIDr = e.getGt().pow(gID, r);

		// V=sigma xor hash(gID^r)
		byte[] hash = Util.hashToLength(gIDr.toByteArray(), sigma.length);
		byte[] V = Util.xorTwoByteArrays(sigma, hash);

		// W =m xor hash(sigma)
		hash = Util.hashToLength(sigma, m.length);
		byte[] W = Util.xorTwoByteArrays(m, hash);

		return new MYCtext(U, V, W);

	}

	/**
	 * The Decrypt algorithm which decrypt a ciphertext using the corresponding
	 * private key.
	 * 
	 * @param c
	 *            the ciphertext to be decrypted
	 * @param sk
	 *            the user's private key
	 * @return the plaintext data or null if the decryption fails.
	 */
	public static byte[] decrypt(MYCtext c, BFUserPrivateKey sk) {
		BFMasterPublicKey mpk = sk.getParam();
		Pairing e = mpk.getPairing();

		// e(sQ,U), sQ is the user private key
		FieldElement temp = e.compute(sk.getKey(), c.getU());

		// sigma = V xor hash(temp)
		byte[] hash = Util.hashToLength(temp.toByteArray(), c.V.length);

		byte[] sigma = Util.xorTwoByteArrays(c.V, hash);

		hash = Util.hashToLength(sigma, c.W.length);

		byte[] m = Util.xorTwoByteArrays(hash, c.W);

		// sigma||m
		byte[] toHash = new byte[sigma.length + m.length];
		System.arraycopy(sigma, 0, toHash, 0, sigma.length);
		System.arraycopy(m, 0, toHash, sigma.length, m.length);

		// hash(sigma||m) to biginteger r;
		Field field = e.getCurve2().getField();
		BigInt r = Util.hashToField(toHash, field);

		if (c.U.equals(e.getCurve2().multiply(mpk.getP(), r)))
			return m;
		else
			return null;

	}

}
