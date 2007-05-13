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

import java.util.LinkedList;
import java.util.List;

import org.jboss.deployers.plugins.deployer.AbstractSimpleDeployer;
import org.jboss.deployers.spi.DeploymentException;
import org.jboss.deployers.spi.deployer.DeploymentUnit;
import org.jboss.logging.Logger;

//$Id$

/**
 * This deployer that calls the registered DeployerHooks
 * 
 * @author Thomas.Diesler@jboss.org
 * @since 24-Apr-2007
 */
public abstract class AbstractWebServiceDeployer extends AbstractSimpleDeployer
{
   // provide logging
   private static final Logger log = Logger.getLogger(AbstractWebServiceDeployer.class);

   private List<DeployerHook> deployerHooks = new LinkedList<DeployerHook>();

   public void addDeployerHook(DeployerHook deployer)
   {
      log.debug("Add deployer hook: " + deployer);
      deployerHooks.add(deployer);
   }

   public void removeDeployerHook(DeployerHook deployer)
   {
      log.debug("Remove deployer hook: " + deployer);
      deployerHooks.remove(deployer);
   }

   @Override
   public void deploy(DeploymentUnit unit) throws DeploymentException
   {
      for (DeployerHook deployer : deployerHooks)
         deployer.deploy(unit);
   }

   @Override
   public void undeploy(DeploymentUnit unit)
   {
      for (DeployerHook deployer : deployerHooks)
         deployer.undeploy(unit);
   }
}
