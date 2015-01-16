package com.csu.ibe;

import java.util.Random;

import uk.ac.ic.doc.jpair.api.Pairing;
import uk.ac.ic.doc.jpair.ibe.key.BFMasterPublicKey;
import uk.ac.ic.doc.jpair.ibe.key.BFUserPrivateKey;
import uk.ac.ic.doc.jpair.ibe.key.BFUserPublicKey;
import uk.ac.ic.doc.jpair.pairing.BigInt;
import uk.ac.ic.doc.jpair.pairing.Point;
import uk.ac.ic.doc.jpair.pairing.Predefined;



public class ClientEnc {
	public MYCtext myenc(String reciever,String txt) {

    	// TODO Auto-generated method stub
 		String ID = reciever;
 		
 		byte[] m = txt.getBytes();
 		 		
 		Random rnd = new Random();
 		
 		Pairing e = Predefined.ssTate();
 		
 		// reconstruct the master public key
 		BigInt Px = new BigInt(IBEConfig.PX);
 		BigInt Py = new BigInt(IBEConfig.PY);
 		BigInt Ppubx = new BigInt(IBEConfig.PpubX);
 		BigInt Ppuby = new BigInt(IBEConfig.PpubY);

 		/**
 		 * the generator of G
 		 */
 		Point P = new Point(Px, Py);
 		/**
 		 * the master public key
 		 */
 		Point Ppub = new Point(Ppubx, Ppuby);

 		BFMasterPublicKey mpk = new BFMasterPublicKey(e, P, Ppub);
 		BFUserPublicKey upk = new BFUserPublicKey(ID, mpk);

 		byte[] sigma = new byte[MYCipher.sigmaBitLength / 8];
 		rnd.nextBytes(sigma);
 		BigInt intsigma = new BigInt(sigma);
 		MYCtext c = null;
 		c = MYCipher.encrypt(upk, m, sigma);
		
  		return c;
 		
	}
	
	public String mydec(MYCtext c1,Point privatekey) {

		// TODO Auto-generated method stub
		
		Pairing e = Predefined.ssTate();

		// reconstruct the master public key
		BigInt Px = new BigInt(IBEConfig.PX);
 		BigInt Py = new BigInt(IBEConfig.PY);
 		BigInt Ppubx = new BigInt(IBEConfig.PpubX);
 		BigInt Ppuby = new BigInt(IBEConfig.PpubY);
 		
		Point P = new Point(Px, Py);
		Point Ppub = new Point(Ppubx, Ppuby);

		BFMasterPublicKey mpk = new BFMasterPublicKey(e, P, Ppub);
		
		BFUserPrivateKey usk = new BFUserPrivateKey(privatekey, mpk);

		byte[] dec = null;
		dec = MYCipher.decrypt(c1, usk);
		String strdec = "您没有权限阅读此条密文";
		if(dec!=null)
			strdec = new String(dec);
		
		return strdec;
	}
}
