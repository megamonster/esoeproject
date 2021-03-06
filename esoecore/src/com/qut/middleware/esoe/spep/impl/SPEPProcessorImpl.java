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
 * Author: Shaun Mangelsdorf, Bradley Beddoes
 * Creation Date: 13/11/2006, 02/02/2007
 * 
 * Purpose: Implements the SPEPProcessor interface.
 */
package com.qut.middleware.esoe.spep.impl;

import java.util.Date;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3._2000._09.xmldsig_.Signature;
import org.w3c.dom.Element;

import com.qut.middleware.crypto.KeystoreResolver;
import com.qut.middleware.esoe.authz.cache.AuthzCacheUpdateFailureRepository;
import com.qut.middleware.esoe.authz.cache.bean.FailedAuthzCacheUpdate;
import com.qut.middleware.esoe.authz.cache.bean.impl.FailedAuthzCacheUpdateImpl;
import com.qut.middleware.esoe.sessions.Principal;
import com.qut.middleware.esoe.spep.Messages;
import com.qut.middleware.esoe.spep.SPEPProcessor;
import com.qut.middleware.esoe.spep.Startup;
import com.qut.middleware.esoe.util.CalendarUtils;
import com.qut.middleware.esoe.ws.WSClient;
import com.qut.middleware.esoe.ws.exception.WSClientException;
import com.qut.middleware.metadata.bean.saml.SPEPRole;
import com.qut.middleware.metadata.bean.saml.endpoint.IndexedEndpoint;
import com.qut.middleware.metadata.exception.MetadataStateException;
import com.qut.middleware.metadata.processor.MetadataProcessor;
import com.qut.middleware.saml2.SchemaConstants;
import com.qut.middleware.saml2.StatusCodeConstants;
import com.qut.middleware.saml2.VersionConstants;
import com.qut.middleware.saml2.exception.InvalidSAMLResponseException;
import com.qut.middleware.saml2.exception.MarshallerException;
import com.qut.middleware.saml2.exception.ReferenceValueException;
import com.qut.middleware.saml2.exception.SignatureValueException;
import com.qut.middleware.saml2.exception.UnmarshallerException;
import com.qut.middleware.saml2.handler.Marshaller;
import com.qut.middleware.saml2.handler.Unmarshaller;
import com.qut.middleware.saml2.handler.impl.MarshallerImpl;
import com.qut.middleware.saml2.handler.impl.UnmarshallerImpl;
import com.qut.middleware.saml2.identifier.IdentifierGenerator;
import com.qut.middleware.saml2.schemas.assertion.NameIDType;
import com.qut.middleware.saml2.schemas.assertion.Subject;
import com.qut.middleware.saml2.schemas.esoe.lxacml.grouptarget.GroupTarget;
import com.qut.middleware.saml2.schemas.esoe.protocol.ClearAuthzCacheRequest;
import com.qut.middleware.saml2.schemas.esoe.protocol.ClearAuthzCacheResponse;
import com.qut.middleware.saml2.validator.SAMLValidator;

/** Implements the SPEPProcessor interface. */

public class SPEPProcessorImpl implements SPEPProcessor
{
	private MetadataProcessor metadata;
	private Startup startup;
	private AuthzCacheUpdateFailureRepository failureRep;
	private IdentifierGenerator identifierGenerator;
	private WSClient wsClient;
	private SAMLValidator samlValidator;
	private String esoeIdentifier;

	private Marshaller<ClearAuthzCacheRequest> clearAuthzCacheRequestMarshaller;
	private Unmarshaller<ClearAuthzCacheResponse> clearAuthzCacheResponseUnmarshaller;

	private final String UNMAR_PKGNAMES = ClearAuthzCacheResponse.class.getPackage().getName();
	private final String MAR_PKGNAMES = ClearAuthzCacheRequest.class.getPackage().getName()
			+ ":" + GroupTarget.class.getPackage().getName(); //$NON-NLS-1$
	private final String[] schemas = new String[] { SchemaConstants.esoeProtocol,
			SchemaConstants.samlProtocol };

	private final String PRINCIPAL_CLEAR_CACHE_REASON = Messages.getString("SPEPProcessorImpl.3"); //$NON-NLS-1$

	/* Local logging instance */
	private Logger logger = LoggerFactory.getLogger(SPEPProcessorImpl.class.getName());

	/**
	 * Constructor
	 * 
	 * @param metadata
	 *            Metadata instance
	 * @param startup
	 *            Startup instance.
	 */
	public SPEPProcessorImpl(MetadataProcessor metadata, Startup startup, AuthzCacheUpdateFailureRepository failureRep,
			WSClient wsClient, IdentifierGenerator identifierGenerator, SAMLValidator samlValidator,
			KeystoreResolver keyStoreResolver, String esoeIdentifier) throws MarshallerException, UnmarshallerException
	{
		if (metadata == null)
		{
			throw new IllegalArgumentException(Messages.getString("SPEPProcessorImpl.1")); //$NON-NLS-1$
		}
		if (startup == null)
		{
			throw new IllegalArgumentException(Messages.getString("SPEPProcessorImpl.2")); //$NON-NLS-1$
		}
		if (failureRep == null)
		{
			throw new IllegalArgumentException(Messages.getString("SPEPProcessorImpl.4")); //$NON-NLS-1$
		}
		if (wsClient == null)
		{
			throw new IllegalArgumentException(Messages.getString("SPEPProcessorImpl.5")); //$NON-NLS-1$
		}
		if (identifierGenerator == null)
		{
			throw new IllegalArgumentException(Messages.getString("SPEPProcessorImpl.6")); //$NON-NLS-1$
		}
		if (samlValidator == null)
		{
			throw new IllegalArgumentException(Messages.getString("SPEPProcessorImpl.7")); //$NON-NLS-1$
		}
		if (keyStoreResolver == null)
		{
			throw new IllegalArgumentException(Messages.getString("SPEPProcessorImpl.8")); //$NON-NLS-1$
		}
		if (esoeIdentifier == null)
		{
			throw new IllegalArgumentException("ESOE identifier cannot be null");
		}

		this.metadata = metadata;
		this.startup = startup;
		this.failureRep = failureRep;
		this.wsClient = wsClient;
		this.identifierGenerator = identifierGenerator;
		this.samlValidator = samlValidator;
		this.esoeIdentifier = esoeIdentifier;

		this.clearAuthzCacheRequestMarshaller = new MarshallerImpl<ClearAuthzCacheRequest>(this.MAR_PKGNAMES,
				this.schemas, keyStoreResolver);
		this.clearAuthzCacheResponseUnmarshaller = new UnmarshallerImpl<ClearAuthzCacheResponse>(this.UNMAR_PKGNAMES,
				this.schemas, this.metadata);

		this.logger.info(Messages.getString("SPEPProcessorImpl.0")); //$NON-NLS-1$
	}

	public void clearPrincipalSPEPCaches(Principal principal)
	{
		List<String> activeDescriptors = principal.getActiveEntityList();
		Element authzClearCacheRequest = null;
		boolean updateResult = false;

		if (activeDescriptors != null && activeDescriptors.size() > 0)
		{
			for (String entityID : activeDescriptors)
			{
				try
				{
					SPEPRole spepRole = this.metadata.getEntityRoleData(entityID, SPEPRole.class);
					if (spepRole == null)
					{
						this.logger.error("Entity did not exist, or did not have the SPEP role required for cache clear. Entity ID: " + entityID);
						continue;
					}

					List<IndexedEndpoint> endpoints = spepRole.getCacheClearServiceEndpointList();

					if (endpoints != null)
					{
						for (IndexedEndpoint endpoint : endpoints)
						{
							String endpointLocation = endpoint.getLocation();
							
							authzClearCacheRequest = generateClearCacheRequest(principal.getSAMLAuthnIdentifier(),
									endpointLocation, this.PRINCIPAL_CLEAR_CACHE_REASON);

							if (authzClearCacheRequest == null)
								this.logger.warn(Messages.getString("SPEPProcessorImpl.9") //$NON-NLS-1$
										+ principal + Messages.getString("SPEPProcessorImpl.10") + entityID); //$NON-NLS-1$
							else
								updateResult = this.sendCacheUpdateRequest(authzClearCacheRequest, endpointLocation);

							if (!updateResult)
							{
								this.recordFailure(authzClearCacheRequest, endpointLocation);
							}
						}
					}
				}
				catch (MarshallerException e)
				{
					this.logger.error(Messages.getString("SPEPProcessorImpl.12") + entityID); //$NON-NLS-1$
					this.logger.debug(e.getLocalizedMessage(), e);
				}
				catch (MetadataStateException e)
				{
					this.logger.error("Metadata was in an invalid state. Error was: " + e.getMessage());
					this.logger.debug(e.getLocalizedMessage(), e);
				}
			}
		}
	}

	/*
	 * Creates and marshalls the authz clear cache request for individual principal sessions on SPEPs the principal has
	 * visited
	 * 
	 * 
	 */
	private Element generateClearCacheRequest(String samlAuthnIdentifier, String endpoint, String reason)
			throws MarshallerException
	{
		Element requestDocument = null;
		ClearAuthzCacheRequest request = new ClearAuthzCacheRequest();
		Subject subject = new Subject();
		NameIDType subjectID = new NameIDType();

		request.setID(this.identifierGenerator.generateSAMLID());
		request.setReason(reason);
		request.setVersion(VersionConstants.saml20);

		// set the desintation endpoint ID
		request.setDestination(endpoint);

		NameIDType issuer = new NameIDType();
		issuer.setValue(this.esoeIdentifier);
		request.setIssuer(issuer);

		subjectID.setValue(samlAuthnIdentifier);
		subject.setNameID(subjectID);
		request.setSubject(subject);

		// Timestamps MUST be set to UTC, no offset
		request.setIssueInstant(CalendarUtils.generateXMLCalendar());

		request.setSignature(new Signature());

		// marshall the clear cache request
		requestDocument = this.clearAuthzCacheRequestMarshaller.marshallSignedElement(request);

		return requestDocument;
	}

	/*
	 * Send an AuthzCacheUpdate request to the specified SPEP endpoint.
	 * 
	 * @param authzClearCacheRequest The xml authz cache request. @param endPoint The endpoint to send to.
	 * 
	 * @return The result of the operation. Either Success or Failure.
	 */
	private boolean sendCacheUpdateRequest(Element authzClearCacheRequest, String endPoint)
	{
		Element responseDocument;
		ClearAuthzCacheResponse clearAuthzCacheResponse = null;

		try
		{
			responseDocument = this.wsClient.authzCacheClear(authzClearCacheRequest, endPoint);
			clearAuthzCacheResponse = this.clearAuthzCacheResponseUnmarshaller.unMarshallSigned(responseDocument);

			// validate the response
			this.samlValidator.getResponseValidator().validate(clearAuthzCacheResponse);

			// process the Authz cache clear response
			if (clearAuthzCacheResponse != null && clearAuthzCacheResponse.getStatus() != null)
			{
				if (clearAuthzCacheResponse.getStatus().getStatusCode() != null)
				{
					if (StatusCodeConstants.success.equals(clearAuthzCacheResponse.getStatus().getStatusCode()
							.getValue()))
					{
						this.logger.debug(Messages.getString("SPEPProcessorImpl.13")); //$NON-NLS-1$
						return true;
					}
				}
				else
					this.logger.debug(Messages.getString("SPEPProcessorImpl.14")); //$NON-NLS-1$
			}
			else
			{
				this.logger.debug(Messages.getString("SPEPProcessorImpl.15")); //$NON-NLS-1$
			}

			return false;
		}
		catch (UnmarshallerException e)
		{
			this.logger.error(Messages.getString("SPEPProcessorImpl.16")); //$NON-NLS-1$
			this.logger.debug(e.getLocalizedMessage(), e);

			return false;
		}
		catch (SignatureValueException e)
		{
			this.logger.error(Messages.getString("SPEPProcessorImpl.17")); //$NON-NLS-1$
			this.logger.debug(e.getLocalizedMessage(), e);

			return false;
		}
		catch (ReferenceValueException e)
		{
			this.logger.error(Messages.getString("SPEPProcessorImpl.18")); //$NON-NLS-1$
			this.logger.debug(e.getLocalizedMessage(), e);

			return false;
		}
		catch (InvalidSAMLResponseException e)
		{
			this.logger.warn(Messages.getString("SPEPProcessorImpl.19")); //$NON-NLS-1$
			this.logger.trace(e.getLocalizedMessage(), e);

			return false;
		}
		catch (WSClientException e)
		{
			this.logger
					.error("WSClientException while attempting to process response from SPEP to clear cache for individual principal session " + endPoint); //$NON-NLS-1$
			this.logger.debug(e.getLocalizedMessage(), e);

			return false;
		}
	}

	/*
	 * Add the failure record to the cache update failure repository.
	 * 
	 * @param request The request that failed to deliver. @param endPoint The end point the the request failed to
	 * deliver to.
	 */
	private synchronized void recordFailure(Element request, String endPoint)
	{
		// create an UpdateFailure object
		FailedAuthzCacheUpdate failure = new FailedAuthzCacheUpdateImpl();

		failure.setEndPoint(endPoint);
		failure.setRequestDocument(request);
		failure.setTimeStamp(new Date(System.currentTimeMillis()));

		this.logger.info(Messages.getString("SPEPProcessorImpl.20") + endPoint); //$NON-NLS-1$

		// add it to failure repository
		this.failureRep.add(failure);

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.qut.middleware.esoe.spep.SPEPProcessor#getMetadata()
	 */
	//public MetadataProcessor getMetadata()
	//{
	//	return this.metadata;
	//}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.qut.middleware.esoe.spep.SPEPProcessor#getStartup()
	 */
	public Startup getStartup()
	{
		return this.startup;
	}
}
