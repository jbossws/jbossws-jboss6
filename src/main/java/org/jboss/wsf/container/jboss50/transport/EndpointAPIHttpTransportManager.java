/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2006, Red Hat Middleware LLC, and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
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
package org.jboss.wsf.container.jboss50.transport;

import org.jboss.deployers.client.plugins.deployment.AbstractDeployment;
import org.jboss.deployers.client.spi.DeployerClient;
import org.jboss.deployers.client.spi.DeploymentFactory;
import org.jboss.deployers.spi.attachments.MutableAttachments;
import org.jboss.deployers.structure.spi.ClassLoaderFactory;
import org.jboss.deployers.structure.spi.DeploymentUnit;
import org.jboss.logging.Logger;
import org.jboss.metadata.web.jboss.JBossWebMetaData;
import org.jboss.wsf.container.jboss50.deployment.tomcat.WebMetaDataModifier;
import org.jboss.wsf.spi.SPIProvider;
import org.jboss.wsf.spi.SPIProviderResolver;
import org.jboss.wsf.spi.deployment.Deployment;
import org.jboss.wsf.spi.deployment.Endpoint;
import org.jboss.wsf.spi.deployment.WSFDeploymentException;
import org.jboss.wsf.spi.management.ServerConfig;
import org.jboss.wsf.spi.management.ServerConfigFactory;
import org.jboss.wsf.spi.transport.TransportManager;
import org.jboss.wsf.spi.transport.ListenerRef;
import org.jboss.wsf.spi.transport.TransportSpec;
import org.jboss.wsf.spi.transport.HttpSpec;
import org.jboss.wsf.spi.transport.HttpListenerRef;

import javax.xml.ws.WebServiceException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Heiko.Braun <heiko.braun@jboss.com>
 */
public class EndpointAPIHttpTransportManager implements TransportManager
{
   private static Logger log = Logger.getLogger(EndpointAPIHttpTransportManager.class);
   private static final String PROCESSED_BY_DEPLOYMENT_FACTORY = "processed.by.deployment.factory";
   private WebAppGenerator generator;
   private DeploymentFactory factory = new DeploymentFactory();

   private WebMetaDataModifier webMetaDataModifier;
   private DeployerClient mainDeployer;

   private Map<String, Deployment> deploymentRegistry = new HashMap<String, Deployment>();
   
   public ListenerRef createListener(Endpoint endpoint, TransportSpec transportSpec)
   {
      assert generator!=null;
      assert webMetaDataModifier!=null;     

      // Resolve the endpoint address
      if(! (transportSpec instanceof HttpSpec))
         throw new IllegalArgumentException("Unknown TransportSpec " + transportSpec);
      HttpSpec httpSpec = (HttpSpec)transportSpec;

      // Create JBossWebMetaData and attach it to the DeploymentUnit
      Deployment topLevelDeployment = endpoint.getService().getDeployment();
      
      // Pass on to the main deployer
      Boolean alreadyDeployed = (Boolean)topLevelDeployment.getProperty(PROCESSED_BY_DEPLOYMENT_FACTORY); 
      if ((alreadyDeployed == null) || (false == alreadyDeployed))
      {
         generator.create(topLevelDeployment);
         deploy(topLevelDeployment);
         topLevelDeployment.setProperty(PROCESSED_BY_DEPLOYMENT_FACTORY, Boolean.TRUE);
      }


      // Server config
      SPIProvider provider = SPIProviderResolver.getInstance().getProvider();
      ServerConfigFactory spi = provider.getSPI(ServerConfigFactory.class);
      ServerConfig serverConfig = spi.getServerConfig();
      String host = serverConfig.getWebServiceHost();
      int port = serverConfig.getWebServicePort();
      String hostAndPort = host + (port > 0 ? ":" + port : "");

      ListenerRef listenerRef = null;
      try
      {
         String ctx = httpSpec.getWebContext();
         String pattern = httpSpec.getUrlPattern();
         listenerRef = new HttpListenerRef(ctx, pattern, new URI("http://" + hostAndPort + ctx + pattern));
      }
      catch (URISyntaxException e)
      {
         throw new RuntimeException("Failed to create ListenerRef", e);
      }

      // Map listenerRef for destroy phase
      deploymentRegistry.put( listenerRef.getUUID(), topLevelDeployment );

      return listenerRef;
   }

   public void destroyListener(ListenerRef ref)
   {
      Deployment dep = deploymentRegistry.get(ref.getUUID());
      if (dep != null)
      {
         // TODO: JBWS-2188
         Boolean alreadyDeployed = (Boolean)dep.getProperty(PROCESSED_BY_DEPLOYMENT_FACTORY); 
         if ((alreadyDeployed != null) && (true == alreadyDeployed))
         {
            try
            {
               undeploy(dep);
            }
            catch (Exception e)
            {
               log.error(e.getMessage(), e);
            }
            dep.removeProperty(PROCESSED_BY_DEPLOYMENT_FACTORY);
         }
      }
      deploymentRegistry.remove(ref.getUUID());
   }

   public void setGenerator(WebAppGenerator generator)
   {
      this.generator = generator;
   }
   
   public void setWebMetaDataModifier(WebMetaDataModifier webMetaDataModifier)
   {
      this.webMetaDataModifier = webMetaDataModifier;
   }

   public void setMainDeployer(DeployerClient mainDeployer)
   {
      this.mainDeployer = mainDeployer;
   }

   private void deploy(Deployment dep)
   {
      JBossWebMetaData jbwmd = dep.getAttachment(JBossWebMetaData.class);
      if (jbwmd == null)
         throw new WebServiceException("Cannot find web meta data");

      try
      {
         webMetaDataModifier.modifyMetaData(dep);

         final AbstractDeployment deployment = createSimpleDeployment(dep.getService().getContextRoot());
         MutableAttachments mutableAttachments = (MutableAttachments)deployment.getPredeterminedManagedObjects();
         mutableAttachments.addAttachment(HttpSpec.PROPERTY_GENERATED_WEBAPP, Boolean.TRUE);
         mutableAttachments.addAttachment(ClassLoaderFactory.class, new ContextClassLoaderFactory());
         mutableAttachments.addAttachment(JBossWebMetaData.class, jbwmd);
         mainDeployer.deploy(deployment);
      }
      catch (Exception ex)
      {
         WSFDeploymentException.rethrow(ex);
      }
   }

   private void undeploy(Deployment dep)
   {
      try
      {
         AbstractDeployment deployment = createSimpleDeployment(dep.getService().getContextRoot());
         mainDeployer.undeploy(deployment);
      }
      catch (Exception ex)
      {
         WSFDeploymentException.rethrow(ex);
      }
   }

   private AbstractDeployment createSimpleDeployment(String name)
   {
      AbstractDeployment unit = new AbstractDeployment(name);
      // There is one top level deployment
      factory.addContext(unit, "");
      return unit;
   }

   private static class ContextClassLoaderFactory implements ClassLoaderFactory
   {
      public ClassLoader createClassLoader(DeploymentUnit unit) throws Exception
      {
         return Thread.currentThread().getContextClassLoader();
      }

      public void removeClassLoader(DeploymentUnit unit) throws Exception
      {
      }
   }
}
