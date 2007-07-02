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
 * Creation Date: 1/5/07
 * 
 * Purpose: Edit Service description logi default implementation
 */
package com.qut.middleware.esoemanager.logic.impl;

import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;

import com.qut.middleware.esoemanager.Constants;
import com.qut.middleware.esoemanager.UtilityFunctions;
import com.qut.middleware.esoemanager.bean.ServiceBean;
import com.qut.middleware.esoemanager.bean.impl.ServiceBeanImpl;
import com.qut.middleware.esoemanager.exception.EditServiceDetailsException;
import com.qut.middleware.esoemanager.exception.SPEPDAOException;
import com.qut.middleware.esoemanager.logic.EditServiceDescriptionLogic;
import com.qut.middleware.esoemanager.spep.sqlmap.SPEPDAO;

public class EditServiceDescriptionLogicImpl implements EditServiceDescriptionLogic
{
	private SPEPDAO spepDAO;
	private UtilityFunctions util;

	/* Local logging instance */
	private Logger logger = Logger.getLogger(EditServiceDescriptionLogicImpl.class.getName());

	public EditServiceDescriptionLogicImpl(SPEPDAO spepDAO)
	{
		if (spepDAO == null)
		{
			this.logger.error("spepDAO for EditServiceDetailsLogicImpl was NULL");
			throw new IllegalArgumentException("spepDAO for EditServiceDetailsLogicImpl was NULL");
		}

		this.spepDAO = spepDAO;
		this.util = new UtilityFunctions();
	}
	
	/* (non-Javadoc)
	 * @see com.qut.middleware.esoemanager.logic.impl.EditServiceDetailsLogic#getServiceDetails(java.lang.String)
	 */
	public ServiceBean getServiceDetails(String entityID) throws EditServiceDetailsException
	{
		ServiceBean serviceDetails = new ServiceBeanImpl();
		List<Map<String, String>> serviceDetailsMap;
		try
		{
			serviceDetailsMap = this.spepDAO.queryServiceDescription(entityID);
		}
		catch (SPEPDAOException e)
		{
			throw new EditServiceDetailsException("Exception when retrieving service details", e);
		}
		
		for (Map<String, String> description : serviceDetailsMap)
		{
			/* There should only be one, if there is multiple results the last one returned will be displayed */
			serviceDetails.setEntityID(description.get(Constants.FIELD_DESCRIPTOR_ID));
			serviceDetails.setServiceName(description.get(Constants.FIELD_SERVICE_NAME));
			serviceDetails.setServiceURL(description.get(Constants.FIELD_SERVICE_URL));
			serviceDetails.setServiceDescription(description.get(Constants.FIELD_SERVICE_DESC));
			serviceDetails.setServiceAuthzFailureMsg(description.get(Constants.FIELD_SERVICE_AUTHZ_FAIL));
		}
		
		return serviceDetails;
	}
	
	/* (non-Javadoc)
	 * @see com.qut.middleware.esoemanager.logic.impl.EditServiceDetailsLogic#updateServiceDetails(java.lang.String, com.qut.middleware.esoemanager.bean.ServiceBean)
	 */
	public void updateServiceDetails(String entityID, ServiceBean serviceDetails) throws EditServiceDetailsException
	{
		try
		{
			this.spepDAO.updateServiceDescription(entityID, serviceDetails.getServiceName(), serviceDetails.getServiceURL(), serviceDetails.getServiceDescription(), serviceDetails.getServiceAuthzFailureMsg());
		}
		catch (SPEPDAOException e)
		{
			this.logger.warn("SPEPDAOException when updating service details " + e.getLocalizedMessage());
			throw new EditServiceDetailsException("SPEPDAOException when updating service details", e);
		}
	}
}