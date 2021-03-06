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
 * Author: Andre Zitelli
 * Creation Date: 11/09/2006
 * 
 * Purpose: A monitor thread which attempts to resend failed cache update attempts at regular intervals.
 * 
 */
package com.qut.middleware.esoe.authz.cache.impl;

import java.text.MessageFormat;
import java.util.Iterator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Element;

import com.qut.middleware.crypto.KeystoreResolver;
import com.qut.middleware.esoe.MonitorThread;
import com.qut.middleware.esoe.authz.cache.AuthzCacheUpdateFailureRepository;
import com.qut.middleware.esoe.authz.cache.PolicyCacheProcessor;
import com.qut.middleware.esoe.authz.cache.PolicyCacheProcessor.result;
import com.qut.middleware.esoe.authz.cache.bean.FailedAuthzCacheUpdate;
import com.qut.middleware.esoe.util.CalendarUtils;
import com.qut.middleware.esoe.ws.WSClient;
import com.qut.middleware.esoe.ws.exception.WSClientException;
import com.qut.middleware.metadata.processor.MetadataProcessor;
import com.qut.middleware.saml2.SchemaConstants;
import com.qut.middleware.saml2.StatusCodeConstants;
import com.qut.middleware.saml2.exception.MarshallerException;
import com.qut.middleware.saml2.exception.ReferenceValueException;
import com.qut.middleware.saml2.exception.SignatureValueException;
import com.qut.middleware.saml2.exception.UnmarshallerException;
import com.qut.middleware.saml2.handler.Marshaller;
import com.qut.middleware.saml2.handler.Unmarshaller;
import com.qut.middleware.saml2.handler.impl.MarshallerImpl;
import com.qut.middleware.saml2.handler.impl.UnmarshallerImpl;
import com.qut.middleware.saml2.identifier.IdentifierGenerator;
import com.qut.middleware.saml2.schemas.esoe.lxacml.grouptarget.GroupTarget;
import com.qut.middleware.saml2.schemas.esoe.protocol.ClearAuthzCacheRequest;
import com.qut.middleware.saml2.schemas.esoe.protocol.ClearAuthzCacheResponse;

public class CacheUpdateFailureMonitor extends Thread implements MonitorThread
{

	private AuthzCacheUpdateFailureRepository updateFailures;
	private IdentifierGenerator identifierGenerator;
	
	private volatile boolean running;
	
	private int retryInterval;
	private int maxFailureAge;
	private final WSClient wsClient;
	private MetadataProcessor metadata;
		
	private Unmarshaller<ClearAuthzCacheResponse> clearAuthzCacheResponseUnmarshaller;
	private Marshaller<ClearAuthzCacheRequest> clearAuthzCacheRequestMarshaller;
	private Unmarshaller<ClearAuthzCacheRequest> clearAuthzCacheRequestUnmarshaller;
	
	private final String MAR_PKGNAMES = ClearAuthzCacheRequest.class.getPackage().getName() + ":" + GroupTarget.class.getPackage().getName(); //$NON-NLS-1$
	private final String UNMAR_PKGNAMES2 = ClearAuthzCacheRequest.class.getPackage().getName() + ":" + GroupTarget.class.getPackage().getName(); //$NON-NLS-1$
	private final String UNMAR_PKGNAMES = ClearAuthzCacheResponse.class.getPackage().getName();

	/* Local logging instance */
	private Logger logger = LoggerFactory.getLogger(CacheUpdateFailureMonitor.class.getName());

	/**
	 * @param failureRep
	 *            The cache update failure repository instance
	 * @param metadata
	 *            The metadata object to be used when resolving SPEP information
	 * @param wsClient
	 *            The ESOE web service object
	 * @param retryInterval
	 *            The retry interval in seconds.
	 * @param maxFailureAge
	 *            The time in seconds that a cache update failure will be held in the repository, in minutes.
	 * @throws UnmarshallerException
	 */
	public CacheUpdateFailureMonitor(AuthzCacheUpdateFailureRepository failureRep, MetadataProcessor metadata,
			WSClient wsClient, KeystoreResolver keyStoreResolver, IdentifierGenerator identifierGenerator, int retryInterval, int maxFailureAge) throws UnmarshallerException, MarshallerException
	{
		this.wsClient = wsClient;

		if (failureRep == null)
			throw new IllegalArgumentException(Messages.getString("CacheUpdateFailureMonitorImpl.1")); //$NON-NLS-1$

		if (metadata == null)
			throw new IllegalArgumentException(Messages.getString("CacheUpdateFailureMonitorImpl.2")); //$NON-NLS-1$

		if (wsClient == null)
			throw new IllegalArgumentException(Messages.getString("CacheUpdateFailureMonitorImpl.3")); //$NON-NLS-1$

		if (keyStoreResolver == null)
			throw new IllegalArgumentException(Messages.getString("CacheUpdateFailureMonitor.0")); //$NON-NLS-1$

		if (identifierGenerator == null)
			throw new IllegalArgumentException(Messages.getString("CacheUpdateFailureMonitor.1")); //$NON-NLS-1$
					
		if (retryInterval <= 0 || (retryInterval) > Integer.MAX_VALUE / 1000)
			throw new IllegalArgumentException(Messages.getString("CacheUpdateFailureMonitorImpl.4")); //$NON-NLS-1$

		if (maxFailureAge < 0 || (maxFailureAge > Integer.MAX_VALUE / 1000))
			throw new IllegalArgumentException(Messages.getString(Messages
					.getString("CacheUpdateFailureMonitorImpl.5"))); //$NON-NLS-1$

		this.metadata = metadata;
		this.updateFailures = failureRep;
		this.retryInterval = retryInterval * 1000;
		this.maxFailureAge = maxFailureAge * 1000;
		this.identifierGenerator = identifierGenerator;
		
		String[] schemas = new String[] { SchemaConstants.esoeProtocol, SchemaConstants.samlProtocol };
		
		this.clearAuthzCacheResponseUnmarshaller = new UnmarshallerImpl<ClearAuthzCacheResponse>(this.UNMAR_PKGNAMES, schemas, this.metadata);
		this.clearAuthzCacheRequestMarshaller = new MarshallerImpl<ClearAuthzCacheRequest>(this.MAR_PKGNAMES, schemas, keyStoreResolver);
		this.clearAuthzCacheRequestUnmarshaller = new UnmarshallerImpl<ClearAuthzCacheRequest>(this.UNMAR_PKGNAMES2, schemas, this.metadata);
		
		this.setName("CacheUpdate failure Monitor");  //$NON-NLS-1$

		this.logger.info(MessageFormat.format(Messages.getString("CacheUpdateFailureMonitorImpl.16"), maxFailureAge, retryInterval) );  //$NON-NLS-1$

		// start the thread (calls the run method)
		this.start();
	}

	
	/*
	 * @see java.lang.Runnable#run()
	 */
	@Override
	public void run()
	{
		this.setRunning(true);
		
		while (this.isRunning())
		{
			// retry any failed updates forever
			try
			{
				sleep(this.retryInterval);
				
				this.flushRepository();
			}		
			catch(InterruptedException e)
			{
				if(!this.isRunning())
					break;
			}
			// ignore other non-runtime exceptions
			catch (Exception e)
			{
				this.logger.trace(e.getLocalizedMessage(), e);
			}
		}
		
		this.logger.info(this.getName() + Messages.getString("CacheUpdateFailureMonitorImpl.17")); //$NON-NLS-1$
					
		return;
	}

	/*
	 * Send an AuthzCacheUpdate request to the specified SPEP endpoint.
	 * 
	 * @param authzClearCacheRequest The string representation of the clear cache request.
	 * @param endPoint The endpoint to send the reqyest to. 
	 * 
	 * @return the result of the attempted send.
	 */
	private result sendCacheUpdate(Element authzClearCacheRequest, String endPoint)
	{
		Element responseDocument;

		try
		{
			this.logger.debug(MessageFormat.format(Messages.getString("CacheUpdateFailureMonitor.2"), endPoint));  //$NON-NLS-1$
			
			responseDocument = this.wsClient.authzCacheClear(authzClearCacheRequest, endPoint);
			
			ClearAuthzCacheResponse clearAuthzCacheResponse = null;
			
			clearAuthzCacheResponse = this.clearAuthzCacheResponseUnmarshaller.unMarshallSigned(responseDocument);

			// process the Authz cache clear Response
			if (clearAuthzCacheResponse != null && clearAuthzCacheResponse.getStatus() != null)
			{
				if(clearAuthzCacheResponse.getStatus().getStatusCode() != null)
				{
					if(StatusCodeConstants.success.equals(clearAuthzCacheResponse.getStatus().getStatusCode().getValue()))
					{
						this.logger.debug(Messages.getString("CacheUpdateFailureMonitor.3")); //$NON-NLS-1$
						return result.Success;
					}
					else
						this.logger.error(MessageFormat.format(Messages.getString("CacheUpdateFailureMonitor.4"), clearAuthzCacheResponse.getStatus().getStatusMessage())); //$NON-NLS-1$
				}
				else
					this.logger.debug(Messages.getString("CacheUpdateFailureMonitor.5")); //$NON-NLS-1$
			}
			else
			{
				this.logger.debug(Messages.getString("CacheUpdateFailureMonitor.6")); //$NON-NLS-1$
			}
			
			return result.Failure;
		}
		catch (SignatureValueException e)
		{
			this.logger.error(Messages.getString("CacheUpdateFailureMonitorImpl.7")); //$NON-NLS-1$
			this.logger.trace(e.getLocalizedMessage(), e);

			return result.Failure;
		}
		catch (ReferenceValueException e)
		{
			this.logger.error(Messages.getString("CacheUpdateFailureMonitorImpl.8")); //$NON-NLS-1$
			this.logger.trace( e.getLocalizedMessage(), e);

			return result.Failure;
		}
		catch (UnmarshallerException e)
		{
			this.logger.error(Messages.getString("CacheUpdateFailureMonitorImpl.9")); //$NON-NLS-1$
			this.logger.trace( e.getLocalizedMessage(), e);

			return result.Failure;
		}
		catch (WSClientException e)
		{
			this.logger.error(Messages.getString("CacheUpdateFailureMonitorImpl.10") + endPoint); //$NON-NLS-1$
			this.logger.trace( e.getLocalizedMessage(), e);

			return result.Failure;
		}
	}

	/*
	 * Iterate through the list of failure objects in the repository and attempt to resend the SPEP notification. If a
	 * request cannot be sent, it remains in the repository, else it is removed. If a failure cannot be sent and it is
	 * older than this.maxFailureAge, it is also removed.
	 * 
	 */
	private void flushRepository()
	{
		PolicyCacheProcessor.result result = PolicyCacheProcessor.result.Failure;

		Iterator<FailedAuthzCacheUpdate> iter = this.updateFailures.getFailures().iterator();

		this.logger.debug(Messages.getString("CacheUpdateFailureMonitorImpl.14") + this.updateFailures.getSize() + Messages.getString("CacheUpdateFailureMonitorImpl.15")); //$NON-NLS-1$ //$NON-NLS-2$
		
		// while failures in repository, send update request to associated SPEP and delete
		while (iter.hasNext())
		{
				FailedAuthzCacheUpdate failure = iter.next();
		
				// we have to regenerate the original request with updated timestamps and sigs before sending
				// because the allowed time skew will more than likely have expired.
				
				Element newDocument = this.regenerateDocument(failure.getRequestDocument());
				
				if(newDocument != null)
				{
					failure.setRequestDocument(newDocument);
				
					result = this.sendCacheUpdate(failure.getRequestDocument(), failure.getEndPoint());
				}
				else
				{
					// skip the send because it won't succeed
					this.logger.error(Messages.getString("CacheUpdateFailureMonitor.7")); //$NON-NLS-1$				
				}
								
				if (result == PolicyCacheProcessor.result.Success)
				{
					this.logger.info(MessageFormat.format(Messages.getString("CacheUpdateFailureMonitorImpl.11"), failure.getEndPoint()) ); //$NON-NLS-1$
					this.updateFailures.remove(failure);	
				}
				else
				// see if it's time to expire the record
				{
					this.logger.info(Messages.getString("CacheUpdateFailureMonitorImpl.18") + failure.getEndPoint() +Messages.getString("CacheUpdateFailureMonitorImpl.19")); //$NON-NLS-1$ //$NON-NLS-2$
							
					// calculate age of failure in minutes
					int age = (int)(System.currentTimeMillis() - failure.getTimeStamp().getTime())  ;
					
					this.logger.debug(MessageFormat.format(Messages.getString("CacheUpdateFailureMonitorImpl.22"),age ,this.maxFailureAge) ); //$NON-NLS-1$ 
					
					if (age > this.maxFailureAge)
					{
						this.logger.info(MessageFormat.format(Messages.getString("CacheUpdateFailureMonitorImpl.12"), failure.getEndPoint()) ); //$NON-NLS-1$
						this.updateFailures.remove(failure);	
					}
				}
			}		
		}

	
	/* Modifies the given string representation of the ClearAuthzCacheRequest with updated timestamps
	 * SAML ID's and signatures. If an error occurs during the process, null is returned.
	 * 
	 */
	private Element regenerateDocument(Element oldDocument)
	{
		this.logger.debug( Messages.getString("CacheUpdateFailureMonitor.8")); //$NON-NLS-1$
		
		Element newDoc = null;
		ClearAuthzCacheRequest request = null;
		
		// unmarshall the original failure request for modification
		try
		{
			request = this.clearAuthzCacheRequestUnmarshaller.unMarshallSigned(oldDocument);
		}
		catch(UnmarshallerException e)
		{
			this.logger.error(Messages.getString("CacheUpdateFailureMonitor.9"));  //$NON-NLS-1$
			this.logger.trace(e.getLocalizedMessage(), e);
		}
		catch(ReferenceValueException e)
		{
			this.logger.error(Messages.getString("CacheUpdateFailureMonitor.10")); //$NON-NLS-1$
			this.logger.trace(e.getLocalizedMessage(), e);
		}
		catch(SignatureValueException e)
		{
			this.logger.error(Messages.getString("CacheUpdateFailureMonitor.11")); //$NON-NLS-1$
			this.logger.trace(e.getLocalizedMessage(), e);
		}
		
		if(request != null)
		{
			try
			{				
				// reset timestamp
				request.setIssueInstant(CalendarUtils.generateXMLCalendar());
				
				// reset request ID
				request.setID(this.identifierGenerator.generateSAMLID());
				
				// re marshall mofified document
				newDoc = this.clearAuthzCacheRequestMarshaller.marshallSignedElement(request);		
			}
			catch(MarshallerException e)
			{
				this.logger.error(Messages.getString("CacheUpdateFailureMonitor.12")); //$NON-NLS-1$
				this.logger.trace(e.getLocalizedMessage(), e);
			}
		}
		
		return newDoc;
	}
	
	/*
	 * (non-Javadoc)
	 * @see com.qut.middleware.esoe.MonitorThread#shutdown()
	 */
	public void shutdown()
	{
		this.setRunning(false);
		
		this.interrupt();
	}
	
	protected synchronized boolean isRunning()
	{
		return this.running;
	}
	
	protected synchronized void setRunning(boolean running)
	{
		this.running = running;
	}
}
