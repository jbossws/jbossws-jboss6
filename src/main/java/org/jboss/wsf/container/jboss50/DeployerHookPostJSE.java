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

// $Id$

import java.util.Iterator;

import org.jboss.deployers.structure.spi.DeploymentUnit;
import org.jboss.metadata.NameValuePair;
import org.jboss.metadata.WebMetaData;
import org.jboss.metadata.web.Servlet;
import org.jboss.wsf.spi.deployment.Deployment;
import org.jboss.wsf.spi.deployment.Endpoint;

/**
 * @author Heiko.Braun@jboss.com
 * @version $Revision$
 */
public abstract class DeployerHookPostJSE extends AbstractDeployerHookJSE
{
   /**
    * The deployment should be created in phase 1.
    */
   public Deployment createDeployment(DeploymentUnit unit)
   {
      Deployment dep = unit.getAttachment(Deployment.class);
      if (null == dep)
         throw new IllegalStateException("spi.Deployment missing. It should be created in Phase 1");

      return dep;
   }

   /**
    * A phase 2 deployer hook needs to reject first-place
    * JSE deployments and wait for those that are re-written.
    * We do it by inspecting the Servlet init parameter.
    * @param unit
    * @return
    */
   @Override
   public boolean isWebServiceDeployment(DeploymentUnit unit)
   {
      if (super.isWebServiceDeployment(unit) == false)
         return false;

      Deployment deployment = unit.getAttachment(Deployment.class);
      boolean isModified = false;
      if (deployment != null)
         isModified = isModifiedServletClass(deployment);
      return isModified;
   }

   private boolean isModifiedServletClass(Deployment dep)
   {
      boolean modified = false;

      WebMetaData webMetaData = dep.getAttachment(WebMetaData.class);
      if (webMetaData != null)
      {
         for (Servlet servlet : webMetaData.getServlets())
         {
            String orgServletClass = servlet.getServletClass();

            // JSP
            if (orgServletClass == null || orgServletClass.length() == 0)
            {
               log.debug("Innore servlet class: " + orgServletClass);
               continue;
            }

            modified = isAlreadyModified(servlet);
         }
      }

      return modified;
   }

   private boolean isAlreadyModified(Servlet servlet)
   {
      Iterator itParams = servlet.getInitParams().iterator();
      while (itParams.hasNext())
      {
         NameValuePair pair = (NameValuePair)itParams.next();
         if (Endpoint.SEPID_DOMAIN_ENDPOINT.equals(pair.getName()))
            return true;
      }
      return false;
   }
}
