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
 * Creation Date: 08/02/2007
 * 
 * Purpose: 
 */
 
#include "ipc/Dispatcher.h"

#include "reporting/ReportingLevels.h"

spep::ipc::MultifacetedDispatcher::MultifacetedDispatcher( spep::ReportingProcessor *reportingProcessor, std::vector<Dispatcher*> dispatchers )
: 
_reportingProcessor( "spep::ipc::MultifacetedDispatcher", reportingProcessor ),
_dispatchers(dispatchers) 
{}

spep::ipc::MultifacetedDispatcher::~MultifacetedDispatcher()
{
	// Free each dispatcher in the list..
	for( DispatcherIterator iter = _dispatchers.begin(); iter != _dispatchers.end(); ++iter )
	{
		delete *iter;
	}
}

bool spep::ipc::MultifacetedDispatcher::dispatch( spep::ipc::MessageHeader &header, spep::ipc::Engine &en )
{
	this->_reportingProcessor.log( spep::DEBUG, "Dispatching request bound for " + header.getDispatch() );
	
	// For each dispatcher in the list..
	for( DispatcherIterator iter = _dispatchers.begin(); iter != _dispatchers.end(); ++iter )
	{
		Dispatcher *dispatcher = (*iter);
		// If it dispatches return true.
		if ( dispatcher->dispatch( header, en ) )
			return true;
	}
	
	this->_reportingProcessor.log( spep::DEBUG, "No dispatcher for " + header.getDispatch() + ". Failing." );
	// All dispatchers returned false, so we can as well.
	return false;
}

spep::ipc::ExceptionCatchingDispatcher::ExceptionCatchingDispatcher( spep::ReportingProcessor *reportingProcessor, spep::ipc::Dispatcher* nextDispatcher )
:
_localReportingProcessor( reportingProcessor->localReportingProcessor( "spep::ipc::ExceptionCatchingDispatcher" ) ),
_nextDispatcher( nextDispatcher )
{
}

spep::ipc::ExceptionCatchingDispatcher::~ExceptionCatchingDispatcher()
{
}

bool spep::ipc::ExceptionCatchingDispatcher::dispatch( spep::ipc::MessageHeader &header, spep::ipc::Engine &engine )
{
	try
	{
		// If everything happens as it should, return the result directly.
		return this->_nextDispatcher->dispatch( header, engine );
	}
	catch( std::exception &ex )
	{
		// Exception where we can return a message to the calling side.
		std::string message( "An exception occurred while processing the request. Message was: " );
		message.append( ex.what() );
		
		if( header.getType() == SPEPIPC_REQUEST )
		{
			// Send the error response.
			engine.sendErrorResponseHeader();
			InvocationTargetException exception( message );
			engine.sendObject(exception);
		}
		else
		{
			// Not expecting a response. We can still log it.
			this->_localReportingProcessor.log( spep::ERROR, "Exception was thrown while dispatching a request, but client does not expect a reply. Error was: " + message );
		}
		
		return true;
	}
	catch( ... )
	{
		// No exception information, but still we must report an error.
		std::string message( "An unknown exception occurred while processing the request." );
		
		if( header.getType() == SPEPIPC_REQUEST )
		{
			// Send the error response.
			engine.sendErrorResponseHeader();
			InvocationTargetException exception( message );
			engine.sendObject(exception);
		}
		else
		{
			// Not expecting a response. We can still log it.
			this->_localReportingProcessor.log( spep::ERROR, "Exception was thrown while dispatching a request, but client does not expect a reply. Error was: " + message );
		}
		
		return true;
	}
}
