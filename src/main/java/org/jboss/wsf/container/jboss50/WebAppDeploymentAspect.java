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

// $Id: WebAppDeployerDeployer.java 3772 2007-07-01 19:29:13Z thomas.diesler@jboss.com $

import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import org.jboss.deployers.client.spi.DeployerClient;
import org.jboss.deployers.vfs.spi.client.VFSDeploymentFactory;
import org.jboss.logging.Logger;
import org.jboss.virtual.VFS;
import org.jboss.virtual.VirtualFile;
import org.jboss.wsf.framework.deployment.WebXMLRewriter;
import org.jboss.wsf.spi.deployment.Deployment;
import org.jboss.wsf.spi.deployment.DeploymentAspect;
import org.jboss.wsf.spi.deployment.WSFDeploymentException;

/**
 * Publish the HTTP service endpoint to Tomcat 
 * 
 * @author Thomas.Diesler@jboss.org
 * @since 12-May-2006
 */
public class WebAppDeploymentAspect extends DeploymentAspect
{
   // provide logging
   private static Logger log = Logger.getLogger(WebAppDeploymentAspect.class);

   private DeployerClient mainDeployer;
   private WebXMLRewriter webXMLRewriter;
   private Map<String, org.jboss.deployers.client.spi.Deployment> deploymentMap = new HashMap<String, org.jboss.deployers.client.spi.Deployment>();

   public void setMainDeployer(DeployerClient mainDeployer)
   {
      this.mainDeployer = mainDeployer;
   }

   public void setWebXMLRewriter(WebXMLRewriter serviceEndpointPublisher)
   {
      this.webXMLRewriter = serviceEndpointPublisher;
   }

   public void create(Deployment dep)
   {
      if (dep.getType().toString().endsWith("EJB21") || dep.getType().toString().endsWith("EJB3"))
      {
         URL warURL = (URL)dep.getProperty(WebXMLRewriter.WEBAPP_URL);

         log.debug("publishServiceEndpoint: " + warURL);
         try
         {
            webXMLRewriter.rewriteWebXml(dep);
            org.jboss.deployers.client.spi.Deployment deployment = createDeploymentContext(warURL);

            mainDeployer.deploy(deployment);

            deploymentMap.put(warURL.toExternalForm(), deployment);
         }
         catch (Exception ex)
         {
            WSFDeploymentException.rethrow(ex);
         }
      }
   }

   public void destroy(Deployment dep)
   {
      URL warURL = (URL)dep.getProperty(WebXMLRewriter.WEBAPP_URL);
      if (warURL == null)
      {
         log.error("Cannot obtain warURL");
         return;
      }

      log.debug("destroyServiceEndpoint: " + warURL);
      try
      {
         org.jboss.deployers.client.spi.Deployment deployment = deploymentMap.get(warURL.toExternalForm());
         if (deployment != null)
         {
            mainDeployer.undeploy(deployment);
            deploymentMap.remove(warURL.toExternalForm());
         }
      }
      catch (Exception ex)
      {
         WSFDeploymentException.rethrow(ex);
      }
   }

   private org.jboss.deployers.client.spi.Deployment createDeploymentContext(URL warURL) throws Exception
   {
      VirtualFile file = VFS.getRoot(warURL);
      return VFSDeploymentFactory.getInstance().createVFSDeployment(file);
   }
}
