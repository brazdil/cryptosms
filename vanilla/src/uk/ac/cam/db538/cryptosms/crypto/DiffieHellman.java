package uk.ac.cam.db538.cryptosms.crypto;

import java.security.SecureRandom;
import java.math.BigInteger;
import java.security.NoSuchAlgorithmException;

import uk.ac.cam.db538.cryptosms.utils.LowLevel;

/**
 * Implements the underlying Diffie-Hellman cryptography.
 */
public class DiffieHellman 
{
//    P (3072): b47e1eda65d8bfd1b4ee9d620bf2d6f04142f61f6cd04cfb7eb174e2177b199e67b4cefaaa38511543712773d24d61486c833d8484d0f54889435afc99a4b3eb0c956356f3b77450ad3d7289990b719a7ace736cd53af29c404435ec131aefeaa4aa1f5a037197d3c2e5fb27aeff2af81f6b59616cf093baae40eb31c8d69031f0b05369ea80c161f42d7420be5332966afa5aa47217d17190fe08adf4293c9883cee369f20e8150cc7324ead87d7025106a558ada8464871ee200c19a92e617abb23219a0986063de611fec23624f076721ed476b674371b27a9a9123b3a6538cc08ac159088d785a76fb6f612a2b0d1c3a6a96077291187bf8be287c06927fd4cd4f2c8d5fadf6153b2e3db137517eedc06f4534082b607298be8f06845e6d95d28dfe41ff18ef123bf52d8dea9cd04880492927e2244ff006497ac94bf4ff607916707091ef5a00d08b118390407e9de0dc200d2bb28df3fe1fdb55d7ced48370a3ee82d0900f049da7776d3a85a82a63b9cca19a1400e73b02a8dbccd22bh
//    Q (3071): 5a3f0f6d32ec5fe8da774eb105f96b7820a17b0fb668267dbf58ba710bbd8ccf33da677d551c288aa1b893b9e926b0a436419ec242687aa444a1ad7e4cd259f5864ab1ab79dbba28569eb944cc85b8cd3d6739b66a9d794e20221af6098d77f552550fad01b8cbe9e172fd93d77f957c0fb5acb0b67849dd57207598e46b4818f85829b4f54060b0fa16ba105f29994b357d2d52390be8b8c87f0456fa149e4c41e771b4f90740a8663992756c3eb81288352ac56d4232438f710060cd49730bd5d9190cd04c3031ef308ff611b12783b390f6a3b5b3a1b8d93d4d4891d9d329c6604560ac8446bc2d3b7db7b09515868e1d354b03b9488c3dfc5f143e03493fea66a79646afd6fb0a9d971ed89ba8bf76e037a29a0415b0394c5f4783422f36cae946ff20ff8c77891dfa96c6f54e682440249493f11227f80324bd64a5fa7fb03c8b383848f7ad00684588c1c8203f4ef06e100695d946f9ff0fedaaebe76a41b851f741684807824ed3bbb69d42d41531dce650cd0a00739d81546de66915h
//    G (2): 3.

	private final static BigInteger DEFAULT_MODULUS 
		= new BigInteger(LowLevel.fromHex(
				"b47e1eda65d8bfd1b4ee9d620bf2d6f04142f61f6cd04cfb7eb174e2177b199e67b4cefaaa38511543712773d24d61486c833d8484d0f54889435afc99a4b3eb0c956356f3b77450ad3d7289990b719a7ace736cd53af29c404435ec131aefeaa4aa1f5a037197d3c2e5fb27aeff2af81f6b59616cf093baae40eb31c8d69031f0b05369ea80c161f42d7420be5332966afa5aa47217d17190fe08adf4293c9883cee369f20e8150cc7324ead87d7025106a558ada8464871ee200c19a92e617abb23219a0986063de611fec23624f076721ed476b674371b27a9a9123b3a6538cc08ac159088d785a76fb6f612a2b0d1c3a6a96077291187bf8be287c06927fd4cd4f2c8d5fadf6153b2e3db137517eedc06f4534082b607298be8f06845e6d95d28dfe41ff18ef123bf52d8dea9cd04880492927e2244ff006497ac94bf4ff607916707091ef5a00d08b118390407e9de0dc200d2bb28df3fe1fdb55d7ced48370a3ee82d0900f049da7776d3a85a82a63b9cca19a1400e73b02a8dbccd22bh"));
	private final static BigInteger DEFAULT_KEY_MAX 
		= new BigInteger(LowLevel.fromHex(
			"5a3f0f6d32ec5fe8da774eb105f96b7820a17b0fb668267dbf58ba710bbd8ccf33da677d551c288aa1b893b9e926b0a436419ec242687aa444a1ad7e4cd259f5864ab1ab79dbba28569eb944cc85b8cd3d6739b66a9d794e20221af6098d77f552550fad01b8cbe9e172fd93d77f957c0fb5acb0b67849dd57207598e46b4818f85829b4f54060b0fa16ba105f29994b357d2d52390be8b8c87f0456fa149e4c41e771b4f90740a8663992756c3eb81288352ac56d4232438f710060cd49730bd5d9190cd04c3031ef308ff611b12783b390f6a3b5b3a1b8d93d4d4891d9d329c6604560ac8446bc2d3b7db7b09515868e1d354b03b9488c3dfc5f143e03493fea66a79646afd6fb0a9d971ed89ba8bf76e037a29a0415b0394c5f4783422f36cae946ff20ff8c77891dfa96c6f54e682440249493f11227f80324bd64a5fa7fb03c8b383848f7ad00684588c1c8203f4ef06e100695d946f9ff0fedaaebe76a41b851f741684807824ed3bbb69d42d41531dce650cd0a00739d81546de66915h"));
	private final static BigInteger DEFAULT_GENERATOR = BigInteger.valueOf(3);
	
    public static DiffieHellman getDefault()
    {
    	return new DiffieHellman(
    		DEFAULT_MODULUS,
    		DEFAULT_KEY_MAX,
    		DEFAULT_GENERATOR
    	);
    }
    
    private BigInteger mModulus;
    private BigInteger mKeyMaximum;
    private BigInteger mGenerator;
    
    private BigInteger mPrivateKey;

    /**
     * Creates a DiffieHellman instance.
     *
     * @param mod 	the modulus to use (P)
     * @param max 	the maximum value for private keys to use (Q)
     * @param gen 	the generator to use (G)
     */
    public DiffieHellman(BigInteger mod, BigInteger max, BigInteger gen)
    {
    	mModulus = mod;
    	mKeyMaximum = max;
    	mGenerator = gen;
    	
    	// check primes are long enough
    	if (mModulus.bitLength() < Encryption.DH_MIN_MODULUS_BITLENGTH)
    		throw new IllegalArgumentException("Modulus is not long enough");
    	if (mKeyMaximum.bitLength() < Encryption.DH_MIN_KEYMAX_BITLENGTH)
    		throw new IllegalArgumentException("Key maximum is not long enough");
    	
    	// check primes are really primes
    	if (!mModulus.isProbablePrime(Integer.MAX_VALUE))
    		throw new IllegalArgumentException("Modulus is probably not a prime");
    	if (!mKeyMaximum.isProbablePrime(Integer.MAX_VALUE))
    		throw new IllegalArgumentException("Key maximum is probably not a prime");
    	
    	// check q | (p - 1)
    	if (!mModulus.subtract(BigInteger.ONE).remainder(mKeyMaximum).equals(BigInteger.ZERO))
    		throw new IllegalArgumentException("Key maximum has to be a divider of (modulus - 1)");
    	
    	// check g != 1
    	if (mGenerator.equals(BigInteger.ONE))
    		throw new IllegalArgumentException("Generator can't equal 1");

    	// check g^q % p = 1
    	if (!mGenerator.modPow(mKeyMaximum, mModulus).equals(BigInteger.ONE))
    		throw new IllegalArgumentException("g^q % p = 1 not satisfied");
    }
    
    /*
     * Generated new public/private key pair for the Diffie-Hellman exchange
     */
    public void generateKeys() {
    	// choose x, such that x > 1 && x < q - 1
        int bitLength = mKeyMaximum.bitLength();
        BigInteger key;
		while (true) {
			key = new BigInteger(bitLength, Encryption.getEncryption().getRandom());
			if (canBePrivateKey(key))
				break;
		}
		setPrivateKey(key);
    }
    
    private boolean canBePrivateKey(BigInteger key) {
	    return (mPrivateKey.compareTo(BigInteger.ONE) > 0 && 
		    	mPrivateKey.compareTo(mKeyMaximum.subtract(BigInteger.ONE)) < 0);
    }
    
    public void setPrivateKey(BigInteger key) {
    	if (canBePrivateKey(key))
    		mPrivateKey = key;
    	else
	    	throw new IllegalArgumentException("Private key has to be between 1 and KeyMax-1");
    }
    
    public BigInteger getPublicKey() {
	    return mGenerator.modPow(mPrivateKey, mModulus);
    }
    
    public BigInteger getSharedKey(BigInteger otherPublicKey) {
    	// check the other public key
    	if (otherPublicKey.equals(BigInteger.ONE))
    		throw new IllegalArgumentException("Public key can't be 1");
    	if (!otherPublicKey.modPow(mKeyMaximum, mModulus).equals(BigInteger.ONE))
    		throw new IllegalArgumentException("Public key was probably generated with different Diffie-Hellman paramters");
    	
    	return otherPublicKey.modPow(mPrivateKey, mModulus);
    }
}