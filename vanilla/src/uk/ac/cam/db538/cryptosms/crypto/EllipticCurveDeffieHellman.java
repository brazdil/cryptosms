/*
 *   Copyright 2011 David Brazdil
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 */
package uk.ac.cam.db538.cryptosms.crypto;

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

/*
 * Class handling the ECDH key negotiation
 */
public class EllipticCurveDeffieHellman {
	public static final int LENGTH_PUBLIC_KEY = 33;
	public static final int LENGTH_PRIVATE_KEY = 32;

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
	
	/**
	 * Instantiates a new elliptic curve deffie hellman.
	 */
	public EllipticCurveDeffieHellman() {
        ECKeyGenerationParameters paramsKeyGen =
            	new ECKeyGenerationParameters(ECDH_PARAMS, mRandom);
        
        ECKeyPairGenerator keyGen = new ECKeyPairGenerator();
        keyGen.init(paramsKeyGen);
     
        mKeyPair = keyGen.generateKeyPair();
	}
	
	/**
	 * Instantiates a new elliptic curve deffie hellman.
	 *
	 * @param privateKey the private key
	 */
	public EllipticCurveDeffieHellman(byte[] privateKey) {
        ECKeyGenerationParameters paramsKeyGen =
            	new ECKeyGenerationParameters(ECDH_PARAMS, mRandom);
        
        ECKeyPairGenerator keyGen = new ECKeyPairGenerator();
        keyGen.init(paramsKeyGen);
     
        mKeyPair = keyGen.createKeyPair(new BigInteger(privateKey));
	}
	
	public byte[] getPublicKey() {
		return ((ECPublicKeyParameters) mKeyPair.getPublic()).getQ().getCompressed().getEncoded();
	}
	
	public byte[] getPrivateKey() {
		return ((ECPrivateKeyParameters) mKeyPair.getPrivate()).getD().toByteArray();
	}
	
	/**
	 * Gets the shared key.
	 *
	 * @param otherPublicKey the other public key
	 * @return the shared key
	 */
	public BigInteger getSharedKey(byte[] otherPublicKey) {
		ECPoint point = ECDH_CURVE.decodePoint(otherPublicKey);
		
		// return the shared secret
		ECDHBasicAgreement agreement = new ECDHBasicAgreement();
		agreement.init(mKeyPair.getPrivate());
		return agreement.calculateAgreement(new ECPublicKeyParameters(point, ECDH_PARAMS));
	}
}
