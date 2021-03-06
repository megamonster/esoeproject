/* 
 * Copyright 2006, Queensland University of Technology
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not 
 * use this file except in compliance with the License. You may obtain a copy of 
 * the License at 
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0 
 * 
 * Unless required by applicable law or agreed to in writing, software 
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT 
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the 
 * License for the specific language governing permissions and limitations under 
 * the License.
 * 
 * Author: Bradley Beddoes
 * Creation Date: 25/10/2006
 * 
 * Purpose: Resolves keys that have participated in XML signing from some externally defined source
 */
package com.qut.middleware.saml2;

import java.math.BigInteger;
import java.security.PublicKey;

import com.qut.middleware.saml2.exception.KeyResolutionException;

/** Resolves keys that have participated in XML signing from some externally defined source. */
public interface ExternalKeyResolver
{
	
	/**
	 * Resolves keys from some external source (SAML metadata, database, ldap etc) to validate XML document.
	 * 
	 * @param keyName Unique name of the key to resolve, all keys in the system must have a unique name
	 * @return The public key represented by supplied name
	 */
	public PublicKey resolveKey(String keyName) throws KeyResolutionException;
	
	/**
	 * Resolves keys from some external source (SAML metadata, database, ldap etc) to validate XML document.
	 * 
	 * @param issuerDN The issuer DN of the key to resolve
	 * @param serialNumber The serial number of the key to resolve. This must be unique per issuer DN.
	 * @return The public key represented by supplied name
	 */
	public PublicKey resolveKey(String issuerDN, BigInteger serialNumber) throws KeyResolutionException;
}
