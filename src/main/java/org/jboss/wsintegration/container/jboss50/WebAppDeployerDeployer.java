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
package org.jboss.wsintegration.container.jboss50;

// $Id$

import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import org.jboss.deployers.plugins.structure.AbstractDeploymentContext;
import org.jboss.deployers.spi.deployment.MainDeployer;
import org.jboss.deployers.spi.structure.DeploymentContext;
import org.jboss.deployers.spi.structure.DeploymentState;
import org.jboss.logging.Logger;
import org.jboss.virtual.VFS;
import org.jboss.virtual.VirtualFile;
import org.jboss.wsintegration.spi.deployment.AbstractDeployer;
import org.jboss.wsintegration.spi.deployment.Deployment;
import org.jboss.wsintegration.spi.deployment.ServiceEndpointPublisher;
import org.jboss.wsintegration.spi.deployment.UnifiedDeploymentInfo;
import org.jboss.wsintegration.spi.deployment.WSDeploymentException;

/**
 * Publish the HTTP service endpoint to Tomcat 
 * 
 * @author Thomas.Diesler@jboss.org
 * @since 12-May-2006
 */
public class WebAppDeployerDeployer extends AbstractDeployer
{
   // provide logging
   private static Logger log = Logger.getLogger(WebAppDeployerDeployer.class);

   private MainDeployer mainDeployer;
   private ServiceEndpointPublisher serviceEndpointPublisher;
   private Map<String, DeploymentContext> contextMap = new HashMap<String, DeploymentContext>();

   public void setMainDeployer(MainDeployer mainDeployer)
   {
      this.mainDeployer = mainDeployer;
   }

   public void setServiceEndpointPublisher(ServiceEndpointPublisher serviceEndpointPublisher)
   {
      this.serviceEndpointPublisher = serviceEndpointPublisher;
   }

   public void create(Deployment dep)
   {
      UnifiedDeploymentInfo udi = dep.getContext().getAttachment(UnifiedDeploymentInfo.class);
      if (udi == null)
         throw new IllegalStateException("Cannot obtain unified deployement info");

      URL warURL = udi.webappURL;

      log.debug("publishServiceEndpoint: " + warURL);
      try
      {
         serviceEndpointPublisher.rewriteWebXml(udi);
         DeploymentContext context = createDeploymentContext(warURL);

         mainDeployer.addDeploymentContext(context);
         mainDeployer.process();

         contextMap.put(warURL.toExternalForm(), context);
      }
      catch (Exception ex)
      {
         WSDeploymentException.rethrow(ex);
      }
   }

   public void destroy(Deployment dep)
   {
      UnifiedDeploymentInfo udi = dep.getContext().getAttachment(UnifiedDeploymentInfo.class);
      if (udi == null)
         throw new IllegalStateException("Cannot obtain unified deployement info");

      URL warURL = udi.webappURL;
      if (warURL == null)
      {
         log.error("Cannot obtain warURL for: " + udi.name);
         return;
      }

      log.debug("destroyServiceEndpoint: " + warURL);
      try
      {
         DeploymentContext context = contextMap.get(warURL.toExternalForm());
         if (context != null)
         {
            context.setState(DeploymentState.UNDEPLOYING);
            mainDeployer.process();
            mainDeployer.removeDeploymentContext(context.getName());

            contextMap.remove(warURL.toExternalForm());
         }
      }
      catch (Exception ex)
      {
         WSDeploymentException.rethrow(ex);
      }
   }

   private DeploymentContext createDeploymentContext(URL warURL) throws Exception
   {
      VirtualFile file = VFS.getRoot(warURL);
      return new AbstractDeploymentContext(file);
   }
}
