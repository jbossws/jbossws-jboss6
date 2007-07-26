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
import org.jboss.metadata.ApplicationMetaData;
import org.jboss.metadata.BeanMetaData;
import org.jboss.wsf.spi.deployment.Deployment;
import org.jboss.wsf.spi.deployment.Deployment.DeploymentType;
import org.jboss.wsf.spi.deployment.ArchiveDeployment;
import org.jboss.wsf.spi.deployment.Endpoint;
import org.jboss.wsf.spi.deployment.Service;
import org.jboss.wsf.spi.metadata.webservices.PortComponentMetaData;
import org.jboss.wsf.spi.metadata.webservices.WebserviceDescriptionMetaData;
import org.jboss.wsf.spi.metadata.webservices.WebservicesMetaData;

/**
 * A deployer JAXRPC EJB21 Endpoints
 *
 * @author Thomas.Diesler@jboss.org
 * @since 25-Apr-2007
 */
public class JAXRPCDeployerHookEJB21 extends AbstractDeployerHookEJB
{
   /** Get the deployemnt type this deployer can handle 
    */
   public DeploymentType getDeploymentType()
   {
      return DeploymentType.JAXRPC_EJB21;
   }

   @Override
   public Deployment createDeployment(DeploymentUnit unit)
   {
      ArchiveDeployment dep = newDeployment(unit);
      dep.setRootFile(new VirtualFileAdaptor(((VFSDeploymentUnit)unit).getRoot()));
      dep.setRuntimeClassLoader(unit.getClassLoader());
      dep.setType(getDeploymentType());

      Service service = dep.getService();

      ApplicationMetaData appmd = unit.getAttachment(ApplicationMetaData.class);
      if (appmd == null)
         throw new IllegalStateException("Deployment unit does not contain application meta data");

      WebservicesMetaData wsMetaData = getWebservicesMetaData(unit);
      if (wsMetaData == null)
         throw new IllegalStateException("Deployment unit does not contain webservices meta data");

      // Copy the attachments
      dep.getContext().addAttachment(WebservicesMetaData.class, wsMetaData);
      dep.getContext().addAttachment(ApplicationMetaData.class, appmd);

      for (WebserviceDescriptionMetaData wsd : wsMetaData.getWebserviceDescriptions())
      {
         for (PortComponentMetaData pcmd : wsd.getPortComponents())
         {
            String ejbLink = pcmd.getEjbLink();
            if (ejbLink == null)
               throw new IllegalStateException("ejb-link cannot be null");

            BeanMetaData beanMetaData = appmd.getBeanByEjbName(ejbLink);
            if (beanMetaData == null)
               throw new IllegalStateException("Cannot obtain bean meta data for: " + ejbLink);

            String ejbClass = beanMetaData.getEjbClass();

            // Create the endpoint
            Endpoint ep = newEndpoint();
            ep.setShortName(ejbLink);
            ep.setService(service);
            ep.setTargetBeanName(ejbClass);

            service.addEndpoint(ep);
         }
      }
      return dep;
   }

   @Override
   public boolean isWebServiceDeployment(DeploymentUnit unit)
   {
      WebservicesMetaData wsMetaData = getWebservicesMetaData(unit);
      return wsMetaData != null && unit.getAllMetaData(ApplicationMetaData.class).size() > 0;
   }
}