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

import org.jboss.deployers.spi.deployer.DeploymentUnit;
import org.jboss.logging.Logger;
import org.jboss.wsf.spi.deployment.DeployerManager;
import org.jboss.wsf.spi.deployment.Deployment;
import org.jboss.wsf.spi.deployment.Endpoint;
import org.jboss.wsf.spi.deployment.Service;
import org.jboss.wsf.spi.deployment.WSDeploymentException;

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

   protected DeployerManager deployerManager;
   protected String deploymentClass;
   protected String serviceClass;
   protected String endpointClass;

   public void setDeployerManager(DeployerManager deploymentManager)
   {
      this.deployerManager = deploymentManager;
   }

   public void setDeploymentClass(String deploymentClass)
   {
      this.deploymentClass = deploymentClass;
   }

   public void setEndpointClass(String endpointClass)
   {
      this.endpointClass = endpointClass;
   }

   public void setServiceClass(String serviceClass)
   {
      this.serviceClass = serviceClass;
   }

   public Deployment createDeployment()
   {
      try
      {
         ClassLoader loader = Thread.currentThread().getContextClassLoader();
         Class<?> clazz = loader.loadClass(deploymentClass);
         return (Deployment)clazz.newInstance();
      }
      catch (Exception ex)
      {
         throw new WSDeploymentException("Cannot load Deployment class: " + deploymentClass);
      }
   }

   public Service createService()
   {
      try
      {
         ClassLoader loader = Thread.currentThread().getContextClassLoader();
         Class<?> clazz = loader.loadClass(serviceClass);
         return (Service)clazz.newInstance();
      }
      catch (Exception ex)
      {
         throw new WSDeploymentException("Cannot load Service class: " + serviceClass);
      }
   }

   public Endpoint createEndpoint()
   {
      try
      {
         ClassLoader loader = Thread.currentThread().getContextClassLoader();
         Class<?> clazz = loader.loadClass(endpointClass);
         return (Endpoint)clazz.newInstance();
      }
      catch (Exception ex)
      {
         throw new WSDeploymentException("Cannot load Endpoint class: " + endpointClass);
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
