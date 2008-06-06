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
package org.jboss.wsf.container.jboss50;

import org.jboss.logging.Logger;
import org.jboss.wsf.spi.WSFRuntime;
import org.jboss.wsf.spi.deployment.Deployment;
import org.jboss.wsf.spi.deployment.DeploymentAspectManager;
import org.jboss.wsf.spi.invocation.InvocationHandlerFactory;
import org.jboss.wsf.spi.invocation.RequestHandlerFactory;
import org.jboss.wsf.spi.management.EndpointRegistry;
import org.jboss.wsf.spi.transport.TransportManagerFactory;

/**
 * @author Heiko.Braun <heiko.braun@jboss.com>
 */
public class BareWSFRuntime implements WSFRuntime
{
   private static final Logger log = Logger.getLogger(BareWSFRuntime.class);
 
   private String runtimeName;

   private DeploymentAspectManager deploymentManager;

   private EndpointRegistry endpointRegistry;

   private RequestHandlerFactory requestHandlerFactory;

   private InvocationHandlerFactory invocationHandlerFactory;

   private TransportManagerFactory transportManagerFactory;

   public BareWSFRuntime(String runtimeName)
   {
      this.runtimeName = runtimeName;
   }
  
   // ---------------------------------------------------------------------------------

   public void create(Deployment deployment)
   {
      deploymentManager.create(deployment, this);
   }

   public void start(Deployment deployment)
   {
      deploymentManager.start(deployment, this);
   }

   public void stop(Deployment deployment)
   {
      deploymentManager.stop(deployment, this);
   }

   public void destroy(Deployment deployment)
   {
      deploymentManager.destroy(deployment, this);
   }

   // ---------------------------------------------------------------------------------

   public void setTransportManagerFactory(TransportManagerFactory factory)
   {
      assert factory!=null;
      log.debug(runtimeName + " -> TransportManagerFactory: " + factory);
      this.transportManagerFactory = factory;
   }

   public TransportManagerFactory getTransportManagerFactory()
   {
      return this.transportManagerFactory;
   }

   public void setEndpointRegistry(EndpointRegistry endpointRegistry)
   {
      assert endpointRegistry!=null;
      log.debug(runtimeName + " -> EndpointRegistry: " + endpointRegistry);
      this.endpointRegistry = endpointRegistry;
   }

   public EndpointRegistry getEndpointRegistry()
   {
      return this.endpointRegistry;
   }

   public void setDeploymentAspectManager(DeploymentAspectManager deploymentManager)
   {
      assert deploymentManager!=null;
      log.debug(runtimeName + " -> DeploymentAspectManager: " + deploymentManager);
      this.deploymentManager = deploymentManager;
   }

   public DeploymentAspectManager getDeploymentAspectManager()
   {
      return this.deploymentManager;
   }

   public void setRequestHandlerFactory(RequestHandlerFactory factory)
   {
      assert factory!=null;
      log.debug(runtimeName + " -> RequestHandlerFactory: "+ factory);
      this.requestHandlerFactory = factory;
   }

   public RequestHandlerFactory getRequestHandlerFactory()
   {
      return this.requestHandlerFactory;
   }

   public void setInvocationHandlerFactory(InvocationHandlerFactory factory)
   {
      assert factory!=null;
      log.debug(runtimeName + " -> InvocationHandlerFactory: "+ factory);
      this.invocationHandlerFactory = factory;
   }

   public InvocationHandlerFactory getInvocationHandlerFactory()
   {
      return this.invocationHandlerFactory;
   }

   public String toString()
   {
      return this.runtimeName+ ": " + super.toString();
   }
}
