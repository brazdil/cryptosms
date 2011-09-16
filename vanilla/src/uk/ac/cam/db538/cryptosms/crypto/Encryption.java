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

public class Encryption {
	public static final int HMAC_LENGTH = 32;
	public static final int HASH_LENGTH = 32;

	public static final int SYM_IV_LENGTH = 16;
	public static final int SYM_BLOCK_LENGTH = 16;
	public static final int SYM_KEY_LENGTH = 32;
	public static final int SYM_OVERHEAD = SYM_IV_LENGTH + HMAC_LENGTH;
	
	public static final int ASYM_KEY_LENGTH = 60;
	public static final int ASYM_BLOCK_LENGTH = ASYM_KEY_LENGTH;
	public static final int ASYM_SIGNATURE_LENGTH = ASYM_KEY_LENGTH;
	
	private static EncryptionInterface mEncryption = null;
	
	public static EncryptionInterface getEncryption() {
		return mEncryption;
	}
	
	public static void setEncryption(EncryptionInterface crypto) {
		mEncryption = crypto;
	}
}
