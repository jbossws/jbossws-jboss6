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

//$Id: ClassLoaderInjectionDeployer.java 3772 2007-07-01 19:29:13Z thomas.diesler@jboss.com $

import org.jboss.deployers.structure.spi.DeploymentUnit;
import org.jboss.metadata.WebMetaData;
import org.jboss.wsf.spi.deployment.DeploymentAspect;
import org.jboss.wsf.spi.deployment.Deployment;

/**
 * A deployer that injects the correct classloader into the Deployment 
 *
 * @author Thomas.Diesler@jboss.org
 * @since 25-Apr-2007
 */
public class ClassLoaderInjectionDeploymentAspect extends DeploymentAspect
{
   @Override
   public void create(Deployment dep)
   {
      DeploymentUnit unit = dep.getContext().getAttachment(DeploymentUnit.class);
      if (unit == null)
         throw new IllegalStateException("Cannot obtain deployement unit");

      ClassLoader classLoader = unit.getClassLoader();

      // Get the webapp context classloader and use it as the deploymet class loader
      WebMetaData webMetaData = dep.getContext().getAttachment(WebMetaData.class);
      if (webMetaData != null)
      {
         classLoader = webMetaData.getContextLoader();
      }

      dep.setClassLoader(classLoader);
   }
}