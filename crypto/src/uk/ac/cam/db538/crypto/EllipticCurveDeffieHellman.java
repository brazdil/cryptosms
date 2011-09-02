package uk.ac.cam.db538.crypto;

import java.math.BigInteger;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

import org.spongycastle.crypto.AsymmetricCipherKeyPair;
import org.spongycastle.crypto.agreement.ECDHBasicAgreement;
import org.spongycastle.crypto.generators.ECKeyPairGenerator;
import org.spongycastle.crypto.params.ECDomainParameters;
import org.spongycastle.crypto.params.ECKeyGenerationParameters;
import org.spongycastle.crypto.params.ECPrivateKeyParameters;
import org.spongycastle.crypto.params.ECPublicKeyParameters;
import org.spongycastle.math.ec.ECCurve;
import org.spongycastle.math.ec.ECPoint;
import org.spongycastle.util.encoders.Hex;

public class EllipticCurveDeffieHellman {
	public static final BigInteger ECDH_P = new BigInteger("0FFFFFFFF00000001000000000000000000000000FFFFFFFFFFFFFFFFFFFFFFFF", 16);
	public static final BigInteger ECDH_A = new BigInteger("0FFFFFFFF00000001000000000000000000000000FFFFFFFFFFFFFFFFFFFFFFFC", 16);
	public static final BigInteger ECDH_B = new BigInteger("05AC635D8AA3A93E7B3EBBD55769886BC651D06B0CC53B0F63BCE3C3E27D2604B", 16);
	public static final BigInteger ECDH_N = new BigInteger("0FFFFFFFF00000000FFFFFFFFFFFFFFFFBCE6FAADA7179E84F3B9CAC2FC632551", 16);
	public static final BigInteger ECDH_H = BigInteger.valueOf(1);
	
	public static final BigInteger ECDH_G_X = new BigInteger("06B17D1F2E12C4247F8BCE6E563A440F277037D812DEB33A0F4A13945D898C296", 16);
	public static final BigInteger ECDH_G_Y = new BigInteger("04FE342E2FE1A7F9B8EE7EB4A7C0F9E162BCE33576B315ECECBB6406837BF51F5", 16);
	public static final byte[] ECDH_G = Hex.decode("036B17D1F2E12C4247F8BCE6E563A440F277037D812DEB33A0F4A13945D898C296");

	public static final ECCurve.Fp ECDH_CURVE = new ECCurve.Fp(ECDH_P, ECDH_A, ECDH_B);
	public static final ECDomainParameters ECDH_PARAMS = new ECDomainParameters(ECDH_CURVE, ECDH_CURVE.decodePoint(ECDH_G), ECDH_N, ECDH_H);
	
	private static SecureRandom mRandom;
	static {
        try {
            mRandom = SecureRandom.getInstance("SHA1PRNG");
        } catch (NoSuchAlgorithmException e){
            throw new RuntimeException("No secure random available!");
        }
	}
	
	private AsymmetricCipherKeyPair mKeyPair;
	
	public EllipticCurveDeffieHellman() {
        ECKeyGenerationParameters paramsKeyGen =
            	new ECKeyGenerationParameters(ECDH_PARAMS, mRandom);
        
        ECKeyPairGenerator keyGen = new ECKeyPairGenerator();
        keyGen.init(paramsKeyGen);
     
        mKeyPair = keyGen.generateKeyPair();
	}
	
	public EllipticCurveDeffieHellman(byte[] privateKey) {
        ECKeyGenerationParameters paramsKeyGen =
            	new ECKeyGenerationParameters(ECDH_PARAMS, mRandom);
        
        ECKeyPairGenerator keyGen = new ECKeyPairGenerator();
        keyGen.init(paramsKeyGen);
     
        mKeyPair = keyGen.createKeyPair(new BigInteger(privateKey));
	}
	
	public byte[] getPublicKey() {
		return ((ECPublicKeyParameters) mKeyPair.getPublic()).getQ().getCompressed().getEncoded();
//		// first byte is always 4 - get rid of it
//		byte[] cropped = new byte[Q.length - 1];
//		System.arraycopy(Q, 1, cropped, 0, Q.length - 1);
//		return cropped;
	}
	
	public byte[] getPrivateKey() {
		return ((ECPrivateKeyParameters) mKeyPair.getPrivate()).getD().toByteArray();
	}
	
	public BigInteger getSharedKey(byte[] otherPublicKey) {
//		// add the leading byte back
//		byte[] otherPointData = new byte[otherPublicKey.length + 1];
//		otherPointData[0] = 0x04;
//		System.arraycopy(otherPublicKey, 0, otherPointData, 1, otherPublicKey.length);
		ECPoint point = ECDH_CURVE.decodePoint(otherPublicKey);
		
		// return the shared secret
		ECDHBasicAgreement agreement = new ECDHBasicAgreement();
		agreement.init(mKeyPair.getPrivate());
		return agreement.calculateAgreement(new ECPublicKeyParameters(point, ECDH_PARAMS));
	}
	
	/**
	 * @param args
	 */
//	public static void main(String[] args) {
//		EllipticCurveDeffieHellman alice = new EllipticCurveDeffieHellman();
//		byte[] privA = alice.getPrivateKey();
//		System.out.println("Private A: 0x" + LowLevel.toHex(privA));
//		byte[] pubA = alice.getPublicKey();
//		String hexA = LowLevel.toHex(pubA);
//		System.out.println("Public A: 0x" + hexA);
//		System.out.println((pubA.length) + " bytes");
////		String hexAX = hexA.substring(0, 64);
////		String hexAY = hexA.substring(64, 128);
////		System.out.println("hexAX: 0x" + hexAX);
////		System.out.println("hexAY: 0x" + hexAY);
////		System.out.println("AX: " + (new BigInteger("0" + hexAX, 16)));
////		System.out.println("AY: " + (new BigInteger("0" + hexAY, 16)));
//		
//		EllipticCurveDeffieHellman bob = new EllipticCurveDeffieHellman();
//		System.out.println("Private B: 0x" + LowLevel.toHex(bob.getPrivateKey()));
//		byte[] pubB = bob.getPublicKey();
//		String hexB = LowLevel.toHex(pubB);
//		System.out.println("Public B: 0x" + hexB);
//		System.out.println((pubB.length) + " bytes");
////		String hexBX = hexB.substring(0, 64);
////		String hexBY = hexB.substring(64, 128);
////		System.out.println("hexBX: 0x" + hexBX);
////		System.out.println("hexBY: 0x" + hexBY);
////		System.out.println("BX: " + (new BigInteger("0" + hexBX, 16)));
////		System.out.println("BY: " + (new BigInteger("0" + hexBY, 16)));
//		
//		System.out.println("Shared A: " + (alice.getSharedKey(pubB)));
//		System.out.println("Shared B: " + (bob.getSharedKey(pubA)));
//		
//		EllipticCurveDeffieHellman alice2 = new EllipticCurveDeffieHellman(privA);
//		System.out.println("Recreated A: 0x" + LowLevel.toHex(alice2.getPrivateKey()));
//		System.out.println("Recreated shared A: " + alice2.getSharedKey(pubB));
//	}
}
