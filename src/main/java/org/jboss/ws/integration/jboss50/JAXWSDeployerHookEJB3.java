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

import java.util.Iterator;

import javax.jws.WebService;
import javax.xml.ws.WebServiceProvider;

import org.jboss.deployers.spi.deployer.DeploymentUnit;
import org.jboss.ejb3.EJBContainer;
import org.jboss.ejb3.Ejb3Deployment;
import org.jboss.ejb3.stateless.StatelessContainer;
import org.jboss.ws.integration.BasicEndpoint;
import org.jboss.ws.integration.Endpoint;
import org.jboss.ws.integration.Service;
import org.jboss.ws.integration.deployment.BasicDeployment;
import org.jboss.ws.integration.deployment.Deployment;
import org.jboss.ws.integration.deployment.Deployment.DeploymentType;
import org.jboss.ws.utils.ObjectNameFactory;

/**
 * A deployer JAXWS EJB3 Endpoints
 *
 * @author Thomas.Diesler@jboss.org
 * @since 25-Apr-2007
 */
public class JAXWSDeployerHookEJB3 extends AbstractDeployerHookEJB
{
   /** Get the deployemnt type this deployer can handle 
    */
   public DeploymentType getDeploymentType()
   {
      return DeploymentType.JAXWS_EJB3;
   }

   @Override
   public Deployment createDeployment(DeploymentUnit unit)
   {
      Deployment dep = createDeployment();
      dep.setType(getDeploymentType());
      dep.setClassLoader(unit.getClassLoader());

      Service service = dep.getService();

      Ejb3Deployment ejb3Deployment = unit.getAttachment(Ejb3Deployment.class);
      if (ejb3Deployment == null)
         throw new IllegalStateException("Deployment unit does not contain ejb3 deployment");

      // Copy the attachments
      dep.getContext().addAttachment(Ejb3Deployment.class, ejb3Deployment);

      Iterator it = ejb3Deployment.getEjbContainers().values().iterator();
      while (it.hasNext())
      {
         EJBContainer container = (EJBContainer)it.next();
         if (isWebServiceBean(container))
         {
            String ejbName = container.getEjbName();
            Class epBean = container.getBeanClass();

            // Create the endpoint
            Endpoint ep = createEndpoint();
            ep.setService(service);
            ep.setEndpointImpl(epBean);
            
            String nameStr = Endpoint.SEPID_DOMAIN + ":" + Endpoint.SEPID_PROPERTY_ENDPOINT + "=" + ejbName;
            ep.setName(ObjectNameFactory.create(nameStr));

            service.addEndpoint(ep);
         }
      }

      return dep;
   }

   @Override
   public boolean isWebServiceDeployment(DeploymentUnit unit)
   {
      Ejb3Deployment ejb3Deployment = unit.getAttachment(Ejb3Deployment.class);
      if (ejb3Deployment == null)
         return false;

      boolean isWebServiceDeployment = false;

      Iterator it = ejb3Deployment.getEjbContainers().values().iterator();
      while (it.hasNext())
      {
         EJBContainer container = (EJBContainer)it.next();
         if (isWebServiceBean(container))
         {
            isWebServiceDeployment = true;
            break;
         }
      }

      return isWebServiceDeployment;
   }

   private boolean isWebServiceBean(EJBContainer container)
   {
      boolean isWebServiceBean = false;
      if (container instanceof StatelessContainer)
      {
         boolean isWebService = container.resolveAnnotation(WebService.class) != null;
         boolean isWebServiceProvider = container.resolveAnnotation(WebServiceProvider.class) != null;
         isWebServiceBean = isWebService || isWebServiceProvider;
      }
      return isWebServiceBean;
   }
}