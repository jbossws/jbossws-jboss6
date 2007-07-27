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

import java.net.URL;

import org.jboss.deployers.structure.spi.DeploymentUnit;
import org.jboss.ejb3.Ejb3Deployment;
import org.jboss.logging.Logger;
import org.jboss.metadata.ApplicationMetaData;
import org.jboss.metadata.WebMetaData;
import org.jboss.wsf.framework.deployment.WebXMLRewriter;
import org.jboss.wsf.spi.deployment.ArchiveDeployment;
import org.jboss.wsf.spi.deployment.Deployment;
import org.jboss.wsf.spi.metadata.j2ee.EJBArchiveMetaData;
import org.jboss.wsf.spi.metadata.j2ee.JSEArchiveMetaData;

/**
 * Build container independent deployment info.
 *
 * @author Thomas.Diesler@jboss.org
 * @since 05-May-2006
 */
public class ContainerMetaDataAdapter
{
   // logging support
   private static Logger log = Logger.getLogger(ContainerMetaDataAdapter.class);

   private EJBArchiveMetaDataAdapterEJB3 applicationMetaDataAdapterEJB3 = new EJBArchiveMetaDataAdapterEJB3();
   private EJBArchiveMetaDataAdapterEJB21 applicationMetaDataAdapterEJB21 = new EJBArchiveMetaDataAdapterEJB21();
   private JSEArchiveMetaDataAdapter webMetaDataAdapter = new JSEArchiveMetaDataAdapter();

   public void setApplicationMetaDataAdapterEJB21(EJBArchiveMetaDataAdapterEJB21 adapter)
   {
      this.applicationMetaDataAdapterEJB21 = adapter;
   }

   public void setApplicationMetaDataAdapterEJB3(EJBArchiveMetaDataAdapterEJB3 adapter)
   {
      this.applicationMetaDataAdapterEJB3 = adapter;
   }

   public void setWebMetaDataAdapter(JSEArchiveMetaDataAdapter adapter)
   {
      this.webMetaDataAdapter = adapter;
   }

   public void buildContainerMetaData(Deployment dep, DeploymentUnit unit)
   {
      dep.addAttachment(DeploymentUnit.class, unit);

      try
      {
         // JSE endpoints
         if (unit.getAttachment(WebMetaData.class) != null)
         {
            JSEArchiveMetaData webMetaData = webMetaDataAdapter.buildUnifiedWebMetaData(dep, unit);
            if (webMetaData != null)
               dep.addAttachment(JSEArchiveMetaData.class, webMetaData);

            if (dep instanceof ArchiveDeployment)
            {
               URL webURL = ((ArchiveDeployment)dep).getRootFile().toURL();
               dep.setProperty(WebXMLRewriter.WEBAPP_URL, webURL);
            }
         }
         
         // EJB3 endpoints
         else if (unit.getAttachment(Ejb3Deployment.class) != null)
         {
            EJBArchiveMetaData appMetaData = applicationMetaDataAdapterEJB3.buildUnifiedApplicationMetaData(dep, unit);
            if (appMetaData != null)
               dep.addAttachment(EJBArchiveMetaData.class, appMetaData);
         }
         
         // EJB21 endpoints
         else if (unit.getAttachment(ApplicationMetaData.class) != null)
         {
            EJBArchiveMetaData appMetaData = applicationMetaDataAdapterEJB21.buildUnifiedApplicationMetaData(dep, unit);
            if (appMetaData != null)
               dep.addAttachment(EJBArchiveMetaData.class, appMetaData);
         }
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
}
