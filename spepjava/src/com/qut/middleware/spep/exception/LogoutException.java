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
 * Author: Shaun Mangelsdorf
 * Creation Date: 19/12/2006
 * 
 * Purpose: Reports that an error occurred while logging a principal out.
 */
package com.qut.middleware.spep.exception;


/** Reports that an error occurred while logging a principal out.*/
public class LogoutException extends Exception
{
	private static final long serialVersionUID = -7662722839702550275L;

	/**
	 * @param message The message explaining why this exception was thrown
	 */
	public LogoutException(String message)
	{
		super(message);
	}

	/**
	 * @param message The message explaining why this exception was thrown
	 * @param cause The exception that caused this exception to be thrown
	 */
	public LogoutException(String message, Exception cause)
	{
		super(message, cause);
	}
}