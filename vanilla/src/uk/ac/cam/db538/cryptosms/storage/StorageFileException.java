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
package uk.ac.cam.db538.cryptosms.storage;

/**
 * 
 * Exception thrown by the Database class object
 * 
 * @author David Brazdil
 *
 */
public class StorageFileException extends Exception {
	private static final long serialVersionUID = -7100685462486843982L;

	/**
	 * Instantiates a new storage file exception.
	 *
	 * @param message the message
	 */
	public StorageFileException(String message) {
		super(message);
	}

	/**
	 * Instantiates a new storage file exception.
	 *
	 * @param e the e
	 */
	public StorageFileException(Exception e) {
		super(e.getMessage());
		initCause(e);
	}
}
