/*
 * JBoss, Home of Professional Open Source
 * Copyright 2005, JBoss Inc., and individual contributors as indicated
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.jboss.ws.integration.jboss50;

//$Id$

import org.jboss.deployers.spi.deployer.DeploymentUnit;
import org.jboss.metadata.WebMetaData;
import org.jboss.metadata.web.Servlet;
import org.jboss.ws.integration.BasicEndpoint;
import org.jboss.ws.integration.Endpoint;
import org.jboss.ws.integration.Service;
import org.jboss.ws.integration.deployment.BasicDeployment;
import org.jboss.ws.integration.deployment.Deployment;
import org.jboss.ws.integration.deployment.Deployment.DeploymentType;
import org.jboss.ws.metadata.webservices.PortComponentMetaData;
import org.jboss.ws.metadata.webservices.WebserviceDescriptionMetaData;
import org.jboss.ws.metadata.webservices.WebservicesMetaData;
import org.jboss.ws.utils.ObjectNameFactory;

/**
 * A deployer JAXRPC JSE Endpoints
 *
 * @author Thomas.Diesler@jboss.org
 * @since 25-Apr-2007
 */
public class JAXRPCDeployerHookJSE extends AbstractDeployerHookJSE
{
   /** Get the deployemnt type this deployer can handle 
    */
   public DeploymentType getDeploymentType()
   {
      return DeploymentType.JAXRPC_JSE;
   }

   /**
    * Create an endpoint for every servlet-link in webservices.xml
    */
   @Override
   public Deployment createDeployment(DeploymentUnit unit)
   {
      Deployment dep = createDeployment();
      dep.setType(getDeploymentType());
      dep.setClassLoader(unit.getClassLoader());

      Service service = dep.getService();

      WebMetaData webMetaData = unit.getAttachment(WebMetaData.class);
      if (webMetaData == null)
         throw new IllegalStateException("Deployment unit does not contain web meta data");

      WebservicesMetaData wsMetaData = getWebservicesMetaData(unit);
      if (wsMetaData == null)
         throw new IllegalStateException("Deployment unit does not contain webservices meta data");

      // Copy the attachments
      dep.getContext().addAttachment(WebservicesMetaData.class, wsMetaData);
      dep.getContext().addAttachment(WebMetaData.class, webMetaData);

      for (WebserviceDescriptionMetaData wsd : wsMetaData.getWebserviceDescriptions())
      {
         for (PortComponentMetaData pcmd : wsd.getPortComponents())
         {
            String servletLink = pcmd.getServletLink();
            if (servletLink == null)
               throw new IllegalStateException("servlet-link cannot be null");

            Servlet servlet = getServletForName(webMetaData, servletLink);
            String servletClass = servlet.getServletClass();

            try
            {
               ClassLoader loader = unit.getClassLoader();
               Class<?> epBean = loader.loadClass(servletClass.trim());

               // Create the endpoint
               Endpoint ep = createEndpoint();
               ep.setService(service);
               ep.setTargetBean(epBean);
               
               String nameStr = Endpoint.SEPID_DOMAIN + ":" + Endpoint.SEPID_PROPERTY_ENDPOINT + "=" + servletLink;
               ep.setName(ObjectNameFactory.create(nameStr));

               service.addEndpoint(ep);
            }
            catch (ClassNotFoundException ex)
            {
               log.warn("Cannot load servlet class: " + servletClass);
            }
         }
      }

      return dep;
   }

   private Servlet getServletForName(WebMetaData wmd, String servletLink)
   {
      for (Servlet servlet : wmd.getServlets())
      {
         if (servletLink.equals(servlet.getName()))
         {
            return servlet;
         }
      }
      throw new IllegalStateException("Cannot find servlet for link: " + servletLink);
   }

   @Override
   public boolean isWebServiceDeployment(DeploymentUnit unit)
   {
      if (super.isWebServiceDeployment(unit) == false)
         return false;

      WebservicesMetaData wsMetaData = getWebservicesMetaData(unit);
      return wsMetaData != null;
   }
}
