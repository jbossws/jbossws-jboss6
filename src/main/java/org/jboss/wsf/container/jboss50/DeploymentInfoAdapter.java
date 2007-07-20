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
import org.jboss.deployers.vfs.spi.structure.VFSDeploymentUnit;
import org.jboss.ejb3.Ejb3Deployment;
import org.jboss.logging.Logger;
import org.jboss.metadata.ApplicationMetaData;
import org.jboss.metadata.WebMetaData;
import org.jboss.wsf.spi.deployment.Deployment;
import org.jboss.wsf.spi.deployment.UnifiedDeploymentInfo;

import java.net.URL;
import java.net.URLClassLoader;

// $Id$

/**
 * Build container independent deployment info.
 *
 * @author Thomas.Diesler@jboss.org
 * @since 05-May-2006
 */
public class DeploymentInfoAdapter
{
   // logging support
   private static Logger log = Logger.getLogger(DeploymentInfoAdapter.class);

   private ApplicationMetaDataAdapterEJB3 applicationMetaDataAdapterEJB3 = new ApplicationMetaDataAdapterEJB3();
   private ApplicationMetaDataAdapterEJB21 applicationMetaDataAdapterEJB21 = new ApplicationMetaDataAdapterEJB21();
   private WebMetaDataAdapter webMetaDataAdapter = new WebMetaDataAdapter();

   public void setApplicationMetaDataAdapterEJB21(ApplicationMetaDataAdapterEJB21 adapter)
   {
      this.applicationMetaDataAdapterEJB21 = adapter;
   }

   public void setApplicationMetaDataAdapterEJB3(ApplicationMetaDataAdapterEJB3 adapter)
   {
      this.applicationMetaDataAdapterEJB3 = adapter;
   }

   public void setWebMetaDataAdapter(WebMetaDataAdapter adapter)
   {
      this.webMetaDataAdapter = adapter;
   }

   public void buildDeploymentInfo(Deployment dep, UnifiedDeploymentInfo udi, DeploymentUnit unit)
   {
      dep.getContext().addAttachment(DeploymentUnit.class, unit);

      try
      {
         if (unit.getParent() != null)
         {
            udi.parent = new UnifiedDeploymentInfo(null);
            buildDeploymentInfo(dep, udi.parent, unit.getParent());
         }

         udi.vfRoot = new VirtualFileAdaptor(((VFSDeploymentUnit)unit).getRoot());

         udi.name = unit.getName();
         udi.simpleName = unit.getSimpleName();
         udi.url = udi.vfRoot.toURL();

         buildMetaData(dep, udi, unit);

         // Since we create temporary classes, we need to create a delegate loader
         // This prevents CCE problems where the parent loader is available at deploy time,
         // and a child loader is available at start time.
         udi.classLoader = new URLClassLoader(new URL[] {}, unit.getClassLoader());

         log.debug("UnifiedDeploymentInfo:\n" + udi);
      }
      catch (RuntimeException rte)
      {
         throw rte;
      }
      catch (Exception ex)
      {
         throw new IllegalStateException(ex);
      }
   }

   private void buildMetaData(Deployment dep, UnifiedDeploymentInfo udi, DeploymentUnit unit) throws Exception
   {
      if (unit.getAttachment(WebMetaData.class) != null)
      {
         udi.metaData = webMetaDataAdapter.buildUnifiedWebMetaData(dep, udi, unit);
         udi.webappURL = udi.vfRoot.toURL();
      }
      else if (unit.getAttachment(Ejb3Deployment.class) != null)
      {
         udi.metaData = applicationMetaDataAdapterEJB3.buildUnifiedApplicationMetaData(dep, udi, unit);
      }
      else if (unit.getAttachment(ApplicationMetaData.class) != null)
      {
         udi.metaData = applicationMetaDataAdapterEJB21.buildUnifiedApplicationMetaData(dep, udi, unit);
      }
   }
}
