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
import org.jboss.ejb3.Container;
import org.jboss.ejb3.EJBContainer;
import org.jboss.ejb3.Ejb3Deployment;
import org.jboss.ejb3.mdb.MessagingContainer;
import org.jboss.ejb3.metamodel.Ejb3PortComponent;
import org.jboss.ejb3.metamodel.EjbJarDD;
import org.jboss.ejb3.metamodel.EnterpriseBean;
import org.jboss.ejb3.metamodel.WebserviceDescription;
import org.jboss.ejb3.metamodel.Webservices;
import org.jboss.ejb3.session.SessionContainer;
import org.jboss.logging.Logger;
import org.jboss.wsf.spi.deployment.Deployment;
import org.jboss.wsf.spi.metadata.j2ee.EJBMetaData;
import org.jboss.wsf.spi.metadata.j2ee.EJBArchiveMetaData;
import org.jboss.wsf.spi.metadata.j2ee.EJBSecurityMetaData;
import org.jboss.wsf.spi.metadata.j2ee.MDBMetaData;
import org.jboss.wsf.spi.metadata.j2ee.SLSBMetaData;
import org.jboss.wsf.spi.metadata.j2ee.EJBArchiveMetaData.PublishLocationAdapter;

/**
 * Build container independent application meta data 
 *
 * @author Thomas.Diesler@jboss.org
 * @since 14-Apr-2007
 */
public class EJBArchiveMetaDataAdapterEJB3
{
   // logging support
   private static Logger log = Logger.getLogger(EJBArchiveMetaDataAdapterEJB3.class);

   public EJBArchiveMetaData buildUnifiedApplicationMetaData(Deployment dep, DeploymentUnit unit)
   {
      Ejb3Deployment ejb3Deployment = unit.getAttachment(Ejb3Deployment.class);
      dep.addAttachment(Ejb3Deployment.class, ejb3Deployment);

      EjbJarDD jarDD = unit.getAttachment(EjbJarDD.class);
      EJBArchiveMetaData umd = new EJBArchiveMetaData();      
      buildUnifiedBeanMetaData(umd, ejb3Deployment);
      buildWebservicesMetaData(umd, jarDD);

      return umd;
   }

   private void buildWebservicesMetaData(EJBArchiveMetaData umd, EjbJarDD jarDD)
   {
      // nothing to do
      if (jarDD == null)
         return;

      Webservices webservices = jarDD.getWebservices();
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

            // com/sun/ts/tests/webservices12/ejb/annotations/WSEjbWebServiceRefTest1
            // WSEjbWebServiceRefTest1VerifyTargetEndpointAddress
            if (contextRoot == null)
               contextRoot = "/" + wsd.getDescriptionName();
         }

         umd.setWebServiceContextRoot(contextRoot);
      }
   }

   private void buildUnifiedBeanMetaData(EJBArchiveMetaData umd, Ejb3Deployment ejb3Deployment)
   {
      List<EJBMetaData> ubmdList = new ArrayList<EJBMetaData>();
      Iterator<Container> it = ejb3Deployment.getEjbContainers().values().iterator();
      while (it.hasNext())
      {
         EJBContainer container = (EJBContainer)it.next();
         EJBMetaData ubmd = null;
         if (container instanceof SessionContainer)
         {
            ubmd = new SLSBMetaData();
         }
         else if (container instanceof MessagingContainer)
         {
            ubmd = new MDBMetaData();
            log.warn("No implemented: initialize MDB destination");
            //((UnifiedMessageDrivenMetaData)ubmd).setDestinationJndiName(((MessagingContainer)container).getDestination());
         }

         if (ubmd != null)
         {
            ubmd.setEjbName(container.getEjbName());
            ubmd.setEjbClass(container.getBeanClassName());

            EnterpriseBean bean = container.getXml();
            Ejb3PortComponent pcmd = (bean != null ? bean.getPortComponent() : null);
            if (pcmd != null)
            {
               ubmd.setPortComponentName(pcmd.getPortComponentName());
               ubmd.setPortComponentURI(pcmd.getPortComponentURI());
               EJBSecurityMetaData smd = new EJBSecurityMetaData();
               smd.setAuthMethod(pcmd.getAuthMethod());
               smd.setTransportGuarantee(pcmd.getTransportGuarantee());
               smd.setSecureWSDLAccess(pcmd.getSecureWSDLAccess());
               ubmd.setSecurityMetaData(smd);
            }

            ubmdList.add(ubmd);
         }
      }
      umd.setEnterpriseBeans(ubmdList);
   }

   private PublishLocationAdapter getPublishLocationAdpater(final Webservices webservices)
   {
      return new PublishLocationAdapter() {
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
}
