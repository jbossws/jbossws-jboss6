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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.jboss.deployers.structure.spi.DeploymentUnit;
import org.jboss.logging.Logger;
import org.jboss.metadata.ApplicationMetaData;
import org.jboss.metadata.BeanMetaData;
import org.jboss.metadata.EjbPortComponentMetaData;
import org.jboss.metadata.MessageDrivenMetaData;
import org.jboss.metadata.SessionMetaData;
import org.jboss.metadata.ApplicationMetaData.WebserviceDescription;
import org.jboss.metadata.ApplicationMetaData.Webservices;
import org.jboss.wsf.spi.deployment.Deployment;
import org.jboss.wsf.spi.deployment.UnifiedDeploymentInfo;
import org.jboss.wsf.spi.metadata.j2ee.UnifiedApplicationMetaData;
import org.jboss.wsf.spi.metadata.j2ee.UnifiedBeanMetaData;
import org.jboss.wsf.spi.metadata.j2ee.UnifiedEjbPortComponentMetaData;
import org.jboss.wsf.spi.metadata.j2ee.UnifiedMessageDrivenMetaData;
import org.jboss.wsf.spi.metadata.j2ee.UnifiedSessionMetaData;
import org.jboss.wsf.spi.metadata.j2ee.UnifiedApplicationMetaData.PublishLocationAdapter;

/**
 * Build container independent application meta data 
 *
 * @author Thomas.Diesler@jboss.org
 * @since 05-May-2006
 */
public class ApplicationMetaDataAdapterEJB21
{
   // logging support
   private static Logger log = Logger.getLogger(ApplicationMetaDataAdapterEJB21.class);

   public UnifiedApplicationMetaData buildUnifiedApplicationMetaData(Deployment dep, UnifiedDeploymentInfo udi, DeploymentUnit unit)
   {
      ApplicationMetaData appMetaData = unit.getAttachment(ApplicationMetaData.class);
      dep.getContext().addAttachment(ApplicationMetaData.class, appMetaData);
      
      UnifiedApplicationMetaData umd = new UnifiedApplicationMetaData();
      buildUnifiedBeanMetaData(umd, appMetaData);
      buildWebservicesMetaData(umd, appMetaData);
      umd.setSecurityDomain(appMetaData.getSecurityDomain());
      
      dep.getContext().addAttachment(UnifiedApplicationMetaData.class, umd);
      return umd;
   }

   private void buildWebservicesMetaData(UnifiedApplicationMetaData umd, ApplicationMetaData apmd)
   {
      Webservices webservices = apmd.getWebservices();
      if (webservices != null)
      {
         String contextRoot = webservices.getContextRoot();
         umd.setPublishLocationAdapter(getPublishLocationAdpater(webservices));

         List<WebserviceDescription> wsDescriptions = webservices.getWebserviceDescriptions();
         if (wsDescriptions.size() > 1)
            log.warn("Multiple <webservice-description> elements not supported");

         if (wsDescriptions.size() > 0)
         {
            WebserviceDescription wsd = wsDescriptions.get(0);
            umd.setConfigName(wsd.getConfigName());
            umd.setConfigFile(wsd.getConfigFile());
         }

         umd.setWebServiceContextRoot(contextRoot);
      }
   }

   private PublishLocationAdapter getPublishLocationAdpater(final Webservices webservices)
   {
      return new PublishLocationAdapter()
      {
         public String getWsdlPublishLocationByName(String name)
         {
            String wsdlPublishLocation = null;
            for (WebserviceDescription wsd : webservices.getWebserviceDescriptions())
            {
               if (wsd.getDescriptionName().equals(name))
               {
                  wsdlPublishLocation = wsd.getWsdlPublishLocation();
               }
            }
            return wsdlPublishLocation;
         }
      };
   }

   private void buildUnifiedBeanMetaData(UnifiedApplicationMetaData umd, ApplicationMetaData appMetaData)
   {
      List<UnifiedBeanMetaData> beans = new ArrayList<UnifiedBeanMetaData>();
      Iterator it = appMetaData.getEnterpriseBeans();
      while (it.hasNext())
      {
         BeanMetaData bmd = (BeanMetaData)it.next();
         buildUnifiedBeanMetaData(beans, bmd);
      }
      umd.setEnterpriseBeans(beans);
   }

   private UnifiedBeanMetaData buildUnifiedBeanMetaData(List<UnifiedBeanMetaData> beans, BeanMetaData bmd)
   {
      UnifiedBeanMetaData ubmd = null;
      if (bmd instanceof SessionMetaData)
      {
         ubmd = new UnifiedSessionMetaData();
      }
      else if (bmd instanceof MessageDrivenMetaData)
      {
         ubmd = new UnifiedMessageDrivenMetaData();
         ((UnifiedMessageDrivenMetaData)ubmd).setDestinationJndiName(((MessageDrivenMetaData)bmd).getDestinationJndiName());
      }

      if (ubmd != null)
      {
         ubmd.setEjbName(bmd.getEjbName());
         ubmd.setEjbClass(bmd.getEjbClass());
         ubmd.setServiceEndpointInterface(bmd.getServiceEndpoint());
         ubmd.setHome(bmd.getHome());
         ubmd.setLocalHome(bmd.getLocalHome());
         ubmd.setJndiName(bmd.getJndiName());
         ubmd.setLocalJndiName(bmd.getLocalJndiName());

         EjbPortComponentMetaData pcmd = bmd.getPortComponent();
         if (pcmd != null)
         {
            UnifiedEjbPortComponentMetaData upcmd = new UnifiedEjbPortComponentMetaData();
            upcmd.setPortComponentName(pcmd.getPortComponentName());
            upcmd.setPortComponentURI(pcmd.getPortComponentURI());
            upcmd.setAuthMethod(pcmd.getAuthMethod());
            upcmd.setTransportGuarantee(pcmd.getTransportGuarantee());
            upcmd.setSecureWSDLAccess(pcmd.getSecureWSDLAccess());
            ubmd.setPortComponent(upcmd);
         }

         beans.add(ubmd);
      }
      return ubmd;
   }
}
