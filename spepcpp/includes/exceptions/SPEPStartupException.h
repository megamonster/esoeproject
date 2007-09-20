/* Copyright 2006-2007, Queensland University of Technology
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
 * Creation Date: 06/06/2007
 * 
 * Purpose: 
 */
 
#ifndef SPEPSTARTUPEXCEPTION_H_
#define SPEPSTARTUPEXCEPTION_H_

#include <exception>
#include <string>

namespace spep
{
	
	class SPEPStartupException : public std::exception
	{
		
		private:
		std::string _message;
		
		public:
		SPEPStartupException( std::string message );
		virtual ~SPEPStartupException() throw();
		
		virtual const char *what() const throw();
		
	};
	
}

#endif /*SPEPSTARTUPEXCEPTION_H_*/
