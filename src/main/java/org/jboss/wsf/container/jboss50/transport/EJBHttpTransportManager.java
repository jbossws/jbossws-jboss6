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

import org.jboss.logging.Logger;
import org.jboss.metadata.web.jboss.JBossWebMetaData;
import org.jboss.wsf.spi.transport.HttpListenerRef;
import org.jboss.wsf.spi.SPIProvider;
import org.jboss.wsf.spi.SPIProviderResolver;
import org.jboss.wsf.spi.deployment.Deployment;
import org.jboss.wsf.spi.deployment.Endpoint;
import org.jboss.wsf.spi.management.ServerConfig;
import org.jboss.wsf.spi.management.ServerConfigFactory;
import org.jboss.wsf.spi.transport.HttpSpec;
import org.jboss.wsf.spi.transport.ListenerRef;
import org.jboss.wsf.spi.transport.TransportManager;
import org.jboss.wsf.spi.transport.TransportSpec;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;

/**
 * Creates a webapp dpeloyment and pushes it into the deployment framework
 * 
 * @author Heiko.Braun <heiko.braun@jboss.com>
 */
public class EJBHttpTransportManager implements TransportManager
{
   private static Logger log = Logger.getLogger(EJBHttpTransportManager.class);
   private static final String PROCESSED_BY_DEPLOYMENT_FACTORY = "processed.by.deployment.factory";
   private WebAppDeploymentFactory deploymentFactory;
   private WebAppGenerator generator;
   private Map<String, Deployment> deploymentRegistry = new HashMap<String, Deployment>();

   public ListenerRef createListener(Endpoint endpoint, TransportSpec transportSpec)
   {

      assert deploymentFactory!=null;
      assert generator!=null;

      // Resolve the endpoint address
      if(! (transportSpec instanceof HttpSpec))
         throw new IllegalArgumentException("Unknown TransportSpec " + transportSpec);
      HttpSpec httpSpec = (HttpSpec)transportSpec;

      // Create JBossWebMetaData and attach it to the DeploymentUnit
      Deployment topLevelDeployment = endpoint.getService().getDeployment();

      // TODO: Somehow the ServletClass and InitParameter need to go from TransportSpec into generator
      topLevelDeployment.setProperty(HttpSpec.PROPERTY_WEBAPP_SERVLET_CLASS, httpSpec.getServletClass());
      topLevelDeployment.setProperty(HttpSpec.PROPERTY_WEBAPP_CONTEXT_PARAMETERS, httpSpec.getContextParameter());      
      
      // TODO: JBWS-2188
      Boolean alreadyDeployed = (Boolean)topLevelDeployment.getProperty(PROCESSED_BY_DEPLOYMENT_FACTORY); 
      if ((alreadyDeployed == null) || (false == alreadyDeployed))
      {
         JBossWebMetaData jbwMetaData = generator.create(topLevelDeployment);
         deploymentFactory.create(topLevelDeployment, jbwMetaData);
         topLevelDeployment.setProperty(PROCESSED_BY_DEPLOYMENT_FACTORY, Boolean.TRUE);
      }

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
               deploymentFactory.destroy(dep);
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

   public void setDeploymentFactory(WebAppDeploymentFactory deploymentFactory)
   {
      this.deploymentFactory = deploymentFactory;
   }

   public void setGenerator(WebAppGenerator generator)
   {
      this.generator = generator;
   }
}
