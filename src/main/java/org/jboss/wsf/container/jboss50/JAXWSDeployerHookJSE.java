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
package org.jboss.wsf.container.jboss50;

//$Id$

import org.jboss.deployers.structure.spi.DeploymentUnit;
import org.jboss.deployers.vfs.spi.structure.VFSDeploymentUnit;
import org.jboss.metadata.WebMetaData;
import org.jboss.metadata.web.Servlet;
import org.jboss.wsf.spi.deployment.Deployment;
import org.jboss.wsf.spi.deployment.Deployment.DeploymentType;
import org.jboss.wsf.spi.deployment.ArchiveDeployment;
import org.jboss.wsf.spi.deployment.Endpoint;
import org.jboss.wsf.spi.deployment.Service;

import javax.jws.WebService;
import javax.xml.ws.WebServiceProvider;
import java.util.ArrayList;
import java.util.List;

/**
 * A deployer JAXWS JSE Endpoints
 *
 * @author Thomas.Diesler@jboss.org
 * @since 25-Apr-2007
 */
public class JAXWSDeployerHookJSE extends AbstractDeployerHookJSE
{
   /** Get the deployemnt type this deployer can handle 
    */
   public DeploymentType getDeploymentType()
   {
      return DeploymentType.JAXWS_JSE;
   }

   @Override
   public Deployment createDeployment(DeploymentUnit unit)
   {
      ArchiveDeployment dep = newDeployment(unit);
      dep.setRootFile(new VirtualFileAdaptor(((VFSDeploymentUnit)unit).getRoot()));
      dep.setRuntimeClassLoader(null);
      dep.setType(getDeploymentType());

      Service service = dep.getService();

      WebMetaData webMetaData = unit.getAttachment(WebMetaData.class);
      if (webMetaData == null)
         throw new IllegalStateException("Deployment unit does not contain web meta data");

      // Copy the attachments
      dep.addAttachment(WebMetaData.class, webMetaData);

      List<Servlet> servlets = getRelevantServlets(webMetaData, unit.getClassLoader());
      for (Servlet servlet : servlets)
      {
         String servletName = servlet.getName();
         String servletClass = servlet.getServletClass();

         // Create the endpoint
         Endpoint ep = newEndpoint(servletClass);
         ep.setShortName(servletName);
         service.addEndpoint(ep);
      }

      return dep;
   }

   @Override
   public boolean isWebServiceDeployment(DeploymentUnit unit)
   {
      if (super.isWebServiceDeployment(unit) == false)
         return false;

      boolean isWebServiceDeployment = false;
      try
      {
         WebMetaData webMetaData = unit.getAttachment(WebMetaData.class);
         List<Servlet> servlets = getRelevantServlets(webMetaData, unit.getClassLoader());
         isWebServiceDeployment = servlets.size() > 0;
      }
      catch (Exception ex)
      {
         log.error("Cannot process web deployment", ex);
      }

      return isWebServiceDeployment;
   }

   private List<Servlet> getRelevantServlets(WebMetaData webMetaData, ClassLoader loader)
   {
      List<Servlet> servlets = new ArrayList<Servlet>();
      for (Servlet servlet : webMetaData.getServlets())
      {
         String servletClassName = servlet.getServletClass();

         // Skip JSPs
         if (servletClassName == null || servletClassName.length() == 0)
            continue;

         try
         {
            Class<?> servletClass = loader.loadClass(servletClassName.trim());
            boolean isWebService = servletClass.isAnnotationPresent(WebService.class);
            boolean isWebServiceProvider = servletClass.isAnnotationPresent(WebServiceProvider.class);
            if (isWebService || isWebServiceProvider)
               servlets.add(servlet);
         }
         catch (ClassNotFoundException ex)
         {
            log.warn("Cannot load servlet class: " + servletClassName);
            continue;
         }
      }
      return servlets;
   }
}