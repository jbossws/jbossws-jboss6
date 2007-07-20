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

import org.jboss.deployers.structure.spi.DeploymentUnit;
import org.jboss.logging.Logger;
import org.jboss.wsf.spi.SPIProvider;
import org.jboss.wsf.spi.SPIProviderResolver;
import org.jboss.wsf.spi.deployment.*;

//$Id$


/**
 * An abstract web service deployer.
 * 
 * @author Thomas.Diesler@jboss.org
 * @since 25-Apr-2007
 */
public abstract class AbstractDeployerHook implements DeployerHook
{
   // provide logging
   protected final Logger log = Logger.getLogger(getClass());

   protected DeploymentAspectManager deploymentAspectManager;

   private DeploymentModelFactory deploymentModelFactory;

   public AbstractDeployerHook()
   {
      SPIProvider spiProvider = SPIProviderResolver.getInstance().getProvider();
      deploymentModelFactory = spiProvider.getSPI(DeploymentModelFactory.class);

      if(null == deploymentModelFactory)
         throw new IllegalStateException("Unable to create spi.deployment.DeploymentModelFactory");
   }

   public void setDeploymentAspectManager(DeploymentAspectManager deploymentManager)
   {
      this.deploymentAspectManager = deploymentManager;
   }

   public Deployment createDeployment()
   {
      try
      {
         return deploymentModelFactory.createDeployment();
      }
      catch (Exception ex)
      {
         throw new WSFDeploymentException("Cannot load spi.deployment.Deployment class", ex);
      }
   }

   public Service createService()
   {
       try
      {
         return deploymentModelFactory.createService();
      }
      catch (Exception ex)
      {
         throw new WSFDeploymentException("Cannot load spi.deployment.Service class", ex);
      }
   }

   public Endpoint createEndpoint()
   {
      try
      {
         return deploymentModelFactory.createEndpoint();
      }
      catch (Exception ex)
      {
         throw new WSFDeploymentException("Cannot load spi.deployment.Endpoint class", ex);
      }
   }

   /** Return true if this deployment should be ignored
    */
   public boolean ignoreDeployment(DeploymentUnit unit)
   {
      String name = unit.getName();
      return (name.startsWith("jboss:id=") && name.indexOf("service=jacc") > 0);
   }
}
